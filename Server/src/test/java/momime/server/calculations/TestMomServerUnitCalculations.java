package momime.server.calculations;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.UnitUtils;
import momime.common.messages.v0_9_4.MemoryCombatAreaEffect;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.server.ServerTestData;
import momime.server.database.JAXBContextCreator;
import momime.server.database.ServerDatabaseLookup;
import momime.server.database.v0_9_4.ServerDatabase;

import org.junit.Test;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the MomServerUnitCalculations class
 */
public final class TestMomServerUnitCalculations
{
	/** Dummy logger to use during unit tests */
	private final Logger debugLogger = Logger.getLogger ("MoMIMEServerUnitTests");

	/**
	 * Tests the calculateUnitScoutingRange class
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If we can't find the player who owns the unit, or the unit has a skill that we can't find in the cache
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	@Test
	public final void testCalculateUnitScoutingRange () throws IOException, JAXBException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();

		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (new PlayerServerDetails (pd, ppk, null, null, null));

		// High men spearmen
		final MemoryUnit highMenSpearmen = UnitUtils.createMemoryUnit ("UN105", 1, 0, 0, true, db, debugLogger);
		highMenSpearmen.setOwningPlayerID (2);
		assertEquals (1, MomServerUnitCalculations.calculateUnitScoutingRange (highMenSpearmen, players, spells, combatAreaEffects, db, debugLogger));

		// Draconian spearmen can fly so can see range 2
		final MemoryUnit draconianSpearmen = UnitUtils.createMemoryUnit ("UN067", 2, 0, 0, true, db, debugLogger);
		draconianSpearmen.setOwningPlayerID (2);
		assertEquals (2, MomServerUnitCalculations.calculateUnitScoutingRange (draconianSpearmen, players, spells, combatAreaEffects, db, debugLogger));

		// Cast chaos channels flight on the high men spearmen to prove this is the same as if they had the skill naturally
		final MemoryMaintainedSpell ccFlight = new MemoryMaintainedSpell ();
		ccFlight.setUnitURN (1);
		ccFlight.setSpellID ("SP093");
		ccFlight.setUnitSkillID ("SS093C");
		spells.add (ccFlight);

		assertEquals (2, MomServerUnitCalculations.calculateUnitScoutingRange (highMenSpearmen, players, spells, combatAreaEffects, db, debugLogger));

		// Beastmaster hero has Scouting III skill
		final MemoryUnit beastmaster = UnitUtils.createMemoryUnit ("UN005", 3, 0, 0, true, db, debugLogger);
		beastmaster.setOwningPlayerID (2);
		assertEquals (3, MomServerUnitCalculations.calculateUnitScoutingRange (beastmaster, players, spells, combatAreaEffects, db, debugLogger));
	}
}
