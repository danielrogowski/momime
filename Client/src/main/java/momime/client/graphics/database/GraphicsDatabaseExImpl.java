package momime.client.graphics.database;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import momime.client.graphics.database.v0_9_5.Animation;
import momime.client.graphics.database.v0_9_5.GraphicsDatabase;
import momime.client.graphics.database.v0_9_5.Pick;
import momime.client.graphics.database.v0_9_5.TileSet;
import momime.client.graphics.database.v0_9_5.Wizard;
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

	/** Map of tileSet IDs to tileSet objects */
	private Map<String, TileSetEx> tileSetsMap;

	/** Map of animation IDs to animation objects */
	private Map<String, AnimationEx> animationsMap;
	
	/**
	 * Builds all the hash maps to enable finding records faster
	 * @throws IOException If any images cannot be loaded, or any consistency checks fail
	 */
	public final void buildMaps () throws IOException
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

		// Create animations map, and check for consistency
		animationsMap = new HashMap<String, AnimationEx> ();
		for (final Animation anim : getAnimation ())
		{
			final AnimationEx aex = (AnimationEx) anim;
			aex.deriveAnimationWidthAndHeight ();
			animationsMap.put (aex.getAnimationID (), aex);
		}
		log.info ("All " + getAnimation ().size () + " animations passed consistency checks");		
		
		// Create tileSets map, and build all the smoothing rule bitmask maps
		tileSetsMap = new HashMap<String, TileSetEx> ();
		for (final TileSet ts : getTileSet ())
		{
			final TileSetEx tsex = (TileSetEx) ts;
			tsex.buildMaps ();
			tsex.deriveAnimationFrameCountAndSpeed (this);
			tsex.deriveTileWidthAndHeight (this);
			tileSetsMap.put (tsex.getTileSetID (), tsex);
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
	 * @param tileSetID Tile set ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Tile set object
	 * @throws RecordNotFoundException If the tileSetID doesn't exist
	 */
	@Override
	public final TileSetEx findTileSet (final String tileSetID, final String caller) throws RecordNotFoundException
	{
		final TileSetEx found = tileSetsMap.get (tileSetID);
		if (found == null)
			throw new RecordNotFoundException (TileSet.class.getName (), tileSetID, caller);

		return found;
	}

	/**
	 * @param animationID Animation ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Animation object
	 * @throws RecordNotFoundException If the animationID doesn't exist
	 */
	@Override
	public final AnimationEx findAnimation (final String animationID, final String caller) throws RecordNotFoundException
	{
		final AnimationEx found = animationsMap.get (animationID);
		if (found == null)
			throw new RecordNotFoundException (Animation.class.getName (), animationID, caller);

		return found;
	}
}
