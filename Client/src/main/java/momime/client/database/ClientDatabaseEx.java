package momime.client.database;

import java.util.List;

import momime.client.database.v0_9_5.MapFeature;
import momime.client.database.v0_9_5.Wizard;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;

/**
 * Adds client-side specific extensions to the common database lookup class
 */
public interface ClientDatabaseEx extends CommonDatabase
{
	/**
	 * @param mapFeatureID Map feature ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Map feature object
	 * @throws RecordNotFoundException If the mapFeatureID doesn't exist
	 */
	@Override
	public MapFeature findMapFeature (final String mapFeatureID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @return Complete list of all wizards in game
	 */
	@Override
	public List<Wizard> getWizard ();
	
	/**
	 * @return Cost to construct the most expensive unit or building in the database
	 */
	public int getMostExpensiveConstructionCost ();
}