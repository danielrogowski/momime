package momime.client.dummy;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.database.v0_9_4.AvailableDatabase;
import momime.common.messages.CoordinatesUtils;
import momime.common.messages.servertoclient.v0_9_4.AddBuildingMessage;
import momime.common.messages.servertoclient.v0_9_4.AddCombatAreaEffectMessage;
import momime.common.messages.servertoclient.v0_9_4.AddMaintainedSpellMessage;
import momime.common.messages.servertoclient.v0_9_4.AddUnitMessage;
import momime.common.messages.servertoclient.v0_9_4.CancelCombatAreaEffectMessage;
import momime.common.messages.servertoclient.v0_9_4.ChooseInitialSpellsNowMessage;
import momime.common.messages.servertoclient.v0_9_4.ChooseInitialSpellsNowRank;
import momime.common.messages.servertoclient.v0_9_4.ChooseYourRaceNowMessage;
import momime.common.messages.servertoclient.v0_9_4.ChosenCustomPhotoMessage;
import momime.common.messages.servertoclient.v0_9_4.ChosenStandardPhotoMessage;
import momime.common.messages.servertoclient.v0_9_4.ChosenWizardMessage;
import momime.common.messages.servertoclient.v0_9_4.DestroyBuildingMessage;
import momime.common.messages.servertoclient.v0_9_4.FogOfWarVisibleAreaChangedMessage;
import momime.common.messages.servertoclient.v0_9_4.FullSpellListMessage;
import momime.common.messages.servertoclient.v0_9_4.KillUnitMessage;
import momime.common.messages.servertoclient.v0_9_4.NewGameDatabaseMessage;
import momime.common.messages.servertoclient.v0_9_4.ReplacePicksMessage;
import momime.common.messages.servertoclient.v0_9_4.StartGameMessage;
import momime.common.messages.servertoclient.v0_9_4.StartGameProgressMessage;
import momime.common.messages.servertoclient.v0_9_4.SwitchOffMaintainedSpellMessage;
import momime.common.messages.servertoclient.v0_9_4.TextPopupMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateCityMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateGlobalEconomyMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateNodeLairTowerUnitIDMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateTerrainMessage;
import momime.common.messages.servertoclient.v0_9_4.YourPhotoIsOkMessage;
import momime.common.messages.servertoclient.v0_9_4.YourRaceIsOkMessage;
import momime.common.messages.v0_9_4.MomGeneralPublicKnowledge;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.MomResourceValue;
import momime.common.messages.v0_9_4.PlayerPick;
import momime.common.messages.v0_9_4.SpellResearchStatus;
import momime.common.messages.v0_9_4.SpellResearchStatusID;

import com.ndg.multiplayer.base.MultiplayerBaseClientThread;
import com.ndg.multiplayer.base.ServerToClientMessage;
import com.ndg.multiplayer.sessionbase.AdditionalPlayerJoined;
import com.ndg.multiplayer.sessionbase.CreateAccountFailed;
import com.ndg.multiplayer.sessionbase.CreateAccountSuccessful;
import com.ndg.multiplayer.sessionbase.JoinFailed;
import com.ndg.multiplayer.sessionbase.JoinSuccessful;
import com.ndg.multiplayer.sessionbase.KickedByAnotherLogin;
import com.ndg.multiplayer.sessionbase.LoginFailed;
import com.ndg.multiplayer.sessionbase.LoginSuccessful;
import com.ndg.multiplayer.sessionbase.LogoutFailed;
import com.ndg.multiplayer.sessionbase.LogoutSuccessful;
import com.ndg.multiplayer.sessionbase.PlayerCombinedPublicInfo;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.multiplayer.sessionbase.RequestSessionListFailed;
import com.ndg.multiplayer.sessionbase.RequestSessionListSuccessful;
import com.ndg.multiplayer.sessionbase.SessionAndPlayerDescriptions;

/**
 * Demo showing how to subclass MultiplayerBaseClientThread
 */
public final class DummyMomClientThread extends MultiplayerBaseClientThread
{
	/** Link to demo client, so we can trigger methods on it */
	private final DummyMomClient client;

	/**
	 * Creates a new thread for handling requests from the server
	 * @param aSocket Socket used to communicate with the server
	 * @param aReadyForMessagesMonitor Thread lock object to notify once thread is ready to send and receive messages; null if no start notification is requested
	 * @param aClient Link to demo client, so we can trigger methods on it
	 * @throws JAXBException If there is a problem creating the contexts
	 */
	public DummyMomClientThread (final Socket aSocket, final Object aReadyForMessagesMonitor, final DummyMomClient aClient)
		throws JAXBException
	{
		// Pulling the JAXB contexts straight out of the spring application context by naming the beans is really ugly but
		// it'll do for now until I put some more work into how to write client side multiplayer apps using spring
		// and include a demo client in NdgMultiplayerSessionDemo
		super (aSocket, (JAXBContext) aClient.getApplicationContext ().getBean ("clientToServerJaxbContext"),
			(JAXBContext) aClient.getApplicationContext ().getBean ("serverToClientJaxbContext"),
			null, aReadyForMessagesMonitor);

		client = aClient;
	}

	/**
	 * Processes XML message received from server
	 * @param msg Message received from serverthat needs to be processed
	 * @throws IOException If we are unable to process the message
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 */
	@Override
	protected final void processMessageFromServer (final ServerToClientMessage msg)
		throws IOException, JAXBException, XMLStreamException
	{
		// See if it is a message that we know how to process
		if (msg instanceof CreateAccountSuccessful)
		{
			final CreateAccountSuccessful account = (CreateAccountSuccessful) msg;
			client.addToTextArea ("Account created, you were allocated player ID " + account.getPlayerID ());
		}
		else if (msg instanceof CreateAccountFailed)
		{
			final CreateAccountFailed account = (CreateAccountFailed) msg;
			client.addToTextArea ("Account creation failed for reason " + account.getReason ());
		}
		else if (msg instanceof LoginSuccessful)
		{
			final LoginSuccessful login = (LoginSuccessful) msg;
			client.setPlayerID (login.getPlayerID ());

			client.addToTextArea ("Logged in successfully as player ID " + login.getPlayerID () + ", you are a member of the following sessions that you could rejoin:");
			outputSessionList (login.getSession ());
			client.addToTextArea ("End of session list");
			client.enableOrDisableButtons ();
		}
		else if (msg instanceof LoginFailed)
		{
			final LoginFailed login = (LoginFailed) msg;
			client.addToTextArea ("Login failed for reason " + login.getReason ());
		}
		else if (msg instanceof LogoutSuccessful)
		{
			client.addToTextArea ("Logged out successfully");
			client.setPlayerID (0);
			client.enableOrDisableButtons ();
		}
		else if (msg instanceof LogoutFailed)
		{
			final LogoutFailed logout = (LogoutFailed) msg;
			client.addToTextArea ("Logout failed for reason " + logout.getReason ());
		}
		else if (msg instanceof KickedByAnotherLogin)
		{
			client.addToTextArea ("We were kicked because another connection logged on with our player ID");
			client.setPlayerID (0);
			client.enableOrDisableButtons ();
		}
		else if (msg instanceof RequestSessionListSuccessful)
		{
			final RequestSessionListSuccessful sessions = (RequestSessionListSuccessful) msg;
			client.addToTextArea ("Received session list:");
			outputSessionList (sessions.getSession ());
			client.addToTextArea ("End of session list");
		}
		else if (msg instanceof RequestSessionListFailed)
		{
			final RequestSessionListFailed sessions = (RequestSessionListFailed) msg;
			client.addToTextArea ("Request session list failed for reason " + sessions.getReason ());
		}
		else if (msg instanceof JoinFailed)
		{
			final JoinFailed join = (JoinFailed) msg;
			client.addToTextArea ("New/Join session failed for reason " + join.getReason ());
		}
		else if (msg instanceof AdditionalPlayerJoined)
		{
			final AdditionalPlayerJoined additional = (AdditionalPlayerJoined) msg;
			client.addToTextArea ("Additional player joined our session: " + additional.getPlayerDescription ().getPlayerName ());
		}
		else if (msg instanceof NewGameDatabaseMessage)
		{
			final NewGameDatabaseMessage newGameDatabaseMessage = (NewGameDatabaseMessage) msg;
			final AvailableDatabase db = newGameDatabaseMessage.getNewGameDatabase ().getMomimeXmlDatabase ().get (0);
			client.addToTextArea ("New game database received containing \"" + db.getDbName () + "\"");
			client.setNewGameDatabase (db);
		}
		else if (msg instanceof JoinSuccessful)
		{
			final JoinSuccessful join = (JoinSuccessful) msg;

			String playerList = null;
			for (final PlayerCombinedPublicInfo playerPublicInfo : join.getPlayer ())
			{
				if (playerList == null)
					playerList = playerPublicInfo.getPlayerDescription ().getPlayerName ();
				else
					playerList = playerList + ", " + playerPublicInfo.getPlayerDescription ().getPlayerName ();

				final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) playerPublicInfo.getPersistentPlayerPublicKnowledge ();

				playerList = playerList + "(wizard " + ppk.getWizardID () + ", photo " + ppk.getStandardPhotoID () + ", has " + ppk.getPick ().size () + " picks)";
			}

			client.addToTextArea ("Successfully joined session " + join.getSessionDescription ().getSessionID ());
			client.addToTextArea ("  Persistent player private knowledge: " + join.getPersistentPlayerPrivateKnowledge ());
			client.addToTextArea ("  Transient player private knowledge: " + join.getTransientPlayerPrivateKnowledge ());
			client.addToTextArea ("  General public knowledge: " + join.getGeneralPublicKnowledge ());
			client.addToTextArea ("  Players now in session: " + playerList);

			final MomGeneralPublicKnowledge gpk = (MomGeneralPublicKnowledge) join.getGeneralPublicKnowledge ();
			client.addToTextArea ("  Client DB: " + gpk.getClientDatabase ());
		}
		else if (msg instanceof TextPopupMessage)
		{
			client.addToTextArea ("Popup msg: " + ((TextPopupMessage) msg).getText ());
		}
		else if (msg instanceof ReplacePicksMessage)
		{
			final ReplacePicksMessage replacePicks = (ReplacePicksMessage) msg;
			String picks = "";
			for (final PlayerPick pick : replacePicks.getPick ())
			{
				if (!picks.equals (""))
					picks = picks + ", ";

				picks = picks + pick.getQuantity () + "x" + pick.getPickID ();
			}

			client.addToTextArea ("Replaced player " + replacePicks.getPlayerID () + "'s pick by: " + picks);
		}
		else if (msg instanceof ChooseInitialSpellsNowMessage)
		{
			final ChooseInitialSpellsNowMessage chooseSpells = (ChooseInitialSpellsNowMessage) msg;
			String output = "Choose initial spells for realm " + chooseSpells.getMagicRealmID () + ":";
			for (final ChooseInitialSpellsNowRank rank : chooseSpells.getSpellRank ())
				output = output + " " + rank.getFreeSpellCount () + "x" + rank.getSpellRankID ();
			client.addToTextArea (output);
		}
		else if (msg instanceof ChosenWizardMessage)
		{
			final ChosenWizardMessage chosen = (ChosenWizardMessage) msg;
			client.addToTextArea ("Player " + chosen.getPlayerID () + " has chosen wizard " + chosen.getWizardID ());
		}
		else if (msg instanceof YourPhotoIsOkMessage)
		{
			client.addToTextArea ("Your photo is OK");
		}
		else if (msg instanceof ChooseYourRaceNowMessage)
		{
			client.addToTextArea ("Choose your race now");
		}
		else if (msg instanceof YourRaceIsOkMessage)
		{
			client.addToTextArea ("Your race is OK");
		}
		else if (msg instanceof StartGameProgressMessage)
		{
			client.addToTextArea ("Start game progress: " + ((StartGameProgressMessage) msg).getStage ());
		}
		else if (msg instanceof FullSpellListMessage)
		{
			final FullSpellListMessage spellsMsg = (FullSpellListMessage) msg;
			String spells = "";
			for (final SpellResearchStatus spell : spellsMsg.getSpellResearchStatus ())
				if (!spell.getStatus ().equals (SpellResearchStatusID.UNAVAILABLE))
				{
					if (!spells.equals (""))
						spells = spells + ", ";

					spells = spells + spell.getSpellID () + "=" + spell.getStatus ();
				}

			client.addToTextArea ("Full spell status list: " + spells);
		}
		else if (msg instanceof ChosenStandardPhotoMessage)
		{
			final ChosenStandardPhotoMessage chosen = (ChosenStandardPhotoMessage) msg;
			client.addToTextArea ("Player " + chosen.getPlayerID () + " chose standard photo " + chosen.getPhotoID ());
		}
		else if (msg instanceof ChosenCustomPhotoMessage)
		{
			final ChosenCustomPhotoMessage chosen = (ChosenCustomPhotoMessage) msg;
			client.addToTextArea ("Player " + chosen.getPlayerID () + " chose custom photo of size " + chosen.getNdgBmpImage ().length + " bytes and flag colour " + chosen.getFlagColour ());
		}
		else if (msg instanceof UpdateTerrainMessage)
		{
			final UpdateTerrainMessage terrain = (UpdateTerrainMessage) msg;
			client.addToTextArea ("Knowledge of terrain at " + CoordinatesUtils.overlandMapCoordinatesToString (terrain.getData ().getMapLocation ()) + " updated");
		}
		else if (msg instanceof UpdateCityMessage)
		{
			final UpdateCityMessage city = (UpdateCityMessage) msg;
			client.addToTextArea ("Knowledge of city at " + CoordinatesUtils.overlandMapCoordinatesToString (city.getData ().getMapLocation ()) + " updated");
		}
		else if (msg instanceof AddBuildingMessage)
		{
			final AddBuildingMessage building = (AddBuildingMessage) msg;
			client.addToTextArea ("Building " + building.getData ().getFirstBuildingID () + " added to city at " + CoordinatesUtils.overlandMapCoordinatesToString (building.getData ().getCityLocation ()) + " updated");
		}
		else if (msg instanceof DestroyBuildingMessage)
		{
			final DestroyBuildingMessage building = (DestroyBuildingMessage) msg;
			client.addToTextArea ("Building " + building.getData ().getBuildingID () + " destroyed or gone out of sight from city at " + CoordinatesUtils.overlandMapCoordinatesToString (building.getData ().getCityLocation ()) + " updated");
		}
		else if (msg instanceof AddUnitMessage)
		{
			final AddUnitMessage unit = (AddUnitMessage) msg;
			client.addToTextArea ("Unit" + unit.getData ().getUnitURN () + " added of type " + unit.getData ().getUnitID ());
		}
		else if (msg instanceof KillUnitMessage)
		{
			final KillUnitMessage unit = (KillUnitMessage) msg;
			client.addToTextArea ("Unit" + unit.getData ().getUnitURN () + " killed or gone out of sight");
		}
		else if (msg instanceof UpdateNodeLairTowerUnitIDMessage)
		{
			final UpdateNodeLairTowerUnitIDMessage update = (UpdateNodeLairTowerUnitIDMessage) msg;
			client.addToTextArea ("Node/Lair/Tower at " + CoordinatesUtils.overlandMapCoordinatesToString (update.getData ().getNodeLairTowerLocation ()) + " known to contain unit ID " + update.getData ().getMonsterUnitID ());
		}
		else if (msg instanceof AddMaintainedSpellMessage)
		{
			final AddMaintainedSpellMessage spell = (AddMaintainedSpellMessage) msg;
			client.addToTextArea ("Spell of type " + spell.getData ().getSpellID () + " added");
		}
		else if (msg instanceof SwitchOffMaintainedSpellMessage)
		{
			final SwitchOffMaintainedSpellMessage spell = (SwitchOffMaintainedSpellMessage) msg;
			client.addToTextArea ("Spell of type " + spell.getData ().getSpellID () + " switched off or gone out of sight");
		}
		else if (msg instanceof AddCombatAreaEffectMessage)
		{
			final AddCombatAreaEffectMessage cae = (AddCombatAreaEffectMessage) msg;
			client.addToTextArea ("CAE of type " + cae.getData ().getCombatAreaEffectID () + " added");
		}
		else if (msg instanceof CancelCombatAreaEffectMessage)
		{
			final CancelCombatAreaEffectMessage cae = (CancelCombatAreaEffectMessage) msg;
			client.addToTextArea ("CAE of type " + cae.getData ().getCombatAreaEffectID () + " cancelled off or gone out of sight");
		}
		else if (msg instanceof FogOfWarVisibleAreaChangedMessage)
		{
			final FogOfWarVisibleAreaChangedMessage fow = (FogOfWarVisibleAreaChangedMessage) msg;
			client.addToTextArea ("Fog of War visible area changed by \"" + fow.getTriggeredFrom () + "\":");
			if (fow.getTerrainUpdate ().size () > 0)						client.addToTextArea ("  " + fow.getTerrainUpdate ().size () +						" terrain changes");
			if (fow.getCityUpdate ().size () > 0)								client.addToTextArea ("  " + fow.getCityUpdate ().size () +								" city changes");
			if (fow.getAddBuilding ().size () > 0)							client.addToTextArea ("  " + fow.getAddBuilding ().size () +							" buildings added");
			if (fow.getDestroyBuilding ().size () > 0)						client.addToTextArea ("  " + fow.getDestroyBuilding ().size () +						" buildings destroyed");
			if (fow.getAddUnit ().size () > 0)									client.addToTextArea ("  " + fow.getAddUnit ().size () +									" units added");
			if (fow.getKillUnit ().size () > 0)									client.addToTextArea ("  " + fow.getKillUnit ().size () +									" units killed");
			if (fow.getUpdateNodeLairTowerUnitID ().size () > 0)	client.addToTextArea ("  " + fow.getUpdateNodeLairTowerUnitID ().size () +	" node/lair/tower scouted unit ID changes");
			if (fow.getAddMaintainedSpell ().size () > 0)					client.addToTextArea ("  " + fow.getAddMaintainedSpell ().size () +				" spells added");
			if (fow.getSwitchOffMaintainedSpell ().size () > 0)		client.addToTextArea ("  " + fow.getSwitchOffMaintainedSpell ().size () +		" spells switched off");
			if (fow.getAddCombatAreaEffect ().size () > 0)				client.addToTextArea ("  " + fow.getAddCombatAreaEffect ().size () +				" combat area effects added");
			if (fow.getCancelCombaAreaEffect ().size () > 0)			client.addToTextArea ("  " + fow.getCancelCombaAreaEffect ().size () +			" combat area effects cancelled");
			if (fow.getFogOfWarUpdate ().size () > 0)					client.addToTextArea ("  " + fow.getFogOfWarUpdate ().size () +					" visible area changes");
			client.addToTextArea ("End of Fog of War visible area changes");
		}
		else if (msg instanceof UpdateGlobalEconomyMessage)
		{
			final UpdateGlobalEconomyMessage econ = (UpdateGlobalEconomyMessage) msg;
			client.addToTextArea ("Received updated resource values:");
			for (final MomResourceValue resource : econ.getResourceValue ())
				client.addToTextArea ("  " + resource.getProductionTypeID () + " producing " + resource.getAmountPerTurn () + " per turn, have " + resource.getAmountStored () + " stored");
			client.addToTextArea ("End of list of resource values");
		}
		else if (msg instanceof StartGameMessage)
		{
			client.addToTextArea ("Starting game (switches client to map view)");
		}
		else
			throw new IOException ("DemoSessionClientThread received a message of type " + msg.getClass ().getName () + " which it does not know how to process");
	}

	/**
	 * @param sessionList Session list to output to client UI
	 */
	private final void outputSessionList (final List<SessionAndPlayerDescriptions> sessionList)
	{
		for (final SessionAndPlayerDescriptions spd : sessionList)
		{
			String playerList = null;
			for (final PlayerDescription pd : spd.getPlayer ())
			{
				if (playerList == null)
					playerList = pd.getPlayerName ();
				else
					playerList = playerList + ", " + pd.getPlayerName ();
			}

			if (playerList == null)
				client.addToTextArea ("  " + spd.getSessionDescription ().getSessionID () + ": \"" + spd.getSessionDescription ().getSessionName () + "\" containing no players");
			else
				client.addToTextArea ("  " + spd.getSessionDescription ().getSessionID () + ": \"" + spd.getSessionDescription ().getSessionName () + "\" containing: " + playerList);
		}
	}

	/**
	 * Handle disconnections
	 * @param e Exception that caused the disconnection, or null if the disconnection was controlled (sent closing conversation tag)
	 */
	@Override
	protected final void disconnected (final Exception e)
	{
		super.disconnected (e);

		if (e == null)
			client.addToTextArea ("Server disconnected with no exception");
		else
			client.addToTextArea ("Server disconnected with exception: " + e.getMessage ());

		client.connection = null;
		client.enableOrDisableButtons ();
	}
}
