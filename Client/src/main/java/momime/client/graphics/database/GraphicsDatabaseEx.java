package momime.client.graphics.database;

import momime.client.graphics.database.v0_9_5.Pick;
import momime.client.graphics.database.v0_9_5.Wizard;
import momime.common.database.RecordNotFoundException;

/**
 * Describes operations that we need to support over the graphics XML file
 */
public interface GraphicsDatabaseEx
{
	/**
	 * @param pickID Pick ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Pick object
	 * @throws RecordNotFoundException If the pickID doesn't exist
	 */
	public Pick findPick (final String pickID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @param wizardID Wizard ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Wizard object
	 * @throws RecordNotFoundException If the wizardID doesn't exist
	 */
	public Wizard findWizard (final String wizardID, final String caller) throws RecordNotFoundException;
}
