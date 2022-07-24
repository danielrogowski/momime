package momime.client.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.swing.JComponent;
import javax.xml.stream.XMLStreamException;

import com.ndg.utils.swing.zorder.ZOrderGraphics;

import jakarta.xml.bind.JAXBException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.AvailableUnit;
import momime.common.messages.MemoryUnit;
import momime.common.messages.UnitStatusID;
import momime.common.utils.ExpandedUnitDetails;

/**
 * Client side only helper methods for dealing with units
 */
public interface UnitClientUtils
{
	/** Array column index in output from calcUnitFigurePositions for the x coord including the offsetX value */
	public final static int CALC_UNIT_FIGURE_POSITIONS_COLUMN_X_EXCL_OFFSET = 0;
	
	/** Array column index in output from calcUnitFigurePositions for the y coord including the offsetY value */
	public final static int CALC_UNIT_FIGURE_POSITIONS_COLUMN_Y_EXCL_OFFSET = 1;
	
	/** Array column index in output from calcUnitFigurePositions for the x coord excluding the offsetX value */
	public final static int CALC_UNIT_FIGURE_POSITIONS_COLUMN_X_INCL_OFFSET = 2;
	
	/** Array column index in output from calcUnitFigurePositions for the y coord excluding the offsetY value */
	public final static int CALC_UNIT_FIGURE_POSITIONS_COLUMN_Y_INCL_OFFSET = 3;
	
	/** Array column index in output from calcUnitFigurePositions for the size to multiply the unit images up by */
	public final static int CALC_UNIT_FIGURE_POSITIONS_COLUMN_UNIT_IMAGE_MULTIPLIER = 4;
	
	/**
	 * Note the generated unit names are obviously very dependant on the selected language, but the names themselves don't get notified
	 * to update themselves when the language changes.  It is the responsibility of whatever is calling this method to register itself to be
	 * notified of language updates, and cause this method to be re-evalulated when that happens.
	 * 
	 * @param unit Unit to generate the name of
	 * @param unitNameType Type of name to generate (see comments against that enum)
	 * @return Generated unit name
	 * @throws RecordNotFoundException If we can't find the unit definition in the server XML
	 */
	public String getUnitName (final AvailableUnit unit, final UnitNameType unitNameType) throws RecordNotFoundException;

	/**
	 * Finds the right icon for skills displayed in the top half of the unit info screen, where we display one icon per each "point"
	 * of the skill, so e.g. Melee 5 displays 5 swords.  The image returned here is transparent, so we can superimpose it over a
	 * background showing which component provided that value, so you can tell the difference between e.g. a unit's base
	 * stat and the bonus being granted from experience.  These were previously referred to as unit attributes.
	 * 
	 * Rules for finding the right icon for these aren't totally straightforward; ranged attacks have their own images and some
	 * unit attributes (and some RATs) have different icons for different weapon grades and some do not.  So this method deals with all that.
	 * 
	 * @param unit Unit whose skills we're drawing
	 * @param unitSkillID Which skill to draw
	 * @return Icon for this unit skill, or null if there isn't one
	 * @throws IOException If there's a problem finding the unit skill icon
	 */
	public BufferedImage getUnitSkillComponentBreakdownIcon (final ExpandedUnitDetails unit, final String unitSkillID) throws IOException;
	
	/**
	 * Finds the right icon for skills displayed in the bottom half of the unit info screen, where we just display a single icon for
	 * the skill no matter what its numeric value is, or if it has no value.  So the image returned here already includes a background.
	 * These were previously referred to as unit skills.
	 * 
	 * Rules for finding the right icon for these aren't totally straightforward; experience icon changes as units
	 * gain level, and some skills (particularly movement type skills like walking/flying) have no icon at all.  So this method deals with all that.
	 * 
	 * @param unit Unit whose skills we're drawing
	 * @param unitSkillID Which skill to draw
	 * @return Icon for this unit skill, or null if there isn't one
	 * @throws IOException If there's a problem finding the unit skill icon
	 */
	public BufferedImage getUnitSkillSingleIcon (final ExpandedUnitDetails unit, final String unitSkillID) throws IOException;
	
	/**
	 * Kills a unit, either permanently removing it or marking it as dead in case it gets Raise or Animate Dead cast on it later
	 * 
	 * @param unit Unit to kill
	 * @param newStatus The new status to set the unit to, e.g. DEAD or KILLED_BY_LACK_OF_PRODUCTION; a null here means remove the unit entirely
	 * @throws IOException If there is a problem
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	public void killUnit (final MemoryUnit unit, final UnitStatusID newStatus) throws IOException, JAXBException, XMLStreamException;
	
	/**
	 * Many unit figures are animated, and so must call this routine to register the animation prior to calling drawUnitFigures. 
	 * NB. This has to work without relying on the AvailableUnit so that we can draw units on the Options screen before joining a game.
	 * 
	 * @param unitID The unit to draw
	 * @param combatActionID The action to show the unit doing
	 * @param direction The direction to show the unit facing
	 * @param component The component that the unit will be drawn onto
	 * @throws IOException If there is a problem
	 */
	public void registerUnitFiguresAnimation (final String unitID, final String combatActionID, final int direction, final JComponent component) throws IOException;

	/**
	 * Unregisters unit animations started by registerUnitFiguresAnimation.  Note animation timers aren't reference counted, so to call this you must be
	 * certain that the animation is no longer being used.  e.g. If two flying units are facing direction 1, and one of them turns to face direction 2, we have to
	 * additionally register the 'flying d2' anim but can't unregister the 'flying d1' anim since another unit is still using it.
	 * 
	 * @param unitID The unit to draw
	 * @param combatActionID The action to show the unit doing
	 * @param direction The direction to show the unit facing
	 * @param component The component that the unit will be drawn onto
	 * @throws IOException If there is a problem
	 */
	public void unregisterUnitFiguresAnimation (final String unitID, final String combatActionID, final int direction, final JComponent component) throws IOException;

	/**
	 * Calculates the positions of all the figures of a unit, taking into account combat scale and so on.
	 * This is used both to draw the figures of a unit, and to get the positions to start ranged attack missiles at when the unit fires.
	 * NB. The resulting array may not have aliveFigureCount elements; it may be x1, x4 or x5 depending on combat scale.
	 * The calling routine should just iterate over the resulting array and make no assumptions about how many elements will contain. 
	 * 
	 * @param totalFigureCount The number of figures the unit had when fully healed
	 * @param aliveFigureCount The number of figures the unit has now
	 * @param offsetX The x offset into the graphics context to draw the unit at
	 * @param offsetY The y offset into the graphics context to draw the unit at
	 * @return Array of all figure positions in pixels; see CALC_UNIT_FIGURE_POSITIONS_COLUMN_ constants for what's stored in each column of the array
	 * @throws IOException If there is a problem
	 */
	public int [] [] calcUnitFigurePositions (final int totalFigureCount, final int aliveFigureCount, final int offsetX, final int offsetY) throws IOException;
	
	/**
	 * Draws the figures of a unit.
	 * NB. This has to work without relying on the AvailableUnit so that we can draw units on the Options screen before joining a game.
	 * 
	 * @param unitID The unit to draw
	 * @param playerID The owner of the unit
	 * @param totalFigureCount The number of figures the unit had when fully healed
	 * @param aliveFigureCount The number of figures the unit has now
	 * @param combatActionID The action to show the unit doing
	 * @param direction The direction to show the unit facing
	 * @param g The graphics context to draw the unit onto
	 * @param offsetX The x offset into the graphics context to draw the unit at
	 * @param offsetY The y offset into the graphics context to draw the unit at
	 * @param sampleTileImageFile The filename of the sample tile (grass or ocean) to draw under this unit; if null, then no sample tile will be drawn
	 * @param registeredAnimation Determines frame number: True=by Swing timer, must have previously called registerRepaintTrigger; False=by System.nanoTime ()
	 * @param baseZOrder Z order for the top of the tile
	 * @param shadingColours List of shading colours to apply to the image
	 * @param mergingRatio How much "dug into the ground" the unit should appear; null/0 means draw normally, 1 will draw nothing at all
	 * @throws IOException If there is a problem
	 */
	public void drawUnitFigures (final String unitID, final int playerID, final int totalFigureCount, final int aliveFigureCount, final String combatActionID,
		final int direction, final ZOrderGraphics g, final int offsetX, final int offsetY, final String sampleTileImageFile, final boolean registeredAnimation,
		final int baseZOrder, final List<String> shadingColours, final Double mergingRatio) throws IOException;

	/**
	 * Version which derives most of the values from an existing unit object.
	 * 
	 * @param unit The unit to draw
	 * @param combatActionID The action to show the unit doing
	 * @param direction The direction to show the unit facing
	 * @param g The graphics context to draw the unit onto
	 * @param offsetX The x offset into the graphics context to draw the unit at
	 * @param offsetY The y offset into the graphics context to draw the unit at
	 * @param drawSampleTile Whether to draw a sample tile (grass or ocean) under this unit
	 * @param registeredAnimation Determines frame number: True=by Swing timer, must have previously called registerRepaintTrigger; False=by System.nanoTime ()
	 * @param baseZOrder Z order for the top of the tile
	 * @param shadingColours List of shading colours to apply to the image
	 * @param mergingRatio How much "dug into the ground" the unit should appear; null/0 means draw normally, 1 will draw nothing at all
	 * @throws IOException If there is a problem
	 */
	public void drawUnitFigures (final ExpandedUnitDetails unit, final String combatActionID, final int direction, final ZOrderGraphics g,
		final int offsetX, final int offsetY, final boolean drawSampleTile, final boolean registeredAnimation, final int baseZOrder,
		final List<String> shadingColours, final Double mergingRatio) throws IOException;

	/**
	 * Plays the sound effect for a particular unit taking a particular action.  This covers all combat actions, so the clank clank of units walking,
	 * the chop chop of melee attacks, the pew or whoosh of ranged attacks, and so on.
	 * 
	 * @param unit The unit making an action
	 * @param combatActionID The type of action being performed
	 * @throws RecordNotFoundException If the unit or its action can't be found in the graphics XML
	 */
	public void playCombatActionSound (final AvailableUnit unit, final String combatActionID) throws RecordNotFoundException;

	/**
	 * @param unit Unit to generate attribute icons for
	 * @param unitSkillID Skill ID to generate attribute icons for
	 * @return Combined image showing icons for this unit attribute, with appropriate background colours; or null if the unit doesn't have this skill or it is value-less
	 * @throws IOException If there is a problem loading any of the images
	 */
	public BufferedImage generateAttributeImage (final ExpandedUnitDetails unit, final String unitSkillID) throws IOException;
}