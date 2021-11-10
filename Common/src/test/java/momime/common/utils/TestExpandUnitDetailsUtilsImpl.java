package momime.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.AddsToSkill;
import momime.common.database.CombatAreaEffect;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.database.HeroItemBonus;
import momime.common.database.HeroItemBonusStat;
import momime.common.database.HeroItemType;
import momime.common.database.Pick;
import momime.common.database.Spell;
import momime.common.database.UnitSkillAndValue;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillEx;
import momime.common.database.UnitSpellEffect;
import momime.common.database.UnitType;
import momime.common.database.WeaponGrade;
import momime.common.messages.AvailableUnit;
import momime.common.messages.ConfusionEffect;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MemoryUnitHeroItemSlot;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.UnitStatusID;

/**
 * Tests the ExpandUnitDetailsUtilsImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestExpandUnitDetailsUtilsImpl
{
	/**
	 * Tests the buildUnitStackMinimalDetails method where the unit we are calculating for is an AvailableUnit so cannot be in the initially generated list
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testBuildUnitStackMinimalDetails_AvailableUnit () throws Exception
	{
		// These are just for mocks and the unit list
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final FogOfWarMemory mem = new FogOfWarMemory ();
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Unit we are calculating stats for
		final UnitDetailsUtils unitDetailsUtils = mock (UnitDetailsUtils.class);

		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		unit.setOwningPlayerID (1);
		
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		when (unitDetailsUtils.expandMinimalUnitDetails (unit, players, mem, db)).thenReturn (mu);
		
		// Other units
		final MemoryUnit unit1 = new MemoryUnit ();
		unit1.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		unit1.setOwningPlayerID (1);
		unit1.setStatus (UnitStatusID.ALIVE);
		mem.getUnit ().add (unit1);

		final MinimalUnitDetails mu1 = mock (MinimalUnitDetails.class);
		when (unitDetailsUtils.expandMinimalUnitDetails (unit1, players, mem, db)).thenReturn (mu1);

		final MemoryUnit wrongLocation = new MemoryUnit ();
		wrongLocation.setUnitLocation (new MapCoordinates3DEx (21, 10, 1));
		wrongLocation.setOwningPlayerID (1);
		wrongLocation.setStatus (UnitStatusID.ALIVE);
		mem.getUnit ().add (wrongLocation);

		final MemoryUnit wrongPlayer = new MemoryUnit ();
		wrongPlayer.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		wrongPlayer.setOwningPlayerID (2);
		wrongPlayer.setStatus (UnitStatusID.ALIVE);
		mem.getUnit ().add (wrongPlayer);
		
		final MemoryUnit deadUnit = new MemoryUnit ();
		deadUnit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		deadUnit.setOwningPlayerID (1);
		deadUnit.setStatus (UnitStatusID.DEAD);
		mem.getUnit ().add (deadUnit);
		
		final MemoryUnit inCombat = new MemoryUnit ();
		inCombat.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		inCombat.setOwningPlayerID (1);
		inCombat.setStatus (UnitStatusID.ALIVE);
		inCombat.setCombatLocation (new MapCoordinates3DEx (19, 10, 1));
		mem.getUnit ().add (inCombat);
		
		final MemoryUnit unit2 = new MemoryUnit ();
		unit2.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		unit2.setOwningPlayerID (1);
		unit2.setStatus (UnitStatusID.ALIVE);
		mem.getUnit ().add (unit2);

		final MinimalUnitDetails mu2 = mock (MinimalUnitDetails.class);
		when (unitDetailsUtils.expandMinimalUnitDetails (unit2, players, mem, db)).thenReturn (mu2);
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		utils.setUnitDetailsUtils (unitDetailsUtils);
		
		// Run method
		final List<MinimalUnitDetails> stack = utils.buildUnitStackMinimalDetails (unit, players, mem, db);
		
		// Check results
		assertEquals (3, stack.size ());
		assertSame (mu1, stack.get (0));
		assertSame (mu2, stack.get (1));
		assertSame (mu, stack.get (2));
	}

	/**
	 * Tests the buildUnitStackMinimalDetails method where the unit we are calculating for is a MemoryUnit so is already in the list
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testBuildUnitStackMinimalDetails_MemoryUnit () throws Exception
	{
		// These are just for mocks and the unit list
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final FogOfWarMemory mem = new FogOfWarMemory ();
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Unit we are calculating stats for
		final UnitDetailsUtils unitDetailsUtils = mock (UnitDetailsUtils.class);

		// Other units
		final MemoryUnit unit1 = new MemoryUnit ();
		unit1.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		unit1.setOwningPlayerID (1);
		unit1.setStatus (UnitStatusID.ALIVE);
		mem.getUnit ().add (unit1);

		final MinimalUnitDetails mu1 = mock (MinimalUnitDetails.class);
		when (unitDetailsUtils.expandMinimalUnitDetails (unit1, players, mem, db)).thenReturn (mu1);

		final MemoryUnit wrongLocation = new MemoryUnit ();
		wrongLocation.setUnitLocation (new MapCoordinates3DEx (21, 10, 1));
		wrongLocation.setOwningPlayerID (1);
		wrongLocation.setStatus (UnitStatusID.ALIVE);
		mem.getUnit ().add (wrongLocation);

		final MemoryUnit wrongPlayer = new MemoryUnit ();
		wrongPlayer.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		wrongPlayer.setOwningPlayerID (2);
		wrongPlayer.setStatus (UnitStatusID.ALIVE);
		mem.getUnit ().add (wrongPlayer);
		
		final MemoryUnit deadUnit = new MemoryUnit ();
		deadUnit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		deadUnit.setOwningPlayerID (1);
		deadUnit.setStatus (UnitStatusID.DEAD);
		mem.getUnit ().add (deadUnit);
		
		final MemoryUnit inCombat = new MemoryUnit ();
		inCombat.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		inCombat.setOwningPlayerID (1);
		inCombat.setStatus (UnitStatusID.ALIVE);
		inCombat.setCombatLocation (new MapCoordinates3DEx (19, 10, 1));
		mem.getUnit ().add (inCombat);
		
		final MemoryUnit unit2 = new MemoryUnit ();
		unit2.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		unit2.setOwningPlayerID (1);
		unit2.setStatus (UnitStatusID.ALIVE);
		mem.getUnit ().add (unit2);

		final MinimalUnitDetails mu2 = mock (MinimalUnitDetails.class);
		when (unitDetailsUtils.expandMinimalUnitDetails (unit2, players, mem, db)).thenReturn (mu2);
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		utils.setUnitDetailsUtils (unitDetailsUtils);
		
		// Run method
		final List<MinimalUnitDetails> stack = utils.buildUnitStackMinimalDetails (unit1, players, mem, db);
		
		// Check results
		assertEquals (2, stack.size ());
		assertSame (mu1, stack.get (0));
		assertSame (mu2, stack.get (1));
	}

	/**
	 * Tests the buildUnitStackMinimalDetails method where the unit we are calculating for is a MemoryUnit that is in combat, so searches for units in the same combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testBuildUnitStackMinimalDetails_InCombat () throws Exception
	{
		// These are just for mocks and the unit list
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final FogOfWarMemory mem = new FogOfWarMemory ();
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Unit we are calculating stats for
		final UnitDetailsUtils unitDetailsUtils = mock (UnitDetailsUtils.class);

		// Other units
		final MemoryUnit unit1 = new MemoryUnit ();
		unit1.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		unit1.setOwningPlayerID (1);
		unit1.setStatus (UnitStatusID.ALIVE);
		unit1.setCombatLocation (new MapCoordinates3DEx (20, 9, 1));
		mem.getUnit ().add (unit1);

		final MinimalUnitDetails mu1 = mock (MinimalUnitDetails.class);
		when (unitDetailsUtils.expandMinimalUnitDetails (unit1, players, mem, db)).thenReturn (mu1);

		final MemoryUnit wrongLocation = new MemoryUnit ();
		wrongLocation.setUnitLocation (new MapCoordinates3DEx (21, 10, 1));
		wrongLocation.setOwningPlayerID (1);
		wrongLocation.setStatus (UnitStatusID.ALIVE);
		wrongLocation.setCombatLocation (new MapCoordinates3DEx (20, 9, 1));
		mem.getUnit ().add (wrongLocation);

		final MemoryUnit wrongPlayer = new MemoryUnit ();
		wrongPlayer.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		wrongPlayer.setOwningPlayerID (2);
		wrongPlayer.setStatus (UnitStatusID.ALIVE);
		wrongPlayer.setCombatLocation (new MapCoordinates3DEx (20, 9, 1));
		mem.getUnit ().add (wrongPlayer);
		
		final MemoryUnit deadUnit = new MemoryUnit ();
		deadUnit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		deadUnit.setOwningPlayerID (1);
		deadUnit.setStatus (UnitStatusID.DEAD);
		deadUnit.setCombatLocation (new MapCoordinates3DEx (20, 9, 1));
		mem.getUnit ().add (deadUnit);
		
		final MemoryUnit wrongCombat = new MemoryUnit ();
		wrongCombat.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		wrongCombat.setOwningPlayerID (1);
		wrongCombat.setStatus (UnitStatusID.ALIVE);
		wrongCombat.setCombatLocation (new MapCoordinates3DEx (19, 10, 1));
		mem.getUnit ().add (wrongCombat);
		
		final MemoryUnit notInCombat = new MemoryUnit ();
		notInCombat.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		notInCombat.setOwningPlayerID (1);
		notInCombat.setStatus (UnitStatusID.ALIVE);
		mem.getUnit ().add (notInCombat);
		
		final MemoryUnit unit2 = new MemoryUnit ();
		unit2.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		unit2.setOwningPlayerID (1);
		unit2.setStatus (UnitStatusID.ALIVE);
		unit2.setCombatLocation (new MapCoordinates3DEx (20, 9, 1));
		mem.getUnit ().add (unit2);

		final MinimalUnitDetails mu2 = mock (MinimalUnitDetails.class);
		when (unitDetailsUtils.expandMinimalUnitDetails (unit2, players, mem, db)).thenReturn (mu2);
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		utils.setUnitDetailsUtils (unitDetailsUtils);
		
		// Run method
		final List<MinimalUnitDetails> stack = utils.buildUnitStackMinimalDetails (unit1, players, mem, db);
		
		// Check results
		assertEquals (2, stack.size ());
		assertSame (mu1, stack.get (0));
		assertSame (mu2, stack.get (1));
	}
	
	/**
	 * Tests the buildUnitStackUnitURNs method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testBuildUnitStackUnitURNs () throws Exception
	{
		// Unit stack
		final List<MinimalUnitDetails> stack = new ArrayList<MinimalUnitDetails> ();
		
		final MinimalUnitDetails mu1 = mock (MinimalUnitDetails.class);
		when (mu1.isMemoryUnit ()).thenReturn (true);
		when (mu1.getUnitURN ()).thenReturn (3);
		stack.add (mu1);
		
		final MinimalUnitDetails mu2 = mock (MinimalUnitDetails.class);
		when (mu2.isMemoryUnit ()).thenReturn (false);
		stack.add (mu2);
		
		final MinimalUnitDetails mu3 = mock (MinimalUnitDetails.class);
		when (mu3.isMemoryUnit ()).thenReturn (true);
		when (mu3.getUnitURN ()).thenReturn (5);
		stack.add (mu3);
		
		final MinimalUnitDetails mu4 = mock (MinimalUnitDetails.class);
		when (mu4.isMemoryUnit ()).thenReturn (false);
		stack.add (mu4);
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		
		// Run method
		final List<Integer> unitURNs = utils.buildUnitStackUnitURNs (stack);
		
		// Check results
		assertEquals (2, unitURNs.size ());
		assertEquals (3, unitURNs.get (0));
		assertEquals (5, unitURNs.get (1));
	}
	
	/**
	 * Tests the getHighestSkillsInUnitStack method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetHighestSkillsInUnitStack () throws Exception
	{
		// Unit stack
		final List<MinimalUnitDetails> stack = new ArrayList<MinimalUnitDetails> ();

		final Set<String> mu1Skills = new HashSet<String> ();
		mu1Skills.add ("US001");
		mu1Skills.add ("US002");
		mu1Skills.add ("US003");
		final MinimalUnitDetails mu1 = mock (MinimalUnitDetails.class);
		when (mu1.listBasicSkillIDs ()).thenReturn (mu1Skills);
		when (mu1.getBasicOrHeroSkillValue ("US001")).thenReturn (2);
		when (mu1.getBasicOrHeroSkillValue ("US002")).thenReturn (null);
		when (mu1.getBasicOrHeroSkillValue ("US003")).thenReturn (3);
		stack.add (mu1);
		
		final Set<String> mu2Skills = new HashSet<String> ();
		mu2Skills.add ("US002");
		mu2Skills.add ("US003");
		mu2Skills.add ("US004");
		final MinimalUnitDetails mu2 = mock (MinimalUnitDetails.class);
		when (mu2.listBasicSkillIDs ()).thenReturn (mu2Skills);
		when (mu2.getBasicOrHeroSkillValue ("US002")).thenReturn (null);
		when (mu2.getBasicOrHeroSkillValue ("US003")).thenReturn (1);
		when (mu2.getBasicOrHeroSkillValue ("US004")).thenReturn (1);
		stack.add (mu2);
		
		final Set<String> mu3Skills = new HashSet<String> ();
		mu3Skills.add ("US004");
		final MinimalUnitDetails mu3 = mock (MinimalUnitDetails.class);
		when (mu3.listBasicSkillIDs ()).thenReturn (mu3Skills);
		when (mu3.getBasicOrHeroSkillValue ("US004")).thenReturn (4);
		stack.add (mu3);
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		
		// Run method
		final Map<String, Integer> map = utils.getHighestSkillsInUnitStack (stack);
		
		// Check results
		assertEquals (3, map.size ());
		assertEquals (2, map.get ("US001"));
		assertEquals (3, map.get ("US003"));
		assertEquals (4, map.get ("US004"));
	}
	
	/**
	 * Tests the addSkillsFromSpells method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillsFromSpells () throws Exception
	{
		// Spell definitions
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Spell spellDef1 = new Spell ();
		when (db.findSpell ("SP001", "addSkillsFromSpells")).thenReturn (spellDef1);

		final UnitSpellEffect effect3 = new UnitSpellEffect ();
		effect3.setUnitSkillID ("SS003");
		effect3.setUnitSkillValue (6);
		
		final Spell spellDef3 = new Spell ();
		spellDef3.getUnitSpellEffect ().add (effect3);
		when (db.findSpell ("SP003", "addSkillsFromSpells")).thenReturn (spellDef3);

		final UnitSpellEffect effect4 = new UnitSpellEffect ();
		effect4.setUnitSkillID ("SS004");
		effect4.setUnitSkillValue (2);
		
		final Spell spellDef4 = new Spell ();
		spellDef4.getUnitSpellEffect ().add (effect4);
		when (db.findSpell ("SP004", "addSkillsFromSpells")).thenReturn (spellDef4);
		
		final Spell spellDef5 = new Spell ();
		when (db.findSpell ("SP005", "addSkillsFromSpells")).thenReturn (spellDef5);
		
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		when (mu.isMemoryUnit ()).thenReturn (true);
		when (mu.getUnitURN ()).thenReturn (4);
		
		final Map<String, Integer> basicSkillValues = new HashMap<String, Integer> ();

		// Other units in the stack
		final List<Integer> unitStackUnitURNs = new ArrayList<Integer> ();
		unitStackUnitURNs.add (1);
		unitStackUnitURNs.add (3);
		unitStackUnitURNs.add (4);
		unitStackUnitURNs.add (5);
		
		final Map<String, Integer> unitStackSkills = new HashMap<String, Integer> ();
		
		// List of spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		for (int n = 1; n <= 5; n++)
		{
			final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
			spell.setUnitURN (n);
			spell.setSpellID ("SP00" + n);
			spell.setUnitSkillID ("SS00" + n);
			spell.setCastingPlayerID (10 + n);
			spells.add (spell);
		}
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		
		// Run method
		final Map<String, Integer> skillsFromSpellsCastOnThisUnit = utils.addSkillsFromSpells (mu, spells, unitStackUnitURNs, basicSkillValues, unitStackSkills, db);
		
		// Check results
		assertEquals (1, basicSkillValues.size ());
		assertEquals (2, basicSkillValues.get ("SS004"));
		
		assertEquals (2, unitStackSkills.size ());
		assertEquals (2, unitStackSkills.get ("SS004"));
		assertEquals (6, unitStackSkills.get ("SS003"));
		
		assertEquals (1, skillsFromSpellsCastOnThisUnit.size ());
		assertEquals (14, skillsFromSpellsCastOnThisUnit.get ("SS004"));
	}
	
	/**
	 * Tests the addValuelessSkillsFromHeroItems method where we don't specify the type of attack skill used
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddValuelessSkillsFromHeroItems_NoAttackSkillSpecified () throws Exception
	{
		// Hero item definitions
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitSkillAndValue swordBonus = new UnitSkillAndValue ();
		swordBonus.setUnitSkillID ("US001");
		swordBonus.setUnitSkillValue (2);
		
		final HeroItemType sword = new HeroItemType ();
		sword.getHeroItemTypeBasicStat ().add (swordBonus);
		sword.getHeroItemTypeAttackType ().add ("US005");
		when (db.findHeroItemType ("IT01", "addValuelessSkillsFromHeroItems")).thenReturn (sword);

		final UnitSkillAndValue shieldBonus = new UnitSkillAndValue ();
		shieldBonus.setUnitSkillID ("US002");
		
		final HeroItemType shield = new HeroItemType ();
		shield.getHeroItemTypeBasicStat ().add (shieldBonus);
		when (db.findHeroItemType ("IT03", "addValuelessSkillsFromHeroItems")).thenReturn (shield);
		
		final HeroItemBonusStat flamingBonus = new HeroItemBonusStat ();
		flamingBonus.setUnitSkillID ("US003");
		flamingBonus.setAppliesOnlyToAttacksAppropriateForTypeOfHeroItem (true);
		
		final HeroItemBonus flaming = new HeroItemBonus ();
		flaming.getHeroItemBonusStat ().add (flamingBonus);
		when (db.findHeroItemBonus ("IB01", "addValuelessSkillsFromHeroItems")).thenReturn (flaming);

		final HeroItemBonusStat blockingBonus = new HeroItemBonusStat ();
		blockingBonus.setUnitSkillID ("US004");
		
		final HeroItemBonus blocking = new HeroItemBonus ();
		blocking.getHeroItemBonusStat ().add (blockingBonus);
		when (db.findHeroItemBonus ("IB03", "addValuelessSkillsFromHeroItems")).thenReturn (blocking);
		
		// Unit we are calculating stats for
		final List<MemoryUnitHeroItemSlot> slots = new ArrayList<MemoryUnitHeroItemSlot> ();
		
		final NumberedHeroItem item1 = new NumberedHeroItem ();
		item1.setHeroItemTypeID ("IT01");
		item1.getHeroItemChosenBonus ().add ("IB01");
		
		final MemoryUnitHeroItemSlot slot1 = new MemoryUnitHeroItemSlot ();
		slot1.setHeroItem (item1);
		slots.add (slot1);
		
		final MemoryUnitHeroItemSlot slot2 = new MemoryUnitHeroItemSlot ();
		slots.add (slot2);
		
		final NumberedHeroItem item3 = new NumberedHeroItem ();
		item3.getHeroItemChosenBonus ().add ("IB03");
		item3.setHeroItemTypeID ("IT03");
		
		final MemoryUnitHeroItemSlot slot3 = new MemoryUnitHeroItemSlot ();
		slot3.setHeroItem (item3);
		slots.add (slot3);		
		
		final Map<String, Integer> basicSkillValues = new HashMap<String, Integer> ();
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		
		// Run method
		utils.addValuelessSkillsFromHeroItems (slots, null, basicSkillValues, db);
		
		// Check results
		assertEquals (2, basicSkillValues.size ());
		assertTrue (basicSkillValues.containsKey ("US002"));		// From shield basic item type 
		assertTrue (basicSkillValues.containsKey ("US004"));		// From blocking bonus imbued into it 
	}

	/**
	 * Tests the addValuelessSkillsFromHeroItems method where we specify the type of attack skill used, but its the wrong skill
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddValuelessSkillsFromHeroItems_Applies () throws Exception
	{
		// Hero item definitions
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitSkillAndValue swordBonus = new UnitSkillAndValue ();
		swordBonus.setUnitSkillID ("US001");
		swordBonus.setUnitSkillValue (2);
		
		final HeroItemType sword = new HeroItemType ();
		sword.getHeroItemTypeBasicStat ().add (swordBonus);
		sword.getHeroItemTypeAttackType ().add ("US005");
		when (db.findHeroItemType ("IT01", "addValuelessSkillsFromHeroItems")).thenReturn (sword);

		final UnitSkillAndValue shieldBonus = new UnitSkillAndValue ();
		shieldBonus.setUnitSkillID ("US002");
		
		final HeroItemType shield = new HeroItemType ();
		shield.getHeroItemTypeBasicStat ().add (shieldBonus);
		when (db.findHeroItemType ("IT03", "addValuelessSkillsFromHeroItems")).thenReturn (shield);
		
		final HeroItemBonusStat flamingBonus = new HeroItemBonusStat ();
		flamingBonus.setUnitSkillID ("US003");
		flamingBonus.setAppliesOnlyToAttacksAppropriateForTypeOfHeroItem (true);
		
		final HeroItemBonus flaming = new HeroItemBonus ();
		flaming.getHeroItemBonusStat ().add (flamingBonus);
		when (db.findHeroItemBonus ("IB01", "addValuelessSkillsFromHeroItems")).thenReturn (flaming);

		final HeroItemBonusStat blockingBonus = new HeroItemBonusStat ();
		blockingBonus.setUnitSkillID ("US004");
		
		final HeroItemBonus blocking = new HeroItemBonus ();
		blocking.getHeroItemBonusStat ().add (blockingBonus);
		when (db.findHeroItemBonus ("IB03", "addValuelessSkillsFromHeroItems")).thenReturn (blocking);
		
		// Unit we are calculating stats for
		final List<MemoryUnitHeroItemSlot> slots = new ArrayList<MemoryUnitHeroItemSlot> ();
		
		final NumberedHeroItem item1 = new NumberedHeroItem ();
		item1.setHeroItemTypeID ("IT01");
		item1.getHeroItemChosenBonus ().add ("IB01");
		
		final MemoryUnitHeroItemSlot slot1 = new MemoryUnitHeroItemSlot ();
		slot1.setHeroItem (item1);
		slots.add (slot1);
		
		final MemoryUnitHeroItemSlot slot2 = new MemoryUnitHeroItemSlot ();
		slots.add (slot2);
		
		final NumberedHeroItem item3 = new NumberedHeroItem ();
		item3.getHeroItemChosenBonus ().add ("IB03");
		item3.setHeroItemTypeID ("IT03");
		
		final MemoryUnitHeroItemSlot slot3 = new MemoryUnitHeroItemSlot ();
		slot3.setHeroItem (item3);
		slots.add (slot3);		
		
		final Map<String, Integer> basicSkillValues = new HashMap<String, Integer> ();
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		
		// Run method
		utils.addValuelessSkillsFromHeroItems (slots, "US006", basicSkillValues, db);
		
		// Check results
		assertEquals (2, basicSkillValues.size ());
		assertTrue (basicSkillValues.containsKey ("US002"));		// From shield basic item type 
		assertTrue (basicSkillValues.containsKey ("US004"));		// From blocking bonus imbued into it 
	}

	/**
	 * Tests the addValuelessSkillsFromHeroItems method where we specify the type of attack skill used, and it matches
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddValuelessSkillsFromHeroItems_NotApplies () throws Exception
	{
		// Hero item definitions
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitSkillAndValue swordBonus = new UnitSkillAndValue ();
		swordBonus.setUnitSkillID ("US001");
		swordBonus.setUnitSkillValue (2);
		
		final HeroItemType sword = new HeroItemType ();
		sword.getHeroItemTypeBasicStat ().add (swordBonus);
		sword.getHeroItemTypeAttackType ().add ("US005");
		when (db.findHeroItemType ("IT01", "addValuelessSkillsFromHeroItems")).thenReturn (sword);

		final UnitSkillAndValue shieldBonus = new UnitSkillAndValue ();
		shieldBonus.setUnitSkillID ("US002");
		
		final HeroItemType shield = new HeroItemType ();
		shield.getHeroItemTypeBasicStat ().add (shieldBonus);
		when (db.findHeroItemType ("IT03", "addValuelessSkillsFromHeroItems")).thenReturn (shield);
		
		final HeroItemBonusStat flamingBonus = new HeroItemBonusStat ();
		flamingBonus.setUnitSkillID ("US003");
		flamingBonus.setAppliesOnlyToAttacksAppropriateForTypeOfHeroItem (true);
		
		final HeroItemBonus flaming = new HeroItemBonus ();
		flaming.getHeroItemBonusStat ().add (flamingBonus);
		when (db.findHeroItemBonus ("IB01", "addValuelessSkillsFromHeroItems")).thenReturn (flaming);

		final HeroItemBonusStat blockingBonus = new HeroItemBonusStat ();
		blockingBonus.setUnitSkillID ("US004");
		
		final HeroItemBonus blocking = new HeroItemBonus ();
		blocking.getHeroItemBonusStat ().add (blockingBonus);
		when (db.findHeroItemBonus ("IB03", "addValuelessSkillsFromHeroItems")).thenReturn (blocking);
		
		// Unit we are calculating stats for
		final List<MemoryUnitHeroItemSlot> slots = new ArrayList<MemoryUnitHeroItemSlot> ();
		
		final NumberedHeroItem item1 = new NumberedHeroItem ();
		item1.setHeroItemTypeID ("IT01");
		item1.getHeroItemChosenBonus ().add ("IB01");
		
		final MemoryUnitHeroItemSlot slot1 = new MemoryUnitHeroItemSlot ();
		slot1.setHeroItem (item1);
		slots.add (slot1);
		
		final MemoryUnitHeroItemSlot slot2 = new MemoryUnitHeroItemSlot ();
		slots.add (slot2);
		
		final NumberedHeroItem item3 = new NumberedHeroItem ();
		item3.getHeroItemChosenBonus ().add ("IB03");
		item3.setHeroItemTypeID ("IT03");
		
		final MemoryUnitHeroItemSlot slot3 = new MemoryUnitHeroItemSlot ();
		slot3.setHeroItem (item3);
		slots.add (slot3);		
		
		final Map<String, Integer> basicSkillValues = new HashMap<String, Integer> ();
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		
		// Run method
		utils.addValuelessSkillsFromHeroItems (slots, "US005", basicSkillValues, db);
		
		// Check results
		assertEquals (3, basicSkillValues.size ());
		assertTrue (basicSkillValues.containsKey ("US002"));		// From shield basic item type 
		assertTrue (basicSkillValues.containsKey ("US004"));		// From blocking bonus imbued into it 
		assertTrue (basicSkillValues.containsKey ("US003"));		// From flaming bonus imbued into the sword, now we're making the right kind of attack for a sword 
	}
	
	/**
	 * Tests the addSkillsGrantedFromOtherSkills method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillsGrantedFromOtherSkills () throws Exception
	{
		// Skill definitions
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitSkillEx skill1 = new UnitSkillEx ();
		skill1.getGrantsSkill ().add ("US002");
		when (db.findUnitSkill ("US001", "addSkillsGrantedFromOtherSkills")).thenReturn (skill1);

		final UnitSkillEx skill2 = new UnitSkillEx ();
		when (db.findUnitSkill ("US002", "addSkillsGrantedFromOtherSkills")).thenReturn (skill2);

		final UnitSkillEx skill3 = new UnitSkillEx ();
		skill3.getGrantsSkill ().add ("US004");
		when (db.findUnitSkill ("US003", "addSkillsGrantedFromOtherSkills")).thenReturn (skill3);
		
		final UnitSkillEx skill4 = new UnitSkillEx ();
		skill4.getGrantsSkill ().add ("US005");
		when (db.findUnitSkill ("US004", "addSkillsGrantedFromOtherSkills")).thenReturn (skill4);

		final UnitSkillEx skill5 = new UnitSkillEx ();
		when (db.findUnitSkill ("US005", "addSkillsGrantedFromOtherSkills")).thenReturn (skill5);
		
		// Skills we start with
		final Map<String, Integer> basicSkillValues = new HashMap<String, Integer> ();
		basicSkillValues.put ("US001", null);
		basicSkillValues.put ("US003", null);
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		
		// Run method
		utils.addSkillsGrantedFromOtherSkills (basicSkillValues, db);
		
		// Check results
		assertEquals (5, basicSkillValues.size ());
		assertTrue (basicSkillValues.containsKey ("US001"));		// Started with it
		assertTrue (basicSkillValues.containsKey ("US002"));		// US001 grants US002
		assertTrue (basicSkillValues.containsKey ("US003"));		// Started with it
		assertTrue (basicSkillValues.containsKey ("US004"));		// US003 grants US004
		assertTrue (basicSkillValues.containsKey ("US005"));		// US003 grants US004 which grants US005
	}

	/**
	 * Tests the copySkillValuesRemovingNegatedSkills method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCopySkillValuesRemovingNegatedSkills () throws Exception
	{
		// These are just for mocks
		final CommonDatabase db = mock (CommonDatabase.class);
		final List<ExpandedUnitDetails> enemyUnits = new ArrayList<ExpandedUnitDetails> (); 

		// Skills we start with
		final Map<String, Integer> basicSkillValues = new HashMap<String, Integer> ();
		basicSkillValues.put ("US001", null);
		basicSkillValues.put ("US002", 2);
		basicSkillValues.put ("US003", null);
		basicSkillValues.put ("US004", 4);
		
		// Which skills are negated
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.isSkillNegated ("US001", basicSkillValues, enemyUnits, db)).thenReturn (true);
		when (unitUtils.isSkillNegated ("US002", basicSkillValues, enemyUnits, db)).thenReturn (false);
		when (unitUtils.isSkillNegated ("US003", basicSkillValues, enemyUnits, db)).thenReturn (false);
		when (unitUtils.isSkillNegated ("US004", basicSkillValues, enemyUnits, db)).thenReturn (true);
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		
		// Run method
		final Map<String, Integer> basicSkillValuesWithNegatedSkillsRemoved = utils.copySkillValuesRemovingNegatedSkills (basicSkillValues, enemyUnits, db);
		
		// Check results
		assertEquals (2, basicSkillValuesWithNegatedSkillsRemoved.size ());
		assertEquals (2, basicSkillValuesWithNegatedSkillsRemoved.get ("US002"));
		assertTrue (basicSkillValuesWithNegatedSkillsRemoved.containsKey ("US003"));
	}
	
	/**
	 * Tests the determineModifiedMagicRealmLifeformType method when no skills modify the unit's magic realm/lifeform type
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineModifiedMagicRealmLifeformType_NoModifications () throws Exception
	{
		// Skill and pick definitions
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitSkillEx uninterestingSkill = new UnitSkillEx ();
		when (db.findUnitSkill ("US001", "determineModifiedMagicRealmLifeformType")).thenReturn (uninterestingSkill);
		
		final Pick defaultMagicRealm = new Pick ();
		when (db.findPick ("MB01", "determineModifiedMagicRealmLifeformType")).thenReturn (defaultMagicRealm);

		// Skills the unit has 
		final Map<String, Integer> basicSkillValues = new HashMap<String, Integer> ();
		basicSkillValues.put ("US001", null);
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		
		// Run method
		final Pick magicRealmLifeformType = utils.determineModifiedMagicRealmLifeformType ("MB01", basicSkillValues, db);
		
		// Check results
		assertSame (defaultMagicRealm, magicRealmLifeformType);
	}

	/**
	 * Tests the determineModifiedMagicRealmLifeformType method when one skill modifies the unit's magic realm/lifeform type
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineModifiedMagicRealmLifeformType_OneModification () throws Exception
	{
		// Skill and pick definitions
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitSkillEx uninterestingSkill = new UnitSkillEx ();
		when (db.findUnitSkill ("US001", "determineModifiedMagicRealmLifeformType")).thenReturn (uninterestingSkill);
		
		final UnitSkillEx firstModification = new UnitSkillEx ();
		firstModification.setChangesUnitToMagicRealm ("MB02");
		when (db.findUnitSkill ("US002", "determineModifiedMagicRealmLifeformType")).thenReturn (firstModification);
		
		final Pick modifiedMagicRealm = new Pick ();
		when (db.findPick ("MB02", "determineModifiedMagicRealmLifeformType")).thenReturn (modifiedMagicRealm);
		
		// Skills the unit has 
		final Map<String, Integer> basicSkillValues = new HashMap<String, Integer> ();
		basicSkillValues.put ("US001", null);
		basicSkillValues.put ("US002", null);
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		
		// Run method
		final Pick magicRealmLifeformType = utils.determineModifiedMagicRealmLifeformType ("MB01", basicSkillValues, db);
		
		// Check results
		assertSame (modifiedMagicRealm, magicRealmLifeformType);
	}

	/**
	 * Tests the determineModifiedMagicRealmLifeformType method when two skills both modify the unit's magic realm/lifeform type
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineModifiedMagicRealmLifeformType_TwoModifications () throws Exception
	{
		// Skill and pick definitions
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitSkillEx uninterestingSkill = new UnitSkillEx ();
		when (db.findUnitSkill ("US001", "determineModifiedMagicRealmLifeformType")).thenReturn (uninterestingSkill);
		
		final UnitSkillEx firstModification = new UnitSkillEx ();
		firstModification.setChangesUnitToMagicRealm ("MB02");
		when (db.findUnitSkill ("US002", "determineModifiedMagicRealmLifeformType")).thenReturn (firstModification);

		final UnitSkillEx secondModification = new UnitSkillEx ();
		secondModification.setChangesUnitToMagicRealm ("MB03");
		when (db.findUnitSkill ("US003", "determineModifiedMagicRealmLifeformType")).thenReturn (secondModification);
		
		final Pick magicRealm4 = new Pick ();
		magicRealm4.getMergedFromPick ().add ("MB02");

		final Pick magicRealm5 = new Pick ();
		magicRealm5.getMergedFromPick ().add ("MB01");
		magicRealm5.getMergedFromPick ().add ("MB02");
		
		final Pick magicRealm6 = new Pick ();
		magicRealm6.getMergedFromPick ().add ("MB02");
		magicRealm6.getMergedFromPick ().add ("MB03");
		
		when (db.getPick ()).thenReturn (Arrays.asList (magicRealm4, magicRealm5, magicRealm6));
		
		// Skills the unit has 
		final Map<String, Integer> basicSkillValues = new HashMap<String, Integer> ();
		basicSkillValues.put ("US001", null);
		basicSkillValues.put ("US002", null);
		basicSkillValues.put ("US003", null);
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		
		// Run method
		final Pick magicRealmLifeformType = utils.determineModifiedMagicRealmLifeformType ("MB01", basicSkillValues, db);
		
		// Check results
		assertSame (magicRealm6, magicRealmLifeformType);
	}

	/**
	 * Tests the determineModifiedMagicRealmLifeformType method when two skills both modify the unit's magic realm/lifeform type
	 * and no magic realm is defined for that combination
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineModifiedMagicRealmLifeformType_Invalid () throws Exception
	{
		// Skill and pick definitions
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitSkillEx uninterestingSkill = new UnitSkillEx ();
		when (db.findUnitSkill ("US001", "determineModifiedMagicRealmLifeformType")).thenReturn (uninterestingSkill);
		
		final UnitSkillEx firstModification = new UnitSkillEx ();
		firstModification.setChangesUnitToMagicRealm ("MB02");
		when (db.findUnitSkill ("US002", "determineModifiedMagicRealmLifeformType")).thenReturn (firstModification);

		final UnitSkillEx secondModification = new UnitSkillEx ();
		secondModification.setChangesUnitToMagicRealm ("MB03");
		when (db.findUnitSkill ("US003", "determineModifiedMagicRealmLifeformType")).thenReturn (secondModification);
		
		final Pick magicRealm4 = new Pick ();
		magicRealm4.getMergedFromPick ().add ("MB02");

		final Pick magicRealm5 = new Pick ();
		magicRealm5.getMergedFromPick ().add ("MB01");
		magicRealm5.getMergedFromPick ().add ("MB02");
		
		when (db.getPick ()).thenReturn (Arrays.asList (magicRealm4, magicRealm5));
		
		// Skills the unit has 
		final Map<String, Integer> basicSkillValues = new HashMap<String, Integer> ();
		basicSkillValues.put ("US001", null);
		basicSkillValues.put ("US002", null);
		basicSkillValues.put ("US003", null);
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		
		// Run method
		assertThrows (MomException.class, () ->
		{
			utils.determineModifiedMagicRealmLifeformType ("MB01", basicSkillValues, db);
		});
	}
	
	/**
	 * Tests the buildInitialBreakdownFromBasicSkills method
	 */
	@Test
	public final void testBuildInitialBreakdownFromBasicSkills ()
	{
		// Skills the unit has 
		final Map<String, Integer> basicSkillValues = new HashMap<String, Integer> ();
		basicSkillValues.put ("US001", 2);
		basicSkillValues.put ("US002", null);
		basicSkillValues.put ("US003", 3);

		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		
		// Run method
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = utils.buildInitialBreakdownFromBasicSkills (basicSkillValues);
		
		// Check results
		assertEquals (3, modifiedSkillValues.size ());
		
		assertEquals (UnitSkillComponent.BASIC, modifiedSkillValues.get ("US001").getSource ());
		assertEquals (1, modifiedSkillValues.get ("US001").getComponents ().size ());
		assertEquals (2, modifiedSkillValues.get ("US001").getComponents ().get (UnitSkillComponent.BASIC));
		
		assertEquals (UnitSkillComponent.BASIC, modifiedSkillValues.get ("US002").getSource ());
		assertEquals (0, modifiedSkillValues.get ("US002").getComponents ().size ());
		
		assertEquals (UnitSkillComponent.BASIC, modifiedSkillValues.get ("US003").getSource ());
		assertEquals (1, modifiedSkillValues.get ("US003").getComponents ().size ());
		assertEquals (3, modifiedSkillValues.get ("US003").getComponents ().get (UnitSkillComponent.BASIC));
	}
	
	/**
	 * Tests increasing our movement speed because we have Wind Mastery cast
	 */
	@Test
	public final void testAdjustMovementSpeedForWindMastery_Increase ()
	{
		// Skills the unit has 
		final Map<String, Integer> basicSkillValues = new HashMap<String, Integer> ();
		basicSkillValues.put (CommonDatabaseConstants.UNIT_SKILL_ID_SAILING, null);
		basicSkillValues.put (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED, 5);

		// Skill breakdowns
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (UnitSkillComponent.BASIC);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 5);
		
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		modifiedSkillValues.put (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED, breakdown);

		// List of spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		final MemoryMaintainedSpell ourWindMastery = new MemoryMaintainedSpell ();
		ourWindMastery.setSpellID (CommonDatabaseConstants.SPELL_ID_WIND_MASTERY);
		ourWindMastery.setCastingPlayerID (1);
		spells.add (ourWindMastery);
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		
		// Run method
		utils.adjustMovementSpeedForWindMastery (basicSkillValues, modifiedSkillValues, 1, spells);
		
		// Check results
		assertEquals (2, breakdown.getComponents ().size ());
		assertEquals (5, breakdown.getComponents ().get (UnitSkillComponent.BASIC));
		assertEquals (2, breakdown.getComponents ().get (UnitSkillComponent.SPELL_EFFECTS));
	}

	/**
	 * Tests decreasing our movement speed because someone else has Wind Mastery cast
	 */
	@Test
	public final void testAdjustMovementSpeedForWindMastery_Decrease ()
	{
		// Skills the unit has 
		final Map<String, Integer> basicSkillValues = new HashMap<String, Integer> ();
		basicSkillValues.put (CommonDatabaseConstants.UNIT_SKILL_ID_SAILING, null);
		basicSkillValues.put (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED, 5);

		// Skill breakdowns
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (UnitSkillComponent.BASIC);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 5);
		
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		modifiedSkillValues.put (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED, breakdown);

		// List of spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		final MemoryMaintainedSpell theirWindMastery = new MemoryMaintainedSpell ();
		theirWindMastery.setSpellID (CommonDatabaseConstants.SPELL_ID_WIND_MASTERY);
		theirWindMastery.setCastingPlayerID (2);
		spells.add (theirWindMastery);
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		
		// Run method
		utils.adjustMovementSpeedForWindMastery (basicSkillValues, modifiedSkillValues, 1, spells);
		
		// Check results
		assertEquals (2, breakdown.getComponents ().size ());
		assertEquals (5, breakdown.getComponents ().get (UnitSkillComponent.BASIC));
		assertEquals (-2, breakdown.getComponents ().get (UnitSkillComponent.SPELL_EFFECTS));
	}

	/**
	 * Tests us and another wizard both having Wind Mastery cast so they cancel each other out
	 */
	@Test
	public final void testAdjustMovementSpeedForWindMastery_Cancel ()
	{
		// Skills the unit has 
		final Map<String, Integer> basicSkillValues = new HashMap<String, Integer> ();
		basicSkillValues.put (CommonDatabaseConstants.UNIT_SKILL_ID_SAILING, null);
		basicSkillValues.put (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED, 5);

		// Skill breakdowns
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (UnitSkillComponent.BASIC);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 5);
		
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		modifiedSkillValues.put (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED, breakdown);

		// List of spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		final MemoryMaintainedSpell ourWindMastery = new MemoryMaintainedSpell ();
		ourWindMastery.setSpellID (CommonDatabaseConstants.SPELL_ID_WIND_MASTERY);
		ourWindMastery.setCastingPlayerID (1);
		spells.add (ourWindMastery);
		
		final MemoryMaintainedSpell theirWindMastery = new MemoryMaintainedSpell ();
		theirWindMastery.setSpellID (CommonDatabaseConstants.SPELL_ID_WIND_MASTERY);
		theirWindMastery.setCastingPlayerID (2);
		spells.add (theirWindMastery);
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		
		// Run method
		utils.adjustMovementSpeedForWindMastery (basicSkillValues, modifiedSkillValues, 1, spells);
		
		// Check results
		assertEquals (1, breakdown.getComponents ().size ());
		assertEquals (5, breakdown.getComponents ().get (UnitSkillComponent.BASIC));
	}

	/**
	 * Tests that we don't get a speed increase from Wind Mastery unless we're a boat (no sailing skill)
	 */
	@Test
	public final void testAdjustMovementSpeedForWindMastery_NotABoat ()
	{
		// Skills the unit has 
		final Map<String, Integer> basicSkillValues = new HashMap<String, Integer> ();
		basicSkillValues.put (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED, 5);

		// Skill breakdowns
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (UnitSkillComponent.BASIC);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 5);
		
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		modifiedSkillValues.put (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED, breakdown);

		// List of spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		final MemoryMaintainedSpell ourWindMastery = new MemoryMaintainedSpell ();
		ourWindMastery.setSpellID (CommonDatabaseConstants.SPELL_ID_WIND_MASTERY);
		ourWindMastery.setCastingPlayerID (1);
		spells.add (ourWindMastery);
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		
		// Run method
		utils.adjustMovementSpeedForWindMastery (basicSkillValues, modifiedSkillValues, 1, spells);
		
		// Check results
		assertEquals (1, breakdown.getComponents ().size ());
		assertEquals (5, breakdown.getComponents ().get (UnitSkillComponent.BASIC));
	}

	/**
	 * Tests that we don't get a speed increase from spells other than Wind Mastery
	 */
	@Test
	public final void testAdjustMovementSpeedForWindMastery_WrongSpell ()
	{
		// Skills the unit has 
		final Map<String, Integer> basicSkillValues = new HashMap<String, Integer> ();
		basicSkillValues.put (CommonDatabaseConstants.UNIT_SKILL_ID_SAILING, null);
		basicSkillValues.put (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED, 5);

		// Skill breakdowns
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (UnitSkillComponent.BASIC);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 5);
		
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		modifiedSkillValues.put (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED, breakdown);

		// List of spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		final MemoryMaintainedSpell ourWindMastery = new MemoryMaintainedSpell ();
		ourWindMastery.setSpellID ("X");
		ourWindMastery.setCastingPlayerID (1);
		spells.add (ourWindMastery);
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		
		// Run method
		utils.adjustMovementSpeedForWindMastery (basicSkillValues, modifiedSkillValues, 1, spells);
		
		// Check results
		assertEquals (1, breakdown.getComponents ().size ());
		assertEquals (5, breakdown.getComponents ().get (UnitSkillComponent.BASIC));
	}
	
	/**
	 * Tests the addBonusesFromWeaponGrade method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddBonusesFromWeaponGrade () throws Exception
	{
		// Weapon grade with bonuses
		final WeaponGrade weaponGrade = new WeaponGrade ();
		
		final AddsToSkill bonus1 = new AddsToSkill ();
		weaponGrade.getAddsToSkill ().add (bonus1);

		final AddsToSkill bonus2 = new AddsToSkill ();
		weaponGrade.getAddsToSkill ().add (bonus2);

		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		final Map<String, Integer> unitStackSkills = new HashMap<String, Integer> ();
		
		// Set up object to test
		final UnitDetailsUtils unitDetailsUtils = mock (UnitDetailsUtils.class);
		
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		utils.setUnitDetailsUtils (unitDetailsUtils);
		
		// Run method
		utils.addBonusesFromWeaponGrade (mu, weaponGrade, modifiedSkillValues, unitStackSkills, null, null, "MB01");
		
		// Check results
		verify (unitDetailsUtils, times (1)).addSkillBonus (mu, null, bonus1, UnitSkillComponent.WEAPON_GRADE, modifiedSkillValues, unitStackSkills, null, null, "MB01");
		verify (unitDetailsUtils, times (1)).addSkillBonus (mu, null, bonus2, UnitSkillComponent.WEAPON_GRADE, modifiedSkillValues, unitStackSkills, null, null, "MB01");
	}
	
	/**
	 * Tests the addBonusesFromExperienceLevel method
	 */
	@Test
	public final void testAddBonusesFromExperienceLevel ()
	{
		// Experience level with bonuses
		final ExperienceLevel expLvl = new ExperienceLevel ();
		
		final UnitSkillAndValue bonus1 = new UnitSkillAndValue ();
		bonus1.setUnitSkillID ("US001");
		bonus1.setUnitSkillValue (2);
		expLvl.getExperienceSkillBonus ().add (bonus1);

		final UnitSkillAndValue bonus2 = new UnitSkillAndValue ();
		bonus2.setUnitSkillID ("US002");
		expLvl.getExperienceSkillBonus ().add (bonus2);

		final UnitSkillAndValue bonus3 = new UnitSkillAndValue ();
		bonus3.setUnitSkillID ("US003");
		bonus3.setUnitSkillValue (3);
		expLvl.getExperienceSkillBonus ().add (bonus3);

		// Skill breakdowns
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (UnitSkillComponent.BASIC);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 2);
		modifiedSkillValues.put ("US003", breakdown);

		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		
		// Run method
		utils.addBonusesFromExperienceLevel (expLvl, modifiedSkillValues);
		
		// Check results
		assertEquals (1, modifiedSkillValues.size ());		// Can't add bonus to skill that we don't already have
		
		assertEquals (2, breakdown.getComponents ().size ());
		assertEquals (2, breakdown.getComponents ().get (UnitSkillComponent.BASIC));
		assertEquals (3, breakdown.getComponents ().get (UnitSkillComponent.EXPERIENCE));
	}
	
	/**
	 * Tests the addSkillsFromCombatAreaEffects method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillsFromCombatAreaEffects () throws Exception
	{
		// Combat area effect definitions
		final CommonDatabase db = mock (CommonDatabase.class);
		
		for (int n = 1; n <= 4; n++)
			if (n != 2)
			{
				final CombatAreaEffect caeDef = new CombatAreaEffect ();
				caeDef.getCombatAreaEffectGrantsSkill ().add ("US00" + n);
				when (db.findCombatAreaEffect ("CAE0" + n, "addSkillsFromCombatAreaEffects")).thenReturn (caeDef);
			}

		// Just for mocks
		final List<ExpandedUnitDetails> enemyUnits = new ArrayList<ExpandedUnitDetails> ();
		
		// Unit we are calculating stats for
		final AvailableUnit unit = new AvailableUnit ();
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();

		// Combat area effects with bonuses
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final MemoryCombatAreaEffect cae1 = new MemoryCombatAreaEffect ();
		cae1.setCombatAreaEffectID ("CAE01");
		combatAreaEffects.add (cae1);
		when (unitUtils.doesCombatAreaEffectApplyToUnit (unit, cae1, db)).thenReturn (true);
		when (unitUtils.isSkillNegated ("US001", modifiedSkillValues, enemyUnits, db)).thenReturn (false);
		
		final MemoryCombatAreaEffect cae2 = new MemoryCombatAreaEffect ();
		cae2.setCombatAreaEffectID ("CAE02");
		combatAreaEffects.add (cae2);
		when (unitUtils.doesCombatAreaEffectApplyToUnit (unit, cae2, db)).thenReturn (false);
		
		final MemoryCombatAreaEffect cae3 = new MemoryCombatAreaEffect ();
		cae3.setCombatAreaEffectID ("CAE03");
		combatAreaEffects.add (cae3);
		when (unitUtils.doesCombatAreaEffectApplyToUnit (unit, cae3, db)).thenReturn (true);
		when (unitUtils.isSkillNegated ("US003", modifiedSkillValues, enemyUnits, db)).thenReturn (true);
		
		final MemoryCombatAreaEffect cae4 = new MemoryCombatAreaEffect ();
		cae4.setCombatAreaEffectID ("CAE04");
		combatAreaEffects.add (cae4);
		when (unitUtils.doesCombatAreaEffectApplyToUnit (unit, cae4, db)).thenReturn (true);
		when (unitUtils.isSkillNegated ("US004", modifiedSkillValues, enemyUnits, db)).thenReturn (false);
					
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		
		// Run method
		final List<String> skillsGrantedFromCombatAreaEffects = utils.addSkillsFromCombatAreaEffects (unit, combatAreaEffects, modifiedSkillValues, enemyUnits, db);
		
		// Check results
		assertEquals (2, skillsGrantedFromCombatAreaEffects.size ());
		assertEquals ("US001", skillsGrantedFromCombatAreaEffects.get (0));
		assertEquals ("US004", skillsGrantedFromCombatAreaEffects.get (1));
	}
	
	/**
	 * Tests the removeNegatedSkillsAddedFromCombatAreaEffects method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRemoveNegatedSkillsAddedFromCombatAreaEffects () throws Exception
	{
		// Just for mocks
		final List<ExpandedUnitDetails> enemyUnits = new ArrayList<ExpandedUnitDetails> ();
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Skill breakdowns
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		modifiedSkillValues.put ("US001", null);
		modifiedSkillValues.put ("US002", null);
		modifiedSkillValues.put ("US003", null);
		
		// Skills to recheck
		final List<String> skillsGrantedFromCombatAreaEffects = new ArrayList<String> ();
		skillsGrantedFromCombatAreaEffects.add ("US002");
		skillsGrantedFromCombatAreaEffects.add ("US003");
		
		// Skills to remove
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.isSkillNegated ("US002", modifiedSkillValues, enemyUnits, db)).thenReturn (true);
		when (unitUtils.isSkillNegated ("US003", modifiedSkillValues, enemyUnits, db)).thenReturn (false);
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		
		// Run method
		utils.removeNegatedSkillsAddedFromCombatAreaEffects (skillsGrantedFromCombatAreaEffects, modifiedSkillValues, enemyUnits, db);
		
		// Check results
		assertEquals (2, modifiedSkillValues.size ());
		assertTrue (modifiedSkillValues.containsKey ("US001"));
		assertTrue (modifiedSkillValues.containsKey ("US003"));
	}
	
	/**
	 * Tests the addBonusesFromOtherSkills method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddBonusesFromOtherSkills () throws Exception
	{
		// Full skill list
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final AddsToSkill addsToSkill1 = new AddsToSkill (); 
		final UnitSkillEx skillDef1 = new UnitSkillEx ();
		skillDef1.setUnitSkillID ("US001");
		skillDef1.getAddsToSkill ().add (addsToSkill1);

		final AddsToSkill addsToSkill2 = new AddsToSkill (); 
		final UnitSkillEx skillDef2 = new UnitSkillEx ();
		skillDef2.setUnitSkillID ("US002");
		skillDef2.getAddsToSkill ().add (addsToSkill2);
		
		final AddsToSkill addsToSkill3 = new AddsToSkill ();
		addsToSkill3.setAffectsEntireStack (true);
		final UnitSkillEx skillDef3 = new UnitSkillEx ();
		skillDef3.setUnitSkillID ("US003");
		skillDef3.getAddsToSkill ().add (addsToSkill3);

		final AddsToSkill addsToSkill4 = new AddsToSkill ();
		addsToSkill4.setAffectsEntireStack (true);
		final UnitSkillEx skillDef4 = new UnitSkillEx ();
		skillDef4.setUnitSkillID ("US004");
		skillDef4.getAddsToSkill ().add (addsToSkill4);
		
		final AddsToSkill addsToSkill5 = new AddsToSkill ();
		addsToSkill5.setPenaltyToEnemy (true);
		final UnitSkillEx skillDef5 = new UnitSkillEx ();
		skillDef5.setUnitSkillID ("US005");
		skillDef5.getAddsToSkill ().add (addsToSkill5);

		final AddsToSkill addsToSkill6 = new AddsToSkill ();
		addsToSkill6.setPenaltyToEnemy (true);
		final UnitSkillEx skillDef6 = new UnitSkillEx ();
		skillDef6.setUnitSkillID ("US006");
		skillDef6.getAddsToSkill ().add (addsToSkill6);

		final AddsToSkill addsToSkill7 = new AddsToSkill (); 
		final UnitSkillEx skillDef7 = new UnitSkillEx ();
		skillDef7.setUnitSkillID ("US007");
		skillDef7.getAddsToSkill ().add (addsToSkill7);
		
		when (db.getUnitSkills ()).thenReturn (Arrays.asList (skillDef1, skillDef2, skillDef3, skillDef4, skillDef5, skillDef6, skillDef7));

		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);

		// Skill breakdowns
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		modifiedSkillValues.put ("US001", new UnitSkillValueBreakdown (UnitSkillComponent.BASIC));
		modifiedSkillValues.put ("US007", new UnitSkillValueBreakdown (UnitSkillComponent.COMBAT_AREA_EFFECTS));

		// Other units in the stack
		final Map<String, Integer> unitStackSkills = new HashMap<String, Integer> ();
		unitStackSkills.put ("US003", null);
		
		// Enemy units giving us penalties
		final ExpandedUnitDetails enemyUnit = mock (ExpandedUnitDetails.class);
		when (enemyUnit.hasModifiedSkill ("US005")).thenReturn (true);
		when (enemyUnit.hasModifiedSkill ("US006")).thenReturn (false);
		
		final List<ExpandedUnitDetails> enemyUnits = new ArrayList<ExpandedUnitDetails> ();
		enemyUnits.add (enemyUnit);
		
		// Verify skills being added
		final UnitDetailsUtils unitDetailsUtils = mock (UnitDetailsUtils.class);
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		utils.setUnitDetailsUtils (unitDetailsUtils);
		
		// Run method
		utils.addBonusesFromOtherSkills (mu, modifiedSkillValues, unitStackSkills, enemyUnits, null, null, "MB01", db);
		
		// Check results
		verify (unitDetailsUtils, times (1)).addSkillBonus (mu, "US001", addsToSkill1, null, modifiedSkillValues, unitStackSkills, null, null, "MB01");
		verify (unitDetailsUtils, times (0)).addSkillBonus (mu, "US002", addsToSkill2, null, modifiedSkillValues, unitStackSkills, null, null, "MB01");
		verify (unitDetailsUtils, times (1)).addSkillBonus (mu, "US003", addsToSkill3, null, modifiedSkillValues, unitStackSkills, null, null, "MB01");
		verify (unitDetailsUtils, times (0)).addSkillBonus (mu, "US004", addsToSkill4, null, modifiedSkillValues, unitStackSkills, null, null, "MB01");
		verify (unitDetailsUtils, times (1)).addSkillBonus (mu, "US005", addsToSkill5, null, modifiedSkillValues, unitStackSkills, null, null, "MB01");
		verify (unitDetailsUtils, times (0)).addSkillBonus (mu, "US006", addsToSkill6, null, modifiedSkillValues, unitStackSkills, null, null, "MB01");
		verify (unitDetailsUtils, times (1)).addSkillBonus (mu, "US007", addsToSkill7, UnitSkillComponent.COMBAT_AREA_EFFECTS,
			modifiedSkillValues, unitStackSkills, null, null, "MB01");
	}
	
	/**
	 * Tests the addBonusesFromHeroItems method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddBonusesFromHeroItems () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitSkillAndValue swordBonus = new UnitSkillAndValue ();
		swordBonus.setUnitSkillID ("US001");
		swordBonus.setUnitSkillValue (2);
		
		final HeroItemType sword = new HeroItemType ();
		sword.getHeroItemTypeBasicStat ().add (swordBonus);
		sword.getHeroItemTypeAttackType ().add ("US005");
		when (db.findHeroItemType ("IT01", "addBonusesFromHeroItems")).thenReturn (sword);

		final UnitSkillAndValue shieldBonus = new UnitSkillAndValue ();
		shieldBonus.setUnitSkillID ("US002");
		
		final HeroItemType shield = new HeroItemType ();
		shield.getHeroItemTypeBasicStat ().add (shieldBonus);
		shield.getHeroItemTypeAttackType ().add ("US001");
		when (db.findHeroItemType ("IT03", "addBonusesFromHeroItems")).thenReturn (shield);

		final HeroItemBonusStat flamingBonus = new HeroItemBonusStat ();
		flamingBonus.setUnitSkillID ("US003");
		
		final HeroItemBonus flaming = new HeroItemBonus ();
		flaming.getHeroItemBonusStat ().add (flamingBonus);
		when (db.findHeroItemBonus ("IB01", "addBonusesFromHeroItems")).thenReturn (flaming);

		final HeroItemBonusStat blockingBonus = new HeroItemBonusStat ();
		blockingBonus.setUnitSkillID ("US004");
		blockingBonus.setUnitSkillValue (3);
		
		final HeroItemBonus blocking = new HeroItemBonus ();
		blocking.getHeroItemBonusStat ().add (blockingBonus);
		when (db.findHeroItemBonus ("IB03", "addBonusesFromHeroItems")).thenReturn (blocking);
		
		final HeroItemBonusStat pokingBonus = new HeroItemBonusStat ();
		pokingBonus.setUnitSkillID ("US005");
		pokingBonus.setUnitSkillValue (1);
		
		final HeroItemBonus poking = new HeroItemBonus ();
		poking.getHeroItemBonusStat ().add (pokingBonus);
		when (db.findHeroItemBonus ("IB04", "addBonusesFromHeroItems")).thenReturn (poking);
		
		final HeroItemBonusStat attackingBonus = new HeroItemBonusStat ();
		attackingBonus.setUnitSkillID (CommonDatabaseConstants.UNIT_SKILL_ID_ATTACK_APPROPRIATE_FOR_TYPE_OF_HERO_ITEM);
		attackingBonus.setUnitSkillValue (4);
		
		final HeroItemBonus attacking = new HeroItemBonus ();
		attacking.getHeroItemBonusStat ().add (attackingBonus);
		when (db.findHeroItemBonus ("IB05", "addBonusesFromHeroItems")).thenReturn (attacking);
		
		// Unit we are calculating stats for
		final List<MemoryUnitHeroItemSlot> slots = new ArrayList<MemoryUnitHeroItemSlot> ();
		
		final NumberedHeroItem item1 = new NumberedHeroItem ();
		item1.setHeroItemTypeID ("IT01");
		item1.getHeroItemChosenBonus ().add ("IB01");
		
		final MemoryUnitHeroItemSlot slot1 = new MemoryUnitHeroItemSlot ();
		slot1.setHeroItem (item1);
		slots.add (slot1);
		
		final MemoryUnitHeroItemSlot slot2 = new MemoryUnitHeroItemSlot ();
		slots.add (slot2);
		
		final NumberedHeroItem item3 = new NumberedHeroItem ();
		item3.getHeroItemChosenBonus ().add ("IB03");
		item3.getHeroItemChosenBonus ().add ("IB04");
		item3.getHeroItemChosenBonus ().add ("IB05");
		item3.setHeroItemTypeID ("IT03");
		
		final MemoryUnitHeroItemSlot slot3 = new MemoryUnitHeroItemSlot ();
		slot3.setHeroItem (item3);
		slots.add (slot3);		

		// Skill breakdowns
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		modifiedSkillValues.put ("US001", new UnitSkillValueBreakdown (UnitSkillComponent.BASIC));
		modifiedSkillValues.put ("US004", new UnitSkillValueBreakdown (UnitSkillComponent.BASIC));
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		
		// Run method
		utils.addBonusesFromHeroItems (slots, modifiedSkillValues, db);
		
		//  Check results
		assertEquals (3, modifiedSkillValues.size ());
		assertEquals (6, modifiedSkillValues.get ("US001").getComponents ().get (UnitSkillComponent.HERO_ITEMS));		// 2 from basic item stat, 4 from attack type
		assertEquals (3, modifiedSkillValues.get ("US004").getComponents ().get (UnitSkillComponent.HERO_ITEMS));
		assertEquals (1, modifiedSkillValues.get ("US005").getComponents ().get (UnitSkillComponent.HERO_ITEMS));		// Adds a skill we didn't have
	}

	/**
	 * Tests the addPenaltiesFromOtherSkills method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddPenaltiesFromOtherSkills () throws Exception
	{
		// Full skill list
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final AddsToSkill addsToSkill1 = new AddsToSkill (); 
		final UnitSkillEx skillDef1 = new UnitSkillEx ();
		skillDef1.setUnitSkillID ("US001");
		skillDef1.getAddsToSkill ().add (addsToSkill1);

		final AddsToSkill addsToSkill2 = new AddsToSkill (); 
		final UnitSkillEx skillDef2 = new UnitSkillEx ();
		skillDef2.setUnitSkillID ("US002");
		skillDef2.getAddsToSkill ().add (addsToSkill2);
		
		final AddsToSkill addsToSkill3 = new AddsToSkill ();
		addsToSkill3.setAffectsEntireStack (true);
		final UnitSkillEx skillDef3 = new UnitSkillEx ();
		skillDef3.setUnitSkillID ("US003");
		skillDef3.getAddsToSkill ().add (addsToSkill3);

		final AddsToSkill addsToSkill4 = new AddsToSkill ();
		addsToSkill4.setAffectsEntireStack (true);
		final UnitSkillEx skillDef4 = new UnitSkillEx ();
		skillDef4.setUnitSkillID ("US004");
		skillDef4.getAddsToSkill ().add (addsToSkill4);
		
		final AddsToSkill addsToSkill5 = new AddsToSkill ();
		addsToSkill5.setPenaltyToEnemy (true);
		final UnitSkillEx skillDef5 = new UnitSkillEx ();
		skillDef5.setUnitSkillID ("US005");
		skillDef5.getAddsToSkill ().add (addsToSkill5);

		final AddsToSkill addsToSkill6 = new AddsToSkill ();
		addsToSkill6.setPenaltyToEnemy (true);
		final UnitSkillEx skillDef6 = new UnitSkillEx ();
		skillDef6.setUnitSkillID ("US006");
		skillDef6.getAddsToSkill ().add (addsToSkill6);

		when (db.getUnitSkills ()).thenReturn (Arrays.asList (skillDef1, skillDef2, skillDef3, skillDef4, skillDef5, skillDef6));

		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);

		// Skill breakdowns
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		modifiedSkillValues.put ("US001", new UnitSkillValueBreakdown (UnitSkillComponent.BASIC));

		// Other units in the stack
		final Map<String, Integer> unitStackSkills = new HashMap<String, Integer> ();
		unitStackSkills.put ("US003", null);
		
		// Enemy units giving us penalties
		final ExpandedUnitDetails enemyUnit = mock (ExpandedUnitDetails.class);
		when (enemyUnit.hasModifiedSkill ("US005")).thenReturn (true);
		when (enemyUnit.hasModifiedSkill ("US006")).thenReturn (false);
		
		final List<ExpandedUnitDetails> enemyUnits = new ArrayList<ExpandedUnitDetails> ();
		enemyUnits.add (enemyUnit);
		
		// Verify skills being added
		final UnitDetailsUtils unitDetailsUtils = mock (UnitDetailsUtils.class);
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		utils.setUnitDetailsUtils (unitDetailsUtils);
		
		// Run method
		utils.addPenaltiesFromOtherSkills (mu, modifiedSkillValues, unitStackSkills, enemyUnits, null, null, "MB01", db);
		
		// Check results
		verify (unitDetailsUtils, times (1)).addSkillPenalty (mu, addsToSkill1, modifiedSkillValues, null, null, "MB01");
		verify (unitDetailsUtils, times (0)).addSkillPenalty (mu, addsToSkill2, modifiedSkillValues, null, null, "MB01");
		verify (unitDetailsUtils, times (1)).addSkillPenalty (mu, addsToSkill3, modifiedSkillValues, null, null, "MB01");
		verify (unitDetailsUtils, times (0)).addSkillPenalty (mu, addsToSkill4, modifiedSkillValues, null, null, "MB01");
		verify (unitDetailsUtils, times (1)).addSkillPenalty (mu, addsToSkill5, modifiedSkillValues, null, null, "MB01");
		verify (unitDetailsUtils, times (0)).addSkillPenalty (mu, addsToSkill6, modifiedSkillValues, null, null, "MB01");
	}
	
	/**
	 * Tests the buildModifiedUpkeepValues method when the unit has no upkeep to begin with
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testBuildModifiedUpkeepValues_UnitHasNoUpkeep () throws Exception
	{
		// Just for mocks
		final CommonDatabase db = mock (CommonDatabase.class);

		// Basic upkeep values
		final Map<String, Integer> basicUpkeepValues = new HashMap<String, Integer> ();

		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Skill breakdowns
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		
		// Run method
		final Map<String, Integer> modifiedUpkeepValues = utils.buildModifiedUpkeepValues (mu, basicUpkeepValues, modifiedSkillValues, db);
		
		// Check results
		assertEquals (0, modifiedUpkeepValues.size ());
	}

	/**
	 * Tests the buildModifiedUpkeepValues method when the unit has upkeep zeroed out because it is Undead
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testBuildModifiedUpkeepValues_UndeadHaveNoUpkeep () throws Exception
	{
		// Just for mocks
		final CommonDatabase db = mock (CommonDatabase.class);

		// Basic upkeep values
		final Map<String, Integer> basicUpkeepValues = new HashMap<String, Integer> ();
		basicUpkeepValues.put (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, 1);

		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		final UnitType unitType = new UnitType ();
		unitType.setUndeadUpkeepPercentage (0);
		when (mu.getUnitType ()).thenReturn (unitType);
		
		// Skill breakdowns
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		modifiedSkillValues.put (CommonDatabaseConstants.UNIT_SKILL_ID_UNDEAD, null);
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		
		// Run method
		final Map<String, Integer> modifiedUpkeepValues = utils.buildModifiedUpkeepValues (mu, basicUpkeepValues, modifiedSkillValues, db);
		
		// Check results
		assertEquals (0, modifiedUpkeepValues.size ());
	}

	/**
	 * Tests the buildModifiedUpkeepValues method when nothing modifies the upkeep values and they are just directly copied
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testBuildModifiedUpkeepValues_DirectCopy () throws Exception
	{
		// Just for mocks
		final CommonDatabase db = mock (CommonDatabase.class);

		// Basic upkeep values
		final Map<String, Integer> basicUpkeepValues = new HashMap<String, Integer> ();
		basicUpkeepValues.put (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, 1);
		basicUpkeepValues.put (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, 3);

		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Skill breakdowns
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		
		// Run method
		final Map<String, Integer> modifiedUpkeepValues = utils.buildModifiedUpkeepValues (mu, basicUpkeepValues, modifiedSkillValues, db);
		
		// Check results
		assertEquals (2, modifiedUpkeepValues.size ());
		assertEquals (1, modifiedUpkeepValues.get (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS));
		assertEquals (3, modifiedUpkeepValues.get (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD));
	}

	/**
	 * Tests the buildModifiedUpkeepValues method on a Noble hero who donates gold instead of costing it
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testBuildModifiedUpkeepValues_Noble () throws Exception
	{
		// Just for mocks
		final CommonDatabase db = mock (CommonDatabase.class);

		// Basic upkeep values
		final Map<String, Integer> basicUpkeepValues = new HashMap<String, Integer> ();
		basicUpkeepValues.put (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, 1);
		basicUpkeepValues.put (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, 3);

		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Skill breakdowns
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		modifiedSkillValues.put (CommonDatabaseConstants.UNIT_SKILL_ID_NOBLE, null);
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		
		// Run method
		final Map<String, Integer> modifiedUpkeepValues = utils.buildModifiedUpkeepValues (mu, basicUpkeepValues, modifiedSkillValues, db);
		
		// Check results
		assertEquals (2, modifiedUpkeepValues.size ());
		assertEquals (1, modifiedUpkeepValues.get (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS));
		assertEquals (-10, modifiedUpkeepValues.get (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD));
	}

	/**
	 * Tests the buildModifiedUpkeepValues method on an Undead summoned creature where upkeep increases
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testBuildModifiedUpkeepValues_UndeadSummoned () throws Exception
	{
		// Just for mocks
		final CommonDatabase db = mock (CommonDatabase.class);

		// Basic upkeep values
		final Map<String, Integer> basicUpkeepValues = new HashMap<String, Integer> ();
		basicUpkeepValues.put (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, 10);

		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);

		final UnitType unitType = new UnitType ();
		unitType.setUndeadUpkeepPercentage (150);
		when (mu.getUnitType ()).thenReturn (unitType);

		// Skill breakdowns
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		modifiedSkillValues.put (CommonDatabaseConstants.UNIT_SKILL_ID_UNDEAD, null);
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		
		// Run method
		final Map<String, Integer> modifiedUpkeepValues = utils.buildModifiedUpkeepValues (mu, basicUpkeepValues, modifiedSkillValues, db);
		
		// Check results
		assertEquals (1, modifiedUpkeepValues.size ());
		assertEquals (15, modifiedUpkeepValues.get (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA));
	}

	/**
	 * Tests the buildModifiedUpkeepValues method where a retort like Summoner makes upkeep cheaper
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testBuildModifiedUpkeepValues_ReductionFromRetort () throws Exception
	{
		// Just for mocks
		final CommonDatabase db = mock (CommonDatabase.class);

		// Basic upkeep values
		final Map<String, Integer> basicUpkeepValues = new HashMap<String, Integer> ();
		basicUpkeepValues.put (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, 10);

		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);

		final UnitType unitType = new UnitType ();
		unitType.setUnitTypeID ("X");
		when (mu.getUnitType ()).thenReturn (unitType);
		
		// Unit owner
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		final PlayerPublicDetails unitOwner = new PlayerPublicDetails (null, pub, null);
		when (mu.getOwningPlayer ()).thenReturn (unitOwner);
		
		// Reduction from retort
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		when (playerPickUtils.totalProductionBonus (CommonDatabaseConstants.PRODUCTION_TYPE_ID_UNIT_UPKEEP_REDUCTION, "X", pub.getPick (), db)).thenReturn (50);

		// Skill breakdowns
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		utils.setPlayerPickUtils (playerPickUtils);
		
		// Run method
		final Map<String, Integer> modifiedUpkeepValues = utils.buildModifiedUpkeepValues (mu, basicUpkeepValues, modifiedSkillValues, db);
		
		// Check results
		assertEquals (1, modifiedUpkeepValues.size ());
		assertEquals (5, modifiedUpkeepValues.get (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA));
	}
	
	/**
	 * Tests the determineControllingPlayerID method in the normal situation where the unit owner controls it
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineControllingPlayerID_Normal () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		when (mu.getOwningPlayerID ()).thenReturn (1);
		
		// Spells cast on it
		final Map<String, Integer> skillsFromSpellsCastOnThisUnit = new HashMap<String, Integer> ();
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (1, utils.determineControllingPlayerID (mu, skillsFromSpellsCastOnThisUnit));
	}

	/**
	 * Tests the determineControllingPlayerID method when the unit is possessed
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineControllingPlayerID_Possession () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Spells cast on it
		final Map<String, Integer> skillsFromSpellsCastOnThisUnit = new HashMap<String, Integer> ();
		skillsFromSpellsCastOnThisUnit.put (CommonDatabaseConstants.UNIT_SKILL_ID_POSSESSION, 2);
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (2, utils.determineControllingPlayerID (mu, skillsFromSpellsCastOnThisUnit));
	}

	/**
	 * Tests the determineControllingPlayerID method when the unit is confused and rolled to be controlled by the enemy this turn
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineControllingPlayerID_Confusion_CasterControlled () throws Exception
	{
		// Unit we are calculating stats for
		final MemoryUnit unit = new MemoryUnit ();
		unit.setConfusionEffect (ConfusionEffect.CASTER_CONTROLLED);
		
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		when (mu.isMemoryUnit ()).thenReturn (true);
		when (mu.getMemoryUnit ()).thenReturn (unit);
		
		// Spells cast on it
		final Map<String, Integer> skillsFromSpellsCastOnThisUnit = new HashMap<String, Integer> ();
		skillsFromSpellsCastOnThisUnit.put (CommonDatabaseConstants.UNIT_SKILL_ID_CONFUSION, 2);
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (2, utils.determineControllingPlayerID (mu, skillsFromSpellsCastOnThisUnit));
	}

	/**
	 * Tests the determineControllingPlayerID method when the unit is confused and rolled to not be controlled by the enemy this turn
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineControllingPlayerID_Confusion_OtherEffect () throws Exception
	{
		// Unit we are calculating stats for
		final MemoryUnit unit = new MemoryUnit ();
		unit.setConfusionEffect (ConfusionEffect.MOVE_RANDOMLY);
		
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		when (mu.isMemoryUnit ()).thenReturn (true);
		when (mu.getMemoryUnit ()).thenReturn (unit);
		when (mu.getOwningPlayerID ()).thenReturn (1);
		
		// Spells cast on it
		final Map<String, Integer> skillsFromSpellsCastOnThisUnit = new HashMap<String, Integer> ();
		skillsFromSpellsCastOnThisUnit.put (CommonDatabaseConstants.UNIT_SKILL_ID_CONFUSION, 2);
		
		// Set up object to test
		final ExpandUnitDetailsUtilsImpl utils = new ExpandUnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (1, utils.determineControllingPlayerID (mu, skillsFromSpellsCastOnThisUnit));
	}
}