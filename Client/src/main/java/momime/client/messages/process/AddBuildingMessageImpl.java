package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.ui.dialogs.MiniCityViewUI;
import momime.client.ui.frames.ChangeConstructionUI;
import momime.client.ui.frames.CityViewUI;
import momime.client.ui.frames.NewTurnMessagesUI;
import momime.client.ui.frames.OverlandMapUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.servertoclient.AddBuildingMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.base.client.CustomDurationServerToClientMessage;

/**
 * Server sends this to clients to tell them about a building added to a city
 */
public final class AddBuildingMessageImpl extends AddBuildingMessage implements CustomDurationServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (AddBuildingMessageImpl.class);

	/** Multiplayer client */
	private MomClient client;
	
	/** Overland map UI */
	private OverlandMapUI overlandMapUI;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** Overland map right hand panel showing economy etc */
	private OverlandMapRightHandPanel overlandMapRightHandPanel;
	
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
		log.trace ("Entering start: " + getData ().getCityLocation () + ", " + getData ().getFirstBuildingID () + ", " + getData ().getSecondBuildingID ());
		
		// If its a city spell, show an animation for it and don't even add the spell yet - the animation handles that as well
		boolean animated = false;
		if (getData ().getBuildingCreatedFromSpellID () != null)
		{
			// If we cast it, then update the entry on the NTM scroll that's telling us to choose a target for it
			if ((getData ().getBuildingCreationSpellCastByPlayerID () != null) && (getData ().getBuildingCreationSpellCastByPlayerID ().equals (getClient ().getOurPlayerID ())) &&
				(getOverlandMapRightHandPanel ().getTargetSpell () != null) && (getOverlandMapRightHandPanel ().getTargetSpell ().getSpellID ().equals (getData ().getBuildingCreatedFromSpellID ())))
			{
				getOverlandMapRightHandPanel ().getTargetSpell ().setTargettedCity ((MapCoordinates3DEx) getData ().getCityLocation ());
				
				// Redraw the NTMs
				getNewTurnMessagesUI ().languageChanged ();
			}
			
			// If we cast it OR its our city, then display a popup window for it
			final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
				(getData ().getCityLocation ().getZ ()).getRow ().get (getData ().getCityLocation ().getY ()).getCell ().get (getData ().getCityLocation ().getX ()).getCityData ();
			
			if (((getData ().getBuildingCreationSpellCastByPlayerID () != null) && (getData ().getBuildingCreationSpellCastByPlayerID ().equals (getClient ().getOurPlayerID ()))) ||
				((cityData != null) && (cityData.getCityOwnerID () != null) && (cityData.getCityOwnerID ().equals (getClient ().getOurPlayerID ()))))
			{
				animated = true;
				
				final MiniCityViewUI miniCityView = getPrototypeFrameCreator ().createMiniCityView ();
				miniCityView.setCityLocation ((MapCoordinates3DEx) getData ().getCityLocation ());
				miniCityView.setBuildingMessage (this);
				miniCityView.setVisible (true);
			}
		}
		
		// If no spell animation, then just add it right away
		if (!animated)
		{
			processOneUpdate ();
			
			// Building may have been city walls and so affect the overland map view
			getOverlandMapUI ().regenerateOverlandMapBitmaps ();
	
			// Don't halt processing of messages
			getClient ().finishCustomDurationMessage (this);
			
		}
		
		log.trace ("Exiting start");
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
		
		// If we've got a city screen open showing this location, may need to set up animation to display the new building(s)
		final CityViewUI cityView = getClient ().getCityViews ().get (getData ().getCityLocation ().toString ());
		if (cityView != null)
			try
			{
				cityView.cityDataChanged ();
			}
			catch (final Exception e)
			{
				log.error (e, e);
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
	 * Nothing to do here when the message completes, because its all handled in MiniCityViewUI
	 */
	@Override
	public final void finish ()
	{
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
}