package momime.common.calculations;

import java.util.List;
import java.util.logging.Logger;

import momime.common.database.CommonDatabaseLookup;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.CoordinatesUtils;
import momime.common.messages.PlayerPickUtils;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.common.messages.v0_9_4.PlayerPick;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.SquareMapDirection;

/**
 * Common calculations pertaining to units
 */
public final class MomUnitCalculations
{
	/**
	 * @param map Our knowledge of the surrounding terrain
	 * @param buildings Pre-locked buildings list
	 * @param cityLocation Location of the city the unit is being constructed at
	 * @param picks Picks of the player who owns the city
	 * @param overlandMapCoordinateSystem Coordinate system for traversing overland map
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Weapon grade that the unit that we build here will have
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 */
	public static final int calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort
		(final List<MemoryBuilding> buildings, final MapVolumeOfMemoryGridCells map, final OverlandMapCoordinates cityLocation,
		final List<PlayerPick> picks, final CoordinateSystem overlandMapCoordinateSystem, final CommonDatabaseLookup db, final Logger debugLogger) throws RecordNotFoundException
	{
		debugLogger.entering (MomUnitCalculations.class.getName (), "calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort", cityLocation);

		// First look for a building that grants magical weapons, i.e. an Alchemists' Guild
		int bestWeaponGrade = 0;
		for (final MemoryBuilding thisBuilding : buildings)
			if (CoordinatesUtils.overlandMapCoordinatesEqual (thisBuilding.getCityLocation (), cityLocation, true))
			{
				final Integer weaponGradeFromBuilding = db.findBuilding (thisBuilding.getBuildingID (), "calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort").getBuildingMagicWeapons ();
				if ((weaponGradeFromBuilding != null) && (weaponGradeFromBuilding > bestWeaponGrade))
					bestWeaponGrade = weaponGradeFromBuilding;
			}

		// Check surrounding tiles, i.e. look for Mithril or Adamantium Ore
		// We can only use these if we found a building that granted some level of magic weapons
		if (bestWeaponGrade > 0)
		{
			final OverlandMapCoordinates coords = new OverlandMapCoordinates ();
			coords.setX (cityLocation.getX ());
			coords.setY (cityLocation.getY ());
			coords.setPlane (cityLocation.getPlane ());

			for (final SquareMapDirection direction : MomCityCalculations.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
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
		final int weaponGradeFromPicks = PlayerPickUtils.getHighestWeaponGradeGrantedByPicks (picks, db, debugLogger);
		if (weaponGradeFromPicks > bestWeaponGrade)
			bestWeaponGrade = weaponGradeFromPicks;

		debugLogger.exiting (MomUnitCalculations.class.getName (), "calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort", bestWeaponGrade);
		return bestWeaponGrade;
	}

	/**
	 * Prevent instantiation
	 */
	private MomUnitCalculations ()
	{
	}
}
