package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.client.ClientTestData;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.common.database.DifficultyLevel;
import momime.common.database.LandProportion;
import momime.common.database.NodeStrength;
import momime.common.database.OverlandMapSize;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.TurnSystem;

import org.junit.Test;

import com.ndg.multiplayer.sessionbase.SessionAndPlayerDescriptions;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

/**
 * Tests the JoinGameUI class
 */
public final class TestJoinGameUI
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
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmJoinGame", "Title")).thenReturn ("Join Game");
		when (lang.findCategoryEntry ("frmJoinGame", "Refresh")).thenReturn ("Refresh");
		when (lang.findCategoryEntry ("frmJoinGame", "Join")).thenReturn ("Join");
		when (lang.findCategoryEntry ("frmJoinGame", "Cancel")).thenReturn ("Cancel");

		when (lang.findCategoryEntry ("frmJoinGame", "SessionsColumn0")).thenReturn ("Game Name");
		when (lang.findCategoryEntry ("frmJoinGame", "SessionsColumn1")).thenReturn ("Players");
		when (lang.findCategoryEntry ("frmJoinGame", "SessionsColumn2")).thenReturn ("Map, Land, Nodes, Difficulty");
		when (lang.findCategoryEntry ("frmJoinGame", "SessionsColumn3")).thenReturn ("Turn System");
		
		when (lang.findCategoryEntry ("NewGameFormTurnSystems", TurnSystem.ONE_PLAYER_AT_A_TIME.name ())).thenReturn ("One player at a time");
		when (lang.findOverlandMapSizeDescription ("MS03")).thenReturn ("Standard");
		when (lang.findLandProportionDescription ("LP03")).thenReturn ("Large");
		when (lang.findNodeStrengthDescription ("NS03")).thenReturn ("Powerful");
		when (lang.findDifficultyLevelDescription ("DL05")).thenReturn ("Impossible");
		when (lang.findCategoryEntry ("frmWaitForPlayersToJoin", "Custom")).thenReturn ("Custom");
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// Set up some dummy sessions
		final OverlandMapSize overlandMapSize = new OverlandMapSize ();
		overlandMapSize.setOverlandMapSizeID ("MS03");
		
		final LandProportion landProportion = new LandProportion ();
		landProportion.setLandProportionID ("LP03");
		
		final NodeStrength nodeStrength = new NodeStrength ();
		nodeStrength.setNodeStrengthID ("NS03");
		
		final DifficultyLevel difficultyLevel = new DifficultyLevel ();
		
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
		spd.setSessionDescription (sd);
		
		final List<SessionAndPlayerDescriptions> sessions = new ArrayList<SessionAndPlayerDescriptions> ();
		sessions.add (spd);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) ClientTestData.createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/JoinGameUI.xml"));
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