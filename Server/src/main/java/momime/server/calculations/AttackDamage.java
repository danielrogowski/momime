package momime.server.calculations;

/**
 * When passing data from the attack damage calc routines into the defence damage calc routines, we need
 * two pieces of info - the maximum potential damage of the attack, and any +to hit bonuses.
 */
public final class AttackDamage
{
	/** Potential maximum damage of the attack, if every hit hits and every defence fails; this can be null for unusual kinds of attack, e.g. Warp Wood */ 
	private final Integer potentialHits;
	
	/** Any bonus to the standard 30% hit rate */
	private final int plusToHit;
	
	/**
	 * @param aPotentialHits Potential maximum damage of the attack, if every hit hits and every defence fails; this can be null for unusual kinds of attack, e.g. Warp Wood
	 * @param aPlusToHit Any bonus to the standard 30% hit rate
	 */
	public AttackDamage (final Integer aPotentialHits, final int aPlusToHit)
	{
		super ();
		potentialHits = aPotentialHits;
		plusToHit = aPlusToHit;
	}
	
	/**
	 * @return String representation of class values
	 */
	@Override
	public final String toString ()
	{
		return "(" + getPotentialHits () + " potential damage at +" + getPlusToHit () + " to hit)"; 
	}

	/**
	 * @return Potential maximum damage of the attack, if every hit hits and every defence fails; this can be null for unusual kinds of attack, e.g. Warp Wood
	 */ 
	public final Integer getPotentialHits ()
	{
		return potentialHits;
	}
	
	/**
	 * @return Any bonus to the standard 30% hit rate
	 */
	public final int getPlusToHit ()
	{
		return plusToHit;
	}
}