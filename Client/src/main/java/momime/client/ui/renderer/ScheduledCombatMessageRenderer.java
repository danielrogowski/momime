package momime.client.ui.renderer;

import java.awt.Component;
import java.awt.Image;
import java.security.InvalidParameterException;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;

import momime.client.scheduledcombatmessages.ScheduledCombatMessageSimpleUI;
import momime.client.scheduledcombatmessages.ScheduledCombatMessageUI;

/**
 * Renderer for drawing combats and messages on the scheduled combats scroll
 */
public final class ScheduledCombatMessageRenderer implements ListCellRenderer<ScheduledCombatMessageUI>
{
	/** Label used to display SimpleUIs */
	private JLabel simpleUILabel = new JLabel ();
	
	/**
	 * Determines the component to use to draw this cell
	 */
	@Override
	public final Component getListCellRendererComponent (final JList<? extends ScheduledCombatMessageUI> list, final ScheduledCombatMessageUI msg,
		final int index, final boolean isSelected, final boolean cellHasFocus)
	{
		final Component component;
		if (msg instanceof ScheduledCombatMessageSimpleUI)
		{
			final ScheduledCombatMessageSimpleUI simple = (ScheduledCombatMessageSimpleUI) msg;
			simpleUILabel.setForeground (simple.getColour ());
			simpleUILabel.setFont (simple.getFont ());
			simpleUILabel.setText (simple.getText ());
			simpleUILabel.setHorizontalAlignment (SwingConstants.CENTER);
			
			final Image image = simple.getImage ();
			simpleUILabel.setIcon ((image == null) ? null : new ImageIcon (image));
			
			component = simpleUILabel;
		}
//		else if (msg instanceof ScheduledCombatMessageComplexUI)
//			component = ((ScheduledCombatMessageComplexUI) msg).getComponent ();
		else
			throw new InvalidParameterException ("ScheduledCombatMessageRenderer is trying to draw a message of class " + msg.getClass ().getName () +
				" but it implements neither the Simple or Complex drawing UI");
		
		return component;
	}
}