package momime.common.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DamageResolutionTypeID;
import momime.common.database.DamageType;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.UnitSpellEffect;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.FogOfWarStateID;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MapVolumeOfFogOfWarStates;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MomCombatTile;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitStatusID;
import momime.common.messages.WizardState;
import momime.common.messages.servertoclient.OverlandCastingInfo;

/**
 * Methods for working with list of MemoryMaintainedSpells
 */
public final class MemoryMaintainedSpellUtilsImpl implements MemoryMaintainedSpellUtils
{
	/** Spell utils */
	private SpellUtils spellUtils;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** Kind of spell utils */
	private KindOfSpellUtils kindOfSpellUtils;
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** MemoryGridCell utils */
	private MemoryGridCellUtils memoryGridCellUtils;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/**
	 * Searches for a maintained spell in a list
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
	@Override
	public final MemoryMaintainedSpell findMaintainedSpell (final List<MemoryMaintainedSpell> spells,
		final Integer castingPlayerID, final String spellID, final Integer unitURN, final String unitSkillID,
		final MapCoordinates3DEx cityLocation, final String citySpellEffectID)
	{
		MemoryMaintainedSpell match = null;
		final Iterator<MemoryMaintainedSpell> iter = spells.iterator ();

		while ((match == null) && (iter.hasNext ()))
		{
			final MemoryMaintainedSpell thisSpell = iter.next ();

			if (((castingPlayerID == null) || (castingPlayerID == thisSpell.getCastingPlayerID ())) &&
				((spellID == null) || (spellID.equals (thisSpell.getSpellID ()))) &&
				(CompareUtils.safeIntegerCompare (unitURN,  thisSpell.getUnitURN ())) &&
				((unitSkillID == null) || (unitSkillID.equals (thisSpell.getUnitSkillID ()))) &&
				(CompareUtils.safeOverlandMapCoordinatesCompare (cityLocation, (MapCoordinates3DEx) thisSpell.getCityLocation ())) &&
				((citySpellEffectID == null) || (citySpellEffectID.equals (thisSpell.getCitySpellEffectID ()))))

				match = thisSpell;
		}

		return match;
	}

	/**
	 * @param spellURN Spell URN to search for
	 * @param spells List of spells to search through
	 * @return Spell with requested URN, or null if not found
	 */
	@Override
	public final MemoryMaintainedSpell findSpellURN (final int spellURN, final List<MemoryMaintainedSpell> spells)
	{
		MemoryMaintainedSpell match = null;
		final Iterator<MemoryMaintainedSpell> iter = spells.iterator ();

		while ((match == null) && (iter.hasNext ()))
		{
			final MemoryMaintainedSpell thisSpell = iter.next ();
			if (thisSpell.getSpellURN () == spellURN)
				match = thisSpell;
		}

		return match;
	}

	/**
	 * @param spellURN Spell URN to search for
	 * @param spells List of spells to search through
	 * @param caller The routine that was looking for the value
	 * @return Spell with requested URN
	 * @throws RecordNotFoundException If spell with requested URN is not found
	 */
	@Override
	public final MemoryMaintainedSpell findSpellURN (final int spellURN, final List<MemoryMaintainedSpell> spells, final String caller)
		throws RecordNotFoundException
	{
			final MemoryMaintainedSpell match = findSpellURN (spellURN, spells);

			if (match == null)
				throw new RecordNotFoundException (MemoryMaintainedSpell.class, spellURN, caller);
			
			return match;
	}

	/**
	 * @param spellURN Spell URN to remove
	 * @param spells List of spells to search through
	 * @throws RecordNotFoundException If spell with requested URN is not found
	 */
	@Override
	public final void removeSpellURN (final int spellURN, final List<MemoryMaintainedSpell> spells)
		throws RecordNotFoundException
	{
		boolean found = false;
		final Iterator<MemoryMaintainedSpell> iter = spells.iterator ();
		while ((!found) && (iter.hasNext ()))
		{
			final MemoryMaintainedSpell thisSpell = iter.next ();
			if (thisSpell.getSpellURN () == spellURN)
			{
				iter.remove ();
				found = true;
			}
		}

		if (!found)
			throw new RecordNotFoundException (MemoryMaintainedSpell.class, spellURN, "removeSpellURN");
	}

	/**
	 * Removes all spells from the list that are cast on any of the units listed
	 *
	 * @param spells List of spells to search through
	 * @param unitURNs List of units to remove spells from
	 */
	@Override
	public final void removeSpellsCastOnUnitStack (final List<MemoryMaintainedSpell> spells, final List<Integer> unitURNs)
	{
		final Iterator<MemoryMaintainedSpell> iter = spells.iterator ();
    	while (iter.hasNext ())
    	{
    		final Integer thisUnitURN = iter.next ().getUnitURN ();

    		if ((thisUnitURN != null) && (unitURNs.contains (thisUnitURN)))
    			iter.remove ();
    	}
	}

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
	@Override
	public final List<UnitSpellEffect> listUnitSpellEffectsNotYetCastOnUnit (final List<MemoryMaintainedSpell> spells, final Spell spell,
		final int castingPlayerID, final int unitURN)
	{
    	final List<UnitSpellEffect> unitSpellEffects;
    	
    	if (spell.getUnitSpellEffect ().size () == 0)
    		unitSpellEffects = null;
    	else
    	{
    		unitSpellEffects = new ArrayList<UnitSpellEffect> ();
    		for (final UnitSpellEffect effect : spell.getUnitSpellEffect ())
    			
    			// Ignore permanent effects, since they aren't choosable
    			if ((effect.isPermanent () == null) || (!effect.isPermanent ()))
				{
    				MemoryMaintainedSpell found = findMaintainedSpell (spells, castingPlayerID, spell.getSpellID (), unitURN, effect.getUnitSkillID (), null, null);
    				
    				// Stasis gets converted to a different unitSkillID after one turn, so have to look for both
    				if ((found == null) && (effect.getUnitSkillID ().equals (CommonDatabaseConstants.UNIT_SKILL_ID_STASIS_FIRST_TURN)))
    					found = findMaintainedSpell (spells, castingPlayerID, spell.getSpellID (), unitURN, CommonDatabaseConstants.UNIT_SKILL_ID_STASIS_LATER_TURNS, null, null);
    				
    				// Web is an exception - we can recast it as long as the previous web has been destroyed and only the remnants remain that prevent flying
    				if ((found == null) || ((effect.getUnitSkillID ().equals (CommonDatabaseConstants.UNIT_SKILL_ID_WEB)) && (found.getVariableDamage () == null)))
    					unitSpellEffects.add (effect);
				}
    	}

    	return unitSpellEffects;
	}
	
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
	@Override
	public final List<String> listCitySpellEffectsNotYetCastAtLocation (final List<MemoryMaintainedSpell> spells, final Spell spell,
		final int castingPlayerID, final MapCoordinates3DEx cityLocation)
	{
    	final List<String> citySpellEffectIDs;
    	
    	if (spell.getSpellHasCityEffect ().size () == 0)
    		citySpellEffectIDs = null;
    	else
    	{
    		citySpellEffectIDs = new ArrayList<String> ();
    		for (final String citySpellEffectID : spell.getSpellHasCityEffect ())
    			if (findMaintainedSpell (spells, castingPlayerID, spell.getSpellID (), null, null, cityLocation, citySpellEffectID) == null)
   					citySpellEffectIDs.add (citySpellEffectID);
    	}

    	return citySpellEffectIDs;
	}

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
	 * @param castingPlayerID Player casting the spell
	 * @param castingUnit Unit casting the spell, if its a hero casting a spell or using a spell imbued into an item, or a creature like Giant Spiders casting web; null if wizard casting
	 * @param variableDamage The damage chosen, for spells where variable mana can be channeled into casting them, e.g. fire bolt; or null if the attack isn't coming from a spell
	 * @param targetUnit Unit to cast the spell on
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param players Players list
	 * @param db Lookup lists built over the XML database
	 * @return VALID_TARGET, or an enum value indicating why it isn't a valid target
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 */
	@Override
	public final TargetSpellResult isUnitValidTargetForSpell (final Spell spell, final SpellBookSectionID overrideSpellBookSection, final MapCoordinates3DEx combatLocation,
		final int castingPlayerID, final ExpandedUnitDetails castingUnit, final Integer variableDamage, final ExpandedUnitDetails targetUnit,
		final FogOfWarMemory mem, final List<? extends PlayerPublicDetails> players, final CommonDatabase db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException
	{
    	final TargetSpellResult result;
    	final int targetUnitURN = targetUnit.getUnitURN ();
    	final SpellBookSectionID useSpellBookSection = (overrideSpellBookSection != null) ? overrideSpellBookSection : spell.getSpellBookSectionID ();
    	
    	// Simpify identifying some kinds of spells so don't have to repeat this all over the place
    	final KindOfSpell kind = getKindOfSpellUtils ().determineKindOfSpell (spell, overrideSpellBookSection);
    	
    	// If we're trying to cast a combat spell, then the unit must be in the right combat
    	if ((combatLocation != null) && ((!combatLocation.equals (targetUnit.getCombatLocation ())) ||
    		(targetUnit.getCombatPosition () == null) || (targetUnit.getCombatSide () == null) || (targetUnit.getCombatHeading () == null)))
    		
    		result = TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT;
    	
    	// For anything other than raise dead-type spell, target unit must be alive
    	else if ((kind != KindOfSpell.RAISE_DEAD) && (targetUnit.getStatus () != UnitStatusID.ALIVE))
    		result = TargetSpellResult.UNIT_DEAD;

    	// For raise dead-type spells, target unit must be dead
    	else if ((kind == KindOfSpell.RAISE_DEAD) && (targetUnit.getStatus () != UnitStatusID.DEAD))
       		result = TargetSpellResult.UNIT_NOT_DEAD;
    	
    	// Casting something beneficial on an enemy unit
    	else if (((useSpellBookSection == SpellBookSectionID.UNIT_ENCHANTMENTS) || (useSpellBookSection == SpellBookSectionID.SPECIAL_UNIT_SPELLS)) &&
    		(targetUnit.getOwningPlayerID () != castingPlayerID))
    		result = TargetSpellResult.ENCHANTING_OR_HEALING_ENEMY; 

    	// Casting something nasty on a friendly unit
    	else if (((useSpellBookSection == SpellBookSectionID.UNIT_CURSES) || (useSpellBookSection == SpellBookSectionID.ATTACK_SPELLS)) &&
    		(targetUnit.getOwningPlayerID () == castingPlayerID))
    		result = TargetSpellResult.CURSING_OR_ATTACKING_OWN;
    	
    	// Trying to raise dead an enemy unit with a spell that doesn't explicitly allow this
    	else if ((kind == KindOfSpell.RAISE_DEAD) && ((spell.isResurrectEnemyUnits () == null) ||
    		(!spell.isResurrectEnemyUnits ())) && (targetUnit.getOwningPlayerID () != castingPlayerID))
    		result = TargetSpellResult.RAISING_ENEMY;
    	
    	// Trying to plane shift while planar seal is in effect
    	else if ((kind == KindOfSpell.PLANE_SHIFT) && (findMaintainedSpell
			(mem.getMaintainedSpell (), null, CommonDatabaseConstants.SPELL_ID_PLANAR_SEAL, null, null, null, null) != null))
    		result = TargetSpellResult.PLANAR_SEAL;
    	
    	// Trying to plane shift while standing in a tower
    	else if ((kind == KindOfSpell.PLANE_SHIFT) && (getMemoryGridCellUtils ().isTerrainTowerOfWizardry
    		(mem.getMap ().getPlane ().get (targetUnit.getUnitLocation ().getZ ()).getRow ().get (targetUnit.getUnitLocation ().getY ()).getCell ().get (targetUnit.getUnitLocation ().getX ()).getTerrainData ())))
    		result = TargetSpellResult.INVALID_MAP_FEATURE;
    	
    	else
    	{
    		// Now check unitSpellEffectIDs
    		final boolean unitSpellEffectRequired = (kind == KindOfSpell.UNIT_ENCHANTMENTS) ||
    			(useSpellBookSection == SpellBookSectionID.UNIT_CURSES);
    		
    		final List<UnitSpellEffect> unitSpellEffects = listUnitSpellEffectsNotYetCastOnUnit (mem.getMaintainedSpell (), spell, castingPlayerID, targetUnitURN);
    		if ((unitSpellEffectRequired) && (unitSpellEffects == null))
    			result = TargetSpellResult.NO_SPELL_EFFECT_IDS_DEFINED;
    		
    		else if ((unitSpellEffectRequired) && (unitSpellEffects.size () == 0))
    			result = TargetSpellResult.ALREADY_HAS_ALL_POSSIBLE_SPELL_EFFECTS;
    		
    		else if ((unitSpellEffectRequired) && (getUnitUtils ().isUnitImmuneToSpellEffects (targetUnit, unitSpellEffects, db)))
    			result = TargetSpellResult.IMMUNE;
    		
    		else if (!getSpellUtils ().spellCanTargetMagicRealmLifeformType (spell, targetUnit.getModifiedUnitMagicRealmLifeformType ().getPickID ()))
    			result = TargetSpellResult.UNIT_INVALID_MAGIC_REALM_LIFEFORM_TYPE;

    		else if ((unitSpellEffectRequired) && (getUnitUtils ().isExperienceBonusAndWeAlreadyHaveTooMuch (targetUnit, unitSpellEffects, db)))
    			result = TargetSpellResult.TOO_MUCH_EXPERIENCE;
    		
    		// combatBaseDamage being not null is what identifies a special unit spell to be a healing spell
    		else if ((kind == KindOfSpell.HEALING) && (getUnitUtils ().getTotalDamageTaken (targetUnit.getUnitDamage ()) == 0)) 
    			result = TargetSpellResult.UNDAMAGED;

    		else if ((kind == KindOfSpell.HEALING) && (getUnitUtils ().getHealableDamageTaken (targetUnit.getUnitDamage ()) == 0))
    			result = TargetSpellResult.PERMANENTLY_DAMAGED;
    		
    		else if ((kind == KindOfSpell.HEALING) && ((targetUnit.getModifiedUnitMagicRealmLifeformType ().isHealEachTurn () == null) ||
    			(!targetUnit.getModifiedUnitMagicRealmLifeformType ().isHealEachTurn ())))
    			result = TargetSpellResult.UNHEALABLE_LIFEFORM_TYPE;
    		
    		else if (kind == KindOfSpell.CHANGE_UNIT_ID)
    		{
    			// Test that the place where the old unit is is valid terrain for the new kind of unit
    			// Ignoring complicated situations here like, what if the old unit is being transported on a ship, or what if its stacked with a unit who has Wind Walking
    			final AvailableUnit unit = new AvailableUnit ();
				unit.setOwningPlayerID (castingPlayerID);
				unit.setUnitID (spell.getSummonedUnit ().get (0));
				
				getUnitUtils ().initializeUnitSkills (unit, null, db);

				final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (unit, null, null, null, players, mem, db);

		    	final OverlandMapTerrainData terrainData = mem.getMap ().getPlane ().get (targetUnit.getUnitLocation ().getZ ()).getRow
	    			().get (targetUnit.getUnitLocation ().getY ()).getCell ().get (targetUnit.getUnitLocation ().getX ()).getTerrainData ();
				
    			if (getUnitCalculations ().calculateDoubleMovementToEnterTileType (xu, xu.listModifiedSkillIDs (), terrainData.getTileTypeID (), db) == null)
    				result = TargetSpellResult.TERRAIN_IMPASSABLE;
    			
	    		else
	    			result = TargetSpellResult.VALID_TARGET;
    		}
    		
    		else
    		{
    			final List<String> spellsImmuneToDispelling = db.getSpell ().stream ().filter
    				(s -> (s.isImmuneToDispelling () != null) && (s.isImmuneToDispelling ())).map (s -> s.getSpellID ()).collect (Collectors.toList ());
    			
    			// We're in a method that specifically deals with targeting units, so we don't have to worry about Disenchant Area
    			// also being able to target spells cast at the location, or Disjunction targeting overland enchantments
	    		if ((useSpellBookSection == SpellBookSectionID.DISPEL_SPELLS) &&
	    			(mem.getMaintainedSpell ().stream ().noneMatch (s -> (s.getUnitURN () != null) && (s.getUnitURN () == targetUnitURN) &&
	    				(s.getCastingPlayerID () != castingPlayerID) && (!spellsImmuneToDispelling.contains (s.getSpellID ())))))
	    			result = TargetSpellResult.NOTHING_TO_DISPEL;
	    		
	    		else if ((useSpellBookSection != SpellBookSectionID.ATTACK_SPELLS) || (combatLocation == null))
	    			result = TargetSpellResult.VALID_TARGET;
	    		
	    		else
	    		{
	    			// Combat attack spell - immunity skill?
	    			final DamageType damageType = db.findDamageType (spell.getAttackSpellDamageTypeID (), "isUnitValidTargetForSpell");
	    			if (targetUnit.isUnitImmuneToDamageType (damageType))
	    				result = TargetSpellResult.IMMUNE;
	    			
	    			// Immune due to resistance?
	    			else if (CommonDatabaseConstants.RESISTANCE_BASED_DAMAGE.contains (spell.getAttackSpellDamageResolutionTypeID ()))
	    			{
    					// Units with 10 or more resistance are immune to spells that roll against resistance
    					// First need to take into account if there's a saving throw modifier
    					// NB. Resistance rolls damage allows no saving throw modifier because in this case, the skill value represents the number of rolls to make
    					int resistance = Math.max (0, targetUnit.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE));
    					if (spell.getAttackSpellDamageResolutionTypeID () != DamageResolutionTypeID.RESISTANCE_ROLLS)
    					{
    						final Integer savingThrowModifier = ((spell.getCombatMaxDamage () == null) || (variableDamage == null)) ? spell.getCombatBaseDamage () : variableDamage;
    						if (savingThrowModifier != null)
    							resistance = resistance - savingThrowModifier;

	    					// Heroes with -spell save items
	    					if ((castingUnit != null) && (castingUnit.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_SAVING_THROW_PENALTY)))
	    						resistance = resistance - castingUnit.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_SAVING_THROW_PENALTY);
    					}
    					
    					if (resistance >= 10)
    						result = TargetSpellResult.TOO_HIGH_RESISTANCE;
    					else
    						result = TargetSpellResult.VALID_TARGET;
	    			}
	    			
	    			// Warp wood has special validation
	    			else if (spell.getAttackSpellDamageResolutionTypeID () == DamageResolutionTypeID.ZEROES_AMMO)
	    			{
	    				if (!targetUnit.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK))
	    					result = TargetSpellResult.NO_RANGED_ATTACK;
	    				else if (!targetUnit.getRangedAttackType ().isWooden ())
	    					result = TargetSpellResult.INVALID_RANGED_ATTACK_TYPE;
	    				else if (targetUnit.getAmmoRemaining () <= 0)
	    					result = TargetSpellResult.NO_AMMUNITION;
	    				else
	    					result = TargetSpellResult.VALID_TARGET;
	    			}
	    			
					// Combat attack spell that rolls against something other than resistance, so always a valid target
	    			else
    	    			result = TargetSpellResult.VALID_TARGET;
	    		}
    		}
    	}

    	return result;
	}

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
	 * @param players Players list
	 * @return VALID_TARGET, or an enum value indicating why it isn't a valid target
	 * @throws RecordNotFoundException If the unit has a skill that we can't find in the cache
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 */
	@Override
	public final TargetSpellResult isCityValidTargetForSpell (final List<MemoryMaintainedSpell> spells, final Spell spell, final int castingPlayerID,
		final MapCoordinates3DEx cityLocation, final MapVolumeOfMemoryGridCells map, final MapVolumeOfFogOfWarStates fow, final List<MemoryBuilding> buildingsList,
		final List<? extends PlayerPublicDetails> players)
		throws RecordNotFoundException, PlayerNotFoundException
	{
    	final TargetSpellResult result;
    	final OverlandMapCityData cityData = map.getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();

    	// Do easy checks first
    	if (fow.getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()) != FogOfWarStateID.CAN_SEE)
    		result = TargetSpellResult.CANNOT_SEE_TARGET;

    	else if (cityData == null)
    		result = TargetSpellResult.NO_CITY_HERE;
    	
    	else if ((spell.getSpellBookSectionID () == SpellBookSectionID.CITY_ENCHANTMENTS) && (cityData.getCityOwnerID () != castingPlayerID))
    		result = TargetSpellResult.ENCHANTING_OR_HEALING_ENEMY; 

    	else if (((spell.getSpellBookSectionID () == SpellBookSectionID.CITY_CURSES) || (spell.getSpellBookSectionID () == SpellBookSectionID.ATTACK_SPELLS))
    		&& (cityData.getCityOwnerID () == castingPlayerID))
    		result = TargetSpellResult.CURSING_OR_ATTACKING_OWN;
    	
    	// Is it a spell that creates a building?
    	else if (spell.getBuildingID () != null)
    	{
    		if (getMemoryBuildingUtils ().findBuilding (buildingsList, cityLocation, spell.getBuildingID ()) != null)
    			result = TargetSpellResult.CITY_ALREADY_HAS_BUILDING;
    		else
    			result = TargetSpellResult.VALID_TARGET;
    	}
    	
    	// Attack spells don't need citySpellEffectIDs, so at this point those are done
    	else if (spell.getSpellBookSectionID () == SpellBookSectionID.ATTACK_SPELLS)
    		result = TargetSpellResult.VALID_TARGET;
    	
    	else
    	{
    		// Now check citySpellEffectIDs
    		final List<String> citySpellEffectIDs = listCitySpellEffectsNotYetCastAtLocation (spells, spell, castingPlayerID, cityLocation);
    		if (citySpellEffectIDs == null)
    			result = TargetSpellResult.NO_SPELL_EFFECT_IDS_DEFINED;
    		
    		else if (citySpellEffectIDs.size () == 0)
    			result = TargetSpellResult.ALREADY_HAS_ALL_POSSIBLE_SPELL_EFFECTS;
    		
    		else if (citySpellEffectIDs.size () > 1)
    			result = TargetSpellResult.VALID_TARGET;
    		
    		else if (!citySpellEffectIDs.get (0).equals (CommonDatabaseConstants.CITY_SPELL_EFFECT_ID_EVIL_PRESENCE))
    			result = TargetSpellResult.VALID_TARGET;

    		else
    		{
    			// Special validation for Evil Presence - make sure the city owner has no Death Books
    			final PlayerPublicDetails cityOwner = getMultiplayerSessionUtils ().findPlayerWithID (players, cityData.getCityOwnerID (), "isCityValidTargetForSpell");
    			final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) cityOwner.getPersistentPlayerPublicKnowledge ();
    			if (getPlayerPickUtils ().getQuantityOfPick (pub.getPick (), CommonDatabaseConstants.PICK_ID_DEATH_BOOK) > 0)
    				result = TargetSpellResult.WIZARD_HAS_DEATH_BOOKS;
    			else
    				result = TargetSpellResult.VALID_TARGET;
    		}
    	}

    	return result;
	}

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
	 * @param players Players list
	 * @param db Lookup lists built over the XML database
	 * @return VALID_TARGET, or an enum value indicating why it isn't a valid target
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final TargetSpellResult isOverlandLocationValidTargetForSpell (final Spell spell, final int castingPlayerID, final MapCoordinates3DEx targetLocation,
		final FogOfWarMemory mem, final MapVolumeOfFogOfWarStates fow, final List<? extends PlayerPublicDetails> players, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
    	final OverlandMapTerrainData terrainData = mem.getMap ().getPlane ().get
    		(targetLocation.getZ ()).getRow ().get (targetLocation.getY ()).getCell ().get (targetLocation.getX ()).getTerrainData ();
    	
    	// Simpify identifying some kinds of spells so don't have to repeat this all over the place
    	final KindOfSpell kind = getKindOfSpellUtils ().determineKindOfSpell (spell, null);
    	
    	// Earth Lore and Enchant Road can always be targeted anywhere, even in blackness where we've never seen before
		TargetSpellResult result;
    	if ((kind == KindOfSpell.EARTH_LORE) || (kind == KindOfSpell.ENCHANT_ROAD))
    		result = TargetSpellResult.VALID_TARGET;
    	
    	// Every other kind of spell must be targetted at a location we can see - you can't blindy throw Disenchant Area or
    	// Corruption at map cells with out of date info about enemy spells that may or may not still be there
    	else if (fow.getPlane ().get (targetLocation.getZ ()).getRow ().get (targetLocation.getY ()).getCell ().get (targetLocation.getX ()) != FogOfWarStateID.CAN_SEE)
    		result = TargetSpellResult.CANNOT_SEE_TARGET;
    	
    	// Can only target certain tile types?
    	else if (spell.getSpellValidTileTypeTarget ().size () > 0)
    	{
	    	if ((terrainData == null) || (terrainData.getTileTypeID () == null))
	    		result = TargetSpellResult.INVALID_TILE_TYPE;
	    	else if (spell.getSpellValidTileTypeTarget ().stream ().anyMatch (t -> t.getTileTypeID ().equals (terrainData.getTileTypeID ())))
	    		result = TargetSpellResult.VALID_TARGET;
	    	else
	    		result = TargetSpellResult.INVALID_TILE_TYPE;
    	}
    	else
    		result = TargetSpellResult.VALID_TARGET;

    	// Can only target certain map features?
    	if ((result == TargetSpellResult.VALID_TARGET) && (spell.getSpellValidMapFeatureTarget ().size () > 0))
    	{
	    	if ((terrainData == null) || (terrainData.getMapFeatureID () == null))
	    		result = TargetSpellResult.INVALID_MAP_FEATURE;
	    	else if (spell.getSpellValidMapFeatureTarget ().stream ().anyMatch (t -> t.getMapFeatureID ().equals (terrainData.getMapFeatureID ())))
	    		result = TargetSpellResult.VALID_TARGET;
	    	else
	    		result = TargetSpellResult.INVALID_MAP_FEATURE;
    	}
    	
    	// If valid so far, then do checks for specific kinds of spells
    	if (result == TargetSpellResult.VALID_TARGET)
   		{
    		if (kind == KindOfSpell.CORRUPTION)
	    	{
		    	if (terrainData.getCorrupted () != null)
		    		result = TargetSpellResult.ALREADY_HAS_ALL_POSSIBLE_SPELL_EFFECTS;
	    	}
	    	
	    	// This is only used for Floating Island
	    	else if (spell.getSpellBookSectionID () == SpellBookSectionID.SUMMONING)
	    	{
	    		if (getUnitUtils ().findFirstAliveEnemyAtLocation (mem.getUnit (), targetLocation.getX (), targetLocation.getY (), targetLocation.getZ (), castingPlayerID) != null)
	    			result = TargetSpellResult.ENEMIES_HERE;
	
	    		else if (getUnitUtils ().countAliveEnemiesAtLocation (mem.getUnit (), targetLocation.getX (), targetLocation.getY (), targetLocation.getZ (), 0) > CommonDatabaseConstants.MAX_UNITS_PER_MAP_CELL)
	    			result = TargetSpellResult.CELL_FULL;
	    		
	    		else
	   			{
	    			final AvailableUnit unit = new AvailableUnit ();
					unit.setOwningPlayerID (castingPlayerID);
					unit.setUnitID (spell.getSummonedUnit ().get (0));
					
					getUnitUtils ().initializeUnitSkills (unit, null, db);
	
					final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (unit, null, null, null, players, mem, db);
	
	    			if (getUnitCalculations ().calculateDoubleMovementToEnterTileType (xu, xu.listModifiedSkillIDs (), terrainData.getTileTypeID (), db) == null)
	    				result = TargetSpellResult.TERRAIN_IMPASSABLE;
	    			
		    		else
		    			result = TargetSpellResult.VALID_TARGET;
	   			}
	    	}
	    	
	    	else if (kind == KindOfSpell.DISPEL_UNIT_CITY_COMBAT_SPELLS)
	    	{
	    		// Get a list of units at the location (ours as well)
	    		final List<Integer> unitURNs = mem.getUnit ().stream ().filter (u -> targetLocation.equals (u.getUnitLocation ())).map (u -> u.getUnitURN ()).collect (Collectors.toList ());
	    		
	    		// Now look for any spells cast by somebody else either targetted directly on the location, or on a unit at the location
	    		if (mem.getMaintainedSpell ().stream ().anyMatch (s -> (s.getCastingPlayerID () != castingPlayerID) &&
	    			((targetLocation.equals (s.getCityLocation ())) || (unitURNs.contains (s.getUnitURN ())))))
	    			
	    			result = TargetSpellResult.VALID_TARGET;
	    		else
	    			result = TargetSpellResult.NOTHING_TO_DISPEL;
	    	}
   		}
    	
    	return result;
	}
	
	/**
	 * Used for disjunction type spells being targetted at overland enchantments
	 * 
	 * @param castingPlayerID Player casting the disjunction spell
	 * @param targetSpell Overland enchantment they want to aim at
	 * @param db Lookup lists built over the XML database
	 * @return VALID_TARGET, or an enum value indicating why it isn't a valid target
	 * @throws RecordNotFoundException If we can't find the definition for the target spell in the DB
	 */
	@Override
	public final TargetSpellResult isSpellValidTargetForSpell (final int castingPlayerID, final MemoryMaintainedSpell targetSpell, final CommonDatabase db)
		throws RecordNotFoundException
	{
		final Spell spell = db.findSpell (targetSpell.getSpellID (), "isSpellValidTargetForSpell");
		
    	final TargetSpellResult result;
    	if (targetSpell.getCastingPlayerID () == castingPlayerID)
    		result = TargetSpellResult.CURSING_OR_ATTACKING_OWN;
    	
    	else if (spell.getSpellBookSectionID () != SpellBookSectionID.OVERLAND_ENCHANTMENTS)
    		result = TargetSpellResult.OVERLAND_ENCHANTMENTS_ONLY;
    	
    	else
    		result = TargetSpellResult.VALID_TARGET;
    	
    	return result;
	}
	
	/**
	 * Used for spells targeted at the Wizard themselves, like Drain Power or Spell Blast.  This is a bit awkward as the way the input params
	 * are supplied is diffferent for client and server, so have to specify them in a way that either can provide (e.g. can't use PlayerServerDetails).
	 * 
	 * @param spell Spell being cast
	 * @param castingPlayerID Player casting the spell
	 * @param castingPriv Private info for the playing casting the spell
	 * @param targetPlayer Player to cast the spell on
	 * @param targetCastingInfo Info about what the player to cast the spell on is casting themselves
	 * @return VALID_TARGET, or an enum value indicating why it isn't a valid target
	 * @throws MomException If we encounter a spell book section we don't know how to handle
	 */
	@Override
	public final TargetSpellResult isWizardValidTargetForSpell (final Spell spell, final int castingPlayerID, final MomPersistentPlayerPrivateKnowledge castingPriv,
		final PlayerPublicDetails targetPlayer, final OverlandCastingInfo targetCastingInfo) throws MomException
	{
    	final TargetSpellResult result;
    	if (castingPlayerID == targetPlayer.getPlayerDescription ().getPlayerID ())
    		result = TargetSpellResult.ATTACKING_OWN_WIZARD;
    	
    	else
    	{
    		final MomPersistentPlayerPublicKnowledge targetPub = (MomPersistentPlayerPublicKnowledge) targetPlayer.getPersistentPlayerPublicKnowledge ();
    		if (targetPub.getWizardState () != WizardState.ACTIVE)
    			result = TargetSpellResult.WIZARD_BANISHED_OR_DEFEATED;
    		
    		else if (!PlayerKnowledgeUtils.isWizard (targetPub.getWizardID ()))
    			result = TargetSpellResult.NOT_A_WIZARD;
    		
    		// Above checks are only ones that apply if the spell is anything other than Spell Blast
    		else if (getKindOfSpellUtils ().determineKindOfSpell (spell, null) == KindOfSpell.ENEMY_WIZARD_SPELLS)
    			result = TargetSpellResult.VALID_TARGET;
    		
    		else if ((targetCastingInfo == null) || (targetCastingInfo.getSpellID () == null) || (targetCastingInfo.getManaSpentOnCasting () == null))
    			result = TargetSpellResult.NO_SPELL_BEING_CAST;
    		
    		else if (getResourceValueUtils ().findAmountStoredForProductionType (castingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA) <
    			targetCastingInfo.getManaSpentOnCasting ())
    			result = TargetSpellResult.INSUFFICIENT_MANA;
    		
    		else
    			result = TargetSpellResult.VALID_TARGET;
    	}

		return result;
	}
	
	/**
	 * Checks whether the specified spell can be targetted at the specified combat map location.
	 * This is only called for spells that are targetted at a location - section SPECIAL_COMBAT_SPELLS
	 * 
	 * @param spell Spell being cast
	 * @param targetLocation Location we want to cast the spell at 
	 * @param map Combat map terrain
	 * @return Whether the location is a valid target or not
	 */
	@Override
	public final boolean isCombatLocationValidTargetForSpell (final Spell spell, final MapCoordinates2DEx targetLocation, final MapAreaOfCombatTiles map)
	{
    	final boolean result;
		final MomCombatTile tile = map.getRow ().get (targetLocation.getY ()).getCell ().get (targetLocation.getX ());
    	
    	// Is it a spell that needs to be targetted at particular wall locations?
    	// Otherwise things like Earth to Mud or Magic Vortex can be aimed anywhere
    	if (spell.getSpellValidBorderTarget ().size () == 0)
    		result = !tile.isOffMapEdge ();
    	else
    	{
    		if ((tile.isWrecked ()) || (tile.getBorderID ().size () == 0))
    			result = false;
    		else
    		{
    			boolean found = false;
    			for (final String combatTileBorderID : spell.getSpellValidBorderTarget ())
    				if (tile.getBorderID ().contains (combatTileBorderID))
    					found = true;
    			
    			result = found;
    		}
    	}
    	
    	return result;
	}
	
	/**
	 * @return Spell utils
	 */
	public final SpellUtils getSpellUtils ()
	{
		return spellUtils;
	}

	/**
	 * @param utils Spell utils
	 */
	public final void setSpellUtils (final SpellUtils utils)
	{
		spellUtils = utils;
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
	 * @return Kind of spell utils
	 */
	public final KindOfSpellUtils getKindOfSpellUtils ()
	{
		return kindOfSpellUtils;
	}

	/**
	 * @param k Kind of spell utils
	 */
	public final void setKindOfSpellUtils (final KindOfSpellUtils k)
	{
		kindOfSpellUtils = k;
	}

	/**
	 * @return Resource value utils
	 */
	public final ResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
	}

	/**
	 * @param util Resource value utils
	 */
	public final void setResourceValueUtils (final ResourceValueUtils util)
	{
		resourceValueUtils = util;
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
	 * @return Session utils
	 */
	public final MultiplayerSessionUtils getMultiplayerSessionUtils ()
	{
		return multiplayerSessionUtils;
	}

	/**
	 * @param util Session utils
	 */
	public final void setMultiplayerSessionUtils (final MultiplayerSessionUtils util)
	{
		multiplayerSessionUtils = util;
	}

	/**
	 * @return Player pick utils
	 */
	public final PlayerPickUtils getPlayerPickUtils ()
	{
		return playerPickUtils;
	}

	/**
	 * @param utils Player pick utils
	 */
	public final void setPlayerPickUtils (final PlayerPickUtils utils)
	{
		playerPickUtils = utils;
	}
}