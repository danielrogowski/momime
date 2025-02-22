package momime.client.messages.process;

import java.io.IOException;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.MomClient;
import momime.client.ui.frames.HeroItemsUI;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.common.messages.servertoclient.AddUnassignedHeroItemMessage;

/**
 * Adds a hero item to the unassigned items in the player's bank vault, either because
 * they just created it, found it as a treasure reward, or moved it there from a hero who was using it.
 */
public final class AddUnassignedHeroItemMessageImpl extends AddUnassignedHeroItemMessage implements BaseServerToClientMessage
{
	/** Multiplayer client */
	private MomClient client;

	/** Hero items UI */
	private HeroItemsUI heroItemsUI;
	
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
		getClient ().getOurPersistentPlayerPrivateKnowledge ().getUnassignedHeroItem ().add (getHeroItem ());
		getHeroItemsUI ().refreshItemsBank ();
		getOverlandMapRightHandPanel ().updateProductionTypesStoppingUsFromEndingTurn ();
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