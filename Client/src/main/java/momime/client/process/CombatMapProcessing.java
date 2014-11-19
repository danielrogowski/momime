package momime.client.process;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

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
	 */
	public void buildUnitsLeftToMoveList ()
		throws JAXBException, XMLStreamException;

	/**
	 * Selects the next unit we need to move in combat
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	public void selectNextUnitToMoveCombat ()
		throws JAXBException, XMLStreamException;
	
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
	 */
	public void selectedUnitDone () throws JAXBException, XMLStreamException;
	
	/**
	 * Indicates that we want to move a different unit before this one
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	public void selectedUnitWait () throws JAXBException, XMLStreamException;

	/**
	 * This is used when right clicking on a specific unit to select it
	 * 
	 * @param unit Unit to manually select
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	public void moveToFrontOfList (final MemoryUnit unit) throws JAXBException, XMLStreamException;
}