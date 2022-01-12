package momime.client.ui.frames;

/**
 * Different states of us asking the server to automatically control our units in combat
 */
public enum CombatAutoControl
{
	/** We are manually controlling units */
	MANUAL,
	
	/** We've asked the server to automatically control our units this turn */
	AUTO,
	
	/**
	 * Our units were auto controlled this turn, but we want to switch back to manual control next turn.
	 * This is to stop selectNextUnitToMoveCombat sending EndCombatTurnMessage to the server as a result of the client animating automatic moves,
	 * which the server finished rolling ages ago and so the client would end up ending a future turn.
	 */
	SWITCHING_TO_MANUAL;
}