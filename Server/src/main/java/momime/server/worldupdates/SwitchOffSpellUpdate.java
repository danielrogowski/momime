package momime.server.worldupdates;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

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
	 * Processes this update
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return True if this update generated any further updates (and hence the manager must resort the list)
	 * @throws IOException If there was a problem
	 * @throws JAXBException If there is a problem sending some message to the client
	 * @throws XMLStreamException If there is a problem sending some message to the client
	 */
	@Override
	public final boolean process (final MomSessionVariables mom) throws IOException, JAXBException, XMLStreamException
	{
		return false;
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