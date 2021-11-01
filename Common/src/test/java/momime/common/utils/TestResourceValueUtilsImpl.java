package momime.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

import momime.common.MomException;
import momime.common.calculations.SkillCalculationsImpl;
import momime.common.calculations.SpellCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.database.Pick;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellSetting;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MagicPowerDistribution;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomResourceValue;
import momime.common.messages.PlayerPick;
import momime.common.messages.UnitStatusID;

/**
 * Tests the ResourceValueUtils class
 */
public final class TestResourceValueUtilsImpl
{
	/**
	 * Tests the findResourceValue method where the resource value does exist
	 */
	@Test
	public final void testFindResourceExists ()
	{
		final List<MomResourceValue> resourceValues = new ArrayList<MomResourceValue> ();
		for (int n = 1; n <= 3; n++)
		{
			final MomResourceValue newValue = new MomResourceValue ();
			newValue.setProductionTypeID ("RE0" + n);
			resourceValues.add (newValue);
		}

		final ResourceValueUtilsImpl utils = new ResourceValueUtilsImpl ();
		assertEquals ("RE02", utils.findResourceValue (resourceValues, "RE02").getProductionTypeID ());
	}

	/**
	 * Tests the findResourceValue method where the resource value doesn't exist
	 */
	@Test
	public final void testFindResourceNotExists ()
	{
		final List<MomResourceValue> resourceValues = new ArrayList<MomResourceValue> ();
		for (int n = 1; n <= 3; n++)
		{
			final MomResourceValue newValue = new MomResourceValue ();
			newValue.setProductionTypeID ("RE0" + n);
			resourceValues.add (newValue);
		}

		final ResourceValueUtilsImpl utils = new ResourceValueUtilsImpl ();
		assertNull (utils.findResourceValue (resourceValues, "RE04"));
	}

	/**
	 * Tests the findAmountPerTurnForProductionType method where the resource value does exist
	 */
	@Test
	public final void testFindAmountPerTurnForProductionType_Exists ()
	{
		final List<MomResourceValue> resourceValues = new ArrayList<MomResourceValue> ();
		for (int n = 1; n <= 3; n++)
		{
			final MomResourceValue newValue = new MomResourceValue ();
			newValue.setProductionTypeID ("RE0" + n);
			newValue.setAmountPerTurn (n * 10);
			resourceValues.add (newValue);
		}

		final ResourceValueUtilsImpl utils = new ResourceValueUtilsImpl ();
		assertEquals (20, utils.findAmountPerTurnForProductionType (resourceValues, "RE02"));
	}

	/**
	 * Tests the findAmountPerTurnForProductionType method where the resource value doesn't exist
	 */
	@Test
	public final void testFindAmountPerTurnForProductionType_NotExists ()
	{
		final List<MomResourceValue> resourceValues = new ArrayList<MomResourceValue> ();
		for (int n = 1; n <= 3; n++)
		{
			final MomResourceValue newValue = new MomResourceValue ();
			newValue.setProductionTypeID ("RE0" + n);
			newValue.setAmountPerTurn (n * 10);
			resourceValues.add (newValue);
		}

		final ResourceValueUtilsImpl utils = new ResourceValueUtilsImpl ();
		assertEquals (0, utils.findAmountPerTurnForProductionType (resourceValues, "RE04"));
	}

	/**
	 * Tests the findAmountStoredForProductionType method where the resource value does exist
	 */
	@Test
	public final void testFindAmountStoredForProductionType_Exists ()
	{
		final List<MomResourceValue> resourceValues = new ArrayList<MomResourceValue> ();
		for (int n = 1; n <= 3; n++)
		{
			final MomResourceValue newValue = new MomResourceValue ();
			newValue.setProductionTypeID ("RE0" + n);
			newValue.setAmountStored (n * 10);
			resourceValues.add (newValue);
		}

		final ResourceValueUtilsImpl utils = new ResourceValueUtilsImpl ();
		assertEquals (20, utils.findAmountStoredForProductionType (resourceValues, "RE02"));
	}

	/**
	 * Tests the findAmountStoredForProductionType method where the resource value doesn't exist
	 */
	@Test
	public final void testFindAmountStoredForProductionType_NotExists ()
	{
		final List<MomResourceValue> resourceValues = new ArrayList<MomResourceValue> ();
		for (int n = 1; n <= 3; n++)
		{
			final MomResourceValue newValue = new MomResourceValue ();
			newValue.setProductionTypeID ("RE0" + n);
			newValue.setAmountStored (n * 10);
			resourceValues.add (newValue);
		}

		final ResourceValueUtilsImpl utils = new ResourceValueUtilsImpl ();
		assertEquals (0, utils.findAmountStoredForProductionType (resourceValues, "RE04"));
	}

	/**
	 * Tests the addToAmountPerTurn method where the production type is already in the list
	 */
	@Test
	public final void testAddToAmountPerTurn_Exists ()
	{
		final List<MomResourceValue> resourceValues = new ArrayList<MomResourceValue> ();
		for (int n = 1; n <= 2; n++)
		{
			final MomResourceValue newValue = new MomResourceValue ();
			newValue.setProductionTypeID ("RE0" + n);
			newValue.setAmountPerTurn (n * 10);
			newValue.setAmountStored (n * 100);
			resourceValues.add (newValue);
		}

		final ResourceValueUtilsImpl utils = new ResourceValueUtilsImpl ();
		utils.addToAmountPerTurn (resourceValues, "RE02", 5);

		assertEquals (2, resourceValues.size ());
		assertEquals ("RE01", resourceValues.get (0).getProductionTypeID ());
		assertEquals (10, resourceValues.get (0).getAmountPerTurn ());
		assertEquals (100, resourceValues.get (0).getAmountStored ());
		assertEquals ("RE02", resourceValues.get (1).getProductionTypeID ());
		assertEquals (25, resourceValues.get (1).getAmountPerTurn ());
		assertEquals (200, resourceValues.get (1).getAmountStored ());
	}

	/**
	 * Tests the addToAmountPerTurn method where the production type isn't already in the list
	 */
	@Test
	public final void testAddToAmountPerTurn_NotExists ()
	{
		final List<MomResourceValue> resourceValues = new ArrayList<MomResourceValue> ();
		for (int n = 1; n <= 2; n++)
		{
			final MomResourceValue newValue = new MomResourceValue ();
			newValue.setProductionTypeID ("RE0" + n);
			newValue.setAmountPerTurn (n * 10);
			newValue.setAmountStored (n * 100);
			resourceValues.add (newValue);
		}

		final ResourceValueUtilsImpl utils = new ResourceValueUtilsImpl ();
		utils.addToAmountPerTurn (resourceValues, "RE03", 5);

		assertEquals (3, resourceValues.size ());
		assertEquals ("RE01", resourceValues.get (0).getProductionTypeID ());
		assertEquals (10, resourceValues.get (0).getAmountPerTurn ());
		assertEquals (100, resourceValues.get (0).getAmountStored ());
		assertEquals ("RE02", resourceValues.get (1).getProductionTypeID ());
		assertEquals (20, resourceValues.get (1).getAmountPerTurn ());
		assertEquals (200, resourceValues.get (1).getAmountStored ());
		assertEquals ("RE03", resourceValues.get (2).getProductionTypeID ());
		assertEquals (5, resourceValues.get (2).getAmountPerTurn ());
		assertEquals (0, resourceValues.get (2).getAmountStored ());
	}

	/**
	 * Tests the addToAmountStored method where the production type is already in the list
	 */
	@Test
	public final void testAddToAmountStored_Exists ()
	{
		final List<MomResourceValue> resourceValues = new ArrayList<MomResourceValue> ();
		for (int n = 1; n <= 2; n++)
		{
			final MomResourceValue newValue = new MomResourceValue ();
			newValue.setProductionTypeID ("RE0" + n);
			newValue.setAmountPerTurn (n * 10);
			newValue.setAmountStored (n * 100);
			resourceValues.add (newValue);
		}

		final ResourceValueUtilsImpl utils = new ResourceValueUtilsImpl ();
		utils.addToAmountStored (resourceValues, "RE02", 5);

		assertEquals (2, resourceValues.size ());
		assertEquals ("RE01", resourceValues.get (0).getProductionTypeID ());
		assertEquals (10, resourceValues.get (0).getAmountPerTurn ());
		assertEquals (100, resourceValues.get (0).getAmountStored ());
		assertEquals ("RE02", resourceValues.get (1).getProductionTypeID ());
		assertEquals (20, resourceValues.get (1).getAmountPerTurn ());
		assertEquals (205, resourceValues.get (1).getAmountStored ());
	}

	/**
	 * Tests the addToAmountStored method where the production type isn't already in the list
	 */
	@Test
	public final void testAddToAmountStored_NotExists ()
	{
		final List<MomResourceValue> resourceValues = new ArrayList<MomResourceValue> ();
		for (int n = 1; n <= 2; n++)
		{
			final MomResourceValue newValue = new MomResourceValue ();
			newValue.setProductionTypeID ("RE0" + n);
			newValue.setAmountPerTurn (n * 10);
			newValue.setAmountStored (n * 100);
			resourceValues.add (newValue);
		}

		final ResourceValueUtilsImpl utils = new ResourceValueUtilsImpl ();
		utils.addToAmountStored (resourceValues, "RE03", 5);

		assertEquals (3, resourceValues.size ());
		assertEquals ("RE01", resourceValues.get (0).getProductionTypeID ());
		assertEquals (10, resourceValues.get (0).getAmountPerTurn ());
		assertEquals (100, resourceValues.get (0).getAmountStored ());
		assertEquals ("RE02", resourceValues.get (1).getProductionTypeID ());
		assertEquals (20, resourceValues.get (1).getAmountPerTurn ());
		assertEquals (200, resourceValues.get (1).getAmountStored ());
		assertEquals ("RE03", resourceValues.get (2).getProductionTypeID ());
		assertEquals (0, resourceValues.get (2).getAmountPerTurn ());
		assertEquals (5, resourceValues.get (2).getAmountStored ());
	}

	/**
	 * Tests the zeroAmountsPerTurn method
	 */
	@Test
	public final void testZeroAmountsPerTurn ()
	{
		final List<MomResourceValue> resourceValues = new ArrayList<MomResourceValue> ();
		for (int n = 1; n <= 2; n++)
		{
			final MomResourceValue newValue = new MomResourceValue ();
			newValue.setProductionTypeID ("RE0" + n);
			newValue.setAmountPerTurn (n * 10);
			newValue.setAmountStored (n * 100);
			resourceValues.add (newValue);
		}

		final ResourceValueUtilsImpl utils = new ResourceValueUtilsImpl ();
		utils.zeroAmountsPerTurn (resourceValues);

		assertEquals (2, resourceValues.size ());
		assertEquals ("RE01", resourceValues.get (0).getProductionTypeID ());
		assertEquals (0, resourceValues.get (0).getAmountPerTurn ());
		assertEquals (100, resourceValues.get (0).getAmountStored ());
		assertEquals ("RE02", resourceValues.get (1).getProductionTypeID ());
		assertEquals (0, resourceValues.get (1).getAmountPerTurn ());
		assertEquals (200, resourceValues.get (1).getAmountStored ());
	}

	/**
	 * Tests the zeroAmountsStored method
	 */
	@Test
	public final void testZeroAmountsStored ()
	{
		final List<MomResourceValue> resourceValues = new ArrayList<MomResourceValue> ();
		for (int n = 1; n <= 2; n++)
		{
			final MomResourceValue newValue = new MomResourceValue ();
			newValue.setProductionTypeID ("RE0" + n);
			newValue.setAmountPerTurn (n * 10);
			newValue.setAmountStored (n * 100);
			resourceValues.add (newValue);
		}

		final ResourceValueUtilsImpl utils = new ResourceValueUtilsImpl ();
		utils.zeroAmountsStored (resourceValues);

		assertEquals (2, resourceValues.size ());
		assertEquals ("RE01", resourceValues.get (0).getProductionTypeID ());
		assertEquals (10, resourceValues.get (0).getAmountPerTurn ());
		assertEquals (0, resourceValues.get (0).getAmountStored ());
		assertEquals ("RE02", resourceValues.get (1).getProductionTypeID ());
		assertEquals (20, resourceValues.get (1).getAmountPerTurn ());
		assertEquals (0, resourceValues.get (1).getAmountStored ());
	}

	/**
	 * Tests the calculateBasicCastingSkill method
	 */
	@Test
	public final void testCalculateBasicCastingSkill ()
	{
		// Casting skill points
		final List<MomResourceValue> resourceValues = new ArrayList<MomResourceValue> ();

		final MomResourceValue skillImprovement = new MomResourceValue ();
		skillImprovement.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT);
		skillImprovement.setAmountStored (10);
		resourceValues.add (skillImprovement);

		// Set up object to test
		final ResourceValueUtilsImpl utils = new ResourceValueUtilsImpl ();
		utils.setSkillCalculations (new SkillCalculationsImpl ());
		
		// Run method
		assertEquals (3, utils.calculateBasicCastingSkill (resourceValues));
	}

	/**
	 * Tests the calculateModifiedCastingSkill method when there are no bonuses
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateModifiedCastingSkill_Basic () throws Exception
	{
		// Casting skill points
		final List<MomResourceValue> resourceValues = new ArrayList<MomResourceValue> ();

		final MomResourceValue skillImprovement = new MomResourceValue ();
		skillImprovement.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT);
		skillImprovement.setAmountStored (10);
		resourceValues.add (skillImprovement);
		
		// Player picks
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		
		final PlayerPublicDetails playerDetails = new PlayerPublicDetails (pd, pub, null);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		// Units
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		// Wizard's Fortress
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Set up object to test
		final ResourceValueUtilsImpl utils = new ResourceValueUtilsImpl ();
		utils.setSkillCalculations (new SkillCalculationsImpl ());
		utils.setMemoryBuildingUtils (memoryBuildingUtils);
		
		// Run method
		assertEquals (3, utils.calculateModifiedCastingSkill (resourceValues, playerDetails, players, mem, db, true));
	}
	
	/**
	 * Tests the calculateModifiedCastingSkill method when there are both types of bonuses
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateModifiedCastingSkill_Modified () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		for (int n = 1; n <= 3; n++)
		{
			final Pick pickDef = new Pick ();
			pickDef.setPickID ("RT0" + n);
			
			if (n == 2)
				pickDef.setDynamicSkillBonus (10);
			
			when (db.findPick (pickDef.getPickID (), "calculateModifiedCastingSkill")).thenReturn (pickDef);
		}
		
		// Casting skill points
		final List<MomResourceValue> resourceValues = new ArrayList<MomResourceValue> ();

		final MomResourceValue skillImprovement = new MomResourceValue ();
		skillImprovement.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT);
		skillImprovement.setAmountStored (10);
		resourceValues.add (skillImprovement);
		
		// Player picks
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		
		final PlayerPublicDetails playerDetails = new PlayerPublicDetails (pd, pub, null);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		for (int n = 1; n <= 3; n++)
		{
			final PlayerPick pick = new PlayerPick ();
			pick.setPickID ("RT0" + n);
			pick.setQuantity (1);
			pub.getPick ().add (pick);
		}
		
		// Units
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MemoryUnit hero = new MemoryUnit ();
		hero.setStatus (UnitStatusID.ALIVE);
		hero.setOwningPlayerID (2);
		hero.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		
		mem.getUnit ().add (hero);
		
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_HERO)).thenReturn (true);
		when (xu.calculateManaTotal ()).thenReturn (15);
		when (expand.expandUnitDetails (hero, null, null, null, players, mem, db)).thenReturn (xu);
		
		// Wizard's Fortress
		final MemoryBuilding fortress = new MemoryBuilding ();
		fortress.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		when (memoryBuildingUtils.findCityWithBuilding (2, CommonDatabaseConstants.BUILDING_FORTRESS, mem.getMap (), mem.getBuilding ())).thenReturn (fortress);
		
		// Set up object to test
		final ResourceValueUtilsImpl utils = new ResourceValueUtilsImpl ();
		utils.setSkillCalculations (new SkillCalculationsImpl ());
		utils.setMemoryBuildingUtils (memoryBuildingUtils);
		utils.setExpandUnitDetails (expand);
		
		// Run method
		assertEquals (3 + 10 + 7, utils.calculateModifiedCastingSkill (resourceValues, playerDetails, players, mem, db, true));
	}
	
	/**
	 * Tests the calculateResearchFromUnits method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateResearchFromUnits () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Players
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		// Units
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MemoryUnit hero = new MemoryUnit ();
		hero.setStatus (UnitStatusID.ALIVE);
		hero.setOwningPlayerID (2);
		
		mem.getUnit ().add (hero);

		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_SAGE)).thenReturn (true);
		when (xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_SAGE)).thenReturn (2);
		when (expand.expandUnitDetails (hero, null, null, null, players, mem, db)).thenReturn (xu);
		
		// Level 2 hero with super sage
		final ExperienceLevel captain = new ExperienceLevel ();
		captain.setLevelNumber (2);
		when (xu.getModifiedExperienceLevel ()).thenReturn (captain);
		
		// Set up object to test
		final ResourceValueUtilsImpl utils = new ResourceValueUtilsImpl ();
		utils.setExpandUnitDetails (expand);
		
		// Run method
		assertEquals (13, utils.calculateResearchFromUnits (2, players, mem, db));
	}
	
	/**
	 * Tests the CalculateBasicFame method
	 */
	@Test
	public final void testCalculateBasicFame ()
	{
		// Accumulated fame
		final List<MomResourceValue> resourceValues = new ArrayList<MomResourceValue> ();

		final MomResourceValue fame = new MomResourceValue ();
		fame.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FAME);
		fame.setAmountStored (3);
		resourceValues.add (fame);

		// Set up object to test
		final ResourceValueUtilsImpl utils = new ResourceValueUtilsImpl ();
		
		// Run method
		assertEquals (3, utils.calculateBasicFame (resourceValues));
	}
	
	/**
	 * Tests the CalculateBasicFame method when there are no bonuses
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateModifiedFame_Basic () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Accumulated fame
		final List<MomResourceValue> resourceValues = new ArrayList<MomResourceValue> ();

		final MomResourceValue fame = new MomResourceValue ();
		fame.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FAME);
		fame.setAmountStored (3);
		resourceValues.add (fame);

		// Player picks
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		
		final PlayerPublicDetails playerDetails = new PlayerPublicDetails (pd, pub, null);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		// Spells
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);
		
		// Units
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		// Set up object to test
		final ResourceValueUtilsImpl utils = new ResourceValueUtilsImpl ();
		utils.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		
		// Run method
		assertEquals (3, utils.calculateModifiedFame (resourceValues, playerDetails, players, mem, db));
	}
	
	/**
	 * Tests the CalculateBasicFame method when all the bonuses apply
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateModifiedFame_Modified () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		for (int n = 1; n <= 3; n++)
		{
			final Pick pickDef = new Pick ();
			pickDef.setPickID ("RT0" + n);
			
			if (n == 2)
				pickDef.setDynamicFameBonus (10);
			
			when (db.findPick (pickDef.getPickID (), "calculateModifiedFame")).thenReturn (pickDef);
		}
		
		// Accumulated fame
		final List<MomResourceValue> resourceValues = new ArrayList<MomResourceValue> ();

		final MomResourceValue fame = new MomResourceValue ();
		fame.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FAME);
		fame.setAmountStored (3);
		resourceValues.add (fame);

		// Player picks
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		
		final PlayerPublicDetails playerDetails = new PlayerPublicDetails (pd, pub, null);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		for (int n = 1; n <= 3; n++)
		{
			final PlayerPick pick = new PlayerPick ();
			pick.setPickID ("RT0" + n);
			pick.setQuantity (1);
			pub.getPick ().add (pick);
		}

		// Units
		final FogOfWarMemory mem = new FogOfWarMemory ();

		final MemoryUnit hero = new MemoryUnit ();
		hero.setStatus (UnitStatusID.ALIVE);
		hero.setOwningPlayerID (2);
		
		mem.getUnit ().add (hero);

		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_LEGENDARY)).thenReturn (true);
		when (xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_LEGENDARY)).thenReturn (2);
		when (expand.expandUnitDetails (hero, null, null, null, players, mem, db)).thenReturn (xu);
		
		// Level 2 hero with super legendary
		final ExperienceLevel captain = new ExperienceLevel ();
		captain.setLevelNumber (2);
		when (xu.getModifiedExperienceLevel ()).thenReturn (captain);
		
		// Spells
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);
		when (memoryMaintainedSpellUtils.findMaintainedSpell (mem.getMaintainedSpell (), 2,
			CommonDatabaseConstants.SPELL_ID_JUST_CAUSE, null, null, null, null)).thenReturn (new MemoryMaintainedSpell ());
		
		// Set up object to test
		final ResourceValueUtilsImpl utils = new ResourceValueUtilsImpl ();
		utils.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		utils.setExpandUnitDetails (expand);
		
		// Run method
		assertEquals (3 + 10 + 10 + 13, utils.calculateModifiedFame (resourceValues, playerDetails, players, mem, db));
	}
	
	/**
	 * Tests the calculateAmountPerTurnForProductionType method
	 * @throws MomException If there is a problem
	 * @throws RecordNotFoundException If we look for a particular record that we expect to be present in the XML file and we can't find it
	 */
	@Test
	public final void testCalculateAmountPerTurnForProductionType () throws MomException, RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final Spell spellDef = new Spell ();
		spellDef.setSpellRealm ("MB01");
		when (db.findSpell ("SP001", "calculateAmountPerTurnForProductionType")).thenReturn (spellDef);
		
		// Our bonus from a number of books
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		final SpellSetting spellSettings = new SpellSetting ();

		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		when (playerPickUtils.getQuantityOfPick (picks, "MB01")).thenReturn (4);
		when (playerPickUtils.getQuantityOfPick (picks, "MB02")).thenReturn (6);
		
		final SpellCalculations spellCalculations = mock (SpellCalculations.class);
		when (spellCalculations.calculateResearchBonus (4, spellSettings, spellDef, picks, db)).thenReturn (0d);
		when (spellCalculations.calculateResearchBonus (6, spellSettings, spellDef, picks, db)).thenReturn (25d);

		// Set up data
		final MomPersistentPlayerPrivateKnowledge privateInfo = new MomPersistentPlayerPrivateKnowledge ();
		
		// Set up object to test
		final ResourceValueUtilsImpl utils = new ResourceValueUtilsImpl ();
		utils.setPlayerPickUtils (playerPickUtils);
		utils.setSpellCalculations (spellCalculations);
		
		// Gold is just a simple read
		final MomResourceValue goldPerTurn = new MomResourceValue ();
		goldPerTurn.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		goldPerTurn.setAmountPerTurn (20);
		privateInfo.getResourceValue ().add (goldPerTurn);
		
		assertEquals (20, utils.calculateAmountPerTurnForProductionType (privateInfo, picks, CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, spellSettings, db));
		
		// Mana and skill are a simple split from magic power
		final MomResourceValue magicPowerPerTurn = new MomResourceValue ();
		magicPowerPerTurn.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER);
		magicPowerPerTurn.setAmountPerTurn (60);
		privateInfo.getResourceValue ().add (magicPowerPerTurn);
		
		final MagicPowerDistribution powerDist = new MagicPowerDistribution ();
		powerDist.setManaRatio (96);			// 40%
		powerDist.setResearchRatio (96);	// 40%
		powerDist.setSkillRatio (48);			// 20%
		privateInfo.setMagicPowerDistribution (powerDist);

		assertEquals (24, utils.calculateAmountPerTurnForProductionType (privateInfo, picks, CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, spellSettings, db));
		assertEquals (12, utils.calculateAmountPerTurnForProductionType (privateInfo, picks, CommonDatabaseConstants.PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT, spellSettings, db));
		
		// Research when we have nothing specific being researched
		assertEquals (24, utils.calculateAmountPerTurnForProductionType (privateInfo, picks, CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH, spellSettings, db));
		
		// Research something in magic realm where we don't get a bonus
		privateInfo.setSpellIDBeingResearched ("SP001"); 
		assertEquals (24, utils.calculateAmountPerTurnForProductionType (privateInfo, picks, CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH, spellSettings, db));
		
		// Research something in magic realm where we do get a bonus
		spellDef.setSpellRealm ("MB02"); 
		assertEquals (24+6, utils.calculateAmountPerTurnForProductionType (privateInfo, picks, CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH, spellSettings, db));
	}
}