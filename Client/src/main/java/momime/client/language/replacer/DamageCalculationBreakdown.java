package momime.client.language.replacer;

import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.DamageCalculationMessage;

import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * The values we need for damage calculation breakdowns are in the most part the same as we receive in the
 * message from the server, but we need to store a few extra values besides.
 */
public final class DamageCalculationBreakdown extends DamageCalculationMessage
{
	/** Language entry ID of the text to display for this entry */
	private String languageEntryID;
	
	/** Attacking unit */
	private MemoryUnit attackerUnit;
	
	/** Unit being attacked */
	private MemoryUnit defenderUnit;
	
	/** Player who owns the attacking unit */
	private PlayerPublicDetails attackingPlayer;
	
	/** Player who owns unit being attacked */
	private PlayerPublicDetails defenderPlayer;
	
	/** Spaces to indent this breakdown line */
	private String indent;

	/**
	 * @return Language entry ID of the text to display for this entry
	 */
	public final String getLanguageEntryID ()
	{
		return languageEntryID;
	}

	/**
	 * @param entry Language entry ID of the text to display for this entry
	 */
	public final void setLanguageEntryID (final String entry)
	{
		languageEntryID = entry;
	}

	/**
	 * @return Attacking unit
	 */
	public final MemoryUnit getAttackerUnit ()
	{
		return attackerUnit;
	}

	/**
	 * @param attacker Attacking unit
	 */
	public final void setAttackerUnit (final MemoryUnit attacker)
	{
		attackerUnit = attacker;
	}
	
	/**
	 * @return Unit being attacked
	 */
	public final MemoryUnit getDefenderUnit ()
	{
		return defenderUnit;
	}
	
	/**
	 * @param defender Unit being attacked
	 */
	public final void setDefenderUnit (final MemoryUnit defender)
	{
		defenderUnit = defender;
	}

	/**
	 * @return Player who owns the attacking unit
	 */
	public final PlayerPublicDetails getAttackingPlayer ()
	{
		return attackingPlayer;
	}

	/**
	 * @param player Player who owns the attacking unit
	 */
	public final void setAttackingPlayer (final PlayerPublicDetails player)
	{
		attackingPlayer = player;
	}
	
	/**
	 * @return Player who owns unit being attacked
	 */
	public final PlayerPublicDetails getDefenderPlayer ()
	{
		return defenderPlayer;
	}
	
	/**
	 * @param player Player who owns unit being attacked
	 */
	public final void setDefenderPlayer (final PlayerPublicDetails player)
	{
		defenderPlayer = player;
	}

	/**
	 * @return Spaces to indent this breakdown line
	 */
	public final String getIndent ()
	{
		return (indent == null) ? "" : indent;
	}

	/**
	 * @param ind Spaces to indent this breakdown line
	 */
	public final void setIndent (final String ind)
	{
		indent = ind;
	}
}