package momime.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import momime.common.database.CommonDatabaseConstants;
import momime.common.database.GenerateTestData;
import momime.common.messages.MapAreaOfMemoryGridCells;
import momime.common.messages.MapRowOfMemoryGridCells;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.OverlandMapTerrainData;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;

/**
 * Tests the MemoryGridCellUtils class
 */
public final class TestMemoryGridCellUtilsImpl
{
	/**
	 * Tests the convertNullTileTypeToFOW method with a non-null value, ignoring roads
	 */
	@Test
	public final void testConvertNullTileTypeToFOW_NonNull_IgnoreRoad ()
	{
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("A");
		terrainData.setRoadTileTypeID ("B");

		final MemoryGridCellUtilsImpl utils = new MemoryGridCellUtilsImpl ();
		assertEquals ("A", utils.convertNullTileTypeToFOW (terrainData, false));
	}

	/**
	 * Tests the convertNullTileTypeToFOW method with a non-null value, and no road
	 */
	@Test
	public final void testConvertNullTileTypeToFOW_NonNull_NoRoad ()
	{
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("A");

		final MemoryGridCellUtilsImpl utils = new MemoryGridCellUtilsImpl ();
		assertEquals ("A", utils.convertNullTileTypeToFOW (terrainData, true));
	}

	/**
	 * Tests the convertNullTileTypeToFOW method with a non-null value, with road
	 */
	@Test
	public final void testConvertNullTileTypeToFOW_NonNull_Road ()
	{
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("A");
		terrainData.setRoadTileTypeID ("B");

		final MemoryGridCellUtilsImpl utils = new MemoryGridCellUtilsImpl ();
		assertEquals ("B", utils.convertNullTileTypeToFOW (terrainData, true));
	}

	
	/**
	 * Tests the convertNullTileTypeToFOW method with a null tile type
	 */
	@Test
	public final void testConvertNullTileTypeToFOW_NullTileTypeID ()
	{
		final MemoryGridCellUtilsImpl utils = new MemoryGridCellUtilsImpl ();
		assertEquals (CommonDatabaseConstants.TILE_TYPE_FOG_OF_WAR, utils.convertNullTileTypeToFOW (new OverlandMapTerrainData (), false));
	}

	/**
	 * Tests the convertNullTileTypeToFOW method with the whole terrain data structure null
	 */
	@Test
	public final void testConvertNullTileTypeToFOW_NullTerrainData ()
	{
		final MemoryGridCellUtilsImpl utils = new MemoryGridCellUtilsImpl ();
		assertEquals (CommonDatabaseConstants.TILE_TYPE_FOG_OF_WAR, utils.convertNullTileTypeToFOW (null, false));
	}

	/**
	 * Tests the isTerrainTowerOfWizardry method on an uncleared tower
	 */
	@Test
	public final void testIsTerrainTowerOfWizardry_Uncleared ()
	{
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setMapFeatureID (CommonDatabaseConstants.FEATURE_UNCLEARED_TOWER_OF_WIZARDRY);

		final MemoryGridCellUtilsImpl utils = new MemoryGridCellUtilsImpl ();
		assertTrue (utils.isTerrainTowerOfWizardry (terrainData));
	}

	/**
	 * Tests the isTerrainTowerOfWizardry method on a cleared tower
	 */
	@Test
	public final void testIsTerrainTowerOfWizardry_Cleared ()
	{
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setMapFeatureID (CommonDatabaseConstants.FEATURE_CLEARED_TOWER_OF_WIZARDRY);

		final MemoryGridCellUtilsImpl utils = new MemoryGridCellUtilsImpl ();
		assertTrue (utils.isTerrainTowerOfWizardry (terrainData));
	}

	/**
	 * Tests the isTerrainTowerOfWizardry method on some other map feature
	 */
	@Test
	public final void testIsTerrainTowerOfWizardry_OtherValue ()
	{
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setMapFeatureID ("A");

		final MemoryGridCellUtilsImpl utils = new MemoryGridCellUtilsImpl ();
		assertFalse (utils.isTerrainTowerOfWizardry (terrainData));
	}

	/**
	 * Tests the isTerrainTowerOfWizardry method on a null map feature (we can see that there is no feature there)
	 */
	@Test
	public final void testIsTerrainTowerOfWizardry_NullMapFeature ()
	{
		final MemoryGridCellUtilsImpl utils = new MemoryGridCellUtilsImpl ();
		assertFalse (utils.isTerrainTowerOfWizardry (new OverlandMapTerrainData ()));
	}

	/**
	 * Tests the isTerrainTowerOfWizardry method on a null map feature (we can't see there)
	 */
	@Test
	public final void testIsTerrainTowerOfWizardry_NullTerrainData ()
	{
		final MemoryGridCellUtilsImpl utils = new MemoryGridCellUtilsImpl ();
		assertFalse (utils.isTerrainTowerOfWizardry (null));
	}

	/**
	 * Tests the blankBuildingsSoldThisTurn method
	 */
	@Test
	public final void testBlankBuildingsSoldThisTurn ()
	{
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);

		// Set bunch of cells to values
		for (int plane = 0; plane < 2; plane++)
			for (int n = 0; n < 20; n++)
				map.getPlane ().get (plane).getRow ().get (n).getCell ().get (n * 2).setBuildingIdSoldThisTurn ("BL" + n);

		// Run method
		final MemoryGridCellUtilsImpl utils = new MemoryGridCellUtilsImpl ();
		utils.blankBuildingsSoldThisTurn (map);

		// Ensure they all get blanked
		for (final MapAreaOfMemoryGridCells plane : map.getPlane ())
			for (final MapRowOfMemoryGridCells row : plane.getRow ())
				for (final MemoryGridCell cell : row.getCell ())
					assertNull (cell.getBuildingIdSoldThisTurn ());
	}
}