package momime.server.calculations;

import java.util.List;

import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.DamageType;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MemoryBuilding;
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
	 * @param attacker Unit making the attack if there is one; null if the damage is coming from a spell (even if the spell was cast by a unit)
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @param divisor Divisor that applies to the unit's actual defence score but NOT to any boosts from immunities; this is used for Armour Piercing, which according to the
	 * 	Wiki applies BEFORE boosts from immunities, e.g. an armour piercing lightning bolt striking magic immune sky drakes has to punch through 50 shields, not 25
	 * 	Special value of 0 means no defence score is taken from the unit at all so usually outputs 0, but some special bonuses can still apply.
	 * @param combatLocation Location where the combat is taking place
	 * @param combatMap Combat scenery
	 * @param trueBuildings True list of buildings
	 * @param db Lookup lists built over the XML database
	 * @return Defence score of the unit vs this incoming attack, taking into account e.g. Fire immunity giving 50 defence vs Fire attacks
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws RecordNotFoundException If one of the combat tile border IDs doesn't exist
	 */
	public int getDefenderDefenceStrength (final ExpandedUnitDetails defender, final ExpandedUnitDetails attacker,
		final AttackDamage attackDamage, final int divisor, final MapCoordinates3DEx combatLocation,
		final MapAreaOfCombatTiles combatMap, final List<MemoryBuilding> trueBuildings, final CommonDatabase db)
		throws MomException, RecordNotFoundException;
}