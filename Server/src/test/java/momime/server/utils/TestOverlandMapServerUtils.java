package momime.server.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import momime.common.MomException;
import momime.common.calculations.MomCityCalculations;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_4.MapAreaOfMemoryGridCells;
import momime.common.messages.v0_9_4.MapRowOfMemoryGridCells;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryGridCell;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.server.ServerTestData;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseValues;
import momime.server.database.v0_9_4.CityNameContainer;
import momime.server.database.v0_9_4.Plane;
import momime.server.database.v0_9_4.Race;
import momime.server.messages.v0_9_4.MomGeneralServerKnowledge;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.MapCoordinates;
import com.ndg.map.SquareMapDirection;
import com.ndg.map.areas.StringMapArea2DArray;

/**
 * Tests the OverlandMapServerUtils class
 */
public class TestOverlandMapServerUtils
{
	/** Dummy logger to use during unit tests */
	private final Logger debugLogger = Logger.getLogger ("MoMIMEServerUnitTests");

	/**
	 * Tests the chooseRandomRaceForPlane method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
 	 * @throws MomException If no races are defined with the requested plane
 	 * @throws RecordNotFoundException If we can't find the returned race ID in the database
	 */
	@Test
	public final void testChooseRandomRaceForPlane () throws IOException, JAXBException, MomException, RecordNotFoundException
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		// Pick a random race for each plane
		for (final Plane plane : db.getPlane ())
		{
			final String raceID = OverlandMapServerUtils.chooseRandomRaceForPlane (plane.getPlaneNumber (), db, debugLogger);

			// Verify race inhabits the correct plane
			assertEquals (plane.getPlaneNumber (), db.findRace (raceID, "testChooseRandomRaceForPlane").getNativePlane ());
		}
	}

	/**
	 * Tests the setContinentalRace method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If we request an entry that can't be found in the database
	 */
	@Test
	public final void testSetContinentalRace () throws IOException, JAXBException, RecordNotFoundException
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = ServerTestData.createOverlandMap (sys);

		// Fill map with ocean
		for (final MapAreaOfMemoryGridCells plane : map.getPlane ())
			for (final MapRowOfMemoryGridCells row : plane.getRow ())
				for (final MemoryGridCell cell : row.getCell ())
				{
					final OverlandMapTerrainData terrain = new OverlandMapTerrainData ();
					terrain.setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_OCEAN);

					cell.setTerrainData (terrain);
				}

		// Create area to output into
		final List<StringMapArea2DArray> continentalRace = new ArrayList<StringMapArea2DArray> ();
		for (int plane = 0; plane < db.getPlane ().size (); plane++)
			continentalRace.add (new StringMapArea2DArray (sys, debugLogger));

		// One cell of land
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).getTerrainData ().setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_GRASS);
		OverlandMapServerUtils.setContinentalRace (map, continentalRace, 20, 10, 1, "RC01", db, debugLogger);
		assertEquals (1, continentalRace.get (1).countCellsEqualTo ("RC01"));

		// Mark a city radius around the tile - just to test a non-square area
		final MapCoordinates coords = new MapCoordinates ();
		coords.setX (20);
		coords.setY (10);

		for (final SquareMapDirection d : MomCityCalculations.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
			if (CoordinateSystemUtils.moveCoordinates (sys, coords, d.getDirectionID ()))
				map.getPlane ().get (1).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ().setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_MOUNTAIN);

		OverlandMapServerUtils.setContinentalRace (map, continentalRace, 20, 10, 1, "RC02", db, debugLogger);
		assertEquals (21, continentalRace.get (1).countCellsEqualTo ("RC02"));
	}

	/**
	 * Tests the decideAllContinentalRaces method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If we request an entry that can't be found in the database
 	 * @throws MomException If no races are defined for a particular plane
	 */
	@Test
	public final void testDecideAllContinentalRaces () throws IOException, JAXBException, RecordNotFoundException, MomException
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = ServerTestData.createOverlandMap (sys);

		// Fill map with ocean
		for (final MapAreaOfMemoryGridCells plane : map.getPlane ())
			for (final MapRowOfMemoryGridCells row : plane.getRow ())
				for (final MemoryGridCell cell : row.getCell ())
				{
					final OverlandMapTerrainData terrain = new OverlandMapTerrainData ();
					terrain.setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_OCEAN);

					cell.setTerrainData (terrain);
				}

		// Run method
		final List<StringMapArea2DArray> continentalRace = OverlandMapServerUtils.decideAllContinentalRaces (map, sys, db, debugLogger);

		// Check results, we should have
		// null at every sea tile
		// race at every land tile, that inhabits the correct plane
		// same race at any adjacent land tile
		for (final Plane plane : db.getPlane ())
			for (int x = 0; x < sys.getWidth (); x++)
				for (int y = 0; y < sys.getHeight (); y++)
				{
					final OverlandMapTerrainData terrain = map.getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x).getTerrainData ();
					final String raceID = continentalRace.get (plane.getPlaneNumber ()).get (x, y);

					if (db.findTileType (terrain.getTileTypeID (), "testDecideAllContinentalRaces").isIsLand ())
					{
						// Land tile
						assertEquals (plane.getPlaneNumber (), db.findRace (raceID, "testDecideAllContinentalRaces").getNativePlane ());

						// Check adjacent tiles are the same race
						for (int d = 1; d <= CoordinateSystemUtils.getMaxDirection (sys.getCoordinateSystemType ()); d++)
						{
							final MapCoordinates coords = new MapCoordinates ();
							coords.setX (x);
							coords.setY (y);

							if (CoordinateSystemUtils.moveCoordinates (sys, coords, d))
							{
								final String adjacentRaceID = continentalRace.get (plane.getPlaneNumber ()).get (coords);
								if (adjacentRaceID != null)
									assertEquals (raceID, adjacentRaceID);
							}
						}
					}
					else
						// Sea tile
						assertNull (raceID);
				}
	}

	/**
	 * Tests the generateCityName method
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If we can't find the requested race
	 */
	@Test
	public final void testGenerateCityName () throws JAXBException, RecordNotFoundException
	{
		// If we have 4 possible names
		final String names [] = new String [] {"Hanna", "Emily", "Mara", "Rose"};

		final Race race = new Race ();
		for (final String thisName : names)
		{
			final CityNameContainer cont = new CityNameContainer ();
			cont.setCityName (thisName);
			race.getCityName ().add (cont);
		}

		// But 3 of them are already used
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.getUsedCityName ().add ("Hanna");
		gsk.getUsedCityName ().add ("Emily");
		gsk.getUsedCityName ().add ("Rose");

		// Then there's only one possible outcome
		assertEquals ("Mara", OverlandMapServerUtils.generateCityName (gsk, race));

		// Now all 4 are used, the next 4 we generate will be suffixed with II
		for (int n = 0; n < 4; n++)
		{
			final String name = OverlandMapServerUtils.generateCityName (gsk, race);
			if (!name.endsWith (" II"))
				fail ("Second round of city names not suffixed with II, got \"" + name + "\"");
		}

		// And the next 4 we generate will be suffixed with III
		for (int n = 0; n < 4; n++)
		{
			final String name = OverlandMapServerUtils.generateCityName (gsk, race);
			if (!name.endsWith (" III"))
				fail ("Third round of city names not suffixed with III, got \"" + name + "\"");
		}
	}
}
