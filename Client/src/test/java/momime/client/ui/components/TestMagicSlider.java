package momime.client.ui.components;

import java.awt.Dimension;
import java.awt.GridBagLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;

import momime.common.database.CommonDatabaseConstants;

import org.junit.jupiter.api.Test;

import com.ndg.swing.GridBagConstraintsNoFill;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;

/**
 * Tests the MagicSlider class
 */
public final class TestMagicSlider
{
	/**
	 * Sets up a dummy panel and frame to display a magic slider
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testMagicSlider () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Set up slider
		final MagicSlider slider = new MagicSlider ();
		slider.setUtils (utils);
		slider.init ("mana", null, CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX / 3);
		
		// Set up dummy panel to display the button
		final JPanel panel = new JPanel ();
		panel.setPreferredSize (new Dimension (100, 400));
		panel.setLayout (new GridBagLayout ());
		
		panel.add (slider, utils.createConstraintsNoFill (0, 0, 1, 1, 0, GridBagConstraintsNoFill.CENTRE));		

		// Set up dummy frame to display the panel
		final JFrame frame = new JFrame ("testMagicSlider");
		frame.setContentPane (panel);
		frame.setLocationRelativeTo (null);
		frame.setResizable (false);		// Must turn resizeable off before calling pack, so pack uses the size for the correct type of window decorations
		frame.pack ();
		frame.setPreferredSize (frame.getSize ());

		frame.setVisible (true);
		Thread.sleep (5000);
		frame.setVisible (false);
	}
}