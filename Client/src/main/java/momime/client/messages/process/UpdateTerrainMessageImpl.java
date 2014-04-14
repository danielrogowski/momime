package momime.client.messages.process;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.ui.OverlandMapUI;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.servertoclient.v0_9_5.UpdateTerrainMessage;
import momime.common.messages.v0_9_5.MemoryGridCell;
import momime.common.utils.CompareUtils;

import com.ndg.map.areas.operations.BooleanMapAreaOperations3D;
import com.ndg.map.areas.storage.MapArea3D;
import com.ndg.map.areas.storage.MapArea3DArrayListImpl;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.client.MultiplayerServerConnection;
import com.ndg.multiplayer.client.SessionServerToClientMessage;

/**
 * Server sends this to the client to tell them the map scenery
 */
public final class UpdateTerrainMessageImpl extends UpdateTerrainMessage implements SessionServerToClientMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (UpdateTerrainMessageImpl.class.getName ());
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Overland map UI */
	private OverlandMapUI overlandMapUI;
	
	/** Operations for 3D boolean map areas */
	private BooleanMapAreaOperations3D booleanMapAreaOperations3D;

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
		log.entering (UpdateTerrainMessageImpl.class.getName (), "process", getData ().getMapLocation ());

		final MapArea3D<Boolean> areaToSmooth = new MapArea3DArrayListImpl<Boolean> ();
		areaToSmooth.setCoordinateSystem (getClient ().getSessionDescription ().getMapSize ());
		
		processOneUpdate (areaToSmooth);
		endUpdates (areaToSmooth);
		
		// Bit of a cop-out just regenerating everything, but we do have node auras and map features as well as the terrain to worry
		// about... so that's already almost everything (although we could avoid regenerating the units...)
		getOverlandMapUI ().regenerateOverlandMapBitmaps ();
		
		log.exiting (UpdateTerrainMessageImpl.class.getName (), "process");
	}
	
	/**
	 * Method called for each individual update; so called once if message was sent in isolation, or multiple times if part of FogOfWarVisibleAreaChangedMessage
	 * @param areaToSmooth Keeps track of which tiles have been updated, so we know which need graphics updating for
	 */
	public final void processOneUpdate (final MapArea3D<Boolean> areaToSmooth)
	{
		log.entering (UpdateTerrainMessageImpl.class.getName (), "processOneUpdate", getData ().getMapLocation ());
		
		final MemoryGridCell gc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
			(getData ().getMapLocation ().getZ ()).getRow ().get (getData ().getMapLocation ().getY ()).getCell ().get (getData ().getMapLocation ().getX ());
		
		// Only need to re-smooth the area if the tile type or rivers have changed
		final String oldTileTypeID = (gc.getTerrainData () == null) ? null : gc.getTerrainData ().getTileTypeID ();
		final String newTileTypeID = (getData ().getTerrainData () == null) ? null : getData ().getTerrainData ().getTileTypeID ();

		final String oldRiverDirections = (gc.getTerrainData () == null) ? null : gc.getTerrainData ().getRiverDirections ();
		final String newRiverDirections = (getData ().getTerrainData () == null) ? null : getData ().getTerrainData ().getRiverDirections ();
		
		if ((!CompareUtils.safeStringCompare (oldTileTypeID, newTileTypeID)) || (!CompareUtils.safeStringCompare (oldRiverDirections, newRiverDirections)))
			areaToSmooth.set ((MapCoordinates3DEx) getData ().getMapLocation (), true);

		// Actually update it
		gc.setTerrainData (getData ().getTerrainData ());
		
		log.exiting (UpdateTerrainMessageImpl.class.getName (), "processOneUpdate");
	}
	
	/**
	 * Called after processOneUpdate has been called n times
	 * @param areaToSmooth Keeps track of which tiles have been updated, so we know which need graphics updating for
	 * @throws RecordNotFoundException If required entries in the graphics XML cannot be found
	 */
	public final void endUpdates (final MapArea3D<Boolean> areaToSmooth) throws RecordNotFoundException
	{
		log.entering (UpdateTerrainMessageImpl.class.getName (), "endUpdates");
		
		getBooleanMapAreaOperations3D ().enlarge (areaToSmooth, null);
		getOverlandMapUI ().smoothMapTerrain (areaToSmooth);
		
		log.exiting (UpdateTerrainMessageImpl.class.getName (), "endUpdates");
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
	 * @return Operations for 3D boolean map areas
	 */
	public final BooleanMapAreaOperations3D getBooleanMapAreaOperations3D ()
	{
		return booleanMapAreaOperations3D;
	}

	/**
	 * @param op Operations for 3D boolean map areas
	 */
	public final void setBooleanMapAreaOperations3D (final BooleanMapAreaOperations3D op)
	{
		booleanMapAreaOperations3D = op;
	}
}
