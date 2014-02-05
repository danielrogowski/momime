package momime.client.messages.process;

import momime.common.messages.servertoclient.v0_9_4.UpdateProductionSoFarMessage;

/**
 * Server sends this to the owner of a city to tell them how many production points they've put into the current construction project so far.
 * Only the owner of the city gets this - so you cannot tell how much production has been generated from cities that you don't own.
 */
public final class UpdateProductionSoFarMessageImpl extends UpdateProductionSoFarMessage
{

}
