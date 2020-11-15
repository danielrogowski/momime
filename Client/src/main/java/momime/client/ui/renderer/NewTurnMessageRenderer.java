package momime.client.ui.renderer;

import java.awt.Component;
import java.awt.Image;
import java.security.InvalidParameterException;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import momime.client.newturnmessages.NewTurnMessageComplexUI;
import momime.client.newturnmessages.NewTurnMessageSimpleUI;
import momime.client.newturnmessages.NewTurnMessageUI;

/**
 * Renderer for drawing new turn messages
 */
public final class NewTurnMessageRenderer implements ListCellRenderer<NewTurnMessageUI>
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (NewTurnMessageRenderer.class);
	
	/** Label used to display SimpleUIs */
	private JLabel simpleUILabel = new JLabel ();
	
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
			final NewTurnMessageSimpleUI simple = (NewTurnMessageSimpleUI) msg;
			simpleUILabel.setForeground (simple.getColour ());
			simpleUILabel.setFont (simple.getFont ());
			
			try
			{
				simpleUILabel.setText (simple.getText ());
			}
			catch (final Exception e)
			{
				log.error (e, e);
			}
			
			simpleUILabel.setHorizontalAlignment (SwingConstants.CENTER);
			
			final Image image = simple.getImage ();
			simpleUILabel.setIcon ((image == null) ? null : new ImageIcon (image));
			
			component = simpleUILabel;
		}
		else if (msg instanceof NewTurnMessageComplexUI)
			component = ((NewTurnMessageComplexUI) msg).getComponent ();
		else
			throw new InvalidParameterException ("NewTurnMessageRenderer is trying to draw a message of class " + msg.getClass ().getName () +
				" but it implements neither the Simple or Complex drawing UI");
		
		return component;
	}
}