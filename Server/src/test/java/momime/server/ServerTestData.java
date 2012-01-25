package momime.server;

import java.io.File;

import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.DifficultyLevelNodeStrength;
import momime.common.messages.v0_9_4.FogOfWarStateID;
import momime.common.messages.v0_9_4.MapAreaOfFogOfWarStates;
import momime.common.messages.v0_9_4.MapAreaOfMemoryGridCells;
import momime.common.messages.v0_9_4.MapRowOfFogOfWarStates;
import momime.common.messages.v0_9_4.MapRowOfMemoryGridCells;
import momime.common.messages.v0_9_4.MapVolumeOfFogOfWarStates;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryGridCell;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.server.database.v0_9_4.DifficultyLevel;
import momime.server.database.v0_9_4.LandProportion;
import momime.server.database.v0_9_4.MapSize;
import momime.server.database.v0_9_4.NodeStrength;
import momime.server.database.v0_9_4.ServerDatabase;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemType;

/**
 * Common constants for server test cases
 */
public final class ServerTestData
{
	/** Location of XML file to test with */
	public static final String SERVER_XML_LOCATION = "F:\\Workspaces\\Eclipse\\MoMIMEServer\\src\\external\\resources\\momime.server.database\\";

	/** Location of XML file to test with */
	public static final File SERVER_XML_FILE = new File ("F:\\Workspaces\\Eclipse\\MoMIMEServer\\src\\external\\resources\\momime.server.database\\Original Master of Magic 1.31 rules.Master of Magic Server.xml");

	/**
	 * This is duplicated from the dummy test client
	 *
	 * @param db Server database loaded from XML
	 * @param mapSizeID Map size to use
	 * @param landProportionID Land proportion to use
	 * @param nodeStrengthID Node strength to use
	 * @param difficultyLevelID Difficulty level to use
	 * @return Session description, built from selecting the specified parts from the server database
	 * @throws RecordNotFoundException If we request an entry that can't be found in the database
	 */
	public final static MomSessionDescription createMomSessionDescription (final ServerDatabase db,
		final String mapSizeID, final String landProportionID, final String nodeStrengthID, final String difficultyLevelID)
		throws RecordNotFoundException
	{
		final MomSessionDescription sd = new MomSessionDescription ();

		for (final MapSize mapSize : db.getMapSize ())
			if (mapSize.getMapSizeID ().equals (mapSizeID))
				sd.setMapSize (mapSize);
		if (sd.getMapSize () == null)
			throw new RecordNotFoundException (MapSize.class.getName (), mapSizeID, "createMomSessionDescription");

		for (final LandProportion landProportion : db.getLandProportion ())
			if (landProportion.getLandProportionID ().equals (landProportionID))
				sd.setLandProportion (landProportion);
		if (sd.getLandProportion () == null)
			throw new RecordNotFoundException (LandProportion.class.getName (), landProportionID, "createMomSessionDescription");

		for (final NodeStrength nodeStrength : db.getNodeStrength ())
			if (nodeStrength.getNodeStrengthID ().equals (nodeStrengthID))
				sd.setNodeStrength (nodeStrength);
		if (sd.getNodeStrength () == null)
			throw new RecordNotFoundException (NodeStrength.class.getName (), nodeStrengthID, "createMomSessionDescription");

		for (final DifficultyLevel difficultyLevel : db.getDifficultyLevel ())
			if (difficultyLevel.getDifficultyLevelID ().equals (difficultyLevelID))
			{
				sd.setDifficultyLevel (difficultyLevel);

				// Also find difficulty level - node strength settings
				for (final DifficultyLevelNodeStrength nodeStrength : difficultyLevel.getDifficultyLevelNodeStrength ())
					if (nodeStrength.getNodeStrengthID ().equals (nodeStrengthID))
						sd.getDifficultyLevelNodeStrength ().add (nodeStrength);
			}
		if (sd.getDifficultyLevel () == null)
			throw new RecordNotFoundException (DifficultyLevel.class.getName (), difficultyLevelID, "createMomSessionDescription");

		return sd;
	}

	/**
	 * @return Demo MoM overland map-like coordinate system with a 60x40 square map wrapping left-to-right but not top-to-bottom
	 */
	public final static CoordinateSystem createOverlandMapCoordinateSystem ()
	{
		final CoordinateSystem sys = new CoordinateSystem ();
		sys.setCoordinateSystemType (CoordinateSystemType.SQUARE);
		sys.setWidth (60);
		sys.setHeight (40);
		sys.setWrapsLeftToRight (true);
		return sys;
	}

	/**
	 * @param sys Overland map coordinate system
	 * @return Map area prepopulated with empty cells
	 */
	public final static MapVolumeOfMemoryGridCells createOverlandMap (final CoordinateSystem sys)
	{
		final MapVolumeOfMemoryGridCells map = new MapVolumeOfMemoryGridCells ();
		for (int plane = 0; plane < 2; plane++)
		{
			final MapAreaOfMemoryGridCells area = new MapAreaOfMemoryGridCells ();
			for (int y = 0; y < sys.getHeight (); y++)
			{
				final MapRowOfMemoryGridCells row = new MapRowOfMemoryGridCells ();
				for (int x = 0; x < sys.getWidth (); x++)
					row.getCell ().add (new MemoryGridCell ());

				area.getRow ().add (row);
			}

			map.getPlane ().add (area);
		}

		return map;
	}

	/**
	 * @param sys Overland map coordinate system
	 * @return FOW area prepopulated with "Never Seen"
	 */
	public final static MapVolumeOfFogOfWarStates createFogOfWarArea (final CoordinateSystem sys)
	{
		final MapVolumeOfFogOfWarStates map = new MapVolumeOfFogOfWarStates ();
		for (int plane = 0; plane < 2; plane++)
		{
			final MapAreaOfFogOfWarStates area = new MapAreaOfFogOfWarStates ();
			for (int y = 0; y < sys.getHeight (); y++)
			{
				final MapRowOfFogOfWarStates row = new MapRowOfFogOfWarStates ();
				for (int x = 0; x < sys.getWidth (); x++)
					row.getCell ().add (FogOfWarStateID.NEVER_SEEN);

				area.getRow ().add (row);
			}

			map.getPlane ().add (area);
		}

		return map;
	}
}
