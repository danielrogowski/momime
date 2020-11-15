package momime.server.fogofwar;

import java.util.List;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.database.CommonDatabase;
import momime.common.database.FogOfWarSetting;
import momime.common.database.FogOfWarValue;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MapVolumeOfFogOfWarStates;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;

/**
 * Contains methods for whether each player can see certain game items during a turn;
 * i.e. methods for when the true values change (or are added or removed) but the visible area that each player can see does not change.
 * 
 * This is used by FogOfWarMidTurnChangesImpl and kept seperate to allow mocking out methods in unit tests.
 */
public interface FogOfWarMidTurnVisibility
{
	/**
	 * @param unit True unit to test
	 * @param trueTerrain True terrain map
	 * @param player The player we are testing whether they can see the unit
	 * @param db Lookup lists built over the XML database
	 * @param fogOfWarSettings Fog of war settings from session description
	 * @return True if player can see this unit
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found
	 * @throws PlayerNotFoundException If the player who owns the unit cannot be found
	 */
	public boolean canSeeUnitMidTurn (final MemoryUnit unit, final MapVolumeOfMemoryGridCells trueTerrain, final PlayerServerDetails player,
		final CommonDatabase db, final FogOfWarSetting fogOfWarSettings)
		throws RecordNotFoundException, PlayerNotFoundException;

	/**
	 * @param spell True spell to test
	 * @param trueTerrain True terrain map
	 * @param trueUnits True list of units
	 * @param player The player we are testing whether they can see the spell
	 * @param db Lookup lists built over the XML database
	 * @param fogOfWarSettings Fog of war settings from session description
	 * @return True if player can see this spell
	 * @throws RecordNotFoundException If the unit that the spell is cast on, or tile type or map feature IDs cannot be found
	 * @throws PlayerNotFoundException If the player who owns the unit cannot be found
	 */
	public boolean canSeeSpellMidTurn (final MemoryMaintainedSpell spell,
		final MapVolumeOfMemoryGridCells trueTerrain, final List<MemoryUnit> trueUnits, final PlayerServerDetails player,
		final CommonDatabase db, final FogOfWarSetting fogOfWarSettings)
		throws RecordNotFoundException, PlayerNotFoundException;

	/**
	 * @param cae True CAE to test
	 * @param fogOfWarArea Area the player can/can't see, outside of FOW recalc
	 * @param setting FOW CAE setting, from session description
	 * @return True if player can see this CAE
	 */
	public boolean canSeeCombatAreaEffectMidTurn (final MemoryCombatAreaEffect cae,
		final MapVolumeOfFogOfWarStates fogOfWarArea, final FogOfWarValue setting);
}