package momime.server.process;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.calculations.SkillCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.WizardPick;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomGeneralPublicKnowledge;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.messages.PendingMovement;
import momime.common.messages.PlayerPick;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;
import momime.common.messages.TurnSystem;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.AddNewTurnMessagesMessage;
import momime.common.messages.servertoclient.ChooseInitialSpellsNowMessage;
import momime.common.messages.servertoclient.ChooseYourRaceNowMessage;
import momime.common.messages.servertoclient.ChosenCustomPhotoMessage;
import momime.common.messages.servertoclient.ChosenStandardPhotoMessage;
import momime.common.messages.servertoclient.ChosenWizardMessage;
import momime.common.messages.servertoclient.EndOfContinuedMovementMessage;
import momime.common.messages.servertoclient.ErasePendingMovementsMessage;
import momime.common.messages.servertoclient.FullSpellListMessage;
import momime.common.messages.servertoclient.OnePlayerSimultaneousTurnDoneMessage;
import momime.common.messages.servertoclient.PendingMovementMessage;
import momime.common.messages.servertoclient.ReplacePicksMessage;
import momime.common.messages.servertoclient.SetCurrentPlayerMessage;
import momime.common.messages.servertoclient.StartGameMessage;
import momime.common.messages.servertoclient.StartSimultaneousTurnMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.PlayerKnowledgeUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.ai.CityAI;
import momime.server.ai.MomAI;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.calculations.ServerSpellCalculations;
import momime.server.database.PickFreeSpellSvr;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.UnitSvr;
import momime.server.database.WizardPickCountSvr;
import momime.server.database.WizardSvr;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.FogOfWarProcessing;
import momime.server.knowledge.MomGeneralServerKnowledgeEx;
import momime.server.utils.PlayerPickServerUtils;
import momime.server.utils.PlayerServerUtils;
import momime.server.utils.UnitServerUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.random.RandomUtils;

/**
 * Methods for any significant message processing to do with game startup and the turn system that isn't done in the message implementations
 */
public final class PlayerMessageProcessingImpl implements PlayerMessageProcessing
{
	/** Class logger */
	private final Log log = LogFactory.getLog (PlayerMessageProcessingImpl.class);

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** MemoryGridCell utils */
	private MemoryGridCellUtils memoryGridCellUtils;
	
	/** Skill calculations */
	private SkillCalculations skillCalculations;
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** City processing methods */
	private CityProcessing cityProcessing;

	/** Spell queueing methods */
	private SpellQueueing spellQueueing;
	
	/** Fog of war update methods */
	private FogOfWarProcessing fogOfWarProcessing;
	
	/** Resource calculations */
	private ServerResourceCalculations serverResourceCalculations;
	
	/** Server-only pick utils */
	private PlayerPickServerUtils playerPickServerUtils;
	
	/** Server-only spell calculations */
	private ServerSpellCalculations serverSpellCalculations;

	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;
	
	/** AI player turns */
	private MomAI momAI;
	
	/** AI decisions about cities */
	private CityAI cityAI;
	
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;

	/** Player utils */
	private PlayerServerUtils playerServerUtils;
	
	/** Simultaneous turns processing */
	private SimultaneousTurnsProcessing simultaneousTurnsProcessing;
	
	/** Simultaneous turns combat scheduler */
	private CombatScheduler combatScheduler;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/**
	 * Message we send to the server when we choose which wizard we want to be; AI players also call this to do their wizard, picks and spells setup
	 * which is why this isn't all just in ChooseWizardMessageImpl
	 *
	 * @param wizardID wizard ID the player wants to choose
	 * @param player Player who sent the message
	 * @param players List of players in the session
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If various elements cannot be found in the DB
	 * @throws MomException If an AI player has enough books that they should get some free spells, but we can't find any suitable free spells to give them
	 */
	@Override
	public final void chooseWizard (final String wizardID, final PlayerServerDetails player,
		final List<PlayerServerDetails> players, final MomSessionDescription sd, final ServerDatabaseEx db)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException
	{
		log.trace ("Entering chooseWizard: Player ID " + player.getPlayerDescription ().getPlayerID () + ", " + wizardID);

		// Check if not specified
		boolean valid;
		WizardSvr wizard = null;
		if (!PlayerKnowledgeUtils.hasWizardBeenChosen (wizardID))
			valid = false;
		
		// Check if custom
		else if (PlayerKnowledgeUtils.isCustomWizard (wizardID))
			valid = sd.getDifficultyLevel ().isCustomWizards ();

		// Check if Raiders
		else if ((player.getPlayerDescription ().isHuman ()) &&
			((wizardID.equals (CommonDatabaseConstants.WIZARD_ID_MONSTERS)) || (wizardID.equals (CommonDatabaseConstants.WIZARD_ID_RAIDERS))))

			valid = false;

		// Check if found
		else
		{
			try
			{
				wizard = db.findWizard (wizardID, "chooseWizard");
				valid = true;
			}
			catch (final RecordNotFoundException e)
			{
				valid = false;
			}
		}

		// Send back appropriate message
		if (!valid)
		{
			// Tell the sender that their choice is invalid
			log.warn (player.getPlayerDescription ().getPlayerName () + " tried to choose invalid wizard ID \"" + wizardID + "\"");

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText ("Wizard choice invalid, please try again");
			player.getConnection ().sendMessageToClient (reply);
		}

		// Wizard might be valid, but check if option prohibits player from choosing same wizard as another player has already chosen
		else if ((sd.getDifficultyLevel ().isEachWizardOnlyOnce ()) && (getPlayerPickServerUtils ().findPlayerUsingWizard (players, wizardID) != null))
		{
			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText ("Game is set to only allow each Wizard to be chosen once, and another player already chose this Wizard, please try again");
			player.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			// Successful - Remember choice on the server
			final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
			ppk.setWizardID (wizardID);

			if (wizard != null)
			{
				// Set photo for pre-defined wizard, including raiders and monsters since sending this causes the client to look up their flag colour
				ppk.setStandardPhotoID (wizardID);

				if ((!wizardID.equals (CommonDatabaseConstants.WIZARD_ID_MONSTERS)) && (!wizardID.equals (CommonDatabaseConstants.WIZARD_ID_RAIDERS)))
				{
					// Find the correct node in the database for the number of player human players have
					final int desiredPickCount;
					if (player.getPlayerDescription ().isHuman ())
						desiredPickCount = sd.getDifficultyLevel ().getHumanSpellPicks ();
					else
						desiredPickCount = sd.getDifficultyLevel ().getAiSpellPicks ();

					WizardPickCountSvr pickCount = null;
					final Iterator<WizardPickCountSvr> iter = wizard.getWizardPickCounts ().iterator ();
					while ((pickCount == null) && (iter.hasNext ()))
					{
						final WizardPickCountSvr thisPickCount = iter.next ();
						if (thisPickCount.getPickCount () == desiredPickCount)
							pickCount = thisPickCount;
					}

					if (pickCount == null)
						throw new RecordNotFoundException (WizardPickCountSvr.class, wizardID + "-" + desiredPickCount, "chooseWizard");

					// Read pre-defined wizard's list of picks from the DB and send them to the player
					// We'll send them to everyone else when the game starts
					for (final WizardPick srcPick : pickCount.getWizardPick ())
					{
						final PlayerPick destPick = new PlayerPick ();
						destPick.setPickID (srcPick.getPick ());
						destPick.setQuantity (srcPick.getQuantity ());
						destPick.setOriginalQuantity (srcPick.getQuantity ());
						ppk.getPick ().add (destPick);
					}

					// Debug picks to server log file
					if (log.isDebugEnabled ())
					{
						String picksDebugDescription = null;

						for (final PlayerPick pick : ppk.getPick ())
						{
							if (picksDebugDescription == null)
								picksDebugDescription = pick.getQuantity () + "x" + pick.getPickID ();
							else
								picksDebugDescription = picksDebugDescription + ", " + pick.getQuantity () + "x" + pick.getPickID ();
						}

						log.debug ("chooseWizard: Read picks for player '" + player.getPlayerDescription ().getPlayerName () + "' who has chosen pre-defined wizard \"" + wizardID + "\":  " + picksDebugDescription);
					}

					// Send picks to the player - they need to know their own picks so they know whether they're allowed to pick a Myrran race not
					// We don't send picks to other players until the game is starting up
					if (player.getPlayerDescription ().isHuman ())
					{
						final ReplacePicksMessage picksMsg = new ReplacePicksMessage ();
						picksMsg.setPlayerID (player.getPlayerDescription ().getPlayerID ());
						picksMsg.getPick ().addAll (ppk.getPick ());
						player.getConnection ().sendMessageToClient (picksMsg);
					}

					// Tell client to either pick free starting spells or pick a race, depending on whether the pre-defined wizard chosen has >1 of any kind of book
					// Its fine to do this before we confirm to the client that their wizard choice was OK by the mmChosenWizard message sent below
					if (player.getPlayerDescription ().isHuman ())
					{
						// This will tell the client to either pick free spells for the first magic realm that they have earned free spells in, or pick their race, depending on what picks they've chosen
						log.debug ("chooseWizard: About to search for first realm (if any) where human player " + player.getPlayerDescription ().getPlayerName () + " gets free spells");
						final ChooseInitialSpellsNowMessage chooseSpellsMsg = getPlayerPickServerUtils ().findRealmIDWhereWeNeedToChooseFreeSpells (player, db);
						if (chooseSpellsMsg != null)
							player.getConnection ().sendMessageToClient (chooseSpellsMsg);
						else
							player.getConnection ().sendMessageToClient (new ChooseYourRaceNowMessage ());
					}
					else
					{
						// For AI players, we call this repeatedly until all free spells have been chosen
						log.debug ("chooseWizard: About to choose all free spells for AI player " + player.getPlayerDescription ().getPlayerName ());

						while (getPlayerPickServerUtils ().findRealmIDWhereWeNeedToChooseFreeSpells (player, db) != null);
					}
				}
			}

			// Tell everyone about the wizard this player has chosen
			broadcastWizardChoice (players, player);
		}

		log.trace ("Exiting chooseWizard");
	}

	/**
	 * Sends the wizard choice of the specified player to all human players in the specified session players list
	 * @param players List of players in the session
	 * @param player Player whose wizard choice we are sending
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	private final void broadcastWizardChoice (final List<PlayerServerDetails> players, final PlayerServerDetails player)
		throws JAXBException, XMLStreamException
	{
		log.trace ("Entering broadcastWizardChoice: Player ID " + player.getPlayerDescription ().getPlayerID ());

		// Convert empty string (custom wizard) to a null
		final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();

		final ChosenWizardMessage msg = new ChosenWizardMessage ();
		msg.setPlayerID (player.getPlayerDescription ().getPlayerID ());
		msg.setWizardID (ppk.getWizardID ());

		getMultiplayerSessionServerUtils ().sendMessageToAllClients (players, msg);

		log.trace ("Exiting broadcastWizardChoice");
	}

	/**
	 * @param wizard Wizard this AI player is playing
	 * @return Player description created for this AI player
	 */
	private final PlayerDescription createAiPlayerDescription (final WizardSvr wizard)
	{
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerName (wizard.getWizardID () + "-" + wizard.getWizardName ());

		return pd;
	}

	/**
	 * Finds all heroes in the XML database and creates them as ungenerated units for all players
	 * @param players List of players in the session
	 * @param gsk Server knowledge structure
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws MomException If initialStatus is an inappropriate value
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws PlayerNotFoundException This only gets generated if addUnitOnServerAndClients tries to send into to players, but we pass null for player list, so won't happen
	 * @throws JAXBException This only gets generated if addUnitOnServerAndClients tries to send into to players, but we pass null for player list, so won't happen
	 * @throws XMLStreamException This only gets generated if addUnitOnServerAndClients tries to send into to players, but we pass null for player list, so won't happen
	 */
	private final void createHeroes (final List<PlayerServerDetails> players, final MomGeneralServerKnowledgeEx gsk,
		final MomSessionDescription sd, final ServerDatabaseEx db)
		throws MomException, RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering createHeroes");

		for (final UnitSvr thisUnit : db.getUnits ())
			if (thisUnit.getUnitMagicRealm ().equals (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))

				// Add this hero for all players, even raiders, just not the monsters
				// We won't end up sending these to the client since we're setting status as 'not generated'
				for (final PlayerServerDetails thisPlayer : players)
				{
					final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) thisPlayer.getPersistentPlayerPublicKnowledge ();
					if (!ppk.getWizardID ().equals (CommonDatabaseConstants.WIZARD_ID_MONSTERS))
						getFogOfWarMidTurnChanges ().addUnitOnServerAndClients (gsk, thisUnit.getUnitID (), null, null, null, thisPlayer, UnitStatusID.NOT_GENERATED, null, sd, db);
				}

		log.trace ("Exiting createHeroes");
	}


	/**
	 * If all players have chosen their wizards and, if necessary, custom picks, then sends message to tell everyone to start
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending any messages to the clients
	 * @throws XMLStreamException If there is a problem sending any messages to the clients
	 * @throws MomException If there is a problem in any game logic or data
	 * @throws RecordNotFoundException If various elements cannot be found in the DB
	 * @throws PlayerNotFoundException If we encounter players that we cannot find in the list
	 */
	@Override
	public final void checkIfCanStartGame (final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, MomException, RecordNotFoundException, PlayerNotFoundException
	{
		log.trace ("Entering checkIfCanStartGame");

		if (getPlayerPickServerUtils ().allPlayersHaveChosenAllDetails (mom.getPlayers (), mom.getSessionDescription ()))
		{
			// Add AI wizards
			log.debug ("checkIfCanStartGame: Yes, " + mom.getSessionDescription ().getAiPlayerCount () + " AI wizards to add");
			mom.getSessionLogger ().info ("All Human players joined - adding AI player(s) and starting game...");

			if (mom.getSessionDescription ().getAiPlayerCount () > 0)
			{
				// Get list of wizard IDs for AI players to choose from
				final List<WizardSvr> availableWizards = getPlayerPickServerUtils ().listWizardsForAIPlayers (mom.getPlayers (), mom.getServerDB ());
				for (int aiPlayerNo = 0; aiPlayerNo < mom.getSessionDescription ().getAiPlayerCount (); aiPlayerNo++)
				{
					// Pick a random wizard for this AI player
					if (availableWizards.size () == 0)
						throw new MomException ("Not enough Wizards defined for number of AI players");

					final WizardSvr chosenWizard = availableWizards.get (getRandomUtils ().nextInt (availableWizards.size ()));
					availableWizards.remove (chosenWizard);

					// Add AI player
					final PlayerServerDetails aiPlayer = mom.addComputerPlayer (createAiPlayerDescription (chosenWizard));

					// Choose wizard
					chooseWizard (chosenWizard.getWizardID (), aiPlayer, mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());

					// Choose race
					final MomTransientPlayerPrivateKnowledge priv = (MomTransientPlayerPrivateKnowledge) aiPlayer.getTransientPlayerPrivateKnowledge ();
					final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) aiPlayer.getPersistentPlayerPublicKnowledge ();

					priv.setFirstCityRaceID (getPlayerPickServerUtils ().chooseRandomRaceForPlane
						(getPlayerPickServerUtils ().startingPlaneForWizard (ppk.getPick (), mom.getServerDB ()), mom.getServerDB ()));
				}
			}

			// Add raiders
			final PlayerServerDetails raidersPlayer = mom.addComputerPlayer (createAiPlayerDescription
				(mom.getServerDB ().findWizard (CommonDatabaseConstants.WIZARD_ID_RAIDERS, "checkIfCanStartGame")));

			chooseWizard (CommonDatabaseConstants.WIZARD_ID_RAIDERS, raidersPlayer, mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());

			// Add monsters
			final PlayerServerDetails monstersPlayer = mom.addComputerPlayer (createAiPlayerDescription
				(mom.getServerDB ().findWizard (CommonDatabaseConstants.WIZARD_ID_MONSTERS, "checkIfCanStartGame")));

			chooseWizard (CommonDatabaseConstants.WIZARD_ID_MONSTERS, monstersPlayer, mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());

			// Broadcast player data
			log.debug ("checkIfCanStartGame: Broadcasting player picks and determining which spells not chosen for free will be researchable");
			mom.getSessionLogger ().info ("Broadcasting player picks and randomizing initial spells...");

			for (final PlayerServerDetails thisPlayer : mom.getPlayers ())
			{
				final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) thisPlayer.getPersistentPlayerPublicKnowledge ();
				final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();

				// Send photo, including for Raiders since this sends their flag colour
				if (ppk.getStandardPhotoID () != null)
				{
					final ChosenStandardPhotoMessage msg = new ChosenStandardPhotoMessage ();
					msg.setPlayerID (thisPlayer.getPlayerDescription ().getPlayerID ());
					msg.setPhotoID (ppk.getStandardPhotoID ());

					getMultiplayerSessionServerUtils ().sendMessageToAllClients (mom.getPlayers (), msg);
				}

				else if (ppk.getCustomPhoto () != null)
				{
					final ChosenCustomPhotoMessage msg = new ChosenCustomPhotoMessage ();
					msg.setPlayerID (thisPlayer.getPlayerDescription ().getPlayerID ());
					msg.setFlagColour (ppk.getCustomFlagColour ());
					msg.setNdgBmpImage (ppk.getCustomPhoto ());

					getMultiplayerSessionServerUtils ().sendMessageToAllClients (mom.getPlayers (), msg);
				}

				// Send picks to everyone - note we don't know if they've already received them, if e.g. player
				// 1 joins and makes their picks then these get stored into public info on the server, then
				// player 2 joins and will be sent to them
				final ReplacePicksMessage picksMsg = new ReplacePicksMessage ();
				picksMsg.setPlayerID (thisPlayer.getPlayerDescription ().getPlayerID ());
				picksMsg.getPick ().addAll (ppk.getPick ());
				getMultiplayerSessionServerUtils ().sendMessageToAllClients (mom.getPlayers (), picksMsg);

				// Grant any free spells the player gets from the picks they've chosen (i.e. Enchant Item & Create Artifact from Artificer)
				final List<String> freeSpellIDs = new ArrayList<String> ();
				for (final PlayerPick pick : ppk.getPick ())
					for (final PickFreeSpellSvr freeSpell : mom.getServerDB ().findPick (pick.getPickID (), "checkIfCanStartGame").getPickFreeSpells ())
						freeSpellIDs.add (freeSpell.getFreeSpellID ());

				if (freeSpellIDs.size () > 0)
					for (final SpellResearchStatus thisSpell : priv.getSpellResearchStatus ())
						if (freeSpellIDs.contains (thisSpell.getSpellID ()))
							thisSpell.setStatus (SpellResearchStatusID.AVAILABLE);

				// For all the spells that we did NOT get for free at the start of the game, decides whether or not they are in our spell book to be available to be researched
				getServerSpellCalculations ().randomizeResearchableSpells (priv.getSpellResearchStatus (), ppk.getPick (), mom.getServerDB ());

				// Give player 8 spells to pick from, out of all those we'll eventually be able to research
				getServerSpellCalculations ().randomizeSpellsResearchableNow (priv.getSpellResearchStatus (), mom.getServerDB ());

				// Send players' spells to them (and them only)
				if (thisPlayer.getPlayerDescription ().isHuman ())
				{
					final FullSpellListMessage spellsMsg = new FullSpellListMessage ();
					spellsMsg.getSpellResearchStatus ().addAll (priv.getSpellResearchStatus ());
					thisPlayer.getConnection ().sendMessageToClient (spellsMsg);
				}
			}

			// Add monsters in nodes/lairs/towers - can only do this after we've added the players
			mom.getSessionLogger ().info ("Filling nodes, lairs & towers with monsters...");
			mom.getOverlandMapGenerator ().fillNodesLairsAndTowersWithMonsters (monstersPlayer);

			// Sort out heroes
			mom.getSessionLogger ().info ("Loading list of heroes for each player...");
			createHeroes (mom.getPlayers (), mom.getGeneralServerKnowledge (), mom.getSessionDescription (), mom.getServerDB ());

			if (mom.getSessionDescription ().getUnitSetting ().isRollHeroSkillsAtStartOfGame ())
			{
				mom.getSessionLogger ().info ("Randomzing hero skills for each player...");
				for (final MemoryUnit thisUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
					if (mom.getServerDB ().findUnit (thisUnit.getUnitID (), "checkIfCanStartGame").getUnitMagicRealm ().equals (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
						getUnitServerUtils ().generateHeroNameAndRandomSkills (thisUnit, mom.getServerDB ());
			}

			// Create cities
			mom.getSessionLogger ().info ("Creating starting cities...");
			getCityProcessing ().createStartingCities (mom.getPlayers (), mom.getGeneralServerKnowledge (), mom.getSessionDescription (), mom.getServerDB ());

			// Now we've created starting cities, we can figure out the initial fog of war area that each player can see
			mom.getSessionLogger ().info ("Generating and sending initial fog of war...");
			for (final PlayerServerDetails thisPlayer : mom.getPlayers ())
				getFogOfWarProcessing ().updateAndSendFogOfWar (mom.getGeneralServerKnowledge ().getTrueMap (), thisPlayer,
					mom.getPlayers (), true, "checkIfCanStartGame", mom.getSessionDescription (), mom.getServerDB ());

			// Give each wizard initial skill and gold, and setting optional farmers in all cities
			mom.getSessionLogger ().info ("Setting wizards' initial skill and gold, and optional farmers in all cities");
			for (final PlayerServerDetails thisPlayer : mom.getPlayers ())
			{
				final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) thisPlayer.getPersistentPlayerPublicKnowledge ();
				final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();

				if (PlayerKnowledgeUtils.isWizard (ppk.getWizardID ()))
				{
					// Calculate each wizard's initial starting casting skills
					// This effectively gives each wizard some starting stored skill in RE10, which will then be sent to the client by RecalculateGlobalProductionValues below
					getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT,
						getSkillCalculations ().getSkillPointsRequiredForCastingSkill (getPlayerPickServerUtils ().getTotalInitialSkill
							(ppk.getPick (), mom.getServerDB ())));

					// Give each wizard their starting gold
					final int startingGold;
					if (thisPlayer.getPlayerDescription ().isHuman ())
						startingGold = mom.getSessionDescription ().getDifficultyLevel ().getHumanStartingGold ();
					else
						startingGold = mom.getSessionDescription ().getDifficultyLevel ().getAiStartingGold ();

					getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, startingGold);
				}

				// Default each player's farmers to just enough to feed their initial units
				getCityAI ().setOptionalFarmersInAllCities (mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), thisPlayer,
					mom.getServerDB (), mom.getSessionDescription ());
			}

			// Calculate and send initial production values - This is especially important in one-at-a-time games with more
			// than one human player, since e.g. player 2 won't otherwise be sent their power base figure until player 1 hits 'next turn'
			mom.getSessionLogger ().info ("Calculating initial production values...");
			getServerResourceCalculations ().recalculateGlobalProductionValues (0, false, mom);

			// Kick off the game - this shows the map screen for the first time
			mom.getSessionLogger ().info ("Starting game...");
			getMultiplayerSessionServerUtils ().sendMessageToAllClients (mom.getPlayers (), new StartGameMessage ());

			// Kick off the first turn
			mom.getSessionLogger ().info ("Kicking off first turn...");
			switch (mom.getSessionDescription ().getTurnSystem ())
			{
				case ONE_PLAYER_AT_A_TIME:
					switchToNextPlayer (mom);
					break;

				case SIMULTANEOUS:
					kickOffSimultaneousTurn (mom);
					break;
					
				default:
					throw new MomException ("checkIfCanStartGame encountered an unknown turn system " + mom.getSessionDescription ().getTurnSystem ());
			}
		}

		log.trace ("Exiting checkIfCanStartGame");
	}

	/**
	 * Processes the 'start phase' (for want of something better to call it), which happens just at the start of each player's turn
	 *
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param onlyOnePlayerID If zero, will process start phase for all players; if specified will process start phase only for the specified player
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	private final void startPhase (final MomSessionVariables mom, final int onlyOnePlayerID)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering startPhase: Player ID " + onlyOnePlayerID);

		if (onlyOnePlayerID == 0)
			mom.getSessionLogger ().info ("Start phase for everyone turn " + mom.getGeneralPublicKnowledge ().getTurnNumber () + "...");
		else
			mom.getSessionLogger ().info ("Start phase for turn " + mom.getGeneralPublicKnowledge ().getTurnNumber () + " - " + getMultiplayerSessionServerUtils ().findPlayerWithID
				(mom.getPlayers (), onlyOnePlayerID, "startPhase").getPlayerDescription ().getPlayerName () + "...");

		// Give units their full movement back again
		getUnitUtils ().resetUnitOverlandMovement (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), onlyOnePlayerID, mom.getServerDB ());

		// Heal hurt units 1pt and gain 1exp
		getFogOfWarMidTurnChanges ().healUnitsAndGainExperience (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), onlyOnePlayerID,
			mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting ());

		// Allow another building to be sold
		getMemoryGridCellUtils ().blankBuildingsSoldThisTurn (mom.getGeneralServerKnowledge ().getTrueMap ().getMap ());

		// Global production - only need to do a simple recalc on turn 1, with no accumulation and no city growth
		if (mom.getGeneralPublicKnowledge ().getTurnNumber () > 1)
		{
			getServerResourceCalculations ().recalculateGlobalProductionValues (onlyOnePlayerID, true, mom);

			// Do this AFTER calculating and accumulating production, so checking for units dying due to insufficient rations happens before city populations might change
			getCityProcessing ().growCitiesAndProgressConstructionProjects (onlyOnePlayerID, mom.getPlayers (), mom.getGeneralServerKnowledge (),
				mom.getSessionDescription (), mom.getServerDB ());
		}

		// Now need to do one final recalc to take into account
		// 1) Cities producing more food/gold due to increased population
		// 2) Cities eating more food due to increased population
		// 3) Completed buildings (both bonuses and increased maintenance)
		getServerResourceCalculations ().recalculateGlobalProductionValues (onlyOnePlayerID, false, mom);

		log.trace ("Exiting startPhase");
	}

	/**
	 * In a one-player-at-a-time game, this gets called when a player clicks the Next Turn button to tell everyone whose turn it is now
	 *
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws RecordNotFoundException If an expected element cannot be found
	 * @throws PlayerNotFoundException If the player who owns a unit, or the previous or next player cannot be found
	 * @throws MomException If the player's unit doesn't have the experience skill
	 */
	@Override
	public final void switchToNextPlayer (final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering switchToNextPlayer: Player ID " +
			mom.getGeneralPublicKnowledge ().getCurrentPlayerID () + " turn " + mom.getGeneralPublicKnowledge ().getTurnNumber ());

		// Find the current player
		int playerIndex;
		if (mom.getGeneralPublicKnowledge ().getCurrentPlayerID () == null)	// First turn
			playerIndex = mom.getPlayers ().size () - 1;		// So we make sure we trip the turn number over
		else
			playerIndex = getMultiplayerSessionServerUtils ().indexOfPlayerWithID (mom.getPlayers (), mom.getGeneralPublicKnowledge ().getCurrentPlayerID (), "switchToNextPlayer");

		// Find the next player
		if (playerIndex >= mom.getPlayers ().size () - 1)
		{
			playerIndex = 0;
			mom.getGeneralPublicKnowledge ().setTurnNumber (mom.getGeneralPublicKnowledge ().getTurnNumber () + 1);
		}
		else
			playerIndex++;

		final PlayerServerDetails currentPlayer = mom.getPlayers ().get (playerIndex);
		mom.getGeneralPublicKnowledge ().setCurrentPlayerID (currentPlayer.getPlayerDescription ().getPlayerID ());

		// Start phase for the new player
		startPhase (mom, mom.getGeneralPublicKnowledge ().getCurrentPlayerID ());

		// Tell everyone who the new current player is, and send each of them their New Turn Messages in the process
		sendNewTurnMessages (mom.getGeneralPublicKnowledge (), mom.getPlayers (), mom.getSessionDescription ().getTurnSystem ());

		// Erase all pending movements on the client, since we're about to process movement
		if (currentPlayer.getPlayerDescription ().isHuman ())
			currentPlayer.getConnection ().sendMessageToClient (new ErasePendingMovementsMessage ());

		// Continue the player's movement
		continueMovement (currentPlayer.getPlayerDescription ().getPlayerID (), mom);

		if (currentPlayer.getPlayerDescription ().isHuman ())
		{
			mom.getSessionLogger ().info ("Human turn " + mom.getGeneralPublicKnowledge ().getTurnNumber () + " - " + currentPlayer.getPlayerDescription ().getPlayerName () + "...");
			currentPlayer.getConnection ().sendMessageToClient (new EndOfContinuedMovementMessage ());
		}
		else
		{
			mom.getSessionLogger ().info ("AI turn " + mom.getGeneralPublicKnowledge ().getTurnNumber () + " - " + currentPlayer.getPlayerDescription ().getPlayerName () + "...");
			getMomAI ().aiPlayerTurn (currentPlayer, mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getSessionDescription (), mom.getServerDB ());
			
			// In the Delphi version, this is triggered back in the VCL thread via the OnTerminate method (which isn't obvious)
			nextTurnButton (mom, currentPlayer);
		}

		log.trace ("Exiting switchToNextPlayer = Player ID " +
			mom.getGeneralPublicKnowledge ().getCurrentPlayerID () + " turn " + mom.getGeneralPublicKnowledge ().getTurnNumber ());
	}

	/**
	 * Kicks off a new turn in an everybody-allocate-movement-then-move-simultaneously game
	 *
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws RecordNotFoundException If an expected element cannot be found
	 * @throws PlayerNotFoundException If the player who owns a unit, or the previous or next player cannot be found
	 * @throws MomException If the player's unit doesn't have the experience skill
	 */
	public final void kickOffSimultaneousTurn (final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering kickOffSimultaneousTurn: " + mom.getGeneralPublicKnowledge ().getTurnNumber ());

		// Bump up the turn number
		mom.getGeneralPublicKnowledge ().setTurnNumber (mom.getGeneralPublicKnowledge ().getTurnNumber () + 1);

		// Process everybody's start phases together
		startPhase (mom, 0);

		// Tell all human players to take their go, and send each of them their New Turn Messages in the process
		sendNewTurnMessages (mom.getGeneralPublicKnowledge (), mom.getPlayers (), mom.getSessionDescription ().getTurnSystem ());

		// Every AI player has their turn
		for (final PlayerServerDetails aiPlayer : mom.getPlayers ())
			if (!aiPlayer.getPlayerDescription ().isHuman ())
			{
				mom.getSessionLogger ().info ("AI turn " + mom.getGeneralPublicKnowledge ().getTurnNumber () + " - " + aiPlayer.getPlayerDescription ().getPlayerName () + "...");
				getMomAI ().aiPlayerTurn (aiPlayer, mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getSessionDescription (), mom.getServerDB ());
			
				// In the Delphi version, this is triggered back in the VCL thread via the OnTerminate method (which isn't obvious)
				nextTurnButton (mom, aiPlayer);
			}

		log.trace ("Exiting kickOffSimultaneousTurn = " + mom.getGeneralPublicKnowledge ().getTurnNumber ());
	}

	/**
	 * Sends all new turn messages queued up on the server to each player, then clears them from the server
	 * This is also used to trigger new turns or when it is a different player's turn
	 * 
	 * @param gpk Public knowledge structure; can pass this as null if messageType = null
	 * @param players List of players in this session
	 * @param messageType Type of message to send according to the turn system being used; null = just send messages, don't start a new turn
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws MomException If the value of messageType isn't recognized
	 */
	@Override
	public final void sendNewTurnMessages (final MomGeneralPublicKnowledge gpk, final List<PlayerServerDetails> players,
		final TurnSystem messageType) throws JAXBException, XMLStreamException, MomException
	{
		log.trace ("Entering sendNewTurnMessages: " + messageType);
		
		for (final PlayerServerDetails player : players)
			if (player.getPlayerDescription ().isHuman ())
			{
				final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();
				
				if (messageType == null)
				{
					// Sending additional messages in-turn to the player, e.g. someone cast an overland spell
					if (trans.getNewTurnMessage ().size () > 0)
					{
						final AddNewTurnMessagesMessage msg = new AddNewTurnMessagesMessage ();
						msg.setExpireMessages (false);
						msg.getMessage ().addAll (trans.getNewTurnMessage ());
						player.getConnection ().sendMessageToClient (msg);
						trans.getNewTurnMessage ().clear ();
					}
				}
				else if (messageType == TurnSystem.SIMULTANEOUS)
				{	
					// Everyone starts turn together, so everyone gets and expires messages together
					final StartSimultaneousTurnMessage msg = new StartSimultaneousTurnMessage ();
					msg.setTurnNumber (gpk.getTurnNumber ());
					msg.setExpireMessages (true);
					msg.getMessage ().addAll (trans.getNewTurnMessage ());
					player.getConnection ().sendMessageToClient (msg);
					trans.getNewTurnMessage ().clear ();
				}
				else if (messageType == TurnSystem.ONE_PLAYER_AT_A_TIME)
				{
					// Everyone needs to know whose turn it is, but only the new 'current' player gets and expires messages
					final SetCurrentPlayerMessage msg = new SetCurrentPlayerMessage ();
					msg.setTurnNumber (gpk.getTurnNumber ());
					msg.setCurrentPlayerID (gpk.getCurrentPlayerID ());
					msg.setExpireMessages (gpk.getCurrentPlayerID () == player.getPlayerDescription ().getPlayerID ());
					
					if (msg.isExpireMessages ())
						msg.getMessage ().addAll (trans.getNewTurnMessage ());
					
					player.getConnection ().sendMessageToClient (msg);
					
					if (msg.isExpireMessages ())
						trans.getNewTurnMessage ().clear ();
				}
				else
					throw new MomException ("sendNewTurnMessages doesn't know how handle messageType of " + messageType);
			}
		
		log.trace ("Exiting sendNewTurnMessages");
	}
	
	/**
	 * Processes the 'end phase' (for want of something better to call it), which happens at the end of each player's turn
	 *
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param onlyOnePlayerID If zero, will process start phase for all players; if specified will process start phase only for the specified player
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void endPhase (final MomSessionVariables mom, final int onlyOnePlayerID)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering endPhase: Player ID " + onlyOnePlayerID);

		if (onlyOnePlayerID == 0)
			mom.getSessionLogger ().info ("End phase for everyone turn " + mom.getGeneralPublicKnowledge ().getTurnNumber () + "...");
		else
			mom.getSessionLogger ().info ("End phase for turn " + mom.getGeneralPublicKnowledge ().getTurnNumber () + " - " + getMultiplayerSessionServerUtils ().findPlayerWithID
				(mom.getPlayers (), onlyOnePlayerID, "endPhase").getPlayerDescription ().getPlayerName () + "...");

		// Put mana into casting spells
		for (final PlayerServerDetails player : mom.getPlayers ())
			if ((onlyOnePlayerID == 0) || (player.getPlayerDescription ().getPlayerID () == onlyOnePlayerID))
				getSpellQueueing ().progressOverlandCasting (mom.getGeneralServerKnowledge (), player, mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());

		// Kick off the next turn
		mom.getSessionLogger ().info ("Kicking off next turn...");
		switch (mom.getSessionDescription ().getTurnSystem ())
		{
			case ONE_PLAYER_AT_A_TIME:
				switchToNextPlayer (mom);
				break;
				
			case SIMULTANEOUS:
				kickOffSimultaneousTurn (mom);
				break;

			default:
				throw new MomException ("endPhase encountered an unknown turn system " + mom.getSessionDescription ().getTurnSystem ());
		}

		log.trace ("Exiting endPhase");
	}

	/**
	 * Human player has clicked the next turn button, or AI player's turn has finished
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param player Player who hit the next turn button
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void nextTurnButton (final MomSessionVariables mom, final PlayerServerDetails player)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering nextTurnButton: Player ID " + player.getPlayerDescription ().getPlayerID ());

		switch (mom.getSessionDescription ().getTurnSystem ())
		{
			case ONE_PLAYER_AT_A_TIME:
				// In a one-player-at-a-time game, we need to verify that the correct player clicked Next Turn, and if all OK run their end phase and then switch to the next player
				if (mom.getGeneralPublicKnowledge ().getCurrentPlayerID ().equals (player.getPlayerDescription ().getPlayerID ()))
					endPhase (mom, player.getPlayerDescription ().getPlayerID ());
				else
				{
					log.warn (player.getPlayerDescription ().getPlayerName () + " clicked Next Turn in one-at-a-time game when wasn't their turn");

					final TextPopupMessage reply = new TextPopupMessage ();
					reply.setText ("You clicked the Next Turn button when it wasn't your turn");
					player.getConnection ().sendMessageToClient (reply);
				}
				break;
				
			case SIMULTANEOUS:
				// In a simultaneous game, we need to check if all players have finished allocating
				// their movement and if so, process combats, research, city growth and so on
				
				// First tell everyone that this player has finished
				final OnePlayerSimultaneousTurnDoneMessage turnDoneMsg = new OnePlayerSimultaneousTurnDoneMessage ();
				turnDoneMsg.setPlayerID (player.getPlayerDescription ().getPlayerID ());
				getMultiplayerSessionServerUtils ().sendMessageToAllClients (mom.getPlayers (), turnDoneMsg);

				// Record on server that this player has finished
				final MomTransientPlayerPublicKnowledge tpk = (MomTransientPlayerPublicKnowledge) player.getTransientPlayerPublicKnowledge ();
				tpk.setMovementAllocatedForTurnNumber (mom.getGeneralPublicKnowledge ().getTurnNumber ());

				// Has everyone finished now?
				if (getPlayerServerUtils ().allPlayersFinishedAllocatingMovement (mom.getPlayers (), mom.getGeneralPublicKnowledge ().getTurnNumber ()))
				{
					// Erase all pending movements on the clients, since we're about to process movement
					getMultiplayerSessionServerUtils ().sendMessageToAllClients (mom.getPlayers (), new ErasePendingMovementsMessage ());
					
					// Process all movements and combats
					processSimultaneousTurnsMovement (mom);
				}				
				break;
				

			default:
				throw new MomException ("nextTurnButton encountered an unknown turn system " + mom.getSessionDescription ().getTurnSystem ());
		}

		log.trace ("Exiting nextTurnButton");
	}

	/**
	 * Checks for any units this player (or all players if 0) has which have pending movement orders (i.e. clicked to move
	 * somewhere which will take longer than a turn to get there), and continues the units further along their movement
	 * 
	 * Really this is part of the player's StartPhase, except that the resulting messages have to be sent after the
	 * mmSetCurrentPlayer or mmSetSimultaneousTurn messages since both these messages cause the client to reset all units for the player back to full movement
	 * 
	 * So order must be StartPhase - Set Player - ContinueMovement
	 * 
	 * @param onlyOnePlayerID If zero, will continue movement for all players; if specified will continue movement only for the specified player
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	final void continueMovement (final int onlyOnePlayerID, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException, MomException, PlayerNotFoundException
	{
		log.trace ("Entering continueMovement: Player ID " + onlyOnePlayerID);

		if (onlyOnePlayerID == 0)
			mom.getSessionLogger ().info ("Continuing pending movements for everyone...");
		else
			mom.getSessionLogger ().info ("Continuing pending movements for " + getMultiplayerSessionServerUtils ().findPlayerWithID
				(mom.getPlayers (), onlyOnePlayerID, "continueMovement").getPlayerDescription ().getPlayerName () + "...");
		
		for (final PlayerServerDetails player : mom.getPlayers ())
			if ((onlyOnePlayerID == 0) || (player.getPlayerDescription ().getPlayerID () == onlyOnePlayerID))
			{
				// If a pending movement doesn't finish this turn then the moveUnitStack routine will recreate the pending movement object and add it to the end of the list
				// So we need to make sure we don't keep going through the list and end up processing pending moves that have only just been added
				// Simplest way to do it is just to copy the list and run down that instead
				final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();
				final List<PendingMovement> moves = new ArrayList<PendingMovement> ();
				moves.addAll (trans.getPendingMovement ());
				trans.getPendingMovement ().clear ();
				
				for (final PendingMovement thisMove : moves)
				{
					// Find each of the units
					final List<MemoryUnit> unitStack = new ArrayList<MemoryUnit> ();
					for (final Integer unitURN : thisMove.getUnitURN ())
						unitStack.add (getUnitUtils ().findUnitURN (unitURN, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), "continueMovement"));
					
					getFogOfWarMidTurnChanges ().moveUnitStack (unitStack, player,
						(MapCoordinates3DEx) thisMove.getMoveFrom (), (MapCoordinates3DEx) thisMove.getMoveTo (), false, mom);
				}
			}

		log.trace ("Exiting continueMovement");				
	}

	/**
	 * Processes PendingMovements at the end of a simultaneous turns game.
	 * 
	 * This routine will end when either we find a combat that needs to be played (in which case it ends with a call to startCombat)
	 * or when the only PendingMovements remaining are unit stacks that have no movement left (in which case it ends with a call to endPhase).
	 * 
	 * It must be able to carry on where it left off, so e.g. it is called the first time, and ends when it finds a combat that a human player needs
	 * to do, then the end of combatEnded will call it again, and it must be able to continue processing remaining movements.
	 * 
	 * Combats just between AI players still cause this routine to exit out, because combatEnded being triggered results in this routine
	 * being called again, so it will start another execution and continue where it left off, and the original invocation will exit out. 
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public final void processSimultaneousTurnsMovement (final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException, MomException, PlayerNotFoundException
	{
		log.trace ("Entering processSimultaneousTurnsMovement");

		// Process non combat moves
		while (findAndProcessOneCellPendingMovement (mom));
		
		// >>>>> Everything below here is "If no combat was initiated"... when that part is written		
		
		// Any pending movements remaining at this point must be unit stacks that ran out of movement before they
		// reached their destination - so we resend these pending moves to the client so the arrows display correctly
		// and it won't ask the player for where to move those units.
		for (final PlayerServerDetails player : mom.getPlayers ())
			if (player.getPlayerDescription ().isHuman ())
			{
				final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();
				final Iterator<PendingMovement> iter = trans.getPendingMovement ().iterator ();
				while (iter.hasNext ())
				{
					final PendingMovement thisMove = iter.next ();

					// Find each of the units
					final List<MemoryUnit> unitStack = new ArrayList<MemoryUnit> ();
					for (final Integer unitURN : thisMove.getUnitURN ())
						unitStack.add (getUnitUtils ().findUnitURN (unitURN, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), "processSimultaneousTurnsMovement"));
					
					// We're at a different location now, and know more about the map than when the path was calculated, so need to recalculate it
					final List<Integer> path = getFogOfWarMidTurnChanges ().determineMovementPath (unitStack, player,
						(MapCoordinates3DEx) thisMove.getMoveFrom (), (MapCoordinates3DEx) thisMove.getMoveTo (), mom);
					
					// In the process we might now find that the location has become unreachable, because of what we've learned about the map
					if (path == null)
						iter.remove ();
					else
					{
						// Use the updated path
						thisMove.getPath ().clear ();
						thisMove.getPath ().addAll (path);
						
						// Send it to the client
						final PendingMovementMessage msg = new PendingMovementMessage ();
						msg.setPendingMovement (thisMove);
						player.getConnection ().sendMessageToClient (msg);
					}
				}
			}

		// Special orders - e.g. settlers building cities.
		// This can generate messages about spirits capturing nodes.
		// We want to send these now, even though we may be just about to run the EndPhase to keep consistency between whether
		// they are considered part of the previous or new turn depending on whether there's any combats or not
		// (Since if there are combats, there's no way to get messages generated
		// here sent with the message block generated in the EndPhase)
		getSimultaneousTurnsProcessing ().processSpecialOrders (mom);
		sendNewTurnMessages (mom.getGeneralPublicKnowledge (), mom.getPlayers (), null);

		// End this turn and start the next one
		endPhase (mom, 0);
		
		log.trace ("Exiting processSimultaneousTurnsMovement");
	}
	

	/**
	 * Searches all pending movements across all players, making a list of all those where the first cell being
	 * moved to is free and clear, or occupied by ourselves (even if subsequent moves result in a combat).
	 * Then chooses one at random and processes the move.
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether we found a move to do
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	final boolean findAndProcessOneCellPendingMovement (final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException, MomException, PlayerNotFoundException
	{
		log.trace ("Entering findAndProcessOneCellPendingMovement");

		// Go through all pending movements for all players
		final List<OneCellPendingMovement> moves = new ArrayList<OneCellPendingMovement> ();
		for (final PlayerServerDetails player : mom.getPlayers ())
		{
			final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();
			final Iterator<PendingMovement> iter = trans.getPendingMovement ().iterator ();
			while (iter.hasNext ())
			{
				final PendingMovement thisMove = iter.next ();
				
				// Loop finding each unit, and in process ensure they all have movement remaining
				int doubleMovementRemaining = Integer.MAX_VALUE;
				int doubleMovementTotal = Integer.MAX_VALUE;
				final List<MemoryUnit> unitStack = new ArrayList<MemoryUnit> ();
				for (final Integer unitURN : thisMove.getUnitURN ())
				{
					final MemoryUnit thisUnit = getUnitUtils ().findUnitURN (unitURN, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), "findAndProcessOneCellPendingMovement");
					
					unitStack.add (thisUnit);
					if (thisUnit.getDoubleOverlandMovesLeft () < doubleMovementRemaining)
						doubleMovementRemaining = thisUnit.getDoubleOverlandMovesLeft ();
					
					final int unitDoubleMovementTotal = mom.getServerDB ().findUnit (thisUnit.getUnitID (), "findAndProcessOneCellPendingMovement").getDoubleMovement ();
					if (unitDoubleMovementTotal < doubleMovementTotal)
						doubleMovementTotal = unitDoubleMovementTotal;
				}
				
				// Any pending movements where the unit stack is out of movement, just leave them in the list until next turn
				if (doubleMovementRemaining > 0)
				{
					// Does this unit stack have a valid single cell non-combat move?
					final OneCellPendingMovement oneCell = getFogOfWarMidTurnChanges ().determineOneCellPendingMovement
						(unitStack, player, thisMove, doubleMovementRemaining, mom);
					
					// Is the destination unreachable?  If so just remove the pending movement altogether
					if (oneCell == null)
						iter.remove ();
					
					// Does it initiate a combat?  If so then leave the pending movement (don't remove it) and we'll deal with it later
					else if (!oneCell.isCombatInitiated ())
						for (int n = 0; n < doubleMovementTotal; n++)
							moves.add (oneCell);
				}
			}
		}
		
		final boolean found = (moves.size () > 0);
		if (found)
		{
			// Pick a random movement to do
			final OneCellPendingMovement oneCell = moves.get (getRandomUtils ().nextInt (moves.size ()));
			final List<MemoryUnit> unitStack = new ArrayList<MemoryUnit> ();
			for (final Integer unitURN : oneCell.getPendingMovement ().getUnitURN ())
				unitStack.add (getUnitUtils ().findUnitURN (unitURN, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), "findAndProcessOneCellPendingMovement"));
			
			// Execute the move
			getFogOfWarMidTurnChanges ().moveUnitStack (unitStack, oneCell.getUnitStackOwner (),
				(MapCoordinates3DEx) oneCell.getPendingMovement ().getMoveFrom (), oneCell.getOneStep (), false, mom);
			
			// If they got to their destination, remove the pending move completely
			if (oneCell.getOneStep ().equals (oneCell.getPendingMovement ().getMoveTo ()))
			{
				final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) oneCell.getUnitStackOwner ().getTransientPlayerPrivateKnowledge ();
				trans.getPendingMovement ().remove (oneCell.getPendingMovement ());
			}
			
			// Otherwise update the pending movement with the new start position
			else
				oneCell.getPendingMovement ().setMoveFrom (oneCell.getOneStep ());
		}
		
		log.trace ("Exiting findAndProcessOneCellPendingMovement = " + found);
		return found;
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
	 * @return MemoryGridCell utils
	 */
	public final MemoryGridCellUtils getMemoryGridCellUtils ()
	{
		return memoryGridCellUtils;
	}

	/**
	 * @param utils MemoryGridCell utils
	 */
	public final void setMemoryGridCellUtils (final MemoryGridCellUtils utils)
	{
		memoryGridCellUtils = utils;
	}
	
	/**
	 * @return Skill calculations
	 */
	public final SkillCalculations getSkillCalculations ()
	{
		return skillCalculations;
	}

	/**
	 * @param calc Skill calculations
	 */
	public final void setSkillCalculations (final SkillCalculations calc)
	{
		skillCalculations = calc;
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
	 * @return Spell queueing methods
	 */
	public final SpellQueueing getSpellQueueing ()
	{
		return spellQueueing;
	}

	/**
	 * @param obj Spell queueing methods
	 */
	public final void setSpellQueueing (final SpellQueueing obj)
	{
		spellQueueing = obj;
	}

	/**
	 * @return Fog of war update methods
	 */
	public final FogOfWarProcessing getFogOfWarProcessing ()
	{
		return fogOfWarProcessing;
	}

	/**
	 * @param obj Fog of war update methods
	 */
	public final void setFogOfWarProcessing (final FogOfWarProcessing obj)
	{
		fogOfWarProcessing = obj;
	}

	/**
	 * @return Resource calculations
	 */
	public final ServerResourceCalculations getServerResourceCalculations ()
	{
		return serverResourceCalculations;
	}

	/**
	 * @param calc Resource calculations
	 */
	public final void setServerResourceCalculations (final ServerResourceCalculations calc)
	{
		serverResourceCalculations = calc;
	}

	/**
	 * @return Server-only pick utils
	 */
	public final PlayerPickServerUtils getPlayerPickServerUtils ()
	{
		return playerPickServerUtils;
	}

	/**
	 * @param utils Server-only pick utils
	 */
	public final void setPlayerPickServerUtils (final PlayerPickServerUtils utils)
	{
		playerPickServerUtils = utils;
	}

	/**
	 * @return Server-only spell calculations
	 */
	public final ServerSpellCalculations getServerSpellCalculations ()
	{
		return serverSpellCalculations;
	}

	/**
	 * @param calc Server-only spell calculations
	 */
	public final void setServerSpellCalculations (final ServerSpellCalculations calc)
	{
		serverSpellCalculations = calc;
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
	 * @return AI player turns
	 */
	public final MomAI getMomAI ()
	{
		return momAI;
	}

	/**
	 * @param ai AI player turns
	 */
	public final void setMomAI (final MomAI ai)
	{
		momAI = ai;
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
	 * @return Player utils
	 */
	public final PlayerServerUtils getPlayerServerUtils ()
	{
		return playerServerUtils;
	}
	
	/**
	 * @param utils Player utils
	 */
	public final void setPlayerServerUtils (final PlayerServerUtils utils)
	{
		playerServerUtils = utils;
	}

	/**
	 * @return Simultaneous turns processing
	 */	
	public final SimultaneousTurnsProcessing getSimultaneousTurnsProcessing ()
	{
		return simultaneousTurnsProcessing;
	}

	/**
	 * @param proc Simultaneous turns processing
	 */
	public final void setSimultaneousTurnsProcessing (final SimultaneousTurnsProcessing proc)
	{
		simultaneousTurnsProcessing = proc;
	}
	
	/**
	 * @return Simultaneous turns combat scheduler
	 */
	public final CombatScheduler getCombatScheduler ()
	{
		return combatScheduler;
	}

	/**
	 * @param scheduler Simultaneous turns combat scheduler
	 */
	public final void setCombatScheduler (final CombatScheduler scheduler)
	{
		combatScheduler = scheduler;
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