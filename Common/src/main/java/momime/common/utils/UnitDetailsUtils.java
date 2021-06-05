package momime.common.utils;

import java.util.List;

import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.AvailableUnit;
import momime.common.messages.MemoryCombatAreaEffect;

/**
 * Methods for working out minimal unit details
 */
public interface UnitDetailsUtils
{
	/**
	 * Calculates minimal unit details that can be derived quickly without examining the whole unit stack.
	 * 
	 * @param unit Unit to expand skill list for
	 * @param players Players list
	 * @param combatAreaEffects List of known combat area effects
	 * @param db Lookup lists built over the XML database
	 * @return List of all skills this unit has, with skills granted from other skills and skills granted from spells merged into the list
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public MinimalUnitDetails expandMinimalUnitDetails (final AvailableUnit unit,
		final List<? extends PlayerPublicDetails> players, final List<MemoryCombatAreaEffect> combatAreaEffects, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;
}