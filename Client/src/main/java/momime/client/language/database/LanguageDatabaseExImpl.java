package momime.client.language.database;

import java.util.HashMap;
import java.util.Map;

import momime.client.language.database.v0_9_5.DifficultyLevel;
import momime.client.language.database.v0_9_5.FogOfWarSetting;
import momime.client.language.database.v0_9_5.LandProportion;
import momime.client.language.database.v0_9_5.LanguageCategory;
import momime.client.language.database.v0_9_5.LanguageDatabase;
import momime.client.language.database.v0_9_5.MapSize;
import momime.client.language.database.v0_9_5.NodeStrength;
import momime.client.language.database.v0_9_5.SpellSetting;
import momime.client.language.database.v0_9_5.UnitSetting;
import momime.client.language.database.v0_9_5.Wizard;

/**
 * Implementation of language XML database - extends stubs auto-generated from XSD to add additional functionality from the interface
 */
public final class LanguageDatabaseExImpl extends LanguageDatabase implements LanguageDatabaseEx
{
	/** Map of wizard IDs to wizard objects */
	private Map<String, Wizard> wizardsMap;
	
	/** Map of map size IDs to map size objects */
	private Map<String, MapSize> mapSizesMap;
	
	/** Map of land proportion IDs to land proportion objects */
	private Map<String, LandProportion> landProportionsMap;
	
	/** Map of node strength IDs to node strength objects */
	private Map<String, NodeStrength> nodeStrengthsMap;
	
	/** Map of difficulty level IDs to difficulty level objects */
	private Map<String, DifficultyLevel> difficultyLevelsMap;
	
	/** Map of fog of war setting IDs to fog of war settings objects */
	private Map<String, FogOfWarSetting> fogOfWarSettingsMap;
	
	/** Map of unit setting IDs to unit settings objects */
	private Map<String, UnitSetting> unitSettingsMap;
	
	/** Map of spell setting IDs to spell settings objects */
	private Map<String, SpellSetting> spellSettingsMap;

	/** Map of category IDs to category objects */
	private Map<String, LanguageCategoryEx> categoriesMap;

	/**
	 * Builds all the hash maps to enable finding records faster
	 */
	public final void buildMaps ()
	{
		// Create wizards map
		wizardsMap = new HashMap<String, Wizard> ();
		for (final Wizard thisWizard : getWizard ())
			wizardsMap.put (thisWizard.getWizardID (), thisWizard);

		// Create map sizes map
		mapSizesMap = new HashMap<String, MapSize> ();
		for (final MapSize thisMapSize : getMapSize ())
			mapSizesMap.put (thisMapSize.getMapSizeID (), thisMapSize);
		
		// Create land proportions map
		landProportionsMap = new HashMap<String, LandProportion> ();
		for (final LandProportion thisLandProportion : getLandProportion ())
			landProportionsMap.put (thisLandProportion.getLandProportionID (), thisLandProportion);

		// Create node strengths map
		nodeStrengthsMap = new HashMap<String, NodeStrength> ();
		for (final NodeStrength thisNodeStrength : getNodeStrength ())
			nodeStrengthsMap.put (thisNodeStrength.getNodeStrengthID (), thisNodeStrength);
		
		// Create difficulty levels map
		difficultyLevelsMap = new HashMap<String, DifficultyLevel> ();
		for (final DifficultyLevel thisDifficultyLevel : getDifficultyLevel ())
			difficultyLevelsMap.put (thisDifficultyLevel.getDifficultyLevelID (), thisDifficultyLevel);

		// Create fog of war settings map
		fogOfWarSettingsMap = new HashMap<String, FogOfWarSetting> ();
		for (final FogOfWarSetting thisFogOfWarSetting : getFogOfWarSetting ())
			fogOfWarSettingsMap.put (thisFogOfWarSetting.getFogOfWarSettingID (), thisFogOfWarSetting);
		
		// Create unit settings map
		unitSettingsMap = new HashMap<String, UnitSetting> ();
		for (final UnitSetting thisUnitSetting : getUnitSetting ())
			unitSettingsMap.put (thisUnitSetting.getUnitSettingID (), thisUnitSetting);
		
		// Create spell settings map
		spellSettingsMap = new HashMap<String, SpellSetting> ();
		for (final SpellSetting thisSpellSetting : getSpellSetting ())
			spellSettingsMap.put (thisSpellSetting.getSpellSettingID (), thisSpellSetting);
		
		// Create categories map
		categoriesMap = new HashMap<String, LanguageCategoryEx> ();
		for (final LanguageCategory thisCategory : getLanguageCategory ())
		{
			final LanguageCategoryEx catEx = (LanguageCategoryEx) thisCategory;
			catEx.buildMap ();
			categoriesMap.put (thisCategory.getLanguageCategoryID (), catEx);
		}
	}	

	/**
	 * @param wizardID Wizard ID to search for
	 * @return Wizard name; or replays back the ID if no description exists
	 */
	@Override
	public final String findWizardName (final String wizardID)
	{
		final Wizard thisWizard = wizardsMap.get (wizardID);
		return (thisWizard == null) ? wizardID : thisWizard.getWizardName ();
	}
	
	/**
	 * @param mapSizeID Map size ID to search for
	 * @return Map size description; or replays back the ID if no description exists
	 */
	@Override
	public final String findMapSizeDescription (final String mapSizeID)
	{
		final MapSize thisMapSize = mapSizesMap.get (mapSizeID);
		return (thisMapSize == null) ? mapSizeID : thisMapSize.getMapSizeDescription ();
	}
	
	/**
	 * @param landProportionID Land proportion ID to search for
	 * @return Land proportion description; or replays back the ID if no description exists
	 */
	@Override
	public final String findLandProportionDescription (final String landProportionID)
	{
		final LandProportion thisLandProportion = landProportionsMap.get (landProportionID);
		return (thisLandProportion == null) ? landProportionID : thisLandProportion.getLandProportionDescription ();
	}
	
	/**
	 * @param nodeStrengthID Node strength ID to search for
	 * @return Node strength description; or replays back the ID if no description exists
	 */
	@Override
	public final String findNodeStrengthDescription (final String nodeStrengthID)
	{
		final NodeStrength thisNodeStrength = nodeStrengthsMap.get (nodeStrengthID);
		return (thisNodeStrength == null) ? nodeStrengthID : thisNodeStrength.getNodeStrengthDescription ();
	}
	
	/**
	 * @param difficultyLevelID Difficulty level ID to search for
	 * @return Difficulty level description; or replays back the ID if no description exists
	 */
	@Override
	public final String findDifficultyLevelDescription (final String difficultyLevelID)
	{
		final DifficultyLevel thisDifficultyLevel = difficultyLevelsMap.get (difficultyLevelID);
		return (thisDifficultyLevel == null) ? difficultyLevelID : thisDifficultyLevel.getDifficultyLevelDescription ();
	}
	
	/**
	 * @param fogOfWarSettingID Fog of War setting ID to search for
	 * @return Fog of War setting description; or replays back the ID if no description exists
	 */
	@Override
	public final String findFogOfWarSettingDescription (final String fogOfWarSettingID)
	{
		final FogOfWarSetting thisFogOfWarSetting = fogOfWarSettingsMap.get (fogOfWarSettingID);
		return (thisFogOfWarSetting == null) ? fogOfWarSettingID : thisFogOfWarSetting.getFogOfWarSettingDescription ();
	}
	
	/**
	 * @param unitSettingID Unit setting ID to search for
	 * @return Unit setting description; or replays back the ID if no description exists
	 */
	@Override
	public final String findUnitSettingDescription (final String unitSettingID)
	{
		final UnitSetting thisUnitSetting = unitSettingsMap.get (unitSettingID);
		return (thisUnitSetting == null) ? unitSettingID : thisUnitSetting.getUnitSettingDescription ();
	}
	
	/**
	 * @param spellSettingID Spell setting ID to search for
	 * @return Spell setting description; or replays back the ID if no description exists
	 */
	@Override
	public final String findSpellSettingDescription (final String spellSettingID)
	{
		final SpellSetting thisSpellSetting = spellSettingsMap.get (spellSettingID);
		return (thisSpellSetting == null) ? spellSettingID : thisSpellSetting.getSpellSettingDescription ();
	}
	
	/**
	 * @param languageCategoryID Category ID to search for
	 * @param languageEntryID Entry ID to search for
	 * @return Text of the requested language entry; or replays the key back if the category or entry doesn't exist
	 */
	@Override
	public final String findCategoryEntry (final String languageCategoryID, final String languageEntryID)
	{
		final LanguageCategoryEx cat = categoriesMap.get (languageCategoryID);
		final String entry = (cat == null) ? null : cat.findEntry (languageEntryID);
		return (entry == null) ? (languageCategoryID + "/" + languageEntryID) : entry;
	}
}
