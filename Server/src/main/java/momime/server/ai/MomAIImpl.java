package momime.server.ai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.sessionbase.PlayerType;
import com.ndg.utils.random.RandomUtils;
import com.ndg.utils.random.WeightedChoicesImpl;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.calculations.CityCalculations;
import momime.common.database.AiUnitCategory;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.SelectedByPick;
import momime.common.database.SpellSetting;
import momime.common.database.WizardObjective;
import momime.common.database.WizardPersonality;
import momime.common.messages.DiplomacyWizardDetails;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.NewTurnMessageOffer;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.PlayerPick;
import momime.common.messages.SpellResearchStatusID;
import momime.common.messages.TurnSystem;
import momime.common.messages.WizardState;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.PlayerKnowledgeUtils;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.SpellUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.process.DiplomacyProcessing;
import momime.server.process.OfferGenerator;
import momime.server.utils.CityServerUtils;
import momime.server.utils.KnownWizardServerUtils;

/**
 * Overall AI strategy + control
 */
public final class MomAIImpl implements MomAI
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (MomAIImpl.class);
	
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
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/** Offer generator */
	private OfferGenerator offerGenerator;
	
	/** Spell utils */
	private SpellUtils spellUtils;
	
	/** Methods for working with wizardIDs */
	private PlayerKnowledgeUtils playerKnowledgeUtils;
	
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/** For calculating relation scores between two wizards */
	private RelationAI relationAI;
	
	/** Methods for AI making decisions about diplomacy with other wizards */
	private DiplomacyAI diplomacyAI;
	
	/** During an AI player's turn, works out what diplomacy proposals they may want to initiate to other wizards */
	private DiplomacyProposalsAI diplomacyProposalsAI;
	
	/** Methods for processing agreed diplomatic actions */
	private DiplomacyProcessing diplomacyProcessing; 
	
	/** Methods for processing diplomacy requests from AI player to another AI player */
	private AIToAIDiplomacy aiToAIDiplomacy;
	
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;
	
	/** Process for making sure one wizard has met another wizard */
	private KnownWizardServerUtils knownWizardServerUtils;
	
	/**
	 * @param player AI player whose turn to take
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param firstPass True if this is the first pass at this AI player's turn (previous player hit next turn); false if the AI player's turn was paused for a combat or diplomacy and now doing 2nd/3rd/etc pass 
	 * @return Whether AI turn was fully completed or not; false if we the AI initated a combat in a one-player-at-a-time game and must resume their turn after the combat ends 
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final boolean aiPlayerTurn (final PlayerServerDetails player, final MomSessionVariables mom, final boolean firstPass)
		throws JAXBException, XMLStreamException, IOException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		
		final KnownWizardDetails wizardDetails = getKnownWizardUtils ().findKnownWizardDetails
			(mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), player.getPlayerDescription ().getPlayerID (), "aiPlayerTurn");

		if (getPlayerKnowledgeUtils ().isWizard (wizardDetails.getWizardID ()))
		{
			getUnitAI ().reallocateHeroItems (player, mom);
			
			// All modifications to visible relation are done in one go, then capped at the end, so we allow them to temporarily go above 100 or below -100
			if (firstPass)
			{
				getRelationAI ().updateVisibleRelationDueToUnitsInOurBorder (player, mom);
				getRelationAI ().updateVisibleRelationDueToPactsAndAlliances (player);
				getRelationAI ().updateVisibleRelationDueToAuraOfMajesty (player);
				getRelationAI ().updateVisibleRelationDueToNastyMaintainedSpells (player, mom);
				getRelationAI ().slideTowardsBaseRelation (player);
				getRelationAI ().capVisibleRelations (player);
				getRelationAI ().regainPatience (player);
			}
			
			// Do we want to make any diplomacy proposals to anyone?
			if (wizardDetails.getWizardState () == WizardState.ACTIVE)
			{
				final Iterator<KnownWizardDetails> iter = priv.getFogOfWarMemory ().getWizardDetails ().iterator ();
				while (iter.hasNext ())
				{
					final DiplomacyWizardDetails talkToWizard = (DiplomacyWizardDetails) iter.next ();
					if ((getPlayerKnowledgeUtils ().isWizard (talkToWizard.getWizardID ())) && (talkToWizard.getWizardState () == WizardState.ACTIVE) &&
						(talkToWizard.getPlayerID () != player.getPlayerDescription ().getPlayerID ()) &&
						(mom.getGeneralPublicKnowledge ().getTurnNumber () >= talkToWizard.getLastTurnTalkedTo () + DiplomacyAIConstants.MINIMUM_TURNS_BETWEEN_TALKING))
					{
						final PlayerServerDetails talkToPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), talkToWizard.getPlayerID (), "aiPlayerTurn");
						final List<DiplomacyProposal> proposals = getDiplomacyProposalsAI ().generateProposals (player, talkToPlayer, mom);
						if (!proposals.isEmpty ())
						{
							if (log.isDebugEnabled ())
								for (final DiplomacyProposal proposal : proposals)
									log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " wants to make proposal " + proposal + " to player ID " + talkToWizard.getPlayerID ());
							
							getKnownWizardServerUtils ().meetWizard (player.getPlayerDescription ().getPlayerID (), talkToWizard.getPlayerID (), false, mom);
							talkToWizard.setLastTurnTalkedTo (mom.getGeneralPublicKnowledge ().getTurnNumber ());
							
							if (talkToPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
							{
								getDiplomacyProcessing ().requestTalking (talkToPlayer, player, mom);
								return false;		// Pause remainder of AI player turn until human player responds
							}
							else if (getDiplomacyAI ().decideWhetherWillTalkTo (player, talkToPlayer, mom))
								getAiToAIDiplomacy ().submitProposals (player, talkToPlayer, proposals, mom);
						}
					}
				}
			}
		}
		
		final int numberOfCities = getCityServerUtils ().countCities (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), player.getPlayerDescription ().getPlayerID ());
		
		// First find out what the best units we can construct or summon are - this gives us
		// something to gauge existing units by, to know if they're now obsolete or not
		final List<AIConstructableUnit> constructableUnits = getUnitAI ().listAllUnitsWeCanConstruct (player, mom);
		
		// Ignore summoned units - otherwise at the start the AI realises that Magic Spirits are actually better than the Spearmen + Swordsmen that it has and tries to use them to defend its city.
		// Similarly later on, the AI shouldn't be trying to fill its cities defensively with all great drakes.
		final List<AIConstructableUnit> cityConstructableCombatUnits = constructableUnits.stream ().filter
			(u -> (u.getAiUnitType () == AIUnitType.COMBAT_UNIT) && (u.getCityLocation () != null)).collect (Collectors.toList ());
		
		boolean combatStarted = false;
		final Map<Integer, List<AIUnitType>> wantedUnitTypesOnEachPlane = new HashMap<Integer, List<AIUnitType>> ();
		
		if ((cityConstructableCombatUnits.isEmpty ()) && (!CommonDatabaseConstants.WIZARD_ID_MONSTERS.equals (wizardDetails.getWizardID ())))
			log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " can't make any combat units in cities at all");
		else
		{
			// Raiders and Rampaging monsters have some special handling
			final boolean isRaiders = CommonDatabaseConstants.WIZARD_ID_RAIDERS.equals (wizardDetails.getWizardID ());
			final boolean isMonsters = CommonDatabaseConstants.WIZARD_ID_MONSTERS.equals (wizardDetails.getWizardID ());
			
			final KnownWizardDetails raidersWizard = mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails ().stream ().filter
				(w -> CommonDatabaseConstants.WIZARD_ID_RAIDERS.equals (w.getWizardID ())).findAny ().orElse (null);
			final Integer ignorePlayerID = isMonsters ? raidersWizard.getPlayerID () : null;

			// Estimate the total strength of all the units we have at every point on the map for attack and defense purposes,
			// as well as the strength of all enemy units for attack purposes.
			final AIUnitsAndRatings [] [] [] ourUnits = new AIUnitsAndRatings [mom.getSessionDescription ().getOverlandMapSize ().getDepth ()] [mom.getSessionDescription ().getOverlandMapSize ().getHeight ()] [mom.getSessionDescription ().getOverlandMapSize ().getWidth ()];
			final AIUnitsAndRatings [] [] [] enemyUnits = new AIUnitsAndRatings [mom.getSessionDescription ().getOverlandMapSize ().getDepth ()] [mom.getSessionDescription ().getOverlandMapSize ().getHeight ()] [mom.getSessionDescription ().getOverlandMapSize ().getWidth ()];
			getUnitAI ().calculateUnitRatingsAtEveryMapCell (ourUnits, enemyUnits, player.getPlayerDescription ().getPlayerID (), ignorePlayerID,
				priv.getFogOfWarMemory (), mom.getPlayers (), mom.getServerDB ());
			
			final List<AIUnitAndRatings> mobileUnits = new ArrayList<AIUnitAndRatings> ();
			final List<AIDefenceLocation> underdefendedLocations;
			final Map<AIUnitType, List<MapCoordinates3DEx>> desiredSpecialUnitLocations;
			final Map<Integer, Map<AIUnitType, List<AIUnitAndRatings>>> specialistUnitsOnEachPlane;
			
			if (isMonsters)
			{
				// Rampaging monsters won't go join a node/lair/tower to help defend it, but we do need to list here cities owned by wizards that have no defence
				underdefendedLocations = getUnitAI ().listUndefendedWizardCities (enemyUnits, player.getPlayerDescription ().getPlayerID (),
					raidersWizard.getPlayerID (), priv.getFogOfWarMemory ().getMap (), mom.getSessionDescription ().getOverlandMapSize ());

				// Rampaging monsters don't evaluate defence, just any unit not in a node/lair/tower is considered mobile
				getUnitAI ().listUnitsNotInNodeLairTowers (ourUnits, mobileUnits, priv.getFogOfWarMemory ().getMap (),
					mom.getSessionDescription ().getOverlandMapSize (), mom.getServerDB ());
				
				// Rampaging monsters don't use settlers, engineers and so on, so just make empty maps
				desiredSpecialUnitLocations = new HashMap<AIUnitType, List<MapCoordinates3DEx>> ();
				specialistUnitsOnEachPlane = new HashMap<Integer, Map<AIUnitType, List<AIUnitAndRatings>>> ();
			}
			else
			{
				for (final AIConstructableUnit unit : cityConstructableCombatUnits)
					log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " could make combat unit " + unit);
				
				final int highestAverageRating = cityConstructableCombatUnits.get (0).getAverageRating ();
				log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + "'s strongest UAR of city constructable combat units = " + highestAverageRating);
	
				// Go through every defensive position that we either own, or is unoccupied and we should capture, seeing if we have enough units there defending
				underdefendedLocations = getUnitAI ().evaluateCurrentDefence (ourUnits, enemyUnits, mobileUnits,
					player.getPlayerDescription ().getPlayerID (), isRaiders, priv.getFogOfWarMemory (), highestAverageRating, mom.getGeneralPublicKnowledge ().getTurnNumber (),
					mom.getSessionDescription ().getOverlandMapSize (), mom.getServerDB ());
	
				for (final AIDefenceLocation location : underdefendedLocations)
					log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " is underdefended at " + location);
				
				// Get a list of all specialist units split by category and what plane they are on
				specialistUnitsOnEachPlane = getUnitAI ().determineSpecialistUnitsOnEachPlane
					(player.getPlayerDescription ().getPlayerID (), mobileUnits, priv.getFogOfWarMemory ().getMap (), mom.getSessionDescription ().getOverlandMapSize ());
				
				// What's the best place we can put a new city on each plane?
				desiredSpecialUnitLocations = getUnitAI ().determineDesiredSpecialUnitLocations (player.getPlayerDescription ().getPlayerID (), priv.getFogOfWarMemory (), mom);
			}
			
			if (!mobileUnits.isEmpty ())
			{
				// Try to find somewhere to move each mobile unit to.
				// In "one player at a time" games, we can see the results our each movement step, so here we only ever move 1 cell at a time.
				// In "simultaneous turns" games, we will move anywhere that we can reach in one turn.
				boolean restart = true;
				int pass = 0;
				while ((restart) && (pass < 200))
				{
					pass++;
					log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " movement pass " + pass);					
					
					restart = false;
					final Map<String, List<AIUnitsAndRatings>> categories = getUnitAI ().categoriseAndStackUnits (mobileUnits, mom.getPlayers (), priv.getFogOfWarMemory (), mom.getServerDB ());				
					
					// Now move units in the order their categories are listed in the database
					final Iterator<AiUnitCategory> categoriesIter = mom.getServerDB ().getAiUnitCategory ().iterator ();
					while ((!restart) && (!combatStarted) && (categoriesIter.hasNext ()))
					{
						final AiUnitCategory category = categoriesIter.next ();
							
						final List<AIUnitsAndRatings> ourUnitStacksInThisCategory = categories.get (category.getAiUnitCategoryID ());
						if (ourUnitStacksInThisCategory != null)
						{
							final Iterator<AIUnitsAndRatings> unitStacksIter = ourUnitStacksInThisCategory.iterator ();
							while ((!restart) && (!combatStarted) && (unitStacksIter.hasNext ()))
							{
								final AIUnitsAndRatings unitStack = unitStacksIter.next ();
									
								// In one-at-a-time games, we move one cell at a time so we can rethink actions as we see more of the map.								
								// In simultaneous turns games, we move as far as we can in one go since we won't learn anything new about the map until we finish allocating all movement.
								log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " checking where it can move stack of " + category.getAiUnitCategoryID () + " - " +
									category.getAiUnitCategoryDescription () + "  from " + unitStack.get (0).getUnit ().getUnitLocation () + ", first Unit URN " + unitStack.get (0).getUnit ().getUnitURN ());
								
								final AIMovementResult movementResult = getUnitAI ().decideAndExecuteUnitMovement (unitStack, category, underdefendedLocations, ourUnitStacksInThisCategory, enemyUnits,
									desiredSpecialUnitLocations, player, mom);

								// In a one-player-at-a-time game, units are moved instantly and so may have joined onto other unit stacks, have movement left,
								// and that remaining movement now be affected by something in the stack they've joined such as a hero with Pathfinding.
								// That's too complicated to try to work out the effect of, so just restart over every time we make a move.
								if (mom.getSessionDescription ().getTurnSystem () == TurnSystem.ONE_PLAYER_AT_A_TIME)
									switch (movementResult)
									{
										case MOVED_AND_STARTED_COMBAT:
											combatStarted = true;
											break;
											
										case MOVED:
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
											break;
											
										default:
											// Just keep going with next unit stack until all are done
									}
							}
						}
					}
				}
			}

			if (!combatStarted)
			{
				// Decide what to build in all of this players' cities, in any that don't currently have construction projects.
				// We always complete the previous construction project, so that if we are deciding between making units in our unit factory
				// or making a building that means we'll make better units in future, that we won't reverse that decision after its been made.
				if (numberOfCities > 0)
				{
					// Raiders consider every city to be a unit factory
					final List<MapCoordinates3DEx> unitFactories;
					if (isRaiders)
						unitFactories = null;
					else
					{
						// Which city can build the best units? (even if we can't afford to make another one)
						final OptionalInt bestAverageRatingFromConstructableUnits = cityConstructableCombatUnits.stream ().mapToInt (u -> u.getAverageRating ()).max ();				
						if (!bestAverageRatingFromConstructableUnits.isPresent ())
						{
							unitFactories = null;
							log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " can't construct any combat units in any cities");
						}
						else
						{
							log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + "'s best unit(s) it can construct in any cities are UAR = " + bestAverageRatingFromConstructableUnits.getAsInt ());
			
							// What units can we make that match our best?
							// This restricts to only units we can afford maintenance of, so if our economy is struggling, we may get an empty list here.
							final List<AIConstructableUnit> bestConstructableUnits = cityConstructableCombatUnits.stream ().filter
								(u -> (u.isCanAffordMaintenance ()) && (u.getAverageRating () == bestAverageRatingFromConstructableUnits.getAsInt ())).collect (Collectors.toList ());
							
							unitFactories = bestConstructableUnits.stream ().map (u -> u.getCityLocation ()).distinct ().collect (Collectors.toList ());
							unitFactories.forEach (c -> log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + "'s city at " + c + " is designated as a unit factory")); 
						}
					}
					
					int needForNewUnitsMod = 0;
					if ((isRaiders) || ((unitFactories != null) && (!unitFactories.isEmpty ())))
					{
						// We need some kind of rating for how badly we think we need to construct more combat units.
						// So base this on, 1) how many locations are underdefended, 2) whether we have sufficient mobile units.
						// This could be a bit more clever, like "are there places we want to attack that we need more/stronger units to consider the attack",
						// but I don't want the AI churning out armies of swordsmen just to try to beat a great drake.
						final int mobileCombatUnits = (int) mobileUnits.stream ().filter (u -> u.getAiUnitType () == AIUnitType.COMBAT_UNIT).count ();
						needForNewUnitsMod = 3 + underdefendedLocations.size () +
							(Math.min (mom.getGeneralPublicKnowledge ().getTurnNumber (), 200) / 5) - mobileCombatUnits;
					}
					log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " need for new units = " + needForNewUnitsMod);
				
					for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
					{
						final int finalZ = z;
						
						// What unit types do we want to build on this plane?
						final Map<AIUnitType, List<AIUnitAndRatings>> specialistUnitsOnThisPlane = specialistUnitsOnEachPlane.get (z);		// May be null					
						
						// This finds all unit types for which we have at least one desired location on this plane
						final List<AIUnitType> wantedUnitTypes = desiredSpecialUnitLocations.entrySet ().stream ().filter (e -> e.getValue ().stream ().anyMatch (c -> c.getZ () == finalZ)).map (e -> e.getKey ()).filter
						
							// Then out of those, find the unit types that we don't already have
							(ut -> (specialistUnitsOnThisPlane == null) || (!specialistUnitsOnThisPlane.containsKey (ut))).collect (Collectors.toList ());
						
						wantedUnitTypesOnEachPlane.put (z, wantedUnitTypes);
						
						for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
							for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
							{
								final OverlandMapCityData cityData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x).getCityData ();
								if ((cityData != null) && (cityData.getCityOwnerID () == player.getPlayerDescription ().getPlayerID ()) && (cityData.getCityPopulation () >= 1000) &&
									((CommonDatabaseConstants.BUILDING_HOUSING.equals (cityData.getCurrentlyConstructingBuildingID ())) ||
									(CommonDatabaseConstants.BUILDING_TRADE_GOODS.equals (cityData.getCurrentlyConstructingBuildingID ()))))
								{
									final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (x, y, z);
									final boolean isUnitFactory = (isRaiders) || ((unitFactories != null) && (unitFactories.contains (cityLocation)));
		
									// Get a list of combat units we'll consider building here, which is only the best 2 in our list, and even then only if this
									// city is a unit factory.  For now, don't worry about whether we can actually afford them or not.
									final List<String> bestConstructableCombatUnitsHere = !isUnitFactory ? new ArrayList<String> () :
										cityConstructableCombatUnits.stream ().filter (u -> cityLocation.equals (u.getCityLocation ())).sorted ().limit (2).map
											(u -> u.getUnit ().getUnitID ()).collect (Collectors.toList ());
											
									// Now make a complete map of all units we could construct at this city, broken down by unit type.
									final Map<AIUnitType, List<AIConstructableUnit>> constructableHere = new HashMap<AIUnitType, List<AIConstructableUnit>> ();
									for (final AIConstructableUnit thisUnit : constructableUnits)
										if ((cityLocation.equals (thisUnit.getCityLocation ())) && (thisUnit.isCanAffordMaintenance ()) &&
											((thisUnit.getAiUnitType () != AIUnitType.COMBAT_UNIT) || (bestConstructableCombatUnitsHere.contains (thisUnit.getUnit ().getUnitID ()))))
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
									getCityAI ().decideWhatToBuild (wizardDetails, cityLocation, cityData, numberOfCities, isUnitFactory, needForNewUnitsMod, constructableHere, wantedUnitTypes,
										priv.getFogOfWarMemory ().getMap (), priv.getFogOfWarMemory ().getBuilding (),
										mom.getSessionDescription (), mom.getServerDB ());
									
									getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
										mom.getPlayers (), cityLocation, mom.getSessionDescription ().getFogOfWarSetting ());
								}
							}
					}
				}
			}  // end of "if not combat started"
		}  // end of "if can build any combat units in any cities"

		if (!combatStarted)
		{
			// Only wizards can use alchemy (raiders don't need mana)
			// Make sure we do this before rush buying projects in cities, as generating mana for Spell of Return is more important
			if ((getPlayerKnowledgeUtils ().isWizard (wizardDetails.getWizardID ())) && (mom.getGeneralPublicKnowledge ().getTurnNumber () >= 20))
			{
				considerAlchemy (player, wizardDetails, mom.getPlayers (), mom.getServerDB ());
				decideMagicPowerDistribution (player, wizardDetails, mom.getPlayers (), mom.getSessionDescription ().getSpellSetting (), mom.getServerDB ());
			}
			
			if (numberOfCities > 0)
			{
				// Decide optimal tax rate
				getCityAI ().decideTaxRate (player, mom);
				
				// This relies on knowing what's being built in each city and the tax rate, so do it almost last
				getCityAI ().setOptionalFarmersInAllCities (player, mom);
				
				// This relies on knowing the production each city is generating, so need the number of farmers + workers set, so do it really last
				getCityAI ().checkForRushBuying (player, mom);
			}
			
			// Only wizards can do anything with spells
			if ((getPlayerKnowledgeUtils ().isWizard (wizardDetails.getWizardID ())) && (wizardDetails.getWizardState () == WizardState.ACTIVE))
			{
				// Do we need to choose a spell to research?
				if (priv.getSpellIDBeingResearched () == null)
					getSpellAI ().decideWhatToResearch (player, mom.getServerDB ());
				
				// Pick spells to cast overland
				getSpellAI ().decideWhatToCastOverland (player, wizardDetails, constructableUnits, wantedUnitTypesOnEachPlane, mom);
			}
		}

		final boolean aiTurnCompleted = !combatStarted;
		return aiTurnCompleted;
	}
	
	/**
	 * @param player AI player to consider converting gold into mana for
	 * @param wizardDetails AI wizard to consider converting gold into mana for
	 * @param players Players list
	 * @param db Lookup lists built over the XML database
     * @throws RecordNotFoundException If we can't find one of our picks in the database
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	final void considerAlchemy (final PlayerServerDetails player, final KnownWizardDetails wizardDetails, final List<PlayerServerDetails> players, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		
		// Want 10x casting skill in MP
		final int desiredMana = getResourceValueUtils ().calculateModifiedCastingSkill (priv.getResourceValue (), wizardDetails, players, priv.getFogOfWarMemory (), db, true) * 10;
		final int currentMana = getResourceValueUtils ().findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
		log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " has " + currentMana + " MP and wants minimum " + desiredMana + " MP from Alchemy");
		
		if (desiredMana > currentMana)
		{
			final boolean alchemyRetort = (getPlayerPickUtils ().getQuantityOfPick (wizardDetails.getPick (), CommonDatabaseConstants.RETORT_ID_ALCHEMY) > 0);
			
			int manaToConvert = desiredMana - currentMana;
			int goldToConvert = alchemyRetort ? manaToConvert : (manaToConvert * 2);
			
			// Have we got enough gold?
			final int currentGold = getResourceValueUtils ().findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
			if (currentGold < goldToConvert)
			{
				goldToConvert = currentGold;
				
				// Make sure its an even number
				if ((!alchemyRetort) && (goldToConvert % 2 != 0))
					goldToConvert--;
				
				manaToConvert = alchemyRetort ? goldToConvert : (goldToConvert / 2);
				log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " only has enough gold to convert " + manaToConvert + " MP");
			}
			
			if (manaToConvert > 0)
			{
				log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " converting " + goldToConvert + " GP into " + manaToConvert + " MP");
				
				getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, -goldToConvert);
				getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, manaToConvert);
			}
		}
	}
	
	/**
	 * @param player AI player to decide magic power distribution for
	 * @param wizardDetails AI wizard to decide magic power distribution for
	 * @param players Players list
	 * @param spellSettings Spell combination settings, either from the server XML cache or the Session description
	 * @param db Lookup lists built over the XML database
     * @throws RecordNotFoundException If we can't find one of our picks in the database
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	final void decideMagicPowerDistribution (final PlayerServerDetails player, final KnownWizardDetails wizardDetails, final List<PlayerServerDetails> players,
		final SpellSetting spellSettings, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		
		// Want 10x casting skill in MP
		final int desiredMana = getResourceValueUtils ().calculateModifiedCastingSkill (priv.getResourceValue (), wizardDetails, players, priv.getFogOfWarMemory (), db, true) * 10;
		final int currentMana = getResourceValueUtils ().findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
		log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " has " + currentMana + " MP and wants minimum " + desiredMana + " MP from magic power");
		
		// Increase MP from magic power until we generate enough to hit desired MP value
		priv.getMagicPowerDistribution ().setManaRatio (0);
		int manaFromMagicPower = 0;
		while ((priv.getMagicPowerDistribution ().getManaRatio () < CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX) &&
			(currentMana + manaFromMagicPower < desiredMana))
		{
			priv.getMagicPowerDistribution ().setManaRatio (priv.getMagicPowerDistribution ().getManaRatio () + 1);
		
			// This method calculates MP split from magic power, and takes Archmage 25% bonus into account
			manaFromMagicPower = getResourceValueUtils ().calculateAmountPerTurnForProductionType
				(priv, wizardDetails.getPick (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, spellSettings, db);
		}

		log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " set MP from magic power at " + priv.getMagicPowerDistribution ().getManaRatio () +
			" / " + CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX + " to generate " + manaFromMagicPower + " MP");
		
		// Now figure split between research and skill improvement
		int remainingPowerDistribution = CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX - priv.getMagicPowerDistribution ().getManaRatio ();
		
		// If we already know Spell of Mastery, remaining research is mostly pointless so throw everything into skill improvement
		final SpellResearchStatusID spellOfMasteryResearch = getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (),
			CommonDatabaseConstants.SPELL_ID_SPELL_OF_MASTERY).getStatus ();
		
		if (spellOfMasteryResearch == SpellResearchStatusID.AVAILABLE)
		{
			priv.getMagicPowerDistribution ().setResearchRatio (0);
			priv.getMagicPowerDistribution ().setSkillRatio (remainingPowerDistribution);
			log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " knows Spell of Mastery so throwing all remaining " +
				remainingPowerDistribution + " / " + CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX + " into skill improvement");
		}
		
		// If Spell of Mastery is available as one of 8 research options (even if we are researching something else first) then throw everything at it
		else if (spellOfMasteryResearch == SpellResearchStatusID.RESEARCHABLE_NOW)
		{
			priv.getMagicPowerDistribution ().setResearchRatio (remainingPowerDistribution);
			priv.getMagicPowerDistribution ().setSkillRatio (0);
			log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " can research Spell of Mastery so throwing all remaining " +
				remainingPowerDistribution + " / " + CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX + " into research");
		}
		
		else
		{
			priv.getMagicPowerDistribution ().setResearchRatio (remainingPowerDistribution / 2);
			priv.getMagicPowerDistribution ().setSkillRatio ((remainingPowerDistribution + 1) / 2);
			log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " splitting remaining magic power 50/50 so research at " +
				priv.getMagicPowerDistribution ().getResearchRatio () + " / " + CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX + " and skill improvement at " +
				priv.getMagicPowerDistribution ().getSkillRatio () + " / " + CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX);
		}		
	}

	/**
	 * AI player decides whether to accept an offer.  Assumes we've already validated that they can afford it.
	 * 
	 * @param player Player who is accepting an offer
	 * @param offer Offer being accepted
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final void decideOffer (final PlayerServerDetails player, final NewTurnMessageOffer offer, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		// AI players should have plenty money so just accept all offers
		getOfferGenerator ().acceptOffer (player, offer, mom);
	}
	
	/**
	 * @param picks Wizard's picks
	 * @param db Lookup lists built over the XML database
	 * @return Chosen personality
	 */
	@Override
	public final String decideWizardPersonality (final List<PlayerPick> picks, final CommonDatabase db)
	{
		final WeightedChoicesImpl<String> choices = new WeightedChoicesImpl<String> ();
		choices.setRandomUtils (getRandomUtils ());
		
		// Each personality has under it rules for how to calculate the chance of picking it
		for (final WizardPersonality personality : db.getWizardPersonality ())
		{
			int weighting = 0;
			for (final SelectedByPick selectedByPick : personality.getPersonalitySelectedByPick ())
				if (getPlayerPickUtils ().getQuantityOfPick (picks, selectedByPick.getPickID ()) > 0)
					weighting = weighting + selectedByPick.getWeighting ();
			
			if (weighting > 0)
			{
				log.debug ("decideWizardPersonality: Weighting for personality " + personality.getWizardPersonalityID () + " = " + weighting);
				choices.add (weighting, personality.getWizardPersonalityID ());
			}
		}
		
		// Pick random personality, or default to the first one
		String personalityID = choices.nextWeightedValue ();
		if (personalityID == null)
			personalityID = db.getWizardPersonality ().get (0).getWizardPersonalityID ();
		
		return personalityID;
	}
	
	/**
	 * @param picks Wizard's picks
	 * @param db Lookup lists built over the XML database
	 * @return Chosen objective
	 */
	@Override
	public final String decideWizardObjective (final List<PlayerPick> picks, final CommonDatabase db)
	{
		final WeightedChoicesImpl<String> choices = new WeightedChoicesImpl<String> ();
		choices.setRandomUtils (getRandomUtils ());
		
		// Each objective has under it rules for how to calculate the chance of picking it
		for (final WizardObjective objective : db.getWizardObjective ())
		{
			int weighting = 0;
			for (final SelectedByPick selectedByPick : objective.getObjectiveSelectedByPick ())
				if (getPlayerPickUtils ().getQuantityOfPick (picks, selectedByPick.getPickID ()) > 0)
					weighting = weighting + selectedByPick.getWeighting ();
			
			if (weighting > 0)
			{
				log.debug ("decideWizardObjective: Weighting for objective " + objective.getWizardObjectiveID () + " = " + weighting);
				choices.add (weighting, objective.getWizardObjectiveID ());
			}
		}
		
		// Pick random objective, or default to the first one
		String objectiveID = choices.nextWeightedValue ();
		if (objectiveID == null)
			objectiveID = db.getWizardObjective ().get (0).getWizardObjectiveID ();
		
		return objectiveID;
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

	/**
	 * @return Resource value utils
	 */
	public final ResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
	}

	/**
	 * @param utils Resource value utils
	 */
	public final void setResourceValueUtils (final ResourceValueUtils utils)
	{
		resourceValueUtils = utils;
	}

	/**
	 * @return Player pick utils
	 */
	public final PlayerPickUtils getPlayerPickUtils ()
	{
		return playerPickUtils;
	}

	/**
	 * @param utils Player pick utils
	 */
	public final void setPlayerPickUtils (final PlayerPickUtils utils)
	{
		playerPickUtils = utils;
	}

	/**
	 * @return Offer generator
	 */
	public final OfferGenerator getOfferGenerator ()
	{
		return offerGenerator;
	}

	/**
	 * @param g Offer generator
	 */
	public final void setOfferGenerator (final OfferGenerator g)
	{
		offerGenerator = g;
	}

	/**
	 * @return Spell utils
	 */
	public final SpellUtils getSpellUtils ()
	{
		return spellUtils;
	}

	/**
	 * @param utils Spell utils
	 */
	public final void setSpellUtils (final SpellUtils utils)
	{
		spellUtils = utils;
	}

	/**
	 * @return Methods for working with wizardIDs
	 */
	public final PlayerKnowledgeUtils getPlayerKnowledgeUtils ()
	{
		return playerKnowledgeUtils;
	}

	/**
	 * @param k Methods for working with wizardIDs
	 */
	public final void setPlayerKnowledgeUtils (final PlayerKnowledgeUtils k)
	{
		playerKnowledgeUtils = k;
	}

	/**
	 * @return Methods for finding KnownWizardDetails from the list
	 */
	public final KnownWizardUtils getKnownWizardUtils ()
	{
		return knownWizardUtils;
	}

	/**
	 * @param k Methods for finding KnownWizardDetails from the list
	 */
	public final void setKnownWizardUtils (final KnownWizardUtils k)
	{
		knownWizardUtils = k;
	}

	/**
	 * @return For calculating relation scores between two wizards
	 */
	public final RelationAI getRelationAI ()
	{
		return relationAI;
	}

	/**
	 * @param ai For calculating relation scores between two wizards
	 */
	public final void setRelationAI (final RelationAI ai)
	{
		relationAI = ai;
	}

	/**
	 * @return Methods for AI making decisions about diplomacy with other wizards
	 */
	public final DiplomacyAI getDiplomacyAI ()
	{
		return diplomacyAI;
	}

	/**
	 * @param ai Methods for AI making decisions about diplomacy with other wizards
	 */
	public final void setDiplomacyAI (final DiplomacyAI ai)
	{
		diplomacyAI = ai;
	}
	
	/**
	 * @return During an AI player's turn, works out what diplomacy proposals they may want to initiate to other wizards
	 */
	public final DiplomacyProposalsAI getDiplomacyProposalsAI ()
	{
		return diplomacyProposalsAI;
	}

	/**
	 * @param p During an AI player's turn, works out what diplomacy proposals they may want to initiate to other wizards
	 */
	public final void setDiplomacyProposalsAI (final DiplomacyProposalsAI p)
	{
		diplomacyProposalsAI = p;
	}

	/**
	 * @return Methods for processing agreed diplomatic actions
	 */
	public final DiplomacyProcessing getDiplomacyProcessing ()
	{
		return diplomacyProcessing;
	}
	
	/**
	 * @param p Methods for processing agreed diplomatic actions
	 */
	public final void setDiplomacyProcessing (final DiplomacyProcessing p)
	{
		diplomacyProcessing = p;
	}

	/**
	 * @return Methods for processing diplomacy requests from AI player to another AI player
	 */
	public final AIToAIDiplomacy getAiToAIDiplomacy ()
	{
		return aiToAIDiplomacy;
	}

	/**
	 * @param d Methods for processing diplomacy requests from AI player to another AI player
	 */
	public final void setAIToAIDiplomacy (final AIToAIDiplomacy d)
	{
		aiToAIDiplomacy = d;
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
	 * @return Process for making sure one wizard has met another wizard
	 */
	public final KnownWizardServerUtils getKnownWizardServerUtils ()
	{
		return knownWizardServerUtils;
	}

	/**
	 * @param k Process for making sure one wizard has met another wizard
	 */
	public final void setKnownWizardServerUtils (final KnownWizardServerUtils k)
	{
		knownWizardServerUtils = k;
	}
}