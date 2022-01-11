package momime.server.utils;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.database.CombatMapLayerID;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomCombatTile;
import momime.common.messages.MomCombatTileLayer;
import momime.common.messages.UnitStatusID;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.server.knowledge.CombatDetails;

/**
 * Methods dealing with combat maps that are only needed on the server
 */
public final class CombatMapServerUtilsImpl implements CombatMapServerUtils
{
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
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
	 * @param playerID Player whose units to count
	 * @param combatLocation Combat units must be in
	 * @param units List of units
	 * @param db Lookup lists built over the XML database
	 * @return Number of alive units belonging to this player at this location
	 */
	@Override
	public final int countPlayersAliveUnitsAtCombatLocation (final int playerID, final MapCoordinates3DEx combatLocation,
		final List<MemoryUnit> units, final CommonDatabase db)
	{
		int count = 0;
		for (final MemoryUnit thisUnit : units)
			if ((thisUnit.getOwningPlayerID () == playerID) && (combatLocation.equals (thisUnit.getCombatLocation ())) && (thisUnit.getCombatHeading () != null) &&
				(thisUnit.getStatus () == UnitStatusID.ALIVE) && (thisUnit.getCombatPosition () != null) && (thisUnit.getCombatSide () != null) &&
				(!db.getUnitsThatMoveThroughOtherUnits ().contains (thisUnit.getUnitID ())))
				
				count++;			

		return count;
	}
	
	/**
	 * @param combatLocation Location where the combat is taking place
	 * @param combatPosition Location of the unit within the combat map
	 * @param combatMap Combat scenery
	 * @param trueSpells True spell details held on server
	 * @param db Lookup lists built over the XML database
	 * @return Whether the specified location is within wall of fire (if there even is a wall of fire here)
	 */
	@Override
	public final boolean isWithinWallOfFire (final MapCoordinates3DEx combatLocation, final MapCoordinates2DEx combatPosition,
		final MapAreaOfCombatTiles combatMap, final List<MemoryMaintainedSpell> trueSpells, final CommonDatabase db)
	{
		final boolean withinWallOfFire;
		
		// First, the city actually has to have wall of fire
		if (getMemoryMaintainedSpellUtils ().findMaintainedSpell (trueSpells, null, CommonDatabaseConstants.SPELL_ID_WALL_OF_FIRE,
			null, null, combatLocation, null) == null)
			
			withinWallOfFire = false;
		else
		{
			// Get the specific tile where the unit is
			final MomCombatTile combatTile = combatMap.getRow ().get (combatPosition.getY ()).getCell ().get (combatPosition.getX ());
			
			// See if any of the layers have a tile that identifies this location as being within the city (this is flagged on road tiles)
			final List<String> cityTiles = db.getCombatTileType ().stream ().filter
				(t -> (t.isInsideCity () != null) && (t.isInsideCity ())).map (t -> t.getCombatTileTypeID ()).collect (Collectors.toList ());
			
			withinWallOfFire = combatTile.getTileLayer ().stream ().anyMatch (l -> cityTiles.contains (l.getCombatTileTypeID ()));
		}
		
		return withinWallOfFire;
	}

	/**
	 * @param combatDetails Combat details storage
	 * @param combatURN Unique identifier for the combat to look for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Requested combat
	 * @throws RecordNotFoundException If the combatURN does not exist
	 */
	@Override
	public final CombatDetails findCombatURN (final List<CombatDetails> combatDetails, final int combatURN, final String caller)
		throws RecordNotFoundException
	{
		return combatDetails.stream ().filter (c -> c.getCombatURN () == combatURN).findAny ().orElseThrow
			(() -> new RecordNotFoundException (CombatDetails.class, combatURN, caller));
	}

	/**
	 * @param combatDetails Combat details storage
	 * @param combatLocation Combat location to look for
	 * @return Requested combat if exists, or null if not found
	 */
	@Override
	public final CombatDetails findCombatByLocation (final List<CombatDetails> combatDetails, final MapCoordinates3DEx combatLocation)
	{
		return combatDetails.stream ().filter (c -> combatLocation.equals (c.getCombatLocation ())).findAny ().orElse (null);
	}

	/**
	 * @param combatDetails Combat details storage
	 * @param combatLocation Combat location to look for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Requested combat
	 * @throws RecordNotFoundException If there is no combat taking place at the requested location
	 */
	@Override
	public final CombatDetails findCombatByLocation (final List<CombatDetails> combatDetails, final MapCoordinates3DEx combatLocation, final String caller)
		throws RecordNotFoundException
	{
		return combatDetails.stream ().filter (c -> combatLocation.equals (c.getCombatLocation ())).findAny ().orElseThrow
			(() -> new RecordNotFoundException (CombatDetails.class, combatLocation.toString (), caller));
	}
	
	/**
	 * @return MemoryMaintainedSpell utils
	 */
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param spellUtils MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils spellUtils)
	{
		memoryMaintainedSpellUtils = spellUtils;
	}
}