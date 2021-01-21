package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.MomClient;
import momime.client.ui.frames.CombatUI;
import momime.client.ui.frames.UnitInfoUI;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.servertoclient.CancelCombatAreaEffectMessage;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.UnitUtils;

/**
 * Server sends this to notify clients of cancelled CAEs, or those that have gone out of view.
 */
public final class CancelCombatAreaEffectMessageImpl extends CancelCombatAreaEffectMessage implements BaseServerToClientMessage
{
	/** Multiplayer client */
	private MomClient client;
	
	/** Combat UI */
	private CombatUI combatUI;
	
	/** Memory CAE utils */
	private MemoryCombatAreaEffectUtils memoryCombatAreaEffectUtils;
	
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
		// Grab the CAE before we remove it
		final MemoryCombatAreaEffect cae = getMemoryCombatAreaEffectUtils ().findCombatAreaEffectURN (getCombatAreaEffectURN (),
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (), "CancelCombatAreaEffectMessageImpl");

		// Remove it
		getMemoryCombatAreaEffectUtils ().removeCombatAreaEffectURN (getCombatAreaEffectURN (),
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect ());
		
		// If there's a combat in progress, the icon for this CAE might need to be added to it
		if (getCombatUI ().isVisible ())
			getCombatUI ().generateCombatAreaEffectIcons ();
		
		// If any open unit info screen are affected by this CAE, then redraw their attributes
		for (final UnitInfoUI unitInfo : getClient ().getUnitInfos ().values ())
			if (getUnitUtils ().doesCombatAreaEffectApplyToUnit (unitInfo.getUnit (), cae, getClient ().getClientDB ()))
				unitInfo.getUnitInfoPanel ().refreshUnitDetails ();
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

	/**
	 * @return Memory CAE utils
	 */
	public final MemoryCombatAreaEffectUtils getMemoryCombatAreaEffectUtils ()
	{
		return memoryCombatAreaEffectUtils;
	}

	/**
	 * @param utils Memory CAE utils
	 */
	public final void setMemoryCombatAreaEffectUtils (final MemoryCombatAreaEffectUtils utils)
	{
		memoryCombatAreaEffectUtils = utils;
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