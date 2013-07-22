package momime.server.process;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.calculations.MomCityCalculations;
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
import momime.common.messages.v0_9_4.CaptureCityDecisionID;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryGridCell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.TurnSystem;
import momime.common.messages.v0_9_4.UnitCombatSideID;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.MomServerCityCalculations;
import momime.server.calculations.MomServerResourceCalculations;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_4.Plane;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.FogOfWarProcessing;
import momime.server.messages.ServerMemoryGridCellUtils;
import momime.server.messages.v0_9_4.ServerGridCell;
import momime.server.utils.CityServerUtils;
import momime.server.utils.OverlandMapServerUtils;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Routines dealing with initiating, progressing and ending combats
 */
public final class CombatProcessingImpl
{
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
				throw new UnsupportedOperationException ("combatEnded doesn't know what to do with scheduled combats yet");
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
}
