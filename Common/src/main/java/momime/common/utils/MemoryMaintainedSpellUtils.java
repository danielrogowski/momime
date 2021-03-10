package momime.common.utils;

import java.util.List;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MapVolumeOfFogOfWarStates;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryMaintainedSpell;

/**
 * Methods for working with list of MemoryMaintainedSpells
 */
public interface MemoryMaintainedSpellUtils
{
	/**
	 * Searches for a maintained spell by its details
	 *
	 * The majority of the search fields are optional, since this allows us to do searches like
	 * looking for a maintained spell that grants unit skill SSxxx, even though we don't know what spell(s) might grant that skill
	 *
	 * @param spells List of spells to search through
	 * @param castingPlayerID Player who cast the spell to search for, or null to match any
	 * @param spellID Unique identifier for the spell to search for, or null to match any
	 * @param unitURN Which unit the spell is cast on - this is mandatory, null will match only non-unit spells or untargetted unit spells
	 * @param unitSkillID Which actual unit spell effect was granted, or null to match any
	 * @param cityLocation Which city the spell is cast on - this is mandatory, null will match only non-city spells or untargetted city spells
	 * @param citySpellEffectID Which actual city spell effect was granted, or null to match any
	 * @return First matching spell found, or null if none matched
	 */
	public MemoryMaintainedSpell findMaintainedSpell (final List<MemoryMaintainedSpell> spells,
		final Integer castingPlayerID, final String spellID, final Integer unitURN, final String unitSkillID,
		final MapCoordinates3DEx cityLocation, final String citySpellEffectID);

	/**
	 * @param spellURN Spell URN to search for
	 * @param spells List of spells to search through
	 * @return Spell with requested URN, or null if not found
	 */
	public MemoryMaintainedSpell findSpellURN (final int spellURN, final List<MemoryMaintainedSpell> spells);

	/**
	 * @param spellURN Spell URN to search for
	 * @param spells List of spells to search through
	 * @param caller The routine that was looking for the value
	 * @return Spell with requested URN
	 * @throws RecordNotFoundException If spell with requested URN is not found
	 */
	public MemoryMaintainedSpell findSpellURN (final int spellURN, final List<MemoryMaintainedSpell> spells, final String caller)
		throws RecordNotFoundException;

	/**
	 * @param spellURN Spell URN to remove
	 * @param spells List of spells to search through
	 * @throws RecordNotFoundException If spell with requested URN is not found
	 */
	public void removeSpellURN (final int spellURN, final List<MemoryMaintainedSpell> spells)
		throws RecordNotFoundException;

	/**
	 * Removes all spells from the list that are cast on any of the units listed
	 *
	 * @param spells List of spells to search through
	 * @param unitURNs List of units to remove spells from
	 */
	public void removeSpellsCastOnUnitStack (final List<MemoryMaintainedSpell> spells, final List<Integer> unitURNs);

	/**
	 * When trying to cast a spell on a unit, this will make a list of all the unit spell effect IDs for that spell that aren't already in effect on that unit.
	 * This is mainly to deal with Warp Creature which has 3 seperate effects and can be multi-cast to get all 3 effects.
	 * 
	 * @param spells List of spells to check against
	 * @param spell Spell being cast
	 * @param castingPlayerID Player casting the spell
	 * @param unitURN Which unit the spell is being case on
	 * @return Null = this spell has no unitSpellEffectIDs defined; empty list = has effect(s) defined but they're all cast on this unit already; non-empty list = list of effects that can still be cast
	 */
	public List<String> listUnitSpellEffectsNotYetCastOnUnit (final List<MemoryMaintainedSpell> spells, final Spell spell,
		final int castingPlayerID, final int unitURN);
	
	/**
	 * When trying to cast a spell on a city, this will make a list of all the city spell effect IDs for that spell that aren't already in effect on that city.
	 * This is mainly to deal with Spell Ward - we might have a Nature and Chaos Ward in place
	 * already, in that case this method will tell us that we can still cast a Life, Death or Sorcery Ward.
	 * 
	 * @param spells List of spells to check against
	 * @param spell Spell being cast
	 * @param castingPlayerID Player casting the spell
	 * @param cityLocation Location of the city
	 * @return Null = this spell has no citySpellEffectIDs defined; empty list = has effect(s) defined but they're all cast on this city already; non-empty list = list of effects that can still be cast
	 */
	public List<String> listCitySpellEffectsNotYetCastAtLocation (final List<MemoryMaintainedSpell> spells, final Spell spell,
		final int castingPlayerID, final MapCoordinates3DEx cityLocation);
	
	/**
	 * Checks whether the specified spell can be targetted at the specified unit.  There's lots of validation to do for this, and the
	 * client does it in a few places and then repeated on the server, so much cleaner if we pull it out into a common routine.
	 * 
	 * In Delphi code this is named isUnitValidTargetForCombatSpell, but took "combat" word out here since its used for validating overland targets as well.
	 * 
	 * @param spell Spell being cast
	 * @param combatLocation The location that the combat is taking place; null for targetting overland spells
	 * @param castingPlayerID Player casting the spell
	 * @param variableDamage The damage chosen, for spells where variable mana can be channeled into casting them, e.g. fire bolt; or null if the attack isn't coming from a spell
	 * @param unit Unit to cast the spell on
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return VALID_TARGET, or an enum value indicating why it isn't a valid target
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 */
	public TargetSpellResult isUnitValidTargetForSpell (final Spell spell, final MapCoordinates3DEx combatLocation,
		final int castingPlayerID, final Integer variableDamage, final ExpandedUnitDetails unit,
		final FogOfWarMemory mem, final CommonDatabase db) throws RecordNotFoundException, MomException, PlayerNotFoundException; 

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
	 * @return VALID_TARGET, or an enum value indicating why it isn't a valid target
	 * @throws RecordNotFoundException If the unit has a skill that we can't find in the cache
	 */
	public TargetSpellResult isCityValidTargetForSpell (final List<MemoryMaintainedSpell> spells, final Spell spell, final int castingPlayerID,
		final MapCoordinates3DEx cityLocation, final MapVolumeOfMemoryGridCells map, final MapVolumeOfFogOfWarStates fow, final List<MemoryBuilding> buildingsList)
		throws RecordNotFoundException; 

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
	 * @param fow Area we can currently see
	 * @param db Lookup lists built over the XML database
	 * @return VALID_TARGET, or an enum value indicating why it isn't a valid target
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the db
	 */
	public TargetSpellResult isOverlandLocationValidTargetForSpell (final Spell spell, final int castingPlayerID, final MapCoordinates3DEx targetLocation,
		final FogOfWarMemory mem, final MapVolumeOfFogOfWarStates fow, final CommonDatabase db) throws RecordNotFoundException;

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
	 * Checks whether the specified spell can be targetted at the specified combat map location.
	 * This is only called for spells that are targetted at a location - section SPECIAL_COMBAT_SPELLS
	 * 
	 * @param spell Spell being cast
	 * @param targetLocation Location we want to cast the spell at 
	 * @param map Combat map terrain
	 * @return Whether the location is a valid target or not
	 */
	public boolean isCombatLocationValidTargetForSpell (final Spell spell, final MapCoordinates2DEx targetLocation, final MapAreaOfCombatTiles map);
}