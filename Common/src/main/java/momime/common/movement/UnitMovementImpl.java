package momime.common.movement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomCombatTile;
import momime.common.messages.UnitStatusID;
import momime.common.utils.CombatMapUtils;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.UnitUtils;

/**
 * Methods dealing with unit movement
 */
public final class UnitMovementImpl implements UnitMovement
{
	/** Marks locations in the doubleMovementDistances array that we haven't checked yet */
	private final static int MOVEMENT_DISTANCE_NOT_YET_CHECKED = -1;
	
	/** Marks locations in the doubleMovementDistances array that we've proved that we cannot move to */
	final static int MOVEMENT_DISTANCE_CANNOT_MOVE_HERE = -2;
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** MemoryGridCell utils */
	private MemoryGridCellUtils memoryGridCellUtils;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Movement utils */
	private MovementUtils movementUtils;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Combat map utils */
	private CombatMapUtils combatMapUtils;
	
	/**
	 * Given a cell we can move from and to, checks if the destination cell is actually passable,
	 * if its a quicker route to it than we may have already found. 
	 * 
	 * @param unitStack Which units are actually moving (may be more friendly units in the start tile that are choosing to stay where they are)
	 * @param unitStackSkills Collective list of skills of all units in the stack
	 * @param moveFrom Coordinates of the cell we are moving from
	 * @param movementType How we are moving between the two cells
	 * @param direction Direction from the from to the to cell, only if movementType is ADJACENT
	 * @param moveTo Coordinates of the cell we can move to
	 * @param movingPlayerID The player who is trying to move here
	 * @param doubleDistanceToHere Amount of movement it took to reach moveFrom
	 * @param doubleMovementRemainingToHere Amount of movement we have remaining after moveFrom
	 * @param cellTransportCapacity Count of the number of free transport spaces at every map cell
	 * @param doubleMovementRates Map indicating the doubled movement cost of entering every type of tile type for this unit stack
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param cellsLeftToCheck List of cells that still need to be checked (we add adjacent cells to the end of this list)
	 * @param mem The player who is trying to move here's knowledge
	 * @param db Lookup lists built over the XML database
	 */
	@Override
	public final void considerPossibleMove (final UnitStack unitStack, final Set<String> unitStackSkills,
		final MapCoordinates3DEx moveFrom, final OverlandMovementType movementType, final int direction,
		final MapCoordinates3DEx moveTo, final int movingPlayerID, final int doubleDistanceToHere, final int doubleMovementRemainingToHere,
		final int [] [] [] cellTransportCapacity, final Map<String, Integer> doubleMovementRates,
		final OverlandMovementCell [] [] [] moves, final List<MapCoordinates3DEx> cellsLeftToCheck, final FogOfWarMemory mem, final CommonDatabase db)
	{
		// Don't bother rechecking if we can already move here for free since we know we can't improve on that
		// So do recheck if we've never found a route to here, or if we've found a route to here that takes non-zero moves to reach here
		OverlandMovementCell cell = moves [moveTo.getZ ()] [moveTo.getY ()] [moveTo.getX ()];
		if ((cell == null) || (cell.getDoubleMovementDistance () > 0))
		{
			// This is a valid location on the map that we've either not visited before or that we've already found another
			// path to (in which case we still need to check it - we might have found a quicker path now)
			// Check if our type of unit(s) can move here
			final Integer doubleMovementRateForThisTileType;
			if (cell != null)
				doubleMovementRateForThisTileType = cell.getDoubleMovementToEnterTile ();
			else
				doubleMovementRateForThisTileType = getMovementUtils ().calculateDoubleMovementToEnterTile
					(unitStack, unitStackSkills, moveTo, cellTransportCapacity, doubleMovementRates, mem.getMap (), db);
			
			if (doubleMovementRateForThisTileType == null)
			{
				// Can't move here
				final OverlandMovementCell impassable = new OverlandMovementCell ();
				impassable.setDoubleMovementToEnterTile (MOVEMENT_DISTANCE_CANNOT_MOVE_HERE);
				impassable.setDoubleMovementDistance (MOVEMENT_DISTANCE_CANNOT_MOVE_HERE);
				
				moves [moveTo.getZ ()] [moveTo.getY ()] [moveTo.getX ()] = impassable;
			}
			else
			{
				// How much movement (total) will it cost us to get here
				final int doubleNewDistanceToNewCoords = doubleDistanceToHere + doubleMovementRateForThisTileType;

				// Is this better than the current value for this cell?
				if ((cell == null) || (doubleNewDistanceToNewCoords < cell.getDoubleMovementDistance ()))
				{
					// Record the new distance
					if (cell == null)
					{
						cell = new OverlandMovementCell ();
						moves [moveTo.getZ ()] [moveTo.getY ()] [moveTo.getX ()] = cell;
					}
					
					cell.setMovementType (movementType);
					cell.setMovedFrom (moveFrom);
					cell.setDirection (direction);
					cell.setDoubleMovementToEnterTile (doubleMovementRateForThisTileType);
					cell.setDoubleMovementDistance (doubleNewDistanceToNewCoords);
					cell.setMoveToInOneTurn (doubleMovementRemainingToHere > 0);

					// Is this a square we have to stop at, i.e. one which contains enemy units?
					final boolean movingHereResultsInAttack = getUnitCalculations ().willMovingHereResultInAnAttackThatWeKnowAbout
						(moveTo.getX (), moveTo.getY (), moveTo.getZ (), movingPlayerID, mem, db);

					// Log that we need to check every location branching off from here
					if (!movingHereResultsInAttack)
						cellsLeftToCheck.add (moveTo);
				}
			}
		}
	}

	/**
	 * @param start Where the unit stack is standing and starting their move from
	 * @param movingPlayerID Owner of the unit stack
	 * @param unitStack Unit stack to move
	 * @param doubleMovementRemaining The lowest movement remaining for any of the units that are moving
	 * @param players List of players in this session
	 * @param overlandMapCoordinateSystem Overland map coordinate system
	 * @param mem The player who is trying to move here's knowledge
	 * @param db Lookup lists built over the XML database
	 * @return Array listing all cells we can reach and the paths to get there
	 * @throws PlayerNotFoundException If an expected player can't be found
	 * @throws RecordNotFoundException If an expected data item can't be found
	 * @throws MomException If there is a problem with any of the calculations
	 */
	@Override
	public final OverlandMovementCell [] [] [] calculateOverlandMovementDistances (final MapCoordinates3DEx start, final int movingPlayerID,
		final UnitStack unitStack, final int doubleMovementRemaining, final List<? extends PlayerPublicDetails> players,
		final CoordinateSystem overlandMapCoordinateSystem, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		final Set<String> unitStackSkills = getUnitCalculations ().listAllSkillsInUnitStack (unitStack.getUnits ());
		
		// Count of the number of free transport spaces at every map cell; these can make otherwise impassable terrain passable
		final int [] [] [] cellTransportCapacity = getMovementUtils ().calculateCellTransportCapacity
			(unitStack, unitStackSkills, movingPlayerID, mem, players, overlandMapCoordinateSystem, db);
		
		// Work out all the movement rates over all tile types of the unit stack.
		// If a transporting move, only the movement speed of the transports matters.
		
		// Not sure this is necessarily correct - see example testCreateUnitStack_TransportOnly - if there are flying/swimming units
		// moving alongside the transports but not inside them, then their movement rates should be considered as well?
		final Map<String, Integer> doubleMovementRates = getMovementUtils ().calculateDoubleMovementRatesForUnitStack
			((unitStack.getTransports ().size () > 0) ? unitStack.getTransports () : unitStack.getUnits (), db);

		// Count how many of OUR units are in every cell on the map - enemy units are fine, we'll just attack them :-)
		final int [] [] [] ourUnitCountAtLocation = getMovementUtils ().countOurAliveUnitsAtEveryLocation (movingPlayerID, mem.getUnit (), overlandMapCoordinateSystem);
		
		// Determine all the places we are blocked from entering for all reasons other than impassable terrain
		final Set<MapCoordinates3DEx> blockedLocations = getMovementUtils ().determineBlockedLocations
			(unitStack, movingPlayerID, ourUnitCountAtLocation, overlandMapCoordinateSystem, mem, db);
	
		// Find usable Earth Gates
		final Set<MapCoordinates3DEx> earthGates = getMovementUtils ().findEarthGates (movingPlayerID, mem.getMaintainedSpell ());
		
		// Find usable Astral Gates
		final Set<MapCoordinates2DEx> astralGates = getMovementUtils ().findAstralGates (movingPlayerID, mem.getMaintainedSpell ());
		
		// Initialize the map area
		final OverlandMovementCell [] [] [] moves = new OverlandMovementCell [overlandMapCoordinateSystem.getDepth ()] [overlandMapCoordinateSystem.getHeight ()] [overlandMapCoordinateSystem.getWidth ()];
		
		// We can move to where we start from for free
		final OverlandMovementCell startCell = new OverlandMovementCell ();
		startCell.setMovementType (OverlandMovementType.START);
		startCell.setMoveToInOneTurn (true);
		moves [start.getZ ()] [start.getY ()] [start.getX ()] = startCell;
	
		// Rather than iterating out distances from the centre, process rings around each location before proceeding to the next location
		// This is to prevent the situation in the original MoM where you are on Enchanced Road, hit 'Up' and the game decides to move you up-left and then right to get there
		final List<MapCoordinates3DEx> cellsLeftToCheck = new ArrayList<MapCoordinates3DEx> ();
		getMovementUtils ().processOverlandMovementCell (unitStack, unitStackSkills, start,
			movingPlayerID, doubleMovementRemaining, blockedLocations, earthGates, astralGates,
			cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, overlandMapCoordinateSystem, mem, db);

		// Keep going until there's nowhere left to check
		while (cellsLeftToCheck.size () > 0)
		{
			getMovementUtils ().processOverlandMovementCell (unitStack, unitStackSkills, cellsLeftToCheck.get (0),
				movingPlayerID, doubleMovementRemaining, blockedLocations, earthGates, astralGates,
				cellTransportCapacity, doubleMovementRates, moves, cellsLeftToCheck, overlandMapCoordinateSystem, mem, db);

			cellsLeftToCheck.remove (0);
		}
		
		// Before we return the array, null out any cells that were impassable
		for (int z = 0; z < overlandMapCoordinateSystem.getDepth (); z++)
			for (int y = 0; y < overlandMapCoordinateSystem.getHeight (); y++)
				for (int x = 0; x < overlandMapCoordinateSystem.getWidth (); x++)
				{
					final OverlandMovementCell move = moves [z] [y] [x];
					if ((move != null) && (move.getDoubleMovementDistance () == MOVEMENT_DISTANCE_CANNOT_MOVE_HERE))
						moves [z] [y] [x] = null;
				}
		
		return moves;
	}

	/**
	 * Calculates how many (doubled) movement points it will take to move from x, y to ever other location in the combat map whether we can move there or not.
	 * 
	 * MoM is a little weird with how movement works - providing you have even 1/2 move left, you can move anywhere, even somewhere
	 * which takes 3 movement to get to - this can happen in combat as well, especially combats in cities when units can walk on the roads.
	 * 
	 * Therefore knowing distances to each location is not enough - we need a separate boolean array
	 * to mark whether we can or cannot reach each location - this is set in MovementTypes.
	 * 
	 * @param doubleMovementDistances Double the number of movement points it takes to move here, 0=free (enchanted road), negative=cannot reach
	 * @param movementDirections Trace of unit directions taken to reach here
	 * @param movementTypes Type of move (or lack of) for every location on the combat map (these correspond exactly to the X, move, attack, icons displayed in the client)
	 * @param unitBeingMoved The unit moving in combat
	 * @param fogOfWarMemory Known overland terrain, units, buildings and so on
	 * @param combatMap The details of the combat terrain
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @param players Players list
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 */
	@Override
	public final void calculateCombatMovementDistances (final int [] [] doubleMovementDistances, final int [] [] movementDirections,
		final CombatMovementType [] [] movementTypes, final ExpandedUnitDetails unitBeingMoved, final FogOfWarMemory fogOfWarMemory,
		final MapAreaOfCombatTiles combatMap, final CoordinateSystem combatMapCoordinateSystem,
		final List<? extends PlayerPublicDetails> players, final CommonDatabase db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException
	{
		// Create other areas
		final boolean [] [] ourUnits = new boolean [combatMapCoordinateSystem.getHeight ()] [combatMapCoordinateSystem.getWidth ()];
		final String [] [] enemyUnits = new String [combatMapCoordinateSystem.getHeight ()] [combatMapCoordinateSystem.getWidth ()];
		
		// Initialize areas
		for (int y = 0; y < combatMapCoordinateSystem.getHeight (); y++)
			for (int x = 0; x < combatMapCoordinateSystem.getWidth (); x++)
			{
				doubleMovementDistances [y] [x] = MOVEMENT_DISTANCE_NOT_YET_CHECKED;
				movementTypes [y] [x] = CombatMovementType.CANNOT_MOVE;
				movementDirections [y] [x] = 0;
				ourUnits [y] [x] = false;
				enemyUnits [y] [x] = null;
			}
		
		// We know combatLocation from the unit being moved
		final MapCoordinates3DEx combatLocation = unitBeingMoved.getCombatLocation ();
		
		// Work this out once only
		final boolean ignoresCombatTerrain = unitBeingMoved.unitIgnoresCombatTerrain (db);
		
		// Mark locations of units on both sides (including the unit being moved)
		// Also make list of units the moving unit can personally see (if its invisible, we must have true sight to counter it, simply knowing where it is isn't enough)
		final List<ExpandedUnitDetails> directlyVisibleEnemyUnits = new ArrayList<ExpandedUnitDetails> (); 
		
		final List<ExpandedUnitDetails> unitsBeingMoved = new ArrayList<ExpandedUnitDetails> ();
		unitsBeingMoved.add (unitBeingMoved);
		
		// Vortexes can move anywhere, even directly onto other units
		if (!db.getUnitsThatMoveThroughOtherUnits ().contains (unitBeingMoved.getUnitID ()))
			for (final MemoryUnit thisUnit : fogOfWarMemory.getUnit ())
				if ((combatLocation.equals (thisUnit.getCombatLocation ())) && (thisUnit.getStatus () == UnitStatusID.ALIVE) &&
					(thisUnit.getCombatPosition () != null) && (thisUnit.getCombatSide () != null) && (thisUnit.getCombatHeading () != null))
				{
					// Note on owning vs controlling player ID - unitBeingMoved.getControllingPlayerID () is the player whose turn it is, who is controlling the unit, so this is fine.
					// But they don't want to attack their own units who might just be temporarily confused, equally if an enemy unit is confused and currently under our
					// control, we still want to kill it - ideally we confusee units and make them kill each other!  So this is why it is not xu.getControllingPlayerID ()
					final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (thisUnit, unitsBeingMoved, null, null, players, fogOfWarMemory, db);
					if ((thisUnit == unitBeingMoved.getMemoryUnit ()) || (xu.getOwningPlayerID () == unitBeingMoved.getControllingPlayerID ()))
						ourUnits [thisUnit.getCombatPosition ().getY ()] [thisUnit.getCombatPosition ().getX ()] = true;
					
					else if (getUnitUtils ().canSeeUnitInCombat (xu, unitBeingMoved.getControllingPlayerID (), players, fogOfWarMemory, db, combatMapCoordinateSystem))
					{
						enemyUnits [thisUnit.getCombatPosition ().getY ()] [thisUnit.getCombatPosition ().getX ()] = getUnitCalculations ().determineCombatActionID (xu, false, db);
						
						boolean visible = true;
						for (final String invisibilitySkillID : CommonDatabaseConstants.UNIT_SKILL_IDS_INVISIBILITY)
							if (xu.hasModifiedSkill (invisibilitySkillID))
								visible = false;
	
						if (visible)
							directlyVisibleEnemyUnits.add (xu);
					}
				}
		
		// If we can attack walls, then get the list of border targets from Disrupt Wall spell
		// Can only attack walls from the outside in, otherwise defenders inside the city would be able to attack their own walls
		final List<String> borderTargetIDs = ((unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_WALL_CRUSHER)) &&
			(!getCombatMapUtils ().isWithinCityWalls (combatLocation, unitBeingMoved.getCombatPosition (), combatMap, fogOfWarMemory.getBuilding (), db))) ?
					
			db.findSpell (CommonDatabaseConstants.SPELL_ID_DISRUPT_WALL, "calculateCombatMovementDistances").getSpellValidBorderTarget () : null;
		
		// We can move to where we start from for free
		doubleMovementDistances [unitBeingMoved.getCombatPosition ().getY ()] [unitBeingMoved.getCombatPosition ().getX ()] = 0;
		movementTypes [unitBeingMoved.getCombatPosition ().getY ()] [unitBeingMoved.getCombatPosition ().getX ()] = CombatMovementType.MOVE;
		
		// Rather than iterating out distances from the centre, process rings around each location before proceeding to the next location.
		// This is to prevent the situation in the original MoM where you are on Enchanced Road,
		// hit 'Up' and the game decides to move you up-left and then right to get there.
		final List<MapCoordinates2DEx> cellsLeftToCheck = new ArrayList<MapCoordinates2DEx> ();
		getMovementUtils ().processCombatMovementCell (unitBeingMoved.getCombatPosition (), unitBeingMoved, ignoresCombatTerrain, cellsLeftToCheck,
			doubleMovementDistances, movementDirections, movementTypes, ourUnits, enemyUnits, borderTargetIDs, combatMap, combatMapCoordinateSystem, db);
		
		// Keep going until there's nowhere left to check
		while (cellsLeftToCheck.size () > 0)
		{
			getMovementUtils ().processCombatMovementCell (cellsLeftToCheck.get (0), unitBeingMoved, ignoresCombatTerrain, cellsLeftToCheck,
				doubleMovementDistances, movementDirections, movementTypes, ourUnits, enemyUnits, borderTargetIDs, combatMap, combatMapCoordinateSystem, db);
			cellsLeftToCheck.remove (0);
		}
		
		// Now check if we can fire missile attacks at any enemies
		if (getUnitCalculations ().canMakeRangedAttack (unitBeingMoved))
		{
			// Are we able to range attack units inside wall of darkness?  We either have to have true sight, or be inside wall of darkness ourselves.
			// Note we'll get false here if there simply is no Wall of Darkness in this combat, but then will get false when we check target units too.
			boolean attackTargetsInsideWallOfDarkness = getCombatMapUtils ().isWithinWallOfDarkness (combatLocation,
				unitBeingMoved.getCombatPosition (), combatMap, fogOfWarMemory.getMaintainedSpell (), db);
			
			for (final String trueSightSkillID : CommonDatabaseConstants.UNIT_SKILL_IDS_TRUE_SIGHT)
				if (unitBeingMoved.hasModifiedSkill (trueSightSkillID))
					attackTargetsInsideWallOfDarkness = true;
			
			// Now each visible unit is targetable
			for (final ExpandedUnitDetails xu : directlyVisibleEnemyUnits)
				if ((attackTargetsInsideWallOfDarkness) || (!getCombatMapUtils ().isWithinWallOfDarkness (combatLocation,
					xu.getCombatPosition (), combatMap, fogOfWarMemory.getMaintainedSpell (), db)))
				{
					final int x = xu.getCombatPosition ().getX ();
					final int y = xu.getCombatPosition ().getY ();
					
					// If the unit is invisible, we have to have True Sight / Illusions Immunity to be able to make a ranged attack against it.
					// Simply being able to see it, or even standing right next to it, isn't enough (expandUnitDetails above dealt with this).
					
					// Firing a missle weapon always uses up all of our movement so mark this for the sake of it - although MovementDistances
					// isn't actually used to reduce the movement a unit has left in this fashion
					movementTypes [y] [x] = CombatMovementType.RANGED_UNIT;
					doubleMovementDistances [y] [x] = 999;
				}
			
			// Can also ranged attack wall segments.
			// The wall of darkness is on the outside of the stone walls, so they're protected by it and we can only range attack the stone walls
			// if we have true sight, are inside the city hitting the stone walls from the other side, or there is no wall of darkness in effect.
			if ((borderTargetIDs != null) && ((attackTargetsInsideWallOfDarkness) || (getMemoryMaintainedSpellUtils ().findMaintainedSpell
				(fogOfWarMemory.getMaintainedSpell (), null, CommonDatabaseConstants.SPELL_ID_WALL_OF_DARKNESS, null, null, combatLocation, null) == null)))
				
				for (int y = 0; y < combatMapCoordinateSystem.getHeight (); y++)
					for (int x = 0; x < combatMapCoordinateSystem.getWidth (); x++)
					{
						final MomCombatTile moveToTile = combatMap.getRow ().get (y).getCell ().get (x);
						
						boolean canAttackTile = false;
						if ((!moveToTile.isWrecked ()) && (moveToTile.getBorderDirections () != null) && (moveToTile.getBorderDirections ().length () > 0))
							for (final String borderID : moveToTile.getBorderID ())
								if (borderTargetIDs.contains (borderID))
									canAttackTile = true;
						
						if (canAttackTile)
						{
							if (movementTypes [y] [x] == CombatMovementType.RANGED_UNIT)
								movementTypes [y] [x] = CombatMovementType.RANGED_UNIT_AND_WALL;
							else
								movementTypes [y] [x] = CombatMovementType.RANGED_WALL;
							
							doubleMovementDistances [y] [x] = 999;
						}
					}
		}
		
		// If unit has teleporting, it can move anywhere as long as the tile is passable
		// This does mean we could end up trying to teleport onto an invisible enemy unit
		if ((unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_TELEPORT)) ||
			(unitBeingMoved.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_MERGING)))
			
			for (int y = 0; y < combatMapCoordinateSystem.getHeight (); y++)
				for (int x = 0; x < combatMapCoordinateSystem.getWidth (); x++)
				{
					if ((movementTypes [y] [x] == CombatMovementType.CANNOT_MOVE) ||
						((movementTypes [y] [x] == CombatMovementType.MOVE) && (doubleMovementDistances [y] [x] > 2)))
					{
						final MomCombatTile moveToTile = combatMap.getRow ().get (y).getCell ().get (x);
						final int doubleMovementToEnterThisTile = getMovementUtils ().calculateDoubleMovementToEnterCombatTile (unitBeingMoved, moveToTile, db);
						
						// Our own units prevent us moving here - so do enemy units, we cannot move onto them, only next to them
						if ((doubleMovementToEnterThisTile < 0) || (ourUnits [y] [x]) || (enemyUnits [y] [x] != null))
						{
							// Can't move here
						}
						else
						{
							movementTypes [y] [x] = CombatMovementType.TELEPORT;
							doubleMovementDistances [y] [x] = 2;
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
	 * @return Movement utils
	 */
	public final MovementUtils getMovementUtils ()
	{
		return movementUtils;
	}

	/**
	 * @param u Movement utils
	 */
	public final void setMovementUtils (final MovementUtils u)
	{
		movementUtils = u;
	}

	/**
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param util Unit utils
	 */
	public final void setUnitUtils (final UnitUtils util)
	{
		unitUtils = util;
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