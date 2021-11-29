package momime.server.worldupdates;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;

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
public final class WorldUpdatesImpl implements WorldUpdates
{
	/** List of pending updates */
	private final List<WorldUpdate> updates = new ArrayList<WorldUpdate> ();
	
	/** Factory for creating world update objects */
	private WorldUpdateFactory worldUpdateFactory;
	
	/**
	 * @param update Update to add to the list
	 * @return Whether the update was added; will return false if its a duplicate update already found to be in the list
	 */
	final boolean add (final WorldUpdate update)
	{
		final boolean added = !updates.contains (update);
		if (added)
			updates.add (update);
		
		return added;
	}
	
	/**
	 * @param unitURN The unit to set to kill
	 * @param untransmittedAction Method by which the unit is being killed; this controls whether the unit is fully removed, or just marked as dead and could be raised
	 * @return Whether the update was added; will return false if its a duplicate update already found to be in the list
	 */
	@Override
	public final boolean killUnit (final int unitURN, final KillUnitActionID untransmittedAction)
	{
		final KillUnitUpdate update = getWorldUpdateFactory ().createKillUnitUpdate ();
		update.setUnitURN (unitURN);
		update.setUntransmittedAction (untransmittedAction);
		
		return add (update);
	}
	
	/**
	 * @param combatAreaEffectURN The combat area effect to remove
	 * @return Whether the update was added; will return false if its a duplicate update already found to be in the list
	 */
	@Override
	public final boolean removeCombatAreaEffect (final int combatAreaEffectURN)
	{
		final RemoveCombatAreaEffectUpdate update = getWorldUpdateFactory ().createRemoveCombatAreaEffectUpdate ();
		update.setCombatAreaEffectURN (combatAreaEffectURN);
		
		return add (update);
	}
	
	/**
	 * @param spellURN The spell to remove
	 * @return Whether the update was added; will return false if its a duplicate update already found to be in the list
	 */
	@Override
	public final boolean switchOffSpell (final int spellURN)
	{
		final SwitchOffSpellUpdate update = getWorldUpdateFactory ().createSwitchOffSpellUpdate ();
		update.setSpellURN (spellURN);
		
		return add (update);
	}

	/**
	 * @param mapLocation The map location to check
	 * @return Whether the update was added; will return false if its a duplicate update already found to be in the list
	 */
	@Override
	public final boolean recheckTransportCapacity (final MapCoordinates3DEx mapLocation)
	{
		final RecheckTransportCapacityUpdate update = getWorldUpdateFactory ().createRecheckTransportCapacityUpdate ();
		update.setMapLocation (mapLocation);
		
		return add (update);
	}
	
	/**
	 * @param cityLocation The map location to check
	 * @return Whether the update was added; will return false if its a duplicate update already found to be in the list
	 */
	@Override
	public final boolean recalculateCity (final MapCoordinates3DEx cityLocation)
	{
		final RecalculateCityUpdate update = getWorldUpdateFactory ().createRecalculateCityUpdate ();
		update.setCityLocation (cityLocation);
		
		return add (update);
	}
	
	/**
	 * Processes all world updates in the update list
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws IOException If there was a problem
	 * @throws JAXBException If there is a problem sending some message to the client
	 * @throws XMLStreamException If there is a problem sending some message to the client
	 */
	@Override
	public final void process (final MomSessionVariables mom) throws IOException, JAXBException, XMLStreamException
	{
		boolean resortList = true;
		while (updates.size () > 0)
		{
			if (resortList)
			{
				updates.sort (new WorldUpdateComparator ());
				resortList = false;
			}
			
			final WorldUpdate update = updates.get (0);
			updates.remove (0);
			
			if (update.process (mom))
				resortList = true;
		}
	}

	/**
	 * @return Factory for creating world update objects
	 */
	public final WorldUpdateFactory getWorldUpdateFactory ()
	{
		return worldUpdateFactory;
	}

	/**
	 * @param f Factory for creating world update objects
	 */
	public final void setWorldUpdateFactory (final WorldUpdateFactory f)
	{
		worldUpdateFactory = f;
	}
}