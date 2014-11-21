package momime.client.process;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.ui.frames.CombatUI;
import momime.common.messages.MemoryUnit;
import momime.common.messages.UnitStatusID;
import momime.common.messages.clienttoserver.EndCombatTurnMessage;

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
	 * @throws IOException If there is a problem
	 */
	@Override
	public final void buildUnitsLeftToMoveList () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering buildUnitsLeftToMoveList");
		
		// Rebuild the list
		unitsLeftToMoveCombat.clear ();
		for (final MemoryUnit mu : getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ())
			if ((mu.getOwningPlayerID () == getClient ().getOurPlayerID ()) && (mu.getDoubleCombatMovesLeft () != null) && (mu.getDoubleCombatMovesLeft () > 0) &&
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
	 * @throws IOException If there is a problem
	 */
	@Override
	public final void selectNextUnitToMoveCombat ()
		throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering selectNextUnitToMoveCombat");
		
		if ((getClient ().getOurPlayerID ().equals (getCombatUI ().getCurrentPlayerID ())) && (!getCombatUI ().isAutoControl ()))
		{
			if (unitsLeftToMoveCombat.size () == 0)
			{
				getCombatUI ().setSelectedUnitInCombat (null);
				
				// In combat, turns are auto-ended when we have no units to move
				final EndCombatTurnMessage msg = new EndCombatTurnMessage ();
				msg.setCombatLocation (getCombatUI ().getCombatLocation ());
				getClient ().getServerConnection ().sendMessageToServer (msg);
			}
			else
				getCombatUI ().setSelectedUnitInCombat (unitsLeftToMoveCombat.get (0));
		}
		
		log.trace ("Exiting selectNextUnitToMoveCombat");
	}

	/**
	 * @param unit Unit to remove from the unitsLeftToMoveCombat list
	 */
	@Override
	public final void removeUnitFromLeftToMoveCombat (final MemoryUnit unit)
	{
		log.trace ("Entering removeUnitFromLeftToMoveCombat");

		unitsLeftToMoveCombat.remove (unit);

		log.trace ("Exiting removeUnitFromLeftToMoveCombat");
	}
	
	/**
	 * Indicates that we don't want the current unit to take any action this turn
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws IOException If there is a problem
	 */
	@Override
	public final void selectedUnitDone () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering selectedUnitsDone");
		
		removeUnitFromLeftToMoveCombat (getCombatUI ().getSelectedUnitInCombat ());	
		selectNextUnitToMoveCombat ();
		
		log.trace ("Exiting selectedUnitsDone");
	}
	
	/**
	 * Indicates that we want to move a different unit before this one
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws IOException If there is a problem
	 */
	@Override
	public final void selectedUnitWait () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering selectedUnitsWait");

		// Only put units back in the 'left to move' list if they already were in it - otherwise this can result in units who've already used
		// up all their movement being put back in the 'left to move' list which really screws things up.
		if (unitsLeftToMoveCombat.remove (getCombatUI ().getSelectedUnitInCombat ()))
			unitsLeftToMoveCombat.add (getCombatUI ().getSelectedUnitInCombat ());
			
		selectNextUnitToMoveCombat ();
		
		log.trace ("Exiting selectedUnitsWait");
	}

	/**
	 * This is used when right clicking on a specific unit to select it
	 * 
	 * @param unit Unit to manually select
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws IOException If there is a problem
	 */
	@Override
	public final void moveToFrontOfList (final MemoryUnit unit) throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering moveToFrontOfList: Unit URN " + unit.getUnitURN ());

		unitsLeftToMoveCombat.remove (unit);
		unitsLeftToMoveCombat.add (0, unit);
			
		selectNextUnitToMoveCombat ();
		
		log.trace ("Exiting moveToFrontOfList");
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