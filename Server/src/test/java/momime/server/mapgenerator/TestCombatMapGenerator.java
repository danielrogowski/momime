package momime.server.mapgenerator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.CombatMapLayerID;
import momime.common.messages.MemoryBuildingUtils;
import momime.common.messages.MemoryMaintainedSpellUtils;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MapAreaOfCombatTiles;
import momime.common.messages.v0_9_4.MapRowOfCombatTiles;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.MemoryGridCell;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.MomCombatTile;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.common.utils.CombatMapUtils;
import momime.common.utils.ICombatMapUtils;
import momime.server.ServerTestData;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseValues;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;

/**
 * Tests the CombatMapGenerator class
 */
public final class TestCombatMapGenerator
{
	/**
	 * Tests the setAllToGrass method
	 */
	@Test
	public final void testSetAllToGrass ()
	{
		final CombatMapGenerator mapGen = new CombatMapGenerator ();
		final MapAreaOfCombatTiles map = mapGen.setAllToGrass ();
		
		// Check results
		int count = 0;
		for (final MapRowOfCombatTiles row : map.getRow ())
			for (final MomCombatTile cell : row.getCell ())
			{
				count++;
				assertNotNull (cell);
				assertNull (cell.getBorderDirections ());
				assertEquals (0, cell.getBorderID ().size ());
				assertEquals (1, cell.getTileLayer ().size ());
				assertEquals (CombatMapLayerID.TERRAIN, cell.getTileLayer ().get (0).getLayer ());
				assertEquals (ServerDatabaseValues.VALUE_COMBAT_TILE_TYPE_GRASS, cell.getTileLayer ().get (0).getCombatTileTypeID ());
			}

		assertEquals (CombatMapGenerator.COMBAT_MAP_WIDTH * CombatMapGenerator.COMBAT_MAP_HEIGHT, count);
		
		// Spot check a few
		assertTrue (map.getRow ().get (0).getCell ().get (0).isOffMapEdge ());
		assertTrue (map.getRow ().get (1).getCell ().get (1).isOffMapEdge ());
		assertFalse (map.getRow ().get (2).getCell ().get (1).isOffMapEdge ());
		assertFalse (map.getRow ().get (2).getCell ().get (10).isOffMapEdge ());
		assertTrue (map.getRow ().get (1).getCell ().get (10).isOffMapEdge ());
	}
	
	/**
	 * Tests the setTerrainFeaturesRandomly method
	 */
	@Test
	public final void testSetTerrainFeaturesRandomly ()
	{
		final CombatMapGenerator mapGen = new CombatMapGenerator ();
		final MapAreaOfCombatTiles map = ServerTestData.createCombatMap ();
		final CombatMapUtils utils = new CombatMapUtils ();

		// Test none
		mapGen.setTerrainFeaturesRandomly (map, "A", 0);
		int count = 0;
		for (final MapRowOfCombatTiles row : map.getRow ())
			for (final MomCombatTile cell : row.getCell ())
				if ("A".equals (utils.getCombatTileTypeForLayer (cell, CombatMapLayerID.BUILDINGS_AND_TERRAIN_FEATURES)))
					count++;
		
		assertEquals (0, count);
		
		// Test small number
		mapGen.setTerrainFeaturesRandomly (map, "B", 10);
		count = 0;
		for (final MapRowOfCombatTiles row : map.getRow ())
			for (final MomCombatTile cell : row.getCell ())
			{
				if ("B".equals (utils.getCombatTileTypeForLayer (cell, CombatMapLayerID.BUILDINGS_AND_TERRAIN_FEATURES)))
					count++;
				
				// Clear it ready for the next test
				cell.getTileLayer ().clear ();
			}
		
		assertEquals (10, count);
		
		// Test asking for more than there are spaces for
		mapGen.setTerrainFeaturesRandomly (map, "C", 100000);
		count = 0;
		for (final MapRowOfCombatTiles row : map.getRow ())
			for (final MomCombatTile cell : row.getCell ())
				if ("C".equals (utils.getCombatTileTypeForLayer (cell, CombatMapLayerID.BUILDINGS_AND_TERRAIN_FEATURES)))
					count++;
		
		assertEquals (CombatMapGenerator.COMBAT_MAP_WIDTH * CombatMapGenerator.COMBAT_MAP_HEIGHT, count);
	}
	
	/**
	 * Tests the placeCombatMapElements method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 */
	@Test
	public final void testPlaceCombatMapElements () throws JAXBException, IOException
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MapAreaOfCombatTiles map = ServerTestData.createCombatMap ();
		
		// Needs the overland map too, to reference the map cell for what terrain is there, and the building+spell lists
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueMap = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueTerrain = new FogOfWarMemory ();
		trueTerrain.setMap (trueMap);
		
		// Overland map location to test
		final OverlandMapCoordinatesEx combatMapLocation = new OverlandMapCoordinatesEx ();
		combatMapLocation.setPlane (1);
		combatMapLocation.setX (20);
		combatMapLocation.setY (15);

		final MemoryGridCell mc = trueTerrain.getMap ().getPlane ().get (1).getRow ().get (15).getCell ().get (20);
		
		// Set up class
		final CombatMapUtils utils = new CombatMapUtils ();
		
		final CombatMapGenerator mapGen = new CombatMapGenerator ();
		mapGen.setCombatMapUtils (utils);
		mapGen.setMemoryBuildingUtils (new MemoryBuildingUtils ());
		mapGen.setMemoryMaintainedSpellUtils (new MemoryMaintainedSpellUtils ());
		
		// If we have no population, no buildings, no tile type, no map feature or anything at all - then method should just run through and do nothing
		mapGen.placeCombatMapElements (map, db, trueTerrain, combatMapLocation);
		for (final MapRowOfCombatTiles row : map.getRow ())
			for (final MomCombatTile cell : row.getCell ())
				assertEquals (0, cell.getTileLayer ().size ());
		
		// Element from a building
		final MemoryBuilding fortress = new MemoryBuilding ();
		fortress.setCityLocation (combatMapLocation);
		fortress.setBuildingID (CommonDatabaseConstants.VALUE_BUILDING_FORTRESS);
		
		trueTerrain.getBuilding ().add (fortress);

		mapGen.placeCombatMapElements (map, db, trueTerrain, combatMapLocation);
		for (int y = 0; y < CombatMapGenerator.COMBAT_MAP_HEIGHT; y++)
			for (int x = 0; x < CombatMapGenerator.COMBAT_MAP_WIDTH; x++)
			{
				final MomCombatTile cell = map.getRow ().get (y).getCell ().get (x);
				if ((x == 3) &&  (y == 9))	// Wizard's fortress combat element coordinates from the server XML file
				{
					assertEquals (1, cell.getTileLayer ().size ());
					assertEquals (CombatMapLayerID.BUILDINGS_AND_TERRAIN_FEATURES, cell.getTileLayer ().get (0).getLayer ());
					assertEquals ("CBL03", cell.getTileLayer ().get (0).getCombatTileTypeID ());
				}
				else
					assertEquals (0, cell.getTileLayer ().size ());
			}
		
		// Element based on tile type
		trueTerrain.getBuilding ().clear ();
		
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT12");		// Sorcery node
		mc.setTerrainData (terrainData);

		mapGen.placeCombatMapElements (map, db, trueTerrain, combatMapLocation);
		for (int y = 0; y < CombatMapGenerator.COMBAT_MAP_HEIGHT; y++)
			for (int x = 0; x < CombatMapGenerator.COMBAT_MAP_WIDTH; x++)
			{
				final MomCombatTile cell = map.getRow ().get (y).getCell ().get (x);
				if ((x == 3) &&  (y == 9))
				{
					assertEquals (1, cell.getTileLayer ().size ());
					assertEquals (CombatMapLayerID.BUILDINGS_AND_TERRAIN_FEATURES, cell.getTileLayer ().get (0).getLayer ());
					assertEquals ("CBL10", cell.getTileLayer ().get (0).getCombatTileTypeID ());
				}
				else
					assertEquals (0, cell.getTileLayer ().size ());
			}
		
		// Element based on map feature
		terrainData.setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_GRASS);		// i.e. anything that doesn't produce a combat map element
		terrainData.setMapFeatureID ("MF19");		// Fallen temple

		mapGen.placeCombatMapElements (map, db, trueTerrain, combatMapLocation);
		for (int y = 0; y < CombatMapGenerator.COMBAT_MAP_HEIGHT; y++)
			for (int x = 0; x < CombatMapGenerator.COMBAT_MAP_WIDTH; x++)
			{
				final MomCombatTile cell = map.getRow ().get (y).getCell ().get (x);
				if ((x == 3) &&  (y == 9))
				{
					assertEquals (1, cell.getTileLayer ().size ());
					assertEquals (CombatMapLayerID.BUILDINGS_AND_TERRAIN_FEATURES, cell.getTileLayer ().get (0).getLayer ());
					assertEquals ("CBL09", cell.getTileLayer ().get (0).getCombatTileTypeID ());
				}
				else
					assertEquals (0, cell.getTileLayer ().size ());
			}
		
		// Element based on a spell
		terrainData.setMapFeatureID (null);
		for (final MapRowOfCombatTiles row : map.getRow ())
			for (final MomCombatTile cell : row.getCell ())
				cell.getTileLayer ().clear ();
		
		final MemoryMaintainedSpell wallOfFire = new MemoryMaintainedSpell ();
		wallOfFire.setSpellID ("SP087");
		wallOfFire.setCityLocation (combatMapLocation);
		
		trueTerrain.getMaintainedSpell ().add (wallOfFire);

		mapGen.placeCombatMapElements (map, db, trueTerrain, combatMapLocation);
		int wallOfFireTileCount = 0;
		for (int y = 0; y < CombatMapGenerator.COMBAT_MAP_HEIGHT; y++)
			for (int x = 0; x < CombatMapGenerator.COMBAT_MAP_WIDTH; x++)
			{
				final MomCombatTile cell = map.getRow ().get (y).getCell ().get (x);
				assertEquals (0, cell.getTileLayer ().size ());
				
				if (cell.getBorderID ().size () == 0)
					assertNull (cell.getBorderDirections ());
				else
				{
					wallOfFireTileCount++;
					assertEquals (1, cell.getBorderID ().size ());
					assertEquals ("CTB04", cell.getBorderID ().get (0));
					assertNotNull (cell.getBorderDirections ());
				}
			}
		
		assertEquals (16, wallOfFireTileCount);
		
		// Element from population, note this puts all kinds of city roads down too as well as the houses
		trueTerrain.getMaintainedSpell ().clear ();
		
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityPopulation (2700);
		mc.setCityData (cityData);

		mapGen.placeCombatMapElements (map, db, trueTerrain, combatMapLocation);
		int roadCount = 0;
		for (int y = 0; y < CombatMapGenerator.COMBAT_MAP_HEIGHT; y++)
			for (int x = 0; x < CombatMapGenerator.COMBAT_MAP_WIDTH; x++)
			{
				final MomCombatTile cell = map.getRow ().get (y).getCell ().get (x);
				if (((x == 4) && (y == 8)) || ((x == 2) && (y == 9)))
				{
					assertEquals (2, cell.getTileLayer ().size ());
					assertEquals (CombatMapLayerID.BUILDINGS_AND_TERRAIN_FEATURES, cell.getTileLayer ().get (0).getLayer ());		// House
					assertEquals ("CBL02", cell.getTileLayer ().get (0).getCombatTileTypeID ());
					assertEquals (CombatMapLayerID.ROAD, cell.getTileLayer ().get (1).getLayer ());	// Road
					assertEquals ("CRL01", cell.getTileLayer ().get (1).getCombatTileTypeID ());
				}
				else if ((x == 4) && (y == 11))		// Special road tile for city entryway
				{
					assertEquals (1, cell.getTileLayer ().size ());
					assertEquals (CombatMapLayerID.ROAD, cell.getTileLayer ().get (0).getLayer ());
					assertEquals ("CRL02", cell.getTileLayer ().get (0).getCombatTileTypeID ());
				}
				else if (cell.getTileLayer ().size () > 0)
				{
					roadCount++;
					assertEquals (1, cell.getTileLayer ().size ());
					assertEquals (CombatMapLayerID.ROAD, cell.getTileLayer ().get (0).getLayer ());
					assertEquals ("CRL01", cell.getTileLayer ().get (0).getCombatTileTypeID ());
				}
			}
		
		assertEquals (22, roadCount);		// Note there's 24 in the DB, but we counted 2 in the check for the houses cells
	}
	
	/**
	 * @param tile Tile to output
	 * @param utils Utils needed to access the layers of the tile
	 * @return Two letter to output for this tile type ID
	 * @throws MomException If we don't know the letter to output for the requested tile type
	 */
	private final String outputCombatTile (final MomCombatTile tile, final ICombatMapUtils utils) throws MomException
	{
		// Terrain layer
		final String terrainTileTypeID = utils.getCombatTileTypeForLayer (tile, CombatMapLayerID.TERRAIN);
		String result;
		if (terrainTileTypeID.equals (ServerDatabaseValues.VALUE_COMBAT_TILE_TYPE_GRASS))
			result = ".";
		else if (terrainTileTypeID.equals (ServerDatabaseValues.VALUE_COMBAT_TILE_TYPE_DARK))
			result = "v";
		else if (terrainTileTypeID.equals (ServerDatabaseValues.VALUE_COMBAT_TILE_TYPE_RIDGE))
			result = "^";
		else
			throw new MomException ("outputCombatTile doesn't know a letter to output for terrain combat tile type \"" + terrainTileTypeID + "\"");
		
		// Features and buildings layer
		final String featureTileTypeID = utils.getCombatTileTypeForLayer (tile, CombatMapLayerID.BUILDINGS_AND_TERRAIN_FEATURES);
		if (featureTileTypeID == null)
			result = result + " ";
		else if (featureTileTypeID.equals (ServerDatabaseValues.VALUE_COMBAT_TILE_TERRAIN_FEATURE))
			result = result + "T";
		else if (featureTileTypeID.equals ("CBL02"))		// House
			result = result + "H";
		else if (featureTileTypeID.equals ("CBL03"))		// Wizard's fortress
			result = result + "W";
		else
			throw new MomException ("outputCombatTile doesn't know a letter to output for feature combat tile type \"" + featureTileTypeID + "\"");
		
		// Road layer
		final String roadTileTypeID = utils.getCombatTileTypeForLayer (tile, CombatMapLayerID.ROAD);
		if (roadTileTypeID == null)
			result = result + " ";
		else
			result = result + "R";

		return result;
	}

	/**
	 * Tests the generateCombatMap method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws MomException If some fatal error happens during map generation
	 * @throws RecordNotFoundException If some entry isn't found in the db during map generation, or one of the smoothing borders isn't found in the fixed arrays
	 */
	@Test
	public final void testGenerateCombatMap () throws IOException, JAXBException, MomException, RecordNotFoundException
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final FogOfWarMemory fow = new FogOfWarMemory ();
		fow.setMap (ServerTestData.createOverlandMap (sd.getMapSize ()));
		
		// Set up class
		final CombatMapUtils utils = new CombatMapUtils ();
		
		final CombatMapGenerator mapGen = new CombatMapGenerator ();
		mapGen.setCombatMapUtils (utils);
		mapGen.setMemoryBuildingUtils (new MemoryBuildingUtils ());
		mapGen.setMemoryMaintainedSpellUtils (new MemoryMaintainedSpellUtils ());
		
		// Location
		final OverlandMapCoordinatesEx combatMapLocation = new OverlandMapCoordinatesEx ();
		combatMapLocation.setPlane (1);
		combatMapLocation.setX (20);
		combatMapLocation.setY (15);
		
		// Put a city here so we get some buildings
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_GRASS);
		
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityPopulation (6000);
		
		final MemoryGridCell mc = fow.getMap ().getPlane ().get (1).getRow ().get (15).getCell ().get (20);
		mc.setTerrainData (terrainData);
		mc.setCityData (cityData);
		
		// And a wizard's fortress
		final MemoryBuilding fortress = new MemoryBuilding ();
		fortress.setCityLocation (combatMapLocation);
		fortress.setBuildingID (CommonDatabaseConstants.VALUE_BUILDING_FORTRESS);
		
		fow.getBuilding ().add (fortress);

		// Run method
		final MapAreaOfCombatTiles map = mapGen.generateCombatMap (db, fow, combatMapLocation);

		// We can't 'test' the output, only that the generation doesn't fail, but interesting to dump the maps to the standard output
		System.out.println ("Combat map:");
		for (int y = 0; y < CombatMapGenerator.COMBAT_MAP_HEIGHT; y++)
		{
			String row = "";
			for (int x = 0; x < CombatMapGenerator.COMBAT_MAP_WIDTH; x++)
				row = row + outputCombatTile (map.getRow ().get (y).getCell ().get (x), utils);

			System.out.println (row);
		}
		System.out.println ();
	}
}
