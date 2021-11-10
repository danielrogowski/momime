package momime.server.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import momime.common.MomException;
import momime.common.database.UnitSkillAndValue;
import momime.common.messages.MemoryUnit;

/**
 * Tests the UnitSkillDirectAccessImpl class
 */
public final class TestUnitSkillDirectAccessImpl
{
	/**
	 * Tests the getDirectSkillValue method
	 */
	@Test
	public final void testGetDirectSkillValue ()
	{
		// Create skills list
		final List<UnitSkillAndValue> skills = new ArrayList<UnitSkillAndValue> ();

		final UnitSkillAndValue skillWithValue = new UnitSkillAndValue ();
		skillWithValue.setUnitSkillID ("US001");
		skillWithValue.setUnitSkillValue (5);
		skills.add (skillWithValue);

		final UnitSkillAndValue skillWithoutValue = new UnitSkillAndValue ();
		skillWithoutValue.setUnitSkillID ("US002");
		skills.add (skillWithoutValue);

		// Test values
		final UnitSkillDirectAccessImpl utils = new UnitSkillDirectAccessImpl ();
		assertEquals (5, utils.getDirectSkillValue (skills, "US001"));
		assertEquals (0, utils.getDirectSkillValue (skills, "US002"));
		assertEquals (-1, utils.getDirectSkillValue (skills, "US004"));
	}
	
	/**
	 * Tests the setDirectSkillValue method on a skill that we already have
	 * @throws MomException If this unit didn't previously have the specified skill
	 */
	@Test
	public final void testSetDirectSkil_lExists () throws MomException
	{
		// Create test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitURN (3);

		// Create skills list
		final UnitSkillAndValue skillWithValue = new UnitSkillAndValue ();
		skillWithValue.setUnitSkillID ("US001");
		skillWithValue.setUnitSkillValue (5);
		unit.getUnitHasSkill ().add (skillWithValue);

		final UnitSkillAndValue skillWithoutValue = new UnitSkillAndValue ();
		skillWithoutValue.setUnitSkillID ("US002");
		unit.getUnitHasSkill ().add (skillWithoutValue);

		// Run method
		final UnitSkillDirectAccessImpl utils = new UnitSkillDirectAccessImpl ();
		utils.setDirectSkillValue (unit, "US002", 3);

		// Check results
		assertEquals (2, unit.getUnitHasSkill ().size ());
		assertEquals ("US001", unit.getUnitHasSkill ().get (0).getUnitSkillID ());
		assertEquals (5, unit.getUnitHasSkill ().get (0).getUnitSkillValue ().intValue ());
		assertEquals ("US002", unit.getUnitHasSkill ().get (1).getUnitSkillID ());
		assertEquals (3, unit.getUnitHasSkill ().get (1).getUnitSkillValue ().intValue ());
	}

	/**
	 * Tests the setDirectSkillValue method on a skill that we don't already have
	 * @throws MomException If this unit didn't previously have the specified skill
	 */
	@Test(expected=MomException.class)
	public final void testSetDirectSkill_NotExists () throws MomException
	{
		// Create test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitURN (3);

		// Create skills list
		final UnitSkillAndValue skillWithValue = new UnitSkillAndValue ();
		skillWithValue.setUnitSkillID ("US001");
		skillWithValue.setUnitSkillValue (5);
		unit.getUnitHasSkill ().add (skillWithValue);

		final UnitSkillAndValue skillWithoutValue = new UnitSkillAndValue ();
		skillWithoutValue.setUnitSkillID ("US002");
		unit.getUnitHasSkill ().add (skillWithoutValue);

		// Run method
		final UnitSkillDirectAccessImpl utils = new UnitSkillDirectAccessImpl ();
		utils.setDirectSkillValue (unit, "US003", 3);
	}
}