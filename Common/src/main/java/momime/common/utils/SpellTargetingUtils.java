package momime.common.utils;

import java.util.List;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MapVolumeOfFogOfWarStates;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.servertoclient.OverlandCastingInfo;

/**
 * Methods that determine whether something is a valid target for a spell
 */
public interface SpellTargetingUtils
{
	/**
	 * Checks whether the specified spell can be targetted at the specified unit.  There's lots of validation to do for this, and the
	 * client does it in a few places and then repeated on the server, so much cleaner if we pull it out into a common routine.
	 * 
	 * In Delphi code this is named isUnitValidTargetForCombatSpell, but took "combat" word out here since its used for validating overland targets as well.
	 * 
	 * @param spell Spell being cast
	 * @param overrideSpellBookSection Usually null; filled in when a spell is of one type, but has a specially coded secondary effect of another type
	 *		For example Wall of Fire is a city enchantment for placing it, but then when we roll for damage we have to treat it like an attack spell 
	 * @param combatLocation The location that the combat is taking place; null for targetting overland spells
	 * @param combatTerrain Generated combat map; null for targetting overland spells
	 * @param castingPlayerID Player casting the spell
	 * @param castingUnit Unit casting the spell, if its a hero casting a spell or using a spell imbued into an item, or a creature like Giant Spiders casting web; null if wizard casting
	 * @param variableDamage The damage chosen, for spells where variable mana can be channeled into casting them, e.g. fire bolt; or null if the attack isn't coming from a spell
	 * @param targetUnit Unit to cast the spell on
	 * @param isTargeting True if calling this method to allow the player to target something at the unit, which means they must be able to see it,
	 * 	False if resolving damage - for example a unit we can't see is not a valid target to select, but if its hit by an area attack like ice storm, then we do damage it
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param fow Area we can currently see
	 * @param players Players list
	 * @param db Lookup lists built over the XML database
	 * @return VALID_TARGET, or an enum value indicating why it isn't a valid target
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 */
	public TargetSpellResult isUnitValidTargetForSpell (final Spell spell, final SpellBookSectionID overrideSpellBookSection, final MapCoordinates3DEx combatLocation,
		final MapAreaOfCombatTiles combatTerrain, final int castingPlayerID, final ExpandedUnitDetails castingUnit, final Integer variableDamage,
		final ExpandedUnitDetails targetUnit, final boolean isTargeting, final FogOfWarMemory mem, final MapVolumeOfFogOfWarStates fow,
		final List<? extends PlayerPublicDetails> players, final CommonDatabase db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException; 

	/**
	 * Checks whether the specified spell can be targetted at the specified city.  There's lots of validation to do for this, and the
	 * client does it in a few places and then repeated on the server, so much cleaner if we pull it out into a common routine.
	 * 
	 * In Delphi code the code for this was duplicated in both the client and server, so this method didn't exist.
	 * 
	 * @param spells List of known existing spells
	 * @param spell Spell being cast
	 * @param castingPlayerID Player casting the spell
	 * @param cityLocation City to cast the spell on
	 * @param map Known terrain
	 * @param fow Area we can currently see
	 * @param buildingsList Known buildings
	 * @param wizards Wizards list
	 * @param db Lookup lists built over the XML database
	 * @return VALID_TARGET, or an enum value indicating why it isn't a valid target
	 * @throws RecordNotFoundException If the unit has a skill that we can't find in the cache
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 */
	public TargetSpellResult isCityValidTargetForSpell (final List<MemoryMaintainedSpell> spells, final Spell spell, final int castingPlayerID,
		final MapCoordinates3DEx cityLocation, final MapVolumeOfMemoryGridCells map, final MapVolumeOfFogOfWarStates fow, final List<MemoryBuilding> buildingsList,
		final List<KnownWizardDetails> wizards, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException; 

	/**
	 * Checks whether the specified spell can be targetted at the specified overland map location.  Spells targetted specifically at
	 * cities have their own isCityValidTargetForSpell validation routine, so this is only used for spells that are targetted at a location
	 * and non-city locations are equally valid targets.  This is currently used for:
	 * Earth Lore, Enchant Road, Corruption, Disenchant Area/True.
	 * 
	 * @param spell Spell being cast
	 * @param castingPlayerID Player casting the spell
	 * @param targetLocation Location we want to cast the spell at 
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param fow Area we can currently see; passing this as null disables the FOW check for some special scenarios
	 * @param players Players list
	 * @param db Lookup lists built over the XML database
	 * @return VALID_TARGET, or an enum value indicating why it isn't a valid target
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public TargetSpellResult isOverlandLocationValidTargetForSpell (final Spell spell, final int castingPlayerID,
		final MapCoordinates3DEx targetLocation, final FogOfWarMemory mem, final MapVolumeOfFogOfWarStates fow,
		final List<? extends PlayerPublicDetails> players, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;

	/**
	 * Used for disjunction type spells being targetted at overland enchantments
	 * 
	 * @param castingPlayerID Player casting the disjunction spell
	 * @param targetSpell Overland enchantment they want to aim at
	 * @param db Lookup lists built over the XML database
	 * @return VALID_TARGET, or an enum value indicating why it isn't a valid target
	 * @throws RecordNotFoundException If we can't find the definition for the target spell in the DB
	 */
	public TargetSpellResult isSpellValidTargetForSpell (final int castingPlayerID, final MemoryMaintainedSpell targetSpell, final CommonDatabase db)
		throws RecordNotFoundException;

	/**
	 * Used for spells targeted at the Wizard themselves, like Drain Power or Spell Blast.  This is a bit awkward as the way the input params
	 * are supplied is diffferent for client and server, so have to specify them in a way that either can provide (e.g. can't use PlayerServerDetails).
	 * 
	 * @param spell Spell being cast
	 * @param castingPlayerID Player casting the spell
	 * @param castingPriv Private info for the playing casting the spell
	 * @param targetPlayerID Player to cast the spell on
	 * @param targetCastingInfo Info about what the player to cast the spell on is casting themselves
	 * @return VALID_TARGET, or an enum value indicating why it isn't a valid target
	 * @throws MomException If we encounter a spell book section we don't know how to handle
	 * @throws RecordNotFoundException If the detatils for the target wizard are missing
	 */
	public TargetSpellResult isWizardValidTargetForSpell (final Spell spell, final int castingPlayerID, final MomPersistentPlayerPrivateKnowledge castingPriv,
		final int targetPlayerID, final OverlandCastingInfo targetCastingInfo)
		throws MomException, RecordNotFoundException;
	
	/**
	 * Checks whether the specified spell can be targetted at the specified combat map location.
	 * This is only called for spells that are targetted at a location - section SPECIAL_COMBAT_SPELLS
	 * 
	 * @param spell Spell being cast
	 * @param targetLocation Location we want to cast the spell at 
	 * @param map Combat map terrain
	 * @param db Lookup lists built over the XML database
	 * @return Whether the location is a valid target or not
	 * @throws RecordNotFoundException If we can't find the a combat tile type in the DB
	 * @throws MomException If we encounter a spell book section we don't know how to handle
	 */
	public boolean isCombatLocationValidTargetForSpell (final Spell spell, final MapCoordinates2DEx targetLocation, final MapAreaOfCombatTiles map,
		final CommonDatabase db) throws RecordNotFoundException, MomException;
}