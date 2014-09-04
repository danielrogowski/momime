package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.process.OverlandMapProcessing;
import momime.common.messages.servertoclient.v0_9_5.SelectNextUnitToMoveOverlandMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.client.MultiplayerServerConnection;
import com.ndg.multiplayer.client.SessionServerToClientMessage;

/**
 * When the server is sending a sequence of messages resulting from a unit moving, it sends this to say that the
 * sequence is over and the client should then ask for movement for the next unit.
 * 
 * So typical sequence is: MoveUnit -> VisAreaChg -> MoveUnit -> VisAreaChg -> SelectNextUnitToMove
 */
public final class SelectNextUnitToMoveOverlandMessageImpl extends SelectNextUnitToMoveOverlandMessage implements SessionServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (SetCurrentPlayerMessageImpl.class);
	
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
		
		getOverlandMapProcessing ().selectNextUnitToMoveOverland ();
		
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