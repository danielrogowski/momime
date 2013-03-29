package momime.server.database;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Validator;

import momime.client.database.v0_9_4.AvailableDatabase;
import momime.client.database.v0_9_4.ClientDatabase;
import momime.client.database.v0_9_4.NewGameDatabase;
import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.MapFeatureProduction;
import momime.common.database.v0_9_4.TaxRate;
import momime.common.database.v0_9_4.WizardPick;
import momime.common.messages.servertoclient.v0_9_4.NewGameDatabaseMessage;
import momime.server.database.v0_9_4.Building;
import momime.server.database.v0_9_4.CombatAreaEffect;
import momime.server.database.v0_9_4.CombatTileBorder;
import momime.server.database.v0_9_4.CombatTileType;
import momime.server.database.v0_9_4.DifficultyLevel;
import momime.server.database.v0_9_4.FogOfWarSetting;
import momime.server.database.v0_9_4.LandProportion;
import momime.server.database.v0_9_4.MapSize;
import momime.server.database.v0_9_4.NodeStrength;
import momime.server.database.v0_9_4.Pick;
import momime.server.database.v0_9_4.PickType;
import momime.server.database.v0_9_4.Plane;
import momime.server.database.v0_9_4.ProductionType;
import momime.server.database.v0_9_4.Race;
import momime.server.database.v0_9_4.RangedAttackType;
import momime.server.database.v0_9_4.Spell;
import momime.server.database.v0_9_4.SpellSetting;
import momime.server.database.v0_9_4.TileType;
import momime.server.database.v0_9_4.Unit;
import momime.server.database.v0_9_4.UnitAttribute;
import momime.server.database.v0_9_4.UnitMagicRealm;
import momime.server.database.v0_9_4.UnitSetting;
import momime.server.database.v0_9_4.UnitSkill;
import momime.server.database.v0_9_4.UnitType;
import momime.server.database.v0_9_4.WeaponGrade;
import momime.server.database.v0_9_4.WizardPickCount;

import org.xml.sax.SAXException;

/**
 * Converters for building derivative XML files from the server XML file
 * Old Delphi unit: MomServerDB.pas
 */
public final class ServerDatabaseConverters
{
	/** Extension that XML files for the server must have */
	public static final String SERVER_XML_FILE_EXTENSION = ".Master of Magic Server.xml";

	/**
	 * @param src Server XML to extract from
	 * @param dbName Filename the server XML was read from
	 * @return Info extracted from server XML
	 */
	private final static AvailableDatabase convertServerToAvailableDatabase (final ServerDatabaseEx src, final String dbName)
	{
		final AvailableDatabase dest = new AvailableDatabase ();
		dest.setDbName (dbName);

		for (final MapSize mapSize : src.getMapSize ())
			dest.getMapSize ().add (mapSize);

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
	 * @param xsdValidator XSD validator created for the server XSD file
	 * @param serverDatabaseContext JAXB Context for loading server XML files
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Info extracted from all available XML databases
	 * @throws JAXBException If there is a problem creating the server XML unmarshaller
	 * @throws MomException If there are no compatible server XML databases
	 */
	public final static NewGameDatabaseMessage buildNewGameDatabase (final File xmlFolder, final Validator xsdValidator, final JAXBContext serverDatabaseContext, final Logger debugLogger)
		throws JAXBException, MomException
	{
		debugLogger.entering (ServerDatabaseConverters.class.getName (), "buildNewGameDatabase");

		// First put the list of all compatibly named XML databases in a string list, so we can sort it before we start trying to load them in and check them
		// Strip the suffix off each one as we add it
		debugLogger.finest ("buildNewGameDatabase searching for XML files in \"" + xmlFolder + "\"");
		final File [] xmlFiles = xmlFolder.listFiles (new SuffixFilenameFilter (SERVER_XML_FILE_EXTENSION));
		if (xmlFiles == null)
			throw new MomException ("Unable to search folder \"" + xmlFolder + "\" for server XML files - check path specified in config file is valid");

		final List<String> xmlFilenames = new ArrayList<String> ();
		for (final File thisFile : xmlFiles)
		{
			String thisFilename = thisFile.getName ();
			thisFilename = thisFilename.substring (0, thisFilename.length () - SERVER_XML_FILE_EXTENSION.length ());

			debugLogger.finest ("buildNewGameDatabase found suitably named XML file \"" + thisFilename + "\"");

			xmlFilenames.add (thisFilename);
		}

		// Sort the filenames
		Collections.sort (xmlFilenames);

		// Now open up each one to check if it is compatible
		final NewGameDatabase newGameDatabase = new NewGameDatabase ();
		final Unmarshaller serverDatabaseUnmarshaller = serverDatabaseContext.createUnmarshaller ();
		serverDatabaseUnmarshaller.setProperty ("com.sun.xml.bind.ObjectFactory", new Object [] {new ServerDatabaseFactory ()});

		for (final String thisFilename : xmlFilenames)
		{
			final File fullFilename = new File (xmlFolder, thisFilename + SERVER_XML_FILE_EXTENSION);

			try
			{
				// Validate XML file against server XSD
				xsdValidator.validate (new StreamSource (fullFilename));

				// Looks ok - attempt to load it in
				final ServerDatabaseEx db = (ServerDatabaseEx) serverDatabaseUnmarshaller.unmarshal (fullFilename);
				db.buildMaps ();

				// Loaded ok, add relevant parts to the new game database
				newGameDatabase.getMomimeXmlDatabase ().add (convertServerToAvailableDatabase (db, thisFilename));
			}
			catch (final IOException e)
			{
				debugLogger.warning ("Server XML database \"" + thisFilename + "\" can't be used because of an error while trying to read it: " + e.getMessage ());
			}
			catch (final SAXException e)
			{
				debugLogger.warning ("Server XML database \"" + thisFilename + "\" can't be used because it doesn't validate against the XSD: " + e.getMessage ());
			}
			catch (final JAXBException e)
			{
				debugLogger.warning ("Server XML database \"" + thisFilename + "\" can't be used because it couldn't be loaded in correctly: " + e.getMessage ());
			}
		}

		if (newGameDatabase.getMomimeXmlDatabase ().size () == 0)
			throw new MomException ("No XML Databases found compatible with this version of MoM IME");

		// Wrap it in message
		final NewGameDatabaseMessage msg = new NewGameDatabaseMessage ();
		msg.setNewGameDatabase (newGameDatabase);

		debugLogger.exiting (ServerDatabaseConverters.class.getName (), "buildNewGameDatabase", newGameDatabase.getMomimeXmlDatabase ().size ());
		return msg;
	}

	/**
	 * @param src Server side database loaded from XML
	 * @param humanSpellPicks Number of picks human players get in this game, as per session description
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Info extracted from server XML
	 * @throws RecordNotFoundException If one of the wizards does not have picks for the specified number of human picks defined
	 */
	public final static ClientDatabase buildClientDatabase (final ServerDatabaseEx src, final int humanSpellPicks, final Logger debugLogger) throws RecordNotFoundException
	{
		debugLogger.exiting (ServerDatabaseConverters.class.getName (), "buildClientDatabase", src);

		final ClientDatabase dest = new ClientDatabase ();

		for (final Plane plane : src.getPlane ())
			dest.getPlane ().add (plane);

		for (final ProductionType productionType : src.getProductionType ())
			dest.getProductionType ().add (productionType);

		for (final TileType tileType : src.getTileType ())
			dest.getTileType ().add (tileType);

		for (final PickType pickType : src.getPickType ())
			dest.getPickType ().add (pickType);

		for (final Pick pick : src.getPick ())
			dest.getPick ().add (pick);

		for (final Race race : src.getRace ())
			dest.getRace ().add (race);

		for (final TaxRate taxRate : src.getTaxRate ())
			dest.getTaxRate ().add (taxRate);

		for (final Building building : src.getBuilding ())
			dest.getBuilding ().add (building);

		for (final UnitAttribute unitAttribute : src.getUnitAttribute ())
			dest.getUnitAttribute ().add (unitAttribute);

		for (final UnitType unitType : src.getUnitType ())
			dest.getUnitType ().add (unitType);

		for (final UnitSkill unitSkill : src.getUnitSkill ())
			dest.getUnitSkill ().add (unitSkill);

		for (final RangedAttackType rangedAttackType : src.getRangedAttackType ())
			dest.getRangedAttackType ().add (rangedAttackType);

		for (final Unit unit : src.getUnit ())
			dest.getUnit ().add (unit);

		for (final WeaponGrade weaponGrade : src.getWeaponGrade ())
			dest.getWeaponGrade ().add (weaponGrade);

		for (final CombatAreaEffect combatAreaEffect : src.getCombatAreaEffect ())
			dest.getCombatAreaEffect ().add (combatAreaEffect);

		for (final Spell spell : src.getSpell ())
			dest.getSpell ().add (spell);

		for (final UnitMagicRealm unitMagicRealm : src.getUnitMagicRealm ())
			dest.getUnitMagicRealm ().add (unitMagicRealm);

		for (final CombatTileType combatTileType : src.getCombatTileType ())
			dest.getCombatTileType ().add (combatTileType);

		for (final CombatTileBorder combatTileBorder : src.getCombatTileBorder ())
			dest.getCombatTileBorder ().add (combatTileBorder);

	    // Derive client-side only flag for map features
		for (final momime.server.database.v0_9_4.MapFeature srcMapFeature : src.getMapFeature ())
		{
			final momime.client.database.v0_9_4.MapFeature destMapFeature = new momime.client.database.v0_9_4.MapFeature ();

			destMapFeature.setMapFeatureID (srcMapFeature.getMapFeatureID ());
			destMapFeature.setCanBuildCity (srcMapFeature.isCanBuildCity ());
			destMapFeature.setFeatureSpellProtection (srcMapFeature.isFeatureSpellProtection ());
			destMapFeature.setFeatureMagicWeapons (srcMapFeature.getFeatureMagicWeapons ());
			destMapFeature.setRaceMineralMultiplerApplies (srcMapFeature.isRaceMineralMultiplerApplies ());

			for (final MapFeatureProduction mapFeatureProduction : srcMapFeature.getMapFeatureProduction ())
				destMapFeature.getMapFeatureProduction ().add (mapFeatureProduction);

			destMapFeature.setAnyMagicRealmsDefined (srcMapFeature.getMapFeatureMagicRealm ().size () > 0);

		    dest.getMapFeature ().add (destMapFeature);
		}

	    // Select correct number of picks for wizards
		for (final momime.server.database.v0_9_4.Wizard srcWizard : src.getWizard ())
		{
			final momime.client.database.v0_9_4.Wizard destWizard = new momime.client.database.v0_9_4.Wizard ();

			destWizard.setWizardID (srcWizard.getWizardID ());

			if ((!srcWizard.getWizardID ().equals (CommonDatabaseConstants.WIZARD_ID_MONSTERS)) &&
				(!srcWizard.getWizardID ().equals (CommonDatabaseConstants.WIZARD_ID_RAIDERS)))
			{
				final Iterator<WizardPickCount> iter = srcWizard.getWizardPickCount ().iterator ();
				boolean found = false;
				while ((!found) && (iter.hasNext ()))
				{
					final WizardPickCount pickCount = iter.next ();
					if (pickCount.getPickCount () == humanSpellPicks)
					{
						found = true;
						for (final WizardPick pick : pickCount.getWizardPick ())
							destWizard.getWizardPick ().add (pick);
					}
				}

				if (!found)
					throw new RecordNotFoundException (WizardPickCount.class.getName (), srcWizard.getWizardID () + "-" + humanSpellPicks, "buildClientDatabase");
			}

			dest.getWizard ().add (destWizard);
		}

		debugLogger.exiting (ServerDatabaseConverters.class.getName (), "buildClientDatabase", dest);
		return dest;
	}

	/**
	 * Prevent instatiation
	 */
	private ServerDatabaseConverters ()
	{
	}
}
