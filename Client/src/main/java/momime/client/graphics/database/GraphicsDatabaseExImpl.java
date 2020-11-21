package momime.client.graphics.database;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import momime.common.database.Animation;
import momime.common.database.AnimationEx;
import momime.common.database.PlayList;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSpecialOrder;

/**
 * Implementation of graphics XML database - extends stubs auto-generated from XSD to add additional functionality from the interface
 */
public final class GraphicsDatabaseExImpl extends GraphicsDatabase implements GraphicsDatabaseEx
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (GraphicsDatabaseExImpl.class);
	
	/** Map of unit attribute component IDs to unit attribute component objects */
	private Map<UnitSkillComponent, UnitSkillComponentImage> unitSkillComponentsMap;
	
	/** Map of unit special order IDs to unit special order objects */
	private Map<UnitSpecialOrder, UnitSpecialOrderImage> unitSpecialOrdersMap;
	
	/** Map of scales to coordinates for each figure count */
	private Map<Integer, CombatTileUnitRelativeScaleGfx> combatTileUnitRelativeScalesMap;
	
	/** Map of animation IDs to animation objects */
	private Map<String, AnimationEx> animationsMap;
	
	/** Map of play list IDs to play list objects */
	private Map<String, PlayList> playListsMap;
	
	/**
	 * Builds all the hash maps to enable finding records faster
	 */
	public final void buildMaps ()
	{
		log.trace ("Entering buildMaps");
		
		unitSkillComponentsMap = getUnitSkillComponentImage ().stream ().collect (Collectors.toMap (i -> i.getUnitSkillComponentID (), i -> i));
		unitSpecialOrdersMap = getUnitSpecialOrderImage ().stream ().collect (Collectors.toMap (i -> i.getUnitSpecialOrderID (), i -> i));
		combatTileUnitRelativeScalesMap = getCombatTileUnitRelativeScales ().stream ().collect (Collectors.toMap (s -> s.getScale (), s -> s));
		animationsMap = getAnimations ().stream ().collect (Collectors.toMap (a -> a.getAnimationID (), a -> a));
		playListsMap = getPlayList ().stream ().collect (Collectors.toMap (p -> p.getPlayListID (), p -> p));
		
		getCombatTileUnitRelativeScales ().forEach (s -> s.buildMap ());
		
		log.trace ("Exiting buildMaps");
	}

	/**
	 * Verifies that all animations, tiles and so on are consistent across the graphics DB
	 * @throws IOException If any images cannot be loaded, or any consistency checks fail
	 */
	public final void consistencyChecks () throws IOException
	{
		log.trace ("Entering consistencyChecks");
		log.info ("Processing graphics XML file");
		
		// Check all animations have frames with consistent sizes
		for (final AnimationEx anim : getAnimations ())
			anim.deriveAnimationWidthAndHeight ();
		
		log.info ("All " + getAnimation ().size () + " graphics XML animations passed consistency checks");		
		
		log.trace ("Exiting consistencyChecks");
	}

	/**
	 * Method triggered by Spring when the the DB is created
	 * @throws IOException If any images cannot be loaded, or any consistency checks fail
	 */
	public final void buildMapsAndRunConsistencyChecks () throws IOException
	{
		log.trace ("Entering buildMapsAndRunConsistencyChecks");

		buildMaps ();
		consistencyChecks ();

		log.trace ("Exiting buildMapsAndRunConsistencyChecks");
	}
	
	/**
	 * @param UnitSkillComponentID Unit attribute component ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit attribute component object
	 * @throws RecordNotFoundException If the UnitSkillComponentID doesn't exist
	 */
	@Override
	public final UnitSkillComponentImage findUnitSkillComponent (final UnitSkillComponent UnitSkillComponentID, final String caller)
		throws RecordNotFoundException
	{
		final UnitSkillComponentImage found = unitSkillComponentsMap.get (UnitSkillComponentID);
		if (found == null)
			throw new RecordNotFoundException (UnitSkillComponentImage.class, UnitSkillComponentID.toString (), caller);

		return found;
	}
	
	/**
	 * @param unitSpecialOrderID Unit special order ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit special order object
	 * @throws RecordNotFoundException If the unitSpecialOrderID doesn't exist
	 */
	@Override
	public final UnitSpecialOrderImage findUnitSpecialOrder (final UnitSpecialOrder unitSpecialOrderID, final String caller) throws RecordNotFoundException
	{
		final UnitSpecialOrderImage found = unitSpecialOrdersMap.get (unitSpecialOrderID);
		if (found == null)
			throw new RecordNotFoundException (UnitSpecialOrderImage.class, unitSpecialOrderID.toString (), caller);

		return found;
	}
	
	/**
	 * @return List of all unit relative scales
	 */
	@SuppressWarnings ("unchecked")
	public final List<CombatTileUnitRelativeScaleGfx> getCombatTileUnitRelativeScales ()
	{
		return (List<CombatTileUnitRelativeScaleGfx>) (List<?>) getCombatTileUnitRelativeScale ();
	}
	
	/**
	 * @param scale Combat tile unit relative scale
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Scale object
	 * @throws RecordNotFoundException If the scale doesn't exist
	 */
	@Override
	public final CombatTileUnitRelativeScaleGfx findCombatTileUnitRelativeScale (final int scale, final String caller) throws RecordNotFoundException
	{
		final CombatTileUnitRelativeScaleGfx found = combatTileUnitRelativeScalesMap.get (scale);
		if (found == null)
			throw new RecordNotFoundException (CombatTileUnitRelativeScale.class, scale, caller);

		return found;
	}

	/**
	 * @return List of all animations
	 */
	@SuppressWarnings ("unchecked")
	public final List<AnimationEx> getAnimations ()
	{
		return (List<AnimationEx>) (List<?>) getAnimation ();
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
			throw new RecordNotFoundException (Animation.class, animationID, caller);

		return found;
	}

	/**
	 * @param playListID Play list ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Play list object
	 * @throws RecordNotFoundException If the playListID doesn't exist
	 */
	@Override
	public final PlayList findPlayList (final String playListID, final String caller) throws RecordNotFoundException
	{
		final PlayList found = playListsMap.get (playListID);
		if (found == null)
			throw new RecordNotFoundException (PlayList.class, playListID, caller);

		return found;
	}
}