package momime.server.worldupdates;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.server.MomSessionVariables;

/**
 * World update for removing a combat area effect
 */
public final class RemoveCombatAreaEffectUpdate implements WorldUpdate
{
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
		return WorldUpdateResult.DONE;
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