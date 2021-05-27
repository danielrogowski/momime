package momime.server.calculations;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.FogOfWarSetting;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.UnitEx;
import momime.common.messages.CombatMapSize;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;
import momime.common.utils.ExpandedUnitDetails;

/**
 * Server only calculations pertaining to units, e.g. calculations relating to fog of war
 */
public interface ServerUnitCalculations
{
	/**
	 * @param unit The unit to check
	 * @param db Lookup lists built over the XML database
	 * @return How many squares this unit can see; by default = 1, flying units automatically get 2, and the Scouting unit skill can push this even higher
	 * @throws RecordNotFoundException If we can't find the player who owns the unit, or the unit has a skill that we can't find in the cache
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	public int calculateUnitScoutingRange (final ExpandedUnitDetails unit, final CommonDatabase db) throws RecordNotFoundException, MomException;

	/**
	 * Rechecks that transports have sufficient space to hold all units for whom the terrain is impassable.
	 * This is used after naval combats where some of the transports may have died, to kill off any surviving units who now have no transport,
	 * or perhaps a unit had Flight cast on it which was dispelled during combat.
	 * 
	 * @param combatLocation The combatLocation where the units need to be rechecked
	 * @param players List of players in this session, this can be passed in null for when units are being added to the map pre-game
	 * @param trueMap True terrain, buildings, spells and so on as known only to the server
	 * @param fogOfWarSettings Fog of war settings from session description
	 * @param db Lookup lists built over the XML database
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void recheckTransportCapacity (final MapCoordinates3DEx combatLocation, final FogOfWarMemory trueMap,
		final List<PlayerServerDetails> players, final FogOfWarSetting fogOfWarSettings, final CommonDatabase db)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException;

	/**
	 * Non-magical ranged attack incurr a -10% to hit penalty for each 3 tiles distance between the attacking and defending unit on the combat map.
	 * This is loosely explained in the manual and strategy guide, but the info on the MoM wiki is clearer.
	 * 
	 * @param attacker Unit firing the ranged attack
	 * @param defender Unit being shot
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @return To hit penalty incurred from the distance between the attacker and defender, NB. this is not capped in any way so may get very high values here
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	public int calculateRangedAttackDistancePenalty (final ExpandedUnitDetails attacker, final ExpandedUnitDetails defender,
		final CombatMapSize combatMapCoordinateSystem) throws MomException;
	
	/**
	 * Gets a list of all the units a summoning spell might summon if we cast it.  That's straightforward for normal summoning spells, but heroes can only be
	 * hired once and if killed are never available to summon again.  Plus some heroes are restricted depending on what our spell book picks are.
	 * 
	 * @param spell Summoning spell
	 * @param player Player casting the spell
	 * @param trueUnits List of true units
	 * @param db Lookup lists built over the XML database
	 * @return List of units this spell might summon if we cast it; list can be empty if we're already summoned and killed all heroes for example
	 * @throws RecordNotFoundException If one of the summoned unit IDs can't be found in the DB
	 */
	public List<UnitEx> listUnitsSpellMightSummon (final Spell spell, final PlayerServerDetails player, final List<MemoryUnit> trueUnits, final CommonDatabase db)
		throws RecordNotFoundException;
	
	/**
	 * Similar to listUnitsSpellMightSummon, except lists all heroes who haven't been killed, and who we have the necessary spell book picks for. 
	 * 
	 * @param player Player recruiting heroes
	 * @param trueUnits List of true units
	 * @param db Lookup lists built over the XML database
	 * @return List of heroes available to us
	 */
	public List<UnitEx> listHeroesForHire (final PlayerServerDetails player, final List<MemoryUnit> trueUnits, final CommonDatabase db);
}