package momime.common.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import momime.common.database.CommonDatabase;
import momime.common.database.UnitSkill;

/**
 * Tests the ExpandedUnitDetailsImpl class
 */
public final class TestExpandedUnitDetailsImpl
{
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

		// Set up object to test
		final ExpandedUnitDetailsImpl unit = new ExpandedUnitDetailsImpl (null, null, null, null, null, null, null, null, null, basicSkillValues, null, null, null);
		
		// Try with no matching skills
		assertFalse (unit.unitIgnoresCombatTerrain (db));
		
		// Now add one
		basicSkillValues.put ("C", null);
		assertTrue (unit.unitIgnoresCombatTerrain (db));
	}

}