package momime.common.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.database.CitySpellEffect;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.UnitSpellEffect;
import momime.common.messages.MemoryMaintainedSpell;

/**
 * Methods for working with list of MemoryMaintainedSpells
 */
public final class MemoryMaintainedSpellUtilsImpl implements MemoryMaintainedSpellUtils
{
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
	 * @param cityLocation City to cast the spell on
	 * @param pickID Magic realm of spell being cast on the city
	 * @param castingPlayerID Player casting the spell
	 * @param spells List of known existing spells
	 * @param db Lookup lists built over the XML database
	 * @return Whether the city is protected against this magic realm
	 * @throws RecordNotFoundException If we can't find one of the city spell effects in the database
	 */
	@Override
	public final boolean isCityProtectedAgainstSpellRealm (final MapCoordinates3DEx cityLocation, final String pickID, final int castingPlayerID,
		final List<MemoryMaintainedSpell> spells, final CommonDatabase db) throws RecordNotFoundException
	{
		boolean prot = false;
		
		final Iterator<MemoryMaintainedSpell> iter = spells.iterator ();
		while ((!prot) && (iter.hasNext ()))
		{
			final MemoryMaintainedSpell thisSpell = iter.next ();
			
			// If we own the protection spell then allow us to still cast spells that are protected against.
			// No reason to block casting beneficial spells like Wall of Fire.
			if ((cityLocation.equals (thisSpell.getCityLocation ())) && (thisSpell.getCitySpellEffectID () != null) && (thisSpell.getCastingPlayerID () != castingPlayerID))
			{
				final CitySpellEffect effect = db.findCitySpellEffect (thisSpell.getCitySpellEffectID (), "isCityProtectedAgainstSpellRealm");
				if (effect.getProtectsAgainstSpellRealm ().contains (pickID))
					prot = true;
			}
		}
		
		return prot;
	}
	
	/**
	 * @param spells Known spells
	 * @param castingPlayerID Player casting the spell
	 * @param combatLocation Location we want to cast the spell at 
	 * @param pickID Magic realm of the spell we want to cast
	 * @param db Lookup lists built over the XML database
	 * @return True if there is a Spell Ward here that blocks casting combat spells of this magic realm
	 * @throws RecordNotFoundException If we can't find one of the city spell effects
	 */
	@Override
	public final boolean isBlockedCastingCombatSpellsOfRealm (final List<MemoryMaintainedSpell> spells, final int castingPlayerID,
		final MapCoordinates3DEx combatLocation, final String pickID, final CommonDatabase db) throws RecordNotFoundException
	{
		boolean found = false;
		
		if (pickID != null)
		{
			final Iterator<MemoryMaintainedSpell> iter = spells.iterator ();
			while ((!found) && (iter.hasNext ()))
			{
				final MemoryMaintainedSpell thisSpell = iter.next ();
				if ((thisSpell.getCastingPlayerID () != castingPlayerID) && (combatLocation.equals (thisSpell.getCityLocation ())) && (thisSpell.getCitySpellEffectID () != null))
				{
					final CitySpellEffect effect = db.findCitySpellEffect (thisSpell.getCitySpellEffectID (), "isBlockedCastingCombatSpellsOfRealm");
					if ((effect.isBlockCastingCombatSpellsOfRealm () != null) && (effect.isBlockCastingCombatSpellsOfRealm ()) &&
						(effect.getProtectsAgainstSpellRealm ().contains (pickID)))
						
						found = true;
				}
			}
		}
		
		return found;
	}

	/**
	 * @param spells Known spells
	 * @param castingPlayerID Player casting the spell
	 * @param combatLocation Location we want to cast the spell at 
	 * @param db Lookup lists built over the XML database
	 * @return List of magic realms that we are not allowed to cast combat spells for
	 * @throws RecordNotFoundException If we can't find one of the city spell effects
	 */
	@Override
	public final Set<String> listMagicRealmsBlockedAsCombatSpells (final List<MemoryMaintainedSpell> spells, final int castingPlayerID,
		final MapCoordinates3DEx combatLocation, final CommonDatabase db) throws RecordNotFoundException
	{
		final Set<String> blocked = new HashSet<String> ();
		
		for (final MemoryMaintainedSpell thisSpell : spells)
			if ((thisSpell.getCastingPlayerID () != castingPlayerID) && (combatLocation.equals (thisSpell.getCityLocation ())) && (thisSpell.getCitySpellEffectID () != null))
			{
				final CitySpellEffect effect = db.findCitySpellEffect (thisSpell.getCitySpellEffectID (), "listMagicRealmsBlockedAsCombatSpells");
				if ((effect.isBlockCastingCombatSpellsOfRealm () != null) && (effect.isBlockCastingCombatSpellsOfRealm ()))
					blocked.addAll (effect.getProtectsAgainstSpellRealm ());
			}
	
		return blocked;
	}
}