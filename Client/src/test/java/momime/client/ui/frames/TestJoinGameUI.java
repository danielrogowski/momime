package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.multiplayer.sessionbase.SessionAndPlayerDescriptions;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.JoinGameScreen;
import momime.client.languages.database.Simple;
import momime.client.languages.database.TurnSystems;
import momime.client.languages.database.WaitForPlayersToJoinScreen;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.common.database.DifficultyLevel;
import momime.common.database.LandProportion;
import momime.common.database.Language;
import momime.common.database.NodeStrength;
import momime.common.database.OverlandMapSize;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.TurnSystem;

/**
 * Tests the JoinGameUI class
 */
public final class TestJoinGameUI extends ClientTestData
{
	/**
	 * Tests the JoinGameUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testJoinGameUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final Simple simpleLang = new Simple ();
		simpleLang.getCancel ().add (createLanguageText (Language.ENGLISH, "Cancel"));
		
		final JoinGameScreen joinGameScreenLang = new JoinGameScreen ();
		joinGameScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Join Game"));
		joinGameScreenLang.getRefresh ().add (createLanguageText (Language.ENGLISH, "Refresh"));
		joinGameScreenLang.getJoin ().add (createLanguageText (Language.ENGLISH, "Join"));
		
		joinGameScreenLang.getSessionsColumnGameName ().add (createLanguageText (Language.ENGLISH, "Game Name"));
		joinGameScreenLang.getSessionsColumnPlayers ().add (createLanguageText (Language.ENGLISH, "Players"));
		joinGameScreenLang.getSessionsColumnSettings ().add (createLanguageText (Language.ENGLISH, "Map, Land, Nodes, Difficulty"));
		joinGameScreenLang.getSessionsColumnTurnSystem ().add (createLanguageText (Language.ENGLISH, "Turn System"));
		
		final TurnSystems turnSystemsLang = new TurnSystems ();
		turnSystemsLang.getOnePlayerAtATime ().add (createLanguageText (Language.ENGLISH, "One player at a time"));
		
		final WaitForPlayersToJoinScreen waitForPlayersToJoinScreenLang = new WaitForPlayersToJoinScreen ();
		waitForPlayersToJoinScreenLang.getCustom ().add (createLanguageText (Language.ENGLISH, "Custom"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSimple ()).thenReturn (simpleLang);
		when (lang.getJoinGameScreen ()).thenReturn (joinGameScreenLang);
		when (lang.getTurnSystems ()).thenReturn (turnSystemsLang);
		when (lang.getWaitForPlayersToJoinScreen ()).thenReturn (waitForPlayersToJoinScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// Set up some dummy sessions
		final OverlandMapSize overlandMapSize = new OverlandMapSize ();
		overlandMapSize.setOverlandMapSizeID ("MS03");
		overlandMapSize.getOverlandMapSizeDescription ().add (createLanguageText (Language.ENGLISH, "Standard"));
		
		final LandProportion landProportion = new LandProportion ();
		landProportion.setLandProportionID ("LP03");
		landProportion.getLandProportionDescription ().add (createLanguageText (Language.ENGLISH, "Large"));
		
		final NodeStrength nodeStrength = new NodeStrength ();
		nodeStrength.setNodeStrengthID ("NS03");
		nodeStrength.getNodeStrengthDescription ().add (createLanguageText (Language.ENGLISH, "Powerful"));
		
		final DifficultyLevel difficultyLevel = new DifficultyLevel ();
		difficultyLevel.setDifficultyLevelID ("DL05");
		difficultyLevel.getDifficultyLevelDescription ().add (createLanguageText (Language.ENGLISH, "Impossible"));
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setAiPlayerCount (4);
		sd.setMaxPlayers (10);
		sd.setSessionName ("Nigel's Game");
		sd.setOverlandMapSize (overlandMapSize);
		sd.setLandProportion (landProportion);
		sd.setNodeStrength (nodeStrength);
		sd.setDifficultyLevel (difficultyLevel);
		sd.setTurnSystem (TurnSystem.ONE_PLAYER_AT_A_TIME);
		
		final SessionAndPlayerDescriptions spd = new SessionAndPlayerDescriptions ();
		spd.setSessionDescription (sd);
		spd.getPlayer ().add (null);
		
		final List<SessionAndPlayerDescriptions> sessions = new ArrayList<SessionAndPlayerDescriptions> ();
		sessions.add (spd);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/JoinGameUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final JoinGameUI join = new JoinGameUI ();
		join.setJoinGameLayout (layout);
		join.setUtils (utils);
		join.setLanguageHolder (langHolder);
		join.setLanguageChangeMaster (langMaster);
		join.setTinyFont (CreateFontsForTests.getTinyFont ());
		join.setLargeFont (CreateFontsForTests.getLargeFont ());
		join.setSessions (sessions);
	
		// Display form		
		join.setVisible (true);
		Thread.sleep (5000);
		join.setVisible (false);
	}
}