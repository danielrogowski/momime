package momime.common.messages;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import momime.common.MomException;
import momime.common.calculations.IMomSkillCalculations;
import momime.common.calculations.IMomSpellCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ICommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.v0_9_4.SpellSettingData;
import momime.common.database.v0_9_4.Spell;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomResourceValue;
import momime.common.messages.v0_9_4.PlayerPick;

/**
 * Methods for working with list of MomResourceValues
 */
public final class ResourceValueUtils implements IResourceValueUtils
{
	/** Class logger */
	private final Logger log = Logger.getLogger (ResourceValueUtils.class.getName ());
	
	/** Skill calculations */
	private IMomSkillCalculations skillCalculations;

	/** Spell calculations */
	private IMomSpellCalculations spellCalculations;
	
	/** Player pick utils */
	private IPlayerPickUtils playerPickUtils;
	
	/**
	 * @param resourceList List of resources to search through
	 * @param productionTypeID Production type ID to search for
	 * @return The resource value for the specified production type, or null if the player has none of this production type
	 */
	final MomResourceValue findResourceValue (final List<MomResourceValue> resourceList, final String productionTypeID)
	{
		MomResourceValue result = null;

		final Iterator<MomResourceValue> iter = resourceList.iterator ();
		while ((result == null) && (iter.hasNext ()))
		{
			final MomResourceValue thisValue = iter.next ();
			if (thisValue.getProductionTypeID ().equals (productionTypeID))
				result = thisValue;
		}

		return result;
	}

	/**
	 * Careful, there is a Delphi method named TMomPlayerResourceValues.FindAmountPerTurnForProductionType, but that does more than
	 * simply look the value up in the list, see calculateAmountPerTurnForProductionType method below
	 *
	 * This method is the equivalent of TMomPlayerResourceValues.FindProductionType, which *only* searches for the value in the list
	 *
	 * @param resourceList List of resources to search through
     * @param productionTypeID Type of production to look up
     * @return How much of this production type that this player generates per turn
     */
	@Override
	public final int findAmountPerTurnForProductionType (final List<MomResourceValue> resourceList, final String productionTypeID)
	{
		log.entering (ResourceValueUtils.class.getName (), "findAmountPerTurnForProductionType", productionTypeID);

		final MomResourceValue playerResourceValue = findResourceValue (resourceList, productionTypeID);

		final int result;
		if (playerResourceValue == null)
			result = 0;
		else
			result = playerResourceValue.getAmountPerTurn ();

		log.exiting (ResourceValueUtils.class.getName (), "findAmountPerTurnForProductionType", result);
		return result;
	}

	/**
	 * @param resourceList List of resources to search through
     * @param productionTypeID Type of production to look up
     * @return How much of this production type that this player has stored
     */
	@Override
	public final int findAmountStoredForProductionType (final List<MomResourceValue> resourceList, final String productionTypeID)
	{
		log.entering (ResourceValueUtils.class.getName (), "findAmountStoredForProductionType", productionTypeID);

		final MomResourceValue playerResourceValue = findResourceValue (resourceList, productionTypeID);

		final int result;
		if (playerResourceValue == null)
			result = 0;
		else
			result = playerResourceValue.getAmountStored ();

		log.exiting (ResourceValueUtils.class.getName (), "findAmountStoredForProductionType", result);
		return result;
	}

	/**
	 * @param resourceList List of resources to update
	 * @param productionTypeID Which type of production we are modifing
	 * @param amountToAdd Amount to modify their per turn production by
	 */
	@Override
	public final void addToAmountPerTurn (final List<MomResourceValue> resourceList, final String productionTypeID, final int amountToAdd)
	{
		log.entering (ResourceValueUtils.class.getName (), "addToAmountPerTurn", new String [] {productionTypeID, new Integer (amountToAdd).toString ()});

		MomResourceValue playerResourceValue = findResourceValue (resourceList, productionTypeID);
		if (playerResourceValue == null)
		{
			playerResourceValue = new MomResourceValue ();
			playerResourceValue.setProductionTypeID (productionTypeID);
			resourceList.add (playerResourceValue);
		}

		playerResourceValue.setAmountPerTurn (playerResourceValue.getAmountPerTurn () + amountToAdd);

		log.exiting (ResourceValueUtils.class.getName (), "addToAmountPerTurn");
	}

	/**
	 * @param resourceList List of resources to update
	 * @param productionTypeID Which type of production we are modifing
	 * @param amountToAdd Amount to modify their stored production by
	 */
	@Override
	public final void addToAmountStored (final List<MomResourceValue> resourceList, final String productionTypeID, final int amountToAdd)
	{
		log.entering (ResourceValueUtils.class.getName (), "addToAmountStored", new String [] {productionTypeID, new Integer (amountToAdd).toString ()});

		MomResourceValue playerResourceValue = findResourceValue (resourceList, productionTypeID);
		if (playerResourceValue == null)
		{
			playerResourceValue = new MomResourceValue ();
			playerResourceValue.setProductionTypeID (productionTypeID);
			resourceList.add (playerResourceValue);
		}

		playerResourceValue.setAmountStored (playerResourceValue.getAmountStored () + amountToAdd);

		log.exiting (ResourceValueUtils.class.getName (), "addToAmountStored");
	}

	/**
	 * Note Delphi version could either erase the values for one player or all players
	 * Java version operates only on one player because each player now has their own resource list
	 *
	 * @param resourceList List of resources to update
	 */
	@Override
	public final void zeroAmountsPerTurn (final List<MomResourceValue> resourceList)
	{
		log.entering (ResourceValueUtils.class.getName (), "zeroAmountsPerTurn");

		for (final MomResourceValue playerResourceValue : resourceList)
			playerResourceValue.setAmountPerTurn (0);

		log.exiting (ResourceValueUtils.class.getName (), "zeroAmountsPerTurn");
	}

	/**
	 * Note Delphi version could either erase the values for one player or all players
	 * Java version operates only on one player because each player now has their own resource list
	 *
	 * @param resourceList List of resources to update
	 */
	@Override
	public final void zeroAmountsStored (final List<MomResourceValue> resourceList)
	{
		log.entering (ResourceValueUtils.class.getName (), "zeroAmountsStored");

		for (final MomResourceValue playerResourceValue : resourceList)
			playerResourceValue.setAmountStored (0);

		log.exiting (ResourceValueUtils.class.getName (), "zeroAmountsStored");
	}

    /**
	 * @param resourceList List of resources to search through
     * @return The specified player's casting skill, in mana-points-spendable/turn instead of raw skill points
     */
	@Override
	public final int calculateCastingSkillOfPlayer (final List<MomResourceValue> resourceList)
	{
		log.entering (ResourceValueUtils.class.getName (), "calculateCastingSkillOfPlayer");

		final int playerSkillPoints = findAmountStoredForProductionType (resourceList, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT);
		final int result = getSkillCalculations ().getCastingSkillForSkillPoints (playerSkillPoints);

		log.exiting (ResourceValueUtils.class.getName (), "calculateCastingSkillOfPlayer", result);
		return result;
	}

	/**
     * This does include splitting magic power into mana/research/skill improvement, but does not include selling 2 rations to get 1 gold
     * Delphi method is TMomPlayerResourceValues.FindAmountPerTurnForProductionType
     *
     * @param privateInfo Private info of the player whose production amount we are calculating
     * @param picks Picks of the player whose production amount we are calculating
     * @param productionTypeID Type of production to calculate
	 * @param spellSettings Spell combination settings, either from the server XML cache or the Session description
	 * @param db Lookup lists built over the XML database
     * @return How much of this production type that this player gets per turn
	 * @throws MomException If we find an invalid casting reduction type
	 * @throws RecordNotFoundException If we look for a particular record that we expect to be present in the XML file and we can't find it
     */
	@Override
	public final int calculateAmountPerTurnForProductionType (final MomPersistentPlayerPrivateKnowledge privateInfo, final List<PlayerPick> picks,
		final String productionTypeID, final SpellSettingData spellSettings, final ICommonDatabase db)
    	throws MomException, RecordNotFoundException
	{
		log.entering (ResourceValueUtils.class.getName (), "calculateAmountPerTurnForProductionType", productionTypeID);

		// Is there a research spell specified?
		final Spell spellBeingResearched;
		if (privateInfo.getSpellIDBeingResearched () == null)
			spellBeingResearched = null;
		else
			spellBeingResearched = db.findSpell (privateInfo.getSpellIDBeingResearched (), "calculateAmountPerTurnForProductionType");

		// Find directly produced values - for research, this will give the amounts produced by libraries, universities, etc.
		final MomResourceValue playerResourceValue = findResourceValue (privateInfo.getResourceValue (), productionTypeID);
		final int rawAmountPerTurn;
		if (playerResourceValue == null)
			rawAmountPerTurn = 0;
		else
			rawAmountPerTurn = playerResourceValue.getAmountPerTurn ();

		// If the production type ID is mana, research or skill improvement then we may also be channeling some in from magic power, and may also get various bonuses
		// So there's 3 portions to the result: the raw value, the amount channeled from magic power, and percentage bonuses
		final int amountPerTurnFromMagicPower;
		final int amountPerTurnFromPercentageBonuses;

		if ((productionTypeID.equals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA)) ||
			(productionTypeID.equals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH)) ||
			(productionTypeID.equals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT)))
		{
			final int powerBase = calculateAmountPerTurnForProductionType (privateInfo, picks,
				CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MAGIC_POWER, spellSettings, db);

			// Calculate research and skill
			final int researchPerTurnAmountFromMagicPower = (int) Math.round (powerBase * privateInfo.getMagicPowerDistribution ().getResearchRatio () / 240d);
			final int skillPerTurnAmountFromMagicPower = (int) Math.round (powerBase * privateInfo.getMagicPowerDistribution ().getSkillRatio () / 240d);

			// Mana is just whatever is left
			final int manaPerTurnAmountFromMagicPower = powerBase - researchPerTurnAmountFromMagicPower - skillPerTurnAmountFromMagicPower;

			// Add on the relevant amount
			if (productionTypeID.equals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA))
				amountPerTurnFromMagicPower = manaPerTurnAmountFromMagicPower;
			else if (productionTypeID.equals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH))
				amountPerTurnFromMagicPower = researchPerTurnAmountFromMagicPower;
			else /* VALUE_PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT */
				amountPerTurnFromMagicPower = skillPerTurnAmountFromMagicPower;

			final int totalBeforePercentageBonuses = rawAmountPerTurn + amountPerTurnFromMagicPower;

			/*
	         *  Add any bonuses we get to each type of production
	         *  This includes things like:
	         *  Archmage gives +50% bonus to magic power spent on skill improvement
	         *  Mana Focusing gives +25% bonus to magic power spent on mana
			 *
	         *  For mana and skill we can just add these on - research is a little
	         *  more complicated since we might need to add or multiply the bonuses
	         *  together, and there is the cap to consider
			 *
	         *  Verified that original MoM always rounds down
	         *  (12 research + 15% = 13.8 but reports as 13
			 */
			if (productionTypeID.equals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH))
			{
				// How many spell books do we have in this realm? (we might get a research bonus)
				final int bookCount;
				if (spellBeingResearched != null)
					bookCount = getPlayerPickUtils ().getQuantityOfPick (picks, spellBeingResearched.getSpellRealm ());
				else
					bookCount = 0;

				// Now can calculate the bonus
				final double researchBonusPercentage = getSpellCalculations ().calculateResearchBonus (bookCount, spellSettings, spellBeingResearched, picks, db);

				// The 0.00001 is a hack so that if we end up with a value like 5.99999999 which is supposed to be 6, just has lost a fraction due to rounding errors, we still round it in the correct direction
				amountPerTurnFromPercentageBonuses = (int) ((totalBeforePercentageBonuses * (researchBonusPercentage / 100d)) + 0.00001d);
			}
			else
			{
				amountPerTurnFromPercentageBonuses = (totalBeforePercentageBonuses * getPlayerPickUtils ().totalProductionBonus (productionTypeID, null, picks, db)) / 100;
			}
		}
		else
		{
			amountPerTurnFromMagicPower = 0;
			amountPerTurnFromPercentageBonuses = 0;
		}

		final int result = rawAmountPerTurn + amountPerTurnFromMagicPower + amountPerTurnFromPercentageBonuses;

		log.exiting (ResourceValueUtils.class.getName (), "calculateAmountPerTurnForProductionType", result);
		return result;
	}

	/**
	 * @return Skill calculations
	 */
	public final IMomSkillCalculations getSkillCalculations ()
	{
		return skillCalculations;
	}

	/**
	 * @param calc Skill calculations
	 */
	public final void setSkillCalculations (final IMomSkillCalculations calc)
	{
		skillCalculations = calc;
	}

	/**
	 * @return Spell calculations
	 */
	public final IMomSpellCalculations getSpellCalculations ()
	{
		return spellCalculations;
	}

	/**
	 * @param calc Spell calculations
	 */
	public final void setSpellCalculations (final IMomSpellCalculations calc)
	{
		spellCalculations = calc;
	}

	/**
	 * @return Player pick utils
	 */
	public final IPlayerPickUtils getPlayerPickUtils ()
	{
		return playerPickUtils;
	}

	/**
	 * @param utils Player pick utils
	 */
	public final void setPlayerPickUtils (final IPlayerPickUtils utils)
	{
		playerPickUtils = utils;
	}
}
