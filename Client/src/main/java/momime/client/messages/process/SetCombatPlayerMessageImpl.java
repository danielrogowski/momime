package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.MomClient;
import momime.client.process.CombatMapProcessing;
import momime.client.ui.frames.CombatUI;
import momime.client.ui.renderer.CastCombatSpellFrom;
import momime.common.calculations.UnitCalculations;
import momime.common.messages.clienttoserver.CombatAutoControlMessage;
import momime.common.messages.servertoclient.SetCombatPlayerMessage;

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
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
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
			
			// We can cast a spell again; and default to casting from the wizard
			getCombatUI ().setCastingSource (new CastCombatSpellFrom (null, null, null), false);
			
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
				getUnitCalculations ().resetUnitCombatMovement (getPlayerID (), (MapCoordinates3DEx) getCombatLocation (), getClient ().getPlayers (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());
				
				// Build a list of the units we need to move
				getCombatMapProcessing ().buildUnitsLeftToMoveList ();			
			}
		}
		else
		{
			log.debug ("Its their combat turn");
			
			// This disables spell casting because it realises it isn't our turn
			getCombatUI ().setCastingSource (null, false);
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
	 * @return Unit calculations
	 */
	public final UnitCalculations getUnitCalculations ()
	{
		return unitCalculations;
	}

	/**
	 * @param calc Unit calculations
	 */
	public final void setUnitCalculations (final UnitCalculations calc)
	{
		unitCalculations = calc;
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