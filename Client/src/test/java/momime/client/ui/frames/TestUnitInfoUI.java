package momime.client.ui.frames;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import momime.client.MomClient;
import momime.client.calculations.MomClientUnitCalculations;
import momime.client.database.ClientDatabaseEx;
import momime.client.graphics.database.v0_9_5.UnitSkill;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.ui.panels.UnitInfoPanel;
import momime.client.ui.renderer.UnitSkillListCellRenderer;
import momime.client.utils.AnimationController;
import momime.client.utils.ResourceValueClientUtilsImpl;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.common.calculations.UnitHasSkillMergedList;
import momime.common.database.v0_9_5.Unit;
import momime.common.messages.v0_9_5.FogOfWarMemory;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.messages.v0_9_5.MomPersistentPlayerPrivateKnowledge;
import momime.common.utils.UnitUtils;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;

/**
 * Tests the UnitInfoUI class
 */
public final class TestUnitInfoUI
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
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);

		when (lang.findCategoryEntry ("frmUnitInfo", "OK")).thenReturn ("OK");
		when (lang.findCategoryEntry ("frmUnitInfo", "Dismiss")).thenReturn ("Dismiss");
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// Mock entries from client DB
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final Unit longbowmen = new Unit ();
		when (db.findUnit (eq ("UN001"), anyString ())).thenReturn (longbowmen);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getOurPlayerID ()).thenReturn (3);
		
		// FOW memory
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge ppk = new MomPersistentPlayerPrivateKnowledge ();
		ppk.setFogOfWarMemory (fow);
		
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (ppk);
		
		// Cell renderer
		final UnitSkillListCellRenderer renderer = new UnitSkillListCellRenderer ();
		
		// Set up production image generator
		final ResourceValueClientUtilsImpl resourceValueClientUtils = new ResourceValueClientUtilsImpl ();

		// Set up unit to display
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitID ("UN001");
		unit.setOwningPlayerID (3);
		
		// Movement
		final MomClientUnitCalculations clientUnitCalc = mock (MomClientUnitCalculations.class);
		
		final UnitSkill movementSkill = new UnitSkill ();
		movementSkill.setMovementIconImageFile ("/momime.client.graphics/unitSkills/USX01-move.png");
		when (clientUnitCalc.findPreferredMovementSkillGraphics (unit)).thenReturn (movementSkill);

		// Skills
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.mergeSpellEffectsIntoSkillList (fow.getMaintainedSpell (), unit)).thenReturn (new UnitHasSkillMergedList ());

		// Unit name
		final UnitClientUtils unitClientUtils = mock (UnitClientUtils.class);
		when (unitClientUtils.getUnitName (unit, UnitNameType.RACE_UNIT_NAME)).thenReturn ("Longbowmen");
		
		// Animation controller
		final AnimationController anim = mock (AnimationController.class);
		
		// Set up panel
		final UnitInfoPanel panel = new UnitInfoPanel ();
		panel.setUtils (utils);
		panel.setLanguageHolder (langHolder);
		panel.setLanguageChangeMaster (langMaster);
		panel.setClient (client);
		panel.setUnitSkillListCellRenderer (renderer);
		panel.setResourceValueClientUtils (resourceValueClientUtils);
		panel.setClientUnitCalculations (clientUnitCalc);
		panel.setUnitUtils (unitUtils);
		panel.setUnitClientUtils (unitClientUtils);
		panel.setAnim (anim);
		panel.setMediumFont (CreateFontsForTests.getMediumFont ());
		panel.setSmallFont (CreateFontsForTests.getSmallFont ());
		
		// Set up form
		final UnitInfoUI frame = new UnitInfoUI (); 
		frame.setUtils (utils);
		frame.setLanguageHolder (langHolder);
		frame.setLanguageChangeMaster (langMaster);
		frame.setClient (client);
		frame.setUnit (unit);
		frame.setUnitClientUtils (unitClientUtils);
		frame.setUnitInfoPanel (panel);

		// Display form		
		frame.setVisible (true);
		Thread.sleep (5000);
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
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);

		when (lang.findCategoryEntry ("frmUnitInfo", "OK")).thenReturn ("OK");
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// Mock entries from client DB
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final Unit longbowmen = new Unit ();
		when (db.findUnit (eq ("UN001"), anyString ())).thenReturn (longbowmen);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getOurPlayerID ()).thenReturn (3);
		
		// FOW memory
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge ppk = new MomPersistentPlayerPrivateKnowledge ();
		ppk.setFogOfWarMemory (fow);
		
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (ppk);
		
		// Cell renderer
		final UnitSkillListCellRenderer renderer = new UnitSkillListCellRenderer ();
		
		// Set up production image generator
		final ResourceValueClientUtilsImpl resourceValueClientUtils = new ResourceValueClientUtilsImpl ();

		// Set up unit to display
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitID ("UN001");
		unit.setOwningPlayerID (4);
		
		// Movement
		final MomClientUnitCalculations clientUnitCalc = mock (MomClientUnitCalculations.class);
		
		final UnitSkill movementSkill = new UnitSkill ();
		movementSkill.setMovementIconImageFile ("/momime.client.graphics/unitSkills/USX01-move.png");
		when (clientUnitCalc.findPreferredMovementSkillGraphics (unit)).thenReturn (movementSkill);

		// Skills
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.mergeSpellEffectsIntoSkillList (fow.getMaintainedSpell (), unit)).thenReturn (new UnitHasSkillMergedList ());

		// Unit name
		final UnitClientUtils unitClientUtils = mock (UnitClientUtils.class);
		when (unitClientUtils.getUnitName (unit, UnitNameType.RACE_UNIT_NAME)).thenReturn ("Longbowmen");

		// Animation controller
		final AnimationController anim = mock (AnimationController.class);
		
		// Set up panel
		final UnitInfoPanel panel = new UnitInfoPanel ();
		panel.setUtils (utils);
		panel.setLanguageHolder (langHolder);
		panel.setLanguageChangeMaster (langMaster);
		panel.setClient (client);
		panel.setUnitSkillListCellRenderer (renderer);
		panel.setResourceValueClientUtils (resourceValueClientUtils);
		panel.setClientUnitCalculations (clientUnitCalc);
		panel.setUnitUtils (unitUtils);
		panel.setUnitClientUtils (unitClientUtils);
		panel.setAnim (anim);
		panel.setMediumFont (CreateFontsForTests.getMediumFont ());
		panel.setSmallFont (CreateFontsForTests.getSmallFont ());
		
		// Set up form
		final UnitInfoUI frame = new UnitInfoUI (); 
		frame.setUtils (utils);
		frame.setLanguageHolder (langHolder);
		frame.setLanguageChangeMaster (langMaster);
		frame.setClient (client);
		frame.setUnit (unit);
		frame.setUnitClientUtils (unitClientUtils);
		frame.setUnitInfoPanel (panel);

		// Display form		
		frame.setVisible (true);
		Thread.sleep (5000);
	}
}