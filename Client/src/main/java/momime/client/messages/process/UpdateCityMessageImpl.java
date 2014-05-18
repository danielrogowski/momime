package momime.client.messages.process;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.ui.CityViewUI;
import momime.client.ui.EditStringUI;
import momime.client.ui.OverlandMapUI;
import momime.client.ui.PrototypeFrameCreator;
import momime.common.messages.servertoclient.v0_9_5.UpdateCityMessage;
import momime.common.messages.v0_9_5.MemoryGridCell;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.client.MultiplayerServerConnection;
import com.ndg.multiplayer.client.SessionServerToClientMessage;

/**
 * Server sends this to the client to tell them the map scenery
y */
public final class UpdateCityMessageImpl extends UpdateCityMessage implements SessionServerToClientMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (UpdateCityMessageImpl.class.getName ());

	/** Multiplayer client */
	private MomClient client;
	
	/** Overland map UI */
	private OverlandMapUI overlandMapUI;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
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
		log.entering (UpdateCityMessageImpl.class.getName (), "process", getData ().getMapLocation ());
		
		processOneUpdate ();
		
		// Regenerate city images to show change in size
		getOverlandMapUI ().regenerateOverlandMapBitmaps ();
		
		log.exiting (UpdateCityMessageImpl.class.getName (), "process");
	}
	
	/**
	 * Method called for each individual update; so called once if message was sent in isolation, or multiple times if part of FogOfWarVisibleAreaChangedMessage
	 * @throws IOException If there is a problem
	 */
	public final void processOneUpdate () throws IOException
	{
		log.entering (UpdateCityMessageImpl.class.getName (), "processOneUpdate", getData ().getMapLocation ());
		
		final MemoryGridCell gc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
			(getData ().getMapLocation ().getZ ()).getRow ().get (getData ().getMapLocation ().getY ()).getCell ().get (getData ().getMapLocation ().getX ());
		
		gc.setCityData (getData ().getCityData ());
		
		// Server works out whether or not this is our city and if it has just been newly added, and so we need to name it
		if ((getData ().isAskForCityName () != null) && (getData ().isAskForCityName ()))
		{
			final EditStringUI askForCityName = getPrototypeFrameCreator ().createEditString ();
			askForCityName.setPrompt ("Enter name for new City");
			askForCityName.setCityBeingNamed ((MapCoordinates3DEx) getData ().getMapLocation ());
			askForCityName.setText (getData ().getCityData ().getCityName ());
			try
			{
				askForCityName.setVisible (true);
			}
			catch (final Exception e)
			{
				e.printStackTrace ();
			}
		}
		
		// If any city screen(s) are displaying this city then we need to update the display
		final CityViewUI cityView = getClient ().getCityViews ().get (getData ().getMapLocation ().toString ());
		if (cityView != null)
			cityView.cityDataUpdated ();
		
		log.exiting (UpdateCityMessageImpl.class.getName (), "processOneUpdate");
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
	 * @return Prototype frame creator
	 */
	public final PrototypeFrameCreator getPrototypeFrameCreator ()
	{
		return prototypeFrameCreator;
	}

	/**
	 * @param obj Prototype frame creator
	 */
	public final void setPrototypeFrameCreator (final PrototypeFrameCreator obj)
	{
		prototypeFrameCreator = obj;
	}
}
