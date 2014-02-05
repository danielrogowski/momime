package momime.client.messages.process;

import momime.common.messages.servertoclient.v0_9_4.AddCombatAreaEffectMessage;

/**
 * Server sends this to notify clients of new CAEs, or those that have newly come into view.
 * Besides the info we remember, the client also needs the spell ID for animation purposes
 */
public final class AddCombatAreaEffectMessageImpl extends AddCombatAreaEffectMessage
{

}
