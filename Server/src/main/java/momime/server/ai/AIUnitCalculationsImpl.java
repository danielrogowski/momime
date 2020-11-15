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
import momime.common.database.RecordNotFoundException;
import momime.common.database.SpellSetting;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.UnitUtils;

/**
 * Methods that the AI uses to calculate stats about types of units and rating how good units are
 */
public final class AIUnitCalculationsImpl implements AIUnitCalculations
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (AIUnitCalculationsImpl.class);
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
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
		log.trace ("Entering determineAIUnitType: " + xu);
		
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

		log.trace ("Exiting determineAIUnitType = " + aiUnitType);
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
		log.trace ("Entering calculateUnitAverageRating: " + unit.getUnitID () + " owned by player ID " + unit.getOwningPlayerID ());
		
		final int rating = (getAiUnitRatingCalculations ().calculateUnitCurrentRating (unit, xu, players, mem, db) + getAiUnitRatingCalculations ().calculateUnitPotentialRating (unit, players, mem, db)) / 2;

		log.trace ("Exiting calculateUnitAverageRating = " + rating);
		return rating;
	}
	
	/**
	 * @param player AI player who is considering constructing the specified unit
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
	public final boolean canAffordUnitMaintenance (final PlayerServerDetails player, final List<PlayerServerDetails> players, final AvailableUnit unit,
		final SpellSetting spellSettings, final CommonDatabase db) throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering canAffordUnitMaintenance: " + unit.getUnitID () + " owned by player ID " + player.getPlayerDescription ().getPlayerID ());

		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		
		final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (unit, null, null, null, players, priv.getFogOfWarMemory (), db);
		
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
					(getPlayerPickUtils ().getQuantityOfPick (pub.getPick (), CommonDatabaseConstants.RETORT_ID_CHANNELER) > 0))
					
					upkeep = upkeep - (upkeep / 2);
				
				if (upkeep > getResourceValueUtils ().calculateAmountPerTurnForProductionType (priv, pub.getPick (), productionTypeID, spellSettings, db))
					ok = false;
			}
		}
		
		log.trace ("Exiting canAffordUnitMaintenance = " + ok);
		return ok;
	}

	/**
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final UnitUtils utils)
	{
		unitUtils = utils;
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