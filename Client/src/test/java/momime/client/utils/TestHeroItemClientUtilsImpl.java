package momime.client.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import momime.common.MomException;
import momime.common.database.HeroItemBonus;
import momime.common.database.HeroItemBonusStat;

/**
 * Tests the HeroItemClientUtilsImpl class
 */
public final class TestHeroItemClientUtilsImpl
{
	/**
	 * Tests the insertGapsBetweenDifferentKindsOfAttributeBonuses method
	 * @throws MomException If we encounter a bonus in the list that doesn't give a bonus to *any* attribute, or to multiple attributes 
	 */
	@Test
	public final void testInsertGapsBetweenDifferentKindsOfAttributeBonuses () throws MomException
	{
		// Set up object to test
		final HeroItemClientUtilsImpl utils = new HeroItemClientUtilsImpl ();

		// Empty list
		final List<HeroItemBonus> bonuses = new ArrayList<HeroItemBonus> ();
		
		utils.insertGapsBetweenDifferentKindsOfAttributeBonuses (bonuses);
		assertEquals (0, bonuses.size ());
		
		// List with only 1 item
		final HeroItemBonusStat firstBonusStat = new HeroItemBonusStat ();
		firstBonusStat.setUnitSkillID ("UA01");
		
		final HeroItemBonus firstBonus = new HeroItemBonus ();
		firstBonus.getHeroItemBonusStat ().add (firstBonusStat);
		bonuses.add (firstBonus);

		utils.insertGapsBetweenDifferentKindsOfAttributeBonuses (bonuses);
		assertEquals (1, bonuses.size ());
		assertSame (firstBonus, bonuses.get (0));
		
		// 2 items which give bonuses to the same stat
		final HeroItemBonusStat secondBonusStat = new HeroItemBonusStat ();
		secondBonusStat.setUnitSkillID ("UA01");
		
		final HeroItemBonus secondBonus = new HeroItemBonus ();
		secondBonus.getHeroItemBonusStat ().add (secondBonusStat);
		bonuses.add (secondBonus);

		utils.insertGapsBetweenDifferentKindsOfAttributeBonuses (bonuses);
		assertEquals (2, bonuses.size ());
		assertSame (firstBonus, bonuses.get (0));
		assertSame (secondBonus, bonuses.get (1));
		
		// Real example with 2-3-1
		for (final String skillID : new String [] {"UA02", "UA02", "UA02", "UA03"})
		{
			final HeroItemBonusStat otherBonusStat = new HeroItemBonusStat ();
			otherBonusStat.setUnitSkillID (skillID);
			
			final HeroItemBonus otherBonus = new HeroItemBonus ();
			otherBonus.getHeroItemBonusStat ().add (otherBonusStat);
			bonuses.add (otherBonus);
		}

		utils.insertGapsBetweenDifferentKindsOfAttributeBonuses (bonuses);
		assertEquals (8, bonuses.size ());
		assertEquals ("UA01", bonuses.get (0).getHeroItemBonusStat ().get (0).getUnitSkillID ());
		assertEquals ("UA01", bonuses.get (1).getHeroItemBonusStat ().get (0).getUnitSkillID ());
		assertNull (bonuses.get (2));
		assertEquals ("UA02", bonuses.get (3).getHeroItemBonusStat ().get (0).getUnitSkillID ());
		assertEquals ("UA02", bonuses.get (4).getHeroItemBonusStat ().get (0).getUnitSkillID ());
		assertEquals ("UA02", bonuses.get (5).getHeroItemBonusStat ().get (0).getUnitSkillID ());
		assertNull (bonuses.get (6));
		assertEquals ("UA03", bonuses.get (7).getHeroItemBonusStat ().get (0).getUnitSkillID ());
	}
	
	/**
	 * Tests the insertGapsBetweenDifferentKindsOfAttributeBonuses method, when we've got a bonus that gives a bonus to 0 attributes
	 * @throws MomException If we encounter a bonus in the list that doesn't give a bonus to *any* attribute, or to multiple attributes 
	 */
	@Test
	public final void testInsertGapsBetweenDifferentKindsOfAttributeBonuses_ZeroBonuses () throws MomException
	{
		// Set up sample list
		final List<HeroItemBonus> bonuses = new ArrayList<HeroItemBonus> ();
		
		final HeroItemBonus firstBonus = new HeroItemBonus ();
		bonuses.add (firstBonus);

		// Set up object to test
		final HeroItemClientUtilsImpl utils = new HeroItemClientUtilsImpl ();

		// Run method
		assertThrows (MomException.class, () ->
		{
			utils.insertGapsBetweenDifferentKindsOfAttributeBonuses (bonuses);
		});
	}
	
	/**
	 * Tests the insertGapsBetweenDifferentKindsOfAttributeBonuses method, when we've got a bonus that gives a bonus to multiple attributes
	 * @throws MomException If we encounter a bonus in the list that doesn't give a bonus to *any* attribute, or to multiple attributes 
	 */
	@Test
	public final void testInsertGapsBetweenDifferentKindsOfAttributeBonuses_MultiBonuses () throws MomException
	{
		// Set up sample list
		final List<HeroItemBonus> bonuses = new ArrayList<HeroItemBonus> ();
		
		final HeroItemBonus firstBonus = new HeroItemBonus ();
		bonuses.add (firstBonus);

		for (int n = 0; n < 2; n++)
		{
			final HeroItemBonusStat firstBonusStat = new HeroItemBonusStat ();
			firstBonusStat.setUnitSkillID ("UA01");
			firstBonus.getHeroItemBonusStat ().add (firstBonusStat);
		}
		
		// Set up object to test
		final HeroItemClientUtilsImpl utils = new HeroItemClientUtilsImpl ();

		// Run method
		assertThrows (MomException.class, () ->
		{
			utils.insertGapsBetweenDifferentKindsOfAttributeBonuses (bonuses);
		});
	}
	
	/**
	 * Tests the shuffleSplitPoint method when all items are in a single column so there's nothing to do
	 * @throws MomException If after shuffling the items, we end up with a list that's too long to display (i.e. is > count * 2); or the left list is so long that we can't split it
	 */
	@Test
	public final void testShuffleSplitPoint_SingleColumn () throws MomException
	{
		// Set up sample list
		final List<HeroItemBonus> bonuses = new ArrayList<HeroItemBonus> ();
		
		for (int n = 0; n < 5; n++)
			bonuses.add (new HeroItemBonus ());
		
		// Set up object to test
		final HeroItemClientUtilsImpl utils = new HeroItemClientUtilsImpl ();
		
		// Run method
		utils.shuffleSplitPoint (bonuses, 5);
		
		// Check results
		assertEquals (5, bonuses.size ());
		assertNotNull (bonuses.get (0));
		assertNotNull (bonuses.get (1));
		assertNotNull (bonuses.get (2));
		assertNotNull (bonuses.get (3));
		assertNotNull (bonuses.get (4));
	}

	/**
	 * Tests the shuffleSplitPoint method when the items are already split at the right point
	 * @throws MomException If after shuffling the items, we end up with a list that's too long to display (i.e. is > count * 2); or the left list is so long that we can't split it
	 */
	@Test
	public final void testShuffleSplitPoint_AlreadyCorrectSplit () throws MomException
	{
		// Set up sample list
		final List<HeroItemBonus> bonuses = new ArrayList<HeroItemBonus> ();
		
		for (int n = 0; n < 8; n++)
			bonuses.add ((n == 5) ? null : new HeroItemBonus ());
		
		// Set up object to test
		final HeroItemClientUtilsImpl utils = new HeroItemClientUtilsImpl ();
		
		// Run method
		utils.shuffleSplitPoint (bonuses, 5);
		
		// Check results - the null gets removed
		assertEquals (7, bonuses.size ());
		assertNotNull (bonuses.get (0));
		assertNotNull (bonuses.get (1));
		assertNotNull (bonuses.get (2));
		assertNotNull (bonuses.get (3));
		assertNotNull (bonuses.get (4));
		
		assertNotNull (bonuses.get (5));
		assertNotNull (bonuses.get (6));
	}

	/**
	 * Tests the shuffleSplitPoint method when it actually needs to do some work
	 * @throws MomException If after shuffling the items, we end up with a list that's too long to display (i.e. is > count * 2); or the left list is so long that we can't split it
	 */
	@Test
	public final void testShuffleSplitPoint_SplitNeeded () throws MomException
	{
		// Set up sample list - like XX XX XX|X XX
		final List<HeroItemBonus> bonuses = new ArrayList<HeroItemBonus> ();
		
		for (final char c : "XX XX XXX XX".toCharArray ())
			bonuses.add ((c == ' ') ? null : new HeroItemBonus ());
		
		// Set up object to test
		final HeroItemClientUtilsImpl utils = new HeroItemClientUtilsImpl ();
		
		// Run method
		utils.shuffleSplitPoint (bonuses, 8);
		
		// Check results - should end up with XX XX   |XXX XX
		assertEquals (14, bonuses.size ());
		assertNotNull (bonuses.get (0));
		assertNotNull (bonuses.get (1));
		assertNull (bonuses.get (2));
		assertNotNull (bonuses.get (3));
		assertNotNull (bonuses.get (4));
		assertNull (bonuses.get (5));
		assertNull (bonuses.get (6));
		assertNull (bonuses.get (7));
		
		assertNotNull (bonuses.get (8));
		assertNotNull (bonuses.get (9));
		assertNotNull (bonuses.get (10));
		assertNull (bonuses.get (11));
		assertNotNull (bonuses.get (12));
		assertNotNull (bonuses.get (13));
	}

	/**
	 * Tests the shuffleSplitPoint method when there's too many items in the left column to split it
	 * @throws MomException If after shuffling the items, we end up with a list that's too long to display (i.e. is > count * 2); or the left list is so long that we can't split it
	 */
	@Test
	public final void testShuffleSplitPoint_OverfillLeftColumn () throws MomException
	{
		// Set up sample list
		final List<HeroItemBonus> bonuses = new ArrayList<HeroItemBonus> ();
		
		for (int n = 0; n < 6; n++)
			bonuses.add (new HeroItemBonus ());
		
		// Set up object to test
		final HeroItemClientUtilsImpl utils = new HeroItemClientUtilsImpl ();
		
		// Run method
		assertThrows (MomException.class, () ->
		{
			utils.shuffleSplitPoint (bonuses, 5);
		});
	}

	/**
	 * Tests the shuffleSplitPoint method when there's too many items and it will overflow the right column
	 * @throws MomException If after shuffling the items, we end up with a list that's too long to display (i.e. is > count * 2); or the left list is so long that we can't split it
	 */
	@Test
	public final void testShuffleSplitPoint_OverfillRightColumn () throws MomException
	{
		// Set up sample list
		final List<HeroItemBonus> bonuses = new ArrayList<HeroItemBonus> ();
		
		for (int n = 0; n < 12; n++)
			bonuses.add ((n == 5) ? null : new HeroItemBonus ());
		
		// Set up object to test
		final HeroItemClientUtilsImpl utils = new HeroItemClientUtilsImpl ();
		
		// Run method
		assertThrows (MomException.class, () ->
		{
			utils.shuffleSplitPoint (bonuses, 5);
		});
	}
}