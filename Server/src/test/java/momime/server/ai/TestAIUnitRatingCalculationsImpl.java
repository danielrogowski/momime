package momime.server.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.UnitEx;
import momime.common.database.UnitSkillEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;
import momime.common.utils.ExpandUnitDetailsImpl;
import momime.common.utils.ExpandUnitDetailsUtilsImpl;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.UnitDetailsUtilsImpl;
import momime.common.utils.UnitUtilsImpl;
import momime.server.ServerTestData;
import momime.server.utils.UnitServerUtilsImpl;
import momime.server.utils.UnitSkillDirectAccessImpl;

/**
 * Tests the AIUnitRatingCalculationsImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestAIUnitRatingCalculationsImpl extends ServerTestData
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (TestAIUnitRatingCalculationsImpl.class);
	
	/**
	 * Tests the calculateUnitRating method on a relatively simple unit with some skills with no value,
	 * some skills with an additive value, some skills with a negative value, and it is slightly damaged.
	 * So what we don't cover here are skills with diminishing values.
	 * 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateUnitRating_Basic () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitSkillEx skillOne = new UnitSkillEx ();
		skillOne.setAiRatingAdditive (10);
		when (db.findUnitSkill ("US001", "calculateUnitRating")).thenReturn (skillOne);

		final UnitSkillEx skillThree = new UnitSkillEx ();
		when (db.findUnitSkill ("US003", "calculateUnitRating")).thenReturn (skillThree);

		final UnitSkillEx skillFour = new UnitSkillEx ();
		skillFour.setAiRatingAdditive (25);
		when (db.findUnitSkill ("US004", "calculateUnitRating")).thenReturn (skillFour);

		final UnitSkillEx skillFive = new UnitSkillEx ();
		skillFive.setAiRatingMultiplicative (2d);
		when (db.findUnitSkill ("US005", "calculateUnitRating")).thenReturn (skillFive);
		
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
		when (xu.getModifiedSkillValue ("US004")).thenReturn (0);
		
		unitSkills.add ("US005");
		when (xu.getModifiedSkillValue ("US005")).thenReturn (0);
		
		when (xu.listModifiedSkillIDs ()).thenReturn (unitSkills);
		
		// Health
		when (xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS)).thenReturn (4);
		when (xu.calculateHitPointsRemaining ()).thenReturn (26);		// So there are 6.5 figures remaining
		
		// Set up object to test
		final AIUnitRatingCalculationsImpl ai  = new AIUnitRatingCalculationsImpl ();
		
		// Run method
		assertEquals ((int) ((70 + 25) * 2 * 1.55d), ai.calculateUnitRating (xu, db)); 
	}

	/**
	 * Tests the calculateUnitRating method on skill with diminishing values, but we don't hit the maximum possible.
	 * 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateUnitRating_Diminishing () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitSkillEx skillOne = new UnitSkillEx ();
		skillOne.setAiRatingAdditive (10);
		skillOne.setAiRatingDiminishesAfter (5);
		when (db.findUnitSkill ("US001", "calculateUnitRating")).thenReturn (skillOne);
		
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
		final AIUnitRatingCalculationsImpl ai  = new AIUnitRatingCalculationsImpl ();
		
		// Run method
		assertEquals (50 + 9 + 8 + 7, ai.calculateUnitRating (xu, db)); 
	}

	/**
	 * Tests the calculateUnitRating method on skill with diminishing values, where we are way over the maximum possible.
	 * 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateUnitRating_Diminishing_Maximum () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitSkillEx skillOne = new UnitSkillEx ();
		skillOne.setAiRatingAdditive (10);
		skillOne.setAiRatingDiminishesAfter (5);
		when (db.findUnitSkill ("US001", "calculateUnitRating")).thenReturn (skillOne);
		
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
		final AIUnitRatingCalculationsImpl ai  = new AIUnitRatingCalculationsImpl ();
		
		// Run method
		assertEquals (50 + 9 + 8 + 7 + 6 + 5 + 4 + 3 + 2 + 1, ai.calculateUnitRating (xu, db)); 
	}

	/**
	 * Not a unit test as such.  This calculates unit ratings for every unit defined in the game and sorts them from
	 * weakest to strongest and outputs the list, to check that the calculated ranking looks sensible.
	 * 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUnitBaseRatings () throws Exception
	{
		// Need the real database
		final CommonDatabase db = loadServerDatabase ();
		
		// Other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Set up object to test
		final UnitDetailsUtilsImpl unitDetailsUtils = new UnitDetailsUtilsImpl ();
		unitDetailsUtils.setMemoryCombatAreaEffectUtils (mock (MemoryCombatAreaEffectUtils.class));
		
		final UnitUtilsImpl unitUtils = new UnitUtilsImpl ();

		final ExpandUnitDetailsUtilsImpl expandUnitDetailsUtils = new ExpandUnitDetailsUtilsImpl ();
		expandUnitDetailsUtils.setUnitDetailsUtils (unitDetailsUtils);
		expandUnitDetailsUtils.setUnitUtils (unitUtils);
		
		final ExpandUnitDetailsImpl expand = new ExpandUnitDetailsImpl ();
		expand.setExpandUnitDetailsUtils (expandUnitDetailsUtils);
		expand.setUnitUtils (unitUtils);
		
		final UnitServerUtilsImpl unitServerUtils = new UnitServerUtilsImpl ();
		unitServerUtils.setExpandUnitDetails (expand);
		unitServerUtils.setUnitUtils (unitUtils);
		
		final AIUnitRatingCalculationsImpl ai = new AIUnitRatingCalculationsImpl ();
		ai.setExpandUnitDetails (expand);
		ai.setUnitSkillDirectAccess (new UnitSkillDirectAccessImpl ());
		
		// Calculate each unit in turn
		final Map<Integer, List<UnitEx>> ratings = new HashMap<Integer, List<UnitEx>> ();
		for (final UnitEx unitDef : db.getUnits ())
		{
			// Need to create a real MemoryUnit so that heroes get their item slots
			final MemoryUnit unit = unitServerUtils.createMemoryUnit (unitDef.getUnitID (), 0, null, 0, db);
			final int rating = ai.calculateUnitCurrentRating (unit, null, players, fow, db);
			
			List<UnitEx> unitsWithThisScore = ratings.get (rating);
			if (unitsWithThisScore == null)
			{
				unitsWithThisScore = new ArrayList<UnitEx> ();
				ratings.put (rating, unitsWithThisScore);
			}
			unitsWithThisScore.add (unitDef);
		}
		
		// Sort results and output
		ratings.entrySet ().stream ().sorted ((e1, e2) -> e1.getKey () - e2.getKey ()).forEach (e -> e.getValue ().forEach (u ->
		{
			String race = "";
			if (u.getUnitRaceID () != null)
				race = u.getUnitRaceID () + " - ";
			log.debug (race + u.getUnitName ().get (0).getText () + " has base rating of " + e.getKey ());
		}));
	}
	
	/**
	 * As above, but calculates potential ratings.
	 * 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUnitPotentialRatings () throws Exception
	{
		// Need the real database
		final CommonDatabase db = loadServerDatabase ();
		
		// Other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Set up object to test
		final UnitDetailsUtilsImpl unitDetailsUtils = new UnitDetailsUtilsImpl ();
		unitDetailsUtils.setMemoryCombatAreaEffectUtils (mock (MemoryCombatAreaEffectUtils.class));
		
		final UnitUtilsImpl unitUtils = new UnitUtilsImpl ();

		final ExpandUnitDetailsUtilsImpl expandUnitDetailsUtils = new ExpandUnitDetailsUtilsImpl ();
		expandUnitDetailsUtils.setUnitDetailsUtils (unitDetailsUtils);
		expandUnitDetailsUtils.setUnitUtils (unitUtils);
		
		final ExpandUnitDetailsImpl expand = new ExpandUnitDetailsImpl ();
		expand.setExpandUnitDetailsUtils (expandUnitDetailsUtils);
		expand.setUnitUtils (unitUtils);
		
		final UnitServerUtilsImpl unitServerUtils = new UnitServerUtilsImpl ();
		unitServerUtils.setExpandUnitDetails (expand);
		unitServerUtils.setUnitUtils (unitUtils);
		
		final AIUnitRatingCalculationsImpl ai = new AIUnitRatingCalculationsImpl ();
		ai.setExpandUnitDetails (expand);
		ai.setUnitSkillDirectAccess (new UnitSkillDirectAccessImpl ());
		
		// Calculate each unit in turn
		final Map<Integer, List<UnitEx>> ratings = new HashMap<Integer, List<UnitEx>> ();
		for (final UnitEx unitDef : db.getUnits ())
		{
			// Need to create a real MemoryUnit so that heroes get their item slots
			final MemoryUnit unit = unitServerUtils.createMemoryUnit (unitDef.getUnitID (), 0, null, 0, db);
			final int rating = ai.calculateUnitPotentialRating (unit, players, fow, db);
			
			List<UnitEx> unitsWithThisScore = ratings.get (rating);
			if (unitsWithThisScore == null)
			{
				unitsWithThisScore = new ArrayList<UnitEx> ();
				ratings.put (rating, unitsWithThisScore);
			}
			unitsWithThisScore.add (unitDef);
		}
		
		// Sort results and output
		ratings.entrySet ().stream ().sorted ((e1, e2) -> e1.getKey () - e2.getKey ()).forEach (e -> e.getValue ().forEach (u ->
		{
			String race = "";
			if (u.getUnitRaceID () != null)
				race = u.getUnitRaceID () + " - ";
			log.debug (race + u.getUnitName ().get (0).getText () + " has potential rating of " + e.getKey ());
		}));
	}
}