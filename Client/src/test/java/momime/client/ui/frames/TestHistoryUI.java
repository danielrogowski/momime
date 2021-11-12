package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.HistoryScreen;
import momime.client.languages.database.MapButtonBar;
import momime.client.languages.database.Month;
import momime.client.languages.database.OverlandMapScreen;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.WizardClientUtils;
import momime.common.database.Language;
import momime.common.messages.MomGeneralPublicKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomTransientPlayerPublicKnowledge;

/**
 * Tests the HistoryUI class
 */
@ExtendWith(MockitoExtension.class)
public final class TestHistoryUI extends ClientTestData
{
	/**
	 * Tests the HistoryUI form when values are within normal bounds
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHistoryUI_WithinBounds () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final HistoryScreen historyScreenLang = new HistoryScreen ();
		historyScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "History of Wizards' Power"));

		final MapButtonBar mapButtonBarLang = new MapButtonBar ();
		mapButtonBarLang.getTurn ().add (createLanguageText (Language.ENGLISH, "MONTH YEAR (Turn TURN)"));
		
		final OverlandMapScreen overlandMapScreen = new OverlandMapScreen ();
		overlandMapScreen.setMapButtonBar (mapButtonBarLang);
		
		final Month month = new Month ();
		month.setMonthNumber (1);
		month.getName ().add (createLanguageText (Language.ENGLISH, "January"));
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getHistoryScreen ()).thenReturn (historyScreenLang);
		when (lang.getOverlandMapScreen ()).thenReturn (overlandMapScreen);
		when (lang.getMonth ()).thenReturn (Arrays.asList (month));
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// Turn number
		final MomClient client = mock (MomClient.class);
		
		final MomGeneralPublicKnowledge gpk = new MomGeneralPublicKnowledge ();
		gpk.setTurnNumber (1);
		
		when (client.getGeneralPublicKnowledge ()).thenReturn (gpk);
		
		// Players
		final WizardClientUtils wizardClientUtils = mock (WizardClientUtils.class);
		
		int wizardNo = 0;
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		for (final String flagColour : new String [] {"B2AAA6", "545D10", "A86E4E", "3C398C", "65483D", "D0CA7D", "571D56", "436243", "520101", "FBE270", "D48F1C", "E9BEA2", "900000", "042444"})
		{
			wizardNo++;
			final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
			final MomTransientPlayerPublicKnowledge trans = new MomTransientPlayerPublicKnowledge ();
			
			String wizardID = Integer.valueOf (wizardNo).toString ();
			while (wizardID.length () < 2)
				wizardID = "0" + wizardID;
			
			pub.setWizardID ("WZ" + wizardID);
			trans.setFlagColour (flagColour);
			
			for (int turnNumber = 0; turnNumber < 230; turnNumber++)
				pub.getPowerBaseHistory ().add (turnNumber + (wizardNo * 5));
			
			final PlayerPublicDetails player = new PlayerPublicDetails (null, pub, trans);
			players.add (player);
			
			when (wizardClientUtils.getPlayerName (player)).thenReturn ("Wizard " + wizardID);
		}
		
		when (client.getPlayers ()).thenReturn (players);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/HistoryUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final HistoryUI history = new HistoryUI ();
		history.setHistoryLayout (layout);
		history.setUtils (utils);
		history.setClient (client);
		history.setWizardClientUtils (wizardClientUtils);
		history.setLanguageHolder (langHolder);
		history.setLanguageChangeMaster (langMaster);
		history.setLargeFont (CreateFontsForTests.getLargeFont ());
		history.setSmallFont (CreateFontsForTests.getSmallFont ());
		
		// Display form		
		history.setVisible (true);
		history.updateTurnLabelText ();		// Must do this after .init (), or the label we're updating won't exist yet
		Thread.sleep (5000);
		history.setVisible (false);
	}

	/**
	 * Tests the HistoryUI form, where there is a score off the top of the chart
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHistoryUI_ScoreOverflow () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final HistoryScreen historyScreenLang = new HistoryScreen ();
		historyScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "History of Wizards' Power"));

		final MapButtonBar mapButtonBarLang = new MapButtonBar ();
		mapButtonBarLang.getTurn ().add (createLanguageText (Language.ENGLISH, "MONTH YEAR (Turn TURN)"));
		
		final OverlandMapScreen overlandMapScreen = new OverlandMapScreen ();
		overlandMapScreen.setMapButtonBar (mapButtonBarLang);
		
		final Month month = new Month ();
		month.setMonthNumber (1);
		month.getName ().add (createLanguageText (Language.ENGLISH, "January"));
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getHistoryScreen ()).thenReturn (historyScreenLang);
		when (lang.getOverlandMapScreen ()).thenReturn (overlandMapScreen);
		when (lang.getMonth ()).thenReturn (Arrays.asList (month));
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// Turn number
		final MomClient client = mock (MomClient.class);
		
		final MomGeneralPublicKnowledge gpk = new MomGeneralPublicKnowledge ();
		gpk.setTurnNumber (1);
		
		when (client.getGeneralPublicKnowledge ()).thenReturn (gpk);
		
		// Players
		final WizardClientUtils wizardClientUtils = mock (WizardClientUtils.class);
		
		int wizardNo = 0;
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		for (final String flagColour : new String [] {"B2AAA6", "545D10", "A86E4E", "3C398C", "65483D", "D0CA7D", "571D56", "436243", "520101", "FBE270", "D48F1C", "E9BEA2", "900000", "042444"})
		{
			wizardNo++;
			final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
			final MomTransientPlayerPublicKnowledge trans = new MomTransientPlayerPublicKnowledge ();
			
			String wizardID = Integer.valueOf (wizardNo).toString ();
			while (wizardID.length () < 2)
				wizardID = "0" + wizardID;
			
			pub.setWizardID ("WZ" + wizardID);
			trans.setFlagColour (flagColour);
			
			for (int turnNumber = 0; turnNumber < 250; turnNumber++)
				pub.getPowerBaseHistory ().add (turnNumber + (wizardNo * 5));
			
			final PlayerPublicDetails player = new PlayerPublicDetails (null, pub, trans);
			players.add (player);
			
			when (wizardClientUtils.getPlayerName (player)).thenReturn ("Wizard " + wizardID);
		}
		
		when (client.getPlayers ()).thenReturn (players);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/HistoryUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final HistoryUI history = new HistoryUI ();
		history.setHistoryLayout (layout);
		history.setUtils (utils);
		history.setClient (client);
		history.setWizardClientUtils (wizardClientUtils);
		history.setLanguageHolder (langHolder);
		history.setLanguageChangeMaster (langMaster);
		history.setLargeFont (CreateFontsForTests.getLargeFont ());
		history.setSmallFont (CreateFontsForTests.getSmallFont ());
		
		// Display form		
		history.setVisible (true);
		history.updateTurnLabelText ();		// Must do this after .init (), or the label we're updating won't exist yet
		Thread.sleep (5000);
		history.setVisible (false);
	}

	/**
	 * Tests the HistoryUI form, where there have been more turns than will fit
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHistoryUI_TurnOverflow () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final HistoryScreen historyScreenLang = new HistoryScreen ();
		historyScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "History of Wizards' Power"));

		final MapButtonBar mapButtonBarLang = new MapButtonBar ();
		mapButtonBarLang.getTurn ().add (createLanguageText (Language.ENGLISH, "MONTH YEAR (Turn TURN)"));
		
		final OverlandMapScreen overlandMapScreen = new OverlandMapScreen ();
		overlandMapScreen.setMapButtonBar (mapButtonBarLang);
		
		final Month month = new Month ();
		month.setMonthNumber (1);
		month.getName ().add (createLanguageText (Language.ENGLISH, "January"));
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getHistoryScreen ()).thenReturn (historyScreenLang);
		when (lang.getOverlandMapScreen ()).thenReturn (overlandMapScreen);
		when (lang.getMonth ()).thenReturn (Arrays.asList (month));
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// Turn number
		final MomClient client = mock (MomClient.class);
		
		final MomGeneralPublicKnowledge gpk = new MomGeneralPublicKnowledge ();
		gpk.setTurnNumber (1);
		
		when (client.getGeneralPublicKnowledge ()).thenReturn (gpk);
		
		// Players
		final WizardClientUtils wizardClientUtils = mock (WizardClientUtils.class);
		
		int wizardNo = 0;
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		for (final String flagColour : new String [] {"B2AAA6", "545D10", "A86E4E", "3C398C", "65483D", "D0CA7D", "571D56", "436243", "520101", "FBE270", "D48F1C", "E9BEA2", "900000", "042444"})
		{
			wizardNo++;
			final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
			final MomTransientPlayerPublicKnowledge trans = new MomTransientPlayerPublicKnowledge ();
			
			String wizardID = Integer.valueOf (wizardNo).toString ();
			while (wizardID.length () < 2)
				wizardID = "0" + wizardID;
			
			pub.setWizardID ("WZ" + wizardID);
			trans.setFlagColour (flagColour);
			
			for (int turnNumber = 0; turnNumber < 300; turnNumber++)
				pub.getPowerBaseHistory ().add ((turnNumber / 2) + (wizardNo * 5));
			
			final PlayerPublicDetails player = new PlayerPublicDetails (null, pub, trans);
			players.add (player);
			
			when (wizardClientUtils.getPlayerName (player)).thenReturn ("Wizard " + wizardID);
		}
		
		when (client.getPlayers ()).thenReturn (players);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/HistoryUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final HistoryUI history = new HistoryUI ();
		history.setHistoryLayout (layout);
		history.setUtils (utils);
		history.setClient (client);
		history.setWizardClientUtils (wizardClientUtils);
		history.setLanguageHolder (langHolder);
		history.setLanguageChangeMaster (langMaster);
		history.setLargeFont (CreateFontsForTests.getLargeFont ());
		history.setSmallFont (CreateFontsForTests.getSmallFont ());
		
		// Display form		
		history.setVisible (true);
		history.updateTurnLabelText ();		// Must do this after .init (), or the label we're updating won't exist yet
		Thread.sleep (5000);
		history.setVisible (false);
	}

	/**
	 * Tests the HistoryUI form, where both axes overflow
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHistoryUI_BothOverflow () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final HistoryScreen historyScreenLang = new HistoryScreen ();
		historyScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "History of Wizards' Power"));

		final MapButtonBar mapButtonBarLang = new MapButtonBar ();
		mapButtonBarLang.getTurn ().add (createLanguageText (Language.ENGLISH, "MONTH YEAR (Turn TURN)"));
		
		final OverlandMapScreen overlandMapScreen = new OverlandMapScreen ();
		overlandMapScreen.setMapButtonBar (mapButtonBarLang);
		
		final Month month = new Month ();
		month.setMonthNumber (1);
		month.getName ().add (createLanguageText (Language.ENGLISH, "January"));
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getHistoryScreen ()).thenReturn (historyScreenLang);
		when (lang.getOverlandMapScreen ()).thenReturn (overlandMapScreen);
		when (lang.getMonth ()).thenReturn (Arrays.asList (month));
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// Turn number
		final MomClient client = mock (MomClient.class);
		
		final MomGeneralPublicKnowledge gpk = new MomGeneralPublicKnowledge ();
		gpk.setTurnNumber (1);
		
		when (client.getGeneralPublicKnowledge ()).thenReturn (gpk);
		
		// Players
		final WizardClientUtils wizardClientUtils = mock (WizardClientUtils.class);
		
		int wizardNo = 0;
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		for (final String flagColour : new String [] {"B2AAA6", "545D10", "A86E4E", "3C398C", "65483D", "D0CA7D", "571D56", "436243", "520101", "FBE270", "D48F1C", "E9BEA2", "900000", "042444"})
		{
			wizardNo++;
			final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
			final MomTransientPlayerPublicKnowledge trans = new MomTransientPlayerPublicKnowledge ();
			
			String wizardID = Integer.valueOf (wizardNo).toString ();
			while (wizardID.length () < 2)
				wizardID = "0" + wizardID;
			
			pub.setWizardID ("WZ" + wizardID);
			trans.setFlagColour (flagColour);
			
			for (int turnNumber = 0; turnNumber < 300; turnNumber++)
				pub.getPowerBaseHistory ().add (turnNumber + (wizardNo * 5));
			
			final PlayerPublicDetails player = new PlayerPublicDetails (null, pub, trans);
			players.add (player);
			
			when (wizardClientUtils.getPlayerName (player)).thenReturn ("Wizard " + wizardID);
		}
		
		when (client.getPlayers ()).thenReturn (players);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/HistoryUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final HistoryUI history = new HistoryUI ();
		history.setHistoryLayout (layout);
		history.setUtils (utils);
		history.setClient (client);
		history.setWizardClientUtils (wizardClientUtils);
		history.setLanguageHolder (langHolder);
		history.setLanguageChangeMaster (langMaster);
		history.setLargeFont (CreateFontsForTests.getLargeFont ());
		history.setSmallFont (CreateFontsForTests.getSmallFont ());
		
		// Display form		
		history.setVisible (true);
		history.updateTurnLabelText ();		// Must do this after .init (), or the label we're updating won't exist yet
		Thread.sleep (5000);
		history.setVisible (false);
	}
}