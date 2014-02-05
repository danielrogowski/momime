package momime.client.messages.process;

import momime.common.messages.servertoclient.v0_9_4.FogOfWarVisibleAreaChangedMessage;

/**
 * Server sends this main message to update the client on changes in their fog of war area and what units, buildings, spells, CAEs, etc. they can see.
 * It basically comprises 0..n of most of the other types of message defined above, sent together so that the client processes them in a single transaction/locked update.
 */
public final class FogOfWarVisibleAreaChangedMessageImpl extends FogOfWarVisibleAreaChangedMessage
{

}
