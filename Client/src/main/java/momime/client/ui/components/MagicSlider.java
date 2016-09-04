package momime.client.ui.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import momime.common.database.CommonDatabaseConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.NdgUIUtils;

/**
 * Mana/Research/Skill sliders on the magic screen that allow sliding the value up and down and clicking the head of the staff to lock it.
 * So its a panel made up of a JToggleButton on the top and a JSlider on the bottom.
 */
public final class MagicSlider extends JPanel
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (MagicSlider.class);
	
	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** Image of the slider empty (set at 0), including the arrows at the top and bottom but without the head of the staff */
	private BufferedImage empty;

	/** Image of the slider fully lit up (set at 240), excluding the arrows at the top and bottom and the head of the staff */
	private BufferedImage full;
	
	/** Image of the head of the staff when the slider is movable */
	private BufferedImage unlocked;

	/** Image of the head of the staff when the slider is locked */
	private BufferedImage locked;

	/** The button for the head of the staff */
	private JToggleButton button;

	/** The slider portion */
	private JSlider slider;
	
	/**
	 * Sets up the panel once all values have been injected
	 * 
	 * @param prefix Prefix of the image names to use
	 * @param buttonAction Action that specifies behaviour when the head of the staff is clicked
	 * @param initialValue Value to initially set the slider at
	 * @throws IOException If a resource cannot be found
	 */
	public final void init (final String prefix, final Action buttonAction, final int initialValue) throws IOException
	{
		log.trace ("Entering init");

		// Load in all necessary images
		empty			= getUtils ().loadImage ("/momime.client.graphics/ui/magicSliders/" + prefix + "StaffEmpty.png");
		full				= getUtils ().loadImage ("/momime.client.graphics/ui/magicSliders/" + prefix + "StaffFull.png");
		unlocked		= getUtils ().loadImage ("/momime.client.graphics/ui/magicSliders/" + prefix + "Unlocked.png");
		locked			= getUtils ().loadImage ("/momime.client.graphics/ui/magicSliders/" + prefix + "Locked.png");
		
		// Set up the layout
		setLayout (new BorderLayout ());
		setOpaque (false);
		
		// The button can paint itself correctly just by setting the icons
		button = new JToggleButton (buttonAction);
		button.setIcon (new ImageIcon (unlocked));
		button.setRolloverIcon (new ImageIcon (unlocked));
		button.setSelectedIcon (new ImageIcon (locked));
		button.setContentAreaFilled (false);
		button.setMargin (new Insets (0, 0, 0, 0));
		button.setBorder (null);

		add (button, BorderLayout.NORTH);
		
		// The slider has to be drawn and sized ourselves
		final Dimension sliderSize = new Dimension (empty.getWidth (), empty.getHeight ());
		
		slider = new JSlider (0, CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX, initialValue)
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				final int x = (unlocked.getWidth () - empty.getWidth ()) / 2;
				final int y = (empty.getHeight () - full.getHeight ()) / 2;
				
				// Draw the full background for the slider
				g.drawImage (empty, x, 0, null);
				
				// Draw a piece of the brighter colour, according to the current slider position
				final int fullPixels = (full.getHeight () * getValue ()) / getMaximum ();
				g.drawImage (full,
					x, y + full.getHeight () - fullPixels, x + full.getWidth (), y + full.getHeight (),
					0, full.getHeight () - fullPixels, full.getWidth (), full.getHeight (), null);
			}
		};
		
		slider.setMinimumSize (sliderSize);
		slider.setMaximumSize (sliderSize);
		slider.setPreferredSize (sliderSize);
		slider.setOrientation (SwingConstants.VERTICAL);
		
		add (slider, BorderLayout.SOUTH);
		
		log.trace ("Exiting init");
	}
	
	/**
	 * @return Whether the head of the staff is locked
	 */
	public final boolean isLocked ()
	{
		return button.isSelected (); 
	}
	
	/**
	 * @return The slider control
	 */
	public final JSlider getSlider ()
	{
		return slider;
	}
	
	/**
	 * @return The current position of the slider
	 */
	public final int getValue ()
	{
		return slider.getValue ();
	}

	/**
	 * @param v The new position for the slider
	 */
	public final void setValue (final int v)
	{
		if (slider.getValue () != v)
			slider.setValue (v);
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
}