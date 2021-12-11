package momime.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.database.Pick;
import momime.common.database.RangedAttackTypeEx;
import momime.common.database.UnitEx;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitType;
import momime.common.database.WeaponGrade;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;

/**
 * Tests the ExpandUnitDetailsImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestExpandUnitDetailsImpl
{
	/**
	 * Tests the expandUnitDetails method when buildUnitStackMinimalDetails returns that the unit isn't in its own stack, which should be impossible
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testExpandUnitDetails_UnitNotInStack () throws Exception
	{
		// These are just for mocks and the unit list
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final FogOfWarMemory mem = new FogOfWarMemory ();
		final List<ExpandedUnitDetails> enemyUnits = new ArrayList<ExpandedUnitDetails> ();
		final CommonDatabase db = mock (CommonDatabase.class);

		// Unit we are calculating stats for
		final MemoryUnit unit = new MemoryUnit ();
		
		// Long series of mocks that take place as each piece of the calculation is delegated to different methods
		final ExpandUnitDetailsUtils expandUnitDetailsUtils = mock (ExpandUnitDetailsUtils.class);
		
		// Units in same stack as the unit we are calculating stats for
		final List<MinimalUnitDetails> unitStack = new ArrayList<MinimalUnitDetails> ();
		
		final MinimalUnitDetails mu1 = mock (MinimalUnitDetails.class);
		when (mu1.getUnit ()).thenReturn (new AvailableUnit ());
		unitStack.add (mu1);
		
		when (expandUnitDetailsUtils.buildUnitStackMinimalDetails (unit, players, mem, db)).thenReturn (unitStack);
		
		// Set up object to test
		final ExpandUnitDetailsImpl expand = new ExpandUnitDetailsImpl ();
		expand.setExpandUnitDetailsUtils (expandUnitDetailsUtils);
		
		// Run method
		assertThrows (MomException.class, () ->
		{
			expand.expandUnitDetails (unit, enemyUnits, "US001", "MB01", players, mem, db);
		});
	}		

	/**
	 * Tests the expandUnitDetails method in normal situation of just stepping through the mocks
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testExpandUnitDetails_Normal () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final RangedAttackTypeEx rangedAttackType = new RangedAttackTypeEx ();
		when (db.findRangedAttackType ("RAT01", "expandUnitDetails")).thenReturn (rangedAttackType);

		final WeaponGrade weaponGrade = new WeaponGrade ();
		when (db.findWeaponGrade (1, "expandUnitDetails")).thenReturn (weaponGrade);

		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitMagicRealm ("MB02");
		unitDef.setRangedAttackType ("RAT01");
		
		final ExperienceLevel basicExpLvl = new ExperienceLevel ();
		final ExperienceLevel modifiedExpLvl = new ExperienceLevel ();
		
		final UnitType unitType = new UnitType ();
		
		final PlayerPublicDetails owningPlayer = new PlayerPublicDetails (null, null, null);
		
		// These are just for mocks and the unit list
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final FogOfWarMemory mem = new FogOfWarMemory ();
		final List<ExpandedUnitDetails> enemyUnits = new ArrayList<ExpandedUnitDetails> ();

		// Unit we are calculating stats for
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitURN (10);
		unit.setOwningPlayerID (2);
		unit.setWeaponGrade (1);

		final Map<String, Integer> basicUpkeepValues = new HashMap<String, Integer> ();
		basicUpkeepValues.put ("RE01", 1);
		
		final MinimalUnitDetails mu = mock (MinimalUnitDetails.class);
		when (mu.isMemoryUnit ()).thenReturn (true);
		when (mu.getUnit ()).thenReturn (unit);
		when (mu.getMemoryUnit ()).thenReturn (unit);
		when (mu.getUnitDefinition ()).thenReturn (unitDef);
		when (mu.getBasicExperienceLevel ()).thenReturn (basicExpLvl);
		when (mu.getModifiedExperienceLevel ()).thenReturn (modifiedExpLvl);
		when (mu.getBasicUpeepValues ()).thenReturn (basicUpkeepValues);
		when (mu.getUnitType ()).thenReturn (unitType);
		when (mu.getOwningPlayer ()).thenReturn (owningPlayer);
		
		// Long series of mocks that take place as each piece of the calculation is delegated to different methods
		final ExpandUnitDetailsUtils expandUnitDetailsUtils = mock (ExpandUnitDetailsUtils.class);
		
		// Mock unit stack
		final List<MinimalUnitDetails> unitStack = new ArrayList<MinimalUnitDetails> ();
		
		final MemoryUnit unit1 = new MemoryUnit ();
		unit1.setUnitURN (11);
		
		final MinimalUnitDetails mu1 = mock (MinimalUnitDetails.class);
		when (mu1.getUnit ()).thenReturn (unit1);
		unitStack.add (mu1);
		unitStack.add (mu);
		
		when (expandUnitDetailsUtils.buildUnitStackMinimalDetails (unit, players, mem, db)).thenReturn (unitStack);
		
		final List<Integer> unitStackUnitURNs = Arrays.asList (10, 11);
		when (expandUnitDetailsUtils.buildUnitStackUnitURNs (unitStack)).thenReturn (unitStackUnitURNs);
		
		// Mock unit skills
		final Map<String, Integer> basicSkillValues = new HashMap<String, Integer> ();
		basicSkillValues.put ("US002", 2);
		when (mu.getBasicSkillValues ()).thenReturn (basicSkillValues);

		final Map<String, Integer> unitStackSkills = new HashMap<String, Integer> ();
		unitStackSkills.put ("US003", 3);
		when (expandUnitDetailsUtils.getHighestSkillsInUnitStack (unitStack)).thenReturn (unitStackSkills);
		
		final Map<String, Integer> skillsFromSpellsCastOnThisUnit = new HashMap<String, Integer> ();
		skillsFromSpellsCastOnThisUnit.put ("US004", 4);
		when (expandUnitDetailsUtils.addSkillsFromSpells (mu, mem.getMaintainedSpell (),
			unitStackUnitURNs, basicSkillValues, unitStackSkills, db)).thenReturn (skillsFromSpellsCastOnThisUnit);
		
		final Map<String, Integer> basicSkillValuesWithNegatedSkillsRemoved = new HashMap<String, Integer> ();
		basicSkillValuesWithNegatedSkillsRemoved.put ("US005", 5);
		when (expandUnitDetailsUtils.copySkillValuesRemovingNegatedSkills (basicSkillValues, enemyUnits, db)).thenReturn (basicSkillValuesWithNegatedSkillsRemoved);
		
		final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (UnitSkillComponent.BASIC);
		breakdown.getComponents ().put (UnitSkillComponent.BASIC, 6);
		
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		modifiedSkillValues.put ("US006", breakdown);
		modifiedSkillValues.put (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT, new UnitSkillValueBreakdown (UnitSkillComponent.BASIC));		// + to hit / block MUST be in the modified skill list
		modifiedSkillValues.put (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK, new UnitSkillValueBreakdown (UnitSkillComponent.BASIC));
		when (expandUnitDetailsUtils.buildInitialBreakdownFromBasicSkills (basicSkillValuesWithNegatedSkillsRemoved)).thenReturn (modifiedSkillValues);
		
		final List<String> skillsGrantedFromCombatAreaEffects = Arrays.asList ("US007");
		when (expandUnitDetailsUtils.addSkillsFromCombatAreaEffects (unit, mem.getCombatAreaEffect (), modifiedSkillValues, enemyUnits, "MB02", db)).thenReturn
			(skillsGrantedFromCombatAreaEffects);
		
		// Mock magic realm/lifeform type
		final Pick magicRealmLifeformType = new Pick ();
		magicRealmLifeformType.setPickID ("MB02");
		when (expandUnitDetailsUtils.determineModifiedMagicRealmLifeformType
			("MB02", basicSkillValuesWithNegatedSkillsRemoved, db)).thenReturn (magicRealmLifeformType);
		
		// Mock upkeep values
		final Map<String, Integer> modifiedUpkeepValues = new HashMap<String, Integer> ();
		modifiedUpkeepValues.put ("RE02", 2);
		when (expandUnitDetailsUtils.buildModifiedUpkeepValues (mu, basicUpkeepValues, modifiedSkillValues, db)).thenReturn (modifiedUpkeepValues);
		
		// Mock controlling player
		when (expandUnitDetailsUtils.determineControllingPlayerID (mu, skillsFromSpellsCastOnThisUnit)).thenReturn (3);
		
		// Set up object to test
		final ExpandUnitDetailsImpl expand = new ExpandUnitDetailsImpl ();
		expand.setExpandUnitDetailsUtils (expandUnitDetailsUtils);
		
		// Run method
		final ExpandedUnitDetails xu = expand.expandUnitDetails (unit, enemyUnits, "US001", "MB01", players, mem, db);
		
		// Check correct values were written to the unit
		assertTrue (xu.isMemoryUnit ());
		assertSame (unit, xu.getMemoryUnit ());
		assertSame (unitDef, xu.getUnitDefinition ());
		assertSame (unitType, xu.getUnitType ());
		assertSame (owningPlayer, xu.getOwningPlayer ());
		assertSame (magicRealmLifeformType, xu.getModifiedUnitMagicRealmLifeformType ());
		assertSame (basicExpLvl, xu.getBasicExperienceLevel ());
		assertSame (modifiedExpLvl, xu.getModifiedExperienceLevel ());
		assertEquals (3, xu.getControllingPlayerID ());
		
		assertEquals (2, xu.getBasicSkillValue ("US002"));
		assertEquals (6, xu.getModifiedSkillValue ("US006"));
		assertEquals (1, xu.getBasicUpkeepValue ("RE01"));
		assertEquals (2, xu.getModifiedUpkeepValue ("RE02"));
		
		// Check every expected method was invoked
		verify (expandUnitDetailsUtils).addValuelessSkillsFromHeroItems (unit.getHeroItemSlot (), "US001", basicSkillValues, db);
		verify (expandUnitDetailsUtils).addSkillsGrantedFromOtherSkills (basicSkillValues, db);
		verify (expandUnitDetailsUtils).adjustMovementSpeedForWindMastery (basicSkillValues, modifiedSkillValues, 2, mem.getMaintainedSpell ());
		verify (expandUnitDetailsUtils).addBonusesFromWeaponGrade (mu, weaponGrade, modifiedSkillValues, unitStackSkills,
			"US001", "MB01", "MB02");
		verify (expandUnitDetailsUtils).addBonusesFromExperienceLevel (modifiedExpLvl, modifiedSkillValues);
		verify (expandUnitDetailsUtils).removeNegatedSkillsAddedFromCombatAreaEffects
			(skillsGrantedFromCombatAreaEffects, modifiedSkillValues, enemyUnits, db);
		verify (expandUnitDetailsUtils).addBonusesFromOtherSkills (mu, modifiedSkillValues, unitStackSkills, enemyUnits, "US001", "MB01", "MB02", db);
		verify (expandUnitDetailsUtils).addBonusesFromHeroItems (unit.getHeroItemSlot (), modifiedSkillValues, "US001", db);
		verify (expandUnitDetailsUtils).addPenaltiesFromHeroItems (mu, modifiedSkillValues, "US001", "MB01", "MB02", db);
		verify (expandUnitDetailsUtils).addPenaltiesFromOtherSkills (mu, modifiedSkillValues, unitStackSkills, enemyUnits, "US001", "MB01", "MB02", db);
		verifyNoMoreInteractions (expandUnitDetailsUtils);
	}		
}