package momime.client.process;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import jakarta.xml.bind.JAXBException;
import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.ui.components.HideableComponent;
import momime.client.ui.components.SelectUnitButton;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.ui.frames.OverlandMapUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.client.ui.panels.OverlandMapRightHandPanelBottom;
import momime.client.ui.panels.OverlandMapRightHandPanelTop;
import momime.common.MomException;
import momime.common.calculations.CityCalculations;
import momime.common.calculations.UnitCalculations;
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
import momime.common.messages.clienttoserver.SpecialOrderButtonMessage;
import momime.common.movement.OverlandMovementCell;
import momime.common.movement.UnitMovement;
import momime.common.movement.UnitStack;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.PendingMovementUtils;
import momime.common.utils.UnitVisibilityUtils;

/**
 * Methods dealing with the turn sequence and overland movement that are too big to leave in
 * message implementations, or are used multiple times. 
 */
public final class OverlandMapProcessingImpl implements OverlandMapProcessing
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (OverlandMapProcessingImpl.class);
	
	/** Ordered list of units that we have to give orders to this turn */
	private final List<MemoryUnit> unitsLeftToMoveOverland = new ArrayList<MemoryUnit> ();
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Multiplayer client */
	private MomClient client;

	/** Methods dealing with checking whether we can see units or not */
	private UnitVisibilityUtils unitVisibilityUtils;
	
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
	
	/** Methods dealing with unit movement */
	private UnitMovement unitMovement;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** MemoryGridCell utils */
	private MemoryGridCellUtils memoryGridCellUtils;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
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
		// Search for units at this location.  Note unlike buildUnitsLeftToMoveList, which ignores units with no movement, pending movement or
		// special orders, here we want any unit as long as its alive and at the right location.
		final Iterator<HideableComponent<SelectUnitButton>> buttonIter = getOverlandMapRightHandPanel ().getSelectUnitButtons ().iterator ();
		
		int count = 0;
		if (unitLocation != null)
			for (final MemoryUnit mu : getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ())
				if ((unitLocation.equals (mu.getUnitLocation ())) && (mu.getStatus () == UnitStatusID.ALIVE) &&
					(getUnitVisibilityUtils ().canSeeUnitOverland (mu, getClient ().getOurPlayerID (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (), getClient ().getClientDB ())))
				{
					count++;
					if (buttonIter.hasNext ())
					{
						final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (mu, null, null, null,
							getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());
						
						final HideableComponent<SelectUnitButton> button = buttonIter.next ();
						button.getComponent ().setUnit (xu);
						button.getComponent ().setSelected (unitsLeftToMoveOverland.contains (mu));	// Pre-select this unit as long as it hasn't already passed its allocated movement sequence
						button.setHidden (false);
					}
					else
						log.warn ("Ran out of select unit boxes when trying to show units at " + unitLocation + " (found " + count + " units)");
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
			getOverlandMapUI ().setUnitMoveFrom (unitLocation);
	
			// Enable or disable the special order buttons like build city, purify, etc.
			enableOrDisableSpecialOrderButtons ();
			updateMovementRemaining ();
			found = true;
		}
		
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
		// Count the number of units with various types of skill
		int settlerCount = 0;
		int spiritCount = 0;
		int purifyCount = 0;
		int engineerCount = 0;
		int planeShiftCount = 0;
		
		OverlandMapTerrainData terrainData = null;
		TileType tileType = null;
		
		if (getOverlandMapUI ().getUnitMoveFrom () != null)
		{
			terrainData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
				(getOverlandMapUI ().getUnitMoveFrom ().getZ ()).getRow ().get (getOverlandMapUI ().getUnitMoveFrom ().getY ()).getCell ().get (getOverlandMapUI ().getUnitMoveFrom ().getX ()).getTerrainData ();
			
			if (terrainData != null)
				tileType = getClient ().getClientDB ().findTileType (terrainData.getTileTypeID (), "enableOrDisableSpecialOrderButtons");
		
			for (final HideableComponent<SelectUnitButton> button : getOverlandMapRightHandPanel ().getSelectUnitButtons ())
				if ((button.getComponent ().isSelected ()) && (button.getComponent ().getUnit ().getOwningPlayerID () == getClient ().getOurPlayerID ()))
				{
					final ExpandedUnitDetails xu = button.getComponent ().getUnit ();
					if (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_CREATE_OUTPOST))
						settlerCount++;
					
					if (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_MELD_WITH_NODE))						
						spiritCount++;

					if (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_PURIFY))
						purifyCount++;

					if (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_BUILD_ROAD))
						engineerCount++;

					boolean planeShift = false;
					for (final String planeShiftSkillID : CommonDatabaseConstants.UNIT_SKILL_IDS_PLANE_SHIFT)
						if (xu.hasModifiedSkill (planeShiftSkillID))
							planeShift = true;

					if (planeShift)
						planeShiftCount++;
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
					getOverlandMapUI ().getUnitMoveFrom ().getZ (), getClient ().getSessionDescription ().getOverlandMapSize ()).get
						(getOverlandMapUI ().getUnitMoveFrom ().getX (), getOverlandMapUI ().getUnitMoveFrom ().getY ())))
				
				createOutpostEnabled = true;
		}
		getOverlandMapRightHandPanel ().setCreateOutpostEnabled (createOutpostEnabled);
		
		// Can we meld with a node?
		boolean meldWithNodeEnabled = false;
		if ((spiritCount == 1) && (terrainData != null))
		{
			if ((tileType.getMagicRealmID () != null) && (!getClient ().getOurPlayerID ().equals (terrainData.getNodeOwnerID ())) &&
				((terrainData.isWarped () == null) || (!terrainData.isWarped ())))
				meldWithNodeEnabled = true;
		}
		getOverlandMapRightHandPanel ().setMeldWithNodeEnabled (meldWithNodeEnabled);
		
		// Can we purify a corrupted tile?
		getOverlandMapRightHandPanel ().setPurifyEnabled ((purifyCount > 0) && (terrainData != null) && (terrainData.getCorrupted () != null));
		
		// Can we build a road?
		getOverlandMapRightHandPanel ().setBuildRoadEnabled ((engineerCount > 0) && (tileType != null) && (tileType.isLand () != null) && (tileType.isLand ()) &&
			(terrainData != null) && (terrainData.getRoadTileTypeID () == null));
		
		// Can we jump to the other plane?
		// Not allowed if we're standing on a tower, or someone has Planar Seal cast
		getOverlandMapRightHandPanel ().setPlaneShiftEnabled ((planeShiftCount > 0) &&
			(!(getMemoryGridCellUtils ().isTerrainTowerOfWizardry (terrainData))) &&
			(getMemoryMaintainedSpellUtils ().findMaintainedSpell (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
				null, CommonDatabaseConstants.SPELL_ID_PLANAR_SEAL, null, null, null, null) == null));
	}
	
	/**
	 * Updates the indicator for how much movement the current unit stack has left
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If a unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns a unit
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	@Override
	public final void updateMovementRemaining ()
		throws JAXBException, XMLStreamException, PlayerNotFoundException, RecordNotFoundException, MomException
	{
		final boolean ourTurn = (getClient ().getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS) ||
			(getClient ().getOurPlayerID ().equals (getClient ().getGeneralPublicKnowledge ().getCurrentPlayerID ()));
		
		// This counts the number of selectUnitButtons, i.e. the number of alive units at location 'unitMoveFrom',
		// even if they've already moved, have no movement left, or don't even belong to us
		int buttonCount = 0;
		
		// Check all unit buttons
		final List<ExpandedUnitDetails> selectedUnits = new ArrayList<ExpandedUnitDetails> ();
		for (final HideableComponent<SelectUnitButton> button : getOverlandMapRightHandPanel ().getSelectUnitButtons ())
			if (!button.isHidden ())
			{
				buttonCount++;
				
				if ((ourTurn) && (button.getComponent ().isSelected ()) && (button.getComponent ().getUnit ().getOwningPlayerID () == getClient ().getOurPlayerID ()))
					selectedUnits.add (button.getComponent ().getUnit ());
			}

		int doubleMovementRemaining = Integer.MAX_VALUE;
		UnitStack unitStack = null;
		if (selectedUnits.size () > 0)
		{
			unitStack = getUnitCalculations ().createUnitStack (selectedUnits, getClient ().getPlayers (),
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());
			
			// Get the list of units who are actually moving
			final List<ExpandedUnitDetails> movingUnits = (unitStack.getTransports ().size () > 0) ? unitStack.getTransports () : unitStack.getUnits ();
			for (final ExpandedUnitDetails thisUnit : movingUnits)
				if (thisUnit.getDoubleOverlandMovesLeft () < doubleMovementRemaining)
					doubleMovementRemaining = thisUnit.getDoubleOverlandMovesLeft ();
		}
		
		// If we didn't find any units at all then we have no movement
		if (doubleMovementRemaining == Integer.MAX_VALUE)
			doubleMovementRemaining = 0;
		
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
		
		getOverlandMapRightHandPanel ().setPatrolEnabled (doubleMovementRemaining > 0);
		getOverlandMapRightHandPanel ().setDoneEnabled (doubleMovementRemaining > 0);
		
		if (doubleMovementRemaining == 0)
		{
			// No units picked to move - remove all shading from the map
			getOverlandMapUI ().setMoves (null);
		}
		else
		{
			// Calculate distances to every point on the map
			final OverlandMovementCell [] [] [] moves = getUnitMovement ().calculateOverlandMovementDistances (getOverlandMapUI ().getUnitMoveFrom (),
				getClient ().getOurPlayerID (), unitStack, doubleMovementRemaining,  getClient ().getPlayers (),
				getClient ().getSessionDescription ().getOverlandMapSize (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (),
				getClient ().getClientDB ());
			
			// Client side tweak for towers - movement routines know that towers exist on plane 0 and so if trying to move onto a tower from Myrror,
			// you would have to click it on Arcanus.  But want to show the tower as moveable on Myrror as well.
			for (int y = 0; y < getClient ().getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
				for (int x = 0; x < getClient ().getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
					if ((moves [1] [y] [x] == null) && (moves [0] [y] [x] != null))
					{
						final OverlandMapTerrainData terrainData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
							(0).getRow ().get (y).getCell ().get (x).getTerrainData ();
						if (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (terrainData))
							moves [1] [y] [x] = moves [0] [y] [x];
					}			

			getOverlandMapUI ().setMoves (moves);
		}			
		getOverlandMapUI ().regenerateMovementTypesBitmap ();
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
			if ((button.getComponent ().getUnit () != null) && (button.getComponent ().getUnit ().getUnit () == unit))
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
		for (final HideableComponent<SelectUnitButton> button : getOverlandMapRightHandPanel ().getSelectUnitButtons ())
			if ((button.getComponent ().isSelected ()) && (button.getComponent ().getUnit ().getOwningPlayerID () == getClient ().getOurPlayerID ()))
				unitsLeftToMoveOverland.remove (button.getComponent ().getUnit ().getMemoryUnit ());
		
		selectNextUnitToMoveOverland ();
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
		for (final HideableComponent<SelectUnitButton> button : getOverlandMapRightHandPanel ().getSelectUnitButtons ())
			if ((button.getComponent ().isSelected ()) && (button.getComponent ().getUnit ().getOwningPlayerID () == getClient ().getOurPlayerID ()))
			{
				// Only put units back in the 'left to move' list if they already were in it - otherwise this can result in units who've already used
				// up all their movement being put back in the 'left to move' list which really screws things up.
				if (unitsLeftToMoveOverland.remove (button.getComponent ().getUnit ().getMemoryUnit ()))
					unitsLeftToMoveOverland.add (button.getComponent ().getUnit ().getMemoryUnit ());
			}
		
		selectNextUnitToMoveOverland ();
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
		for (final HideableComponent<SelectUnitButton> button : getOverlandMapRightHandPanel ().getSelectUnitButtons ())
			if ((button.getComponent ().isSelected ()) && (button.getComponent ().getUnit ().getOwningPlayerID () == getClient ().getOurPlayerID ()))
			{
				unitsLeftToMoveOverland.remove (button.getComponent ().getUnit ().getMemoryUnit ());
				button.getComponent ().getUnit ().setSpecialOrder (UnitSpecialOrder.PATROL);
			}
		
		selectNextUnitToMoveOverland ();
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
		final List<Integer> movingUnitURNs = new ArrayList<Integer> ();
		for (final HideableComponent<SelectUnitButton> button : getOverlandMapRightHandPanel ().getSelectUnitButtons ())
			if ((!button.isHidden ()) && (button.getComponent ().isSelected ()) && (button.getComponent ().getUnit ().getOwningPlayerID () == getClient ().getOurPlayerID ()))
				movingUnitURNs.add (button.getComponent ().getUnit ().getUnitURN ());
		
		if (movingUnitURNs.size () > 0)
		{
			// If moving onto a tower then force moveTo plane to be 0
			if ((moveTo.getZ () > 0) && (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
				(moveTo.getZ ()).getRow ().get (moveTo.getY ()).getCell ().get (moveTo.getX ()).getTerrainData ())))
				
				moveTo.setZ (0);
			
			final RequestMoveOverlandUnitStackMessage msg = new RequestMoveOverlandUnitStackMessage ();
			msg.setMoveFrom (getOverlandMapUI ().getUnitMoveFrom ());
			msg.setMoveTo (moveTo);
			msg.getUnitURN ().addAll (movingUnitURNs);
			
			getClient ().getServerConnection ().sendMessageToServer (msg);
		}
	}
	
	/**
	 * Tells the server that we want to have the currently selected unit(s) perform some special action,
	 * such as settlers building an outpost, engineers building a road, or magic spirits capturing a node. 
	 * 
	 * @param specialOrder Special order to perform
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws IOException If the unit whose details we are storing is not a MemoryUnit 
	 */
	@Override
	public final void specialOrderButton (final UnitSpecialOrder specialOrder) throws JAXBException, XMLStreamException, IOException
	{
		final List<Integer> movingUnitURNs = new ArrayList<Integer> ();
		for (final HideableComponent<SelectUnitButton> button : getOverlandMapRightHandPanel ().getSelectUnitButtons ())
			if ((!button.isHidden ()) && (button.getComponent ().isSelected ()) && (button.getComponent ().getUnit ().getOwningPlayerID () == getClient ().getOurPlayerID ()))
				movingUnitURNs.add (button.getComponent ().getUnit ().getUnitURN ());
		
		if (movingUnitURNs.size () > 0)
		{
			if (!getClient ().isPlayerTurn ())
			{
				final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
				msg.setLanguageTitle (getLanguages ().getOverlandMapScreen ().getMapRightHandBar ().getNotYourTurnTitle ());
				msg.setLanguageText (getLanguages ().getOverlandMapScreen ().getMapRightHandBar ().getNotYourTurn ());
				msg.setVisible (true);
			}
			else
			{
				final SpecialOrderButtonMessage msg = new SpecialOrderButtonMessage ();
				msg.setMapLocation (getOverlandMapUI ().getUnitMoveFrom ());
				msg.setSpecialOrder (specialOrder);
				msg.getUnitURN ().addAll (movingUnitURNs);
				
				getClient ().getServerConnection ().sendMessageToServer (msg);
			}
		}
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
	 * @throws RecordNotFoundException If a unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns a unit
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	@Override
	public final void nextTurnButton ()
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
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
	 * @return Language database holder
	 */
	public final LanguageDatabaseHolder getLanguageHolder ()
	{
		return languageHolder;
	}
	
	/**
	 * @param holder Language database holder
	 */
	public final void setLanguageHolder (final LanguageDatabaseHolder holder)
	{
		languageHolder = holder;
	}

	/**
	 * Convenience shortcut for accessing the Language XML database
	 * @return New singular language XML
	 */
	public final MomLanguagesEx getLanguages ()
	{
		return getLanguageHolder ().getLanguages ();
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
	 * @return Methods dealing with checking whether we can see units or not
	 */
	public final UnitVisibilityUtils getUnitVisibilityUtils ()
	{
		return unitVisibilityUtils;
	}

	/**
	 * @param u Methods dealing with checking whether we can see units or not
	 */
	public final void setUnitVisibilityUtils (final UnitVisibilityUtils u)
	{
		unitVisibilityUtils = u;
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

	/**
	 * @return Methods dealing with unit movement
	 */
	public final UnitMovement getUnitMovement ()
	{
		return unitMovement;
	}

	/**
	 * @param u Methods dealing with unit movement
	 */
	public final void setUnitMovement (final UnitMovement u)
	{
		unitMovement = u;
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
	 * @return MemoryGridCell utils
	 */
	public final MemoryGridCellUtils getMemoryGridCellUtils ()
	{
		return memoryGridCellUtils;
	}

	/**
	 * @param utils MemoryGridCell utils
	 */
	public final void setMemoryGridCellUtils (final MemoryGridCellUtils utils)
	{
		memoryGridCellUtils = utils;
	}

	/**
	 * @return MemoryMaintainedSpell utils
	 */
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param spellUtils MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils spellUtils)
	{
		memoryMaintainedSpellUtils = spellUtils;
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

	/**
	 * @return Prototype frame creator
	 */
	public final PrototypeFrameCreator getPrototypeFrameCreator ()
	{
		return prototypeFrameCreator;
	}

	/**
	 * @param obj Prototype frame creator
	 */
	public final void setPrototypeFrameCreator (final PrototypeFrameCreator obj)
	{
		prototypeFrameCreator = obj;
	}
}