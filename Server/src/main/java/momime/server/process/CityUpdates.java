package momime.server.process;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;

import jakarta.xml.bind.JAXBException;
import momime.common.messages.CaptureCityDecisionID;
import momime.server.MomSessionVariables;

/**
 * Server side city updates
 */
public interface CityUpdates
{
	/**
	 * Handles all the updates and knock on effects when a city is captured or razed.
	 * Note after this method ends, it may have completely tore down the session, which we can tell because the players list will be empty. 
	 * 
	 * @param cityLocation Location of captured or razed city
	 * @param attackingPlayer Player who captured the city; if an outpost shrinks and disappears then its possible this can be null, but it must be specified if captureCityDecision is CAPTURE
	 * @param defendingPlayer Player who lost the city
	 * @param captureCityDecision Whether the city is being captured, razed or converted to ruins
	 * @param goldInRuin Only used if captureCityDecision is RUIN
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws IOException If there is another kind of problem
	 */
	public void conquerCity (final MapCoordinates3DEx cityLocation, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final CaptureCityDecisionID captureCityDecision, final int goldInRuin, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;
}