package momime.server.worldupdates;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.server.MomSessionVariables;
import momime.server.fogofwar.KillUnitActionID;

/**
 * World update for killing a unit, whether that means really removing it permanently or just marking it as dead 
 */
public final class KillUnitUpdate implements WorldUpdate
{
	/** The unit to set to kill */
	private int unitURN;
	
	/** Method by which the unit is being killed; this controls whether the unit is fully removed, or just marked as dead and could be raised */
	private KillUnitActionID untransmittedAction;
	
	/**
	 * @return Enum indicating which kind of update this is
	 */
	@Override
	public final KindOfWorldUpdate getKindOfWorldUpdate ()
	{
		return KindOfWorldUpdate.KILL_UNIT;
	}
	
	/**
	 * @param o Other object to compare against
	 * @return Whether this and the other object hold the same values
	 */
	@Override
	public final boolean equals (final Object o)
	{
		final boolean e;
		if (o instanceof KillUnitUpdate)
			e = (getUnitURN () == ((KillUnitUpdate) o).getUnitURN ());
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
	 * @return The unit to set to kill
	 */
	public final int getUnitURN ()
	{
		return unitURN;
	}

	/**
	 * @param u The unit to set to kill
	 */
	public final void setUnitURN (final int u)
	{
		unitURN = u;
	}
	
	/**
	 * @return Method by which the unit is being killed; this controls whether the unit is fully removed, or just marked as dead and could be raised
	 */
	public final KillUnitActionID getUntransmittedAction ()
	{
		return untransmittedAction;
	}
	
	/**
	 * @param a Method by which the unit is being killed; this controls whether the unit is fully removed, or just marked as dead and could be raised
	 */
	public final void setUntransmittedAction (final KillUnitActionID a)
	{
		untransmittedAction = a;
	}
}