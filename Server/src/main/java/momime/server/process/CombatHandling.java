package momime.server.process;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MemoryUnit;
import momime.common.utils.ExpandedUnitDetails;
import momime.server.MomSessionVariables;

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
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param moveFrom Cell being moved from
	 * @param moveTo Cell moving into
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether the final unit on one side of combat burned itself to death hence letting the other side win
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	public boolean crossCombatBorder (final ExpandedUnitDetails xu, final MapCoordinates3DEx combatLocation, final MapAreaOfCombatTiles combatMap,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final MapCoordinates2DEx moveFrom, final MapCoordinates2DEx moveTo, final MomSessionVariables mom)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException;
	
	/**
	 * Checks to see if a Magic Vortex hits any units directly under it or adjacent to it.  It will attack the side who owns it as well.
	 * 
	 * @param vortex The vortex to check damage from
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether the vortex killed the last unit on one or other side of the combat and ended it or not
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	public boolean damageFromVortex (final MemoryUnit vortex, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException;
}