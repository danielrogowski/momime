package momime.common.utils;

import java.util.List;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.Pact;
import momime.common.messages.PactType;

/**
 * Methods for finding KnownWizardDetails from the list.
 * When used on client, the list will be obtained from getClient ().getOurPersistentPlayerPrivateKnowledge ().getKnownWizardDetails ().
 * WHen used on server, the list will be obtained from gsk.getTrueWizardDetails ().
 */
public final class KnownWizardUtilsImpl implements KnownWizardUtils
{
	/**
	 * @param knownWizards List of KnownWizardDetails to search
	 * @param playerID playerID to search for
	 * @return Info about the wizard that the requested player is playing as, or null if not found
	 */
	@Override
	public final KnownWizardDetails findKnownWizardDetails (final List<KnownWizardDetails> knownWizards, final int playerID)
	{
		return knownWizards.stream ().filter (d -> d.getPlayerID () == playerID).findAny ().orElse (null);
	}
	
	/**
	 * @param knownWizards List of KnownWizardDetails to search
	 * @param playerID playerID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem (the server should always send the client details about a wizard before the client may need it)
	 * @return Info about the wizard that the requested player is playing as
	 * @throws RecordNotFoundException If the wizard isn't found in the list
	 */
	@Override
	public final KnownWizardDetails findKnownWizardDetails (final List<KnownWizardDetails> knownWizards, final int playerID, final String caller)
		throws RecordNotFoundException
	{
		return knownWizards.stream ().filter (d -> d.getPlayerID () == playerID).findAny ().orElseThrow
			(() -> new RecordNotFoundException (KnownWizardDetails.class, playerID, caller));
	}

	/**
	 * @param pacts List of packs for a player
	 * @param pactPlayerID Player we are interested in the pact with
	 * @return Type of pact in effect; null = there isn't one (default state)
	 */
	@Override
	public final PactType findPactWith (final List<Pact> pacts, final int pactPlayerID)
	{
		 return pacts.stream ().filter (p -> p.getPactWithPlayerID () == pactPlayerID).map (p -> p.getPactType ()).findAny ().orElse (null);
	}
	
	/**
	 * @param pacts List of packs for a player
	 * @param pactPlayerID Player we are interested in the pact with
	 * @param pactType Type of pact in effect; null = there isn't one (default state)
	 */
	@Override
	public final void updatePactWith (final List<Pact> pacts, final int pactPlayerID, final PactType pactType)
	{
		// Look for an existing record
		Pact pact = pacts.stream ().filter (p -> p.getPactWithPlayerID () == pactPlayerID).findAny ().orElse (null);
		
		// What we do with the record we found depends if we're trying to set or clear a value
		if ((pactType == null) && (pact != null))
			pacts.remove (pact);
		
		else if ((pactType != null) && (pact == null))
		{
			pact = new Pact ();
			pact.setPactWithPlayerID (pactPlayerID);
			pact.setPactType (pactType);
			pacts.add (pact);
		}
		
		else if ((pactType != null) && (pact != null) && (pact.getPactType () != pactType))
			pact.setPactType (pactType);
	}
	
	/**
	 * @param maximumGoldTribute Maximum gold tribute allowed, from DiplomacyWizardDetails
	 * @param tier Gold offer tier 1..4
	 * @return Actual gold amount for this tier
	 */
	@Override
	public final int convertGoldOfferTierToAmount (final int maximumGoldTribute, final int tier)
	{
		final int percentage = tier * 25;
		final int baseAmount = (maximumGoldTribute * percentage) / 100;
		final int roundedAmount = (baseAmount / 25) * 25;
		return roundedAmount;
	}
}