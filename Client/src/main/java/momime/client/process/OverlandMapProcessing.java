package momime.client.process;

import java.io.IOException;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitSpecialOrder;
import momime.common.messages.MemoryUnit;


/**
 * Methods dealing with the turn sequence and overland movement that are too big to leave in
 * message implementations, or are used multiple times. 
 */
public interface OverlandMapProcessing
{
	/**
	 * At the start of a turn, once all our movement has been reset and the server has sent any continuation moves to us, this gets called.
	 * It builds a list of units we need to give movement orders to i.e. all those units which have movement left and are not patrolling.
	 * 
	 * @throws RecordNotFoundException If a unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns a unit
	 * @throws MomException If we cannot find any appropriate experience level for a unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	public void buildUnitsLeftToMoveList ()
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException;

	/**
	 * Selects and centres the map on the next unit which we need to give a movement order to
	 * @throws RecordNotFoundException If a unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns a unit
	 * @throws MomException If we cannot find any appropriate experience level for a unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @return Whether there was a unit left to move
	 */
	public boolean selectNextUnitToMoveOverland ()
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException;
	
	/**
	 * Sets the select unit boxes appropriately for the units we have in the specified cell
	 * @param unitLocation Location of the unit stack to move; null means we're moving nothing so just remove all old unit selection buttons
	 * @throws RecordNotFoundException If a unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns a unit
	 * @throws MomException If we cannot find any appropriate experience level for a unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @return True if there were unit(s) at the location to select OR we had any units left to move; false if we found units neither way
	 */
	public boolean showSelectUnitBoxes (final MapCoordinates3DEx unitLocation)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException;
	
	/**
	 * To be able to build cities and perform other special orders, there are a number of checks we need to do
	 * @throws RecordNotFoundException If a unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns a unit
	 * @throws MomException If we cannot find any appropriate experience level for a unit
	 */
	public void enableOrDisableSpecialOrderButtons () throws RecordNotFoundException, PlayerNotFoundException, MomException;
	
	/**
	 * Updates the indicator for how much movement the current unit stack has left
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If a unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns a unit
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public void updateMovementRemaining ()
		throws JAXBException, XMLStreamException, PlayerNotFoundException, RecordNotFoundException, MomException;

	/**
	 * @return Whether we're in the middle of the server processing and sending us pending moves
	 */
	public boolean isProcessingContinuedMovement ();
	
	/**
	 * @param cont Whether we're in the middle of the server processing and sending us pending moves
	 */
	public void setProcessingContinuedMovement (final boolean cont);
	
	/**
	 * @return Whether at least one of the select unit boxes is selected
	 */
	public boolean isAnyUnitSelectedToMove ();

	/**
	 * @param unit Unit to test
	 * @return Whether the specified unit has a selected box - this doesn't imply we can move it, enemy units' boxes are permanently selected so their wizard colour shows 
	 */
	public boolean isUnitSelected (final MemoryUnit unit);
	
	/**
	 * Removes all currently selected units from the 'units left to move' list, so that we won't ask the player about these units again this turn
	 * 
	 * @throws RecordNotFoundException If a unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns a unit
	 * @throws MomException If we cannot find any appropriate experience level for a unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	public void selectedUnitsDone () throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException;
	
	/**
	 * Moves all currently selected units to the end of the 'units left to move' list, so that we will ask the player
	 * about these units again this turn, but only after we've prompted them to move every other unit first
	 * 
	 * @throws RecordNotFoundException If a unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns a unit
	 * @throws MomException If we cannot find any appropriate experience level for a unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	public void selectedUnitsWait () throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException;
	
	/**
	 * Sets all selected units into patrolling mode, so that we won't ask the player about these units again in this or subsequent turns
	 * 
	 * @throws RecordNotFoundException If a unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns a unit
	 * @throws MomException If we cannot find any appropriate experience level for a unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	public void selectedUnitsPatrol () throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException;

	/**
	 * Tells the server that we want to move the currently selected units to a different location on the overland map
	 * @param moveTo The place to move to
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public void moveUnitStackTo (final MapCoordinates3DEx moveTo) throws JAXBException, XMLStreamException, MomException;

	/**
	 * Tells the server that we want to have the currently selected unit(s) perform some special action,
	 * such as settlers building an outpost, engineers building a road, or magic spirits capturing a node. 
	 * 
	 * @param specialOrder Special order to perform
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws IOException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public void specialOrderButton (final UnitSpecialOrder specialOrder) throws JAXBException, XMLStreamException, IOException;
	
	/**
	 * @param unit Unit to remove from the unitsLeftToMoveOverland list
	 */
	public void removeUnitFromLeftToMoveOverland (final MemoryUnit unit);
	
	/**
	 * Tell the server we clicked the Next Turn button
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If a unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns a unit
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public void nextTurnButton ()
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException;
}