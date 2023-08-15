package momime.client.ui.frames;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import jakarta.xml.bind.JAXBException;
import momime.client.messages.process.MoveUnitStackOverlandMessageImpl;
import momime.common.MomException;
import momime.common.database.AnimationEx;
import momime.common.database.RecordNotFoundException;
import momime.common.movement.OverlandMovementCell;

/**
 * Screen for displaying the overland map, including the buttons and side panels and so on that appear in the same frame
 */
public interface OverlandMapUI
{
	/**
	 * @param v Whether to display or hide this screen
	 * @throws IOException If a resource cannot be found
	 */
	public void setVisible (final boolean v) throws IOException;
	
	/**
	 * Called when game is closing down; hides the window, but doesn't record the fact that its hidden in the config file
	 */
	public void closedown ();
	
	/**
	 * Generates big bitmaps of the entire overland map in each frame of animation.
	 * Delphi client did this rather differently, by building Direct3D vertex buffers to display all the map tiles; equivalent method there was RegenerateCompleteSceneryView.
	 * 
	 * @throws IOException If there is a problem loading any of the images
	 */
	public void regenerateOverlandMapBitmaps () throws IOException;
	
	/**
	 * Generates big bitmap of the smoothed edges of blackness that obscure the edges of the outermost tiles we can see, so that the edges aren't just a solid black line
	 * 
	 * @throws IOException If there is a problem loading any of the images
	 */
	public void regenerateFogOfWarBitmap () throws IOException;
	
	/**
	 * Updates the turn label
	 */
	public void updateTurnLabelText ();
	
	/**
	 * Creates highlighting bitmap showing map cells we can/can't move to
	 */
	public void regenerateMovementTypesBitmap ();
	
	/**
	 * Ensures that the specified location is visible
	 * 
	 * @param x X coordinate to show, in map coords
	 * @param y Y coordinate to show, in map coords
	 * @param plane Plane to show, in map coords
	 * @param force If true, will forcibly recentre the map on x, y regardless of whether x, y is already visible
	 */
	public void scrollTo (final int x, final int y, final int plane, final boolean force);
	
	/**
	 * Called when player clicks an Overland Enchantment on the magic sliders screen to target a Disjunction-type spell at
	 * 
	 * @param spellURN Overland Enchantment to target
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If a unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns a unit
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public void targetOverlandSpellURN (final int spellURN)
		throws JAXBException, XMLStreamException, PlayerNotFoundException, RecordNotFoundException, MomException;

	/**
	 * Called when player clicks a Wizard to target a spell like Spell Blast
	 * 
	 * @param targetPlayerID Wizard to target
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If a unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns a unit
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public void targetOverlandPlayerID (final int targetPlayerID)
		throws JAXBException, XMLStreamException, PlayerNotFoundException, RecordNotFoundException, MomException;
	
	/**
	 * Forces the scenery panel to be redrawn as soon as possible
	 */
	public void repaintSceneryPanel ();

	/**
	 * @return The map location we're currently selecting/deselecting units at ready to choose an order for them or tell them where to move/attack
	 */
	public MapCoordinates3DEx getUnitMoveFrom ();
	
	/**
	 * @param u The map location we're currently selecting/deselecting units at ready to choose an order for them or tell them where to move/attack
	 */
	public void setUnitMoveFrom (final MapCoordinates3DEx u);
	
	/**
	 * @return Frame number being displayed
	 */
	public int getTerrainAnimFrame ();

	/**
	 * @return The plane that the UI is currently displaying
	 */
	public int getMapViewPlane ();
	
	/**
	 * @param stack Unit stack that's in the middle of moving from one cell to another
	 */
	public void setUnitStackMoving (final MoveUnitStackOverlandMessageImpl stack);
	
	/**
	 * @param an Animation to display for a spell being cast
	 */
	public void setOverlandCastAnimation (final AnimationEx an);
	
	/**
	 * @param x X coord to display overland cast animation at, in pixels
	 */
	public void setOverlandCastAnimationX (final int x);
	
	/**
	 * @param y Y coord to display overland cast animation at, in pixels
	 */
	public void setOverlandCastAnimationY (final int y);
	
	/**
	 * @param p Plane to display overland cast animation at; null means both (its cast at a tower)
	 */
	public void setOverlandCastAnimationPlane (final Integer p);
	
	/**
	 * @param frame Frame number to display of overland cast animation
	 */
	public void setOverlandCastAnimationFrame (final int frame);

	/**
	 * @param m Area detailing which map cells we can/can't move to
	 */
	public void setMoves (final OverlandMovementCell [] [] [] m);
}