package momime.client.messages.process;

import momime.common.messages.servertoclient.BroadcastChatMessage;

/**
 * Server bounces this back to all clients for chat messages.
 * PlayerName is not passed as a PlayerID so that chat messages can originate from the server.
 */
public final class BroadcastChatMessageImpl extends BroadcastChatMessage
{

}