package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.ui.frames.ChangeConstructionUI;
import momime.client.ui.frames.OverlandMapUI;
import momime.common.messages.servertoclient.v0_9_5.AddBuildingMessage;
import momime.common.messages.v0_9_5.MemoryBuilding;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.client.MultiplayerServerConnection;
import com.ndg.multiplayer.client.SessionServerToClientMessage;

/**
 * Server sends this to clients to tell them about a building added to a city
 */
public final class AddBuildingMessageImpl extends AddBuildingMessage implements SessionServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (AddBuildingMessageImpl.class);

	/** Multiplayer client */
	private MomClient client;
	
	/** Overland map UI */
	private OverlandMapUI overlandMapUI;
	
	/**
	 * Method called when this message is sent in isolation
	 * 
	 * @param sender Connection to the server
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void process (final MultiplayerServerConnection sender)
		throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering process: " + getData ().getCityLocation () + ", " + getData ().getFirstBuildingID () + ", " + getData ().getSecondBuildingID ());
		
		processOneUpdate ();
		
		// Building may have been city walls and so affect the overland map view
		getOverlandMapUI ().regenerateOverlandMapBitmaps ();
		
		log.trace ("Exiting process");
	}

	/**
	 * Method called for each individual update; so called once if message was sent in isolation, or multiple times if part of FogOfWarVisibleAreaChangedMessage
	 */
	public final void processOneUpdate ()
	{
		log.trace ("Entering processOneUpdate: " + getData ().getCityLocation () + ", " + getData ().getFirstBuildingID () + ", " + getData ().getSecondBuildingID ());
		
		// Add building(s)
		final MemoryBuilding firstBuilding = new MemoryBuilding ();
		firstBuilding.setBuildingID (getData ().getFirstBuildingID ());
		firstBuilding.setCityLocation (new MapCoordinates3DEx ((MapCoordinates3DEx) getData ().getCityLocation ()));
		
		getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding ().add (firstBuilding);
		
		if (getData ().getSecondBuildingID () != null)
		{
			final MemoryBuilding secondBuilding = new MemoryBuilding ();
			secondBuilding.setBuildingID (getData ().getSecondBuildingID ());
			secondBuilding.setCityLocation (new MapCoordinates3DEx ((MapCoordinates3DEx) getData ().getCityLocation ()));
			
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding ().add (secondBuilding);
		}
		
		// Addition of a building will alter what we can construct in that city, if we've got the change construction screen open
		final ChangeConstructionUI changeConstruction = getClient ().getChangeConstructions ().get (getData ().getCityLocation ().toString ());
		if (changeConstruction != null)
			try
			{
				changeConstruction.updateWhatCanBeConstructed ();
			}
			catch (final Exception e)
			{
				log.error (e, e);
			}		
		
		log.trace ("Exiting processOneUpdate");
	}
	
	/**
	 * @return Multiplayer client
	 */
	public final MomClient getClient ()
	{
		return client;
	}
	
	/**
	 * @param obj Multiplayer client
	 */
	public final void setClient (final MomClient obj)
	{
		client = obj;
	}

	/**
	 * @return Overland map UI
	 */
	public final OverlandMapUI getOverlandMapUI ()
	{
		return overlandMapUI;
	}

	/**
	 * @param ui Overland map UI
	 */
	public final void setOverlandMapUI (final OverlandMapUI ui)
	{
		overlandMapUI = ui;
	}
}