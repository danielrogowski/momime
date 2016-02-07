package momime.server.process;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.calculations.CityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitCombatSideID;
import momime.common.messages.CaptureCityDecisionID;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.PendingMovement;
import momime.common.messages.TurnSystem;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.AddUnassignedHeroItemMessage;
import momime.common.messages.servertoclient.AskForCaptureCityDecisionMessage;
import momime.common.messages.servertoclient.CombatEndedMessage;
import momime.common.messages.servertoclient.SelectNextUnitToMoveOverlandMessage;
import momime.common.messages.servertoclient.StartCombatMessage;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerCityCalculations;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.calculations.ServerUnitCalculations;
import momime.server.database.ServerDatabaseValues;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.FogOfWarMidTurnMultiChanges;
import momime.server.fogofwar.FogOfWarProcessing;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.mapgenerator.CombatMapGenerator;
import momime.server.utils.CityServerUtils;
import momime.server.utils.OverlandMapServerUtils;

/**
 * Routines dealing with starting and ending combats
 */
public final class CombatStartAndEndImpl implements CombatStartAndEnd
{
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
	private final Log log = LogFactory.getLog (CombatStartAndEndImpl.class);

	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** MemoryGridCell utils */
	private MemoryGridCellUtils memoryGridCellUtils;

	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** City calculations */
	private CityCalculations cityCalculations;
	
	/** Server-only city calculations */
	private ServerCityCalculations serverCityCalculations;
	
	/** Server-only overland map utils */
	private OverlandMapServerUtils overlandMapServerUtils;
	
	/** Server-only city utils */
	private CityServerUtils cityServerUtils;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnMultiChanges fogOfWarMidTurnMultiChanges;
	
	/** Main FOW update routine */
	private FogOfWarProcessing fogOfWarProcessing;

	/** Resource calculations */
	private ServerResourceCalculations serverResourceCalculations;

	/** Map generator */
	private CombatMapGenerator combatMapGenerator;
	
	/** Combat processing */
	private CombatProcessing combatProcessing;
	
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;
	
	/** Methods for dealing with player msgs */
	private PlayerMessageProcessing playerMessageProcessing;
	
	/** Server-only unit calculations */
	private ServerUnitCalculations serverUnitCalculations;
	
	/**
	 * Sets up a combat on the server and any client(s) who are involved
	 *
	 * @param defendingLocation Location where defending units are standing
	 * @param attackingFrom Location where attacking units are standing (which will be a map tile adjacent to defendingLocation)
	 * @param attackingUnitURNs Which of the attacker's unit stack are attacking - they might be leaving some behind; mandatory
	 * @param defendingUnitURNs Which of the defender's unit stack are defending - used for simultaneous turns games; optional, null = all units in defendingLocation
	 * @param attackerPendingMovement In simultaneous turns games, the PendingMovement the attacker made which caused the combat currently taking place at this location
	 * @param defenderPendingMovement In simultaneous turns games, the PendingMovement the defender made which caused the combat currently taking place at this location (border conflicts/counterattacks only)
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void startCombat (final MapCoordinates3DEx defendingLocation, final MapCoordinates3DEx attackingFrom,
		final List<Integer> attackingUnitURNs, final List<Integer> defendingUnitURNs,
		final PendingMovement attackerPendingMovement, final PendingMovement defenderPendingMovement, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.trace ("Entering startCombat: " + defendingLocation + ", " + attackingFrom);
		
		// If attacking a tower in Myrror, then Defending-AttackingFrom will be 0-1
		// If attacking from a tower onto Myrror, then Defending-AttackingFrom will be 1-0 - both of these should be shown on Myrror only
		// Any tower combats to/from Arcanus will be 0-0, and should appear on Arcanus only
		// Hence the reason for the Max
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (defendingLocation.getX (), defendingLocation.getY (),
			Math.max (defendingLocation.getZ (), attackingFrom.getZ ()));
		
		final ServerGridCellEx tc = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(combatLocation.getZ ()).getRow ().get (combatLocation.getY ()).getCell ().get (combatLocation.getX ());
		
		// Record the pending movement(s)
		tc.setCombatAttackerPendingMovement (attackerPendingMovement);
		tc.setCombatDefenderPendingMovement (defenderPendingMovement);
		
		// The attacker is easy to find from the first attackingUnitURN
		if ((attackingUnitURNs == null) || (attackingUnitURNs.size () == 0))
			throw new MomException ("startCombat given null or empty list of attackingUnitURNs");
		
		final MemoryUnit firstAttackingUnit = getUnitUtils ().findUnitURN (attackingUnitURNs.get (0), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), "startCombat-A");
		final PlayerServerDetails attackingPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID
			(mom.getPlayers (), firstAttackingUnit.getOwningPlayerID (), "startCombat-A");
		
		// Find out who we're attacking - if an empty city, we can get null here
		final MemoryUnit firstDefendingUnit;
		if ((defendingUnitURNs != null) && (defendingUnitURNs.size () > 0))
			firstDefendingUnit = getUnitUtils ().findUnitURN (defendingUnitURNs.get (0), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), "startCombat-D");
		else
			firstDefendingUnit = getUnitUtils ().findFirstAliveEnemyAtLocation (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (),
				defendingLocation.getX (), defendingLocation.getY (), defendingLocation.getZ (), 0);
		
		PlayerServerDetails defendingPlayer = (firstDefendingUnit == null) ? null : getMultiplayerSessionServerUtils ().findPlayerWithID
			(mom.getPlayers (), firstDefendingUnit.getOwningPlayerID (), "startCombat-D");
		
		// Start off the message to the client
		final StartCombatMessage startCombatMessage = new StartCombatMessage ();
		startCombatMessage.setCombatLocation (combatLocation);
		
		// Generate the combat scenery
		tc.setCombatMap (getCombatMapGenerator ().generateCombatMap (mom.getSessionDescription ().getCombatMapSize (),
			mom.getServerDB (), mom.getGeneralServerKnowledge ().getTrueMap (), combatLocation));
		startCombatMessage.setCombatTerrain (tc.getCombatMap ());
		
		// Set the location of both defenders and attackers.
		// Need the map generation done first, so we know where there is impassable terrain to avoid placing units on it.
		// Final 'True' parameter is because only some of the units in the attacking cell may actually be attacking, whereas everyone in the defending cell will always help defend.
		// We need to do this (at least on the server) even if we immediately end the combat below, since we need to mark the attackers into the combat so that they will advance 1 square.
		log.debug ("Positioning defenders at " + defendingLocation);
		getCombatProcessing ().positionCombatUnits (combatLocation, startCombatMessage, attackingPlayer, defendingPlayer, mom.getSessionDescription ().getCombatMapSize (), defendingLocation,
			COMBAT_SETUP_DEFENDER_FRONT_ROW_CENTRE_X, COMBAT_SETUP_DEFENDER_FRONT_ROW_CENTRE_Y, COMBAT_SETUP_DEFENDER_ROWS, COMBAT_SETUP_DEFENDER_FACING,
			UnitCombatSideID.DEFENDER, defendingUnitURNs, tc.getCombatMap (), mom);
				
		log.debug ("Positioning attackers at " + defendingLocation);
		getCombatProcessing ().positionCombatUnits (combatLocation, startCombatMessage, attackingPlayer, defendingPlayer, mom.getSessionDescription ().getCombatMapSize (), attackingFrom,
			COMBAT_SETUP_ATTACKER_FRONT_ROW_CENTRE_X, COMBAT_SETUP_ATTACKER_FRONT_ROW_CENTRE_Y, COMBAT_SETUP_ATTACKER_ROWS, COMBAT_SETUP_ATTACKER_FACING,
			UnitCombatSideID.ATTACKER, attackingUnitURNs, tc.getCombatMap (), mom);
		
		// Are there any defenders (attacking an empty city) - if not then bypass the combat entirely
		if (defendingPlayer == null)
		{
			log.debug ("Combat ending before it starts");
			
			// If attacking an enemy city with no defence, set the player correctly so we notify the player that they've lost their city.
			if (tc.getCityData () != null)
				defendingPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), tc.getCityData ().getCityOwnerID (), "startCombat-CD");
				
			combatEnded (combatLocation, attackingPlayer, defendingPlayer, attackingPlayer,	// <-- who won
				null, mom);
		}
		else
		{
			log.debug ("Continuing combat setup");
			
			// Store the two players involved
			tc.setAttackingPlayerID (attackingPlayer.getPlayerDescription ().getPlayerID ());
			tc.setDefendingPlayerID (defendingPlayer.getPlayerDescription ().getPlayerID ());
			
			// Set casting skill allocation for this combat 
			final MomPersistentPlayerPrivateKnowledge attackingPriv = (MomPersistentPlayerPrivateKnowledge) attackingPlayer.getPersistentPlayerPrivateKnowledge ();
			tc.setCombatAttackerCastingSkillRemaining (getResourceValueUtils ().calculateCastingSkillOfPlayer (attackingPriv.getResourceValue ()));
			
			final MomPersistentPlayerPrivateKnowledge defendingPriv = (MomPersistentPlayerPrivateKnowledge) defendingPlayer.getPersistentPlayerPrivateKnowledge ();
			tc.setCombatDefenderCastingSkillRemaining (getResourceValueUtils ().calculateCastingSkillOfPlayer (defendingPriv.getResourceValue ()));
			
			// Finally send the message, containing all the unit positions, units (if monsters in a node/lair/tower) and combat scenery
			if (attackingPlayer.getPlayerDescription ().isHuman ())
				attackingPlayer.getConnection ().sendMessageToClient (startCombatMessage);

			if (defendingPlayer.getPlayerDescription ().isHuman ())
				defendingPlayer.getConnection ().sendMessageToClient (startCombatMessage);
			
			// Kick off first turn of combat
			getCombatProcessing ().progressCombat (combatLocation, true, false, mom);
		}

		log.trace ("Exiting startCombat");
	}
	
	/**
	 * Handles tidying up when a combat ends
	 * 
	 * If the combat results in the attacker capturing a city, and the attacker is a human player, then this gets called twice - the first time it
	 * will spot that CaptureCityDecision = cdcUndecided, send a message to the player to ask them for the decision, and when we get an answer back, it'll be called again
	 * 
	 * @param combatLocation The location the combat is taking place at (may not necessarily be the location of the defending units, see where this is set in startCombat)
	 * @param attackingPlayer Player who is attacking
	 * @param defendingPlayer Player who is defending - there should be no situations anymore where this can be passed in as null
	 * @param winningPlayer Player who won
	 * @param captureCityDecision If taken a city and winner has decided whether to raze or capture it then is passed in here; null = player hasn't decided yet (see comment above)
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void combatEnded (final MapCoordinates3DEx combatLocation,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final PlayerServerDetails winningPlayer,
		final CaptureCityDecisionID captureCityDecision, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.trace ("Entering combatEnded: " + combatLocation);
		
		final ServerGridCellEx tc = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(combatLocation.getZ ()).getRow ().get (combatLocation.getY ()).getCell ().get (combatLocation.getX ());
		
		// If we're walking into a city that we don't already own (its possible we're moving into our own city if this is a "walk in without a fight")
		// then don't end the combat just yet - first ask the winner whether they want to capture or raze the city
		if ((winningPlayer == attackingPlayer) && (captureCityDecision == null) && (tc.getCityData () != null) &&
			(attackingPlayer.getPlayerDescription ().getPlayerID () != tc.getCityData ().getCityOwnerID ()))
		{
			log.debug ("Undecided city capture, bulk of method will not run");
			
			final AskForCaptureCityDecisionMessage msg = new AskForCaptureCityDecisionMessage ();
			msg.setCityLocation (combatLocation);
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
			msg.setHeroItemCount (tc.getItemsFromHeroesWhoDiedInCombat ().size ());
			
			// Deal with the attacking player swiping gold from a city they just took - we do this first so we can send it with the CombatEnded message
			final MomPersistentPlayerPrivateKnowledge atkPriv = (MomPersistentPlayerPrivateKnowledge) attackingPlayer.getPersistentPlayerPrivateKnowledge ();
			if ((captureCityDecision != null) && (defendingPlayer != null))
			{
				// Calc as a long since the the multiplication could give a really big number
				final MomPersistentPlayerPrivateKnowledge defPriv = (MomPersistentPlayerPrivateKnowledge) defendingPlayer.getPersistentPlayerPrivateKnowledge ();
				
				final long cityPopulation = tc.getCityData ().getCityPopulation ();
				final long totalGold = getResourceValueUtils ().findAmountStoredForProductionType (defPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
				final long totalPopulation = getOverlandMapServerUtils ().totalPlayerPopulation
					(mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), defendingPlayer.getPlayerDescription ().getPlayerID (),
					mom.getSessionDescription ().getOverlandMapSize (), mom.getServerDB ());
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
				getResourceValueUtils ().addToAmountStored (defPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, (int) -goldSwiped);
				getResourceValueUtils ().addToAmountStored (atkPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, (int) goldSwiped + goldFromRazing);
			}
			
			// Cancel any spells that were cast in combat
			getFogOfWarMidTurnMultiChanges ().switchOffMaintainedSpellsCastInCombatLocation_OnServerAndClients
				(mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), combatLocation, mom.getServerDB (), mom.getSessionDescription ());
			
			getFogOfWarMidTurnMultiChanges ().switchOffMaintainedSpellsCastOnUnitsInCombat_OnServerAndClients
				(mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), combatLocation, mom.getServerDB (), mom.getSessionDescription ());
			
			// Undead created from ghouls / life stealing?
			final PlayerServerDetails losingPlayer = (winningPlayer == attackingPlayer) ? defendingPlayer : attackingPlayer;
			msg.setUndeadCreated (getCombatProcessing ().createUndead (combatLocation, winningPlayer, losingPlayer,
				mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), mom.getSessionDescription ().getFogOfWarSetting (), mom.getServerDB ()));

			// Send the CombatEnded message
			// Remember defending player may still be nil if we attacked an empty lair
			if ((defendingPlayer != null) && (defendingPlayer.getPlayerDescription ().isHuman ()))
				defendingPlayer.getConnection ().sendMessageToClient (msg);
			
			if (attackingPlayer.getPlayerDescription ().isHuman ())
				attackingPlayer.getConnection ().sendMessageToClient (msg);
			
			// Kill off dead units from the combat and remove any combat summons like Phantom Warriors
			// This also removes ('kills') on the client monsters in a lair/node/tower who won
			// Have to do this before we advance the attacker, otherwise we end up trying to advance the combat summoned units
			getCombatProcessing ().purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer,
				mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), mom.getSessionDescription ().getFogOfWarSetting (), mom.getServerDB ());
			
			// If its a border conflict, then we don't actually care who won - one side will already have been wiped out, and hence their
			// PendingMovement will have been removed, leaving the winner's PendingMovement still to be processed, and the main
			// processSimultaneousTurnsMovement method will figure out what to do about that (i.e. whether its a move or another combat)
			if ((mom.getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS) &&
				(tc.getCombatAttackerPendingMovement () != null) && (tc.getCombatDefenderPendingMovement () != null))
			{
				log.debug ("Border conflict, so don't care who won");
			}
				
			// If the attacker won then advance their units to the target square
			else if (winningPlayer == attackingPlayer)
			{
				log.debug ("Attacker won");
				
				// Work out moveToPlane - If attackers are capturing a tower from Myrror, in which case they jump to Arcanus as part of the move
				final MapCoordinates3DEx moveTo = new MapCoordinates3DEx (combatLocation);
				if (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (tc.getTerrainData ()))
					moveTo.setZ (0);
				
				// Put all the attackers in a list, and figure out moveFrom.
				// NB. We intentionally don't check combatPosition and heading here because we DO want units to advance if they
				// were land units sitting in transports during a naval combat.
				MapCoordinates3DEx moveFrom = null;
				final List<MemoryUnit> unitStack = new ArrayList<MemoryUnit> ();
				for (final MemoryUnit trueUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
					if ((trueUnit.getStatus () == UnitStatusID.ALIVE) && (combatLocation.equals (trueUnit.getCombatLocation ())) &&
						(trueUnit.getCombatSide () == UnitCombatSideID.ATTACKER))
					{
						unitStack.add (trueUnit);
						moveFrom = new MapCoordinates3DEx ((MapCoordinates3DEx) trueUnit.getUnitLocation ());
					}

				// Its possible to get a list of 0 here, if the only surviving attacking units were combat summons like phantom warriors which have now been removed
				if (unitStack.size () > 0)
					getFogOfWarMidTurnMultiChanges ().moveUnitStackOneCellOnServerAndClients (unitStack, attackingPlayer,
						moveFrom, moveTo, mom.getPlayers (), mom.getGeneralServerKnowledge (), mom.getSessionDescription (), mom.getServerDB ());
				
				// Deal with cities
				if (captureCityDecision == CaptureCityDecisionID.CAPTURE)
				{
					// Destroy enemy wizards' fortress and/or summoning circle
					final MemoryBuilding wizardsFortress = getMemoryBuildingUtils ().findBuilding (mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (), combatLocation, CommonDatabaseConstants.BUILDING_FORTRESS);
					final MemoryBuilding summoningCircle = getMemoryBuildingUtils ().findBuilding (mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (), combatLocation, CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE);
					
					if (wizardsFortress != null)
						getFogOfWarMidTurnChanges ().destroyBuildingOnServerAndClients (mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (),
							wizardsFortress.getBuildingURN (), false, mom.getSessionDescription (), mom.getServerDB ());

					if (summoningCircle != null)
						getFogOfWarMidTurnChanges ().destroyBuildingOnServerAndClients (mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (),
							summoningCircle.getBuildingURN (), false, mom.getSessionDescription (), mom.getServerDB ());
					
					// Deal with spells cast on the city:
					// 1) Any spells the defender had cast on the city must be enchantments - which unfortunately we don't get - so cancel these
					// 2) Any spells the attacker had cast on the city must be curses - we don't want to curse our own city - so cancel them
					// 3) Any spells a 3rd player (neither the defender nor attacker) had cast on the city must be curses - and I'm sure they'd like to continue cursing the new city owner :D
					getFogOfWarMidTurnMultiChanges ().switchOffMaintainedSpellsInLocationOnServerAndClients
						(mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), combatLocation,
						attackingPlayer.getPlayerDescription ().getPlayerID (), mom.getServerDB (), mom.getSessionDescription ());
				
					if (defendingPlayer != null)
						getFogOfWarMidTurnMultiChanges ().switchOffMaintainedSpellsInLocationOnServerAndClients
							(mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), combatLocation,
							defendingPlayer.getPlayerDescription ().getPlayerID (), mom.getServerDB (), mom.getSessionDescription ());

					// Take ownership of the city
					tc.getCityData ().setCityOwnerID (attackingPlayer.getPlayerDescription ().getPlayerID ());
					tc.getCityData ().setProductionSoFar (null);
					tc.getCityData ().setCurrentlyConstructingUnitID (null);
					tc.getCityData ().setCurrentlyConstructingBuildingID (ServerDatabaseValues.CITY_CONSTRUCTION_DEFAULT);
					
					// Although farmers will be the same, capturing player may have a different tax rate or different units stationed here so recalc rebels
					tc.getCityData ().setNumberOfRebels (getCityCalculations ().calculateCityRebels
						(mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
						mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (),
						combatLocation, atkPriv.getTaxRateID (), mom.getServerDB ()).getFinalTotal ());
					
					getServerCityCalculations ().ensureNotTooManyOptionalFarmers (tc.getCityData ());
					
					getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
						mom.getPlayers (), combatLocation, mom.getSessionDescription ().getFogOfWarSetting ());
				}
				else if (captureCityDecision == CaptureCityDecisionID.RAZE)
				{
					// Cancel all spells cast on the city regardless of owner
					getFogOfWarMidTurnMultiChanges ().switchOffMaintainedSpellsInLocationOnServerAndClients
						(mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), combatLocation, 0, mom.getServerDB (), mom.getSessionDescription ());
					
					// Wreck all the buildings
					getFogOfWarMidTurnMultiChanges ().destroyAllBuildingsInLocationOnServerAndClients (mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (),
						combatLocation, mom.getSessionDescription (), mom.getServerDB ());
					
					// Wreck the city
					tc.setCityData (null);
					getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getPlayers (),
						combatLocation, mom.getSessionDescription ().getFogOfWarSetting ());
				}
			}
			else
			{
				log.debug ("Defender won");
			}
			
			// Give any hero items to the winner
			if (tc.getItemsFromHeroesWhoDiedInCombat ().size () > 0)
			{
				final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) winningPlayer.getPersistentPlayerPrivateKnowledge ();
				priv.getUnassignedHeroItem ().addAll (tc.getItemsFromHeroesWhoDiedInCombat ());
				
				if (winningPlayer.getPlayerDescription ().isHuman ())
				{
					final AddUnassignedHeroItemMessage heroItemMsg = new AddUnassignedHeroItemMessage ();
					for (final NumberedHeroItem item : tc.getItemsFromHeroesWhoDiedInCombat ())
					{
						heroItemMsg.setHeroItem (item);
						winningPlayer.getConnection ().sendMessageToClient (heroItemMsg);
					}
				}
				
				tc.getItemsFromHeroesWhoDiedInCombat ().clear ();
			}

			// Recheck that transports have enough capacity to hold all units that require them (both for attacker and defender, and regardless who won)
			getServerUnitCalculations ().recheckTransportCapacity (combatLocation, mom.getGeneralServerKnowledge ().getTrueMap (),
				mom.getPlayers (), mom.getSessionDescription ().getFogOfWarSetting (), mom.getServerDB ());
			
			// Set all units CombatX, CombatY back to -1, -1 so we don't think they're in combat anymore.
			// Have to do this after we advance the attackers, otherwise the attackers' CombatX, CombatY will already
			// be -1, -1 and we'll no longer be able to tell who was part of the attacking force and who wasn't.
			// Have to do this before we recalc FOW, since if one side was wiped out, the FOW update may delete their memory of the opponent... which
			// then crashes the client if we then try to send msgs to take those opposing units out of combat.
			log.debug ("Removing units out of combat");
			getCombatProcessing ().removeUnitsFromCombat (attackingPlayer, defendingPlayer, mom.getGeneralServerKnowledge ().getTrueMap (), combatLocation, mom.getServerDB ());
			
			// Even if one side won the combat, they might have lost their unit with the longest scouting range
			// So easiest to just recalculate FOW for both players
			if (defendingPlayer != null)
				getFogOfWarProcessing ().updateAndSendFogOfWar (mom.getGeneralServerKnowledge ().getTrueMap (),
					defendingPlayer, mom.getPlayers (), "combatEnded-D", mom.getSessionDescription (), mom.getServerDB ());
			
			// If attacker won, we'll have already recalc'd their FOW when their units advanced 1 square
			// But lets do it anyway - maybe they captured a city with an Oracle
			getFogOfWarProcessing ().updateAndSendFogOfWar (mom.getGeneralServerKnowledge ().getTrueMap (),
				attackingPlayer, mom.getPlayers (), "combatEnded-A", mom.getSessionDescription (), mom.getServerDB ());
			
			// Remove all combat area effects from spells like Prayer, Mass Invisibility, etc.
			log.debug ("Removing all spell CAEs");
			getFogOfWarMidTurnMultiChanges ().removeCombatAreaEffectsFromLocalisedSpells
				(mom.getGeneralServerKnowledge ().getTrueMap (), combatLocation, mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ());
			
			// Assuming both sides may have taken losses, could have gained/lost a city, etc. etc., best to just recalculate production for both
			// DefendingPlayer may still be nil
			getServerResourceCalculations ().recalculateGlobalProductionValues (attackingPlayer.getPlayerDescription ().getPlayerID (), false, mom);
			
			if (defendingPlayer != null)
				getServerResourceCalculations ().recalculateGlobalProductionValues (defendingPlayer.getPlayerDescription ().getPlayerID (), false, mom);
		
			if (mom.getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS)
			{
				// Clean up the PendingMovement(s) that caused this combat
				if (tc.getCombatAttackerPendingMovement () == null)
					throw new MomException ("Simultaneous turns combat ended, but CombatAttackerPendingMovement is null");
				
				// If its a border conflict, do not clean up the PendingMovement - the unit stack didn't advance yet and still needs to do so
				// so only clear the ref to the pending movements from the grid cell
				if (tc.getCombatDefenderPendingMovement () == null)
					
					// NB. PendingMovements of the side who was wiped out will already have been removed from the list as the last unit died
					// (see killUnitOnServerAndClients) so we may actually have nothing to remove here
					atkPriv.getPendingMovement ().remove (tc.getCombatAttackerPendingMovement ());

				tc.setCombatAttackerPendingMovement (null);
				tc.setCombatDefenderPendingMovement (null);
				
				// This routine is reponsible for figuring out if there are more combats to play, or if we can start the next turn
				getPlayerMessageProcessing ().processSimultaneousTurnsMovement (mom);
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
			
			// Clear out combat related items
			tc.setAttackingPlayerID (null);
			tc.setDefendingPlayerID (null);
			tc.setCombatCurrentPlayerID (null);
			tc.setSpellCastThisCombatTurn (null);
			tc.setCombatDefenderCastingSkillRemaining (null);
			tc.setCombatAttackerCastingSkillRemaining (null);
		}
		
		log.trace ("Exiting combatEnded");
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
	 * @return Server-only city calculations
	 */
	public final ServerCityCalculations getServerCityCalculations ()
	{
		return serverCityCalculations;
	}

	/**
	 * @param calc Server-only city calculations
	 */
	public final void setServerCityCalculations (final ServerCityCalculations calc)
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
	public final ServerResourceCalculations getServerResourceCalculations ()
	{
		return serverResourceCalculations;
	}

	/**
	 * @param calc Resource calculations
	 */
	public final void setServerResourceCalculations (final ServerResourceCalculations calc)
	{
		serverResourceCalculations = calc;
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

	/**
	 * @return Combat processing
	 */
	public final CombatProcessing getCombatProcessing ()
	{
		return combatProcessing;
	}

	/**
	 * @param proc Combat processing
	 */
	public final void setCombatProcessing (final CombatProcessing proc)
	{
		combatProcessing = proc;
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
	 * @return Server-only unit calculations
	 */
	public final ServerUnitCalculations getServerUnitCalculations ()
	{
		return serverUnitCalculations;
	}

	/**
	 * @param calc Server-only unit calculations
	 */
	public final void setServerUnitCalculations (final ServerUnitCalculations calc)
	{
		serverUnitCalculations = calc;
	}
}