package momime.server.database;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import momime.client.database.AvailableDatabase;
import momime.client.database.NewGameDatabase;
import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseImpl;
import momime.common.database.DifficultyLevel;
import momime.common.database.FogOfWarSetting;
import momime.common.database.LandProportion;
import momime.common.database.NodeStrength;
import momime.common.database.OverlandMapSize;
import momime.common.database.SpellSetting;
import momime.common.database.UnitSetting;
import momime.common.messages.servertoclient.NewGameDatabaseMessage;

/**
 * Converters for building derivative XML files from the server XML file
 * Old Delphi unit: MomServerDB.pas
 */
public final class ServerDatabaseConvertersImpl implements ServerDatabaseConverters
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (ServerDatabaseConvertersImpl.class);
	
	/** Extension that XML files for the server must have */
	public static final String SERVER_XML_FILE_EXTENSION = ".momime.xml";

	/**
	 * @param src Server XML to extract from
	 * @param dbName Filename the server XML was read from
	 * @return Info extracted from server XML
	 */
	private final AvailableDatabase convertServerToAvailableDatabase (final CommonDatabase src, final String dbName)
	{
		final AvailableDatabase dest = new AvailableDatabase ();
		dest.setDbName (dbName);
		dest.setNewGameDefaults (src.getNewGameDefaults ());

		for (final OverlandMapSize overlandMapSize : src.getOverlandMapSize ())
			dest.getOverlandMapSize ().add (overlandMapSize);

		for (final LandProportion landProportion : src.getLandProportion ())
			dest.getLandProportion ().add (landProportion);

		for (final NodeStrength nodeStrength : src.getNodeStrength ())
			dest.getNodeStrength ().add (nodeStrength);

		for (final DifficultyLevel difficultyLevel : src.getDifficultyLevel ())
			dest.getDifficultyLevel ().add (difficultyLevel);

		for (final FogOfWarSetting fogOfWarSetting : src.getFogOfWarSetting ())
			dest.getFogOfWarSetting ().add (fogOfWarSetting);

		for (final UnitSetting unitSetting : src.getUnitSetting ())
			dest.getUnitSetting ().add (unitSetting);

		for (final SpellSetting spellSetting : src.getSpellSetting ())
			dest.getSpellSetting ().add (spellSetting);

		return dest;
	}

	/**
	 * Finds all the compatible (i.e. correct namespace) XML databases on the server and extracts a small portion of each needed for setting up new games
	 * @param xmlFolder Folder in which to look for server XML files, e.g. F:\Workspaces\Delphi\Master of Magic\XML Files\Server\
	 * @param commonDatabaseUnmarshaller JAXB Unmarshaller for loading database XML files
	 * @return Info extracted from all available XML databases
	 * @throws JAXBException If there is a problem creating the server XML unmarshaller
	 * @throws MomException If there are no compatible server XML databases
	 * @throws IOException If there is a problem reading the XML databases
	 */
	@Override
	public final NewGameDatabaseMessage buildNewGameDatabase (final File xmlFolder, final Unmarshaller commonDatabaseUnmarshaller)
		throws JAXBException, MomException, IOException
	{
		log.trace ("Entering buildNewGameDatabase");

		// First put the list of all compatibly named XML databases in a string list, so we can sort it before we start trying to load them in and check them
		// Strip the suffix off each one as we add it
		log.info ("Generating list of all available server XML files in \"" + xmlFolder + "\"...");
		final File [] xmlFiles = xmlFolder.listFiles (new SuffixFilenameFilter (SERVER_XML_FILE_EXTENSION));
		if (xmlFiles == null)
			throw new MomException ("Unable to search folder \"" + xmlFolder + "\" for server XML files - check path specified in config file is valid");

		final List<String> xmlFilenames = new ArrayList<String> ();
		for (final File thisFile : xmlFiles)
		{
			String thisFilename = thisFile.getName ();
			thisFilename = thisFilename.substring (0, thisFilename.length () - SERVER_XML_FILE_EXTENSION.length ());

			log.debug ("buildNewGameDatabase found suitably named XML file \"" + thisFilename + "\"");

			xmlFilenames.add (thisFilename);
		}

		// Sort the filenames
		Collections.sort (xmlFilenames);
		
		// Put them into a map
		final Map<String, File> map = new HashMap<String, File> ();
		for (final String thisFilename : xmlFilenames)
		{
			final File thisFile = new File (xmlFolder, thisFilename + SERVER_XML_FILE_EXTENSION);
			map.put (thisFilename, thisFile);
		}
		
		// Call other method to do the guts of the work
		final NewGameDatabaseMessage msg = buildNewGameDatabase (map, commonDatabaseUnmarshaller);
		
		log.trace ("Exiting buildNewGameDatabase");
		return msg;
	}
	
	/**
	 * Finds all the compatible (i.e. correct namespace) XML databases on the server and extracts a small portion of each needed for setting up new games
	 * 
	 * The reason the input takes a URL is so that this can be tested from the MoMIMEServer project, when the XML file is in the MoMIMEServerDatabase project,
	 * so in a command line build there is no physical folder on disk to pass into the method signature declared in the interface.
	 * 
	 * @param xmlFiles Map of database names to URLs to locate them
	 * @param commonDatabaseUnmarshaller JAXB Unmarshaller for loading server XML files
	 * @return Info extracted from all available XML databases
	 * @throws JAXBException If there is a problem creating the server XML unmarshaller
	 * @throws MomException If there are no compatible server XML databases
	 */
	final NewGameDatabaseMessage buildNewGameDatabase (final Map<String, File> xmlFiles, final Unmarshaller commonDatabaseUnmarshaller)
		throws JAXBException, MomException
	{
		log.trace ("Entering buildNewGameDatabase");

		// Now open up each one to check if it is compatible
		final NewGameDatabase newGameDatabase = new NewGameDatabase ();

		for (final Entry<String, File> thisXmlFile : xmlFiles.entrySet ())
			try
			{
				// Attempt to load it in
				final CommonDatabaseImpl db = (CommonDatabaseImpl) commonDatabaseUnmarshaller.unmarshal (thisXmlFile.getValue ());
				db.buildMaps ();
				db.consistencyChecks ();

				// Loaded ok, add relevant parts to the new game database
				newGameDatabase.getMomimeXmlDatabase ().add (convertServerToAvailableDatabase (db, thisXmlFile.getKey ()));
			}
			catch (final Exception e)
			{
				log.warn ("Server XML database \"" + thisXmlFile.getKey () + "\" can't be used because of: " + e.getMessage ());
			}

		if (newGameDatabase.getMomimeXmlDatabase ().size () == 0)
			throw new MomException ("No XML Databases found compatible with this version of MoM IME");

		// Wrap it in message
		final NewGameDatabaseMessage msg = new NewGameDatabaseMessage ();
		msg.setNewGameDatabase (newGameDatabase);

		log.info ("Found " + newGameDatabase.getMomimeXmlDatabase ().size () + " compatible server XML file(s)");
		log.trace ("Exiting buildNewGameDatabase = " + newGameDatabase.getMomimeXmlDatabase ().size ());
		return msg;
	}
}