package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.ui.frames.CityViewUI;
import momime.client.ui.frames.MagicSlidersUI;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.servertoclient.v0_9_5.SwitchOffMaintainedSpellMessage;
import momime.common.utils.MemoryMaintainedSpellUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

/**
 * Server sends this to notify clients of cancelled maintained spells, or those that have gone out of view
 */
public final class SwitchOffMaintainedSpellMessageImpl extends SwitchOffMaintainedSpellMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (SwitchOffMaintainedSpellMessageImpl.class);

	/** Multiplayer client */
	private MomClient client;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Magic sliders screen */
	private MagicSlidersUI magicSlidersUI;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start: " + getData ().getSpellID ());
		
		processOneUpdate ();
		
		log.trace ("Exiting start");		
	}

	/**
	 * Method called for each individual update; so called once if message was sent in isolation, or multiple times if part of FogOfWarVisibleAreaChangedMessage
	 * @throws RecordNotFoundException If we can't find the spell we're supposed to be switching off
	 */
	public final void processOneUpdate () throws RecordNotFoundException
	{
		log.trace ("Entering processOneUpdate: " + getData ().getSpellID ());
		
		getMemoryMaintainedSpellUtils ().switchOffMaintainedSpell (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
			getData ().getCastingPlayerID (), getData ().getSpellID (), getData ().getUnitURN (), getData ().getUnitSkillID (),
			(MapCoordinates3DEx) getData ().getCityLocation (), getData ().getCitySpellEffectID ());
		
		// If we've got a city screen open showing where the spell was cancelled from, then remove it from the enchantments list
		if (getData ().getCityLocation () != null)
		{
			final CityViewUI cityView = getClient ().getCityViews ().get (getData ().getCityLocation ().toString ());
			if (cityView != null)
				try
				{
					cityView.cityDataChanged ();
					cityView.spellsChanged ();
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
		}
		
		// If we've got the magic screen loaded up, update overland enchantments
		else if (getData ().getUnitURN () == null)
			getMagicSlidersUI ().spellsChanged ();
		
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
	 * @return MemoryMaintainedSpell utils
	 */
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param spellUtils MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils spellUtils)
	{
		memoryMaintainedSpellUtils = spellUtils;
	}

	/**
	 * @return Magic sliders screen
	 */
	public final MagicSlidersUI getMagicSlidersUI ()
	{
		return magicSlidersUI;
	}

	/**
	 * @param ui Magic sliders screen
	 */
	public final void setMagicSlidersUI (final MagicSlidersUI ui)
	{
		magicSlidersUI = ui;
	}
}