package momime.common.calculations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemType;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.SquareMapDirection;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.CombatAction;
import momime.common.database.CombatMapLayerID;
import momime.common.database.CombatTileBorder;
import momime.common.database.CombatTileBorderBlocksMovementID;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.MovementRateRule;
import momime.common.database.RecordNotFoundException;
import momime.common.database.TileTypeEx;
import momime.common.database.UnitCanCast;
import momime.common.database.UnitSkillEx;
import momime.common.messages.ConfusionEffect;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MemoryUnitHeroItemSlot;
import momime.common.messages.MomCombatTile;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.PlayerPick;
import momime.common.messages.UnitStatusID;
import momime.common.utils.CombatMapUtils;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.UnitUtils;

/**
 * Common calculations pertaining to units
 */
public final class UnitCalculationsImpl implements UnitCalculations
{
	/** Marks locations in the doubleMovementDistances array that we haven't checked yet */
	private final static int MOVEMENT_DISTANCE_NOT_YET_CHECKED = -1;

	/** Marks locations in the doubleMovementDistances array that we've proved that we cannot move to */
	private final static int MOVEMENT_DISTANCE_CANNOT_MOVE_HERE = -2;
	
	/** List of confusion effects where the player does not get any allocated movement */
	private final static List<ConfusionEffect> CONFUSION_NOT_PLAYER_CONTROLLED = Arrays.asList (ConfusionEffect.DO_NOTHING, ConfusionEffect.MOVE_RANDOMLY);
	
	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/** Combat map utils */
	private CombatMapUtils combatMapUtils;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;

	/** MemoryGridCell utils */
	private MemoryGridCellUtils memoryGridCellUtils;
	
	/**
	 * Gives all units full movement back again for their combat turn
	 *
	 * @param playerID Player whose units to update 
	 * @param combatLocation Where the combat is taking place
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return List of units that didn't get any movement allocated because they're stuck in a web
	 * @throws RecordNotFoundException If the unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	@Override
	public final List<ExpandedUnitDetails> resetUnitCombatMovement (final int playerID, final MapCoordinates3DEx combatLocation,
		final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		final List<ExpandedUnitDetails> webbedUnits = new ArrayList<ExpandedUnitDetails> ();
		
		for (final MemoryUnit thisUnit : mem.getUnit ())
			if ((combatLocation.equals (thisUnit.getCombatLocation ())) && (thisUnit.getCombatPosition () != null) &&
				(thisUnit.getCombatSide () != null) && (thisUnit.getCombatHeading () != null) && (thisUnit.getStatus () == UnitStatusID.ALIVE))
			{
				final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (thisUnit, null, null, null, players, mem, db);
				if (xu.getControllingPlayerID () == playerID)
				{				
					final boolean webbed;
					if (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_WEB))
					{
						final Integer webHP = xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_WEB);
						webbed = (webHP != null) && (webHP > 0);
					}
					else
						webbed = false;
						
					if (webbed)
						thisUnit.setDoubleCombatMovesLeft (0);
					else
					{
						if ((xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_CONFUSION)) && (CONFUSION_NOT_PLAYER_CONTROLLED.contains (thisUnit.getConfusionEffect ())))
							thisUnit.setDoubleCombatMovesLeft (0);
						else						
							thisUnit.setDoubleCombatMovesLeft (2 * xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED));
					}
					
					if (webbed)
						webbedUnits.add (xu);
				}
			}
		
		return webbedUnits;
	}
	
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
		(final List<MemoryBuilding> buildings, final MapVolumeOfMemoryGridCells map, final MapCoordinates3DEx cityLocation,
		final List<PlayerPick> picks, final CoordinateSystem overlandMapCoordinateSystem, final CommonDatabase db) throws RecordNotFoundException
	{
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
			final MapCoordinates3DEx coords = new MapCoordinates3DEx (cityLocation);
			for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
			{
				if (getCoordinateSystemUtils ().move3DCoordinates (overlandMapCoordinateSystem, coords, direction.getDirectionID ()))
				{
					final OverlandMapTerrainData terrainData = map.getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
					if ((terrainData != null) && (terrainData.getCorrupted () == null) && (terrainData.getMapFeatureID () != null))
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
			// Any types of wall here that block movement?  (not using iterator because there's going to be so few of these).
			// NB. You still cannot walk across corners of city walls even when they've been wrecked.
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
					
					// Mud overrides anything else in the terrain layer, but movement rate can still be reduced by roads or set to impassable by buildings
					if ((layer == CombatMapLayerID.TERRAIN) && (tile.isMud ()))
						result = 1000;
					else
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
	 * Initializes any values on the unit at the start of a combat
	 * NB. Available units can never expend ranged attack ammo or use mana, but storing these values keeps avoids the need for the
	 * methods to use the Fog of War memory to look for spell effects that might increase ammo or mana
	 * 
	 * @param unit Unit we want to give ammo+mana to
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	@Override
	public final void giveUnitFullRangedAmmoAndMana (final ExpandedUnitDetails unit) throws MomException
	{
		// Easy values
		unit.setAmmoRemaining (unit.calculateFullRangedAttackAmmo ());
		unit.setManaRemaining (unit.calculateManaTotal ());

		// Fixed spells, like Giant Spiders 'casting' web or Magicians casting Fireball
		unit.getMemoryUnit ().getFixedSpellsRemaining ().clear ();
		for (final UnitCanCast fixedSpell : unit.getUnitDefinition ().getUnitCanCast ())
		{
			final int count;
			if ((fixedSpell.getNumberOfTimes () != null) && (fixedSpell.getNumberOfTimes () > 0))
				count = fixedSpell.getNumberOfTimes ();
			else
				count = -1;
				
			unit.getMemoryUnit ().getFixedSpellsRemaining ().add (count);
		}
		
		// Spell charges on hero items
		unit.getMemoryUnit ().getHeroItemSpellChargesRemaining ().clear ();
		for (final MemoryUnitHeroItemSlot slot : unit.getMemoryUnit ().getHeroItemSlot ())
		{
			final int count;
			if (slot.getHeroItem () == null)
				count = -1;
			else if (slot.getHeroItem ().getSpellChargeCount () == null)
				count = -1;
			else
				count = slot.getHeroItem ().getSpellChargeCount ();
			
			unit.getMemoryUnit ().getHeroItemSpellChargesRemaining ().add (count);
		}
	}

	/**
	 * Decreases amount of ranged ammo remaining for this unit when it fires a ranged attack
	 * @param unit Unit making the ranged attack
	 */
	@Override
	public final void decreaseRangedAttackAmmo (final MemoryUnit unit)
	{
		if (unit.getAmmoRemaining () > 0)
			unit.setAmmoRemaining (unit.getAmmoRemaining () - 1);
		else
			unit.setManaRemaining (unit.getManaRemaining () - 3);
	}
	
	/**
	 * This isn't as straightforward as it sounds, we either need dedicated ranged attack ammo (which can be phys or magic ranged attacks)
	 * or caster units can spend mana to fire ranged attacks, but only magical ranged attacks
	 * 
	 * @param unit Unit to calculate for
	 * @return Whether the unit can make a ranged attack in combat and has ammo to do so
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	@Override
	public final boolean canMakeRangedAttack (final ExpandedUnitDetails unit) throws MomException
	{
		final boolean result;
		
		// First we have to actually have a ranged attack
		if ((!unit.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK)) ||
			(unit.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK) <= 0))
			result = false;
		
		// If we have ranged attack ammo left then this is easy
		else if (unit.getAmmoRemaining () > 0)
			result = true;
		
		// If we don't have enough spare mana then we can't use it to fire with
		else if (unit.getManaRemaining () < 3)
			result = false;
		
		// We have to have a ranged attack ID to check
		else
		{
			// We have spare mana to fire but first we have to prove that our type of attack is magical - we can't use Mana to fire a bow!
			if (unit.getRangedAttackType () == null)
				result = false;
			else
				result = (unit.getRangedAttackType ().getMagicRealmID () != null);
		}
		
		return result;
	}

	/**
	 * This is much simpler than canMakeRangedAttack, as we don't need ammo to fire with.
	 * This is really here to stop settlers with 0 attack trying to attack other units.
	 * But also have to stop grounded units attacking flying units, unless they have a thrown/gaze/breath attack.
	 * 
	 * @param enemyCombatActionID Standing combat action of the unit we want to attack, so we know whether it is flying
	 * @param unit Unit doing the attacking
	 * @param db Lookup lists built over the XML database
	 * @return Whether the unit can make a melee attack in combat
	 * @throws RecordNotFoundException If the enemyCombatActionID cannot be found
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	@Override
	public final boolean canMakeMeleeAttack (final String enemyCombatActionID, final ExpandedUnitDetails unit, final CommonDatabase db)
		throws MomException, RecordNotFoundException
	{
		boolean result;
		
		// First we have to actually have a ranged attack
		if ((!unit.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK)) ||
			(unit.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK) <= 0))
			result = false;
		
		// AI calls this without specifying enemyCombatActionID
		else if (enemyCombatActionID == null)
			result = true;
		
		else
		{
			// Is the enemy unit flying?
			final CombatAction enemyCombatAction = db.findCombatAction (enemyCombatActionID, "canMakeMeleeAttack");
			if (enemyCombatAction.getCanOnlyBeAttackedByUnitsWithSkill ().size () == 0)
				result = true;
			else
			{
				// To attack it, we have to have the same combatActionID (also flying), or one of the listed skills
				result = determineCombatActionID (unit, false, db).equals (enemyCombatActionID);
				final Iterator<String> iter = unit.listModifiedSkillIDs ().iterator ();
				while ((!result) && (iter.hasNext ()))
				{
					final String unitSkillID = iter.next ();
					if (enemyCombatAction.getCanOnlyBeAttackedByUnitsWithSkill ().contains (unitSkillID))
						result = true;
				}
			}
		}

		return result;
	}
		
	/**
	 * Will return true if we blunder onto a tile containing invisible units we couldn't see
	 * 
	 * @param x X coordinate of the location we want to check
	 * @param y Y coordinate of the location we want to check
	 * @param plane Plane we want to check
	 * @param movingPlayerID The player who is trying to move here
	 * @param map The player who is trying to move here's knowledge of the terrain
	 * @param units The player who is trying to move here's knowledge of units
	 * @return Whether moving here will result in an attack or not
	 */
	@Override
	public final boolean willMovingHereResultInAnAttack (final int x, final int y, final int plane, final int movingPlayerID,
		final MapVolumeOfMemoryGridCells map, final List<MemoryUnit> units)
	{
		// Work out what plane to look for units on
		final MemoryGridCell mc = map.getPlane ().get (plane).getRow ().get (y).getCell ().get (x);
		final int towerPlane;
		if (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (mc.getTerrainData ()))
			towerPlane = 0;
		else
			towerPlane = plane;

		// The easiest one to check for is an enemy city - even if there's no units there, it still counts as an attack so we can decide whether to raze or capture it
		final boolean resultsInAttack;
		if ((mc.getCityData () != null) && (mc.getCityData ().getCityOwnerID () != movingPlayerID))
			resultsInAttack = true;

		// Lastly check for enemy units
		else if (getUnitUtils ().findFirstAliveEnemyAtLocation (units, x, y, towerPlane, movingPlayerID) != null)
			resultsInAttack = true;
		else
			resultsInAttack = false;

		return resultsInAttack;
	}
	
	/**
	 * Will only return true if we can see units in the target tile; if there's invisible enemies there will return false
	 * 
	 * @param x X coordinate of the location we want to check
	 * @param y Y coordinate of the location we want to check
	 * @param plane Plane we want to check
	 * @param movingPlayerID The player who is trying to move here
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Whether moving here will result in an attack or not
	 */
	@Override
	public final boolean willMovingHereResultInAnAttackThatWeKnowAbout (final int x, final int y, final int plane, final int movingPlayerID,
		final FogOfWarMemory mem, final CommonDatabase db)
	{
		// Work out what plane to look for units on
		final MemoryGridCell mc = mem.getMap ().getPlane ().get (plane).getRow ().get (y).getCell ().get (x);
		final int towerPlane;
		if (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (mc.getTerrainData ()))
			towerPlane = 0;
		else
			towerPlane = plane;

		// The easiest one to check for is an enemy city - even if there's no units there, it still counts as an attack so we can decide whether to raze or capture it
		final boolean resultsInAttack;
		if ((mc.getCityData () != null) && (mc.getCityData ().getCityOwnerID () != movingPlayerID))
			resultsInAttack = true;

		// Lastly check for enemy units
		else if (getUnitUtils ().findFirstAliveEnemyWeCanSeeAtLocation (movingPlayerID, mem, x, y, towerPlane, movingPlayerID, db) != null)
			resultsInAttack = true;
		else
			resultsInAttack = false;

		return resultsInAttack;
	}
	
	/**
	 * @param unitStack Unit stack to check
	 * @return Merged list of every skill that at least one unit in the stack has, including skills granted from spells
	 * @throws MomException If the list includes something other than MemoryUnits or ExpandedUnitDetails
	 */
	@Override
	public final Set<String> listAllSkillsInUnitStack (final List<ExpandedUnitDetails> unitStack) throws MomException
	{
		final Set<String> list = new HashSet<String> ();
		String debugList = "";

		if (unitStack != null)
			for (final ExpandedUnitDetails thisUnit : unitStack)
				for (final String thisSkillID : thisUnit.listModifiedSkillIDs ())
					if (!list.contains (thisSkillID))
					{
						list.add (thisSkillID);

						if (!debugList.equals (""))
							debugList = debugList + ", ";

						debugList = debugList + thisSkillID;
					}

		return list;
	}

	/**
	 * @param unit Unit that we want to move
	 * @param unitStackSkills All the skills that any units in the stack moving with this unit have, in case any have e.g. path finding that we can take advantage of - get by calling listAllSkillsInUnitStack
	 * @param tileTypeID Type of tile we are moving onto
	 * @param db Lookup lists built over the XML database
	 * @return Double the number of movement points we will use to walk onto that tile; null = impassable
	 */
	@Override
	public final Integer calculateDoubleMovementToEnterTileType (final ExpandedUnitDetails unit, final Set<String> unitStackSkills, final String tileTypeID, final CommonDatabase db)
	{
		// We basically run down the movement rate rules and stop as soon as we find the first applicable one
		// Terrain is impassable if we check every movement rule and none of them are applicable
		Integer doubleMovement = null;
		final Iterator<MovementRateRule> rules = db.getMovementRateRule ().iterator ();
		while ((doubleMovement == null) && (rules.hasNext ()))
		{
			final MovementRateRule thisRule = rules.next ();

			// All 3 parts are optional
			if (((thisRule.getTileTypeID () == null) || (thisRule.getTileTypeID ().equals (tileTypeID))) &&
				((thisRule.getUnitSkillID () == null) || (unit.hasModifiedSkill (thisRule.getUnitSkillID ()))) &&
				((thisRule.getUnitStackSkillID () == null) || (unitStackSkills.contains (thisRule.getUnitStackSkillID ()))))

				doubleMovement = thisRule.getDoubleMovement ();
		}

		return doubleMovement;
	}
	
	/**
	 * @param unit Unit that we want to move
	 * @param unitStackSkills All the skills that any units in the stack moving with this unit have, in case any have e.g. path finding that we can take advantage of - get by calling listAllSkillsInUnitStack
	 * @param db Lookup lists built over the XML database
	 * @return Whether this unit can pass over every type of possible terrain on the map; i.e. true for swimming units like Lizardmen, any flying unit, or any unit stacked with a Wind Walking unit
	 */
	@Override
	public final boolean areAllTerrainTypesPassable (final ExpandedUnitDetails unit, final Set<String> unitStackSkills, final CommonDatabase db)
	{
		// Go through each tile type
		boolean result = true;
		final Iterator<TileTypeEx> iter = db.getTileTypes ().iterator ();
		while ((result) && (iter.hasNext ()))
		{
			final TileTypeEx tileType = iter.next ();
			if ((!tileType.getTileTypeID ().equals (CommonDatabaseConstants.TILE_TYPE_FOG_OF_WAR_HAVE_SEEN)) &&
				(calculateDoubleMovementToEnterTileType (unit, unitStackSkills, tileType.getTileTypeID (), db) == null))
				
				result = false;
		}

		return result;
	}
	
	/**
	 * Checks whether selectedUnits includes any transports, and if so whether the other units fit inside them, and whether any others in the same map cell should be added to the stack.
	 * See the UnitStack object for a lot more comments on the rules by which this needs to work.
	 * 
	 * @param selectedUnits Units selected by the player to move
	 * @param players Players list
	 * @param fogOfWarMemory Known overland terrain, units, buildings and so on 
	 * @param db Lookup lists built over the XML database
	 * @return UnitStack object
	 * @throws PlayerNotFoundException If we cannot find the player who a unit in the same location as our transports
	 * @throws RecordNotFoundException If we can't find the definitions for any of the units at the location
	 * @throws MomException If selectedUnits is empty, all the units aren't at the same location, or all the units don't have the same owner 
	 */
	@Override
	public final UnitStack createUnitStack (final List<ExpandedUnitDetails> selectedUnits,
		final List<? extends PlayerPublicDetails> players, final FogOfWarMemory fogOfWarMemory, final CommonDatabase db)
		throws PlayerNotFoundException, RecordNotFoundException, MomException
	{
		// We need at least one unit, or we can't even figure out the location and owner
		if (selectedUnits.size () == 0)
			throw new MomException ("createUnitStack: Selected units list is empty");
		
		final Set<String> unitStackSkills = listAllSkillsInUnitStack (selectedUnits);
		
		// Count the units already in the stack into 3 categories: transports, units that want to be in transports (have some impassable terrain), and units that will accompany transports (all terrain passable)
		// Also while we're at it, find the location of the unit stack
		final List<ExpandedUnitDetails> transports = new ArrayList<ExpandedUnitDetails> ();
		int transportCapacity = 0;
		int unitsInside = 0;
		int owningPlayerID = 0;
		MapCoordinates3DEx unitLocation = null;
		
		for (final ExpandedUnitDetails thisUnit : selectedUnits)
		{
			// Categorise unit
			final Integer thisTransportCapacity = thisUnit.getUnitDefinition ().getTransportCapacity ();
			if ((thisTransportCapacity != null) && (thisTransportCapacity > 0))
			{
				transports.add (thisUnit);
				transportCapacity = transportCapacity + thisTransportCapacity;
			}
			else if (!areAllTerrainTypesPassable (thisUnit, unitStackSkills, db))
				unitsInside++;
			
			// Record or check location
			if (unitLocation == null)
				unitLocation = thisUnit.getUnitLocation ();
			else if (!unitLocation.equals (thisUnit.getUnitLocation ()))
				throw new MomException ("createUnitStack: All selected units are not in the same starting location, expected " + unitLocation + " but Unit URN " +
					thisUnit.getUnitURN () + " is at " + thisUnit.getUnitLocation ());
			
			// Record or check player
			if (owningPlayerID == 0)
				owningPlayerID = thisUnit.getOwningPlayerID ();
			else if (owningPlayerID != thisUnit.getOwningPlayerID ())
				throw new MomException ("createUnitStack: All selected units are not owned by the same player");				
		}
		
		// Now can figure out if the stack will move in "transported" or "normal" mode
		final UnitStack stack = new UnitStack ();
		if ((transportCapacity > 0) && (transportCapacity >= unitsInside))
		{
			// "transported" mode, so first take the list of transports, and any units already specified
			stack.getTransports ().addAll (transports);
			stack.getUnits ().addAll (selectedUnits);
			stack.getUnits ().removeAll (transports);
			
			// Now search for other units in the same location that can also fit and should be added to the stack
			for (final MemoryUnit thisUnit : fogOfWarMemory.getUnit ())
				
				if ((thisUnit.getOwningPlayerID () == owningPlayerID) && (thisUnit.getStatus () == UnitStatusID.ALIVE) &&
					(unitLocation.equals (thisUnit.getUnitLocation ())) && (!selectedUnits.stream ().anyMatch (xu -> thisUnit == xu.getUnit ())))
				{
					// Never automatically add additional transports
					final Integer thisTransportCapacity = db.findUnit (thisUnit.getUnitID (), "createUnitStack").getTransportCapacity ();
					if ((thisTransportCapacity == null) || (thisTransportCapacity <= 0))
					{
						// Always automatically add "outside" units; add "inside" units only if there's still space
						final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (thisUnit, null, null, null, players, fogOfWarMemory, db);
						if (areAllTerrainTypesPassable (xu, unitStackSkills, db))
							stack.getUnits ().add (xu);
						else if (unitsInside < transportCapacity)
						{
							stack.getUnits ().add (xu);
							unitsInside++;
						}
					}
				}
		}
		else
		{
			// "normal" mode, so just take the unit stack as given
			stack.getUnits ().addAll (selectedUnits); 
		}
		
		return stack;
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
		
		// Quick check, if there's no border at all or the tile has been wrecked, then must be OK
		final MomCombatTile tile = combatMap.getRow ().get (y).getCell ().get (x);
		if ((tile.getBorderDirections () == null) || (tile.isWrecked ()))
			ok = true;
		
		// So there is a border - check if it includes the requested direction.
		// Have to check +1/-1 of the requested direction so that flat walls also stop moving in diagonals past the wall.
		else if ((!tile.getBorderDirections ().contains (Integer.valueOf (d).toString ())) &&
					(!tile.getBorderDirections ().contains (Integer.valueOf (getCoordinateSystemUtils ().normalizeDirection (combatMapCoordinateSystemType, d+1)).toString ())) &&
					(!tile.getBorderDirections ().contains (Integer.valueOf (getCoordinateSystemUtils ().normalizeDirection (combatMapCoordinateSystemType, d-1)).toString ())))
					
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
				
				else if ((border.getBlocksMovement () == CombatTileBorderBlocksMovementID.CANNOT_CROSS_DIAGONALS) &&
					(!tile.getBorderDirections ().contains (Integer.valueOf (d).toString ())))
					
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
	final void processCell (final MapCoordinates2DEx moveFrom, final ExpandedUnitDetails unitBeingMoved, final boolean ignoresCombatTerrain,
		final List<MapCoordinates2DEx> cellsLeftToCheck, final int [] [] doubleMovementDistances, final int [] [] movementDirections,
		final CombatMoveType [] [] movementTypes, final boolean [] [] ourUnits, final String [] [] enemyUnits, final List<String> borderTargetIDs,
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
					final int doubleMovementToEnterThisTile = calculateDoubleMovementToEnterCombatTile (moveToTile, db);
					
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
							((okToCrossCombatTileBorder (combatMap, combatMapCoordinateSystem.getCoordinateSystemType (), moveFrom.getX (), moveFrom.getY (), d, db)) &&
							(okToCrossCombatTileBorder (combatMap, combatMapCoordinateSystem.getCoordinateSystemType (), moveTo.getX (), moveTo.getY (),
								getCoordinateSystemUtils ().normalizeDirection (combatMapCoordinateSystem.getCoordinateSystemType (), d+4), db)));
						
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
									final CombatMoveType combatMoveType;
									if ((enemyCombatActionID != null) || (canAttackTile))
									{
										// Can we attack the unit here?
										if (canMakeMeleeAttack (enemyCombatActionID, unitBeingMoved, db))
										{
											if (enemyCombatActionID != null)
												combatMoveType = canAttackTile ? CombatMoveType.MELEE_UNIT_AND_WALL : CombatMoveType.MELEE_UNIT;
											else
												combatMoveType = CombatMoveType.MELEE_WALL;
										}
										else
										{
											combatMoveType = CombatMoveType.CANNOT_MOVE;
											doubleMovementDistances [moveTo.getY ()] [moveTo.getX ()] = MOVEMENT_DISTANCE_CANNOT_MOVE_HERE;
											movementDirections [moveTo.getY ()] [moveTo.getX ()] = 0;
										}
									}
									else
										combatMoveType = CombatMoveType.MOVE;

									movementTypes [moveTo.getY ()] [moveTo.getX ()] = combatMoveType;
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
		final CombatMoveType [] [] movementTypes, final ExpandedUnitDetails unitBeingMoved, final FogOfWarMemory fogOfWarMemory,
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
				movementTypes [y] [x] = CombatMoveType.CANNOT_MOVE;
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
		
		for (final MemoryUnit thisUnit : fogOfWarMemory.getUnit ())
			if ((combatLocation.equals (thisUnit.getCombatLocation ())) && (thisUnit.getStatus () == UnitStatusID.ALIVE) &&
				(thisUnit.getCombatPosition () != null) && (thisUnit.getCombatSide () != null) && (thisUnit.getCombatHeading () != null))
			{
				// Note on owning vs controlling player ID - unitBeingMoved.getControllingPlayerID () is the player whose turn it is, who is controlling the unit, so this is fine.
				// But they don't want to attack their own units who might just be temporarily confused, equally if an enemy unit is confused and currently under our
				// control, we still want to kill it - ideally we confusee units and make them kill each other!  So this is why it is not xu.getControllingPlayerID ()
				final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (thisUnit, unitsBeingMoved, null, null, players, fogOfWarMemory, db);
				if ((thisUnit == unitBeingMoved.getMemoryUnit ()) || (xu.getOwningPlayerID () == unitBeingMoved.getControllingPlayerID ()))
					ourUnits [thisUnit.getCombatPosition ().getY ()] [thisUnit.getCombatPosition ().getX ()] = true;
				
				else if (getUnitUtils ().canSeeUnitInCombat (xu, unitBeingMoved.getControllingPlayerID (), players, fogOfWarMemory, db, combatMapCoordinateSystem))
				{
					enemyUnits [thisUnit.getCombatPosition ().getY ()] [thisUnit.getCombatPosition ().getX ()] = determineCombatActionID (xu, false, db);
					
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
		movementTypes [unitBeingMoved.getCombatPosition ().getY ()] [unitBeingMoved.getCombatPosition ().getX ()] = CombatMoveType.MOVE;
		
		// Rather than iterating out distances from the centre, process rings around each location before proceeding to the next location.
		// This is to prevent the situation in the original MoM where you are on Enchanced Road,
		// hit 'Up' and the game decides to move you up-left and then right to get there.
		final List<MapCoordinates2DEx> cellsLeftToCheck = new ArrayList<MapCoordinates2DEx> ();
		processCell (unitBeingMoved.getCombatPosition (), unitBeingMoved, ignoresCombatTerrain, cellsLeftToCheck,
			doubleMovementDistances, movementDirections, movementTypes, ourUnits, enemyUnits, borderTargetIDs, combatMap, combatMapCoordinateSystem, db);
		
		// Keep going until there's nowhere left to check
		while (cellsLeftToCheck.size () > 0)
		{
			processCell (cellsLeftToCheck.get (0), unitBeingMoved, ignoresCombatTerrain, cellsLeftToCheck,
				doubleMovementDistances, movementDirections, movementTypes, ourUnits, enemyUnits, borderTargetIDs, combatMap, combatMapCoordinateSystem, db);
			cellsLeftToCheck.remove (0);
		}
		
		// Now check if we can fire missile attacks at any enemies
		if (canMakeRangedAttack (unitBeingMoved))
		{
			for (final ExpandedUnitDetails xu : directlyVisibleEnemyUnits)
			{
				final int x = xu.getCombatPosition ().getX ();
				final int y = xu.getCombatPosition ().getY ();
				
				// If the unit is invisible, we have to have True Sight / Illusions Immunity to be able to make a ranged attack against it.
				// Simply being able to see it, or even standing right next to it, isn't enough.
				
				// Firing a missle weapon always uses up all of our movement so mark this for the sake of it - although MovementDistances
				// isn't actually used to reduce the movement a unit has left in this fashion
				movementTypes [y] [x] = CombatMoveType.RANGED_UNIT;
				doubleMovementDistances [y] [x] = 999;
			}
			
			// Can also ranged attack wall segments
			if (borderTargetIDs != null)
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
							if (movementTypes [y] [x] == CombatMoveType.RANGED_UNIT)
								movementTypes [y] [x] = CombatMoveType.RANGED_UNIT_AND_WALL;
							else
								movementTypes [y] [x] = CombatMoveType.RANGED_WALL;
							
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
					if ((movementTypes [y] [x] == CombatMoveType.CANNOT_MOVE) ||
						((movementTypes [y] [x] == CombatMoveType.MOVE) && (doubleMovementDistances [y] [x] > 2)))
					{
						final MomCombatTile moveToTile = combatMap.getRow ().get (y).getCell ().get (x);
						final int doubleMovementToEnterThisTile = calculateDoubleMovementToEnterCombatTile (moveToTile, db);
						
						// Our own units prevent us moving here - so do enemy units, we cannot move onto them, only next to them
						if ((doubleMovementToEnterThisTile < 0) || (ourUnits [y] [x]) || (enemyUnits [y] [x] != null))
						{
							// Can't move here
						}
						else
						{
							movementTypes [y] [x] = CombatMoveType.TELEPORT;
							doubleMovementDistances [y] [x] = 2;
						}
					}
				}
	}
	
	/**
	 * Chooses the preferred method of movement for this unit, i.e. the one with the lowest preference number (no. 1 is chosen first, then no. 2, etc.)
	 * 
	 * This ensures that e.g. Flying units (whether natural flight, spell-cast Flight or Chaos Channels Flight) all show the
	 * correct flight icon, and units with Swimming/Sailing show the wave icon
	 * 
	 * @param unit Unit to determine the movement graphics for
	 * @param db Lookup lists built over the XML database
	 * @return Movement graphics node
	 * @throws MomException If this unit has no skills which have movement graphics, or we can't find its experience level
	 */
	@Override
	public final UnitSkillEx findPreferredMovementSkillGraphics (final ExpandedUnitDetails unit, final CommonDatabase db) throws MomException
	{
		// Check all movement skills
		UnitSkillEx bestMatch = null;
		for (final UnitSkillEx thisSkill : db.getUnitSkills ())
			if (thisSkill.getMovementIconImagePreference () != null)
				if ((bestMatch == null) || (thisSkill.getMovementIconImagePreference () < bestMatch.getMovementIconImagePreference ()))
					if (unit.hasModifiedSkill (thisSkill.getUnitSkillID ()))
						bestMatch = thisSkill;
		
		if (bestMatch == null)
			throw new MomException ("Unit " + unit.getUnitID () + " has no skills which have movement graphics");
		
		return bestMatch;
	}

	/**
	 * combatActionIDs are MELEE when attacking melee, RANGED when attacking ranged, and generated by
	 * this routine when units are not attacking.  It looks up the combatActionIDs depending on what movement
	 * skills the unit has in such a way that we avoid having to hard code combatActionIDs.
	 * 
	 * e.g. a regular unit of swordsmen shows the STAND image while not moving, but if we cast
	 * Flight on them then we need to show the FLY animation instead.
	 *
	 * In the animations as directly converted from the original MoM graphics, WALK and FLY look the same - they
	 * resolve to the same animation, named e.g. UN100_D4_WALKFLY.  However the intention in the long term is
	 * to separate these and show flying units significantly raised up off the ground, so you can actually see flying
	 * units coming down to ground level when they have web cast on them, or swordsmen high up in the
	 * air when they have flight cast on them.
	 * 
	 * @param unit Unit to determine the combat action ID for
	 * @param isMoving Whether the unit is standing still or moving
	 * @param db Lookup lists built over the XML database
	 * @return Action ID for a unit standing still or moving
	 * @throws MomException If this unit has no skills which have movement graphics, we can't find its experience level, or a movement skill doesn't specify an action ID
	 */
	@Override
	public final String determineCombatActionID (final ExpandedUnitDetails unit, final boolean isMoving, final CommonDatabase db) throws MomException
	{
		// This is pretty straightforward, findPreferredMovementSkillGraphics does most of the work for us
		final UnitSkillEx movementSkill = findPreferredMovementSkillGraphics (unit, db);
		final String combatActionID = isMoving ? movementSkill.getMoveActionID () : movementSkill.getStandActionID ();
		
		if (combatActionID == null)
			throw new MomException ("determineCombatActionID for unit " + unit.getUnitID () + " found movement skill " + movementSkill.getUnitSkillID () +
				" but the movement skill doesn't specify a combatActionID for isMoving = " + isMoving); 
		
		return combatActionID;
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
}