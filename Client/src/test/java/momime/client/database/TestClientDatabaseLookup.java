package momime.client.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import momime.client.database.v0_9_4.ClientDatabase;
import momime.client.database.v0_9_4.MapFeature;
import momime.client.database.v0_9_4.Wizard;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.WizardPick;

import org.junit.Test;

/**
 * Tests the ClientDatabaseLookup class
 * We've already tested the lookups in TestCommonDatabaseLookup - what we're really checking here is that the return types allow direct access to the client-only properties without typecasting
 */
public final class TestClientDatabaseLookup
{
	/**
	 * Tests the findMapFeatureID method to find a mapFeature ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindMapFeatureID_Exists () throws RecordNotFoundException
	{
		final ClientDatabase db = new ClientDatabase ();
		for (int n = 1; n <= 3; n++)
		{
			final MapFeature newMapFeature = new MapFeature ();
			newMapFeature.setMapFeatureID ("MF0" + n);
			newMapFeature.setAnyMagicRealmsDefined (true);
			db.getMapFeature ().add (newMapFeature);
		}

		final ClientDatabaseLookup lookup = new ClientDatabaseLookup (db);

		assertEquals ("MF02", lookup.findMapFeature ("MF02", "testFindMapFeatureID_Exists").getMapFeatureID ());
		assertTrue (lookup.findMapFeature ("MF02", "testFindMapFeatureID_Exists").isAnyMagicRealmsDefined ());
	}

	/**
	 * Tests the findMapFeatureID method to find a mapFeature ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindMapFeatureID_NotExists () throws RecordNotFoundException
	{
		final ClientDatabase db = new ClientDatabase ();
		for (int n = 1; n <= 3; n++)
		{
			final MapFeature newMapFeature = new MapFeature ();
			newMapFeature.setMapFeatureID ("MF0" + n);
			db.getMapFeature ().add (newMapFeature);
		}

		final ClientDatabaseLookup lookup = new ClientDatabaseLookup (db);

		assertNull (lookup.findMapFeature ("MF04", "testFindMapFeatureID_NotExists"));
	}

	/**
	 * Tests the findWizardID method to find a wizard ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindWizardID_Exists () throws RecordNotFoundException
	{
		final ClientDatabase db = new ClientDatabase ();
		for (int n = 1; n <= 3; n++)
		{
			final Wizard newWizard = new Wizard ();
			newWizard.setWizardID ("WZ0" + n);

			final WizardPick pick = new WizardPick ();
			pick.setPick ("MB0" + n);
			pick.setQuantity (n);
			newWizard.getWizardPick ().add (pick);

			db.getWizard ().add (newWizard);
		}

		final ClientDatabaseLookup lookup = new ClientDatabaseLookup (db);

		assertEquals ("WZ02", lookup.findWizard ("WZ02", "testFindWizardID_Exists").getWizardID ());
		assertEquals (1, lookup.findWizard ("WZ02", "testFindWizardID_Exists").getWizardPick ().size ());
		assertEquals ("MB02", lookup.findWizard ("WZ02", "testFindWizardID_Exists").getWizardPick ().get (0).getPick ());
		assertEquals (2, lookup.findWizard ("WZ02", "testFindWizardID_Exists").getWizardPick ().get (0).getQuantity ());
	}

	/**
	 * Tests the findWizardID method to find a wizard ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindWizardID_NotExists () throws RecordNotFoundException
	{
		final ClientDatabase db = new ClientDatabase ();
		for (int n = 1; n <= 3; n++)
		{
			final Wizard newWizard = new Wizard ();
			newWizard.setWizardID ("WZ0" + n);
			db.getWizard ().add (newWizard);
		}

		final ClientDatabaseLookup lookup = new ClientDatabaseLookup (db);

		assertNull (lookup.findWizard ("WZ04", "testFindWizardID_NotExists"));
	}
}
