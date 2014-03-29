package momime.server.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.v0_9_4.UnitSettingData;
import momime.common.database.v0_9_4.UnitHasSkill;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.servertoclient.v0_9_4.SetSpecialOrderMessage;
import momime.common.messages.v0_9_4.AvailableUnit;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MemoryGridCell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.UnitAddBumpTypeID;
import momime.common.messages.v0_9_4.UnitSpecialOrder;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.common.utils.PendingMovementUtils;
import momime.common.utils.UnitUtils;
import momime.server.calculations.MomServerUnitCalculations;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_4.Unit;
import momime.server.database.v0_9_4.UnitSkill;
import momime.server.messages.ServerMemoryGridCellUtils;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.random.RandomUtils;

/**
 * Server side only helper methods for dealing with units
 */
public final class UnitServerUtilsImpl implements UnitServerUtils
{
	/** Class logger */
	private final Logger log = Logger.getLogger (UnitServerUtilsImpl.class.getName ());
	
	/** Unit utils */
	private UnitUtils unitUtils;

	/** Pending movement utils */
	private PendingMovementUtils pendingMovementUtils;
	
	/** Server-only unit calculations */
	private MomServerUnitCalculations serverUnitCalculations;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/**
	 * Chooses a name for this hero (out of 5 possibilities) and rolls their random skills
	 * @param unit The hero to generate name and skills for
	 * @param db Lookup lists built over the XML database
	 * @throws MomException If we find a hero who has no possible names defined, or who needs a random skill and we can't find a suitable one
	 * @throws RecordNotFoundException If we can't find the definition for the unit
	 */
	@Override
	public final void generateHeroNameAndRandomSkills (final MemoryUnit unit, final ServerDatabaseEx db)
		throws MomException, RecordNotFoundException
	{
		log.entering (UnitServerUtilsImpl.class.getName (), "generateHeroNameAndRandomSkills", unit.getUnitID ());

		final Unit unitDefinition = db.findUnit (unit.getUnitID (), "generateHeroNameAndRandomSkills");

		// Pick a name at random
		if (unitDefinition.getHeroName ().size () == 0)
			throw new MomException ("No hero names found for unit ID \"" + unit.getUnitID () + "\"");

		unit.setHeroNameID (unitDefinition.getHeroName ().get (getRandomUtils ().nextInt (unitDefinition.getHeroName ().size ())).getHeroNameID ());

		// Any random skills to add?
		if ((unitDefinition.getHeroRandomPickCount () != null) && (unitDefinition.getHeroRandomPickCount () > 0))
		{
			log.finest ("Hero " + unit.getUnitID () + "' belonging to player ID " + unit.getOwningPlayerID () + " skills before rolling extras: " + getUnitUtils ().describeBasicSkillValuesInDebugString (unit));

			// Run once for each random skill we get
			for (int pickNo = 0; pickNo < unitDefinition.getHeroRandomPickCount (); pickNo++)
			{
				// Get a list of all valid choices
				final List<String> skillChoices = new ArrayList<String> ();
				for (final UnitSkill thisSkill : db.getUnitSkill ())
				{
					// We can spot hero skills since they'll have at least one of these values filled in
					if (((thisSkill.getHeroSkillTypeID () != null) && (!thisSkill.getHeroSkillTypeID ().equals (""))) ||
						((thisSkill.isOnlyIfHaveAlready () != null) && (thisSkill.isOnlyIfHaveAlready ())) ||
						((thisSkill.getMaxOccurrences () != null) && (thisSkill.getMaxOccurrences () > 0)))
					{
						// Its a hero skill - do we have it already?
						final int currentSkillValue = getUnitUtils ().getBasicSkillValue (unit.getUnitHasSkill (), thisSkill.getUnitSkillID ());

						// Is it applicable?
						// If the unit has no hero random pick type specified, then it means it can use any hero skills
						// If the skill has no hero random pick type specified, then it means it can be used by any units
						if (((unitDefinition.getHeroRandomPickType () == null) || (thisSkill.getHeroSkillTypeID () == null) || (unitDefinition.getHeroRandomPickType ().equals (thisSkill.getHeroSkillTypeID ()))) &&
							((thisSkill.isOnlyIfHaveAlready () == null) || (!thisSkill.isOnlyIfHaveAlready ()) || (currentSkillValue > 0)) &&
							((thisSkill.getMaxOccurrences () == null) || (currentSkillValue < thisSkill.getMaxOccurrences ())))

								skillChoices.add (thisSkill.getUnitSkillID ());
					}
				}

				// Pick one
				if (skillChoices.size () == 0)
					throw new MomException ("generateHeroNameAndRandomSkills: No valid hero skill choices for unit " + unit.getUnitID ());

				final String skillID = skillChoices.get (getRandomUtils ().nextInt (skillChoices.size ()));
				final int currentSkillValue = getUnitUtils ().getBasicSkillValue (unit.getUnitHasSkill (), skillID);
				if (currentSkillValue >= 0)
					getUnitUtils ().setBasicSkillValue (unit, skillID, currentSkillValue + 1);
				else
				{
					final UnitHasSkill newSkill = new UnitHasSkill ();
					newSkill.setUnitSkillID (skillID);
					newSkill.setUnitSkillValue (1);
					unit.getUnitHasSkill ().add (newSkill);
				}

				log.finest ("Hero " + unit.getUnitID () + "' belonging to player ID " + unit.getOwningPlayerID () + " skills after rolling extras: " + getUnitUtils ().describeBasicSkillValuesInDebugString (unit));
			}
		}

		// Update status
		unit.setStatus (UnitStatusID.GENERATED);

		log.exiting (UnitServerUtilsImpl.class.getName (), "generateHeroNameAndRandomSkills");
	}

	/**
	 * @param order Special order that a unit has
	 * @return True if this special order results in the unit dying/being removed from play
	 */
	@Override
	public final boolean doesUnitSpecialOrderResultInDeath (final UnitSpecialOrder order)
	{
		return (order == UnitSpecialOrder.BUILD_CITY) || (order == UnitSpecialOrder.MELD_WITH_NODE) || (order == UnitSpecialOrder.DISMISS);
	}
	
	/**
	 * Sets a special order on a unit, and sends the special order to the player owning the unit
	 * 
	 * @param trueUnit Unit to give an order to
	 * @param specialOrder Order to give to this unit
	 * @param player Player who owns the unit
	 * @throws RecordNotFoundException If we can't find the unit in the player's memory (they don't know about their own unit?)
	 * @throws JAXBException If there is a problem sending the message to the client
	 * @throws XMLStreamException If there is a problem sending the message to the client
	 */
	@Override
	public final void setAndSendSpecialOrder (final MemoryUnit trueUnit, final UnitSpecialOrder specialOrder, final PlayerServerDetails player)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		log.entering (UnitServerUtilsImpl.class.getName (), "findUnitWithPlayerAndID",
			new String [] {player.getPlayerDescription ().getPlayerName (), new Integer (trueUnit.getUnitURN ()).toString (), specialOrder.toString ()});
		
		// Setting a special order cancels any other kind of move
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge (); 
		final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();
		getPendingMovementUtils ().removeUnitFromAnyPendingMoves (trans.getPendingMovement (), trueUnit.getUnitURN ());
		
		// Set in true memory
		trueUnit.setSpecialOrder (specialOrder);
		
		// Set in server's copy of player memory
		getUnitUtils ().findUnitURN (trueUnit.getUnitURN (), priv.getFogOfWarMemory ().getUnit (), "setAndSendSpecialOrder").setSpecialOrder (specialOrder);
		
		// Set in client's copy of player memory
		final SetSpecialOrderMessage msg = new SetSpecialOrderMessage ();
		msg.setSpecialOrder (specialOrder);
		msg.getUnitURN ().add (trueUnit.getUnitURN ());
		player.getConnection ().sendMessageToClient (msg);

		log.exiting (UnitServerUtilsImpl.class.getName (), "findUnitWithPlayerAndID");
	}

	/**
	 * @param units List of units to search through
	 * @param playerID Player to search for
	 * @param unitID Unit ID to search for
	 * @return Unit with requested ID belonging to the requested player, or null if not found
	 */
	@Override
	public final MemoryUnit findUnitWithPlayerAndID (final List<MemoryUnit> units, final int playerID, final String unitID)
	{
		log.entering (UnitServerUtilsImpl.class.getName (), "findUnitWithPlayerAndID", new String [] {new Integer (playerID).toString (), unitID});

		MemoryUnit result = null;
		final Iterator<MemoryUnit> iter = units.iterator ();
		while ((result == null) && (iter.hasNext ()))
		{
			final MemoryUnit thisUnit = iter.next ();
			if ((thisUnit.getOwningPlayerID () == playerID) && (thisUnit.getUnitID ().equals (unitID)))
				result = thisUnit;
		}

		log.exiting (UnitServerUtilsImpl.class.getName (), "findUnitWithPlayerAndID", result);
		return result;
	}

	/**
	 * Internal method used by findNearestLocationWhereUnitCanBeAdded
	 * @param addLocation Location that we're trying to add a unit
	 * @param testUnit Type of unit that we're trying to add
	 * @param testUnitSkills The skills that testUnit has
	 * @param trueMap Server's true knowledge of terrain, units and so on
	 * @param settings Unit settings from session description
	 * @param db Lookup lists built over the XML database
	 * @return Whether unit can be added here or not
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found
	 */
	final boolean canUnitBeAddedHere (final OverlandMapCoordinatesEx addLocation, final AvailableUnit testUnit, final List<String> testUnitSkills,
		final FogOfWarMemory trueMap, final UnitSettingData settings, final ServerDatabaseEx db)
		throws RecordNotFoundException
	{
		log.entering (UnitServerUtilsImpl.class.getName (), "canUnitBeAddedHere", addLocation);

		// Any other units here?
		final boolean unitCheckOk;
		final MemoryUnit unitHere = getUnitUtils ().findFirstAliveEnemyAtLocation (trueMap.getUnit (), addLocation.getX (), addLocation.getY (), addLocation.getZ (), 0);
		if (unitHere != null)
		{
			// Do we own it?
			if (unitHere.getOwningPlayerID () == testUnit.getOwningPlayerID ())
				// Maximum number of units already in the cell?
				unitCheckOk = (getUnitUtils ().countAliveEnemiesAtLocation (trueMap.getUnit (), addLocation.getX (), addLocation.getY (), addLocation.getZ (), 0) < settings.getUnitsPerMapCell ());
			else
				// Enemy unit in the cell, so we can't add here
				unitCheckOk = false;
		}
		else
			unitCheckOk = true;

		// If ok so far, check the unit is allowed on this type of terrain

		// Also specifically check for nodes/lairs/towers, since although we've checked for units, we can't place a unit on top of an empty lair either
		// Technically if we own a cleared tower or node, we could place units there, but that's hard to deal with since we need to use the players'
		// knowledge that the tower has been cleared so then this can't just be TrueMap-based anymore
		final boolean okToAdd;
		if (!unitCheckOk)
			okToAdd = false;
		else
		{
			final MemoryGridCell tc = trueMap.getMap ().getPlane ().get (addLocation.getZ ()).getRow ().get (addLocation.getY ()).getCell ().get (addLocation.getX ());
			if ((ServerMemoryGridCellUtils.isNodeLairTower (tc.getTerrainData (), db)) ||
				(getServerUnitCalculations ().calculateDoubleMovementToEnterTileType (testUnit, testUnitSkills, tc.getTerrainData ().getTileTypeID (), trueMap.getMaintainedSpell (), db) == null))

				okToAdd = false;
			else
			{
				// Lastly check for someone else's empty city (although to have two adjacent cities would be a bit weird)
				if ((tc.getCityData () != null) && (tc.getCityData ().getCityPopulation () != null) && (tc.getCityData ().getCityOwnerID () != null) &&
					(tc.getCityData ().getCityPopulation () > 0) && (tc.getCityData ().getCityOwnerID () != testUnit.getOwningPlayerID ()))

					okToAdd = false;
				else
					okToAdd = true;
			}
		}

		log.exiting (UnitServerUtilsImpl.class.getName (), "canUnitBeAddedHere", okToAdd);
		return okToAdd;
	}

	/**
	 * When a unit is built or summoned, works out where to put it
	 * If the city is already full, will resort to bumping the new unit into one of the outlying 8 squares
	 *
	 * @param desiredLocation Location that we're trying to add a unit
	 * @param unitID Type of unit that we're trying to add
	 * @param playerID Player who is trying to add the unit
	 * @param trueMap Server's true knowledge of terrain, units and so on
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @return Location + bump type; note class and bump type will always be filled in, but location may be null if the unit cannot fit anywhere
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found
	 */
	@Override
	public final UnitAddLocation findNearestLocationWhereUnitCanBeAdded (final OverlandMapCoordinatesEx desiredLocation, final String unitID, final int playerID,
		final FogOfWarMemory trueMap, final MomSessionDescription sd, final ServerDatabaseEx db)
		throws RecordNotFoundException
	{
		log.entering (UnitServerUtilsImpl.class.getName (), "findNearestLocationWhereUnitCanBeAdded", new String []
			{desiredLocation.toString (), unitID, new Integer (playerID).toString ()});

		// Create test unit
		final AvailableUnit testUnit = new AvailableUnit ();
		testUnit.setUnitID (unitID);
		testUnit.setOwningPlayerID (playerID);
		getUnitUtils ().initializeUnitSkills (testUnit, 0, true, db);

		final List<String> testUnitSkillList = new ArrayList<String> ();
		for (final UnitHasSkill testUnitSkill : testUnit.getUnitHasSkill ())
			testUnitSkillList.add (testUnitSkill.getUnitSkillID ());

		// First try the centre
		OverlandMapCoordinatesEx addLocation = null;
		UnitAddBumpTypeID bumpType = UnitAddBumpTypeID.NO_ROOM;

		if (canUnitBeAddedHere (desiredLocation, testUnit, testUnitSkillList, trueMap, sd.getUnitSetting (), db))
		{
			addLocation = desiredLocation;
			bumpType = UnitAddBumpTypeID.CITY;
		}
		else
		{
			int direction = 1;
			while ((addLocation == null) && (direction <= getCoordinateSystemUtils ().getMaxDirection (sd.getMapSize ().getCoordinateSystemType ())))
			{
				final OverlandMapCoordinatesEx adjacentLocation = new OverlandMapCoordinatesEx ();
				adjacentLocation.setX (desiredLocation.getX ());
				adjacentLocation.setY (desiredLocation.getY ());
				adjacentLocation.setZ (desiredLocation.getZ ());

				if (getCoordinateSystemUtils ().moveCoordinates (sd.getMapSize (), adjacentLocation, direction))
					if (canUnitBeAddedHere (adjacentLocation, testUnit, testUnitSkillList, trueMap, sd.getUnitSetting (), db))
					{
						addLocation = adjacentLocation;
						bumpType = UnitAddBumpTypeID.BUMPED;
					}

				direction++;
			}
		}

		final UnitAddLocation result = new UnitAddLocation (addLocation, bumpType);

		log.exiting (UnitServerUtilsImpl.class.getName (), "findNearestLocationWhereUnitCanBeAdded", result);
		return result;
	}

	/**
	 * @param units List of units to check through
	 * @param order Type of order to look for
	 * @return List of all units, regardless of which player they belong to, with the requested order
	 */
	@Override
	public final List<MemoryUnit> listUnitsWithSpecialOrder (final List<MemoryUnit> units, final UnitSpecialOrder order)
	{
		log.entering (UnitServerUtilsImpl.class.getName (), "listUnitsWithSpecialOrder", order);
		
		final List<MemoryUnit> matches = new ArrayList<MemoryUnit> ();
		for (final MemoryUnit thisUnit : units)
			if ((thisUnit.getStatus () == UnitStatusID.ALIVE) && (thisUnit.getSpecialOrder () == order))
				matches.add (thisUnit);
		
		log.exiting (UnitServerUtilsImpl.class.getName (), "listUnitsWithSpecialOrder", matches.size ());
		return matches;
	}
	
	/**
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final UnitUtils utils)
	{
		unitUtils = utils;
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
	 * @return Server-only unit calculations
	 */
	public final MomServerUnitCalculations getServerUnitCalculations ()
	{
		return serverUnitCalculations;
	}

	/**
	 * @param calc Server-only unit calculations
	 */
	public final void setServerUnitCalculations (final MomServerUnitCalculations calc)
	{
		serverUnitCalculations = calc;
	}

	/**
	 * @return Random number generator
	 */
	public final RandomUtils getRandomUtils ()
	{
		return randomUtils;
	}

	/**
	 * @param utils Random number generator
	 */
	public final void setRandomUtils (final RandomUtils utils)
	{
		randomUtils = utils;
	}

	/**
	 * @return Coordinate system utils
	 */
	public final CoordinateSystemUtils getCoordinateSystemUtils ()
	{
		return coordinateSystemUtils;
	}

	/**
	 * @param utils Coordinate system utils
	 */
	public final void setCoordinateSystemUtils (final CoordinateSystemUtils utils)
	{
		coordinateSystemUtils = utils;
	}
}
