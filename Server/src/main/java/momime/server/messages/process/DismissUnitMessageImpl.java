package momime.server.messages.process;

import java.io.IOException;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

import momime.common.database.UnitSpecialOrder;
import momime.common.messages.MemoryUnit;
import momime.common.messages.TurnSystem;
import momime.common.messages.clienttoserver.DismissUnitMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.fogofwar.KillUnitActionID;
import momime.server.utils.PlayerServerUtils;
import momime.server.utils.UnitServerUtils;

/**
 * Client can send this to request that a unit of theirs be killed off
 */
public final class DismissUnitMessageImpl extends DismissUnitMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (DismissUnitMessageImpl.class);
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;
	
	/** Resource calculations */
	private ServerResourceCalculations serverResourceCalculations;
	
	/** Player utils */
	private PlayerServerUtils playerServerUtils;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there was another kind of problem
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, IOException
	{
		final MomSessionVariables mom = (MomSessionVariables) thread;

		// Find the unit being dismissed
		final MemoryUnit trueUnit = getUnitUtils ().findUnitURN (getUnitURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ());

		// Validation
		String error = null;
		if (!getPlayerServerUtils ().isPlayerTurn (sender, mom.getGeneralPublicKnowledge (), mom.getSessionDescription ().getTurnSystem ()))
			error = "You can't dismiss units when it isn't your turn";
		
		else if (trueUnit == null)
			error = "Can't find the unit you tried to dismiss";
		
		else if (!sender.getPlayerDescription ().getPlayerID ().equals (trueUnit.getOwningPlayerID ()))
			error = "You tried to dimiss a unit that you don't own";

		else if (trueUnit.getCombatLocation () != null)
			error = "You can't dismiss units that are currently in combat";
		
		else
			error = null;
		
		if (error != null)
		{
			// Return error
			log.warn ("process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + error);

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (error);
			sender.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			// Do immediately or at end of turn?
			if (mom.getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS)
			{
				getUnitServerUtils ().setAndSendSpecialOrder (trueUnit, UnitSpecialOrder.DISMISS, sender, mom);

				// Unit probably had some maintenance (only need to do here - if actually killing unit by world updates then engine will already know it has to do this)
				getServerResourceCalculations ().recalculateGlobalProductionValues (sender.getPlayerDescription ().getPlayerID (), false, mom);
			}
			else
			{
				// Regular units are killed outright, heroes are killed outright on the clients but return to 'Generated' status on the server
				mom.getWorldUpdates ().killUnit (getUnitURN (), KillUnitActionID.DISMISS);
				mom.getWorldUpdates ().process (mom);
			}
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
}