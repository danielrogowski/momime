package momime.common.utils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.common.database.CombatAreaEffect;
import momime.common.database.CombatAreaEffectAttributeBonus;
import momime.common.database.CombatAreaEffectSkillBonus;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceAttributeBonus;
import momime.common.database.ExperienceLevel;
import momime.common.database.ExperienceSkillBonus;
import momime.common.database.Unit;
import momime.common.database.UnitAttributeComponent;
import momime.common.database.UnitAttributePositiveNegative;
import momime.common.database.UnitHasAttributeValue;
import momime.common.database.UnitMagicRealm;
import momime.common.database.UnitSkill;
import momime.common.database.WeaponGrade;
import momime.common.database.WeaponGradeAttributeBonus;
import momime.common.database.WeaponGradeSkillBonus;
import momime.common.messages.AvailableUnit;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;

import org.junit.Test;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * Tests the UnitSkillUtils class
 */
public final class TestUnitSkillUtilsImpl
{
	/**
	 * Tests the getModifiedSkillValue method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetModifiedSkillValue () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final WeaponGrade weaponGradeDef = new WeaponGrade ();
		for (final String unitSkillID : new String [] {"US002", "US004"})
		{
			final WeaponGradeSkillBonus bonus = new WeaponGradeSkillBonus ();
			bonus.setUnitSkillID (unitSkillID);
			bonus.setBonusValue (10);
			weaponGradeDef.getWeaponGradeSkillBonus ().add (bonus);
		}
		when (db.findWeaponGrade (1, "getModifiedSkillValue")).thenReturn (weaponGradeDef);
		
		final CombatAreaEffect caeDef = new CombatAreaEffect ();
		for (final String unitSkillID : new String [] {"US002", "US004"})
		{
			final CombatAreaEffectSkillBonus bonus = new CombatAreaEffectSkillBonus ();
			bonus.setUnitSkillID (unitSkillID);
			bonus.setBonusValue (1000);
			caeDef.getCombatAreaEffectSkillBonus ().add (bonus);
		}
		when (db.findCombatAreaEffect ("CAE01", "getModifiedSkillValue")).thenReturn (caeDef);
		
		// Lists
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();

		// Sample unit
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final AvailableUnit unit = new AvailableUnit ();
		unit.setWeaponGrade (1);
		when (unitUtils.getModifiedUnitMagicRealmLifeformTypeID (unit, unit.getUnitHasSkill (), spells, db)).thenReturn ("A");
		
		// Base skill values
		when (unitUtils.getBasicSkillValue (unit.getUnitHasSkill (), "US001")).thenReturn (-1);
		when (unitUtils.getBasicSkillValue (unit.getUnitHasSkill (), "US002")).thenReturn (-1);
		when (unitUtils.getBasicSkillValue (unit.getUnitHasSkill (), "US003")).thenReturn (1);
		when (unitUtils.getBasicSkillValue (unit.getUnitHasSkill (), "US004")).thenReturn (1);
		
		// Experience level
		final ExperienceLevel expLvl = new ExperienceLevel ();
		for (final String unitSkillID : new String [] {"US002", "US004"})
		{
			final ExperienceSkillBonus bonus = new ExperienceSkillBonus ();
			bonus.setUnitSkillID (unitSkillID);
			bonus.setBonusValue (100);
			expLvl.getExperienceSkillBonus ().add (bonus);
		}
		when (unitUtils.getExperienceLevel (unit, true, players, combatAreaEffects, db)).thenReturn (expLvl);
		
		// CAE
		final MemoryCombatAreaEffect cae = new MemoryCombatAreaEffect ();
		cae.setCombatAreaEffectID ("CAE01");
		
		when (unitUtils.doesCombatAreaEffectApplyToUnit (unit, cae, db)).thenReturn (true);
		combatAreaEffects.add (cae);
		
		// Set up object to test
		final UnitSkillUtilsImpl utils = new UnitSkillUtilsImpl ();
		utils.setUnitUtils (unitUtils);

		// Skill that we don't have
		assertEquals (-1, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US001", players, spells, combatAreaEffects, db));
		
		// Skill that we don't have so bonuses to it still don't apply
		assertEquals (-1, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US002", players, spells, combatAreaEffects, db));

		// Skill that we do have, to which no bonuses apply
		assertEquals (1, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US003", players, spells, combatAreaEffects, db));
		
		// Skill that we do have, and has weapon grade, experience and CAE bonuses
		assertEquals (1111, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US004", players, spells, combatAreaEffects, db));
	}
	
	/**
	 * Tests the addToAttributeValue method
	 */
	@Test
	public final void testAddToAttributeValue ()
	{
		final UnitSkillUtilsImpl utils = new UnitSkillUtilsImpl ();
		
		assertEquals (5, utils.addToAttributeValue (5, UnitAttributePositiveNegative.BOTH));
		assertEquals (0, utils.addToAttributeValue (0, UnitAttributePositiveNegative.BOTH));
		assertEquals (-5, utils.addToAttributeValue (-5, UnitAttributePositiveNegative.BOTH));
		
		assertEquals (5, utils.addToAttributeValue (5, UnitAttributePositiveNegative.POSITIVE));
		assertEquals (0, utils.addToAttributeValue (0, UnitAttributePositiveNegative.POSITIVE));
		assertEquals (0, utils.addToAttributeValue (-5, UnitAttributePositiveNegative.POSITIVE));
		
		assertEquals (0, utils.addToAttributeValue (5, UnitAttributePositiveNegative.NEGATIVE));
		assertEquals (0, utils.addToAttributeValue (0, UnitAttributePositiveNegative.NEGATIVE));
		assertEquals (-5, utils.addToAttributeValue (-5, UnitAttributePositiveNegative.NEGATIVE));
	}
	
	/**
	 * Tests the getModifiedAttributeValue method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetModifiedAttributeValue () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Unit unitDef = new Unit ();
		for (final String unitAttributeID : new String [] {"UA02", "UA03"})
		{
			final UnitHasAttributeValue unitAttr = new UnitHasAttributeValue ();
			unitAttr.setUnitAttributeID (unitAttributeID);
			unitAttr.setAttributeValue (1);
			unitDef.getUnitAttributeValue ().add (unitAttr);
		}
		when (db.findUnit ("UN001", "getModifiedAttributeValue")).thenReturn (unitDef);

		final WeaponGrade weaponGradeDef = new WeaponGrade ();
		for (final String unitAttributeID : new String [] {"UA03"})
		{
			final WeaponGradeAttributeBonus bonus = new WeaponGradeAttributeBonus ();
			bonus.setUnitAttributeID (unitAttributeID);
			bonus.setBonusValue (10);
			weaponGradeDef.getWeaponGradeAttributeBonus ().add (bonus);
		}
		when (db.findWeaponGrade (eq (1), anyString ())).thenReturn (weaponGradeDef);

		final CombatAreaEffect caeDef = new CombatAreaEffect ();
		for (final String unitAttributeID : new String [] {"UA03"})
		{
			final CombatAreaEffectAttributeBonus bonus = new CombatAreaEffectAttributeBonus ();
			bonus.setUnitAttributeID (unitAttributeID);
			bonus.setBonusValue (1000);
			caeDef.getCombatAreaEffectAttributeBonus ().add (bonus);
		}
		when (db.findCombatAreaEffect (eq ("CAE01"), anyString ())).thenReturn (caeDef);
		
		final UnitSkill heroSkill = new UnitSkill ();
		heroSkill.setUnitSkillID ("HS01");
		heroSkill.setAddsToAttributeID ("UA03");
		
		final List<UnitSkill> skillDefs = new ArrayList<UnitSkill> ();
		skillDefs.add (heroSkill);
		doReturn (skillDefs).when (db).getUnitSkills ();
		
		// Lists
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();

		// Sample unit
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID ("UN001");
		unit.setWeaponGrade (1);

		// Hero skill value - these are halved
		when (unitUtils.getBasicSkillValue (unit.getUnitHasSkill (), "HS01")).thenReturn (20000);
		
		// Experience level
		final ExperienceLevel expLvl = new ExperienceLevel ();
		for (final String unitAttributeID : new String [] {"UA03"})
		{
			final ExperienceAttributeBonus bonus = new ExperienceAttributeBonus ();
			bonus.setUnitAttributeID (unitAttributeID);
			bonus.setBonusValue (100);
			expLvl.getExperienceAttributeBonus ().add (bonus);
		}
		when (unitUtils.getExperienceLevel (unit, true, players, combatAreaEffects, db)).thenReturn (expLvl);

		// CAE
		final MemoryCombatAreaEffect cae = new MemoryCombatAreaEffect ();
		cae.setCombatAreaEffectID ("CAE01");
		
		when (unitUtils.doesCombatAreaEffectApplyToUnit (unit, cae, db)).thenReturn (true);
		combatAreaEffects.add (cae);
		
		// Set up object to test
		final UnitSkillUtilsImpl utils = new UnitSkillUtilsImpl ();
		utils.setUnitUtils (unitUtils);

		// Attribute that we don't even have
		assertEquals (0, utils.getModifiedAttributeValue (unit, "UA01", UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db));
		
		// Attribute that we have but gets no bonuses
		assertEquals (1, utils.getModifiedAttributeValue (unit, "UA02", UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db));
		
		// Attribute that gets all the bonuses
		assertEquals (11111, utils.getModifiedAttributeValue (unit, "UA03", UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db));
		
		// Test asking for specific breakdown components		
		assertEquals (1, utils.getModifiedAttributeValue (unit, "UA03", UnitAttributeComponent.BASIC, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db));
		assertEquals (10, utils.getModifiedAttributeValue (unit, "UA03", UnitAttributeComponent.WEAPON_GRADE, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db));
		assertEquals (100, utils.getModifiedAttributeValue (unit, "UA03", UnitAttributeComponent.EXPERIENCE, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db));
		assertEquals (1000, utils.getModifiedAttributeValue (unit, "UA03", UnitAttributeComponent.COMBAT_AREA_EFFECTS, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db));
		assertEquals (10000, utils.getModifiedAttributeValue (unit, "UA03", UnitAttributeComponent.HERO_SKILLS, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db));
	}

	/**
	 * Tests the getModifiedUpkeepValue method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetModifiedUpkeepValue () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final Unit unitDef = new Unit ();
		unitDef.setUnitMagicRealm ("A");
		when (db.findUnit ("UN001", "getModifiedUpkeepValue")).thenReturn (unitDef);
		
		final UnitMagicRealm magicRealm = new UnitMagicRealm ();
		magicRealm.setUnitTypeID ("T");
		when (db.findUnitMagicRealm ("A", "getModifiedUpkeepValue")).thenReturn (magicRealm);
		
		// Lists
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		// Player
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		
		final PlayerPublicDetails owningPlayer = new PlayerPublicDetails (null, pub, null);
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, 1, "getModifiedUpkeepValue")).thenReturn (owningPlayer);
		
		// Sample unit
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID ("UN001");
		unit.setOwningPlayerID (1);

		// Base upkeep values
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.getBasicUpkeepValue (unit, "RE01", db)).thenReturn (0);
		when (unitUtils.getBasicUpkeepValue (unit, "RE02", db)).thenReturn (5);
		
		// Set up object to test
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		
		final UnitSkillUtilsImpl utils = new UnitSkillUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setMultiplayerSessionUtils (multiplayerSessionUtils);
		utils.setPlayerPickUtils (playerPickUtils);
		
		// Upkeep that we pay none of
		assertEquals (0, utils.getModifiedUpkeepValue (unit, "RE01", players, db));
		
		// Upkeep with no reduction
		assertEquals (5, utils.getModifiedUpkeepValue (unit, "RE02", players, db));
		
		// Apply reduction
		when (playerPickUtils.totalProductionBonus (CommonDatabaseConstants.PRODUCTION_TYPE_ID_UNIT_UPKEEP_REDUCTION, "T", pub.getPick (), db)).thenReturn (50);
		assertEquals (3, utils.getModifiedUpkeepValue (unit, "RE02", players, db));
	}
}