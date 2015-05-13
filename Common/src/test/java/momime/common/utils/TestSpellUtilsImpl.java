package momime.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import momime.common.MomException;
import momime.common.calculations.SpellCalculationsImpl;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.GenerateTestData;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.SpellSetting;
import momime.common.database.SpellValidUnitTarget;
import momime.common.messages.PlayerPick;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;

import org.junit.Test;

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
		final CommonDatabase db = GenerateTestData.createDB ();

		final List<SpellResearchStatus> statuses = new ArrayList<SpellResearchStatus> ();
		for (final Spell thisSpell : db.getSpells ())
		{
			final SpellResearchStatus thisStatus = new SpellResearchStatus ();
			thisStatus.setSpellID (thisSpell.getSpellID ());
			thisStatus.setStatus (SpellResearchStatusID.UNAVAILABLE);
			statuses.add (thisStatus);
		}

		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		assertEquals (GenerateTestData.MAGIC_SPIRIT_SPELL, utils.findSpellResearchStatus (statuses, GenerateTestData.MAGIC_SPIRIT_SPELL).getSpellID ());
	}

	/**
	 * Tests the findSpellResearchStatus method on a spell that doesn't exist
	 * @throws RecordNotFoundException If the research status for this spell can't be found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindSpellResearchStatus_NotExists () throws RecordNotFoundException
	{
		final CommonDatabase db = GenerateTestData.createDB ();

		final List<SpellResearchStatus> statuses = new ArrayList<SpellResearchStatus> ();
		for (final Spell thisSpell : db.getSpells ())
		{
			final SpellResearchStatus thisStatus = new SpellResearchStatus ();
			thisStatus.setSpellID (thisSpell.getSpellID ());
			thisStatus.setStatus (SpellResearchStatusID.UNAVAILABLE);
			statuses.add (thisStatus);
		}

		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		utils.findSpellResearchStatus (statuses, "X");
	}

	/**
	 * Tests the spellSummonsUnitTypeID method
	 * @throws MomException If there is a problem
	 * @throws RecordNotFoundException If we encounter a record that can't be found in the DB
	 */
	@Test
	public final void testSpellSummonsUnitTypeID () throws MomException, RecordNotFoundException
	{
		final CommonDatabase db = GenerateTestData.createDB ();
		final SpellUtilsImpl utils = new SpellUtilsImpl ();

		assertNull (utils.spellSummonsUnitTypeID (GenerateTestData.createArcaneNormalSpell (), db));
		assertEquals ("S", utils.spellSummonsUnitTypeID (GenerateTestData.createArcaneSummoningSpell (), db));

		assertNull (utils.spellSummonsUnitTypeID (GenerateTestData.createChaosNormalSpell (), db));
		assertEquals ("S", utils.spellSummonsUnitTypeID (GenerateTestData.createArcaneSummoningSpell (), db));

		assertEquals ("H", utils.spellSummonsUnitTypeID (GenerateTestData.createSummonHeroSpell (), db));
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
		final CommonDatabase db = GenerateTestData.createDB ();
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		final SpellCalculationsImpl calc = new SpellCalculationsImpl ();
		final PlayerPickUtilsImpl playerPickUtils = new PlayerPickUtilsImpl ();
		utils.setPlayerPickUtils (playerPickUtils);
		utils.setSpellCalculations (calc);
		calc.setSpellUtils (utils);

		// Recommended spell settings - 8% per book multiplicative, 90% cap
		// Using this because all overland spell casting costs end in 0 or 5, so with nice round percentage reductions in multiples of 10% we can't get any results ending in anything other than .0 or .5
		final SpellSetting spellSettings = GenerateTestData.createRecommendedSpellSettings ();

		// 10 Nature books gives us a 22.1312% reduction
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		playerPickUtils.updatePickQuantity (picks, GenerateTestData.NATURE_BOOK, 10);

		// Sprites come out as 77.8688
		final Spell sprites = new Spell ();
		sprites.setOverlandCastingCost (100);
		sprites.setSpellRealm (GenerateTestData.NATURE_BOOK);
		assertEquals ("Sprites does not have expected reduced casting cost", 78, utils.getReducedOverlandCastingCost (sprites, picks, spellSettings, db));

		// Nature's Eye comes out as 58.4016
		final Spell naturesEye = new Spell ();
		naturesEye.setOverlandCastingCost (75);
		naturesEye.setSpellRealm (GenerateTestData.NATURE_BOOK);
		assertEquals ("Nature's Eye does not have expected reduced casting cost", 59, utils.getReducedOverlandCastingCost (naturesEye, picks, spellSettings, db));
	}

	/**
	 * Tests the getReducedCombatCastingCost method
	 * @throws MomException If there is a problem
	 * @throws RecordNotFoundException If the test db doesn't include an entry that we reference
	 */
	@Test
	public final void testGetReducedCombatCastingCost () throws MomException, RecordNotFoundException
	{
		final CommonDatabase db = GenerateTestData.createDB ();
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		final SpellCalculationsImpl calc = new SpellCalculationsImpl ();
		final PlayerPickUtilsImpl playerPickUtils = new PlayerPickUtilsImpl ();
		utils.setPlayerPickUtils (playerPickUtils);
		utils.setSpellCalculations (calc);
		calc.setSpellUtils (utils);

		// Original spell settings - 10% per book additive, 100% cap
		final SpellSetting spellSettings = GenerateTestData.createOriginalSpellSettings ();

		// 10 Nature books gives us a 30% reduction
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		playerPickUtils.updatePickQuantity (picks, GenerateTestData.NATURE_BOOK, 10);

		// Web comes out at an even 7.0
		final Spell web = new Spell ();
		web.setCombatCastingCost (10);
		web.setSpellRealm (GenerateTestData.NATURE_BOOK);
		assertEquals ("Web not have expected reduced casting cost", 7, utils.getReducedCombatCastingCost (web, picks, spellSettings, db));

		// Earth to Mud comes out at 10.5
		final Spell earthToMud = new Spell ();
		earthToMud.setCombatCastingCost (15);
		earthToMud.setSpellRealm (GenerateTestData.NATURE_BOOK);
		assertEquals ("Earth to Mud does not have expected reduced casting cost", 11, utils.getReducedCombatCastingCost (earthToMud, picks, spellSettings, db));

		// Giant Strength comes out at 5.6
		final Spell giantStrength = new Spell ();
		giantStrength.setCombatCastingCost (8);
		giantStrength.setSpellRealm (GenerateTestData.NATURE_BOOK);
		assertEquals ("Giant Strength does not have expected reduced casting cost (1)", 6, utils.getReducedCombatCastingCost (giantStrength, picks, spellSettings, db));

		// With only 20% reduction, Giant Strength comes out at 6.4
		playerPickUtils.updatePickQuantity (picks, GenerateTestData.NATURE_BOOK, -1);
		assertEquals ("Giant Strength does not have expected reduced casting cost (2)", 7, utils.getReducedCombatCastingCost (giantStrength, picks, spellSettings, db));
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
		confusionSavingThrowModifier.setSavingThrowAttributeID (RESISTANCE);
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
		shatterSavingThrowModifier.setSavingThrowAttributeID (RESISTANCE);
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
		final CommonDatabase db = GenerateTestData.createDB ();

		final List<SpellResearchStatus> statuses = new ArrayList<SpellResearchStatus> ();
		for (final Spell thisSpell : db.getSpells ())
		{
			final SpellResearchStatus thisStatus = new SpellResearchStatus ();
			thisStatus.setSpellID (thisSpell.getSpellID ());
			thisStatus.setStatus (SpellResearchStatusID.UNAVAILABLE);
			statuses.add (thisStatus);
		}

		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		
		// Do a count while they're all still unavailable
		assertEquals (2, utils.getSpellsForRealmRankStatus (statuses, GenerateTestData.CHAOS_BOOK, GenerateTestData.COMMON, SpellResearchStatusID.UNAVAILABLE, db).size ());
		assertEquals (1, utils.getSpellsForRealmRankStatus (statuses, GenerateTestData.NATURE_BOOK, GenerateTestData.UNCOMMON, SpellResearchStatusID.UNAVAILABLE, db).size ());
		assertEquals (3, utils.getSpellsForRealmRankStatus (statuses, null, GenerateTestData.COMMON, SpellResearchStatusID.UNAVAILABLE, db).size ());

		// Then change some statuses
		statuses.get (1).setStatus (SpellResearchStatusID.RESEARCHABLE);
		statuses.get (2).setStatus (SpellResearchStatusID.RESEARCHABLE_NOW);
		statuses.get (4).setStatus (SpellResearchStatusID.AVAILABLE);
		statuses.get (6).setStatus (SpellResearchStatusID.AVAILABLE);

		assertEquals (1, utils.getSpellsForRealmRankStatus (statuses, GenerateTestData.CHAOS_BOOK, GenerateTestData.COMMON, SpellResearchStatusID.UNAVAILABLE, db).size ());
		assertEquals (1, utils.getSpellsForRealmRankStatus (statuses, GenerateTestData.CHAOS_BOOK, GenerateTestData.COMMON, SpellResearchStatusID.RESEARCHABLE, db).size ());
		assertEquals (0, utils.getSpellsForRealmRankStatus (statuses, GenerateTestData.NATURE_BOOK, GenerateTestData.UNCOMMON, SpellResearchStatusID.UNAVAILABLE, db).size ());
		assertEquals (1, utils.getSpellsForRealmRankStatus (statuses, GenerateTestData.NATURE_BOOK, GenerateTestData.UNCOMMON, SpellResearchStatusID.RESEARCHABLE_NOW, db).size ());
		assertEquals (1, utils.getSpellsForRealmRankStatus (statuses, null, GenerateTestData.COMMON, SpellResearchStatusID.UNAVAILABLE, db).size ());
		assertEquals (2, utils.getSpellsForRealmRankStatus (statuses, null, GenerateTestData.COMMON, SpellResearchStatusID.AVAILABLE, db).size ());
	}

	/**
	 * Tests the getSpellsForStatus method
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Test
	public final void testGetSpellsForStatus () throws RecordNotFoundException
	{
		final CommonDatabase db = GenerateTestData.createDB ();

		final List<SpellResearchStatus> statuses = new ArrayList<SpellResearchStatus> ();
		for (final Spell thisSpell : db.getSpells ())
		{
			final SpellResearchStatus thisStatus = new SpellResearchStatus ();
			thisStatus.setSpellID (thisSpell.getSpellID ());
			thisStatus.setStatus (SpellResearchStatusID.UNAVAILABLE);
			statuses.add (thisStatus);
		}

		statuses.get (1).setStatus (SpellResearchStatusID.AVAILABLE);
		statuses.get (3).setStatus (SpellResearchStatusID.AVAILABLE);

		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		assertEquals (2, utils.getSpellsForStatus (statuses, SpellResearchStatusID.AVAILABLE, db).size ());
		assertEquals (db.getSpells ().size () - 2, utils.getSpellsForStatus (statuses, SpellResearchStatusID.UNAVAILABLE, db).size ());
	}

	/**
	 * Tests the getSpellsForRealmAndRank method
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Test
	public final void testGetSpellsForRealmAndRank () throws RecordNotFoundException
	{
		final CommonDatabase db = GenerateTestData.createDB ();
		final SpellUtilsImpl utils = new SpellUtilsImpl ();

		final List<SpellResearchStatus> statuses = new ArrayList<SpellResearchStatus> ();
		for (final Spell thisSpell : db.getSpells ())
		{
			final SpellResearchStatus thisStatus = new SpellResearchStatus ();
			thisStatus.setSpellID (thisSpell.getSpellID ());
			statuses.add (thisStatus);
		}

		// Check we can search for a proper magic realm
		assertEquals (2, utils.getSpellsForRealmAndRank (statuses, GenerateTestData.CHAOS_BOOK, GenerateTestData.COMMON, db).size ());

		// Check we can search for Arcane spells
		assertEquals (3, utils.getSpellsForRealmAndRank (statuses, null, GenerateTestData.COMMON, db).size ());
	}

	/**
	 * Tests the getSpellsForRankAndStatus method
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Test
	public final void testGetSpellsForRankAndStatus () throws RecordNotFoundException
	{
		final CommonDatabase db = GenerateTestData.createDB ();

		final List<SpellResearchStatus> statuses = new ArrayList<SpellResearchStatus> ();
		for (final Spell thisSpell : db.getSpells ())
		{
			final SpellResearchStatus thisStatus = new SpellResearchStatus ();
			thisStatus.setSpellID (thisSpell.getSpellID ());
			thisStatus.setStatus (SpellResearchStatusID.UNAVAILABLE);
			statuses.add (thisStatus);
		}

		// Do a count while they're all still unavailable
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		assertEquals (6, utils.getSpellsForRankAndStatus (statuses, GenerateTestData.COMMON, SpellResearchStatusID.UNAVAILABLE, db).size ());
		assertEquals (1, utils.getSpellsForRankAndStatus (statuses, GenerateTestData.UNCOMMON, SpellResearchStatusID.UNAVAILABLE, db).size ());

		// Set a common and an uncommon to available, so count should only drop by 1
		statuses.get (0).setStatus (SpellResearchStatusID.AVAILABLE);
		statuses.get (2).setStatus (SpellResearchStatusID.AVAILABLE);

		assertEquals (5, utils.getSpellsForRankAndStatus (statuses, GenerateTestData.COMMON, SpellResearchStatusID.UNAVAILABLE, db).size ());
		assertEquals (0, utils.getSpellsForRankAndStatus (statuses, GenerateTestData.UNCOMMON, SpellResearchStatusID.UNAVAILABLE, db).size ());
		assertEquals (1, utils.getSpellsForRankAndStatus (statuses, GenerateTestData.COMMON, SpellResearchStatusID.AVAILABLE, db).size ());
		assertEquals (1, utils.getSpellsForRankAndStatus (statuses, GenerateTestData.UNCOMMON, SpellResearchStatusID.AVAILABLE, db).size ());
	}

	/**
	 * Tests the getSpellsNotInBookForRealmAndRank method
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Test
	public final void testGetSpellsNotInBookForRealmAndRank () throws RecordNotFoundException
	{
		final CommonDatabase db = GenerateTestData.createDB ();

		final List<SpellResearchStatus> statuses = new ArrayList<SpellResearchStatus> ();
		for (final Spell thisSpell : db.getSpells ())
		{
			final SpellResearchStatus thisStatus = new SpellResearchStatus ();
			thisStatus.setSpellID (thisSpell.getSpellID ());
			thisStatus.setStatus (SpellResearchStatusID.UNAVAILABLE);
			statuses.add (thisStatus);
		}

		// Do a count while they're all still unavailable
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		assertEquals (2, utils.getSpellsNotInBookForRealmAndRank (statuses, GenerateTestData.CHAOS_BOOK, GenerateTestData.COMMON, db).size ());
		assertEquals (3, utils.getSpellsNotInBookForRealmAndRank (statuses, null, GenerateTestData.COMMON, db).size ());

		// "not in spell book" shouldn't reduce the count; a different status should
		statuses.get (1).setStatus (SpellResearchStatusID.RESEARCHABLE);
		statuses.get (5).setStatus (SpellResearchStatusID.NOT_IN_SPELL_BOOK);

		assertEquals (1, utils.getSpellsNotInBookForRealmAndRank (statuses, GenerateTestData.CHAOS_BOOK, GenerateTestData.COMMON, db).size ());
		assertEquals (3, utils.getSpellsNotInBookForRealmAndRank (statuses, null, GenerateTestData.COMMON, db).size ());
	}

	/**
	 * Tests the getSpellRanksForStatus method
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Test
	public final void testGetSpellRanksForStatus () throws RecordNotFoundException
	{
		final CommonDatabase db = GenerateTestData.createDB ();

		final List<SpellResearchStatus> statuses = new ArrayList<SpellResearchStatus> ();
		for (final Spell thisSpell : db.getSpells ())
		{
			final SpellResearchStatus thisStatus = new SpellResearchStatus ();
			thisStatus.setSpellID (thisSpell.getSpellID ());
			thisStatus.setStatus (SpellResearchStatusID.UNAVAILABLE);
			statuses.add (thisStatus);
		}

		// Do a count while they're all still unavailable
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		assertEquals (2, utils.getSpellRanksForStatus (statuses, SpellResearchStatusID.UNAVAILABLE, db).size ());

		// Only 1 uncommon spell in the test data, so changing its status should reduce the count
		statuses.get (2).setStatus (SpellResearchStatusID.RESEARCHABLE);
		assertEquals (1, utils.getSpellRanksForStatus (statuses, SpellResearchStatusID.UNAVAILABLE, db).size ());
	}

	/**
	 * Tests the getSpellRanksForMagicRealm method
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Test
	public final void testGetSpellRanksForMagicRealm () throws RecordNotFoundException
	{
		final CommonDatabase db = GenerateTestData.createDB ();
		final SpellUtilsImpl utils = new SpellUtilsImpl ();

		assertEquals (2, utils.getSpellRanksForMagicRealm (db.getSpells (), GenerateTestData.NATURE_BOOK).size ());
		assertEquals (1, utils.getSpellRanksForMagicRealm (db.getSpells (), GenerateTestData.CHAOS_BOOK).size ());
		assertEquals (1, utils.getSpellRanksForMagicRealm (db.getSpells (), null).size ());
	}

	/**
	 * Tests the getSortedSpellsInSection method
	 * @throws MomException If we encounter an unkown research status or castType
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Test
	public final void testGetSortedSpellsInSection () throws MomException, RecordNotFoundException
	{
		final CommonDatabase db = GenerateTestData.createDB ();

		final List<SpellResearchStatus> statuses = new ArrayList<SpellResearchStatus> ();
		for (final Spell thisSpell : db.getSpells ())
		{
			final SpellResearchStatus thisStatus = new SpellResearchStatus ();
			thisStatus.setSpellID (thisSpell.getSpellID ());
			thisStatus.setStatus (SpellResearchStatusID.UNAVAILABLE);
			statuses.add (thisStatus);
		}

		// Sort on overland casting cost
		// Test data contains 7 spells, but 2 are combat only
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		final List<Spell> overlandOrder = utils.getSortedSpellsInSection (statuses, null, SpellCastType.OVERLAND, db);
		assertEquals (5, overlandOrder.size ());
		assertEquals (GenerateTestData.GIANT_SPIDERS_SPELL, overlandOrder.get (0).getSpellID ());	// 3
		assertEquals (GenerateTestData.WARP_WOOD, overlandOrder.get (1).getSpellID ());					// 5
		assertEquals (GenerateTestData.MAGIC_SPIRIT_SPELL, overlandOrder.get (2).getSpellID ());		// 6
		assertEquals (GenerateTestData.HELL_HOUNDS_SPELL, overlandOrder.get (3).getSpellID ());		// 7
		assertEquals (GenerateTestData.DISPEL_MAGIC_SPELL, overlandOrder.get (4).getSpellID ());		// 9

		// Sort on combat casting cost
		// Test data contains 7 spells, but 4 are overland only
		final List<Spell> combatOrder = utils.getSortedSpellsInSection (statuses, null, SpellCastType.COMBAT, db);
		assertEquals (3, combatOrder.size ());
		assertEquals (GenerateTestData.SUMMONING_CIRCLE, combatOrder.get (0).getSpellID ());			// 5
		assertEquals (GenerateTestData.EARTH_TO_MUD, combatOrder.get (1).getSpellID ());				// 10
		assertEquals (GenerateTestData.WARP_WOOD, combatOrder.get (2).getSpellID ());					// 15

		// Sort on research cost - this should include all spells regardless of whether they are overland/combat/both
		for (final SpellResearchStatus thisStatus : statuses)
		{
			thisStatus.setStatus (SpellResearchStatusID.RESEARCHABLE_NOW);
			thisStatus.setRemainingResearchCost (db.findSpell (thisStatus.getSpellID (), "testGetSortedSpellsInSection").getResearchCost ());
		}

		assertEquals (0, utils.getSortedSpellsInSection (statuses, null, SpellCastType.OVERLAND, db).size ());
		final List<Spell> researchOrder = utils.getSortedSpellsInSection (statuses, SpellBookSectionID.RESEARCHABLE_NOW, SpellCastType.OVERLAND, db);

		assertEquals (7, researchOrder.size ());
		assertEquals (GenerateTestData.DISPEL_MAGIC_SPELL, researchOrder.get (0).getSpellID ());		// 1
		assertEquals (GenerateTestData.MAGIC_SPIRIT_SPELL, researchOrder.get (1).getSpellID ());		// 2
		assertEquals (GenerateTestData.WARP_WOOD, researchOrder.get (2).getSpellID ());					// 3
		assertEquals (GenerateTestData.SUMMONING_CIRCLE, researchOrder.get (3).getSpellID ());		// 5
		assertEquals (GenerateTestData.EARTH_TO_MUD, researchOrder.get (4).getSpellID ());				// 6
		assertEquals (GenerateTestData.GIANT_SPIDERS_SPELL, researchOrder.get (5).getSpellID ());	// 7
		assertEquals (GenerateTestData.HELL_HOUNDS_SPELL, researchOrder.get (6).getSpellID ());		// 9
	}
}