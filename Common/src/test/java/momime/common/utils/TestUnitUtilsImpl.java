package momime.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.database.AddsToSkill;
import momime.common.database.CombatAreaAffectsPlayersID;
import momime.common.database.CombatAreaEffect;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.GenerateTestData;
import momime.common.database.NegatedBySkill;
import momime.common.database.NegatedByUnitID;
import momime.common.database.Pick;
import momime.common.database.RecordNotFoundException;
import momime.common.database.StoredDamageTypeID;
import momime.common.database.UnitCombatSideID;
import momime.common.database.UnitEx;
import momime.common.database.UnitSkillAndValue;
import momime.common.database.UnitSkillEx;
import momime.common.database.UnitSpecialOrder;
import momime.common.database.UnitSpellEffect;
import momime.common.database.UnitTypeEx;
import momime.common.messages.AvailableUnit;
import momime.common.messages.ConfusionEffect;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MemoryUnitHeroItemSlot;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.UnitDamage;
import momime.common.messages.UnitStatusID;

/**
 * Tests the UnitUtils class
 */
@ExtendWith(MockitoExtension.class)
public final class TestUnitUtilsImpl
{
	/**
	 * Tests the findUnitURN method on a unit that does exist
	 * @throws RecordNotFoundException If unit with requested URN is not found
	 */
	@Test
	public final void testFindUnitURN_Exists () throws RecordNotFoundException
	{
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();
		for (int n = 1; n <= 3; n++)
		{
			final MemoryUnit unit = new MemoryUnit ();
			unit.setUnitURN (n);
			units.add (unit);
		}

		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		assertEquals (2, utils.findUnitURN (2, units).getUnitURN ());
		assertEquals (2, utils.findUnitURN (2, units, "testFindUnitURN_Exists").getUnitURN ());
	}

	/**
	 * Tests the findUnitURN method on a unit that doesn't exist
	 * @throws RecordNotFoundException If unit with requested URN is not found
	 */
	@Test
	public final void testFindUnitURN_NotExists () throws RecordNotFoundException
	{
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();
		for (int n = 1; n <= 3; n++)
		{
			final MemoryUnit unit = new MemoryUnit ();
			unit.setUnitURN (n);
			units.add (unit);
		}

		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		assertNull (utils.findUnitURN (4, units));
		
		assertThrows (RecordNotFoundException.class, () ->
		{
			utils.findUnitURN (4, units, "testFindUnitURN_NotExists");
		});
	}

	/**
	 * Tests the removeUnitURN method on a unit that does exist
	 * @throws RecordNotFoundException If unit with requested URN is not found
	 */
	@Test
	public final void testRemoveUnitURN_Exists () throws RecordNotFoundException
	{
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();
		for (int n = 1; n <= 3; n++)
		{
			final MemoryUnit unit = new MemoryUnit ();
			unit.setUnitURN (n);
			units.add (unit);
		}

		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		utils.removeUnitURN (2, units);
		assertEquals (2, units.size ());
		assertEquals (1, units.get (0).getUnitURN ());
		assertEquals (3, units.get (1).getUnitURN ());
	}

	/**
	 * Tests the removeUnitURN method on a unit that doesn't exist
	 * @throws RecordNotFoundException If unit with requested URN is not found
	 */
	@Test
	public final void testRemoveUnitURN_NotExists () throws RecordNotFoundException
	{
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();
		for (int n = 1; n <= 3; n++)
		{
			final MemoryUnit unit = new MemoryUnit ();
			unit.setUnitURN (n);
			units.add (unit);
		}

		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		assertThrows (RecordNotFoundException.class, () ->
		{
			utils.removeUnitURN (4, units);
		});
	}

	/**
	 * Tests the initializeUnitSkills method with no exp and not reading skills from XML
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 */
	@Test
	public final void testInitializeUnitSkills_NoSkills () throws RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final UnitEx unitDef = new UnitEx ();
		when (db.findUnit ("UN001", "initializeUnitSkills")).thenReturn (unitDef);
		
		// Set up test unit
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID ("UN001");
		
		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();

		// Run method
		assertSame (unitDef, utils.initializeUnitSkills (unit, -1, db));

		// Check results
		assertEquals (0, unit.getUnitHasSkill ().size ());
	}

	/**
	 * Tests the initializeUnitSkills method with trying to pass an exp value on a unit type what doesn't gain exp
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 */
	@Test
	public final void testInitializeUnitSkills_ExpOnUnitThatCannotHaveAny () throws RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm ("MB01");
		when (db.findUnit ("UN001", "initializeUnitSkills")).thenReturn (unitDef);
		
		final Pick unitMagicRealm = new Pick ();
		unitMagicRealm.setUnitTypeID ("N");
		when (db.findPick ("MB01", "initializeUnitSkills")).thenReturn (unitMagicRealm);
		
		final UnitTypeEx unitType = new UnitTypeEx ();
		when (db.findUnitType ("N", "initializeUnitSkills")).thenReturn (unitType);
		
		// Set up test unit
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID ("UN001");
		
		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();

		// Run method
		assertSame (unitDef, utils.initializeUnitSkills (unit, 100, db));

		// Check results
		assertEquals (0, unit.getUnitHasSkill ().size ());
	}

	/**
	 * Tests the initializeUnitSkills method with no exp and not reading skills from XML
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 */
	@Test
	public final void testInitializeUnitSkills_ExpOnly () throws RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm ("MB01");
		when (db.findUnit ("UN001", "initializeUnitSkills")).thenReturn (unitDef);
		
		final Pick unitMagicRealm = new Pick ();
		unitMagicRealm.setUnitTypeID ("N");
		when (db.findPick ("MB01", "initializeUnitSkills")).thenReturn (unitMagicRealm);
		
		final UnitTypeEx unitType = new UnitTypeEx ();
		unitType.getExperienceLevel ().add (null);		// Any record here is good enough
		when (db.findUnitType ("N", "initializeUnitSkills")).thenReturn (unitType);
		
		// Set up test unit
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID ("UN001");
		
		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();

		// Run method
		assertSame (unitDef, utils.initializeUnitSkills (unit, 100, db));

		// Check results
		assertEquals (1, unit.getUnitHasSkill ().size ());
		assertEquals (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE, unit.getUnitHasSkill ().get (0).getUnitSkillID ());
		assertEquals (100, unit.getUnitHasSkill ().get (0).getUnitSkillValue ().intValue ());
	}

	/**
	 * Tests the initializeUnitSkills method with no exp, but reading skills from XML
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 */
	@Test
	public final void testInitializeUnitSkills_SkillsOnly () throws RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final UnitSkillAndValue unitDefSkill = new UnitSkillAndValue ();
		unitDefSkill.setUnitSkillID ("US001");
		unitDefSkill.setUnitSkillValue (5);
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm ("MB01");
		unitDef.getUnitHasSkill ().add (unitDefSkill);
		when (db.findUnit ("UN001", "initializeUnitSkills")).thenReturn (unitDef);
		
		final Pick unitMagicRealm = new Pick ();
		unitMagicRealm.setUnitTypeID ("N");
		when (db.findPick ("MB01", "initializeUnitSkills")).thenReturn (unitMagicRealm);
		
		final UnitTypeEx unitType = new UnitTypeEx ();
		when (db.findUnitType ("N", "initializeUnitSkills")).thenReturn (unitType);
		
		// Set up test unit
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID ("UN001");
		
		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();

		// Run method
		assertSame (unitDef, utils.initializeUnitSkills (unit, 100, db));

		// Check results
		assertEquals (1, unit.getUnitHasSkill ().size ());
		assertEquals ("US001", unit.getUnitHasSkill ().get (0).getUnitSkillID ());
		assertEquals (5, unit.getUnitHasSkill ().get (0).getUnitSkillValue ().intValue ());
	}

	/**
	 * Tests the initializeUnitSkills method with exp and reading skills from XML
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 */
	@Test
	public final void testInitializeUnitSkills_ExpAndSkills () throws RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final UnitSkillAndValue unitDefSkill = new UnitSkillAndValue ();
		unitDefSkill.setUnitSkillID ("US001");
		unitDefSkill.setUnitSkillValue (5);
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm ("MB01");
		unitDef.getUnitHasSkill ().add (unitDefSkill);
		when (db.findUnit ("UN001", "initializeUnitSkills")).thenReturn (unitDef);
		
		final Pick unitMagicRealm = new Pick ();
		unitMagicRealm.setUnitTypeID ("N");
		when (db.findPick ("MB01", "initializeUnitSkills")).thenReturn (unitMagicRealm);
		
		final UnitTypeEx unitType = new UnitTypeEx ();
		unitType.getExperienceLevel ().add (null);		// Any record here is good enough
		when (db.findUnitType ("N", "initializeUnitSkills")).thenReturn (unitType);
		
		// Set up test unit
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID ("UN001");
		
		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();

		// Run method
		assertSame (unitDef, utils.initializeUnitSkills (unit, 100, db));

		// Check results
		assertEquals (2, unit.getUnitHasSkill ().size ());
		assertEquals (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE, unit.getUnitHasSkill ().get (0).getUnitSkillID ());
		assertEquals (100, unit.getUnitHasSkill ().get (0).getUnitSkillValue ().intValue ());

		assertEquals ("US001", unit.getUnitHasSkill ().get (1).getUnitSkillID ());
		assertEquals (5, unit.getUnitHasSkill ().get (1).getUnitSkillValue ().intValue ());
	}
	
	/**
	 * Tests the isSkillNegated method when its negated by another of our own skills (e.g. Stone Skin is negated by Iron Skin)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsSkillNegated_Ours () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final NegatedBySkill negatedBy = new NegatedBySkill ();
		negatedBy.setNegatedBySkillID ("US002");
		negatedBy.setNegatedByUnitID (NegatedByUnitID.OUR_UNIT);
		
		final UnitSkillEx negatedSkill = new UnitSkillEx ();
		negatedSkill.getNegatedBySkill ().add (negatedBy);
		when (db.findUnitSkill ("US001", "isSkillNegated")).thenReturn (negatedSkill);
		
		// Our skills
		final Map<String, Object> ourSkillValues = new HashMap<String, Object> ();
		ourSkillValues.put ("US002", null);
		
		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		// Run method
		assertTrue (utils.isSkillNegated ("US001", ourSkillValues, null, db));
	}
	
	/**
	 * Tests the isSkillNegated method when its negated by another of our own skills, but we don't have that skill
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsSkillNegated_Ours_DontHaveNecessarySkill () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final NegatedBySkill negatedBy = new NegatedBySkill ();
		negatedBy.setNegatedBySkillID ("US002");
		negatedBy.setNegatedByUnitID (NegatedByUnitID.OUR_UNIT);
		
		final UnitSkillEx negatedSkill = new UnitSkillEx ();
		negatedSkill.getNegatedBySkill ().add (negatedBy);
		when (db.findUnitSkill ("US001", "isSkillNegated")).thenReturn (negatedSkill);
		
		// Our skills
		final Map<String, Object> ourSkillValues = new HashMap<String, Object> ();
		ourSkillValues.put ("US003", null);
		
		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		// Run method
		assertFalse (utils.isSkillNegated ("US001", ourSkillValues, null, db));
	}
	
	/**
	 * Tests the isSkillNegated method when its negated by an enemy unit skill (e.g First Strike is negated by Negate First Strike)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsSkillNegated_Enemy () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final NegatedBySkill negatedBy = new NegatedBySkill ();
		negatedBy.setNegatedBySkillID ("US002");
		negatedBy.setNegatedByUnitID (NegatedByUnitID.ENEMY_UNIT);
		
		final UnitSkillEx negatedSkill = new UnitSkillEx ();
		negatedSkill.getNegatedBySkill ().add (negatedBy);
		when (db.findUnitSkill ("US001", "isSkillNegated")).thenReturn (negatedSkill);
		
		// Our skills
		final Map<String, Object> ourSkillValues = new HashMap<String, Object> ();
		ourSkillValues.put ("US002", null);
		
		// Enemy units
		final ExpandedUnitDetails xu1 = mock (ExpandedUnitDetails.class);
		when (xu1.hasModifiedSkill ("US002")).thenReturn (false);

		final ExpandedUnitDetails xu2 = mock (ExpandedUnitDetails.class);
		when (xu2.hasModifiedSkill ("US002")).thenReturn (true);
		
		final List<ExpandedUnitDetails> enemyUnits = Arrays.asList (xu1, xu2);
		
		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		// Run method
		assertTrue (utils.isSkillNegated ("US001", ourSkillValues, enemyUnits, db));
	}
	
	/**
	 * Tests the isSkillNegated method when its negated by an enemy unit skill, but they don't have it
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsSkillNegated_Enemy_DontHaveNecessarySkill () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final NegatedBySkill negatedBy = new NegatedBySkill ();
		negatedBy.setNegatedBySkillID ("US002");
		negatedBy.setNegatedByUnitID (NegatedByUnitID.ENEMY_UNIT);
		
		final UnitSkillEx negatedSkill = new UnitSkillEx ();
		negatedSkill.getNegatedBySkill ().add (negatedBy);
		when (db.findUnitSkill ("US001", "isSkillNegated")).thenReturn (negatedSkill);
		
		// Our skills
		final Map<String, Object> ourSkillValues = new HashMap<String, Object> ();
		ourSkillValues.put ("US002", null);
		
		// Enemy units
		final ExpandedUnitDetails xu1 = mock (ExpandedUnitDetails.class);
		when (xu1.hasModifiedSkill ("US002")).thenReturn (false);

		final List<ExpandedUnitDetails> enemyUnits = Arrays.asList (xu1);
		
		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		// Run method
		assertFalse (utils.isSkillNegated ("US001", ourSkillValues, enemyUnits, db));
	}
	
	/**
	 * Tests the doesCombatAreaEffectApplyToUnit method on Magic Vortex, which CAEs never apply to
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Vortex () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getUnitsThatMoveThroughOtherUnits ()).thenReturn (Arrays.asList ("UN002"));

		// Create test unit
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID ("UN002");
		
		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		// Even though CAE is global and for the right player, with affects blank it shouldn't apply
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (unit, null, db));
	}
	
	/**
	 * Tests the doesCombatAreaEffectApplyToUnit method on an available unit and a CAE with affects players = blank
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Available_AffectsBlank () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final CombatAreaEffect caeDef = new CombatAreaEffect ();
		when (db.findCombatAreaEffect ("CAE01", "doesCombatAreaEffectApplyToUnit")).thenReturn (caeDef);
		
		// Create test unit
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		unit.setOwningPlayerID (1);

		final MemoryCombatAreaEffect effect = new MemoryCombatAreaEffect ();
		effect.setCombatAreaEffectID ("CAE01");
		effect.setCastingPlayerID (1);

		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		// Even though CAE is global and for the right player, with affects blank it shouldn't apply
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (unit, effect, db));
	}

	/**
	 * Tests the doesCombatAreaEffectApplyToUnit method on an available unit and a CAE with affects players = all
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Available_AffectsAll () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final CombatAreaEffect caeDef = new CombatAreaEffect ();
		caeDef.setCombatAreaAffectsPlayers (CombatAreaAffectsPlayersID.ALL_EVEN_NOT_IN_COMBAT);
		when (db.findCombatAreaEffect ("CAE01", "doesCombatAreaEffectApplyToUnit")).thenReturn (caeDef);
		
		// Create test units
		final AvailableUnit ourUnit = new AvailableUnit ();
		ourUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		ourUnit.setOwningPlayerID (1);

		final AvailableUnit theirUnit = new AvailableUnit ();
		theirUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		theirUnit.setOwningPlayerID (2);
		
		final MemoryCombatAreaEffect effect = new MemoryCombatAreaEffect ();
		effect.setCombatAreaEffectID ("CAE01");
		effect.setCastingPlayerID (1);

		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		// Global all players CAE should affect all players - don't need to worry about in combat or not, since available units can't be in combat
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));

		// Localise the CAE in the same spot as the units - should still apply
		effect.setMapLocation (new MapCoordinates3DEx (15, 10, 1));
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));

		// Move the CAE to a different spot than the units - should no longer apply
		effect.getMapLocation ().setX (16);
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));
	}

	/**
	 * Tests the doesCombatAreaEffectApplyToUnit method on an available unit and a CAE with affects players = caster
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Available_AffectsCaster () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final CombatAreaEffect caeDef = new CombatAreaEffect ();
		caeDef.setCombatAreaAffectsPlayers (CombatAreaAffectsPlayersID.CASTER_ONLY);
		when (db.findCombatAreaEffect ("CAE01", "doesCombatAreaEffectApplyToUnit")).thenReturn (caeDef);
		
		// Create test units
		final AvailableUnit ourUnit = new AvailableUnit ();
		ourUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		ourUnit.setOwningPlayerID (1);

		final AvailableUnit theirUnit = new AvailableUnit ();
		theirUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		theirUnit.setOwningPlayerID (2);
		
		final MemoryCombatAreaEffect effect = new MemoryCombatAreaEffect ();
		effect.setCombatAreaEffectID ("CAE01");
		effect.setCastingPlayerID (1);

		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		// Global caster CAE should affect only the caster
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));

		// Localise the CAE in the same spot as the units - however for Caster Only, this means the units also have to be in combat, which they aren't so it still doesn't apply
		effect.setMapLocation (new MapCoordinates3DEx (15, 10, 1));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));

		// Move the CAE to a different spot than the units - should no longer apply
		effect.getMapLocation ().setX (16);
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));
	}

	/**
	 * Tests the doesCombatAreaEffectApplyToUnit method on an available unit and a CAE with affects players = both
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Available_AffectsBothInCombat () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final CombatAreaEffect caeDef = new CombatAreaEffect ();
		caeDef.setCombatAreaAffectsPlayers (CombatAreaAffectsPlayersID.BOTH_PLAYERS_IN_COMBAT);
		when (db.findCombatAreaEffect ("CAE01", "doesCombatAreaEffectApplyToUnit")).thenReturn (caeDef);
		
		// Create test units
		final AvailableUnit ourUnit = new AvailableUnit ();
		ourUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		ourUnit.setOwningPlayerID (1);

		final AvailableUnit theirUnit = new AvailableUnit ();
		theirUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		theirUnit.setOwningPlayerID (2);
		
		final MemoryCombatAreaEffect effect = new MemoryCombatAreaEffect ();
		effect.setCombatAreaEffectID ("CAE01");
		effect.setCastingPlayerID (1);

		// Any settings make no difference, since available units cannot be in combat

		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		// Global both CAE should affect both combat players, but available units can't be in combat
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));

		// Localise the CAE in the same spot as the units
		effect.setMapLocation (new MapCoordinates3DEx (15, 10, 1));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));

		// Move the CAE to a different spot than the units
		effect.getMapLocation ().setX (16);
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));
	}

	/**
	 * Tests the doesCombatAreaEffectApplyToUnit method on an available unit and a CAE with affects players = opponent
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Available_AffectsOpponent () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final CombatAreaEffect caeDef = new CombatAreaEffect ();
		caeDef.setCombatAreaAffectsPlayers (CombatAreaAffectsPlayersID.COMBAT_OPPONENT);
		when (db.findCombatAreaEffect ("CAE01", "doesCombatAreaEffectApplyToUnit")).thenReturn (caeDef);
		
		// Create test units
		final AvailableUnit ourUnit = new AvailableUnit ();
		ourUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		ourUnit.setOwningPlayerID (1);

		final AvailableUnit theirUnit = new AvailableUnit ();
		theirUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		theirUnit.setOwningPlayerID (2);
		
		final MemoryCombatAreaEffect effect = new MemoryCombatAreaEffect ();
		effect.setCombatAreaEffectID ("CAE01");
		effect.setCastingPlayerID (1);

		// Any settings make no difference, since available units cannot be in combat so there can be no opponent

		// Global opponent CAE should only combat opponent, but available units can't be in combat
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));

		// Localise the CAE in the same spot as the units
		effect.setMapLocation (new MapCoordinates3DEx (15, 10, 1));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));

		// Move the CAE to a different spot than the units
		effect.getMapLocation ().setX (16);
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));
	}

	/**
	 * Tests the doesCombatAreaEffectApplyToUnit method on a real unit and a CAE with affects players = blank
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Real_AffectsBlank () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final CombatAreaEffect caeDef = new CombatAreaEffect ();
		when (db.findCombatAreaEffect ("CAE01", "doesCombatAreaEffectApplyToUnit")).thenReturn (caeDef);
		
		// Create test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		unit.setOwningPlayerID (1);

		final MemoryCombatAreaEffect effect = new MemoryCombatAreaEffect ();
		effect.setCombatAreaEffectID ("CAE01");
		effect.setCastingPlayerID (1);

		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();

		// Even though CAE is global and for the right player, with affects blank it shouldn't apply
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (unit, effect, db));
	}

	/**
	 * Tests the doesCombatAreaEffectApplyToUnit method on a real unit and a CAE with affects players = all
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Real_AffectsAll () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final CombatAreaEffect caeDef = new CombatAreaEffect ();
		caeDef.setCombatAreaAffectsPlayers (CombatAreaAffectsPlayersID.ALL_EVEN_NOT_IN_COMBAT);
		when (db.findCombatAreaEffect ("CAE01", "doesCombatAreaEffectApplyToUnit")).thenReturn (caeDef);
		
		// Create test units
		final MemoryUnit ourUnit = new MemoryUnit ();
		ourUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		ourUnit.setOwningPlayerID (1);

		final MemoryUnit theirUnit = new MemoryUnit ();
		theirUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		theirUnit.setOwningPlayerID (2);
		
		final MemoryCombatAreaEffect effect = new MemoryCombatAreaEffect ();
		effect.setCombatAreaEffectID ("CAE01");
		effect.setCastingPlayerID (1);

		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();

		// Global all CAE should affect all players regardless of location or whether in combat or not
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));

		// Localise the CAE in the same spot as the units - should still apply
		effect.setMapLocation (new MapCoordinates3DEx (15, 10, 1));
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));

		// Move the CAE to a different spot than the units - should no longer apply
		effect.getMapLocation ().setX (16);
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));

		// Put the unit into combat - Note the units are at 15,10,1 but in a combat at 16,10,1 which is the location of the effect, so it should apply
		ourUnit.setCombatLocation (new MapCoordinates3DEx (16, 10, 1));
		ourUnit.setCombatPosition (new MapCoordinates2DEx (5, 6));
		ourUnit.setCombatHeading (1);
		ourUnit.setCombatSide (UnitCombatSideID.ATTACKER);
		
		theirUnit.setCombatLocation (new MapCoordinates3DEx (16, 10, 1));
		theirUnit.setCombatPosition (new MapCoordinates2DEx (15, 12));
		theirUnit.setCombatHeading (5);
		theirUnit.setCombatSide (UnitCombatSideID.DEFENDER);
		
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));

		// Move the CAE away from the combat (note we're moving it to the location the units are actually at) - should no longer apply
		effect.getMapLocation ().setX (15);
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));
	}

	/**
	 * Tests the doesCombatAreaEffectApplyToUnit method on a real unit and a CAE with affects players = caster
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Real_AffectsCaster () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final CombatAreaEffect caeDef = new CombatAreaEffect ();
		caeDef.setCombatAreaAffectsPlayers (CombatAreaAffectsPlayersID.CASTER_ONLY);
		when (db.findCombatAreaEffect ("CAE01", "doesCombatAreaEffectApplyToUnit")).thenReturn (caeDef);
		
		// Create test units
		final MemoryUnit ourUnit = new MemoryUnit ();
		ourUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		ourUnit.setOwningPlayerID (1);

		final MemoryUnit theirUnit = new MemoryUnit ();
		theirUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		theirUnit.setOwningPlayerID (2);
		
		final MemoryCombatAreaEffect effect = new MemoryCombatAreaEffect ();
		effect.setCombatAreaEffectID ("CAE01");
		effect.setCastingPlayerID (1);

		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();

		// Global caster CAE should affect only the caster
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));

		// Localise the CAE in the same spot as the units - however for Caster Only, this means the units also have to be in combat, which they aren't so it still doesn't apply
		effect.setMapLocation (new MapCoordinates3DEx (15, 10, 1));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));

		// Move the CAE to a different spot than the units - should no longer apply
		effect.getMapLocation ().setX (16);
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));

		// Put the unit into combat - Note the units are at 0,0,0 but in a combat at 1,0,0 which is the location of the effect, so it should apply
		ourUnit.setCombatLocation (new MapCoordinates3DEx (16, 10, 1));
		ourUnit.setCombatPosition (new MapCoordinates2DEx (5, 6));
		ourUnit.setCombatHeading (1);
		ourUnit.setCombatSide (UnitCombatSideID.ATTACKER);
		
		theirUnit.setCombatLocation (new MapCoordinates3DEx (16, 10, 1));
		theirUnit.setCombatPosition (new MapCoordinates2DEx (15, 12));
		theirUnit.setCombatHeading (5);
		theirUnit.setCombatSide (UnitCombatSideID.DEFENDER);
		
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));

		// Move the CAE away from the combat (note we're moving it to the location the units are actually at) - should no longer apply
		effect.getMapLocation ().setX (15);
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));
	}

	/**
	 * Tests the doesCombatAreaEffectApplyToUnit method on a real unit and a CAE with affects players = both
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Real_AffectsBothInCombat () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final CombatAreaEffect caeDef = new CombatAreaEffect ();
		caeDef.setCombatAreaAffectsPlayers (CombatAreaAffectsPlayersID.BOTH_PLAYERS_IN_COMBAT);
		when (db.findCombatAreaEffect ("CAE01", "doesCombatAreaEffectApplyToUnit")).thenReturn (caeDef);
		
		// Create test units
		final MemoryUnit ourUnit = new MemoryUnit ();
		ourUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		ourUnit.setOwningPlayerID (1);

		final MemoryUnit theirUnit = new MemoryUnit ();
		theirUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		theirUnit.setOwningPlayerID (2);
		
		final MemoryCombatAreaEffect effect = new MemoryCombatAreaEffect ();
		effect.setCombatAreaEffectID ("CAE01");
		effect.setCastingPlayerID (1);

		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();

		// Any settings make no difference until we put the unit into combat

		// Global both CAE should affect both combat players, but available units can't be in combat
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));

		// Localise the CAE in the same spot as the units
		effect.setMapLocation (new MapCoordinates3DEx (15, 10, 1));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));

		// Move the CAE to a different spot than the units
		effect.getMapLocation ().setX (16);
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));

		// Put the unit into combat - Note the units are at 0,0,0 but in a combat at 1,0,0 which is the location of the effect, so it should apply
		ourUnit.setCombatLocation (new MapCoordinates3DEx (16, 10, 1));
		ourUnit.setCombatPosition (new MapCoordinates2DEx (5, 6));
		ourUnit.setCombatHeading (1);
		ourUnit.setCombatSide (UnitCombatSideID.ATTACKER);
		
		theirUnit.setCombatLocation (new MapCoordinates3DEx (16, 10, 1));
		theirUnit.setCombatPosition (new MapCoordinates2DEx (15, 12));
		theirUnit.setCombatHeading (5);
		theirUnit.setCombatSide (UnitCombatSideID.DEFENDER);
		
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));

		// Move the CAE away from the combat (note we're moving it to the location the units are actually at) - should no longer apply
		effect.getMapLocation ().setX (15);
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));
	}

	/**
	 * Tests the doesCombatAreaEffectApplyToUnit method on a real unit and a CAE with affects players = opponent
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Real_AffectsOpponent () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final CombatAreaEffect caeDef = new CombatAreaEffect ();
		caeDef.setCombatAreaAffectsPlayers (CombatAreaAffectsPlayersID.COMBAT_OPPONENT);
		when (db.findCombatAreaEffect ("CAE01", "doesCombatAreaEffectApplyToUnit")).thenReturn (caeDef);
		
		// Create test units
		final MemoryUnit ourUnit = new MemoryUnit ();
		ourUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		ourUnit.setOwningPlayerID (1);

		final MemoryUnit theirUnit = new MemoryUnit ();
		theirUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		theirUnit.setOwningPlayerID (2);
		
		final MemoryCombatAreaEffect effect = new MemoryCombatAreaEffect ();
		effect.setCombatAreaEffectID ("CAE01");
		effect.setCastingPlayerID (1);

		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();

		// Any settings make no difference until we put the unit into combat

		// Global opponent CAE should only combat opponent, but available units can't be in combat
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));

		// Localise the CAE in the same spot as the units
		effect.setMapLocation (new MapCoordinates3DEx (15, 10, 1));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));

		// Move the CAE to a different spot than the units
		effect.getMapLocation ().setX (16);
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));

		// Put the unit into combat - Note the units are at 0,0,0 but in a combat at 1,0,0 which is the location of the effect, so it should apply
		ourUnit.setCombatLocation (new MapCoordinates3DEx (16, 10, 1));
		ourUnit.setCombatPosition (new MapCoordinates2DEx (5, 6));
		ourUnit.setCombatHeading (1);
		ourUnit.setCombatSide (UnitCombatSideID.ATTACKER);
		
		theirUnit.setCombatLocation (new MapCoordinates3DEx (16, 10, 1));
		theirUnit.setCombatPosition (new MapCoordinates2DEx (15, 12));
		theirUnit.setCombatHeading (5);
		theirUnit.setCombatSide (UnitCombatSideID.DEFENDER);
		
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));

		// Move the CAE away from the combat (note we're moving it to the location the units are actually at) - should no longer apply
		effect.getMapLocation ().setX (15);
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, db));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, db));
	}

	/**
	 * Tests the listUnitURNs method
	 * @throws Exception If there is a problem
	 */
	@SuppressWarnings ({"rawtypes", "unchecked"})
	@Test
	public final void testListUnitURNs () throws Exception
	{
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		// Test on null list
		assertEquals ("()", utils.listUnitURNs (null));

		// Test on list with single unit
		final List units = new ArrayList ();
		final MemoryUnit one = new MemoryUnit ();
		one.setUnitURN (1);
		units.add (one);

		assertEquals ("(1)", utils.listUnitURNs (units));

		// Test on list with multiple units
		final MemoryUnit five = new MemoryUnit ();
		five.setUnitURN (5);
		units.add (five);

		final ExpandedUnitDetails xuThree = mock (ExpandedUnitDetails.class);
		when (xuThree.getUnitURN ()).thenReturn (3);
		when (xuThree.isMemoryUnit ()).thenReturn (true);
		units.add (xuThree);

		assertEquals ("(1, 5, 3)", utils.listUnitURNs (units));
	}

	/**
	 * Tests the findFirstAliveEnemyAtLocation method
	 */
	@Test
	public final void testFindFirstAliveEnemyAtLocation ()
	{
		// Put into a list units that meet every criteria except one
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();

		// Null location
		final MemoryUnit u1 = new MemoryUnit ();
		u1.setOwningPlayerID (5);
		u1.setStatus (UnitStatusID.ALIVE);
		units.add (u1);

		// Wrong location
		final MemoryUnit u2 = new MemoryUnit ();
		u2.setOwningPlayerID (5);
		u2.setStatus (UnitStatusID.ALIVE);
		u2.setUnitLocation (new MapCoordinates3DEx (2, 3, 0));
		units.add (u2);

		// Wrong player (i.e. player matches)
		final MemoryUnit u3 = new MemoryUnit ();
		u3.setOwningPlayerID (4);
		u3.setStatus (UnitStatusID.ALIVE);
		u3.setUnitLocation (new MapCoordinates3DEx (2, 3, 1));
		units.add (u3);

		// Null status
		final MemoryUnit u4 = new MemoryUnit ();
		u4.setOwningPlayerID (5);
		u4.setUnitLocation (new MapCoordinates3DEx (2, 3, 1));
		units.add (u4);

		// Unit is dead
		final MemoryUnit u5 = new MemoryUnit ();
		u5.setOwningPlayerID (5);
		u5.setStatus (UnitStatusID.DEAD);
		u5.setUnitLocation (new MapCoordinates3DEx (2, 3, 1));
		units.add (u5);

		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		assertNull (utils.findFirstAliveEnemyAtLocation (units, 2, 3, 1, 4));

		// Now add one that actually matches
		final MemoryUnit u6 = new MemoryUnit ();
		u6.setOwningPlayerID (5);
		u6.setStatus (UnitStatusID.ALIVE);
		u6.setUnitLocation (new MapCoordinates3DEx (2, 3, 1));
		units.add (u6);

		assertSame (u6, utils.findFirstAliveEnemyAtLocation (units, 2, 3, 1, 4));
	}
	
	/**
	 * Tests the findFirstAliveEnemyWeCanSeeAtLocation method
	 */
	@Test
	public final void testFindFirstAliveEnemyWeCanSeeAtLocation ()
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Put into a list units that meet every criteria except one
		final FogOfWarMemory mem = new FogOfWarMemory ();
		final UnitVisibilityUtils unitVisibilityUtils = mock (UnitVisibilityUtils.class);
		
		// Null location
		final MemoryUnit u1 = new MemoryUnit ();
		u1.setOwningPlayerID (5);
		u1.setStatus (UnitStatusID.ALIVE);
		mem.getUnit ().add (u1);

		// Wrong location
		final MemoryUnit u2 = new MemoryUnit ();
		u2.setOwningPlayerID (5);
		u2.setStatus (UnitStatusID.ALIVE);
		u2.setUnitLocation (new MapCoordinates3DEx (2, 3, 0));
		mem.getUnit ().add (u2);

		// Wrong player (i.e. player matches)
		final MemoryUnit u3 = new MemoryUnit ();
		u3.setOwningPlayerID (4);
		u3.setStatus (UnitStatusID.ALIVE);
		u3.setUnitLocation (new MapCoordinates3DEx (2, 3, 1));
		mem.getUnit ().add (u3);

		// Null status
		final MemoryUnit u4 = new MemoryUnit ();
		u4.setOwningPlayerID (5);
		u4.setUnitLocation (new MapCoordinates3DEx (2, 3, 1));
		mem.getUnit ().add (u4);

		// Unit is dead
		final MemoryUnit u5 = new MemoryUnit ();
		u5.setOwningPlayerID (5);
		u5.setStatus (UnitStatusID.DEAD);
		u5.setUnitLocation (new MapCoordinates3DEx (2, 3, 1));
		mem.getUnit ().add (u5);
		
		// Unit is invisible
		final MemoryUnit u6 = new MemoryUnit ();
		u6.setOwningPlayerID (5);
		u6.setStatus (UnitStatusID.ALIVE);
		u6.setUnitLocation (new MapCoordinates3DEx (2, 3, 1));
		mem.getUnit ().add (u6);
		
		when (unitVisibilityUtils.canSeeUnitOverland (u6, 4, mem.getMaintainedSpell (), db)).thenReturn (false);
		
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		utils.setUnitVisibilityUtils (unitVisibilityUtils);
		assertNull (utils.findFirstAliveEnemyWeCanSeeAtLocation (4, mem, 2, 3, 1, 4, db));
		
		// Now add one that actually matches
		final MemoryUnit u7 = new MemoryUnit ();
		u7.setOwningPlayerID (5);
		u7.setStatus (UnitStatusID.ALIVE);
		u7.setUnitLocation (new MapCoordinates3DEx (2, 3, 1));
		mem.getUnit ().add (u7);
		
		when (unitVisibilityUtils.canSeeUnitOverland (u7, 4, mem.getMaintainedSpell (), db)).thenReturn (true);

		assertSame (u7, utils.findFirstAliveEnemyWeCanSeeAtLocation (4, mem, 2, 3, 1, 4, db));
	}

	/**
	 * Tests the listAliveEnemiesAtLocation method
	 */
	@Test
	public final void testListAliveEnemiesAtLocation ()
	{
		// Put into a list units that meet every criteria except one
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();

		// Null location
		final MemoryUnit u1 = new MemoryUnit ();
		u1.setOwningPlayerID (5);
		u1.setStatus (UnitStatusID.ALIVE);
		units.add (u1);

		// Wrong location
		final MemoryUnit u2 = new MemoryUnit ();
		u2.setOwningPlayerID (5);
		u2.setStatus (UnitStatusID.ALIVE);
		u2.setUnitLocation (new MapCoordinates3DEx (2, 3, 0));
		units.add (u2);

		// Wrong player (i.e. player matches)
		final MemoryUnit u3 = new MemoryUnit ();
		u3.setOwningPlayerID (4);
		u3.setStatus (UnitStatusID.ALIVE);
		u3.setUnitLocation (new MapCoordinates3DEx (2, 3, 1));
		units.add (u3);

		// Null status
		final MemoryUnit u4 = new MemoryUnit ();
		u4.setOwningPlayerID (5);
		u4.setUnitLocation (new MapCoordinates3DEx (2, 3, 1));
		units.add (u4);

		// Unit is dead
		final MemoryUnit u5 = new MemoryUnit ();
		u5.setOwningPlayerID (5);
		u5.setStatus (UnitStatusID.DEAD);
		u5.setUnitLocation (new MapCoordinates3DEx (2, 3, 1));
		units.add (u5);

		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		assertEquals (0, utils.listAliveEnemiesAtLocation (units, 2, 3, 1, 4).size ());

		// Now add one that actually matches
		final MemoryUnit u6 = new MemoryUnit ();
		u6.setOwningPlayerID (5);
		u6.setStatus (UnitStatusID.ALIVE);
		u6.setUnitLocation (new MapCoordinates3DEx (2, 3, 1));
		units.add (u6);

		final List<MemoryUnit> list1 = utils.listAliveEnemiesAtLocation (units, 2, 3, 1, 4);
		assertEquals (1, list1.size ());
		assertSame (u6, list1.get (0));

		// Add second matching unit
		final MemoryUnit u7 = new MemoryUnit ();
		u7.setOwningPlayerID (5);
		u7.setStatus (UnitStatusID.ALIVE);
		u7.setUnitLocation (new MapCoordinates3DEx (2, 3, 1));
		units.add (u7);

		final List<MemoryUnit> list2 = utils.listAliveEnemiesAtLocation (units, 2, 3, 1, 4);
		assertEquals (2, list2.size ());
		assertSame (u6, list2.get (0));
		assertSame (u7, list2.get (1));
	}
	
	/**
	 * Tests the countAliveEnemiesAtLocation method
	 */
	@Test
	public final void testCountAliveEnemiesAtLocation ()
	{
		// Put into a list units that meet every criteria except one
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();

		// Null location
		final MemoryUnit u1 = new MemoryUnit ();
		u1.setOwningPlayerID (5);
		u1.setStatus (UnitStatusID.ALIVE);
		units.add (u1);

		// Wrong location
		final MemoryUnit u2 = new MemoryUnit ();
		u2.setOwningPlayerID (5);
		u2.setStatus (UnitStatusID.ALIVE);
		u2.setUnitLocation (new MapCoordinates3DEx (2, 3, 0));
		units.add (u2);

		// Wrong player (i.e. player matches)
		final MemoryUnit u3 = new MemoryUnit ();
		u3.setOwningPlayerID (4);
		u3.setStatus (UnitStatusID.ALIVE);
		u3.setUnitLocation (new MapCoordinates3DEx (2, 3, 1));
		units.add (u3);

		// Null status
		final MemoryUnit u4 = new MemoryUnit ();
		u4.setOwningPlayerID (5);
		u4.setUnitLocation (new MapCoordinates3DEx (2, 3, 1));
		units.add (u4);

		// Unit is dead
		final MemoryUnit u5 = new MemoryUnit ();
		u5.setOwningPlayerID (5);
		u5.setStatus (UnitStatusID.DEAD);
		u5.setUnitLocation (new MapCoordinates3DEx (2, 3, 1));
		units.add (u5);

		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		assertEquals (0, utils.countAliveEnemiesAtLocation (units, 2, 3, 1, 4));

		// Now add one that actually matches
		final MemoryUnit u6 = new MemoryUnit ();
		u6.setOwningPlayerID (5);
		u6.setStatus (UnitStatusID.ALIVE);
		u6.setUnitLocation (new MapCoordinates3DEx (2, 3, 1));
		units.add (u6);

		assertEquals (1, utils.countAliveEnemiesAtLocation (units, 2, 3, 1, 4));

		// Add second matching unit
		final MemoryUnit u7 = new MemoryUnit ();
		u7.setOwningPlayerID (5);
		u7.setStatus (UnitStatusID.ALIVE);
		u7.setUnitLocation (new MapCoordinates3DEx (2, 3, 1));
		units.add (u7);

		assertEquals (2, utils.countAliveEnemiesAtLocation (units, 2, 3, 1, 4));
	}
	
	/**
	 * Tests the findAliveUnitInCombatAt method
	 */
	@Test
	public final void testFindAliveUnitInCombatAt ()
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getUnitsThatMoveThroughOtherUnits ()).thenReturn (Arrays.asList ("UN001"));
		
		// Put into a list units that meet every criteria except one
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();

		// Unit is dead
		final MemoryUnit u1 = new MemoryUnit ();
		u1.setUnitID ("UN002");
		u1.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		u1.setCombatPosition (new MapCoordinates2DEx (14, 7));
		u1.setCombatSide (UnitCombatSideID.ATTACKER);
		u1.setCombatHeading (1);
		u1.setStatus (UnitStatusID.DEAD);
		
		units.add (u1);
		
		// Wrong combat location
		final MemoryUnit u2 = new MemoryUnit ();
		u2.setUnitID ("UN002");
		u2.setCombatLocation (new MapCoordinates3DEx (21, 10, 1));
		u2.setCombatPosition (new MapCoordinates2DEx (14, 7));
		u2.setCombatSide (UnitCombatSideID.ATTACKER);
		u2.setCombatHeading (1);
		u2.setStatus (UnitStatusID.ALIVE);
		
		units.add (u2);
		
		// Wrong combat position
		final MemoryUnit u3 = new MemoryUnit ();
		u3.setUnitID ("UN002");
		u3.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		u3.setCombatPosition (new MapCoordinates2DEx (15, 7));
		u3.setCombatSide (UnitCombatSideID.ATTACKER);
		u3.setCombatHeading (1);
		u3.setStatus (UnitStatusID.ALIVE);
		
		units.add (u3);
		
		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		// Should get a null
		final MapCoordinates3DEx loc = new MapCoordinates3DEx (20, 10, 1);
		final MapCoordinates2DEx pos = new MapCoordinates2DEx (14, 7);
		assertNull (utils.findAliveUnitInCombatAt (units, loc, pos, db, false));
		
		// Add a vortex that matches, may be returned or not depending on last param
		final MemoryUnit u4 = new MemoryUnit ();
		u4.setUnitID ("UN001");
		u4.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		u4.setCombatPosition (new MapCoordinates2DEx (14, 7));
		u4.setCombatSide (UnitCombatSideID.ATTACKER);
		u4.setCombatHeading (1);
		u4.setStatus (UnitStatusID.ALIVE);
		
		units.add (u4);

		assertNull (utils.findAliveUnitInCombatAt (units, loc, pos, db, false));
		assertSame (u4, utils.findAliveUnitInCombatAt (units, loc, pos, db, true));
		
		// Add a real unit that matches, will be returned in preference to the vortex
		final MemoryUnit u5 = new MemoryUnit ();
		u5.setUnitID ("UN002");
		u5.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		u5.setCombatPosition (new MapCoordinates2DEx (14, 7));
		u5.setCombatSide (UnitCombatSideID.ATTACKER);
		u5.setCombatHeading (1);
		u5.setStatus (UnitStatusID.ALIVE);
		
		units.add (u5);
		
		assertSame (u5, utils.findAliveUnitInCombatAt (units, loc, pos, db, false));
		assertSame (u5, utils.findAliveUnitInCombatAt (units, loc, pos, db, true));
	}
	
	/**
	 * Tests the findAliveUnitInCombatWeCanSeeAt method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFindAliveUnitInCombatWeCanSeeAt () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getUnitsThatMoveThroughOtherUnits ()).thenReturn (Arrays.asList ("UN001"));
		
		// Put into a list units that meet every criteria except one
		final FogOfWarMemory mem = new FogOfWarMemory ();
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();

		final ExpandUnitDetails expandUnitDetails = mock (ExpandUnitDetails.class);
		final UnitVisibilityUtils unitVisibilityUtils = mock (UnitVisibilityUtils.class);
		
		// Unit is dead
		final MemoryUnit u1 = new MemoryUnit ();
		u1.setUnitID ("UN002");
		u1.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		u1.setCombatPosition (new MapCoordinates2DEx (14, 7));
		u1.setCombatSide (UnitCombatSideID.ATTACKER);
		u1.setCombatHeading (1);
		u1.setStatus (UnitStatusID.DEAD);
		
		mem.getUnit ().add (u1);
		
		// Wrong combat location
		final MemoryUnit u2 = new MemoryUnit ();
		u2.setUnitID ("UN002");
		u2.setCombatLocation (new MapCoordinates3DEx (21, 10, 1));
		u2.setCombatPosition (new MapCoordinates2DEx (14, 7));
		u2.setCombatSide (UnitCombatSideID.ATTACKER);
		u2.setCombatHeading (1);
		u2.setStatus (UnitStatusID.ALIVE);
		
		mem.getUnit ().add (u2);
		
		// Wrong combat position
		final MemoryUnit u3 = new MemoryUnit ();
		u3.setUnitID ("UN002");
		u3.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		u3.setCombatPosition (new MapCoordinates2DEx (15, 7));
		u3.setCombatSide (UnitCombatSideID.ATTACKER);
		u3.setCombatHeading (1);
		u3.setStatus (UnitStatusID.ALIVE);
		
		mem.getUnit ().add (u3);
		
		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		utils.setExpandUnitDetails (expandUnitDetails);
		utils.setUnitVisibilityUtils (unitVisibilityUtils);
		
		// Should get a null
		final MapCoordinates3DEx loc = new MapCoordinates3DEx (20, 10, 1);
		final MapCoordinates2DEx pos = new MapCoordinates2DEx (14, 7);
		assertNull (utils.findAliveUnitInCombatWeCanSeeAt (loc, pos, 4, players, mem, db, sys, false));
		
		// Unit is invisible
		final MemoryUnit u4 = new MemoryUnit ();
		u4.setUnitID ("UN002");
		u4.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		u4.setCombatPosition (new MapCoordinates2DEx (14, 7));
		u4.setCombatSide (UnitCombatSideID.ATTACKER);
		u4.setCombatHeading (1);
		u4.setStatus (UnitStatusID.ALIVE);
		
		mem.getUnit ().add (u4);

		final ExpandedUnitDetails xu4 = mock (ExpandedUnitDetails.class);
		when (expandUnitDetails.expandUnitDetails (u4, null, null, null, players, mem, db)).thenReturn (xu4);
		when (unitVisibilityUtils.canSeeUnitInCombat (xu4, 4, players, mem, db, sys)).thenReturn (false);
		
		assertNull (utils.findAliveUnitInCombatWeCanSeeAt (loc, pos, 4, players, mem, db, sys, false));
		assertNull (utils.findAliveUnitInCombatWeCanSeeAt (loc, pos, 4, players, mem, db, sys, true));
		
		// Vortex is invisible
		final MemoryUnit u5 = new MemoryUnit ();
		u5.setUnitID ("UN001");
		u5.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		u5.setCombatPosition (new MapCoordinates2DEx (14, 7));
		u5.setCombatSide (UnitCombatSideID.ATTACKER);
		u5.setCombatHeading (1);
		u5.setStatus (UnitStatusID.ALIVE);
		
		mem.getUnit ().add (u5);
		
		final ExpandedUnitDetails xu5 = mock (ExpandedUnitDetails.class);
		when (expandUnitDetails.expandUnitDetails (u5, null, null, null, players, mem, db)).thenReturn (xu5);
		when (unitVisibilityUtils.canSeeUnitInCombat (xu5, 4, players, mem, db, sys)).thenReturn (false);

		assertNull (utils.findAliveUnitInCombatWeCanSeeAt (loc, pos, 4, players, mem, db, sys, false));
		assertNull (utils.findAliveUnitInCombatWeCanSeeAt (loc, pos, 4, players, mem, db, sys, true));
		
		// Add a vortex that matches, may be returned or not depending on last param
		final MemoryUnit u6 = new MemoryUnit ();
		u6.setUnitID ("UN001");
		u6.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		u6.setCombatPosition (new MapCoordinates2DEx (14, 7));
		u6.setCombatSide (UnitCombatSideID.ATTACKER);
		u6.setCombatHeading (1);
		u6.setStatus (UnitStatusID.ALIVE);
		
		mem.getUnit ().add (u6);
		
		final ExpandedUnitDetails xu6 = mock (ExpandedUnitDetails.class);
		when (expandUnitDetails.expandUnitDetails (u6, null, null, null, players, mem, db)).thenReturn (xu6);
		when (unitVisibilityUtils.canSeeUnitInCombat (xu6, 4, players, mem, db, sys)).thenReturn (true);

		assertNull (utils.findAliveUnitInCombatWeCanSeeAt (loc, pos, 4, players, mem, db, sys, false));
		assertSame (xu6, utils.findAliveUnitInCombatWeCanSeeAt (loc, pos, 4, players, mem, db, sys, true));
		
		// Add a real unit that matches, will be returned in preference to the vortex
		final MemoryUnit u7 = new MemoryUnit ();
		u7.setUnitID ("UN002");
		u7.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		u7.setCombatPosition (new MapCoordinates2DEx (14, 7));
		u7.setCombatSide (UnitCombatSideID.ATTACKER);
		u7.setCombatHeading (1);
		u7.setStatus (UnitStatusID.ALIVE);
		
		mem.getUnit ().add (u7);

		final ExpandedUnitDetails xu7 = mock (ExpandedUnitDetails.class);
		when (expandUnitDetails.expandUnitDetails (u7, null, null, null, players, mem, db)).thenReturn (xu7);
		when (unitVisibilityUtils.canSeeUnitInCombat (xu7, 4, players, mem, db, sys)).thenReturn (true);
		
		assertSame (xu7, utils.findAliveUnitInCombatWeCanSeeAt (loc, pos, 4, players, mem, db, sys, false));
		assertSame (xu7, utils.findAliveUnitInCombatWeCanSeeAt (loc, pos, 4, players, mem, db, sys, true));
	}

	/**
	 * Tests the copyUnitValues method including movement fields
	 */
	@Test
	public final void testCopyUnitValues_includeMovementFields ()
	{
		// Source object
		final MemoryUnit source = new MemoryUnit ();
		source.setDoubleOverlandMovesLeft (1);
		source.setDoubleCombatMovesLeft (2);
		source.setSpecialOrder (UnitSpecialOrder.BUILD_ROAD);
		
		// AvailableUnit fields
		source.setOwningPlayerID (3);
		source.setUnitID ("UN001");
		source.setWeaponGrade (4);
		source.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));

		// AvailableUnit - skills list
		final UnitSkillAndValue srcSkill = new UnitSkillAndValue ();
		srcSkill.setUnitSkillID ("US001");
		srcSkill.setUnitSkillValue (5);
		source.getUnitHasSkill ().add (srcSkill);

		// MemoryUnit fields
		source.setUnitURN (6);
		source.setHeroNameID ("HN01");
		source.setUnitName ("Bob");
		source.setAmmoRemaining (7);
		
		source.setManaRemaining (8);
		source.setStatus (UnitStatusID.ALIVE);
		source.setWasSummonedInCombat (true);
		source.setCombatHeading (9);
		source.setCombatSide (UnitCombatSideID.ATTACKER);
		source.setConfusionEffect (ConfusionEffect.OWNER_CONTROLLED);

		source.getFixedSpellsRemaining ().add (10);
		source.getHeroItemSpellChargesRemaining ().add (11);
		
		source.setCombatLocation (new MapCoordinates3DEx (21, 10, 1));
		source.setCombatPosition (new MapCoordinates2DEx (5, 7));
		
		// MemoryUnit - hero item slots list
		final NumberedHeroItem srcItem = new NumberedHeroItem ();
		srcItem.setHeroItemURN (12);
		srcItem.setHeroItemName ("Sword");
		srcItem.setHeroItemTypeID ("IT01");
		srcItem.setHeroItemImageNumber (13);
		srcItem.setSpellID ("SP001");
		srcItem.setSpellChargeCount (14);
		
		srcItem.getHeroItemChosenBonus ().add ("IB01");
		
		final MemoryUnitHeroItemSlot srcItemSlot = new MemoryUnitHeroItemSlot ();
		srcItemSlot.setHeroItem (srcItem);
		
		source.getHeroItemSlot ().add (srcItemSlot);
		
		// Memory unit - damage
		final UnitDamage srcDamage = new UnitDamage ();
		srcDamage.setDamageType (StoredDamageTypeID.PERMANENT);
		srcDamage.setDamageTaken (15);
		source.getUnitDamage ().add (srcDamage);
		
		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		// Call method
		final MemoryUnit dest = new MemoryUnit ();
		utils.copyUnitValues (source, dest, true);
		
		// Check results
		assertEquals (1, dest.getDoubleOverlandMovesLeft ());
		assertEquals (2, dest.getDoubleCombatMovesLeft ());
		assertEquals (UnitSpecialOrder.BUILD_ROAD, dest.getSpecialOrder ());
		
		// AvailableUnit fields
		assertEquals (3, dest.getOwningPlayerID ());
		assertEquals ("UN001", dest.getUnitID ());
		assertEquals (4, dest.getWeaponGrade ());
		assertEquals (new MapCoordinates3DEx (20, 10, 1), dest.getUnitLocation ());
		assertNotSame (source.getUnitLocation (), dest.getUnitLocation ());

		// AvailableUnit - skills list
		assertEquals (1, dest.getUnitHasSkill ().size ());
		assertEquals ("US001", dest.getUnitHasSkill ().get (0).getUnitSkillID ());
		assertEquals (5, dest.getUnitHasSkill ().get (0).getUnitSkillValue ());
		assertNotSame (source.getUnitHasSkill ().get (0), dest.getUnitHasSkill ().get (0));

		// MemoryUnit fields
		assertEquals (6, dest.getUnitURN ());
		assertEquals ("HN01", dest.getHeroNameID ());
		assertEquals ("Bob", dest.getUnitName ());
		assertEquals (7, dest.getAmmoRemaining ());
		
		assertEquals (8, dest.getManaRemaining ());
		assertEquals (UnitStatusID.ALIVE, dest.getStatus ());
		assertTrue (dest.isWasSummonedInCombat ());
		assertEquals (9, dest.getCombatHeading ());
		assertEquals (UnitCombatSideID.ATTACKER, dest.getCombatSide ());
		assertEquals (ConfusionEffect.OWNER_CONTROLLED, dest.getConfusionEffect ());

		assertEquals (1, dest.getFixedSpellsRemaining ().size ());
		assertEquals (10, dest.getFixedSpellsRemaining ().get (0));
		assertEquals (1, dest.getHeroItemSpellChargesRemaining ().size ());
		assertEquals (11, dest.getHeroItemSpellChargesRemaining ().get (0));
		
		assertEquals (new MapCoordinates3DEx (21, 10, 1), dest.getCombatLocation ());
		assertNotSame (source.getCombatLocation (), dest.getCombatLocation ());
		
		assertEquals (new MapCoordinates2DEx (5, 7), dest.getCombatPosition ());
		assertNotSame (source.getCombatPosition (), dest.getCombatPosition ());
		
		// MemoryUnit - hero item slots list
		assertEquals (1, dest.getHeroItemSlot ().size ());
		assertEquals (12, dest.getHeroItemSlot ().get (0).getHeroItem ().getHeroItemURN ());
		assertEquals ("Sword", dest.getHeroItemSlot ().get (0).getHeroItem ().getHeroItemName ());
		assertEquals ("IT01", dest.getHeroItemSlot ().get (0).getHeroItem ().getHeroItemTypeID ());
		assertEquals (13, dest.getHeroItemSlot ().get (0).getHeroItem ().getHeroItemImageNumber ());
		assertEquals ("SP001", dest.getHeroItemSlot ().get (0).getHeroItem ().getSpellID ());
		assertEquals (14, dest.getHeroItemSlot ().get (0).getHeroItem ().getSpellChargeCount ());
		
		assertEquals (1, dest.getHeroItemSlot ().get (0).getHeroItem ().getHeroItemChosenBonus ().size ());
		assertEquals ("IB01", dest.getHeroItemSlot ().get (0).getHeroItem ().getHeroItemChosenBonus ().get (0));
		
		assertNotSame (source.getHeroItemSlot ().get (0), dest.getHeroItemSlot ().get (0));
		assertNotSame (source.getHeroItemSlot ().get (0).getHeroItem (), dest.getHeroItemSlot ().get (0).getHeroItem ());
		
		// Memory unit - damage
		assertEquals (1, dest.getUnitDamage ().size ());
		assertEquals (StoredDamageTypeID.PERMANENT, dest.getUnitDamage ().get (0).getDamageType ());
		assertEquals (15, dest.getUnitDamage ().get (0).getDamageTaken ());
		assertNotSame (source.getUnitDamage ().get (0), dest.getUnitDamage ().get (0));
	}
	
	/**
	 * Tests the copyUnitValues method excluding movement fields
	 */
	@Test
	public final void testCopyUnitValues_excludeMovementFields ()
	{
		// Source object
		final MemoryUnit source = new MemoryUnit ();
		source.setDoubleOverlandMovesLeft (1);
		source.setDoubleCombatMovesLeft (2);
		source.setSpecialOrder (UnitSpecialOrder.BUILD_ROAD);
		
		// AvailableUnit fields
		source.setOwningPlayerID (3);
		source.setUnitID ("UN001");
		source.setWeaponGrade (4);
		source.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));

		// AvailableUnit - skills list
		final UnitSkillAndValue srcSkill = new UnitSkillAndValue ();
		srcSkill.setUnitSkillID ("US001");
		srcSkill.setUnitSkillValue (5);
		source.getUnitHasSkill ().add (srcSkill);

		// MemoryUnit fields
		source.setUnitURN (6);
		source.setHeroNameID ("HN01");
		source.setUnitName ("Bob");
		source.setAmmoRemaining (7);
		
		source.setManaRemaining (8);
		source.setStatus (UnitStatusID.ALIVE);
		source.setWasSummonedInCombat (true);
		source.setCombatHeading (9);
		source.setCombatSide (UnitCombatSideID.ATTACKER);
		source.setConfusionEffect (ConfusionEffect.OWNER_CONTROLLED);

		source.getFixedSpellsRemaining ().add (10);
		source.getHeroItemSpellChargesRemaining ().add (11);
		
		source.setCombatLocation (new MapCoordinates3DEx (21, 10, 1));
		source.setCombatPosition (new MapCoordinates2DEx (5, 7));
		
		// MemoryUnit - hero item slots list
		final NumberedHeroItem srcItem = new NumberedHeroItem ();
		srcItem.setHeroItemURN (12);
		srcItem.setHeroItemName ("Sword");
		srcItem.setHeroItemTypeID ("IT01");
		srcItem.setHeroItemImageNumber (13);
		srcItem.setSpellID ("SP001");
		srcItem.setSpellChargeCount (14);
		
		srcItem.getHeroItemChosenBonus ().add ("IB01");
		
		final MemoryUnitHeroItemSlot srcItemSlot = new MemoryUnitHeroItemSlot ();
		srcItemSlot.setHeroItem (srcItem);
		
		source.getHeroItemSlot ().add (srcItemSlot);
		
		// Memory unit - damage
		final UnitDamage srcDamage = new UnitDamage ();
		srcDamage.setDamageType (StoredDamageTypeID.PERMANENT);
		srcDamage.setDamageTaken (15);
		source.getUnitDamage ().add (srcDamage);
		
		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		// Call method
		final MemoryUnit dest = new MemoryUnit ();
		utils.copyUnitValues (source, dest, false);
		
		// Check results
		assertEquals (0, dest.getDoubleOverlandMovesLeft ());
		assertNull (dest.getDoubleCombatMovesLeft ());
		assertNull (dest.getSpecialOrder ());
		
		// AvailableUnit fields
		assertEquals (3, dest.getOwningPlayerID ());
		assertEquals ("UN001", dest.getUnitID ());
		assertEquals (4, dest.getWeaponGrade ());
		assertEquals (new MapCoordinates3DEx (20, 10, 1), dest.getUnitLocation ());
		assertNotSame (source.getUnitLocation (), dest.getUnitLocation ());

		// AvailableUnit - skills list
		assertEquals (1, dest.getUnitHasSkill ().size ());
		assertEquals ("US001", dest.getUnitHasSkill ().get (0).getUnitSkillID ());
		assertEquals (5, dest.getUnitHasSkill ().get (0).getUnitSkillValue ());
		assertNotSame (source.getUnitHasSkill ().get (0), dest.getUnitHasSkill ().get (0));

		// MemoryUnit fields
		assertEquals (6, dest.getUnitURN ());
		assertEquals ("HN01", dest.getHeroNameID ());
		assertEquals ("Bob", dest.getUnitName ());
		assertEquals (7, dest.getAmmoRemaining ());
		
		assertEquals (8, dest.getManaRemaining ());
		assertEquals (UnitStatusID.ALIVE, dest.getStatus ());
		assertTrue (dest.isWasSummonedInCombat ());
		assertEquals (9, dest.getCombatHeading ());
		assertEquals (UnitCombatSideID.ATTACKER, dest.getCombatSide ());
		assertEquals (ConfusionEffect.OWNER_CONTROLLED, dest.getConfusionEffect ());

		assertEquals (1, dest.getFixedSpellsRemaining ().size ());
		assertEquals (10, dest.getFixedSpellsRemaining ().get (0));
		assertEquals (1, dest.getHeroItemSpellChargesRemaining ().size ());
		assertEquals (11, dest.getHeroItemSpellChargesRemaining ().get (0));
		
		assertEquals (new MapCoordinates3DEx (21, 10, 1), dest.getCombatLocation ());
		assertNotSame (source.getCombatLocation (), dest.getCombatLocation ());
		
		assertEquals (new MapCoordinates2DEx (5, 7), dest.getCombatPosition ());
		assertNotSame (source.getCombatPosition (), dest.getCombatPosition ());
		
		// MemoryUnit - hero item slots list
		assertEquals (1, dest.getHeroItemSlot ().size ());
		assertEquals (12, dest.getHeroItemSlot ().get (0).getHeroItem ().getHeroItemURN ());
		assertEquals ("Sword", dest.getHeroItemSlot ().get (0).getHeroItem ().getHeroItemName ());
		assertEquals ("IT01", dest.getHeroItemSlot ().get (0).getHeroItem ().getHeroItemTypeID ());
		assertEquals (13, dest.getHeroItemSlot ().get (0).getHeroItem ().getHeroItemImageNumber ());
		assertEquals ("SP001", dest.getHeroItemSlot ().get (0).getHeroItem ().getSpellID ());
		assertEquals (14, dest.getHeroItemSlot ().get (0).getHeroItem ().getSpellChargeCount ());
		
		assertEquals (1, dest.getHeroItemSlot ().get (0).getHeroItem ().getHeroItemChosenBonus ().size ());
		assertEquals ("IB01", dest.getHeroItemSlot ().get (0).getHeroItem ().getHeroItemChosenBonus ().get (0));
		
		assertNotSame (source.getHeroItemSlot ().get (0), dest.getHeroItemSlot ().get (0));
		assertNotSame (source.getHeroItemSlot ().get (0).getHeroItem (), dest.getHeroItemSlot ().get (0).getHeroItem ());
		
		// Memory unit - damage
		assertEquals (1, dest.getUnitDamage ().size ());
		assertEquals (StoredDamageTypeID.PERMANENT, dest.getUnitDamage ().get (0).getDamageType ());
		assertEquals (15, dest.getUnitDamage ().get (0).getDamageTaken ());
		assertNotSame (source.getUnitDamage ().get (0), dest.getUnitDamage ().get (0));
	}
	
	/**
	 * Tests the copyUnitValues method with all field values left as null
	 */
	@Test
	public final void testCopyUnitValues_nulls ()
	{
		// Source object
		final MemoryUnit source = new MemoryUnit ();
		
		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		// Call method
		final MemoryUnit dest = new MemoryUnit ();
		utils.copyUnitValues (source, dest, false);
		
		// Check results
		assertEquals (0, dest.getDoubleOverlandMovesLeft ());
		assertNull (dest.getDoubleCombatMovesLeft ());
		assertNull (dest.getSpecialOrder ());
		
		// AvailableUnit fields
		assertEquals (0, dest.getOwningPlayerID ());
		assertNull (dest.getUnitID ());
		assertNull (dest.getWeaponGrade ());
		assertNull (dest.getUnitLocation ());

		// AvailableUnit - skills list
		assertEquals (0, dest.getUnitHasSkill ().size ());

		// MemoryUnit fields
		assertEquals (0, dest.getUnitURN ());
		assertNull (dest.getHeroNameID ());
		assertNull (dest.getUnitName ());
		assertEquals (0, dest.getAmmoRemaining ());
		
		assertEquals (0, dest.getManaRemaining ());
		assertNull (dest.getStatus ());
		assertFalse (dest.isWasSummonedInCombat ());
		assertNull (dest.getCombatHeading ());
		assertNull (dest.getCombatSide ());
		assertNull (dest.getConfusionEffect ());

		assertEquals (0, dest.getFixedSpellsRemaining ().size ());
		assertEquals (0, dest.getHeroItemSpellChargesRemaining ().size ());
		
		assertNull (dest.getCombatLocation ());
		assertNull (dest.getCombatPosition ());
		
		// MemoryUnit - hero item slots list
		assertEquals (0, dest.getHeroItemSlot ().size ());
		
		// Memory unit - damage
		assertEquals (0, dest.getUnitDamage ().size ());
	}
	
	/**
	 * Tests the getTotalDamageTaken method
	 */
	@Test
	public final void testGetTotalDamageTaken ()
	{
		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();

		// Try empty list
		final List<UnitDamage> damages = new ArrayList<UnitDamage> ();
		assertEquals (0, utils.getTotalDamageTaken (damages));
		
		// Try real example
		int dmg = 1;
		for (final StoredDamageTypeID dmgType : StoredDamageTypeID.values ())
		{
			dmg++;
			
			final UnitDamage unitDamage = new UnitDamage ();
			unitDamage.setDamageTaken (dmg);
			unitDamage.setDamageType (dmgType);
			damages.add (unitDamage);
		}

		assertEquals (2+3+4, utils.getTotalDamageTaken (damages));
	}

	/**
	 * Tests the getHealableDamageTaken method
	 */
	@Test
	public final void testGetHealableDamageTaken ()
	{
		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();

		// Try empty list
		final List<UnitDamage> damages = new ArrayList<UnitDamage> ();
		assertEquals (0, utils.getHealableDamageTaken (damages));
		
		// Try real example
		int dmg = 1;
		for (final StoredDamageTypeID dmgType : StoredDamageTypeID.values ())
		{
			dmg++;
			
			final UnitDamage unitDamage = new UnitDamage ();
			unitDamage.setDamageTaken (dmg);
			unitDamage.setDamageType (dmgType);
			damages.add (unitDamage);
		}

		assertEquals (2+3, utils.getHealableDamageTaken (damages));		// Permanent (4) is the last component and gets excluded
	}
	
	/**
	 * Tests the isUnitImmuneToSpellEffects method when there's no spell effects listed
	 * (In the actual place this method is used, this would've already been caught by a previous check, so this scenario is a bit dubious)
	 * 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitImmuneToSpellEffects_emptyList () throws Exception
	{
		// Possible effects
		final List<UnitSpellEffect> effects = new ArrayList<UnitSpellEffect> ();
		
		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		// Run method
		assertTrue (utils.isUnitImmuneToSpellEffects (null, effects, null));
	}

	/**
	 * Tests the isUnitImmuneToSpellEffects method when the unit is immune to all spell effects
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitImmuneToSpellEffects_immune () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final NegatedBySkill negated = new NegatedBySkill ();
		negated.setNegatedBySkillID ("US002");
		negated.setNegatedByUnitID (NegatedByUnitID.OUR_UNIT);
		
		final UnitSkillEx skillDef = new UnitSkillEx ();
		skillDef.getNegatedBySkill ().add (negated);
		when (db.findUnitSkill ("US001", "isUnitImmuneToSpellEffects")).thenReturn (skillDef);
		
		// Possible effects
		final List<UnitSpellEffect> effects = new ArrayList<UnitSpellEffect> ();
		
		final UnitSpellEffect effect = new UnitSpellEffect ();
		effect.setUnitSkillID ("US001");
		effects.add (effect);
		
		// Unit being tested
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.hasModifiedSkill ("US002")).thenReturn (true);
		
		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		// Run method
		assertTrue (utils.isUnitImmuneToSpellEffects (xu, effects, db));
	}

	/**
	 * Tests the isUnitImmuneToSpellEffects method when the unit doesn't have the skill to make itself immune
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitImmuneToSpellEffects_dontHaveSkill () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final NegatedBySkill negated = new NegatedBySkill ();
		negated.setNegatedBySkillID ("US002");
		negated.setNegatedByUnitID (NegatedByUnitID.OUR_UNIT);
		
		final UnitSkillEx skillDef = new UnitSkillEx ();
		skillDef.getNegatedBySkill ().add (negated);
		when (db.findUnitSkill ("US001", "isUnitImmuneToSpellEffects")).thenReturn (skillDef);
		
		// Possible effects
		final List<UnitSpellEffect> effects = new ArrayList<UnitSpellEffect> ();
		
		final UnitSpellEffect effect = new UnitSpellEffect ();
		effect.setUnitSkillID ("US001");
		effects.add (effect);
		
		// Unit being tested
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.hasModifiedSkill ("US002")).thenReturn (false);
		
		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		// Run method
		assertFalse (utils.isUnitImmuneToSpellEffects (xu, effects, db));
	}

	/**
	 * Tests the isUnitImmuneToSpellEffects method when the negation comes from the enemy unit instead
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitImmuneToSpellEffects_negaedByEnemy () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final NegatedBySkill negated = new NegatedBySkill ();
		negated.setNegatedBySkillID ("US002");
		negated.setNegatedByUnitID (NegatedByUnitID.ENEMY_UNIT);
		
		final UnitSkillEx skillDef = new UnitSkillEx ();
		skillDef.getNegatedBySkill ().add (negated);
		when (db.findUnitSkill ("US001", "isUnitImmuneToSpellEffects")).thenReturn (skillDef);
		
		// Possible effects
		final List<UnitSpellEffect> effects = new ArrayList<UnitSpellEffect> ();
		
		final UnitSpellEffect effect = new UnitSpellEffect ();
		effect.setUnitSkillID ("US001");
		effects.add (effect);
		
		// Unit being tested
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		
		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		// Run method
		assertFalse (utils.isUnitImmuneToSpellEffects (xu, effects, db));
	}
	
	/**
	 * Tests the isExperienceBonusAndWeAlreadyHaveTooMuch method when there's no spell effects listed
	 * (In the actual place this method is used, this would've already been caught by a previous check, so this scenario is a bit dubious)
	 * 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsExperienceBonusAndWeAlreadyHaveTooMuch_emptyList () throws Exception
	{
		// Possible effects
		final List<UnitSpellEffect> effects = new ArrayList<UnitSpellEffect> ();

		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		// Run method
		assertFalse (utils.isExperienceBonusAndWeAlreadyHaveTooMuch (null, effects, null));
	}

	/**
	 * Tests the isExperienceBonusAndWeAlreadyHaveTooMuch method when the spell effect boosts some other stat other than experience
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsExperienceBonusAndWeAlreadyHaveTooMuch_notExperience () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final AddsToSkill boost = new AddsToSkill ();
		boost.setAddsToSkillID ("US002");
		boost.setAddsToSkillValue (100);

		final UnitSkillEx skillDef = new UnitSkillEx ();
		skillDef.getAddsToSkill ().add (boost);
		when (db.findUnitSkill ("US001", "isExperienceBonusAndWeAlreadyHaveTooMuch")).thenReturn (skillDef);
		
		// Possible effects
		final List<UnitSpellEffect> effects = new ArrayList<UnitSpellEffect> ();

		final UnitSpellEffect effect = new UnitSpellEffect ();
		effect.setUnitSkillID ("US001");
		effects.add (effect);

		// Unit being tested
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		
		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		// Run method
		assertFalse (utils.isExperienceBonusAndWeAlreadyHaveTooMuch (xu, effects, db));
	}

	/**
	 * Tests the isExperienceBonusAndWeAlreadyHaveTooMuch method when we do already have too much experience
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsExperienceBonusAndWeAlreadyHaveTooMuch_tooMuchExperience () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final AddsToSkill boost = new AddsToSkill ();
		boost.setAddsToSkillID (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE);
		boost.setAddsToSkillValue (100);

		final UnitSkillEx skillDef = new UnitSkillEx ();
		skillDef.getAddsToSkill ().add (boost);
		when (db.findUnitSkill ("US001", "isExperienceBonusAndWeAlreadyHaveTooMuch")).thenReturn (skillDef);
		
		// Possible effects
		final List<UnitSpellEffect> effects = new ArrayList<UnitSpellEffect> ();

		final UnitSpellEffect effect = new UnitSpellEffect ();
		effect.setUnitSkillID ("US001");
		effects.add (effect);

		// Unit being tested
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);		
		when (xu.hasBasicSkill (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE)).thenReturn (true);
		when (xu.getBasicSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE)).thenReturn (101);
		
		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		// Run method
		assertTrue (utils.isExperienceBonusAndWeAlreadyHaveTooMuch (xu, effects, db));
	}

	/**
	 * Tests the isExperienceBonusAndWeAlreadyHaveTooMuch method when we do have experience, but not enough
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsExperienceBonusAndWeAlreadyHaveTooMuch_notEnoughExperience () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final AddsToSkill boost = new AddsToSkill ();
		boost.setAddsToSkillID (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE);
		boost.setAddsToSkillValue (100);

		final UnitSkillEx skillDef = new UnitSkillEx ();
		skillDef.getAddsToSkill ().add (boost);
		when (db.findUnitSkill ("US001", "isExperienceBonusAndWeAlreadyHaveTooMuch")).thenReturn (skillDef);
		
		// Possible effects
		final List<UnitSpellEffect> effects = new ArrayList<UnitSpellEffect> ();

		final UnitSpellEffect effect = new UnitSpellEffect ();
		effect.setUnitSkillID ("US001");
		effects.add (effect);

		// Unit being tested
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);		
		when (xu.hasBasicSkill (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE)).thenReturn (true);
		when (xu.getBasicSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE)).thenReturn (99);
		
		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		// Run method
		assertFalse (utils.isExperienceBonusAndWeAlreadyHaveTooMuch (xu, effects, db));
	}
	
	/**
	 * Tests the isExperienceBonusAndWeAlreadyHaveTooMuch method when its a summoned unit that cannot have experience
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsExperienceBonusAndWeAlreadyHaveTooMuch_cantHaveExperience () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final AddsToSkill boost = new AddsToSkill ();
		boost.setAddsToSkillID (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE);
		boost.setAddsToSkillValue (100);

		final UnitSkillEx skillDef = new UnitSkillEx ();
		skillDef.getAddsToSkill ().add (boost);
		when (db.findUnitSkill ("US001", "isExperienceBonusAndWeAlreadyHaveTooMuch")).thenReturn (skillDef);
		
		// Possible effects
		final List<UnitSpellEffect> effects = new ArrayList<UnitSpellEffect> ();

		final UnitSpellEffect effect = new UnitSpellEffect ();
		effect.setUnitSkillID ("US001");
		effects.add (effect);

		// Unit being tested
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.hasBasicSkill (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE)).thenReturn (false);
		
		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		// Run method
		assertFalse (utils.isExperienceBonusAndWeAlreadyHaveTooMuch (xu, effects, db));
	}
}