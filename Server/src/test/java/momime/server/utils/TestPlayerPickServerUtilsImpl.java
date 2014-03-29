package momime.server.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.WizardPick;
import momime.common.messages.servertoclient.v0_9_4.ChooseInitialSpellsNowMessage;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.PlayerPick;
import momime.common.messages.v0_9_4.SpellResearchStatus;
import momime.common.messages.v0_9_4.SpellResearchStatusID;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.SpellUtils;
import momime.server.ai.SpellAI;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_4.Pick;
import momime.server.database.v0_9_4.PickType;
import momime.server.database.v0_9_4.PickTypeCountContainer;
import momime.server.database.v0_9_4.PickTypeGrantsSpells;
import momime.server.database.v0_9_4.Plane;
import momime.server.database.v0_9_4.Race;
import momime.server.database.v0_9_4.Spell;
import momime.server.database.v0_9_4.Wizard;

import org.junit.Test;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.random.RandomUtils;

/**
 * Tests the PlayerPickServerUtilsImpl class
 */
public final class TestPlayerPickServerUtilsImpl
{
	/**
	 * Tests the getTotalInitialSkill method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetTotalInitialSkill () throws Exception
	{
		// Mock some types of pick
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		for (int n = 1; n <= 5; n++)
		{
			final Pick pick = new Pick ();
			pick.setPickID ("MB0" + n);
			pick.setPickInitialSkill (2);
			when (db.findPick ("MB0" + n, "getTotalInitialSkill")).thenReturn (pick);
		}
		
		for (int n = 1; n <= 2; n++)
		{
			final Pick pick = new Pick ();
			pick.setPickID ("RT0" + n);
			if (n == 2)
				pick.setPickInitialSkill (10);
			when (db.findPick ("RT0" + n, "getTotalInitialSkill")).thenReturn (pick);
		}

		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		
		// Add 1x Book 1, 2x Book 2, 3x Book 3, 4x Book 4 and 5x Book 5 = 15 books x2 skill per book = 30
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		for (int n = 1; n <= 5; n++)
		{
			final PlayerPick pick = new PlayerPick ();
			pick.setPickID ("MB0" + n);
			pick.setQuantity (n);
			picks.add (pick);
		}

		assertEquals (30, utils.getTotalInitialSkill (picks, db));

		// Add a retort that gives +10
		final PlayerPick archmage = new PlayerPick ();
		archmage.setPickID ("RT02");
		archmage.setQuantity (1);
		picks.add (archmage);

		assertEquals (40, utils.getTotalInitialSkill (picks, db));

		// Add a retort that gives nothing
		final PlayerPick somethingElse = new PlayerPick ();
		somethingElse.setPickID ("RT01");
		somethingElse.setQuantity (1);
		picks.add (somethingElse);

		assertEquals (40, utils.getTotalInitialSkill (picks, db));
	}

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

		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		
		// Run test
		final PlayerServerDetails player = utils.findPlayerUsingWizard (players, "WZ04");
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

		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		
		// Run test
		assertNull (utils.findPlayerUsingWizard (players, "WZ10"));
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

		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		
		// Run test
		final PlayerServerDetails player = utils.findPlayerUsingStandardPhoto (players, "WZ04");
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

		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		
		// Run test
		assertNull (utils.findPlayerUsingStandardPhoto (players, "WZ10"));
	}

	/**
	 * Tests the validateCustomPicks method, trying to select custom picks when we didn't choose a wizard yet
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCustomPicks_DidntPickWizardYet () throws Exception
	{
		// Mock some types of pick
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		for (int n = 1; n <= 3; n++)
		{
			final Pick pick = new Pick ();
			pick.setPickID ("MB0" + n);
			pick.setPickCost (n);
			when (db.findPick ("MB0" + n, "validateCustomPicks")).thenReturn (pick);
		}

		// Set up player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPrivateKnowledge priv = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, null, null, priv);

		// Create requested picks list
		final List<WizardPick> picks = new ArrayList<WizardPick> ();
		
		final WizardPick pick1 = new WizardPick ();
		pick1.setPick ("MB02");
		pick1.setQuantity (4);
		picks.add (pick1);
		
		final WizardPick pick2 = new WizardPick ();
		pick2.setPick ("MB03");
		pick2.setQuantity (1);
		picks.add (pick2);

		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		
		// Check results
		assertNotNull (utils.validateCustomPicks (player, picks, 11, db));
	}

	/**
	 * Tests the validateCustomPicks method, trying to select custom picks when we picked a standard wizard
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCustomPicks_PickedStandardWizard () throws Exception
	{
		// Mock some types of pick
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		for (int n = 1; n <= 3; n++)
		{
			final Pick pick = new Pick ();
			pick.setPickID ("MB0" + n);
			pick.setPickCost (n);
			when (db.findPick ("MB0" + n, "validateCustomPicks")).thenReturn (pick);
		}

		// Set up player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		ppk.setWizardID ("WZ01");
		
		final MomTransientPlayerPrivateKnowledge priv = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, null, null, priv);

		// Create requested picks list
		final List<WizardPick> picks = new ArrayList<WizardPick> ();
		
		final WizardPick pick1 = new WizardPick ();
		pick1.setPick ("MB02");
		pick1.setQuantity (4);
		picks.add (pick1);
		
		final WizardPick pick2 = new WizardPick ();
		pick2.setPick ("MB03");
		pick2.setQuantity (1);
		picks.add (pick2);

		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		
		// Check results
		assertNotNull (utils.validateCustomPicks (player, picks, 11, db));
	}

	/**
	 * Tests the validateCustomPicks method, trying to select custom picks but we didn't pick enough
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCustomPicks_PickedNotEnough () throws Exception
	{
		// Mock some types of pick
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		for (int n = 1; n <= 3; n++)
		{
			final Pick pick = new Pick ();
			pick.setPickID ("MB0" + n);
			pick.setPickCost (n);
			when (db.findPick ("MB0" + n, "validateCustomPicks")).thenReturn (pick);
		}

		// Set up player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		ppk.setWizardID ("");
		
		final MomTransientPlayerPrivateKnowledge priv = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, null, null, priv);

		// Create requested picks list
		final List<WizardPick> picks = new ArrayList<WizardPick> ();
		
		final WizardPick pick1 = new WizardPick ();
		pick1.setPick ("MB02");
		pick1.setQuantity (4);
		picks.add (pick1);

		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		
		// Check results
		assertNotNull (utils.validateCustomPicks (player, picks, 11, db));
	}
	
	/**
	 * Tests the validateCustomPicks method, trying to select custom picks but we picked too many
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCustomPicks_PickedTooMany () throws Exception
	{
		// Mock some types of pick
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		for (int n = 1; n <= 3; n++)
		{
			final Pick pick = new Pick ();
			pick.setPickID ("MB0" + n);
			pick.setPickCost (n);
			when (db.findPick ("MB0" + n, "validateCustomPicks")).thenReturn (pick);
		}

		// Set up player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		ppk.setWizardID ("");
		
		final MomTransientPlayerPrivateKnowledge priv = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, null, null, priv);

		// Create requested picks list
		final List<WizardPick> picks = new ArrayList<WizardPick> ();
		
		final WizardPick pick1 = new WizardPick ();
		pick1.setPick ("MB02");
		pick1.setQuantity (4);
		picks.add (pick1);
		
		final WizardPick pick2 = new WizardPick ();
		pick2.setPick ("MB03");
		pick2.setQuantity (2);
		picks.add (pick2);

		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		
		// Check results
		assertNotNull (utils.validateCustomPicks (player, picks, 11, db));
	}
	
	/**
	 * Tests the validateCustomPicks method, trying to select custom picks and everything is fine
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCustomPicks () throws Exception
	{
		// Mock some types of pick
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		for (int n = 1; n <= 3; n++)
		{
			final Pick pick = new Pick ();
			pick.setPickID ("MB0" + n);
			pick.setPickCost (n);
			when (db.findPick ("MB0" + n, "validateCustomPicks")).thenReturn (pick);
		}

		// Set up player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		ppk.setWizardID ("");
		
		final MomTransientPlayerPrivateKnowledge priv = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, null, null, priv);

		// Create requested picks list
		final List<WizardPick> picks = new ArrayList<WizardPick> ();
		
		final WizardPick pick1 = new WizardPick ();
		pick1.setPick ("MB02");
		pick1.setQuantity (4);
		picks.add (pick1);
		
		final WizardPick pick2 = new WizardPick ();
		pick2.setPick ("MB03");
		pick2.setQuantity (1);
		picks.add (pick2);

		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		
		// Check results
		assertNull (utils.validateCustomPicks (player, picks, 11, db));
	}
	
	/**
	 * Tests the countFreeSpellsLeftToChoose method when we don't have enough picks to get any free spells
	 * @throws RecordNotFoundException If there is a problem
	 */
	@Test
	public final void testCountFreeSpellsLeftToChoose_None () throws RecordNotFoundException
	{
		// Mock the pick and pick type details
		final Pick pick = new Pick ();
		pick.setPickType ("X");
		
		final PickType pickType = new PickType ();
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findPick ("MB01", "countFreeSpellsLeftToChoose")).thenReturn (pick);
		when (db.findPickType ("X", "countFreeSpellsLeftToChoose")).thenReturn (pickType);

		// Set up player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
		
		// Type of book we're choosing picks for
		final PlayerPick playerPick = new PlayerPick ();
		playerPick.setPickID ("MB01");
		playerPick.setQuantity (5);
		
		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		
		// Run method
		final ChooseInitialSpellsNowMessage msg = utils.countFreeSpellsLeftToChoose (player, playerPick, db);
		
		// Check results
		assertNull (msg);
	}
	
	/**
	 * Tests the countFreeSpellsLeftToChoose method when we do get some free spells
	 * @throws RecordNotFoundException If there is a problem
	 */
	@Test
	public final void testCountFreeSpellsLeftToChoose () throws RecordNotFoundException
	{
		// Mock the pick and pick type details
		final Pick pick = new Pick ();
		pick.setPickType ("X");
		
		final PickType pickType = new PickType ();
		
		final PickTypeCountContainer elevenPicks = new PickTypeCountContainer ();
		elevenPicks.setCount (11);
		pickType.getPickTypeCount ().add (elevenPicks);
		
		final PickTypeGrantsSpells common = new PickTypeGrantsSpells ();
		common.setSpellRank ("SR01");
		common.setSpellsFreeAtStart (10);
		elevenPicks.getSpellCount ().add (common);

		final PickTypeGrantsSpells uncommon = new PickTypeGrantsSpells ();
		uncommon.setSpellRank ("SR02");
		uncommon.setSpellsFreeAtStart (2);
		elevenPicks.getSpellCount ().add (uncommon);

		final PickTypeGrantsSpells rare = new PickTypeGrantsSpells ();
		rare.setSpellRank ("SR03");
		rare.setSpellsFreeAtStart (1);
		elevenPicks.getSpellCount ().add (rare);
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findPick ("MB01", "countFreeSpellsLeftToChoose")).thenReturn (pick);
		when (db.findPickType ("X", "countFreeSpellsLeftToChoose")).thenReturn (pickType);

		// Set up player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
		
		// Type of book we're choosing picks for
		final PlayerPick playerPick = new PlayerPick ();
		playerPick.setPickID ("MB01");
		playerPick.setQuantity (11);

		// We already picked 6 of the free common spells, and the two uncommon
		final List<momime.common.database.v0_9_4.Spell> commonsAlreadyChosen = new ArrayList<momime.common.database.v0_9_4.Spell> ();
		for (int n = 0; n < 6; n++)
			commonsAlreadyChosen.add (null);		// Don't care what they are, just want the count

		final List<momime.common.database.v0_9_4.Spell> uncommonsAlreadyChosen = new ArrayList<momime.common.database.v0_9_4.Spell> ();
		for (int n = 0; n < 2; n++)
			uncommonsAlreadyChosen.add (null);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.getSpellsForRealmRankStatus (priv.getSpellResearchStatus (), "MB01", "SR01", SpellResearchStatusID.AVAILABLE, db)).thenReturn (commonsAlreadyChosen);
		when (spellUtils.getSpellsForRealmRankStatus (priv.getSpellResearchStatus (), "MB01", "SR02", SpellResearchStatusID.AVAILABLE, db)).thenReturn (uncommonsAlreadyChosen);
		
		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		
		// Run method
		final ChooseInitialSpellsNowMessage msg = utils.countFreeSpellsLeftToChoose (player, playerPick, db);
		
		// Check results
		assertEquals ("MB01", msg.getMagicRealmID ());
		assertEquals (2, msg.getSpellRank ().size ());
		assertEquals ("SR01", msg.getSpellRank ().get (0).getSpellRankID ());
		assertEquals (4, msg.getSpellRank ().get (0).getFreeSpellCount ());
		assertEquals ("SR03", msg.getSpellRank ().get (1).getSpellRankID ());
		assertEquals (1, msg.getSpellRank ().get (1).getFreeSpellCount ());
	}
	
	/**
	 * Tests the findRealmIDWhereWeNeedToChooseFreeSpells method on a human player
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFindRealmIDWhereWeNeedToChooseFreeSpells_Human () throws Exception
	{
		// Mock the two types of pick and pick type details
		final Pick pick1 = new Pick ();
		pick1.setPickType ("X");

		final Pick pick2 = new Pick ();
		pick2.setPickType ("X");
		
		final PickType pickType = new PickType ();
		
		final PickTypeCountContainer sixPicks = new PickTypeCountContainer ();
		sixPicks.setCount (6);
		pickType.getPickTypeCount ().add (sixPicks);
		
		final PickTypeGrantsSpells common = new PickTypeGrantsSpells ();
		common.setSpellRank ("SR01");
		common.setSpellsFreeAtStart (4);
		sixPicks.getSpellCount ().add (common);

		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findPick ("MB01", "countFreeSpellsLeftToChoose")).thenReturn (pick1);
		when (db.findPick ("MB02", "countFreeSpellsLeftToChoose")).thenReturn (pick2);
		when (db.findPickType ("X", "countFreeSpellsLeftToChoose")).thenReturn (pickType);

		// Set up player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);
		
		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, priv, null, null);
		
		// We've got 6 of both types of book
		for (int n = 1; n <= 2; n++)
		{
			final PlayerPick playerPick = new PlayerPick ();
			playerPick.setPickID ("MB0" + n);
			playerPick.setQuantity (6);
			ppk.getPick ().add (playerPick);
		}

		// We already picked all 4 of the MB01 free spells, and 1 of the MB02 free spells
		final List<momime.common.database.v0_9_4.Spell> firstsAlreadyChosen = new ArrayList<momime.common.database.v0_9_4.Spell> ();
		for (int n = 0; n < 4; n++)
			firstsAlreadyChosen.add (null);		// Don't care what they are, just want the count

		final List<momime.common.database.v0_9_4.Spell> secondsAlreadyChosen = new ArrayList<momime.common.database.v0_9_4.Spell> ();
		secondsAlreadyChosen.add (null);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.getSpellsForRealmRankStatus (priv.getSpellResearchStatus (), "MB01", "SR01", SpellResearchStatusID.AVAILABLE, db)).thenReturn (firstsAlreadyChosen);
		when (spellUtils.getSpellsForRealmRankStatus (priv.getSpellResearchStatus (), "MB02", "SR01", SpellResearchStatusID.AVAILABLE, db)).thenReturn (secondsAlreadyChosen);
		
		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		
		// Run method
		final ChooseInitialSpellsNowMessage msg = utils.findRealmIDWhereWeNeedToChooseFreeSpells (player, db);
		
		// Check results
		assertEquals ("MB02", msg.getMagicRealmID ());
		assertEquals (1, msg.getSpellRank ().size ());
		assertEquals ("SR01", msg.getSpellRank ().get (0).getSpellRankID ());
		assertEquals (3, msg.getSpellRank ().get (0).getFreeSpellCount ());
	}

	/**
	 * Tests the findRealmIDWhereWeNeedToChooseFreeSpells method when we've already chosen everything
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFindRealmIDWhereWeNeedToChooseFreeSpells_AllChosen () throws Exception
	{
		// Mock the two types of pick and pick type details
		final Pick pick1 = new Pick ();
		pick1.setPickType ("X");

		final Pick pick2 = new Pick ();
		pick2.setPickType ("X");
		
		final PickType pickType = new PickType ();
		
		final PickTypeCountContainer sixPicks = new PickTypeCountContainer ();
		sixPicks.setCount (6);
		pickType.getPickTypeCount ().add (sixPicks);
		
		final PickTypeGrantsSpells common = new PickTypeGrantsSpells ();
		common.setSpellRank ("SR01");
		common.setSpellsFreeAtStart (4);
		sixPicks.getSpellCount ().add (common);

		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findPick ("MB01", "countFreeSpellsLeftToChoose")).thenReturn (pick1);
		when (db.findPick ("MB02", "countFreeSpellsLeftToChoose")).thenReturn (pick2);
		when (db.findPickType ("X", "countFreeSpellsLeftToChoose")).thenReturn (pickType);

		// Set up player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);
		
		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, priv, null, null);
		
		// We've got 6 of both types of book
		for (int n = 1; n <= 2; n++)
		{
			final PlayerPick playerPick = new PlayerPick ();
			playerPick.setPickID ("MB0" + n);
			playerPick.setQuantity (6);
			ppk.getPick ().add (playerPick);
		}

		// We already picked all 4 of both types of spells
		final List<momime.common.database.v0_9_4.Spell> firstsAlreadyChosen = new ArrayList<momime.common.database.v0_9_4.Spell> ();
		for (int n = 0; n < 4; n++)
			firstsAlreadyChosen.add (null);		// Don't care what they are, just want the count

		final List<momime.common.database.v0_9_4.Spell> secondsAlreadyChosen = new ArrayList<momime.common.database.v0_9_4.Spell> ();
		for (int n = 0; n < 4; n++)
			secondsAlreadyChosen.add (null);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.getSpellsForRealmRankStatus (priv.getSpellResearchStatus (), "MB01", "SR01", SpellResearchStatusID.AVAILABLE, db)).thenReturn (firstsAlreadyChosen);
		when (spellUtils.getSpellsForRealmRankStatus (priv.getSpellResearchStatus (), "MB02", "SR01", SpellResearchStatusID.AVAILABLE, db)).thenReturn (secondsAlreadyChosen);
		
		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		
		// Run method
		final ChooseInitialSpellsNowMessage msg = utils.findRealmIDWhereWeNeedToChooseFreeSpells (player, db);
		
		// Check results
		assertNull (msg);
	}
	
	/**
	 * Tests the findRealmIDWhereWeNeedToChooseFreeSpells method on an AI player
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFindRealmIDWhereWeNeedToChooseFreeSpells_AI () throws Exception
	{
		// Mock the two types of pick and pick type details
		final Pick pick1 = new Pick ();
		pick1.setPickType ("X");

		final Pick pick2 = new Pick ();
		pick2.setPickType ("X");
		
		final PickType pickType = new PickType ();
		
		final PickTypeCountContainer sixPicks = new PickTypeCountContainer ();
		sixPicks.setCount (6);
		pickType.getPickTypeCount ().add (sixPicks);
		
		final PickTypeGrantsSpells common = new PickTypeGrantsSpells ();
		common.setSpellRank ("SR01");
		common.setSpellsFreeAtStart (4);
		sixPicks.getSpellCount ().add (common);

		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findPick ("MB01", "countFreeSpellsLeftToChoose")).thenReturn (pick1);
		when (db.findPick ("MB02", "countFreeSpellsLeftToChoose")).thenReturn (pick2);
		when (db.findPickType ("X", "countFreeSpellsLeftToChoose")).thenReturn (pickType);

		// Set up player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (-1);
		pd.setHuman (false);
		pd.setPlayerName ("Name");
		
		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, priv, null, null);
		
		// We've got 6 of both types of book
		for (int n = 1; n <= 2; n++)
		{
			final PlayerPick playerPick = new PlayerPick ();
			playerPick.setPickID ("MB0" + n);
			playerPick.setQuantity (6);
			ppk.getPick ().add (playerPick);
		}

		// We already picked all 4 of the MB01 free spells, and 1 of the MB02 free spells
		final List<momime.common.database.v0_9_4.Spell> firstsAlreadyChosen = new ArrayList<momime.common.database.v0_9_4.Spell> ();
		for (int n = 0; n < 4; n++)
			firstsAlreadyChosen.add (null);		// Don't care what they are, just want the count

		final List<momime.common.database.v0_9_4.Spell> secondsAlreadyChosen = new ArrayList<momime.common.database.v0_9_4.Spell> ();
		secondsAlreadyChosen.add (null);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.getSpellsForRealmRankStatus (priv.getSpellResearchStatus (), "MB01", "SR01", SpellResearchStatusID.AVAILABLE, db)).thenReturn (firstsAlreadyChosen);
		when (spellUtils.getSpellsForRealmRankStatus (priv.getSpellResearchStatus (), "MB02", "SR01", SpellResearchStatusID.AVAILABLE, db)).thenReturn (secondsAlreadyChosen);
		
		// Set up object to test
		final SpellAI spellAI = mock (SpellAI.class);
		final SpellResearchStatus spell1 = new SpellResearchStatus ();
		final SpellResearchStatus spell2 = new SpellResearchStatus ();
		final SpellResearchStatus spell3 = new SpellResearchStatus ();
		when (spellAI.chooseFreeSpellAI (priv.getSpellResearchStatus (), "MB02", "SR01", "Name", db)).thenReturn (spell1, spell2, spell3);
		
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		utils.setSpellAI (spellAI);
		
		// Run method
		final ChooseInitialSpellsNowMessage msg = utils.findRealmIDWhereWeNeedToChooseFreeSpells (player, db);
		
		// Check results - it *does* still return the "message" for AI players, so the calling routine knows whether it chose spells or had nothing to do
		assertEquals ("MB02", msg.getMagicRealmID ());
		assertEquals (1, msg.getSpellRank ().size ());
		assertEquals ("SR01", msg.getSpellRank ().get (0).getSpellRankID ());
		assertEquals (3, msg.getSpellRank ().get (0).getFreeSpellCount ());
		
		assertEquals (SpellResearchStatusID.AVAILABLE, spell1.getStatus ());
		assertEquals (SpellResearchStatusID.AVAILABLE, spell2.getStatus ());
		assertEquals (SpellResearchStatusID.AVAILABLE, spell3.getStatus ());
	}

	/**
	 * Tests the validateInitialSpellSelection method on a valid selection
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateInitialSpellSelection () throws Exception
	{
		// Mock the pick and pick type details
		final Pick pick = new Pick ();
		pick.setPickType ("X");
		
		final PickType pickType = new PickType ();
		
		final PickTypeCountContainer sixPicks = new PickTypeCountContainer ();
		sixPicks.setCount (6);
		pickType.getPickTypeCount ().add (sixPicks);
		
		final PickTypeGrantsSpells common = new PickTypeGrantsSpells ();
		common.setSpellRank ("SR01");
		common.setSpellsFreeAtStart (4);
		sixPicks.getSpellCount ().add (common);
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findPick ("MB01", "countFreeSpellsLeftToChoose")).thenReturn (pick);
		when (db.findPickType ("X", "countFreeSpellsLeftToChoose")).thenReturn (pickType);
		
		for (int n = 1; n <= 4; n++)
		{
			final Spell spell = new Spell ();
			spell.setSpellRealm ("MB01");
			spell.setSpellRank ("SR01");
			
			when (db.findSpell ("SP00" + n, "validateInitialSpellSelection")).thenReturn (spell);
		}
		
		// Set up player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);
		
		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, priv, null, null);
		
		// Picks we have
		final PlayerPick playerPick = new PlayerPick ();
		playerPick.setPickID ("MB01");
		playerPick.setQuantity (6);
		ppk.getPick ().add (playerPick);
		
		// Spells we have
		final SpellUtils spellUtils = mock (SpellUtils.class);
		
		// Spells we're trying to choose
		final List<String> spellIDs = new ArrayList<String> ();
		for (int n = 1; n <= 3; n++)
			spellIDs.add ("SP00" + n);

		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		
		// Run method
		assertNull (utils.validateInitialSpellSelection (player, "MB01", spellIDs, db));
	}

	/**
	 * Tests the validateInitialSpellSelection method trying to pick free spells in a magic realm that we have no books in
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateInitialSpellSelection_NoBooks () throws Exception
	{
		// Mock the pick and pick type details
		final Pick pick = new Pick ();
		pick.setPickType ("X");
		
		final PickType pickType = new PickType ();
		
		final PickTypeCountContainer sixPicks = new PickTypeCountContainer ();
		sixPicks.setCount (6);
		pickType.getPickTypeCount ().add (sixPicks);
		
		final PickTypeGrantsSpells common = new PickTypeGrantsSpells ();
		common.setSpellRank ("SR01");
		common.setSpellsFreeAtStart (4);
		sixPicks.getSpellCount ().add (common);
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findPick ("MB01", "countFreeSpellsLeftToChoose")).thenReturn (pick);
		when (db.findPickType ("X", "countFreeSpellsLeftToChoose")).thenReturn (pickType);
		
		for (int n = 1; n <= 4; n++)
		{
			final Spell spell = new Spell ();
			spell.setSpellRealm ("MB01");
			spell.setSpellRank ("SR01");
			
			when (db.findSpell ("SP00" + n, "validateInitialSpellSelection")).thenReturn (spell);
		}
		
		// Set up player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);
		
		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, priv, null, null);
		
		// Picks we have
		final PlayerPick playerPick = new PlayerPick ();
		playerPick.setPickID ("MB01");
		playerPick.setQuantity (6);
		ppk.getPick ().add (playerPick);
		
		// Spells we have
		final SpellUtils spellUtils = mock (SpellUtils.class);
		
		// Spells we're trying to choose
		final List<String> spellIDs = new ArrayList<String> ();
		for (int n = 1; n <= 3; n++)
			spellIDs.add ("SP00" + n);

		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		
		// Run method
		assertNotNull (utils.validateInitialSpellSelection (player, "MB02", spellIDs, db));		// <--
	}

	/**
	 * Tests the validateInitialSpellSelection method where the number of books we have doesn't grant any free spells
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateInitialSpellSelection_NotEnoughBooks () throws Exception
	{
		// Mock the pick and pick type details
		final Pick pick = new Pick ();
		pick.setPickType ("X");
		
		final PickType pickType = new PickType ();		// <--- Didn't set 6 picks to actually give any free spells
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findPick ("MB01", "countFreeSpellsLeftToChoose")).thenReturn (pick);
		when (db.findPickType ("X", "countFreeSpellsLeftToChoose")).thenReturn (pickType);
		
		for (int n = 1; n <= 4; n++)
		{
			final Spell spell = new Spell ();
			spell.setSpellRealm ("MB01");
			spell.setSpellRank ("SR01");
			
			when (db.findSpell ("SP00" + n, "validateInitialSpellSelection")).thenReturn (spell);
		}
		
		// Set up player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);
		
		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, priv, null, null);
		
		// Picks we have
		final PlayerPick playerPick = new PlayerPick ();
		playerPick.setPickID ("MB01");
		playerPick.setQuantity (6);
		ppk.getPick ().add (playerPick);
		
		// Spells we have
		final SpellUtils spellUtils = mock (SpellUtils.class);
		
		// Spells we're trying to choose
		final List<String> spellIDs = new ArrayList<String> ();
		for (int n = 1; n <= 3; n++)
			spellIDs.add ("SP00" + n);

		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		
		// Run method
		assertNotNull (utils.validateInitialSpellSelection (player, "MB01", spellIDs, db));
	}

	/**
	 * Tests the validateInitialSpellSelection method where we've already made all our free picks
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateInitialSpellSelection_AlreadyChosen () throws Exception
	{
		// Mock the pick and pick type details
		final Pick pick = new Pick ();
		pick.setPickType ("X");
		
		final PickType pickType = new PickType ();
		
		final PickTypeCountContainer sixPicks = new PickTypeCountContainer ();
		sixPicks.setCount (6);
		pickType.getPickTypeCount ().add (sixPicks);
		
		final PickTypeGrantsSpells common = new PickTypeGrantsSpells ();
		common.setSpellRank ("SR01");
		common.setSpellsFreeAtStart (4);
		sixPicks.getSpellCount ().add (common);
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findPick ("MB01", "countFreeSpellsLeftToChoose")).thenReturn (pick);
		when (db.findPickType ("X", "countFreeSpellsLeftToChoose")).thenReturn (pickType);
		
		for (int n = 1; n <= 4; n++)
		{
			final Spell spell = new Spell ();
			spell.setSpellRealm ("MB01");
			spell.setSpellRank ("SR01");
			
			when (db.findSpell ("SP00" + n, "validateInitialSpellSelection")).thenReturn (spell);
		}
		
		// Set up player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);
		
		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, priv, null, null);
		
		// Picks we have
		final PlayerPick playerPick = new PlayerPick ();
		playerPick.setPickID ("MB01");
		playerPick.setQuantity (6);
		ppk.getPick ().add (playerPick);
		
		// Spells we have
		final List<momime.common.database.v0_9_4.Spell> alreadyChosen = new ArrayList<momime.common.database.v0_9_4.Spell> ();
		for (int n = 0; n < 4; n++)
			alreadyChosen.add (null);		// Don't care what they are, just want the count

		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.getSpellsForRealmRankStatus (priv.getSpellResearchStatus (), "MB01", "SR01", SpellResearchStatusID.AVAILABLE, db)).thenReturn (alreadyChosen);
		
		// Spells we're trying to choose
		final List<String> spellIDs = new ArrayList<String> ();
		for (int n = 1; n <= 3; n++)
			spellIDs.add ("SP00" + n);

		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		
		// Run method
		assertNotNull (utils.validateInitialSpellSelection (player, "MB01", spellIDs, db));
	}

	/**
	 * Tests the validateInitialSpellSelection method when we're trying to choose too many spells
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateInitialSpellSelection_TooMany () throws Exception
	{
		// Mock the pick and pick type details
		final Pick pick = new Pick ();
		pick.setPickType ("X");
		
		final PickType pickType = new PickType ();
		
		final PickTypeCountContainer sixPicks = new PickTypeCountContainer ();
		sixPicks.setCount (6);
		pickType.getPickTypeCount ().add (sixPicks);
		
		final PickTypeGrantsSpells common = new PickTypeGrantsSpells ();
		common.setSpellRank ("SR01");
		common.setSpellsFreeAtStart (2);		// <---
		sixPicks.getSpellCount ().add (common);
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findPick ("MB01", "countFreeSpellsLeftToChoose")).thenReturn (pick);
		when (db.findPickType ("X", "countFreeSpellsLeftToChoose")).thenReturn (pickType);
		
		for (int n = 1; n <= 4; n++)
		{
			final Spell spell = new Spell ();
			spell.setSpellRealm ("MB01");
			spell.setSpellRank ("SR01");
			
			when (db.findSpell ("SP00" + n, "validateInitialSpellSelection")).thenReturn (spell);
		}
		
		// Set up player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);
		
		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, priv, null, null);
		
		// Picks we have
		final PlayerPick playerPick = new PlayerPick ();
		playerPick.setPickID ("MB01");
		playerPick.setQuantity (6);
		ppk.getPick ().add (playerPick);
		
		// Spells we have
		final SpellUtils spellUtils = mock (SpellUtils.class);
		
		// Spells we're trying to choose
		final List<String> spellIDs = new ArrayList<String> ();
		for (int n = 1; n <= 3; n++)
			spellIDs.add ("SP00" + n);

		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		
		// Run method
		assertNotNull (utils.validateInitialSpellSelection (player, "MB01", spellIDs, db));
	}

	/**
	 * Tests the validateInitialSpellSelection trying to choose spells of the wrong rank
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateInitialSpellSelection_WrongRank () throws Exception
	{
		// Mock the pick and pick type details
		final Pick pick = new Pick ();
		pick.setPickType ("X");
		
		final PickType pickType = new PickType ();
		
		final PickTypeCountContainer sixPicks = new PickTypeCountContainer ();
		sixPicks.setCount (6);
		pickType.getPickTypeCount ().add (sixPicks);
		
		final PickTypeGrantsSpells common = new PickTypeGrantsSpells ();
		common.setSpellRank ("SR01");
		common.setSpellsFreeAtStart (4);
		sixPicks.getSpellCount ().add (common);
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findPick ("MB01", "countFreeSpellsLeftToChoose")).thenReturn (pick);
		when (db.findPickType ("X", "countFreeSpellsLeftToChoose")).thenReturn (pickType);
		
		for (int n = 1; n <= 4; n++)
		{
			final Spell spell = new Spell ();
			spell.setSpellRealm ("MB01");
			spell.setSpellRank ((n == 3) ? "SR02" : "SR01");		// <---
			
			when (db.findSpell ("SP00" + n, "validateInitialSpellSelection")).thenReturn (spell);
		}
		
		// Set up player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);
		
		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, priv, null, null);
		
		// Picks we have
		final PlayerPick playerPick = new PlayerPick ();
		playerPick.setPickID ("MB01");
		playerPick.setQuantity (6);
		ppk.getPick ().add (playerPick);
		
		// Spells we have
		final SpellUtils spellUtils = mock (SpellUtils.class);
		
		// Spells we're trying to choose
		final List<String> spellIDs = new ArrayList<String> ();
		for (int n = 1; n <= 3; n++)
			spellIDs.add ("SP00" + n);

		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		
		// Run method
		assertNotNull (utils.validateInitialSpellSelection (player, "MB01", spellIDs, db));
	}

	/**
	 * Tests the validateInitialSpellSelection method trying to choose spells of the wrong realm
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateInitialSpellSelection_WrongRealm () throws Exception
	{
		// Mock the pick and pick type details
		final Pick pick = new Pick ();
		pick.setPickType ("X");
		
		final PickType pickType = new PickType ();
		
		final PickTypeCountContainer sixPicks = new PickTypeCountContainer ();
		sixPicks.setCount (6);
		pickType.getPickTypeCount ().add (sixPicks);
		
		final PickTypeGrantsSpells common = new PickTypeGrantsSpells ();
		common.setSpellRank ("SR01");
		common.setSpellsFreeAtStart (4);
		sixPicks.getSpellCount ().add (common);
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findPick ("MB01", "countFreeSpellsLeftToChoose")).thenReturn (pick);
		when (db.findPickType ("X", "countFreeSpellsLeftToChoose")).thenReturn (pickType);
		
		for (int n = 1; n <= 4; n++)
		{
			final Spell spell = new Spell ();
			spell.setSpellRealm ((n == 3) ? "MB02" : "MB01");		// <---
			spell.setSpellRank ("SR01");
			
			when (db.findSpell ("SP00" + n, "validateInitialSpellSelection")).thenReturn (spell);
		}
		
		// Set up player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);
		
		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, priv, null, null);
		
		// Picks we have
		final PlayerPick playerPick = new PlayerPick ();
		playerPick.setPickID ("MB01");
		playerPick.setQuantity (6);
		ppk.getPick ().add (playerPick);
		
		// Spells we have
		final SpellUtils spellUtils = mock (SpellUtils.class);
		
		// Spells we're trying to choose
		final List<String> spellIDs = new ArrayList<String> ();
		for (int n = 1; n <= 3; n++)
			spellIDs.add ("SP00" + n);

		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		
		// Run method
		assertNotNull (utils.validateInitialSpellSelection (player, "MB01", spellIDs, db));
	}
	
	/**
	 * Tests the validateRaceChoice method picking a race that doesn't even exist
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateRaceChoice_NotFound () throws Exception
	{
		// Mock race details
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);

		// Set up player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);
		
		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, null, null, null);
		
		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		
		// Run method
		assertNotNull (utils.validateRaceChoice (player, "X", db));
	}

	/**
	 * Tests the validateRaceChoice method picking a Myrran race when we do have the Myrran pick
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateRaceChoice_MyrranWithRetort () throws Exception
	{
		// Mock race details
		final Race race = new Race ();
		race.setNativePlane (1);
		
		final Plane arcanus = new Plane ();
		final Plane myrror = new Plane ();
		myrror.setPrerequisitePickToChooseNativeRace ("RT08");
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findRace ("RC01", "validateRaceChoice")).thenReturn (race);
		when (db.findPlane (0, "validateRaceChoice")).thenReturn (arcanus);
		when (db.findPlane (1, "validateRaceChoice")).thenReturn (myrror);

		// Set up player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);
		
		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, null, null, null);
		
		// Picks we have
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		when (playerPickUtils.getQuantityOfPick (ppk.getPick (), "RT08")).thenReturn (1);
		
		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		utils.setPlayerPickUtils (playerPickUtils);
		
		// Run method
		assertNull (utils.validateRaceChoice (player, "RC01", db));
	}

	/**
	 * Tests the validateRaceChoice method picking a Myrran race when we don't have the Myrran pick
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateRaceChoice_MyrranWithoutRetort () throws Exception
	{
		// Mock race details
		final Race race = new Race ();
		race.setNativePlane (1);
		
		final Plane arcanus = new Plane ();
		final Plane myrror = new Plane ();
		myrror.setPrerequisitePickToChooseNativeRace ("RT08");
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findRace ("RC01", "validateRaceChoice")).thenReturn (race);
		when (db.findPlane (0, "validateRaceChoice")).thenReturn (arcanus);
		when (db.findPlane (1, "validateRaceChoice")).thenReturn (myrror);

		// Set up player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);
		
		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, null, null, null);
		
		// Picks we have
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		
		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		utils.setPlayerPickUtils (playerPickUtils);
		
		// Run method
		assertNotNull (utils.validateRaceChoice (player, "RC01", db));
	}

	/**
	 * Tests the validateRaceChoice method picking an Arcanian race when we do have the Myrran pick
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateRaceChoice_ArcanusWithRetort () throws Exception
	{
		// Mock race details
		final Race race = new Race ();
		race.setNativePlane (0);
		
		final Plane arcanus = new Plane ();
		final Plane myrror = new Plane ();
		myrror.setPrerequisitePickToChooseNativeRace ("RT08");
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findRace ("RC01", "validateRaceChoice")).thenReturn (race);
		when (db.findPlane (0, "validateRaceChoice")).thenReturn (arcanus);
		when (db.findPlane (1, "validateRaceChoice")).thenReturn (myrror);

		// Set up player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);
		
		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, null, null, null);
		
		// Picks we have
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		when (playerPickUtils.getQuantityOfPick (ppk.getPick (), "RT08")).thenReturn (1);
		
		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		utils.setPlayerPickUtils (playerPickUtils);
		
		// Run method
		assertNull (utils.validateRaceChoice (player, "RC01", db));
	}

	/**
	 * Tests the validateRaceChoice method picking an Arcanian race when we don't have the Myrran pick
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateRaceChoice_ArcanusWithoutRetort () throws Exception
	{
		// Mock race details
		final Race race = new Race ();
		race.setNativePlane (0);
		
		final Plane arcanus = new Plane ();
		final Plane myrror = new Plane ();
		myrror.setPrerequisitePickToChooseNativeRace ("RT08");
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findRace ("RC01", "validateRaceChoice")).thenReturn (race);
		when (db.findPlane (0, "validateRaceChoice")).thenReturn (arcanus);
		when (db.findPlane (1, "validateRaceChoice")).thenReturn (myrror);

		// Set up player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);
		
		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, null, null, null);
		
		// Picks we have
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		
		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		utils.setPlayerPickUtils (playerPickUtils);
		
		// Run method
		assertNull (utils.validateRaceChoice (player, "RC01", db));
	}

	/**
	 * Tests the hasChosenAllDetails method where we've chosen a standard wizard
	 */
	@Test
	public final void testHasChosenAllDetails_Standard ()
	{
		// Set up player details
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPrivateKnowledge priv = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, null, null, priv);
		
		// Make selections
		ppk.setWizardID ("WZ01");
		priv.setFirstCityRaceID ("RC01");

		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		
		// Run method
		assertTrue (utils.hasChosenAllDetails (player));
	}

	/**
	 * Tests the hasChosenAllDetails method where we've chosen a custom wizard
	 */
	@Test
	public final void testHasChosenAllDetails_Custom ()
	{
		// Set up player details
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPrivateKnowledge priv = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, null, null, priv);
		
		// Make selections
		ppk.setWizardID ("");
		priv.setFirstCityRaceID ("RC01");
		priv.setCustomPicksChosen (true);

		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		
		// Run method
		assertTrue (utils.hasChosenAllDetails (player));
	}

	/**
	 * Tests the hasChosenAllDetails method where we've chosen a custom wizard but didn't make custom picks yet
	 */
	@Test
	public final void testHasChosenAllDetails_CustomNotChosen ()
	{
		// Set up player details
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPrivateKnowledge priv = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, null, null, priv);
		
		// Make selections
		ppk.setWizardID ("");
		priv.setFirstCityRaceID ("RC01");

		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		
		// Run method
		assertFalse (utils.hasChosenAllDetails (player));
	}

	/**
	 * Tests the hasChosenAllDetails method where we've not chosen a wizard
	 */
	@Test
	public final void testHasChosenAllDetails_NoWizard ()
	{
		// Set up player details
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPrivateKnowledge priv = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, null, null, priv);
		
		// Make selections
		priv.setFirstCityRaceID ("RC01");

		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		
		// Run method
		assertFalse (utils.hasChosenAllDetails (player));
	}
	
	/**
	 * Tests the hasChosenAllDetails method where we've not chosen a race
	 */
	@Test
	public final void testHasChosenAllDetails_NoRace ()
	{
		// Set up player details
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPrivateKnowledge priv = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, null, null, priv);
		
		// Make selections
		ppk.setWizardID ("WZ01");

		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		
		// Run method
		assertFalse (utils.hasChosenAllDetails (player));
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

		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		
		// Single player game just against raiders, and nothing chosen yet
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPrivateKnowledge priv = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, null, null, priv);
		players.add (player);

		assertFalse (utils.allPlayersHaveChosenAllDetails (players, sd));

		// Fill in details
		ppk.setWizardID ("WZ01");
		priv.setFirstCityRaceID ("RC01");
		assertTrue (utils.allPlayersHaveChosenAllDetails (players, sd));

		// Add an AI player - this doesn't stop the game starting, since AI players are added after
		sd.setMaxPlayers (4);
		sd.setAiPlayerCount (1);
		assertTrue (utils.allPlayersHaveChosenAllDetails (players, sd));

		// Add slot for 2nd player
		sd.setMaxPlayers (5);
		assertFalse (utils.allPlayersHaveChosenAllDetails (players, sd));

		// Add second player
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (3);
		pd2.setHuman (true);

		final MomPersistentPlayerPublicKnowledge ppk2 = new MomPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPrivateKnowledge priv2 = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, ppk2, null, null, priv2);
		players.add (player2);

		assertFalse (utils.allPlayersHaveChosenAllDetails (players, sd));

		// Fill in second player details
		ppk2.setWizardID ("WZ02");
		priv2.setFirstCityRaceID ("RC02");
		assertTrue (utils.allPlayersHaveChosenAllDetails (players, sd));
	}

	/**
	 * Tests the listWizardsForAIPlayers method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testListWizardsForAIPlayers () throws Exception
	{
		// Mock 9 wizards in DB
		final List<Wizard> availableWizards = new ArrayList<Wizard> ();
		for (int n = 1; n <= 9; n++)
		{
			final Wizard wizard = new Wizard ();
			wizard.setWizardID ("WZ0" + n);
			availableWizards.add (wizard);
		}

		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.getWizard ()).thenReturn (availableWizards);

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
		ppk2.setStandardPhotoID ("WZ08");
		players.add (new PlayerServerDetails (pd2, ppk2, null, null, null));

		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		
		// Run test
		final List<Wizard> wizardIDs = utils.listWizardsForAIPlayers (players, db);
		assertEquals (7, wizardIDs.size ());
		assertEquals ("WZ01", wizardIDs.get (0).getWizardID ());
		assertEquals ("WZ03", wizardIDs.get (1).getWizardID ());
		assertEquals ("WZ04", wizardIDs.get (2).getWizardID ());
		assertEquals ("WZ05", wizardIDs.get (3).getWizardID ());
		assertEquals ("WZ06", wizardIDs.get (4).getWizardID ());
		assertEquals ("WZ07", wizardIDs.get (5).getWizardID ());
		assertEquals ("WZ09", wizardIDs.get (6).getWizardID ());
	}

	/**
	 * Tests the startingPlaneForWizard method when the player doesn't have the Myrran retort
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testStartingPlaneForWizard_Arcanus () throws Exception
	{
		// Set up pick details
		final Plane arcanus = new Plane ();
		final Plane myrror = new Plane ();
		myrror.setPrerequisitePickToChooseNativeRace ("RT08");
		myrror.setPlaneNumber (1);
		
		final List<Plane> planes = new ArrayList<Plane> ();
		planes.add (arcanus);
		planes.add (myrror);

		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.getPlane ()).thenReturn (planes);

		// Picks we have
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		
		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		utils.setPlayerPickUtils (playerPickUtils);

		// Run method
		assertEquals (0, utils.startingPlaneForWizard (picks, db));
	}
	
	/**
	 * Tests the startingPlaneForWizard method when the player does have the Myrran retort
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testStartingPlaneForWizard_Myrran () throws Exception
	{
		// Set up pick details
		final Plane arcanus = new Plane ();
		final Plane myrror = new Plane ();
		myrror.setPrerequisitePickToChooseNativeRace ("RT08");
		myrror.setPlaneNumber (1);
		
		final List<Plane> planes = new ArrayList<Plane> ();
		planes.add (arcanus);
		planes.add (myrror);

		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.getPlane ()).thenReturn (planes);

		// Picks we have
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		
		when (playerPickUtils.getQuantityOfPick (picks, "RT08")).thenReturn (1);
		
		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		utils.setPlayerPickUtils (playerPickUtils);

		// Run method
		assertEquals (1, utils.startingPlaneForWizard (picks, db));
	}

	/**
	 * Tests the chooseRandomRaceForPlane method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testChooseRandomRaceForPlane () throws Exception
	{
		// Mock some races, 6 on arcanus, 3 on myrror
		final List<Race> races = new ArrayList<Race> ();
		for (int n = 1; n <= 9; n++)
		{
			final Race race = new Race ();
			race.setRaceID ("RC0" + n);
			race.setNativePlane ((n <= 6) ? 0 : 1);
			races.add (race);
		}
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.getRace ()).thenReturn (races);
			
		// Fix results
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (6)).thenReturn (3);		// Arcanus race
		when (random.nextInt (3)).thenReturn (1);		// Myrror race
		
		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		utils.setRandomUtils (random);

		// Run test
		assertEquals ("RC04", utils.chooseRandomRaceForPlane (0, db));
		assertEquals ("RC08", utils.chooseRandomRaceForPlane (1, db));
	}
}
