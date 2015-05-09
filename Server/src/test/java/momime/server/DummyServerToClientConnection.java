package momime.server;

import java.util.ArrayList;
import java.util.List;

import com.ndg.multiplayer.base.ServerToClientMessage;
import com.ndg.multiplayer.server.ServerToClientSessionConnection;
import com.ndg.multiplayer.server.session.PlayerServerDetails;

/**
 * Dummy connection object that captures all messages sent to the client in a list instead of actually sending them
 */
public final class DummyServerToClientConnection implements ServerToClientSessionConnection
{
	/** List of all messages that have been sent over this 'connection' */
	private final List<ServerToClientMessage> messages;
	
	/** Dummy player storage */
	private PlayerServerDetails player;
	
	/**
	 * Initialize list of messages
	 */
	public DummyServerToClientConnection ()
	{
		super ();
		messages = new ArrayList<ServerToClientMessage> ();
	}
	
	/**
	 * Records XML message in a list
	 * @param msg Message to record
	 */
	@Override
	public final void sendMessageToClient (final ServerToClientMessage msg)
	{
		messages.add (msg);
	}

	/**
	 * @return List of all messages that have been sent over this 'connection'
	 */
	public final List<ServerToClientMessage> getMessages ()
	{
		return messages;
	}

	/**
	 * @return Dummy player storage
	 */
	@Override
	public final PlayerServerDetails getPlayer ()
	{
		return player;
	}

	/**
	 * @param p Dummy player storage
	 */
	@Override
	public final void setPlayer (final PlayerServerDetails p)
	{
		player = p;
	}
}