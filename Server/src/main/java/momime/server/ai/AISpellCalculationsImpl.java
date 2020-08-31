package momime.server.ai;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ProductionTypeAndUndoubledValue;
import momime.common.database.RecordNotFoundException;
import momime.common.database.SpellBookSectionID;
import momime.common.messages.AvailableUnit;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.UnitUtils;
import momime.server.calculations.ServerUnitCalculations;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.SpellSvr;
import momime.server.database.UnitSvr;

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
	
	/** Methods that the AI uses to calculate stats about types of units and rating how good units are */
	private AIUnitCalculations aiUnitCalculations;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Server-only unit calculations */
	private ServerUnitCalculations serverUnitCalculations;
	
	/**
	 * @param player Player who wants to cast a spell
	 * @param players Players list
	 * @param spell Spell they want to cast
	 * @param trueUnits List of true units
	 * @param db Lookup lists built over the XML database
	 * @return Whether the player can afford maintence cost of the spell after it is cast
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public boolean canAffordSpellMaintenance (final PlayerServerDetails player, final List<PlayerServerDetails> players, final SpellSvr spell,
		final List<MemoryUnit> trueUnits, final ServerDatabaseEx db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering canAffordSpellMaintenance: Player ID " + player.getPlayerDescription ().getPlayerID () + ", spell ID " + spell.getSpellID ());

		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		
		boolean ok = true;
		
		// Direct upkeep of the spell
		final Iterator<ProductionTypeAndUndoubledValue> upkeepIter = spell.getSpellUpkeep ().iterator ();
		while ((ok) && (upkeepIter.hasNext ()))
		{
			final ProductionTypeAndUndoubledValue upkeep = upkeepIter.next ();
			
			// Note there is no getModifiedUpkeepValue for spells - see how recalculateAmountsPerTurn works 
			int consumption = upkeep.getUndoubledProductionValue ();
			if (consumption > 0)
			{
				// Don't bother to take into account here that AI players get cheaper upkeep - but do take into account channeler retort halfing upkeep
				if ((consumption > 1) && (upkeep.getProductionTypeID ().equals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)) &&
					(getPlayerPickUtils ().getQuantityOfPick (pub.getPick (), CommonDatabaseConstants.RETORT_ID_CHANNELER) > 0))
					
					consumption = consumption - (consumption / 2);
				
				// Are we generating enough spare mana?
				if (consumption > getResourceValueUtils ().findAmountPerTurnForProductionType (priv.getResourceValue (), upkeep.getProductionTypeID ()))
					ok = false;
			}
		}
		
		// Upkeep of any units the spell may summon.
		// Use proper method for this that will ignore heroes we already have.
		if (spell.getSpellBookSectionID () == SpellBookSectionID.SUMMONING)
		{
			final Iterator<UnitSvr> summonedIter = getServerUnitCalculations ().listUnitsSpellMightSummon (spell, player, trueUnits, db).iterator ();
			while ((ok) && (summonedIter.hasNext ()))
			{
				final UnitSvr unitDef = summonedIter.next ();
	
				final AvailableUnit unit = new AvailableUnit ();
				unit.setOwningPlayerID (player.getPlayerDescription ().getPlayerID ());
				unit.setUnitID (unitDef.getUnitID ());
				
				getUnitUtils ().initializeUnitSkills (unit, null, db);
				
				if (!getAiUnitCalculations ().canAffordUnitMaintenance (player, players, unit, db))
					ok = false;
			}
		}
		
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

	/**
	 * @return Methods that the AI uses to calculate stats about types of units and rating how good units are
	 */
	public final AIUnitCalculations getAiUnitCalculations ()
	{
		return aiUnitCalculations;
	}

	/**
	 * @param calc Methods that the AI uses to calculate stats about types of units and rating how good units are
	 */
	public final void setAiUnitCalculations (final AIUnitCalculations calc)
	{
		aiUnitCalculations = calc;
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
	 * @return Server-only unit calculations
	 */
	public final ServerUnitCalculations getServerUnitCalculations ()
	{
		return serverUnitCalculations;
	}

	/**
	 * @param calc Server-only unit calculations
	 */
	public final void setServerUnitCalculations (final ServerUnitCalculations calc)
	{
		serverUnitCalculations = calc;
	}
}