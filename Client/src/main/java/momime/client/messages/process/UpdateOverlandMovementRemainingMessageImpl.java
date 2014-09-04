package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.process.OverlandMapProcessing;
import momime.common.messages.servertoclient.v0_9_5.UpdateOverlandMovementRemainingMessage;
import momime.common.messages.servertoclient.v0_9_5.UpdateOverlandMovementRemainingUnit;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.utils.UnitUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.client.MultiplayerServerConnection;
import com.ndg.multiplayer.client.SessionServerToClientMessage;

/**
 * Server tells the client how much movement each unit in a stack has left after completing its move (in case it can move again)
 */
public final class UpdateOverlandMovementRemainingMessageImpl extends UpdateOverlandMovementRemainingMessage implements SessionServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (SetCurrentPlayerMessageImpl.class);

	/** Multiplayer client */
	private MomClient client;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
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
		
		for (final UpdateOverlandMovementRemainingUnit movementRemaining : getUnit ())
		{
			final MemoryUnit mu = getUnitUtils ().findUnitURN (movementRemaining.getUnitURN (),
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "UpdateOverlandMovementRemainingMessageImpl");
			
			mu.setDoubleOverlandMovesLeft (movementRemaining.getDoubleMovesLeft ());
			
			// If unit has no movement left, remove it from the wait list
			if (movementRemaining.getDoubleMovesLeft () == 0)
				getOverlandMapProcessing ().removeUnitFromLeftToMoveOverland (mu);
		}
		
		log.trace ("Exiting process");
	}

	/**
	 * @return Multiplayer client
	 */
	public final MomClient getClient ()
	{
		return client;
	}
	
	/**
	 * @param obj Multiplayer client
	 */
	public final void setClient (final MomClient obj)
	{
		client = obj;
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