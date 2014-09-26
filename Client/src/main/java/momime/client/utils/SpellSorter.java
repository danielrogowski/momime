package momime.client.utils;

import java.util.Comparator;

import momime.common.database.v0_9_5.Spell;
import momime.common.database.v0_9_5.SpellBookSectionID;

/**
 * Sorts spells in the order they should appear in the spell book.
 * All the spells in one list will all be from the same section, so we don't need to worry about comparing sectionIDs.
 */
public final class SpellSorter implements Comparator<Spell>
{
	/** The section being sorted */
	private final SpellBookSectionID sectionID;

	/**
	 * @param aSectionID The section being sorted
	 */
	public SpellSorter (final SpellBookSectionID aSectionID)
	{
		sectionID = aSectionID;
	}
	
	/**
	 * @param s1 First spell to compare
	 * @param s2 Second spell to compare
	 * @return Value suitable to sort spells into order
	 */
	@Override
	public final int compare (final Spell s1, final Spell s2)
	{
		// Get the base values for both spells
		final int value1 = spellSortValue (s1);
		final int value2 = spellSortValue (s2);
		
		// If they're different, good enough, if they're the same, sort by spellID
		final int result;
		if (value1 != value2)
			result = value1 - value2;
		else
			result = s1.getSpellID ().compareTo (s2.getSpellID ());
		
		return result;
	}
	
	/**
	 * @param spell Spell to compare
	 * @return Sort value for this spell 
	 */
	final int spellSortValue (final Spell spell)
	{
		// If we know the spell then sort it by casting cost; if we don't know it then sort it by research cost.
		final int value;
		if ((sectionID == SpellBookSectionID.RESEARCHABLE_NOW) || (sectionID == SpellBookSectionID.RESEARCHABLE))
			
			value = (spell.getResearchCost () == null) ? 0 : spell.getResearchCost ();
		else
		{
			// If we have an overland casting cost then use it; for combat-only spells, sort them assuming their overland casting cost would be 5x combat casting cost
			if (spell.getOverlandCastingCost () != null)
				value = spell.getOverlandCastingCost ();
			
			else if (spell.getCombatCastingCost () != null)
				value = 5 * spell.getCombatCastingCost ();
			
			else
				value = 0;
		}
		
		return value;
	}
}