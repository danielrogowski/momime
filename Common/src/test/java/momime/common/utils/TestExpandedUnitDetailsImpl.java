package momime.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.database.UnitEx;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillPositiveNegative;
import momime.common.messages.AvailableUnit;
import momime.common.messages.MemoryUnit;

/**
 * Tests the ExpandedUnitDetailsImpl class
 */
@ExtendWith(MockitoExtension.class)
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
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (UnitSkillComponent.BASIC);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 5);
		breakdown.getComponents ().put (UnitSkillComponent.EXPERIENCE, 3);
		breakdown.getComponents ().put (UnitSkillComponent.SPELL_EFFECTS, 2);
		breakdown.getComponents ().put (UnitSkillComponent.COMBAT_AREA_EFFECTS, -1);

		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		modifiedSkillValues.put ("US001", breakdown);

		// Set up test unit
		final AvailableUnit unit = new AvailableUnit ();
		
		final ExpandedUnitDetailsImpl xu = new ExpandedUnitDetailsImpl (unit, null, null, null, null, null, null, null, null, 2, null, modifiedSkillValues, null, null, null);
		
		// Run method
		assertEquals (9, xu.getModifiedSkillValue ("US001").intValue ());
	}

	/**
	 * Tests the getModifiedSkillValue method where one of the breakdown components has no value
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetModifiedSkillValue_NullComponent () throws Exception
	{
		// Include a component with a null value 
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (UnitSkillComponent.BASIC);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 5);
		breakdown.getComponents ().put (UnitSkillComponent.SPELL_EFFECTS, null);

		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		modifiedSkillValues.put ("US001", breakdown);

		// Set up test unit
		final AvailableUnit unit = new AvailableUnit ();
		
		final ExpandedUnitDetailsImpl xu = new ExpandedUnitDetailsImpl (unit, null, null, null, null, null, null, null, null, 2, null, modifiedSkillValues, null, null, null);
		
		// Run method
		assertThrows (MomException.class, () ->
		{
			xu.getModifiedSkillValue ("US001");
		});
	}

	/**
	 * Tests the getModifiedSkillValue method asking for a skill that we don't even have
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetModifiedSkillValue_DontHaveSkill () throws Exception
	{
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();

		// Set up test unit
		final AvailableUnit unit = new AvailableUnit ();

		final ExpandedUnitDetailsImpl xu = new ExpandedUnitDetailsImpl (unit, null, null, null, null, null, null, null, null, 2, null, modifiedSkillValues, null, null, null);
		
		// Run method
		assertThrows (MomException.class, () ->
		{
			xu.getModifiedSkillValue ("US001");
		});
	}
	
	/**
	 * Tests the getModifiedSkillValue method on a valueless skill, e.g. First Strike
	 * Also checks the special case of experience when it has no value yet
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetModifiedSkillValue_ValuelessSkill () throws Exception
	{
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		modifiedSkillValues.put ("US001", new UnitSkillValueBreakdown (UnitSkillComponent.BASIC));
		modifiedSkillValues.put (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE, new UnitSkillValueBreakdown (UnitSkillComponent.BASIC));

		// Set up test unit
		final AvailableUnit unit = new AvailableUnit ();
		
		final ExpandedUnitDetailsImpl xu = new ExpandedUnitDetailsImpl (unit, null, null, null, null, null, null, null, null, 2, null, modifiedSkillValues, null, null, null);
		
		// Run method
		assertNull (xu.getModifiedSkillValue ("US001"));
		assertEquals (0, xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE).intValue ());
	}
	
	/**
	 * Tests the filterModifiedSkillValue method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFilterModifiedSkillValue_Normal () throws Exception
	{
		// Set up skill components with various components and positive/negative values 
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (UnitSkillComponent.BASIC);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 5);
		breakdown.getComponents ().put (UnitSkillComponent.EXPERIENCE, 3);
		breakdown.getComponents ().put (UnitSkillComponent.SPELL_EFFECTS, 2);
		breakdown.getComponents ().put (UnitSkillComponent.COMBAT_AREA_EFFECTS, -1);

		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		modifiedSkillValues.put ("US001", breakdown);

		// Set up test unit
		final AvailableUnit unit = new AvailableUnit ();
		
		final ExpandedUnitDetailsImpl xu = new ExpandedUnitDetailsImpl (unit, null, null, null, null, null, null, null, null, 2, null, modifiedSkillValues, null, null, null);
		
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
	 * Tests the calculateHitPointsRemaining method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateHitPointsRemaining () throws Exception
	{
		// Mock database
		final UnitEx unitDef = new UnitEx ();

		// Damage taken
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		// Set up object to test
		final UnitSkillValueBreakdown hp = new UnitSkillValueBreakdown (UnitSkillComponent.BASIC);

		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		modifiedSkillValues.put (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS, hp);

		final MemoryUnit unit = new MemoryUnit ();
		final ExpandedUnitDetailsImpl xu = new ExpandedUnitDetailsImpl (unit, unitDef, null, null, null, null, null, null, null, 2, null, modifiedSkillValues, null, null, unitUtils);
		
		// Unit with 1 HP per figure at full health of 6 figures
		unitDef.setFigureCount (6);
		hp.getComponents ().put (UnitSkillComponent.BASIC, 1);

		assertEquals (6, xu.calculateHitPointsRemaining ());
	
		// Take 1 hit
		when (unitUtils.getTotalDamageTaken (unit.getUnitDamage ())).thenReturn (1);
		assertEquals (5, xu.calculateHitPointsRemaining ());

		// Now it has 4 HP per figure
		hp.getComponents ().put (UnitSkillComponent.EXPERIENCE, 3);
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
		final UnitEx unitDef = new UnitEx ();

		// Damage taken
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		// Set up object to test
		final UnitSkillValueBreakdown hp = new UnitSkillValueBreakdown (UnitSkillComponent.BASIC);

		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		modifiedSkillValues.put (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS, hp);

		final MemoryUnit unit = new MemoryUnit ();
		final ExpandedUnitDetailsImpl xu = new ExpandedUnitDetailsImpl (unit, unitDef, null, null, null, null, null, null, null, 2, null, modifiedSkillValues, null, null, unitUtils);

		// Unit with 1 HP per figure at full health of 6 figures
		unitDef.setFigureCount (6);
		hp.getComponents ().put (UnitSkillComponent.BASIC, 1);
		
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
		hp.getComponents ().put (UnitSkillComponent.EXPERIENCE, 3);
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
		final UnitEx unitDef = new UnitEx ();

		// Damage taken
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		// Set up object to test
		final UnitSkillValueBreakdown hp = new UnitSkillValueBreakdown (UnitSkillComponent.BASIC);

		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		modifiedSkillValues.put (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS, hp);

		final MemoryUnit unit = new MemoryUnit ();
		final ExpandedUnitDetailsImpl xu = new ExpandedUnitDetailsImpl (unit, unitDef, null, null, null, null, null, null, null, 2, null, modifiedSkillValues, null, null, unitUtils);
		
		// Unit with 1 HP per figure at full health of 6 figures (actually nbr of figures is irrelevant)
		unitDef.setFigureCount (6);
		hp.getComponents ().put (UnitSkillComponent.BASIC, 1);

		assertEquals (1, xu.calculateHitPointsRemainingOfFirstFigure ());
	
		// Taking a hit makes no difference, now we're just on the same figure, with same HP
		when (unitUtils.getTotalDamageTaken (unit.getUnitDamage ())).thenReturn (1);
		assertEquals (1, xu.calculateHitPointsRemainingOfFirstFigure ());

		// Now it has 4 HP per figure
		hp.getComponents ().put (UnitSkillComponent.EXPERIENCE, 3);
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
	 * Tests the calculateFullRangedAttackAmmo method
	 * Its a bit of a dumb test, since its only returning the value straight out of getModifiedSkillValue, but including it to be complete and as a pretest for giveUnitFullRangedAmmoAndMana
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateFullRangedAttackAmmo () throws Exception
	{
		// Test unit without the ranged attack skill
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();

		final ExpandedUnitDetailsImpl unit = new ExpandedUnitDetailsImpl (null, null, null, null, null, null, null, null, null, 2, null, modifiedSkillValues, null, null, null);
		assertEquals (0, unit.calculateFullRangedAttackAmmo ());

		// Test unit with the ranged attack skill
		final UnitSkillValueBreakdown ammo = new UnitSkillValueBreakdown (UnitSkillComponent.BASIC);
		ammo.getComponents ().put (UnitSkillComponent.BASIC, 5);
		
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
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		final ExperienceLevel expLevel = new ExperienceLevel ();
		
		final ExpandedUnitDetailsImpl xu = new ExpandedUnitDetailsImpl (unit, null, null, null, null, null, null, null, expLevel, 2, null, modifiedSkillValues, null, null, null);
		
		// Test a non-casting unit
		assertEquals (0, xu.calculateManaTotal ());

		// Test an archangel
		final UnitSkillValueBreakdown casterUnitSkill = new UnitSkillValueBreakdown (UnitSkillComponent.BASIC);
		casterUnitSkill.getComponents ().put (UnitSkillComponent.BASIC, 40);
		modifiedSkillValues.put (CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_UNIT, casterUnitSkill);
		
		assertEquals (40, xu.calculateManaTotal ());

		// Test a low hero, lv 3 caster skill * lv 2 (+1=3) exp * 2½ = 22½
		expLevel.setLevelNumber (2);
		modifiedSkillValues.remove (CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_UNIT);
		
		final UnitSkillValueBreakdown casterHeroSkill = new UnitSkillValueBreakdown (UnitSkillComponent.BASIC);
		casterHeroSkill.getComponents ().put (UnitSkillComponent.BASIC, 3);
		modifiedSkillValues.put (CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_HERO, casterHeroSkill);
		
		assertEquals (22, xu.calculateManaTotal ());
		
		// Test a higher hero, lv 5 caster skill * lv 4 (+1=5) exp * 2½ = 62½, +40 from unit caster skill = 102½
		expLevel.setLevelNumber (4);
		casterHeroSkill.getComponents ().put (UnitSkillComponent.BASIC, 5);
		modifiedSkillValues.put (CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_UNIT, casterUnitSkill);
		
		assertEquals (102, xu.calculateManaTotal ());
	}
}