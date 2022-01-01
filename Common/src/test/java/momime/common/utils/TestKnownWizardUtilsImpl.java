package momime.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.KnownWizardDetails;

/**
 * Tests the KnownWizardUtilsImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestKnownWizardUtilsImpl
{
	/**
	 * Tests the findKnownWizardDetails method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFindKnownWizardDetails_Exists () throws Exception
	{
		// Build sample list
		final List<KnownWizardDetails> list = new ArrayList<KnownWizardDetails> ();
		for (int n = 1; n <= 3; n++)
		{
			final KnownWizardDetails details = new KnownWizardDetails ();
			details.setPlayerID (n);
			list.add (details);
		}
		
		// Set up object to test
		final KnownWizardUtilsImpl utils = new KnownWizardUtilsImpl ();
		
		// Run method
		assertEquals (2, utils.findKnownWizardDetails (list, 2).getPlayerID ());
		assertEquals (2, utils.findKnownWizardDetails (list, 2, "Unit test").getPlayerID ());
	}

	/**
	 * Tests the findKnownWizardDetails method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFindKnownWizardDetails_NotExists () throws Exception
	{
		// Build sample list
		final List<KnownWizardDetails> list = new ArrayList<KnownWizardDetails> ();
		for (int n = 1; n <= 3; n++)
		{
			final KnownWizardDetails details = new KnownWizardDetails ();
			details.setPlayerID (n);
			list.add (details);
		}
		
		// Set up object to test
		final KnownWizardUtilsImpl utils = new KnownWizardUtilsImpl ();
		
		// Run method
		assertNull (utils.findKnownWizardDetails (list, 4));
		assertThrows (RecordNotFoundException.class, () ->
		{
			utils.findKnownWizardDetails (list, 4, "Unit test");
		});
	}
}
