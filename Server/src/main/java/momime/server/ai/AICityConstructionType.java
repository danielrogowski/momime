package momime.server.ai;

/**
 * Different types of things we can build in a city
 */
enum AICityConstructionType
{
	/** Any building improvement */
	BUILDING,
	
	/** Build a combat unit at unit factory */
	COMBAT_UNIT,
	
	/** Build a settler */ 
	SETTLER,
	
	/** Build an engineer */
	ENGINEER;
}