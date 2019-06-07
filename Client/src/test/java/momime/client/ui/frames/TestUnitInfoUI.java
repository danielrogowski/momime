package momime.client.ui.frames;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.calculations.ClientUnitCalculations;
import momime.client.database.ClientDatabaseEx;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.UnitGfx;
import momime.client.graphics.database.UnitSkillGfx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.ui.panels.UnitInfoPanel;
import momime.client.ui.renderer.UnitAttributeListCellRenderer;
import momime.client.ui.renderer.UnitSkillListCellRenderer;
import momime.client.utils.AnimationController;
import momime.client.utils.ResourceValueClientUtilsImpl;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.common.database.Unit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.UnitUtils;

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
		
		// Mock entries from Graphics XML
		final UnitGfx unitGfx = new UnitGfx ();
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.findUnit (eq ("UN001"), anyString ())).thenReturn (unitGfx);

		// Mock entries from client DB
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final Unit longbowmen = new Unit ();
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
		
		// Set up unit to display
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitID ("UN001");
		unit.setOwningPlayerID (pd.getPlayerID ());
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getUnit ()).thenReturn (unit);
		when (xu.getUnitID ()).thenReturn ("UN001");
		when (xu.getUnitDefinition ()).thenReturn (longbowmen);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getOwningPlayer ()).thenReturn (unitOwner);
		when (unitUtils.expandUnitDetails (unit, null, null, null, players, fow, db)).thenReturn (xu);
		
		// Movement
		final ClientUnitCalculations clientUnitCalc = mock (ClientUnitCalculations.class);
		
		final UnitSkillGfx movementSkill = new UnitSkillGfx ();
		movementSkill.setMovementIconImageFile ("/momime.client.graphics/unitSkills/USX01-move.png");
		when (clientUnitCalc.findPreferredMovementSkillGraphics (xu)).thenReturn (movementSkill);

		// Unit name
		final UnitClientUtils unitClientUtils = mock (UnitClientUtils.class);
		when (unitClientUtils.getUnitName (unit, UnitNameType.RACE_UNIT_NAME)).thenReturn ("Longbowmen");
		
		// Animation controller
		final AnimationController anim = mock (AnimationController.class);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) ClientTestData.createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.panels/UnitInfoPanel.xml"));
		layout.buildMaps ();
		
		// Set up panel
		final UnitInfoPanel panel = new UnitInfoPanel ();
		panel.setUnitInfoLayout (layout);
		panel.setUtils (utils);
		panel.setLanguageHolder (langHolder);
		panel.setLanguageChangeMaster (langMaster);
		panel.setClient (client);
		panel.setGraphicsDB (gfx);
		panel.setUnitSkillListCellRenderer (renderer);
		panel.setUnitAttributeListCellRenderer (attributeRenderer);
		panel.setResourceValueClientUtils (resourceValueClientUtils);
		panel.setClientUnitCalculations (clientUnitCalc);
		panel.setUnitUtils (unitUtils);
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
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);

		when (lang.findCategoryEntry ("frmUnitInfo", "OK")).thenReturn ("OK");
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// Mock entries from Graphics XML
		final UnitGfx unitGfx = new UnitGfx ();
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.findUnit (eq ("UN001"), anyString ())).thenReturn (unitGfx);
		
		// Mock entries from client DB
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final Unit longbowmen = new Unit ();
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
		
		// Set up unit to display
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitID ("UN001");
		unit.setOwningPlayerID (pd.getPlayerID ());
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getUnit ()).thenReturn (unit);
		when (xu.getUnitID ()).thenReturn ("UN001");
		when (xu.getUnitDefinition ()).thenReturn (longbowmen);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getOwningPlayer ()).thenReturn (unitOwner);
		when (unitUtils.expandUnitDetails (unit, null, null, null, players, fow, db)).thenReturn (xu);
		
		// Movement
		final ClientUnitCalculations clientUnitCalc = mock (ClientUnitCalculations.class);
		
		final UnitSkillGfx movementSkill = new UnitSkillGfx ();
		movementSkill.setMovementIconImageFile ("/momime.client.graphics/unitSkills/USX01-move.png");
		when (clientUnitCalc.findPreferredMovementSkillGraphics (xu)).thenReturn (movementSkill);

		// Unit name
		final UnitClientUtils unitClientUtils = mock (UnitClientUtils.class);
		when (unitClientUtils.getUnitName (unit, UnitNameType.RACE_UNIT_NAME)).thenReturn ("Longbowmen");

		// Animation controller
		final AnimationController anim = mock (AnimationController.class);

		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) ClientTestData.createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.panels/UnitInfoPanel.xml"));
		layout.buildMaps ();
		
		// Set up panel
		final UnitInfoPanel panel = new UnitInfoPanel ();
		panel.setUnitInfoLayout (layout);
		panel.setUtils (utils);
		panel.setLanguageHolder (langHolder);
		panel.setLanguageChangeMaster (langMaster);
		panel.setClient (client);
		panel.setGraphicsDB (gfx);
		panel.setUnitSkillListCellRenderer (renderer);
		panel.setUnitAttributeListCellRenderer (attributeRenderer);
		panel.setResourceValueClientUtils (resourceValueClientUtils);
		panel.setClientUnitCalculations (clientUnitCalc);
		panel.setUnitUtils (unitUtils);
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
		frame.setUnitInfoPanel (panel);

		// Display form		
		frame.setVisible (true);
		Thread.sleep (5000);
		frame.setVisible (false);
	}
}