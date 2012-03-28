package momime.common.messages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import momime.common.MomException;
import momime.common.calculations.UnitHasSkillMergedList;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.GenerateTestData;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.Unit;
import momime.common.database.v0_9_4.UnitHasSkill;
import momime.common.messages.v0_9_4.AvailableUnit;
import momime.common.messages.v0_9_4.MemoryCombatAreaEffect;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.MomTransientPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.common.messages.v0_9_4.PlayerPick;
import momime.common.messages.v0_9_4.UnitStatusID;

import org.junit.Test;

import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the UnitUtils class
 */
public final class TestUnitUtils
{
	/** Dummy logger to use during unit tests */
	private final Logger debugLogger = Logger.getLogger ("MoMIMECommonUnitTests");

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

		assertEquals (2, UnitUtils.findUnitURN (2, units, debugLogger).getUnitURN ());
		assertEquals (2, UnitUtils.findUnitURN (2, units, "testFindUnitURN_Exists", debugLogger).getUnitURN ());
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

		assertNull (UnitUtils.findUnitURN (4, units, debugLogger));
		UnitUtils.findUnitURN (4, units, "testFindUnitURN_NotExists", debugLogger);
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

		UnitUtils.removeUnitURN (2, units, debugLogger);
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

		UnitUtils.removeUnitURN (4, units, debugLogger);
	}

	/**
	 * Tests the initializeUnitSkills method with no exp and not reading skills from XML
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 */
	@Test
	public final void testInitializeUnitSkills_NoSkills () throws RecordNotFoundException
	{
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);

		assertEquals (GenerateTestData.BARBARIAN_SPEARMEN,
			UnitUtils.initializeUnitSkills (unit, -1, false, GenerateTestData.createDB (), debugLogger).getUnitID ());

		assertEquals (0, unit.getUnitHasSkill ().size ());
	}

	/**
	 * Tests the initializeUnitSkills method with trying to pass an exp value on a unit type what doesn't gain exp
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 */
	@Test
	public final void testInitializeUnitSkills_ExpOnUnitThatCannotHaveAny () throws RecordNotFoundException
	{
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID (GenerateTestData.MAGIC_SPIRIT_UNIT);

		assertEquals (GenerateTestData.MAGIC_SPIRIT_UNIT,
			UnitUtils.initializeUnitSkills (unit, 100, false, GenerateTestData.createDB (), debugLogger).getUnitID ());

		assertEquals (0, unit.getUnitHasSkill ().size ());
	}

	/**
	 * Tests the initializeUnitSkills method with no exp and not reading skills from XML
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 */
	@Test
	public final void testInitializeUnitSkills_ExpOnly () throws RecordNotFoundException
	{
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);

		assertEquals (GenerateTestData.BARBARIAN_SPEARMEN,
			UnitUtils.initializeUnitSkills (unit, 100, false, GenerateTestData.createDB (), debugLogger).getUnitID ());

		assertEquals (1, unit.getUnitHasSkill ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE, unit.getUnitHasSkill ().get (0).getUnitSkillID ());
		assertEquals (100, unit.getUnitHasSkill ().get (0).getUnitSkillValue ().intValue ());
	}

	/**
	 * Tests the initializeUnitSkills method with no exp, but reading skills from XML
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 */
	@Test
	public final void testInitializeUnitSkills_SkillsOnly () throws RecordNotFoundException
	{
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID (GenerateTestData.DARK_ELF_WARLOCKS);

		assertEquals (GenerateTestData.DARK_ELF_WARLOCKS,
			UnitUtils.initializeUnitSkills (unit, -1, true, GenerateTestData.createDB (), debugLogger).getUnitID ());

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
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID (GenerateTestData.DARK_ELF_WARLOCKS);

		assertEquals (GenerateTestData.DARK_ELF_WARLOCKS,
			UnitUtils.initializeUnitSkills (unit, 100, true, GenerateTestData.createDB (), debugLogger).getUnitID ());

		assertEquals (3, unit.getUnitHasSkill ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE, unit.getUnitHasSkill ().get (0).getUnitSkillID ());
		assertEquals (100, unit.getUnitHasSkill ().get (0).getUnitSkillValue ().intValue ());
		assertEquals (GenerateTestData.WALKING, unit.getUnitHasSkill ().get (1).getUnitSkillID ());
		assertNull (unit.getUnitHasSkill ().get (1).getUnitSkillValue ());
		assertEquals (GenerateTestData.RANGED_ATTACK_AMMO, unit.getUnitHasSkill ().get (2).getUnitSkillID ());
		assertEquals (4, unit.getUnitHasSkill ().get (2).getUnitSkillValue ().intValue ());
	}

	/**
	 * Tests the createMemoryUnit method
	 * We don't really need to test all combinations of params, since that just affects the call to initializeUnitSkills, which we've already tested above
	 *
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 */
	@Test
	public final void testCreateMemoryUnit () throws RecordNotFoundException
	{
		final MemoryUnit unit = UnitUtils.createMemoryUnit (GenerateTestData.DARK_ELF_WARLOCKS, 1, 3, 100, true, GenerateTestData.createDB (), debugLogger);

		assertEquals (3, unit.getUnitHasSkill ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE, unit.getUnitHasSkill ().get (0).getUnitSkillID ());
		assertEquals (100, unit.getUnitHasSkill ().get (0).getUnitSkillValue ().intValue ());
		assertEquals (GenerateTestData.WALKING, unit.getUnitHasSkill ().get (1).getUnitSkillID ());
		assertNull (unit.getUnitHasSkill ().get (1).getUnitSkillValue ());
		assertEquals (GenerateTestData.RANGED_ATTACK_AMMO, unit.getUnitHasSkill ().get (2).getUnitSkillID ());
		assertEquals (4, unit.getUnitHasSkill ().get (2).getUnitSkillValue ().intValue ());

		assertEquals (2, unit.getDoubleOverlandMovesLeft ());
		assertEquals (3, unit.getWeaponGrade ().intValue ());
		assertEquals (UnitStatusID.ALIVE, unit.getStatus ());
	}

	/**
	 * Tests the getFullFigureCount method
	 */
	@Test
	public final void testGetFullFigureCount ()
	{
		final Unit unit = new Unit ();
		unit.setFigureCount (1);
		assertEquals (1, UnitUtils.getFullFigureCount (unit));

		unit.setFigureCount (4);
		assertEquals (4, UnitUtils.getFullFigureCount (unit));

		// Hydra
		unit.setFigureCount (9);
		assertEquals (1, UnitUtils.getFullFigureCount (unit));
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
		assertEquals (5, UnitUtils.getBasicSkillValue (skills, "US001"));
		assertEquals (0, UnitUtils.getBasicSkillValue (skills, "US002"));
		assertEquals (-1, UnitUtils.getBasicSkillValue (skills, "US004"));
	}

	/**
	 * Tests the setBasicSkillValue method on a skill that we already have
	 * @throws MomException If this unit didn't previously have the specified skill
	 */
	@Test
	public final void testSetBasicSkillValue_Exists () throws MomException
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
		UnitUtils.setBasicSkillValue (unit, "US002", 3, debugLogger);

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
	public final void testSetBasicSkillValue_NotExists () throws MomException
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
		UnitUtils.setBasicSkillValue (unit, "US003", 3, debugLogger);
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
		assertEquals ("5xUS001, US002", UnitUtils.describeBasicSkillValuesInDebugString (unit));
	}

	/**
	 * Tests the mergeSpellEffectsIntoSkillList method
	 */
	@Test
	public final void testMergeSpellEffectsIntoSkillList ()
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

		// Create spells list
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		for (int n = 3; n <= 4; n++)
		{
			final MemoryMaintainedSpell newSpell = new MemoryMaintainedSpell ();
			newSpell.setUnitSkillID ("US00" + n);
			newSpell.setUnitURN (n);
			spells.add (newSpell);
		}

		final MemoryMaintainedSpell nonUnitSpell = new MemoryMaintainedSpell ();
		spells.add (nonUnitSpell);

		// Merge in spell skills
		final UnitHasSkillMergedList mergedSkills = UnitUtils.mergeSpellEffectsIntoSkillList (spells, unit, debugLogger);

		// Test values
		assertEquals (5, UnitUtils.getBasicSkillValue (mergedSkills, "US001"));
		assertEquals (0, UnitUtils.getBasicSkillValue (mergedSkills, "US002"));
		assertEquals (0, UnitUtils.getBasicSkillValue (mergedSkills, "US003"));		// granted from spell
		assertEquals (-1, UnitUtils.getBasicSkillValue (mergedSkills, "US004"));		// not granted from spell because wrong unit URN
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

		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final PlayerPublicDetails player = new PlayerPublicDetails (new PlayerDescription (), pk, null);
		player.getPlayerDescription ().setPlayerID (1);
		players.add (player);

		// Create CAEs
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();

		// Create normal unit
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		unit.setOwningPlayerID (1);

		final UnitHasSkill unitExperience = new UnitHasSkill ();
		unitExperience.setUnitSkillID (CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE);
		unitExperience.setUnitSkillValue (20);
		unit.getUnitHasSkill ().add (unitExperience);

		assertEquals (2, UnitUtils.getExperienceLevel (unit, false, players, combatAreaEffects, GenerateTestData.createDB (), debugLogger).getLevelNumber ());
		assertEquals (2, UnitUtils.getExperienceLevel (unit, true, players, combatAreaEffects, GenerateTestData.createDB (), debugLogger).getLevelNumber ());

		// Give player warlord
		final PlayerPick warlord = new PlayerPick ();
		warlord.setPickID (CommonDatabaseConstants.VALUE_RETORT_ID_WARLORD);
		warlord.setQuantity (1);
		pk.getPick ().add (warlord);

		assertEquals (2, UnitUtils.getExperienceLevel (unit, false, players, combatAreaEffects, GenerateTestData.createDB (), debugLogger).getLevelNumber ());
		assertEquals (3, UnitUtils.getExperienceLevel (unit, true, players, combatAreaEffects, GenerateTestData.createDB (), debugLogger).getLevelNumber ());

		// Give player crusade
		final MemoryCombatAreaEffect crusade = new MemoryCombatAreaEffect ();
		crusade.setCastingPlayerID (1);
		crusade.setCombatAreaEffectID (CommonDatabaseConstants.COMBAT_AREA_EFFECT_CRUSADE);
		combatAreaEffects.add (crusade);

		assertEquals (2, UnitUtils.getExperienceLevel (unit, false, players, combatAreaEffects, GenerateTestData.createDB (), debugLogger).getLevelNumber ());
		assertEquals (4, UnitUtils.getExperienceLevel (unit, true, players, combatAreaEffects, GenerateTestData.createDB (), debugLogger).getLevelNumber ());

		// Give it lots of exp - show that 3 is the highest level we can attain through experience
		unitExperience.setUnitSkillValue (100);
		assertEquals (3, UnitUtils.getExperienceLevel (unit, false, players, combatAreaEffects, GenerateTestData.createDB (), debugLogger).getLevelNumber ());
		assertEquals (5, UnitUtils.getExperienceLevel (unit, true, players, combatAreaEffects, GenerateTestData.createDB (), debugLogger).getLevelNumber ());
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

		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final PlayerPublicDetails player = new PlayerPublicDetails (new PlayerDescription (), pk, null);
		player.getPlayerDescription ().setPlayerID (1);
		players.add (player);

		// Create CAEs
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();

		// Create normal unit
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID (GenerateTestData.DWARF_HERO);
		unit.setOwningPlayerID (1);

		final UnitHasSkill unitExperience = new UnitHasSkill ();
		unitExperience.setUnitSkillID (CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE);
		unitExperience.setUnitSkillValue (20);
		unit.getUnitHasSkill ().add (unitExperience);

		assertEquals (2, UnitUtils.getExperienceLevel (unit, false, players, combatAreaEffects, GenerateTestData.createDB (), debugLogger).getLevelNumber ());
		assertEquals (2, UnitUtils.getExperienceLevel (unit, true, players, combatAreaEffects, GenerateTestData.createDB (), debugLogger).getLevelNumber ());

		// Give player warlord
		final PlayerPick warlord = new PlayerPick ();
		warlord.setPickID (CommonDatabaseConstants.VALUE_RETORT_ID_WARLORD);
		warlord.setQuantity (1);
		pk.getPick ().add (warlord);

		assertEquals (2, UnitUtils.getExperienceLevel (unit, false, players, combatAreaEffects, GenerateTestData.createDB (), debugLogger).getLevelNumber ());
		assertEquals (3, UnitUtils.getExperienceLevel (unit, true, players, combatAreaEffects, GenerateTestData.createDB (), debugLogger).getLevelNumber ());

		// Give player crusade
		final MemoryCombatAreaEffect crusade = new MemoryCombatAreaEffect ();
		crusade.setCastingPlayerID (1);
		crusade.setCombatAreaEffectID (CommonDatabaseConstants.COMBAT_AREA_EFFECT_CRUSADE);
		combatAreaEffects.add (crusade);

		assertEquals (2, UnitUtils.getExperienceLevel (unit, false, players, combatAreaEffects, GenerateTestData.createDB (), debugLogger).getLevelNumber ());
		assertEquals (4, UnitUtils.getExperienceLevel (unit, true, players, combatAreaEffects, GenerateTestData.createDB (), debugLogger).getLevelNumber ());

		// Give it lots of exp
		unitExperience.setUnitSkillValue (60);
		assertEquals (6, UnitUtils.getExperienceLevel (unit, false, players, combatAreaEffects, GenerateTestData.createDB (), debugLogger).getLevelNumber ());
		assertEquals (8, UnitUtils.getExperienceLevel (unit, true, players, combatAreaEffects, GenerateTestData.createDB (), debugLogger).getLevelNumber ());

		// Give it even more exp, to prove that we can't get level 9 through warlord+crusade
		unitExperience.setUnitSkillValue (70);
		assertEquals (7, UnitUtils.getExperienceLevel (unit, false, players, combatAreaEffects, GenerateTestData.createDB (), debugLogger).getLevelNumber ());
		assertEquals (8, UnitUtils.getExperienceLevel (unit, true, players, combatAreaEffects, GenerateTestData.createDB (), debugLogger).getLevelNumber ());
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

		assertNull (UnitUtils.getExperienceLevel (unit, false, players, combatAreaEffects, GenerateTestData.createDB (), debugLogger));
		assertNull (UnitUtils.getExperienceLevel (unit, true, players, combatAreaEffects, GenerateTestData.createDB (), debugLogger));

		// Give player warlord, to prove that this doesn't raise the -1 to 0
		final PlayerPick warlord = new PlayerPick ();
		warlord.setPickID (CommonDatabaseConstants.VALUE_RETORT_ID_WARLORD);
		warlord.setQuantity (1);
		pk.getPick ().add (warlord);

		assertNull (UnitUtils.getExperienceLevel (unit, false, players, combatAreaEffects, GenerateTestData.createDB (), debugLogger));
		assertNull (UnitUtils.getExperienceLevel (unit, true, players, combatAreaEffects, GenerateTestData.createDB (), debugLogger));
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
		unit.setUnitLocation (new OverlandMapCoordinates ());
		unit.setOwningPlayerID (1);

		final MemoryCombatAreaEffect effect = new MemoryCombatAreaEffect ();
		effect.setCombatAreaEffectID (GenerateTestData.CAE_AFFECTS_BLANK);
		effect.setCastingPlayerID (1);

		// Even though CAE is global and for the right player, with affects blank it shouldn't apply
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (unit, effect, GenerateTestData.createDB (), debugLogger));
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
		ourUnit.setUnitLocation (new OverlandMapCoordinates ());
		ourUnit.setOwningPlayerID (1);

		final AvailableUnit theirUnit = new AvailableUnit ();
		theirUnit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		theirUnit.setUnitLocation (new OverlandMapCoordinates ());
		theirUnit.setOwningPlayerID (2);

		final MemoryCombatAreaEffect effect = new MemoryCombatAreaEffect ();
		effect.setCombatAreaEffectID (GenerateTestData.CAE_AFFECTS_ALL);
		effect.setCastingPlayerID (1);

		// Global all players CAE should affect all players - don't need to worry about in combat or not, since available units can't be in combat
		assertTrue (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertTrue (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));

		// Localise the CAE in the same spot as the units - should still apply
		effect.setMapLocation (new OverlandMapCoordinates ());
		assertTrue (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertTrue (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));

		// Move the CAE to a different spot than the units - should no longer apply
		effect.getMapLocation ().setX (1);
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));
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
		ourUnit.setUnitLocation (new OverlandMapCoordinates ());
		ourUnit.setOwningPlayerID (1);

		final AvailableUnit theirUnit = new AvailableUnit ();
		theirUnit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		theirUnit.setUnitLocation (new OverlandMapCoordinates ());
		theirUnit.setOwningPlayerID (2);

		final MemoryCombatAreaEffect effect = new MemoryCombatAreaEffect ();
		effect.setCombatAreaEffectID (GenerateTestData.CAE_AFFECTS_CASTER);
		effect.setCastingPlayerID (1);

		// Global caster CAE should affect only the caster
		assertTrue (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));

		// Localise the CAE in the same spot as the units - however for Caster Only, this means the units also have to be in combat, which they aren't so it still doesn't apply
		effect.setMapLocation (new OverlandMapCoordinates ());
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));

		// Move the CAE to a different spot than the units - should no longer apply
		effect.getMapLocation ().setX (1);
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));
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
		ourUnit.setUnitLocation (new OverlandMapCoordinates ());
		ourUnit.setOwningPlayerID (1);

		final AvailableUnit theirUnit = new AvailableUnit ();
		theirUnit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		theirUnit.setUnitLocation (new OverlandMapCoordinates ());
		theirUnit.setOwningPlayerID (2);

		final MemoryCombatAreaEffect effect = new MemoryCombatAreaEffect ();
		effect.setCombatAreaEffectID (GenerateTestData.CAE_AFFECTS_BOTH);
		effect.setCastingPlayerID (1);

		// Any settings make no difference, since available units cannot be in combat

		// Global both CAE should affect both combat players, but available units can't be in combat
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));

		// Localise the CAE in the same spot as the units
		effect.setMapLocation (new OverlandMapCoordinates ());
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));

		// Move the CAE to a different spot than the units
		effect.getMapLocation ().setX (1);
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));
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
		ourUnit.setUnitLocation (new OverlandMapCoordinates ());
		ourUnit.setOwningPlayerID (1);

		final AvailableUnit theirUnit = new AvailableUnit ();
		theirUnit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		theirUnit.setUnitLocation (new OverlandMapCoordinates ());
		theirUnit.setOwningPlayerID (2);

		final MemoryCombatAreaEffect effect = new MemoryCombatAreaEffect ();
		effect.setCombatAreaEffectID (GenerateTestData.CAE_AFFECTS_OPPONENT);
		effect.setCastingPlayerID (1);

		// Any settings make no difference, since available units cannot be in combat so there can be no opponent

		// Global opponent CAE should only combat opponent, but available units can't be in combat
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));

		// Localise the CAE in the same spot as the units
		effect.setMapLocation (new OverlandMapCoordinates ());
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));

		// Move the CAE to a different spot than the units
		effect.getMapLocation ().setX (1);
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));
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
		unit.setUnitLocation (new OverlandMapCoordinates ());
		unit.setOwningPlayerID (1);

		final MemoryCombatAreaEffect effect = new MemoryCombatAreaEffect ();
		effect.setCombatAreaEffectID (GenerateTestData.CAE_AFFECTS_BLANK);
		effect.setCastingPlayerID (1);

		// Even though CAE is global and for the right player, with affects blank it shouldn't apply
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (unit, effect, GenerateTestData.createDB (), debugLogger));
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
		ourUnit.setUnitLocation (new OverlandMapCoordinates ());
		ourUnit.setOwningPlayerID (1);

		final MemoryUnit theirUnit = new MemoryUnit ();
		theirUnit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		theirUnit.setUnitLocation (new OverlandMapCoordinates ());
		theirUnit.setOwningPlayerID (2);

		final MemoryCombatAreaEffect effect = new MemoryCombatAreaEffect ();
		effect.setCombatAreaEffectID (GenerateTestData.CAE_AFFECTS_ALL);
		effect.setCastingPlayerID (1);

		// Global all CAE should affect all players regardless of location or whether in combat or not
		assertTrue (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertTrue (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));

		// Localise the CAE in the same spot as the units - should still apply
		effect.setMapLocation (new OverlandMapCoordinates ());
		assertTrue (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertTrue (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));

		// Move the CAE to a different spot than the units - should no longer apply
		effect.getMapLocation ().setX (1);
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));

		// Put the unit into combat - Note the units are at 0,0,0 but in a combat at 1,0,0 which is the location of the effect, so it should apply
		ourUnit.setCombatLocation (new OverlandMapCoordinates ());
		ourUnit.getCombatLocation ().setX (1);
		theirUnit.setCombatLocation (new OverlandMapCoordinates ());
		theirUnit.getCombatLocation ().setX (1);
		assertTrue (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertTrue (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));

		// Move the CAE away from the combat (note we're moving it to the location the units are actually at) - should no longer apply
		effect.getMapLocation ().setX (0);
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));
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
		ourUnit.setUnitLocation (new OverlandMapCoordinates ());
		ourUnit.setOwningPlayerID (1);

		final MemoryUnit theirUnit = new MemoryUnit ();
		theirUnit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		theirUnit.setUnitLocation (new OverlandMapCoordinates ());
		theirUnit.setOwningPlayerID (2);

		final MemoryCombatAreaEffect effect = new MemoryCombatAreaEffect ();
		effect.setCombatAreaEffectID (GenerateTestData.CAE_AFFECTS_CASTER);
		effect.setCastingPlayerID (1);

		// Global caster CAE should affect only the caster
		assertTrue (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));

		// Localise the CAE in the same spot as the units - however for Caster Only, this means the units also have to be in combat, which they aren't so it still doesn't apply
		effect.setMapLocation (new OverlandMapCoordinates ());
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));

		// Move the CAE to a different spot than the units - should no longer apply
		effect.getMapLocation ().setX (1);
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));

		// Put the unit into combat - Note the units are at 0,0,0 but in a combat at 1,0,0 which is the location of the effect, so it should apply
		ourUnit.setCombatLocation (new OverlandMapCoordinates ());
		ourUnit.getCombatLocation ().setX (1);
		theirUnit.setCombatLocation (new OverlandMapCoordinates ());
		theirUnit.getCombatLocation ().setX (1);
		assertTrue (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));

		// Move the CAE away from the combat (note we're moving it to the location the units are actually at) - should no longer apply
		effect.getMapLocation ().setX (0);
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));
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
		ourUnit.setUnitLocation (new OverlandMapCoordinates ());
		ourUnit.setOwningPlayerID (1);

		final MemoryUnit theirUnit = new MemoryUnit ();
		theirUnit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		theirUnit.setUnitLocation (new OverlandMapCoordinates ());
		theirUnit.setOwningPlayerID (2);

		final MemoryCombatAreaEffect effect = new MemoryCombatAreaEffect ();
		effect.setCombatAreaEffectID (GenerateTestData.CAE_AFFECTS_BOTH);
		effect.setCastingPlayerID (1);

		// Any settings make no difference until we put the unit into combat

		// Global both CAE should affect both combat players, but available units can't be in combat
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));

		// Localise the CAE in the same spot as the units
		effect.setMapLocation (new OverlandMapCoordinates ());
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));

		// Move the CAE to a different spot than the units
		effect.getMapLocation ().setX (1);
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));

		// Put the unit into combat - Note the units are at 0,0,0 but in a combat at 1,0,0 which is the location of the effect, so it should apply
		ourUnit.setCombatLocation (new OverlandMapCoordinates ());
		ourUnit.getCombatLocation ().setX (1);
		theirUnit.setCombatLocation (new OverlandMapCoordinates ());
		theirUnit.getCombatLocation ().setX (1);
		assertTrue (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertTrue (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));

		// Move the CAE away from the combat (note we're moving it to the location the units are actually at) - should no longer apply
		effect.getMapLocation ().setX (0);
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));
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
		ourUnit.setUnitLocation (new OverlandMapCoordinates ());
		ourUnit.setOwningPlayerID (1);

		final MemoryUnit theirUnit = new MemoryUnit ();
		theirUnit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		theirUnit.setUnitLocation (new OverlandMapCoordinates ());
		theirUnit.setOwningPlayerID (2);

		final MemoryCombatAreaEffect effect = new MemoryCombatAreaEffect ();
		effect.setCombatAreaEffectID (GenerateTestData.CAE_AFFECTS_OPPONENT);
		effect.setCastingPlayerID (1);

		// Any settings make no difference until we put the unit into combat

		// Global opponent CAE should only combat opponent, but available units can't be in combat
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));

		// Localise the CAE in the same spot as the units
		effect.setMapLocation (new OverlandMapCoordinates ());
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));

		// Move the CAE to a different spot than the units
		effect.getMapLocation ().setX (1);
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));

		// Put the unit into combat - Note the units are at 0,0,0 but in a combat at 1,0,0 which is the location of the effect, so it should apply
		ourUnit.setCombatLocation (new OverlandMapCoordinates ());
		ourUnit.getCombatLocation ().setX (1);
		theirUnit.setCombatLocation (new OverlandMapCoordinates ());
		theirUnit.getCombatLocation ().setX (1);
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertTrue (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));

		// Move the CAE away from the combat (note we're moving it to the location the units are actually at) - should no longer apply
		effect.getMapLocation ().setX (0);
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (ourUnit, effect, GenerateTestData.createDB (), debugLogger));
		assertFalse (UnitUtils.doesCombatAreaEffectApplyToUnit (theirUnit, effect, GenerateTestData.createDB (), debugLogger));
	}

	/**
	 * Tests the getModifiedUnitMagicRealmLifeformTypeID method on a unit with skills that don't modify its magic realm
	 * @throws RecordNotFoundException If the unit has a skill that we can't find in the cache
	 */
	@Test
	public final void testGetModifiedUnitMagicRealmLifeformTypeID_NoModification () throws RecordNotFoundException
	{
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);

		final UnitHasSkill flight = new UnitHasSkill ();
		flight.setUnitSkillID (GenerateTestData.UNIT_SKILL_FLIGHT);
		unit.getUnitHasSkill ().add (flight);

		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		assertEquals (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL, UnitUtils.getModifiedUnitMagicRealmLifeformTypeID
			(unit, unit.getUnitHasSkill (), spells, GenerateTestData.createDB (), debugLogger));
	}

	/**
	 * Tests the getModifiedUnitMagicRealmLifeformTypeID method on a unit with a skill that modifies its magic realm
	 * @throws RecordNotFoundException If the unit has a skill that we can't find in the cache
	 */
	@Test
	public final void testGetModifiedUnitMagicRealmLifeformTypeID_ModifiedBySkill () throws RecordNotFoundException
	{
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);

		final UnitHasSkill flight = new UnitHasSkill ();
		flight.setUnitSkillID (GenerateTestData.UNIT_SKILL_CC_FLIGHT);
		unit.getUnitHasSkill ().add (flight);

		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		assertEquals (GenerateTestData.LIFEFORM_TYPE_CC, UnitUtils.getModifiedUnitMagicRealmLifeformTypeID (unit, unit.getUnitHasSkill (), spells, GenerateTestData.createDB (), debugLogger));
	}

	/**
	 * Tests the getModifiedUnitMagicRealmLifeformTypeID method on a unit with a spell cast on it that gives a skill that modifies its magic realm
	 * @throws RecordNotFoundException If the unit has a skill that we can't find in the cache
	 */
	@Test
	public final void testGetModifiedUnitMagicRealmLifeformTypeID_ModifiedBySpell () throws RecordNotFoundException
	{
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		unit.setUnitURN (1);

		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		final MemoryMaintainedSpell flight = new MemoryMaintainedSpell ();
		flight.setUnitSkillID (GenerateTestData.UNIT_SKILL_CC_FLIGHT);
		flight.setUnitURN (1);
		spells.add (flight);

		assertEquals (GenerateTestData.LIFEFORM_TYPE_CC, UnitUtils.getModifiedUnitMagicRealmLifeformTypeID (unit, unit.getUnitHasSkill (), spells, GenerateTestData.createDB (), debugLogger));
	}

	/**
	 * Tests the getModifiedSkillValue method on an available unit
	 * @throws RecordNotFoundException If the unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	@Test
	public final void testGetModifiedSkillValue_Available () throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		// Create unit
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		unit.setOwningPlayerID (1);
		unit.setUnitLocation (new OverlandMapCoordinates ());

		final UnitHasSkill experience = new UnitHasSkill ();
		experience.setUnitSkillID (CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE);
		experience.setUnitSkillValue (0);
		unit.getUnitHasSkill ().add (experience);

		final UnitHasSkill thrownWeapons = new UnitHasSkill ();
		thrownWeapons.setUnitSkillID (GenerateTestData.UNIT_SKILL_THROWN_WEAPONS);
		thrownWeapons.setUnitSkillValue (2);
		unit.getUnitHasSkill ().add (thrownWeapons);

		final UnitHasSkill skillWithoutValue = new UnitHasSkill ();
		skillWithoutValue.setUnitSkillID (GenerateTestData.UNIT_SKILL_FLIGHT);
		unit.getUnitHasSkill ().add (skillWithoutValue);

		// Create players
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final PlayerPublicDetails ppd = new PlayerPublicDetails (new PlayerDescription (), ppk, new MomTransientPlayerPublicKnowledge ());
		ppd.getPlayerDescription ().setPlayerID (1);
		players.add (ppd);

		// Create spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		// Create CAEs
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();

		// Test with no modifications
		assertEquals (2, UnitUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), GenerateTestData.UNIT_SKILL_THROWN_WEAPONS, players, spells, combatAreaEffects, GenerateTestData.createDB (), debugLogger));
		assertEquals (0, UnitUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), GenerateTestData.UNIT_SKILL_FLIGHT, players, spells, combatAreaEffects, GenerateTestData.createDB (), debugLogger));
		assertEquals (-1, UnitUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), GenerateTestData.UNIT_SKILL_CC_FLIGHT, players, spells, combatAreaEffects, GenerateTestData.createDB (), debugLogger));

		// Make unit level 3, should then get +3 on attack skill
		experience.setUnitSkillValue (30);
		assertEquals (5, UnitUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), GenerateTestData.UNIT_SKILL_THROWN_WEAPONS, players, spells, combatAreaEffects, GenerateTestData.createDB (), debugLogger));

		// Make player warlord, should up exp level and hence grant another +1
		final PlayerPick warlord = new PlayerPick ();
		warlord.setPickID (CommonDatabaseConstants.VALUE_RETORT_ID_WARLORD);
		warlord.setQuantity (1);
		ppk.getPick ().add (warlord);
		assertEquals (6, UnitUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), GenerateTestData.UNIT_SKILL_THROWN_WEAPONS, players, spells, combatAreaEffects, GenerateTestData.createDB (), debugLogger));

		// Give unit adamantium weapons
		unit.setWeaponGrade (3);
		assertEquals (8, UnitUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), GenerateTestData.UNIT_SKILL_THROWN_WEAPONS, players, spells, combatAreaEffects, GenerateTestData.createDB (), debugLogger));

		// Put a CAE in the wrong location
		final MemoryCombatAreaEffect cae = new MemoryCombatAreaEffect ();
		final OverlandMapCoordinates caeCoords = new OverlandMapCoordinates ();
		caeCoords.setX (1);
		cae.setMapLocation (caeCoords);
		cae.setCombatAreaEffectID (GenerateTestData.CAE_AFFECTS_ALL);
		combatAreaEffects.add (cae);

		assertEquals (8, UnitUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), GenerateTestData.UNIT_SKILL_THROWN_WEAPONS, players, spells, combatAreaEffects, GenerateTestData.createDB (), debugLogger));

		// Fix location
		caeCoords.setX (0);
		assertEquals (9, UnitUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), GenerateTestData.UNIT_SKILL_THROWN_WEAPONS, players, spells, combatAreaEffects, GenerateTestData.createDB (), debugLogger));
	}

	/**
	 * Tests the getModifiedSkillValue method on a real unit
	 * @throws RecordNotFoundException If the unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	@Test
	public final void testGetModifiedSkillValue_Real () throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		// Create unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
		unit.setOwningPlayerID (1);
		unit.setUnitLocation (new OverlandMapCoordinates ());
		unit.setUnitURN (1);

		final UnitHasSkill experience = new UnitHasSkill ();
		experience.setUnitSkillID (CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE);
		experience.setUnitSkillValue (0);
		unit.getUnitHasSkill ().add (experience);

		final UnitHasSkill thrownWeapons = new UnitHasSkill ();
		thrownWeapons.setUnitSkillID (GenerateTestData.UNIT_SKILL_THROWN_WEAPONS);
		thrownWeapons.setUnitSkillValue (2);
		unit.getUnitHasSkill ().add (thrownWeapons);

		// Create players
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final PlayerPublicDetails ppd = new PlayerPublicDetails (new PlayerDescription (), ppk, new MomTransientPlayerPublicKnowledge ());
		ppd.getPlayerDescription ().setPlayerID (1);
		players.add (ppd);

		// Create spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		// Create CAEs
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();

		// Test with no modifications
		assertEquals (2, UnitUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), GenerateTestData.UNIT_SKILL_THROWN_WEAPONS, players, spells, combatAreaEffects, GenerateTestData.createDB (), debugLogger));
		assertEquals (-1, UnitUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), GenerateTestData.UNIT_SKILL_FLIGHT, players, spells, combatAreaEffects, GenerateTestData.createDB (), debugLogger));
		assertEquals (-1, UnitUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), GenerateTestData.UNIT_SKILL_CC_FLIGHT, players, spells, combatAreaEffects, GenerateTestData.createDB (), debugLogger));

		// Make unit level 3, should then get +3 on attack skill
		experience.setUnitSkillValue (30);
		assertEquals (5, UnitUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), GenerateTestData.UNIT_SKILL_THROWN_WEAPONS, players, spells, combatAreaEffects, GenerateTestData.createDB (), debugLogger));

		// Make player warlord, should up exp level and hence grant another +1
		final PlayerPick warlord = new PlayerPick ();
		warlord.setPickID (CommonDatabaseConstants.VALUE_RETORT_ID_WARLORD);
		warlord.setQuantity (1);
		ppk.getPick ().add (warlord);
		assertEquals (6, UnitUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), GenerateTestData.UNIT_SKILL_THROWN_WEAPONS, players, spells, combatAreaEffects, GenerateTestData.createDB (), debugLogger));

		// Give unit adamantium weapons
		unit.setWeaponGrade (3);
		assertEquals (8, UnitUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), GenerateTestData.UNIT_SKILL_THROWN_WEAPONS, players, spells, combatAreaEffects, GenerateTestData.createDB (), debugLogger));

		// Put a CAE in the wrong location
		final MemoryCombatAreaEffect cae = new MemoryCombatAreaEffect ();
		final OverlandMapCoordinates caeCoords = new OverlandMapCoordinates ();
		caeCoords.setX (1);
		cae.setMapLocation (caeCoords);
		cae.setCombatAreaEffectID (GenerateTestData.CAE_AFFECTS_ALL);
		combatAreaEffects.add (cae);

		assertEquals (8, UnitUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), GenerateTestData.UNIT_SKILL_THROWN_WEAPONS, players, spells, combatAreaEffects, GenerateTestData.createDB (), debugLogger));

		// Fix location
		caeCoords.setX (0);
		assertEquals (9, UnitUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), GenerateTestData.UNIT_SKILL_THROWN_WEAPONS, players, spells, combatAreaEffects, GenerateTestData.createDB (), debugLogger));

		// The CAE is set to only apply to normal units - so if we chaos channel it, the CAE won't apply anymore, but we'll get a new skill
		final MemoryMaintainedSpell ccFlight = new MemoryMaintainedSpell ();
		ccFlight.setUnitURN (1);
		ccFlight.setUnitSkillID (GenerateTestData.UNIT_SKILL_CC_FLIGHT);
		spells.add (ccFlight);

		assertEquals (0, UnitUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), GenerateTestData.UNIT_SKILL_CC_FLIGHT, players, spells, combatAreaEffects, GenerateTestData.createDB (), debugLogger));
		assertEquals (8, UnitUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), GenerateTestData.UNIT_SKILL_THROWN_WEAPONS, players, spells, combatAreaEffects, GenerateTestData.createDB (), debugLogger));
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

		assertEquals (1, UnitUtils.getBasicUpkeepValue (warlocks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, GenerateTestData.createDB (), debugLogger));
		assertEquals (5, UnitUtils.getBasicUpkeepValue (warlocks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, GenerateTestData.createDB (), debugLogger));
		assertEquals (0, UnitUtils.getBasicUpkeepValue (warlocks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, GenerateTestData.createDB (), debugLogger));

		assertEquals (0, UnitUtils.getBasicUpkeepValue (stoneGiant, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, GenerateTestData.createDB (), debugLogger));
		assertEquals (0, UnitUtils.getBasicUpkeepValue (stoneGiant, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, GenerateTestData.createDB (), debugLogger));
		assertEquals (9, UnitUtils.getBasicUpkeepValue (stoneGiant, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, GenerateTestData.createDB (), debugLogger));
	}

	/**
	 * Tests the getModifiedUpkeepValue method
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws RecordNotFoundException If the unitID doesn't exist
	 */
	@Test
	public final void testGetModifiedUpkeepValue () throws PlayerNotFoundException, RecordNotFoundException
	{
		// Create units
		final AvailableUnit warlocks = new AvailableUnit ();
		warlocks.setUnitID (GenerateTestData.DARK_ELF_WARLOCKS);
		warlocks.setOwningPlayerID (1);

		final AvailableUnit stoneGiant = new AvailableUnit ();
		stoneGiant.setUnitID (GenerateTestData.STONE_GIANT_UNIT);
		stoneGiant.setOwningPlayerID (1);

		// Create player
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		final PlayerPublicDetails ppd = new PlayerPublicDetails (new PlayerDescription (), ppk, new MomTransientPlayerPublicKnowledge ());
		ppd.getPlayerDescription ().setPlayerID (1);
		players.add (ppd);

		// Before any reductions
		assertEquals (1, UnitUtils.getModifiedUpkeepValue (warlocks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, players, GenerateTestData.createDB (), debugLogger));
		assertEquals (5, UnitUtils.getModifiedUpkeepValue (warlocks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, players, GenerateTestData.createDB (), debugLogger));
		assertEquals (0, UnitUtils.getModifiedUpkeepValue (warlocks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, players, GenerateTestData.createDB (), debugLogger));

		assertEquals (0, UnitUtils.getModifiedUpkeepValue (stoneGiant, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, players, GenerateTestData.createDB (), debugLogger));
		assertEquals (0, UnitUtils.getModifiedUpkeepValue (stoneGiant, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, players, GenerateTestData.createDB (), debugLogger));
		assertEquals (9, UnitUtils.getModifiedUpkeepValue (stoneGiant, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, players, GenerateTestData.createDB (), debugLogger));

		// Add summoner retort - should reduce 9 to (9/4 = 2.25 rounded down to 2) = 7
		final PlayerPick summoner = new PlayerPick ();
		summoner.setPickID (GenerateTestData.SUMMONER);
		summoner.setQuantity (1);
		ppk.getPick ().add (summoner);

		assertEquals (1, UnitUtils.getModifiedUpkeepValue (warlocks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, players, GenerateTestData.createDB (), debugLogger));
		assertEquals (5, UnitUtils.getModifiedUpkeepValue (warlocks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, players, GenerateTestData.createDB (), debugLogger));
		assertEquals (0, UnitUtils.getModifiedUpkeepValue (warlocks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, players, GenerateTestData.createDB (), debugLogger));

		assertEquals (0, UnitUtils.getModifiedUpkeepValue (stoneGiant, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, players, GenerateTestData.createDB (), debugLogger));
		assertEquals (0, UnitUtils.getModifiedUpkeepValue (stoneGiant, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, players, GenerateTestData.createDB (), debugLogger));
		assertEquals (7, UnitUtils.getModifiedUpkeepValue (stoneGiant, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, players, GenerateTestData.createDB (), debugLogger));
	}

	/**
	 * Tests the resetUnitOverlandMovement method for all players
	 * @throws RecordNotFoundException If we can't find the definition for one of the units
	 */
	@Test
	public final void testResetUnitOverlandMovement_AllPlayers ()
		throws RecordNotFoundException
	{
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();

		// Create units owned by 3 players
		for (int playerID = 1; playerID <= 3; playerID++)
		{
			final MemoryUnit spearmen = new MemoryUnit ();
			spearmen.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
			spearmen.setOwningPlayerID (playerID);
			units.add (spearmen);

			final MemoryUnit hellHounds = new MemoryUnit ();
			hellHounds.setUnitID (GenerateTestData.HELL_HOUNDS_UNIT);
			hellHounds.setOwningPlayerID (playerID);
			units.add (hellHounds);
		}

		UnitUtils.resetUnitOverlandMovement (units, 0, GenerateTestData.createDB (), debugLogger);

		assertEquals (2, units.get (0).getDoubleOverlandMovesLeft ());
		assertEquals (4, units.get (1).getDoubleOverlandMovesLeft ());
		assertEquals (2, units.get (2).getDoubleOverlandMovesLeft ());
		assertEquals (4, units.get (3).getDoubleOverlandMovesLeft ());
		assertEquals (2, units.get (4).getDoubleOverlandMovesLeft ());
		assertEquals (4, units.get (5).getDoubleOverlandMovesLeft ());
	}

	/**
	 * Tests the resetUnitOverlandMovement method for a single players
	 * @throws RecordNotFoundException If we can't find the definition for one of the units
	 */
	@Test
	public final void testResetUnitOverlandMovement_OnePlayer ()
		throws RecordNotFoundException
	{
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();

		// Create units owned by 3 players
		for (int playerID = 1; playerID <= 3; playerID++)
		{
			final MemoryUnit spearmen = new MemoryUnit ();
			spearmen.setUnitID (GenerateTestData.BARBARIAN_SPEARMEN);
			spearmen.setOwningPlayerID (playerID);
			units.add (spearmen);

			final MemoryUnit hellHounds = new MemoryUnit ();
			hellHounds.setUnitID (GenerateTestData.HELL_HOUNDS_UNIT);
			hellHounds.setOwningPlayerID (playerID);
			units.add (hellHounds);
		}

		UnitUtils.resetUnitOverlandMovement (units, 2, GenerateTestData.createDB (), debugLogger);

		assertEquals (0, units.get (0).getDoubleOverlandMovesLeft ());
		assertEquals (0, units.get (1).getDoubleOverlandMovesLeft ());
		assertEquals (2, units.get (2).getDoubleOverlandMovesLeft ());
		assertEquals (4, units.get (3).getDoubleOverlandMovesLeft ());
		assertEquals (0, units.get (4).getDoubleOverlandMovesLeft ());
		assertEquals (0, units.get (5).getDoubleOverlandMovesLeft ());
	}

	/**
	 * Tests the listUnitURNs method
	 */
	@Test
	public final void testListUnitURNs ()
	{
		// Test on null list
		assertEquals ("()", UnitUtils.listUnitURNs (null));

		// Test on list with single unit
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();
		final MemoryUnit one = new MemoryUnit ();
		one.setUnitURN (1);
		units.add (one);

		assertEquals ("(1)", UnitUtils.listUnitURNs (units));

		// Test on list with multiple units
		final MemoryUnit five = new MemoryUnit ();
		five.setUnitURN (5);
		units.add (five);

		final MemoryUnit three = new MemoryUnit ();
		three.setUnitURN (3);
		units.add (three);

		assertEquals ("(1, 5, 3)", UnitUtils.listUnitURNs (units));
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
		final OverlandMapCoordinates u2location = new OverlandMapCoordinates ();
		u2location.setX (2);
		u2location.setY (3);
		u2location.setPlane (0);

		final MemoryUnit u2 = new MemoryUnit ();
		u2.setOwningPlayerID (5);
		u2.setStatus (UnitStatusID.ALIVE);
		u2.setUnitLocation (u2location);
		units.add (u2);

		// Wrong player (i.e. player matches)
		final OverlandMapCoordinates u3location = new OverlandMapCoordinates ();
		u3location.setX (2);
		u3location.setY (3);
		u3location.setPlane (1);

		final MemoryUnit u3 = new MemoryUnit ();
		u3.setOwningPlayerID (4);
		u3.setStatus (UnitStatusID.ALIVE);
		u3.setUnitLocation (u3location);
		units.add (u3);

		// Null status
		final OverlandMapCoordinates u4location = new OverlandMapCoordinates ();
		u4location.setX (2);
		u4location.setY (3);
		u4location.setPlane (1);

		final MemoryUnit u4 = new MemoryUnit ();
		u4.setOwningPlayerID (5);
		u4.setUnitLocation (u4location);
		units.add (u4);

		// Unit is dead
		final OverlandMapCoordinates u5location = new OverlandMapCoordinates ();
		u5location.setX (2);
		u5location.setY (3);
		u5location.setPlane (1);

		final MemoryUnit u5 = new MemoryUnit ();
		u5.setOwningPlayerID (5);
		u5.setStatus (UnitStatusID.DEAD);
		u5.setUnitLocation (u5location);
		units.add (u5);

		assertNull (UnitUtils.findFirstAliveEnemyAtLocation (units, 2, 3, 1, 4, debugLogger));

		// Now add one that actually matches
		final OverlandMapCoordinates u6location = new OverlandMapCoordinates ();
		u6location.setX (2);
		u6location.setY (3);
		u6location.setPlane (1);

		final MemoryUnit u6 = new MemoryUnit ();
		u6.setOwningPlayerID (5);
		u6.setStatus (UnitStatusID.ALIVE);
		u6.setUnitLocation (u6location);
		units.add (u6);

		assertEquals (u6, UnitUtils.findFirstAliveEnemyAtLocation (units, 2, 3, 1, 4, debugLogger));
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
		final OverlandMapCoordinates u2location = new OverlandMapCoordinates ();
		u2location.setX (2);
		u2location.setY (3);
		u2location.setPlane (0);

		final MemoryUnit u2 = new MemoryUnit ();
		u2.setOwningPlayerID (5);
		u2.setStatus (UnitStatusID.ALIVE);
		u2.setUnitLocation (u2location);
		units.add (u2);

		// Wrong player (i.e. player matches)
		final OverlandMapCoordinates u3location = new OverlandMapCoordinates ();
		u3location.setX (2);
		u3location.setY (3);
		u3location.setPlane (1);

		final MemoryUnit u3 = new MemoryUnit ();
		u3.setOwningPlayerID (4);
		u3.setStatus (UnitStatusID.ALIVE);
		u3.setUnitLocation (u3location);
		units.add (u3);

		// Null status
		final OverlandMapCoordinates u4location = new OverlandMapCoordinates ();
		u4location.setX (2);
		u4location.setY (3);
		u4location.setPlane (1);

		final MemoryUnit u4 = new MemoryUnit ();
		u4.setOwningPlayerID (5);
		u4.setUnitLocation (u4location);
		units.add (u4);

		// Unit is dead
		final OverlandMapCoordinates u5location = new OverlandMapCoordinates ();
		u5location.setX (2);
		u5location.setY (3);
		u5location.setPlane (1);

		final MemoryUnit u5 = new MemoryUnit ();
		u5.setOwningPlayerID (5);
		u5.setStatus (UnitStatusID.DEAD);
		u5.setUnitLocation (u5location);
		units.add (u5);

		assertEquals (0, UnitUtils.countAliveEnemiesAtLocation (units, 2, 3, 1, 4, debugLogger));

		// Now add one that actually matches
		final OverlandMapCoordinates u6location = new OverlandMapCoordinates ();
		u6location.setX (2);
		u6location.setY (3);
		u6location.setPlane (1);

		final MemoryUnit u6 = new MemoryUnit ();
		u6.setOwningPlayerID (5);
		u6.setStatus (UnitStatusID.ALIVE);
		u6.setUnitLocation (u6location);
		units.add (u6);

		assertEquals (1, UnitUtils.countAliveEnemiesAtLocation (units, 2, 3, 1, 4, debugLogger));

		// Add second matching unit
		final OverlandMapCoordinates u7location = new OverlandMapCoordinates ();
		u7location.setX (2);
		u7location.setY (3);
		u7location.setPlane (1);

		final MemoryUnit u7 = new MemoryUnit ();
		u7.setOwningPlayerID (5);
		u7.setStatus (UnitStatusID.ALIVE);
		u7.setUnitLocation (u7location);
		units.add (u7);

		assertEquals (2, UnitUtils.countAliveEnemiesAtLocation (units, 2, 3, 1, 4, debugLogger));
	}
}
