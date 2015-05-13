package momime.client.ui.dialogs;

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
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.ui.frames.CombatUI;
import momime.client.utils.TextUtilsImpl;
import momime.common.calculations.SpellCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Spell;
import momime.common.database.SpellSetting;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.utils.SpellUtilsImpl;

import org.junit.Test;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

/**
 * Tests the VariableManaUI class
 */
public final class TestVariableManaUI
{
	/**
	 * Tests the VariableManaUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testVariableManaUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("VariableMana", "Title")).thenReturn ("Additional Power");
		when (lang.findCategoryEntry ("VariableMana", "Damage")).thenReturn ("VALUE damage");
		
		final ProductionTypeLang manaProduction = new ProductionTypeLang ();
		manaProduction.setProductionTypeSuffix ("MP");
		when (lang.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (manaProduction);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// Player
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);
		pd.setHuman (true);
		
		final PlayerPublicDetails player = new PlayerPublicDetails (pd, pub, null);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		// Session description
		final SpellSetting spellSettings = new SpellSetting ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setSpellSetting (spellSettings);
		
		final MomClient client = mock (MomClient.class);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getOurPlayerID ()).thenReturn (pd.getPlayerID ());
		when (client.getSessionDescription ()).thenReturn (sd);
		
		// Client db
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd.getPlayerID (), "sliderPositionChanged")).thenReturn (player);
		
		// Example spell
		final Spell spell = new Spell ();
		spell.setCombatBaseDamage (5);
		spell.setCombatMaxDamage (25);
		spell.setCombatManaPerAdditionalDamagePoint (2);
		spell.setCombatCastingCost (20);

		// Spell casting reduction
		final SpellCalculations spellCalc = mock (SpellCalculations.class);
		when (spellCalc.calculateCastingCostReduction (0, spellSettings, spell, pub.getPick (), db)).thenReturn (30d);
		
		final SpellUtilsImpl spellUtils = new SpellUtilsImpl ();
		spellUtils.setSpellCalculations (spellCalc);
		
		// Need combatUI to tell whether it is an overland or combat cast
		final CombatUI combatUI = new CombatUI ();
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) ClientTestData.createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.dialogs/VariableManaUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final VariableManaUI box = new VariableManaUI ();
		box.setVariableManaLayout (layout);
		box.setUtils (utils);
		box.setLanguageHolder (langHolder);
		box.setLanguageChangeMaster (langMaster);
		box.setTextUtils (new TextUtilsImpl ());
		box.setMultiplayerSessionUtils (multiplayerSessionUtils);
		box.setClient (client);
		box.setSpellUtils (spellUtils);
		box.setMediumFont (CreateFontsForTests.getMediumFont ());
		box.setCombatUI (combatUI);
		box.setSpellBeingTargetted (spell);
		
		// Display form		
		box.setModal (false);
		box.setVisible (true);
		Thread.sleep (5000);
		box.setVisible (false);
	}
}