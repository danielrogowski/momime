package momime.server.ui;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import momime.common.messages.v0_9_5.MomSessionDescription;

/**
 * Window to display the details of a one session, when using the OneWindowPerGameUI
 */
public class SessionWindow
{
	/**
	 * The actual window/frame
	 */
	private final JFrame frame;

	/**
	 * The text area on the form to write log messages to
	 */
	private final JTextArea textArea;

	/**
	 * Number of lines of text currently in the window
	 */
	private int linesOfText;

	/**
	 * Number of lines of text we put in the window before we start wrapping lines off the top
	 */
	private static final int MAX_LINE_COUNT = 400;

	/**
	 * Creates a window with a text area that can receive log messages
	 * @param session The session this window is for
	 */
	SessionWindow (final MomSessionDescription session)
	{
		// Initialize the frame
		frame = new JFrame ();
		frame.setSize (600, 400);
		frame.setTitle ("Session " + session.getSessionID () + " - " + session.getSessionName ());
		frame.setDefaultCloseOperation (WindowConstants.DO_NOTHING_ON_CLOSE);	// Window can only close by the session ending

		// Initialize the content pane
		final JPanel contentPane = new JPanel ();
		contentPane.setBorder (BorderFactory.createCompoundBorder (BorderFactory.createEmptyBorder (SwingLayoutConstants.CONTENT_PANE_BORDER_SIZE, SwingLayoutConstants.CONTENT_PANE_BORDER_SIZE,
			SwingLayoutConstants.CONTENT_PANE_BORDER_SIZE, SwingLayoutConstants.CONTENT_PANE_BORDER_SIZE), contentPane.getBorder ()));
		contentPane.setLayout (new BoxLayout (contentPane, BoxLayout.Y_AXIS));		// Layout is pretty irrelevant since there's only one control

		// Create the list of log messages
		textArea = new JTextArea ();
		textArea.setFont (new JTextField ().getFont ());	// Otherwise defaults to a courier type font?
		linesOfText = 0;

		// Put the list of log messages in a scroll pane
		final JScrollPane scrollPane = new JScrollPane (textArea);
		scrollPane.setAlignmentX (Component.LEFT_ALIGNMENT);
		scrollPane.setAlignmentY (Component.TOP_ALIGNMENT);
		scrollPane.setMinimumSize (new Dimension (100, 50));
		scrollPane.setPreferredSize (new Dimension (100, 50));
		contentPane.add (scrollPane);

		// Show frame
		frame.setContentPane (contentPane);
		frame.setVisible (true);
	}

	/**
	 * Adds a line of text to the log area
	 * @param newText Line of text to add
	 */
	void addLine (final String newText)
	{
		// Strip off old lines to make space
		while (linesOfText >= MAX_LINE_COUNT)
		{
			textArea.setText (textArea.getText ().substring (textArea.getText ().indexOf ("\r\n") + 2));
			linesOfText--;
		}

		// Add new text
		textArea.setText (textArea.getText () + newText);
		linesOfText++;
	}

	/**
	 * Closes out the window
	 */
	void close ()
	{
		frame.dispose ();
	}
}
