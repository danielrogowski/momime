package momime.client.ui.dialogs;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.VariableMana;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.ui.frames.CombatUI;
import momime.client.utils.TextUtilsImpl;
import momime.common.calculations.SpellCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Language;
import momime.common.database.ProductionTypeEx;
import momime.common.database.Spell;
import momime.common.database.SpellSetting;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.utils.SpellUtilsImpl;

/**
 * Tests the VariableManaUI class
 */
public final class TestVariableManaUI extends ClientTestData
{
	/**
	 * Tests the VariableManaUI form, on a spell where it takes multiple MP points to raise 1 damage point
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testVariableManaUI_ManaPerAdditionalDamagePoint () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final ProductionTypeEx manaProduction = new ProductionTypeEx ();
		manaProduction.getProductionTypeSuffix ().add (createLanguageText (Language.ENGLISH, "MP"));
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, "sliderPositionChanged")).thenReturn (manaProduction);
		
		// Mock entries from the language XML
		final VariableMana variableManaLang = new VariableMana ();
		variableManaLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Additional Power"));
		variableManaLang.getDamage ().add (createLanguageText (Language.ENGLISH, "VALUE damage"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getVariableMana ()).thenReturn (variableManaLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
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
		when (client.getClientDB ()).thenReturn (db);
		
		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (eq (players), eq (pd.getPlayerID ()), anyString ())).thenReturn (player);
		
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
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.dialogs/VariableManaUI.xml"));
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

	/**
	 * Tests the VariableManaUI form, on a spell where a single MP point raises multiple damage points
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testVariableManaUI_AdditionalDamagePointsPerMana () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();

		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final ProductionTypeEx manaProduction = new ProductionTypeEx ();
		manaProduction.getProductionTypeSuffix ().add (createLanguageText (Language.ENGLISH, "MP"));
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, "sliderPositionChanged")).thenReturn (manaProduction);
		
		// Mock entries from the language XML
		final VariableMana variableManaLang = new VariableMana ();
		variableManaLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Additional Power"));
		variableManaLang.getDamage ().add (createLanguageText (Language.ENGLISH, "VALUE damage"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getVariableMana ()).thenReturn (variableManaLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
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
		when (client.getClientDB ()).thenReturn (db);
		
		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (eq (players), eq (pd.getPlayerID ()), anyString ())).thenReturn (player);
		
		// Example spell; so for 20 MP we get 30 attack; for each additional +1 MP we get +3 attack; for maximum of 60 MP giving 150 attack
		final Spell spell = new Spell ();
		spell.setCombatBaseDamage (30);
		spell.setCombatMaxDamage (150);
		spell.setCombatAdditionalDamagePointsPerMana (3);
		spell.setCombatCastingCost (20);

		// Spell casting reduction
		final SpellCalculations spellCalc = mock (SpellCalculations.class);
		when (spellCalc.calculateCastingCostReduction (0, spellSettings, spell, pub.getPick (), db)).thenReturn (30d);
		
		final SpellUtilsImpl spellUtils = new SpellUtilsImpl ();
		spellUtils.setSpellCalculations (spellCalc);
		
		// Need combatUI to tell whether it is an overland or combat cast
		final CombatUI combatUI = new CombatUI ();
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.dialogs/VariableManaUI.xml"));
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