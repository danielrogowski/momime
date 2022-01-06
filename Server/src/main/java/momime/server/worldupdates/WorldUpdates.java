package momime.server.worldupdates;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;

import jakarta.xml.bind.JAXBException;
import momime.server.MomSessionVariables;
import momime.server.fogofwar.KillUnitActionID;

/**
 * Manages the server updating its true copy of the game world, and the knock on effects of such updates.
 * 
 * Callers will call one or more of the "add" methods to register the updates they wish to make and then call process.
 * Process will sort the updates into the correct order and process them one at a time, and any update may add
 * other updates into the list, which themselves may add further updates, and process will keep resorting them and
 * keep processing them until there's no updates left.
 * 
 * Each session (MomSessionThread / MomSessionVariables) has its own copy of this class managing the update list for that session.
 */
public interface WorldUpdates
{
	/**
	 * @param unitURN The unit to set to kill
	 * @param untransmittedAction Method by which the unit is being killed; this controls whether the unit is fully removed, or just marked as dead and could be raised
	 * @return Whether the update was added; will return false if its a duplicate update already found to be in the list
	 */
	public boolean killUnit (final int unitURN, final KillUnitActionID untransmittedAction);
	
	/**
	 * @param combatAreaEffectURN The combat area effect to remove
	 * @return Whether the update was added; will return false if its a duplicate update already found to be in the list
	 */
	public boolean removeCombatAreaEffect (final int combatAreaEffectURN);
	
	/**
	 * @param spellURN The spell to remove
	 * @return Whether the update was added; will return false if its a duplicate update already found to be in the list
	 */
	public boolean switchOffSpell (final int spellURN);

	/**
	 * @param mapLocation The map location to check
	 * @return Whether the update was added; will return false if its a duplicate update already found to be in the list
	 */
	public boolean recheckTransportCapacity (final MapCoordinates3DEx mapLocation);
	
	/**
	 * @param cityLocation The map location to check
	 * @return Whether the update was added; will return false if its a duplicate update already found to be in the list
	 */
	public boolean recalculateCity (final MapCoordinates3DEx cityLocation);
	
	/**
	 * @param playerID The player to recalculate production for
	 * @return Whether the update was added; will return false if its a duplicate update already found to be in the list
	 */
	public boolean recalculateProduction (final int playerID);

	/**
	 * @param playerID The player to recalculate visible area for
	 * @return Whether the update was added; will return false if its a duplicate update already found to be in the list
	 */
	public boolean recalculateFogOfWar (final int playerID);
	
	/**
	 * Processes all world updates in the update list
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether any of the updates processed included killing a unit
	 * @throws JAXBException If there is a problem sending some message to the client
	 * @throws XMLStreamException If there is a problem sending some message to the client
	 * @throws IOException If there is another kind of problem
	 */
	public boolean process (final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;
}