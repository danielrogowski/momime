package momime.common.utils;

import java.util.Iterator;
import java.util.List;

import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.calculations.SkillCalculations;
import momime.common.calculations.SpellCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Pick;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellSetting;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomResourceValue;
import momime.common.messages.PlayerPick;
import momime.common.messages.UnitStatusID;

/**
 * Methods for working with list of MomResourceValues
 */
public final class ResourceValueUtilsImpl implements ResourceValueUtils
{
	/** Skill calculations */
	private SkillCalculations skillCalculations;

	/** Spell calculations */
	private SpellCalculations spellCalculations;
	
	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;

	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
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
		final MomResourceValue playerResourceValue = findResourceValue (resourceList, productionTypeID);

		final int result;
		if (playerResourceValue == null)
			result = 0;
		else
			result = playerResourceValue.getAmountPerTurn ();

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
		final MomResourceValue playerResourceValue = findResourceValue (resourceList, productionTypeID);

		final int result;
		if (playerResourceValue == null)
			result = 0;
		else
			result = playerResourceValue.getAmountStored ();

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
		MomResourceValue playerResourceValue = findResourceValue (resourceList, productionTypeID);
		if (playerResourceValue == null)
		{
			playerResourceValue = new MomResourceValue ();
			playerResourceValue.setProductionTypeID (productionTypeID);
			resourceList.add (playerResourceValue);
		}

		playerResourceValue.setAmountPerTurn (playerResourceValue.getAmountPerTurn () + amountToAdd);
	}

	/**
	 * @param resourceList List of resources to update
	 * @param productionTypeID Which type of production we are modifing
	 * @param amountToAdd Amount to modify their stored production by
	 */
	@Override
	public final void addToAmountStored (final List<MomResourceValue> resourceList, final String productionTypeID, final int amountToAdd)
	{
		MomResourceValue playerResourceValue = findResourceValue (resourceList, productionTypeID);
		if (playerResourceValue == null)
		{
			playerResourceValue = new MomResourceValue ();
			playerResourceValue.setProductionTypeID (productionTypeID);
			resourceList.add (playerResourceValue);
		}

		playerResourceValue.setAmountStored (playerResourceValue.getAmountStored () + amountToAdd);
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
		for (final MomResourceValue playerResourceValue : resourceList)
			playerResourceValue.setAmountPerTurn (0);
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
		for (final MomResourceValue playerResourceValue : resourceList)
			playerResourceValue.setAmountStored (0);
	}

    /**
	 * @param resourceList List of resources to search through
     * @return The specified player's casting skill, in mana-points-spendable/turn instead of raw skill points
     */
	@Override
	public final int calculateBasicCastingSkill (final List<MomResourceValue> resourceList)
	{
		final int playerSkillPoints = findAmountStoredForProductionType (resourceList, CommonDatabaseConstants.PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT);
		final int result = getSkillCalculations ().getCastingSkillForSkillPoints (playerSkillPoints);

		return result;
	}

    /**
	 * @param resourceList List of resources to search through
	 * @param wizardDetails Details about the wizard whose casting skill we want
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @param includeBonusFromHeroesAtFortress Whether to add on bonus from heroes parked at the Wizard's Fortress, or only from Archmage
     * @return The specified player's casting skill, including bonuses from Archmage and heroes
     * @throws RecordNotFoundException If we can't find one of our picks in the database
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
     */
	@Override
	public final int calculateModifiedCastingSkill (final List<MomResourceValue> resourceList, final KnownWizardDetails wizardDetails,
		final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem, final CommonDatabase db, final boolean includeBonusFromHeroesAtFortress)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		int total = calculateBasicCastingSkill (resourceList);
		
		// Archmage
		for (final PlayerPick pick : wizardDetails.getPick ())
		{
			final Pick pickDef = db.findPick (pick.getPickID (), "calculateModifiedCastingSkill");
			if (pickDef.getDynamicSkillBonus () != null)
				total = total + (pickDef.getDynamicSkillBonus () * pick.getQuantity ());
		}
		
		// Heroes with caster skill who are parked at our Fortress
		if (includeBonusFromHeroesAtFortress)
		{
			final MemoryBuilding fortressLocation = getMemoryBuildingUtils ().findCityWithBuilding
				(wizardDetails.getPlayerID (), CommonDatabaseConstants.BUILDING_FORTRESS, mem.getMap (), mem.getBuilding ());
			if (fortressLocation != null)
			{
				int heroesTotalMana = 0;
				
				for (final MemoryUnit unit : mem.getUnit ())
					if ((unit.getStatus () == UnitStatusID.ALIVE) && (unit.getOwningPlayerID () == wizardDetails.getPlayerID ()) &&
						(fortressLocation.getCityLocation ().equals (unit.getUnitLocation ())))
					{
						final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (unit, null, null, null, players, mem, db);
						if (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_HERO))
							heroesTotalMana = heroesTotalMana + xu.calculateManaTotal ();
					}
				
				total = total + (heroesTotalMana / 2);
			}
		}
		
		return total;
	}

	/**
	 * There isn't a "calculateBasicResearch" and "calculateModifiedResearch" since basic research is calculated with calculateAmountPerTurnForProductionType.
	 * So this method adds on the modified bonus onto basic research, for which the only source is from Sage heroes.
	 * 
	 * @param playerID Player we want to calculate modified research for
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Research bonus from units
     * @throws RecordNotFoundException If we can't find one of our picks in the database
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final int calculateResearchFromUnits (final int playerID, final List<? extends PlayerPublicDetails> players,
		final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		int total = 0;
		
		for (final MemoryUnit unit : mem.getUnit ())
			if ((unit.getStatus () == UnitStatusID.ALIVE) && (unit.getOwningPlayerID () == playerID))
			{
				final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (unit, null, null, null, players, mem, db);
				if (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_SAGE))
				{
					final int expLevel = xu.getModifiedExperienceLevel ().getLevelNumber ();
					final int heroSkillValue = ((xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_SAGE) + 1) * 3 * (expLevel+1)) / 2;
					total = total + heroSkillValue;
				}
			}
		
		return total;
	}
	
    /**
	 * @param resourceList List of resources to search through
     * @return The specified player's fame accumulated through battles
     */
	@Override
	public final int calculateBasicFame (final List<MomResourceValue> resourceList)
	{
		return findAmountStoredForProductionType (resourceList, CommonDatabaseConstants.PRODUCTION_TYPE_ID_FAME);
	}
	
    /**
	 * @param resourceList List of resources to search through
	 * @param wizardDetails Details about the wizard whose fame we want
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
     * @return The specified player's fame accumulated through battles, retorts, spells and legendary heroes
     * @throws RecordNotFoundException If we can't find one of our picks in the database
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
     */
	@Override
	public final int calculateModifiedFame (final List<MomResourceValue> resourceList, final KnownWizardDetails wizardDetails,
		final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		int total = calculateBasicFame (resourceList);

		// Famous
		for (final PlayerPick pick : wizardDetails.getPick ())
		{
			final Pick pickDef = db.findPick (pick.getPickID (), "calculateModifiedFame");
			if (pickDef.getDynamicFameBonus () != null)
				total = total + (pickDef.getDynamicFameBonus () * pick.getQuantity ());
		}
		
		// Just Cause
		if (getMemoryMaintainedSpellUtils ().findMaintainedSpell (mem.getMaintainedSpell (), wizardDetails.getPlayerID (),
			CommonDatabaseConstants.SPELL_ID_JUST_CAUSE, null, null, null, null) != null)
			
			total = total + 10;
		
		// Legendary heroes
		for (final MemoryUnit unit : mem.getUnit ())
			if ((unit.getStatus () == UnitStatusID.ALIVE) && (unit.getOwningPlayerID () == wizardDetails.getPlayerID ()))
			{
				final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (unit, null, null, null, players, mem, db);
				if (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_LEGENDARY))
				{
					final int expLevel = xu.getModifiedExperienceLevel ().getLevelNumber ();
					final int heroSkillValue = ((xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_LEGENDARY) + 1) * 3 * (expLevel+1)) / 2;
					total = total + heroSkillValue;
				}
			}
		
		return total;
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
		final String productionTypeID, final SpellSetting spellSettings, final CommonDatabase db)
    	throws MomException, RecordNotFoundException
	{
		// Find directly produced values - for research, this will give the amounts produced by libraries, universities, etc.
		final int rawAmountPerTurn = findAmountPerTurnForProductionType (privateInfo.getResourceValue (), productionTypeID);

		// If the production type ID is mana, research or skill improvement then we may also be channeling some in from magic power, and may also get various bonuses
		// So there's 3 portions to the result: the raw value, the amount channeled from magic power, and percentage bonuses
		final int amountPerTurnFromMagicPower;
		final int amountPerTurnFromPercentageBonuses;

		if ((productionTypeID.equals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)) ||
			(productionTypeID.equals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH)) ||
			(productionTypeID.equals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT)))
		{
			final int powerBase = calculateAmountPerTurnForProductionType (privateInfo, picks,
				CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER, spellSettings, db);

			// Calculate research and skill
			final int manaPerTurnAmountFromMagicPower		= (powerBase * privateInfo.getMagicPowerDistribution ().getManaRatio ()		/ CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX);
			final int researchPerTurnAmountFromMagicPower	= (powerBase * privateInfo.getMagicPowerDistribution ().getResearchRatio ()	/ CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX);
			final int skillPerTurnAmountFromMagicPower			= (powerBase * privateInfo.getMagicPowerDistribution ().getSkillRatio ()			/ CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX);

			// Add on the relevant amount
			if (productionTypeID.equals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA))
				amountPerTurnFromMagicPower = manaPerTurnAmountFromMagicPower;
			else if (productionTypeID.equals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH))
				amountPerTurnFromMagicPower = researchPerTurnAmountFromMagicPower;
			else /* PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT */
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
			if (productionTypeID.equals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH))
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
		return result;
	}

	/**
	 * @return Skill calculations
	 */
	public final SkillCalculations getSkillCalculations ()
	{
		return skillCalculations;
	}

	/**
	 * @param calc Skill calculations
	 */
	public final void setSkillCalculations (final SkillCalculations calc)
	{
		skillCalculations = calc;
	}

	/**
	 * @return Spell calculations
	 */
	public final SpellCalculations getSpellCalculations ()
	{
		return spellCalculations;
	}

	/**
	 * @param calc Spell calculations
	 */
	public final void setSpellCalculations (final SpellCalculations calc)
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

	/**
	 * @return Memory building utils
	 */
	public final MemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param utils Memory building utils
	 */
	public final void setMemoryBuildingUtils (final MemoryBuildingUtils utils)
	{
		memoryBuildingUtils = utils;
	}

	/**
	 * @return expandUnitDetails method
	 */
	public final ExpandUnitDetails getExpandUnitDetails ()
	{
		return expandUnitDetails;
	}

	/**
	 * @param e expandUnitDetails method
	 */
	public final void setExpandUnitDetails (final ExpandUnitDetails e)
	{
		expandUnitDetails = e;
	}

	/**
	 * @return MemoryMaintainedSpell utils
	 */
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param spellUtils MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils spellUtils)
	{
		memoryMaintainedSpellUtils = spellUtils;
	}
}