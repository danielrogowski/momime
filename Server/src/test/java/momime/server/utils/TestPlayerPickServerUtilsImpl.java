package momime.server.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.multiplayer.server.ServerToClientSessionConnection;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.random.RandomUtils;

import momime.common.database.CommonDatabase;
import momime.common.database.Pick;
import momime.common.database.PickAndQuantity;
import momime.common.database.PickType;
import momime.common.database.PickTypeCountContainer;
import momime.common.database.PickTypeGrantsSpells;
import momime.common.database.Plane;
import momime.common.database.RaceEx;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.WizardEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.PlayerPick;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;
import momime.common.messages.servertoclient.ChooseInitialSpellsNowMessage;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.PlayerKnowledgeUtils;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.SpellUtils;
import momime.server.MomSessionVariables;
import momime.server.ai.SpellAI;
import momime.server.messages.MomGeneralServerKnowledge;

/**
 * Tests the PlayerPickServerUtilsImpl class
 */
@ExtendWith(MockitoExtension.class)
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
		final CommonDatabase db = mock (CommonDatabase.class);
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
	 * Tests the findWizardUsingStandardPhoto method on a standard photo that is in the list
	 */
	@Test
	public final void testFindWizardUsingStandardPhoto_Exists ()
	{
		final List<KnownWizardDetails> wizards = new ArrayList<KnownWizardDetails> ();
		for (int n = 1; n <= 9; n++)
		{
			final KnownWizardDetails wizardDetails = new KnownWizardDetails ();
			wizardDetails.setStandardPhotoID ("WZ0" + n);

			wizards.add (wizardDetails);
		}

		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		
		// Run test
		final KnownWizardDetails wizard = utils.findWizardUsingStandardPhoto (wizards, "WZ04");
		assertEquals ("WZ04", wizard.getStandardPhotoID ());
	}

	/**
	 * Tests the findWizardUsingStandardPhoto method on a standard photo that isn't in the list
	 */
	@Test
	public final void testFindWizardUsingStandardPhoto_NotExists ()
	{
		final List<KnownWizardDetails> wizards = new ArrayList<KnownWizardDetails> ();
		for (int n = 1; n <= 9; n++)
		{
			final KnownWizardDetails wizardDetails = new KnownWizardDetails ();
			wizardDetails.setStandardPhotoID ("WZ0" + n);

			wizards.add (wizardDetails);
		}

		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		
		// Run test
		assertNull (utils.findWizardUsingStandardPhoto (wizards, "WZ10"));
	}

	/**
	 * Tests the validateCustomPicks method, trying to select custom picks when we didn't choose a wizard yet
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCustomPicks_DidntPickWizardYet () throws Exception
	{
		// Set up player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPrivateKnowledge priv = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, null, null, priv);

		// Wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);
		
		final KnownWizardDetails wizardDetails = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), pd.getPlayerID (), "validateCustomPicks")).thenReturn (wizardDetails);
		
		// Create requested picks list
		final List<PickAndQuantity> picks = new ArrayList<PickAndQuantity> ();
		
		final PickAndQuantity pick1 = new PickAndQuantity ();
		pick1.setPickID ("MB02");
		pick1.setQuantity (4);
		picks.add (pick1);
		
		final PickAndQuantity pick2 = new PickAndQuantity ();
		pick2.setPickID ("MB03");
		pick2.setQuantity (1);
		picks.add (pick2);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		utils.setKnownWizardUtils (knownWizardUtils);
		
		// Check results
		assertNotNull (utils.validateCustomPicks (player, picks, 11, mom));
	}

	/**
	 * Tests the validateCustomPicks method, trying to select custom picks when we picked a standard wizard
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCustomPicks_PickedStandardWizard () throws Exception
	{
		// Set up player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);

		final MomTransientPlayerPrivateKnowledge priv = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, priv);

		// Wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);
		
		final KnownWizardDetails wizardDetails = new KnownWizardDetails ();
		wizardDetails.setWizardID ("WZ01");
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), pd.getPlayerID (), "validateCustomPicks")).thenReturn (wizardDetails);
		
		// Create requested picks list
		final List<PickAndQuantity> picks = new ArrayList<PickAndQuantity> ();
		
		final PickAndQuantity pick1 = new PickAndQuantity ();
		pick1.setPickID ("MB02");
		pick1.setQuantity (4);
		picks.add (pick1);
		
		final PickAndQuantity pick2 = new PickAndQuantity ();
		pick2.setPickID ("MB03");
		pick2.setQuantity (1);
		picks.add (pick2);

		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		utils.setKnownWizardUtils (knownWizardUtils);
		
		// Check results
		assertNotNull (utils.validateCustomPicks (player, picks, 11, mom));
	}

	/**
	 * Tests the validateCustomPicks method, trying to select custom picks but we didn't pick enough
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCustomPicks_PickedNotEnough () throws Exception
	{
		// Mock some types of pick
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Pick pick = new Pick ();
		pick.setPickID ("MB02");
		pick.setPickCost (2);
		when (db.findPick ("MB02", "validateCustomPicks")).thenReturn (pick);

		// Set up player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);
		
		final MomTransientPlayerPrivateKnowledge priv = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, priv);

		// Wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);
		
		final KnownWizardDetails wizardDetails = new KnownWizardDetails ();
		wizardDetails.setWizardID ("");
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), pd.getPlayerID (), "validateCustomPicks")).thenReturn (wizardDetails);
		
		// Create requested picks list
		final List<PickAndQuantity> picks = new ArrayList<PickAndQuantity> ();
		
		final PickAndQuantity pick1 = new PickAndQuantity ();
		pick1.setPickID ("MB02");
		pick1.setQuantity (4);
		picks.add (pick1);

		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getServerDB ()).thenReturn (db);
		
		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		utils.setKnownWizardUtils (knownWizardUtils);
		
		// Check results
		assertNotNull (utils.validateCustomPicks (player, picks, 11, mom));
	}
	
	/**
	 * Tests the validateCustomPicks method, trying to select custom picks but we picked too many
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCustomPicks_PickedTooMany () throws Exception
	{
		// Mock some types of pick
		final CommonDatabase db = mock (CommonDatabase.class);
		for (int n = 2; n <= 3; n++)
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

		final MomTransientPlayerPrivateKnowledge priv = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, priv);

		// Wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);
		
		final KnownWizardDetails wizardDetails = new KnownWizardDetails ();
		wizardDetails.setWizardID ("");
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), pd.getPlayerID (), "validateCustomPicks")).thenReturn (wizardDetails);
		
		// Create requested picks list
		final List<PickAndQuantity> picks = new ArrayList<PickAndQuantity> ();
		
		final PickAndQuantity pick1 = new PickAndQuantity ();
		pick1.setPickID ("MB02");
		pick1.setQuantity (4);
		picks.add (pick1);
		
		final PickAndQuantity pick2 = new PickAndQuantity ();
		pick2.setPickID ("MB03");
		pick2.setQuantity (2);
		picks.add (pick2);

		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getServerDB ()).thenReturn (db);
		
		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		utils.setKnownWizardUtils (knownWizardUtils);
		
		// Check results
		assertNotNull (utils.validateCustomPicks (player, picks, 11, mom));
	}
	
	/**
	 * Tests the validateCustomPicks method, trying to select custom picks and everything is fine
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateCustomPicks () throws Exception
	{
		// Mock some types of pick
		final CommonDatabase db = mock (CommonDatabase.class);
		for (int n = 2; n <= 3; n++)
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

		final MomTransientPlayerPrivateKnowledge priv = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, priv);

		// Wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);
		
		final KnownWizardDetails wizardDetails = new KnownWizardDetails ();
		wizardDetails.setWizardID ("");
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), pd.getPlayerID (), "validateCustomPicks")).thenReturn (wizardDetails);
		
		// Create requested picks list
		final List<PickAndQuantity> picks = new ArrayList<PickAndQuantity> ();
		
		final PickAndQuantity pick1 = new PickAndQuantity ();
		pick1.setPickID ("MB02");
		pick1.setQuantity (4);
		picks.add (pick1);
		
		final PickAndQuantity pick2 = new PickAndQuantity ();
		pick2.setPickID ("MB03");
		pick2.setQuantity (1);
		picks.add (pick2);

		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getServerDB ()).thenReturn (db);
		
		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		utils.setKnownWizardUtils (knownWizardUtils);
		
		// Check results
		assertNull (utils.validateCustomPicks (player, picks, 11, mom));
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
		
		final CommonDatabase db = mock (CommonDatabase.class);
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
		
		final CommonDatabase db = mock (CommonDatabase.class);
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
		final List<momime.common.database.Spell> commonsAlreadyChosen = new ArrayList<momime.common.database.Spell> ();
		for (int n = 0; n < 6; n++)
			commonsAlreadyChosen.add (null);		// Don't care what they are, just want the count

		final List<momime.common.database.Spell> uncommonsAlreadyChosen = new ArrayList<momime.common.database.Spell> ();
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

		final CommonDatabase db = mock (CommonDatabase.class);
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
		final List<momime.common.database.Spell> firstsAlreadyChosen = new ArrayList<momime.common.database.Spell> ();
		for (int n = 0; n < 4; n++)
			firstsAlreadyChosen.add (null);		// Don't care what they are, just want the count

		final List<momime.common.database.Spell> secondsAlreadyChosen = new ArrayList<momime.common.database.Spell> ();
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

		final CommonDatabase db = mock (CommonDatabase.class);
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
		final List<momime.common.database.Spell> firstsAlreadyChosen = new ArrayList<momime.common.database.Spell> ();
		for (int n = 0; n < 4; n++)
			firstsAlreadyChosen.add (null);		// Don't care what they are, just want the count

		final List<momime.common.database.Spell> secondsAlreadyChosen = new ArrayList<momime.common.database.Spell> ();
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

		final CommonDatabase db = mock (CommonDatabase.class);
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
		final List<momime.common.database.Spell> firstsAlreadyChosen = new ArrayList<momime.common.database.Spell> ();
		for (int n = 0; n < 4; n++)
			firstsAlreadyChosen.add (null);		// Don't care what they are, just want the count

		final List<momime.common.database.Spell> secondsAlreadyChosen = new ArrayList<momime.common.database.Spell> ();
		secondsAlreadyChosen.add (null);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.getSpellsForRealmRankStatus (priv.getSpellResearchStatus (), "MB01", "SR01", SpellResearchStatusID.AVAILABLE, db)).thenReturn (firstsAlreadyChosen);
		when (spellUtils.getSpellsForRealmRankStatus (priv.getSpellResearchStatus (), "MB02", "SR01", SpellResearchStatusID.AVAILABLE, db)).thenReturn (secondsAlreadyChosen);
		
		// Set up object to test
		final SpellAI spellAI = mock (SpellAI.class);
		final SpellResearchStatus spell1 = new SpellResearchStatus ();
		final SpellResearchStatus spell2 = new SpellResearchStatus ();
		final SpellResearchStatus spell3 = new SpellResearchStatus ();
		when (spellAI.chooseFreeSpellAI (priv.getSpellResearchStatus (), "MB02", "SR01", db)).thenReturn (spell1, spell2, spell3);
		
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
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findPick ("MB01", "countFreeSpellsLeftToChoose")).thenReturn (pick);
		when (db.findPickType ("X", "countFreeSpellsLeftToChoose")).thenReturn (pickType);
		
		for (int n = 1; n <= 3; n++)
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
		
		final CommonDatabase db = mock (CommonDatabase.class);
		
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
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findPick ("MB01", "countFreeSpellsLeftToChoose")).thenReturn (pick);
		when (db.findPickType ("X", "countFreeSpellsLeftToChoose")).thenReturn (pickType);
		
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
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findPick ("MB01", "countFreeSpellsLeftToChoose")).thenReturn (pick);
		when (db.findPickType ("X", "countFreeSpellsLeftToChoose")).thenReturn (pickType);
		
		final Spell spell = new Spell ();
		spell.setSpellRealm ("MB01");
		spell.setSpellRank ("SR01");
		
		when (db.findSpell ("SP001", "validateInitialSpellSelection")).thenReturn (spell);
		
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
		final List<momime.common.database.Spell> alreadyChosen = new ArrayList<momime.common.database.Spell> ();
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
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findPick ("MB01", "countFreeSpellsLeftToChoose")).thenReturn (pick);
		when (db.findPickType ("X", "countFreeSpellsLeftToChoose")).thenReturn (pickType);
		
		for (int n = 1; n <= 3; n++)
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
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findPick ("MB01", "countFreeSpellsLeftToChoose")).thenReturn (pick);
		when (db.findPickType ("X", "countFreeSpellsLeftToChoose")).thenReturn (pickType);
		
		for (int n = 1; n <= 3; n++)
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
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findPick ("MB01", "countFreeSpellsLeftToChoose")).thenReturn (pick);
		when (db.findPickType ("X", "countFreeSpellsLeftToChoose")).thenReturn (pickType);
		
		for (int n = 1; n <= 3; n++)
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
		final CommonDatabase db = mock (CommonDatabase.class);

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
		final RaceEx race = new RaceEx ();
		race.setNativePlane (1);
		
		final Plane myrror = new Plane ();
		myrror.setPrerequisitePickToChooseNativeRace ("RT08");
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findRace ("RC01", "validateRaceChoice")).thenReturn (race);
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
		final RaceEx race = new RaceEx ();
		race.setNativePlane (1);
		
		final Plane myrror = new Plane ();
		myrror.setPrerequisitePickToChooseNativeRace ("RT08");
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findRace ("RC01", "validateRaceChoice")).thenReturn (race);
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
		final RaceEx race = new RaceEx ();
		race.setNativePlane (0);
		
		final Plane arcanus = new Plane ();
		final Plane myrror = new Plane ();
		myrror.setPrerequisitePickToChooseNativeRace ("RT08");
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findRace ("RC01", "validateRaceChoice")).thenReturn (race);
		when (db.findPlane (0, "validateRaceChoice")).thenReturn (arcanus);

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
	 * Tests the validateRaceChoice method picking an Arcanian race when we don't have the Myrran pick
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateRaceChoice_ArcanusWithoutRetort () throws Exception
	{
		// Mock race details
		final RaceEx race = new RaceEx ();
		race.setNativePlane (0);
		
		final Plane arcanus = new Plane ();
		final Plane myrror = new Plane ();
		myrror.setPrerequisitePickToChooseNativeRace ("RT08");
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findRace ("RC01", "validateRaceChoice")).thenReturn (race);
		when (db.findPlane (0, "validateRaceChoice")).thenReturn (arcanus);

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
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHasChosenAllDetails_Standard () throws Exception
	{
		// Set up player details
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPrivateKnowledge priv = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, null, null, priv);
		
		// Wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);
		
		final KnownWizardDetails wizardDetails = new KnownWizardDetails ();
		wizardDetails.setWizardID ("WZ01");
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), pd.getPlayerID ())).thenReturn (wizardDetails);
		
		// Make selections
		priv.setFirstCityRaceID ("RC01");

		// Has wizard been chosen?
		final PlayerKnowledgeUtils playerKnowledgeUtils = mock (PlayerKnowledgeUtils.class);
		when (playerKnowledgeUtils.isCustomWizard ("WZ01")).thenReturn (false);
				
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		utils.setPlayerKnowledgeUtils (playerKnowledgeUtils);
		utils.setKnownWizardUtils (knownWizardUtils);
		
		// Run method
		assertTrue (utils.hasChosenAllDetails (player, mom));
	}

	/**
	 * Tests the hasChosenAllDetails method where we've chosen a custom wizard
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHasChosenAllDetails_Custom () throws Exception
	{
		// Set up player details
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPrivateKnowledge priv = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, null, null, priv);
		
		// Wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);
		
		final KnownWizardDetails wizardDetails = new KnownWizardDetails ();
		wizardDetails.setWizardID (null);
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), pd.getPlayerID ())).thenReturn (wizardDetails);
		
		// Make selections
		priv.setFirstCityRaceID ("RC01");
		priv.setCustomPicksChosen (true);

		// Has wizard been chosen?
		final PlayerKnowledgeUtils playerKnowledgeUtils = mock (PlayerKnowledgeUtils.class);
		when (playerKnowledgeUtils.isCustomWizard (null)).thenReturn (true);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		utils.setPlayerKnowledgeUtils (playerKnowledgeUtils);
		utils.setKnownWizardUtils (knownWizardUtils);
		
		// Run method
		assertTrue (utils.hasChosenAllDetails (player, mom));
	}

	/**
	 * Tests the hasChosenAllDetails method where we've chosen a custom wizard but didn't make custom picks yet
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHasChosenAllDetails_CustomNotChosen () throws Exception
	{
		// Set up player details
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPrivateKnowledge priv = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, null, null, priv);
		
		// Wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);
		
		final KnownWizardDetails wizardDetails = new KnownWizardDetails ();
		wizardDetails.setWizardID (null);
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), pd.getPlayerID ())).thenReturn (wizardDetails);
		
		// Make selections
		priv.setFirstCityRaceID ("RC01");

		// Has wizard been chosen?
		final PlayerKnowledgeUtils playerKnowledgeUtils = mock (PlayerKnowledgeUtils.class);
		when (playerKnowledgeUtils.isCustomWizard (null)).thenReturn (true);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		utils.setPlayerKnowledgeUtils (playerKnowledgeUtils);
		utils.setKnownWizardUtils (knownWizardUtils);
		
		// Run method
		assertFalse (utils.hasChosenAllDetails (player, mom));
	}

	/**
	 * Tests the hasChosenAllDetails method where we've not chosen a wizard
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHasChosenAllDetails_NoWizard () throws Exception
	{
		// Set up player details
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPrivateKnowledge priv = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, null, null, priv);

		// Wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), pd.getPlayerID ())).thenReturn (null);
		
		// Make selections
		priv.setFirstCityRaceID ("RC01");

		// Has wizard been chosen?
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		utils.setKnownWizardUtils (knownWizardUtils);
		
		// Run method
		assertFalse (utils.hasChosenAllDetails (player, mom));
	}
	
	/**
	 * Tests the hasChosenAllDetails method where we've not chosen a race
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHasChosenAllDetails_NoRace () throws Exception
	{
		// Set up player details
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPrivateKnowledge priv = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, null, null, priv);
		
		// Wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);
		
		final KnownWizardDetails wizardDetails = new KnownWizardDetails ();
		wizardDetails.setWizardID ("WZ01");

		// Wizard
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), pd.getPlayerID ())).thenReturn (wizardDetails);

		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		utils.setKnownWizardUtils (knownWizardUtils);
		
		// Run method
		assertFalse (utils.hasChosenAllDetails (player, mom));
	}
	
	/**
	 * Tests the allPlayersHaveChosenAllDetails method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAllPlayersHaveChosenAllDetails () throws Exception
	{
		// Delphi client sets maxplayers = human opponents + AI opponents + 3, so follow that here
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setMaxPlayers (3);
		sd.setAiPlayerCount (0);

		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Has wizard been chosen?
		final PlayerKnowledgeUtils playerKnowledgeUtils = mock (PlayerKnowledgeUtils.class);
		when (playerKnowledgeUtils.isCustomWizard ("WZ01")).thenReturn (false);
		when (playerKnowledgeUtils.isCustomWizard ("WZ02")).thenReturn (false);
		
		// Session variables
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);

		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		utils.setPlayerKnowledgeUtils (playerKnowledgeUtils);
		utils.setKnownWizardUtils (knownWizardUtils);
		
		// Single player game just against raiders, and nothing chosen yet
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setHuman (true);

		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPrivateKnowledge priv = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player = new PlayerServerDetails (pd, ppk, null, null, priv);
		players.add (player);

		// Wizard
		final KnownWizardDetails wizardDetails = new KnownWizardDetails ();
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), pd.getPlayerID ())).thenReturn (wizardDetails);
		
		assertFalse (utils.allPlayersHaveChosenAllDetails (mom));

		// Fill in details
		wizardDetails.setWizardID ("WZ01");
		priv.setFirstCityRaceID ("RC01");
		assertTrue (utils.allPlayersHaveChosenAllDetails (mom));

		// Add an AI player - this doesn't stop the game starting, since AI players are added after
		sd.setMaxPlayers (4);
		sd.setAiPlayerCount (1);
		assertTrue (utils.allPlayersHaveChosenAllDetails (mom));

		// Add slot for 2nd player
		sd.setMaxPlayers (5);
		assertFalse (utils.allPlayersHaveChosenAllDetails (mom));

		// Add second player
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (3);
		pd2.setHuman (true);

		final MomPersistentPlayerPublicKnowledge ppk2 = new MomPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPrivateKnowledge priv2 = new MomTransientPlayerPrivateKnowledge ();

		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, ppk2, null, null, priv2);
		players.add (player2);

		final KnownWizardDetails wizardDetails2 = new KnownWizardDetails ();
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), pd2.getPlayerID ())).thenReturn (wizardDetails2);
		
		assertFalse (utils.allPlayersHaveChosenAllDetails (mom));

		// Fill in second player details
		wizardDetails2.setWizardID ("WZ02");
		priv2.setFirstCityRaceID ("RC02");
		assertTrue (utils.allPlayersHaveChosenAllDetails (mom));
	}
	
	/**
	 * Tests the allPlayersAreConnected method
	 */
	@Test
	public final void testAllPlayersAreConnected ()
	{
		// Set up 2 human and 1 AI player, 1 human player is still disconnected
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setHuman (true);
		final PlayerServerDetails player1 = new PlayerServerDetails (pd1, null, null, null, null);
		player1.setConnection (mock (ServerToClientSessionConnection.class));

		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setHuman (true);
		final PlayerServerDetails player2 = new PlayerServerDetails (pd2, null, null, null, null);
		
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setHuman (false);
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, null, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player1);
		players.add (player2);
		players.add (player3);
		
		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		
		// Initially should return false
		assertFalse (utils.allPlayersAreConnected (players));
		
		// Missing human player connects
		player2.setConnection (mock (ServerToClientSessionConnection.class));
		assertTrue (utils.allPlayersAreConnected (players));
	}

	/**
	 * Tests the listWizardsForAIPlayers method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testListWizardsForAIPlayers () throws Exception
	{
		// Mock 9 wizards in DB
		final List<WizardEx> availableWizards = new ArrayList<WizardEx> ();
		for (int n = 1; n <= 9; n++)
		{
			final WizardEx wizard = new WizardEx ();
			wizard.setWizardID ("WZ0" + n);
			availableWizards.add (wizard);
		}

		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getWizards ()).thenReturn (availableWizards);

		// Create human players using 2 wizards
		final List<KnownWizardDetails> wizards = new ArrayList<KnownWizardDetails> ();

		final KnownWizardDetails wizard1 = new KnownWizardDetails ();
		wizard1.setStandardPhotoID ("WZ02");
		wizards.add (wizard1);

		final KnownWizardDetails wizard2 = new KnownWizardDetails ();
		wizard2.setStandardPhotoID ("WZ08");
		wizards.add (wizard2);

		// Set up object to test
		final PlayerPickServerUtilsImpl utils = new PlayerPickServerUtilsImpl ();
		
		// Run test
		final List<WizardEx> wizardIDs = utils.listWizardsForAIPlayers (wizards, db);
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

		final CommonDatabase db = mock (CommonDatabase.class);
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

		final CommonDatabase db = mock (CommonDatabase.class);
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
		final List<RaceEx> races = new ArrayList<RaceEx> ();
		for (int n = 1; n <= 9; n++)
		{
			final RaceEx race = new RaceEx ();
			race.setRaceID ("RC0" + n);
			race.setNativePlane ((n <= 6) ? 0 : 1);
			races.add (race);
		}
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getRaces ()).thenReturn (races);
			
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