package momime.server.worldupdates;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.servertoclient.CancelCombatAreaEffectMessage;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.server.MomSessionVariables;
import momime.server.fogofwar.FogOfWarMidTurnVisibility;

/**
 * World update for removing a combat area effect
 */
public final class RemoveCombatAreaEffectUpdate implements WorldUpdate
{
	/** Memory CAE utils */
	private MemoryCombatAreaEffectUtils memoryCombatAreaEffectUtils;
	
	/** FOW visibility checks */
	private FogOfWarMidTurnVisibility fogOfWarMidTurnVisibility;
	
	/** The combat area effect to remove */
	private int combatAreaEffectURN;
	
	/**
	 * @return Enum indicating which kind of update this is
	 */
	@Override
	public final KindOfWorldUpdate getKindOfWorldUpdate ()
	{
		return KindOfWorldUpdate.REMOVE_COMBAT_AREA_EFFECT;
	}

	/**
	 * @param o Other object to compare against
	 * @return Whether this and the other object hold the same values
	 */
	@Override
	public final boolean equals (final Object o)
	{
		final boolean e;
		if (o instanceof RemoveCombatAreaEffectUpdate)
			e = (getCombatAreaEffectURN () == ((RemoveCombatAreaEffectUpdate) o).getCombatAreaEffectURN ());
		else
			e = false;
		
		return e;
	}
	
	/**
	 * @return String representation of class, for debug messages
	 */
	@Override
	public final String toString ()
	{
		return "Remove combat area effect URN " + getCombatAreaEffectURN ();
	}
	
	/**
	 * Processes this update
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether this update was processed and/or generated any further updates
	 * @throws IOException If there was a problem
	 * @throws JAXBException If there is a problem sending some message to the client
	 * @throws XMLStreamException If there is a problem sending some message to the client
	 */
	@Override
	public final WorldUpdateResult process (final MomSessionVariables mom) throws IOException, JAXBException, XMLStreamException
	{
		// Get the CAE's details before we remove it
		final MemoryCombatAreaEffect trueCAE = getMemoryCombatAreaEffectUtils ().findCombatAreaEffectURN (getCombatAreaEffectURN (),
			mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), "RemoveCombatAreaEffectUpdate");
		
		// Remove on server
		getMemoryCombatAreaEffectUtils ().removeCombatAreaEffectURN (getCombatAreaEffectURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect ());

		// Build the message ready to send it to whoever can see the CAE
		final CancelCombatAreaEffectMessage msg = new CancelCombatAreaEffectMessage ();
		msg.setCombatAreaEffectURN (getCombatAreaEffectURN ());

		// Check which players can see the CAE
		for (final PlayerServerDetails player : mom.getPlayers ())
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			if (getFogOfWarMidTurnVisibility ().canSeeCombatAreaEffectMidTurn (trueCAE, priv.getFogOfWar (),
				mom.getSessionDescription ().getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ()))
			{
				// Update player's memory on server
				getMemoryCombatAreaEffectUtils ().removeCombatAreaEffectURN (getCombatAreaEffectURN (), priv.getFogOfWarMemory ().getCombatAreaEffect ());

				// Update on client
				if (player.getPlayerDescription ().isHuman ())
					player.getConnection ().sendMessageToClient (msg);
			}
		}
		
		return WorldUpdateResult.DONE;
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
	 * @return FOW visibility checks
	 */
	public final FogOfWarMidTurnVisibility getFogOfWarMidTurnVisibility ()
	{
		return fogOfWarMidTurnVisibility;
	}

	/**
	 * @param vis FOW visibility checks
	 */
	public final void setFogOfWarMidTurnVisibility (final FogOfWarMidTurnVisibility vis)
	{
		fogOfWarMidTurnVisibility = vis;
	}
	
	/**
	 * @return The combat area effect to remove
	 */
	public final int getCombatAreaEffectURN ()
	{
		return combatAreaEffectURN;
	}

	/**
	 * @param c The combat area effect to remove
	 */
	public final void setCombatAreaEffectURN (final int c)
	{
		combatAreaEffectURN = c;
	}
}