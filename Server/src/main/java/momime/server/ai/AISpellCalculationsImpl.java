package momime.server.ai;

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ProductionTypeAndUndoubledValue;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.ResourceValueUtils;
import momime.server.database.SpellSvr;

/**
 * Methods that the AI uses to calculate stats about types of spells it might want to cast
 */
public final class AISpellCalculationsImpl implements AISpellCalculations
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (AISpellCalculationsImpl.class);

	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/**
	 * @param player Player who wants to cast a spell
	 * @param spell Spell they want to cast
	 * @return Whether the player can afford maintence cost of the spell after it is cast
	 */
	@Override
	public final boolean canAffordSpellMaintenance (final PlayerServerDetails player, final SpellSvr spell)
	{
		log.trace ("Entering canAffordSpellMaintenance: Player ID " + player.getPlayerDescription ().getPlayerID () + ", spell ID " + spell.getSpellID ());

		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		
		final boolean channeler = (getPlayerPickUtils ().getQuantityOfPick (pub.getPick (), CommonDatabaseConstants.RETORT_ID_CHANNELER) > 0);
		
		boolean ok = true;
		final Iterator<ProductionTypeAndUndoubledValue> iter = spell.getSpellUpkeep ().iterator ();
		while ((ok) && (iter.hasNext ()))
		{
			final ProductionTypeAndUndoubledValue upkeep = iter.next ();
			
			// Note there is no getModifiedUpkeepValue for spells - see how recalculateAmountsPerTurn works 
			int consumption = upkeep.getUndoubledProductionValue ();
			if (consumption < 0)
			{
				// Don't bother to take into account here that AI players get cheaper upkeep - but do take into account channeler retort halfing upkeep
				if ((channeler) && (consumption < -1) && (upkeep.getProductionTypeID ().equals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)))
					consumption = consumption + ((-consumption) / 2);
				
				// Are we generating enough spare mana?
				if (-consumption > getResourceValueUtils ().findAmountPerTurnForProductionType (priv.getResourceValue (), upkeep.getProductionTypeID ()))
					ok = false;
			}
		}
		
		// When the AI starts casting summoning spells, this will need to look up maintenance of the created units
		// Note canAffordUnitMaintenance is not clever enough to half mana upkeep of summoned units for Channelers yet either, though it does do Summoner retort correctly
		
		log.trace ("Exiting canAffordSpellMaintenance = " + ok);
		return ok;
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
	 * @return Resource value utils
	 */
	public final ResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
	}

	/**
	 * @param utils Resource value utils
	 */
	public final void setResourceValueUtils (final ResourceValueUtils utils)
	{
		resourceValueUtils = utils;
	}
}