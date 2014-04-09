package momime.client.messages.process;

import momime.common.messages.servertoclient.v0_9_5.UpdateUnitToAliveMessage;

/**
 * This is like a reduced version of addUnitMessage.
 * Server sends this to clients to tell them about a new unit that already existed on the map but at an odd status, that is now being set to alive.
 */
public final class UpdateUnitToAliveMessageImpl extends UpdateUnitToAliveMessage
{

}
