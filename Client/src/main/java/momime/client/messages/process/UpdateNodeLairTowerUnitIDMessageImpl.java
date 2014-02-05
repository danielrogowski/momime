package momime.client.messages.process;

import momime.common.messages.servertoclient.v0_9_4.UpdateNodeLairTowerUnitIDMessage;

/**
 * Server sends this as part of the main FOW message if we need to update our knowledge of what monsters are in
 * nodes/lairs/towers other than by scouting (initiating a combat). Or can send single message
 */
public final class UpdateNodeLairTowerUnitIDMessageImpl extends UpdateNodeLairTowerUnitIDMessage
{

}
