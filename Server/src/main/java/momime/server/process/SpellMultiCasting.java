package momime.server.process;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;

import jakarta.xml.bind.JAXBException;
import momime.common.database.Spell;
import momime.common.messages.servertoclient.AttackCitySpellResult;
import momime.server.MomSessionVariables;

/**
 * Handles casting spells that have more than one effect, and so need to call multiple methods in SpellCastingImpl
 */
public interface SpellMultiCasting
{
	/**
	 * Used for spells that can hit the units stationed in a city, destroy buildings and (sometimes) kill some of the population as well.  Earthquake + Call the Void.
	 * 
	 * @param spell Spell being cast
	 * @param castingPlayer Player who is casting it; can be null for city damage from events like Earthquake and Great Meteor
	 * @param eventID The event that caused an attack, if it wasn't initiated by a player
	 * @param variableDamage The damage chosen, for spells where variable mana can be channeled into casting them
	 * @param targetLocation The city being hit
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Counts of how many units, buildings and population were killed
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	public AttackCitySpellResult castCityAttackSpell (final Spell spell, final PlayerServerDetails castingPlayer, final String eventID,
		final Integer variableDamage, final MapCoordinates3DEx targetLocation, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;
}