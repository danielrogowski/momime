package momime.server;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import momime.common.database.CommonDatabaseConstants;
import momime.common.database.CommonXsdResourceResolver;
import momime.common.database.DifficultyLevelNodeStrength;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.MapSizeData;
import momime.common.messages.CombatMapSizeData;
import momime.common.messages.FogOfWarStateID;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MapAreaOfFogOfWarStates;
import momime.common.messages.MapAreaOfMemoryGridCells;
import momime.common.messages.MapAreaOfStrings;
import momime.common.messages.MapRowOfCombatTiles;
import momime.common.messages.MapRowOfFogOfWarStates;
import momime.common.messages.MapRowOfMemoryGridCells;
import momime.common.messages.MapRowOfStrings;
import momime.common.messages.MapVolumeOfFogOfWarStates;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MapVolumeOfStrings;
import momime.common.messages.MomCombatTile;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapTerrainData;
import momime.server.database.ServerDatabaseConstants;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseExImpl;
import momime.server.database.ServerDatabaseFactory;
import momime.server.database.v0_9_5.DifficultyLevel;
import momime.server.database.v0_9_5.FogOfWarSetting;
import momime.server.database.v0_9_5.LandProportion;
import momime.server.database.v0_9_5.MapSize;
import momime.server.database.v0_9_5.NodeStrength;
import momime.server.database.v0_9_5.ServerDatabase;
import momime.server.database.v0_9_5.SpellSetting;
import momime.server.database.v0_9_5.UnitSetting;
import momime.server.messages.v0_9_5.ServerGridCell;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemType;

/**
 * Common constants for server test cases
 */
public final class ServerTestData
{
	/**
	 * @return Parsed server database with all the hash maps built, needed by those tests that require too much data to mock out, but generally avoid using this if at all possible 
	 * @throws Exception If there is a problem
	 */
	public final static ServerDatabaseEx loadServerDatabase () throws Exception
	{
		// XSD
		final URL xsdResource = new Object ().getClass ().getResource (ServerDatabaseConstants.SERVER_XSD_LOCATION);
		assertNotNull ("MoM IME Server XSD could not be found on classpath", xsdResource);

		final SchemaFactory schemaFactory = SchemaFactory.newInstance (XMLConstants.W3C_XML_SCHEMA_NS_URI);
		schemaFactory.setResourceResolver (new CommonXsdResourceResolver (DOMImplementationRegistry.newInstance ()));
		
		final Schema schema = schemaFactory.newSchema (xsdResource);

		final Unmarshaller unmarshaller = JAXBContext.newInstance (ServerDatabase.class).createUnmarshaller ();		
		unmarshaller.setProperty ("com.sun.xml.bind.ObjectFactory", new Object [] {new ServerDatabaseFactory ()});
		unmarshaller.setSchema (schema);
		
		// XML - not straightforward to find this, because its in src/external/resources so isn't on the classpath
		// So instead find something that is on the classpath of the MoMIMEServer project, then modify that location
		final File serverXsdFile = new File (xsdResource.getFile ());
		final File serverXmlFile = new File (serverXsdFile, "../../../../src/external/resources/momime.server.database/Original Master of Magic 1.31 rules.Master of Magic Server.xml");
		
		final ServerDatabaseExImpl serverDB = (ServerDatabaseExImpl) unmarshaller.unmarshal (serverXmlFile);
		serverDB.buildMaps ();
		return serverDB;
	}

	/**
	 * @return Location of server XML to test with
	 * @throws IOException If we are unable to locate the server XML file
	 */
	public final static File locateServerXmlFile () throws IOException
	{
		// Not straightforward to find this, because its in src/external/resources so isn't on the classpath
		// So instead find something that is on the classpath of the MoMIMEServer project, then modify that location
		final URL serverXSD = new Object ().getClass ().getResource (ServerDatabaseConstants.SERVER_XSD_LOCATION);
		final File serverXsdFile = new File (serverXSD.getFile ());
		return new File (serverXsdFile, "../../../../src/external/resources/momime.server.database/Original Master of Magic 1.31 rules.Master of Magic Server.xml");
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

		sd.setCombatMapSize (createCombatMapSizeData ());
		
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
		sys.setDepth (2);
		sys.setWrapsLeftToRight (true);
		return sys;
	}

	/**
	 * @return Overland map coordinate system that can be included into session description
	 */
	public final static MapSizeData createMapSizeData ()
	{
		final MapSizeData sys = new MapSizeData ();
		sys.setCoordinateSystemType (CoordinateSystemType.SQUARE);
		sys.setWidth (60);
		sys.setHeight (40);
		sys.setDepth (2);
		sys.setWrapsLeftToRight (true);
		
		sys.setCitySeparation (3);
		sys.setContinentalRaceChance (75);
		
		return sys;
	}

	/**
	 * @return Demo MoM combat map-like coordinate system with a 60x40 diamond non-wrapping map
	 */
	public final static CoordinateSystem createCombatMapCoordinateSystem ()
	{
		final CoordinateSystem sys = new CoordinateSystem ();
		sys.setCoordinateSystemType (CoordinateSystemType.DIAMOND);
		sys.setWidth (CommonDatabaseConstants.COMBAT_MAP_WIDTH);
		sys.setHeight (CommonDatabaseConstants.COMBAT_MAP_HEIGHT);
		return sys;
	}

	/**
	 * @return Combat map coordinate system that can be included into session description
	 */
	public final static CombatMapSizeData createCombatMapSizeData ()
	{
		final CombatMapSizeData sys = new CombatMapSizeData ();
		sys.setCoordinateSystemType (CoordinateSystemType.DIAMOND);
		sys.setWidth (CommonDatabaseConstants.COMBAT_MAP_WIDTH);
		sys.setHeight (CommonDatabaseConstants.COMBAT_MAP_HEIGHT);
		sys.setZoneWidth (10);
		sys.setZoneHeight (8);
		return sys;
	}

	/**
	 * @param sys Overland map coordinate system
	 * @return FOW area prepopulated with "Never Seen"
	 */
	public final static MapVolumeOfFogOfWarStates createFogOfWarArea (final CoordinateSystem sys)
	{
		final MapVolumeOfFogOfWarStates map = new MapVolumeOfFogOfWarStates ();
		for (int plane = 0; plane < sys.getDepth (); plane++)
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
	public final static MapVolumeOfStrings createStringsVolume (final CoordinateSystem sys)
	{
		final MapVolumeOfStrings map = new MapVolumeOfStrings ();
		for (int plane = 0; plane < sys.getDepth (); plane++)
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
		for (int plane = 0; plane < sys.getDepth (); plane++)
		{
			final MapAreaOfMemoryGridCells area = new MapAreaOfMemoryGridCells ();
			for (int y = 0; y < sys.getHeight (); y++)
			{
				final MapRowOfMemoryGridCells row = new MapRowOfMemoryGridCells ();
				for (int x = 0; x < sys.getWidth (); x++)
					row.getCell ().add (new ServerGridCell ());

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
		for (int plane = 0; plane < sys.getDepth (); plane++)
		{
			final MapAreaOfMemoryGridCells area = new MapAreaOfMemoryGridCells ();
			for (int y = 0; y < sys.getHeight (); y++)
			{
				final MapRowOfMemoryGridCells row = new MapRowOfMemoryGridCells ();
				for (int x = 0; x < sys.getWidth (); x++)
				{
					final ServerGridCell mc = new ServerGridCell ();

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

	/**
	 * @return Map area prepopulated with empty cells
	 */
	public final static MapAreaOfCombatTiles createCombatMap ()
	{
		final MapAreaOfCombatTiles map = new MapAreaOfCombatTiles ();
		for (int y = 0; y < CommonDatabaseConstants.COMBAT_MAP_HEIGHT; y++)
		{
			final MapRowOfCombatTiles row = new MapRowOfCombatTiles ();
			for (int x = 0; x < CommonDatabaseConstants.COMBAT_MAP_WIDTH; x++)
				row.getCell ().add (new MomCombatTile ());

			map.getRow ().add (row);
		}

		return map;
	}
}