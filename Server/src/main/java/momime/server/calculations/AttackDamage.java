package momime.server.calculations;

import momime.common.database.DamageResolutionTypeID;
import momime.common.utils.CompareUtils;
import momime.server.database.DamageTypeSvr;
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
	
	/** Type of damage dealt, for purposes of immunities */
	private final DamageTypeSvr damageType;
	
	/** Rules by which the damage will be applied */
	private final DamageResolutionTypeID damageResolutionTypeID;
	
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
	 * @param aDamageType Type of damage dealt, for purposes of immunities
	 * @param aDamageResolutionTypeID Rules by which the damage will be applied; ignored for spells
	 * @param aSpell The spell that's causing the damage; or null if the attack isn't coming from a spell
	 * @param anAttackFromSkillID The skill ID of the incoming attack, e.g. bonus from Long Range only activates vs ranged attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param anAttackFromMagicRealmID The magic realm of the incoming attack, e.g. bonus from Bless only activates vs Death and Chaos-based attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against; ignored for spells
	 * @param aRepetitions The number of times this damage is dealt
	 */
	public AttackDamage (final Integer aPotentialHits, final int aPlusToHit, final DamageTypeSvr aDamageType, final DamageResolutionTypeID aDamageResolutionTypeID,
		final SpellSvr aSpell, final String anAttackFromSkillID, final String anAttackFromMagicRealmID, final int aRepetitions)
	{
		super ();
		potentialHits = aPotentialHits;
		chanceToHit = aPlusToHit + 3;
		spell = aSpell;
		attackFromSkillID = anAttackFromSkillID;
		attackFromMagicRealmID = (spell != null) ? spell.getSpellRealm () : anAttackFromMagicRealmID;
		damageType = aDamageType;
		damageResolutionTypeID = (spell != null) ? spell.getAttackSpellDamageResolutionTypeID () : aDamageResolutionTypeID;
		repetitions = aRepetitions;
	}
	
	/**
	 * @param src AttackDamage object to copy value sfrom
	 * @param aDamageResolutionTypeID Override for rules by which the damage will be applied 
	 */
	public AttackDamage (final AttackDamage src, final DamageResolutionTypeID aDamageResolutionTypeID)
	{
		super ();
		potentialHits = src.getPotentialHits ();
		chanceToHit = src.getChanceToHit ();
		damageType = src.getDamageType ();
		spell = src.getSpell ();
		attackFromSkillID = src.getAttackFromSkillID ();
		attackFromMagicRealmID = src.getAttackFromMagicRealmID ();
		repetitions = src.getRepetitions ();

		damageResolutionTypeID = aDamageResolutionTypeID;
	}
	
	/**
	 * @return String representation of class values
	 */
	@Override
	public final String toString ()
	{
		return "(" + getPotentialHits () + " potential damage at +" + getChanceToHit () + " to hit of type " +
			((getDamageType () == null) ? "null" : getDamageType ().getDamageTypeID ()) + " as " + getDamageResolutionTypeID () + ")"; 
	}

	/**
	 * Needed to make matchers in unit tests work correctly
	 */
	@Override
	public final boolean equals (final Object o)
	{
		final boolean result;
		if (o instanceof AttackDamage)
		{
			final AttackDamage a = (AttackDamage) o; 
			result = (CompareUtils.safeIntegerCompare (getPotentialHits (), a.getPotentialHits ())) &&
				(getChanceToHit () == a.getChanceToHit ()) && (getDamageType () == a.getDamageType ()) && (getSpell () == a.getSpell ()) &&
				(CompareUtils.safeStringCompare (getAttackFromSkillID (), a.getAttackFromSkillID ())) && (getRepetitions () == a.getRepetitions ()) &&
				(CompareUtils.safeStringCompare (getAttackFromMagicRealmID (), a.getAttackFromMagicRealmID ())) &&
				(getDamageResolutionTypeID () == a.getDamageResolutionTypeID ());
		}
		else
			result = super.equals (o);
		
		return result;
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
	 * @return Type of damage dealt, for purposes of immunities
	 */
	public final DamageTypeSvr getDamageType ()
	{
		return damageType;
	}
	
	/**
	 * @return Rules by which the damage will be applied
	 */
	public final DamageResolutionTypeID getDamageResolutionTypeID ()
	{
		return damageResolutionTypeID;
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