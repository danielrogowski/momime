package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.WizardGfx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.ui.PlayerColourImageGeneratorImpl;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomTransientPlayerPublicKnowledge;

import org.junit.Test;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

/**
 * Tests the WizardsUI class
 */
public final class TestWizardsUI extends ClientTestData
{
	/**
	 * Tests the WizardsUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testWizardsUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmWizards", "Title")).thenReturn ("Wizards");
		when (lang.findCategoryEntry ("frmWizards", "Close")).thenReturn ("Close");
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Mock graphics DB
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		for (int n = 1; n <= 14; n++)
		{
			final WizardGfx wizard = new WizardGfx ();
			wizard.setWizardID ("WZ" + ((n < 10) ? "0" : "") + n);
			wizard.setPortraitFile ("/momime.client.graphics/wizards/" + wizard.getWizardID () + ".png");
			
			when (gfx.findWizard (wizard.getWizardID (), "WizardsUI")).thenReturn (wizard);
		}
		
		// Players
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		for (int n = 1; n <= 16; n++)
		{
			final PlayerDescription pd = new PlayerDescription ();
			pd.setHuman (n <= 14);
			pd.setPlayerID ((n <= 14) ? n : -n);
			
			final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
			MomTransientPlayerPublicKnowledge trans = null;
			if (n == 16)
				pub.setWizardID (CommonDatabaseConstants.WIZARD_ID_RAIDERS);
			else if (n == 15)
				pub.setWizardID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);
			else
			{
				pub.setWizardID ("WZ" + ((n < 10) ? "0" : "") + n);
				pub.setStandardPhotoID (pub.getWizardID ());
				
				final String hex = Integer.toHexString (pd.getPlayerID ());
				trans = new MomTransientPlayerPublicKnowledge ();
				trans.setFlagColour ("FF" + hex + "0" + hex + "0");
			}
			
			final PlayerPublicDetails player = new PlayerPublicDetails (pd, pub, trans);
			players.add (player);
			
			when (multiplayerSessionUtils.findPlayerWithID (players, pd.getPlayerID (), "PlayerColourImageGeneratorImpl")).thenReturn (player);
		}
		
		final MomClient client = mock (MomClient.class);
		when (client.getPlayers ()).thenReturn (players);
		
		// Image generator
		final PlayerColourImageGeneratorImpl gen = new PlayerColourImageGeneratorImpl ();
		gen.setUtils (utils);
		gen.setClient (client);
		gen.setMultiplayerSessionUtils (multiplayerSessionUtils);

		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/WizardsUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final WizardsUI wizards = new WizardsUI ();
		wizards.setWizardsLayout (layout);
		wizards.setUtils (utils);
		wizards.setLanguageHolder (langHolder);
		wizards.setLanguageChangeMaster (langMaster);
		wizards.setClient (client);
		wizards.setPlayerColourImageGenerator (gen);
		wizards.setGraphicsDB (gfx);
		wizards.setSmallFont (CreateFontsForTests.getSmallFont ());

		// Display form		
		wizards.setVisible (true);
		Thread.sleep (5000);
		wizards.setVisible (false);
	}
}