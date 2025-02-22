package momime.server.calculations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.coordinates.MapCoordinates2DEx;

import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.PickAndQuantity;
import momime.common.database.RangedAttackTypeEx;
import momime.common.database.Spell;
import momime.common.database.UnitEx;
import momime.common.database.UnitSkillEx;
import momime.common.messages.CombatMapSize;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MemoryUnit;
import momime.common.messages.UnitStatusID;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.PlayerPickUtils;
import momime.server.ServerTestData;
import momime.server.utils.UnitServerUtils;

/**
 * Tests the ServerUnitCalculationsImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestServerUnitCalculationsImpl extends ServerTestData
{
	/**
	 * Tests the calculateUnitScoutingRange class
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateUnitScoutingRange () throws Exception
	{
		// Mock database
		final UnitSkillEx flightSkill = new UnitSkillEx ();
		flightSkill.setUnitSkillScoutingRange (2);
		
		final UnitSkillEx otherSkill = new UnitSkillEx ();

		final UnitSkillEx longSightSkill = new UnitSkillEx ();
		longSightSkill.setUnitSkillScoutingRange (4);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findUnitSkill ("US001", "calculateUnitScoutingRange")).thenReturn (flightSkill);
		when (db.findUnitSkill ("US002", "calculateUnitScoutingRange")).thenReturn (otherSkill);
		when (db.findUnitSkill ("US003", "calculateUnitScoutingRange")).thenReturn (longSightSkill);
		
		// Set up object to test
		final ServerUnitCalculationsImpl calc = new ServerUnitCalculationsImpl ();

		// Unit with no skills and no scouting range
		final ExpandedUnitDetails unit = mock (ExpandedUnitDetails.class);
		assertEquals (1, calc.calculateUnitScoutingRange (unit, db));
		
		// Unit with Scouting III
		when (unit.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_SCOUTING)).thenReturn (true);
		when (unit.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_SCOUTING)).thenReturn (3);
		assertEquals (3, calc.calculateUnitScoutingRange (unit, db));
		
		// Unit with two skills, one which grants Scouting II (like Flight) and one which has nothing at all to do with scouting
		final Set<String> unitSkillIDs = new HashSet<String> ();
		unitSkillIDs.add ("US001");
		unitSkillIDs.add ("US002");
		
		when (unit.listModifiedSkillIDs ()).thenReturn (unitSkillIDs);
		assertEquals (3, calc.calculateUnitScoutingRange (unit, db));
		
		// Unit with a skill which grants Scouting IV
		unitSkillIDs.add ("US003");
		assertEquals (4, calc.calculateUnitScoutingRange (unit, db));
	}

	/**
	 * Tests the calculateRangedAttackDistancePenalty method on a magic attack
	 * @throws Exception If there if a problem
	 */
	@Test
	public final void testCalculateRangedAttackDistancePenalty_Magic () throws Exception
	{
		// RAT
		final RangedAttackTypeEx rat = new RangedAttackTypeEx ();
		rat.setMagicRealmID ("A");

		// Coordinate system
		final CombatMapSize sys = createCombatMapSize ();
		
		// Units
		final ExpandedUnitDetails attacker = mock (ExpandedUnitDetails.class);
		when (attacker.getRangedAttackType ()).thenReturn (rat);
		
		final ExpandedUnitDetails defender = mock (ExpandedUnitDetails.class);
		
		// Set up object to test
		final ServerUnitCalculationsImpl calc = new ServerUnitCalculationsImpl ();
		
		// Run method
		assertEquals (0, calc.calculateRangedAttackDistancePenalty (attacker, defender, sys));
	}

	/**
	 * Tests the calculateRangedAttackDistancePenalty method on a physical attack, but its close enough to get no penalty
	 * @throws Exception If there if a problem
	 */
	@Test
	public final void testCalculateRangedAttackDistancePenalty_Close () throws Exception
	{
		// RAT
		final RangedAttackTypeEx rat = new RangedAttackTypeEx ();

		// Coordinate system
		final CombatMapSize sys = createCombatMapSize ();
		
		// Units
		final ExpandedUnitDetails attacker = mock (ExpandedUnitDetails.class);
		when (attacker.getRangedAttackType ()).thenReturn (rat);
		when (attacker.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 4));
		
		final ExpandedUnitDetails defender = mock (ExpandedUnitDetails.class);
		when (defender.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (6, 4));
		
		// Set up object to test
		final ServerUnitCalculationsImpl calc = new ServerUnitCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		assertEquals (0, calc.calculateRangedAttackDistancePenalty (attacker, defender, sys));
	}

	/**
	 * Tests the calculateRangedAttackDistancePenalty method on a physical attack, at short range so we get a small penalty
	 * @throws Exception If there if a problem
	 */
	@Test
	public final void testCalculateRangedAttackDistancePenalty_Short () throws Exception
	{
		// RAT
		final RangedAttackTypeEx rat = new RangedAttackTypeEx ();

		// Coordinate system
		final CombatMapSize sys = createCombatMapSize ();
		
		// Units
		final ExpandedUnitDetails attacker = mock (ExpandedUnitDetails.class);
		when (attacker.getRangedAttackType ()).thenReturn (rat);
		when (attacker.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 4));
		
		final ExpandedUnitDetails defender = mock (ExpandedUnitDetails.class);
		when (defender.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (6, 6));
		
		// Set up object to test
		final ServerUnitCalculationsImpl calc = new ServerUnitCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		assertEquals (1, calc.calculateRangedAttackDistancePenalty (attacker, defender, sys));
	}

	/**
	 * Tests the calculateRangedAttackDistancePenalty method on a physical attack, at long range so we get a big penalty
	 * @throws Exception If there if a problem
	 */
	@Test
	public final void testCalculateRangedAttackDistancePenalty_Long () throws Exception
	{
		// RAT
		final RangedAttackTypeEx rat = new RangedAttackTypeEx ();

		// Coordinate system
		final CombatMapSize sys = createCombatMapSize ();
		
		// Units
		final ExpandedUnitDetails attacker = mock (ExpandedUnitDetails.class);
		when (attacker.getRangedAttackType ()).thenReturn (rat);
		when (attacker.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (0, 7));
		
		final ExpandedUnitDetails defender = mock (ExpandedUnitDetails.class);
		when (defender.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (7, 6));
		
		// Set up object to test
		final ServerUnitCalculationsImpl calc = new ServerUnitCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		assertEquals (3, calc.calculateRangedAttackDistancePenalty (attacker, defender, sys));
	}

	/**
	 * Tests the calculateRangedAttackDistancePenalty method on a physical attack, where the long range skill applies
	 * @throws Exception If there if a problem
	 */
	@Test
	public final void testCalculateRangedAttackDistancePenalty_LongRange () throws Exception
	{
		// RAT
		final RangedAttackTypeEx rat = new RangedAttackTypeEx ();

		// Coordinate system
		final CombatMapSize sys = createCombatMapSize ();
		
		// Units
		final ExpandedUnitDetails attacker = mock (ExpandedUnitDetails.class);
		when (attacker.getRangedAttackType ()).thenReturn (rat);
		when (attacker.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (0, 7));
		
		final ExpandedUnitDetails defender = mock (ExpandedUnitDetails.class);
		when (defender.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (7, 6));
		
		// We do have the Long Range skill
		when (attacker.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_LONG_RANGE)).thenReturn (true);
		
		// Set up object to test
		final ServerUnitCalculationsImpl calc = new ServerUnitCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		assertEquals (1, calc.calculateRangedAttackDistancePenalty (attacker, defender, sys));
	}
	
	/**
	 * Tests the listUnitsSpellMightSummon method on a normal summoning spell that can only summon one kind of unit with no restrictions
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testListUnitsSpellMightSummon_Normal () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm ("LT01");
		when (db.findUnit ("UN001", "listUnitsSpellMightSummon")).thenReturn (unitDef);
		
		// Units
		final List<MemoryUnit> trueUnits = new ArrayList<MemoryUnit> ();

		// Wizard
		final KnownWizardDetails wizardDetails = new KnownWizardDetails ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.getSummonedUnit ().add ("UN001");
		
		// Set up object to test
		final ServerUnitCalculationsImpl calc = new ServerUnitCalculationsImpl ();
		
		// Run method
		final List<UnitEx> list = calc.listUnitsSpellMightSummon (spell, wizardDetails, trueUnits, db);
		
		// Check results
		assertEquals (1, list.size ());
		assertSame (unitDef, list.get (0));
	}

	/**
	 * Tests the listUnitsSpellMightSummon method on a hero summoning spell that has many choices and must consider the status and prereqs of each hero
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testListUnitsSpellMightSummon_Heroes () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		for (int n = 1; n <= 9; n++)
		{
			final UnitEx unitDef = new UnitEx ();
			unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
			unitDef.setUnitID ("UN00" + n);
			when (db.findUnit (unitDef.getUnitID (), "listUnitsSpellMightSummon")).thenReturn (unitDef);
			
			// 7 & 8 are special hereos that require a particular pick to get
			if ((n == 7) || (n == 8))
			{
				final PickAndQuantity prereq = new PickAndQuantity ();
				prereq.setPickID ("MB0" + n);
				prereq.setQuantity (1);
				unitDef.getUnitPickPrerequisite ().add (prereq);
			}
		}
		
		// Units
		final UnitServerUtils unitServerUtils = mock (UnitServerUtils.class);
		final List<MemoryUnit> trueUnits = new ArrayList<MemoryUnit> ();
		
		for (int n = 1; n <= 9; n++)
		{
			final MemoryUnit mu = new MemoryUnit ();
			if (n == 3)
				mu.setStatus (UnitStatusID.ALIVE);
			else if (n == 5)
				mu.setStatus (UnitStatusID.DEAD);
			else
				mu.setStatus (UnitStatusID.GENERATED);
			
			when (unitServerUtils.findUnitWithPlayerAndID (trueUnits, 3, "UN00" + n)).thenReturn (mu);
		}		

		// Wizard
		final KnownWizardDetails wizardDetails = new KnownWizardDetails ();
		wizardDetails.setPlayerID (3);
		
		// We don't have a MB07, but we do have a MB08
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		when (playerPickUtils.getQuantityOfPick (wizardDetails.getPick (), "MB07")).thenReturn (0);
		when (playerPickUtils.getQuantityOfPick (wizardDetails.getPick (), "MB08")).thenReturn (1);
		
		// Spell
		final Spell spell = new Spell ();
		for (int n = 1; n <= 9; n++)
			spell.getSummonedUnit ().add ("UN00" + n);
		
		// Set up object to test
		final ServerUnitCalculationsImpl calc = new ServerUnitCalculationsImpl ();
		calc.setUnitServerUtils (unitServerUtils);
		calc.setPlayerPickUtils (playerPickUtils);
		
		// Run method
		final List<UnitEx> list = calc.listUnitsSpellMightSummon (spell, wizardDetails, trueUnits, db);
		
		// Check results
		assertEquals (6, list.size ());
		assertEquals ("UN001", list.get (0).getUnitID ()); 
		assertEquals ("UN002", list.get (1).getUnitID ()); 
		assertEquals ("UN004", list.get (2).getUnitID ()); 
		assertEquals ("UN006", list.get (3).getUnitID ()); 
		assertEquals ("UN008", list.get (4).getUnitID ()); 
		assertEquals ("UN009", list.get (5).getUnitID ()); 
	}
}