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

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItemBonusStat;
import momime.common.database.RecordNotFoundException;
import momime.common.database.SpellSetting;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitStatusID;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.UnitUtils;
import momime.common.utils.UnitUtilsImpl;
import momime.server.ServerTestData;
import momime.server.database.AiUnitCategorySvr;
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
	
	/**
	 * Tests the unitMatchesCategory method in the simplest case where the category specifies no criteria
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUnitMatchesCategory_NoCriteria () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Category
		final AiUnitCategorySvr category = new AiUnitCategorySvr ();
		
		// Unit
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		
		// Fog of war memory
		final FogOfWarMemory mem = new FogOfWarMemory ();

		// Set up object to test
		final UnitAIImpl ai = new UnitAIImpl ();
		
		// Run method
		assertTrue (ai.unitMatchesCategory (xu, category, mem, db));
	}

	/**
	 * Tests the unitMatchesCategory method where the criteria requires the unit to have a certain skill
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUnitMatchesCategory_UnitSkill () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Category
		final AiUnitCategorySvr category = new AiUnitCategorySvr ();
		category.setUnitSkillID ("US001");
		
		// Unit
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		
		// Fog of war memory
		final FogOfWarMemory mem = new FogOfWarMemory ();

		// Set up object to test
		final UnitAIImpl ai = new UnitAIImpl ();
		
		// Run method
		assertFalse (ai.unitMatchesCategory (xu, category, mem, db));
		when (xu.hasModifiedSkill ("US001")).thenReturn (true);
		assertTrue (ai.unitMatchesCategory (xu, category, mem, db));
	}

	/**
	 * Tests the unitMatchesCategory method where the criteria requires that the unit is a transport
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUnitMatchesCategory_Transport () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);

		final UnitSvr unitDef = new UnitSvr ();
		
		// Category
		final AiUnitCategorySvr category = new AiUnitCategorySvr ();
		category.setTransport (true);
		
		// Unit
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getUnitDefinition ()).thenReturn (unitDef);
		
		// Fog of war memory
		final FogOfWarMemory mem = new FogOfWarMemory ();

		// Set up object to test
		final UnitAIImpl ai = new UnitAIImpl ();
		
		// Run method
		assertFalse (ai.unitMatchesCategory (xu, category, mem, db));
		unitDef.setTransportCapacity (1);
		assertTrue (ai.unitMatchesCategory (xu, category, mem, db));
	}

	/**
	 * Tests the unitMatchesCategory method where the criteria requires the unit to be able to pass over all terrain (swimmer, flyer, non-corporeal)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUnitMatchesCategory_AllTerrain () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Category
		final AiUnitCategorySvr category = new AiUnitCategorySvr ();
		category.setAllTerrainPassable (true);
		
		// Unit
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		
		// Fog of war memory
		final FogOfWarMemory mem = new FogOfWarMemory ();

		// Set up object to test
		final UnitCalculations calc = mock (UnitCalculations.class);
		
		final UnitAIImpl ai = new UnitAIImpl ();
		ai.setUnitCalculations (calc);
		
		// Run method
		assertFalse (ai.unitMatchesCategory (xu, category, mem, db));
		when (calc.areAllTerrainTypesPassable (xu, xu.listModifiedSkillIDs (), db)).thenReturn (true);
		assertTrue (ai.unitMatchesCategory (xu, category, mem, db));
	}

	/**
	 * Tests the unitMatchesCategory method where the criteria requires the unit be in a transport
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUnitMatchesCategory_InTransport () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final UnitSvr nonTransportDef = new UnitSvr ();
		when (db.findUnit ("UN001", "unitMatchesCategory")).thenReturn (nonTransportDef);
		
		final UnitSvr transportDef = new UnitSvr ();
		transportDef.setTransportCapacity (1);
		when (db.findUnit ("UN002", "unitMatchesCategory")).thenReturn (transportDef);
		
		// Category
		final AiUnitCategorySvr category = new AiUnitCategorySvr ();
		category.setInTransport (true);
		
		// Unit
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getUnitLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 0));
		when (xu.getOwningPlayerID ()).thenReturn (3);
		
		// Fog of war memory
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells terrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (terrain);
		
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);

		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		when (memoryGridCellUtils.convertNullTileTypeToFOW (terrainData)).thenReturn ("TT01");

		// Set up object to test
		final UnitCalculations calc = mock (UnitCalculations.class);
		
		final UnitAIImpl ai = new UnitAIImpl ();
		ai.setUnitCalculations (calc);
		ai.setMemoryGridCellUtils (memoryGridCellUtils);
		
		// Unit can move over this terrain, it doesn't need a transport
		when (calc.calculateDoubleMovementToEnterTileType (xu, xu.listModifiedSkillIDs (), "TT01", db)).thenReturn (2);
		assertFalse (ai.unitMatchesCategory (xu, category, mem, db));
		
		// Unit can't move over this terrain, but it can't find a transport to get in either
		when (calc.calculateDoubleMovementToEnterTileType (xu, xu.listModifiedSkillIDs (), "TT01", db)).thenReturn (null);
		
		final MemoryUnit nonTransport = new MemoryUnit ();
		nonTransport.setOwningPlayerID (3);
		nonTransport.setStatus (UnitStatusID.ALIVE);
		nonTransport.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
		nonTransport.setUnitID ("UN001");
		mem.getUnit ().add (nonTransport);
		
		assertFalse (ai.unitMatchesCategory (xu, category, mem, db));
		
		// Add a transport
		final MemoryUnit transport = new MemoryUnit ();
		transport.setOwningPlayerID (3);
		transport.setStatus (UnitStatusID.ALIVE);
		transport.setUnitLocation (new MapCoordinates3DEx (20, 10, 0));
		transport.setUnitID ("UN002");
		mem.getUnit ().add (transport);

		assertTrue (ai.unitMatchesCategory (xu, category, mem, db));
	}
}