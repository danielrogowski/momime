package momime.server.process;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.servertoclient.OverlandCastingInfo;
import momime.server.MomSessionVariables;

/**
 * Methods for casting specific types of spells
 */
public interface SpellCasting
{
	/**
	 * Processes casting a summoning spell overland, finding where there is space for the unit to go and adding it
	 * 
	 * @param spell Summoning spell
	 * @param player Player who is casting it
	 * @param summonLocation Location where the unit will appear (or try to)
	 * @param sendNewTurnMessage Notify player about the summoned unit on NTM scroll?
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void castOverlandSummoningSpell (final Spell spell, final PlayerServerDetails player, final MapCoordinates3DEx summonLocation,
		final boolean sendNewTurnMessage, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException;

	/**
	 * Normally the spells being cast by other wizards are private, but we get to see this info if we have Detect Magic or Spell Blast cast.
	 * 
	 * @param ourSpellID Which spell allows us to see the info - Detect Magic or Spell Blast
	 * @param onlyOnePlayerID If zero, will send to all players who have Detect Magic cast; if specified will send only to the specified player
	 * @param players List of players in the session
	 * @param spells List of known spells
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void sendOverlandCastingInfo (final String ourSpellID, final int onlyOnePlayerID, final List<PlayerServerDetails> players,
		final List<MemoryMaintainedSpell> spells) throws JAXBException, XMLStreamException;
	
	/**
	 * @param player Player to create casting info for
	 * @param ourSpellID Which spell allows us to see the info - Detect Magic or Spell Blast
	 * @return Summary details about what the wizard is casting overland
	 */
	public OverlandCastingInfo createOverlandCastingInfo (final PlayerServerDetails player, final String ourSpellID);
	
	/**
	 * Processes casting an attack spell overland that hits all units in a stack (that are valid targets), or all units in multiple stacks.
	 * If multiple targetLocations are specified then the units may not all belong to the same player.
	 * 
	 * @param castingPlayer Player who cast the attack spelll; can be null if not being cast by a player
	 * @param eventID The event that caused an attack, if it wasn't initiated by a player
	 * @param spell Which attack spell they cast
	 * @param variableDamage The damage chosen, for spells where variable mana can be channeled into casting them
	 * @param targetLocations Location(s) where the spell is aimed
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Number of units that were killed
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public int castOverlandAttackSpell (final PlayerServerDetails castingPlayer, final String eventID, final Spell spell, final Integer variableDamage,
		final List<MapCoordinates3DEx> targetLocations, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException;

	/**
	 * Rolls when a spell has a certain % chance of destroying each building in a city.  Used for Earthquake and Chaos Rift.
	 * 
	 * @param spellID The spell that is destroying the buildings
	 * @param castingPlayerID Who cast the spell; null if not from a spell 
	 * @param percentageChance The % chance of each building being destroyed
	 * @param targetLocations The city(s) being targeted
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Number of buildings that were destroyed
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public int rollChanceOfEachBuildingBeingDestroyed (final String spellID, final Integer castingPlayerID, final int percentageChance,
		final List<MapCoordinates3DEx> targetLocations, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException;

	/**
	 * @param targetLocation Tile to corrupt
	 * @param trueMap True terrain, buildings, spells and so on as known only to the server
	 * @param players List of players in the session
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void corruptTile (final MapCoordinates3DEx targetLocation, final FogOfWarMemory trueMap,
		final List<PlayerServerDetails> players, final MomSessionDescription sd, final CommonDatabase db)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException;
	
	/**
	 * Change a change tile type kind of spell, like Change Terrain or Raise Volcano
	 * 
	 * @param spell Which spell was cast
	 * @param targetLocation Tile to change
	 * @param castingPlayerID Player who cast the spell
	 * @param trueMap True terrain, buildings, spells and so on as known only to the server
	 * @param players List of players in the session
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @param conjunctionEventID Currently active conjunction, if there is one
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void changeTileType (final Spell spell, final MapCoordinates3DEx targetLocation, final int castingPlayerID, final FogOfWarMemory trueMap,
		final List<PlayerServerDetails> players, final MomSessionDescription sd, final CommonDatabase db, final String conjunctionEventID)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException;
}