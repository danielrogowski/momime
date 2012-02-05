package momime.server.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.v0_9_4.DifficultyLevelData;
import momime.common.database.v0_9_4.WizardPick;
import momime.common.messages.servertoclient.v0_9_4.ChooseInitialSpellsNowMessage;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.PlayerPick;
import momime.common.messages.v0_9_4.SpellResearchStatus;
import momime.common.messages.v0_9_4.SpellResearchStatusID;
import momime.server.ServerTestData;
import momime.server.database.JAXBContextCreator;
import momime.server.database.ServerDatabaseLookup;
import momime.server.database.v0_9_4.ServerDatabase;
import momime.server.database.v0_9_4.Spell;
import momime.server.database.v0_9_4.Wizard;

import org.junit.Test;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the PlayerPickServerUtils class
 */
public final class TestPlayerPickServerUtils
{
	/** Dummy logger to use during unit tests */
	private final Logger debugLogger = Logger.getLogger ("MoMIMEServerUnitTests");

	/**
	 * Tests the findPlayerUsingWizard method on a wizard who is in the list
	 */
	@Test
	public final void testFindPlayerUsingWizard_Exists ()
	{
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		for (int n = 1; n <= 9; n++)
		{
			final PlayerDescription pd = new PlayerDescription ();
			pd.setPlayerID (n);

			final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
			ppk.setWizardID ("WZ0" + n);

			players.add (new PlayerServerDetails (pd, ppk, null, null, null));
		}

		final PlayerServerDetails player = PlayerPickServerUtils.findPlayerUsingWizard (players, "WZ04", debugLogger);
		final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		assertEquals ("WZ04", ppk.getWizardID ());
	}

	/**
	 * Tests the findPlayerUsingWizard method on a wizard who isn't in the list
	 */
	@Test
	public final void testFindPlayerUsingWizard_NotExists ()
	{
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		for (int n = 1; n <= 9; n++)
		{
			final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
			ppk.setWizardID ("WZ0" + n);

			players.add (new PlayerServerDetails (null, ppk, null, null, null));
		}

		assertNull (PlayerPickServerUtils.findPlayerUsingWizard (players, "WZ10", debugLogger));
	}

	/**
	 * Tests the findPlayerUsingStandardPhoto method on a standard photo that is in the list
	 */
	@Test
	public final void testFindPlayerUsingStandardPhoto_Exists ()
	{
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		for (int n = 1; n <= 9; n++)
		{
			final PlayerDescription pd = new PlayerDescription ();
			pd.setPlayerID (n);

			final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
			ppk.setStandardPhotoID ("WZ0" + n);

			players.add (new PlayerServerDetails (pd, ppk, null, null, null));
		}

		final PlayerServerDetails player = PlayerPickServerUtils.findPlayerUsingStandardPhoto (players, "WZ04", debugLogger);
		final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		assertEquals ("WZ04", ppk.getStandardPhotoID ());
	}

	/**
	 * Tests the findPlayerUsingStandardPhoto method on a standard photo that isn't in the list
	 */
	@Test
	public final void testFindPlayerUsingStandardPhoto_NotExists ()
	{
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		for (int n = 1; n <= 9; n++)
		{
			final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
			ppk.setStandardPhotoID ("WZ0" + n);

			players.add (new PlayerServerDetails (null, ppk, null, null, null));
		}

		assertNull (PlayerPickServerUtils.findPlayerUsingStandardPhoto (players, "WZ10", debugLogger));
	}

	/**
	 * Tests the validateCustomPicks method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 */
	@Test
	public final void testValidateCustomPicks () throws IOException, JAXBException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		// Set up session description
		final DifficultyLevelData dl = new DifficultyLevelData ();
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setDifficultyLevel (dl);

		// Set up player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPrivateKnowledge priv = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, null, null, priv);

		// Create requested picks list
		final List<WizardPick> picks = new ArrayList<WizardPick> ();

		// This is invalid because the player didn't choose a wizard yet
		assertNotNull (PlayerPickServerUtils.validateCustomPicks (player, picks, sd, db, debugLogger));

		// Can't choose a pre-defined wizard either
		ppk.setWizardID ("WZ01");
		assertNotNull (PlayerPickServerUtils.validateCustomPicks (player, picks, sd, db, debugLogger));

		// This is valid, because they're requesting 0 picks and the session description currently says 0 picks
		ppk.setWizardID ("");
		assertNull (PlayerPickServerUtils.validateCustomPicks (player, picks, sd, db, debugLogger));

		// Invalid because we didn't choose enough picks
		dl.setHumanSpellPicks (11);
		assertNotNull (PlayerPickServerUtils.validateCustomPicks (player, picks, sd, db, debugLogger));

		// Still not enough picks
		final WizardPick lifeBook = new WizardPick ();
		lifeBook.setPick ("MB01");
		lifeBook.setQuantity (10);
		picks.add (lifeBook);

		assertNotNull (PlayerPickServerUtils.validateCustomPicks (player, picks, sd, db, debugLogger));

		// Too many picks
		final WizardPick warlord = new WizardPick ();
		warlord.setPick (CommonDatabaseConstants.VALUE_RETORT_ID_WARLORD);
		warlord.setQuantity (1);
		picks.add (warlord);

		assertNotNull (PlayerPickServerUtils.validateCustomPicks (player, picks, sd, db, debugLogger));

		// Just right
		lifeBook.setQuantity (9);
		assertNull (PlayerPickServerUtils.validateCustomPicks (player, picks, sd, db, debugLogger));

		// Already chosen picks
		priv.setCustomPicksChosen (true);
		assertNotNull (PlayerPickServerUtils.validateCustomPicks (player, picks, sd, db, debugLogger));
	}

	/**
	 * Tests the findRealmIDWhereWeNeedToChooseFreeSpells method on a human player
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFindRealmIDWhereWeNeedToChooseFreeSpells_Human () throws Exception
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		// Set up player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();

		for (final Spell spell : serverDB.getSpell ())
		{
			final SpellResearchStatus researchStatus = new SpellResearchStatus ();
			researchStatus.setSpellID (spell.getSpellID ());
			researchStatus.setStatus (SpellResearchStatusID.UNAVAILABLE);
			priv.getSpellResearchStatus ().add (researchStatus);
		}

		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, priv, null, null);

		// So far we have no books, so we get no free spells
		assertNull (PlayerPickServerUtils.findRealmIDWhereWeNeedToChooseFreeSpells (player, db, debugLogger));

		// Give player a retort and 1 chaos book - neither of which is enough to grant any free spells
		final PlayerPick retort = new PlayerPick ();
		retort.setPickID (CommonDatabaseConstants.VALUE_RETORT_ID_ALCHEMY);
		retort.setQuantity (1);
		ppk.getPick ().add (retort);

		final PlayerPick chaosBook = new PlayerPick ();
		chaosBook.setPickID ("MB03");
		chaosBook.setQuantity (1);
		ppk.getPick ().add (chaosBook);

		assertNull (PlayerPickServerUtils.findRealmIDWhereWeNeedToChooseFreeSpells (player, db, debugLogger));

		// Give player 4 nature books - which then grant 3 free common nature spells
		final PlayerPick natureBooks = new PlayerPick ();
		natureBooks.setPickID ("MB04");
		natureBooks.setQuantity (4);
		ppk.getPick ().add (natureBooks);

		final ChooseInitialSpellsNowMessage natureBooksResult = PlayerPickServerUtils.findRealmIDWhereWeNeedToChooseFreeSpells (player, db, debugLogger);
		assertEquals ("MB04", natureBooksResult.getMagicRealmID ());
		assertEquals (1, natureBooksResult.getSpellRank ().size ());
		assertEquals ("SR01", natureBooksResult.getSpellRank ().get (0).getSpellRankID ());
		assertEquals (3, natureBooksResult.getSpellRank ().get (0).getFreeSpellCount ());

		// Choose 2 of those spells, then we only have 1 left to choose
		priv.getSpellResearchStatus ().get (3).setStatus (SpellResearchStatusID.AVAILABLE);
		priv.getSpellResearchStatus ().get (7).setStatus (SpellResearchStatusID.AVAILABLE);

		final ChooseInitialSpellsNowMessage chosenSomeSpells = PlayerPickServerUtils.findRealmIDWhereWeNeedToChooseFreeSpells (player, db, debugLogger);
		assertEquals ("MB04", chosenSomeSpells.getMagicRealmID ());
		assertEquals (1, chosenSomeSpells.getSpellRank ().size ());
		assertEquals ("SR01", chosenSomeSpells.getSpellRank ().get (0).getSpellRankID ());
		assertEquals (1, chosenSomeSpells.getSpellRank ().get (0).getFreeSpellCount ());

		// Now get 11 nature books, so we get free spells at 3 different ranks, 2 of which we've already chosen
		natureBooks.setQuantity (11);

		final ChooseInitialSpellsNowMessage elevenBooks = PlayerPickServerUtils.findRealmIDWhereWeNeedToChooseFreeSpells (player, db, debugLogger);
		assertEquals ("MB04", elevenBooks.getMagicRealmID ());
		assertEquals (3, elevenBooks.getSpellRank ().size ());
		assertEquals ("SR01", elevenBooks.getSpellRank ().get (0).getSpellRankID ());
		assertEquals (8, elevenBooks.getSpellRank ().get (0).getFreeSpellCount ());
		assertEquals ("SR02", elevenBooks.getSpellRank ().get (1).getSpellRankID ());
		assertEquals (2, elevenBooks.getSpellRank ().get (1).getFreeSpellCount ());
		assertEquals ("SR03", elevenBooks.getSpellRank ().get (2).getSpellRankID ());
		assertEquals (1, elevenBooks.getSpellRank ().get (2).getFreeSpellCount ());
	}

	/**
	 * Tests the findRealmIDWhereWeNeedToChooseFreeSpells method on an AI player
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFindRealmIDWhereWeNeedToChooseFreeSpells_AI () throws Exception
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		// Set up player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (false);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();

		for (final Spell spell : serverDB.getSpell ())
		{
			final SpellResearchStatus researchStatus = new SpellResearchStatus ();
			researchStatus.setSpellID (spell.getSpellID ());
			researchStatus.setStatus (SpellResearchStatusID.UNAVAILABLE);
			priv.getSpellResearchStatus ().add (researchStatus);
		}

		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, priv, null, null);

		// So far we have no books, so we get no free spells
		assertNull (PlayerPickServerUtils.findRealmIDWhereWeNeedToChooseFreeSpells (player, db, debugLogger));

		// Give player a retort and 1 chaos book - neither of which is enough to grant any free spells
		final PlayerPick retort = new PlayerPick ();
		retort.setPickID (CommonDatabaseConstants.VALUE_RETORT_ID_ALCHEMY);
		retort.setQuantity (1);
		ppk.getPick ().add (retort);

		final PlayerPick chaosBook = new PlayerPick ();
		chaosBook.setPickID ("MB03");
		chaosBook.setQuantity (1);
		ppk.getPick ().add (chaosBook);

		assertNull (PlayerPickServerUtils.findRealmIDWhereWeNeedToChooseFreeSpells (player, db, debugLogger));

		// Give player 4 nature books - which then grant 3 free common nature spells
		// Since its an AI player, they then actually choose the spells
		// There's exactly 3 common nature spells with AI research order 1, so the AI player should choose to get those for free
		final PlayerPick natureBooks = new PlayerPick ();
		natureBooks.setPickID ("MB04");
		natureBooks.setQuantity (4);
		ppk.getPick ().add (natureBooks);

		PlayerPickServerUtils.findRealmIDWhereWeNeedToChooseFreeSpells (player, db, debugLogger);
		assertEquals (SpellResearchStatusID.UNAVAILABLE, priv.getSpellResearchStatus ().get (0).getStatus ());
		assertEquals (SpellResearchStatusID.UNAVAILABLE, priv.getSpellResearchStatus ().get (1).getStatus ());
		assertEquals (SpellResearchStatusID.UNAVAILABLE, priv.getSpellResearchStatus ().get (2).getStatus ());
		assertEquals (SpellResearchStatusID.AVAILABLE, priv.getSpellResearchStatus ().get (3).getStatus ());
		assertEquals (SpellResearchStatusID.UNAVAILABLE, priv.getSpellResearchStatus ().get (4).getStatus ());
		assertEquals (SpellResearchStatusID.UNAVAILABLE, priv.getSpellResearchStatus ().get (5).getStatus ());
		assertEquals (SpellResearchStatusID.AVAILABLE, priv.getSpellResearchStatus ().get (6).getStatus ());
		assertEquals (SpellResearchStatusID.UNAVAILABLE, priv.getSpellResearchStatus ().get (7).getStatus ());
		assertEquals (SpellResearchStatusID.UNAVAILABLE, priv.getSpellResearchStatus ().get (8).getStatus ());
		assertEquals (SpellResearchStatusID.AVAILABLE, priv.getSpellResearchStatus ().get (9).getStatus ());

		// If we run it again, there's nothing else to pick because we've already done so
		assertNull (PlayerPickServerUtils.findRealmIDWhereWeNeedToChooseFreeSpells (player, db, debugLogger));
	}

	/**
	 * Tests the validateInitialSpellSelection method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If the pick ID can't be found in the database, or refers to a pick type ID that can't be found; or the player has a spell research status that isn't found
	 */
	@Test
	public final void testValidateInitialSpellSelection () throws IOException, JAXBException, RecordNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		// Set up player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();

		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, priv, null, null);

		// Ask for a pick ID that we have none of
		final List<String> spellIDs = new ArrayList<String> ();
		assertNotNull (PlayerPickServerUtils.validateInitialSpellSelection (player, "MB04", spellIDs, db, debugLogger));

		// Using a number of books that isn't listed under pick type "B" means we get no free spells
		// Bit of an artificial test because we're triggering it by going over the number of books we could ever have
		final PlayerPick natureBooks = new PlayerPick ();
		natureBooks.setPickID ("MB04");
		natureBooks.setQuantity (21);
		ppk.getPick ().add (natureBooks);

		assertNotNull (PlayerPickServerUtils.validateInitialSpellSelection (player, "MB04", spellIDs, db, debugLogger));

		// Get 4 books, so we get 3 free spell choices - because we're requesting no spells, its valid
		natureBooks.setQuantity (4);
		assertNull (PlayerPickServerUtils.validateInitialSpellSelection (player, "MB04", spellIDs, db, debugLogger));

		// Request wrong rank
		spellIDs.add ("SP011");
		assertNotNull (PlayerPickServerUtils.validateInitialSpellSelection (player, "MB04", spellIDs, db, debugLogger));

		// Request wrong magic realm
		spellIDs.set (0, "SP041");
		assertNotNull (PlayerPickServerUtils.validateInitialSpellSelection (player, "MB04", spellIDs, db, debugLogger));

		// Valid request
		spellIDs.set (0, "SP001");
		assertNull (PlayerPickServerUtils.validateInitialSpellSelection (player, "MB04", spellIDs, db, debugLogger));

		spellIDs.add ("SP002");
		spellIDs.add ("SP003");
		assertNull (PlayerPickServerUtils.validateInitialSpellSelection (player, "MB04", spellIDs, db, debugLogger));

		// Request too many
		spellIDs.add ("SP004");
		assertNotNull (PlayerPickServerUtils.validateInitialSpellSelection (player, "MB04", spellIDs, db, debugLogger));
	}

	/**
	 * Tests the validateRaceChoice method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If we choose a race whose native plane can't be found
	 */
	@Test
	public final void testValidateRaceChoice () throws IOException, JAXBException, RecordNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		// Set up player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();

		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, null, null, null);

		// Invalid race
		assertNotNull (PlayerPickServerUtils.validateRaceChoice (player, "RC15", db, debugLogger));

		// Race with no pre-requisite
		assertNull (PlayerPickServerUtils.validateRaceChoice (player, "RC04", db, debugLogger));

		// Myrran race without Myrran pick
		assertNotNull (PlayerPickServerUtils.validateRaceChoice (player, "RC14", db, debugLogger));

		// Myrran race without Myrran pick
		final PlayerPick myrran = new PlayerPick ();
		myrran.setPickID ("RT08");
		myrran.setQuantity (1);
		ppk.getPick ().add (myrran);

		assertNull (PlayerPickServerUtils.validateRaceChoice (player, "RC14", db, debugLogger));
	}

	/**
	 * Tests the hasChosenAllDetails method
	 */
	@Test
	public final void testHasChosenAllDetails ()
	{
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPrivateKnowledge priv = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, null, null, priv);

		// Nothing picked yet
		assertFalse (PlayerPickServerUtils.hasChosenAllDetails (player, debugLogger));

		// Wizard but no race
		ppk.setWizardID ("WZ01");
		assertFalse (PlayerPickServerUtils.hasChosenAllDetails (player, debugLogger));

		// Standard wizard
		priv.setFirstCityRaceID ("RC01");
		assertTrue (PlayerPickServerUtils.hasChosenAllDetails (player, debugLogger));

		// Custom wizard without custom picks chosen
		ppk.setWizardID ("");
		assertFalse (PlayerPickServerUtils.hasChosenAllDetails (player, debugLogger));

		// Chosen custom picks
		priv.setCustomPicksChosen (true);
		assertTrue (PlayerPickServerUtils.hasChosenAllDetails (player, debugLogger));
	}

	/**
	 * Tests the allPlayersHaveChosenAllDetails method
	 */
	@Test
	public final void testAllPlayersHaveChosenAllDetails ()
	{
		// Delphi client sets maxplayers = human opponents + AI opponents + 3, so follow that here
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setMaxPlayers (3);
		sd.setAiPlayerCount (0);

		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Single player game just against raiders, and nothing chosen yet
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPrivateKnowledge priv = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, null, null, priv);
		players.add (player);

		assertFalse (PlayerPickServerUtils.allPlayersHaveChosenAllDetails (players, sd, debugLogger));

		// Fill in details
		ppk.setWizardID ("WZ01");
		priv.setFirstCityRaceID ("RC01");
		assertTrue (PlayerPickServerUtils.allPlayersHaveChosenAllDetails (players, sd, debugLogger));

		// Add an AI player - this doesn't stop the game starting, since AI players are added after
		sd.setMaxPlayers (4);
		sd.setAiPlayerCount (1);
		assertTrue (PlayerPickServerUtils.allPlayersHaveChosenAllDetails (players, sd, debugLogger));

		// Add slot for 2nd player
		sd.setMaxPlayers (5);
		assertFalse (PlayerPickServerUtils.allPlayersHaveChosenAllDetails (players, sd, debugLogger));

		// Add second player
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (3);
		pd2.setHuman (true);

		final MomPersistentPlayerPublicKnowledge ppk2 = new MomPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPrivateKnowledge priv2 = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, ppk2, null, null, priv2);
		players.add (player2);

		assertFalse (PlayerPickServerUtils.allPlayersHaveChosenAllDetails (players, sd, debugLogger));

		// Fill in second player details
		ppk2.setWizardID ("WZ02");
		priv2.setFirstCityRaceID ("RC02");
		assertTrue (PlayerPickServerUtils.allPlayersHaveChosenAllDetails (players, sd, debugLogger));
	}

	/**
	 * Tests the listWizardsForAIPlayers method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 */
	@Test
	public final void testListWizardsForAIPlayers () throws IOException, JAXBException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		// Quick check on DB
		assertEquals (16, db.getWizards ().size ());

		// Create human players using 2 wizards
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		final MomPersistentPlayerPublicKnowledge ppk1 = new MomPersistentPlayerPublicKnowledge ();
		ppk1.setStandardPhotoID ("WZ02");
		players.add (new PlayerServerDetails (pd1, ppk1, null, null, null));

		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (2);
		final MomPersistentPlayerPublicKnowledge ppk2 = new MomPersistentPlayerPublicKnowledge ();
		ppk2.setStandardPhotoID ("WZ12");
		players.add (new PlayerServerDetails (pd2, ppk2, null, null, null));

		// Run test
		final List<Wizard> wizardIDs = PlayerPickServerUtils.listWizardsForAIPlayers (players, db, debugLogger);
		assertEquals (12, wizardIDs.size ());
		assertEquals ("WZ01", wizardIDs.get (0).getWizardID ());
		assertEquals ("WZ03", wizardIDs.get (1).getWizardID ());
		assertEquals ("WZ04", wizardIDs.get (2).getWizardID ());
		assertEquals ("WZ05", wizardIDs.get (3).getWizardID ());
		assertEquals ("WZ06", wizardIDs.get (4).getWizardID ());
		assertEquals ("WZ07", wizardIDs.get (5).getWizardID ());
		assertEquals ("WZ08", wizardIDs.get (6).getWizardID ());
		assertEquals ("WZ09", wizardIDs.get (7).getWizardID ());
		assertEquals ("WZ10", wizardIDs.get (8).getWizardID ());
		assertEquals ("WZ11", wizardIDs.get (9).getWizardID ());
		assertEquals ("WZ13", wizardIDs.get (10).getWizardID ());
		assertEquals ("WZ14", wizardIDs.get (11).getWizardID ());
	}

	/**
	 * Tests the startingPlaneForWizard method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 */
	@Test
	public final void testStartingPlaneForWizard () throws IOException, JAXBException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();

		// No picks
		assertEquals (0, PlayerPickServerUtils.startingPlaneForWizard (picks, db, debugLogger));

		// Irrelevant pick
		final PlayerPick warlord = new PlayerPick ();
		warlord.setPickID (CommonDatabaseConstants.VALUE_RETORT_ID_WARLORD);
		warlord.setQuantity (1);
		picks.add (warlord);

		assertEquals (0, PlayerPickServerUtils.startingPlaneForWizard (picks, db, debugLogger));

		// Myrran pick
		final PlayerPick myrran = new PlayerPick ();
		myrran.setPickID ("RT08");
		myrran.setQuantity (1);
		picks.add (myrran);

		assertEquals (1, PlayerPickServerUtils.startingPlaneForWizard (picks, db, debugLogger));
	}

	/**
	 * Tests the chooseRandomRaceForPlane method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws MomException If there are no races defined in the database that inhabit this plane
	 */
	@Test
	public final void testChooseRandomRaceForPlane () throws IOException, JAXBException, MomException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		final int arcanusRaceID = Integer.parseInt (PlayerPickServerUtils.chooseRandomRaceForPlane (0, db, debugLogger).substring (2));
		assertTrue ((arcanusRaceID >= 1) && (arcanusRaceID <= 9));

		final int myrranRaceID = Integer.parseInt (PlayerPickServerUtils.chooseRandomRaceForPlane (1, db, debugLogger).substring (2));
		assertTrue ((myrranRaceID >= 10) && (myrranRaceID <= 14));
	}
}
