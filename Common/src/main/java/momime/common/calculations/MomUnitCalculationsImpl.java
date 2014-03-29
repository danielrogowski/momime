package momime.common.calculations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.CombatMapLayerID;
import momime.common.database.v0_9_4.CombatTileBorder;
import momime.common.database.v0_9_4.CombatTileBorderBlocksMovementID;
import momime.common.database.v0_9_4.RangedAttackType;
import momime.common.database.v0_9_4.Unit;
import momime.common.database.v0_9_4.UnitHasSkill;
import momime.common.messages.CombatMapCoordinatesEx;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.v0_9_4.AvailableUnit;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MapAreaOfCombatTiles;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.MemoryCombatAreaEffect;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomCombatTile;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.common.messages.v0_9_4.PlayerPick;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.common.utils.CombatMapUtils;
import momime.common.utils.MomUnitAttributeComponent;
import momime.common.utils.MomUnitAttributePositiveNegative;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.UnitUtils;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemType;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.SquareMapDirection;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * Common calculations pertaining to units
 */
public final class MomUnitCalculationsImpl implements MomUnitCalculations
{
	/** Class logger */
	private final Logger log = Logger.getLogger (MomUnitCalculationsImpl.class.getName ());
	
	/** Initial state where each combat map tile hasn't been checked yet */ 
	private final static int MOVEMENT_DISTANCE_NOT_YET_CHECKED = -1;
	
	/** Proved that we cannot move here */
	private final static int MOVEMENT_DISTANCE_IMPASSABLE = -2;
	
	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/** Combat map utils */
	private CombatMapUtils combatMapUtils;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/**
	 * @param map Our knowledge of the surrounding terrain
	 * @param buildings Pre-locked buildings list
	 * @param cityLocation Location of the city the unit is being constructed at
	 * @param picks Picks of the player who owns the city
	 * @param overlandMapCoordinateSystem Coordinate system for traversing overland map
	 * @param db Lookup lists built over the XML database
	 * @return Weapon grade that the unit that we build here will have
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 */
	@Override
	public final int calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort
		(final List<MemoryBuilding> buildings, final MapVolumeOfMemoryGridCells map, final OverlandMapCoordinatesEx cityLocation,
		final List<PlayerPick> picks, final CoordinateSystem overlandMapCoordinateSystem, final CommonDatabase db) throws RecordNotFoundException
	{
		log.entering (MomUnitCalculationsImpl.class.getName (), "calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort", cityLocation);

		// First look for a building that grants magical weapons, i.e. an Alchemists' Guild
		int bestWeaponGrade = 0;
		for (final MemoryBuilding thisBuilding : buildings)
			if (thisBuilding.getCityLocation ().equals (cityLocation))
			{
				final Integer weaponGradeFromBuilding = db.findBuilding (thisBuilding.getBuildingID (), "calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort").getBuildingMagicWeapons ();
				if ((weaponGradeFromBuilding != null) && (weaponGradeFromBuilding > bestWeaponGrade))
					bestWeaponGrade = weaponGradeFromBuilding;
			}

		// Check surrounding tiles, i.e. look for Mithril or Adamantium Ore
		// We can only use these if we found a building that granted some level of magic weapons
		if (bestWeaponGrade > 0)
		{
			final OverlandMapCoordinatesEx coords = new OverlandMapCoordinatesEx ();
			coords.setX (cityLocation.getX ());
			coords.setY (cityLocation.getY ());
			coords.setZ (cityLocation.getZ ());

			for (final SquareMapDirection direction : MomCityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
			{
				if (getCoordinateSystemUtils ().moveCoordinates (overlandMapCoordinateSystem, coords, direction.getDirectionID ()))
				{
					final OverlandMapTerrainData terrainData = map.getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
					if ((terrainData != null) && (terrainData.getMapFeatureID () != null))
					{
						final Integer featureMagicWeapons = db.findMapFeature (terrainData.getMapFeatureID (), "calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort").getFeatureMagicWeapons ();
						if ((featureMagicWeapons != null) && (featureMagicWeapons > bestWeaponGrade))
							bestWeaponGrade = featureMagicWeapons;
					}
				}
			}
		}

		// Check if the wizard has any retorts which give magical weapons, i.e. Alchemy
		final int weaponGradeFromPicks = getPlayerPickUtils ().getHighestWeaponGradeGrantedByPicks (picks, db);
		if (weaponGradeFromPicks > bestWeaponGrade)
			bestWeaponGrade = weaponGradeFromPicks;

		log.exiting (MomUnitCalculationsImpl.class.getName (), "calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort", bestWeaponGrade);
		return bestWeaponGrade;
	}

	/**
	 * Flying units obviously ignore this although they still can't enter impassable terrain
	 * @param tile Combat tile being entered
	 * @param db Lookup lists built over the XML database
	 * @return 2x movement points required to enter this tile; negative value indicates impassable; will never return zero
	 * @throws RecordNotFoundException If we counter a combatTileBorderID or combatTileTypeID that can't be found in the db
	 */
	@Override
	public final int calculateDoubleMovementToEnterCombatTile (final MomCombatTile tile, final CommonDatabase db)
		throws RecordNotFoundException
	{
		int result = -1;		// Impassable
		
		if (!tile.isOffMapEdge ())
		{
			// Any types of wall here that block movement?  (not using iterator because there's going to be so few of these)
			boolean impassableBorderFound = false;
			for (final String borderID : tile.getBorderID ())
				if (db.findCombatTileBorder (borderID, "calculateDoubleMovementToEnterCombatTile").getBlocksMovement () == CombatTileBorderBlocksMovementID.WHOLE_TILE_IMPASSABLE)
					impassableBorderFound = true;
			
			if (!impassableBorderFound)
			{
				// Check each layer for the first which specifies movement
				// This works in the opposite order than the Delphi code, here we check the lowest layer (terrain) first and overwrite the value with higher layers
				// The delphi code started with the highest layer and worked down, but skipping as soon as it got a non-zero value
				for (final CombatMapLayerID layer : CombatMapLayerID.values ())
				{
					final String combatTileTypeID = getCombatMapUtils ().getCombatTileTypeForLayer (tile, layer);
					if (combatTileTypeID != null)		// layers are often not all populated
					{
						final Integer movement = db.findCombatTileType (combatTileTypeID, "calculateDoubleMovementToEnterCombatTile").getDoubleMovement ();
						if (movement != null)		// many tiles have no effect at all on movement, e.g. houses
							result = movement;
					}
				}
			}
		}
		
		return result;
	}

	/**
	 * @param unit Unit we want to check
	 * @param skills List of skills the unit has, either just unit.getUnitHasSkill () or can pre-merge with spell skill list by calling mergeSpellEffectsIntoSkillList
	 * @param players Players list
	 * @param spells Known spells
	 * @param combatAreaEffects Known combat area effects
	 * @param db Lookup lists built over the XML database
	 * @return How much ranged ammo this unit has when fully loaded
	 * @throws RecordNotFoundException If the unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	@Override
	public final int calculateFullRangedAttackAmmo (final AvailableUnit unit, final List<UnitHasSkill> skills, final List<? extends PlayerPublicDetails> players,
		final List<MemoryMaintainedSpell> spells, final List<MemoryCombatAreaEffect> combatAreaEffects, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		return getUnitUtils ().getModifiedSkillValue (unit, skills, CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_RANGED_ATTACK_AMMO, players, spells, combatAreaEffects, db);
	}

	/**
	 * @param unit Unit we want to check
	 * @param skills List of skills the unit has, either just unit.getUnitHasSkill () or can pre-merge with spell skill list by calling mergeSpellEffectsIntoSkillList
	 * @param players Players list
	 * @param spells Known spells
	 * @param combatAreaEffects Known combat area effects
	 * @param db Lookup lists built over the XML database
	 * @return How much mana the unit has total, before any is spent in combat
	 * @throws RecordNotFoundException If the unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	@Override
	public final int calculateManaTotal (final AvailableUnit unit, final List<UnitHasSkill> skills, final List<? extends PlayerPublicDetails> players,
		final List<MemoryMaintainedSpell> spells, final List<MemoryCombatAreaEffect> combatAreaEffects, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		// Unit caster skill is easy, this directly says how many MP the unit has
		int total = getUnitUtils ().getModifiedSkillValue (unit, skills, CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_CASTER_UNIT, players, spells, combatAreaEffects, db);
		
		// The hero caster skill is a bit more of a pain, since we get more mana at higher experience levels
		int heroSkillValue = getUnitUtils ().getModifiedSkillValue (unit, skills, CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_CASTER_HERO, players, spells, combatAreaEffects, db);
		if (heroSkillValue > 0)
		{
			final int expLevel = getUnitUtils ().getExperienceLevel (unit, true, players, combatAreaEffects, db).getLevelNumber ();
			heroSkillValue = (heroSkillValue * 5 * (expLevel+1)) / 2;
			
			if (total < 0)
				total = heroSkillValue;
			else
				total = total + heroSkillValue;
		}
		
		return total;
	}

	/**
	 * Initializes any values on the unit at the start of a combat
	 * NB. Available units can never expend ranged attack ammo or use mana, but storing these values keeps avoids the need for the
	 * methods to use the Fog of War memory to look for spell effects that might increase ammo or mana
	 * 
	 * @param unit Unit we want to give ammo+mana to
	 * @param players Players list
	 * @param spells Known spells
	 * @param combatAreaEffects Known combat area effects
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If the unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	@Override
	public final void giveUnitFullRangedAmmoAndMana (final MemoryUnit unit, final List<? extends PlayerPublicDetails> players,
		final List<MemoryMaintainedSpell> spells, final List<MemoryCombatAreaEffect> combatAreaEffects, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		final UnitHasSkillMergedList mergedSkills = getUnitUtils ().mergeSpellEffectsIntoSkillList (spells, unit);
		
		// Now set the values
		unit.setRangedAttackAmmo (calculateFullRangedAttackAmmo (unit, mergedSkills, players, spells, combatAreaEffects, db));
		unit.setManaRemaining (calculateManaTotal (unit, mergedSkills, players, spells, combatAreaEffects, db));
	}
	
	/**
	 * First figure will take full damage before the second figure takes any damage
	 * 
	 * @param unit Unit to calculate figure count for
	 * @param players Players list
	 * @param spells Known spells
	 * @param combatAreaEffects Known combat area effects
	 * @param db Lookup lists built over the XML database
	 * @return Number of figures left alive in this unit
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 */
	@Override
	public final int calculateAliveFigureCount (final MemoryUnit unit, final List<? extends PlayerPublicDetails> players,
		final List<MemoryMaintainedSpell> spells, final List<MemoryCombatAreaEffect> combatAreaEffects, final CommonDatabase db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException
	{
		int figures = getUnitUtils ().getFullFigureCount (db.findUnit (unit.getUnitID (), "calculateAliveFigureCount")) -
				
			// Take off 1 for each full set of HP the unit has taken in damage
			(unit.getDamageTaken () / getUnitUtils ().getModifiedAttributeValue (unit, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_HIT_POINTS,
				MomUnitAttributeComponent.ALL, MomUnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db));
		
		// Protect against weird results
		if (figures < 0)
			figures = 0;
		
		return figures;
	}
	
	/**
	 * Of course available units can never lose hitpoints, however we still need to define this so the unit info screen can use
	 * it and draw darkened hearts for 'real' units who have taken damage.
	 * 
	 * @param unit Unit to calculate HP for
	 * @param players Players list
	 * @param spells Known spells
	 * @param combatAreaEffects Known combat area effects
	 * @param db Lookup lists built over the XML database
	 * @return How many hit points the first figure in this unit has left
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 */
	@Override
	public final int calculateHitPointsRemainingOfFirstFigure (final AvailableUnit unit, final List<? extends PlayerPublicDetails> players,
		final List<MemoryMaintainedSpell> spells, final List<MemoryCombatAreaEffect> combatAreaEffects, final CommonDatabase db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException
	{
		final int hitPointsPerFigure = getUnitUtils ().getModifiedAttributeValue (unit, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_HIT_POINTS,
			MomUnitAttributeComponent.ALL, MomUnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db);
		
		// Work out how much damage the first figure has taken
		final int damageTaken;
		if (unit instanceof MemoryUnit)
			damageTaken = ((MemoryUnit) unit).getDamageTaken ();
		else
			damageTaken = 0;
			
		final int firstFigureDamageTaken = damageTaken % hitPointsPerFigure;
		
		// Then from that work out how many hit points the first figure has left
		return hitPointsPerFigure - firstFigureDamageTaken;
	}
	
	/**
	 * This isn't as straightforward as it sounds, we either need dedicated ranged attack ammo (which can be phys or magic ranged attacks)
	 * or caster units can spend mana to fire ranged attacks, but only magical ranged attacks
	 * 
	 * @param unit Unit to calculate for
	 * @param players Players list
	 * @param spells Known spells
	 * @param combatAreaEffects Known combat area effects
	 * @param db Lookup lists built over the XML database
	 * @return Whether the unit can make a ranged attack in combat and has ammo to do so
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 */
	@Override
	public final boolean canMakeRangedAttack (final MemoryUnit unit, final List<? extends PlayerPublicDetails> players,
		final List<MemoryMaintainedSpell> spells, final List<MemoryCombatAreaEffect> combatAreaEffects, final CommonDatabase db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException
	{
		final boolean result;
		
		// First we have to actually have a ranged attack
		if (getUnitUtils ().getModifiedAttributeValue (unit, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_RANGED_ATTACK,
			MomUnitAttributeComponent.ALL, MomUnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db) <= 0)
			
			result = false;
		
		// If we have ranged attack ammo left then this is easy
		else if (unit.getRangedAttackAmmo () > 0)
			result = true;
		
		// If we don't have enough spare mana then we can't use it to fire with
		else if (unit.getManaRemaining () < 3)
			result = false;
		
		// We have to have a ranged attack ID to check
		else
		{
			// We have spare mana to fire but first we have to prove that our type of attack is magical - we can't use Mana to fire a bow!
			final Unit unitDef = db.findUnit (unit.getUnitID (), "canMakeRangedAttack");
			if (unitDef.getRangedAttackType () == null)
				result = false;
			else
			{
				final RangedAttackType rat = db.findRangedAttackType (unitDef.getRangedAttackType (), "canMakeRangedAttack");
				result = (rat.getMagicRealmID () != null);
			}
		}
		
		return result;
	}
	
	/**
	 * 
	 * @param combatMap Combat map units are moving around
	 * @param combatMapCoordinateSystemType Coordinate system type used by combat maps
	 * @param x X coordinate of combat tile we're moving from
	 * @param y Y coordinate of combat tile we're moving from
	 * @param d Direction we're trying to move
	 * @param db Lookup lists built over the XML database
	 * @return Whether we can cross the specified tile border
	 * @throws RecordNotFoundException If the tile has a combat tile border ID that doesn't exist
	 */
	final boolean okToCrossCombatTileBorder (final MapAreaOfCombatTiles combatMap, final CoordinateSystemType combatMapCoordinateSystemType,
		final int x, final int y, final int d, final CommonDatabase db) throws RecordNotFoundException
	{
		boolean ok;
		
		// Quick check, if there's no border at all then must be OK
		final MomCombatTile tile = combatMap.getRow ().get (y).getCell ().get (x);
		if (tile.getBorderDirections () == null)
			ok = true;
		
		// So there is a border - check if it includes the requested direction.
		// Have to check +1/-1 of the requested direction so that flat walls also stop moving in diagonals past the wall.
		else if ((!tile.getBorderDirections ().contains (new Integer (d).toString ())) &&
					(!tile.getBorderDirections ().contains (new Integer (getCoordinateSystemUtils ().normalizeDirection (combatMapCoordinateSystemType, d+1)).toString ())) &&
					(!tile.getBorderDirections ().contains (new Integer (getCoordinateSystemUtils ().normalizeDirection (combatMapCoordinateSystemType, d-1)).toString ())))
					
			ok = true;
		else
		{
			// So there is a border in the requested direction - but is it a type of border that actually blocks movement?
			// (Wall of fire/darkness don't)
			ok = true;
			final Iterator<String> iter = tile.getBorderID ().iterator ();
			while ((ok) && (iter.hasNext ()))
			{
				final String borderID = iter.next ();
				final CombatTileBorder border = db.findCombatTileBorder (borderID, "okToCrossCombatTileBorder");
				if (border.getBlocksMovement () == CombatTileBorderBlocksMovementID.CANNOT_CROSS_SPECIFIED_BORDERS)
					ok = false;
			}
		}
		
		return ok;
	}
	
	/**
	 * Adds all directions from the given location to the list of cells left to check for combat movement
	 * 
	 * @param moveFrom Combat tile we're moving from
	 * @param unitBeingMoved The unit moving in combat
	 * @param cellsLeftToCheck List of combat tiles we still need to check movement from
	 * @param doubleMovementDistances Double the number of movement points it takes to move here, 0=free (enchanted road), negative=cannot reach
	 * @param movementDirections Trace of unit directions taken to reach here
	 * @param movementTypes Type of move (or lack of) for every location on the combat map (these correspond exactly to the X, move, attack, icons displayed in the client)
	 * @param ourUnits Array marking location of all of our units in the combat
	 * @param enemyUnits Array marking location of all enemy units in the combat
	 * @param combatMap The details of the combat terrain
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If we counter a combatTileBorderID or combatTileTypeID that can't be found in the db
	 */
	final void processCell (final CombatMapCoordinatesEx moveFrom, final MemoryUnit unitBeingMoved, final List<CombatMapCoordinatesEx> cellsLeftToCheck,
		final int [] [] doubleMovementDistances, final int [] [] movementDirections, final CombatMoveType [] [] movementTypes,
		final boolean [] [] ourUnits, final boolean [] [] enemyUnits,
		final MapAreaOfCombatTiles combatMap, final CoordinateSystem combatMapCoordinateSystem, final CommonDatabase db)
		throws RecordNotFoundException
	{
		final int distance = doubleMovementDistances [moveFrom.getY ()] [moveFrom.getX ()];
		final int doubleMovementRemainingToHere = unitBeingMoved.getDoubleCombatMovesLeft () - distance;
		
		for (int d = 1; d <= getCoordinateSystemUtils ().getMaxDirection (combatMapCoordinateSystem.getCoordinateSystemType ()); d++)
		{
			final CombatMapCoordinatesEx moveTo = new CombatMapCoordinatesEx ();
			moveTo.setX (moveFrom.getX ());
			moveTo.setY (moveFrom.getY ());
			
			if (getCoordinateSystemUtils ().moveCoordinates (combatMapCoordinateSystem, moveTo, d))
				if (doubleMovementDistances [moveTo.getY ()] [moveTo.getX ()] >= MOVEMENT_DISTANCE_NOT_YET_CHECKED)
				{
					// This is a valid location on the map that we've either not visited before or that we've already found another path to
					// (in which case we still need to check it - we might have found a quicker path now)
					
					// Check if our type of unit can move here
					final MomCombatTile moveToTile = combatMap.getRow ().get (moveTo.getY ()).getCell ().get (moveTo.getX ());
					final int doubleMovementToEnterThisTile = calculateDoubleMovementToEnterCombatTile (moveToTile, db);
					
					// Our own units prevent us moving here - enemy units don't because by 'moving there' we'll attack them
					if ((doubleMovementToEnterThisTile < 0) || (ourUnits [moveTo.getY ()] [moveTo.getX ()]))
					{
						// Can't move here
						doubleMovementDistances [moveTo.getY ()] [moveTo.getX ()] = MOVEMENT_DISTANCE_IMPASSABLE;
					}

					// Can we cross the border between the two tiles?
					// i.e. check there's not a stone wall in the exit from the first cell or in the entrance to the second cell
					else if ((okToCrossCombatTileBorder (combatMap, combatMapCoordinateSystem.getCoordinateSystemType (), moveFrom.getX (), moveFrom.getY (), d, db)) &&
						(okToCrossCombatTileBorder (combatMap, combatMapCoordinateSystem.getCoordinateSystemType (), moveTo.getX (), moveTo.getY (),
							getCoordinateSystemUtils ().normalizeDirection (combatMapCoordinateSystem.getCoordinateSystemType (), d+4), db)))
					{
						// How much movement (total) will it cost us to get here
						final int newDistance = distance + doubleMovementToEnterThisTile;
						
						// Is this better than the current value for this cell?
						if ((doubleMovementDistances [moveTo.getY ()] [moveTo.getX ()] < 0) || (newDistance < doubleMovementDistances [moveTo.getY ()] [moveTo.getX ()]))
						{
							// Record the new distance
							doubleMovementDistances [moveTo.getY ()] [moveTo.getX ()] = newDistance;
							movementDirections [moveTo.getY ()] [moveTo.getX ()] = d;
							
							if (doubleMovementRemainingToHere > 0)
							{
								// Is there an enemy in this square to attack?
								if (enemyUnits [moveTo.getY ()] [moveTo.getX ()])
									movementTypes [moveTo.getY ()] [moveTo.getX ()] = CombatMoveType.MELEE;
								else
									movementTypes [moveTo.getY ()] [moveTo.getX ()] = CombatMoveType.MOVE;
							}
							
							// If there is an enemy here, don't check further squares
							if (!enemyUnits [moveTo.getY ()] [moveTo.getX ()])
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
		final CombatMoveType [] [] movementTypes, final MemoryUnit unitBeingMoved, final FogOfWarMemory fogOfWarMemory,
		final MapAreaOfCombatTiles combatMap, final CoordinateSystem combatMapCoordinateSystem,
		final List<? extends PlayerPublicDetails> players, final CommonDatabase db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.entering (MomUnitCalculationsImpl.class.getName (), "calculateCombatMovementDistances", unitBeingMoved.getUnitURN ());

		// Create other areas
		final boolean [] [] ourUnits = new boolean [combatMapCoordinateSystem.getHeight ()] [combatMapCoordinateSystem.getWidth ()];
		final boolean [] [] enemyUnits = new boolean [combatMapCoordinateSystem.getHeight ()] [combatMapCoordinateSystem.getWidth ()];
		
		// Initialize areas
		for (int y = 0; y < combatMapCoordinateSystem.getHeight (); y++)
			for (int x = 0; x < combatMapCoordinateSystem.getWidth (); x++)
			{
				doubleMovementDistances [y] [x] = MOVEMENT_DISTANCE_NOT_YET_CHECKED;
				movementTypes [y] [x] = CombatMoveType.CANNOT_MOVE;
				movementDirections [y] [x] = 0;
				ourUnits [y] [x] = false;
				enemyUnits [y] [x] = false;
			}
		
		// We know combatLocation from the unit being moved
		final OverlandMapCoordinatesEx combatLocation = (OverlandMapCoordinatesEx) unitBeingMoved.getCombatLocation ();
		
		// Mark locations of units on both sides (including the unit being moved)
		for (final MemoryUnit thisUnit : fogOfWarMemory.getUnit ())
			if ((combatLocation.equals (thisUnit.getCombatLocation ())) && (thisUnit.getStatus () == UnitStatusID.ALIVE) && (thisUnit.getCombatPosition () != null))
			{
				if (thisUnit.getOwningPlayerID () == unitBeingMoved.getOwningPlayerID ())
					ourUnits [thisUnit.getCombatPosition ().getY ()] [thisUnit.getCombatPosition ().getX ()] = true;
				else
					enemyUnits [thisUnit.getCombatPosition ().getY ()] [thisUnit.getCombatPosition ().getX ()] = true;
			}
		
		// We can move to where we start from for free
		doubleMovementDistances [unitBeingMoved.getCombatPosition ().getY ()] [unitBeingMoved.getCombatPosition ().getX ()] = 0;
		movementTypes [unitBeingMoved.getCombatPosition ().getY ()] [unitBeingMoved.getCombatPosition ().getX ()] = CombatMoveType.MOVE;
		
		// Rather than iterating out distances from the centre, process rings around each location before proceeding to the next location.
		// This is to prevent the situation in the original MoM where you are on Enchanced Road,
		// hit 'Up' and the game decides to move you up-left and then right to get there.
		final List<CombatMapCoordinatesEx> cellsLeftToCheck = new ArrayList<CombatMapCoordinatesEx> ();
		processCell ((CombatMapCoordinatesEx) unitBeingMoved.getCombatPosition (), unitBeingMoved, cellsLeftToCheck,
			doubleMovementDistances, movementDirections, movementTypes, ourUnits, enemyUnits, combatMap, combatMapCoordinateSystem, db);
		
		// Keep going until there's nowhere left to check
		while (cellsLeftToCheck.size () > 0)
		{
			processCell (cellsLeftToCheck.get (0), unitBeingMoved, cellsLeftToCheck,
				doubleMovementDistances, movementDirections, movementTypes, ourUnits, enemyUnits, combatMap, combatMapCoordinateSystem, db);
			cellsLeftToCheck.remove (0);
		}
		
		// Now check if we can fire missile attacks at any enemies
		if (canMakeRangedAttack (unitBeingMoved, players, fogOfWarMemory.getMaintainedSpell (), fogOfWarMemory.getCombatAreaEffect (), db))
			for (int y = 0; y < combatMapCoordinateSystem.getHeight (); y++)
				for (int x = 0; x < combatMapCoordinateSystem.getWidth (); x++)
					if (enemyUnits [y] [x])
					{
						// Firing a missle weapon always uses up all of our movement so mark this for the sake of it - although MovementDistances
						// isn't actually used to reduce the movement a unit has left in this fashion
						movementTypes [y] [x] = CombatMoveType.RANGED;
						doubleMovementDistances [y] [x] = 999;
					}
		
		log.exiting (MomUnitCalculationsImpl.class.getName (), "calculateCombatMovementDistances");
	}
	
	/**
	 * @return Player pick utils
	 */
	public final PlayerPickUtils getPlayerPickUtils ()
	{
		return playerPickUtils;
	}

	/**
	 * @param utils Player pick utils
	 */
	public final void setPlayerPickUtils (final PlayerPickUtils utils)
	{
		playerPickUtils = utils;
	}

	/**
	 * @return Combat map utils
	 */
	public final CombatMapUtils getCombatMapUtils ()
	{
		return combatMapUtils;
	}

	/**
	 * @param utils Combat map utils
	 */
	public final void setCombatMapUtils (final CombatMapUtils utils)
	{
		combatMapUtils = utils;
	}

	/**
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final UnitUtils utils)
	{
		unitUtils = utils;
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
}
