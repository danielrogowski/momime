package momime.server.mapgenerator;

import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.CombatMapSize;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapAreaOfCombatTiles;


/**
 * Server only class which contains all the code for generating a random combat map
 * 
 * NB. This is a singleton generator class - each method call generates and returns a new combat map
 * This is in contrast to the OverlandMapGenerator which has one generator created per session and is tied to the overland map being created
 */
public interface CombatMapGenerator
{
	/**
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @param db Server database XML
	 * @param trueTerrain Details of the overland map, buildings and so on
	 * @param combatMapLocation The location that the map is being generated for (we need this in order to look for buildings, etc)
	 * @return Newly generated combat map
	 * @throws RecordNotFoundException If one of the elements that meets the conditions specifies a combatTileTypeID that doesn't exist in the database
	 */
	public MapAreaOfCombatTiles generateCombatMap (final CombatMapSize combatMapCoordinateSystem,
		final CommonDatabase db, final FogOfWarMemory trueTerrain, final MapCoordinates3DEx combatMapLocation)
		throws RecordNotFoundException;
	
	
	/**
	 * Many elements of a combat map are random, e.g. placement of ridges, dark areas, rocks and trees.  So when a city enchantment
	 * (Wall of Fire/Darkness) is cast during a combat, we want to regenerate only the tile borders to show the new enchantment
	 * without regenerating the random elements of the combat map.
	 *  
	 * @param map Map to renegerate the tile borders of
	 * @param db Server database XML
	 * @param trueTerrain Details of the overland map, buildings and so on
	 * @param combatMapLocation The location that the map is being regenerated for (we need this in order to look for buildings, etc)
	 * @throws RecordNotFoundException If one of the elements that meets the conditions specifies a combatTileTypeID that doesn't exist in the database
	 */
	public void regenerateCombatTileBorders (final MapAreaOfCombatTiles map, final CommonDatabase db, final FogOfWarMemory trueTerrain, final MapCoordinates3DEx combatMapLocation)
		throws RecordNotFoundException;
}