package momime.client.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.common.database.CommonDatabase;
import momime.common.database.Language;
import momime.common.database.WizardEx;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;

/**
 * Tests the WizardClientUtilsImpl class
 */
public final class TestWizardClientUtilsImpl extends ClientTestData
{
	/**
	 * Tests the getPlayerName method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetPlayerName () throws Exception
	{
		// Mock entries from the language XML
		final CommonDatabase db = mock (CommonDatabase.class);

		final WizardEx wizardDef = new WizardEx ();
		wizardDef.getWizardName ().add (createLanguageText (Language.ENGLISH, "Merlin"));
		when (db.findWizard ("WZ01", "getPlayerName")).thenReturn (wizardDef);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerName ("Mr. Blah");
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		pub.setWizardID ("WZ01");
		
		final PlayerPublicDetails player = new PlayerPublicDetails (pd, pub, null); 
		
		// Set up object to test
		final WizardClientUtilsImpl utils = new WizardClientUtilsImpl ();
		utils.setLanguageHolder (new LanguageDatabaseHolder ());
		utils.setClient (client);
		
		// Try with a human player
		pd.setHuman (true);
		assertEquals ("Mr. Blah", utils.getPlayerName (player));
		
		// Try with an AI player
		pd.setHuman (false);
		assertEquals ("Merlin", utils.getPlayerName (player));
	}
}