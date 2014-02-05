package momime.client.messages.process;

import momime.common.messages.servertoclient.v0_9_4.UpdateDamageTakenAndExperienceMessage;

/**
 * Server sends this to to update these values without showing any animations.
 * Used when units heal and gain experience at the start of a turn, and when units gain experience during combat.
 */
public final class UpdateDamageTakenAndExperienceMessageImpl extends UpdateDamageTakenAndExperienceMessage
{

}
