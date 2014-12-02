package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.xml.bind.Unmarshaller;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.database.AvailableDatabase;
import momime.client.database.NewGameDatabase;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.ui.fonts.CreateFontsForTests;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

/**
 * Tests the NewGameUI class
 */
public final class TestNewGameUI
{
	/**
	 * Tests the new card in the card layout
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testNewGameUI_New () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);

		when (lang.findCategoryEntry ("frmNewGame", "Cancel")).thenReturn ("Cancel");
		when (lang.findCategoryEntry ("frmNewGame", "OK")).thenReturn ("OK");
		
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
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
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
		when (client.getNewGameDatabase ()).thenReturn (dbs);
		
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
		game.setNewGameLayoutMain (mainLayout);
		game.setNewGameLayoutNew (newLayout);
		game.setSmallFont (CreateFontsForTests.getSmallFont ());
		game.setMediumFont (CreateFontsForTests.getMediumFont ());
		game.setLargeFont (CreateFontsForTests.getLargeFont ());

		// Display form
		game.setVisible (true);
		Thread.sleep (5000);
		game.setVisible (false);
	}
}