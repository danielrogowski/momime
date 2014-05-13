package momime.client.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.JPanel;
import javax.swing.WindowConstants;

import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.client.ui.panels.CityViewPanel;

/**
 * City screen, so you can view current buildings, production and civilians, examine
 * calculation breakdowns and change production and civilian task assignments
 */
public final class CityViewUI extends MomClientAbstractUI
{
	/** Large font */
	private Font largeFont;

	/** Medium font */
	private Font mediumFont;

	/** Panel where all the buildings are drawn */
	private CityViewPanel cityViewPanel;
	
	/** Overland map UI */
	private OverlandMapUI overlandMapUI;
	
	/** The city being viewed */
	private MapCoordinates3DEx cityLocation;
	
	/** Typical inset used on this screen layout */
	private final static int INSET = 3;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/cityView/background.png");

		// Initialize the frame
		getFrame ().setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		
		// Do this "too early" on purpose, so that the window isn't centred over the map, but is a little down-right of it
		getFrame ().setLocationRelativeTo (getOverlandMapUI ().getFrame ());
		
		// Initialize the content pane
		final JPanel contentPane = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				super.paintComponent (g);
				g.drawImage (background, 0, 0, background.getWidth () * 2, background.getHeight () * 2, null);
			}
		};
		
		final Dimension fixedSize = new Dimension (background.getWidth () * 2, background.getHeight () * 2);
 		contentPane.setPreferredSize (fixedSize);
		
		// Set up layout
		contentPane.setLayout (new GridBagLayout ());
		
		// Set up the city view panel
		getCityViewPanel ().setCityLocation (getCityLocation ());
		getCityViewPanel ().init ();
		
		final GridBagConstraints cityViewPanelConstraints = getUtils ().createConstraints (0, 6, 2, INSET, GridBagConstraints.CENTER);
		cityViewPanelConstraints.gridheight = 4;
		
		contentPane.add (getCityViewPanel (), cityViewPanelConstraints);
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);	// Must turn resizeable off before calling pack, so pack uses the size for the correct type of window decorations
		getFrame ().pack ();
		getFrame ().setPreferredSize (getFrame ().getSize ());
	}

	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
	}

	/**
	 * @return Large font
	 */
	public final Font getLargeFont ()
	{
		return largeFont;
	}

	/**
	 * @param font Large font
	 */
	public final void setLargeFont (final Font font)
	{
		largeFont = font;
	}

	/**
	 * @return Medium font
	 */
	public final Font getMediumFont ()
	{
		return mediumFont;
	}

	/**
	 * @param font Medium font
	 */
	public final void setMediumFont (final Font font)
	{
		mediumFont = font;
	}

	/**
	 * @return Panel where all the buildings are drawn
	 */
	public final CityViewPanel getCityViewPanel ()
	{
		return cityViewPanel;
	}

	/**
	 * @param pnl Panel where all the buildings are drawn
	 */
	public final void setCityViewPanel (final CityViewPanel pnl)
	{
		cityViewPanel = pnl;
	}

	/**
	 * @return Overland map UI
	 */
	public final OverlandMapUI getOverlandMapUI ()
	{
		return overlandMapUI;
	}

	/**
	 * @param ui Overland map UI
	 */
	public final void setOverlandMapUI (final OverlandMapUI ui)
	{
		overlandMapUI = ui;
	}

	/**
	 * @return The city being viewed
	 */
	public final MapCoordinates3DEx getCityLocation ()
	{
		return cityLocation;
	}

	/**
	 * @param loc The city being viewed
	 */
	public final void setCityLocation (final MapCoordinates3DEx loc)
	{
		cityLocation = loc;
	}
}