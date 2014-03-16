package momime.client.graphics.database;

import java.util.HashMap;
import java.util.Map;

import momime.client.graphics.database.v0_9_5.GraphicsDatabase;
import momime.client.graphics.database.v0_9_5.Pick;
import momime.client.graphics.database.v0_9_5.Wizard;
import momime.common.database.RecordNotFoundException;

/**
 * Implementation of graphics XML database - extends stubs auto-generated from XSD to add additional functionality from the interface
 */
public final class GraphicsDatabaseExImpl extends GraphicsDatabase implements GraphicsDatabaseEx
{
	/** Map of pick IDs to pick objects */
	private Map<String, Pick> picksMap;

	/** Map of wizard IDs to wizard objects */
	private Map<String, Wizard> wizardsMap;
	
	/**
	 * Builds all the hash maps to enable finding records faster
	 */
	public final void buildMaps ()
	{
		// Create picks map
		picksMap = new HashMap<String, Pick> ();
		for (final Pick thisPick : getPick ())
			picksMap.put (thisPick.getPickID (), thisPick);

		// Create wizards map
		wizardsMap = new HashMap<String, Wizard> ();
		for (final Wizard thisWizard : getWizard ())
			wizardsMap.put (thisWizard.getWizardID (), thisWizard);
	}

	/**
	 * @param pickID Pick ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Pick object
	 * @throws RecordNotFoundException If the pickID doesn't exist
	 */
	@Override
	public final Pick findPick (final String pickID, final String caller) throws RecordNotFoundException
	{
		final Pick found = picksMap.get (pickID);
		if (found == null)
			throw new RecordNotFoundException (Pick.class.getName (), pickID, caller);

		return found;
	}
	
	/**
	 * @param wizardID Wizard ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Wizard object
	 * @throws RecordNotFoundException If the wizardID doesn't exist
	 */
	@Override
	public final Wizard findWizard (final String wizardID, final String caller) throws RecordNotFoundException
	{
		final Wizard found = wizardsMap.get (wizardID);
		if (found == null)
			throw new RecordNotFoundException (Wizard.class.getName (), wizardID, caller);

		return found;
	}
}
