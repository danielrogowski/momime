package momime.server.ai;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

import momime.server.database.SpellSvr;

/**
 * Methods that the AI uses to calculate stats about types of spells it might want to cast
 */
public interface AISpellCalculations
{
	/**
	 * @param player Player who wants to cast a spell
	 * @param spell Spell they want to cast
	 * @return Whether the player can afford maintence cost of the spell after it is cast
	 */
	public boolean canAffordSpellMaintenance (final PlayerServerDetails player, final SpellSvr spell);
}