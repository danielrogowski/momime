package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.utils.swing.NdgUIUtils;
import com.ndg.utils.swing.NdgUIUtilsImpl;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

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
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MomGeneralPublicKnowledge;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.utils.PlayerKnowledgeUtils;

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
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (mem);

		final WizardClientUtils wizardClientUtils = mock (WizardClientUtils.class);
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);

		int wizardNo = 0;
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		for (final String flagColour : new String [] {"B2AAA6", "545D10", "A86E4E", "3C398C", "65483D", "D0CA7D", "571D56", "436243", "520101", "FBE270", "D48F1C", "E9BEA2", "900000", "042444"})
		{
			wizardNo++;
			final MomTransientPlayerPublicKnowledge trans = new MomTransientPlayerPublicKnowledge ();
			final KnownWizardDetails wizardDetails = new KnownWizardDetails ();
			
			String wizardID = Integer.valueOf (wizardNo).toString ();
			while (wizardID.length () < 2)
				wizardID = "0" + wizardID;
			
			wizardDetails.setPlayerID (wizardNo);
			wizardDetails.setWizardID ("WZ" + wizardID);
			
			trans.setFlagColour (flagColour);
			
			for (int turnNumber = 0; turnNumber < 230; turnNumber++)
				wizardDetails.getPowerBaseHistory ().add (turnNumber + (wizardNo * 5));
			
			final PlayerDescription pd = new PlayerDescription ();
			pd.setPlayerID (wizardNo);
			
			final PlayerPublicDetails player = new PlayerPublicDetails (pd, null, trans);
			players.add (player);
			mem.getWizardDetails ().add (wizardDetails);
			
			when (wizardClientUtils.getPlayerName (player)).thenReturn ("Wizard " + wizardID);
			when (multiplayerSessionUtils.findPlayerWithID (players, wizardNo, "HistoryUI")).thenReturn (player);
		}
		
		when (client.getPlayers ()).thenReturn (players);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		
		// Wizards
		final PlayerKnowledgeUtils playerKnowledgeUtils = mock (PlayerKnowledgeUtils.class);
		for (int n = 1; n <= 14; n++)
		{
			final String wizardID = "WZ" + ((n < 10) ? "0" : "") + n;
			when (playerKnowledgeUtils.isWizard (wizardID)).thenReturn (true);
		}
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/HistoryUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final HistoryUI history = new HistoryUI ();
		history.setHistoryLayout (layout);
		history.setUtils (utils);
		history.setClient (client);
		history.setWizardClientUtils (wizardClientUtils);
		history.setPlayerKnowledgeUtils (playerKnowledgeUtils);
		history.setLanguageHolder (langHolder);
		history.setLanguageChangeMaster (langMaster);
		history.setMultiplayerSessionUtils (multiplayerSessionUtils);
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
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (mem);

		final WizardClientUtils wizardClientUtils = mock (WizardClientUtils.class);
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);

		int wizardNo = 0;
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		for (final String flagColour : new String [] {"B2AAA6", "545D10", "A86E4E", "3C398C", "65483D", "D0CA7D", "571D56", "436243", "520101", "FBE270", "D48F1C", "E9BEA2", "900000", "042444"})
		{
			wizardNo++;
			final MomTransientPlayerPublicKnowledge trans = new MomTransientPlayerPublicKnowledge ();
			final KnownWizardDetails wizardDetails = new KnownWizardDetails ();

			String wizardID = Integer.valueOf (wizardNo).toString ();
			while (wizardID.length () < 2)
				wizardID = "0" + wizardID;
			
			wizardDetails.setPlayerID (wizardNo);
			wizardDetails.setWizardID ("WZ" + wizardID);
			
			trans.setFlagColour (flagColour);
			
			for (int turnNumber = 0; turnNumber < 250; turnNumber++)
				wizardDetails.getPowerBaseHistory ().add (turnNumber + (wizardNo * 5));
			
			final PlayerDescription pd = new PlayerDescription ();
			pd.setPlayerID (wizardNo);
			
			final PlayerPublicDetails player = new PlayerPublicDetails (pd, null, trans);
			players.add (player);
			mem.getWizardDetails ().add (wizardDetails);
			
			when (wizardClientUtils.getPlayerName (player)).thenReturn ("Wizard " + wizardID);
			when (multiplayerSessionUtils.findPlayerWithID (players, wizardNo, "HistoryUI")).thenReturn (player);
		}
		
		when (client.getPlayers ()).thenReturn (players);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);

		// Wizards
		final PlayerKnowledgeUtils playerKnowledgeUtils = mock (PlayerKnowledgeUtils.class);
		for (int n = 1; n <= 14; n++)
		{
			final String wizardID = "WZ" + ((n < 10) ? "0" : "") + n;
			when (playerKnowledgeUtils.isWizard (wizardID)).thenReturn (true);
		}
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/HistoryUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final HistoryUI history = new HistoryUI ();
		history.setHistoryLayout (layout);
		history.setUtils (utils);
		history.setClient (client);
		history.setWizardClientUtils (wizardClientUtils);
		history.setPlayerKnowledgeUtils (playerKnowledgeUtils);
		history.setLanguageHolder (langHolder);
		history.setLanguageChangeMaster (langMaster);
		history.setMultiplayerSessionUtils (multiplayerSessionUtils);
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
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (mem);

		final WizardClientUtils wizardClientUtils = mock (WizardClientUtils.class);
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		
		int wizardNo = 0;
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		for (final String flagColour : new String [] {"B2AAA6", "545D10", "A86E4E", "3C398C", "65483D", "D0CA7D", "571D56", "436243", "520101", "FBE270", "D48F1C", "E9BEA2", "900000", "042444"})
		{
			wizardNo++;
			final MomTransientPlayerPublicKnowledge trans = new MomTransientPlayerPublicKnowledge ();
			final KnownWizardDetails wizardDetails = new KnownWizardDetails ();
			
			String wizardID = Integer.valueOf (wizardNo).toString ();
			while (wizardID.length () < 2)
				wizardID = "0" + wizardID;
			
			wizardDetails.setPlayerID (wizardNo);
			wizardDetails.setWizardID ("WZ" + wizardID);
			
			trans.setFlagColour (flagColour);
			
			for (int turnNumber = 0; turnNumber < 300; turnNumber++)
				wizardDetails.getPowerBaseHistory ().add ((turnNumber / 2) + (wizardNo * 5));
			
			final PlayerDescription pd = new PlayerDescription ();
			pd.setPlayerID (wizardNo);
			
			final PlayerPublicDetails player = new PlayerPublicDetails (pd, null, trans);
			players.add (player);
			mem.getWizardDetails ().add (wizardDetails);
			
			when (wizardClientUtils.getPlayerName (player)).thenReturn ("Wizard " + wizardID);
			when (multiplayerSessionUtils.findPlayerWithID (players, wizardNo, "HistoryUI")).thenReturn (player);
		}
		
		when (client.getPlayers ()).thenReturn (players);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);

		// Wizards
		final PlayerKnowledgeUtils playerKnowledgeUtils = mock (PlayerKnowledgeUtils.class);
		for (int n = 1; n <= 14; n++)
		{
			final String wizardID = "WZ" + ((n < 10) ? "0" : "") + n;
			when (playerKnowledgeUtils.isWizard (wizardID)).thenReturn (true);
		}
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/HistoryUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final HistoryUI history = new HistoryUI ();
		history.setHistoryLayout (layout);
		history.setUtils (utils);
		history.setClient (client);
		history.setWizardClientUtils (wizardClientUtils);
		history.setPlayerKnowledgeUtils (playerKnowledgeUtils);
		history.setLanguageHolder (langHolder);
		history.setLanguageChangeMaster (langMaster);
		history.setMultiplayerSessionUtils (multiplayerSessionUtils);
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
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (mem);

		final WizardClientUtils wizardClientUtils = mock (WizardClientUtils.class);
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);

		int wizardNo = 0;
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		for (final String flagColour : new String [] {"B2AAA6", "545D10", "A86E4E", "3C398C", "65483D", "D0CA7D", "571D56", "436243", "520101", "FBE270", "D48F1C", "E9BEA2", "900000", "042444"})
		{
			wizardNo++;
			final MomTransientPlayerPublicKnowledge trans = new MomTransientPlayerPublicKnowledge ();
			final KnownWizardDetails wizardDetails = new KnownWizardDetails ();

			String wizardID = Integer.valueOf (wizardNo).toString ();
			while (wizardID.length () < 2)
				wizardID = "0" + wizardID;
			
			wizardDetails.setPlayerID (wizardNo);
			wizardDetails.setWizardID ("WZ" + wizardID);
			
			trans.setFlagColour (flagColour);
			
			for (int turnNumber = 0; turnNumber < 300; turnNumber++)
				wizardDetails.getPowerBaseHistory ().add (turnNumber + (wizardNo * 5));
			
			final PlayerDescription pd = new PlayerDescription ();
			pd.setPlayerID (wizardNo);
			
			final PlayerPublicDetails player = new PlayerPublicDetails (pd, null, trans);
			players.add (player);
			mem.getWizardDetails ().add (wizardDetails);
			
			when (wizardClientUtils.getPlayerName (player)).thenReturn ("Wizard " + wizardID);
			when (multiplayerSessionUtils.findPlayerWithID (players, wizardNo, "HistoryUI")).thenReturn (player);
		}
		
		when (client.getPlayers ()).thenReturn (players);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);

		// Wizards
		final PlayerKnowledgeUtils playerKnowledgeUtils = mock (PlayerKnowledgeUtils.class);
		for (int n = 1; n <= 14; n++)
		{
			final String wizardID = "WZ" + ((n < 10) ? "0" : "") + n;
			when (playerKnowledgeUtils.isWizard (wizardID)).thenReturn (true);
		}
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/HistoryUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final HistoryUI history = new HistoryUI ();
		history.setHistoryLayout (layout);
		history.setUtils (utils);
		history.setClient (client);
		history.setWizardClientUtils (wizardClientUtils);
		history.setPlayerKnowledgeUtils (playerKnowledgeUtils);
		history.setLanguageHolder (langHolder);
		history.setLanguageChangeMaster (langMaster);
		history.setMultiplayerSessionUtils (multiplayerSessionUtils);
		history.setLargeFont (CreateFontsForTests.getLargeFont ());
		history.setSmallFont (CreateFontsForTests.getSmallFont ());
		
		// Display form		
		history.setVisible (true);
		history.updateTurnLabelText ();		// Must do this after .init (), or the label we're updating won't exist yet
		Thread.sleep (5000);
		history.setVisible (false);
	}
}