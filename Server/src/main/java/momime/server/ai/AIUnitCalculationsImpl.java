package momime.server.ai;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ProductionTypeEx;
import momime.common.database.RecordNotFoundException;
import momime.common.database.SpellSetting;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.ResourceValueUtils;

/**
 * Methods that the AI uses to calculate stats about types of units and rating how good units are
 */
public final class AIUnitCalculationsImpl implements AIUnitCalculations
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (AIUnitCalculationsImpl.class);
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** Underlying methods that the AI uses to calculate ratings about how good units are */
	private AIUnitRatingCalculations aiUnitRatingCalculations;
	
	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/**
	 * @param xu Unit to check
	 * @return Type of unit for the AI to treat this unit as, so it knows what to do with it
	 */
	@Override
	public final AIUnitType determineAIUnitType (final ExpandedUnitDetails xu)
	{
		final AIUnitType aiUnitType;
		
		if (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_CREATE_OUTPOST))
			aiUnitType = AIUnitType.BUILD_CITY;
		
		else if (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_BUILD_ROAD))
			aiUnitType = AIUnitType.BUILD_ROAD;
		
		else if (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_MELD_WITH_NODE))
			aiUnitType = AIUnitType.MELD_WITH_NODE;
		
		else if (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_PURIFY))
			aiUnitType = AIUnitType.PURIFY;
		
		else if ((xu.getUnitDefinition ().getTransportCapacity () != null) && (xu.getUnitDefinition ().getTransportCapacity () > 0))
			aiUnitType = AIUnitType.TRANSPORT;
		
		else
			aiUnitType = AIUnitType.COMBAT_UNIT;

		return aiUnitType;
	}

	/**
	 * @param unit Unit to calculate value for
	 * @param xu Expanded unit details to calculate value for if already worked out, otherwise can pass null
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Value AI estimates for the quality, usefulness and effectiveness for defensive purposes
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final int calculateUnitAverageRating (final AvailableUnit unit, final ExpandedUnitDetails xu, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		final int rating = (getAiUnitRatingCalculations ().calculateUnitCurrentRating (unit, xu, players, mem, db) + getAiUnitRatingCalculations ().calculateUnitPotentialRating (unit, players, mem, db)) / 2;
		return rating;
	}
	
	/**
	 * @param player AI player who is considering constructing the specified unit
	 * @param wizardDetails AI wizard who is considering constructing the specified unit
	 * @param players Players list
	 * @param unit Unit they want to construct
	 * @param spellSettings Spell combination settings, either from the server XML cache or the Session description
	 * @param db Lookup lists built over the XML database
	 * @return Whether or not we can afford the additional maintenance cost of this unit - will ignore rations since we can always allocate more farmers
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final boolean canAffordUnitMaintenance (final PlayerServerDetails player, final KnownWizardDetails wizardDetails, final List<PlayerServerDetails> players, final AvailableUnit unit,
		final SpellSetting spellSettings, final CommonDatabase db) throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		
		final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (unit, null, null, null, players, priv.getFogOfWarMemory (), db);
		
		// Now we can check its upkeep
		boolean ok = true;
		final Iterator<String> iter = xu.listModifiedUpkeepProductionTypeIDs ().iterator ();
		while ((ok) && (iter.hasNext ()))
		{
			final String productionTypeID = iter.next ();
			
			// Ignore rations - we can always just change more workers into farmers
			if (!productionTypeID.equals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS))
			{
				int upkeep = xu.getModifiedUpkeepValue (productionTypeID);
				
				// Halve mana upkeep if we have channeler retort
				if ((upkeep > 1) && (productionTypeID.equals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)) &&
					(getPlayerPickUtils ().getQuantityOfPick (wizardDetails.getPick (), CommonDatabaseConstants.RETORT_ID_CHANNELER) > 0))
					
					upkeep = upkeep - (upkeep / 2);
				
				final int productionPerTurn = getResourceValueUtils ().calculateAmountPerTurnForProductionType (priv, wizardDetails.getPick (), productionTypeID, spellSettings, db);
				int combinedPerTurn = productionPerTurn;
				
				// If the unit has mana upkeep, then also consider how much gold we're generating
				int goldPerTurn = 0;
				if (productionTypeID.equals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA))
				{
					goldPerTurn = getResourceValueUtils ().calculateAmountPerTurnForProductionType (priv, wizardDetails.getPick (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, spellSettings, db);
					combinedPerTurn = combinedPerTurn + ((getPlayerPickUtils ().getQuantityOfPick (wizardDetails.getPick (), CommonDatabaseConstants.RETORT_ID_ALCHEMY) > 0) ?
						goldPerTurn : (goldPerTurn/2));
				}
				
				if (upkeep > combinedPerTurn)
				{
					ok = false;
					if (log.isDebugEnabled ())
					{
						if (productionTypeID.equals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA))
						{
							log.debug ("AI player ID " + player.getPlayerDescription ().getPlayerID () + " can't afford " + upkeep + " MP upkeep of " +
								unit.getUnitID () + " when it has " + productionPerTurn + " MP + " + goldPerTurn + " GP = " + combinedPerTurn + " combined spare income per turn"); 
						}
						else
						{
							final ProductionTypeEx productionType = db.findProductionType (productionTypeID, "canAffordUnitMaintenance");
							log.debug ("AI player ID " + player.getPlayerDescription ().getPlayerID () + " can't afford " + upkeep + " " +
								productionType.getProductionTypeSuffix ().get (0).getText () + " upkeep of " +
								unit.getUnitID () + " when it has " + productionPerTurn + " " +
								productionType.getProductionTypeSuffix ().get (0).getText () + " spare per turn");
						}
					}
				}
			}
		}
		
		return ok;
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
	 * @return Resource value utils
	 */
	public final ResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
	}

	/**
	 * @param util Resource value utils
	 */
	public final void setResourceValueUtils (final ResourceValueUtils util)
	{
		resourceValueUtils = util;
	}

	/**
	 * @return Underlying methods that the AI uses to calculate ratings about how good units are
	 */
	public final AIUnitRatingCalculations getAiUnitRatingCalculations ()
	{
		return aiUnitRatingCalculations;
	}

	/**
	 * @param calc Underlying methods that the AI uses to calculate ratings about how good units are
	 */
	public final void setAiUnitRatingCalculations (final AIUnitRatingCalculations calc)
	{
		aiUnitRatingCalculations = calc;
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