package momime.server.worldupdates;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.server.MomSessionVariables;

/**
 * World update for rechecking that units in an overland map cell are a valid stack for the type of terrain, e.g.. that there are enough boats to carry everyone that can't swim 
 */
public final class RecheckTransportCapacityUpdate implements WorldUpdate
{
	/** The map location to check */
	private MapCoordinates3DEx mapLocation;
	
	/**
	 * @return Enum indicating which kind of update this is
	 */
	@Override
	public final KindOfWorldUpdate getKindOfWorldUpdate ()
	{
		return KindOfWorldUpdate.RECHECK_TRANSPORT_CAPACITY;
	}
	
	/**
	 * @param o Other object to compare against
	 * @return Whether this and the other object hold the same values
	 */
	@Override
	public final boolean equals (final Object o)
	{
		final boolean e;
		if (o instanceof RecheckTransportCapacityUpdate)
			e = (getMapLocation () == ((RecheckTransportCapacityUpdate) o).getMapLocation ());
		else
			e = false;
		
		return e;
	}
	
	/**
	 * @return String representation of class, for debug messages
	 */
	@Override
	public final String toString ()
	{
		return "Recheck transport capacity at " + getMapLocation ();
	}
	
	/**
	 * Processes this update
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether this update was processed and/or generated any further updates
	 * @throws IOException If there was a problem
	 * @throws JAXBException If there is a problem sending some message to the client
	 * @throws XMLStreamException If there is a problem sending some message to the client
	 */
	@Override
	public final WorldUpdateResult process (final MomSessionVariables mom) throws IOException, JAXBException, XMLStreamException
	{
		return WorldUpdateResult.DONE;
	}

	/**
	 * @return The map location to check
	 */
	public final MapCoordinates3DEx getMapLocation ()
	{
		return mapLocation;
	}

	/**
	 * @param l The map location to check
	 */
	public final void setMapLocation (final MapCoordinates3DEx l)
	{
		mapLocation = l;
	}
}