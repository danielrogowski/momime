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
import com.ndg.random.RandomUtilsImpl;
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
import momime.client.utils.PlayerPickClientUtilsImpl;
import momime.client.utils.TextUtilsImpl;
import momime.client.utils.WizardClientUtils;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Language;
import momime.common.database.Pick;
import momime.common.database.WizardEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.messages.PlayerPick;
import momime.common.messages.WizardState;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.PlayerKnowledgeUtils;

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
		simpleLang.getAnd ().add (createLanguageText (Language.ENGLISH, "and"));
		
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
		
		for (int n = 1; n <= 13; n++)
		{
			final WizardEx wizard = new WizardEx ();
			wizard.setWizardID ("WZ" + ((n < 10) ? "0" : "") + n);
			wizard.setPortraitImageFile ("/momime.client.graphics/wizards/" + wizard.getWizardID () + ".png");
			
			if (n != 6)
				when (db.findWizard (wizard.getWizardID (), "WizardsUI")).thenReturn (wizard);
		}
		
		for (int n = 1; n <= 6; n++)
		{
			final Pick retort = new Pick ();
			switch (n)
			{
				case 1: retort.getPickDescriptionSingular ().add (createLanguageText (Language.ENGLISH, "Warlord")); break;
				case 2: retort.getPickDescriptionSingular ().add (createLanguageText (Language.ENGLISH, "Divine Power")); break;
				case 3: retort.getPickDescriptionSingular ().add (createLanguageText (Language.ENGLISH, "Nature Mastery")); break;
				case 4: retort.getPickDescriptionSingular ().add (createLanguageText (Language.ENGLISH, "Charismatic")); break;
				case 5: retort.getPickDescriptionSingular ().add (createLanguageText (Language.ENGLISH, "Sage Master")); break;
				case 6: retort.getPickDescriptionSingular ().add (createLanguageText (Language.ENGLISH, "Node Mastery")); break;
			}
			
			when (db.findPick (eq ("RT0" + n), anyString ())).thenReturn (retort);
		}
		
		for (int n = 1; n <= 5; n++)
		{
			final Pick book = new Pick ();
			final String imageName;
			switch (n)
			{
				case 1: imageName = "life"; break;
				case 2: imageName = "death"; break;
				case 3: imageName = "chaos"; break;
				case 4: imageName = "nature"; break;
				case 5: imageName = "sorcery"; break;
				default:
					throw new Exception ("Don't know image name for pick " + n);
			}
			
			for (int m = 1; m <= 3; m++)
				book.getBookImageFile ().add ("/momime.client.graphics/picks/" + imageName + "-" + m + ".png");
			
			when (db.findPick (eq ("MB0" + n), anyString ())).thenReturn (book);
		}
		
		// Players
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		for (int n = 1; n <= 16; n++)
		{
			final PlayerDescription pd = new PlayerDescription ();
			pd.setHuman (n <= 14);
			pd.setPlayerID ((n <= 14) ? n : -n);
			
			final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
			pub.setWizardState ((n == 14) ? WizardState.BANISHED : WizardState.ACTIVE);
			
			final KnownWizardDetails wizardDetails = new KnownWizardDetails ();
			
			MomTransientPlayerPublicKnowledge trans = null;
			if (n == 16)
				wizardDetails.setWizardID (CommonDatabaseConstants.WIZARD_ID_RAIDERS);
			else if (n == 15)
				wizardDetails.setWizardID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);
			else
			{
				wizardDetails.setWizardID ("WZ" + ((n < 10) ? "0" : "") + n);
				pub.setStandardPhotoID (wizardDetails.getWizardID ());
				
				final String hex = Integer.toHexString (pd.getPlayerID ());
				trans = new MomTransientPlayerPublicKnowledge ();
				trans.setFlagColour ("FF" + hex + "0" + hex + "0");
			}
			
			final PlayerPublicDetails player = new PlayerPublicDetails (pd, pub, trans);
			players.add (player);
			
			if ((n <= 14) && (n != 6))
			{
				when (multiplayerSessionUtils.findPlayerWithID (eq (players), eq (pd.getPlayerID ()), anyString ())).thenReturn (player);
				when (knownWizardUtils.findKnownWizardDetails (eq (priv.getKnownWizardDetails ()), eq (pd.getPlayerID ()), anyString ())).thenReturn (wizardDetails);
			}
			
			when (knownWizardUtils.findKnownWizardDetails (priv.getKnownWizardDetails (), pd.getPlayerID ())).thenReturn ((n == 6) ? null : wizardDetails);
		}
		
		final MomClient client = mock (MomClient.class);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		
		// First wizard
		final WizardClientUtils wizardClientUtils = mock (WizardClientUtils.class);
		when (wizardClientUtils.getPlayerName (players.get (0))).thenReturn ("Merlin");
		
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) players.get (0).getPersistentPlayerPublicKnowledge ();
		for (int n = 1; n <= 6; n++)
		{
			final PlayerPick pick = new PlayerPick ();
			pick.setPickID ("RT0" + n);
			pick.setQuantity (1);
			pub.getPick ().add (pick);
		}

		for (int n = 1; n <= 5; n++)
		{
			final PlayerPick pick = new PlayerPick ();
			pick.setPickID ("MB0" + n);
			pick.setQuantity (7);
			pub.getPick ().add (pick);
		}
		
		// Memory
		final FogOfWarMemory mem = new FogOfWarMemory ();
		priv.setFogOfWarMemory (mem);
		
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);

		// Wizards
		final PlayerKnowledgeUtils playerKnowledgeUtils = mock (PlayerKnowledgeUtils.class);
		for (int n = 1; n <= 14; n++)
		{
			final String wizardID = "WZ" + ((n < 10) ? "0" : "") + n;
			if (n != 6)
				when (playerKnowledgeUtils.isWizard (wizardID)).thenReturn (true);
		}
		
		// Image generator
		final PlayerColourImageGeneratorImpl gen = new PlayerColourImageGeneratorImpl ();
		gen.setUtils (utils);
		gen.setClient (client);
		gen.setMultiplayerSessionUtils (multiplayerSessionUtils);
		gen.setKnownWizardUtils (knownWizardUtils);

		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/WizardsUI.xml"));
		layout.buildMaps ();
		
		// This is only used for a tiny random number function, easier to use real one than to mock it
		final PlayerPickClientUtilsImpl playerPickClientUtils = new PlayerPickClientUtilsImpl ();
		playerPickClientUtils.setRandomUtils (new RandomUtilsImpl ());
		
		final TextUtilsImpl textUtils = new TextUtilsImpl ();
		textUtils.setLanguageHolder (langHolder);
		
		// Set up form
		final WizardsUI wizards = new WizardsUI ();
		wizards.setWizardsLayout (layout);
		wizards.setUtils (utils);
		wizards.setLanguageHolder (langHolder);
		wizards.setLanguageChangeMaster (langMaster);
		wizards.setClient (client);
		wizards.setPlayerColourImageGenerator (gen);
		wizards.setWizardClientUtils (wizardClientUtils);
		wizards.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		wizards.setPlayerKnowledgeUtils (playerKnowledgeUtils);
		wizards.setSmallFont (CreateFontsForTests.getSmallFont ());
		wizards.setMediumFont (CreateFontsForTests.getMediumFont ());
		wizards.setLargeFont (CreateFontsForTests.getLargeFont ());
		wizards.setTextUtils (textUtils);
		wizards.setPlayerPickClientUtils (playerPickClientUtils);
		wizards.setKnownWizardUtils (knownWizardUtils);

		// Display form		
		wizards.setVisible (true);
		wizards.wizardButtons.get (0).doClick ();
		Thread.sleep (5000);
		wizards.setVisible (false);
	}
}