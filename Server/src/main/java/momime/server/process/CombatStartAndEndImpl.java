package momime.server.process;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.calculations.MomCityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.CaptureCityDecisionID;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.TurnSystem;
import momime.common.messages.UnitCombatSideID;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.AskForCaptureCityDecisionMessage;
import momime.common.messages.servertoclient.CombatEndedMessage;
import momime.common.messages.servertoclient.SelectNextUnitToMoveOverlandMessage;
import momime.common.messages.servertoclient.StartCombatMessage;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.ScheduledCombatUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.MomServerCityCalculations;
import momime.server.calculations.MomServerResourceCalculations;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.FogOfWarProcessing;
import momime.server.mapgenerator.CombatMapGenerator;
import momime.server.messages.v0_9_5.ServerGridCell;
import momime.server.utils.CityServerUtils;
import momime.server.utils.OverlandMapServerUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

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

	/** Scheduled combat utils */
	private ScheduledCombatUtils scheduledCombatUtils; 
	
	/** Simultaneous turns combat scheduler */
	private CombatScheduler combatScheduler;
	
	/** Map generator */
	private CombatMapGenerator combatMapGenerator;
	
	/** Combat processing */
	private CombatProcessing combatProcessing;
	
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;
	
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
	public final void startCombat (final MapCoordinates3DEx defendingLocation, final MapCoordinates3DEx attackingFrom,
		final Integer scheduledCombatURN, final PlayerServerDetails attackingPlayer, final List<Integer> attackingUnitURNs, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.trace ("Entering startCombat: " + defendingLocation + ", " + attackingFrom + ", " + scheduledCombatURN +
			", Player ID " + attackingPlayer.getPlayerDescription ().getPlayerID ());
		
		// If attacking a tower in Myrror, then Defending-AttackingFrom will be 0-1
		// If attacking from a tower onto Myrror, then Defending-AttackingFrom will be 1-0 - both of these should be shown on Myrror only
		// Any tower combats to/from Arcanus will be 0-0, and should appear on Arcanus only
		// Hence the reason for the Max
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (defendingLocation.getX (), defendingLocation.getY (),
			Math.max (defendingLocation.getZ (), attackingFrom.getZ ()));
		
		// If this is a scheduled combat then check it out... WalkInWithoutAFight may be switched on, in which case we want to move rather than setting up a combat
		boolean walkInWithoutAFight = false;
		if (scheduledCombatURN != null)
			walkInWithoutAFight = getScheduledCombatUtils ().findScheduledCombatURN
				(mom.getGeneralServerKnowledge ().getScheduledCombat (), scheduledCombatURN, "startCombat").isWalkInWithoutAFight ();
		
		// Record the scheduled combat ID
		final ServerGridCell tc = (ServerGridCell) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(combatLocation.getZ ()).getRow ().get (combatLocation.getY ()).getCell ().get (combatLocation.getX ());
		tc.setScheduledCombatURN (scheduledCombatURN);
		
		// Find out who we're attacking - if an empty city, we can get null here
		final MemoryUnit firstDefendingUnit = getUnitUtils ().findFirstAliveEnemyAtLocation (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (),
			defendingLocation.getX (), defendingLocation.getY (), defendingLocation.getZ (), 0);
		PlayerServerDetails defendingPlayer = (firstDefendingUnit == null) ? null : getMultiplayerSessionServerUtils ().findPlayerWithID
			(mom.getPlayers (), firstDefendingUnit.getOwningPlayerID (), "startCombat");
		
		// If there is a real defender, we need to inform all players that they're involved in a combat
		// We dealt with informing about the attacker in InitiateCombat
		if ((defendingPlayer != null) && (mom.getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS))
			getCombatScheduler ().informClientsOfPlayerBusyInCombat (defendingPlayer, mom.getPlayers (), true);

		// Start off the message to the client
		final StartCombatMessage startCombatMessage = new StartCombatMessage ();
		startCombatMessage.setCombatLocation (combatLocation);
		startCombatMessage.setScheduledCombatURN (scheduledCombatURN);
		
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
			UnitCombatSideID.DEFENDER, null, tc.getCombatMap (), mom);
				
		log.debug ("Positioning attackers at " + defendingLocation);
		getCombatProcessing ().positionCombatUnits (combatLocation, startCombatMessage, attackingPlayer, defendingPlayer, mom.getSessionDescription ().getCombatMapSize (), attackingFrom,
			COMBAT_SETUP_ATTACKER_FRONT_ROW_CENTRE_X, COMBAT_SETUP_ATTACKER_FRONT_ROW_CENTRE_Y, COMBAT_SETUP_ATTACKER_ROWS, COMBAT_SETUP_ATTACKER_FACING,
			UnitCombatSideID.ATTACKER, attackingUnitURNs, tc.getCombatMap (), mom);
		
		// Are there any defenders (attacking an empty city) - if not then bypass the combat entirely
		if ((walkInWithoutAFight) || (defendingPlayer == null))
		{
			log.debug ("Combat ending before it starts");
			
			// There's a few situations to deal with here:
			// If "walk in without a fight" is on, then we have other units already at that location, so there definitely no defender.
			// The other way we can get to this block of code is if there's no defending units - this includes that we may be walking into
			// an enemy city with no defence, and for this situation we need to set the player correctly so we notify the player that they've lost their city.
			// It should be impossible to initiate a combat against your own city, since the "walk in without a fight" check is done first.
			if (walkInWithoutAFight)
				defendingPlayer = null;
			
			else if ((tc.getCityData () != null) && (tc.getCityData ().getCityPopulation () != null) && (tc.getCityData ().getCityPopulation () > 0) && (tc.getCityData ().getCityOwnerID () != null))
				defendingPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), tc.getCityData ().getCityOwnerID (), "startCombat-CD");
				
			else
				// It'll be null anyway, but just to be certain...
				defendingPlayer = null;
				
			combatEnded (combatLocation, attackingPlayer, defendingPlayer, attackingPlayer,	// <-- who won
				null, mom);
		}
		else
		{
			log.debug ("Continuing combat setup");
			
			// Set casting skill allocation for this combat 
			final MomPersistentPlayerPrivateKnowledge attackingPriv = (MomPersistentPlayerPrivateKnowledge) attackingPlayer.getPersistentPlayerPrivateKnowledge ();
			tc.setCombatAttackerCastingSkillRemaining (getResourceValueUtils ().calculateCastingSkillOfPlayer (attackingPriv.getResourceValue ()));
			
			if (defendingPlayer != null)
			{
				final MomPersistentPlayerPrivateKnowledge defendingPriv = (MomPersistentPlayerPrivateKnowledge) defendingPlayer.getPersistentPlayerPrivateKnowledge ();
				tc.setCombatDefenderCastingSkillRemaining (getResourceValueUtils ().calculateCastingSkillOfPlayer (defendingPriv.getResourceValue ()));
			}
			
			// Finally send the message, containing all the unit positions, units (if monsters in a node/lair/tower) and combat scenery
			if (attackingPlayer.getPlayerDescription ().isHuman ())
				attackingPlayer.getConnection ().sendMessageToClient (startCombatMessage);

			if ((defendingPlayer != null) && (defendingPlayer.getPlayerDescription ().isHuman ()))
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
	@Override
	public final void combatEnded (final MapCoordinates3DEx combatLocation,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final PlayerServerDetails winningPlayer,
		final CaptureCityDecisionID captureCityDecision, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.trace ("Entering combatEnded: " + combatLocation);
		
		final ServerGridCell tc = (ServerGridCell) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(combatLocation.getZ ()).getRow ().get (combatLocation.getY ()).getCell ().get (combatLocation.getX ());
		
		// If we're walking into a city that we don't already own (its possible we're moving into our own city if this is a "walk in without a fight")
		// then don't end the combat just yet - first ask the winner whether they want to capture or raze the city
		if ((winningPlayer == attackingPlayer) && (captureCityDecision == null) && (tc.getCityData () != null) &&
			(tc.getCityData ().getCityPopulation () != null) && (tc.getCityData ().getCityPopulation () > 0) &&
			(!attackingPlayer.getPlayerDescription ().getPlayerID ().equals (tc.getCityData ().getCityOwnerID ())))
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
				(mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), combatLocation, mom.getServerDB (), mom.getSessionDescription ());
			
			// Kill off dead units from the combat and remove any combat summons like Phantom Warriors
			// This also removes ('kills') on the client monsters in a lair/node/tower who won
			// Have to do this before we advance the attacker, otherwise we end up trying to advance the combat summoned units
			getCombatProcessing ().purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer,
				mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), mom.getSessionDescription ().getFogOfWarSetting (), mom.getServerDB ());
			
			// If the attacker won then advance their units to the target square
			if (winningPlayer == attackingPlayer)
			{
				log.debug ("Attacker won");
				
				// Work out moveToPlane - If attackers are capturing a tower from Myrror, in which case they jump to Arcanus as part of the move
				final MapCoordinates3DEx moveTo = new MapCoordinates3DEx (combatLocation);
				if (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (tc.getTerrainData ()))
					moveTo.setZ (0);
				
				// Put all the attackers in a list, and figure out moveFrom
				MapCoordinates3DEx moveFrom = null;
				final List<MemoryUnit> unitStack = new ArrayList<MemoryUnit> ();
				for (final MemoryUnit trueUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
					if ((trueUnit.getStatus () == UnitStatusID.ALIVE) && (combatLocation.equals (trueUnit.getCombatLocation ())) &&
						(trueUnit.getCombatSide () == UnitCombatSideID.ATTACKER))
					{
						unitStack.add (trueUnit);
						moveFrom = new MapCoordinates3DEx ((MapCoordinates3DEx) trueUnit.getUnitLocation ());
					}

				getFogOfWarMidTurnChanges ().moveUnitStackOneCellOnServerAndClients (unitStack, attackingPlayer,
					moveFrom, moveTo, mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getSessionDescription (), mom.getServerDB ());
				
				// Deal with cities
				if (captureCityDecision == CaptureCityDecisionID.CAPTURE)
				{
					// Destroy enemy wizards' fortress and/or summoning circle
					final MemoryBuilding wizardsFortress = getMemoryBuildingUtils ().findBuilding (mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (), combatLocation, CommonDatabaseConstants.VALUE_BUILDING_FORTRESS);
					final MemoryBuilding summoningCircle = getMemoryBuildingUtils ().findBuilding (mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (), combatLocation, CommonDatabaseConstants.VALUE_BUILDING_SUMMONING_CIRCLE);
					
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
					getFogOfWarMidTurnChanges ().switchOffMaintainedSpellsInLocationOnServerAndClients
						(mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), combatLocation,
						attackingPlayer.getPlayerDescription ().getPlayerID (), mom.getServerDB (), mom.getSessionDescription ());
				
					if (defendingPlayer != null)
						getFogOfWarMidTurnChanges ().switchOffMaintainedSpellsInLocationOnServerAndClients
							(mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), combatLocation,
							defendingPlayer.getPlayerDescription ().getPlayerID (), mom.getServerDB (), mom.getSessionDescription ());

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
				else if (captureCityDecision == CaptureCityDecisionID.RAZE)
				{
					// Cancel all spells cast on the city regardless of owner
					getFogOfWarMidTurnChanges ().switchOffMaintainedSpellsInLocationOnServerAndClients
						(mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), combatLocation, 0, mom.getServerDB (), mom.getSessionDescription ());
					
					// Wreck all the buildings
					getFogOfWarMidTurnChanges ().destroyAllBuildingsInLocationOnServerAndClients (mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (),
						combatLocation, mom.getSessionDescription (), mom.getServerDB ());
					
					// Wreck the city
					tc.setCityData (null);
					getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getPlayers (),
						combatLocation, mom.getSessionDescription ().getFogOfWarSetting (), false);
				}
			}
			else
			{
				log.debug ("Defender won");
			}
			
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
					defendingPlayer, mom.getPlayers (), false, "combatEnded-D", mom.getSessionDescription (), mom.getServerDB ());
			
			// If attacker won, we'll have already recalc'd their FOW when their units advanced 1 square
			// But lets do it anyway - maybe they captured a city with an Oracle
			getFogOfWarProcessing ().updateAndSendFogOfWar (mom.getGeneralServerKnowledge ().getTrueMap (),
				attackingPlayer, mom.getPlayers (), false, "combatEnded-A", mom.getSessionDescription (), mom.getServerDB ());
			
			// Remove all combat area effects from spells like Prayer, Mass Invisibility, etc.
			log.debug ("Removing all spell CAEs");
			getFogOfWarMidTurnChanges ().removeCombatAreaEffectsFromLocalisedSpells
				(mom.getGeneralServerKnowledge ().getTrueMap (), combatLocation, mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ());
			
			// Handle clearing up and removing the scheduled combat
			if (tc.getScheduledCombatURN () != null)
			{
				log.debug ("Tidying up scheduled combat");
				getCombatScheduler ().processEndOfScheduledCombat (tc.getScheduledCombatURN (), winningPlayer, mom);
				tc.setScheduledCombatURN (null);
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
}