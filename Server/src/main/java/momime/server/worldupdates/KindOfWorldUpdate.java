package momime.server.worldupdates;

/**
 * Assigns sort order to each kind of update, so for example we always process city recalculations last.
 * 
 * Ordering here is to maintain referential integrity.  If we kill a unit but switch off the spells that were cast on it after, then by the time
 * we process those spells, they will have a unitURN that doesn't exist, which means then we struggle to know which players can "see"
 * the spells.  Basically any update that adds a update higher up the list should return REDO_BECAUSE_EARLIER_UPDATES_ADDED.
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
	RECALCULATE_CITY (5),
	
	/** Recalculate the amount of each type of resource we are generating each turn now, e.g. because upkeep is less because something has been killed off */
	RECALCULATE_PRODUCTION (6);
	
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