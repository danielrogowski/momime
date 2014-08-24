package momime.client.newturnmessages;

/**
 * Category header, which displays a title in the new turn messages scroll, e.g. "City Growth" above all the details about city populations that grew this turn.
 */
final class NewTurnMessageCategory implements NewTurnMessageUI
{
	/** One of the SORT_ORDER_ constants, indicating the sort order group that this category is the title for */
	private final NewTurnMessageSortOrder sortOrder;

	/**
	 * @param aSortOrder One of the SORT_ORDER_ constants, indicating the sort order group that this category is the title for
	 */
	NewTurnMessageCategory (final NewTurnMessageSortOrder aSortOrder)
	{
		super ();
		sortOrder = aSortOrder;
	}
	
	/**
	 * @return One of the SORT_ORDER_ constants, indicating the sort order group that this category is the title for
	 */
	@Override
	public final NewTurnMessageSortOrder getSortOrder ()
	{
		return sortOrder;
	}
}