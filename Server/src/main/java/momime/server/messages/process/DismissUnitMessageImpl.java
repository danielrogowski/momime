package momime.server.messages.process;

import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.clienttoserver.v0_9_5.DismissUnitMessage;
import momime.common.messages.servertoclient.v0_9_5.KillUnitActionID;
import momime.common.messages.servertoclient.v0_9_5.TextPopupMessage;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.messages.v0_9_5.TurnSystem;
import momime.common.messages.v0_9_5.UnitSpecialOrder;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.MomServerResourceCalculations;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.utils.UnitServerUtils;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Client can send this to request that a unit of theirs be killed off
 */
public final class DismissUnitMessageImpl extends DismissUnitMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (DismissUnitMessageImpl.class.getName ());
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;
	
	/** Resource calculations */
	private MomServerResourceCalculations serverResourceCalculations;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.entering (DismissUnitMessageImpl.class.getName (), "process",
			new Integer [] {sender.getPlayerDescription ().getPlayerID (), getUnitURN ()});

		final MomSessionVariables mom = (MomSessionVariables) thread;

		// Find the unit being dismissed
		final MemoryUnit trueUnit = getUnitUtils ().findUnitURN (getUnitURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ());

		// Validation
		String error = null;
		if (trueUnit == null)
			error = "Can't find the unit you tried to dismiss";
		
		else if (!sender.getPlayerDescription ().getPlayerID ().equals (trueUnit.getOwningPlayerID ()))
			error = "You tried to dimiss a unit that you don't own";
		
		else
			error = null;
		
		if (error != null)
		{
			// Return error
			log.warning (DismissUnitMessageImpl.class.getName () + ".process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + error);

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (error);
			sender.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			// Do immediately or at end of turn?
			if (mom.getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS)
				getUnitServerUtils ().setAndSendSpecialOrder (trueUnit, UnitSpecialOrder.DISMISS, sender);
			else
			{
				// Regular units are killed outright, heroes are killed outright on the clients but return to 'Generated' status on the server
				final KillUnitActionID action;
				if (mom.getServerDB ().findUnit (trueUnit.getUnitID (), "DismissUnitMessageImpl").getUnitMagicRealm ().equals (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
					action = KillUnitActionID.HERO_DIMISSED_VOLUNTARILY;
				else
					action = KillUnitActionID.FREE;
				
				getFogOfWarMidTurnChanges ().killUnitOnServerAndClients (trueUnit, action, null, mom.getGeneralServerKnowledge ().getTrueMap (),
					mom.getPlayers (), mom.getSessionDescription ().getFogOfWarSetting (), mom.getServerDB ());
			}
			
			// Unit probably had some maintenance
			getServerResourceCalculations ().recalculateGlobalProductionValues (sender.getPlayerDescription ().getPlayerID (), false, mom);
		}

		log.exiting (DismissUnitMessageImpl.class.getName (), "process");
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
	 * @return Resource calculations
	 */
	public final MomServerResourceCalculations getServerResourceCalculations ()
	{
		return serverResourceCalculations;
	}

	/**
	 * @param calc Resource calculations
	 */
	public final void setServerResourceCalculations (final MomServerResourceCalculations calc)
	{
		serverResourceCalculations = calc;
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
}
