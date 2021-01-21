package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.MomClient;
import momime.client.process.OverlandMapProcessing;
import momime.client.ui.frames.OverlandMapUI;
import momime.common.messages.servertoclient.PendingMovementMessage;
import momime.common.utils.UnitUtils;

/**
 * Server sends this to clients who request that units move further than they can reach in one turn, or in "simultaneous turns" mode.
 * This is that tells the client where to draw the white arrows showing the unit stack's intended movement.
 */
public final class PendingMovementMessageImpl extends PendingMovementMessage implements BaseServerToClientMessage
{
	/** Multiplayer client */
	private MomClient client;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Turn sequence and movement helper methods */
	private OverlandMapProcessing overlandMapProcessing;
	
	/** Overland map UI */
	private OverlandMapUI overlandMapUI;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		// Add the pending movement to our local storage
		getClient ().getOurPersistentPlayerPrivateKnowledge ().getPendingMovement ().add (getPendingMovement ());
		
		// Remove the units from the 'left to move' list.
		// The server will send a separate message to tell us to call SelectNextUnitToMoveOverland
		for (final Integer unitURN : getPendingMovement ().getUnitURN ())
			getOverlandMapProcessing ().removeUnitFromLeftToMoveOverland (getUnitUtils ().findUnitURN
				(unitURN, getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "PendingMovementMessageImpl"));

		// Show the pending movement on the overland map
		getOverlandMapUI ().repaintSceneryPanel ();
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

	/**
	 * @return Overland map UI
	 */
	public final OverlandMapUI getOverlandMapUI ()
	{
		return overlandMapUI;
	}

	/**
	 * @param ui Overland map UI
	 */
	public final void setOverlandMapUI (final OverlandMapUI ui)
	{
		overlandMapUI = ui;
	}
}