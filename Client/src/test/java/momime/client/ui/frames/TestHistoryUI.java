package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.WizardClientUtils;
import momime.common.messages.MomGeneralPublicKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomTransientPlayerPublicKnowledge;

/**
 * Tests the HistoryUI class
 */
public final class TestHistoryUI extends ClientTestData
{
	/**
	 * Tests the HistoryUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHistoryUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);

		when (lang.findCategoryEntry ("frmHistory", "Title")).thenReturn ("History of Wizards' Power");
		
		when (lang.findCategoryEntry ("Months", "MNTH01")).thenReturn ("January");
		when (lang.findCategoryEntry ("frmMapButtonBar", "Turn")).thenReturn ("MONTH YEAR (Turn TURN)");
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);

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
}