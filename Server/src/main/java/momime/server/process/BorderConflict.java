package momime.server.process;

/**
 * Border conflicts a.k.a. counterattacks are when, in simultaneous turns games, two opposing unit stacks both
 * give an order to attack the other one, i.e. A <-> B.  These are processed ahead of other combats, and then
 * the winning side's PendingMovement remains to be executed.
 * 
 * Note not all units in cells A or B may be attacking - so it may take two wins for unit stack A to take location B.
 */
class BorderConflict
{
	/** First pending movement involved */
	private final OneCellPendingMovement firstMove;
	
	/** Second pending movement involved */
	private final OneCellPendingMovement secondMove;
	
	/**
	 * @param aFirstMove First pending movement involved
	 * @param aSecondMove Second pending movement involved
	 */
	BorderConflict (final OneCellPendingMovement aFirstMove, final OneCellPendingMovement aSecondMove)
	{
		super ();
		firstMove = aFirstMove;
		secondMove = aSecondMove;
	}

	/**
	 * @return First pending movement involved
	 */
	public final OneCellPendingMovement getFirstMove ()
	{
		return firstMove;
	}
	
	/**
	 * @return Second pending movement involved
	 */
	public final OneCellPendingMovement getSecondMove ()
	{
		return secondMove;
	}
}