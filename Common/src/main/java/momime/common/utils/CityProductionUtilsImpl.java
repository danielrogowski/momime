package momime.common.utils;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.ProductionAmountBucketID;
import momime.common.database.RecordNotFoundException;
import momime.common.internal.CityProductionBreakdown;

/**
 * Helper classes used by city production calculations
 */
public final class CityProductionUtilsImpl implements CityProductionUtils
{
	/**
	 * Adds a concrete (not percentage) production value (not consumption) to the breakdown for a specified productionTypeID.
	 * 
	 * @param breakdown Breakdown to add value to.  productionTypeID must already be set in it.
	 * @param doubleProductionAmount Amount to add, doubled
	 * @param overrideBucket Usually null, and which bucket to add the value to is read from the production type; but can be set as an override
	 * @param db Lookup lists built over the XML database
	 * @return Which bucket the value was added to
	 * @throws RecordNotFoundException If the productionTypeID can't be found in the database
	 * @throws MomException If we try to add an odd amount to the "after" bucket
	 */
	@Override
	public final ProductionAmountBucketID addProductionAmountToBreakdown (final CityProductionBreakdown breakdown, final int doubleProductionAmount,
		final ProductionAmountBucketID overrideBucket, final CommonDatabase db)
		throws RecordNotFoundException, MomException
	{
		final ProductionAmountBucketID bucket;
		if (overrideBucket != null)
			bucket = overrideBucket;
		else
			bucket = db.findProductionType (breakdown.getProductionTypeID (), "addProductionAmountToBreakdown").getProductionAmountBucketID ();
		
		switch (bucket)
		{
			case BEFORE_PERCENTAGE_BONUSES:
				breakdown.setDoubleProductionAmountBeforePercentages (breakdown.getDoubleProductionAmountBeforePercentages () + doubleProductionAmount);
				break;
				
			case AFTER_PERCENTAGE_BONUSES:
				if (doubleProductionAmount % 2 != 0)
					throw new MomException ("Tried to add doubled production amount " + doubleProductionAmount + " to \"after\" bucket of " + breakdown.getProductionTypeID () +
						", but \"after\" values must be even values so they can be halved");
				
				breakdown.setProductionAmountToAddAfterPercentages (breakdown.getProductionAmountToAddAfterPercentages () + (doubleProductionAmount / 2));
				break;
				
			default:
				throw new MomException ("addProductionAmountToBreakdown doesn't know where to put values for bucket \"" + bucket + "\"");
		}
		
		return bucket;
	}
}