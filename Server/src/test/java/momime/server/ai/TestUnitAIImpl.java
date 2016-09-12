package momime.server.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItemBonusStat;
import momime.common.database.RecordNotFoundException;
import momime.common.database.SpellSetting;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.UnitUtils;
import momime.common.utils.UnitUtilsImpl;
import momime.server.ServerTestData;
import momime.server.database.HeroItemBonusSvr;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.UnitSkillSvr;
import momime.server.database.UnitSvr;
import momime.server.utils.UnitServerUtilsImpl;
import momime.server.utils.UnitSkillDirectAccessImpl;

/**
 * Tests the UnitAIImpl class
 */
public final class TestUnitAIImpl
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (TestUnitAIImpl.class);
	
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
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final UnitSkillSvr skillOne = new UnitSkillSvr ();
		skillOne.setAiRatingAdditive (10);
		when (db.findUnitSkill ("US001", "calculateUnitRating")).thenReturn (skillOne);

		final UnitSkillSvr skillTwo = new UnitSkillSvr ();
		skillTwo.setAiRatingAdditive (20);
		when (db.findUnitSkill ("US002", "calculateUnitRating")).thenReturn (skillTwo);

		final UnitSkillSvr skillThree = new UnitSkillSvr ();
		when (db.findUnitSkill ("US003", "calculateUnitRating")).thenReturn (skillThree);

		final UnitSkillSvr skillFour = new UnitSkillSvr ();
		skillFour.setAiRatingAdditive (25);
		when (db.findUnitSkill ("US004", "calculateUnitRating")).thenReturn (skillFour);

		final UnitSkillSvr skillFive = new UnitSkillSvr ();
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
		unitSkills.add ("US005");
		
		when (xu.listModifiedSkillIDs ()).thenReturn (unitSkills);
		
		// Health
		when (xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS)).thenReturn (4);
		when (xu.calculateHitPointsRemaining ()).thenReturn (26);		// So there are 6.5 figures remaining
		
		// Set up object to test
		final UnitAIImpl ai = new UnitAIImpl ();
		
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
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final UnitSkillSvr skillOne = new UnitSkillSvr ();
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
		final UnitAIImpl ai = new UnitAIImpl ();
		
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
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final UnitSkillSvr skillOne = new UnitSkillSvr ();
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
		final UnitAIImpl ai = new UnitAIImpl ();
		
		// Run method
		assertEquals (50 + 9 + 8 + 7 + 6 + 5 + 4 + 3 + 2 + 1, ai.calculateUnitRating (xu, db)); 
	}
	
	/**
	 * Tests the calculateHeroItemBonusRating method
	 */
	@Test
	public final void testCalculateHeroItemBonusRating ()
	{
		// Set up object to test
		final UnitAIImpl ai = new UnitAIImpl ();

		// No stat entries
		final HeroItemBonusSvr bonus = new HeroItemBonusSvr ();
		assertEquals (2, ai.calculateHeroItemBonusRating (bonus));
		
		// A +1 stat, a valueless stat, and a +5 stat
		final HeroItemBonusStat statOne = new HeroItemBonusStat ();
		statOne.setUnitSkillValue (1);
		
		final HeroItemBonusStat statTwo = new HeroItemBonusStat ();

		final HeroItemBonusStat statThree = new HeroItemBonusStat ();
		statThree.setUnitSkillValue (5);
		
		bonus.getHeroItemBonusStat ().add (statOne);
		bonus.getHeroItemBonusStat ().add (statTwo);
		bonus.getHeroItemBonusStat ().add (statThree);
	
		assertEquals (1+2+5, ai.calculateHeroItemBonusRating (bonus));
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
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		
		// Other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Set up object to test
		final UnitUtilsImpl unitUtils = new UnitUtilsImpl ();
		unitUtils.setMemoryCombatAreaEffectUtils (mock (MemoryCombatAreaEffectUtils.class));
		
		final UnitServerUtilsImpl unitServerUtils = new UnitServerUtilsImpl ();
		unitServerUtils.setUnitUtils (unitUtils);
		
		final UnitAIImpl ai = new UnitAIImpl ();
		ai.setUnitUtils (unitUtils);
		
		// Calculate each unit in turn
		final Map<Integer, List<UnitSvr>> ratings = new HashMap<Integer, List<UnitSvr>> ();
		for (final UnitSvr unitDef : db.getUnits ())
		{
			// Need to create a real MemoryUnit so that heroes get their item slots
			final MemoryUnit unit = unitServerUtils.createMemoryUnit (unitDef.getUnitID (), 0, null, 0, db);
			final int rating = ai.calculateUnitCurrentRating (unit, players, fow, db);
			
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
					race = db.findRace (u.getUnitRaceID (), "testUnitBaseRatings").getRaceName () + " ";
				}
				catch (final RecordNotFoundException ex)
				{
				}
			log.debug (race + u.getUnitName () + " has base rating of " + e.getKey ());
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
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		
		// Other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Set up object to test
		final UnitUtilsImpl unitUtils = new UnitUtilsImpl ();
		unitUtils.setMemoryCombatAreaEffectUtils (mock (MemoryCombatAreaEffectUtils.class));
		
		final UnitServerUtilsImpl unitServerUtils = new UnitServerUtilsImpl ();
		unitServerUtils.setUnitUtils (unitUtils);
		
		final UnitAIImpl ai = new UnitAIImpl ();
		ai.setUnitUtils (unitUtils);
		ai.setUnitSkillDirectAccess (new UnitSkillDirectAccessImpl ());
		
		// Calculate each unit in turn
		final Map<Integer, List<UnitSvr>> ratings = new HashMap<Integer, List<UnitSvr>> ();
		for (final UnitSvr unitDef : db.getUnits ())
		{
			// Need to create a real MemoryUnit so that heroes get their item slots
			final MemoryUnit unit = unitServerUtils.createMemoryUnit (unitDef.getUnitID (), 0, null, 0, db);
			final int rating = ai.calculateUnitPotentialRating (unit, players, fow, db);
			
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
					race = db.findRace (u.getUnitRaceID (), "testUnitBaseRatings").getRaceName () + " ";
				}
				catch (final RecordNotFoundException ex)
				{
				}
			log.debug (race + u.getUnitName () + " has potential rating of " + e.getKey ());
		}));
	}
	
	/**
	 * Tests the canAffordUnitMaintenance method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanAffordUnitMaintenance () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);

		// Player
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);
		
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		priv.setFogOfWarMemory (fow);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, pub, priv, null, null);
		
		// Session description
		final SpellSetting spellSettings = new SpellSetting ();
		
		// Test unit
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final AvailableUnit unit = new AvailableUnit ();
		
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (unit, null, null, null, players, fow, db)).thenReturn (xu);
		
		// Resources it consumes
		final Set<String> upkeeps = new HashSet<String> ();
		upkeeps.add (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		upkeeps.add (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS);
		
		when (xu.listModifiedUpkeepProductionTypeIDs ()).thenReturn (upkeeps);
		when (xu.getModifiedUpkeepValue (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD)).thenReturn (5);
		when (xu.getModifiedUpkeepValue (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS)).thenReturn (2);
		
		// Resources we have
		final ResourceValueUtils resources = mock (ResourceValueUtils.class);
		when (resources.calculateAmountPerTurnForProductionType (priv, pub.getPick (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, spellSettings, db)).thenReturn (6);
		when (resources.calculateAmountPerTurnForProductionType (priv, pub.getPick (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, spellSettings, db)).thenReturn (1);
		
		// Set up object to test
		final UnitAIImpl ai = new UnitAIImpl ();
		ai.setUnitUtils (unitUtils);
		ai.setResourceValueUtils (resources);
		
		// Run method
		assertTrue (ai.canAffordUnitMaintenance (player, players, unit, spellSettings, db));

		when (resources.calculateAmountPerTurnForProductionType (priv, pub.getPick (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, spellSettings, db)).thenReturn (4);
		assertFalse (ai.canAffordUnitMaintenance (player, players, unit, spellSettings, db));
	}
}