package momime.server.mapgenerator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MapAreaOfMemoryGridCells;
import momime.common.messages.v0_9_4.MapRowOfMemoryGridCells;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryGridCell;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.server.ServerTestData;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseValues;
import momime.server.database.v0_9_4.Plane;

import org.junit.Test;

/**
 * Tests the OverlandMapGenerator class
 */
public final class TestOverlandMapGenerator
{
	/** Dummy logger to use during unit tests */
	private final Logger debugLogger = Logger.getLogger ("MoMIMEServerUnitTests");

	/**
	 * Tests the setAllToWater method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws MomException If some fatal error happens during map generation
	 * @throws RecordNotFoundException If there is a problem building the session description
	 */
	@Test
	public final void testSetAllToWater () throws IOException, JAXBException, MomException, RecordNotFoundException
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final FogOfWarMemory fow = new FogOfWarMemory ();
		final OverlandMapGenerator mapGen = new OverlandMapGenerator (fow, sd, db, debugLogger);

		mapGen.setAllToWater ();

		// Check results
		int count = 0;
		for (final MapAreaOfMemoryGridCells plane : fow.getMap ().getPlane ())
			for (final MapRowOfMemoryGridCells row : plane.getRow ())
				for (final MemoryGridCell cell : row.getCell ())
				{
					count++;
					assertNotNull (cell);
					assertNotNull (cell.getTerrainData ());
					assertEquals (ServerDatabaseValues.VALUE_TILE_TYPE_OCEAN, cell.getTerrainData ().getTileTypeID ());
				}

		assertEquals (2 * 60 * 40, count);
	}

	/**
	 * Tests the setHighestTiles method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws MomException If some fatal error happens during map generation
	 * @throws RecordNotFoundException If there is a problem building the session description
	 */
	@Test
	public final void testSetHighestTiles () throws IOException, JAXBException, MomException, RecordNotFoundException
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final FogOfWarMemory fow = new FogOfWarMemory ();
		final OverlandMapGenerator mapGen = new OverlandMapGenerator (fow, sd, db, debugLogger);

		// Create a dummy height map, this makes 1 cell of height 0, 2 of height 1, 3 of height 2 and so on, and from the other side
		// 1 of height 98, 2 of height 97, 3 of height 96, 4 of height 95, and so on
		final HeightMapGenerator heightMap = new HeightMapGenerator (sd.getMapSize (),
			sd.getMapSize ().getZoneWidth (), sd.getMapSize ().getZoneHeight (), sd.getLandProportion ().getTundraRowCount (), debugLogger);

		for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
			for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
				heightMap.getZeroBasedHeightMap () [y] [x] = x + y;

		heightMap.countTilesAtEachHeight ();

		// So this should now "fill in" the bottom right hand triangle
		mapGen.setAllToWater ();
		mapGen.setHighestTiles (heightMap, 0, ServerDatabaseValues.VALUE_TILE_TYPE_GRASS, 12);

		// Requested 12, but only choices are 10 and 15, so should round down to marking 10 tiles
		for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
			for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
			{
				if (x + y >= 95)
					assertEquals (ServerDatabaseValues.VALUE_TILE_TYPE_GRASS, fow.getMap ().getPlane ().get (0).getRow ().get (y).getCell ().get (x).getTerrainData ().getTileTypeID ());
				else
					assertEquals (ServerDatabaseValues.VALUE_TILE_TYPE_OCEAN, fow.getMap ().getPlane ().get (0).getRow ().get (y).getCell ().get (x).getTerrainData ().getTileTypeID ());
			}

		// Requested 13, but only choices are 10 and 15, so should round up to marking 15 tiles
		mapGen.setAllToWater ();
		mapGen.setHighestTiles (heightMap, 0, ServerDatabaseValues.VALUE_TILE_TYPE_GRASS, 13);

		for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
			for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
			{
				if (x + y >= 94)
					assertEquals (ServerDatabaseValues.VALUE_TILE_TYPE_GRASS, fow.getMap ().getPlane ().get (0).getRow ().get (y).getCell ().get (x).getTerrainData ().getTileTypeID ());
				else
					assertEquals (ServerDatabaseValues.VALUE_TILE_TYPE_OCEAN, fow.getMap ().getPlane ().get (0).getRow ().get (y).getCell ().get (x).getTerrainData ().getTileTypeID ());
			}
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
		final OverlandMapGenerator gen = new OverlandMapGenerator (null, null, null, debugLogger); 
		assertEquals (32, gen.findTerrainBorder8 ("10000011"));
	}

	/**
	 * Tests the findTerrainBorder8 method to find a bitmask that doesn't exist
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws MomException If some fatal error happens during map generation
	 * @throws RecordNotFoundException If no tile with this bitmask is defined
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindTerrainBorder8_NotFound () throws JAXBException, MomException, RecordNotFoundException
	{
		final OverlandMapGenerator gen = new OverlandMapGenerator (null, null, null, debugLogger); 
		assertEquals (32, gen.findTerrainBorder8 ("10101010"));
	}

	/**
	 * Tests the convertNeighbouringTilesToDirections method
	 * @throws MomException If some fatal error happens during map generation
	 */
	@Test
	public final void testConvertNeighbouringTilesToDirections () throws MomException
	{
		final OverlandMapGenerator gen = new OverlandMapGenerator (null, null, null, debugLogger); 
		assertEquals ("37", gen.convertNeighbouringTilesToDirections ("0101", -1));
		assertEquals ("357", gen.convertNeighbouringTilesToDirections ("0111", -1));
		assertEquals ("37", gen.convertNeighbouringTilesToDirections ("0111", 5));
	}

	/**
	 * Tests the checkAllDirectionsLeadToGrass method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws MomException If some fatal error happens during map generation
	 * @throws RecordNotFoundException If there is a problem building the session description
	 */
	@Test
	public final void testCheckAllDirectionsLeadToGrass () throws IOException, JAXBException, MomException, RecordNotFoundException
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final FogOfWarMemory fow = new FogOfWarMemory ();
		final OverlandMapGenerator mapGen = new OverlandMapGenerator (fow, sd, db, debugLogger);

		// Grass to the east and west (via wrapping), ocean to the south because we didn't overwrite it
		mapGen.setAllToWater ();
		fow.getMap ().getPlane ().get (0).getRow ().get (0).getCell ().get (1).getTerrainData ().setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_GRASS);
		fow.getMap ().getPlane ().get (0).getRow ().get (0).getCell ().get (59).getTerrainData ().setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_GRASS);

		// Run tests
		final boolean [] [] riverPending = new boolean [sd.getMapSize ().getHeight ()] [sd.getMapSize ().getWidth ()];
		assertTrue (mapGen.checkAllDirectionsLeadToGrass (0, 0, 0, "3", riverPending));
		assertTrue (mapGen.checkAllDirectionsLeadToGrass (0, 0, 0, "37", riverPending));
		assertFalse (mapGen.checkAllDirectionsLeadToGrass (0, 0, 0, "357", riverPending));	// because includes an ocean tile
		assertFalse (mapGen.checkAllDirectionsLeadToGrass (0, 0, 0, "137", riverPending));	// because goes off the top of the map

		// Map features are no longer grass
		fow.getMap ().getPlane ().get (0).getRow ().get (0).getCell ().get (59).getTerrainData ().setMapFeatureID ("X");
		assertFalse (mapGen.checkAllDirectionsLeadToGrass (0, 0, 0, "37", riverPending));
		fow.getMap ().getPlane ().get (0).getRow ().get (0).getCell ().get (59).getTerrainData ().setMapFeatureID (null);
		assertTrue (mapGen.checkAllDirectionsLeadToGrass (0, 0, 0, "37", riverPending));

		// Pending rivers will become rivers, so are no longer considered grass either
		riverPending [0] [59] = true;
		assertFalse (mapGen.checkAllDirectionsLeadToGrass (0, 0, 0, "37", riverPending));
	}

	/**
	 * Tests the countStringRepetitions method
	 * @throws MomException If some fatal error happens during map generation
	 */
	@Test
	public final void testCountStringRepetitions () throws MomException
	{
		final OverlandMapGenerator gen = new OverlandMapGenerator (null, null, null, debugLogger); 
		assertEquals ("Zero repetitions", 0, gen.countStringRepetitions ("C", "abcde"));
		assertEquals ("One in the middle", 1, gen.countStringRepetitions ("c", "abcde"));
		assertEquals ("Two in the middle", 2, gen.countStringRepetitions ("c", "abcgcde"));
		assertEquals ("Two including one at the start", 2, gen.countStringRepetitions ("c", "cgcde"));
		assertEquals ("Two including one at the end", 2, gen.countStringRepetitions ("c", "abcgc"));
	}

	/**
	 * Tests the chooseRandomNodeTileTypeID method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws MomException If some fatal error happens during map generation
	 * @throws RecordNotFoundException If we can't find the returned tile type in the DB
	 */
	@Test
	public final void testChooseRandomNodeTileTypeID () throws IOException, JAXBException, MomException, RecordNotFoundException
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells map = new MapVolumeOfMemoryGridCells ();
		final FogOfWarMemory fow = new FogOfWarMemory ();
		fow.setMap (map);

		final OverlandMapGenerator mapGen = new OverlandMapGenerator (fow, sd, db, debugLogger);

		assertNotNull (db.findTileType (mapGen.chooseRandomNodeTileTypeID (), "testChooseRandomNodeTileTypeID").getMagicRealmID ());
	}

	/**
	 * Tests the chooseRandomLairFeatureID method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws MomException If some fatal error happens during map generation
	 * @throws RecordNotFoundException If we can't find the returned map feature in the DB
	 */
	@Test
	public final void testChooseRandomLairFeatureID () throws IOException, JAXBException, MomException, RecordNotFoundException
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells map = new MapVolumeOfMemoryGridCells ();
		final FogOfWarMemory fow = new FogOfWarMemory ();
		fow.setMap (map);

		final OverlandMapGenerator mapGen = new OverlandMapGenerator (fow, sd, db, debugLogger);

		assertTrue (db.findMapFeature (mapGen.chooseRandomLairFeatureID (), "testChooseRandomLairFeatureID").getMapFeatureMagicRealm ().size () > 0);
	}

	/**
	 * Tests the findMostExpensiveMonster method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 */
	@Test
	public final void testFindMostExpensiveMonster () throws IOException, JAXBException
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final OverlandMapGenerator gen = new OverlandMapGenerator (null, null, db, debugLogger); 

		assertEquals ("UN179", gen.findMostExpensiveMonster ("LT01", 1000).getUnitID ());		// Arch angel
		assertEquals ("UN178", gen.findMostExpensiveMonster ("LT01", 900).getUnitID ());		// Angel
		assertEquals ("UN176", gen.findMostExpensiveMonster ("LT01", 500).getUnitID ());		// Unicorns
		assertEquals ("UN177", gen.findMostExpensiveMonster ("LT01", 100).getUnitID ());		// Guardian spirit
		assertNull (gen.findMostExpensiveMonster ("LT01", 45));											// Nothing that cheap
	}

	/**
	 * @param tileTypeID Tile type ID to output
	 * @return Single letter to output for this tile type ID
	 * @throws MomException If we don't know the letter to output for the requested tile type
	 */
	private final String tileTypeIdToSingleLetter (final String tileTypeID) throws MomException
	{
		final String result;
		if (tileTypeID.equals (ServerDatabaseValues.VALUE_TILE_TYPE_OCEAN))
			result = ".";
		else if (tileTypeID.equals (ServerDatabaseValues.VALUE_TILE_TYPE_GRASS))
			result = "G";
		else if (tileTypeID.equals (ServerDatabaseValues.VALUE_TILE_TYPE_TUNDRA))
			result = "T";
		else if (tileTypeID.equals (ServerDatabaseValues.VALUE_TILE_TYPE_HILLS))
			result = "H";
		else if (tileTypeID.equals (ServerDatabaseValues.VALUE_TILE_TYPE_MOUNTAIN))
			result = "^";
		else if (tileTypeID.equals (ServerDatabaseValues.VALUE_TILE_TYPE_SHORE))
			result = "S";
		else if (tileTypeID.equals (CommonDatabaseConstants.VALUE_TILE_TYPE_FOREST))
			result = "F";
		else if (tileTypeID.equals (CommonDatabaseConstants.VALUE_TILE_TYPE_DESERT))
			result = "D";
		else if (tileTypeID.equals (CommonDatabaseConstants.VALUE_TILE_TYPE_SWAMP))
			result = "W";
		else if (tileTypeID.equals (ServerDatabaseValues.VALUE_TILE_TYPE_RIVER))
			result = "R";
		else if (tileTypeID.equals (ServerDatabaseValues.VALUE_TILE_TYPE_OCEANSIDE_RIVER_MOUTH))
			result = "O";
		else if (tileTypeID.equals (ServerDatabaseValues.VALUE_TILE_TYPE_LANDSIDE_RIVER_MOUTH))
			result = "L";
		else if ((tileTypeID.equals ("TT12")) || (tileTypeID.equals ("TT13")) || (tileTypeID.equals ("TT14")))
			result = "N";
		else
			throw new MomException ("tileTypeIdToSingleLetter doesn't know a letter to output for tile type \"" + tileTypeID + "\"");

		return result;
	}

	/**
	 * Tests the generateOverlandTerrain method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws MomException If some fatal error happens during map generation
	 * @throws RecordNotFoundException If some entry isn't found in the db during map generation, or one of the smoothing borders isn't found in the fixed arrays
	 */
	@Test
	public final void testGenerateOverlandTerrain () throws IOException, JAXBException, MomException, RecordNotFoundException
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final FogOfWarMemory fow = new FogOfWarMemory ();
		final OverlandMapGenerator mapGen = new OverlandMapGenerator (fow, sd, db, debugLogger);

		mapGen.generateOverlandTerrain ();

		// We can't 'test' the output, only that the generation doesn't fail, but interesting to dump the maps to the standard output
		for (final Plane plane : db.getPlane ())
		{
			System.out.println (plane.getPlaneDescription () + ":");
			for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
			{
				String row = "";
				for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
					row = row + tileTypeIdToSingleLetter (fow.getMap ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x).getTerrainData ().getTileTypeID ());

				System.out.println (row);
			}

			System.out.println ();
		}
	}
}
