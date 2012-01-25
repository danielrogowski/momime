package momime.server.ui;

/**
 * Stores general constants controlling appearance and sizing of the UI
 * Any frame-specific values are declared as constants within that frame
 */
final class SwingLayoutConstants
{
	/**
	 * Space to leave around the outside of the content pane, by:
	 * contentPane.setBorder (BorderFactory.createCompoundBorder (BorderFactory.createEmptyBorder (CONTENT_PANE_BORDER_SIZE, CONTENT_PANE_BORDER_SIZE,
	 * CONTENT_PANE_BORDER_SIZE, CONTENT_PANE_BORDER_SIZE), contentPane.getBorder ()));
	 */
	public static final int CONTENT_PANE_BORDER_SIZE = 8;

	/** Space to leave between controls */
	public static final int SPACE_BETWEEN_CONTROLS = 8;
}
