package momime.server.process;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.calculations.IMomSkillCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.WizardPick;
import momime.common.messages.IMemoryGridCellUtils;
import momime.common.messages.IResourceValueUtils;
import momime.common.messages.IUnitUtils;
import momime.common.messages.PlayerKnowledgeUtils;
import momime.common.messages.servertoclient.v0_9_4.AddNewTurnMessagesMessage;
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
import momime.common.messages.servertoclient.v0_9_4.StartSimultaneousTurnMessage;
import momime.common.messages.servertoclient.v0_9_4.TextPopupMessage;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomGeneralPublicKnowledge;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.PlayerPick;
import momime.common.messages.v0_9_4.SpellResearchStatus;
import momime.common.messages.v0_9_4.SpellResearchStatusID;
import momime.common.messages.v0_9_4.TurnSystem;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.server.IMomSessionVariables;
import momime.server.ai.ICityAI;
import momime.server.ai.IMomAI;
import momime.server.calculations.IMomServerResourceCalculations;
import momime.server.calculations.IMomServerSpellCalculations;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_4.PickFreeSpell;
import momime.server.database.v0_9_4.Unit;
import momime.server.database.v0_9_4.Wizard;
import momime.server.database.v0_9_4.WizardPickCount;
import momime.server.fogofwar.IFogOfWarMidTurnChanges;
import momime.server.fogofwar.IFogOfWarProcessing;
import momime.server.messages.v0_9_4.MomGeneralServerKnowledge;
import momime.server.utils.IPlayerPickServerUtils;
import momime.server.utils.IUnitServerUtils;
import momime.server.utils.RandomUtils;

import com.ndg.multiplayer.server.IMultiplayerServerUtils;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Methods for any significant message processing to do with game startup and the turn system that isn't done in the message implementations
 */
public final class PlayerMessageProcessing implements IPlayerMessageProcessing
{
	/** Class logger */
	private final Logger log = Logger.getLogger (PlayerMessageProcessing.class.getName ());

	/** Unit utils */
	private IUnitUtils unitUtils;
	
	/** MemoryGridCell utils */
	private IMemoryGridCellUtils memoryGridCellUtils;
	
	/** Skill calculations */
	private IMomSkillCalculations skillCalculations;
	
	/** Resource value utils */
	private IResourceValueUtils resourceValueUtils;
	
	/** Methods for updating true map + players' memory */
	private IFogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** City processing methods */
	private ICityProcessing cityProcessing;

	/** Spell processing methods */
	private ISpellProcessing spellProcessing;
	
	/** Fog of war update methods */
	private IFogOfWarProcessing fogOfWarProcessing;
	
	/** Resource calculations */
	private IMomServerResourceCalculations serverResourceCalculations;
	
	/** Server-only pick utils */
	private IPlayerPickServerUtils playerPickServerUtils;
	
	/** Server-only spell calculations */
	private IMomServerSpellCalculations serverSpellCalculations;

	/** Server-only unit utils */
	private IUnitServerUtils unitServerUtils;
	
	/** AI player turns */
	private IMomAI momAI;
	
	/** AI decisions about cities */
	private ICityAI cityAI;
	
	/** Player list utils */
	private IMultiplayerServerUtils multiplayerServerUtils;
	
	/**
	 * Message we send to the server when we choose which wizard we want to be; AI players also call this to do their wizard, picks and spells setup
	 * which is why this isn't all just in ChooseWizardMessageImpl
	 *
	 * @param wizardIdFromMessage wizard ID the player wants to choose
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
	public final void chooseWizard (final String wizardIdFromMessage, final PlayerServerDetails player,
		final List<PlayerServerDetails> players, final MomSessionDescription sd, final ServerDatabaseEx db)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException
	{
		log.entering (PlayerMessageProcessing.class.getName (), "chooseWizard", wizardIdFromMessage);

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
			log.warning (player.getPlayerDescription ().getPlayerName () + " tried to choose invalid wizard ID \"" + wizardID + "\"");

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
					if (log.isLoggable (Level.FINEST))
					{
						String picksDebugDescription = null;

						for (final PlayerPick pick : ppk.getPick ())
						{
							if (picksDebugDescription == null)
								picksDebugDescription = pick.getQuantity () + "x" + pick.getPickID ();
							else
								picksDebugDescription = picksDebugDescription + ", " + pick.getQuantity () + "x" + pick.getPickID ();
						}

						log.finest (PlayerMessageProcessing.class.getName () + ".chooseWizard: Read picks for player '" + player.getPlayerDescription ().getPlayerName () + "' who has chosen pre-defined wizard \"" + wizardID + "\":  " + picksDebugDescription);
					}

					// Commenting this out because don't think client needs this info yet
					// Send picks
					// if (player.getPlayerDescription ().isHuman ())
					// sendPicksToPlayer (players, player, false, false);

					// Tell client to either pick free starting spells or pick a race, depending on whether the pre-defined wizard chosen has >1 of any kind of book
					// Its fine to do this before we confirm to the client that their wizard choice was OK by the mmChosenWizard message sent below
					if (player.getPlayerDescription ().isHuman ())
					{
						// This will tell the client to either pick free spells for the first magic realm that they have earned free spells in, or pick their race, depending on what picks they've chosen
						log.finest (PlayerMessageProcessing.class.getName () + ".chooseWizard: About to search for first realm (if any) where human player " + player.getPlayerDescription ().getPlayerName () + " gets free spells");
						final ChooseInitialSpellsNowMessage chooseSpellsMsg = getPlayerPickServerUtils ().findRealmIDWhereWeNeedToChooseFreeSpells (player, db);
						if (chooseSpellsMsg != null)
							player.getConnection ().sendMessageToClient (chooseSpellsMsg);
						else
							player.getConnection ().sendMessageToClient (new ChooseYourRaceNowMessage ());
					}
					else
					{
						// For AI players, we call this repeatedly until all free spells have been chosen
						log.finest (PlayerMessageProcessing.class.getName () + ".chooseWizard: About to choose all free spells for AI player " + player.getPlayerDescription ().getPlayerName ());

						while (getPlayerPickServerUtils ().findRealmIDWhereWeNeedToChooseFreeSpells (player, db) != null);
					}
				}
			}

			// Tell everyone about the wizard this player has chosen
			broadcastWizardChoice (players, player);
		}

		log.exiting (PlayerMessageProcessing.class.getName (), "chooseWizard");
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
		log.entering (PlayerMessageProcessing.class.getName (), "broadcastWizardChoice", player.getPlayerDescription ().getPlayerID ());

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

		getMultiplayerServerUtils ().sendMessageToAllClients (players, msg);

		log.exiting (PlayerMessageProcessing.class.getName (), "broadcastWizardChoice");
	}

	/**
	 * @param players List of players in the session
	 * @param stage Stage of starting up the game that we are currently at
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	private final void sendStartGameProgressMessage (final List<PlayerServerDetails> players, final StartGameProgressStageID stage)
		throws JAXBException, XMLStreamException
	{
		log.entering (PlayerMessageProcessing.class.getName (), "sendStartGameProgressMessage", stage);

		final StartGameProgressMessage msg = new StartGameProgressMessage ();
		msg.setStage (stage);

		getMultiplayerServerUtils ().sendMessageToAllClients (players, msg);

		log.exiting (PlayerMessageProcessing.class.getName (), "sendStartGameProgressMessage");
	}

	/**
	 * @param wizard Wizard this AI player is playing
	 * @return Player description created for this AI player
	 */
	private final PlayerDescription createAiPlayerDescription (final Wizard wizard)
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
	private final void createHeroes (final List<PlayerServerDetails> players, final MomGeneralServerKnowledge gsk,
		final MomSessionDescription sd, final ServerDatabaseEx db)
		throws MomException, RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.entering (PlayerMessageProcessing.class.getName (), "createHeroes");

		for (final Unit thisUnit : db.getUnit ())
			if (thisUnit.getUnitMagicRealm ().equals (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))

				// Add this hero for all players, even raiders, just not the monsters
				// We won't end up sending these to the client since we're setting status as 'not generated'
				for (final PlayerServerDetails thisPlayer : players)
				{
					final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) thisPlayer.getPersistentPlayerPublicKnowledge ();
					if (!ppk.getWizardID ().equals (CommonDatabaseConstants.WIZARD_ID_MONSTERS))
						getFogOfWarMidTurnChanges ().addUnitOnServerAndClients (gsk, thisUnit.getUnitID (), null, null, null, thisPlayer, UnitStatusID.NOT_GENERATED, null, sd, db);
				}

		log.exiting (PlayerMessageProcessing.class.getName (), "createHeroes");
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
	public final void checkIfCanStartGame (final IMomSessionVariables mom)
		throws JAXBException, XMLStreamException, MomException, RecordNotFoundException, PlayerNotFoundException
	{
		log.entering (PlayerMessageProcessing.class.getName (), "checkIfCanStartGame");

		if (getPlayerPickServerUtils ().allPlayersHaveChosenAllDetails (mom.getPlayers (), mom.getSessionDescription ()))
		{
			// Add AI wizards
			log.finest ("checkIfCanStartGame: Yes, " + mom.getSessionDescription ().getAiPlayerCount () + " AI wizards to add");
			mom.getSessionLogger ().info ("All Human players joined - adding AI player(s) and starting game...");

			sendStartGameProgressMessage (mom.getPlayers (), StartGameProgressStageID.ADDING_AI_PLAYERS);
			if (mom.getSessionDescription ().getAiPlayerCount () > 0)
			{
				// Get list of wizard IDs for AI players to choose from
				final List<Wizard> availableWizards = getPlayerPickServerUtils ().listWizardsForAIPlayers (mom.getPlayers (), mom.getServerDB ());
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
			log.finest ("checkIfCanStartGame: Broadcasting player picks and determining which spells not chosen for free will be researchable");
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

					getMultiplayerServerUtils ().sendMessageToAllClients (mom.getPlayers (), msg);
				}

				else if (ppk.getCustomPhoto () != null)
				{
					final ChosenCustomPhotoMessage msg = new ChosenCustomPhotoMessage ();
					msg.setPlayerID (thisPlayer.getPlayerDescription ().getPlayerID ());
					msg.setFlagColour (ppk.getCustomFlagColour ());
					msg.setNdgBmpImage (ppk.getCustomPhoto ());

					getMultiplayerServerUtils ().sendMessageToAllClients (mom.getPlayers (), msg);
				}

				// Send picks to everyone - note we don't know if they've already received them, if e.g. player
				// 1 joins and makes their picks then these get stored into public info on the server, then
				// player 2 joins and will be sent to them
				final ReplacePicksMessage picksMsg = new ReplacePicksMessage ();
				picksMsg.setPlayerID (thisPlayer.getPlayerDescription ().getPlayerID ());
				picksMsg.getPick ().addAll (ppk.getPick ());
				getMultiplayerServerUtils ().sendMessageToAllClients (mom.getPlayers (), picksMsg);

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
			sendStartGameProgressMessage (mom.getPlayers (), StartGameProgressStageID.ADDING_MONSTERS);
			mom.getSessionLogger ().info ("Filling nodes, lairs & towers with monsters...");
			mom.getOverlandMapGenerator ().fillNodesLairsAndTowersWithMonsters (mom.getGeneralServerKnowledge (), monstersPlayer);

			// Sort out heroes
			sendStartGameProgressMessage (mom.getPlayers (), StartGameProgressStageID.ADDING_HEROES);
			mom.getSessionLogger ().info ("Loading list of heroes for each player...");
			createHeroes (mom.getPlayers (), mom.getGeneralServerKnowledge (), mom.getSessionDescription (), mom.getServerDB ());

			if (mom.getSessionDescription ().getUnitSetting ().isRollHeroSkillsAtStartOfGame ())
			{
				mom.getSessionLogger ().info ("Randomzing hero skills for each player...");
				for (final MemoryUnit thisUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
					if (mom.getServerDB ().findUnit (thisUnit.getUnitID (), "checkIfCanStartGame").getUnitMagicRealm ().equals (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
						getUnitServerUtils ().generateHeroNameAndRandomSkills (thisUnit, mom.getServerDB ());
			}

			// Create cities
			sendStartGameProgressMessage (mom.getPlayers (), StartGameProgressStageID.ADDING_CITIES);
			mom.getSessionLogger ().info ("Creating starting cities...");
			getCityProcessing ().createStartingCities (mom.getPlayers (), mom.getGeneralServerKnowledge (), mom.getSessionDescription (), mom.getServerDB ());

			// Now we've created starting cities, we can figure out the initial fog of war area that each player can see
			sendStartGameProgressMessage (mom.getPlayers (), StartGameProgressStageID.GENERATING_INITIAL_FOG_OF_WAR);
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
					getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT,
						getSkillCalculations ().getSkillPointsRequiredForCastingSkill (getPlayerPickServerUtils ().getTotalInitialSkill
							(ppk.getPick (), mom.getServerDB ())));

					// Give each wizard their starting gold
					final int startingGold;
					if (thisPlayer.getPlayerDescription ().isHuman ())
						startingGold = mom.getSessionDescription ().getDifficultyLevel ().getHumanStartingGold ();
					else
						startingGold = mom.getSessionDescription ().getDifficultyLevel ().getAiStartingGold ();

					getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, startingGold);
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
			getMultiplayerServerUtils ().sendMessageToAllClients (mom.getPlayers (), new StartGameMessage ());

			// Kick off the first turn
			mom.getSessionLogger ().info ("Kicking off first turn...");
			switch (mom.getSessionDescription ().getTurnSystem ())
			{
				case ONE_PLAYER_AT_A_TIME:
					switchToNextPlayer (mom);
					break;

				default:
					throw new MomException ("checkIfCanStartGame encountered an unknown turn system " + mom.getSessionDescription ().getTurnSystem ());
			}
		}

		log.exiting (PlayerMessageProcessing.class.getName (), "checkIfCanStartGame");
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
	private final void startPhase (final IMomSessionVariables mom, final int onlyOnePlayerID)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.entering (PlayerMessageProcessing.class.getName (), "startPhase", onlyOnePlayerID);

		if (onlyOnePlayerID == 0)
			mom.getSessionLogger ().info ("Start phase for everyone turn " + mom.getGeneralPublicKnowledge ().getTurnNumber () + "...");
		else
			mom.getSessionLogger ().info ("Start phase for turn " + mom.getGeneralPublicKnowledge ().getTurnNumber () + " - " + MultiplayerSessionServerUtils.findPlayerWithID
				(mom.getPlayers (), onlyOnePlayerID, "startPhase").getPlayerDescription ().getPlayerName () + "...");

		// Give units their full movement back again
		getUnitUtils ().resetUnitOverlandMovement (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), onlyOnePlayerID, mom.getServerDB ());

		// Heal hurt units 1pt and gain 1exp
		getFogOfWarMidTurnChanges ().healUnitsAndGainExperience (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), onlyOnePlayerID,
			mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ());

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

		log.exiting (PlayerMessageProcessing.class.getName (), "startPhase");
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
	public final void switchToNextPlayer (final IMomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.entering (PlayerMessageProcessing.class.getName (), "switchToNextPlayer",
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
		startPhase (mom, mom.getGeneralPublicKnowledge ().getCurrentPlayerID ());

		// Tell everyone who the new current player is, and send each of them their New Turn Messages in the process
		sendNewTurnMessages (mom.getGeneralPublicKnowledge (), mom.getPlayers (), mom.getSessionDescription ().getTurnSystem ());

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
			getMomAI ().aiPlayerTurn (currentPlayer, mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getSessionDescription (), mom.getServerDB ());
			nextTurnButton (mom, currentPlayer);
		}

		log.exiting (PlayerMessageProcessing.class.getName (), "switchToNextPlayer",
			"Player ID " + mom.getGeneralPublicKnowledge ().getCurrentPlayerID () + " turn " + mom.getGeneralPublicKnowledge ().getTurnNumber ());
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
		log.entering (PlayerMessageProcessing.class.getName (), "sendNewTurnMessages");
		
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
					throw new MomException (PlayerMessageProcessing.class.getName () + ".sendNewTurnMessages doesn't know how handle messageType of " + messageType);
			}
		
		log.exiting (PlayerMessageProcessing.class.getName (), "sendNewTurnMessages");
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
	private final void endPhase (final IMomSessionVariables mom, final int onlyOnePlayerID)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.entering (PlayerMessageProcessing.class.getName (), "endPhase", onlyOnePlayerID);

		if (onlyOnePlayerID == 0)
			mom.getSessionLogger ().info ("End phase for everyone turn " + mom.getGeneralPublicKnowledge ().getTurnNumber () + "...");
		else
			mom.getSessionLogger ().info ("End phase for turn " + mom.getGeneralPublicKnowledge ().getTurnNumber () + " - " + MultiplayerSessionServerUtils.findPlayerWithID
				(mom.getPlayers (), onlyOnePlayerID, "endPhase").getPlayerDescription ().getPlayerName () + "...");

		// Put mana into casting spells
		for (final PlayerServerDetails player : mom.getPlayers ())
			if ((onlyOnePlayerID == 0) || (player.getPlayerDescription ().getPlayerID () == onlyOnePlayerID))
				getSpellProcessing ().progressOverlandCasting (mom.getGeneralServerKnowledge (), player, mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());

		// Kick off the next turn
		mom.getSessionLogger ().info ("Kicking off next turn...");
		switch (mom.getSessionDescription ().getTurnSystem ())
		{
			case ONE_PLAYER_AT_A_TIME:
				switchToNextPlayer (mom);
				break;

			default:
				throw new MomException ("endPhase encountered an unknown turn system " + mom.getSessionDescription ().getTurnSystem ());
		}

		log.exiting (PlayerMessageProcessing.class.getName (), "endPhase");
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
	public final void nextTurnButton (final IMomSessionVariables mom, final PlayerServerDetails player)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.entering (PlayerMessageProcessing.class.getName (), "nextTurnButton", player.getPlayerDescription ().getPlayerID ());

		switch (mom.getSessionDescription ().getTurnSystem ())
		{
			case ONE_PLAYER_AT_A_TIME:
				// In a one-player-at-a-time game, we need to verify that the correct player clicked Next Turn, and if all OK run their end phase and then switch to the next player
				if (mom.getGeneralPublicKnowledge ().getCurrentPlayerID ().equals (player.getPlayerDescription ().getPlayerID ()))
					endPhase (mom, player.getPlayerDescription ().getPlayerID ());
				else
				{
					log.warning (player.getPlayerDescription ().getPlayerName () + " clicked Next Turn in one-at-a-time game when wasn't their turn");

					final TextPopupMessage reply = new TextPopupMessage ();
					reply.setText ("You clicked the Next Turn button when it wasn't your turn");
					player.getConnection ().sendMessageToClient (reply);
				}
				break;

			default:
				throw new MomException ("nextTurnButton encountered an unknown turn system " + mom.getSessionDescription ().getTurnSystem ());
		}

		log.exiting (PlayerMessageProcessing.class.getName (), "nextTurnButton");
	}

	/**
	 * @return Unit utils
	 */
	public final IUnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final IUnitUtils utils)
	{
		unitUtils = utils;
	}

	/**
	 * @return MemoryGridCell utils
	 */
	public final IMemoryGridCellUtils getMemoryGridCellUtils ()
	{
		return memoryGridCellUtils;
	}

	/**
	 * @param utils MemoryGridCell utils
	 */
	public final void setMemoryGridCellUtils (final IMemoryGridCellUtils utils)
	{
		memoryGridCellUtils = utils;
	}
	
	/**
	 * @return Skill calculations
	 */
	public final IMomSkillCalculations getSkillCalculations ()
	{
		return skillCalculations;
	}

	/**
	 * @param calc Skill calculations
	 */
	public final void setSkillCalculations (final IMomSkillCalculations calc)
	{
		skillCalculations = calc;
	}
	
	/**
	 * @return Resource value utils
	 */
	public final IResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
	}

	/**
	 * @param utils Resource value utils
	 */
	public final void setResourceValueUtils (final IResourceValueUtils utils)
	{
		resourceValueUtils = utils;
	}
	
	/**
	 * @return Methods for updating true map + players' memory
	 */
	public final IFogOfWarMidTurnChanges getFogOfWarMidTurnChanges ()
	{
		return fogOfWarMidTurnChanges;
	}

	/**
	 * @param obj Methods for updating true map + players' memory
	 */
	public final void setFogOfWarMidTurnChanges (final IFogOfWarMidTurnChanges obj)
	{
		fogOfWarMidTurnChanges = obj;
	}

	/**
	 * @return City processing methods
	 */
	public final ICityProcessing getCityProcessing ()
	{
		return cityProcessing;
	}

	/**
	 * @param obj City processing methods
	 */
	public final void setCityProcessing (final ICityProcessing obj)
	{
		cityProcessing = obj;
	}

	/**
	 * @return Spell processing methods
	 */
	public final ISpellProcessing getSpellProcessing ()
	{
		return spellProcessing;
	}

	/**
	 * @param obj Spell processing methods
	 */
	public final void setSpellProcessing (final ISpellProcessing obj)
	{
		spellProcessing = obj;
	}

	/**
	 * @return Fog of war update methods
	 */
	public final IFogOfWarProcessing getFogOfWarProcessing ()
	{
		return fogOfWarProcessing;
	}

	/**
	 * @param obj Fog of war update methods
	 */
	public final void setFogOfWarProcessing (final IFogOfWarProcessing obj)
	{
		fogOfWarProcessing = obj;
	}

	/**
	 * @return Resource calculations
	 */
	public final IMomServerResourceCalculations getServerResourceCalculations ()
	{
		return serverResourceCalculations;
	}

	/**
	 * @param calc Resource calculations
	 */
	public final void setServerResourceCalculations (final IMomServerResourceCalculations calc)
	{
		serverResourceCalculations = calc;
	}

	/**
	 * @return Server-only pick utils
	 */
	public final IPlayerPickServerUtils getPlayerPickServerUtils ()
	{
		return playerPickServerUtils;
	}

	/**
	 * @param utils Server-only pick utils
	 */
	public final void setPlayerPickServerUtils (final IPlayerPickServerUtils utils)
	{
		playerPickServerUtils = utils;
	}

	/**
	 * @return Server-only spell calculations
	 */
	public final IMomServerSpellCalculations getServerSpellCalculations ()
	{
		return serverSpellCalculations;
	}

	/**
	 * @param calc Server-only spell calculations
	 */
	public final void setServerSpellCalculations (final IMomServerSpellCalculations calc)
	{
		serverSpellCalculations = calc;
	}

	/**
	 * @return Server-only unit utils
	 */
	public final IUnitServerUtils getUnitServerUtils ()
	{
		return unitServerUtils;
	}

	/**
	 * @param utils Server-only unit utils
	 */
	public final void setUnitServerUtils (final IUnitServerUtils utils)
	{
		unitServerUtils = utils;
	}
	
	/**
	 * @return AI player turns
	 */
	public final IMomAI getMomAI ()
	{
		return momAI;
	}

	/**
	 * @param ai AI player turns
	 */
	public final void setMomAI (final IMomAI ai)
	{
		momAI = ai;
	}

	/**
	 * @return AI decisions about cities
	 */
	public final ICityAI getCityAI ()
	{
		return cityAI;
	}

	/**
	 * @param ai AI decisions about cities
	 */
	public final void setCityAI (final ICityAI ai)
	{
		cityAI = ai;
	}

	/**
	 * @return Player list utils
	 */
	public final IMultiplayerServerUtils getMultiplayerServerUtils ()
	{
		return multiplayerServerUtils;
	}

	/**
	 * @param obj Player list utils
	 */
	public final void setMultiplayerServerUtils (final IMultiplayerServerUtils obj)
	{
		multiplayerServerUtils = obj;
	}
}
