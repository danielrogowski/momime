package momime.common.movement;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.utils.Holder;

import momime.common.MomException;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CombatMapLayerID;
import momime.common.database.CombatTileBorderBlocksMovementID;
import momime.common.database.CombatTileType;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.TileTypeEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomCombatTile;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitStatusID;
import momime.common.utils.CombatMapUtils;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;

/**
 * There's a lot of methods involved in calculating movement.  All the component methods are here, then the main front end methods are in UnitMovementImpl
 * so that they are kept independant of each other for unit tests.
 */
public final class MovementUtilsImpl implements MovementUtils
{
	/** Marks locations in the doubleMovementDistances array that we haven't checked yet */
	private final static int MOVEMENT_DISTANCE_NOT_YET_CHECKED = -1;

	/** Marks locations in the doubleMovementDistances array that we've proved that we cannot move to */
	private final static int MOVEMENT_DISTANCE_CANNOT_MOVE_HERE = -2;
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** MemoryGridCell utils */
	private MemoryGridCellUtils memoryGridCellUtils;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;

	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;

	/** Methods dealing with unit movement */
	private UnitMovement unitMovement;
	
	/** Combat map utils */
	private CombatMapUtils combatMapUtils;
	
	/**
	 * @param unitStack Which units are actually moving (may be more friendly units in the start tile that are choosing to stay where they are)
	 * @param unitStackSkills Collective list of skills of all units in the stack
	 * @param movingPlayerID The player who is trying to move here
	 * @param map The player who is trying to move here's knowledge
	 * @param players List of players in this session
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return Count of the number of free transport spaces at every map cell
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found
	 * @throws MomException If the list includes something other than MemoryUnits or ExpandedUnitDetails
	 */
	@Override
	public final int [] [] [] calculateCellTransportCapacity (final UnitStack unitStack, final Set<String> unitStackSkills, final int movingPlayerID, final FogOfWarMemory map,
		final List<? extends PlayerPublicDetails> players, final CoordinateSystem sys, final CommonDatabase db)
		throws PlayerNotFoundException, RecordNotFoundException, MomException 
	{
		// If its a transported movement, then by defintion the stack can move onto another transport if it wants to,
		// so don't need to make any special considerations for moving units onto a transport
		final int [] [] [] cellTransportCapacity;
		if (unitStack.getTransports ().size () > 0)
			cellTransportCapacity = null;
		else
		{
			// Find how much spare transport capacity we have on every cell of the map.
			// We add +capacity for every transport found, and -1 capacity for every unit that is on terrain impassable to itself (therefore must be in a transport).
			// Then any spaces left with 1 or higher value have spare space units could be loaded into.
			cellTransportCapacity = new int [sys.getDepth ()] [sys.getHeight ()] [sys.getWidth ()]; 
			for (final MemoryUnit thisUnit : map.getUnit ())
				if ((thisUnit.getOwningPlayerID () == movingPlayerID) && (thisUnit.getStatus () == UnitStatusID.ALIVE) && (thisUnit.getUnitLocation () != null))
				{
					final int x = thisUnit.getUnitLocation ().getX ();
					final int y = thisUnit.getUnitLocation ().getY ();
					final int z = thisUnit.getUnitLocation ().getZ ();
					
					final OverlandMapTerrainData terrainData = map.getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x).getTerrainData ();
					final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (thisUnit, null, null, null, players, map, db);
					
					// Count space granted by transports
					final Integer unitTransportCapacity = xu.getUnitDefinition ().getTransportCapacity ();
					if ((unitTransportCapacity != null) && (unitTransportCapacity > 0))
						cellTransportCapacity [z] [y] [x] = cellTransportCapacity [z] [y] [x] + unitTransportCapacity;
					
					// Count space taken up by units already in transports
					else
					{
						final String tileTypeID = getMemoryGridCellUtils ().convertNullTileTypeToFOW (terrainData, false);
						if (getUnitCalculations ().calculateDoubleMovementToEnterTileType (xu, unitStackSkills, tileTypeID, db) == null)
							cellTransportCapacity [z] [y] [x]--;
					}
				}			
		}
		
		return cellTransportCapacity;
	}

	/**
	 * @param unitStack Unit stack we are moving
	 * @param db Lookup lists built over the XML database
	 * @return Map indicating the doubled movement cost of entering every type of tile type for this unit stack
	 * @throws RecordNotFoundException If the definition of a spell that is cast on the unit cannot be found in the db
	 * @throws MomException If the list includes something other than MemoryUnits or ExpandedUnitDetails
	 */
	@Override
	public final Map<String, Integer> calculateDoubleMovementRatesForUnitStack (final List<ExpandedUnitDetails> unitStack,
		final CommonDatabase db) throws RecordNotFoundException, MomException
	{
		// Get list of all the skills that any unit in the stack has, in case any of them have path finding, wind walking, etc.
		final Set<String> unitStackSkills = getUnitCalculations ().listAllSkillsInUnitStack (unitStack);

		// Go through each tile type
		final Map<String, Integer> movementRates = new HashMap<String, Integer> ();
		for (final TileTypeEx tileType : db.getTileTypes ())
			if (!tileType.getTileTypeID ().equals (CommonDatabaseConstants.TILE_TYPE_FOG_OF_WAR_HAVE_SEEN))
			{
				Integer worstMovementRate = 0;

				// Check every unit - stop if we've found that terrain is impassable to someone
				final Iterator<ExpandedUnitDetails> unitIter = unitStack.iterator ();
				while ((worstMovementRate != null) && (unitIter.hasNext ()))
				{
					final ExpandedUnitDetails thisUnit = unitIter.next ();

					final Integer thisMovementRate = getUnitCalculations ().calculateDoubleMovementToEnterTileType (thisUnit, unitStackSkills, tileType.getTileTypeID (), db);
					if (thisMovementRate == null)
						worstMovementRate = null;
					else if (thisMovementRate > worstMovementRate)
						worstMovementRate = thisMovementRate;
				}

				// No point putting it into the map if it is impassable - HashMap.get returns null for keys not in the map anyway
				if (worstMovementRate != null)
					movementRates.put (tileType.getTileTypeID (), worstMovementRate);
			}

		return movementRates;
	}
	
	/**
	 * @param playerID Player whose units to count
	 * @param units Player's knowledge of all units
	 * @param sys Overland map coordinate system
	 * @return Count how many of that player's units are in every cell on the map
	 */
	@Override
	public final int [] [] [] countOurAliveUnitsAtEveryLocation (final int playerID, final List<MemoryUnit> units, final CoordinateSystem sys)
	{
		final int [] [] [] count = new int [sys.getDepth ()] [sys.getHeight ()] [sys.getWidth ()];
		for (final MemoryUnit thisUnit : units)
			if ((thisUnit.getOwningPlayerID () == playerID) && (thisUnit.getStatus () == UnitStatusID.ALIVE) && (thisUnit.getUnitLocation () != null))
				count [thisUnit.getUnitLocation ().getZ ()] [thisUnit.getUnitLocation ().getY ()] [thisUnit.getUnitLocation ().getX ()]++;

		return count;
	}

	/**
	 * Finds all places on the overland map our unit stack cannot go because either:
	 * 1) We already have units there, and number of units there + number of units in stack will be > 9
	 * 2) Its a Tower of Wizardry, and Planar Seal is cast (even our own)
	 * 3) Spell Ward, and we have summoned creatures of the corresponding type (even our own)
	 * 4) Someone else's Flying Fortress, and we have non-flying units
	 * 
	 * These are all pretty special circumstances that won't list out many coordinates.  What this doesn't include is locations
	 * where the terrain itself is impassable, or we'd end up listing every known water tile if its a stack of land units.
	 * 
	 * @param unitStack Which units are actually moving (may be more friendly units in the start tile that are choosing to stay where they are)
	 * @param movingPlayerID The player who is trying to move here
	 * @param ourUnitCountAtLocation Count how many of our units are in every cell on the map
	 * @param overlandMapCoordinateSystem Overland map coordinate system
	 * @param spells Known spells
	 * @param terrain Player knowledge of terrain
	 * @param db Lookup lists built over the XML database
	 * @return Set of all overland map locations this unit stack is blocked from entering for one of the above reasons
	 * @throws RecordNotFoundException If an expected data item can't be found
	 */
	@Override
	public final Set<MapCoordinates3DEx> determineBlockedLocations (final UnitStack unitStack, final int movingPlayerID,
		final int [] [] [] ourUnitCountAtLocation, final CoordinateSystem overlandMapCoordinateSystem,
		final List<MemoryMaintainedSpell> spells, final MapVolumeOfMemoryGridCells terrain, final CommonDatabase db)
		throws RecordNotFoundException
	{
		// What magic realm(s) are the units in the stack?
		final Set<String> unitStackMagicRealms = new HashSet<String> ();
		unitStack.getUnits ().forEach (xu -> unitStackMagicRealms.add (xu.getModifiedUnitMagicRealmLifeformType ().getPickID ()));
		unitStack.getTransports ().forEach (xu -> unitStackMagicRealms.add (xu.getModifiedUnitMagicRealmLifeformType ().getPickID ()));
		
		// Search for locations where some city spell effect stops some of the unit stack from entering
		final Set<String> blockingCitySpellEffectIDs = db.getCitySpellEffect ().stream ().filter
			(e -> (e.isBlockEntryByCreaturesOfRealm () != null) && (e.isBlockEntryByCreaturesOfRealm ()) &&
				(!Collections.disjoint (e.getProtectsAgainstSpellRealm (), unitStackMagicRealms))).map (e -> e.getCitySpellEffectID ()).collect (Collectors.toSet ());
		
		final Set<MapCoordinates3DEx> blockedLocations = spells.stream ().filter
			(s -> (s.getCityLocation () != null) && (blockingCitySpellEffectIDs.contains (s.getCitySpellEffectID ()))).map
			(s -> (MapCoordinates3DEx) s.getCityLocation ()).collect (Collectors.toSet ());

		// Can the whole unit stack all fly?
		final List<String> flightSkills = db.findCombatTileType
			(CommonDatabaseConstants.COMBAT_TILE_TYPE_CLOUD, "calculateDoubleMovementToEnterTile").getCombatTileTypeRequiresSkill ();
		final Holder<Boolean> allCanFly = new Holder<Boolean> (true);
		unitStack.getUnits ().forEach (xu ->
		{
			if (flightSkills.stream ().noneMatch (f -> xu.hasModifiedSkill (f)))
				allCanFly.setValue (false);
		});
		unitStack.getTransports ().forEach (xu ->
		{
			if (flightSkills.stream ().noneMatch (f -> xu.hasModifiedSkill (f)))
				allCanFly.setValue (false);
		});
		
		// Unless every single unit can fly, we're blocked from entering anywhere with Flying Fortress
		if (!allCanFly.getValue ())
			spells.stream ().filter (s -> (s.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_FLYING_FORTRESS)) && (s.getCastingPlayerID () != movingPlayerID)).map
				(s -> (MapCoordinates3DEx) s.getCityLocation ()).forEach (l -> blockedLocations.add (l));
		
		// Towers are impassable if Planar Seal is cast; also check numbers of units we have in each cell here too
		final boolean planarSeal = (getMemoryMaintainedSpellUtils ().findMaintainedSpell
			(spells, null, CommonDatabaseConstants.SPELL_ID_PLANAR_SEAL, null, null, null, null) != null);
				
		for (int z = 0; z < overlandMapCoordinateSystem.getDepth (); z++)
			for (int y = 0; y < overlandMapCoordinateSystem.getHeight (); y++)
				for (int x = 0; x < overlandMapCoordinateSystem.getWidth (); x++)
					
					if (ourUnitCountAtLocation [z] [y] [x] + unitStack.getTransports ().size () + unitStack.getUnits ().size () > CommonDatabaseConstants.MAX_UNITS_PER_MAP_CELL)
						blockedLocations.add (new MapCoordinates3DEx (x, y, z));
		
					else if (planarSeal)
					{
						final OverlandMapTerrainData terrainData = terrain.getPlane ().get (z).getRow ().get (y).getCell ().get (x).getTerrainData ();
						if (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (terrainData))
							blockedLocations.add (new MapCoordinates3DEx (x, y, z));
					}							
		
		return blockedLocations;
	}
	
	/**
	 * @param movingPlayerID The player who is trying to move
	 * @param spells Known spells
	 * @return All locations where we have an Earth Gate cast on a city
	 */
	@Override
	public final Set<MapCoordinates3DEx> findEarthGates (final int movingPlayerID, final List<MemoryMaintainedSpell> spells)
	{
		return spells.stream ().filter (s -> (s.getCastingPlayerID () == movingPlayerID) && (s.getSpellID ().equals
			(CommonDatabaseConstants.SPELL_ID_EARTH_GATE))).map (s -> (MapCoordinates3DEx) s.getCityLocation ()).collect
				(Collectors.toSet ());
	}
	
	/**
	 * @param movingPlayerID The player who is trying to move
	 * @param spells Known spells
	 * @return All locations where we have an Astral Gate cast on a city, unless Planar Seal is cast in which case we always get an empty set
	 */
	@Override
	public final Set<MapCoordinates2DEx> findAstralGates (final int movingPlayerID, final List<MemoryMaintainedSpell> spells)
	{
		final Set<MapCoordinates2DEx> astralGates;
		
		if (getMemoryMaintainedSpellUtils ().findMaintainedSpell
			(spells, null, CommonDatabaseConstants.SPELL_ID_PLANAR_SEAL, null, null, null, null) != null)
			
			astralGates = new HashSet<MapCoordinates2DEx> ();
		else
			astralGates = spells.stream ().filter (s -> (s.getCastingPlayerID () == movingPlayerID) && (s.getSpellID ().equals
				(CommonDatabaseConstants.SPELL_ID_ASTRAL_GATE))).map (s -> new MapCoordinates2DEx (s.getCityLocation ().getX (), s.getCityLocation ().getY ())).collect
					(Collectors.toSet ());
		
		return astralGates;
	}
	
	/**
	 * @param unitStack Which units are actually moving (may be more friendly units in the start tile that are choosing to stay where they are)
	 * @param unitStackSkills Collective list of skills of all units in the stack
	 * @param coords Where we are trying to move to
	 * @param cellTransportCapacity Number of free spaces on transports at each map cell
	 * @param doubleMovementRates Movement to enter each kind of overland map tile
	 * @param terrain Player knowledge of terrain
	 * @param db Lookup lists built over the XML database
	 * @return Double number of movement points it costs to enter this tile; null if the tile is impassable
	 */
	@Override
	public final Integer calculateDoubleMovementToEnterTile (final UnitStack unitStack, final Set<String> unitStackSkills, final MapCoordinates3DEx coords,
		final int [] [] [] cellTransportCapacity, final Map<String, Integer> doubleMovementRates,
		final MapVolumeOfMemoryGridCells terrain, final CommonDatabase db)
	{
		final OverlandMapTerrainData terrainData = terrain.getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
		
		Integer movementRate = doubleMovementRates.get (getMemoryGridCellUtils ().convertNullTileTypeToFOW (terrainData, true));
		
		// If the cell is otherwise impassable to us (i.e. land units trying to walk onto water) but there's enough space in a transport there, then allow it
		if ((movementRate == null) && (cellTransportCapacity != null) && (cellTransportCapacity [coords.getZ ()] [coords.getY ()] [coords.getX ()] > 0))
		{
			// Work out how many spaces we -need-
			// Can't do this up front because it varies depending on whether the terrain being moved to is impassable to each kind of unit in the stack
			int spaceRequired = 0;
			boolean impassableToTransport = false;
			for (final ExpandedUnitDetails thisUnit : unitStack.getUnits ())
			{															
				final boolean impassable = (getUnitCalculations ().calculateDoubleMovementToEnterTileType (thisUnit, unitStackSkills,
					getMemoryGridCellUtils ().convertNullTileTypeToFOW (terrainData, false), db) == null);
				
				// Count space granted by transports
				final Integer unitTransportCapacity = thisUnit.getUnitDefinition ().getTransportCapacity ();
				if ((unitTransportCapacity != null) && (unitTransportCapacity > 0))
				{
					if (impassable)
						impassableToTransport = true;
					else
						spaceRequired = spaceRequired - unitTransportCapacity;
				}
				
				// Count space taken up by units already in transports
				else if (impassable)									
					spaceRequired++;
			}							
			
			// If the cell is impassable to one of our transports then the free space is irrelevant, we just can't go there
			if ((!impassableToTransport) && (cellTransportCapacity [coords.getZ ()] [coords.getY ()] [coords.getX ()] >= spaceRequired))
				movementRate = 2;
		}
		
		return movementRate;
	}
	
	/**
	 * @param unitStack Which units are actually moving (may be more friendly units in the start tile that are choosing to stay where they are)
	 * @param unitStackSkills Collective list of skills of all units in the stack
	 * @param moveFrom Coordinates of the cell we are moving from
	 * @param movingPlayerID The player who is trying to move here
	 * @param doubleMovementRemaining The lowest movement remaining for any of the units that are moving
	 * @param blockedLocations Set of all overland map locations this unit stack is blocked from entering for any reasons other than impassable terrain
	 * @param earthGates Earth gates we can use
	 * @param astralGates Astral gates we can use
	 * @param cellTransportCapacity Count of the number of free transport spaces at every map cell
	 * @param doubleMovementRates Map indicating the doubled movement cost of entering every type of tile type for this unit stack
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param cellsLeftToCheck List of cells that still need to be checked (we add adjacent cells to the end of this list)
	 * @param overlandMapCoordinateSystem Overland map coordinate system
	 * @param mem The player who is trying to move here's knowledge
	 * @param db Lookup lists built over the XML database
	 */
	@Override
	public final void processOverlandMovementCell (final UnitStack unitStack, final Set<String> unitStackSkills,
		final MapCoordinates3DEx moveFrom, final int movingPlayerID, final int doubleMovementRemaining,
		final Set<MapCoordinates3DEx> blockedLocations, final Set<MapCoordinates3DEx> earthGates, final Set<MapCoordinates2DEx> astralGates,
		final int [] [] [] cellTransportCapacity, final Map<String, Integer> doubleMovementRates,
		final OverlandMovementCell [] [] [] moves, final List<MapCoordinates3DEx> cellsLeftToCheck,
		final CoordinateSystem overlandMapCoordinateSystem, final FogOfWarMemory mem, final CommonDatabase db)
	{
		final int doubleDistanceToHere = moves [moveFrom.getZ ()] [moveFrom.getY ()] [moveFrom.getX ()].getDoubleMovementDistance ();
		final int doubleMovementRemainingToHere = doubleMovementRemaining - doubleDistanceToHere;
		
		// Are we stood in a tower?
		int minPlane = moveFrom.getZ ();
		int maxPlane = moveFrom.getZ ();

		final OverlandMapTerrainData moveFromTerrain = mem.getMap ().getPlane ().get
			(moveFrom.getZ ()).getRow ().get (moveFrom.getY ()).getCell ().get (moveFrom.getX ()).getTerrainData ();
		if (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (moveFromTerrain))
		{
			minPlane = 0;
			maxPlane = overlandMapCoordinateSystem.getDepth () - 1;
		}
		
		// Adjacent moves
		for (int cellPlane = minPlane; cellPlane <= maxPlane; cellPlane++)
			for (int d = 1; d <= getCoordinateSystemUtils ().getMaxDirection (overlandMapCoordinateSystem.getCoordinateSystemType ()); d++)
			{
				final MapCoordinates3DEx moveTo = new MapCoordinates3DEx (moveFrom.getX (), moveFrom.getY (), cellPlane);
				if (getCoordinateSystemUtils ().move3DCoordinates (overlandMapCoordinateSystem, moveTo, d))
				{
					// If moving onto a tower then must set plane to be 0
					final OverlandMapTerrainData moveToTerrain = mem.getMap ().getPlane ().get
						(moveTo.getZ ()).getRow ().get (moveTo.getY ()).getCell ().get (moveTo.getX ()).getTerrainData ();
					if (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (moveToTerrain))
						moveTo.setZ (0);
					
					if (!blockedLocations.contains (moveTo))
						getUnitMovement ().considerPossibleMove (unitStack, unitStackSkills, moveFrom, OverlandMovementType.ADJACENT, d, moveTo, movingPlayerID,
							doubleDistanceToHere, doubleMovementRemainingToHere, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
				}
			}
		
		// Earth gates
		if (earthGates.contains (moveFrom))
			for (final MapCoordinates3DEx earthGate : earthGates)
				if ((!earthGate.equals (moveFrom)) && (earthGate.getZ () == moveFrom.getZ ()) && (!blockedLocations.contains (earthGate)))
					getUnitMovement ().considerPossibleMove (unitStack, unitStackSkills, moveFrom, OverlandMovementType.EARTH_GATE, 0, earthGate, movingPlayerID,
						doubleDistanceToHere, doubleMovementRemainingToHere, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		
		// Astral gates
		if (astralGates.contains (new MapCoordinates2DEx (moveFrom.getX (), moveFrom.getY ())))
		{
			final MapCoordinates3DEx coords = new MapCoordinates3DEx (moveFrom.getX (), moveFrom.getY (), 1 - moveFrom.getZ ());
			if (!blockedLocations.contains (coords))
				getUnitMovement ().considerPossibleMove (unitStack, unitStackSkills, moveFrom, OverlandMovementType.ASTRAL_GATE, 0, coords, movingPlayerID,
					doubleDistanceToHere, doubleMovementRemainingToHere, cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, mem, db);
		}
	}
		
	/**
	 * Flying units obviously ignore this although they still can't enter impassable terrain
	 * @param xu The unit that is moving; if passed in as null then can't do this check on specific movement skills
	 * @param tile Combat tile being entered
	 * @param db Lookup lists built over the XML database
	 * @return 2x movement points required to enter this tile; negative value indicates impassable; will never return zero
	 * @throws RecordNotFoundException If we counter a combatTileBorderID or combatTileTypeID that can't be found in the db
	 */
	@Override
	public final int calculateDoubleMovementToEnterCombatTile (final ExpandedUnitDetails xu, final MomCombatTile tile, final CommonDatabase db)
		throws RecordNotFoundException
	{
		int result = -1;		// Impassable
		
		if (!tile.isOffMapEdge ())
		{
			// Any types of wall here that block movement?  (not using iterator because there's going to be so few of these).
			// NB. You still cannot walk across corners of city walls even when they've been wrecked.
			boolean impassableFound = false;
			for (final String borderID : tile.getBorderID ())
				if (db.findCombatTileBorder (borderID, "calculateDoubleMovementToEnterCombatTile").getBlocksMovement () == CombatTileBorderBlocksMovementID.WHOLE_TILE_IMPASSABLE)
					impassableFound = true;
			
			// Check each layer for the first which specifies movement
			// This works in the opposite order than the Delphi code, here we check the lowest layer (terrain) first and overwrite the value with higher layers
			// The delphi code started with the highest layer and worked down, but skipping as soon as it got a non-zero value
			for (final CombatMapLayerID layer : CombatMapLayerID.values ())
				
				// If we found anything that's impassable on any layer, just stop - nothing on a higher layer can make it passable
				if (!impassableFound)
				{
					// Mud overrides anything else in the terrain layer, but movement rate can still be reduced by roads or set to impassable by buildings
					if ((layer == CombatMapLayerID.TERRAIN) && (tile.isMud ()))
						result = 1000;
					else
					{
						final String combatTileTypeID = getCombatMapUtils ().getCombatTileTypeForLayer (tile, layer);
						if (combatTileTypeID != null)		// layers are often not all populated
						{
							// If the tile requires specific kinds of movement, see if we have them
							final CombatTileType combatTileType = db.findCombatTileType (combatTileTypeID, "calculateDoubleMovementToEnterCombatTile");
							if ((xu != null) && (combatTileType.getCombatTileTypeRequiresSkill ().size () > 0) &&
								(combatTileType.getCombatTileTypeRequiresSkill ().stream ().noneMatch (s -> xu.hasModifiedSkill (s))))
								
								impassableFound = true;
							else
							{
								final Integer movement = combatTileType.getDoubleMovement ();
								if (movement != null)		// many tiles have no effect at all on movement, e.g. houses
								{
									if (movement < 0)
										impassableFound = true;
									else
										result = movement;
								}
							}
						}
					}
				}
			
			if (impassableFound)
				result = -1;
		}
		
		return result;
	}
	
	/**
	 * Adds all directions from the given location to the list of cells left to check for combat movement
	 * 
	 * @param moveFrom Combat tile we're moving from
	 * @param unitBeingMoved The unit moving in combat
	 * @param ignoresCombatTerrain True if the unit has a skill with the "ignoreCombatTerrain" flag
	 * @param cellsLeftToCheck List of combat tiles we still need to check movement from
	 * @param doubleMovementDistances Double the number of movement points it takes to move here, 0=free (enchanted road), negative=cannot reach
	 * @param movementDirections Trace of unit directions taken to reach here
	 * @param movementTypes Type of move (or lack of) for every location on the combat map (these correspond exactly to the X, move, attack, icons displayed in the client)
	 * @param ourUnits Array marking location of all of our units in the combat
	 * @param enemyUnits Array marking location of all enemy units in the combat; each element in the array is their combatActionID so we know which ones are flying
	 * @param borderTargetIDs List of tile borders that we can attack besides being able to target units; null if there are none
	 * @param combatMap The details of the combat terrain
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If we counter a combatTileBorderID or combatTileTypeID that can't be found in the db
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	@Override
	public final void processCombatMovementCell (final MapCoordinates2DEx moveFrom, final ExpandedUnitDetails unitBeingMoved, final boolean ignoresCombatTerrain,
		final List<MapCoordinates2DEx> cellsLeftToCheck, final int [] [] doubleMovementDistances, final int [] [] movementDirections,
		final CombatMovementType [] [] movementTypes, final boolean [] [] ourUnits, final String [] [] enemyUnits, final List<String> borderTargetIDs,
		final MapAreaOfCombatTiles combatMap, final CoordinateSystem combatMapCoordinateSystem, final CommonDatabase db)
		throws RecordNotFoundException, MomException
	{
		final int distance = doubleMovementDistances [moveFrom.getY ()] [moveFrom.getX ()];
		final int doubleMovementRemainingToHere = unitBeingMoved.getDoubleCombatMovesLeft () - distance;
		
		for (int d = 1; d <= getCoordinateSystemUtils ().getMaxDirection (combatMapCoordinateSystem.getCoordinateSystemType ()); d++)
		{
			final MapCoordinates2DEx moveTo = new MapCoordinates2DEx (moveFrom);
			if (getCoordinateSystemUtils ().move2DCoordinates (combatMapCoordinateSystem, moveTo, d))
				if (doubleMovementDistances [moveTo.getY ()] [moveTo.getX ()] >= MOVEMENT_DISTANCE_NOT_YET_CHECKED)
				{
					// This is a valid location on the map that we've either not visited before or that we've already found another path to
					// (in which case we still need to check it - we might have found a quicker path now)
					
					// Check if our type of unit can move here
					final MomCombatTile moveToTile = combatMap.getRow ().get (moveTo.getY ()).getCell ().get (moveTo.getX ());
					final int doubleMovementToEnterThisTile = calculateDoubleMovementToEnterCombatTile (unitBeingMoved, moveToTile, db);
					
					// Can we attack the tile itself?
					boolean canAttackTile = false;
					if ((!moveToTile.isWrecked ()) && (moveToTile.getBorderDirections () != null) && (moveToTile.getBorderDirections ().length () > 0) && (borderTargetIDs != null))
						for (final String borderID : moveToTile.getBorderID ())
							if (borderTargetIDs.contains (borderID))
								canAttackTile = true;
					
					// Our own units prevent us moving here - enemy units don't because by 'moving there' we'll attack them
					if (((doubleMovementToEnterThisTile < 0) && (!canAttackTile)) || (ourUnits [moveTo.getY ()] [moveTo.getX ()]))
					{
						// Can't move here
						doubleMovementDistances [moveTo.getY ()] [moveTo.getX ()] = MOVEMENT_DISTANCE_CANNOT_MOVE_HERE;
					}

					// Can we cross the border between the two tiles?
					// i.e. check there's not a stone wall in the exit from the first cell or in the entrance to the second cell
					else
					{
						final boolean canCrossBorder = ignoresCombatTerrain ||
							((getUnitCalculations ().okToCrossCombatTileBorder (combatMap, combatMapCoordinateSystem.getCoordinateSystemType (),
								moveFrom.getX (), moveFrom.getY (), d, db)) &&
							(getUnitCalculations ().okToCrossCombatTileBorder (combatMap, combatMapCoordinateSystem.getCoordinateSystemType (),
								moveTo.getX (), moveTo.getY (), getCoordinateSystemUtils ().normalizeDirection (combatMapCoordinateSystem.getCoordinateSystemType (), d+4), db)));
						
						if (canCrossBorder || canAttackTile)
						{
							// How much movement (total) will it cost us to get here
							
							// If we ignore terrain, then we only call calculateDoubleMovementToEnterCombatTile to check if the
							// tile is impassable or not - but if its not, then override whatever value we got back
							final int newDistance = distance + ((ignoresCombatTerrain || (doubleMovementToEnterThisTile < 0)) ? 2 : doubleMovementToEnterThisTile);
							
							// Is this better than the current value for this cell?
							if ((doubleMovementDistances [moveTo.getY ()] [moveTo.getX ()] < 0) || (newDistance < doubleMovementDistances [moveTo.getY ()] [moveTo.getX ()]))
							{
								// Record the new distance
								doubleMovementDistances [moveTo.getY ()] [moveTo.getX ()] = newDistance;
								movementDirections [moveTo.getY ()] [moveTo.getX ()] = d;
								final String enemyCombatActionID = canCrossBorder ? enemyUnits [moveTo.getY ()] [moveTo.getX ()] : null;
								
								if (doubleMovementRemainingToHere > 0)
								{
									// Is there an enemy in this square to attack?
									final CombatMovementType movementType;
									if ((enemyCombatActionID != null) || (canAttackTile))
									{
										// Can we attack the unit here?
										if (getUnitCalculations ().canMakeMeleeAttack (enemyCombatActionID, unitBeingMoved, db))
										{
											if (enemyCombatActionID != null)
												movementType = canAttackTile ? CombatMovementType.MELEE_UNIT_AND_WALL : CombatMovementType.MELEE_UNIT;
											else
												movementType = CombatMovementType.MELEE_WALL;
										}
										else
										{
											movementType = CombatMovementType.CANNOT_MOVE;
											doubleMovementDistances [moveTo.getY ()] [moveTo.getX ()] = MOVEMENT_DISTANCE_CANNOT_MOVE_HERE;
											movementDirections [moveTo.getY ()] [moveTo.getX ()] = 0;
										}
									}
									else
										movementType = CombatMovementType.MOVE;

									movementTypes [moveTo.getY ()] [moveTo.getX ()] = movementType;
								}
								
								// If there is an enemy here, don't check further squares
								if ((canCrossBorder) && (enemyCombatActionID == null) && (doubleMovementToEnterThisTile > 0))
								{
									// Log that we need to check every location branching off from here.
									// For the AI combat routine, we have to recurse right to the edge of the map - we can't just stop when we
									// run out of movement - otherwise the AI routines can't figure out how to walk across the board to a unit that is currently out of range.
									cellsLeftToCheck.add (moveTo);
								}
							}
						}
					}
				}
		}
	}
		
	/**
	 * @return expandUnitDetails method
	 */
	public final ExpandUnitDetails getExpandUnitDetails ()
	{
		return expandUnitDetails;
	}

	/**
	 * @param e expandUnitDetails method
	 */
	public final void setExpandUnitDetails (final ExpandUnitDetails e)
	{
		expandUnitDetails = e;
	}

	/**
	 * @return Unit calculations
	 */
	public final UnitCalculations getUnitCalculations ()
	{
		return unitCalculations;
	}

	/**
	 * @param calc Unit calculations
	 */
	public final void setUnitCalculations (final UnitCalculations calc)
	{
		unitCalculations = calc;
	}

	/**
	 * @return MemoryGridCell utils
	 */
	public final MemoryGridCellUtils getMemoryGridCellUtils ()
	{
		return memoryGridCellUtils;
	}

	/**
	 * @param utils MemoryGridCell utils
	 */
	public final void setMemoryGridCellUtils (final MemoryGridCellUtils utils)
	{
		memoryGridCellUtils = utils;
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

	/**
	 * @return Coordinate system utils
	 */
	public final CoordinateSystemUtils getCoordinateSystemUtils ()
	{
		return coordinateSystemUtils;
	}

	/**
	 * @param utils Coordinate system utils
	 */
	public final void setCoordinateSystemUtils (final CoordinateSystemUtils utils)
	{
		coordinateSystemUtils = utils;
	}

	/**
	 * @return Methods dealing with unit movement
	 */
	public final UnitMovement getUnitMovement ()
	{
		return unitMovement;
	}

	/**
	 * @param u Methods dealing with unit movement
	 */
	public final void setUnitMovement (final UnitMovement u)
	{
		unitMovement = u;
	}

	/**
	 * @return Combat map utils
	 */
	public final CombatMapUtils getCombatMapUtils ()
	{
		return combatMapUtils;
	}

	/**
	 * @param util Combat map utils
	 */
	public final void setCombatMapUtils (final CombatMapUtils util)
	{
		combatMapUtils = util;
	}
}