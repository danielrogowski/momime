package momime.client.messages.process;

import momime.common.messages.servertoclient.v0_9_5.DamageCalculationMessage;

/**
 * Server telling the two players involved in a combat how damage was calculated.
 * Either attackSkillID or attackAttributeID will be filled in, but not both:
 *		attackSkillID will be filled in if the attack is is a special skill like First Strike, Fire Breath etc.
 *		attackAttributeID will be filled in (UA01 or UA02) if the attack is a standard melee or ranged attack.
 */
public final class DamageCalculationMessageImpl extends DamageCalculationMessage
{

}
