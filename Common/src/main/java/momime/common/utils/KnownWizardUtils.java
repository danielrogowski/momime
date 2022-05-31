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
public interface KnownWizardUtils
{
	/**
	 * @param knownWizards List of KnownWizardDetails to search
	 * @param playerID playerID to search for
	 * @return Info about the wizard that the requested player is playing as, or null if not found
	 */
	public KnownWizardDetails findKnownWizardDetails (final List<KnownWizardDetails> knownWizards, final int playerID);
	
	/**
	 * @param knownWizards List of KnownWizardDetails to search
	 * @param playerID playerID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem (the server should always send the client details about a wizard before the client may need it)
	 * @return Info about the wizard that the requested player is playing as
	 * @throws RecordNotFoundException If the wizard isn't found in the list
	 */
	public KnownWizardDetails findKnownWizardDetails (final List<KnownWizardDetails> knownWizards, final int playerID, final String caller)
		throws RecordNotFoundException;

	/**
	 * @param pacts List of packs for a player
	 * @param pactPlayerID Player we are interested in the pact with
	 * @return Type of pact in effect; null = there isn't one (default state)
	 */
	public PactType findPactWith (final List<Pact> pacts, final int pactPlayerID);
	
	/**
	 * @param pacts List of packs for a player
	 * @param pactPlayerID Player we are interested in the pact with
	 * @param pactType Type of pact in effect; null = there isn't one (default state)
	 */
	public void updatePactWith (final List<Pact> pacts, final int pactPlayerID, final PactType pactType);
}