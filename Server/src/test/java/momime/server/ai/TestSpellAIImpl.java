package momime.server.ai;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.common.MomException;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;
import momime.common.utils.SpellUtilsImpl;
import momime.server.ServerTestData;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.SpellSvr;

import org.junit.Test;

import com.ndg.random.RandomUtils;

/**
 * Tests the SpellAI class
 */
public final class TestSpellAIImpl extends ServerTestData
{
	/**
	 * Tests the chooseSpellToResearchAI method with a valid spell list
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testChooseSpellToResearchAI_Valid () throws Exception
	{
		final ServerDatabaseEx db = loadServerDatabase ();
		
		// Fix random result
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (2)).thenReturn (1);
		
		// Set up object to test
		final SpellAIImpl ai = new SpellAIImpl ();
		ai.setRandomUtils (random);

		// List the 2nd 10 earth spells, two of these have research order 1, so the routine should pick either of them
		final List<SpellSvr> spells = new ArrayList<SpellSvr> ();
		for (int n = 10; n < 20; n++)
			spells.add (db.getSpells ().get (n));

		assertEquals ("SP020", ai.chooseSpellToResearchAI (spells, -1).getSpellID ());
	}

	/**
	 * Tests the chooseSpellToResearchAI method with an empty spell list
	 * @throws MomException If the list was empty
	 */
	@Test(expected=MomException.class)
	public final void testChooseSpellToResearchAI_EmptyList () throws MomException
	{
		final SpellAIImpl ai = new SpellAIImpl ();
		ai.chooseSpellToResearchAI (new ArrayList<SpellSvr> (), -1);
	}

	/**
	 * Tests the chooseFreeSpellAI method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testChooseFreeSpellAI () throws Exception
	{
		final ServerDatabaseEx db = loadServerDatabase ();

		// Fix random result
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (2)).thenReturn (1);
		
		// Set up test object
		final SpellAIImpl ai = new SpellAIImpl ();
		ai.setSpellUtils (new SpellUtilsImpl ());
		ai.setRandomUtils (random);

		// Player knows no spells yet
		final List<SpellResearchStatus> spells = new ArrayList<SpellResearchStatus> ();
		for (final SpellSvr spell : db.getSpells ())
		{
			final SpellResearchStatus researchStatus = new SpellResearchStatus ();
			researchStatus.setSpellID (spell.getSpellID ());
			researchStatus.setStatus (SpellResearchStatusID.UNAVAILABLE);
			spells.add (researchStatus);
		}

		// Same magic realm/spell rank at the 10 from the previous test
		assertEquals ("SP020", ai.chooseFreeSpellAI (spells, "MB04", "SR02", -1, db).getSpellID ());

		// If we give the player one of the spells, should always pick the other one
		spells.get (19).setStatus (SpellResearchStatusID.AVAILABLE);
		assertEquals ("SP013", ai.chooseFreeSpellAI (spells, "MB04", "SR02", -1, db).getSpellID ());
	}
}