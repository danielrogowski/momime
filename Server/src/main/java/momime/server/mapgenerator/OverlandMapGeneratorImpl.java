package momime.server.mapgenerator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.MapCoordinates2D;
import com.ndg.map.areas.operations.BooleanMapAreaOperations2D;
import com.ndg.map.areas.storage.MapArea2D;
import com.ndg.map.areas.storage.MapArea2DArrayListImpl;
import com.ndg.map.areas.storage.MapArea3D;
import com.ndg.map.areas.storage.MapArea3DArrayListImpl;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DifficultyLevelNodeStrength;
import momime.common.database.DifficultyLevelPlane;
import momime.common.database.LandProportionPlane;
import momime.common.database.LandProportionTileType;
import momime.common.database.MapFeatureEx;
import momime.common.database.MapFeatureMagicRealm;
import momime.common.database.MapSizePlane;
import momime.common.database.NodeStrengthPlane;
import momime.common.database.Pick;
import momime.common.database.RecordNotFoundException;
import momime.common.database.TileType;
import momime.common.database.TileTypeAreaEffect;
import momime.common.database.TileTypeEx;
import momime.common.database.TileTypeFeatureChance;
import momime.common.database.Unit;
import momime.common.database.UnitEx;
import momime.common.messages.MapAreaOfMemoryGridCells;
import momime.common.messages.MapRowOfMemoryGridCells;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitStatusID;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.database.ServerDatabaseValues;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.messages.MomGeneralServerKnowledge;

/**
 * Server only class which contains all the code for generating a random overland map
 *
 * There's no need to run this in a separate thread like in Delphi, because with the entire server XML file cached this version runs considerably faster
 */
public final class OverlandMapGeneratorImpl implements OverlandMapGenerator
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (OverlandMapGeneratorImpl.class);
	
	/** Where to write the generated map to */
	private MomGeneralServerKnowledge gsk;

	/** Session description containing the parameters used to generate the map */
	private MomSessionDescription sd;

	/** Server database cache */
	private CommonDatabase db;

	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;

	/** MemoryGridCell utils */
	private MemoryGridCellUtils memoryGridCellUtils;

	/** Random number generator */
	private RandomUtils randomUtils;
	
	/** Boolean operations for 2D maps */
	private BooleanMapAreaOperations2D booleanMapAreaOperations2D;

	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Blobs expand out only in north/south/east/west directions - no diagonals */
	private final static int [] BLOB_EXPANSION_DIRECTIONS = new int [] {1, 3, 5, 7};

	/**
	 * Describes the order that the smoothing tiles appear in TERRAIN.LBX where the smoothing takes all 8 adjacent tiles into consideration
	 * e.g. 00010000 is the smoothing tile to use for a water tile when direction 4 has land in it, therefore for this we would use Entry No. 0 in the animation texture list
	 * '0' means there is more sea in that direction, '1' means there is land
	 *
	 * Since this is only used for river generation now, it could be optimized by keeping only the entries which actually have valid river possibilities
	 * However then there'd be less protection against the smoothing rules generating an unrecognized bitmask
	 */
	private final static String [] TERRAIN_BORDER8_NEIGHBOURING_TILES = new String []
		{"00010000",
		"00011000",
		"00011100",
		"00001100",
		"00000100",
		"00010100",
		"01000100",
		"00000101",
		"00110000",
		"00001000",
		"00000110",
		"01000001",
		"00010001",
		"01010000",
		"01110000",
		"00100000",
		"11111111",
		"00000010",
		"00000111",
		"01100000",
		"10000000",
		"00000011",
		"01010001",
		"01010100",
		"00010101",
		"01000000",
		"11000000",
		"11000001",
		"10000001",
		"00000001",
		"01000101",
		"01010101",

		"10000011",	// 32-corners
		"11000011",
		"10000111",
		"11000111",
		"00111000",
		"01111000",
		"00111100",
		"01111100",
		"11100000",
		"11110000",
		"11100001",
		"11110001",
		"00001110",
		"00011110",
		"00001111",
		"00011111",
		"11011101",
		"11001101",
		"11011001",
		"11001001",
		"10011101",
		"10001101",
		"10011001",
		"10001001",
		"11011100",
		"11001100",
		"11011000",
		"11001000",
		"10011100",
		"10001100",
		"10011000",
		"10001000",
		"00100111",
		"00110111",
		"01100111",
		"01110111",
		"00100011",
		"00110011",
		"01100011",
		"01110011",
		"00100110",
		"00110110",
		"01100110",
		"01110110",
		"00100010",
		"00110010",
		"01100010",
		"01110010",
		"00111110",
		"10001111",
		"11100011",
		"11111000",
		"00111111",
		"11001111",
		"11110011",
		"11111100",
		"01111110",
		"10011111",
		"11100111",
		"11111001",
		"01111111",
		"11011111",
		"11110111",
		"11111101",
		"01110001",
		"01100001",
		"00110001",
		"00100001",
		"01110101",
		"01100101",
		"00110101",
		"00100101",
		"01110100",
		"01100100",
		"00110100",
		"00100100",
		"00011101",
		"01011101",
		"01011100",
		"00011001",
		"01011001",
		"01011000",
		"00001101",
		"01001101",
		"01001100",
		"00001001",
		"01001001",
		"01001000",
		"01000010",
		"01000011",
		"01000110",
		"01000111",
		"01010010",
		"01010011",
		"01010110",
		"01010111",
		"00010010",
		"00010011",
		"00010110",
		"00010111",
		"10000100",
		"10010100",
		"10010000",
		"10000101",
		"10010101",
		"10010001",
		"11000100",
		"11010100",
		"11010000",
		"11000101",
		"11010101",
		"11010001",
		"10010011",
		"11010011",
		"10010111",
		"11010111",
		"00111001",
		"01111001",
		"00111101",
		"01111101",
		"11100100",
		"11110100",
		"11100101",
		"11110101",
		"01001110",
		"01011110",
		"01001111",
		"01011111"};

	/**
	 * Stores, for each shore tile, up to 4 possible oceanside river mouth tiles (stored here as river directions) we can consider converting them into
	 * For example a corner shore tile (with shore in directions 3 & 5) might sprout a river in either direction 3 only, direction 5 only, or both
	 */
	private final static String [] [] RIVER_MOUTH_DIRECTIONS = new String [] []
		{{null, null, null, null},
		{"5", null, null, null},
		{"5", null, null, null},
		{"5", null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{"3", null, null, null},
		{"5", null, null, null},
		{"7", null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{"3", null, null, null},
		{"3", null, null, null},
		{"7", "1", "3", "5"},
		{"7", null, null, null},
		{"7", null, null, null},
		{"3", null, null, null},
		{"1", null, null, null},
		{"7", null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{"1", null, null, null},
		{"1", null, null, null},
		{"1", null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},

		{"7", "1", "17", null},
		{"7", "1", "17", null},
		{"7", "1", "17", null},
		{"7", "1", "17", null},
		{"5", "3", "35", null},
		{"5", "3", "35", null},
		{"5", "3", "35", null},
		{"5", "3", "35", null},
		{"1", "3", "13", null},
		{"1", "3", "13", null},
		{"1", "3", "13", null},
		{"1", "3", "13", null},
		{"5", "7", "57", null},
		{"5", "7", "57", null},
		{"5", "7", "57", null},
		{"5", "7", "57", null},

		{"1", "5", null, null},
		{"1", "5", null, null},
		{"1", "5", null, null},
		{"1", "5", null, null},
		{"1", "5", null, null},
		{"1", "5", null, null},
		{"1", "5", null, null},
		{"1", "5", null, null},
		{"1", "5", null, null},
		{"1", "5", null, null},
		{"1", "5", null, null},
		{"1", "5", null, null},
		{"1", "5", null, null},
		{"1", "5", null, null},
		{"1", "5", null, null},
		{"1", "5", null, null},

		{"7", "3", null, null},
		{"7", "3", null, null},
		{"7", "3", null, null},
		{"7", "3", null, null},
		{"7", "3", null, null},
		{"7", "3", null, null},
		{"7", "3", null, null},
		{"7", "3", null, null},
		{"7", "3", null, null},
		{"7", "3", null, null},
		{"7", "3", null, null},
		{"7", "3", null, null},
		{"7", "3", null, null},
		{"7", "3", null, null},
		{"7", "3", null, null},
		{"7", "3", null, null},

		{"5", "7", "3", null},
		{"7", "1", "5", null},
		{"1", "3", "7", null},
		{"3", "1", "5", null},
		{"5", "7", "3", null},
		{"7", "1", "5", null},
		{"1", "3", "7", null},
		{"3", "1", "5", null},
		{"5", "7", "3", null},
		{"7", "1", "5", null},
		{"1", "3", "7", null},
		{"3", "1", "5", null},
		{"5", "7", "3", null},
		{"7", "1", "5", null},
		{"1", "3", "7", null},
		{"3", "1", "5", null},

		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null},
		{null, null, null, null}};

	/**
	 * Describes the directions that each river tile has river branching off to
	 * The client decides the actual tile numbers, so this is now just a quick list of all the binary numbers 1..15
	 */
	private final static String [] RIVER_TILES = new String []
		{"0001",
		"0010",
		"0011",
		"0100",
		"0101",
		"0111",
		"0110",
		"1000",
		"1001",
		"1010",
		"1011",
		"1100",
		"1101",
		"1110",
		"1111"};

	/**
	 * Main routine to generate the overland terrain map
	 * @throws MomException If some fatal error happens during map generation
	 * @throws RecordNotFoundException If some entry isn't found in the db during map generation, or one of the smoothing borders isn't found in the fixed arrays
	 */
	@Override
	public final void generateOverlandTerrain () throws MomException, RecordNotFoundException
	{
		// Quick check that arrays are the same length
		if (TERRAIN_BORDER8_NEIGHBOURING_TILES.length != RIVER_MOUTH_DIRECTIONS.length)
			throw new MomException ("momime.server.MomMapGenerator: Tile number and river mouth arrays must be same length");
		
		// Start map generation, this initializes both planes at once
		setAllToWater ();

		final double landTileCountTimes100 = sd.getOverlandMapSize ().getWidth () * sd.getOverlandMapSize ().getHeight () * sd.getLandProportion ().getPercentageOfMapIsLand ();
		for (int plane = 0; plane < db.getPlane ().size (); plane++)
		{
			// Generate height-based scenery
			final HeightMapGenerator heightMap = new HeightMapGenerator (sd.getOverlandMapSize (), sd.getOverlandMapSize ().getZoneWidth (), sd.getOverlandMapSize ().getZoneHeight (),
				sd.getLandProportion ().getTundraRowCount ());
			heightMap.setRandomUtils (getRandomUtils ());
			heightMap.generateHeightMap ();

			setHighestTiles (heightMap, plane, ServerDatabaseValues.TILE_TYPE_GRASS,
				(int) Math.round (landTileCountTimes100 / 100d));

			setHighestTiles (heightMap, plane, ServerDatabaseValues.TILE_TYPE_HILLS,
				(int) Math.round (landTileCountTimes100 * sd.getLandProportion ().getPercentageOfLandIsHills () / 10000d));

			setHighestTiles (heightMap, plane, ServerDatabaseValues.TILE_TYPE_MOUNTAIN,
				(int) Math.round (landTileCountTimes100 * sd.getLandProportion ().getPercentageOfLandIsHills () * sd.getLandProportion ().getPercentageOfHillsAreMountains () / 1000000d));

			// Special rules for Tundra
			makeTundra ();

			// Blob-based scenery
			for (final LandProportionTileType blobTileType : sd.getLandProportion ().getLandProportionTileType ())
				placeBlobs (ServerDatabaseValues.TILE_TYPE_GRASS, blobTileType.getTileTypeID (),
					(int) Math.round (landTileCountTimes100 * blobTileType.getPercentageOfLand () / 10000d), blobTileType.getEachAreaTileCount (), plane);
		}

		// Special rules for Towes of Wizardy
		placeTowersOfWizardry ();

		for (int plane = 0; plane < db.getPlane ().size (); plane++)
		{
			// Generate shore tiles and extend them into rivers
			final int [] [] shoreTileNumbers = determineShoreTileNumbers (ServerDatabaseValues.TILE_TYPE_OCEAN, ServerDatabaseValues.TILE_TYPE_SHORE, plane);

			makeRivers (shoreTileNumbers, plane);
		}

		// Place remaining items
		placeTerrainFeatures ();
		placeNodes ();
		placeLairs (sd.getOverlandMapSize ().getNormalLairCount (), false);
		placeLairs (sd.getOverlandMapSize ().getWeakLairCount (), true);
	}

	/**
	 * Creates all the map cells and sets them all to water
	 */
	final void setAllToWater ()
	{
		// Delete anything there currently
		gsk.getTrueMap ().setMap (new MapVolumeOfMemoryGridCells ());

		// Create all the map cells
		for (int plane = 0; plane < sd.getOverlandMapSize ().getDepth (); plane++)
		{
			final MapAreaOfMemoryGridCells area = new MapAreaOfMemoryGridCells ();
			for (int y = 0; y < sd.getOverlandMapSize ().getHeight (); y++)
			{
				final MapRowOfMemoryGridCells row = new MapRowOfMemoryGridCells ();
				for (int x = 0; x < sd.getOverlandMapSize ().getWidth (); x++)
				{
					final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
					terrainData.setTileTypeID (ServerDatabaseValues.TILE_TYPE_OCEAN);

					final ServerGridCellEx cell = new ServerGridCellEx ();
					cell.setTerrainData (terrainData);
					row.getCell ().add (cell);
				}

				area.getRow ().add (row);
			}

			gsk.getTrueMap ().getMap ().getPlane ().add (area);
		}
	}

	/**
	 * Finds the height in the heightmap for which as close as possible to DesiredTileCount tiles are above that height and the remainder are below it.
	 * Then sets all tiles above this height to have scenery TileTypeID.
	 * 
	 * This doesn't really need a unit test, because it just delegates to HeightMapGenerator.setHighestTiles, which has a decent test.
	 *
	 * @param heightMap Height map generated for this plane
	 * @param plane Plane number to output tiles to
	 * @param tileTypeID The tile type to set the highest tiles to
	 * @param desiredTileCount How many tiles to set to this tile type
	 */
	private final void setHighestTiles (final HeightMapGenerator heightMap, final int plane, final String tileTypeID, final int desiredTileCount)
	{
		heightMap.setHighestTiles (desiredTileCount, (x, y) ->
			gsk.getTrueMap ().getMap ().getPlane ().get (plane).getRow ().get (y).getCell ().get (x).getTerrainData ().setTileTypeID (tileTypeID));
	}

	/**
	 * Forces the top row to be tundra, and the top 7 rows have a chance of converting grassland to tundra
	 */
	final void makeTundra ()
	{
		// If wraps both ways, there'll be no tundra at all
		if ((!sd.getOverlandMapSize ().isWrapsLeftToRight ()) || (!sd.getOverlandMapSize ().isWrapsTopToBottom ()))
		{
			for (int plane = 0; plane < sd.getOverlandMapSize ().getDepth (); plane++)
				for (int x = 0; x < sd.getOverlandMapSize ().getWidth (); x++)
					for (int y = 0; y < sd.getOverlandMapSize ().getHeight (); y++)
					{
						final OverlandMapTerrainData terrainData = gsk.getTrueMap ().getMap ().getPlane ().get (plane).getRow ().get (y).getCell ().get (x).getTerrainData ();

						// What's the nearest distance to a non-wrapping edge from this square
						int d = Integer.MAX_VALUE;
						if (!sd.getOverlandMapSize ().isWrapsLeftToRight ())
							d = Math.min (Math.min (d, x), sd.getOverlandMapSize ().getWidth () - 1 - x);

						if (!sd.getOverlandMapSize ().isWrapsTopToBottom ())
							d = Math.min (Math.min (d, y), sd.getOverlandMapSize ().getHeight () - 1 - y);

						if (d == 0)
							terrainData.setTileTypeID (ServerDatabaseValues.TILE_TYPE_TUNDRA);
						else if ((d < sd.getLandProportion ().getTundraRowCount ()) && (terrainData.getTileTypeID ().equals (ServerDatabaseValues.TILE_TYPE_GRASS)))
						{
							log.debug ("makeTundra: Grass at " + x + ", " + y + ", " + plane + " is " + d + " away from the nearest non-wrapping edge, so has a " + d + "/" +
								sd.getLandProportion ().getTundraRowCount () + " chance of being staying as grass");
							
							if (getRandomUtils ().nextInt (sd.getLandProportion ().getTundraRowCount ()) >= d)
								terrainData.setTileTypeID (ServerDatabaseValues.TILE_TYPE_TUNDRA);
						}
					}
		}
	}

	/**
	 * Routine for placing a single area of forest, desert and swamp
	 * @param changeFromTileTypeID Tile type to change from
	 * @param changeToTileTypeID Tile type to change to
	 * @param eachAreaTileCount Typical size of blob we want to place
	 * @param plane Plane to place the blob on
	 * @return Size of blob created
	 */
	private final int placeSingleBlob (final String changeFromTileTypeID, final String changeToTileTypeID, final int eachAreaTileCount, final int plane)
	{
		// Make a list of all the possible start locations
		final List<MapCoordinates2DEx> startingPositions = new ArrayList<MapCoordinates2DEx> ();
		for (int x = 0; x < sd.getOverlandMapSize ().getWidth (); x++)
			for (int y = 0; y < sd.getOverlandMapSize ().getHeight (); y++)
				if (gsk.getTrueMap ().getMap ().getPlane ().get (plane).getRow ().get (y).getCell ().get (x).getTerrainData ().getTileTypeID ().equals (changeFromTileTypeID))
					startingPositions.add (new MapCoordinates2DEx (x, y));

		// Pick a random start location
		int tilesPlaced = 0;
		if (startingPositions.size () > 0)
		{
			// Found a centre tile
			MapCoordinates2DEx coords = startingPositions.get (getRandomUtils ().nextInt (startingPositions.size ()));

			// Work out how big we'll aim to make this blob, this will be +/- 50% of the average size
			int blobSize = getRandomUtils ().nextInt (eachAreaTileCount) + (eachAreaTileCount / 2);

			// Create an area to keep track of the tiles in this blob
			final MapArea2D<Boolean> thisBlob = new MapArea2DArrayListImpl<Boolean> ();
			thisBlob.setCoordinateSystem (sd.getOverlandMapSize ());
			do
			{
				// Set this tile
				gsk.getTrueMap ().getMap ().getPlane ().get (plane).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ().setTileTypeID (changeToTileTypeID);
				thisBlob.set (coords, true);
				tilesPlaced++;
				blobSize--;

				// Look for a neighbouring tile
				if (blobSize > 0)
				{
					// Copy the current blob
					final MapArea2D<Boolean> possibleNextCells = new MapArea2DArrayListImpl<Boolean> (); 
					possibleNextCells.setCoordinateSystem (sd.getOverlandMapSize ());
					
					for (int copyX = 0; copyX < sd.getOverlandMapSize ().getWidth (); copyX++)
						for (int copyY = 0; copyY < sd.getOverlandMapSize ().getHeight (); copyY++)
							possibleNextCells.set (copyX, copyY, thisBlob.get (copyX, copyY));
					
					// Create a ring around the current blob, i.e. this will tell us all the possible cells we could expand the blob into
					getBooleanMapAreaOperations2D ().enlarge (possibleNextCells, BLOB_EXPANSION_DIRECTIONS, false);

					// Deselect any that are not grass
					for (int grassX = 0; grassX < sd.getOverlandMapSize ().getWidth (); grassX++)
						for (int grassY = 0; grassY < sd.getOverlandMapSize ().getHeight (); grassY++)
							if (!gsk.getTrueMap ().getMap ().getPlane ().get (plane).getRow ().get (grassY).getCell ().get (grassX).getTerrainData ().getTileTypeID ().equals (changeFromTileTypeID))
								possibleNextCells.set (grassX, grassY, false);

					// Pick a cell to expand into
					final MapCoordinates2DEx nextCoords = getBooleanMapAreaOperations2D ().findRandomCellEqualTo (possibleNextCells, true);
					if (nextCoords == null)
						blobSize = 0;		// No remaining adjacent grass tiles, so set to zero to force loop to exit
					else
						coords = nextCoords;
				}
			} while (blobSize > 0);
		}

		return tilesPlaced;
	}

	/**
	 * Routine for replacing some of the grass with areas of forest, desert and swamp
	 * @param changeFromTileTypeID Tile type to change from
	 * @param changeToTileTypeID Tile type to change to
	 * @param desiredTileCount Number of tiles to convert
	 * @param eachAreaTileCount Try to split blobs into areas of approximately this size
	 * @param plane Plane to place blobs on
	 */
	private final void placeBlobs (final String changeFromTileTypeID, final String changeToTileTypeID, final int desiredTileCount, final int eachAreaTileCount, final int plane)
	{
		int totalTilesPlaced = 0;
		while (totalTilesPlaced < desiredTileCount)
		{
			final int thisTilesPlaced = placeSingleBlob (changeFromTileTypeID, changeToTileTypeID, eachAreaTileCount, plane);

			// If we didn't place a single tile then there can't be any grass left, in which case force the loop to exit
			if (thisTilesPlaced == 0)
				totalTilesPlaced = desiredTileCount;
			else
				totalTilesPlaced = totalTilesPlaced + thisTilesPlaced;
		}
	}

	/**
	 * Places towers of wizardry onto both planes
	 * @throws MomException If we can't find a suitable location to place all Towers of Wizardry even after reducing desired separation
	 */
	final void placeTowersOfWizardry () throws MomException
	{
		// Place each tower in turn
		int towersOfWizardrySeparation = sd.getOverlandMapSize ().getTowersOfWizardrySeparation ();
		for (int towerNo = 0; towerNo < sd.getOverlandMapSize ().getTowersOfWizardryCount (); towerNo++)
		{
			boolean placedOk = false;
			while (!placedOk)
			{
				// Build initial list of suitable locations - we do this on each loop because we may reduce the separation
				// Avoid map cells which are tundra on either plane - this avoids putting towers too close to the top or bottom
				final MapArea2D<Boolean> possibleLocations = new MapArea2DArrayListImpl<Boolean> ();
				possibleLocations.setCoordinateSystem (sd.getOverlandMapSize ());
				
				for (int x = 0; x < sd.getOverlandMapSize ().getWidth (); x++)
					for (int y = 0; y < sd.getOverlandMapSize ().getHeight (); y++)
					{
						boolean possibleLocation = true;
						for (int plane = 0; plane < sd.getOverlandMapSize ().getDepth (); plane++)
							if (gsk.getTrueMap ().getMap ().getPlane ().get (plane).getRow ().get (y).getCell ().get (x).getTerrainData ().getTileTypeID ().equals (ServerDatabaseValues.TILE_TYPE_TUNDRA))
								possibleLocation = false;

						if (possibleLocation)
							possibleLocations.set (x, y, Boolean.TRUE);
					}

				// Deselect any tiles close to towers already placed
				for (int x = 0; x < sd.getOverlandMapSize ().getWidth (); x++)
					for (int y = 0; y < sd.getOverlandMapSize ().getHeight (); y++)
						if (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (gsk.getTrueMap ().getMap ().getPlane ().get (0).getRow ().get (y).getCell ().get (x).getTerrainData ()))
							getBooleanMapAreaOperations2D ().deselectRadius (possibleLocations, x, y, towersOfWizardrySeparation);

				// Pick a location to put the wizardry tile at
				final MapCoordinates2D coords = getBooleanMapAreaOperations2D ().findRandomCellEqualTo (possibleLocations, true);
				if (coords == null)
				{
					// Nowhere left within the radius of existing towers - so drop the radius
					if (towersOfWizardrySeparation == 0)
						throw new MomException ("Couldn''t find enough locations to place Towers of Wizardry even after reducing desired separation");

					towersOfWizardrySeparation--;
					log.warn ("Reducing towersOfWizardrySeparation from desired value of " + sd.getOverlandMapSize ().getTowersOfWizardrySeparation () + " down to " +
						towersOfWizardrySeparation + " otherwise cannot fit " + sd.getOverlandMapSize ().getTowersOfWizardryCount () + " towers on the map");
				}
				else
				{
					// Place tower on all planes
					placedOk = true;
					for (int plane = 0; plane < sd.getOverlandMapSize ().getDepth (); plane++)
					{
						final OverlandMapTerrainData terrainData = gsk.getTrueMap ().getMap ().getPlane ().get (plane).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
						terrainData.setMapFeatureID (CommonDatabaseConstants.FEATURE_UNCLEARED_TOWER_OF_WIZARDRY);
						terrainData.setTileTypeID (ServerDatabaseValues.TILE_TYPE_GRASS);
					}
					
					// Set value of monsters + treasure in this tower, but only on Arcanus
					final ServerGridCellEx thisCell = (ServerGridCellEx) gsk.getTrueMap ().getMap ().getPlane ().get (0).getRow ().get (coords.getY ()).getCell ().get (coords.getX ());
					thisCell.setNodeLairTowerPowerProportion (getRandomUtils ().nextInt (10001) / 10000d);
				}
			}
		}
	}

	/**
	 * @param neighbouringTiles Bitmask of whether adjoining tiles contain more ocean (0) or land (1)
	 * @return Tile number corresponding to this bitmask
	 * @throws RecordNotFoundException If no tile with this bitmask is defined
	 */
	final int findTerrainBorder8 (final String neighbouringTiles)
		throws RecordNotFoundException
	{
		// List all possible matches rather than just returning the first one - that way we can list the same bitmask multiple times and choose one at random
		// I don't think this is strictly necessary anymore now that smoothing is only used for shoreline - all values in TERRAIN_BORDER8_NEIGHBOURING_TILES are unique??
		final List<Integer> matches = new ArrayList<Integer> ();
		for (int tileNumber = 0; tileNumber < TERRAIN_BORDER8_NEIGHBOURING_TILES.length; tileNumber++)
			if (TERRAIN_BORDER8_NEIGHBOURING_TILES [tileNumber].equals (neighbouringTiles))
				matches.add (Integer.valueOf (tileNumber));

		if (matches.size () == 0)
			throw new RecordNotFoundException ("TERRAIN_BORDER8_NEIGHBOURING_TILES", neighbouringTiles, "findTerrainBorder8");

		return matches.get (getRandomUtils ().nextInt (matches.size ())).intValue ();
	}

	/**
	 * Converts water tiles next to land into shoreline and picks their logical tile number
	 * This used to be the server-side smoothing algorithm - but now all smoothing is done client-side
	 * However we still need to use this for river generation - basically we are doing this to ensure that we don't generate a river mouth for which there's no graphic to display it on the client
	 * We also need to do this server-side because Shore has different characteristics from Ocean - Shore produces ½ food and 10% gold bonus whereas Ocean doesn't
	 *
	 * @param tileTypeIdToSmooth Tile type to change from (ocean)
	 * @param tileTypeIdToSetTo Tile type to change to (shore)
	 * @param plane Plane to operate on
	 * @return Array of tile numbers to allocate to each shore tile
	 * @throws RecordNotFoundException If we generate a bitmask that isn't listed in TERRAIN_BORDER8_NEIGHBOURING_TILES
	 */
	private final int [] [] determineShoreTileNumbers (final String tileTypeIdToSmooth, final String tileTypeIdToSetTo, final int plane)
		throws RecordNotFoundException
	{
		final int [] [] shoreTileNumbers = new int [sd.getOverlandMapSize ().getHeight ()] [sd.getOverlandMapSize ().getWidth ()];

		for (int x = 0; x < sd.getOverlandMapSize ().getWidth (); x++)
			for (int y = 0; y < sd.getOverlandMapSize ().getHeight (); y++)
			{
				final OverlandMapTerrainData terrainData = gsk.getTrueMap ().getMap ().getPlane ().get (plane).getRow ().get (y).getCell ().get (x).getTerrainData ();
				if (terrainData.getTileTypeID ().equals (tileTypeIdToSmooth))
				{
					// Found an ocean tile - check neighbouring tiles
					final boolean [] directions = new boolean [getCoordinateSystemUtils ().getMaxDirection (sd.getOverlandMapSize ().getCoordinateSystemType ())];
					for (int d = 0; d < directions.length; d++)
					{
						final MapCoordinates2DEx coords = new MapCoordinates2DEx (x, y);
						if (getCoordinateSystemUtils ().move2DCoordinates (sd.getOverlandMapSize (), coords, d + 1))
						{
							final String thisTileTypeID = gsk.getTrueMap ().getMap ().getPlane ().get (plane).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ().getTileTypeID ();

							// Careful, adjacent sea tiles might have already been converted to shoreline
							directions [d] = ((!thisTileTypeID.equals (tileTypeIdToSmooth)) && (!thisTileTypeID.equals (tileTypeIdToSetTo)));
						}
						else
						{
							// Off edge of map - assume its more sea/tundra/etc
							directions [d] = false;
						}
					}

					// If two flat edges are included, then the diagonal is assumed to be included (there are 95 tiles of this type out of 255 which aren't
					// included in the 160 size tileset, 160 + 95 = 255 so this then includes everything)
					// Basically this is a hard coded copy of the "SS161" smoothing system in the graphics XML file
					if ((directions [0]) && (directions [2])) directions [1] = true;
					if ((directions [2]) && (directions [4])) directions [3] = true;
					if ((directions [4]) && (directions [6])) directions [5] = true;
					if ((directions [6]) && (directions [0])) directions [7] = true;

					// Build into string
					String neighbouringTiles = "";
					boolean anyNeighbouringLand = false;
					for (final boolean includeDirection : directions)
						if (includeDirection)
						{
							neighbouringTiles = neighbouringTiles + "1";
							anyNeighbouringLand = true;
						}
						else
							neighbouringTiles = neighbouringTiles + "0";

					// Leave fully ocean files as ocean
					// Convert any ocean tile with at least one piece of neighbouring land to a shore tile
					if (anyNeighbouringLand)
					{
						terrainData.setTileTypeID (tileTypeIdToSetTo);
						shoreTileNumbers [y] [x] = findTerrainBorder8 (neighbouringTiles);
					}
				}
			}

		return shoreTileNumbers;
	}

	/**
	 * @param riverBitmask River bitmask string, e.g. 0101
	 * @param exceptFor Direction to exclude from the result, or -1 to include them all
	 * @return Direction list, e.g. 37
	 */
	final String convertNeighbouringTilesToDirections (final String riverBitmask, final int exceptFor)
	{
		String result = "";
		for (int directionChk = 0; directionChk < 4; directionChk++)
		{
			// Convert direction to a regular 1,3,5,7 value
			final int d = (directionChk * 2) + 1;

			if ((d != exceptFor) && (riverBitmask.substring (directionChk, directionChk + 1).equals ("1")))
				result = result + d;
		}

		return result;
	}

	/**
	 *
	 * @param x X coordinate to check from
	 * @param y Y coordinate to check from
	 * @param plane Plane to check
	 * @param directions List of directions to check - can be more than one e.g. "13" to check both directions 1 and 3
	 * @param riverPending Array of locations where river placement is pending (so we don't get stuck in infinite loops re-listing cells that we already know need attention)
	 * @return True if all directions listed, from x, y, lead to Grass
	 */
	final boolean checkAllDirectionsLeadToGrass (final int x, final int y, final int plane, final String directions, final boolean [] [] riverPending)
	{
		boolean result = true;
		int directionNo = 0;

		while ((result) && (directionNo < directions.length ()))
		{
			final int d = Integer.parseInt (directions.substring (directionNo, directionNo + 1));

			final MapCoordinates2DEx coords = new MapCoordinates2DEx (x, y);
			if (getCoordinateSystemUtils ().move2DCoordinates (sd.getOverlandMapSize (), coords, d))
			{
				final OverlandMapTerrainData terrainData = gsk.getTrueMap ().getMap ().getPlane ().get (plane).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();

				final boolean riverPendingValue = riverPending [coords.getY ()] [coords.getX ()];

				if ((!terrainData.getTileTypeID ().equals (ServerDatabaseValues.TILE_TYPE_GRASS)) ||
					(riverPendingValue) || (terrainData.getMapFeatureID () != null))

					result = false;
			}
			else
			{
				// Off edge of map - invalid
				result = false;
			}

			directionNo++;
		}

		return result;
	}

	/**
	 * @param substring String to search for
	 * @param stringToSearch String to search in
	 * @return The number of times substring appears in stringToSearch
	 */
	final int countStringRepetitions (final String substring, final String stringToSearch)
	{
		final int substringLength = substring.length ();
		int count = 0;

		for (int startPosition = 0; startPosition <= stringToSearch.length () - substringLength; startPosition++)
		{
			if (stringToSearch.substring (startPosition, startPosition + substringLength).equals (substring))
				count++;
		}

		return count;
	}

	/**
	 * Continues a river on from x, y, plane in the specified direction(s)
	 * @param x X coordinate of current river tile
	 * @param y Y coordinate of current river tile
	 * @param plane Plane the river is on
	 * @param directions Directions that the river proceeds from this tile
	 * @param isRiverMouth If this tile is the river mouth (start of a new river)
	 * @param riverPending Array of locations where river placement is pending (so we don't get stuck in infinite loops re-listing cells that we already know need attention)
	 * @throws MomException If we find a river flowing off the map edge
	 */
	private final void processRiverDirections (final int x, final int y, final int plane, final String directions, final boolean isRiverMouth,
		final boolean [] [] riverPending)
		throws MomException
	{
		// Before we process the entire river branch under each direction,
		// set each direction to non-grass so that we can't accidentally loop round onto it
		for (int directionNo = 0; directionNo < directions.length (); directionNo++)
		{
			final int d = Integer.parseInt (directions.substring (directionNo, directionNo + 1));

			// Work out where the next tile is
			final MapCoordinates2DEx coords = new MapCoordinates2DEx (x, y);
			if (!getCoordinateSystemUtils ().move2DCoordinates (sd.getOverlandMapSize (), coords, d))
				throw new MomException ("ProcessRiverDirections: Error in river algorithm - found a river flowing off the edge of the map");

			// Mark it as pending
			riverPending [coords.getY ()] [coords.getX ()] = true;
		}

		// Process each direction
		for (int directionNo = 0; directionNo < directions.length (); directionNo++)
		{
			final int d = Integer.parseInt (directions.substring (directionNo, directionNo + 1));

			// Work out where the next tile is, and what direction we got here from
			final MapCoordinates2DEx coords = new MapCoordinates2DEx (x, y);
			if (!getCoordinateSystemUtils ().move2DCoordinates (sd.getOverlandMapSize (), coords, d))
				throw new MomException ("ProcessRiverDirections: Error in river algorithm - found a river flowing off the edge of the map");

			final int d2 = getCoordinateSystemUtils ().normalizeDirection (sd.getOverlandMapSize ().getCoordinateSystemType (),
				d + (getCoordinateSystemUtils ().getMaxDirection (sd.getOverlandMapSize ().getCoordinateSystemType ()) / 2));

			// Verify that this tile hasn't been altered since we checked that it was grass
			if (!riverPending [coords.getY ()] [coords.getX ()])
				throw new MomException ("ProcessRiverTile: Tile is no longer \''River Pending\"");

			// Find how many directions we can include at most
			// d2 is the direction we came FROM, so we HAVE to show a river flowing back in that direction
			int directionCount = 0;
			for (int directionChk = 0; directionChk < 4; directionChk++)
			{
				// Convert direction to a regular 1,3,5,7 value
				final int d3 = (directionChk * 2) + 1;
				if ((d3 == d2) || (checkAllDirectionsLeadToGrass (coords.getX (), coords.getY (), plane, Integer.valueOf (d3).toString (), riverPending)))
					directionCount++;
			}

			// Out of the possible maximum number of directions, randomly pick how many directions we will actually send the river
			// 1 = Just send it back in the direction it came from, so end the river
			directionCount = getRandomUtils ().nextInt (directionCount) + 1;

			// List out all the possible directions codes which have this number of directions and include direction d2
			final List<String> possibleBitmasks = new ArrayList<String> ();
			for (final String riverBitmask : RIVER_TILES)
			{
				final int stringPosition = (d2 - 1) / 2;

				if ((riverBitmask.substring (stringPosition, stringPosition + 1).equals ("1")) &&			// d2 is included
					(countStringRepetitions ("1", riverBitmask) == directionCount) &&		// desired number of branches
					(checkAllDirectionsLeadToGrass (coords.getX (), coords.getY (), plane, convertNeighbouringTilesToDirections (riverBitmask, d2), riverPending)))	// leads to grass

					possibleBitmasks.add (riverBitmask);
			}

			// We must have found at least one
			if (possibleBitmasks.size () == 0)
				throw new MomException ("ProcessRiverTile: No river tiles are suitable - was looking for a bitmask with " + directionCount + " directions, including direction " + d2);

			// Pick a random neighbouring tiles string
			final String riverBitmask = possibleBitmasks.get (getRandomUtils ().nextInt (possibleBitmasks.size ()));

			// Update this tile
			riverPending [coords.getY ()] [coords.getX ()] = false;

			final OverlandMapTerrainData terrainData = gsk.getTrueMap ().getMap ().getPlane ().get (plane).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
			if (isRiverMouth)
				terrainData.setTileTypeID (ServerDatabaseValues.TILE_TYPE_LANDSIDE_RIVER_MOUTH);
			else
				terrainData.setTileTypeID (ServerDatabaseValues.TILE_TYPE_RIVER);

			// Set the directions the river at this tile heads in

			// NB. This is used to draw the graphics for the river tile on the client and so needs to include all directions, and so the call to
			// ConvertNeighbouringTilesToDirections needs to INCLUDE the direction we came FROM...
			terrainData.setRiverDirections (convertNeighbouringTilesToDirections (riverBitmask, -1));

			// Process further tiles
			// ...whereas this call needs to know what direction to continue the river in, and we don't need to continue it in the direction
			// its already coming from, so this EXCLUDES the direction we came from
			processRiverDirections (coords.getX (), coords.getY (), plane, convertNeighbouringTilesToDirections (riverBitmask, d2), false, riverPending);
		}
	}

	/**
	 * Finds pieces of shoreline, starts a river at the opening into the sea, and follows it up in random directions
	 *
	 * Ideally (to get most realistic results) we should find points up on mountains to start rivers at and make the river flow down the heightmap
	 * However we have the added complication that we have to make sure we only generate river tiles for which there is a graphic to display it on the client
	 * (Especially since there's no graphics for rivers in mountains, so the whole concept of starting a river up in a mountain wouldn't work anyway)
	 *
	 * So instead this works totally opposite - it finds shore tiles, turns them into river mouths, and the progresses the river inland in random directions
	 *
	 * @param shoreTileNumbers Array of tile numbers allocated to each shore tile
	 * @param plane Plane to add rivers to
	 * @throws MomException If a tile which we proved to be a valid possibility for a river mouth later is found that we can't convert it to a river mouth
	 */
	private final void makeRivers (final int [] [] shoreTileNumbers, final int plane)
		throws MomException
	{
		final boolean [] [] riverPending = new boolean [sd.getOverlandMapSize ().getHeight ()] [sd.getOverlandMapSize ().getWidth ()];

		int riverNo = 0;
		while (riverNo < sd.getOverlandMapSize ().getRiverCount ())
		{
			// Get a list of all shoreline tiles for which there is a river tile where all the directions
			// lead to grassland, i.e. all valid starting locations for a river
			final List<MapCoordinates2D> startingPositions = new ArrayList<MapCoordinates2D> ();
			for (int x = 0; x < sd.getOverlandMapSize ().getWidth (); x++)
				for (int y = 0; y < sd.getOverlandMapSize ().getHeight (); y++)
					if (gsk.getTrueMap ().getMap ().getPlane ().get (plane).getRow ().get (y).getCell ().get (x).getTerrainData ().getTileTypeID ().equals (ServerDatabaseValues.TILE_TYPE_SHORE))
					{
						// Found a shore tile, now check
						// a) that the shore tile has possibilities for a river leading from it (e.g. a shore tile with only shore in the corner(s) can never have a river leading from it)
						// b) the directions the river(s) lead in lead to grass
						final String [] riverOptions = RIVER_MOUTH_DIRECTIONS [shoreTileNumbers [y] [x]];
						boolean foundPossibleRiver = false;
						int index = 0;
						while ((!foundPossibleRiver) && (index < riverOptions.length))
						{
							final String riverDirections = riverOptions [index];
							if ((riverDirections != null) && (checkAllDirectionsLeadToGrass (x, y, plane, riverDirections, riverPending)))
								foundPossibleRiver = true;

							index++;
						}

						if (foundPossibleRiver)
						{
							final MapCoordinates2D coords = new MapCoordinates2D ();
							coords.setX (x);
							coords.setY (y);

							startingPositions.add (coords);
						}
					}

			// Pick a location for the river mouth
			if (startingPositions.size () == 0)
			{
				// No more suitable start locations, so give up trying - force loop to exit
				log.warn ("Couldn't place desired number of rivers on plane " + plane + " - wanted " + sd.getOverlandMapSize ().getRiverCount () + " rivers but only managed to find space for " + riverNo);
				riverNo = sd.getOverlandMapSize ().getRiverCount ();
			}
			else
			{
				// Pick a random start location
				final MapCoordinates2D coords = startingPositions.get (getRandomUtils ().nextInt (startingPositions.size ()));

				// Convert river mouth tile
				final OverlandMapTerrainData riverMouth = gsk.getTrueMap ().getMap ().getPlane ().get (plane).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
				riverMouth.setTileTypeID (ServerDatabaseValues.TILE_TYPE_OCEANSIDE_RIVER_MOUTH);

				// Pick the directions to extend this river in
				final String [] riverOptions = RIVER_MOUTH_DIRECTIONS [shoreTileNumbers [coords.getY ()] [coords.getX ()]];
				final List<String> validRiverOptions = new ArrayList<String> ();

				for (int index = 0; index < riverOptions.length; index++)
				{
					final String riverDirections = riverOptions [index];
					if ((riverDirections != null) && (checkAllDirectionsLeadToGrass (coords.getX (), coords.getY (), plane, riverDirections, riverPending)))
						validRiverOptions.add (riverDirections);
				}

				if (validRiverOptions.size () == 0)
					throw new MomException ("momime.server.MomMapGenerator.makeRivers: Tile which we proved to be a valid possibility for a river mouth then couldn't be converted to a river mouth");

				riverMouth.setRiverDirections (validRiverOptions.get (getRandomUtils ().nextInt (validRiverOptions.size ())));

				// Now recursively generate the river
				processRiverDirections (coords.getX (), coords.getY (), plane, riverMouth.getRiverDirections (), true, riverPending);

				// Go to next river
				riverNo++;
			}
		}
	}

	/**
	 * Places terrain features onto the map, such as gems or gold
	 * @throws MomException If the right planes aren't listed in the session description
	 * @throws RecordNotFoundException If we find a tile type that can't be found in the cache
	 */
	final void placeTerrainFeatures () throws MomException, RecordNotFoundException
	{
		// Validate the right planes are listed in the session description
		if (sd.getLandProportion ().getLandProportionPlane ().size () != sd.getOverlandMapSize ().getDepth ())
			throw new MomException ("placeTerrainFeatures: Incorrect number of Land Proportion Planes listed in session description");

		final List<Integer> planeNumbersFound = new ArrayList<Integer> ();
		for (final LandProportionPlane plane : sd.getLandProportion ().getLandProportionPlane ())
		{
			// Check it isn't already in the list
			for (final Integer planeCheck : planeNumbersFound)
				if (planeCheck == plane.getPlaneNumber ())
					throw new MomException ("placeTerrainFeatures: Plane " + planeCheck + " is repeated twice in session description Land Proportion Planes");

			planeNumbersFound.add (plane.getPlaneNumber ());
		}

		// Now we know the session description is valid, can process each plane in turn
		for (final LandProportionPlane plane : sd.getLandProportion ().getLandProportionPlane ())
			for (int x = 0; x < sd.getOverlandMapSize ().getWidth (); x++)
				for (int y = 0; y < sd.getOverlandMapSize ().getHeight (); y++)
				{
					final OverlandMapTerrainData terrainData = gsk.getTrueMap ().getMap ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x).getTerrainData ();

					// The cache lists the chances of getting each type of feature on this tile type.
					// Tiles that can never get any features, like ocean, have nothing listed.
					final List<TileTypeFeatureChance> featureChances = db.findTileType (terrainData.getTileTypeID (), "placeTerrainFeatures").getTileTypeFeatureChance ();
					if (featureChances.size () > 0)
					{					
						// Make sure we check there are no features already placed, since we will have towers of wizardry at this stage
						if ((terrainData.getMapFeatureID () == null) && (getRandomUtils ().nextInt (plane.getFeatureChance ()) == 0))
						{
							// Total up all the chances listed for this type of tile
							int totalChance = 0;
							for (final TileTypeFeatureChance thisFeature : featureChances)
								if (thisFeature.getPlaneNumber () == plane.getPlaneNumber ())
									totalChance = totalChance + thisFeature.getFeatureChance ();
	
							if (totalChance > 0)
							{
								// Pick a random terrain feature
								totalChance = getRandomUtils ().nextInt (totalChance);
	
								// Get the feature ID of the feature we picked
								int featureNo = 0;
								String feature = null;
								while ((feature == null) && (featureNo < featureChances.size ()))
								{
									final TileTypeFeatureChance thisFeature = featureChances.get (featureNo);
									if (thisFeature.getPlaneNumber () == plane.getPlaneNumber ())
									{
										if (totalChance < thisFeature.getFeatureChance ())
											feature = thisFeature.getMapFeatureID ();
										else
											totalChance = totalChance - thisFeature.getFeatureChance ();
									}
	
									featureNo++;
								}
	
								// Update tile
								terrainData.setMapFeatureID (feature);
							}
						}
					}
				}
	}

	/**
	 * First used to test whether the node rings can fit (if setAuraFromNode is set to null) and then used to actually set the rings
	 * @param nodeAuraSize The number of squares the node aura will cover
	 * @param x The X location of the actual node
	 * @param y The Y location of the actual node
	 * @param plane The plane the node is on
	 * @param setAuraFromNode If set, will update the auraFromNode on each square of node aura; if null, only performs a test whether the node aura fits
	 * @return True if could fit node rings at this location, false if not
	 */
	final boolean placeNodeRings (final int nodeAuraSize, final int x, final int y, final int plane, final MapCoordinates3DEx setAuraFromNode)
	{
		// Radius zero is the centre square only
		final MapCoordinates2DEx coords = new MapCoordinates2DEx (x, y);

		int nodeAuraLeftToPlace = nodeAuraSize;
		int r = 0;
		boolean fits = true;

		while ((fits) && (nodeAuraLeftToPlace > 0))
		{
			// How many squares are there at this radius
			final int squaresAtThisRadius;
			if (r == 0)
				squaresAtThisRadius = 1;
			else
				squaresAtThisRadius = r * 8;

			// How many of the squares do we need to be available?
			int squaresRequired = Math.min (nodeAuraLeftToPlace, squaresAtThisRadius);

			// Make a list of how many are actually available
			final List<MapCoordinates2D> possibleLocations = new ArrayList<MapCoordinates2D> ();
			if (r == 0)
			{
				if (getCoordinateSystemUtils ().are2DCoordinatesWithinRange (sd.getOverlandMapSize (), coords))
				{
					final ServerGridCellEx thisCell = (ServerGridCellEx) gsk.getTrueMap ().getMap ().getPlane ().get (plane).getRow ().get (coords.getY ()).getCell ().get (coords.getX ());
					if (thisCell.getAuraFromNode () == null)
					{
						final MapCoordinates2D possibleCoords = new MapCoordinates2D ();
						possibleCoords.setX (coords.getX ());
						possibleCoords.setY (coords.getY ());

						possibleLocations.add (possibleCoords);
					}
				}
			}
			else
			{
				// Move down-left
				getCoordinateSystemUtils ().move2DCoordinates (sd.getOverlandMapSize (), coords, 6);

				for (int directionChk = 0; directionChk < 4; directionChk++)
				{
					final int d = (directionChk * 2) + 1;
					for (int l = 0; l < r * 2; l++)
					{
						// Move in direction d
						if (getCoordinateSystemUtils ().move2DCoordinates (sd.getOverlandMapSize (), coords, d))
						{
							final ServerGridCellEx thisCell = (ServerGridCellEx) gsk.getTrueMap ().getMap ().getPlane ().get (plane).getRow ().get (coords.getY ()).getCell ().get (coords.getX ());
							if (thisCell.getAuraFromNode () == null)
							{
								final MapCoordinates2D possibleCoords = new MapCoordinates2D ();
								possibleCoords.setX (coords.getX ());
								possibleCoords.setY (coords.getY ());

								possibleLocations.add (possibleCoords);
							}
						}
					}
				}
			}

			// Did we find enough coordinates?
			if (possibleLocations.size () < squaresRequired)
				fits = false;

			// Actually mark them off?
			else if (setAuraFromNode != null)
			{
				while (squaresRequired > 0)
				{
					// Pick a random square
					final int listIndex = getRandomUtils ().nextInt (possibleLocations.size ());
					final MapCoordinates2D auraCoords = possibleLocations.get (listIndex);

					final ServerGridCellEx auraCell = (ServerGridCellEx) gsk.getTrueMap ().getMap ().getPlane ().get (plane).getRow ().get (auraCoords.getY ()).getCell ().get (auraCoords.getX ());
					auraCell.setAuraFromNode (new MapCoordinates3DEx (setAuraFromNode));

					possibleLocations.remove (listIndex);
					squaresRequired--;
					nodeAuraLeftToPlace--;
				}
			}

			// Even though we're only testing, not actually setting the node aura, we still need to keep track of totals
			else
				nodeAuraLeftToPlace = nodeAuraLeftToPlace - squaresRequired;

			// Next ring
			r++;
		}

		return fits;
	}

	/**
	 * Places nodes randomly on both planes
	 * @throws MomException If there are no node tile types defined in the database
	 */
	final void placeNodes () throws MomException
	{
		// Validate the right map size planes are listed in the session description
		if (sd.getOverlandMapSize ().getMapSizePlane ().size () != sd.getOverlandMapSize ().getDepth ())
			throw new MomException ("placeNodes: Incorrect number of Map Size Planes listed in session description");

		final List<Integer> mapSizePlaneNumbersFound = new ArrayList<Integer> ();
		for (final MapSizePlane plane : sd.getOverlandMapSize ().getMapSizePlane ())
		{
			// Check it isn't already in the list
			for (final Integer planeCheck : mapSizePlaneNumbersFound)
				if (planeCheck == plane.getPlaneNumber ())
					throw new MomException ("placeNodes: Plane " + planeCheck + " is repeated twice in session description Map Size Planes");

			mapSizePlaneNumbersFound.add (plane.getPlaneNumber ());
		}
		
		// Validate the right node strength planes are listed in the session description
		if (sd.getNodeStrength ().getNodeStrengthPlane ().size () != sd.getOverlandMapSize ().getDepth ())
			throw new MomException ("placeNodes: Incorrect number of Node Strength Planes listed in session description");

		final List<Integer> planeNumbersFound = new ArrayList<Integer> ();
		for (final NodeStrengthPlane plane : sd.getNodeStrength ().getNodeStrengthPlane ())
		{
			// Check it isn't already in the list
			for (final Integer planeCheck : planeNumbersFound)
				if (planeCheck == plane.getPlaneNumber ())
					throw new MomException ("placeNodes: Plane " + planeCheck + " is repeated twice in session description Node Strength Planes");

			planeNumbersFound.add (plane.getPlaneNumber ());
		}

		// Now we know the session description is valid, can process each plane in turn
		for (final MapSizePlane mapSizePlane : sd.getOverlandMapSize ().getMapSizePlane ())
			for (int nodeNo = 0; nodeNo < mapSizePlane.getNumberOfNodesOnPlane (); nodeNo++)
			{
				// Find the equivalent node strength plane (don't need to be too careful here, we've already validated and know it exists)
				NodeStrengthPlane plane = null;
				for (final NodeStrengthPlane thisPlane : sd.getNodeStrength ().getNodeStrengthPlane ())
					if (thisPlane.getPlaneNumber () == mapSizePlane.getPlaneNumber ())
						plane = thisPlane;

				// Pick a random size for this node's aura
				final double nodePowerProportion = getRandomUtils ().nextInt (10001) / 10000d;	// Make sure we can roll a 1.0, which you cannot just by using nextDouble ()
				final int nodeAuraSize = plane.getNodeAuraSquaresMinimum () + (int) Math.round
					((plane.getNodeAuraSquaresMaximum () - plane.getNodeAuraSquaresMinimum ()) * nodePowerProportion);

				// Check every cell to see if its possible to fit a node here
				final List<MapCoordinates2D> possibleLocations = new ArrayList<MapCoordinates2D> ();
				for (int x = 0; x < sd.getOverlandMapSize ().getWidth (); x++)
					for (int y = 0; y < sd.getOverlandMapSize ().getHeight (); y++)
					{
						final ServerGridCellEx thisCell = (ServerGridCellEx) gsk.getTrueMap ().getMap ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x);

						if ((thisCell.getTerrainData ().getTileTypeID ().equals (ServerDatabaseValues.TILE_TYPE_GRASS)) &&
							(thisCell.getTerrainData ().getMapFeatureID () == null) &&
							(thisCell.getAuraFromNode () == null) && (placeNodeRings (nodeAuraSize, x, y, plane.getPlaneNumber (), null)))
						{
							final MapCoordinates2D coords = new MapCoordinates2D ();
							coords.setX (x);
							coords.setY (y);

							possibleLocations.add (coords);
						}
					}

				// Pick a location for the node
				if (possibleLocations.size () == 0)
				{
					// Nowhere to fit it - skip this node, but don't exit the loop totally
					// Maybe the reason we couldn't fit it is because it had a huge aura, and next time we'll pick a small aura and be able to fit that one
					log.warn ("Failed to find place to put a node with aura size " + nodeAuraSize + " on plane " + plane.getPlaneNumber () + ", node lost");
				}
				else
				{
					final MapCoordinates2D coords = possibleLocations.get (getRandomUtils ().nextInt (possibleLocations.size ()));
					final ServerGridCellEx thisCell = (ServerGridCellEx) gsk.getTrueMap ().getMap ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ());

					thisCell.getTerrainData ().setTileTypeID (chooseRandomNodeTileTypeID ());
					thisCell.setNodeLairTowerPowerProportion (nodePowerProportion);

					final MapCoordinates3DEx auraFromNode = new MapCoordinates3DEx (coords.getX (), coords.getY (), plane.getPlaneNumber ());

					placeNodeRings (nodeAuraSize, coords.getX (), coords.getY (), plane.getPlaneNumber (), auraFromNode);
				}
			}
	}

	/**
 	 * @return Random Tile Type ID for a node
 	 * @throws MomException If no tile types are defined with magic realm IDs
	 */
	final String chooseRandomNodeTileTypeID () throws MomException
	{
		// List all candidates
		final List<String> nodeTileTypeIDs = new ArrayList<String> ();
		for (final TileTypeEx thisTileType : db.getTileTypes ())
			if (thisTileType.getMagicRealmID () != null)
				nodeTileTypeIDs.add (thisTileType.getTileTypeID ());

		if (nodeTileTypeIDs.size () == 0)
			throw new MomException ("chooseRandomNodeTileTypeID: No node tile types are defined (i.e. tile types with Magic Realm IDs)");

		final String chosenTileTypeID = nodeTileTypeIDs.get (getRandomUtils ().nextInt (nodeTileTypeIDs.size ()));
		return chosenTileTypeID;
	}

	/**
	 * @return Random Feature ID for a monster lair, i.e. any feature that contains monsters that isn't a tower of wizardry
 	 * @throws MomException If no map features are defined with magic realm IDs
	 */
	final String chooseRandomLairFeatureID () throws MomException
	{
		// List all candidates
		final List<String> lairMapFeatureIDs = new ArrayList<String> ();
		for (final MapFeatureEx thisFeature : db.getMapFeatures ())
			if ((thisFeature.getMapFeatureMagicRealm ().size () > 0) &&
				(!thisFeature.getMapFeatureID ().equals (CommonDatabaseConstants.FEATURE_CLEARED_TOWER_OF_WIZARDRY)) &&
				(!thisFeature.getMapFeatureID ().equals (CommonDatabaseConstants.FEATURE_UNCLEARED_TOWER_OF_WIZARDRY)))

				lairMapFeatureIDs.add (thisFeature.getMapFeatureID ());

		if (lairMapFeatureIDs.size () == 0)
			throw new MomException ("chooseRandomLairFeatureID: No lair map feature are defined (i.e. non-tower map features with Magic Realm IDs)");

		final String chosenMapFeatureID = lairMapFeatureIDs.get (getRandomUtils ().nextInt (lairMapFeatureIDs.size ()));
		return chosenMapFeatureID;
	}

	/**
	 * Places lairs randomly on the maps
	 * There's a set number of lairs which are randomly placed between both planes - there isn't a fixed number for each plane
	 * Doesn't attempt to keep them any particular distance from anything else
	 * @param numberOfLairs Number of lairs to place
	 * @param isWeak Whether the lairs are weak or normal
	 * @throws MomException If there are no lair map features defined in the database
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
	 */
	final void placeLairs (final int numberOfLairs, final boolean isWeak)
		throws MomException, RecordNotFoundException
	{
		// Check if possible to place a lair at every cell
		final List<MapCoordinates3DEx> possibleLocations = new ArrayList<MapCoordinates3DEx> ();
		for (int plane = 0; plane < sd.getOverlandMapSize ().getDepth (); plane++)
			for (int x = 0; x < sd.getOverlandMapSize ().getWidth (); x++)
				for (int y = 0; y < sd.getOverlandMapSize ().getHeight (); y++)
				{
					final OverlandMapTerrainData terrainData = gsk.getTrueMap ().getMap ().getPlane ().get (plane).getRow ().get (y).getCell ().get (x).getTerrainData ();
					if ((terrainData.getMapFeatureID () == null) &&
						(db.findTileType (terrainData.getTileTypeID (), "placeLairs").isCanPlaceLair ()))
						
						possibleLocations.add (new MapCoordinates3DEx (x, y, plane));
				}

		// Place lairs
		int lairsPlaced =0;
		while ((lairsPlaced < numberOfLairs) && (possibleLocations.size () > 0))
		{
			final int listIndex = getRandomUtils ().nextInt (possibleLocations.size ());
			final MapCoordinates3DEx coords = possibleLocations.get (listIndex);

			final ServerGridCellEx lairCell = (ServerGridCellEx) gsk.getTrueMap ().getMap ().getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ());

			lairCell.getTerrainData ().setMapFeatureID (chooseRandomLairFeatureID ());
			lairCell.setLairWeak (isWeak);
			lairCell.setNodeLairTowerPowerProportion (getRandomUtils ().nextInt (10001) / 10000d);

			possibleLocations.remove (listIndex);
			lairsPlaced++;
		}
		
		if (lairsPlaced < numberOfLairs)
			log.warn ("Tried to place " + numberOfLairs + " lairs, but could only find space for " + lairsPlaced + " of them");
	}

	/**
	 * Creates the initial combat area effects from the map scenery i.e. node auras
	 * This is an entirely separate process from the terrain generation, and runs separately after the terrain generation has finished
	 * 
	 * @throws RecordNotFoundException If we encounter a combat area effect that we can't find in the cache
	 * @throws JAXBException This only gets generated if addCombatAreaEffectOnServerAndClients tries to send into to players, but we pass null for player list, so won't happen
	 * @throws XMLStreamException This only gets generated if addCombatAreaEffectOnServerAndClients tries to send into to players, but we pass null for player list, so won't happen
	 */
	@Override
	public final void generateInitialCombatAreaEffects ()
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		for (int plane = 0; plane < sd.getOverlandMapSize ().getDepth (); plane++)
			for (int x = 0; x < sd.getOverlandMapSize ().getWidth (); x++)
				for (int y = 0; y < sd.getOverlandMapSize ().getHeight (); y++)
				{
					final ServerGridCellEx thisCell = (ServerGridCellEx) gsk.getTrueMap ().getMap ().getPlane ().get (plane).getRow ().get (y).getCell ().get (x);
					final TileTypeEx thisTileType = db.findTileType (thisCell.getTerrainData ().getTileTypeID (), "generateInitialCombatAreaEffects");

					// Check for area effects from the terrain, e.g. node dispelling effect
					if (thisTileType.getTileTypeAreaEffect ().size () > 0)
					{
						for (final TileTypeAreaEffect thisEffect : thisTileType.getTileTypeAreaEffect ())
							getFogOfWarMidTurnChanges ().addCombatAreaEffectOnServerAndClients
								(gsk, thisEffect.getCombatAreaEffectID (), null, null, null, new MapCoordinates3DEx (x, y, plane), null, sd);
					}
					
					// If this tile is an aura from another square, then we need to add the node aura here too
					else if (thisCell.getAuraFromNode () != null)
					{
						final ServerGridCellEx nodeCell = (ServerGridCellEx) gsk.getTrueMap ().getMap ().getPlane ().get (thisCell.getAuraFromNode ().getZ ()).getRow ().get (thisCell.getAuraFromNode ().getY ()).getCell ().get (thisCell.getAuraFromNode ().getX ());
						final TileTypeEx nodeTileType = db.findTileType (nodeCell.getTerrainData ().getTileTypeID (), "generateInitialCombatAreaEffects");

						for (final TileTypeAreaEffect thisEffect : nodeTileType.getTileTypeAreaEffect ())

							// It must specify that this effect extends out across the node aura
							if ((thisEffect.isExtendAcrossNodeAura () != null) && (thisEffect.isExtendAcrossNodeAura ()))
								getFogOfWarMidTurnChanges ().addCombatAreaEffectOnServerAndClients
									(gsk, thisEffect.getCombatAreaEffectID (), null, null, null, new MapCoordinates3DEx (x, y, plane), null, sd);
					}
				}
	}

	/**
	 * @param magicRealmLifeformTypeID Type of monsters to find
	 * @param monsterBudget Maximum cost of monster
	 * @return Most expensive monster of the requested type, or null if the cheapest monster of this type is still more expensive than our budget
	 */
	final Unit findMostExpensiveMonster (final String magicRealmLifeformTypeID, final int monsterBudget)
	{
		Unit bestMatch = null;

		for (final UnitEx thisUnit : db.getUnits ())
			if (magicRealmLifeformTypeID.equals (thisUnit.getUnitMagicRealm ()))
				if ((thisUnit.getProductionCost () != null) && (thisUnit.getProductionCost () <= monsterBudget) && (thisUnit.getProductionCost () > 0) &&
					((bestMatch == null) || (thisUnit.getProductionCost () > bestMatch.getProductionCost ())))

					bestMatch = thisUnit;

		return bestMatch;
	}

	/**
	 * Fills a single lair or towers with random monsters once the magic realm and strength of the monsters has been decided
	 * See strategy guide pages 416 & 418
	 *
	 * @param lairLocation Location of the lair to add monsters to
	 * @param magicRealmLifeformTypeID Type of monsters to add
	 * @param monsterStrengthMin Minimum strength of monsters to add
	 * @param monsterStrengthMax Maximum strength of monsters to add
	 * @param powerProportion Proportion between min and max of monster strengths, so 0 = use minimum, 1 = use maximum
	 * @param monsterPlayer Player who owns the monsters we add
	 * @throws RecordNotFoundException If we encounter any records that can't be found in the cache
	 * @throws MomException If the unit's skill list ends up containing the same skill twice
	 * @throws PlayerNotFoundException This only gets generated if addUnitOnServerAndClients tries to send into to players, but we pass null for player list, so won't happen
	 * @throws JAXBException This only gets generated if addUnitOnServerAndClients tries to send into to players, but we pass null for player list, so won't happen
	 * @throws XMLStreamException This only gets generated if addUnitOnServerAndClients tries to send into to players, but we pass null for player list, so won't happen
	 */
	final void fillSingleLairOrTowerWithMonsters (final MapCoordinates3DEx lairLocation, final String magicRealmLifeformTypeID,
		final int monsterStrengthMin, final int monsterStrengthMax, final double powerProportion,
		final PlayerServerDetails monsterPlayer)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		int monstersStrength = monsterStrengthMin + (int) Math.round ((monsterStrengthMax - monsterStrengthMin) * powerProportion);

		// Deal with main monsters
		final int mainMonsterBudget = monstersStrength / (getRandomUtils ().nextInt (4) + 1);
		final Unit mainMonster = findMostExpensiveMonster (magicRealmLifeformTypeID, mainMonsterBudget);

		int mainMonsterCount = 0;
		if (mainMonster != null)
		{
			mainMonsterCount = Math.min (monstersStrength / mainMonster.getProductionCost (), CommonDatabaseConstants.MAX_UNITS_PER_MAP_CELL - 1);
			if ((mainMonsterCount > 1) && (getRandomUtils ().nextBoolean ()))
				mainMonsterCount--;

			// Actually add them
			for (int monsterNo = 0; monsterNo < mainMonsterCount; monsterNo++)
			{
				getFogOfWarMidTurnChanges ().addUnitOnServerAndClients (gsk, mainMonster.getUnitID (), lairLocation, null, null, null,
					monsterPlayer, UnitStatusID.ALIVE, null, sd, db);
				monstersStrength = monstersStrength - mainMonster.getProductionCost ();
			}
		}

		// Deal with secondary monsters
		final int secondaryMonsterBudget = monstersStrength / (getRandomUtils ().nextInt (CommonDatabaseConstants.MAX_UNITS_PER_MAP_CELL + 1 - mainMonsterCount) + 1);
		final Unit secondaryMonster = findMostExpensiveMonster (magicRealmLifeformTypeID, secondaryMonsterBudget);

		if (secondaryMonster != null)
		{
			final int secondaryMonsterCount = Math.min (monstersStrength / secondaryMonster.getProductionCost (), CommonDatabaseConstants.MAX_UNITS_PER_MAP_CELL - mainMonsterCount);

			// Actually add them
			for (int monsterNo = 0; monsterNo < secondaryMonsterCount; monsterNo++)
				getFogOfWarMidTurnChanges ().addUnitOnServerAndClients (gsk, secondaryMonster.getUnitID (), lairLocation, null, null, null,
					monsterPlayer, UnitStatusID.ALIVE, null, sd, db);
		}
	}

	/**
	 * Mechanism for generating rampaging monsters spawning from a lair/node is different than populating the lair/node in the first place.
	 * Basically just pick random units until we run out of budget.
	 * 
	 * @param spawnLocation Location next to a lair/node to add monsters to
	 * @param magicRealmLifeformTypeID Type of monsters to add
	 * @param monsterBudget Maximum amount of points to spend buying monsters
	 * @param monsterPlayer Player who owns the monsters we add
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	final void spawnRampagingMonsters (final MapCoordinates3DEx spawnLocation, final String magicRealmLifeformTypeID,
		final int monsterBudget, final PlayerServerDetails monsterPlayer, final MomSessionVariables mom)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException
	{
		int remainingBudget = monsterBudget;
		int monstersCreated = 0;
		boolean keepGoing = true;
		
		while (keepGoing)
		{
			// What monsters are within our budget?
			final List<UnitEx> choices = new ArrayList<UnitEx> ();
			for (final UnitEx thisUnit : mom.getServerDB ().getUnits ())
				if (magicRealmLifeformTypeID.equals (thisUnit.getUnitMagicRealm ()))
					if ((thisUnit.getProductionCost () != null) && (thisUnit.getProductionCost () <= remainingBudget) && (thisUnit.getProductionCost () > 0))
						choices.add (thisUnit);
			
			if (choices.isEmpty ())
				keepGoing = false;
			else
			{
				final UnitEx unitDef = choices.get (getRandomUtils ().nextInt (choices.size ()));
				monstersCreated++;
				remainingBudget = remainingBudget - unitDef.getProductionCost ();

				getFogOfWarMidTurnChanges ().addUnitOnServerAndClients (mom.getGeneralServerKnowledge (), unitDef.getUnitID (), spawnLocation, null, null, null,
					monsterPlayer, UnitStatusID.ALIVE, null, mom.getSessionDescription (), mom.getServerDB ());
				
				if (monstersCreated >= CommonDatabaseConstants.MAX_UNITS_PER_MAP_CELL)
					keepGoing = false;
			}
		}
	}

	/**
	 * Sets the treasure value according to the powerProportion rolled at this location
	 * See strategy guide pages 416 & 418
	 *
	 * @param gridCell Cell to set the treasure value for
	 * @param treasureMin Minimum value of treasure to assign
	 * @param treasureMax Maximum value of treasure to assign
	 */
	final void assignTreasureValue (final ServerGridCellEx gridCell, final int treasureMin, final int treasureMax)
	{
		final int treasureValue = treasureMin + (int) Math.round ((treasureMax - treasureMin) * gridCell.getNodeLairTowerPowerProportion ());
		gridCell.setTreasureValue (treasureValue);
		
		log.debug ("Treasure value for " + gridCell.getTerrainData ().getTileTypeID () + ", " + gridCell.getTerrainData ().getMapFeatureID () + " is " + treasureValue);
	}
	
	/**
	 * Fills all nodes, lairs and towers of wizardry on the map with random monsters
	 * This is really separate from the rest of the methods in this class which are to do with generating the terrain
	 * However its still to do with generating the map so this class is still the most sensible place for it
	 *
	 * @param monsterPlayer Player who owns the monsters we add
	 * @throws RecordNotFoundException If we encounter any records that can't be found in the cache
	 * @throws MomException If the unit's skill list ends up containing the same skill twice
	 * @throws PlayerNotFoundException This only gets generated if addUnitOnServerAndClients tries to send into to players, but we pass null for player list, so won't happen
	 * @throws JAXBException This only gets generated if addUnitOnServerAndClients tries to send into to players, but we pass null for player list, so won't happen
	 * @throws XMLStreamException This only gets generated if addUnitOnServerAndClients tries to send into to players, but we pass null for player list, so won't happen
	 */
	@Override
	public final void fillNodesLairsAndTowersWithMonsters (final PlayerServerDetails monsterPlayer)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		// Validate the right planes are listed in the session description
		if (sd.getDifficultyLevel ().getDifficultyLevelPlane ().size () != db.getPlane ().size ())
			throw new MomException ("fillNodesLairsAndTowersWithMonsters: Incorrect number of Difficulty Level Planes listed in session description");

		{
			final List<Integer> planeNumbersFound = new ArrayList<Integer> ();
			for (final DifficultyLevelPlane plane : sd.getDifficultyLevel ().getDifficultyLevelPlane ())
			{
				// Check it isn't already in the list
				for (final Integer planeCheck : planeNumbersFound)
					if (planeCheck == plane.getPlaneNumber ())
						throw new MomException ("fillNodesLairsAndTowersWithMonsters: Plane " + planeCheck + " is repeated twice in session description Difficulty Level Planes");
	
				planeNumbersFound.add (plane.getPlaneNumber ());
			}
		}

		// Validate the right difficulty level - node strengths are listed in the session description
		if (sd.getDifficultyLevel ().getDifficultyLevelNodeStrength ().size () != db.getPlane ().size ())
			throw new MomException ("fillNodesLairsAndTowersWithMonsters: Incorrect number of Difficulty Level Node Strengths listed in session description");

		{
			final List<Integer> planeNumbersFound = new ArrayList<Integer> ();
			for (final DifficultyLevelNodeStrength plane : sd.getDifficultyLevel ().getDifficultyLevelNodeStrength ())
			{
				// Check it isn't already in the list
				for (final Integer planeCheck : planeNumbersFound)
					if (planeCheck == plane.getPlaneNumber ())
						throw new MomException ("fillNodesLairsAndTowersWithMonsters: Plane " + planeCheck + " is repeated twice in session description Difficulty Level - Node Strengths");
	
				planeNumbersFound.add (plane.getPlaneNumber ());
			}
		}
		
		// Now we know the session description is valid, can process each plane in turn
		for (final DifficultyLevelPlane plane : sd.getDifficultyLevel ().getDifficultyLevelPlane ())
		{
			// Find the equivalent difficulty level - node strength (don't need to be too careful here, we've already validated and know it exists)
			DifficultyLevelNodeStrength nodeStrength = null;
			for (final DifficultyLevelNodeStrength nodeStrengthPlane : sd.getDifficultyLevel ().getDifficultyLevelNodeStrength ())
				if (nodeStrengthPlane.getPlaneNumber () == plane.getPlaneNumber ())
					nodeStrength = nodeStrengthPlane;
			
			// Add monsters to this plane
			log.debug ("fillNodesLairsAndTowersWithMonsters for plane " + plane.getPlaneNumber ());

			for (int x = 0; x < sd.getOverlandMapSize ().getWidth (); x++)
				for (int y = 0; y < sd.getOverlandMapSize ().getHeight (); y++)
				{
					final ServerGridCellEx thisCell = (ServerGridCellEx) gsk.getTrueMap ().getMap ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x);

					// Check the map feature
					// This covers lairs and towers of wizardry
					// Make sure for Towers we only populate them on the first plane
					if ((thisCell.getTerrainData ().getMapFeatureID () != null) &&
						((!getMemoryGridCellUtils ().isTerrainTowerOfWizardry (thisCell.getTerrainData ())) || (plane.getPlaneNumber () == 0)))
					{
						final MapFeatureEx feature = db.findMapFeature (thisCell.getTerrainData ().getMapFeatureID (), "fillNodesLairsAndTowersWithMonsters");

						// Total up all the chances listed for this type of tile
						int totalChance = 0;
						for (final MapFeatureMagicRealm thisMagicRealm : feature.getMapFeatureMagicRealm ())
							totalChance = totalChance + thisMagicRealm.getFeatureChance ();

						if (totalChance > 0)
						{
							// Pick a random magic realm to pick monsters from
							totalChance = getRandomUtils ().nextInt (totalChance);

							// Get the feature ID of the feature we picked
							final Iterator<MapFeatureMagicRealm> magicRealmIterator = feature.getMapFeatureMagicRealm ().iterator ();
							String magicRealmID = null;

							while ((magicRealmID == null) && (magicRealmIterator.hasNext ()))
							{
								final MapFeatureMagicRealm thisMagicRealm = magicRealmIterator.next ();

								if (totalChance < thisMagicRealm.getFeatureChance ())
									magicRealmID = thisMagicRealm.getMagicRealm ();
								else
									totalChance = totalChance - thisMagicRealm.getFeatureChance ();
							}

							// Pick random value for the strength of monsters here
							if (magicRealmID != null)
							{
								final MapCoordinates3DEx lairLocation = new MapCoordinates3DEx (x, y, plane.getPlaneNumber ());								

								if (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (thisCell.getTerrainData ()))
								{
									fillSingleLairOrTowerWithMonsters (lairLocation, magicRealmID,
										sd.getDifficultyLevel ().getTowerMonstersMinimum (), sd.getDifficultyLevel ().getTowerMonstersMaximum (),
										thisCell.getNodeLairTowerPowerProportion (), monsterPlayer);
									
									assignTreasureValue (thisCell, sd.getDifficultyLevel ().getTowerTreasureMinimum (), sd.getDifficultyLevel ().getTowerTreasureMaximum ());
								}
								else if (thisCell.isLairWeak ())
								{
									fillSingleLairOrTowerWithMonsters (lairLocation, magicRealmID,
										plane.getWeakLairMonstersMinimum (), plane.getWeakLairMonstersMaximum (),
										thisCell.getNodeLairTowerPowerProportion (), monsterPlayer);
									
									assignTreasureValue (thisCell, plane.getWeakLairTreasureMinimum (), plane.getWeakLairTreasureMaximum ());
								}
								else
								{
									fillSingleLairOrTowerWithMonsters (lairLocation, magicRealmID,
										plane.getNormalLairMonstersMinimum (), plane.getNormalLairMonstersMaximum (),
										thisCell.getNodeLairTowerPowerProportion (), monsterPlayer);
									
									assignTreasureValue (thisCell, plane.getNormalLairTreasureMinimum (), plane.getNormalLairTreasureMaximum ());
								}
							}
						}
					}
					
					// Check tile type - this covers nodes
					else
					{
						final TileTypeEx tileType = db.findTileType (thisCell.getTerrainData ().getTileTypeID (), "fillNodesLairsAndTowersWithMonsters");
						if (tileType.getMagicRealmID () != null)
						{
							fillSingleLairOrTowerWithMonsters (new MapCoordinates3DEx (x, y, plane.getPlaneNumber ()), tileType.getMagicRealmID (),
								nodeStrength.getMonstersMinimum (), nodeStrength.getMonstersMaximum (),
								thisCell.getNodeLairTowerPowerProportion (), monsterPlayer);
							
							assignTreasureValue (thisCell, nodeStrength.getTreasureMinimum (), nodeStrength.getTreasureMaximum ());
						}
					}
				}
		}
	}
	
	/**
	 * Sets the race for all land squares connected to x, y
	 * @param map True terrain
	 * @param area Area to mark connected land
	 * @param x X coordinate of starting location
	 * @param y Y coordinate of starting location
	 * @param plane Plane of starting location
	 * @param cdb Lookup lists built over the XML database
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
	 */
	final void markContinent (final MapVolumeOfMemoryGridCells map, final MapArea3D<Boolean> area,
		final int x, final int y, final int plane, final CommonDatabase cdb) throws RecordNotFoundException
	{
		final CoordinateSystem sys = area.getCoordinateSystem ();

		// Set centre tile
		area.set (x, y, plane, true);

		// Now branch out in every direction from here
		for (int d = 1; d <= getCoordinateSystemUtils ().getMaxDirection (sys.getCoordinateSystemType ()); d++)
		{
			final MapCoordinates3DEx coords = new MapCoordinates3DEx (x, y, plane);
			if (getCoordinateSystemUtils ().move3DCoordinates (sys, coords, d))
			{
				final OverlandMapTerrainData terrain = map.getPlane ().get (plane).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
				final TileType tileType = cdb.findTileType (terrain.getTileTypeID (), "markContinent");
				
				if ((tileType.isLand () != null) && (tileType.isLand ()) && (area.get (coords) == null))
					markContinent (map, area, coords.getX (), coords.getY (), plane, cdb);
			}
		}
	}
	
	/**
	 * Tries to find a node or lair to generate rampaging monsters from
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If an expected data item can't be found
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void generateRampagingMonsters (final MomSessionVariables mom)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		// Accumulator does tick up and reset even before the minimum turn, otherwise it would be guaranteed to get monsters on turn 50, so check turn here
		if (mom.getGeneralPublicKnowledge ().getTurnNumber () >= mom.getSessionDescription ().getDifficultyLevel ().getRampagingMonstersMinimumTurnNumber ())
		{
			// Find the 2 players we need
			PlayerServerDetails raidersPlayer = null;
			PlayerServerDetails monsterPlayer = null;
			for (final PlayerServerDetails player : mom.getPlayers ())
			{
				final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
				if (CommonDatabaseConstants.WIZARD_ID_RAIDERS.equals (pub.getWizardID ()))
					raidersPlayer = player;
				else if (CommonDatabaseConstants.WIZARD_ID_MONSTERS.equals (pub.getWizardID ()))
					monsterPlayer = player;
			}
			
			if (raidersPlayer == null)
				throw new MomException ("generateRampagingMonsters can't find raiders player");
			if (monsterPlayer == null)
				throw new MomException ("generateRampagingMonsters can't find rampaging monsters player");
			
			// Mark all continents containing non-raider cities
			final MapArea3D<Boolean> monsterContinents = new MapArea3DArrayListImpl<Boolean> ();
			monsterContinents.setCoordinateSystem (mom.getSessionDescription ().getOverlandMapSize ());
			
			for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
				for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
					for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
					{
						final ServerGridCellEx gc = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x);
						if ((gc.getCityData () != null) && (gc.getCityData ().getCityOwnerID () != raidersPlayer.getPlayerDescription ().getPlayerID ()))
							markContinent (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), monsterContinents, x, y, z, mom.getServerDB ());
					}
			
			// Search for nodes and lairs which haven't been cleared yet (the treasureValue gets set to null when they've been cleared),
			// are on the same continent as a non-raider city, and contained summoned units besides Life creatures
			final List<MapCoordinates3DEx> monsterLocations = new ArrayList<MapCoordinates3DEx> ();
			final List<String> monsterMagicRealms = new ArrayList<String> ();
			
			for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
				for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
					for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
					{
						final ServerGridCellEx gc = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x);
						if ((gc.getTreasureValue () != null) && (!getMemoryGridCellUtils ().isTerrainTowerOfWizardry (gc.getTerrainData ())) && (monsterContinents.get (x, y, z) != null))
						{
							// Find the magic realms of the creatures here
							final MapCoordinates3DEx monsterLocation = new MapCoordinates3DEx (x, y, z);
							final List<String> magicRealms = new ArrayList<String> ();
							for (final MemoryUnit mu : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
								if ((monsterLocation.equals (mu.getUnitLocation ())) && (mu.getStatus () == UnitStatusID.ALIVE) && (mu.getOwningPlayerID () == monsterPlayer.getPlayerDescription ().getPlayerID ()))
								{
									final UnitEx unitDef = mom.getServerDB ().findUnit (mu.getUnitID (), "generateRampagingMonsters");
									if (!magicRealms.contains (unitDef.getUnitMagicRealm ()))
										magicRealms.add (unitDef.getUnitMagicRealm ());
								}									
							
							if (magicRealms.size () == 1)
							{
								final Pick pick = mom.getServerDB ().findPick (magicRealms.get (0), "generateRampagingMonsters");
								if ((pick.isGenerateRampagingMonsters () != null) && (pick.isGenerateRampagingMonsters ()))
								{
									monsterLocations.add (monsterLocation);
									monsterMagicRealms.add (magicRealms.get (0));
								}
							}
						}
					}
			
			// Pick the node/lair that will generate monsters
			if (!monsterLocations.isEmpty ())
			{
				final int index = getRandomUtils ().nextInt (monsterLocations.size ());
				final MapCoordinates3DEx monsterLocation = monsterLocations.get (index);
				final String monsterMagicRealm = monsterMagicRealms.get (index);
				final int monsterBudget = ((2 + getRandomUtils ().nextInt (mom.getSessionDescription ().getDifficultyLevel ().getRampagingMonstersAccumulatorMaximum ()) +
					getRandomUtils ().nextInt (mom.getSessionDescription ().getDifficultyLevel ().getRampagingMonstersAccumulatorMaximum ())) *
						mom.getGeneralPublicKnowledge ().getTurnNumber ()) / 4;
				
				log.debug ("Generating rampaing monsters from node/lair at " + monsterLocation + " of realm " + monsterMagicRealm + " with budget " + monsterBudget);
				
				// Pick the adjacent tile where the monsters will spawn
				final List<MapCoordinates3DEx> spawnLocations = new ArrayList<MapCoordinates3DEx> ();
				for (int d = 1; d <= getCoordinateSystemUtils ().getMaxDirection (mom.getSessionDescription ().getOverlandMapSize ().getCoordinateSystemType ()); d++)
				{
					final MapCoordinates3DEx coords = new MapCoordinates3DEx (monsterLocation);
					if (getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, d))
					{
						final OverlandMapTerrainData terrainData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
						final TileType tileType = mom.getServerDB ().findTileType (terrainData.getTileTypeID (), "generateRampagingMonsters");
						if ((tileType.isLand () != null) && (tileType.isLand ()) && (!getMemoryGridCellUtils ().isNodeLairTower (terrainData, mom.getServerDB ())) &&
							(getUnitUtils ().findFirstAliveEnemyAtLocation (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), coords.getX (), coords.getY (), coords.getZ (), 0) == null))
							spawnLocations.add (coords);
					}
				}
				
				if (spawnLocations.isEmpty ())
					log.debug ("Wanted to spawn rampaging monsters from " + monsterLocation + ", but there is no free adjacent land tile");
				else
				{
					final MapCoordinates3DEx spawnLocation = spawnLocations.get (getRandomUtils ().nextInt (spawnLocations.size ()));
					log.debug ("Spawning rampaing monsters from node/lair at " + monsterLocation + " to tile " + spawnLocation);
					
					spawnRampagingMonsters (spawnLocation, monsterMagicRealm, monsterBudget, monsterPlayer, mom);
				}
			}
		}
	}

	/**
	 * @return Where to write the generated map to
	 */
	public final MomGeneralServerKnowledge getGsk ()
	{
		return gsk;
	}

	/**
	 * @param terr Where to write the generated map to
	 */
	public final void setGsk (final MomGeneralServerKnowledge terr)
	{
		gsk = terr;
	}
	
	/**
	 * @return Methods for updating true map + players' memory
	 */
	public final FogOfWarMidTurnChanges getFogOfWarMidTurnChanges ()
	{
		return fogOfWarMidTurnChanges;
	}

	/**
	 * @param obj Methods for updating true map + players' memory
	 */
	public final void setFogOfWarMidTurnChanges (final FogOfWarMidTurnChanges obj)
	{
		fogOfWarMidTurnChanges = obj;
	}

	/**
	 * @return MemoryGridCell utils
	 */
	public final MemoryGridCellUtils getMemoryGridCellUtils ()
	{
		return memoryGridCellUtils;
	}

	/**
	 * @param utils MemoryGridCell utils
	 */
	public final void setMemoryGridCellUtils (final MemoryGridCellUtils utils)
	{
		memoryGridCellUtils = utils;
	}

	/**
	 * @return Session description containing the parameters used to generate the map
	 */
	public final MomSessionDescription getSessionDescription ()
	{
		return sd;
	}

	/**
	 * @param obj Session description containing the parameters used to generate the map
	 */
	public final void setSessionDescription (final MomSessionDescription obj)
	{
		sd = obj;
	}

	/**
	 * @return Server database cache
	 */
	public final CommonDatabase getServerDB ()
	{
		return db;
	}

	/**
	 * @param obj Server database cache
	 */
	public final void setServerDB (final CommonDatabase obj)
	{
		db = obj;
	}

	/**
	 * @return Random number generator
	 */
	public final RandomUtils getRandomUtils ()
	{
		return randomUtils;
	}

	/**
	 * @param utils Random number generator
	 */
	public final void setRandomUtils (final RandomUtils utils)
	{
		randomUtils = utils;
	}

	/**
	 * @return Boolean operations for 2D maps
	 */
	public final BooleanMapAreaOperations2D getBooleanMapAreaOperations2D ()
	{
		return booleanMapAreaOperations2D;
	}

	/**
	 * @param op Boolean operations for 2D maps
	 */
	public final void setBooleanMapAreaOperations2D (final BooleanMapAreaOperations2D op)
	{
		booleanMapAreaOperations2D = op;
	}

	/**
	 * @return Coordinate system utils
	 */
	public final CoordinateSystemUtils getCoordinateSystemUtils ()
	{
		return coordinateSystemUtils;
	}

	/**
	 * @param utils Coordinate system utils
	 */
	public final void setCoordinateSystemUtils (final CoordinateSystemUtils utils)
	{
		coordinateSystemUtils = utils;
	}

	/**
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final UnitUtils utils)
	{
		unitUtils = utils;
	}
}