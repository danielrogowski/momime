package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.MomClient;
import momime.client.ui.frames.CityViewUI;
import momime.client.ui.frames.CombatUI;
import momime.client.ui.frames.MagicSlidersUI;
import momime.client.ui.frames.UnitInfoUI;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.SwitchOffMaintainedSpellMessage;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.UnitUtils;

/**
 * Server sends this to notify clients of cancelled maintained spells, or those that have gone out of view
 */
public final class SwitchOffMaintainedSpellMessageImpl extends SwitchOffMaintainedSpellMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (SwitchOffMaintainedSpellMessageImpl.class);

	/** Multiplayer client */
	private MomClient client;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Magic sliders screen */
	private MagicSlidersUI magicSlidersUI;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Combat UI */
	private CombatUI combatUI;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		processOneUpdate ();
	}

	/**
	 * Method called for each individual update; so called once if message was sent in isolation, or multiple times if part of FogOfWarVisibleAreaChangedMessage
	 */
	public final void processOneUpdate ()
	{
		try
		{
			// Find the spell details before we remove it
			final MemoryMaintainedSpell spell = getMemoryMaintainedSpellUtils ().findSpellURN
				(getSpellURN (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (), "SwitchOffMaintainedSpellMessageImpl.processOneUpdate");
			
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
					ui.getUnitInfoPanel ().refreshUnitDetails ();

				// If its being removed from a combat unit, need to check if we need to remove an animation from over the unit's head to no longer show the effect, e.g. Confusion
				if (spell.isCastInCombat ())
				{
					final MemoryUnit u = getUnitUtils ().findUnitURN (spell.getUnitURN (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "SwitchOffMaintainedSpellMessageImpl.processOneUpdate (C)");
					
					final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (u, null, null, null,
						getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());
					
					getCombatUI ().setUnitToDrawAtLocation (u.getCombatPosition ().getX (), u.getCombatPosition ().getY (), xu);
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

	/**
	 * @return Combat UI
	 */
	public final CombatUI getCombatUI ()
	{
		return combatUI;
	}

	/**
	 * @param ui Combat UI
	 */
	public final void setCombatUI (final CombatUI ui)
	{
		combatUI = ui;
	}
}