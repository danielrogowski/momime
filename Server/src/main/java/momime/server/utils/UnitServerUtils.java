package momime.server.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import com.ndg.map.CoordinateSystemUtils;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.v0_9_4.UnitSettingData;
import momime.common.database.v0_9_4.UnitHasSkill;
import momime.common.messages.CoordinatesUtils;
import momime.common.messages.UnitUtils;
import momime.common.messages.v0_9_4.AvailableUnit;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MemoryGridCell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.common.messages.v0_9_4.UnitAddBumpTypeID;
import momime.common.messages.v0_9_4.UnitSpecialOrder;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.server.calculations.MomServerUnitCalculations;
import momime.server.database.ServerDatabaseLookup;
import momime.server.database.v0_9_4.Unit;
import momime.server.database.v0_9_4.UnitSkill;
import momime.server.messages.ServerMemoryGridCellUtils;

/**
 * Server side only helper methods for dealing with units
 */
public final class UnitServerUtils
{
	/**
	 * Chooses a name for this hero (out of 5 possibilities) and rolls their random skills
	 * @param unit The hero to generate name and skills for
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws MomException If we find a hero who has no possible names defined, or who needs a random skill and we can't find a suitable one
	 * @throws RecordNotFoundException If we can't find the definition for the unit
	 */
	public static final void generateHeroNameAndRandomSkills (final MemoryUnit unit, final ServerDatabaseLookup db, final Logger debugLogger)
		throws MomException, RecordNotFoundException
	{
		debugLogger.entering (UnitServerUtils.class.getName (), "generateHeroNameAndRandomSkills", unit.getUnitID ());

		final Unit unitDefinition = db.findUnit (unit.getUnitID (), "generateHeroNameAndRandomSkills");

		// Pick a name at random
		if (unitDefinition.getHeroName ().size () == 0)
			throw new MomException ("No hero names found for unit ID \"" + unit.getUnitID () + "\"");

		unit.setHeroNameID (unitDefinition.getHeroName ().get (RandomUtils.getGenerator ().nextInt (unitDefinition.getHeroName ().size ())).getHeroNameID ());

		// Any random skills to add?
		if ((unitDefinition.getHeroRandomPickCount () != null) && (unitDefinition.getHeroRandomPickCount () > 0))
		{
			debugLogger.finest ("Hero " + unit.getUnitID () + "' belonging to player ID " + unit.getOwningPlayerID () + " skills before rolling extras: " + UnitUtils.describeBasicSkillValuesInDebugString (unit));

			// Run once for each random skill we get
			for (int pickNo = 0; pickNo < unitDefinition.getHeroRandomPickCount (); pickNo++)
			{
				// Get a list of all valid choices
				final List<String> skillChoices = new ArrayList<String> ();
				for (final UnitSkill thisSkill : db.getUnitSkills ())
				{
					// We can spot hero skills since they'll have at least one of these values filled in
					if (((thisSkill.getHeroSkillTypeID () != null) && (!thisSkill.getHeroSkillTypeID ().equals (""))) ||
						((thisSkill.isOnlyIfHaveAlready () != null) && (thisSkill.isOnlyIfHaveAlready ())) ||
						((thisSkill.getMaxOccurrences () != null) && (thisSkill.getMaxOccurrences () > 0)))
					{
						// Its a hero skill - do we have it already?
						final int currentSkillValue = UnitUtils.getBasicSkillValue (unit.getUnitHasSkill (), thisSkill.getUnitSkillID ());

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

				final String skillID = skillChoices.get (RandomUtils.getGenerator ().nextInt (skillChoices.size ()));
				final int currentSkillValue = UnitUtils.getBasicSkillValue (unit.getUnitHasSkill (), skillID);
				if (currentSkillValue >= 0)
					UnitUtils.setBasicSkillValue (unit, skillID, currentSkillValue + 1, debugLogger);
				else
				{
					final UnitHasSkill newSkill = new UnitHasSkill ();
					newSkill.setUnitSkillID (skillID);
					newSkill.setUnitSkillValue (1);
					unit.getUnitHasSkill ().add (newSkill);
				}

				debugLogger.finest ("Hero " + unit.getUnitID () + "' belonging to player ID " + unit.getOwningPlayerID () + " skills after rolling extras: " + UnitUtils.describeBasicSkillValuesInDebugString (unit));
			}
		}

		// Update status
		unit.setStatus (UnitStatusID.GENERATED);

		debugLogger.exiting (UnitServerUtils.class.getName (), "generateHeroNameAndRandomSkills");
	}

	/**
	 * @param order Special order that a unit has
	 * @return True if this special order results in the unit dying/being removed from play
	 */
	public static final boolean doesUnitSpecialOrderResultInDeath (final UnitSpecialOrder order)
	{
		return (order == UnitSpecialOrder.BUILD_CITY) || (order == UnitSpecialOrder.MELD_WITH_NODE) || (order == UnitSpecialOrder.DISMISS);
	}

	/**
	 * @param units List of units to search through
	 * @param playerID Player to search for
	 * @param unitID Unit ID to search for
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Unit with requested ID belonging to the requested player, or null if not found
	 */
	public final static MemoryUnit findUnitWithPlayerAndID (final List<MemoryUnit> units, final int playerID, final String unitID, final Logger debugLogger)
	{
		debugLogger.entering (UnitUtils.class.getName (), "findUnitWithPlayerAndID", new String [] {new Integer (playerID).toString (), unitID});

		MemoryUnit result = null;
		final Iterator<MemoryUnit> iter = units.iterator ();
		while ((result == null) && (iter.hasNext ()))
		{
			final MemoryUnit thisUnit = iter.next ();
			if ((thisUnit.getOwningPlayerID () == playerID) && (thisUnit.getUnitID ().equals (unitID)))
				result = thisUnit;
		}

		debugLogger.exiting (UnitUtils.class.getName (), "findUnitWithPlayerAndID", result);
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
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Whether unit can be added here or not
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found
	 */
	final static boolean canUnitBeAddedHere (final OverlandMapCoordinates addLocation, final AvailableUnit testUnit, final List<String> testUnitSkills,
		final FogOfWarMemory trueMap, final UnitSettingData settings, final ServerDatabaseLookup db, final Logger debugLogger)
		throws RecordNotFoundException
	{
		debugLogger.entering (UnitUtils.class.getName (), "canUnitBeAddedHere", CoordinatesUtils.overlandMapCoordinatesToString (addLocation));

		// Any other units here?
		final boolean unitCheckOk;
		final MemoryUnit unitHere = UnitUtils.findFirstAliveEnemyAtLocation (trueMap.getUnit (), addLocation.getX (), addLocation.getY (), addLocation.getPlane (), 0, debugLogger);
		if (unitHere != null)
		{
			// Do we own it?
			if (unitHere.getOwningPlayerID () == testUnit.getOwningPlayerID ())
				// Maximum number of units already in the cell?
				unitCheckOk = (UnitUtils.countAliveEnemiesAtLocation (trueMap.getUnit (), addLocation.getX (), addLocation.getY (), addLocation.getPlane (), 0, debugLogger) < settings.getUnitsPerMapCell ());
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
			final MemoryGridCell tc = trueMap.getMap ().getPlane ().get (addLocation.getPlane ()).getRow ().get (addLocation.getY ()).getCell ().get (addLocation.getX ());
			if ((ServerMemoryGridCellUtils.isNodeLairTower (tc.getTerrainData (), db)) ||
				(MomServerUnitCalculations.calculateDoubleMovementToEnterTileType (testUnit, testUnitSkills, tc.getTerrainData ().getTileTypeID (), trueMap.getMaintainedSpell (), db, debugLogger) == null))

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

		debugLogger.exiting (UnitUtils.class.getName (), "canUnitBeAddedHere", okToAdd);
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
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Location + bump type; note class and bump type will always be filled in, but location may be null if the unit cannot fit anywhere
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found
	 */
	public final static UnitAddLocation findNearestLocationWhereUnitCanBeAdded (final OverlandMapCoordinates desiredLocation, final String unitID, final int playerID,
		final FogOfWarMemory trueMap, final MomSessionDescription sd, final ServerDatabaseLookup db, final Logger debugLogger)
		throws RecordNotFoundException
	{
		debugLogger.entering (UnitUtils.class.getName (), "findNearestLocationWhereUnitCanBeAdded", new String []
			{CoordinatesUtils.overlandMapCoordinatesToString (desiredLocation), unitID, new Integer (playerID).toString ()});

		// Create test unit
		final AvailableUnit testUnit = new AvailableUnit ();
		testUnit.setUnitID (unitID);
		testUnit.setOwningPlayerID (playerID);
		UnitUtils.initializeUnitSkills (testUnit, 0, true, db, debugLogger);

		final List<String> emptySkillList = new ArrayList<String> ();

		// First try the centre
		OverlandMapCoordinates addLocation = null;
		UnitAddBumpTypeID bumpType = UnitAddBumpTypeID.NO_ROOM;

		if (canUnitBeAddedHere (desiredLocation, testUnit, emptySkillList, trueMap, sd.getUnitSetting (), db, debugLogger))
		{
			addLocation = desiredLocation;
			bumpType = UnitAddBumpTypeID.CITY;
		}
		else
		{
			int direction = 1;
			while ((addLocation == null) && (direction <= CoordinateSystemUtils.getMaxDirection (sd.getMapSize ().getCoordinateSystemType ())))
			{
				final OverlandMapCoordinates adjacentLocation = new OverlandMapCoordinates ();
				adjacentLocation.setX (desiredLocation.getX ());
				adjacentLocation.setY (desiredLocation.getY ());
				adjacentLocation.setPlane (desiredLocation.getPlane ());

				if (CoordinateSystemUtils.moveCoordinates (sd.getMapSize (), adjacentLocation, direction))
					if (canUnitBeAddedHere (adjacentLocation, testUnit, emptySkillList, trueMap, sd.getUnitSetting (), db, debugLogger))
					{
						addLocation = adjacentLocation;
						bumpType = UnitAddBumpTypeID.BUMPED;
					}

				direction++;
			}
		}

		final UnitAddLocation result = new UnitAddLocation (addLocation, bumpType);

		debugLogger.exiting (UnitUtils.class.getName (), "findNearestLocationWhereUnitCanBeAdded", result);
		return result;
	}

	/**
	 * Prevent instantiation
	 */
	private UnitServerUtils ()
	{
	}
}
