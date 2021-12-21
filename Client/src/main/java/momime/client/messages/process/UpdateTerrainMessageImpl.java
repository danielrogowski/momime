package momime.client.messages.process;

import java.io.IOException;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.areas.operations.BooleanMapAreaOperations3D;
import com.ndg.map.areas.storage.MapArea3D;
import com.ndg.map.areas.storage.MapArea3DArrayListImpl;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.MomClient;
import momime.client.calculations.OverlandMapBitmapGenerator;
import momime.client.ui.frames.OverlandMapUI;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.servertoclient.UpdateTerrainMessage;
import momime.common.utils.CompareUtils;

/**
 * Server sends this to the client to tell them the map scenery
 */
public final class UpdateTerrainMessageImpl extends UpdateTerrainMessage implements BaseServerToClientMessage
{
	/** Multiplayer client */
	private MomClient client;
	
	/** Overland map UI */
	private OverlandMapUI overlandMapUI;
	
	/** Overland map right hand panel showing economy etc */
	private OverlandMapRightHandPanel overlandMapRightHandPanel;
	
	/** Overland map bitmap generator */
	private OverlandMapBitmapGenerator overlandMapBitmapGenerator;
	
	/** Operations for 3D boolean map areas */
	private BooleanMapAreaOperations3D booleanMapAreaOperations3D;

	/**
	 * Method called when this message is sent in isolation
	 * 
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		final MapArea3D<Boolean> areaToSmooth = new MapArea3DArrayListImpl<Boolean> ();
		areaToSmooth.setCoordinateSystem (getClient ().getSessionDescription ().getOverlandMapSize ());
		
		processOneUpdate (areaToSmooth);
		endUpdates (areaToSmooth);
		
		// Bit of a cop-out just regenerating everything, but we do have node auras and map features as well as the terrain to worry
		// about... so that's already almost everything (although we could avoid regenerating the units...)
		getOverlandMapUI ().regenerateOverlandMapBitmaps ();
		getOverlandMapRightHandPanel ().regenerateMiniMapBitmap ();
	}
	
	/**
	 * Method called for each individual update; so called once if message was sent in isolation, or multiple times if part of FogOfWarVisibleAreaChangedMessage
	 * @param areaToSmooth Keeps track of which tiles have been updated, so we know which need graphics updating for
	 */
	public final void processOneUpdate (final MapArea3D<Boolean> areaToSmooth)
	{
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
	}
	
	/**
	 * Called after processOneUpdate has been called n times
	 * @param areaToSmooth Keeps track of which tiles have been updated, so we know which need graphics updating for
	 * @throws RecordNotFoundException If required entries in the graphics XML cannot be found
	 */
	public final void endUpdates (final MapArea3D<Boolean> areaToSmooth) throws RecordNotFoundException
	{
		getBooleanMapAreaOperations3D ().enlarge (areaToSmooth, null, false);
		getOverlandMapBitmapGenerator ().smoothMapTerrain (areaToSmooth);
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
	 * @return Overland map right hand panel showing economy etc
	 */
	public final OverlandMapRightHandPanel getOverlandMapRightHandPanel ()
	{
		return overlandMapRightHandPanel;
	}

	/**
	 * @param panel Overland map right hand panel showing economy etc
	 */
	public final void setOverlandMapRightHandPanel (final OverlandMapRightHandPanel panel)
	{
		overlandMapRightHandPanel = panel;
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

	/**
	 * @return Overland map bitmap generator
	 */
	public final OverlandMapBitmapGenerator getOverlandMapBitmapGenerator ()
	{
		return overlandMapBitmapGenerator;
	}
	
	/**
	 * @param gen Overland map bitmap generator
	 */
	public final void setOverlandMapBitmapGenerator (final OverlandMapBitmapGenerator gen)
	{
		overlandMapBitmapGenerator = gen;
	}
}