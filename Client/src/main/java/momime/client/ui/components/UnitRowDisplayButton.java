package momime.client.ui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.JButton;

import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.ui.PlayerColourImageGenerator;
import momime.common.messages.MemoryUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.NdgUIUtils;

/**
 * Component which draws a unit on patch of colour matching the colour of the player flags.
 * i.e. looks just like SelectUnitButton, without the grey button portion and the health bar.
 */
public final class UnitRowDisplayButton extends JButton
{
	/** Class logger */
	private final Log log = LogFactory.getLog (UnitRowDisplayButton.class);

	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Player colour image generator */
	private PlayerColourImageGenerator playerColourImageGenerator;

	/** Unit being selected */
	private MemoryUnit unit;
	
	/** Button size */
	private Dimension buttonSize;

	/**
	 * Sets up the panel once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	public final void init () throws IOException
	{
		log.trace ("Entering init");

		// We don't really need this image, but use it to fix the size of the button		
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/overland/unitBackground.png");
		
		// Fix size
		buttonSize = new Dimension (background.getWidth (), background.getHeight ());
		setMinimumSize (buttonSize);
		setMaximumSize (buttonSize);
		setPreferredSize (buttonSize);

		log.trace ("Exiting init");
	}
	
	/**
	 * Override default drawing of the button
	 */
	@Override
	protected final void paintComponent (final Graphics g)
	{
		// Offset the image when the button is pressed
		final int offset = (getModel ().isPressed () && getModel ().isArmed ()) ? 1 : 0;
		
		// Draw blackness - only really matters for when the button is pressed and we have that 1 pixel border to fill in
		g.setColor (Color.BLACK);
		g.fillRect (0, 0, buttonSize.width, buttonSize.height);
		
		// All the rest only makes sense if we have a unit to draw
		if (getUnit () != null)
			try
			{
				// Draw player colour patch
				final BufferedImage playerColour = getPlayerColourImageGenerator ().getUnitBackgroundImage (getUnit ().getOwningPlayerID ());
				g.drawImage (playerColour, offset, offset, null);
			
				// Draw the unit itself
				final BufferedImage unitImage = getUtils ().loadImage (getGraphicsDB ().findUnit (getUnit ().getUnitID (), "UnitRowDisplayButton").getUnitOverlandImageFile ());
				g.drawImage (unitImage, offset, offset, null);
			}
			catch (final IOException e)
			{
				log.error (e, e);
			}
	}
	
	/**
	 * @return Helper methods and constants for creating and laying out Swing components
	 */
	public final NdgUIUtils getUtils ()
	{
		return utils;
	}

	/**
	 * @param util Helper methods and constants for creating and laying out Swing components
	 */
	public final void setUtils (final NdgUIUtils util)
	{
		utils = util;
	}
	
	/**
	 * @return Graphics database
	 */
	public final GraphicsDatabaseEx getGraphicsDB ()
	{
		return graphicsDB;
	}

	/**
	 * @param db Graphics database
	 */
	public final void setGraphicsDB (final GraphicsDatabaseEx db)
	{
		graphicsDB = db;
	}
	
	/**
	 * @return Player colour image generator
	 */
	public final PlayerColourImageGenerator getPlayerColourImageGenerator ()
	{
		return playerColourImageGenerator;
	}

	/**
	 * @param gen Player colour image generator
	 */
	public final void setPlayerColourImageGenerator (final PlayerColourImageGenerator gen)
	{
		playerColourImageGenerator = gen;
	}

	/**
	 * @return Unit being selected
	 */
	public final MemoryUnit getUnit ()
	{
		return unit;
	}

	/**
	 * @param u Unit being selected
	 */
	public final void setUnit (final MemoryUnit u)
	{
		unit = u;
		repaint ();
	}
}