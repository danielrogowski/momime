package momime.server.calculations;

import momime.common.database.DamageTypeID;
import momime.server.database.SpellSvr;

/**
 * When passing data from the attack damage calc routines into the defence damage calc routines, we need
 * two pieces of info - the maximum potential damage of the attack, and any +to hit bonuses.
 */
public final class AttackDamage
{
	/**
	 * Potential maximum damage of the attack, if every hit hits and every defence fails; this can be null for unusual kinds of attack, e.g. Warp Wood.
	 * The actual meaning of this value is different between different damage types.  e.g. can be an actual damage amount, a saving throw
	 * modifier, or a % chance.
	 */ 
	private final Integer potentialHits;
	
	/** Any bonus to the standard 30% hit rate */
	private final int plusToHit;
	
	/** Kind of damage being dealt */
	private final DamageTypeID damageType;
	
	/** The spell that's causing the damage; or null if the attack isn't coming from a spell */
	private final SpellSvr spell;
	
	/**
	 * The number of times this damage is dealt - typically 1, but e.g. the "thrown 1" attack of a barbarian unit with 6 figures is actually made as 6 separate
	 * strength 1 attacks, not a single strength 6 attack, according to the MoM wiki at least. 
	 */
	private final int repetitions;
	
	/**
	 * @param aPotentialHits Potential maximum damage of the attack, if every hit hits and every defence fails; this can be null for unusual kinds of attack, e.g. Warp Wood
	 * @param aPlusToHit Any bonus to the standard 30% hit rate
	 * @param aDamageType Kind of damage being dealt
	 * @param aSpell The spell that's causing the damage; or null if the attack isn't coming from a spell
	 * @param aRepetitions The number of times this damage is dealt
	 */
	public AttackDamage (final Integer aPotentialHits, final int aPlusToHit, final DamageTypeID aDamageType, final SpellSvr aSpell, final int aRepetitions)
	{
		super ();
		potentialHits = aPotentialHits;
		plusToHit = aPlusToHit;
		damageType = aDamageType;
		spell = aSpell;
		repetitions = aRepetitions;
	}
	
	/**
	 * @return String representation of class values
	 */
	@Override
	public final String toString ()
	{
		return "(" + getPotentialHits () + " potential damage at +" + getPlusToHit () + " to hit of type " + getDamageType () + ")"; 
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

	/**
	 * @return Kind of damage being dealt
	 */
	public final DamageTypeID getDamageType ()
	{
		return damageType;
	}

	/**
	 * @return The spell that's causing the damage; or null if the attack isn't coming from a spell
	 */
	public final SpellSvr getSpell ()
	{
		return spell;
	}

	/**
	 * @return The number of times this damage is dealt 
	 */
	public final int getRepetitions ()
	{
		return repetitions;
	}
}