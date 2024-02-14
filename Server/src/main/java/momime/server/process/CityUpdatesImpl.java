package momime.server.process;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;

import jakarta.xml.bind.JAXBException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.CaptureCityDecisionID;
import momime.common.utils.MemoryBuildingUtils;
import momime.server.MomSessionVariables;
import momime.server.utils.CityServerUtils;

/**
 * Server side city updates
 */
public final class CityUpdatesImpl implements CityUpdates
{
	/** City processing methods */
	private CityProcessing cityProcessing;
	
	/** MemoryBuilding utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** Server-only city utils */
	private CityServerUtils cityServerUtils;
	
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
	@Override
	public final void conquerCity (final MapCoordinates3DEx cityLocation, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final CaptureCityDecisionID captureCityDecision, final int goldInRuin, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		// Before we remove buildings, check if this was the wizard's fortress and/or summoning circle
		boolean wasWizardsFortress = (getMemoryBuildingUtils ().findBuilding
			(mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (), cityLocation, CommonDatabaseConstants.BUILDING_FORTRESS) != null);
		
		final boolean wasSummoningCircle = (captureCityDecision != null) && (getMemoryBuildingUtils ().findBuilding
			(mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (), cityLocation, CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE) != null);
		
		// Deal with cities
		if (captureCityDecision == CaptureCityDecisionID.CAPTURE)
			getCityProcessing ().captureCity (cityLocation, attackingPlayer, defendingPlayer, mom);
		
		else if (captureCityDecision == CaptureCityDecisionID.RAZE)
			getCityProcessing ().razeCity (cityLocation, mom);

		else if (captureCityDecision == CaptureCityDecisionID.RUIN)
			getCityProcessing ().ruinCity (cityLocation, goldInRuin, mom);
		
		// If they're already banished and this was their last city being taken, then treat it just like their wizard's fortress being taken
		boolean canStealSpells = true;
		if ((!wasWizardsFortress) && (captureCityDecision != null) &&
			(getCityServerUtils ().countCities (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), defendingPlayer.getPlayerDescription ().getPlayerID (), true) == 0))
		{
			wasWizardsFortress = true;
			canStealSpells = false;		// Except their wizard's fortress was not really taken, so you don't steal spells / MP from it
		}
		
		// Deal with wizard being banished
		if ((wasWizardsFortress) && (captureCityDecision != CaptureCityDecisionID.RAMPAGE))
			getCityProcessing ().banishWizard (attackingPlayer, defendingPlayer, canStealSpells, mom);
		
		// From here on have to be really careful, as banishWizard may have completely tore down the session, which we can tell because the players list will be empty
		
		// If their summoning circle was taken, but they still have their fortress elsewhere, then move summoning circle to there
		if ((wasSummoningCircle) && (captureCityDecision != CaptureCityDecisionID.RAMPAGE) && (mom.getPlayers ().size () > 0))
			getCityProcessing ().moveSummoningCircleToWizardsFortress (defendingPlayer.getPlayerDescription ().getPlayerID (), mom);
	}

	/**
	 * @return City processing methods
	 */
	public final CityProcessing getCityProcessing ()
	{
		return cityProcessing;
	}

	/**
	 * @param obj City processing methods
	 */
	public final void setCityProcessing (final CityProcessing obj)
	{
		cityProcessing = obj;
	}

	/**
	 * @return MemoryBuilding utils
	 */
	public final MemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param utils MemoryBuilding utils
	 */
	public final void setMemoryBuildingUtils (final MemoryBuildingUtils utils)
	{
		memoryBuildingUtils = utils;
	}

	/**
	 * @return Server-only city utils
	 */
	public final CityServerUtils getCityServerUtils ()
	{
		return cityServerUtils;
	}

	/**
	 * @param utils Server-only city utils
	 */
	public final void setCityServerUtils (final CityServerUtils utils)
	{
		cityServerUtils = utils;
	}
}