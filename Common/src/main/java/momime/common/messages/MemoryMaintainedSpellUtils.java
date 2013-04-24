package momime.common.messages;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.common.utils.CompareUtils;

/**
 * Methods for working with list of MemoryMaintainedSpells
 */
public final class MemoryMaintainedSpellUtils implements IMemoryMaintainedSpellUtils
{
	/** Class logger */
	private final Logger log = Logger.getLogger (MemoryMaintainedSpellUtils.class.getName ());
	
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
		final OverlandMapCoordinates cityLocation, final String citySpellEffectID)
	{
		log.entering (MemoryMaintainedSpellUtils.class.getName (), "findMaintainedSpell", new String []
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
				(CompareUtils.safeIntegerCompare (unitURN,  thisSpell.getUnitURN ())) &&
				((unitSkillID == null) || (unitSkillID.equals (thisSpell.getUnitSkillID ()))) &&
				(CoordinatesUtils.overlandMapCoordinatesEqual (cityLocation, thisSpell.getCityLocation (), true)) &&
				((citySpellEffectID == null) || (citySpellEffectID.equals (thisSpell.getCitySpellEffectID ()))))

				match = thisSpell;
		}

		log.exiting (MemoryMaintainedSpellUtils.class.getName (), "findMaintainedSpell", match);
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
		final OverlandMapCoordinates cityLocation, final String citySpellEffectID)
		throws RecordNotFoundException
	{
		log.entering (MemoryMaintainedSpellUtils.class.getName (), "switchOffMaintainedSpell", new String []
			{new Integer (castingPlayerID).toString (), spellID,
			(unitURN == null) ? "null" : unitURN.toString (), unitSkillID,
			CoordinatesUtils.overlandMapCoordinatesToString (cityLocation), citySpellEffectID});

		boolean found = false;
		final Iterator<MemoryMaintainedSpell> iter = spells.iterator ();
		while ((!found) && (iter.hasNext ()))
		{
			final MemoryMaintainedSpell thisSpell = iter.next ();

			if ((castingPlayerID == thisSpell.getCastingPlayerID ()) &&
				(spellID.equals (thisSpell.getSpellID ())) &&
				(CompareUtils.safeIntegerCompare (unitURN,  thisSpell.getUnitURN ())) &&
				((unitSkillID == null) || (unitSkillID.equals (thisSpell.getUnitSkillID ()))) &&
				(CoordinatesUtils.overlandMapCoordinatesEqual (cityLocation, thisSpell.getCityLocation (), true)) &&
				((citySpellEffectID == null) || (citySpellEffectID.equals (thisSpell.getCitySpellEffectID ()))))
			{
				iter.remove ();
				found = true;
			}
		}

		if (!found)
			throw new RecordNotFoundException (MemoryMaintainedSpell.class.getName (), spellID + " - " + castingPlayerID, "switchOffMaintainedSpell");

		log.exiting (MemoryMaintainedSpellUtils.class.getName (), "switchOffMaintainedSpell");
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
    	log.entering (MemoryMaintainedSpellUtils.class.getName (), "removeSpellsCastOnUnitStack", unitURNs);

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

    	log.exiting (MemoryMaintainedSpellUtils.class.getName (), "removeSpellsCastOnUnitStack", numberRemoved);
	}
}
