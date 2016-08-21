package momime.client.ui.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

/**
 * Action that cycles its text through a set of objects in order to pick one
 * This is used for a lot of the buttons on the new game form
 * 
 * @param <E> The type of object being cycled through
 */
public class CycleAction<E> extends AbstractAction
{
	/** Items being cycled through */
	private List<CycleActionItem> items = new ArrayList<CycleActionItem> ();
	
	/** Index of currently selected item; -1 indicates nothing is selected because the list is empty */
	private int index = -1;

	/**
	 * Disable action until some items get added to it
	 */
	public CycleAction ()
	{
		super ();
		setEnabled (false);
	}
	
	/**
	 * Empties the list of items
	 */
	public final void clearItems ()
	{
		items.clear ();
		index = -1;
		setEnabled (false);
		putValue (Action.NAME, null);
	}
	
	/**
	 * Adds an item to the list of those that can be selected
	 * If the list was empty, adding an item automatically causes it to become selected
	 * 
	 * If the list only contains a single items, disables itself
	 * 
	 * @param anItem Underlying list item
	 * @param aText Corresponding text to display
	 */
	public final void addItem (final E anItem, final String aText)
	{
		items.add (new CycleActionItem (anItem, aText));
		
		if (index < 0)
		{
			index = 0;
			putValue (Action.NAME, aText);
			selectedItemChanged ();
		}
		
		setEnabled (items.size () > 1);
	}
	
	/**
	 * @return The currently selected item
	 */
	public final E getSelectedItem ()
	{
		return (index < 0) ? null : items.get (index).getItem ();
	}
	
	/**
	 * Can subclass CycleAction and override this to provide some custom behaviour when an item is chosen
	 */
	protected void selectedItemChanged ()
	{
	}
	
	/**
	 * Select the next item 
	 */
	@Override
	public final void actionPerformed (@SuppressWarnings ("unused") final ActionEvent e)
	{
		if (items.size () > 1)
		{
			index++;
			if (index >= items.size ())
				index = 0;
		
			putValue (Action.NAME, items.get (index).getText ());
			selectedItemChanged ();
		}
	}
	
	/**
	 * Internal class used to hold the name corresponding to each value
	 * 
	 * Used this approach rather than .toString () for one because correctly implementing .toString () on JAXB generated
	 * classes is a pain, have to extend every class and extend the ObjectFactory; and for two because I need to be
	 * able to put nulls into the list.
	 */
	private final class CycleActionItem
	{
		/** Underlying list item */
		private final E item;
		
		/** Corresponding text to display */
		private final String text;
		
		/**
		 * @param anItem Underlying list item
		 * @param aText Corresponding text to display
		 */
		private CycleActionItem (final E anItem, final String aText)
		{
			item = anItem;
			text = aText;
		}

		/**
		 * @return Underlying list item
		 */
		public final E getItem ()
		{
			return item;
		}

		/**
		 * @return Corresponding text to display
		 */
		public final String getText ()
		{
			return text;
		}
	}
}