package momime.client.ui.frames;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.Unmarshaller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.multiplayer.base.client.ClientToServerConnection;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.random.RandomUtilsImpl;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.database.AvailableDatabase;
import momime.client.database.NewGameDatabase;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.ChooseFlagColourScreen;
import momime.client.languages.database.ChooseInitialSpellsScreen;
import momime.client.languages.database.ChoosePortraitScreen;
import momime.client.languages.database.ChooseRaceScreen;
import momime.client.languages.database.ChooseWizardScreen;
import momime.client.languages.database.CustomDebugTab;
import momime.client.languages.database.CustomDifficultyTab1;
import momime.client.languages.database.CustomDifficultyTab2;
import momime.client.languages.database.CustomDifficultyTab3;
import momime.client.languages.database.CustomFogOfWarTab;
import momime.client.languages.database.CustomLandProportionTab;
import momime.client.languages.database.CustomMapSizeTab;
import momime.client.languages.database.CustomNodesTab;
import momime.client.languages.database.CustomPicksScreen;
import momime.client.languages.database.CustomSpellsTab;
import momime.client.languages.database.CustomUnitsTab;
import momime.client.languages.database.NewGameScreen;
import momime.client.languages.database.Simple;
import momime.client.languages.database.TurnSystems;
import momime.client.languages.database.WaitForPlayersToJoinScreen;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.TextUtilsImpl;
import momime.client.utils.WizardClientUtils;
import momime.common.database.CommonDatabase;
import momime.common.database.DifficultyLevel;
import momime.common.database.FogOfWarSetting;
import momime.common.database.LandProportion;
import momime.common.database.Language;
import momime.common.database.NodeStrength;
import momime.common.database.OverlandMapSize;
import momime.common.database.Pick;
import momime.common.database.Plane;
import momime.common.database.RaceEx;
import momime.common.database.Spell;
import momime.common.database.SpellRank;
import momime.common.database.SpellSetting;
import momime.common.database.UnitSetting;
import momime.common.database.WizardEx;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.messages.servertoclient.ChooseInitialSpellsNowRank;
import momime.common.utils.PlayerPickUtilsImpl;

/**
 * Tests the NewGameUI class
 */
@ExtendWith(MockitoExtension.class)
public final class TestNewGameUI extends ClientTestData
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

		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final List<WizardEx> wizards = new ArrayList<WizardEx> ();
		for (int n = 1; n <= 14; n++)
		{
			final WizardEx wizard = new WizardEx ();
			wizard.setWizardID ("WZ" + ((n < 10) ? "0" : "") + n);
			wizard.getWizardName ().add (createLanguageText (Language.ENGLISH, "Wiz " + wizard.getWizardID ()));
			
			when (db.findWizard (eq (wizard.getWizardID ()), anyString ())).thenReturn (wizard);
			wizards.add (wizard);
		}
		when (db.getWizards ()).thenReturn (wizards);

		final List<Pick> picks = new ArrayList<Pick> ();
		for (int n = 1; n <= 18; n++)
		{
			final Pick pick = new Pick ();
			pick.setPickID ("RT" + ((n < 10) ? "0" : "") + n);
			pick.setPickCost (1);
			pick.getPickDescriptionSingular ().add (createLanguageText (Language.ENGLISH, "Retort " + pick.getPickID ()));
			picks.add (pick);
			when (db.findPick (eq (pick.getPickID ()), anyString ())).thenReturn (pick);
		}

		int magicRealmNo = 0;
		for (final String magicRealmName : new String [] {"Life", "Death", "Chaos", "Nature", "Sorcery"})
		{
			int colourNo = magicRealmNo * 2;
			magicRealmNo++;
			
			final Pick pick = new Pick ();
			pick.setPickID ("MB0" + magicRealmNo);
			pick.setPickCost (1);
			pick.getBookshelfDescription ().add (createLanguageText (Language.ENGLISH, magicRealmName));
			pick.setPickBookshelfTitleColour ("FF" + colourNo + "0" + colourNo + "0");
			
			for (int imageNo = 1; imageNo <= 3; imageNo++)
				pick.getBookImageFile ().add ("/momime.client.graphics/picks/" + magicRealmName.toLowerCase () + "-" + imageNo + ".png");
			
			picks.add (pick);
			when (db.findPick (eq (pick.getPickID ()), anyString ())).thenReturn (pick);
		}
		
		doReturn (picks).when (db).getPick ();

		// Don't allow picking both life + death books
		for (int n = 1; n <= 2; n++)
			db.findPick ("MB0" + n, "createNewGameUI").getPickExclusiveFrom ().add ("MB0" + (3-n));

		for (int n = 1; n <= 4; n++)
		{
			final SpellRank rank = new SpellRank ();
			rank.getSpellRankDescription ().add (createLanguageText (Language.ENGLISH, "Rank " + n));
			when (db.findSpellRank ("SR0" + n, "updateInitialSpellsCount")).thenReturn (rank);
		}
		
		final List<Spell> spells = new ArrayList<Spell> ();
		for (int rank = 1; rank <= 4; rank++)
			for (int spellNo = 0; spellNo < 10; spellNo++)
			{
				final Spell spell = new Spell ();
				spell.setSpellRealm ("MB01");
				spell.setSpellRank ("SR0" + rank);
				spell.setSpellID ("SP0" + rank + spellNo);
				spell.getSpellName ().add (createLanguageText (Language.ENGLISH, "Spell " + spell.getSpellID ()));
				spells.add (spell);
				
				when (db.findSpell (eq (spell.getSpellID ()), anyString ())).thenReturn (spell); 
			}
		
		doReturn (spells).when (db).getSpell ();

		final List<Plane> planes = new ArrayList<Plane> ();
		for (int n = 0; n < 2; n++)
		{
			final Plane plane = new Plane ();
			plane.setPlaneNumber (n);
			plane.getPlaneRacesTitle ().add (createLanguageText (Language.ENGLISH, "Plane " + n + " races"));
			planes.add (plane);
			
			when (db.findPlane (eq (n), anyString ())).thenReturn (plane);
		}
		
		doReturn (planes).when (db).getPlane ();
		
		final List<RaceEx> races = new ArrayList<RaceEx> ();
		for (int plane = 0; plane < 2; plane++)
			for (int raceNo = 0; raceNo < 5; raceNo++)
			{
				final RaceEx race = new RaceEx ();
				race.setRaceID ("RC" + plane + raceNo);
				race.setNativePlane (plane);
				race.getRaceNameSingular ().add (createLanguageText (Language.ENGLISH, "Race " + race.getRaceID ()));
				races.add (race);
			}
		
		doReturn (races).when (db).getRaces ();

		// Players
		final PlayerDescription pd = new PlayerDescription ();
		pd.setHuman (true);
		pd.setPlayerName ("Mr. Blah");
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		pub.setWizardID ("");
		
		final MomTransientPlayerPublicKnowledge trans = new MomTransientPlayerPublicKnowledge ();

		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final PlayerPublicDetails ourPlayer = new PlayerPublicDetails (pd, pub, trans);
		players.add (ourPlayer);
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (eq (players), eq (3), anyString ())).thenReturn (ourPlayer);
		
		// Mock entries from the language XML
		final Simple simpleLang = new Simple ();
		simpleLang.getCancel ().add (createLanguageText (Language.ENGLISH, "Cancel"));
		simpleLang.getOk ().add (createLanguageText (Language.ENGLISH, "OK"));
		simpleLang.getAnd ().add (createLanguageText (Language.ENGLISH, "And"));
		
		final TurnSystems turnSystemsLang = new TurnSystems ();
		turnSystemsLang.getOnePlayerAtATime ().add (createLanguageText (Language.ENGLISH, "One player at a time"));
		
		// NEW GAME PANEL
		final NewGameScreen newGameScreenLang = new NewGameScreen ();
		newGameScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "New Game"));
		newGameScreenLang.getHumanOpponents ().add (createLanguageText (Language.ENGLISH, "Human Opponents"));
		newGameScreenLang.getAiOpponents ().add (createLanguageText (Language.ENGLISH, "AI Opponents"));
		newGameScreenLang.getMapSize ().add (createLanguageText (Language.ENGLISH, "Map Size and makeup"));
		newGameScreenLang.getLandProportion ().add (createLanguageText (Language.ENGLISH, "Land Proportion"));
		newGameScreenLang.getNodes ().add (createLanguageText (Language.ENGLISH, "Nodes"));
		newGameScreenLang.getDifficulty ().add (createLanguageText (Language.ENGLISH, "Difficulty"));
		newGameScreenLang.getTurnSystem ().add (createLanguageText (Language.ENGLISH, "Turn System"));
		newGameScreenLang.getFogOfWar ().add (createLanguageText (Language.ENGLISH, "Fog of War"));
		newGameScreenLang.getUnitSettings ().add (createLanguageText (Language.ENGLISH, "Unit Settings"));
		newGameScreenLang.getSpellSettings ().add (createLanguageText (Language.ENGLISH, "Spell Settings"));
		newGameScreenLang.getDebugOptions ().add (createLanguageText (Language.ENGLISH, "Use any debug options?"));
		newGameScreenLang.getGameName ().add (createLanguageText (Language.ENGLISH, "Game Name"));
		newGameScreenLang.getCustomize ().add (createLanguageText (Language.ENGLISH, "Customize?"));
		
		// CUSTOM MAP SIZE PANEL
		final CustomMapSizeTab customMapSizeTabLang = new CustomMapSizeTab ();
		customMapSizeTabLang.getTitle ().add (createLanguageText (Language.ENGLISH, "New Game: Custom Map Size"));
		customMapSizeTabLang.getMapSize ().add (createLanguageText (Language.ENGLISH, "Map Size"));
		customMapSizeTabLang.getWrapsLeftRight ().add (createLanguageText (Language.ENGLISH, "Map wraps Left-Right?"));
		customMapSizeTabLang.getWrapsTopBottom ().add (createLanguageText (Language.ENGLISH, "Map wraps Top-Bottom?"));
		customMapSizeTabLang.getZones ().add (createLanguageText (Language.ENGLISH, "Terrain generator splits map into zones with size approximately"));
		customMapSizeTabLang.getNormalLairCount ().add (createLanguageText (Language.ENGLISH, "Number of normal strength lairs"));
		customMapSizeTabLang.getWeakLairCount ().add (createLanguageText (Language.ENGLISH, "Number of weak strength lairs"));
		customMapSizeTabLang.getTowers ().add (createLanguageText (Language.ENGLISH, "Number of Towers of Wizardry"));
		customMapSizeTabLang.getTowersSeparation ().add (createLanguageText (Language.ENGLISH, "Minimum separation between Towers"));
		customMapSizeTabLang.getContinental ().add (createLanguageText (Language.ENGLISH, "Chance that raider cities are the race chosen for the continent"));
		customMapSizeTabLang.getCitySeparation ().add (createLanguageText (Language.ENGLISH, "Minimum separation between Cities"));
		newGameScreenLang.setCustomMapSizeTab (customMapSizeTabLang);
		
		// CUSTOM LAND PROPORTION PANEL
		final CustomLandProportionTab customLandProportionTabLang = new CustomLandProportionTab ();
		customLandProportionTabLang.getTitle ().add (createLanguageText (Language.ENGLISH, "New Game: Custom Land Proportion"));
		customLandProportionTabLang.getPercentageMapIsLand ().add (createLanguageText (Language.ENGLISH, "% of map is land"));
		customLandProportionTabLang.getHillsProportion ().add (createLanguageText (Language.ENGLISH, "% of land is hills"));
		customLandProportionTabLang.getMountainsProportion ().add (createLanguageText (Language.ENGLISH, "% of hills are mountains"));
		customLandProportionTabLang.getTreesProportion ().add (createLanguageText (Language.ENGLISH, "% of land is trees, split into areas"));
		customLandProportionTabLang.getTreeAreaTileCountPrefix ().add (createLanguageText (Language.ENGLISH, "each approximately"));
		customLandProportionTabLang.getTreeAreaTileCountSuffix ().add (createLanguageText (Language.ENGLISH, "tiles"));
		customLandProportionTabLang.getDesertProportion ().add (createLanguageText (Language.ENGLISH, "% of land is desert, split into areas"));
		customLandProportionTabLang.getDesertAreaTileCountPrefix ().add (createLanguageText (Language.ENGLISH, "each approximately"));
		customLandProportionTabLang.getDesertAreaTileCountSuffix ().add (createLanguageText (Language.ENGLISH, "tiles"));
		customLandProportionTabLang.getSwampProportion ().add (createLanguageText (Language.ENGLISH, "% of land is swamp, split into areas"));
		customLandProportionTabLang.getSwampAreaTileCountPrefix ().add (createLanguageText (Language.ENGLISH, "each approximately"));
		customLandProportionTabLang.getSwampAreaTileCountSuffix ().add (createLanguageText (Language.ENGLISH, "tiles"));
		customLandProportionTabLang.getTundraPrefix ().add (createLanguageText (Language.ENGLISH, "Tundra appears"));
		customLandProportionTabLang.getTundraSuffix ().add (createLanguageText (Language.ENGLISH, "tiles from the edge of the map"));
		customLandProportionTabLang.getRivers ().add (createLanguageText (Language.ENGLISH, "Number of rivers on each plane"));
		customLandProportionTabLang.getArcanusPrefix ().add (createLanguageText (Language.ENGLISH, "Tiles on Arcanus have a 1 in"));
		customLandProportionTabLang.getArcanusSuffix ().add (createLanguageText (Language.ENGLISH, "chance of containing a mineral deposit"));
		customLandProportionTabLang.getMyrrorPrefix ().add (createLanguageText (Language.ENGLISH, "Tiles on Myrror have a 1 in"));
		customLandProportionTabLang.getMyrrorSuffix ().add (createLanguageText (Language.ENGLISH, "chance of containing a mineral deposit"));
		newGameScreenLang.setCustomLandProportionTab (customLandProportionTabLang);
		
		// CUSTOM NODES PANEL
		final CustomNodesTab customNodesTabLang = new CustomNodesTab ();
		customNodesTabLang.getTitle ().add (createLanguageText (Language.ENGLISH, "New Game: Custom Nodes"));
		customNodesTabLang.getMagicPowerPrefix ().add (createLanguageText (Language.ENGLISH, "Each square of node aura generates"));
		customNodesTabLang.getMagicPowerSuffix ().add (createLanguageText (Language.ENGLISH, "/2 magic power"));
		customNodesTabLang.getArcanusCount ().add (createLanguageText (Language.ENGLISH, "Number of nodes on Arcanus"));
		customNodesTabLang.getArcanusNodeAuraPrefix ().add (createLanguageText (Language.ENGLISH, "Each node on Arcanus has an aura between"));
		customNodesTabLang.getArcanusNodeAuraSuffix ().add (createLanguageText (Language.ENGLISH, "squares"));
		customNodesTabLang.getMyrrorCount ().add (createLanguageText (Language.ENGLISH, "Number of nodes on Myrror"));
		customNodesTabLang.getMyrrorNodeAuraPrefix ().add (createLanguageText (Language.ENGLISH, "Each node on Myrror has an aura between"));
		customNodesTabLang.getMyrrorNodeAuraSuffix ().add (createLanguageText (Language.ENGLISH, "squares"));
		newGameScreenLang.setCustomNodesTab (customNodesTabLang);

		// CUSTOM DIFFICULTY PANEL (1 of 3)
		final CustomDifficultyTab1 customDifficultyTab1Lang = new CustomDifficultyTab1 ();
		customDifficultyTab1Lang.getTitle ().add (createLanguageText (Language.ENGLISH, "New Game: Custom Difficulty (1 of 2)"));
		customDifficultyTab1Lang.getSpellPicks ().add (createLanguageText (Language.ENGLISH, "Spell picks:"));
		customDifficultyTab1Lang.getHumanSpellPicks ().add (createLanguageText (Language.ENGLISH, "Human"));
		customDifficultyTab1Lang.getAiSpellPicks ().add (createLanguageText (Language.ENGLISH, "AI"));
		customDifficultyTab1Lang.getGold ().add (createLanguageText (Language.ENGLISH, "Gold:"));
		customDifficultyTab1Lang.getHumanGold ().add (createLanguageText (Language.ENGLISH, "Human"));
		customDifficultyTab1Lang.getAiGold ().add (createLanguageText (Language.ENGLISH, "AI"));
		customDifficultyTab1Lang.getAiPopulationGrowthRateMultiplier ().add (createLanguageText (Language.ENGLISH, "AI population growth rate multiplier:"));
		customDifficultyTab1Lang.getAiPopulationGrowthRateMultiplierWizards ().add (createLanguageText (Language.ENGLISH, "Wizards"));
		customDifficultyTab1Lang.getAiPopulationGrowthRateMultiplierRaiders ().add (createLanguageText (Language.ENGLISH, "Raiders"));
		customDifficultyTab1Lang.getAiProductionRateMultiplier ().add (createLanguageText (Language.ENGLISH, "AI production rate multiplier:"));
		customDifficultyTab1Lang.getAiProductionRateMultiplierWizards ().add (createLanguageText (Language.ENGLISH, "Wizards"));
		customDifficultyTab1Lang.getAiProductionRateMultiplierRaiders ().add (createLanguageText (Language.ENGLISH, "Raiders"));
		customDifficultyTab1Lang.getAiSpellResearchMultiplier ().add (createLanguageText (Language.ENGLISH, "AI spell research multiplier:"));
		customDifficultyTab1Lang.getAiUpkeepMultiplier ().add (createLanguageText (Language.ENGLISH, "AI upkeep multiplier:"));
		customDifficultyTab1Lang.getCustomWizards ().add (createLanguageText (Language.ENGLISH, "Allow custom wizards?"));
		customDifficultyTab1Lang.getEachWizardOnlyOnce ().add (createLanguageText (Language.ENGLISH, "Each wizard can be chosen only once?"));
		customDifficultyTab1Lang.getFameRazingPenalty ().add (createLanguageText (Language.ENGLISH, "Fame penalty for razing captured cities?"));
		customDifficultyTab1Lang.getWizardCitySize ().add (createLanguageText (Language.ENGLISH, "Wizards' cities start at size"));
		customDifficultyTab1Lang.getMaxCitySize ().add (createLanguageText (Language.ENGLISH, "Maximum city size"));
		customDifficultyTab1Lang.getRaiderCityCount ().add (createLanguageText (Language.ENGLISH, "Number of neutral/raider cities"));
		customDifficultyTab1Lang.getRaiderCitySizePrefix ().add (createLanguageText (Language.ENGLISH, "Neutral/raider cities start between size"));
		customDifficultyTab1Lang.getRaiderCityGrowthPrefix ().add (createLanguageText (Language.ENGLISH, "and can grow up to"));
		customDifficultyTab1Lang.getRaiderCityGrowthSuffix ().add (createLanguageText (Language.ENGLISH, "larger than their starting size"));
		newGameScreenLang.setCustomDifficultyTab1 (customDifficultyTab1Lang);
		
		// CUSTOM DIFFICULTY PANEL (2 of 3)
		final CustomDifficultyTab2 customDifficultyTab2Lang = new CustomDifficultyTab2 ();
		customDifficultyTab2Lang.getTitle ().add (createLanguageText (Language.ENGLISH, "New Game: Custom Difficulty (2 of 2)"));
		customDifficultyTab2Lang.getTowerMonsters ().add (createLanguageText (Language.ENGLISH, "Towers: Monsters"));
		customDifficultyTab2Lang.getTowerTreasure ().add (createLanguageText (Language.ENGLISH, "Towers: Treasure"));
		customDifficultyTab2Lang.getNormalLairs ().add (createLanguageText (Language.ENGLISH, "Normal strength lairs:"));
		customDifficultyTab2Lang.getNormalArcanusLairMonsters ().add (createLanguageText (Language.ENGLISH, "On Arcanus: Monsters"));
		customDifficultyTab2Lang.getNormalArcanusLairTreasure ().add (createLanguageText (Language.ENGLISH, "On Arcanus: Treasure"));
		customDifficultyTab2Lang.getNormalMyrrorLairMonsters ().add (createLanguageText (Language.ENGLISH, "On Myrror: Monsters"));
		customDifficultyTab2Lang.getNormalMyrrorLairTreasure ().add (createLanguageText (Language.ENGLISH, "On Myrror: Treasure"));
		customDifficultyTab2Lang.getWeakLairs ().add (createLanguageText (Language.ENGLISH, "Weak strength lairs:"));
		customDifficultyTab2Lang.getWeakArcanusLairMonsters ().add (createLanguageText (Language.ENGLISH, "On Arcanus: Monsters"));
		customDifficultyTab2Lang.getWeakArcanusLairTreasure ().add (createLanguageText (Language.ENGLISH, "On Arcanus: Treasure"));
		customDifficultyTab2Lang.getWeakMyrrorLairMonsters ().add (createLanguageText (Language.ENGLISH, "On Myrror: Monsters"));
		customDifficultyTab2Lang.getWeakMyrrorLairTreasure ().add (createLanguageText (Language.ENGLISH, "On Myrror: Treasure"));
		newGameScreenLang.setCustomDifficultyTab2 (customDifficultyTab2Lang);
		
		// CUSTOM DIFFICULTY PANEL (3 of 3)
		final CustomDifficultyTab3 customDifficultyTab3Lang = new CustomDifficultyTab3 ();
		customDifficultyTab3Lang.getTitle ().add (createLanguageText (Language.ENGLISH, "New Game: Custom Node Difficulty"));
		customDifficultyTab3Lang.getArcanusNodeMonsters ().add (createLanguageText (Language.ENGLISH, "On Arcanus: Monsters"));
		customDifficultyTab3Lang.getArcanusNodeTreasure ().add (createLanguageText (Language.ENGLISH, "On Arcanus: Treasure"));
		customDifficultyTab3Lang.getMyrrorNodeMonsters ().add (createLanguageText (Language.ENGLISH, "On Myrror: Monsters"));
		customDifficultyTab3Lang.getMyrrorNodeTreasure ().add (createLanguageText (Language.ENGLISH, "On Myrror: Treasure"));
		newGameScreenLang.setCustomDifficultyTab3 (customDifficultyTab3Lang);
		
		// CUSTOM FOG OF WAR PANEL
		final CustomFogOfWarTab customFogOfWarTabLang = new CustomFogOfWarTab ();
		customFogOfWarTabLang.getTitle ().add (createLanguageText (Language.ENGLISH, "New Game: Custom Fog of War"));
		customFogOfWarTabLang.getAlways ().add (createLanguageText (Language.ENGLISH, "Always see once seen"));
		customFogOfWarTabLang.getRemember ().add (createLanguageText (Language.ENGLISH, "Remember as last seen"));
		customFogOfWarTabLang.getForget ().add (createLanguageText (Language.ENGLISH, "Forget"));
		customFogOfWarTabLang.getTerrainNodesAuras ().add (createLanguageText (Language.ENGLISH, "Terrain, Nodes and Auras"));
		customFogOfWarTabLang.getCitiesSpellsCombatAreaEffects ().add (createLanguageText (Language.ENGLISH, "Cities, Combat Spells and Combat Area Effects"));
		customFogOfWarTabLang.getConstructing ().add (createLanguageText (Language.ENGLISH, "Can see what enemy cities are constructing?"));
		customFogOfWarTabLang.getUnits ().add (createLanguageText (Language.ENGLISH, "Units"));
		newGameScreenLang.setCustomFogOfWarTab (customFogOfWarTabLang);
		
		// CUSTOM UNIT SETTINGS PANEL
		final CustomUnitsTab customUnitsTabLang = new CustomUnitsTab ();
		customUnitsTabLang.getTitle ().add (createLanguageText (Language.ENGLISH, "New Game: Custom Unit Settings"));
		customUnitsTabLang.getMaxPerGridCell ().add (createLanguageText (Language.ENGLISH, "Maximum units per grid cell"));
		customUnitsTabLang.getCanExceedMaximumUnitsDuringCombat ().add (createLanguageText (Language.ENGLISH, "Can temporarily exceed maximum units per map cell during combat (e.g. with Phantom Warriors, Earth/Air/Fire Elementals)"));
		customUnitsTabLang.getMaxHeroes ().add (createLanguageText (Language.ENGLISH, "Maximum heroes at a time"));
		customUnitsTabLang.getRollHeroSkillsAtStartOfGame ().add (createLanguageText (Language.ENGLISH, "Hero random skills are rolled at the start of the game (so cannot reroll by reloading the game just before spell finishes casting)"));
		customUnitsTabLang.getMaxHeroItemBonuses ().add (createLanguageText (Language.ENGLISH, "Maximum bonuses on a hero item"));
		customUnitsTabLang.getMaxHeroItemSpellCharges ().add (createLanguageText (Language.ENGLISH, "Maximum spell charges on a hero item"));
		customUnitsTabLang.getMaxHeroItemsInBank ().add (createLanguageText (Language.ENGLISH, "Maximum hero items in storage"));
		customUnitsTabLang.getUnlimited ().add (createLanguageText (Language.ENGLISH, "(blank = unlimited)"));
		newGameScreenLang.setCustomUnitsTab (customUnitsTabLang);
		
		// CUSTOM SPELL SETTINGS PANEL
		final CustomSpellsTab customSpellsTabLang = new CustomSpellsTab ();
		customSpellsTabLang.getTitle ().add (createLanguageText (Language.ENGLISH, "New Game: Custom Spell Settings"));
		customSpellsTabLang.getSwitchResearch ().add (createLanguageText (Language.ENGLISH, "Allow switching which spell is being researched?"));
		customSpellsTabLang.getSwitchResearchNo ().add (createLanguageText (Language.ENGLISH, "No - must finish current research first"));
		customSpellsTabLang.getSwitchResearchNotStarted ().add (createLanguageText (Language.ENGLISH, "Only if current spell research not started"));
		customSpellsTabLang.getSwitchResearchLose ().add (createLanguageText (Language.ENGLISH, "Yes, but lose any current research"));
		customSpellsTabLang.getSwitchResearchFreely ().add (createLanguageText (Language.ENGLISH, "Yes, can switch freely with no penalty"));
		customSpellsTabLang.getBooksToObtainFirstReduction ().add (createLanguageText (Language.ENGLISH, "Spell books to obtain first reduction"));
		customSpellsTabLang.getEachBook ().add (createLanguageText (Language.ENGLISH, "Each book gives"));
		customSpellsTabLang.getCastingReductionPrefix ().add (createLanguageText (Language.ENGLISH, "a"));
		customSpellsTabLang.getCastingReductionSuffix ().add (createLanguageText (Language.ENGLISH, "% reduction in casting cost"));
		customSpellsTabLang.getResearchBonusPrefix ().add (createLanguageText (Language.ENGLISH, "and a"));
		customSpellsTabLang.getResearchBonusSuffix ().add (createLanguageText (Language.ENGLISH, "% bonus to research"));
		customSpellsTabLang.getCastingReductionCombinationAdd ().add (createLanguageText (Language.ENGLISH, "Casting cost reductions are added together"));
		customSpellsTabLang.getCastingReductionCombinationMultiply ().add (createLanguageText (Language.ENGLISH, "Casting cost reductions are multiplied together"));
		customSpellsTabLang.getResearchBonusCombinationAdd ().add (createLanguageText (Language.ENGLISH, "Research bonuses are added together"));
		customSpellsTabLang.getResearchBonusCombinationMultiply ().add (createLanguageText (Language.ENGLISH, "Research bonuses are multiplied together"));
		customSpellsTabLang.getCastingReductionCap ().add (createLanguageText (Language.ENGLISH, "Casting cost reduction is capped at"));
		customSpellsTabLang.getResearchBonusCap ().add (createLanguageText (Language.ENGLISH, "Research bonus is capped at"));
		customSpellsTabLang.getStolenFromFortress ().add (createLanguageText (Language.ENGLISH, "Spells stolen when banishing wizard"));
		newGameScreenLang.setCustomSpellsTab (customSpellsTabLang);
		
		// DEBUG OPTIONS PANEL
		final CustomDebugTab customDebugTabLang = new CustomDebugTab ();
		customDebugTabLang.getTitle ().add (createLanguageText (Language.ENGLISH, "New Game: Debug Options"));
		customDebugTabLang.getDisableFogOfWar ().add (createLanguageText (Language.ENGLISH, "Disable Fog of War (as if all players have Nature Awareness cast all the time)"));
		newGameScreenLang.setCustomDebugTab (customDebugTabLang);
		
		// WIZARD SELECTION PANEL
		final ChooseWizardScreen chooseWizardScreenLang = new ChooseWizardScreen ();
		chooseWizardScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Select Wizard"));
		chooseWizardScreenLang.getCustom ().add (createLanguageText (Language.ENGLISH, "Custom"));

		// PORTRAIT SELECTION PANEL (for custom wizards)
		final ChoosePortraitScreen choosePortraitScreenLang = new ChoosePortraitScreen ();
		choosePortraitScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Select Picture"));
		choosePortraitScreenLang.getCustom ().add (createLanguageText (Language.ENGLISH, "Custom"));
		
		// FLAG COLOUR PANEL (for custom wizards with custom portraits)
		final ChooseFlagColourScreen chooseFlagColourScreenLang = new ChooseFlagColourScreen ();
		chooseFlagColourScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Select Flag Colour"));
		chooseFlagColourScreenLang.getRed ().add (createLanguageText (Language.ENGLISH, "Red"));
		chooseFlagColourScreenLang.getGreen ().add (createLanguageText (Language.ENGLISH, "Green"));
		chooseFlagColourScreenLang.getBlue ().add (createLanguageText (Language.ENGLISH, "Blue"));
		
		// CUSTOM PICKS PANEL (for custom wizards)
		final CustomPicksScreen customPicksScreenLang = new CustomPicksScreen ();
		customPicksScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Select Custom Picks"));
		
		// FREE SPELL SELECTION PANEL
		final ChooseInitialSpellsScreen chooseInitialSpellsScreenLang = new ChooseInitialSpellsScreen ();
		chooseInitialSpellsScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Select MAGIC_REALM Spells"));
		
		// RACE SELECTION PANEL
		final ChooseRaceScreen chooseRaceScreenLang = new ChooseRaceScreen ();
		chooseRaceScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Choose Race"));

		// WAITING TO OTHER PLAYERS TO JOIN PANEL
		final WaitForPlayersToJoinScreen waitForPlayersToJoinScreenLang = new WaitForPlayersToJoinScreen ();
		waitForPlayersToJoinScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Waiting for other Players to Join"));
		waitForPlayersToJoinScreenLang.getListColumnName ().add (createLanguageText (Language.ENGLISH, "Name"));
		waitForPlayersToJoinScreenLang.getListColumnWizard ().add (createLanguageText (Language.ENGLISH, "Wizard"));
		waitForPlayersToJoinScreenLang.getListColumnHumanOrAI ().add (createLanguageText (Language.ENGLISH, "Human or AI?"));
		waitForPlayersToJoinScreenLang.getHuman ().add (createLanguageText (Language.ENGLISH, "Human"));
		waitForPlayersToJoinScreenLang.getAi ().add (createLanguageText (Language.ENGLISH, "AI"));
		waitForPlayersToJoinScreenLang.getCustom ().add (createLanguageText (Language.ENGLISH, "Custom"));
		
		final WizardClientUtils wizardClientUtils = mock (WizardClientUtils.class);
		when (wizardClientUtils.getPlayerName (ourPlayer)).thenReturn (pd.getPlayerName ());

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSimple ()).thenReturn (simpleLang);
		when (lang.getTurnSystems ()).thenReturn (turnSystemsLang);
		when (lang.getNewGameScreen ()).thenReturn (newGameScreenLang);
		when (lang.getChooseWizardScreen ()).thenReturn (chooseWizardScreenLang);
		when (lang.getChoosePortraitScreen ()).thenReturn (choosePortraitScreenLang);
		when (lang.getChooseFlagColourScreen ()).thenReturn (chooseFlagColourScreenLang);
		when (lang.getCustomPicksScreen ()).thenReturn (customPicksScreenLang);
		when (lang.getChooseInitialSpellsScreen ()).thenReturn (chooseInitialSpellsScreenLang);
		when (lang.getChooseRaceScreen ()).thenReturn (chooseRaceScreenLang);
		when (lang.getWaitForPlayersToJoinScreen ()).thenReturn (waitForPlayersToJoinScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
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
		
		// Default new game settings
		final AvailableDatabase newGameDB = dbs.getMomimeXmlDatabase ().get (0);
		
		final OverlandMapSize overlandMapSize = new OverlandMapSize ();
		overlandMapSize.setOverlandMapSizeID ("60x40");
		newGameDB.getOverlandMapSize ().add (overlandMapSize);
		
		final LandProportion landProportion = new LandProportion ();
		landProportion.setLandProportionID ("LP01");
		newGameDB.getLandProportion ().add (landProportion);
		
		final NodeStrength nodeStrength = new NodeStrength ();
		nodeStrength.setNodeStrengthID ("NS01");
		newGameDB.getNodeStrength ().add (nodeStrength);

		final DifficultyLevel difficulty = new DifficultyLevel ();
		difficulty.setDifficultyLevelID ("DL01");
		difficulty.setCustomWizards (true);
		difficulty.setHumanSpellPicks (11);
		newGameDB.getDifficultyLevel ().add (difficulty);
		
		final FogOfWarSetting fowSetting = new FogOfWarSetting ();
		fowSetting.setFogOfWarSettingID ("FOW01");
		newGameDB.getFogOfWarSetting ().add (fowSetting);
		
		final UnitSetting unitSettings = new UnitSetting ();
		unitSettings.setUnitSettingID ("US01");
		newGameDB.getUnitSetting ().add (unitSettings);
		
		final SpellSetting spellSettings = new SpellSetting ();
		spellSettings.setSpellSettingID ("SS01");
		newGameDB.getSpellSetting ().add (spellSettings);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setDifficultyLevel (difficulty);
		when (client.getSessionDescription ()).thenReturn (sd);
		
		// Server connection
		when (client.getServerConnection ()).thenReturn (mock (ClientToServerConnection.class));

		// Layouts
		final Unmarshaller unmarshaller = createXmlLayoutUnmarshaller ();
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
		final XmlLayoutContainerEx flagColourLayout		= (XmlLayoutContainerEx) unmarshaller.unmarshal (getClass ().getResource ("/momime.client.ui.frames/NewGameUI-FlagColour.xml"));
		final XmlLayoutContainerEx picksLayout				= (XmlLayoutContainerEx) unmarshaller.unmarshal (getClass ().getResource ("/momime.client.ui.frames/NewGameUI-Picks.xml"));
		final XmlLayoutContainerEx waitLayout					= (XmlLayoutContainerEx) unmarshaller.unmarshal (getClass ().getResource ("/momime.client.ui.frames/NewGameUI-Wait.xml"));
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
		flagColourLayout.buildMaps ();
		picksLayout.buildMaps ();
		waitLayout.buildMaps ();
		
		// Set up form
		final NewGameUI game = new NewGameUI ();
		game.setUtils (utils);
		game.setLanguageHolder (langHolder);
		game.setLanguageChangeMaster (langMaster);
		game.setClient (client);
		game.setMultiplayerSessionUtils (multiplayerSessionUtils);
		game.setWizardClientUtils (wizardClientUtils);
		game.setPlayerPickUtils (new PlayerPickUtilsImpl ());
		game.setRandomUtils (new RandomUtilsImpl ());
		game.setTextUtils (new TextUtilsImpl ());
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
		game.setNewGameLayoutFlagColour (flagColourLayout);
		game.setNewGameLayoutPicks (picksLayout);
		game.setNewGameLayoutWait (waitLayout);
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
	 * Tests the "custom flag colour" screen (if a custom wizard with a custom portrait was chosen)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testNewGameUI_CustomFlagColour () throws Exception
	{
		final NewGameUI game = createNewGameUI ();
		
		// Display form
		game.setVisible (true);
		game.showCustomFlagColourPanel ();
		Thread.sleep (5000);
		game.setVisible (false);
	}
	
	/**
	 * Tests the "custom picks" screen (if a custom wizard was chosen)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testNewGameUI_CustomPicks () throws Exception
	{
		final NewGameUI game = createNewGameUI ();
		
		// Display form
		game.setVisible (true);
		game.afterJoinedSession ();		// Need this too to create the retort buttons and bookshelves
		game.showCustomPicksPanel ();
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
		
		// Set up some dummy players

		// Display form
		game.setVisible (true);
		game.showWaitPanel ();
		Thread.sleep (5000);
		game.setVisible (false);
	}
}