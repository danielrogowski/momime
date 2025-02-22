package momime.client.newturnmessages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests the NewTurnMessageSorter class
 */
@ExtendWith(MockitoExtension.class)
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
			NewTurnMessageSortOrder.SORT_ORDER_CONSTRUCTION, NewTurnMessageSortOrder.SORT_ORDER_CITY_GROWTH})
		{
			final NewTurnMessageUI value = mock (NewTurnMessageUI.class);
			when (value.getSortOrder ()).thenReturn (s);
			msgs.add (value);
		}
		
		// Sort it
		Collections.sort (msgs, new NewTurnMessageSorter ());
		
		// Check values were sorted correctly
		assertEquals (NewTurnMessageSortOrder.SORT_ORDER_CONSTRUCTION, msgs.get (0).getSortOrder ());
		assertEquals (NewTurnMessageSortOrder.SORT_ORDER_CITY_GROWTH, msgs.get (1).getSortOrder ());
		assertEquals (NewTurnMessageSortOrder.SORT_ORDER_CITY_GROWTH, msgs.get (2).getSortOrder ());
		assertEquals (NewTurnMessageSortOrder.SORT_ORDER_CITY_DEATH, msgs.get (3).getSortOrder ());
	}
}