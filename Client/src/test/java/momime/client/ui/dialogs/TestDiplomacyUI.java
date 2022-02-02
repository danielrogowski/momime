package momime.client.ui.dialogs;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.DiplomacyScreen;
import momime.client.utils.SpellClientUtilsImpl;
import momime.common.database.AnimationEx;
import momime.common.database.AnimationFrame;
import momime.common.database.CommonDatabase;
import momime.common.database.Language;
import momime.common.database.WizardEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.utils.KnownWizardUtils;

/**
 * Tests the DiplomacyUI class
 */
@ExtendWith(MockitoExtension.class)
public final class TestDiplomacyUI extends ClientTestData
{
	/**
	 * Tests the DiplomacyUI class
	 * @param anotherWizard True if custom photo; false for standard photo 
	 * @throws Exception If there is a problem
	 */
	public final void testDiplomacyUI (final boolean anotherWizard) throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();

		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		if (!anotherWizard)
		{
			final WizardEx wizard = new WizardEx ();
			wizard.setPortraitImageFile ("/momime.client.graphics/wizards/WZ12.png");
			when (db.findWizard ("WZ01", "DiplomacyUI")).thenReturn (wizard);
		}
		
		// Mock entries from the graphics XML
		final AnimationEx fade = new AnimationEx ();
		fade.setAnimationSpeed (8);
		for (int n = 1; n <= 15; n++)
		{
			final AnimationFrame frame = new AnimationFrame ();
			frame.setImageFile ("/momime.client.graphics/ui/mirror/mirror-fade-frame" + ((n < 10) ? "0" : "") + n + ".png");
			fade.getFrame ().add (frame);
		}
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.findAnimation (OverlandEnchantmentsUI.MIRROR_ANIM, "DiplomacyUI")).thenReturn (fade);
		
		// Mock entries from the language XML
		final DiplomacyScreen diplomacyScreenLang = new DiplomacyScreen ();
		diplomacyScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Diplomacy"));
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getDiplomacyScreen ()).thenReturn (diplomacyScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// FOW (just to add the spell into)
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (fow);
		
		// Wizard
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		
		if (!anotherWizard)
		{
			final KnownWizardDetails wizardDetails1 = new KnownWizardDetails ();
			wizardDetails1.setStandardPhotoID ("WZ01");
			when (knownWizardUtils.findKnownWizardDetails (eq (fow.getWizardDetails ()), eq (1), anyString ())).thenReturn (wizardDetails1);
		}
		else
		{
			final KnownWizardDetails wizardDetails2 = new KnownWizardDetails ();
			wizardDetails2.setCustomPhoto (Files.readAllBytes (Paths.get (getClass ().getResource ("/CustomWizardPhoto.png").toURI ())));
			when (knownWizardUtils.findKnownWizardDetails (eq (fow.getWizardDetails ()), eq (2), anyString ())).thenReturn (wizardDetails2);
		}
		
		// Client
		final MomClient client = mock (MomClient.class);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);

		if (!anotherWizard)
			when (client.getClientDB ()).thenReturn (db);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.dialogs/DiplomacyUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final DiplomacyUI diplomacy = new DiplomacyUI ();
		diplomacy.setDiplomacyLayout (layout);
		diplomacy.setUtils (utils);
		diplomacy.setClient (client);
		diplomacy.setGraphicsDB (gfx);
		diplomacy.setLanguageHolder (langHolder);
		diplomacy.setLanguageChangeMaster (langMaster);
		diplomacy.setKnownWizardUtils (knownWizardUtils);
		diplomacy.setTalkingWizardID (anotherWizard ? 2 : 1);
		diplomacy.setSpellClientUtils (new SpellClientUtilsImpl ());
		
		// Display form		
		diplomacy.setModal (false);
		diplomacy.setVisible (true);
		Thread.sleep (5000);
		diplomacy.setVisible (false);
	}

	/**
	 * Tests the DiplomacyUI form using a standard photo
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDiplomacyUI_StandardPhoto () throws Exception
	{
		testDiplomacyUI (false);
	}

	/**
	 * Tests the DiplomacyUI form using a custom photo
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDiplomacyUI_CustomPhoto () throws Exception
	{
		testDiplomacyUI (true);
	}
}