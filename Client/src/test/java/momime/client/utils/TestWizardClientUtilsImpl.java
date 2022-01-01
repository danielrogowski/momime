package momime.client.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.common.database.CommonDatabase;
import momime.common.database.Language;
import momime.common.database.WizardEx;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.PlayerKnowledgeUtils;

/**
 * Tests the WizardClientUtilsImpl class
 */
@ExtendWith(MockitoExtension.class)
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
		
		// Client
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		
		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		pd.setPlayerName ("Mr. Blah");
		
		final KnownWizardDetails wizardDetails = new KnownWizardDetails ();
		wizardDetails.setWizardID ("WZ01");
		
		final PlayerPublicDetails player = new PlayerPublicDetails (pd, null, null); 
		
		// Wizards
		final PlayerKnowledgeUtils playerKnowledgeUtils = mock (PlayerKnowledgeUtils.class);
		when (playerKnowledgeUtils.isCustomWizard ("WZ01")).thenReturn (false);
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (priv.getKnownWizardDetails (), pd.getPlayerID ())).thenReturn (wizardDetails);
		
		// Set up object to test
		final WizardClientUtilsImpl utils = new WizardClientUtilsImpl ();
		utils.setLanguageHolder (new LanguageDatabaseHolder ());
		utils.setClient (client);
		utils.setPlayerKnowledgeUtils (playerKnowledgeUtils);
		utils.setKnownWizardUtils (knownWizardUtils);
		
		// Try with a human player
		pd.setHuman (true);
		assertEquals ("Mr. Blah", utils.getPlayerName (player));
		
		// Try with an AI player
		pd.setHuman (false);
		assertEquals ("Merlin", utils.getPlayerName (player));
	}
}