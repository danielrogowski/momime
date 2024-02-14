package momime.server.ai;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

import momime.common.messages.DiplomacyAction;

/**
 * Stores details of one diplomacy proposal an AI player would like to make; assumed its known who the two wizards involved are
 */
public final class DiplomacyProposal
{
	/** Which action we want to take */
	private final DiplomacyAction action;
	
	/** For "propose declare war on other wizard" or "break alliance with other wizard", who the other wizard is */
	private final PlayerServerDetails other;
	
	/** For exchanging spells, which spell we want to request */
	private final String requestSpellID;
	
	/**
	 * @param anAction Which action we want to take
	 * @param anOther For "propose declare war on other wizard" or "break alliance with other wizard", who the other wizard is
	 * @param aRequestSpellID For exchanging spells, which spell we want to request
	 */
	DiplomacyProposal (final DiplomacyAction anAction, final PlayerServerDetails anOther, final String aRequestSpellID)
	{
		action = anAction;
		other = anOther;
		requestSpellID = aRequestSpellID;
	}
	
	/**
	 * @return String representation of values, only used for debug messages
	 */
	@Override
	public final String toString ()
	{
		final StringBuilder s = new StringBuilder ();
		s.append (action);
		
		if (other != null)
			s.append (" with player ID " + other.getPlayerDescription ().getPlayerID ());
		
		if (requestSpellID != null)
			s.append (" spell ID " + requestSpellID);
		
		return s.toString ();
	}
	
	/**
	 * @return Which action we want to take
	 */
	public final DiplomacyAction getAction ()
	{
		return action;
	}
	
	/**
	 * @return For "propose declare war on other wizard" or "break alliance with other wizard", who the other wizard is
	 */
	public final PlayerServerDetails getOther ()
	{
		return other;
	}
	
	/**
	 * @return For exchanging spells, which spell we want to request
	 */
	public final String getRequestSpellID ()
	{
		return requestSpellID;
	}
}