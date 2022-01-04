package momime.server.ai;

import java.util.List;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.SpellSetting;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.utils.ExpandedUnitDetails;

/**
 * Methods that the AI uses to calculate stats about types of units and rating how good units are
 */
public interface AIUnitCalculations
{
	/**
	 * @param xu Unit to check
	 * @return Type of unit for the AI to treat this unit as, so it knows what to do with it
	 */
	public AIUnitType determineAIUnitType (final ExpandedUnitDetails xu);

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
	public int calculateUnitAverageRating (final AvailableUnit unit, final ExpandedUnitDetails xu, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;

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
	public boolean canAffordUnitMaintenance (final PlayerServerDetails player, final KnownWizardDetails wizardDetails, final List<PlayerServerDetails> players, final AvailableUnit unit,
		final SpellSetting spellSettings, final CommonDatabase db) throws RecordNotFoundException, PlayerNotFoundException, MomException;
}