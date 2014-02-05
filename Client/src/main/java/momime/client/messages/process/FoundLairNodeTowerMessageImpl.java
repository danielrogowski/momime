package momime.client.messages.process;

import momime.common.messages.servertoclient.v0_9_4.FoundLairNodeTowerMessage;

/**
 * Server sends this to client when a unit tries to move into a monster-type square, to make the
 * client show the 'Your scouts have spotted Sky Drakes.  Do you want to attack?' message.
 * 
 * Client sends back attackNodeLairTowerMessage if they click Yes.
 * 
 * These will be the actual cells containing the units, so if either side is in a tower, then their plane will say 0.
 */
public final class FoundLairNodeTowerMessageImpl extends FoundLairNodeTowerMessage
{

}
