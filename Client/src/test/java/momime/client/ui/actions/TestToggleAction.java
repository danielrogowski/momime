package momime.client.ui.actions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests the ToggleAction class
 */
@ExtendWith(MockitoExtension.class)
public final class TestToggleAction
{
	/**
	 * This is a lot simpler than TestCycleAction but still made sense to write the test in a similar fashion
	 */
	@Test
	public final void testToggleAction ()
	{
		// Starts disabled
		final ToggleAction action = new ToggleAction ();
		assertFalse (action.isSelected ());
		
		// Turn it on
		action.actionPerformed (null);
		assertTrue (action.isSelected ());

		// Turn it off
		action.actionPerformed (null);
		assertFalse (action.isSelected ());
	}
}
