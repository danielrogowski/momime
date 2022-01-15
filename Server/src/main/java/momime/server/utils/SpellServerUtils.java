package momime.server.utils;

import java.util.List;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SwitchResearch;
import momime.common.messages.MemoryUnit;
import momime.server.MomSessionVariables;

/**
 * Server side methods dealing with researching and casting spells
 */
public interface SpellServerUtils
{
	/**
	 * @param player Player who wants to switch research
	 * @param spellID Spell that we want to research
	 * @param switchResearch Switch research option from session description
	 * @param db Lookup lists built over the XML database
	 * @return null if choice is acceptable; message to send back to client if choice isn't acceptable
	 * @throws RecordNotFoundException If either the spell we want to research now, or the spell previously being researched, can't be found
	 */
	public String validateResearch (final PlayerServerDetails player, final String spellID,
		final SwitchResearch switchResearch, final CommonDatabase db) throws RecordNotFoundException;

	/**
	 * Generates list of target units for Great Unsummoning and Death Wish
	 * 
	 * @param spell Spell being cast
	 * @param castingPlayer Player who is casting the spell
	 * @param isTargeting True if calling this method to allow the player to target something at the unit, which means they must be able to see it,
	 * 	False if resolving damage - for example a unit we can't see is not a valid target to select, but if its hit by an area attack like ice storm, then we do damage it
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return List of target units
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public List<MemoryUnit> listGlobalAttackTargets (final Spell spell, final PlayerServerDetails castingPlayer,
		final boolean isTargeting, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;
}