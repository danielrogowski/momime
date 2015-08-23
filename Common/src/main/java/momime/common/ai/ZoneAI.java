package momime.common.ai;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.areas.storage.MapArea3D;

import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.FogOfWarMemory;

/**
 * Calculates zones / national borders from what a player knows about the overland map.
 * This is principally used for the AI on the server, but is in the common project so that the client can
 * have a tickbox on the options screen to display their own border - which is mostly for purposes
 * of testing that this works as intended.
 */
public interface ZoneAI
{
	/**
	 * @param fogOfWarMemory Known overland terrain, units, buildings and so on
	 * @param overlandMapCoordinateSystem Coordinate system for traversing overland map
	 * @param playerID Player whose border we want to calculate
	 * @param separation How close our cities have to be to consider the area between them to be probably passable without getting attacked
	 * @param db Lookup lists built over the XML database
	 * @return 3D area marked with all the locations we consider to be our territory
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the db
	 */
	public MapArea3D<Boolean> calculateFriendlyZone (final FogOfWarMemory fogOfWarMemory,
		final CoordinateSystem overlandMapCoordinateSystem, final int playerID, final int separation, final CommonDatabase db)
		throws RecordNotFoundException;
}