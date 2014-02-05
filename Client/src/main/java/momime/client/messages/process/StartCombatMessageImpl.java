package momime.client.messages.process;

import momime.common.messages.servertoclient.v0_9_4.StartCombatMessage;

/**
 * Server sends this to the client when they are involved in a combat to start things off - this includes
 * details of all the units in the combat and the terrain, so is probably the most complex multiplayer messages other than the massive FOW message.
 */
public final class StartCombatMessageImpl extends StartCombatMessage
{

}
