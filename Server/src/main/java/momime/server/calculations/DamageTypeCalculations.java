package momime.server.calculations;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.DamageType;
import momime.common.database.RecordNotFoundException;
import momime.common.utils.ExpandedUnitDetails;

/**
 * Methods dealing with deciding the damage type of attacks, and dealing with immunities to damage types
 */
public interface DamageTypeCalculations
{
	/**
	 * @param attacker Unit making the attack
	 * @param attackSkillID The skill being used to attack
	 * @param db Lookup lists built over the XML database
	 * @return Damage type dealt by this kind of unit skill
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If there is an error in the game logic
	 */
	public DamageType determineSkillDamageType (final ExpandedUnitDetails attacker, final String attackSkillID, final CommonDatabase db)
		throws RecordNotFoundException, MomException;
	
	/**
	 * @param defender Unit being hit; note these details must have been generated for the specific attacker and type of incoming attack in order to be correct
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @param divisor Divisor that applies to the unit's actual defence score but NOT to any boosts from immunities; this is used for Armour Piercing, which according to the
	 * 	Wiki applies BEFORE boosts from immunities, e.g. an armour piercing lightning bolt striking magic immune sky drakes has to punch through 50 shields, not 25
	 * @return Defence score of the unit vs this incoming attack, taking into account e.g. Fire immunity giving 50 defence vs Fire attacks
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	public int getDefenderDefenceStrength (final ExpandedUnitDetails defender, final AttackDamage attackDamage, final int divisor) throws MomException;
}