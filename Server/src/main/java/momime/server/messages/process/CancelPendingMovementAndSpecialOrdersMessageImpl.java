package momime.server.messages.process;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.messages.clienttoserver.v0_9_4.CancelPendingMovementAndSpecialOrdersMessage;
import momime.common.messages.servertoclient.v0_9_4.TextPopupMessage;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.server.MomSessionVariables;

import com.ndg.multiplayer.server.ProcessableClientToServerMessage;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;

/**
 * Client sends this to server if they decide to cancel a pending movement and/or special orders
 */
public final class CancelPendingMovementAndSpecialOrdersMessageImpl extends CancelPendingMovementAndSpecialOrdersMessage implements ProcessableClientToServerMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (CancelPendingMovementAndSpecialOrdersMessageImpl.class.getName ());

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
		log.entering (CancelPendingMovementAndSpecialOrdersMessageImpl.class.getName (), "process",
			new String [] {sender.getPlayerDescription ().getPlayerID ().toString (), new Integer (getUnitURN ()).toString ()});
		
		final MomSessionVariables mom = (MomSessionVariables) thread;

		// Find the unit and do some basic validation
		final String error;
		final MemoryUnit trueUnit = mom.getUnitUtils ().findUnitURN (getUnitURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ());
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
			log.warning (CancelPendingMovementAndSpecialOrdersMessageImpl.class.getName () + ".process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + error);

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (error);
			sender.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) sender.getTransientPlayerPrivateKnowledge ();
			mom.getPendingMovementUtils ().removeAnyPendingMovesThatIncludeUnit (trans.getPendingMovement (), getUnitURN ());
		
			// Clear special orders - if the unit was on a 'die' order, this means its going to start using upkeep again
			final boolean recalcProduction = mom.getUnitServerUtils ().doesUnitSpecialOrderResultInDeath (trueUnit.getSpecialOrder ());
			trueUnit.setSpecialOrder (null);
			if (recalcProduction)
				mom.getServerResourceCalculations ().recalculateGlobalProductionValues (sender.getPlayerDescription ().getPlayerID (), false, mom);
		}
		
		log.exiting (CancelPendingMovementAndSpecialOrdersMessageImpl.class.getName (), "process");
	}
}
