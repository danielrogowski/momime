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
import momime.common.messages.MemoryUnit;

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
	 * Tests the calculateHitPointsRemaining method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateHitPointsRemaining () throws Exception
	{
		// Mock database
		final Unit unitDef = new Unit ();

		// Damage taken
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		// Set up object to test
		final Map<UnitSkillComponent, Integer> hp = new HashMap<UnitSkillComponent, Integer> ();

		final Map<String, Map<UnitSkillComponent, Integer>> modifiedSkillValues = new HashMap<String, Map<UnitSkillComponent, Integer>> ();
		modifiedSkillValues.put (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS, hp);

		final MemoryUnit unit = new MemoryUnit ();
		final ExpandedUnitDetailsImpl xu = new ExpandedUnitDetailsImpl (unit, unitDef, null, null, null, null, null, null, null, null, modifiedSkillValues, null, null, unitUtils);
		
		// Unit with 1 HP per figure at full health of 6 figures
		unitDef.setFigureCount (6);
		hp.put (UnitSkillComponent.BASIC, 1);

		assertEquals (6, xu.calculateHitPointsRemaining ());
	
		// Take 1 hit
		when (unitUtils.getTotalDamageTaken (unit.getUnitDamage ())).thenReturn (1);
		assertEquals (5, xu.calculateHitPointsRemaining ());

		// Now it has 4 HP per figure
		hp.put (UnitSkillComponent.EXPERIENCE, 3);
		assertEquals (23, xu.calculateHitPointsRemaining ());
		
		// Take 2 more hits
		when (unitUtils.getTotalDamageTaken (unit.getUnitDamage ())).thenReturn (3);
		assertEquals (21, xu.calculateHitPointsRemaining ());
	}
	
	/**
	 * Tests the calculateAliveFigureCount method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateAliveFigureCount () throws Exception
	{
		// Mock database
		final Unit unitDef = new Unit ();

		// Damage taken
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		// Set up object to test
		final Map<UnitSkillComponent, Integer> hp = new HashMap<UnitSkillComponent, Integer> ();

		final Map<String, Map<UnitSkillComponent, Integer>> modifiedSkillValues = new HashMap<String, Map<UnitSkillComponent, Integer>> ();
		modifiedSkillValues.put (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS, hp);

		final MemoryUnit unit = new MemoryUnit ();
		final ExpandedUnitDetailsImpl xu = new ExpandedUnitDetailsImpl (unit, unitDef, null, null, null, null, null, null, null, null, modifiedSkillValues, null, null, unitUtils);

		// Unit with 1 HP per figure at full health of 6 figures
		unitDef.setFigureCount (6);
		hp.put (UnitSkillComponent.BASIC, 1);
		
		assertEquals (6, xu.calculateAliveFigureCount ());
		
		// Now it takes 2 hits
		when (unitUtils.getTotalDamageTaken (unit.getUnitDamage ())).thenReturn (2);
		assertEquals (4, xu.calculateAliveFigureCount ());

		// Now its dead
		when (unitUtils.getTotalDamageTaken (unit.getUnitDamage ())).thenReturn (6);
		assertEquals (0, xu.calculateAliveFigureCount ());

		// Now its more than dead
		when (unitUtils.getTotalDamageTaken (unit.getUnitDamage ())).thenReturn (9);
		assertEquals (0, xu.calculateAliveFigureCount ());
		
		// Now it has 4 HP per figure, so 6x4=24 total damage
		hp.put (UnitSkillComponent.EXPERIENCE, 3);
		assertEquals (4, xu.calculateAliveFigureCount ());
		
		// With 11 dmg taken, there's still only 2 figures dead, since it rounds down
		when (unitUtils.getTotalDamageTaken (unit.getUnitDamage ())).thenReturn (11);
		assertEquals (4, xu.calculateAliveFigureCount ());
		
		// With 12 dmg taken, there's 3 figures dead
		when (unitUtils.getTotalDamageTaken (unit.getUnitDamage ())).thenReturn (12);
		assertEquals (3, xu.calculateAliveFigureCount ());

		// Nearly dead
		when (unitUtils.getTotalDamageTaken (unit.getUnitDamage ())).thenReturn (23);
		assertEquals (1, xu.calculateAliveFigureCount ());
	}
	
	/**
	 * Tests the calculateHitPointsRemainingOfFirstFigure method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateHitPointsRemainingOfFirstFigure () throws Exception
	{
		// Mock database
		final Unit unitDef = new Unit ();

		// Damage taken
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		// Set up object to test
		final Map<UnitSkillComponent, Integer> hp = new HashMap<UnitSkillComponent, Integer> ();

		final Map<String, Map<UnitSkillComponent, Integer>> modifiedSkillValues = new HashMap<String, Map<UnitSkillComponent, Integer>> ();
		modifiedSkillValues.put (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS, hp);

		final MemoryUnit unit = new MemoryUnit ();
		final ExpandedUnitDetailsImpl xu = new ExpandedUnitDetailsImpl (unit, unitDef, null, null, null, null, null, null, null, null, modifiedSkillValues, null, null, unitUtils);
		
		// Unit with 1 HP per figure at full health of 6 figures (actually nbr of figures is irrelevant)
		unitDef.setFigureCount (6);
		hp.put (UnitSkillComponent.BASIC, 1);

		assertEquals (1, xu.calculateHitPointsRemainingOfFirstFigure ());
	
		// Taking a hit makes no difference, now we're just on the same figure, with same HP
		when (unitUtils.getTotalDamageTaken (unit.getUnitDamage ())).thenReturn (1);
		assertEquals (1, xu.calculateHitPointsRemainingOfFirstFigure ());

		// Now it has 4 HP per figure
		hp.put (UnitSkillComponent.EXPERIENCE, 3);
		assertEquals (3, xu.calculateHitPointsRemainingOfFirstFigure ());
		
		// Take 2 more hits
		when (unitUtils.getTotalDamageTaken (unit.getUnitDamage ())).thenReturn (3);
		assertEquals (1, xu.calculateHitPointsRemainingOfFirstFigure ());
		
		// 1 more hit and first figure is dead, so second on full HP
		when (unitUtils.getTotalDamageTaken (unit.getUnitDamage ())).thenReturn (4);
		assertEquals (4, xu.calculateHitPointsRemainingOfFirstFigure ());

		// 2 and a quarter figures dead
		when (unitUtils.getTotalDamageTaken (unit.getUnitDamage ())).thenReturn (9);
		assertEquals (3, xu.calculateHitPointsRemainingOfFirstFigure ());

		// 2 and three-quarter figures dead
		when (unitUtils.getTotalDamageTaken (unit.getUnitDamage ())).thenReturn (11);
		assertEquals (1, xu.calculateHitPointsRemainingOfFirstFigure ());
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

		// Test a low hero, lv 3 caster skill * lv 2 (+1=3) exp * 2� = 22�
		expLevel.setLevelNumber (2);
		modifiedSkillValues.remove (CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_UNIT);
		
		final Map<UnitSkillComponent, Integer> casterHeroSkill = new HashMap<UnitSkillComponent, Integer> ();
		casterHeroSkill.put (UnitSkillComponent.BASIC, 3);
		modifiedSkillValues.put (CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_HERO, casterHeroSkill);
		
		assertEquals (22, xu.calculateManaTotal ());
		
		// Test a higher hero, lv 5 caster skill * lv 4 (+1=5) exp * 2� = 62�, +40 from unit caster skill = 102�
		expLevel.setLevelNumber (4);
		casterHeroSkill.put (UnitSkillComponent.BASIC, 5);
		modifiedSkillValues.put (CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_UNIT, casterUnitSkill);
		
		assertEquals (102, xu.calculateManaTotal ());
	}
}