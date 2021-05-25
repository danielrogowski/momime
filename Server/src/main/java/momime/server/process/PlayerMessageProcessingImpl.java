package momime.server.process;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.calculations.SkillCalculations;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.FogOfWarSetting;
import momime.common.database.MomDatabase;
import momime.common.database.PickAndQuantity;
import momime.common.database.Plane;
import momime.common.database.RecordNotFoundException;
import momime.common.database.TileType;
import momime.common.database.UnitEx;
import momime.common.database.UnitSpecialOrder;
import momime.common.database.WizardEx;
import momime.common.database.WizardPickCount;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomGeneralPublicKnowledge;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.PendingMovement;
import momime.common.messages.PlayerPick;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;
import momime.common.messages.TurnSystem;
import momime.common.messages.UnitStatusID;
import momime.common.messages.WizardState;
import momime.common.messages.servertoclient.AddNewTurnMessagesMessage;
import momime.common.messages.servertoclient.AddPowerBaseHistoryMessage;
import momime.common.messages.servertoclient.AnimationID;
import momime.common.messages.servertoclient.ChooseInitialSpellsNowMessage;
import momime.common.messages.servertoclient.ChooseYourRaceNowMessage;
import momime.common.messages.servertoclient.ChosenCustomPhotoMessage;
import momime.common.messages.servertoclient.ChosenStandardPhotoMessage;
import momime.common.messages.servertoclient.ChosenWizardMessage;
import momime.common.messages.servertoclient.EndOfContinuedMovementMessage;
import momime.common.messages.servertoclient.ErasePendingMovementsMessage;
import momime.common.messages.servertoclient.FullSpellListMessage;
import momime.common.messages.servertoclient.OnePlayerSimultaneousTurnDoneMessage;
import momime.common.messages.servertoclient.PlayAnimationMessage;
import momime.common.messages.servertoclient.PowerBaseHistoryPlayer;
import momime.common.messages.servertoclient.ReplacePicksMessage;
import momime.common.messages.servertoclient.SetCurrentPlayerMessage;
import momime.common.messages.servertoclient.StartGameMessage;
import momime.common.messages.servertoclient.StartSimultaneousTurnMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.utils.CompareUtils;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.PlayerKnowledgeUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.ai.CityAI;
import momime.server.ai.MomAI;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.calculations.ServerSpellCalculations;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.FogOfWarMidTurnMultiChanges;
import momime.server.fogofwar.FogOfWarProcessing;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.messages.MomGeneralServerKnowledge;
import momime.server.utils.PlayerPickServerUtils;
import momime.server.utils.PlayerServerUtils;
import momime.server.utils.UnitServerUtils;

/**
 * Methods for any significant message processing to do with game startup and the turn system that isn't done in the message implementations
 */
public final class PlayerMessageProcessingImpl implements PlayerMessageProcessing
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (PlayerMessageProcessingImpl.class);
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** MemoryGridCell utils */
	private MemoryGridCellUtils memoryGridCellUtils;
	
	/** Skill calculations */
	private SkillCalculations skillCalculations;
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnMultiChanges fogOfWarMidTurnMultiChanges;
	
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
	
	/** Random number generator */
	private RandomUtils randomUtils;

	/** Number of save points to keep for each session */
	private int savePointKeepCount;
	
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
		final List<PlayerServerDetails> players, final MomSessionDescription sd, final CommonDatabase db)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException
	{
		// Check if not specified
		boolean valid;
		WizardEx wizard = null;
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
		else if ((sd.getDifficultyLevel ().isEachWizardOnlyOnce ()) && (!PlayerKnowledgeUtils.isCustomWizard (wizardID)) &&
			(getPlayerPickServerUtils ().findPlayerUsingWizard (players, wizardID) != null))
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

					final Optional<WizardPickCount> pickCount = wizard.getWizardPickCount ().stream ().filter (pc -> pc.getPickCount () == desiredPickCount).findAny ();
					if (pickCount.isEmpty ())
						throw new RecordNotFoundException (WizardPickCount.class, wizardID + "-" + desiredPickCount, "chooseWizard");

					// Read pre-defined wizard's list of picks from the DB and send them to the player
					// We'll send them to everyone else when the game starts
					for (final PickAndQuantity srcPick : pickCount.get ().getWizardPick ())
					{
						final PlayerPick destPick = new PlayerPick ();
						destPick.setPickID (srcPick.getPickID ());
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
		// Convert empty string (custom wizard) to a null
		final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();

		final ChosenWizardMessage msg = new ChosenWizardMessage ();
		msg.setPlayerID (player.getPlayerDescription ().getPlayerID ());
		msg.setWizardID (ppk.getWizardID ());

		getMultiplayerSessionServerUtils ().sendMessageToAllClients (players, msg);
	}

	/**
	 * @param wizard Wizard this AI player is playing
	 * @return Player description created for this AI player
	 */
	private final PlayerDescription createAiPlayerDescription (final WizardEx wizard)
	{
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerName (wizard.getWizardID () + "-" + wizard.getWizardName ().get (0).getText ());
		pd.setHuman (false);

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
		final MomSessionDescription sd, final CommonDatabase db)
		throws MomException, RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		for (final UnitEx thisUnit : db.getUnits ())
			if (thisUnit.getUnitMagicRealm ().equals (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))

				// Add this hero for all players, even raiders, just not the monsters
				// We won't end up sending these to the client since we're setting status as 'not generated'
				for (final PlayerServerDetails thisPlayer : players)
				{
					final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) thisPlayer.getPersistentPlayerPublicKnowledge ();
					if (!ppk.getWizardID ().equals (CommonDatabaseConstants.WIZARD_ID_MONSTERS))
						getFogOfWarMidTurnChanges ().addUnitOnServerAndClients (gsk, thisUnit.getUnitID (), null, null, null, thisPlayer, UnitStatusID.NOT_GENERATED, null, sd, db);
				}
	}


	/**
	 * If all players have chosen their wizards and, if necessary, custom picks, then sends message to tell everyone to start
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending any messages to the clients
	 * @throws XMLStreamException If there is a problem sending any messages to the clients
	 * @throws IOException If there are any other kinds of faults
	 */
	@Override
	public final void checkIfCanStartGame (final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		if (getPlayerPickServerUtils ().allPlayersHaveChosenAllDetails (mom.getPlayers (), mom.getSessionDescription ()))
		{
			// Add AI wizards
			log.debug ("checkIfCanStartGame: Yes, " + mom.getSessionDescription ().getAiPlayerCount () + " AI wizards to add");
			log.info ("All Human players joined - adding AI player(s) and starting game...");

			if (mom.getSessionDescription ().getAiPlayerCount () > 0)
			{
				// Get list of wizard IDs for AI players to choose from
				final List<WizardEx> availableWizards = getPlayerPickServerUtils ().listWizardsForAIPlayers (mom.getPlayers (), mom.getServerDB ());
				for (int aiPlayerNo = 0; aiPlayerNo < mom.getSessionDescription ().getAiPlayerCount (); aiPlayerNo++)
				{
					// Pick a random wizard for this AI player
					if (availableWizards.size () == 0)
						throw new MomException ("Not enough Wizards defined for number of AI players");

					final WizardEx chosenWizard = availableWizards.get (getRandomUtils ().nextInt (availableWizards.size ()));
					availableWizards.remove (chosenWizard);

					// Add AI player
					final PlayerServerDetails aiPlayer = mom.addPlayer (createAiPlayerDescription (chosenWizard), null, null, true, null);

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
			final PlayerServerDetails raidersPlayer = mom.addPlayer (createAiPlayerDescription
				(mom.getServerDB ().findWizard (CommonDatabaseConstants.WIZARD_ID_RAIDERS, "checkIfCanStartGame")), null, null, true, null);

			final MomPersistentPlayerPrivateKnowledge raidersPriv = (MomPersistentPlayerPrivateKnowledge) raidersPlayer.getPersistentPlayerPrivateKnowledge ();
			raidersPriv.getSpellResearchStatus ().forEach (r -> r.setStatus (SpellResearchStatusID.UNAVAILABLE));
			
			chooseWizard (CommonDatabaseConstants.WIZARD_ID_RAIDERS, raidersPlayer, mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());

			// Add monsters
			final PlayerServerDetails monstersPlayer = mom.addPlayer (createAiPlayerDescription
				(mom.getServerDB ().findWizard (CommonDatabaseConstants.WIZARD_ID_MONSTERS, "checkIfCanStartGame")), null, null, true, null);

			final MomPersistentPlayerPrivateKnowledge monstersPriv = (MomPersistentPlayerPrivateKnowledge) monstersPlayer.getPersistentPlayerPrivateKnowledge ();
			monstersPriv.getSpellResearchStatus ().forEach (r -> r.setStatus (SpellResearchStatusID.UNAVAILABLE));
			
			chooseWizard (CommonDatabaseConstants.WIZARD_ID_MONSTERS, monstersPlayer, mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());

			// Broadcast player data
			log.debug ("checkIfCanStartGame: Broadcasting player picks and determining which spells not chosen for free will be researchable");
			log.info ("Broadcasting player picks and randomizing initial spells...");

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
					for (final String freeSpellID : mom.getServerDB ().findPick (pick.getPickID (), "checkIfCanStartGame").getPickFreeSpell ())
						freeSpellIDs.add (freeSpellID);

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
			log.info ("Filling nodes, lairs & towers with monsters...");
			mom.getOverlandMapGenerator ().fillNodesLairsAndTowersWithMonsters (monstersPlayer);

			// Sort out heroes
			log.info ("Loading list of heroes for each player...");
			createHeroes (mom.getPlayers (), mom.getGeneralServerKnowledge (), mom.getSessionDescription (), mom.getServerDB ());

			if (mom.getSessionDescription ().getUnitSetting ().isRollHeroSkillsAtStartOfGame ())
			{
				log.info ("Randomzing hero skills for each player...");
				for (final MemoryUnit thisUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
					if (mom.getServerDB ().findUnit (thisUnit.getUnitID (), "checkIfCanStartGame").getUnitMagicRealm ().equals (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
						getUnitServerUtils ().generateHeroNameAndRandomSkills (thisUnit, mom.getServerDB ());
			}

			// Create cities
			log.info ("Creating starting cities...");
			getCityProcessing ().createStartingCities (mom.getPlayers (), mom.getGeneralServerKnowledge (), mom.getSessionDescription (), mom.getServerDB ());

			// Now we've created starting cities, we can figure out the initial fog of war area that each player can see
			log.info ("Generating and sending initial fog of war...");
			for (final PlayerServerDetails thisPlayer : mom.getPlayers ())
				getFogOfWarProcessing ().updateAndSendFogOfWar (mom.getGeneralServerKnowledge ().getTrueMap (), thisPlayer,
					mom.getPlayers (), "checkIfCanStartGame", mom.getSessionDescription (), mom.getServerDB ());

			// Give each wizard initial skill and gold, and setting optional farmers in all cities
			log.info ("Setting wizards' initial skill and gold, and optional farmers in all cities");
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
			log.info ("Calculating initial production values...");
			getServerResourceCalculations ().recalculateGlobalProductionValues (0, false, mom);

			// Kick off the game - this shows the map screen for the first time
			log.info ("Starting game...");
			getMultiplayerSessionServerUtils ().sendMessageToAllClients (mom.getPlayers (), new StartGameMessage ());

			// Kick off the first turn
			log.info ("Kicking off first turn...");
			switch (mom.getSessionDescription ().getTurnSystem ())
			{
				case ONE_PLAYER_AT_A_TIME:
					switchToNextPlayer (mom, false);
					break;

				case SIMULTANEOUS:
					kickOffSimultaneousTurn (mom, false);
					break;
					
				default:
					throw new MomException ("checkIfCanStartGame encountered an unknown turn system " + mom.getSessionDescription ().getTurnSystem ());
			}
		}
	}

	/**
	 * After reloading a saved game, checks whether all human players have joined back in, and if so then starts the game back up again
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending any messages to the clients
	 * @throws XMLStreamException If there is a problem sending any messages to the clients
	 * @throws MomException If there is a problem in any game logic or data
	 * @throws RecordNotFoundException If various elements cannot be found in the DB
	 * @throws PlayerNotFoundException If we encounter players that we cannot find in the list
	 */
	@Override
	public final void checkIfCanStartLoadedGame (final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, MomException, RecordNotFoundException, PlayerNotFoundException
	{
		if (getPlayerPickServerUtils ().allPlayersAreConnected (mom.getPlayers ()))
		{
			// Use the same Start Game message as when starting a new game; this tells the client to close out the "Wait for players" list and show the overland map
			log.info ("Starting game...");
			getMultiplayerSessionServerUtils ().sendMessageToAllClients (mom.getPlayers (), new StartGameMessage ());
			
			// Start up the first turn
			switch (mom.getSessionDescription ().getTurnSystem ())
			{
				case ONE_PLAYER_AT_A_TIME:
					switchToNextPlayer (mom, true);
					break;

				case SIMULTANEOUS:
					kickOffSimultaneousTurn (mom, true);
					break;
					
				default:
					throw new MomException ("checkIfCanStartLoadedGame encountered an unknown turn system " + mom.getSessionDescription ().getTurnSystem ());
			}
		}
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
		if (onlyOnePlayerID == 0)
			log.info ("Start phase for everyone turn " + mom.getGeneralPublicKnowledge ().getTurnNumber () + "...");
		else
			log.info ("Start phase for turn " + mom.getGeneralPublicKnowledge ().getTurnNumber () + " - " + getMultiplayerSessionServerUtils ().findPlayerWithID
				(mom.getPlayers (), onlyOnePlayerID, "startPhase").getPlayerDescription ().getPlayerName () + "...");

		// Heal hurt units 1pt and gain 1exp
		getFogOfWarMidTurnMultiChanges ().healUnitsAndGainExperience (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), onlyOnePlayerID,
			mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting ());

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

		// Give units their full movement back again
		// NB. Do this after our cities may have constructed new units above
		getFogOfWarMidTurnMultiChanges ().resetUnitOverlandMovement (onlyOnePlayerID, mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (),
			mom.getSessionDescription ().getFogOfWarSetting (), mom.getServerDB ());
		
		// Now need to do one final recalc to take into account
		// 1) Cities producing more food/gold due to increased population
		// 2) Cities eating more food due to increased population
		// 3) Completed buildings (both bonuses and increased maintenance)
		getServerResourceCalculations ().recalculateGlobalProductionValues (onlyOnePlayerID, false, mom);
		storePowerBaseHistory (onlyOnePlayerID, mom.getPlayers ());
	}
	
	/**
	 * Saves the game state for the current turn
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 */
	private final void saveGame (final MomSessionVariables mom)
	{
		try
		{
			// Temporarily strip out the database, so it doesn't get included in the saved game file.
			final MomDatabase db = mom.getGeneralPublicKnowledge ().getMomDatabase ();
			mom.getGeneralPublicKnowledge ().setMomDatabase (null);
			try
			{
				mom.saveGame (Integer.valueOf (mom.getGeneralPublicKnowledge ().getTurnNumber ()).toString ());
			}
			finally
			{
				mom.getGeneralPublicKnowledge ().setMomDatabase (db);
			}
			
			if (getSavePointKeepCount () > 0)
				mom.deleteOldestSavePoints (getSavePointKeepCount ());
		}
		catch (final Exception e)
		{
			// Don't allow failure to save the game to totally kill things if there's a problem
			log.error (e, e);
		}
	}

	/**
	 * In a one-player-at-a-time game, this gets called when a player clicks the Next Turn button to tell everyone whose turn it is now
	 *
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param loadingSavedGame True if the turn is being started immediately after loading a saved game
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws RecordNotFoundException If an expected element cannot be found
	 * @throws PlayerNotFoundException If the player who owns a unit, or the previous or next player cannot be found
	 * @throws MomException If the player's unit doesn't have the experience skill
	 */
	@Override
	public final void switchToNextPlayer (final MomSessionVariables mom, final boolean loadingSavedGame)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		final PlayerServerDetails currentPlayer;
		if (loadingSavedGame)
			currentPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), mom.getGeneralPublicKnowledge ().getCurrentPlayerID (), "switchToNextPlayer (L)");
		else
		{
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
	
			currentPlayer = mom.getPlayers ().get (playerIndex);
			mom.getGeneralPublicKnowledge ().setCurrentPlayerID (currentPlayer.getPlayerDescription ().getPlayerID ());
			
			// Save the game on turn number changes
			if (playerIndex == 0)
				saveGame (mom);
		}

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
			log.info ("Human turn " + mom.getGeneralPublicKnowledge ().getTurnNumber () + " - " + currentPlayer.getPlayerDescription ().getPlayerName () + "...");
			currentPlayer.getConnection ().sendMessageToClient (new EndOfContinuedMovementMessage ());
		}
		else
		{
			log.info ("AI turn " + mom.getGeneralPublicKnowledge ().getTurnNumber () + " - " + currentPlayer.getPlayerDescription ().getPlayerName () + "...");
			if (getMomAI ().aiPlayerTurn (currentPlayer, mom))
			
				// In the Delphi version, this is triggered back in the VCL thread via the OnTerminate method (which isn't obvious)
				nextTurnButton (mom, currentPlayer);
		}
	}

	/**
	 * Kicks off a new turn in an everybody-allocate-movement-then-move-simultaneously game
	 *
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param loadingSavedGame True if the turn is being started immediately after loading a saved game
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws RecordNotFoundException If an expected element cannot be found
	 * @throws PlayerNotFoundException If the player who owns a unit, or the previous or next player cannot be found
	 * @throws MomException If the player's unit doesn't have the experience skill
	 */
	@Override
	public final void kickOffSimultaneousTurn (final MomSessionVariables mom, final boolean loadingSavedGame)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		if (!loadingSavedGame)
		{
			// Bump up the turn number
			mom.getGeneralPublicKnowledge ().setTurnNumber (mom.getGeneralPublicKnowledge ().getTurnNumber () + 1);
	
			// Save the game every turn
			saveGame (mom);
		}
		
		// Process everybody's start phases together
		startPhase (mom, 0);

		// Tell all human players to take their go, and send each of them their New Turn Messages in the process
		sendNewTurnMessages (mom.getGeneralPublicKnowledge (), mom.getPlayers (), mom.getSessionDescription ().getTurnSystem ());

		// Every AI player has their turn
		for (final PlayerServerDetails aiPlayer : mom.getPlayers ())
			if (!aiPlayer.getPlayerDescription ().isHuman ())
			{
				log.info ("AI turn " + mom.getGeneralPublicKnowledge ().getTurnNumber () + " - " + aiPlayer.getPlayerDescription ().getPlayerName () + "...");
				getMomAI ().aiPlayerTurn (aiPlayer, mom);
			
				// In the Delphi version, this is triggered back in the VCL thread via the OnTerminate method (which isn't obvious)
				nextTurnButton (mom, aiPlayer);
			}
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
					msg.setExpireMessages (player.getPlayerDescription ().getPlayerID ().equals (gpk.getCurrentPlayerID ()));
					
					if (msg.isExpireMessages ())
						msg.getMessage ().addAll (trans.getNewTurnMessage ());
					
					player.getConnection ().sendMessageToClient (msg);
					
					if (msg.isExpireMessages ())
						trans.getNewTurnMessage ().clear ();
				}
				else
					throw new MomException ("sendNewTurnMessages doesn't know how handle messageType of " + messageType);
			}
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
		if (onlyOnePlayerID == 0)
			log.info ("End phase for everyone turn " + mom.getGeneralPublicKnowledge ().getTurnNumber () + "...");
		else
			log.info ("End phase for turn " + mom.getGeneralPublicKnowledge ().getTurnNumber () + " - " + getMultiplayerSessionServerUtils ().findPlayerWithID
				(mom.getPlayers (), onlyOnePlayerID, "endPhase").getPlayerDescription ().getPlayerName () + "...");

		// Put mana into casting spells
		for (final PlayerServerDetails player : mom.getPlayers ())
			if ((onlyOnePlayerID == 0) || (player.getPlayerDescription ().getPlayerID () == onlyOnePlayerID))
				getSpellQueueing ().progressOverlandCasting (player, mom);
		
		// Progress multi-turn special orders
		progressMultiTurnSpecialOrders (mom.getGeneralServerKnowledge ().getTrueMap (), onlyOnePlayerID,
			mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting ());

		// Kick off the next turn
		log.info ("Kicking off next turn...");
		switch (mom.getSessionDescription ().getTurnSystem ())
		{
			case ONE_PLAYER_AT_A_TIME:
				switchToNextPlayer (mom, false);
				break;
				
			case SIMULTANEOUS:
				kickOffSimultaneousTurn (mom, false);
				break;

			default:
				throw new MomException ("endPhase encountered an unknown turn system " + mom.getSessionDescription ().getTurnSystem ());
		}
	}
	
	/**
	 * Purifying corruption and building roads can take several turns
	 *
	 * @param trueMap True terrain and unit details
	 * @param onlyOnePlayerID If zero, will process special orders for all players; if specified will process special orders only for the specified player
	 * @param players List of players in the session
	 * @param db Lookup lists built over the XML database
	 * @param fogOfWarSettings Fog of war settings from session description
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	final void progressMultiTurnSpecialOrders (final FogOfWarMemory trueMap, final int onlyOnePlayerID,
		final List<PlayerServerDetails> players, final CommonDatabase db, final FogOfWarSetting fogOfWarSettings)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		for (final MemoryUnit tu : trueMap.getUnit ())
			if ((tu.getStatus () == UnitStatusID.ALIVE) && ((tu.getSpecialOrder () == UnitSpecialOrder.PURIFY) || (tu.getSpecialOrder () == UnitSpecialOrder.BUILD_ROAD)) &&
				((onlyOnePlayerID == 0) || (tu.getOwningPlayerID () == onlyOnePlayerID)))
			{
				final ServerGridCellEx gc = (ServerGridCellEx) trueMap.getMap ().getPlane ().get
					(tu.getUnitLocation ().getZ ()).getRow ().get (tu.getUnitLocation ().getY ()).getCell ().get (tu.getUnitLocation ().getX ());
				
				final OverlandMapTerrainData terrainData = gc.getTerrainData ();
				
				switch (tu.getSpecialOrder ())
				{
					case PURIFY:
					{
						final Integer oldValue = terrainData.getCorrupted ();
						
						// Purify a little bit more
						if ((terrainData.getCorrupted () != null) && (terrainData.getCorrupted () > 0))
							terrainData.setCorrupted (terrainData.getCorrupted () - 1);
						
						// Corruption all gone?
						if ((terrainData.getCorrupted () == null) || (terrainData.getCorrupted () <= 0))
							terrainData.setCorrupted (null);
						
						// Send to anyone who can see it
						if (!CompareUtils.safeIntegerCompare (oldValue, terrainData.getCorrupted ()))
							getFogOfWarMidTurnChanges ().updatePlayerMemoryOfTerrain (trueMap.getMap (), players,
								(MapCoordinates3DEx) tu.getUnitLocation (), fogOfWarSettings.getTerrainAndNodeAuras ());
						
						// If corruption is all gone then cancel purify orders for all units here
						if (terrainData.getCorrupted () == null)
							for (final MemoryUnit tu2 : trueMap.getUnit ())
								if ((tu2.getStatus () == UnitStatusID.ALIVE) && (tu2.getSpecialOrder () == UnitSpecialOrder.PURIFY) && (tu2.getUnitLocation ().equals (tu.getUnitLocation ())))
								{
									tu2.setSpecialOrder (null);
									getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (tu2, trueMap.getMap (), players, db, fogOfWarSettings, null);
								}
						break;
					}
						
					case BUILD_ROAD:
					{
						final int oldValue = (gc.getRoadProductionSoFar () == null) ? 0 : gc.getRoadProductionSoFar ();
						
						// Lay a little more pavement - dwarven engineers construct 2 points per turn instead of 1
						final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (tu, null, null, null, players, trueMap, db);
						Integer skillValue = xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_BUILD_ROAD);
						if ((skillValue == null) || (skillValue < 1))
							skillValue = 1;
						
						gc.setRoadProductionSoFar (oldValue + skillValue);
						
						// Finished?
						final TileType tileType = db.findTileType (terrainData.getTileTypeID (), "progressMultiTurnSpecialOrders");
						if ((tileType.getProductionToBuildRoad () != null) && (gc.getRoadProductionSoFar () >= tileType.getProductionToBuildRoad ()))
						{
							gc.setRoadProductionSoFar (null);
							
							// If we're standing on a tower, then we build a road on both planes
							final Integer singlePlane = getMemoryGridCellUtils ().isTerrainTowerOfWizardry (terrainData) ? null : tu.getUnitLocation ().getZ ();
							for (final Plane plane : db.getPlane ())
								if ((singlePlane == null) || (singlePlane == plane.getPlaneNumber ()))
								{
									final OverlandMapTerrainData roadTerrainData = trueMap.getMap ().getPlane ().get
										(plane.getPlaneNumber ()).getRow ().get (tu.getUnitLocation ().getY ()).getCell ().get (tu.getUnitLocation ().getX ()).getTerrainData ();

									final String roadTileTypeID = ((plane.isRoadsEnchanted () != null) && (plane.isRoadsEnchanted ())) ?
											CommonDatabaseConstants.TILE_TYPE_ENCHANTED_ROAD : CommonDatabaseConstants.TILE_TYPE_NORMAL_ROAD;
									roadTerrainData.setRoadTileTypeID (roadTileTypeID);

									// Send to anyone who can see it
									getFogOfWarMidTurnChanges ().updatePlayerMemoryOfTerrain (trueMap.getMap (), players,
										new MapCoordinates3DEx (tu.getUnitLocation ().getX (), tu.getUnitLocation ().getY (), plane.getPlaneNumber ()),
										fogOfWarSettings.getTerrainAndNodeAuras ());
								}
							
							// Cancel road building orders for all units here
							for (final MemoryUnit tu2 : trueMap.getUnit ())
								if ((tu2.getStatus () == UnitStatusID.ALIVE) && (tu2.getSpecialOrder () == UnitSpecialOrder.BUILD_ROAD) && (tu2.getUnitLocation ().equals (tu.getUnitLocation ())))
								{
									tu2.setSpecialOrder (null);
									getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (tu2, trueMap.getMap (), players, db, fogOfWarSettings, null);
								}
						}
						
						break;
					}
						
					default:
						throw new MomException ("progressMultiTurnSpecialOrders does not know what to do with special order " + tu.getSpecialOrder ());
				}
			}
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
					getSimultaneousTurnsProcessing ().processSimultaneousTurnsMovement (mom);
				}				
				break;
				

			default:
				throw new MomException ("nextTurnButton encountered an unknown turn system " + mom.getSessionDescription ().getTurnSystem ());
		}
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
		if (onlyOnePlayerID == 0)
			log.info ("Continuing pending movements for everyone...");
		else
			log.info ("Continuing pending movements for " + getMultiplayerSessionServerUtils ().findPlayerWithID
				(mom.getPlayers (), onlyOnePlayerID, "continueMovement").getPlayerDescription ().getPlayerName () + "...");
		
		for (final PlayerServerDetails player : mom.getPlayers ())
			if ((onlyOnePlayerID == 0) || (player.getPlayerDescription ().getPlayerID () == onlyOnePlayerID))
			{
				// If a pending movement doesn't finish this turn then the moveUnitStack routine will recreate the pending movement object and add it to the end of the list
				// So we need to make sure we don't keep going through the list and end up processing pending moves that have only just been added
				// Simplest way to do it is just to copy the list and run down that instead
				final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
				final List<PendingMovement> moves = new ArrayList<PendingMovement> ();
				moves.addAll (priv.getPendingMovement ());
				priv.getPendingMovement ().clear ();
				
				for (final PendingMovement thisMove : moves)
				{
					// Find each of the units
					final List<ExpandedUnitDetails> unitStack = new ArrayList<ExpandedUnitDetails> ();
					for (final Integer unitURN : thisMove.getUnitURN ())
					{
						final MemoryUnit tu = getUnitUtils ().findUnitURN (unitURN, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), "continueMovement");
						final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (tu, null, null, null, mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
						unitStack.add (xu);
					}
					
					getFogOfWarMidTurnMultiChanges ().moveUnitStack (unitStack, player, false,
						(MapCoordinates3DEx) thisMove.getMoveFrom (), (MapCoordinates3DEx) thisMove.getMoveTo (), false, mom);
				}
			}
	}

	/**
	 * During the start phase, when resources are recalculated, this stores the power base of each wizard, which is public info to every player via the Historian screen.
	 * 
	 * @param onlyOnePlayerID If zero, will record power base for all players; if specified will record power base only for the specified player
	 * @param players List of players in the session
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	final void storePowerBaseHistory (final int onlyOnePlayerID, final List<PlayerServerDetails> players) throws JAXBException, XMLStreamException
	{
		final AddPowerBaseHistoryMessage msg = new AddPowerBaseHistoryMessage ();
		
		for (final PlayerServerDetails player : players)
			if ((onlyOnePlayerID == 0) || (player.getPlayerDescription ().getPlayerID () == onlyOnePlayerID))
			{
				final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
				
				// Ignore raiders and rampaging monsters
				if (PlayerKnowledgeUtils.isWizard (pub.getWizardID ()))
				{
					final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();					
					final int powerBase = getResourceValueUtils ().findAmountPerTurnForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER);
					
					// Store on server
					pub.getPowerBaseHistory ().add (powerBase);
					
					// Send to clients
					final PowerBaseHistoryPlayer item = new PowerBaseHistoryPlayer ();
					item.setPlayerID (player.getPlayerDescription ().getPlayerID ());
					item.setPowerBase (powerBase);
					msg.getPlayer ().add (item);
				}
			}		
		
		if (msg.getPlayer ().size () > 0)
			getMultiplayerSessionServerUtils ().sendMessageToAllClients (players, msg);
	}
	
	/**
	 * Checks to see if anyone has won the game, by every other wizard being defeated
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws PlayerNotFoundException If the requested playerID cannot be found
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void checkIfWonGame (final MomSessionVariables mom) throws PlayerNotFoundException, JAXBException, XMLStreamException
	{
		int aliveCount = 0;
		PlayerServerDetails aliveWizard = null;
		
		for (final PlayerServerDetails player : mom.getPlayers ())
		{
			final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
			if ((PlayerKnowledgeUtils.isWizard (pub.getWizardID ())) && (pub.getWizardState () != WizardState.DEFEATED))
			{
				aliveCount++;
				aliveWizard = player;
			}
		}
		
		if ((aliveCount == 1) && (aliveWizard != null) && (aliveWizard.getPlayerDescription ().isHuman ()))
		{
			// Tell them they won, then end the session by converting them to an AI player
			final PlayAnimationMessage msg = new PlayAnimationMessage ();
			msg.setAnimationID (AnimationID.WON);
			msg.setPlayerID (aliveWizard.getPlayerDescription ().getPlayerID ());
			aliveWizard.getConnection ().sendMessageToClient (msg);
			
			mom.updateHumanPlayerToAI (aliveWizard.getPlayerDescription ().getPlayerID ());
		}
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
	 * @return Unit calculations
	 */
	public final UnitCalculations getUnitCalculations ()
	{
		return unitCalculations;
	}

	/**
	 * @param calc Unit calculations
	 */
	public final void setUnitCalculations (final UnitCalculations calc)
	{
		unitCalculations = calc;
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
	 * @return Methods for updating true map + players' memory
	 */
	public final FogOfWarMidTurnMultiChanges getFogOfWarMidTurnMultiChanges ()
	{
		return fogOfWarMidTurnMultiChanges;
	}

	/**
	 * @param obj Methods for updating true map + players' memory
	 */
	public final void setFogOfWarMidTurnMultiChanges (final FogOfWarMidTurnMultiChanges obj)
	{
		fogOfWarMidTurnMultiChanges = obj;
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
	 * @return Number of save points to keep for each session
	 */
	public final int getSavePointKeepCount ()
	{
		return savePointKeepCount;
	}

	/**
	 * @param count Number of save points to keep for each session
	 */
	public final void setSavePointKeepCount (final int count)
	{
		savePointKeepCount = count;
	}
}