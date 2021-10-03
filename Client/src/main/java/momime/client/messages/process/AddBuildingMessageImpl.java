package momime.client.messages.process;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.base.client.CustomDurationServerToClientMessage;

import momime.client.MomClient;
import momime.client.ui.dialogs.MiniCityViewUI;
import momime.client.ui.frames.ChangeConstructionUI;
import momime.client.ui.frames.CityViewUI;
import momime.client.ui.frames.NewTurnMessagesUI;
import momime.client.ui.frames.OverlandMapUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.common.calculations.CityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.servertoclient.AddBuildingMessage;

/**
 * Server sends this to clients to tell them about a building added to a city
 */
public final class AddBuildingMessageImpl extends AddBuildingMessage implements CustomDurationServerToClientMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (AddBuildingMessageImpl.class);

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
	
	/** City calculations */
	private CityCalculations cityCalculations;
	
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
		// If its a city spell, show an animation for it and don't even add the building yet - the animation handles that as well
		boolean animated = false;
		if (getBuildingsCreatedFromSpellID () != null)
		{
			// Find which city(s) were changed
			final List<MapCoordinates3DEx> cityLocations = getBuilding ().stream ().map (b -> (MapCoordinates3DEx) b.getCityLocation ()).distinct ().collect (Collectors.toList ());		
			for (final MapCoordinates3DEx cityLocation : cityLocations)
			{
				// If we cast it, then update the entry on the NTM scroll that's telling us to choose a target for it
				if ((getBuildingCreationSpellCastByPlayerID () != null) && (getBuildingCreationSpellCastByPlayerID ().equals (getClient ().getOurPlayerID ())) &&
					(getOverlandMapRightHandPanel ().getTargetSpell () != null) && (getOverlandMapRightHandPanel ().getTargetSpell ().getSpellID ().equals (getBuildingsCreatedFromSpellID ())))
				{
					getOverlandMapRightHandPanel ().getTargetSpell ().setTargetedCity (cityLocation);
					
					// Redraw the NTMs
					getNewTurnMessagesUI ().languageChanged ();
				}
				
				// If we cast it OR its our city, then display a popup window for it.
				// Exception is Spell of Return - this adds buildings, and has a animation to display, but we need to display that even if we cannot
				// see the city where the wizard is returning to (in which case we won't get the AddBuildingMessage).
				// So that's handled from the UpdateWizardState message and not here.
				if (!getBuildingsCreatedFromSpellID ().equals (CommonDatabaseConstants.SPELL_ID_SPELL_OF_RETURN))
				{
					final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
						(cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();
					
					if (((getBuildingCreationSpellCastByPlayerID () != null) && (getBuildingCreationSpellCastByPlayerID ().equals (getClient ().getOurPlayerID ()))) ||
						((cityData != null) && (cityData.getCityOwnerID () == getClient ().getOurPlayerID ())))
					{
						animated = true;
						
						final MiniCityViewUI miniCityView = getPrototypeFrameCreator ().createMiniCityView ();
						miniCityView.setCityLocation (cityLocation);
						miniCityView.setRenderCityData (getCityCalculations ().buildRenderCityData (cityLocation,
							getClient ().getSessionDescription ().getOverlandMapSize (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ()));						
						miniCityView.setAddBuildingMessage (this);
						miniCityView.setVisible (true);
					}
				}
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
	}

	/**
	 * Method called for each individual update; so called once if message was sent in isolation, or multiple times if part of FogOfWarVisibleAreaChangedMessage
	 */
	public final void processOneUpdate ()
	{
		// Add building(s)
		getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding ().addAll (getBuilding ());

		// Find which city(s) were changed
		final List<MapCoordinates3DEx> cityLocations = getBuilding ().stream ().map (b -> (MapCoordinates3DEx) b.getCityLocation ()).distinct ().collect (Collectors.toList ());		
		for (final MapCoordinates3DEx cityLocation : cityLocations)
		{
			// If we've got a city screen open showing this location, need to rebuild RenderCityData
			final CityViewUI cityView = getClient ().getCityViews ().get (cityLocation.toString ());
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
			final ChangeConstructionUI changeConstruction = getClient ().getChangeConstructions ().get (cityLocation.toString ());
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

	/**
	 * @return City calculations
	 */
	public final CityCalculations getCityCalculations ()
	{
		return cityCalculations;
	}

	/**
	 * @param calc City calculations
	 */
	public final void setCityCalculations (final CityCalculations calc)
	{
		cityCalculations = calc;
	}
}