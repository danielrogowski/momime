package momime.server.worldupdates;

/**
 * Assigns sort order to each kind of update, so for example we always process city recalculations last
 */
enum KindOfWorldUpdate
{
	/** Remove a combat area effect */
	REMOVE_COMBAT_AREA_EFFECT (1),
	
	/** Switching off a spell */
	SWITCH_OFF_SPELL (2),
	
	/** Killing a unit */
	KILL_UNIT (3),
	
	/** Recheck that units in an overland map cell are a valid stack for the type of terrain, e.g.. that there are enough boats to carry everyone that can't swim */
	RECHECK_TRANSPORT_CAPACITY (4),
	
	/** Recalculate all city stats such as size and number of rebels */
	RECALCULATE_CITY (5);	
	
	/** Sort order for this kind of update */
	private final int sortOrder;
	
	/**
	 * @param aSortOrder Sort order for this kind of update
	 */
	KindOfWorldUpdate (int aSortOrder)
	{
		sortOrder = aSortOrder;
	}

	/**
	 * @return Sort order for this kind of update
	 */
	final int getSortOrder ()
	{
		return sortOrder;
	}
}