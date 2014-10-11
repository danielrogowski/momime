package momime.client.process;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.ui.frames.CombatUI;
import momime.common.messages.clienttoserver.v0_9_5.EndCombatTurnMessage;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.messages.v0_9_5.UnitStatusID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Methods dealing with combat movement and unit lists, to keep this from making CombatUI too large and complicated.
 * Also many of these have equivalents in OverlandMapProcessingImpl so it made sense to keep the separation the same. 
 */
public final class CombatMapProcessingImpl implements CombatMapProcessing
{
	/** Class logger */
	private final Log log = LogFactory.getLog (CombatMapProcessingImpl.class);
	
	/** Ordered list of units that we have to give orders to this combat turn; if it isn't our turn, this is empty */
	private final List<MemoryUnit> unitsLeftToMoveCombat = new ArrayList<MemoryUnit> ();

	/** Combat UI */
	private CombatUI combatUI;
	
	/** Multiplayer client */
	private MomClient client;
	
	/**
	 * At the start of our combat turn, once all our movement has been reset, this gets called.
	 * It builds a list of units we need to give orders during this combat turn. 
	 * 
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final void buildUnitsLeftToMoveList ()
		throws JAXBException, XMLStreamException
	{
		log.trace ("Entering buildUnitsLeftToMoveList");
		
		// Rebuild the list
		unitsLeftToMoveCombat.clear ();
		for (final MemoryUnit mu : getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ())
			if ((mu.getOwningPlayerID () == getClient ().getOurPlayerID ()) && (mu.getDoubleCombatMovesLeft () > 0) &&
				(mu.getStatus () == UnitStatusID.ALIVE) && (getCombatUI ().getCombatLocation ().equals (mu.getCombatLocation ())))
				
				unitsLeftToMoveCombat.add (mu);
		
		// Ask for movement orders for the first unit
		selectNextUnitToMoveCombat ();
		
		log.trace ("Exiting buildUnitsLeftToMoveList");
	}

	/**
	 * Selects the next unit we need to move in combat
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	public final void selectNextUnitToMoveCombat ()
		throws JAXBException, XMLStreamException
	{
		log.trace ("Entering selectNextUnitToMoveCombat");
		
		if ((getClient ().getOurPlayerID ().equals (getCombatUI ().getCurrentPlayerID ())) && (!getCombatUI ().isAutoControl ()))
		{
			if (unitsLeftToMoveCombat.size () == 0)
			{
				// In combat, turns are auto-ended when we have no units to move
				final EndCombatTurnMessage msg = new EndCombatTurnMessage ();
				msg.setCombatLocation (getCombatUI ().getCombatLocation ());
				getClient ().getServerConnection ().sendMessageToServer (msg);
			}
			else
				setSelectedUnitInCombat (unitsLeftToMoveCombat.get (0));
		}
		
		log.trace ("Exiting selectNextUnitToMoveCombat");
	}

	/**
	 * Updates various controls, e.g. melee/ranged attack strength displayed in the panel at the bottom, when a different unit is selected in combat
	 * @param unit Unit to select
	 */
	public final void setSelectedUnitInCombat (final MemoryUnit unit)
	{
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
}