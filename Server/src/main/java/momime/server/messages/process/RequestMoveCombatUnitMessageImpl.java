package momime.server.messages.process;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

import jakarta.xml.bind.JAXBException;
import momime.common.messages.CombatMapSize;
import momime.common.messages.MemoryUnit;
import momime.common.messages.UnitStatusID;
import momime.common.messages.clienttoserver.RequestMoveCombatUnitMessage;
import momime.common.messages.servertoclient.MoveUnitInCombatReason;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.movement.CombatMovementType;
import momime.common.movement.UnitMovement;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.process.CombatProcessing;

/**
 * Client sends this to server to request a unit be moved in combat
 * 
 * This is a fair bit simpler than overland movement because
 * 1) We only ever move 1 unit at a time (no stacks)
 * 2) We always move straight away (no simultaneous movement)
 * 3) You can only ever move as far as you can reach in 1 turn (no pending movements)
 */
public final class RequestMoveCombatUnitMessageImpl extends RequestMoveCombatUnitMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (RequestMoveCombatUnitMessageImpl.class);

	/** Unit utils */
	private UnitUtils unitUtils;

	/** Combat processing */
	private CombatProcessing combatProcessing;
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/** Methods dealing with unit movement */
	private UnitMovement unitMovement;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, IOException
	{
		final MomSessionVariables mom = (MomSessionVariables) thread;

		// Find the unit being moved
		final MemoryUnit tu = getUnitUtils ().findUnitURN (getUnitURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ());

		// Check we're allowed to move the unit
		String error = null;
		if (tu == null)
			error = "Cannot find the unit you are trying to move";
		else if ((tu.getCombatLocation () == null) || (tu.getCombatPosition () == null) || (tu.getCombatHeading () == null) || (tu.getCombatSide () == null))
			error = "The unit you are trying to move is not in a combat";
		else if (tu.getStatus () != UnitStatusID.ALIVE)
			error = "The unit you are trying to move is dead/dismissed";
		else if ((tu.getDoubleCombatMovesLeft () == null) || (tu.getDoubleCombatMovesLeft () <= 0))
			error = "The unit you are trying to move has no movement remaining";
		else if (getMoveTo ().equals (tu.getCombatPosition ()))
			error = "You cannot move from a location back to the same location";
		
		// Calculate distances to every point on the map
		final CombatMapSize combatMapSize = mom.getSessionDescription ().getCombatMapSize ();
		
		final int [] [] movementDirections = new int [combatMapSize.getHeight ()] [combatMapSize.getWidth ()];
		final CombatMovementType [] [] movementTypes = new CombatMovementType [combatMapSize.getHeight ()] [combatMapSize.getWidth ()];
		
		ExpandedUnitDetails xu = null;
		if (error == null)
		{
			final ServerGridCellEx tc = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
				(tu.getCombatLocation ().getZ ()).getRow ().get (tu.getCombatLocation ().getY ()).getCell ().get (tu.getCombatLocation ().getX ());
			
			if (!sender.getPlayerDescription ().getPlayerID ().equals (tc.getCombatCurrentPlayerID ()))
				error = "You cannot move units in combat when it isn't your turn";
			else
			{
				xu = getExpandUnitDetails ().expandUnitDetails (tu, null, null, null,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());

				if (xu.getControllingPlayerID () != sender.getPlayerDescription ().getPlayerID ())
					error = "The unit you are trying to move is controlled by another player";
				else
				{
					final int [] [] doubleMovementDistances = new int [combatMapSize.getHeight ()] [combatMapSize.getWidth ()];
					
					getUnitMovement ().calculateCombatMovementDistances (doubleMovementDistances, movementDirections, movementTypes,
						xu, mom.getGeneralServerKnowledge ().getTrueMap (), tc.getCombatMap (), combatMapSize, mom.getPlayers (), mom.getServerDB ());
					
					// Can we reach where we're trying to go?
					if (movementTypes [getMoveTo ().getY ()] [getMoveTo ().getX ()] == CombatMovementType.CANNOT_MOVE)
						error = "The unit you are trying to move cannot reach this location";
				}
			}
		}

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
			getCombatProcessing ().okToMoveUnitInCombat (xu, (MapCoordinates2DEx) getMoveTo (), MoveUnitInCombatReason.MANUAL,
				movementDirections, movementTypes, mom);
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
	 * @return Combat processing
	 */
	public final CombatProcessing getCombatProcessing ()
	{
		return combatProcessing;
	}

	/**
	 * @param proc Combat processing
	 */
	public final void setCombatProcessing (final CombatProcessing proc)
	{
		combatProcessing = proc;
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

	/**
	 * @return Methods dealing with unit movement
	 */
	public final UnitMovement getUnitMovement ()
	{
		return unitMovement;
	}

	/**
	 * @param u Methods dealing with unit movement
	 */
	public final void setUnitMovement (final UnitMovement u)
	{
		unitMovement = u;
	}
}