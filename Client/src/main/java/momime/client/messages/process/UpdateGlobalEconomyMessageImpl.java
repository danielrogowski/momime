package momime.client.messages.process;

import momime.common.messages.servertoclient.v0_9_4.UpdateGlobalEconomyMessage;

/**
 * Server sends this to each client to tell them what their current production rates and storage are.
 * 
 * This is a good place to send OverlandCastingSkillRemainingThisTurn to the client as well, since any instantly cast spells
 * will result in mana being reduced so new GPVs will need to be sent anyway (and recalc'd in case the new instantly cast spell has some maintenance).
 * 
 * Similarly the OverlandCastingSkillRemainingThisTurn value needs to be set on the client at the start of each turn, so why not include it in the GPV message.
 * 
 * Also both stored mana and OverlandCastingSkillRemainingThisTurn being set on the client simultaneously is convenient
 * for working out EffectiveCastingSkillRemainingThisTurn.
 * 
 * CastingSkillRemainingThisCombat is also sent by the server to avoid having to repeat the skill calc on the client,
 * since new GPVs are sent (to update mana) every time we cast a combat spell.
 */
public final class UpdateGlobalEconomyMessageImpl extends UpdateGlobalEconomyMessage
{

}
