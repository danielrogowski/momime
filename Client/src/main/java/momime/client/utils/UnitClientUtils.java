package momime.client.utils;

import java.awt.Graphics;
import java.io.IOException;

import javax.swing.JComponent;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_5.AvailableUnit;

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
	 * @throws IOException If there is a problem
	 */
	public void drawUnitFigures (final String unitID, final String unitTypeID, final int totalFigureCount, final int aliveFigureCount, final String combatActionID,
		final int direction, final Graphics g, final int offsetX, final int offsetY, final String sampleTileImageFile) throws IOException;

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
	 * @throws IOException If there is a problem
	 */
	public void drawUnitFigures (final AvailableUnit unit, final String combatActionID,
		final int direction, final Graphics g, final int offsetX, final int offsetY, final boolean drawSampleTile) throws IOException;
}