package momime.server.ai;

/**
 * Possible outcomes when the AI tries to decide where to move a unit.
 * Really there's only 3 outcomes that actually affect processing, but made it much more detailed because
 * it makes it easier to follow when debugging or writing unit tests.
 */
enum AIMovementResult
{
	/** Upon more detail check, found stack had no movement left after all so cannot make a move */
	NO_MOVEMENT_LEFT,
	
	/** No movement codes matched and so no destination could be chosen */
	NO_DESTINATION_CHOSEN,
	
	/** Destination chosen but failed to trace route to it */
	NO_ROUTE_TO_DESTINATION,
	
	/** Already at destination so nothing to do */
	ALREADY_AT_DESTINATION,
	
	/** Made valid move */
	MOVED,
	
	/** Made valid move onto enemy units and started a combat in a one-player-at-a-time game */
	MOVED_AND_STARTED_COMBAT,
	
	/** Processed some special order, like settlers turning into a city or engineers building a road */
	PROCESSED_SPECIAL_ORDER,
	
	/** Attempted to process some special order, but validation blocked it */
	PROCESS_SPECIAL_ORDER_FAILED,
	
	/** Destination obj neither had a location or special order set */
	DESTINATION_HAS_NO_VALUE_SET;	
}