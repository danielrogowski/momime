package momime.common.calculations;

/**
 * The different types of 'move' a combat move can result in
 */
public enum CombatMoveType
{
	/** Can't move here, reach here, its too far away, unpassable terrain, or so on */
	CANNOT_MOVE,
	
	/** Can walk here in one turn */
	MOVE,
	
	/** Adjacent enemy unit we can hit */
	MELEE,
	
	/** Enemy unit and we have a ranged attack we can fire at it */
	RANGED;
}
