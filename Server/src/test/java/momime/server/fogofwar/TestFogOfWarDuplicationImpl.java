package momime.server.fogofwar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.database.UnitSkillAndValue;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.utils.MemoryBuildingUtilsImpl;
import momime.common.utils.MemoryCombatAreaEffectUtilsImpl;
import momime.common.utils.MemoryMaintainedSpellUtilsImpl;
import momime.common.utils.UnitUtilsImpl;
import momime.server.utils.UnitSkillDirectAccessImpl;

/**
 * Tests the FogOfWarDuplication class
 */
@ExtendWith(MockitoExtension.class)
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
		
		// Corrupted?
		sourceData.setCorrupted (3);
		assertTrue (dup.copyTerrainAndNodeAura (source, destination));
		assertEquals (3, destination.getTerrainData ().getCorrupted ().intValue ());
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
		assertTrue (dup.copyCityData (source, destination, true, false));
		assertNotNull (destination.getCityData ());
		assertFalse (dup.copyCityData (source, destination, true, false));

		// City population
		sourceData.setCityPopulation (1);
		assertTrue (dup.copyCityData (source, destination, true, false));
		assertEquals (1, destination.getCityData ().getCityPopulation ());
		assertFalse (dup.copyCityData (source, destination, true, false));

		// Number of rebels
		sourceData.setNumberOfRebels (2);
		assertTrue (dup.copyCityData (source, destination, true, false));
		assertEquals (2, destination.getCityData ().getNumberOfRebels ());
		assertFalse (dup.copyCityData (source, destination, true, false));

		// Minimum farmers
		sourceData.setMinimumFarmers (3);
		assertTrue (dup.copyCityData (source, destination, true, false));
		assertEquals (3, destination.getCityData ().getMinimumFarmers ());
		assertFalse (dup.copyCityData (source, destination, true, false));

		// Optional farmers
		sourceData.setOptionalFarmers (4);
		assertTrue (dup.copyCityData (source, destination, true, false));
		assertEquals (4, destination.getCityData ().getOptionalFarmers ());
		assertFalse (dup.copyCityData (source, destination, true, false));

		// City owner
		sourceData.setCityOwnerID (5);
		assertTrue (dup.copyCityData (source, destination, true, false));
		assertEquals (5, destination.getCityData ().getCityOwnerID ());
		assertFalse (dup.copyCityData (source, destination, true, false));

		// City race
		sourceData.setCityRaceID ("A");
		assertTrue (dup.copyCityData (source, destination, true, false));
		assertEquals ("A", destination.getCityData ().getCityRaceID ());
		assertFalse (dup.copyCityData (source, destination, true, false));

		// City size
		sourceData.setCitySizeID ("B");
		assertTrue (dup.copyCityData (source, destination, true, false));
		assertEquals ("B", destination.getCityData ().getCitySizeID ());
		assertFalse (dup.copyCityData (source, destination, true, false));

		// City name
		sourceData.setCityName ("C");
		assertTrue (dup.copyCityData (source, destination, true, false));
		assertEquals ("C", destination.getCityData ().getCityName ());
		assertFalse (dup.copyCityData (source, destination, true, false));

		// Currently constructing
		sourceData.setCurrentlyConstructingBuildingID ("D");
		assertTrue (dup.copyCityData (source, destination, true, false));
		assertEquals ("D", destination.getCityData ().getCurrentlyConstructingBuildingID ());
		assertFalse (dup.copyCityData (source, destination, true, false));

		sourceData.setCurrentlyConstructingUnitID ("F");
		assertTrue (dup.copyCityData (source, destination, true, false));
		assertEquals ("F", destination.getCityData ().getCurrentlyConstructingUnitID ());
		assertFalse (dup.copyCityData (source, destination, true, false));

		// Change to another actual value
		sourceData.setCityRaceID ("E");
		assertTrue (dup.copyCityData (source, destination, true, false));
		assertEquals ("E", destination.getCityData ().getCityRaceID ());
		assertFalse (dup.copyCityData (source, destination, true, false));

		// Change to null
		sourceData.setCityRaceID (null);
		assertTrue (dup.copyCityData (source, destination, true, false));
		assertNull (destination.getCityData ().getCityRaceID ());
		assertFalse (dup.copyCityData (source, destination, true, false));

		// Currently constructing gets blanked out if we pass in null, so since it currently has a value, that counts as a change
		assertTrue (dup.copyCityData (source, destination, false, false));
		assertNull (destination.getCityData ().getCurrentlyConstructingBuildingID ());
		assertNull (destination.getCityData ().getCurrentlyConstructingUnitID ());

		// Any change to its value doesn't count as a change
		sourceData.setCurrentlyConstructingBuildingID ("G");
		sourceData.setCurrentlyConstructingUnitID ("H");
		assertFalse (dup.copyCityData (source, destination, false, false));
		
		// Setting production so far doesn't count as a change
		sourceData.setProductionSoFar (100);
		assertFalse (dup.copyCityData (source, destination, false, false));
		assertNull (destination.getCityData ().getProductionSoFar ());
		
		// Unless we set the flag to true
		assertTrue (dup.copyCityData (source, destination, false, true));
		assertEquals (100, destination.getCityData ().getProductionSoFar ().intValue ());
		assertFalse (dup.copyCityData (source, destination, false, true));
		
		// Setting flag false blanks it out again, and counts as a change
		assertTrue (dup.copyCityData (source, destination, false, false));
		assertNull (destination.getCityData ().getProductionSoFar ());
		assertFalse (dup.copyCityData (source, destination, false, false));
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
			building.setBuildingURN (n);
			building.setBuildingID ("BL0" + n);
			building.setCityLocation (buildingCoords);

			destination.add (building);
		}

		// Test a building already in the list
		final MapCoordinates3DEx existingCoords = new MapCoordinates3DEx (22, 12, 1);

		final MemoryBuilding existingBuilding = new MemoryBuilding ();
		existingBuilding.setBuildingURN (2);
		existingBuilding.setBuildingID ("BL02");
		existingBuilding.setCityLocation (existingCoords);

		assertEquals (3, destination.size ());
		assertFalse (dup.copyBuilding (existingBuilding, destination));
		assertEquals (3, destination.size ());

		// Test a building already in the list (location same but different building ID)
		final MemoryBuilding newBuilding = new MemoryBuilding ();
		newBuilding.setBuildingURN (4);
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
		// Set up object to test
		final FogOfWarDuplicationImpl dup = new FogOfWarDuplicationImpl ();
		dup.setUnitUtils (new UnitUtilsImpl ());
		dup.setUnitSkillDirectAccess (new UnitSkillDirectAccessImpl ());

		final List<MemoryUnit> destination = new ArrayList<MemoryUnit> ();

		// First unit (spearmen)
		final MapCoordinates3DEx unitOneLocation = new MapCoordinates3DEx (22, 12, 1);

		final MemoryUnit unitOne = new MemoryUnit ();
		unitOne.setUnitLocation (unitOneLocation);
		unitOne.setOwningPlayerID (2);

		assertTrue (dup.copyUnit (unitOne, destination, true));

		// Check again without changing anything
		assertFalse (dup.copyUnit (unitOne, destination, true));

		// Change a value
		unitOne.setManaRemaining (1);
		assertTrue (dup.copyUnit (unitOne, destination, true));
		assertFalse (dup.copyUnit (unitOne, destination, true));

		// Second unit (magicians)
		final MapCoordinates3DEx unitTwoLocation = new MapCoordinates3DEx (22, 12, 1);

		final MemoryUnit unitTwo = new MemoryUnit ();
		unitTwo.setUnitLocation (unitTwoLocation);
		unitTwo.setOwningPlayerID (2);
		
		final UnitSkillAndValue unitTwoAmmo = new UnitSkillAndValue ();
		unitTwoAmmo.setUnitSkillID ("US132");
		unitTwoAmmo.setUnitSkillValue (5);
		unitTwo.getUnitHasSkill ().add (unitTwoAmmo);

		assertTrue (dup.copyUnit (unitTwo, destination, true));
		assertFalse (dup.copyUnit (unitTwo, destination, true));

		// Give them more ammo
		unitTwoAmmo.setUnitSkillValue (20);
		assertTrue (dup.copyUnit (unitTwo, destination, true));
		assertFalse (dup.copyUnit (unitTwo, destination, true));

		// Cast flight on them (ok so normally this is done via the spells list and merging that into the unit skills list, but this is what's appropriate for this test...)
		final UnitSkillAndValue flight = new UnitSkillAndValue ();
		flight.setUnitSkillID ("SS056");
		unitTwo.getUnitHasSkill ().add (flight);

		assertTrue (dup.copyUnit (unitTwo, destination, true));
		assertFalse (dup.copyUnit (unitTwo, destination, true));
	}

	/**
	 * Tests the copyMaintainedSpell method when the spell doesn't exist in the destination list
	 */
	@Test
	public final void testCopyMaintainedSpell_New ()
	{
		// Source CAE
		final MemoryMaintainedSpell source = new MemoryMaintainedSpell ();
		source.setSpellURN (1);
		source.setCastingPlayerID (2);
		
		// Destination list
		final List<MemoryMaintainedSpell> list = new ArrayList<MemoryMaintainedSpell> (); 		
		
		// Set up object to test
		final FogOfWarDuplicationImpl dup = new FogOfWarDuplicationImpl ();
		dup.setMemoryMaintainedSpellUtils (new MemoryMaintainedSpellUtilsImpl ());
		
		// Run method
		assertTrue (dup.copyMaintainedSpell (source, list));
		
		// Check results
		assertEquals (1, list.size ());
		assertNotSame (source, list.get (0));
		assertEquals (source.getSpellURN (), list.get (0).getSpellURN ());
	}
	
	/**
	 * Tests the copyMaintainedSpell method when the spell already exists in the destination list and hasn't changed
	 */
	@Test
	public final void testCopyMaintainedSpell_Same ()
	{
		// Source CAE
		final MemoryMaintainedSpell source = new MemoryMaintainedSpell ();
		source.setSpellURN (1);
		source.setCastingPlayerID (2);
		
		// Destination list
		final MemoryMaintainedSpell dest = new MemoryMaintainedSpell ();
		dest.setSpellURN (1);
		dest.setCastingPlayerID (2);
		
		final List<MemoryMaintainedSpell> list = new ArrayList<MemoryMaintainedSpell> ();
		list.add (dest);
		
		// Set up object to test
		final FogOfWarDuplicationImpl dup = new FogOfWarDuplicationImpl ();
		dup.setMemoryMaintainedSpellUtils (new MemoryMaintainedSpellUtilsImpl ());
		
		// Run method
		assertFalse (dup.copyMaintainedSpell (source, list));
		
		// Check results
		assertEquals (1, list.size ());
		assertSame (dest, list.get (0));
	}
	
	/**
	 * Tests the copyMaintainedSpell method when the spell already exists in the destination list but has changed
	 */
	@Test
	public final void testCopyMaintainedSpell_Changed ()
	{
		// Source CAE
		final MemoryMaintainedSpell source = new MemoryMaintainedSpell ();
		source.setSpellURN (1);
		source.setCastingPlayerID (2);
		
		// Destination list
		final MemoryMaintainedSpell dest = new MemoryMaintainedSpell ();
		dest.setSpellURN (1);
		dest.setCastingPlayerID (3);
		
		final List<MemoryMaintainedSpell> list = new ArrayList<MemoryMaintainedSpell> ();
		list.add (dest);
		
		// Set up object to test
		final FogOfWarDuplicationImpl dup = new FogOfWarDuplicationImpl ();
		dup.setMemoryMaintainedSpellUtils (new MemoryMaintainedSpellUtilsImpl ());
		
		// Run method
		assertTrue (dup.copyMaintainedSpell (source, list));
		
		// Check results
		assertEquals (1, list.size ());
		assertSame (dest, list.get (0));
		assertEquals (2, dest.getCastingPlayerID ());
	}
	
	/**
	 * Tests the copyCombatAreaEffect method when the CAE doesn't exist in the destination list
	 */
	@Test
	public final void testCopyCombatAreaEffect_New ()
	{
		// Source CAE
		final MemoryCombatAreaEffect source = new MemoryCombatAreaEffect ();
		source.setCombatAreaEffectURN (1);
		source.setCastingPlayerID (2);
		
		// Destination list
		final List<MemoryCombatAreaEffect> list = new ArrayList<MemoryCombatAreaEffect> (); 		
		
		// Set up object to test
		final FogOfWarDuplicationImpl dup = new FogOfWarDuplicationImpl ();
		dup.setMemoryCombatAreaEffectUtils (new MemoryCombatAreaEffectUtilsImpl ());
		
		// Run method
		assertTrue (dup.copyCombatAreaEffect (source, list));
		
		// Check results
		assertEquals (1, list.size ());
		assertNotSame (source, list.get (0));
		assertEquals (source.getCombatAreaEffectURN (), list.get (0).getCombatAreaEffectURN ());
	}
	
	/**
	 * Tests the copyCombatAreaEffect method when the CAE already exists in the destination list and hasn't changed
	 */
	@Test
	public final void testCopyCombatAreaEffect_Same ()
	{
		// Source CAE
		final MemoryCombatAreaEffect source = new MemoryCombatAreaEffect ();
		source.setCombatAreaEffectURN (1);
		source.setCastingPlayerID (2);
		
		// Destination list
		final MemoryCombatAreaEffect dest = new MemoryCombatAreaEffect ();
		dest.setCombatAreaEffectURN (1);
		dest.setCastingPlayerID (2);
		
		final List<MemoryCombatAreaEffect> list = new ArrayList<MemoryCombatAreaEffect> ();
		list.add (dest);
		
		// Set up object to test
		final FogOfWarDuplicationImpl dup = new FogOfWarDuplicationImpl ();
		dup.setMemoryCombatAreaEffectUtils (new MemoryCombatAreaEffectUtilsImpl ());
		
		// Run method
		assertFalse (dup.copyCombatAreaEffect (source, list));
		
		// Check results
		assertEquals (1, list.size ());
		assertSame (dest, list.get (0));
	}
	
	/**
	 * Tests the copyCombatAreaEffect method when the CAE already exists in the destination list but has changed
	 */
	@Test
	public final void testCopyCombatAreaEffect_Changed ()
	{
		// Source CAE
		final MemoryCombatAreaEffect source = new MemoryCombatAreaEffect ();
		source.setCombatAreaEffectURN (1);
		source.setCastingPlayerID (2);
		
		// Destination list
		final MemoryCombatAreaEffect dest = new MemoryCombatAreaEffect ();
		dest.setCombatAreaEffectURN (1);
		dest.setCastingPlayerID (3);
		
		final List<MemoryCombatAreaEffect> list = new ArrayList<MemoryCombatAreaEffect> ();
		list.add (dest);
		
		// Set up object to test
		final FogOfWarDuplicationImpl dup = new FogOfWarDuplicationImpl ();
		dup.setMemoryCombatAreaEffectUtils (new MemoryCombatAreaEffectUtilsImpl ());
		
		// Run method
		assertTrue (dup.copyCombatAreaEffect (source, list));
		
		// Check results
		assertEquals (1, list.size ());
		assertSame (dest, list.get (0));
		assertEquals (2, dest.getCastingPlayerID ().intValue ());
	}
}