package momime.client.ui.panels;

import java.awt.Graphics;
import java.io.IOException;

import javax.swing.JPanel;

import momime.client.language.LanguageVariableUIImpl;

/**
 * Ancestor used by all of the UI panels.
 */
public abstract class MomClientPanelUI extends LanguageVariableUIImpl
{
	/** The actual panel */
	private JPanel panel;

	/**
	 * @return The actual panel
	 * @throws IOException If a resource cannot be found
	 */
	public final JPanel getPanel () throws IOException
	{
		if (panel == null)
		{
			final MomClientPanelUI container = this;
			panel = new JPanel ()
			{
				/**
				 * Assume the majority of panels are going to have custom backgrounds, so delegate the
				 * paintComponent method out to the container class to make it easier to override it.
				 */
				@Override
				protected final void paintComponent (final Graphics g)
				{
					super.paintComponent (g);
					container.paintComponent (g);
				}
			};
			
			init ();
			languageChanged ();
			getLanguageChangeMaster ().addLanguageChangeListener (this);
		}
		
		return panel;
	}

	/**
	 * By default this does nothing, but provides an easier placeholder to override for painting custom backgrounds and so on
	 * @param g Graphics context
	 */
	protected void paintComponent (@SuppressWarnings ("unused") final Graphics g)
	{
	}
}