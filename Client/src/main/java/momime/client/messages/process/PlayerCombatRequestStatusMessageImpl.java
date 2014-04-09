package momime.client.messages.process;

import momime.common.messages.servertoclient.v0_9_5.PlayerCombatRequestStatusMessage;

/**
 * Server sends this to inform clients whether players are are busy fighting a combat or have
 * requested a particular combat (in either situation, we can't request to play a combat against them)
 */
public final class PlayerCombatRequestStatusMessageImpl extends PlayerCombatRequestStatusMessage
{

}
