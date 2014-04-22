package momime.server.fogofwar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import momime.common.database.v0_9_4.UnitHasSkill;
import momime.common.messages.v0_9_5.MemoryBuilding;
import momime.common.messages.v0_9_5.MemoryCombatAreaEffect;
import momime.common.messages.v0_9_5.MemoryGridCell;
import momime.common.messages.v0_9_5.MemoryMaintainedSpell;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.messages.v0_9_5.OverlandMapCityData;
import momime.common.messages.v0_9_5.OverlandMapTerrainData;
import momime.common.utils.MemoryBuildingUtilsImpl;
import momime.common.utils.MemoryCombatAreaEffectUtilsImpl;
import momime.common.utils.MemoryMaintainedSpellUtilsImpl;
import momime.common.utils.UnitUtilsImpl;
import momime.server.ServerTestData;
import momime.server.database.ServerDatabaseEx;

import org.junit.Test;

import com.ndg.map.coordinates.MapCoordinates3DEx;

/**
 * Tests the FogOfWarDuplication class
 */
public final class TestFogOfWarDuplicationImpl
{
	/**
	 * Tests the copyTerrainAndNodeAura method
	 */
	@Test
	public final void testCopyTerrainAndNodeAura ()
	{
		final FogOfWarDuplicationImpl dup = new FogOfWarDuplicationImpl ();
		
		// Source terrain data is mandatory; destination terrain data is optional
		final OverlandMapTerrainData sourceData = new OverlandMapTerrainData ();
		final MemoryGridCell source = new MemoryGridCell ();
		final MemoryGridCell destination = new MemoryGridCell ();
		source.setTerrainData (sourceData);

		// Considered an update even though there's no source data because destination has no terrainData yet
		// Every time we do a test, repeat it without changing anything to show that we get false back
		assertTrue (dup.copyTerrainAndNodeAura (source, destination));
		assertNotNull (destination.getTerrainData ());
		assertFalse (dup.copyTerrainAndNodeAura (source, destination));

		// Tile type
		sourceData.setTileTypeID ("A");
		assertTrue (dup.copyTerrainAndNodeAura (source, destination));
		assertEquals ("A", destination.getTerrainData ().getTileTypeID ());
		assertFalse (dup.copyTerrainAndNodeAura (source, destination));

		// Map feature
		sourceData.setMapFeatureID ("B");
		assertTrue (dup.copyTerrainAndNodeAura (source, destination));
		assertEquals ("B", destination.getTerrainData ().getMapFeatureID ());
		assertFalse (dup.copyTerrainAndNodeAura (source, destination));

		// River directions
		sourceData.setRiverDirections ("C");
		assertTrue (dup.copyTerrainAndNodeAura (source, destination));
		assertEquals ("C", destination.getTerrainData ().getRiverDirections ());
		assertFalse (dup.copyTerrainAndNodeAura (source, destination));

		// Node owner
		sourceData.setNodeOwnerID (1);
		assertTrue (dup.copyTerrainAndNodeAura (source, destination));
		assertEquals (1, destination.getTerrainData ().getNodeOwnerID ().intValue ());
		assertFalse (dup.copyTerrainAndNodeAura (source, destination));

		// Change to another actual value
		sourceData.setTileTypeID ("D");
		assertTrue (dup.copyTerrainAndNodeAura (source, destination));
		assertEquals ("D", destination.getTerrainData ().getTileTypeID ());
		assertFalse (dup.copyTerrainAndNodeAura (source, destination));

		// Change to null
		sourceData.setTileTypeID (null);
		assertTrue (dup.copyTerrainAndNodeAura (source, destination));
		assertNull (destination.getTerrainData ().getTileTypeID ());
		assertFalse (dup.copyTerrainAndNodeAura (source, destination));
	}

	/**
	 * Tests the blankTerrainAndNodeAura method
	 */
	@Test
	public final void testBlankTerrainAndNodeAura ()
	{
		final FogOfWarDuplicationImpl dup = new FogOfWarDuplicationImpl ();

		// Set one of the values to an actual value
		OverlandMapTerrainData destinationData = new OverlandMapTerrainData ();
		final MemoryGridCell destination = new MemoryGridCell ();
		destination.setTerrainData (destinationData);

		destinationData.setTileTypeID ("A");
		assertTrue (dup.blankTerrainAndNodeAura (destination));
		assertNull (destination.getTerrainData ());

		// Setting destination but leaving all the values none doesn't count as an update
		destinationData = new OverlandMapTerrainData ();
		destination.setTerrainData (destinationData);

		assertFalse (dup.blankTerrainAndNodeAura (destination));
		assertNull (destination.getTerrainData ());

		// Try when destination data itself is null
		assertFalse (dup.blankTerrainAndNodeAura (destination));
		assertNull (destination.getTerrainData ());
	}

	/**
	 * Tests the copyCityData method
	 */
	@Test
	public final void testCopyCityData ()
	{
		final FogOfWarDuplicationImpl dup = new FogOfWarDuplicationImpl ();

		// Source terrain data is mandatory; destination terrain data is optional
		final OverlandMapCityData sourceData = new OverlandMapCityData ();
		final MemoryGridCell source = new MemoryGridCell ();
		final MemoryGridCell destination = new MemoryGridCell ();
		source.setCityData (sourceData);

		// Considered an update even though there's no source data because destination has no cityData yet
		// Every time we do a test, repeat it without changing anything to show that we get false back
		assertTrue (dup.copyCityData (source, destination, true));
		assertNotNull (destination.getCityData ());
		assertFalse (dup.copyCityData (source, destination, true));

		// City population
		sourceData.setCityPopulation (1);
		assertTrue (dup.copyCityData (source, destination, true));
		assertEquals (1, destination.getCityData ().getCityPopulation ().intValue ());
		assertFalse (dup.copyCityData (source, destination, true));

		// Number of rebels
		sourceData.setNumberOfRebels (2);
		assertTrue (dup.copyCityData (source, destination, true));
		assertEquals (2, destination.getCityData ().getNumberOfRebels ().intValue ());
		assertFalse (dup.copyCityData (source, destination, true));

		// Minimum farmers
		sourceData.setMinimumFarmers (3);
		assertTrue (dup.copyCityData (source, destination, true));
		assertEquals (3, destination.getCityData ().getMinimumFarmers ().intValue ());
		assertFalse (dup.copyCityData (source, destination, true));

		// Optional farmers
		sourceData.setOptionalFarmers (4);
		assertTrue (dup.copyCityData (source, destination, true));
		assertEquals (4, destination.getCityData ().getOptionalFarmers ().intValue ());
		assertFalse (dup.copyCityData (source, destination, true));

		// City owner
		sourceData.setCityOwnerID (5);
		assertTrue (dup.copyCityData (source, destination, true));
		assertEquals (5, destination.getCityData ().getCityOwnerID ().intValue ());
		assertFalse (dup.copyCityData (source, destination, true));

		// City race
		sourceData.setCityRaceID ("A");
		assertTrue (dup.copyCityData (source, destination, true));
		assertEquals ("A", destination.getCityData ().getCityRaceID ());
		assertFalse (dup.copyCityData (source, destination, true));

		// City size
		sourceData.setCitySizeID ("B");
		assertTrue (dup.copyCityData (source, destination, true));
		assertEquals ("B", destination.getCityData ().getCitySizeID ());
		assertFalse (dup.copyCityData (source, destination, true));

		// City name
		sourceData.setCityName ("C");
		assertTrue (dup.copyCityData (source, destination, true));
		assertEquals ("C", destination.getCityData ().getCityName ());
		assertFalse (dup.copyCityData (source, destination, true));

		// Currently constructing
		sourceData.setCurrentlyConstructingBuildingOrUnitID ("D");
		assertTrue (dup.copyCityData (source, destination, true));
		assertEquals ("D", destination.getCityData ().getCurrentlyConstructingBuildingOrUnitID ());
		assertFalse (dup.copyCityData (source, destination, true));

		// Change to another actual value
		sourceData.setCityRaceID ("E");
		assertTrue (dup.copyCityData (source, destination, true));
		assertEquals ("E", destination.getCityData ().getCityRaceID ());
		assertFalse (dup.copyCityData (source, destination, true));

		// Change to null
		sourceData.setCityRaceID (null);
		assertTrue (dup.copyCityData (source, destination, true));
		assertNull (destination.getCityData ().getCityRaceID ());
		assertFalse (dup.copyCityData (source, destination, true));

		// Currently constructing gets blanked out if we pass in null, so since it currently has a value, that counts as a change
		assertTrue (dup.copyCityData (source, destination, false));
		assertNull (destination.getCityData ().getCurrentlyConstructingBuildingOrUnitID ());

		// Any change to its value doesn't count as a change
		sourceData.setCurrentlyConstructingBuildingOrUnitID ("F");
		assertFalse (dup.copyCityData (source, destination, false));
	}

	/**
	 * Tests the blankCityData method
	 */
	@Test
	public final void testBlankCityData ()
	{
		final FogOfWarDuplicationImpl dup = new FogOfWarDuplicationImpl ();

		// Set one of the values to an actual value
		OverlandMapCityData destinationData = new OverlandMapCityData ();
		final MemoryGridCell destination = new MemoryGridCell ();
		destination.setCityData (destinationData);

		destinationData.setCityRaceID ("A");
		assertTrue (dup.blankCityData (destination));
		assertNull (destination.getCityData ());

		// Setting destination but leaving all the values none doesn't count as an update
		destinationData = new OverlandMapCityData ();
		destination.setCityData (destinationData);

		assertFalse (dup.blankCityData (destination));
		assertNull (destination.getCityData ());

		// Try when destination data itself is null
		assertFalse (dup.blankCityData (destination));
		assertNull (destination.getCityData ());
	}

	/**
	 * Tests the copyBuilding method
	 */
	@Test
	public final void testCopyBuilding ()
	{
		final FogOfWarDuplicationImpl dup = new FogOfWarDuplicationImpl ();
		dup.setMemoryBuildingUtils (new MemoryBuildingUtilsImpl ());

		final List<MemoryBuilding> destination = new ArrayList<MemoryBuilding> ();

		// Put 3 buildings into the list
		for (int n = 1; n <= 3; n++)
		{
			final MapCoordinates3DEx buildingCoords = new MapCoordinates3DEx (20 + n, 10 + n, 1);

			final MemoryBuilding building = new MemoryBuilding ();
			building.setBuildingID ("BL0" + n);
			building.setCityLocation (buildingCoords);

			destination.add (building);
		}

		// Test a building already in the list
		final MapCoordinates3DEx existingCoords = new MapCoordinates3DEx (22, 12, 1);

		final MemoryBuilding existingBuilding = new MemoryBuilding ();
		existingBuilding.setBuildingID ("BL02");
		existingBuilding.setCityLocation (existingCoords);

		assertEquals (3, destination.size ());
		assertFalse (dup.copyBuilding (existingBuilding, destination));
		assertEquals (3, destination.size ());

		// Test a building already in the list (location same but different building ID)
		final MemoryBuilding newBuilding = new MemoryBuilding ();
		newBuilding.setBuildingID ("BL03");
		newBuilding.setCityLocation (existingCoords);

		assertEquals (3, destination.size ());
		assertTrue (dup.copyBuilding (newBuilding, destination));

		assertEquals (4, destination.size ());
		assertEquals ("BL03", destination.get (3).getBuildingID ());
		assertEquals (22, destination.get (3).getCityLocation ().getX ());
		assertEquals (12, destination.get (3).getCityLocation ().getY ());
		assertEquals (1, destination.get (3).getCityLocation ().getZ ());
	}

	/**
	 * Tests the copyUnit method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCopyUnit () throws Exception
	{
		final FogOfWarDuplicationImpl dup = new FogOfWarDuplicationImpl ();
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		dup.setUnitUtils (utils);

		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		final List<MemoryUnit> destination = new ArrayList<MemoryUnit> ();

		// First unit (spearmen)
		final MapCoordinates3DEx unitOneLocation = new MapCoordinates3DEx (22, 12, 1);

		final MemoryUnit unitOne = utils.createMemoryUnit ("UN105", 1, 1, 10, true, db);
		unitOne.setUnitLocation (unitOneLocation);
		unitOne.setOwningPlayerID (2);

		assertTrue (dup.copyUnit (unitOne, destination));

		// Check again without changing anything
		assertFalse (dup.copyUnit (unitOne, destination));

		// Change a value
		unitOne.setDamageTaken (1);
		assertTrue (dup.copyUnit (unitOne, destination));
		assertFalse (dup.copyUnit (unitOne, destination));

		// Second unit (magicians)
		final MapCoordinates3DEx unitTwoLocation = new MapCoordinates3DEx (22, 12, 1);

		final MemoryUnit unitTwo = utils.createMemoryUnit ("UN052", 2, 0, 25, true, db);
		unitTwo.setUnitLocation (unitTwoLocation);
		unitTwo.setOwningPlayerID (2);

		assertTrue (dup.copyUnit (unitTwo, destination));
		assertFalse (dup.copyUnit (unitTwo, destination));

		// Give them more ammo
		utils.setBasicSkillValue (unitTwo, "US132", 20);
		assertTrue (dup.copyUnit (unitTwo, destination));
		assertFalse (dup.copyUnit (unitTwo, destination));

		// Cast flight on them (ok so normally this is done via the spells list and merging that into the unit skills list, but this is what's appropriate for this test...)
		final UnitHasSkill flight = new UnitHasSkill ();
		flight.setUnitSkillID ("SS056");
		unitTwo.getUnitHasSkill ().add (flight);

		assertTrue (dup.copyUnit (unitTwo, destination));
		assertFalse (dup.copyUnit (unitTwo, destination));
	}

	/**
	 * Tests the copyMaintainedSpell method
	 */
	@Test
	public final void testCopyMaintainedSpell ()
	{
		final FogOfWarDuplicationImpl dup = new FogOfWarDuplicationImpl ();
		dup.setMemoryMaintainedSpellUtils (new MemoryMaintainedSpellUtilsImpl ());

		final List<MemoryMaintainedSpell> destination = new ArrayList<MemoryMaintainedSpell> ();

		// Put 3 spells into the list
		for (int n = 1; n <= 3; n++)
		{
			final MapCoordinates3DEx spellCoords = new MapCoordinates3DEx (20 + n, 10 + n, 1);

			final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
			spell.setSpellID ("SP00" + n);
			spell.setCityLocation (spellCoords);
			spell.setCastingPlayerID (n);

			destination.add (spell);
		}

		// Test a spell already in the list
		final MapCoordinates3DEx existingCoords = new MapCoordinates3DEx (22, 12, 1);

		final MemoryMaintainedSpell existingSpell = new MemoryMaintainedSpell ();
		existingSpell.setSpellID ("SP002");
		existingSpell.setCityLocation (existingCoords);
		existingSpell.setCastingPlayerID (2);

		assertEquals (3, destination.size ());
		assertFalse (dup.copyMaintainedSpell (existingSpell, destination));
		assertEquals (3, destination.size ());

		// Test a spell already in the list (location same but different spell ID)
		final MemoryMaintainedSpell newSpell = new MemoryMaintainedSpell ();
		newSpell.setSpellID ("SP003");
		newSpell.setCityLocation (existingCoords);
		newSpell.setCastingPlayerID (3);

		assertEquals (3, destination.size ());
		assertTrue (dup.copyMaintainedSpell (newSpell, destination));

		assertEquals (4, destination.size ());
		assertEquals ("SP003", destination.get (3).getSpellID ());
		assertEquals (3, destination.get (3).getCastingPlayerID ());
		assertEquals (22, destination.get (3).getCityLocation ().getX ());
		assertEquals (12, destination.get (3).getCityLocation ().getY ());
		assertEquals (1, destination.get (3).getCityLocation ().getZ ());
	}

	/**
	 * Tests the copyCombatAreaEffect method
	 */
	@Test
	public final void testCopyCombatAreaEffect ()
	{
		final FogOfWarDuplicationImpl dup = new FogOfWarDuplicationImpl ();
		dup.setMemoryCombatAreaEffectUtils (new MemoryCombatAreaEffectUtilsImpl ());

		final List<MemoryCombatAreaEffect> destination = new ArrayList<MemoryCombatAreaEffect> ();

		// Put 3 combatAreaEffects into the list
		for (int n = 1; n <= 3; n++)
		{
			final MapCoordinates3DEx combatAreaEffectCoords = new MapCoordinates3DEx (20 + n, 10 + n, 1);

			final MemoryCombatAreaEffect combatAreaEffect = new MemoryCombatAreaEffect ();
			combatAreaEffect.setCombatAreaEffectID ("CAE0" + n);
			combatAreaEffect.setMapLocation (combatAreaEffectCoords);
			combatAreaEffect.setCastingPlayerID (n);

			destination.add (combatAreaEffect);
		}

		// Test a combatAreaEffect already in the list
		final MapCoordinates3DEx existingCoords = new MapCoordinates3DEx (22, 12, 1);

		final MemoryCombatAreaEffect existingCombatAreaEffect = new MemoryCombatAreaEffect ();
		existingCombatAreaEffect.setCombatAreaEffectID ("CAE02");
		existingCombatAreaEffect.setMapLocation (existingCoords);
		existingCombatAreaEffect.setCastingPlayerID (2);

		assertEquals (3, destination.size ());
		assertFalse (dup.copyCombatAreaEffect (existingCombatAreaEffect, destination));
		assertEquals (3, destination.size ());

		// Test a combatAreaEffect already in the list (location same but different combatAreaEffect ID)
		final MemoryCombatAreaEffect newCombatAreaEffect = new MemoryCombatAreaEffect ();
		newCombatAreaEffect.setCombatAreaEffectID ("CAE03");
		newCombatAreaEffect.setMapLocation (existingCoords);
		newCombatAreaEffect.setCastingPlayerID (3);

		assertEquals (3, destination.size ());
		assertTrue (dup.copyCombatAreaEffect (newCombatAreaEffect, destination));

		assertEquals (4, destination.size ());
		assertEquals ("CAE03", destination.get (3).getCombatAreaEffectID ());
		assertEquals (3, destination.get (3).getCastingPlayerID ().intValue ());
		assertEquals (22, destination.get (3).getMapLocation ().getX ());
		assertEquals (12, destination.get (3).getMapLocation ().getY ());
		assertEquals (1, destination.get (3).getMapLocation ().getZ ());
	}
}
