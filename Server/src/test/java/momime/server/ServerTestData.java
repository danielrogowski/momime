package momime.server;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.DifficultyLevelNodeStrength;
import momime.common.messages.v0_9_4.FogOfWarStateID;
import momime.common.messages.v0_9_4.MapAreaOfFogOfWarStates;
import momime.common.messages.v0_9_4.MapAreaOfMemoryGridCells;
import momime.common.messages.v0_9_4.MapAreaOfStrings;
import momime.common.messages.v0_9_4.MapRowOfFogOfWarStates;
import momime.common.messages.v0_9_4.MapRowOfMemoryGridCells;
import momime.common.messages.v0_9_4.MapRowOfStrings;
import momime.common.messages.v0_9_4.MapVolumeOfFogOfWarStates;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MapVolumeOfStrings;
import momime.common.messages.v0_9_4.MemoryGridCell;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.server.config.ServerConfigConstants;
import momime.server.database.JAXBContextCreator;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseFactory;
import momime.server.database.v0_9_4.DifficultyLevel;
import momime.server.database.v0_9_4.FogOfWarSetting;
import momime.server.database.v0_9_4.LandProportion;
import momime.server.database.v0_9_4.MapSize;
import momime.server.database.v0_9_4.NodeStrength;
import momime.server.database.v0_9_4.SpellSetting;
import momime.server.database.v0_9_4.UnitSetting;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemType;

/**
 * Common constants for server test cases
 */
public final class ServerTestData
{
	/**
	 * @return Location of Original Master of Magic 1.31 rules.Master of Magic Server.xml to test with
	 * @throws IOException If we are unable to locate the server XML file
	 */
	public final static File locateServerXmlFile () throws IOException
	{
		// Not straightforward to find this, because its in src/external/resources so isn't on the classpath

		// Moreover, if we search for something that is on the classpath of the MoMIMEServerDatabase project, and run this test as part of
		// a maven command line build, we get a URL back of the form jar:file:<maven repository>MoMIMEServerDatabase.jar!/momime.server.database/MoMIMEServerDatabase.xsd

		// We can't alter that URL to locate the server XML file within the JAR, simply because the server XML is intentionally not in the JAR or anywhere in Maven at all

		// So only way to do this is locate some resource in *this* project, and modify the location from there
		// This makes the assumption that the MoMIMEServerDatabase project is called as such and hasn't been checked out under a different name
		final URL configXsd = new Object ().getClass ().getResource (ServerConfigConstants.CONFIG_XSD_LOCATION);
		final File configXsdFile = new File (configXsd.getFile ());
		final File serverXmlFile = new File (configXsdFile, "../../../../../MoMIMEServerDatabase/src/external/resources/momime.server.database/Original Master of Magic 1.31 rules.Master of Magic Server.xml");

		return serverXmlFile.getCanonicalFile ();
	}

	/**
	 * @return Parsed server database with all the hash maps built, needed by most of the tests 
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 */
	public final static ServerDatabaseEx loadServerDatabase () throws IOException, JAXBException
	{
		final Unmarshaller unmarshaller = JAXBContextCreator.createServerDatabaseContext ().createUnmarshaller ();		
		unmarshaller.setProperty ("com.sun.xml.bind.ObjectFactory", new Object [] {new ServerDatabaseFactory ()});

		final ServerDatabaseEx serverDB = (ServerDatabaseEx) unmarshaller.unmarshal (locateServerXmlFile ());
		serverDB.buildMaps ();
		return serverDB;
	}

	/**
	 * @param db Server database loaded from XML
	 * @param mapSizeID Map size to use
	 * @param landProportionID Land proportion to use
	 * @param nodeStrengthID Node strength to use
	 * @param difficultyLevelID Difficulty level to use
	 * @param fogOfWarSettingID Fog of war settings to use
	 * @param unitSettingID Unit settings to use
	 * @param spellSettingID Spell settings to use
	 * @return Session description, built from selecting the specified parts from the server database
	 * @throws RecordNotFoundException If we request an entry that can't be found in the database
	 */
	public final static MomSessionDescription createMomSessionDescription (final ServerDatabaseEx db,
		final String mapSizeID, final String landProportionID, final String nodeStrengthID, final String difficultyLevelID,
		final String fogOfWarSettingID, final String unitSettingID, final String spellSettingID)
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

		for (final FogOfWarSetting fogOfWarSetting : db.getFogOfWarSetting ())
			if (fogOfWarSetting.getFogOfWarSettingID ().equals (fogOfWarSettingID))
				sd.setFogOfWarSetting (fogOfWarSetting);
		if (sd.getFogOfWarSetting () == null)
			throw new RecordNotFoundException (FogOfWarSetting.class.getName (), fogOfWarSettingID, "createMomSessionDescription");

		for (final UnitSetting unitSetting : db.getUnitSetting ())
			if (unitSetting.getUnitSettingID ().equals (unitSettingID))
				sd.setUnitSetting (unitSetting);
		if (sd.getUnitSetting () == null)
			throw new RecordNotFoundException (UnitSetting.class.getName (), unitSettingID, "createMomSessionDescription");

		for (final SpellSetting spellSetting : db.getSpellSetting ())
			if (spellSetting.getSpellSettingID ().equals (spellSettingID))
				sd.setSpellSetting (spellSetting);
		if (sd.getSpellSetting () == null)
			throw new RecordNotFoundException (SpellSetting.class.getName (), spellSettingID, "createMomSessionDescription");

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

	/**
	 * @param sys Overland map coordinate system
	 * @return Strings area prepopulated with nulls
	 */
	public final static MapVolumeOfStrings createStringsArea (final CoordinateSystem sys)
	{
		final MapVolumeOfStrings map = new MapVolumeOfStrings ();
		for (int plane = 0; plane < 2; plane++)
		{
			final MapAreaOfStrings area = new MapAreaOfStrings ();
			for (int y = 0; y < sys.getHeight (); y++)
			{
				final MapRowOfStrings row = new MapRowOfStrings ();
				for (int x = 0; x < sys.getWidth (); x++)
					row.getCell ().add (null);

				area.getRow ().add (row);
			}

			map.getPlane ().add (area);
		}

		return map;
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
	 * @param workbook Excel workbook to read tile types from
	 * @return Map area prepopulated with terrain populated from an Excel spreadsheet
	 * @throws IOException If the file cannot be read
	 * @throws InvalidFormatException If the file is not a valid Excel file
	 */
	public final static MapVolumeOfMemoryGridCells createOverlandMapFromExcel (final CoordinateSystem sys, final Workbook workbook)
		throws IOException, InvalidFormatException
	{
		final MapVolumeOfMemoryGridCells map = new MapVolumeOfMemoryGridCells ();
		for (int plane = 0; plane < 2; plane++)
		{
			final MapAreaOfMemoryGridCells area = new MapAreaOfMemoryGridCells ();
			for (int y = 0; y < sys.getHeight (); y++)
			{
				final MapRowOfMemoryGridCells row = new MapRowOfMemoryGridCells ();
				for (int x = 0; x < sys.getWidth (); x++)
				{
					final MemoryGridCell mc = new MemoryGridCell ();

					// Look for data in Excel sheet
					final Cell cell = workbook.getSheetAt (plane).getRow (y + 1).getCell (x + 1);
					if (cell != null)
					{
						final int tileTypeNumber = (int) cell.getNumericCellValue ();
						if (tileTypeNumber > 0)
						{
							String tileTypeID = new Integer (tileTypeNumber).toString ();
							while (tileTypeID.length () < 2)
								tileTypeID = "0" + tileTypeID;

							final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
							terrainData.setTileTypeID ("TT" + tileTypeID);
							mc.setTerrainData (terrainData);
						}
					}

					row.getCell ().add (mc);
				}

				area.getRow ().add (row);
			}

			map.getPlane ().add (area);
		}

		return map;
	}
}
