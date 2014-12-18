package momime.common.utils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.anyString;

import java.util.ArrayList;
import java.util.List;

import momime.common.database.CommonDatabaseConstants;
import momime.common.database.GenerateTestData;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Pick;
import momime.common.database.PickExclusiveFrom;
import momime.common.database.PickPrerequisite;
import momime.common.messages.PlayerPick;

import org.junit.Test;

/**
 * Tests the PlayerPickUtils class
 */
public final class TestPlayerPickUtilsImpl
{
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
		final PlayerPickUtilsImpl utils = new PlayerPickUtilsImpl (); 
		assertEquals ("Ariel's standard 20 picks did not total 20", 20, utils.getTotalPickCost (createAriel20PicksList (), GenerateTestData.createDB ()));
	}

	/**
	 * Tests the getQuantityOfPick method
	 */
	@Test
	public final void testGetQuantityOfPick ()
	{
		final PlayerPickUtilsImpl utils = new PlayerPickUtilsImpl (); 
		assertEquals ("Ariel's standard 20 picks did include 14 life books", 14, utils.getQuantityOfPick (createAriel20PicksList (), "MB01"));
	}

	/**
	 * Tests the updatePickQuantity method
	 */
	@Test
	public final void testUpdatePickQuantity ()
	{
		final PlayerPickUtilsImpl utils = new PlayerPickUtilsImpl (); 
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();

		// Add one
		utils.updatePickQuantity (picks, GenerateTestData.LIFE_BOOK, 3);
		assertEquals (1, picks.size ());
		assertEquals (GenerateTestData.LIFE_BOOK, picks.get (0).getPickID ());
		assertEquals (3, picks.get (0).getQuantity ());

		// Add another
		utils.updatePickQuantity (picks, GenerateTestData.CHAOS_BOOK, 2);
		assertEquals (2, picks.size ());
		assertEquals (GenerateTestData.LIFE_BOOK, picks.get (0).getPickID ());
		assertEquals (3, picks.get (0).getQuantity ());
		assertEquals (GenerateTestData.CHAOS_BOOK, picks.get (1).getPickID ());
		assertEquals (2, picks.get (1).getQuantity ());

		// Increase quantity of existing pick
		utils.updatePickQuantity (picks, GenerateTestData.LIFE_BOOK, 2);
		assertEquals (2, picks.size ());
		assertEquals (GenerateTestData.LIFE_BOOK, picks.get (0).getPickID ());
		assertEquals (5, picks.get (0).getQuantity ());
		assertEquals (GenerateTestData.CHAOS_BOOK, picks.get (1).getPickID ());
		assertEquals (2, picks.get (1).getQuantity ());

		// Reduce quantity of existing pick to 0 so it gets removed
		utils.updatePickQuantity (picks, GenerateTestData.LIFE_BOOK, -5);
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
		final PlayerPickUtilsImpl utils = new PlayerPickUtilsImpl (); 
		assertEquals ("Ariel's standard 20 picks did include 14 books", 14, utils.countPicksOfType (createAriel20PicksList (), "B", false, GenerateTestData.createDB ()));
	}

	/**
	 * Tests the meetsPickRequirements method
	 * @throws RecordNotFoundException If there is a problem
	 */
	@Test
	public final void testMeetsPickRequirements () throws RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		for (int n = 1 ; n <= 4; n++)
		{
			final Pick Book = new Pick ();
			Book.setPickType ("B");
			when (db.findPick (eq ("MB0" + n), anyString ())).thenReturn (Book);
		}
		
		final PickPrerequisite archmagePrereq = new PickPrerequisite ();
		archmagePrereq.setPrerequisiteCount (4);
		archmagePrereq.setPrerequisiteTypeID ("B");
		
		final Pick archmage = new Pick ();
		archmage.getPickPrerequisite ().add (archmagePrereq);
		when (db.findPick (eq ("RT01"), anyString ())).thenReturn (archmage);

		final PickPrerequisite divinePowerPrereq = new PickPrerequisite (); 
		divinePowerPrereq.setPrerequisiteCount (4);
		divinePowerPrereq.setPrerequisiteID ("MB01");
		
		final Pick divinePower = new Pick ();
		divinePower.getPickPrerequisite ().add (divinePowerPrereq);
		when (db.findPick (eq ("RT02"), anyString ())).thenReturn (divinePower);
		
		final PickPrerequisite sorceryMasteryPrereq = new PickPrerequisite (); 
		sorceryMasteryPrereq.setPrerequisiteCount (4);
		sorceryMasteryPrereq.setPrerequisiteID ("MB03");
		
		final Pick sorceryMastery = new Pick ();
		sorceryMastery.getPickPrerequisite ().add (sorceryMasteryPrereq);
		when (db.findPick (eq ("RT03"), anyString ())).thenReturn (sorceryMastery);
		
		final Pick runemaster = new Pick ();
		for (int n = 1; n <= 3; n++)
		{
			final PickPrerequisite req = new PickPrerequisite ();
			req.setPrerequisiteTypeID ("B");
			req.setPrerequisiteCount (2);
			runemaster.getPickPrerequisite ().add (req);
		}
		when (db.findPick (eq ("RT04"), anyString ())).thenReturn (runemaster);
		
		final Pick nodeMastery = new Pick ();
		for (int n = 3; n <= 5; n++)
		{
			final PickPrerequisite req = new PickPrerequisite ();
			req.setPrerequisiteID ("MB0" + n);
			req.setPrerequisiteCount (1);
			nodeMastery.getPickPrerequisite ().add (req);
		}
		when (db.findPick (eq ("RT05"), anyString ())).thenReturn (nodeMastery);
		
		// Set up object to test
		final PlayerPickUtilsImpl utils = new PlayerPickUtilsImpl (); 

		// Divine power needs 4 life books; Archmage needs 4 of any book; so both should return false with 3 life books
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		utils.updatePickQuantity (picks, "MB01", 3);
		assertEquals ("Archmage was allowed with only 3 books", false, utils.meetsPickRequirements ("RT01", picks, db));
		assertEquals ("Divine Power was allowed with only 3 life books", false, utils.meetsPickRequirements ("RT02", picks, db));

		// Archmage needs all 4 books of the same type - adding a sorcery book doesn't help
		utils.updatePickQuantity (picks, "MB03", 1);
		assertEquals ("Archmage was allowed with all 4 books not being the same type", false, utils.meetsPickRequirements ("RT01", picks, db));

		// Both should be OK with 4 life books
		utils.updatePickQuantity (picks, "MB01", 1);
		assertEquals ("Archmage was not allowed with only 4 life books", true, utils.meetsPickRequirements ("RT01", picks, db));
		assertEquals ("Divine Power was not allowed with only 4 life books", true, utils.meetsPickRequirements ("RT02", picks, db));

		// Sorcery mastery needs 4 sorcery books
		assertEquals ("Sorcery Mastery was allowed with only 1 sorcery book", false, utils.meetsPickRequirements ("RT03", picks, db));
		utils.updatePickQuantity (picks, "MB03", 3);
		assertEquals ("Sorcery Mastery was not allowed with 4 sorcery books", true, utils.meetsPickRequirements ("RT03", picks, db));

		// Runemaster requires 2 of 3 different types of spell book
		assertEquals ("Runemaster was allowed with only 2 types of book", false, utils.meetsPickRequirements ("RT04", picks, db));
		utils.updatePickQuantity (picks, "MB04", 1);
		assertEquals ("Runemaster was allowed with only having 1 book in the 3rd type", false, utils.meetsPickRequirements ("RT04", picks, db));
		utils.updatePickQuantity (picks, "MB04", 1);
		assertEquals ("Runemaster was not allowed with 2 of 3 different types of spell book", true, utils.meetsPickRequirements ("RT04", picks, db));

		// Node mastery requires 3 specific books
		assertEquals ("Node Mastery was allowed without a Chaos book", false, utils.meetsPickRequirements ("RT05", picks, db));
		utils.updatePickQuantity (picks, "MB05", 1);
		assertEquals ("Node Mastery was not allowed with all 3 book types", true, utils.meetsPickRequirements ("RT05", picks, db));
	}

	/**
	 * Tests the canSafelyRemove method
	 * @throws RecordNotFoundException If there is a problem
	 */
	@Test
	public final void testCanSafelyRemove () throws RecordNotFoundException
	{
		final PlayerPickUtilsImpl utils = new PlayerPickUtilsImpl (); 
		final CommonDatabase db = GenerateTestData.createDB ();
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();

		// Archmage needs 4 of any book so should return false if we try to remove a life book
		utils.updatePickQuantity (picks, GenerateTestData.ARCHMAGE, 1);
		utils.updatePickQuantity (picks, GenerateTestData.LIFE_BOOK, 4);
		assertEquals ("Allowed to remove a book even though we have Archmage", false, utils.canSafelyRemove (GenerateTestData.LIFE_BOOK, picks, db));
		assertEquals ("Allowed to remove a book that we don't have", false, utils.canSafelyRemove (GenerateTestData.SORCERY_BOOK, picks, db));

		// If we add another book first, then should be able to remove it
		utils.updatePickQuantity (picks, GenerateTestData.LIFE_BOOK, 1);
		assertEquals ("Not allowed to remove a book even though we have 5 of them", true, utils.canSafelyRemove (GenerateTestData.LIFE_BOOK, picks, db));
	}

	/**
	 * Tests the canSafelyAdd method
	 * @throws RecordNotFoundException If there is a problem
	 */
	@Test
	public final void testCanSafelyAdd () throws RecordNotFoundException
	{
		// Mock database
		final Pick pickB = new Pick ();

		final PickExclusiveFrom ex = new PickExclusiveFrom ();
		ex.setPickExclusiveFromID ("A");
		pickB.getPickExclusiveFrom ().add (ex);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findPick ("B", "canSafelyAdd")).thenReturn (pickB);
		
		// Set up object to test
		final PlayerPickUtilsImpl utils = new PlayerPickUtilsImpl ();
		
		// Can't add a death book if we have a life book
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		utils.updatePickQuantity (picks, "C", 1);
		utils.updatePickQuantity (picks, "D", 1);
		assertEquals (true, utils.canSafelyAdd ("B", picks, db));

		utils.updatePickQuantity (picks, "A", 1);
		assertEquals (false, utils.canSafelyAdd ("B", picks, db));
	}

	/**
	 * Tests the getHighestWeaponGradeGrantedByPicks method
	 * @throws RecordNotFoundException If there is a problem
	 */
	@Test
	public final void testGetHighestWeaponGradeGrantedByPicks () throws RecordNotFoundException
	{
		final PlayerPickUtilsImpl utils = new PlayerPickUtilsImpl (); 
		final CommonDatabase db = GenerateTestData.createDB ();

		// Ariel at 20 picks - she has a pile of retorts, but Alchemy isn't one of them
		final List<PlayerPick> picks = createAriel20PicksList ();
		assertEquals ("Ariel has magic weapons", 0, utils.getHighestWeaponGradeGrantedByPicks (picks, db));

		utils.updatePickQuantity (picks, CommonDatabaseConstants.RETORT_ID_ALCHEMY, 1);
		assertEquals ("Ariel still has no magic weapons even with Alchemy retort", 1, utils.getHighestWeaponGradeGrantedByPicks (picks, db));
	}

	/**
	 * Tests the totalReligiousBuildingBonus & pickIdsContributingToReligiousBuildingBonus methods
	 * @throws RecordNotFoundException If there is a problem
	 */
	@Test
	public final void testTotalReligiousBuildingBonus () throws RecordNotFoundException
	{
		final PlayerPickUtilsImpl utils = new PlayerPickUtilsImpl (); 
		final CommonDatabase db = GenerateTestData.createDB ();
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();

		utils.updatePickQuantity (picks, GenerateTestData.LIFE_BOOK, 5);
		assertEquals ("Life books shouldn't give religious buildings bonus", 0, utils.totalReligiousBuildingBonus (picks, db));

		utils.updatePickQuantity (picks, GenerateTestData.DIVINE_POWER, 1);
		assertEquals ("Divine power should give religious buildings bonus", 50, utils.totalReligiousBuildingBonus (picks, db));

		final List<String> pickIDs = utils.pickIdsContributingToReligiousBuildingBonus (picks, db);
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
		final PlayerPickUtilsImpl utils = new PlayerPickUtilsImpl (); 
		final CommonDatabase db = GenerateTestData.createDB ();
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();

		// Archmage gives +50% to magic power spent on improving skill; whatever unit type we pass in is irrelevant
		utils.updatePickQuantity (picks, GenerateTestData.ARCHMAGE, 1);
		assertEquals ("Archmage didn't give +50% to magic power spent on improving skill", 50, utils.totalProductionBonus (CommonDatabaseConstants.PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT, null, picks, db));
		assertEquals ("Archmage didn't give +50% to magic power spent on improving skill with a unit type supplied", 50, utils.totalProductionBonus (CommonDatabaseConstants.PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT, CommonDatabaseConstants.UNIT_TYPE_ID_SUMMONED, picks, db));

		// Summoner gives +25% to unit upkeep reduction but only on unit type = Summoned
		utils.updatePickQuantity (picks, GenerateTestData.SUMMONER, 1);
		assertEquals ("Summoner didn't give +25% to unit upkeep reduction on summoning spells", 25, utils.totalProductionBonus (CommonDatabaseConstants.PRODUCTION_TYPE_ID_UNIT_UPKEEP_REDUCTION, CommonDatabaseConstants.UNIT_TYPE_ID_SUMMONED, picks, db));
		assertEquals ("Summoner still gave +25% to unit upkeep reduction on spells for the wrong unit type", 0, utils.totalProductionBonus (CommonDatabaseConstants.PRODUCTION_TYPE_ID_UNIT_UPKEEP_REDUCTION, "N", picks, db));
		assertEquals ("Summoner still gave +25% to unit upkeep reduction with a null unit type", 0, utils.totalProductionBonus (CommonDatabaseConstants.PRODUCTION_TYPE_ID_UNIT_UPKEEP_REDUCTION, null, picks, db));

		// NB. Most of the other bonuses are to spell research and/or casting cost reduction, which can't be tested with this method since they
		// take into account the session description settings for whether to add or multiply the bonuses together
	}
}