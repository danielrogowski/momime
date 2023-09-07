package momime.server.ai;

import java.util.List;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.utils.ExpandedUnitDetails;

/**
 * Underlying methods that the AI uses to calculate ratings about how good units are
 */
public interface AIUnitRatingCalculations
{
	/**
	 * @param unit Unit to calculate value for
	 * @param xu Expanded unit details to calculate value for if already worked out, otherwise can pass null
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Value AI estimates for the current quality, usefulness and effectiveness of this unit
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public int calculateUnitCurrentRating (final AvailableUnit unit, final ExpandedUnitDetails xu, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;

	/**
	 * @param unit Unit to calculate value for
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Value AI estimates for the quality, usefulness and effectiveness that this unit has the potential to become
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public int calculateUnitPotentialRating (final AvailableUnit unit, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;

	/**
	 * If a unit stack is controlled by a wizard, bump up the rating of the unit stack to account for spells the wizard may cast to change the outcome of the battle
	 * 
	 * @param ratings Unit stack being evaluated
	 * @param players Players list
	 * @param wizards True wizard details list
	 * @return Bonus to add to unit stack rating
	 * @throws PlayerNotFoundException If the player who owns the unit stack cannot be found
	 * @throws RecordNotFoundException If the wizard who owns the unit stack cannot be found
	 */
	public int ratingBonusFromPowerBase (final AIUnitsAndRatings ratings, final List<PlayerServerDetails> players, final List<KnownWizardDetails> wizards)
		throws PlayerNotFoundException, RecordNotFoundException;
}