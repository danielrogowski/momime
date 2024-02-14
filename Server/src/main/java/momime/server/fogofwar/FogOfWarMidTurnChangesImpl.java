package momime.server.fogofwar;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.sessionbase.PlayerType;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CitySpellEffect;
import momime.common.database.CommonDatabase;
import momime.common.database.FogOfWarSetting;
import momime.common.database.FogOfWarValue;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.messages.FogOfWarStateID;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
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
import momime.common.messages.servertoclient.DestroyBuildingMessage;
import momime.common.messages.servertoclient.FogOfWarVisibleAreaChangedMessage;
import momime.common.messages.servertoclient.UpdateCityMessage;
import momime.common.messages.servertoclient.UpdateCityMessageData;
import momime.common.messages.servertoclient.UpdateTerrainMessage;
import momime.common.messages.servertoclient.UpdateTerrainMessageData;
import momime.common.movement.OverlandMovementCell;
import momime.common.utils.CombatMapUtils;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.PendingMovementUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.FogOfWarCalculations;
import momime.server.mapgenerator.CombatMapGenerator;
import momime.server.messages.MomGeneralServerKnowledge;
import momime.server.process.ResolveAttackTarget;
import momime.server.utils.UnitServerUtils;

/**
 * This contains all methods that allow changes in the server's true memory to be replicated into each player's memory and send update messages to each client
 * i.e. methods for when the true values change (or are added or removed) but the visible area that each player can see does not change
 */
public final class FogOfWarMidTurnChangesImpl implements FogOfWarMidTurnChanges
{
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
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;

	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
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
					if (thisPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
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
					if (thisPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
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
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws PlayerNotFoundException If we can't find one of the players
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 */
	@Override
	public final void updatePlayerMemoryOfSpell (final MemoryMaintainedSpell trueSpell, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, PlayerNotFoundException, RecordNotFoundException, MomException
	{
		// First build the message
		final AddOrUpdateMaintainedSpellMessage spellMsg = new AddOrUpdateMaintainedSpellMessage ();
		spellMsg.setMaintainedSpell (trueSpell);

		// Check which players can see the spell
		for (final PlayerServerDetails thisPlayer : mom.getPlayers ())
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			
			if (getFogOfWarMidTurnVisibility ().canSeeSpellMidTurn (trueSpell, thisPlayer, mom))
			{
				// Update player's memory on server
				if (getFogOfWarDuplication ().copyMaintainedSpell (trueSpell, priv.getFogOfWarMemory ().getMaintainedSpell ()))

					// Update player's memory on client
					if (thisPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
						thisPlayer.getConnection ().sendMessageToClient (spellMsg);
			}
		}

		// The stolen spell might be Awareness, Nature Awareness, Nature's Eye, or a curse on an enemy city, so might affect the fog of wag of the player who cast it.
		// Technically we should also do this for the old player who lost the spell, but its fine, they will end up losing sight of it soon enough.
		final PlayerServerDetails castingPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), trueSpell.getCastingPlayerID (), "updatePlayerMemoryOfSpell");
		getFogOfWarProcessing ().updateAndSendFogOfWar (castingPlayer, "updatePlayerMemoryOfSpell", mom);
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
					if (thisPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
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
	 * @param unitID Type of unit to create
	 * @param locationToAddUnit Location to add the new unit; can be null for adding heroes that haven't been summoned yet
	 * @param buildingsLocation Location the unit was built - might be different from locationToAddUnit if the city is full and the unit got bumped to an adjacent tile; passed as null for units not built in cities such as summons
	 * @param overrideStartingExperience Set this to not calculate startingExperience from buildings at buildingLocation and instead just take this value
	 * @param combatLocation The location of the combat that this unit is being summoned into; null for anything other than combat summons
	 * @param unitOwner Player who will own the new unit
	 * @param initialStatus Initial status of the unit, typically ALIVE
	 * @param addOnClients Usually true, can set to false when monsters are initially added to the map and don't need to worry about who can see them
	 * @param giveFullOverlandMovement If true, doubleOverlandMovesLeft will be initialized to its full value
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Newly created unit
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final MemoryUnit addUnitOnServerAndClients (final String unitID, final MapCoordinates3DEx locationToAddUnit, final MapCoordinates3DEx buildingsLocation,
		final Integer overrideStartingExperience, final MapCoordinates3DEx combatLocation, final PlayerServerDetails unitOwner, final UnitStatusID initialStatus,
		final boolean addOnClients, final boolean giveFullOverlandMovement, final MomSessionVariables mom)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException
	{
		// There's a bunch of other unit statuses that don't make sense to use here - so worth checking this
		if ((initialStatus != UnitStatusID.NOT_GENERATED) && (initialStatus != UnitStatusID.ALIVE))
			throw new MomException ("addUnitOnServerAndClients: Invalid initial status of " + initialStatus);

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
			final KnownWizardDetails unitOwnerWizard = getKnownWizardUtils ().findKnownWizardDetails (mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (),
				unitOwner.getPlayerDescription ().getPlayerID (), "addUnitOnServerAndClients");
			
			startingExperience = getMemoryBuildingUtils ().experienceFromBuildings (mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (),
				mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), buildingsLocation, mom.getServerDB ());
			
			weaponGrade = getUnitCalculations ().calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort
				(mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
					buildingsLocation, unitOwnerWizard.getPick (), mom.getSessionDescription ().getOverlandMapSize (), mom.getServerDB ());
		}
		else
		{
			startingExperience = 0;
			weaponGrade = null;
		}

		// Add on server
		// Even for heroes, we load in their default skill list - this is how heroes default skills are loaded during game startup
		final MemoryUnit newUnit = getUnitServerUtils ().createMemoryUnit (unitID, mom.getGeneralServerKnowledge ().getNextFreeUnitURN (), weaponGrade, startingExperience, mom.getServerDB ());
		newUnit.setOwningPlayerID (unitOwner.getPlayerDescription ().getPlayerID ());
		newUnit.setCombatLocation (combatLocation);

		mom.getGeneralServerKnowledge ().setNextFreeUnitURN (mom.getGeneralServerKnowledge ().getNextFreeUnitURN () + 1);
		mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ().add (newUnit);

		if (initialStatus == UnitStatusID.ALIVE)
			updateUnitStatusToAliveOnServerAndClients (newUnit, locationToAddUnit, unitOwner, addOnClients, giveFullOverlandMovement, mom);
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
	 * @param addOnClients Usually true, can set to false when monsters are initially added to the map and don't need to worry about who can see them
	 * @param giveFullOverlandMovement If true, doubleOverlandMovesLeft will be initialized to its full value
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void updateUnitStatusToAliveOnServerAndClients (final MemoryUnit trueUnit, final MapCoordinates3DEx locationToAddUnit,
		final PlayerServerDetails unitOwner, final boolean addOnClients, final boolean giveFullOverlandMovement, final MomSessionVariables mom)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException
	{
		// Update on server
		final MapCoordinates3DEx unitLocation = new MapCoordinates3DEx (locationToAddUnit);

		trueUnit.setUnitLocation (unitLocation);
		trueUnit.setStatus (UnitStatusID.ALIVE);
		
		// Movement allocation?
		if (giveFullOverlandMovement)
			trueUnit.setDoubleOverlandMovesLeft (2 * getExpandUnitDetails ().expandUnitDetails (trueUnit, null, null, null,
				mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ()).getMovementSpeed ());

		// What can the new unit see? (it may expand the unit owner's vision to see things that they couldn't previously)
		if ((addOnClients) && (trueUnit.getCombatLocation () == null))
			getFogOfWarProcessing ().updateAndSendFogOfWar (unitOwner, "updateUnitStatusToAliveOnServerAndClients", mom);

		// Tell clients?
		if (addOnClients)
			updatePlayerMemoryOfUnit (trueUnit, mom, null);
	}

	/**
	 * Sends transient spell casts to human players who are in range to see it.  This is purely for purposes of them displaying the animation,
	 * the spell is then discarded and no actual updates take place on the server or client as a result of this, other than that the client stops asking the caster to target it.
	 * 
	 * @param transientSpell The spell being cast
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void sendTransientSpellToClients (final MemoryMaintainedSpell transientSpell, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		// Build the message ready to send it to whoever can see the spell
		final AddOrUpdateMaintainedSpellMessage msg = new AddOrUpdateMaintainedSpellMessage ();
		msg.setMaintainedSpell (transientSpell);
		msg.setNewlyCast (true);
		msg.setSpellTransient (true);

		// Check which players can see the spell; force the caster to be able to see it for casting Earth Lore in black areas
		for (final PlayerServerDetails player : mom.getPlayers ())
			if ((player.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN) && ((transientSpell.getCastingPlayerID () == player.getPlayerDescription ().getPlayerID ()) ||
				(getFogOfWarMidTurnVisibility ().canSeeSpellMidTurn (transientSpell, player, mom))))
				
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
	 * @param trueSpell True spell to add
	 * @param skipAnimation Tell the client to skip showing any animation and sound effect associated with this spell
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void addExistingTrueMaintainedSpellToClients (final MemoryMaintainedSpell trueSpell, final boolean skipAnimation, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException
	{
		// Build the message ready to send it to whoever can see the spell
		final AddOrUpdateMaintainedSpellMessage msg = new AddOrUpdateMaintainedSpellMessage ();
		msg.setMaintainedSpell (trueSpell);
		
		// Spells added via this method must be new, or just being targetted, so either way from the client's point of view they must be newly cast
		// so normally this is "true", but in reality what the client uses this for is to decide whether to show the animation for it
		msg.setNewlyCast (!skipAnimation);
		msg.setSpellTransient (false);

		// Check which players can see the spell
		for (final PlayerServerDetails player : mom.getPlayers ())
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			if (getFogOfWarMidTurnVisibility ().canSeeSpellMidTurn (trueSpell, player, mom))
			{
				// Update player's memory on server
				if (getFogOfWarDuplication ().copyMaintainedSpell (trueSpell, priv.getFogOfWarMemory ().getMaintainedSpell ()))

					// Update on client
					if (player.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
						player.getConnection ().sendMessageToClient (msg);
			}
		}
		
		// Does the spell generate a CAE? e.g. Heavenly Light and Cloud of Shadow; if so then add it
		if (trueSpell.getCitySpellEffectID () != null)
		{
			final CitySpellEffect citySpellEffect = mom.getServerDB ().findCitySpellEffect (trueSpell.getCitySpellEffectID (), "addExistingTrueMaintainedSpellToClients");
			if (citySpellEffect.getCombatAreaEffectID () != null)
			{
				final Spell spellDef = mom.getServerDB ().findSpell (trueSpell.getSpellID (), "addExistingTrueMaintainedSpellToClients");
				
				// We can assume casting cost is overland casting cost, as CAEs cast in combat generate the CAE only without a maintained spell
				addCombatAreaEffectOnServerAndClients (mom.getGeneralServerKnowledge (), citySpellEffect.getCombatAreaEffectID (), trueSpell.getSpellID (), trueSpell.getCastingPlayerID (),
					spellDef.getOverlandCastingCost (), (MapCoordinates3DEx) trueSpell.getCityLocation (), mom.getPlayers (), mom.getSessionDescription ());
			}
		}

		// The new spell might be Awareness, Nature Awareness, Nature's Eye, or a curse on an enemy city, so might affect the fog of war of the player who cast it
		// While it may seem a bit odd to do this here (since the spell already existed on the server before calling this),
		// the spell would have been in an untargetted state so we wouldn't know what city to apply it to, so this is definitely the right place to do this
		final PlayerServerDetails castingPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), trueSpell.getCastingPlayerID (), "addExistingTrueMaintainedSpellToClients");
		getFogOfWarProcessing ().updateAndSendFogOfWar (castingPlayer, "addExistingTrueMaintainedSpellToClients", mom);
	}

	/**
	 * @param castingPlayerID Player who cast the spell
	 * @param spellID Which spell it is
	 * @param unitURN Indicates which unit the spell is cast on; null for spells not cast on units
	 * @param unitSkillID If a spell cast on a unit, indicates the specific skill that this spell grants the unit
	 * @param castInCombat Whether this spell was cast in combat or not
	 * @param cityLocation Indicates which city the spell is cast on; null for spells not cast on cities
	 * @param citySpellEffectID If a spell cast on a city, indicates the specific effect that this spell grants the city
	 * @param variableDamage Chosen damage selected for the spell, for spells like fire bolt where a varying amount of mana can be channeled into the spell
	 * @param skipAnimation Tell the client to skip showing any animation and sound effect associated with this spell
	 * @param addOnClients Usually true, can set to false when spells that need targeting later are initially added on server only
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Newly created spell in server's true memory
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final MemoryMaintainedSpell addMaintainedSpellOnServerAndClients (final int castingPlayerID, final String spellID, final Integer unitURN, final String unitSkillID,
		final boolean castInCombat, final MapCoordinates3DEx cityLocation, final String citySpellEffectID, final Integer variableDamage, final boolean skipAnimation,
		final boolean addOnClients, final MomSessionVariables mom)
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
		trueSpell.setSpellURN (mom.getGeneralServerKnowledge ().getNextFreeSpellURN ());

		mom.getGeneralServerKnowledge ().setNextFreeSpellURN (mom.getGeneralServerKnowledge ().getNextFreeSpellURN () + 1);
		mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ().add (trueSpell);

		// Then let the other routine deal with updating player memory and the clients
		if (addOnClients)
			addExistingTrueMaintainedSpellToClients (trueSpell, skipAnimation, mom);

		return trueSpell;
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
						if (player.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
							player.getConnection ().sendMessageToClient (msg);
				}
			}
	}

	/**
	 * @param cityLocation Location of the city to add the building(s) to
	 * @param buildingIDs List of building IDs to create, mandatory
	 * @param buildingsCreatedFromSpellID The spell that resulted in the creation of this building (e.g. casting Wall of Stone creates City Walls); null if building was constructed in the normal way
	 * @param buildingCreationSpellCastByPlayerID The player who cast the spell that resulted in the creation of this building; null if building was constructed in the normal way
	 * @param addOnClients Usually true, can set to false when buildings are initially added to the map and don't need to worry about who can see them
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void addBuildingOnServerAndClients (final MapCoordinates3DEx cityLocation, final List<String> buildingIDs,
		final String buildingsCreatedFromSpellID, final Integer buildingCreationSpellCastByPlayerID, final boolean addOnClients, final MomSessionVariables mom)
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
			trueBuilding.setBuildingURN (mom.getGeneralServerKnowledge ().getNextFreeBuildingURN ());
			
			mom.getGeneralServerKnowledge ().setNextFreeBuildingURN (mom.getGeneralServerKnowledge ().getNextFreeBuildingURN () + 1);
			mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ().add (trueBuilding);
			
			msg.getBuilding ().add (trueBuilding);
		}

		msg.setBuildingsCreatedFromSpellID (buildingsCreatedFromSpellID);
		msg.setBuildingCreationSpellCastByPlayerID (buildingCreationSpellCastByPlayerID);

		// Check which players can see the building
		if (addOnClients)
		{
			for (final PlayerServerDetails player : mom.getPlayers ())
			{
				final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
				final FogOfWarStateID state = priv.getFogOfWar ().getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ());
				if (getFogOfWarCalculations ().canSeeMidTurn (state, mom.getSessionDescription ().getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ()))
				{
					// Add into player's memory on server
					for (final MemoryBuilding trueBuilding : msg.getBuilding ())
						getFogOfWarDuplication ().copyBuilding (trueBuilding, priv.getFogOfWarMemory ().getBuilding ());
	
					// Send to client
					if (player.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
						player.getConnection ().sendMessageToClient (msg);
				}
			}

			// The new building might be an Oracle, so recalculate fog of war
			// Buildings added at the start of the game are added straight to the TrueMap without using this
			// routine, so this doesn't cause a bunch of FOW recalculations before the game starts
			final OverlandMapCityData cityData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();
			final PlayerServerDetails cityOwner = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), cityData.getCityOwnerID (), "addBuildingOnServerAndClients");
			getFogOfWarProcessing ().updateAndSendFogOfWar (cityOwner, "addBuildingOnServerAndClients", mom);
		}
	}

	/**
	 * @param buildingURNs Which buildings to remove
	 * @param updateBuildingSoldThisTurn If true, tells client to update the buildingSoldThisTurn flag, which will prevents this city from selling a 2nd building this turn
	 * @param buildingsDestroyedBySpellID The spell that resulted in destroying these building(s), e.g. Earthquake; null if buildings destroyed for any other reason
	 * @param buildingDestructionSpellCastByPlayerID The player who cast the spell that resulted in the destruction of these buildings; null if not from a spell
	 * @param buildingDestructionSpellLocation The location the spell was targeted - need this because it might have destroyed 0 buildings; null if not from a spell
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void destroyBuildingOnServerAndClients (final List<Integer> buildingURNs, final boolean updateBuildingSoldThisTurn,
		final String buildingsDestroyedBySpellID, final Integer buildingDestructionSpellCastByPlayerID, final MapCoordinates3DEx buildingDestructionSpellLocation,
		final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		// Grab the details of all the buldings before we destroy them on the server
		final List<MemoryBuilding> trueBuildings = new ArrayList<MemoryBuilding> ();
		final List<MapCoordinates3DEx> cityLocations = new ArrayList<MapCoordinates3DEx> ();
		for (final Integer buildingURN : buildingURNs)
		{
			final MemoryBuilding trueBuilding = getMemoryBuildingUtils ().findBuildingURN (buildingURN, mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (), "destroyBuildingOnServerAndClients");
			trueBuildings.add (trueBuilding);
			
			final MapCoordinates3DEx cityLocation = (MapCoordinates3DEx) trueBuilding.getCityLocation ();
			if (!cityLocations.contains (cityLocation))
				cityLocations.add (cityLocation);
			
			getMemoryBuildingUtils ().removeBuildingURN (buildingURN, mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ());
		}		
		
		// Find who's city was targeted
		final Integer targetedCityOwnerID = (buildingDestructionSpellLocation == null ) ? null : 
			mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get (buildingDestructionSpellLocation.getZ ()) .getRow ().get
				(buildingDestructionSpellLocation.getY ()).getCell ().get (buildingDestructionSpellLocation.getX ()).getCityData ().getCityOwnerID ();
		
		// Deal with each player individually - as the buildings may be in different cities, the lists we send to each player might be different
		for (final PlayerServerDetails player : mom.getPlayers ())
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

			final DestroyBuildingMessage msg = player.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN ? new DestroyBuildingMessage () : null;

			for (final MemoryBuilding trueBuilding : trueBuildings)
			{
				final MapCoordinates3DEx cityLocation = (MapCoordinates3DEx) trueBuilding.getCityLocation ();
				final FogOfWarStateID state = priv.getFogOfWar ().getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ());
				if (getFogOfWarCalculations ().canSeeMidTurn (state, mom.getSessionDescription ().getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ()))
				{
					// Remove from player's memory on server
					getMemoryBuildingUtils ().removeBuildingURN (trueBuilding.getBuildingURN (), priv.getFogOfWarMemory ().getBuilding ());
	
					// Send to client
					if (msg != null)
						msg.getBuildingURN ().add (trueBuilding.getBuildingURN ());
				}
			}

			// Send completed message
			// Usually don't bother sending an empty list, but if this is a from a spell that failed to actually destroy any buildings, then still have to
			if ((msg != null) && ((msg.getBuildingURN ().size () > 0) || ((buildingDestructionSpellCastByPlayerID != null) &&
				((buildingDestructionSpellCastByPlayerID.equals (player.getPlayerDescription ().getPlayerID ()))) ||
					((targetedCityOwnerID != null) && (targetedCityOwnerID.equals (player.getPlayerDescription ().getPlayerID ()))))))
			{
				msg.setUpdateBuildingSoldThisTurn (updateBuildingSoldThisTurn);
				msg.setBuildingsDestroyedBySpellID (buildingsDestroyedBySpellID);
				msg.setBuildingDestructionSpellCastByPlayerID (buildingDestructionSpellCastByPlayerID);
				msg.setBuildingDestructionSpellLocation (buildingDestructionSpellLocation);
				
				player.getConnection ().sendMessageToClient (msg);
			}
		}

		// The destroyed building might be an Oracle, so recalculate fog of war - first figure out who's cities were affected
		final List<Integer> cityOwners = new ArrayList<Integer> ();
		for (final MapCoordinates3DEx cityLocation : cityLocations)
		{
			final OverlandMapCityData cityData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();
			if (!cityOwners.contains (cityData.getCityOwnerID ()))
				cityOwners.add (cityData.getCityOwnerID ());
		}
		
		for (final Integer cityOwnerID : cityOwners)
		{
			final PlayerServerDetails cityOwner = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), cityOwnerID, "destroyBuildingOnServerAndClients");
			getFogOfWarProcessing ().updateAndSendFogOfWar (cityOwner, "destroyBuildingOnServerAndClients", mom);
		}
	}

	/**
	 * Informs clients who can see this unit of any changes
	 *
	 * @param tu True unit details
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param fowMessages If null then any necessary client messgaes will be sent individually; if map is passed in then any necessary client messages are collated here ready to be sent in bulk
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found, or the player should be able to see the unit but it isn't in their list
	 * @throws PlayerNotFoundException If the player who owns the unit cannot be found
	 * @throws MomException If the player's unit doesn't have the experience skill
	 */
	@Override
	public final void updatePlayerMemoryOfUnit (final MemoryUnit tu, final MomSessionVariables mom, final Map<Integer, FogOfWarVisibleAreaChangedMessage> fowMessages)
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
		for (final PlayerServerDetails thisPlayer : mom.getPlayers ())
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			if (getFogOfWarMidTurnVisibility ().canSeeUnitMidTurn (tu, thisPlayer, mom))
				
				// Update player's memory on server
				if (getFogOfWarDuplication ().copyUnit (tu, priv.getFogOfWarMemory ().getUnit (), tu.getOwningPlayerID () == thisPlayer.getPlayerDescription ().getPlayerID ()))
					
					// Update player's memory on client
					if (thisPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
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
	 * @param wreckTilePosition If the tile was attacked directly with Wall Crusher skill, the location of the tile that was attacked
	 * @param wrecked If the tile was attacked directly with Wall Crusher skill, whether the attempt was successful or not
	 * @param skipAnimation Tell the client to skip showing any animation and sound effect associated with this spell
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found, or a player should know about one of the units but we can't find it in their memory
	 * @throws PlayerNotFoundException If the player who owns the unit cannot be found
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final void sendDamageToClients (final MemoryUnit tuAttacker, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final List<ResolveAttackTarget> tuDefenders, final String attackSkillID, final String attackSpellID,
		final MapCoordinates2DEx wreckTilePosition, final Boolean wrecked,
		final boolean skipAnimation, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		for (final PlayerServerDetails thisPlayer : mom.getPlayers ())
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			
			// We might get the interesting situation where an outside observer not involved in the combat can see one
			// unit stack but not the other - and so know about one unit but not the other.
			// We handle this by leaving one of the UnitURNs as null, but this means we have to build the message separately for each client.
			final ApplyDamageMessage msg;
			if (thisPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
			{
				msg = new ApplyDamageMessage ();
				msg.setYourCombat ((!skipAnimation) && ((thisPlayer == attackingPlayer) || (thisPlayer == defendingPlayer)));
				msg.setWreckTilePosition (wreckTilePosition);
				msg.setWrecked (wrecked);
			}
			else
				msg = null;
			
			// Attacking unit
			if ((tuAttacker != null) && (getFogOfWarMidTurnVisibility ().canSeeUnitMidTurn (tuAttacker, thisPlayer, mom)))
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
			for (final ResolveAttackTarget tuDefender : tuDefenders)
				if (getFogOfWarMidTurnVisibility ().canSeeUnitMidTurn (tuDefender.getDefender (), thisPlayer, mom))
				{
					// Update player's memory of defender on server
					final MemoryUnit muDefender = getUnitUtils ().findUnitURN (tuDefender.getDefender ().getUnitURN (), priv.getFogOfWarMemory ().getUnit (), "sendDamageToClients-d");
					muDefender.setCombatHeading (tuDefender.getDefender ().getCombatHeading ());

					muDefender.getUnitDamage ().clear ();
					tuDefender.getDefender ().getUnitDamage ().forEach (tuDamage ->
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
						msgUnit.setDefenderUnitURN (tuDefender.getDefender ().getUnitURN ());
						msgUnit.setDefenderDirection (tuDefender.getDefender ().getCombatHeading ());
						msgUnit.getDefenderUnitDamage ().addAll (tuDefender.getDefender ().getUnitDamage ());
						
						if (tuDefender.getSpellOverride () != null)
							msgUnit.setOverrideSpellID (tuDefender.getSpellOverride ().getSpellID ());
						
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
				if (player.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
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
						if (player.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
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
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @return First location to move to
	 */
	@Override
	public final MapCoordinates3DEx determineMovementDirection (final MapCoordinates3DEx moveFrom, final MapCoordinates3DEx moveTo,
		final OverlandMovementCell [] [] [] moves)
	{
		// The value at each cell of the directions grid is the direction we need to have come FROM to get there
		// So we need to start at the destinationand follow backwards down the movement path until we
		// get back to the From location, and the direction we want is the one that led us to the From location
		MapCoordinates3DEx coords = new MapCoordinates3DEx (moveTo);
		OverlandMovementCell cell = moves [coords.getZ ()] [coords.getY ()] [coords.getX ()]; 
		
		while (!cell.getMovedFrom ().equals (moveFrom)) 
		{
			coords = cell.getMovedFrom ();
			cell = moves [coords.getZ ()] [coords.getY ()] [coords.getX ()];
		}

		return new MapCoordinates3DEx (coords);
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

	/**
	 * @return Methods for finding KnownWizardDetails from the list
	 */
	public final KnownWizardUtils getKnownWizardUtils ()
	{
		return knownWizardUtils;
	}

	/**
	 * @param k Methods for finding KnownWizardDetails from the list
	 */
	public final void setKnownWizardUtils (final KnownWizardUtils k)
	{
		knownWizardUtils = k;
	}
}