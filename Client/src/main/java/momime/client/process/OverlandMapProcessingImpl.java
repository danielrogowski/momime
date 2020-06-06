package momime.client.process;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.client.MomClient;
import momime.client.ui.components.HideableComponent;
import momime.client.ui.components.SelectUnitButton;
import momime.client.ui.frames.OverlandMapUI;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.client.ui.panels.OverlandMapRightHandPanelBottom;
import momime.client.ui.panels.OverlandMapRightHandPanelTop;
import momime.common.MomException;
import momime.common.calculations.CityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.MapFeature;
import momime.common.database.RecordNotFoundException;
import momime.common.database.TileType;
import momime.common.database.UnitSpecialOrder;
import momime.common.messages.MemoryUnit;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.TurnSystem;
import momime.common.messages.UnitStatusID;
import momime.common.messages.clienttoserver.NextTurnButtonMessage;
import momime.common.messages.clienttoserver.RequestMoveOverlandUnitStackMessage;
import momime.common.messages.clienttoserver.RequestOverlandMovementDistancesMessage;
import momime.common.messages.clienttoserver.SpecialOrderButtonMessage;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.PendingMovementUtils;
import momime.common.utils.UnitUtils;

/**
 * Methods dealing with the turn sequence and overland movement that are too big to leave in
 * message implementations, or are used multiple times. 
 */
public final class OverlandMapProcessingImpl implements OverlandMapProcessing
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (OverlandMapProcessingImpl.class);
	
	/** Ordered list of units that we have to give orders to this turn */
	private final List<MemoryUnit> unitsLeftToMoveOverland = new ArrayList<MemoryUnit> ();
	
	/** Multiplayer client */
	private MomClient client;

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Pending movement utils */
	private PendingMovementUtils pendingMovementUtils;

	/** City calculations */
	private CityCalculations cityCalculations;
	
	/** Overland map UI */
	private OverlandMapUI overlandMapUI;
	
	/** Overland map right hand panel showing economy etc */
	private OverlandMapRightHandPanel overlandMapRightHandPanel;
	
	/** Whether we're in the middle of the server processing and sending us pending moves */
	private boolean processingContinuedMovement;
	
	/** The map location we're currently selecting/deselecting units at ready to choose an order for them or tell them where to move/attack */
	private MapCoordinates3DEx unitMoveFrom;
		
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
	@Override
	public final void buildUnitsLeftToMoveList ()
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		log.trace ("Entering buildUnitsLeftToMoveList");
		
		// Rebuild the list
		unitsLeftToMoveOverland.clear ();
		for (final MemoryUnit mu : getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ())
			if ((mu.getOwningPlayerID () == getClient ().getOurPlayerID ()) && (mu.getDoubleOverlandMovesLeft () > 0) &&
				(mu.getSpecialOrder () == null) && (mu.getStatus () == UnitStatusID.ALIVE) &&
				(getPendingMovementUtils ().findPendingMoveForUnit (getClient ().getOurPersistentPlayerPrivateKnowledge ().getPendingMovement (), mu.getUnitURN ()) == null))
				
				unitsLeftToMoveOverland.add (mu);
		
		// On every other turn, prompt to move units in the reverse order
		if ((getClient ().getGeneralPublicKnowledge ().getTurnNumber () % 2 == 0) && (unitsLeftToMoveOverland.size () > 1))
		{
			final List<MemoryUnit> listCopy = new ArrayList<MemoryUnit> ();
			for (int n = 0; n < unitsLeftToMoveOverland.size (); n++)
				listCopy.add (unitsLeftToMoveOverland.get (unitsLeftToMoveOverland.size () - 1 - n));
			
			unitsLeftToMoveOverland.clear ();
			unitsLeftToMoveOverland.addAll (listCopy);
		}
		
		// Ask for movement orders for the first unit
		selectNextUnitToMoveOverland ();
		
		log.trace ("Exiting buildUnitsLeftToMoveList");
	}
	
	/**
	 * Selects and centres the map on the next unit which we need to give a movement order to
	 * @throws RecordNotFoundException If a unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns a unit
	 * @throws MomException If we cannot find any appropriate experience level for a unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @return Whether there was a unit left to move
	 */
	@Override
	public final boolean selectNextUnitToMoveOverland ()
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		log.trace ("Entering selectNextUnitToMoveOverland");
		
		final boolean found = (unitsLeftToMoveOverland.size () > 0);
		if (found)
		{
			final MemoryUnit unitToMove = unitsLeftToMoveOverland.get (0);
			
			// Select this unit stack
			showSelectUnitBoxes (new MapCoordinates3DEx ((MapCoordinates3DEx) unitToMove.getUnitLocation ()));
			
			// Shift the map to be centred on this location
			getOverlandMapUI ().scrollTo (unitToMove.getUnitLocation ().getX (), unitToMove.getUnitLocation ().getY (), unitToMove.getUnitLocation ().getZ (), false);
		}
		else
		{
			// Get rid of any old select unit buttons, then use the regular routine to sort the panels and buttons out
			showSelectUnitBoxes (null);
		}
		
		log.trace ("Exiting selectNextUnitToMoveOverland = " + found);
		return found;
	}

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
	@Override
	public final boolean showSelectUnitBoxes (final MapCoordinates3DEx unitLocation)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		log.trace ("Entering showSelectUnitBoxes: " + unitLocation);
		
		// Search for units at this location.  Note unlike buildUnitsLeftToMoveList, which ignores units with no movement, pending movement or
		// special orders, here we want any unit as long as its alive and at the right location.
		final Iterator<HideableComponent<SelectUnitButton>> buttonIter = getOverlandMapRightHandPanel ().getSelectUnitButtons ().iterator ();
		
		int count = 0;
		if (unitLocation != null)
			for (final MemoryUnit mu : getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ())
				if ((unitLocation.equals (mu.getUnitLocation ())) && (mu.getStatus () == UnitStatusID.ALIVE))
				{
					final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (mu, null, null, null,
						getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());
					
					final HideableComponent<SelectUnitButton> button = buttonIter.next ();
					button.getComponent ().setUnit (xu);
					button.getComponent ().setSelected (unitsLeftToMoveOverland.contains (mu));	// Pre-select this unit as long as it hasn't already passed its allocated movement sequence
					button.setHidden (false);
					count++;
				}

		// Get rid of any remaining spare unused buttons
		while (buttonIter.hasNext ())
		{
			final HideableComponent<SelectUnitButton> button = buttonIter.next ();
			button.setHidden (true);
			button.getComponent ().setSelected (false);
			button.getComponent ().setUnit (null);
		}
		
		// If we found no units at all (probably player right clicked on empty piece of map) then just jump to next unit to move.
		// This helps the game not get stuck at "Current player: you" without suggesting what unit you need to move.
		final boolean found;
		if ((count == 0) && (unitLocation != null))
			found = selectNextUnitToMoveOverland ();
		else
		{
			// Even if we auto selected zero units, we still have to set these, since the player might then decide to select one of the units
			// manually to move it, in which case we need to know where its moving from
	
	        // This is the single only place unitMoveFrom is ever set
			unitMoveFrom = unitLocation;
	
			// Enable or disable the special order buttons like build city, purify, etc.
			enableOrDisableSpecialOrderButtons ();
			updateMovementRemaining ();
			found = true;
		}
		
		log.trace ("Exiting showSelectUnitBoxes = " + found);
		return found;
	}
	
	/**
	 * To be able to build cities and perform other special orders, there are a number of checks we need to do
	 * @throws RecordNotFoundException If a unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns a unit
	 * @throws MomException If we cannot find any appropriate experience level for a unit
	 */
	@Override
	public final void enableOrDisableSpecialOrderButtons () throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering enableOrDisableSpecialOrderButtons");
		
		// Count the number of units with various types of skill
		int settlerCount = 0;
		int spiritCount = 0;
		int purifyCount = 0;
		int engineerCount = 0;
		
		OverlandMapTerrainData terrainData = null;
		TileType tileType = null;
		
		if (unitMoveFrom != null)
		{
			terrainData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
				(unitMoveFrom.getZ ()).getRow ().get (unitMoveFrom.getY ()).getCell ().get (unitMoveFrom.getX ()).getTerrainData ();
			
			if (terrainData != null)
				tileType = getClient ().getClientDB ().findTileType (terrainData.getTileTypeID (), "enableOrDisableSpecialOrderButtons");
		
			for (final HideableComponent<SelectUnitButton> button : getOverlandMapRightHandPanel ().getSelectUnitButtons ())
				if ((button.getComponent ().isSelected ()) && (button.getComponent ().getUnit ().getOwningPlayerID () == getClient ().getOurPlayerID ()))
				{
					if (button.getComponent ().getUnit ().hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_CREATE_OUTPOST))
						settlerCount++;
					
					if (button.getComponent ().getUnit ().hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_MELD_WITH_NODE))						
						spiritCount++;

					if (button.getComponent ().getUnit ().hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_PURIFY))
						purifyCount++;

					if (button.getComponent ().getUnit ().hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_BUILD_ROAD))
						engineerCount++;
				}
		}
		
		// Can we create a new city?
		boolean createOutpostEnabled = false;
		if ((settlerCount == 1) && (terrainData != null))
		{
			final MapFeature mapFeature = (terrainData.getMapFeatureID () == null) ? null :
				getClient ().getClientDB ().findMapFeature (terrainData.getMapFeatureID (), "enableOrDisableSpecialOrderButtons");
			
			if (((Boolean.TRUE.equals (tileType.isCanBuildCity ())) &&
				((mapFeature == null) || (Boolean.TRUE.equals (mapFeature.isCanBuildCity ())))) &&
				(!getCityCalculations ().markWithinExistingCityRadius (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (), null,
					unitMoveFrom.getZ (), getClient ().getSessionDescription ().getOverlandMapSize ()).get (unitMoveFrom.getX (), unitMoveFrom.getY ())))
				
				createOutpostEnabled = true;
		}
		getOverlandMapRightHandPanel ().setCreateOutpostEnabled (createOutpostEnabled);
		
		// Can we meld with a node?
		boolean meldWithNodeEnabled = false;
		if ((spiritCount == 1) && (terrainData != null))
		{
			if ((tileType.getMagicRealmID () != null) && (!getClient ().getOurPlayerID ().equals (terrainData.getNodeOwnerID ())))
				meldWithNodeEnabled = true;
		}
		getOverlandMapRightHandPanel ().setMeldWithNodeEnabled (meldWithNodeEnabled);
		
		// Can we purify a corrupted tile?
		getOverlandMapRightHandPanel ().setPurifyEnabled ((purifyCount > 0) && (terrainData != null) && (terrainData.getCorrupted () != null));
		
		// Can we build a road?
		getOverlandMapRightHandPanel ().setBuildRoadEnabled ((engineerCount > 0) && (tileType != null) && (tileType.isLand () != null) && (tileType.isLand ()) &&
			(terrainData != null) && (terrainData.getRoadTileTypeID () == null));
		
		log.trace ("Exiting enableOrDisableSpecialOrderButtons");
	}
	
	/**
	 * Updates the indicator for how much movement the current unit stack has left
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	@Override
	public final void updateMovementRemaining () throws JAXBException, XMLStreamException, MomException
	{
		log.trace ("Entering updateMovementRemaining");
		
		final boolean ourTurn = (getClient ().getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS) ||
			(getClient ().getOurPlayerID ().equals (getClient ().getGeneralPublicKnowledge ().getCurrentPlayerID ()));
		
		// This counts the number of selectUnitButtons, i.e. the number of alive units at location 'unitMoveFrom',
		// even if they've already moved, have no movement left, or don't even belong to us
		int buttonCount = 0;
		
		// This finds the least doubleOverlandMovesLeft of any selected units
		// Force to zero if it isn't our turn, otherwise we're able to move our units in someone else's turn!
		int leastDoubleOverlandMovesLeft = ourTurn ? Integer.MAX_VALUE : 0;
		
		// Check all unit buttons
		final List<Integer> selectedUnitURNs = new ArrayList<Integer> ();
		for (final HideableComponent<SelectUnitButton> button : getOverlandMapRightHandPanel ().getSelectUnitButtons ())
			if (!button.isHidden ())
			{
				buttonCount++;
				
				if ((leastDoubleOverlandMovesLeft > 0) && (button.getComponent ().isSelected ()) && (button.getComponent ().getUnit ().getOwningPlayerID () == getClient ().getOurPlayerID ()))
				{
					leastDoubleOverlandMovesLeft = Math.min (leastDoubleOverlandMovesLeft, button.getComponent ().getUnit ().getDoubleOverlandMovesLeft ());
					selectedUnitURNs.add (button.getComponent ().getUnit ().getUnitURN ());
				}
			}
		
		// If we didn't find any units at all then we have no movement
		if (leastDoubleOverlandMovesLeft == Integer.MAX_VALUE)
			leastDoubleOverlandMovesLeft = 0;
		
		// Put the right hand panel into the correct mode
		if (buttonCount > 0)
		{
			getOverlandMapRightHandPanel ().setTop (OverlandMapRightHandPanelTop.UNITS);
			getOverlandMapRightHandPanel ().setBottom (OverlandMapRightHandPanelBottom.SPECIAL_ORDERS);
		}
		else
		{
			getOverlandMapRightHandPanel ().setTop (OverlandMapRightHandPanelTop.ECONOMY);
			
			if ((!processingContinuedMovement) && (ourTurn) && (unitsLeftToMoveOverland.size () == 0))
				getOverlandMapRightHandPanel ().setBottom (OverlandMapRightHandPanelBottom.NEXT_TURN_BUTTON);
			else
				getOverlandMapRightHandPanel ().setBottom (OverlandMapRightHandPanelBottom.PLAYER);
		}
		
		getOverlandMapRightHandPanel ().setPatrolEnabled (leastDoubleOverlandMovesLeft > 0);
		getOverlandMapRightHandPanel ().setDoneEnabled (leastDoubleOverlandMovesLeft > 0);
		
		if (leastDoubleOverlandMovesLeft == 0)
		{
			// No units picked to move - remove all shading from the map
			getOverlandMapUI ().setMovementTypes (null);
		}
		else
		{
			// Ask server to calculate distances to every point on the map
			final RequestOverlandMovementDistancesMessage msg = new RequestOverlandMovementDistancesMessage ();
			msg.setMoveFrom (unitMoveFrom);
			msg.getUnitURN ().addAll (selectedUnitURNs);
			getClient ().getServerConnection ().sendMessageToServer (msg);
		}			
		
		log.trace ("Exiting updateMovementRemaining");
	}
	
	/**
	 * @return Whether at least one of the select unit boxes is selected
	 */
	@Override
	public final boolean isAnyUnitSelectedToMove ()
	{
		// Following the logic above in updateMovementRemaining, the 'Done' button is enabled only when leastDoubleOverlandMovesLeft > 0
		// i.e. when we have a valid unit selected that has remaining movement
		return getOverlandMapRightHandPanel ().isDoneEnabled ();
	}

	/**
	 * @param unit Unit to test
	 * @return Whether the specified unit has a selected box - this doesn't imply we can move it, enemy units' boxes are permanently selected so their wizard colour shows 
	 */
	@Override
	public final boolean isUnitSelected (final MemoryUnit unit)
	{
		boolean found = false;
		boolean selected = false;
		
		final Iterator<HideableComponent<SelectUnitButton>> iter = getOverlandMapRightHandPanel ().getSelectUnitButtons ().iterator ();
		while ((!found) && (iter.hasNext ()))
		{
			final HideableComponent<SelectUnitButton> button = iter.next ();
			if (button.getComponent ().getUnit ().getUnit () == unit)
			{
				found = true;
				selected = button.getComponent ().isSelected ();
			}
		}
		
		return selected;
	}
	
	/**
	 * Removes all currently selected units from the 'units left to move' list, so that we won't ask the player about these units again this turn
	 * 
	 * @throws RecordNotFoundException If a unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns a unit
	 * @throws MomException If we cannot find any appropriate experience level for a unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final void selectedUnitsDone () throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		log.trace ("Entering selectedUnitsDone");

		for (final HideableComponent<SelectUnitButton> button : getOverlandMapRightHandPanel ().getSelectUnitButtons ())
			if ((button.getComponent ().isSelected ()) && (button.getComponent ().getUnit ().getOwningPlayerID () == getClient ().getOurPlayerID ()))
				unitsLeftToMoveOverland.remove (button.getComponent ().getUnit ().getMemoryUnit ());
		
		selectNextUnitToMoveOverland ();
		
		log.trace ("Exiting selectedUnitsDone");
	}
	
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
	@Override
	public final void selectedUnitsWait () throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		log.trace ("Entering selectedUnitsWait");

		for (final HideableComponent<SelectUnitButton> button : getOverlandMapRightHandPanel ().getSelectUnitButtons ())
			if ((button.getComponent ().isSelected ()) && (button.getComponent ().getUnit ().getOwningPlayerID () == getClient ().getOurPlayerID ()))
			{
				// Only put units back in the 'left to move' list if they already were in it - otherwise this can result in units who've already used
				// up all their movement being put back in the 'left to move' list which really screws things up.
				if (unitsLeftToMoveOverland.remove (button.getComponent ().getUnit ().getMemoryUnit ()))
					unitsLeftToMoveOverland.add (button.getComponent ().getUnit ().getMemoryUnit ());
			}
		
		selectNextUnitToMoveOverland ();
		
		log.trace ("Exiting selectedUnitsWait");
	}
	
	/**
	 * Sets all selected units into patrolling mode, so that we won't ask the player about these units again in this or subsequent turns
	 * 
	 * @throws RecordNotFoundException If a unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns a unit
	 * @throws MomException If we cannot find any appropriate experience level for a unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final void selectedUnitsPatrol () throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		log.trace ("Entering selectedUnitsPatrol");

		for (final HideableComponent<SelectUnitButton> button : getOverlandMapRightHandPanel ().getSelectUnitButtons ())
			if ((button.getComponent ().isSelected ()) && (button.getComponent ().getUnit ().getOwningPlayerID () == getClient ().getOurPlayerID ()))
			{
				unitsLeftToMoveOverland.remove (button.getComponent ().getUnit ().getMemoryUnit ());
				button.getComponent ().getUnit ().setSpecialOrder (UnitSpecialOrder.PATROL);
			}
		
		selectNextUnitToMoveOverland ();
		
		log.trace ("Exiting selectedUnitsPatrol");
	}
	
	/**
	 * Tells the server that we want to move the currently selected units to a different location on the overland map
	 * @param moveTo The place to move to
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	@Override
	public final void moveUnitStackTo (final MapCoordinates3DEx moveTo) throws JAXBException, XMLStreamException, MomException
	{
		log.trace ("Entering moveUnitStackTo: " + moveTo);

		final List<Integer> movingUnitURNs = new ArrayList<Integer> ();
		for (final HideableComponent<SelectUnitButton> button : getOverlandMapRightHandPanel ().getSelectUnitButtons ())
			if ((!button.isHidden ()) && (button.getComponent ().isSelected ()) && (button.getComponent ().getUnit ().getOwningPlayerID () == getClient ().getOurPlayerID ()))
				movingUnitURNs.add (button.getComponent ().getUnit ().getUnitURN ());
		
		if (movingUnitURNs.size () > 0)
		{
			final RequestMoveOverlandUnitStackMessage msg = new RequestMoveOverlandUnitStackMessage ();
			msg.setMoveFrom (unitMoveFrom);
			msg.setMoveTo (moveTo);
			msg.getUnitURN ().addAll (movingUnitURNs);
			
			getClient ().getServerConnection ().sendMessageToServer (msg);
		}
		
		log.trace ("Exiting moveUnitStackTo");
	}
	
	/**
	 * Tells the server that we want to have the currently selected unit(s) perform some special action,
	 * such as settlers building an outpost, engineers building a road, or magic spirits capturing a node. 
	 * 
	 * @param specialOrder Special order to perform
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	@Override
	public final void specialOrderButton (final UnitSpecialOrder specialOrder) throws JAXBException, XMLStreamException, MomException
	{
		log.trace ("Entering specialOrderButton: " + specialOrder);

		final List<Integer> movingUnitURNs = new ArrayList<Integer> ();
		for (final HideableComponent<SelectUnitButton> button : getOverlandMapRightHandPanel ().getSelectUnitButtons ())
			if ((!button.isHidden ()) && (button.getComponent ().isSelected ()) && (button.getComponent ().getUnit ().getOwningPlayerID () == getClient ().getOurPlayerID ()))
				movingUnitURNs.add (button.getComponent ().getUnit ().getUnitURN ());
		
		if (movingUnitURNs.size () > 0)
		{
			final SpecialOrderButtonMessage msg = new SpecialOrderButtonMessage ();
			msg.setMapLocation (unitMoveFrom);
			msg.setSpecialOrder (specialOrder);
			msg.getUnitURN ().addAll (movingUnitURNs);
			
			getClient ().getServerConnection ().sendMessageToServer (msg);
		}

		log.trace ("Exiting specialOrderButton");
	}
	
	/**
	 * @param unit Unit to remove from the unitsLeftToMoveOverland list
	 */
	@Override
	public final void removeUnitFromLeftToMoveOverland (final MemoryUnit unit)
	{
		unitsLeftToMoveOverland.remove (unit);
	}
	
	/**
	 * Tell the server we clicked the Next Turn button
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	@Override
	public final void nextTurnButton () throws JAXBException, XMLStreamException, MomException
	{
		log.trace ("Entering nextTurn");

		// Prevent doing anything with units after clicking next turn
		for (final HideableComponent<SelectUnitButton> button : getOverlandMapRightHandPanel ().getSelectUnitButtons ())
		{
			button.setHidden (true);
			button.getComponent ().setSelected (false);
			button.getComponent ().setUnit (null);
		}
		
		// Make sure UpdateMovementRemaining will hide the next turn button rather than showing it
		setProcessingContinuedMovement (true);
		updateMovementRemaining ();
		unitsLeftToMoveOverland.clear ();
		
		// Send message to server
		getClient ().getServerConnection ().sendMessageToServer (new NextTurnButtonMessage ());
			
		log.trace ("Exiting nextTurn");
	}
	
	/**
	 * @return Whether we're in the middle of the server processing and sending us pending moves
	 */
	@Override
	public final boolean isProcessingContinuedMovement ()
	{
		return processingContinuedMovement;
	}
	
	/**
	 * @param cont Whether we're in the middle of the server processing and sending us pending moves
	 */
	@Override
	public final void setProcessingContinuedMovement (final boolean cont)
	{
		processingContinuedMovement = cont;
	}

	/**
	 * Note this being non-null doesn't necessarily mean we have any units actually selected to move - can select/deselect units via the buttons in the right hand panel
	 * @return The map location we're currently selecting/deselecting units at ready to choose an order for them or tell them where to move/attack
	 */
	@Override
	public final MapCoordinates3DEx getUnitMoveFrom ()
	{
		return unitMoveFrom;
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
	 * @param util Unit utils
	 */
	public final void setUnitUtils (final UnitUtils util)
	{
		unitUtils = util;
	}
	
	/**
	 * @return Pending movement utils
	 */
	public final PendingMovementUtils getPendingMovementUtils ()
	{
		return pendingMovementUtils;
	}

	/**
	 * @param utils Pending movement utils
	 */
	public final void setPendingMovementUtils (final PendingMovementUtils utils)
	{
		pendingMovementUtils = utils;
	}

	/**
	 * @return City calculations
	 */
	public final CityCalculations getCityCalculations ()
	{
		return cityCalculations;
	}

	/**
	 * @param calc City calculations
	 */
	public final void setCityCalculations (final CityCalculations calc)
	{
		cityCalculations = calc;
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
	
	/**
	 * @return Overland map right hand panel showing economy etc
	 */
	public final OverlandMapRightHandPanel getOverlandMapRightHandPanel ()
	{
		return overlandMapRightHandPanel;
	}

	/**
	 * @param panel Overland map right hand panel showing economy etc
	 */
	public final void setOverlandMapRightHandPanel (final OverlandMapRightHandPanel panel)
	{
		overlandMapRightHandPanel = panel;
	}
}