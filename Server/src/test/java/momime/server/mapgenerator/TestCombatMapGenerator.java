package momime.server.mapgenerator;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.CombatMapLayerID;
import momime.common.messages.MemoryBuildingUtils;
import momime.common.messages.MemoryMaintainedSpellUtils;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MapAreaOfCombatTiles;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.MemoryGridCell;
import momime.common.messages.v0_9_4.MomCombatTile;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.common.utils.CombatMapUtils;
import momime.common.utils.ICombatMapUtils;
import momime.server.ServerTestData;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseValues;

import org.junit.Test;

/**
 * Tests the CombatMapGenerator class
 */
public final class TestCombatMapGenerator
{
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
		final OverlandMapCoordinates combatMapLocation = new OverlandMapCoordinates ();
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
