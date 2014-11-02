package momime.client.ui.components;

import java.awt.CardLayout;
import java.awt.Component;

import javax.swing.JPanel;

/**
 * Normally if we call .setVisible (false) on a Swing component, the layout manager will re-evaluate everything, allowing
 * other components to use up the space of the hidden component.
 * 
 * This gets around that problem so that we can hide a component but still have it take up the space as if it were visible.
 * It does so by setting up a CardLayout, with two cards, one being the real component, and the other being a blank panel.
 *
 * @param <C> The type of underlying component being displayed/hidden
 */
public final class HideableComponent<C extends Component> extends JPanel
{
	/** Card layout key for showing the real component */
	private static final String COMPONENT_PANEL = "C";

	/** Card layout key for showing a panel to hide the component */
	private static final String HIDDEN_PANEL = "P";
	
	/** The underlying component */
	private C component;
	
	/** Whether the component is shown or hidden */
	private boolean hidden;
	
	/** Card layout */
	private CardLayout cardLayout;

	/**
	 * @param c The underlying component
	 */
	public HideableComponent (final C c)
	{
		super ();
		
		// Set up the card layout
		component = c;
		
		cardLayout = new CardLayout ();
		setLayout (cardLayout);
		setOpaque (false);
		
		add (component, COMPONENT_PANEL);
		
		// Set up the hidden panel
		final JPanel hiddenPanel = new JPanel ();
		hiddenPanel.setOpaque (false);
		
		add (hiddenPanel, HIDDEN_PANEL);
		
		setHidden (false);
	}
	
	/**
	 * @return The underlying component
	 */
	public final C getComponent ()
	{
		return component;
	}

	/**
	 * @return Whether the component is shown or hidden
	 */
	public final boolean isHidden ()
	{
		return hidden;
	}

	/**
	 * @param value Whether the component is shown or hidden
	 */
	public final void setHidden (final boolean value)
	{
		hidden = value;
		cardLayout.show (this, hidden ? HIDDEN_PANEL : COMPONENT_PANEL);
	}
}