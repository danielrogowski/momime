package momime.server.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_4.SpellResearchStatus;
import momime.common.messages.v0_9_4.SpellResearchStatusID;
import momime.server.ServerTestData;
import momime.server.database.JAXBContextCreator;
import momime.server.database.ServerDatabaseLookup;
import momime.server.database.v0_9_4.ServerDatabase;
import momime.server.database.v0_9_4.Spell;

import org.junit.Test;

/**
 * Tests the SpellAI class
 */
public final class TestSpellAI
{
	/** Dummy logger to use during unit tests */
	private final Logger debugLogger = Logger.getLogger ("MoMIMEServerUnitTests");

	/**
	 * Tests the chooseSpellToResearchAI method with a valid spell list
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws MomException If the list was empty
	 */
	@Test
	public final void testChooseSpellToResearchAI_Valid () throws IOException, JAXBException, MomException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());

		// List the 2nd 10 earth spells, two of these have research order 1, so the routine should pick either of them
		final List<Spell> spells = new ArrayList<Spell> ();
		for (int n = 10; n < 20; n++)
			spells.add (serverDB.getSpell ().get (n));

		final Spell spell = SpellAI.chooseSpellToResearchAI (spells, "AI Player", debugLogger);
		assertTrue ("Chosen spell was " + spell.getSpellID (), (spell.getSpellID ().equals ("SP013")) || (spell.getSpellID ().equals ("SP020")));
	}

	/**
	 * Tests the chooseSpellToResearchAI method with an empty spell list
	 * @throws MomException If the list was empty
	 */
	@Test(expected=MomException.class)
	public final void testChooseSpellToResearchAI_EmptyList () throws MomException
	{
		SpellAI.chooseSpellToResearchAI (new ArrayList<Spell> (), "AI Player", debugLogger);
	}

	/**
	 * Tests the chooseFreeSpellAI method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws MomException If no eligible spells are available (e.g. player has them all researched already)
	 * @throws RecordNotFoundException If the spell chosen couldn't be found in the player's spell list
	 */
	@Test
	public final void testChooseFreeSpellAI () throws IOException, JAXBException, MomException, RecordNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		// Player knows no spells yet
		final List<SpellResearchStatus> spells = new ArrayList<SpellResearchStatus> ();
		for (final Spell spell : serverDB.getSpell ())
		{
			final SpellResearchStatus researchStatus = new SpellResearchStatus ();
			researchStatus.setSpellID (spell.getSpellID ());
			researchStatus.setStatus (SpellResearchStatusID.UNAVAILABLE);
			spells.add (researchStatus);
		}

		// Same magic realm/spell rank at the 10 from the previous test
		final SpellResearchStatus spell = SpellAI.chooseFreeSpellAI (spells, "MB04", "SR02", "AI Player", db, debugLogger);
		assertTrue ("Chosen spell was " + spell.getSpellID (), (spell.getSpellID ().equals ("SP013")) || (spell.getSpellID ().equals ("SP020")));

		// If we give the player one of the spells, should always pick the other one
		spells.get (12).setStatus (SpellResearchStatusID.AVAILABLE);
		assertEquals ("SP020", SpellAI.chooseFreeSpellAI (spells, "MB04", "SR02", "AI Player", db, debugLogger).getSpellID ());
	}
}
