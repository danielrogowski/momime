package momime.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import momime.common.MomException;
import momime.common.calculations.HeroItemCalculations;
import momime.common.calculations.SpellCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItem;
import momime.common.database.Pick;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.SpellSetting;
import momime.common.database.UnitEx;
import momime.common.database.ValidUnitTarget;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.PlayerPick;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;

/**
 * Tests the SpellUtils class
 */
@ExtendWith(MockitoExtension.class)
public final class TestSpellUtilsImpl
{
	/** Life creature */
	private final static String LIFE_CREATURE = "LT01";

	/** Chaos channeled creature */
	private final static String CHAOS_CHANNELED_CREATURE = "LTCC";

	/** Undead creature */
	private final static String UNDEAD_CREATURE = "LTU";

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
	@Test
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
		assertThrows (RecordNotFoundException.class, () ->
		{
			utils.findSpellResearchStatus (statuses, "SP004");
		});
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

		final Pick magicRealm = new Pick ();
		magicRealm.setUnitTypeID ("X");
		when (db.findPick ("MB01", "spellSummonsUnitTypeID")).thenReturn (magicRealm);
		
		final Spell nonSummoningSpell = new Spell ();
		nonSummoningSpell.setSpellBookSectionID (SpellBookSectionID.OVERLAND_ENCHANTMENTS);

		final Spell summoningSpell = new Spell ();
		summoningSpell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);

		for (int n = 1; n <= 3; n++)
		{
			final String summonedUnitID = "UN00" + n;
			summoningSpell.getSummonedUnit ().add (summonedUnitID);
			
			final UnitEx unitDef = new UnitEx ();
			unitDef.setUnitMagicRealm ("MB01");
			when (db.findUnit (summonedUnitID, "spellSummonsUnitTypeID")).thenReturn (unitDef);
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
	@Test
	public final void testSpellSummonsUnitTypeID_Invalid () throws MomException, RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final Pick magicRealmOne = new Pick ();
		magicRealmOne.setUnitTypeID ("X");
		when (db.findPick ("MB01", "spellSummonsUnitTypeID")).thenReturn (magicRealmOne);
		
		final Pick magicRealmTwo = new Pick ();
		magicRealmTwo.setUnitTypeID ("Y");
		when (db.findPick ("MB02", "spellSummonsUnitTypeID")).thenReturn (magicRealmTwo);
		
		final Spell summoningSpell = new Spell ();
		summoningSpell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);

		for (int n = 1; n <= 2; n++)
		{
			final String summonedUnitID = "UN00" + n;
			summoningSpell.getSummonedUnit ().add (summonedUnitID);
			
			final UnitEx unitDef = new UnitEx ();
			unitDef.setUnitMagicRealm ("MB0" + n);
			when (db.findUnit (summonedUnitID, "spellSummonsUnitTypeID")).thenReturn (unitDef);
		}
		
		// Set up object to test
		final SpellUtilsImpl utils = new SpellUtilsImpl ();

		// Run method
		assertThrows (MomException.class, () ->
		{
			utils.spellSummonsUnitTypeID (summoningSpell, db);
		});
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
	 * Tests the spellCanBeCastIn method on a the create artifact spell
	 * @throws MomException If there is a problem
	 */
	@Test
	public final void testSpellCanBeCastIn_CreateArtifact () throws MomException
	{
		final Spell spell = new Spell ();
		spell.setHeroItemBonusMaximumCraftingCost (0);

		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		assertTrue (utils.spellCanBeCastIn (spell, SpellCastType.OVERLAND));
		assertFalse (utils.spellCanBeCastIn (spell, SpellCastType.COMBAT));
	}
	
	/**
	 * Tests the getUnmodifiedCombatCastingCost method on a normal spell without variable damage
	 * @throws MomException If there is a problem
	 */
	@Test
	public final void testGetUnmodifiedCombatCastingCost_Normal () throws MomException
	{
		final Spell spell = new Spell ();
		spell.setCombatCastingCost (30);
		
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		assertEquals (30, utils.getUnmodifiedCombatCastingCost (spell, null, null)); 
	}
	
	/**
	 * Tests the getUnmodifiedCombatCastingCost where each additional dmg point costs multiple MP
	 * @throws MomException If there is a problem
	 */
	@Test
	public final void testGetUnmodifiedCombatCastingCost_ManaPerDamage () throws MomException
	{
		final Spell spell = new Spell ();
		spell.setCombatCastingCost (30);
		spell.setCombatBaseDamage (20);
		spell.setCombatMaxDamage (110);
		spell.setCombatManaPerAdditionalDamagePoint (3);
		
		// So we want to do 80 damage, which is base + 60
		// Each of those +60 costs 3 MP so total of +180
		// plus the base casting cost of 30 = 210
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		assertEquals (210, utils.getUnmodifiedCombatCastingCost (spell, 80, null)); 
	}
	
	/**
	 * Tests the getUnmodifiedCombatCastingCost where each additional MP grant multiple dmg
	 * @throws MomException If there is a problem
	 */
	@Test
	public final void testGetUnmodifiedCombatCastingCost_DamagePerMana () throws MomException
	{
		final Spell spell = new Spell ();
		spell.setCombatCastingCost (30);
		spell.setCombatBaseDamage (20);
		spell.setCombatMaxDamage (110);
		spell.setCombatAdditionalDamagePointsPerMana (3);
		
		// So we want to do 80 damage, which is base + 60
		// We get +3 for each additional MP so +60 dmg costs +20 MP
		// plus the base casting cost of 30 = 50
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		assertEquals (50, utils.getUnmodifiedCombatCastingCost (spell, 80, null)); 
	}
	
	/**
	 * Tests the getUnmodifiedCombatCastingCost where the spell doesn't define whether
	 * its "MP per dmg" or "dmg per MP"
	 * @throws MomException If there is a problem
	 */
	@Test
	public final void testGetUnmodifiedCombatCastingCost_Error () throws MomException
	{
		final Spell spell = new Spell ();
		spell.setCombatCastingCost (30);
		spell.setCombatBaseDamage (20);
		spell.setCombatMaxDamage (110);
		
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		
		assertThrows (MomException.class, () ->
		{
			utils.getUnmodifiedCombatCastingCost (spell, 80, null);
		});
	}
	
	/**
	 * Tests the getUnmodifiedOverlandCastingCost method on a normal spell without variable damage
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetUnmodifiedOverlandCastingCost_Normal () throws Exception
	{
		final Spell spell = new Spell ();
		spell.setOverlandCastingCost (30);
		
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		assertEquals (30, utils.getUnmodifiedOverlandCastingCost (spell, null, null, null, null)); 
	}
	
	/**
	 * Tests the getUnmodifiedOverlandCastingCost method on a create artifact spell
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetUnmodifiedOverlandCastingCost_CreateArtifact () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Spell and item being crafted
		final Spell spell = new Spell ();
		
		final HeroItem item = new HeroItem ();
		
		// Mock the cost
		final HeroItemCalculations calc = mock (HeroItemCalculations.class);
		when (calc.calculateCraftingCost (item, db)).thenReturn (200);
		
		// Call method
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		utils.setHeroItemCalculations (calc);
		assertEquals (200, utils.getUnmodifiedOverlandCastingCost (spell, item, null, null, db)); 
	}
	
	/**
	 * Tests the getUnmodifiedOverlandCastingCost where each additional dmg point costs multiple MP
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetUnmodifiedOverlandCastingCost_ManaPerDamage () throws Exception
	{
		final Spell spell = new Spell ();
		spell.setOverlandCastingCost (30);
		spell.setOverlandBaseDamage (20);
		spell.setOverlandMaxDamage (110);
		spell.setOverlandManaPerAdditionalDamagePoint (3);
		
		// So we want to do 80 damage, which is base + 60
		// Each of those +60 costs 3 MP so total of +180
		// plus the base casting cost of 30 = 210
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		assertEquals (210, utils.getUnmodifiedOverlandCastingCost (spell, null, 80, null, null)); 
	}
	
	/**
	 * Tests the getUnmodifiedOverlandCastingCost where each additional MP grant multiple dmg
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetUnmodifiedOverlandCastingCost_DamagePerMana () throws Exception
	{
		final Spell spell = new Spell ();
		spell.setOverlandCastingCost (30);
		spell.setOverlandBaseDamage (20);
		spell.setOverlandMaxDamage (110);
		spell.setOverlandAdditionalDamagePointsPerMana (3);
		
		// So we want to do 80 damage, which is base + 60
		// We get +3 for each additional MP so +60 dmg costs +20 MP
		// plus the base casting cost of 30 = 50
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		assertEquals (50, utils.getUnmodifiedOverlandCastingCost (spell, null, 80, null, null)); 
	}
	
	/**
	 * Tests the getUnmodifiedOverlandCastingCost where the spell doesn't define whether
	 * its "MP per dmg" or "dmg per MP"
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetUnmodifiedOverlandCastingCost_Error () throws Exception
	{
		final Spell spell = new Spell ();
		spell.setOverlandCastingCost (30);
		spell.setOverlandBaseDamage (20);
		spell.setOverlandMaxDamage (110);
		
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		
		assertThrows (MomException.class, () ->
		{
			utils.getUnmodifiedOverlandCastingCost (spell, null, 80, null, null);
		});
	}
	
	/**
	 * Tests the getReducedCastingCost method.  Just test this directly with mocks.
	 * getReducedCombatCastingCost and getReducedOverlandCastingCost are then just simple
	 * combinations of other methods that have unit tests, so don't need their own tests.
	 * 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetReducedCastingCost () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Spell to test
		final Spell spell = new Spell ();
		spell.setSpellRealm ("MB01");

		// Number of picks we have in the magic realm of the spell
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		when (playerPickUtils.getQuantityOfPick (picks, "MB01")).thenReturn (8);
		
		// Casting cost reduction
		final SpellSetting spellSettings = new SpellSetting ();
		
		final SpellCalculations spellCalculations = mock (SpellCalculations.class);
		when (spellCalculations.calculateCastingCostReduction (8, spellSettings, spell, picks, db)).thenReturn (15.5);
		
		// Casting cost increase
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		// Set up object to test
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		utils.setPlayerPickUtils (playerPickUtils);
		utils.setSpellCalculations (spellCalculations);
		
		// 15.5% of 2000 is 310
		assertEquals (2000 - 310, utils.getReducedCastingCost (spell, 2000, picks, spells, spellSettings, db));
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

		// Set up object to test
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		
		// Run method
		assertNull (utils.getModifiedSectionID (spell, SpellResearchStatusID.UNAVAILABLE, true));
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

		// Set up object to test
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		
		// Run method
		assertNull (utils.getModifiedSectionID (spell, SpellResearchStatusID.NOT_IN_SPELL_BOOK, true));
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

		// Set up object to test
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		
		// Run method
		assertEquals (SpellBookSectionID.RESEARCHABLE, utils.getModifiedSectionID (spell, SpellResearchStatusID.RESEARCHABLE, true));
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

		// Set up object to test
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		
		// Run method
		assertEquals (SpellBookSectionID.RESEARCHABLE_NOW, utils.getModifiedSectionID (spell, SpellResearchStatusID.RESEARCHABLE_NOW, true));
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

		// Set up object to test
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		
		// Run method
		assertEquals (SpellBookSectionID.SUMMONING, utils.getModifiedSectionID (spell, SpellResearchStatusID.AVAILABLE, true));
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

		assertEquals (true, utils.spellCanTargetMagicRealmLifeformType (resistElements, CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL),
			"Resist elements should be targettable against LTN");
		assertEquals (true, utils.spellCanTargetMagicRealmLifeformType (resistElements, CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO),
			"Resist elements should be targettable against LTH");
		assertEquals (true, utils.spellCanTargetMagicRealmLifeformType (resistElements, LIFE_CREATURE),
			"Resist elements should be targettable against LT01");
		assertEquals (true, utils.spellCanTargetMagicRealmLifeformType (resistElements, CHAOS_CHANNELED_CREATURE),
			"Resist elements should be targettable against LTCC");
		assertEquals (true, utils.spellCanTargetMagicRealmLifeformType (resistElements, UNDEAD_CREATURE),
			"Resist elements should be targettable against LTU");

		// Shatter can only be cast on normal units (has a Target record defined, with no saving throw)
		final Spell shatter = new Spell ();
		final ValidUnitTarget shatterSavingThrowModifier = new ValidUnitTarget ();
		shatterSavingThrowModifier.setTargetMagicRealmID (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		shatter.getSpellValidUnitTarget ().add (shatterSavingThrowModifier);

		assertEquals (true, utils.spellCanTargetMagicRealmLifeformType (shatter, CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL),
			"Shatter should be targettable against LTN");
		assertEquals (false, utils.spellCanTargetMagicRealmLifeformType (shatter, CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO),
			"Shatter shouldn't be targettable against LTH");
		assertEquals (false, utils.spellCanTargetMagicRealmLifeformType (shatter, LIFE_CREATURE),
			"Shatter shouldn't be targettable against LT01");
		assertEquals (false, utils.spellCanTargetMagicRealmLifeformType (shatter, CHAOS_CHANNELED_CREATURE),
			"Shatter shouldn't be targettable against LTCC");
		assertEquals (false, utils.spellCanTargetMagicRealmLifeformType (shatter, UNDEAD_CREATURE),
			"Shatter shouldn't be targettable against LTU");

		// Flame Blade can be cast on normal units, heroes and Chaos Channeled units, but has no saving throw
		final Spell flameBlade = new Spell ();
		for (final String magicRealmID : new String [] {CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL,
			CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO, CHAOS_CHANNELED_CREATURE})
		{
			final ValidUnitTarget flameBladeTarget = new ValidUnitTarget ();
			flameBladeTarget.setTargetMagicRealmID (magicRealmID);
			flameBlade.getSpellValidUnitTarget ().add (flameBladeTarget);
		}

		assertEquals (true, utils.spellCanTargetMagicRealmLifeformType (flameBlade, CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL),
			"Flame blade should be targettable against LTN");
		assertEquals (true, utils.spellCanTargetMagicRealmLifeformType (flameBlade, CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO),
			"Flame blade should be targettable against LTH");
		assertEquals (false, utils.spellCanTargetMagicRealmLifeformType (flameBlade, LIFE_CREATURE),
			"Flame blade shouldn't be targettable against LT01");
		assertEquals (true, utils.spellCanTargetMagicRealmLifeformType (flameBlade, CHAOS_CHANNELED_CREATURE),
			"Flame blade should be targettable against LTCC");
		assertEquals (false, utils.spellCanTargetMagicRealmLifeformType (flameBlade, UNDEAD_CREATURE),
			"Flame blade shouldn't be targettable against LTU");
	}
	
	/**
	 * Tests the findMagicRealmLifeformTypeTarget method when a record does exist
	 */
	@Test
	public final void testFindMagicRealmLifeformTypeTarget_Exists ()
	{
		// Set up spell
		final Spell spell = new Spell ();
		for (final String magicRealmID : new String [] {"A", "B", "C"})
		{
			final ValidUnitTarget target = new ValidUnitTarget ();
			target.setTargetMagicRealmID (magicRealmID);
			spell.getSpellValidUnitTarget ().add (target);
		}
		
		// Set up object to test
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		
		// Call method
		assertEquals ("B", utils.findMagicRealmLifeformTypeTarget (spell, "B").getTargetMagicRealmID ());
	}

	/**
	 * Tests the findMagicRealmLifeformTypeTarget method when a record does not exist
	 */
	@Test
	public final void testFindMagicRealmLifeformTypeTarget_NotExists ()
	{
		// Set up spell
		final Spell spell = new Spell ();
		for (final String magicRealmID : new String [] {"A", "B", "C"})
		{
			final ValidUnitTarget target = new ValidUnitTarget ();
			target.setTargetMagicRealmID (magicRealmID);
			spell.getSpellValidUnitTarget ().add (target);
		}
		
		// Set up object to test
		final SpellUtilsImpl utils = new SpellUtilsImpl ();
		
		// Call method
		assertNull (utils.findMagicRealmLifeformTypeTarget (spell, "D"));
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