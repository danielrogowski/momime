package momime.server.mapgenerator;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.server.MomSessionVariables;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Server only class which contains all the code for generating a random overland map
 *
 * There's no need to run this in a separate thread like in Delphi, because with the entire server XML file cached this version runs considerably faster
 */
public interface OverlandMapGenerator
{
	/**
	 * Main routine to generate the overland terrain map
	 * @throws MomException If some fatal error happens during map generation
	 * @throws RecordNotFoundException If some entry isn't found in the db during map generation, or one of the smoothing borders isn't found in the fixed arrays
	 */
	public void generateOverlandTerrain () throws MomException, RecordNotFoundException;
	
	/**
	 * Creates the initial combat area effects from the map scenery i.e. node auras
	 * This is an entirely separate process from the terrain generation, and runs separately after the terrain generation has finished
	 * 
	 * @throws RecordNotFoundException If we encounter a combat area effect that we can't find in the cache
	 * @throws JAXBException This only gets generated if addCombatAreaEffectOnServerAndClients tries to send into to players, but we pass null for player list, so won't happen
	 * @throws XMLStreamException This only gets generated if addCombatAreaEffectOnServerAndClients tries to send into to players, but we pass null for player list, so won't happen
	 */
	public void generateInitialCombatAreaEffects ()
		throws RecordNotFoundException, JAXBException, XMLStreamException;

	/**
	 * Fills all nodes, lairs and towers of wizardry on the map with random monsters
	 * This is really separate from the rest of the methods in this class which are to do with generating the terrain
	 * However its still to do with generating the map so this class is still the most sensible place for it
	 *
	 * @param monsterPlayer Player who owns the monsters we add
	 * @throws RecordNotFoundException If we encounter any records that can't be found in the cache
	 * @throws MomException If the unit's skill list ends up containing the same skill twice
	 * @throws PlayerNotFoundException This only gets generated if addUnitOnServerAndClients tries to send into to players, but we pass null for player list, so won't happen
	 * @throws JAXBException This only gets generated if addUnitOnServerAndClients tries to send into to players, but we pass null for player list, so won't happen
	 * @throws XMLStreamException This only gets generated if addUnitOnServerAndClients tries to send into to players, but we pass null for player list, so won't happen
	 */
	public void fillNodesLairsAndTowersWithMonsters (final PlayerServerDetails monsterPlayer)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException;

	/**
	 * Tries to find a node or lair to generate rampaging monsters from
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If an expected data item can't be found
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void generateRampagingMonsters (final MomSessionVariables mom)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException;
}