package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.ui.frames.CityViewUI;
import momime.client.ui.frames.EditStringUI;
import momime.client.ui.frames.NewTurnMessagesUI;
import momime.client.ui.frames.OverlandMapUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.servertoclient.UpdateCityMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

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
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
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
		
		// Regenerate city images to show change in size
		getOverlandMapUI ().regenerateOverlandMapBitmaps ();
		
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
		
		// Server works out whether or not this is our city and if it has just been newly added, and so we need to name it
		if ((getData ().isAskForCityName () != null) && (getData ().isAskForCityName ()))
		{
			final EditStringUI askForCityName = getPrototypeFrameCreator ().createEditString ();
			askForCityName.setTitleLanguageCategoryID ("frmNameCity");
			askForCityName.setTitleLanguageEntryID ("Title");
			askForCityName.setPromptLanguageCategoryID ("frmNameCity");
			askForCityName.setPromptLanguageEntryID ("Prompt");
			askForCityName.setCityBeingNamed ((MapCoordinates3DEx) getData ().getMapLocation ());
			askForCityName.setText (getData ().getCityData ().getCityName ());
			try
			{
				askForCityName.setVisible (true);
			}
			catch (final Exception e)
			{
				log.error (e, e);
			}
		}
		
		// If any city screen(s) are displaying this city then we need to update the display
		final CityViewUI cityView = getClient ().getCityViews ().get (getData ().getMapLocation ().toString ());
		if (cityView != null)
		{
			cityView.cityDataChanged ();
			cityView.productionSoFarChanged ();
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
}