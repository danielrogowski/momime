package momime.server.fogofwar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import momime.common.database.v0_9_4.UnitHasSkill;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.MemoryCombatAreaEffect;
import momime.common.messages.v0_9_4.MemoryGridCell;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.UnitUtils;
import momime.server.ServerTestData;
import momime.server.database.ServerDatabaseEx;

import org.junit.Test;

/**
 * Tests the FogOfWarDuplication class
 */
public final class TestFogOfWarDuplication
{
	/**
	 * Tests the copyTerrainAndNodeAura method
	 */
	@Test
	public final void testCopyTerrainAndNodeAura ()
	{
		final FogOfWarDuplication dup = new FogOfWarDuplication ();
		
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
		final FogOfWarDuplication dup = new FogOfWarDuplication ();

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
		final FogOfWarDuplication dup = new FogOfWarDuplication ();

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
		final FogOfWarDuplication dup = new FogOfWarDuplication ();

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
		final FogOfWarDuplication dup = new FogOfWarDuplication ();
		dup.setMemoryBuildingUtils (new MemoryBuildingUtils ());

		final List<MemoryBuilding> destination = new ArrayList<MemoryBuilding> ();

		// Put 3 buildings into the list
		for (int n = 1; n <= 3; n++)
		{
			final OverlandMapCoordinatesEx buildingCoords = new OverlandMapCoordinatesEx ();
			buildingCoords.setX (20 + n);
			buildingCoords.setY (10 + n);
			buildingCoords.setPlane (1);

			final MemoryBuilding building = new MemoryBuilding ();
			building.setBuildingID ("BL0" + n);
			building.setCityLocation (buildingCoords);

			destination.add (building);
		}

		// Test a building already in the list
		final OverlandMapCoordinatesEx existingCoords = new OverlandMapCoordinatesEx ();
		existingCoords.setX (22);
		existingCoords.setY (12);
		existingCoords.setPlane (1);

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
		assertEquals (1, destination.get (3).getCityLocation ().getPlane ());
	}

	/**
	 * Tests the copyUnit method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCopyUnit () throws Exception
	{
		final FogOfWarDuplication dup = new FogOfWarDuplication ();
		final UnitUtils utils = new UnitUtils ();
		dup.setUnitUtils (utils);

		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		final List<MemoryUnit> destination = new ArrayList<MemoryUnit> ();

		// First unit (spearmen)
		final OverlandMapCoordinatesEx unitOneLocation = new OverlandMapCoordinatesEx ();
		unitOneLocation.setX (22);
		unitOneLocation.setY (12);
		unitOneLocation.setPlane (1);

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
		final OverlandMapCoordinatesEx unitTwoLocation = new OverlandMapCoordinatesEx ();
		unitTwoLocation.setX (22);
		unitTwoLocation.setY (12);
		unitTwoLocation.setPlane (1);

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
		final FogOfWarDuplication dup = new FogOfWarDuplication ();
		dup.setMemoryMaintainedSpellUtils (new MemoryMaintainedSpellUtils ());

		final List<MemoryMaintainedSpell> destination = new ArrayList<MemoryMaintainedSpell> ();

		// Put 3 spells into the list
		for (int n = 1; n <= 3; n++)
		{
			final OverlandMapCoordinatesEx spellCoords = new OverlandMapCoordinatesEx ();
			spellCoords.setX (20 + n);
			spellCoords.setY (10 + n);
			spellCoords.setPlane (1);

			final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
			spell.setSpellID ("SP00" + n);
			spell.setCityLocation (spellCoords);
			spell.setCastingPlayerID (n);

			destination.add (spell);
		}

		// Test a spell already in the list
		final OverlandMapCoordinatesEx existingCoords = new OverlandMapCoordinatesEx ();
		existingCoords.setX (22);
		existingCoords.setY (12);
		existingCoords.setPlane (1);

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
		assertEquals (1, destination.get (3).getCityLocation ().getPlane ());
	}

	/**
	 * Tests the copyCombatAreaEffect method
	 */
	@Test
	public final void testCopyCombatAreaEffect ()
	{
		final FogOfWarDuplication dup = new FogOfWarDuplication ();
		dup.setMemoryCombatAreaEffectUtils (new MemoryCombatAreaEffectUtils ());

		final List<MemoryCombatAreaEffect> destination = new ArrayList<MemoryCombatAreaEffect> ();

		// Put 3 combatAreaEffects into the list
		for (int n = 1; n <= 3; n++)
		{
			final OverlandMapCoordinatesEx combatAreaEffectCoords = new OverlandMapCoordinatesEx ();
			combatAreaEffectCoords.setX (20 + n);
			combatAreaEffectCoords.setY (10 + n);
			combatAreaEffectCoords.setPlane (1);

			final MemoryCombatAreaEffect combatAreaEffect = new MemoryCombatAreaEffect ();
			combatAreaEffect.setCombatAreaEffectID ("CAE0" + n);
			combatAreaEffect.setMapLocation (combatAreaEffectCoords);
			combatAreaEffect.setCastingPlayerID (n);

			destination.add (combatAreaEffect);
		}

		// Test a combatAreaEffect already in the list
		final OverlandMapCoordinatesEx existingCoords = new OverlandMapCoordinatesEx ();
		existingCoords.setX (22);
		existingCoords.setY (12);
		existingCoords.setPlane (1);

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
		assertEquals (1, destination.get (3).getMapLocation ().getPlane ());
	}
}
