package momime.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import momime.common.MomException;
import momime.common.calculations.SkillCalculationsImpl;
import momime.common.calculations.SpellCalculationsImpl;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.GenerateTestData;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.SpellSettingData;
import momime.common.messages.MagicPowerDistribution;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomResourceValue;
import momime.common.messages.PlayerPick;

import org.junit.Test;

/**
 * Tests the ResourceValueUtils class
 */
public final class TestResourceValueUtilsImpl
{
	/**
	 * Tests the findResourceValue method where the resource value does exist
	 */
	@Test
	public final void testFindResourceValue_Exists ()
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
	public final void testFindResourceValue_NotExists ()
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
	 * Tests the calculateCastingSkillOfPlayer method
	 */
	@Test
	public final void testCalculateCastingSkillOfPlayer ()
	{
		final List<MomResourceValue> resourceValues = new ArrayList<MomResourceValue> ();

		final MomResourceValue skillImprovement = new MomResourceValue ();
		skillImprovement.setProductionTypeID (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT);
		skillImprovement.setAmountStored (10);
		resourceValues.add (skillImprovement);

		final ResourceValueUtilsImpl utils = new ResourceValueUtilsImpl ();
		utils.setSkillCalculations (new SkillCalculationsImpl ());
		assertEquals (3, utils.calculateCastingSkillOfPlayer (resourceValues));
	}

	/**
	 * Tests the calculateAmountPerTurnForProductionType method
	 * @throws MomException If there is a problem
	 * @throws RecordNotFoundException If we look for a particular record that we expect to be present in the XML file and we can't find it
	 */
	@Test
	public final void testCalculateAmountPerTurnForProductionType () throws MomException, RecordNotFoundException
	{
		final ResourceValueUtilsImpl utils = new ResourceValueUtilsImpl ();
		final PlayerPickUtilsImpl playerPickUtils = new PlayerPickUtilsImpl ();
		final SpellCalculationsImpl spellCalculations = new SpellCalculationsImpl ();
		utils.setPlayerPickUtils (playerPickUtils);
		utils.setSpellCalculations (spellCalculations);
		spellCalculations.setSpellUtils (new SpellUtilsImpl ());
		
		final MomPersistentPlayerPrivateKnowledge privateInfo = new MomPersistentPlayerPrivateKnowledge ();
		final SpellSettingData spellSettings = GenerateTestData.createOriginalSpellSettings ();
		final CommonDatabase db = GenerateTestData.createDB ();

		// Add some production
		utils.addToAmountPerTurn (privateInfo.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, 20);
		utils.addToAmountPerTurn (privateInfo.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, 5);				// Library + Sages' Guild
		utils.addToAmountPerTurn (privateInfo.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MAGIC_POWER, 24);		// 12 book start

		// Set our magic sliders
		final MagicPowerDistribution powerDist = new MagicPowerDistribution ();
		powerDist.setManaRatio (96);			// 40%
		powerDist.setResearchRatio (96);	// 40%
		powerDist.setSkillRatio (48);			// 20%
		privateInfo.setMagicPowerDistribution (powerDist);

		// 11 Chaos books + Summoner
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		playerPickUtils.updatePickQuantity (picks, GenerateTestData.CHAOS_BOOK, 11);
		playerPickUtils.updatePickQuantity (picks, GenerateTestData.SUMMONER, 1);

		// Simple read
		assertEquals ("Simple gold read", 20, utils.calculateAmountPerTurnForProductionType (privateInfo, picks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, spellSettings, db));

		// Would give us +5 gold, but that's dealt with elsewhere
		utils.addToAmountPerTurn (privateInfo.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, 10);
		assertEquals ("Prove don't get money from selling rations", 20, utils.calculateAmountPerTurnForProductionType (privateInfo, picks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, spellSettings, db));

		// 20% of 24 = 4.8
		assertEquals ("Unmodified skill calculation", 4, utils.calculateAmountPerTurnForProductionType (privateInfo, picks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT, spellSettings, db));

		// 5 + (40% of 24) = 5 + 9.6 = 14.6
		assertEquals ("Unmodified research calculation", 14, utils.calculateAmountPerTurnForProductionType (privateInfo, picks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, spellSettings, db));

		// 24 - 5 - 10 = 9
		assertEquals ("Unmodified mana calculation", 9, utils.calculateAmountPerTurnForProductionType (privateInfo, picks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, spellSettings, db));

		// Archmage gives +50%, so 4 * 1.5 = 6
		playerPickUtils.updatePickQuantity (picks, GenerateTestData.ARCHMAGE, 1);
		assertEquals ("Archmage", 6, utils.calculateAmountPerTurnForProductionType (privateInfo, picks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT, spellSettings, db));

		// Mana focusing gives +25%, so 9 * 1.25 = 11.25
		playerPickUtils.updatePickQuantity (picks, GenerateTestData.MANA_FOCUSING, 1);
		assertEquals ("Mana focusing", 11, utils.calculateAmountPerTurnForProductionType (privateInfo, picks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, spellSettings, db));

		// Prove researching a non-chaos non-summoning spell gets no bonus
		privateInfo.setSpellIDBeingResearched (GenerateTestData.EARTH_TO_MUD);
		assertEquals ("Research with no bonus", 14, utils.calculateAmountPerTurnForProductionType (privateInfo, picks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, spellSettings, db));

		// Chaos books give +40%, so 14 * 1.4 = 19
		privateInfo.setSpellIDBeingResearched (GenerateTestData.WARP_WOOD);
		assertEquals ("Research with book bonus", 19, utils.calculateAmountPerTurnForProductionType (privateInfo, picks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, spellSettings, db));

		// Summoner gives +25%, so 14 * 1.25 = 17.5
		privateInfo.setSpellIDBeingResearched (GenerateTestData.GIANT_SPIDERS_SPELL);
		assertEquals ("Research with Summoner bonus", 17, utils.calculateAmountPerTurnForProductionType (privateInfo, picks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, spellSettings, db));

		// Both combined gives +65%, so 14 * 1.65 = 23.1
		privateInfo.setSpellIDBeingResearched (GenerateTestData.HELL_HOUNDS_SPELL);
		assertEquals ("Research with combined bonus", 23, utils.calculateAmountPerTurnForProductionType (privateInfo, picks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, spellSettings, db));
	}
}