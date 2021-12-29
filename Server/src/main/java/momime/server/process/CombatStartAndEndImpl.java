package momime.server.process;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.database.CitySize;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitCombatSideID;
import momime.common.messages.CaptureCityDecisionID;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
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
import momime.common.utils.PlayerKnowledgeUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.ai.MomAI;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.FogOfWarMidTurnMultiChanges;
import momime.server.fogofwar.FogOfWarProcessing;
import momime.server.fogofwar.KillUnitActionID;
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
	
	/** X coord of centre location around which defenders are placed; this is the location of the door in the city walls */
	public final static int COMBAT_SETUP_DEFENDER_FRONT_ROW_CENTRE_X = 3;
	
	/** Y coord of centre location around which defenders are placed; this is the location of the door in the city walls */
	public final static int COMBAT_SETUP_DEFENDER_FRONT_ROW_CENTRE_Y = 11;
	
	/** Direction that defenders initially face */
	final static int COMBAT_SETUP_DEFENDER_FACING = 4;

	/** Defenders require 3 rows to fit 9 units if they're in a city with city walls (2 > 4 > 3) */
	final static int COMBAT_SETUP_DEFENDER_ROWS = 4;

	/** X coord of centre location around which attackers are placed */
	final static int COMBAT_SETUP_ATTACKER_FRONT_ROW_CENTRE_X = 7;
	
	/** Y coord of centre location around which attackers are placed */
	final static int COMBAT_SETUP_ATTACKER_FRONT_ROW_CENTRE_Y = 19;
	
	/** Direction that attackers initially face */
	final static int COMBAT_SETUP_ATTACKER_FACING = 8;

	/** Attackers require 3 rows to fit 9 units, thought the final row will only ever have 1 unit in it */
	final static int COMBAT_SETUP_ATTACKER_ROWS = 3;
	
	/** Class logger */
	private final static Log log = LogFactory.getLog (CombatStartAndEndImpl.class);

	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** MemoryGridCell utils */
	private MemoryGridCellUtils memoryGridCellUtils;

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Server-only overland map utils */
	private OverlandMapServerUtils overlandMapServerUtils;
	
	/** Server-only city utils */
	private CityServerUtils cityServerUtils;
	
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
	
	/** Simultaneous turns processing */
	private SimultaneousTurnsProcessing simultaneousTurnsProcessing;
	
	/** AI player turns */
	private MomAI momAI;
	
	/** Methods for dealing with player msgs */
	private PlayerMessageProcessing playerMessageProcessing;
	
	/** City processing methods */
	private CityProcessing cityProcessing;
	
	/** MemoryBuilding utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/** Casting for each type of spell */
	private SpellCasting spellCasting;
	
	/** Methods for working with wizardIDs */
	private PlayerKnowledgeUtils playerKnowledgeUtils;
	
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
		// If attacking a tower in Myrror, then Defending-AttackingFrom will be 0-1
		// If attacking from a tower onto Myrror, then Defending-AttackingFrom will be 1-0 - both of these should be shown on Myrror only
		// Any tower combats to/from Arcanus will be 0-0, and should appear on Arcanus only
		// Really the only point of this is whether the grass should be drawn green or brown
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
		final PositionCombatUnitsSummary defenderSummary = getCombatProcessing ().positionCombatUnits (combatLocation, startCombatMessage,
			attackingPlayer, defendingPlayer, mom.getSessionDescription ().getCombatMapSize (), defendingLocation,
			COMBAT_SETUP_DEFENDER_FRONT_ROW_CENTRE_X, COMBAT_SETUP_DEFENDER_FRONT_ROW_CENTRE_Y,
			COMBAT_SETUP_DEFENDER_ROWS, COMBAT_SETUP_DEFENDER_FACING,
			UnitCombatSideID.DEFENDER, defendingUnitURNs, tc.getCombatMap (), mom);
				
		log.debug ("Positioning attackers at " + defendingLocation);
		final PositionCombatUnitsSummary attackerSummary = getCombatProcessing ().positionCombatUnits (combatLocation, startCombatMessage,
			attackingPlayer, defendingPlayer, mom.getSessionDescription ().getCombatMapSize (), attackingFrom,
			COMBAT_SETUP_ATTACKER_FRONT_ROW_CENTRE_X, COMBAT_SETUP_ATTACKER_FRONT_ROW_CENTRE_Y,
			COMBAT_SETUP_ATTACKER_ROWS, COMBAT_SETUP_ATTACKER_FACING,
			UnitCombatSideID.ATTACKER, attackingUnitURNs, tc.getCombatMap (), mom);
		
		// Remember how many units were initially on each side - need this to award fame at the end
		if (defenderSummary != null)
		{
			tc.setDefenderUnitCount (defenderSummary.getUnitCount ());
			tc.setDefenderMostExpensiveUnitCost (defenderSummary.getMostExpensiveUnitCost ());
			tc.setDefenderSpecialFameLost (0);
		}
		
		if (attackerSummary != null)
		{
			tc.setAttackerUnitCount (attackerSummary.getUnitCount ());
			tc.setAttackerMostExpensiveUnitCost (attackerSummary.getMostExpensiveUnitCost ());
			tc.setAttackerSpecialFameLost (0);
		}
		
		// Did the attacker try to attack with only non-combat units?  If so then bypass the combat entirely
		if ((attackerSummary != null) && (attackerSummary.getUnitCount () == 0))
		{
			log.debug ("Combat ending before it starts (no attackers)");
			
			// If attacking an enemy city with no defence, set the player correctly so we notify the player that they've lost their city.
			if ((defendingPlayer == null) && (tc.getCityData () != null))
				defendingPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), tc.getCityData ().getCityOwnerID (), "startCombat-CA");
				
			combatEnded (combatLocation, attackingPlayer, defendingPlayer, defendingPlayer,	// <-- who won
				null, mom);
		}
		
		// Are there any defenders (attacking an empty city) - if not then bypass the combat entirely
		else if (defendingPlayer == null)
		{
			log.debug ("Combat ending before it starts (no defenders)");
			
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
			tc.setCombatTurnCount (0);
			tc.setCollateralAccumulator (0);
			tc.setAttackingPlayerID (attackingPlayer.getPlayerDescription ().getPlayerID ());
			tc.setDefendingPlayerID (defendingPlayer.getPlayerDescription ().getPlayerID ());
			tc.getLastCombatMoveDirection ().clear ();
			
			// Set casting skill allocation for this combat 
			final MomPersistentPlayerPrivateKnowledge attackingPriv = (MomPersistentPlayerPrivateKnowledge) attackingPlayer.getPersistentPlayerPrivateKnowledge ();
			tc.setCombatAttackerCastingSkillRemaining (getResourceValueUtils ().calculateModifiedCastingSkill (attackingPriv.getResourceValue (),
				attackingPlayer, mom.getPlayers (), attackingPriv.getFogOfWarMemory (), mom.getServerDB (), false)); 
			
			final MomPersistentPlayerPrivateKnowledge defendingPriv = (MomPersistentPlayerPrivateKnowledge) defendingPlayer.getPersistentPlayerPrivateKnowledge ();
			tc.setCombatDefenderCastingSkillRemaining (getResourceValueUtils ().calculateModifiedCastingSkill (defendingPriv.getResourceValue (),
				defendingPlayer, mom.getPlayers (), defendingPriv.getFogOfWarMemory (), mom.getServerDB (), false)); 
			
			// Finally send the message, containing all the unit positions, units (if monsters in a node/lair/tower) and combat scenery
			if (attackingPlayer.getPlayerDescription ().isHuman ())
				attackingPlayer.getConnection ().sendMessageToClient (startCombatMessage);

			if (defendingPlayer.getPlayerDescription ().isHuman ())
				defendingPlayer.getConnection ().sendMessageToClient (startCombatMessage);
			
			// Kick off first turn of combat
			getCombatProcessing ().progressCombat (combatLocation, true, false, mom);
		}
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
		final ServerGridCellEx tc = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(combatLocation.getZ ()).getRow ().get (combatLocation.getY ()).getCell ().get (combatLocation.getX ());
		
		CaptureCityDecisionID useCaptureCityDecision = captureCityDecision;

		final MomPersistentPlayerPublicKnowledge atkPub = (MomPersistentPlayerPublicKnowledge) attackingPlayer.getPersistentPlayerPublicKnowledge ();
		
		// If we're walking into a city that we don't already own (its possible we're moving into our own city if this is a "walk in without a fight")
		// then don't end the combat just yet - first ask the winner whether they want to capture or raze the city
		if ((winningPlayer == attackingPlayer) && (useCaptureCityDecision == null) && (tc.getCityData () != null) &&
			(attackingPlayer.getPlayerDescription ().getPlayerID () != tc.getCityData ().getCityOwnerID ()))
		{
			if (tc.getCityData ().getCityPopulation () < 1000)
			{
				log.debug ("Captured an outpost, automatic raze");
				useCaptureCityDecision = CaptureCityDecisionID.RAZE;
			}
			
			else if (attackingPlayer.getPlayerDescription ().isHuman ())
			{
				log.debug ("Undecided city capture, bulk of method will not run");
				
				final AskForCaptureCityDecisionMessage msg = new AskForCaptureCityDecisionMessage ();
				msg.setCityLocation (combatLocation);
				if (defendingPlayer != null)
					msg.setDefendingPlayerID (defendingPlayer.getPlayerDescription ().getPlayerID ());
				
				attackingPlayer.getConnection ().sendMessageToClient (msg);
			}
			
			// Rampaging monsters have their own special options for captured cities
			else if (CommonDatabaseConstants.WIZARD_ID_MONSTERS.equals (atkPub.getWizardID ()))
			{
				if (getMemoryBuildingUtils ().findBuilding (mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (), combatLocation, CommonDatabaseConstants.BUILDING_FORTRESS) != null)
					useCaptureCityDecision = CaptureCityDecisionID.RAMPAGE;
				else
					useCaptureCityDecision = getRandomUtils ().nextBoolean () ? CaptureCityDecisionID.RAMPAGE : CaptureCityDecisionID.RUIN;
			}
			
			else
			{
				log.debug ("AI player ID " + attackingPlayer.getPlayerDescription ().getPlayerID () + " captured a city");
				useCaptureCityDecision = CaptureCityDecisionID.CAPTURE;
			}
		}
		
		// Exact opposite check as above, just now useCaptureCityDecision may have been set
		if ((winningPlayer != attackingPlayer) || (useCaptureCityDecision != null) || (tc.getCityData () == null) ||
			(attackingPlayer.getPlayerDescription ().getPlayerID () == tc.getCityData ().getCityOwnerID ()))
		{
			// Build the bulk of the CombatEnded message
			final CombatEndedMessage msg = new CombatEndedMessage ();
			msg.setCombatLocation (combatLocation);
			msg.setWinningPlayerID (winningPlayer.getPlayerDescription ().getPlayerID ());
			msg.setCaptureCityDecisionID (useCaptureCityDecision);
			msg.setHeroItemCount (tc.getItemsFromHeroesWhoDiedInCombat ().size ());
			
			// Start to work out fame change for each player involved
			int winningFameChange = 0;
			int losingFameChange = 0;
			long goldSwiped = 0;
			boolean wasWizardsFortress = false;
			
			// Deal with the attacking player swiping gold from a city they just took - we do this first so we can send it with the CombatEnded message
			final MomPersistentPlayerPrivateKnowledge atkPriv = (MomPersistentPlayerPrivateKnowledge) attackingPlayer.getPersistentPlayerPrivateKnowledge ();
			final MomPersistentPlayerPrivateKnowledge defPriv = (defendingPlayer == null) ? null : (MomPersistentPlayerPrivateKnowledge) defendingPlayer.getPersistentPlayerPrivateKnowledge ();
			
			if ((useCaptureCityDecision != null) && (defendingPlayer != null))
			{
				// Calc as a long since the the multiplication could give a really big number
				final long cityPopulation = tc.getCityData ().getCityPopulation ();
				final long totalGold = getResourceValueUtils ().findAmountStoredForProductionType (defPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
				final long totalPopulation = getOverlandMapServerUtils ().totalPlayerPopulation
					(mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), defendingPlayer.getPlayerDescription ().getPlayerID (),
					mom.getSessionDescription ().getOverlandMapSize (), mom.getServerDB ());
				goldSwiped = (totalGold * cityPopulation) / totalPopulation;
				msg.setGoldSwiped ((int) goldSwiped);
				
				// Any gold from razing buildings?
				final int goldFromRazing; 
				if (useCaptureCityDecision == CaptureCityDecisionID.RAZE)
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
				
				// Fame gained/lost for capturing a city
				final CitySize citySize = mom.getServerDB ().findCitySize (tc.getCityData ().getCitySizeID (), "combatEnded");
				if (citySize.getFameLostForLosing () != null)
				{
					losingFameChange = losingFameChange - citySize.getFameLostForLosing ();
					
					if ((useCaptureCityDecision == CaptureCityDecisionID.RAZE) && (mom.getSessionDescription ().getDifficultyLevel ().isFameRazingPenalty ()))
						winningFameChange = winningFameChange - citySize.getFameLostForLosing ();
				}
				
				if ((citySize.getFameGainedForCapturing () != null) &&
					((useCaptureCityDecision == CaptureCityDecisionID.CAPTURE) || (!mom.getSessionDescription ().getDifficultyLevel ().isFameRazingPenalty ())))
					
					winningFameChange = winningFameChange + citySize.getFameGainedForCapturing ();
				
				// Need this much lower down too
				wasWizardsFortress = (getMemoryBuildingUtils ().findBuilding
					(mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (), combatLocation, CommonDatabaseConstants.BUILDING_FORTRESS) != null);
				if (wasWizardsFortress)
					winningFameChange = winningFameChange + 5;
			}
			
			// If its a city combat, population or buildings may be lost even if the attacker did not win
			if ((tc.getCityData () != null) &&
				(((winningPlayer == attackingPlayer) && ((useCaptureCityDecision == CaptureCityDecisionID.CAPTURE) ||
					(useCaptureCityDecision == CaptureCityDecisionID.RAMPAGE))) ||
				(winningPlayer == defendingPlayer)))
			{
				if (tc.getCollateralAccumulator () == null)
					tc.setCollateralAccumulator (0);
				
				int baseChance = 0;
				if (winningPlayer == attackingPlayer)
					baseChance = baseChance + 10;
				
				if (useCaptureCityDecision == CaptureCityDecisionID.RAMPAGE)
					baseChance = baseChance + 40;
				
				if ((tc.getCollateralAccumulator () > 0) || (baseChance > 0))
				{
					// Roll population
					final int populationChance = Math.min (baseChance + (tc.getCollateralAccumulator () * 2), 50);
					final int populationRolls = (tc.getCityData ().getCityPopulation () / 1000) - 1;
					int populationKilled = 0;
					for (int n = 0; n < populationRolls; n++)
						if (getRandomUtils ().nextInt (100) < populationChance)
							populationKilled++;
			
					if (populationKilled > 0)
					{
						tc.getCityData ().setCityPopulation (tc.getCityData ().getCityPopulation () - (populationKilled * 1000));
						msg.setPopulationKilled (populationKilled);
					}
					
					// Roll buildings
					final int buildingsChance = Math.min (baseChance + tc.getCollateralAccumulator (), 75);
					final int buildingsDestroyed = getSpellCasting ().rollChanceOfEachBuildingBeingDestroyed (null, null, buildingsChance, Arrays.asList (combatLocation), mom);
					
					if (buildingsDestroyed > 0)
						msg.setBuildingsDestroyed (buildingsDestroyed);

					// If buildings are destroyed, that recalculates the city anyway
					if ((populationKilled > 0) && (buildingsDestroyed == 0))
					{
						mom.getWorldUpdates ().recalculateCity (combatLocation);
						mom.getWorldUpdates ().process (mom);
					}
				}
			}
			
			// Cancel any spells that were cast in combat, note doing so can actually kill some units
			getFogOfWarMidTurnMultiChanges ().switchOffSpellsCastInCombat (combatLocation, mom);

			// Work out moveToPlane - If attackers are capturing a tower from Myrror, in which case they jump to Arcanus as part of the move
			final MapCoordinates3DEx moveTo = new MapCoordinates3DEx (combatLocation);
			if (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (tc.getTerrainData ()))
				moveTo.setZ (0);
			
			// Units with regeneration come back from being dead and/or regain full health
			msg.setRegeneratedCount (getCombatProcessing ().regenerateUnits (combatLocation, winningPlayer,
				mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), mom.getSessionDescription ().getFogOfWarSetting (), mom.getServerDB ()));
			
			// Undead created from ghouls / life stealing?
			// Note these are always moved to the "moveTo" i.e. defending location - if the attacker won, their main force will advance
			// there in the code below; if the defender won, the undead need to be moved to be stacked with the rest of the defenders.
			// This doesn't bother checking if we end up with too many units; if there are, they get removed in killUnitsIfTooManyInMapCell below.
			final PlayerServerDetails losingPlayer = (winningPlayer == attackingPlayer) ? defendingPlayer : attackingPlayer;
			final List<MemoryUnit> undead;
			if (useCaptureCityDecision == CaptureCityDecisionID.RAMPAGE)
				undead = new ArrayList<MemoryUnit> ();
			else
				undead = getCombatProcessing ().createUndead (combatLocation, moveTo, winningPlayer, losingPlayer,
					mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), mom.getSessionDescription ().getFogOfWarSetting (), mom.getServerDB ());
			
			msg.setUndeadCreated (undead.size ());

			final List<MemoryUnit> zombies = getCombatProcessing ().createZombies (combatLocation, moveTo, winningPlayer, mom);
			msg.setZombiesCreated (zombies.size ());
			
			// If won a combat vs 4 or more units, gain +1 fame
			// If lost a combat and lost 4 or more units, lose -1 fame
			int attackerFameChange = (winningPlayer == attackingPlayer) ? winningFameChange : losingFameChange;
			int defenderFameChange = (winningPlayer == defendingPlayer) ? winningFameChange : losingFameChange;
			
			if ((winningPlayer == attackingPlayer) && (tc.getDefenderUnitCount () >= 4))
			{
				attackerFameChange++;
				defenderFameChange--;
			}

			if ((winningPlayer == defendingPlayer) && (tc.getAttackerUnitCount () >= 4))
			{
				defenderFameChange++;
				attackerFameChange--;
			}
			
			// If won a combat vs an expensive unit, gain +1 fame
			// If lost a combat including losing an expensive unit, lose -1 fame
			if ((winningPlayer == attackingPlayer) && (tc.getDefenderMostExpensiveUnitCost () >= 600))
			{
				attackerFameChange++;
				defenderFameChange--;
			}

			if ((winningPlayer == defendingPlayer) && (tc.getAttackerMostExpensiveUnitCost () >= 600))
			{
				defenderFameChange++;
				attackerFameChange--;
			}
			
			// Fame for losing heroes
			if ((winningPlayer == attackingPlayer) && (tc.getDefenderSpecialFameLost () != null))
				defenderFameChange = defenderFameChange - tc.getDefenderSpecialFameLost ();

			if ((winningPlayer == defendingPlayer) && (tc.getAttackerSpecialFameLost () != null))
				attackerFameChange = attackerFameChange - tc.getAttackerSpecialFameLost ();
			
			// Update fame
			final MomPersistentPlayerPublicKnowledge defPub = (defendingPlayer == null) ? null : (MomPersistentPlayerPublicKnowledge) defendingPlayer.getPersistentPlayerPublicKnowledge ();

			if ((attackerFameChange != 0) && (getPlayerKnowledgeUtils ().isWizard (atkPub.getWizardID ())))
			{
				// Fame cannot go negative
				int attackerFame = getResourceValueUtils ().findAmountStoredForProductionType (atkPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_FAME);
				if (attackerFame + attackerFameChange < 0)
					attackerFameChange = -attackerFame;
				
				if (attackerFameChange != 0)
					getResourceValueUtils ().addToAmountStored (atkPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_FAME, attackerFameChange);
			}

			if ((defenderFameChange != 0) && (defendingPlayer != null) && (getPlayerKnowledgeUtils ().isWizard (defPub.getWizardID ())))
			{
				// Fame cannot go negative
				int defenderFame = getResourceValueUtils ().findAmountStoredForProductionType (defPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_FAME);
				if (defenderFame + defenderFameChange < 0)
					defenderFameChange = -defenderFame;
				
				if (defenderFameChange != 0)
					getResourceValueUtils ().addToAmountStored (defPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_FAME, defenderFameChange);
			}
			
			// Send the CombatEnded message
			// Remember defending player may still be nil if we attacked an empty lair
			if ((defendingPlayer != null) && (defendingPlayer.getPlayerDescription ().isHuman ()))
			{
				msg.setFameChange (defenderFameChange);
				defendingPlayer.getConnection ().sendMessageToClient (msg);
			}
			
			if (attackingPlayer.getPlayerDescription ().isHuman ())
			{
				msg.setFameChange (attackerFameChange);
				attackingPlayer.getConnection ().sendMessageToClient (msg);
			}
			
			// Kill off dead units from the combat and remove any combat summons like Phantom Warriors
			// This also removes ('kills') on the client monsters in a lair/node/tower who won
			// Have to do this before we advance the attacker, otherwise we end up trying to advance the combat summoned units
			getCombatProcessing ().purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer, mom);
			
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
				
				// Put all the attackers in a list, and figure out moveFrom.
				// NB. We intentionally don't check combatPosition and heading here because we DO want units to advance if they
				// were land units sitting in transports during a naval combat.
				// Also find any similar defenders, who didn't participate in the combat.
				MapCoordinates3DEx moveFrom = null;
				final List<MemoryUnit> unitStack = new ArrayList<MemoryUnit> ();
				
				for (final MemoryUnit trueUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
					if ((trueUnit.getStatus () == UnitStatusID.ALIVE) && (combatLocation.equals (trueUnit.getCombatLocation ())) && (trueUnit.getCombatSide () != null))
					{
						if (trueUnit.getCombatSide () == UnitCombatSideID.ATTACKER)
						{
							// Rampaging monsters that rampage through the city are just killed off rather than advancing
							if (useCaptureCityDecision == CaptureCityDecisionID.RAMPAGE)
								mom.getWorldUpdates ().killUnit (trueUnit.getUnitURN (), KillUnitActionID.PERMANENT_DAMAGE);
							else
							{
								unitStack.add (trueUnit);
								moveFrom = new MapCoordinates3DEx ((MapCoordinates3DEx) trueUnit.getUnitLocation ());
							}
						}
						
						// Kill any leftover defenders
						else if ((!undead.contains (trueUnit)) && (!zombies.contains (trueUnit)))
							mom.getWorldUpdates ().killUnit (trueUnit.getUnitURN (), KillUnitActionID.HEALABLE_OVERLAND_DAMAGE);
					}
				
				// Kill any leftover defenders
				mom.getWorldUpdates ().process (mom);
				
				// Its possible to get a list of 0 here, if the only surviving attacking units were combat summons like phantom warriors which have now been removed
				if (unitStack.size () > 0)
					getFogOfWarMidTurnMultiChanges ().moveUnitStackOneCellOnServerAndClients (unitStack, attackingPlayer, moveFrom, moveTo, mom);
				
				// Before we remove buildings, check if this was the wizard's fortress and/or summoning circle
				final boolean wasSummoningCircle = (useCaptureCityDecision != null) && (getMemoryBuildingUtils ().findBuilding
					(mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (), combatLocation, CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE) != null);
				
				// Deal with cities
				if (useCaptureCityDecision == CaptureCityDecisionID.CAPTURE)
					getCityProcessing ().captureCity (combatLocation, attackingPlayer, defendingPlayer, mom);
				
				else if (useCaptureCityDecision == CaptureCityDecisionID.RAZE)
					getCityProcessing ().razeCity (combatLocation, mom);

				else if (useCaptureCityDecision == CaptureCityDecisionID.RUIN)
					getCityProcessing ().ruinCity (combatLocation, (int) goldSwiped, mom);
				
				// If they're already banished and this was their last city being taken, then treat it just like their wizard's fortress being taken
				if ((!wasWizardsFortress) && (useCaptureCityDecision != null) &&
					(getCityServerUtils ().countCities (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), defendingPlayer.getPlayerDescription ().getPlayerID ()) == 0))
					
					wasWizardsFortress = true;
				
				// Deal with wizard being banished
				if ((wasWizardsFortress) && (useCaptureCityDecision != CaptureCityDecisionID.RAMPAGE))
					getCityProcessing ().banishWizard (attackingPlayer, defendingPlayer, mom);
				
				// From here on have to be really careful, as banishWizard may have completely tore down the session, which we can tell because the players list will be empty
				
				// If their summoning circle was taken, but they still have their fortress elsewhere, then move summoning circle to there
				if ((wasSummoningCircle) && (useCaptureCityDecision != CaptureCityDecisionID.RAMPAGE) && (mom.getPlayers ().size () > 0))
					getCityProcessing ().moveSummoningCircleToWizardsFortress (defendingPlayer.getPlayerDescription ().getPlayerID (), mom);
			}
			else
			{
				log.debug ("Defender won");
			}
			
			if (mom.getPlayers ().size () > 0)
			{
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
				
				// If life stealing attacks created some undead, its possible we've now got over 9 units in the map cell so have to kill some off again.
				// Its a bit backwards letting them get created, then killing them off, but there's too much else going on like removing dead units and combat
				// summons and advancing attackers into the target square, that its difficult to know up front whether is space to create the undead or not.
				getCombatProcessing ().killUnitsIfTooManyInMapCell (moveTo, zombies, mom);
				getCombatProcessing ().killUnitsIfTooManyInMapCell (moveTo, undead, mom);
	
				// Recheck that transports have enough capacity to hold all units that require them (both for attacker and defender, and regardless who won)
				getCombatProcessing ().recheckTransportCapacityAfterCombat (combatLocation, mom);
				
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
					getFogOfWarProcessing ().updateAndSendFogOfWar (defendingPlayer, "combatEnded-D", mom);
				
				// If attacker won, we'll have already recalc'd their FOW when their units advanced 1 square
				// But lets do it anyway - maybe they captured a city with an Oracle
				getFogOfWarProcessing ().updateAndSendFogOfWar (attackingPlayer, "combatEnded-A", mom);
				
				// Remove all combat area effects from spells like Prayer, Mass Invisibility, etc.
				log.debug ("Removing all spell CAEs");
				getFogOfWarMidTurnMultiChanges ().removeCombatAreaEffectsFromLocalisedSpells (combatLocation, mom);
				
				// Assuming both sides may have taken losses, could have gained/lost a city, etc. etc., best to just recalculate production for both
				// DefendingPlayer may still be nil
				getServerResourceCalculations ().recalculateGlobalProductionValues (attackingPlayer.getPlayerDescription ().getPlayerID (), false, mom);
				
				if (defendingPlayer != null)
					getServerResourceCalculations ().recalculateGlobalProductionValues (defendingPlayer.getPlayerDescription ().getPlayerID (), false, mom);

				// Clear out combat related items
				tc.setCombatTurnCount (null);
				tc.setAttackingPlayerID (null);
				tc.setDefendingPlayerID (null);
				tc.setCombatCurrentPlayerID (null);
				tc.setSpellCastThisCombatTurn (null);
				tc.setCombatDefenderCastingSkillRemaining (null);
				tc.setCombatAttackerCastingSkillRemaining (null);
				tc.setDefenderUnitCount (null);
				tc.setAttackerUnitCount (null);
				tc.setDefenderMostExpensiveUnitCost (null);
				tc.setAttackerMostExpensiveUnitCost (null);
				tc.setDefenderSpecialFameLost (null);
				tc.setAttackerSpecialFameLost (null);
				tc.setCollateralAccumulator (null);
				
				// Figure out what to do next now the combat is over
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
					getSimultaneousTurnsProcessing ().processSimultaneousTurnsMovement (mom);
				}
				else
				{
					// Which player is currently taking their one-player-at-a-time turn?  They must have started the combat, and now need to continue their turn
					final PlayerServerDetails currentPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID
						(mom.getPlayers (), mom.getGeneralPublicKnowledge ().getCurrentPlayerID (), "combatEnded (R)");
					
					if (currentPlayer.getPlayerDescription ().isHuman ())
					{
						// If this is a one-at-a-time turns game, we need to tell the player to make their next move
						currentPlayer.getConnection ().sendMessageToClient (new SelectNextUnitToMoveOverlandMessage ());
					}
					else
					{
						// AI players proceed with moving remaining unit stacks, possibly starting another combat or completing their turn
						log.info ("Resuming AI turn " + mom.getGeneralPublicKnowledge ().getTurnNumber () + " - " + currentPlayer.getPlayerDescription ().getPlayerName () + "...");
						if (getMomAI ().aiPlayerTurn (currentPlayer, mom))
							getPlayerMessageProcessing ().nextTurnButton (mom, currentPlayer);
					}
				}
			}
		}
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
	 * @return Simultaneous turns processing
	 */	
	public final SimultaneousTurnsProcessing getSimultaneousTurnsProcessing ()
	{
		return simultaneousTurnsProcessing;
	}

	/**
	 * @param proc Simultaneous turns processing
	 */
	public final void setSimultaneousTurnsProcessing (final SimultaneousTurnsProcessing proc)
	{
		simultaneousTurnsProcessing = proc;
	}

	/**
	 * @return AI player turns
	 */
	public final MomAI getMomAI ()
	{
		return momAI;
	}

	/**
	 * @param ai AI player turns
	 */
	public final void setMomAI (final MomAI ai)
	{
		momAI = ai;
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
	 * @return MemoryBuilding utils
	 */
	public final MemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param utils MemoryBuilding utils
	 */
	public final void setMemoryBuildingUtils (final MemoryBuildingUtils utils)
	{
		memoryBuildingUtils = utils;
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
	 * @return Casting for each type of spell
	 */
	public final SpellCasting getSpellCasting ()
	{
		return spellCasting;
	}

	/**
	 * @param c Casting for each type of spell
	 */
	public final void setSpellCasting (final SpellCasting c)
	{
		spellCasting = c;
	}

	/**
	 * @return Methods for working with wizardIDs
	 */
	public final PlayerKnowledgeUtils getPlayerKnowledgeUtils ()
	{
		return playerKnowledgeUtils;
	}

	/**
	 * @param k Methods for working with wizardIDs
	 */
	public final void setPlayerKnowledgeUtils (final PlayerKnowledgeUtils k)
	{
		playerKnowledgeUtils = k;
	}
}