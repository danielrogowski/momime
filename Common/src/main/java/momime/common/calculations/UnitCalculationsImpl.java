package momime.common.calculations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemType;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.SquareMapDirection;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.CombatMapLayerID;
import momime.common.database.CombatTileBorder;
import momime.common.database.CombatTileBorderBlocksMovementID;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.MovementRateRule;
import momime.common.database.RangedAttackType;
import momime.common.database.RecordNotFoundException;
import momime.common.database.TileType;
import momime.common.database.Unit;
import momime.common.database.UnitCanCast;
import momime.common.database.UnitSkillAndValue;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillPositiveNegative;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MemoryUnitHeroItemSlot;
import momime.common.messages.MomCombatTile;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.PlayerPick;
import momime.common.messages.UnitStatusID;
import momime.common.utils.CombatMapUtils;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.UnitSkillUtils;
import momime.common.utils.UnitUtils;

/**
 * Common calculations pertaining to units
 */
public final class UnitCalculationsImpl implements UnitCalculations
{
	/** Class logger */
	private final Log log = LogFactory.getLog (UnitCalculationsImpl.class);
	
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
	
	/** Unit skill utils */
	private UnitSkillUtils unitSkillUtils;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/**
	 * Gives all units full movement back again overland
	 *
	 * @param onlyOnePlayerID If zero, will reset movmenet for units belonging to all players; if specified will reset movement only for units belonging to the specified player
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If the unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	@Override
	public final void resetUnitOverlandMovement (final int onlyOnePlayerID, final List<? extends PlayerPublicDetails> players,
		final FogOfWarMemory mem, final CommonDatabase db) throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering resetUnitOverlandMovement: Player ID " + onlyOnePlayerID);

		for (final MemoryUnit thisUnit : mem.getUnit ())
			if ((onlyOnePlayerID == 0) || (onlyOnePlayerID == thisUnit.getOwningPlayerID ()))
				thisUnit.setDoubleOverlandMovesLeft (2 * getUnitSkillUtils ().getModifiedSkillValue (thisUnit, thisUnit.getUnitHasSkill (),
					CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED, null, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH,
					null, null, players, mem, db));

		log.trace ("Exiting resetUnitOverlandMovement");
	}

	/**
	 * Gives all units full movement back again for their combat turn
	 *
	 * @param playerID Player whose units to update 
	 * @param combatLocation Where the combat is taking place
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If the unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	@Override
	public final void resetUnitCombatMovement (final int playerID, final MapCoordinates3DEx combatLocation,
		final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering resetUnitCombatMovement: Player ID " + playerID + ", " + combatLocation);

		for (final MemoryUnit thisUnit : mem.getUnit ())
			if ((thisUnit.getOwningPlayerID () == playerID) && (combatLocation.equals (thisUnit.getCombatLocation ())) && (thisUnit.getCombatPosition () != null) &&
				(thisUnit.getCombatSide () != null) && (thisUnit.getCombatHeading () != null) && (thisUnit.getStatus () == UnitStatusID.ALIVE))
					
				thisUnit.setDoubleCombatMovesLeft (2 * getUnitSkillUtils ().getModifiedSkillValue (thisUnit, thisUnit.getUnitHasSkill (),
					CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED, null, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH,
					null, null, players, mem, db));

		log.trace ("Exiting resetUnitCombatMovement");
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
		log.trace ("Entering calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort: " + cityLocation);

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

		log.trace ("Exiting calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort = " + bestWeaponGrade);
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
		log.trace ("Entering calculateDoubleMovementToEnterCombatTile");

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
		
		log.trace ("Exiting calculateDoubleMovementToEnterCombatTile = " + result);
		return result;
	}

	/**
	 * Initializes any values on the unit at the start of a combat
	 * NB. Available units can never expend ranged attack ammo or use mana, but storing these values keeps avoids the need for the
	 * methods to use the Fog of War memory to look for spell effects that might increase ammo or mana
	 * 
	 * @param unit Unit we want to give ammo+mana to
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If the unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	@Override
	public final void giveUnitFullRangedAmmoAndMana (final MemoryUnit unit, final List<? extends PlayerPublicDetails> players,
		final FogOfWarMemory mem, final CommonDatabase db) throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering giveUnitFullRangedAmmoAndMana: Unit URN " + unit.getUnitURN () + ", " + unit.getUnitID ());
		
		final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (unit, null, null, null, players, mem, db);
		
		// Easy values
		unit.setAmmoRemaining (xu.calculateFullRangedAttackAmmo ());
		unit.setManaRemaining (xu.calculateManaTotal ());

		// Fixed spells, like Giant Spiders 'casting' web or Magicians casting Fireball
		unit.getFixedSpellsRemaining ().clear ();
		for (final UnitCanCast fixedSpell : xu.getUnitDefinition ().getUnitCanCast ())
		{
			final int count;
			if ((fixedSpell.getNumberOfTimes () != null) && (fixedSpell.getNumberOfTimes () > 0))
				count = fixedSpell.getNumberOfTimes ();
			else
				count = -1;
				
			unit.getFixedSpellsRemaining ().add (count);
		}
		
		// Spell charges on hero items
		unit.getHeroItemSpellChargesRemaining ().clear ();
		for (final MemoryUnitHeroItemSlot slot : unit.getHeroItemSlot ())
		{
			final int count;
			if (slot.getHeroItem () == null)
				count = -1;
			else if (slot.getHeroItem ().getSpellChargeCount () == null)
				count = -1;
			else
				count = slot.getHeroItem ().getSpellChargeCount ();
			
			unit.getHeroItemSpellChargesRemaining ().add (count);
		}

		log.trace ("Exiting giveUnitFullRangedAmmoAndMana");
	}

	/**
	 * Decreases amount of ranged ammo remaining for this unit when it fires a ranged attack
	 * @param unit Unit making the ranged attack
	 */
	@Override
	public final void decreaseRangedAttackAmmo (final MemoryUnit unit)
	{
		log.trace ("Entering decreaseRangedAttackAmmo: Unit URN " + unit.getUnitURN () + ", " + unit.getAmmoRemaining () + ", " + unit.getManaRemaining ());
		
		if (unit.getAmmoRemaining () > 0)
			unit.setAmmoRemaining (unit.getAmmoRemaining () - 1);
		else
			unit.setManaRemaining (unit.getManaRemaining () - 3);

		log.trace ("Exiting decreaseRangedAttackAmmo = " + unit.getAmmoRemaining () + ", " + unit.getManaRemaining ());
	}
	
	/**
	 * This isn't as straightforward as it sounds, we either need dedicated ranged attack ammo (which can be phys or magic ranged attacks)
	 * or caster units can spend mana to fire ranged attacks, but only magical ranged attacks
	 * 
	 * @param unit Unit to calculate for
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Whether the unit can make a ranged attack in combat and has ammo to do so
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 */
	@Override
	public final boolean canMakeRangedAttack (final MemoryUnit unit, final List<? extends PlayerPublicDetails> players,
		final FogOfWarMemory mem, final CommonDatabase db) throws RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.trace ("Entering canMakeRangedAttack: Unit URN " + unit.getUnitURN ());
		
		final boolean result;
		
		// First we have to actually have a ranged attack
		if (getUnitSkillUtils ().getModifiedSkillValue (unit, unit.getUnitHasSkill (), CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK, null,
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, mem, db) <= 0)
			
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
			final Unit unitDef = db.findUnit (unit.getUnitID (), "canMakeRangedAttack");
			if (unitDef.getRangedAttackType () == null)
				result = false;
			else
			{
				final RangedAttackType rat = db.findRangedAttackType (unitDef.getRangedAttackType (), "canMakeRangedAttack");
				result = (rat.getMagicRealmID () != null);
			}
		}
		
		log.trace ("Entering canMakeRangedAttack = " + result);
		return result;
	}
	
	/**
	 * @param unitStack Unit stack to check
	 * @param spells Known spells
	 * @param db Lookup lists built over the XML database
	 * @return Merged list of every skill that at least one unit in the stack has, including skills granted from spells
	 * @throws RecordNotFoundException If the definition of a spell that is cast on the unit cannot be found in the db
	 * @throws MomException If the list includes something other than MemoryUnits or ExpandedUnitDetails
	 */
	@Override
	public final List<String> listAllSkillsInUnitStack (final List<MemoryUnit> unitStack,
		final List<MemoryMaintainedSpell> spells, final CommonDatabase db) throws RecordNotFoundException, MomException
	{
		log.trace ("Entering listAllSkillsInUnitStack: " + getUnitUtils ().listUnitURNs (unitStack));

		final List<String> list = new ArrayList<String> ();
		String debugList = "";

		if (unitStack != null)
			for (final MemoryUnit thisUnit : unitStack)
				for (final UnitSkillAndValue thisSkill : getUnitUtils ().mergeSpellEffectsIntoSkillList (spells, thisUnit, db))
					if (!list.contains (thisSkill.getUnitSkillID ()))
					{
						list.add (thisSkill.getUnitSkillID ());

						if (!debugList.equals (""))
							debugList = debugList + ", ";

						debugList = debugList + thisSkill.getUnitSkillID ();
					}

		log.trace ("Exiting listAllSkillsInUnitStack = " + debugList);
		return list;
	}

	/**
	 * @param unit Unit that we want to move
	 * @param unitStackSkills All the skills that any units in the stack moving with this unit have, in case any have e.g. path finding that we can take advantage of - get by calling listAllSkillsInUnitStack
	 * @param tileTypeID Type of tile we are moving onto
	 * @param spells Known spells
	 * @param db Lookup lists built over the XML database
	 * @return Double the number of movement points we will use to walk onto that tile; null = impassable
	 * @throws RecordNotFoundException If the definition of a spell that is cast on the unit cannot be found in the db
	 */
	@Override
	public final Integer calculateDoubleMovementToEnterTileType (final AvailableUnit unit, final List<String> unitStackSkills, final String tileTypeID,
		final List<MemoryMaintainedSpell> spells, final CommonDatabase db) throws RecordNotFoundException
	{
		log.trace ("Entering calculateDoubleMovementToEnterTileType: " + unit.getUnitID () + ", Player ID " + unit.getOwningPlayerID () + ", " + tileTypeID);

		// Only merge the units list of skills once
		final List<UnitSkillAndValue> unitHasSkills;
		if (unit instanceof MemoryUnit)
			unitHasSkills = getUnitUtils ().mergeSpellEffectsIntoSkillList (spells, (MemoryUnit) unit, db);
		else
			unitHasSkills = unit.getUnitHasSkill ();

		// Turn it into a list of strings so we can search it more quickly
		final List<String> unitSkills = new ArrayList<String> ();
		for (final UnitSkillAndValue thisSkill : unitHasSkills)
			unitSkills.add (thisSkill.getUnitSkillID ());

		// We basically run down the movement rate rules and stop as soon as we find the first applicable one
		// Terrain is impassable if we check every movement rule and none of them are applicable
		Integer doubleMovement = null;
		final Iterator<MovementRateRule> rules = db.getMovementRateRule ().iterator ();
		while ((doubleMovement == null) && (rules.hasNext ()))
		{
			final MovementRateRule thisRule = rules.next ();

			// All 3 parts are optional
			if (((thisRule.getTileTypeID () == null) || (thisRule.getTileTypeID ().equals (tileTypeID))) &&
				((thisRule.getUnitSkillID () == null) || (unitSkills.contains (thisRule.getUnitSkillID ()))) &&
				((thisRule.getUnitStackSkillID () == null) || (unitStackSkills.contains (thisRule.getUnitStackSkillID ()))))

				doubleMovement = thisRule.getDoubleMovement ();
		}

		log.trace ("Exiting calculateDoubleMovementToEnterTileType = " + doubleMovement);
		return doubleMovement;
	}
	
	/**
	 * @param unit Unit that we want to move
	 * @param unitStackSkills All the skills that any units in the stack moving with this unit have, in case any have e.g. path finding that we can take advantage of - get by calling listAllSkillsInUnitStack
	 * @param spells Known spells
	 * @param db Lookup lists built over the XML database
	 * @return Whether this unit can pass over every type of possible terrain on the map; i.e. true for swimming units like Lizardmen, any flying unit, or any unit stacked with a Wind Walking unit
	 * @throws RecordNotFoundException If the definition of a spell that is cast on the unit cannot be found in the db
	 */
	@Override
	public final boolean areAllTerrainTypesPassable (final AvailableUnit unit, final List<String> unitStackSkills,
		final List<MemoryMaintainedSpell> spells, final CommonDatabase db) throws RecordNotFoundException
	{
		log.trace ("Entering areAllTerrainTypesPassable: " + unit.getUnitID ());

		// Go through each tile type
		boolean result = true;
		final Iterator<? extends TileType> iter = db.getTileTypes ().iterator ();
		while ((result) && (iter.hasNext ()))
		{
			final TileType tileType = iter.next ();
			if ((!tileType.getTileTypeID ().equals (CommonDatabaseConstants.TILE_TYPE_FOG_OF_WAR_HAVE_SEEN)) &&
				(calculateDoubleMovementToEnterTileType (unit, unitStackSkills, tileType.getTileTypeID (), spells, db) == null))
				
				result = false;
		}

		log.trace ("Exiting areAllTerrainTypesPassable = " + result);
		return result;
	}
	
	/**
	 * Checks whether selectedUnits includes any transports, and if so whether the other units fit inside them, and whether any others in the same map cell should be added to the stack.
	 * See the UnitStack object for a lot more comments on the rules by which this needs to work.
	 * 
	 * @param selectedUnits Units selected by the player to move
	 * @param fogOfWarMemory Known overland terrain, units, buildings and so on 
	 * @param db Lookup lists built over the XML database
	 * @return UnitStack object
	 * @throws RecordNotFoundException If we can't find the definitions for any of the units at the location
	 * @throws MomException If selectedUnits is empty, all the units aren't at the same location, or all the units don't have the same owner 
	 */
	@Override
	public final UnitStack createUnitStack (final List<MemoryUnit> selectedUnits, final FogOfWarMemory fogOfWarMemory, final CommonDatabase db)
		throws RecordNotFoundException, MomException
	{
		log.trace ("Entering createUnitStack: " + getUnitUtils ().listUnitURNs (selectedUnits));

		// We need at least one unit, or we can't even figure out the location and owner
		if (selectedUnits.size () == 0)
			throw new MomException ("createUnitStack: Selected units list is empty");
		
		final List<String> unitStackSkills = listAllSkillsInUnitStack (selectedUnits, fogOfWarMemory.getMaintainedSpell (), db);
		
		// Count the units already in the stack into 3 categories: transports, units that want to be in transports (have some impassable terrain), and units that will accompany transports (all terrain passable)
		// Also while we're at it, find the location of the unit stack
		final List<MemoryUnit> transports = new ArrayList<MemoryUnit> ();
		int transportCapacity = 0;
		int unitsInside = 0;
		int owningPlayerID = 0;
		MapCoordinates3DEx unitLocation = null;
		
		for (final MemoryUnit thisUnit : selectedUnits)
		{
			// Categorise unit
			final Integer thisTransportCapacity = db.findUnit (thisUnit.getUnitID (), "createUnitStack").getTransportCapacity ();
			if ((thisTransportCapacity != null) && (thisTransportCapacity > 0))
			{
				transports.add (thisUnit);
				transportCapacity = transportCapacity + thisTransportCapacity;
			}
			else if (!areAllTerrainTypesPassable (thisUnit, unitStackSkills, fogOfWarMemory.getMaintainedSpell (), db))
				unitsInside++;
			
			// Record or check location
			if (unitLocation == null)
				unitLocation = (MapCoordinates3DEx) thisUnit.getUnitLocation ();
			else if (!unitLocation.equals (thisUnit.getUnitLocation ()))
				throw new MomException ("createUnitStack: All selected units are not in the same starting location");
			
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
					(unitLocation.equals (thisUnit.getUnitLocation ())) && (!selectedUnits.contains (thisUnit)))
				{
					// Never automatically add additional transports
					final Integer thisTransportCapacity = db.findUnit (thisUnit.getUnitID (), "createUnitStack").getTransportCapacity ();
					if ((thisTransportCapacity == null) || (thisTransportCapacity <= 0))
					{
						// Always automatically add "outside" units; add "inside" units only if there's still space
						if (areAllTerrainTypesPassable (thisUnit, unitStackSkills, fogOfWarMemory.getMaintainedSpell (), db))
							stack.getUnits ().add (thisUnit);
						else if (unitsInside < transportCapacity)
						{
							stack.getUnits ().add (thisUnit);
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
		
		log.trace ("Exiting createUnitStack = " + stack);
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
	 * @param ignoresCombatTerrain True if the unit has a skill with the "ignoreCombatTerrain" flag
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
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	final void processCell (final MapCoordinates2DEx moveFrom, final ExpandedUnitDetails unitBeingMoved, final boolean ignoresCombatTerrain,
		final List<MapCoordinates2DEx> cellsLeftToCheck, final int [] [] doubleMovementDistances, final int [] [] movementDirections,
		final CombatMoveType [] [] movementTypes, final boolean [] [] ourUnits, final boolean [] [] enemyUnits,
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
					
					// Our own units prevent us moving here - enemy units don't because by 'moving there' we'll attack them
					if ((doubleMovementToEnterThisTile < 0) || (ourUnits [moveTo.getY ()] [moveTo.getX ()]))
					{
						// Can't move here
						doubleMovementDistances [moveTo.getY ()] [moveTo.getX ()] = MOVEMENT_DISTANCE_IMPASSABLE;
					}

					// Can we cross the border between the two tiles?
					// i.e. check there's not a stone wall in the exit from the first cell or in the entrance to the second cell
					else if ((ignoresCombatTerrain) ||
						((okToCrossCombatTileBorder (combatMap, combatMapCoordinateSystem.getCoordinateSystemType (), moveFrom.getX (), moveFrom.getY (), d, db)) &&
						(okToCrossCombatTileBorder (combatMap, combatMapCoordinateSystem.getCoordinateSystemType (), moveTo.getX (), moveTo.getY (),
							getCoordinateSystemUtils ().normalizeDirection (combatMapCoordinateSystem.getCoordinateSystemType (), d+4), db))))
					{
						// How much movement (total) will it cost us to get here
						
						// If we ignore terrain, then we only call calculateDoubleMovementToEnterCombatTile to check if the
						// tile is impassable or not - but if its not, then override whatever value we got back
						final int newDistance = distance + (ignoresCombatTerrain ? 2 : doubleMovementToEnterThisTile);
						
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
		final CombatMoveType [] [] movementTypes, final ExpandedUnitDetails unitBeingMoved, final FogOfWarMemory fogOfWarMemory,
		final MapAreaOfCombatTiles combatMap, final CoordinateSystem combatMapCoordinateSystem,
		final List<? extends PlayerPublicDetails> players, final CommonDatabase db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.trace ("Entering calculateCombatMovementDistances: Unit URN " + unitBeingMoved.getUnitURN ());

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
		final MapCoordinates3DEx combatLocation = unitBeingMoved.getCombatLocation ();
		
		// Work this out once only
		final boolean ignoresCombatTerrain = unitBeingMoved.unitIgnoresCombatTerrain (db);
		
		// Mark locations of units on both sides (including the unit being moved)
		for (final MemoryUnit thisUnit : fogOfWarMemory.getUnit ())
			if ((combatLocation.equals (thisUnit.getCombatLocation ())) && (thisUnit.getStatus () == UnitStatusID.ALIVE) &&
				(thisUnit.getCombatPosition () != null) && (thisUnit.getCombatSide () != null) && (thisUnit.getCombatHeading () != null))
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
		final List<MapCoordinates2DEx> cellsLeftToCheck = new ArrayList<MapCoordinates2DEx> ();
		processCell (unitBeingMoved.getCombatPosition (), unitBeingMoved, ignoresCombatTerrain, cellsLeftToCheck,
			doubleMovementDistances, movementDirections, movementTypes, ourUnits, enemyUnits, combatMap, combatMapCoordinateSystem, db);
		
		// Keep going until there's nowhere left to check
		while (cellsLeftToCheck.size () > 0)
		{
			processCell (cellsLeftToCheck.get (0), unitBeingMoved, ignoresCombatTerrain, cellsLeftToCheck,
				doubleMovementDistances, movementDirections, movementTypes, ourUnits, enemyUnits, combatMap, combatMapCoordinateSystem, db);
			cellsLeftToCheck.remove (0);
		}
		
		// Now check if we can fire missile attacks at any enemies
		if (canMakeRangedAttack (unitBeingMoved.getMemoryUnit (), players, fogOfWarMemory, db))
			for (int y = 0; y < combatMapCoordinateSystem.getHeight (); y++)
				for (int x = 0; x < combatMapCoordinateSystem.getWidth (); x++)
					if (enemyUnits [y] [x])
					{
						// Firing a missle weapon always uses up all of our movement so mark this for the sake of it - although MovementDistances
						// isn't actually used to reduce the movement a unit has left in this fashion
						movementTypes [y] [x] = CombatMoveType.RANGED;
						doubleMovementDistances [y] [x] = 999;
					}
		
		log.trace ("Exiting calculateCombatMovementDistances");
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
	 * @return Unit skill utils
	 */
	public final UnitSkillUtils getUnitSkillUtils ()
	{
		return unitSkillUtils;
	}

	/**
	 * @param utils Unit skill utils
	 */
	public final void setUnitSkillUtils (final UnitSkillUtils utils)
	{
		unitSkillUtils = utils;
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