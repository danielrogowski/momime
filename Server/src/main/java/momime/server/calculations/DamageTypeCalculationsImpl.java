package momime.server.calculations;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DamageTypeImmunity;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillPositiveNegative;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;
import momime.common.utils.UnitSkillUtils;
import momime.server.database.DamageTypeSvr;
import momime.server.database.PickSvr;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.UnitSvr;
import momime.server.database.UnitTypeSvr;

/**
 * Methods dealing with deciding the damage type of attacks, and dealing with immunities to damage types
 */
public final class DamageTypeCalculationsImpl implements DamageTypeCalculations
{
	/** Class logger */
	private final Log log = LogFactory.getLog (DamageTypeCalculationsImpl.class);
	
	/** Unit skill utils */
	private UnitSkillUtils unitSkillUtils;
	
	/**
	 * @param attacker Unit making the attack
	 * @param attackSkillID The skill being used to attack
	 * @param db Lookup lists built over the XML database
	 * @return Damage type dealt by this kind of unit skill
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 */
	@Override
	public final DamageTypeSvr determineSkillDamageType (final MemoryUnit attacker, final String attackSkillID, final ServerDatabaseEx db)
		throws RecordNotFoundException
	{
		log.trace ("Entering determineSkillDamageType: Unit URN " + attacker.getUnitURN () + " skill " + attackSkillID);

		final UnitSvr unitDef = db.findUnit (attacker.getUnitID (), "determineSkillDamageType");
		
		// Look up basic damage type of skill - if it is the ranged attack skill, then the base damage type comes from the RAT instead
		final String damageTypeID;
		if (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK.equals (attackSkillID))
			damageTypeID = db.findRangedAttackType (unitDef.getRangedAttackType (), "determineSkillDamageType").getDamageTypeID ();
		else
			damageTypeID = db.findUnitSkill (attackSkillID, "determineSkillDamageType").getDamageTypeID ();
		
		DamageTypeSvr damageType = db.findDamageType (damageTypeID, "determineSkillDamageType");
		
		// Does it have an enhanced version?
		if (damageType.getEnhancedVersion () != null)
		{
			// Do we have a unit type or weapon grade that grants the enhanced version?
			final PickSvr magicRealm = db.findPick (unitDef.getUnitMagicRealm (), "determineSkillDamageType");
			final UnitTypeSvr unitType = db.findUnitType (magicRealm.getUnitTypeID (), "determineSkillDamageType");
			boolean enhanced = unitType.isEnhancesDamageType ();
			
			if ((!enhanced) && (attacker.getWeaponGrade () != null))
				enhanced = db.findWeaponGrade (attacker.getWeaponGrade (), "determineSkillDamageType").isEnhancesDamageType ();
			
			if (enhanced)
				damageType = db.findDamageType (damageType.getEnhancedVersion (), "determineSkillDamageType-E");
		}
		
		log.trace ("Exiting determineSkillDamageType = " + damageType.getDamageTypeID ());
		return damageType;
	}
	
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
	@Override
	public final int getDefenderDefenceStrength (final MemoryUnit defender, final AttackDamage attackDamage, final int divisor,
		final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering getDefenderDefenceStrength: Unit URN " + defender.getUnitURN () + " hit by " + attackDamage);

		// Work out basic stat
		int defenderDefenceStrength = Math.max (0, getUnitSkillUtils ().getModifiedSkillValue (defender, defender.getUnitHasSkill (),
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH,
			attackDamage.getAttackFromSkillID (), attackDamage.getAttackFromMagicRealmID (), players, mem, db)) / divisor;
		
		// See if we have any immunity to the type of damage
		for (final DamageTypeImmunity imm : attackDamage.getDamageType ().getDamageTypeImmunity ())
			
			// Total immunity was already dealt with in attackFromUnitSkill
			if ((imm.getBoostsDefenceTo () != null) && (getUnitSkillUtils ().getModifiedSkillValue (defender, defender.getUnitHasSkill (), imm.getUnitSkillID (),
				UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, attackDamage.getAttackFromSkillID (), attackDamage.getAttackFromMagicRealmID (), players, mem, db) >= 0))
				
				defenderDefenceStrength = Math.max (defenderDefenceStrength, imm.getBoostsDefenceTo ());
		
		log.trace ("Exiting getDefenderDefenceStrength = " + defenderDefenceStrength);
		return defenderDefenceStrength;
	}

	/**
	 * @return Unit skill utils
	 */
	public final UnitSkillUtils getUnitSkillUtils ()
	{
		return unitSkillUtils;
	}

	/**
	 * @param utils Unit skill utils
	 */
	public final void setUnitSkillUtils (final UnitSkillUtils utils)
	{
		unitSkillUtils = utils;
	}
}