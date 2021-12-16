package momime.client.ui.dialogs;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.audio.AudioPlayer;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.Simple;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.common.database.CommonDatabase;
import momime.common.database.Event;
import momime.common.database.Language;
import momime.common.messages.servertoclient.RandomEventMessage;

/**
 * Tests the RandomEventUI class
 */
@ExtendWith(MockitoExtension.class)
public final class TestRandomEventUI extends ClientTestData
{
	/**
	 * Tests the RandomEventUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRandomEventUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Event eventDef = new Event ();
		eventDef.getEventName ().add (createLanguageText (Language.ENGLISH, "Conjunction of Nature"));
		eventDef.getEventDescriptionStart ().add (createLanguageText (Language.ENGLISH, "The rising triad of green stars come together, doubling all power gained from green nodes and halving all others."));
		eventDef.setEventImageFile ("/momime.client.graphics/events/EV16.png");
		when (db.findEvent ("EV01", "RandomEventUI")).thenReturn (eventDef);
		
		// Mock entries from the language XML
		final Simple simpleLang = new Simple ();
		simpleLang.getOk ().add (createLanguageText (Language.ENGLISH, "OK"));
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSimple ()).thenReturn (simpleLang);

		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// Player
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Random event
		final RandomEventMessage event = new RandomEventMessage ();
		event.setEventID ("EV01");
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.dialogs/RandomEventUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final RandomEventUI box = new RandomEventUI ();
		box.setUtils (utils);
		box.setLanguageHolder (langHolder);
		box.setLanguageChangeMaster (langMaster);
		box.setClient (client);
		box.setSmallFont (CreateFontsForTests.getSmallFont ());
		box.setRandomEventLayout (layout);
		box.setRandomEventMessage (event);
		box.setMusicPlayer (mock (AudioPlayer.class));
		
		// Display form		
		box.setModal (false);
		box.setVisible (true);
		Thread.sleep (5000);
		box.setVisible (false);
	}
}