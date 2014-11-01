package momime.client.messages.process;

import momime.common.messages.servertoclient.ScheduledCombatWalkInWithoutAFightMessage;

/**
 * Server sends this to tell client that a combat isn't a combat anymore (that player
 * already captured the target square in another combat) and they can just walk in without a fight if they wish to
 */
public final class ScheduledCombatWalkInWithoutAFightMessageImpl extends ScheduledCombatWalkInWithoutAFightMessage
{

}