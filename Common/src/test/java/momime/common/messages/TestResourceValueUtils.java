package momime.common.messages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.CommonDatabaseLookup;
import momime.common.database.GenerateTestData;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.v0_9_4.SpellSettingData;
import momime.common.messages.v0_9_4.MagicPowerDistribution;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomResourceValue;
import momime.common.messages.v0_9_4.PlayerPick;

import org.junit.Test;

/**
 * Tests the ResourceValueUtils class
 */
public final class TestResourceValueUtils
{
	/** Dummy logger to use during unit tests */
	private final Logger debugLogger = Logger.getLogger ("MoMIMECommonUnitTests");

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

		assertEquals ("RE02", ResourceValueUtils.findResourceValue (resourceValues, "RE02").getProductionTypeID ());
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

		assertNull (ResourceValueUtils.findResourceValue (resourceValues, "RE04"));
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

		assertEquals (20, ResourceValueUtils.findAmountStoredForProductionType (resourceValues, "RE02", debugLogger));
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

		assertEquals (0, ResourceValueUtils.findAmountStoredForProductionType (resourceValues, "RE04", debugLogger));
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

		ResourceValueUtils.addToAmountPerTurn (resourceValues, "RE02", 5, debugLogger);

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

		ResourceValueUtils.addToAmountPerTurn (resourceValues, "RE03", 5, debugLogger);

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

		ResourceValueUtils.addToAmountStored (resourceValues, "RE02", 5, debugLogger);

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

		ResourceValueUtils.addToAmountStored (resourceValues, "RE03", 5, debugLogger);

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

		ResourceValueUtils.zeroAmountsPerTurn (resourceValues, debugLogger);

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

		ResourceValueUtils.zeroAmountsStored (resourceValues, debugLogger);

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

		assertEquals (3, ResourceValueUtils.calculateCastingSkillOfPlayer (resourceValues, debugLogger));
	}

	/**
	 * Tests the calculateAmountPerTurnForProductionType method
	 * @throws MomException If there is a problem
	 * @throws RecordNotFoundException If we look for a particular record that we expect to be present in the XML file and we can't find it
	 */
	@Test
	public final void testCalculateAmountPerTurnForProductionType () throws MomException, RecordNotFoundException
	{
		final MomPersistentPlayerPrivateKnowledge privateInfo = new MomPersistentPlayerPrivateKnowledge ();
		final SpellSettingData spellSettings = GenerateTestData.createOriginalSpellSettings ();
		final CommonDatabaseLookup db = GenerateTestData.createDB ();

		// Add some production
		ResourceValueUtils.addToAmountPerTurn (privateInfo.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, 20, debugLogger);
		ResourceValueUtils.addToAmountPerTurn (privateInfo.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, 5, debugLogger);				// Library + Sages' Guild
		ResourceValueUtils.addToAmountPerTurn (privateInfo.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MAGIC_POWER, 24, debugLogger);		// 12 book start

		// Set our magic sliders
		final MagicPowerDistribution powerDist = new MagicPowerDistribution ();
		powerDist.setManaRatio (96);			// 40%
		powerDist.setResearchRatio (96);	// 40%
		powerDist.setSkillRatio (48);			// 20%
		privateInfo.setMagicPowerDistribution (powerDist);

		// 11 Chaos books + Summoner
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		PlayerPickUtils.updatePickQuantity (picks, GenerateTestData.CHAOS_BOOK, 11, debugLogger);
		PlayerPickUtils.updatePickQuantity (picks, GenerateTestData.SUMMONER, 1, debugLogger);

		// Simple read
		assertEquals ("Simple gold read", 20, ResourceValueUtils.calculateAmountPerTurnForProductionType (privateInfo, picks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, spellSettings, db, debugLogger));

		// Would give us +5 gold, but that's dealt with elsewhere
		ResourceValueUtils.addToAmountPerTurn (privateInfo.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, 10, debugLogger);
		assertEquals ("Prove don't get money from selling rations", 20, ResourceValueUtils.calculateAmountPerTurnForProductionType (privateInfo, picks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, spellSettings, db, debugLogger));

		// 20% of 24 = 4.8
		assertEquals ("Unmodified skill calculation", 5, ResourceValueUtils.calculateAmountPerTurnForProductionType (privateInfo, picks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT, spellSettings, db, debugLogger));

		// 5 + (40% of 24) = 5 + 9.6 = 14.6
		assertEquals ("Unmodified research calculation", 15, ResourceValueUtils.calculateAmountPerTurnForProductionType (privateInfo, picks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, spellSettings, db, debugLogger));

		// 24 - 5 - 10 = 9
		assertEquals ("Unmodified mana calculation", 9, ResourceValueUtils.calculateAmountPerTurnForProductionType (privateInfo, picks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, spellSettings, db, debugLogger));

		// Archmage gives +50%, so 5 * 1.5 = 7.5
		PlayerPickUtils.updatePickQuantity (picks, GenerateTestData.ARCHMAGE, 1, debugLogger);
		assertEquals ("Archmage", 7, ResourceValueUtils.calculateAmountPerTurnForProductionType (privateInfo, picks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT, spellSettings, db, debugLogger));

		// Mana focusing gives +25%, so 9 * 1.25 = 11.25
		PlayerPickUtils.updatePickQuantity (picks, GenerateTestData.MANA_FOCUSING, 1, debugLogger);
		assertEquals ("Mana focusing", 11, ResourceValueUtils.calculateAmountPerTurnForProductionType (privateInfo, picks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, spellSettings, db, debugLogger));

		// Prove researching a non-chaos non-summoning spell gets no bonus
		privateInfo.setSpellIDBeingResearched (GenerateTestData.EARTH_TO_MUD);
		assertEquals ("Research with no bonus", 15, ResourceValueUtils.calculateAmountPerTurnForProductionType (privateInfo, picks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, spellSettings, db, debugLogger));

		// Chaos books give +40%, so 15 * 1.4 = 21
		privateInfo.setSpellIDBeingResearched (GenerateTestData.WARP_WOOD);
		assertEquals ("Research with book bonus", 21, ResourceValueUtils.calculateAmountPerTurnForProductionType (privateInfo, picks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, spellSettings, db, debugLogger));

		// Summoner gives +25%, so 15 * 1.25 = 18.75
		privateInfo.setSpellIDBeingResearched (GenerateTestData.GIANT_SPIDERS_SPELL);
		assertEquals ("Research with Summoner bonus", 18, ResourceValueUtils.calculateAmountPerTurnForProductionType (privateInfo, picks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, spellSettings, db, debugLogger));

		// Both combined gives +65%, so 15 * 1.65 = 24.75
		privateInfo.setSpellIDBeingResearched (GenerateTestData.HELL_HOUNDS_SPELL);
		assertEquals ("Research with combined bonus", 24, ResourceValueUtils.calculateAmountPerTurnForProductionType (privateInfo, picks, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, spellSettings, db, debugLogger));
	}
}
