package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.ui.frames.ChangeConstructionUI;
import momime.client.ui.frames.CityViewUI;
import momime.client.ui.frames.OverlandMapUI;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.servertoclient.DestroyBuildingMessage;
import momime.common.utils.MemoryBuildingUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

/**
 * Server sends this to clients to tell them about a building destroyed (or sold) from a city
 */
public final class DestroyBuildingMessageImpl extends DestroyBuildingMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (DestroyBuildingMessageImpl.class);

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
		processOneUpdate ();
		
		// Building may have been city walls and so affect the overland map view
		getOverlandMapUI ().regenerateOverlandMapBitmaps ();
	}

	/**
	 * Method called for each individual update; so called once if message was sent in isolation, or multiple times if part of FogOfWarVisibleAreaChangedMessage
	 * @throws RecordNotFoundException If we can't find the building that the server is telling us to remove
	 */
	public final void processOneUpdate () throws RecordNotFoundException
	{
		// Grab details about the building before we remove it
		final MemoryBuilding building = getMemoryBuildingUtils ().findBuildingURN
			(getBuildingURN (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (), "DestroyBuildingMessageImpl");
		
		// Remove building
		getMemoryBuildingUtils ().removeBuildingURN (getBuildingURN (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding ());
		
		// If we sold this building, then record that we're not allowed to sell another one this turn
		if (isUpdateBuildingSoldThisTurn ())
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
				(building.getCityLocation ().getZ ()).getRow ().get (building.getCityLocation ().getY ()).getCell ().get
				(building.getCityLocation ().getX ()).setBuildingIdSoldThisTurn (building.getBuildingID ());
		
		// If we've got a city screen open showing this location, need to rebuild RenderCityData
		final CityViewUI cityView = getClient ().getCityViews ().get (building.getCityLocation ().toString ());
		if (cityView != null)
			try
			{
				cityView.cityDataChanged ();
			}
			catch (final Exception e)
			{
				log.error (e, e);
			}		
		
		// Removal of a building will alter what we can construct in that city, if we've got the change construction screen open
		final ChangeConstructionUI changeConstruction = getClient ().getChangeConstructions ().get (building.getCityLocation ().toString ());
		if (changeConstruction != null)
			try
			{
				changeConstruction.updateWhatCanBeConstructed ();
			}
			catch (final Exception e)
			{
				log.error (e, e);
			}		
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