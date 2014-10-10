package momime.client.utils;

import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * Client side only helper methods for dealing with players/wizards
 */
public interface WizardClientUtils
{
	/**
	 * Human players display as whatever name they've chosen, i.e. the account name associated with their playerID.
	 * AI players pull their wizard name from the language XML file and will only default to the playerName from the
	 * multiplayer layer if we can't find one. 
	 * 
	 * @param player Player to get the name of
	 * @return Displayable player name
	 */
	public String getPlayerName (final PlayerPublicDetails player);
}