package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;

import java.util.ArrayList;
import java.util.List;

import momime.client.MomClient;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.v0_9_5.ProductionType;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.ResourceValueUtils;

import org.junit.Test;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;

/**
 * Tests the AlchemyUI class
 */
public final class TestAlchemyUI
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
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);

		when (lang.findCategoryEntry ("frmAlchemy", "Title")).thenReturn ("Alchemy");
		when (lang.findCategoryEntry ("frmAlchemy", "OK")).thenReturn ("OK");
		when (lang.findCategoryEntry ("frmAlchemy", "Cancel")).thenReturn ("Cancel");
		when (lang.findCategoryEntry ("frmAlchemy", "Conversion")).thenReturn ("Transmute FROM_PRODUCTION_TYPE to TO_PRODUCTION_TYPE");

		final ProductionType goldProduction = new ProductionType ();
		goldProduction.setProductionTypeDescription ("Gold");
		goldProduction.setProductionTypeSuffix ("GP");
		when (lang.findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD)).thenReturn (goldProduction);
		
		final ProductionType manaProduction = new ProductionType ();
		manaProduction.setProductionTypeDescription ("Mana");
		manaProduction.setProductionTypeSuffix ("MP");
		when (lang.findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA)).thenReturn (manaProduction);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);

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
		
		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (eq (players), eq (pd.getPlayerID ()), anyString ())).thenReturn (player);
		
		// Picks
		final PlayerPickUtils pickUtils = mock (PlayerPickUtils.class);
		when (pickUtils.getQuantityOfPick (pub.getPick (), CommonDatabaseConstants.VALUE_RETORT_ID_ALCHEMY)).thenReturn (retortValue);
		
		// Resources we have
		final ResourceValueUtils resourceUtils = mock (ResourceValueUtils.class);
		when (resourceUtils.findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD)).thenReturn (125);
		when (resourceUtils.findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA)).thenReturn (75);
		
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