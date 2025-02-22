package momime.server.mapgenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.utils.random.RandomUtils;
import com.ndg.utils.random.RandomUtilsImpl;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import momime.common.MomException;
import momime.common.database.CombatMapLayerID;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.CommonXsdResourceResolver;
import momime.common.database.TileTypeEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MapRowOfCombatTiles;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MomCombatTile;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.utils.CombatMapUtils;
import momime.common.utils.CombatMapUtilsImpl;
import momime.common.utils.MemoryBuildingUtilsImpl;
import momime.common.utils.MemoryMaintainedSpellUtilsImpl;
import momime.server.ServerTestData;
import momime.server.utils.CombatMapServerUtilsImpl;
import momime.unittests.mapstorage.StoredCombatMap;

/**
 * Tests the CombatMapGeneratorImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestCombatMapGeneratorImpl extends ServerTestData
{
	/**
	 * Tests the setAllToDefaultTerrain method
	 */
	@Test
	public final void testSetAllToDefaultTerrain ()
	{
		final TileTypeEx tileType = new TileTypeEx ();
		tileType.setCombatTileTypeID ("X");
		
		final CombatMapGeneratorImpl mapGen = new CombatMapGeneratorImpl ();
		final MapAreaOfCombatTiles map = mapGen.setAllToDefaultTerrain (createCombatMapCoordinateSystem (), tileType);
		
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
				assertEquals ("X", cell.getTileLayer ().get (0).getCombatTileTypeID ());
			}

		assertEquals (CommonDatabaseConstants.COMBAT_MAP_WIDTH * CommonDatabaseConstants.COMBAT_MAP_HEIGHT, count);
		
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
		final CoordinateSystem sys = createCombatMapCoordinateSystem ();
		
		final CombatMapGeneratorImpl mapGen = new CombatMapGeneratorImpl ();
		mapGen.setRandomUtils (new RandomUtilsImpl ());
		
		final MapAreaOfCombatTiles map = createCombatMap ();
		final CombatMapUtilsImpl utils = new CombatMapUtilsImpl ();

		// Test none
		mapGen.setTerrainFeaturesRandomly (map, sys, "A", 0);
		int count = 0;
		for (final MapRowOfCombatTiles row : map.getRow ())
			for (final MomCombatTile cell : row.getCell ())
				if ("A".equals (utils.getCombatTileTypeForLayer (cell, CombatMapLayerID.BUILDINGS_AND_TERRAIN_FEATURES)))
					count++;
		
		assertEquals (0, count);
		
		// Test small number - this could be improved by fixing the random number generator results, but its OK as a test for now
		mapGen.setTerrainFeaturesRandomly (map, sys, "B", 10);
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
		mapGen.setTerrainFeaturesRandomly (map, sys, "C", 100000);
		count = 0;
		for (final MapRowOfCombatTiles row : map.getRow ())
			for (final MomCombatTile cell : row.getCell ())
				if ("C".equals (utils.getCombatTileTypeForLayer (cell, CombatMapLayerID.BUILDINGS_AND_TERRAIN_FEATURES)))
					count++;
		
		assertEquals (CommonDatabaseConstants.COMBAT_MAP_WIDTH * CommonDatabaseConstants.COMBAT_MAP_HEIGHT, count);
	}
	
	/**
	 * Tests the placeCombatMapElements method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testPlaceCombatMapElements () throws Exception
	{
		final CommonDatabase db = loadServerDatabase ();
		final MapAreaOfCombatTiles map = createCombatMap ();
		
		// Needs the overland map too, to reference the map cell for what terrain is there, and the building+spell lists
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueMap = createOverlandMap (sys);
		
		final FogOfWarMemory trueTerrain = new FogOfWarMemory ();
		trueTerrain.setMap (trueMap);
		
		// Overland map location to test
		final MapCoordinates3DEx combatMapLocation = new MapCoordinates3DEx (20, 15, 1);
		final MemoryGridCell mc = trueTerrain.getMap ().getPlane ().get (1).getRow ().get (15).getCell ().get (20);
		
		// Set up class
		final CombatMapServerUtilsImpl utils = new CombatMapServerUtilsImpl ();
		
		final CombatMapGeneratorImpl mapGen = new CombatMapGeneratorImpl ();
		mapGen.setCombatMapServerUtils (utils);
		mapGen.setMemoryBuildingUtils (new MemoryBuildingUtilsImpl ());
		mapGen.setMemoryMaintainedSpellUtils (new MemoryMaintainedSpellUtilsImpl ());
		mapGen.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// If we have no population, no buildings, no tile type, no map feature or anything at all - then method should just run through and do nothing
		mapGen.placeCombatMapElements (map, db, trueTerrain, sys, combatMapLocation);
		for (final MapRowOfCombatTiles row : map.getRow ())
			for (final MomCombatTile cell : row.getCell ())
				assertEquals (0, cell.getTileLayer ().size ());
		
		// Element from a building
		final MemoryBuilding fortress = new MemoryBuilding ();
		fortress.setCityLocation (combatMapLocation);
		fortress.setBuildingID (CommonDatabaseConstants.BUILDING_FORTRESS);
		
		trueTerrain.getBuilding ().add (fortress);

		mapGen.placeCombatMapElements (map, db, trueTerrain, sys, combatMapLocation);
		for (int y = 0; y < CommonDatabaseConstants.COMBAT_MAP_HEIGHT; y++)
			for (int x = 0; x < CommonDatabaseConstants.COMBAT_MAP_WIDTH; x++)
			{
				final MomCombatTile cell = map.getRow ().get (y).getCell ().get (x);
				if ((x == 3) &&  (y == 8))	// Wizard's fortress combat element coordinates from the server XML file
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

		mapGen.placeCombatMapElements (map, db, trueTerrain, sys, combatMapLocation);
		for (int y = 0; y < CommonDatabaseConstants.COMBAT_MAP_HEIGHT; y++)
			for (int x = 0; x < CommonDatabaseConstants.COMBAT_MAP_WIDTH; x++)
			{
				final MomCombatTile cell = map.getRow ().get (y).getCell ().get (x);
				if ((x == 3) &&  (y == 8))
				{
					assertEquals (1, cell.getTileLayer ().size ());
					assertEquals (CombatMapLayerID.BUILDINGS_AND_TERRAIN_FEATURES, cell.getTileLayer ().get (0).getLayer ());
					assertEquals ("CBL10", cell.getTileLayer ().get (0).getCombatTileTypeID ());
				}
				else
					assertEquals (0, cell.getTileLayer ().size ());
			}
		
		// Element based on map feature
		terrainData.setTileTypeID (CommonDatabaseConstants.TILE_TYPE_GRASS);		// i.e. anything that doesn't produce a combat map element
		terrainData.setMapFeatureID ("MF19");		// Fallen temple

		mapGen.placeCombatMapElements (map, db, trueTerrain, sys, combatMapLocation);
		for (int y = 0; y < CommonDatabaseConstants.COMBAT_MAP_HEIGHT; y++)
			for (int x = 0; x < CommonDatabaseConstants.COMBAT_MAP_WIDTH; x++)
			{
				final MomCombatTile cell = map.getRow ().get (y).getCell ().get (x);
				if ((x == 3) &&  (y == 8))
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

		mapGen.placeCombatMapElements (map, db, trueTerrain, sys, combatMapLocation);
		int wallOfFireTileCount = 0;
		for (int y = 0; y < CommonDatabaseConstants.COMBAT_MAP_HEIGHT; y++)
			for (int x = 0; x < CommonDatabaseConstants.COMBAT_MAP_WIDTH; x++)
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
		
		assertEquals (12, wallOfFireTileCount);
		
		// Element from population, note this puts all kinds of city roads down too as well as the houses
		trueTerrain.getMaintainedSpell ().clear ();
		
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityPopulation (2700);
		mc.setCityData (cityData);

		terrainData.setRoadTileTypeID (CommonDatabaseConstants.TILE_TYPE_NORMAL_ROAD);

		mapGen.placeCombatMapElements (map, db, trueTerrain, sys, combatMapLocation);
		int roadCount = 0;
		for (int y = 0; y < CommonDatabaseConstants.COMBAT_MAP_HEIGHT; y++)
			for (int x = 0; x < CommonDatabaseConstants.COMBAT_MAP_WIDTH; x++)
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
				else if ((x == 3) && (y == 11))		// Special road tile for city entryway
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
		
		assertEquals (13, roadCount);		// Note there's 4x4-1=15 in the DB, but we counted 2 in the check for the houses cells
	}
	
	/**
	 * @param tile Tile to output
	 * @param utils Utils needed to access the layers of the tile
	 * @return Two letter to output for this tile type ID
	 * @throws MomException If we don't know the letter to output for the requested tile type
	 */
	private final String outputCombatTile (final MomCombatTile tile, final CombatMapUtils utils) throws MomException
	{
		// Terrain layer
		final String terrainTileTypeID = utils.getCombatTileTypeForLayer (tile, CombatMapLayerID.TERRAIN);
		String result;
		if (terrainTileTypeID.equals ("CTL01"))		// default/grass
			result = ".";
		else if (terrainTileTypeID.equals (CommonDatabaseConstants.COMBAT_TILE_TYPE_DARK))
			result = "v";
		else if (terrainTileTypeID.equals (CommonDatabaseConstants.COMBAT_TILE_TYPE_RIDGE))
			result = "^";
		else
			throw new MomException ("outputCombatTile doesn't know a letter to output for terrain combat tile type \"" + terrainTileTypeID + "\"");
		
		// Features and buildings layer
		final String featureTileTypeID = utils.getCombatTileTypeForLayer (tile, CombatMapLayerID.BUILDINGS_AND_TERRAIN_FEATURES);
		if (featureTileTypeID == null)
			result = result + " ";
		else if (featureTileTypeID.equals (CommonDatabaseConstants.COMBAT_TILE_TERRAIN_FEATURE))
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
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateCombatMap () throws Exception
	{
		final CommonDatabase db = loadServerDatabase ();

		final MomSessionDescription sd = createMomSessionDescription (db, "MS03", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final FogOfWarMemory fow = new FogOfWarMemory ();
		fow.setMap (createOverlandMap (sd.getOverlandMapSize ()));
		
		// Need real random number generator to generate a meaningful map
		final RandomUtils random = new RandomUtilsImpl ();
		
		// Set up class
		final CombatMapGeneratorImpl mapGen = new CombatMapGeneratorImpl ();
		mapGen.setCombatMapServerUtils (new CombatMapServerUtilsImpl ());
		mapGen.setMemoryBuildingUtils (new MemoryBuildingUtilsImpl ());
		mapGen.setMemoryMaintainedSpellUtils (new MemoryMaintainedSpellUtilsImpl ());
		mapGen.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		mapGen.setRandomUtils (random);
		
		// Location
		final MapCoordinates3DEx combatMapLocation = new MapCoordinates3DEx (20, 15, 1);
		
		// Put a city here so we get some buildings
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID (CommonDatabaseConstants.TILE_TYPE_GRASS);
		
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityPopulation (6000);
		
		final MemoryGridCell mc = fow.getMap ().getPlane ().get (1).getRow ().get (15).getCell ().get (20);
		mc.setTerrainData (terrainData);
		mc.setCityData (cityData);
		
		// And a wizard's fortress
		final MemoryBuilding fortress = new MemoryBuilding ();
		fortress.setCityLocation (combatMapLocation);
		fortress.setBuildingID (CommonDatabaseConstants.BUILDING_FORTRESS);
		
		fow.getBuilding ().add (fortress);

		// Run method
		final MapAreaOfCombatTiles map = mapGen.generateCombatMap (sd.getCombatMapSize (), db, fow, sd.getOverlandMapSize (), combatMapLocation);

		// We can't 'test' the output, only that the generation doesn't fail, but interesting to dump the maps to the standard output
		final CombatMapUtilsImpl utils = new CombatMapUtilsImpl ();
		
		System.out.println ("Combat map:");
		for (int y = 0; y < CommonDatabaseConstants.COMBAT_MAP_HEIGHT; y++)
		{
			String row = "";
			for (int x = 0; x < CommonDatabaseConstants.COMBAT_MAP_WIDTH; x++)
				row = row + outputCombatTile (map.getRow ().get (y).getCell ().get (x), utils);

			System.out.println (row);
		}
		System.out.println ();

		// Save the generated map out to an XML file, so the bitmap generator in the client can test generating a real bitmap of it
		final StoredCombatMap container = new StoredCombatMap ();
		container.setCombatMap (map);
		
		final URL xsdResource = getClass ().getResource ("/momime.unittests.mapstorage/MapStorage.xsd");
		assertNotNull (xsdResource, "Map storage XSD could not be found on classpath");

		final SchemaFactory schemaFactory = SchemaFactory.newInstance (XMLConstants.W3C_XML_SCHEMA_NS_URI);
		schemaFactory.setResourceResolver (new CommonXsdResourceResolver (DOMImplementationRegistry.newInstance ()));
		
		final Marshaller marshaller = JAXBContext.newInstance (StoredCombatMap.class).createMarshaller ();
		marshaller.marshal (container, new File ("target/generatedCombatMap.xml"));
	}
}