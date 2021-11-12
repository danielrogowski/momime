package momime.client.ui.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import javax.swing.Action;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests the CycleAction class
 */
@ExtendWith(MockitoExtension.class)
public final class TestCycleAction
{
	/**
	 * The preconditions for a lot of the methods involve calling other methods, so its difficult to separate these out to
	 * make real isolated tests, so instead written this more like a script which more follows the way this would actually get used.
	 */
	@Test
	public final void testCycleAction ()
	{
		// Starts off with no items so should be disabled
		final CycleAction<Integer> action = new CycleAction<Integer> ();
		assertNull (action.getSelectedItem ());
		assertNull (action.getValue (Action.NAME));
		assertFalse (action.isEnabled ());
		
		// Triggering action does nothing with no items (mainly just prove no exception thrown)
		action.actionPerformed (null);
		assertNull (action.getSelectedItem ());
		assertNull (action.getValue (Action.NAME));
		assertFalse (action.isEnabled ());
		
		// Adding an item selects it but leaves action disabled
		action.addItem (1, "One");
		assertEquals (1, action.getSelectedItem ().intValue ());
		assertEquals ("One", action.getValue (Action.NAME));
		assertFalse (action.isEnabled ());
		
		// Triggering action does nothing with only 1 item
		action.actionPerformed (null);
		assertEquals (1, action.getSelectedItem ().intValue ());
		assertEquals ("One", action.getValue (Action.NAME));
		assertFalse (action.isEnabled ());
		
		// Adding another item enables the action
		action.addItem (2, "Two");
		assertEquals (1, action.getSelectedItem ().intValue ());
		assertEquals ("One", action.getValue (Action.NAME));
		assertTrue (action.isEnabled ());
		
		// Triggering action moves to 2nd item
		action.actionPerformed (null);
		assertEquals (2, action.getSelectedItem ().intValue ());
		assertEquals ("Two", action.getValue (Action.NAME));
		assertTrue (action.isEnabled ());

		// Triggering action moves back to 1st item
		action.actionPerformed (null);
		assertEquals (1, action.getSelectedItem ().intValue ());
		assertEquals ("One", action.getValue (Action.NAME));
		assertTrue (action.isEnabled ());
		
		// Prove can add null items with a description
		action.addItem (null, "Three");
		assertEquals (1, action.getSelectedItem ().intValue ());
		assertEquals ("One", action.getValue (Action.NAME));
		assertTrue (action.isEnabled ());
		
		// Triggering action moves to 2nd item
		action.actionPerformed (null);
		assertEquals (2, action.getSelectedItem ().intValue ());
		assertEquals ("Two", action.getValue (Action.NAME));
		assertTrue (action.isEnabled ());
		
		// Triggering action moves to 3rd item
		action.actionPerformed (null);
		assertNull (action.getSelectedItem ());
		assertEquals ("Three", action.getValue (Action.NAME));
		assertTrue (action.isEnabled ());

		// Triggering action moves back to 1st item
		action.actionPerformed (null);
		assertEquals (1, action.getSelectedItem ().intValue ());
		assertEquals ("One", action.getValue (Action.NAME));
		assertTrue (action.isEnabled ());

		// Clearing it wipes everything and disables control again
		action.clearItems ();
		assertNull (action.getSelectedItem ());
		assertNull (action.getValue (Action.NAME));
		assertFalse (action.isEnabled ());
	}	
}
