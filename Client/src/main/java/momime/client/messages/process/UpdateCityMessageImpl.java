package momime.client.messages.process;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import jakarta.xml.bind.JAXBException;
import momime.client.MomClient;
import momime.client.ui.frames.ChangeConstructionUI;
import momime.client.ui.frames.CitiesListUI;
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
	/** Multiplayer client */
	private MomClient client;
	
	/** Overland map UI */
	private OverlandMapUI overlandMapUI;
	
	/** Overland map right hand panel showing economy etc */
	private OverlandMapRightHandPanel overlandMapRightHandPanel;
	
	/** New turn messages UI */
	private NewTurnMessagesUI newTurnMessagesUI;

	/** Cities list */
	private CitiesListUI citiesListUI;
	
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
		final Set<UpdateUIElement> uiElements = processOneUpdate ();
		
		// Regenerate city images to show change in size or owner
		if (uiElements.contains (UpdateUIElement.REGENERATE_OVERLAND_MAP_BITMAPS))
			getOverlandMapUI ().regenerateOverlandMapBitmaps ();
		
		if (uiElements.contains (UpdateUIElement.REGENERATE_MINI_MAP_BITMAPS))
			getOverlandMapRightHandPanel ().regenerateMiniMapBitmap ();
		
		if (uiElements.contains (UpdateUIElement.REFRESH_CITIES_LIST))
			getCitiesListUI ().refreshCitiesList ();
		
		if (uiElements.contains (UpdateUIElement.REGENERATE_MINI_MAP_BITMAPS))
			getCitiesListUI ().regenerateMiniMapBitmaps ();
	}
	
	/**
	 * Method called for each individual update; so called once if message was sent in isolation, or multiple times if part of FogOfWarVisibleAreaChangedMessage
	 * 
	 * @return UI elements that need updating as a result of the changes
	 * @throws IOException If there is a problem
	 */
	public final Set<UpdateUIElement> processOneUpdate () throws IOException
	{
		final MemoryGridCell gc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
			(getData ().getMapLocation ().getZ ()).getRow ().get (getData ().getMapLocation ().getY ()).getCell ().get (getData ().getMapLocation ().getX ());
		
		// Compare the new and old data to see what UI elements we need to update
		final Set<UpdateUIElement> uiElements = new HashSet<UpdateUIElement> ();
		
		// Only need to regenerate mini maps if a city was created, destroyed, or changed owner
		if (((gc.getCityData () == null) && (getData ().getCityData () != null)) ||
			((gc.getCityData () != null) && (getData ().getCityData () == null)) ||
			((gc.getCityData () != null) && (getData ().getCityData () != null) && (gc.getCityData ().getCityOwnerID () != getData ().getCityData ().getCityOwnerID ())))
		{
			uiElements.add (UpdateUIElement.REGENERATE_MINI_MAP_BITMAPS);
			uiElements.add (UpdateUIElement.REGENERATE_OVERLAND_MAP_BITMAPS);
		}
		
		// Also regenerate overland map if the city size changed
		if ((gc.getCityData () != null) && (getData ().getCityData () != null) && (!gc.getCityData ().getCitySizeID ().equals (getData ().getCityData ().getCitySizeID ())))
			uiElements.add (UpdateUIElement.REGENERATE_OVERLAND_MAP_BITMAPS);
		
		// Only refresh cities list if the city used to be, or now is, ours
		if (((gc.getCityData () != null) && (gc.getCityData ().getCityOwnerID () == getClient ().getOurPlayerID ())) ||
			((getData ().getCityData () != null) && (getData ().getCityData ().getCityOwnerID () == getClient ().getOurPlayerID ())))
			
			uiElements.add (UpdateUIElement.REFRESH_CITIES_LIST);
		
		// Now accept the new data
		gc.setCityData (getData ().getCityData ());
		
		// If any city screen(s) are displaying this city then we need to update the display, or close it if the city was destroyed
		final CityViewUI cityView = getClient ().getCityViews ().get (getData ().getMapLocation ().toString ());
		if (cityView != null)
		{
			if (getData ().getCityData () != null)
			{
				// Update city screen
				cityView.cityDataChanged ();
				cityView.productionSoFarChanged ();
				cityView.recheckRushBuyEnabled ();
				cityView.spellsChanged ();
			}
			else
			{
				// Close city screen
				cityView.close ();
			}
		}
		
		// Also see if any change construction screens are open
		final ChangeConstructionUI changeConstruction = getClient ().getChangeConstructions ().get (getData ().getMapLocation ().toString ());
		if ((changeConstruction != null) && (getData ().getCityData () == null))
			changeConstruction.close ();;
		
		// If any new turn message(s) are showing what this city may have just constructed, then we need to update those as well
		getNewTurnMessagesUI ().cityDataChanged ((MapCoordinates3DEx) getData ().getMapLocation ());
		
		return uiElements;
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
	 * @return Cities list
	 */
	public final CitiesListUI getCitiesListUI ()
	{
		return citiesListUI;
	}

	/**
	 * @param ui Cities list
	 */
	public final void setCitiesListUI (final CitiesListUI ui)
	{
		citiesListUI = ui;
	}
}