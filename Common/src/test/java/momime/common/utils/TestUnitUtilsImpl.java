package momime.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.common.MomException;
import momime.common.calculations.UnitHasSkillMergedList;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.GenerateTestData;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.Unit;
import momime.common.database.UnitCombatSideID;
import momime.common.database.UnitHasSkill;
import momime.common.database.UnitSkill;
import momime.common.database.UnitSpellEffect;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.PlayerPick;
import momime.common.messages.UnitStatusID;

import org.junit.Test;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the UnitUtils class
 */
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
	@Test(expected=RecordNotFoundException.class)
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
		utils.findUnitURN (4, units, "testFindUnitURN_NotExists");
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
	@Test(expected=RecordNotFoundException.class)
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
		utils.removeUnitURN (4, units);
	}

	/**
	 * Tests the initializeUnitSkills method with no exp and not reading skills from XML
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 */
	@Test
	public final void testInitializeUnitSkills_NoSkills () throws RecordNotFoundException
	{
		final UnitUtilsImpl utils = new UnitUtilsImpl ();

		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);

		assertEquals (GenerateTestData.BARBARIAN_SPEARMEN,
			utils.initializeUnitSkills (unit, -1, GenerateTestData.createDB ()).getUnitID ());

		assertEquals (0, unit.getUnitHasSkill ().size ());
	}

	/**
	 * Tests the initializeUnitSkills method with trying to pass an exp value on a unit type what doesn't gain exp
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 */
	@Test
	public final void testInitializeUnitSkills_ExpOnUnitThatCannotHaveAny () throws RecordNotFoundException
	{
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID (GenerateTestData.MAGIC_SPIRIT_UNIT);

		assertEquals (GenerateTestData.MAGIC_SPIRIT_UNIT,
			utils.initializeUnitSkills (unit, 100, GenerateTestData.createDB ()).getUnitID ());

		assertEquals (0, unit.getUnitHasSkill ().size ());
	}

	/**
	 * Tests the initializeUnitSkills method with no exp and not reading skills from XML
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 */
	@Test
	public final void testInitializeUnitSkills_ExpOnly () throws RecordNotFoundException
	{
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);

		assertEquals (GenerateTestData.BARBARIAN_SPEARMEN,
			utils.initializeUnitSkills (unit, 100, GenerateTestData.createDB ()).getUnitID ());

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
		final UnitUtilsImpl utils = new UnitUtilsImpl ();

		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID (GenerateTestData.DARK_ELF_WARLOCKS);

		assertEquals (GenerateTestData.DARK_ELF_WARLOCKS,
			utils.initializeUnitSkills (unit, -1, GenerateTestData.createDB ()).getUnitID ());

		assertEquals (2, unit.getUnitHasSkill ().size ());
		assertEquals (GenerateTestData.WALKING, unit.getUnitHasSkill ().get (0).getUnitSkillID ());
		assertNull (unit.getUnitHasSkill ().get (0).getUnitSkillValue ());
		assertEquals (GenerateTestData.RANGED_ATTACK_AMMO, unit.getUnitHasSkill ().get (1).getUnitSkillID ());
		assertEquals (4, unit.getUnitHasSkill ().get (1).getUnitSkillValue ().intValue ());
	}

	/**
	 * Tests the initializeUnitSkills method with exp and reading skills from XML
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 */
	@Test
	public final void testInitializeUnitSkills_ExpAndSkills () throws RecordNotFoundException
	{
		final UnitUtilsImpl utils = new UnitUtilsImpl ();

		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID (GenerateTestData.DARK_ELF_WARLOCKS);

		assertEquals (GenerateTestData.DARK_ELF_WARLOCKS,
			utils.initializeUnitSkills (unit, 100, GenerateTestData.createDB ()).getUnitID ());

		assertEquals (3, unit.getUnitHasSkill ().size ());
		assertEquals (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE, unit.getUnitHasSkill ().get (0).getUnitSkillID ());
		assertEquals (100, unit.getUnitHasSkill ().get (0).getUnitSkillValue ().intValue ());
		assertEquals (GenerateTestData.WALKING, unit.getUnitHasSkill ().get (1).getUnitSkillID ());
		assertNull (unit.getUnitHasSkill ().get (1).getUnitSkillValue ());
		assertEquals (GenerateTestData.RANGED_ATTACK_AMMO, unit.getUnitHasSkill ().get (2).getUnitSkillID ());
		assertEquals (4, unit.getUnitHasSkill ().get (2).getUnitSkillValue ().intValue ());
	}

	/**
	 * Tests the getFullFigureCount method
	 */
	@Test
	public final void testGetFullFigureCount ()
	{
		final UnitUtilsImpl utils = new UnitUtilsImpl ();

		final Unit unit = new Unit ();
		unit.setFigureCount (1);
		assertEquals (1, utils.getFullFigureCount (unit));

		unit.setFigureCount (4);
		assertEquals (4, utils.getFullFigureCount (unit));

		// Hydra
		unit.setFigureCount (9);
		assertEquals (1, utils.getFullFigureCount (unit));
	}

	/**
	 * Tests the getBasicSkillValue method
	 */
	@Test
	public final void testGetBasicSkillValue ()
	{
		// Create skills list
		final List<UnitHasSkill> skills = new ArrayList<UnitHasSkill> ();

		final UnitHasSkill skillWithValue = new UnitHasSkill ();
		skillWithValue.setUnitSkillID ("US001");
		skillWithValue.setUnitSkillValue (5);
		skills.add (skillWithValue);

		final UnitHasSkill skillWithoutValue = new UnitHasSkill ();
		skillWithoutValue.setUnitSkillID ("US002");
		skills.add (skillWithoutValue);

		// Test values
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		assertEquals (5, utils.getBasicSkillValue (skills, "US001"));
		assertEquals (0, utils.getBasicSkillValue (skills, "US002"));
		assertEquals (-1, utils.getBasicSkillValue (skills, "US004"));
	}

	/**
	 * Tests the setBasicSkillValue method on a skill that we already have
	 * @throws MomException If this unit didn't previously have the specified skill
	 */
	@Test
	public final void testSetBasicSkillExists () throws MomException
	{
		// Create test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitURN (3);

		// Create skills list
		final UnitHasSkill skillWithValue = new UnitHasSkill ();
		skillWithValue.setUnitSkillID ("US001");
		skillWithValue.setUnitSkillValue (5);
		unit.getUnitHasSkill ().add (skillWithValue);

		final UnitHasSkill skillWithoutValue = new UnitHasSkill ();
		skillWithoutValue.setUnitSkillID ("US002");
		unit.getUnitHasSkill ().add (skillWithoutValue);

		// Run method
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		utils.setBasicSkillValue (unit, "US002", 3);

		// Check results
		assertEquals (2, unit.getUnitHasSkill ().size ());
		assertEquals ("US001", unit.getUnitHasSkill ().get (0).getUnitSkillID ());
		assertEquals (5, unit.getUnitHasSkill ().get (0).getUnitSkillValue ().intValue ());
		assertEquals ("US002", unit.getUnitHasSkill ().get (1).getUnitSkillID ());
		assertEquals (3, unit.getUnitHasSkill ().get (1).getUnitSkillValue ().intValue ());
	}

	/**
	 * Tests the setBasicSkillValue method on a skill that we don't already have
	 * @throws MomException If this unit didn't previously have the specified skill
	 */
	@Test(expected=MomException.class)
	public final void testSetBasicSkillNotExists () throws MomException
	{
		// Create test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitURN (3);

		// Create skills list
		final UnitHasSkill skillWithValue = new UnitHasSkill ();
		skillWithValue.setUnitSkillID ("US001");
		skillWithValue.setUnitSkillValue (5);
		unit.getUnitHasSkill ().add (skillWithValue);

		final UnitHasSkill skillWithoutValue = new UnitHasSkill ();
		skillWithoutValue.setUnitSkillID ("US002");
		unit.getUnitHasSkill ().add (skillWithoutValue);

		// Run method
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		utils.setBasicSkillValue (unit, "US003", 3);
	}

	/**
	 * Tests the describeBasicSkillValuesInDebugString method
	 */
	@Test
	public final void testDescribeBasicSkillValuesInDebugString ()
	{
		// Create test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitURN (3);

		// Create skills list
		final UnitHasSkill skillWithValue = new UnitHasSkill ();
		skillWithValue.setUnitSkillID ("US001");
		skillWithValue.setUnitSkillValue (5);
		unit.getUnitHasSkill ().add (skillWithValue);

		final UnitHasSkill skillWithoutValue = new UnitHasSkill ();
		skillWithoutValue.setUnitSkillID ("US002");
		unit.getUnitHasSkill ().add (skillWithoutValue);

		// Run test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		assertEquals ("5xUS001, US002", utils.describeBasicSkillValuesInDebugString (unit));
	}

	/**
	 * Tests the mergeSpellEffectsIntoSkillList method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testMergeSpellEffectsIntoSkillList () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final UnitSpellEffect unitSpellEffect = new UnitSpellEffect ();
		unitSpellEffect.setUnitSkillID ("US003");
		unitSpellEffect.setUnitSkillValue (7);
		
		final Spell spellDef = new Spell ();
		spellDef.getUnitSpellEffect ().add (unitSpellEffect);
		
		when (db.findSpell ("SP001", "mergeSpellEffectsIntoSkillList")).thenReturn (spellDef);

		// Create test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitURN (3);

		// Create skills list
		final UnitHasSkill skillWithValue = new UnitHasSkill ();
		skillWithValue.setUnitSkillID ("US001");
		skillWithValue.setUnitSkillValue (5);
		unit.getUnitHasSkill ().add (skillWithValue);

		final UnitHasSkill skillWithoutValue = new UnitHasSkill ();
		skillWithoutValue.setUnitSkillID ("US002");
		unit.getUnitHasSkill ().add (skillWithoutValue);

		// Create spells list
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		for (int n = 3; n <= 4; n++)
		{
			final MemoryMaintainedSpell newSpell = new MemoryMaintainedSpell ();
			newSpell.setSpellID ("SP001");
			newSpell.setUnitSkillID ("US00" + n);
			newSpell.setUnitURN (n);
			spells.add (newSpell);
		}

		final MemoryMaintainedSpell nonUnitSpell = new MemoryMaintainedSpell ();
		spells.add (nonUnitSpell);

		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		// Run method
		final UnitHasSkillMergedList mergedSkills = utils.mergeSpellEffectsIntoSkillList (spells, unit, db);

		// Check results
		assertEquals (3, mergedSkills.size ());
		assertEquals ("US001", mergedSkills.get (0).getUnitSkillID ());
		assertEquals (5, mergedSkills.get (0).getUnitSkillValue ().intValue ());
		assertEquals ("US002", mergedSkills.get (1).getUnitSkillID ());
		assertNull (mergedSkills.get (1).getUnitSkillValue ());
		assertEquals ("US003", mergedSkills.get (2).getUnitSkillID ());
		assertEquals (7, mergedSkills.get (2).getUnitSkillValue ().intValue ());
	}

	/**
	 * Tests the getExperienceLevel method with a normal unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws RecordNotFoundException If we can't find the unit, unit type, magic realm or so on
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	@Test
	public final void testGetExperienceLevel_Normal () throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		// Create player
		final MomPersistentPlayerPublicKnowledge pk = new MomPersistentPlayerPublicKnowledge ();
		
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final PlayerPublicDetails player = new PlayerPublicDetails (pd, pk, null);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player);

		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd.getPlayerID (), "getExperienceLevel")).thenReturn (player);
		
		// Create CAEs
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();

		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		utils.setPlayerPickUtils (new PlayerPickUtilsImpl ());
		utils.setMemoryCombatAreaEffectUtils (new MemoryCombatAreaEffectUtilsImpl ());
		utils.setMultiplayerSessionUtils (multiplayerSessionUtils);

		// Create normal unit
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		unit.setOwningPlayerID (1);

		final UnitHasSkill unitExperience = new UnitHasSkill ();
		unitExperience.setUnitSkillID (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE);
		unitExperience.setUnitSkillValue (20);
		unit.getUnitHasSkill ().add (unitExperience);

		assertEquals (2, utils.getExperienceLevel (unit, false, players, combatAreaEffects, GenerateTestData.createDB ()).getLevelNumber ());
		assertEquals (2, utils.getExperienceLevel (unit, true, players, combatAreaEffects, GenerateTestData.createDB ()).getLevelNumber ());

		// Give player warlord
		final PlayerPick warlord = new PlayerPick ();
		warlord.setPickID (CommonDatabaseConstants.RETORT_ID_WARLORD);
		warlord.setQuantity (1);
		pk.getPick ().add (warlord);

		assertEquals (2, utils.getExperienceLevel (unit, false, players, combatAreaEffects, GenerateTestData.createDB ()).getLevelNumber ());
		assertEquals (3, utils.getExperienceLevel (unit, true, players, combatAreaEffects, GenerateTestData.createDB ()).getLevelNumber ());

		// Give player crusade
		final MemoryCombatAreaEffect crusade = new MemoryCombatAreaEffect ();
		crusade.setCastingPlayerID (1);
		crusade.setCombatAreaEffectID (CommonDatabaseConstants.COMBAT_AREA_EFFECT_CRUSADE);
		combatAreaEffects.add (crusade);

		assertEquals (2, utils.getExperienceLevel (unit, false, players, combatAreaEffects, GenerateTestData.createDB ()).getLevelNumber ());
		assertEquals (4, utils.getExperienceLevel (unit, true, players, combatAreaEffects, GenerateTestData.createDB ()).getLevelNumber ());

		// Give it lots of exp - show that 3 is the highest level we can attain through experience
		unitExperience.setUnitSkillValue (100);
		assertEquals (3, utils.getExperienceLevel (unit, false, players, combatAreaEffects, GenerateTestData.createDB ()).getLevelNumber ());
		assertEquals (5, utils.getExperienceLevel (unit, true, players, combatAreaEffects, GenerateTestData.createDB ()).getLevelNumber ());
	}

	/**
	 * Tests the getExperienceLevel method with a hero unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws RecordNotFoundException If we can't find the unit, unit type, magic realm or so on
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	@Test
	public final void testGetExperienceLevel_Hero () throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		// Create player
		final MomPersistentPlayerPublicKnowledge pk = new MomPersistentPlayerPublicKnowledge ();
		
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final PlayerPublicDetails player = new PlayerPublicDetails (pd, pk, null);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player);

		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd.getPlayerID (), "getExperienceLevel")).thenReturn (player);

		// Create CAEs
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();

		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		utils.setPlayerPickUtils (new PlayerPickUtilsImpl ());
		utils.setMemoryCombatAreaEffectUtils (new MemoryCombatAreaEffectUtilsImpl ());
		utils.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		// Create normal unit
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID (GenerateTestData.DWARF_HERO);
		unit.setOwningPlayerID (1);

		final UnitHasSkill unitExperience = new UnitHasSkill ();
		unitExperience.setUnitSkillID (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE);
		unitExperience.setUnitSkillValue (20);
		unit.getUnitHasSkill ().add (unitExperience);

		assertEquals (2, utils.getExperienceLevel (unit, false, players, combatAreaEffects, GenerateTestData.createDB ()).getLevelNumber ());
		assertEquals (2, utils.getExperienceLevel (unit, true, players, combatAreaEffects, GenerateTestData.createDB ()).getLevelNumber ());

		// Give player warlord
		final PlayerPick warlord = new PlayerPick ();
		warlord.setPickID (CommonDatabaseConstants.RETORT_ID_WARLORD);
		warlord.setQuantity (1);
		pk.getPick ().add (warlord);

		assertEquals (2, utils.getExperienceLevel (unit, false, players, combatAreaEffects, GenerateTestData.createDB ()).getLevelNumber ());
		assertEquals (3, utils.getExperienceLevel (unit, true, players, combatAreaEffects, GenerateTestData.createDB ()).getLevelNumber ());

		// Give player crusade
		final MemoryCombatAreaEffect crusade = new MemoryCombatAreaEffect ();
		crusade.setCastingPlayerID (1);
		crusade.setCombatAreaEffectID (CommonDatabaseConstants.COMBAT_AREA_EFFECT_CRUSADE);
		combatAreaEffects.add (crusade);

		assertEquals (2, utils.getExperienceLevel (unit, false, players, combatAreaEffects, GenerateTestData.createDB ()).getLevelNumber ());
		assertEquals (4, utils.getExperienceLevel (unit, true, players, combatAreaEffects, GenerateTestData.createDB ()).getLevelNumber ());

		// Give it lots of exp
		unitExperience.setUnitSkillValue (60);
		assertEquals (6, utils.getExperienceLevel (unit, false, players, combatAreaEffects, GenerateTestData.createDB ()).getLevelNumber ());
		assertEquals (8, utils.getExperienceLevel (unit, true, players, combatAreaEffects, GenerateTestData.createDB ()).getLevelNumber ());

		// Give it even more exp, to prove that we can't get level 9 through warlord+crusade
		unitExperience.setUnitSkillValue (70);
		assertEquals (7, utils.getExperienceLevel (unit, false, players, combatAreaEffects, GenerateTestData.createDB ()).getLevelNumber ());
		assertEquals (8, utils.getExperienceLevel (unit, true, players, combatAreaEffects, GenerateTestData.createDB ()).getLevelNumber ());
	}

	/**
	 * Tests the getExperienceLevel method with a summoned unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws RecordNotFoundException If we can't find the unit, unit type, magic realm or so on
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	@Test
	public final void testGetExperienceLevel_Summoned () throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		// Create player
		final MomPersistentPlayerPublicKnowledge pk = new MomPersistentPlayerPublicKnowledge ();

		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final PlayerPublicDetails player = new PlayerPublicDetails (new PlayerDescription (), pk, null);
		player.getPlayerDescription ().setPlayerID (1);
		players.add (player);

		// Create CAEs
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();

		// Create summoned unit
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID (GenerateTestData.WAR_BEARS_UNIT);
		unit.setOwningPlayerID (1);

		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		assertNull (utils.getExperienceLevel (unit, false, players, combatAreaEffects, GenerateTestData.createDB ()));
		assertNull (utils.getExperienceLevel (unit, true, players, combatAreaEffects, GenerateTestData.createDB ()));

		// Give player warlord, to prove that this doesn't raise the -1 to 0
		final PlayerPick warlord = new PlayerPick ();
		warlord.setPickID (CommonDatabaseConstants.RETORT_ID_WARLORD);
		warlord.setQuantity (1);
		pk.getPick ().add (warlord);

		assertNull (utils.getExperienceLevel (unit, false, players, combatAreaEffects, GenerateTestData.createDB ()));
		assertNull (utils.getExperienceLevel (unit, true, players, combatAreaEffects, GenerateTestData.createDB ()));
	}

	/**
	 * Tests the doesCombatAreaEffectApplyToUnit method on an available unit and a CAE with affects players = blank
	 * @throws RecordNotFoundException If we can't find the definition for the CAE
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Available_AffectsBlank () throws RecordNotFoundException
	{
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		unit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		unit.setOwningPlayerID (1);

		final MemoryCombatAreaEffect effect = new MemoryCombatAreaEffect ();
		effect.setCombatAreaEffectID (GenerateTestData.CAE_AFFECTS_BLANK);
		effect.setCastingPlayerID (1);

		// Even though CAE is global and for the right player, with affects blank it shouldn't apply
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (unit, effect, GenerateTestData.createDB ()));
	}

	/**
	 * Tests the doesCombatAreaEffectApplyToUnit method on an available unit and a CAE with affects players = all
	 * @throws RecordNotFoundException If we can't find the definition for the CAE
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Available_AffectsAll () throws RecordNotFoundException
	{
		final AvailableUnit ourUnit = new AvailableUnit ();
		ourUnit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		ourUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		ourUnit.setOwningPlayerID (1);

		final AvailableUnit theirUnit = new AvailableUnit ();
		theirUnit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		theirUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		theirUnit.setOwningPlayerID (2);

		final MemoryCombatAreaEffect effect = new MemoryCombatAreaEffect ();
		effect.setCombatAreaEffectID (GenerateTestData.CAE_AFFECTS_ALL);
		effect.setCastingPlayerID (1);

		// Global all players CAE should affect all players - don't need to worry about in combat or not, since available units can't be in combat
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));

		// Localise the CAE in the same spot as the units - should still apply
		effect.setMapLocation (new MapCoordinates3DEx (15, 10, 1));
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));

		// Move the CAE to a different spot than the units - should no longer apply
		effect.getMapLocation ().setX (16);
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));
	}

	/**
	 * Tests the doesCombatAreaEffectApplyToUnit method on an available unit and a CAE with affects players = caster
	 * @throws RecordNotFoundException If we can't find the definition for the CAE
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Available_AffectsCaster () throws RecordNotFoundException
	{
		final AvailableUnit ourUnit = new AvailableUnit ();
		ourUnit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		ourUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		ourUnit.setOwningPlayerID (1);

		final AvailableUnit theirUnit = new AvailableUnit ();
		theirUnit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		theirUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		theirUnit.setOwningPlayerID (2);

		final MemoryCombatAreaEffect effect = new MemoryCombatAreaEffect ();
		effect.setCombatAreaEffectID (GenerateTestData.CAE_AFFECTS_CASTER);
		effect.setCastingPlayerID (1);

		// Global caster CAE should affect only the caster
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));

		// Localise the CAE in the same spot as the units - however for Caster Only, this means the units also have to be in combat, which they aren't so it still doesn't apply
		effect.setMapLocation (new MapCoordinates3DEx (15, 10, 1));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));

		// Move the CAE to a different spot than the units - should no longer apply
		effect.getMapLocation ().setX (16);
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));
	}

	/**
	 * Tests the doesCombatAreaEffectApplyToUnit method on an available unit and a CAE with affects players = both
	 * @throws RecordNotFoundException If we can't find the definition for the CAE
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Available_AffectsBothInCombat () throws RecordNotFoundException
	{
		final AvailableUnit ourUnit = new AvailableUnit ();
		ourUnit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		ourUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		ourUnit.setOwningPlayerID (1);

		final AvailableUnit theirUnit = new AvailableUnit ();
		theirUnit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		theirUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		theirUnit.setOwningPlayerID (2);

		final MemoryCombatAreaEffect effect = new MemoryCombatAreaEffect ();
		effect.setCombatAreaEffectID (GenerateTestData.CAE_AFFECTS_BOTH);
		effect.setCastingPlayerID (1);

		// Any settings make no difference, since available units cannot be in combat

		// Global both CAE should affect both combat players, but available units can't be in combat
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));

		// Localise the CAE in the same spot as the units
		effect.setMapLocation (new MapCoordinates3DEx (15, 10, 1));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));

		// Move the CAE to a different spot than the units
		effect.getMapLocation ().setX (16);
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));
	}

	/**
	 * Tests the doesCombatAreaEffectApplyToUnit method on an available unit and a CAE with affects players = opponent
	 * @throws RecordNotFoundException If we can't find the definition for the CAE
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Available_AffectsOpponent () throws RecordNotFoundException
	{
		final AvailableUnit ourUnit = new AvailableUnit ();
		ourUnit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		ourUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		ourUnit.setOwningPlayerID (1);

		final AvailableUnit theirUnit = new AvailableUnit ();
		theirUnit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		theirUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		theirUnit.setOwningPlayerID (2);

		final MemoryCombatAreaEffect effect = new MemoryCombatAreaEffect ();
		effect.setCombatAreaEffectID (GenerateTestData.CAE_AFFECTS_OPPONENT);
		effect.setCastingPlayerID (1);

		// Any settings make no difference, since available units cannot be in combat so there can be no opponent

		// Global opponent CAE should only combat opponent, but available units can't be in combat
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));

		// Localise the CAE in the same spot as the units
		effect.setMapLocation (new MapCoordinates3DEx (15, 10, 1));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));

		// Move the CAE to a different spot than the units
		effect.getMapLocation ().setX (16);
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));
	}

	/**
	 * Tests the doesCombatAreaEffectApplyToUnit method on a real unit and a CAE with affects players = blank
	 * @throws RecordNotFoundException If we can't find the definition for the CAE
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Real_AffectsBlank () throws RecordNotFoundException
	{
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		unit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		unit.setOwningPlayerID (1);

		final MemoryCombatAreaEffect effect = new MemoryCombatAreaEffect ();
		effect.setCombatAreaEffectID (GenerateTestData.CAE_AFFECTS_BLANK);
		effect.setCastingPlayerID (1);

		// Even though CAE is global and for the right player, with affects blank it shouldn't apply
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (unit, effect, GenerateTestData.createDB ()));
	}

	/**
	 * Tests the doesCombatAreaEffectApplyToUnit method on a real unit and a CAE with affects players = all
	 * @throws RecordNotFoundException If we can't find the definition for the CAE
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Real_AffectsAll () throws RecordNotFoundException
	{
		final MemoryUnit ourUnit = new MemoryUnit ();
		ourUnit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		ourUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		ourUnit.setOwningPlayerID (1);

		final MemoryUnit theirUnit = new MemoryUnit ();
		theirUnit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		theirUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		theirUnit.setOwningPlayerID (2);

		final MemoryCombatAreaEffect effect = new MemoryCombatAreaEffect ();
		effect.setCombatAreaEffectID (GenerateTestData.CAE_AFFECTS_ALL);
		effect.setCastingPlayerID (1);

		// Global all CAE should affect all players regardless of location or whether in combat or not
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));

		// Localise the CAE in the same spot as the units - should still apply
		effect.setMapLocation (new MapCoordinates3DEx (15, 10, 1));
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));

		// Move the CAE to a different spot than the units - should no longer apply
		effect.getMapLocation ().setX (16);
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));

		// Put the unit into combat - Note the units are at 15,10,1 but in a combat at 16,10,1 which is the location of the effect, so it should apply
		ourUnit.setCombatLocation (new MapCoordinates3DEx (16, 10, 1));
		ourUnit.setCombatPosition (new MapCoordinates2DEx (5, 6));
		ourUnit.setCombatHeading (1);
		ourUnit.setCombatSide (UnitCombatSideID.ATTACKER);
		
		theirUnit.setCombatLocation (new MapCoordinates3DEx (16, 10, 1));
		theirUnit.setCombatPosition (new MapCoordinates2DEx (15, 12));
		theirUnit.setCombatHeading (5);
		theirUnit.setCombatSide (UnitCombatSideID.DEFENDER);
		
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));

		// Move the CAE away from the combat (note we're moving it to the location the units are actually at) - should no longer apply
		effect.getMapLocation ().setX (15);
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));
	}

	/**
	 * Tests the doesCombatAreaEffectApplyToUnit method on a real unit and a CAE with affects players = caster
	 * @throws RecordNotFoundException If we can't find the definition for the CAE
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Real_AffectsCaster () throws RecordNotFoundException
	{
		final MemoryUnit ourUnit = new MemoryUnit ();
		ourUnit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		ourUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		ourUnit.setOwningPlayerID (1);

		final MemoryUnit theirUnit = new MemoryUnit ();
		theirUnit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		theirUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		theirUnit.setOwningPlayerID (2);

		final MemoryCombatAreaEffect effect = new MemoryCombatAreaEffect ();
		effect.setCombatAreaEffectID (GenerateTestData.CAE_AFFECTS_CASTER);
		effect.setCastingPlayerID (1);

		// Global caster CAE should affect only the caster
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));

		// Localise the CAE in the same spot as the units - however for Caster Only, this means the units also have to be in combat, which they aren't so it still doesn't apply
		effect.setMapLocation (new MapCoordinates3DEx (15, 10, 1));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));

		// Move the CAE to a different spot than the units - should no longer apply
		effect.getMapLocation ().setX (16);
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));

		// Put the unit into combat - Note the units are at 0,0,0 but in a combat at 1,0,0 which is the location of the effect, so it should apply
		ourUnit.setCombatLocation (new MapCoordinates3DEx (16, 10, 1));
		ourUnit.setCombatPosition (new MapCoordinates2DEx (5, 6));
		ourUnit.setCombatHeading (1);
		ourUnit.setCombatSide (UnitCombatSideID.ATTACKER);
		
		theirUnit.setCombatLocation (new MapCoordinates3DEx (16, 10, 1));
		theirUnit.setCombatPosition (new MapCoordinates2DEx (15, 12));
		theirUnit.setCombatHeading (5);
		theirUnit.setCombatSide (UnitCombatSideID.DEFENDER);
		
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));

		// Move the CAE away from the combat (note we're moving it to the location the units are actually at) - should no longer apply
		effect.getMapLocation ().setX (15);
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));
	}

	/**
	 * Tests the doesCombatAreaEffectApplyToUnit method on a real unit and a CAE with affects players = both
	 * @throws RecordNotFoundException If we can't find the definition for the CAE
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Real_AffectsBothInCombat () throws RecordNotFoundException
	{
		final MemoryUnit ourUnit = new MemoryUnit ();
		ourUnit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		ourUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		ourUnit.setOwningPlayerID (1);

		final MemoryUnit theirUnit = new MemoryUnit ();
		theirUnit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		theirUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		theirUnit.setOwningPlayerID (2);

		final MemoryCombatAreaEffect effect = new MemoryCombatAreaEffect ();
		effect.setCombatAreaEffectID (GenerateTestData.CAE_AFFECTS_BOTH);
		effect.setCastingPlayerID (1);

		// Any settings make no difference until we put the unit into combat

		// Global both CAE should affect both combat players, but available units can't be in combat
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));

		// Localise the CAE in the same spot as the units
		effect.setMapLocation (new MapCoordinates3DEx (15, 10, 1));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));

		// Move the CAE to a different spot than the units
		effect.getMapLocation ().setX (16);
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));

		// Put the unit into combat - Note the units are at 0,0,0 but in a combat at 1,0,0 which is the location of the effect, so it should apply
		ourUnit.setCombatLocation (new MapCoordinates3DEx (16, 10, 1));
		ourUnit.setCombatPosition (new MapCoordinates2DEx (5, 6));
		ourUnit.setCombatHeading (1);
		ourUnit.setCombatSide (UnitCombatSideID.ATTACKER);
		
		theirUnit.setCombatLocation (new MapCoordinates3DEx (16, 10, 1));
		theirUnit.setCombatPosition (new MapCoordinates2DEx (15, 12));
		theirUnit.setCombatHeading (5);
		theirUnit.setCombatSide (UnitCombatSideID.DEFENDER);
		
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));

		// Move the CAE away from the combat (note we're moving it to the location the units are actually at) - should no longer apply
		effect.getMapLocation ().setX (15);
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));
	}

	/**
	 * Tests the doesCombatAreaEffectApplyToUnit method on a real unit and a CAE with affects players = opponent
	 * @throws RecordNotFoundException If we can't find the definition for the CAE
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Real_AffectsOpponent () throws RecordNotFoundException
	{
		final MemoryUnit ourUnit = new MemoryUnit ();
		ourUnit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		ourUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		ourUnit.setOwningPlayerID (1);

		final MemoryUnit theirUnit = new MemoryUnit ();
		theirUnit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		theirUnit.setUnitLocation (new MapCoordinates3DEx (15, 10, 1));
		theirUnit.setOwningPlayerID (2);

		final MemoryCombatAreaEffect effect = new MemoryCombatAreaEffect ();
		effect.setCombatAreaEffectID (GenerateTestData.CAE_AFFECTS_OPPONENT);
		effect.setCastingPlayerID (1);

		// Any settings make no difference until we put the unit into combat

		// Global opponent CAE should only combat opponent, but available units can't be in combat
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));

		// Localise the CAE in the same spot as the units
		effect.setMapLocation (new MapCoordinates3DEx (15, 10, 1));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));

		// Move the CAE to a different spot than the units
		effect.getMapLocation ().setX (16);
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));

		// Put the unit into combat - Note the units are at 0,0,0 but in a combat at 1,0,0 which is the location of the effect, so it should apply
		ourUnit.setCombatLocation (new MapCoordinates3DEx (16, 10, 1));
		ourUnit.setCombatPosition (new MapCoordinates2DEx (5, 6));
		ourUnit.setCombatHeading (1);
		ourUnit.setCombatSide (UnitCombatSideID.ATTACKER);
		
		theirUnit.setCombatLocation (new MapCoordinates3DEx (16, 10, 1));
		theirUnit.setCombatPosition (new MapCoordinates2DEx (15, 12));
		theirUnit.setCombatHeading (5);
		theirUnit.setCombatSide (UnitCombatSideID.DEFENDER);
		
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertTrue (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));

		// Move the CAE away from the combat (note we're moving it to the location the units are actually at) - should no longer apply
		effect.getMapLocation ().setX (15);
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB ()));
		assertFalse (utils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB ()));
	}

	/**
	 * Tests the getModifiedUnitMagicRealmLifeformTypeID method on a unit with skills that don't modify its magic realm
	 * @throws RecordNotFoundException If the unit has a skill that we can't find in the cache
	 */
	@Test
	public final void testGetModifiedUnitMagicRealmLifeformTypeID_NoModification () throws RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Unit unitDef = new Unit ();
		unitDef.setUnitMagicRealm ("A");
		when (db.findUnit ("UN001", "getModifiedUnitMagicRealmLifeformTypeID")).thenReturn (unitDef);
		
		final UnitSkill skillDef = new UnitSkill ();
		when (db.findUnitSkill ("US001", "getModifiedUnitMagicRealmLifeformTypeID")).thenReturn (skillDef);
		
		// Set up sample unit
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID ("UN001");

		final UnitHasSkill unitSkill = new UnitHasSkill ();
		unitSkill.setUnitSkillID ("US001");
		unit.getUnitHasSkill ().add (unitSkill);

		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		// Run method
		assertEquals ("A", utils.getModifiedUnitMagicRealmLifeformTypeID (unit, unit.getUnitHasSkill (), spells, db));
	}

	/**
	 * Tests the getModifiedUnitMagicRealmLifeformTypeID method on a unit with a skill that modifies its magic realm
	 * @throws RecordNotFoundException If the unit has a skill that we can't find in the cache
	 */
	@Test
	public final void testGetModifiedUnitMagicRealmLifeformTypeID_ModifiedBySkill () throws RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Unit unitDef = new Unit ();
		unitDef.setUnitMagicRealm ("A");
		when (db.findUnit ("UN001", "getModifiedUnitMagicRealmLifeformTypeID")).thenReturn (unitDef);
		
		final UnitSkill skillDef = new UnitSkill ();
		skillDef.setChangesUnitToMagicRealm ("B");
		when (db.findUnitSkill ("US001", "getModifiedUnitMagicRealmLifeformTypeID")).thenReturn (skillDef);
		
		// Set up sample unit
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID ("UN001");

		final UnitHasSkill unitSkill = new UnitHasSkill ();
		unitSkill.setUnitSkillID ("US001");
		unit.getUnitHasSkill ().add (unitSkill);

		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		// Run method
		assertEquals ("B", utils.getModifiedUnitMagicRealmLifeformTypeID (unit, unit.getUnitHasSkill (), spells, db));
	}

	/**
	 * Tests the getModifiedUnitMagicRealmLifeformTypeID method on a unit with a spell cast on it that gives a skill that modifies its magic realm
	 * @throws RecordNotFoundException If the unit has a skill that we can't find in the cache
	 */
	@Test
	public final void testGetModifiedUnitMagicRealmLifeformTypeID_ModifiedBySpell () throws RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Unit unitDef = new Unit ();
		unitDef.setUnitMagicRealm ("A");
		when (db.findUnit ("UN001", "getModifiedUnitMagicRealmLifeformTypeID")).thenReturn (unitDef);
		
		final UnitSkill skillDef = new UnitSkill ();
		skillDef.setChangesUnitToMagicRealm ("B");
		when (db.findUnitSkill ("US001", "getModifiedUnitMagicRealmLifeformTypeID")).thenReturn (skillDef);
		
		final UnitSpellEffect unitSpellEffect = new UnitSpellEffect ();
		unitSpellEffect.setUnitSkillID ("US001");
		
		final Spell spellDef = new Spell ();
		spellDef.getUnitSpellEffect ().add (unitSpellEffect);
		when (db.findSpell ("SP001", "mergeSpellEffectsIntoSkillList")).thenReturn (spellDef);
		
		// Set up sample unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitURN (1);
		unit.setUnitID ("UN001");

		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		final MemoryMaintainedSpell flight = new MemoryMaintainedSpell ();
		flight.setSpellID ("SP001");
		flight.setUnitSkillID ("US001");
		flight.setUnitURN (1);
		spells.add (flight);
		
		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		// Run method
		assertEquals ("B", utils.getModifiedUnitMagicRealmLifeformTypeID (unit, unit.getUnitHasSkill (), spells, db));
	}

	/**
	 * Tests the getBasicUpkeepValue method
	 * @throws RecordNotFoundException If the unitID doesn't exist
	 */
	@Test
	public final void testGetBasicUpkeepValue () throws RecordNotFoundException
	{
		final AvailableUnit warlocks = new AvailableUnit ();
		warlocks.setUnitID (GenerateTestData.DARK_ELF_WARLOCKS);

		final AvailableUnit stoneGiant = new AvailableUnit ();
		stoneGiant.setUnitID (GenerateTestData.STONE_GIANT_UNIT);

		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		assertEquals (1, utils.getBasicUpkeepValue (warlocks, CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, GenerateTestData.createDB ()));
		assertEquals (5, utils.getBasicUpkeepValue (warlocks, CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, GenerateTestData.createDB ()));
		assertEquals (0, utils.getBasicUpkeepValue (warlocks, CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, GenerateTestData.createDB ()));

		assertEquals (0, utils.getBasicUpkeepValue (stoneGiant, CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, GenerateTestData.createDB ()));
		assertEquals (0, utils.getBasicUpkeepValue (stoneGiant, CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, GenerateTestData.createDB ()));
		assertEquals (9, utils.getBasicUpkeepValue (stoneGiant, CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, GenerateTestData.createDB ()));
	}

	/**
	 * Tests the resetUnitOverlandMovement method for all players
	 * @throws RecordNotFoundException If we can't find the definition for one of the units
	 */
	@Test
	public final void testResetUnitOverlandMovement_AllPlayers ()
		throws RecordNotFoundException
	{
		// Mock some database movement values
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Unit unitA = new Unit ();
		unitA.setDoubleMovement (2);
		when (db.findUnit ("A", "resetUnitOverlandMovement")).thenReturn (unitA);
		
		final Unit unitB = new Unit ();
		unitB.setDoubleMovement (4);
		when (db.findUnit ("B", "resetUnitOverlandMovement")).thenReturn (unitB);
		
		// Set up some test units
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();

		// Create units owned by 3 players
		for (int playerID = 1; playerID <= 3; playerID++)
		{
			final MemoryUnit spearmen = new MemoryUnit ();
			spearmen.setUnitID ("A");
			spearmen.setOwningPlayerID (playerID);
			units.add (spearmen);

			final MemoryUnit hellHounds = new MemoryUnit ();
			hellHounds.setUnitID ("B");
			hellHounds.setOwningPlayerID (playerID);
			units.add (hellHounds);
		}

		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		utils.resetUnitOverlandMovement (units, 0, db);

		assertEquals (2, units.get (0).getDoubleOverlandMovesLeft ());
		assertEquals (4, units.get (1).getDoubleOverlandMovesLeft ());
		assertEquals (2, units.get (2).getDoubleOverlandMovesLeft ());
		assertEquals (4, units.get (3).getDoubleOverlandMovesLeft ());
		assertEquals (2, units.get (4).getDoubleOverlandMovesLeft ());
		assertEquals (4, units.get (5).getDoubleOverlandMovesLeft ());
	}

	/**
	 * Tests the resetUnitOverlandMovement method for a single player
	 * @throws RecordNotFoundException If we can't find the definition for one of the units
	 */
	@Test
	public final void testResetUnitOverlandMovement_OnePlayer ()
		throws RecordNotFoundException
	{
		// Mock some database movement values
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Unit unitA = new Unit ();
		unitA.setDoubleMovement (2);
		when (db.findUnit ("A", "resetUnitOverlandMovement")).thenReturn (unitA);
		
		final Unit unitB = new Unit ();
		unitB.setDoubleMovement (4);
		when (db.findUnit ("B", "resetUnitOverlandMovement")).thenReturn (unitB);
		
		// Set up some test units
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();

		// Create units owned by 3 players
		for (int playerID = 1; playerID <= 3; playerID++)
		{
			final MemoryUnit spearmen = new MemoryUnit ();
			spearmen.setUnitID ("A");
			spearmen.setOwningPlayerID (playerID);
			units.add (spearmen);

			final MemoryUnit hellHounds = new MemoryUnit ();
			hellHounds.setUnitID ("B");
			hellHounds.setOwningPlayerID (playerID);
			units.add (hellHounds);
		}

		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		utils.resetUnitOverlandMovement (units, 2, db);

		assertEquals (0, units.get (0).getDoubleOverlandMovesLeft ());
		assertEquals (0, units.get (1).getDoubleOverlandMovesLeft ());
		assertEquals (2, units.get (2).getDoubleOverlandMovesLeft ());
		assertEquals (4, units.get (3).getDoubleOverlandMovesLeft ());
		assertEquals (0, units.get (4).getDoubleOverlandMovesLeft ());
		assertEquals (0, units.get (5).getDoubleOverlandMovesLeft ());
	}

	/**
	 * Tests the resetUnitCombatMovement method
	 * @throws RecordNotFoundException If we can't find the definition for one of the units
	 */
	@Test
	public final void testResetUnitCombatMovement ()
		throws RecordNotFoundException
	{
		// Mock some database movement values
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Unit unitA = new Unit ();
		unitA.setDoubleMovement (2);
		when (db.findUnit ("A", "resetUnitCombatMovement")).thenReturn (unitA);
		
		final Unit unitB = new Unit ();
		unitB.setDoubleMovement (4);
		when (db.findUnit ("B", "resetUnitCombatMovement")).thenReturn (unitB);
		
		// Set up some test units
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();

		// Unit A that matches
		final MemoryUnit u1 = new MemoryUnit ();
		u1.setStatus (UnitStatusID.ALIVE);
		u1.setUnitID ("A");
		u1.setOwningPlayerID (1);
		u1.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		u1.setCombatPosition (new MapCoordinates2DEx (0, 0));
		u1.setCombatSide (UnitCombatSideID.ATTACKER);
		u1.setCombatHeading (1);
		units.add (u1);

		// Wrong location
		final MemoryUnit u2 = new MemoryUnit ();
		u2.setStatus (UnitStatusID.ALIVE);
		u2.setUnitID ("A");
		u2.setOwningPlayerID (1);
		u2.setCombatLocation (new MapCoordinates3DEx (21, 10, 1));
		u2.setCombatPosition (new MapCoordinates2DEx (0, 0));
		u2.setCombatSide (UnitCombatSideID.ATTACKER);
		u2.setCombatHeading (1);
		units.add (u2);

		// No combat position
		final MemoryUnit u3 = new MemoryUnit ();
		u3.setStatus (UnitStatusID.ALIVE);
		u3.setUnitID ("A");
		u3.setOwningPlayerID (1);
		u3.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		u3.setCombatSide (UnitCombatSideID.ATTACKER);
		u3.setCombatHeading (1);
		units.add (u3);
		
		// Wrong player
		final MemoryUnit u4 = new MemoryUnit ();
		u4.setStatus (UnitStatusID.ALIVE);
		u4.setUnitID ("A");
		u4.setOwningPlayerID (2);
		u4.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		u4.setCombatPosition (new MapCoordinates2DEx (0, 0));
		u4.setCombatSide (UnitCombatSideID.ATTACKER);
		u4.setCombatHeading (1);
		units.add (u4);
		
		// Unit B that matches
		final MemoryUnit u5 = new MemoryUnit ();
		u5.setStatus (UnitStatusID.ALIVE);
		u5.setUnitID ("B");
		u5.setOwningPlayerID (1);
		u5.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		u5.setCombatPosition (new MapCoordinates2DEx (0, 0));
		u5.setCombatSide (UnitCombatSideID.ATTACKER);
		u5.setCombatHeading (1);
		units.add (u5);
		
		// Set up object to test
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		// Run method
		final MapCoordinates3DEx loc = new MapCoordinates3DEx (20, 10, 1);
		utils.resetUnitCombatMovement (units, 1, loc, db);

		// Check results
		assertEquals (2, u1.getDoubleCombatMovesLeft ().intValue ());
		assertNull (u2.getDoubleCombatMovesLeft ());
		assertNull (u3.getDoubleCombatMovesLeft ());
		assertNull (u4.getDoubleCombatMovesLeft ());
		assertEquals (4, u5.getDoubleCombatMovesLeft ().intValue ());
	}

	/**
	 * Tests the listUnitURNs method
	 */
	@Test
	public final void testListUnitURNs ()
	{
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		
		// Test on null list
		assertEquals ("()", utils.listUnitURNs (null));

		// Test on list with single unit
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();
		final MemoryUnit one = new MemoryUnit ();
		one.setUnitURN (1);
		units.add (one);

		assertEquals ("(1)", utils.listUnitURNs (units));

		// Test on list with multiple units
		final MemoryUnit five = new MemoryUnit ();
		five.setUnitURN (5);
		units.add (five);

		final MemoryUnit three = new MemoryUnit ();
		three.setUnitURN (3);
		units.add (three);

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

		assertEquals (u6, utils.findFirstAliveEnemyAtLocation (units, 2, 3, 1, 4));
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
	 * Tests the beforeKillingUnit method
	 */
	@Test
	public final void testBeforeKillingUnit ()
	{
		// Set up test data
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MemoryMaintainedSpell wrongUnit = new MemoryMaintainedSpell ();
		wrongUnit.setUnitURN (6);
		mem.getMaintainedSpell ().add (wrongUnit);

		final MemoryMaintainedSpell rightUnit = new MemoryMaintainedSpell ();
		rightUnit.setUnitURN (5);
		mem.getMaintainedSpell ().add (rightUnit);

		final MemoryMaintainedSpell noUnit = new MemoryMaintainedSpell ();
		mem.getMaintainedSpell ().add (noUnit);
		
		// Run test
		new UnitUtilsImpl ().beforeKillingUnit (mem, 5);
		
		// Check results
		assertEquals (2, mem.getMaintainedSpell ().size ());
		assertSame (wrongUnit, mem.getMaintainedSpell ().get (0));
		assertSame (noUnit, mem.getMaintainedSpell ().get (1));
	}
	
	/**
	 * Tests the findAliveUnitInCombatAt method
	 */
	@Test
	public final void testFindAliveUnitInCombatAt ()
	{
		// Put into a list units that meet every criteria except one
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();

		// Unit is dead
		final MemoryUnit u1 = new MemoryUnit ();
		u1.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		u1.setCombatPosition (new MapCoordinates2DEx (14, 7));
		u1.setCombatSide (UnitCombatSideID.ATTACKER);
		u1.setCombatHeading (1);
		u1.setStatus (UnitStatusID.DEAD);
		
		units.add (u1);
		
		// Wrong combat location
		final MemoryUnit u2 = new MemoryUnit ();
		u2.setCombatLocation (new MapCoordinates3DEx (21, 10, 1));
		u2.setCombatPosition (new MapCoordinates2DEx (14, 7));
		u2.setCombatSide (UnitCombatSideID.ATTACKER);
		u2.setCombatHeading (1);
		u2.setStatus (UnitStatusID.ALIVE);
		
		units.add (u2);
		
		// Wrong combat position
		final MemoryUnit u3 = new MemoryUnit ();
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
		assertNull (utils.findAliveUnitInCombatAt (units, loc, pos));
		
		// Add one that matches
		final MemoryUnit u4 = new MemoryUnit ();
		u4.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		u4.setCombatPosition (new MapCoordinates2DEx (14, 7));
		u4.setCombatSide (UnitCombatSideID.ATTACKER);
		u4.setCombatHeading (1);
		u4.setStatus (UnitStatusID.ALIVE);
		
		units.add (u4);
		
		// Show that we find it
		assertSame (u4, utils.findAliveUnitInCombatAt (units, loc, pos));
	}
}