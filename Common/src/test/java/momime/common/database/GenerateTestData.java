package momime.common.database;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemType;

import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MapAreaOfMemoryGridCells;
import momime.common.messages.MapRowOfCombatTiles;
import momime.common.messages.MapRowOfMemoryGridCells;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MomCombatTile;

/**
 * Utility methods for unit tests, such as creating coordinate systems and blank maps.
 */
public final class GenerateTestData
{
	/**
	 *	<spellSetting spellSettingID="SS01">
	 *		<switchResearch>D</switchResearch>
	 *		<spellBooksToObtainFirstReduction>8</spellBooksToObtainFirstReduction>
	 *		<spellBooksCastingReduction>10</spellBooksCastingReduction>
	 *		<spellBooksCastingReductionCap>100</spellBooksCastingReductionCap>
	 *		<spellBooksCastingReductionCombination>A</spellBooksCastingReductionCombination>
	 *		<spellBooksResearchBonus>10</spellBooksResearchBonus>
	 *		<spellBooksResearchBonusCap>1000</spellBooksResearchBonusCap>
	 *		<spellBooksResearchBonusCombination>A</spellBooksResearchBonusCombination>
	 *		<spellSettingDescription>Original</spellSettingDescription>
	 *	</spellSetting>
	 *
	 * @return Spell settings, configured like the original spell settings in the server XML file
	 */
	public final static SpellSetting createOriginalSpellSettings ()
	{
		final SpellSetting settings = new SpellSetting ();
		settings.setSwitchResearch (SwitchResearch.DISALLOWED);
		settings.setSpellBooksToObtainFirstReduction (8);
		settings.setSpellBooksCastingReduction (10);
		settings.setSpellBooksCastingReductionCap (100);
		settings.setSpellBooksCastingReductionCombination (CastingReductionCombination.ADDITIVE);
		settings.setSpellBooksResearchBonus (10);
		settings.setSpellBooksResearchBonusCap (1000);
		settings.setSpellBooksResearchBonusCombination (CastingReductionCombination.ADDITIVE);
		return settings;
	}

	/**
	 *	<spellSetting spellSettingID="SS02">
	 *		<switchResearch>F</switchResearch>
	 *		<spellBooksToObtainFirstReduction>8</spellBooksToObtainFirstReduction>
	 *		<spellBooksCastingReduction>8</spellBooksCastingReduction>
	 *		<spellBooksCastingReductionCap>90</spellBooksCastingReductionCap>
	 *		<spellBooksCastingReductionCombination>M</spellBooksCastingReductionCombination>
	 *		<spellBooksResearchBonus>10</spellBooksResearchBonus>
	 *		<spellBooksResearchBonusCap>1000</spellBooksResearchBonusCap>
	 *		<spellBooksResearchBonusCombination>A</spellBooksResearchBonusCombination>
	 *		<spellSettingDescription>Recommended</spellSettingDescription>
	 *	</spellSetting>

	 * @return Spell settings, configured like the recommended spell settings in the server XML file
	 */
	public final static SpellSetting createRecommendedSpellSettings ()
	{
		final SpellSetting settings = new SpellSetting ();
		settings.setSwitchResearch (SwitchResearch.FREE);
		settings.setSpellBooksToObtainFirstReduction (8);
		settings.setSpellBooksCastingReduction (8);
		settings.setSpellBooksCastingReductionCap (90);
		settings.setSpellBooksCastingReductionCombination (CastingReductionCombination.MULTIPLICATIVE);
		settings.setSpellBooksResearchBonus (10);
		settings.setSpellBooksResearchBonusCap (1000);
		settings.setSpellBooksResearchBonusCombination (CastingReductionCombination.ADDITIVE);
		return settings;
	}

	/**
	 * @return Demo MoM overland map-like coordinate system with a 60x40 square map wrapping left-to-right but not top-to-bottom
	 */
	public final static CoordinateSystem createOverlandMapCoordinateSystem ()
	{
		final CoordinateSystem sys = new CoordinateSystem ();
		sys.setCoordinateSystemType (CoordinateSystemType.SQUARE);
		sys.setWidth (60);
		sys.setHeight (40);
		sys.setDepth (2);
		sys.setWrapsLeftToRight (true);
		return sys;
	}

	/**
	 * @return Overland map coordinate system that can be included into session description
	 */
	public final static OverlandMapSize createOverlandMapSize ()
	{
		final OverlandMapSize sys = new OverlandMapSize ();
		sys.setCoordinateSystemType (CoordinateSystemType.SQUARE);
		sys.setWidth (60);
		sys.setHeight (40);
		sys.setDepth (2);
		sys.setWrapsLeftToRight (true);
		
		sys.setCitySeparation (3);
		sys.setContinentalRaceChance (75);
		
		return sys;
	}
	
	/**
	 * @param sys Overland map coordinate system
	 * @return Map area prepopulated with empty cells
	 */
	public final static MapVolumeOfMemoryGridCells createOverlandMap (final CoordinateSystem sys)
	{
		final MapVolumeOfMemoryGridCells map = new MapVolumeOfMemoryGridCells ();
		for (int plane = 0; plane < 2; plane++)
		{
			final MapAreaOfMemoryGridCells area = new MapAreaOfMemoryGridCells ();
			for (int y = 0; y < sys.getHeight (); y++)
			{
				final MapRowOfMemoryGridCells row = new MapRowOfMemoryGridCells ();
				for (int x = 0; x < sys.getWidth (); x++)
					row.getCell ().add (new MemoryGridCell ());

				area.getRow ().add (row);
			}

			map.getPlane ().add (area);
		}

		return map;
	}

	/**
	 * @return Demo MoM combat map-like coordinate system with a 60x40 diamond non-wrapping map
	 */
	public final static CoordinateSystem createCombatMapCoordinateSystem ()
	{
		final CoordinateSystem sys = new CoordinateSystem ();
		sys.setCoordinateSystemType (CoordinateSystemType.DIAMOND);
		sys.setWidth (12);
		sys.setHeight (25);
		return sys;
	}

	/**
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @return Map area prepopulated with empty cells
	 */
	public final static MapAreaOfCombatTiles createCombatMap (final CoordinateSystem combatMapCoordinateSystem)
	{
		final MapAreaOfCombatTiles map = new MapAreaOfCombatTiles ();
		for (int y = 0; y < combatMapCoordinateSystem.getHeight (); y++)
		{
			final MapRowOfCombatTiles row = new MapRowOfCombatTiles ();
			for (int x = 0; x < combatMapCoordinateSystem.getWidth (); x++)
				row.getCell ().add (new MomCombatTile ());

			map.getRow ().add (row);
		}

		return map;
	}

	/**
	 * Prevent instantiation
	 */
	private GenerateTestData ()
	{
	}
}