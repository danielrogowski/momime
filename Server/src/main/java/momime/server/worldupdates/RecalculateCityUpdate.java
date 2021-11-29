package momime.server.worldupdates;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.server.MomSessionVariables;

/**
 * World update for recalculationg all city stats such as size and number of rebels 
 */
public final class RecalculateCityUpdate implements WorldUpdate
{
	/** The city location to check */
	private MapCoordinates3DEx cityLocation;
	
	/**
	 * @return Enum indicating which kind of update this is
	 */
	@Override
	public final KindOfWorldUpdate getKindOfWorldUpdate ()
	{
		return KindOfWorldUpdate.RECALCULATE_CITY;
	}
	
	/**
	 * @param o Other object to compare against
	 * @return Whether this and the other object hold the same values
	 */
	@Override
	public final boolean equals (final Object o)
	{
		final boolean e;
		if (o instanceof RecalculateCityUpdate)
			e = (getCityLocation () == ((RecalculateCityUpdate) o).getCityLocation ());
		else
			e = false;
		
		return e;
	}
	
	/**
	 * Processes this update
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return True if this update generated any further updates (and hence the manager must resort the list)
	 * @throws IOException If there was a problem
	 * @throws JAXBException If there is a problem sending some message to the client
	 * @throws XMLStreamException If there is a problem sending some message to the client
	 */
	@Override
	public final boolean process (final MomSessionVariables mom) throws IOException, JAXBException, XMLStreamException
	{
		return false;
	}

	/**
	 * @return The city location to check
	 */
	public final MapCoordinates3DEx getCityLocation ()
	{
		return cityLocation;
	}

	/**
	 * @param l The city location to check
	 */
	public final void setCityLocation (final MapCoordinates3DEx l)
	{
		cityLocation = l;
	}
}