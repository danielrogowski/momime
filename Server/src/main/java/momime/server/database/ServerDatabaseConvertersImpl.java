package momime.server.database;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import momime.client.database.AvailableDatabase;
import momime.client.database.ClientDatabase;
import momime.client.database.NewGameDatabase;
import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.MapFeatureProduction;
import momime.common.database.RecordNotFoundException;
import momime.common.database.TaxRate;
import momime.common.database.WizardPick;
import momime.common.messages.servertoclient.NewGameDatabaseMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Converters for building derivative XML files from the server XML file
 * Old Delphi unit: MomServerDB.pas
 */
public final class ServerDatabaseConvertersImpl implements ServerDatabaseConverters
{
	/** Class logger */
	private final Log log = LogFactory.getLog (ServerDatabaseConvertersImpl.class);
	
	/** Extension that XML files for the server must have */
	public static final String SERVER_XML_FILE_EXTENSION = ".Master of Magic Server.xml";

	/**
	 * @param src Server XML to extract from
	 * @param dbName Filename the server XML was read from
	 * @return Info extracted from server XML
	 */
	private final AvailableDatabase convertServerToAvailableDatabase (final ServerDatabaseEx src, final String dbName)
	{
		final AvailableDatabase dest = new AvailableDatabase ();
		dest.setDbName (dbName);
		dest.setNewGameDefaults (src.getNewGameDefaults ());

		for (final OverlandMapSizeSvr overlandMapSize : src.getOverlandMapSizes ())
			dest.getOverlandMapSize ().add (overlandMapSize);

		for (final LandProportionSvr landProportion : src.getLandProportions ())
			dest.getLandProportion ().add (landProportion);

		for (final NodeStrengthSvr nodeStrength : src.getNodeStrengths ())
			dest.getNodeStrength ().add (nodeStrength);

		for (final DifficultyLevelSvr difficultyLevel : src.getDifficultyLevels ())
			dest.getDifficultyLevel ().add (difficultyLevel);

		for (final FogOfWarSettingSvr fogOfWarSetting : src.getFogOfWarSettings ())
			dest.getFogOfWarSetting ().add (fogOfWarSetting);

		for (final UnitSettingSvr unitSetting : src.getUnitSettings ())
			dest.getUnitSetting ().add (unitSetting);

		for (final SpellSettingSvr spellSetting : src.getSpellSettings ())
			dest.getSpellSetting ().add (spellSetting);

		return dest;
	}

	/**
	 * Finds all the compatible (i.e. correct namespace) XML databases on the server and extracts a small portion of each needed for setting up new games
	 * @param xmlFolder Folder in which to look for server XML files, e.g. F:\Workspaces\Delphi\Master of Magic\XML Files\Server\
	 * @param serverDatabaseUnmarshaller JAXB Unmarshaller for loading server XML files
	 * @return Info extracted from all available XML databases
	 * @throws JAXBException If there is a problem creating the server XML unmarshaller
	 * @throws MomException If there are no compatible server XML databases
	 * @throws IOException If there is a problem reading the XML databases
	 */
	@Override
	public final NewGameDatabaseMessage buildNewGameDatabase (final File xmlFolder, final Unmarshaller serverDatabaseUnmarshaller)
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
		final NewGameDatabaseMessage msg = buildNewGameDatabase (map, serverDatabaseUnmarshaller);
		
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
	 * @param serverDatabaseUnmarshaller JAXB Unmarshaller for loading server XML files
	 * @return Info extracted from all available XML databases
	 * @throws JAXBException If there is a problem creating the server XML unmarshaller
	 * @throws MomException If there are no compatible server XML databases
	 */
	final NewGameDatabaseMessage buildNewGameDatabase (final Map<String, File> xmlFiles, final Unmarshaller serverDatabaseUnmarshaller)
		throws JAXBException, MomException
	{
		log.trace ("Entering buildNewGameDatabase");

		// Now open up each one to check if it is compatible
		final NewGameDatabase newGameDatabase = new NewGameDatabase ();

		for (final Entry<String, File> thisXmlFile : xmlFiles.entrySet ())
			try
			{
				// Attempt to load it in
				final ServerDatabaseExImpl db = (ServerDatabaseExImpl) serverDatabaseUnmarshaller.unmarshal (thisXmlFile.getValue ());
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

	/**
	 * @param src Server side database loaded from XML
	 * @param humanSpellPicks Number of picks human players get in this game, as per session description
	 * @return Info extracted from server XML
	 * @throws RecordNotFoundException If one of the wizards does not have picks for the specified number of human picks defined
	 */
	@Override
	public final ClientDatabase buildClientDatabase (final ServerDatabaseEx src, final int humanSpellPicks) throws RecordNotFoundException
	{
		log.trace ("Exiting buildClientDatabase");

		final ClientDatabase dest = new ClientDatabase ();

		for (final PlaneSvr plane : src.getPlanes ())
			dest.getPlane ().add (plane);

		for (final ProductionTypeSvr productionType : src.getProductionTypes ())
			dest.getProductionType ().add (productionType);

		for (final TileTypeSvr tileType : src.getTileTypes ())
			dest.getTileType ().add (tileType);

		for (final PickTypeSvr pickType : src.getPickTypes ())
			dest.getPickType ().add (pickType);

		for (final PickSvr pick : src.getPicks ())
			dest.getPick ().add (pick);

		for (final RaceSvr race : src.getRaces ())
			dest.getRace ().add (race);

		for (final TaxRate taxRate : src.getTaxRate ())
			dest.getTaxRate ().add (taxRate);

		for (final BuildingSvr building : src.getBuildings ())
			dest.getBuilding ().add (building);

		for (final UnitTypeSvr unitType : src.getUnitTypes ())
			dest.getUnitType ().add (unitType);

		for (final UnitSkillSvr unitSkill : src.getUnitSkills ())
			dest.getUnitSkill ().add (unitSkill);

		for (final RangedAttackTypeSvr rangedAttackType : src.getRangedAttackTypes ())
			dest.getRangedAttackType ().add (rangedAttackType);

		for (final UnitSvr unit : src.getUnits ())
			dest.getUnit ().add (unit);

		for (final WeaponGradeSvr weaponGrade : src.getWeaponGrades ())
			dest.getWeaponGrade ().add (weaponGrade);

		for (final CombatAreaEffectSvr combatAreaEffect : src.getCombatAreaEffects ())
			dest.getCombatAreaEffect ().add (combatAreaEffect);

		for (final SpellSvr spell : src.getSpells ())
			dest.getSpell ().add (spell);

		for (final UnitMagicRealmSvr unitMagicRealm : src.getUnitMagicRealms ())
			dest.getUnitMagicRealm ().add (unitMagicRealm);

		for (final CombatTileTypeSvr combatTileType : src.getCombatTileTypes ())
			dest.getCombatTileType ().add (combatTileType);

		for (final CombatTileBorderSvr combatTileBorder : src.getCombatTileBorders ())
			dest.getCombatTileBorder ().add (combatTileBorder);
		
		dest.getMovementRateRule ().addAll (src.getMovementRateRule ());

	    // Derive client-side only flag for map features
		for (final MapFeatureSvr srcMapFeature : src.getMapFeatures ())
		{
			final momime.client.database.MapFeature destMapFeature = new momime.client.database.MapFeature ();

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
		for (final WizardSvr srcWizard : src.getWizards ())
		{
			final momime.client.database.Wizard destWizard = new momime.client.database.Wizard ();

			destWizard.setWizardID (srcWizard.getWizardID ());

			if ((!srcWizard.getWizardID ().equals (CommonDatabaseConstants.WIZARD_ID_MONSTERS)) &&
				(!srcWizard.getWizardID ().equals (CommonDatabaseConstants.WIZARD_ID_RAIDERS)))
			{
				final Iterator<WizardPickCountSvr> iter = srcWizard.getWizardPickCounts ().iterator ();
				boolean found = false;
				while ((!found) && (iter.hasNext ()))
				{
					final WizardPickCountSvr pickCount = iter.next ();
					if (pickCount.getPickCount () == humanSpellPicks)
					{
						found = true;
						for (final WizardPick pick : pickCount.getWizardPick ())
							destWizard.getWizardPick ().add (pick);
					}
				}

				if (!found)
					throw new RecordNotFoundException (WizardPickCountSvr.class, srcWizard.getWizardID () + "-" + humanSpellPicks, "buildClientDatabase");
			}

			dest.getWizard ().add (destWizard);
		}

		log.trace ("Exiting buildClientDatabase");
		return dest;
	}
}