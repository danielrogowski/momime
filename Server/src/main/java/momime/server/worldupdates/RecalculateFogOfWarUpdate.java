package momime.server.worldupdates;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.server.MomSessionVariables;
import momime.server.fogofwar.FogOfWarProcessing;

/**
 * World update for recalculating the area a player can see
 */
public final class RecalculateFogOfWarUpdate implements WorldUpdate
{
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;
	
	/** Main FOW update routine */
	private FogOfWarProcessing fogOfWarProcessing;
	
	/** The player to recalculate fog of war for */
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
		if (o instanceof RecalculateFogOfWarUpdate)
			e = (getPlayerID () == ((RecalculateFogOfWarUpdate) o).getPlayerID ());
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
		return "Recalculate FOW for player ID " + getPlayerID ();
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
		final PlayerServerDetails castingPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), getPlayerID (), "RecalculateFogOfWarUpdate");
		getFogOfWarProcessing ().updateAndSendFogOfWar (castingPlayer, "RecalculateFogOfWarUpdate", mom);
		
		return WorldUpdateResult.DONE;
	}

	/**
	 * @return Server only helper methods for dealing with players in a session
	 */
	public final MultiplayerSessionServerUtils getMultiplayerSessionServerUtils ()
	{
		return multiplayerSessionServerUtils;
	}

	/**
	 * @param obj Server only helper methods for dealing with players in a session
	 */
	public final void setMultiplayerSessionServerUtils (final MultiplayerSessionServerUtils obj)
	{
		multiplayerSessionServerUtils = obj;
	}
	
	/**
	 * @return Main FOW update routine
	 */
	public final FogOfWarProcessing getFogOfWarProcessing ()
	{
		return fogOfWarProcessing;
	}

	/**
	 * @param obj Main FOW update routine
	 */
	public final void setFogOfWarProcessing (final FogOfWarProcessing obj)
	{
		fogOfWarProcessing = obj;
	}
	
	/**
	 * @return The player to recalculate fog of war  for
	 */
	public final int getPlayerID ()
	{
		return playerID;
	}

	/**
	 * @param p The player to recalculate fog of war  for
	 */
	public final void setPlayerID (final int p)
	{
		playerID = p;
	}
}