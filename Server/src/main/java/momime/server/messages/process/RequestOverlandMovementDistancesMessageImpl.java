package momime.server.messages.process;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.clienttoserver.v0_9_4.RequestOverlandMovementDistancesMessage;
import momime.common.messages.servertoclient.v0_9_4.MapAreaOfOverlandMoveType;
import momime.common.messages.servertoclient.v0_9_4.MapRowOfOverlandMoveType;
import momime.common.messages.servertoclient.v0_9_4.MapVolumeOfOverlandMoveType;
import momime.common.messages.servertoclient.v0_9_4.OverlandMoveTypeID;
import momime.common.messages.servertoclient.v0_9_4.OverlandMovementTypesMessage;
import momime.common.messages.servertoclient.v0_9_4.TextPopupMessage;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MoveResultsInAttackTypeID;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.server.MomSessionVariables;

import com.ndg.multiplayer.server.ProcessableClientToServerMessage;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;

/**
 * Client sends this to server to request coordinates where this unit stack can move to
 */
public final class RequestOverlandMovementDistancesMessageImpl extends RequestOverlandMovementDistancesMessage implements ProcessableClientToServerMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (RequestOverlandMovementDistancesMessageImpl.class.getName ());

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
		log.entering (RequestOverlandMovementDistancesMessageImpl.class.getName (), "process", new Integer []
			{getMoveFrom ().getX (), getMoveFrom ().getY (), getMoveFrom ().getPlane (), sender.getPlayerDescription ().getPlayerID ()});

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
			// Proceed with calculation
			final int [] [] [] doubleMovementDistances										= new int [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getMapSize ().getHeight ()] [mom.getSessionDescription ().getMapSize ().getWidth ()];
			final int [] [] [] movementDirections												= new int [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getMapSize ().getHeight ()] [mom.getSessionDescription ().getMapSize ().getWidth ()];
			final boolean [] [] [] canMoveToInOneTurn										= new boolean [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getMapSize ().getHeight ()] [mom.getSessionDescription ().getMapSize ().getWidth ()];
			final MoveResultsInAttackTypeID [] [] [] movingHereResultsInAttack	= new MoveResultsInAttackTypeID [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getMapSize ().getHeight ()] [mom.getSessionDescription ().getMapSize ().getWidth ()];

			mom.getServerUnitCalculations ().calculateOverlandMovementDistances (getMoveFrom ().getX (), getMoveFrom ().getY (), getMoveFrom ().getPlane (),
				sender.getPlayerDescription ().getPlayerID (), priv.getFogOfWarMemory (), priv.getNodeLairTowerKnownUnitIDs (),
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

		log.exiting (RequestOverlandMovementDistancesMessageImpl.class.getName (), "process");
	}
}
