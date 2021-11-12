package momime.client.ui.frames;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.Simple;
import momime.client.languages.database.WizardsScreen;
import momime.client.ui.PlayerColourImageGeneratorImpl;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Language;
import momime.common.database.WizardEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.messages.WizardState;
import momime.common.utils.MemoryMaintainedSpellUtils;

/**
 * Tests the WizardsUI class
 */
@ExtendWith(MockitoExtension.class)
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
		final Simple simpleLang = new Simple ();
		simpleLang.getClose ().add (createLanguageText (Language.ENGLISH, "Close"));
		
		final WizardsScreen wizardsScreenLang = new WizardsScreen ();
		wizardsScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Wizards"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSimple ()).thenReturn (simpleLang);
		when (lang.getWizardsScreen ()).thenReturn (wizardsScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		for (int n = 1; n <= 14; n++)
		{
			final WizardEx wizard = new WizardEx ();
			wizard.setWizardID ("WZ" + ((n < 10) ? "0" : "") + n);
			wizard.setPortraitImageFile ("/momime.client.graphics/wizards/" + wizard.getWizardID () + ".png");
			
			when (db.findWizard (wizard.getWizardID (), "WizardsUI")).thenReturn (wizard);
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
			pub.setWizardState ((n == 14) ? WizardState.BANISHED : WizardState.ACTIVE);
			
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
			
			when (multiplayerSessionUtils.findPlayerWithID (eq (players), eq (pd.getPlayerID ()), anyString ())).thenReturn (player);
		}
		
		final MomClient client = mock (MomClient.class);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getClientDB ()).thenReturn (db);
		
		// Memory
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (mem);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);
		
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
		wizards.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		wizards.setSmallFont (CreateFontsForTests.getSmallFont ());
		wizards.setLargeFont (CreateFontsForTests.getLargeFont ());

		// Display form		
		wizards.setVisible (true);
		Thread.sleep (5000);
		wizards.setVisible (false);
	}
}