package momime.client.ui;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.database.ClientDatabaseEx;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.v0_9_5.CityViewElement;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.common.calculations.MomCityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.newgame.v0_9_5.MapSizeData;
import momime.common.database.v0_9_5.Building;
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

		final momime.client.language.database.v0_9_5.Building granaryName = new momime.client.language.database.v0_9_5.Building ();
		granaryName.setBuildingName ("Granary");
		when (lang.findBuilding ("BL04")).thenReturn (granaryName);
		
		final momime.client.language.database.v0_9_5.Building fightersGuildName = new momime.client.language.database.v0_9_5.Building ();
		fightersGuildName.setBuildingName ("Fighters' Guild");
		when (lang.findBuilding ("BL05")).thenReturn (fightersGuildName);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Mock entries from the graphics XML
		final CityViewElement granary = new CityViewElement ();
		granary.setCityViewImageFile ("/momime.client.graphics/cityView/buildings/BL29.png");
		
		final CityViewElement fightersGuild = new CityViewElement ();
		fightersGuild.setCityViewImageFile ("/momime.client.graphics/cityView/buildings/BL05-frame1.png");
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.findBuilding ("BL04", "BuildingListCellRenderer")).thenReturn (granary);
		when (gfx.findBuilding ("BL05", "BuildingListCellRenderer")).thenReturn (fightersGuild);
		
		// Client DB
		final List<Building> buildings = new ArrayList<Building> ();
		// for (int m = 1; m <= 6; m++)
		for (int n = 1; n <= 6; n++)
		{
			final Building building = new Building ();
			building.setBuildingID ("BL0" + n);
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
		
		// Set up form
		final ChangeConstructionUI changeConstruction = new ChangeConstructionUI ();
		changeConstruction.setUtils (utils);
		changeConstruction.setLanguageHolder (langHolder);
		changeConstruction.setLanguageChangeMaster (langMaster);
		changeConstruction.setClient (client);
		changeConstruction.setGraphicsDB (gfx);
		changeConstruction.setMemoryBuildingUtils (memoryBuildingUtils);
		changeConstruction.setCityCalculations (cityCalc);
		changeConstruction.setCityLocation (new MapCoordinates3DEx (20, 10, 0));
		changeConstruction.setCityViewUI (new CityViewUI ());
		changeConstruction.setMediumFont (CreateFontsForTests.getMediumFont ());

		// Display form		
		changeConstruction.setVisible (true);
		Thread.sleep (50000);
	}
}