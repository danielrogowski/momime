package momime.server.process;

import java.util.List;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.FogOfWarSetting;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.FogOfWarMemory;
import momime.server.MomSessionVariables;

/**
 * Deals with any processing at the end of one player's turn in combat (after none of their units have any moves left)
 */
public interface CombatEndTurn
{
	/**
	 * Makes any rolls necessary at the start of a combat turn.  Note what this means by "combat turn" is different than what combatEndTurn below means.
	 * Here we mean before EITHER player has taken their turn, i.e. immediately before the defender gets a turn.
	 * 
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param combatLocation The location the combat is taking place
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	public void combatBeforeEitherTurn (final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final MapCoordinates3DEx combatLocation,
		final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException;
	
	/**
	 * Deals with any processing at the start of one player's turn in combat, before their movement is initialized.
	 * 
	 * @param combatLocation The location the combat is taking place
	 * @param playerID Which player is about to have their combat turn
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return List of units frozen in terror who will not get any movement allocation this turn; will return null (rather than an empty list) if the combat ends
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	public List<Integer> startCombatTurn (final MapCoordinates3DEx combatLocation, final int playerID,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException;
	
	/**
	 * Deals with any processing at the end of one player's turn in combat (after none of their units have any moves left) 
	 * 
	 * @param combatLocation The location the combat is taking place
	 * @param playerID Which player just finished their combat turn
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @param fogOfWarSettings Fog of War settings from session description
	 * @throws RecordNotFoundException If an expected data item cannot be found
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	public void combatEndTurn (final MapCoordinates3DEx combatLocation, final int playerID,
		final List<PlayerServerDetails> players, final FogOfWarMemory mem, final CommonDatabase db, final FogOfWarSetting fogOfWarSettings)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException;
}