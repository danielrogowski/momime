package momime.client.process;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.ui.frames.CombatAutoControl;
import momime.client.ui.frames.CombatUI;
import momime.client.ui.renderer.CastCombatSpellFrom;
import momime.common.messages.MemoryUnit;
import momime.common.messages.UnitStatusID;
import momime.common.messages.clienttoserver.EndCombatTurnMessage;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;

/**
 * Methods dealing with combat movement and unit lists, to keep this from making CombatUI too large and complicated.
 * Also many of these have equivalents in OverlandMapProcessingImpl so it made sense to keep the separation the same. 
 */
public final class CombatMapProcessingImpl implements CombatMapProcessing
{
	/** Ordered list of units that we have to give orders to this combat turn; if it isn't our turn, this is empty */
	private final List<MemoryUnit> unitsLeftToMoveCombat = new ArrayList<MemoryUnit> ();

	/** Combat UI */
	private CombatUI combatUI;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
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
		// Rebuild the list
		unitsLeftToMoveCombat.clear ();
		for (final MemoryUnit mu : getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ())
			if ((mu.getDoubleCombatMovesLeft () != null) && (mu.getDoubleCombatMovesLeft () > 0) &&
				(mu.getStatus () == UnitStatusID.ALIVE) && (getCombatUI ().getCombatLocation ().equals (mu.getCombatLocation ())) &&
				(mu.getCombatPosition () != null) && (mu.getCombatHeading () != null) && (mu.getCombatSide () != null))
			{
				final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (mu, null, null, null, getClient ().getPlayers (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());
				
				if (xu.getControllingPlayerID () == getClient ().getOurPlayerID ())
					unitsLeftToMoveCombat.add (mu);
			}
		
		// Ask for movement orders for the first unit
		selectNextUnitToMoveCombat ();
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
		if ((getClient ().getOurPlayerID ().equals (getCombatUI ().getCurrentPlayerID ())) && (getCombatUI ().getAutoControl () == CombatAutoControl.MANUAL))
		{
			// Revert back to the spells the wizard knows, and their cost reductions, in case the spell book was showing casting for a particular unit
			if ((getCombatUI ().getCastingSource () != null) && (getCombatUI ().getCastingSource ().getCastingUnit () != null))
				getCombatUI ().setCastingSource (new CastCombatSpellFrom (null, null, null), false);
			
			if (unitsLeftToMoveCombat.size () == 0)
			{
				getCombatUI ().setSelectedUnitInCombat (null);
				
				// In combat, turns are auto-ended when we have no units to move
				final EndCombatTurnMessage msg = new EndCombatTurnMessage ();
				msg.setCombatURN (getCombatUI ().getCombatURN ());
				getClient ().getServerConnection ().sendMessageToServer (msg);
			}
			else
				getCombatUI ().setSelectedUnitInCombat (unitsLeftToMoveCombat.get (0));
		}
	}

	/**
	 * @param unit Unit to remove from the unitsLeftToMoveCombat list
	 */
	@Override
	public final void removeUnitFromLeftToMoveCombat (final MemoryUnit unit)
	{
		unitsLeftToMoveCombat.remove (unit);
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
		removeUnitFromLeftToMoveCombat (getCombatUI ().getSelectedUnitInCombat ());	
		selectNextUnitToMoveCombat ();
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
		// Only put units back in the 'left to move' list if they already were in it - otherwise this can result in units who've already used
		// up all their movement being put back in the 'left to move' list which really screws things up.
		if (unitsLeftToMoveCombat.remove (getCombatUI ().getSelectedUnitInCombat ()))
			unitsLeftToMoveCombat.add (getCombatUI ().getSelectedUnitInCombat ());
			
		selectNextUnitToMoveCombat ();
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
		unitsLeftToMoveCombat.remove (unit);
		unitsLeftToMoveCombat.add (0, unit);
			
		selectNextUnitToMoveCombat ();
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
	 * @return expandUnitDetails method
	 */
	public final ExpandUnitDetails getExpandUnitDetails ()
	{
		return expandUnitDetails;
	}

	/**
	 * @param e expandUnitDetails method
	 */
	public final void setExpandUnitDetails (final ExpandUnitDetails e)
	{
		expandUnitDetails = e;
	}
}