package momime.client.messages.process;

import momime.common.messages.servertoclient.v0_9_5.SelectNextUnitToMoveOverlandMessage;

/**
 * When the server is sending a sequence of messages resulting from a unit moving, it sends this to say that the
 * sequence is over and the client should then ask for movement for the next unit.
 * 
 * So typical sequence is: MoveUnit -> VisAreaChg -> MoveUnit -> VisAreaChg -> SelectNextUnitToMove
 */
public final class SelectNextUnitToMoveOverlandMessageImpl extends SelectNextUnitToMoveOverlandMessage
{

}
