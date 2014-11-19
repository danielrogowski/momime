package momime.client.process;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.ui.frames.CombatUI;
import momime.common.MomException;
import momime.common.calculations.CombatMoveType;
import momime.common.calculations.MomUnitCalculations;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.CombatMapSizeData;
import momime.common.messages.MemoryUnit;
import momime.common.messages.UnitStatusID;
import momime.common.messages.clienttoserver.EndCombatTurnMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.PlayerNotFoundException;

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
	
	/** Unit calculations */
	private MomUnitCalculations unitCalculations;
	
	/** Currently selected unit */
	private MemoryUnit selectedUnitInCombat;
	
	/**
	 * At the start of our combat turn, once all our movement has been reset, this gets called.
	 * It builds a list of units we need to give orders during this combat turn. 
	 * 
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 */
	@Override
	public final void buildUnitsLeftToMoveList ()
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
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
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 */
	@Override
	public final void selectNextUnitToMoveCombat ()
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.trace ("Entering selectNextUnitToMoveCombat");
		
		if ((getClient ().getOurPlayerID ().equals (getCombatUI ().getCurrentPlayerID ())) && (!getCombatUI ().isAutoControl ()))
		{
			if (unitsLeftToMoveCombat.size () == 0)
			{
				setSelectedUnitInCombat (null);
				
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
	 * @return Currently selected unit
	 */
	@Override
	public final MemoryUnit getSelectedUnitInCombat ()
	{
		return selectedUnitInCombat;
	}	
	
	/**
	 * Updates various controls, e.g. melee/ranged attack strength displayed in the panel at the bottom, when a different unit is selected in combat
	 * 
	 * @param unit Unit to select; this may be null if we've got no further units to move and our turn is ending
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 */
	public final void setSelectedUnitInCombat (final MemoryUnit unit) throws RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.trace ("Entering setSelectedUnitInCombat: " + ((unit == null) ? "null" : "Unit URN " + unit.getUnitURN ()));
		
		// Record the selected unit
		selectedUnitInCombat = unit;
		
		// Work out where this unit can and cannot move
		if (unit == null)
			getCombatUI ().setMovementTypes (null);
		else
		{
			final CombatMapSizeData combatMapSize = getClient ().getSessionDescription ().getCombatMapSize ();
			
			final int [] [] movementDirections = new int [combatMapSize.getHeight ()] [combatMapSize.getWidth ()];
			final CombatMoveType [] [] movementTypes = new CombatMoveType [combatMapSize.getHeight ()] [combatMapSize.getWidth ()];
			final int [] [] doubleMovementDistances = new int [combatMapSize.getHeight ()] [combatMapSize.getWidth ()];
	
			getUnitCalculations ().calculateCombatMovementDistances (doubleMovementDistances, movementDirections, movementTypes, unit,
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getCombatUI ().getCombatTerrain (),
				combatMapSize, getClient ().getPlayers (), getClient ().getClientDB ());
			
			// The only array we actually need to keep is the movementTypes, to show the correct icons as the mouse moves over different tiles
			getCombatUI ().setMovementTypes (movementTypes);
		}
		
		log.trace ("Entering setSelectedUnitInCombat");
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
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 */
	@Override
	public final void selectedUnitDone () throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.trace ("Entering selectedUnitsDone");
		
		removeUnitFromLeftToMoveCombat (selectedUnitInCombat);	
		selectNextUnitToMoveCombat ();
		
		log.trace ("Exiting selectedUnitsDone");
	}
	
	/**
	 * Indicates that we want to move a different unit before this one
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 */
	@Override
	public final void selectedUnitWait () throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.trace ("Entering selectedUnitsWait");

		// Only put units back in the 'left to move' list if they already were in it - otherwise this can result in units who've already used
		// up all their movement being put back in the 'left to move' list which really screws things up.
		if (unitsLeftToMoveCombat.remove (selectedUnitInCombat))
			unitsLeftToMoveCombat.add (selectedUnitInCombat);
			
		selectNextUnitToMoveCombat ();
		
		log.trace ("Exiting selectedUnitsWait");
	}

	/**
	 * This is used when right clicking on a specific unit to select it
	 * 
	 * @param unit Unit to manually select
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 */
	@Override
	public final void moveToFrontOfList (final MemoryUnit unit) throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
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

	/**
	 * @return Unit calculations
	 */
	public final MomUnitCalculations getUnitCalculations ()
	{
		return unitCalculations;
	}

	/**
	 * @param calc Unit calculations
	 */
	public final void setUnitCalculations (final MomUnitCalculations calc)
	{
		unitCalculations = calc;
	}
}