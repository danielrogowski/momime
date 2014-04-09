package momime.server.process;

import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.calculations.MomCityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.servertoclient.v0_9_5.KillUnitActionID;
import momime.common.messages.servertoclient.v0_9_5.TextPopupMessage;
import momime.common.messages.v0_9_5.MemoryGridCell;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.messages.v0_9_5.UnitSpecialOrder;
import momime.server.MomSessionVariables;
import momime.server.database.v0_9_4.MapFeature;
import momime.server.database.v0_9_4.Plane;
import momime.server.database.v0_9_4.TileType;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.utils.CityServerUtils;
import momime.server.utils.OverlandMapServerUtils;
import momime.server.utils.UnitServerUtils;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

/**
 * Processing methods specifically for dealing with simultaneous turns games
 */
public final class SimultaneousTurnsProcessingImpl implements SimultaneousTurnsProcessing
{
	/** Class logger */
	private final Logger log = Logger.getLogger (SimultaneousTurnsProcessingImpl.class.getName ());

	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** City calculations */
	private MomCityCalculations cityCalculations;
	
	/** City processing methods */
	private CityProcessing cityProcessing;

	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;

	/** Server-only overland map utils */
	private OverlandMapServerUtils overlandMapServerUtils;
	
	/** Server-only city utils */
	private CityServerUtils cityServerUtils;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/**
	 * Processes all unit & building special orders in a simultaneous turns game 'end phase'
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void processSpecialOrders (final MomSessionVariables mom)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException
	{
		log.entering (SimultaneousTurnsProcessingImpl.class.getName (), "processSpecialOrders");
		
		// Dismiss units with pending dismiss orders.
		// Regular units are killed outright, heroes are killed outright on the clients but return to 'Generated' status on the server.
		final List<MemoryUnit> dismisses = getUnitServerUtils ().listUnitsWithSpecialOrder (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), UnitSpecialOrder.DISMISS);
		for (final MemoryUnit trueUnit : dismisses)
		{
			final KillUnitActionID action;
			if (mom.getServerDB ().findUnit (trueUnit.getUnitID (), "processSpecialOrders-d").getUnitMagicRealm ().equals (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
				action = KillUnitActionID.HERO_DIMISSED_VOLUNTARILY;
			else
				action = KillUnitActionID.FREE;
			
			getFogOfWarMidTurnChanges ().killUnitOnServerAndClients (trueUnit, action, null,
				mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), mom.getSessionDescription ().getFogOfWarSetting (), mom.getServerDB ());
		}
		
		// Sell buildings
		for (final Plane plane : mom.getServerDB ().getPlane ())
			for (int x = 0; x < mom.getSessionDescription ().getMapSize ().getWidth (); x++)
				for (int y = 0; y < mom.getSessionDescription ().getMapSize ().getHeight (); y++)
				{
					final MemoryGridCell tc = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
						(plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x);
					
					if ((tc.getCityData () != null) && (tc.getBuildingIdSoldThisTurn () != null) && (tc.getCityData ().getCityPopulation () != null) &&
						(tc.getCityData ().getCityPopulation () > 0))
					{
						final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx ();
						cityLocation.setX (x);
						cityLocation.setY (y);
						cityLocation.setZ (plane.getPlaneNumber ());
						
						getCityProcessing ().sellBuilding (mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), cityLocation, tc.getBuildingIdSoldThisTurn (),
							false, true, mom.getSessionDescription (), mom.getServerDB ());
					}
				}
		
		// Get a list of all settlers with build orders.
		
		// Have to be careful here - two settlers (whether owned by the same or different players) can both be on
		// pending build city orders right next to each other - only one of them is going to be able to build a city
		// because the other will then be within 3 squares of the first city.
		
		// So process settlers in a random order, then a random settler will win the 'race' and get to settle the contested location.
		final List<MemoryUnit> settlers = getUnitServerUtils ().listUnitsWithSpecialOrder (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), UnitSpecialOrder.BUILD_CITY);
		while (settlers.size () > 0)
		{
			// Pick a random settler and remove them from the list
			final int settlerIndex = getRandomUtils ().nextInt (settlers.size ());
			final MemoryUnit settler = settlers.get (settlerIndex);
			settlers.remove (settlerIndex);
			
			// Find where the settler is
			final MemoryGridCell tc = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
				(settler.getUnitLocation ().getZ ()).getRow ().get (settler.getUnitLocation ().getY ()).getCell ().get (settler.getUnitLocation ().getX ());
			final TileType tileType = mom.getServerDB ().findTileType (tc.getTerrainData ().getTileTypeID (), "processSpecialOrders-t");
			final MapFeature mapFeature = (tc.getTerrainData ().getMapFeatureID () == null) ? null : mom.getServerDB ().findMapFeature
				(tc.getTerrainData ().getMapFeatureID (), "processSpecialOrders-f");
			
			final PlayerServerDetails settlerOwner = MultiplayerSessionServerUtils.findPlayerWithID (mom.getPlayers (), settler.getOwningPlayerID (), "processSpecialOrders-s");

			String error = null;
			if (!tileType.isCanBuildCity ())
				error = "The type of terrain here has changed, you can no longer build a city here";
			else if ((mapFeature != null) && (!mapFeature.isCanBuildCity ()))
				error = "The map feature here has changed, you can no longer build a city here";
			else if (getCityCalculations ().markWithinExistingCityRadius
				(mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
				settler.getUnitLocation ().getZ (), mom.getSessionDescription ().getMapSize ()).get (settler.getUnitLocation ().getX (), settler.getUnitLocation ().getY ()))
				
				error = "Another city was built before yours and is within " + mom.getSessionDescription ().getMapSize ().getCitySeparation () +
					" squares of where you are trying to build, so you cannot build here anymore";

			if (error != null)
			{
				// Show error
				log.warning (SimultaneousTurnsProcessingImpl.class.getName () + ".process: " + settlerOwner.getPlayerDescription ().getPlayerName () + " got an error: " + error);

				if (settlerOwner.getPlayerDescription ().isHuman ())
				{
					final TextPopupMessage reply = new TextPopupMessage ();
					reply.setText (error);
					settlerOwner.getConnection ().sendMessageToClient (reply);
				}
			}
			else
			{
				getCityServerUtils ().buildCityFromSettler (mom.getGeneralServerKnowledge (), settlerOwner, settler,
					mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());
			}			
		}
		
		// Get a list of all spirits with meld orders.
		// Have to be careful here - although only one player can have units at any one node, its perfectly valid to
		// put multiple spirits all on meld orders in the same turn, especially if trying to take a node from an
		// enemy wizard - that way can put say 4 spirits all on meld orders and have a good chance that one of them will succeed.
		final List<MemoryUnit> spirits = getUnitServerUtils ().listUnitsWithSpecialOrder (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), UnitSpecialOrder.MELD_WITH_NODE);
		while (spirits.size () > 0)
		{
			// Pick a random spirit and remove them from the list
			final int spiritIndex = getRandomUtils ().nextInt (spirits.size ());
			final MemoryUnit spirit = spirits.get (spiritIndex);
			spirits.remove (spiritIndex);

			// Find where the spirit is
			final MemoryGridCell tc = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
				(spirit.getUnitLocation ().getZ ()).getRow ().get (spirit.getUnitLocation ().getY ()).getCell ().get (spirit.getUnitLocation ().getX ());

			final PlayerServerDetails spiritOwner = MultiplayerSessionServerUtils.findPlayerWithID (mom.getPlayers (), spirit.getOwningPlayerID (), "processSpecialOrders-s");
			
			// Since nodes can't be changed to any other kind of terrain, only thing which can go wrong here is if we already own the node
			String error = null;
			if ((tc.getTerrainData () != null) && (spirit.getOwningPlayerID () == tc.getTerrainData ().getNodeOwnerID ()))
				error = "You've already captured this node";

			if (error != null)
			{
				// Show error
				log.warning (SimultaneousTurnsProcessingImpl.class.getName () + ".process: " + spiritOwner.getPlayerDescription ().getPlayerName () + " got an error: " + error);

				if (spiritOwner.getPlayerDescription ().isHuman ())
				{
					final TextPopupMessage reply = new TextPopupMessage ();
					reply.setText (error);
					spiritOwner.getConnection ().sendMessageToClient (reply);
				}
			}
			else
			{
				getOverlandMapServerUtils ().attemptToMeldWithNode (spirit, mom.getGeneralServerKnowledge ().getTrueMap (),
					mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());
			}			
		}
		
		log.exiting (SimultaneousTurnsProcessingImpl.class.getName (), "processSpecialOrders");
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
	 * @return City calculations
	 */
	public final MomCityCalculations getCityCalculations ()
	{
		return cityCalculations;
	}

	/**
	 * @param calc City calculations
	 */
	public final void setCityCalculations (final MomCityCalculations calc)
	{
		cityCalculations = calc;
	}
	
	/**
	 * @return Server-only unit utils
	 */
	public final UnitServerUtils getUnitServerUtils ()
	{
		return unitServerUtils;
	}

	/**
	 * @param utils Server-only unit utils
	 */
	public final void setUnitServerUtils (final UnitServerUtils utils)
	{
		unitServerUtils = utils;
	}

	/**
	 * @return Server-only overland map utils
	 */
	public final OverlandMapServerUtils getOverlandMapServerUtils ()
	{
		return overlandMapServerUtils;
	}
	
	/**
	 * @param utils Server-only overland map utils
	 */
	public final void setOverlandMapServerUtils (final OverlandMapServerUtils utils)
	{
		overlandMapServerUtils = utils;
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
	
	/**
	 * @return Random number generator
	 */
	public final RandomUtils getRandomUtils ()
	{
		return randomUtils;
	}

	/**
	 * @param utils Random number generator
	 */
	public final void setRandomUtils (final RandomUtils utils)
	{
		randomUtils = utils;
	}
}
