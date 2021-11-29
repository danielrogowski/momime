package momime.server.worldupdates;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.server.MomSessionVariables;

/**
 * World update for switching off a maintained spell
 */
public final class SwitchOffSpellUpdate implements WorldUpdate
{
	/** The spell to switch off */
	private int spellURN;
	
	/**
	 * @return Enum indicating which kind of update this is
	 */
	@Override
	public final KindOfWorldUpdate getKindOfWorldUpdate ()
	{
		return KindOfWorldUpdate.SWITCH_OFF_SPELL;
	}
	
	/**
	 * @param o Other object to compare against
	 * @return Whether this and the other object hold the same values
	 */
	@Override
	public final boolean equals (final Object o)
	{
		final boolean e;
		if (o instanceof SwitchOffSpellUpdate)
			e = (getSpellURN () == ((SwitchOffSpellUpdate) o).getSpellURN ());
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
		return "Switch off spell URN " + getSpellURN ();
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
		throw new MomException (toString () + " not yet impelemented");
	}

	/**
	 * @return The spell to switch off
	 */
	public final int getSpellURN ()
	{
		return spellURN;
	}

	/**
	 * @param s The spell to switch off
	 */
	public final void setSpellURN (final int s)
	{
		spellURN = s;
	}
}