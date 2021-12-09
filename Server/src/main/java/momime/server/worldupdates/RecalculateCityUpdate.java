package momime.server.worldupdates;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.calculations.CityCalculations;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerCityCalculations;
import momime.server.fogofwar.FogOfWarMidTurnChanges;

/**
 * World update for recalculationg all city stats such as size and number of rebels 
 */
public final class RecalculateCityUpdate implements WorldUpdate
{
	/** Server-only city calculations */
	private ServerCityCalculations serverCityCalculations;
	
	/** City calculations */
	private CityCalculations cityCalculations;
	
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** The city location to check */
	private MapCoordinates3DEx cityLocation;
	
	/**
	 * @return Enum indicating which kind of update this is
	 */
	@Override
	public final KindOfWorldUpdate getKindOfWorldUpdate ()
	{
		return KindOfWorldUpdate.RECALCULATE_CITY;
	}
	
	/**
	 * @param o Other object to compare against
	 * @return Whether this and the other object hold the same values
	 */
	@Override
	public final boolean equals (final Object o)
	{
		final boolean e;
		if (o instanceof RecalculateCityUpdate)
			e = getCityLocation ().equals (((RecalculateCityUpdate) o).getCityLocation ());
		else
			e = false;
		
		return e;
	}
	
	/**
	 * @return String representation of class, for debug messages
	 */
	@Override
	public final String toString ()
	{
		return "Recalculate city at " + getCityLocation ();
	}
	
	/**
	 * Processes this update
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether this update was processed and/or generated any further updates
	 * @throws JAXBException If there is a problem sending some message to the client
	 * @throws XMLStreamException If there is a problem sending some message to the client
	 * @throws RecordNotFoundException If we find a game element (unit, building or so on) that we can't find the definition for in the DB
	 * @throws PlayerNotFoundException If we can't find the player who owns a game element
	 * @throws MomException If there are any issues with data or calculation logic
	 */
	@Override
	public final WorldUpdateResult process (final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		final MemoryGridCell tc = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ());
		
		final PlayerServerDetails cityOwner = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), tc.getCityData ().getCityOwnerID (), "RecalculateCityUpdate");
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) cityOwner.getPersistentPlayerPrivateKnowledge ();
		
		getServerCityCalculations ().calculateCitySizeIDAndMinimumFarmers (mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
			mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (), mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
			getCityLocation (), mom.getSessionDescription (), mom.getServerDB (), mom.getGeneralPublicKnowledge ().getConjunctionEventID ());

		tc.getCityData ().setNumberOfRebels (getCityCalculations ().calculateCityRebels (mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
			mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (),
			mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), getCityLocation (), priv.getTaxRateID (), mom.getServerDB ()).getFinalTotal ());

		getServerCityCalculations ().ensureNotTooManyOptionalFarmers (tc.getCityData ());

		// Send the updated city stats to any clients that can see the city
		getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getPlayers (),
			getCityLocation (), mom.getSessionDescription ().getFogOfWarSetting ());
		
		return WorldUpdateResult.DONE;
	}

	/**
	 * @return Server-only city calculations
	 */
	public final ServerCityCalculations getServerCityCalculations ()
	{
		return serverCityCalculations;
	}

	/**
	 * @param calc Server-only city calculations
	 */
	public final void setServerCityCalculations (final ServerCityCalculations calc)
	{
		serverCityCalculations = calc;
	}

	/**
	 * @return City calculations
	 */
	public final CityCalculations getCityCalculations ()
	{
		return cityCalculations;
	}

	/**
	 * @param calc City calculations
	 */
	public final void setCityCalculations (final CityCalculations calc)
	{
		cityCalculations = calc;
	}

	/**
	 * @return Server only helper methods for dealing with players in a session
	 */
	public final MultiplayerSessionServerUtils getMultiplayerSessionServerUtils ()
	{
		return multiplayerSessionServerUtils;
	}

	/**
	 * @param obj Server only helper methods for dealing with players in a session
	 */
	public final void setMultiplayerSessionServerUtils (final MultiplayerSessionServerUtils obj)
	{
		multiplayerSessionServerUtils = obj;
	}

	/**
	 * @return Methods for updating true map + players' memory
	 */
	public final FogOfWarMidTurnChanges getFogOfWarMidTurnChanges ()
	{
		return fogOfWarMidTurnChanges;
	}

	/**
	 * @param obj Methods for updating true map + players' memory
	 */
	public final void setFogOfWarMidTurnChanges (final FogOfWarMidTurnChanges obj)
	{
		fogOfWarMidTurnChanges = obj;
	}
	
	/**
	 * @return The city location to check
	 */
	public final MapCoordinates3DEx getCityLocation ()
	{
		return cityLocation;
	}

	/**
	 * @param l The city location to check
	 */
	public final void setCityLocation (final MapCoordinates3DEx l)
	{
		cityLocation = l;
	}
}