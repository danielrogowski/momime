package momime.server.mapgenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.areas.operations.BooleanMapAreaOperations2DImpl;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.utils.random.RandomUtils;
import com.ndg.utils.random.RandomUtilsImpl;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.CommonXsdResourceResolver;
import momime.common.database.LandProportion;
import momime.common.database.LandProportionPlane;
import momime.common.database.MapFeatureEx;
import momime.common.database.MapSizePlane;
import momime.common.database.NodeStrength;
import momime.common.database.NodeStrengthPlane;
import momime.common.database.OverlandMapSize;
import momime.common.database.Plane;
import momime.common.database.RecordNotFoundException;
import momime.common.database.TileTypeAreaEffect;
import momime.common.database.TileTypeEx;
import momime.common.database.TileTypeFeatureChance;
import momime.common.database.UnitEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapAreaOfMemoryGridCells;
import momime.common.messages.MapRowOfMemoryGridCells;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitStatusID;
import momime.common.utils.MemoryGridCellUtilsImpl;
import momime.server.MomSessionVariables;
import momime.server.ServerTestData;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.messages.MomGeneralServerKnowledge;
import momime.unittests.mapstorage.StoredOverlandMap;

/**
 * Tests the OverlandMapGenerator class
 */
@ExtendWith(MockitoExtension.class)
public final class TestOverlandMapGeneratorImpl extends ServerTestData
{
	/**
	 * Tests the setAllToWater method
	 */
	@Test
	public final void testSetAllToWater ()
	{
		// Session description
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		
		// Set up object to test
		final OverlandMapGeneratorImpl mapGen = new OverlandMapGeneratorImpl ();

		// Run method
		final MapVolumeOfMemoryGridCells terrain = mapGen.setAllToWater (overlandMapSize);

		// Check results
		int count = 0;
		for (final MapAreaOfMemoryGridCells plane : terrain.getPlane ())
			for (final MapRowOfMemoryGridCells row : plane.getRow ())
				for (final MemoryGridCell cell : row.getCell ())
				{
					final ServerGridCellEx serverCell = (ServerGridCellEx) cell;
					count++;
					assertNotNull (serverCell);
					assertNotNull (serverCell.getTerrainData ());
					assertEquals (CommonDatabaseConstants.TILE_TYPE_OCEAN, serverCell.getTerrainData ().getTileTypeID ());
				}

		assertEquals (2 * 60 * 40, count);
	}
	
	/**
	 * Tests the makeTundra method
	 */
	@Test
	public final void testMakeTundra ()
	{
		// Session description
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		
		final LandProportion landProportion = new LandProportion ();
		landProportion.setTundraRowCount (4);
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		sd.setLandProportion (landProportion);
		
		// Mock random number generator
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (4)).thenReturn (3, 2,1, 2,2,2, 1,1,1);		// Compare to - 1, 1,2, 1,2,3, 1,2,3 - 5 of the values are >=
		
		// Set up object to test
		final OverlandMapGeneratorImpl mapGen = new OverlandMapGeneratorImpl ();
		mapGen.setRandomUtils (random);

		// Except on the top rows, tundra can only be placed on grasslands, so leave the world as mostly water,
		// and then set some specific grass cells to test with.  This sets a triangle grass shape.
		final MapVolumeOfMemoryGridCells terrain = mapGen.setAllToWater (overlandMapSize);
		for (int x = 0; x < 6; x++)
			for (int y = 0; y < x; y++)
				terrain.getPlane ().get (0).getRow ().get (y).getCell ().get (x).getTerrainData ().setTileTypeID (CommonDatabaseConstants.TILE_TYPE_GRASS);
		
		// Run method
		mapGen.makeTundra (terrain, sd);

		// Check results
		int count = 0;
		for (final MapAreaOfMemoryGridCells plane : terrain.getPlane ())
			for (final MapRowOfMemoryGridCells row : plane.getRow ())
				for (final MemoryGridCell cell : row.getCell ())
					if (cell.getTerrainData ().getTileTypeID ().equals (CommonDatabaseConstants.TILE_TYPE_TUNDRA))
						count++;
		
		assertEquals ((2 * 2 * 60) + 5, count);
	}
	
	/**
	 * Tests the placeTowersOfWizardry method placing the normal number of towers (6) anywhere on the map
	 * @throws MomException If we can't find a suitable location to place all Towers of Wizardry even after reducing desired separation
	 */
	@Test
	public final void testPlaceTowersOfWizardry () throws MomException
	{
		// Session description
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		overlandMapSize.setTowersOfWizardryCount (6);
		overlandMapSize.setTowersOfWizardrySeparation (10);
		
		// Use real random number generator
		final RandomUtilsImpl random = new RandomUtilsImpl ();
		
		final BooleanMapAreaOperations2DImpl ops = new BooleanMapAreaOperations2DImpl ();
		ops.setRandomUtils (random);
		ops.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Set up object to test
		final OverlandMapGeneratorImpl mapGen = new OverlandMapGeneratorImpl ();
		mapGen.setBooleanMapAreaOperations2D (ops);
		mapGen.setRandomUtils (random);
		mapGen.setMemoryGridCellUtils (new MemoryGridCellUtilsImpl ());
		
		// Use this to create the map so it creates the terrainData objects, which the method on ServerTestData does not
		final MapVolumeOfMemoryGridCells terrain = mapGen.setAllToWater (overlandMapSize);

		// Run method
		mapGen.placeTowersOfWizardry (terrain, overlandMapSize);
		
		// Check results - search for the towers on Arcanus, then we know what the value in Myrror should be
		int count = 0;
		for (int x = 0; x < overlandMapSize.getWidth (); x++)
			for (int y = 0; y < overlandMapSize.getHeight (); y++)
			{
				final ServerGridCellEx arcanusCell = (ServerGridCellEx) terrain.getPlane ().get (0).getRow ().get (y).getCell ().get (x);
				final ServerGridCellEx myrrorCell = (ServerGridCellEx) terrain.getPlane ().get (1).getRow ().get (y).getCell ().get (x);
				
				if (arcanusCell.getTerrainData ().getMapFeatureID () == null)
				{
					// Its still a water tile
					assertEquals (CommonDatabaseConstants.TILE_TYPE_OCEAN, arcanusCell.getTerrainData ().getTileTypeID ());
					assertNull (arcanusCell.getNodeLairTowerPowerProportion ());
					
					assertNull (myrrorCell.getTerrainData ().getMapFeatureID ());
					assertEquals (CommonDatabaseConstants.TILE_TYPE_OCEAN, myrrorCell.getTerrainData ().getTileTypeID ());
					assertNull (myrrorCell.getNodeLairTowerPowerProportion ());
				}
				else
				{
					// Its a tower
					count++;
					
					assertEquals (CommonDatabaseConstants.FEATURE_UNCLEARED_TOWER_OF_WIZARDRY, arcanusCell.getTerrainData ().getMapFeatureID ());
					assertEquals (CommonDatabaseConstants.TILE_TYPE_GRASS, arcanusCell.getTerrainData ().getTileTypeID ());
					assertNotNull (arcanusCell.getNodeLairTowerPowerProportion ());
					
					if ((arcanusCell.getNodeLairTowerPowerProportion () < 0) || (arcanusCell.getNodeLairTowerPowerProportion () > 1))
						fail ("NodeLairTowerPowerProportion must be between 0..1");
					
					assertEquals (CommonDatabaseConstants.FEATURE_UNCLEARED_TOWER_OF_WIZARDRY, myrrorCell.getTerrainData ().getMapFeatureID ());
					assertEquals (CommonDatabaseConstants.TILE_TYPE_GRASS, myrrorCell.getTerrainData ().getTileTypeID ());
					assertNull (myrrorCell.getNodeLairTowerPowerProportion ());
				}
			}
		
		assertEquals (overlandMapSize.getTowersOfWizardryCount (), count);
	}

	/**
	 * Proves that towers will never be placed on Tundra
	 * @throws MomException If we can't find a suitable location to place all Towers of Wizardry even after reducing desired separation
	 */
	@Test
	public final void testPlaceTowersOfWizardry_Tundra () throws MomException
	{
		// Session description
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		overlandMapSize.setTowersOfWizardryCount (6);
		overlandMapSize.setTowersOfWizardrySeparation (10);
		
		// Use real random number generator
		final RandomUtilsImpl random = new RandomUtilsImpl ();
		
		final BooleanMapAreaOperations2DImpl ops = new BooleanMapAreaOperations2DImpl ();
		ops.setRandomUtils (random);
		ops.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Set up object to test
		final OverlandMapGeneratorImpl mapGen = new OverlandMapGeneratorImpl ();
		mapGen.setBooleanMapAreaOperations2D (ops);
		mapGen.setRandomUtils (random);
		mapGen.setMemoryGridCellUtils (new MemoryGridCellUtilsImpl ());
		
		// Set rows 0..19 as tundra on Arcanus; set rows 21..39 as tundra on Myrror
		// Therefore if the method works properly, it'll be forced to place all towers on row 20
		final MapVolumeOfMemoryGridCells terrain = mapGen.setAllToWater (overlandMapSize);
		for (int x = 0; x < overlandMapSize.getWidth (); x++)
			for (int y = 0; y < overlandMapSize.getHeight (); y++)
				if (y < 20)
					terrain.getPlane ().get (0).getRow ().get (y).getCell ().get (x).getTerrainData ().setTileTypeID (CommonDatabaseConstants.TILE_TYPE_TUNDRA);
				else if (y > 20)
					terrain.getPlane ().get (1).getRow ().get (y).getCell ().get (x).getTerrainData ().setTileTypeID (CommonDatabaseConstants.TILE_TYPE_TUNDRA);
		
		// Run method
		mapGen.placeTowersOfWizardry (terrain, overlandMapSize);
		
		// Check results - search for the towers on Arcanus, then we know what the value in Myrror should be
		int count = 0;
		for (int x = 0; x < overlandMapSize.getWidth (); x++)
			for (int y = 0; y < overlandMapSize.getHeight (); y++)
			{
				final ServerGridCellEx arcanusCell = (ServerGridCellEx) terrain.getPlane ().get (0).getRow ().get (y).getCell ().get (x);
				final ServerGridCellEx myrrorCell = (ServerGridCellEx) terrain.getPlane ().get (1).getRow ().get (y).getCell ().get (x);
				
				if (arcanusCell.getTerrainData ().getMapFeatureID () == null)
				{
					// Its still a water or tundra tile
					assertNull (arcanusCell.getNodeLairTowerPowerProportion ());
					
					assertNull (myrrorCell.getTerrainData ().getMapFeatureID ());
					assertNull (myrrorCell.getNodeLairTowerPowerProportion ());
				}
				else
				{
					// Its a tower
					count++;
					assertEquals (20, y);
					
					assertEquals (CommonDatabaseConstants.FEATURE_UNCLEARED_TOWER_OF_WIZARDRY, arcanusCell.getTerrainData ().getMapFeatureID ());
					assertEquals (CommonDatabaseConstants.TILE_TYPE_GRASS, arcanusCell.getTerrainData ().getTileTypeID ());
					assertNotNull (arcanusCell.getNodeLairTowerPowerProportion ());
					
					if ((arcanusCell.getNodeLairTowerPowerProportion () < 0) || (arcanusCell.getNodeLairTowerPowerProportion () > 1))
						fail ("NodeLairTowerPowerProportion must be between 0..1");
					
					assertEquals (CommonDatabaseConstants.FEATURE_UNCLEARED_TOWER_OF_WIZARDRY, myrrorCell.getTerrainData ().getMapFeatureID ());
					assertEquals (CommonDatabaseConstants.TILE_TYPE_GRASS, myrrorCell.getTerrainData ().getTileTypeID ());
					assertNull (myrrorCell.getNodeLairTowerPowerProportion ());
				}
			}
		
		assertEquals (overlandMapSize.getTowersOfWizardryCount (), count);
	}

	/**
	 * Try to place 61 towers on a map that only has space for 60 of them
	 * @throws MomException If we can't find a suitable location to place all Towers of Wizardry even after reducing desired separation
	 */
	@Test
	public final void testPlaceTowersOfWizardry_Full () throws MomException
	{
		// Session description
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		overlandMapSize.setTowersOfWizardryCount (61);
		overlandMapSize.setTowersOfWizardrySeparation (10);
		
		// Use real random number generator
		final RandomUtilsImpl random = new RandomUtilsImpl ();
		
		final BooleanMapAreaOperations2DImpl ops = new BooleanMapAreaOperations2DImpl ();
		ops.setRandomUtils (random);
		ops.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Set up object to test
		final OverlandMapGeneratorImpl mapGen = new OverlandMapGeneratorImpl ();
		mapGen.setBooleanMapAreaOperations2D (ops);
		mapGen.setRandomUtils (random);
		mapGen.setMemoryGridCellUtils (new MemoryGridCellUtilsImpl ());
		
		// Set rows 0..19 as tundra on Arcanus; set rows 21..39 as tundra on Myrror
		// Therefore if the method works properly, it'll be forced to place all towers on row 20
		final MapVolumeOfMemoryGridCells terrain = mapGen.setAllToWater (overlandMapSize);
		for (int x = 0; x < overlandMapSize.getWidth (); x++)
			for (int y = 0; y < overlandMapSize.getHeight (); y++)
				if (y < 20)
					terrain.getPlane ().get (0).getRow ().get (y).getCell ().get (x).getTerrainData ().setTileTypeID (CommonDatabaseConstants.TILE_TYPE_TUNDRA);
				else if (y > 20)
					terrain.getPlane ().get (1).getRow ().get (y).getCell ().get (x).getTerrainData ().setTileTypeID (CommonDatabaseConstants.TILE_TYPE_TUNDRA);
		
		// Run method
		assertThrows (MomException.class, () ->
		{
			mapGen.placeTowersOfWizardry (terrain, overlandMapSize);
		});
	}
	
	/**
	 * Tests the findTerrainBorder8 method to find a bitmask that exists
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws MomException If some fatal error happens during map generation
	 * @throws RecordNotFoundException If no tile with this bitmask is defined
	 */
	@Test
	public final void testFindTerrainBorder8_Found () throws JAXBException, MomException, RecordNotFoundException
	{
		final OverlandMapGeneratorImpl gen = new OverlandMapGeneratorImpl ();
		gen.setRandomUtils (mock (RandomUtils.class));
		
		assertEquals (32, gen.findTerrainBorder8 ("10000011"));
	}

	/**
	 * Tests the findTerrainBorder8 method to find a bitmask that doesn't exist
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws MomException If some fatal error happens during map generation
	 * @throws RecordNotFoundException If no tile with this bitmask is defined
	 */
	@Test
	public final void testFindTerrainBorder8_NotFound () throws JAXBException, MomException, RecordNotFoundException
	{
		final OverlandMapGeneratorImpl gen = new OverlandMapGeneratorImpl ();
		
		assertThrows (RecordNotFoundException.class, () ->
		{
			gen.findTerrainBorder8 ("10101010");
		});
	}

	/**
	 * Tests the convertNeighbouringTilesToDirections method
	 * @throws MomException If some fatal error happens during map generation
	 */
	@Test
	public final void testConvertNeighbouringTilesToDirections () throws MomException
	{
		final OverlandMapGeneratorImpl gen = new OverlandMapGeneratorImpl (); 
		assertEquals ("37", gen.convertNeighbouringTilesToDirections ("0101", -1));
		assertEquals ("357", gen.convertNeighbouringTilesToDirections ("0111", -1));
		assertEquals ("37", gen.convertNeighbouringTilesToDirections ("0111", 5));
	}

	/**
	 * Tests the checkAllDirectionsLeadToGrass method
	 */
	@Test
	public final void testCheckAllDirectionsLeadToGrass ()
	{
		// Session description
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		
		// Set up object to test
		final OverlandMapGeneratorImpl mapGen = new OverlandMapGeneratorImpl ();
		mapGen.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());

		// Grass to the east and west (via wrapping), ocean to the south because we didn't overwrite it
		final MapVolumeOfMemoryGridCells terrain = mapGen.setAllToWater (overlandMapSize);
		terrain.getPlane ().get (0).getRow ().get (0).getCell ().get (1).getTerrainData ().setTileTypeID (CommonDatabaseConstants.TILE_TYPE_GRASS);
		terrain.getPlane ().get (0).getRow ().get (0).getCell ().get (59).getTerrainData ().setTileTypeID (CommonDatabaseConstants.TILE_TYPE_GRASS);

		// Run tests
		final boolean [] [] riverPending = new boolean [overlandMapSize.getHeight ()] [overlandMapSize.getWidth ()];
		assertTrue (mapGen.checkAllDirectionsLeadToGrass (terrain, overlandMapSize, 0, 0, 0, "3", riverPending));
		assertTrue (mapGen.checkAllDirectionsLeadToGrass (terrain, overlandMapSize, 0, 0, 0, "37", riverPending));
		assertFalse (mapGen.checkAllDirectionsLeadToGrass (terrain, overlandMapSize, 0, 0, 0, "357", riverPending));	// because includes an ocean tile
		assertFalse (mapGen.checkAllDirectionsLeadToGrass (terrain, overlandMapSize, 0, 0, 0, "137", riverPending));	// because goes off the top of the map

		// Map features are no longer grass
		terrain.getPlane ().get (0).getRow ().get (0).getCell ().get (59).getTerrainData ().setMapFeatureID ("X");
		assertFalse (mapGen.checkAllDirectionsLeadToGrass (terrain, overlandMapSize, 0, 0, 0, "37", riverPending));
		terrain.getPlane ().get (0).getRow ().get (0).getCell ().get (59).getTerrainData ().setMapFeatureID (null);
		assertTrue (mapGen.checkAllDirectionsLeadToGrass (terrain, overlandMapSize, 0, 0, 0, "37", riverPending));

		// Pending rivers will become rivers, so are no longer considered grass either
		riverPending [0] [59] = true;
		assertFalse (mapGen.checkAllDirectionsLeadToGrass (terrain, overlandMapSize, 0, 0, 0, "37", riverPending));
	}

	/**
	 * Tests the countStringRepetitions method
	 */
	@Test
	public final void testCountStringRepetitions ()
	{
		final OverlandMapGeneratorImpl gen = new OverlandMapGeneratorImpl (); 
		assertEquals (0, gen.countStringRepetitions ("C", "abcde"), "Zero repetitions");
		assertEquals (1, gen.countStringRepetitions ("c", "abcde"), "One in the middle");
		assertEquals (2, gen.countStringRepetitions ("c", "abcgcde"), "Two in the middle");
		assertEquals (2, gen.countStringRepetitions ("c", "cgcde"), "Two including one at the start");
		assertEquals (2, gen.countStringRepetitions ("c", "abcgc"), "Two including one at the end");
	}
	
	/**
	 * Tests the placeTerrainFeatures method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testPlaceTerrainFeatures () throws Exception
	{
		// Session description
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		
		final LandProportionPlane landProportionArcanus = new LandProportionPlane ();
		landProportionArcanus.setFeatureChance (17);
		landProportionArcanus.setPlaneNumber (0);

		final LandProportionPlane landProportionMyrror = new LandProportionPlane ();
		landProportionMyrror.setFeatureChance (10);
		landProportionMyrror.setPlaneNumber (1);
		
		final LandProportion landProportion = new LandProportion ();
		landProportion.getLandProportionPlane ().add (landProportionArcanus);
		landProportion.getLandProportionPlane ().add (landProportionMyrror);
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		sd.setLandProportion (landProportion);
		
		// Mock server database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final TileTypeEx ocean = new TileTypeEx ();
		when (db.findTileType (CommonDatabaseConstants.TILE_TYPE_OCEAN, "placeTerrainFeatures")).thenReturn (ocean);

		// Arcanus can get MF01..02, Myrror can get MF01..03
		final TileTypeEx mountain = new TileTypeEx ();
		for (int plane = 0; plane < overlandMapSize.getDepth (); plane++)
			for (int n = 1; n <= 2+plane; n++)
			{
				final TileTypeFeatureChance feature = new TileTypeFeatureChance ();
				feature.setMapFeatureID ("MF0" + n);
				feature.setFeatureChance (n);
				feature.setPlaneNumber (plane);
				mountain.getTileTypeFeatureChance ().add (feature);
			}
		when (db.findTileType (CommonDatabaseConstants.TILE_TYPE_MOUNTAIN, "placeTerrainFeatures")).thenReturn (mountain);
		
		// Mock random number generator
		final RandomUtils random = mock (RandomUtils.class);
		
		// 3 out of 30 map cells on Arcanus get features, and 5 out of 30 map cells on Myrror get features
		when (random.nextInt (17)).thenReturn (0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
		when (random.nextInt (10)).thenReturn (0, 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 5);
		
		// Set up object to test
		final OverlandMapGeneratorImpl mapGen = new OverlandMapGeneratorImpl ();
		mapGen.setRandomUtils (random);

		// Place some tile types that get chances for map features
		final MapVolumeOfMemoryGridCells terrain = mapGen.setAllToWater (overlandMapSize);
		for (int plane = 0; plane < overlandMapSize.getDepth (); plane++)
			for (int x = 0; x < overlandMapSize.getWidth (); x++)
			{
				final OverlandMapTerrainData tc = terrain.getPlane ().get (plane).getRow ().get (10).getCell ().get (x).getTerrainData ();
				tc.setTileTypeID (CommonDatabaseConstants.TILE_TYPE_MOUNTAIN);
				
				// Prove existing features won't be overridden
				if (x % 2 == 0)
					tc.setMapFeatureID ("MF99");
			}
		
		// The specific types of map features placed on Arcanus (1/3 chance of MF01, 2/3 chance of MF02)
		when (random.nextInt (3)).thenReturn (0, 1, 2);

		// The specific types of map features placed on Myrror (1/6 chance of MF01, 2/6 chance of MF02, 3/6 chance of MF03)
		when (random.nextInt (6)).thenReturn (0, 1, 2, 3, 4);
		
		// Run method
		mapGen.placeTerrainFeatures (terrain, sd, db);
		
		// 30 map cells on each plane should've been considered
		verify (random, times (30)).nextInt (landProportionArcanus.getFeatureChance ());
		verify (random, times (30)).nextInt (landProportionMyrror.getFeatureChance ());
		
		// 3 and 5 features actually got placed
		for (int plane = 0; plane < overlandMapSize.getDepth (); plane++)
		{
			int count = 0;
			for (int x = 0; x < overlandMapSize.getWidth (); x++)
			{
				final OverlandMapTerrainData tc = terrain.getPlane ().get (plane).getRow ().get (10).getCell ().get (x).getTerrainData ();
				if ((tc.getMapFeatureID () != null) && (!tc.getMapFeatureID ().equals ("MF99")))
					count++;
			}
			
			assertEquals ((plane == 0) ? 3 : 5, count);
		}

		verify (random, times (3)).nextInt (3);
		verify (random, times (5)).nextInt (6);
		
		// Check the specific cells
		assertEquals ("MF01", terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (1).getTerrainData ().getMapFeatureID ());
		assertEquals ("MF02", terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (21).getTerrainData ().getMapFeatureID ());
		assertEquals ("MF02", terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (41).getTerrainData ().getMapFeatureID ());

		assertEquals ("MF01", terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (1).getTerrainData ().getMapFeatureID ());
		assertEquals ("MF02", terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (13).getTerrainData ().getMapFeatureID ());
		assertEquals ("MF02", terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (25).getTerrainData ().getMapFeatureID ());
		assertEquals ("MF03", terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (37).getTerrainData ().getMapFeatureID ());
		assertEquals ("MF03", terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (49).getTerrainData ().getMapFeatureID ());

		verifyNoMoreInteractions (random);
	}
	
	/**
	 * Tests the placeNodeRings method just performing a test whether the rings fit
	 */
	@Test
	public final void testPlaceNodeRings_TestOnly ()
	{
		// Session description
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		
		// Map storage
		final MapVolumeOfMemoryGridCells terrain = createOverlandMap (overlandMapSize);
		
		// Set up object to test
		final OverlandMapGeneratorImpl mapGen = new OverlandMapGeneratorImpl ();
		mapGen.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Nothing in the way yet, so will definitely fit
		// centre square, then 6/8 in the radius 1 ring
		assertTrue (mapGen.placeNodeRings (terrain, overlandMapSize, 7, 20, 10, 0, null));
		
		// If the centre ring is blocked then its an immediate fail, even though there's 7 free spaces in the radius 1 ring
		((ServerGridCellEx) terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (20)).setAuraFromNode (new MapCoordinates3DEx (0, 0, 0));
		assertFalse (mapGen.placeNodeRings (terrain, overlandMapSize, 7, 20, 10, 0, null));
		
		// Unblock the centre ring again, and block 2 cells in the radius 1 ring, so there's still enough space, just
		((ServerGridCellEx) terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (20)).setAuraFromNode (null);
		((ServerGridCellEx) terrain.getPlane ().get (0).getRow ().get (11).getCell ().get (21)).setAuraFromNode (new MapCoordinates3DEx (0, 0, 0));
		((ServerGridCellEx) terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (19)).setAuraFromNode (new MapCoordinates3DEx (0, 0, 0));
		assertTrue (mapGen.placeNodeRings (terrain, overlandMapSize, 7, 20, 10, 0, null));
		
		// Block 3rd cell in the radius 1 ring and it fails, even though if it expanded two 2 rings there's plenty of space
		((ServerGridCellEx) terrain.getPlane ().get (0).getRow ().get (9).getCell ().get (21)).setAuraFromNode (new MapCoordinates3DEx (0, 0, 0));
		assertFalse (mapGen.placeNodeRings (terrain, overlandMapSize, 7, 20, 10, 0, null));
	}

	/**
	 * Tests the placeNodeRings method in update mode
	 */
	@Test
	public final void testPlaceNodeRings_Update ()
	{
		// Session description
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		
		// Map storage
		final MapVolumeOfMemoryGridCells terrain = createOverlandMap (overlandMapSize);
		
		// Set up object to test
		final OverlandMapGeneratorImpl mapGen = new OverlandMapGeneratorImpl ();
		mapGen.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		mapGen.setRandomUtils (new RandomUtilsImpl ());

		// Block 2 cells in the radius 1 ring so when we place 7 node auras, there's only exactly the right number of cells left
		// Unblock the centre ring again, and block 2 cells in the radius 1 ring, so there's still enough space, just
		((ServerGridCellEx) terrain.getPlane ().get (0).getRow ().get (11).getCell ().get (21)).setAuraFromNode (new MapCoordinates3DEx (0, 0, 0));
		((ServerGridCellEx) terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (19)).setAuraFromNode (new MapCoordinates3DEx (0, 0, 0));
		
		// Run method
		final MapCoordinates3DEx coords = new MapCoordinates3DEx (20, 10, 0);
		assertTrue (mapGen.placeNodeRings (terrain, overlandMapSize, 7, 20, 10, 0, coords));
		
		// Check results
		assertEquals (coords, ((ServerGridCellEx) terrain.getPlane ().get (0).getRow ().get (9).getCell ().get (19)).getAuraFromNode ());
		assertEquals (coords, ((ServerGridCellEx) terrain.getPlane ().get (0).getRow ().get (9).getCell ().get (20)).getAuraFromNode ());
		assertEquals (coords, ((ServerGridCellEx) terrain.getPlane ().get (0).getRow ().get (9).getCell ().get (21)).getAuraFromNode ());
		// assertEquals (coords, ((ServerGridCellEx) terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (19)).getAuraFromNode ());
		assertEquals (coords, ((ServerGridCellEx) terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (20)).getAuraFromNode ());
		assertEquals (coords, ((ServerGridCellEx) terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (21)).getAuraFromNode ());
		assertEquals (coords, ((ServerGridCellEx) terrain.getPlane ().get (0).getRow ().get (11).getCell ().get (19)).getAuraFromNode ());
		assertEquals (coords, ((ServerGridCellEx) terrain.getPlane ().get (0).getRow ().get (11).getCell ().get (20)).getAuraFromNode ());
		// assertEquals (coords, ((ServerGridCellEx) terrain.getPlane ().get (0).getRow ().get (11).getCell ().get (21)).getAuraFromNode ());
	}

	/**
	 * Tests the placeNodes method 
	 * @throws MomException If there are no node tile types defined in the database
	 */
	@Test
	public final void testPlaceNodes () throws MomException
	{
		// Session description
		final MapSizePlane mapSizeArcanus = new MapSizePlane ();
		mapSizeArcanus.setNumberOfNodesOnPlane (14);
		mapSizeArcanus.setPlaneNumber (0);

		final MapSizePlane mapSizeMyrror = new MapSizePlane ();
		mapSizeMyrror.setNumberOfNodesOnPlane (18);
		mapSizeMyrror.setPlaneNumber (1);
		
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		overlandMapSize.getMapSizePlane ().add (mapSizeArcanus);
		overlandMapSize.getMapSizePlane ().add (mapSizeMyrror);

		final NodeStrengthPlane nodeStrengthArcanus = new NodeStrengthPlane ();
		nodeStrengthArcanus.setNodeAuraSquaresMinimum (5);
		nodeStrengthArcanus.setNodeAuraSquaresMaximum (10);
		nodeStrengthArcanus.setPlaneNumber (0);
		
		final NodeStrengthPlane nodeStrengthMyrror = new NodeStrengthPlane ();
		nodeStrengthMyrror.setNodeAuraSquaresMinimum (10);
		nodeStrengthMyrror.setNodeAuraSquaresMaximum (15);
		nodeStrengthMyrror.setPlaneNumber (1);
		
		final NodeStrength nodeStrength = new NodeStrength ();
		nodeStrength.getNodeStrengthPlane ().add (nodeStrengthArcanus);
		nodeStrength.getNodeStrengthPlane ().add (nodeStrengthMyrror);
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		sd.setNodeStrength (nodeStrength);
		
		// Mock server database
		final CommonDatabase db = mock (CommonDatabase.class);

		final List<TileTypeEx> nodeTypes = new ArrayList<TileTypeEx> ();
		for (int n = 1; n <= 6; n++)
		{
			final TileTypeEx nodeType = new TileTypeEx ();
			nodeType.setTileTypeID ("NT0" + n);
			
			if (n <= 3)
				nodeType.setMagicRealmID ("MB0" + n);
			
			nodeTypes.add (nodeType);
		}
		when (db.getTileTypes ()).thenReturn (nodeTypes);
		
		// Set up object to test
		final OverlandMapGeneratorImpl mapGen = new OverlandMapGeneratorImpl ();
		mapGen.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		mapGen.setRandomUtils (new RandomUtilsImpl ());
		
		// Only grass tiles are candidates for placing nodes, so make everything grass
		final MapVolumeOfMemoryGridCells terrain = mapGen.setAllToWater (overlandMapSize);
		for (final MapAreaOfMemoryGridCells plane : terrain.getPlane ())
			for (final MapRowOfMemoryGridCells row : plane.getRow ())
				for (final MemoryGridCell cell : row.getCell ())
					cell.getTerrainData ().setTileTypeID (CommonDatabaseConstants.TILE_TYPE_GRASS);
		
		// Run method
		mapGen.placeNodes (terrain, sd, db);
		
		// Check the right number of nodes were places on the each plane, that the nodeLairTowerProportions are correctly set,
		// and the number of auraFromNode values looks reasonable and they all point back to actual nodes
		for (final NodeStrengthPlane plane : nodeStrength.getNodeStrengthPlane ())
		{
			int nodeCount = 0;
			int auraCount = 0;
			
			for (final MapRowOfMemoryGridCells row : terrain.getPlane ().get (plane.getPlaneNumber ()).getRow ())
				for (final MemoryGridCell cell : row.getCell ())
				{
					final ServerGridCellEx serverCell = (ServerGridCellEx) cell;
					
					// Check actual nodes
					if (serverCell.getTerrainData ().getTileTypeID ().startsWith ("NT0"))
					{
						nodeCount++;
						assertNotNull (serverCell.getNodeLairTowerPowerProportion ());
						
						if ((serverCell.getNodeLairTowerPowerProportion () < 0) || (serverCell.getNodeLairTowerPowerProportion () > 1))
							fail ("NodeLairTowerPowerProportion must be between 0..1");
					}
					else
						assertNull (serverCell.getNodeLairTowerPowerProportion ());
					
					// Check auras
					if (serverCell.getAuraFromNode () != null)
					{
						auraCount++;
						final MemoryGridCell nodeCell = terrain.getPlane ().get
							(serverCell.getAuraFromNode ().getZ ()).getRow ().get (serverCell.getAuraFromNode ().getY ()).getCell ().get (serverCell.getAuraFromNode ().getX ());
						
						assertNotEquals (CommonDatabaseConstants.TILE_TYPE_GRASS, nodeCell.getTerrainData ().getTileTypeID ());						 
					}
				}
			
			// Check plane totals
			final MapSizePlane mapSizePlane = overlandMapSize.getMapSizePlane ().get (plane.getPlaneNumber ()); 
			
			assertEquals (mapSizePlane.getNumberOfNodesOnPlane (), nodeCount);
			
			if ((auraCount < mapSizePlane.getNumberOfNodesOnPlane () * plane.getNodeAuraSquaresMinimum ()) ||
				(auraCount > mapSizePlane.getNumberOfNodesOnPlane () * plane.getNodeAuraSquaresMaximum ()))
				
				fail ("auraCount for plane " + plane.getPlaneNumber () + " must be between " +
					(mapSizePlane.getNumberOfNodesOnPlane () * plane.getNodeAuraSquaresMinimum ()) + " and " +
					(mapSizePlane.getNumberOfNodesOnPlane () * plane.getNodeAuraSquaresMaximum ()) + " but was " + auraCount);
		}
	}
	
	/**
	 * Tests the chooseRandomNodeTileTypeID method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testChooseRandomNodeTileTypeID () throws Exception
	{
		// Mock server database - 3 node tile types, and 3 non-node tile types
		final CommonDatabase db = mock (CommonDatabase.class);

		final List<TileTypeEx> nodeTypes = new ArrayList<TileTypeEx> ();
		for (int n = 1; n <= 6; n++)
		{
			final TileTypeEx nodeType = new TileTypeEx ();
			nodeType.setTileTypeID ("NT0" + n);
			
			if (n <= 3)
				nodeType.setMagicRealmID ("MB0" + n);
			
			nodeTypes.add (nodeType);
		}
		when (db.getTileTypes ()).thenReturn (nodeTypes);

		// Fix random result
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (3)).thenReturn (2);
		
		// Set up object to test
		final OverlandMapGeneratorImpl mapGen = new OverlandMapGeneratorImpl ();
		mapGen.setRandomUtils (random);

		assertEquals ("NT03", mapGen.chooseRandomNodeTileTypeID (db));
	}

	/**
	 * Tests the chooseRandomLairFeatureID method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testChooseRandomLairFeatureID () throws Exception
	{
		// Mock server database - creates 7 map features, MF10A .. MF16A, note MF12A is intentionally the code for an uncleared tower so won't be considered
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final List<MapFeatureEx> mapFeatures = new ArrayList<MapFeatureEx> ();
		for (int n = 0; n <= 6; n++)
		{
			final MapFeatureEx mapFeature = new MapFeatureEx ();
			mapFeature.setMapFeatureID ("MF1" + n + "A");
			
			if (n <= 3)
				mapFeature.getMapFeatureMagicRealm ().add (null);		// Doesn't matter what's here, as long as its a non-empty list
			
			mapFeatures.add (mapFeature);
		}
		when (db.getMapFeatures ()).thenReturn (mapFeatures);

		// Fix random result
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (3)).thenReturn (2);
		
		// Set up object to test
		final OverlandMapGeneratorImpl mapGen = new OverlandMapGeneratorImpl ();
		mapGen.setRandomUtils (random);

		assertEquals ("MF13A", mapGen.chooseRandomLairFeatureID (db));
	}
	
	/**
	 * Tests the placeLairs method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testPlaceLairs () throws Exception
	{
		// Session description
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		
		// Mock server database - ocean is TT09 so lets create 3 tile types that can get lairs and 6 that can't
		final CommonDatabase db = mock (CommonDatabase.class);

		for (int n = 1; n <= 9; n++)
		{
			final TileTypeEx tileType = new TileTypeEx ();
			tileType.setTileTypeID ("TT0" + n);
			tileType.setCanPlaceLair (n <= 3);
			
			when (db.findTileType (tileType.getTileTypeID (), "placeLairs")).thenReturn (tileType);
		}		

		final List<MapFeatureEx> mapFeatures = new ArrayList<MapFeatureEx> ();
		for (int n = 1; n <= 6; n++)
		{
			final MapFeatureEx mapFeature = new MapFeatureEx ();
			mapFeature.setMapFeatureID ("MF0" + n);
			
			if (n <= 3)
				mapFeature.getMapFeatureMagicRealm ().add (null);		// Doesn't matter what's here, as long as its a non-empty list
			
			mapFeatures.add (mapFeature);
		}
		when (db.getMapFeatures ()).thenReturn (mapFeatures);
		
		// Set up object to test
		final OverlandMapGeneratorImpl mapGen = new OverlandMapGeneratorImpl ();
		mapGen.setRandomUtils (new RandomUtilsImpl ());

		// Set up some tiles which can accept lairs, leave rest as ocean which can't.
		// So we put 30 tiles on each plane that can accept lairs; but then on 10 of them put another mapFeatureID
		final MapVolumeOfMemoryGridCells terrain = mapGen.setAllToWater (overlandMapSize);
		for (int plane = 0; plane < overlandMapSize.getDepth (); plane++)
			for (int y = 0; y <= 9; y++)
				for (int x = 1; x <= 9; x++)
				{
					final OverlandMapTerrainData mc = terrain.getPlane ().get (plane).getRow ().get (y).getCell ().get (x).getTerrainData ();
					mc.setTileTypeID ("TT0" + x);
					
					if (y % 2 == 0)
						mc.setMapFeatureID ("X");
				}
		
		// Place 10 weak lairs
		mapGen.placeLairs (terrain, overlandMapSize, db, 10, true);

		int weakCount = 0;
		for (final MapAreaOfMemoryGridCells plane : terrain.getPlane ())
			for (final MapRowOfMemoryGridCells row : plane.getRow ())
				for (final MemoryGridCell cell : row.getCell ())
				{
					final ServerGridCellEx serverCell = (ServerGridCellEx) cell;
					if ((serverCell.getTerrainData ().getMapFeatureID () != null) && (!serverCell.getTerrainData ().getMapFeatureID ().equals ("X")))
					{
						// Lair was placed here
						weakCount++;
						
						if ((!serverCell.getTerrainData ().getTileTypeID ().equals ("TT01")) && (!serverCell.getTerrainData ().getTileTypeID ().equals ("TT02")) && (!serverCell.getTerrainData ().getTileTypeID ().equals ("TT03")))
							fail ("Lairs should only be placed on TT01..03, but found " + serverCell.getTerrainData ().getTileTypeID ());

						if ((!serverCell.getTerrainData ().getMapFeatureID ().equals ("MF01")) && (!serverCell.getTerrainData ().getMapFeatureID ().equals ("MF02")) && (!serverCell.getTerrainData ().getMapFeatureID ().equals ("MF03")))
							fail ("Only map features MF01..03 should be placed, but found " + serverCell.getTerrainData ().getMapFeatureID ());

						assertNotNull (serverCell.getNodeLairTowerPowerProportion ());
						
						if ((serverCell.getNodeLairTowerPowerProportion () < 0) || (serverCell.getNodeLairTowerPowerProportion () > 1))
							fail ("NodeLairTowerPowerProportion must be between 0..1");
						
						assertTrue (serverCell.isLairWeak ());
					}
					else
					{
						// Lair wasn't placed here
						assertNull (serverCell.getNodeLairTowerPowerProportion ());
						assertNull (serverCell.isLairWeak ());
					}
				}
		
		assertEquals (10, weakCount);
		
		// Place 15 strong lairs (in addition)
		mapGen.placeLairs (terrain, overlandMapSize, db, 15, false);
		
		weakCount = 0;
		int strongCount = 0;
		for (final MapAreaOfMemoryGridCells plane : terrain.getPlane ())
			for (final MapRowOfMemoryGridCells row : plane.getRow ())
				for (final MemoryGridCell cell : row.getCell ())
				{
					final ServerGridCellEx serverCell = (ServerGridCellEx) cell;
					if ((serverCell.getTerrainData ().getMapFeatureID () != null) && (!serverCell.getTerrainData ().getMapFeatureID ().equals ("X")))
					{
						// Lair was placed here
						if ((!serverCell.getTerrainData ().getTileTypeID ().equals ("TT01")) && (!serverCell.getTerrainData ().getTileTypeID ().equals ("TT02")) && (!serverCell.getTerrainData ().getTileTypeID ().equals ("TT03")))
							fail ("Lairs should only be placed on TT01..03, but found " + serverCell.getTerrainData ().getTileTypeID ());

						if ((!serverCell.getTerrainData ().getMapFeatureID ().equals ("MF01")) && (!serverCell.getTerrainData ().getMapFeatureID ().equals ("MF02")) && (!serverCell.getTerrainData ().getMapFeatureID ().equals ("MF03")))
							fail ("Only map features MF01..03 should be placed, but found " + serverCell.getTerrainData ().getMapFeatureID ());

						assertNotNull (serverCell.getNodeLairTowerPowerProportion ());
						
						if ((serverCell.getNodeLairTowerPowerProportion () < 0) || (serverCell.getNodeLairTowerPowerProportion () > 1))
							fail ("NodeLairTowerPowerProportion must be between 0..1");
						
						assertNotNull (serverCell.isLairWeak ());
						if (serverCell.isLairWeak ())
							weakCount++;
						else
							strongCount++;
					}
					else
					{
						// Lair wasn't placed here
						assertNull (serverCell.getNodeLairTowerPowerProportion ());
						assertNull (serverCell.isLairWeak ());
					}
				}
		
		assertEquals (10, weakCount);
		assertEquals (15, strongCount);
		
		// Try to place 15 more, to prove that it'll just place as many as will fit (5) and leave it at that
		mapGen.placeLairs (terrain, overlandMapSize, db, 15, false);

		weakCount = 0;
		strongCount = 0;
		for (final MapAreaOfMemoryGridCells plane : terrain.getPlane ())
			for (final MapRowOfMemoryGridCells row : plane.getRow ())
				for (final MemoryGridCell cell : row.getCell ())
				{
					final ServerGridCellEx serverCell = (ServerGridCellEx) cell;
					if ((serverCell.getTerrainData ().getMapFeatureID () != null) && (!serverCell.getTerrainData ().getMapFeatureID ().equals ("X")))
					{
						// Lair was placed here
						if ((!serverCell.getTerrainData ().getTileTypeID ().equals ("TT01")) && (!serverCell.getTerrainData ().getTileTypeID ().equals ("TT02")) && (!serverCell.getTerrainData ().getTileTypeID ().equals ("TT03")))
							fail ("Lairs should only be placed on TT01..03, but found " + serverCell.getTerrainData ().getTileTypeID ());

						if ((!serverCell.getTerrainData ().getMapFeatureID ().equals ("MF01")) && (!serverCell.getTerrainData ().getMapFeatureID ().equals ("MF02")) && (!serverCell.getTerrainData ().getMapFeatureID ().equals ("MF03")))
							fail ("Only map features MF01..03 should be placed, but found " + serverCell.getTerrainData ().getMapFeatureID ());

						assertNotNull (serverCell.getNodeLairTowerPowerProportion ());
						
						if ((serverCell.getNodeLairTowerPowerProportion () < 0) || (serverCell.getNodeLairTowerPowerProportion () > 1))
							fail ("NodeLairTowerPowerProportion must be between 0..1");
						
						assertNotNull (serverCell.isLairWeak ());
						if (serverCell.isLairWeak ())
							weakCount++;
						else
							strongCount++;
					}
					else
					{
						// Lair wasn't placed here
						assertNull (serverCell.getNodeLairTowerPowerProportion ());
						assertNull (serverCell.isLairWeak ());
					}
				}
		
		assertEquals (10, weakCount);
		assertEquals (20, strongCount);
	}

	/**
	 * @param tileTypeID Tile type ID to output
	 * @return Single letter to output for this tile type ID
	 * @throws MomException If we don't know the letter to output for the requested tile type
	 */
	private final String tileTypeIdToSingleLetter (final String tileTypeID) throws MomException
	{
		final String result;
		if (tileTypeID.equals (CommonDatabaseConstants.TILE_TYPE_OCEAN))
			result = ".";
		else if (tileTypeID.equals (CommonDatabaseConstants.TILE_TYPE_GRASS))
			result = "G";
		else if (tileTypeID.equals (CommonDatabaseConstants.TILE_TYPE_TUNDRA))
			result = "T";
		else if (tileTypeID.equals (CommonDatabaseConstants.TILE_TYPE_HILLS))
			result = "H";
		else if (tileTypeID.equals (CommonDatabaseConstants.TILE_TYPE_MOUNTAIN))
			result = "^";
		else if (tileTypeID.equals (CommonDatabaseConstants.TILE_TYPE_SHORE))
			result = "S";
		else if (tileTypeID.equals (CommonDatabaseConstants.TILE_TYPE_FOREST))
			result = "F";
		else if (tileTypeID.equals (CommonDatabaseConstants.TILE_TYPE_DESERT))
			result = "D";
		else if (tileTypeID.equals (CommonDatabaseConstants.TILE_TYPE_SWAMP))
			result = "W";
		else if (tileTypeID.equals (CommonDatabaseConstants.TILE_TYPE_RIVER))
			result = "R";
		else if (tileTypeID.equals (CommonDatabaseConstants.TILE_TYPE_OCEANSIDE_RIVER_MOUTH))
			result = "O";
		else if (tileTypeID.equals (CommonDatabaseConstants.TILE_TYPE_LANDSIDE_RIVER_MOUTH))
			result = "L";
		else if ((tileTypeID.equals ("TT12")) || (tileTypeID.equals ("TT13")) || (tileTypeID.equals ("TT14")))
			result = "N";
		else
			throw new MomException ("tileTypeIdToSingleLetter doesn't know a letter to output for tile type \"" + tileTypeID + "\"");

		return result;
	}

	/**
	 * Tests the generateOverlandTerrain method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateOverlandTerrain () throws Exception
	{
		final CommonDatabase db = loadServerDatabase ();

		final MomSessionDescription sd = createMomSessionDescription (db, "MS03", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		// Map storage
		final FogOfWarMemory fow = new FogOfWarMemory ();

		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (fow);

		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		
		// Set up object to test
		final OverlandMapGeneratorImpl mapGen = new OverlandMapGeneratorImpl ();
		
		mapGen.setMemoryGridCellUtils (new MemoryGridCellUtilsImpl ());

		// Need real random number generator to get a nice map out
		final CoordinateSystemUtilsImpl coordinateSystemUtils = new CoordinateSystemUtilsImpl ();
		
		final BooleanMapAreaOperations2DImpl op = new BooleanMapAreaOperations2DImpl ();
		op.setCoordinateSystemUtils (coordinateSystemUtils);
		
		final RandomUtils random = new RandomUtilsImpl ();
		op.setRandomUtils (random);
		mapGen.setRandomUtils (random);
		mapGen.setBooleanMapAreaOperations2D (op);
		mapGen.setCoordinateSystemUtils (coordinateSystemUtils);
		
		// Run method
		mapGen.generateOverlandTerrain (mom);
		
		// Check all the map cells look valid
		assertEquals (sd.getOverlandMapSize ().getDepth (), fow.getMap ().getPlane ().size ());
		
		// Check that all node/lair/towers have their proportion set between 0 and 1 (and that the proportion isn't set for any other cells)

		// Dump the maps to the standard output
		for (final Plane plane : db.getPlane ())
		{
			System.out.println (plane.getPlaneDescription () + ":");
			for (int y = 0; y < sd.getOverlandMapSize ().getHeight (); y++)
			{
				String row = "";
				for (int x = 0; x < sd.getOverlandMapSize ().getWidth (); x++)
					row = row + tileTypeIdToSingleLetter (fow.getMap ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x).getTerrainData ().getTileTypeID ());

				System.out.println (row);
			}

			System.out.println ();
		}
		
		// Save the generated map out to an XML file, so the bitmap generator in the client can test generating a real bitmap of it
		final StoredOverlandMap container = new StoredOverlandMap ();
		container.setOverlandMap (fow.getMap ());
		
		final URL xsdResource = getClass ().getResource ("/momime.unittests.mapstorage/MapStorage.xsd");
		assertNotNull (xsdResource, "Map storage XSD could not be found on classpath");

		final SchemaFactory schemaFactory = SchemaFactory.newInstance (XMLConstants.W3C_XML_SCHEMA_NS_URI);
		schemaFactory.setResourceResolver (new CommonXsdResourceResolver (DOMImplementationRegistry.newInstance ()));
		
		final Marshaller marshaller = JAXBContext.newInstance (StoredOverlandMap.class).createMarshaller ();
		marshaller.marshal (container, new File ("target/generatedOverlandMap.xml"));
	}

	/**
	 * Tests the generateInitialCombatAreaEffects method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateInitialCombatAreaEffects () throws Exception
	{
		// Session description
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);

		// Mock server database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final TileTypeEx ocean = new TileTypeEx ();
		ocean.setTileTypeID (CommonDatabaseConstants.TILE_TYPE_OCEAN);
		when (db.findTileType (ocean.getTileTypeID (), "generateInitialCombatAreaEffects")).thenReturn (ocean);
		
		final TileTypeEx dudTileType = new TileTypeEx ();
		dudTileType.setTileTypeID ("TT01");
		when (db.findTileType (dudTileType.getTileTypeID (), "generateInitialCombatAreaEffects")).thenReturn (dudTileType);

		final TileTypeEx activeTileType = new TileTypeEx ();
		activeTileType.setTileTypeID ("TT02");
		when (db.findTileType (activeTileType.getTileTypeID (), "generateInitialCombatAreaEffects")).thenReturn (activeTileType);
		
		final TileTypeAreaEffect centreOnly = new TileTypeAreaEffect ();
		centreOnly.setCombatAreaEffectID ("CAE01");
		centreOnly.setExtendAcrossNodeAura (false);
		activeTileType.getTileTypeAreaEffect ().add (centreOnly);

		final TileTypeAreaEffect expandsAcross = new TileTypeAreaEffect ();
		expandsAcross.setCombatAreaEffectID ("CAE02");
		expandsAcross.setExtendAcrossNodeAura (true);
		activeTileType.getTileTypeAreaEffect ().add (expandsAcross);
		
		// Map storage
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (fow);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		
		// Set up object to test
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		
		final OverlandMapGeneratorImpl mapGen = new OverlandMapGeneratorImpl ();
		mapGen.setFogOfWarMidTurnChanges (midTurn);

		// Set 2 special tile types, one which generates some CAEs and one which doesn't
		// and a couple of auras which point back at those tile types, again one which generates some CAEs and one which doesn't
		fow.setMap (mapGen.setAllToWater (overlandMapSize));
		fow.getMap ().getPlane ().get (0).getRow ().get (10).getCell ().get (20).getTerrainData ().setTileTypeID ("TT01");
		for (int x = 19; x <= 21; x++)
			((ServerGridCellEx) fow.getMap ().getPlane ().get (0).getRow ().get (10).getCell ().get (x)).setAuraFromNode (new MapCoordinates3DEx (20, 10, 0));

		fow.getMap ().getPlane ().get (0).getRow ().get (10).getCell ().get (40).getTerrainData ().setTileTypeID ("TT02");
		for (int x = 39; x <= 41; x++)
			((ServerGridCellEx) fow.getMap ().getPlane ().get (0).getRow ().get (10).getCell ().get (x)).setAuraFromNode (new MapCoordinates3DEx (40, 10, 0));
		
		// Run method
		mapGen.generateInitialCombatAreaEffects (mom);
		
		// Check results
		verify (midTurn).addCombatAreaEffectOnServerAndClients (gsk, "CAE02", null, null, null, new MapCoordinates3DEx (39, 10, 0), null, sd);
		verify (midTurn).addCombatAreaEffectOnServerAndClients (gsk, "CAE01", null, null, null, new MapCoordinates3DEx (40, 10, 0), null, sd);
		verify (midTurn).addCombatAreaEffectOnServerAndClients (gsk, "CAE02", null, null, null, new MapCoordinates3DEx (40, 10, 0), null, sd);
		verify (midTurn).addCombatAreaEffectOnServerAndClients (gsk, "CAE02", null, null, null, new MapCoordinates3DEx (41, 10, 0), null, sd);
		
		verifyNoMoreInteractions (midTurn);
	}

	/**
	 * Tests the findMostExpensiveMonster method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFindMostExpensiveMonster () throws Exception
	{
		// Mock server database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up 10 units, costing 0..900, except every other one is the wrong magic realm
		final List<UnitEx> unitDefs = new ArrayList<UnitEx> ();
		for (int n = 0; n < 10; n++)
		{
			final UnitEx unitDef = new UnitEx ();
			unitDef.setUnitID ("UN00" + n);
			unitDef.setProductionCost (n * 100);
			unitDef.setUnitMagicRealm ("MB0" + (n % 2));
			
			unitDefs.add (unitDef);
		}
		
		// Add another with no production cost at all, just to prove it doesn't break
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitID ("UN010");
		unitDef.setUnitMagicRealm ("MB00");
		unitDefs.add (unitDef);
		
		when (db.getUnits ()).thenReturn (unitDefs);
		
		// Set up object to test
		final OverlandMapGeneratorImpl mapGen = new OverlandMapGeneratorImpl ();
		
		// Run method
		assertEquals ("UN008", mapGen.findMostExpensiveMonster ("MB00", 1000, db).getUnitID ());
		assertEquals ("UN008", mapGen.findMostExpensiveMonster ("MB00", 801, db).getUnitID ());
		assertEquals ("UN008", mapGen.findMostExpensiveMonster ("MB00", 800, db).getUnitID ());
		assertEquals ("UN006", mapGen.findMostExpensiveMonster ("MB00", 799, db).getUnitID ());
		assertEquals ("UN002", mapGen.findMostExpensiveMonster ("MB00", 250, db).getUnitID ());
		
		// Won't pick the one with zero production cost, even though that's technically the most expensive
		assertNull (mapGen.findMostExpensiveMonster ("MB00", 99, db));
	}
	
	/**
	 * Tests the fillSingleLairOrTowerWithMonsters method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFillSingleLairOrTowerWithMonsters () throws Exception
	{
		// Mock server database
		final CommonDatabase db = mock (CommonDatabase.class);

		final List<UnitEx> unitDefs = new ArrayList<UnitEx> ();
		for (int n = 0; n < 10; n++)
		{
			final UnitEx unitDef = new UnitEx ();
			unitDef.setUnitID ("UN00" + n);
			unitDef.setProductionCost (n * 100);
			unitDef.setUnitMagicRealm ("MB01");
			
			unitDefs.add (unitDef);
		}		

		when (db.getUnits ()).thenReturn (unitDefs);
		
		// Set up monster player
		final PlayerServerDetails monsterPlayer = new PlayerServerDetails (null, null, null, null, null); 
		
		// Mock random number generator
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (4)).thenReturn (2);					// Divide 5400 budget by d4 roll of 3 = 1800 max price per main monster, so it'll pick UN009 at 900 pts
		when (random.nextBoolean ()).thenReturn (true);		// Yes put one of the 6 main monsters back
		when (random.nextInt (5)).thenReturn (1);					// Divide 900 budget for secondary monsters by d5 roll of 2 = 450 max price per secondary monster, so picks UN004 
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		
		// Set up object to test
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		
		final OverlandMapGeneratorImpl mapGen = new OverlandMapGeneratorImpl ();
		mapGen.setRandomUtils (random);
		mapGen.setFogOfWarMidTurnChanges (midTurn);
		
		// Run method
		final MapCoordinates3DEx coords = new MapCoordinates3DEx (20, 10, 0);
		mapGen.fillSingleLairOrTowerWithMonsters (coords, "MB01", 3000, 6000, 0.8, monsterPlayer, mom);		// 0.8 thru 3000-6000 is 5400
		
		// Check results
		verify (midTurn, times (5)).addUnitOnServerAndClients ("UN009", coords, null, null, null, monsterPlayer, UnitStatusID.ALIVE, false, false, mom);
		verify (midTurn, times (2)).addUnitOnServerAndClients ("UN004", coords, null, null, null, monsterPlayer, UnitStatusID.ALIVE, false, false, mom);
		
		verifyNoMoreInteractions (midTurn);
	}
}