package momime.server.calculations;

import java.util.Iterator;
import java.util.List;

import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DamageType;
import momime.common.database.DamageTypeImmunity;
import momime.common.database.Pick;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitSkillEx;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MomCombatTile;
import momime.common.utils.CombatMapUtils;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.UnitUtils;

/**
 * Methods dealing with deciding the damage type of attacks, and dealing with immunities to damage types
 */
public final class DamageTypeCalculationsImpl implements DamageTypeCalculations
{
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Combat map utils */
	private CombatMapUtils combatMapUtils;
	
	/**
	 * @param attacker Unit making the attack
	 * @param attackSkillID The skill being used to attack
	 * @param db Lookup lists built over the XML database
	 * @return Damage type dealt by this kind of unit skill
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If there is an error in the game logic
	 */
	@Override
	public final DamageType determineSkillDamageType (final ExpandedUnitDetails attacker, final String attackSkillID, final CommonDatabase db)
		throws RecordNotFoundException, MomException
	{
		// Look up basic damage type of skill - if it is the ranged attack skill, then the base damage type comes from the RAT instead
		final String damageTypeID;
		if (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK.equals (attackSkillID))
			damageTypeID = attacker.getRangedAttackType ().getDamageTypeID ();
		else
			damageTypeID = db.findUnitSkill (attackSkillID, "determineSkillDamageType").getDamageTypeID ();
		
		DamageType damageType = db.findDamageType (damageTypeID, "determineSkillDamageType");
		
		// Does it have an enhanced version?
		if (damageType.getEnhancedVersion () != null)
		{
			// Do we have a unit type or weapon grade that grants the enhanced version?
			// Simply being a hero, chaos channeled unit or undead makes attacks negate Weapon Immunity
			final Pick magicRealm = attacker.getModifiedUnitMagicRealmLifeformType ();
			boolean enhanced = (magicRealm.isEnhancesDamageType () != null) && (magicRealm.isEnhancesDamageType ());
			
			// Any weapon grade other than normal weapons also negate Weapon Immunity
			if ((!enhanced) && (attacker.getWeaponGrade () != null))
				enhanced = attacker.getWeaponGrade ().isEnhancesDamageType ();
			
			// Lastly check for any skills that negate Weapon Immunity, e.g. Eldritch Weapon
			if (!enhanced)
			{
				final Iterator<String> iter = attacker.listModifiedSkillIDs ().iterator ();
				while ((!enhanced) && (iter.hasNext ()))
				{
					final UnitSkillEx unitSkill = db.findUnitSkill (iter.next (), "determineSkillDamageType");
					if ((unitSkill.isEnhancesDamageType () != null) && (unitSkill.isEnhancesDamageType ()))
						enhanced = true;
				}
			}
			
			if (enhanced)
				damageType = db.findDamageType (damageType.getEnhancedVersion (), "determineSkillDamageType-E");
		}
		
		return damageType;
	}
	
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
	@Override
	public final int getDefenderDefenceStrength (final ExpandedUnitDetails defender, final ExpandedUnitDetails attacker,
		final AttackDamage attackDamage, final int divisor, final MapCoordinates3DEx combatLocation,
		final MapAreaOfCombatTiles combatMap, final List<MemoryBuilding> trueBuildings, final CommonDatabase db)
		throws MomException, RecordNotFoundException
	{
		// Work out basic stat
		int defenderDefenceStrength = ((divisor == 0) || (!defender.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE))) ? 0 :
			Math.max (0, defender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE)) / divisor;
		
		// See if we have any immunity to the type of damage
		for (final DamageTypeImmunity imm : attackDamage.getDamageType ().getDamageTypeImmunity ())
			
			// Total immunity was already dealt with in attackFromUnitSkill
			if ((imm.getBoostsDefenceTo () != null) && (defender.hasModifiedSkill (imm.getUnitSkillID ())))				
				defenderDefenceStrength = Math.max (defenderDefenceStrength, imm.getBoostsDefenceTo ());
		
		// Defence bonus from being inside city walls?  Still get this even if its an illusionary or armour piercing attack
		if ((attacker != null) &&
			(getCombatMapUtils ().isWithinCityWalls (combatLocation, defender.getCombatPosition (), combatMap, trueBuildings, db)) &&
			(!getCombatMapUtils ().isWithinCityWalls (combatLocation, attacker.getCombatPosition (), combatMap, trueBuildings, db)))
		{
			defenderDefenceStrength++;
			
			// If on tile with intact walls, might get more bonus besides
			final MomCombatTile tile = combatMap.getRow ().get (defender.getCombatPosition ().getY ()).getCell ().get (defender.getCombatPosition ().getX ());
			if ((!tile.isWrecked ()) && (tile.getBorderDirections () != null) && (tile.getBorderDirections ().length () > 0))
				for (final String combatTileBorderID : tile.getBorderID ())
				{
					final Integer bonus = db.findCombatTileBorder (combatTileBorderID, "getDefenderDefenceStrength").getDefenceBonus ();
					if (bonus != null)
						defenderDefenceStrength = defenderDefenceStrength + bonus;
				}
		}
		
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

	/**
	 * @return Combat map utils
	 */
	public final CombatMapUtils getCombatMapUtils ()
	{
		return combatMapUtils;
	}

	/**
	 * @param proc Combat map utils
	 */
	public final void setCombatMapUtils (final CombatMapUtils proc)
	{
		combatMapUtils = proc;
	}
}