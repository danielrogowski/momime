package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.ui.frames.ChangeConstructionUI;
import momime.client.ui.frames.OverlandMapUI;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.servertoclient.DestroyBuildingMessage;
import momime.common.utils.MemoryBuildingUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

/**
 * Server sends this to clients to tell them about a building destroyed (or sold) from a city
 */
public final class DestroyBuildingMessageImpl extends DestroyBuildingMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (DestroyBuildingMessageImpl.class);

	/** Multiplayer client */
	private MomClient client;
	
	/** Overland map UI */
	private OverlandMapUI overlandMapUI;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start: " + getData ().getCityLocation () + ", " + getData ().getBuildingID () + ", " + getData ().isUpdateBuildingSoldThisTurn ());
		
		processOneUpdate ();
		
		// Building may have been city walls and so affect the overland map view
		getOverlandMapUI ().regenerateOverlandMapBitmaps ();
		
		log.trace ("Exiting start");
	}

	/**
	 * Method called for each individual update; so called once if message was sent in isolation, or multiple times if part of FogOfWarVisibleAreaChangedMessage
	 * @throws RecordNotFoundException If we can't find the building that the server is telling us to remove
	 */
	public final void processOneUpdate () throws RecordNotFoundException
	{
		log.trace ("Entering processOneUpdate: " + getData ().getCityLocation () + ", " + getData ().getBuildingID () + ", " + getData ().isUpdateBuildingSoldThisTurn ());
		
		// Remove building
		getMemoryBuildingUtils ().destroyBuilding (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (),
			(MapCoordinates3DEx) getData ().getCityLocation (), getData ().getBuildingID ());
		
		// If we sold this building, then record that we're not allowed to sell another one this turn
		if (getData ().isUpdateBuildingSoldThisTurn ())
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
				(getData ().getCityLocation ().getZ ()).getRow ().get (getData ().getCityLocation ().getY ()).getCell ().get
				(getData ().getCityLocation ().getX ()).setBuildingIdSoldThisTurn (getData ().getBuildingID ());
		
		// Removal of a building will alter what we can construct in that city, if we've got the change construction screen open
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

	/**
	 * @return Memory building utils
	 */
	public final MemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param mbu Memory building utils
	 */
	public final void setMemoryBuildingUtils (final MemoryBuildingUtils mbu)
	{
		memoryBuildingUtils = mbu;
	}
}