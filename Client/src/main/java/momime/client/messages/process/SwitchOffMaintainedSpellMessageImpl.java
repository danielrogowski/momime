package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.ui.frames.ArmyListUI;
import momime.client.ui.frames.CityViewUI;
import momime.client.ui.frames.MagicSlidersUI;
import momime.client.ui.frames.UnitInfoUI;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.SwitchOffMaintainedSpellMessage;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.UnitUtils;

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
	
	/** Army list */
	private ArmyListUI armyListUI;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start: Spell URN " + getSpellURN ());
		
		processOneUpdate ();
		
		log.trace ("Exiting start");		
	}

	/**
	 * Method called for each individual update; so called once if message was sent in isolation, or multiple times if part of FogOfWarVisibleAreaChangedMessage
	 */
	public final void processOneUpdate ()
	{
		log.trace ("Entering processOneUpdate: Spell URN " + getSpellURN ());

		try
		{
			// Find the spell details before we remove it
			final MemoryMaintainedSpell spell = getMemoryMaintainedSpellUtils ().findSpellURN
				(getSpellURN (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (), "SwitchOffMaintainedSpellMessageImpl");
			
			// Remove it
			getMemoryMaintainedSpellUtils ().removeSpellURN (getSpellURN (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell ());
			
			// If we've got a city screen open showing where the spell was cancelled from, then remove it from the enchantments list
			if (spell.getCityLocation () != null)
			{
				final CityViewUI cityView = getClient ().getCityViews ().get (spell.getCityLocation ().toString ());
				if (cityView != null)
				{
					cityView.cityDataChanged ();
					cityView.spellsChanged ();
				}
			}
			
			// If we've got a unit info display showing for this unit, then remove the spell effect from it
			else if (spell.getUnitURN () != null)
			{
				final UnitInfoUI ui = getClient ().getUnitInfos ().get (spell.getUnitURN ());
				if (ui != null)
					ui.getUnitInfoPanel ().showUnit (ui.getUnit ());
				
				// Also need to update the upkeep shown on the army list?
				if (spell.getCastingPlayerID () == getClient ().getOurPlayerID ())
				{
					final MemoryUnit u = getUnitUtils ().findUnitURN (spell.getUnitURN (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "SwitchOffMaintainedSpellMessageImpl");
					
					if (u.getOwningPlayerID () == getClient ().getOurPlayerID ())
						getArmyListUI ().refreshArmyList ((MapCoordinates3DEx) u.getUnitLocation ());
				}
			}
			
			// If we've got the magic screen loaded up, update overland enchantments
			else
				getMagicSlidersUI ().spellsChanged ();
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
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

	/**
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final UnitUtils utils)
	{
		unitUtils = utils;
	}
}