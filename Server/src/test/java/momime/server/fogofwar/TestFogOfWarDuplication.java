package momime.server.fogofwar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import momime.common.database.v0_9_4.UnitHasSkill;
import momime.common.messages.UnitUtils;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.MemoryCombatAreaEffect;
import momime.common.messages.v0_9_4.MemoryGridCell;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.server.ServerTestData;
import momime.server.database.ServerDatabaseEx;

import org.junit.Test;

/**
 * Tests the FogOfWarDuplication class
 */
public final class TestFogOfWarDuplication
{
	/** Dummy logger to use during unit tests */
	private final Logger debugLogger = Logger.getLogger ("MoMIMEServerUnitTests");

	/**
	 * Tests the copyTerrainAndNodeAura method
	 */
	@Test
	public final void testCopyTerrainAndNodeAura ()
	{
		// Source terrain data is mandatory; destination terrain data is optional
		final OverlandMapTerrainData sourceData = new OverlandMapTerrainData ();
		final MemoryGridCell source = new MemoryGridCell ();
		final MemoryGridCell destination = new MemoryGridCell ();
		source.setTerrainData (sourceData);

		// Considered an update even though there's no source data because destination has no terrainData yet
		// Every time we do a test, repeat it without changing anything to show that we get false back
		assertTrue (FogOfWarDuplication.copyTerrainAndNodeAura (source, destination));
		assertNotNull (destination.getTerrainData ());
		assertFalse (FogOfWarDuplication.copyTerrainAndNodeAura (source, destination));

		// Tile type
		sourceData.setTileTypeID ("A");
		assertTrue (FogOfWarDuplication.copyTerrainAndNodeAura (source, destination));
		assertEquals ("A", destination.getTerrainData ().getTileTypeID ());
		assertFalse (FogOfWarDuplication.copyTerrainAndNodeAura (source, destination));

		// Map feature
		sourceData.setMapFeatureID ("B");
		assertTrue (FogOfWarDuplication.copyTerrainAndNodeAura (source, destination));
		assertEquals ("B", destination.getTerrainData ().getMapFeatureID ());
		assertFalse (FogOfWarDuplication.copyTerrainAndNodeAura (source, destination));

		// River directions
		sourceData.setRiverDirections ("C");
		assertTrue (FogOfWarDuplication.copyTerrainAndNodeAura (source, destination));
		assertEquals ("C", destination.getTerrainData ().getRiverDirections ());
		assertFalse (FogOfWarDuplication.copyTerrainAndNodeAura (source, destination));

		// Node owner
		sourceData.setNodeOwnerID (1);
		assertTrue (FogOfWarDuplication.copyTerrainAndNodeAura (source, destination));
		assertEquals (1, destination.getTerrainData ().getNodeOwnerID ().intValue ());
		assertFalse (FogOfWarDuplication.copyTerrainAndNodeAura (source, destination));

		// Change to another actual value
		sourceData.setTileTypeID ("D");
		assertTrue (FogOfWarDuplication.copyTerrainAndNodeAura (source, destination));
		assertEquals ("D", destination.getTerrainData ().getTileTypeID ());
		assertFalse (FogOfWarDuplication.copyTerrainAndNodeAura (source, destination));

		// Change to null
		sourceData.setTileTypeID (null);
		assertTrue (FogOfWarDuplication.copyTerrainAndNodeAura (source, destination));
		assertNull (destination.getTerrainData ().getTileTypeID ());
		assertFalse (FogOfWarDuplication.copyTerrainAndNodeAura (source, destination));
	}

	/**
	 * Tests the blankTerrainAndNodeAura method
	 */
	@Test
	public final void testBlankTerrainAndNodeAura ()
	{
		// Set one of the values to an actual value
		OverlandMapTerrainData destinationData = new OverlandMapTerrainData ();
		final MemoryGridCell destination = new MemoryGridCell ();
		destination.setTerrainData (destinationData);

		destinationData.setTileTypeID ("A");
		assertTrue (FogOfWarDuplication.blankTerrainAndNodeAura (destination));
		assertNull (destination.getTerrainData ());

		// Setting destination but leaving all the values none doesn't count as an update
		destinationData = new OverlandMapTerrainData ();
		destination.setTerrainData (destinationData);

		assertFalse (FogOfWarDuplication.blankTerrainAndNodeAura (destination));
		assertNull (destination.getTerrainData ());

		// Try when destination data itself is null
		assertFalse (FogOfWarDuplication.blankTerrainAndNodeAura (destination));
		assertNull (destination.getTerrainData ());
	}

	/**
	 * Tests the copyCityData method
	 */
	@Test
	public final void testCopyCityData ()
	{
		// Source terrain data is mandatory; destination terrain data is optional
		final OverlandMapCityData sourceData = new OverlandMapCityData ();
		final MemoryGridCell source = new MemoryGridCell ();
		final MemoryGridCell destination = new MemoryGridCell ();
		source.setCityData (sourceData);

		// Considered an update even though there's no source data because destination has no cityData yet
		// Every time we do a test, repeat it without changing anything to show that we get false back
		assertTrue (FogOfWarDuplication.copyCityData (source, destination, true));
		assertNotNull (destination.getCityData ());
		assertFalse (FogOfWarDuplication.copyCityData (source, destination, true));

		// City population
		sourceData.setCityPopulation (1);
		assertTrue (FogOfWarDuplication.copyCityData (source, destination, true));
		assertEquals (1, destination.getCityData ().getCityPopulation ().intValue ());
		assertFalse (FogOfWarDuplication.copyCityData (source, destination, true));

		// Number of rebels
		sourceData.setNumberOfRebels (2);
		assertTrue (FogOfWarDuplication.copyCityData (source, destination, true));
		assertEquals (2, destination.getCityData ().getNumberOfRebels ().intValue ());
		assertFalse (FogOfWarDuplication.copyCityData (source, destination, true));

		// Minimum farmers
		sourceData.setMinimumFarmers (3);
		assertTrue (FogOfWarDuplication.copyCityData (source, destination, true));
		assertEquals (3, destination.getCityData ().getMinimumFarmers ().intValue ());
		assertFalse (FogOfWarDuplication.copyCityData (source, destination, true));

		// Optional farmers
		sourceData.setOptionalFarmers (4);
		assertTrue (FogOfWarDuplication.copyCityData (source, destination, true));
		assertEquals (4, destination.getCityData ().getOptionalFarmers ().intValue ());
		assertFalse (FogOfWarDuplication.copyCityData (source, destination, true));

		// City owner
		sourceData.setCityOwnerID (5);
		assertTrue (FogOfWarDuplication.copyCityData (source, destination, true));
		assertEquals (5, destination.getCityData ().getCityOwnerID ().intValue ());
		assertFalse (FogOfWarDuplication.copyCityData (source, destination, true));

		// City race
		sourceData.setCityRaceID ("A");
		assertTrue (FogOfWarDuplication.copyCityData (source, destination, true));
		assertEquals ("A", destination.getCityData ().getCityRaceID ());
		assertFalse (FogOfWarDuplication.copyCityData (source, destination, true));

		// City size
		sourceData.setCitySizeID ("B");
		assertTrue (FogOfWarDuplication.copyCityData (source, destination, true));
		assertEquals ("B", destination.getCityData ().getCitySizeID ());
		assertFalse (FogOfWarDuplication.copyCityData (source, destination, true));

		// City name
		sourceData.setCityName ("C");
		assertTrue (FogOfWarDuplication.copyCityData (source, destination, true));
		assertEquals ("C", destination.getCityData ().getCityName ());
		assertFalse (FogOfWarDuplication.copyCityData (source, destination, true));

		// Currently constructing
		sourceData.setCurrentlyConstructingBuildingOrUnitID ("D");
		assertTrue (FogOfWarDuplication.copyCityData (source, destination, true));
		assertEquals ("D", destination.getCityData ().getCurrentlyConstructingBuildingOrUnitID ());
		assertFalse (FogOfWarDuplication.copyCityData (source, destination, true));

		// Change to another actual value
		sourceData.setCityRaceID ("E");
		assertTrue (FogOfWarDuplication.copyCityData (source, destination, true));
		assertEquals ("E", destination.getCityData ().getCityRaceID ());
		assertFalse (FogOfWarDuplication.copyCityData (source, destination, true));

		// Change to null
		sourceData.setCityRaceID (null);
		assertTrue (FogOfWarDuplication.copyCityData (source, destination, true));
		assertNull (destination.getCityData ().getCityRaceID ());
		assertFalse (FogOfWarDuplication.copyCityData (source, destination, true));

		// Currently constructing gets blanked out if we pass in null, so since it currently has a value, that counts as a change
		assertTrue (FogOfWarDuplication.copyCityData (source, destination, false));
		assertNull (destination.getCityData ().getCurrentlyConstructingBuildingOrUnitID ());

		// Any change to its value doesn't count as a change
		sourceData.setCurrentlyConstructingBuildingOrUnitID ("F");
		assertFalse (FogOfWarDuplication.copyCityData (source, destination, false));
	}

	/**
	 * Tests the blankCityData method
	 */
	@Test
	public final void testBlankCityData ()
	{
		// Set one of the values to an actual value
		OverlandMapCityData destinationData = new OverlandMapCityData ();
		final MemoryGridCell destination = new MemoryGridCell ();
		destination.setCityData (destinationData);

		destinationData.setCityRaceID ("A");
		assertTrue (FogOfWarDuplication.blankCityData (destination));
		assertNull (destination.getCityData ());

		// Setting destination but leaving all the values none doesn't count as an update
		destinationData = new OverlandMapCityData ();
		destination.setCityData (destinationData);

		assertFalse (FogOfWarDuplication.blankCityData (destination));
		assertNull (destination.getCityData ());

		// Try when destination data itself is null
		assertFalse (FogOfWarDuplication.blankCityData (destination));
		assertNull (destination.getCityData ());
	}

	/**
	 * Tests the copyBuilding method
	 */
	@Test
	public final void testCopyBuilding ()
	{
		final List<MemoryBuilding> destination = new ArrayList<MemoryBuilding> ();

		// Put 3 buildings into the list
		for (int n = 1; n <= 3; n++)
		{
			final OverlandMapCoordinates buildingCoords = new OverlandMapCoordinates ();
			buildingCoords.setX (20 + n);
			buildingCoords.setY (10 + n);
			buildingCoords.setPlane (1);

			final MemoryBuilding building = new MemoryBuilding ();
			building.setBuildingID ("BL0" + n);
			building.setCityLocation (buildingCoords);

			destination.add (building);
		}

		// Test a building already in the list
		final OverlandMapCoordinates existingCoords = new OverlandMapCoordinates ();
		existingCoords.setX (22);
		existingCoords.setY (12);
		existingCoords.setPlane (1);

		final MemoryBuilding existingBuilding = new MemoryBuilding ();
		existingBuilding.setBuildingID ("BL02");
		existingBuilding.setCityLocation (existingCoords);

		assertEquals (3, destination.size ());
		assertFalse (FogOfWarDuplication.copyBuilding (existingBuilding, destination, debugLogger));
		assertEquals (3, destination.size ());

		// Test a building already in the list (location same but different building ID)
		final MemoryBuilding newBuilding = new MemoryBuilding ();
		newBuilding.setBuildingID ("BL03");
		newBuilding.setCityLocation (existingCoords);

		assertEquals (3, destination.size ());
		assertTrue (FogOfWarDuplication.copyBuilding (newBuilding, destination, debugLogger));

		assertEquals (4, destination.size ());
		assertEquals ("BL03", destination.get (3).getBuildingID ());
		assertEquals (22, destination.get (3).getCityLocation ().getX ());
		assertEquals (12, destination.get (3).getCityLocation ().getY ());
		assertEquals (1, destination.get (3).getCityLocation ().getPlane ());
	}

	/**
	 * Tests the copyUnit method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 */
	@Test
	public final void testCopyUnit () throws IOException, JAXBException
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		final List<MemoryUnit> destination = new ArrayList<MemoryUnit> ();

		// First unit (spearmen)
		final OverlandMapCoordinates unitOneLocation = new OverlandMapCoordinates ();
		unitOneLocation.setX (22);
		unitOneLocation.setY (12);
		unitOneLocation.setPlane (1);

		final MemoryUnit unitOne = UnitUtils.createMemoryUnit ("UN105", 1, 1, 10, true, db, debugLogger);
		unitOne.setUnitLocation (unitOneLocation);
		unitOne.setOwningPlayerID (2);

		assertTrue (FogOfWarDuplication.copyUnit (unitOne, destination, debugLogger));

		// Check again without changing anything
		assertFalse (FogOfWarDuplication.copyUnit (unitOne, destination, debugLogger));

		// Change a value
		unitOne.setDamageTaken (1);
		assertTrue (FogOfWarDuplication.copyUnit (unitOne, destination, debugLogger));
		assertFalse (FogOfWarDuplication.copyUnit (unitOne, destination, debugLogger));

		// Second unit (magicians)
		final OverlandMapCoordinates unitTwoLocation = new OverlandMapCoordinates ();
		unitTwoLocation.setX (22);
		unitTwoLocation.setY (12);
		unitTwoLocation.setPlane (1);

		final MemoryUnit unitTwo = UnitUtils.createMemoryUnit ("UN052", 2, 0, 25, true, db, debugLogger);
		unitTwo.setUnitLocation (unitTwoLocation);
		unitTwo.setOwningPlayerID (2);

		assertTrue (FogOfWarDuplication.copyUnit (unitTwo, destination, debugLogger));
		assertFalse (FogOfWarDuplication.copyUnit (unitTwo, destination, debugLogger));

		// Give them more ammo
		UnitUtils.setBasicSkillValue (unitTwo, "US132", 20, debugLogger);
		assertTrue (FogOfWarDuplication.copyUnit (unitTwo, destination, debugLogger));
		assertFalse (FogOfWarDuplication.copyUnit (unitTwo, destination, debugLogger));

		// Cast flight on them (ok so normally this is done via the spells list and merging that into the unit skills list, but this is what's appropriate for this test...)
		final UnitHasSkill flight = new UnitHasSkill ();
		flight.setUnitSkillID ("SS056");
		unitTwo.getUnitHasSkill ().add (flight);

		assertTrue (FogOfWarDuplication.copyUnit (unitTwo, destination, debugLogger));
		assertFalse (FogOfWarDuplication.copyUnit (unitTwo, destination, debugLogger));
	}

	/**
	 * Tests the copySpell method
	 */
	@Test
	public final void testCopySpell ()
	{
		final List<MemoryMaintainedSpell> destination = new ArrayList<MemoryMaintainedSpell> ();

		// Put 3 spells into the list
		for (int n = 1; n <= 3; n++)
		{
			final OverlandMapCoordinates spellCoords = new OverlandMapCoordinates ();
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
		final OverlandMapCoordinates existingCoords = new OverlandMapCoordinates ();
		existingCoords.setX (22);
		existingCoords.setY (12);
		existingCoords.setPlane (1);

		final MemoryMaintainedSpell existingSpell = new MemoryMaintainedSpell ();
		existingSpell.setSpellID ("SP002");
		existingSpell.setCityLocation (existingCoords);
		existingSpell.setCastingPlayerID (2);

		assertEquals (3, destination.size ());
		assertFalse (FogOfWarDuplication.copyMaintainedSpell (existingSpell, destination, debugLogger));
		assertEquals (3, destination.size ());

		// Test a spell already in the list (location same but different spell ID)
		final MemoryMaintainedSpell newSpell = new MemoryMaintainedSpell ();
		newSpell.setSpellID ("SP003");
		newSpell.setCityLocation (existingCoords);
		newSpell.setCastingPlayerID (3);

		assertEquals (3, destination.size ());
		assertTrue (FogOfWarDuplication.copyMaintainedSpell (newSpell, destination, debugLogger));

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
		final List<MemoryCombatAreaEffect> destination = new ArrayList<MemoryCombatAreaEffect> ();

		// Put 3 combatAreaEffects into the list
		for (int n = 1; n <= 3; n++)
		{
			final OverlandMapCoordinates combatAreaEffectCoords = new OverlandMapCoordinates ();
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
		final OverlandMapCoordinates existingCoords = new OverlandMapCoordinates ();
		existingCoords.setX (22);
		existingCoords.setY (12);
		existingCoords.setPlane (1);

		final MemoryCombatAreaEffect existingCombatAreaEffect = new MemoryCombatAreaEffect ();
		existingCombatAreaEffect.setCombatAreaEffectID ("CAE02");
		existingCombatAreaEffect.setMapLocation (existingCoords);
		existingCombatAreaEffect.setCastingPlayerID (2);

		assertEquals (3, destination.size ());
		assertFalse (FogOfWarDuplication.copyCombatAreaEffect (existingCombatAreaEffect, destination, debugLogger));
		assertEquals (3, destination.size ());

		// Test a combatAreaEffect already in the list (location same but different combatAreaEffect ID)
		final MemoryCombatAreaEffect newCombatAreaEffect = new MemoryCombatAreaEffect ();
		newCombatAreaEffect.setCombatAreaEffectID ("CAE03");
		newCombatAreaEffect.setMapLocation (existingCoords);
		newCombatAreaEffect.setCastingPlayerID (3);

		assertEquals (3, destination.size ());
		assertTrue (FogOfWarDuplication.copyCombatAreaEffect (newCombatAreaEffect, destination, debugLogger));

		assertEquals (4, destination.size ());
		assertEquals ("CAE03", destination.get (3).getCombatAreaEffectID ());
		assertEquals (3, destination.get (3).getCastingPlayerID ().intValue ());
		assertEquals (22, destination.get (3).getMapLocation ().getX ());
		assertEquals (12, destination.get (3).getMapLocation ().getY ());
		assertEquals (1, destination.get (3).getMapLocation ().getPlane ());
	}
}
