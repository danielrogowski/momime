package momime.server.messages.process;

import momime.common.messages.clienttoserver.v0_9_4.ChoseNotToDoScheduledCombatMessage;

/**
 * Client sends this if they've initiated a scheduled combat in the movement phase, but now decided they don't want to play it after all.
 * Since you can't choose not to do a combat that you initiated, the only circumstance this happens in is when you've scouted a node/lair/tower and
 * have second thoughts about attacking those sky drakes :)
 */
public final class ChoseNotToDoScheduledCombatMessageImpl extends ChoseNotToDoScheduledCombatMessage
{

}
