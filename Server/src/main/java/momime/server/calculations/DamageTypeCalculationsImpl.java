package momime.server.calculations;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DamageTypeImmunity;
import momime.common.database.RecordNotFoundException;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.UnitUtils;
import momime.server.database.DamageTypeSvr;
import momime.server.database.PickSvr;
import momime.server.database.RangedAttackTypeSvr;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.WeaponGradeSvr;

/**
 * Methods dealing with deciding the damage type of attacks, and dealing with immunities to damage types
 */
public final class DamageTypeCalculationsImpl implements DamageTypeCalculations
{
	/** Class logger */
	private final Log log = LogFactory.getLog (DamageTypeCalculationsImpl.class);

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/**
	 * @param attacker Unit making the attack
	 * @param attackSkillID The skill being used to attack
	 * @param db Lookup lists built over the XML database
	 * @return Damage type dealt by this kind of unit skill
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If there is an error in the game logic
	 */
	@Override
	public final DamageTypeSvr determineSkillDamageType (final ExpandedUnitDetails attacker, final String attackSkillID, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException
	{
		log.trace ("Entering determineSkillDamageType: " + attacker.getDebugIdentifier () + " skill " + attackSkillID);

		// Look up basic damage type of skill - if it is the ranged attack skill, then the base damage type comes from the RAT instead
		final String damageTypeID;
		if (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK.equals (attackSkillID))
			damageTypeID = ((RangedAttackTypeSvr) attacker.getRangedAttackType ()).getDamageTypeID ();
		else
			damageTypeID = db.findUnitSkill (attackSkillID, "determineSkillDamageType").getDamageTypeID ();
		
		DamageTypeSvr damageType = db.findDamageType (damageTypeID, "determineSkillDamageType");
		
		// Does it have an enhanced version?
		if (damageType.getEnhancedVersion () != null)
		{
			// Do we have a unit type or weapon grade that grants the enhanced version?
			final PickSvr magicRealm = (PickSvr) attacker.getModifiedUnitMagicRealmLifeformType ();
			boolean enhanced = (magicRealm.isEnhancesDamageType () != null) && (magicRealm.isEnhancesDamageType ());
			
			if ((!enhanced) && (attacker.getWeaponGrade () != null))
				enhanced = ((WeaponGradeSvr) attacker.getWeaponGrade ()).isEnhancesDamageType ();
			
			if (enhanced)
				damageType = db.findDamageType (damageType.getEnhancedVersion (), "determineSkillDamageType-E");
		}
		
		log.trace ("Exiting determineSkillDamageType = " + damageType.getDamageTypeID ());
		return damageType;
	}
	
	/**
	 * @param defender Unit being hit; note these details must have been generated for the specific attacker and type of incoming attack in order to be correct
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @param divisor Divisor that applies to the unit's actual defence score but NOT to any boosts from immunities; this is used for Armour Piercing, which according to the
	 * 	Wiki applies BEFORE boosts from immunities, e.g. an armour piercing lightning bolt striking magic immune sky drakes has to punch through 50 shields, not 25
	 * @return Defence score of the unit vs this incoming attack, taking into account e.g. Fire immunity giving 50 defence vs Fire attacks
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	@Override
	public final int getDefenderDefenceStrength (final ExpandedUnitDetails defender, final AttackDamage attackDamage, final int divisor) throws MomException
	{
		log.trace ("Entering getDefenderDefenceStrength: " + defender.getDebugIdentifier () + " hit by " + attackDamage);

		// Work out basic stat
		int defenderDefenceStrength = Math.max (0, defender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE)) / divisor;
		
		// See if we have any immunity to the type of damage
		for (final DamageTypeImmunity imm : attackDamage.getDamageType ().getDamageTypeImmunity ())
			
			// Total immunity was already dealt with in attackFromUnitSkill
			if ((imm.getBoostsDefenceTo () != null) && (defender.hasModifiedSkill (imm.getUnitSkillID ())))				
				defenderDefenceStrength = Math.max (defenderDefenceStrength, imm.getBoostsDefenceTo ());
		
		log.trace ("Exiting getDefenderDefenceStrength = " + defenderDefenceStrength);
		return defenderDefenceStrength;
	}

	/**
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final UnitUtils utils)
	{
		unitUtils = utils;
	}
}