package momime.common.calculations;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import momime.common.MomException;
import momime.common.database.ICommonDatabase;
import momime.common.database.GenerateTestData;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.v0_9_4.CastingReductionCombination;
import momime.common.database.newgame.v0_9_4.SpellSettingData;
import momime.common.database.newgame.v0_9_4.SwitchResearch;
import momime.common.messages.v0_9_4.PlayerPick;

import org.junit.Test;

/**
 * Tests the calculations in the MomSpellCalculations class
 */
public final class TestMomSpellCalculations
{
	/** Dummy logger to use during unit tests */
	private final Logger debugLogger = Logger.getLogger ("MoMIMECommonUnitTests");

	/** How much we tolerate floating point results to be wrong by because of rounding errors */
	private static final double DOUBLE_TOLERANCE = 0.0000000000001;

	/**
	 * @return None of the pre-defined spell settings use multiplicative for research, so this creates special test settings with 8% per book multiplicative, 1000% cap
	 */
	private final SpellSettingData createSpecialSpellSettings ()
	{
		final SpellSettingData settings = new SpellSettingData ();
		settings.setSwitchResearch (SwitchResearch.FREE);
		settings.setSpellBooksToObtainFirstReduction (8);
		settings.setSpellBooksCastingReduction (8);
		settings.setSpellBooksCastingReductionCap (90);
		settings.setSpellBooksCastingReductionCombination (CastingReductionCombination.MULTIPLICATIVE);
		settings.setSpellBooksResearchBonus (8);
		settings.setSpellBooksResearchBonusCap (1000);
		settings.setSpellBooksResearchBonusCombination (CastingReductionCombination.MULTIPLICATIVE);
		return settings;
	}

	/**
	 * @return Sample player picks - Tauron gets 10 Chaos books + Chaos mastery
	 */
	private final List<PlayerPick> createPlayerPicks ()
	{
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();

		final PlayerPick chaosBooks = new PlayerPick ();
		chaosBooks.setPickID (GenerateTestData.CHAOS_BOOK);
		chaosBooks.setQuantity (10);
		picks.add (chaosBooks);

		final PlayerPick chaosMastery = new PlayerPick ();
		chaosMastery.setPickID (GenerateTestData.CHAOS_MASTERY);
		chaosMastery.setQuantity (1);
		picks.add (chaosMastery);

		return picks;
	}

	/**
	 * @param picks Pick list to add to
	 * @param pickID ID of retort to add to list
	 */
	private final void addRetort (final List<PlayerPick> picks, final String pickID)
	{
		final PlayerPick retort = new PlayerPick ();
		retort.setPickID (pickID);
		retort.setQuantity (1);
		picks.add (retort);
	}

	/**
	 * Tests the calculateCastingCostReduction method with adding bonuses together
	 * @throws MomException If there is a problem
	 * @throws RecordNotFoundException If we encounter a record that can't be found in the DB
	 */
	@Test
	public final void testCalculateCastingCostReductionAdditive () throws MomException, RecordNotFoundException
	{
		final SpellSettingData spellSettings = GenerateTestData.createOriginalSpellSettings ();
		final List<PlayerPick> picks = createPlayerPicks ();
		final ICommonDatabase db = GenerateTestData.createDB ();

		// Tests for different spells and whether we pass in the retort list or not
		assertEquals ("10 books at standard settings should give 3x casting cost reduction", 30, MomSpellCalculations.calculateCastingCostReduction (10, spellSettings, null, null, db, debugLogger), DOUBLE_TOLERANCE);

		assertEquals ("Chaos mastery shouldn't apply when we don't specify a spell", 30, MomSpellCalculations.calculateCastingCostReduction (10, spellSettings, null, picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Normal, no extra retorts", 0, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createArcaneNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Summoning, no extra retorts", 0, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createArcaneSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Normal, no extra retorts", 0, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createNatureNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Summoning, no extra retorts", 0, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createNatureSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Normal, no extra retorts", 45, MomSpellCalculations.calculateCastingCostReduction (10, spellSettings, GenerateTestData.createChaosNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Summoning, no extra retorts", 45, MomSpellCalculations.calculateCastingCostReduction (10, spellSettings, GenerateTestData.createChaosSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);

		// Add in Summoner (25% bonus to summoning only)
		addRetort (picks, GenerateTestData.SUMMONER);
		assertEquals ("Summoner shouldn't apply when we don't specify a spell", 30, MomSpellCalculations.calculateCastingCostReduction (10, spellSettings, null, picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Normal, Summoner", 0, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createArcaneNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Summoning, Summoner", 25, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createArcaneSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Normal, Summoner", 0, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createNatureNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Summoning, Summoner", 25, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createNatureSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Normal, Summoner", 45, MomSpellCalculations.calculateCastingCostReduction (10, spellSettings, GenerateTestData.createChaosNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Summoning, Summoner", 70, MomSpellCalculations.calculateCastingCostReduction (10, spellSettings, GenerateTestData.createChaosSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);

		// Add in Runemaster (25% bonus to Arcane)
		addRetort (picks, GenerateTestData.RUNEMASTER);
		assertEquals ("Runemaster shouldn't apply when we don't specify a spell", 30, MomSpellCalculations.calculateCastingCostReduction (10, spellSettings, null, picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Normal, Summoner + Runemaster", 25, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createArcaneNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Summoning, Summoner + Runemaster", 50, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createArcaneSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Normal, Summoner + Runemaster", 0, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createNatureNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Summoning, Summoner + Runemaster", 25, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createNatureSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Normal, Summoner + Runemaster", 45, MomSpellCalculations.calculateCastingCostReduction (10, spellSettings, GenerateTestData.createChaosNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Summoning, Summoner + Runemaster", 70, MomSpellCalculations.calculateCastingCostReduction (10, spellSettings, GenerateTestData.createChaosSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);

		// Sage Master does nothing for casting
		addRetort (picks, GenerateTestData.SAGE_MASTER);
		assertEquals ("Sage Master should have no effect on casting cost", 30, MomSpellCalculations.calculateCastingCostReduction (10, spellSettings, null, picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Normal, Summoner + Runemaster + Sage Master", 25, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createArcaneNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Summoning, Summoner + Runemaster + Sage Master", 50, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createArcaneSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Normal, Summoner + Runemaster + Sage Master", 0, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createNatureNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Summoning, Summoner + Runemaster + Sage Master", 25, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createNatureSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Normal, Summoner + Runemaster + Sage Master", 45, MomSpellCalculations.calculateCastingCostReduction (10, spellSettings, GenerateTestData.createChaosNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Summoning, Summoner + Runemaster + Sage Master", 70, MomSpellCalculations.calculateCastingCostReduction (10, spellSettings, GenerateTestData.createChaosSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);

		// Cap... 100 books should do it ;-)
		assertEquals ("Cap", 100, MomSpellCalculations.calculateCastingCostReduction (100, spellSettings, GenerateTestData.createArcaneSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
	}

	/**
	 * Tests the calculateResearchBonus method with adding bonuses together
	 * @throws MomException If there is a problem
	 * @throws RecordNotFoundException If we encounter a record that can't be found in the DB
	 */
	@Test
	public final void testCalculateResearchBonusAdditive () throws MomException, RecordNotFoundException
	{
		final SpellSettingData spellSettings = GenerateTestData.createOriginalSpellSettings ();
		final List<PlayerPick> picks = createPlayerPicks ();
		final ICommonDatabase db = GenerateTestData.createDB ();

		// Tests for different spells and whether we pass in the retort list or not
		assertEquals ("10 books at standard settings should give 3x research bonus", 30, MomSpellCalculations.calculateResearchBonus (10, spellSettings, null, null, db, debugLogger), DOUBLE_TOLERANCE);

		assertEquals ("Chaos mastery shouldn't apply when we don't specify a spell", 30, MomSpellCalculations.calculateResearchBonus (10, spellSettings, null, picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Normal, no extra retorts", 0, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createArcaneNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Summoning, no extra retorts", 0, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createArcaneSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Normal, no extra retorts", 0, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createNatureNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Summoning, no extra retorts", 0, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createNatureSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Normal, no extra retorts", 45, MomSpellCalculations.calculateResearchBonus (10, spellSettings, GenerateTestData.createChaosNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Summoning, no extra retorts", 45, MomSpellCalculations.calculateResearchBonus (10, spellSettings, GenerateTestData.createChaosSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);

		// Add in Summoner (25% bonus to summoning only)
		addRetort (picks, GenerateTestData.SUMMONER);
		assertEquals ("Summoner shouldn't apply when we don't specify a spell", 30, MomSpellCalculations.calculateResearchBonus (10, spellSettings, null, picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Normal, Summoner", 0, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createArcaneNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Summoning, Summoner", 25, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createArcaneSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Normal, Summoner", 0, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createNatureNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Summoning, Summoner", 25, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createNatureSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Normal, Summoner", 45, MomSpellCalculations.calculateResearchBonus (10, spellSettings, GenerateTestData.createChaosNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Summoning, Summoner", 70, MomSpellCalculations.calculateResearchBonus (10, spellSettings, GenerateTestData.createChaosSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);

		// Add in Runemaster (25% bonus to Arcane)
		addRetort (picks, GenerateTestData.RUNEMASTER);
		assertEquals ("Runemaster shouldn't apply when we don't specify a spell", 30, MomSpellCalculations.calculateResearchBonus (10, spellSettings, null, picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Normal, Summoner + Runemaster", 25, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createArcaneNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Summoning, Summoner + Runemaster", 50, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createArcaneSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Normal, Summoner + Runemaster", 0, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createNatureNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Summoning, Summoner + Runemaster", 25, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createNatureSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Normal, Summoner + Runemaster", 45, MomSpellCalculations.calculateResearchBonus (10, spellSettings, GenerateTestData.createChaosNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Summoning, Summoner + Runemaster", 70, MomSpellCalculations.calculateResearchBonus (10, spellSettings, GenerateTestData.createChaosSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);

		// Sage Master (25% research bonus to everything)
		addRetort (picks, GenerateTestData.SAGE_MASTER);
		assertEquals ("Sage Master should work even without a spell specified", 55, MomSpellCalculations.calculateResearchBonus (10, spellSettings, null, picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Normal, Summoner + Runemaster + Sage Master", 50, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createArcaneNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Summoning, Summoner + Runemaster + Sage Master", 75, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createArcaneSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Normal, Summoner + Runemaster + Sage Master", 25, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createNatureNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Summoning, Summoner + Runemaster + Sage Master", 50, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createNatureSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Normal, Summoner + Runemaster + Sage Master", 70, MomSpellCalculations.calculateResearchBonus (10, spellSettings, GenerateTestData.createChaosNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Summoning, Summoner + Runemaster + Sage Master", 95, MomSpellCalculations.calculateResearchBonus (10, spellSettings, GenerateTestData.createChaosSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);

		// Cap... 100 books should do it ;-)
		assertEquals ("Cap", 1000, MomSpellCalculations.calculateResearchBonus (100, spellSettings, GenerateTestData.createArcaneSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
	}

	/**
	 * Tests the calculateCastingCostReduction method with multiplying bonuses together
	 * @throws MomException If there is a problem
	 * @throws RecordNotFoundException If we encounter a record that can't be found in the DB
	 */
	@Test
	public final void testCalculateCastingCostReductionMultiplicative () throws MomException, RecordNotFoundException
	{
		final SpellSettingData spellSettings = GenerateTestData.createRecommendedSpellSettings ();
		final List<PlayerPick> picks = createPlayerPicks ();
		final ICommonDatabase db = GenerateTestData.createDB ();

		// Tests for different spells and whether we pass in the retort list or not
		assertEquals ("10 books at standard settings should give 3x casting cost reduction", 22.1312, MomSpellCalculations.calculateCastingCostReduction (10, spellSettings, null, null, db, debugLogger), DOUBLE_TOLERANCE);

		assertEquals ("Chaos mastery shouldn't apply when we don't specify a spell", 22.1312, MomSpellCalculations.calculateCastingCostReduction (10, spellSettings, null, picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Normal, no extra retorts", 0, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createArcaneNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Summoning, no extra retorts", 0, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createArcaneSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Normal, no extra retorts", 0, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createNatureNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Summoning, no extra retorts", 0, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createNatureSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Normal, no extra retorts", 33.81152, MomSpellCalculations.calculateCastingCostReduction (10, spellSettings, GenerateTestData.createChaosNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Summoning, no extra retorts", 33.81152, MomSpellCalculations.calculateCastingCostReduction (10, spellSettings, GenerateTestData.createChaosSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);

		// Add in Summoner (25% bonus to summoning only)
		addRetort (picks, GenerateTestData.SUMMONER);
		assertEquals ("Summoner shouldn't apply when we don't specify a spell", 22.1312, MomSpellCalculations.calculateCastingCostReduction (10, spellSettings, null, picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Normal, Summoner", 0, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createArcaneNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Summoning, Summoner", 25, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createArcaneSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Normal, Summoner", 0, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createNatureNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Summoning, Summoner", 25, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createNatureSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Normal, Summoner", 33.81152, MomSpellCalculations.calculateCastingCostReduction (10, spellSettings, GenerateTestData.createChaosNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Summoning, Summoner", 50.35864, MomSpellCalculations.calculateCastingCostReduction (10, spellSettings, GenerateTestData.createChaosSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);

		// Add in Runemaster (25% bonus to Arcane)
		addRetort (picks, GenerateTestData.RUNEMASTER);
		assertEquals ("Runemaster shouldn't apply when we don't specify a spell", 22.1312, MomSpellCalculations.calculateCastingCostReduction (10, spellSettings, null, picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Normal, Summoner + Runemaster", 25, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createArcaneNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Summoning, Summoner + Runemaster", 43.75, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createArcaneSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Normal, Summoner + Runemaster", 0, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createNatureNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Summoning, Summoner + Runemaster", 25, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createNatureSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Normal, Summoner + Runemaster", 33.81152, MomSpellCalculations.calculateCastingCostReduction (10, spellSettings, GenerateTestData.createChaosNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Summoning, Summoner + Runemaster", 50.35864, MomSpellCalculations.calculateCastingCostReduction (10, spellSettings, GenerateTestData.createChaosSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);

		// Sage Master does nothing for casting
		addRetort (picks, GenerateTestData.SAGE_MASTER);
		assertEquals ("Sage Master should have no effect on casting cost", 22.1312, MomSpellCalculations.calculateCastingCostReduction (10, spellSettings, null, picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Normal, Summoner + Runemaster + Sage Master", 25, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createArcaneNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Summoning, Summoner + Runemaster + Sage Master", 43.75, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createArcaneSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Normal, Summoner + Runemaster + Sage Master", 0, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createNatureNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Summoning, Summoner + Runemaster + Sage Master", 25, MomSpellCalculations.calculateCastingCostReduction (0, spellSettings, GenerateTestData.createNatureSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Normal, Summoner + Runemaster + Sage Master", 33.81152, MomSpellCalculations.calculateCastingCostReduction (10, spellSettings, GenerateTestData.createChaosNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Summoning, Summoner + Runemaster + Sage Master", 50.35864, MomSpellCalculations.calculateCastingCostReduction (10, spellSettings, GenerateTestData.createChaosSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);

		// Cap... 100 books should do it ;-)
		assertEquals ("Cap", 90, MomSpellCalculations.calculateCastingCostReduction (100, spellSettings, GenerateTestData.createArcaneSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
	}

	/**
	 * Tests the calculateResearchBonus method with multiplying bonuses together
	 * @throws MomException If there is a problem
	 * @throws RecordNotFoundException If we encounter a record that can't be found in the DB
	 */
	@Test
	public final void testCalculateResearchBonusMultiplicative () throws MomException, RecordNotFoundException
	{
		final SpellSettingData spellSettings = createSpecialSpellSettings ();
		final List<PlayerPick> picks = createPlayerPicks ();
		final ICommonDatabase db = GenerateTestData.createDB ();

		// Tests for different spells and whether we pass in the retort list or not
		assertEquals ("10 books at standard settings should give 3x research bonus", 25.9712, MomSpellCalculations.calculateResearchBonus (10, spellSettings, null, null, db, debugLogger), DOUBLE_TOLERANCE);

		assertEquals ("Chaos mastery shouldn't apply when we don't specify a spell", 25.9712, MomSpellCalculations.calculateResearchBonus (10, spellSettings, null, picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Normal, no extra retorts", 0, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createArcaneNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Summoning, no extra retorts", 0, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createArcaneSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Normal, no extra retorts", 0, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createNatureNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Summoning, no extra retorts", 0, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createNatureSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Normal, no extra retorts", 44.86688, MomSpellCalculations.calculateResearchBonus (10, spellSettings, GenerateTestData.createChaosNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Summoning, no extra retorts", 44.86688, MomSpellCalculations.calculateResearchBonus (10, spellSettings, GenerateTestData.createChaosSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);

		// Add in Summoner (25% bonus to summoning only)
		addRetort (picks, GenerateTestData.SUMMONER);
		assertEquals ("Summoner shouldn't apply when we don't specify a spell", 25.9712, MomSpellCalculations.calculateResearchBonus (10, spellSettings, null, picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Normal, Summoner", 0, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createArcaneNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Summoning, Summoner", 25, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createArcaneSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Normal, Summoner", 0, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createNatureNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Summoning, Summoner", 25, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createNatureSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Normal, Summoner", 44.86688, MomSpellCalculations.calculateResearchBonus (10, spellSettings, GenerateTestData.createChaosNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Summoning, Summoner", 81.0836, MomSpellCalculations.calculateResearchBonus (10, spellSettings, GenerateTestData.createChaosSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);

		// Add in Runemaster (25% bonus to Arcane)
		addRetort (picks, GenerateTestData.RUNEMASTER);
		assertEquals ("Runemaster shouldn't apply when we don't specify a spell", 25.9712, MomSpellCalculations.calculateResearchBonus (10, spellSettings, null, picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Normal, Summoner + Runemaster", 25, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createArcaneNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Summoning, Summoner + Runemaster", 56.25, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createArcaneSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Normal, Summoner + Runemaster", 0, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createNatureNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Summoning, Summoner + Runemaster", 25, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createNatureSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Normal, Summoner + Runemaster", 44.86688, MomSpellCalculations.calculateResearchBonus (10, spellSettings, GenerateTestData.createChaosNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Summoning, Summoner + Runemaster", 81.0836, MomSpellCalculations.calculateResearchBonus (10, spellSettings, GenerateTestData.createChaosSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);

		// Sage Master (25% research bonus to everything)
		addRetort (picks, GenerateTestData.SAGE_MASTER);
		assertEquals ("Sage Master should have no effect on casting cost", 57.464, MomSpellCalculations.calculateResearchBonus (10, spellSettings, null, picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Normal, Summoner + Runemaster + Sage Master", 56.25, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createArcaneNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Arcane, Summoning, Summoner + Runemaster + Sage Master", 95.3125, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createArcaneSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Normal, Summoner + Runemaster + Sage Master", 25, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createNatureNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Nature, Summoning, Summoner + Runemaster + Sage Master", 56.25, MomSpellCalculations.calculateResearchBonus (0, spellSettings, GenerateTestData.createNatureSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Normal, Summoner + Runemaster + Sage Master", 81.0836, MomSpellCalculations.calculateResearchBonus (10, spellSettings, GenerateTestData.createChaosNormalSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
		assertEquals ("Chaos, Summoning, Summoner + Runemaster + Sage Master", 126.3545, MomSpellCalculations.calculateResearchBonus (10, spellSettings, GenerateTestData.createChaosSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);

		// Cap... 100 books should do it ;-)
		assertEquals ("Cap", 1000, MomSpellCalculations.calculateResearchBonus (100, spellSettings, GenerateTestData.createArcaneSummoningSpell (), picks, db, debugLogger), DOUBLE_TOLERANCE);
	}
}