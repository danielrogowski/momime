package momime.client.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;

import org.junit.Test;

/**
 * Tests the SpellSorter class
 */
public final class TestSpellSorter
{
	/**
	 * Tests the spellSortValue method
	 */
	@Test
	public final void testSpellSortValue ()
	{
		// Set up some dummy spells
		final Spell spell1 = new Spell ();
		
		final Spell spell2 = new Spell ();
		spell2.setResearchCost (70);
		spell2.setOverlandCastingCost (80);

		final Spell spell3 = new Spell ();
		spell3.setCombatCastingCost (15);

		final Spell spell4 = new Spell ();
		spell4.setOverlandCastingCost (100);
		spell4.setCombatCastingCost (18);
		
		// Test spells in the research section
		final SpellSorter researchSorter = new SpellSorter (SpellBookSectionID.RESEARCHABLE_NOW);
		assertEquals (0, researchSorter.spellSortValue (spell1));
		assertEquals (70, researchSorter.spellSortValue (spell2));

		// Test spells in the unknown section
		final SpellSorter unknownSorter = new SpellSorter (SpellBookSectionID.RESEARCHABLE);
		assertEquals (0, unknownSorter.spellSortValue (spell1));
		assertEquals (70, unknownSorter.spellSortValue (spell2));
		
		// Test spells in some regular casting section
		final SpellSorter castingSorter = new SpellSorter (SpellBookSectionID.OVERLAND_ENCHANTMENTS);
		assertEquals (0, castingSorter.spellSortValue (spell1));
		assertEquals (80, castingSorter.spellSortValue (spell2));
		assertEquals (75, castingSorter.spellSortValue (spell3));
		assertEquals (100, castingSorter.spellSortValue (spell4));
	}
	
	/**
	 * Tests the compare method
	 */
	@Test
	public final void testCompare ()
	{
		// Set up some dummy spells
		final Spell spell1 = new Spell ();
		spell1.setSpellID ("SP001");
		spell1.setResearchCost (50);
		
		final Spell spell2 = new Spell ();
		spell2.setSpellID ("SP002");
		spell2.setResearchCost (80);
		
		final Spell spell3 = new Spell ();
		spell3.setSpellID ("SP003");
		spell3.setResearchCost (70);
		
		final Spell spell4 = new Spell ();
		spell4.setSpellID ("SP004");
		spell4.setResearchCost (80);
		
		final Spell spell5 = new Spell ();
		spell5.setSpellID ("SP005");
		spell5.setResearchCost (25);
		
		final Spell spell6 = new Spell ();
		spell6.setSpellID ("SP006");
		spell6.setResearchCost (100);
		
		final List<Spell> spells = new ArrayList<Spell> ();
		spells.add (spell1);
		spells.add (spell2);
		spells.add (spell3);
		spells.add (spell4);
		spells.add (spell5);
		spells.add (spell6);
		
		// Deriving the right values is tested above, so no need to complicate that here, just test some fixed research values
		final SpellSorter researchSorter = new SpellSorter (SpellBookSectionID.RESEARCHABLE_NOW);
		Collections.sort (spells, researchSorter);
		
		// Check results
		assertEquals (6, spells.size ());
		assertSame (spell5, spells.get (0));
		assertSame (spell1, spells.get (1));
		assertSame (spell3, spells.get (2));
		assertSame (spell2, spells.get (3));
		assertSame (spell4, spells.get (4));
		assertSame (spell6, spells.get (5));
	}		
}