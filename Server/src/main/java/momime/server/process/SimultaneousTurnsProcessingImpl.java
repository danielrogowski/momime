package momime.server.process;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.calculations.CityCalculations;
import momime.common.database.MapFeature;
import momime.common.database.Plane;
import momime.common.database.RecordNotFoundException;
import momime.common.database.TileType;
import momime.common.database.UnitSpecialOrder;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.PendingMovement;
import momime.common.messages.servertoclient.PendingMovementMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.FogOfWarMidTurnMultiChanges;
import momime.server.fogofwar.KillUnitActionID;
import momime.server.utils.CityServerUtils;
import momime.server.utils.OverlandMapServerUtils;
import momime.server.utils.UnitServerUtils;

/**
 * Processing methods specifically for dealing with simultaneous turns games
 */
public final class SimultaneousTurnsProcessingImpl implements SimultaneousTurnsProcessing
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (SimultaneousTurnsProcessingImpl.class);

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnMultiChanges fogOfWarMidTurnMultiChanges;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** City calculations */
	private CityCalculations cityCalculations;
	
	/** City processing methods */
	private CityProcessing cityProcessing;

	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;

	/** Server-only overland map utils */
	private OverlandMapServerUtils overlandMapServerUtils;
	
	/** Server-only city utils */
	private CityServerUtils cityServerUtils;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;

	/** Starting and ending combats */
	private CombatStartAndEnd combatStartAndEnd;
	
	/** Methods for dealing with player msgs */
	private PlayerMessageProcessing playerMessageProcessing;

	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/**
	 * Processes PendingMovements at the end of a simultaneous turns game.
	 * 
	 * This routine will end when either we find a combat that needs to be played (in which case it ends with a call to startCombat)
	 * or when the only PendingMovements remaining are unit stacks that have no movement left (in which case it ends with a call to endPhase).
	 * 
	 * It must be able to carry on where it left off, so e.g. it is called the first time, and ends when it finds a combat that a human player needs
	 * to do, then the end of combatEnded will call it again, and it must be able to continue processing remaining movements.
	 * 
	 * Combats just between AI players still cause this routine to exit out, because combatEnded being triggered results in this routine
	 * being called again, so it will start another execution and continue where it left off, and the original invocation will exit out. 
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void processSimultaneousTurnsMovement (final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException, MomException, PlayerNotFoundException
	{
		// Process non combat moves
		while (findAndProcessOneCellPendingMovement (mom));

		// Is there a combat to process?
		if (!findAndProcessOneCombat (mom))
		{
			// No more combats - so end the turn
			log.debug ("No more combats, so ending turn " + mom.getGeneralPublicKnowledge ().getTurnNumber ());
			
			// Any pending movements remaining at this point must be unit stacks that ran out of movement before they
			// reached their destination - so we resend these pending moves to the client so the arrows display correctly
			// and it won't ask the player for where to move those units.
			for (final PlayerServerDetails player : mom.getPlayers ())
			{
				final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
				if (player.getPlayerDescription ().isHuman ())
				{
					final Iterator<PendingMovement> iter = priv.getPendingMovement ().iterator ();
					while (iter.hasNext ())
					{
						final PendingMovement thisMove = iter.next ();
	
						// Find each of the units
						final List<ExpandedUnitDetails> unitStack = new ArrayList<ExpandedUnitDetails> ();
						for (final Integer unitURN : thisMove.getUnitURN ())
						{
							final MemoryUnit tu = getUnitUtils ().findUnitURN (unitURN, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), "processSimultaneousTurnsMovement");
							unitStack.add (getExpandUnitDetails ().expandUnitDetails (tu, null, null, null,
								mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ()));
						}
						
						// We're at a different location now, and know more about the map than when the path was calculated, so need to recalculate it
						final List<Integer> path = getFogOfWarMidTurnMultiChanges ().determineMovementPath (unitStack, player,
							(MapCoordinates3DEx) thisMove.getMoveFrom (), (MapCoordinates3DEx) thisMove.getMoveTo (), mom);
						
						// In the process we might now find that the location has become unreachable, because of what we've learned about the map
						if (path == null)
							iter.remove ();
						else
						{
							// Use the updated path
							thisMove.getPath ().clear ();
							thisMove.getPath ().addAll (path);
							
							// Send it to the client
							final PendingMovementMessage msg = new PendingMovementMessage ();
							msg.setPendingMovement (thisMove);
							player.getConnection ().sendMessageToClient (msg);
						}
					}
				}
				else
					// AI players always scrub all pending moves after movement + combats are resolved, and recalculate where to go next turn
					priv.getPendingMovement ().clear ();
			}
	
			// Special orders - e.g. settlers building cities.
			// This can generate messages about spirits capturing nodes.
			// We want to send these now, even though we may be just about to run the EndPhase to keep consistency between whether
			// they are considered part of the previous or new turn depending on whether there's any combats or not
			// (Since if there are combats, there's no way to get messages generated
			// here sent with the message block generated in the EndPhase)
			processSpecialOrders (mom);
			getPlayerMessageProcessing ().sendNewTurnMessages (mom.getGeneralPublicKnowledge (), mom.getPlayers (), null);
	
			// End this turn and start the next one
			getPlayerMessageProcessing ().endPhase (mom, 0);
		}
	}
	
	/**
	 * Searches all pending movements across all players, making a list of all those where the first cell being
	 * moved to is free and clear, or occupied by ourselves (even if subsequent moves result in a combat).
	 * Then chooses one at random and processes the move.
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether we found a move to do
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	final boolean findAndProcessOneCellPendingMovement (final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException, MomException, PlayerNotFoundException
	{
		// Go through all pending movements for all players
		final List<OneCellPendingMovement> moves = new ArrayList<OneCellPendingMovement> ();
		for (final PlayerServerDetails player : mom.getPlayers ())
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			final Iterator<PendingMovement> iter = priv.getPendingMovement ().iterator ();
			while (iter.hasNext ())
			{
				final PendingMovement thisMove = iter.next ();
				
				// Loop finding each unit, and in process ensure they all have movement remaining
				int doubleMovementRemaining = Integer.MAX_VALUE;
				int movementTotal = Integer.MAX_VALUE;
				final List<ExpandedUnitDetails> unitStack = new ArrayList<ExpandedUnitDetails> ();
				for (final Integer unitURN : thisMove.getUnitURN ())
				{
					final MemoryUnit thisUnit = getUnitUtils ().findUnitURN (unitURN, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), "findAndProcessOneCellPendingMovement");
					final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (thisUnit, null, null, null,
						mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
					unitStack.add (xu);
					
					if (thisUnit.getDoubleOverlandMovesLeft () < doubleMovementRemaining)
						doubleMovementRemaining = thisUnit.getDoubleOverlandMovesLeft ();
					
					final int unitMovementTotal = xu.getMovementSpeed ();
					if (unitMovementTotal < movementTotal)
						movementTotal = unitMovementTotal;
				}
				
				// Any pending movements where the unit stack is out of movement, just leave them in the list until next turn
				if (doubleMovementRemaining > 0)
				{
					// Does this unit stack have a valid single cell non-combat move?
					final OneCellPendingMovement oneCell = getFogOfWarMidTurnMultiChanges ().determineOneCellPendingMovement
						(unitStack, player, thisMove, doubleMovementRemaining, mom);
					
					// Is the destination unreachable?  If so just remove the pending movement altogether
					if (oneCell == null)
						iter.remove ();
					
					// Does it initiate a combat?  If so then leave the pending movement (don't remove it) and we'll deal with it later
					else if (!oneCell.isCombatInitiated ())
					{
						// Careful, this generates a crazy amount of logs
						// log.debug ("findAndProcessOneCellPendingMovement found a move that needs doing: " + oneCell);
						
						for (int n = 0; n < movementTotal; n++)
							moves.add (oneCell);
					}
				}
			}
		}
		
		final boolean found = (moves.size () > 0);
		if (found)
		{
			// Pick a random movement to do
			final OneCellPendingMovement oneCell = moves.get (getRandomUtils ().nextInt (moves.size ()));
			log.debug ("Randomly chose move to do for player ID " + oneCell.getUnitStackOwner ().getPlayerDescription ().getPlayerID () +
				" from " + oneCell.getPendingMovement ().getMoveFrom () + " to " + oneCell.getPendingMovement ().getMoveTo () +
				", stepping to " + oneCell.getOneStep ());
			
			final List<ExpandedUnitDetails> unitStack = new ArrayList<ExpandedUnitDetails> ();
			for (final Integer unitURN : oneCell.getPendingMovement ().getUnitURN ())
			{
				final MemoryUnit tu = getUnitUtils ().findUnitURN (unitURN, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), "findAndProcessOneCellPendingMovement");
				unitStack.add (getExpandUnitDetails ().expandUnitDetails (tu, null, null, null,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ()));
			}
			
			// Execute the move
			getFogOfWarMidTurnMultiChanges ().moveUnitStack (unitStack, oneCell.getUnitStackOwner (), true,
				(MapCoordinates3DEx) oneCell.getPendingMovement ().getMoveFrom (), oneCell.getOneStep (), false, mom);
			
			// If they got to their destination, remove the pending move completely
			if (oneCell.getOneStep ().equals (oneCell.getPendingMovement ().getMoveTo ()))
			{
				final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) oneCell.getUnitStackOwner ().getPersistentPlayerPrivateKnowledge ();
				priv.getPendingMovement ().remove (oneCell.getPendingMovement ());
			}
			
			// Otherwise update the pending movement with the new start position
			else
				oneCell.getPendingMovement ().setMoveFrom (oneCell.getOneStep ());
		}
		
		return found;
	}

	/**
	 * Searches all pending movements across all players, making a list all of them (which at this stage are assumed to be combats).
	 * Then chooses one at random and initiates the combat.
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether we found a combat to do
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	final boolean findAndProcessOneCombat (final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException, MomException, PlayerNotFoundException
	{
		// Go through all pending movements for all players
		final List<OneCellPendingMovement> combats = new ArrayList<OneCellPendingMovement> ();
		for (final PlayerServerDetails player : mom.getPlayers ())
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			final Iterator<PendingMovement> iter = priv.getPendingMovement ().iterator ();
			while (iter.hasNext ())
			{
				final PendingMovement thisMove = iter.next ();
				
				// Loop finding each unit, and in process ensure they all have movement remaining
				int doubleMovementRemaining = Integer.MAX_VALUE;
				final List<ExpandedUnitDetails> unitStack = new ArrayList<ExpandedUnitDetails> ();
				for (final Integer unitURN : thisMove.getUnitURN ())
				{
					final MemoryUnit thisUnit = getUnitUtils ().findUnitURN (unitURN, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), "findAndProcessOneCombat");
					unitStack.add (getExpandUnitDetails ().expandUnitDetails (thisUnit, null, null, null,
						mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ()));
					
					if (thisUnit.getDoubleOverlandMovesLeft () < doubleMovementRemaining)
						doubleMovementRemaining = thisUnit.getDoubleOverlandMovesLeft ();
				}
				
				// Any pending movements where the unit stack is out of movement, just leave them in the list until next turn
				if (doubleMovementRemaining > 0)
				{
					// Does this unit stack have a valid single cell non-combat move?
					final OneCellPendingMovement oneCell = getFogOfWarMidTurnMultiChanges ().determineOneCellPendingMovement
						(unitStack, player, thisMove, doubleMovementRemaining, mom);
					
					// Is the destination unreachable?  If so these should have been dealt with by findAndProcessOneCellPendingMovement
					if (oneCell == null)
						throw new MomException ("findAndProcessOneCombat found a PendingMovement with an unreachable destination");
					
					// If this doesn't initiate a combat, then these should have been dealt with by findAndProcessOneCellPendingMovement
					if (!oneCell.isCombatInitiated ())
						throw new MomException ("findAndProcessOneCombat found a PendingMovement with movement remaining that isn't a combat");

					// Add the combat to the list
					combats.add (oneCell);
					log.debug ("findAndProcessOneCombat found a combat that needs doing: #" + combats.size () + " " + oneCell);
				}
			}
		}
		
		final boolean found = (combats.size () > 0);
		if (found)
		{
			// Now we've got a list of all outstanding combats, check to see if any of them are border conflicts/counterattacks
			// i.e. we find two PendingMovements that match up where A is moving to B and B is moving to A and the players are different.
			final List<BorderConflict> borderConflicts = new ArrayList<BorderConflict> ();
			for (int n = 0; n < combats.size () - 1; n++)
			{
				final OneCellPendingMovement firstMove = combats.get (n);
				for (int m = n + 1; m < combats.size (); m++)
				{
					final OneCellPendingMovement secondMove = combats.get (m);
					if ((firstMove.getUnitStackOwner () != secondMove.getUnitStackOwner ()) &&
						(firstMove.getPendingMovement ().getMoveFrom ().equals (secondMove.getOneStep ())) && 
						(secondMove.getPendingMovement ().getMoveFrom ().equals (firstMove.getOneStep ())))
					{
						borderConflicts.add (new BorderConflict (firstMove, secondMove));
						log.debug ("findAndProcessOneCombat found a border conflict that needs doing: #" + borderConflicts.size () + " " + firstMove + " / " + secondMove);
					}
				}
			}
			
			if (borderConflicts.size () > 0)
			{
				// Pick a random border conflict to do
				final BorderConflict borderConflict = borderConflicts.get (getRandomUtils ().nextInt (borderConflicts.size ()));
				log.debug ("Randomly chose border conflict: " + borderConflict.getFirstMove () + " / " + borderConflict.getSecondMove ());

				// Get the two map cells
				final OverlandMapCityData firstMoveFromCity = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
					(borderConflict.getFirstMove ().getPendingMovement ().getMoveFrom ().getZ ()).getRow ().get
					(borderConflict.getFirstMove ().getPendingMovement ().getMoveFrom ().getY ()).getCell ().get
					(borderConflict.getFirstMove ().getPendingMovement ().getMoveFrom ().getX ()).getCityData ();
				
				final OverlandMapCityData secondMoveFromCity = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
					(borderConflict.getSecondMove ().getPendingMovement ().getMoveFrom ().getZ ()).getRow ().get
					(borderConflict.getSecondMove ().getPendingMovement ().getMoveFrom ().getY ()).getCell ().get
					(borderConflict.getSecondMove ().getPendingMovement ().getMoveFrom ().getX ()).getCityData ();
				
				// Choose who will be "attacker" and "defender"
				// If one is a city, force them to be attacker so the city doesn't feature in the combat, otherwise choose random
				final OneCellPendingMovement attackerMove;
				final OneCellPendingMovement defenderMove;
				
				if (firstMoveFromCity != null)
				{
					attackerMove = borderConflict.getFirstMove ();
					defenderMove = borderConflict.getSecondMove ();
				}
				else if (secondMoveFromCity != null)
				{
					attackerMove = borderConflict.getSecondMove ();
					defenderMove = borderConflict.getFirstMove ();
				}
				else if (getRandomUtils ().nextBoolean ())
				{
					attackerMove = borderConflict.getFirstMove ();
					defenderMove = borderConflict.getSecondMove ();
				}
				else
				{
					attackerMove = borderConflict.getSecondMove ();
					defenderMove = borderConflict.getFirstMove ();
				}
				
				// Execute the combat
				getCombatStartAndEnd ().startCombat ((MapCoordinates3DEx) defenderMove.getPendingMovement ().getMoveFrom (),
					(MapCoordinates3DEx) attackerMove.getPendingMovement ().getMoveFrom (),
					attackerMove.getPendingMovement ().getUnitURN (), defenderMove.getPendingMovement ().getUnitURN (),
					attackerMove.getPendingMovement (), defenderMove.getPendingMovement (), mom);
			}
			else
			{
				// Pick a random regular combat to do
				final OneCellPendingMovement oneCell = combats.get (getRandomUtils ().nextInt (combats.size ()));
				log.debug ("Randomly chose combat: " + oneCell);
				
				// Execute the combat
				getCombatStartAndEnd ().startCombat (oneCell.getOneStep (),
					(MapCoordinates3DEx) oneCell.getPendingMovement ().getMoveFrom (), oneCell.getPendingMovement ().getUnitURN (), null,
					oneCell.getPendingMovement (), null, mom);
			}
		}
		
		return found;
	}
	
	/**
	 * Processes all unit & building special orders in a simultaneous turns game 'end phase'
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void processSpecialOrders (final MomSessionVariables mom)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException
	{
		// Dismiss units with pending dismiss orders.
		// Regular units are killed outright, heroes are killed outright on the clients but return to 'Generated' status on the server.
		final List<MemoryUnit> dismisses = getUnitServerUtils ().listUnitsWithSpecialOrder (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), UnitSpecialOrder.DISMISS);
		for (final MemoryUnit trueUnit : dismisses)
			mom.getWorldUpdates ().killUnit (trueUnit.getUnitURN (), KillUnitActionID.DISMISS);
		
		mom.getWorldUpdates ().process (mom);
		
		// Sell buildings
		for (final Plane plane : mom.getServerDB ().getPlane ())
			for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
				for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
				{
					final MemoryGridCell tc = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
						(plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x);
					
					if ((tc.getCityData () != null) && (tc.getBuildingIdSoldThisTurn () != null))
					{
						final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (x, y, plane.getPlaneNumber ());
						
						final MemoryBuilding buildingToSell = getMemoryBuildingUtils ().findBuilding
							(mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (), cityLocation, tc.getBuildingIdSoldThisTurn ());
						
						getCityProcessing ().sellBuilding (mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), cityLocation, buildingToSell.getBuildingURN (),
							false, true, mom.getSessionDescription (), mom.getServerDB ());
					}
				}
		
		// Get a list of all units with plane shift orders
		final List<MemoryUnit> planeShifters = getUnitServerUtils ().listUnitsWithSpecialOrder (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), UnitSpecialOrder.PLANE_SHIFT);
		while (planeShifters.size () > 0)
		{
			// Pick a random plane shifter and find any other units in the same stack plane shifting with it
			final int planeShifterIndex = getRandomUtils ().nextInt (planeShifters.size ());
			final MemoryUnit planeShifter = planeShifters.get (planeShifterIndex);
			
			final List<ExpandedUnitDetails> unitStack = new ArrayList<ExpandedUnitDetails> ();
			for (final MemoryUnit tu : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
				if ((planeShifters.contains (tu)) && (tu.getUnitLocation ().equals (planeShifter.getUnitLocation ())))
					unitStack.add (getExpandUnitDetails ().expandUnitDetails (tu, null, null, null, mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ()));
			
			getFogOfWarMidTurnMultiChanges ().planeShiftUnitStack (unitStack, mom);
			
			// Remove the whole stack from the list of plane shifters; also clear their special orders
			for (final ExpandedUnitDetails xu : unitStack)
			{
				planeShifters.remove (xu.getMemoryUnit ());
				
				xu.setSpecialOrder (null);
				getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (xu.getMemoryUnit (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
					mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting (), null);
			}
		}
		
		// Get a list of all settlers with build orders.
		
		// Have to be careful here - two settlers (whether owned by the same or different players) can both be on
		// pending build city orders right next to each other - only one of them is going to be able to build a city
		// because the other will then be within 3 squares of the first city.
		
		// So process settlers in a random order, then a random settler will win the 'race' and get to settle the contested location.
		final List<MemoryUnit> settlers = getUnitServerUtils ().listUnitsWithSpecialOrder (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), UnitSpecialOrder.BUILD_CITY);
		while (settlers.size () > 0)
		{
			// Pick a random settler and remove them from the list
			final int settlerIndex = getRandomUtils ().nextInt (settlers.size ());
			final MemoryUnit settler = settlers.get (settlerIndex);
			settlers.remove (settlerIndex);
			
			// Find where the settler is
			final MemoryGridCell tc = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
				(settler.getUnitLocation ().getZ ()).getRow ().get (settler.getUnitLocation ().getY ()).getCell ().get (settler.getUnitLocation ().getX ());
			final TileType tileType = mom.getServerDB ().findTileType (tc.getTerrainData ().getTileTypeID (), "processSpecialOrders-t");
			final MapFeature mapFeature = (tc.getTerrainData ().getMapFeatureID () == null) ? null : mom.getServerDB ().findMapFeature
				(tc.getTerrainData ().getMapFeatureID (), "processSpecialOrders-f");
			
			final PlayerServerDetails settlerOwner = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), settler.getOwningPlayerID (), "processSpecialOrders-s");
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) settlerOwner.getPersistentPlayerPrivateKnowledge ();

			String error = null;
			if (!tileType.isCanBuildCity ())
				error = "The type of terrain here has changed, you can no longer build a city here";
			else if ((mapFeature != null) && (!mapFeature.isCanBuildCity ()))
				error = "The map feature here has changed, you can no longer build a city here";
			else if (getCityCalculations ().markWithinExistingCityRadius
				(mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), priv.getFogOfWarMemory ().getMap (),
				settler.getUnitLocation ().getZ (), mom.getSessionDescription ().getOverlandMapSize ()).get (settler.getUnitLocation ().getX (), settler.getUnitLocation ().getY ()))
				
				error = "Another city was built before yours and is within " + mom.getSessionDescription ().getOverlandMapSize ().getCitySeparation () +
					" squares of where you are trying to build, so you cannot build here anymore";

			if (error != null)
			{
				// Show error
				log.warn ("process: " + settlerOwner.getPlayerDescription ().getPlayerName () + " got an error: " + error);

				if (settlerOwner.getPlayerDescription ().isHuman ())
				{
					final TextPopupMessage reply = new TextPopupMessage ();
					reply.setText (error);
					settlerOwner.getConnection ().sendMessageToClient (reply);
				}
			}
			else
				getCityServerUtils ().buildCityFromSettler (settlerOwner, settler, mom);
		}
		
		// Get a list of all spirits with meld orders.
		// Have to be careful here - although only one player can have units at any one node, its perfectly valid to
		// put multiple spirits all on meld orders in the same turn, especially if trying to take a node from an
		// enemy wizard - that way can put say 4 spirits all on meld orders and have a good chance that one of them will succeed.
		final List<MemoryUnit> spirits = getUnitServerUtils ().listUnitsWithSpecialOrder (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), UnitSpecialOrder.MELD_WITH_NODE);
		while (spirits.size () > 0)
		{
			// Pick a random spirit and remove them from the list
			final int spiritIndex = getRandomUtils ().nextInt (spirits.size ());
			final MemoryUnit spirit = spirits.get (spiritIndex);
			spirits.remove (spiritIndex);

			// Find where the spirit is
			final MemoryGridCell tc = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
				(spirit.getUnitLocation ().getZ ()).getRow ().get (spirit.getUnitLocation ().getY ()).getCell ().get (spirit.getUnitLocation ().getX ());

			final PlayerServerDetails spiritOwner = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), spirit.getOwningPlayerID (), "processSpecialOrders-s");
			
			// Since nodes can't be changed to any other kind of terrain, only thing which can go wrong here is if we already own the node
			String error = null;
			if ((tc.getTerrainData () != null) && (tc.getTerrainData ().getNodeOwnerID () != null) && (spirit.getOwningPlayerID () == tc.getTerrainData ().getNodeOwnerID ()))
				error = "You've already captured this node";
			
			else if ((tc.getTerrainData () != null) && (tc.getTerrainData ().isWarped () != null) && (tc.getTerrainData ().isWarped ()))
				error = "You cannot capture warped nodes";

			if (error != null)
			{
				// Show error
				log.warn ("process: " + spiritOwner.getPlayerDescription ().getPlayerName () + " got an error: " + error);

				if (spiritOwner.getPlayerDescription ().isHuman ())
				{
					final TextPopupMessage reply = new TextPopupMessage ();
					reply.setText (error);
					spiritOwner.getConnection ().sendMessageToClient (reply);
				}
			}
			else
			{
				final ExpandedUnitDetails xuSpirit = getExpandUnitDetails ().expandUnitDetails (spirit, null, null, null,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
						
				getOverlandMapServerUtils ().attemptToMeldWithNode (xuSpirit, mom);
			}			
		}
	}

	/**
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final UnitUtils utils)
	{
		unitUtils = utils;
	}

	/**
	 * @return Methods for updating true map + players' memory
	 */
	public final FogOfWarMidTurnChanges getFogOfWarMidTurnChanges ()
	{
		return fogOfWarMidTurnChanges;
	}

	/**
	 * @param obj Methods for updating true map + players' memory
	 */
	public final void setFogOfWarMidTurnChanges (final FogOfWarMidTurnChanges obj)
	{
		fogOfWarMidTurnChanges = obj;
	}

	/**
	 * @return Methods for updating true map + players' memory
	 */
	public final FogOfWarMidTurnMultiChanges getFogOfWarMidTurnMultiChanges ()
	{
		return fogOfWarMidTurnMultiChanges;
	}

	/**
	 * @param obj Methods for updating true map + players' memory
	 */
	public final void setFogOfWarMidTurnMultiChanges (final FogOfWarMidTurnMultiChanges obj)
	{
		fogOfWarMidTurnMultiChanges = obj;
	}
	
	/**
	 * @return Memory building utils
	 */
	public final MemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param utils Memory building utils
	 */
	public final void setMemoryBuildingUtils (final MemoryBuildingUtils utils)
	{
		memoryBuildingUtils = utils;
	}
	
	/**
	 * @return City processing methods
	 */
	public final CityProcessing getCityProcessing ()
	{
		return cityProcessing;
	}

	/**
	 * @param obj City processing methods
	 */
	public final void setCityProcessing (final CityProcessing obj)
	{
		cityProcessing = obj;
	}

	/**
	 * @return City calculations
	 */
	public final CityCalculations getCityCalculations ()
	{
		return cityCalculations;
	}

	/**
	 * @param calc City calculations
	 */
	public final void setCityCalculations (final CityCalculations calc)
	{
		cityCalculations = calc;
	}
	
	/**
	 * @return Server-only unit utils
	 */
	public final UnitServerUtils getUnitServerUtils ()
	{
		return unitServerUtils;
	}

	/**
	 * @param utils Server-only unit utils
	 */
	public final void setUnitServerUtils (final UnitServerUtils utils)
	{
		unitServerUtils = utils;
	}

	/**
	 * @return Server-only overland map utils
	 */
	public final OverlandMapServerUtils getOverlandMapServerUtils ()
	{
		return overlandMapServerUtils;
	}
	
	/**
	 * @param utils Server-only overland map utils
	 */
	public final void setOverlandMapServerUtils (final OverlandMapServerUtils utils)
	{
		overlandMapServerUtils = utils;
	}

	/**
	 * @return Server-only city utils
	 */
	public final CityServerUtils getCityServerUtils ()
	{
		return cityServerUtils;
	}

	/**
	 * @param utils Server-only city utils
	 */
	public final void setCityServerUtils (final CityServerUtils utils)
	{
		cityServerUtils = utils;
	}
	
	/**
	 * @return Random number generator
	 */
	public final RandomUtils getRandomUtils ()
	{
		return randomUtils;
	}

	/**
	 * @param utils Random number generator
	 */
	public final void setRandomUtils (final RandomUtils utils)
	{
		randomUtils = utils;
	}

	/**
	 * @return Server only helper methods for dealing with players in a session
	 */
	public final MultiplayerSessionServerUtils getMultiplayerSessionServerUtils ()
	{
		return multiplayerSessionServerUtils;
	}

	/**
	 * @param obj Server only helper methods for dealing with players in a session
	 */
	public final void setMultiplayerSessionServerUtils (final MultiplayerSessionServerUtils obj)
	{
		multiplayerSessionServerUtils = obj;
	}

	/**
	 * @return Starting and ending combats
	 */
	public final CombatStartAndEnd getCombatStartAndEnd ()
	{
		return combatStartAndEnd;
	}

	/**
	 * @param cse Starting and ending combats
	 */
	public final void setCombatStartAndEnd (final CombatStartAndEnd cse)
	{
		combatStartAndEnd = cse;
	}

	/**
	 * @return Methods for dealing with player msgs
	 */
	public PlayerMessageProcessing getPlayerMessageProcessing ()
	{
		return playerMessageProcessing;
	}

	/**
	 * @param obj Methods for dealing with player msgs
	 */
	public final void setPlayerMessageProcessing (final PlayerMessageProcessing obj)
	{
		playerMessageProcessing = obj;
	}

	/**
	 * @return expandUnitDetails method
	 */
	public final ExpandUnitDetails getExpandUnitDetails ()
	{
		return expandUnitDetails;
	}

	/**
	 * @param e expandUnitDetails method
	 */
	public final void setExpandUnitDetails (final ExpandUnitDetails e)
	{
		expandUnitDetails = e;
	}
}