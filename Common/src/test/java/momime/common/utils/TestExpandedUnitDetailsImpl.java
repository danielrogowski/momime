package momime.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.database.Unit;
import momime.common.database.UnitSkill;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillPositiveNegative;
import momime.common.messages.AvailableUnit;

/**
 * Tests the ExpandedUnitDetailsImpl class
 */
public final class TestExpandedUnitDetailsImpl
{
	/**
	 * Tests the getModifiedSkillValue method in the normal situation where we have some components to total up
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetModifiedSkillValue_Normal () throws Exception
	{
		// Set up skill components with various components and positive/negative values 
		final Map<UnitSkillComponent, Integer> components = new HashMap<UnitSkillComponent, Integer> ();
		components.put (UnitSkillComponent.BASIC, 5);
		components.put (UnitSkillComponent.EXPERIENCE, 3);
		components.put (UnitSkillComponent.SPELL_EFFECTS, 2);
		components.put (UnitSkillComponent.COMBAT_AREA_EFFECTS, -1);

		final Map<String, Map<UnitSkillComponent, Integer>> modifiedSkillValues = new HashMap<String, Map<UnitSkillComponent, Integer>> ();
		modifiedSkillValues.put ("US001", components);

		// Set up test unit
		final AvailableUnit unit = new AvailableUnit ();
		
		final ExpandedUnitDetailsImpl xu = new ExpandedUnitDetailsImpl (unit, null, null, null, null, null, null, null, null, null, modifiedSkillValues, null, null, null);
		
		// Run method
		assertEquals (9, xu.getModifiedSkillValue ("US001").intValue ());
	}

	/**
	 * Tests the getModifiedSkillValue method where one of the breakdown components has no value
	 * @throws Exception If there is a problem
	 */
	@Test(expected=MomException.class)
	public final void testGetModifiedSkillValue_NullComponent () throws Exception
	{
		// Include a component with a null value 
		final Map<UnitSkillComponent, Integer> components = new HashMap<UnitSkillComponent, Integer> ();
		components.put (UnitSkillComponent.BASIC, 5);
		components.put (UnitSkillComponent.SPELL_EFFECTS, null);

		final Map<String, Map<UnitSkillComponent, Integer>> modifiedSkillValues = new HashMap<String, Map<UnitSkillComponent, Integer>> ();
		modifiedSkillValues.put ("US001", components);

		// Set up test unit
		final AvailableUnit unit = new AvailableUnit ();
		
		final ExpandedUnitDetailsImpl xu = new ExpandedUnitDetailsImpl (unit, null, null, null, null, null, null, null, null, null, modifiedSkillValues, null, null, null);
		
		// Run method
		xu.getModifiedSkillValue ("US001");
	}

	/**
	 * Tests the getModifiedSkillValue method asking for a skill that we don't even have
	 * @throws Exception If there is a problem
	 */
	@Test(expected=MomException.class)
	public final void testGetModifiedSkillValue_DontHaveSkill () throws Exception
	{
		final Map<String, Map<UnitSkillComponent, Integer>> modifiedSkillValues = new HashMap<String, Map<UnitSkillComponent, Integer>> ();

		// Set up test unit
		final AvailableUnit unit = new AvailableUnit ();

		final ExpandedUnitDetailsImpl xu = new ExpandedUnitDetailsImpl (unit, null, null, null, null, null, null, null, null, null, modifiedSkillValues, null, null, null);
		
		// Run method
		xu.getModifiedSkillValue ("US001");
	}
	
	/**
	 * Tests the getModifiedSkillValue method on a valueless skill, e.g. First Strike
	 * Also checks the special case of +to hit/block when we have no modifiers
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetModifiedSkillValue_ValuelessSkill () throws Exception
	{
		final Map<String, Map<UnitSkillComponent, Integer>> modifiedSkillValues = new HashMap<String, Map<UnitSkillComponent, Integer>> ();
		modifiedSkillValues.put ("US001", null);
		modifiedSkillValues.put (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT, null);

		// Set up test unit
		final AvailableUnit unit = new AvailableUnit ();
		
		final ExpandedUnitDetailsImpl xu = new ExpandedUnitDetailsImpl (unit, null, null, null, null, null, null, null, null, null, modifiedSkillValues, null, null, null);
		
		// Run method
		assertNull (xu.getModifiedSkillValue ("US001"));
		assertEquals (0, xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT).intValue ());
	}
	
	/**
	 * Tests the filterModifiedSkillValue method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFilterModifiedSkillValue_Normal () throws Exception
	{
		// Set up skill components with various components and positive/negative values 
		final Map<UnitSkillComponent, Integer> components = new HashMap<UnitSkillComponent, Integer> ();
		components.put (UnitSkillComponent.BASIC, 5);
		components.put (UnitSkillComponent.EXPERIENCE, 3);
		components.put (UnitSkillComponent.SPELL_EFFECTS, 2);
		components.put (UnitSkillComponent.COMBAT_AREA_EFFECTS, -1);

		final Map<String, Map<UnitSkillComponent, Integer>> modifiedSkillValues = new HashMap<String, Map<UnitSkillComponent, Integer>> ();
		modifiedSkillValues.put ("US001", components);

		// Set up test unit
		final AvailableUnit unit = new AvailableUnit ();
		
		final ExpandedUnitDetailsImpl xu = new ExpandedUnitDetailsImpl (unit, null, null, null, null, null, null, null, null, null, modifiedSkillValues, null, null, null);
		
		// Run method
		assertEquals (9, xu.filterModifiedSkillValue ("US001", UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH).intValue ());
		assertEquals (10, xu.filterModifiedSkillValue ("US001", UnitSkillComponent.ALL, UnitSkillPositiveNegative.POSITIVE).intValue ());
		assertEquals (-1, xu.filterModifiedSkillValue ("US001", UnitSkillComponent.ALL, UnitSkillPositiveNegative.NEGATIVE).intValue ());

		assertEquals (3, xu.filterModifiedSkillValue ("US001", UnitSkillComponent.EXPERIENCE, UnitSkillPositiveNegative.BOTH).intValue ());
		assertEquals (3, xu.filterModifiedSkillValue ("US001", UnitSkillComponent.EXPERIENCE, UnitSkillPositiveNegative.POSITIVE).intValue ());
		assertEquals (0, xu.filterModifiedSkillValue ("US001", UnitSkillComponent.EXPERIENCE, UnitSkillPositiveNegative.NEGATIVE).intValue ());

		assertEquals (-1, xu.filterModifiedSkillValue ("US001", UnitSkillComponent.COMBAT_AREA_EFFECTS, UnitSkillPositiveNegative.BOTH).intValue ());
		assertEquals (0, xu.filterModifiedSkillValue ("US001", UnitSkillComponent.COMBAT_AREA_EFFECTS, UnitSkillPositiveNegative.POSITIVE).intValue ());
		assertEquals (-1, xu.filterModifiedSkillValue ("US001", UnitSkillComponent.COMBAT_AREA_EFFECTS, UnitSkillPositiveNegative.NEGATIVE).intValue ());

		assertEquals (0, xu.filterModifiedSkillValue ("US001", UnitSkillComponent.HERO_ITEMS, UnitSkillPositiveNegative.BOTH).intValue ());
		assertEquals (0, xu.filterModifiedSkillValue ("US001", UnitSkillComponent.HERO_ITEMS, UnitSkillPositiveNegative.POSITIVE).intValue ());
		assertEquals (0, xu.filterModifiedSkillValue ("US001", UnitSkillComponent.HERO_ITEMS, UnitSkillPositiveNegative.NEGATIVE).intValue ());
	}
	
	/**
	 * Tests the getFullFigureCount method
	 */
	@Test
	public final void testGetFullFigureCount ()
	{
		// Mock database
		final Unit unitDef = new Unit ();
		
		// Set up object to test
		final ExpandedUnitDetailsImpl unit = new ExpandedUnitDetailsImpl (null, unitDef, null, null, null, null, null, null, null, null, null, null, null, null);

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
		final ExpandedUnitDetailsImpl xu = new ExpandedUnitDetailsImpl (unit, null, null, null, null, null, null, null, null, basicSkillValues, null, null, null, null);
		
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

		final ExpandedUnitDetailsImpl unit = new ExpandedUnitDetailsImpl (null, null, null, null, null, null, null, null, null, null, modifiedSkillValues, null, null, null);
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
		
		final ExpandedUnitDetailsImpl xu = new ExpandedUnitDetailsImpl (unit, null, null, null, null, null, null, null, expLevel, null, modifiedSkillValues, null, null, null);
		
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