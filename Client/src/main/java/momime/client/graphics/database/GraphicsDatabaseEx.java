package momime.client.graphics.database;

import momime.common.database.AnimationEx;
import momime.common.database.PlayList;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSpecialOrder;

/**
 * Describes operations that we need to support over the graphics XML file
 */
public interface GraphicsDatabaseEx
{
	/**
	 * @param UnitSkillComponentID Unit attribute component ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit attribute component object
	 * @throws RecordNotFoundException If the UnitSkillComponentID doesn't exist
	 */
	public UnitSkillComponentImage findUnitSkillComponent (final UnitSkillComponent UnitSkillComponentID, final String caller) throws RecordNotFoundException;

	/**
	 * @param unitSpecialOrderID Unit special order ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit special order object
	 * @throws RecordNotFoundException If the unitSpecialOrderID doesn't exist
	 */
	public UnitSpecialOrderImage findUnitSpecialOrder (final UnitSpecialOrder unitSpecialOrderID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @param scale Combat tile unit relative scale
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Scale object
	 * @throws RecordNotFoundException If the scale doesn't exist
	 */
	public CombatTileUnitRelativeScaleGfx findCombatTileUnitRelativeScale (final int scale, final String caller) throws RecordNotFoundException;
		
	/**
	 * @param animationID Animation ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Animation object
	 * @throws RecordNotFoundException If the animationID doesn't exist
	 */
	public AnimationEx findAnimation (final String animationID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @param playListID Play list ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Play list object
	 * @throws RecordNotFoundException If the playListID doesn't exist
	 */
	public PlayList findPlayList (final String playListID, final String caller) throws RecordNotFoundException;
}