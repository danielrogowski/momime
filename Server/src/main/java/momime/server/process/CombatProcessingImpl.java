package momime.server.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.calculations.CombatMoveType;
import momime.common.calculations.MomUnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.v0_9_5.FogOfWarSettingData;
import momime.common.messages.servertoclient.v0_9_5.KillUnitActionID;
import momime.common.messages.servertoclient.v0_9_5.KillUnitMessage;
import momime.common.messages.servertoclient.v0_9_5.KillUnitMessageData;
import momime.common.messages.servertoclient.v0_9_5.MoveUnitInCombatMessage;
import momime.common.messages.servertoclient.v0_9_5.SetCombatPlayerMessage;
import momime.common.messages.servertoclient.v0_9_5.SetUnitIntoOrTakeUnitOutOfCombatMessage;
import momime.common.messages.servertoclient.v0_9_5.StartCombatMessage;
import momime.common.messages.servertoclient.v0_9_5.StartCombatMessageUnit;
import momime.common.messages.v0_9_5.FogOfWarMemory;
import momime.common.messages.v0_9_5.MapAreaOfCombatTiles;
import momime.common.messages.v0_9_5.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_5.MemoryCombatAreaEffect;
import momime.common.messages.v0_9_5.MemoryGridCell;
import momime.common.messages.v0_9_5.MemoryMaintainedSpell;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.messages.v0_9_5.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_5.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_5.TurnSystem;
import momime.common.messages.v0_9_5.UnitCombatSideID;
import momime.common.messages.v0_9_5.UnitStatusID;
import momime.common.utils.CombatMapUtils;
import momime.common.utils.CombatPlayers;
import momime.common.utils.MomUnitAttributeComponent;
import momime.common.utils.MomUnitAttributePositiveNegative;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.ai.CombatAI;
import momime.server.database.ServerDatabaseEx;
import momime.server.fogofwar.FogOfWarDuplication;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.messages.ServerMemoryGridCellUtils;
import momime.server.messages.v0_9_5.ServerGridCell;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Routines dealing with initiating and progressing combats, as well as moving and attacking
 */
public final class CombatProcessingImpl implements CombatProcessing
{
	/** Max number of units to fill each row during combat set up */ 
	private static final int COMBAT_SETUP_UNITS_PER_ROW = 5;
	
	/** Class logger */
	private final Log log = LogFactory.getLog (CombatProcessingImpl.class);

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Unit calculations */
	private MomUnitCalculations unitCalculations;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** FOW duplication utils */
	private FogOfWarDuplication fogOfWarDuplication;
	
	/** Simultaneous turns combat scheduler */
	private CombatScheduler combatScheduler;
	
	/** Combat map utils */
	private CombatMapUtils combatMapUtils;
	
	/** Combat AI */
	private CombatAI combatAI;
	
	/** Damage processor */
	private DamageProcessor damageProcessor;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** Starting and ending combats */
	private CombatStartAndEnd combatStartAndEnd;
	
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;
	
	/**
	 * Sets up a potential combat on the server.  If we're attacking an enemy unit stack or city, then all this does is calls StartCombat.
	 * If we're attacking a node/lair/tower, then this handles scouting the node/lair/tower, sending to the client the details
	 * of what monster we scouted, and StartCombat is only called when/if they click "Yes" they want to attack.
	 * This is declared separately so it can be used immediately from MoveUnitStack in one-at-a-time games, or from requesting scheduled combats in Simultaneous turns games.
	 * 
	 * This could probably be removed now, and have everything directly call startCombat
	 *
	 * @param defendingLocation Location where defending units are standing
	 * @param attackingFrom Location where attacking units are standing (which will be a map tile adjacent to defendingLocation)
	 * @param scheduledCombatURN Scheduled combat URN, if simultaneous turns game; null for one-at-a-time games
	 * @param attackingPlayer Player who is attacking
	 * @param attackingUnitURNs Which of the attacker's unit stack are attacking - they might be leaving some behind
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void initiateCombat (final MapCoordinates3DEx defendingLocation, final MapCoordinates3DEx attackingFrom,
		final Integer scheduledCombatURN, final PlayerServerDetails attackingPlayer, final List<Integer> attackingUnitURNs, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.trace ("Entering initiateCombat: " + defendingLocation + ", " + attackingFrom + ", " + scheduledCombatURN +
			", Player ID " + attackingPlayer.getPlayerDescription ().getPlayerID ());
		
		// We need to inform all players that the attacker is involved in a combat.
		// We deal with the defender (if its a real human/human combat) within StartCombat
		if ((mom.getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS) && (attackingPlayer.getPlayerDescription ().isHuman ()))
			getCombatScheduler ().informClientsOfPlayerBusyInCombat (attackingPlayer, mom.getPlayers (), true);
		
		// Start right away
		getCombatStartAndEnd ().startCombat (defendingLocation, attackingFrom, scheduledCombatURN, attackingPlayer, attackingUnitURNs, mom);

		log.trace ("Exiting initiateCombat");
	}
		
	/**
	 * Purpose of this is to check for impassable terrain obstructions.  All the rocks, housing, ridges and so on are still passable, the only impassable things are
	 * city wall corners and the main feature (node, temple, tower of wizardry, etc. on the defender side).
	 * 
	 * So for attackers we always end up with 4 full rows like
	 * XXXXX
	 * XXXXX
	 * XXXXX
	 * XXXXX
	 * 
	 * For defenders in a city with walls we end up with 5 rows patterened like so (or there may be more spaces if no city walls) 
	 *   XXX
	 * XXXXX
	 * XX  XX
	 * XXXXX
	 *   XXX
	 *   
	 * so either way we should always have 20 spaces
	 *
	 * @param startX X coordinate within the combat map to centre the units around
	 * @param startY Y coordinate within the combat map to centre the units around
	 * @param unitHeading The direction the placed units will be facing
	 * @param maxRows Number of rows the units need to be arranged into in order to fit
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @param combatMap Combat scenery we are placing the units onto (important because some tiles will be impassable to some types of unit)
	 * @param db Lookup lists built over the XML database
	 * @return List of how many units we can fit into each row
	 * @throws RecordNotFoundException If we counter a combatTileBorderID or combatTileTypeID that can't be found in the db
	 */
	final List<Integer> determineMaxUnitsInRow (final int startX, final int startY, final int unitHeading, final int maxRows,
		final CoordinateSystem combatMapCoordinateSystem, final MapAreaOfCombatTiles combatMap, final ServerDatabaseEx db) throws RecordNotFoundException
	{
		final List<Integer> maxUnitsInRow = new ArrayList<Integer> ();
		final MapCoordinates2DEx centre = new MapCoordinates2DEx (startX, startY);
	
		for (int rowNo = 0; rowNo < maxRows; rowNo++)
		{
			int maxUnitsInThisRow = 0;
			
			// Move down-left to start of row...
			final MapCoordinates2DEx coords = new MapCoordinates2DEx (centre);
			for (int n = 0; n < COMBAT_SETUP_UNITS_PER_ROW/2; n++)
				getCoordinateSystemUtils ().move2DCoordinates (combatMapCoordinateSystem, coords, 6);
			
			// ..then position units in an up-right fashion to fill the row
			for (int n = 0; n < COMBAT_SETUP_UNITS_PER_ROW; n++)
			{
				if (getUnitCalculations ().calculateDoubleMovementToEnterCombatTile (combatMap.getRow ().get (coords.getY ()).getCell ().get (coords.getX ()), db) >= 0)
					maxUnitsInThisRow++;
				
				getCoordinateSystemUtils ().move2DCoordinates (combatMapCoordinateSystem, coords, 2);
			}
			
			maxUnitsInRow.add (maxUnitsInThisRow);
			getCoordinateSystemUtils ().move2DCoordinates (combatMapCoordinateSystem, centre,
				getCoordinateSystemUtils ().normalizeDirection (combatMapCoordinateSystem.getCoordinateSystemType (), unitHeading + 4));
		}
		
		return maxUnitsInRow;
	}
	
	/**
	 * 1 = Melee heroes
	 * 2 = Melee units
	 * 3 = Ranged heroes
	 * 4 = Ranged units
	 * 5 = Settlers
	 * 
	 * @param unit Unit to compare
	 * @param players Players list
	 * @param spells Known spells
	 * @param combatAreaEffects Known combat area effects
	 * @param db Lookup lists built over the XML database
	 * @return Combat class of the unit, as defined in the list above
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 */
	final int calculateUnitCombatClass (final MemoryUnit unit, final List<PlayerServerDetails> players, final List<MemoryMaintainedSpell> spells,
		final List<MemoryCombatAreaEffect> combatAreaEffects, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.trace ("Entering calculateUnitCombatClass: Unit URN " + unit.getUnitURN ());
		final int result;
		
		// Does this unit have a ranged attack?
		if (getUnitUtils ().getModifiedAttributeValue (unit, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_RANGED_ATTACK,
			MomUnitAttributeComponent.ALL, MomUnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db) > 0)
		{
			// Ranged hero or regular unit?
			if (db.findUnit (unit.getUnitID (), "calculateUnitCombatClass").getUnitMagicRealm ().equals (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
				result = 3;
			else
				result = 4;
		}
		
		// Does this unit have a melee attack?
		else if (getUnitUtils ().getModifiedAttributeValue (unit, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_MELEE_ATTACK,
			MomUnitAttributeComponent.ALL, MomUnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db) > 0)
		{
			// Melee hero or regular unit?
			if (db.findUnit (unit.getUnitID (), "calculateUnitCombatClass").getUnitMagicRealm ().equals (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
				result = 1;
			else
				result = 2;
		}
		
		else
			// No attacks at all (settlers)
			result = 5;
		
		log.trace ("Exiting calculateUnitCombatClass = " + result);
		return result;
	}
	
	/**
	 * @param units List of units being positioned in combat; must already have been sorted into combat class order
	 * @return List of how many of each combat class there are
	 */
	final List<Integer> listNumberOfEachCombatClass (final List<MemoryUnitAndCombatClass> units)
	{
		log.trace ("Entering listNumberOfEachCombatClass: " + units.size ());
				
		final List<Integer> counts = new ArrayList<Integer> ();
		
		int currentCombatClass = 0;
		int currentCombatClassCount = 0;
		for (final MemoryUnitAndCombatClass unit : units)
		{
			if (unit.getCombatClass () == currentCombatClass)
				currentCombatClassCount++;
			else
			{
				// Previous entry to add to list?
				if (currentCombatClassCount > 0)
					counts.add (currentCombatClassCount);
				
				currentCombatClass = unit.getCombatClass ();
				currentCombatClassCount = 1;
			}
		}

		// Final entry to add to list?
		if (currentCombatClassCount > 0)
			counts.add (currentCombatClassCount);
		
		log.trace ("Exiting listNumberOfEachCombatClass = " + counts.size ());
		return counts;
	}
	
	/**
	 * Compacts up the smallest values in the list if we've got too many rows, so if the
	 * e.g. above are the attackers, they only have 4 rows So want to go from 4, 2, 2, 6, 3 to 4, 4, 6, 3
	 * 
	 * @param unitsInRow The number of units to arrange into each row of combat setup
	 * @param maxRows Number of rows the units need to be arranged into in order to fit
	 */
	final void mergeRowsIfTooMany (final List<Integer> unitsInRow, final int maxRows)
	{
		log.trace ("Entering mergeRowsIfTooMany: " + unitsInRow.size () + ", " + maxRows);
		
		while (unitsInRow.size () > maxRows)
		{
			// Find the lowest adjacent two values - start by assuming its the first two
			int bestRowNo = 0;
			int bestCombatClass = unitsInRow.get (0) + unitsInRow.get (1);
			
			for (int rowNo = 1; rowNo < unitsInRow.size () - 1; rowNo++)
			{
				final int thisCombatClass = unitsInRow.get (rowNo) + unitsInRow.get (rowNo+1);
				if (thisCombatClass < bestCombatClass)
				{
					bestCombatClass = thisCombatClass;
					bestRowNo = rowNo;
				}
			}
			
			// Compact out the entry
			unitsInRow.set (bestRowNo, unitsInRow.get (bestRowNo) + unitsInRow.get (bestRowNo+1));
			unitsInRow.remove (bestRowNo+1);
		}
		
		log.trace ("Exiting mergeRowsIfTooMany = " + unitsInRow.size ());
	}
	
	/**
	 * Move units backwards if there's too many units in any row
	 * So e.g. want to go from 4, 4, 6, 3 to 4, 4, 5, 4
	 * 
	 * @param unitsInRow The number of units to arrange into each row of combat setup
	 * @param maxUnitsInRow Maximum number of units we can fit on each row
	 */
	final void moveUnitsInOverfullRowsBackwards (final List<Integer> unitsInRow, final List<Integer> maxUnitsInRow)
	{
		log.trace ("Entering moveUnitsInOverfullRowsBackwards: " + unitsInRow.size () + ", " + maxUnitsInRow.size ());
		
		int overflow = 0;
		for (int rowNo = 0; rowNo < unitsInRow.size (); rowNo++)
		{
			// Any spare space in this row?
			if ((overflow > 0) && (unitsInRow.get (rowNo) < maxUnitsInRow.get (rowNo)))
			{
				final int unitsToBump = Math.min (overflow, maxUnitsInRow.get (rowNo) - unitsInRow.get (rowNo));
				overflow = overflow - unitsToBump;
				unitsInRow.set (rowNo, unitsInRow.get (rowNo) + unitsToBump);
			}
			
			// Any that don't fit in this row?
			if (unitsInRow.get (rowNo) > maxUnitsInRow.get (rowNo))
			{
				overflow = overflow + (unitsInRow.get (rowNo) - maxUnitsInRow.get (rowNo));
				unitsInRow.set (rowNo, maxUnitsInRow.get (rowNo));
			}
		}
		
		// We can have overflow from the end, so if we do and we've got spare rows, then fill them
		while ((overflow > 0) && (unitsInRow.size () < maxUnitsInRow.size ()))
		{
			final int rowNo = unitsInRow.size ();
			final int unitsInThisRow = Math.min (overflow, maxUnitsInRow.get (rowNo));
			unitsInRow.add (unitsInThisRow);
			overflow = overflow - unitsInThisRow;
		}
		
		// If we've *still* got an overflow then just shove the units in the back row even though they don't fit (they'll get fixed up in moveUnitsInOverfullRowsForwards)
		if (overflow > 0)
		{
			final int rowNo = unitsInRow.size () - 1;
			unitsInRow.set (rowNo, unitsInRow.get (rowNo) + overflow);
		}
		
		log.trace ("Exiting moveUnitsInOverfullRowsBackwards = " + unitsInRow.size ());
	}

	/**
	 * Move units forwards if there's too many units in any row
	 * So e.g. want to go from 4, 4, 6, 3 to 4, 5, 5, 3
	 * 
	 * @param unitsInRow The number of units to arrange into each row of combat setup
	 * @param maxUnitsInRow Maximum number of units we can fit on each row
	 * @throws MomException If there's not enough space to fit all the units
	 */
	final void moveUnitsInOverfullRowsForwards (final List<Integer> unitsInRow, final List<Integer> maxUnitsInRow) throws MomException
	{
		log.trace ("Entering moveUnitsInOverfullRowsForwards: " + unitsInRow + ", " + maxUnitsInRow.size ());
		
		int overflow = 0;
		for (int rowNo = unitsInRow.size () - 1; rowNo >= 0; rowNo--)
		{
			// Any spare space in this row?
			if ((overflow > 0) && (unitsInRow.get (rowNo) < maxUnitsInRow.get (rowNo)))
			{
				final int unitsToBump = Math.min (overflow, maxUnitsInRow.get (rowNo) - unitsInRow.get (rowNo));
				overflow = overflow - unitsToBump;
				unitsInRow.set (rowNo, unitsInRow.get (rowNo) + unitsToBump);
			}
			
			// Any that don't fit in this row?
			if (unitsInRow.get (rowNo) > maxUnitsInRow.get (rowNo))
			{
				overflow = overflow + (unitsInRow.get (rowNo) - maxUnitsInRow.get (rowNo));
				unitsInRow.set (rowNo, maxUnitsInRow.get (rowNo));
			}
		}
		
		// If we've *still* got an overflow then we've got a problem because there's literally nowhere left to fit the units
		if (overflow > 0)
			throw new MomException ("moveUnitsInOverfullRowsForwards: Not enough room to position all units in combat - " + overflow);
		
		log.trace ("Exiting moveUnitsInOverfullRowsForwards: " + unitsInRow.size ());
	}
	
	/**
	 * Final piece of positionCombatUnits - after all the positioning logic is complete, actually places the units into combat
	 *  
	 * @param combatLocation The location the combat is taking place at (may not necessarily be the location of the defending units, see where this is set in startCombat)
	 * @param startX X coordinate within the combat map to centre the units around
	 * @param startY Y coordinate within the combat map to centre the units around
	 * @param unitHeading Heading the units should be set to face
	 * @param combatSide Side the units should be set to be on
	 * @param unitsToPosition Ordered list of the units being placed into combat
	 * @param unitsInRow Number of units to arrange into each row
	 * @param startCombatMessage Message being built up ready to send to human participants; if combat is between two AI players can pass this in as null
	 * @param attackingPlayer Player who is attacking
	 * @param defendingPlayer Player who is defending - may be null if taking an empty lair, or a "walk in without a fight" in simultaneous turns games
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @param combatMap Combat scenery we are placing the units onto (important because some tiles will be impassable to some types of unit)
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 */
	final void placeCombatUnits (final MapCoordinates3DEx combatLocation, final int startX, final int startY, final int unitHeading, final UnitCombatSideID combatSide,
		final List<MemoryUnitAndCombatClass> unitsToPosition, final List<Integer> unitsInRow, final StartCombatMessage startCombatMessage,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final CoordinateSystem combatMapCoordinateSystem, final MapAreaOfCombatTiles combatMap, final ServerDatabaseEx db)
		throws RecordNotFoundException
	{
		log.trace ("Entering placeCombatUnits: " + combatLocation + ", (" + startX + ", " + startY + "), " + unitHeading + ", " + combatSide);
		
		int unitNo = 0;		// Index into unit list of unit being placed
		final MapCoordinates2DEx centre = new MapCoordinates2DEx (startX, startY);
		
		for (final Integer unitsOnThisRow : unitsInRow)
		{
			final MapCoordinates2DEx coords = new MapCoordinates2DEx (centre);
				
			// Move down-left to start of row...
			for (int n = 0; n < unitsOnThisRow/2; n++)
				getCoordinateSystemUtils ().move2DCoordinates (combatMapCoordinateSystem, coords, 6);
				
			// ..then position units in an up-right fashion to fill the row
			for (int n = 0; n < unitsOnThisRow; n++)
			{
				// Make sure the cell is passable
				while (getUnitCalculations ().calculateDoubleMovementToEnterCombatTile (combatMap.getRow ().get (coords.getY ()).getCell ().get (coords.getX ()), db) < 0)
					getCoordinateSystemUtils ().move2DCoordinates (combatMapCoordinateSystem, coords, 2);
				
				// Place unit
				final MemoryUnit trueUnit = unitsToPosition.get (unitNo).getUnit ();
				
				// Update true unit on server
				trueUnit.setCombatLocation (combatLocation);
				trueUnit.setCombatPosition (new MapCoordinates2DEx (coords));
				trueUnit.setCombatHeading (unitHeading);
				trueUnit.setCombatSide (combatSide);
				
				// Attacking player's memory on server
				final MomPersistentPlayerPrivateKnowledge atkPriv = (MomPersistentPlayerPrivateKnowledge) attackingPlayer.getPersistentPlayerPrivateKnowledge ();
				final MemoryUnit atkUnit = getUnitUtils ().findUnitURN (trueUnit.getUnitURN (), atkPriv.getFogOfWarMemory ().getUnit (), "placeCombatUnits-A");
				atkUnit.setCombatLocation (combatLocation);
				atkUnit.setCombatPosition (new MapCoordinates2DEx (coords));
				atkUnit.setCombatHeading (unitHeading);
				atkUnit.setCombatSide (combatSide);
				
				// Defending player may be nil if we're attacking an empty lair
				if (defendingPlayer != null)
				{
					// Update player memory on server
					final MomPersistentPlayerPrivateKnowledge defPriv = (MomPersistentPlayerPrivateKnowledge) defendingPlayer.getPersistentPlayerPrivateKnowledge ();
					final MemoryUnit defUnit = getUnitUtils ().findUnitURN (trueUnit.getUnitURN (), defPriv.getFogOfWarMemory ().getUnit (), "placeCombatUnits-D");
					defUnit.setCombatLocation (combatLocation);
					defUnit.setCombatPosition (new MapCoordinates2DEx (coords));
					defUnit.setCombatHeading (unitHeading);
					defUnit.setCombatSide (combatSide);
				}
				
				// Send unit positioning to clients
				if (startCombatMessage != null)
				{
					final StartCombatMessageUnit unitPlacement = new StartCombatMessageUnit ();
					unitPlacement.setUnitURN (trueUnit.getUnitURN ());
					unitPlacement.setCombatPosition (new MapCoordinates2DEx (coords));
					unitPlacement.setCombatHeading (unitHeading);
					unitPlacement.setCombatSide (combatSide);
				
					startCombatMessage.getUnitPlacement ().add (unitPlacement);
				}
				
				// Move up-right to next position
				unitNo++;
				getCoordinateSystemUtils ().move2DCoordinates (combatMapCoordinateSystem, coords, 2);
			}
				
			getCoordinateSystemUtils ().move2DCoordinates (combatMapCoordinateSystem, centre,
				getCoordinateSystemUtils ().normalizeDirection (combatMapCoordinateSystem.getCoordinateSystemType (), unitHeading + 4));
		}
			
		log.trace ("Exiting placeCombatUnits");
	}

	/**
	 * Sets units into combat (e.g. sets their combatLocation to put them into combat, and sets their position, heading and side within that combat)
	 * and adds the info to the StartCombatMessage to inform the the two clients involved to do the same.
	 * One call does units on one side (attacking or defending) of the combat.
	 * 
	 * @param combatLocation The location the combat is taking place at (may not necessarily be the location of the defending units, see where this is set in startCombat)
	 * @param startCombatMessage Message being built up ready to send to human participants; if combat is between two AI players can pass this in as null
	 * @param attackingPlayer Player who is attacking
	 * @param defendingPlayer Player who is defending - may be null if taking an empty lair, or a "walk in without a fight" in simultaneous turns games
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @param currentLocation The overland map location where the units being put into combat are standing - so attackers are 1 tile away from the actual combatLocation
	 * @param startX X coordinate within the combat map to centre the units around
	 * @param startY Y coordinate within the combat map to centre the units around
	 * @param maxRows Number of rows the units need to be arranged into in order to fit
	 * @param unitHeading Heading the units should be set to face
	 * @param combatSide Side the units should be set to be on
	 * @param onlyUnitURNs If null, all units at currentLocation will be put into combat; if non-null, only the listed Unit URNs will be put into combat
	 * 	This is because the attacker may elect to not attack with every single unit in their stack
	 * @param combatMap Combat scenery we are placing the units onto (important because some tiles will be impassable to some types of unit)
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws MomException If there is a logic failure, e.g. not enough space to fit all the units
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws PlayerNotFoundException If we can't find the player who owns one of the units
	 */
	@Override
	public final void positionCombatUnits (final MapCoordinates3DEx combatLocation, final StartCombatMessage startCombatMessage,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final CoordinateSystem combatMapCoordinateSystem,
		final MapCoordinates3DEx currentLocation, final int startX, final int startY, final int maxRows, final int unitHeading,
		final UnitCombatSideID combatSide, final List<Integer> onlyUnitURNs, final MapAreaOfCombatTiles combatMap, final MomSessionVariables mom)
		throws MomException, RecordNotFoundException, PlayerNotFoundException
	{
		log.trace ("Entering positionCombatUnits: " + currentLocation + ", (" +
			startX + ", " + startY + "), " + maxRows + ", " + unitHeading + ", " + combatSide + ", " + ((onlyUnitURNs == null) ? "All" : new Integer (onlyUnitURNs.size ()).toString ()));
		
		// First check for obstructions, to work out the maximum number of units we can fit on each row
		final List<Integer> maxUnitsInRow = determineMaxUnitsInRow (startX, startY, unitHeading, maxRows,
			combatMapCoordinateSystem, combatMap, mom.getServerDB ());
		
		// Make a list of all the units we need to position - attackers may not have selected entire stack to attack with
		final List<MemoryUnitAndCombatClass> unitsToPosition = new ArrayList<MemoryUnitAndCombatClass> ();
		for (final MemoryUnit tu : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
			if ((currentLocation.equals (tu.getUnitLocation ())) && (tu.getStatus () == UnitStatusID.ALIVE) &&
				((onlyUnitURNs == null) || (onlyUnitURNs.contains (tu.getUnitURN ()))))
			{
				// Give unit full ammo and mana
				// Have to do this now, since sorting the units relies on knowing what ranged attacks they have
				getUnitCalculations ().giveUnitFullRangedAmmoAndMana (tu, mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
					mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), mom.getServerDB ());
				
				// Add it to the list
				unitsToPosition.add (new MemoryUnitAndCombatClass (tu, calculateUnitCombatClass (tu, mom.getPlayers (),
					mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), mom.getServerDB ())));
			}
		
		// Sort the units by their "combat class"; this sorts in the order: Melee heroes, melee units, ranged heroes, ranged units, settlers
		Collections.sort (unitsToPosition);
		
		// Create a list of how many of each combat class there is, e.g. may have 4 MH, 2 MU, 2 RH, 6 RU, 3 ST = 17 units
		// This is our ideal row placement
		final List<Integer> unitsInRow = listNumberOfEachCombatClass (unitsToPosition);
		
		// Shuffle rows around to ensure we end up meeting maxRows and maxUnitsInRow limitations
		mergeRowsIfTooMany (unitsInRow, maxRows);
		moveUnitsInOverfullRowsBackwards (unitsInRow, maxUnitsInRow);
		moveUnitsInOverfullRowsForwards (unitsInRow, maxUnitsInRow);
		
		// Now can actually place the units
		placeCombatUnits (combatLocation, startX, startY, unitHeading, combatSide, unitsToPosition, unitsInRow, startCombatMessage,
			attackingPlayer, defendingPlayer, combatMapCoordinateSystem, combatMap, mom.getServerDB ());

		log.trace ("Exiting positionCombatUnits");
	}
	
	/**
	 * Progresses the combat happening at the specified location.
	 * Will cycle through playing out AI players' combat turns until either the combat ends or it is a human players turn, at which
	 * point it will send a message to them to tell them to take their turn and then exit.
	 * 
	 * @param combatLocation Where the combat is taking place
	 * @param initialFirstTurn True if this is the initial call from startCombat; false if it is being called by a human playing ending their combat turn or turning auto on
	 * @param initialAutoControlHumanPlayer True if it is being called by a human player turning auto on
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void progressCombat (final MapCoordinates3DEx combatLocation, final boolean initialFirstTurn,
		final boolean initialAutoControlHumanPlayer, final MomSessionVariables mom)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering progressCombat: " + combatLocation + ", " + initialFirstTurn + ", " + initialAutoControlHumanPlayer);

		final ServerGridCell tc = (ServerGridCell) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(combatLocation.getZ ()).getRow ().get (combatLocation.getY ()).getCell ().get (combatLocation.getX ());

		// These get modified by the loop
		boolean firstTurn = initialFirstTurn;
		boolean autoControlHumanPlayer = initialAutoControlHumanPlayer;
		
		// We cannot safely determine the players involved until we've proved there are actually some units on each side.
		// Keep going until one side is wiped out or a human player needs to take their turn.
		boolean aiPlayerTurn = true;
		CombatPlayers combatPlayers = getCombatMapUtils ().determinePlayersInCombatFromLocation
			(combatLocation, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), mom.getPlayers ());
		while ((aiPlayerTurn) && (combatPlayers.bothFound ()))
		{
			// Who should take their turn next?
			// If human player hits Auto, then we want to play their turn for them through their AI, without switching players
			if (!autoControlHumanPlayer)
			{
				// Defender always goes first
				if (firstTurn)
				{
					firstTurn = false;
					tc.setCombatCurrentPlayer (combatPlayers.getDefendingPlayer ().getPlayerDescription ().getPlayerID ());
					log.debug ("First turn - Defender");
				}
				
				// Otherwise compare against the player from the last turn
				else if (combatPlayers.getDefendingPlayer ().getPlayerDescription ().getPlayerID ().equals (tc.getCombatCurrentPlayer ()))
				{
					tc.setCombatCurrentPlayer (combatPlayers.getAttackingPlayer ().getPlayerDescription ().getPlayerID ());
					log.debug ("Attacker's turn");
				}
				else
				{
					tc.setCombatCurrentPlayer (combatPlayers.getDefendingPlayer ().getPlayerDescription ().getPlayerID ());
					log.debug ("Defender's turn");
				}
				
				// Tell all human players involved in the combat who the new player is
				final SetCombatPlayerMessage msg = new SetCombatPlayerMessage ();
				msg.setCombatLocation (combatLocation);
				msg.setPlayerID (tc.getCombatCurrentPlayer ());
				
				if (combatPlayers.getDefendingPlayer ().getPlayerDescription ().isHuman ())
					((PlayerServerDetails) combatPlayers.getDefendingPlayer ()).getConnection ().sendMessageToClient (msg);

				if (combatPlayers.getAttackingPlayer ().getPlayerDescription ().isHuman ())
					((PlayerServerDetails) combatPlayers.getAttackingPlayer ()).getConnection ().sendMessageToClient (msg);
				
				// Give this player all their movement for this turn
				getUnitUtils ().resetUnitCombatMovement (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), tc.getCombatCurrentPlayer (), combatLocation, mom.getServerDB ());
				
				// Allow the player to cast a spell this turn
				tc.setSpellCastThisCombatTurn (null);
			}
			
			// AI or human player?
			if (tc.getCombatCurrentPlayer () != null)
			{
				final PlayerServerDetails combatCurrentPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), tc.getCombatCurrentPlayer (), "progressCombat");
				if ((combatCurrentPlayer.getPlayerDescription ().isHuman ()) && (!autoControlHumanPlayer))
				{
					// Human players' turn.
					// Nothing to do here - we already notified them to take their turn so the loop & this method will just
					// exit and combat will proceed when we receive messages from the player.
					aiPlayerTurn = false;
				}
				else
				{
					// Take AI players' turn
					getCombatAI ().aiCombatTurn (combatLocation, combatCurrentPlayer, mom);
					aiPlayerTurn = true;
					autoControlHumanPlayer = false;
				}
			}
			
			// Was either side wiped out yet?
			combatPlayers = getCombatMapUtils ().determinePlayersInCombatFromLocation
				(combatLocation, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), mom.getPlayers ());
		}
		
		log.trace ("Exiting progressCombat");
	}
	
	/**
	 * Regular units who die in combat are only set to status=DEAD - they are not actually freed immediately in case someone wants to cast Animate Dead on them.
	 * After a combat ends, this routine properly frees them both on the server and all clients.
	 * 
	 * Heroes who die in combat are set to musDead on the server and the client who owns them - they're freed immediately on the other clients.
	 * This means we don't have to touch dead heroes here, just leave them as they are.
	 * 
	 * It also removes combat summons, e.g. Phantom Warriors, even if they are not dead.
	 * 
	 * It also removes monsters left alive guarding nodes/lairs/towers from the client (leaving them on the server) - these only ever exist
	 * temporarily on the client who is attacking, and the "knows about unit" routine the FOW is based on always returns False for them,
	 * so we have handle them as a special case here.
	 * 
	 * @param combatLocation The location the combat is taking place at (may not necessarily be the location of the defending units, see where this is set in startCombat)
	 * @param attackingPlayer Player who is attacking
	 * @param defendingPlayer Player who is defending - may be null if taking an empty lair, or a "walk in without a fight" in simultaneous turns games
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param players List of players in the session
	 * @param fogOfWarSettings Fog of war settings from session description
	 * @param db Lookup lists built over the XML database
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void purgeDeadUnitsAndCombatSummonsFromCombat (final MapCoordinates3DEx combatLocation,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final FogOfWarMemory trueMap,
		final List<PlayerServerDetails> players, final FogOfWarSettingData fogOfWarSettings, final ServerDatabaseEx db)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException
	{
		log.trace ("Entering purgeDeadUnitsAndCombatSummonsFromCombat: " + combatLocation);
		
		final MemoryGridCell tc = trueMap.getMap ().getPlane ().get (combatLocation.getZ ()).getRow ().get (combatLocation.getY ()).getCell ().get (combatLocation.getX ());
		
		// Is this someone attacking a node/lair/tower?
		// If DefendingPlayer is nil (we wiped out the monsters), there'll be no monsters to remove, so in which case we don't care that we get this value wrong
		final MomPersistentPlayerPublicKnowledge defPub = (defendingPlayer == null) ? null : (MomPersistentPlayerPublicKnowledge) defendingPlayer.getPersistentPlayerPublicKnowledge ();
		final boolean attackingNodeLairTower = (defPub != null) && (CommonDatabaseConstants.WIZARD_ID_MONSTERS.equals (defPub.getWizardID ())) &&
			ServerMemoryGridCellUtils.isNodeLairTower (tc.getTerrainData (), db);
		
		// Then check all the units
		// Had better copy the units list since we'll be removing units from it as we go along
		int deadCount = 0;
		int summonedCount = 0;
		int monstersCount = 0;
		
		final List<MemoryUnit> copyOfTrueUnits = new ArrayList<MemoryUnit> ();
		copyOfTrueUnits.addAll (trueMap.getUnit ());
		for (final MemoryUnit trueUnit : copyOfTrueUnits)
			
			// Don't check combatPosition here - DEAD units should have no position
			if (combatLocation.equals (trueUnit.getCombatLocation ()))
			{
				// Permanently remove any dead regular units (which were kept around until now so they could be Animate Dead'ed)
				// Also remove any combat summons like Phantom Warriors
				final boolean manuallyTellAttackerClientToKill;
				final boolean manuallyTellDefenderClientToKill;
				if ((trueUnit.isWasSummonedInCombat ()) || ((trueUnit.getStatus () == UnitStatusID.DEAD) &&
					(!db.findUnit (trueUnit.getUnitID (), "purgeDeadUnitsAndCombatSummonsFromCombat").getUnitMagicRealm ().equals (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))))
				{
					if (trueUnit.getStatus () == UnitStatusID.DEAD)
						deadCount++;
					else
						summonedCount++;
					
					// The kill routine below will free off dead units on the server fine, but never instruct the client to do the same,
					// since the "knows about unit" routine always returns false for dead units
					
					// Players not involved in the combat would have freed the units straight away from the
					// ApplyDamage message, so this only affects the attacking & defending players
					
					// Heroes are fine - for the player who owns the hero, we leave them as dead so they can be later resurrected;
					// the enemy of the hero would have freed them from ApplyDamage
					manuallyTellAttackerClientToKill = (trueUnit.getStatus () == UnitStatusID.DEAD);
					manuallyTellDefenderClientToKill = (manuallyTellAttackerClientToKill) && (defendingPlayer != null);
					
					if (manuallyTellAttackerClientToKill)
						log.debug ("purgeDeadUnitsAndCombatSummonsFromCombat: Telling attacker to remove dead unit URN " + trueUnit.getUnitURN ());
					if (manuallyTellDefenderClientToKill)
						log.debug ("purgeDeadUnitsAndCombatSummonsFromCombat: Telling defender to remove dead unit URN " + trueUnit.getUnitURN ());
					
					// Use regular kill routine
					getFogOfWarMidTurnChanges ().killUnitOnServerAndClients (trueUnit, KillUnitActionID.FREE, null, trueMap, players, fogOfWarSettings, db);
				}
				else
				{
					// Even though we don't remove them on the server, tell the client to remove monsters left alive guarding nodes/lairs/towers
					manuallyTellAttackerClientToKill = (attackingNodeLairTower) && (defendingPlayer != null) &&
						(trueUnit.getOwningPlayerID () == defendingPlayer.getPlayerDescription ().getPlayerID ());
					
					// But make sure we don't remove monsters from the monster player's memory, since they know about their own units!
					manuallyTellDefenderClientToKill = false;
					if (manuallyTellAttackerClientToKill)
					{
						log.debug ("purgeDeadUnitsAndCombatSummonsFromCombat: Telling attacking player to remove NLT monster with unit URN " + trueUnit.getUnitURN ());
						monstersCount++;
					}
				}
				
				// Special case where we have to tell the client to kill off the unit outside of the FOW routines?
				if ((manuallyTellAttackerClientToKill) || (manuallyTellDefenderClientToKill))
				{
					final KillUnitMessageData manualKillMessageData = new KillUnitMessageData ();
					manualKillMessageData.setUnitURN (trueUnit.getUnitURN ());
					manualKillMessageData.setKillUnitActionID (KillUnitActionID.FREE);
				
					final KillUnitMessage manualKillMessage = new KillUnitMessage ();
					manualKillMessage.setData (manualKillMessageData);

					// Defender
					if (manuallyTellDefenderClientToKill)
					{
						final MomPersistentPlayerPrivateKnowledge defPriv = (MomPersistentPlayerPrivateKnowledge) defendingPlayer.getPersistentPlayerPrivateKnowledge ();
						getUnitUtils ().removeUnitURN (trueUnit.getUnitURN (), defPriv.getFogOfWarMemory ().getUnit ());

						if (defendingPlayer.getPlayerDescription ().isHuman ())
							defendingPlayer.getConnection ().sendMessageToClient (manualKillMessage);
					}
					
					// Attacker
					if (manuallyTellAttackerClientToKill)
					{
						final MomPersistentPlayerPrivateKnowledge atkPriv = (MomPersistentPlayerPrivateKnowledge) attackingPlayer.getPersistentPlayerPrivateKnowledge ();
						getUnitUtils ().removeUnitURN (trueUnit.getUnitURN (), atkPriv.getFogOfWarMemory ().getUnit ());
					
						if (attackingPlayer.getPlayerDescription ().isHuman ())
							attackingPlayer.getConnection ().sendMessageToClient (manualKillMessage);
					}
				}
			}
		
		log.debug ("purgeDeadUnitsAndCombatSummonsFromCombat permanently freed " +
			deadCount + " dead units and " + summonedCount + " summons, and told attacking client to free " + monstersCount + "monster defenders who''re still alive");
		log.trace ("Exiting purgeDeadUnitsAndCombatSummonsFromCombat");
	}

	/**
	 * Units are put into combat initially as part of the big StartCombat message rather than here.
	 * This routine is used a) for units added after a combat starts (e.g. summoning fire elementals or phantom warriors) and
	 * b) taking units out of combat when it ends.
	 * 
	 * Logic for taking units out of combat at the end very much mirrors what's in purgeDeadUnitsAndCombatSummonsFromCombat, e.g. don't
	 * try to take a monster in a lair out of combat in player's memory on server or client because purgeDeadUnitsAndCombatSummonsFromCombat will
	 * already have killed it off.
	 * 
	 * @param attackingPlayer Player who is attacking
	 * @param defendingPlayer Player who is defending - may be null if taking an empty lair, or a "walk in without a fight" in simultaneous turns games
	 * @param trueTerrain True overland terrain
	 * @param trueUnit The true unit being put into or taken out of combat
	 * @param terrainLocation The location the combat is taking place
	 * @param combatLocation For putting unit into combat, is the location the combat is taking place (i.e. = terrainLocation), for taking unit out of combat will be null
	 * @param combatPosition For putting unit into combat, is the starting position the unit is standing in on the battlefield, for taking unit out of combat will be null
	 * @param combatHeading For putting unit into combat, is the direction the the unit is heading on the battlefield, for taking unit out of combat will be null
	 * @param combatSide For putting unit into combat, specifies which side they're on, for taking unit out of combat will be null
	 * @param summonedBySpellID For summoning new units directly into combat (e.g. fire elementals) gives the spellID they were summoned with; otherwise null
	 * @param db Lookup lists built over the XML database
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 */
	@Override
	public final void setUnitIntoOrTakeUnitOutOfCombat (final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final MapVolumeOfMemoryGridCells trueTerrain,
		final MemoryUnit trueUnit, final MapCoordinates3DEx terrainLocation, final MapCoordinates3DEx combatLocation, final MapCoordinates2DEx combatPosition,
		final Integer combatHeading, final UnitCombatSideID combatSide, final String summonedBySpellID, final ServerDatabaseEx db)
		throws JAXBException, XMLStreamException, RecordNotFoundException
	{
		log.trace ("Entering setUnitIntoOrTakeUnitOutOfCombat: Unit URN " + trueUnit.getUnitURN () + ", " + combatSide + ", " + combatLocation);

		final MemoryGridCell tc = trueTerrain.getPlane ().get (terrainLocation.getZ ()).getRow ().get (terrainLocation.getY ()).getCell ().get (terrainLocation.getX ());
		
		// Is this someone attacking a node/lair/tower, and the combat is ending?
		// If DefendingPlayer is nil (we wiped out the monsters), there'll be no monsters to remove, so in which case we don't care that we get this value wrong
		final MomPersistentPlayerPublicKnowledge defPub = (defendingPlayer == null) ? null : (MomPersistentPlayerPublicKnowledge) defendingPlayer.getPersistentPlayerPublicKnowledge ();
		final boolean attackingNodeLairTower = (defPub != null) && (combatLocation == null) && (CommonDatabaseConstants.WIZARD_ID_MONSTERS.equals (defPub.getWizardID ())) &&
			ServerMemoryGridCellUtils.isNodeLairTower (tc.getTerrainData (), db);

		// Update true unit on server
		trueUnit.setCombatLocation (combatLocation);
		trueUnit.setCombatPosition (combatPosition);
		trueUnit.setCombatHeading (combatHeading);
		trueUnit.setCombatSide (combatSide);
		
		// Build message
		final SetUnitIntoOrTakeUnitOutOfCombatMessage msg = new SetUnitIntoOrTakeUnitOutOfCombatMessage ();
		msg.setUnitURN (trueUnit.getUnitURN ());
		msg.setCombatLocation (combatLocation);
		msg.setCombatPosition (combatPosition);
		msg.setCombatHeading (combatHeading);
		msg.setCombatSide (combatSide);
		msg.setSummonedBySpellID (summonedBySpellID);
		
		// Defending player may still be nil if we're attacking an empty lair
		if (defendingPlayer != null)
		{
			// Update player memory on server
			final MomPersistentPlayerPrivateKnowledge defPriv = (MomPersistentPlayerPrivateKnowledge) defendingPlayer.getPersistentPlayerPrivateKnowledge ();
			final MemoryUnit defUnit = getUnitUtils ().findUnitURN (trueUnit.getUnitURN (), defPriv.getFogOfWarMemory ().getUnit (), "setUnitIntoOrTakeUnitOutOfCombat-D");
			defUnit.setCombatLocation (combatLocation);
			defUnit.setCombatPosition (combatPosition);
			defUnit.setCombatHeading (combatHeading);
			defUnit.setCombatSide (combatSide);
			
			// Update on client
			if (defendingPlayer.getPlayerDescription ().isHuman ())
			{
				log.debug ("setUnitIntoOrTakeUnitOutOfCombat sending change in URN " + trueUnit.getUnitURN () + " to defender's client");
				defendingPlayer.getConnection ().sendMessageToClient (msg);
			}
		}

		// The exception here is we're attacking a node/lair/tower, and we lost.
		// In this case there's still monsters left alive - but by this stage we've already killed them off on the client, so unless we
		// specifically check for this, we end up telling the client to remove these 'dead' monsters from combat... and the client goes splat.
		// This has to match the condition in purgeDeadUnitsAndCombatSummonsFromCombat below, i.e. if purgeDeadUnitsAndCombatSummonsFromCombat
		// told the client to kill off the units, then don't try to take them out of combat here.
		final boolean manuallyTellAttackerClientToKill = (attackingNodeLairTower) && (defendingPlayer != null) &&
			(trueUnit.getOwningPlayerID () == defendingPlayer.getPlayerDescription ().getPlayerID ());
		if (!manuallyTellAttackerClientToKill)
		{
			// Update player memory on server
			final MomPersistentPlayerPrivateKnowledge atkPriv = (MomPersistentPlayerPrivateKnowledge) attackingPlayer.getPersistentPlayerPrivateKnowledge ();
			final MemoryUnit atkUnit = getUnitUtils ().findUnitURN (trueUnit.getUnitURN (), atkPriv.getFogOfWarMemory ().getUnit (), "setUnitIntoOrTakeUnitOutOfCombat-A");
			atkUnit.setCombatLocation (combatLocation);
			atkUnit.setCombatPosition (combatPosition);
			atkUnit.setCombatHeading (combatHeading);
			atkUnit.setCombatSide (combatSide);

			// Update on client
			if (attackingPlayer.getPlayerDescription ().isHuman ())
			{
				log.debug ("setUnitIntoOrTakeUnitOutOfCombat sending change in URN " + trueUnit.getUnitURN () + " to attacker's client");
				attackingPlayer.getConnection ().sendMessageToClient (msg);
			}
		}
		
		log.trace ("Exiting setUnitIntoOrTakeUnitOutOfCombat");
	}
	
	/**
	 * At the end of a combat, takes all units out of combat by nulling out all their combat values
	 * 
	 * @param attackingPlayer Player who is attacking
	 * @param defendingPlayer Player who is defending - may be null if taking an empty lair, or a "walk in without a fight" in simultaneous turns games
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param combatLocation The location the combat took place
	 * @param db Lookup lists built over the XML database
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 */
	@Override
	public final void removeUnitsFromCombat (final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final FogOfWarMemory trueMap,
		final MapCoordinates3DEx combatLocation, final ServerDatabaseEx db)
		throws JAXBException, XMLStreamException, RecordNotFoundException
	{
		log.trace ("Entering removeUnitsFromCombat: " + combatLocation);
		
		for (final MemoryUnit trueUnit : trueMap.getUnit ())
			if (combatLocation.equals (trueUnit.getCombatLocation ()))
				setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer, trueMap.getMap (), trueUnit, combatLocation, null, null, null, null, null, db);
		
		log.trace ("Exiting removeUnitsFromCombat");
	}
	
	/**
	 * @param tu Unit moving
	 * @param doubleMovementPoints Number of movement points it is moving in combat
	 */
	final void reduceMovementRemaining (final MemoryUnit tu, final int doubleMovementPoints)
	{
		tu.setDoubleCombatMovesLeft (Math.max (0, tu.getDoubleCombatMovesLeft () - doubleMovementPoints));
	}
	
	/**
	 * Once we have mapped out the directions and distances around the combat map and verified that our desired destination is
	 * fine, this routine actually handles sending the movement animations, performing the updates, and resolving any attacks.
	 *  
	 * This is separate so that it can be called directly by the AI.
	 *  
	 * @param tu The unit being moved
	 * @param moveTo The position within the combat map that the unit wants to move to (or attack)
	 * @param movementDirections The map of movement directions generated by calculateCombatMovementDistances
	 * @param movementTypes The map of movement types generated by calculateCombatMovementDistances
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void okToMoveUnitInCombat (final MemoryUnit tu, final MapCoordinates2DEx moveTo,
		final int [] [] movementDirections, final CombatMoveType [] [] movementTypes, final MomSessionVariables mom)
		throws MomException, PlayerNotFoundException, RecordNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering okToMoveUnitInCombat: Unit URN " + tu.getUnitURN ());
		
		// Find who the two players are
		final MapCoordinates3DEx combatLocation = (MapCoordinates3DEx) tu.getCombatLocation ();
		final CombatPlayers combatPlayers = getCombatMapUtils ().determinePlayersInCombatFromLocation
			(combatLocation, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), mom.getPlayers ());
		
		if (!combatPlayers.bothFound ())
			throw new MomException ("okToMoveUnitInCombat: One or other cell has no combat units left so player could not be determined");
		
		final PlayerServerDetails attackingPlayer = (PlayerServerDetails) combatPlayers.getAttackingPlayer ();
		final PlayerServerDetails defendingPlayer = (PlayerServerDetails) combatPlayers.getDefendingPlayer ();
		
		// Get the grid cell, so we can access the combat map
		final ServerGridCell combatCell = (ServerGridCell) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(combatLocation.getZ ()).getRow ().get (combatLocation.getY ()).getCell ().get (combatLocation.getX ());
		
		// Ranged attacks always reduce movement remaining to zero and never result in the unit actually moving.
		// The value gets sent to the client by resolveAttack below.
		if (movementTypes [moveTo.getY ()] [moveTo.getX ()] == CombatMoveType.RANGED)
			tu.setDoubleCombatMovesLeft (0);
		else
		{
			// The value at each cell of the directions grid is the direction we need to have come from to get there.
			// So we need to start at the destination and follow backwards down the movement path until we get back to the From location.
			final List<Integer> directions = new ArrayList<Integer> ();
			final MapCoordinates2DEx movePath = new MapCoordinates2DEx (moveTo);
			
			while (!movePath.equals (tu.getCombatPosition ()))
			{
				final int d = movementDirections [movePath.getY ()] [movePath.getX ()];
				directions.add (0, d);
				
				if (!getCoordinateSystemUtils ().move2DCoordinates (mom.getSessionDescription ().getCombatMapSize (), movePath,
					getCoordinateSystemUtils ().normalizeDirection (mom.getSessionDescription ().getCombatMapSize ().getCoordinateSystemType (), d+4)))
					
					throw new MomException ("okToMoveUnitInCombat: Server map tracing moved to a cell off the map (B)");
			}
			
			// We might not actually walk the last square if there is an enemy there - we might attack it instead, in which case we don't move.
			// However we still use up movement, as above.
			if ((movementTypes [moveTo.getY ()] [moveTo.getX ()] != CombatMoveType.MOVE) && (directions.size () > 0))
				directions.remove (directions.size () - 1);
			
			// Send direction messages to the client, reducing the unit's movement with each step
			// (so that's why we do this even if both players are AI)
			for (final int d : directions)
			{
				// Move to the new cell
				final MoveUnitInCombatMessage msg = new MoveUnitInCombatMessage ();
				msg.setUnitURN (tu.getUnitURN ());
				msg.setMoveFrom (new MapCoordinates2DEx (movePath));		// Message needs to keep the old coords, so copy them
				msg.setDirection (d);
				
				if (!getCoordinateSystemUtils ().move2DCoordinates (mom.getSessionDescription ().getCombatMapSize (), movePath, d))
					throw new MomException ("okToMoveUnitInCombat: Server map tracing moved to a cell off the map (F)");
				
				// How much movement did it take us to walk into this cell?
				reduceMovementRemaining (tu, getUnitCalculations ().calculateDoubleMovementToEnterCombatTile
					(combatCell.getCombatMap ().getRow ().get (movePath.getY ()).getCell ().get (movePath.getX ()), mom.getServerDB ()));
				msg.setDoubleCombatMovesLeft (tu.getDoubleCombatMovesLeft ());
				
				// Only send this to the players involved in the combat.
				// Players not involved in the combat don't care where the units are positioned.
				if (attackingPlayer.getPlayerDescription ().isHuman ())
					attackingPlayer.getConnection ().sendMessageToClient (msg);

				if (defendingPlayer.getPlayerDescription ().isHuman ())
					defendingPlayer.getConnection ().sendMessageToClient (msg);
			}
			
			// If the unit it making an attack, that takes half its total movement
			if (movementTypes [moveTo.getY ()] [moveTo.getX ()] != CombatMoveType.MOVE)
				reduceMovementRemaining (tu, mom.getServerDB ().findUnit (tu.getUnitID (), "okToMoveUnitInCombat").getDoubleMovement () / 2);
			
			// Actually put the units in that location on the server
			tu.setCombatPosition (movePath);
		
			// Update attacker's memory on server
			final MapCoordinates2DEx movePathAttackersMemory = new MapCoordinates2DEx (movePath);
			
			final MomPersistentPlayerPrivateKnowledge attackerPriv = (MomPersistentPlayerPrivateKnowledge) attackingPlayer.getPersistentPlayerPrivateKnowledge ();
			getUnitUtils ().findUnitURN (tu.getUnitURN (), attackerPriv.getFogOfWarMemory ().getUnit (), "okToMoveUnitInCombat-A").setCombatPosition (movePathAttackersMemory);

			// Update defender's memory on server
			final MapCoordinates2DEx movePathDefendersMemory = new MapCoordinates2DEx (movePath);
			
			final MomPersistentPlayerPrivateKnowledge defenderPriv = (MomPersistentPlayerPrivateKnowledge) defendingPlayer.getPersistentPlayerPrivateKnowledge ();
			getUnitUtils ().findUnitURN (tu.getUnitURN (), defenderPriv.getFogOfWarMemory ().getUnit (), "okToMoveUnitInCombat-D").setCombatPosition (movePathDefendersMemory);
		}
		
		// Anything special to do?
		switch (movementTypes [moveTo.getY ()] [moveTo.getX ()])
		{
			case MELEE:
				getDamageProcessor ().resolveAttack (tu, getUnitUtils ().findAliveUnitInCombatAt (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), combatLocation, moveTo),
					attackingPlayer, defendingPlayer,
					movementDirections [moveTo.getY ()] [moveTo.getX ()],
					false, combatLocation, mom);
				break;
				
			case RANGED:
				getDamageProcessor ().resolveAttack (tu, getUnitUtils ().findAliveUnitInCombatAt (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (),
					combatLocation, moveTo), attackingPlayer, defendingPlayer,
					getCoordinateSystemUtils ().determineDirectionTo (mom.getSessionDescription ().getCombatMapSize (), (MapCoordinates2DEx) tu.getCombatPosition (), moveTo),
					true, combatLocation, mom);
				break;
				
			case MOVE:
				// Nothing special to do
				break;

			case CANNOT_MOVE:
				// Should be impossible
				break;
		}
		
		log.trace ("Exiting okToMoveUnitInCombat");
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
	
	/**
	 * @return Methods for updating true map + players' memory
	 */
	public final FogOfWarMidTurnChanges getFogOfWarMidTurnChanges ()
	{
		return fogOfWarMidTurnChanges;
	}

	/**
	 * @param obj Methods for updating true map + players' memory
	 */
	public final void setFogOfWarMidTurnChanges (final FogOfWarMidTurnChanges obj)
	{
		fogOfWarMidTurnChanges = obj;
	}

	/**
	 * @return FOW duplication utils
	 */
	public final FogOfWarDuplication getFogOfWarDuplication ()
	{
		return fogOfWarDuplication;
	}

	/**
	 * @param dup FOW duplication utils
	 */
	public final void setFogOfWarDuplication (final FogOfWarDuplication dup)
	{
		fogOfWarDuplication = dup;
	}
	
	/**
	 * @return Simultaneous turns combat scheduler
	 */
	public final CombatScheduler getCombatScheduler ()
	{
		return combatScheduler;
	}

	/**
	 * @param scheduler Simultaneous turns combat scheduler
	 */
	public final void setCombatScheduler (final CombatScheduler scheduler)
	{
		combatScheduler = scheduler;
	}

	/**
	 * @return Combat map utils
	 */
	public final CombatMapUtils getCombatMapUtils ()
	{
		return combatMapUtils;
	}
	
	/**
	 * @param utils Combat map utils
	 */
	public final void setCombatMapUtils (final CombatMapUtils utils)
	{
		combatMapUtils = utils;
	}

	/**
	 * @return Combat AI
	 */
	public final CombatAI getCombatAI ()
	{
		return combatAI;
	}

	/**
	 * @param ai Combat AI
	 */
	public final void setCombatAI (final CombatAI ai)
	{
		combatAI = ai;
	}
	
	/**
	 * @return Damage processor
	 */
	public final DamageProcessor getDamageProcessor ()
	{
		return damageProcessor;
	}

	/**
	 * @param proc Damage processor
	 */
	public final void setDamageProcessor (final DamageProcessor proc)
	{
		damageProcessor = proc;
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

	/**
	 * @return Starting and ending combats
	 */
	public final CombatStartAndEnd getCombatStartAndEnd ()
	{
		return combatStartAndEnd;
	}

	/**
	 * @param cse Starting and ending combats
	 */
	public final void setCombatStartAndEnd (final CombatStartAndEnd cse)
	{
		combatStartAndEnd = cse;
	}

	/**
	 * @return Server only helper methods for dealing with players in a session
	 */
	public final MultiplayerSessionServerUtils getMultiplayerSessionServerUtils ()
	{
		return multiplayerSessionServerUtils;
	}

	/**
	 * @param obj Server only helper methods for dealing with players in a session
	 */
	public final void setMultiplayerSessionServerUtils (final MultiplayerSessionServerUtils obj)
	{
		multiplayerSessionServerUtils = obj;
	}
}