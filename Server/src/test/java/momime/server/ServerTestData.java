package momime.server;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemType;
import com.ndg.utils.XmlConstants;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import momime.common.database.AnimationEx;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.CommonDatabaseFactory;
import momime.common.database.CommonDatabaseImpl;
import momime.common.database.CommonDatabaseObjectFactory;
import momime.common.database.CommonXsdResourceResolver;
import momime.common.database.DifficultyLevel;
import momime.common.database.DifficultyLevelNodeStrength;
import momime.common.database.FogOfWarSetting;
import momime.common.database.LandProportion;
import momime.common.database.MapFeatureEx;
import momime.common.database.MomDatabase;
import momime.common.database.NodeStrength;
import momime.common.database.OverlandMapSize;
import momime.common.database.RecordNotFoundException;
import momime.common.database.SmoothedTileTypeEx;
import momime.common.database.SpellSetting;
import momime.common.database.TileSetEx;
import momime.common.database.UnitSetting;
import momime.common.database.WizardEx;
import momime.common.messages.CombatMapSize;
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
import momime.server.knowledge.ServerGridCellEx;

/**
 * Common constants for server test cases
 */
public class ServerTestData
{
	/**
	 * @return Parsed server database with all the hash maps built, needed by those tests that require too much data to mock out, but generally avoid using this if at all possible 
	 * @throws Exception If there is a problem
	 */
	public final CommonDatabase loadServerDatabase () throws Exception
	{
		// Need to set up a proper factory to create classes with spring injections
		final CommonDatabaseObjectFactory factory = new CommonDatabaseObjectFactory ();
		factory.setFactory (new CommonDatabaseFactory ()
		{
			@Override
			public final CommonDatabaseImpl createDatabase ()
			{
				return new CommonDatabaseImpl ();
			}
			
			@Override
			public final WizardEx createWizard ()
			{
				return new WizardEx (); 
			}
			
			@Override
			public final MapFeatureEx createMapFeature ()
			{
				return new MapFeatureEx ();
			}
			
			@Override
			public final TileSetEx createTileSet ()
			{
				return new TileSetEx ();
			}
			
			@Override
			public final SmoothedTileTypeEx createSmoothedTileType ()
			{
				return new SmoothedTileTypeEx ();
			}
			
			@Override
			public final AnimationEx createAnimation ()
			{
				return new AnimationEx ();
			}
		});

		// XSD
		final URL xsdResource = getClass ().getResource (CommonDatabaseConstants.COMMON_XSD_LOCATION);
		assertNotNull (xsdResource, "MoM IME Common XSD could not be found on classpath");

		final SchemaFactory schemaFactory = SchemaFactory.newInstance (XMLConstants.W3C_XML_SCHEMA_NS_URI);
		schemaFactory.setResourceResolver (new CommonXsdResourceResolver (DOMImplementationRegistry.newInstance ()));
		
		final Schema schema = schemaFactory.newSchema (xsdResource);

		final Unmarshaller unmarshaller = JAXBContext.newInstance (MomDatabase.class).createUnmarshaller ();		
		unmarshaller.setProperty (XmlConstants.OBJECT_FACTORY, new Object [] {factory});
		unmarshaller.setSchema (schema);
		
		// XML - not straightforward to find this, because its in src/external/resources so isn't on the classpath
		// So instead find something that is on the classpath of the MoMIMEServer project, then modify that location
		final URL exampleResource = getClass ().getResource ("/MoMIMEServerVersion.properties");
		assertNotNull (exampleResource, "MoMIMEServerVersion.properties could not be found on classpath");
		
		final File exampleResourceFile = new File (exampleResource.getFile ());
		final File serverXmlFile = new File (exampleResourceFile, "../../../src/external/resources/momime.server.database/Original Master of Magic 1.31 rules.momime.xml");
		
		final CommonDatabaseImpl serverDB = (CommonDatabaseImpl) unmarshaller.unmarshal (serverXmlFile);
		serverDB.buildMaps ();
		return serverDB;
	}

	/**
	 * @return Location of server XML to test with
	 * @throws IOException If we are unable to locate the server XML file
	 */
	public final File locateServerXmlFile () throws IOException
	{
		// Not straightforward to find this, because its in src/external/resources so isn't on the classpath
		// So instead find something that is on the classpath of the MoMIMEServer project, then modify that location
		final URL exampleResource = getClass ().getResource ("/MoMIMEServerVersion.properties");
		assertNotNull (exampleResource, "MoMIMEServerVersion.properties could not be found on classpath");
		
		final File exampleResourceFile = new File (exampleResource.getFile ());
		return new File (exampleResourceFile, "../../../src/external/resources/momime.server.database/Original Master of Magic 1.31 rules.momime.xml");
	}
	
	/**
	 * @param db Server database loaded from XML
	 * @param overlandMapSizeID Overland map size to use
	 * @param landProportionID Land proportion to use
	 * @param nodeStrengthID Node strength to use
	 * @param difficultyLevelID Difficulty level to use
	 * @param fogOfWarSettingID Fog of war settings to use
	 * @param unitSettingID Unit settings to use
	 * @param spellSettingID Spell settings to use
	 * @return Session description, built from selecting the specified parts from the server database
	 * @throws RecordNotFoundException If we request an entry that can't be found in the database
	 */
	public final MomSessionDescription createMomSessionDescription (final CommonDatabase db,
		final String overlandMapSizeID, final String landProportionID, final String nodeStrengthID, final String difficultyLevelID,
		final String fogOfWarSettingID, final String unitSettingID, final String spellSettingID)
		throws RecordNotFoundException
	{
		final MomSessionDescription sd = new MomSessionDescription ();

		for (final OverlandMapSize mapSize : db.getOverlandMapSize ())
			if (mapSize.getOverlandMapSizeID ().equals (overlandMapSizeID))
				sd.setOverlandMapSize (mapSize);
		if (sd.getOverlandMapSize () == null)
			throw new RecordNotFoundException (OverlandMapSize.class.getName (), overlandMapSizeID, "createMomSessionDescription");

		sd.setCombatMapSize (createCombatMapSize ());
		
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

		for (final DifficultyLevel src : db.getDifficultyLevel ())
			if (src.getDifficultyLevelID ().equals (difficultyLevelID))
			{
				// Copy the difficulty level, so we can reduce the difficulty level-node strength records to only those matching the requested node strength
				final DifficultyLevel dest = new DifficultyLevel ();
			    dest.setHumanSpellPicks				(src.getHumanSpellPicks ());
			    dest.setAiSpellPicks					(src.getAiSpellPicks ());
			    dest.setHumanStartingGold			(src.getHumanStartingGold ());
			    dest.setAiStartingGold					(src.getAiStartingGold ());
			    dest.setCustomWizards				(src.isCustomWizards ());
			    dest.setTowerMonstersMinimum	(src.getTowerMonstersMinimum ());
			    dest.setTowerMonstersMaximum	(src.getTowerMonstersMaximum ());
			    dest.setTowerTreasureMinimum	(src.getTowerTreasureMinimum ());
			    dest.setTowerTreasureMaximum	(src.getTowerTreasureMaximum ());
			    dest.setRaiderCityStartSizeMin		(src.getRaiderCityStartSizeMin ());
			    dest.setRaiderCityStartSizeMax	(src.getRaiderCityStartSizeMax ());
			    dest.setRaiderCityGrowthCap		(src.getRaiderCityGrowthCap ());
			    dest.setWizardCityStartSize			(src.getWizardCityStartSize ());
			    dest.setCityMaxSize					(src.getCityMaxSize ());
			    dest.setDifficultyLevelID				(src.getDifficultyLevelID ());

			    dest.getDifficultyLevelDescription	().addAll (src.getDifficultyLevelDescription ());
			    dest.getDifficultyLevelPlane ().addAll (src.getDifficultyLevelPlane ());
				sd.setDifficultyLevel (dest);
				
				// Copy only the relevant difficulty level-node strength records
				for (final DifficultyLevelNodeStrength ns : src.getDifficultyLevelNodeStrength ())
					if (ns.getNodeStrengthID ().equals (nodeStrengthID))
						dest.getDifficultyLevelNodeStrength ().add (ns);
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
	public final CoordinateSystem createOverlandMapCoordinateSystem ()
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
	public final OverlandMapSize createOverlandMapSize ()
	{
		final OverlandMapSize sys = new OverlandMapSize ();
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
	public final CoordinateSystem createCombatMapCoordinateSystem ()
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
	public final CombatMapSize createCombatMapSize ()
	{
		final CombatMapSize sys = new CombatMapSize ();
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
	public final MapVolumeOfFogOfWarStates createFogOfWarArea (final CoordinateSystem sys)
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
	public final MapVolumeOfStrings createStringsVolume (final CoordinateSystem sys)
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
	public final MapVolumeOfMemoryGridCells createOverlandMap (final CoordinateSystem sys)
	{
		final MapVolumeOfMemoryGridCells map = new MapVolumeOfMemoryGridCells ();
		for (int plane = 0; plane < sys.getDepth (); plane++)
		{
			final MapAreaOfMemoryGridCells area = new MapAreaOfMemoryGridCells ();
			for (int y = 0; y < sys.getHeight (); y++)
			{
				final MapRowOfMemoryGridCells row = new MapRowOfMemoryGridCells ();
				for (int x = 0; x < sys.getWidth (); x++)
					row.getCell ().add (new ServerGridCellEx ());

				area.getRow ().add (row);
			}

			map.getPlane ().add (area);
		}

		return map;
	}

	/**
	 * @return Map area prepopulated with empty cells
	 */
	public final MapAreaOfCombatTiles createCombatMap ()
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