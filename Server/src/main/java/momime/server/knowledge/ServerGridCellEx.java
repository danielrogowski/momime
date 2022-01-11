package momime.server.knowledge;

import momime.server.messages.ServerGridCell;

/**
 * Server-side only additional storage required at every map cell, that doesn't need to be persisted into save game files
 */
public final class ServerGridCellEx extends ServerGridCell
{
	/** Whether the lair here was generated as "weak" - this is needed when populating the lair with monsters */ 
	private Boolean lairWeak;
	
	/**
	 * Stores the random roll between 0..1 for where between min and max the various stats of a node/lair/tower are.
	 * e.g. for nodes, this keeps the size of the aura, the strength of the defending monsters, and the quality of treasure reward all at the same level.
	 * Same as "lairWeak", this is only used temporarily during map generation, so doesn't need to be persisted into saved game files. 
	 */
	private Double nodeLairTowerPowerProportion;
	
	/**
	 * @return Whether the lair here was generated as "weak" - this is needed when populating the lair with monsters
	 */ 
	public final Boolean isLairWeak ()
	{
		return lairWeak;
	}

	/**
	 * @param weak Whether the lair here was generated as "weak" - this is needed when populating the lair with monsters
	 */ 
	public final void setLairWeak (final Boolean weak)
	{
		lairWeak = weak;
	}

	/**
	 * @return Random roll between 0..1 for where between min and max the various stats of a node/lair/tower are
	 */
	public final Double getNodeLairTowerPowerProportion ()
	{
		return nodeLairTowerPowerProportion;
	}

	/**
	 * @param prop Random roll between 0..1 for where between min and max the various stats of a node/lair/tower are
	 */
	public final void setNodeLairTowerPowerProportion (final Double prop)
	{
		nodeLairTowerPowerProportion = prop;
	}
}