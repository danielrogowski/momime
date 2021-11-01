package momime.common.utils;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.database.CombatMapLayerID;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomCombatTile;
import momime.common.messages.MomCombatTileLayer;
import momime.common.messages.UnitStatusID;

/**
 * Helper utils for dealing with combat maps
 */
public final class CombatMapUtilsImpl implements CombatMapUtils
{
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
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
	 * Rechecks whether both sides in combat still have units left alive, and if so who the two players are.
	 * Delphi method used to be called RecheckCountsOfUnitsInCombat_MustLockUL, and returned a true/false for whether
	 * both values had been found.  Equivalent here is to call .bothFound () on the returned obj.
	 *  
	 * @param combatLocation Overland map coordinates where combat is taking place
	 * @param units List of known units
	 * @param players Players list
	 * @param db Lookup lists built over the XML database
	 * @return Who the attacking and defending players are
	 * @throws PlayerNotFoundException If we determine the attacking or defending player ID, but that ID then can't be found in the players list
	 */
	@Override
	public final CombatPlayers determinePlayersInCombatFromLocation (final MapCoordinates3DEx combatLocation,
		final List<MemoryUnit> units, final List<? extends PlayerPublicDetails> players, final CommonDatabase db) throws PlayerNotFoundException
	{
		Integer attackingPlayerID = null;
		Integer defendingPlayerID = null;
		
		// Stop as soon as we've found them both
		final Iterator<MemoryUnit> iter = units.iterator ();
		while ((iter.hasNext ()) && ((attackingPlayerID == null) || (defendingPlayerID == null)))
		{
			final MemoryUnit thisUnit = iter.next ();
			if ((thisUnit.getStatus () == UnitStatusID.ALIVE) && (combatLocation.equals (thisUnit.getCombatLocation ())) &&
				(thisUnit.getCombatPosition () != null) && (thisUnit.getCombatSide () != null) && (thisUnit.getCombatHeading () != null) &&
				(!db.getUnitsThatMoveThroughOtherUnits ().contains (thisUnit.getUnitID ())))
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
			((attackingPlayerID == null) ? null : getMultiplayerSessionUtils ().findPlayerWithID (players, attackingPlayerID, "determinePlayersInCombatFromLocation-A"),
			(defendingPlayerID == null) ? null : getMultiplayerSessionUtils ().findPlayerWithID (players, defendingPlayerID, "determinePlayersInCombatFromLocation-D"));
		
		return result;
	}

	/**
	 * @param combatLocation Location where the combat is taking place
	 * @param combatPosition Location of the unit within the combat map
	 * @param combatMap Combat scenery
	 * @param trueBuildings True list of buildings
	 * @param db Lookup lists built over the XML database
	 * @return Whether the specified location is within city walls (if there even are any)
	 */
	@Override
	public final boolean isWithinCityWalls (final MapCoordinates3DEx combatLocation, final MapCoordinates2DEx combatPosition,
		final MapAreaOfCombatTiles combatMap, final List<MemoryBuilding> trueBuildings, final CommonDatabase db)
	{
		final boolean withinCityWalls;
		
		// First, the city actually has to have city walls
		if (getMemoryBuildingUtils ().findBuilding (trueBuildings, combatLocation, db.getCityWallsBuildingID ()) == null)
			withinCityWalls = false;
		else
		{
			// Get the specific tile where the unit is
			final MomCombatTile combatTile = combatMap.getRow ().get (combatPosition.getY ()).getCell ().get (combatPosition.getX ());
			
			// See if any of the layers have a tile that identifies this location as being within the city (this is flagged on road tiles)
			final List<String> cityTiles = db.getCombatTileType ().stream ().filter
				(t -> (t.isInsideCity () != null) && (t.isInsideCity ())).map (t -> t.getCombatTileTypeID ()).collect (Collectors.toList ());
			
			withinCityWalls = combatTile.getTileLayer ().stream ().anyMatch (l -> cityTiles.contains (l.getCombatTileTypeID ()));
		}
		
		return withinCityWalls;
	}

	/**
	 * @param combatLocation Location where the combat is taking place
	 * @param combatPosition Location of the unit within the combat map
	 * @param combatMap Combat scenery
	 * @param trueSpells True list of spells
	 * @param db Lookup lists built over the XML database
	 * @return Whether the specified location is within wall of darkness (if there even is a wall of darkness here)
	 */
	@Override
	public final boolean isWithinWallOfDarkness (final MapCoordinates3DEx combatLocation, final MapCoordinates2DEx combatPosition,
		final MapAreaOfCombatTiles combatMap, final List<MemoryMaintainedSpell> trueSpells, final CommonDatabase db)
	{
		final boolean withinWallOfDarkness;

		// First, the city actually has to have wall of darkness
		if (getMemoryMaintainedSpellUtils ().findMaintainedSpell (trueSpells, null, CommonDatabaseConstants.SPELL_ID_WALL_OF_DARKNESS,
			null, null, combatLocation, null) == null)
		
			withinWallOfDarkness = false;
		else
		{
			// Get the specific tile where the unit is
			final MomCombatTile combatTile = combatMap.getRow ().get (combatPosition.getY ()).getCell ().get (combatPosition.getX ());
			
			// See if any of the layers have a tile that identifies this location as being within the city (this is flagged on road tiles)
			final List<String> cityTiles = db.getCombatTileType ().stream ().filter
				(t -> (t.isInsideCity () != null) && (t.isInsideCity ())).map (t -> t.getCombatTileTypeID ()).collect (Collectors.toList ());
			
			withinWallOfDarkness = combatTile.getTileLayer ().stream ().anyMatch (l -> cityTiles.contains (l.getCombatTileTypeID ()));
		}
		
		return withinWallOfDarkness;
	}
	
	/**
	 * @return Session utils
	 */
	public final MultiplayerSessionUtils getMultiplayerSessionUtils ()
	{
		return multiplayerSessionUtils;
	}

	/**
	 * @param util Session utils
	 */
	public final void setMultiplayerSessionUtils (final MultiplayerSessionUtils util)
	{
		multiplayerSessionUtils = util;
	}

	/**
	 * @return Memory building utils
	 */
	public final MemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param utils Memory building utils
	 */
	public final void setMemoryBuildingUtils (final MemoryBuildingUtils utils)
	{
		memoryBuildingUtils = utils;
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