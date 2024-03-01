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
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.CombatAction;
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
import momime.common.movement.UnitStack;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.UnitUtils;

/**
 * Common calculations pertaining to units
 */
public final class UnitCalculationsImpl implements UnitCalculations
{
	/** List of confusion effects where the player does not get any allocated movement */
	private final static List<ConfusionEffect> CONFUSION_NOT_PLAYER_CONTROLLED = Arrays.asList (ConfusionEffect.DO_NOTHING, ConfusionEffect.MOVE_RANDOMLY);
	
	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;

	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/**
	 * Gives all units full movement back again for their combat turn
	 *
	 * @param playerID Player whose units to update 
	 * @param combatLocation Where the combat is taking place
	 * @param terrifiedUnitURNs List of units who failed their resistance roll against Terror spell and so cannot move this turn
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return List of units that didn't get any movement allocated because they're stuck in a web
	 * @throws RecordNotFoundException If the unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	@Override
	public final List<ExpandedUnitDetails> resetUnitCombatMovement (final int playerID, final MapCoordinates3DEx combatLocation, final List<Integer> terrifiedUnitURNs,
		final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		final List<ExpandedUnitDetails> webbedUnits = new ArrayList<ExpandedUnitDetails> ();
		
		for (final MemoryUnit thisUnit : mem.getUnit ())
			if ((combatLocation.equals (thisUnit.getCombatLocation ())) && (thisUnit.getCombatPosition () != null) &&
				(thisUnit.getCombatSide () != null) && (thisUnit.getCombatHeading () != null) && (thisUnit.getStatus () == UnitStatusID.ALIVE))
			{
				final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (thisUnit, null, null, null, players, mem, db);
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
					
					else if (terrifiedUnitURNs.contains (thisUnit.getUnitURN ()))
						thisUnit.setDoubleCombatMovesLeft (0);
					
					else if ((xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_CONFUSION)) && (CONFUSION_NOT_PLAYER_CONTROLLED.contains (thisUnit.getConfusionEffect ())))
						thisUnit.setDoubleCombatMovesLeft (0);
					
					else						
						thisUnit.setDoubleCombatMovesLeft (2 * xu.getMovementSpeed ());
					
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
		final MemoryGridCell mc = map.getPlane ().get (plane).getRow ().get (y).getCell ().get (x);

		// The easiest one to check for is an enemy city - even if there's no units there, it still counts as an attack so we can decide whether to raze or capture it
		final boolean resultsInAttack;
		if ((mc.getCityData () != null) && (mc.getCityData ().getCityOwnerID () != movingPlayerID))
			resultsInAttack = true;

		// Lastly check for enemy units
		else if (getUnitUtils ().findFirstAliveEnemyAtLocation (units, x, y, plane, movingPlayerID) != null)
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
		final MemoryGridCell mc = mem.getMap ().getPlane ().get (plane).getRow ().get (y).getCell ().get (x);

		// The easiest one to check for is an enemy city - even if there's no units there, it still counts as an attack so we can decide whether to raze or capture it
		final boolean resultsInAttack;
		if ((mc.getCityData () != null) && (mc.getCityData ().getCityOwnerID () != movingPlayerID))
			resultsInAttack = true;

		// Lastly check for enemy units
		else if (getUnitUtils ().findFirstAliveEnemyWeCanSeeAtLocation (movingPlayerID, mem, x, y, plane, movingPlayerID, db) != null)
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
	 * @param unitStack Unit stack we are moving; if we're only trying to test whether the terrain is impassable or not (result == null or not) then can pass in null for this
	 * @param unitStackSkills All the skills that any units in the stack moving with this unit have, in case any have e.g. path finding that we can take advantage of - get by calling listAllSkillsInUnitStack
	 * @param tileTypeID Type of tile we are moving onto
	 * @param db Lookup lists built over the XML database
	 * @return Double the number of movement points we will use to walk onto that tile; null = impassable
	 * @throws MomException If the list includes something other than MemoryUnits
	 */
	@Override
	public final Integer calculateDoubleMovementToEnterTileType (final ExpandedUnitDetails unit, final List<ExpandedUnitDetails> unitStack, final Set<String> unitStackSkills, final String tileTypeID, final CommonDatabase db)
		throws MomException
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
			{
				boolean applies = true;
				
				// Checking the special wind walking flag is a bit more complicated
				if ((thisRule.isOnlyIfWeHaveLessMovementRemainingThanTheUnitWithTheStackSkill () != null) && (thisRule.isOnlyIfWeHaveLessMovementRemainingThanTheUnitWithTheStackSkill ()))
				{
					if ((thisRule.getUnitStackSkillID () != null) && (unitStack != null))
					{
						// Find the maximum movement remaining of any unit in the stack who has the "unitStackSkillID" skill (it may be us, if we are the wind walker)
						int maximumWindWalkingMovementRemaining = 0;
						for (final ExpandedUnitDetails windWalker : unitStack)
							if ((windWalker.hasModifiedSkill (thisRule.getUnitStackSkillID ())) && (windWalker.getDoubleOverlandMovesLeft () > maximumWindWalkingMovementRemaining))
								maximumWindWalkingMovementRemaining = windWalker.getDoubleOverlandMovesLeft ();
						
						applies = (unit.getDoubleOverlandMovesLeft () < maximumWindWalkingMovementRemaining);
					}
					else
						applies = false;
				}
				
				if (applies)
					doubleMovement = thisRule.getDoubleMovement ();
			}
		}

		return doubleMovement;
	}
	
	/**
	 * This is same as calling calculateDoubleMovementToEnterTileType and checking if the result == null
	 * 
	 * @param unit Unit that we want to move
	 * @param unitStackSkills All the skills that any units in the stack moving with this unit have, in case any have e.g. path finding that we can take advantage of - get by calling listAllSkillsInUnitStack
	 * @param tileTypeID Type of tile we are moving onto
	 * @param db Lookup lists built over the XML database
	 * @return Double the number of movement points we will use to walk onto that tile; null = impassable
	 */
	@Override
	public final boolean isTileTypeImpassable (final ExpandedUnitDetails unit, final Set<String> unitStackSkills, final String tileTypeID, final CommonDatabase db)
	{
		// There can't be rows in the movement rate table that set combinations as impassable - they're all "allows" rows.  So just test whether every rule fails to match.
		return db.getMovementRateRule ().stream ().noneMatch (thisRule -> ((thisRule.getTileTypeID () == null) || (thisRule.getTileTypeID ().equals (tileTypeID))) &&
			((thisRule.getUnitSkillID () == null) || (unit.hasModifiedSkill (thisRule.getUnitSkillID ()))) &&
			((thisRule.getUnitStackSkillID () == null) || (unitStackSkills.contains (thisRule.getUnitStackSkillID ()))));
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
				(isTileTypeImpassable (unit, unitStackSkills, tileType.getTileTypeID (), db)))
				
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
						final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (thisUnit, null, null, null, players, fogOfWarMemory, db);
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
	@Override
	public final boolean okToCrossCombatTileBorder (final MapAreaOfCombatTiles combatMap, final CoordinateSystemType combatMapCoordinateSystemType,
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
}