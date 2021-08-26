package momime.common.utils;

import momime.common.MomException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;

/**
 * Working out the kind of spell from the XML is a bunch of ad hoc rules for each spell book section, so keep them all in one
 * place so we don't have to track down everywhere that needs updating every time these rules change.
 */
public interface KindOfSpellUtils
{
	/**
	 * @param spell Spell definition
	 * @param overrideSpellBookSection Usually null; filled in when a spell is of one type, but has a specially coded secondary effect of another type
	 *		For example Wall of Fire is a city enchantment for placing it, but then when we roll for damage we have to treat it like an attack spell 
	 * @return Which kind of spell it is
	 * @throws MomException If we encounter a spell book section we don't know how to handle
	 */
	public KindOfSpell determineKindOfSpell (final Spell spell, final SpellBookSectionID overrideSpellBookSection)
		throws MomException;
}