package momime.server.ai;

import java.util.List;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.SpellSvr;

/**
 * Methods that the AI uses to calculate stats about types of spells it might want to cast
 */
public interface AISpellCalculations
{
	/**
	 * @param player Player who wants to cast a spell
	 * @param players Players list
	 * @param spell Spell they want to cast
	 * @param db Lookup lists built over the XML database
	 * @return Whether the player can afford maintence cost of the spell after it is cast
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public boolean canAffordSpellMaintenance (final PlayerServerDetails player, final List<PlayerServerDetails> players, final SpellSvr spell, final ServerDatabaseEx db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;
}