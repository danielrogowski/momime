package momime.server.mapgenerator;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_5.FogOfWarMemory;
import momime.common.messages.v0_9_5.MapAreaOfCombatTiles;
import momime.server.database.ServerDatabaseEx;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;


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
	public MapAreaOfCombatTiles generateCombatMap (final CoordinateSystem combatMapCoordinateSystem,
		final ServerDatabaseEx db, final FogOfWarMemory trueTerrain, final MapCoordinates3DEx combatMapLocation)
		throws RecordNotFoundException;
}