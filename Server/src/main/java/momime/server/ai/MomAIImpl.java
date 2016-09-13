package momime.server.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitStatusID;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.PlayerKnowledgeUtils;
import momime.server.database.ServerDatabaseEx;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.messages.ServerMemoryGridCellUtils;

/**
 * Overall AI strategy + control
 */
public final class MomAIImpl implements MomAI
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (MomAIImpl.class);
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** AI decisions about cities */
	private CityAI cityAI;

	/** AI decisions about spells */
	private SpellAI spellAI;

	/** AI decisions about units */
	private UnitAI unitAI;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/**
	 *
	 * @param player AI player whose turn to take
	 * @param players List of players in the session
	 * @param trueMap True map details
	 * @param turnNumber Current turn number
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If we can't find the race inhabiting the city, or various buildings
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws PlayerNotFoundException If we can't find the player who owns a unit
	 * @throws MomException If we find a consumption value that is not an exact multiple of 2, or we find a production value that is not an exact multiple of 2 that should be
	 */
	@Override
	public final void aiPlayerTurn (final PlayerServerDetails player, final List<PlayerServerDetails> players, final FogOfWarMemory trueMap, final int turnNumber,
		final MomSessionDescription sd, final ServerDatabaseEx db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		log.trace ("Entering aiPlayerTurn: Player ID " + player.getPlayerDescription ().getPlayerID () + ", turn " + turnNumber);

		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		
		// First find out what the best units we can construct or summon are - this gives us
		// something to gauge existing units by, to know if they're now obsolete or not
		final List<AIConstructableUnit> constructableUnits = getUnitAI ().listAllUnitsWeCanConstruct (player, players, sd, db);
		if (constructableUnits.isEmpty ())
			log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " can't make any units at all");
		else
		{
			for (final AIConstructableUnit unit : constructableUnits)
				log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " could make unit " + unit);
			
			// Work out how much defence we want to aim to have at every defensive position.  Even 1 point short will be regarded as underdefended,
			// so this starts out that 1 defender is fine and will ramp up at turn 50,100,150,200 until we hit a max of wanting 5 defenders.
			// It'll also start to consider positions as underdefended as better units become available to us.
			final int highestAverageRating = constructableUnits.get (0).getAverageRating ();
			final int desiredDefenceRating = (highestAverageRating * Math.min (turnNumber, 200)) / 50;
			log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + "'s strongest UAR = " + highestAverageRating + ", so DDR = " + desiredDefenceRating);
			
			// Estimate the total strength of all the units we have at every point on the map for attack and defense purposes,
			// as well as the strength of all enemy units for attack purposes.
			final AIUnitsAndRatings [] [] [] ourUnits = new AIUnitsAndRatings [sd.getOverlandMapSize ().getDepth ()] [sd.getOverlandMapSize ().getHeight ()] [sd.getOverlandMapSize ().getWidth ()];
			final AIUnitsAndRatings [] [] [] enemyUnits = new AIUnitsAndRatings [sd.getOverlandMapSize ().getDepth ()] [sd.getOverlandMapSize ().getHeight ()] [sd.getOverlandMapSize ().getWidth ()];
			for (final MemoryUnit mu : priv.getFogOfWarMemory ().getUnit ())
				if (mu.getStatus () == UnitStatusID.ALIVE)
				{
					final AIUnitsAndRatings [] [] [] unitArray = (mu.getOwningPlayerID () == player.getPlayerDescription ().getPlayerID ()) ? ourUnits : enemyUnits;
					AIUnitsAndRatings unitList = unitArray [mu.getUnitLocation ().getZ ()] [mu.getUnitLocation ().getY ()] [mu.getUnitLocation ().getX ()];
					if (unitList == null)
					{
						unitList = new AIUnitsAndRatingsImpl ();
						unitArray [mu.getUnitLocation ().getZ ()] [mu.getUnitLocation ().getY ()] [mu.getUnitLocation ().getX ()] = unitList; 
					}
					
					unitList.add (new AIUnitAndRatings (mu,
						getUnitAI ().calculateUnitCurrentRating (mu, players, priv.getFogOfWarMemory (), db),
						getUnitAI ().calculateUnitAverageRating (mu, players, priv.getFogOfWarMemory (), db)));
				}
			
			// Go through every defensive position that we either own, or is unoccupied and we should capture, seeing if we have enough units there defending
			final List<AIDefenceLocation> underdefendedLocations = new ArrayList<AIDefenceLocation> ();
			final List<AIUnitAndRatings> mobileUnits = new ArrayList<AIUnitAndRatings> ();
			
			for (int z = 0; z < sd.getOverlandMapSize ().getDepth (); z++)
				for (int y = 0; y < sd.getOverlandMapSize ().getHeight (); y++)
					for (int x = 0; x < sd.getOverlandMapSize ().getWidth (); x++)
					{
						final AIUnitsAndRatings ours = ourUnits [z] [y] [x];
						final AIUnitsAndRatings theirs = enemyUnits [z] [y] [x];
						
						final MemoryGridCell mc = priv.getFogOfWarMemory ().getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x);
						final OverlandMapCityData cityData = mc.getCityData ();
						final OverlandMapTerrainData terrainData = mc.getTerrainData ();
						if (((cityData != null) || (ServerMemoryGridCellUtils.isNodeLairTower (terrainData, db))) && (theirs == null)) 
						{
							final MapCoordinates3DEx coords = new MapCoordinates3DEx (x, y, z);
							
							final int defenceRating = (ours == null) ? 0 : ours.stream ().mapToInt (u -> u.getAverageRating ()).sum ();
							final String description = (cityData != null) ? ("city belonging to " + cityData.getCityOwnerID ()) : (terrainData.getTileTypeID () + "/" + terrainData.getMapFeatureID ());
							
							final int thisDesiredDefenceRating = desiredDefenceRating * (getMemoryBuildingUtils ().findBuilding
								(priv.getFogOfWarMemory ().getBuilding (), coords, CommonDatabaseConstants.BUILDING_FORTRESS) == null ? 1 : 2);
							
							log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " sees " + description +
								" at (" + x + ", " + y + ", " + z + "), CDR = " + defenceRating + ", DDR = " + thisDesiredDefenceRating);
							
							if (defenceRating < thisDesiredDefenceRating)
								underdefendedLocations.add (new AIDefenceLocation (coords, thisDesiredDefenceRating - defenceRating));
							else if (defenceRating > thisDesiredDefenceRating)
							{
								// Maybe we have too much defence?  Can we lose a unit or two without becoming underdefended?
								Collections.sort (ours);
								int defenceSoFar = 0;
								final Iterator<AIUnitAndRatings> iter = ours.iterator ();
								while (iter.hasNext ())
								{
									final AIUnitAndRatings thisUnit = iter.next ();
									if (defenceSoFar >= thisDesiredDefenceRating)
									{
										iter.remove ();
										mobileUnits.add (thisUnit);
									}
									else
										defenceSoFar = defenceSoFar + thisUnit.getAverageRating ();
								}
							}
						}

						// All units not in defensive positions are automatically mobile
						else if (ours != null)
						{
							mobileUnits.addAll (ours);
							ourUnits [z] [y] [x] = null;
						}
					}
			
			Collections.sort (underdefendedLocations);
			for (final AIDefenceLocation location : underdefendedLocations)
				log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " is underdefended at " + location);
			
			for (final AIUnitAndRatings mu : mobileUnits)
				log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " can move " + mu);
		}

		// Decide what to build in all of this players' cities
		// Note we do this EVERY TURN - we don't wait for the previous building to complete - this allows the AI player to change their mind
		// e.g. if a city has minimal defence and has a university almost built and then a
		// group of halbardiers show up 2 squares away, you're going to want to stuff the university and rush buy the best unit you can afford
		for (int z = 0; z < sd.getOverlandMapSize ().getDepth (); z++)
			for (int y = 0; y < sd.getOverlandMapSize ().getHeight (); y++)
				for (int x = 0; x < sd.getOverlandMapSize ().getWidth (); x++)
				{
					final OverlandMapCityData cityData = trueMap.getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x).getCityData ();
					if ((cityData != null) && (cityData.getCityOwnerID () == player.getPlayerDescription ().getPlayerID ()))
					{
						final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (x, y, z);

						getCityAI ().decideWhatToBuild (cityLocation, cityData, priv.getFogOfWarMemory ().getMap (), priv.getFogOfWarMemory ().getBuilding (), sd, db);
						getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (trueMap.getMap (), players, cityLocation, sd.getFogOfWarSetting ());
					}
				}

		// This relies on knowing what's being built in each city, so do it 2nd
		getCityAI ().setOptionalFarmersInAllCities (trueMap, players, player, db, sd);

		// Do we need to choose a spell to research?
		if ((PlayerKnowledgeUtils.isWizard (pub.getWizardID ())) && (priv.getSpellIDBeingResearched () == null))
			getSpellAI ().decideWhatToResearch (player, db);

		log.trace ("Exiting aiPlayerTurn");
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
	 * @return AI decisions about cities
	 */
	public final CityAI getCityAI ()
	{
		return cityAI;
	}

	/**
	 * @param ai AI decisions about cities
	 */
	public final void setCityAI (final CityAI ai)
	{
		cityAI = ai;
	}

	/**
	 * @return AI decisions about spells
	 */
	public final SpellAI getSpellAI ()
	{
		return spellAI;
	}

	/**
	 * @param ai AI decisions about spells
	 */
	public final void setSpellAI (final SpellAI ai)
	{
		spellAI = ai;
	}

	/**
	 * @return AI decisions about units
	 */
	public final UnitAI getUnitAI ()
	{
		return unitAI;
	}

	/**
	 * @param ai AI decisions about units
	 */
	public final void setUnitAI (final UnitAI ai)
	{
		unitAI = ai;
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
}