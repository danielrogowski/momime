package momime.server.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.calculations.CombatMoveType;
import momime.common.calculations.MomCityCalculations;
import momime.common.calculations.MomUnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.CombatMapCoordinatesEx;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.servertoclient.v0_9_4.AskForCaptureCityDecisionMessage;
import momime.common.messages.servertoclient.v0_9_4.CombatEndedMessage;
import momime.common.messages.servertoclient.v0_9_4.DamageCalculationMessage;
import momime.common.messages.servertoclient.v0_9_4.DamageCalculationMessageTypeID;
import momime.common.messages.servertoclient.v0_9_4.FoundLairNodeTowerMessage;
import momime.common.messages.servertoclient.v0_9_4.KillUnitActionID;
import momime.common.messages.servertoclient.v0_9_4.KillUnitMessage;
import momime.common.messages.servertoclient.v0_9_4.KillUnitMessageData;
import momime.common.messages.servertoclient.v0_9_4.MoveUnitInCombatMessage;
import momime.common.messages.servertoclient.v0_9_4.SelectNextUnitToMoveOverlandMessage;
import momime.common.messages.servertoclient.v0_9_4.SetCombatPlayerMessage;
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
import momime.common.messages.v0_9_4.MoveResultsInAttackTypeID;
import momime.common.messages.v0_9_4.TurnSystem;
import momime.common.messages.v0_9_4.UnitCombatSideID;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.common.utils.CombatMapUtils;
import momime.common.utils.CombatPlayers;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.MomUnitAttributeComponent;
import momime.common.utils.MomUnitAttributePositiveNegative;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.ScheduledCombatUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.ai.CombatAI;
import momime.server.calculations.MomServerCityCalculations;
import momime.server.calculations.MomServerResourceCalculations;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_4.Plane;
import momime.server.fogofwar.FogOfWarDuplication;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.FogOfWarProcessing;
import momime.server.fogofwar.UntransmittedKillUnitActionID;
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
import com.ndg.random.RandomUtils;

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
	
	/** Combat map utils */
	private CombatMapUtils combatMapUtils;
	
	/** Combat AI */
	private CombatAI combatAI;
	
	/** Random number generator */
	private RandomUtils randomUtils;

	/**
	 * Sets up a potential combat on the server.  If we're attacking an enemy unit stack or city, then all this does is calls StartCombat.
	 * If we're attacking a node/lair/tower, then this handles scouting the node/lair/tower, sending to the client the details
	 * of what monster we scouted, and StartCombat is only called when/if they click "Yes" they want to attack.
	 * This is declared separately so it can be used immediately from MoveUnitStack in one-at-a-time games, or from requesting scheduled combats in Simultaneous turns games.
	 *
	 * @param defendingLocation Location where defending units are standing
	 * @param attackingFrom Location where attacking units are standing (which will be a map tile adjacent to defendingLocation)
	 * @param scheduledCombatURN Scheduled combat URN, if simultaneous turns game; null for one-at-a-time games
	 * @param attackingPlayer Player who is attacking
	 * @param attackingUnitURNs Which of the attacker's unit stack are attacking - they might be leaving some behind
	 * @param typeOfCombat Whether we are scouting a node/lair/tower, or actually attacking something
	 * @param monsterUnitID If typeOfCombat=SCOUT, then this says the type of monster that the attacker can see; if typeOfCombat=something else this is null
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void initiateCombat (final OverlandMapCoordinatesEx defendingLocation, final OverlandMapCoordinatesEx attackingFrom,
		final Integer scheduledCombatURN, final PlayerServerDetails attackingPlayer, final List<Integer> attackingUnitURNs,
		final MoveResultsInAttackTypeID typeOfCombat, final String monsterUnitID, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.entering (CombatProcessingImpl.class.getName (), "initiateCombat",
			new String [] {defendingLocation.toString (), attackingFrom.toString (), (scheduledCombatURN == null) ? "NotSched" : scheduledCombatURN.toString (),
			attackingPlayer.getPlayerDescription ().getPlayerID ().toString (), typeOfCombat.toString ()});
		
		// We need to inform all players that the attacker is involved in a combat.
		// We deal with the defender (if its a real human/human combat) within StartCombat - we deal with the attacker here so that the player is
		// recorded as being 'in a combat' while the 'found node/lair/tower' window is displayed.
		if ((mom.getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS) && (attackingPlayer.getPlayerDescription ().isHuman ()))
			getCombatScheduler ().informClientsOfPlayerBusyInCombat (attackingPlayer, mom.getPlayers (), true);
		
		if (typeOfCombat == MoveResultsInAttackTypeID.SCOUT)
		{
			// Show "Scouts have spotted some Sky Drakes, do you want to attack?" message
			final FoundLairNodeTowerMessage msg = new FoundLairNodeTowerMessage ();
			msg.setDefendingLocation (defendingLocation);
			msg.setAttackingFrom (attackingFrom);
			msg.setMonsterUnitID (monsterUnitID);
			msg.setScheduledCombatURN (scheduledCombatURN);
			msg.getUnitURN ().addAll (attackingUnitURNs);
			attackingPlayer.getConnection ().sendMessageToClient (msg);
			
			// Record the scouted monster ID on the server too
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) attackingPlayer.getPersistentPlayerPrivateKnowledge ();
			priv.getNodeLairTowerKnownUnitIDs ().getPlane ().get (defendingLocation.getPlane ()).getRow ().get
				(defendingLocation.getY ()).getCell ().set (defendingLocation.getX (), monsterUnitID);
		}
		else
		{
			// Start right away
			startCombat (defendingLocation, attackingFrom, scheduledCombatURN, attackingPlayer, attackingUnitURNs, mom);
		}

		log.exiting (CombatProcessingImpl.class.getName (), "initiateCombat");
	}
		
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
		
		// Find out who we're attacking - if an empty lair, we can get null here
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
			progressCombat (combatLocation, true, false, mom);
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
	 * 1 = Melee heroes
	 * 2 = Melee units
	 * 3 = Ranged heroes
	 * 4 = Ranged units
	 * 5 = Settlers
	 * 
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
		log.entering (CombatProcessingImpl.class.getName (), "mergeRowsIfTooMany", new Integer [] {unitsInRow.size (), maxRows});
		
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
		
		log.exiting (CombatProcessingImpl.class.getName (), "mergeRowsIfTooMany", unitsInRow.size ());
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
				final MomPersistentPlayerPrivateKnowledge atkPriv = (MomPersistentPlayerPrivateKnowledge) attackingPlayer.getPersistentPlayerPrivateKnowledge ();
				
				// createDefenders=true indicates that we're attacking monsters in a lair; these only exist in the player's memory for the duration
				// of the combat, so that's true for the player's memory on the server as well as sending to the client.  So need to create the units here.
				final MemoryUnit atkUnit;
				if ((startCombatMessage != null) && (startCombatMessage.isCreateDefenders ()) && (combatSide == UnitCombatSideID.DEFENDER))
				{
					// Add into attacker's memory - this copies *everything*, including the combat fields, so don't need to repeat it
					getFogOfWarDuplication ().copyUnit (trueUnit, atkPriv.getFogOfWarMemory ().getUnit ());
				}
				else
				{
					// Update values on existing unit in attacker's memory
					atkUnit = getUnitUtils ().findUnitURN (trueUnit.getUnitURN (), atkPriv.getFogOfWarMemory ().getUnit (), "placeCombatUnits-A");
				
					final CombatMapCoordinatesEx atkUnitPosition = new CombatMapCoordinatesEx ();
					atkUnitPosition.setX (coords.getX ());
					atkUnitPosition.setY (coords.getY ());
				
					atkUnit.setCombatLocation (combatLocation);
					atkUnit.setCombatPosition (atkUnitPosition);
					atkUnit.setCombatHeading (unitHeading);
					atkUnit.setCombatSide (combatSide);
				}
				
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
	 * Progresses the combat happening at the specified location.
	 * Will cycle through playing out AI players' combat turns until either the combat ends or it is a human players turn, at which
	 * point it will send a message to them to tell them to take their turn and then exit.
	 * 
	 * @param combatLocation Where the combat is taking place
	 * @param initialFirstTurn True if this is the initial call from startCombat; false if it is being called by a human playing ending their combat turn or turning auto on
	 * @param initialAutoControlHumanPlayer True if it is being called by a human player turning auto on
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void progressCombat (final OverlandMapCoordinatesEx combatLocation, final boolean initialFirstTurn,
		final boolean initialAutoControlHumanPlayer, final MomSessionVariables mom)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.entering (CombatProcessingImpl.class.getName (), "progressCombat", new String []
			{combatLocation.toString (), new Boolean (initialFirstTurn).toString (), new Boolean (initialAutoControlHumanPlayer).toString ()});

		final ServerGridCell tc = (ServerGridCell) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(combatLocation.getPlane ()).getRow ().get (combatLocation.getY ()).getCell ().get (combatLocation.getX ());

		// These get modified by the loop
		boolean firstTurn = initialFirstTurn;
		boolean autoControlHumanPlayer = initialAutoControlHumanPlayer;
		
		// We cannot safely determine the players involved until we've proved there are actually some units on each side.
		// Keep going until one side is wiped out or a human player needs to take their turn.
		boolean aiPlayerTurn = true;
		CombatPlayers combatPlayers = getCombatMapUtils ().determinePlayersInCombatFromLocation
			(combatLocation, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), mom.getPlayers ());
		while ((aiPlayerTurn) && (combatPlayers.bothFound ()))
		{
			// Who should take their turn next?
			// If human player hits Auto, then we want to play their turn for them through their AI, without switching players
			if (!autoControlHumanPlayer)
			{
				// Defender always goes first
				if (firstTurn)
				{
					firstTurn = false;
					tc.setCombatCurrentPlayer (combatPlayers.getDefendingPlayer ().getPlayerDescription ().getPlayerID ());
					log.finest ("First turn - Defender");
				}
				
				// Otherwise compare against the player from the last turn
				else if (combatPlayers.getDefendingPlayer ().getPlayerDescription ().getPlayerID ().equals (tc.getCombatCurrentPlayer ()))
				{
					tc.setCombatCurrentPlayer (combatPlayers.getAttackingPlayer ().getPlayerDescription ().getPlayerID ());
					log.finest ("Attacker's turn");
				}
				else
				{
					tc.setCombatCurrentPlayer (combatPlayers.getDefendingPlayer ().getPlayerDescription ().getPlayerID ());
					log.finest ("Defender's turn");
				}
				
				// Tell all human players involved in the combat who the new player is
				final SetCombatPlayerMessage msg = new SetCombatPlayerMessage ();
				msg.setCombatLocation (combatLocation);
				msg.setPlayerID (tc.getCombatCurrentPlayer ());
				
				if (combatPlayers.getDefendingPlayer ().getPlayerDescription ().isHuman ())
					((PlayerServerDetails) combatPlayers.getDefendingPlayer ()).getConnection ().sendMessageToClient (msg);

				if (combatPlayers.getAttackingPlayer ().getPlayerDescription ().isHuman ())
					((PlayerServerDetails) combatPlayers.getAttackingPlayer ()).getConnection ().sendMessageToClient (msg);
				
				// Give this player all their movement for this turn
				getUnitUtils ().resetUnitCombatMovement (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), tc.getCombatCurrentPlayer (), combatLocation, mom.getServerDB ());
				
				// Allow the player to cast a spell this turn
				tc.setSpellCastThisCombatTurn (null);
			}
			
			// AI or human player?
			if (tc.getCombatCurrentPlayer () != null)
			{
				final PlayerServerDetails combatCurrentPlayer = MultiplayerSessionServerUtils.findPlayerWithID (mom.getPlayers (), tc.getCombatCurrentPlayer (), "progressCombat");
				if ((combatCurrentPlayer.getPlayerDescription ().isHuman ()) && (!autoControlHumanPlayer))
				{
					// Human players' turn.
					// Nothing to do here - we already notified them to take their turn so the loop & this method will just
					// exit and combat will proceed when we receive messages from the player.
					aiPlayerTurn = false;
				}
				else
				{
					// Take AI players' turn
					getCombatAI ().aiCombatTurn (combatLocation, combatCurrentPlayer, mom);
					aiPlayerTurn = true;
					autoControlHumanPlayer = false;
				}
			}
			
			// Was either side wiped out yet?
			combatPlayers = getCombatMapUtils ().determinePlayersInCombatFromLocation
				(combatLocation, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), mom.getPlayers ());
		}
		
		log.exiting (CombatProcessingImpl.class.getName (), "progressCombat");
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
					
					// Deal with spells cast on the city:
					// 1) Any spells the defender had cast on the city must be enchantments - which unfortunately we don't get - so cancel these
					// 2) Any spells the attacker had cast on the city must be curses - we don't want to curse our own city - so cancel them
					// 3) Any spells a 3rd player (neither the defender nor attacker) had cast on the city must be curses - and I'm sure they'd like to continue cursing the new city owner :D
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
				else if (captureCityDecision == CaptureCityDecisionID.RAZE)
				{
					// Cancel all spells cast on the city regardless of owner
					getFogOfWarMidTurnChanges ().switchOffMaintainedSpellsInLocationOnServerAndClients
						(mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), combatLocation, combatLocation,
						attackingPlayer, defendingPlayer, 0, mom.getServerDB (), mom.getSessionDescription ());
					
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
				log.finest ("Defender won");
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
			
			// Don't check combatPosition here - DEAD units should have no position
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
					getFogOfWarMidTurnChanges ().killUnitOnServerAndClients (trueUnit, KillUnitActionID.FREE, null, trueMap, players, sd, db);
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
	 * @param tu Unit moving
	 * @param doubleMovementPoints Number of movement points it is moving in combat
	 */
	final void reduceMovementRemaining (final MemoryUnit tu, final int doubleMovementPoints)
	{
		tu.setDoubleCombatMovesLeft (Math.max (0, tu.getDoubleCombatMovesLeft () - doubleMovementPoints));
	}
	
	/**
	 * Once we have mapped out the directions and distances around the combat map and verified that our desired destination is
	 * fine, this routine actually handles sending the movement animations, performing the updates, and resolving any attacks.
	 *  
	 * This is separate so that it can be called directly by the AI.
	 *  
	 * @param tu The unit being moved
	 * @param moveTo The position within the combat map that the unit wants to move to (or attack)
	 * @param movementDirections The map of movement directions generated by calculateCombatMovementDistances
	 * @param movementTypes The map of movement types generated by calculateCombatMovementDistances
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void okToMoveUnitInCombat (final MemoryUnit tu, final CombatMapCoordinatesEx moveTo,
		final int [] [] movementDirections, final CombatMoveType [] [] movementTypes, final MomSessionVariables mom)
		throws MomException, PlayerNotFoundException, RecordNotFoundException, JAXBException, XMLStreamException
	{
		log.entering (CombatProcessingImpl.class.getName (), "okToMoveUnitInCombat");
		
		// Find who the two players are
		final OverlandMapCoordinatesEx combatLocation = (OverlandMapCoordinatesEx) tu.getCombatLocation ();
		final CombatPlayers combatPlayers = combatMapUtils.determinePlayersInCombatFromLocation
			(combatLocation, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), mom.getPlayers ());
		
		if (!combatPlayers.bothFound ())
			throw new MomException ("okToMoveUnitInCombat: One or other cell has no combat units left so player could not be determined");
		
		final PlayerServerDetails attackingPlayer = (PlayerServerDetails) combatPlayers.getAttackingPlayer ();
		final PlayerServerDetails defendingPlayer = (PlayerServerDetails) combatPlayers.getDefendingPlayer ();
		
		// Get the grid cell, so we can access the combat map
		final ServerGridCell combatCell = (ServerGridCell) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(combatLocation.getPlane ()).getRow ().get (combatLocation.getY ()).getCell ().get (combatLocation.getX ());
		
		// Ranged attacks always reduce movement remaining to zero and never result in the unit actually moving.
		// The value gets sent to the client by resolveAttack below.
		if (movementTypes [moveTo.getY ()] [moveTo.getX ()] == CombatMoveType.RANGED)
			tu.setDoubleCombatMovesLeft (0);
		else
		{
			// The value at each cell of the directions grid is the direction we need to have come from to get there.
			// So we need to start at the destination and follow backwards down the movement path until we get back to the From location.
			final List<Integer> directions = new ArrayList<Integer> ();
			final CombatMapCoordinatesEx movePath = new CombatMapCoordinatesEx ();
			movePath.setX (moveTo.getX ());
			movePath.setY (moveTo.getY ());
			
			while (!movePath.equals (tu.getCombatPosition ()))
			{
				final int d = movementDirections [movePath.getY ()] [movePath.getX ()];
				directions.add (0, d);
				
				if (!CoordinateSystemUtils.moveCoordinates (mom.getCombatMapCoordinateSystem (), movePath,
					CoordinateSystemUtils.normalizeDirection (mom.getCombatMapCoordinateSystem ().getCoordinateSystemType (), d+4)))
					
					throw new MomException ("okToMoveUnitInCombat: Server map tracing moved to a cell off the map (B)");
			}
			
			// We might not actually walk the last square if there is an enemy there - we might attack it instead, in which case we don't move.
			// However we still use up movement, as above.
			if ((movementTypes [moveTo.getY ()] [moveTo.getX ()] != CombatMoveType.MOVE) && (directions.size () > 0))
				directions.remove (directions.size () - 1);
			
			// Send direction messages to the client, reducing the unit's movement with each step
			// (so that's why we do this even if both players are AI)
			final MoveUnitInCombatMessage msg = new MoveUnitInCombatMessage ();
			msg.setUnitURN (tu.getUnitURN ());
			for (final int d : directions)
			{
				msg.setMoveFrom (movePath);
				msg.setDirection (d);
				
				// Move to the new cell
				// Message needs to contain the old coords, but we need the new coords to calc movementRemaining, so temporarily need both sets
				final CombatMapCoordinatesEx moveStep = new CombatMapCoordinatesEx ();
				moveStep.setX (movePath.getX ());
				moveStep.setY (movePath.getY ());

				if (!CoordinateSystemUtils.moveCoordinates (mom.getCombatMapCoordinateSystem (), moveStep, d))
					throw new MomException ("okToMoveUnitInCombat: Server map tracing moved to a cell off the map (F)");
				
				// How much movement did it take us to walk into this cell?
				reduceMovementRemaining (tu, unitCalculations.calculateDoubleMovementToEnterCombatTile
					(combatCell.getCombatMap ().getRow ().get (moveStep.getY ()).getCell ().get (moveStep.getX ()), mom.getServerDB ()));
				msg.setDoubleCombatMovesLeft (tu.getDoubleCombatMovesLeft ());
				
				// Only send this to the players involved in the combat.
				// Players not involved in the combat don't care where the units are positioned.
				if (attackingPlayer.getPlayerDescription ().isHuman ())
					attackingPlayer.getConnection ().sendMessageToClient (msg);

				if (defendingPlayer.getPlayerDescription ().isHuman ())
					defendingPlayer.getConnection ().sendMessageToClient (msg);
				
				// Update main copy of coords
				movePath.setX (moveStep.getX ());
				movePath.setY (moveStep.getY ());
			}
			
			// If the unit it making an attack, that takes half its total movement
			if (movementTypes [moveTo.getY ()] [moveTo.getX ()] != CombatMoveType.MOVE)
				reduceMovementRemaining (tu, mom.getServerDB ().findUnit (tu.getUnitID (), "okToMoveUnitInCombat").getDoubleMovement () / 2);
			
			// Actually put the units in that location on the server
			tu.setCombatPosition (movePath);
		
			// Update attacker's memory on server
			final CombatMapCoordinatesEx movePathAttackersMemory = new CombatMapCoordinatesEx ();
			movePathAttackersMemory.setX (movePath.getX ());
			movePathAttackersMemory.setY (movePath.getY ());
			
			final MomPersistentPlayerPrivateKnowledge attackerPriv = (MomPersistentPlayerPrivateKnowledge) attackingPlayer.getPersistentPlayerPrivateKnowledge ();
			getUnitUtils ().findUnitURN (tu.getUnitURN (), attackerPriv.getFogOfWarMemory ().getUnit (), "okToMoveUnitInCombat-A").setCombatPosition (movePathAttackersMemory);

			// Update defender's memory on server
			final CombatMapCoordinatesEx movePathDefendersMemory = new CombatMapCoordinatesEx ();
			movePathDefendersMemory.setX (movePath.getX ());
			movePathDefendersMemory.setY (movePath.getY ());
			
			final MomPersistentPlayerPrivateKnowledge defenderPriv = (MomPersistentPlayerPrivateKnowledge) defendingPlayer.getPersistentPlayerPrivateKnowledge ();
			getUnitUtils ().findUnitURN (tu.getUnitURN (), defenderPriv.getFogOfWarMemory ().getUnit (), "okToMoveUnitInCombat-D").setCombatPosition (movePathDefendersMemory);
		}
		
		// Anything special to do?
		switch (movementTypes [moveTo.getY ()] [moveTo.getX ()])
		{
			case MELEE:
				resolveAttack (tu, getUnitUtils ().findAliveUnitInCombatAt (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), combatLocation, moveTo),
					attackingPlayer, defendingPlayer,
					movementDirections [moveTo.getY ()] [moveTo.getX ()],
					false, combatLocation, mom);
				break;
				
			case RANGED:
				resolveAttack (tu, getUnitUtils ().findAliveUnitInCombatAt (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (),
					combatLocation, moveTo), attackingPlayer, defendingPlayer,
					CoordinateSystemUtils.determineDirectionTo (mom.getCombatMapCoordinateSystem (), tu.getCombatPosition (), moveTo),
					true, combatLocation, mom);
				break;
				
			case MOVE:
				// Nothing special to do
				break;

			case CANNOT_MOVE:
				// Should be impossible
				break;
		}
		
		log.exiting (CombatProcessingImpl.class.getName (), "okToMoveUnitInCombat");
	}
	
	/**
	 * Just deals with making sure we only send to human players
	 * 
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param msg Message to send
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	final void sendDamageCalculationMessage (final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final DamageCalculationMessage msg)
		throws JAXBException, XMLStreamException
	{
		if (attackingPlayer.getPlayerDescription ().isHuman ())
			attackingPlayer.getConnection ().sendMessageToClient (msg);
		
		if (defendingPlayer.getPlayerDescription ().isHuman ())
			defendingPlayer.getConnection ().sendMessageToClient (msg);		
	}
	
	/**
	 * Performs one attack in combat.
	 * If a close combat attack, also resolves the defender retaliating.
	 * Also checks to see if the attack results in either side being wiped out, in which case ends the combat.
	 * 
	 * @param attacker Unit making the attack
	 * @param defender Unit being hit
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackerDirection The direction the attacker needs to turn to in order to be facing the defender
	 * @param isRangedAttack True for ranged attacks; false for close combat attacks
	 * @param combatLocation Where the combat is taking place
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	final void resolveAttack (final MemoryUnit attacker, final MemoryUnit defender,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final int attackerDirection, final boolean isRangedAttack,
		final OverlandMapCoordinatesEx combatLocation, final MomSessionVariables mom)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.entering (CombatProcessingImpl.class.getName (), "resolveAttack");

		// We send this a couple of times for different parts of the calculation, so initialize it here
		final DamageCalculationMessage damageCalculationMsg = new DamageCalculationMessage ();
		damageCalculationMsg.setAttackerUnitURN (attacker.getUnitURN ());
		damageCalculationMsg.setDefenderUnitURN (defender.getUnitURN ());
		
		if (isRangedAttack)
			damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.RANGED_ATTACK);
		else
			damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.MELEE_ATTACK);
		
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		
		// Make the units face each other
		attacker.setCombatHeading (attackerDirection);
		defender.setCombatHeading (CoordinateSystemUtils.normalizeDirection (mom.getCombatMapCoordinateSystem ().getCoordinateSystemType (), attackerDirection + 4));
		
		// Both attack simultaneously, before damage is applied
		// Defender only retaliates against close combat attacks, not ranged attacks
		final int damageToDefender;
		final int damageToAttacker;
		if (isRangedAttack)
		{
			damageToDefender = calculateDamage (attacker, defender, attackingPlayer, defendingPlayer,
				CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_RANGED_ATTACK, damageCalculationMsg, mom.getPlayers (),
					mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), mom.getServerDB ());
			damageToAttacker = 0;
		}
		else
		{
			damageToDefender = calculateDamage (attacker, defender, attackingPlayer, defendingPlayer,
				CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_MELEE_ATTACK, damageCalculationMsg, mom.getPlayers (),
				mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), mom.getServerDB ());
			
			damageToAttacker = calculateDamage (defender, attacker, defendingPlayer, attackingPlayer,
				CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_MELEE_ATTACK, damageCalculationMsg, mom.getPlayers (),
				mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), mom.getServerDB ());
		}
		
		// Now apply damage
		defender.setDamageTaken (defender.getDamageTaken () + damageToDefender);
		attacker.setDamageTaken (attacker.getDamageTaken () + damageToAttacker);
		
		// Update damage taken in player's memory on server, and on all clients who can see the unit.
		// This includes both players involved in the combat (who will queue this up as an animation), and players who aren't involved in the combat but
		// can see the units fighting (who will update the damage immediately).
		// This also sends the number of combat movement points the attacker has left.
		getFogOfWarMidTurnChanges ().sendCombatDamageToClients (attacker, defender, attackingPlayer, defendingPlayer, isRangedAttack,
			mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), combatLocation, mom.getServerDB (), mom.getSessionDescription ());
		
		// Now we know who the COMBAT attacking and defending players are, we can work out whose
		// is whose unit - because it could be the defending players' unit making the attack in combat.
		// We have to know this, because if both units die at the same time, the defender wins the combat.
		final MemoryUnit attackingPlayerUnit;
		final MemoryUnit defendingPlayerUnit;
		if (attacker.getOwningPlayerID () == attackingPlayer.getPlayerDescription ().getPlayerID ())
		{
			attackingPlayerUnit = attacker;
			defendingPlayerUnit = defender;
		}
		else
		{
			attackingPlayerUnit = defender;
			defendingPlayerUnit = attacker;
		}
		
		// Kill off either unit if dead.
		// We don't need to notify the clients of this separately, clients can tell from the damage taken values above whether the units are dead or not, whether
		// or not they're involved in the combat.
		boolean combatEnded = false;
		if (getUnitCalculations ().calculateAliveFigureCount (attackingPlayerUnit, mom.getPlayers (),
			mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), mom.getServerDB ()) == 0)
		{
			getFogOfWarMidTurnChanges ().killUnitOnServerAndClients (attackingPlayerUnit, null, UntransmittedKillUnitActionID.COMBAT_DAMAGE,
				mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());
			
			getFogOfWarMidTurnChanges ().grantExperienceToUnitsInCombat (combatLocation, UnitCombatSideID.DEFENDER,
				mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (),
				mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ());
			
			// If the attacker is now wiped out, this is the last record we will ever have of who the attacking player was, so we have to deal with tidying up the combat now
			if (countUnitsInCombat (combatLocation, UnitCombatSideID.ATTACKER, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ()) == 0)
			{
				combatEnded = true;
				combatEnded (combatLocation, attackingPlayer, defendingPlayer, defendingPlayer,	// <-- who won
					null, mom);
			}
		}

		if (getUnitCalculations ().calculateAliveFigureCount (defendingPlayerUnit, mom.getPlayers (),
			mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), mom.getServerDB ()) == 0)
		{
			getFogOfWarMidTurnChanges ().killUnitOnServerAndClients (defendingPlayerUnit, null, UntransmittedKillUnitActionID.COMBAT_DAMAGE,
				mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());
			
			getFogOfWarMidTurnChanges ().grantExperienceToUnitsInCombat (combatLocation, UnitCombatSideID.ATTACKER,
				mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (),
				mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ());
			
			// If the defender is now wiped out, this is the last record we will ever have of who the defending player was, so we have to deal with tidying up the combat now.
			// If attacker was also wiped out then we've already done this - the defender won by default.
			if ((!combatEnded) && (countUnitsInCombat (combatLocation, UnitCombatSideID.DEFENDER, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ()) == 0))
				combatEnded (combatLocation, attackingPlayer, defendingPlayer, attackingPlayer,	// <-- who won
					null, mom);
		}
		
		log.exiting (CombatProcessingImpl.class.getName (), "resolveAttack");	
	}

	/**
	 * @param combatLocation Location that combat is taking place
	 * @param combatSide Which side to count
	 * @param trueUnits List of true units
	 * @return How many units are still left alive in combat on the requested side
	 */
	final int countUnitsInCombat (final OverlandMapCoordinatesEx combatLocation, final UnitCombatSideID combatSide,
		final List<MemoryUnit> trueUnits)
	{
		log.entering (CombatProcessingImpl.class.getName (), "countUnitsInCombat",
			new String [] {combatLocation.toString (), combatSide.name ()});
			
		int count = 0;
		for (final MemoryUnit trueUnit : trueUnits)
			if ((trueUnit.getStatus () == UnitStatusID.ALIVE) && (combatLocation.equals (trueUnit.getCombatLocation ())) &&
				(trueUnit.getCombatSide () == combatSide) && (trueUnit.getCombatPosition () != null))
					
				count++;

		log.exiting (CombatProcessingImpl.class.getName (), "countUnitsInCombat", count);
		return count;
	}
	
	/**
	 * NB. This doesn't actually apply the damage, so that both the attack and counterattack damage can be calculated and applied at the same time
	 * 
	 * @param attacker Unit making the attack
	 * @param defender Unit being hit
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackAttributeID The attribute being used to attack, i.e. UA01 (swords) or UA02 (ranged)
	 * @param damageCalculationMsg Partially pre-filled damage calc message so that it can be reused
	 * @param players Players list
	 * @param spells Known spells
	 * @param combatAreaEffects Known combat area effects
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	final int calculateDamage (final MemoryUnit attacker, final MemoryUnit defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final String attackAttributeID, final DamageCalculationMessage damageCalculationMsg, final List<PlayerServerDetails> players,
		final List<MemoryMaintainedSpell> spells, final List<MemoryCombatAreaEffect> combatAreaEffects, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.entering (CombatProcessingImpl.class.getName (), "resolveAttack", new String []
			{new Integer (attacker.getUnitURN ()).toString (), new Integer (defender.getUnitURN ()).toString (), attackAttributeID});
		
		// Store values straight into the message
		// The attacker and defender may be switched, so redo the message from scratch
		damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.ATTACK_AND_DEFENCE_STATISTICS);
		damageCalculationMsg.setAttackerUnitURN (attacker.getUnitURN ());
		damageCalculationMsg.setDefenderUnitURN (defender.getUnitURN ());
		damageCalculationMsg.setAttackAttributeID (attackAttributeID);
		
		// How many potential hits can we make - See page 285 in the strategy guide
		damageCalculationMsg.setAttackerFigures (getUnitCalculations ().calculateAliveFigureCount (attacker, players, spells, combatAreaEffects, db));
		damageCalculationMsg.setAttackStrength (getUnitUtils ().getModifiedAttributeValue (attacker, attackAttributeID,
			MomUnitAttributeComponent.ALL, MomUnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db));
		
		damageCalculationMsg.setPotentialDamage (damageCalculationMsg.getAttackerFigures () * damageCalculationMsg.getAttackStrength ());
		
		damageCalculationMsg.setChanceToHit (3 + getUnitUtils ().getModifiedAttributeValue (attacker, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_PLUS_TO_HIT,
			MomUnitAttributeComponent.ALL, MomUnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db));
		
		damageCalculationMsg.setTenTimesAverageDamage (damageCalculationMsg.getPotentialDamage () * damageCalculationMsg.getChanceToHit ());
		
		// How many actually hit
		int actualDamage = 0;
		for (int swingNo = 0; swingNo < damageCalculationMsg.getPotentialDamage (); swingNo++)
			if (getRandomUtils ().nextInt (10) < damageCalculationMsg.getChanceToHit ())
				actualDamage++;
		
		damageCalculationMsg.setActualDamage (actualDamage);
		
		// Set up defender stats
		damageCalculationMsg.setDefenderFigures (getUnitCalculations ().calculateAliveFigureCount (defender, players, spells, combatAreaEffects, db));
		damageCalculationMsg.setDefenceStrength (getUnitUtils ().getModifiedAttributeValue (defender, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_DEFENCE,
			MomUnitAttributeComponent.ALL, MomUnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db));

		damageCalculationMsg.setChanceToDefend (3 + getUnitUtils ().getModifiedAttributeValue (defender, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK,
			MomUnitAttributeComponent.ALL, MomUnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db));
		
		damageCalculationMsg.setTenTimesAverageBlock (damageCalculationMsg.getDefenceStrength () * damageCalculationMsg.getChanceToDefend ());
		
		// Dish out damage - See page 287 in the strategy guide
		// We can't do all defending in one go, each figure only gets to use its shields if the previous figure dies.
		// e.g. a unit of 8 spearmen has to take 2 hits, if all 8 spearmen get to try to block the 2 hits, they might not even lose 1 figure.
		// However only the first unit gets to use its shield, even if it blocks 1 hit it will be killed by the 2nd hit.
		int totalHits = 0;
		int defendingFiguresRemaining = damageCalculationMsg.getDefenderFigures ();
		int hitPointsRemainingOfFirstFigure = getUnitCalculations ().calculateHitPointsRemainingOfFirstFigure (defender, players, spells, combatAreaEffects, db);
		int hitsLeftToApply = actualDamage;
		final StringBuffer actualBlockedHits = new StringBuffer ();
		
		while ((defendingFiguresRemaining > 0) && (hitsLeftToApply > 0))
		{
			// New figure taking damage, so it gets to try to block some hits
			int thisBlockedHits = 0;
			for (int blockNo = 0; blockNo < damageCalculationMsg.getDefenceStrength (); blockNo++)
				if (getRandomUtils ().nextInt (10) < damageCalculationMsg.getChanceToDefend ())
					thisBlockedHits++;
			
			hitsLeftToApply = hitsLeftToApply - thisBlockedHits;
			
			if (actualBlockedHits.length () > 0)
				actualBlockedHits.append (",");
			
			actualBlockedHits.append (thisBlockedHits);
			
			// If any damage was not blocked by shields then it goes to health
			if (hitsLeftToApply > 0)
			{
				// Work out how many hits the current figure will take
				final int hitsOnThisFigure = Math.min (hitsLeftToApply, hitPointsRemainingOfFirstFigure);
				
				// Update counters for next figure.
				// Note it doesn't matter that we're decreasing defendingFigures even if the figure didn't die, because in that case Hits
				// will now be zero and the loop with exit, so the values of these variables won't matter at all, only the totalHits return value does.
				hitsLeftToApply = hitsLeftToApply - hitsOnThisFigure;
				totalHits = totalHits + hitsOnThisFigure;
				defendingFiguresRemaining--;
				hitPointsRemainingOfFirstFigure = getUnitUtils ().getModifiedAttributeValue (defender, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_HIT_POINTS,
					MomUnitAttributeComponent.ALL, MomUnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db);
			}
		}
		
		damageCalculationMsg.setActualBlockedHits (actualBlockedHits.toString ());
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		
		log.exiting (CombatProcessingImpl.class.getName (), "resolveAttack", totalHits);
		return totalHits;
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

	/**
	 * @return Combat map utils
	 */
	public final CombatMapUtils getCombatMapUtils ()
	{
		return combatMapUtils;
	}
	
	/**
	 * @param utils Combat map utils
	 */
	public final void setCombatMapUtils (final CombatMapUtils utils)
	{
		combatMapUtils = utils;
	}

	/**
	 * @return Combat AI
	 */
	public final CombatAI getCombatAI ()
	{
		return combatAI;
	}

	/**
	 * @param ai Combat AI
	 */
	public final void setCombatAI (final CombatAI ai)
	{
		combatAI = ai;
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
}
