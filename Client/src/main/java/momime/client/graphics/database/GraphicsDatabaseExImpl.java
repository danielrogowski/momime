package momime.client.graphics.database;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import momime.client.graphics.database.v0_9_5.Animation;
import momime.client.graphics.database.v0_9_5.GraphicsDatabase;
import momime.client.graphics.database.v0_9_5.Pick;
import momime.client.graphics.database.v0_9_5.TileSet;
import momime.client.graphics.database.v0_9_5.Wizard;
import momime.common.MomException;
import momime.common.database.RecordNotFoundException;

/**
 * Implementation of graphics XML database - extends stubs auto-generated from XSD to add additional functionality from the interface
 */
public final class GraphicsDatabaseExImpl extends GraphicsDatabase implements GraphicsDatabaseEx
{
	/** Class logger */
	private final Logger log = Logger.getLogger (GraphicsDatabaseExImpl.class.getName ());
	
	/** Map of pick IDs to pick objects */
	private Map<String, Pick> picksMap;

	/** Map of wizard IDs to wizard objects */
	private Map<String, Wizard> wizardsMap;

	/** Map of animation IDs to animation objects */
	private Map<String, Animation> animationsMap;
	
	/**
	 * Builds all the hash maps to enable finding records faster
	 * @throws MomException If there are invalid rules defined, i.e. with a mixture of null and non-null condition/set rules
	 * @throws RecordNotFoundException If an image is missing, or a tile type refers to a smoothing system that doesn't exist
	 */
	public final void buildMaps () throws MomException, RecordNotFoundException
	{
		log.entering (GraphicsDatabaseExImpl.class.getName (), "buildMaps");
		log.info ("Processing graphics XML file");
		
		// Create picks map
		picksMap = new HashMap<String, Pick> ();
		for (final Pick thisPick : getPick ())
			picksMap.put (thisPick.getPickID (), thisPick);

		// Create wizards map
		wizardsMap = new HashMap<String, Wizard> ();
		for (final Wizard thisWizard : getWizard ())
			wizardsMap.put (thisWizard.getWizardID (), thisWizard);
		
		// Create animations map
		animationsMap = new HashMap<String, Animation> ();
		for (final Animation thisAnimation : getAnimation ())
			animationsMap.put (thisAnimation.getAnimationID (), thisAnimation);
		
		// Build all the smoothing rule bitmask maps
		for (final TileSet ts : getTileSet ())
		{
			final TileSetEx tsex = (TileSetEx) ts;
			tsex.buildMaps ();
			tsex.deriveAnimationFrameCountAndSpeed (this);
		}

		log.exiting (GraphicsDatabaseExImpl.class.getName (), "buildMaps");
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

	/**
	 * @param animationID Animation ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Animation object
	 * @throws RecordNotFoundException If the animationID doesn't exist
	 */
	@Override
	public final Animation findAnimation (final String animationID, final String caller) throws RecordNotFoundException
	{
		final Animation found = animationsMap.get (animationID);
		if (found == null)
			throw new RecordNotFoundException (Animation.class.getName (), animationID, caller);

		return found;
	}
}
