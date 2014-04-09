package momime.client.messages.process;

import momime.common.messages.servertoclient.v0_9_5.UpdateRemainingResearchCostMessage;

/**
 * Server sends this to client to update the number of research points they have left to spend before getting a particular spell.
 * This isn't used to set RemainingResearchCost = 0 when research is completed, because when we complete researching a spell, the server also has to
 * randomly pick the 8 further choices of what to research next.  So in that situation we just send the whole fullSpellListMessage again.
 */
public final class UpdateRemainingResearchCostMessageImpl extends UpdateRemainingResearchCostMessage
{

}
