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
	
	/** To hit chance, in units of 10% */
	private int chanceToHit;
	
	/** Kind of damage being dealt */
	private final DamageTypeID damageType;
	
	/** The spell that's causing the damage; or null if the attack isn't coming from a spell */
	private final SpellSvr spell;

	/** The skill ID of the incoming attack, e.g. bonus from Long Range only activates vs ranged attacks */
	private final String attackFromSkillID;
	
	/** The magic realm of the incoming attack, e.g. bonus from Bless only activates vs Death and Chaos-based attacks */
	private final String attackFromMagicRealmID;
	
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
	 * @param anAttackFromSkillID The skill ID of the incoming attack, e.g. bonus from Long Range only activates vs ranged attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param anAttackFromMagicRealmID The magic realm of the incoming attack, e.g. bonus from Bless only activates vs Death and Chaos-based attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against; ignored for spells
	 * @param aRepetitions The number of times this damage is dealt
	 */
	public AttackDamage (final Integer aPotentialHits, final int aPlusToHit, final DamageTypeID aDamageType, final SpellSvr aSpell,
		final String anAttackFromSkillID, final String anAttackFromMagicRealmID, final int aRepetitions)
	{
		super ();
		potentialHits = aPotentialHits;
		chanceToHit = aPlusToHit + 3;
		damageType = aDamageType;
		spell = aSpell;
		attackFromSkillID = anAttackFromSkillID;
		attackFromMagicRealmID = (spell != null) ? spell.getSpellRealm () : anAttackFromMagicRealmID;
		repetitions = aRepetitions;
	}
	
	/**
	 * @return String representation of class values
	 */
	@Override
	public final String toString ()
	{
		return "(" + getPotentialHits () + " potential damage at +" + getChanceToHit () + " to hit of type " + getDamageType () + ")"; 
	}

	/**
	 * @return Potential maximum damage of the attack, if every hit hits and every defence fails; this can be null for unusual kinds of attack, e.g. Warp Wood
	 */ 
	public final Integer getPotentialHits ()
	{
		return potentialHits;
	}
	
	/**
	 * @return To hit chance, in units of 10%
	 */
	public final int getChanceToHit ()
	{
		return chanceToHit;
	}

	/**
	 * @param chance To hit chance, in units of 10%
	 */
	public final void setChanceToHit (final int chance)
	{
		chanceToHit = chance;
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
	 * @return The skill ID of the incoming attack, e.g. bonus from Long Range only activates vs ranged attacks
	 */
	public final String getAttackFromSkillID ()
	{
		return attackFromSkillID;
	}
	
	/**
	 * @return The magic realm of the incoming attack, e.g. bonus from Bless only activates vs Death and Chaos-based attacks
	 */
	public final String getAttackFromMagicRealmID ()
	{
		return attackFromMagicRealmID;
	}
	
	/**
	 * @return The number of times this damage is dealt 
	 */
	public final int getRepetitions ()
	{
		return repetitions;
	}
}