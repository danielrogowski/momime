package momime.client.messages.process;

import momime.common.messages.servertoclient.v0_9_4.ReplacePicksMessage;

/**
 * Server updating client with the complete list of picks that a particular player now has; this could change because:
 * 1) They've chosen a standard wizard and the server is confirming what picks that standard wizard has;
 * 2) Chosen a custom wizard and server is confirming that the custom picks chosen are OK; or
 * 3) Found a book/retort from a lair during the game.
 */
public final class ReplacePicksMessageImpl extends ReplacePicksMessage
{

}
