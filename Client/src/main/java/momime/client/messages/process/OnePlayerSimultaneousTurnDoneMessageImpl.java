package momime.client.messages.process;

import momime.common.messages.servertoclient.v0_9_5.OnePlayerSimultaneousTurnDoneMessage;

/**
 * Server sends this to all clients to notify that one player has finished allocating simultaneous movement
 * (so the client can show a 'tick' next to them in the turn bar)
 */
public final class OnePlayerSimultaneousTurnDoneMessageImpl extends OnePlayerSimultaneousTurnDoneMessage
{

}
