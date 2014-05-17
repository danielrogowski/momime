package momime.client.language.database;

import java.util.List;

import momime.client.language.database.v0_9_5.CitySize;
import momime.client.language.database.v0_9_5.KnownServer;
import momime.client.language.database.v0_9_5.Pick;
import momime.client.language.database.v0_9_5.Plane;
import momime.client.language.database.v0_9_5.Race;
import momime.client.language.database.v0_9_5.Spell;

/**
 * Describes operations that we need to support over the language XML file
 */
public interface LanguageDatabaseEx
{
	/**
	 * @param planeNumber Plane number to search for
	 * @return Plane descriptions object; or null if not found
	 */
	public Plane findPlane (final int planeNumber);

	/**
	 * @param pickID Pick ID to search for
	 * @return Pick descriptions object; or null if not found
	 */
	public Pick findPick (final String pickID);
	
	/**
	 * @param wizardID Wizard ID to search for
	 * @return Wizard name; or replays back the ID if no description exists
	 */
	public String findWizardName (final String wizardID);

	/**
	 * @param raceID Race ID to search for
	 * @return Race descriptions object; or null if not found
	 */
	public Race findRace (final String raceID);

	/**
	 * @param citySizeID City size ID to search for
	 * @return City size descriptions object; or null if not found
	 */
	public CitySize findCitySize (final String citySizeID);
	
	/**
	 * @param spellRankID Spell rank ID to search for
	 * @return Spell rank description; or replays back the ID if no description exists
	 */
	public String findSpellRankDescription (final String spellRankID);
	
	/**
	 * @param spellID Spell ID to search for
	 * @return Spell descriptions object; or null if not found
	 */
	public Spell findSpell (final String spellID);
	
	/**
	 * @param mapSizeID Map size ID to search for
	 * @return Map size description; or replays back the ID if no description exists
	 */
	public String findMapSizeDescription (final String mapSizeID);
	
	/**
	 * @param landProportionID Land proportion ID to search for
	 * @return Land proportion description; or replays back the ID if no description exists
	 */
	public String findLandProportionDescription (final String landProportionID);
	
	/**
	 * @param nodeStrengthID Node strength ID to search for
	 * @return Node strength description; or replays back the ID if no description exists
	 */
	public String findNodeStrengthDescription (final String nodeStrengthID);
	
	/**
	 * @param difficultyLevelID Difficulty level ID to search for
	 * @return Difficulty level description; or replays back the ID if no description exists
	 */
	public String findDifficultyLevelDescription (final String difficultyLevelID);
	
	/**
	 * @param fogOfWarSettingID Fog of War setting ID to search for
	 * @return Fog of War setting description; or replays back the ID if no description exists
	 */
	public String findFogOfWarSettingDescription (final String fogOfWarSettingID);
	
	/**
	 * @param unitSettingID Unit setting ID to search for
	 * @return Unit setting description; or replays back the ID if no description exists
	 */
	public String findUnitSettingDescription (final String unitSettingID);
	
	/**
	 * @param spellSettingID Spell setting ID to search for
	 * @return Spell setting description; or replays back the ID if no description exists
	 */
	public String findSpellSettingDescription (final String spellSettingID);
	
	/**
	 * @param languageCategoryID Category ID to search for
	 * @param languageEntryID Entry ID to search for
	 * @return Text of the requested language entry; or replays the key back if the category or entry doesn't exist
	 */
	public String findCategoryEntry (final String languageCategoryID, final String languageEntryID);
	
	/**
	 * @return List of all known servers
	 */
	public List<KnownServer> getKnownServer ();
}
