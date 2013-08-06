package momime.server.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.calculations.MomCityCalculations;
import momime.common.calculations.MomUnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.CombatMapCoordinatesEx;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.servertoclient.v0_9_4.AskForCaptureCityDecisionMessage;
import momime.common.messages.servertoclient.v0_9_4.CombatEndedMessage;
import momime.common.messages.servertoclient.v0_9_4.KillUnitActionID;
import momime.common.messages.servertoclient.v0_9_4.KillUnitMessage;
import momime.common.messages.servertoclient.v0_9_4.KillUnitMessageData;
import momime.common.messages.servertoclient.v0_9_4.SelectNextUnitToMoveOverlandMessage;
import momime.common.messages.servertoclient.v0_9_4.SetUnitIntoOrTakeUnitOutOfCombatMessage;
import momime.common.messages.servertoclient.v0_9_4.StartCombatMessage;
import momime.common.messages.servertoclient.v0_9_4.StartCombatMessageUnit;
import momime.common.messages.v0_9_4.CaptureCityDecisionID;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MapAreaOfCombatTiles;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryCombatAreaEffect;
import momime.common.messages.v0_9_4.MemoryGridCell;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.TurnSystem;
import momime.common.messages.v0_9_4.UnitCombatSideID;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.MomUnitAttributeComponent;
import momime.common.utils.MomUnitAttributePositiveNegative;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.ScheduledCombatUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.MomServerCityCalculations;
import momime.server.calculations.MomServerResourceCalculations;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_4.Plane;
import momime.server.fogofwar.FogOfWarDuplication;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.FogOfWarProcessing;
import momime.server.mapgenerator.CombatMapGenerator;
import momime.server.messages.ServerMemoryGridCellUtils;
import momime.server.messages.v0_9_4.ServerGridCell;
import momime.server.utils.CityServerUtils;
import momime.server.utils.OverlandMapServerUtils;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Routines dealing with initiating, progressing and ending combats
 */
public final class CombatProcessingImpl implements CombatProcessing
{
	/** Max number of units to fill each row during combat set up */ 
	private static final int COMBAT_SETUP_UNITS_PER_ROW = 5;
	
	// NB. These aren't private so that the unit tests can use them too
	
	/** X coord of centre location around which defenders are placed */
	static final int COMBAT_SETUP_DEFENDER_FRONT_ROW_CENTRE_X = 4;
	
	/** Y coord of centre location around which defenders are placed */
	static final int COMBAT_SETUP_DEFENDER_FRONT_ROW_CENTRE_Y = 11;
	
	/** Direction that defenders initially face */
	static final int COMBAT_SETUP_DEFENDER_FACING = 4;

	/** Defenders may require 5 rows to fit 20 units if they're in a city with city walls */
	static final int COMBAT_SETUP_DEFENDER_ROWS = 5;

	/** X coord of centre location around which attackers are placed */
	static final int COMBAT_SETUP_ATTACKER_FRONT_ROW_CENTRE_X = 7;
	
	/** Y coord of centre location around which attackers are placed */
	static final int COMBAT_SETUP_ATTACKER_FRONT_ROW_CENTRE_Y = 17;
	
	/** Direction that attackers initially face */
	static final int COMBAT_SETUP_ATTACKER_FACING = 8;

	/** Attackers never have any obstructions so 4 rows x5 per row is always enough to fit 20 units */
	static final int COMBAT_SETUP_ATTACKER_ROWS = 4;
	
	/** Class logger */
	private final Logger log = Logger.getLogger (CombatProcessingImpl.class.getName ());

	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** MemoryGridCell utils */
	private MemoryGridCellUtils memoryGridCellUtils;

	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Unit calculations */
	private MomUnitCalculations unitCalculations;
	
	/** City calculations */
	private MomCityCalculations cityCalculations;
	
	/** Server-only city calculations */
	private MomServerCityCalculations serverCityCalculations;
	
	/** Server-only overland map utils */
	private OverlandMapServerUtils overlandMapServerUtils;
	
	/** Server-only city utils */
	private CityServerUtils cityServerUtils;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** Main FOW update routine */
	private FogOfWarProcessing fogOfWarProcessing;

	/** Resource calculations */
	private MomServerResourceCalculations serverResourceCalculations;

	/** FOW duplication utils */
	private FogOfWarDuplication fogOfWarDuplication;
	
	/** Scheduled combat utils */
	private ScheduledCombatUtils scheduledCombatUtils; 
	
	/** Simultaneous turns combat scheduler */
	private CombatScheduler combatScheduler;
	
	/** Map generator */
	private CombatMapGenerator combatMapGenerator;
	
	/**
	 * Sets up a combat on the server and any client(s) who are involved
	 *
	 * @param defendingLocation Location where defending units are standing
	 * @param attackingFrom Location where attacking units are standing (which will be a map tile adjacent to defendingLocation)
	 * @param scheduledCombatURN Scheduled combat URN, if simultaneous turns game; null for one-at-a-time games
	 * @param attackingPlayer Player who is attacking
	 * @param attackingUnitURNs Which of the attacker's unit stack are attacking - they might be leaving some behind
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void startCombat (final OverlandMapCoordinatesEx defendingLocation, final OverlandMapCoordinatesEx attackingFrom,
		final Integer scheduledCombatURN, final PlayerServerDetails attackingPlayer, final List<Integer> attackingUnitURNs, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.entering (CombatProcessingImpl.class.getName (), "startCombat",
			new String [] {defendingLocation.toString (), attackingFrom.toString (), (scheduledCombatURN == null) ? "NotSched" : scheduledCombatURN.toString (),
			attackingPlayer.getPlayerDescription ().getPlayerID ().toString ()});
		
		// If attacking a tower in Myrror, then Defending-AttackingFrom will be 1-0
		// If attacking from a tower onto Myrror, then Defending-AttackingFrom will be 0-1 - both of these should be shown on Myrror only
		// Any tower combats to/from Arcanus will be 0-0, and should appear on Arcanus only
		// Hence the reason for the Max
		final OverlandMapCoordinatesEx combatLocation = new OverlandMapCoordinatesEx ();
		combatLocation.setX (defendingLocation.getX ());
		combatLocation.setY (defendingLocation.getY ());
		combatLocation.setPlane (Math.max (defendingLocation.getPlane (), attackingFrom.getPlane ()));
		
		// If this is a scheduled combat then check it out... WalkInWithoutAFight may be switched on, in which case we want to move rather than setting up a combat
		boolean walkInWithoutAFight = false;
		if (scheduledCombatURN != null)
			walkInWithoutAFight = getScheduledCombatUtils ().findScheduledCombatURN
				(mom.getGeneralServerKnowledge ().getScheduledCombat (), scheduledCombatURN).isWalkInWithoutAFight ();
		
		// Record the scheduled combat ID
		final ServerGridCell tc = (ServerGridCell) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(combatLocation.getPlane ()).getRow ().get (combatLocation.getY ()).getCell ().get (combatLocation.getX ());
		tc.setScheduledCombatURN (scheduledCombatURN);
		
		// Find out who we're attacking - if an empty lair, we can get nil here
		final MemoryUnit firstDefendingUnit = getUnitUtils ().findFirstAliveEnemyAtLocation (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (),
			defendingLocation.getX (), defendingLocation.getY (), defendingLocation.getPlane (), 0);
		PlayerServerDetails defendingPlayer = (firstDefendingUnit == null) ? null : MultiplayerSessionServerUtils.findPlayerWithID
			(mom.getPlayers (), firstDefendingUnit.getOwningPlayerID (), "startCombat");
		final MomPersistentPlayerPublicKnowledge defPub = (defendingPlayer == null) ? null : (MomPersistentPlayerPublicKnowledge) defendingPlayer.getPersistentPlayerPublicKnowledge ();
		
		// If there is a real defender, we need to inform all players that they're involved in a combat
		// We dealt with informing about the attacker in InitiateCombat
		if ((defendingPlayer != null) && (mom.getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS))
			getCombatScheduler ().informClientsOfPlayerBusyInCombat (defendingPlayer, mom.getPlayers (), true);

		// Start off the message to the client
		final StartCombatMessage startCombatMessage = new StartCombatMessage ();
		startCombatMessage.setCombatLocation (combatLocation);
		startCombatMessage.setScheduledCombatURN (scheduledCombatURN);
		startCombatMessage.setCreateDefenders ((defPub != null) && (defPub.getWizardID ().equals (CommonDatabaseConstants.WIZARD_ID_MONSTERS)) &&
			(ServerMemoryGridCellUtils.isNodeLairTower (tc.getTerrainData (), mom.getServerDB ())));
		
		// Generate the combat scenery
		tc.setCombatMap (getCombatMapGenerator ().generateCombatMap (mom.getCombatMapCoordinateSystem (),
			mom.getServerDB (), mom.getGeneralServerKnowledge ().getTrueMap (), combatLocation));
		startCombatMessage.setCombatTerrain (tc.getCombatMap ());
		
		// Set the location of both defenders and attackers.
		// Need the map generation done first, so we know where there is impassable terrain to avoid placing units on it.
		// Final 'True' parameter is because only some of the units in the attacking cell may actually be attacking, whereas everyone in the defending cell will always help defend.
		// We need to do this (at least on the server) even if we immediately end the combat below, since we need to mark the attackers into the combat so that they will advance 1 square.
		log.finest ("Positioning defenders at " + defendingLocation);
		positionCombatUnits (combatLocation, startCombatMessage, attackingPlayer, defendingPlayer, mom.getCombatMapCoordinateSystem (), defendingLocation,
			COMBAT_SETUP_DEFENDER_FRONT_ROW_CENTRE_X, COMBAT_SETUP_DEFENDER_FRONT_ROW_CENTRE_Y, COMBAT_SETUP_DEFENDER_ROWS, COMBAT_SETUP_DEFENDER_FACING,
			UnitCombatSideID.DEFENDER, null, tc.getCombatMap (), mom);
				
		log.finest ("Positioning attackers at " + defendingLocation);
		positionCombatUnits (combatLocation, startCombatMessage, attackingPlayer, defendingPlayer, mom.getCombatMapCoordinateSystem (), attackingFrom,
			COMBAT_SETUP_ATTACKER_FRONT_ROW_CENTRE_X, COMBAT_SETUP_ATTACKER_FRONT_ROW_CENTRE_Y, COMBAT_SETUP_ATTACKER_ROWS, COMBAT_SETUP_ATTACKER_FACING,
			UnitCombatSideID.ATTACKER, attackingUnitURNs, tc.getCombatMap (), mom);
		
		// Are there any defenders (some lairs are empty) - if not then bypass the combat entirely
		if ((walkInWithoutAFight) || (defendingPlayer == null))
		{
			log.finest ("Combat ending before it starts");
			
			// There's a few situations to deal with here:
			// If "walk in without a fight" is on, then we have other units already at that location, so there definitely no defender.
			// The other way we can get to this block of code is if there's no defending units - this includes that we may be walking into
			// an enemy city with no defence, and for this situation we need to set the player correctly so we notify the player that they've lost their city.
			// It should be impossible to initiate a combat against your own city, since the "walk in without a fight" check is done first.
			if (walkInWithoutAFight)
				defendingPlayer = null;
			
			else if ((tc.getCityData () != null) && (tc.getCityData ().getCityPopulation () != null) && (tc.getCityData ().getCityPopulation () > 0) && (tc.getCityData ().getCityOwnerID () != null))
				defendingPlayer = MultiplayerSessionServerUtils.findPlayerWithID (mom.getPlayers (), tc.getCityData ().getCityOwnerID (), "startCombat-CD");
				
			else
				// It'll be null anyway, but just to be certain...
				defendingPlayer = null;
				
			combatEnded (combatLocation, attackingPlayer, defendingPlayer, attackingPlayer,	// <-- who won
				null, mom);
		}
		else
		{
			log.finest ("Continuing combat setup");
			throw new UnsupportedOperationException ("Real combats not implemented yet");
		}

		log.exiting (CombatProcessingImpl.class.getName (), "startCombat");
	}
	
	/**
	 * Purpose of this is to check for impassable terrain obstructions.  All the rocks, housing, ridges and so on are still passable, the only impassable things are
	 * city wall corners and the main feature (node, temple, tower of wizardry, etc. on the defender side).
	 * 
	 * So for attackers we always end up with 4 full rows like
	 * XXXXX
	 * XXXXX
	 * XXXXX
	 * XXXXX
	 * 
	 * For defenders in a city with walls we end up with 5 rows patterened like so (or there may be more spaces if no city walls) 
	 *   XXX
	 * XXXXX
	 * XX  XX
	 * XXXXX
	 *   XXX
	 *   
	 * so either way we should always have 20 spaces
	 *
	 * @param startX X coordinate within the combat map to centre the units around
	 * @param startY Y coordinate within the combat map to centre the units around
	 * @param unitHeading The direction the placed units will be facing
	 * @param maxRows Number of rows the units need to be arranged into in order to fit
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @param combatMap Combat scenery we are placing the units onto (important because some tiles will be impassable to some types of unit)
	 * @param db Lookup lists built over the XML database
	 * @return List of how many units we can fit into each row
	 * @throws RecordNotFoundException If we counter a combatTileBorderID or combatTileTypeID that can't be found in the db
	 */
	final List<Integer> determineMaxUnitsInRow (final int startX, final int startY, final int unitHeading, final int maxRows,
		final CoordinateSystem combatMapCoordinateSystem, final MapAreaOfCombatTiles combatMap, final ServerDatabaseEx db) throws RecordNotFoundException
	{
		final List<Integer> maxUnitsInRow = new ArrayList<Integer> ();
		final CombatMapCoordinatesEx centre = new CombatMapCoordinatesEx ();
		centre.setX (startX);
		centre.setY (startY);
	
		for (int rowNo = 0; rowNo < maxRows; rowNo++)
		{
			int maxUnitsInThisRow = 0;
			
			final CombatMapCoordinatesEx coords = new CombatMapCoordinatesEx ();
			coords.setX (centre.getX ());
			coords.setY (centre.getY ());
			
			// Move down-left to start of row...
			for (int n = 0; n < COMBAT_SETUP_UNITS_PER_ROW/2; n++)
				CoordinateSystemUtils.moveCoordinates (combatMapCoordinateSystem, coords, 6);
			
			// ..then position units in an up-right fashion to fill the row
			for (int n = 0; n < COMBAT_SETUP_UNITS_PER_ROW; n++)
			{
				if (getUnitCalculations ().calculateDoubleMovementToEnterCombatTile (combatMap.getRow ().get (coords.getY ()).getCell ().get (coords.getX ()), db) >= 0)
					maxUnitsInThisRow++;
				
				CoordinateSystemUtils.moveCoordinates (combatMapCoordinateSystem, coords, 2);
			}
			
			maxUnitsInRow.add (maxUnitsInThisRow);
			CoordinateSystemUtils.moveCoordinates (combatMapCoordinateSystem, centre,
				CoordinateSystemUtils.normalizeDirection (combatMapCoordinateSystem.getCoordinateSystemType (), unitHeading + 4));
		}
		
		return maxUnitsInRow;
	}
	
	/**
	 * @param unit Unit to compare
	 * @param players Players list
	 * @param spells Known spells
	 * @param combatAreaEffects Known combat area effects
	 * @param db Lookup lists built over the XML database
	 * @return Combat class of the unit, as defined in the list above
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 */
	final int calculateUnitCombatClass (final MemoryUnit unit, final List<PlayerServerDetails> players, final List<MemoryMaintainedSpell> spells,
		final List<MemoryCombatAreaEffect> combatAreaEffects, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.entering (CombatProcessingImpl.class.getName (), "calculateUnitCombatClass", unit.getUnitURN ());
		final int result;
		
		// Does this unit have a ranged attack?
		if (getUnitUtils ().getModifiedAttributeValue (unit, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_RANGED_ATTACK,
			MomUnitAttributeComponent.ALL, MomUnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db) > 0)
		{
			// Ranged hero or regular unit?
			if (db.findUnit (unit.getUnitID (), "calculateUnitCombatClass").getUnitMagicRealm ().equals (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
				result = 3;
			else
				result = 4;
		}
		
		// Does this unit have a melee attack?
		else if (getUnitUtils ().getModifiedAttributeValue (unit, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_MELEE_ATTACK,
			MomUnitAttributeComponent.ALL, MomUnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db) > 0)
		{
			// Melee hero or regular unit?
			if (db.findUnit (unit.getUnitID (), "calculateUnitCombatClass").getUnitMagicRealm ().equals (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
				result = 1;
			else
				result = 2;
		}
		
		else
			// No attacks at all (settlers)
			result = 5;
		
		log.exiting (CombatProcessingImpl.class.getName (), "calculateUnitCombatClass", result);
		return result;
	}
	
	/**
	 * @param units List of units being positioned in combat; must already have been sorted into combat class order
	 * @return List of how many of each combat class there are
	 */
	final List<Integer> listNumberOfEachCombatClass (final List<MemoryUnitAndCombatClass> units)
	{
		log.entering (CombatProcessingImpl.class.getName (), "listNumberOfEachCombatClass", units.size ());
				
		final List<Integer> counts = new ArrayList<Integer> ();
		
		int currentCombatClass = 0;
		int currentCombatClassCount = 0;
		for (final MemoryUnitAndCombatClass unit : units)
		{
			if (unit.getCombatClass () == currentCombatClass)
				currentCombatClassCount++;
			else
			{
				// Previous entry to add to list?
				if (currentCombatClassCount > 0)
					counts.add (currentCombatClassCount);
				
				currentCombatClass = unit.getCombatClass ();
				currentCombatClassCount = 1;
			}
		}

		// Final entry to add to list?
		if (currentCombatClassCount > 0)
			counts.add (currentCombatClassCount);
		
		log.exiting (CombatProcessingImpl.class.getName (), "listNumberOfEachCombatClass", counts.size ());
		return counts;
	}
	
	/**
	 * Compacts up the smallest values in the list if we've got too many rows, so if the
	 * e.g. above are the attackers, they only have 4 rows So want to go from 4, 2, 2, 6, 3 to 4, 4, 6, 3
	 * 
	 * @param unitsInRow The number of units to arrange into each row of combat setup
	 * @param maxRows Number of rows the units need to be arranged into in order to fit
	 */
	final void mergeRowsIfTooMany (final List<Integer> unitsInRow, final int maxRows)
	{
		log.entering (CombatProcessingImpl.class.getName (), "listNumberOfEachCombatClass", new Integer [] {unitsInRow.size (), maxRows});
		
		while (unitsInRow.size () > maxRows)
		{
			// Find the lowest adjacent two values - start by assuming its the first two
			int bestRowNo = 0;
			int bestCombatClass = unitsInRow.get (0) + unitsInRow.get (1);
			
			for (int rowNo = 1; rowNo < unitsInRow.size () - 1; rowNo++)
			{
				final int thisCombatClass = unitsInRow.get (rowNo) + unitsInRow.get (rowNo+1);
				if (thisCombatClass < bestCombatClass)
				{
					bestCombatClass = thisCombatClass;
					bestRowNo = rowNo;
				}
			}
			
			// Compact out the entry
			unitsInRow.set (bestRowNo, unitsInRow.get (bestRowNo) + unitsInRow.get (bestRowNo+1));
			unitsInRow.remove (bestRowNo+1);
		}
		
		log.exiting (CombatProcessingImpl.class.getName (), "listNumberOfEachCombatClass", unitsInRow.size ());
	}
	
	/**
	 * Move units backwards if there's too many units in any row
	 * So e.g. want to go from 4, 4, 6, 3 to 4, 4, 5, 4
	 * 
	 * @param unitsInRow The number of units to arrange into each row of combat setup
	 * @param maxUnitsInRow Maximum number of units we can fit on each row
	 */
	final void moveUnitsInOverfullRowsBackwards (final List<Integer> unitsInRow, final List<Integer> maxUnitsInRow)
	{
		log.entering (CombatProcessingImpl.class.getName (), "moveUnitsInOverfullRowsBackwards", new Integer [] {unitsInRow.size (), maxUnitsInRow.size ()});
		
		int overflow = 0;
		for (int rowNo = 0; rowNo < unitsInRow.size (); rowNo++)
		{
			// Any spare space in this row?
			if ((overflow > 0) && (unitsInRow.get (rowNo) < maxUnitsInRow.get (rowNo)))
			{
				final int unitsToBump = Math.min (overflow, maxUnitsInRow.get (rowNo) - unitsInRow.get (rowNo));
				overflow = overflow - unitsToBump;
				unitsInRow.set (rowNo, unitsInRow.get (rowNo) + unitsToBump);
			}
			
			// Any that don't fit in this row?
			if (unitsInRow.get (rowNo) > maxUnitsInRow.get (rowNo))
			{
				overflow = overflow + (unitsInRow.get (rowNo) - maxUnitsInRow.get (rowNo));
				unitsInRow.set (rowNo, maxUnitsInRow.get (rowNo));
			}
		}
		
		// We can have overflow from the end, so if we do and we've got spare rows, then fill them
		while ((overflow > 0) && (unitsInRow.size () < maxUnitsInRow.size ()))
		{
			final int rowNo = unitsInRow.size ();
			final int unitsInThisRow = Math.min (overflow, maxUnitsInRow.get (rowNo));
			unitsInRow.add (unitsInThisRow);
			overflow = overflow - unitsInThisRow;
		}
		
		// If we've *still* got an overflow then just shove the units in the back row even though they don't fit (they'll get fixed up in moveUnitsInOverfullRowsForwards)
		if (overflow > 0)
		{
			final int rowNo = unitsInRow.size () - 1;
			unitsInRow.set (rowNo, unitsInRow.get (rowNo) + overflow);
		}
		
		log.exiting (CombatProcessingImpl.class.getName (), "moveUnitsInOverfullRowsBackwards", unitsInRow.size ());
	}

	/**
	 * Move units forwards if there's too many units in any row
	 * So e.g. want to go from 4, 4, 6, 3 to 4, 5, 5, 3
	 * 
	 * @param unitsInRow The number of units to arrange into each row of combat setup
	 * @param maxUnitsInRow Maximum number of units we can fit on each row
	 * @throws MomException If there's not enough space to fit all the units
	 */
	final void moveUnitsInOverfullRowsForwards (final List<Integer> unitsInRow, final List<Integer> maxUnitsInRow) throws MomException
	{
		log.entering (CombatProcessingImpl.class.getName (), "moveUnitsInOverfullRowsForwards", new Integer [] {unitsInRow.size (), maxUnitsInRow.size ()});
		
		int overflow = 0;
		for (int rowNo = unitsInRow.size () - 1; rowNo >= 0; rowNo--)
		{
			// Any spare space in this row?
			if ((overflow > 0) && (unitsInRow.get (rowNo) < maxUnitsInRow.get (rowNo)))
			{
				final int unitsToBump = Math.min (overflow, maxUnitsInRow.get (rowNo) - unitsInRow.get (rowNo));
				overflow = overflow - unitsToBump;
				unitsInRow.set (rowNo, unitsInRow.get (rowNo) + unitsToBump);
			}
			
			// Any that don't fit in this row?
			if (unitsInRow.get (rowNo) > maxUnitsInRow.get (rowNo))
			{
				overflow = overflow + (unitsInRow.get (rowNo) - maxUnitsInRow.get (rowNo));
				unitsInRow.set (rowNo, maxUnitsInRow.get (rowNo));
			}
		}
		
		// If we've *still* got an overflow then we've got a problem because there's literally nowhere left to fit the units
		if (overflow > 0)
			throw new MomException ("moveUnitsInOverfullRowsForwards: Not enough room to position all units in combat - " + overflow);
		
		log.exiting (CombatProcessingImpl.class.getName (), "moveUnitsInOverfullRowsForwards", unitsInRow.size ());
	}
	
	/**
	 * Final piece of positionCombatUnits - after all the positioning logic is complete, actually places the units into combat
	 *  
	 * @param combatLocation The location the combat is taking place at (may not necessarily be the location of the defending units, see where this is set in startCombat)
	 * @param startX X coordinate within the combat map to centre the units around
	 * @param startY Y coordinate within the combat map to centre the units around
	 * @param unitHeading Heading the units should be set to face
	 * @param combatSide Side the units should be set to be on
	 * @param unitsToPosition Ordered list of the units being placed into combat
	 * @param unitsInRow Number of units to arrange into each row
	 * @param startCombatMessage Message being built up ready to send to human participants; if combat is between two AI players can pass this in as null
	 * @param attackingPlayer Player who is attacking
	 * @param defendingPlayer Player who is defending - may be null if taking an empty lair, or a "walk in without a fight" in simultaneous turns games
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @param combatMap Combat scenery we are placing the units onto (important because some tiles will be impassable to some types of unit)
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 */
	final void placeCombatUnits (final OverlandMapCoordinatesEx combatLocation, final int startX, final int startY, final int unitHeading, final UnitCombatSideID combatSide,
		final List<MemoryUnitAndCombatClass> unitsToPosition, final List<Integer> unitsInRow, final StartCombatMessage startCombatMessage,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final CoordinateSystem combatMapCoordinateSystem, final MapAreaOfCombatTiles combatMap, final ServerDatabaseEx db)
		throws RecordNotFoundException
	{
		log.entering (CombatProcessingImpl.class.getName (), "placeCombatUnits");
		
		int unitNo = 0;		// Index into unit list of unit being placed
		final CombatMapCoordinatesEx centre = new CombatMapCoordinatesEx ();
		centre.setX (startX);
		centre.setY (startY);
		
		for (final Integer unitsOnThisRow : unitsInRow)
		{
			final CombatMapCoordinatesEx coords = new CombatMapCoordinatesEx ();
			coords.setX (centre.getX ());
			coords.setY (centre.getY ());
				
			// Move down-left to start of row...
			for (int n = 0; n < unitsOnThisRow/2; n++)
				CoordinateSystemUtils.moveCoordinates (combatMapCoordinateSystem, coords, 6);
				
			// ..then position units in an up-right fashion to fill the row
			for (int n = 0; n < unitsOnThisRow; n++)
			{
				// Make sure the cell is passable
				while (getUnitCalculations ().calculateDoubleMovementToEnterCombatTile (combatMap.getRow ().get (coords.getY ()).getCell ().get (coords.getX ()), db) < 0)
					CoordinateSystemUtils.moveCoordinates (combatMapCoordinateSystem, coords, 2);
				
				// Place unit
				final MemoryUnit trueUnit = unitsToPosition.get (unitNo).getUnit ();
				
				// Update true unit on server
				final CombatMapCoordinatesEx trueUnitPosition = new CombatMapCoordinatesEx ();
				trueUnitPosition.setX (coords.getX ());
				trueUnitPosition.setY (coords.getY ());
				
				trueUnit.setCombatLocation (combatLocation);
				trueUnit.setCombatPosition (trueUnitPosition);
				trueUnit.setCombatHeading (unitHeading);
				trueUnit.setCombatSide (combatSide);
				
				// Attacking player's memory on server
				final CombatMapCoordinatesEx atkUnitPosition = new CombatMapCoordinatesEx ();
				atkUnitPosition.setX (coords.getX ());
				atkUnitPosition.setY (coords.getY ());

				final MomPersistentPlayerPrivateKnowledge atkPriv = (MomPersistentPlayerPrivateKnowledge) attackingPlayer.getPersistentPlayerPrivateKnowledge ();
				final MemoryUnit atkUnit = getUnitUtils ().findUnitURN (trueUnit.getUnitURN (), atkPriv.getFogOfWarMemory ().getUnit (), "placeCombatUnits-A");
				atkUnit.setCombatLocation (combatLocation);
				atkUnit.setCombatPosition (atkUnitPosition);
				atkUnit.setCombatHeading (unitHeading);
				atkUnit.setCombatSide (combatSide);
				
				// Defending player may be nil if we're attacking an empty lair
				if (defendingPlayer != null)
				{
					// Update player memory on server
					final CombatMapCoordinatesEx defUnitPosition = new CombatMapCoordinatesEx ();
					defUnitPosition.setX (coords.getX ());
					defUnitPosition.setY (coords.getY ());

					final MomPersistentPlayerPrivateKnowledge defPriv = (MomPersistentPlayerPrivateKnowledge) defendingPlayer.getPersistentPlayerPrivateKnowledge ();
					final MemoryUnit defUnit = getUnitUtils ().findUnitURN (trueUnit.getUnitURN (), defPriv.getFogOfWarMemory ().getUnit (), "placeCombatUnits-D");
					defUnit.setCombatLocation (combatLocation);
					defUnit.setCombatPosition (defUnitPosition);
					defUnit.setCombatHeading (unitHeading);
					defUnit.setCombatSide (combatSide);
				}
				
				// Send unit positioning to clients
				if (startCombatMessage != null)
				{
					final CombatMapCoordinatesEx msgUnitPosition = new CombatMapCoordinatesEx ();
					msgUnitPosition.setX (coords.getX ());
					msgUnitPosition.setY (coords.getY ());

					final StartCombatMessageUnit unitPlacement = new StartCombatMessageUnit ();
					unitPlacement.setUnitURN (trueUnit.getUnitURN ());
					unitPlacement.setCombatPosition (msgUnitPosition);
					unitPlacement.setCombatHeading (unitHeading);
					unitPlacement.setCombatSide (combatSide);
				
					// Include unit details if necessary
					if ((startCombatMessage.isCreateDefenders ()) && (combatSide == UnitCombatSideID.DEFENDER))
						unitPlacement.setUnitDetails (getFogOfWarDuplication ().createAddUnitMessage (trueUnit, db));
				
					startCombatMessage.getUnitPlacement ().add (unitPlacement);
				}
				
				// Move up-right to next position
				unitNo++;
				CoordinateSystemUtils.moveCoordinates (combatMapCoordinateSystem, coords, 2);
			}
				
			CoordinateSystemUtils.moveCoordinates (combatMapCoordinateSystem, centre,
				CoordinateSystemUtils.normalizeDirection (combatMapCoordinateSystem.getCoordinateSystemType (), unitHeading + 4));
		}
			
		log.exiting (CombatProcessingImpl.class.getName (), "placeCombatUnits");
	}

	
	/**
	 * Sets units into combat (e.g. sets their combatLocation to put them into combat, and sets their position, heading and side within that combat)
	 * and adds the info to the StartCombatMessage to inform the the two clients involved to do the same.
	 * One call does units on one side (attacking or defending) of the combat.
	 * 
	 * @param combatLocation The location the combat is taking place at (may not necessarily be the location of the defending units, see where this is set in startCombat)
	 * @param startCombatMessage Message being built up ready to send to human participants; if combat is between two AI players can pass this in as null
	 * @param attackingPlayer Player who is attacking
	 * @param defendingPlayer Player who is defending - may be null if taking an empty lair, or a "walk in without a fight" in simultaneous turns games
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @param currentLocation The overland map location where the units being put into combat are standing - so attackers are 1 tile away from the actual combatLocation
	 * @param startX X coordinate within the combat map to centre the units around
	 * @param startY Y coordinate within the combat map to centre the units around
	 * @param maxRows Number of rows the units need to be arranged into in order to fit
	 * @param unitHeading Heading the units should be set to face
	 * @param combatSide Side the units should be set to be on
	 * @param onlyUnitURNs If null, all units at currentLocation will be put into combat; if non-null, only the listed Unit URNs will be put into combat
	 * 	This is because the attacker may elect to not attack with every single unit in their stack
	 * @param combatMap Combat scenery we are placing the units onto (important because some tiles will be impassable to some types of unit)
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws MomException If there is a logic failure, e.g. not enough space to fit all the units
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws PlayerNotFoundException If we can't find the player who owns one of the units
	 */
	final void positionCombatUnits (final OverlandMapCoordinatesEx combatLocation, final StartCombatMessage startCombatMessage,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final CoordinateSystem combatMapCoordinateSystem,
		final OverlandMapCoordinatesEx currentLocation, final int startX, final int startY, final int maxRows, final int unitHeading,
		final UnitCombatSideID combatSide, final List<Integer> onlyUnitURNs, final MapAreaOfCombatTiles combatMap, final MomSessionVariables mom)
		throws MomException, RecordNotFoundException, PlayerNotFoundException
	{
		log.entering (CombatProcessingImpl.class.getName (), "positionCombatUnits",
			new String [] {currentLocation.toString (), new Integer (startX).toString (), new Integer (startY).toString (), new Integer (maxRows).toString (),
			new Integer (unitHeading).toString (), combatSide.toString (), (onlyUnitURNs == null) ? "All" : new Integer (onlyUnitURNs.size ()).toString ()});
		
		// First check for obstructions, to work out the maximum number of units we can fit on each row
		final List<Integer> maxUnitsInRow = determineMaxUnitsInRow (startX, startY, unitHeading, maxRows,
			combatMapCoordinateSystem, combatMap, mom.getServerDB ());
		
		// Make a list of all the units we need to position - attackers may not have selected entire stack to attack with
		final List<MemoryUnitAndCombatClass> unitsToPosition = new ArrayList<MemoryUnitAndCombatClass> ();
		for (final MemoryUnit tu : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
			if ((currentLocation.equals (tu.getUnitLocation ())) && (tu.getStatus () == UnitStatusID.ALIVE) &&
				((onlyUnitURNs == null) || (onlyUnitURNs.contains (tu.getUnitURN ()))))
			{
				// Give unit full ammo and mana
				// Have to do this now, since sorting the units relies on knowing what ranged attacks they have
				getUnitCalculations ().giveUnitFullRangedAmmoAndMana (tu, mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
					mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), mom.getServerDB ());
				
				// Add it to the list
				unitsToPosition.add (new MemoryUnitAndCombatClass (tu, calculateUnitCombatClass (tu, mom.getPlayers (),
					mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), mom.getServerDB ())));
			}
		
		// Sort the units by their "combat class"; this sorts in the order: Melee heroes, melee units, ranged heroes, ranged units, settlers
		Collections.sort (unitsToPosition);
		
		// Create a list of how many of each combat class there is, e.g. may have 4 MH, 2 MU, 2 RH, 6 RU, 3 ST = 17 units
		// This is our ideal row placement
		final List<Integer> unitsInRow = listNumberOfEachCombatClass (unitsToPosition);
		
		// Shuffle rows around to ensure we end up meeting maxRows and maxUnitsInRow limitations
		mergeRowsIfTooMany (unitsInRow, maxRows);
		moveUnitsInOverfullRowsBackwards (unitsInRow, maxUnitsInRow);
		moveUnitsInOverfullRowsForwards (unitsInRow, maxUnitsInRow);
		
		// Now can actually place the units
		placeCombatUnits (combatLocation, startX, startY, unitHeading, combatSide, unitsToPosition, unitsInRow, startCombatMessage,
			attackingPlayer, defendingPlayer, combatMapCoordinateSystem, combatMap, mom.getServerDB ());

		log.exiting (CombatProcessingImpl.class.getName (), "positionCombatUnits");
	}
	
	/**
	 * Handles tidying up when a combat ends
	 * 
	 * If the combat results in the attacker capturing a city, and the attacker is a human player, then this gets called twice - the first time it
	 * will spot that CaptureCityDecision = cdcUndecided, send a message to the player to ask them for the decision, and when we get an answer back, it'll be called again
	 * 
	 * @param combatLocation The location the combat is taking place at (may not necessarily be the location of the defending units, see where this is set in startCombat)
	 * @param attackingPlayer Player who is attacking
	 * @param defendingPlayer Player who is defending - may be null if taking an empty lair, or a "walk in without a fight" in simultaneous turns games
	 * @param winningPlayer Player who won
	 * @param captureCityDecision If taken a city and winner has decided whether to raze or capture it then is passed in here; null = player hasn't decided yet (see comment above)
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public final void combatEnded (final OverlandMapCoordinatesEx combatLocation,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final PlayerServerDetails winningPlayer,
		final CaptureCityDecisionID captureCityDecision, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.entering (CombatProcessingImpl.class.getName (), "combatEnded", combatLocation);
		
		final ServerGridCell tc = (ServerGridCell) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(combatLocation.getPlane ()).getRow ().get (combatLocation.getY ()).getCell ().get (combatLocation.getX ());
		
		// If we're walking into a city that we don't already own (its possible we're moving into our own city if this is a "walk in without a fight")
		// then don't end the combat just yet - first ask the winner whether they want to capture or raze the city
		if ((winningPlayer == attackingPlayer) && (captureCityDecision == null) && (tc.getCityData () != null) &&
			(tc.getCityData ().getCityPopulation () != null) && (tc.getCityData ().getCityPopulation () > 0) &&
			(!attackingPlayer.getPlayerDescription ().getPlayerID ().equals (tc.getCityData ().getCityOwnerID ())))
		{
			log.finest ("Undecided city capture, bulk of method will not run");
			
			final AskForCaptureCityDecisionMessage msg = new AskForCaptureCityDecisionMessage ();
			msg.setCombatLocation (combatLocation);
			if (defendingPlayer != null)
				msg.setDefendingPlayerID (defendingPlayer.getPlayerDescription ().getPlayerID ());
			
			attackingPlayer.getConnection ().sendMessageToClient (msg);
		}
		else
		{
			// Build the bulk of the CombatEnded message
			final CombatEndedMessage msg = new CombatEndedMessage ();
			msg.setCombatLocation (combatLocation);
			msg.setWinningPlayerID (winningPlayer.getPlayerDescription ().getPlayerID ());
			msg.setCaptureCityDecisionID (captureCityDecision);
			msg.setScheduledCombatURN (tc.getScheduledCombatURN ());
			
			// Deal with the attacking player swiping gold from a city they just took - we do this first so we can send it with the CombatEnded message
			final MomPersistentPlayerPrivateKnowledge atkPriv = (MomPersistentPlayerPrivateKnowledge) attackingPlayer.getPersistentPlayerPrivateKnowledge ();
			if ((captureCityDecision != null) && (defendingPlayer != null))
			{
				// Calc as a long since the the multiplication could give a really big number
				final MomPersistentPlayerPrivateKnowledge defPriv = (MomPersistentPlayerPrivateKnowledge) defendingPlayer.getPersistentPlayerPrivateKnowledge ();
				
				final long cityPopulation = tc.getCityData ().getCityPopulation ();
				final long totalGold = getResourceValueUtils ().findAmountStoredForProductionType (defPriv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD);
				final long totalPopulation = getOverlandMapServerUtils ().totalPlayerPopulation
					(mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), defendingPlayer.getPlayerDescription ().getPlayerID (),
					mom.getSessionDescription ().getMapSize (), mom.getServerDB ());
				final long goldSwiped = (totalGold * cityPopulation) / totalPopulation;
				msg.setGoldSwiped ((int) goldSwiped);
				
				// Any gold from razing buildings?
				final int goldFromRazing; 
				if (captureCityDecision == CaptureCityDecisionID.RAZE)
				{
					goldFromRazing = getCityServerUtils ().totalCostOfBuildingsAtLocation (combatLocation,
						mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (), mom.getServerDB ()) / 10;
					msg.setGoldFromRazing (goldFromRazing);
				}
				else
					goldFromRazing = 0;
				
				// Swipe it - the updated values will be sent to the players below
				getResourceValueUtils ().addToAmountStored (defPriv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, (int) -goldSwiped);
				getResourceValueUtils ().addToAmountStored (atkPriv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, (int) goldSwiped + goldFromRazing);
			}
			
			// Send the CombatEnded message
			// Remember defending player may still be nil if we attacked an empty lair
			if ((defendingPlayer != null) && (defendingPlayer.getPlayerDescription ().isHuman ()))
				defendingPlayer.getConnection ().sendMessageToClient (msg);
			
			if (attackingPlayer.getPlayerDescription ().isHuman ())
				attackingPlayer.getConnection ().sendMessageToClient (msg);
			
			// Cancel any combat unit enchantments/curses that were cast in combat
			getFogOfWarMidTurnChanges ().switchOffMaintainedSpellsCastOnUnitsInCombat_OnServerAndClients
				(mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), combatLocation, attackingPlayer, defendingPlayer, mom.getServerDB (), mom.getSessionDescription ());
			
			// Kill off dead units from the combat and remove any combat summons like Phantom Warriors
			// This also removes ('kills') on the client monsters in a lair/node/tower who won
			// Have to do this before we advance the attacker, otherwise we end up trying to advance the combat summoned units
			purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer,
				mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());
			
			// If we cleared out a node/lair/tower, then the player who cleared it now knows it to be empty
			// Client knows to do this themselves because they won
			if ((winningPlayer == attackingPlayer) && (ServerMemoryGridCellUtils.isNodeLairTower (tc.getTerrainData (), mom.getServerDB ())))
			{
				log.finest ("Attacking player " + attackingPlayer.getPlayerDescription ().getPlayerName () + " now knows the node/lair/tower to be empty");
				final MomPersistentPlayerPrivateKnowledge atkPub = (MomPersistentPlayerPrivateKnowledge) attackingPlayer.getPersistentPlayerPrivateKnowledge ();
				atkPub.getNodeLairTowerKnownUnitIDs ().getPlane ().get (combatLocation.getPlane ()).getRow ().get (combatLocation.getY ()).getCell ().set (combatLocation.getX (), "");
			}
			
			// If the attacker won then advance their units to the target square
			if (winningPlayer == attackingPlayer)
			{
				log.finest ("Attacker won");
				
				// Work out moveToPlane - If attackers are capturing a tower from Myrror, in which case they jump to Arcanus as part of the move
				final OverlandMapCoordinatesEx moveTo = new OverlandMapCoordinatesEx ();
				moveTo.setX (combatLocation.getX ());
				moveTo.setY (combatLocation.getY ());
				if (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (tc.getTerrainData ()))
				{
					moveTo.setPlane (0);
				}
				else
					moveTo.setPlane (combatLocation.getPlane ());
				
				// Put all the attackers in a list, and figure out moveFrom
				final OverlandMapCoordinatesEx moveFrom = new OverlandMapCoordinatesEx ();
				final List<MemoryUnit> unitStack = new ArrayList<MemoryUnit> ();
				for (final MemoryUnit trueUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
					if ((trueUnit.getStatus () == UnitStatusID.ALIVE) && (combatLocation.equals (trueUnit.getCombatLocation ())) &&
						(trueUnit.getCombatSide () == UnitCombatSideID.ATTACKER))
					{
						unitStack.add (trueUnit);
						moveFrom.setX (trueUnit.getUnitLocation ().getX ());
						moveFrom.setY (trueUnit.getUnitLocation ().getY ());
						moveFrom.setPlane (trueUnit.getUnitLocation ().getPlane ());
					}

				getFogOfWarMidTurnChanges ().moveUnitStackOneCellOnServerAndClients (unitStack, attackingPlayer,
					moveFrom, moveTo, mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getSessionDescription (), mom.getServerDB ());
				
				// If we captured a monster lair, temple, etc. then remove it from the map (don't removed nodes or towers of course)
				if ((tc.getTerrainData ().getMapFeatureID () != null) &&
					(mom.getServerDB ().findMapFeature (tc.getTerrainData ().getMapFeatureID (), "combatEnded").getMapFeatureMagicRealm ().size () > 0) &&
					(!getMemoryGridCellUtils ().isTerrainTowerOfWizardry (tc.getTerrainData ())))
				{
					log.finest ("Removing lair");
					tc.getTerrainData ().setMapFeatureID (null);
					getFogOfWarMidTurnChanges ().updatePlayerMemoryOfTerrain (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
						mom.getPlayers (), combatLocation, mom.getSessionDescription ().getFogOfWarSetting ().getTerrainAndNodeAuras ());
				}					
				
				// If we captured a tower of wizardry, then turn the light on
				else if (CommonDatabaseConstants.VALUE_FEATURE_UNCLEARED_TOWER_OF_WIZARDRY.equals (tc.getTerrainData ().getMapFeatureID ()))
				{
					log.finest ("Turning light on in tower");
					for (final Plane plane : mom.getServerDB ().getPlane ())
					{
						final OverlandMapCoordinatesEx towerCoords = new OverlandMapCoordinatesEx ();
						towerCoords.setX (combatLocation.getX ());
						towerCoords.setY (combatLocation.getY ());
						towerCoords.setPlane (plane.getPlaneNumber ());
						
						mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get (combatLocation.getPlane ()).getRow ().get (combatLocation.getY ()).getCell ().get
							(combatLocation.getX ()).getTerrainData ().setMapFeatureID (CommonDatabaseConstants.VALUE_FEATURE_CLEARED_TOWER_OF_WIZARDRY);
						getFogOfWarMidTurnChanges ().updatePlayerMemoryOfTerrain (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
							mom.getPlayers (), towerCoords, mom.getSessionDescription ().getFogOfWarSetting ().getTerrainAndNodeAuras ());
					}
				}
				
				// Deal with cities
				if (captureCityDecision == CaptureCityDecisionID.CAPTURE)
				{
					// Destroy enemy wizards' fortress and/or summoning circle
					if (getMemoryBuildingUtils ().findBuilding (mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (), combatLocation, CommonDatabaseConstants.VALUE_BUILDING_FORTRESS))
						getFogOfWarMidTurnChanges ().destroyBuildingOnServerAndClients (mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), combatLocation,
							CommonDatabaseConstants.VALUE_BUILDING_FORTRESS, false, mom.getSessionDescription (), mom.getServerDB ());

					if (getMemoryBuildingUtils ().findBuilding (mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (), combatLocation, CommonDatabaseConstants.VALUE_BUILDING_SUMMONING_CIRCLE))
						getFogOfWarMidTurnChanges ().destroyBuildingOnServerAndClients (mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), combatLocation,
							CommonDatabaseConstants.VALUE_BUILDING_SUMMONING_CIRCLE, false, mom.getSessionDescription (), mom.getServerDB ());
				}
				else if (captureCityDecision == CaptureCityDecisionID.RAZE)
				{
					// Deal with spells cast on the city:
					// 1) Any spells the defender had cast on the city must be enchantments - which unfortunately we don't get - so cancel these
					// 2) Any spells the attacker had cast on the city must be curses - we don't want to curse our own city - so cancel them
					// 3) Any spells a 3rd player (neither the defender nor attacker) had cast on the city must be curses - and I'm sure they'd like to continue cursing the new city owner :)
					getFogOfWarMidTurnChanges ().switchOffMaintainedSpellsInLocationOnServerAndClients
						(mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), combatLocation, combatLocation,
						attackingPlayer, defendingPlayer, attackingPlayer.getPlayerDescription ().getPlayerID (), mom.getServerDB (), mom.getSessionDescription ());
					
					if (defendingPlayer != null)
						getFogOfWarMidTurnChanges ().switchOffMaintainedSpellsInLocationOnServerAndClients
							(mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), combatLocation, combatLocation,
							attackingPlayer, defendingPlayer, defendingPlayer.getPlayerDescription ().getPlayerID (), mom.getServerDB (), mom.getSessionDescription ());
					
					// Take ownership of the city
					tc.getCityData ().setCityOwnerID (attackingPlayer.getPlayerDescription ().getPlayerID ());
					
					// Although farmers will be the same, capturing player may have a different tax rate or different units stationed here so recalc rebels
					tc.getCityData ().setNumberOfRebels (getCityCalculations ().calculateCityRebels
						(mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
						mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (),
						combatLocation, atkPriv.getTaxRateID (), mom.getServerDB ()).getFinalTotal ());
					
					getServerCityCalculations ().ensureNotTooManyOptionalFarmers (tc.getCityData ());
					
					getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
						mom.getPlayers (), combatLocation, mom.getSessionDescription ().getFogOfWarSetting (), false);
				}
			}
			else
			{
				log.finest ("Defender won");
				
				// Cancel all spells cast on the city regardless of owner
				getFogOfWarMidTurnChanges ().switchOffMaintainedSpellsInLocationOnServerAndClients (mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (),
					combatLocation, combatLocation, attackingPlayer, defendingPlayer, 0, mom.getServerDB (), mom.getSessionDescription ());
				
				// Wreck all the buildings
				getFogOfWarMidTurnChanges ().destroyAllBuildingsInLocationOnServerAndClients (mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (),
					combatLocation, mom.getSessionDescription (), mom.getServerDB ());
				
				// Wreck the city
				tc.setCityData (null);
				getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getPlayers (),
					combatLocation, mom.getSessionDescription ().getFogOfWarSetting (), false);
			}
			
			// Set all units CombatX, CombatY back to -1, -1 so we don't think they're in combat anymore.
			// Have to do this after we advance the attackers, otherwise the attackers' CombatX, CombatY will already
			// be -1, -1 and we'll no longer be able to tell who was part of the attacking force and who wasn't.
			// Have to do this before we recalc FOW, since if one side was wiped out, the FOW update may delete their memory of the opponent... which
			// then crashes the client if we then try to send msgs to take those opposing units out of combat.
			log.finest ("Removing units out of combat");
			removeUnitsFromCombat (attackingPlayer, defendingPlayer, mom.getGeneralServerKnowledge ().getTrueMap (), combatLocation, mom.getServerDB ());
			
			// Even if one side won the combat, they might have lost their unit with the longest scouting range
			// So easiest to just recalculate FOW for both players
			if (defendingPlayer != null)
				getFogOfWarProcessing ().updateAndSendFogOfWar (mom.getGeneralServerKnowledge ().getTrueMap (),
					defendingPlayer, mom.getPlayers (), false, "combatEnded-D", mom.getSessionDescription (), mom.getServerDB ());
			
			// If attacker won, we'll have already recalc'd their FOW when their units advanced 1 square
			// But lets do it anyway - maybe they captured a city with an Oracle
			getFogOfWarProcessing ().updateAndSendFogOfWar (mom.getGeneralServerKnowledge ().getTrueMap (),
				attackingPlayer, mom.getPlayers (), false, "combatEnded-A", mom.getSessionDescription (), mom.getServerDB ());
			
			// Remove all combat area effects from spells like Prayer, Mass Invisibility, etc.
			log.finest ("Removing all spell CAEs");
			getFogOfWarMidTurnChanges ().removeCombatAreaEffectsFromLocalisedSpells
				(mom.getGeneralServerKnowledge ().getTrueMap (), combatLocation, mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ());
			
			// Handle clearing up and removing the scheduled combat
			if (tc.getScheduledCombatURN () != null)
			{
				log.finest ("Tidying up scheduled combat");
				throw new UnsupportedOperationException ("combatEnded doesn't know what to do with scheduled combats yet");
			}
			
			// Assuming both sides may have taken losses, could have gained/lost a city, etc. etc., best to just recalculate production for both
			// DefendingPlayer may still be nil
			getServerResourceCalculations ().recalculateGlobalProductionValues (attackingPlayer.getPlayerDescription ().getPlayerID (), false, mom);
			
			if (defendingPlayer != null)
				getServerResourceCalculations ().recalculateGlobalProductionValues (defendingPlayer.getPlayerDescription ().getPlayerID (), false, mom);
		
			if (mom.getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS)
			{
				// If this is a simultaneous turns game, we need to inform all players that these two players are no longer in combat
				getCombatScheduler ().informClientsOfPlayerBusyInCombat (attackingPlayer, mom.getPlayers (), false);
				
				if (defendingPlayer != null)
					getCombatScheduler ().informClientsOfPlayerBusyInCombat (defendingPlayer, mom.getPlayers (), false);
			}
			else
			{
				// If this is a one-at-a-time turns game, we need to tell the player to make their next move
				// There's no harm in sending this to a player whose turn it isn't, the client will perform no effect in that case
				final SelectNextUnitToMoveOverlandMessage nextUnitMsg = new SelectNextUnitToMoveOverlandMessage ();
				if (attackingPlayer.getPlayerDescription ().isHuman ())
					attackingPlayer.getConnection ().sendMessageToClient (nextUnitMsg);

				if ((defendingPlayer != null) && (defendingPlayer.getPlayerDescription ().isHuman ()))
					defendingPlayer.getConnection ().sendMessageToClient (nextUnitMsg);
			}
		}
		
		log.exiting (CombatProcessingImpl.class.getName (), "combatEnded");
	}
	
	/**
	 * Regular units who die in combat are only set to status=DEAD - they are not actually freed immediately in case someone wants to cast Animate Dead on them.
	 * After a combat ends, this routine properly frees them both on the server and all clients.
	 * 
	 * Heroes who die in combat are set to musDead on the server and the client who owns them - they're freed immediately on the other clients.
	 * This means we don't have to touch dead heroes here, just leave them as they are.
	 * 
	 * It also removes combat summons, e.g. Phantom Warriors, even if they are not dead.
	 * 
	 * It also removes monsters left alive guarding nodes/lairs/towers from the client (leaving them on the server) - these only ever exist
	 * temporarily on the client who is attacking, and the "knows about unit" routine the FOW is based on always returns False for them,
	 * so we have handle them as a special case here.
	 * 
	 * @param combatLocation The location the combat is taking place at (may not necessarily be the location of the defending units, see where this is set in startCombat)
	 * @param attackingPlayer Player who is attacking
	 * @param defendingPlayer Player who is defending - may be null if taking an empty lair, or a "walk in without a fight" in simultaneous turns games
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param players List of players in the session
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	final void purgeDeadUnitsAndCombatSummonsFromCombat (final OverlandMapCoordinatesEx combatLocation,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final FogOfWarMemory trueMap,
		final List<PlayerServerDetails> players, final MomSessionDescription sd, final ServerDatabaseEx db)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException
	{
		log.entering (CombatProcessingImpl.class.getName (), "purgeDeadUnitsAndCombatSummonsFromCombat", combatLocation);
		
		final MemoryGridCell tc = trueMap.getMap ().getPlane ().get (combatLocation.getPlane ()).getRow ().get (combatLocation.getY ()).getCell ().get (combatLocation.getX ());
		
		// Is this someone attacking a node/lair/tower?
		// If DefendingPlayer is nil (we wiped out the monsters), there'll be no monsters to remove, so in which case we don't care that we get this value wrong
		final MomPersistentPlayerPublicKnowledge defPub = (defendingPlayer == null) ? null : (MomPersistentPlayerPublicKnowledge) defendingPlayer.getPersistentPlayerPublicKnowledge ();
		final boolean attackingNodeLairTower = (defPub != null) && (CommonDatabaseConstants.WIZARD_ID_MONSTERS.equals (defPub.getWizardID ())) &&
			ServerMemoryGridCellUtils.isNodeLairTower (tc.getTerrainData (), db);
		
		// Then check all the units
		// Had better copy the units list since we'll be removing units from it as we go along
		int deadCount = 0;
		int summonedCount = 0;
		int monstersCount = 0;
		
		final List<MemoryUnit> copyOfTrueUnits = new ArrayList<MemoryUnit> ();
		copyOfTrueUnits.addAll (trueMap.getUnit ());
		for (final MemoryUnit trueUnit : copyOfTrueUnits)
			if (combatLocation.equals (trueUnit.getCombatLocation ()))
			{
				// Permanently remove any dead regular units (which were kept around until now so they could be Animate Dead'ed)
				// Also remove any combat summons like Phantom Warriors
				final boolean manuallyTellAttackerClientToKill;
				final boolean manuallyTellDefenderClientToKill;
				if ((trueUnit.isWasSummonedInCombat ()) || ((trueUnit.getStatus () == UnitStatusID.DEAD) &&
					(!db.findUnit (trueUnit.getUnitID (), "purgeDeadUnitsAndCombatSummonsFromCombat").getUnitMagicRealm ().equals (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))))
				{
					if (trueUnit.getStatus () == UnitStatusID.DEAD)
						deadCount++;
					else
						summonedCount++;
					
					// The kill routine below will free off dead units on the server fine, but never instruct the client to do the same,
					// since the "knows about unit" routine always returns false for dead units
					
					// Players not involved in the combat would have freed the units straight away from the
					// ApplyDamage message, so this only affects the attacking & defending players
					
					// Heroes are fine - for the player who owns the hero, we leave them as dead so they can be later resurrected;
					// the enemy of the hero would have freed them from ApplyDamage
					manuallyTellAttackerClientToKill = (trueUnit.getStatus () == UnitStatusID.DEAD);
					manuallyTellDefenderClientToKill = (manuallyTellAttackerClientToKill) && (defendingPlayer != null);
					
					if (manuallyTellAttackerClientToKill)
						log.finest ("purgeDeadUnitsAndCombatSummonsFromCombat: Telling attacker to remove dead unit URN " + trueUnit.getUnitURN ());
					if (manuallyTellDefenderClientToKill)
						log.finest ("purgeDeadUnitsAndCombatSummonsFromCombat: Telling defender to remove dead unit URN " + trueUnit.getUnitURN ());
					
					// Use regular kill routine
					getFogOfWarMidTurnChanges ().killUnitOnServerAndClients (trueUnit, KillUnitActionID.FREE, trueMap, players, sd, db);
				}
				else
				{
					// Even though we don't remove them on the server, tell the client to remove monsters left alive guarding nodes/lairs/towers
					manuallyTellAttackerClientToKill = (attackingNodeLairTower) && (defendingPlayer != null) &&
						(trueUnit.getOwningPlayerID () == defendingPlayer.getPlayerDescription ().getPlayerID ());
					
					// But make sure we don't remove monsters from the monster player's memory, since they know about their own units!
					manuallyTellDefenderClientToKill = false;
					if (manuallyTellAttackerClientToKill)
					{
						log.finest ("purgeDeadUnitsAndCombatSummonsFromCombat: Telling attacking player to remove NLT monster with unit URN " + trueUnit.getUnitURN ());
						monstersCount++;
					}
				}
				
				// Special case where we have to tell the client to kill off the unit outside of the FOW routines?
				if ((manuallyTellAttackerClientToKill) || (manuallyTellDefenderClientToKill))
				{
					final KillUnitMessageData manualKillMessageData = new KillUnitMessageData ();
					manualKillMessageData.setUnitURN (trueUnit.getUnitURN ());
					manualKillMessageData.setKillUnitActionID (KillUnitActionID.FREE);
				
					final KillUnitMessage manualKillMessage = new KillUnitMessage ();
					manualKillMessage.setData (manualKillMessageData);

					// Defender
					if (manuallyTellDefenderClientToKill)
					{
						final MomPersistentPlayerPrivateKnowledge defPriv = (MomPersistentPlayerPrivateKnowledge) defendingPlayer.getPersistentPlayerPrivateKnowledge ();
						getUnitUtils ().removeUnitURN (trueUnit.getUnitURN (), defPriv.getFogOfWarMemory ().getUnit ());

						if (defendingPlayer.getPlayerDescription ().isHuman ())
							defendingPlayer.getConnection ().sendMessageToClient (manualKillMessage);
					}
					
					// Attacker
					if (manuallyTellAttackerClientToKill)
					{
						final MomPersistentPlayerPrivateKnowledge atkPriv = (MomPersistentPlayerPrivateKnowledge) attackingPlayer.getPersistentPlayerPrivateKnowledge ();
						getUnitUtils ().removeUnitURN (trueUnit.getUnitURN (), atkPriv.getFogOfWarMemory ().getUnit ());
					
						if (attackingPlayer.getPlayerDescription ().isHuman ())
							attackingPlayer.getConnection ().sendMessageToClient (manualKillMessage);
					}
				}
			}
		
		log.finest ("purgeDeadUnitsAndCombatSummonsFromCombat permanently freed " +
			deadCount + " dead units and " + summonedCount + " summons, and told attacking client to free " + monstersCount + "monster defenders who''re still alive");
		log.exiting (CombatProcessingImpl.class.getName (), "purgeDeadUnitsAndCombatSummonsFromCombat");
	}

	/**
	 * Units are put into combat initially as part of the big StartCombat message rather than here.
	 * This routine is used a) for units added after a combat starts (e.g. summoning fire elementals or phantom warriors) and
	 * b) taking units out of combat when it ends.
	 * 
	 * Logic for taking units out of combat at the end very much mirrors what's in purgeDeadUnitsAndCombatSummonsFromCombat, e.g. don't
	 * try to take a monster in a lair out of combat in player's memory on server or client because purgeDeadUnitsAndCombatSummonsFromCombat will
	 * already have killed it off.
	 * 
	 * @param attackingPlayer Player who is attacking
	 * @param defendingPlayer Player who is defending - may be null if taking an empty lair, or a "walk in without a fight" in simultaneous turns games
	 * @param trueTerrain True overland terrain
	 * @param trueUnit The true unit being put into or taken out of combat
	 * @param terrainLocation The location the combat is taking place
	 * @param combatLocation For putting unit into combat, is the location the combat is taking place (i.e. = terrainLocation), for taking unit out of combat will be null
	 * @param combatPosition For putting unit into combat, is the starting position the unit is standing in on the battlefield, for taking unit out of combat will be null
	 * @param combatHeading For putting unit into combat, is the direction the the unit is heading on the battlefield, for taking unit out of combat will be null
	 * @param combatSide For putting unit into combat, specifies which side they're on, for taking unit out of combat will be null
	 * @param summonedBySpellID For summoning new units directly into combat (e.g. fire elementals) gives the spellID they were summoned with; otherwise null
	 * @param db Lookup lists built over the XML database
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 */
	final void setUnitIntoOrTakeUnitOutOfCombat (final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final MapVolumeOfMemoryGridCells trueTerrain,
		final MemoryUnit trueUnit, final OverlandMapCoordinatesEx terrainLocation, final OverlandMapCoordinatesEx combatLocation, final CombatMapCoordinatesEx combatPosition,
		final Integer combatHeading, final UnitCombatSideID combatSide, final String summonedBySpellID, final ServerDatabaseEx db)
		throws JAXBException, XMLStreamException, RecordNotFoundException
	{
		log.entering (CombatProcessingImpl.class.getName (), "setUnitIntoOrTakeUnitOutOfCombat", new String [] {new Integer (trueUnit.getUnitURN ()).toString (),
			(combatSide == null) ? "Side null" : combatSide.toString (), (combatLocation == null) ? "Loc null" : combatLocation.toString ()});

		final MemoryGridCell tc = trueTerrain.getPlane ().get (terrainLocation.getPlane ()).getRow ().get (terrainLocation.getY ()).getCell ().get (terrainLocation.getX ());
		
		// Is this someone attacking a node/lair/tower, and the combat is ending?
		// If DefendingPlayer is nil (we wiped out the monsters), there'll be no monsters to remove, so in which case we don't care that we get this value wrong
		final MomPersistentPlayerPublicKnowledge defPub = (defendingPlayer == null) ? null : (MomPersistentPlayerPublicKnowledge) defendingPlayer.getPersistentPlayerPublicKnowledge ();
		final boolean attackingNodeLairTower = (defPub != null) && (combatLocation == null) && (CommonDatabaseConstants.WIZARD_ID_MONSTERS.equals (defPub.getWizardID ())) &&
			ServerMemoryGridCellUtils.isNodeLairTower (tc.getTerrainData (), db);

		// Update true unit on server
		trueUnit.setCombatLocation (combatLocation);
		trueUnit.setCombatPosition (combatPosition);
		trueUnit.setCombatHeading (combatHeading);
		trueUnit.setCombatSide (combatSide);
		
		// Build message
		final SetUnitIntoOrTakeUnitOutOfCombatMessage msg = new SetUnitIntoOrTakeUnitOutOfCombatMessage ();
		msg.setUnitURN (trueUnit.getUnitURN ());
		msg.setCombatLocation (combatLocation);
		msg.setCombatPosition (combatPosition);
		msg.setCombatHeading (combatHeading);
		msg.setCombatSide (combatSide);
		msg.setSummonedBySpellID (summonedBySpellID);
		
		// Defending player may still be nil if we're attacking an empty lair
		if (defendingPlayer != null)
		{
			// Update player memory on server
			final MomPersistentPlayerPrivateKnowledge defPriv = (MomPersistentPlayerPrivateKnowledge) defendingPlayer.getPersistentPlayerPrivateKnowledge ();
			final MemoryUnit defUnit = getUnitUtils ().findUnitURN (trueUnit.getUnitURN (), defPriv.getFogOfWarMemory ().getUnit (), "setUnitIntoOrTakeUnitOutOfCombat-D");
			defUnit.setCombatLocation (combatLocation);
			defUnit.setCombatPosition (combatPosition);
			defUnit.setCombatHeading (combatHeading);
			defUnit.setCombatSide (combatSide);
			
			// Update on client
			if (defendingPlayer.getPlayerDescription ().isHuman ())
			{
				log.finest ("setUnitIntoOrTakeUnitOutOfCombat sending change in URN " + trueUnit.getUnitURN () + " to defender's client");
				defendingPlayer.getConnection ().sendMessageToClient (msg);
			}
		}

		// The exception here is we're attacking a node/lair/tower, and we lost.
		// In this case there's still monsters left alive - but by this stage we've already killed them off on the client, so unless we
		// specifically check for this, we end up telling the client to remove these 'dead' monsters from combat... and the client goes splat.
		// This has to match the condition in purgeDeadUnitsAndCombatSummonsFromCombat below, i.e. if purgeDeadUnitsAndCombatSummonsFromCombat
		// told the client to kill off the units, then don't try to take them out of combat here.
		final boolean manuallyTellAttackerClientToKill = (attackingNodeLairTower) && (defendingPlayer != null) &&
			(trueUnit.getOwningPlayerID () == defendingPlayer.getPlayerDescription ().getPlayerID ());
		if (!manuallyTellAttackerClientToKill)
		{
			// Update player memory on server
			final MomPersistentPlayerPrivateKnowledge atkPriv = (MomPersistentPlayerPrivateKnowledge) attackingPlayer.getPersistentPlayerPrivateKnowledge ();
			final MemoryUnit atkUnit = getUnitUtils ().findUnitURN (trueUnit.getUnitURN (), atkPriv.getFogOfWarMemory ().getUnit (), "setUnitIntoOrTakeUnitOutOfCombat-A");
			atkUnit.setCombatLocation (combatLocation);
			atkUnit.setCombatPosition (combatPosition);
			atkUnit.setCombatHeading (combatHeading);
			atkUnit.setCombatSide (combatSide);

			// Update on client
			if (attackingPlayer.getPlayerDescription ().isHuman ())
			{
				log.finest ("setUnitIntoOrTakeUnitOutOfCombat sending change in URN " + trueUnit.getUnitURN () + " to attacker's client");
				attackingPlayer.getConnection ().sendMessageToClient (msg);
			}
		}
		
		log.exiting (CombatProcessingImpl.class.getName (), "setUnitIntoOrTakeUnitOutOfCombat");
	}
	
	/**
	 * At the end of a combat, takes all units out of combat by nulling out all their combat values
	 * 
	 * @param attackingPlayer Player who is attacking
	 * @param defendingPlayer Player who is defending - may be null if taking an empty lair, or a "walk in without a fight" in simultaneous turns games
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param combatLocation The location the combat took place
	 * @param db Lookup lists built over the XML database
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 */
	final void removeUnitsFromCombat (final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final FogOfWarMemory trueMap,
		final OverlandMapCoordinatesEx combatLocation, final ServerDatabaseEx db)
		throws JAXBException, XMLStreamException, RecordNotFoundException
	{
		log.entering (CombatProcessingImpl.class.getName (), "removeUnitsFromCombat", combatLocation);
		
		for (final MemoryUnit trueUnit : trueMap.getUnit ())
			if (combatLocation.equals (trueUnit.getCombatLocation ()))
				setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer, trueMap.getMap (), trueUnit, combatLocation, null, null, null, null, null, db);
		
		log.exiting (CombatProcessingImpl.class.getName (), "removeUnitsFromCombat");
	}
	
	/**
	 * @return Resource value utils
	 */
	public final ResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
	}

	/**
	 * @param utils Resource value utils
	 */
	public final void setResourceValueUtils (final ResourceValueUtils utils)
	{
		resourceValueUtils = utils;
	}
	
	/**
	 * @return MemoryGridCell utils
	 */
	public final MemoryGridCellUtils getMemoryGridCellUtils ()
	{
		return memoryGridCellUtils;
	}

	/**
	 * @param utils MemoryGridCell utils
	 */
	public final void setMemoryGridCellUtils (final MemoryGridCellUtils utils)
	{
		memoryGridCellUtils = utils;
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
	 * @return Unit calculations
	 */
	public final MomUnitCalculations getUnitCalculations ()
	{
		return unitCalculations;
	}

	/**
	 * @param calc Unit calculations
	 */
	public final void setUnitCalculations (final MomUnitCalculations calc)
	{
		unitCalculations = calc;
	}
	
	/**
	 * @return City calculations
	 */
	public final MomCityCalculations getCityCalculations ()
	{
		return cityCalculations;
	}

	/**
	 * @param calc City calculations
	 */
	public final void setCityCalculations (final MomCityCalculations calc)
	{
		cityCalculations = calc;
	}

	/**
	 * @return Server-only city calculations
	 */
	public final MomServerCityCalculations getServerCityCalculations ()
	{
		return serverCityCalculations;
	}

	/**
	 * @param calc Server-only city calculations
	 */
	public final void setServerCityCalculations (final MomServerCityCalculations calc)
	{
		serverCityCalculations = calc;
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
	 * @return Main FOW update routine
	 */
	public final FogOfWarProcessing getFogOfWarProcessing ()
	{
		return fogOfWarProcessing;
	}

	/**
	 * @param obj Main FOW update routine
	 */
	public final void setFogOfWarProcessing (final FogOfWarProcessing obj)
	{
		fogOfWarProcessing = obj;
	}

	/**
	 * @return Resource calculations
	 */
	public final MomServerResourceCalculations getServerResourceCalculations ()
	{
		return serverResourceCalculations;
	}

	/**
	 * @param calc Resource calculations
	 */
	public final void setServerResourceCalculations (final MomServerResourceCalculations calc)
	{
		serverResourceCalculations = calc;
	}

	/**
	 * @return FOW duplication utils
	 */
	public final FogOfWarDuplication getFogOfWarDuplication ()
	{
		return fogOfWarDuplication;
	}

	/**
	 * @param dup FOW duplication utils
	 */
	public final void setFogOfWarDuplication (final FogOfWarDuplication dup)
	{
		fogOfWarDuplication = dup;
	}
	
	/**
	 * @return Scheduled combat utils
	 */
	public final ScheduledCombatUtils getScheduledCombatUtils ()
	{
		return scheduledCombatUtils;
	}

	/**
	 * @param utils Scheduled combat utils
	 */
	public final void setScheduledCombatUtils (final ScheduledCombatUtils utils)
	{
		scheduledCombatUtils = utils;
	}
	
	/**
	 * @return Simultaneous turns combat scheduler
	 */
	public final CombatScheduler getCombatScheduler ()
	{
		return combatScheduler;
	}

	/**
	 * @param scheduler Simultaneous turns combat scheduler
	 */
	public final void setCombatScheduler (final CombatScheduler scheduler)
	{
		combatScheduler = scheduler;
	}

	/**
	 * @return Map generator
	 */
	public final CombatMapGenerator getCombatMapGenerator ()
	{
		return combatMapGenerator;
	}

	/**
	 * @param gen Map generator
	 */
	public final void setCombatMapGenerator (final CombatMapGenerator gen)
	{
		combatMapGenerator = gen;
	}
}
