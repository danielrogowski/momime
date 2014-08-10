package momime.common.utils;

import java.util.Iterator;
import java.util.List;

import momime.common.database.v0_9_5.CombatMapLayerID;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.messages.v0_9_5.MomCombatTile;
import momime.common.messages.v0_9_5.MomCombatTileLayer;
import momime.common.messages.v0_9_5.UnitStatusID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * Helper utils for dealing with combat maps
 */
public final class CombatMapUtilsImpl implements CombatMapUtils
{
	/** Class logger */
	private final Log log = LogFactory.getLog (CombatMapUtilsImpl.class);
	
	/**
	 * @param tile Tile to search
	 * @param layer Layer to look for
	 * @return combatTileTypeID at this layer, or null if this layer doesn't exist
	 */
	@Override
	public final String getCombatTileTypeForLayer (final MomCombatTile tile, final CombatMapLayerID layer)
	{
		// Search to see if layer already exists
		MomCombatTileLayer found = null;
		final Iterator<MomCombatTileLayer> layers = tile.getTileLayer ().iterator ();
		while ((found == null) && (layers.hasNext ()))
		{
			final MomCombatTileLayer thisLayer = layers.next ();
			if (thisLayer.getLayer () == layer)
				found = thisLayer;
		}
		
		// Did we find it?
		final String result;
		if (found != null)
			result = found.getCombatTileTypeID ();
		else
			result = null;
		
		return result;
	}
	
	/**
	 * Checks to see if the specified layer exists already for the specified tile; if it does then it updates it, if it doesn't then it adds it
	 * 
	 * @param tile Tile to update
	 * @param layer Layer to update
	 * @param combatTileTypeID New tile type to use for that layer
	 */
	@Override
	public final void setCombatTileTypeForLayer (final MomCombatTile tile, final CombatMapLayerID layer, final String combatTileTypeID)
	{
		// Search to see if layer already exists
		MomCombatTileLayer found = null;
		final Iterator<MomCombatTileLayer> layers = tile.getTileLayer ().iterator ();
		while ((found == null) && (layers.hasNext ()))
		{
			final MomCombatTileLayer thisLayer = layers.next ();
			if (thisLayer.getLayer () == layer)
				found = thisLayer;
		}
		
		// Did we find it?
		if (found != null)
			found.setCombatTileTypeID (combatTileTypeID);
		else
		{
			final MomCombatTileLayer newLayer = new MomCombatTileLayer ();
			newLayer.setLayer (layer);
			newLayer.setCombatTileTypeID (combatTileTypeID);
			
			tile.getTileLayer ().add (newLayer);
		}
	}

	/**
	 * Rechecks whether both sides in combat still have units left alive, and if so who the two players are.
	 * Delphi method used to be called RecheckCountsOfUnitsInCombat_MustLockUL, and returned a true/false for whether
	 * both values had been found.  Equivalent here is to call .bothFound () on the returned obj.
	 *  
	 * @param combatLocation Overland map coordinates where combat is taking place
	 * @param units List of known units
	 * @param players Players list
	 * @return Who the attacking and defending players are
	 * @throws PlayerNotFoundException If we determine the attacking or defending player ID, but that ID then can't be found in the players list
	 */
	@Override
	public final CombatPlayers determinePlayersInCombatFromLocation (final MapCoordinates3DEx combatLocation,
		final List<MemoryUnit> units, final List<? extends PlayerPublicDetails> players) throws PlayerNotFoundException
	{
		log.trace ("Entering determinePlayersInCombatFromLocation: " + combatLocation);
		
		Integer attackingPlayerID = null;
		Integer defendingPlayerID = null;
		
		// Stop as soon as we've found them both
		final Iterator<MemoryUnit> iter = units.iterator ();
		while ((iter.hasNext ()) && ((attackingPlayerID == null) || (defendingPlayerID == null)))
		{
			final MemoryUnit thisUnit = iter.next ();
			if ((thisUnit.getStatus () == UnitStatusID.ALIVE) && (combatLocation.equals (thisUnit.getCombatLocation ())) &&
				(thisUnit.getCombatPosition () != null) && (thisUnit.getCombatSide () != null))					
			{
				switch (thisUnit.getCombatSide ())
				{
					case ATTACKER:
						attackingPlayerID = thisUnit.getOwningPlayerID ();
						break;
				
					case DEFENDER:
						defendingPlayerID = thisUnit.getOwningPlayerID ();
						break;
				}
			}
		}
		
		final CombatPlayers result = new CombatPlayers
			((attackingPlayerID == null) ? null : MultiplayerSessionUtils.findPlayerWithID (players, attackingPlayerID, "determinePlayersInCombatFromLocation-A"),
			(defendingPlayerID == null) ? null : MultiplayerSessionUtils.findPlayerWithID (players, defendingPlayerID, "determinePlayersInCombatFromLocation-D"));
		log.trace ("Exiting determinePlayersInCombatFromLocation = " + attackingPlayerID + ", " + defendingPlayerID);
		
		return result;
	}
	
	/**
	 * @param playerID Player whose units to count
	 * @param combatLocation Combat units must be in
	 * @param units List of units
	 * @return Number of alive units belonging to this player at this location
	 */
	@Override
	public final int countPlayersAliveUnitsAtCombatLocation (final int playerID, final MapCoordinates3DEx combatLocation, final List<MemoryUnit> units)
	{
		log.trace ("Entering countPlayersAliveUnitsAtCombatLocation: Player ID " + playerID + ", " + combatLocation);
		
		int count = 0;
		for (final MemoryUnit thisUnit : units)
			if ((thisUnit.getOwningPlayerID () == playerID) && (combatLocation.equals (thisUnit.getCombatLocation ())) && (thisUnit.getStatus () == UnitStatusID.ALIVE))
				count++;			

		log.trace ("Exiting countPlayersAliveUnitsAtCombatLocation = " + count);
		return count;
	}
}