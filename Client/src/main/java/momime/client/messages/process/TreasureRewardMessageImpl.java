package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.ui.dialogs.TreasureUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.common.messages.servertoclient.TreasureRewardMessage;

/**
 * Stores all the treasure gained from capturing a node/lair/tower.
 * This is purely informational, like NTMs.  Other messages (e.g. add hero item, full spell list) will be sent containing the actual updates.
 */
public final class TreasureRewardMessageImpl extends TreasureRewardMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (TreasureRewardMessageImpl.class);

	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start: " + getHeroItem ().size () + ", " + getSpellID ().size () + ", " + getResource ().size () + ", " +
			getPick ().size () + ", " + getPrisoner ().size ());

		final TreasureUI treasureUI = getPrototypeFrameCreator ().createTreasureReward ();
		treasureUI.setTreasureReward (this);
		treasureUI.setVisible (true);
		
		log.trace ("Exiting start");
	}
	
	/**
	 * @return Prototype frame creator
	 */
	public final PrototypeFrameCreator getPrototypeFrameCreator ()
	{
		return prototypeFrameCreator;
	}

	/**
	 * @param obj Prototype frame creator
	 */
	public final void setPrototypeFrameCreator (final PrototypeFrameCreator obj)
	{
		prototypeFrameCreator = obj;
	}
}