package momime.common.utils;

import java.util.Iterator;
import java.util.List;

import momime.common.MomException;
import momime.common.calculations.MomSkillCalculations;
import momime.common.calculations.MomSpellCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.SpellSettingData;
import momime.common.database.Spell;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomResourceValue;
import momime.common.messages.PlayerPick;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Methods for working with list of MomResourceValues
 */
public final class ResourceValueUtilsImpl implements ResourceValueUtils
{
	/** Class logger */
	private final Log log = LogFactory.getLog (ResourceValueUtilsImpl.class);
	
	/** Skill calculations */
	private MomSkillCalculations skillCalculations;

	/** Spell calculations */
	private MomSpellCalculations spellCalculations;
	
	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
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
		log.trace ("Entering findAmountPerTurnForProductionType: " + productionTypeID);

		final MomResourceValue playerResourceValue = findResourceValue (resourceList, productionTypeID);

		final int result;
		if (playerResourceValue == null)
			result = 0;
		else
			result = playerResourceValue.getAmountPerTurn ();

		log.trace ("Exiting findAmountPerTurnForProductionType = " + result);
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
		log.trace ("Entering findAmountStoredForProductionType: " + productionTypeID);

		final MomResourceValue playerResourceValue = findResourceValue (resourceList, productionTypeID);

		final int result;
		if (playerResourceValue == null)
			result = 0;
		else
			result = playerResourceValue.getAmountStored ();

		log.trace ("Exiting findAmountStoredForProductionType = " + result);
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
		log.trace ("Entering addToAmountPerTurn: " + productionTypeID + ", adding " + amountToAdd);

		MomResourceValue playerResourceValue = findResourceValue (resourceList, productionTypeID);
		if (playerResourceValue == null)
		{
			playerResourceValue = new MomResourceValue ();
			playerResourceValue.setProductionTypeID (productionTypeID);
			resourceList.add (playerResourceValue);
		}

		playerResourceValue.setAmountPerTurn (playerResourceValue.getAmountPerTurn () + amountToAdd);

		log.trace ("Exiting addToAmountPerTurn");
	}

	/**
	 * @param resourceList List of resources to update
	 * @param productionTypeID Which type of production we are modifing
	 * @param amountToAdd Amount to modify their stored production by
	 */
	@Override
	public final void addToAmountStored (final List<MomResourceValue> resourceList, final String productionTypeID, final int amountToAdd)
	{
		log.trace ("Entering addToAmountStored: " + productionTypeID + ", adding " + amountToAdd);

		MomResourceValue playerResourceValue = findResourceValue (resourceList, productionTypeID);
		if (playerResourceValue == null)
		{
			playerResourceValue = new MomResourceValue ();
			playerResourceValue.setProductionTypeID (productionTypeID);
			resourceList.add (playerResourceValue);
		}

		playerResourceValue.setAmountStored (playerResourceValue.getAmountStored () + amountToAdd);

		log.trace ("Exiting addToAmountStored");
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
		log.trace ("Entering zeroAmountsPerTurn");

		for (final MomResourceValue playerResourceValue : resourceList)
			playerResourceValue.setAmountPerTurn (0);

		log.trace ("Exiting zeroAmountsPerTurn");
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
		log.trace ("Entering zeroAmountsStored");

		for (final MomResourceValue playerResourceValue : resourceList)
			playerResourceValue.setAmountStored (0);

		log.trace ("Exiting zeroAmountsStored");
	}

    /**
	 * @param resourceList List of resources to search through
     * @return The specified player's casting skill, in mana-points-spendable/turn instead of raw skill points
     */
	@Override
	public final int calculateCastingSkillOfPlayer (final List<MomResourceValue> resourceList)
	{
		log.trace ("Entering calculateCastingSkillOfPlayer");

		final int playerSkillPoints = findAmountStoredForProductionType (resourceList, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT);
		final int result = getSkillCalculations ().getCastingSkillForSkillPoints (playerSkillPoints);

		log.trace ("Exiting calculateCastingSkillOfPlayer = " + result);
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
		final String productionTypeID, final SpellSettingData spellSettings, final CommonDatabase db)
    	throws MomException, RecordNotFoundException
	{
		log.trace ("Entering calculateAmountPerTurnForProductionType = " + productionTypeID);

		// Find directly produced values - for research, this will give the amounts produced by libraries, universities, etc.
		final int rawAmountPerTurn = findAmountPerTurnForProductionType (privateInfo.getResourceValue (), productionTypeID);
		log.trace ("rawAmountPerTurn " + productionTypeID + " = " + rawAmountPerTurn);

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
			final int manaPerTurnAmountFromMagicPower		= (powerBase * privateInfo.getMagicPowerDistribution ().getManaRatio ()		/ CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX);
			final int researchPerTurnAmountFromMagicPower	= (powerBase * privateInfo.getMagicPowerDistribution ().getResearchRatio ()	/ CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX);
			final int skillPerTurnAmountFromMagicPower			= (powerBase * privateInfo.getMagicPowerDistribution ().getSkillRatio ()			/ CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX);

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
				// Is there a research spell specified?
				final Spell spellBeingResearched;
				if (privateInfo.getSpellIDBeingResearched () == null)
					spellBeingResearched = null;
				else
					spellBeingResearched = db.findSpell (privateInfo.getSpellIDBeingResearched (), "calculateAmountPerTurnForProductionType");

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

		log.trace ("Exiting calculateAmountPerTurnForProductionType = " + result);
		return result;
	}

	/**
	 * @return Skill calculations
	 */
	public final MomSkillCalculations getSkillCalculations ()
	{
		return skillCalculations;
	}

	/**
	 * @param calc Skill calculations
	 */
	public final void setSkillCalculations (final MomSkillCalculations calc)
	{
		skillCalculations = calc;
	}

	/**
	 * @return Spell calculations
	 */
	public final MomSpellCalculations getSpellCalculations ()
	{
		return spellCalculations;
	}

	/**
	 * @param calc Spell calculations
	 */
	public final void setSpellCalculations (final MomSpellCalculations calc)
	{
		spellCalculations = calc;
	}

	/**
	 * @return Player pick utils
	 */
	public final PlayerPickUtils getPlayerPickUtils ()
	{
		return playerPickUtils;
	}

	/**
	 * @param utils Player pick utils
	 */
	public final void setPlayerPickUtils (final PlayerPickUtils utils)
	{
		playerPickUtils = utils;
	}
}