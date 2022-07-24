package momime.server.worldupdates;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.utils.random.RandomUtils;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.calculations.UnitCalculations;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MemoryUnit;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitStatusID;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryGridCellUtils;
import momime.server.MomSessionVariables;
import momime.server.fogofwar.KillUnitActionID;

/**
 * World update for rechecking that units in an overland map cell are a valid stack for the type of terrain, e.g.. that there are enough boats to carry everyone that can't swim 
 */
public final class RecheckTransportCapacityUpdate implements WorldUpdate
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (RecheckTransportCapacityUpdate.class);
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** MemoryGridCell utils */
	private MemoryGridCellUtils memoryGridCellUtils;
	
	/** Random utils */
	private RandomUtils randomUtils; 
	
	/** The map location to check */
	private MapCoordinates3DEx mapLocation;
	
	/**
	 * @return Enum indicating which kind of update this is
	 */
	@Override
	public final KindOfWorldUpdate getKindOfWorldUpdate ()
	{
		return KindOfWorldUpdate.RECHECK_TRANSPORT_CAPACITY;
	}
	
	/**
	 * @param o Other object to compare against
	 * @return Whether this and the other object hold the same values
	 */
	@Override
	public final boolean equals (final Object o)
	{
		final boolean e;
		if (o instanceof RecheckTransportCapacityUpdate)
			e = getMapLocation ().equals (((RecheckTransportCapacityUpdate) o).getMapLocation ());
		else
			e = false;
		
		return e;
	}
	
	/**
	 * @return String representation of class, for debug messages
	 */
	@Override
	public final String toString ()
	{
		return "Recheck transport capacity at " + getMapLocation ();
	}
	
	/**
	 * Processes this update
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether this update was processed and/or generated any further updates
	 * @throws JAXBException If there is a problem sending some message to the client
	 * @throws XMLStreamException If there is a problem sending some message to the client
	 * @throws RecordNotFoundException If we find a game element (unit, building or so on) that we can't find the definition for in the DB
	 * @throws PlayerNotFoundException If we can't find the player who owns a game element
	 * @throws MomException If there are any issues with data or calculation logic
	 */
	@Override
	public final WorldUpdateResult process (final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		WorldUpdateResult result = WorldUpdateResult.DONE;
		
		final OverlandMapTerrainData terrainData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(getMapLocation ().getZ ()).getRow ().get (getMapLocation ().getY ()).getCell ().get (getMapLocation ().getX ()).getTerrainData ();
		
		// List all the units at this location owned by this player
		final List<ExpandedUnitDetails> unitStack = new ArrayList<ExpandedUnitDetails> ();
		for (final MemoryUnit tu : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
			if ((tu.getStatus () == UnitStatusID.ALIVE) && (getMapLocation ().equals (tu.getUnitLocation ())))
				unitStack.add (getExpandUnitDetails ().expandUnitDetails (tu, null, null, null, mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ()));
		
		// Get a list of the unit stack skills
		final Set<String> unitStackSkills = getUnitCalculations ().listAllSkillsInUnitStack (unitStack);
		
		// Now check each unit in the stack
		final List<ExpandedUnitDetails> impassableUnits = new ArrayList<ExpandedUnitDetails> ();
		int spaceRequired = 0;
		for (final ExpandedUnitDetails tu : unitStack)
		{
			final boolean impassable = (getUnitCalculations ().calculateDoubleMovementToEnterTileType (tu, unitStackSkills,
				getMemoryGridCellUtils ().convertNullTileTypeToFOW (terrainData, false), mom.getServerDB ()) == null);
				
			// Count space granted by transports
			final Integer unitTransportCapacity = tu.getUnitDefinition ().getTransportCapacity ();
			if ((unitTransportCapacity != null) && (unitTransportCapacity > 0))
			{
				// Transports on impassable terrain just get killed (maybe a ship had its flight spell dispelled during an overland combat)
				if (impassable)
				{
					log.debug ("Killing Unit URN " + tu.getUnitURN () + " (transport on impassable terrain)");
					if (mom.getWorldUpdates ().killUnit (tu.getMemoryUnit ().getUnitURN (), KillUnitActionID.HEALABLE_OVERLAND_DAMAGE))
						result = WorldUpdateResult.DONE_AND_LATER_UPDATES_ADDED;
				}
				else
					spaceRequired = spaceRequired - unitTransportCapacity;
			}
			else if (impassable)
			{
				spaceRequired++;
				impassableUnits.add (tu);
			}
		}
		
		// Need to kill off any units?
		while ((spaceRequired > 0) && (impassableUnits.size () > 0))
		{
			final ExpandedUnitDetails killUnit = impassableUnits.get (getRandomUtils ().nextInt (impassableUnits.size ()));
			log.debug ("Killing Unit URN " + killUnit.getUnitURN () + " (unit on impassable terrain)");
			
			if (mom.getWorldUpdates ().killUnit (killUnit.getMemoryUnit ().getUnitURN (), KillUnitActionID.HEALABLE_OVERLAND_DAMAGE))
				result = WorldUpdateResult.DONE_AND_LATER_UPDATES_ADDED;
			
			spaceRequired--;
			impassableUnits.remove (killUnit);
		}
		
		return result;
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
	 * @return Random utils
	 */
	public final RandomUtils getRandomUtils ()
	{
		return randomUtils;
	}

	/**
	 * @param utils Random utils
	 */
	public final void setRandomUtils (final RandomUtils utils)
	{
		randomUtils = utils;
	}
	
	/**
	 * @return The map location to check
	 */
	public final MapCoordinates3DEx getMapLocation ()
	{
		return mapLocation;
	}

	/**
	 * @param l The map location to check
	 */
	public final void setMapLocation (final MapCoordinates3DEx l)
	{
		mapLocation = l;
	}
}