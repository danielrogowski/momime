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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

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
import momime.client.config.MomImeClientConfig;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.language.replacer.UnitStatsLanguageVariableReplacer;
import momime.client.languages.database.ChangeConstructionScreen;
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
import momime.common.database.CityViewElement;
import momime.common.database.CommonDatabase;
import momime.common.database.Language;
import momime.common.database.ProductionTypeAndUndoubledValue;
import momime.common.database.ProductionTypeEx;
import momime.common.database.ProductionTypeImage;
import momime.common.database.UnitEx;
import momime.common.database.UnitSkillAndValue;
import momime.common.database.UnitSkillEx;
import momime.common.database.UnitSkillTypeID;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.PlayerPickUtils;

/**
 * Tests the UnitInfoPanel class
 */
@ExtendWith(MockitoExtension.class)
public final class TestUnitInfoPanel extends ClientTestData
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
		final ChangeConstructionScreen changeConstructionScreenLang = new ChangeConstructionScreen ();
		changeConstructionScreenLang.getUpkeep ().add (createLanguageText (Language.ENGLISH, "Upkeep"));
		changeConstructionScreenLang.getCost ().add (createLanguageText (Language.ENGLISH, "Cost"));
		changeConstructionScreenLang.getBuildingURN ().add (createLanguageText (Language.ENGLISH, "Building URN"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getChangeConstructionScreen ()).thenReturn (changeConstructionScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Client city calculations derive language strings
		final ClientCityCalculations clientCityCalc = mock (ClientCityCalculations.class);
		when (clientCityCalc.describeWhatBuildingAllows ("BL01", new MapCoordinates3DEx (20, 10, 0))).thenReturn ("This is what the Granary allows");
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Mock entries from client DB
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Building granary = new Building ();
		granary.getBuildingName ().add (createLanguageText (Language.ENGLISH, "Granary"));
		granary.getBuildingHelpText ().add (createLanguageText (Language.ENGLISH, "This is the long description of what a Granary does"));
		granary.setProductionCost (60);
		when (db.findBuilding (eq ("BL01"), anyString ())).thenReturn (granary);

		final BuildingPopulationProductionModifier upkeep = new BuildingPopulationProductionModifier ();
		upkeep.setProductionTypeID ("RE01");
		upkeep.setDoubleAmount (-4);
		granary.getBuildingPopulationProductionModifier ().add (upkeep);
		
		when (db.findBuilding ("BL01", "showBuilding")).thenReturn (granary);

		final ProductionTypeImage goldImageContainer = new ProductionTypeImage ();
		goldImageContainer.setProductionImageFile ("/momime.client.graphics/production/gold/1.png");
		goldImageContainer.setProductionValue ("1");
		
		final ProductionTypeEx goldImages = new ProductionTypeEx ();
		goldImages.getProductionTypeImage ().add (goldImageContainer);
		goldImages.buildMap ();
		
		when (db.findProductionType ("RE01", "generateUpkeepImage")).thenReturn (goldImages);

		final CityViewElement granaryImage = new CityViewElement ();
		granaryImage.setCityViewImageFile ("/momime.client.graphics/cityView/buildings/BL29.png");
		
		when (db.findCityViewElementBuilding (eq ("BL01"), anyString ())).thenReturn (granaryImage);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Set up production image generator
		final ResourceValueClientUtilsImpl resourceValueClientUtils = new ResourceValueClientUtilsImpl ();
		resourceValueClientUtils.setClient (client);
		resourceValueClientUtils.setUtils (utils);
		
		// Animation controller
		final AnimationControllerImpl anim = new AnimationControllerImpl ();
		anim.setUtils (utils);
		
		// Set up building to display
		final MemoryBuilding building = new MemoryBuilding ();
		building.setBuildingID ("BL01");
		building.setCityLocation (new MapCoordinates3DEx (20, 10, 0));

		// Cell renderers
		final UnitAttributeListCellRenderer attributeRenderer = new UnitAttributeListCellRenderer ();
		attributeRenderer.setLanguageHolder (langHolder);
		attributeRenderer.setClient (client);

		final UnitSkillListCellRenderer skillRenderer = new UnitSkillListCellRenderer ();
		skillRenderer.setLanguageHolder (langHolder);
		skillRenderer.setClient (client);
		skillRenderer.setUtils (utils);
		
		// Create some dummy actions for buttons
		final Action blahAction = new LoggingAction ("Blah", (ev) -> {});
		final Action pantsAction = new LoggingAction ("Pants", (ev) -> {});

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
		final ChangeConstructionScreen changeConstructionScreenLang = new ChangeConstructionScreen ();
		changeConstructionScreenLang.getUpkeep ().add (createLanguageText (Language.ENGLISH, "Upkeep"));
		changeConstructionScreenLang.getCost ().add (createLanguageText (Language.ENGLISH, "Cost"));
		changeConstructionScreenLang.getUnitURN ().add (createLanguageText (Language.ENGLISH, "Unit URN"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getChangeConstructionScreen ()).thenReturn (changeConstructionScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// Mock entries from the graphics XML
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		
		// Mock entries from client DB
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx longbowmen = new UnitEx ();
		longbowmen.setProductionCost (80);
		longbowmen.setRangedAttackType ("RAT01");

		final ProductionTypeAndUndoubledValue goldUpkeep = new ProductionTypeAndUndoubledValue ();
		goldUpkeep.setProductionTypeID ("RE01");
		longbowmen.getUnitUpkeep ().add (goldUpkeep);

		final ProductionTypeAndUndoubledValue rationsUpkeep = new ProductionTypeAndUndoubledValue ();
		rationsUpkeep.setProductionTypeID ("RE02");
		longbowmen.getUnitUpkeep ().add (rationsUpkeep);
		
		when (db.findUnit (eq ("UN001"), anyString ())).thenReturn (longbowmen);

		final ProductionTypeImage goldImageContainer = new ProductionTypeImage ();
		goldImageContainer.setProductionImageFile ("/momime.client.graphics/production/gold/1.png");
		goldImageContainer.setProductionValue ("1");
		
		final ProductionTypeEx goldImages = new ProductionTypeEx ();
		goldImages.getProductionTypeImage ().add (goldImageContainer);
		goldImages.buildMap ();
		when (db.findProductionType ("RE01", "generateUpkeepImage")).thenReturn (goldImages);

		final ProductionTypeImage rationsImageContainer = new ProductionTypeImage ();
		rationsImageContainer.setProductionImageFile ("/momime.client.graphics/production/rations/1.png");
		rationsImageContainer.setProductionValue ("1");

		final ProductionTypeEx rationsImages = new ProductionTypeEx ();
		rationsImages.getProductionTypeImage ().add (rationsImageContainer);
		rationsImages.buildMap ();
		when (db.findProductionType ("RE02", "generateUpkeepImage")).thenReturn (rationsImages);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Players
		final PlayerDescription pd = new PlayerDescription ();
		pd.setHuman (true);
		pd.setPlayerID (1);
		
		final PlayerPublicDetails player = new PlayerPublicDetails (pd, null, null);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player);
		when (client.getPlayers ()).thenReturn (players);
		
		// Wizard
		final KnownWizardDetails wizardDetails = new KnownWizardDetails ();

		// FOW memory
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge ppk = new MomPersistentPlayerPrivateKnowledge ();
		ppk.setFogOfWarMemory (fow);
		
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (ppk);

		// Set up production image generator
		final ResourceValueClientUtilsImpl resourceValueClientUtils = new ResourceValueClientUtilsImpl ();
		resourceValueClientUtils.setClient (client);
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
		
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getUnit ()).thenReturn (unit);
		when (xu.getUnitID ()).thenReturn ("UN001");
		when (xu.getUnitDefinition ()).thenReturn (longbowmen);
		when (xu.getOwningWizard ()).thenReturn (wizardDetails);
		when (expand.expandUnitDetails (unit, null, null, null, players, fow, db)).thenReturn (xu);
		
		// Skills
		final UnitClientUtils unitClientUtils = mock (UnitClientUtils.class);
		final Set<String> modifiedSkillIDs = new HashSet<String> ();
		for (int n = 1; n <= 5; n++)
		{
			final String skillID = "US0" + n;
			
			final UnitSkillEx skillDef = new UnitSkillEx ();
			skillDef.getUnitSkillDescription ().add (createLanguageText (Language.ENGLISH, "Name of skill " + skillID));
			skillDef.setUnitSkillTypeID (UnitSkillTypeID.NO_VALUE);
			skillDef.setUnitSkillImageFile ("/momime.client.graphics/unitSkills/US0" + (n+25) + "-icon.png");
			when (db.findUnitSkill (eq (skillID), anyString ())).thenReturn (skillDef);

			// Unit stat
			final UnitSkillAndValue skill = new UnitSkillAndValue ();
			skill.setUnitSkillID (skillID);
			unit.getUnitHasSkill ().add (skill);
			
			modifiedSkillIDs.add (skillID);
			when (xu.hasModifiedSkill (skillID)).thenReturn (n % 2 == 0);

			// Icon
			when (unitClientUtils.getUnitSkillSingleIcon (xu, skillID)).thenReturn (utils.loadImage ("/momime.client.graphics/unitSkills/US0" + (n+25) + "-icon.png"));
		}
		
		// Attributes
		final UnitCalculations unitCalc = mock (UnitCalculations.class);

		int unitAttrNo = 0;
		for (final String unitAttributeDesc : new String [] {"Melee", "Ranged", "+ to Hit", "Defence", "Resistance", "Hit Points", "+ to Block"})
		{
			unitAttrNo++;
			final String attrID = "UA0" + unitAttrNo;

			// Lang
			final UnitSkillEx skillDef = new UnitSkillEx ();
			skillDef.getUnitSkillDescription ().add (createLanguageText (Language.ENGLISH, unitAttributeDesc));
			skillDef.setUnitSkillTypeID (UnitSkillTypeID.ATTRIBUTE);
			when (db.findUnitSkill (eq (attrID), anyString ())).thenReturn (skillDef);

			// Gfx
			when (unitClientUtils.generateAttributeImage (xu, attrID)).thenReturn (createSolidImage (289, 15, unitAttrNo * 35));
			
			// Unit stat
			final UnitSkillAndValue attr = new UnitSkillAndValue ();
			attr.setUnitSkillID (attrID);
			unit.getUnitHasSkill ().add (attr);

			modifiedSkillIDs.add (attrID);
			when (xu.hasModifiedSkill (attrID)).thenReturn (true);
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
		
		// Cell renderers
		final UnitStatsLanguageVariableReplacer replacer = mock (UnitStatsLanguageVariableReplacer.class);
		for (int n = 1; n <= 5; n++)
			when (replacer.replaceVariables ("Name of skill US0" + n)).thenReturn ("Name of skill US0" + n);
		
		// Cell renderers
		final UnitAttributeListCellRenderer attributeRenderer = new UnitAttributeListCellRenderer ();
		attributeRenderer.setLanguageHolder (langHolder);
		attributeRenderer.setClient (client);

		final UnitSkillListCellRenderer skillRenderer = new UnitSkillListCellRenderer ();
		skillRenderer.setUnitStatsReplacer (replacer);
		skillRenderer.setLanguageHolder (langHolder);
		skillRenderer.setClient (client);
		skillRenderer.setUtils (utils);
		skillRenderer.setUnitClientUtils (unitClientUtils);

		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.panels/UnitInfoPanel.xml"));
		layout.buildMaps ();
		
		// Set up panel
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);

		final UnitInfoPanel panel = new UnitInfoPanel ();
		panel.setUnitInfoLayout (layout);
		panel.setUtils (utils);
		panel.setLanguageHolder (langHolder);
		panel.setLanguageChangeMaster (langMaster);
		panel.setClient (client);
		panel.setResourceValueClientUtils (resourceValueClientUtils);
		panel.setAnim (anim);
		panel.setUnitCalculations (unitCalc);
		panel.setUnitClientUtils (unitClientUtils);
		panel.setUnitAttributeListCellRenderer (attributeRenderer);
		panel.setUnitSkillListCellRenderer (skillRenderer);
		panel.setTextUtils (new TextUtilsImpl ());
		panel.setMediumFont (CreateFontsForTests.getMediumFont ());
		panel.setSmallFont (CreateFontsForTests.getSmallFont ());
		panel.setPlayerPickUtils (playerPickUtils);
		panel.setExpandUnitDetails (expand);
		panel.setClientConfig (new MomImeClientConfig ());
		
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