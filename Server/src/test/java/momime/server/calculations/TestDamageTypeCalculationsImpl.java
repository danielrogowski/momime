package momime.server.calculations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DamageTypeImmunity;
import momime.common.utils.ExpandedUnitDetails;
import momime.server.database.DamageTypeSvr;
import momime.server.database.PickSvr;
import momime.server.database.RangedAttackTypeSvr;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.UnitSkillSvr;
import momime.server.database.UnitSvr;
import momime.server.database.WeaponGradeSvr;

/**
 * Tests the DamageTypeCalculationsImpl class
 */
public final class TestDamageTypeCalculationsImpl
{
	/**
	 * Tests the determineSkillDamageType method on the regular melee skill with basic weapons
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineSkillDamageType_Melee_BasicWeapons () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final UnitSvr unitDef = new UnitSvr ();
		unitDef.setUnitMagicRealm ("LTN");
		when (db.findUnit ("UN001", "determineSkillDamageType")).thenReturn (unitDef);
		
		final PickSvr magicRealm = new PickSvr ();
		when (db.findPick ("LTN", "determineSkillDamageType")).thenReturn (magicRealm);
		
		final UnitSkillSvr melee = new UnitSkillSvr ();
		melee.setDamageTypeID ("DT01");
		when (db.findUnitSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, "determineSkillDamageType")).thenReturn (melee);
		
		final WeaponGradeSvr weaponGrade = new WeaponGradeSvr ();
		weaponGrade.setEnhancesDamageType (false);
		when (db.findWeaponGrade (0, "determineSkillDamageType")).thenReturn (weaponGrade);

		// Damage types
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		damageType.setEnhancedVersion ("DT02");
		when (db.findDamageType ("DT01", "determineSkillDamageType")).thenReturn (damageType);
		
		final DamageTypeSvr enhanced = new DamageTypeSvr ();
		when (db.findDamageType ("DT02", "determineSkillDamageType-E")).thenReturn (enhanced);
		
		// Unit making the attack
		final ExpandedUnitDetails attacker = mock (ExpandedUnitDetails.class);
		when (attacker.getWeaponGrade ()).thenReturn (weaponGrade);
		when (attacker.getModifiedUnitMagicRealmLifeformType ()).thenReturn (magicRealm);

		// Set up object to test
		final DamageTypeCalculationsImpl calc = new DamageTypeCalculationsImpl ();
		
		// Run method
		assertSame (damageType, calc.determineSkillDamageType (attacker, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, db));
	}
	
	/**
	 * Tests the determineSkillDamageType method on a hero unit, who automatically do enhanced damage which bypasses Weapon Immunity
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineSkillDamageType_Melee_Hero () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final UnitSvr unitDef = new UnitSvr ();
		unitDef.setUnitMagicRealm ("LTH");
		when (db.findUnit ("UN001", "determineSkillDamageType")).thenReturn (unitDef);
		
		final PickSvr magicRealm = new PickSvr ();
		magicRealm.setEnhancesDamageType (true);
		when (db.findPick ("LTH", "determineSkillDamageType")).thenReturn (magicRealm);
		
		final UnitSkillSvr melee = new UnitSkillSvr ();
		melee.setDamageTypeID ("DT01");
		when (db.findUnitSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, "determineSkillDamageType")).thenReturn (melee);
		
		final WeaponGradeSvr weaponGrade = new WeaponGradeSvr ();
		weaponGrade.setEnhancesDamageType (false);
		when (db.findWeaponGrade (0, "determineSkillDamageType")).thenReturn (weaponGrade);

		// Damage types
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		damageType.setEnhancedVersion ("DT02");
		when (db.findDamageType ("DT01", "determineSkillDamageType")).thenReturn (damageType);
		
		final DamageTypeSvr enhanced = new DamageTypeSvr ();
		when (db.findDamageType ("DT02", "determineSkillDamageType-E")).thenReturn (enhanced);
		
		// Unit making the attack
		final ExpandedUnitDetails attacker = mock (ExpandedUnitDetails.class);
		when (attacker.getWeaponGrade ()).thenReturn (weaponGrade);
		when (attacker.getModifiedUnitMagicRealmLifeformType ()).thenReturn (magicRealm);
		
		// Set up object to test
		final DamageTypeCalculationsImpl calc = new DamageTypeCalculationsImpl ();
		
		// Run method
		assertSame (enhanced, calc.determineSkillDamageType (attacker, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, db));
	}
	
	/**
	 * Tests the determineSkillDamageType method on the regular melee skill with a better weapon grade
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineSkillDamageType_Melee_WeaponGrade () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final UnitSvr unitDef = new UnitSvr ();
		unitDef.setUnitMagicRealm ("LTN");
		when (db.findUnit ("UN001", "determineSkillDamageType")).thenReturn (unitDef);
		
		final PickSvr magicRealm = new PickSvr ();
		when (db.findPick ("LTN", "determineSkillDamageType")).thenReturn (magicRealm);
		
		final UnitSkillSvr melee = new UnitSkillSvr ();
		melee.setDamageTypeID ("DT01");
		when (db.findUnitSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, "determineSkillDamageType")).thenReturn (melee);
		
		final WeaponGradeSvr weaponGrade = new WeaponGradeSvr ();
		weaponGrade.setEnhancesDamageType (true);
		when (db.findWeaponGrade (1, "determineSkillDamageType")).thenReturn (weaponGrade);

		// Damage types
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		damageType.setEnhancedVersion ("DT02");
		when (db.findDamageType ("DT01", "determineSkillDamageType")).thenReturn (damageType);
		
		final DamageTypeSvr enhanced = new DamageTypeSvr ();
		when (db.findDamageType ("DT02", "determineSkillDamageType-E")).thenReturn (enhanced);
		
		// Unit making the attack
		final ExpandedUnitDetails attacker = mock (ExpandedUnitDetails.class);
		when (attacker.getWeaponGrade ()).thenReturn (weaponGrade);
		when (attacker.getModifiedUnitMagicRealmLifeformType ()).thenReturn (magicRealm);

		// Set up object to test
		final DamageTypeCalculationsImpl calc = new DamageTypeCalculationsImpl ();
		
		// Run method
		assertSame (enhanced, calc.determineSkillDamageType (attacker, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, db));
	}
	
	/**
	 * Tests the determineSkillDamageType method on a special skill (i.e. one that doesn't have an enhanced version) with a better weapon grade
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineSkillDamageType_Special_WeaponGrade () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final UnitSvr unitDef = new UnitSvr ();
		unitDef.setUnitMagicRealm ("LTN");
		when (db.findUnit ("UN001", "determineSkillDamageType")).thenReturn (unitDef);
		
		final PickSvr magicRealm = new PickSvr ();
		when (db.findPick ("LTN", "determineSkillDamageType")).thenReturn (magicRealm);
		
		final UnitSkillSvr melee = new UnitSkillSvr ();
		melee.setDamageTypeID ("DT01");
		when (db.findUnitSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, "determineSkillDamageType")).thenReturn (melee);
		
		final WeaponGradeSvr weaponGrade = new WeaponGradeSvr ();
		weaponGrade.setEnhancesDamageType (true);
		when (db.findWeaponGrade (1, "determineSkillDamageType")).thenReturn (weaponGrade);

		// Damage types
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		when (db.findDamageType ("DT01", "determineSkillDamageType")).thenReturn (damageType);
		
		final DamageTypeSvr enhanced = new DamageTypeSvr ();
		when (db.findDamageType ("DT02", "determineSkillDamageType-E")).thenReturn (enhanced);
		
		// Unit making the attack
		final ExpandedUnitDetails attacker = mock (ExpandedUnitDetails.class);
		when (attacker.getWeaponGrade ()).thenReturn (weaponGrade);
		when (attacker.getModifiedUnitMagicRealmLifeformType ()).thenReturn (magicRealm);
		
		// Set up object to test
		final DamageTypeCalculationsImpl calc = new DamageTypeCalculationsImpl ();
		
		// Run method
		assertSame (damageType, calc.determineSkillDamageType (attacker, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, db));
	}
	
	/**
	 * Tests the determineSkillDamageType method on a ranged attack with basic weapons
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineSkillDamageType_Ranged_BasicWeapons () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final UnitSvr unitDef = new UnitSvr ();
		unitDef.setUnitMagicRealm ("LTN");
		unitDef.setRangedAttackType ("RAT01");
		when (db.findUnit ("UN001", "determineSkillDamageType")).thenReturn (unitDef);
		
		final PickSvr magicRealm = new PickSvr ();
		when (db.findPick ("LTN", "determineSkillDamageType")).thenReturn (magicRealm);
		
		final RangedAttackTypeSvr rat = new RangedAttackTypeSvr ();
		rat.setDamageTypeID ("DT01");
		when (db.findRangedAttackType ("RAT01", "determineSkillDamageType")).thenReturn (rat);
		
		final WeaponGradeSvr weaponGrade = new WeaponGradeSvr ();
		weaponGrade.setEnhancesDamageType (false);
		when (db.findWeaponGrade (0, "determineSkillDamageType")).thenReturn (weaponGrade);

		// Damage types
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		damageType.setEnhancedVersion ("DT02");
		when (db.findDamageType ("DT01", "determineSkillDamageType")).thenReturn (damageType);
		
		final DamageTypeSvr enhanced = new DamageTypeSvr ();
		when (db.findDamageType ("DT02", "determineSkillDamageType-E")).thenReturn (enhanced);
		
		// Unit making the attack
		final ExpandedUnitDetails attacker = mock (ExpandedUnitDetails.class);
		when (attacker.getWeaponGrade ()).thenReturn (weaponGrade);
		when (attacker.getModifiedUnitMagicRealmLifeformType ()).thenReturn (magicRealm);
		when (attacker.getRangedAttackType ()).thenReturn (rat);
		
		// Set up object to test
		final DamageTypeCalculationsImpl calc = new DamageTypeCalculationsImpl ();
		
		// Run method
		assertSame (damageType, calc.determineSkillDamageType (attacker, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK, db));
	}
	
	/**
	 * Tests the determineSkillDamageType method on a ranged attack from a hero unit, who automatically do enhanced damage which bypasses Weapon Immunity
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineSkillDamageType_Ranged_Hero () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final UnitSvr unitDef = new UnitSvr ();
		unitDef.setUnitMagicRealm ("LTH");
		unitDef.setRangedAttackType ("RAT01");
		when (db.findUnit ("UN001", "determineSkillDamageType")).thenReturn (unitDef);
		
		final PickSvr magicRealm = new PickSvr ();
		magicRealm.setEnhancesDamageType (true);
		when (db.findPick ("LTH", "determineSkillDamageType")).thenReturn (magicRealm);
		
		final RangedAttackTypeSvr rat = new RangedAttackTypeSvr ();
		rat.setDamageTypeID ("DT01");
		when (db.findRangedAttackType ("RAT01", "determineSkillDamageType")).thenReturn (rat);
		
		final WeaponGradeSvr weaponGrade = new WeaponGradeSvr ();
		weaponGrade.setEnhancesDamageType (false);
		when (db.findWeaponGrade (0, "determineSkillDamageType")).thenReturn (weaponGrade);

		// Damage types
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		damageType.setEnhancedVersion ("DT02");
		when (db.findDamageType ("DT01", "determineSkillDamageType")).thenReturn (damageType);
		
		final DamageTypeSvr enhanced = new DamageTypeSvr ();
		when (db.findDamageType ("DT02", "determineSkillDamageType-E")).thenReturn (enhanced);
		
		// Unit making the attack
		final ExpandedUnitDetails attacker = mock (ExpandedUnitDetails.class);
		when (attacker.getWeaponGrade ()).thenReturn (weaponGrade);
		when (attacker.getModifiedUnitMagicRealmLifeformType ()).thenReturn (magicRealm);
		when (attacker.getRangedAttackType ()).thenReturn (rat);
		
		// Set up object to test
		final DamageTypeCalculationsImpl calc = new DamageTypeCalculationsImpl ();
		
		// Run method
		assertSame (enhanced, calc.determineSkillDamageType (attacker, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK, db));
	}
	
	/**
	 * Tests the determineSkillDamageType method on a ranged attack with a better weapon grade
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineSkillDamageType_Ranged_WeaponGrade () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final UnitSvr unitDef = new UnitSvr ();
		unitDef.setUnitMagicRealm ("LTN");
		unitDef.setRangedAttackType ("RAT01");
		when (db.findUnit ("UN001", "determineSkillDamageType")).thenReturn (unitDef);
		
		final PickSvr magicRealm = new PickSvr ();
		when (db.findPick ("LTN", "determineSkillDamageType")).thenReturn (magicRealm);
		
		final RangedAttackTypeSvr rat = new RangedAttackTypeSvr ();
		rat.setDamageTypeID ("DT01");
		when (db.findRangedAttackType ("RAT01", "determineSkillDamageType")).thenReturn (rat);
		
		final WeaponGradeSvr weaponGrade = new WeaponGradeSvr ();
		weaponGrade.setEnhancesDamageType (true);
		when (db.findWeaponGrade (1, "determineSkillDamageType")).thenReturn (weaponGrade);

		// Damage types
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		damageType.setEnhancedVersion ("DT02");
		when (db.findDamageType ("DT01", "determineSkillDamageType")).thenReturn (damageType);
		
		final DamageTypeSvr enhanced = new DamageTypeSvr ();
		when (db.findDamageType ("DT02", "determineSkillDamageType-E")).thenReturn (enhanced);
		
		// Unit making the attack
		final ExpandedUnitDetails attacker = mock (ExpandedUnitDetails.class);
		when (attacker.getWeaponGrade ()).thenReturn (weaponGrade);
		when (attacker.getModifiedUnitMagicRealmLifeformType ()).thenReturn (magicRealm);
		when (attacker.getRangedAttackType ()).thenReturn (rat);
		
		// Set up object to test
		final DamageTypeCalculationsImpl calc = new DamageTypeCalculationsImpl ();
		
		// Run method
		assertSame (enhanced, calc.determineSkillDamageType (attacker, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK, db));
	}
	
	/**
	 * Tests the determineSkillDamageType method on a magic ranged attack (i.e. one that has no enhanced version)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineSkillDamageType_Ranged_Magic () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final UnitSvr unitDef = new UnitSvr ();
		unitDef.setUnitMagicRealm ("LTN");
		unitDef.setRangedAttackType ("RAT01");
		when (db.findUnit ("UN001", "determineSkillDamageType")).thenReturn (unitDef);
		
		final PickSvr magicRealm = new PickSvr ();
		when (db.findPick ("LTN", "determineSkillDamageType")).thenReturn (magicRealm);
		
		final RangedAttackTypeSvr rat = new RangedAttackTypeSvr ();
		rat.setDamageTypeID ("DT01");
		when (db.findRangedAttackType ("RAT01", "determineSkillDamageType")).thenReturn (rat);
		
		final WeaponGradeSvr weaponGrade = new WeaponGradeSvr ();
		weaponGrade.setEnhancesDamageType (true);
		when (db.findWeaponGrade (1, "determineSkillDamageType")).thenReturn (weaponGrade);

		// Damage types
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		when (db.findDamageType ("DT01", "determineSkillDamageType")).thenReturn (damageType);
		
		final DamageTypeSvr enhanced = new DamageTypeSvr ();
		when (db.findDamageType ("DT02", "determineSkillDamageType-E")).thenReturn (enhanced);
		
		// Unit making the attack
		final ExpandedUnitDetails attacker = mock (ExpandedUnitDetails.class);
		when (attacker.getWeaponGrade ()).thenReturn (weaponGrade);
		when (attacker.getModifiedUnitMagicRealmLifeformType ()).thenReturn (magicRealm);
		when (attacker.getRangedAttackType ()).thenReturn (rat);
		
		// Set up object to test
		final DamageTypeCalculationsImpl calc = new DamageTypeCalculationsImpl ();
		
		// Run method
		assertSame (damageType, calc.determineSkillDamageType (attacker, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK, db));
	}
	
	/**
	 * Tests the getDefenderDefenceStrength method when no immunities are triggered
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetDefenderDefenceStrength_NoImmunities () throws Exception
	{
		// Damage being applied
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		
		final AttackDamage attackDamage = new AttackDamage (0, 0, damageType, null, null, null, null, 1);
		
		// Unit being hit
		final ExpandedUnitDetails defender = mock (ExpandedUnitDetails.class);
		when (defender.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE)).thenReturn (true);
		when (defender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE)).thenReturn (5);
		
		// Set up object to test
		final DamageTypeCalculationsImpl calc = new DamageTypeCalculationsImpl ();
		
		// Run method
		assertEquals (5, calc.getDefenderDefenceStrength (defender, attackDamage, 1));
	}

	/**
	 * Tests the getDefenderDefenceStrength method when no immunities are triggered, but the attack is armour piercing
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetDefenderDefenceStrength_ArmourPiercing () throws Exception
	{
		// Damage being applied
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		
		final AttackDamage attackDamage = new AttackDamage (0, 0, damageType, null, null, null, null, 1);
		
		// Unit being hit
		final ExpandedUnitDetails defender = mock (ExpandedUnitDetails.class);
		when (defender.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE)).thenReturn (true);
		when (defender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE)).thenReturn (5);
		
		// Set up object to test
		final DamageTypeCalculationsImpl calc = new DamageTypeCalculationsImpl ();
		
		// Run method
		assertEquals (2, calc.getDefenderDefenceStrength (defender, attackDamage, 2));
	}

	/**
	 * Tests the getDefenderDefenceStrength method when an immunity is defined, but we don't have it
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetDefenderDefenceStrength_ImmunityWeDontHave () throws Exception
	{
		// Damage being applied
		final DamageTypeImmunity imm = new DamageTypeImmunity ();
		imm.setBoostsDefenceTo (50);
		imm.setUnitSkillID ("US001");
		
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		damageType.getDamageTypeImmunity ().add (imm);
		
		final AttackDamage attackDamage = new AttackDamage (0, 0, damageType, null, null, null, null, 1);
		
		// Unit being hit
		final ExpandedUnitDetails defender = mock (ExpandedUnitDetails.class);
		when (defender.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE)).thenReturn (true);
		when (defender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE)).thenReturn (5);
		
		// Set up object to test
		final DamageTypeCalculationsImpl calc = new DamageTypeCalculationsImpl ();
		
		// Run method
		assertEquals (5, calc.getDefenderDefenceStrength (defender, attackDamage, 1));
	}

	/**
	 * Tests the getDefenderDefenceStrength method when an immunity is triggered
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetDefenderDefenceStrength_ImmunityWeDoHave () throws Exception
	{
		// Damage being applied
		final DamageTypeImmunity imm = new DamageTypeImmunity ();
		imm.setBoostsDefenceTo (50);
		imm.setUnitSkillID ("US001");
		
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		damageType.getDamageTypeImmunity ().add (imm);
		
		final AttackDamage attackDamage = new AttackDamage (0, 0, damageType, null, null, null, null, 1);
		
		// Unit being hit
		final ExpandedUnitDetails defender = mock (ExpandedUnitDetails.class);
		when (defender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE)).thenReturn (5);
		when (defender.hasModifiedSkill ("US001")).thenReturn (true);
		
		// Set up object to test
		final DamageTypeCalculationsImpl calc = new DamageTypeCalculationsImpl ();
		
		// Run method
		assertEquals (50, calc.getDefenderDefenceStrength (defender, attackDamage, 1));
	}

	/**
	 * Tests the getDefenderDefenceStrength method to show that armour piercing doesn't apply to immunities
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetDefenderDefenceStrength_ImmunityWithArmourPiercing () throws Exception
	{
		// Damage being applied
		final DamageTypeImmunity imm = new DamageTypeImmunity ();
		imm.setBoostsDefenceTo (50);
		imm.setUnitSkillID ("US001");
		
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		damageType.getDamageTypeImmunity ().add (imm);
		
		final AttackDamage attackDamage = new AttackDamage (0, 0, damageType, null, null, null, null, 1);
		
		// Unit being hit
		final ExpandedUnitDetails defender = mock (ExpandedUnitDetails.class);
		when (defender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE)).thenReturn (5);
		when (defender.hasModifiedSkill ("US001")).thenReturn (true);
		
		// Set up object to test
		final DamageTypeCalculationsImpl calc = new DamageTypeCalculationsImpl ();
		
		// Run method
		assertEquals (50, calc.getDefenderDefenceStrength (defender, attackDamage, 2));
	}
}