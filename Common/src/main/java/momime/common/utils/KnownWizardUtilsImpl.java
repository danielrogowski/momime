package momime.common.utils;

import java.util.List;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.KnownWizardDetails;

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
}