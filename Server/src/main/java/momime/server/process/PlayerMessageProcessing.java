package momime.server.process;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.calculations.MomSkillCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.WizardPick;
import momime.common.messages.MemoryGridCellUtils;
import momime.common.messages.PlayerKnowledgeUtils;
import momime.common.messages.ResourceValueUtils;
import momime.common.messages.UnitUtils;
import momime.common.messages.servertoclient.v0_9_4.ChooseInitialSpellsNowMessage;
import momime.common.messages.servertoclient.v0_9_4.ChooseYourRaceNowMessage;
import momime.common.messages.servertoclient.v0_9_4.ChosenCustomPhotoMessage;
import momime.common.messages.servertoclient.v0_9_4.ChosenStandardPhotoMessage;
import momime.common.messages.servertoclient.v0_9_4.ChosenWizardMessage;
import momime.common.messages.servertoclient.v0_9_4.EndOfContinuedMovementMessage;
import momime.common.messages.servertoclient.v0_9_4.ErasePendingMovementsMessage;
import momime.common.messages.servertoclient.v0_9_4.FullSpellListMessage;
import momime.common.messages.servertoclient.v0_9_4.ReplacePicksMessage;
import momime.common.messages.servertoclient.v0_9_4.SetCurrentPlayerMessage;
import momime.common.messages.servertoclient.v0_9_4.StartGameMessage;
import momime.common.messages.servertoclient.v0_9_4.StartGameProgressMessage;
import momime.common.messages.servertoclient.v0_9_4.StartGameProgressStageID;
import momime.common.messages.servertoclient.v0_9_4.TextPopupMessage;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.PlayerPick;
import momime.common.messages.v0_9_4.SpellResearchStatus;
import momime.common.messages.v0_9_4.SpellResearchStatusID;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.server.MomSessionThread;
import momime.server.ai.CityAI;
import momime.server.ai.MomAI;
import momime.server.calculations.MomServerResourceCalculations;
import momime.server.calculations.MomServerSpellCalculations;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_4.PickFreeSpell;
import momime.server.database.v0_9_4.Unit;
import momime.server.database.v0_9_4.Wizard;
import momime.server.database.v0_9_4.WizardPickCount;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.FogOfWarProcessing;
import momime.server.mapgenerator.OverlandMapGenerator;
import momime.server.messages.v0_9_4.MomGeneralServerKnowledge;
import momime.server.utils.PlayerPickServerUtils;
import momime.server.utils.RandomUtils;
import momime.server.utils.UnitServerUtils;

import com.ndg.multiplayer.server.MultiplayerServerUtils;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Methods for any significant message processing to do with game startup and the turn system that isn't done in the message implementations
 */
public final class PlayerMessageProcessing
{
	/**
	 * Message we send to the server when we choose which wizard we want to be; AI players also call this to do their wizard, picks and spells setup
	 * which is why this isn't all just in ChooseWizardMessageImpl
	 *
	 * @param wizardIdFromMessage wizard ID the player wants to choose
	 * @param player Player who sent the message
	 * @param players List of players in the session
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If various elements cannot be found in the DB
	 * @throws MomException If an AI player has enough books that they should get some free spells, but we can't find any suitable free spells to give them
	 */
	public static final void chooseWizard (final String wizardIdFromMessage, final PlayerServerDetails player,
		final List<PlayerServerDetails> players, final MomSessionDescription sd, final ServerDatabaseEx db, final Logger debugLogger)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException
	{
		debugLogger.entering (PlayerMessageProcessing.class.getName (), "chooseWizard", wizardIdFromMessage);

		// Blank or null are used for custom wizard
		final String wizardID = ((wizardIdFromMessage != null) && (wizardIdFromMessage.equals (""))) ? null : wizardIdFromMessage;

		// Check if custom
		boolean valid;
		Wizard wizard = null;
		if (wizardID == null)
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
			debugLogger.warning (player.getPlayerDescription ().getPlayerName () + " tried to choose invalid wizard ID \"" + wizardID + "\"");

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText ("Wizard choice invalid, please try again");
			player.getConnection ().sendMessageToClient (reply);
		}

		// Wizard might be valid, but check if option prohibits player from choosing same wizard as another player has already chosen
		else if ((sd.getDifficultyLevel ().isEachWizardOnlyOnce ()) && (PlayerPickServerUtils.findPlayerUsingWizard (players, wizardID, debugLogger) != null))
		{
			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText ("Game is set to only allow each Wizard to be chosen once, and another player already chose this Wizard, please try again");
			player.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			// Successful - Remember choice on the server
			// Convert custom null to blank at this point
			final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();

			ppk.setWizardID ((wizardID == null) ? "" : wizardID);

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

					WizardPickCount pickCount = null;
					final Iterator<WizardPickCount> iter = wizard.getWizardPickCount ().iterator ();
					while ((pickCount == null) && (iter.hasNext ()))
					{
						final WizardPickCount thisPickCount = iter.next ();
						if (thisPickCount.getPickCount () == desiredPickCount)
							pickCount = thisPickCount;
					}

					if (pickCount == null)
						throw new RecordNotFoundException (WizardPickCount.class.getName (), wizardID + "-" + desiredPickCount, "chooseWizard");

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
					if (debugLogger.isLoggable (Level.FINEST))
					{
						String picksDebugDescription = null;

						for (final PlayerPick pick : ppk.getPick ())
						{
							if (picksDebugDescription == null)
								picksDebugDescription = pick.getQuantity () + "x" + pick.getPickID ();
							else
								picksDebugDescription = picksDebugDescription + ", " + pick.getQuantity () + "x" + pick.getPickID ();
						}

						debugLogger.finest (PlayerMessageProcessing.class.getName () + ".chooseWizard: Read picks for player '" + player.getPlayerDescription ().getPlayerName () + "' who has chosen pre-defined wizard \"" + wizardID + "\":  " + picksDebugDescription);
					}

					// Commenting this out because don't think client needs this info yet
					// Send picks
					// if (player.getPlayerDescription ().isHuman ())
					// sendPicksToPlayer (players, player, false, false, debugLogger);

					// Tell client to either pick free starting spells or pick a race, depending on whether the pre-defined wizard chosen has >1 of any kind of book
					// Its fine to do this before we confirm to the client that their wizard choice was OK by the mmChosenWizard message sent below
					if (player.getPlayerDescription ().isHuman ())
					{
						// This will tell the client to either pick free spells for the first magic realm that they have earned free spells in, or pick their race, depending on what picks they've chosen
						debugLogger.finest (PlayerMessageProcessing.class.getName () + ".chooseWizard: About to search for first realm (if any) where human player " + player.getPlayerDescription ().getPlayerName () + " gets free spells");
						final ChooseInitialSpellsNowMessage chooseSpellsMsg = PlayerPickServerUtils.findRealmIDWhereWeNeedToChooseFreeSpells (player, db, debugLogger);
						if (chooseSpellsMsg != null)
							player.getConnection ().sendMessageToClient (chooseSpellsMsg);
						else
							player.getConnection ().sendMessageToClient (new ChooseYourRaceNowMessage ());
					}
					else
					{
						// For AI players, we call this repeatedly until all free spells have been chosen
						debugLogger.finest (PlayerMessageProcessing.class.getName () + ".chooseWizard: About to choose all free spells for AI player " + player.getPlayerDescription ().getPlayerName ());

						while (PlayerPickServerUtils.findRealmIDWhereWeNeedToChooseFreeSpells (player, db, debugLogger) != null);
					}
				}
			}

			// Tell everyone about the wizard this player has chosen
			broadcastWizardChoice (players, player, debugLogger);
		}

		debugLogger.exiting (PlayerMessageProcessing.class.getName (), "chooseWizard");
	}

	/**
	 * Sends the wizard choice of the specified player to all human players in the specified session players list
	 * @param players List of players in the session
	 * @param player Player whose wizard choice we are sending
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	private static void broadcastWizardChoice (final List<PlayerServerDetails> players, final PlayerServerDetails player, final Logger debugLogger)
		throws JAXBException, XMLStreamException
	{
		debugLogger.entering (PlayerMessageProcessing.class.getName (), "broadcastWizardChoice", player.getPlayerDescription ().getPlayerID ());

		// Convert empty string (custom wizard) to a null
		final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		final String wizardIdToSend;
		if (ppk.getWizardID ().equals (""))
			wizardIdToSend = null;
		else
			wizardIdToSend = ppk.getWizardID ();

		final ChosenWizardMessage msg = new ChosenWizardMessage ();
		msg.setPlayerID (player.getPlayerDescription ().getPlayerID ());
		msg.setWizardID (wizardIdToSend);

		MultiplayerServerUtils.sendMessageToAllClients (players, msg, debugLogger);

		debugLogger.exiting (PlayerMessageProcessing.class.getName (), "broadcastWizardChoice");
	}

	/**
	 * @param players List of players in the session
	 * @param stage Stage of starting up the game that we are currently at
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	private final static void sendStartGameProgressMessage (final List<PlayerServerDetails> players, final StartGameProgressStageID stage, final Logger debugLogger)
		throws JAXBException, XMLStreamException
	{
		debugLogger.entering (PlayerMessageProcessing.class.getName (), "sendStartGameProgressMessage", stage);

		final StartGameProgressMessage msg = new StartGameProgressMessage ();
		msg.setStage (stage);

		MultiplayerServerUtils.sendMessageToAllClients (players, msg, debugLogger);

		debugLogger.exiting (PlayerMessageProcessing.class.getName (), "sendStartGameProgressMessage");
	}

	/**
	 * @param wizard Wizard this AI player is playing
	 * @return Player description created for this AI player
	 */
	private final static PlayerDescription createAiPlayerDescription (final Wizard wizard)
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
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws MomException If initialStatus is an inappropriate value
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws PlayerNotFoundException This only gets generated if addUnitOnServerAndClients tries to send into to players, but we pass null for player list, so won't happen
	 * @throws JAXBException This only gets generated if addUnitOnServerAndClients tries to send into to players, but we pass null for player list, so won't happen
	 * @throws XMLStreamException This only gets generated if addUnitOnServerAndClients tries to send into to players, but we pass null for player list, so won't happen
	 */
	private final static void createHeroes (final List<PlayerServerDetails> players, final MomGeneralServerKnowledge gsk,
		final MomSessionDescription sd, final ServerDatabaseEx db, final Logger debugLogger)
		throws MomException, RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		debugLogger.entering (PlayerMessageProcessing.class.getName (), "createHeroes");

		for (final Unit thisUnit : db.getUnit ())
			if (thisUnit.getUnitMagicRealm ().equals (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))

				// Add this hero for all players, even raiders, just not the monsters
				// We won't end up sending these to the client since we're setting status as 'not generated'
				for (final PlayerServerDetails thisPlayer : players)
				{
					final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) thisPlayer.getPersistentPlayerPublicKnowledge ();
					if (!ppk.getWizardID ().equals (CommonDatabaseConstants.WIZARD_ID_MONSTERS))
						FogOfWarMidTurnChanges.addUnitOnServerAndClients (gsk, thisUnit.getUnitID (), null, null, null, thisPlayer, UnitStatusID.NOT_GENERATED, null, sd, db, debugLogger);
				}

		debugLogger.exiting (PlayerMessageProcessing.class.getName (), "createHeroes");
	}


	/**
	 * If all players have chosen their wizards and, if necessary, custom picks, then sends message to tell everyone to start
	 * @param mom Thread running this session
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem sending any messages to the clients
	 * @throws XMLStreamException If there is a problem sending any messages to the clients
	 * @throws MomException If there is a problem in any game logic or data
	 * @throws RecordNotFoundException If various elements cannot be found in the DB
	 * @throws PlayerNotFoundException If we encounter players that we cannot find in the list
	 */
	public final static void checkIfCanStartGame (final MomSessionThread mom, final Logger debugLogger)
		throws JAXBException, XMLStreamException, MomException, RecordNotFoundException, PlayerNotFoundException
	{
		debugLogger.entering (PlayerMessageProcessing.class.getName (), "checkIfCanStartGame");

		if (PlayerPickServerUtils.allPlayersHaveChosenAllDetails (mom.getPlayers (), mom.getSessionDescription (), debugLogger))
		{
			// Add AI wizards
			debugLogger.finest ("checkIfCanStartGame: Yes, " + mom.getSessionDescription ().getAiPlayerCount () + " AI wizards to add");
			mom.getSessionLogger ().info ("All Human players joined - adding AI player(s) and starting game...");

			sendStartGameProgressMessage (mom.getPlayers (), StartGameProgressStageID.ADDING_AI_PLAYERS, debugLogger);
			if (mom.getSessionDescription ().getAiPlayerCount () > 0)
			{
				// Get list of wizard IDs for AI players to choose from
				final List<Wizard> availableWizards = PlayerPickServerUtils.listWizardsForAIPlayers (mom.getPlayers (), mom.getServerDB (), debugLogger);
				for (int aiPlayerNo = 0; aiPlayerNo < mom.getSessionDescription ().getAiPlayerCount (); aiPlayerNo++)
				{
					// Pick a random wizard for this AI player
					if (availableWizards.size () == 0)
						throw new MomException ("Not enough Wizards defined for number of AI players");

					final Wizard chosenWizard = availableWizards.get (RandomUtils.getGenerator ().nextInt (availableWizards.size ()));
					availableWizards.remove (chosenWizard);

					// Add AI player
					final PlayerServerDetails aiPlayer = mom.addComputerPlayer (createAiPlayerDescription (chosenWizard));

					// Choose wizard
					chooseWizard (chosenWizard.getWizardID (), aiPlayer, mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB (), debugLogger);

					// Choose race
					final MomTransientPlayerPrivateKnowledge priv = (MomTransientPlayerPrivateKnowledge) aiPlayer.getTransientPlayerPrivateKnowledge ();
					final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) aiPlayer.getPersistentPlayerPublicKnowledge ();

					priv.setFirstCityRaceID (PlayerPickServerUtils.chooseRandomRaceForPlane
						(PlayerPickServerUtils.startingPlaneForWizard (ppk.getPick (), mom.getServerDB (), debugLogger), mom.getServerDB (), debugLogger));
				}
			}

			// Add raiders
			final PlayerServerDetails raidersPlayer = mom.addComputerPlayer (createAiPlayerDescription
				(mom.getServerDB ().findWizard (CommonDatabaseConstants.WIZARD_ID_RAIDERS, "checkIfCanStartGame")));

			chooseWizard (CommonDatabaseConstants.WIZARD_ID_RAIDERS, raidersPlayer, mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB (), debugLogger);

			// Add monsters
			final PlayerServerDetails monstersPlayer = mom.addComputerPlayer (createAiPlayerDescription
				(mom.getServerDB ().findWizard (CommonDatabaseConstants.WIZARD_ID_MONSTERS, "checkIfCanStartGame")));

			chooseWizard (CommonDatabaseConstants.WIZARD_ID_MONSTERS, monstersPlayer, mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB (), debugLogger);

			// Broadcast player data
			debugLogger.finest ("checkIfCanStartGame: Broadcasting player picks and determining which spells not chosen for free will be researchable");
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

					MultiplayerServerUtils.sendMessageToAllClients (mom.getPlayers (), msg, debugLogger);
				}

				else if (ppk.getCustomPhoto () != null)
				{
					final ChosenCustomPhotoMessage msg = new ChosenCustomPhotoMessage ();
					msg.setPlayerID (thisPlayer.getPlayerDescription ().getPlayerID ());
					msg.setFlagColour (ppk.getCustomFlagColour ());
					msg.setNdgBmpImage (ppk.getCustomPhoto ());

					MultiplayerServerUtils.sendMessageToAllClients (mom.getPlayers (), msg, debugLogger);
				}

				// Send picks to everyone - note we don't know if they've already received them, if e.g. player
				// 1 joins and makes their picks then these get stored into public info on the server, then
				// player 2 joins and will be sent to them
				final ReplacePicksMessage picksMsg = new ReplacePicksMessage ();
				picksMsg.setPlayerID (thisPlayer.getPlayerDescription ().getPlayerID ());
				picksMsg.getPick ().addAll (ppk.getPick ());
				MultiplayerServerUtils.sendMessageToAllClients (mom.getPlayers (), picksMsg, debugLogger);

				// Grant any free spells the player gets from the picks they've chosen (i.e. Enchant Item & Create Artifact from Artificer)
				final List<String> freeSpellIDs = new ArrayList<String> ();
				for (final PlayerPick pick : ppk.getPick ())
					for (final PickFreeSpell freeSpell : mom.getServerDB ().findPick (pick.getPickID (), "checkIfCanStartGame").getPickFreeSpell ())
						freeSpellIDs.add (freeSpell.getFreeSpellID ());

				if (freeSpellIDs.size () > 0)
					for (final SpellResearchStatus thisSpell : priv.getSpellResearchStatus ())
						if (freeSpellIDs.contains (thisSpell.getSpellID ()))
							thisSpell.setStatus (SpellResearchStatusID.AVAILABLE);

				// For all the spells that we did NOT get for free at the start of the game, decides whether or not they are in our spell book to be available to be researched
				MomServerSpellCalculations.randomizeResearchableSpells (priv.getSpellResearchStatus (), ppk.getPick (), mom.getServerDB (), debugLogger);

				// Give player 8 spells to pick from, out of all those we'll eventually be able to research
				MomServerSpellCalculations.randomizeSpellsResearchableNow (priv.getSpellResearchStatus (), mom.getServerDB (), debugLogger);

				// Send players' spells to them (and them only)
				if (thisPlayer.getPlayerDescription ().isHuman ())
				{
					final FullSpellListMessage spellsMsg = new FullSpellListMessage ();
					spellsMsg.getSpellResearchStatus ().addAll (priv.getSpellResearchStatus ());
					thisPlayer.getConnection ().sendMessageToClient (spellsMsg);
				}
			}

			// Add monsters in nodes/lairs/towers - can only do this after we've added the players
			sendStartGameProgressMessage (mom.getPlayers (), StartGameProgressStageID.ADDING_MONSTERS, debugLogger);
			mom.getSessionLogger ().info ("Filling nodes, lairs & towers with monsters...");
			OverlandMapGenerator.fillNodesLairsAndTowersWithMonsters (mom.getSessionDescription (), mom.getGeneralServerKnowledge (), mom.getServerDB (), monstersPlayer, debugLogger);

			// Sort out heroes
			sendStartGameProgressMessage (mom.getPlayers (), StartGameProgressStageID.ADDING_HEROES, debugLogger);
			mom.getSessionLogger ().info ("Loading list of heroes for each player...");
			createHeroes (mom.getPlayers (), mom.getGeneralServerKnowledge (), mom.getSessionDescription (), mom.getServerDB (), debugLogger);

			if (mom.getSessionDescription ().getUnitSetting ().isRollHeroSkillsAtStartOfGame ())
			{
				mom.getSessionLogger ().info ("Randomzing hero skills for each player...");
				for (final MemoryUnit thisUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
					if (mom.getServerDB ().findUnit (thisUnit.getUnitID (), "checkIfCanStartGame").getUnitMagicRealm ().equals (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
						UnitServerUtils.generateHeroNameAndRandomSkills (thisUnit, mom.getServerDB (), debugLogger);
			}

			// Create cities
			sendStartGameProgressMessage (mom.getPlayers (), StartGameProgressStageID.ADDING_CITIES, debugLogger);
			mom.getSessionLogger ().info ("Creating starting cities...");
			CityProcessing.createStartingCities (mom.getPlayers (), mom.getGeneralServerKnowledge (), mom.getSessionDescription (), mom.getServerDB (), debugLogger);

			// Now we've created starting cities, we can figure out the initial fog of war area that each player can see
			sendStartGameProgressMessage (mom.getPlayers (), StartGameProgressStageID.GENERATING_INITIAL_FOG_OF_WAR, debugLogger);
			mom.getSessionLogger ().info ("Generating and sending initial fog of war...");
			for (final PlayerServerDetails thisPlayer : mom.getPlayers ())
				FogOfWarProcessing.updateAndSendFogOfWar (mom.getGeneralServerKnowledge ().getTrueMap (), thisPlayer,
					mom.getPlayers (), true, "checkIfCanStartGame", mom.getSessionDescription (), mom.getServerDB (), debugLogger);

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
					ResourceValueUtils.addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT,
						MomSkillCalculations.getSkillPointsRequiredForCastingSkill (PlayerPickServerUtils.getTotalInitialSkill
							(ppk.getPick (), mom.getServerDB (), debugLogger), debugLogger), debugLogger);

					// Give each wizard their starting gold
					final int startingGold;
					if (thisPlayer.getPlayerDescription ().isHuman ())
						startingGold = mom.getSessionDescription ().getDifficultyLevel ().getHumanStartingGold ();
					else
						startingGold = mom.getSessionDescription ().getDifficultyLevel ().getAiStartingGold ();

					ResourceValueUtils.addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, startingGold, debugLogger);
				}

				// Default each player's farmers to just enough to feed their initial units
				CityAI.setOptionalFarmersInAllCities (mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), thisPlayer,
					mom.getServerDB (), mom.getSessionDescription (), debugLogger);
			}

			// Calculate and send initial production values - This is especially important in one-at-a-time games with more
			// than one human player, since e.g. player 2 won't otherwise be sent their power base figure until player 1 hits 'next turn'
			mom.getSessionLogger ().info ("Calculating initial production values...");
			MomServerResourceCalculations.recalculateGlobalProductionValues (0, false, mom.getPlayers (),
				mom.getGeneralServerKnowledge (), mom.getSessionDescription (), mom.getServerDB (), debugLogger);

			// Kick off the game - this shows the map screen for the first time
			mom.getSessionLogger ().info ("Starting game...");
			MultiplayerServerUtils.sendMessageToAllClients (mom.getPlayers (), new StartGameMessage (), debugLogger);

			// Kick off the first turn
			mom.getSessionLogger ().info ("Kicking off first turn...");
			switch (mom.getSessionDescription ().getTurnSystem ())
			{
				case ONE_PLAYER_AT_A_TIME:
					switchToNextPlayer (mom, debugLogger);
					break;

				default:
					throw new MomException ("checkIfCanStartGame encountered an unknown turn system " + mom.getSessionDescription ().getTurnSystem ());
			}
		}

		debugLogger.exiting (PlayerMessageProcessing.class.getName (), "checkIfCanStartGame");
	}

	/**
	 * Processes the 'start phase' (for want of something better to call it), which happens just at the start of each player's turn
	 *
	 * @param mom Thread running this session
	 * @param onlyOnePlayerID If zero, will process start phase for all players; if specified will process start phase only for the specified player
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	private final static void startPhase (final MomSessionThread mom, final int onlyOnePlayerID, final Logger debugLogger)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		debugLogger.entering (PlayerMessageProcessing.class.getName (), "startPhase", onlyOnePlayerID);

		if (onlyOnePlayerID == 0)
			mom.getSessionLogger ().info ("Start phase for everyone turn " + mom.getGeneralPublicKnowledge ().getTurnNumber () + "...");
		else
			mom.getSessionLogger ().info ("Start phase for turn " + mom.getGeneralPublicKnowledge ().getTurnNumber () + " - " + MultiplayerSessionServerUtils.findPlayerWithID
				(mom.getPlayers (), onlyOnePlayerID, "startPhase").getPlayerDescription ().getPlayerName () + "...");

		// Give units their full movement back again
		UnitUtils.resetUnitOverlandMovement (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), onlyOnePlayerID, mom.getServerDB (), debugLogger);

		// Heal hurt units 1pt and gain 1exp
		FogOfWarMidTurnChanges.healUnitsAndGainExperience (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), onlyOnePlayerID,
			mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription (), debugLogger);

		// Allow another building to be sold
		MemoryGridCellUtils.blankBuildingsSoldThisTurn (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), debugLogger);

		// Global production - only need to do a simple recalc on turn 1, with no accumulation and no city growth
		if (mom.getGeneralPublicKnowledge ().getTurnNumber () > 1)
		{
			MomServerResourceCalculations.recalculateGlobalProductionValues (onlyOnePlayerID, true, mom.getPlayers (), mom.getGeneralServerKnowledge (),
				mom.getSessionDescription (), mom.getServerDB (), debugLogger);

			// Do this AFTER calculating and accumulating production, so checking for units dying due to insufficient rations happens before city populations might change
			CityProcessing.growCitiesAndProgressConstructionProjects (onlyOnePlayerID, mom.getPlayers (), mom.getGeneralServerKnowledge (),
				mom.getSessionDescription (), mom.getServerDB (), debugLogger);
		}

		// Now need to do one final recalc to take into account
		// 1) Cities producing more food/gold due to increased population
		// 2) Cities eating more food due to increased population
		// 3) Completed buildings (both bonuses and increased maintenance)
		MomServerResourceCalculations.recalculateGlobalProductionValues (onlyOnePlayerID, false, mom.getPlayers (), mom.getGeneralServerKnowledge (),
			mom.getSessionDescription (), mom.getServerDB (), debugLogger);

		debugLogger.exiting (PlayerMessageProcessing.class.getName (), "startPhase");
	}

	/**
	 * In a one-player-at-a-time game, this gets called when a player clicks the Next Turn button to tell everyone whose turn it is now
	 *
	 * @param mom Thread running this session
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws RecordNotFoundException If an expected element cannot be found
	 * @throws PlayerNotFoundException If the player who owns a unit, or the previous or next player cannot be found
	 * @throws MomException If the player's unit doesn't have the experience skill
	 */
	public final static void switchToNextPlayer (final MomSessionThread mom, final Logger debugLogger)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		debugLogger.entering (PlayerMessageProcessing.class.getName (), "switchToNextPlayer",
			new Integer [] {mom.getGeneralPublicKnowledge ().getTurnNumber (), mom.getGeneralPublicKnowledge ().getCurrentPlayerID ()});

		// Find the current player
		int playerIndex;
		if (mom.getGeneralPublicKnowledge ().getCurrentPlayerID () == null)	// First turn
			playerIndex = mom.getPlayers ().size () - 1;		// So we make sure we trip the turn number over
		else
			playerIndex = MultiplayerSessionUtils.indexOfPlayerWithID (mom.getPlayers (), mom.getGeneralPublicKnowledge ().getCurrentPlayerID (), "switchToNextPlayer");

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
		startPhase (mom, mom.getGeneralPublicKnowledge ().getCurrentPlayerID (), debugLogger);

		// Tell everyone who the new current player is, and send each of them their New Turn Messages in the process
		// NB. No NTMs yet because start phase after turn 1 not yet written, so this is a hack for now because we know there are no NTMs
		final SetCurrentPlayerMessage msg = new SetCurrentPlayerMessage ();
		msg.setTurnNumber (mom.getGeneralPublicKnowledge ().getTurnNumber ());
		msg.setCurrentPlayerID (mom.getGeneralPublicKnowledge ().getCurrentPlayerID ());

		for (final PlayerServerDetails thisPlayer : mom.getPlayers ())
			if (thisPlayer.getPlayerDescription ().isHuman ())
			{
				msg.setExpireMessages (thisPlayer == currentPlayer);
				thisPlayer.getConnection ().sendMessageToClient (msg);
			}

		// Erase all pending movements on the client, since we're about to process movement
		if (currentPlayer.getPlayerDescription ().isHuman ())
			currentPlayer.getConnection ().sendMessageToClient (new ErasePendingMovementsMessage ());

		// Continue the player's movement
		// Call to ContinueMovement missing here

		if (currentPlayer.getPlayerDescription ().isHuman ())
		{
			mom.getSessionLogger ().info ("Human turn " + mom.getGeneralPublicKnowledge ().getTurnNumber () + " - " + currentPlayer.getPlayerDescription ().getPlayerName () + "...");
			currentPlayer.getConnection ().sendMessageToClient (new EndOfContinuedMovementMessage ());
		}
		else
		{
			mom.getSessionLogger ().info ("AI turn " + mom.getGeneralPublicKnowledge ().getTurnNumber () + " - " + currentPlayer.getPlayerDescription ().getPlayerName () + "...");
			MomAI.aiPlayerTurn (currentPlayer, mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getSessionDescription (), mom.getServerDB (), debugLogger);
			nextTurnButton (mom, currentPlayer, debugLogger);
		}

		debugLogger.exiting (PlayerMessageProcessing.class.getName (), "switchToNextPlayer",
			new Integer [] {mom.getGeneralPublicKnowledge ().getTurnNumber (), mom.getGeneralPublicKnowledge ().getCurrentPlayerID ()});
	}

	/**
	 * Processes the 'end phase' (for want of something better to call it), which happens at the end of each player's turn
	 *
	 * @param mom Thread running this session
	 * @param onlyOnePlayerID If zero, will process start phase for all players; if specified will process start phase only for the specified player
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	private final static void endPhase (final MomSessionThread mom, final int onlyOnePlayerID, final Logger debugLogger)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		debugLogger.entering (PlayerMessageProcessing.class.getName (), "endPhase", onlyOnePlayerID);

		if (onlyOnePlayerID == 0)
			mom.getSessionLogger ().info ("End phase for everyone turn " + mom.getGeneralPublicKnowledge ().getTurnNumber () + "...");
		else
			mom.getSessionLogger ().info ("End phase for turn " + mom.getGeneralPublicKnowledge ().getTurnNumber () + " - " + MultiplayerSessionServerUtils.findPlayerWithID
				(mom.getPlayers (), onlyOnePlayerID, "endPhase").getPlayerDescription ().getPlayerName () + "...");

		// Put mana into casting spells
		for (final PlayerServerDetails player : mom.getPlayers ())
			if ((onlyOnePlayerID == 0) || (player.getPlayerDescription ().getPlayerID () == onlyOnePlayerID))
				SpellProcessing.progressOverlandCasting (mom.getGeneralServerKnowledge (), player, mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB (), debugLogger);

		// Kick off the next turn
		mom.getSessionLogger ().info ("Kicking off next turn...");
		switch (mom.getSessionDescription ().getTurnSystem ())
		{
			case ONE_PLAYER_AT_A_TIME:
				switchToNextPlayer (mom, debugLogger);
				break;

			default:
				throw new MomException ("endPhase encountered an unknown turn system " + mom.getSessionDescription ().getTurnSystem ());
		}

		debugLogger.exiting (PlayerMessageProcessing.class.getName (), "endPhase");
	}

	/**
	 * Human player has clicked the next turn button, or AI player's turn has finished
	 * @param mom Thread running this session
	 * @param player Player who hit the next turn button
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public final static void nextTurnButton (final MomSessionThread mom, final PlayerServerDetails player, final Logger debugLogger)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		debugLogger.entering (PlayerMessageProcessing.class.getName (), "nextTurnButton", player.getPlayerDescription ().getPlayerID ());

		switch (mom.getSessionDescription ().getTurnSystem ())
		{
			case ONE_PLAYER_AT_A_TIME:
				// In a one-player-at-a-time game, we need to verify that the correct player clicked Next Turn, and if all OK run their end phase and then switch to the next player
				if (mom.getGeneralPublicKnowledge ().getCurrentPlayerID ().equals (player.getPlayerDescription ().getPlayerID ()))
					endPhase (mom, player.getPlayerDescription ().getPlayerID (), debugLogger);
				else
				{
					debugLogger.warning (player.getPlayerDescription ().getPlayerName () + " clicked Next Turn in one-at-a-time game when wasn't their turn");

					final TextPopupMessage reply = new TextPopupMessage ();
					reply.setText ("You clicked the Next Turn button when it wasn't your turn");
					player.getConnection ().sendMessageToClient (reply);
				}
				break;

			default:
				throw new MomException ("nextTurnButton encountered an unknown turn system " + mom.getSessionDescription ().getTurnSystem ());
		}

		debugLogger.exiting (PlayerMessageProcessing.class.getName (), "nextTurnButton");
	}

	/**
	 * Prevent instantiation
	 */
	private PlayerMessageProcessing ()
	{
	}
}
