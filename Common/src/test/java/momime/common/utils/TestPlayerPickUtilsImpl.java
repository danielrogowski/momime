package momime.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import momime.common.database.CommonDatabase;
import momime.common.database.Pick;
import momime.common.database.PickExclusiveFrom;
import momime.common.database.PickPrerequisite;
import momime.common.database.PickProductionBonus;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.PlayerPick;

/**
 * Tests the PlayerPickUtils class
 */
public final class TestPlayerPickUtilsImpl
{
	/**
	 * Tests the getTotalPickCost method
	 * @throws RecordNotFoundException If there is a problem
	 */
	@Test
	public final void testGetTotalPickCost () throws RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Pick pickOneDef = new Pick ();
		pickOneDef.setPickCost (3);
		when (db.findPick ("A", "getTotalPickCost")).thenReturn (pickOneDef);

		final Pick pickTwoDef = new Pick ();
		pickTwoDef.setPickCost (2);
		when (db.findPick ("B", "getTotalPickCost")).thenReturn (pickTwoDef);
		
		// Set up pick list
		final PlayerPick pickOne = new PlayerPick ();
		pickOne.setPickID ("A");
		pickOne.setQuantity (1);

		final PlayerPick pickTwo = new PlayerPick ();
		pickTwo.setPickID ("B");
		pickTwo.setQuantity (8);
		
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		picks.add (pickOne);
		picks.add (pickTwo);
		
		// Set up object to test
		final PlayerPickUtilsImpl utils = new PlayerPickUtilsImpl (); 

		// Run method
		assertEquals ((1*3) + (2*8), utils.getTotalPickCost (picks, db));
	}

	/**
	 * Tests the getQuantityOfPick method
	 */
	@Test
	public final void testGetQuantityOfPick ()
	{
		// Set up pick list
		final PlayerPick pickOne = new PlayerPick ();
		pickOne.setPickID ("A");
		pickOne.setQuantity (1);

		final PlayerPick pickTwo = new PlayerPick ();
		pickTwo.setPickID ("B");
		pickTwo.setQuantity (8);
		
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		picks.add (pickOne);
		picks.add (pickTwo);

		// Set up object to test
		final PlayerPickUtilsImpl utils = new PlayerPickUtilsImpl ();
		
		// Run method
		assertEquals (1, utils.getQuantityOfPick (picks, "A"));
		assertEquals (8, utils.getQuantityOfPick (picks, "B"));
		assertEquals (0, utils.getQuantityOfPick (picks, "C"));
	}

	/**
	 * Tests the updatePickQuantity method
	 */
	@Test
	public final void testUpdatePickQuantity ()
	{
		// Start with empty list
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		
		// Set up object to test
		final PlayerPickUtilsImpl utils = new PlayerPickUtilsImpl (); 

		// Add one
		utils.updatePickQuantity (picks, "A", 3);
		assertEquals (1, picks.size ());
		assertEquals ("A", picks.get (0).getPickID ());
		assertEquals (3, picks.get (0).getQuantity ());

		// Add another
		utils.updatePickQuantity (picks, "B", 2);
		assertEquals (2, picks.size ());
		assertEquals ("A", picks.get (0).getPickID ());
		assertEquals (3, picks.get (0).getQuantity ());
		assertEquals ("B", picks.get (1).getPickID ());
		assertEquals (2, picks.get (1).getQuantity ());

		// Increase quantity of existing pick
		utils.updatePickQuantity (picks, "A", 2);
		assertEquals (2, picks.size ());
		assertEquals ("A", picks.get (0).getPickID ());
		assertEquals (5, picks.get (0).getQuantity ());
		assertEquals ("B", picks.get (1).getPickID ());
		assertEquals (2, picks.get (1).getQuantity ());

		// Reduce quantity of existing pick to 0 so it gets removed
		utils.updatePickQuantity (picks, "A", -5);
		assertEquals (1, picks.size ());
		assertEquals ("B", picks.get (0).getPickID ());
		assertEquals (2, picks.get (0).getQuantity ());
	}

	/**
	 * Tests the countPicksOfType method
	 * @throws RecordNotFoundException If there is a problem
	 */
	@Test
	public final void testCountPicksOfType () throws RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Pick pickOneDef = new Pick ();
		pickOneDef.setPickType ("Y");
		when (db.findPick ("A", "countPicksOfType")).thenReturn (pickOneDef);

		final Pick pickTwoDef = new Pick ();
		pickTwoDef.setPickType ("Z");
		when (db.findPick ("B", "countPicksOfType")).thenReturn (pickTwoDef);

		final Pick pickThreeDef = new Pick ();
		pickThreeDef.setPickType ("Y");
		when (db.findPick ("C", "countPicksOfType")).thenReturn (pickThreeDef);
		
		// Set up pick list
		final PlayerPick pickOne = new PlayerPick ();
		pickOne.setPickID ("A");
		pickOne.setOriginalQuantity (1);
		pickOne.setQuantity (2);

		final PlayerPick pickTwo = new PlayerPick ();
		pickTwo.setPickID ("B");
		pickTwo.setOriginalQuantity (3);
		pickTwo.setQuantity (4);

		final PlayerPick pickThree = new PlayerPick ();
		pickThree.setPickID ("C");
		pickThree.setOriginalQuantity (5);
		pickThree.setQuantity (7);
		
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		picks.add (pickOne);
		picks.add (pickTwo);
		picks.add (pickThree);
		
		// Set up object to test
		final PlayerPickUtilsImpl utils = new PlayerPickUtilsImpl (); 

		// Run method
		assertEquals (2+7, utils.countPicksOfType (picks, "Y", false, db));
		assertEquals (1+5, utils.countPicksOfType (picks, "Y", true, db));
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
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final Pick pickOneDef = new Pick ();
		when (db.findPick ("A", "meetsPickRequirements")).thenReturn (pickOneDef);

		final PickPrerequisite prereq = new PickPrerequisite ();
		prereq.setPrerequisiteID ("A");
		prereq.setPrerequisiteCount (4);
		
		final Pick pickTwoDef = new Pick ();
		pickTwoDef.getPickPrerequisite ().add (prereq);		
		when (db.findPick ("B", "meetsPickRequirements")).thenReturn (pickTwoDef);
		
		// Set up pick list
		final PlayerPick pickOne = new PlayerPick ();
		pickOne.setPickID ("A");
		pickOne.setQuantity (4);

		final PlayerPick pickTwo = new PlayerPick ();
		pickTwo.setPickID ("B");
		pickTwo.setQuantity (1);
		
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		picks.add (pickOne);
		picks.add (pickTwo);
		
		// Set up object to test
		final PlayerPickUtilsImpl utils = new PlayerPickUtilsImpl ();
		
		// We cannot remove a book that we don't have
		assertFalse (utils.canSafelyRemove ("C", picks, db));

		// We cannot remove one of our 4 A's since we have a book B that requires all 4 A's, but we can remove the B
		assertFalse (utils.canSafelyRemove ("A", picks, db));
		assertTrue (utils.canSafelyRemove ("B", picks, db));
		
		// If we add another book first, then should be able to remove it
		pickOne.setQuantity (5);
		assertTrue (utils.canSafelyRemove ("A", picks, db));
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
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final Pick pickOneDef = new Pick ();
		pickOneDef.setPickMagicWeapons (1);
		when (db.findPick ("A", "getHighestWeaponGradeGrantedByPicks")).thenReturn (pickOneDef);

		final Pick pickTwoDef = new Pick ();
		when (db.findPick ("B", "getHighestWeaponGradeGrantedByPicks")).thenReturn (pickTwoDef);

		final Pick pickThreeDef = new Pick ();
		pickThreeDef.setPickMagicWeapons (2);
		when (db.findPick ("C", "getHighestWeaponGradeGrantedByPicks")).thenReturn (pickThreeDef);
		
		// Set up object to test
		final PlayerPickUtilsImpl utils = new PlayerPickUtilsImpl (); 
		
		// Test having no picks that grant a weapon grade
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		
		final PlayerPick pickTwo = new PlayerPick ();
		pickTwo.setPickID ("B");		
		picks.add (pickTwo);
		
		assertEquals (0, utils.getHighestWeaponGradeGrantedByPicks (picks, db));
		
		// Use the lower weapon grade
		final PlayerPick pickOne = new PlayerPick ();
		pickOne.setPickID ("A");		
		picks.add (pickOne);
		
		assertEquals (1, utils.getHighestWeaponGradeGrantedByPicks (picks, db));
		
		// Use the higher weapon grade
		final PlayerPick pickThree = new PlayerPick ();
		pickThree.setPickID ("C");		
		picks.add (pickThree);
		
		assertEquals (2, utils.getHighestWeaponGradeGrantedByPicks (picks, db));
	}

	/**
	 * Tests the totalReligiousBuildingBonus method
	 * @throws RecordNotFoundException If there is a problem
	 */
	@Test
	public final void testTotalReligiousBuildingBonus () throws RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final Pick pickOneDef = new Pick ();
		pickOneDef.setPickReligiousBuildingBonus (2);
		when (db.findPick ("A", "totalReligiousBuildingBonus")).thenReturn (pickOneDef);

		final Pick pickTwoDef = new Pick ();
		when (db.findPick ("B", "totalReligiousBuildingBonus")).thenReturn (pickTwoDef);

		final Pick pickThreeDef = new Pick ();
		pickThreeDef.setPickReligiousBuildingBonus (10);
		when (db.findPick ("C", "totalReligiousBuildingBonus")).thenReturn (pickThreeDef);
		
		// Set up pick list
		final PlayerPick pickOne = new PlayerPick ();
		pickOne.setPickID ("A");
		pickOne.setQuantity (1);

		final PlayerPick pickTwo = new PlayerPick ();
		pickTwo.setPickID ("B");
		pickTwo.setQuantity (2);

		final PlayerPick pickThree = new PlayerPick ();
		pickThree.setPickID ("C");
		pickThree.setQuantity (3);
		
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		picks.add (pickOne);
		picks.add (pickTwo);
		picks.add (pickThree);
		
		// Set up object to test
		final PlayerPickUtilsImpl utils = new PlayerPickUtilsImpl ();
		
		// Run method
		assertEquals (32, utils.totalReligiousBuildingBonus (picks, db));
	}

	/**
	 * Tests the pickIdsContributingToReligiousBuildingBonus method
	 * @throws RecordNotFoundException If there is a problem
	 */
	@Test
	public final void testPickIdsContributingToReligiousBuildingBonus () throws RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final Pick pickOneDef = new Pick ();
		pickOneDef.setPickReligiousBuildingBonus (2);
		when (db.findPick ("A", "pickIdsContributingToReligiousBuildingBonus")).thenReturn (pickOneDef);

		final Pick pickTwoDef = new Pick ();
		when (db.findPick ("B", "pickIdsContributingToReligiousBuildingBonus")).thenReturn (pickTwoDef);

		final Pick pickThreeDef = new Pick ();
		pickThreeDef.setPickReligiousBuildingBonus (10);
		when (db.findPick ("C", "pickIdsContributingToReligiousBuildingBonus")).thenReturn (pickThreeDef);
		
		// Set up pick list
		final PlayerPick pickOne = new PlayerPick ();
		pickOne.setPickID ("A");
		pickOne.setQuantity (1);

		final PlayerPick pickTwo = new PlayerPick ();
		pickTwo.setPickID ("B");
		pickTwo.setQuantity (2);

		final PlayerPick pickThree = new PlayerPick ();
		pickThree.setPickID ("C");
		pickThree.setQuantity (3);
		
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		picks.add (pickOne);
		picks.add (pickTwo);
		picks.add (pickThree);
		
		// Set up object to test
		final PlayerPickUtilsImpl utils = new PlayerPickUtilsImpl ();
		
		// Run method
		final List<String> pickIDs = utils.pickIdsContributingToReligiousBuildingBonus (picks, db);
		assertEquals (2, pickIDs.size ());
		assertEquals ("A", pickIDs.get (0));
		assertEquals ("C", pickIDs.get (1));
	}

	/**
	 * Tests the totalProductionBonus method
	 * @throws RecordNotFoundException If there is a problem
	 */
	@Test
	public final void testTotalProductionBonus () throws RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final Pick pickOneDef = new Pick ();
		when (db.findPick ("A", "totalProductionBonus")).thenReturn (pickOneDef);

		final Pick pickTwoDef = new Pick ();
		when (db.findPick ("B", "totalProductionBonus")).thenReturn (pickTwoDef);

		final Pick pickThreeDef = new Pick ();
		when (db.findPick ("C", "totalProductionBonus")).thenReturn (pickThreeDef);
		
		final PickProductionBonus bonusToAllUnitTypes = new PickProductionBonus ();
		bonusToAllUnitTypes.setProductionTypeID ("PT01");
		bonusToAllUnitTypes.setPercentageBonus (10);
		pickOneDef.getPickProductionBonus ().add (bonusToAllUnitTypes);
		
		final PickProductionBonus bonusToWrongProductionType = new PickProductionBonus ();
		bonusToWrongProductionType.setProductionTypeID ("PT02");
		bonusToWrongProductionType.setPercentageBonus (10);
		pickTwoDef.getPickProductionBonus ().add (bonusToWrongProductionType);

		final PickProductionBonus bonusToSpecificUnitType = new PickProductionBonus ();
		bonusToSpecificUnitType.setProductionTypeID ("PT01");
		bonusToSpecificUnitType.setUnitTypeID ("Z");
		bonusToSpecificUnitType.setPercentageBonus (25);
		pickThreeDef.getPickProductionBonus ().add (bonusToSpecificUnitType);
		
		// Set up pick list
		final PlayerPick pickOne = new PlayerPick ();
		pickOne.setPickID ("A");
		pickOne.setQuantity (1);

		final PlayerPick pickTwo = new PlayerPick ();
		pickTwo.setPickID ("B");
		pickTwo.setQuantity (2);

		final PlayerPick pickThree = new PlayerPick ();
		pickThree.setPickID ("C");
		pickThree.setQuantity (3);
		
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		picks.add (pickOne);
		picks.add (pickTwo);
		picks.add (pickThree);
		
		// Set up object to test
		final PlayerPickUtilsImpl utils = new PlayerPickUtilsImpl ();

		// Run method
		assertEquals (10, utils.totalProductionBonus ("PT01", "Y", picks, db));
		assertEquals (85, utils.totalProductionBonus ("PT01", "Z", picks, db));
	}
}