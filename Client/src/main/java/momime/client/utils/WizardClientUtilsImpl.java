package momime.client.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.KnownWizardDetails;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.PlayerKnowledgeUtils;

/**
 * Client side only helper methods for dealing with players/wizards
 */
public final class WizardClientUtilsImpl implements WizardClientUtils
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (WizardClientUtilsImpl.class);
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;

	/** Multiplayer client */
	private MomClient client;
	
	/** Methods for working with wizardIDs */
	private PlayerKnowledgeUtils playerKnowledgeUtils;
	
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/**
	 * Human players display as whatever name they've chosen, i.e. the account name associated with their playerID.
	 * AI players pull their wizard name from the language XML file and will only default to the playerName from the
	 * multiplayer layer if we can't find one. 
	 * 
	 * @param player Player to get the name of
	 * @return Displayable player name
	 */
	@Override
	public final String getPlayerName (final PlayerPublicDetails player)
	{
		String playerName = player.getPlayerDescription ().getPlayerName ();
		
		// Now see if we can find a better name for AI players
		if (!player.getPlayerDescription ().isHuman ())
			try
			{
				final KnownWizardDetails wizardDetails = getKnownWizardUtils ().findKnownWizardDetails
					(getClient ().getOurPersistentPlayerPrivateKnowledge ().getKnownWizardDetails (), player.getPlayerDescription ().getPlayerID ());
				
				if ((wizardDetails != null) && (!getPlayerKnowledgeUtils ().isCustomWizard (wizardDetails.getWizardID ())))
					playerName = getLanguageHolder ().findDescription (getClient ().getClientDB ().findWizard (wizardDetails.getWizardID (), "getPlayerName").getWizardName ());
			}
			catch (final RecordNotFoundException e)
			{
				log.error (e, e);
			}
	
		return playerName;
	}

	/**
	 * @return Language database holder
	 */
	public final LanguageDatabaseHolder getLanguageHolder ()
	{
		return languageHolder;
	}
	
	/**
	 * @param holder Language database holder
	 */
	public final void setLanguageHolder (final LanguageDatabaseHolder holder)
	{
		languageHolder = holder;
	}

	/**
	 * @return Multiplayer client
	 */
	public final MomClient getClient ()
	{
		return client;
	}
	
	/**
	 * @param obj Multiplayer client
	 */
	public final void setClient (final MomClient obj)
	{
		client = obj;
	}

	/**
	 * @return Methods for working with wizardIDs
	 */
	public final PlayerKnowledgeUtils getPlayerKnowledgeUtils ()
	{
		return playerKnowledgeUtils;
	}

	/**
	 * @param k Methods for working with wizardIDs
	 */
	public final void setPlayerKnowledgeUtils (final PlayerKnowledgeUtils k)
	{
		playerKnowledgeUtils = k;
	}

	/**
	 * @return Methods for finding KnownWizardDetails from the list
	 */
	public final KnownWizardUtils getKnownWizardUtils ()
	{
		return knownWizardUtils;
	}

	/**
	 * @param k Methods for finding KnownWizardDetails from the list
	 */
	public final void setKnownWizardUtils (final KnownWizardUtils k)
	{
		knownWizardUtils = k;
	}
}