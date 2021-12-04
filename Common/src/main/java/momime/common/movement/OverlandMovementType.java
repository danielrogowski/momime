package momime.common.movement;

/**
 * Different ways a unit can move from one overland tile to another.
 * 
 *  Note "Tower of Wizardry" is not a movement type.  Units stood inside Towers are considered to be simultaneously on both planes so they have
 *  no need to make a "move" from one plane to the other.  They just have 16 adjacent cells they can travel to instead of 8.
 *  
 *  Also leaving out naturally Plane Shifting units (Shadow Demons or units with Planar Travel) as they have a complicated situation where they can
 *  attempt to jump to the other plane without knowing if it will actually succeed or not.
 */
public enum OverlandMovementType
{
	/** This is where we started, MP = none */
	START,
	
	/** Walking (or flying, swimming, using road or so on) from one tile to an adjacent tile; MP cost = that of the tile being moved to, or none if enchanted road */
	ADJACENT,
	
	/** Jumping from one city with an earth gate to another city on the same plane also with an earth gate; MP = that of the city being moved to */
	EARTH_GATE,
	
	/** Jumping from one plane to the other at a city with an astral gate, can also be used in reverse from the plane without a city; MP = none */
	ASTRAL_GATE;
}