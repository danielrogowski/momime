package momime.server.messages;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_4.PlayerPick;
import momime.server.ServerTestData;
import momime.server.database.JAXBContextCreator;
import momime.server.database.ServerDatabaseLookup;
import momime.server.database.v0_9_4.ServerDatabase;

import org.junit.Test;

/**
 * Tests the ServerPlayerPickUtils class
 */
public final class TestServerPlayerPickUtils
{
	/** Dummy logger to use during unit tests */
	private final Logger debugLogger = Logger.getLogger ("MoMIMEServerUnitTests");

	/**
	 * Tests the getTotalInitialSkill method
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If we have a pick in our list which can't be found in the db
	 */
	@Test
	public final void testGetTotalInitialSkill () throws JAXBException, RecordNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.SERVER_XML_FILE);
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		// Add 1x Book 1, 2x Book 2, 3x Book 3, 4x Book 4 and 5x Book 5 = 15 books x2 skill per book = 30
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		for (int n = 1; n <= 5; n++)
		{
			final PlayerPick pick = new PlayerPick ();
			pick.setPickID ("MB0" + n);
			pick.setQuantity (n);
			picks.add (pick);
		}

		assertEquals (30, ServerPlayerPickUtils.getTotalInitialSkill (picks, db, debugLogger));

		// Archmage adds +10
		final PlayerPick archmage = new PlayerPick ();
		archmage.setPickID ("RT04");
		archmage.setQuantity (1);
		picks.add (archmage);

		assertEquals (40, ServerPlayerPickUtils.getTotalInitialSkill (picks, db, debugLogger));

		// Some other irrelevant retort adds nothing
		final PlayerPick somethingElse = new PlayerPick ();
		somethingElse.setPickID ("RT05");
		somethingElse.setQuantity (1);
		picks.add (somethingElse);

		assertEquals (40, ServerPlayerPickUtils.getTotalInitialSkill (picks, db, debugLogger));
	}
}
