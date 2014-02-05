package momime.client.messages.process;

import momime.common.messages.servertoclient.v0_9_4.SpellResearchChangedMessage;

/**
 * Server sends this back to a client who requested a change in research to let them know the change was OK.
 * This isn't used to set research to 'nothing', so safe to assume that SpellID is non-blank.
 */
public final class SpellResearchChangedMessageImpl extends SpellResearchChangedMessage
{

}
