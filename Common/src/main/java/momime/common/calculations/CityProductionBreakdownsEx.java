package momime.common.calculations;

import java.util.Iterator;

import momime.common.internal.CityProductionBreakdown;
import momime.common.internal.CityProductionBreakdowns;

/**
 * Adds a find method the list of city productions
 */
public final class CityProductionBreakdownsEx extends CityProductionBreakdowns
{
	/**
	 * @param productionTypeID Production type to search for
	 * @return Requested production type, or null if the city does not produce or consume any of this type of production
	 */
	public final CityProductionBreakdown findProductionType (final String productionTypeID)
	{
		CityProductionBreakdown result = null;
		final Iterator<CityProductionBreakdown> productions = getProductionType ().iterator ();

		while ((result == null) && (productions.hasNext ()))
		{
			final CityProductionBreakdown thisProduction = productions.next ();
			if (thisProduction.getProductionTypeID ().equals (productionTypeID))
				result = thisProduction;
		}

		return result;
	}

	/**
	 * @param productionTypeID Production type to search for
	 * @return Requested production type, or a newly created one if it wasn't previously in the list
	 */
	final CityProductionBreakdown findOrAddProductionType (final String productionTypeID)
	{
		CityProductionBreakdown result = findProductionType (productionTypeID);
		if (result == null)
		{
			result = new CityProductionBreakdown ();
			result.setProductionTypeID (productionTypeID);
			getProductionType ().add (result);
		}

		return result;
	}
}