package momime.client.messages.process;

import momime.common.messages.servertoclient.v0_9_5.EndOfContinuedMovementMessage;

/**
 * Server sends to client to tell them that it has finished processed their continued unit movement
 * left over from the last turn, and so they can start to allocate new movement.
 * This is only sent for one-at-a-time games - since with simultaneous turns movement, movement is at the end rather than the beginning of a turn.
 * It is also only sent to the player whose turn it now is.
 */
public final class EndOfContinuedMovementMessageImpl extends EndOfContinuedMovementMessage
{

}
