package momime.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.calculations.UnitHasSkillMergedList;
import momime.common.database.AddsToSkill;
import momime.common.database.CombatAreaEffect;
import momime.common.database.CombatAreaEffectSkillBonus;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.database.NegatedBySkill;
import momime.common.database.NegatedByUnitID;
import momime.common.database.UnitSkill;
import momime.common.database.UnitSkillAndValue;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillPositiveNegative;
import momime.common.database.WeaponGrade;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;

/**
 * Tests the UnitSkillUtils class
 */
public final class TestUnitSkillUtilsImpl
{
	/**
	 * Tests the addToSkillValue method
	 */
	@Test
	public final void testAddToSkillValue ()
	{
		final UnitSkillUtilsImpl utils = new UnitSkillUtilsImpl ();
		
		assertEquals (5, utils.addToSkillValue (5, UnitSkillPositiveNegative.BOTH));
		assertEquals (0, utils.addToSkillValue (0, UnitSkillPositiveNegative.BOTH));
		assertEquals (-5, utils.addToSkillValue (-5, UnitSkillPositiveNegative.BOTH));
		
		assertEquals (5, utils.addToSkillValue (5, UnitSkillPositiveNegative.POSITIVE));
		assertEquals (0, utils.addToSkillValue (0, UnitSkillPositiveNegative.POSITIVE));
		assertEquals (0, utils.addToSkillValue (-5, UnitSkillPositiveNegative.POSITIVE));
		
		assertEquals (0, utils.addToSkillValue (5, UnitSkillPositiveNegative.NEGATIVE));
		assertEquals (0, utils.addToSkillValue (0, UnitSkillPositiveNegative.NEGATIVE));
		assertEquals (-5, utils.addToSkillValue (-5, UnitSkillPositiveNegative.NEGATIVE));
	}	

	/**
	 * Tests the getModifiedSkillValue method requesting each different component of the skill value
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetModifiedSkillValue_Components () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final WeaponGrade weaponGradeDef = new WeaponGrade ();
		for (final String unitSkillID : new String [] {"US002", "US004"})
		{
			final UnitSkillAndValue bonus = new UnitSkillAndValue ();
			bonus.setUnitSkillID (unitSkillID);
			bonus.setUnitSkillValue (10);
			weaponGradeDef.getWeaponGradeSkillBonus ().add (bonus);
		}
		when (db.findWeaponGrade (1, "getModifiedSkillValue")).thenReturn (weaponGradeDef);
		
		final CombatAreaEffect caeDef = new CombatAreaEffect ();
		for (final String unitSkillID : new String [] {"US002", "US004"})
		{
			final CombatAreaEffectSkillBonus bonus = new CombatAreaEffectSkillBonus ();
			bonus.setUnitSkillID (unitSkillID);
			bonus.setUnitSkillValue (1000);
			caeDef.getCombatAreaEffectSkillBonus ().add (bonus);
		}
		when (db.findCombatAreaEffect ("CAE01", "getModifiedSkillValue")).thenReturn (caeDef);
		
		final AddsToSkill heroSkillBonus = new AddsToSkill ();
		heroSkillBonus.setAddsToSkillID ("US004");
		heroSkillBonus.setAddsToSkillDivisor (2);
		
		final UnitSkill heroSkill = new UnitSkill ();
		heroSkill.setUnitSkillID ("HS001");
		heroSkill.getAddsToSkill ().add (heroSkillBonus);

		final AddsToSkill spellSkillBonus = new AddsToSkill ();
		spellSkillBonus.setAddsToSkillID ("US004");
		spellSkillBonus.setAddsToSkillFixed (10000);
		
		final UnitSkill spellSkill = new UnitSkill ();
		spellSkill.setUnitSkillID ("SS001");
		spellSkill.getAddsToSkill ().add (spellSkillBonus);
		
		final List<UnitSkill> skills = new ArrayList<UnitSkill> ();
		skills.add (heroSkill);
		skills.add (spellSkill);
		doReturn (skills).when (db).getUnitSkills ();
		
		for (final String unitSkillID : new String [] {"US001", "US002", "US003", "US004", "HS001", "SS001"})
		{
			final UnitSkill unitSkill = new UnitSkill ();
			when (db.findUnitSkill (unitSkillID, "getModifiedSkillValue")).thenReturn (unitSkill);
		}
		
		// Lists
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final FogOfWarMemory fow = new FogOfWarMemory ();

		// Sample unit
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final AvailableUnit unit = new AvailableUnit ();
		unit.setWeaponGrade (1);
		when (unitUtils.getModifiedUnitMagicRealmLifeformTypeID (unit, unit.getUnitHasSkill (), fow.getMaintainedSpell (), db)).thenReturn ("A");
		
		// Base skill values
		when (unitUtils.getBasicSkillValue (unit.getUnitHasSkill (), "US001")).thenReturn (-1);
		when (unitUtils.getBasicSkillValue (unit.getUnitHasSkill (), "US002")).thenReturn (-1);
		when (unitUtils.getBasicSkillValue (unit.getUnitHasSkill (), "US003")).thenReturn (1);
		when (unitUtils.getBasicSkillValue (unit.getUnitHasSkill (), "US004")).thenReturn (1);
		when (unitUtils.getBasicSkillValue (unit.getUnitHasSkill (), "HS001")).thenReturn (1);
		when (unitUtils.getBasicSkillValue (unit.getUnitHasSkill (), "SS001")).thenReturn (0);
		
		// Experience level
		final ExperienceLevel expLvl = new ExperienceLevel ();
		expLvl.setLevelNumber (5);		// Makes the hero skill give 6 / 2 (see setAddsToAttributeDivisor above) = +3
		
		for (final String unitSkillID : new String [] {"US002", "US004"})
		{
			final UnitSkillAndValue bonus = new UnitSkillAndValue ();
			bonus.setUnitSkillID (unitSkillID);
			bonus.setUnitSkillValue (100);
			expLvl.getExperienceSkillBonus ().add (bonus);
		}
		when (unitUtils.getExperienceLevel (unit, true, players, fow.getCombatAreaEffect (), db)).thenReturn (expLvl);
		
		// CAE
		final MemoryCombatAreaEffect cae = new MemoryCombatAreaEffect ();
		cae.setCombatAreaEffectID ("CAE01");
		
		when (unitUtils.doesCombatAreaEffectApplyToUnit (unit, cae, db)).thenReturn (true);
		fow.getCombatAreaEffect ().add (cae);
		
		// Set up object to test
		final UnitSkillUtilsImpl utils = new UnitSkillUtilsImpl ();
		utils.setUnitUtils (unitUtils);

		// Skill that we don't have
		assertEquals (-1, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US001", null,
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db));
		
		// Skill that we don't have so bonuses to it still don't apply
		assertEquals (-1, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US002", null,
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db));

		// Skill that we do have, to which no bonuses apply
		assertEquals (1, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US003", null,
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db));
		
		// Skill that we do have, and has weapon grade, experience and CAE bonuses
		assertEquals (11114, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US004", null,
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db));
		
		// Test asking for specific breakdown components		
		assertEquals (1, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US004", null,
			UnitSkillComponent.BASIC, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db));

		assertEquals (10, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US004", null,
			UnitSkillComponent.WEAPON_GRADE, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db));

		assertEquals (100, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US004", null,
			UnitSkillComponent.EXPERIENCE, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db));
		
		assertEquals (1000, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US004", null,
			UnitSkillComponent.COMBAT_AREA_EFFECTS, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db));

		assertEquals (10000, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US004", null,
			UnitSkillComponent.SPELL_EFFECTS, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db));
		
		assertEquals (3, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US004", null,
			UnitSkillComponent.HERO_SKILLS, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db));
	}

	/**
	 * Tests the getModifiedSkillValue method on bonuses which only apply to certain types of incoming attack, e.g. Resist Elements and Large Shield
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetModifiedSkillValue_ConditionalBonuses () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final AddsToSkill largeShieldBonus = new AddsToSkill ();
		largeShieldBonus.setAddsToSkillID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE);
		largeShieldBonus.setAddsToSkillFixed (2);
		largeShieldBonus.setOnlyVersusAttacksFromSkillID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK);
		
		final UnitSkill largeShield = new UnitSkill ();
		largeShield.getAddsToSkill ().add (largeShieldBonus);
		largeShield.setUnitSkillID ("US001");
		when (db.findUnitSkill ("US001", "getModifiedSkillValue")).thenReturn (largeShield);

		final AddsToSkill resistElementsBonus = new AddsToSkill ();
		resistElementsBonus.setAddsToSkillID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE);
		resistElementsBonus.setAddsToSkillFixed (3);
		resistElementsBonus.setOnlyVersusAttacksFromMagicRealmID ("MB02");
		
		final UnitSkill resistElements = new UnitSkill ();
		resistElements.getAddsToSkill ().add (resistElementsBonus);
		resistElements.setUnitSkillID ("US002");
		when (db.findUnitSkill ("US002", "getModifiedSkillValue")).thenReturn (resistElements);

		final UnitSkill thrownWeapons = new UnitSkill ();
		thrownWeapons.setUnitSkillID ("US003");
		when (db.findUnitSkill ("US003", "getModifiedSkillValue")).thenReturn (thrownWeapons);

		final UnitSkill rangedAttack = new UnitSkill ();
		rangedAttack.setUnitSkillID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK);
		when (db.findUnitSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK, "getModifiedSkillValue")).thenReturn (rangedAttack);
		
		final UnitSkill defence = new UnitSkill ();
		defence.setUnitSkillID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE);
		when (db.findUnitSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE, "getModifiedSkillValue")).thenReturn (defence);
		
		final List<UnitSkill> skills = new ArrayList<UnitSkill> ();
		skills.add (largeShield);
		skills.add (resistElements);
		skills.add (thrownWeapons);
		skills.add (rangedAttack);
		skills.add (defence);
		doReturn (skills).when (db).getUnitSkills ();
		
		// Lists
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Sample unit
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final AvailableUnit unit = new AvailableUnit ();
		when (unitUtils.getModifiedUnitMagicRealmLifeformTypeID (unit, unit.getUnitHasSkill (), fow.getMaintainedSpell (), db)).thenReturn ("A");

		// Set up object to test
		final UnitSkillUtilsImpl utils = new UnitSkillUtilsImpl ();
		utils.setUnitUtils (unitUtils);

		// Start off without either skill
		when (unitUtils.getBasicSkillValue (unit.getUnitHasSkill (), "US001")).thenReturn (-1);
		when (unitUtils.getBasicSkillValue (unit.getUnitHasSkill (), "US002")).thenReturn (-1);
		when (unitUtils.getBasicSkillValue (unit.getUnitHasSkill (), CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE)).thenReturn (1);

		// Large Shield
		assertEquals (1, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE, null,
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db));
		assertEquals (1, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE, null,
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, null, players, fow, db));
		assertEquals (1, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE, null,
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, "US003", null, players, fow, db));
		assertEquals (1, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE, null,
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK, null, players, fow, db));

		when (unitUtils.getBasicSkillValue (unit.getUnitHasSkill (), "US001")).thenReturn (0);
		
		assertEquals (1, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE, null,
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db));
		assertEquals (1, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE, null,
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, null, players, fow, db));
		assertEquals (1, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE, null,
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, "US003", null, players, fow, db));
		assertEquals (3, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE, null,
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK, null, players, fow, db));
		
		// Large Shield isn't really implemented like that - its implemented as "skillID is anything other than Melee", so that it works for thrown, etc too
		largeShieldBonus.setOnlyVersusAttacksFromSkillID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK);
		largeShieldBonus.setNegateOnlyVersusAttacksFromSkillID (true);

		assertEquals (1, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE, null,
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db));
		assertEquals (1, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE, null,
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, null, players, fow, db));
		assertEquals (3, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE, null,		// <---
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, "US003", null, players, fow, db));
		assertEquals (3, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE, null,
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK, null, players, fow, db));
		
		// Resist Elements - note Large Shield intentionally DOES apply to incoming spell attacks too
		assertEquals (1, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE, null,
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db));
		assertEquals (3, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE, null,
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, "MB01", players, fow, db));
		assertEquals (3, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE, null,
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, "MB02", players, fow, db));

		when (unitUtils.getBasicSkillValue (unit.getUnitHasSkill (), "US002")).thenReturn (0);

		assertEquals (1, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE, null,
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db));
		assertEquals (3, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE, null,
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, "MB01", players, fow, db));
		assertEquals (6, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE, null,
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, "MB02", players, fow, db));
	}
	
	/**
	 * Tests the getModifiedSkillValue method on a skill that gets negated by our own skill
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetModifiedSkillValue_NegatedByOwnSkill () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final NegatedBySkill elementalArmourNegatesResistElements = new NegatedBySkill ();
		elementalArmourNegatesResistElements.setNegatedBySkillID ("US002");
		elementalArmourNegatesResistElements.setNegatedByUnitID (NegatedByUnitID.OUR_UNIT);
		
		final UnitSkill resistElements = new UnitSkill ();
		resistElements.getNegatedBySkill ().add (elementalArmourNegatesResistElements);
		when (db.findUnitSkill ("US001", "getModifiedSkillValue")).thenReturn (resistElements);
		
		final UnitSkill elementalArmour = new UnitSkill ();
		when (db.findUnitSkill ("US002", "getModifiedSkillValue")).thenReturn (elementalArmour);

		// Lists
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Sample unit
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final AvailableUnit unit = new AvailableUnit ();
		when (unitUtils.getModifiedUnitMagicRealmLifeformTypeID (unit, unit.getUnitHasSkill (), fow.getMaintainedSpell (), db)).thenReturn ("A");

		// Set up object to test
		final UnitSkillUtilsImpl utils = new UnitSkillUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		
		// Now have the lower grade skill but not the higher one
		when (unitUtils.getBasicSkillValue (unit.getUnitHasSkill (), "US001")).thenReturn (0);
		when (unitUtils.getBasicSkillValue (unit.getUnitHasSkill (), "US002")).thenReturn (-1);
		
		assertEquals (0, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US001", null, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db));
		assertEquals (-1, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US002", null, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db));
		
		// Grant the higher grade skill, now the lower grade skill is negated
		when (unitUtils.getBasicSkillValue (unit.getUnitHasSkill (), "US002")).thenReturn (0);

		assertEquals (-1, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US001", null, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db));
		assertEquals (0, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US002", null, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db));
	}

	/**
	 * Tests the getModifiedSkillValue method on a skill that gets negated by an enemy unit's skill
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetModifiedSkillValue_NegatedByEnemySkill () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final NegatedBySkill illusionsImmunityNegatesInvisibility = new NegatedBySkill ();
		illusionsImmunityNegatesInvisibility.setNegatedBySkillID ("US002");
		illusionsImmunityNegatesInvisibility.setNegatedByUnitID (NegatedByUnitID.ENEMY_UNIT);
		
		final UnitSkill invisibility = new UnitSkill ();
		invisibility.getNegatedBySkill ().add (illusionsImmunityNegatesInvisibility);
		when (db.findUnitSkill ("US001", "getModifiedSkillValue")).thenReturn (invisibility);
		
		final UnitSkill illusionsImmunity = new UnitSkill ();
		when (db.findUnitSkill ("US002", "getModifiedSkillValue")).thenReturn (illusionsImmunity);
		
		// Lists
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Sample unit
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final AvailableUnit unit = new AvailableUnit ();
		when (unitUtils.getModifiedUnitMagicRealmLifeformTypeID (unit, unit.getUnitHasSkill (), fow.getMaintainedSpell (), db)).thenReturn ("A");

		// Set up object to test
		final UnitSkillUtilsImpl utils = new UnitSkillUtilsImpl ();
		utils.setUnitUtils (unitUtils);

		// With no enemies listed, we still indicate that we have the skill
		when (unitUtils.getBasicSkillValue (unit.getUnitHasSkill (), "US001")).thenReturn (0);
		assertEquals (0, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US001", null, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db));
		
		// Enemy who doesn't have the skill
		final MemoryUnit enemy1 = new MemoryUnit ();
		final UnitHasSkillMergedList enemy1Skills = new UnitHasSkillMergedList ();
		enemy1Skills.add (null);		// Just to make the lists unique
		
		when (unitUtils.mergeSpellEffectsIntoSkillList (fow.getMaintainedSpell (), enemy1, db)).thenReturn (enemy1Skills);
		when (unitUtils.getBasicSkillValue (enemy1Skills, "US002")).thenReturn (-1);
		
		final List<MemoryUnit> enemies = new ArrayList<MemoryUnit> ();
		enemies.add (enemy1);

		assertEquals (0, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US001", enemies, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db));
		
		// Enemy who does have the skill
		final MemoryUnit enemy2 = new MemoryUnit ();
		final UnitHasSkillMergedList enemy2Skills = new UnitHasSkillMergedList ();
		enemy2Skills.add (null);		// Just to make the lists unique
		enemy2Skills.add (null);
		when (unitUtils.mergeSpellEffectsIntoSkillList (fow.getMaintainedSpell (), enemy2, db)).thenReturn (enemy2Skills);
		when (unitUtils.getBasicSkillValue (enemy2Skills, "US002")).thenReturn (0);
		
		enemies.add (enemy2);
		assertEquals (-1, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US001", enemies, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db));
		
		// Remove 2nd enemy again, just to prove the mocks are working correctly and treating the skill lists uniquely
		enemies.remove (enemy2);
		assertEquals (0, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US001", enemies, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db));
	}
	
	/**
	 * Tests the unitIgnoresCombatTerrain method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUnitIgnoresCombatTerrain () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitSkill skillA = new UnitSkill ();
		when (db.findUnitSkill ("A", "unitIgnoresCombatTerrain")).thenReturn (skillA);

		final UnitSkill skillB = new UnitSkill ();
		skillB.setIgnoreCombatTerrain (false);
		when (db.findUnitSkill ("B", "unitIgnoresCombatTerrain")).thenReturn (skillB);

		final UnitSkill skillC = new UnitSkill ();
		skillC.setIgnoreCombatTerrain (true);
		when (db.findUnitSkill ("C", "unitIgnoresCombatTerrain")).thenReturn (skillC);
		
		// Unit to test with
		final MemoryUnit unit = new MemoryUnit ();
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		// Skill list
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final UnitHasSkillMergedList skills = new UnitHasSkillMergedList ();
		
		final UnitSkillAndValue hasSkillA = new UnitSkillAndValue ();
		hasSkillA.setUnitSkillID ("A");
		skills.add (hasSkillA);
		
		final UnitSkillAndValue hasSkillB = new UnitSkillAndValue ();
		hasSkillB.setUnitSkillID ("B");
		skills.add (hasSkillB);
		
		when (unitUtils.mergeSpellEffectsIntoSkillList (spells, unit, db)).thenReturn (skills);

		// Set up object to test
		final UnitSkillUtilsImpl utils = new UnitSkillUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		
		// Try with no matching skills
		assertFalse (utils.unitIgnoresCombatTerrain (unit, spells, db));
		
		// Now add one
		final UnitSkillAndValue hasSkillC = new UnitSkillAndValue ();
		hasSkillC.setUnitSkillID ("C");
		skills.add (hasSkillC);
		
		assertTrue (utils.unitIgnoresCombatTerrain (unit, spells, db));
	}
}