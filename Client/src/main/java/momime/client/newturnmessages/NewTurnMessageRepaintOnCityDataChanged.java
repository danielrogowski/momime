package momime.client.newturnmessages;

import com.ndg.map.MapCoordinates3D;

/**
 * NTMs should implement this if they display any info about a particular city that needs to be redrawn if that city data changes
 */
public interface NewTurnMessageRepaintOnCityDataChanged
{
	/**
	 * @return The location of the city that this NTM is displaying info about
	 */
	public MapCoordinates3D getCityLocation ();
}