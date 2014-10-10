package momime.client.utils;

import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.common.messages.v0_9_5.MomPersistentPlayerPublicKnowledge;

import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * Client side only helper methods for dealing with players/wizards
 */
public final class WizardClientUtilsImpl implements WizardClientUtils
{
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;

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
				playerName = getLanguage ().findWizardName (pub.getWizardID ());
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
	 * Convenience shortcut for accessing the Language XML database
	 * @return Language database
	 */
	public final LanguageDatabaseEx getLanguage ()
	{
		return languageHolder.getLanguage ();
	}
}