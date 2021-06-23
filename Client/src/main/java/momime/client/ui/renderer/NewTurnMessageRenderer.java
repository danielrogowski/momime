package momime.client.ui.renderer;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.security.InvalidParameterException;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.GridBagConstraintsNoFill;
import com.ndg.swing.NdgUIUtils;

import momime.client.newturnmessages.NewTurnMessageComplexUI;
import momime.client.newturnmessages.NewTurnMessageSimpleUI;
import momime.client.newturnmessages.NewTurnMessageUI;

/**
 * Renderer for drawing new turn messages
 */
public final class NewTurnMessageRenderer implements ListCellRenderer<NewTurnMessageUI>
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (NewTurnMessageRenderer.class);
	
	/** Gap between text and icon for simple layouts */
	private final static int INSET = 2;
	
	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;

	/** Outside panel to make inner panel take up minimum necessary space */
	private JPanel simplePanel;
	
	/** Panel containing text labels */
	private JPanel textPanel;
	
	/** Icon */
	private JLabel simpleIcon;
	
	/** The full width of the new turn messages scroll */
	private int scrollWidth;
	
	/**
	 * Determines the component to use to draw this cell
	 */
	@SuppressWarnings ("unused")
	@Override
	public final Component getListCellRendererComponent (final JList<? extends NewTurnMessageUI> list, final NewTurnMessageUI msg,
		final int index, final boolean isSelected, final boolean cellHasFocus)
	{
		final Component component;
		if (msg instanceof NewTurnMessageSimpleUI)
		{
			// Set up panel if this is the first time
			if (simplePanel == null)
			{
				simplePanel = new JPanel (new GridBagLayout ());
				simplePanel.setOpaque (false);
				
				textPanel = new JPanel (new GridBagLayout ()); 
				textPanel.setOpaque (false);
				
				simpleIcon = new JLabel ();

				simplePanel.add (textPanel, getUtils ().createConstraintsNoFill (0, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
				simplePanel.add (simpleIcon, getUtils ().createConstraintsNoFill (1, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
			}
			
			// Remove old labels
			textPanel.removeAll ();
			
			// Set up simple UI
			final NewTurnMessageSimpleUI simple = (NewTurnMessageSimpleUI) msg;
			final int imageWidth = (simple.getImage () == null) ? 0 : simple.getImage ().getWidth (null);
			try
			{
				final List<JLabel> labels = getUtils ().wrapLabels (simple.getFont (), simple.getColour (), simple.getText (),
					getScrollWidth () - (4 * INSET) - imageWidth);
				
				int y = 0;
				for (final JLabel label : labels)
				{
					label.setHorizontalAlignment (SwingConstants.CENTER);
					textPanel.add (label, getUtils ().createConstraintsNoFill (0, y, 1, 1, 0, GridBagConstraintsNoFill.CENTRE));
					y++;
				}
			}
			catch (final Exception e)
			{
				log.error (e, e);
			}
			
			final Image image = simple.getImage ();
			simpleIcon.setIcon ((image == null) ? null : new ImageIcon (image));
			
			component = simplePanel;
		}
		else if (msg instanceof NewTurnMessageComplexUI)
			component = ((NewTurnMessageComplexUI) msg).getComponent ();
		else
			throw new InvalidParameterException ("NewTurnMessageRenderer is trying to draw a message of class " + msg.getClass ().getName () +
				" but it implements neither the Simple or Complex drawing UI");
		
		return component;
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
	 * @return The full width of the new turn messages scroll
	 */
	public final int getScrollWidth ()
	{
		return scrollWidth;
	}

	/**
	 * @param w The full width of the new turn messages scroll
	 */
	public final void setScrollWidth (final int w)
	{
		scrollWidth = w;
	}
}