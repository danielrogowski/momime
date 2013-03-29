package momime.server.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_4.CityNameContainer;
import momime.server.database.v0_9_4.Plane;
import momime.server.database.v0_9_4.Race;
import momime.server.mapgenerator.OverlandMapGenerator;
import momime.server.messages.v0_9_4.MomGeneralServerKnowledge;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.MapCoordinates;
import com.ndg.map.areas.StringMapArea2DArray;

/**
 * Server side only helper methods for dealing with the overland map
 */
public final class OverlandMapServerUtils
{
	/**
	 * @param plane Plane that we want to choose a race for
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
 	 * @return ID of a random race who inhabits the requested plane
 	 * @throws MomException If no races are defined with the requested plane
	 */
	final static String chooseRandomRaceForPlane (final int plane, final ServerDatabaseEx db, final Logger debugLogger)
		throws MomException
	{
		debugLogger.exiting (OverlandMapGenerator.class.getName (), "chooseRandomRaceForPlane");

		// List all candidates
		final List<String> raceIDs = new ArrayList<String> ();
		for (final Race thisRace : db.getRace ())
			if (thisRace.getNativePlane () == plane)
				raceIDs.add (thisRace.getRaceID ());

		if (raceIDs.size () == 0)
			throw new MomException ("chooseRandomRaceForPlane: No races are defined who inhabit plane \"" + plane + "\"");

		final String chosenRaceID = raceIDs.get (RandomUtils.getGenerator ().nextInt (raceIDs.size ()));

		debugLogger.exiting (OverlandMapGenerator.class.getName (), "chooseRandomRaceForPlane", chosenRaceID);
		return chosenRaceID;
	}

	/**
	 * Sets the race for all land squares connected to x, y
	 * @param map True terrain
	 * @param continentalRace Map area listing the continental race ID at each location
	 * @param x X coordinate of starting location
	 * @param y Y coordinate of starting location
	 * @param plane Plane of starting location
	 * @param raceID Race ID to set
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
	 */
	final static void setContinentalRace (final MapVolumeOfMemoryGridCells map, final List<StringMapArea2DArray> continentalRace,
		final int x, final int y, final int plane, final String raceID, final ServerDatabaseEx db, final Logger debugLogger) throws RecordNotFoundException
	{
		debugLogger.entering (OverlandMapServerUtils.class.getName (), "setContinentalRace",
			new String [] {new Integer (x).toString (), new Integer (y).toString (), new Integer (plane).toString (), raceID});

		final CoordinateSystem sys = continentalRace.get (plane).getCoordinateSystem ();

		// Set centre tile
		continentalRace.get (plane).set (x, y, raceID);

		// Now branch out in every direction from here
		for (int d = 1; d <= CoordinateSystemUtils.getMaxDirection (sys.getCoordinateSystemType ()); d++)
		{
			final MapCoordinates coords = new MapCoordinates ();
			coords.setX (x);
			coords.setY (y);

			if (CoordinateSystemUtils.moveCoordinates (sys, coords, d))
			{
				final OverlandMapTerrainData terrain = map.getPlane ().get (plane).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();

				// NB. IsLand is Boolean so could be null, but should be set for all tile types produced by the map generator
				// the only tile types for which it is null are those which are special references for the movement tables, e.g. road tiles
				if ((db.findTileType (terrain.getTileTypeID (), "setContinentalRace").isIsLand ()) && (continentalRace.get (plane).get (coords) == null))
					setContinentalRace (map, continentalRace, coords.getX (), coords.getY (), plane, raceID, db, debugLogger);
			}
		}

		debugLogger.exiting (OverlandMapServerUtils.class.getName (), "setContinentalRace");
	}

	/**
	 * Sets the continental race (mostly likely race raiders cities at each location will choose) for every land tile on the map
	 *
	 * @param map Known terrain
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return Generated area of race IDs
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
 	 * @throws MomException If no races are defined for a particular plane
	 */
	public final static List<StringMapArea2DArray> decideAllContinentalRaces (final MapVolumeOfMemoryGridCells map,
		final CoordinateSystem sys, final ServerDatabaseEx db, final Logger debugLogger) throws RecordNotFoundException, MomException
	{
		debugLogger.entering (OverlandMapServerUtils.class.getName (), "decideAllContinentalRaces");

		// Allocate a race to each continent of land for raider cities
		final List<StringMapArea2DArray> continentalRace = new ArrayList<StringMapArea2DArray> ();
		for (int plane = 0; plane < db.getPlane ().size (); plane++)
			continentalRace.add (new StringMapArea2DArray (sys, debugLogger));

		for (final Plane plane : db.getPlane ())
			for (int x = 0; x < sys.getWidth (); x++)
				for (int y = 0; y < sys.getHeight (); y++)
				{
					final OverlandMapTerrainData terrain = map.getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x).getTerrainData ();

					// NB. IsLand is Boolean so could be null, but should be set for all tile types produced by the map generator
					// the only tile types for which it is null are those which are special references for the movement tables, e.g. road tiles
					if ((db.findTileType (terrain.getTileTypeID (), "decideAllContinentalRaces").isIsLand ()) &&
						(continentalRace.get (plane.getPlaneNumber ()).get (x, y) == null))

						setContinentalRace (map, continentalRace, x, y, plane.getPlaneNumber (),
							chooseRandomRaceForPlane (plane.getPlaneNumber (), db, debugLogger), db, debugLogger);
				}

		debugLogger.exiting (OverlandMapServerUtils.class.getName (), "decideAllContinentalRaces", continentalRace);
		return continentalRace;
	}

	/**
	 * NB. This will always return names unique from those names it has generated before - but if human players happen to rename their cities
	 * to a name that the generator hasn't produced yet, it won't avoid generating that name
	 *
	 * @param gsk Server knowledge data structure
	 * @param race The race who is creating a new city
	 * @return Auto generated city name
	 */
	public final static String generateCityName (final MomGeneralServerKnowledge gsk, final Race race)
	{
		final List<String> possibleChoices = new ArrayList<String> ();

		// Try increasing suffixes, (none), II, III, IV, V, and so on... eventually we have to generate a city name that's not been used!
		String chosenCityName = null;
		int numeral = 1;

		while (chosenCityName == null)
		{
			// Test each name to see if it has been used before
			possibleChoices.clear ();
			for (final CityNameContainer thisCache : race.getCityName ())
			{
				String thisName = thisCache.getCityName ();
				if (numeral > 1)
					thisName = thisName + " " + RomanNumerals.intToRoman (numeral);

				if (!gsk.getUsedCityName ().contains (thisName))
					possibleChoices.add (thisName);
			}

			// If any names are left then pick one, if not then increase the roman numeral suffix
			if (possibleChoices.size () > 0)
				chosenCityName = possibleChoices.get (RandomUtils.getGenerator ().nextInt (possibleChoices.size ()));
			else
				numeral++;
		}

		gsk.getUsedCityName ().add (chosenCityName);

		return chosenCityName;
	}

	/**
	 * Prevent instantiation
	 */
	private OverlandMapServerUtils ()
	{
	}
}
