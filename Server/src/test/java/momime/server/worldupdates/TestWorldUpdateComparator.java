package momime.server.worldupdates;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests the WorldUpdateComparator class
 */
@ExtendWith(MockitoExtension.class)
public final class TestWorldUpdateComparator
{
	/**
	 * Tests the WorldUpdateComparator class
	 */
	@Test
	public final void testWorldUpdateComparator ()
	{
		// Set up some dummy items to sort, in the wrong order
		final WorldUpdate killUnitUpdate = mock (WorldUpdate.class);
		when (killUnitUpdate.getKindOfWorldUpdate ()).thenReturn (KindOfWorldUpdate.KILL_UNIT);
		
		final WorldUpdate recheckTransportCapacityUpdate = mock (WorldUpdate.class);
		when (recheckTransportCapacityUpdate.getKindOfWorldUpdate ()).thenReturn (KindOfWorldUpdate.RECHECK_TRANSPORT_CAPACITY);
		
		final WorldUpdate recalculateCityUpdate = mock (WorldUpdate.class);
		when (recalculateCityUpdate.getKindOfWorldUpdate ()).thenReturn (KindOfWorldUpdate.RECALCULATE_CITY);
		
		final List<WorldUpdate> list = new ArrayList<WorldUpdate> ();
		list.add (recalculateCityUpdate);
		list.add (killUnitUpdate);
		list.add (recheckTransportCapacityUpdate);
		
		// Sort list
		list.sort (new WorldUpdateComparator ());
		
		// Check results
		assertSame (killUnitUpdate, list.get (0));
		assertSame (recheckTransportCapacityUpdate, list.get (1));
		assertSame (recalculateCityUpdate, list.get (2));
	}
}