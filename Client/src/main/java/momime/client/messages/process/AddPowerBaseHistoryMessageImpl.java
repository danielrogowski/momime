package momime.client.messages.process;

import java.io.IOException;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.client.MomClient;
import momime.client.ui.frames.HistoryUI;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.servertoclient.AddPowerBaseHistoryMessage;
import momime.common.messages.servertoclient.PowerBaseHistoryPlayer;

/**
 * Server broadcasts history of all wizards' power base each turn to show on the Historian screen
 */
public final class AddPowerBaseHistoryMessageImpl extends AddPowerBaseHistoryMessage implements BaseServerToClientMessage
{
	/** Multiplayer client */
	private MomClient client;
	
	/** UI for screen showing power base history for each wizard */
	private HistoryUI historyUI;

	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		for (final PowerBaseHistoryPlayer value : getPlayer ())
		{
			final PlayerPublicDetails thisPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), value.getPlayerID ());
			if (thisPlayer != null)
			{
				final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) thisPlayer.getPersistentPlayerPublicKnowledge ();
				for (int n = 0; n < value.getZeroCount (); n++)
					pub.getPowerBaseHistory ().add (0);
				
				pub.getPowerBaseHistory ().add (value.getPowerBase ());
			}
		}
		
		getHistoryUI ().redrawChart ();
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
	 * @return UI for screen showing power base history for each wizard
	 */
	public final HistoryUI getHistoryUI ()
	{
		return historyUI;
	}

	/**
	 * @param h UI for screen showing power base history for each wizard
	 */
	public final void setHistoryUI (final HistoryUI h)
	{
		historyUI = h;
	}

	/**
	 * @return Session utils
	 */
	public final MultiplayerSessionUtils getMultiplayerSessionUtils ()
	{
		return multiplayerSessionUtils;
	}

	/**
	 * @param util Session utils
	 */
	public final void setMultiplayerSessionUtils (final MultiplayerSessionUtils util)
	{
		multiplayerSessionUtils = util;
	}
}