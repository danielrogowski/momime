package momime.client.ui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * Action that has a "selected" state that is toggled each time it is activated
 */
public class ToggleAction extends AbstractAction
{
	/** Unique value for serialization */
	private static final long serialVersionUID = 7375177369674126804L;
	
	/** Whether the action has been selected */
	private boolean selected;

	/**
	 * Can subclass ToggleAction and override this to provide some custom behaviour when the action is selected/deselected
	 */
	protected void selectedChanged ()
	{
	}
	
	/**
	 * Toggle the state 
	 */
	@Override
	public final void actionPerformed (final ActionEvent e)
	{
		selected = !selected;
		selectedChanged ();
	}
	
	/**
	 * @return Whether the action has been selected
	 */
	public final boolean isSelected ()
	{
		return selected;
	}
}
