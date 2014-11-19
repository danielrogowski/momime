package momime.client.process;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MemoryUnit;

/**
 * Methods dealing with combat movement and unit lists, to keep this from making CombatUI too large and complicated.
 * Also many of these have equivalents in OverlandMapProcessingImpl so it made sense to keep the separation the same. 
 */
public interface CombatMapProcessing
{
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
	public void buildUnitsLeftToMoveList ()
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException;

	/**
	 * Selects the next unit we need to move in combat
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 */
	public void selectNextUnitToMoveCombat ()
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException;
	
	/**
	 * @return Currently selected unit; null if it isn't our turn
	 */
	public MemoryUnit getSelectedUnitInCombat ();
	
	/**
	 * @param unit Unit to remove from the unitsLeftToMoveCombat list
	 */
	public void removeUnitFromLeftToMoveCombat (final MemoryUnit unit);

	/**
	 * Indicates that we don't want the current unit to take any action this turn
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 */
	public void selectedUnitDone () throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException;
	
	/**
	 * Indicates that we want to move a different unit before this one
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 */
	public void selectedUnitWait () throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException;

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
	public void moveToFrontOfList (final MemoryUnit unit) throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException;
}