package momime.client.ui.frames;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.Unmarshaller;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.database.AvailableDatabase;
import momime.client.database.ClientDatabaseEx;
import momime.client.database.NewGameDatabase;
import momime.client.database.Wizard;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.v0_9_5.Pick;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.common.database.Plane;
import momime.common.database.Race;
import momime.common.database.Spell;
import momime.common.database.newgame.DifficultyLevelData;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.servertoclient.ChooseInitialSpellsNowRank;

import org.junit.Test;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

/**
 * Tests the NewGameUI class
 */
public final class TestNewGameUI
{
	/**
	 * @return Newly set up NewGameUI to test with
	 * @throws Exception If there is a problem
	 */
	private final NewGameUI createNewGameUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();

		// Mock client database
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		// Mock entries from the graphics XML
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);

		when (lang.findCategoryEntry ("frmNewGame", "Cancel")).thenReturn ("Cancel");
		when (lang.findCategoryEntry ("frmNewGame", "OK")).thenReturn ("OK");

		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// NEW GAME PANEL
		when (lang.findCategoryEntry ("frmNewGame", "Title")).thenReturn ("New Game");
		when (lang.findCategoryEntry ("frmNewGame", "HumanOpponents")).thenReturn ("Human Opponents");
		when (lang.findCategoryEntry ("frmNewGame", "AIOpponents")).thenReturn ("AI Opponents");
		when (lang.findCategoryEntry ("frmNewGame", "MapSize")).thenReturn ("Map Size and makeup");
		when (lang.findCategoryEntry ("frmNewGame", "LandProportion")).thenReturn ("Land Proportion");
		when (lang.findCategoryEntry ("frmNewGame", "Nodes")).thenReturn ("Nodes");
		when (lang.findCategoryEntry ("frmNewGame", "Difficulty")).thenReturn ("Difficulty");
		when (lang.findCategoryEntry ("frmNewGame", "TurnSystem")).thenReturn ("Turn System");
		when (lang.findCategoryEntry ("frmNewGame", "FogOfWar")).thenReturn ("Fog of War");
		when (lang.findCategoryEntry ("frmNewGame", "UnitSettings")).thenReturn ("Unit Settings");
		when (lang.findCategoryEntry ("frmNewGame", "SpellSettings")).thenReturn ("Spell Settings");
		when (lang.findCategoryEntry ("frmNewGame", "DebugOptions")).thenReturn ("Use any debug options?");
		when (lang.findCategoryEntry ("frmNewGame", "GameName")).thenReturn ("Game Name");
		when (lang.findCategoryEntry ("frmNewGame", "Customize")).thenReturn ("Customize?");
		
		// WIZARD SELECTION PANEL
		when (lang.findCategoryEntry ("frmChooseWizard", "Title")).thenReturn ("Select Wizard");
		when (lang.findCategoryEntry ("frmChooseWizard", "Custom")).thenReturn ("Custom");

		final List<Wizard> wizards = new ArrayList<Wizard> ();
		for (int n = 1; n <= 14; n++)
		{
			final Wizard wizard = new Wizard ();
			wizard.setWizardID ("WZ" + ((n < 10) ? "0" : "") + n);
			wizards.add (wizard);
		}
		when (db.getWizard ()).thenReturn (wizards);
		
		for (int n = 1; n <= 14; n++)
		{
			final String wizardID = "WZ" + ((n < 10) ? "0" : "") + n;
			when (lang.findWizardName (wizardID)).thenReturn ("Wiz " + wizardID);
		}
		
		// PORTRAIT SELECTION PANEL (for custom wizards)
		when (lang.findCategoryEntry ("frmChoosePortrait", "Title")).thenReturn ("Select Picture");
		when (lang.findCategoryEntry ("frmChoosePortrait", "Custom")).thenReturn ("Custom");
		
		// FREE SPELL SELECTION PANEL
		when (lang.findCategoryEntry ("frmChooseInitialSpells", "Title")).thenReturn ("Select MAGIC_REALM Spells");
		
		final momime.client.language.database.v0_9_5.Pick magicRealmLang = new momime.client.language.database.v0_9_5.Pick ();
		magicRealmLang.setBookshelfDescription ("Life");
		when (lang.findPick ("MB01")).thenReturn (magicRealmLang);

		final Pick magicRealm = new Pick ();
		magicRealm.setPickBookshelfTitleColour ("FF8080");
		when (gfx.findPick (eq ("MB01"), anyString ())).thenReturn (magicRealm);
		
		for (int n = 1; n <= 4; n++)
			when (lang.findSpellRankDescription ("SR0" + n)).thenReturn ("Rank " + n);
		
		final List<Spell> spells = new ArrayList<Spell> ();
		for (int rank = 1; rank <= 4; rank++)
			for (int spellNo = 0; spellNo < 10; spellNo++)
			{
				final Spell spell = new Spell ();
				spell.setSpellRealm ("MB01");
				spell.setSpellRank ("SR0" + rank);
				spell.setSpellID ("SP0" + rank + spellNo);
				spells.add (spell);
				
				final momime.client.language.database.v0_9_5.Spell spellLang = new momime.client.language.database.v0_9_5.Spell ();
				spellLang.setSpellName ("Spell " + spell.getSpellID ());
				when (lang.findSpell (spell.getSpellID ())).thenReturn (spellLang);
			}
		
		doReturn (spells).when (db).getSpell ();
		
		// RACE SELECTION PANEL
		when (lang.findCategoryEntry ("frmChooseRace", "Title")).thenReturn ("Choose Race");

		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();

		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final PlayerPublicDetails ourPlayer = new PlayerPublicDetails (null, pub, null);
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, 3, "NewGameUI.showRacePanel")).thenReturn (ourPlayer);
		
		final List<Plane> planes = new ArrayList<Plane> ();
		for (int n = 0; n < 2; n++)
		{
			final Plane plane = new Plane ();
			plane.setPlaneNumber (n);
			planes.add (plane);
			
			when (db.findPlane (n, "NewGameUI.showRacePanel")).thenReturn (plane);
			
			final momime.client.language.database.v0_9_5.Plane planeLang = new momime.client.language.database.v0_9_5.Plane ();
			planeLang.setPlaneRacesTitle ("Plane " + n + " races");
			when (lang.findPlane (n)).thenReturn (planeLang);
		}
		
		doReturn (planes).when (db).getPlane ();
		
		final List<Race> races = new ArrayList<Race> ();
		for (int plane = 0; plane < 2; plane++)
			for (int raceNo = 0; raceNo < 5; raceNo++)
			{
				final Race race = new Race ();
				race.setRaceID ("RC" + plane + raceNo);
				race.setNativePlane (plane);
				races.add (race);
				
				final momime.client.language.database.v0_9_5.Race raceLang = new momime.client.language.database.v0_9_5.Race ();
				raceLang.setRaceName ("Race " + race.getRaceID ());
				when (lang.findRace (race.getRaceID ())).thenReturn (raceLang);
			}
		
		doReturn (races).when (db).getRace ();

		// WAITING TO OTHER PLAYERS TO JOIN PANEL
		when (lang.findCategoryEntry ("frmWaitForPlayersToJoin", "Title")).thenReturn ("Waiting for other Players to Join");
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// Mock list of available databases
		final NewGameDatabase dbs = new NewGameDatabase ();
		for (int n = 1; n <= 3; n++)
		{
			final AvailableDatabase ad = new AvailableDatabase ();
			ad.setDbName ("DB " + n);
			dbs.getMomimeXmlDatabase ().add (ad);
		}
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getNewGameDatabase ()).thenReturn (dbs);
		when (client.getOurPlayerID ()).thenReturn (3);
		when (client.getPlayers ()).thenReturn (players);
		
		// Session description
		final DifficultyLevelData difficulty = new DifficultyLevelData ();
		difficulty.setCustomWizards (true);
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setDifficultyLevel (difficulty);
		when (client.getSessionDescription ()).thenReturn (sd);
		
		// Layouts
		final Unmarshaller unmarshaller = ClientTestData.createXmlLayoutUnmarshaller ();
		final XmlLayoutContainerEx mainLayout = (XmlLayoutContainerEx) unmarshaller.unmarshal (getClass ().getResource ("/momime.client.ui.frames/NewGameUI-Main.xml"));
		final XmlLayoutContainerEx newLayout = (XmlLayoutContainerEx) unmarshaller.unmarshal (getClass ().getResource ("/momime.client.ui.frames/NewGameUI-New.xml"));
		mainLayout.buildMaps ();
		newLayout.buildMaps ();
		
		// Set up form
		final NewGameUI game = new NewGameUI ();
		game.setUtils (utils);
		game.setLanguageHolder (langHolder);
		game.setLanguageChangeMaster (langMaster);
		game.setClient (client);
		game.setGraphicsDB (gfx);
		game.setMultiplayerSessionUtils (multiplayerSessionUtils);
		game.setNewGameLayoutMain (mainLayout);
		game.setNewGameLayoutNew (newLayout);
		game.setSmallFont (CreateFontsForTests.getSmallFont ());
		game.setMediumFont (CreateFontsForTests.getMediumFont ());
		game.setLargeFont (CreateFontsForTests.getLargeFont ());
		
		return game;
	}

	/**
	 * Tests the "new game" screen
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testNewGameUI_NewGame () throws Exception
	{
		final NewGameUI game = createNewGameUI ();
		
		// Display form
		game.setVisible (true);
		game.showNewGamePanel ();
		Thread.sleep (5000);
		game.setVisible (false);
	}

	/**
	 * Tests the "choose wizard" screen
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testNewGameUI_ChooseWizard () throws Exception
	{
		final NewGameUI game = createNewGameUI ();
		
		// Display form
		game.setVisible (true);
		game.afterJoinedSession ();
		Thread.sleep (5000);
		game.setVisible (false);
	}

	/**
	 * Tests the "choose portrait" screen (if a custom wizard was chosen)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testNewGameUI_ChoosePortrait () throws Exception
	{
		final NewGameUI game = createNewGameUI ();
		
		// Display form
		game.setVisible (true);
		game.afterJoinedSession ();		// Need this too to create the wizard buttons
		game.showPortraitPanel ();
		Thread.sleep (5000);
		game.setVisible (false);
	}
	
	/**
	 * Tests the "choose free spells" screen, to pick spells when we have 2 or more books of a particular magic realm
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testNewGameUI_ChooseFreeSpells () throws Exception
	{
		final NewGameUI game = createNewGameUI ();
		
		// Number of each spell rank that we get to choose
		// This has to list all ranks, even the ones we get no free spells in
		final List<ChooseInitialSpellsNowRank> spellRanks = new ArrayList<ChooseInitialSpellsNowRank> ();
		
		for (int n = 1; n <= 4; n++)
		{
			final ChooseInitialSpellsNowRank rank = new ChooseInitialSpellsNowRank ();
			rank.setSpellRankID ("SR0" + n);
			rank.setFreeSpellCount ((n == 1) ? 4 : 0);
			spellRanks.add (rank);
		}
		
		// Display form
		game.setVisible (true);
		game.showInitialSpellsPanel ("MB01", spellRanks);
		Thread.sleep (5000);
		game.setVisible (false);
	}

	/**
	 * Tests the "choose race" screen
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testNewGameUI_ChooseRace () throws Exception
	{
		final NewGameUI game = createNewGameUI ();
		
		// Display form
		game.setVisible (true);
		game.afterJoinedSession ();		// Need this too to create the race buttons
		game.showRacePanel ();;
		Thread.sleep (5000);
		game.setVisible (false);
	}

	/**
	 * Tests the "wait for other players to join" screen
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testNewGameUI_Wait () throws Exception
	{
		final NewGameUI game = createNewGameUI ();
		
		// Display form
		game.setVisible (true);
		game.showWaitPanel ();
		Thread.sleep (5000);
		game.setVisible (false);
	}
}