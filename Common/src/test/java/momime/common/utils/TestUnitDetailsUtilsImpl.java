package momime.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.database.AddsToSkill;
import momime.common.database.AddsToSkillValueType;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.database.Pick;
import momime.common.database.UnitEx;
import momime.common.database.UnitSkillAndValue;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillEx;
import momime.common.database.UnitTypeEx;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;

/**
 * Tests the UnitDetailsUtilsImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestUnitDetailsUtilsImpl
{
	/**
	 * Tests the expandMinimalUnitDetails method on a unit that doesn't get experience (like a summoned unit)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testExpandMinimalUnitDetails_NoExperience () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm ("MB01");
		when (db.findUnit ("UN001", "expandMinimalUnitDetails")).thenReturn (unitDef);
		
		final Pick magicRealm = new Pick ();
		magicRealm.setUnitTypeID ("S");
		when (db.findPick ("MB01", "expandMinimalUnitDetails")).thenReturn (magicRealm);
		
		final UnitTypeEx summoned = new UnitTypeEx ();
		when (db.findUnitType ("S", "expandMinimalUnitDetails")).thenReturn (summoned);
		
		// Owning player
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();

		final PlayerPublicDetails owningPlayer = new PlayerPublicDetails (null, null, null);
		when (multiplayerSessionUtils.findPlayerWithID (players, 2, "expandMinimalUnitDetails")).thenReturn (owningPlayer);
				
		// Owning wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		
		final KnownWizardDetails owningWizard = new KnownWizardDetails ();
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), 2, "expandMinimalUnitDetails")).thenReturn (owningWizard);
		
		// Unit
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID ("UN001");
		unit.setOwningPlayerID (2);

		final UnitSkillAndValue valuedSkill = new UnitSkillAndValue ();
		valuedSkill.setUnitSkillID ("US001");
		valuedSkill.setUnitSkillValue (5);
		unit.getUnitHasSkill ().add (valuedSkill);
		
		final UnitSkillAndValue valuelessSkill = new UnitSkillAndValue ();
		valuelessSkill.setUnitSkillID ("US002");
		unit.getUnitHasSkill ().add (valuelessSkill);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		utils.setMultiplayerSessionUtils (multiplayerSessionUtils);
		utils.setKnownWizardUtils (knownWizardUtils);
		
		// Run method
		final MinimalUnitDetails mu = utils.expandMinimalUnitDetails (unit, players, mem, db);
		
		// Check results
		assertSame (unit, mu.getUnit ());
		assertSame (unitDef, mu.getUnitDefinition ());
		assertSame (summoned, mu.getUnitType ());
		assertSame (owningPlayer, mu.getOwningPlayer ());
		assertSame (owningWizard, mu.getOwningWizard ());
		assertNull (mu.getBasicExperienceLevel ());
		assertNull (mu.getModifiedExperienceLevel ());
		
		assertEquals (2, mu.getBasicSkillValues ().size ());
		assertTrue (mu.hasBasicSkill ("US001"));
		assertTrue (mu.hasBasicSkill ("US002"));
		assertEquals (5, mu.getBasicSkillValue ("US001"));
		assertNull (mu.getBasicSkillValue ("US002"));
	}
	
	/**
	 * Tests the expandMinimalUnitDetails method on a unit that has experience, with no modifiers (Warlord + Crusade)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testExpandMinimalUnitDetails_BasicExperience () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		when (db.findUnit ("UN001", "expandMinimalUnitDetails")).thenReturn (unitDef);
		
		final Pick lifeformType = new Pick ();
		lifeformType.setUnitTypeID ("N");
		when (db.findPick (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL, "expandMinimalUnitDetails")).thenReturn (lifeformType);

		final ExperienceLevel veteran = new ExperienceLevel ();
		veteran.setLevelNumber (2);
		veteran.setExperienceRequired (60);

		final ExperienceLevel elite = new ExperienceLevel ();
		elite.setLevelNumber (3);
		elite.setExperienceRequired (120);
		
		final ExperienceLevel ultraElite = new ExperienceLevel ();
		ultraElite.setLevelNumber (4);
		
		final UnitTypeEx normalUnit = new UnitTypeEx ();
		normalUnit.getExperienceLevel ().add (veteran);
		normalUnit.getExperienceLevel ().add (elite);
		normalUnit.getExperienceLevel ().add (ultraElite);
		when (db.findUnitType ("N", "expandMinimalUnitDetails")).thenReturn (normalUnit);
		
		// Owning player
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();

		final PlayerPublicDetails owningPlayer = new PlayerPublicDetails (null, null, null);
		when (multiplayerSessionUtils.findPlayerWithID (players, 2, "expandMinimalUnitDetails")).thenReturn (owningPlayer);
				
		// Owning wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		
		final KnownWizardDetails owningWizard = new KnownWizardDetails ();
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), 2, "expandMinimalUnitDetails")).thenReturn (owningWizard);
		
		// Unit
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID ("UN001");
		unit.setOwningPlayerID (2);

		final UnitSkillAndValue valuedSkill = new UnitSkillAndValue ();
		valuedSkill.setUnitSkillID ("US001");
		valuedSkill.setUnitSkillValue (5);
		unit.getUnitHasSkill ().add (valuedSkill);
		
		final UnitSkillAndValue valuelessSkill = new UnitSkillAndValue ();
		valuelessSkill.setUnitSkillID ("US002");
		unit.getUnitHasSkill ().add (valuelessSkill);
		
		final UnitSkillAndValue experience = new UnitSkillAndValue ();
		experience.setUnitSkillID (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE);
		experience.setUnitSkillValue (60);
		unit.getUnitHasSkill ().add (experience);
		
		// Set up object to test
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		final MemoryCombatAreaEffectUtils memoryCombatAreaEffectUtils = mock (MemoryCombatAreaEffectUtils.class);
		
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		utils.setMultiplayerSessionUtils (multiplayerSessionUtils);
		utils.setKnownWizardUtils (knownWizardUtils);
		utils.setPlayerPickUtils (playerPickUtils);
		utils.setMemoryCombatAreaEffectUtils (memoryCombatAreaEffectUtils);
		
		// Run method
		final MinimalUnitDetails mu = utils.expandMinimalUnitDetails (unit, players, mem, db);
		
		// Check results
		assertSame (unit, mu.getUnit ());
		assertSame (unitDef, mu.getUnitDefinition ());
		assertSame (normalUnit, mu.getUnitType ());
		assertSame (owningPlayer, mu.getOwningPlayer ());
		assertSame (owningWizard, mu.getOwningWizard ());
		assertSame (veteran, mu.getBasicExperienceLevel ());
		assertSame (veteran, mu.getModifiedExperienceLevel ());
		
		assertEquals (3, mu.getBasicSkillValues ().size ());
		assertTrue (mu.hasBasicSkill (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE));
		assertTrue (mu.hasBasicSkill ("US002"));
		assertTrue (mu.hasBasicSkill ("US002"));
		assertEquals (60, mu.getBasicSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE));
		assertEquals (5, mu.getBasicSkillValue ("US001"));
		assertNull (mu.getBasicSkillValue ("US002"));
	}
	
	/**
	 * Tests the expandMinimalUnitDetails method on a unit that has heroism cast on it to raise its experience
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testExpandMinimalUnitDetails_Heroism () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		when (db.findUnit ("UN001", "expandMinimalUnitDetails")).thenReturn (unitDef);
		
		final Pick lifeformType = new Pick ();
		lifeformType.setUnitTypeID ("N");
		when (db.findPick (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL, "expandMinimalUnitDetails")).thenReturn (lifeformType);

		final ExperienceLevel veteran = new ExperienceLevel ();
		veteran.setLevelNumber (2);
		veteran.setExperienceRequired (60);

		final ExperienceLevel elite = new ExperienceLevel ();
		elite.setLevelNumber (3);
		elite.setExperienceRequired (120);
		
		final ExperienceLevel ultraElite = new ExperienceLevel ();
		ultraElite.setLevelNumber (4);
		
		final UnitTypeEx normalUnit = new UnitTypeEx ();
		normalUnit.getExperienceLevel ().add (veteran);
		normalUnit.getExperienceLevel ().add (elite);
		normalUnit.getExperienceLevel ().add (ultraElite);
		when (db.findUnitType ("N", "expandMinimalUnitDetails")).thenReturn (normalUnit);
		
		final AddsToSkill heroismAddsToExperience = new AddsToSkill ();
		heroismAddsToExperience.setAddsToSkillID (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE);
		heroismAddsToExperience.setAddsToSkillValueType (AddsToSkillValueType.LOCK);
		heroismAddsToExperience.setAddsToSkillValue (120);
		
		final UnitSkillEx heroismSkill = new UnitSkillEx ();
		heroismSkill.getAddsToSkill ().add (heroismAddsToExperience);
		when (db.findUnitSkill ("US001", "expandMinimalUnitDetails")).thenReturn (heroismSkill);
		
		// Owning player
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();

		final PlayerPublicDetails owningPlayer = new PlayerPublicDetails (null, null, null);
		when (multiplayerSessionUtils.findPlayerWithID (players, 2, "expandMinimalUnitDetails")).thenReturn (owningPlayer);
				
		// Owning wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		
		final KnownWizardDetails owningWizard = new KnownWizardDetails ();
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), 2, "expandMinimalUnitDetails")).thenReturn (owningWizard);
		
		// Unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitURN (6);
		unit.setUnitID ("UN001");
		unit.setOwningPlayerID (2);

		final UnitSkillAndValue valuedSkill = new UnitSkillAndValue ();
		valuedSkill.setUnitSkillID ("US001");
		valuedSkill.setUnitSkillValue (5);
		unit.getUnitHasSkill ().add (valuedSkill);
		
		final UnitSkillAndValue valuelessSkill = new UnitSkillAndValue ();
		valuelessSkill.setUnitSkillID ("US002");
		unit.getUnitHasSkill ().add (valuelessSkill);
		
		final UnitSkillAndValue experience = new UnitSkillAndValue ();
		experience.setUnitSkillID (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE);
		experience.setUnitSkillValue (60);
		unit.getUnitHasSkill ().add (experience);
		
		// Heroism cast on it
		final MemoryMaintainedSpell heroism = new MemoryMaintainedSpell ();
		heroism.setUnitURN (6);
		heroism.setUnitSkillID ("US001");
		mem.getMaintainedSpell ().add (heroism);
		
		// Set up object to test
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		final MemoryCombatAreaEffectUtils memoryCombatAreaEffectUtils = mock (MemoryCombatAreaEffectUtils.class);
		
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		utils.setMultiplayerSessionUtils (multiplayerSessionUtils);
		utils.setKnownWizardUtils (knownWizardUtils);
		utils.setPlayerPickUtils (playerPickUtils);
		utils.setMemoryCombatAreaEffectUtils (memoryCombatAreaEffectUtils);
		
		// Run method
		final MinimalUnitDetails mu = utils.expandMinimalUnitDetails (unit, players, mem, db);
		
		// Check results
		assertSame (unit, mu.getUnit ());
		assertSame (unitDef, mu.getUnitDefinition ());
		assertSame (normalUnit, mu.getUnitType ());
		assertSame (owningPlayer, mu.getOwningPlayer ());
		assertSame (owningWizard, mu.getOwningWizard ());
		assertSame (elite, mu.getBasicExperienceLevel ());
		assertSame (elite, mu.getModifiedExperienceLevel ());
		
		assertEquals (3, mu.getBasicSkillValues ().size ());
		assertTrue (mu.hasBasicSkill (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE));
		assertTrue (mu.hasBasicSkill ("US002"));
		assertTrue (mu.hasBasicSkill ("US002"));
		assertEquals (60, mu.getBasicSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE));
		assertEquals (5, mu.getBasicSkillValue ("US001"));
		assertNull (mu.getBasicSkillValue ("US002"));
	}
	
	/**
	 * Tests the expandMinimalUnitDetails method on a unit that has experience levels raised by Warlord + Crusade
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testExpandMinimalUnitDetails_ModifiedExperience () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		when (db.findUnit ("UN001", "expandMinimalUnitDetails")).thenReturn (unitDef);
		
		final Pick lifeformType = new Pick ();
		lifeformType.setUnitTypeID ("N");
		when (db.findPick (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL, "expandMinimalUnitDetails")).thenReturn (lifeformType);

		final ExperienceLevel veteran = new ExperienceLevel ();
		veteran.setLevelNumber (2);
		veteran.setExperienceRequired (60);

		final ExperienceLevel elite = new ExperienceLevel ();
		elite.setLevelNumber (3);
		elite.setExperienceRequired (120);
		
		final ExperienceLevel ultraElite = new ExperienceLevel ();
		ultraElite.setLevelNumber (4);
		
		final UnitTypeEx normalUnit = new UnitTypeEx ();
		normalUnit.getExperienceLevel ().add (veteran);
		normalUnit.getExperienceLevel ().add (elite);
		normalUnit.getExperienceLevel ().add (ultraElite);
		when (db.findUnitType ("N", "expandMinimalUnitDetails")).thenReturn (normalUnit);
		
		// Owning player
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();

		final PlayerPublicDetails owningPlayer = new PlayerPublicDetails (null, null, null);
		when (multiplayerSessionUtils.findPlayerWithID (players, 2, "expandMinimalUnitDetails")).thenReturn (owningPlayer);
				
		// Owning wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		
		final KnownWizardDetails owningWizard = new KnownWizardDetails ();
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), 2, "expandMinimalUnitDetails")).thenReturn (owningWizard);
		
		// Unit
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID ("UN001");
		unit.setOwningPlayerID (2);

		final UnitSkillAndValue valuedSkill = new UnitSkillAndValue ();
		valuedSkill.setUnitSkillID ("US001");
		valuedSkill.setUnitSkillValue (5);
		unit.getUnitHasSkill ().add (valuedSkill);
		
		final UnitSkillAndValue valuelessSkill = new UnitSkillAndValue ();
		valuelessSkill.setUnitSkillID ("US002");
		unit.getUnitHasSkill ().add (valuelessSkill);
		
		final UnitSkillAndValue experience = new UnitSkillAndValue ();
		experience.setUnitSkillID (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE);
		experience.setUnitSkillValue (60);
		unit.getUnitHasSkill ().add (experience);

		// Warlord
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		when (playerPickUtils.getQuantityOfPick (owningWizard.getPick (), CommonDatabaseConstants.RETORT_ID_WARLORD)).thenReturn (1);
		
		// Crusade
		final MemoryCombatAreaEffectUtils memoryCombatAreaEffectUtils = mock (MemoryCombatAreaEffectUtils.class);
		when (memoryCombatAreaEffectUtils.findCombatAreaEffect (mem.getCombatAreaEffect (), null, CommonDatabaseConstants.COMBAT_AREA_EFFECT_CRUSADE, 2)).thenReturn (new MemoryCombatAreaEffect ());
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		utils.setMultiplayerSessionUtils (multiplayerSessionUtils);
		utils.setKnownWizardUtils (knownWizardUtils);
		utils.setPlayerPickUtils (playerPickUtils);
		utils.setMemoryCombatAreaEffectUtils (memoryCombatAreaEffectUtils);
		
		// Run method
		final MinimalUnitDetails mu = utils.expandMinimalUnitDetails (unit, players, mem, db);
		
		// Check results
		assertSame (unit, mu.getUnit ());
		assertSame (unitDef, mu.getUnitDefinition ());
		assertSame (normalUnit, mu.getUnitType ());
		assertSame (owningPlayer, mu.getOwningPlayer ());
		assertSame (owningWizard, mu.getOwningWizard ());
		assertSame (veteran, mu.getBasicExperienceLevel ());
		assertSame (ultraElite, mu.getModifiedExperienceLevel ());
		
		assertEquals (3, mu.getBasicSkillValues ().size ());
		assertTrue (mu.hasBasicSkill (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE));
		assertTrue (mu.hasBasicSkill ("US002"));
		assertTrue (mu.hasBasicSkill ("US002"));
		assertEquals (60, mu.getBasicSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE));
		assertEquals (5, mu.getBasicSkillValue ("US001"));
		assertNull (mu.getBasicSkillValue ("US002"));
	}
	
	/**
	 * Tests the addSkillBonus method when we don't have the necessary requisite skill to get the bonus
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillBonus_DontHaveSkill () throws Exception
	{
		// Details about skill bonus to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.ADD_FIXED);
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.UNIT_DOES_NOT_HAVE_SKILL, utils.addSkillBonus
			(null, null, addsToSkill, null, modifiedSkillValues, null, null, null, null));
	}

	/**
	 * Tests the addSkillBonus method when we skill modification is a penalty rather than a bonus, so is ignored
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillBonus_Penalty () throws Exception
	{
		// Details about skill bonus to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.LOCK);
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		modifiedSkillValues.put ("US001", new UnitSkillValueBreakdown (null));
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.INCORRECT_TYPE_OF_ADJUSTMENT, utils.addSkillBonus (null, null, addsToSkill, null, modifiedSkillValues, null, null, null, null));
	}

	/**
	 * Tests the addSkillBonus method in the situation where a fixed value gets added to a skill with no conditions that restrict it from being added
	 * and we previously had none of that skill component
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillBonus_Fixed_NewComponent () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill bonus to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.ADD_FIXED);
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.APPLIES, utils.addSkillBonus (mu, null, addsToSkill, null, modifiedSkillValues, null, null, null, null));
		
		// Check results
		assertEquals (1, breakdown.getComponents ().size ());
		assertEquals (2, breakdown.getComponents ().get (UnitSkillComponent.SPELL_EFFECTS));
	}

	/**
	 * Tests the addSkillBonus method in the situation where a fixed value gets added to a skill with no conditions that restrict it from being added
	 * and we already had some of that skill component
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillBonus_Fixed_AddToComponent () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill bonus to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.ADD_FIXED);
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		breakdown.getComponents ().put (UnitSkillComponent.SPELL_EFFECTS, 3);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.APPLIES, utils.addSkillBonus (mu, null, addsToSkill, null, modifiedSkillValues, null, null, null, null));
		
		// Check results
		assertEquals (1, breakdown.getComponents ().size ());
		assertEquals (5, breakdown.getComponents ().get (UnitSkillComponent.SPELL_EFFECTS));
	}

	/**
	 * Tests the addSkillBonus method in the situation where a fixed value gets added to a skill with no conditions that restrict it from being added
	 * and we override the component it gets recorded as, like happens with weapon grades and CAEs
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillBonus_Fixed_OverrideComponent () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill bonus to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.ADD_FIXED);
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.APPLIES, utils.addSkillBonus (mu, null, addsToSkill, UnitSkillComponent.WEAPON_GRADE, modifiedSkillValues, null, null, null, null));
		
		// Check results
		assertEquals (1, breakdown.getComponents ().size ());
		assertEquals (2, breakdown.getComponents ().get (UnitSkillComponent.WEAPON_GRADE));
	}

	/**
	 * Tests the addSkillBonus method in the situation where we raise a skill we do have to a minimum value
	 * and we already had some of that skill component
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillBonus_Minimum_Applies () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill bonus to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (6);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.MINIMUM);
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 1);
		breakdown.getComponents ().put (UnitSkillComponent.SPELL_EFFECTS, 1);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.APPLIES, utils.addSkillBonus (mu, null, addsToSkill, null, modifiedSkillValues, null, null, null, null));
		
		// Check results
		assertEquals (2, breakdown.getComponents ().size ());
		assertEquals (1, breakdown.getComponents ().get (UnitSkillComponent.BASIC));
		assertEquals (5, breakdown.getComponents ().get (UnitSkillComponent.SPELL_EFFECTS));
	}
	
	/**
	 * Tests the addSkillBonus method in the situation where we raise a skill we do have to a minimum value, but we already have the skill at least that value
	 * and we already had some of that skill component
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillBonus_Minimum_DoesNotApplies () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill bonus to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.MINIMUM);
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 4);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method - it still returns APPLIES because the bonus would apply, we just don't need it because we already have the skill value higher
		assertEquals (AddSkillBonusResult.APPLIES, utils.addSkillBonus (mu, null, addsToSkill, null, modifiedSkillValues, null, null, null, null));
		
		// Check results
		assertEquals (1, breakdown.getComponents ().size ());
		assertEquals (4, breakdown.getComponents ().get (UnitSkillComponent.BASIC));
	}
	
	/**
	 * Tests the addSkillBonus method in the situation where the amount that gets added is a division of the unit's level, like happens with hero skills
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillBonus_Divisor_NormalSkill () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		final ExperienceLevel expLvl = new ExperienceLevel ();
		expLvl.setLevelNumber (5);
		when (mu.getModifiedExperienceLevel ()).thenReturn (expLvl);
		
		// Details about skill bonus to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.ADD_DIVISOR);
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();

		final UnitSkillValueBreakdown skillToDivide = new UnitSkillValueBreakdown (null);
		modifiedSkillValues.put ("US002", skillToDivide);
		skillToDivide.getComponents ().put (UnitSkillComponent.BASIC, 1);		// Regular version of skill like Arcane Power
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.APPLIES, utils.addSkillBonus (mu, "US002", addsToSkill, null, modifiedSkillValues, null, null, null, null));
		
		// Check results
		assertEquals (1, breakdown.getComponents ().size ());
		assertEquals (3, breakdown.getComponents ().get (UnitSkillComponent.HERO_SKILLS));		// Level (5+1) / 2 divisor = 3
	}

	/**
	 * Tests the addSkillBonus method in the situation where the amount that gets added is a division of the unit's level,
	 * and the hero has the super version of the skill
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillBonus_Divisor_SuperSkill () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		final ExperienceLevel expLvl = new ExperienceLevel ();
		expLvl.setLevelNumber (5);
		when (mu.getModifiedExperienceLevel ()).thenReturn (expLvl);
		
		// Details about skill bonus to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.ADD_DIVISOR);
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();

		final UnitSkillValueBreakdown skillToDivide = new UnitSkillValueBreakdown (null);
		modifiedSkillValues.put ("US002", skillToDivide);
		skillToDivide.getComponents ().put (UnitSkillComponent.BASIC, 2);		// Super version of skill like Super Arcane Power
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.APPLIES, utils.addSkillBonus (mu, "US002", addsToSkill, null, modifiedSkillValues, null, null, null, null));
		
		// Check results
		assertEquals (1, breakdown.getComponents ().size ());
		assertEquals (4, breakdown.getComponents ().get (UnitSkillComponent.HERO_SKILLS));		// Level ((5+1) * 1.5) / 2 divisor = 4.5 rounded down
	}

	/**
	 * Tests the addSkillBonus method in the situation where a fixed value gets added and the value that gets added is taken from the skill value
	 * rather than defined against the bonus, as happens with skills like Holy Bonus
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillBonus_Fixed_ValueFromSkill () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill bonus to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.ADD_FIXED);
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		modifiedSkillValues.put ("US001", breakdown);

		final UnitSkillValueBreakdown bonusSkill = new UnitSkillValueBreakdown (null);
		modifiedSkillValues.put ("US002", bonusSkill);
		bonusSkill.getComponents ().put (UnitSkillComponent.BASIC, 2);		// So like Holy Bonus 2
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.APPLIES, utils.addSkillBonus (mu, "US002", addsToSkill, null, modifiedSkillValues, null, null, null, null));
		
		// Check results
		assertEquals (1, breakdown.getComponents ().size ());
		assertEquals (2, breakdown.getComponents ().get (UnitSkillComponent.SPELL_EFFECTS));
	}

	/**
	 * Tests the addSkillBonus method in the situation where a fixed value gets added and the value that gets added is taken from the skill value
	 * of another unit in the stack rather than defined against the bonus, as happens with skills like Holy Bonus
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillBonus_Fixed_ValueFromStackedSkill () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill bonus to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAffectsEntireStack (true);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.ADD_FIXED);
		
		// Unit stack skills
		final Map<String, Integer> unitStackSkills = new HashMap<String, Integer> ();
		unitStackSkills.put ("US002", 2);
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		modifiedSkillValues.put ("US001", breakdown);

		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.APPLIES, utils.addSkillBonus (mu, "US002", addsToSkill, null, modifiedSkillValues, unitStackSkills, null, null, null));
		
		// Check results
		assertEquals (1, breakdown.getComponents ().size ());
		assertEquals (2, breakdown.getComponents ().get (UnitSkillComponent.STACK));
	}

	/**
	 * Tests the addSkillBonus method in the situation where the amount that gets added is a division of the skill level of another unit in the stack,
	 * like heroes Leadership ability giving +attack to the entire stack, depending on the level of the hero
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillBonus_Divisor_ValueFromStack () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill bonus to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAffectsEntireStack (true);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.ADD_DIVISOR);
		
		// Unit stack skills
		final Map<String, Integer> unitStackSkills = new HashMap<String, Integer> ();
		unitStackSkills.put ("US002", 12);		// This is already set to hero level * 2 for basic version and * 3 for super version
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();

		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.APPLIES, utils.addSkillBonus (mu, "US002", addsToSkill, null, modifiedSkillValues, unitStackSkills, null, null, null));
		
		// Check results
		assertEquals (1, breakdown.getComponents ().size ());
		assertEquals (3, breakdown.getComponents ().get (UnitSkillComponent.STACK));		// 12 is doubled so 6, then / 2 from the skill
	}

	/**
	 * Tests the addSkillBonus method in the situation where an enemy unit modifies our skill value by a fixed amount, like when trying to
	 * hit an invisible unit, the invisible unit reduces your + to hit by 1
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillBonus_Fixed_Penalty () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill bonus to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (-2);
		addsToSkill.setPenaltyToEnemy (true);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.ADD_FIXED);
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.APPLIES, utils.addSkillBonus (mu, null, addsToSkill, null, modifiedSkillValues, null, null, null, null));
		
		// Check results
		assertEquals (1, breakdown.getComponents ().size ());
		assertEquals (-2, breakdown.getComponents ().get (UnitSkillComponent.PENALTIES));
	}

	/**
	 * Tests the addSkillBonus method when the bonus only applies to units in combat, and we are in combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillBonus_OnlyInCombat_Applies () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		when (mu.isMemoryUnit ()).thenReturn (true);
		when (mu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		
		// Details about skill bonus to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.ADD_FIXED);
		addsToSkill.setOnlyInCombat (true);
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.APPLIES, utils.addSkillBonus (mu, null, addsToSkill, null, modifiedSkillValues, null, null, null, null));
		
		// Check results
		assertEquals (1, breakdown.getComponents ().size ());
		assertEquals (2, breakdown.getComponents ().get (UnitSkillComponent.SPELL_EFFECTS));
	}

	/**
	 * Tests the addSkillBonus method when the bonus only applies to units in combat, and we aren't in combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillBonus_OnlyInCombat_NotApplies () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill bonus to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.ADD_FIXED);
		addsToSkill.setOnlyInCombat (true);
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.NOT_IN_COMBAT, utils.addSkillBonus (mu, null, addsToSkill, null, modifiedSkillValues, null, null, null, null));
		
		// Check results
		assertEquals (0, breakdown.getComponents ().size ());
	}

	/**
	 * Tests the addSkillBonus method when the bonus has a skill based restriction about the type of incoming attack, but we don't have that info 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillBonus_NoInfoAboutAttack_SkillRequired () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill bonus to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.ADD_FIXED);
		addsToSkill.setOnlyVersusAttacksFromSkillID ("US003");
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.NO_INFO_ABOUT_INCOMING_ATTACK, utils.addSkillBonus (mu, null, addsToSkill, null, modifiedSkillValues, null, null, null, null));
		
		// Check results
		assertEquals (0, breakdown.getComponents ().size ());
	}

	/**
	 * Tests the addSkillBonus method when the bonus has a magic realm based restriction about the type of incoming attack, but we don't have that info 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillBonus_NoInfoAboutAttack_MagicRealmRequired () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill bonus to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.ADD_FIXED);
		addsToSkill.setOnlyVersusAttacksFromMagicRealmID ("MB01");
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.NO_INFO_ABOUT_INCOMING_ATTACK, utils.addSkillBonus (mu, null, addsToSkill, null, modifiedSkillValues, null, null, null, null));
		
		// Check results
		assertEquals (0, breakdown.getComponents ().size ());
	}

	/**
	 * Tests the addSkillBonus method when the bonus only applies against a specific type of attack, and it does match
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillBonus_OnlyVersusSpecificSkill_Applies () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill bonus to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.ADD_FIXED);
		addsToSkill.setOnlyVersusAttacksFromSkillID ("US003");
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.APPLIES, utils.addSkillBonus (mu, null, addsToSkill, null, modifiedSkillValues, null, "US003", null, null));
		
		// Check results
		assertEquals (1, breakdown.getComponents ().size ());
		assertEquals (2, breakdown.getComponents ().get (UnitSkillComponent.SPELL_EFFECTS));
	}

	/**
	 * Tests the addSkillBonus method when the bonus only applies against a specific type of attack, and it does not match
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillBonus_OnlyVersusSpecificSkill_NotApplies () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill bonus to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.ADD_FIXED);
		addsToSkill.setOnlyVersusAttacksFromSkillID ("US003");
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.WRONG_ATTACK_SKILL, utils.addSkillBonus (mu, null, addsToSkill, null, modifiedSkillValues, null, "US004", null, null));
		
		// Check results
		assertEquals (0, breakdown.getComponents ().size ());
	}

	/**
	 * Tests the addSkillBonus method when the bonus only applies against a specific type of attack, and its a magic based attack instead, so bonus does not apply
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillBonus_OnlyVersusSpecificSkill_SpellsDontApply () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill bonus to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.ADD_FIXED);
		addsToSkill.setOnlyVersusAttacksFromSkillID ("US003");
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.WRONG_ATTACK_SKILL, utils.addSkillBonus (mu, null, addsToSkill, null, modifiedSkillValues, null, null, "MB01", null));
		
		// Check results
		assertEquals (0, breakdown.getComponents ().size ());
	}
	
	/**
	 * Tests the addSkillBonus method when the bonus applies against all types of attack EXCEPT one specific attack, and its a different attack, so bonus applies
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillBonus_VersusAllExceptSpecificSkill_Applies () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill bonus to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.ADD_FIXED);
		addsToSkill.setOnlyVersusAttacksFromSkillID ("US003");
		addsToSkill.setNegateOnlyVersusAttacksFromSkillID (true);
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.APPLIES, utils.addSkillBonus (mu, null, addsToSkill, null, modifiedSkillValues, null, "US004", null, null));
		
		// Check results
		assertEquals (1, breakdown.getComponents ().size ());
		assertEquals (2, breakdown.getComponents ().get (UnitSkillComponent.SPELL_EFFECTS));
	}

	/**
	 * Tests the addSkillBonus method when the bonus applies against all types of attack EXCEPT one specific attack, and its that attack, so bonus does not apply
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillBonus_VersusAllExceptSpecificSkill_NotApplies () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill bonus to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.ADD_FIXED);
		addsToSkill.setOnlyVersusAttacksFromSkillID ("US003");
		addsToSkill.setNegateOnlyVersusAttacksFromSkillID (true);
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.WRONG_ATTACK_SKILL, utils.addSkillBonus (mu, null, addsToSkill, null, modifiedSkillValues, null, "US003", null, null));
		
		// Check results
		assertEquals (0, breakdown.getComponents ().size ());
	}

	/**
	 * Tests the addSkillBonus method when the bonus applies against all types of attack EXCEPT one specific attack,
	 * and that includes magic based attacks, so bonus does apply (Large Shield does give +2 defence against spells like Fire Bolt)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillBonus_VersusAllExceptSpecificSkill_SpellsDoApply () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill bonus to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.ADD_FIXED);
		addsToSkill.setOnlyVersusAttacksFromSkillID ("US003");
		addsToSkill.setNegateOnlyVersusAttacksFromSkillID (true);
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.APPLIES, utils.addSkillBonus (mu, null, addsToSkill, null, modifiedSkillValues, null, null, "MB01", null));
		
		// Check results
		assertEquals (1, breakdown.getComponents ().size ());
		assertEquals (2, breakdown.getComponents ().get (UnitSkillComponent.SPELL_EFFECTS));
	}

	/**
	 * Tests the addSkillBonus method when the bonus only applies against attacks associated with a particular magic realm, and it does match
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillBonus_OnlyVersusSpecificMagicRealm_Applies () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill bonus to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.ADD_FIXED);
		addsToSkill.setOnlyVersusAttacksFromMagicRealmID ("MB01");
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.APPLIES, utils.addSkillBonus (mu, null, addsToSkill, null, modifiedSkillValues, null, null, "MB01", null));
		
		// Check results
		assertEquals (1, breakdown.getComponents ().size ());
		assertEquals (2, breakdown.getComponents ().get (UnitSkillComponent.SPELL_EFFECTS));
	}

	/**
	 * Tests the addSkillBonus method when the bonus only applies against attacks associated with a particular magic realm, and it does not match
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillBonus_OnlyVersusSpecificMagicRealm_NotApplies () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill bonus to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.ADD_FIXED);
		addsToSkill.setOnlyVersusAttacksFromMagicRealmID ("MB01");
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.WRONG_ATTACK_MAGIC_REALM, utils.addSkillBonus (mu, null, addsToSkill, null, modifiedSkillValues, null, null, "MB02", null));
		
		// Check results
		assertEquals (0, breakdown.getComponents ().size ());
	}

	/**
	 * Tests the addSkillBonus method when the bonus only applies to creates of a particular magic realm (nothing to do with the magic realm of the attack)
	 * and it does match
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillBonus_OnlyCreaturesOfMagicRealm_Applies () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill bonus to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.ADD_FIXED);
		addsToSkill.setOnlyAppliesToMagicRealmID ("MB01");
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.APPLIES, utils.addSkillBonus (mu, null, addsToSkill, null, modifiedSkillValues, null, null, null, "MB01"));
		
		// Check results
		assertEquals (1, breakdown.getComponents ().size ());
		assertEquals (2, breakdown.getComponents ().get (UnitSkillComponent.SPELL_EFFECTS));
	}

	/**
	 * Tests the addSkillBonus method when the bonus only applies to creates of a particular magic realm (nothing to do with the magic realm of the attack)
	 * and it does not match
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillBonus_OnlyCreaturesOfMagicRealm_NotApplies () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill bonus to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.ADD_FIXED);
		addsToSkill.setOnlyAppliesToMagicRealmID ("MB01");
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.WRONG_MAGIC_REALM_LIFEFORM_TYPE_ID, utils.addSkillBonus
			(mu, null, addsToSkill, null, modifiedSkillValues, null, null, null, "MB02"));
		
		// Check results
		assertEquals (0, breakdown.getComponents ().size ());
	}

	/**
	 * Tests the addSkillBonus method when the bonus only applies to certain ranged attack types, which we have
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillBonus_OnlySpecificRAT_Applies () throws Exception
	{
		// Unit we are calculating stats for
		final UnitEx unitDef = new UnitEx ();
		unitDef.setRangedAttackType ("RAT01");
		
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		when (mu.getUnitDefinition ()).thenReturn (unitDef);
		
		// Details about skill bonus to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.ADD_FIXED);
		addsToSkill.setRangedAttackTypeID ("RAT01");
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.APPLIES, utils.addSkillBonus (mu, null, addsToSkill, null, modifiedSkillValues, null, null, null, "MB01"));
		
		// Check results
		assertEquals (1, breakdown.getComponents ().size ());
		assertEquals (2, breakdown.getComponents ().get (UnitSkillComponent.SPELL_EFFECTS));
	}

	/**
	 * Tests the addSkillBonus method when the bonus only applies to certain ranged attack types, which we don't have
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillBonus_OnlySpecificRAT_NotApplies () throws Exception
	{
		// Unit we are calculating stats for
		final UnitEx unitDef = new UnitEx ();
		unitDef.setRangedAttackType ("RAT02");
		
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		when (mu.getUnitDefinition ()).thenReturn (unitDef);
		
		// Details about skill bonus to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.ADD_FIXED);
		addsToSkill.setRangedAttackTypeID ("RAT01");
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.WRONG_RANGED_ATTACK_TYPE, utils.addSkillBonus (mu, null, addsToSkill, null, modifiedSkillValues, null, null, null, "MB01"));
		
		// Check results
		assertEquals (0, breakdown.getComponents ().size ());
	}

	/**
	 * Tests the addSkillPenalty method when we don't have the necessary requisite skill to get the Penalty
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillPenalty_DontHaveSkill () throws Exception
	{
		// Details about skill Penalty to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.LOCK);
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.UNIT_DOES_NOT_HAVE_SKILL, utils.addSkillPenalty (null, addsToSkill, null, modifiedSkillValues, null, null, null));
	}

	/**
	 * Tests the addSkillPenalty method when we skill modification is a penalty rather than a Penalty, so is ignored
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillPenalty_Bonus () throws Exception
	{
		// Details about skill Penalty to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.ADD_FIXED);
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		modifiedSkillValues.put ("US001", new UnitSkillValueBreakdown (null));
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.INCORRECT_TYPE_OF_ADJUSTMENT, utils.addSkillPenalty (null, addsToSkill, null, modifiedSkillValues, null, null, null));
	}

	/**
	 * Tests the addSkillPenalty method when we skill modification has no value, so is ignored
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillPenalty_Valueless () throws Exception
	{
		// Details about skill Penalty to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.LOCK);
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		modifiedSkillValues.put ("US001", new UnitSkillValueBreakdown (null));
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.INCORRECT_TYPE_OF_ADJUSTMENT, utils.addSkillPenalty (null, addsToSkill, null, modifiedSkillValues, null, null, null));
	}

	/**
	 * Tests the addSkillPenalty method in the situation where the skill value gets locked at a certain value
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillPenalty_Lock () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill Penalty to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (7);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.LOCK);
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 50);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.APPLIES, utils.addSkillPenalty (mu, addsToSkill, null, modifiedSkillValues, null, null, null));
		
		// Check results
		assertEquals (2, breakdown.getComponents ().size ());
		assertEquals (50, breakdown.getComponents ().get (UnitSkillComponent.BASIC));
		assertEquals (-43, breakdown.getComponents ().get (UnitSkillComponent.SPELL_EFFECTS));
	}

	/**
	 * Tests the addSkillPenalty method in the situation where a locked skill value is higher than what it was previously.
	 * This gets blocked because the assumption is all modifications of type lock are meant to be penalties.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillPenalty_CantLockHigher () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill Penalty to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (57);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.LOCK);
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 50);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method (it still returns APPLIES because all the precondition checks passed, and this scenario is N/A for the addSkillBonus method)
		assertEquals (AddSkillBonusResult.APPLIES, utils.addSkillPenalty (mu, addsToSkill, null, modifiedSkillValues, null, null, null));
		
		// Check results
		assertEquals (1, breakdown.getComponents ().size ());
		assertEquals (50, breakdown.getComponents ().get (UnitSkillComponent.BASIC));
	}

	/**
	 * Tests the addSkillPenalty method in the situation where the skill value gets divided by a certain value
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillPenalty_Divide () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill Penalty to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (4);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.DIVIDE);
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 50);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.APPLIES, utils.addSkillPenalty (mu, addsToSkill, null, modifiedSkillValues, null, null, null));
		
		// Check results
		assertEquals (2, breakdown.getComponents ().size ());
		assertEquals (50, breakdown.getComponents ().get (UnitSkillComponent.BASIC));
		assertEquals (-38, breakdown.getComponents ().get (UnitSkillComponent.SPELL_EFFECTS));
	}
	/**
	 * Tests the addSkillPenalty method in the situation where the skill value gets multiplied by a certain value
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillPenalty_Multiply () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill Penalty to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (12);		// so x3
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.MULTIPLY);
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 50);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.APPLIES, utils.addSkillPenalty (mu, addsToSkill, null, modifiedSkillValues, null, null, null));
		
		// Check results
		assertEquals (2, breakdown.getComponents ().size ());
		assertEquals (50, breakdown.getComponents ().get (UnitSkillComponent.BASIC));
		assertEquals (100, breakdown.getComponents ().get (UnitSkillComponent.SPELL_EFFECTS));
	}

	/**
	 * Tests the addSkillPenalty method when the Penalty only applies to units in combat, and we are in combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillPenalty_OnlyInCombat_Applies () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		when (mu.isMemoryUnit ()).thenReturn (true);
		when (mu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		
		// Details about skill Penalty to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.LOCK);
		addsToSkill.setOnlyInCombat (true);
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 50);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.APPLIES, utils.addSkillPenalty (mu, addsToSkill, null, modifiedSkillValues, null, null, null));
		
		// Check results
		assertEquals (2, breakdown.getComponents ().size ());
		assertEquals (50, breakdown.getComponents ().get (UnitSkillComponent.BASIC));
		assertEquals (-48, breakdown.getComponents ().get (UnitSkillComponent.SPELL_EFFECTS));
	}

	/**
	 * Tests the addSkillPenalty method when the Penalty only applies to units in combat, and we aren't in combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillPenalty_OnlyInCombat_NotApplies () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill Penalty to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.LOCK);
		addsToSkill.setOnlyInCombat (true);
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 50);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.NOT_IN_COMBAT, utils.addSkillPenalty (mu, addsToSkill, null, modifiedSkillValues, null, null, null));
		
		// Check results
		assertEquals (1, breakdown.getComponents ().size ());
		assertEquals (50, breakdown.getComponents ().get (UnitSkillComponent.BASIC));
	}

	/**
	 * Tests the addSkillPenalty method when the Penalty has a skill based restriction about the type of incoming attack, but we don't have that info 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillPenalty_NoInfoAboutAttack_SkillRequired () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill Penalty to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.LOCK);
		addsToSkill.setOnlyVersusAttacksFromSkillID ("US003");
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 50);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.NO_INFO_ABOUT_INCOMING_ATTACK, utils.addSkillPenalty (mu, addsToSkill, null, modifiedSkillValues, null, null, null));
		
		// Check results
		assertEquals (1, breakdown.getComponents ().size ());
		assertEquals (50, breakdown.getComponents ().get (UnitSkillComponent.BASIC));
	}

	/**
	 * Tests the addSkillPenalty method when the Penalty has a magic realm based restriction about the type of incoming attack, but we don't have that info 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillPenalty_NoInfoAboutAttack_MagicRealmRequired () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill Penalty to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.LOCK);
		addsToSkill.setOnlyVersusAttacksFromMagicRealmID ("MB01");
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 50);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.NO_INFO_ABOUT_INCOMING_ATTACK, utils.addSkillPenalty (mu, addsToSkill, null, modifiedSkillValues, null, null, null));
		
		// Check results
		assertEquals (1, breakdown.getComponents ().size ());
		assertEquals (50, breakdown.getComponents ().get (UnitSkillComponent.BASIC));
	}

	/**
	 * Tests the addSkillPenalty method when the Penalty only applies against a specific type of attack, and it does match
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillPenalty_OnlyVersusSpecificSkill_Applies () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill Penalty to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.LOCK);
		addsToSkill.setOnlyVersusAttacksFromSkillID ("US003");
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 50);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.APPLIES, utils.addSkillPenalty (mu, addsToSkill, null, modifiedSkillValues, "US003", null, null));
		
		// Check results
		assertEquals (2, breakdown.getComponents ().size ());
		assertEquals (50, breakdown.getComponents ().get (UnitSkillComponent.BASIC));
		assertEquals (-48, breakdown.getComponents ().get (UnitSkillComponent.SPELL_EFFECTS));
	}

	/**
	 * Tests the addSkillPenalty method when the Penalty only applies against a specific type of attack, and it does not match
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillPenalty_OnlyVersusSpecificSkill_NotApplies () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill Penalty to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.LOCK);
		addsToSkill.setOnlyVersusAttacksFromSkillID ("US003");
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 50);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.WRONG_ATTACK_SKILL, utils.addSkillPenalty (mu, addsToSkill, null, modifiedSkillValues, "US004", null, null));
		
		// Check results
		assertEquals (1, breakdown.getComponents ().size ());
		assertEquals (50, breakdown.getComponents ().get (UnitSkillComponent.BASIC));
	}

	/**
	 * Tests the addSkillPenalty method when the Penalty only applies against a specific type of attack, and its a magic based attack instead, so Penalty does not apply
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillPenalty_OnlyVersusSpecificSkill_SpellsDontApply () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill Penalty to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.LOCK);
		addsToSkill.setOnlyVersusAttacksFromSkillID ("US003");
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 50);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.WRONG_ATTACK_SKILL, utils.addSkillPenalty (mu, addsToSkill, null, modifiedSkillValues, null, "MB01", null));
		
		// Check results
		assertEquals (1, breakdown.getComponents ().size ());
		assertEquals (50, breakdown.getComponents ().get (UnitSkillComponent.BASIC));
	}
	
	/**
	 * Tests the addSkillPenalty method when the Penalty applies against all types of attack EXCEPT one specific attack, and its a different attack, so Penalty applies
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillPenalty_VersusAllExceptSpecificSkill_Applies () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill Penalty to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.LOCK);
		addsToSkill.setOnlyVersusAttacksFromSkillID ("US003");
		addsToSkill.setNegateOnlyVersusAttacksFromSkillID (true);
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 50);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.APPLIES, utils.addSkillPenalty (mu, addsToSkill, null, modifiedSkillValues, "US004", null, null));
		
		// Check results
		assertEquals (2, breakdown.getComponents ().size ());
		assertEquals (50, breakdown.getComponents ().get (UnitSkillComponent.BASIC));
		assertEquals (-48, breakdown.getComponents ().get (UnitSkillComponent.SPELL_EFFECTS));
	}

	/**
	 * Tests the addSkillPenalty method when the Penalty applies against all types of attack EXCEPT one specific attack, and its that attack, so Penalty does not apply
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillPenalty_VersusAllExceptSpecificSkill_NotApplies () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill Penalty to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.LOCK);
		addsToSkill.setOnlyVersusAttacksFromSkillID ("US003");
		addsToSkill.setNegateOnlyVersusAttacksFromSkillID (true);
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 50);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.WRONG_ATTACK_SKILL, utils.addSkillPenalty (mu, addsToSkill, null, modifiedSkillValues, "US003", null, null));
		
		// Check results
		assertEquals (1, breakdown.getComponents ().size ());
		assertEquals (50, breakdown.getComponents ().get (UnitSkillComponent.BASIC));
	}

	/**
	 * Tests the addSkillPenalty method when the Penalty applies against all types of attack EXCEPT one specific attack,
	 * and that includes magic based attacks, so Penalty does apply (Large Shield does give +2 defence against spells like Fire Bolt)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillPenalty_VersusAllExceptSpecificSkill_SpellsDoApply () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill Penalty to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.LOCK);
		addsToSkill.setOnlyVersusAttacksFromSkillID ("US003");
		addsToSkill.setNegateOnlyVersusAttacksFromSkillID (true);
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 50);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.APPLIES, utils.addSkillPenalty (mu, addsToSkill, null, modifiedSkillValues, null, "MB01", null));
		
		// Check results
		assertEquals (2, breakdown.getComponents ().size ());
		assertEquals (50, breakdown.getComponents ().get (UnitSkillComponent.BASIC));
		assertEquals (-48, breakdown.getComponents ().get (UnitSkillComponent.SPELL_EFFECTS));
	}

	/**
	 * Tests the addSkillPenalty method when the Penalty only applies against attacks associated with a particular magic realm, and it does match
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillPenalty_OnlyVersusSpecificMagicRealm_Applies () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill Penalty to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.LOCK);
		addsToSkill.setOnlyVersusAttacksFromMagicRealmID ("MB01");
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 50);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.APPLIES, utils.addSkillPenalty (mu, addsToSkill, null, modifiedSkillValues, null, "MB01", null));
		
		// Check results
		assertEquals (2, breakdown.getComponents ().size ());
		assertEquals (50, breakdown.getComponents ().get (UnitSkillComponent.BASIC));
		assertEquals (-48, breakdown.getComponents ().get (UnitSkillComponent.SPELL_EFFECTS));
	}

	/**
	 * Tests the addSkillPenalty method when the Penalty only applies against attacks associated with a particular magic realm, and it does not match
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillPenalty_OnlyVersusSpecificMagicRealm_NotApplies () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill Penalty to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.LOCK);
		addsToSkill.setOnlyVersusAttacksFromMagicRealmID ("MB01");
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 50);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.WRONG_ATTACK_MAGIC_REALM, utils.addSkillPenalty (mu, addsToSkill, null, modifiedSkillValues, null, "MB02", null));
		
		// Check results
		assertEquals (1, breakdown.getComponents ().size ());
		assertEquals (50, breakdown.getComponents ().get (UnitSkillComponent.BASIC));
	}

	/**
	 * Tests the addSkillPenalty method when the Penalty only applies to creates of a particular magic realm (nothing to do with the magic realm of the attack)
	 * and it does match
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillPenalty_OnlyCreaturesOfMagicRealm_Applies () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill Penalty to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.LOCK);
		addsToSkill.setOnlyAppliesToMagicRealmID ("MB01");
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 50);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.APPLIES, utils.addSkillPenalty (mu, addsToSkill, null, modifiedSkillValues, null, null, "MB01"));
		
		// Check results
		assertEquals (2, breakdown.getComponents ().size ());
		assertEquals (50, breakdown.getComponents ().get (UnitSkillComponent.BASIC));
		assertEquals (-48, breakdown.getComponents ().get (UnitSkillComponent.SPELL_EFFECTS));
	}

	/**
	 * Tests the addSkillPenalty method when the Penalty only applies to creates of a particular magic realm (nothing to do with the magic realm of the attack)
	 * and it does not match
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillPenalty_OnlyCreaturesOfMagicRealm_NotApplies () throws Exception
	{
		// Unit we are calculating stats for
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		
		// Details about skill Penalty to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.LOCK);
		addsToSkill.setOnlyAppliesToMagicRealmID ("MB01");
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 50);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.WRONG_MAGIC_REALM_LIFEFORM_TYPE_ID, utils.addSkillPenalty (mu, addsToSkill, null, modifiedSkillValues, null, null, "MB02"));
		
		// Check results
		assertEquals (1, breakdown.getComponents ().size ());
		assertEquals (50, breakdown.getComponents ().get (UnitSkillComponent.BASIC));
	}

	/**
	 * Tests the addSkillPenalty method when the Penalty only applies to certain ranged attack types, which we have
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillPenalty_OnlySpecificRAT_Applies () throws Exception
	{
		// Unit we are calculating stats for
		final UnitEx unitDef = new UnitEx ();
		unitDef.setRangedAttackType ("RAT01");
		
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		when (mu.getUnitDefinition ()).thenReturn (unitDef);
		
		// Details about skill Penalty to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.LOCK);
		addsToSkill.setRangedAttackTypeID ("RAT01");
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 50);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.APPLIES, utils.addSkillPenalty (mu, addsToSkill, null, modifiedSkillValues, null, null, "MB01"));
		
		// Check results
		assertEquals (2, breakdown.getComponents ().size ());
		assertEquals (50, breakdown.getComponents ().get (UnitSkillComponent.BASIC));
		assertEquals (-48, breakdown.getComponents ().get (UnitSkillComponent.SPELL_EFFECTS));
	}

	/**
	 * Tests the addSkillPenalty method when the Penalty only applies to certain ranged attack types, which we don't have
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddSkillPenalty_OnlySpecificRAT_NotApplies () throws Exception
	{
		// Unit we are calculating stats for
		final UnitEx unitDef = new UnitEx ();
		unitDef.setRangedAttackType ("RAT02");
		
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		when (mu.getUnitDefinition ()).thenReturn (unitDef);
		
		// Details about skill Penalty to add
		final AddsToSkill addsToSkill = new AddsToSkill ();
		addsToSkill.setAddsToSkillID ("US001");
		addsToSkill.setAddsToSkillValue (2);
		addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.LOCK);
		addsToSkill.setRangedAttackTypeID ("RAT01");
		
		// Existing skill breakdown
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (null);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 50);
		modifiedSkillValues.put ("US001", breakdown);
		
		// Set up object to test
		final UnitDetailsUtilsImpl utils = new UnitDetailsUtilsImpl ();
		
		// Run method
		assertEquals (AddSkillBonusResult.WRONG_RANGED_ATTACK_TYPE, utils.addSkillPenalty (mu, addsToSkill, null, modifiedSkillValues, null, null, "MB01"));
		
		// Check results
		assertEquals (1, breakdown.getComponents ().size ());
		assertEquals (50, breakdown.getComponents ().get (UnitSkillComponent.BASIC));
	}
}