package momime.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import momime.common.MomException;
import momime.common.calculations.SpellCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.SpellSetting;
import momime.common.database.SpellValidUnitTarget;
import momime.common.database.SummonedUnit;
import momime.common.database.Unit;
import momime.common.database.UnitMagicRealm;
import momime.common.messages.PlayerPick;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;

/**
 * Tests the SpellUtils class
 */
public final class TestSpellUtilsImpl
{
	/** Life creature */
	private static final String LIFE_CREATURE = "LT01";

	/** Chaos channeled creature */
	private static final String CHAOS_CHANNELED_CREATURE = "LTCC";

	/** Undead creature */
	private static final String UNDEAD_CREATURE = "LTU";

	/** Resistance attribute */
	private static final String RESISTANCE = "UA05";

	// Methods dealing with a single spell

	/**
	 * Tests the findSpellResearchStatus method on a spell that exists
	 * @throws RecordNotFoundException If the research status for this spell can't be found
	 */
	@Test
	public final void testFindSpellResearchStatus_Exists () throws RecordNotFoundException
	{
		// Set up dummy spell list
		final List<SpellResearchStatus> statuses = new ArrayList<SpellResearchStatus> ();
		for (int n = 1; n <= 3; n++)
		{
			final SpellResearchStatus thisStatus = new SpellResearchStatus ();
			thisStatus.setSpellID ("SP00" + n);
			statuses.add (thisStatus);
		}

		// Set up object to test
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		
		// Run method
		assertEquals ("SP002", utils.findSpellResearchStatus (statuses, "SP002").getSpellID ());
	}

	/**
	 * Tests the findSpellResearchStatus method on a spell that doesn't exist
	 * @throws RecordNotFoundException If the research status for this spell can't be found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindSpellResearchStatus_NotExists () throws RecordNotFoundException
	{
		// Set up dummy spell list
		final List<SpellResearchStatus> statuses = new ArrayList<SpellResearchStatus> ();
		for (int n = 1; n <= 3; n++)
		{
			final SpellResearchStatus thisStatus = new SpellResearchStatus ();
			thisStatus.setSpellID ("SP00" + n);
			statuses.add (thisStatus);
		}

		// Set up object to test
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		
		// Run method
		utils.findSpellResearchStatus (statuses, "SP004");
	}

	/**
	 * Tests the spellSummonsUnitTypeID method
	 * @throws MomException If there is a problem
	 * @throws RecordNotFoundException If we encounter a record that can't be found in the DB
	 */
	@Test
	public final void testSpellSummonsUnitTypeID_Valid () throws MomException, RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final UnitMagicRealm magicRealm = new UnitMagicRealm ();
		magicRealm.setUnitTypeID ("X");
		when (db.findUnitMagicRealm ("MB01", "spellSummonsUnitTypeID")).thenReturn (magicRealm);
		
		final Spell nonSummoningSpell = new Spell ();
		nonSummoningSpell.setSpellBookSectionID (SpellBookSectionID.OVERLAND_ENCHANTMENTS);

		final Spell summoningSpell = new Spell ();
		summoningSpell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);

		for (int n = 1; n <= 3; n++)
		{
			final SummonedUnit summonedUnit = new SummonedUnit ();
			summonedUnit.setSummonedUnitID ("UN00" + n);
			summoningSpell.getSummonedUnit ().add (summonedUnit);
			
			final Unit unitDef = new Unit ();
			unitDef.setUnitMagicRealm ("MB01");
			when (db.findUnit (summonedUnit.getSummonedUnitID (), "spellSummonsUnitTypeID")).thenReturn (unitDef);
		}
		
		// Set up object to test
		final SpellUtilsImpl utils = new SpellUtilsImpl ();

		// Run method
		assertNull (utils.spellSummonsUnitTypeID (nonSummoningSpell, db));
		assertEquals ("X", utils.spellSummonsUnitTypeID (summoningSpell, db));
	}

	/**
	 * Tests the spellSummonsUnitTypeID method when a spell is set to summon units of multiple unit types
	 * @throws MomException If there is a problem
	 * @throws RecordNotFoundException If we encounter a record that can't be found in the DB
	 */
	@Test(expected=MomException.class)
	public final void testSpellSummonsUnitTypeID_Invalid () throws MomException, RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final UnitMagicRealm magicRealmOne = new UnitMagicRealm ();
		magicRealmOne.setUnitTypeID ("X");
		when (db.findUnitMagicRealm ("MB01", "spellSummonsUnitTypeID")).thenReturn (magicRealmOne);
		
		final UnitMagicRealm magicRealmTwo = new UnitMagicRealm ();
		magicRealmTwo.setUnitTypeID ("Y");
		when (db.findUnitMagicRealm ("MB02", "spellSummonsUnitTypeID")).thenReturn (magicRealmTwo);
		
		final Spell summoningSpell = new Spell ();
		summoningSpell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);

		for (int n = 1; n <= 2; n++)
		{
			final SummonedUnit summonedUnit = new SummonedUnit ();
			summonedUnit.setSummonedUnitID ("UN00" + n);
			summoningSpell.getSummonedUnit ().add (summonedUnit);
			
			final Unit unitDef = new Unit ();
			unitDef.setUnitMagicRealm ("MB0" + n);
			when (db.findUnit (summonedUnit.getSummonedUnitID (), "spellSummonsUnitTypeID")).thenReturn (unitDef);
		}
		
		// Set up object to test
		final SpellUtilsImpl utils = new SpellUtilsImpl ();

		// Run method
		utils.spellSummonsUnitTypeID (summoningSpell, db);
	}

	/**
	 * Tests the spellCanBeCastIn method on an overland-only spell
	 * @throws MomException If there is a problem
	 */
	@Test
	public final void testSpellCanBeCastIn_OverlandOnly () throws MomException
	{
		final Spell spell = new Spell ();
		spell.setOverlandCastingCost (1);

		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		assertTrue (utils.spellCanBeCastIn (spell, SpellCastType.OVERLAND));
		assertFalse (utils.spellCanBeCastIn (spell, SpellCastType.COMBAT));
	}

	/**
	 * Tests the spellCanBeCastIn method on an overland-only spell
	 * @throws MomException If there is a problem
	 */
	@Test
	public final void testSpellCanBeCastIn_CombatOnly () throws MomException
	{
		final Spell spell = new Spell ();
		spell.setCombatCastingCost (1);

		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		assertFalse (utils.spellCanBeCastIn (spell, SpellCastType.OVERLAND));
		assertTrue (utils.spellCanBeCastIn (spell, SpellCastType.COMBAT));
	}

	/**
	 * Tests the spellCanBeCastIn method on an overland-only spell
	 * @throws MomException If there is a problem
	 */
	@Test
	public final void testSpellCanBeCastIn_Both () throws MomException
	{
		final Spell spell = new Spell ();
		spell.setOverlandCastingCost (1);
		spell.setCombatCastingCost (1);

		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		assertTrue (utils.spellCanBeCastIn (spell, SpellCastType.OVERLAND));
		assertTrue (utils.spellCanBeCastIn (spell, SpellCastType.COMBAT));
	}

	/**
	 * Tests the getReducedOverlandCastingCost method
	 * @throws MomException If there is a problem
	 * @throws RecordNotFoundException If the test db doesn't include an entry that we reference
	 */
	@Test
	public final void testGetReducedOverlandCastingCost () throws MomException, RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Numbers of books that we have
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);

		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		when (playerPickUtils.getQuantityOfPick (picks, "MB01")).thenReturn (4);
		when (playerPickUtils.getQuantityOfPick (picks, "MB02")).thenReturn (6);
		
		// Casting cost reduction
		final SpellCalculations calc = mock (SpellCalculations.class);
		final SpellSetting spellSettings = new SpellSetting ();
		final Spell spell = new Spell ();
		
		when (calc.calculateCastingCostReduction (4, spellSettings, spell, picks, db)).thenReturn (10d);
		when (calc.calculateCastingCostReduction (6, spellSettings, spell, picks, db)).thenReturn (20d);
		
		// Set up object to test
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		utils.setPlayerPickUtils (playerPickUtils);
		utils.setSpellCalculations (calc);

		// Arcane spell gives no bonus
		spell.setOverlandCastingCost (80);
		spell.setCombatCastingCost (100);
		assertEquals (80, utils.getReducedOverlandCastingCost (spell, picks, spellSettings, db));
		
		// Now try spells with magic realms that get a reduction
		spell.setSpellRealm ("MB01");
		assertEquals (80-8, utils.getReducedOverlandCastingCost (spell, picks, spellSettings, db));
		
		spell.setSpellRealm ("MB02");
		assertEquals (80-16, utils.getReducedOverlandCastingCost (spell, picks, spellSettings, db));
	}

	/**
	 * Tests the getReducedCombatCastingCost method
	 * @throws MomException If there is a problem
	 * @throws RecordNotFoundException If the test db doesn't include an entry that we reference
	 */
	@Test
	public final void testGetReducedCombatCastingCost () throws MomException, RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Numbers of books that we have
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);

		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		when (playerPickUtils.getQuantityOfPick (picks, "MB01")).thenReturn (4);
		when (playerPickUtils.getQuantityOfPick (picks, "MB02")).thenReturn (6);
		
		// Casting cost reduction
		final SpellCalculations calc = mock (SpellCalculations.class);
		final SpellSetting spellSettings = new SpellSetting ();
		final Spell spell = new Spell ();
		
		when (calc.calculateCastingCostReduction (4, spellSettings, spell, picks, db)).thenReturn (10d);
		when (calc.calculateCastingCostReduction (6, spellSettings, spell, picks, db)).thenReturn (20d);
		
		// Set up object to test
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		utils.setPlayerPickUtils (playerPickUtils);
		utils.setSpellCalculations (calc);

		// Arcane spell gives no bonus
		spell.setOverlandCastingCost (100);
		spell.setCombatCastingCost (80);
		assertEquals (80, utils.getReducedCombatCastingCost (spell, picks, spellSettings, db));
		
		// Now try spells with magic realms that get a reduction
		spell.setSpellRealm ("MB01");
		assertEquals (80-8, utils.getReducedCombatCastingCost (spell, picks, spellSettings, db));
		
		spell.setSpellRealm ("MB02");
		assertEquals (80-16, utils.getReducedCombatCastingCost (spell, picks, spellSettings, db));
	}

	/**
	 * Tests the getModifiedSectionID method when we pass flag to say don't modify it
	 * @throws MomException if the status is invalid
	 */
	@Test
	public final void testModifiedGetSectionID_Unmodified () throws MomException
	{
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);

		// This also proves that passing in null doesn't throw a NullPointerException if we pass in false
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		assertEquals (SpellBookSectionID.SUMMONING, utils.getModifiedSectionID (spell, null, false));
	}

	/**
	 * Tests the getModifiedSectionID method when the spell is unavailable
	 * @throws MomException if the status is invalid
	 */
	@Test
	public final void testModifiedGetSectionID_Unavailable () throws MomException
	{
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);

		// Unmodified section
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.UNAVAILABLE);

		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		assertNull (utils.getModifiedSectionID (spell, researchStatus, true));
	}

	/**
	 * Tests the getModifiedSectionID method when the spell isn't in our spell book
	 * @throws MomException if the status is invalid
	 */
	@Test
	public final void testModifiedGetSectionID_NotInSpellBook () throws MomException
	{
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);

		// Unmodified section
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.NOT_IN_SPELL_BOOK);

		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		assertNull (utils.getModifiedSectionID (spell, researchStatus, true));
	}

	/**
	 * Tests the getModifiedSectionID method when the spell is in our spell book but we can't learn it yet
	 * @throws MomException if the status is invalid
	 */
	@Test
	public final void testModifiedGetSectionID_Researchable () throws MomException
	{
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);

		// Unmodified section
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.RESEARCHABLE);

		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		assertEquals (SpellBookSectionID.RESEARCHABLE, utils.getModifiedSectionID (spell, researchStatus, true));
	}

	/**
	 * Tests the getModifiedSectionID method when the spell is one of the 8 we can now learn
	 * @throws MomException if the status is invalid
	 */
	@Test
	public final void testModifiedGetSectionID_ResearchableNow () throws MomException
	{
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);

		// Unmodified section
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.RESEARCHABLE_NOW);

		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		assertEquals (SpellBookSectionID.RESEARCHABLE_NOW, utils.getModifiedSectionID (spell, researchStatus, true));
	}

	/**
	 * Tests the getModifiedSectionID method when the spell is one that we know
	 * @throws MomException if the status is invalid
	 */
	@Test
	public final void testModifiedGetSectionID_Available () throws MomException
	{
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);

		// Unmodified section
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);

		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		assertEquals (SpellBookSectionID.SUMMONING, utils.getModifiedSectionID (spell, researchStatus, true));
	}

	/**
	 * Tests the spellCanTargetMagicRealmLifeformType method
	 * Note this doesn't test the validMagicRealmLifeformTypeIDs output
	 */
	@Test
	public final void testSpellCanTargetMagicRealmLifeformType ()
	{
		// Set up object to test
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		
		// Resist elements can be cast on anything (has no Targets defined)
		final Spell resistElements = new Spell ();

		assertEquals ("Resist elements should be targettable against LTN", true, utils.spellCanTargetMagicRealmLifeformType (resistElements, CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL));
		assertEquals ("Resist elements should be targettable against LTH", true, utils.spellCanTargetMagicRealmLifeformType (resistElements, CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO));
		assertEquals ("Resist elements should be targettable against LT01", true, utils.spellCanTargetMagicRealmLifeformType (resistElements, LIFE_CREATURE));
		assertEquals ("Resist elements should be targettable against LTCC", true, utils.spellCanTargetMagicRealmLifeformType (resistElements, CHAOS_CHANNELED_CREATURE));
		assertEquals ("Resist elements should be targettable against LTU", true, utils.spellCanTargetMagicRealmLifeformType (resistElements, UNDEAD_CREATURE));

		// Confusion can be cast on anything, but has a saving throw modifier (so has a Target record defined)
		final Spell confusion = new Spell ();
		final SpellValidUnitTarget confusionSavingThrowModifier = new SpellValidUnitTarget ();
		confusionSavingThrowModifier.setSavingThrowSkillID (RESISTANCE);
		confusionSavingThrowModifier.setSavingThrowModifier (-4);
		confusion.getSpellValidUnitTarget ().add (confusionSavingThrowModifier);

		assertEquals ("Confusion should be targettable against LTN", true, utils.spellCanTargetMagicRealmLifeformType (confusion, CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL));
		assertEquals ("Confusion should be targettable against LTH", true, utils.spellCanTargetMagicRealmLifeformType (confusion, CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO));
		assertEquals ("Confusion should be targettable against LT01", true, utils.spellCanTargetMagicRealmLifeformType (confusion, LIFE_CREATURE));
		assertEquals ("Confusion should be targettable against LTCC", true, utils.spellCanTargetMagicRealmLifeformType (confusion, CHAOS_CHANNELED_CREATURE));
		assertEquals ("Confusion should be targettable against LTU", true, utils.spellCanTargetMagicRealmLifeformType (confusion, UNDEAD_CREATURE));

		// Shatter can only be cast on normal units (has a Target record defined, with no saving throw)
		final Spell shatter = new Spell ();
		final SpellValidUnitTarget shatterSavingThrowModifier = new SpellValidUnitTarget ();
		shatterSavingThrowModifier.setTargetMagicRealmID (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		shatterSavingThrowModifier.setSavingThrowSkillID (RESISTANCE);
		shatter.getSpellValidUnitTarget ().add (shatterSavingThrowModifier);

		assertEquals ("Shatter should be targettable against LTN", true, utils.spellCanTargetMagicRealmLifeformType (shatter, CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL));
		assertEquals ("Shatter shouldn't be targettable against LTH", false, utils.spellCanTargetMagicRealmLifeformType (shatter, CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO));
		assertEquals ("Shatter shouldn't be targettable against LT01", false, utils.spellCanTargetMagicRealmLifeformType (shatter, LIFE_CREATURE));
		assertEquals ("Shatter shouldn't be targettable against LTCC", false, utils.spellCanTargetMagicRealmLifeformType (shatter, CHAOS_CHANNELED_CREATURE));
		assertEquals ("Shatter shouldn't be targettable against LTU", false, utils.spellCanTargetMagicRealmLifeformType (shatter, UNDEAD_CREATURE));

		// Flame Blade can be cast on normal units, heroes and Chaos Channeled units, but has no saving throw
		final Spell flameBlade = new Spell ();
		for (final String magicRealmID : new String [] {CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL,
			CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO, CHAOS_CHANNELED_CREATURE})
		{
			final SpellValidUnitTarget flameBladeTarget = new SpellValidUnitTarget ();
			flameBladeTarget.setTargetMagicRealmID (magicRealmID);
			flameBlade.getSpellValidUnitTarget ().add (flameBladeTarget);
		}

		assertEquals ("Flame blade should be targettable against LTN", true, utils.spellCanTargetMagicRealmLifeformType (flameBlade, CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL));
		assertEquals ("Flame blade should be targettable against LTH", true, utils.spellCanTargetMagicRealmLifeformType (flameBlade, CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO));
		assertEquals ("Flame blade shouldn't be targettable against LT01", false, utils.spellCanTargetMagicRealmLifeformType (flameBlade, LIFE_CREATURE));
		assertEquals ("Flame blade should be targettable against LTCC", true, utils.spellCanTargetMagicRealmLifeformType (flameBlade, CHAOS_CHANNELED_CREATURE));
		assertEquals ("Flame blade shouldn't be targettable against LTU", false, utils.spellCanTargetMagicRealmLifeformType (flameBlade, UNDEAD_CREATURE));
	}
	
	/**
	 * Tests the findMagicRealmLifeformTypeTarget method
	 */
	@Test
	public final void testFindMagicRealmLifeformTypeTarget ()
	{
		// Set up object to test
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		
		// If nothing in list, then get null back
		final Spell spell = new Spell ();
		assertNull (utils.findMagicRealmLifeformTypeTarget (spell, UNDEAD_CREATURE));
		
		// If list contains wrong type, then get null back
		final SpellValidUnitTarget specificSavingThrowModifier = new SpellValidUnitTarget ();
		specificSavingThrowModifier.setTargetMagicRealmID (LIFE_CREATURE);
		spell.getSpellValidUnitTarget ().add (specificSavingThrowModifier);
		assertNull (utils.findMagicRealmLifeformTypeTarget (spell, UNDEAD_CREATURE));
		
		// If list contains exact type, then get it back
		assertEquals (LIFE_CREATURE, utils.findMagicRealmLifeformTypeTarget (spell, LIFE_CREATURE).getTargetMagicRealmID ());
		
		// Add a null entry into the list as well
		final SpellValidUnitTarget generalSavingThrowModifier = new SpellValidUnitTarget ();
		spell.getSpellValidUnitTarget ().add (generalSavingThrowModifier);

		assertNull (utils.findMagicRealmLifeformTypeTarget (spell, UNDEAD_CREATURE).getTargetMagicRealmID ());
		assertEquals (LIFE_CREATURE, utils.findMagicRealmLifeformTypeTarget (spell, LIFE_CREATURE).getTargetMagicRealmID ());
		
		// Prove it behaves the same if the null comes first, i.e. that the LIFE_CREATURE doesn't exist as soon as it finds the null, and takes the specific record in preference
		spell.getSpellValidUnitTarget ().remove (generalSavingThrowModifier);
		spell.getSpellValidUnitTarget ().add (0, generalSavingThrowModifier);

		assertNull (utils.findMagicRealmLifeformTypeTarget (spell, UNDEAD_CREATURE).getTargetMagicRealmID ());
		assertEquals (LIFE_CREATURE, utils.findMagicRealmLifeformTypeTarget (spell, LIFE_CREATURE).getTargetMagicRealmID ());
	}

	// Methods dealing with lists of spells

	/**
	 * Tests the getSpellsForRealmRankStatus method
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Test
	public final void testGetSpellsForRealmRankStatus () throws RecordNotFoundException
	{
		// Mock list of spells
		final CommonDatabase db = mock (CommonDatabase.class);

		final List<SpellResearchStatus> statuses = new ArrayList<SpellResearchStatus> ();
		int n = 0;
		for (final SpellResearchStatusID status : new SpellResearchStatusID [] {SpellResearchStatusID.UNAVAILABLE, SpellResearchStatusID.AVAILABLE, SpellResearchStatusID.NOT_IN_SPELL_BOOK})
			for (int realm = 1; realm <= 3; realm++)
				for (int rank = 1; rank <= 3; rank++)
				{
					n++;
					final SpellResearchStatus thisStatus = new SpellResearchStatus ();
					thisStatus.setSpellID ((n < 10) ? "SP00" + n : "SP0" + n);
					thisStatus.setStatus (status);
					statuses.add (thisStatus);
					
					final Spell spell = new Spell ();
					spell.setSpellID (thisStatus.getSpellID ());
					spell.setSpellRealm ("MB0" + realm);
					spell.setSpellRank ("SR0" + rank);
					when (db.findSpell (thisStatus.getSpellID (), "getSpellsForRealmRankStatusInternal")).thenReturn (spell);
				}

		// Set up object to test
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		
		// Run method
		final List<Spell> spells = utils.getSpellsForRealmRankStatus (statuses, "MB02", "SR02", SpellResearchStatusID.AVAILABLE, db);
		
		// Check results
		assertEquals (1, spells.size ());
		assertEquals ("SP014", spells.get (0).getSpellID ());
	}

	/**
	 * Tests the getSpellsForStatus method
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Test
	public final void testGetSpellsForStatus () throws RecordNotFoundException
	{
		// Mock list of spells
		final CommonDatabase db = mock (CommonDatabase.class);

		final List<SpellResearchStatus> statuses = new ArrayList<SpellResearchStatus> ();
		int n = 0;
		for (final SpellResearchStatusID status : new SpellResearchStatusID [] {SpellResearchStatusID.UNAVAILABLE, SpellResearchStatusID.AVAILABLE, SpellResearchStatusID.NOT_IN_SPELL_BOOK})
			for (int realm = 1; realm <= 3; realm++)
				for (int rank = 1; rank <= 3; rank++)
				{
					n++;
					final SpellResearchStatus thisStatus = new SpellResearchStatus ();
					thisStatus.setSpellID ((n < 10) ? "SP00" + n : "SP0" + n);
					thisStatus.setStatus (status);
					statuses.add (thisStatus);
					
					final Spell spell = new Spell ();
					spell.setSpellID (thisStatus.getSpellID ());
					spell.setSpellRealm ("MB0" + realm);
					spell.setSpellRank ("SR0" + rank);
					when (db.findSpell (thisStatus.getSpellID (), "getSpellsForRealmRankStatusInternal")).thenReturn (spell);
				}

		// Set up object to test
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		
		// Run method
		final List<Spell> spells = utils.getSpellsForStatus (statuses, SpellResearchStatusID.AVAILABLE, db);
		
		// Check results
		assertEquals (9, spells.size ());
		assertEquals ("SP010", spells.get (0).getSpellID ());
		assertEquals ("SP011", spells.get (1).getSpellID ());
		assertEquals ("SP012", spells.get (2).getSpellID ());
		assertEquals ("SP013", spells.get (3).getSpellID ());
		assertEquals ("SP014", spells.get (4).getSpellID ());
		assertEquals ("SP015", spells.get (5).getSpellID ());
		assertEquals ("SP016", spells.get (6).getSpellID ());
		assertEquals ("SP017", spells.get (7).getSpellID ());
		assertEquals ("SP018", spells.get (8).getSpellID ());
	}

	/**
	 * Tests the getSpellsForRealmAndRank method
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Test
	public final void testGetSpellsForRealmAndRank () throws RecordNotFoundException
	{
		// Mock list of spells
		final CommonDatabase db = mock (CommonDatabase.class);

		final List<SpellResearchStatus> statuses = new ArrayList<SpellResearchStatus> ();
		int n = 0;
		for (final SpellResearchStatusID status : new SpellResearchStatusID [] {SpellResearchStatusID.UNAVAILABLE, SpellResearchStatusID.AVAILABLE, SpellResearchStatusID.NOT_IN_SPELL_BOOK})
			for (int realm = 1; realm <= 3; realm++)
				for (int rank = 1; rank <= 3; rank++)
				{
					n++;
					final SpellResearchStatus thisStatus = new SpellResearchStatus ();
					thisStatus.setSpellID ((n < 10) ? "SP00" + n : "SP0" + n);
					thisStatus.setStatus (status);
					statuses.add (thisStatus);
					
					final Spell spell = new Spell ();
					spell.setSpellID (thisStatus.getSpellID ());
					spell.setSpellRealm ("MB0" + realm);
					spell.setSpellRank ("SR0" + rank);
					when (db.findSpell (thisStatus.getSpellID (), "getSpellsForRealmRankStatusInternal")).thenReturn (spell);
				}

		// Set up object to test
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		
		// Run method
		final List<Spell> spells = utils.getSpellsForRealmAndRank (statuses, "MB02", "SR02", db);
		
		// Check results
		assertEquals (3, spells.size ());
		assertEquals ("SP005", spells.get (0).getSpellID ());
		assertEquals ("SP014", spells.get (1).getSpellID ());
		assertEquals ("SP023", spells.get (2).getSpellID ());
	}

	/**
	 * Tests the getSpellsForRankAndStatus method
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Test
	public final void testGetSpellsForRankAndStatus () throws RecordNotFoundException
	{
		// Mock list of spells
		final CommonDatabase db = mock (CommonDatabase.class);

		final List<SpellResearchStatus> statuses = new ArrayList<SpellResearchStatus> ();
		int n = 0;
		for (final SpellResearchStatusID status : new SpellResearchStatusID [] {SpellResearchStatusID.UNAVAILABLE, SpellResearchStatusID.AVAILABLE, SpellResearchStatusID.NOT_IN_SPELL_BOOK})
			for (int realm = 1; realm <= 3; realm++)
				for (int rank = 1; rank <= 3; rank++)
				{
					n++;
					final SpellResearchStatus thisStatus = new SpellResearchStatus ();
					thisStatus.setSpellID ((n < 10) ? "SP00" + n : "SP0" + n);
					thisStatus.setStatus (status);
					statuses.add (thisStatus);
					
					final Spell spell = new Spell ();
					spell.setSpellID (thisStatus.getSpellID ());
					spell.setSpellRealm ("MB0" + realm);
					spell.setSpellRank ("SR0" + rank);
					when (db.findSpell (thisStatus.getSpellID (), "getSpellsForRealmRankStatusInternal")).thenReturn (spell);
				}

		// Set up object to test
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		
		// Run method
		final List<Spell> spells = utils.getSpellsForRankAndStatus (statuses, "SR02", SpellResearchStatusID.AVAILABLE, db);
		
		// Check results
		assertEquals (3, spells.size ());
		assertEquals ("SP011", spells.get (0).getSpellID ());
		assertEquals ("SP014", spells.get (1).getSpellID ());
		assertEquals ("SP017", spells.get (2).getSpellID ());
	}

	/**
	 * Tests the getSpellsNotInBookForRealmAndRank method
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Test
	public final void testGetSpellsNotInBookForRealmAndRank () throws RecordNotFoundException
	{
		// Mock list of spells
		final CommonDatabase db = mock (CommonDatabase.class);

		final List<SpellResearchStatus> statuses = new ArrayList<SpellResearchStatus> ();
		int n = 0;
		for (final SpellResearchStatusID status : new SpellResearchStatusID [] {SpellResearchStatusID.UNAVAILABLE, SpellResearchStatusID.AVAILABLE, SpellResearchStatusID.NOT_IN_SPELL_BOOK})
			for (int realm = 1; realm <= 3; realm++)
				for (int rank = 1; rank <= 3; rank++)
				{
					n++;
					final SpellResearchStatus thisStatus = new SpellResearchStatus ();
					thisStatus.setSpellID ((n < 10) ? "SP00" + n : "SP0" + n);
					thisStatus.setStatus (status);
					statuses.add (thisStatus);
					
					final Spell spell = new Spell ();
					spell.setSpellID (thisStatus.getSpellID ());
					spell.setSpellRealm ("MB0" + realm);
					spell.setSpellRank ("SR0" + rank);
					when (db.findSpell (thisStatus.getSpellID (), "getSpellsForRealmRankStatusInternal")).thenReturn (spell);
				}

		// Set up object to test
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		
		// Run method
		final List<Spell> spells = utils.getSpellsNotInBookForRealmAndRank (statuses, "MB02", "SR02", db);
		
		// Check results
		assertEquals (2, spells.size ());
		assertEquals ("SP005", spells.get (0).getSpellID ());
		assertEquals ("SP023", spells.get (1).getSpellID ());
	}

	/**
	 * Tests the getSpellRanksForStatus method
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Test
	public final void testGetSpellRanksForStatus () throws RecordNotFoundException
	{
		// Mock list of spells
		final CommonDatabase db = mock (CommonDatabase.class);

		final List<SpellResearchStatus> statuses = new ArrayList<SpellResearchStatus> ();
		int n = 0;
		for (final SpellResearchStatusID status : new SpellResearchStatusID [] {SpellResearchStatusID.UNAVAILABLE, SpellResearchStatusID.AVAILABLE, SpellResearchStatusID.NOT_IN_SPELL_BOOK})
			for (int realm = 1; realm <= 3; realm++)
				for (int rank = 1; rank <= 3; rank++)
				{
					n++;
					final SpellResearchStatus thisStatus = new SpellResearchStatus ();
					thisStatus.setSpellID ((n < 10) ? "SP00" + n : "SP0" + n);
					thisStatus.setStatus (status);
					statuses.add (thisStatus);
					
					final Spell spell = new Spell ();
					spell.setSpellID (thisStatus.getSpellID ());
					spell.setSpellRealm ("MB0" + realm);
					spell.setSpellRank ("SR0" + rank);
					when (db.findSpell (thisStatus.getSpellID (), "getSpellRanksForStatus")).thenReturn (spell);
				}

		// Set up object to test
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		
		// Run method
		final List<String> spellRanks = utils.getSpellRanksForStatus (statuses, SpellResearchStatusID.AVAILABLE, db);
		
		// Check results
		assertEquals (3, spellRanks.size ());
		assertEquals ("SR01", spellRanks.get (0));
		assertEquals ("SR02", spellRanks.get (1));
		assertEquals ("SR03", spellRanks.get (2));
	}

	/**
	 * Tests the getSpellRanksForMagicRealm method
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Test
	public final void testGetSpellRanksForMagicRealm () throws RecordNotFoundException
	{
		// Set up list of spells
		final List<Spell> spells = new ArrayList<Spell> ();
		for (int realm = 1; realm <= 3; realm++)
			for (int rank = 1; rank <= 3; rank++)
			{
				final Spell spell = new Spell ();
				spell.setSpellRealm ("MB0" + realm);
				spell.setSpellRank ("SR0" + rank);
				spells.add (spell);
			}

		// Set up object to test
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		
		// Run method
		final List<String> spellRanks = utils.getSpellRanksForMagicRealm (spells, "MB02");
		
		// Check results
		assertEquals (3, spellRanks.size ());
		assertEquals ("SR01", spellRanks.get (0));
		assertEquals ("SR02", spellRanks.get (1));
		assertEquals ("SR03", spellRanks.get (2));
	}

	/**
	 * Tests the getSortedSpellsInSection method
	 * @throws MomException If we encounter an unkown research status or castType
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Test
	public final void testGetSortedSpellsInSection () throws MomException, RecordNotFoundException
	{
		// Mock list of spells
		final CommonDatabase db = mock (CommonDatabase.class);

		final List<SpellResearchStatus> statuses = new ArrayList<SpellResearchStatus> ();
		int n = 0;
		for (final SpellBookSectionID section : new SpellBookSectionID [] {SpellBookSectionID.ATTACK_SPELLS, SpellBookSectionID.OVERLAND_ENCHANTMENTS, SpellBookSectionID.SUMMONING})
			for (final SpellResearchStatusID status : new SpellResearchStatusID [] {SpellResearchStatusID.UNAVAILABLE, SpellResearchStatusID.AVAILABLE, SpellResearchStatusID.NOT_IN_SPELL_BOOK})
				for (int realm = 1; realm <= 3; realm++)
					for (int rank = 1; rank <= 3; rank++)
					{
						n++;
						final SpellResearchStatus thisStatus = new SpellResearchStatus ();
						thisStatus.setSpellID ((n < 10) ? "SP00" + n : "SP0" + n);
						thisStatus.setStatus (status);
						statuses.add (thisStatus);
						
						final Spell spell = new Spell ();
						spell.setSpellID (thisStatus.getSpellID ());
						spell.setSpellRealm ("MB0" + realm);
						spell.setSpellRank ("SR0" + rank);
						spell.setSpellBookSectionID (section);
						spell.setOverlandCastingCost (100 - n);
						when (db.findSpell (thisStatus.getSpellID (), "getSortedSpellsInSection")).thenReturn (spell);
					}

		// Set up object to test
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		
		// Run method
		final List<Spell> spells = utils.getSortedSpellsInSection (statuses, SpellBookSectionID.OVERLAND_ENCHANTMENTS, SpellCastType.OVERLAND, db);
		
		// Check results
		assertEquals (9, spells.size ());
		assertEquals ("SP045", spells.get (0).getSpellID ());
		assertEquals ("SP044", spells.get (1).getSpellID ());
		assertEquals ("SP043", spells.get (2).getSpellID ());
		assertEquals ("SP042", spells.get (3).getSpellID ());
		assertEquals ("SP041", spells.get (4).getSpellID ());
		assertEquals ("SP040", spells.get (5).getSpellID ());
		assertEquals ("SP039", spells.get (6).getSpellID ());
		assertEquals ("SP038", spells.get (7).getSpellID ());
		assertEquals ("SP037", spells.get (8).getSpellID ());
	}
}