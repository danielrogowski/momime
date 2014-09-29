package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.ui.dialogs.MiniCityViewUI;
import momime.client.ui.frames.CityViewUI;
import momime.client.ui.frames.NewTurnMessagesUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.common.database.v0_9_5.Spell;
import momime.common.database.v0_9_5.SpellBookSectionID;
import momime.common.messages.servertoclient.v0_9_5.AddMaintainedSpellMessage;
import momime.common.messages.v0_9_5.OverlandMapCityData;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.base.client.CustomDurationServerToClientMessage;

/**
 * Server sends this to notify clients of new maintained spells cast, or those that have newly come into view
 */
public final class AddMaintainedSpellMessageImpl extends AddMaintainedSpellMessage implements CustomDurationServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (AddMaintainedSpellMessageImpl.class);

	/** Multiplayer client */
	private MomClient client;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** Overland map right hand panel showing economy etc */
	private OverlandMapRightHandPanel overlandMapRightHandPanel;
	
	/** New turn messages UI */
	private NewTurnMessagesUI newTurnMessagesUI;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start: " + getData ().getSpellID ());
		
		// If its a city spell, show an animation for it and don't even add the spell yet - the animation handles that as well
		final Spell spell = getClient ().getClientDB ().findSpell (getData ().getSpellID (), "AddMaintainedSpellMessageImpl");
		boolean animated = false;
		
		if ((spell.getSpellBookSectionID () == SpellBookSectionID.CITY_ENCHANTMENTS) || (spell.getSpellBookSectionID () == SpellBookSectionID.CITY_CURSES))
		{
			// If we cast it, then update the entry on the NTM scroll that's telling us to choose a target for it
			if ((getData ().getCastingPlayerID () == getClient ().getOurPlayerID ()) && (getOverlandMapRightHandPanel ().getTargetSpell () != null) &&
				(getOverlandMapRightHandPanel ().getTargetSpell ().getSpellID ().equals (getData ().getSpellID ())))
			{
				getOverlandMapRightHandPanel ().getTargetSpell ().setTargettedCity ((MapCoordinates3DEx) getData ().getCityLocation ());
				
				// Redraw the NTMs
				getNewTurnMessagesUI ().languageChanged ();
			}
			
			// If we cast it OR its our city, then display a popup window for it
			final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
				(getData ().getCityLocation ().getZ ()).getRow ().get (getData ().getCityLocation ().getY ()).getCell ().get (getData ().getCityLocation ().getX ()).getCityData ();
			
			if ((getData ().getCastingPlayerID () == getClient ().getOurPlayerID ()) ||
				((cityData != null) && (cityData.getCityOwnerID () != null) && (cityData.getCityOwnerID ().equals (getClient ().getOurPlayerID ()))))
			{
				animated = true;
				
				final MiniCityViewUI miniCityView = getPrototypeFrameCreator ().createMiniCityView ();
				miniCityView.setCityLocation ((MapCoordinates3DEx) getData ().getCityLocation ());
				miniCityView.setSpellMessage (this);
				miniCityView.setVisible (true);
			}
		}
		
		// If no spell animation, then just add it right away
		if (!animated)
		{
			processOneUpdate ();
			
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
		log.trace ("Entering processOneUpdate: " + getData ().getSpellID ());
		
		getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell ().add (getData ());
		
		// If we've got a city screen open showing where the spell was cast, may need to set up animation to display it
		if (getData ().getCityLocation () != null)
		{
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