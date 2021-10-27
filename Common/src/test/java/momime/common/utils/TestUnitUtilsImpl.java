package momime.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

import momime.common.database.AddsToSkill;
import momime.common.database.AddsToSkillValueType;
import momime.common.database.CombatAreaAffectsPlayersID;
import momime.common.database.CombatAreaEffect;
import momime.common.database.CombatAreaEffectSkillBonus;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.database.HeroItemBonus;
import momime.common.database.HeroItemBonusStat;
import momime.common.database.HeroItemType;
import momime.common.database.NegatedBySkill;
import momime.common.database.NegatedByUnitID;
import momime.common.database.Pick;
import momime.common.database.ProductionTypeAndUndoubledValue;
import momime.common.database.RangedAttackTypeEx;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.StoredDamageTypeID;
import momime.common.database.UnitCombatSideID;
import momime.common.database.UnitEx;
import momime.common.database.UnitSkillAndValue;
import momime.common.database.UnitSkillEx;
import momime.common.database.UnitSpellEffect;
import momime.common.database.UnitType;
import momime.common.database.WeaponGrade;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MemoryUnitHeroItemSlot;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.UnitDamage;
import momime.common.messages.UnitStatusID;

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
		
		final UnitType unitType = new UnitType ();
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
		
		final UnitType unitType = new UnitType ();
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
		
		final UnitType unitType = new UnitType ();
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
		
		final UnitType unitType = new UnitType ();
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
	 * Tests the expandSkillList method on a summoned available unit which has no skills which grant other skills
	 * and we provide no info about enemies or the type of incoming attack, so is about the most simple example possible.
	 * Also prove that even if we have no stats whatsoever that contribute to +to hit, that we get a null out (since this was changed).
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testExpandUnitDetails_AvailableUnit_Summoned () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm ("MB01");
		when (db.findUnit (eq ("UN001"), anyString ())).thenReturn (unitDef);
		
		final Pick unitMagicRealm = new Pick ();
		unitMagicRealm.setUnitTypeID ("S");
		when (db.findPick (eq ("MB01"), anyString ())).thenReturn (unitMagicRealm);
		
		final UnitType unitType = new UnitType ();
		when (db.findUnitType (eq ("S"), anyString ())).thenReturn (unitType);
		
		for (int n = 1; n <= 3; n++)
			when (db.findUnitSkill (eq ("US00" + n), anyString ())).thenReturn (new UnitSkillEx ());
			
		when (db.findUnitSkill (eq (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT), anyString ())).thenReturn (new UnitSkillEx ());
		
		// Create other lists
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		// Players
		final PlayerDescription owningPd = new PlayerDescription ();
		owningPd.setPlayerID (1);
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		
		final PlayerPublicDetails owningPlayer = new PlayerPublicDetails (owningPd, pub, null);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (eq (players), eq (owningPd.getPlayerID ()), anyString ())).thenReturn (owningPlayer);
		
		// Create test unit
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID ("UN001");
		unit.setOwningPlayerID (owningPd.getPlayerID ());
		
		for (int n = 1; n <= 3; n++)
		{
			final UnitSkillAndValue skill = new UnitSkillAndValue ();
			skill.setUnitSkillID ("US00" + n);
			
			if (n == 2)
				skill.setUnitSkillValue (4);
			
			unit.getUnitHasSkill ().add (skill);
		}

		// Set up object to test
		final UnitDetailsUtilsImpl unitDetailsUtils = new UnitDetailsUtilsImpl ();
		unitDetailsUtils.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		utils.setUnitDetailsUtils (unitDetailsUtils);
		
		// Run method
		final ExpandedUnitDetails details = utils.expandUnitDetails (unit, null, null, null, players, mem, db);
		
		// Do simple checks
		assertSame (unit, details.getUnit ());
		assertFalse (details.isMemoryUnit ());
		assertSame (unitDef, details.getUnitDefinition ());
		assertSame (unitType, details.getUnitType ());
		assertSame (owningPlayer, details.getOwningPlayer ());
		assertNull (details.getWeaponGrade ());
		assertNull (details.getRangedAttackType ());
		assertNull (details.getBasicExperienceLevel ());
		assertNull (details.getModifiedExperienceLevel ());
		
		assertSame (unitMagicRealm, details.getModifiedUnitMagicRealmLifeformType ());
		
		// Check skills
		for (int n = 1; n <= 3; n++)
		{
			assertTrue (details.hasBasicSkill ("US00" + n));
			assertTrue (details.hasModifiedSkill ("US00" + n));
		}
		
		assertNull (details.getBasicSkillValue ("US001"));
		assertEquals (4, details.getBasicSkillValue ("US002").intValue ());
		assertNull (details.getBasicSkillValue ("US003"));

		assertNull (details.getModifiedSkillValue ("US001"));
		assertEquals (4, details.getModifiedSkillValue ("US002").intValue ());
		assertNull (details.getModifiedSkillValue ("US003"));
		
		assertFalse (details.hasBasicSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT));
		assertFalse (details.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT));
	}

	/**
	 * Tests the expandSkillList method when there's not even a player specified.  This is used in a couple of places, e.g. hitting Rush Buy.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testExpandUnitDetails_AvailableUnit_NoPlayer () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm ("MB01");
		when (db.findUnit (eq ("UN001"), anyString ())).thenReturn (unitDef);
		
		final Pick unitMagicRealm = new Pick ();
		unitMagicRealm.setUnitTypeID ("S");
		when (db.findPick (eq ("MB01"), anyString ())).thenReturn (unitMagicRealm);
		
		final UnitType unitType = new UnitType ();
		when (db.findUnitType (eq ("S"), anyString ())).thenReturn (unitType);
		
		for (int n = 1; n <= 3; n++)
			when (db.findUnitSkill (eq ("US00" + n), anyString ())).thenReturn (new UnitSkillEx ());
			
		when (db.findUnitSkill (eq (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT), anyString ())).thenReturn (new UnitSkillEx ());
		
		// Create other lists
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		// Create test unit
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID ("UN001");
		
		for (int n = 1; n <= 3; n++)
		{
			final UnitSkillAndValue skill = new UnitSkillAndValue ();
			skill.setUnitSkillID ("US00" + n);
			
			if (n == 2)
				skill.setUnitSkillValue (4);
			
			unit.getUnitHasSkill ().add (skill);
		}

		// Set up object to test
		final UnitDetailsUtilsImpl unitDetailsUtils = new UnitDetailsUtilsImpl ();
		
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		utils.setUnitDetailsUtils (unitDetailsUtils);
		
		// Run method
		final ExpandedUnitDetails details = utils.expandUnitDetails (unit, null, null, null, null, mem, db);
		
		// Do simple checks
		assertSame (unit, details.getUnit ());
		assertFalse (details.isMemoryUnit ());
		assertSame (unitDef, details.getUnitDefinition ());
		assertSame (unitType, details.getUnitType ());
		assertNull (details.getOwningPlayer ());
		assertNull (details.getWeaponGrade ());
		assertNull (details.getRangedAttackType ());
		assertNull (details.getBasicExperienceLevel ());
		assertNull (details.getModifiedExperienceLevel ());
		
		assertSame (unitMagicRealm, details.getModifiedUnitMagicRealmLifeformType ());
		
		// Check skills
		for (int n = 1; n <= 3; n++)
		{
			assertTrue (details.hasBasicSkill ("US00" + n));
			assertTrue (details.hasModifiedSkill ("US00" + n));
		}
		
		assertNull (details.getBasicSkillValue ("US001"));
		assertEquals (4, details.getBasicSkillValue ("US002").intValue ());
		assertNull (details.getBasicSkillValue ("US003"));

		assertNull (details.getModifiedSkillValue ("US001"));
		assertEquals (4, details.getModifiedSkillValue ("US002").intValue ());
		assertNull (details.getModifiedSkillValue ("US003"));
		
		assertFalse (details.hasBasicSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT));
		assertFalse (details.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT));
	}
	
	/**
	 * Tests the expandSkillList method on a normal unit which hence has an experience level to calculate, and the exp level gives a boost to one of our stats,
	 * plus we have couple of spells cast on it that grant additional skills
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testExpandUnitDetails_MemoryUnit_SkillsFromSpells_ExpLvl () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm ("LTN");
		when (db.findUnit (eq ("UN001"), anyString ())).thenReturn (unitDef);
		
		final Pick unitMagicRealm = new Pick ();
		unitMagicRealm.setUnitTypeID ("N");
		when (db.findPick (eq ("LTN"), anyString ())).thenReturn (unitMagicRealm);
		
		final UnitType unitType = new UnitType ();
		when (db.findUnitType (eq ("N"), anyString ())).thenReturn (unitType);
		
		for (int n = 0; n <= 5; n++)
		{
			final ExperienceLevel expLvl = new ExperienceLevel ();
			expLvl.setLevelNumber (n);
			expLvl.setExperienceRequired (n * 10);
			unitType.getExperienceLevel ().add (expLvl);
			
			if (n > 0)
			{
				final UnitSkillAndValue expBonus1 = new UnitSkillAndValue ();
				expBonus1.setUnitSkillID ("US002");
				expBonus1.setUnitSkillValue (n);
				expLvl.getExperienceSkillBonus ().add (expBonus1);
				
				// Exp bonus to a skill that we don't have, to prove that we don't suddenly gain the skill.
				// This is like exp giving bonus to Thrown Weapons - the unit doesn't suddenly gain Thrown Weapons if it didn't already have them.
				final UnitSkillAndValue expBonus2 = new UnitSkillAndValue ();
				expBonus2.setUnitSkillID ("US006");
				expBonus2.setUnitSkillValue (n);
				expLvl.getExperienceSkillBonus ().add (expBonus2);
			}
		}
		
		for (int n = 1; n <= 6; n++)
			when (db.findUnitSkill (eq ("US00" + n), anyString ())).thenReturn (new UnitSkillEx ());
		
		when (db.findUnitSkill (eq (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE), anyString ())).thenReturn (new UnitSkillEx ());
		
		final UnitSpellEffect spellEffect1 = new UnitSpellEffect ();
		spellEffect1.setUnitSkillID ("US004");
		
		final Spell spellDef1 = new Spell ();
		spellDef1.getUnitSpellEffect ().add (spellEffect1);
		when (db.findSpell (eq ("SP001"), anyString ())).thenReturn (spellDef1);

		final UnitSpellEffect spellEffect2 = new UnitSpellEffect ();
		spellEffect2.setUnitSkillID ("US005");
		spellEffect2.setUnitSkillValue (3);
		
		final Spell spellDef2 = new Spell ();
		spellDef2.getUnitSpellEffect ().add (spellEffect2);
		when (db.findSpell (eq ("SP002"), anyString ())).thenReturn (spellDef2);

		// Create other lists
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MemoryMaintainedSpell spell1 = new MemoryMaintainedSpell ();
		spell1.setUnitURN (1);
		spell1.setSpellID ("SP001");
		spell1.setUnitSkillID ("US004");
		mem.getMaintainedSpell ().add (spell1);

		final MemoryMaintainedSpell spell2 = new MemoryMaintainedSpell ();
		spell2.setUnitURN (1);
		spell2.setSpellID ("SP002");
		spell2.setUnitSkillID ("US005");
		mem.getMaintainedSpell ().add (spell2);
		
		// Players
		final PlayerDescription owningPd = new PlayerDescription ();
		owningPd.setPlayerID (1);
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		
		final PlayerPublicDetails owningPlayer = new PlayerPublicDetails (owningPd, pub, null);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (eq (players), eq (owningPd.getPlayerID ()), anyString ())).thenReturn (owningPlayer);
		
		// Warlord and Crusade
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		when (playerPickUtils.getQuantityOfPick (pub.getPick (), CommonDatabaseConstants.RETORT_ID_WARLORD)).thenReturn (1);
		
		final MemoryCombatAreaEffectUtils caeUtils = mock (MemoryCombatAreaEffectUtils.class);
		when (caeUtils.findCombatAreaEffect (mem.getCombatAreaEffect (), null, CommonDatabaseConstants.COMBAT_AREA_EFFECT_CRUSADE, owningPd.getPlayerID ())).thenReturn (new MemoryCombatAreaEffect ());
		
		// Create test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitURN (1);
		unit.setUnitID ("UN001");
		unit.setOwningPlayerID (owningPd.getPlayerID ());
		unit.setStatus (UnitStatusID.ALIVE);
		mem.getUnit ().add (unit);
		
		for (int n = 1; n <= 3; n++)
		{
			final UnitSkillAndValue skill = new UnitSkillAndValue ();
			skill.setUnitSkillID ("US00" + n);
			
			if (n == 2)
				skill.setUnitSkillValue (4);
			
			unit.getUnitHasSkill ().add (skill);
		}
		
		final UnitSkillAndValue exp = new UnitSkillAndValue ();
		exp.setUnitSkillID (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE);
		exp.setUnitSkillValue (38);
		unit.getUnitHasSkill ().add (exp);

		// Set up object to test
		final UnitDetailsUtilsImpl unitDetailsUtils = new UnitDetailsUtilsImpl ();
		unitDetailsUtils.setMultiplayerSessionUtils (multiplayerSessionUtils);
		unitDetailsUtils.setMemoryCombatAreaEffectUtils (caeUtils);
		unitDetailsUtils.setPlayerPickUtils (playerPickUtils);
		
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		utils.setUnitDetailsUtils (unitDetailsUtils);
		utils.setPlayerPickUtils (playerPickUtils);
		
		// Run method
		final ExpandedUnitDetails details = utils.expandUnitDetails (unit, null, null, null, players, mem, db);
		
		// Do simple checks
		assertSame (unit, details.getUnit ());
		assertTrue (details.isMemoryUnit ());
		assertSame (unit, details.getMemoryUnit ());
		assertSame (unitDef, details.getUnitDefinition ());
		assertSame (unitType, details.getUnitType ());
		assertSame (owningPlayer, details.getOwningPlayer ());
		assertNull (details.getWeaponGrade ());
		assertNull (details.getRangedAttackType ());
		
		assertEquals (3, details.getBasicExperienceLevel ().getLevelNumber ());
		assertEquals (5, details.getModifiedExperienceLevel ().getLevelNumber ());
		
		assertSame (unitMagicRealm, details.getModifiedUnitMagicRealmLifeformType ());
		
		// Check skills
		for (int n = 1; n <= 5; n++)
		{
			assertTrue (details.hasBasicSkill ("US00" + n));
			assertTrue (details.hasModifiedSkill ("US00" + n));
		}

		assertFalse (details.hasBasicSkill ("US006"));
		assertFalse (details.hasModifiedSkill ("US006"));
		
		assertNull (details.getBasicSkillValue ("US001"));
		assertEquals (4, details.getBasicSkillValue ("US002").intValue ());
		assertNull (details.getBasicSkillValue ("US003"));
		assertNull (details.getBasicSkillValue ("US004"));
		assertEquals (3, details.getBasicSkillValue ("US005").intValue ());

		assertNull (details.getModifiedSkillValue ("US001"));
		assertEquals (4 + 5, details.getModifiedSkillValue ("US002").intValue ());		// +5 from boosted experience level
		assertNull (details.getModifiedSkillValue ("US003"));
		assertNull (details.getModifiedSkillValue ("US004"));
		assertEquals (3, details.getModifiedSkillValue ("US005").intValue ());
	}
	
	/**
	 * Tests the expandSkillList method on a normal unit which has a skill that grants 3 other skills;
	 * 1 of which is negated by another of our skills, and 1 of which is negated by the enemy we're fighting.
	 * Also we have adamantium weapons that gives a couple of bonuses, and our RAT is something like arrows that does get the bonus.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testExpandUnitDetails_MemoryUnit_GrantAndNegateSkills_WeaponGradeRAT () throws Exception
	{
		// Mock database
		// US001 grants US002, US003 & US004, but US003 gets cancelled by us having US005 and US004 gets cancelled by enemy having US006
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm ("LTN");
		unitDef.setRangedAttackType ("RAT01");
		when (db.findUnit (eq ("UN001"), anyString ())).thenReturn (unitDef);
		
		final Pick unitMagicRealm = new Pick ();
		unitMagicRealm.setUnitTypeID ("N");
		when (db.findPick (eq ("LTN"), anyString ())).thenReturn (unitMagicRealm);
		
		final UnitType unitType = new UnitType ();
		when (db.findUnitType (eq ("N"), anyString ())).thenReturn (unitType);
		
		// Skill defintions
		for (final int n : new int [] {2, 5, 6})
			when (db.findUnitSkill (eq ("US00" + n), anyString ())).thenReturn (new UnitSkillEx ());
		
		when (db.findUnitSkill (eq (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK), anyString ())).thenReturn (new UnitSkillEx ());
		
		final UnitSkillEx skillThatGrantsOthers = new UnitSkillEx ();
		for (int n = 2; n <= 4; n++)
			skillThatGrantsOthers.getGrantsSkill ().add ("US00" + n);
		when (db.findUnitSkill (eq ("US001"), anyString ())).thenReturn (skillThatGrantsOthers);
		
		final NegatedBySkill negatedByOurs = new NegatedBySkill ();
		negatedByOurs.setNegatedBySkillID ("US005");
		negatedByOurs.setNegatedByUnitID (NegatedByUnitID.OUR_UNIT);
		
		final UnitSkillEx skillCancelledByOurs = new UnitSkillEx ();
		skillCancelledByOurs.getNegatedBySkill ().add (negatedByOurs);
		when (db.findUnitSkill (eq ("US003"), anyString ())).thenReturn (skillCancelledByOurs);

		final NegatedBySkill negatedByEnemys = new NegatedBySkill ();
		negatedByEnemys.setNegatedBySkillID ("US006");
		negatedByEnemys.setNegatedByUnitID (NegatedByUnitID.ENEMY_UNIT);
		
		final UnitSkillEx skillCancelledByEnemys = new UnitSkillEx ();
		skillCancelledByEnemys.getNegatedBySkill ().add (negatedByEnemys);
		when (db.findUnitSkill (eq ("US004"), anyString ())).thenReturn (skillCancelledByEnemys);
		
		// RAT definition
		final RangedAttackTypeEx rat = new RangedAttackTypeEx ();
		when (db.findRangedAttackType (eq ("RAT01"), anyString ())).thenReturn (rat);
		
		// Weapon grade definition
		final WeaponGrade weaponGrade = new WeaponGrade ();
		for (final String bonusSkillID : new String [] {"US001", CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK})
		{
			final AddsToSkill weaponGradeBonus = new AddsToSkill ();
			weaponGradeBonus.setAddsToSkillID (bonusSkillID);
			weaponGradeBonus.setAddsToSkillValue (2);
			weaponGradeBonus.setAddsToSkillValueType (AddsToSkillValueType.ADD_FIXED);
			weaponGrade.getAddsToSkill ().add (weaponGradeBonus);
		}
		
		when (db.findWeaponGrade (eq (2), anyString ())).thenReturn (weaponGrade);

		// Create other lists
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		// Players
		final PlayerDescription owningPd = new PlayerDescription ();
		owningPd.setPlayerID (1);

		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		
		final PlayerPublicDetails owningPlayer = new PlayerPublicDetails (owningPd, pub, null);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (eq (players), eq (owningPd.getPlayerID ()), anyString ())).thenReturn (owningPlayer);
		
		// Create test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitURN (1);
		unit.setUnitID ("UN001");
		unit.setOwningPlayerID (owningPd.getPlayerID ());
		unit.setStatus (UnitStatusID.ALIVE);
		unit.setWeaponGrade (2);
		mem.getUnit ().add (unit);
		
		for (final int n : new int [] {1, 5})
		{
			final UnitSkillAndValue skill = new UnitSkillAndValue ();
			skill.setUnitSkillID ("US00" + n);
			
			if (n == 1)
				skill.setUnitSkillValue (4);
			
			unit.getUnitHasSkill ().add (skill);
		}
		
		final UnitSkillAndValue rangedAttack = new UnitSkillAndValue ();
		rangedAttack.setUnitSkillID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK);
		rangedAttack.setUnitSkillValue (1);
		unit.getUnitHasSkill ().add (rangedAttack);
		
		// Mock enemy units - we only need any one of them to have the negating skill
		final List<ExpandedUnitDetails> enemyUnits = new ArrayList<ExpandedUnitDetails> ();
		for (int n = 1; n <= 3; n++)
		{
			final ExpandedUnitDetails enemyUnit = mock (ExpandedUnitDetails.class);
			when (enemyUnit.hasModifiedSkill ("US006")).thenReturn (n == 2);
			enemyUnits.add (enemyUnit);
		}
		
		// Set up object to test
		final UnitDetailsUtilsImpl unitDetailsUtils = new UnitDetailsUtilsImpl ();
		unitDetailsUtils.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		utils.setUnitDetailsUtils (unitDetailsUtils);
		utils.setExpandUnitDetailsUtils (new ExpandUnitDetailsUtilsImpl ());		// Until these unit tests are made more modular
		
		// Run method
		final ExpandedUnitDetails details = utils.expandUnitDetails (unit, enemyUnits, null, null, players, mem, db);
		
		// Do simple checks
		assertSame (unit, details.getUnit ());
		assertTrue (details.isMemoryUnit ());
		assertSame (unit, details.getMemoryUnit ());
		assertSame (unitDef, details.getUnitDefinition ());
		assertSame (unitType, details.getUnitType ());
		assertSame (owningPlayer, details.getOwningPlayer ());
		assertSame (weaponGrade, details.getWeaponGrade ());
		assertSame (rat, details.getRangedAttackType ());
		assertNull (details.getBasicExperienceLevel ());
		assertNull (details.getModifiedExperienceLevel ());
		
		assertSame (unitMagicRealm, details.getModifiedUnitMagicRealmLifeformType ());
		
		// Check skills
		for (int n = 1; n <= 6; n++)
		{
			assertEquals ("Failed for US00" + n, n < 6, details.hasBasicSkill ("US00" + n));
			assertEquals ("Failed for US00" + n, (n==1) || (n==2) || (n==5), details.hasModifiedSkill ("US00" + n));
			
			if (n == 1)
				assertEquals (4, details.getBasicSkillValue ("US00" + n).intValue ());
			else if (details.hasBasicSkill ("US00" + n))
				assertNull (details.getBasicSkillValue ("US00" + n));

			if (n == 1)
				assertEquals (4 + 2, details.getModifiedSkillValue ("US00" + n).intValue ());
			else if (details.hasModifiedSkill ("US00" + n))
				assertNull (details.getModifiedSkillValue ("US00" + n));
		}
		
		assertTrue (details.hasBasicSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK));
		assertTrue (details.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK));
		
		assertEquals (1, details.getBasicSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK).intValue ());
		assertEquals (1 + 2, details.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK).intValue ());
	}

	/**
	 * Tests the expandSkillList method on a normal unit which:
	 * - Has adamantium weapons that gives a couple of bonuses, but our RAT is a magic attack so doesn't get the bonus;
	 * - Gets a skill boost from a CAE;
	 * - Has its magic realm/lifeform type altered by a skill, e.g. chaos channels.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testExpandUnitDetails_MemoryUnit_WeaponGradeNoRAT_CAE_ModifyMagicRealm () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm ("LTN");
		unitDef.setRangedAttackType ("RAT01");
		when (db.findUnit (eq ("UN001"), anyString ())).thenReturn (unitDef);
		
		final Pick unitMagicRealm = new Pick ();
		unitMagicRealm.setPickID ("LTN");
		unitMagicRealm.setUnitTypeID ("N");
		when (db.findPick (eq ("LTN"), anyString ())).thenReturn (unitMagicRealm);
		
		final Pick modifiedMagicRealm = new Pick ();
		modifiedMagicRealm.setPickID ("LTC");
		when (db.findPick (eq ("LTC"), anyString ())).thenReturn (modifiedMagicRealm);
		
		final UnitType unitType = new UnitType ();
		when (db.findUnitType (eq ("N"), anyString ())).thenReturn (unitType);
		
		// Skill defintions
		when (db.findUnitSkill (eq (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK), anyString ())).thenReturn (new UnitSkillEx ());
		when (db.findUnitSkill (eq ("US001"), anyString ())).thenReturn (new UnitSkillEx ());

		final UnitSkillEx chaosChannels = new UnitSkillEx ();
		chaosChannels.setChangesUnitToMagicRealm ("LTC");
		when (db.findUnitSkill (eq ("US002"), anyString ())).thenReturn (chaosChannels);

		final UnitSkillEx skillFromCAE = new UnitSkillEx ();
		when (db.findUnitSkill (eq ("CS001"), anyString ())).thenReturn (skillFromCAE);
		
		// RAT definition
		final RangedAttackTypeEx rat = new RangedAttackTypeEx ();
		when (db.findRangedAttackType (eq ("RAT01"), anyString ())).thenReturn (rat);
		
		// Weapon grade definition
		final WeaponGrade weaponGrade = new WeaponGrade ();
		for (final String bonusSkillID : new String [] {"US001", CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK})
		{
			final AddsToSkill weaponGradeBonus = new AddsToSkill ();
			weaponGradeBonus.setAddsToSkillID (bonusSkillID);
			weaponGradeBonus.setAddsToSkillValue (2);
			weaponGradeBonus.setAddsToSkillValueType (AddsToSkillValueType.ADD_FIXED);
			
			if (bonusSkillID.equals (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK))
				weaponGradeBonus.setRangedAttackTypeID ("RAT02");		// <---
			
			weaponGrade.getAddsToSkill ().add (weaponGradeBonus);
		}
		
		when (db.findWeaponGrade (eq (2), anyString ())).thenReturn (weaponGrade);
		
		// CAE definiton
		final CombatAreaEffect caeDef = new CombatAreaEffect ();
		caeDef.setCombatAreaAffectsPlayers (CombatAreaAffectsPlayersID.ALL_EVEN_NOT_IN_COMBAT);
		when (db.findCombatAreaEffect (eq ("CAE01"), anyString ())).thenReturn (caeDef);

		final CombatAreaEffectSkillBonus caeBonus = new CombatAreaEffectSkillBonus ();
		caeBonus.setUnitSkillID ("CS001");
		caeDef.getCombatAreaEffectSkillBonus ().add (caeBonus);
		
		// Create other lists
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MemoryCombatAreaEffect cae = new MemoryCombatAreaEffect ();
		cae.setCombatAreaEffectID ("CAE01");
		mem.getCombatAreaEffect ().add (cae);
		
		// Players
		final PlayerDescription owningPd = new PlayerDescription ();
		owningPd.setPlayerID (1);
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		
		final PlayerPublicDetails owningPlayer = new PlayerPublicDetails (owningPd, pub, null);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (eq (players), eq (owningPd.getPlayerID ()), anyString ())).thenReturn (owningPlayer);
		
		// Create test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitURN (1);
		unit.setUnitID ("UN001");
		unit.setOwningPlayerID (owningPd.getPlayerID ());
		unit.setStatus (UnitStatusID.ALIVE);
		unit.setWeaponGrade (2);
		mem.getUnit ().add (unit);
		
		final UnitSkillAndValue rangedAttack = new UnitSkillAndValue ();
		rangedAttack.setUnitSkillID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK);
		rangedAttack.setUnitSkillValue (1);
		unit.getUnitHasSkill ().add (rangedAttack);

		final UnitSkillAndValue regularSkill = new UnitSkillAndValue ();
		regularSkill.setUnitSkillID ("US001");
		regularSkill.setUnitSkillValue (2);
		unit.getUnitHasSkill ().add (regularSkill);

		final UnitSkillAndValue cc = new UnitSkillAndValue ();
		cc.setUnitSkillID ("US002");
		unit.getUnitHasSkill ().add (cc);
		
		// Set up object to test
		final UnitDetailsUtilsImpl unitDetailsUtils = new UnitDetailsUtilsImpl ();
		unitDetailsUtils.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		utils.setUnitDetailsUtils (unitDetailsUtils);
		utils.setExpandUnitDetailsUtils (new ExpandUnitDetailsUtilsImpl ());		// Until these unit tests are made more modular
		
		// Run method
		final ExpandedUnitDetails details = utils.expandUnitDetails (unit, null, null, null, players, mem, db);
		
		// Do simple checks
		assertSame (unit, details.getUnit ());
		assertTrue (details.isMemoryUnit ());
		assertSame (unit, details.getMemoryUnit ());
		assertSame (unitDef, details.getUnitDefinition ());
		assertSame (unitType, details.getUnitType ());
		assertSame (owningPlayer, details.getOwningPlayer ());
		assertSame (weaponGrade, details.getWeaponGrade ());
		assertSame (rat, details.getRangedAttackType ());
		assertNull (details.getBasicExperienceLevel ());
		assertNull (details.getModifiedExperienceLevel ());
		
		assertSame (modifiedMagicRealm, details.getModifiedUnitMagicRealmLifeformType ());
		
		// Check skills
		assertTrue (details.hasBasicSkill ("US001"));
		assertTrue (details.hasModifiedSkill ("US001"));

		assertTrue (details.hasBasicSkill ("US002"));
		assertTrue (details.hasModifiedSkill ("US002"));

		assertFalse (details.hasBasicSkill ("CS001"));
		assertTrue (details.hasModifiedSkill ("CS001"));
		
		assertEquals (2, details.getBasicSkillValue ("US001").intValue ());
		assertEquals (2 + 2, details.getModifiedSkillValue ("US001").intValue ());
		
		assertNull (details.getBasicSkillValue ("US002"));
		assertNull (details.getModifiedSkillValue ("US002"));

		assertNull (details.getModifiedSkillValue ("CS001"));
		
		assertTrue (details.hasBasicSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK));
		assertTrue (details.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK));
		
		assertEquals (1, details.getBasicSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK).intValue ());
		assertEquals (1, details.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK).intValue ());		// +2 doesn't apply
	}

	/**
	 * Tests the expandSkillList method on a normal unit which:
	 * - Has its magic realm/lifeform type altered by multiple skills, e.g. is both Undead and Chaos Channeled;
	 * - Has both Warlord and Crusade, but having both would push the unit to an experience level that doesn't exist, so it only gets 1 bump up instead of 2;
	 * - Gains a bonus to defence and +to hit - but the unit has neither of these as a base value - to prove that the +to hit bonus still applies but the +defence bonus does not
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testExpandUnitDetails_MemoryUnit_MergedMagicRealm_ExpLevelOverflow_PlusToHitBonus () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm ("LTN");
		when (db.findUnit (eq ("UN001"), anyString ())).thenReturn (unitDef);
		
		// Pick defintions
		final Pick unitMagicRealm = new Pick ();
		unitMagicRealm.setUnitTypeID ("N");
		when (db.findPick (eq ("LTN"), anyString ())).thenReturn (unitMagicRealm);

		final Pick mergedMagicRealm = new Pick ();
		mergedMagicRealm.setPickID ("LTUC");
		
		for (final String mergedFromPickID : new String [] {"LTC", "LTU"})
			mergedMagicRealm.getMergedFromPick ().add (mergedFromPickID);
		
		when (db.findPick (eq ("LTUC"), anyString ())).thenReturn (mergedMagicRealm);
		
		final List<Pick> picks = new ArrayList<Pick> ();
		picks.add (unitMagicRealm);
		picks.add (mergedMagicRealm);
		doReturn (picks).when (db).getPick ();
		
		// Unit type and experience definition
		final UnitType unitType = new UnitType ();
		when (db.findUnitType (eq ("N"), anyString ())).thenReturn (unitType);
		
		for (int n = 0; n <= 4; n++)
		{
			final ExperienceLevel expLvl = new ExperienceLevel ();
			expLvl.setLevelNumber (n);
			expLvl.setExperienceRequired (n * 10);
			unitType.getExperienceLevel ().add (expLvl);
			
			if (n > 0)
			{
				final UnitSkillAndValue expBonus1 = new UnitSkillAndValue ();
				expBonus1.setUnitSkillID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE);
				expBonus1.setUnitSkillValue (n);
				expLvl.getExperienceSkillBonus ().add (expBonus1);
				
				final UnitSkillAndValue expBonus2 = new UnitSkillAndValue ();
				expBonus2.setUnitSkillID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT);
				expBonus2.setUnitSkillValue (n);
				expLvl.getExperienceSkillBonus ().add (expBonus2);
			}
		}
		
		// Skill definitions
		final UnitSkillEx chaosChannels = new UnitSkillEx ();
		chaosChannels.setChangesUnitToMagicRealm ("LTC");
		when (db.findUnitSkill (eq ("US001"), anyString ())).thenReturn (chaosChannels);

		final UnitSkillEx undead = new UnitSkillEx ();
		undead.setChangesUnitToMagicRealm ("LTU");
		when (db.findUnitSkill (eq ("US002"), anyString ())).thenReturn (undead);
		
		when (db.findUnitSkill (eq (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE), anyString ())).thenReturn (new UnitSkillEx ());
		
		for (final String unitSkillID : new String [] {CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT})
			when (db.findUnitSkill (eq (unitSkillID), anyString ())).thenReturn (new UnitSkillEx ());
		
		// Create other lists
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		// Players
		final PlayerDescription owningPd = new PlayerDescription ();
		owningPd.setPlayerID (1);
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		
		final PlayerPublicDetails owningPlayer = new PlayerPublicDetails (owningPd, pub, null);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (eq (players), eq (owningPd.getPlayerID ()), anyString ())).thenReturn (owningPlayer);
		
		// Warlord and Crusade
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		when (playerPickUtils.getQuantityOfPick (pub.getPick (), CommonDatabaseConstants.RETORT_ID_WARLORD)).thenReturn (1);
		
		final MemoryCombatAreaEffectUtils caeUtils = mock (MemoryCombatAreaEffectUtils.class);
		when (caeUtils.findCombatAreaEffect (mem.getCombatAreaEffect (), null, CommonDatabaseConstants.COMBAT_AREA_EFFECT_CRUSADE, owningPd.getPlayerID ())).thenReturn (new MemoryCombatAreaEffect ());
		
		// Create test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitURN (1);
		unit.setUnitID ("UN001");
		unit.setOwningPlayerID (owningPd.getPlayerID ());
		unit.setStatus (UnitStatusID.ALIVE);
		mem.getUnit ().add (unit);
		
		for (int n = 1; n <= 2; n++)
		{
			final UnitSkillAndValue skill = new UnitSkillAndValue ();
			skill.setUnitSkillID ("US00" + n);
			unit.getUnitHasSkill ().add (skill);
		}
		
		final UnitSkillAndValue exp = new UnitSkillAndValue ();
		exp.setUnitSkillID (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE);
		exp.setUnitSkillValue (38);
		unit.getUnitHasSkill ().add (exp);

		// Set up object to test
		final UnitDetailsUtilsImpl unitDetailsUtils = new UnitDetailsUtilsImpl ();
		unitDetailsUtils.setMultiplayerSessionUtils (multiplayerSessionUtils);
		unitDetailsUtils.setMemoryCombatAreaEffectUtils (caeUtils);
		unitDetailsUtils.setPlayerPickUtils (playerPickUtils);
		
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		utils.setUnitDetailsUtils (unitDetailsUtils);
		utils.setPlayerPickUtils (playerPickUtils);
		
		// Run method
		final ExpandedUnitDetails details = utils.expandUnitDetails (unit, null, null, null, players, mem, db);
		
		// Do simple checks
		assertSame (unit, details.getUnit ());
		assertTrue (details.isMemoryUnit ());
		assertSame (unit, details.getMemoryUnit ());
		assertSame (unitDef, details.getUnitDefinition ());
		assertSame (unitType, details.getUnitType ());
		assertSame (owningPlayer, details.getOwningPlayer ());
		assertNull (details.getWeaponGrade ());
		assertNull (details.getRangedAttackType ());
		
		assertEquals (3, details.getBasicExperienceLevel ().getLevelNumber ());
		assertEquals (4, details.getModifiedExperienceLevel ().getLevelNumber ());
		
		assertSame (mergedMagicRealm, details.getModifiedUnitMagicRealmLifeformType ());
		
		// Check skills
		for (int n = 1; n <= 2; n++)
		{
			assertTrue (details.hasBasicSkill ("US00" + n));
			assertTrue (details.hasModifiedSkill ("US00" + n));
			
			assertNull (details.getBasicSkillValue ("US00" + n));
			assertNull (details.getModifiedSkillValue ("US00" + n));
		}
		
		assertFalse (details.hasBasicSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE));
		assertFalse (details.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE));				// <-- note this is still "False" despite the +4 bonus

		assertFalse (details.hasBasicSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT));
		assertTrue (details.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT));		// <-- note this is "True"
		
		assertEquals (4, details.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT).intValue ());		// no basic component, but +4 bonus from exp
	}

	/**
	 * Tests the expandSkillList method specifically around bonuses from hero-like skills where the bonus we get comes from our level, so
	 * - Standard skill which adds +1 pt per level
	 * - Super skill which adds +1½ pts per level
	 * - Standard skill with divisor 2 which adds +½ pt level
	 * - Super skill with divisor 2 which adds +¾ per level
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testExpandUnitDetails_MemoryUnit_HeroSkills () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm ("LTN");
		when (db.findUnit (eq ("UN001"), anyString ())).thenReturn (unitDef);
		
		final Pick unitMagicRealm = new Pick ();
		unitMagicRealm.setUnitTypeID ("N");
		when (db.findPick (eq ("LTN"), anyString ())).thenReturn (unitMagicRealm);
		
		final UnitType unitType = new UnitType ();
		when (db.findUnitType (eq ("N"), anyString ())).thenReturn (unitType);
		
		for (int n = 0; n <= 8; n++)
		{
			final ExperienceLevel expLvl = new ExperienceLevel ();
			expLvl.setLevelNumber (n);
			expLvl.setExperienceRequired (n * 10);
			unitType.getExperienceLevel ().add (expLvl);
		}
		
		// The skills that the bonuses add to
		final List<UnitSkillEx> unitSkillDefs = new ArrayList<UnitSkillEx> ();
		for (int n = 1; n <= 4; n++)
		{
			final UnitSkillEx skillDef = new UnitSkillEx ();
			skillDef.setUnitSkillID ("US00" + n);
			unitSkillDefs.add (skillDef);
		}
		
		final UnitSkillEx expDef = new UnitSkillEx ();
		expDef.setUnitSkillID (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE);
		unitSkillDefs.add (expDef);
		
		// The hero skills that add to them
		for (int n = 1; n <= 4; n++)
		{
			final AddsToSkill heroSkillBonus = new AddsToSkill ();
			heroSkillBonus.setAddsToSkillID ("US00" + n);
			heroSkillBonus.setAddsToSkillValue ((n <= 2) ? 1 : 2);
			heroSkillBonus.setAddsToSkillValueType (AddsToSkillValueType.ADD_DIVISOR);
			
			final UnitSkillEx heroSkill = new UnitSkillEx ();
			heroSkill.setUnitSkillID ("HS0" + n);
			heroSkill.getAddsToSkill ().add (heroSkillBonus);
			
			unitSkillDefs.add (heroSkill);
		}

		for (final UnitSkillEx skillDef : unitSkillDefs)
			when (db.findUnitSkill (eq (skillDef.getUnitSkillID ()), anyString ())).thenReturn (skillDef);
		
		doReturn (unitSkillDefs).when (db).getUnitSkills ();
		
		// Create other lists
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		// Players
		final PlayerDescription owningPd = new PlayerDescription ();
		owningPd.setPlayerID (1);
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		
		final PlayerPublicDetails owningPlayer = new PlayerPublicDetails (owningPd, pub, null);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (eq (players), eq (owningPd.getPlayerID ()), anyString ())).thenReturn (owningPlayer);
		
		// Create test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitURN (1);
		unit.setUnitID ("UN001");
		unit.setOwningPlayerID (owningPd.getPlayerID ());
		unit.setStatus (UnitStatusID.ALIVE);
		mem.getUnit ().add (unit);

		for (int n = 1; n <= 4; n++)
		{
			final UnitSkillAndValue skill = new UnitSkillAndValue ();
			skill.setUnitSkillID ("HS0" + n);
			skill.setUnitSkillValue (2 - (n % 2));		// This is whether the skill is "super" or not, i.e. did the hero get 1 or 2 picks of the skill
			unit.getUnitHasSkill ().add (skill);
		}
		
		for (int n = 1; n <= 4; n++)
		{
			final UnitSkillAndValue skill = new UnitSkillAndValue ();
			skill.setUnitSkillID ("US00" + n);
			skill.setUnitSkillValue (1);			
			unit.getUnitHasSkill ().add (skill);
		}
		
		final UnitSkillAndValue exp = new UnitSkillAndValue ();
		exp.setUnitSkillID (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE);
		unit.getUnitHasSkill ().add (exp);

		// Set up object to test
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		
		final UnitDetailsUtilsImpl unitDetailsUtils = new UnitDetailsUtilsImpl ();
		unitDetailsUtils.setMultiplayerSessionUtils (multiplayerSessionUtils);
		unitDetailsUtils.setMemoryCombatAreaEffectUtils (mock (MemoryCombatAreaEffectUtils.class));
		unitDetailsUtils.setPlayerPickUtils (playerPickUtils);
		
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		utils.setUnitDetailsUtils (unitDetailsUtils);
		utils.setPlayerPickUtils (playerPickUtils);
		utils.setExpandUnitDetailsUtils (new ExpandUnitDetailsUtilsImpl ());		// Until these unit tests are made more modular

		// Test every experience level
		for (int expLevel = 0; expLevel <= 8; expLevel++)
		{
			// Run method
			exp.setUnitSkillValue (expLevel * 10);
			final ExpandedUnitDetails details = utils.expandUnitDetails (unit, null, null, null, players, mem, db);
			
			// Do simple checks
			assertSame (unit, details.getUnit ());
			assertTrue (details.isMemoryUnit ());
			assertSame (unit, details.getMemoryUnit ());
			assertSame (unitDef, details.getUnitDefinition ());
			assertSame (unitType, details.getUnitType ());
			assertSame (owningPlayer, details.getOwningPlayer ());
			assertNull (details.getWeaponGrade ());
			assertNull (details.getRangedAttackType ());
			
			assertEquals (expLevel, details.getBasicExperienceLevel ().getLevelNumber ());
			assertEquals (expLevel, details.getModifiedExperienceLevel ().getLevelNumber ());
			
			assertSame (unitMagicRealm, details.getModifiedUnitMagicRealmLifeformType ());
			
			// Check skills
			for (int n = 1; n <= 4; n++)
			{
				assertTrue (details.hasBasicSkill ("US00" + n));
				assertTrue (details.hasModifiedSkill ("US00" + n));
	
				assertTrue (details.hasBasicSkill ("HS0" + n));
				assertTrue (details.hasModifiedSkill ("HS0" + n));
				
				assertEquals (2 - (n % 2), details.getBasicSkillValue ("HS0" + n).intValue ());
				assertEquals (2 - (n % 2), details.getModifiedSkillValue ("HS0" + n).intValue ());
			}

			// Here's the values we're most interested to check
			assertEquals ("Level " + expLevel, 1, details.getBasicSkillValue ("US001").intValue ());
			assertEquals ("Level " + expLevel, 2 + expLevel, details.getModifiedSkillValue ("US001").intValue ());						// Even at the "no exp" level, you still get a +1

			assertEquals ("Level " + expLevel, 1, details.getBasicSkillValue ("US002").intValue ());
			assertEquals ("Level " + expLevel, 1 + (((1+expLevel)*3)/2), details.getModifiedSkillValue ("US002").intValue ());	// Matches "Constitution" page on MoM Wiki

			assertEquals ("Level " + expLevel, 1, details.getBasicSkillValue ("US003").intValue ());
			assertEquals ("Level " + expLevel, 1 + ((1+expLevel)/2), details.getModifiedSkillValue ("US003").intValue ());			// Matches "Blademaster" page on MoM Wiki

			assertEquals ("Level " + expLevel, 1, details.getBasicSkillValue ("US004").intValue ());
			assertEquals ("Level " + expLevel, 1 + (((1+expLevel)*3)/4), details.getModifiedSkillValue ("US004").intValue ());	// Matches "Blademaster" page on MoM Wiki
		}
	}

	/**
	 * Tests the expandSkillList method specifically around different ways of calculating the amount of bonus to add, so we test
	 * - A bonus from a skill where the value is defined against the skill itself, like flame blade giving +2 attack
	 * - A bonus from a skill where the value is defined specific to that unit (similar to Resistance To All +2), like the unit stack skills, but try it on the single unit first
	 * - A bonus from unit stack skills
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testExpandUnitDetails_MemoryUnit_BonusValues () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm ("LTN");
		when (db.findUnit (eq ("UN001"), anyString ())).thenReturn (unitDef);
		
		final Pick unitMagicRealm = new Pick ();
		unitMagicRealm.setUnitTypeID ("N");
		when (db.findPick (eq ("LTN"), anyString ())).thenReturn (unitMagicRealm);
		
		final UnitType unitType = new UnitType ();
		when (db.findUnitType (eq ("N"), anyString ())).thenReturn (unitType);

		// The skills that the bonuses add to
		final List<UnitSkillEx> unitSkillDefs = new ArrayList<UnitSkillEx> ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitSkillEx skillDef = new UnitSkillEx ();
			skillDef.setUnitSkillID ("US00" + n);
			unitSkillDefs.add (skillDef);
		}
		
		// The skills that add to them
		for (int n = 1; n <= 3; n++)
		{
			final AddsToSkill bonusSkillBonus = new AddsToSkill ();
			bonusSkillBonus.setAddsToSkillID ("US00" + n);
			bonusSkillBonus.setAddsToSkillValueType (AddsToSkillValueType.ADD_FIXED);
			
			if (n == 1)
				bonusSkillBonus.setAddsToSkillValue (2);
			
			if (n == 3)
				bonusSkillBonus.setAffectsEntireStack (true);
			
			final UnitSkillEx bonusSkill = new UnitSkillEx ();
			bonusSkill.setUnitSkillID ("US00" + (n+3));
			bonusSkill.getAddsToSkill ().add (bonusSkillBonus);
			
			unitSkillDefs.add (bonusSkill);
		}

		for (final UnitSkillEx skillDef : unitSkillDefs)
			when (db.findUnitSkill (eq (skillDef.getUnitSkillID ()), anyString ())).thenReturn (skillDef);
		
		doReturn (unitSkillDefs).when (db).getUnitSkills ();
		
		// Create other lists
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		// Players
		final PlayerDescription owningPd = new PlayerDescription ();
		owningPd.setPlayerID (1);
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		
		final PlayerPublicDetails owningPlayer = new PlayerPublicDetails (owningPd, pub, null);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (eq (players), eq (owningPd.getPlayerID ()), anyString ())).thenReturn (owningPlayer);
		
		// Create test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitURN (1);
		unit.setUnitID ("UN001");
		unit.setOwningPlayerID (owningPd.getPlayerID ());
		unit.setStatus (UnitStatusID.ALIVE);
		unit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		mem.getUnit ().add (unit);

		for (int n = 1; n <= 5; n++)
		{
			final UnitSkillAndValue skill = new UnitSkillAndValue ();
			skill.setUnitSkillID ("US00" + n);
			
			if (n <= 3)
				skill.setUnitSkillValue (1);
			else if (n == 5)
				skill.setUnitSkillValue (3);
			
			unit.getUnitHasSkill ().add (skill);
		}
		
		// Create the other unit in our stack
		final MemoryUnit otherUnit = new MemoryUnit ();
		otherUnit.setUnitURN (2);
		otherUnit.setUnitID ("UN001");
		otherUnit.setOwningPlayerID (owningPd.getPlayerID ());
		otherUnit.setStatus (UnitStatusID.ALIVE);
		otherUnit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		mem.getUnit ().add (otherUnit);
		
		final UnitSkillAndValue otherUnitSkill = new UnitSkillAndValue ();
		otherUnitSkill.setUnitSkillID ("US006");
		otherUnitSkill.setUnitSkillValue (4);
		otherUnit.getUnitHasSkill ().add (otherUnitSkill);
		
		// Set up object to test
		final UnitDetailsUtilsImpl unitDetailsUtils = new UnitDetailsUtilsImpl ();
		unitDetailsUtils.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		utils.setUnitDetailsUtils (unitDetailsUtils);
		utils.setExpandUnitDetailsUtils (new ExpandUnitDetailsUtilsImpl ());		// Until these unit tests are made more modular
		
		// Run method
		final ExpandedUnitDetails details = utils.expandUnitDetails (unit, null, null, null, players, mem, db);
		
		// Do simple checks
		assertSame (unit, details.getUnit ());
		assertTrue (details.isMemoryUnit ());
		assertSame (unit, details.getMemoryUnit ());
		assertSame (unitDef, details.getUnitDefinition ());
		assertSame (unitType, details.getUnitType ());
		assertSame (owningPlayer, details.getOwningPlayer ());
		assertNull (details.getWeaponGrade ());
		assertNull (details.getRangedAttackType ());
		assertNull (details.getBasicExperienceLevel ());
		assertNull (details.getModifiedExperienceLevel ());
		
		assertSame (unitMagicRealm, details.getModifiedUnitMagicRealmLifeformType ());
		
		// Check skills
		for (int n = 1; n <= 5; n++)
		{
			assertTrue (details.hasBasicSkill ("US00" + n));
			assertTrue (details.hasModifiedSkill ("US00" + n));
		}

		assertFalse (details.hasBasicSkill ("US006"));
		assertFalse (details.hasModifiedSkill ("US006"));
		
		assertEquals (1, details.getBasicSkillValue ("US001").intValue ());
		assertEquals (1 + 2, details.getModifiedSkillValue ("US001").intValue ());		// +2 fixed value on the skill itself, like flame blade

		assertEquals (1, details.getBasicSkillValue ("US002").intValue ());
		assertEquals (1 + 3, details.getModifiedSkillValue ("US002").intValue ());		// +3 coming from a skill strength defined on the unit itself

		assertEquals (1, details.getBasicSkillValue ("US003").intValue ());
		assertEquals (1 + 4, details.getModifiedSkillValue ("US003").intValue ());		// +4 coming from another unit in the stack
	}
	
	/**
	 * Tests the expandSkillList method specifically around bonuses from hero items, so:
	 * - A bonus based on the type of item that has no skill value, like shields granting the Large Shield skill (1)
	 * - A bonus based on the type of item that has a skill value, like plate mail granting +2 defence (2)
	 * - A bonus based on the type of item that has a skill value, but we don't have the base skill that its trying to add to (3) 
	 * - An imbued valueless skill, like endurance (4)
	 * - An imbued valueless skill that in turn grants other skills, like a hero item with imbued Invulnerability granting the Invulnerability skill (10) which in turn grants the Weapon Immunity skill (5)
	 * - An imbued bonus, like +2 defence (6)
	 * - An imbued bonus but we don't have the base skill that its trying to add to (7)
	 * - A "+attack" bonus, which grants a bonus to one skill we do have (8) and one that we don't (9) 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testExpandUnitDetails_MemoryUnit_HeroItems () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm ("LTN");
		when (db.findUnit (eq ("UN001"), anyString ())).thenReturn (unitDef);
		
		final Pick unitMagicRealm = new Pick ();
		unitMagicRealm.setUnitTypeID ("N");
		when (db.findPick (eq ("LTN"), anyString ())).thenReturn (unitMagicRealm);
		
		final UnitType unitType = new UnitType ();
		when (db.findUnitType (eq ("N"), anyString ())).thenReturn (unitType);

		// Skill definitions
		for (int n = 1; n <= 9; n++)
			when (db.findUnitSkill (eq ("US00" + n), anyString ())).thenReturn (new UnitSkillEx ());

		final UnitSkillEx invulnerabilityDef = new UnitSkillEx ();
		invulnerabilityDef.getGrantsSkill ().add ("US005");
		when (db.findUnitSkill (eq ("US010"), anyString ())).thenReturn (invulnerabilityDef);
		
		// Hero item type
		final HeroItemType heroItemType = new HeroItemType ();
		
		for (int n = 1; n <= 3; n++)
		{
			final UnitSkillAndValue heroItemTypeBonus = new UnitSkillAndValue ();
			heroItemTypeBonus.setUnitSkillID ("US00" + n);
			
			if (n > 1)
				heroItemTypeBonus.setUnitSkillValue (2);
			
			heroItemType.getHeroItemTypeBasicStat ().add (heroItemTypeBonus);
		}
		
		for (int n = 8; n <= 9; n++)
			heroItemType.getHeroItemTypeAttackType ().add ("US00" + n);
		
		when (db.findHeroItemType (eq ("IT01"), anyString ())).thenReturn (heroItemType);
		
		// Imbuable properties
		for (int n = 1; n <= 4; n++)
		{
			final HeroItemBonusStat imbueBonus = new HeroItemBonusStat ();
			imbueBonus.setUnitSkillID ((n == 2) ? "US010" : ("US00" + (n+3)));
			
			if (n >= 3)
				imbueBonus.setUnitSkillValue (2);
			
			final HeroItemBonus imbue = new HeroItemBonus ();
			imbue.getHeroItemBonusStat ().add (imbueBonus);
			when (db.findHeroItemBonus (eq ("IB0" + n), anyString ())).thenReturn (imbue);
		}

		final HeroItemBonusStat plusAttackBonus = new HeroItemBonusStat ();
		plusAttackBonus.setUnitSkillID (CommonDatabaseConstants.UNIT_SKILL_ID_ATTACK_APPROPRIATE_FOR_TYPE_OF_HERO_ITEM);
		plusAttackBonus.setUnitSkillValue (3);
		
		final HeroItemBonus plusAttack = new HeroItemBonus ();
		plusAttack.getHeroItemBonusStat ().add (plusAttackBonus);
		when (db.findHeroItemBonus (eq ("IB05"), anyString ())).thenReturn (plusAttack);
		
		// Create other lists
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		// Players
		final PlayerDescription owningPd = new PlayerDescription ();
		owningPd.setPlayerID (1);
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		
		final PlayerPublicDetails owningPlayer = new PlayerPublicDetails (owningPd, pub, null);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (eq (players), eq (owningPd.getPlayerID ()), anyString ())).thenReturn (owningPlayer);
		
		// Create test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitURN (1);
		unit.setUnitID ("UN001");
		unit.setOwningPlayerID (owningPd.getPlayerID ());
		unit.setStatus (UnitStatusID.ALIVE);
		unit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		mem.getUnit ().add (unit);

		for (final int n : new int [] {2, 6, 8})
		{
			final UnitSkillAndValue skill = new UnitSkillAndValue ();
			skill.setUnitSkillID ("US00" + n);
			skill.setUnitSkillValue (1);
			unit.getUnitHasSkill ().add (skill);
		}
		
		// Create hero item
		final NumberedHeroItem item = new NumberedHeroItem ();
		item.setHeroItemTypeID ("IT01");
		
		for (int n = 1; n <= 5; n++)
			item.getHeroItemChosenBonus ().add ("IB0" + n);
		
		final MemoryUnitHeroItemSlot slot = new MemoryUnitHeroItemSlot ();
		slot.setHeroItem (item);
		
		unit.getHeroItemSlot ().add (slot);
		
		// Set up object to test
		final UnitDetailsUtilsImpl unitDetailsUtils = new UnitDetailsUtilsImpl ();
		unitDetailsUtils.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		utils.setUnitDetailsUtils (unitDetailsUtils);
		
		// Run method
		final ExpandedUnitDetails details = utils.expandUnitDetails (unit, null, null, null, players, mem, db);
		
		// Do simple checks
		assertSame (unit, details.getUnit ());
		assertTrue (details.isMemoryUnit ());
		assertSame (unit, details.getMemoryUnit ());
		assertSame (unitDef, details.getUnitDefinition ());
		assertSame (unitType, details.getUnitType ());
		assertSame (owningPlayer, details.getOwningPlayer ());
		assertNull (details.getWeaponGrade ());
		assertNull (details.getRangedAttackType ());
		assertNull (details.getBasicExperienceLevel ());
		assertNull (details.getModifiedExperienceLevel ());
		
		assertSame (unitMagicRealm, details.getModifiedUnitMagicRealmLifeformType ());
		
		// Tests to do with the basic item type
		assertTrue (details.hasBasicSkill ("US001"));
		assertTrue (details.hasModifiedSkill ("US001"));
		assertNull (details.getBasicSkillValue ("US001"));
		assertNull (details.getModifiedSkillValue ("US001"));

		assertTrue (details.hasBasicSkill ("US002"));
		assertTrue (details.hasModifiedSkill ("US002"));
		assertEquals (1, details.getBasicSkillValue ("US002").intValue ());
		assertEquals (1 + 2, details.getModifiedSkillValue ("US002").intValue ());

		assertFalse (details.hasBasicSkill ("US003"));
		assertFalse (details.hasModifiedSkill ("US003"));
		
		// Tests to do with imbued skills
		assertTrue (details.hasBasicSkill ("US004"));
		assertTrue (details.hasModifiedSkill ("US004"));
		assertNull (details.getBasicSkillValue ("US004"));
		assertNull (details.getModifiedSkillValue ("US004"));

		assertTrue (details.hasBasicSkill ("US010"));
		assertTrue (details.hasModifiedSkill ("US010"));
		assertNull (details.getBasicSkillValue ("US010"));
		assertNull (details.getModifiedSkillValue ("US010"));
		assertTrue (details.hasBasicSkill ("US005"));
		assertTrue (details.hasModifiedSkill ("US005"));
		assertNull (details.getBasicSkillValue ("US005"));
		assertNull (details.getModifiedSkillValue ("US005"));

		assertTrue (details.hasBasicSkill ("US006"));
		assertTrue (details.hasModifiedSkill ("US006"));
		assertEquals (1, details.getBasicSkillValue ("US006").intValue ());
		assertEquals (1 + 2, details.getModifiedSkillValue ("US006").intValue ());
	
		assertFalse (details.hasBasicSkill ("US007"));
		assertTrue (details.hasModifiedSkill ("US007"));
		assertEquals (2, details.getModifiedSkillValue ("US007").intValue ());

		assertTrue (details.hasBasicSkill ("US008"));
		assertTrue (details.hasModifiedSkill ("US008"));
		assertEquals (1, details.getBasicSkillValue ("US008").intValue ());
		assertEquals (1 + 3, details.getModifiedSkillValue ("US008").intValue ());
		assertFalse (details.hasBasicSkill ("US009"));
		assertFalse (details.hasModifiedSkill ("US009"));
	}

	/**
	 * Tests the expandSkillList method specifically around bonuses that only apply vs certain types of incoming attack, so:
	 * - A bonus that only applies if we input a specific attackFromSkillID value
	 * - A bonus that only applies if we input anything other than a specific attackFromSkillID value
	 * - A bonus that only applies if we input a specific attackFromMagicRealmID value 
	 * - A bonus that only applies if the unit has a specific rangedAttackType
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testExpandUnitDetails_MemoryUnit_ConditionalBonuses () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm ("LTN");
		unitDef.setRangedAttackType ("RAT01");
		when (db.findUnit (eq ("UN001"), anyString ())).thenReturn (unitDef);
		
		final Pick unitMagicRealm = new Pick ();
		unitMagicRealm.setUnitTypeID ("N");
		when (db.findPick (eq ("LTN"), anyString ())).thenReturn (unitMagicRealm);
		
		final UnitType unitType = new UnitType ();
		when (db.findUnitType (eq ("N"), anyString ())).thenReturn (unitType);

		// The skills that the bonuses add to
		final List<UnitSkillEx> unitSkillDefs = new ArrayList<UnitSkillEx> ();
		for (int n = 1; n <= 5; n++)
		{
			final UnitSkillEx skillDef = new UnitSkillEx ();
			skillDef.setUnitSkillID ("US00" + n);
			unitSkillDefs.add (skillDef);
		}
		
		// The skills that add to them
		for (int n = 1; n <= 5; n++)
		{
			final AddsToSkill bonusSkillBonus = new AddsToSkill ();
			bonusSkillBonus.setAddsToSkillID ("US00" + n);
			bonusSkillBonus.setAddsToSkillValue (n);
			bonusSkillBonus.setAddsToSkillValueType (AddsToSkillValueType.ADD_FIXED);
			
			switch (n)
			{
				case 1:
					bonusSkillBonus.setOnlyVersusAttacksFromSkillID ("UA01");
					break;
					
				case 2:
					bonusSkillBonus.setOnlyVersusAttacksFromSkillID ("UA02");
					bonusSkillBonus.setNegateOnlyVersusAttacksFromSkillID (true);
					break;
					
				case 3:
					bonusSkillBonus.setOnlyVersusAttacksFromMagicRealmID ("MB01");
					break;
				
				case 4:
					bonusSkillBonus.setRangedAttackTypeID ("RAT02");
					break;
					
				case 5:
					bonusSkillBonus.setOnlyInCombat (true);
					break;
			}

			String s = Integer.valueOf (n + 5).toString ();
			while (s.length () < 3)
				s = "0" + s;
			
			final UnitSkillEx bonusSkill = new UnitSkillEx ();
			bonusSkill.setUnitSkillID ("US" + s);
			bonusSkill.getAddsToSkill ().add (bonusSkillBonus);
			
			unitSkillDefs.add (bonusSkill);
		}

		for (final UnitSkillEx skillDef : unitSkillDefs)
			when (db.findUnitSkill (eq (skillDef.getUnitSkillID ()), anyString ())).thenReturn (skillDef);
		
		doReturn (unitSkillDefs).when (db).getUnitSkills ();
		
		// RAT definitions
		final RangedAttackTypeEx rat1 = new RangedAttackTypeEx ();
		when (db.findRangedAttackType (eq ("RAT01"), anyString ())).thenReturn (rat1);
		
		final RangedAttackTypeEx rat2 = new RangedAttackTypeEx ();
		when (db.findRangedAttackType (eq ("RAT02"), anyString ())).thenReturn (rat2);
		
		// Create other lists
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		// Players
		final PlayerDescription owningPd = new PlayerDescription ();
		owningPd.setPlayerID (1);
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		
		final PlayerPublicDetails owningPlayer = new PlayerPublicDetails (owningPd, pub, null);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (eq (players), eq (owningPd.getPlayerID ()), anyString ())).thenReturn (owningPlayer);
		
		// Create test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitURN (1);
		unit.setUnitID ("UN001");
		unit.setOwningPlayerID (owningPd.getPlayerID ());
		unit.setStatus (UnitStatusID.ALIVE);
		unit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		mem.getUnit ().add (unit);

		for (int n = 1; n <= 10; n++)
		{
			String s = Integer.valueOf (n).toString ();
			while (s.length () < 3)
				s = "0" + s;
			
			final UnitSkillAndValue skill = new UnitSkillAndValue ();
			skill.setUnitSkillID ("US" + s);
			
			if (n <= 5)
				skill.setUnitSkillValue (1);
			
			unit.getUnitHasSkill ().add (skill);
		}

		// Set up object to test
		final UnitDetailsUtilsImpl unitDetailsUtils = new UnitDetailsUtilsImpl ();
		unitDetailsUtils.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		utils.setUnitDetailsUtils (unitDetailsUtils);
		utils.setExpandUnitDetailsUtils (new ExpandUnitDetailsUtilsImpl ());		// Until these unit tests are made more modular
		
		// If we specify nothing about the type of incoming attack, then only the RAT-based bonus can apply
		final ExpandedUnitDetails details1 = utils.expandUnitDetails (unit, null, null, null, players, mem, db);

		assertSame (unit, details1.getUnit ());
		assertTrue (details1.isMemoryUnit ());
		assertSame (unit, details1.getMemoryUnit ());
		assertSame (unitDef, details1.getUnitDefinition ());
		assertSame (unitType, details1.getUnitType ());
		assertSame (owningPlayer, details1.getOwningPlayer ());
		assertNull (details1.getWeaponGrade ());
		assertSame (rat1, details1.getRangedAttackType ());
		assertNull (details1.getBasicExperienceLevel ());
		assertNull (details1.getModifiedExperienceLevel ());
		assertSame (unitMagicRealm, details1.getModifiedUnitMagicRealmLifeformType ());

		assertTrue (details1.hasBasicSkill ("US001"));
		assertTrue (details1.hasModifiedSkill ("US001"));
		assertEquals (1, details1.getBasicSkillValue ("US001").intValue ());
		assertEquals (1, details1.getModifiedSkillValue ("US001").intValue ());
		
		assertTrue (details1.hasBasicSkill ("US002"));
		assertTrue (details1.hasModifiedSkill ("US002"));
		assertEquals (1, details1.getBasicSkillValue ("US002").intValue ());
		assertEquals (1, details1.getModifiedSkillValue ("US002").intValue ());
		
		assertTrue (details1.hasBasicSkill ("US003"));
		assertTrue (details1.hasModifiedSkill ("US003"));
		assertEquals (1, details1.getBasicSkillValue ("US003").intValue ());
		assertEquals (1, details1.getModifiedSkillValue ("US003").intValue ());
		
		assertTrue (details1.hasBasicSkill ("US004"));
		assertTrue (details1.hasModifiedSkill ("US004"));
		assertEquals (1, details1.getBasicSkillValue ("US004").intValue ());
		assertEquals (1, details1.getModifiedSkillValue ("US004").intValue ());
		
		assertTrue (details1.hasBasicSkill ("US005"));
		assertTrue (details1.hasModifiedSkill ("US005"));
		assertEquals (1, details1.getBasicSkillValue ("US005").intValue ());
		assertEquals (1, details1.getModifiedSkillValue ("US005").intValue ());
		
		// Now use the right RAT
		unitDef.setRangedAttackType ("RAT02");
		final ExpandedUnitDetails details2 = utils.expandUnitDetails (unit, null, null, null, players, mem, db);

		assertSame (unit, details2.getUnit ());
		assertTrue (details2.isMemoryUnit ());
		assertSame (unit, details2.getMemoryUnit ());
		assertSame (unitDef, details2.getUnitDefinition ());
		assertSame (unitType, details2.getUnitType ());
		assertSame (owningPlayer, details2.getOwningPlayer ());
		assertNull (details2.getWeaponGrade ());
		assertSame (rat2, details2.getRangedAttackType ());
		assertNull (details2.getBasicExperienceLevel ());
		assertNull (details2.getModifiedExperienceLevel ());
		assertSame (unitMagicRealm, details2.getModifiedUnitMagicRealmLifeformType ());

		assertTrue (details2.hasBasicSkill ("US001"));
		assertTrue (details2.hasModifiedSkill ("US001"));
		assertEquals (1, details2.getBasicSkillValue ("US001").intValue ());
		assertEquals (1, details2.getModifiedSkillValue ("US001").intValue ());
		
		assertTrue (details2.hasBasicSkill ("US002"));
		assertTrue (details2.hasModifiedSkill ("US002"));
		assertEquals (1, details2.getBasicSkillValue ("US002").intValue ());
		assertEquals (1, details2.getModifiedSkillValue ("US002").intValue ());
		
		assertTrue (details2.hasBasicSkill ("US003"));
		assertTrue (details2.hasModifiedSkill ("US003"));
		assertEquals (1, details2.getBasicSkillValue ("US003").intValue ());
		assertEquals (1, details2.getModifiedSkillValue ("US003").intValue ());
		
		assertTrue (details2.hasBasicSkill ("US004"));
		assertTrue (details2.hasModifiedSkill ("US004"));
		assertEquals (1, details2.getBasicSkillValue ("US004").intValue ());
		assertEquals (1 + 4, details2.getModifiedSkillValue ("US004").intValue ());

		assertTrue (details2.hasBasicSkill ("US005"));
		assertTrue (details2.hasModifiedSkill ("US005"));
		assertEquals (1, details2.getBasicSkillValue ("US005").intValue ());
		assertEquals (1, details2.getModifiedSkillValue ("US005").intValue ());
		
		// Attack with the right attackSkillID
		final ExpandedUnitDetails details3 = utils.expandUnitDetails (unit, null, "UA01", null, players, mem, db);

		assertSame (unit, details3.getUnit ());
		assertTrue (details3.isMemoryUnit ());
		assertSame (unit, details3.getMemoryUnit ());
		assertSame (unitDef, details3.getUnitDefinition ());
		assertSame (unitType, details3.getUnitType ());
		assertSame (owningPlayer, details3.getOwningPlayer ());
		assertNull (details3.getWeaponGrade ());
		assertSame (rat2, details3.getRangedAttackType ());
		assertNull (details3.getBasicExperienceLevel ());
		assertNull (details3.getModifiedExperienceLevel ());
		assertSame (unitMagicRealm, details3.getModifiedUnitMagicRealmLifeformType ());

		assertTrue (details3.hasBasicSkill ("US001"));
		assertTrue (details3.hasModifiedSkill ("US001"));
		assertEquals (1, details3.getBasicSkillValue ("US001").intValue ());
		assertEquals (1 + 1, details3.getModifiedSkillValue ("US001").intValue ());
		
		assertTrue (details3.hasBasicSkill ("US002"));
		assertTrue (details3.hasModifiedSkill ("US002"));
		assertEquals (1, details3.getBasicSkillValue ("US002").intValue ());
		assertEquals (1 + 2, details3.getModifiedSkillValue ("US002").intValue ());
		
		assertTrue (details3.hasBasicSkill ("US003"));
		assertTrue (details3.hasModifiedSkill ("US003"));
		assertEquals (1, details3.getBasicSkillValue ("US003").intValue ());
		assertEquals (1, details3.getModifiedSkillValue ("US003").intValue ());
		
		assertTrue (details3.hasBasicSkill ("US004"));
		assertTrue (details3.hasModifiedSkill ("US004"));
		assertEquals (1, details3.getBasicSkillValue ("US004").intValue ());
		assertEquals (1 + 4, details3.getModifiedSkillValue ("US004").intValue ());

		assertTrue (details3.hasBasicSkill ("US005"));
		assertTrue (details3.hasModifiedSkill ("US005"));
		assertEquals (1, details3.getBasicSkillValue ("US005").intValue ());
		assertEquals (1, details3.getModifiedSkillValue ("US005").intValue ());
		
		// Attack with the only attackSkillID that means we *don't* get the 3rd bonus
		final ExpandedUnitDetails details4 = utils.expandUnitDetails (unit, null, "UA02", null, players, mem, db);

		assertSame (unit, details4.getUnit ());
		assertTrue (details4.isMemoryUnit ());
		assertSame (unit, details4.getMemoryUnit ());
		assertSame (unitDef, details4.getUnitDefinition ());
		assertSame (unitType, details4.getUnitType ());
		assertSame (owningPlayer, details4.getOwningPlayer ());
		assertNull (details4.getWeaponGrade ());
		assertSame (rat2, details4.getRangedAttackType ());
		assertNull (details4.getBasicExperienceLevel ());
		assertNull (details4.getModifiedExperienceLevel ());
		assertSame (unitMagicRealm, details4.getModifiedUnitMagicRealmLifeformType ());

		assertTrue (details4.hasBasicSkill ("US001"));
		assertTrue (details4.hasModifiedSkill ("US001"));
		assertEquals (1, details4.getBasicSkillValue ("US001").intValue ());
		assertEquals (1, details4.getModifiedSkillValue ("US001").intValue ());
		
		assertTrue (details4.hasBasicSkill ("US002"));
		assertTrue (details4.hasModifiedSkill ("US002"));
		assertEquals (1, details4.getBasicSkillValue ("US002").intValue ());
		assertEquals (1, details4.getModifiedSkillValue ("US002").intValue ());
		
		assertTrue (details4.hasBasicSkill ("US003"));
		assertTrue (details4.hasModifiedSkill ("US003"));
		assertEquals (1, details4.getBasicSkillValue ("US003").intValue ());
		assertEquals (1, details4.getModifiedSkillValue ("US003").intValue ());
		
		assertTrue (details4.hasBasicSkill ("US004"));
		assertTrue (details4.hasModifiedSkill ("US004"));
		assertEquals (1, details4.getBasicSkillValue ("US004").intValue ());
		assertEquals (1 + 4, details4.getModifiedSkillValue ("US004").intValue ());

		assertTrue (details4.hasBasicSkill ("US005"));
		assertTrue (details4.hasModifiedSkill ("US005"));
		assertEquals (1, details4.getBasicSkillValue ("US005").intValue ());
		assertEquals (1, details4.getModifiedSkillValue ("US005").intValue ());
		
		// Attack with the right magicRealmID
		final ExpandedUnitDetails details5 = utils.expandUnitDetails (unit, null, null, "MB01", players, mem, db);

		assertSame (unit, details5.getUnit ());
		assertTrue (details5.isMemoryUnit ());
		assertSame (unit, details5.getMemoryUnit ());
		assertSame (unitDef, details5.getUnitDefinition ());
		assertSame (unitType, details5.getUnitType ());
		assertSame (owningPlayer, details5.getOwningPlayer ());
		assertNull (details5.getWeaponGrade ());
		assertSame (rat2, details5.getRangedAttackType ());
		assertNull (details5.getBasicExperienceLevel ());
		assertNull (details5.getModifiedExperienceLevel ());
		assertSame (unitMagicRealm, details5.getModifiedUnitMagicRealmLifeformType ());

		assertTrue (details5.hasBasicSkill ("US001"));
		assertTrue (details5.hasModifiedSkill ("US001"));
		assertEquals (1, details5.getBasicSkillValue ("US001").intValue ());
		assertEquals (1, details5.getModifiedSkillValue ("US001").intValue ());
		
		assertTrue (details5.hasBasicSkill ("US002"));
		assertTrue (details5.hasModifiedSkill ("US002"));
		assertEquals (1, details5.getBasicSkillValue ("US002").intValue ());
		assertEquals (1 + 2, details5.getModifiedSkillValue ("US002").intValue ());
		
		assertTrue (details5.hasBasicSkill ("US003"));
		assertTrue (details5.hasModifiedSkill ("US003"));
		assertEquals (1, details5.getBasicSkillValue ("US003").intValue ());
		assertEquals (1 + 3, details5.getModifiedSkillValue ("US003").intValue ());
		
		assertTrue (details5.hasBasicSkill ("US004"));
		assertTrue (details5.hasModifiedSkill ("US004"));
		assertEquals (1, details5.getBasicSkillValue ("US004").intValue ());
		assertEquals (1 + 4, details5.getModifiedSkillValue ("US004").intValue ());
		
		assertTrue (details5.hasBasicSkill ("US005"));
		assertTrue (details5.hasModifiedSkill ("US005"));
		assertEquals (1, details5.getBasicSkillValue ("US005").intValue ());
		assertEquals (1, details5.getModifiedSkillValue ("US005").intValue ());

		// Attack in combat
		unit.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		final ExpandedUnitDetails details6 = utils.expandUnitDetails (unit, null, null, null, players, mem, db);

		assertSame (unit, details6.getUnit ());
		assertTrue (details6.isMemoryUnit ());
		assertSame (unit, details6.getMemoryUnit ());
		assertSame (unitDef, details6.getUnitDefinition ());
		assertSame (unitType, details6.getUnitType ());
		assertSame (owningPlayer, details6.getOwningPlayer ());
		assertNull (details6.getWeaponGrade ());
		assertSame (rat2, details6.getRangedAttackType ());
		assertNull (details6.getBasicExperienceLevel ());
		assertNull (details6.getModifiedExperienceLevel ());
		assertSame (unitMagicRealm, details6.getModifiedUnitMagicRealmLifeformType ());

		assertTrue (details6.hasBasicSkill ("US001"));
		assertTrue (details6.hasModifiedSkill ("US001"));
		assertEquals (1, details6.getBasicSkillValue ("US001").intValue ());
		assertEquals (1, details6.getModifiedSkillValue ("US001").intValue ());
		
		assertTrue (details6.hasBasicSkill ("US002"));
		assertTrue (details6.hasModifiedSkill ("US002"));
		assertEquals (1, details6.getBasicSkillValue ("US002").intValue ());
		assertEquals (1, details6.getModifiedSkillValue ("US002").intValue ());
		
		assertTrue (details6.hasBasicSkill ("US003"));
		assertTrue (details6.hasModifiedSkill ("US003"));
		assertEquals (1, details6.getBasicSkillValue ("US003").intValue ());
		assertEquals (1, details6.getModifiedSkillValue ("US003").intValue ());
		
		assertTrue (details6.hasBasicSkill ("US004"));
		assertTrue (details6.hasModifiedSkill ("US004"));
		assertEquals (1, details6.getBasicSkillValue ("US004").intValue ());
		assertEquals (1 + 4, details6.getModifiedSkillValue ("US004").intValue ());

		assertTrue (details6.hasBasicSkill ("US005"));
		assertTrue (details6.hasModifiedSkill ("US005"));
		assertEquals (1, details6.getBasicSkillValue ("US005").intValue ());
		assertEquals (1 + 5, details6.getModifiedSkillValue ("US005").intValue ());
	}
	
	/**
	 * Tests the expandSkillList method calculating upkeep values
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testExpandUnitDetails_MemoryUnit_UpkeepValues () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm ("LTN");
		when (db.findUnit (eq ("UN001"), anyString ())).thenReturn (unitDef);
		
		final Pick unitMagicRealm = new Pick ();
		unitMagicRealm.setUnitTypeID ("N");
		when (db.findPick (eq ("LTN"), anyString ())).thenReturn (unitMagicRealm);
		
		final UnitType unitType = new UnitType ();
		unitType.setUnitTypeID ("N");
		when (db.findUnitType (eq ("N"), anyString ())).thenReturn (unitType);

		when (db.findUnitSkill (eq (CommonDatabaseConstants.UNIT_SKILL_ID_UNDEAD), anyString ())).thenReturn (new UnitSkillEx ());
		
		// Unit upkeeps
		final ProductionTypeAndUndoubledValue upkeep1 = new ProductionTypeAndUndoubledValue ();
		upkeep1.setProductionTypeID ("RE01");
		upkeep1.setUndoubledProductionValue (1);
		unitDef.getUnitUpkeep ().add (upkeep1);

		final ProductionTypeAndUndoubledValue upkeep2 = new ProductionTypeAndUndoubledValue ();
		upkeep2.setProductionTypeID ("RE02");
		upkeep2.setUndoubledProductionValue (5);
		unitDef.getUnitUpkeep ().add (upkeep2);
		
		// Create other lists
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		// Players
		final PlayerDescription owningPd = new PlayerDescription ();
		owningPd.setPlayerID (1);
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		
		final PlayerPublicDetails owningPlayer = new PlayerPublicDetails (owningPd, pub, null);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (eq (players), eq (owningPd.getPlayerID ()), anyString ())).thenReturn (owningPlayer);
		
		// Upkeep reduction from picks (summoner retort)
		final PlayerPickUtils pickUtils = mock (PlayerPickUtils.class);
		
		// Create test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitURN (1);
		unit.setUnitID ("UN001");
		unit.setOwningPlayerID (owningPd.getPlayerID ());
		unit.setStatus (UnitStatusID.ALIVE);
		unit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		mem.getUnit ().add (unit);

		// Set up object to test
		final UnitDetailsUtilsImpl unitDetailsUtils = new UnitDetailsUtilsImpl ();
		unitDetailsUtils.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		final UnitUtilsImpl utils = new UnitUtilsImpl ();
		utils.setUnitDetailsUtils (unitDetailsUtils);
		utils.setPlayerPickUtils (pickUtils);
		
		// Upkeep with no modifiers
		final ExpandedUnitDetails details1 = utils.expandUnitDetails (unit, null, null, null, players, mem, db);

		assertSame (unit, details1.getUnit ());
		assertTrue (details1.isMemoryUnit ());
		assertSame (unit, details1.getMemoryUnit ());
		assertSame (unitDef, details1.getUnitDefinition ());
		assertSame (unitType, details1.getUnitType ());
		assertSame (owningPlayer, details1.getOwningPlayer ());
		assertNull (details1.getWeaponGrade ());
		assertNull (details1.getRangedAttackType ());
		assertNull (details1.getBasicExperienceLevel ());
		assertNull (details1.getModifiedExperienceLevel ());
		assertSame (unitMagicRealm, details1.getModifiedUnitMagicRealmLifeformType ());
		
		assertEquals (1, details1.getBasicUpkeepValue ("RE01"));
		assertEquals (5, details1.getBasicUpkeepValue ("RE02"));
		assertEquals (1, details1.getModifiedUpkeepValue ("RE01"));
		assertEquals (5, details1.getModifiedUpkeepValue ("RE02"));

		// Undead normal units have no upkeep
		final UnitSkillAndValue undead = new UnitSkillAndValue ();
		undead.setUnitSkillID (CommonDatabaseConstants.UNIT_SKILL_ID_UNDEAD);
		unit.getUnitHasSkill ().add (undead);

		final ExpandedUnitDetails details2 = utils.expandUnitDetails (unit, null, null, null, players, mem, db);

		assertSame (unit, details2.getUnit ());
		assertTrue (details2.isMemoryUnit ());
		assertSame (unit, details2.getMemoryUnit ());
		assertSame (unitDef, details2.getUnitDefinition ());
		assertSame (unitType, details2.getUnitType ());
		assertSame (owningPlayer, details2.getOwningPlayer ());
		assertNull (details2.getWeaponGrade ());
		assertNull (details2.getRangedAttackType ());
		assertNull (details2.getBasicExperienceLevel ());
		assertNull (details2.getModifiedExperienceLevel ());
		assertSame (unitMagicRealm, details2.getModifiedUnitMagicRealmLifeformType ());
		
		assertEquals (1, details2.getBasicUpkeepValue ("RE01"));
		assertEquals (5, details2.getBasicUpkeepValue ("RE02"));
		assertEquals (0, details2.getModifiedUpkeepValue ("RE01"));
		assertEquals (0, details2.getModifiedUpkeepValue ("RE02"));
		
		// Undead summoned units have +50% upkeep
		unitType.setUndeadUpkeepPercentage (150);

		final ExpandedUnitDetails details3 = utils.expandUnitDetails (unit, null, null, null, players, mem, db);

		assertSame (unit, details3.getUnit ());
		assertTrue (details3.isMemoryUnit ());
		assertSame (unit, details3.getMemoryUnit ());
		assertSame (unitDef, details3.getUnitDefinition ());
		assertSame (unitType, details3.getUnitType ());
		assertSame (owningPlayer, details3.getOwningPlayer ());
		assertNull (details3.getWeaponGrade ());
		assertNull (details3.getRangedAttackType ());
		assertNull (details3.getBasicExperienceLevel ());
		assertNull (details3.getModifiedExperienceLevel ());
		assertSame (unitMagicRealm, details3.getModifiedUnitMagicRealmLifeformType ());
		
		assertEquals (1, details3.getBasicUpkeepValue ("RE01"));
		assertEquals (5, details3.getBasicUpkeepValue ("RE02"));
		assertEquals (1, details3.getModifiedUpkeepValue ("RE01"));
		assertEquals (7, details3.getModifiedUpkeepValue ("RE02"));		// 5 + 50% = 7.5, rounded down
		
		// 50% reduction from retorts
		when (pickUtils.totalProductionBonus (CommonDatabaseConstants.PRODUCTION_TYPE_ID_UNIT_UPKEEP_REDUCTION, "N", pub.getPick (), db)).thenReturn (70);
		
		final ExpandedUnitDetails details4 = utils.expandUnitDetails (unit, null, null, null, players, mem, db);

		assertSame (unit, details4.getUnit ());
		assertTrue (details4.isMemoryUnit ());
		assertSame (unit, details4.getMemoryUnit ());
		assertSame (unitDef, details4.getUnitDefinition ());
		assertSame (unitType, details4.getUnitType ());
		assertSame (owningPlayer, details4.getOwningPlayer ());
		assertNull (details4.getWeaponGrade ());
		assertNull (details4.getRangedAttackType ());
		assertNull (details4.getBasicExperienceLevel ());
		assertNull (details4.getModifiedExperienceLevel ());
		assertSame (unitMagicRealm, details4.getModifiedUnitMagicRealmLifeformType ());
		
		assertEquals (1, details4.getBasicUpkeepValue ("RE01"));
		assertEquals (5, details4.getBasicUpkeepValue ("RE02"));
		assertEquals (1, details4.getModifiedUpkeepValue ("RE01"));
		assertEquals (3, details4.getModifiedUpkeepValue ("RE02"));		// 70% of 7 = 4.9, rounded down so get a reduction of 4  
	}
	
	/**
	 * Tests the doesCombatAreaEffectApplyToUnit method on an available unit and a CAE with affects players = blank
	 * @throws RecordNotFoundException If we can't find the definition for the CAE
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Available_AffectsBlank () throws RecordNotFoundException
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
	 * @throws RecordNotFoundException If we can't find the definition for the CAE
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Available_AffectsAll () throws RecordNotFoundException
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
	 * @throws RecordNotFoundException If we can't find the definition for the CAE
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Available_AffectsCaster () throws RecordNotFoundException
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
	 * @throws RecordNotFoundException If we can't find the definition for the CAE
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Available_AffectsBothInCombat () throws RecordNotFoundException
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
	 * @throws RecordNotFoundException If we can't find the definition for the CAE
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Available_AffectsOpponent () throws RecordNotFoundException
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
	 * @throws RecordNotFoundException If we can't find the definition for the CAE
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Real_AffectsBlank () throws RecordNotFoundException
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
	 * @throws RecordNotFoundException If we can't find the definition for the CAE
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Real_AffectsAll () throws RecordNotFoundException
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
	 * @throws RecordNotFoundException If we can't find the definition for the CAE
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Real_AffectsCaster () throws RecordNotFoundException
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
	 * @throws RecordNotFoundException If we can't find the definition for the CAE
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Real_AffectsBothInCombat () throws RecordNotFoundException
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
	 * @throws RecordNotFoundException If we can't find the definition for the CAE
	 */
	@Test
	public final void testDoesCombatAreaEffectApplyToUnit_Real_AffectsOpponent () throws RecordNotFoundException
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
}