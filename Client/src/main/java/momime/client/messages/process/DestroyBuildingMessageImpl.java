package momime.client.messages.process;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.JAXBException;
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
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.servertoclient.DestroyBuildingMessage;
import momime.common.utils.MemoryBuildingUtils;

/**
 * Server sends this to clients to tell them about a building destroyed (or sold) from a city
 */
public final class DestroyBuildingMessageImpl extends DestroyBuildingMessage implements CustomDurationServerToClientMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (DestroyBuildingMessageImpl.class);

	/** Multiplayer client */
	private MomClient client;
	
	/** Overland map UI */
	private OverlandMapUI overlandMapUI;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** Overland map right hand panel showing economy etc */
	private OverlandMapRightHandPanel overlandMapRightHandPanel;

	/** New turn messages UI */
	private NewTurnMessagesUI newTurnMessagesUI;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** City calculations */
	private CityCalculations cityCalculations;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		// If its a city spell like Earthquake, show an animation for it and don't even remove the buildings yet - the animation handles that as well
		boolean animated = false;
		if ((getBuildingsDestroyedBySpellID () != null) && (getBuildingDestructionSpellLocation () != null))
		{
			// If we cast it, then update the entry on the NTM scroll that's telling us to choose a target for it
			if ((getBuildingDestructionSpellCastByPlayerID () != null) && (getBuildingDestructionSpellCastByPlayerID ().equals (getClient ().getOurPlayerID ())) &&
				(getOverlandMapRightHandPanel ().getTargetSpell () != null) && (getOverlandMapRightHandPanel ().getTargetSpell ().getSpellID ().equals (getBuildingsDestroyedBySpellID ())))
			{
				getOverlandMapRightHandPanel ().getTargetSpell ().setTargetedCity ((MapCoordinates3DEx) getBuildingDestructionSpellLocation ());
				
				// Redraw the NTMs
				getNewTurnMessagesUI ().languageChanged ();
			}
			
			// If we cast it OR its our city, and its a one time spell not a CAE that triggers every turn, then display a popup window for it.
			final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
				(getBuildingDestructionSpellLocation ().getZ ()).getRow ().get (getBuildingDestructionSpellLocation ().getY ()).getCell ().get (getBuildingDestructionSpellLocation ().getX ()).getCityData ();

			final Spell spellDef = getClient ().getClientDB ().findSpell (getBuildingsDestroyedBySpellID (), "DestroyBuildingMessageImpl");
			
			if ((getBuildingDestructionSpellCastByPlayerID () != null) && (cityData != null) &&
				(spellDef.getSpellHasCityEffect ().size () == 0) && (spellDef.getCombatCastAnimation () == null) &&
				((getBuildingDestructionSpellCastByPlayerID ().equals (getClient ().getOurPlayerID ())) || (cityData.getCityOwnerID () == getClient ().getOurPlayerID ())))
			{
				animated = true;
				
				final MiniCityViewUI miniCityView = getPrototypeFrameCreator ().createMiniCityView ();
				miniCityView.setCityLocation ((MapCoordinates3DEx) getBuildingDestructionSpellLocation ());
				miniCityView.setRenderCityData (getCityCalculations ().buildRenderCityData ((MapCoordinates3DEx) getBuildingDestructionSpellLocation (),
					getClient ().getSessionDescription ().getOverlandMapSize (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ()));						
				miniCityView.setDestroyBuildingMessage (this);
				miniCityView.setVisible (true);
			}
		}
		
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
	 * @throws RecordNotFoundException If we can't find the building that the server is telling us to remove
	 */
	public final void processOneUpdate () throws RecordNotFoundException
	{
		final List<MapCoordinates3DEx> cityLocations = new ArrayList<MapCoordinates3DEx> ();
		for (final Integer thisBuildingURN : getBuildingURN ())
		{
			// Grab details about the building before we remove it
			final MemoryBuilding building = getMemoryBuildingUtils ().findBuildingURN
				(thisBuildingURN, getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (), "DestroyBuildingMessageImpl");
			
			if (!cityLocations.contains (building.getCityLocation ()))
				cityLocations.add ((MapCoordinates3DEx) building.getCityLocation ());
			
			// Remove building
			getMemoryBuildingUtils ().removeBuildingURN (thisBuildingURN, getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding ());

			// If we sold this building, then record that we're not allowed to sell another one this turn
			if (isUpdateBuildingSoldThisTurn ())
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
					(building.getCityLocation ().getZ ()).getRow ().get (building.getCityLocation ().getY ()).getCell ().get
					(building.getCityLocation ().getX ()).setBuildingIdSoldThisTurn (building.getBuildingID ());
		}
		
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
			
			// Removal of a building will alter what we can construct in that city, if we've got the change construction screen open
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