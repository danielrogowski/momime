package momime.client.ui.components;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;

/**
 * Tests the HideableComponent class
 */
public final class TestHideableComponent
{
	/**
	 * Tests the HideableComponent class
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHideableComponent () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Set up buttons
		final JButton button1 = new JButton ("One");
		final HideableComponent<JButton> button2 = new HideableComponent<JButton> (new JButton ("Two"));
		final JButton button3 = new JButton ("Three");
		
		// Set up content pane
		JPanel contentPane = new JPanel ();
		contentPane.setLayout (new FlowLayout ());
		contentPane.add (button1);
		contentPane.add (button2);
		contentPane.add (button3);
		
		// Set up timer
		final Timer timer = new Timer (1000, new ActionListener ()
		{
			@Override
			public void actionPerformed (final ActionEvent ev)
			{
				button2.setHidden (!button2.isHidden ());
			}
		});
		
		// Set up frame
		JFrame frame = new JFrame ();
		frame.setContentPane (contentPane);
		frame.pack ();
		frame.setVisible (true);

		timer.start ();
		Thread.sleep (5000);
		timer.stop ();
	}
}