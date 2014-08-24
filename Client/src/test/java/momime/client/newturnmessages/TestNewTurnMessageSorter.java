package momime.client.newturnmessages;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * Tests the NewTurnMessageSorter class
 */
public final class TestNewTurnMessageSorter
{
	/**
	 * Tests the compare method
	 */
	@Test
	public final void testCompare ()
	{
		// Build up a test list
		final List<NewTurnMessageUI> msgs = new ArrayList<NewTurnMessageUI> ();
		for (final NewTurnMessageSortOrder s : new NewTurnMessageSortOrder []
			{NewTurnMessageSortOrder.SORT_ORDER_CITY_GROWTH, NewTurnMessageSortOrder.SORT_ORDER_CITY_DEATH,
			NewTurnMessageSortOrder.SORT_ORDER_CONSTRUCTION_COMPLETED, NewTurnMessageSortOrder.SORT_ORDER_CITY_GROWTH})
		{
			final NewTurnMessageUI value = mock (NewTurnMessageUI.class);
			when (value.getSortOrder ()).thenReturn (s);
			msgs.add (value);
		}
		
		// Sort it
		Collections.sort (msgs, new NewTurnMessageSorter ());
		
		// Check values were sorted correctly
		assertEquals (NewTurnMessageSortOrder.SORT_ORDER_CONSTRUCTION_COMPLETED, msgs.get (0).getSortOrder ());
		assertEquals (NewTurnMessageSortOrder.SORT_ORDER_CITY_GROWTH, msgs.get (1).getSortOrder ());
		assertEquals (NewTurnMessageSortOrder.SORT_ORDER_CITY_GROWTH, msgs.get (2).getSortOrder ());
		assertEquals (NewTurnMessageSortOrder.SORT_ORDER_CITY_DEATH, msgs.get (3).getSortOrder ());
	}
}