package momime.server.worldupdates;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerResourceCalculations;

/**
 * World update for recalculating per turn production amounts
 */
public final class RecalculateProductionUpdate implements WorldUpdate
{
	/** Resource calculations */
	private ServerResourceCalculations serverResourceCalculations;
	
	/** The player to recalculate production for */
	private int playerID;
	
	/**
	 * @return Enum indicating which kind of update this is
	 */
	@Override
	public final KindOfWorldUpdate getKindOfWorldUpdate ()
	{
		return KindOfWorldUpdate.RECALCULATE_PRODUCTION;
	}
	
	/**
	 * @param o Other object to compare against
	 * @return Whether this and the other object hold the same values
	 */
	@Override
	public final boolean equals (final Object o)
	{
		final boolean e;
		if (o instanceof RecalculateProductionUpdate)
			e = (getPlayerID () == ((RecalculateProductionUpdate) o).getPlayerID ());
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
		return "Recalculate production for player ID " + getPlayerID ();
	}
	
	/**
	 * Processes this update
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether this update was processed and/or generated any further updates
	 * @throws JAXBException If there is a problem sending some message to the client
	 * @throws XMLStreamException If there is a problem sending some message to the client
	 * @throws RecordNotFoundException If we find a game element (unit, building or so on) that we can't find the definition for in the DB
	 * @throws PlayerNotFoundException If we can't find the player who owns a game element
	 * @throws MomException If there are any issues with data or calculation logic
	 */
	@Override
	public final WorldUpdateResult process (final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		getServerResourceCalculations ().recalculateGlobalProductionValues (playerID, false, mom);
		return WorldUpdateResult.DONE;
	}

	/**
	 * @return Resource calculations
	 */
	public final ServerResourceCalculations getServerResourceCalculations ()
	{
		return serverResourceCalculations;
	}

	/**
	 * @param calc Resource calculations
	 */
	public final void setServerResourceCalculations (final ServerResourceCalculations calc)
	{
		serverResourceCalculations = calc;
	}
	
	/**
	 * @return The player to recalculate production for
	 */
	public final int getPlayerID ()
	{
		return playerID;
	}

	/**
	 * @param p The player to recalculate production for
	 */
	public final void setPlayerID (final int p)
	{
		playerID = p;
	}
}