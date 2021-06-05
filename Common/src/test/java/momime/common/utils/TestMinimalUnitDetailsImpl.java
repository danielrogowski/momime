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
import momime.common.database.UnitEx;
import momime.common.database.UnitSkillEx;
import momime.common.database.UnitType;

/**
 * Tests the MinimalUnitDetailsImpl class
 */
public final class TestMinimalUnitDetailsImpl
{
	/**
	 * Tests the isHero method on a hero
	 */
	@Test
	public final void testIsHero_Yes ()
	{
		// Mock database
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
		
		// Set up object to test
		final MinimalUnitDetailsImpl mu = new MinimalUnitDetailsImpl (null, unitDef, null, null, null, null, null);
		
		// Call method
		assertTrue (mu.isHero ());
	}

	/**
	 * Tests the isHero method on a normal unit
	 */
	@Test
	public final void testIsHero_No ()
	{
		// Mock database
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		
		// Set up object to test
		final MinimalUnitDetailsImpl mu = new MinimalUnitDetailsImpl (null, unitDef, null, null, null, null, null);
		
		// Call method
		assertFalse (mu.isHero ());
	}
	
	/**
	 * Tests the isSummoned method on a summoned unit
	 */
	@Test
	public final void testIsSummoned_Yes ()
	{
		// Mock database
		final UnitType unitTypeDef = new UnitType ();
		unitTypeDef.setUnitTypeID (CommonDatabaseConstants.UNIT_TYPE_ID_SUMMONED);
		
		// Set up object to test
		final MinimalUnitDetailsImpl mu = new MinimalUnitDetailsImpl (null, null, unitTypeDef, null, null, null, null);
		
		// Call method
		assertTrue (mu.isSummoned ());
	}
	
	/**
	 * Tests the isSummoned method on a normal unit
	 */
	@Test
	public final void testIsSummoned_No ()
	{
		// Mock database
		final UnitType unitTypeDef = new UnitType ();
		unitTypeDef.setUnitTypeID ("N");
		
		// Set up object to test
		final MinimalUnitDetailsImpl mu = new MinimalUnitDetailsImpl (null, null, unitTypeDef, null, null, null, null);
		
		// Call method
		assertFalse (mu.isSummoned ());
	}
	
	/**
	 * Tests the getFullFigureCount method on a normal unit
	 */
	@Test
	public final void testGetFullFigureCount_Normal ()
	{
		// Mock database
		final UnitEx unitDef = new UnitEx ();
		
		// Set up object to test
		final MinimalUnitDetailsImpl mu = new MinimalUnitDetailsImpl (null, unitDef, null, null, null, null, null);

		// Run method
		unitDef.setFigureCount (4);
		assertEquals (4, mu.getFullFigureCount ());
	}
	
	/**
	 * Tests the getFullFigureCount method on a Hydra
	 */
	@Test
	public final void testGetFullFigureCount_Hydra ()
	{
		// Mock database
		final UnitEx unitDef = new UnitEx ();
		
		// Set up object to test
		final MinimalUnitDetailsImpl mu = new MinimalUnitDetailsImpl (null, unitDef, null, null, null, null, null);

		// Run method
		unitDef.setFigureCount (9);
		assertEquals (1, mu.getFullFigureCount ());
	}
	
	/**
	 * Tests the unitIgnoresCombatTerrain method on a unit which doesn't have any skills which make it ignore combat terrain
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUnitIgnoresCombatTerrain_No () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitSkillEx skillA = new UnitSkillEx ();
		when (db.findUnitSkill ("A", "unitIgnoresCombatTerrain")).thenReturn (skillA);

		final UnitSkillEx skillB = new UnitSkillEx ();
		skillB.setIgnoreCombatTerrain (false);
		when (db.findUnitSkill ("B", "unitIgnoresCombatTerrain")).thenReturn (skillB);

		final UnitSkillEx skillC = new UnitSkillEx ();
		skillC.setIgnoreCombatTerrain (true);
		when (db.findUnitSkill ("C", "unitIgnoresCombatTerrain")).thenReturn (skillC);
		
		// Has skills, but none that ignore combat terrain
		final Map<String, Integer> basicSkillValues = new HashMap<String, Integer> ();
		basicSkillValues.put ("A", null);
		basicSkillValues.put ("B", null);

		// Set up object to test
		final MinimalUnitDetailsImpl mu = new MinimalUnitDetailsImpl (null, null, null, null, null, null, basicSkillValues);
		
		// Call method
		assertFalse (mu.unitIgnoresCombatTerrain (db));
	}

	/**
	 * Tests the unitIgnoresCombatTerrain method on a unit which has a skill which make it ignore combat terrain
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUnitIgnoresCombatTerrain_Yes () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitSkillEx skillA = new UnitSkillEx ();
		when (db.findUnitSkill ("A", "unitIgnoresCombatTerrain")).thenReturn (skillA);

		final UnitSkillEx skillB = new UnitSkillEx ();
		skillB.setIgnoreCombatTerrain (false);
		when (db.findUnitSkill ("B", "unitIgnoresCombatTerrain")).thenReturn (skillB);

		final UnitSkillEx skillC = new UnitSkillEx ();
		skillC.setIgnoreCombatTerrain (true);
		when (db.findUnitSkill ("C", "unitIgnoresCombatTerrain")).thenReturn (skillC);
		
		// Has skill that ignores combat terrain
		final Map<String, Integer> basicSkillValues = new HashMap<String, Integer> ();
		basicSkillValues.put ("A", null);
		basicSkillValues.put ("C", null);

		// Set up object to test
		final MinimalUnitDetailsImpl mu = new MinimalUnitDetailsImpl (null, null, null, null, null, null, basicSkillValues);
		
		// Call method
		assertTrue (mu.unitIgnoresCombatTerrain (db));
	}
	
	/**
	 * Tests the calculateFameLostForUnitDying method on an experienced hero
	 */
	@Test
	public final void testCalculateFameLostForUnitDying_Hero ()
	{
		// Mock database
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
		
		final ExperienceLevel expLevel = new ExperienceLevel ();
		expLevel.setLevelNumber (3);
		
		// Set up object to test
		final MinimalUnitDetailsImpl mu = new MinimalUnitDetailsImpl (null, unitDef, null, null, null, expLevel, null);
		
		// Call method
		assertEquals (2, mu.calculateFameLostForUnitDying ());
	}
	
	/**
	 * Tests the calculateFameLostForUnitDying method on a normal experienced unit
	 */
	@Test
	public final void testCalculateFameLostForUnitDying_NormalUnit ()
	{
		// Mock database
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		
		final ExperienceLevel expLevel = new ExperienceLevel ();
		expLevel.setLevelNumber (3);
		
		// Set up object to test
		final MinimalUnitDetailsImpl mu = new MinimalUnitDetailsImpl (null, unitDef, null, null, null, expLevel, null);
		
		// Call method
		assertEquals (0, mu.calculateFameLostForUnitDying ());
	}
}