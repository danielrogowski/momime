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
import momime.common.calculations.CityCalculations;
import momime.common.database.CommonDatabaseConstants;
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
	private static final Log log = LogFactory.getLog (AddBuildingMessageImpl.class);

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
		log.trace ("Entering start: " + getFirstBuilding ().getCityLocation () + ", " + getFirstBuilding ().getBuildingID () + ", " +
			((getSecondBuilding () == null) ? "null" : getSecondBuilding ().getBuildingID ()));
		
		// If its a city spell, show an animation for it and don't even add the spell yet - the animation handles that as well
		boolean animated = false;
		if (getBuildingCreatedFromSpellID () != null)
		{
			// If we cast it, then update the entry on the NTM scroll that's telling us to choose a target for it
			if ((getBuildingCreationSpellCastByPlayerID () != null) && (getBuildingCreationSpellCastByPlayerID ().equals (getClient ().getOurPlayerID ())) &&
				(getOverlandMapRightHandPanel ().getTargetSpell () != null) && (getOverlandMapRightHandPanel ().getTargetSpell ().getSpellID ().equals (getBuildingCreatedFromSpellID ())))
			{
				getOverlandMapRightHandPanel ().getTargetSpell ().setTargettedCity ((MapCoordinates3DEx) getFirstBuilding ().getCityLocation ());
				
				// Redraw the NTMs
				getNewTurnMessagesUI ().languageChanged ();
			}
			
			// If we cast it OR its our city, then display a popup window for it.
			// Exception is Spell of Return - this adds buildings, and has a animation to display, but we need to display that even if we cannot
			// see the city where the wizard is returning to (in which case we won't get the AddBuildingMessage).
			// So that's handled from the UpdateWizardState message and not here.
			if (!getBuildingCreatedFromSpellID ().equals (CommonDatabaseConstants.SPELL_ID_SPELL_OF_RETURN))
			{
				final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
					(getFirstBuilding ().getCityLocation ().getZ ()).getRow ().get (getFirstBuilding ().getCityLocation ().getY ()).getCell ().get (getFirstBuilding ().getCityLocation ().getX ()).getCityData ();
				
				if (((getBuildingCreationSpellCastByPlayerID () != null) && (getBuildingCreationSpellCastByPlayerID ().equals (getClient ().getOurPlayerID ()))) ||
					((cityData != null) && (cityData.getCityOwnerID () == getClient ().getOurPlayerID ())))
				{
					animated = true;
					
					final MiniCityViewUI miniCityView = getPrototypeFrameCreator ().createMiniCityView ();
					miniCityView.setCityLocation ((MapCoordinates3DEx) getFirstBuilding ().getCityLocation ());
					miniCityView.setRenderCityData (getCityCalculations ().buildRenderCityData ((MapCoordinates3DEx) getFirstBuilding ().getCityLocation (),
						getClient ().getSessionDescription ().getOverlandMapSize (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ()));						
					miniCityView.setBuildingMessage (this);
					miniCityView.setVisible (true);
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
		
		log.trace ("Exiting start");
	}

	/**
	 * Method called for each individual update; so called once if message was sent in isolation, or multiple times if part of FogOfWarVisibleAreaChangedMessage
	 */
	public final void processOneUpdate ()
	{
		log.trace ("Entering processOneUpdate: " + getFirstBuilding ().getCityLocation () + ", " + getFirstBuilding ().getBuildingID () + ", " +
			((getSecondBuilding () == null) ? "null" : getSecondBuilding ().getBuildingID ()));
		
		// Add building(s)
		getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding ().add (firstBuilding);
		if (getSecondBuilding () != null)
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding ().add (secondBuilding);
		
		// If we've got a city screen open showing this location, may need to set up animation to display the new building(s)
		final CityViewUI cityView = getClient ().getCityViews ().get (getFirstBuilding ().getCityLocation ().toString ());
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
		final ChangeConstructionUI changeConstruction = getClient ().getChangeConstructions ().get (getFirstBuilding ().getCityLocation ().toString ());
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