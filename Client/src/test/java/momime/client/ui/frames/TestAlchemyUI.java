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

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.AlchemyScreen;
import momime.client.languages.database.Simple;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Language;
import momime.common.database.ProductionTypeEx;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.ResourceValueUtils;

/**
 * Tests the AlchemyUI class
 */
@ExtendWith(MockitoExtension.class)
public final class TestAlchemyUI extends ClientTestData
{
	/**
	 * Tests the AlchemyUI form, when we don't have the alchemy retort
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAlchemyUI_NoRetort () throws Exception
	{
		testAlchemyUI (0);
	}

	/**
	 * Tests the AlchemyUI form, when we do have the alchemy retort
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAlchemyUI_WithRetort () throws Exception
	{
		testAlchemyUI (1);
	}

	/**
	 * Tests the AlchemyUI form
	 * @param retortValue Whether we have the alchemy retort or not
	 * @throws Exception If there is a problem
	 */
	private final void testAlchemyUI (final int retortValue) throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final ProductionTypeEx goldProduction = new ProductionTypeEx ();
		goldProduction.getProductionTypeDescription ().add (createLanguageText (Language.ENGLISH, "Gold"));
		goldProduction.getProductionTypeSuffix ().add (createLanguageText (Language.ENGLISH, "GP"));
		when (db.findProductionType (eq (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD), anyString ())).thenReturn (goldProduction);
		
		final ProductionTypeEx manaProduction = new ProductionTypeEx ();
		manaProduction.getProductionTypeDescription ().add (createLanguageText (Language.ENGLISH, "Mana"));
		manaProduction.getProductionTypeSuffix ().add (createLanguageText (Language.ENGLISH, "MP"));
		when (db.findProductionType (eq (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA), anyString ())).thenReturn (manaProduction);
		
		// Mock entries from the language XML
		final Simple simpleLang = new Simple ();
		simpleLang.getOk ().add (createLanguageText (Language.ENGLISH, "OK"));
		simpleLang.getCancel ().add (createLanguageText (Language.ENGLISH, "Cancel"));
		
		final AlchemyScreen alchemyScreenLang = new AlchemyScreen ();
		alchemyScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Alchemy"));
		alchemyScreenLang.getConversion ().add (createLanguageText (Language.ENGLISH, "Transmute FROM_PRODUCTION_TYPE to TO_PRODUCTION_TYPE"));
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSimple ()).thenReturn (simpleLang);
		when (lang.getAlchemyScreen ()).thenReturn (alchemyScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Player
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);
		pd.setHuman (true);
		
		final PlayerPublicDetails player = new PlayerPublicDetails (pd, pub, null);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player);
		
		final MomClient client = mock (MomClient.class);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getOurPlayerID ()).thenReturn (3);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		when (client.getClientDB ()).thenReturn (db);
		
		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (eq (players), eq (pd.getPlayerID ()), anyString ())).thenReturn (player);
		
		// Picks
		final PlayerPickUtils pickUtils = mock (PlayerPickUtils.class);
		when (pickUtils.getQuantityOfPick (pub.getPick (), CommonDatabaseConstants.RETORT_ID_ALCHEMY)).thenReturn (retortValue);
		
		// Resources we have
		final ResourceValueUtils resourceUtils = mock (ResourceValueUtils.class);
		when (resourceUtils.findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD)).thenReturn (125);
		when (resourceUtils.findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (75);
		
		// Set up form
		final AlchemyUI alchemy = new AlchemyUI ();
		alchemy.setUtils (utils);
		alchemy.setLanguageHolder (langHolder);
		alchemy.setLanguageChangeMaster (langMaster);
		alchemy.setClient (client);
		alchemy.setPlayerPickUtils (pickUtils);
		alchemy.setResourceValueUtils (resourceUtils);
		alchemy.setMultiplayerSessionUtils (multiplayerSessionUtils);
		alchemy.setLargeFont (CreateFontsForTests.getLargeFont ());
		alchemy.setSmallFont (CreateFontsForTests.getSmallFont ());

		// Display form		
		alchemy.setVisible (true);
		Thread.sleep (5000);
		alchemy.setVisible (false);
	}
}