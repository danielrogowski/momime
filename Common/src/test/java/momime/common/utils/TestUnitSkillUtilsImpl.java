package momime.common.utils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.database.AddsToSkill;
import momime.common.database.CombatAreaEffect;
import momime.common.database.CombatAreaEffectSkillBonus;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.database.ExperienceSkillBonus;
import momime.common.database.Unit;
import momime.common.database.UnitMagicRealm;
import momime.common.database.UnitSkill;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillPositiveNegative;
import momime.common.database.WeaponGrade;
import momime.common.database.WeaponGradeSkillBonus;
import momime.common.messages.AvailableUnit;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;

/**
 * Tests the UnitSkillUtils class
 */
public final class TestUnitSkillUtilsImpl
{
	/**
	 * Tests the addToSkillValue method
	 */
	@Test
	public final void testAddToSkillValue ()
	{
		final UnitSkillUtilsImpl utils = new UnitSkillUtilsImpl ();
		
		assertEquals (5, utils.addToSkillValue (5, UnitSkillPositiveNegative.BOTH));
		assertEquals (0, utils.addToSkillValue (0, UnitSkillPositiveNegative.BOTH));
		assertEquals (-5, utils.addToSkillValue (-5, UnitSkillPositiveNegative.BOTH));
		
		assertEquals (5, utils.addToSkillValue (5, UnitSkillPositiveNegative.POSITIVE));
		assertEquals (0, utils.addToSkillValue (0, UnitSkillPositiveNegative.POSITIVE));
		assertEquals (0, utils.addToSkillValue (-5, UnitSkillPositiveNegative.POSITIVE));
		
		assertEquals (0, utils.addToSkillValue (5, UnitSkillPositiveNegative.NEGATIVE));
		assertEquals (0, utils.addToSkillValue (0, UnitSkillPositiveNegative.NEGATIVE));
		assertEquals (-5, utils.addToSkillValue (-5, UnitSkillPositiveNegative.NEGATIVE));
	}	

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
		
		final AddsToSkill heroSkillBonus = new AddsToSkill ();
		heroSkillBonus.setAddsToSkillID ("US004");
		heroSkillBonus.setAddsToSkillDivisor (2);
		
		final UnitSkill heroSkill = new UnitSkill ();
		heroSkill.setUnitSkillID ("HS001");
		heroSkill.getAddsToSkill ().add (heroSkillBonus);

		final AddsToSkill spellSkillBonus = new AddsToSkill ();
		spellSkillBonus.setAddsToSkillID ("US004");
		spellSkillBonus.setAddsToSkillFixed (10000);
		
		final UnitSkill spellSkill = new UnitSkill ();
		spellSkill.setUnitSkillID ("SS001");
		spellSkill.getAddsToSkill ().add (spellSkillBonus);
		
		final List<UnitSkill> skills = new ArrayList<UnitSkill> ();
		skills.add (heroSkill);
		skills.add (spellSkill);
		doReturn (skills).when (db).getUnitSkills ();
		
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
		when (unitUtils.getBasicSkillValue (unit.getUnitHasSkill (), "HS001")).thenReturn (1);
		when (unitUtils.getBasicSkillValue (unit.getUnitHasSkill (), "SS001")).thenReturn (0);
		
		// Experience level
		final ExperienceLevel expLvl = new ExperienceLevel ();
		expLvl.setLevelNumber (5);		// Makes the hero skill give 6 / 2 (see setAddsToAttributeDivisor above) = +3
		
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
		assertEquals (-1, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US001",
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, players, spells, combatAreaEffects, db));
		
		// Skill that we don't have so bonuses to it still don't apply
		assertEquals (-1, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US002",
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, players, spells, combatAreaEffects, db));

		// Skill that we do have, to which no bonuses apply
		assertEquals (1, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US003",
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, players, spells, combatAreaEffects, db));
		
		// Skill that we do have, and has weapon grade, experience and CAE bonuses
		assertEquals (11114, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US004",
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, players, spells, combatAreaEffects, db));
		
		// Test asking for specific breakdown components		
		assertEquals (1, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US004",
			UnitSkillComponent.BASIC, UnitSkillPositiveNegative.BOTH, players, spells, combatAreaEffects, db));

		assertEquals (10, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US004",
				UnitSkillComponent.WEAPON_GRADE, UnitSkillPositiveNegative.BOTH, players, spells, combatAreaEffects, db));

		assertEquals (100, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US004",
				UnitSkillComponent.EXPERIENCE, UnitSkillPositiveNegative.BOTH, players, spells, combatAreaEffects, db));
		
		assertEquals (1000, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US004",
			UnitSkillComponent.COMBAT_AREA_EFFECTS, UnitSkillPositiveNegative.BOTH, players, spells, combatAreaEffects, db));

		assertEquals (10000, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US004",
			UnitSkillComponent.SPELL_EFFECTS, UnitSkillPositiveNegative.BOTH, players, spells, combatAreaEffects, db));
		
		assertEquals (3, utils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "US004",
			UnitSkillComponent.HERO_SKILLS, UnitSkillPositiveNegative.BOTH, players, spells, combatAreaEffects, db));
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