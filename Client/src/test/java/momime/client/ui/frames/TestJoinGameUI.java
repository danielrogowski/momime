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
import momime.common.messages.MomSessionDescription;

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
		when (lang.findCategoryEntry ("frmJoinGame", "SessionsColumn2")).thenReturn ("Map Size");
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// Set up some dummy sessions
		final MomSessionDescription sd1 = new MomSessionDescription ();
		sd1.setAiPlayerCount (4);
		sd1.setMaxPlayers (10);
		sd1.setSessionName ("Nigel's Game");
		sd1.setMapSize (ClientTestData.createMapSizeData ());
		
		final SessionAndPlayerDescriptions spd1 = new SessionAndPlayerDescriptions ();
		spd1.setSessionDescription (sd1);
		spd1.getPlayer ().add (null);
		spd1.setSessionDescription (sd1);
		
		final List<SessionAndPlayerDescriptions> sessions = new ArrayList<SessionAndPlayerDescriptions> ();
		sessions.add (spd1);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) ClientTestData.createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/JoinGameUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final JoinGameUI join = new JoinGameUI ();
		join.setJoinGameLayout (layout);
		join.setUtils (utils);
		join.setLanguageHolder (langHolder);
		join.setLanguageChangeMaster (langMaster);
		join.setSmallFont (CreateFontsForTests.getSmallFont ());
		join.setLargeFont (CreateFontsForTests.getLargeFont ());
		join.setSessions (sessions);
	
		// Display form		
		join.setVisible (true);
		Thread.sleep (5000);
		join.setVisible (false);
	}
}