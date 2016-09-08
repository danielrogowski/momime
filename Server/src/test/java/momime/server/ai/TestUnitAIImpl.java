package momime.server.ai;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.UnitUtilsImpl;
import momime.server.ServerTestData;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.UnitSkillSvr;
import momime.server.database.UnitSvr;

/**
 * Tests the UnitAIImpl class
 */
public final class TestUnitAIImpl
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (TestUnitAIImpl.class);
	
	/**
	 * Tests the calculateUnitCurrentRating method on a relatively simple unit with some skills with no value,
	 * some skills with an additive value, some skills with a negative value, and it is slightly damaged.
	 * So what we don't cover here are skills with diminishing values.
	 * 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateUnitCurrentRating_Basic () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final UnitSkillSvr skillOne = new UnitSkillSvr ();
		skillOne.setAiRatingAdditive (10);
		when (db.findUnitSkill ("US001", "calculateUnitCurrentRating")).thenReturn (skillOne);

		final UnitSkillSvr skillTwo = new UnitSkillSvr ();
		skillTwo.setAiRatingAdditive (20);
		when (db.findUnitSkill ("US002", "calculateUnitCurrentRating")).thenReturn (skillTwo);

		final UnitSkillSvr skillThree = new UnitSkillSvr ();
		when (db.findUnitSkill ("US003", "calculateUnitCurrentRating")).thenReturn (skillThree);

		final UnitSkillSvr skillFour = new UnitSkillSvr ();
		skillFour.setAiRatingAdditive (25);
		when (db.findUnitSkill ("US004", "calculateUnitCurrentRating")).thenReturn (skillFour);

		final UnitSkillSvr skillFive = new UnitSkillSvr ();
		skillFive.setAiRatingMultiplicative (2d);
		when (db.findUnitSkill ("US005", "calculateUnitCurrentRating")).thenReturn (skillFive);
		
		// Mock unit details to calculate
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK)).thenReturn (true);
		
		final Set<String> unitSkills = new HashSet<String> ();
		
		// Skills
		unitSkills.add ("US001");
		when (xu.getModifiedSkillValue ("US001")).thenReturn (7);

		unitSkills.add ("US003");
		when (xu.getModifiedSkillValue ("US003")).thenReturn (6);
		
		unitSkills.add ("US004");
		unitSkills.add ("US005");
		
		when (xu.listModifiedSkillIDs ()).thenReturn (unitSkills);
		
		// Health
		when (xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS)).thenReturn (4);
		when (xu.calculateHitPointsRemaining ()).thenReturn (26);		// So there are 6.5 figures remaining
		
		// Set up object to test
		final UnitAIImpl ai = new UnitAIImpl ();
		
		// Run method
		assertEquals ((int) ((70 + 25) * 2 * 1.55d), ai.calculateUnitCurrentRating (xu, db)); 
	}

	/**
	 * Tests the calculateUnitCurrentRating method on skill with diminishing values, but we don't hit the maximum possible.
	 * 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateUnitCurrentRating_Diminishing () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final UnitSkillSvr skillOne = new UnitSkillSvr ();
		skillOne.setAiRatingAdditive (10);
		skillOne.setAiRatingDiminishesAfter (5);
		when (db.findUnitSkill ("US001", "calculateUnitCurrentRating")).thenReturn (skillOne);
		
		// Mock unit details to calculate
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK)).thenReturn (true);
		
		final Set<String> unitSkills = new HashSet<String> ();
		
		// Skills
		unitSkills.add ("US001");
		when (xu.getModifiedSkillValue ("US001")).thenReturn (8);
		
		when (xu.listModifiedSkillIDs ()).thenReturn (unitSkills);
		
		// Health
		when (xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS)).thenReturn (1);
		when (xu.calculateHitPointsRemaining ()).thenReturn (1);
		
		// Set up object to test
		final UnitAIImpl ai = new UnitAIImpl ();
		
		// Run method
		assertEquals (50 + 9 + 8 + 7, ai.calculateUnitCurrentRating (xu, db)); 
	}

	/**
	 * Tests the calculateUnitCurrentRating method on skill with diminishing values, where we are way over the maximum possible.
	 * 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateUnitCurrentRating_Diminishing_Maximum () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final UnitSkillSvr skillOne = new UnitSkillSvr ();
		skillOne.setAiRatingAdditive (10);
		skillOne.setAiRatingDiminishesAfter (5);
		when (db.findUnitSkill ("US001", "calculateUnitCurrentRating")).thenReturn (skillOne);
		
		// Mock unit details to calculate
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK)).thenReturn (true);
		
		final Set<String> unitSkills = new HashSet<String> ();
		
		// Skills
		unitSkills.add ("US001");
		when (xu.getModifiedSkillValue ("US001")).thenReturn (28);
		
		when (xu.listModifiedSkillIDs ()).thenReturn (unitSkills);
		
		// Health
		when (xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS)).thenReturn (1);
		when (xu.calculateHitPointsRemaining ()).thenReturn (1);
		
		// Set up object to test
		final UnitAIImpl ai = new UnitAIImpl ();
		
		// Run method
		assertEquals (50 + 9 + 8 + 7 + 6 + 5 + 4 + 3 + 2 + 1, ai.calculateUnitCurrentRating (xu, db)); 
	}
	
	/**
	 * Not a unit test as such.  This calculates unit ratings for every unit defined in the game and sorts them from
	 * weakest to strongest and outputs the list, to check that the calculated ranking looks sensible.
	 * 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUnitRatings () throws Exception
	{
		// Need the real database
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		
		// Other lists
		final FogOfWarMemory fow = new FogOfWarMemory (); 

		// Set up object to test
		final UnitUtilsImpl unitUtils = new UnitUtilsImpl ();
		unitUtils.setMemoryCombatAreaEffectUtils (mock (MemoryCombatAreaEffectUtils.class));
		
		final UnitAIImpl ai = new UnitAIImpl ();
		
		// Calculate each unit in turn
		final Map<Integer, List<UnitSvr>> ratings = new HashMap<Integer, List<UnitSvr>> ();
		for (final UnitSvr unitDef : db.getUnits ())
		{
			final AvailableUnit unit = new AvailableUnit ();
			unit.setUnitID (unitDef.getUnitID ());
			unitUtils.initializeUnitSkills (unit, 0, db);
			
			final ExpandedUnitDetails xu = unitUtils.expandUnitDetails (unit, null, null, null, null, fow, db);
			final int rating = ai.calculateUnitCurrentRating (xu, db);
			
			List<UnitSvr> unitsWithThisScore = ratings.get (rating);
			if (unitsWithThisScore == null)
			{
				unitsWithThisScore = new ArrayList<UnitSvr> ();
				ratings.put (rating, unitsWithThisScore);
			}
			unitsWithThisScore.add (unitDef);
		}
		
		// Sort results and output
		ratings.entrySet ().stream ().sorted ((e1, e2) -> e1.getKey () - e2.getKey ()).forEach (e -> e.getValue ().forEach (u ->
		{
			String race = "";
			if (u.getUnitRaceID () != null)
				try
				{
					race = db.findRace (u.getUnitRaceID (), "testUnitRatings").getRaceName () + " ";
				}
				catch (final RecordNotFoundException ex)
				{
				}
			log.debug (race + u.getUnitName () + " has base rating of " + e.getKey ());
		}));
	}
}