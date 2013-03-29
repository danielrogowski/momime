package momime.common.messages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ICommonDatabase;
import momime.common.database.GenerateTestData;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.v0_9_4.SpellSettingData;
import momime.common.database.v0_9_4.Spell;
import momime.common.database.v0_9_4.SpellValidUnitTarget;
import momime.common.messages.v0_9_4.PlayerPick;
import momime.common.messages.v0_9_4.SpellResearchStatus;
import momime.common.messages.v0_9_4.SpellResearchStatusID;
import momime.common.player.MomSpellCastType;

import org.junit.Test;

/**
 * Tests the SpellUtils class
 */
public final class TestSpellUtils
{
	/** Dummy logger to use during unit tests */
	private final Logger debugLogger = Logger.getLogger ("MoMIMECommonUnitTests");

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
		final ICommonDatabase db = GenerateTestData.createDB ();

		final List<SpellResearchStatus> statuses = new ArrayList<SpellResearchStatus> ();
		for (final Spell thisSpell : db.getSpell ())
		{
			final SpellResearchStatus thisStatus = new SpellResearchStatus ();
			thisStatus.setSpellID (thisSpell.getSpellID ());
			thisStatus.setStatus (SpellResearchStatusID.UNAVAILABLE);
			statuses.add (thisStatus);
		}

		assertEquals (GenerateTestData.MAGIC_SPIRIT_SPELL, SpellUtils.findSpellResearchStatus (statuses, GenerateTestData.MAGIC_SPIRIT_SPELL, debugLogger).getSpellID ());
	}

	/**
	 * Tests the findSpellResearchStatus method on a spell that doesn't exist
	 * @throws RecordNotFoundException If the research status for this spell can't be found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindSpellResearchStatus_NotExists () throws RecordNotFoundException
	{
		final ICommonDatabase db = GenerateTestData.createDB ();

		final List<SpellResearchStatus> statuses = new ArrayList<SpellResearchStatus> ();
		for (final Spell thisSpell : db.getSpell ())
		{
			final SpellResearchStatus thisStatus = new SpellResearchStatus ();
			thisStatus.setSpellID (thisSpell.getSpellID ());
			thisStatus.setStatus (SpellResearchStatusID.UNAVAILABLE);
			statuses.add (thisStatus);
		}

		SpellUtils.findSpellResearchStatus (statuses, "X", debugLogger);
	}

	/**
	 * Tests the spellSummonsUnitTypeID method
	 * @throws MomException If there is a problem
	 * @throws RecordNotFoundException If we encounter a record that can't be found in the DB
	 */
	@Test
	public final void testSpellSummonsUnitTypeID () throws MomException, RecordNotFoundException
	{
		final ICommonDatabase db = GenerateTestData.createDB ();

		assertNull (SpellUtils.spellSummonsUnitTypeID (GenerateTestData.createArcaneNormalSpell (), db, debugLogger));
		assertEquals ("S", SpellUtils.spellSummonsUnitTypeID (GenerateTestData.createArcaneSummoningSpell (), db, debugLogger));

		assertNull (SpellUtils.spellSummonsUnitTypeID (GenerateTestData.createChaosNormalSpell (), db, debugLogger));
		assertEquals ("S", SpellUtils.spellSummonsUnitTypeID (GenerateTestData.createArcaneSummoningSpell (), db, debugLogger));

		assertEquals ("H", SpellUtils.spellSummonsUnitTypeID (GenerateTestData.createSummonHeroSpell (), db, debugLogger));
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

		assertTrue (SpellUtils.spellCanBeCastIn (spell, MomSpellCastType.OVERLAND, debugLogger));
		assertFalse (SpellUtils.spellCanBeCastIn (spell, MomSpellCastType.COMBAT, debugLogger));
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

		assertFalse (SpellUtils.spellCanBeCastIn (spell, MomSpellCastType.OVERLAND, debugLogger));
		assertTrue (SpellUtils.spellCanBeCastIn (spell, MomSpellCastType.COMBAT, debugLogger));
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

		assertTrue (SpellUtils.spellCanBeCastIn (spell, MomSpellCastType.OVERLAND, debugLogger));
		assertTrue (SpellUtils.spellCanBeCastIn (spell, MomSpellCastType.COMBAT, debugLogger));
	}

	/**
	 * Tests the getReducedOverlandCastingCost method
	 * @throws MomException If there is a problem
	 * @throws RecordNotFoundException If the test db doesn't include an entry that we reference
	 */
	@Test
	public final void testGetReducedOverlandCastingCost () throws MomException, RecordNotFoundException
	{
		final ICommonDatabase db = GenerateTestData.createDB ();

		// Recommended spell settings - 8% per book multiplicative, 90% cap
		// Using this because all overland spell casting costs end in 0 or 5, so with nice round percentage reductions in multiples of 10% we can't get any results ending in anything other than .0 or .5
		final SpellSettingData spellSettings = GenerateTestData.createRecommendedSpellSettings ();

		// 10 Nature books gives us a 22.1312% reduction
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		PlayerPickUtils.updatePickQuantity (picks, GenerateTestData.NATURE_BOOK, 10, debugLogger);

		// Sprites come out as 77.8688
		final Spell sprites = new Spell ();
		sprites.setOverlandCastingCost (100);
		sprites.setSpellRealm (GenerateTestData.NATURE_BOOK);
		assertEquals ("Sprites does not have expected reduced casting cost", 78, SpellUtils.getReducedOverlandCastingCost (sprites, picks, spellSettings, db, debugLogger));

		// Nature's Eye comes out as 58.4016
		final Spell naturesEye = new Spell ();
		naturesEye.setOverlandCastingCost (75);
		naturesEye.setSpellRealm (GenerateTestData.NATURE_BOOK);
		assertEquals ("Nature's Eye does not have expected reduced casting cost", 59, SpellUtils.getReducedOverlandCastingCost (naturesEye, picks, spellSettings, db, debugLogger));
	}

	/**
	 * Tests the getReducedCombatCastingCost method
	 * @throws MomException If there is a problem
	 * @throws RecordNotFoundException If the test db doesn't include an entry that we reference
	 */
	@Test
	public final void testGetReducedCombatCastingCost () throws MomException, RecordNotFoundException
	{
		final ICommonDatabase db = GenerateTestData.createDB ();

		// Original spell settings - 10% per book additive, 100% cap
		final SpellSettingData spellSettings = GenerateTestData.createOriginalSpellSettings ();

		// 10 Nature books gives us a 30% reduction
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		PlayerPickUtils.updatePickQuantity (picks, GenerateTestData.NATURE_BOOK, 10, debugLogger);

		// Web comes out at an even 7.0
		final Spell web = new Spell ();
		web.setCombatCastingCost (10);
		web.setSpellRealm (GenerateTestData.NATURE_BOOK);
		assertEquals ("Web not have expected reduced casting cost", 7, SpellUtils.getReducedCombatCastingCost (web, picks, spellSettings, db, debugLogger));

		// Earth to Mud comes out at 10.5
		final Spell earthToMud = new Spell ();
		earthToMud.setCombatCastingCost (15);
		earthToMud.setSpellRealm (GenerateTestData.NATURE_BOOK);
		assertEquals ("Earth to Mud does not have expected reduced casting cost", 11, SpellUtils.getReducedCombatCastingCost (earthToMud, picks, spellSettings, db, debugLogger));

		// Giant Strength comes out at 5.6
		final Spell giantStrength = new Spell ();
		giantStrength.setCombatCastingCost (8);
		giantStrength.setSpellRealm (GenerateTestData.NATURE_BOOK);
		assertEquals ("Giant Strength does not have expected reduced casting cost (1)", 6, SpellUtils.getReducedCombatCastingCost (giantStrength, picks, spellSettings, db, debugLogger));

		// With only 20% reduction, Giant Strength comes out at 6.4
		PlayerPickUtils.updatePickQuantity (picks, GenerateTestData.NATURE_BOOK, -1, debugLogger);
		assertEquals ("Giant Strength does not have expected reduced casting cost (2)", 7, SpellUtils.getReducedCombatCastingCost (giantStrength, picks, spellSettings, db, debugLogger));
	}

	/**
	 * Tests the getModifiedSectionID method when we pass flag to say don't modify it
	 * @throws MomException if the status is invalid
	 */
	@Test
	public final void testModifiedGetSectionID_Unmodified () throws MomException
	{
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (CommonDatabaseConstants.SPELL_BOOK_SECTION_SUMMONING);

		// This also proves that passing in null doesn't throw a NullPointerException if we pass in false
		assertEquals (CommonDatabaseConstants.SPELL_BOOK_SECTION_SUMMONING, SpellUtils.getModifiedSectionID (spell, null, false, debugLogger));
	}

	/**
	 * Tests the getModifiedSectionID method when the spell is unavailable
	 * @throws MomException if the status is invalid
	 */
	@Test
	public final void testModifiedGetSectionID_Unavailable () throws MomException
	{
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (CommonDatabaseConstants.SPELL_BOOK_SECTION_SUMMONING);

		// Unmodified section
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.UNAVAILABLE);

		assertEquals (CommonDatabaseConstants.SPELL_BOOK_SECTION_NOT_IN_SPELL_BOOK, SpellUtils.getModifiedSectionID (spell, researchStatus, true, debugLogger));
	}

	/**
	 * Tests the getModifiedSectionID method when the spell isn't in our spell book
	 * @throws MomException if the status is invalid
	 */
	@Test
	public final void testModifiedGetSectionID_NotInSpellBook () throws MomException
	{
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (CommonDatabaseConstants.SPELL_BOOK_SECTION_SUMMONING);

		// Unmodified section
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.NOT_IN_SPELL_BOOK);

		assertEquals (CommonDatabaseConstants.SPELL_BOOK_SECTION_NOT_IN_SPELL_BOOK, SpellUtils.getModifiedSectionID (spell, researchStatus, true, debugLogger));
	}

	/**
	 * Tests the getModifiedSectionID method when the spell is in our spell book but we can't learn it yet
	 * @throws MomException if the status is invalid
	 */
	@Test
	public final void testModifiedGetSectionID_Researchable () throws MomException
	{
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (CommonDatabaseConstants.SPELL_BOOK_SECTION_SUMMONING);

		// Unmodified section
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.RESEARCHABLE);

		assertEquals (CommonDatabaseConstants.SPELL_BOOK_SECTION_UNKNOWN_SPELLS, SpellUtils.getModifiedSectionID (spell, researchStatus, true, debugLogger));
	}

	/**
	 * Tests the getModifiedSectionID method when the spell is one of the 8 we can now learn
	 * @throws MomException if the status is invalid
	 */
	@Test
	public final void testModifiedGetSectionID_ResearchableNow () throws MomException
	{
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (CommonDatabaseConstants.SPELL_BOOK_SECTION_SUMMONING);

		// Unmodified section
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.RESEARCHABLE_NOW);

		assertEquals (CommonDatabaseConstants.SPELL_BOOK_SECTION_RESEARCH_SPELLS, SpellUtils.getModifiedSectionID (spell, researchStatus, true, debugLogger));
	}

	/**
	 * Tests the getModifiedSectionID method when the spell is one that we know
	 * @throws MomException if the status is invalid
	 */
	@Test
	public final void testModifiedGetSectionID_Available () throws MomException
	{
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (CommonDatabaseConstants.SPELL_BOOK_SECTION_SUMMONING);

		// Unmodified section
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);

		assertEquals (CommonDatabaseConstants.SPELL_BOOK_SECTION_SUMMONING, SpellUtils.getModifiedSectionID (spell, researchStatus, true, debugLogger));
	}

	/**
	 * Tests the spellCanTargetMagicRealmLifeformType method
	 * Note this doesn't test the validMagicRealmLifeformTypeIDs output
	 */
	@Test
	public final void testSpellCanTargetMagicRealmLifeformType ()
	{
		// Resist elements can be cast on anything (has no Targets defined)
		final Spell resistElements = new Spell ();

		assertEquals ("Resist elements should be targettable against LTN", true, SpellUtils.spellCanTargetMagicRealmLifeformType (resistElements, CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL, null, debugLogger));
		assertEquals ("Resist elements should be targettable against LTH", true, SpellUtils.spellCanTargetMagicRealmLifeformType (resistElements, CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO, null, debugLogger));
		assertEquals ("Resist elements should be targettable against LT01", true, SpellUtils.spellCanTargetMagicRealmLifeformType (resistElements, LIFE_CREATURE, null, debugLogger));
		assertEquals ("Resist elements should be targettable against LTCC", true, SpellUtils.spellCanTargetMagicRealmLifeformType (resistElements, CHAOS_CHANNELED_CREATURE, null, debugLogger));
		assertEquals ("Resist elements should be targettable against LTU", true, SpellUtils.spellCanTargetMagicRealmLifeformType (resistElements, UNDEAD_CREATURE, null, debugLogger));

		// Confusion can be cast on anything, but has a saving throw modifier (so has a Target record defined)
		final Spell confusion = new Spell ();
		final SpellValidUnitTarget confusionSavingThrowModifier = new SpellValidUnitTarget ();
		confusionSavingThrowModifier.setSavingThrowAttributeID (RESISTANCE);
		confusionSavingThrowModifier.setSavingThrowModifier (-4);
		confusion.getSpellValidUnitTarget ().add (confusionSavingThrowModifier);

		assertEquals ("Confusion should be targettable against LTN", true, SpellUtils.spellCanTargetMagicRealmLifeformType (confusion, CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL, null, debugLogger));
		assertEquals ("Confusion should be targettable against LTH", true, SpellUtils.spellCanTargetMagicRealmLifeformType (confusion, CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO, null, debugLogger));
		assertEquals ("Confusion should be targettable against LT01", true, SpellUtils.spellCanTargetMagicRealmLifeformType (confusion, LIFE_CREATURE, null, debugLogger));
		assertEquals ("Confusion should be targettable against LTCC", true, SpellUtils.spellCanTargetMagicRealmLifeformType (confusion, CHAOS_CHANNELED_CREATURE, null, debugLogger));
		assertEquals ("Confusion should be targettable against LTU", true, SpellUtils.spellCanTargetMagicRealmLifeformType (confusion, UNDEAD_CREATURE, null, debugLogger));

		// Shatter can only be cast on normal units (has a Target record defined, with no saving throw)
		final Spell shatter = new Spell ();
		final SpellValidUnitTarget shatterSavingThrowModifier = new SpellValidUnitTarget ();
		shatterSavingThrowModifier.setTargetMagicRealmID (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		shatterSavingThrowModifier.setSavingThrowAttributeID (RESISTANCE);
		shatter.getSpellValidUnitTarget ().add (shatterSavingThrowModifier);

		assertEquals ("Shatter should be targettable against LTN", true, SpellUtils.spellCanTargetMagicRealmLifeformType (shatter, CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL, null, debugLogger));
		assertEquals ("Shatter shouldn't be targettable against LTH", false, SpellUtils.spellCanTargetMagicRealmLifeformType (shatter, CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO, null, debugLogger));
		assertEquals ("Shatter shouldn't be targettable against LT01", false, SpellUtils.spellCanTargetMagicRealmLifeformType (shatter, LIFE_CREATURE, null, debugLogger));
		assertEquals ("Shatter shouldn't be targettable against LTCC", false, SpellUtils.spellCanTargetMagicRealmLifeformType (shatter, CHAOS_CHANNELED_CREATURE, null, debugLogger));
		assertEquals ("Shatter shouldn't be targettable against LTU", false, SpellUtils.spellCanTargetMagicRealmLifeformType (shatter, UNDEAD_CREATURE, null, debugLogger));

		// Flame Blade can be cast on normal units, heroes and Chaos Channeled units, but has no saving throw
		final Spell flameBlade = new Spell ();
		for (final String magicRealmID : new String [] {CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL,
			CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO, CHAOS_CHANNELED_CREATURE})
		{
			final SpellValidUnitTarget flameBladeTarget = new SpellValidUnitTarget ();
			flameBladeTarget.setTargetMagicRealmID (magicRealmID);
			flameBlade.getSpellValidUnitTarget ().add (flameBladeTarget);
		}

		assertEquals ("Flame blade should be targettable against LTN", true, SpellUtils.spellCanTargetMagicRealmLifeformType (flameBlade, CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL, null, debugLogger));
		assertEquals ("Flame blade should be targettable against LTH", true, SpellUtils.spellCanTargetMagicRealmLifeformType (flameBlade, CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO, null, debugLogger));
		assertEquals ("Flame blade shouldn't be targettable against LT01", false, SpellUtils.spellCanTargetMagicRealmLifeformType (flameBlade, LIFE_CREATURE, null, debugLogger));
		assertEquals ("Flame blade should be targettable against LTCC", true, SpellUtils.spellCanTargetMagicRealmLifeformType (flameBlade, CHAOS_CHANNELED_CREATURE, null, debugLogger));
		assertEquals ("Flame blade shouldn't be targettable against LTU", false, SpellUtils.spellCanTargetMagicRealmLifeformType (flameBlade, UNDEAD_CREATURE, null, debugLogger));
	}

	// Methods dealing with lists of spells

	/**
	 * Tests the getSpellsForRealmRankStatus method
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Test
	public final void testGetSpellsForRealmRankStatus () throws RecordNotFoundException
	{
		final ICommonDatabase db = GenerateTestData.createDB ();

		final List<SpellResearchStatus> statuses = new ArrayList<SpellResearchStatus> ();
		for (final Spell thisSpell : db.getSpell ())
		{
			final SpellResearchStatus thisStatus = new SpellResearchStatus ();
			thisStatus.setSpellID (thisSpell.getSpellID ());
			thisStatus.setStatus (SpellResearchStatusID.UNAVAILABLE);
			statuses.add (thisStatus);
		}

		// Do a count while they're all still unavailable
		assertEquals (2, SpellUtils.getSpellsForRealmRankStatus (statuses, GenerateTestData.CHAOS_BOOK, GenerateTestData.COMMON, SpellResearchStatusID.UNAVAILABLE, db, debugLogger).size ());
		assertEquals (1, SpellUtils.getSpellsForRealmRankStatus (statuses, GenerateTestData.NATURE_BOOK, GenerateTestData.UNCOMMON, SpellResearchStatusID.UNAVAILABLE, db, debugLogger).size ());
		assertEquals (3, SpellUtils.getSpellsForRealmRankStatus (statuses, null, GenerateTestData.COMMON, SpellResearchStatusID.UNAVAILABLE, db, debugLogger).size ());

		// Then change some statuses
		statuses.get (1).setStatus (SpellResearchStatusID.RESEARCHABLE);
		statuses.get (2).setStatus (SpellResearchStatusID.RESEARCHABLE_NOW);
		statuses.get (4).setStatus (SpellResearchStatusID.AVAILABLE);
		statuses.get (6).setStatus (SpellResearchStatusID.AVAILABLE);

		assertEquals (1, SpellUtils.getSpellsForRealmRankStatus (statuses, GenerateTestData.CHAOS_BOOK, GenerateTestData.COMMON, SpellResearchStatusID.UNAVAILABLE, db, debugLogger).size ());
		assertEquals (1, SpellUtils.getSpellsForRealmRankStatus (statuses, GenerateTestData.CHAOS_BOOK, GenerateTestData.COMMON, SpellResearchStatusID.RESEARCHABLE, db, debugLogger).size ());
		assertEquals (0, SpellUtils.getSpellsForRealmRankStatus (statuses, GenerateTestData.NATURE_BOOK, GenerateTestData.UNCOMMON, SpellResearchStatusID.UNAVAILABLE, db, debugLogger).size ());
		assertEquals (1, SpellUtils.getSpellsForRealmRankStatus (statuses, GenerateTestData.NATURE_BOOK, GenerateTestData.UNCOMMON, SpellResearchStatusID.RESEARCHABLE_NOW, db, debugLogger).size ());
		assertEquals (1, SpellUtils.getSpellsForRealmRankStatus (statuses, null, GenerateTestData.COMMON, SpellResearchStatusID.UNAVAILABLE, db, debugLogger).size ());
		assertEquals (2, SpellUtils.getSpellsForRealmRankStatus (statuses, null, GenerateTestData.COMMON, SpellResearchStatusID.AVAILABLE, db, debugLogger).size ());
	}

	/**
	 * Tests the getSpellsForStatus method
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Test
	public final void testGetSpellsForStatus () throws RecordNotFoundException
	{
		final ICommonDatabase db = GenerateTestData.createDB ();

		final List<SpellResearchStatus> statuses = new ArrayList<SpellResearchStatus> ();
		for (final Spell thisSpell : db.getSpell ())
		{
			final SpellResearchStatus thisStatus = new SpellResearchStatus ();
			thisStatus.setSpellID (thisSpell.getSpellID ());
			thisStatus.setStatus (SpellResearchStatusID.UNAVAILABLE);
			statuses.add (thisStatus);
		}

		statuses.get (1).setStatus (SpellResearchStatusID.AVAILABLE);
		statuses.get (3).setStatus (SpellResearchStatusID.AVAILABLE);

		assertEquals (2, SpellUtils.getSpellsForStatus (statuses, SpellResearchStatusID.AVAILABLE, db, debugLogger).size ());
		assertEquals (db.getSpell ().size () - 2, SpellUtils.getSpellsForStatus (statuses, SpellResearchStatusID.UNAVAILABLE, db, debugLogger).size ());
	}

	/**
	 * Tests the getSpellsForRealmAndRank method
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Test
	public final void testGetSpellsForRealmAndRank () throws RecordNotFoundException
	{
		final ICommonDatabase db = GenerateTestData.createDB ();

		final List<SpellResearchStatus> statuses = new ArrayList<SpellResearchStatus> ();
		for (final Spell thisSpell : db.getSpell ())
		{
			final SpellResearchStatus thisStatus = new SpellResearchStatus ();
			thisStatus.setSpellID (thisSpell.getSpellID ());
			statuses.add (thisStatus);
		}

		// Check we can search for a proper magic realm
		assertEquals (2, SpellUtils.getSpellsForRealmAndRank (statuses, GenerateTestData.CHAOS_BOOK, GenerateTestData.COMMON, db, debugLogger).size ());

		// Check we can search for Arcane spells
		assertEquals (3, SpellUtils.getSpellsForRealmAndRank (statuses, null, GenerateTestData.COMMON, db, debugLogger).size ());
	}

	/**
	 * Tests the getSpellsForRankAndStatus method
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Test
	public final void testGetSpellsForRankAndStatus () throws RecordNotFoundException
	{
		final ICommonDatabase db = GenerateTestData.createDB ();

		final List<SpellResearchStatus> statuses = new ArrayList<SpellResearchStatus> ();
		for (final Spell thisSpell : db.getSpell ())
		{
			final SpellResearchStatus thisStatus = new SpellResearchStatus ();
			thisStatus.setSpellID (thisSpell.getSpellID ());
			thisStatus.setStatus (SpellResearchStatusID.UNAVAILABLE);
			statuses.add (thisStatus);
		}

		// Do a count while they're all still unavailable
		assertEquals (6, SpellUtils.getSpellsForRankAndStatus (statuses, GenerateTestData.COMMON, SpellResearchStatusID.UNAVAILABLE, db, debugLogger).size ());
		assertEquals (1, SpellUtils.getSpellsForRankAndStatus (statuses, GenerateTestData.UNCOMMON, SpellResearchStatusID.UNAVAILABLE, db, debugLogger).size ());

		// Set a common and an uncommon to available, so count should only drop by 1
		statuses.get (0).setStatus (SpellResearchStatusID.AVAILABLE);
		statuses.get (2).setStatus (SpellResearchStatusID.AVAILABLE);

		assertEquals (5, SpellUtils.getSpellsForRankAndStatus (statuses, GenerateTestData.COMMON, SpellResearchStatusID.UNAVAILABLE, db, debugLogger).size ());
		assertEquals (0, SpellUtils.getSpellsForRankAndStatus (statuses, GenerateTestData.UNCOMMON, SpellResearchStatusID.UNAVAILABLE, db, debugLogger).size ());
		assertEquals (1, SpellUtils.getSpellsForRankAndStatus (statuses, GenerateTestData.COMMON, SpellResearchStatusID.AVAILABLE, db, debugLogger).size ());
		assertEquals (1, SpellUtils.getSpellsForRankAndStatus (statuses, GenerateTestData.UNCOMMON, SpellResearchStatusID.AVAILABLE, db, debugLogger).size ());
	}

	/**
	 * Tests the getSpellsNotInBookForRealmAndRank method
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Test
	public final void testGetSpellsNotInBookForRealmAndRank () throws RecordNotFoundException
	{
		final ICommonDatabase db = GenerateTestData.createDB ();

		final List<SpellResearchStatus> statuses = new ArrayList<SpellResearchStatus> ();
		for (final Spell thisSpell : db.getSpell ())
		{
			final SpellResearchStatus thisStatus = new SpellResearchStatus ();
			thisStatus.setSpellID (thisSpell.getSpellID ());
			thisStatus.setStatus (SpellResearchStatusID.UNAVAILABLE);
			statuses.add (thisStatus);
		}

		// Do a count while they're all still unavailable
		assertEquals (2, SpellUtils.getSpellsNotInBookForRealmAndRank (statuses, GenerateTestData.CHAOS_BOOK, GenerateTestData.COMMON, db, debugLogger).size ());
		assertEquals (3, SpellUtils.getSpellsNotInBookForRealmAndRank (statuses, null, GenerateTestData.COMMON, db, debugLogger).size ());

		// "not in spell book" shouldn't reduce the count; a different status should
		statuses.get (1).setStatus (SpellResearchStatusID.RESEARCHABLE);
		statuses.get (5).setStatus (SpellResearchStatusID.NOT_IN_SPELL_BOOK);

		assertEquals (1, SpellUtils.getSpellsNotInBookForRealmAndRank (statuses, GenerateTestData.CHAOS_BOOK, GenerateTestData.COMMON, db, debugLogger).size ());
		assertEquals (3, SpellUtils.getSpellsNotInBookForRealmAndRank (statuses, null, GenerateTestData.COMMON, db, debugLogger).size ());
	}

	/**
	 * Tests the getSpellRanksForStatus method
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Test
	public final void testGetSpellRanksForStatus () throws RecordNotFoundException
	{
		final ICommonDatabase db = GenerateTestData.createDB ();

		final List<SpellResearchStatus> statuses = new ArrayList<SpellResearchStatus> ();
		for (final Spell thisSpell : db.getSpell ())
		{
			final SpellResearchStatus thisStatus = new SpellResearchStatus ();
			thisStatus.setSpellID (thisSpell.getSpellID ());
			thisStatus.setStatus (SpellResearchStatusID.UNAVAILABLE);
			statuses.add (thisStatus);
		}

		// Do a count while they're all still unavailable
		assertEquals (2, SpellUtils.getSpellRanksForStatus (statuses, SpellResearchStatusID.UNAVAILABLE, db, debugLogger).size ());

		// Only 1 uncommon spell in the test data, so changing its status should reduce the count
		statuses.get (2).setStatus (SpellResearchStatusID.RESEARCHABLE);
		assertEquals (1, SpellUtils.getSpellRanksForStatus (statuses, SpellResearchStatusID.UNAVAILABLE, db, debugLogger).size ());
	}

	/**
	 * Tests the getSpellRanksForMagicRealm method
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Test
	public final void testGetSpellRanksForMagicRealm () throws RecordNotFoundException
	{
		final ICommonDatabase db = GenerateTestData.createDB ();

		assertEquals (2, SpellUtils.getSpellRanksForMagicRealm (db.getSpell (), GenerateTestData.NATURE_BOOK, debugLogger).size ());
		assertEquals (1, SpellUtils.getSpellRanksForMagicRealm (db.getSpell (), GenerateTestData.CHAOS_BOOK, debugLogger).size ());
		assertEquals (1, SpellUtils.getSpellRanksForMagicRealm (db.getSpell (), null, debugLogger).size ());
	}

	/**
	 * Tests the getSortedSpellsInSection method
	 * @throws MomException If we encounter an unkown research status or castType
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Test
	public final void testGetSortedSpellsInSection () throws MomException, RecordNotFoundException
	{
		final ICommonDatabase db = GenerateTestData.createDB ();

		final List<SpellResearchStatus> statuses = new ArrayList<SpellResearchStatus> ();
		for (final Spell thisSpell : db.getSpell ())
		{
			final SpellResearchStatus thisStatus = new SpellResearchStatus ();
			thisStatus.setSpellID (thisSpell.getSpellID ());
			thisStatus.setStatus (SpellResearchStatusID.UNAVAILABLE);
			statuses.add (thisStatus);
		}

		// Sort on overland casting cost
		// Test data contains 7 spells, but 2 are combat only
		final List<Spell> overlandOrder = SpellUtils.getSortedSpellsInSection (statuses, CommonDatabaseConstants.SPELL_BOOK_SECTION_NOT_IN_SPELL_BOOK, MomSpellCastType.OVERLAND, db, debugLogger);
		assertEquals (5, overlandOrder.size ());
		assertEquals (GenerateTestData.GIANT_SPIDERS_SPELL, overlandOrder.get (0).getSpellID ());	// 3
		assertEquals (GenerateTestData.WARP_WOOD, overlandOrder.get (1).getSpellID ());					// 5
		assertEquals (GenerateTestData.MAGIC_SPIRIT_SPELL, overlandOrder.get (2).getSpellID ());		// 6
		assertEquals (GenerateTestData.HELL_HOUNDS_SPELL, overlandOrder.get (3).getSpellID ());		// 7
		assertEquals (GenerateTestData.DISPEL_MAGIC_SPELL, overlandOrder.get (4).getSpellID ());		// 9

		// Sort on combat casting cost
		// Test data contains 7 spells, but 4 are overland only
		final List<Spell> combatOrder = SpellUtils.getSortedSpellsInSection (statuses, CommonDatabaseConstants.SPELL_BOOK_SECTION_NOT_IN_SPELL_BOOK, MomSpellCastType.COMBAT, db, debugLogger);
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

		assertEquals (0, SpellUtils.getSortedSpellsInSection (statuses, CommonDatabaseConstants.SPELL_BOOK_SECTION_NOT_IN_SPELL_BOOK, MomSpellCastType.OVERLAND, db, debugLogger).size ());
		final List<Spell> researchOrder = SpellUtils.getSortedSpellsInSection (statuses, CommonDatabaseConstants.SPELL_BOOK_SECTION_RESEARCH_SPELLS, MomSpellCastType.OVERLAND, db, debugLogger);

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
