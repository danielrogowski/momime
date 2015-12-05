package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.MomClient;
import momime.client.ui.frames.ArmyListUI;
import momime.client.ui.frames.CityViewUI;
import momime.client.ui.frames.NewTurnMessagesUI;
import momime.client.ui.frames.OverlandMapUI;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.servertoclient.UpdateCityMessage;

/**
 * Server sends this to the client to tell them the map scenery
 */
public final class UpdateCityMessageImpl extends UpdateCityMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (UpdateCityMessageImpl.class);

	/** Multiplayer client */
	private MomClient client;
	
	/** Overland map UI */
	private OverlandMapUI overlandMapUI;
	
	/** Overland map right hand panel showing economy etc */
	private OverlandMapRightHandPanel overlandMapRightHandPanel;
	
	/** Army list */
	private ArmyListUI armyListUI;
	
	/** New turn messages UI */
	private NewTurnMessagesUI newTurnMessagesUI;
	
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
		log.trace ("Entering start: " + getData ().getMapLocation ());
		
		processOneUpdate ();
		
		// Regenerate city images to show change in size or owner
		getOverlandMapUI ().regenerateOverlandMapBitmaps ();
		getOverlandMapRightHandPanel ().regenerateMiniMapBitmap ();
		getArmyListUI ().regenerateMiniMapBitmaps ();
		
		log.trace ("Exiting start");
	}
	
	/**
	 * Method called for each individual update; so called once if message was sent in isolation, or multiple times if part of FogOfWarVisibleAreaChangedMessage
	 * @throws IOException If there is a problem
	 */
	public final void processOneUpdate () throws IOException
	{
		log.trace ("Entering processOneUpdate: " + getData ().getMapLocation ());
		
		final MemoryGridCell gc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
			(getData ().getMapLocation ().getZ ()).getRow ().get (getData ().getMapLocation ().getY ()).getCell ().get (getData ().getMapLocation ().getX ());
		
		gc.setCityData (getData ().getCityData ());
		
		// If any city screen(s) are displaying this city then we need to update the display
		final CityViewUI cityView = getClient ().getCityViews ().get (getData ().getMapLocation ().toString ());
		if (cityView != null)
		{
			cityView.cityDataChanged ();
			cityView.productionSoFarChanged ();
			cityView.recheckRushBuyEnabled ();
		}
		
		// If any new turn message(s) are showing what this city may have just constructed, then we need to update those as well
		getNewTurnMessagesUI ().cityDataChanged ((MapCoordinates3DEx) getData ().getMapLocation ());
		
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
	 * @return New turn messages UI
	 */
	public final NewTurnMessagesUI getNewTurnMessagesUI ()
	{
		return newTurnMessagesUI;
	}

	/**
	 * @param ui New turn messages UI
	 */
	public final void setNewTurnMessagesUI (final NewTurnMessagesUI ui)
	{
		newTurnMessagesUI = ui;
	}

	/**
	 * @return Army list
	 */
	public final ArmyListUI getArmyListUI ()
	{
		return armyListUI;
	}
	
	/**
	 * @param ui Army list
	 */
	public final void setArmyListUI (final ArmyListUI ui)
	{
		armyListUI = ui;
	}
}