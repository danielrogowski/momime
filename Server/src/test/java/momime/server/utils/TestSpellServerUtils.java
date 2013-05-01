package momime.server.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import momime.common.database.newgame.v0_9_4.SwitchResearch;
import momime.common.database.v0_9_4.Spell;
import momime.common.messages.SpellUtils;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.SpellResearchStatus;
import momime.common.messages.v0_9_4.SpellResearchStatusID;
import momime.server.ServerTestData;
import momime.server.database.ServerDatabaseEx;

import org.junit.Test;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the SpellServerUtils class
 */
public final class TestSpellServerUtils
{
	/**
	 * Tests the validateResearch method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 */
	@Test
	public final void testValidateResearch () throws IOException, JAXBException
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);

		// Initialize spell research
		for (final Spell thisSpell : db.getSpell ())
		{
			final SpellResearchStatus thisStatus = new SpellResearchStatus ();
			thisStatus.setSpellID (thisSpell.getSpellID ());
			thisStatus.setStatus (SpellResearchStatusID.UNAVAILABLE);

			if (thisSpell.getResearchCost () != null)
				thisStatus.setRemainingResearchCost (thisSpell.getResearchCost ());

			priv.getSpellResearchStatus ().add (thisStatus);
		}
		
		// Set up object to test
		final SpellServerUtils utils = new SpellServerUtils ();
		final SpellUtils spellUtils = new SpellUtils ();
		utils.setSpellUtils (spellUtils);

		// Can't research anything if its unavailable
		for (final SwitchResearch switchResearch : SwitchResearch.values ())
			assertNotNull (utils.validateResearch (player, "SP100", switchResearch, db));

		// Other statuses don't help either
		final SpellResearchStatus spell100 = spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP100");
		spell100.setStatus (SpellResearchStatusID.NOT_IN_SPELL_BOOK);
		for (final SwitchResearch switchResearch : SwitchResearch.values ())
			assertNotNull (utils.validateResearch (player, "SP100", switchResearch, db));

		spell100.setStatus (SpellResearchStatusID.AVAILABLE);
		for (final SwitchResearch switchResearch : SwitchResearch.values ())
			assertNotNull (utils.validateResearch (player, "SP100", switchResearch, db));

		spell100.setStatus (SpellResearchStatusID.RESEARCHABLE);
		for (final SwitchResearch switchResearch : SwitchResearch.values ())
			assertNotNull (utils.validateResearch (player, "SP100", switchResearch, db));

		// Now can research it regardless of setting because we have no current research
		spell100.setStatus (SpellResearchStatusID.RESEARCHABLE_NOW);
		for (final SwitchResearch switchResearch : SwitchResearch.values ())
			assertNull (utils.validateResearch (player, "SP100", switchResearch, db));

		// Also always fine if we're just setting research to what it already is
		priv.setSpellIDBeingResearched ("SP100");
		for (final SwitchResearch switchResearch : SwitchResearch.values ())
			assertNull (utils.validateResearch (player, "SP100", switchResearch, db));

		// Now set it to something different, disallowed then won't let us switch research
		priv.setSpellIDBeingResearched ("SP101");
		assertNull (utils.validateResearch (player, "SP100", SwitchResearch.FREE, db));
		assertNull (utils.validateResearch (player, "SP100", SwitchResearch.LOSE_CURRENT_RESEARCH, db));
		assertNotNull (utils.validateResearch (player, "SP100", SwitchResearch.DISALLOWED, db));
		assertNull (utils.validateResearch (player, "SP100", SwitchResearch.ONLY_IF_NOT_STARTED, db));

		// If we spent 1 BP on the spell previously being researched, that stops us swapping research if that is the chosen setting
		final SpellResearchStatus spell101 = spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP101");
		spell101.setRemainingResearchCost (spell101.getRemainingResearchCost () - 1);
		assertNull (utils.validateResearch (player, "SP100", SwitchResearch.FREE, db));
		assertNull (utils.validateResearch (player, "SP100", SwitchResearch.LOSE_CURRENT_RESEARCH, db));
		assertNotNull (utils.validateResearch (player, "SP100", SwitchResearch.DISALLOWED, db));
		assertNotNull (utils.validateResearch (player, "SP100", SwitchResearch.ONLY_IF_NOT_STARTED, db));
	}
}
