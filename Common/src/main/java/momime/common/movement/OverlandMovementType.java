package momime.common.movement;

/**
 * Different ways a unit can move from one overland tile to another 
 */
public enum OverlandMovementType
{
	/** This is where we started, MP = none */
	START,
	
	/** Walking (or flying, swimming, using road or so on) from one tile to an adjacent tile; MP cost = that of the tile being moved to, or none if enchanted road */
	ADJACENT,
	
	/** Jumping from one plane to the other at a Tower of Wizardry; MP = none */
	TOWER_OF_WIZARDRY,
	
	/** Jumping from one city with an earth gate to another city on the same plane also with an earth gate; MP = that of the city being moved to */
	EARTH_GATE,
	
	/** Jumping from one plane to the other at a city with an astral gate, can also be used in reverse from the plane without a city; MP = none same as tower */
	ASTRAL_GATE,
	
	/** Units that can freely jump between the two planes as they wish, e.g. Shadow Demons or units with Planar Travel cast on them; MP = none same as tower */  
	PLANE_SHIFT;
}