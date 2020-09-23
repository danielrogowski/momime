package momime.server.ai;

/**
 * Different results that can happen from the AI moving one unit in combat
 */
public enum CombatAIMovementResult
{
	/** Found nothing useful to do */
	NOTHING,
	
	/** Found a useful move or attack to do, or cast a spell */
	MOVED_OR_ATTACKED,
	
	/** Attacked or cast a spell and that attack or spell wiped out one or both sides in the combat */
	ENDED_COMBAT;
}