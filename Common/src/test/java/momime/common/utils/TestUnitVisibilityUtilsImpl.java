package momime.common.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.GenerateTestData;
import momime.common.database.NegatedBySkill;
import momime.common.database.NegatedByUnitID;
import momime.common.database.UnitCombatSideID;
import momime.common.database.UnitSkillAndValue;
import momime.common.database.UnitSkillEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MemoryUnitHeroItemSlot;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.UnitStatusID;

/**
 * Tests the UnitVisibilityUtilsImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestUnitVisibilityUtilsImpl
{
	/**
	 * Tests the canSeeUnitInCombat method when its our own unit so we can see it automatically
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanSeeUnitInCombat_ourUnit () throws Exception
	{
		// Unit we're trying to see
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		
		// Set up object to test
		final UnitVisibilityUtilsImpl utils = new UnitVisibilityUtilsImpl ();
		
		// Run method
		assertTrue (utils.canSeeUnitInCombat (xu, 2, null, null, null, null));
	}

	/**
	 * Tests the canSeeUnitInCombat method when its an enemy unit but has no invisibility
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanSeeUnitInCombat_visible () throws Exception
	{
		// Unit we're trying to see
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getOwningPlayerID ()).thenReturn (3);
		when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY)).thenReturn (false);
		when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_SPELL)).thenReturn (false);
		when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_HERO_ITEM)).thenReturn (false);
		
		// Set up object to test
		final UnitVisibilityUtilsImpl utils = new UnitVisibilityUtilsImpl ();
		
		// Run method
		assertTrue (utils.canSeeUnitInCombat (xu, 2, null, null, null, null));
	}

	/**
	 * Tests the canSeeUnitInCombat method when its an enemy unit and its invisible and we have nothing to counter it and no unit next to it
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanSeeUnitInCombat_invisible () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitSkillEx invisibility = new UnitSkillEx ();
		
		for (int n = 1; n <= 2; n++)
		{
			final NegatedBySkill negated = new NegatedBySkill ();
			negated.setNegatedBySkillID ("US00" + n);
			negated.setNegatedByUnitID ((n == 1) ? NegatedByUnitID.ENEMY_UNIT : NegatedByUnitID.OUR_UNIT);
			invisibility.getNegatedBySkill ().add (negated);
		}
		
		when (db.findUnitSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY, "canSeeUnitInCombat")).thenReturn (invisibility);
		
		// Unit we're trying to see
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getOwningPlayerID ()).thenReturn (3);
		when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY)).thenReturn (true);
		when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_SPELL)).thenReturn (false);
		when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_HERO_ITEM)).thenReturn (false);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (3, 7));
		
		// Our units
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final MemoryUnit ourUnit = new MemoryUnit ();
		ourUnit.setOwningPlayerID (2);
		ourUnit.setStatus (UnitStatusID.ALIVE);
		ourUnit.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		ourUnit.setCombatPosition (new MapCoordinates2DEx (5, 7));
		ourUnit.setCombatSide (UnitCombatSideID.ATTACKER);
		ourUnit.setCombatHeading (1);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.getUnit ().add (ourUnit);
		
		final ExpandUnitDetails expandUnitDetails = mock (ExpandUnitDetails.class);
		final ExpandedUnitDetails ourXU = mock (ExpandedUnitDetails.class);
		when (ourXU.hasModifiedSkill ("US001")).thenReturn (false);
		
		when (expandUnitDetails.expandUnitDetails (ourUnit, null, null, null, players, mem, db)).thenReturn (ourXU);
		
		// Distance between units
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();
		
		final CoordinateSystemUtils coordinateSystemUtils = mock (CoordinateSystemUtils.class);
		when (coordinateSystemUtils.determineStep2DDistanceBetween (sys, new MapCoordinates2DEx (3, 7), new MapCoordinates2DEx (5, 7))).thenReturn (2);
		
		// Set up object to test
		final UnitVisibilityUtilsImpl utils = new UnitVisibilityUtilsImpl ();
		utils.setCoordinateSystemUtils (coordinateSystemUtils);
		utils.setExpandUnitDetails (expandUnitDetails);
		
		// Run method
		assertFalse (utils.canSeeUnitInCombat (xu, 2, players, mem, db, sys));
	}

	/**
	 * Tests the canSeeUnitInCombat method when its an enemy unit and its invisible but we have a unit next to it
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanSeeUnitInCombat_adjacent () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitSkillEx invisibility = new UnitSkillEx ();
		
		for (int n = 1; n <= 2; n++)
		{
			final NegatedBySkill negated = new NegatedBySkill ();
			negated.setNegatedBySkillID ("US00" + n);
			negated.setNegatedByUnitID ((n == 1) ? NegatedByUnitID.ENEMY_UNIT : NegatedByUnitID.OUR_UNIT);
			invisibility.getNegatedBySkill ().add (negated);
		}
		
		when (db.findUnitSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY, "canSeeUnitInCombat")).thenReturn (invisibility);
		
		// Unit we're trying to see
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getOwningPlayerID ()).thenReturn (3);
		when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY)).thenReturn (true);
		when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_SPELL)).thenReturn (false);
		when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_HERO_ITEM)).thenReturn (false);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (3, 7));
		
		// Our units
		final MemoryUnit ourUnit = new MemoryUnit ();
		ourUnit.setOwningPlayerID (2);
		ourUnit.setStatus (UnitStatusID.ALIVE);
		ourUnit.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		ourUnit.setCombatPosition (new MapCoordinates2DEx (5, 7));
		ourUnit.setCombatSide (UnitCombatSideID.ATTACKER);
		ourUnit.setCombatHeading (1);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.getUnit ().add (ourUnit);
		
		// Distance between units
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();
		
		final CoordinateSystemUtils coordinateSystemUtils = mock (CoordinateSystemUtils.class);
		when (coordinateSystemUtils.determineStep2DDistanceBetween (sys, new MapCoordinates2DEx (3, 7), new MapCoordinates2DEx (5, 7))).thenReturn (1);
		
		// Set up object to test
		final UnitVisibilityUtilsImpl utils = new UnitVisibilityUtilsImpl ();
		utils.setCoordinateSystemUtils (coordinateSystemUtils);
		
		// Run method
		assertTrue (utils.canSeeUnitInCombat (xu, 2, null, mem, db, sys));
	}

	/**
	 * Tests the canSeeUnitInCombat method when its an enemy unit and its invisible but we have a unit with True Sight
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanSeeUnitInCombat_trueSight () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitSkillEx invisibility = new UnitSkillEx ();
		
		for (int n = 1; n <= 2; n++)
		{
			final NegatedBySkill negated = new NegatedBySkill ();
			negated.setNegatedBySkillID ("US00" + n);
			negated.setNegatedByUnitID ((n == 1) ? NegatedByUnitID.ENEMY_UNIT : NegatedByUnitID.OUR_UNIT);
			invisibility.getNegatedBySkill ().add (negated);
		}
		
		when (db.findUnitSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY, "canSeeUnitInCombat")).thenReturn (invisibility);
		
		// Unit we're trying to see
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getOwningPlayerID ()).thenReturn (3);
		when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY)).thenReturn (true);
		when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_SPELL)).thenReturn (false);
		when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_HERO_ITEM)).thenReturn (false);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (3, 7));
		
		// Our units
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final MemoryUnit ourUnit = new MemoryUnit ();
		ourUnit.setOwningPlayerID (2);
		ourUnit.setStatus (UnitStatusID.ALIVE);
		ourUnit.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		ourUnit.setCombatPosition (new MapCoordinates2DEx (5, 7));
		ourUnit.setCombatSide (UnitCombatSideID.ATTACKER);
		ourUnit.setCombatHeading (1);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.getUnit ().add (ourUnit);
		
		final ExpandUnitDetails expandUnitDetails = mock (ExpandUnitDetails.class);
		final ExpandedUnitDetails ourXU = mock (ExpandedUnitDetails.class);
		when (ourXU.hasModifiedSkill ("US001")).thenReturn (true);
		
		when (expandUnitDetails.expandUnitDetails (ourUnit, null, null, null, players, mem, db)).thenReturn (ourXU);
		
		// Distance between units
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();
		
		final CoordinateSystemUtils coordinateSystemUtils = mock (CoordinateSystemUtils.class);
		when (coordinateSystemUtils.determineStep2DDistanceBetween (sys, new MapCoordinates2DEx (3, 7), new MapCoordinates2DEx (5, 7))).thenReturn (2);
		
		// Set up object to test
		final UnitVisibilityUtilsImpl utils = new UnitVisibilityUtilsImpl ();
		utils.setCoordinateSystemUtils (coordinateSystemUtils);
		utils.setExpandUnitDetails (expandUnitDetails);
		
		// Run method
		assertTrue (utils.canSeeUnitInCombat (xu, 2, players, mem, db, sys));
	}

	/**
	 * Tests the canSeeUnitOverland method when its our own unit so we can see it automatically
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanSeeUnitOverland_ourUnit () throws Exception
	{
		// Unit we're trying to see
		final MemoryUnit mu = new MemoryUnit ();
		mu.setOwningPlayerID (2);
		
		// Set up object to test
		final UnitVisibilityUtilsImpl utils = new UnitVisibilityUtilsImpl ();
		
		// Run method
		assertTrue (utils.canSeeUnitOverland (mu, 2, null, null));
	}

	/**
	 * Tests the canSeeUnitOverland method when its an enemy unit that's naturally invisible
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanSeeUnitOverland_naturalInvisibility () throws Exception
	{
		// Unit we're trying to see
		final MemoryUnit mu = new MemoryUnit ();
		mu.setOwningPlayerID (3);
		
		final UnitSkillAndValue invisibility = new UnitSkillAndValue ();
		invisibility.setUnitSkillID (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY);
		mu.getUnitHasSkill ().add (invisibility);
		
		// Set up object to test
		final UnitVisibilityUtilsImpl utils = new UnitVisibilityUtilsImpl ();
		
		// Run method
		assertFalse (utils.canSeeUnitOverland (mu, 2, null, null));
	}

	/**
	 * Tests the canSeeUnitOverland method when its an enemy unit that has the invisibility spell cast on it
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanSeeUnitOverland_invisibilityFromSpell () throws Exception
	{
		// Unit we're trying to see
		final MemoryUnit mu = new MemoryUnit ();
		mu.setUnitURN (20);
		mu.setOwningPlayerID (3);
		
		// Spells
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		when (memoryMaintainedSpellUtils.findMaintainedSpell (spells,
			null, null, 20, CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_SPELL, null, null)).thenReturn (new MemoryMaintainedSpell ());
		
		// Set up object to test
		final UnitVisibilityUtilsImpl utils = new UnitVisibilityUtilsImpl ();
		utils.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		
		// Run method
		assertFalse (utils.canSeeUnitOverland (mu, 2, spells, null));
	}

	/**
	 * Tests the canSeeUnitOverland method when its an enemy hero with an item that grants invisibility
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanSeeUnitOverland_invisibilityFromHeroItem () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getInvisibilityHeroItemBonusID ()).thenReturn (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_HERO_ITEM);
		
		// Unit we're trying to see
		final MemoryUnit mu = new MemoryUnit ();
		mu.setUnitURN (20);
		mu.setOwningPlayerID (3);
		
		final NumberedHeroItem item = new NumberedHeroItem ();
		item.getHeroItemChosenBonus ().add (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_HERO_ITEM);
		
		final MemoryUnitHeroItemSlot itemSlot = new MemoryUnitHeroItemSlot ();
		itemSlot.setHeroItem (item);
		mu.getHeroItemSlot ().add (itemSlot);
		
		// Spells
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		when (memoryMaintainedSpellUtils.findMaintainedSpell (spells,
			null, null, 20, CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_SPELL, null, null)).thenReturn (null);
		
		// Set up object to test
		final UnitVisibilityUtilsImpl utils = new UnitVisibilityUtilsImpl ();
		utils.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		
		// Run method
		assertFalse (utils.canSeeUnitOverland (mu, 2, spells, db));
	}
}