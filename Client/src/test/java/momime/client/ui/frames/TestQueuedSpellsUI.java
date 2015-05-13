package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.database.ClientDatabaseEx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.ProductionTypeLang;
import momime.client.language.database.SpellLang;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.ui.renderer.QueuedSpellListCellRenderer;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Spell;
import momime.common.database.SpellSetting;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.utils.SpellUtils;

import org.junit.Test;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

/**
 * Tests the QueuedSpellsUI class
 */
public final class TestQueuedSpellsUI
{
	/**
	 * Tests the QueuedSpellsUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testQueuedSpellsUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmSpellQueue", "Title")).thenReturn ("Queued Overland Spells");
		when (lang.findCategoryEntry ("frmSpellQueue", "Close")).thenReturn ("Close");

		for (int n = 1; n <= 5; n++)
		{
			final SpellLang spellLang = new SpellLang ();
			spellLang.setSpellName ("Spell SP00" + n);
			when (lang.findSpell ("SP00" + n)).thenReturn (spellLang);
		}
		
		final ProductionTypeLang manaProduction = new ProductionTypeLang ();
		manaProduction.setProductionTypeSuffix ("MP");
		
		when (lang.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (manaProduction);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// Session description
		final SpellSetting spellSettings = new SpellSetting ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setSpellSetting (spellSettings);
		
		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);
		pd.setHuman (true);
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		
		final PlayerPublicDetails player = new PlayerPublicDetails (pd, pub, null);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd.getPlayerID (), "QueuedSpellListCellRenderer")).thenReturn (player);
		
		// Mock spell definitions
		final SpellUtils spellUtils = mock (SpellUtils.class);
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		for (int n = 1; n <= 5; n++)
		{
			final Spell spell = new Spell ();
			when (db.findSpell ("SP00" + n, "QueuedSpellListCellRenderer")).thenReturn (spell);
			when (spellUtils.getReducedOverlandCastingCost (spell, pub.getPick (), spellSettings, db)).thenReturn (n * 100);
		}
		
		// Mock queued spells
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		
		for (int n = 1; n <= 5; n++)
			priv.getQueuedSpellID ().add ("SP00" + n);			

		final MomClient client = mock (MomClient.class);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getOurPlayerID ()).thenReturn (pd.getPlayerID ());
		when (client.getClientDB ()).thenReturn (db);
		when (client.getSessionDescription ()).thenReturn (sd);
		
		// Cell renderer
		final QueuedSpellListCellRenderer renderer = new QueuedSpellListCellRenderer ();
		renderer.setLanguageHolder (langHolder);
		renderer.setMultiplayerSessionUtils (multiplayerSessionUtils);
		renderer.setClient (client);
		renderer.setSpellUtils (spellUtils);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) ClientTestData.createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/QueuedSpellsUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final QueuedSpellsUI queue = new QueuedSpellsUI ();
		queue.setQueuedSpellsLayout (layout);
		queue.setUtils (utils);
		queue.setLanguageHolder (langHolder);
		queue.setLanguageChangeMaster (langMaster);
		queue.setLargeFont (CreateFontsForTests.getLargeFont ());
		queue.setSmallFont (CreateFontsForTests.getSmallFont ());
		queue.setClient (client);
		queue.setQueuedSpellListCellRenderer (renderer);

		// Display form		
		queue.setVisible (true);
		queue.updateQueuedSpells ();
		Thread.sleep (5000);
		queue.setVisible (false);
	}
}