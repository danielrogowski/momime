package momime.server.ai;

import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.PlayerKnowledgeUtils;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.server.database.ServerDatabaseLookup;
import momime.server.database.v0_9_4.Plane;
import momime.server.fogofwar.FogOfWarMidTurnChanges;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Overall AI strategy + control
 */
public final class MomAI
{
	/**
	 *
	 * @param player AI player whose turn to take
	 * @param players List of players in the session
	 * @param trueMap True map details
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws RecordNotFoundException If we can't find the race inhabiting the city, or various buildings
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws PlayerNotFoundException If we can't find the player who owns a unit
	 * @throws MomException If we find a consumption value that is not an exact multiple of 2, or we find a production value that is not an exact multiple of 2 that should be
	 */
	public final static void aiPlayerTurn (final PlayerServerDetails player, final List<PlayerServerDetails> players, final FogOfWarMemory trueMap,
		final MomSessionDescription sd, final ServerDatabaseLookup db, final Logger debugLogger)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		debugLogger.entering (MomAI.class.getName (), "aiPlayerTurn", player.getPlayerDescription ().getPlayerName ());

		// Decide what to build in all of this players' cities
		// Note we do this EVERY TURN - we don't wait for the previous building to complete - this allows the AI player to change their mind
		// e.g. if a city has minimal defence and has a university almost built and then a
		// group of halbardiers show up 2 squares away, you're going to want to stuff the university and rush buy the best unit you can afford
		for (final Plane plane : db.getPlanes ())
			for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
				for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
				{
					final OverlandMapCityData cityData = trueMap.getMap ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x).getCityData ();
					if ((cityData != null) && (cityData.getCityPopulation () != null) && (cityData.getCityOwnerID () != null) &&
						(cityData.getCityPopulation () > 0) && (cityData.getCityOwnerID () == player.getPlayerDescription ().getPlayerID ()))
					{
						final OverlandMapCoordinates cityLocation = new OverlandMapCoordinates ();
						cityLocation.setX (x);
						cityLocation.setY (y);
						cityLocation.setPlane (plane.getPlaneNumber ());

						CityAI.decideWhatToBuild (cityLocation, cityData, trueMap.getMap (), trueMap.getBuilding (), sd, db, debugLogger);
						FogOfWarMidTurnChanges.updatePlayerMemoryOfCity (trueMap.getMap (), players, cityLocation, sd, debugLogger);
					}
				}

		// This relies on knowing what's being built in each city, so do it 2nd
		CityAI.setOptionalFarmersInAllCities (trueMap, players, player, db, sd, debugLogger);

		// Do we need to choose a spell to research?
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		if ((PlayerKnowledgeUtils.isWizard (pub.getWizardID ())) && (priv.getSpellIDBeingResearched () == null))
			SpellAI.decideWhatToResearch (player, db, debugLogger);

		debugLogger.exiting (MomAI.class.getName (), "aiPlayerTurn", player.getPlayerDescription ().getPlayerName ());
	}

	/**
	 * Prevent instantiation
	 */
	private MomAI ()
	{
	}
}
