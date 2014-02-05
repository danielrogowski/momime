package momime.client.messages.process;

import momime.common.messages.servertoclient.v0_9_4.SetUnitIntoOrTakeUnitOutOfCombatMessage;

/**
 * Server sends this to client when a combat is over to take those units out of combat.
 * For taking units out of combat, all the values will be omitted except for the unitURN.
 */
public final class SetUnitIntoOrTakeUnitOutOfCombatMessageImpl extends SetUnitIntoOrTakeUnitOutOfCombatMessage
{

}
