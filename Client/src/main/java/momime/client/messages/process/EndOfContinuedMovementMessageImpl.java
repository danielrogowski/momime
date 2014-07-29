package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.client.MultiplayerServerConnection;
import com.ndg.multiplayer.client.SessionServerToClientMessage;

import momime.client.process.OverlandMapProcessing;
import momime.common.messages.servertoclient.v0_9_5.EndOfContinuedMovementMessage;

/**
 * Server sends to client to tell them that it has finished processed their continued unit movement
 * left over from the last turn, and so they can start to allocate new movement.
 * This is only sent for one-at-a-time games - since with simultaneous turns movement, movement is at the end rather than the beginning of a turn.
 * It is also only sent to the player whose turn it now is.
 */
public final class EndOfContinuedMovementMessageImpl extends EndOfContinuedMovementMessage implements SessionServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (EndOfContinuedMovementMessageImpl.class);

	/** Turn sequence and movement helper methods */
	private OverlandMapProcessing overlandMapProcessing;
	
	/**
	 * @param sender Connection to the server
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void process (final MultiplayerServerConnection sender)
		throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering process");
		
		getOverlandMapProcessing ().setProcessingContinuedMovement (false);
		getOverlandMapProcessing ().buildUnitsLeftToMoveList ();
		
		log.trace ("Exiting process");
	}

	/**
	 * @return Turn sequence and movement helper methods
	 */
	public final OverlandMapProcessing getOverlandMapProcessing ()
	{
		return overlandMapProcessing;
	}

	/**
	 * @param proc Turn sequence and movement helper methods
	 */
	public final void setOverlandMapProcessing (final OverlandMapProcessing proc)
	{
		overlandMapProcessing = proc;
	}
}