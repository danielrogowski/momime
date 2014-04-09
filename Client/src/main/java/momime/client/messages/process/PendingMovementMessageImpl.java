package momime.client.messages.process;

import momime.common.messages.servertoclient.v0_9_5.PendingMovementMessage;

/**
 * Server sends this to clients who request that units move further than they can reach in one turn, or in "simultaneous turns" mode.
 * This is that tells the client where to draw the white arrows showing the unit stack's intended movement.
 */
public final class PendingMovementMessageImpl extends PendingMovementMessage
{

}
