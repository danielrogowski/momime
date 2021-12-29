package momime.server.fogofwar;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.FogOfWarSetting;
import momime.common.database.FogOfWarValue;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.FogOfWarVisibleAreaChangedMessage;
import momime.common.movement.OverlandMovementCell;
import momime.common.utils.ExpandedUnitDetails;
import momime.server.MomSessionVariables;
import momime.server.messages.MomGeneralServerKnowledge;
import momime.server.process.ResolveAttackTarget;

/**
 * This contains all methods that allow changes in the server's true memory to be replicated into each player's memory and send update messages to each client
 * i.e. methods for when the true values change (or are added or removed) but the visible area that each player can see does not change
 */
public interface FogOfWarMidTurnChanges
{
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
	public void updatePlayerMemoryOfTerrain (final MapVolumeOfMemoryGridCells trueTerrain,
		final List<PlayerServerDetails> players, final MapCoordinates3DEx coords,
		final FogOfWarValue terrainAndNodeAurasSetting)
		throws JAXBException, XMLStreamException;

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
	public void updatePlayerMemoryOfCity (final MapVolumeOfMemoryGridCells trueTerrain,
		final List<PlayerServerDetails> players, final MapCoordinates3DEx coords, final FogOfWarSetting fogOfWarSettings)
		throws JAXBException, XMLStreamException;

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
	public void updatePlayerMemoryOfSpell (final MemoryMaintainedSpell trueSpell, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, PlayerNotFoundException, RecordNotFoundException, MomException;
	
	/**
	 * After updating the true copy of a CAE, this routine copies and sends the new value to players who can see it
	 *
	 * @param trueCAE True CAE that was updated
	 * @param players List of players in the session
	 * @param sd Session description
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 */
	public void updatePlayerMemoryOfCombatAreaEffect (final MemoryCombatAreaEffect trueCAE, final List<PlayerServerDetails> players,
		final MomSessionDescription sd) throws JAXBException, XMLStreamException;
	
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
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Newly created unit
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public MemoryUnit addUnitOnServerAndClients (final String unitID, final MapCoordinates3DEx locationToAddUnit, final MapCoordinates3DEx buildingsLocation,
		final Integer overrideStartingExperience, final MapCoordinates3DEx combatLocation, final PlayerServerDetails unitOwner, final UnitStatusID initialStatus,
		final boolean addOnClients, final MomSessionVariables mom)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException;

	/**
	 * When we summon a hero, we don't 'add' it, the unit object already exists - but we still need to perform similar updates
	 * to addUnitOnServerAndClients to set the unit's location, see the area it can see, status and tell clients to update the same
	 *
	 * Although not written yet, this will also be used for bringing dead regular units back to life with the Raise Dead spell
	 *
	 * @param trueUnit The unit to set to alive
	 * @param locationToAddUnit Location to add the new unit, must be filled in
	 * @param unitOwner Player who will own the new unit, note the reason this has to be passed in separately is because the players list is allowed to be null
	 * @param addOnClients Usually true, can set to false when monsters are initially added to the map and don't need to worry about who can see them
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void updateUnitStatusToAliveOnServerAndClients (final MemoryUnit trueUnit, final MapCoordinates3DEx locationToAddUnit,
		final PlayerServerDetails unitOwner, final boolean addOnClients, final MomSessionVariables mom)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException;

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
	public void sendTransientSpellToClients (final MapVolumeOfMemoryGridCells trueTerrain, final List<MemoryUnit> trueUnits,
		final MemoryMaintainedSpell transientSpell, final List<PlayerServerDetails> players,
		final CommonDatabase db, final FogOfWarSetting fogOfWarSettings)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException;
	
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
	public void addExistingTrueMaintainedSpellToClients (final MemoryMaintainedSpell trueSpell, final boolean skipAnimation, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException;

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
	public MemoryMaintainedSpell addMaintainedSpellOnServerAndClients (final int castingPlayerID, final String spellID, final Integer unitURN, final String unitSkillID,
		final boolean castInCombat, final MapCoordinates3DEx cityLocation, final String citySpellEffectID, final Integer variableDamage, final boolean skipAnimation,
		final boolean addOnClients, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException;

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
	public void addCombatAreaEffectOnServerAndClients (final MomGeneralServerKnowledge gsk,
		final String combatAreaEffectID, final String spellID, final Integer castingPlayerID, final Integer castingCost, final MapCoordinates3DEx mapLocation,
		final List<PlayerServerDetails> players, final MomSessionDescription sd)
		throws JAXBException, XMLStreamException;

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
	public void addBuildingOnServerAndClients (final MapCoordinates3DEx cityLocation, final List<String> buildingIDs,
		final String buildingsCreatedFromSpellID, final Integer buildingCreationSpellCastByPlayerID, final boolean addOnClients, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException;

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
	public void destroyBuildingOnServerAndClients (final List<Integer> buildingURNs, final boolean updateBuildingSoldThisTurn,
		final String buildingsDestroyedBySpellID, final Integer buildingDestructionSpellCastByPlayerID, final MapCoordinates3DEx buildingDestructionSpellLocation,
		final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException;
	
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
	public void updatePlayerMemoryOfUnit (final MemoryUnit tu, final MapVolumeOfMemoryGridCells trueTerrain,
		final List<PlayerServerDetails> players, final CommonDatabase db, final FogOfWarSetting fogOfWarSettings,
		final Map<Integer, FogOfWarVisibleAreaChangedMessage> fowMessages)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException;
	
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
	 * @param players List of players in the session
	 * @param trueTerrain True terrain map
	 * @param db Lookup lists built over the XML database
	 * @param fogOfWarSettings Fog of War settings from session description
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found, or a player should know about one of the units but we can't find it in their memory
	 * @throws PlayerNotFoundException If the player who owns the unit cannot be found
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	public void sendDamageToClients (final MemoryUnit tuAttacker, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final List<ResolveAttackTarget> tuDefenders, final String attackSkillID, final String attackSpellID,
		final MapCoordinates2DEx wreckTilePosition, final Boolean wrecked,
		final boolean skipAnimation, final List<PlayerServerDetails> players, final MapVolumeOfMemoryGridCells trueTerrain,
		final CommonDatabase db, final FogOfWarSetting fogOfWarSettings)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException;

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
	public void addUnitStackIncludingSpellsToServerPlayerMemoryAndSendToClient (final List<MemoryUnit> unitStack,
		final List<MemoryMaintainedSpell> trueSpells, final PlayerServerDetails player)
		throws RecordNotFoundException, JAXBException, XMLStreamException;

	/**
	 * Similar to the above routine, this removed all the listed units from the player's memory on the server, along with any unit spells
	 * Unlike the above routine, it sends no messages to inform the client
	 *
	 * @param unitStack List of unit URNs to remove
	 * @param player Player to remove them from
	 */
	public void freeUnitStackIncludingSpellsFromServerPlayerMemoryOnly (final List<Integer> unitStack, final PlayerServerDetails player);

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
	public MapCoordinates3DEx determineMovementDirection (final MapCoordinates3DEx moveFrom, final MapCoordinates3DEx moveTo,
		final OverlandMovementCell [] [] [] moves);

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
	public void reduceMovementRemaining (final List<ExpandedUnitDetails> unitStack, final Set<String> unitStackSkills, final String tileTypeID,
		final CommonDatabase db) throws RecordNotFoundException, MomException;
}