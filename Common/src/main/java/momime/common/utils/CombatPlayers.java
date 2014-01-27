package momime.common.utils;

import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * Stores the attacking and defending players in a particular combat
 */
public final class CombatPlayers
{
	/** Player who is attacking */
	private final PlayerPublicDetails attackingPlayer;
	
	/** Player who is defending */
	private final PlayerPublicDetails defendingPlayer;
	
	/**
	 * @param anAttackingPlayer Player who is attacking
	 * @param aDefendingPlayer Player who is defending
	 */
	CombatPlayers (final PlayerPublicDetails anAttackingPlayer, final PlayerPublicDetails aDefendingPlayer)
	{
		attackingPlayer = anAttackingPlayer;
		defendingPlayer = aDefendingPlayer;
	}	
	
	/**
	 * @return True if both attacking and defending player were determined successfully; false if either is null (which probably means empty lair, or one side has been wiped out)
	 */
	public final boolean bothFound ()
	{
		return ((attackingPlayer != null) && (defendingPlayer != null));
	}

	/**
	 * @return Player who is attacking
	 */
	public final PlayerPublicDetails getAttackingPlayer ()
	{
		return attackingPlayer;
	}
	
	/**
	 * @return Player who is defending
	 */
	public final PlayerPublicDetails getDefendingPlayer ()
	{
		return defendingPlayer;
	}
}
