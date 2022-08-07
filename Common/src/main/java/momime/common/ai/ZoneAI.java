package momime.common.ai;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.areas.storage.MapArea3D;

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
	 * @return 3D area marked with the player ID we consider as owning each tile
	 * @throws RecordNotFoundException If a wizard who owns a city can't be found
	 */
	public MapArea3D<Integer> calculateZones (final FogOfWarMemory fogOfWarMemory, final CoordinateSystem overlandMapCoordinateSystem)
		throws RecordNotFoundException;
}