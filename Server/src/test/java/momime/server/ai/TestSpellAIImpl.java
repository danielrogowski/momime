package momime.server.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import momime.common.MomException;
import momime.common.messages.v0_9_4.SpellResearchStatus;
import momime.common.messages.v0_9_4.SpellResearchStatusID;
import momime.common.utils.SpellUtilsImpl;
import momime.server.ServerTestData;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_4.Spell;

import org.junit.Test;

/**
 * Tests the SpellAI class
 */
public final class TestSpellAIImpl
{
	/**
	 * Tests the chooseSpellToResearchAI method with a valid spell list
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testChooseSpellToResearchAI_Valid () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final SpellAIImpl ai = new SpellAIImpl ();

		// List the 2nd 10 earth spells, two of these have research order 1, so the routine should pick either of them
		final List<Spell> spells = new ArrayList<Spell> ();
		for (int n = 10; n < 20; n++)
			spells.add (db.getSpell ().get (n));

		final Spell spell = ai.chooseSpellToResearchAI (spells, "AI Player");
		assertTrue ("Chosen spell was " + spell.getSpellID (), (spell.getSpellID ().equals ("SP013")) || (spell.getSpellID ().equals ("SP020")));
	}

	/**
	 * Tests the chooseSpellToResearchAI method with an empty spell list
	 * @throws MomException If the list was empty
	 */
	@Test(expected=MomException.class)
	public final void testChooseSpellToResearchAI_EmptyList () throws MomException
	{
		final SpellAIImpl ai = new SpellAIImpl ();
		ai.chooseSpellToResearchAI (new ArrayList<Spell> (), "AI Player");
	}

	/**
	 * Tests the chooseFreeSpellAI method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testChooseFreeSpellAI () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		
		// Set up test object
		final SpellAIImpl ai = new SpellAIImpl ();
		ai.setSpellUtils (new SpellUtilsImpl ());

		// Player knows no spells yet
		final List<SpellResearchStatus> spells = new ArrayList<SpellResearchStatus> ();
		for (final Spell spell : db.getSpell ())
		{
			final SpellResearchStatus researchStatus = new SpellResearchStatus ();
			researchStatus.setSpellID (spell.getSpellID ());
			researchStatus.setStatus (SpellResearchStatusID.UNAVAILABLE);
			spells.add (researchStatus);
		}

		// Same magic realm/spell rank at the 10 from the previous test
		final SpellResearchStatus spell = ai.chooseFreeSpellAI (spells, "MB04", "SR02", "AI Player", db);
		assertTrue ("Chosen spell was " + spell.getSpellID (), (spell.getSpellID ().equals ("SP013")) || (spell.getSpellID ().equals ("SP020")));

		// If we give the player one of the spells, should always pick the other one
		spells.get (12).setStatus (SpellResearchStatusID.AVAILABLE);
		assertEquals ("SP020", ai.chooseFreeSpellAI (spells, "MB04", "SR02", "AI Player", db).getSpellID ());
	}
}
