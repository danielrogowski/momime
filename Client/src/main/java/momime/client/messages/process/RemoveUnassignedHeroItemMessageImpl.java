package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.MomClient;
import momime.client.ui.frames.HeroItemsUI;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.servertoclient.RemoveUnassignedHeroItemMessage;
import momime.common.utils.HeroItemUtils;

/**
 * Removes a hero item from the unassigned items in the player's bank vault, either because
 * they're destroying it on the anvil, or moving it from there to a hero to make use of it.
 */
public final class RemoveUnassignedHeroItemMessageImpl extends RemoveUnassignedHeroItemMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (RemoveUnassignedHeroItemMessageImpl.class);

	/** Multiplayer client */
	private MomClient client;

	/** Hero items UI */
	private HeroItemsUI heroItemsUI;
	
	/** Hero item utils */
	private HeroItemUtils heroItemUtils;
	
	/** Overland map right hand panel showing economy etc */
	private OverlandMapRightHandPanel overlandMapRightHandPanel;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start: " + getHeroItemURN ());
		
		final NumberedHeroItem item = getHeroItemUtils ().findHeroItemURN (getHeroItemURN (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getUnassignedHeroItem ());
		
		if (item == null)
			log.warn ("Server told us to remove Hero Item URN " + getHeroItemURN () + " from our bank, but can't find it");
		else
		{
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getUnassignedHeroItem ().remove (item);
			getHeroItemsUI ().refreshItemsBank ();
			getOverlandMapRightHandPanel ().updateProductionTypesStoppingUsFromEndingTurn ();
		}
		
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

	/**
	 * @return Hero item utils
	 */
	public final HeroItemUtils getHeroItemUtils ()
	{
		return heroItemUtils;
	}

	/**
	 * @param util Hero item utils
	 */
	public final void setHeroItemUtils (final HeroItemUtils util)
	{
		heroItemUtils = util;
	}

	/**
	 * @return Overland map right hand panel showing economy etc
	 */
	public final OverlandMapRightHandPanel getOverlandMapRightHandPanel ()
	{
		return overlandMapRightHandPanel;
	}

	/**
	 * @param panel Overland map right hand panel showing economy etc
	 */
	public final void setOverlandMapRightHandPanel (final OverlandMapRightHandPanel panel)
	{
		overlandMapRightHandPanel = panel;
	}
}