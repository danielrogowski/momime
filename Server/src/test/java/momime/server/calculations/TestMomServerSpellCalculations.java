package momime.server.calculations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_4.PlayerPick;
import momime.common.messages.v0_9_4.SpellResearchStatus;
import momime.common.messages.v0_9_4.SpellResearchStatusID;
import momime.server.ServerTestData;
import momime.server.database.JAXBContextCreator;
import momime.server.database.ServerDatabaseLookup;
import momime.server.database.v0_9_4.ServerDatabase;
import momime.server.database.v0_9_4.Spell;

import org.junit.Test;

/**
 * Tests the MomServerSpellCalculations class
 */
public final class TestMomServerSpellCalculations
{
	/** Dummy logger to use during unit tests */
	private final Logger debugLogger = Logger.getLogger ("MoMIMEServerUnitTests");

	/**
	 * Tests the randomizeResearchableSpells method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If we encounter a pick or other item that we can't find in the cache
	 */
	@Test
	public final void testRandomizeResearchableSpells () throws IOException, JAXBException, RecordNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		// Every spell is initially unavailbale
		final List<SpellResearchStatus> spells = new ArrayList<SpellResearchStatus> ();
		for (final Spell spell : serverDB.getSpell ())
		{
			final SpellResearchStatus status = new SpellResearchStatus ();
			status.setSpellID (spell.getSpellID ());
			status.setStatus (SpellResearchStatusID.UNAVAILABLE);
			spells.add (status);
		}

		// With only 1 book, we get 3 common and 1 uncommon spell researchable
		// That also means the rest of the common and uncommon spells are obtainable, but rare and very rares aren't
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		final PlayerPick natureBook = new PlayerPick ();
		natureBook.setPickID ("MB04");
		natureBook.setQuantity (1);
		picks.add (natureBook);

		MomServerSpellCalculations.randomizeResearchableSpells (spells, picks, db, debugLogger);

		// Check results
		final List<Integer> reseachableSpellsFirstPass = new ArrayList<Integer> ();
		int researchableCount = 0;
		for (int n = 0; n < 10; n++)
		{
			if (spells.get (n).getStatus ().equals (SpellResearchStatusID.RESEARCHABLE))
			{
				researchableCount++;
				reseachableSpellsFirstPass.add (n);
			}

			else if (!spells.get (n).getStatus ().equals (SpellResearchStatusID.NOT_IN_SPELL_BOOK))
				fail ("Remaining common spells should be marked as not in book");
		}
		assertEquals (3, researchableCount);

		for (int n = 10; n < 20; n++)
		{
			if (spells.get (n).getStatus ().equals (SpellResearchStatusID.RESEARCHABLE))
			{
				researchableCount++;
				reseachableSpellsFirstPass.add (n);
			}

			else if (!spells.get (n).getStatus ().equals (SpellResearchStatusID.NOT_IN_SPELL_BOOK))
				fail ("Remaining uncommon spells should be marked as not in book");
		}
		assertEquals (4, researchableCount);

		for (int n = 20; n < spells.size (); n++)
			assertEquals (SpellResearchStatusID.UNAVAILABLE, spells.get (n).getStatus ());

		// Research one of those common spells
		spells.get (reseachableSpellsFirstPass.get (0)).setStatus (SpellResearchStatusID.AVAILABLE);

		// Gain an extra book, now we should get 5 common, 2 uncommon, 1 rare
		natureBook.setQuantity (2);
		MomServerSpellCalculations.randomizeResearchableSpells (spells, picks, db, debugLogger);

		// Recheck the counts
		final List<Integer> reseachableSpellsSecondPass = new ArrayList<Integer> ();
		researchableCount = 0;
		for (int n = 0; n < 10; n++)
		{
			// Remember we made 1 available too
			if ((spells.get (n).getStatus ().equals (SpellResearchStatusID.AVAILABLE)) || (spells.get (n).getStatus ().equals (SpellResearchStatusID.RESEARCHABLE)))
			{
				researchableCount++;
				reseachableSpellsSecondPass.add (n);
			}

			else if (!spells.get (n).getStatus ().equals (SpellResearchStatusID.NOT_IN_SPELL_BOOK))
				fail ("Remaining common spells should be marked as not in book");
		}
		assertEquals (5, researchableCount);

		for (int n = 10; n < 20; n++)
		{
			if (spells.get (n).getStatus ().equals (SpellResearchStatusID.RESEARCHABLE))
			{
				researchableCount++;
				reseachableSpellsSecondPass.add (n);
			}

			else if (!spells.get (n).getStatus ().equals (SpellResearchStatusID.NOT_IN_SPELL_BOOK))
				fail ("Remaining uncommon spells should be marked as not in book");
		}
		assertEquals (7, researchableCount);

		for (int n = 20; n < 30; n++)
		{
			if (spells.get (n).getStatus ().equals (SpellResearchStatusID.RESEARCHABLE))
			{
				researchableCount++;
				reseachableSpellsSecondPass.add (n);
			}

			else if (!spells.get (n).getStatus ().equals (SpellResearchStatusID.NOT_IN_SPELL_BOOK))
				fail ("Remaining rare spells should be marked as not in book");
		}
		assertEquals (8, researchableCount);

		for (int n = 30; n < spells.size (); n++)
			assertEquals (SpellResearchStatusID.UNAVAILABLE, spells.get (n).getStatus ());

		// Check that all the spells that were available the first time were still available the second time,
		// i.e. that we didn't re-randomize the list totally
		for (final Integer spellIndex : reseachableSpellsFirstPass)
			if (!reseachableSpellsSecondPass.contains (spellIndex))
				fail ("Expect spell " + spellIndex + " to still be in second list");
	}

	/**
	 * Tests the randomizeSpellsResearchableNow method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Test
	public final void testRandomizeSpellsResearchableNow () throws IOException, JAXBException, RecordNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		// Every spell is initially unavailbale
		final List<SpellResearchStatus> spells = new ArrayList<SpellResearchStatus> ();
		for (final Spell spell : serverDB.getSpell ())
		{
			final SpellResearchStatus status = new SpellResearchStatus ();
			status.setSpellID (spell.getSpellID ());
			status.setStatus (SpellResearchStatusID.UNAVAILABLE);
			spells.add (status);
		}

		// The key part of this is that it will exhaust lower spell ranks first before moving onto higher ones
		// So lets set 2 spells to already be "researchable", 4 commons and 4 uncommons
		for (int n = 0; n < 2; n++)
			spells.get (n).setStatus (SpellResearchStatusID.RESEARCHABLE_NOW);

		for (int n = 6; n < 14; n++)
			spells.get (n).setStatus (SpellResearchStatusID.RESEARCHABLE);

		MomServerSpellCalculations.randomizeSpellsResearchableNow (spells, db, debugLogger);

		// So should now have our original 2 spells, all 4 commons and 2 out of 4 of the uncommons "researchable now"
		assertEquals (SpellResearchStatusID.RESEARCHABLE_NOW, spells.get (0).getStatus ());
		assertEquals (SpellResearchStatusID.RESEARCHABLE_NOW, spells.get (1).getStatus ());
		assertEquals (SpellResearchStatusID.UNAVAILABLE, spells.get (2).getStatus ());
		assertEquals (SpellResearchStatusID.UNAVAILABLE, spells.get (3).getStatus ());
		assertEquals (SpellResearchStatusID.UNAVAILABLE, spells.get (4).getStatus ());
		assertEquals (SpellResearchStatusID.UNAVAILABLE, spells.get (5).getStatus ());
		assertEquals (SpellResearchStatusID.RESEARCHABLE_NOW, spells.get (6).getStatus ());
		assertEquals (SpellResearchStatusID.RESEARCHABLE_NOW, spells.get (7).getStatus ());
		assertEquals (SpellResearchStatusID.RESEARCHABLE_NOW, spells.get (8).getStatus ());
		assertEquals (SpellResearchStatusID.RESEARCHABLE_NOW, spells.get (9).getStatus ());

		int count = 0;
		for (int n = 10; n < 14; n++)
		{
			if (spells.get (n).getStatus ().equals (SpellResearchStatusID.RESEARCHABLE_NOW))
				count++;

			else if (!spells.get (n).getStatus ().equals (SpellResearchStatusID.RESEARCHABLE))
				fail ("Unexpected status");
		}

		assertEquals (2, count);

		assertEquals (SpellResearchStatusID.UNAVAILABLE, spells.get (14).getStatus ());
		assertEquals (SpellResearchStatusID.UNAVAILABLE, spells.get (15).getStatus ());
		assertEquals (SpellResearchStatusID.UNAVAILABLE, spells.get (16).getStatus ());
		assertEquals (SpellResearchStatusID.UNAVAILABLE, spells.get (17).getStatus ());
		assertEquals (SpellResearchStatusID.UNAVAILABLE, spells.get (18).getStatus ());
		assertEquals (SpellResearchStatusID.UNAVAILABLE, spells.get (19).getStatus ());
	}
}
