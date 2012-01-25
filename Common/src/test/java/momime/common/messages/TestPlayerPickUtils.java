package momime.common.messages;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import momime.common.database.CommonDatabaseConstants;
import momime.common.database.CommonDatabaseLookup;
import momime.common.database.RecordNotFoundException;
import momime.common.database.GenerateTestData;
import momime.common.database.v0_9_4.Pick;
import momime.common.database.v0_9_4.PickExclusiveFrom;
import momime.common.database.v0_9_4.PickPrerequisite;
import momime.common.messages.v0_9_4.PlayerPick;

import org.junit.Test;

/**
 * Tests the PlayerPickUtils class
 */
public final class TestPlayerPickUtils
{
	/** Dummy logger to use during unit tests */
	private final Logger debugLogger = Logger.getLogger ("MoMIMECommonUnitTests");

	/**
	 * @return Death book with its exclusivities
	 */
	private final Pick createDeathBook ()
	{
		final Pick pick = new Pick ();

		final PickExclusiveFrom ex = new PickExclusiveFrom ();
		ex.setPickExclusiveFromID (GenerateTestData.LIFE_BOOK);
		pick.getPickExclusiveFrom ().add (ex);

		return pick;
	}

	/**
	 * @return Divine power retort with its pre-requisites
	 */
	private final Pick createDivinePowerRetort ()
	{
		final Pick pick = new Pick ();

		final PickPrerequisite req = new PickPrerequisite ();
		req.setPrerequisiteID (GenerateTestData.LIFE_BOOK);
		req.setPrerequisiteCount (4);
		pick.getPickPrerequisite ().add (req);

		return pick;
	}

	/**
	 * @return Sorcery mastery retort with its pre-requisites
	 */
	private final Pick createSorceryMasteryRetort ()
	{
		final Pick pick = new Pick ();

		final PickPrerequisite req = new PickPrerequisite ();
		req.setPrerequisiteID (GenerateTestData.SORCERY_BOOK);
		req.setPrerequisiteCount (4);
		pick.getPickPrerequisite ().add (req);

		return pick;
	}

	/**
	 * @return Runemaster retort with its pre-requisites
	 */
	private final Pick createRunemasterRetort ()
	{
		final Pick pick = new Pick ();

		for (int n = 1; n <= 3; n++)
		{
			final PickPrerequisite req = new PickPrerequisite ();
			req.setPrerequisiteTypeID (GenerateTestData.BOOK);
			req.setPrerequisiteCount (2);
			pick.getPickPrerequisite ().add (req);
		}

		return pick;
	}

	/**
	 * @return Node mastery retort with its pre-requisites
	 */
	private final Pick createNodeMasteryRetort ()
	{
		final Pick pick = new Pick ();

		for (int n = 3; n <= 5; n++)
		{
			final PickPrerequisite req = new PickPrerequisite ();
			req.setPrerequisiteID ("MB0" + n);
			req.setPrerequisiteCount (1);
			pick.getPickPrerequisite ().add (req);
		}

		return pick;
	}

	/**
	 * @return Ariel's standard picks for 20 picks
	 */
	private final List<PlayerPick> createAriel20PicksList ()
	{
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();

		final PlayerPick lifeBooks = new PlayerPick ();
		lifeBooks.setPickID (GenerateTestData.LIFE_BOOK);
		lifeBooks.setQuantity (14);
		picks.add (lifeBooks);

		for (int n = 9; n <= 12; n++)
		{
			// This loop is a fudge to get retorts 5, 9, 10 and 12 added, without having to write the code out 4 times
			final int retortNumber;
			if (n == 11)
				retortNumber = 5;
			else
				retortNumber = n;

			final PlayerPick retort = new PlayerPick ();

			if (retortNumber < 10)
				retort.setPickID ("RT0" + retortNumber);
			else
				retort.setPickID ("RT" + retortNumber);

			retort.setQuantity (1);
			picks.add (retort);
		}

		return picks;
	}

	/**
	 * Tests the getTotalPickCost method
	 * @throws RecordNotFoundException If there is a problem
	 */
	@Test
	public final void testGetTotalPickCost () throws RecordNotFoundException
	{
		assertEquals ("Ariel's standard 20 picks did not total 20", 20, PlayerPickUtils.getTotalPickCost (createAriel20PicksList (), GenerateTestData.createDB (), debugLogger));
	}

	/**
	 * Tests the getQuantityOfPick method
	 */
	@Test
	public final void testGetQuantityOfPick ()
	{
		assertEquals ("Ariel's standard 20 picks did include 14 life books", 14, PlayerPickUtils.getQuantityOfPick (createAriel20PicksList (), "MB01", debugLogger));
	}

	/**
	 * Tests the updatePickQuantity method
	 */
	@Test
	public final void testUpdatePickQuantity ()
	{
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();

		// Add one
		PlayerPickUtils.updatePickQuantity (picks, GenerateTestData.LIFE_BOOK, 3, debugLogger);
		assertEquals (1, picks.size ());
		assertEquals (GenerateTestData.LIFE_BOOK, picks.get (0).getPickID ());
		assertEquals (3, picks.get (0).getQuantity ());

		// Add another
		PlayerPickUtils.updatePickQuantity (picks, GenerateTestData.CHAOS_BOOK, 2, debugLogger);
		assertEquals (2, picks.size ());
		assertEquals (GenerateTestData.LIFE_BOOK, picks.get (0).getPickID ());
		assertEquals (3, picks.get (0).getQuantity ());
		assertEquals (GenerateTestData.CHAOS_BOOK, picks.get (1).getPickID ());
		assertEquals (2, picks.get (1).getQuantity ());

		// Increase quantity of existing pick
		PlayerPickUtils.updatePickQuantity (picks, GenerateTestData.LIFE_BOOK, 2, debugLogger);
		assertEquals (2, picks.size ());
		assertEquals (GenerateTestData.LIFE_BOOK, picks.get (0).getPickID ());
		assertEquals (5, picks.get (0).getQuantity ());
		assertEquals (GenerateTestData.CHAOS_BOOK, picks.get (1).getPickID ());
		assertEquals (2, picks.get (1).getQuantity ());

		// Reduce quantity of existing pick to 0 so it gets removed
		PlayerPickUtils.updatePickQuantity (picks, GenerateTestData.LIFE_BOOK, -5, debugLogger);
		assertEquals (1, picks.size ());
		assertEquals (GenerateTestData.CHAOS_BOOK, picks.get (0).getPickID ());
		assertEquals (2, picks.get (0).getQuantity ());
	}

	/**
	 * Tests the countPicksOfType method
	 * @throws RecordNotFoundException If there is a problem
	 */
	@Test
	public final void testCountPicksOfType () throws RecordNotFoundException
	{
		assertEquals ("Ariel's standard 20 picks did include 14 books", 14, PlayerPickUtils.countPicksOfType (createAriel20PicksList (), "B", false, GenerateTestData.createDB (), debugLogger));
	}

	/**
	 * Tests the meetsPickRequirements method
	 * @throws RecordNotFoundException If there is a problem
	 */
	@Test
	public final void testMeetsPickRequirements () throws RecordNotFoundException
	{
		final CommonDatabaseLookup db = GenerateTestData.createDB ();
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();

		// Divine power needs 4 life books; Archmage needs 4 of any book; so both should return false with 3 life books
		PlayerPickUtils.updatePickQuantity (picks, GenerateTestData.LIFE_BOOK, 3, debugLogger);
		assertEquals ("Archmage was allowed with only 3 books", false, PlayerPickUtils.meetsPickRequirements (GenerateTestData.createArchmageRetort (), picks, db, debugLogger));
		assertEquals ("Divine Power was allowed with only 3 life books", false, PlayerPickUtils.meetsPickRequirements (createDivinePowerRetort (), picks, db, debugLogger));

		// Archmage needs all 4 books of the same type - adding a sorcery book doesn't help
		PlayerPickUtils.updatePickQuantity (picks, GenerateTestData.SORCERY_BOOK, 1, debugLogger);
		assertEquals ("Archmage was allowed with all 4 books not being the same type", false, PlayerPickUtils.meetsPickRequirements (GenerateTestData.createArchmageRetort (), picks, db, debugLogger));

		// Both should be OK with 4 life books
		PlayerPickUtils.updatePickQuantity (picks, GenerateTestData.LIFE_BOOK, 1, debugLogger);
		assertEquals ("Archmage was not allowed with only 4 life books", true, PlayerPickUtils.meetsPickRequirements (GenerateTestData.createArchmageRetort (), picks, db, debugLogger));
		assertEquals ("Divine Power was not allowed with only 4 life books", true, PlayerPickUtils.meetsPickRequirements (createDivinePowerRetort (), picks, db, debugLogger));

		// Sorcery mastery needs 4 sorcery books
		assertEquals ("Sorcery Mastery was allowed with only 1 sorcery book", false, PlayerPickUtils.meetsPickRequirements (createSorceryMasteryRetort (), picks, db, debugLogger));
		PlayerPickUtils.updatePickQuantity (picks, GenerateTestData.SORCERY_BOOK, 3, debugLogger);
		assertEquals ("Sorcery Mastery was not allowed with 4 sorcery books", true, PlayerPickUtils.meetsPickRequirements (createSorceryMasteryRetort (), picks, db, debugLogger));

		// Runemaster requires 2 of 3 different types of spell book
		assertEquals ("Runemaster was allowed with only 2 types of book", false, PlayerPickUtils.meetsPickRequirements (createRunemasterRetort (), picks, db, debugLogger));
		PlayerPickUtils.updatePickQuantity (picks, GenerateTestData.NATURE_BOOK, 1, debugLogger);
		assertEquals ("Runemaster was allowed with only having 1 book in the 3rd type", false, PlayerPickUtils.meetsPickRequirements (createRunemasterRetort (), picks, db, debugLogger));
		PlayerPickUtils.updatePickQuantity (picks, GenerateTestData.NATURE_BOOK, 1, debugLogger);
		assertEquals ("Runemaster was not allowed with 2 of 3 different types of spell book", true, PlayerPickUtils.meetsPickRequirements (createRunemasterRetort (), picks, db, debugLogger));

		// Node mastery requires 3 specific books
		assertEquals ("Node Mastery was allowed without a Chaos book", false, PlayerPickUtils.meetsPickRequirements (createNodeMasteryRetort (), picks, db, debugLogger));
		PlayerPickUtils.updatePickQuantity (picks, GenerateTestData.CHAOS_BOOK, 1, debugLogger);
		assertEquals ("Node Mastery was not allowed with all 3 book types", true, PlayerPickUtils.meetsPickRequirements (createNodeMasteryRetort (), picks, db, debugLogger));
	}

	/**
	 * Tests the canSafelyRemove method
	 * @throws RecordNotFoundException If there is a problem
	 */
	@Test
	public final void testCanSafelyRemove () throws RecordNotFoundException
	{
		final CommonDatabaseLookup db = GenerateTestData.createDB ();
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();

		// Archmage needs 4 of any book so should return false if we try to remove a life book
		PlayerPickUtils.updatePickQuantity (picks, GenerateTestData.ARCHMAGE, 1, debugLogger);
		PlayerPickUtils.updatePickQuantity (picks, GenerateTestData.LIFE_BOOK, 4, debugLogger);
		assertEquals ("Allowed to remove a book even though we have Archmage", false, PlayerPickUtils.canSafelyRemove (GenerateTestData.LIFE_BOOK, picks, db, debugLogger));
		assertEquals ("Allowed to remove a book that we don't have", false, PlayerPickUtils.canSafelyRemove (GenerateTestData.SORCERY_BOOK, picks, db, debugLogger));

		// If we add another book first, then should be able to remove it
		PlayerPickUtils.updatePickQuantity (picks, GenerateTestData.LIFE_BOOK, 1, debugLogger);
		assertEquals ("Not allowed to remove a book even though we have 5 of them", true, PlayerPickUtils.canSafelyRemove (GenerateTestData.LIFE_BOOK, picks, db, debugLogger));
	}

	/**
	 * Tests the canSafelyAdd method
	 */
	@Test
	public final void testCanSafelyAdd ()
	{
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();

		// Can't add a death book if we have a life book
		PlayerPickUtils.updatePickQuantity (picks, GenerateTestData.ARTIFICER, 1, debugLogger);
		PlayerPickUtils.updatePickQuantity (picks, GenerateTestData.CHAOS_BOOK, 1, debugLogger);
		assertEquals ("Not allowed to add a Death book even though we have no life books", true, PlayerPickUtils.canSafelyAdd (createDeathBook (), picks, debugLogger));

		PlayerPickUtils.updatePickQuantity (picks, GenerateTestData.LIFE_BOOK, 1, debugLogger);
		assertEquals ("Allowed to add a Death book even though we have a life book", false, PlayerPickUtils.canSafelyAdd (createDeathBook (), picks, debugLogger));
	}

	/**
	 * Tests the getHighestWeaponGradeGrantedByPicks method
	 * @throws RecordNotFoundException If there is a problem
	 */
	@Test
	public final void testGetHighestWeaponGradeGrantedByPicks () throws RecordNotFoundException
	{
		final CommonDatabaseLookup db = GenerateTestData.createDB ();

		// Ariel at 20 picks - she has a pile of retorts, but Alchemy isn't one of them
		final List<PlayerPick> picks = createAriel20PicksList ();
		assertEquals ("Ariel has magic weapons", 0, PlayerPickUtils.getHighestWeaponGradeGrantedByPicks (picks, db, debugLogger));

		PlayerPickUtils.updatePickQuantity (picks, CommonDatabaseConstants.VALUE_RETORT_ID_ALCHEMY, 1, debugLogger);
		assertEquals ("Ariel still has no magic weapons even with Alchemy retort", 1, PlayerPickUtils.getHighestWeaponGradeGrantedByPicks (picks, db, debugLogger));
	}

	/**
	 * Tests the totalReligiousBuildingBonus & pickIdsContributingToReligiousBuildingBonus methods
	 * @throws RecordNotFoundException If there is a problem
	 */
	@Test
	public final void testTotalReligiousBuildingBonus () throws RecordNotFoundException
	{
		final CommonDatabaseLookup db = GenerateTestData.createDB ();
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();

		PlayerPickUtils.updatePickQuantity (picks, GenerateTestData.LIFE_BOOK, 5, debugLogger);
		assertEquals ("Life books shouldn't give religious buildings bonus", 0, PlayerPickUtils.totalReligiousBuildingBonus (picks, db, debugLogger));

		PlayerPickUtils.updatePickQuantity (picks, GenerateTestData.DIVINE_POWER, 1, debugLogger);
		assertEquals ("Divine power should give religious buildings bonus", 50, PlayerPickUtils.totalReligiousBuildingBonus (picks, db, debugLogger));

		final List<String> pickIDs = PlayerPickUtils.pickIdsContributingToReligiousBuildingBonus (picks, db, debugLogger);
		assertEquals ("Array Pick IDs for religious buildings bonus not correct length", 1, pickIDs.size ());
		assertEquals ("Array Pick IDs for religious buildings bonus not correct contents", GenerateTestData.DIVINE_POWER, pickIDs.get (0));
	}

	/**
	 * Tests the totalProductionBonus method
	 * @throws RecordNotFoundException If there is a problem
	 */
	@Test
	public final void testTotalProductionBonus () throws RecordNotFoundException
	{
		final CommonDatabaseLookup db = GenerateTestData.createDB ();
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();

		// Archmage gives +50% to magic power spent on improving skill; whatever unit type we pass in is irrelevant
		PlayerPickUtils.updatePickQuantity (picks, GenerateTestData.ARCHMAGE, 1, debugLogger);
		assertEquals ("Archmage didn't give +50% to magic power spent on improving skill", 50, PlayerPickUtils.totalProductionBonus (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT, null, picks, db, debugLogger));
		assertEquals ("Archmage didn't give +50% to magic power spent on improving skill with a unit type supplied", 50, PlayerPickUtils.totalProductionBonus (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT, CommonDatabaseConstants.VALUE_UNIT_TYPE_ID_SUMMONED, picks, db, debugLogger));

		// Summoner gives +25% to unit upkeep reduction but only on unit type = Summoned
		PlayerPickUtils.updatePickQuantity (picks, GenerateTestData.SUMMONER, 1, debugLogger);
		assertEquals ("Summoner didn't give +25% to unit upkeep reduction on summoning spells", 25, PlayerPickUtils.totalProductionBonus (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_UNIT_UPKEEP_REDUCTION, CommonDatabaseConstants.VALUE_UNIT_TYPE_ID_SUMMONED, picks, db, debugLogger));
		assertEquals ("Summoner still gave +25% to unit upkeep reduction on spells for the wrong unit type", 0, PlayerPickUtils.totalProductionBonus (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_UNIT_UPKEEP_REDUCTION, "N", picks, db, debugLogger));
		assertEquals ("Summoner still gave +25% to unit upkeep reduction with a null unit type", 0, PlayerPickUtils.totalProductionBonus (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_UNIT_UPKEEP_REDUCTION, null, picks, db, debugLogger));

		// NB. Most of the other bonuses are to spell research and/or casting cost reduction, which can't be tested with this method since they
		// take into account the session description settings for whether to add or multiply the bonuses together
	}
}
