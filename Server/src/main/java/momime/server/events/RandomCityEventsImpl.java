package momime.server.events;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.SquareMapDirection;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.calculations.CityCalculationsImpl;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Event;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.UnitEx;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitStatusID;
import momime.common.messages.WizardState;
import momime.common.messages.servertoclient.AttackCitySpellResult;
import momime.common.messages.servertoclient.UpdateWizardStateMessage;
import momime.common.utils.MemoryBuildingUtils;
import momime.server.MomSessionVariables;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.FogOfWarProcessing;
import momime.server.fogofwar.KillUnitActionID;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.process.CityProcessing;
import momime.server.process.SpellMultiCasting;

/**
 * Deals with random events which pick a city target:
 * Great Meteor, Earthquake, Plague, Rebellion, Depletion, New Minerals, Population Boom
 * Also Diplomatic Marriage, but here we're picking a raider city rather than one owned by a wizard
 */
public final class RandomCityEventsImpl implements RandomCityEvents
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (RandomCityEventsImpl.class);
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/** Rolls random events */
	private RandomEvents randomEvents;
	
	/** FOW single changes */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;
	
	/** City processing methods */
	private CityProcessing cityProcessing;
	
	/** Casting spells that have more than one effect */
	private SpellMultiCasting spellMultiCasting;
	
	/** Main FOW update routine */
	private FogOfWarProcessing fogOfWarProcessing;
	
	/**
	 * Can only call this on events that are targeted at cities
	 * 
	 * @param event Event we want to find a target for
	 * @param cityLocation Location we are considering for the event
	 * @param cityOwnerID Will only consider cities with the specified owner
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether the city is a valid target for the event or not
	 * @throws RecordNotFoundException If we can't find the definition for a unit stationed in the city
	 */
	@Override
	public final boolean isCityValidTargetForEvent (final Event event, final MapCoordinates3DEx cityLocation, final int cityOwnerID, final MomSessionVariables mom)
		throws RecordNotFoundException
	{
		boolean valid = false;

		final OverlandMapCityData cityData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();
		if ((cityData != null) && (cityData.getCityOwnerID () == cityOwnerID) && (cityData.getCityPopulation () >= 1000))
		{
			// Do we need to make sure there's a mineral in the area?  Depletion
			if (event.getEventID ().equals (CommonDatabaseConstants.EVENT_ID_DEPLETION))
			{
				final MapCoordinates3DEx coords = new MapCoordinates3DEx (cityLocation);
				for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
					if (getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, direction.getDirectionID ()))
					{
						final OverlandMapTerrainData terrainData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
							(coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
						if (event.getEventMapFeatureID ().contains (terrainData.getMapFeatureID ()))
							valid = true;
					}
			}
			
			// Do we need to make sure there's a hill or mountain in the area without a mineral?  New Minerals
			else if (event.getEventID ().equals (CommonDatabaseConstants.EVENT_ID_NEW_MINERALS))
			{
				final MapCoordinates3DEx coords = new MapCoordinates3DEx (cityLocation);
				for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
					if (getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, direction.getDirectionID ()))
					{
						final OverlandMapTerrainData terrainData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
							(coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
						if ((event.getEventTileTypeID ().contains (terrainData.getTileTypeID ())) && (terrainData.getMapFeatureID () == null))
							valid = true;
					}
			}
			
			// Do we need to make sure it isn't the wizard's fortress, there's no heroes, and not too many summoned units here?  Rebellion
			else if (event.getEventID ().equals (CommonDatabaseConstants.EVENT_ID_REBELLION))
			{
				if (getMemoryBuildingUtils ().findBuilding (mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (),
					cityLocation, CommonDatabaseConstants.BUILDING_FORTRESS) == null)
				{
					// How many of each type of unit type are here?
					boolean heroFound = false;
					int normalUnitCount = 0;
					int summonedUnitCount = 0;
					
					final Iterator<MemoryUnit> iter = mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ().iterator ();
					while ((!heroFound) && (iter.hasNext ()))
					{
						final MemoryUnit mu = iter.next ();
						if ((cityLocation.equals (mu.getUnitLocation ())) && (mu.getStatus () == UnitStatusID.ALIVE))
						{
							final UnitEx unitDef = mom.getServerDB ().findUnit (mu.getUnitID (), "isCityValidTargetForEvent");
							if (unitDef.getUnitMagicRealm ().equals (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
								heroFound = true;
							else if (unitDef.getUnitMagicRealm ().equals (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL))
								normalUnitCount++;
							else
								summonedUnitCount++;							
						}
					}
					
					valid = (!heroFound) && (summonedUnitCount <= normalUnitCount);
				}
			}
			
			// Population Boom and Plague can only be applied if the city doesn't already have some other population effect in place
			else if ((event.getEventID ().equals (CommonDatabaseConstants.EVENT_ID_POPULATION_BOOM)) ||
				(event.getEventID ().equals (CommonDatabaseConstants.EVENT_ID_PLAGUE)))
				
				valid = (cityData.getPopulationEventID () == null);
			
			// Other city events have no special requirements
			else
				valid = true;
		}
		
		return valid;
	}

	/**
	 * @param event Event to trigger
	 * @param targetWizard Wizard the event is being triggered for
	 * @param cityLocation City the event is being triggered for
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If we can't find an expected data item
	 * @throws PlayerNotFoundException If we can't find one of the players
	 * @throws MomException If there is another kind of error
	 * @throws JAXBException If there is a problem sending the message
	 * @throws XMLStreamException If there is a problem sending the message
	 */
	@Override
	public final void triggerCityEvent (final Event event, final PlayerServerDetails targetWizard, final MapCoordinates3DEx cityLocation, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		final ServerGridCellEx gc = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ());
		final OverlandMapCityData cityData = gc.getCityData ();
		
		// Do we need to pick an existing mineral in the area?  Depletion
		if (event.getEventID ().equals (CommonDatabaseConstants.EVENT_ID_DEPLETION))
		{
			final List<MapCoordinates3DEx> mineralLocations = new ArrayList<MapCoordinates3DEx> ();
			
			final MapCoordinates3DEx coords = new MapCoordinates3DEx (cityLocation);
			for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
				if (getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, direction.getDirectionID ()))
				{
					final OverlandMapTerrainData terrainData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
						(coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
					if (event.getEventMapFeatureID ().contains (terrainData.getMapFeatureID ()))
						mineralLocations.add (new MapCoordinates3DEx (coords));
				}
					
			if (mineralLocations.isEmpty ())
				throw new MomException ("Event " + event.getEventID () + " was picked for wizard " + targetWizard.getPlayerDescription ().getPlayerName () +
					" and city " + cityLocation + ", but now can't find a suitable mineral");
			
			final MapCoordinates3DEx mineralLocation = mineralLocations.get (getRandomUtils ().nextInt (mineralLocations.size ()));
			log.debug ("Depletion event, wizard " + targetWizard.getPlayerDescription ().getPlayerName () + ", city " + cityLocation +
				" will lose mineral at " + mineralLocation);
			
			final OverlandMapTerrainData mineralTerrainData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
				(mineralLocation.getZ ()).getRow ().get (mineralLocation.getY ()).getCell ().get (mineralLocation.getX ()).getTerrainData ();
			getRandomEvents ().sendRandomEventMessage (event.getEventID (), targetWizard.getPlayerDescription ().getPlayerID (),
				cityData.getCitySizeID (), cityData.getCityName (), mineralTerrainData.getMapFeatureID (), null, null, false, false, mom.getPlayers (), null);
			
			mineralTerrainData.setMapFeatureID (null);
			getFogOfWarMidTurnChanges ().updatePlayerMemoryOfTerrain (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
				mom.getPlayers (), mineralLocation, mom.getSessionDescription ().getFogOfWarSetting ().getTerrainAndNodeAuras ());
		}
		
		// Do we need to pick a hill or mountain in the area without a mineral, as well as a new mineral to add?  New Minerals
		else if (event.getEventID ().equals (CommonDatabaseConstants.EVENT_ID_NEW_MINERALS))
		{
			final List<MapCoordinates3DEx> mineralLocations = new ArrayList<MapCoordinates3DEx> ();
			
			final MapCoordinates3DEx coords = new MapCoordinates3DEx (cityLocation);
			for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
				if (getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, direction.getDirectionID ()))
				{
					final OverlandMapTerrainData terrainData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
						(coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
					if ((event.getEventTileTypeID ().contains (terrainData.getTileTypeID ())) && (terrainData.getMapFeatureID () == null))
						mineralLocations.add (new MapCoordinates3DEx (coords));
				}
					
			if (mineralLocations.isEmpty ())
				throw new MomException ("Event " + event.getEventID () + " was picked for wizard " + targetWizard.getPlayerDescription ().getPlayerName () +
					" and city " + cityLocation + ", but now can't find a suitable hill/mountain without minerals");
			
			final MapCoordinates3DEx mineralLocation = mineralLocations.get (getRandomUtils ().nextInt (mineralLocations.size ()));
			final String mapFeatureID = event.getEventMapFeatureID ().get (getRandomUtils ().nextInt (event.getEventMapFeatureID ().size ()));
			
			log.debug ("New Minerals event, wizard " + targetWizard.getPlayerDescription ().getPlayerName () + ", city " + cityLocation +
				" will gain mineral " + mapFeatureID + " at " + mineralLocation);
			getRandomEvents ().sendRandomEventMessage (event.getEventID (), targetWizard.getPlayerDescription ().getPlayerID (),
				cityData.getCitySizeID (), cityData.getCityName (), mapFeatureID, null, null, false, false, mom.getPlayers (), null);
			
			final OverlandMapTerrainData mineralTerrainData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
				(mineralLocation.getZ ()).getRow ().get (mineralLocation.getY ()).getCell ().get (mineralLocation.getX ()).getTerrainData ();
			mineralTerrainData.setMapFeatureID (mapFeatureID);
			getFogOfWarMidTurnChanges ().updatePlayerMemoryOfTerrain (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
				mom.getPlayers (), mineralLocation, mom.getSessionDescription ().getFogOfWarSetting ().getTerrainAndNodeAuras ());
		}
		
		// Other city events have no additional random elements to roll
		else
		{
			// Plague +  Population Boom are the only ones with duration
			if (event.getMinimumDuration () != null)
			{
				getRandomEvents ().sendRandomEventMessage (event.getEventID (), targetWizard.getPlayerDescription ().getPlayerID (),
					cityData.getCitySizeID (), cityData.getCityName (), null, null, null, false, false, mom.getPlayers (), null);
				
				cityData.setPopulationEventID (event.getEventID ());
				gc.setPopulationEventStartedTurnNumber (mom.getGeneralPublicKnowledge ().getTurnNumber ());
				getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
					mom.getPlayers (), cityLocation, mom.getSessionDescription ().getFogOfWarSetting ());
			}
			
			// Rebellion, Diplomatic Marriage
			else if ((event.getEventID ().equals (CommonDatabaseConstants.EVENT_ID_REBELLION)) ||
				(event.getEventID ().equals (CommonDatabaseConstants.EVENT_ID_DIPLOMATIC_MARRIAGE)))
			{
				getRandomEvents ().sendRandomEventMessage (event.getEventID (), targetWizard.getPlayerDescription ().getPlayerID (),
					cityData.getCitySizeID (), cityData.getCityName (), null, null, null, false, false, mom.getPlayers (), null);

				// Who are the old and new owners?
				PlayerServerDetails raiders = null;
				for (final PlayerServerDetails thisPlayer : mom.getPlayers ())
				{
					final MomPersistentPlayerPublicKnowledge thisPub = (MomPersistentPlayerPublicKnowledge) thisPlayer.getPersistentPlayerPublicKnowledge ();
					if (CommonDatabaseConstants.WIZARD_ID_RAIDERS.equals (thisPub.getWizardID ()))
						raiders = thisPlayer;
				}
				
				if (raiders == null)
					throw new MomException ("Trying to process Rebellion event, but can't find rebel player to give city to");
				
				final PlayerServerDetails oldCityOwner;
				final PlayerServerDetails newCityOwner;
				
				if (event.getEventID ().equals (CommonDatabaseConstants.EVENT_ID_DIPLOMATIC_MARRIAGE))
				{
					newCityOwner = targetWizard;
					oldCityOwner = raiders;
				}
				else
				{
					oldCityOwner = targetWizard;
					newCityOwner = raiders;
					
					// If raiders were defeated then re-activate them
					final MomPersistentPlayerPublicKnowledge raidersPub = (MomPersistentPlayerPublicKnowledge) raiders.getPersistentPlayerPublicKnowledge ();
					if (raidersPub.getWizardState () != WizardState.ACTIVE)
					{
						raidersPub.setWizardState (WizardState.ACTIVE);
						
						final UpdateWizardStateMessage msg = new UpdateWizardStateMessage ();
						msg.setBanishedPlayerID (raiders.getPlayerDescription ().getPlayerID ());
						msg.setWizardState (WizardState.ACTIVE);
						getMultiplayerSessionServerUtils ().sendMessageToAllClients (mom.getPlayers (), msg);
					}
				}
				
				// Change ownership of the city
				getCityProcessing ().captureCity (cityLocation, newCityOwner, oldCityOwner, mom);
				
				// Change ownership of any normal units, and kill off any summoned units
				for (final MemoryUnit mu : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
				{
					if ((cityLocation.equals (mu.getUnitLocation ())) && (mu.getStatus () == UnitStatusID.ALIVE))
					{
						final UnitEx unitDef = mom.getServerDB ().findUnit (mu.getUnitID (), "triggerCityEvent");
						if (unitDef.getUnitMagicRealm ().equals (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL))
						{
							mu.setOwningPlayerID (newCityOwner.getPlayerDescription ().getPlayerID ());
							getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (mu, mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
								mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting (), null);
						}
						else
							mom.getWorldUpdates ().killUnit (mu.getUnitURN (), KillUnitActionID.PERMANENT_DAMAGE);							
					}
				}
				
				mom.getWorldUpdates ().process (mom);

				// Update what both players can see, otherwise the new city owner can't see the city they now own
				getFogOfWarProcessing ().updateAndSendFogOfWar (mom.getGeneralServerKnowledge ().getTrueMap (),
					oldCityOwner, mom.getPlayers (), "event-DM-REB-Old", mom.getSessionDescription (), mom.getServerDB ());
				getFogOfWarProcessing ().updateAndSendFogOfWar (mom.getGeneralServerKnowledge ().getTrueMap (),
					newCityOwner, mom.getPlayers (), "event-DM-REB-New", mom.getSessionDescription (), mom.getServerDB ());
			}
				
			// Great Meteor, Earthquake 
			else
			{
				final Spell spellDef = mom.getServerDB ().findSpell (event.getEventSpellID (), "triggerCityEvent");

				final AttackCitySpellResult attackCitySpellResult = getSpellMultiCasting ().castCityAttackSpell (spellDef, null, event.getEventID (), null, cityLocation, mom);			
				
				getRandomEvents ().sendRandomEventMessage (event.getEventID (), targetWizard.getPlayerDescription ().getPlayerID (),
					cityData.getCitySizeID (), cityData.getCityName (), null, null, null, false, false, mom.getPlayers (), attackCitySpellResult);
			}
		}
	}

	/**
	 * @param event Event to switch off
	 * @param cityLocation City the event is being switched off for
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If we can't find an expected data item
	 * @throws PlayerNotFoundException If we can't find the player who owns a game element
	 * @throws MomException If there is another kind of error
	 * @throws JAXBException If there is a problem sending the message
	 * @throws XMLStreamException If there is a problem sending the message
	 */
	@Override
	public final void cancelCityEvent (final Event event, final MapCoordinates3DEx cityLocation, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		final ServerGridCellEx gc = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ());
		final OverlandMapCityData cityData = gc.getCityData ();
		
		// Its possible the owner of the city changed since the event started.  Capturing a city doesn't cancel events in the same way that it cancels spells cast on the city.
		getRandomEvents ().sendRandomEventMessage (event.getEventID (), cityData.getCityOwnerID (),
			cityData.getCitySizeID (), cityData.getCityName (), null, null, null, false, true, mom.getPlayers (), null);
		
		cityData.setPopulationEventID (null);
		gc.setPopulationEventStartedTurnNumber (null);
		getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
			mom.getPlayers (), cityLocation, mom.getSessionDescription ().getFogOfWarSetting ());
	}
	
	/**
	 * @return Coordinate system utils
	 */
	public final CoordinateSystemUtils getCoordinateSystemUtils ()
	{
		return coordinateSystemUtils;
	}

	/**
	 * @param utils Coordinate system utils
	 */
	public final void setCoordinateSystemUtils (final CoordinateSystemUtils utils)
	{
		coordinateSystemUtils = utils;
	}

	/**
	 * @return Memory building utils
	 */
	public final MemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param utils Memory building utils
	 */
	public final void setMemoryBuildingUtils (final MemoryBuildingUtils utils)
	{
		memoryBuildingUtils = utils;
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

	/**
	 * @return Rolls random events
	 */
	public final RandomEvents getRandomEvents ()
	{
		return randomEvents;
	}

	/**
	 * @param e Rolls random events
	 */
	public final void setRandomEvents (final RandomEvents e)
	{
		randomEvents = e;
	}

	/**
	 * @return FOW single changes
	 */
	public final FogOfWarMidTurnChanges getFogOfWarMidTurnChanges ()
	{
		return fogOfWarMidTurnChanges;
	}

	/**
	 * @param single FOW single changes
	 */
	public final void setFogOfWarMidTurnChanges (final FogOfWarMidTurnChanges single)
	{
		fogOfWarMidTurnChanges = single;
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
	 * @return Casting spells that have more than one effect
	 */
	public final SpellMultiCasting getSpellMultiCasting ()
	{
		return spellMultiCasting;
	}

	/**
	 * @param c Casting spells that have more than one effect
	 */
	public final void setSpellMultiCasting (final SpellMultiCasting c)
	{
		spellMultiCasting = c;
	}

	/**
	 * @return Main FOW update routine
	 */
	public final FogOfWarProcessing getFogOfWarProcessing ()
	{
		return fogOfWarProcessing;
	}

	/**
	 * @param obj Main FOW update routine
	 */
	public final void setFogOfWarProcessing (final FogOfWarProcessing obj)
	{
		fogOfWarProcessing = obj;
	}
}