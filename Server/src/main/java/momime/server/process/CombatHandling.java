package momime.server.process;

import java.util.List;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.utils.ExpandedUnitDetails;

/**
 * More methods dealing with executing combats
 */
public interface CombatHandling
{
	/**
	 * Checks to see if anything special needs to happen when a unit crosses over the border between two combat tiles
	 * 
	 * @param xu Unit that is moving across a border
	 * @param combatLocation Location where the combat is taking place
	 * @param combatMap Combat scenery
	 * @param moveFrom Cell being moved from
	 * @param moveTo Cell moving into
	 * @param trueSpells True spell details held on server
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If we can't find one of the border definitions
	 */
	public void crossCombatBorder (final ExpandedUnitDetails xu, final MapCoordinates3DEx combatLocation, final MapAreaOfCombatTiles combatMap,
		final MapCoordinates2DEx moveFrom, final MapCoordinates2DEx moveTo,
		final List<MemoryMaintainedSpell> trueSpells, final CommonDatabase db)
		throws RecordNotFoundException;
}