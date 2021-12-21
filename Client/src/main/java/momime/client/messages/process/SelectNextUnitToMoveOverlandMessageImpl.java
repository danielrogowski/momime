package momime.client.messages.process;

import java.io.IOException;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.process.OverlandMapProcessing;
import momime.common.messages.servertoclient.SelectNextUnitToMoveOverlandMessage;

/**
 * When the server is sending a sequence of messages resulting from a unit moving, it sends this to say that the
 * sequence is over and the client should then ask for movement for the next unit.
 * 
 * So typical sequence is: MoveUnit -> VisAreaChg -> MoveUnit -> VisAreaChg -> SelectNextUnitToMove
 */
public final class SelectNextUnitToMoveOverlandMessageImpl extends SelectNextUnitToMoveOverlandMessage implements BaseServerToClientMessage
{
	/** Turn sequence and movement helper methods */
	private OverlandMapProcessing overlandMapProcessing;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		getOverlandMapProcessing ().selectNextUnitToMoveOverland ();
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