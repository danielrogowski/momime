package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.MomClient;
import momime.client.ui.frames.HeroItemsUI;
import momime.common.messages.servertoclient.AddUnassignedHeroItemMessage;

/**
 * Adds a hero item to the unassigned items in the player's bank vault, either because
 * they just created it, found it as a treasure reward, or moved it there from a hero who was using it.
 */
public final class AddUnassignedHeroItemMessageImpl extends AddUnassignedHeroItemMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (AddUnassignedHeroItemMessageImpl.class);

	/** Multiplayer client */
	private MomClient client;

	/** Hero items UI */
	private HeroItemsUI heroItemsUI;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start: " + getHeroItem ().getHeroItemURN () + ", " + getHeroItem ().getHeroItemName ());
		
		getClient ().getOurPersistentPlayerPrivateKnowledge ().getUnassignedHeroItem ().add (getHeroItem ());
		getHeroItemsUI ().refreshItemsBank ();
		
		log.trace ("Exiting start");
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
	 * @return Hero items UI
	 */
	public final HeroItemsUI getHeroItemsUI ()
	{
		return heroItemsUI;
	}

	/**
	 * @param ui Hero items UI
	 */
	public final void setHeroItemsUI (final HeroItemsUI ui)
	{
		heroItemsUI = ui;
	}
}