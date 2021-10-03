package momime.server.fogofwar;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CitySpellEffect;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DamageResolutionTypeID;
import momime.common.database.FogOfWarSetting;
import momime.common.database.FogOfWarValue;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.FogOfWarStateID;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.UnitDamage;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.AddBuildingMessage;
import momime.common.messages.servertoclient.AddOrUpdateCombatAreaEffectMessage;
import momime.common.messages.servertoclient.AddOrUpdateMaintainedSpellMessage;
import momime.common.messages.servertoclient.AddOrUpdateUnitMessage;
import momime.common.messages.servertoclient.ApplyDamageMessage;
import momime.common.messages.servertoclient.ApplyDamageMessageUnit;
import momime.common.messages.servertoclient.CancelCombatAreaEffectMessage;
import momime.common.messages.servertoclient.DestroyBuildingMessage;
import momime.common.messages.servertoclient.FogOfWarVisibleAreaChangedMessage;
import momime.common.messages.servertoclient.KillUnitMessage;
import momime.common.messages.servertoclient.SwitchOffMaintainedSpellMessage;
import momime.common.messages.servertoclient.UpdateCityMessage;
import momime.common.messages.servertoclient.UpdateCityMessageData;
import momime.common.messages.servertoclient.UpdateCombatMapMessage;
import momime.common.messages.servertoclient.UpdateTerrainMessage;
import momime.common.messages.servertoclient.UpdateTerrainMessageData;
import momime.common.utils.CombatMapUtils;
import momime.common.utils.CombatPlayers;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.PendingMovementUtils;
import momime.common.utils.UnitUtils;
import momime.server.calculations.FogOfWarCalculations;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.mapgenerator.CombatMapGenerator;
import momime.server.messages.MomGeneralServerKnowledge;
import momime.server.utils.UnitServerUtils;

/**
 * This contains all methods that allow changes in the server's true memory to be replicated into each player's memory and send update messages to each client
 * i.e. methods for when the true values change (or are added or removed) but the visible area that each player can see does not change
 */
public final class FogOfWarMidTurnChangesImpl implements FogOfWarMidTurnChanges
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (FogOfWarMidTurnChangesImpl.class);
	
	/** Single cell FOW calculations */
	private FogOfWarCalculations fogOfWarCalculations;
	
	/** FOW visibility checks */
	private FogOfWarMidTurnVisibility fogOfWarMidTurnVisibility;

	/** FOW duplication utils */
	private FogOfWarDuplication fogOfWarDuplication;
	
	/** Main FOW update routine */
	private FogOfWarProcessing fogOfWarProcessing;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;
	
	/** MemoryBuilding utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** Memory CAE utils */
	private MemoryCombatAreaEffectUtils memoryCombatAreaEffectUtils;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** Pending movement utils */
	private PendingMovementUtils pendingMovementUtils;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;
	
	/** Combat map utils */
	private CombatMapUtils combatMapUtils;
	
	/** Map generator */
	private CombatMapGenerator combatMapGenerator;
	
	/**
	 * After setting the various terrain values in the True Map, this routine copies and sends the new value to players who can see it
	 * i.e. the caller must update the True Map value themselves before calling this
	 *
	 * @param trueTerrain True terrain map
	 * @param players List of players in the session
	 * @param coords Location of the terrain that has been updated
	 * @param terrainAndNodeAurasSetting Terrain and Node Auras FOW setting from session description
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 */
	@Override
	public final void updatePlayerMemoryOfTerrain (final MapVolumeOfMemoryGridCells trueTerrain,
		final List<PlayerServerDetails> players, final MapCoordinates3DEx coords,
		final FogOfWarValue terrainAndNodeAurasSetting)
		throws JAXBException, XMLStreamException
	{
		final MemoryGridCell tc = trueTerrain.getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ());

		// First build the message
		final UpdateTerrainMessageData terrainMsg = new UpdateTerrainMessageData ();
		terrainMsg.setMapLocation (coords);
		terrainMsg.setTerrainData (tc.getTerrainData ());

		final UpdateTerrainMessage terrainMsgContainer = new UpdateTerrainMessage ();
		terrainMsgContainer.setData (terrainMsg);

		// Check which players can see the terrain
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			final FogOfWarStateID state = priv.getFogOfWar ().getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ());

			if (getFogOfWarCalculations ().canSeeMidTurn (state, terrainAndNodeAurasSetting))
			{
				// Update player's memory on server
				final MemoryGridCell mc = priv.getFogOfWarMemory ().getMap ().getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ());
				if (getFogOfWarDuplication ().copyTerrainAndNodeAura (tc, mc))

					// Update player's memory on client
					if (thisPlayer.getPlayerDescription ().isHuman ())
						thisPlayer.getConnection ().sendMessageToClient (terrainMsgContainer);
			}
		}
	}

	/**
	 * After setting the various terrain values in the True Map, this routine copies and sends the new value to players who can see it
	 * i.e. the caller must update the True Map value themselves before calling this
	 *
	 * @param trueTerrain True terrain map
	 * @param players List of players in the session
	 * @param coords Location of the city that has been updated
	 * @param fogOfWarSettings Fog of War settings from session description
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 */
	@Override
	public final void updatePlayerMemoryOfCity (final MapVolumeOfMemoryGridCells trueTerrain,
		final List<PlayerServerDetails> players, final MapCoordinates3DEx coords, final FogOfWarSetting fogOfWarSettings)
		throws JAXBException, XMLStreamException
	{
		final MemoryGridCell tc = trueTerrain.getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ());

		// First build the message
		final UpdateCityMessageData cityMsg = new UpdateCityMessageData ();
		cityMsg.setMapLocation (coords);

		final UpdateCityMessage cityMsgContainer = new UpdateCityMessage ();
		cityMsgContainer.setData (cityMsg);

		// Check which players can see the city
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			final FogOfWarStateID state = priv.getFogOfWar ().getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ());

			if (getFogOfWarCalculations ().canSeeMidTurn (state, fogOfWarSettings.getCitiesSpellsAndCombatAreaEffects ()))
			{
				// Update player's memory on server
				final MemoryGridCell mc = priv.getFogOfWarMemory ().getMap ().getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ());

				final boolean includeCurrentlyConstructing;
				final boolean includeProductionSoFar;
				if (tc.getCityData () == null)
				{
					includeCurrentlyConstructing = false;
					includeProductionSoFar = false;
				}
				else
				{
					includeCurrentlyConstructing = (thisPlayer.getPlayerDescription ().getPlayerID () == tc.getCityData ().getCityOwnerID ()) ||
						(fogOfWarSettings.isSeeEnemyCityConstruction ());
					
					includeProductionSoFar = thisPlayer.getPlayerDescription ().getPlayerID () == tc.getCityData ().getCityOwnerID ();
				}

				if (getFogOfWarDuplication ().copyCityData (tc, mc, includeCurrentlyConstructing, includeProductionSoFar))

					// Update player's memory on client
					if (thisPlayer.getPlayerDescription ().isHuman ())
					{
						// Note unlike the terrain msg which we can build once and send to each applicable player, the data for the city msg
						// needs to be reset for each player, since their visibility of the currentlyConstructing value may be different
						cityMsg.setCityData (mc.getCityData ());
						thisPlayer.getConnection ().sendMessageToClient (cityMsgContainer);
					}
			}
		}
	}

	/**
	 * After updating the true copy of a spell, this routine copies and sends the new value to players who can see it
	 *
	 * @param trueSpell True spell that was updated
	 * @param gsk Server knowledge structure
	 * @param players List of players in the session
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws PlayerNotFoundException If we can't find one of the players
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 */
	@Override
	public final void updatePlayerMemoryOfSpell (final MemoryMaintainedSpell trueSpell, final MomGeneralServerKnowledge gsk,
		final List<PlayerServerDetails> players, final CommonDatabase db, final MomSessionDescription sd)
		throws JAXBException, XMLStreamException, PlayerNotFoundException, RecordNotFoundException, MomException
	{
		// First build the message
		final AddOrUpdateMaintainedSpellMessage spellMsg = new AddOrUpdateMaintainedSpellMessage ();
		spellMsg.setMaintainedSpell (trueSpell);

		// Check which players can see the spell
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			
			if (getFogOfWarMidTurnVisibility ().canSeeSpellMidTurn (trueSpell, gsk.getTrueMap ().getMap (), gsk.getTrueMap ().getUnit (), thisPlayer, db, sd.getFogOfWarSetting ()))
			{
				// Update player's memory on server
				if (getFogOfWarDuplication ().copyMaintainedSpell (trueSpell, priv.getFogOfWarMemory ().getMaintainedSpell ()))

					// Update player's memory on client
					if (thisPlayer.getPlayerDescription ().isHuman ())
						thisPlayer.getConnection ().sendMessageToClient (spellMsg);
			}
		}

		// The stolen spell might be Awareness, Nature Awareness, Nature's Eye, or a curse on an enemy city, so might affect the fog of wag of the player who cast it.
		// Technically we should also do this for the old player who lost the spell, but its fine, they will end up losing sight of it soon enough.
		final PlayerServerDetails castingPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (players, trueSpell.getCastingPlayerID (), "updatePlayerMemoryOfSpell");
		getFogOfWarProcessing ().updateAndSendFogOfWar (gsk.getTrueMap (), castingPlayer, players, "updatePlayerMemoryOfSpell", sd, db);
	}
	
	/**
	 * After updating the true copy of a CAE, this routine copies and sends the new value to players who can see it
	 *
	 * @param trueCAE True CAE that was updated
	 * @param players List of players in the session
	 * @param sd Session description
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 */
	@Override
	public final void updatePlayerMemoryOfCombatAreaEffect (final MemoryCombatAreaEffect trueCAE, final List<PlayerServerDetails> players,
		final MomSessionDescription sd) throws JAXBException, XMLStreamException
	{
		// First build the message
		final AddOrUpdateCombatAreaEffectMessage caeMsg = new AddOrUpdateCombatAreaEffectMessage ();
		caeMsg.setMemoryCombatAreaEffect (trueCAE);

		// Check which players can see the spell
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			
			if (getFogOfWarMidTurnVisibility ().canSeeCombatAreaEffectMidTurn (trueCAE, priv.getFogOfWar (), sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ()))
			{
				// Update player's memory on server
				if (getFogOfWarDuplication ().copyCombatAreaEffect (trueCAE, priv.getFogOfWarMemory ().getCombatAreaEffect ()))

					// Update player's memory on client
					if (thisPlayer.getPlayerDescription ().isHuman ())
						thisPlayer.getConnection ().sendMessageToClient (caeMsg);
			}
		}
	}
	
	/**
	 * Adds a unit to the server's true memory, and checks who can see it - sending a message to update the client of any human players who can see it
	 *
	 * Heroes are added using this method during game startup - at which point they're added only on the server and they're off
	 * the map (null location) - so heroes are NEVER sent to the client using this method, meaning that we don't need to worry about sending skill lists with this method
	 *
	 * @param gsk Server knowledge structure to add the unit to
	 * @param unitID Type of unit to create
	 * @param locationToAddUnit Location to add the new unit; can be null for adding heroes that haven't been summoned yet
	 * @param buildingsLocation Location the unit was built - might be different from locationToAddUnit if the city is full and the unit got bumped to an adjacent tile; passed as null for units not built in cities such as summons
	 * @param overrideStartingExperience Set this to not calculate startingExperience from buildings at buildingLocation and instead just take this value
	 * @param combatLocation The location of the combat that this unit is being summoned into; null for anything other than combat summons
	 * @param unitOwner Player who will own the new unit
	 * @param initialStatus Initial status of the unit, typically ALIVE
	 * @param players List of players in this session, this can be passed in null for when units are being added to the map pre-game
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @return Newly created unit
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final MemoryUnit addUnitOnServerAndClients (final MomGeneralServerKnowledge gsk,
		final String unitID, final MapCoordinates3DEx locationToAddUnit, final MapCoordinates3DEx buildingsLocation, final Integer overrideStartingExperience,
		final MapCoordinates3DEx combatLocation, final PlayerServerDetails unitOwner, final UnitStatusID initialStatus, final List<PlayerServerDetails> players,
		final MomSessionDescription sd, final CommonDatabase db)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException
	{
		// There's a bunch of other unit statuses that don't make sense to use here - so worth checking this
		if ((initialStatus != UnitStatusID.NOT_GENERATED) && (initialStatus != UnitStatusID.ALIVE))
			throw new MomException ("addUnitOnServerAndClients: Invalid initial status of " + initialStatus);

		final MomPersistentPlayerPublicKnowledge unitOwnerPPK = (MomPersistentPlayerPublicKnowledge) unitOwner.getPersistentPlayerPublicKnowledge ();

		// Check how much experience this unit should have
		// Note the reason we pass in buildingsLocation separately from locationToAddUnit is in case a city is full and a unit gets bumped
		// to the outside - it still needs to get the bonus from the buildings back in the city
		final int startingExperience;
		final Integer weaponGrade;
		if (overrideStartingExperience != null)
		{
			startingExperience = overrideStartingExperience;
			weaponGrade = null;
		}
		else if (buildingsLocation != null)
		{
			startingExperience = getMemoryBuildingUtils ().experienceFromBuildings (gsk.getTrueMap ().getBuilding (), buildingsLocation, db);
			weaponGrade = getUnitCalculations ().calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort
				(gsk.getTrueMap ().getBuilding (), gsk.getTrueMap ().getMap (), buildingsLocation, unitOwnerPPK.getPick (), sd.getOverlandMapSize (), db);
		}
		else
		{
			startingExperience = 0;
			weaponGrade = null;
		}

		// Add on server
		// Even for heroes, we load in their default skill list - this is how heroes default skills are loaded during game startup
		final MemoryUnit newUnit = getUnitServerUtils ().createMemoryUnit (unitID, gsk.getNextFreeUnitURN (), weaponGrade, startingExperience, db);
		newUnit.setOwningPlayerID (unitOwner.getPlayerDescription ().getPlayerID ());
		newUnit.setCombatLocation (combatLocation);

		gsk.setNextFreeUnitURN (gsk.getNextFreeUnitURN () + 1);
		gsk.getTrueMap ().getUnit ().add (newUnit);

		if (initialStatus == UnitStatusID.ALIVE)
			updateUnitStatusToAliveOnServerAndClients (newUnit, locationToAddUnit, unitOwner, players, gsk.getTrueMap (), sd, db);
		else
			newUnit.setStatus (initialStatus);

		return newUnit;
	}

	/**
	 * When we summon a hero, we don't 'add' it, the unit object already exists - but we still need to perform similar updates
	 * to addUnitOnServerAndClients to set the unit's location, see the area it can see, status and tell clients to update the same
	 *
	 * @param trueUnit The unit to set to alive
	 * @param locationToAddUnit Location to add the new unit, must be filled in
	 * @param unitOwner Player who will own the new unit, note the reason this has to be passed in separately is because the players list is allowed to be null
	 * @param players List of players in this session, this can be passed in null for when units are being added to the map pre-game
	 * @param trueMap True terrain, buildings, spells and so on as known only to the server
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void updateUnitStatusToAliveOnServerAndClients (final MemoryUnit trueUnit, final MapCoordinates3DEx locationToAddUnit,
		final PlayerServerDetails unitOwner, final List<PlayerServerDetails> players, final FogOfWarMemory trueMap,
		final MomSessionDescription sd, final CommonDatabase db)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException
	{
		// Update on server
		final MapCoordinates3DEx unitLocation = new MapCoordinates3DEx (locationToAddUnit);

		trueUnit.setUnitLocation (unitLocation);
		trueUnit.setStatus (UnitStatusID.ALIVE);

		// What can the new unit see? (it may expand the unit owner's vision to see things that they couldn't previously)
		if ((players != null) && (trueUnit.getCombatLocation () == null))
			getFogOfWarProcessing ().updateAndSendFogOfWar (trueMap, unitOwner, players, "updateUnitStatusToAliveOnServerAndClients", sd, db);

		// Tell clients?
		// Player list can be null, we use this for pre-adding units to the map before the fog of war has even been set up
		if (players != null)
			updatePlayerMemoryOfUnit (trueUnit, trueMap.getMap (), players, db, sd.getFogOfWarSetting (), null);
	}

	/**
	 * Kills off a unit, on the server and usually also on whichever clients can see the unit
	 * There are a number of different possibilities for how we need to handle this, depending on how the unit died and whether it is a regular/summoned unit or a hero
	 *
	 * @param trueUnit The unit to set to alive
	 * @param untransmittedAction Method by which the unit is being killed
	 * @param players List of players in this session, this can be passed in null for when units are being added to the map pre-game
	 * @param trueMap True terrain, buildings, spells and so on as known only to the server
	 * @param fogOfWarSettings Fog of war settings from session description
	 * @param db Lookup lists built over the XML database
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void killUnitOnServerAndClients (final MemoryUnit trueUnit, final KillUnitActionID untransmittedAction,
		final FogOfWarMemory trueMap, final List<PlayerServerDetails> players,
		final FogOfWarSetting fogOfWarSettings, final CommonDatabase db)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException
	{
		final boolean isHero = db.findUnit (trueUnit.getUnitID (), "killUnitOnServerAndClients").getUnitMagicRealm ().equals
			(CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
		
		// If the unit was a hero dying in combat, move any items they had into the pool for the winner of the combat to claim
		final ServerGridCellEx gc = (trueUnit.getCombatLocation () == null) ? null :
			(ServerGridCellEx) trueMap.getMap ().getPlane ().get (trueUnit.getCombatLocation ().getZ ()).getRow ().get
				(trueUnit.getCombatLocation ().getY ()).getCell ().get (trueUnit.getCombatLocation ().getX ());

		if ((trueUnit.getCombatLocation () != null) && (untransmittedAction != KillUnitActionID.PERMANENT_DAMAGE))
			trueUnit.getHeroItemSlot ().stream ().filter (slot -> (slot.getHeroItem () != null)).forEach (slot ->
			{
				gc.getItemsFromHeroesWhoDiedInCombat ().add (slot.getHeroItem ());
				slot.setHeroItem (null);
			});
		
		// Check which players could see the unit
		for (final PlayerServerDetails player : players)
		{
			if (getFogOfWarMidTurnVisibility ().canSeeUnitMidTurn (trueUnit, trueMap.getMap (), player, db, fogOfWarSettings))
			{
				final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
				
				// Remove unit from players' memory on server - this doesn't suffer from the issue described below so we can just do it
				getPendingMovementUtils ().removeUnitFromAnyPendingMoves (priv.getPendingMovement (), trueUnit.getUnitURN ());
				getUnitUtils ().beforeKillingUnit (priv.getFogOfWarMemory (), trueUnit.getUnitURN ());	// Removes spells cast on unit
				
				// Map the transmittedAction to a unit status
				final UnitStatusID newStatusInPlayersMemoryOnServer;
				final UnitStatusID newStatusInPlayersMemoryOnClient;
				
				switch (untransmittedAction)
				{
					// Heroes are killed outright on the clients (even if ours, and just dismissing him and may resummon him later), but return to 'Generated' status on the server below
					case PERMANENT_DAMAGE:
					case DISMISS:
						newStatusInPlayersMemoryOnServer = null;
						newStatusInPlayersMemoryOnClient = null;
						break;
						
					// If its our unit or hero dying from lack of production then the client still needs the unit object left around temporarily while it sorts the NTM out.
					// But anybody else's units dying from lack of production can just be removed.
				    case LACK_OF_PRODUCTION:
						newStatusInPlayersMemoryOnServer = null;
						if (trueUnit.getOwningPlayerID () == player.getPlayerDescription ().getPlayerID ())
							newStatusInPlayersMemoryOnClient = UnitStatusID.KILLED_BY_LACK_OF_PRODUCTION;
						else
							newStatusInPlayersMemoryOnClient = null;
						break;
					
					// If we're not involved in the combat, then units are remove immediately from the client.
					// If its somebody else's hero dying, then they're remove immediately from the client.
					// If its a regular unit dying in a combat we're involved in, or our own hero dying, then we might raise/animate dead it, so mark those as dead but don't remove them.
				    case HEALABLE_COMBAT_DAMAGE:
						if (trueUnit.getOwningPlayerID () == player.getPlayerDescription ().getPlayerID ())
							newStatusInPlayersMemoryOnServer = UnitStatusID.DEAD;
						else if (isHero)
							newStatusInPlayersMemoryOnServer = null;
						else if ((player.getPlayerDescription ().getPlayerID ().equals (gc.getAttackingPlayerID ())) || (player.getPlayerDescription ().getPlayerID ().equals (gc.getDefendingPlayerID ())))
							newStatusInPlayersMemoryOnServer = UnitStatusID.DEAD;
						else
							newStatusInPlayersMemoryOnServer = null;
						
						newStatusInPlayersMemoryOnClient = newStatusInPlayersMemoryOnServer;
				    	break;
				    	
				    // As above, but immediately remove regular units even if we own them
				    case HEALABLE_OVERLAND_DAMAGE:
				    	if (isHero && (trueUnit.getOwningPlayerID () == player.getPlayerDescription ().getPlayerID ()))
							newStatusInPlayersMemoryOnServer = UnitStatusID.DEAD;
				    	else
				    		newStatusInPlayersMemoryOnServer = null;

						newStatusInPlayersMemoryOnClient = newStatusInPlayersMemoryOnServer;
				    	break;
				    	
				    default:
				    	throw new MomException ("killUnitOnServerAndClients doesn't know what unit status to convert " + untransmittedAction + " into");
				}

				// If still in combat, only set to DEAD in player's memory on server, rather than removing entirely
				if (newStatusInPlayersMemoryOnServer == null)
				{
					log.debug ("Removing unit URN " + trueUnit.getUnitURN () + " from player ID " + player.getPlayerDescription ().getPlayerID () + "'s memory on server");
					getUnitUtils ().removeUnitURN (trueUnit.getUnitURN (), priv.getFogOfWarMemory ().getUnit ());
				}
				else
				{
					log.debug ("Marking unit URN " + trueUnit.getUnitURN () + " as " + newStatusInPlayersMemoryOnServer + " in player ID " + player.getPlayerDescription ().getPlayerID () + "'s memory on server");
					getUnitUtils ().findUnitURN (trueUnit.getUnitURN (), priv.getFogOfWarMemory ().getUnit (), "killUnitOnServerAndClients").setStatus (newStatusInPlayersMemoryOnServer);
				}
				
				if (player.getPlayerDescription ().isHuman ())
				{
					// New status has to be set per player depending on who can see it
					log.debug ("Telling client to mark unit URN " + trueUnit.getUnitURN () + " as " + newStatusInPlayersMemoryOnClient + " in player ID " + player.getPlayerDescription ().getPlayerID () + "'s memory");

					final KillUnitMessage msg = new KillUnitMessage ();
					msg.setUnitURN (trueUnit.getUnitURN ());
					msg.setNewStatus (newStatusInPlayersMemoryOnClient);
					
					player.getConnection ().sendMessageToClient (msg);
				}
			}
		}

		// Update the true copy of the unit as appropriate
		getUnitUtils ().beforeKillingUnit (trueMap, trueUnit.getUnitURN ());	// Removes spells cast on unit
		
		switch (untransmittedAction)
		{
			// Complete remove unit
			case PERMANENT_DAMAGE:
				log.debug ("Permanently removing unit URN " + trueUnit.getUnitURN () + " from server's true memory");
				getUnitUtils ().removeUnitURN (trueUnit.getUnitURN (), trueMap.getUnit ());
				break;

			// Dismissed heroes go back to Generated
			// Heroes dismissed by lack of production go back to Generated
			case DISMISS:
			case LACK_OF_PRODUCTION:
				if (isHero)
				{
					log.debug ("Setting hero with unit URN " + trueUnit.getUnitURN () + " back to generated in server's true memory (dismissed or lack of production)");
					trueUnit.setStatus (UnitStatusID.GENERATED);
				}
				else
				{
					log.debug ("Permanently removing unit URN " + trueUnit.getUnitURN () + " from server's true memory (dismissed or lack of production)");
					getUnitUtils ().removeUnitURN (trueUnit.getUnitURN (), trueMap.getUnit ());
				}
				break;
				
			// Killed by taking damage in combat.
			// All units killed by combat damage are kept around for the moment, since one of the players in the combat may Raise Dead them.
			// Heroes are kept at DEAD even after the combat ends, in case the player resurrects them.
			case HEALABLE_COMBAT_DAMAGE:
				log.debug ("Marking unit with unit URN " + trueUnit.getUnitURN () + " as dead in server's true memory (combat damage)");
				trueUnit.setStatus (UnitStatusID.DEAD);
				break;

			// Killed by taking damage overland.
			// As above except we only need to mark heroes as DEAD, since there's no way to resurrect regular units on the overland map.
			case HEALABLE_OVERLAND_DAMAGE:
				if (isHero)
				{
					log.debug ("Marking hero with unit URN " + trueUnit.getUnitURN () + " as dead in server's true memory (overland damage)");
					trueUnit.setStatus (UnitStatusID.DEAD);
				}
				else
				{
					log.debug ("Permanently removing unit URN " + trueUnit.getUnitURN () + " from server's true memory (overland damage)");
					getUnitUtils ().removeUnitURN (trueUnit.getUnitURN (), trueMap.getUnit ());
				}
				break;
				
			default:
				throw new MomException ("killUnitOnServerAndClients doesn't know what to do with true units when action = " + untransmittedAction);
		}
	}

	/**
	 * Sends transient spell casts to human players who are in range to see it.  This is purely for purposes of them displaying the animation,
	 * the spell is then discarded and no actual updates take place on the server or client as a result of this, other than that the client stops asking the caster to target it.
	 * 
	 * @param trueTerrain True terrain map
	 * @param trueUnits True list of units
	 * @param transientSpell The spell being cast
	 * @param players List of players in the session
	 * @param db Lookup lists built over the XML database
	 * @param fogOfWarSettings Fog of war settings from session description
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void sendTransientSpellToClients (final MapVolumeOfMemoryGridCells trueTerrain, final List<MemoryUnit> trueUnits,
		final MemoryMaintainedSpell transientSpell, final List<PlayerServerDetails> players,
		final CommonDatabase db, final FogOfWarSetting fogOfWarSettings)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		// Build the message ready to send it to whoever can see the spell
		final AddOrUpdateMaintainedSpellMessage msg = new AddOrUpdateMaintainedSpellMessage ();
		msg.setMaintainedSpell (transientSpell);
		msg.setNewlyCast (true);
		msg.setSpellTransient (true);

		// Check which players can see the spell; force the caster to be able to see it for casting Earth Lore in black areas
		for (final PlayerServerDetails player : players)
			if ((player.getPlayerDescription ().isHuman ()) && ((transientSpell.getCastingPlayerID () == player.getPlayerDescription ().getPlayerID ()) ||
				(getFogOfWarMidTurnVisibility ().canSeeSpellMidTurn (transientSpell, trueTerrain, trueUnits, player, db, fogOfWarSettings))))
				
				player.getConnection ().sendMessageToClient (msg);
	}
	
	/**
	 * Checks who can see a maintained spell that already exists on the server, adding it into the memory
	 * of anyone who can see it and also sending a message to update the client
	 *
	 * The reason we need this separate from addMaintainedSpellOnServerAndClients is for casting
	 * overland city/unit spells for which we have to pick a target before the casting of the spell is "complete"
	 *
	 * Spells in this "cast-but-not-targetted" state exist on the server but not in player's memory or on clients, so when their target has been set, this method is then called
	 *
	 * @param gsk Server knowledge structure
	 * @param trueSpell True spell to add
	 * @param players List of players in the session
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void addExistingTrueMaintainedSpellToClients (final MomGeneralServerKnowledge gsk,
		final MemoryMaintainedSpell trueSpell, final List<PlayerServerDetails> players,
		final CommonDatabase db, final MomSessionDescription sd)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException
	{
		// Build the message ready to send it to whoever can see the spell
		final AddOrUpdateMaintainedSpellMessage msg = new AddOrUpdateMaintainedSpellMessage ();
		msg.setMaintainedSpell (trueSpell);
		msg.setNewlyCast (true);		// Spells added via this method must be new, or just being targetted, so either way from the client's point of view they must be newly cast
		msg.setSpellTransient (false);

		// Check which players can see the spell
		for (final PlayerServerDetails player : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			if (getFogOfWarMidTurnVisibility ().canSeeSpellMidTurn (trueSpell, gsk.getTrueMap ().getMap (), gsk.getTrueMap ().getUnit (), player, db, sd.getFogOfWarSetting ()))
			{
				// Update player's memory on server
				if (getFogOfWarDuplication ().copyMaintainedSpell (trueSpell, priv.getFogOfWarMemory ().getMaintainedSpell ()))

					// Update on client
					if (player.getPlayerDescription ().isHuman ())
						player.getConnection ().sendMessageToClient (msg);
			}
		}
		
		// Does the spell generate a CAE? e.g. Heavenly Light and Cloud of Shadow; if so then add it
		if (trueSpell.getCitySpellEffectID () != null)
		{
			final CitySpellEffect citySpellEffect = db.findCitySpellEffect (trueSpell.getCitySpellEffectID (), "addExistingTrueMaintainedSpellToClients");
			if (citySpellEffect.getCombatAreaEffectID () != null)
			{
				final Spell spellDef = db.findSpell (trueSpell.getSpellID (), "addExistingTrueMaintainedSpellToClients");
				
				// We can assume casting cost is overland casting cost, as CAEs cast in combat generate the CAE only without a maintained spell
				addCombatAreaEffectOnServerAndClients (gsk, citySpellEffect.getCombatAreaEffectID (), trueSpell.getSpellID (), trueSpell.getCastingPlayerID (),
					spellDef.getOverlandCastingCost (), (MapCoordinates3DEx) trueSpell.getCityLocation (), players, sd);
			}
		}

		// The new spell might be Awareness, Nature Awareness, Nature's Eye, or a curse on an enemy city, so might affect the fog of war of the player who cast it
		// While it may seem a bit odd to do this here (since the spell already existed on the server before calling this),
		// the spell would have been in an untargetted state so we wouldn't know what city to apply it to, so this is definitely the right place to do this
		final PlayerServerDetails castingPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (players, trueSpell.getCastingPlayerID (), "addExistingTrueMaintainedSpellToClients");
		getFogOfWarProcessing ().updateAndSendFogOfWar (gsk.getTrueMap (), castingPlayer, players, "addExistingTrueMaintainedSpellToClients", sd, db);
	}

	/**
	 * @param gsk Server knowledge structure to add the spell to
	 * @param castingPlayerID Player who cast the spell
	 * @param spellID Which spell it is
	 * @param unitURN Indicates which unit the spell is cast on; null for spells not cast on units
	 * @param unitSkillID If a spell cast on a unit, indicates the specific skill that this spell grants the unit
	 * @param castInCombat Whether this spell was cast in combat or not
	 * @param cityLocation Indicates which city the spell is cast on; null for spells not cast on cities
	 * @param citySpellEffectID If a spell cast on a city, indicates the specific effect that this spell grants the city
	 * @param variableDamage Chosen damage selected for the spell, for spells like fire bolt where a varying amount of mana can be channeled into the spell
	 * @param players List of players in the session, this can be passed in null for when spells that require a target are added initially only on the server
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @return Newly created spell in server's true memory
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final MemoryMaintainedSpell addMaintainedSpellOnServerAndClients (final MomGeneralServerKnowledge gsk,
		final int castingPlayerID, final String spellID, final Integer unitURN, final String unitSkillID,
		final boolean castInCombat, final MapCoordinates3DEx cityLocation, final String citySpellEffectID, final Integer variableDamage, final List<PlayerServerDetails> players,
		final CommonDatabase db, final MomSessionDescription sd)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException
	{
		// First add on server
		final MapCoordinates3DEx spellLocation;
		if (cityLocation == null)
			spellLocation = null;
		else
			spellLocation = new MapCoordinates3DEx (cityLocation);

		final MemoryMaintainedSpell trueSpell = new MemoryMaintainedSpell ();
		trueSpell.setCastingPlayerID (castingPlayerID);
		trueSpell.setSpellID (spellID);
		trueSpell.setUnitURN (unitURN);
		trueSpell.setUnitSkillID (unitSkillID);
		trueSpell.setCastInCombat (castInCombat);
		trueSpell.setCityLocation (spellLocation);
		trueSpell.setCitySpellEffectID (citySpellEffectID);
		trueSpell.setVariableDamage (variableDamage);
		trueSpell.setSpellURN (gsk.getNextFreeSpellURN ());

		gsk.setNextFreeSpellURN (gsk.getNextFreeSpellURN () + 1);
		gsk.getTrueMap ().getMaintainedSpell ().add (trueSpell);

		// Then let the other routine deal with updating player memory and the clients
		if (players != null)
			addExistingTrueMaintainedSpellToClients (gsk, trueSpell, players, db, sd);

		return trueSpell;
	}

	/**
	 * Be very careful about calling this directly as it does not do things like cleaning up attached CAEs.  Maybe need to call switchOffSpell instead.
	 * 
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param spellURN Which spell it is
	 * @param players List of players in the session
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @return Whether switching off the spell resulted in the death of the unit it was cast on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final boolean switchOffMaintainedSpellOnServerAndClients (final FogOfWarMemory trueMap, final int spellURN,
		final List<PlayerServerDetails> players, final CommonDatabase db, final MomSessionDescription sd)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException
	{
		boolean killed = false;
		
		// Get the spell details before we remove it
		final MemoryMaintainedSpell trueSpell = getMemoryMaintainedSpellUtils ().findSpellURN (spellURN, trueMap.getMaintainedSpell ());
		if (trueSpell != null)
		{
			// Switch off on server
			getMemoryMaintainedSpellUtils ().removeSpellURN (spellURN, trueMap.getMaintainedSpell ());
	
			// Build the message ready to send it to whoever could see the spell
			final SwitchOffMaintainedSpellMessage msg = new SwitchOffMaintainedSpellMessage ();
			msg.setSpellURN (spellURN);
	
			// Check which players could see the spell
			for (final PlayerServerDetails player : players)
			{
				final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
				if (getFogOfWarMidTurnVisibility ().canSeeSpellMidTurn (trueSpell, trueMap.getMap (), trueMap.getUnit (), player, db, sd.getFogOfWarSetting ()))
				{
					// Update player's memory on server
					getMemoryMaintainedSpellUtils ().removeSpellURN (spellURN, priv.getFogOfWarMemory ().getMaintainedSpell ());
	
					// Update on client
					if (player.getPlayerDescription ().isHuman ())
						player.getConnection ().sendMessageToClient (msg);
				}
			}
			
			// Does the spell generate a CAE? e.g. Heavenly Light and Cloud of Shadow; if so then remove it
			if (trueSpell.getCitySpellEffectID () != null)
			{
				final CitySpellEffect citySpellEffect = db.findCitySpellEffect (trueSpell.getCitySpellEffectID (), "switchOffMaintainedSpellOnServerAndClients");
				if (citySpellEffect.getCombatAreaEffectID () != null)
				{
					final MemoryCombatAreaEffect trueCAE = getMemoryCombatAreaEffectUtils ().findCombatAreaEffect
						(trueMap.getCombatAreaEffect (), (MapCoordinates3DEx) trueSpell.getCityLocation (), citySpellEffect.getCombatAreaEffectID (), trueSpell.getCastingPlayerID ());
					
					if (trueCAE != null)
						removeCombatAreaEffectFromServerAndClients (trueMap, trueCAE.getCombatAreaEffectURN (), players, sd);
				}
				
				// The only spells with a citySpellEffectID that can be cast in combat are Wall of Fire / Wall of Darkness.
				// If these get cancelled, we need to regnerate the combat map.
				else
				{
					final Spell spellDef = db.findSpell (trueSpell.getSpellID (), "switchOffMaintainedSpellOnServerAndClients");
					if (((spellDef.getSpellBookSectionID () == SpellBookSectionID.CITY_ENCHANTMENTS) || (spellDef.getSpellBookSectionID () == SpellBookSectionID.CITY_CURSES)) &&
						spellDef.getCombatCastingCost () != null)
					{
						final CombatPlayers combatPlayers = getCombatMapUtils ().determinePlayersInCombatFromLocation
							((MapCoordinates3DEx) trueSpell.getCityLocation (), trueMap.getUnit (), players);
						if (combatPlayers.bothFound ())
						{
							final PlayerServerDetails attackingPlayer = (PlayerServerDetails) combatPlayers.getAttackingPlayer ();
							final PlayerServerDetails defendingPlayer = (PlayerServerDetails) combatPlayers.getDefendingPlayer ();
						
							final ServerGridCellEx gc = (ServerGridCellEx) trueMap.getMap ().getPlane ().get
								(trueSpell.getCityLocation ().getZ ()).getRow ().get (trueSpell.getCityLocation ().getY ()).getCell ().get (trueSpell.getCityLocation ().getX ());
							
							getCombatMapGenerator ().regenerateCombatTileBorders (gc.getCombatMap (), db, trueMap, (MapCoordinates3DEx) trueSpell.getCityLocation ());
							
							// Send the updated map
							final UpdateCombatMapMessage combatMapMsg = new UpdateCombatMapMessage ();
							combatMapMsg.setCombatLocation (trueSpell.getCityLocation ());
							combatMapMsg.setCombatTerrain (gc.getCombatMap ());
							
							if (attackingPlayer.getPlayerDescription ().isHuman ())
								attackingPlayer.getConnection ().sendMessageToClient (combatMapMsg);
	
							if (defendingPlayer.getPlayerDescription ().isHuman ())
								defendingPlayer.getConnection ().sendMessageToClient (combatMapMsg);
						}
					}
				}
			}
			
			// If spell was cast on a unit, then see if removing the spell killed it
			// e.g. Unit has 5 HP, cast Lionheart on it in combat gives +3 so now has 8 HP.  Unit takes 6 HP damage, then wins the combat.
			// Lionheart gets cancelled so now unit has -1 HP.
			if (trueSpell.getUnitURN () != null)
			{
				final MemoryUnit mu = getUnitUtils ().findUnitURN (trueSpell.getUnitURN (), trueMap.getUnit (), "switchOffMaintainedSpellOnServerAndClients");
				final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (mu, null, null, null, players, trueMap, db);
				if (xu.calculateAliveFigureCount () <= 0)
				{
					killed = true;
	
					// Work out if this is happening in combat or not
					final KillUnitActionID action = (mu.getCombatLocation () == null) ? KillUnitActionID.HEALABLE_OVERLAND_DAMAGE : KillUnitActionID.HEALABLE_COMBAT_DAMAGE;
					
					killUnitOnServerAndClients (mu, action, trueMap, players, sd.getFogOfWarSetting (), db);
				}
			}
	
			// The removed spell might be Awareness, Nature Awareness, Nature's Eye, or a curse on an enemy city, so might affect the fog of war of the player who cast it
			final PlayerServerDetails castingPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (players, trueSpell.getCastingPlayerID (), "switchOffMaintainedSpellOnServerAndClients");
			getFogOfWarProcessing ().updateAndSendFogOfWar (trueMap, castingPlayer, players, "switchOffMaintainedSpellOnServerAndClients", sd, db);
		}
		
		return killed;
	}

	/**
	 * @param gsk Server knowledge structure to add the CAE to
	 * @param combatAreaEffectID Which CAE is it
	 * @param spellID Which spell was cast to produce this CAE; null for CAEs that aren't from spells, like node auras
	 * @param castingPlayerID Player who cast the CAE if it was created via a spell; null for natural CAEs (like node auras)
	 * @param castingCost Amount of MP put into the spell, prior to any reductions the caster got; null for natural CAEs (like node auras)
	 * @param mapLocation Indicates which city the CAE is cast on; null for CAEs not cast on cities
	 * @param players List of players in the session, this can be passed in null for when CAEs are being added to the map pre-game
	 * @param sd Session description
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void addCombatAreaEffectOnServerAndClients (final MomGeneralServerKnowledge gsk,
		final String combatAreaEffectID, final String spellID, final Integer castingPlayerID, final Integer castingCost, final MapCoordinates3DEx mapLocation,
		final List<PlayerServerDetails> players, final MomSessionDescription sd)
		throws JAXBException, XMLStreamException
	{
		// First add on server
		final MapCoordinates3DEx caeLocation;
		if (mapLocation == null)
			caeLocation = null;
		else
			caeLocation = new MapCoordinates3DEx (mapLocation);

		final MemoryCombatAreaEffect trueCAE = new MemoryCombatAreaEffect ();
		trueCAE.setCombatAreaEffectID (combatAreaEffectID);
		trueCAE.setCastingPlayerID (castingPlayerID);
		trueCAE.setCastingCost (castingCost);
		trueCAE.setMapLocation (caeLocation);
		trueCAE.setCombatAreaEffectURN (gsk.getNextFreeCombatAreaEffectURN ());

		gsk.setNextFreeCombatAreaEffectURN (gsk.getNextFreeCombatAreaEffectURN () + 1);
		gsk.getTrueMap ().getCombatAreaEffect ().add (trueCAE);

		// Build the message ready to send it to whoever can see the CAE
		final AddOrUpdateCombatAreaEffectMessage msg = new AddOrUpdateCombatAreaEffectMessage ();
		msg.setMemoryCombatAreaEffect (trueCAE);
		msg.setSpellID (spellID);

		// Check which players can see the CAE
		if (players != null)
			for (final PlayerServerDetails player : players)
			{
				final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
				if (getFogOfWarMidTurnVisibility ().canSeeCombatAreaEffectMidTurn (trueCAE, priv.getFogOfWar (), sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ()))
				{
					// Update player's memory on server
					if (getFogOfWarDuplication ().copyCombatAreaEffect (trueCAE, priv.getFogOfWarMemory ().getCombatAreaEffect ()))
	
						// Update on client
						if (player.getPlayerDescription ().isHuman ())
							player.getConnection ().sendMessageToClient (msg);
				}
			}
	}

	/**
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param combatAreaEffectURN Which CAE is it
	 * @param players List of players in the session
	 * @param sd Session description
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void removeCombatAreaEffectFromServerAndClients (final FogOfWarMemory trueMap,
		final int combatAreaEffectURN, final List<PlayerServerDetails> players, final MomSessionDescription sd)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException
	{
		// Get the CAE's details before we remove it
		final MemoryCombatAreaEffect trueCAE = getMemoryCombatAreaEffectUtils ().findCombatAreaEffectURN (combatAreaEffectURN, trueMap.getCombatAreaEffect (), "removeCombatAreaEffectFromServerAndClients");
		
		// Remove on server
		getMemoryCombatAreaEffectUtils ().removeCombatAreaEffectURN (combatAreaEffectURN, trueMap.getCombatAreaEffect ());

		// Build the message ready to send it to whoever can see the CAE
		final CancelCombatAreaEffectMessage msg = new CancelCombatAreaEffectMessage ();
		msg.setCombatAreaEffectURN (combatAreaEffectURN);

		// Check which players can see the CAE
		for (final PlayerServerDetails player : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			if (getFogOfWarMidTurnVisibility ().canSeeCombatAreaEffectMidTurn (trueCAE, priv.getFogOfWar (), sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ()))		// Cheating a little passing msgData as the CAE details, but we know they're correct
			{
				// Update player's memory on server
				getMemoryCombatAreaEffectUtils ().removeCombatAreaEffectURN (combatAreaEffectURN, priv.getFogOfWarMemory ().getCombatAreaEffect ());

				// Update on client
				if (player.getPlayerDescription ().isHuman ())
					player.getConnection ().sendMessageToClient (msg);
			}
		}
	}
	
	/**
	 * @param gsk Server knowledge structure to add the building(s) to
	 * @param players List of players in the session, this can be passed in null for when buildings are being added to the map pre-game
	 * @param cityLocation Location of the city to add the building(s) to
	 * @param buildingIDs List of building IDs to create, mandatory
	 * @param buildingCreatedFromSpellID The spell that resulted in the creation of this building (e.g. casting Wall of Stone creates City Walls); null if building was constructed in the normal way
	 * @param buildingCreationSpellCastByPlayerID The player who cast the spell that resulted in the creation of this building; null if building was constructed in the normal way
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void addBuildingOnServerAndClients (final MomGeneralServerKnowledge gsk, final List<PlayerServerDetails> players,
		final MapCoordinates3DEx cityLocation, final List<String> buildingIDs,
		final String buildingCreatedFromSpellID, final Integer buildingCreationSpellCastByPlayerID,
		final MomSessionDescription sd, final CommonDatabase db)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		// Build the message ready to send it to whoever can see the building
		// This is done here rather in a method on FogOfWarDuplication because its a bit weird where we can have two buildings but both are optional
		final AddBuildingMessage msg = new AddBuildingMessage ();
		
		// First add on server
		for (final String buildingID : buildingIDs)
		{
			final MemoryBuilding trueBuilding = new MemoryBuilding ();
			trueBuilding.setCityLocation (new MapCoordinates3DEx (cityLocation));
			trueBuilding.setBuildingID (buildingID);
			trueBuilding.setBuildingURN (gsk.getNextFreeBuildingURN ());
			
			gsk.setNextFreeBuildingURN (gsk.getNextFreeBuildingURN () + 1);
			gsk.getTrueMap ().getBuilding ().add (trueBuilding);
			
			msg.getBuilding ().add (trueBuilding);
		}

		msg.setBuildingsCreatedFromSpellID (buildingCreatedFromSpellID);
		msg.setBuildingCreationSpellCastByPlayerID (buildingCreationSpellCastByPlayerID);

		// Check which players can see the building
		if (players != null)
		{
			for (final PlayerServerDetails player : players)
			{
				final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
				final FogOfWarStateID state = priv.getFogOfWar ().getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ());
				if (getFogOfWarCalculations ().canSeeMidTurn (state, sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ()))
				{
					// Add into player's memory on server
					for (final MemoryBuilding trueBuilding : msg.getBuilding ())
						getFogOfWarDuplication ().copyBuilding (trueBuilding, priv.getFogOfWarMemory ().getBuilding ());
	
					// Send to client
					if (player.getPlayerDescription ().isHuman ())
						player.getConnection ().sendMessageToClient (msg);
				}
			}

			// The new building might be an Oracle, so recalculate fog of war
			// Buildings added at the start of the game are added straight to the TrueMap without using this
			// routine, so this doesn't cause a bunch of FOW recalculations before the game starts
			final OverlandMapCityData cityData = gsk.getTrueMap ().getMap ().getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();
			final PlayerServerDetails cityOwner = getMultiplayerSessionServerUtils ().findPlayerWithID (players, cityData.getCityOwnerID (), "addBuildingOnServerAndClients");
			getFogOfWarProcessing ().updateAndSendFogOfWar (gsk.getTrueMap (), cityOwner, players, "addBuildingOnServerAndClients", sd, db);
		}
	}

	/**
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param players List of players in the session
	 * @param buildingURNs Which buildings to remove
	 * @param updateBuildingSoldThisTurn If true, tells client to update the buildingSoldThisTurn flag, which will prevents this city from selling a 2nd building this turn
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void destroyBuildingOnServerAndClients (final FogOfWarMemory trueMap,
		final List<PlayerServerDetails> players, final List<Integer> buildingURNs, final boolean updateBuildingSoldThisTurn,
		final MomSessionDescription sd, final CommonDatabase db)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		// Grab the details of all the buldings before we destroy them on the server
		final List<MemoryBuilding> trueBuildings = new ArrayList<MemoryBuilding> ();
		final List<MapCoordinates3DEx> cityLocations = new ArrayList<MapCoordinates3DEx> ();
		for (final Integer buildingURN : buildingURNs)
		{
			final MemoryBuilding trueBuilding = getMemoryBuildingUtils ().findBuildingURN (buildingURNs.get (0), trueMap.getBuilding (), "destroyBuildingOnServerAndClients");
			trueBuildings.add (trueBuilding);
			
			final MapCoordinates3DEx cityLocation = (MapCoordinates3DEx) trueBuilding.getCityLocation ();
			if (!cityLocations.contains (cityLocation))
				cityLocations.add (cityLocation);
			
			getMemoryBuildingUtils ().removeBuildingURN (buildingURN, trueMap.getBuilding ());
		}		
		
		// Deal with each player individually - as the buildings may be in different cities, the lists we send to each player might be different
		for (final PlayerServerDetails player : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

			final DestroyBuildingMessage msg = player.getPlayerDescription ().isHuman () ? new DestroyBuildingMessage () : null;

			for (final MemoryBuilding trueBuilding : trueBuildings)
			{
				final MapCoordinates3DEx cityLocation = (MapCoordinates3DEx) trueBuilding.getCityLocation ();
				final FogOfWarStateID state = priv.getFogOfWar ().getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ());
				if (getFogOfWarCalculations ().canSeeMidTurn (state, sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ()))
				{
					// Remove from player's memory on server
					for (final Integer buildingURN : buildingURNs)
						getMemoryBuildingUtils ().removeBuildingURN (buildingURN, priv.getFogOfWarMemory ().getBuilding ());
	
					// Send to client
					if (msg != null)
						msg.getBuildingURN ().add (trueBuilding.getBuildingURN ());
				}
			}

			// Send completed message
			if ((msg != null) && (msg.getBuildingURN ().size () > 0))
			{
				msg.setUpdateBuildingSoldThisTurn (updateBuildingSoldThisTurn);
				player.getConnection ().sendMessageToClient (msg);
			}
		}

		// The destroyed building might be an Oracle, so recalculate fog of war - first figure out who's cities were affected
		final List<Integer> cityOwners = new ArrayList<Integer> ();
		for (final MapCoordinates3DEx cityLocation : cityLocations)
		{
			final OverlandMapCityData cityData = trueMap.getMap ().getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();
			if (!cityOwners.contains (cityData.getCityOwnerID ()))
				cityOwners.add (cityData.getCityOwnerID ());
		}
		
		for (final Integer cityOwnerID : cityOwners)
		{
			final PlayerServerDetails cityOwner = getMultiplayerSessionServerUtils ().findPlayerWithID (players, cityOwnerID, "destroyBuildingOnServerAndClients");
			getFogOfWarProcessing ().updateAndSendFogOfWar (trueMap, cityOwner, players, "destroyBuildingOnServerAndClients", sd, db);
		}
	}

	/**
	 * Informs clients who can see this unit of any changes
	 *
	 * @param tu True unit details
	 * @param trueTerrain True terrain map
	 * @param players List of players in the session
	 * @param db Lookup lists built over the XML database
	 * @param fogOfWarSettings Fog of war settings from session description
	 * @param fowMessages If null then any necessary client messgaes will be sent individually; if map is passed in then any necessary client messages are collated here ready to be sent in bulk
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found, or the player should be able to see the unit but it isn't in their list
	 * @throws PlayerNotFoundException If the player who owns the unit cannot be found
	 * @throws MomException If the player's unit doesn't have the experience skill
	 */
	@Override
	public final void updatePlayerMemoryOfUnit (final MemoryUnit tu, final MapVolumeOfMemoryGridCells trueTerrain,
		final List<PlayerServerDetails> players, final CommonDatabase db, final FogOfWarSetting fogOfWarSettings,
		final Map<Integer, FogOfWarVisibleAreaChangedMessage> fowMessages)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		// First build the message
		final AddOrUpdateUnitMessage msg;
		if (fowMessages == null)
		{
			msg = new AddOrUpdateUnitMessage ();
			msg.setMemoryUnit (tu);
		}
		else
			msg = null;

		// Check which players can see the unit
		// Note it isn't enough to say "Is the Unit URN in the player's memory" - maybe they've seen the unit before and are remembering
		// what they saw, but cannot see it now - in that case they shouldn't receive any updates about the unit
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			if (getFogOfWarMidTurnVisibility ().canSeeUnitMidTurn (tu, trueTerrain, thisPlayer, db, fogOfWarSettings))
				
				// Update player's memory on server
				if (getFogOfWarDuplication ().copyUnit (tu, priv.getFogOfWarMemory ().getUnit (), tu.getOwningPlayerID () == thisPlayer.getPlayerDescription ().getPlayerID ()))
					
					// Update player's memory on client
					if (thisPlayer.getPlayerDescription ().isHuman ())
					{
						if (msg != null)
							thisPlayer.getConnection ().sendMessageToClient (msg);
						else
						{
							// Does a FOW message already exist for this player?
							FogOfWarVisibleAreaChangedMessage fowMessage = fowMessages.get (thisPlayer.getPlayerDescription ().getPlayerID ());
							if (fowMessage == null)
							{
								fowMessage = new FogOfWarVisibleAreaChangedMessage ();
								fowMessages.put (thisPlayer.getPlayerDescription ().getPlayerID (), fowMessage);
								fowMessage.setTriggeredFrom ("Bulk unit update");
							}
							fowMessage.getAddOrUpdateUnit ().add (tu);
						}
					}
		}
	}

	/**
	 * Informs clients who can see any of the units involved about how much combat damage an attacker and/or defender(s) have taken.
	 * The two players in combat use this to show the animation of the attack, be it a melee, ranged or spell attack.
	 * If the damage is enough to kill off the unit, the client will take care of this - we don't need to send a separate KillUnitMessage.
	 * 
	 * @param tuAttacker Server's true memory of unit that made the attack; or null if the attack isn't coming from a unit
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit; can be null if won't be animated
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit; can be null if won't be animated
	 * @param tuDefenders Server's true memory of unit(s) that got hit
	 * @param attackSkillID Skill used to make the attack, e.g. for gaze or breath attacks
	 * @param attackSpellID Spell used to make the attack
	 * @param specialDamageResolutionsApplied List of special damage resolutions done to the defender (used for warp wood); limitation that client assumes this damage type is applied to ALL defenders
	 * @param wreckTilePosition If the tile was attacked directly with Wall Crusher skill, the location of the tile that was attacked
	 * @param wrecked If the tile was attacked directly with Wall Crusher skill, whether the attempt was successful or not
	 * @param players List of players in the session
	 * @param trueTerrain True terrain map
	 * @param db Lookup lists built over the XML database
	 * @param fogOfWarSettings Fog of War settings from session description
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found, or a player should know about one of the units but we can't find it in their memory
	 * @throws PlayerNotFoundException If the player who owns the unit cannot be found
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final void sendDamageToClients (final MemoryUnit tuAttacker, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final List<MemoryUnit> tuDefenders, final String attackSkillID, final String attackSpellID, final List<DamageResolutionTypeID> specialDamageResolutionsApplied,
		final MapCoordinates2DEx wreckTilePosition, final Boolean wrecked, final List<PlayerServerDetails> players, final MapVolumeOfMemoryGridCells trueTerrain,
		final CommonDatabase db, final FogOfWarSetting fogOfWarSettings)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			
			// We might get the interesting situation where an outside observer not involved in the combat can see one
			// unit stack but not the other - and so know about one unit but not the other.
			// We handle this by leaving one of the UnitURNs as null, but this means we have to build the message separately for each client.
			final ApplyDamageMessage msg;
			if (thisPlayer.getPlayerDescription ().isHuman ())
			{
				msg = new ApplyDamageMessage ();
				msg.setYourCombat ((thisPlayer == attackingPlayer) || (thisPlayer == defendingPlayer));
				msg.setWreckTilePosition (wreckTilePosition);
				msg.setWrecked (wrecked);
				if (specialDamageResolutionsApplied != null)
					msg.getSpecialDamageResolutionTypeID ().addAll (specialDamageResolutionsApplied);
			}
			else
				msg = null;
			
			// Attacking unit
			if ((tuAttacker != null) && (getFogOfWarMidTurnVisibility ().canSeeUnitMidTurn (tuAttacker, trueTerrain, thisPlayer, db, fogOfWarSettings)))
			{
				// Update player's memory of attacker on server
				final MemoryUnit muAttacker = getUnitUtils ().findUnitURN (tuAttacker.getUnitURN (), priv.getFogOfWarMemory ().getUnit (), "sendDamageToClients-a");
				muAttacker.setCombatHeading (tuAttacker.getCombatHeading ());
				muAttacker.setDoubleCombatMovesLeft (tuAttacker.getDoubleCombatMovesLeft ());

				muAttacker.getUnitDamage ().clear ();
				tuAttacker.getUnitDamage ().forEach (tuDamage ->
				{
					final UnitDamage muDamage = new UnitDamage ();
					muDamage.setDamageType (tuDamage.getDamageType ());
					muDamage.setDamageTaken (tuDamage.getDamageTaken ());
					muAttacker.getUnitDamage ().add (muDamage);
				});
				
				// Update player's memory of attacker on client
				if (msg != null)
				{
					msg.setAttackerUnitURN (tuAttacker.getUnitURN ());
					msg.setAttackerDirection (tuAttacker.getCombatHeading ());
					msg.setAttackerDoubleCombatMovesLeft (tuAttacker.getDoubleCombatMovesLeft ());
					msg.getAttackerUnitDamage ().addAll (tuAttacker.getUnitDamage ());
				}
			}
			
			// Defending unit(s)
			for (final MemoryUnit tuDefender : tuDefenders)
				if (getFogOfWarMidTurnVisibility ().canSeeUnitMidTurn (tuDefender, trueTerrain, thisPlayer, db, fogOfWarSettings))
				{
					// Update player's memory of defender on server
					final MemoryUnit muDefender = getUnitUtils ().findUnitURN (tuDefender.getUnitURN (), priv.getFogOfWarMemory ().getUnit (), "sendDamageToClients-d");
					muDefender.setCombatHeading (tuDefender.getCombatHeading ());

					muDefender.getUnitDamage ().clear ();
					tuDefender.getUnitDamage ().forEach (tuDamage ->
					{
						final UnitDamage muDamage = new UnitDamage ();
						muDamage.setDamageType (tuDamage.getDamageType ());
						muDamage.setDamageTaken (tuDamage.getDamageTaken ());
						muDefender.getUnitDamage ().add (muDamage);
					});
					
					// Update player's memory of defender on client
					if (msg != null)
					{
						final ApplyDamageMessageUnit msgUnit = new ApplyDamageMessageUnit ();
						msgUnit.setDefenderUnitURN (tuDefender.getUnitURN ());
						msgUnit.setDefenderDirection (tuDefender.getCombatHeading ());
						msgUnit.getDefenderUnitDamage ().addAll (tuDefender.getUnitDamage ());
						msg.getDefenderUnit ().add (msgUnit);
					}
				}
			
			// Do we have a message to send?
			if (msg != null)
				if ((msg.getAttackerUnitURN () != null) || (msg.getDefenderUnit ().size () > 0))
				{
					msg.setAttackSkillID (attackSkillID);
					msg.setAttackSpellID (attackSpellID);
					thisPlayer.getConnection ().sendMessageToClient (msg);
				}
		}		
	}

	/**
	 * Copies all of the listed units from source to destination, along with any unit spells (enchantments like Flame Blade or curses like Weakness)
	 * and then sends them to the client; this is used when a unit stack comes into view when it is moving

	 * @param unitStack List of units to copy
	 * @param trueSpells True spell details held on server
	 * @param player Player to copy details into
	 * @throws RecordNotFoundException If unit with requested URN is not found
	 * @throws JAXBException If there is a problem sending a message to the client
	 * @throws XMLStreamException If there is a problem sending a message to the client
	 */
	@Override
	public final void addUnitStackIncludingSpellsToServerPlayerMemoryAndSendToClient (final List<MemoryUnit> unitStack,
		final List<MemoryMaintainedSpell> trueSpells, final PlayerServerDetails player)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		// Units
		final List<Integer> unitURNs = new ArrayList<Integer> ();
		for (final MemoryUnit tu : unitStack)
		{
			unitURNs.add (tu.getUnitURN ());

			if (getFogOfWarDuplication ().copyUnit (tu, priv.getFogOfWarMemory ().getUnit (), tu.getOwningPlayerID () == player.getPlayerDescription ().getPlayerID ()))
				if (player.getPlayerDescription ().isHuman ())
				{
					final AddOrUpdateUnitMessage msg = new AddOrUpdateUnitMessage ();
					msg.setMemoryUnit (tu);
					player.getConnection ().sendMessageToClient (msg);
				}
		}

		// Spells cast on those units
		for (final MemoryMaintainedSpell trueSpell : trueSpells)
			if (trueSpell.getUnitURN () != null)
				if (unitURNs.contains (trueSpell.getUnitURN ()))
					if (getFogOfWarDuplication ().copyMaintainedSpell (trueSpell, priv.getFogOfWarMemory ().getMaintainedSpell ()))
						if (player.getPlayerDescription ().isHuman ())
						{
							final AddOrUpdateMaintainedSpellMessage msg = new AddOrUpdateMaintainedSpellMessage ();
							msg.setMaintainedSpell (trueSpell);
							msg.setNewlyCast (false);
							msg.setSpellTransient (false);
							player.getConnection ().sendMessageToClient (msg);
						}
	}

	/**
	 * Similar to the above routine, this removed all the listed units from the player's memory on the server, along with any unit spells
	 * Unlike the above routine, it sends no messages to inform the client
	 *
	 * @param unitStack List of unit URNs to remove
	 * @param player Player to remove them from
	 */
	@Override
	public final void freeUnitStackIncludingSpellsFromServerPlayerMemoryOnly (final List<Integer> unitStack, final PlayerServerDetails player)
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		// Units
		final Iterator<MemoryUnit> units = priv.getFogOfWarMemory ().getUnit ().iterator ();
		while (units.hasNext ())
		{
			final MemoryUnit thisUnit = units.next ();
			if (unitStack.contains (thisUnit.getUnitURN ()))
				units.remove ();
		}

		// Spells
		final Iterator<MemoryMaintainedSpell> spells = priv.getFogOfWarMemory ().getMaintainedSpell ().iterator ();
		while (spells.hasNext ())
		{
			final MemoryMaintainedSpell thisSpell = spells.next ();
			if (thisSpell.getUnitURN () != null)
				if (unitStack.contains (thisSpell.getUnitURN ()))
					spells.remove ();
		}
	}

	/**
	 * Finds the direction to make a one cell move in, when trying to get from moveFrom to moveTo
	 * This is based on the directions calculated by the movement routine so will take into account everything known about the terrain, so the direction
	 * chosen will try to make use of roads etc. and take into account the types of units moving rather than just being in a straight line
	 *
	 * @param moveFrom Location to move from
	 * @param moveTo Location to determine direction to
	 * @param movementDirections Movement directions from moveFrom to every location on the map
	 * @param sys Overland map coordinate system
	 * @return Direction to make one cell move in
	 * @throws MomException If we can't find a route from moveFrom to moveTo
	 */
	@Override
	public final int determineMovementDirection (final MapCoordinates3DEx moveFrom, final MapCoordinates3DEx moveTo,
		final int [] [] [] movementDirections, final CoordinateSystem sys) throws MomException
	{
		// The value at each cell of the directions grid is the direction we need to have come FROM to get there
		// So we need to start at the destinationand follow backwards down the movement path until we
		// get back to the From location, and the direction we want is the one that led us to the From location
		final MapCoordinates3DEx coords = new MapCoordinates3DEx (moveTo);

		int direction = -1;
		while ((coords.getX () != moveFrom.getX () || (coords.getY () != moveFrom.getY ())))
		{
			direction = movementDirections [coords.getZ ()] [coords.getY ()] [coords.getX ()];
			if (!getCoordinateSystemUtils ().move3DCoordinates (sys, coords, getCoordinateSystemUtils ().normalizeDirection (sys.getCoordinateSystemType (), direction + 4)))
				throw new MomException ("determineMovementDirection: Server map tracing moved to a cell off the map");
		}

		if (direction < 0)
			throw new MomException ("determineMovementDirection: Failed to trace a route from the movement target " + moveTo + " back to the movement origin " + moveFrom);

		return direction;
	}

	/**
	 * Reduces the amount of remaining movement that all units in this stack have left by the amount that it costs them to enter a grid cell of the specified type
	 *
	 * @param unitStack The unit stack that is moving
	 * @param unitStackSkills All the skills that any units in the stack have
	 * @param tileTypeID Tile type being moved onto
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If the definition of a spell that is cast on the unit cannot be found in the db
	 * @throws MomException If the one of the units in the list is not a MemoryUnit 
	 */
	@Override
	public final void reduceMovementRemaining (final List<ExpandedUnitDetails> unitStack, final Set<String> unitStackSkills, final String tileTypeID,
		final CommonDatabase db) throws RecordNotFoundException, MomException
	{
		for (final ExpandedUnitDetails thisUnit : unitStack)
		{
			// Don't just work out the distance it took for the whole stack to get here - if e.g. one unit can fly it might be able to
			// move into mountains taking only 1 MP whereas another unit in the same stack might take 3 MP
			Integer doubleMovementCost = getUnitCalculations ().calculateDoubleMovementToEnterTileType (thisUnit, unitStackSkills, tileTypeID, db);
			
			// The only way we can get impassable here as a valid move is if we're loading onto a transport, in which case force movement spent to 2
			if (doubleMovementCost == null)
				doubleMovementCost = 2;

			// Entirely valid for doubleMovementCost to be > doubleOverlandMovesLeft, if e.g. a spearmen with 1 MP is moving onto mountains which cost 3 MP
			if (doubleMovementCost > thisUnit.getDoubleOverlandMovesLeft ())
				thisUnit.setDoubleOverlandMovesLeft (0);
			else
				thisUnit.setDoubleOverlandMovesLeft (thisUnit.getDoubleOverlandMovesLeft () - doubleMovementCost);
		}
	}
	
	/**
	 * @return Single cell FOW calculations
	 */
	public final FogOfWarCalculations getFogOfWarCalculations ()
	{
		return fogOfWarCalculations;
	}

	/**
	 * @param calc Single cell FOW calculations
	 */
	public final void setFogOfWarCalculations (final FogOfWarCalculations calc)
	{
		fogOfWarCalculations = calc;
	}

	/**
	 * @return FOW visibility checks
	 */
	public final FogOfWarMidTurnVisibility getFogOfWarMidTurnVisibility ()
	{
		return fogOfWarMidTurnVisibility;
	}

	/**
	 * @param vis FOW visibility checks
	 */
	public final void setFogOfWarMidTurnVisibility (final FogOfWarMidTurnVisibility vis)
	{
		fogOfWarMidTurnVisibility = vis;
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
	 * @return Memory CAE utils
	 */
	public final MemoryCombatAreaEffectUtils getMemoryCombatAreaEffectUtils ()
	{
		return memoryCombatAreaEffectUtils;
	}

	/**
	 * @param utils Memory CAE utils
	 */
	public final void setMemoryCombatAreaEffectUtils (final MemoryCombatAreaEffectUtils utils)
	{
		memoryCombatAreaEffectUtils = utils;
	}

	/**
	 * @return MemoryMaintainedSpell utils
	 */
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param utils MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils utils)
	{
		memoryMaintainedSpellUtils = utils;
	}

	/**
	 * @return Unit calculations
	 */
	public final UnitCalculations getUnitCalculations ()
	{
		return unitCalculations;
	}

	/**
	 * @param calc Unit calculations
	 */
	public final void setUnitCalculations (final UnitCalculations calc)
	{
		unitCalculations = calc;
	}

	/**
	 * @return Pending movement utils
	 */
	public final PendingMovementUtils getPendingMovementUtils ()
	{
		return pendingMovementUtils;
	}

	/**
	 * @param utils Pending movement utils
	 */
	public final void setPendingMovementUtils (final PendingMovementUtils utils)
	{
		pendingMovementUtils = utils;
	}

	/**
	 * @return Coordinate system utils
	 */
	public final CoordinateSystemUtils getCoordinateSystemUtils ()
	{
		return coordinateSystemUtils;
	}

	/**
	 * @param utils Coordinate system utils
	 */
	public final void setCoordinateSystemUtils (final CoordinateSystemUtils utils)
	{
		coordinateSystemUtils = utils;
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