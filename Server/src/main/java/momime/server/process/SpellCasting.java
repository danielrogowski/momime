package momime.server.process;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.messages.MemoryMaintainedSpell;
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
	 * @param castingPlayer Player who cast the attack spell
	 * @param spell Which attack spell they cast
	 * @param variableDamage The damage chosen, for spells where variable mana can be channeled into casting them
	 * @param targetLocations Location(s) where the spell is aimed
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void castOverlandAttackSpell (final PlayerServerDetails castingPlayer, final Spell spell, final Integer variableDamage,
		final List<MapCoordinates3DEx> targetLocations, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException;

	/**
	 * Rolls when a spell has a certain % chance of destroying each building in a city.  Used for Earthquake and Chaos Rift.
	 * 
	 * @param spellID The spell that is destroying the buildings
	 * @param castingPlayerID Who cast the spell
	 * @param percentageChance The % chance of each building being destroyed
	 * @param targetLocations The city(s) being targeted
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void rollChanceOfEachBuildingBeingDestroyed (final String spellID, final int castingPlayerID, final int percentageChance,
		final List<MapCoordinates3DEx> targetLocations, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException;
}