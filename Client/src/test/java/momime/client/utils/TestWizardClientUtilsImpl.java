package momime.client.utils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;

import org.junit.Test;

import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the WizardClientUtilsImpl class
 */
public final class TestWizardClientUtilsImpl
{
	/**
	 * Tests the getPlayerName method
	 */
	@Test
	public final void testGetPlayerName ()
	{
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findWizardName ("WZ01")).thenReturn ("Merlin");

		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerName ("Mr. Blah");
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		pub.setWizardID ("WZ01");
		
		final PlayerPublicDetails player = new PlayerPublicDetails (pd, pub, null); 
		
		// Set up object to test
		final WizardClientUtilsImpl utils = new WizardClientUtilsImpl ();
		utils.setLanguageHolder (langHolder);
		
		// Try with a human player
		pd.setHuman (true);
		assertEquals ("Mr. Blah", utils.getPlayerName (player));
		
		// Try with an AI player
		pd.setHuman (false);
		assertEquals ("Merlin", utils.getPlayerName (player));
	}
}