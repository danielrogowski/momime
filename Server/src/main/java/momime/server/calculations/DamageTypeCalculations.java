package momime.server.calculations;

import java.util.List;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.server.database.DamageTypeSvr;
import momime.server.database.ServerDatabaseEx;

/**
 * Methods dealing with deciding the damage type of attacks, and dealing with immunities to damage types
 */
public interface DamageTypeCalculations
{
	/**
	 * @param attacker Unit making the attack
	 * @param attackSkillID The skill being used to attack
	 * @param spells Known spells
	 * @param db Lookup lists built over the XML database
	 * @return Damage type dealt by this kind of unit skill
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If there is an error in the game logic
	 */
	public DamageTypeSvr determineSkillDamageType (final MemoryUnit attacker, final String attackSkillID, final List<MemoryMaintainedSpell> spells, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException;
	
	/**
	 * @param defender Unit being hit
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @param divisor Divisor that applies to the unit's actual defence score but NOT to any boosts from immunities; this is used for Armour Piercing, which according to the
	 * 	Wiki applies BEFORE boosts from immunities, e.g. an armour piercing lightning bolt striking magic immune sky drakes has to punch through 50 shields, not 25
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Defence score of the unit vs this incoming attack, taking into account e.g. Fire immunity giving 50 defence vs Fire attacks
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	public int getDefenderDefenceStrength (final MemoryUnit defender, final AttackDamage attackDamage, final int divisor,
		final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;
}