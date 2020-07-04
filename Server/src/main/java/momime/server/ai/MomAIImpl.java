package momime.server.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.calculations.CityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.TurnSystem;
import momime.common.utils.PlayerKnowledgeUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.database.AiUnitCategorySvr;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.utils.CityServerUtils;

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
	
	/** Random number generator */
	private RandomUtils randomUtils;

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** City calculations */
	private CityCalculations cityCalculations;
	
	/** Server-only city utils */
	private CityServerUtils cityServerUtils;
	
	/**
	 *
	 * @param player AI player whose turn to take
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If we can't find the race inhabiting the city, or various buildings
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws PlayerNotFoundException If we can't find the player who owns a unit
	 * @throws MomException If we find a consumption value that is not an exact multiple of 2, or we find a production value that is not an exact multiple of 2 that should be
	 */
	@Override
	public final void aiPlayerTurn (final PlayerServerDetails player, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		log.trace ("Entering aiPlayerTurn: Player ID " + player.getPlayerDescription ().getPlayerID () + ", turn " + mom.getGeneralPublicKnowledge ().getTurnNumber ());

		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		final int numberOfCities = getCityServerUtils ().countCities (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), player.getPlayerDescription ().getPlayerID ());
		
		// First find out what the best units we can construct or summon are - this gives us
		// something to gauge existing units by, to know if they're now obsolete or not
		final List<AIConstructableUnit> constructableUnits = getUnitAI ().listAllUnitsWeCanConstruct (player, mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());
		
		// Ignore summoned units - otherwise at the start the AI realises that Magic Spirits are actually better than the Spearmen + Swordsmen that it has and tries to use them to defend its city.
		// Similarly later on, the AI shouldn't be trying to fill its cities defensively with all great drakes.
		final List<AIConstructableUnit> cityConstructableCombatUnits = constructableUnits.stream ().filter
			(u -> (u.getAiUnitType () == AIUnitType.COMBAT_UNIT) && (u.getCityLocation () != null)).collect (Collectors.toList ());
		
		if (cityConstructableCombatUnits.isEmpty ())
			log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " can't make any combat units in cities at all");
		else
		{
			for (final AIConstructableUnit unit : cityConstructableCombatUnits)
				log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " could make combat unit " + unit);
			
			final int highestAverageRating = cityConstructableCombatUnits.get (0).getAverageRating ();
			log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + "'s strongest UAR of city constructable combat units = " + highestAverageRating);

			// Raiders have some special handling
			final boolean isRaiders = CommonDatabaseConstants.WIZARD_ID_RAIDERS.equals (pub.getWizardID ());
			
			// Estimate the total strength of all the units we have at every point on the map for attack and defense purposes,
			// as well as the strength of all enemy units for attack purposes.
			final AIUnitsAndRatings [] [] [] ourUnits = new AIUnitsAndRatings [mom.getSessionDescription ().getOverlandMapSize ().getDepth ()] [mom.getSessionDescription ().getOverlandMapSize ().getHeight ()] [mom.getSessionDescription ().getOverlandMapSize ().getWidth ()];
			final AIUnitsAndRatings [] [] [] enemyUnits = new AIUnitsAndRatings [mom.getSessionDescription ().getOverlandMapSize ().getDepth ()] [mom.getSessionDescription ().getOverlandMapSize ().getHeight ()] [mom.getSessionDescription ().getOverlandMapSize ().getWidth ()];
			getUnitAI ().calculateUnitRatingsAtEveryMapCell (ourUnits, enemyUnits, player.getPlayerDescription ().getPlayerID (), priv.getFogOfWarMemory (), mom.getPlayers (), mom.getServerDB ());
			
			// Go through every defensive position that we either own, or is unoccupied and we should capture, seeing if we have enough units there defending
			final List<AIUnitAndRatings> mobileUnits = new ArrayList<AIUnitAndRatings> ();
			final List<AIDefenceLocation> underdefendedLocations = getUnitAI ().evaluateCurrentDefence (ourUnits, enemyUnits, mobileUnits,
				player.getPlayerDescription ().getPlayerID (), isRaiders, priv.getFogOfWarMemory (), highestAverageRating, mom.getGeneralPublicKnowledge ().getTurnNumber (),
				mom.getSessionDescription ().getOverlandMapSize (), mom.getServerDB ());

			for (final AIDefenceLocation location : underdefendedLocations)
				log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " is underdefended at " + location);
			
			// Get a list of all specialist units split by category and what plane they are on
			final Map<Integer, Map<AIUnitType, List<AIUnitAndRatings>>> specialistUnitsOnEachPlane = getUnitAI ().determineSpecialistUnitsOnEachPlane
				(player.getPlayerDescription ().getPlayerID (), mobileUnits);
			
			// What's the best place we can put a new city on each plane?
			final Map<Integer, Map<AIUnitType, List<MapCoordinates3DEx>>> desiredSpecialUnitLocationsOnEachPlane = getUnitAI ().determineDesiredSpecialUnitLocationsOnEachPlane
				(player.getPlayerDescription ().getPlayerID (), mom.getPlayers (), priv.getFogOfWarMemory (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
				mom.getSessionDescription (), mom.getServerDB ());
			
			if (((PlayerKnowledgeUtils.isWizard (pub.getWizardID ())) || (isRaiders)) && (!mobileUnits.isEmpty ()))
			{
				// Try to find somewhere to move each mobile unit to.
				// In "one player at a time" games, we can see the results our each movement step, so here we only ever move 1 cell at a time.
				// In "simultaneous turns" games, we will move anywhere that we can reach in one turn.
				boolean restart = true;
				int pass = 0;
				while (restart)
				{
					pass++;
					log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " movement pass " + pass);					
					
					restart = false;
					final Map<String, List<AIUnitsAndRatings>> categories = getUnitAI ().categoriseAndStackUnits (mobileUnits, mom.getPlayers (), priv.getFogOfWarMemory (), mom.getServerDB ());				
					
					// Now move units in the order their categories are listed in the database
					final Iterator<AiUnitCategorySvr> categoriesIter = mom.getServerDB ().getAiUnitCategories ().iterator ();
					while ((!restart) && (categoriesIter.hasNext ()))
					{
						final AiUnitCategorySvr category = categoriesIter.next ();
							
						final List<AIUnitsAndRatings> ourUnitStacksInThisCategory = categories.get (category.getAiUnitCategoryID ());
						if (ourUnitStacksInThisCategory != null)
						{
							final Iterator<AIUnitsAndRatings> unitStacksIter = ourUnitStacksInThisCategory.iterator ();
							while ((!restart) && (unitStacksIter.hasNext ()))
							{
								final AIUnitsAndRatings unitStack = unitStacksIter.next ();
									
								// In one-at-a-time games, we move one cell at a time so we can rethink actions as we see more of the map.								
								// In simultaneous turns games, we move as far as we can in one go since we won't learn anything new about the map until we finish allocating all movement.
								log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " checking where it can move stack of " + category.getAiUnitCategoryID () + " - " +
									category.getAiUnitCategoryDescription () + "  from " + unitStack.get (0).getUnit ().getUnitLocation () + ", first Unit URN " + unitStack.get (0).getUnit ().getUnitURN ());
								
								final boolean tookAction = getUnitAI ().decideAndExecuteUnitMovement (unitStack, category, underdefendedLocations, ourUnitStacksInThisCategory, enemyUnits,
									desiredSpecialUnitLocationsOnEachPlane.get (unitStack.get (0).getUnit ().getUnitLocation ().getZ ()), player, mom);

								// In a one-player-at-a-time game, units are moved instantly and so may have joined onto other unit stacks, have movement left,
								// and that remaining movement now be affected by something in the stack they've joined such as a hero with Pathfinding.
								// That's too complicated to try to work out the effect of, so just restart over every time we make a move.
								if ((tookAction) && (mom.getSessionDescription ().getTurnSystem () == TurnSystem.ONE_PLAYER_AT_A_TIME))
								{
									restart = true;
									
									// As a result of taking some action, like a settler building a city or combat units dying in a fight, some of our mobile units
									// may not exist anymore so better recheck them before we restart
									final Iterator<AIUnitAndRatings> mobileUnitsIter = mobileUnits.iterator ();
									while (mobileUnitsIter.hasNext ())
									{
										final AIUnitAndRatings thisUnit = mobileUnitsIter.next ();
										if (getUnitUtils ().findUnitURN (thisUnit.getUnit ().getUnitURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ()) == null)
											mobileUnitsIter.remove ();
									}
								}
							}
						}
					}
				}
			}

			// Raiders consider every city to be a unit factory
			final List<MapCoordinates3DEx> unitFactories;
			if (isRaiders)
				unitFactories = null;
			else
			{
				// Which city can build the best units? (even if we can't afford to make another one)
				final OptionalInt bestAverageRatingFromConstructableUnits = constructableUnits.stream ().filter
					(u -> u.getCityLocation () != null).mapToInt (u -> u.getAverageRating ()).max ();
				
				if (!bestAverageRatingFromConstructableUnits.isPresent ())
				{
					unitFactories = null;
					log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " can't construct any units in any cities");
				}
				else
				{
					log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + "'s best unit(s) it can construct in any cities are UAR = " + bestAverageRatingFromConstructableUnits.getAsInt ());
	
					// What units can we make that match our best?
					// This restricts to only units we can afford maintenance of, so if our economy is struggling, we may get an empty list here.
					final List<AIConstructableUnit> bestConstructableUnits = constructableUnits.stream ().filter
						(u -> (u.getCityLocation () != null) && (u.isCanAffordMaintenance ()) && (u.getAverageRating () == bestAverageRatingFromConstructableUnits.getAsInt ())).collect (Collectors.toList ());
					
					unitFactories = bestConstructableUnits.stream ().map (u -> u.getCityLocation ()).distinct ().collect (Collectors.toList ());
					unitFactories.forEach (c -> log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + "'s city at " + c + " is designated as a unit factory")); 
				}
			}
			
			int needForNewUnits = 0;
			if ((isRaiders) || ((unitFactories != null) && (!unitFactories.isEmpty ())))
			{
				// We need some kind of rating for how badly we think we need to construct more combat units.
				// So base this on, 1) how many locations are underdefended, 2) whether we have sufficient mobile units.
				// This could be a bit more clever, like "are there places we want to attack that we need more/stronger units to consider the attack",
				// but I don't want the AI churning out armies of swordsmen just to try to beat a great drake.
				needForNewUnits = 3 + underdefendedLocations.size () +
					(Math.min (mom.getGeneralPublicKnowledge ().getTurnNumber (), 200) / 5) - mobileUnits.size ();
			}
			log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " need for new units = " + needForNewUnits);
			
			// Decide what to build in all of this players' cities, in any that don't currently have construction projects.
			// We always complete the previous construction project, so that if we are deciding between making units in our unit factory
			// or making a building that means we'll make better units in future, that we won't reverse that decision after its been made.
			if (numberOfCities > 0)
				for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
				{
					// What unit types do we want to build on this plane?
					final Map<AIUnitType, List<AIUnitAndRatings>> specialistUnitsOnThisPlane = specialistUnitsOnEachPlane.get (z);								// May be null					
					final Map<AIUnitType, List<MapCoordinates3DEx>> desiredSpecialUnitLocations = desiredSpecialUnitLocationsOnEachPlane.get (z);	// Always a valid list, even if empty
					
					// We need to NOT have one of that type of unit already, so that we want to build one
					final List<AIUnitType> wantedUnitTypes = desiredSpecialUnitLocations.keySet ().stream ().filter (ut ->
						(specialistUnitsOnThisPlane == null) || (!specialistUnitsOnThisPlane.containsKey (ut))).collect (Collectors.toList ());
					
					for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
						for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
						{
							final OverlandMapCityData cityData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x).getCityData ();
							if ((cityData != null) && (cityData.getCityOwnerID () == player.getPlayerDescription ().getPlayerID ()) &&
								((CommonDatabaseConstants.BUILDING_HOUSING.equals (cityData.getCurrentlyConstructingBuildingID ())) ||
								(CommonDatabaseConstants.BUILDING_TRADE_GOODS.equals (cityData.getCurrentlyConstructingBuildingID ()))))
							{
								final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (x, y, z);
								final boolean isUnitFactory = (isRaiders) || ((unitFactories != null) && (unitFactories.contains (cityLocation)));
	
								// Get a list of combat units we'll consider building here, which is only the best 2 in our list, and even then only if this
								// city is a unit factory.  For now, don't worry about whether we can actually afford them or not.
								final List<String> constructableCombatUnits = !isUnitFactory ? new ArrayList<String> () :
									constructableUnits.stream ().filter (u -> (cityLocation.equals (u.getCityLocation ())) && (u.getAiUnitType () == AIUnitType.COMBAT_UNIT)).sorted ().limit (2).map
										(u -> u.getUnit ().getUnitID ()).collect (Collectors.toList ());
										
								// Now make a complete map of all units we could construct at this city, broken down by unit type.
								final Map<AIUnitType, List<AIConstructableUnit>> constructableHere = new HashMap<AIUnitType, List<AIConstructableUnit>> ();
								for (final AIConstructableUnit thisUnit : constructableUnits)
									if ((cityLocation.equals (thisUnit.getCityLocation ())) && (thisUnit.isCanAffordMaintenance ()) &&
										((thisUnit.getAiUnitType () != AIUnitType.COMBAT_UNIT) || (constructableCombatUnits.contains (thisUnit.getUnit ().getUnitID ()))))
									{
										List<AIConstructableUnit> unitsOfThisType = constructableHere.get (thisUnit.getAiUnitType ());
										if (unitsOfThisType == null)
										{
											unitsOfThisType = new ArrayList<AIConstructableUnit> ();
											constructableHere.put (thisUnit.getAiUnitType (), unitsOfThisType);
										}
										
										unitsOfThisType.add (thisUnit);
									}

								// Now we can decide what to build
								getCityAI ().decideWhatToBuild (cityLocation, cityData, numberOfCities, isUnitFactory, needForNewUnits, constructableHere, wantedUnitTypes,
									priv.getFogOfWarMemory ().getMap (), priv.getFogOfWarMemory ().getBuilding (),
									mom.getSessionDescription (), mom.getServerDB ());
								
								getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
									mom.getPlayers (), cityLocation, mom.getSessionDescription ().getFogOfWarSetting ());
							}
						}
				}
			}

		if (numberOfCities > 0)
		{
			// Decide optimal tax rate
			getCityAI ().decideTaxRate (player, mom);
			
			// This relies on knowing what's being built in each city and the tax rate, so do it almost last
			getCityAI ().setOptionalFarmersInAllCities (mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), player, mom.getServerDB (), mom.getSessionDescription ());
			
			// This relies on knowing the production each city is generating, so need the number of farmers + workers set, so do it really last
			getCityAI ().checkForRushBuying (player, mom);
		}
		
		// Do we need to choose a spell to research?
		if ((PlayerKnowledgeUtils.isWizard (pub.getWizardID ())) && (priv.getSpellIDBeingResearched () == null))
			getSpellAI ().decideWhatToResearch (player, mom.getServerDB ());

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
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final UnitUtils utils)
	{
		unitUtils = utils;
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