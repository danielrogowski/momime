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
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.ChangeConstructionScreen;
import momime.client.languages.database.Simple;
import momime.client.languages.database.UnitInfoScreen;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.ui.panels.UnitInfoPanel;
import momime.client.ui.renderer.UnitAttributeListCellRenderer;
import momime.client.ui.renderer.UnitSkillListCellRenderer;
import momime.client.utils.AnimationController;
import momime.client.utils.ResourceValueClientUtilsImpl;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.client.utils.WizardClientUtils;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.Language;
import momime.common.database.UnitEx;
import momime.common.database.UnitSkillEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.PlayerPickUtils;

/**
 * Tests the UnitInfoUI class
 */
@ExtendWith(MockitoExtension.class)
public final class TestUnitInfoUI extends ClientTestData
{
	/**
	 * Tests the UnitInfoUI form displaying one of our units
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUnitInfoUI_OurUnit () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final Simple simpleLang = new Simple ();
		simpleLang.getOk ().add (createLanguageText (Language.ENGLISH, "OK"));
		
		final UnitInfoScreen unitInfoScreenLang = new UnitInfoScreen ();
		unitInfoScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "PLAYER_NAME's UNIT_NAME"));
		unitInfoScreenLang.getDismiss ().add (createLanguageText (Language.ENGLISH, "Dismiss"));

		final ChangeConstructionScreen changeConstructionScreenLang = new ChangeConstructionScreen ();
		changeConstructionScreenLang.getUpkeep ().add (createLanguageText (Language.ENGLISH, "Upkeep"));
		changeConstructionScreenLang.getCost ().add (createLanguageText (Language.ENGLISH, "Cost"));
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSimple ()).thenReturn (simpleLang);
		when (lang.getUnitInfoScreen ()).thenReturn (unitInfoScreenLang);
		when (lang.getChangeConstructionScreen ()).thenReturn (changeConstructionScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Mock entries from client DB
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx longbowmen = new UnitEx ();
		longbowmen.setUnitMagicRealm ("X");
		when (db.findUnit (eq ("UN001"), anyString ())).thenReturn (longbowmen);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// FOW memory
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge ppk = new MomPersistentPlayerPrivateKnowledge ();
		ppk.setFogOfWarMemory (fow);
		
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (ppk);
		
		// Cell renderer
		final UnitSkillListCellRenderer renderer = new UnitSkillListCellRenderer ();
		
		final UnitAttributeListCellRenderer attributeRenderer = new UnitAttributeListCellRenderer ();
		attributeRenderer.setLanguageHolder (langHolder);
		
		// Set up production image generator
		final ResourceValueClientUtilsImpl resourceValueClientUtils = new ResourceValueClientUtilsImpl ();
		
		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setHuman (true);
		pd.setPlayerID (3);
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		
		final PlayerPublicDetails unitOwner = new PlayerPublicDetails (pd, pub, null);
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (unitOwner);
		
		when (client.getOurPlayerID ()).thenReturn (pd.getPlayerID ());
		when (client.getPlayers ()).thenReturn (players);
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		final WizardClientUtils wizardClientUtils = mock (WizardClientUtils.class);
		
		when (multiplayerSessionUtils.findPlayerWithID (players, pd.getPlayerID ())).thenReturn (unitOwner);
		when (wizardClientUtils.getPlayerName (unitOwner)).thenReturn ("Nigel");
		
		// Wizard
		final KnownWizardDetails owningWizard = new KnownWizardDetails ();
		
		// Set up unit to display
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitID ("UN001");
		unit.setOwningPlayerID (pd.getPlayerID ());
		
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getUnit ()).thenReturn (unit);
		when (xu.getUnitID ()).thenReturn ("UN001");
		when (xu.getUnitDefinition ()).thenReturn (longbowmen);
		when (xu.getOwningWizard ()).thenReturn (owningWizard);
		when (expand.expandUnitDetails (unit, null, null, null, players, fow, db)).thenReturn (xu);
		
		// Movement
		final UnitCalculations unitCalc = mock (UnitCalculations.class);
		
		final UnitSkillEx movementSkill = new UnitSkillEx ();
		movementSkill.setMovementIconImageFile ("/momime.client.graphics/unitSkills/USX01-move.png");

		// Unit name
		final UnitClientUtils unitClientUtils = mock (UnitClientUtils.class);
		when (unitClientUtils.getUnitName (unit, UnitNameType.RACE_UNIT_NAME)).thenReturn ("Longbowmen");
		
		// Animation controller
		final AnimationController anim = mock (AnimationController.class);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.panels/UnitInfoPanel.xml"));
		layout.buildMaps ();
		
		// Set up panel
		final UnitInfoPanel panel = new UnitInfoPanel ();
		panel.setUnitInfoLayout (layout);
		panel.setUtils (utils);
		panel.setLanguageHolder (langHolder);
		panel.setLanguageChangeMaster (langMaster);
		panel.setClient (client);
		panel.setUnitSkillListCellRenderer (renderer);
		panel.setUnitAttributeListCellRenderer (attributeRenderer);
		panel.setResourceValueClientUtils (resourceValueClientUtils);
		panel.setUnitCalculations (unitCalc);
		panel.setExpandUnitDetails (expand);
		panel.setUnitClientUtils (unitClientUtils);
		panel.setAnim (anim);
		panel.setMediumFont (CreateFontsForTests.getMediumFont ());
		panel.setSmallFont (CreateFontsForTests.getSmallFont ());
		panel.setPlayerPickUtils (mock (PlayerPickUtils.class));
		
		// Set up form
		final UnitInfoUI frame = new UnitInfoUI (); 
		frame.setUtils (utils);
		frame.setLanguageHolder (langHolder);
		frame.setLanguageChangeMaster (langMaster);
		frame.setClient (client);
		frame.setUnit (unit);
		frame.setUnitClientUtils (unitClientUtils);
		frame.setMultiplayerSessionUtils (multiplayerSessionUtils);
		frame.setWizardClientUtils (wizardClientUtils);
		frame.setUnitInfoPanel (panel);

		// Display form		
		frame.setVisible (true);
		Thread.sleep (5000);
		frame.setVisible (false);
	}

	/**
	 * Tests the UnitInfoUI form displaying somebody else's unit
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUnitInfoUI_EnemyUnit () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final Simple simpleLang = new Simple ();
		simpleLang.getOk ().add (createLanguageText (Language.ENGLISH, "OK"));
		
		final UnitInfoScreen unitInfoScreenLang = new UnitInfoScreen ();
		unitInfoScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "PLAYER_NAME's UNIT_NAME"));

		final ChangeConstructionScreen changeConstructionScreenLang = new ChangeConstructionScreen ();
		changeConstructionScreenLang.getUpkeep ().add (createLanguageText (Language.ENGLISH, "Upkeep"));
		changeConstructionScreenLang.getCost ().add (createLanguageText (Language.ENGLISH, "Cost"));
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSimple ()).thenReturn (simpleLang);
		when (lang.getUnitInfoScreen ()).thenReturn (unitInfoScreenLang);
		when (lang.getChangeConstructionScreen ()).thenReturn (changeConstructionScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// Mock entries from client DB
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx longbowmen = new UnitEx ();
		longbowmen.setUnitMagicRealm ("X");
		when (db.findUnit (eq ("UN001"), anyString ())).thenReturn (longbowmen);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// FOW memory
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge ppk = new MomPersistentPlayerPrivateKnowledge ();
		ppk.setFogOfWarMemory (fow);
		
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (ppk);
		
		// Cell renderer
		final UnitSkillListCellRenderer renderer = new UnitSkillListCellRenderer ();
		
		final UnitAttributeListCellRenderer attributeRenderer = new UnitAttributeListCellRenderer ();
		attributeRenderer.setLanguageHolder (langHolder);
		
		// Set up production image generator
		final ResourceValueClientUtilsImpl resourceValueClientUtils = new ResourceValueClientUtilsImpl ();

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setHuman (true);
		pd.setPlayerID (3);
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		
		final PlayerPublicDetails unitOwner = new PlayerPublicDetails (pd, pub, null);
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (unitOwner);
		
		when (client.getOurPlayerID ()).thenReturn (pd.getPlayerID () + 1);		// Purposefully make it different
		when (client.getPlayers ()).thenReturn (players);
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		final WizardClientUtils wizardClientUtils = mock (WizardClientUtils.class);
		
		when (multiplayerSessionUtils.findPlayerWithID (players, pd.getPlayerID ())).thenReturn (unitOwner);
		when (wizardClientUtils.getPlayerName (unitOwner)).thenReturn ("Nigel");
		
		// Wizard
		final KnownWizardDetails owningWizard = new KnownWizardDetails ();
		
		// Set up unit to display
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitID ("UN001");
		unit.setOwningPlayerID (pd.getPlayerID ());
		
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getUnit ()).thenReturn (unit);
		when (xu.getUnitID ()).thenReturn ("UN001");
		when (xu.getUnitDefinition ()).thenReturn (longbowmen);
		when (xu.getOwningWizard ()).thenReturn (owningWizard);
		when (expand.expandUnitDetails (unit, null, null, null, players, fow, db)).thenReturn (xu);
		
		// Movement
		final UnitCalculations unitCalc = mock (UnitCalculations.class);
		
		final UnitSkillEx movementSkill = new UnitSkillEx ();
		movementSkill.setMovementIconImageFile ("/momime.client.graphics/unitSkills/USX01-move.png");

		// Unit name
		final UnitClientUtils unitClientUtils = mock (UnitClientUtils.class);
		when (unitClientUtils.getUnitName (unit, UnitNameType.RACE_UNIT_NAME)).thenReturn ("Longbowmen");

		// Animation controller
		final AnimationController anim = mock (AnimationController.class);

		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.panels/UnitInfoPanel.xml"));
		layout.buildMaps ();
		
		// Set up panel
		final UnitInfoPanel panel = new UnitInfoPanel ();
		panel.setUnitInfoLayout (layout);
		panel.setUtils (utils);
		panel.setLanguageHolder (langHolder);
		panel.setLanguageChangeMaster (langMaster);
		panel.setClient (client);
		panel.setUnitSkillListCellRenderer (renderer);
		panel.setUnitAttributeListCellRenderer (attributeRenderer);
		panel.setResourceValueClientUtils (resourceValueClientUtils);
		panel.setUnitCalculations (unitCalc);
		panel.setExpandUnitDetails (expand);
		panel.setUnitClientUtils (unitClientUtils);
		panel.setAnim (anim);
		panel.setMediumFont (CreateFontsForTests.getMediumFont ());
		panel.setSmallFont (CreateFontsForTests.getSmallFont ());
		panel.setPlayerPickUtils (mock (PlayerPickUtils.class));
		
		// Set up form
		final UnitInfoUI frame = new UnitInfoUI (); 
		frame.setUtils (utils);
		frame.setLanguageHolder (langHolder);
		frame.setLanguageChangeMaster (langMaster);
		frame.setClient (client);
		frame.setUnit (unit);
		frame.setUnitClientUtils (unitClientUtils);
		frame.setMultiplayerSessionUtils (multiplayerSessionUtils);
		frame.setWizardClientUtils (wizardClientUtils);
		frame.setUnitInfoPanel (panel);

		// Display form		
		frame.setVisible (true);
		Thread.sleep (5000);
		frame.setVisible (false);
	}
}