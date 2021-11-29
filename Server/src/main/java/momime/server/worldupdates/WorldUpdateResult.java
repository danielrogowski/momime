package momime.server.worldupdates;

/**
 * Result from one WorldUpdate's process method
 */
enum WorldUpdateResult
{
	/** Update was processed, and no more updates were added */
	DONE,
	
	/** Update was processed, and further later updates were added */
	DONE_AND_LATER_UPDATES_ADDED,
	
	/** Update was aborted because updates that should be handled first were added */
	REDO_BECAUSE_EARLIER_UPDATES_ADDED;
}