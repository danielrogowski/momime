package momime.server.messages.process;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

import momime.common.messages.MemoryUnit;
import momime.common.messages.TurnSystem;
import momime.common.messages.UnitStatusID;
import momime.common.messages.clienttoserver.RequestMoveOverlandUnitStackMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.fogofwar.FogOfWarMidTurnMultiChanges;

/**
 * Client sends this to server to request a unit stack be moved on the overland map
 */
public final class RequestMoveOverlandUnitStackMessageImpl extends RequestMoveOverlandUnitStackMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (RequestMoveOverlandUnitStackMessageImpl.class);
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnMultiChanges fogOfWarMidTurnMultiChanges;
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
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
		final MomSessionVariables mom = (MomSessionVariables) thread;

		// Process through all the units
		String error = null;
		if (getUnitURN ().size () == 0)
			error = "You must select at least one unit to move.";
		else if (getMoveFrom ().equals (getMoveTo ()))
			error = "You cannot move from a location back to the same location.";

		int doubleMovementRemaining = Integer.MAX_VALUE;
		final List<ExpandedUnitDetails> unitStack = new ArrayList<ExpandedUnitDetails> ();

		final Iterator<Integer> unitUrnIterator = getUnitURN ().iterator ();
		while ((error == null) && (unitUrnIterator.hasNext ()))
		{
			final Integer thisUnitURN = unitUrnIterator.next ();
			final MemoryUnit thisUnit = getUnitUtils ().findUnitURN (thisUnitURN, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ());

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
				unitStack.add (getExpandUnitDetails ().expandUnitDetails (thisUnit, null, null, null,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ()));
				
				if (thisUnit.getDoubleOverlandMovesLeft () < doubleMovementRemaining)
					doubleMovementRemaining = thisUnit.getDoubleOverlandMovesLeft ();
			}
		}

		if (doubleMovementRemaining <= 0)
			error = "Some of the units you are trying to move have no movement remaining";

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
			// Proceed with move
			getFogOfWarMidTurnMultiChanges ().moveUnitStack (unitStack, sender, true, (MapCoordinates3DEx) getMoveFrom (), (MapCoordinates3DEx) getMoveTo (),
				(mom.getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS), mom);
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
	 * @return expandUnitDetails method
	 */
	public final ExpandUnitDetails getExpandUnitDetails ()
	{
		return expandUnitDetails;
	}

	/**
	 * @param e expandUnitDetails method
	 */
	public final void setExpandUnitDetails (final ExpandUnitDetails e)
	{
		expandUnitDetails = e;
	}
}