package momime.common.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DamageTypeID;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.SpellHasCityEffect;
import momime.common.database.UnitAttributeComponent;
import momime.common.database.UnitAttributePositiveNegative;
import momime.common.database.UnitSpellEffect;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.UnitStatusID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * Methods for working with list of MemoryMaintainedSpells
 */
public final class MemoryMaintainedSpellUtilsImpl implements MemoryMaintainedSpellUtils
{
	/** Class logger */
	private final Log log = LogFactory.getLog (MemoryMaintainedSpellUtilsImpl.class);
	
	/** Spell utils */
	private SpellUtils spellUtils;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Unit skill utils */
	private UnitSkillUtils unitSkillUtils;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
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
		log.trace ("Entering findMaintainedSpell: " + castingPlayerID + ", " + spellID);

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

		log.trace ("Entering findMaintainedSpell = " + match);
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
		log.trace ("Entering findSpellURN: " + spellURN);

		MemoryMaintainedSpell match = null;
		final Iterator<MemoryMaintainedSpell> iter = spells.iterator ();

		while ((match == null) && (iter.hasNext ()))
		{
			final MemoryMaintainedSpell thisSpell = iter.next ();
			if (thisSpell.getSpellURN () == spellURN)
				match = thisSpell;
		}

		log.trace ("Entering findSpellURN = " + match);
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
			log.trace ("Entering findSpellURN: " + spellURN + ", " + caller);

			final MemoryMaintainedSpell match = findSpellURN (spellURN, spells);

			if (match == null)
				throw new RecordNotFoundException (MemoryMaintainedSpell.class, spellURN, caller);
			
			log.trace ("Entering findSpellURN = " + match);
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
		log.trace ("Entering removeSpellURN: " + spellURN);

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

		log.trace ("Exiting removeSpellURN");
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
    	log.trace ("Entering removeSpellsCastOnUnitStack: " + unitURNs);

    	int numberRemoved = 0;

		final Iterator<MemoryMaintainedSpell> iter = spells.iterator ();
    	while (iter.hasNext ())
    	{
    		final Integer thisUnitURN = iter.next ().getUnitURN ();

    		if ((thisUnitURN != null) && (unitURNs.contains (thisUnitURN)))
    		{
    			iter.remove ();
    			numberRemoved++;
    		}
    	}

    	log.trace ("Exiting removeSpellsCastOnUnitStack = " + numberRemoved);
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
	public final List<String> listUnitSpellEffectsNotYetCastOnUnit (final List<MemoryMaintainedSpell> spells, final Spell spell,
		final int castingPlayerID, final int unitURN)
	{
    	log.trace ("Entering listUnitSpellEffectsNotYetCastOnUnit: " + spell.getSpellID () + ", Unit URN " + unitURN);
    	
    	final List<String> unitSpellEffectIDs;
    	
    	if (spell.getUnitSpellEffect ().size () == 0)
    		unitSpellEffectIDs = null;
    	else
    	{
    		unitSpellEffectIDs = new ArrayList<String> ();
    		for (final UnitSpellEffect effect : spell.getUnitSpellEffect ())
    			if (findMaintainedSpell (spells, castingPlayerID, spell.getSpellID (), unitURN, effect.getUnitSkillID (), null, null) == null)
    				unitSpellEffectIDs.add (effect.getUnitSkillID ());
    	}

    	log.trace ("Exiting listUnitSpellEffectsNotYetCastOnUnit = " + unitSpellEffectIDs);
    	return unitSpellEffectIDs;
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
    	log.trace ("Entering listCitySpellEffectsNotYetCastAtLocation: " + spell.getSpellID () + ", " + cityLocation);
    	
    	final List<String> citySpellEffectIDs;
    	
    	if (spell.getSpellHasCityEffect ().size () == 0)
    		citySpellEffectIDs = null;
    	else
    	{
    		citySpellEffectIDs = new ArrayList<String> ();
    		for (final SpellHasCityEffect effect : spell.getSpellHasCityEffect ())
    			if (findMaintainedSpell (spells, castingPlayerID, spell.getSpellID (), null, null, cityLocation, effect.getCitySpellEffectID ()) == null)
   					citySpellEffectIDs.add (effect.getCitySpellEffectID ());
    	}

    	log.trace ("Exiting listCitySpellEffectsNotYetCastAtLocation = " + citySpellEffectIDs);
    	return citySpellEffectIDs;
	}

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
	 * @param players Players list
	 * @param spells Known spells
	 * @param combatAreaEffects Known combat area effects
	 * @param db Lookup lists built over the XML database
	 * @return VALID_TARGET, or an enum value indicating why it isn't a valid target
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 */
	@Override
	public final TargetSpellResult isUnitValidTargetForSpell (final Spell spell, final MapCoordinates3DEx combatLocation,
		final int castingPlayerID, final Integer variableDamage, final MemoryUnit unit, final List<? extends PlayerPublicDetails> players,
		final List<MemoryMaintainedSpell> spells, final List<MemoryCombatAreaEffect> combatAreaEffects, final CommonDatabase db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException
	{
    	log.trace ("Entering isUnitValidTargetForSpell: " + spell.getSpellID () + ", Player ID " + castingPlayerID + ", " + combatLocation);
    	
    	final TargetSpellResult result;
    	
    	// Do easy checks first
    	if ((combatLocation != null) && ((!combatLocation.equals (unit.getCombatLocation ())) ||
    		(unit.getCombatPosition () == null) || (unit.getCombatSide () == null) || (unit.getCombatHeading () == null)))
    		
    		result = TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT;
    	
    	else if (unit.getStatus () != UnitStatusID.ALIVE)
    		result =TargetSpellResult.UNIT_DEAD;
    	
    	else if ((spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_ENCHANTMENTS) && (unit.getOwningPlayerID () != castingPlayerID))
    		result = TargetSpellResult.ENCHANTING_ENEMY; 

    	else if (((spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_CURSES) || (spell.getSpellBookSectionID () == SpellBookSectionID.ATTACK_SPELLS)) &&
    				(unit.getOwningPlayerID () == castingPlayerID))
    				result = TargetSpellResult.CURSING_OR_ATTACKING_OWN;
    	
    	else
    	{
    		// Now check unitSpellEffectIDs
    		final boolean unitSpellEffectRequired = (spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_ENCHANTMENTS) ||
    			(spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_CURSES);
    		
    		final List<String> unitSpellEffectIDs = listUnitSpellEffectsNotYetCastOnUnit (spells, spell, castingPlayerID, unit.getUnitURN ());
    		if ((unitSpellEffectRequired) && (unitSpellEffectIDs == null))
    			result = TargetSpellResult.NO_SPELL_EFFECT_IDS_DEFINED;
    		
    		else if ((unitSpellEffectRequired) && (unitSpellEffectIDs.size () == 0))
    			result = TargetSpellResult.ALREADY_HAS_ALL_POSSIBLE_SPELL_EFFECTS;
    		
    		else if (!getSpellUtils ().spellCanTargetMagicRealmLifeformType (spell,
    			getUnitUtils ().getModifiedUnitMagicRealmLifeformTypeID (unit, unit.getUnitHasSkill (), spells, db)))
    			
    			result = TargetSpellResult.UNIT_INVALID_MAGIC_REALM_LIFEFORM_TYPE;
    		
    		else if ((spell.getSpellBookSectionID () != SpellBookSectionID.ATTACK_SPELLS) || (combatLocation == null))
    			result = TargetSpellResult.VALID_TARGET;
    		
    		else
    			// Combat attack spell
    			switch (spell.getAttackSpellDamageType ())
    			{
    				case EACH_FIGURE_RESIST_OR_DIE:
    				case SINGLE_FIGURE_RESIST_OR_DIE:
    				case RESIST_OR_TAKE_DAMAGE:
    				case RESISTANCE_ROLLS:
    				case DISINTEGRATE:
    					// Units with 10 or more resistance are immune to spells that roll against resistance
    					// First need to take into account if there's a saving throw modifier, NB. Resistance rolls damage allows no saving throw modifier
    					int resistance = getUnitSkillUtils ().getModifiedAttributeValue (unit, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE,
       						UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db);
    					if (spell.getAttackSpellDamageType () != DamageTypeID.RESISTANCE_ROLLS)
    					{
    						final Integer savingThrowModifier = (spell.getCombatMaxDamage () == null) ? spell.getCombatBaseDamage () : variableDamage;
    						if (savingThrowModifier != null)
    							resistance = resistance - savingThrowModifier;
    					}
    						
    					if (resistance >= 10)
    						result = TargetSpellResult.TOO_HIGH_RESISTANCE;
    					else
    						result = TargetSpellResult.VALID_TARGET;
    					break;
    				
    				default:
    					// Combat attack spell that rolls against something other than resistance, so always a valid target
    	    			result = TargetSpellResult.VALID_TARGET;
    			}
    	}

    	log.trace ("Exiting isUnitValidTargetForSpell = " + result);
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
	 * @param buildingsList Known buildings
	 * @param db Lookup lists built over the XML database
	 * @return VALID_TARGET, or an enum value indicating why it isn't a valid target
	 * @throws RecordNotFoundException If the unit has a skill that we can't find in the cache
	 */
	@Override
	public final TargetSpellResult isCityValidTargetForSpell (final List<MemoryMaintainedSpell> spells, final Spell spell, final int castingPlayerID,
		final MapCoordinates3DEx cityLocation, final MapVolumeOfMemoryGridCells map, final List<MemoryBuilding> buildingsList,
		final CommonDatabase db) throws RecordNotFoundException
	{
    	log.trace ("Entering isCityValidTargetForSpell: " + spell.getSpellID () + ", Player ID " + castingPlayerID);
    	
    	final TargetSpellResult result;
    	
    	final OverlandMapCityData cityData = map.getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();
    	
    	// Do easy checks first
    	if (cityData == null)
    		result = TargetSpellResult.NO_CITY_HERE;
    	
    	else if ((spell.getSpellBookSectionID () == SpellBookSectionID.CITY_ENCHANTMENTS) && (cityData.getCityOwnerID () != castingPlayerID))
    		result = TargetSpellResult.ENCHANTING_ENEMY; 

    	else if ((spell.getSpellBookSectionID () == SpellBookSectionID.CITY_CURSES) && (cityData.getCityOwnerID () == castingPlayerID))
    		result = TargetSpellResult.CURSING_OR_ATTACKING_OWN;
    	
    	// Is it a spell that creates a building?
    	else if (spell.getBuildingID () != null)
    	{
    		if (getMemoryBuildingUtils ().findBuilding (buildingsList, cityLocation, spell.getBuildingID ()) != null)
    			result = TargetSpellResult.CITY_ALREADY_HAS_BUILDING;
    		else
    			result = TargetSpellResult.VALID_TARGET;
    	}
    	else
    	{
    		// Now check citySpellEffectIDs
    		final List<String> citySpellEffectIDs = listCitySpellEffectsNotYetCastAtLocation (spells, spell, castingPlayerID, cityLocation);
    		if (citySpellEffectIDs == null)
    			result = TargetSpellResult.NO_SPELL_EFFECT_IDS_DEFINED;
    		
    		else if (citySpellEffectIDs.size () == 0)
    			result = TargetSpellResult.ALREADY_HAS_ALL_POSSIBLE_SPELL_EFFECTS;
    		
    		else    			
    			result = TargetSpellResult.VALID_TARGET;
    	}

    	log.trace ("Exiting isCityValidTargetForSpell = " + result);
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
	 * @return Unit skill utils
	 */
	public final UnitSkillUtils getUnitSkillUtils ()
	{
		return unitSkillUtils;
	}

	/**
	 * @param utils Unit skill utils
	 */
	public final void setUnitSkillUtils (final UnitSkillUtils utils)
	{
		unitSkillUtils = utils;
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
}