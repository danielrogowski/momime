package momime.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.database.UnitEx;
import momime.common.database.UnitTypeEx;

/**
 * Tests the MinimalUnitDetailsImpl class
 */
@ExtendWith(MockitoExtension.class)
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
		final MinimalUnitDetailsImpl mu = new MinimalUnitDetailsImpl (null, unitDef, null, null, null, null, null, null);
		
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
		final MinimalUnitDetailsImpl mu = new MinimalUnitDetailsImpl (null, unitDef, null, null, null, null, null, null);
		
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
		final UnitTypeEx unitTypeDef = new UnitTypeEx ();
		unitTypeDef.setUnitTypeID (CommonDatabaseConstants.UNIT_TYPE_ID_SUMMONED);
		
		// Set up object to test
		final MinimalUnitDetailsImpl mu = new MinimalUnitDetailsImpl (null, null, unitTypeDef, null, null, null, null, null);
		
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
		final UnitTypeEx unitTypeDef = new UnitTypeEx ();
		unitTypeDef.setUnitTypeID ("N");
		
		// Set up object to test
		final MinimalUnitDetailsImpl mu = new MinimalUnitDetailsImpl (null, null, unitTypeDef, null, null, null, null, null);
		
		// Call method
		assertFalse (mu.isSummoned ());
	}
	
	/**
	 * Tests the getBasicOrHeroSkillValue method on a hero
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetBasicOrHeroSkillValue_Hero () throws Exception
	{
		// Mock database
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);

		final ExperienceLevel expLevel = new ExperienceLevel ();
		expLevel.setLevelNumber (3);
		
		// Unit skills
		final Map<String, Integer> basicSkillValues = new HashMap<String, Integer> ();
		basicSkillValues.put ("A", 2);
		
		// Set up object to test
		final MinimalUnitDetailsImpl mu = new MinimalUnitDetailsImpl (null, unitDef, null, null, null, null, expLevel, basicSkillValues);
		
		// Call method
		// Value is doubled so 6, which can check on e.g. Might page of wiki for level 3 hero with Super Might
		assertEquals (12, mu.getBasicOrHeroSkillValue ("A").intValue ());
	}
	
	/**
	 * Tests the getBasicOrHeroSkillValue method on a normal unit
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetBasicOrHeroSkillValue_Normal () throws Exception
	{
		// Mock database
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);

		final ExperienceLevel expLevel = new ExperienceLevel ();
		expLevel.setLevelNumber (3);
		
		// Unit skills
		final Map<String, Integer> basicSkillValues = new HashMap<String, Integer> ();
		basicSkillValues.put ("A", 2);
		
		// Set up object to test
		final MinimalUnitDetailsImpl mu = new MinimalUnitDetailsImpl (null, unitDef, null, null, null, null, expLevel, basicSkillValues);
		
		// Call method
		assertEquals (2, mu.getBasicOrHeroSkillValue ("A").intValue ());
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
		final MinimalUnitDetailsImpl mu = new MinimalUnitDetailsImpl (null, unitDef, null, null, null, null, null, null);

		// Run method
		unitDef.setFigureCount (4);
		assertEquals (4, mu.getFullFigureCount ());
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
		final MinimalUnitDetailsImpl mu = new MinimalUnitDetailsImpl (null, unitDef, null, null, null, null, expLevel, null);
		
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
		final MinimalUnitDetailsImpl mu = new MinimalUnitDetailsImpl (null, unitDef, null, null, null, null, expLevel, null);
		
		// Call method
		assertEquals (0, mu.calculateFameLostForUnitDying ());
	}
}