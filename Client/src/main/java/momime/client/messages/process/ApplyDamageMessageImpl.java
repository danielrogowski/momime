package momime.client.messages.process;

import momime.common.messages.servertoclient.v0_9_5.ApplyDamageMessage;

/**
 * Message server sends to all clients when an attack takes place, this might damage the attacker and/or the defender.
 * For the players actually involved in the combat, this message will also generate the animation to show the units swinging their swords at each other.
 */
public final class ApplyDamageMessageImpl extends ApplyDamageMessage
{

}
