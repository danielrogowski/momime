package momime.common.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_5.Spell;
import momime.common.database.v0_9_5.SpellHasCityEffect;
import momime.common.database.v0_9_5.UnitSpellEffect;
import momime.common.messages.v0_9_5.MemoryMaintainedSpell;
import momime.common.messages.v0_9_5.MemoryUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;

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
		log.trace ("Entering findMaintainedSpell: Player ID " + castingPlayerID + ", " + spellID + ", Unit URN " + unitURN + ", " + unitSkillID + ", " + cityLocation + ", " + citySpellEffectID);

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

		log.trace ("Exiting findMaintainedSpell = " + match);
		return match;
	}

	/**
	 * Removes a maintained spell from the list
	 *
	 * @param spells List of spells to search through
	 * @param castingPlayerID Player who cast the spell to search for
	 * @param spellID Unique identifier for the spell to search for
	 * @param unitURN Which unit the spell is cast on
	 * @param unitSkillID Which actual unit spell effect was granted
	 * @param cityLocation Which city the spell is cast on
	 * @param citySpellEffectID Which actual city spell effect was granted
	 * @throws RecordNotFoundException If we can't find the requested spell
	 */
	@Override
	public final void switchOffMaintainedSpell (final List<MemoryMaintainedSpell> spells,
		final int castingPlayerID, final String spellID, final Integer unitURN, final String unitSkillID,
		final MapCoordinates3DEx cityLocation, final String citySpellEffectID)
		throws RecordNotFoundException
	{
		log.trace ("Entering switchOffMaintainedSpell: Player ID " + castingPlayerID + ", " + spellID + ", Unit URN " + unitURN + ", " + unitSkillID + ", " + cityLocation + ", " + citySpellEffectID);

		boolean found = false;
		final Iterator<MemoryMaintainedSpell> iter = spells.iterator ();
		while ((!found) && (iter.hasNext ()))
		{
			final MemoryMaintainedSpell thisSpell = iter.next ();

			if ((castingPlayerID == thisSpell.getCastingPlayerID ()) &&
				(spellID.equals (thisSpell.getSpellID ())) &&
				(CompareUtils.safeIntegerCompare (unitURN,  thisSpell.getUnitURN ())) &&
				((unitSkillID == null) || (unitSkillID.equals (thisSpell.getUnitSkillID ()))) &&
				(CompareUtils.safeOverlandMapCoordinatesCompare (cityLocation, (MapCoordinates3DEx) thisSpell.getCityLocation ())) &&
				((citySpellEffectID == null) || (citySpellEffectID.equals (thisSpell.getCitySpellEffectID ()))))
			{
				iter.remove ();
				found = true;
			}
		}

		if (!found)
			throw new RecordNotFoundException (MemoryMaintainedSpell.class, spellID + " - " + castingPlayerID, "switchOffMaintainedSpell");

		log.trace ("Exiting switchOffMaintainedSpell");
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
	 * @param spells List of known existing spells
	 * @param spell Spell being cast
	 * @param castingPlayerID Player casting the spell
	 * @param unit Unit to cast the spell on
	 * @param db Lookup lists built over the XML database
	 * @return VALID_TARGET, or an enum value indicating why it isn't a valid target
	 * @throws RecordNotFoundException If the unit has a skill that we can't find in the cache
	 */
	@Override
	public final TargetUnitSpellResult isUnitValidTargetForSpell (final List<MemoryMaintainedSpell> spells,
		final Spell spell, final int castingPlayerID, final MemoryUnit unit, final CommonDatabase db) throws RecordNotFoundException
	{
    	log.trace ("Entering isUnitValidTargetForSpell: " + spell.getSpellID () + ", Player ID " + castingPlayerID);
    	
    	final TargetUnitSpellResult result;
    	
    	// Do easy checks first
    	if ((spell.getSpellBookSectionID ().equals (CommonDatabaseConstants.SPELL_BOOK_SECTION_UNIT_ENCHANTMENTS) && (unit.getOwningPlayerID () != castingPlayerID)))
    		result = TargetUnitSpellResult.ENCHANTING_ENEMY_UNIT; 

    	else if ((spell.getSpellBookSectionID ().equals (CommonDatabaseConstants.SPELL_BOOK_SECTION_UNIT_CURSES) && (unit.getOwningPlayerID () == castingPlayerID)))
    		result = TargetUnitSpellResult.CURSING_OWN_UNIT;
    	
    	else
    	{
    		// Now check unitSpellEffectIDs
    		final List<String> unitSpellEffectIDs = listUnitSpellEffectsNotYetCastOnUnit (spells, spell, castingPlayerID, unit.getUnitURN ());
    		if (unitSpellEffectIDs == null)
    			result = TargetUnitSpellResult.NO_SPELL_EFFECT_IDS_DEFINED;
    		
    		else if (unitSpellEffectIDs.size () == 0)
    			result = TargetUnitSpellResult.ALREADY_HAS_ALL_POSSIBLE_SPELL_EFFECTS;
    		
    		else if (getSpellUtils ().spellCanTargetMagicRealmLifeformType (spell,
    			getUnitUtils ().getModifiedUnitMagicRealmLifeformTypeID (unit, unit.getUnitHasSkill (), spells, db)))
    			
    			result = TargetUnitSpellResult.VALID_TARGET;
    		else
    			result = TargetUnitSpellResult.INVALID_MAGIC_REALM_LIFEFORM_TYPE;
    	}

    	log.trace ("Exiting isUnitValidTargetForSpell = " + result);
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
}