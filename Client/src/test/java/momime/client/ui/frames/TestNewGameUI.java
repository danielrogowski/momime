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
		
		// CUSTOM MAP SIZE PANEL
		when (lang.findCategoryEntry ("frmNewGameCustomMapSize", "Title")).thenReturn ("New Game: Custom Map Size");
		when (lang.findCategoryEntry ("frmNewGameCustomMapSize", "MapSize")).thenReturn ("Map Size");
		when (lang.findCategoryEntry ("frmNewGameCustomMapSize", "WrapsLeftRight")).thenReturn ("Map wraps Left-Right?");
		when (lang.findCategoryEntry ("frmNewGameCustomMapSize", "WrapsTopBottom")).thenReturn ("Map wraps Top-Bottom?");
		when (lang.findCategoryEntry ("frmNewGameCustomMapSize", "Zones")).thenReturn ("Terrain generator splits map into zones with size approximately");
		when (lang.findCategoryEntry ("frmNewGameCustomMapSize", "Towers")).thenReturn ("Number of Towers of Wizardry");
		when (lang.findCategoryEntry ("frmNewGameCustomMapSize", "TowersSeparation")).thenReturn ("Minimum separation between Towers");
		when (lang.findCategoryEntry ("frmNewGameCustomMapSize", "Continental")).thenReturn ("Chance that raider cities are the race chosen for the continent");
		when (lang.findCategoryEntry ("frmNewGameCustomMapSize", "CitySeparation")).thenReturn ("Minimum separation between Cities");
		
		// CUSTOM LAND PROPORTION PANEL
		when (lang.findCategoryEntry ("frmNewGameCustomLandProportion", "Title")).thenReturn ("New Game: Custom Land Proportion");
		when (lang.findCategoryEntry ("frmNewGameCustomLandProportion", "PercentageMapIsLand")).thenReturn ("% of map is land");
		when (lang.findCategoryEntry ("frmNewGameCustomLandProportion", "HillsProportion")).thenReturn ("% of land is hills");
		when (lang.findCategoryEntry ("frmNewGameCustomLandProportion", "MountainsProportion")).thenReturn ("% of hills are mountains");
		when (lang.findCategoryEntry ("frmNewGameCustomLandProportion", "TreesProportion")).thenReturn ("% of land is trees, split into areas");
		when (lang.findCategoryEntry ("frmNewGameCustomLandProportion", "TreeAreaTileCountPrefix")).thenReturn ("each approximately");
		when (lang.findCategoryEntry ("frmNewGameCustomLandProportion", "TreeAreaTileCountSuffix")).thenReturn ("tiles");
		when (lang.findCategoryEntry ("frmNewGameCustomLandProportion", "DesertProportion")).thenReturn ("% of land is desert, split into areas");
		when (lang.findCategoryEntry ("frmNewGameCustomLandProportion", "DesertAreaTileCountPrefix")).thenReturn ("each approximately");
		when (lang.findCategoryEntry ("frmNewGameCustomLandProportion", "DesertAreaTileCountSuffix")).thenReturn ("tiles");
		when (lang.findCategoryEntry ("frmNewGameCustomLandProportion", "SwampProportion")).thenReturn ("% of land is swamp, split into areas");
		when (lang.findCategoryEntry ("frmNewGameCustomLandProportion", "SwampAreaTileCountPrefix")).thenReturn ("each approximately");
		when (lang.findCategoryEntry ("frmNewGameCustomLandProportion", "SwampAreaTileCountSuffix")).thenReturn ("tiles");
		when (lang.findCategoryEntry ("frmNewGameCustomLandProportion", "TundraPrefix")).thenReturn ("Tundra appears");
		when (lang.findCategoryEntry ("frmNewGameCustomLandProportion", "TundraSuffix")).thenReturn ("tiles from the edge of the map");
		when (lang.findCategoryEntry ("frmNewGameCustomLandProportion", "Rivers")).thenReturn ("Number of rivers on each plane");
		when (lang.findCategoryEntry ("frmNewGameCustomLandProportion", "ArcanusPrefix")).thenReturn ("Tiles on Arcanus have a 1 in");
		when (lang.findCategoryEntry ("frmNewGameCustomLandProportion", "ArcanusSuffix")).thenReturn ("chance of containing a mineral deposit");
		when (lang.findCategoryEntry ("frmNewGameCustomLandProportion", "MyrrorPrefix")).thenReturn ("Tiles on Myrror have a 1 in");
		when (lang.findCategoryEntry ("frmNewGameCustomLandProportion", "MyrrorSuffix")).thenReturn ("chance of containing a mineral deposit");
		
		// CUSTOM NODES PANEL
		when (lang.findCategoryEntry ("frmNewGameCustomNodes", "Title")).thenReturn ("New Game: Custom Nodes");
		when (lang.findCategoryEntry ("frmNewGameCustomNodes", "MagicPowerPrefix")).thenReturn ("Each square of node aura generates");
		when (lang.findCategoryEntry ("frmNewGameCustomNodes", "MagicPowerSuffix")).thenReturn ("/2 magic power");
		when (lang.findCategoryEntry ("frmNewGameCustomNodes", "ArcanusCount")).thenReturn ("Number of nodes on Arcanus");
		when (lang.findCategoryEntry ("frmNewGameCustomNodes", "ArcanusNodeAuraPrefix")).thenReturn ("Each node on Arcanus has an aura between");
		when (lang.findCategoryEntry ("frmNewGameCustomNodes", "ArcanusNodeAuraSuffix")).thenReturn ("squares");
		when (lang.findCategoryEntry ("frmNewGameCustomNodes", "MyrrorCount")).thenReturn ("Number of nodes on Myrror");
		when (lang.findCategoryEntry ("frmNewGameCustomNodes", "MyrrorNodeAuraPrefix")).thenReturn ("Each node on Myrror has an aura between");
		when (lang.findCategoryEntry ("frmNewGameCustomNodes", "MyrrorNodeAuraSuffix")).thenReturn ("squares");

		// CUSTOM DIFFICULTY PANEL (1 of 3)
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty1", "Title")).thenReturn ("New Game: Custom Difficulty (1 of 2)");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty1", "SpellPicks")).thenReturn ("Spell picks:");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty1", "HumanSpellPicks")).thenReturn ("Human");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty1", "AISpellPicks")).thenReturn ("AI");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty1", "Gold")).thenReturn ("Gold:");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty1", "HumanGold")).thenReturn ("Human");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty1", "AIGold")).thenReturn ("AI");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty1", "CustomWizards")).thenReturn ("Allow custom wizards?");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty1", "EachWizardOnlyOnce")).thenReturn ("Each wizard can be chosen only once?");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty1", "WizardCitySize")).thenReturn ("Wizards' cities start at size");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty1", "MaxCitySize")).thenReturn ("Maximum city size");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty1", "RaiderCityCount")).thenReturn ("Number of neutral/raider cities");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty1", "RaiderCitySizePrefix")).thenReturn ("Neutral/raider cities start between size");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty1", "RaiderCitySizeAnd")).thenReturn ("and");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty1", "RaiderCityGrowthPrefix")).thenReturn ("and can grow up to");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty1", "RaiderCityGrowthSuffix")).thenReturn ("larger than their starting size");
		
		// CUSTOM DIFFICULTY PANEL (2 of 3)
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty2", "Title")).thenReturn ("New Game: Custom Difficulty (2 of 2)");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty2", "TowerMonsters")).thenReturn ("Towers: Monsters");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty2", "TowerTreasure")).thenReturn ("Towers: Treasure");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty2", "NormalLairCount")).thenReturn ("Number of normal strength lairs");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty2", "NormalArcanusLairMonsters")).thenReturn ("On Arcanus: Monsters");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty2", "NormalArcanusLairTreasure")).thenReturn ("On Arcanus: Treasure");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty2", "NormalMyrrorLairMonsters")).thenReturn ("On Myrror: Monsters");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty2", "NormalMyrrorLairTreasure")).thenReturn ("On Myrror: Treasure");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty2", "WeakLairCount")).thenReturn ("Number of weak strength lairs");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty2", "WeakArcanusLairMonsters")).thenReturn ("On Arcanus: Monsters");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty2", "WeakArcanusLairTreasure")).thenReturn ("On Arcanus: Treasure");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty2", "WeakMyrrorLairMonsters")).thenReturn ("On Myrror: Monsters");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty2", "WeakMyrrorLairTreasure")).thenReturn ("On Myrror: Treasure");
		
		// CUSTOM DIFFICULTY PANEL (3 of 3)
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty3", "Title")).thenReturn ("New Game: Custom Node Difficulty");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty3", "ArcanusNodeMonsters")).thenReturn ("On Arcanus: Monsters");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty3", "ArcanusNodeTreasure")).thenReturn ("On Arcanus: Treasure");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty3", "MyrrorNodeMonsters")).thenReturn ("On Myrror: Monsters");
		when (lang.findCategoryEntry ("frmNewGameCustomDifficulty3", "MyrrorNodeTreasure")).thenReturn ("On Myrror: Treasure");
		
		// CUSTOM FOG OF WAR PANEL
		when (lang.findCategoryEntry ("frmNewGameCustomFogOfWar", "Title")).thenReturn ("New Game: Custom Fog of War");
		when (lang.findCategoryEntry ("frmNewGameCustomFogOfWar", "TerrainNodesAuras")).thenReturn ("Terrain, Nodes and Auras");
		when (lang.findCategoryEntry ("frmNewGameCustomFogOfWar", "TerrainAlways")).thenReturn ("Always see once seen");
		when (lang.findCategoryEntry ("frmNewGameCustomFogOfWar", "TerrainRemember")).thenReturn ("Remember as last seen");
		when (lang.findCategoryEntry ("frmNewGameCustomFogOfWar", "TerrainForget")).thenReturn ("Forget");
		when (lang.findCategoryEntry ("frmNewGameCustomFogOfWar", "CitiesSpellsCAEs")).thenReturn ("Cities, Combat Spells and Combat Area Effects");
		when (lang.findCategoryEntry ("frmNewGameCustomFogOfWar", "CitiesAlways")).thenReturn ("Always see once seen");
		when (lang.findCategoryEntry ("frmNewGameCustomFogOfWar", "CitiesRemember")).thenReturn ("Remember as last seen");
		when (lang.findCategoryEntry ("frmNewGameCustomFogOfWar", "CitiesForget")).thenReturn ("Forget");
		when (lang.findCategoryEntry ("frmNewGameCustomFogOfWar", "Constructing")).thenReturn ("Can see what enemy cities are constructing?");
		when (lang.findCategoryEntry ("frmNewGameCustomFogOfWar", "Units")).thenReturn ("Units");
		when (lang.findCategoryEntry ("frmNewGameCustomFogOfWar", "UnitsAlways")).thenReturn ("Always see once seen");
		when (lang.findCategoryEntry ("frmNewGameCustomFogOfWar", "UnitsRemember")).thenReturn ("Remember as last seen");
		when (lang.findCategoryEntry ("frmNewGameCustomFogOfWar", "UnitsForget")).thenReturn ("Forget");
		
		// CUSTOM UNIT SETTINGS PANEL
		when (lang.findCategoryEntry ("frmNewGameCustomUnits", "Title")).thenReturn ("New Game: Custom Unit Settings");
		when (lang.findCategoryEntry ("frmNewGameCustomUnits", "MaxPerGridCell")).thenReturn ("Maximum units per grid cell");
		when (lang.findCategoryEntry ("frmNewGameCustomUnits", "CanExceedMaximumUnitsDuringCombat")).thenReturn ("Can temporarily exceed maximum during combat (e.g. with Phantom Warriors, Earth/Air/Fire Elementals)");
		when (lang.findCategoryEntry ("frmNewGameCustomUnits", "MaxHeroes")).thenReturn ("Maximum heroes at a time");
		when (lang.findCategoryEntry ("frmNewGameCustomUnits", "RollHeroSkillsAtStartOfGame")).thenReturn ("Hero random skills are rolled at the start of the game (so cannot reroll by reloading the game just before spell finishes casting)");
		
		// CUSTOM SPELL SETTINGS PANEL
		when (lang.findCategoryEntry ("frmNewGameCustomSpells", "Title")).thenReturn ("New Game: Custom Spell Settings");
		when (lang.findCategoryEntry ("frmNewGameCustomSpells", "SwitchResearch")).thenReturn ("Allow switching which spell is being researched?");
		when (lang.findCategoryEntry ("frmNewGameCustomSpells", "SwitchResearchNo")).thenReturn ("No - must finish current research first");
		when (lang.findCategoryEntry ("frmNewGameCustomSpells", "SwitchResearchNotStarted")).thenReturn ("Only if current spell research not started");
		when (lang.findCategoryEntry ("frmNewGameCustomSpells", "SwitchResearchLose")).thenReturn ("Yes, but lose any current research");
		when (lang.findCategoryEntry ("frmNewGameCustomSpells", "SwitchResearchFreely")).thenReturn ("Yes, can switch freely with no penalty");
		when (lang.findCategoryEntry ("frmNewGameCustomSpells", "BooksToObtainFirstReduction")).thenReturn ("Spell books to obtain first reduction");
		when (lang.findCategoryEntry ("frmNewGameCustomSpells", "EachBook")).thenReturn ("Each book gives");
		when (lang.findCategoryEntry ("frmNewGameCustomSpells", "CastingReductionPrefix")).thenReturn ("a");
		when (lang.findCategoryEntry ("frmNewGameCustomSpells", "CastingReductionSuffix")).thenReturn ("% reduction in casting cost");
		when (lang.findCategoryEntry ("frmNewGameCustomSpells", "ResearchBonusPrefix")).thenReturn ("and a");
		when (lang.findCategoryEntry ("frmNewGameCustomSpells", "ResearchBonusSuffix")).thenReturn ("% bonus to research");
		when (lang.findCategoryEntry ("frmNewGameCustomSpells", "CastingReductionCombinationAdd")).thenReturn ("Casting cost reductions are added together");
		when (lang.findCategoryEntry ("frmNewGameCustomSpells", "CastingReductionCombinationMultiply")).thenReturn ("Casting cost reductions are multiplied together");
		when (lang.findCategoryEntry ("frmNewGameCustomSpells", "ResearchBonusCombinationAdd")).thenReturn ("Research bonuses are added together");
		when (lang.findCategoryEntry ("frmNewGameCustomSpells", "ResearchBonusCombinationMultiply")).thenReturn ("Research bonuses are multiplied together");
		when (lang.findCategoryEntry ("frmNewGameCustomSpells", "CastingReductionCap")).thenReturn ("Casting cost reduction is capped at");
		when (lang.findCategoryEntry ("frmNewGameCustomSpells", "ResearchBonusCap")).thenReturn ("Research bonus is capped at");
		
		// DEBUG OPTIONS PANEL
		when (lang.findCategoryEntry ("frmNewGameCustomDebug", "Title")).thenReturn ("New Game: Debug Options");
		when (lang.findCategoryEntry ("frmNewGameCustomDebug", "DisableFogOfWar")).thenReturn ("Disable Fog of War (as if all players have Nature Awareness cast all the time)");
		
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
		final XmlLayoutContainerEx mainLayout				= (XmlLayoutContainerEx) unmarshaller.unmarshal (getClass ().getResource ("/momime.client.ui.frames/NewGameUI-Main.xml"));
		final XmlLayoutContainerEx newLayout					= (XmlLayoutContainerEx) unmarshaller.unmarshal (getClass ().getResource ("/momime.client.ui.frames/NewGameUI-New.xml"));
		final XmlLayoutContainerEx mapSizeLayout			= (XmlLayoutContainerEx) unmarshaller.unmarshal (getClass ().getResource ("/momime.client.ui.frames/NewGameUI-MapSize.xml"));
		final XmlLayoutContainerEx landProportionLayout	= (XmlLayoutContainerEx) unmarshaller.unmarshal (getClass ().getResource ("/momime.client.ui.frames/NewGameUI-LandProportion.xml"));
		final XmlLayoutContainerEx nodesLayout				= (XmlLayoutContainerEx) unmarshaller.unmarshal (getClass ().getResource ("/momime.client.ui.frames/NewGameUI-Nodes.xml"));
		final XmlLayoutContainerEx difficulty1Layout			= (XmlLayoutContainerEx) unmarshaller.unmarshal (getClass ().getResource ("/momime.client.ui.frames/NewGameUI-Difficulty1.xml"));
		final XmlLayoutContainerEx difficulty2Layout			= (XmlLayoutContainerEx) unmarshaller.unmarshal (getClass ().getResource ("/momime.client.ui.frames/NewGameUI-Difficulty2.xml"));
		final XmlLayoutContainerEx difficulty3Layout			= (XmlLayoutContainerEx) unmarshaller.unmarshal (getClass ().getResource ("/momime.client.ui.frames/NewGameUI-Difficulty3.xml"));
		final XmlLayoutContainerEx fogOfWarLayout			= (XmlLayoutContainerEx) unmarshaller.unmarshal (getClass ().getResource ("/momime.client.ui.frames/NewGameUI-FogOfWar.xml"));
		final XmlLayoutContainerEx unitSettingsLayout		= (XmlLayoutContainerEx) unmarshaller.unmarshal (getClass ().getResource ("/momime.client.ui.frames/NewGameUI-UnitSettings.xml"));
		final XmlLayoutContainerEx spellSettingsLayout		= (XmlLayoutContainerEx) unmarshaller.unmarshal (getClass ().getResource ("/momime.client.ui.frames/NewGameUI-SpellSettings.xml"));
		final XmlLayoutContainerEx debugOptionsLayout	= (XmlLayoutContainerEx) unmarshaller.unmarshal (getClass ().getResource ("/momime.client.ui.frames/NewGameUI-Debug.xml"));
		mainLayout.buildMaps ();
		newLayout.buildMaps ();
		mapSizeLayout.buildMaps ();
		landProportionLayout.buildMaps ();
		nodesLayout.buildMaps ();
		difficulty1Layout.buildMaps ();
		difficulty2Layout.buildMaps ();
		difficulty3Layout.buildMaps ();
		fogOfWarLayout.buildMaps ();
		unitSettingsLayout.buildMaps ();
		spellSettingsLayout.buildMaps ();
		debugOptionsLayout.buildMaps ();
		
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
		game.setNewGameLayoutMapSize (mapSizeLayout);
		game.setNewGameLayoutLandProportion (landProportionLayout);
		game.setNewGameLayoutNodes (nodesLayout);
		game.setNewGameLayoutDifficulty1 (difficulty1Layout);
		game.setNewGameLayoutDifficulty2 (difficulty2Layout);
		game.setNewGameLayoutDifficulty3 (difficulty3Layout);
		game.setNewGameLayoutFogOfWar (fogOfWarLayout);
		game.setNewGameLayoutUnits (unitSettingsLayout);
		game.setNewGameLayoutSpells (spellSettingsLayout);
		game.setNewGameLayoutDebug (debugOptionsLayout);
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
	 * Tests the "custom map size" screen
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testNewGameUI_MapSize () throws Exception
	{
		final NewGameUI game = createNewGameUI ();
		
		// Display form
		game.setVisible (true);
		game.customizeMapSize.setSelected (true);
		game.showNextNewGamePanel ();
		Thread.sleep (5000);
		game.setVisible (false);
	}
	
	/**
	 * Tests the "custom land proportion" screen
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testNewGameUI_LandProportion () throws Exception
	{
		final NewGameUI game = createNewGameUI ();
		
		// Display form
		game.setVisible (true);
		game.customizeLandProportion.setSelected (true);
		game.showNextNewGamePanel ();
		Thread.sleep (5000);
		game.setVisible (false);
	}
	
	/**
	 * Tests the "custom nodes" screen
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testNewGameUI_Nodes () throws Exception
	{
		final NewGameUI game = createNewGameUI ();
		
		// Display form
		game.setVisible (true);
		game.customizeNodes.setSelected (true);
		game.showNextNewGamePanel ();
		Thread.sleep (5000);
		game.setVisible (false);
	}
	
	/**
	 * Tests the "custom difficulty (1 of 3)" screen
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testNewGameUI_Difficulty1 () throws Exception
	{
		final NewGameUI game = createNewGameUI ();
		
		// Display form
		game.setVisible (true);
		game.customizeDifficulty.setSelected (true);
		game.showNextNewGamePanel ();
		Thread.sleep (5000);
		game.setVisible (false);
	}
	
	/**
	 * Tests the "custom difficulty (2 of 3)" screen
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testNewGameUI_Difficulty2 () throws Exception
	{
		final NewGameUI game = createNewGameUI ();
		
		// Display form
		game.setVisible (true);
		game.customizeDifficulty.setSelected (true);
		game.showNextNewGamePanel ();
		game.showNextNewGamePanel ();
		Thread.sleep (5000);
		game.setVisible (false);
	}
	
	/**
	 * Tests the "custom difficulty (3 of 3)" screen
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testNewGameUI_Difficulty3 () throws Exception
	{
		final NewGameUI game = createNewGameUI ();
		
		// Display form
		game.setVisible (true);
		game.customizeDifficulty.setSelected (true);
		game.showNextNewGamePanel ();
		game.showNextNewGamePanel ();
		game.showNextNewGamePanel ();
		Thread.sleep (5000);
		game.setVisible (false);
	}

	/**
	 * Tests the "custom fog of war" screen
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testNewGameUI_FogOfWar () throws Exception
	{
		final NewGameUI game = createNewGameUI ();
		
		// Display form
		game.setVisible (true);
		game.customizeFogOfWar.setSelected (true);
		game.showNextNewGamePanel ();
		Thread.sleep (5000);
		game.setVisible (false);
	}
	
	/**
	 * Tests the "custom unit settings" screen
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testNewGameUI_Units () throws Exception
	{
		final NewGameUI game = createNewGameUI ();
		
		// Display form
		game.setVisible (true);
		game.customizeUnits.setSelected (true);
		game.showNextNewGamePanel ();
		Thread.sleep (5000);
		game.setVisible (false);
	}
	
	/**
	 * Tests the "custom spell settings" screen
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testNewGameUI_Spells () throws Exception
	{
		final NewGameUI game = createNewGameUI ();
		
		// Display form
		game.setVisible (true);
		game.customizeSpells.setSelected (true);
		game.showNextNewGamePanel ();
		Thread.sleep (5000);
		game.setVisible (false);
	}
	
	/**
	 * Tests the "debug options" screen
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testNewGameUI_DebugOptions () throws Exception
	{
		final NewGameUI game = createNewGameUI ();
		
		// Display form
		game.setVisible (true);
		game.changeDebugOptionsAction.actionPerformed (null);
		game.showNextNewGamePanel ();
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