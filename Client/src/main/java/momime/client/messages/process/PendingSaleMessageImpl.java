package momime.client.messages.process;

import momime.common.messages.servertoclient.v0_9_4.PendingSaleMessage;

/**
 * Server sends this in a simultaneous turns game to inform the city owner *only* that a building will be sold at the end of the turn.
 * It can also be sent with buildingID omitted, to cancel selling anything at the end of the turn.
 */
public final class PendingSaleMessageImpl extends PendingSaleMessage
{

}
