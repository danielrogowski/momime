package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.process.CombatMapProcessing;
import momime.client.ui.frames.CombatUI;
import momime.common.messages.clienttoserver.CombatAutoControlMessage;
import momime.common.messages.servertoclient.SetCombatPlayerMessage;
import momime.common.utils.UnitUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

/**
 * Server sends this to clients involved in particular combat to tell them whose turn it is next
 */
public final class SetCombatPlayerMessageImpl extends SetCombatPlayerMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (SetCombatPlayerMessageImpl.class);

	/** Combat UI */
	private CombatUI combatUI;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Combat map processing */
	private CombatMapProcessing combatMapProcessing;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start: " + getCombatLocation () + ", Player ID " + getPlayerID ());
		
		getCombatUI ().setCurrentPlayerID (getPlayerID ());
		
		if (getPlayerID () == getClient ().getOurPlayerID ())
		{
			log.debug ("Its our combat turn, auto = " + getCombatUI ().isAutoControl ());
			
			// Tell the server to auto control our units?
			if (getCombatUI ().isAutoControl ())
			{
				final CombatAutoControlMessage msg = new CombatAutoControlMessage ();
				msg.setCombatLocation (getCombatLocation ());
				getClient ().getServerConnection ().sendMessageToServer (msg);
			}
			else
			{
				// Our turn with manual control
				// Give all units their movement for this turn
				getUnitUtils ().resetUnitCombatMovement (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (),
					getPlayerID (), (MapCoordinates3DEx) getCombatLocation (), getClient ().getClientDB ());
				
				// Build a list of the units we need to move
				getCombatMapProcessing ().buildUnitsLeftToMoveList ();			
			}
		}
		else
		{
			log.debug ("Its their combat turn");
		}
		
		log.trace ("Exiting start");
	}

	/**
	 * @return Combat UI
	 */
	public final CombatUI getCombatUI ()
	{
		return combatUI;
	}

	/**
	 * @param ui Combat UI
	 */
	public final void setCombatUI (final CombatUI ui)
	{
		combatUI = ui;
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
	 * @return Combat map processing
	 */
	public final CombatMapProcessing getCombatMapProcessing ()
	{
		return combatMapProcessing;
	}

	/**
	 * @param proc Combat map processing
	 */
	public final void setCombatMapProcessing (final CombatMapProcessing proc)
	{
		combatMapProcessing = proc;
	}
}