package momime.client.ui.frames;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.calculations.ClientCityCalculations;
import momime.client.database.ClientDatabaseEx;
import momime.client.graphics.database.AnimationGfx;
import momime.client.graphics.database.CityViewElementGfx;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.ProductionTypeGfx;
import momime.client.graphics.database.ProductionTypeImageGfx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.BuildingLang;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.ui.panels.UnitInfoPanel;
import momime.client.ui.renderer.BuildingListCellRenderer;
import momime.client.ui.renderer.UnitAttributeListCellRenderer;
import momime.client.ui.renderer.UnitListCellRenderer;
import momime.client.ui.renderer.UnitSkillListCellRenderer;
import momime.client.utils.AnimationControllerImpl;
import momime.client.utils.ResourceValueClientUtilsImpl;
import momime.client.utils.TextUtilsImpl;
import momime.common.calculations.CityCalculations;
import momime.common.database.Building;
import momime.common.database.BuildingPopulationProductionModifier;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.OverlandMapSize;
import momime.common.database.Race;
import momime.common.database.RaceCannotBuild;
import momime.common.database.Unit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;

/**
 * Tests the ChangeConstructionUI class
 */
public final class TestChangeConstructionUI extends ClientTestData
{
	/**
	 * Tests the ChangeConstructionUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testChangeConstructionUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();

		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCitySizeName ("CS01")).thenReturn ("City of CITY_NAME");
		when (lang.findCategoryEntry ("frmChangeConstruction", "Title")).thenReturn ("Change construction for CITY_NAME");
		when (lang.findCategoryEntry ("frmChangeConstruction", "Upkeep")).thenReturn ("Upkeep");
		when (lang.findCategoryEntry ("frmChangeConstruction", "Moves")).thenReturn ("Moves");
		when (lang.findCategoryEntry ("frmChangeConstruction", "Cost")).thenReturn ("Cost");
		when (lang.findCategoryEntry ("frmChangeConstruction", "OK")).thenReturn ("OK");
		when (lang.findCategoryEntry ("frmChangeConstruction", "Cancel")).thenReturn ("Cancel");

		final BuildingLang granaryName = new BuildingLang ();
		granaryName.setBuildingName ("Granary");
		when (lang.findBuilding ("BL04")).thenReturn (granaryName);
		
		final BuildingLang fightersGuildName = new BuildingLang ();
		fightersGuildName.setBuildingName ("Fighters' Guild");
		when (lang.findBuilding ("BL05")).thenReturn (fightersGuildName);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Client city calculations derive language strings
		final ClientCityCalculations clientCityCalc = mock (ClientCityCalculations.class);
		when (clientCityCalc.describeWhatBuildingAllows ("BL04", new MapCoordinates3DEx (20, 10, 0))).thenReturn ("This is what the Granary allows");
		when (clientCityCalc.describeWhatBuildingAllows ("BL05", new MapCoordinates3DEx (20, 10, 0))).thenReturn ("This is what the Fighters' Guild allows");
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Mock entries from the graphics XML
		final CityViewElementGfx granary = new CityViewElementGfx ();
		granary.setCityViewImageFile ("/momime.client.graphics/cityView/buildings/BL29.png");
		
		final AnimationGfx fightersGuildAnim = new AnimationGfx ();
		fightersGuildAnim.setAnimationSpeed (4);
		for (int n = 1; n <= 9; n++)
			fightersGuildAnim.getFrame ().add ("/momime.client.graphics/cityView/buildings/BL05-frame" + n + ".png");
		
		final CityViewElementGfx fightersGuild = new CityViewElementGfx ();
		fightersGuild.setCityViewAnimation ("FIGHTERS_GUILD");
		
		final ProductionTypeImageGfx plusOneImageContainer = new ProductionTypeImageGfx ();
		plusOneImageContainer.setProductionImageFile ("/momime.client.graphics/production/gold/1.png");
		plusOneImageContainer.setProductionValue ("1");
		
		final ProductionTypeGfx productionTypeImages = new ProductionTypeGfx ();
		productionTypeImages.getProductionTypeImage ().add (plusOneImageContainer);
		productionTypeImages.buildMap ();
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.findBuilding (eq ("BL04"), anyString ())).thenReturn (granary);
		when (gfx.findBuilding (eq ("BL05"), anyString ())).thenReturn (fightersGuild);
		when (gfx.findAnimation ("FIGHTERS_GUILD", "registerRepaintTrigger")).thenReturn (fightersGuildAnim);
		when (gfx.findProductionType ("RE01", "generateUpkeepImage")).thenReturn (productionTypeImages);
		
		// Client DB
		final List<Building> buildings = new ArrayList<Building> ();
		// for (int m = 1; m <= 6; m++)
		for (int n = 1; n <= 6; n++)
		{
			final Building building = new Building ();
			building.setBuildingID ("BL0" + n);
			building.setProductionCost (n * 50);
			
			final BuildingPopulationProductionModifier upkeep = new BuildingPopulationProductionModifier ();
			upkeep.setProductionTypeID ("RE01");
			upkeep.setDoubleAmount (n * -2);
			building.getBuildingPopulationProductionModifier ().add (upkeep);
			
			buildings.add (building);
		}
		
		final RaceCannotBuild raceCannotBuild = new RaceCannotBuild ();
		raceCannotBuild.setCannotBuildBuildingID ("BL02");
		
		final Race race = new Race ();
		race.getRaceCannotBuild ().add (raceCannotBuild);
		
		final List<Unit> units = new ArrayList<Unit> ();
		for (int n = 1; n <= 5; n++)
		{
			final Unit unit = new Unit ();
			unit.setUnitID ("UN00" + n);

//			if (n == 1)
				unit.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
//			else
//				unit.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
			
			if (n == 2)
				unit.setUnitRaceID ("RC01");
			else if (n == 3)
				unit.setUnitRaceID ("RC02");
			
			units.add (unit);
		}
		
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		doReturn (buildings).when (db).getBuildings ();
		doReturn (units).when (db).getUnits ();
		when (db.findRace ("RC01", "updateWhatCanBeConstructed")).thenReturn (race);
		
		// City data
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		cityData.setCitySizeID ("CS01");
		cityData.setCityName ("Blahdy Blah");

		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		
		final MapVolumeOfMemoryGridCells terrain = createOverlandMap (overlandMapSize);
		terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		final FogOfWarMemory fow = new FogOfWarMemory ();
		fow.setMap (terrain);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (fow);
		
		final MomClient client = mock (MomClient.class);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getSessionDescription ()).thenReturn (sd);
		
		// What city can construct
		final ClientCityCalculations clientCityCalculations = mock (ClientCityCalculations.class);
		
		final List<Building> buildingsCanConstruct = new ArrayList<Building> ();
		buildingsCanConstruct.add (buildings.get (3));
		buildingsCanConstruct.add (buildings.get (4));
		when (clientCityCalculations.listBuildingsCityCanConstruct (new MapCoordinates3DEx (20, 10, 0))).thenReturn (buildingsCanConstruct);

		// Current construction
		cityData.setCurrentlyConstructingBuildingID ("BL05");
		when (db.findBuilding (eq ("BL04"), anyString ())).thenReturn (buildings.get (3));
		when (db.findBuilding (eq ("BL05"), anyString ())).thenReturn (buildings.get (4));
		
		// Set up production image generator
		final ResourceValueClientUtilsImpl resourceValueClientUtils = new ResourceValueClientUtilsImpl ();
		resourceValueClientUtils.setGraphicsDB (gfx);
		resourceValueClientUtils.setUtils (utils);
		
		// Animation controller
		final AnimationControllerImpl anim = new AnimationControllerImpl ();
		anim.setGraphicsDB (gfx);
		anim.setUtils (utils);

		// Cell renderers
		final BuildingListCellRenderer buildingRenderer = new BuildingListCellRenderer ();
		buildingRenderer.setGraphicsDB (gfx);
		buildingRenderer.setLanguageHolder (langHolder);
		buildingRenderer.setAnim (anim);
		
		final UnitListCellRenderer unitRenderer = new UnitListCellRenderer ();
		unitRenderer.setGraphicsDB (gfx);
		unitRenderer.setLanguageHolder (langHolder);

		final UnitAttributeListCellRenderer attributeRenderer = new UnitAttributeListCellRenderer ();
		attributeRenderer.setLanguageHolder (langHolder);
		
		final UnitSkillListCellRenderer renderer = new UnitSkillListCellRenderer ();
		renderer.setLanguageHolder (langHolder);
		renderer.setGraphicsDB (gfx);
		renderer.setUtils (utils);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.panels/UnitInfoPanel.xml"));
		layout.buildMaps ();
		
		// Set up panel
		final CityCalculations cityCalc = mock (CityCalculations.class);

		final UnitInfoPanel panel = new UnitInfoPanel ();
		panel.setUnitInfoLayout (layout);
		panel.setUtils (utils);
		panel.setLanguageHolder (langHolder);
		panel.setLanguageChangeMaster (langMaster);
		panel.setClient (client);
		panel.setGraphicsDB (gfx);
		panel.setResourceValueClientUtils (resourceValueClientUtils);
		panel.setClientCityCalculations (clientCityCalc);
		panel.setUnitSkillListCellRenderer (renderer);
		panel.setUnitAttributeListCellRenderer (attributeRenderer);
		panel.setTextUtils (new TextUtilsImpl ());
		panel.setMediumFont (CreateFontsForTests.getMediumFont ());
		panel.setSmallFont (CreateFontsForTests.getSmallFont ());
		panel.setAnim (anim);
		
		// Set up form
		final ChangeConstructionUI changeConstruction = new ChangeConstructionUI ();
		changeConstruction.setUtils (utils);
		changeConstruction.setLanguageHolder (langHolder);
		changeConstruction.setLanguageChangeMaster (langMaster);
		changeConstruction.setClient (client);
		changeConstruction.setGraphicsDB (gfx);
		changeConstruction.setCityCalculations (cityCalc);
		changeConstruction.setClientCityCalculations (clientCityCalculations);
		changeConstruction.setUnitInfoPanel (panel);
		changeConstruction.setBuildingListCellRenderer (buildingRenderer);
		changeConstruction.setUnitListCellRenderer (unitRenderer);
		changeConstruction.setCityLocation (new MapCoordinates3DEx (20, 10, 0));
		changeConstruction.setMediumFont (CreateFontsForTests.getMediumFont ());
		changeConstruction.setSmallFont (CreateFontsForTests.getSmallFont ());
		changeConstruction.setAnim (anim);

		// Display form		
		changeConstruction.setVisible (true);
		Thread.sleep (5000);
		changeConstruction.setVisible (false);
	}
}