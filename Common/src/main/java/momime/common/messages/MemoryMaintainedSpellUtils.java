package momime.common.messages;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;

/**
 * Methods for working with list of MemoryMaintainedSpells
 */
public final class MemoryMaintainedSpellUtils
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
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return First matching spell found, or null if none matched
	 */
	public static final MemoryMaintainedSpell findMaintainedSpell (final List<MemoryMaintainedSpell> spells,
		final Integer castingPlayerID, final String spellID, final Integer unitURN, final String unitSkillID,
		final OverlandMapCoordinates cityLocation, final String citySpellEffectID, final Logger debugLogger)
	{
		debugLogger.entering (MemoryMaintainedSpellUtils.class.getName (), "findMaintainedSpell", new String []
			{(castingPlayerID == null) ? "null" : castingPlayerID.toString (), spellID,
			(unitURN == null) ? "null" : unitURN.toString (), unitSkillID,
			CoordinatesUtils.overlandMapCoordinatesToString (cityLocation), citySpellEffectID});

		MemoryMaintainedSpell match = null;
		final Iterator<MemoryMaintainedSpell> iter = spells.iterator ();

		while ((match == null) && (iter.hasNext ()))
		{
			final MemoryMaintainedSpell thisSpell = iter.next ();

			if (((castingPlayerID == null) || (castingPlayerID == thisSpell.getCastingPlayerID ())) &&
				((spellID == null) || (spellID.equals (thisSpell.getSpellID ()))) &&
				(((unitURN == null) && (thisSpell.getUnitURN () == null)) || ((unitURN != null) && (unitURN.equals (thisSpell.getUnitURN ())))) &&
				((unitSkillID == null) || (unitSkillID.equals (thisSpell.getUnitSkillID ()))) &&
				(((cityLocation == null) && (thisSpell.getCityLocation () == null)) || ((cityLocation != null) && (CoordinatesUtils.overlandMapCoordinatesEqual (cityLocation, thisSpell.getCityLocation ())))) &&
				((citySpellEffectID == null) || (citySpellEffectID.equals (thisSpell.getCitySpellEffectID ()))))

				match = thisSpell;
		}

		debugLogger.exiting (MemoryMaintainedSpellUtils.class.getName (), "findMaintainedSpell", match);
		return match;
	}

	/**
	 * Removes all spells from the list that are cast on any of the units listed
	 *
	 * @param spells List of spells to search through
	 * @param unitURNs List of units to remove spells from
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 */
	public static final void removeSpellsCastOnUnitStack (final List<MemoryMaintainedSpell> spells, final List<Integer> unitURNs, final Logger debugLogger)
	{
    	debugLogger.entering (MemoryMaintainedSpellUtils.class.getName (), "removeSpellsCastOnUnitStack", unitURNs);

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

    	debugLogger.exiting (MemoryMaintainedSpellUtils.class.getName (), "removeSpellsCastOnUnitStack", numberRemoved);
	}

	/**
	 * Prevent instantiation
	 */
	private MemoryMaintainedSpellUtils ()
	{
	}
}
