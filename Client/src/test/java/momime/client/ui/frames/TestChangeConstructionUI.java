package momime.client.ui.frames;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.calculations.MomClientCityCalculations;
import momime.client.database.ClientDatabaseEx;
import momime.client.graphics.database.AnimationEx;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.ProductionTypeEx;
import momime.client.graphics.database.v0_9_5.AnimationFrame;
import momime.client.graphics.database.v0_9_5.CityViewElement;
import momime.client.graphics.database.v0_9_5.ProductionTypeImage;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.ui.renderer.BuildingListCellRenderer;
import momime.client.ui.renderer.CellRendererFactory;
import momime.client.utils.AnimationControllerImpl;
import momime.client.utils.ResourceValueClientUtilsImpl;
import momime.client.utils.TextUtilsImpl;
import momime.common.calculations.MomCityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.newgame.v0_9_5.MapSizeData;
import momime.common.database.v0_9_5.Building;
import momime.common.database.v0_9_5.BuildingPopulationProductionModifier;
import momime.common.database.v0_9_5.Race;
import momime.common.database.v0_9_5.RaceCannotBuild;
import momime.common.database.v0_9_5.Unit;
import momime.common.messages.v0_9_5.FogOfWarMemory;
import momime.common.messages.v0_9_5.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_5.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_5.MomSessionDescription;
import momime.common.messages.v0_9_5.OverlandMapCityData;
import momime.common.utils.MemoryBuildingUtils;

import org.junit.Test;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;

/**
 * Tests the ChangeConstructionUI class
 */
public final class TestChangeConstructionUI
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

		final momime.client.language.database.v0_9_5.Building granaryName = new momime.client.language.database.v0_9_5.Building ();
		granaryName.setBuildingName ("Granary");
		when (lang.findBuilding ("BL04")).thenReturn (granaryName);
		
		final momime.client.language.database.v0_9_5.Building fightersGuildName = new momime.client.language.database.v0_9_5.Building ();
		fightersGuildName.setBuildingName ("Fighters' Guild");
		when (lang.findBuilding ("BL05")).thenReturn (fightersGuildName);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Client city calculations derive language strings
		final MomClientCityCalculations clientCityCalc = mock (MomClientCityCalculations.class);
		when (clientCityCalc.describeWhatBuildingAllows ("BL04", new MapCoordinates3DEx (20, 10, 0))).thenReturn ("This is what the Granary allows");
		when (clientCityCalc.describeWhatBuildingAllows ("BL05", new MapCoordinates3DEx (20, 10, 0))).thenReturn ("This is what the Fighters' Guild allows");
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Mock entries from the graphics XML
		final CityViewElement granary = new CityViewElement ();
		granary.setCityViewImageFile ("/momime.client.graphics/cityView/buildings/BL29.png");
		
		final AnimationEx fightersGuildAnim = new AnimationEx ();
		fightersGuildAnim.setAnimationSpeed (4);
		for (int n = 1; n <= 9; n++)
		{
			final AnimationFrame frame = new AnimationFrame ();
			frame.setFrameImageFile ("/momime.client.graphics/cityView/buildings/BL05-frame" + n + ".png");
			fightersGuildAnim.getFrame ().add (frame);
		}
		
		final CityViewElement fightersGuild = new CityViewElement ();
		fightersGuild.setCityViewAnimation ("FIGHTERS_GUILD");
		
		final ProductionTypeImage plusOneImageContainer = new ProductionTypeImage ();
		plusOneImageContainer.setProductionImageFile ("/momime.client.graphics/production/gold/1.png");
		plusOneImageContainer.setProductionValue ("1");
		
		final ProductionTypeEx productionTypeImages = new ProductionTypeEx ();
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

			if (n == 1)
				unit.setUnitMagicRealm (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
			else
				unit.setUnitMagicRealm (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
			
			if (n == 2)
				unit.setUnitRaceID ("RC01");
			else if (n == 3)
				unit.setUnitRaceID ("RC02");
			
			units.add (unit);
		}
		
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		doReturn (buildings).when (db).getBuilding ();
		doReturn (units).when (db).getUnit ();
		when (db.findRace ("RC01", "ChangeConstructionUI.init")).thenReturn (race);
		
		// City data
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		cityData.setCitySizeID ("CS01");
		cityData.setCityName ("Blahdy Blah");

		final MapSizeData mapSize = ClientTestData.createMapSizeData ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setMapSize (mapSize);
		
		final MapVolumeOfMemoryGridCells terrain = ClientTestData.createOverlandMap (mapSize);
		terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		final FogOfWarMemory fow = new FogOfWarMemory ();
		fow.setMap (terrain);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (fow);
		
		final MomClient client = mock (MomClient.class);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getSessionDescription ()).thenReturn (sd);
		
		// Buildings
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		for (int n = 0; n < buildings.size () - 1; n++)
			when (memoryBuildingUtils.meetsBuildingRequirements (fow.getBuilding (), new MapCoordinates3DEx (20, 10, 0), buildings.get (n))).thenReturn (true);
		
		when (memoryBuildingUtils.findBuilding (fow.getBuilding (), new MapCoordinates3DEx (20, 10, 0), "BL03")).thenReturn (true);
		
		for (int n = 0; n < units.size () - 1; n++)
			when (memoryBuildingUtils.meetsUnitRequirements (fow.getBuilding (), new MapCoordinates3DEx (20, 10, 0), units.get (n))).thenReturn (true);
		
		// Tile type reqs
		final MomCityCalculations cityCalc = mock (MomCityCalculations.class);
		for (int n = 1; n < buildings.size (); n++)
			when (cityCalc.buildingPassesTileTypeRequirements (fow.getMap (), new MapCoordinates3DEx (20, 10, 0), buildings.get (n), mapSize)).thenReturn (true);
		
		// Current construction
		cityData.setCurrentlyConstructingBuildingOrUnitID ("BL05");
		when (db.findBuilding ("BL05", "ChangeConstructionUI.init")).thenReturn (buildings.get (4));
		
		// Set up production image generator
		final ResourceValueClientUtilsImpl resourceValueClientUtils = new ResourceValueClientUtilsImpl ();
		resourceValueClientUtils.setGraphicsDB (gfx);
		resourceValueClientUtils.setUtils (utils);
		
		// Animation controller
		final AnimationControllerImpl anim = new AnimationControllerImpl ();
		anim.setGraphicsDB (gfx);
		anim.setUtils (utils);

		// Cell renderer
		final BuildingListCellRenderer renderer = new BuildingListCellRenderer ();
		renderer.setGraphicsDB (gfx);
		renderer.setLanguageHolder (langHolder);
		renderer.setAnim (anim);
		
		final CellRendererFactory cellRendererFactory = mock (CellRendererFactory.class);
		when (cellRendererFactory.createBuildingListCellRenderer ()).thenReturn (renderer);
		
		// Set up form
		final ChangeConstructionUI changeConstruction = new ChangeConstructionUI ();
		changeConstruction.setUtils (utils);
		changeConstruction.setLanguageHolder (langHolder);
		changeConstruction.setLanguageChangeMaster (langMaster);
		changeConstruction.setClient (client);
		changeConstruction.setGraphicsDB (gfx);
		changeConstruction.setCellRendererFactory (cellRendererFactory);
		changeConstruction.setMemoryBuildingUtils (memoryBuildingUtils);
		changeConstruction.setCityCalculations (cityCalc);
		changeConstruction.setClientCityCalculations (clientCityCalc);
		changeConstruction.setCityLocation (new MapCoordinates3DEx (20, 10, 0));
		changeConstruction.setCityViewUI (new CityViewUI ());
		changeConstruction.setTextUtils (new TextUtilsImpl ());
		changeConstruction.setResourceValueClientUtils (resourceValueClientUtils);
		changeConstruction.setMediumFont (CreateFontsForTests.getMediumFont ());
		changeConstruction.setSmallFont (CreateFontsForTests.getSmallFont ());
		changeConstruction.setAnim (anim);

		// Display form		
		changeConstruction.setVisible (true);
		Thread.sleep (5000);
	}
}