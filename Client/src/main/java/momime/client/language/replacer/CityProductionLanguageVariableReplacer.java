package momime.client.language.replacer;

import momime.common.internal.CityProductionBreakdown;
import momime.common.internal.CityProductionBreakdownBuilding;
import momime.common.internal.CityProductionBreakdownEvent;
import momime.common.internal.CityProductionBreakdownMapFeature;
import momime.common.internal.CityProductionBreakdownPickType;
import momime.common.internal.CityProductionBreakdownPlane;
import momime.common.internal.CityProductionBreakdownPopulationTask;
import momime.common.internal.CityProductionBreakdownSpell;
import momime.common.internal.CityProductionBreakdownTileType;

/**
 * Language replacer for city production variables
 */
public interface CityProductionLanguageVariableReplacer extends BreakdownLanguageVariableReplacer<CityProductionBreakdown>
{
	/**
	 * @param pop Population task specific breakdown
	 */
	public void setCurrentPopulationTask (final CityProductionBreakdownPopulationTask pop);

	/**
	 * @param tile Tile type specific breakdown
	 */
	public void setCurrentTileType (final CityProductionBreakdownTileType tile);

	/**
	 * @param feature Map feature specific breakdown
	 */
	public void setCurrentMapFeature (final CityProductionBreakdownMapFeature feature);

	/**
	 * @param building Building specific breakdown
	 */
	public void setCurrentBuilding (final CityProductionBreakdownBuilding building);

	/**
	 * @param event Event specific breakdown
	 */
	public void setCurrentEvent (final CityProductionBreakdownEvent event);
	
	/**
	 * @param spell Spell specific breakdown
	 */
	public void setCurrentSpell (final CityProductionBreakdownSpell spell);
	
	/**
	 * @param pickType Pick type specific breakdown
	 */
	public void setCurrentPickType (final CityProductionBreakdownPickType pickType);
	
	/**
	 * @param plane Plane specific breakdown
	 */
	public void setCurrentPlane (final CityProductionBreakdownPlane plane);
}