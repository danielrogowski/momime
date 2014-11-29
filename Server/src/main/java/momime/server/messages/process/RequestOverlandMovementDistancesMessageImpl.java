package momime.server.messages.process;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.UnitStatusID;
import momime.common.messages.clienttoserver.RequestOverlandMovementDistancesMessage;
import momime.common.messages.servertoclient.MapAreaOfOverlandMoveType;
import momime.common.messages.servertoclient.MapRowOfOverlandMoveType;
import momime.common.messages.servertoclient.MapVolumeOfOverlandMoveType;
import momime.common.messages.servertoclient.OverlandMoveTypeID;
import momime.common.messages.servertoclient.OverlandMovementTypesMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerUnitCalculations;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

/**
 * Client sends this to server to request coordinates where this unit stack can move to
 */
public final class RequestOverlandMovementDistancesMessageImpl extends RequestOverlandMovementDistancesMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (RequestOverlandMovementDistancesMessageImpl.class);

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Server-only unit calculations */
	private ServerUnitCalculations serverUnitCalculations;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, RecordNotFoundException
	{
		log.trace ("Entering process: (" + getMoveFrom ().getX () + ", " + getMoveFrom ().getY () + ", " + getMoveFrom ().getZ () +
			"), Player ID " + sender.getPlayerDescription ().getPlayerID ());

		final MomSessionVariables mom = (MomSessionVariables) thread;
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) sender.getPersistentPlayerPrivateKnowledge ();

		// Process through all the units
		String error = null;
		if (getUnitURN ().size () == 0)
			error = "You must select at least one unit to move.";

		int doubleMovementRemaining = Integer.MAX_VALUE;
		final List<MemoryUnit> unitStack = new ArrayList<MemoryUnit> ();

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
			log.warn ("process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + error);

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (error);
			sender.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			// Proceed with calculation
			final int [] [] [] doubleMovementDistances			= new int [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getMapSize ().getHeight ()] [mom.getSessionDescription ().getMapSize ().getWidth ()];
			final int [] [] [] movementDirections					= new int [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getMapSize ().getHeight ()] [mom.getSessionDescription ().getMapSize ().getWidth ()];
			final boolean [] [] [] canMoveToInOneTurn			= new boolean [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getMapSize ().getHeight ()] [mom.getSessionDescription ().getMapSize ().getWidth ()];
			final boolean [] [] [] movingHereResultsInAttack	= new boolean [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getMapSize ().getHeight ()] [mom.getSessionDescription ().getMapSize ().getWidth ()];

			getServerUnitCalculations ().calculateOverlandMovementDistances (getMoveFrom ().getX (), getMoveFrom ().getY (), getMoveFrom ().getZ (),
				sender.getPlayerDescription ().getPlayerID (), priv.getFogOfWarMemory (),
				unitStack, doubleMovementRemaining, doubleMovementDistances, movementDirections, canMoveToInOneTurn, movingHereResultsInAttack,
				mom.getSessionDescription (), mom.getServerDB ());

			// The client only needs to know which areas they can reach, and which they can reach in one turn - not the actual distances or any of the other stuff that gets generated
			final MapVolumeOfOverlandMoveType movementTypesVolume = new MapVolumeOfOverlandMoveType ();
			for (int plane = 0; plane < mom.getServerDB ().getPlane ().size (); plane++)
			{
				final MapAreaOfOverlandMoveType movementTypesPlane = new MapAreaOfOverlandMoveType ();
				for (int y = 0; y < mom.getSessionDescription ().getMapSize ().getHeight (); y++)
				{
					final MapRowOfOverlandMoveType movementTypesRow = new MapRowOfOverlandMoveType ();
					for (int x = 0; x < mom.getSessionDescription ().getMapSize ().getWidth (); x++)
					{
						final OverlandMoveTypeID movementType;
						if (doubleMovementDistances [plane] [y] [x] < 0)
							movementType = OverlandMoveTypeID.CANNOT_MOVE_HERE;
						else if (canMoveToInOneTurn [plane] [y] [x])
							movementType = OverlandMoveTypeID.MOVE_IN_ONE_TURN;
						else
							movementType = OverlandMoveTypeID.MOVE_IN_MULTIPLE_TURNS;

						movementTypesRow.getCell ().add (movementType);
					}

					movementTypesPlane.getRow ().add (movementTypesRow);
				}

				movementTypesVolume.getPlane ().add (movementTypesPlane);
			}

			final OverlandMovementTypesMessage msg = new OverlandMovementTypesMessage ();
			msg.setMovementTypes (movementTypesVolume);
			sender.getConnection ().sendMessageToClient (msg);
		}

		log.trace ("Exiting process");
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
	 * @return Server-only unit calculations
	 */
	public final ServerUnitCalculations getServerUnitCalculations ()
	{
		return serverUnitCalculations;
	}

	/**
	 * @param calc Server-only unit calculations
	 */
	public final void setServerUnitCalculations (final ServerUnitCalculations calc)
	{
		serverUnitCalculations = calc;
	}
}