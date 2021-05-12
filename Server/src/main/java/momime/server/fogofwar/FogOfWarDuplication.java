package momime.server.fogofwar;

import java.util.List;

import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;

/**
 * Methods for comparing and copying data from one source against a destination container
 * This is used for copying data from the server's true memory into player's memory
 *
 * Note these must always make deep copies - if an object includes another object (e.g. OverlandMapCoordinates or UnitHasSkill) then
 * we need to make a copy of the child object and reference that as well, we can't just copy the reference from source
 */
public interface FogOfWarDuplication
{
	/**
	 * Copies all the terrain and node aura related data items from source to destination
	 * @param source The map cell to copy from
	 * @param destination The map cell to copy to
	 * @return Whether any update actually happened (i.e. false if source and destination already had the same info)
	 */
	public boolean copyTerrainAndNodeAura (final MemoryGridCell source, final MemoryGridCell destination);

	/**
	 * Wipes all memory of the terrain at this location
	 * @param destination Map cell from player's memorized map
	 * @return True if an actual update was made; false if the player already knew nothing
	 */
	public boolean blankTerrainAndNodeAura (final MemoryGridCell destination);

	/**
	 * Copies all the city related data items from source to destination
	 * @param source The map cell to copy from
	 * @param destination The map cell to copy to
	 * @param includeCurrentlyConstructing Can see current construction project if its our city OR session description FOW option is set
	 * @param includeProductionSoFar Can see progress made on current construction project if its our city
	 * @return Whether any update actually happened (i.e. false if source and destination already had the same info)
	 */
	public boolean copyCityData (final MemoryGridCell source, final MemoryGridCell destination, final boolean includeCurrentlyConstructing, final boolean includeProductionSoFar);

	/**
	 * Wipes all memory of the city at this location
	 * @param destination Map cell from player's memorized map
	 * @return True if an actual update was made; false if the player already knew nothing
	 */
	public boolean blankCityData (final MemoryGridCell destination);

	/**
	 * Copies a building from source into the destination list
	 * @param source The building to copy from (i.e. the true building details)
	 * @param destination The building list to copy into (i.e. the player's memory of buildings)
	 * @return Whether any update actually happened (i.e. false if the building was already in the list)
	 */
	public boolean copyBuilding (final MemoryBuilding source, final List<MemoryBuilding> destination);

	/**
	 * Copies a unit from source into the destination list
	 * @param source The unit to copy from (i.e. the true unit details)
	 * @param destination The building list to copy into (i.e. the player's memory of buildings)
	 * @param includeMovementFields Only the player who owns a unit can see its movement remaining and special orders
	 * @return Whether any update actually happened (i.e. false if the unit was already in the list AND all the details already exactly matched)
	 */
	public boolean copyUnit (final MemoryUnit source, final List<MemoryUnit> destination, final boolean includeMovementFields);

	/**
	 * Copies a spell from source into the destination list
	 * @param source The spell to copy from (i.e. the true spell details)
	 * @param destination The spell list to copy into (i.e. the player's memory of spells)
	 * @return Whether any update actually happened (i.e. false if the spell was already in the list AND all the details already exactly matched)
	 */
	public boolean copyMaintainedSpell (final MemoryMaintainedSpell source, final List<MemoryMaintainedSpell> destination);

	/**
	 * Copies a CAE from source into the destination list
	 * @param source The CAE to copy from (i.e. the true CAE details)
	 * @param destination The CAE list to copy into (i.e. the player's memory of CAEs)
	 * @return Whether any update actually happened (i.e. false if the CAE was already in the list AND all the details already exactly matched)
	 */
	public boolean copyCombatAreaEffect (final MemoryCombatAreaEffect source, final List<MemoryCombatAreaEffect> destination);
}