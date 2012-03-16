package momime.server.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import momime.common.database.newgame.v0_9_4.SwitchResearch;
import momime.common.database.v0_9_4.Spell;
import momime.common.messages.SpellUtils;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.SpellResearchStatus;
import momime.common.messages.v0_9_4.SpellResearchStatusID;
import momime.server.ServerTestData;
import momime.server.database.JAXBContextCreator;
import momime.server.database.ServerDatabaseLookup;
import momime.server.database.v0_9_4.ServerDatabase;

import org.junit.Test;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the SpellServerUtils class
 */
public final class TestSpellServerUtils
{
	/** Dummy logger to use during unit tests */
	private final Logger debugLogger = Logger.getLogger ("MoMIMEServerUnitTests");

	/**
	 * Tests the validateResearch method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 */
	@Test
	public final void testValidateResearch () throws IOException, JAXBException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);

		// Initialize spell research
		for (final Spell thisSpell : db.getSpells ())
		{
			final SpellResearchStatus thisStatus = new SpellResearchStatus ();
			thisStatus.setSpellID (thisSpell.getSpellID ());
			thisStatus.setStatus (SpellResearchStatusID.UNAVAILABLE);

			if (thisSpell.getResearchCost () != null)
				thisStatus.setRemainingResearchCost (thisSpell.getResearchCost ());

			priv.getSpellResearchStatus ().add (thisStatus);
		}

		// Can't research anything if its unavailable
		for (final SwitchResearch switchResearch : SwitchResearch.values ())
			assertNotNull (SpellServerUtils.validateResearch (player, "SP100", switchResearch, db, debugLogger));

		// Other statuses don't help either
		final SpellResearchStatus spell100 = SpellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP100", debugLogger);
		spell100.setStatus (SpellResearchStatusID.NOT_IN_SPELL_BOOK);
		for (final SwitchResearch switchResearch : SwitchResearch.values ())
			assertNotNull (SpellServerUtils.validateResearch (player, "SP100", switchResearch, db, debugLogger));

		spell100.setStatus (SpellResearchStatusID.AVAILABLE);
		for (final SwitchResearch switchResearch : SwitchResearch.values ())
			assertNotNull (SpellServerUtils.validateResearch (player, "SP100", switchResearch, db, debugLogger));

		spell100.setStatus (SpellResearchStatusID.RESEARCHABLE);
		for (final SwitchResearch switchResearch : SwitchResearch.values ())
			assertNotNull (SpellServerUtils.validateResearch (player, "SP100", switchResearch, db, debugLogger));

		// Now can research it regardless of setting because we have no current research
		spell100.setStatus (SpellResearchStatusID.RESEARCHABLE_NOW);
		for (final SwitchResearch switchResearch : SwitchResearch.values ())
			assertNull (SpellServerUtils.validateResearch (player, "SP100", switchResearch, db, debugLogger));

		// Also always fine if we're just setting research to what it already is
		priv.setSpellIDBeingResearched ("SP100");
		for (final SwitchResearch switchResearch : SwitchResearch.values ())
			assertNull (SpellServerUtils.validateResearch (player, "SP100", switchResearch, db, debugLogger));

		// Now set it to something different, disallowed then won't let us switch research
		priv.setSpellIDBeingResearched ("SP101");
		assertNull (SpellServerUtils.validateResearch (player, "SP100", SwitchResearch.FREE, db, debugLogger));
		assertNull (SpellServerUtils.validateResearch (player, "SP100", SwitchResearch.LOSE_CURRENT_RESEARCH, db, debugLogger));
		assertNotNull (SpellServerUtils.validateResearch (player, "SP100", SwitchResearch.DISALLOWED, db, debugLogger));
		assertNull (SpellServerUtils.validateResearch (player, "SP100", SwitchResearch.ONLY_IF_NOT_STARTED, db, debugLogger));

		// If we spent 1 BP on the spell previously being researched, that stops us swapping research if that is the chosen setting
		final SpellResearchStatus spell101 = SpellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP101", debugLogger);
		spell101.setRemainingResearchCost (spell101.getRemainingResearchCost () - 1);
		assertNull (SpellServerUtils.validateResearch (player, "SP100", SwitchResearch.FREE, db, debugLogger));
		assertNull (SpellServerUtils.validateResearch (player, "SP100", SwitchResearch.LOSE_CURRENT_RESEARCH, db, debugLogger));
		assertNotNull (SpellServerUtils.validateResearch (player, "SP100", SwitchResearch.DISALLOWED, db, debugLogger));
		assertNotNull (SpellServerUtils.validateResearch (player, "SP100", SwitchResearch.ONLY_IF_NOT_STARTED, db, debugLogger));
	}
}
