package momime.client.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;

/**
 * Client side only helper methods for dealing with players/wizards
 */
public final class WizardClientUtilsImpl implements WizardClientUtils
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (WizardClientUtilsImpl.class);
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;

	/** Multiplayer client */
	private MomClient client;
	
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
		{
			final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
			if (pub.getWizardID () != null)
				try
				{
					playerName = getLanguageHolder ().findDescription (getClient ().getClientDB ().findWizard (pub.getWizardID (), "getPlayerName").getWizardName ());
				}
				catch (final RecordNotFoundException e)
				{
					log.error (e, e);
				}
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
}