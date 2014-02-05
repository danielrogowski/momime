package momime.client.messages.process;

import momime.common.messages.servertoclient.v0_9_4.KillUnitMessage;

/**
 * Server sends this to everyone to notify of dead units, except where it is already obvious from an Apply Damage message that a unit is dead
 */
public final class KillUnitMessageImpl extends KillUnitMessage
{

}
