package momime.common.calculations;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import momime.common.database.CommonDatabaseConstants;
import momime.common.database.GenerateTestData;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.PlayerPickUtils;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.common.messages.v0_9_4.PlayerPick;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;

/**
 * Tests the calculations in the MomUnitCalculations class
 */
public final class TestMomUnitCalculations
{
	/**
	 * Tests the calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort method
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 */
	@Test
	public final void testCalculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort () throws RecordNotFoundException
	{
		// Set up object to test
		final MomUnitCalculations calc = new MomUnitCalculations ();
		calc.setPlayerPickUtils (new PlayerPickUtils ());
		
		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();

		// Picks
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();

		// Location
		final OverlandMapCoordinates cityLocation = new OverlandMapCoordinates ();
		cityLocation.setX (2);
		cityLocation.setY (2);
		cityLocation.setPlane (0);

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// Do basic calc where nothing gives mag weps
		assertEquals (0, calc.calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort (buildings, map, cityLocation, picks, sys, GenerateTestData.createDB ()));

		// Adamantium next to city, but we can't use it without an alchemists' guild
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setMapFeatureID (GenerateTestData.ADAMANTIUM_ORE);
		map.getPlane ().get (0).getRow ().get (2).getCell ().get (3).setTerrainData (terrainData);

		assertEquals (0, calc.calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort (buildings, map, cityLocation, picks, sys, GenerateTestData.createDB ()));

		// Alchemy retort grants us grade 1, but still can't use that adamantium
		final PlayerPick alchemy = new PlayerPick ();
		alchemy.setPickID (CommonDatabaseConstants.VALUE_RETORT_ID_ALCHEMY);
		alchemy.setQuantity (1);
		picks.add (alchemy);

		assertEquals (1, calc.calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort (buildings, map, cityLocation, picks, sys, GenerateTestData.createDB ()));

		// Add the wrong type of building, to prove that it doesn't help
		final OverlandMapCoordinates sagesGuildLocation = new OverlandMapCoordinates ();
		sagesGuildLocation.setX (2);
		sagesGuildLocation.setY (2);
		sagesGuildLocation.setPlane (0);

		final MemoryBuilding sagesGuild = new MemoryBuilding ();
		sagesGuild.setCityLocation (sagesGuildLocation);
		sagesGuild.setBuildingID (GenerateTestData.SAGES_GUILD);
		buildings.add (sagesGuild);

		assertEquals (1, calc.calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort (buildings, map, cityLocation, picks, sys, GenerateTestData.createDB ()));

		// Add an alchemists' guild, in the wrong place
		final OverlandMapCoordinates alchemistsGuildLocation = new OverlandMapCoordinates ();
		alchemistsGuildLocation.setX (2);
		alchemistsGuildLocation.setY (2);
		alchemistsGuildLocation.setPlane (1);

		final MemoryBuilding alchemistsGuild = new MemoryBuilding ();
		alchemistsGuild.setCityLocation (alchemistsGuildLocation);
		alchemistsGuild.setBuildingID (GenerateTestData.ALCHEMISTS_GUILD);
		buildings.add (alchemistsGuild);

		assertEquals (1, calc.calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort (buildings, map, cityLocation, picks, sys, GenerateTestData.createDB ()));

		// Move it to the right place
		alchemistsGuildLocation.setPlane (0);
		assertEquals (3, calc.calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort (buildings, map, cityLocation, picks, sys, GenerateTestData.createDB ()));
	}
}
