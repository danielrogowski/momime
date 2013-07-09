package momime.server.messages.process;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.clienttoserver.v0_9_4.RequestMoveOverlandUnitStackMessage;
import momime.common.messages.servertoclient.v0_9_4.TextPopupMessage;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.TurnSystem;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.server.MomSessionVariables;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

/**
 * Client sends this to server to request a unit stack be moved on the overland map
 */
public final class RequestMoveOverlandUnitStackMessageImpl extends RequestMoveOverlandUnitStackMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (RequestMoveOverlandUnitStackMessageImpl.class.getName ());
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException Iif there is a problem sending a reply back to the client
	 * @throws XMLStreamException If there is a problem sending a reply back to the client
	 * @throws IOException If there are any processing errors
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, IOException
	{
		log.entering (RequestMoveOverlandUnitStackMessageImpl.class.getName (), "process",
			new String [] {sender.getPlayerDescription ().getPlayerID ().toString (), getUnitURN ().toString (),
			(getMoveFrom () == null) ? "null" : getMoveFrom ().toString (), (getMoveTo () == null) ? "null" : getMoveTo ().toString ()});

		final MomSessionVariables mom = (MomSessionVariables) thread;

		// Process through all the units
		String error = null;
		if (getUnitURN ().size () == 0)
			error = "You must select at least one unit to move.";
		else if ((getMoveFrom ().getX () == getMoveTo ().getX ()) && (getMoveFrom ().getY () == getMoveTo ().getY ()))
			error = "You cannot move from a location back to the same location.";

		int doubleMovementRemaining = Integer.MAX_VALUE;
		final List<MemoryUnit> unitStack = new ArrayList<MemoryUnit> ();

		final Iterator<Integer> unitUrnIterator = getUnitURN ().iterator ();
		while ((error == null) && (unitUrnIterator.hasNext ()))
		{
			final Integer thisUnitURN = unitUrnIterator.next ();
			final MemoryUnit thisUnit = mom.getUnitUtils ().findUnitURN (thisUnitURN, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ());

			if (thisUnit == null)
				error = "Some of the units you are trying to move could not be found";
			else if (thisUnit.getOwningPlayerID () != sender.getPlayerDescription ().getPlayerID ())
				error = "Some of the units you are trying to move belong to another player";
			else if (thisUnit.getStatus () != UnitStatusID.ALIVE)
				error = "Some of the units you are trying to move are dead/dismissed.";
			else if (!thisUnit.getUnitLocation ().equals (getMoveFrom ()))
				error = "Some of the units you are trying to move are not at the starting location";
			else
			{
				unitStack.add (thisUnit);
				if (thisUnit.getDoubleOverlandMovesLeft () < doubleMovementRemaining)
					doubleMovementRemaining = thisUnit.getDoubleOverlandMovesLeft ();
			}
		}

		if (doubleMovementRemaining <= 0)
			error = "Some of the units you are trying to move have no movement remaining";

		if (error != null)
		{
			// Return error
			log.warning (RequestOverlandMovementDistancesMessageImpl.class.getName () + ".process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + error);

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (error);
			sender.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			// Proceed with move
			mom.getFogOfWarMidTurnChanges ().moveUnitStack (unitStack, sender, (OverlandMapCoordinatesEx) getMoveFrom (), (OverlandMapCoordinatesEx) getMoveTo (),
				(mom.getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS),
				mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getSessionDescription (), mom.getServerDB ());
		}

		log.exiting (RequestMoveOverlandUnitStackMessageImpl.class.getName (), "process");
	}
}
