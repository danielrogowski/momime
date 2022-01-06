package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.datatype.DatatypeFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.multiplayer.sessionbase.PlayerType;
import com.ndg.multiplayer.sessionbase.SavedGamePoint;
import com.ndg.multiplayer.sessionbase.SavedGameSession;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.LoadGameScreen;
import momime.client.languages.database.Simple;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.common.database.DifficultyLevel;
import momime.common.database.LandProportion;
import momime.common.database.Language;
import momime.common.database.NodeStrength;
import momime.common.database.OverlandMapSize;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.TurnSystem;

/**
 * Tests the LoadGameUI class
 */
@ExtendWith(MockitoExtension.class)
public final class TestLoadGameUI extends ClientTestData
{
	/**
	 * @return Newly created LoadGameUI
	 * @throws Exception If there is a problem
	 */
	private final LoadGameUI createLoadGameUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final Simple simpleLang = new Simple ();
		simpleLang.getCancel ().add (createLanguageText (Language.ENGLISH, "Cancel"));
		
		final LoadGameScreen loadGameScreenLang = new LoadGameScreen ();
		loadGameScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Load Game"));
		loadGameScreenLang.getDeleteSavedGame ().add (createLanguageText (Language.ENGLISH, "Delete"));
		loadGameScreenLang.getSelectSavedGame ().add (createLanguageText (Language.ENGLISH, "Select"));
		loadGameScreenLang.getSelectSavePoint ().add (createLanguageText (Language.ENGLISH, "Load"));
		loadGameScreenLang.getBack ().add (createLanguageText (Language.ENGLISH, "Back"));
		
		loadGameScreenLang.getSavedGamesColumnGameName ().add (createLanguageText (Language.ENGLISH, "Game Name"));
		loadGameScreenLang.getSavedGamesColumnPlayers ().add (createLanguageText (Language.ENGLISH, "Players"));
		loadGameScreenLang.getSavedGamesColumnGameStarted ().add (createLanguageText (Language.ENGLISH, "Game Started"));
		loadGameScreenLang.getSavedGamesColumnLatestSave ().add (createLanguageText (Language.ENGLISH, "Latest Save"));
		
		loadGameScreenLang.getSavePointsColumnSavedAt ().add (createLanguageText (Language.ENGLISH, "Saved At"));
		loadGameScreenLang.getSavePointsColumnTurn ().add (createLanguageText (Language.ENGLISH, "Turn"));
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSimple ()).thenReturn (simpleLang);
		when (lang.getLoadGameScreen ()).thenReturn (loadGameScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/JoinGameUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final LoadGameUI load = new LoadGameUI ();
		load.setJoinGameLayout (layout);
		load.setUtils (utils);
		load.setLanguageHolder (langHolder);
		load.setLanguageChangeMaster (langMaster);
		load.setTinyFont (CreateFontsForTests.getTinyFont ());
		load.setLargeFont (CreateFontsForTests.getLargeFont ());

		return load;
	}
	
	/**
	 * Tests the LoadGameUI form displaying saved games
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testLoadGameUI_SavedGames () throws Exception
	{
		final LoadGameUI load = createLoadGameUI ();
		final DatatypeFactory datatypeFactory = DatatypeFactory.newInstance ();
		
		// Set up some dummy saved games
		final OverlandMapSize overlandMapSize = new OverlandMapSize ();
		overlandMapSize.setOverlandMapSizeID ("MS03");
		
		final LandProportion landProportion = new LandProportion ();
		landProportion.setLandProportionID ("LP03");
		
		final NodeStrength nodeStrength = new NodeStrength ();
		nodeStrength.setNodeStrengthID ("NS03");
		
		final DifficultyLevel difficultyLevel = new DifficultyLevel ();
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		pd1.setPlayerName ("Nigel");
		pd1.setPlayerType (PlayerType.HUMAN);
		
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (2);
		pd2.setPlayerName ("AI Wizard");
		pd2.setPlayerType (PlayerType.AI);
		
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (3);
		pd3.setPlayerName ("Raiders");
		pd3.setPlayerType (PlayerType.AI);
		
		final PlayerDescription pd4 = new PlayerDescription ();
		pd4.setPlayerID (4);
		pd4.setPlayerName ("Rampaging Monsters");
		pd4.setPlayerType (PlayerType.AI);
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setAiPlayerCount (4);
		sd.setMaxPlayers (10);
		sd.setSessionName ("Nigel's Game");
		sd.setOverlandMapSize (overlandMapSize);
		sd.setLandProportion (landProportion);
		sd.setNodeStrength (nodeStrength);
		sd.setDifficultyLevel (difficultyLevel);
		sd.setTurnSystem (TurnSystem.ONE_PLAYER_AT_A_TIME);
		sd.setStartedAt (datatypeFactory.newXMLGregorianCalendar (new GregorianCalendar ()));
		
		final SavedGameSession savedGame = new SavedGameSession ();
		savedGame.setSessionDescription (sd);
		savedGame.getPlayer ().add (pd1);
		savedGame.getPlayer ().add (pd2);
		savedGame.getPlayer ().add (pd3);
		savedGame.getPlayer ().add (pd4);
		savedGame.setLatestSavedAt (datatypeFactory.newXMLGregorianCalendar (new GregorianCalendar ()));
		savedGame.setLatestSavedGameIdentifier ("99");
		
		final List<SavedGameSession> savedGames = new ArrayList<SavedGameSession> ();
		savedGames.add (savedGame);
		
		// Display form		
		load.setVisible (true);
		load.setSavedGames (savedGames);
		Thread.sleep (5000);
		load.setVisible (false);
	}

	/**
	 * Tests the LoadGameUI form displaying save points
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testLoadGameUI_SavePoints () throws Exception
	{
		final LoadGameUI load = createLoadGameUI ();
		final DatatypeFactory datatypeFactory = DatatypeFactory.newInstance ();
		
		// Set up some dummy save points
		final SavedGamePoint savePoint = new SavedGamePoint ();
		savePoint.setSavedAt (datatypeFactory.newXMLGregorianCalendar (new GregorianCalendar ()));
		savePoint.setSavedGameIdentifier ("99");
		
		final List<SavedGamePoint> savePoints = new ArrayList<SavedGamePoint> ();
		savePoints.add (savePoint);
		
		// Display form		
		load.setVisible (true);
		load.setSavePoints (savePoints);
		Thread.sleep (5000);
		load.setVisible (false);
	}
}