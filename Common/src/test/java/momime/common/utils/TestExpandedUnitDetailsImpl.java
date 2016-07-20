package momime.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.database.Unit;
import momime.common.database.UnitSkill;
import momime.common.database.UnitSkillComponent;
import momime.common.messages.AvailableUnit;

/**
 * Tests the ExpandedUnitDetailsImpl class
 */
public final class TestExpandedUnitDetailsImpl
{
	/**
	 * Tests the getFullFigureCount method
	 */
	@Test
	public final void testGetFullFigureCount ()
	{
		// Mock database
		final Unit unitDef = new Unit ();
		
		// Set up object to test
		final ExpandedUnitDetailsImpl unit = new ExpandedUnitDetailsImpl (null, unitDef, null, null, null, null, null, null, null, null, null, null, null);

		// Run method
		unitDef.setFigureCount (1);
		assertEquals (1, unit.getFullFigureCount ());

		unitDef.setFigureCount (4);
		assertEquals (4, unit.getFullFigureCount ());

		// Hydra
		unitDef.setFigureCount (9);
		assertEquals (1, unit.getFullFigureCount ());
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
		
		// Skill list
		final Map<String, Integer> basicSkillValues = new HashMap<String, Integer> ();
		basicSkillValues.put ("A", null);
		basicSkillValues.put ("B", null);

		// Underlying unit
		final AvailableUnit unit = new AvailableUnit ();
		
		// Set up object to test
		final ExpandedUnitDetailsImpl xu = new ExpandedUnitDetailsImpl (unit, null, null, null, null, null, null, null, null, basicSkillValues, null, null, null);
		
		// Try with no matching skills
		assertFalse (xu.unitIgnoresCombatTerrain (db));
		
		// Now add one
		basicSkillValues.put ("C", null);
		assertTrue (xu.unitIgnoresCombatTerrain (db));
	}

	/**
	 * Tests the calculateFullRangedAttackAmmo method
	 * Its a bit of a dumb test, since its only returning the value straight out of getModifiedSkillValue, but including it to be complete and as a pretest for giveUnitFullRangedAmmoAndMana
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateFullRangedAttackAmmo () throws Exception
	{
		// Test unit without the ranged attack skill
		final Map<String, Map<UnitSkillComponent, Integer>> modifiedSkillValues = new HashMap<String, Map<UnitSkillComponent, Integer>> ();

		final ExpandedUnitDetailsImpl unit = new ExpandedUnitDetailsImpl (null, null, null, null, null, null, null, null, null, null, modifiedSkillValues, null, null);
		assertEquals (0, unit.calculateFullRangedAttackAmmo ());

		// Test unit with the ranged attack skill
		final Map<UnitSkillComponent, Integer> ammo = new HashMap<UnitSkillComponent, Integer> ();
		ammo.put (UnitSkillComponent.BASIC, 5);
		
		modifiedSkillValues.put (CommonDatabaseConstants.UNIT_SKILL_ID_RANGED_ATTACK_AMMO, ammo);
		assertEquals (5, unit.calculateFullRangedAttackAmmo ());
	}
	
	/**
	 * Tests the calculateManaTotal method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateManaTotal () throws Exception
	{
		// Underlying unit
		final AvailableUnit unit = new AvailableUnit ();
		
		// Set up object to test
		final Map<String, Map<UnitSkillComponent, Integer>> modifiedSkillValues = new HashMap<String, Map<UnitSkillComponent, Integer>> ();
		final ExperienceLevel expLevel = new ExperienceLevel ();
		
		final ExpandedUnitDetailsImpl xu = new ExpandedUnitDetailsImpl (unit, null, null, null, null, null, null, null, expLevel, null, modifiedSkillValues, null, null);
		
		// Test a non-casting unit
		assertEquals (0, xu.calculateManaTotal ());

		// Test an archangel
		final Map<UnitSkillComponent, Integer> casterUnitSkill = new HashMap<UnitSkillComponent, Integer> ();
		casterUnitSkill.put (UnitSkillComponent.BASIC, 40);
		modifiedSkillValues.put (CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_UNIT, casterUnitSkill);
		
		assertEquals (40, xu.calculateManaTotal ());

		// Test a low hero, lv 3 caster skill * lv 2 (+1=3) exp * 2½ = 22½
		expLevel.setLevelNumber (2);
		modifiedSkillValues.remove (CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_UNIT);
		
		final Map<UnitSkillComponent, Integer> casterHeroSkill = new HashMap<UnitSkillComponent, Integer> ();
		casterHeroSkill.put (UnitSkillComponent.BASIC, 3);
		modifiedSkillValues.put (CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_HERO, casterHeroSkill);
		
		assertEquals (22, xu.calculateManaTotal ());
		
		// Test a higher hero, lv 5 caster skill * lv 4 (+1=5) exp * 2½ = 62½, +40 from unit caster skill = 102½
		expLevel.setLevelNumber (4);
		casterHeroSkill.put (UnitSkillComponent.BASIC, 5);
		modifiedSkillValues.put (CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_UNIT, casterUnitSkill);
		
		assertEquals (102, xu.calculateManaTotal ());
	}
}