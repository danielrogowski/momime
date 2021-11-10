package momime.common.calculations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

import momime.common.MomException;
import momime.common.database.CastingReductionCombination;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.GenerateTestData;
import momime.common.database.Pick;
import momime.common.database.PickProductionBonus;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellSetting;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.PlayerPick;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.SpellUtils;

/**
 * Tests the calculations in the SpellCalculationsImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestSpellCalculationsImpl
{
	/** How much we tolerate floating point results to be wrong by because of rounding errors */
	private final static double DOUBLE_TOLERANCE = 0.0000000000001;

	/**
	 * Tests the calculateCastingCostReduction method with adding bonuses together
	 * @throws MomException If there is a problem
	 * @throws RecordNotFoundException If we encounter a record that can't be found in the DB
	 */
	@Test
	public final void testCalculateCastingCostReductionAdditive () throws MomException, RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final PickProductionBonus bonusToEverythingValue = new PickProductionBonus ();
		bonusToEverythingValue.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_SPELL_COST_REDUCTION);
		bonusToEverythingValue.setPercentageBonus (10);
		
		final Pick bonusToEverythingDef = new Pick ();
		bonusToEverythingDef.getPickProductionBonus ().add (bonusToEverythingValue);
		when (db.findPick ("RT01", "calculateCastingCostReduction")).thenReturn (bonusToEverythingDef);

		final PickProductionBonus bonusToMagicRealmValue = new PickProductionBonus ();
		bonusToMagicRealmValue.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_SPELL_COST_REDUCTION);
		bonusToMagicRealmValue.setPercentageBonus (15);
		bonusToMagicRealmValue.setMagicRealmID ("MB01");
		
		final Pick bonusToMagicRealmDef = new Pick ();
		bonusToMagicRealmDef.getPickProductionBonus ().add (bonusToMagicRealmValue);
		when (db.findPick ("RT02", "calculateCastingCostReduction")).thenReturn (bonusToMagicRealmDef);
		
		final PickProductionBonus bonusToArcaneValue = new PickProductionBonus ();
		bonusToArcaneValue.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_SPELL_COST_REDUCTION);
		bonusToArcaneValue.setPercentageBonus (20);
		bonusToArcaneValue.setMagicRealmIdBlank (true);
		
		final Pick bonusToArcaneDef = new Pick ();
		bonusToArcaneDef.getPickProductionBonus ().add (bonusToArcaneValue);
		when (db.findPick ("RT03", "calculateCastingCostReduction")).thenReturn (bonusToArcaneDef);

		final PickProductionBonus bonusToUnitTypeValue = new PickProductionBonus ();
		bonusToUnitTypeValue.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_SPELL_COST_REDUCTION);
		bonusToUnitTypeValue.setPercentageBonus (25);
		bonusToUnitTypeValue.setUnitTypeID ("X");
		
		final Pick bonusToUnitTypeDef = new Pick ();
		bonusToUnitTypeDef.getPickProductionBonus ().add (bonusToUnitTypeValue);
		when (db.findPick ("RT04", "calculateCastingCostReduction")).thenReturn (bonusToUnitTypeDef);
		
		// Spell settings
		final SpellSetting spellSettings = new SpellSetting ();
		spellSettings.setSpellBooksCastingReductionCombination (CastingReductionCombination.ADDITIVE);
		spellSettings.setSpellBooksToObtainFirstReduction (4);
		spellSettings.setSpellBooksCastingReduction (8);
		spellSettings.setSpellBooksCastingReductionCap (80);
		
		// Set up object to test
		final SpellUtils spellUtils = mock (SpellUtils.class);

		final SpellCalculationsImpl calc = new SpellCalculationsImpl ();
		calc.setSpellUtils (spellUtils);

		// Not enough books to gain any bonus
		assertEquals (0, calc.calculateCastingCostReduction (3, spellSettings, null, null, db), DOUBLE_TOLERANCE);
		
		// Single bonus
		assertEquals (8, calc.calculateCastingCostReduction (4, spellSettings, null, null, db), DOUBLE_TOLERANCE);
		
		// Additive bonus
		assertEquals (7 * 8, calc.calculateCastingCostReduction (10, spellSettings, null, null, db), DOUBLE_TOLERANCE);
		
		// Capped - otherwise it would be 12 * 8 = 96%
		assertEquals (80, calc.calculateCastingCostReduction (15, spellSettings, null, null, db), DOUBLE_TOLERANCE);
		
		// Now supply list of picks and specify the spell being cast
		final Spell spell = new Spell ();
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();

		assertEquals (8, calc.calculateCastingCostReduction (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		
		// Retort that gives +10% regardless of the spell being cast
		final PlayerPick bonusToEverything = new PlayerPick ();
		bonusToEverything.setPickID ("RT01");
		bonusToEverything.setQuantity (1);
		picks.add (bonusToEverything);
		
		assertEquals (18, calc.calculateCastingCostReduction (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);

		// Retort that gives +15% only when casting spells of particular magic realm
		final PlayerPick bonusToMagicRealm = new PlayerPick ();
		bonusToMagicRealm.setPickID ("RT02");
		bonusToMagicRealm.setQuantity (1);
		picks.add (bonusToMagicRealm);
		
		assertEquals (18, calc.calculateCastingCostReduction (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		spell.setSpellRealm ("MB02");
		assertEquals (18, calc.calculateCastingCostReduction (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		spell.setSpellRealm ("MB01");
		assertEquals (33, calc.calculateCastingCostReduction (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		
		// Retort that gives +20% only when casting arcane spells
		final PlayerPick bonusToArcane = new PlayerPick ();
		bonusToArcane.setPickID ("RT03");
		bonusToArcane.setQuantity (1);
		picks.add (bonusToArcane);
		
		assertEquals (33, calc.calculateCastingCostReduction (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		spell.setSpellRealm ("MB02");
		assertEquals (18, calc.calculateCastingCostReduction (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		spell.setSpellRealm (null);
		assertEquals (38, calc.calculateCastingCostReduction (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		spell.setSpellRealm ("MB02");

		// Retort that gives +25% only when summoning units of a particular type
		final PlayerPick bonusToUnitType = new PlayerPick ();
		bonusToUnitType.setPickID ("RT04");
		bonusToUnitType.setQuantity (1);
		picks.add (bonusToUnitType);
		
		assertEquals (18, calc.calculateCastingCostReduction (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		when (spellUtils.spellSummonsUnitTypeID (spell, db)).thenReturn ("Y");
		assertEquals (18, calc.calculateCastingCostReduction (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		when (spellUtils.spellSummonsUnitTypeID (spell, db)).thenReturn ("X");
		assertEquals (43, calc.calculateCastingCostReduction (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
	}

	/**
	 * Tests the calculateCastingCostReduction method with multiplying bonuses together
	 * @throws MomException If there is a problem
	 * @throws RecordNotFoundException If we encounter a record that can't be found in the DB
	 */
	@Test
	public final void testCalculateCastingCostReductionMultiplicative () throws MomException, RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final PickProductionBonus bonusToEverythingValue = new PickProductionBonus ();
		bonusToEverythingValue.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_SPELL_COST_REDUCTION);
		bonusToEverythingValue.setPercentageBonus (10);
		
		final Pick bonusToEverythingDef = new Pick ();
		bonusToEverythingDef.getPickProductionBonus ().add (bonusToEverythingValue);
		when (db.findPick ("RT01", "calculateCastingCostReduction")).thenReturn (bonusToEverythingDef);

		final PickProductionBonus bonusToMagicRealmValue = new PickProductionBonus ();
		bonusToMagicRealmValue.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_SPELL_COST_REDUCTION);
		bonusToMagicRealmValue.setPercentageBonus (15);
		bonusToMagicRealmValue.setMagicRealmID ("MB01");
		
		final Pick bonusToMagicRealmDef = new Pick ();
		bonusToMagicRealmDef.getPickProductionBonus ().add (bonusToMagicRealmValue);
		when (db.findPick ("RT02", "calculateCastingCostReduction")).thenReturn (bonusToMagicRealmDef);
		
		final PickProductionBonus bonusToArcaneValue = new PickProductionBonus ();
		bonusToArcaneValue.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_SPELL_COST_REDUCTION);
		bonusToArcaneValue.setPercentageBonus (20);
		bonusToArcaneValue.setMagicRealmIdBlank (true);
		
		final Pick bonusToArcaneDef = new Pick ();
		bonusToArcaneDef.getPickProductionBonus ().add (bonusToArcaneValue);
		when (db.findPick ("RT03", "calculateCastingCostReduction")).thenReturn (bonusToArcaneDef);

		final PickProductionBonus bonusToUnitTypeValue = new PickProductionBonus ();
		bonusToUnitTypeValue.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_SPELL_COST_REDUCTION);
		bonusToUnitTypeValue.setPercentageBonus (25);
		bonusToUnitTypeValue.setUnitTypeID ("X");
		
		final Pick bonusToUnitTypeDef = new Pick ();
		bonusToUnitTypeDef.getPickProductionBonus ().add (bonusToUnitTypeValue);
		when (db.findPick ("RT04", "calculateCastingCostReduction")).thenReturn (bonusToUnitTypeDef);
		
		// Spell settings
		final SpellSetting spellSettings = new SpellSetting ();
		spellSettings.setSpellBooksCastingReductionCombination (CastingReductionCombination.MULTIPLICATIVE);
		spellSettings.setSpellBooksToObtainFirstReduction (4);
		spellSettings.setSpellBooksCastingReduction (8);
		spellSettings.setSpellBooksCastingReductionCap (80);
		
		// Set up object to test
		final SpellUtils spellUtils = mock (SpellUtils.class);

		final SpellCalculationsImpl calc = new SpellCalculationsImpl ();
		calc.setSpellUtils (spellUtils);

		// Not enough books to gain any bonus
		assertEquals (0, calc.calculateCastingCostReduction (3, spellSettings, null, null, db), DOUBLE_TOLERANCE);
		
		// Single bonus
		assertEquals (8, calc.calculateCastingCostReduction (4, spellSettings, null, null, db), DOUBLE_TOLERANCE);
		
		// Multiplicative bonus
		assertEquals (100 * (1 - Math.pow (0.92d, 7)), calc.calculateCastingCostReduction (10, spellSettings, null, null, db), DOUBLE_TOLERANCE);
		
		// Capped - otherwise it would be 100 * (1 - (0.92 ^ 20)) = 81.13%
		assertEquals (80, calc.calculateCastingCostReduction (23, spellSettings, null, null, db), DOUBLE_TOLERANCE);
		
		// Now supply list of picks and specify the spell being cast
		final Spell spell = new Spell ();
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();

		assertEquals (8, calc.calculateCastingCostReduction (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		
		// Retort that gives +10% regardless of the spell being cast
		final PlayerPick bonusToEverything = new PlayerPick ();
		bonusToEverything.setPickID ("RT01");
		bonusToEverything.setQuantity (1);
		picks.add (bonusToEverything);
		
		assertEquals (100 * (1 - (0.92d * 0.9d)), calc.calculateCastingCostReduction (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);

		// Retort that gives +15% only when casting spells of particular magic realm
		final PlayerPick bonusToMagicRealm = new PlayerPick ();
		bonusToMagicRealm.setPickID ("RT02");
		bonusToMagicRealm.setQuantity (1);
		picks.add (bonusToMagicRealm);
		
		assertEquals (100 * (1 - (0.92d * 0.9d)), calc.calculateCastingCostReduction (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		spell.setSpellRealm ("MB02");
		assertEquals (100 * (1 - (0.92d * 0.9d)), calc.calculateCastingCostReduction (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		spell.setSpellRealm ("MB01");
		assertEquals (100 * (1 - (0.92d * 0.9d * 0.85d)), calc.calculateCastingCostReduction (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		
		// Retort that gives +20% only when casting arcane spells
		final PlayerPick bonusToArcane = new PlayerPick ();
		bonusToArcane.setPickID ("RT03");
		bonusToArcane.setQuantity (1);
		picks.add (bonusToArcane);
		
		assertEquals (100 * (1 - (0.92d * 0.9d * 0.85d)), calc.calculateCastingCostReduction (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		spell.setSpellRealm ("MB02");
		assertEquals (100 * (1 - (0.92d * 0.9d)), calc.calculateCastingCostReduction (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		spell.setSpellRealm (null);
		assertEquals (100 * (1 - (0.92d * 0.9d * 0.8d)), calc.calculateCastingCostReduction (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		spell.setSpellRealm ("MB02");

		// Retort that gives +25% only when summoning units of a particular type
		final PlayerPick bonusToUnitType = new PlayerPick ();
		bonusToUnitType.setPickID ("RT04");
		bonusToUnitType.setQuantity (1);
		picks.add (bonusToUnitType);
		
		assertEquals (100 * (1 - (0.92d * 0.9d)), calc.calculateCastingCostReduction (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		when (spellUtils.spellSummonsUnitTypeID (spell, db)).thenReturn ("Y");
		assertEquals (100 * (1 - (0.92d * 0.9d)), calc.calculateCastingCostReduction (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		when (spellUtils.spellSummonsUnitTypeID (spell, db)).thenReturn ("X");
		assertEquals (100 * (1 - (0.92d * 0.9d * 0.75d)), calc.calculateCastingCostReduction (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
	}

	/**
	 * Tests the calculateResearchBonus method with adding bonuses together - when additive, this is basically the same as calculateCastingCostReduction
	 * @throws MomException If there is a problem
	 * @throws RecordNotFoundException If we encounter a record that can't be found in the DB
	 */
	@Test
	public final void testCalculateResearchBonusAdditive () throws MomException, RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final PickProductionBonus bonusToEverythingValue = new PickProductionBonus ();
		bonusToEverythingValue.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH);
		bonusToEverythingValue.setPercentageBonus (10);
		
		final Pick bonusToEverythingDef = new Pick ();
		bonusToEverythingDef.getPickProductionBonus ().add (bonusToEverythingValue);
		when (db.findPick ("RT01", "calculateResearchBonus")).thenReturn (bonusToEverythingDef);

		final PickProductionBonus bonusToMagicRealmValue = new PickProductionBonus ();
		bonusToMagicRealmValue.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH);
		bonusToMagicRealmValue.setPercentageBonus (15);
		bonusToMagicRealmValue.setMagicRealmID ("MB01");
		
		final Pick bonusToMagicRealmDef = new Pick ();
		bonusToMagicRealmDef.getPickProductionBonus ().add (bonusToMagicRealmValue);
		when (db.findPick ("RT02", "calculateResearchBonus")).thenReturn (bonusToMagicRealmDef);
		
		final PickProductionBonus bonusToArcaneValue = new PickProductionBonus ();
		bonusToArcaneValue.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH);
		bonusToArcaneValue.setPercentageBonus (20);
		bonusToArcaneValue.setMagicRealmIdBlank (true);
		
		final Pick bonusToArcaneDef = new Pick ();
		bonusToArcaneDef.getPickProductionBonus ().add (bonusToArcaneValue);
		when (db.findPick ("RT03", "calculateResearchBonus")).thenReturn (bonusToArcaneDef);

		final PickProductionBonus bonusToUnitTypeValue = new PickProductionBonus ();
		bonusToUnitTypeValue.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH);
		bonusToUnitTypeValue.setPercentageBonus (25);
		bonusToUnitTypeValue.setUnitTypeID ("X");
		
		final Pick bonusToUnitTypeDef = new Pick ();
		bonusToUnitTypeDef.getPickProductionBonus ().add (bonusToUnitTypeValue);
		when (db.findPick ("RT04", "calculateResearchBonus")).thenReturn (bonusToUnitTypeDef);
		
		// Spell settings
		final SpellSetting spellSettings = new SpellSetting ();
		spellSettings.setSpellBooksResearchBonusCombination (CastingReductionCombination.ADDITIVE);
		spellSettings.setSpellBooksToObtainFirstReduction (4);
		spellSettings.setSpellBooksResearchBonus (8);
		spellSettings.setSpellBooksResearchBonusCap (80);
		
		// Set up object to test
		final SpellUtils spellUtils = mock (SpellUtils.class);

		final SpellCalculationsImpl calc = new SpellCalculationsImpl ();
		calc.setSpellUtils (spellUtils);

		// Not enough books to gain any bonus
		assertEquals (0, calc.calculateResearchBonus (3, spellSettings, null, null, db), DOUBLE_TOLERANCE);
		
		// Single bonus
		assertEquals (8, calc.calculateResearchBonus (4, spellSettings, null, null, db), DOUBLE_TOLERANCE);
		
		// Additive bonus
		assertEquals (7 * 8, calc.calculateResearchBonus (10, spellSettings, null, null, db), DOUBLE_TOLERANCE);
		
		// Capped - otherwise it would be 12 * 8 = 96%
		assertEquals (80, calc.calculateResearchBonus (15, spellSettings, null, null, db), DOUBLE_TOLERANCE);
		
		// Now supply list of picks and specify the spell being cast
		final Spell spell = new Spell ();
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();

		assertEquals (8, calc.calculateResearchBonus (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		
		// Retort that gives +10% regardless of the spell being cast
		final PlayerPick bonusToEverything = new PlayerPick ();
		bonusToEverything.setPickID ("RT01");
		bonusToEverything.setQuantity (1);
		picks.add (bonusToEverything);
		
		assertEquals (18, calc.calculateResearchBonus (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);

		// Retort that gives +15% only when casting spells of particular magic realm
		final PlayerPick bonusToMagicRealm = new PlayerPick ();
		bonusToMagicRealm.setPickID ("RT02");
		bonusToMagicRealm.setQuantity (1);
		picks.add (bonusToMagicRealm);
		
		assertEquals (18, calc.calculateResearchBonus (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		spell.setSpellRealm ("MB02");
		assertEquals (18, calc.calculateResearchBonus (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		spell.setSpellRealm ("MB01");
		assertEquals (33, calc.calculateResearchBonus (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		
		// Retort that gives +20% only when casting arcane spells
		final PlayerPick bonusToArcane = new PlayerPick ();
		bonusToArcane.setPickID ("RT03");
		bonusToArcane.setQuantity (1);
		picks.add (bonusToArcane);
		
		assertEquals (33, calc.calculateResearchBonus (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		spell.setSpellRealm ("MB02");
		assertEquals (18, calc.calculateResearchBonus (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		spell.setSpellRealm (null);
		assertEquals (38, calc.calculateResearchBonus (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		spell.setSpellRealm ("MB02");

		// Retort that gives +25% only when summoning units of a particular type
		final PlayerPick bonusToUnitType = new PlayerPick ();
		bonusToUnitType.setPickID ("RT04");
		bonusToUnitType.setQuantity (1);
		picks.add (bonusToUnitType);
		
		assertEquals (18, calc.calculateResearchBonus (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		when (spellUtils.spellSummonsUnitTypeID (spell, db)).thenReturn ("Y");
		assertEquals (18, calc.calculateResearchBonus (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		when (spellUtils.spellSummonsUnitTypeID (spell, db)).thenReturn ("X");
		assertEquals (43, calc.calculateResearchBonus (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
	}

	/**
	 * Tests the calculateResearchBonus method with multiplying bonuses together
	 * @throws MomException If there is a problem
	 * @throws RecordNotFoundException If we encounter a record that can't be found in the DB
	 */
	@Test
	public final void testCalculateResearchBonusMultiplicative () throws MomException, RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final PickProductionBonus bonusToEverythingValue = new PickProductionBonus ();
		bonusToEverythingValue.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH);
		bonusToEverythingValue.setPercentageBonus (10);
		
		final Pick bonusToEverythingDef = new Pick ();
		bonusToEverythingDef.getPickProductionBonus ().add (bonusToEverythingValue);
		when (db.findPick ("RT01", "calculateResearchBonus")).thenReturn (bonusToEverythingDef);

		final PickProductionBonus bonusToMagicRealmValue = new PickProductionBonus ();
		bonusToMagicRealmValue.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH);
		bonusToMagicRealmValue.setPercentageBonus (15);
		bonusToMagicRealmValue.setMagicRealmID ("MB01");
		
		final Pick bonusToMagicRealmDef = new Pick ();
		bonusToMagicRealmDef.getPickProductionBonus ().add (bonusToMagicRealmValue);
		when (db.findPick ("RT02", "calculateResearchBonus")).thenReturn (bonusToMagicRealmDef);
		
		final PickProductionBonus bonusToArcaneValue = new PickProductionBonus ();
		bonusToArcaneValue.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH);
		bonusToArcaneValue.setPercentageBonus (20);
		bonusToArcaneValue.setMagicRealmIdBlank (true);
		
		final Pick bonusToArcaneDef = new Pick ();
		bonusToArcaneDef.getPickProductionBonus ().add (bonusToArcaneValue);
		when (db.findPick ("RT03", "calculateResearchBonus")).thenReturn (bonusToArcaneDef);

		final PickProductionBonus bonusToUnitTypeValue = new PickProductionBonus ();
		bonusToUnitTypeValue.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH);
		bonusToUnitTypeValue.setPercentageBonus (25);
		bonusToUnitTypeValue.setUnitTypeID ("X");
		
		final Pick bonusToUnitTypeDef = new Pick ();
		bonusToUnitTypeDef.getPickProductionBonus ().add (bonusToUnitTypeValue);
		when (db.findPick ("RT04", "calculateResearchBonus")).thenReturn (bonusToUnitTypeDef);
		
		// Spell settings
		final SpellSetting spellSettings = new SpellSetting ();
		spellSettings.setSpellBooksResearchBonusCombination (CastingReductionCombination.MULTIPLICATIVE);
		spellSettings.setSpellBooksToObtainFirstReduction (4);
		spellSettings.setSpellBooksResearchBonus (8);
		spellSettings.setSpellBooksResearchBonusCap (80);
		
		// Set up object to test
		final SpellUtils spellUtils = mock (SpellUtils.class);

		final SpellCalculationsImpl calc = new SpellCalculationsImpl ();
		calc.setSpellUtils (spellUtils);

		// Not enough books to gain any bonus
		assertEquals (0, calc.calculateResearchBonus (3, spellSettings, null, null, db), DOUBLE_TOLERANCE);
		
		// Single bonus
		assertEquals (8, calc.calculateResearchBonus (4, spellSettings, null, null, db), DOUBLE_TOLERANCE);
		
		// Multiplicative bonus
		assertEquals (100 * (Math.pow (1.08d, 7) - 1), calc.calculateResearchBonus (10, spellSettings, null, null, db), DOUBLE_TOLERANCE);
		
		// Capped - otherwise it would be 100 * ((1.08 ^ 8) - 1) = 85.09%
		assertEquals (80, calc.calculateResearchBonus (11, spellSettings, null, null, db), DOUBLE_TOLERANCE);
		
		// Now supply list of picks and specify the spell being cast
		final Spell spell = new Spell ();
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();

		assertEquals (8, calc.calculateResearchBonus (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		
		// Retort that gives +10% regardless of the spell being cast
		final PlayerPick bonusToEverything = new PlayerPick ();
		bonusToEverything.setPickID ("RT01");
		bonusToEverything.setQuantity (1);
		picks.add (bonusToEverything);
		
		assertEquals (100 * ((1.08d * 1.1d) - 1), calc.calculateResearchBonus (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);

		// Retort that gives +15% only when casting spells of particular magic realm
		final PlayerPick bonusToMagicRealm = new PlayerPick ();
		bonusToMagicRealm.setPickID ("RT02");
		bonusToMagicRealm.setQuantity (1);
		picks.add (bonusToMagicRealm);
		
		assertEquals (100 * ((1.08d * 1.1d) - 1), calc.calculateResearchBonus (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		spell.setSpellRealm ("MB02");
		assertEquals (100 * ((1.08d * 1.1d) - 1), calc.calculateResearchBonus (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		spell.setSpellRealm ("MB01");
		assertEquals (100 * ((1.08d * 1.1d * 1.15d) - 1), calc.calculateResearchBonus (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		
		// Retort that gives +20% only when casting arcane spells
		final PlayerPick bonusToArcane = new PlayerPick ();
		bonusToArcane.setPickID ("RT03");
		bonusToArcane.setQuantity (1);
		picks.add (bonusToArcane);
		
		assertEquals (100 * ((1.08d * 1.1d * 1.15d) - 1), calc.calculateResearchBonus (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		spell.setSpellRealm ("MB02");
		assertEquals (100 * ((1.08d * 1.1d) - 1), calc.calculateResearchBonus (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		spell.setSpellRealm (null);
		assertEquals (100 * ((1.08d * 1.1d * 1.2d) - 1), calc.calculateResearchBonus (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		spell.setSpellRealm ("MB02");

		// Retort that gives +25% only when summoning units of a particular type
		final PlayerPick bonusToUnitType = new PlayerPick ();
		bonusToUnitType.setPickID ("RT04");
		bonusToUnitType.setQuantity (1);
		picks.add (bonusToUnitType);
		
		assertEquals (100 * ((1.08d * 1.1d) - 1), calc.calculateResearchBonus (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		when (spellUtils.spellSummonsUnitTypeID (spell, db)).thenReturn ("Y");
		assertEquals (100 * ((1.08d * 1.1d) - 1), calc.calculateResearchBonus (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
		when (spellUtils.spellSummonsUnitTypeID (spell, db)).thenReturn ("X");
		assertEquals (100 * ((1.08d * 1.1d * 1.25d) - 1), calc.calculateResearchBonus (4, spellSettings, spell, picks, db), DOUBLE_TOLERANCE);
	}
	
	/**
	 * Tests the calculateDoubleCombatCastingRangePenalty method
	 */
	@Test
	public final void testCalculateDoubleCombatCastingRangePenalty ()
	{
		// These are only referenced by mocks, so don't need anything real here
		final MapVolumeOfMemoryGridCells map = new MapVolumeOfMemoryGridCells ();
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		final CoordinateSystem overlandMapCoordinateSystem = GenerateTestData.createOverlandMapCoordinateSystem ();
		
		// Player
		final PlayerDescription pd = new PlayerDescription ();
		final MomPersistentPlayerPublicKnowledge ppk = new MomPersistentPlayerPublicKnowledge ();
		pd.setPlayerID (3);
		final PlayerPublicDetails player = new PlayerPublicDetails (pd, ppk, null);
		
		// Set up object to test
		final MemoryBuildingUtils utils = mock (MemoryBuildingUtils.class);
		final PlayerPickUtils picks = mock (PlayerPickUtils.class);
		final SpellCalculationsImpl calc = new SpellCalculationsImpl ();
		calc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		calc.setMemoryBuildingUtils (utils);
		calc.setPlayerPickUtils (picks);
		
		// We're banished
		final MapCoordinates3DEx location1 = new MapCoordinates3DEx (3, 7, 0);
		assertNull (calc.calculateDoubleCombatCastingRangePenalty (player, location1, false, map, buildings, overlandMapCoordinateSystem));
		
		// Combat at wizard's fortress
		final MemoryBuilding fortressLocation = new MemoryBuilding ();
		fortressLocation.setCityLocation (new MapCoordinates3DEx (3, 7, 0));
		when (utils.findCityWithBuilding (pd.getPlayerID (), CommonDatabaseConstants.BUILDING_FORTRESS, map, buildings)).thenReturn (fortressLocation);

		assertEquals (1, calc.calculateDoubleCombatCastingRangePenalty (player, location1, false, map, buildings, overlandMapCoordinateSystem).intValue ());
		
		// Combat right next to wizard's fortress
		final MapCoordinates3DEx location2 = new MapCoordinates3DEx (4, 7, 0);
		assertEquals (2, calc.calculateDoubleCombatCastingRangePenalty (player, location2, false, map, buildings, overlandMapCoordinateSystem).intValue ());
		
		// Combat 1 square away but on other plane
		final MapCoordinates3DEx location3 = new MapCoordinates3DEx (4, 7, 1);
		assertEquals (6, calc.calculateDoubleCombatCastingRangePenalty (player, location3, false, map, buildings, overlandMapCoordinateSystem).intValue ());
		assertEquals (2, calc.calculateDoubleCombatCastingRangePenalty (player, location3, true, map, buildings, overlandMapCoordinateSystem).intValue ());
		
		// 5 across, 3 down = 5.83 distance away
		final MapCoordinates3DEx location4 = new MapCoordinates3DEx (3+5, 7+3, 0);
		assertEquals (2, calc.calculateDoubleCombatCastingRangePenalty (player, location4, false, map, buildings, overlandMapCoordinateSystem).intValue ());

		// Same, but go left, across the wrapping boundary
		final MapCoordinates3DEx location5 = new MapCoordinates3DEx (58, 7+3, 0);
		assertEquals (2, calc.calculateDoubleCombatCastingRangePenalty (player, location5, false, map, buildings, overlandMapCoordinateSystem).intValue ());
		
		// 6 across, 0 down = 6.00 away
		final MapCoordinates3DEx location6 = new MapCoordinates3DEx (3+6, 7, 0);
		assertEquals (3, calc.calculateDoubleCombatCastingRangePenalty (player, location6, false, map, buildings, overlandMapCoordinateSystem).intValue ());

		// 6 across, 20 down = 20.88 away
		final MapCoordinates3DEx location7 = new MapCoordinates3DEx (3+6, 7+20, 0);
		assertEquals (5, calc.calculateDoubleCombatCastingRangePenalty (player, location7, false, map, buildings, overlandMapCoordinateSystem).intValue ());

		// Prove caps at 3x
		final MapCoordinates3DEx location8 = new MapCoordinates3DEx (3+20, 7+20, 0);
		assertEquals (6, calc.calculateDoubleCombatCastingRangePenalty (player, location8, false, map, buildings, overlandMapCoordinateSystem).intValue ());
		
		// Channeler makes everying x1 unless right at wizard's fortress
		when (picks.getQuantityOfPick (ppk.getPick (), CommonDatabaseConstants.RETORT_ID_CHANNELER)).thenReturn (1);
		assertEquals (1, calc.calculateDoubleCombatCastingRangePenalty (player, location1, false, map, buildings, overlandMapCoordinateSystem).intValue ());
		assertEquals (2, calc.calculateDoubleCombatCastingRangePenalty (player, location2, false, map, buildings, overlandMapCoordinateSystem).intValue ());
		assertEquals (2, calc.calculateDoubleCombatCastingRangePenalty (player, location3, false, map, buildings, overlandMapCoordinateSystem).intValue ());
		assertEquals (2, calc.calculateDoubleCombatCastingRangePenalty (player, location3, true, map, buildings, overlandMapCoordinateSystem).intValue ());
		assertEquals (2, calc.calculateDoubleCombatCastingRangePenalty (player, location6, false, map, buildings, overlandMapCoordinateSystem).intValue ());
		assertEquals (2, calc.calculateDoubleCombatCastingRangePenalty (player, location7, false, map, buildings, overlandMapCoordinateSystem).intValue ());
		assertEquals (2, calc.calculateDoubleCombatCastingRangePenalty (player, location8, false, map, buildings, overlandMapCoordinateSystem).intValue ());
	}
}