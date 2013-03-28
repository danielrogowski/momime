package momime.server;

import java.util.ArrayList;
import java.util.List;

import com.ndg.multiplayer.base.IServerToClientConnection;
import com.ndg.multiplayer.base.ServerToClientMessage;

/**
 * Dummy connection object that captures all messages sent to the client in a list instead of actually sending them
 */
public final class DummyServerToClientConnection implements IServerToClientConnection
{
	/** List of all messages that have been sent over this 'connection' */
	private final List<ServerToClientMessage> messages;
	
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
}
