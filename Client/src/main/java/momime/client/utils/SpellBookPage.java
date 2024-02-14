package momime.client.utils;

import java.util.ArrayList;
import java.util.List;

import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;

/**
 * Represents one page of the spell book in the UI and the up-to-4 spells on it
 */
public final class SpellBookPage
{
	/** The spell book section */
	private SpellBookSectionID sectionID;
	
	/** Is this the first page of this section?  i.e. show the title or not */
	private boolean firstPageOfSection;
	
	/** The spells on this page */
	private List<Spell> spells = new ArrayList<Spell> ();

	/**
	 * @return The spell book section
	 */
	public final SpellBookSectionID getSectionID ()
	{
		return sectionID;
	}

	/**
	 * @param section The spell book section
	 */
	public final void setSectionID (final SpellBookSectionID section)
	{
		sectionID = section;
	}
	
	/**
	 * @return Is this the first page of this section?  i.e. show the title or not
	 */
	public final boolean isFirstPageOfSection ()
	{
		return firstPageOfSection;
	}

	/**
	 * @param first Is this the first page of this section?  i.e. show the title or not
	 */
	public final void setFirstPageOfSection (final boolean first)
	{
		firstPageOfSection = first;
	}
	
	/**
	 * @return The spells on this page
	 */
	public final List<Spell> getSpells ()
	{
		return spells;
	}
}