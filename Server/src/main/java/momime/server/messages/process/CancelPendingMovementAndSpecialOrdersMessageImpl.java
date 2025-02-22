package momime.server.messages.process;

import java.io.IOException;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.UnitStatusID;
import momime.common.messages.clienttoserver.CancelPendingMovementAndSpecialOrdersMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.utils.PendingMovementUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.utils.UnitServerUtils;

/**
 * Client sends this to server if they decide to cancel a pending movement and/or special orders
 */
public final class CancelPendingMovementAndSpecialOrdersMessageImpl extends CancelPendingMovementAndSpecialOrdersMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (CancelPendingMovementAndSpecialOrdersMessageImpl.class);

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;
	
	/** Pending movement utils */
	private PendingMovementUtils pendingMovementUtils;
	
	/** Resource calculations */
	private ServerResourceCalculations serverResourceCalculations;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the client
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the client
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, IOException
	{
		final MomSessionVariables mom = (MomSessionVariables) thread;

		// Find the unit and do some basic validation
		final String error;
		final MemoryUnit trueUnit = getUnitUtils ().findUnitURN (getUnitURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ());
		if (trueUnit == null)
			error = "Can't find the unit you are trying to cancel movement and/or special orders for";
		else if (trueUnit.getOwningPlayerID () != sender.getPlayerDescription ().getPlayerID ())
			error = "Tried to cancel movement and/or special orders for a unit that you don't own";
		else if (trueUnit.getStatus () != UnitStatusID.ALIVE)
			error = "Tried to cancel movement and/or special orders for a unit that isn't alive";
		else
			error = null;
		
		// All ok?
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
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) sender.getPersistentPlayerPrivateKnowledge ();
			getPendingMovementUtils ().removeAnyPendingMovesThatIncludeUnit (priv.getPendingMovement (), getUnitURN ());
		
			// Clear special orders - if the unit was on a 'die' order, this means its going to start using upkeep again
			final boolean recalcProduction = getUnitServerUtils ().doesUnitSpecialOrderResultInDeath (trueUnit.getSpecialOrder ());
			trueUnit.setSpecialOrder (null);
			if (recalcProduction)
				getServerResourceCalculations ().recalculateGlobalProductionValues (sender.getPlayerDescription ().getPlayerID (), false, mom);
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
	 * @return Pending movement utils
	 */
	public final PendingMovementUtils getPendingMovementUtils ()
	{
		return pendingMovementUtils;
	}

	/**
	 * @param utils Pending movement utils
	 */
	public final void setPendingMovementUtils (final PendingMovementUtils utils)
	{
		pendingMovementUtils = utils;
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
}