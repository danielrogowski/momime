package momime.client.utils;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.JComponent;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.UntransmittedKillUnitActionID;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.servertoclient.KillUnitActionID;
import momime.common.messages.AvailableUnit;

/**
 * Client side only helper methods for dealing with units
 */
public interface UnitClientUtils
{
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
	 * Rules for finding the right icon for a unit attribute aren't totally straightforward; ranged attacks have their own images and some
	 * unit attributes (and some RATs) have different icons for different weapon types and some do not.  So this method deals with all that.
	 * 
	 * @param unit Unit whose attributes we're drawing
	 * @param unitAttributeID Which attribute to draw
	 * @return Icon for this unit attribute, or null if there isn't one
	 * @throws IOException If there's a problem finding the unit attribute icon
	 */
	public BufferedImage getUnitAttributeIcon (final AvailableUnit unit, final String unitAttributeID) throws IOException;
	
	/**
	 * Rules for finding the right icon for a unit skills aren't totally straightforward; experience icon changes as units
	 * gain level, and some skills (particularly movement type skills like walking/flying) have no icon at all.  So this method deals with all that.
	 * 
	 * @param unit Unit whose skills we're drawing
	 * @param unitSkillID Which attribute to draw
	 * @return Icon for this unit skill, or null if there isn't one
	 * @throws IOException If there's a problem finding the unit skill icon
	 */
	public BufferedImage getUnitSkillIcon (final AvailableUnit unit, final String unitSkillID) throws IOException;
	
	/**
	 * Kills a unit, either permanently removing it or marking it as dead in case it gets Raise or Animate Dead cast on it later
	 * 
	 * @param unitURN Unit to kill
	 * @param transmittedAction Method by which the unit is being killed, out of possible values that are sent from the server; null if untransmittedAction is filled in
	 * @param untransmittedAction Method by which the unit is being killed, out of possible values that are inferred from other messages; null if transmittedAction is filled in
	 * @throws IOException If there is a problem
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	public void killUnit (final int unitURN, final KillUnitActionID transmittedAction, final UntransmittedKillUnitActionID untransmittedAction)
		throws IOException, JAXBException, XMLStreamException;
	
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
	 * @param unitID The unit to draw
	 * @param unitTypeID The type of unit it is (can pass in null - the only value it cares about is if this is 'S' for summoned units)
	 * @param totalFigureCount The number of figures the unit had when fully healed
	 * @param aliveFigureCount The number of figures the unit has now
	 * @param offsetX The x offset into the graphics context to draw the unit at
	 * @param offsetY The y offset into the graphics context to draw the unit at
	 * @return Array of all figure positions in pixels; [0] element = x coord, [1] element = y coord, [2] element = multiplier to enlarge image by
	 * @throws IOException If there is a problem
	 */
	public int [] [] calcUnitFigurePositions (final String unitID, final String unitTypeID, final int totalFigureCount, final int aliveFigureCount,
		final int offsetX, final int offsetY) throws IOException;
	
	/**
	 * Draws the figures of a unit.
	 * NB. This has to work without relying on the AvailableUnit so that we can draw units on the Options screen before joining a game.
	 * 
	 * @param unitID The unit to draw
	 * @param unitTypeID The type of unit it is (can pass in null - the only value it cares about is if this is 'S' for summoned units)
	 * @param totalFigureCount The number of figures the unit had when fully healed
	 * @param aliveFigureCount The number of figures the unit has now
	 * @param combatActionID The action to show the unit doing
	 * @param direction The direction to show the unit facing
	 * @param g The graphics context to draw the unit onto
	 * @param offsetX The x offset into the graphics context to draw the unit at
	 * @param offsetY The y offset into the graphics context to draw the unit at
	 * @param sampleTileImageFile The filename of the sample tile (grass or ocean) to draw under this unit; if null, then no sample tile will be drawn
	 * @param registeredAnimation Determines frame number: True=by Swing timer, must have previously called registerRepaintTrigger; False=by System.nanoTime ()
	 * @throws IOException If there is a problem
	 */
	public void drawUnitFigures (final String unitID, final String unitTypeID, final int totalFigureCount, final int aliveFigureCount, final String combatActionID,
		final int direction, final Graphics g, final int offsetX, final int offsetY, final String sampleTileImageFile, final boolean registeredAnimation) throws IOException;

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
	 * @throws IOException If there is a problem
	 */
	public void drawUnitFigures (final AvailableUnit unit, final String combatActionID,
		final int direction, final Graphics g, final int offsetX, final int offsetY, final boolean drawSampleTile, final boolean registeredAnimation) throws IOException;

	/**
	 * When we 4x the number of units in a tile, if they still take the same amount of time to walk from tile to tile, it looks
	 * like they are really sprinting it - so make these take longer, according to the same rules as how unitImageMultiplier is
	 * calculated in drawUnitFigures above.  i.e. if unitImageMultiplier == 2 then take 1 second, if unitImageMultiplier == 1 then take 2 seconds.
	 *  
	 * @param unit The unit that is walking/flying
	 * @return Time, in seconds, a unit takes to walk from tile to tile in combat
	 * @throws RecordNotFoundException If we can't find the unit definition or its magic realm
	 * @throws MomException If we encounter a combatScale that we don't know how to handle
	 */
	public double calculateWalkTiming (final AvailableUnit unit) throws RecordNotFoundException, MomException;

	/**
	 * Plays the sound effect for a particular unit taking a particular action.  This covers all combat actions, so the clank clank of units walking,
	 * the chop chop of melee attacks, the pew or whoosh of ranged attacks, and so on.
	 * 
	 * @param unit The unit making an action
	 * @param combatActionID The type of action being performed
	 * @throws RecordNotFoundException If the unit or its action can't be found in the graphics XML
	 */
	public void playCombatActionSound (final AvailableUnit unit, final String combatActionID) throws RecordNotFoundException;
}