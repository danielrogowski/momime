package momime.common.messages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.logging.Logger;

import momime.common.database.CommonDatabaseConstants;
import momime.common.database.GenerateTestData;
import momime.common.messages.v0_9_4.MapAreaOfMemoryGridCells;
import momime.common.messages.v0_9_4.MapRowOfMemoryGridCells;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryGridCell;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;

/**
 * Tests the MemoryGridCellUtils class
 */
public final class TestMemoryGridCellUtils
{
	/** Dummy logger to use during unit tests */
	private final Logger debugLogger = Logger.getLogger ("MoMIMECommonUnitTests");

	/**
	 * Tests the convertNullTileTypeToFOW method with a non-null value
	 */
	@Test
	public final void testConvertNullTileTypeToFOW_NonNull ()
	{
		assertEquals ("A", MemoryGridCellUtils.convertNullTileTypeToFOW ("A"));
	}

	/**
	 * Tests the convertNullTileTypeToFOW method with a null value
	 */
	@Test
	public final void testConvertNullTileTypeToFOW_Null ()
	{
		assertEquals (CommonDatabaseConstants.VALUE_TILE_TYPE_FOG_OF_WAR, MemoryGridCellUtils.convertNullTileTypeToFOW (null));
	}

	/**
	 * Tests the isFeatureTowerOfWizardry method on an uncleared tower
	 */
	@Test
	public final void testIsFeatureTowerOfWizardry_Uncleared ()
	{
		assertTrue (MemoryGridCellUtils.isFeatureTowerOfWizardry (CommonDatabaseConstants.VALUE_FEATURE_UNCLEARED_TOWER_OF_WIZARDRY));
	}

	/**
	 * Tests the isFeatureTowerOfWizardry method on a cleared tower
	 */
	@Test
	public final void testIsFeatureTowerOfWizardry_Cleared ()
	{
		assertTrue (MemoryGridCellUtils.isFeatureTowerOfWizardry (CommonDatabaseConstants.VALUE_FEATURE_CLEARED_TOWER_OF_WIZARDRY));
	}

	/**
	 * Tests the isFeatureTowerOfWizardry method on some other map feature
	 */
	@Test
	public final void testIsFeatureTowerOfWizardry_OtherValue ()
	{
		assertFalse (MemoryGridCellUtils.isFeatureTowerOfWizardry ("A"));
	}

	/**
	 * Tests the isFeatureTowerOfWizardry method on a null map feature (we can't see there, or we can see that there is no feature there)
	 */
	@Test
	public final void testIsFeatureTowerOfWizardry_Null ()
	{
		assertFalse (MemoryGridCellUtils.isFeatureTowerOfWizardry (null));
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
		MemoryGridCellUtils.blankBuildingsSoldThisTurn (map, debugLogger);

		// Ensure they all get blanked
		for (final MapAreaOfMemoryGridCells plane : map.getPlane ())
			for (final MapRowOfMemoryGridCells row : plane.getRow ())
				for (final MemoryGridCell cell : row.getCell ())
					assertNull (cell.getBuildingIdSoldThisTurn ());
	}
}
