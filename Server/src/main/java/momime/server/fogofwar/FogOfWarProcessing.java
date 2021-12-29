package momime.server.fogofwar;

import javax.xml.stream.XMLStreamException;

import com.ndg.map.CoordinateSystem;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MapVolumeOfFogOfWarStates;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.server.MomSessionVariables;

/**
 * This contains the methods that recheck what areas of the map a specific player can see, and depending which areas
 * have come into sight, or are going out of sight, and depending on the chosen FOW settings on the new game form,
 * works out all the terrain, cities, buildings, units, and so on that need to be updated or removed from the player's memory
 * and builds the appropriate update message to inform the client of those changes
 *
 * i.e. methods for when the true values remain the same but the visible area changes
 */
public interface FogOfWarProcessing
{
	/**
	 * Marks that we can see all cells within a particular radius
	 * @param fogOfWarArea Player's fog of war area
	 * @param trueTerrain True overland map terrain
	 * @param sys Overland map coordinate system
	 * @param x X coordinate of map cell to update
	 * @param y Y coordinate of map cell to update
	 * @param plane Plane of map cell to update
	 * @param radius Visible radius (negative = do nothing, 0 = this cell only, 1 = 1 ring around this cell, and so on)
	 */
	public void canSeeRadius (final MapVolumeOfFogOfWarStates fogOfWarArea, final MapVolumeOfMemoryGridCells trueTerrain,
		final CoordinateSystem sys, final int x, final int y, final int plane, final int radius);
	
	/**
	 * This routine handles when the area that a player can see changes; it:
	 * 1) Checks what the player can now see
	 * 2) Compares the area against what the player could see before
	 * 3) Checks which map cells, buildings, spells, CAEs and units the player can either now see that they couldn't see before, or now can't see that they could see before
	 * 4) Updates the server copy of their memory accordingly
	 * 5) If a human player, sends messages to the client to update their memory there accordingly
	 *
	 * Note this *doesn't* need to worry about "what if the map cell, list of buildings, etc. has changed since last turn" - those
	 * types of changes are dealt with by all the methods in FogOfWarMidTurnChanges - this only needs to deal with
	 * working out updates when the visible area changes, but the true values remain the same
	 *
	 * @param player The player whose FOW we are recalculating
	 * @param triggeredFrom What caused the change in visible area - this is only used for debug messages on the client
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void updateAndSendFogOfWar (final PlayerServerDetails player, final String triggeredFrom, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException;
}