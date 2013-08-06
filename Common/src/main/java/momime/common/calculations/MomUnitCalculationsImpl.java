package momime.common.calculations;

import java.util.List;
import java.util.logging.Logger;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.CombatMapLayerID;
import momime.common.database.v0_9_4.CombatTileBorderBlocksMovementID;
import momime.common.database.v0_9_4.UnitHasSkill;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.v0_9_4.AvailableUnit;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.MemoryCombatAreaEffect;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomCombatTile;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.common.messages.v0_9_4.PlayerPick;
import momime.common.utils.CombatMapUtils;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.UnitUtils;

import com.ndg.map.CoordinateSystem;
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
	
	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/** Combat map utils */
	private CombatMapUtils combatMapUtils;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
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
			coords.setPlane (cityLocation.getPlane ());

			for (final SquareMapDirection direction : MomCityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
			{
				if (CoordinateSystemUtils.moveCoordinates (overlandMapCoordinateSystem, coords, direction.getDirectionID ()))
				{
					final OverlandMapTerrainData terrainData = map.getPlane ().get (coords.getPlane ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
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
}
