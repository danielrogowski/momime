package momime.client.utils;

import java.io.IOException;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.client.ui.renderer.CastCombatSpellFrom;
import momime.common.database.Spell;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MemoryUnit;
import momime.common.messages.clienttoserver.RequestCastSpellMessage;

/**
 * Handlers for mouse move and mouse click events in CombatUI relating to targeting spells
 */
public interface CombatSpellClientUtils
{
	/**
	 * Handles doing final validation and building up the request message to cast a combat spell when the player clicks a tile in the combat map.
	 * So this won't be called for combat spells that don't require any targeting, e.g. combat enchantments like Prayer.
	 * 
	 * @param spell The spell being cast
	 * @param combatLocation Where the combat is taking place
	 * @param combatURN Unique identifier for the combat, for sending messages back to the server
	 * @param combatCoords The tile within the combat map where the player wants to target the spell
	 * @param castingSource Source that is currently casting a combat spell
	 * @param combatTerrain Combat terrain
	 * @param unitBeingRaised If casting a raise dead spell, which unit the player chose to raise
	 * @param showErrorMessage If its not a valid cast, show message box to the player explaining why not?
	 * @return Message to send to server to request spell cast if it is valid; if it is not valid for some reason then returns null
	 * @throws IOException If there is a problem
	 */
	public RequestCastSpellMessage buildCastCombatSpellMessage (final Spell spell, final MapCoordinates3DEx combatLocation, final int combatURN,
		final MapCoordinates2DEx combatCoords, final CastCombatSpellFrom castingSource, final MapAreaOfCombatTiles combatTerrain,
		final MemoryUnit unitBeingRaised, final boolean showErrorMessage) throws IOException;

	/**
	 * As player is moving the mouse around the combat terrain, test whether the tile they are holding the mouse over at the moment
	 * is a vaild place to cast the spell or not.
	 * 
	 * Basically this is exactly the same logic as in buildCastCombatSpellMessage, except that we aren't building up the message.
	 * 
	 * @param spell The spell being cast
	 * @param combatLocation Where the combat is taking place
	 * @param combatURN Unique identifier for the combat, for sending messages back to the server
	 * @param combatCoords The tile within the combat map where the player wants to target the spell
	 * @param castingSource Source that is currently casting a combat spell
	 * @param combatTerrain Combat terrain
	 * @param unitBeingRaised If casting a raise dead spell, which unit the player chose to raise
	 * @return Whether the desired target tile is a valid place to cast the spell or not
	 * @throws IOException If there is a problem
	 */
	public boolean isCombatTileValidTargetForSpell (final Spell spell, final MapCoordinates3DEx combatLocation, final int combatURN,
		final MapCoordinates2DEx combatCoords, final CastCombatSpellFrom castingSource, final MapAreaOfCombatTiles combatTerrain,
		final MemoryUnit unitBeingRaised) throws IOException;
}