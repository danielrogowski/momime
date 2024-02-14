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
import momime.common.messages.Pact;
import momime.common.messages.PactType;

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
	
	/**
	 * Tests the findPactWith method when there is a pact between the two wizards
	 */
	@Test
	public final void testFindPactWith_Exists ()
	{
		// List of pacts
		final List<Pact> pacts = new ArrayList<Pact> ();
		
		int n = 0;
		for (final PactType pactType : PactType.values ())
		{
			n++;
			final Pact pact = new Pact ();
			pact.setPactType (pactType);
			pact.setPactWithPlayerID (n);
			pacts.add (pact);
		}
		
		// Set up object to test
		final KnownWizardUtilsImpl utils = new KnownWizardUtilsImpl ();
		
		// Run method
		assertEquals (PactType.ALLIANCE, utils.findPactWith (pacts, 2));
	}
	
	/**
	 * Tests the findPactWith method when there is no pact between the two wizards
	 */
	@Test
	public final void testFindPactWith_NotExists ()
	{
		// List of pacts
		final List<Pact> pacts = new ArrayList<Pact> ();
		
		int n = 0;
		for (final PactType pactType : PactType.values ())
		{
			n++;
			final Pact pact = new Pact ();
			pact.setPactType (pactType);
			pact.setPactWithPlayerID (n);
			pacts.add (pact);
		}
		
		// Set up object to test
		final KnownWizardUtilsImpl utils = new KnownWizardUtilsImpl ();
		
		// Run method
		assertNull (utils.findPactWith (pacts, 4));
	}

	/**
	 * Tests the findPactWith method when there is a pact between the two wizards, and we're updating it to a different kind of pact
	 */
	@Test
	public final void testUpdatePactWith_changePactType ()
	{
		// List of pacts
		final List<Pact> pacts = new ArrayList<Pact> ();
		
		int n = 0;
		for (final PactType pactType : PactType.values ())
		{
			n++;
			final Pact pact = new Pact ();
			pact.setPactType (pactType);
			pact.setPactWithPlayerID (n);
			pacts.add (pact);
		}
		
		// Set up object to test
		final KnownWizardUtilsImpl utils = new KnownWizardUtilsImpl ();
		
		// Run method
		utils.updatePactWith (pacts, 2, PactType.WAR);
		
		// Check results
		assertEquals (3, pacts.size ());
		assertEquals (1, pacts.get (0).getPactWithPlayerID ());
		assertEquals (PactType.WIZARD_PACT, pacts.get (0).getPactType ());
		assertEquals (2, pacts.get (1).getPactWithPlayerID ());
		assertEquals (PactType.WAR, pacts.get (1).getPactType ());
		assertEquals (3, pacts.get (2).getPactWithPlayerID ());
		assertEquals (PactType.WAR, pacts.get (2).getPactType ());
	}

	/**
	 * Tests the findPactWith method when there is a pact between the two wizards, and we're removing it
	 */
	@Test
	public final void testUpdatePactWith_removePact ()
	{
		// List of pacts
		final List<Pact> pacts = new ArrayList<Pact> ();
		
		int n = 0;
		for (final PactType pactType : PactType.values ())
		{
			n++;
			final Pact pact = new Pact ();
			pact.setPactType (pactType);
			pact.setPactWithPlayerID (n);
			pacts.add (pact);
		}
		
		// Set up object to test
		final KnownWizardUtilsImpl utils = new KnownWizardUtilsImpl ();
		
		// Run method
		utils.updatePactWith (pacts, 2, null);
		
		// Check results
		assertEquals (2, pacts.size ());
		assertEquals (1, pacts.get (0).getPactWithPlayerID ());
		assertEquals (PactType.WIZARD_PACT, pacts.get (0).getPactType ());
		assertEquals (3, pacts.get (1).getPactWithPlayerID ());
		assertEquals (PactType.WAR, pacts.get (1).getPactType ());
	}

	/**
	 * Tests the findPactWith method when there isn't a pact between the two wizards, and we're keeping it that way
	 */
	@Test
	public final void testUpdatePactWith_DontAddPact ()
	{
		// List of pacts
		final List<Pact> pacts = new ArrayList<Pact> ();
		
		int n = 0;
		for (final PactType pactType : PactType.values ())
		{
			n++;
			final Pact pact = new Pact ();
			pact.setPactType (pactType);
			pact.setPactWithPlayerID (n);
			pacts.add (pact);
		}
		
		// Set up object to test
		final KnownWizardUtilsImpl utils = new KnownWizardUtilsImpl ();
		
		// Run method
		utils.updatePactWith (pacts, 4, null);
		
		// Check results
		assertEquals (3, pacts.size ());
		assertEquals (1, pacts.get (0).getPactWithPlayerID ());
		assertEquals (PactType.WIZARD_PACT, pacts.get (0).getPactType ());
		assertEquals (2, pacts.get (1).getPactWithPlayerID ());
		assertEquals (PactType.ALLIANCE, pacts.get (1).getPactType ());
		assertEquals (3, pacts.get (2).getPactWithPlayerID ());
		assertEquals (PactType.WAR, pacts.get (2).getPactType ());
	}

	/**
	 * Tests the findPactWith method when there isn't a pact between the two wizards, and we're adding one
	 */
	@Test
	public final void testUpdatePactWith_AddPact ()
	{
		// List of pacts
		final List<Pact> pacts = new ArrayList<Pact> ();
		
		int n = 0;
		for (final PactType pactType : PactType.values ())
		{
			n++;
			final Pact pact = new Pact ();
			pact.setPactType (pactType);
			pact.setPactWithPlayerID (n);
			pacts.add (pact);
		}
		
		// Set up object to test
		final KnownWizardUtilsImpl utils = new KnownWizardUtilsImpl ();
		
		// Run method
		utils.updatePactWith (pacts, 4, PactType.ALLIANCE);
		
		// Check results
		assertEquals (4, pacts.size ());
		assertEquals (1, pacts.get (0).getPactWithPlayerID ());
		assertEquals (PactType.WIZARD_PACT, pacts.get (0).getPactType ());
		assertEquals (2, pacts.get (1).getPactWithPlayerID ());
		assertEquals (PactType.ALLIANCE, pacts.get (1).getPactType ());
		assertEquals (3, pacts.get (2).getPactWithPlayerID ());
		assertEquals (PactType.WAR, pacts.get (2).getPactType ());
		assertEquals (4, pacts.get (3).getPactWithPlayerID ());
		assertEquals (PactType.ALLIANCE, pacts.get (3).getPactType ());
	}
	
	/**
	 * Tests the convertGoldOfferTierToAmount method
	 */
	@Test
	public final void testConvertGoldOfferTierToAmount ()
	{
		// Set up object to test
		final KnownWizardUtilsImpl utils = new KnownWizardUtilsImpl ();
		
		// Run method
		assertEquals (25, utils.convertGoldOfferTierToAmount (180, 1));		// 180/4 = 45, but they're always rounded down to a multiple of 25 
		assertEquals (75, utils.convertGoldOfferTierToAmount (180, 2));		// 180/2 = 90 
		assertEquals (125, utils.convertGoldOfferTierToAmount (180, 3));		// 180*3/4 = 135 
		assertEquals (175, utils.convertGoldOfferTierToAmount (180, 4));		// 180 
	}
}