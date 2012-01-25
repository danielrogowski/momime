package momime.client.dummy;

import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

/**
 * Helper methods for laying out swing frames
 */
public final class SwingLayoutUtils
{
	/**
	 * Space to leave around the outside of the content pane, by:
	 * contentPane.setBorder (BorderFactory.createCompoundBorder (BorderFactory.createEmptyBorder (CONTENT_PANE_BORDER_SIZE, CONTENT_PANE_BORDER_SIZE,
	 * CONTENT_PANE_BORDER_SIZE, CONTENT_PANE_BORDER_SIZE), contentPane.getBorder ()));
	 */
	public static final int CONTENT_PANE_BORDER_SIZE = 8;

	/**
	 * @param panel Panel to add a standard invisible gap border to
	 */
	public static final void addGapBorder (final JPanel panel)
	{
		panel.setBorder (BorderFactory.createCompoundBorder (BorderFactory.createEmptyBorder
			(CONTENT_PANE_BORDER_SIZE, CONTENT_PANE_BORDER_SIZE,
			 CONTENT_PANE_BORDER_SIZE, CONTENT_PANE_BORDER_SIZE), panel.getBorder ()));
	}

	/**
	 * @param panel Panel to add a visible frame around, like a TPanel in Delphi
	 * @param text Text to write onto the frame border
	 */
	public static final void addTextBorder (final JPanel panel, final String text)
	{
		panel.setBorder (BorderFactory.createCompoundBorder (BorderFactory.createTitledBorder
			(BorderFactory.createEtchedBorder (EtchedBorder.LOWERED), text,
			 TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.BELOW_TOP, panel.getFont ().deriveFont (Font.BOLD)),
			 BorderFactory.createEmptyBorder (CONTENT_PANE_BORDER_SIZE, CONTENT_PANE_BORDER_SIZE,
					 											CONTENT_PANE_BORDER_SIZE, CONTENT_PANE_BORDER_SIZE)));
	}

	/**
	 * Prevent instantiation
	 */
	private SwingLayoutUtils ()
	{
	}
}
