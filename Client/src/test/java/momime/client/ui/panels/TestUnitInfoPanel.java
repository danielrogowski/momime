package momime.client.ui.panels;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.junit.Test;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.actions.LoggingAction;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.calculations.ClientCityCalculations;
import momime.client.calculations.ClientUnitCalculations;
import momime.client.config.MomImeClientConfigEx;
import momime.client.database.ClientDatabaseEx;
import momime.client.graphics.database.CityViewElementGfx;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.ProductionTypeGfx;
import momime.client.graphics.database.ProductionTypeImageGfx;
import momime.client.graphics.database.UnitGfx;
import momime.client.graphics.database.UnitSkillGfx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.BuildingLang;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.UnitSkillLang;
import momime.client.language.replacer.UnitStatsLanguageVariableReplacer;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.ui.renderer.UnitAttributeListCellRenderer;
import momime.client.ui.renderer.UnitSkillListCellRenderer;
import momime.client.utils.AnimationControllerImpl;
import momime.client.utils.ResourceValueClientUtilsImpl;
import momime.client.utils.TextUtilsImpl;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.common.calculations.UnitCalculations;
import momime.common.database.Building;
import momime.common.database.BuildingPopulationProductionModifier;
import momime.common.database.ProductionTypeAndUndoubledValue;
import momime.common.database.Unit;
import momime.common.database.UnitSkillAndValue;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillPositiveNegative;
import momime.common.database.UnitSkillTypeID;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.UnitUtils;

/**
 * Tests the UnitInfoPanel class
 */
public final class TestUnitInfoPanel
{
	/**
	 * Tests the info panel to display a building
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testBuildingInfoPanel () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		
		when (lang.findCategoryEntry ("frmChangeConstruction", "Upkeep")).thenReturn ("Upkeep");
		when (lang.findCategoryEntry ("frmChangeConstruction", "Cost")).thenReturn ("Cost");
		when (lang.findCategoryEntry ("frmChangeConstruction", "BuildingURN")).thenReturn ("Building URN");

		final BuildingLang granaryName = new BuildingLang ();
		granaryName.setBuildingName ("Granary");
		granaryName.setBuildingHelpText ("This is the long description of what a Granary does");
		when (lang.findBuilding ("BL01")).thenReturn (granaryName);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Client city calculations derive language strings
		final ClientCityCalculations clientCityCalc = mock (ClientCityCalculations.class);
		when (clientCityCalc.describeWhatBuildingAllows ("BL01", new MapCoordinates3DEx (20, 10, 0))).thenReturn ("This is what the Granary allows");
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Mock entries from the graphics XML
		final CityViewElementGfx granaryImage = new CityViewElementGfx ();
		granaryImage.setCityViewImageFile ("/momime.client.graphics/cityView/buildings/BL29.png");
		
		final ProductionTypeImageGfx goldImageContainer = new ProductionTypeImageGfx ();
		goldImageContainer.setProductionImageFile ("/momime.client.graphics/production/gold/1.png");
		goldImageContainer.setProductionValue ("1");
		
		final ProductionTypeGfx goldImages = new ProductionTypeGfx ();
		goldImages.getProductionTypeImage ().add (goldImageContainer);
		goldImages.buildMap ();
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.findBuilding (eq ("BL01"), anyString ())).thenReturn (granaryImage);
		when (gfx.findProductionType ("RE01", "generateUpkeepImage")).thenReturn (goldImages);
		
		// Mock entries from client DB
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final Building granary = new Building ();
		granary.setProductionCost (60);

		final BuildingPopulationProductionModifier upkeep = new BuildingPopulationProductionModifier ();
		upkeep.setProductionTypeID ("RE01");
		upkeep.setDoubleAmount (-4);
		granary.getBuildingPopulationProductionModifier ().add (upkeep);
		
		when (db.findBuilding ("BL01", "showBuilding")).thenReturn (granary);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Set up production image generator
		final ResourceValueClientUtilsImpl resourceValueClientUtils = new ResourceValueClientUtilsImpl ();
		resourceValueClientUtils.setGraphicsDB (gfx);
		resourceValueClientUtils.setUtils (utils);
		
		// Animation controller
		final AnimationControllerImpl anim = new AnimationControllerImpl ();
		anim.setGraphicsDB (gfx);
		anim.setUtils (utils);
		
		// Set up building to display
		final MemoryBuilding building = new MemoryBuilding ();
		building.setBuildingID ("BL01");
		building.setCityLocation (new MapCoordinates3DEx (20, 10, 0));

		// Cell renderers
		final UnitAttributeListCellRenderer attributeRenderer = new UnitAttributeListCellRenderer ();
		attributeRenderer.setLanguageHolder (langHolder);

		final UnitSkillListCellRenderer skillRenderer = new UnitSkillListCellRenderer ();
		skillRenderer.setLanguageHolder (langHolder);
		skillRenderer.setGraphicsDB (gfx);
		skillRenderer.setUtils (utils);
		
		// Create some dummy actions for buttons
		final Action blahAction = new LoggingAction ("Blah", (ev) -> {});
		final Action pantsAction = new LoggingAction ("Pants", (ev) -> {});

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
		panel.setResourceValueClientUtils (resourceValueClientUtils);
		panel.setAnim (anim);
		panel.setClientCityCalculations (clientCityCalc);
		panel.setUnitAttributeListCellRenderer (attributeRenderer);
		panel.setUnitSkillListCellRenderer (skillRenderer);
		panel.setTextUtils (new TextUtilsImpl ());
		panel.setMediumFont (CreateFontsForTests.getMediumFont ());
		panel.setSmallFont (CreateFontsForTests.getSmallFont ());
		panel.getActions ().add (blahAction);
		panel.getActions ().add (pantsAction);
		panel.getPanel ();
		panel.showBuilding (building);

		// Set up a dummy frame to display the panel
		final JFrame frame = new JFrame ("testBuildingInfoPanel");
		frame.setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		frame.setContentPane (panel.getPanel ());
		frame.pack ();
		frame.setLocationRelativeTo (null);

		frame.setVisible (true);
		Thread.sleep (5000);
		frame.setVisible (false);
	}

	/**
	 * Tests the info panel to display a unit
	 * @param actions Action buttons to display
	 * @throws Exception If there is a problem
	 */
	private final void testUnitInfoPanel (final Action [] actions) throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);

		when (lang.findCategoryEntry ("frmChangeConstruction", "Upkeep")).thenReturn ("Upkeep");
		when (lang.findCategoryEntry ("frmChangeConstruction", "Cost")).thenReturn ("Cost");
		when (lang.findCategoryEntry ("frmChangeConstruction", "UnitURN")).thenReturn ("Unit URN");
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// Mock entries from the graphics XML
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		
		final ProductionTypeImageGfx goldImageContainer = new ProductionTypeImageGfx ();
		goldImageContainer.setProductionImageFile ("/momime.client.graphics/production/gold/1.png");
		goldImageContainer.setProductionValue ("1");
		
		final ProductionTypeGfx goldImages = new ProductionTypeGfx ();
		goldImages.getProductionTypeImage ().add (goldImageContainer);
		goldImages.buildMap ();
		when (gfx.findProductionType ("RE01", "generateUpkeepImage")).thenReturn (goldImages);

		final ProductionTypeImageGfx rationsImageContainer = new ProductionTypeImageGfx ();
		rationsImageContainer.setProductionImageFile ("/momime.client.graphics/production/rations/1.png");
		rationsImageContainer.setProductionValue ("1");

		final ProductionTypeGfx rationsImages = new ProductionTypeGfx ();
		rationsImages.getProductionTypeImage ().add (rationsImageContainer);
		rationsImages.buildMap ();
		when (gfx.findProductionType ("RE02", "generateUpkeepImage")).thenReturn (rationsImages);
		
		final UnitGfx unitGfx = new UnitGfx ();
		when (gfx.findUnit (eq ("UN001"), anyString ())).thenReturn (unitGfx);
		
		// Mock entries from client DB
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final Unit longbowmen = new Unit ();
		longbowmen.setProductionCost (80);
		longbowmen.setRangedAttackType ("RAT01");

		final ProductionTypeAndUndoubledValue goldUpkeep = new ProductionTypeAndUndoubledValue ();
		goldUpkeep.setProductionTypeID ("RE01");
		longbowmen.getUnitUpkeep ().add (goldUpkeep);

		final ProductionTypeAndUndoubledValue rationsUpkeep = new ProductionTypeAndUndoubledValue ();
		rationsUpkeep.setProductionTypeID ("RE02");
		longbowmen.getUnitUpkeep ().add (rationsUpkeep);
		
		when (db.findUnit (eq ("UN001"), anyString ())).thenReturn (longbowmen);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Players
		final PlayerDescription pd = new PlayerDescription ();
		pd.setHuman (true);
		pd.setPlayerID (1);
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		final PlayerPublicDetails player = new PlayerPublicDetails (pd, pub, null);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player);
		when (client.getPlayers ()).thenReturn (players);

		// FOW memory
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge ppk = new MomPersistentPlayerPrivateKnowledge ();
		ppk.setFogOfWarMemory (fow);
		
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (ppk);

		// Set up production image generator
		final ResourceValueClientUtilsImpl resourceValueClientUtils = new ResourceValueClientUtilsImpl ();
		resourceValueClientUtils.setGraphicsDB (gfx);
		resourceValueClientUtils.setUtils (utils);

		// Animation controller
		final AnimationControllerImpl anim = new AnimationControllerImpl ();
		anim.setGraphicsDB (gfx);
		anim.setUtils (utils);
		
		// Set up unit to display
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID ("UN001");
		unit.setWeaponGrade (2);
		unit.setOwningPlayerID (1);
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getUnit ()).thenReturn (unit);
		when (xu.getUnitID ()).thenReturn ("UN001");
		when (xu.getUnitDefinition ()).thenReturn (longbowmen);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getOwningPlayer ()).thenReturn (player);
		when (unitUtils.expandUnitDetails (unit, null, null, null, players, fow, db)).thenReturn (xu);
		
		// Skills
		final UnitClientUtils unitClientUtils = mock (UnitClientUtils.class);
		final Set<String> modifiedSkillIDs = new HashSet<String> ();
		for (int n = 1; n <= 5; n++)
		{
			final String skillID = "US0" + n;
			
			// Lang
			final UnitSkillLang skillLang = new UnitSkillLang ();
			skillLang.setUnitSkillDescription ("Name of skill " + skillID);
			when (lang.findUnitSkill (skillID)).thenReturn (skillLang);

			// Gfx
			final UnitSkillGfx skillGfx = new UnitSkillGfx ();
			skillGfx.setUnitSkillTypeID (UnitSkillTypeID.NO_VALUE);
			skillGfx.setUnitSkillImageFile ("/momime.client.graphics/unitSkills/US0" + (n+13) + "-icon.png");
			
			when (gfx.findUnitSkill (eq (skillID), anyString ())).thenReturn (skillGfx);
			
			// Unit stat
			final UnitSkillAndValue skill = new UnitSkillAndValue ();
			skill.setUnitSkillID (skillID);
			unit.getUnitHasSkill ().add (skill);
			
			modifiedSkillIDs.add (skillID);
			when (xu.hasModifiedSkill (skillID)).thenReturn (n % 2 == 0);

			// Icon
			when (unitClientUtils.getUnitSkillSingleIcon (xu, skillID)).thenReturn (utils.loadImage ("/momime.client.graphics/unitSkills/US0" + (n+13) + "-icon.png"));
		}
		
		// Attributes
		final UnitCalculations unitCalc = mock (UnitCalculations.class);

		int unitAttrNo = 0;
		for (final String unitAttributeDesc : new String [] {"Melee", "Ranged", "+ to Hit", "Defence", "Resistance", "Hit Points", "+ to Block"})
		{
			unitAttrNo++;
			final String attrID = "UA0" + unitAttrNo;

			// Lang
			final UnitSkillLang unitAttrLang = new UnitSkillLang ();
			unitAttrLang.setUnitSkillDescription (unitAttributeDesc);
			when (lang.findUnitSkill (attrID)).thenReturn (unitAttrLang);

			// Gfx
			final UnitSkillGfx unitAttrGfx = new UnitSkillGfx ();
			unitAttrGfx.setUnitSkillTypeID (UnitSkillTypeID.ATTRIBUTE);
			when (gfx.findUnitSkill (eq (attrID), anyString ())).thenReturn (unitAttrGfx);
			
			when (unitClientUtils.generateAttributeImage (xu, attrID)).thenReturn (ClientTestData.createSolidImage (289, 15, unitAttrNo * 35));
			
			// Unit stat
			final UnitSkillAndValue attr = new UnitSkillAndValue ();
			attr.setUnitSkillID (attrID);
			unit.getUnitHasSkill ().add (attr);

			modifiedSkillIDs.add (attrID);
			when (xu.filterModifiedSkillValue (attrID, UnitSkillComponent.ALL, UnitSkillPositiveNegative.POSITIVE)).thenReturn (1);		// Just to get past check
		}
		when (xu.listModifiedSkillIDs ()).thenReturn (modifiedSkillIDs);
		
		// Upkeep
		when (xu.getModifiedUpkeepValue ("RE01")).thenReturn (2);
		when (xu.getModifiedUpkeepValue ("RE02")).thenReturn (1);
		
		final Set<String> upkeeps = new HashSet<String> ();
		upkeeps.add ("RE01");
		upkeeps.add ("RE02");
		when (xu.listModifiedUpkeepProductionTypeIDs ()).thenReturn (upkeeps);
		
		// Unit name
		when (unitClientUtils.getUnitName (unit, UnitNameType.RACE_UNIT_NAME)).thenReturn ("Longbowmen");
		
		// Attribute icons
		unitAttrNo = 0;
		for (final String unitAttributeImage : new String [] {"meleeNormal", null, "plusToHit", "defenceNormal", "resist", "hitPoints", "plusToBlock"})
		{
			unitAttrNo++;
			final String useAttributeImage = (unitAttributeImage != null) ? "/momime.client.graphics/unitSkills/" + unitAttributeImage + ".png" :
				"/momime.client.graphics/rangedAttacks/rock/iconNormal.png";
			
			when (unitClientUtils.getUnitSkillComponentBreakdownIcon (xu, "UA0" + unitAttrNo)).thenReturn (utils.loadImage (useAttributeImage));
		}
		
		// Cell renderers
		final UnitStatsLanguageVariableReplacer replacer = mock (UnitStatsLanguageVariableReplacer.class);
		for (int n = 1; n <= 5; n++)
			when (replacer.replaceVariables ("Name of skill US0" + n)).thenReturn ("Name of skill US0" + n);
		
		// Cell renderers
		final UnitAttributeListCellRenderer attributeRenderer = new UnitAttributeListCellRenderer ();
		attributeRenderer.setLanguageHolder (langHolder);

		final UnitSkillListCellRenderer skillRenderer = new UnitSkillListCellRenderer ();
		skillRenderer.setUnitStatsReplacer (replacer);
		skillRenderer.setLanguageHolder (langHolder);
		skillRenderer.setGraphicsDB (gfx);
		skillRenderer.setUtils (utils);
		skillRenderer.setUnitClientUtils (unitClientUtils);

		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) ClientTestData.createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.panels/UnitInfoPanel.xml"));
		layout.buildMaps ();
		
		// Set up panel
		final ClientUnitCalculations clientUnitCalc = mock (ClientUnitCalculations.class);
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);

		final UnitInfoPanel panel = new UnitInfoPanel ();
		panel.setUnitInfoLayout (layout);
		panel.setUtils (utils);
		panel.setLanguageHolder (langHolder);
		panel.setLanguageChangeMaster (langMaster);
		panel.setClient (client);
		panel.setGraphicsDB (gfx);
		panel.setResourceValueClientUtils (resourceValueClientUtils);
		panel.setAnim (anim);
		panel.setUnitCalculations (unitCalc);
		panel.setClientUnitCalculations (clientUnitCalc);
		panel.setUnitClientUtils (unitClientUtils);
		panel.setUnitAttributeListCellRenderer (attributeRenderer);
		panel.setUnitSkillListCellRenderer (skillRenderer);
		panel.setTextUtils (new TextUtilsImpl ());
		panel.setMediumFont (CreateFontsForTests.getMediumFont ());
		panel.setSmallFont (CreateFontsForTests.getSmallFont ());
		panel.setPlayerPickUtils (playerPickUtils);
		panel.setUnitUtils (unitUtils);
		panel.setClientConfig (new MomImeClientConfigEx ());
		
		if (actions != null)
			for (final Action action : actions)
				panel.getActions ().add (action);
		
		panel.setButtonsPositionRight (true);
		panel.getPanel ();
		panel.showUnit (unit);

		// Set up a dummy frame to display the panel
		final JFrame frame = new JFrame ("testUnitInfoPanel");
		frame.setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		frame.setContentPane (panel.getPanel ());
		frame.pack ();
		frame.setLocationRelativeTo (null);

		frame.setVisible (true);
		Thread.sleep (5000);
		frame.setVisible (false);
	}

	/**
	 * Tests the info panel to display a unit, with 0 buttons
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUnitInfoPanel_NoButtons () throws Exception
	{
		testUnitInfoPanel (null);
	}

	/**
	 * Tests the info panel to display a unit, with 1 button
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUnitInfoPanel_OneButtons () throws Exception
	{
		final Action blahAction = new LoggingAction ("Blah", (ev) -> {});
		
		testUnitInfoPanel (new Action [] {blahAction});
	}

	/**
	 * Tests the info panel to display a unit, with 2 buttons
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUnitInfoPanel_TwoButtons () throws Exception
	{
		final Action blahAction = new LoggingAction ("Blah", (ev) -> {});
		final Action pantsAction = new LoggingAction ("Pants", (ev) -> {});
		
		testUnitInfoPanel (new Action [] {blahAction, pantsAction});
	}
	
	/**
	 * Tests the info panel to display a unit, with 3 buttons
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUnitInfoPanel_ThreeButtons () throws Exception
	{
		final Action blahAction = new LoggingAction ("Blah", (ev) -> {});
		final Action pantsAction = new LoggingAction ("Pants", (ev) -> {});
		final Action yuckAction = new LoggingAction ("Yuck", (ev) -> {});
		
		testUnitInfoPanel (new Action [] {blahAction, pantsAction, yuckAction});
	}
}