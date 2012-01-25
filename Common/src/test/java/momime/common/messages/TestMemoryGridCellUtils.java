package momime.common.messages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import momime.common.database.CommonDatabaseConstants;

import org.junit.Test;

/**
 * Tests the MemoryGridCellUtils class
 */
public final class TestMemoryGridCellUtils
{
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
}
