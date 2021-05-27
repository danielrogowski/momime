package momime.server.calculations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.FogOfWarSetting;
import momime.common.database.PickAndQuantity;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.UnitEx;
import momime.common.messages.CombatMapSize;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitStatusID;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.UnitUtils;
import momime.server.database.ServerDatabaseValues;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.KillUnitActionID;
import momime.server.utils.UnitServerUtils;

/**
 * Server only calculations pertaining to units, e.g. calculations relating to fog of war
 */
public final class ServerUnitCalculationsImpl implements ServerUnitCalculations
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (ServerUnitCalculationsImpl.class);
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** MemoryGridCell utils */
	private MemoryGridCellUtils memoryGridCellUtils;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** Random utils */
	private RandomUtils randomUtils; 
	
	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/**
	 * @param unit The unit to check
	 * @param db Lookup lists built over the XML database
	 * @return How many squares this unit can see; by default = 1, flying units automatically get 2, and the Scouting unit skill can push this even higher
	 * @throws RecordNotFoundException If we can't find the player who owns the unit, or the unit has a skill that we can't find in the cache
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	@Override
	public final int calculateUnitScoutingRange (final ExpandedUnitDetails unit, final CommonDatabase db) throws RecordNotFoundException, MomException
	{
		int scoutingRange = 1;

		// Actual scouting skill
		if (unit.hasModifiedSkill (ServerDatabaseValues.UNIT_SKILL_ID_SCOUTING))
			scoutingRange = Math.max (scoutingRange, unit.getModifiedSkillValue (ServerDatabaseValues.UNIT_SKILL_ID_SCOUTING));

		// Scouting range granted by other skills (i.e. flight skills)
		for (final String thisSkillID : unit.listModifiedSkillIDs ())
		{
			final Integer unitSkillScoutingRange = db.findUnitSkill (thisSkillID, "calculateUnitScoutingRange").getUnitSkillScoutingRange ();
			if (unitSkillScoutingRange != null)
				scoutingRange = Math.max (scoutingRange, unitSkillScoutingRange);
		}

		return scoutingRange;
	}

	/**
	 * Rechecks that transports have sufficient space to hold all units for whom the terrain is impassable.
	 * This is used after naval combats where some of the transports may have died, to kill off any surviving units who now have no transport,
	 * or perhaps a unit had Flight cast on it which was dispelled during combat.
	 * 
	 * @param combatLocation The combatLocation where the units need to be rechecked
	 * @param players List of players in this session, this can be passed in null for when units are being added to the map pre-game
	 * @param trueMap True terrain, buildings, spells and so on as known only to the server
	 * @param fogOfWarSettings Fog of war settings from session description
	 * @param db Lookup lists built over the XML database
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void recheckTransportCapacity (final MapCoordinates3DEx combatLocation, final FogOfWarMemory trueMap,
		final List<PlayerServerDetails> players, final FogOfWarSetting fogOfWarSettings, final CommonDatabase db)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException
	{
		// First get a list of the map coordinates and players to check; this could be two cells if the defender won - they'll have units at the combatLocation and the
		// attackers' transports may have been wiped out but the transported units are still sat at the point they attacked from.
		final List<MapCoordinates3DEx> mapLocations = new ArrayList<MapCoordinates3DEx> ();
		final List<Integer> playerIDs = new ArrayList<Integer> ();
		for (final MemoryUnit tu : trueMap.getUnit ())
			if ((tu.getStatus () == UnitStatusID.ALIVE) && (combatLocation.equals (tu.getCombatLocation ())))
			{
				if (!mapLocations.contains (tu.getUnitLocation ()))
					mapLocations.add ((MapCoordinates3DEx) tu.getUnitLocation ());
				
				if (!playerIDs.contains (tu.getOwningPlayerID ()))
					playerIDs.add (tu.getOwningPlayerID ());
			}
		
		// Now check all locations and all players
		for (final MapCoordinates3DEx mapLocation : mapLocations)
			for (final Integer playerID : playerIDs)
			{
				log.debug ("recheckTransportCapacity checking location " + mapLocation + " for units owned by player ID " + playerID);

				final OverlandMapTerrainData terrainData = trueMap.getMap ().getPlane ().get
					(mapLocation.getZ ()).getRow ().get (mapLocation.getY ()).getCell ().get (mapLocation.getX ()).getTerrainData ();
				
				// List all the units at this location owned by this player
				final List<ExpandedUnitDetails> unitStack = new ArrayList<ExpandedUnitDetails> ();
				for (final MemoryUnit tu : trueMap.getUnit ())
					if ((tu.getStatus () == UnitStatusID.ALIVE) && (mapLocation.equals (tu.getUnitLocation ())) && (playerID == tu.getOwningPlayerID ()))
						unitStack.add (getUnitUtils ().expandUnitDetails (tu, null, null, null, players, trueMap, db));
				
				// Get a list of the unit stack skills
				final Set<String> unitStackSkills = getUnitCalculations ().listAllSkillsInUnitStack (unitStack);
				
				// Now check each unit in the stack
				final List<ExpandedUnitDetails> impassableUnits = new ArrayList<ExpandedUnitDetails> ();
				int spaceRequired = 0;
				for (final ExpandedUnitDetails tu : unitStack)
				{
					final boolean impassable = (getUnitCalculations ().calculateDoubleMovementToEnterTileType (tu, unitStackSkills,
						getMemoryGridCellUtils ().convertNullTileTypeToFOW (terrainData, false), db) == null);
						
					// Count space granted by transports
					final Integer unitTransportCapacity = tu.getUnitDefinition ().getTransportCapacity ();
					if ((unitTransportCapacity != null) && (unitTransportCapacity > 0))
					{
						// Transports on impassable terrain just get killed (maybe a ship had its flight spell dispelled during an overland combat)
						if (impassable)
						{
							log.debug ("Killing Unit URN " + tu.getUnitURN () + " (transport on impassable terrain)");
							getFogOfWarMidTurnChanges ().killUnitOnServerAndClients (tu.getMemoryUnit (), KillUnitActionID.HEALABLE_OVERLAND_DAMAGE, trueMap, players, fogOfWarSettings, db);
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
					
					getFogOfWarMidTurnChanges ().killUnitOnServerAndClients (killUnit.getMemoryUnit (), KillUnitActionID.HEALABLE_OVERLAND_DAMAGE, trueMap, players, fogOfWarSettings, db);
					
					spaceRequired--;
					impassableUnits.remove (killUnit);
				}
			}
	}

	/**
	 * Non-magical ranged attack incurr a -10% to hit penalty for each 3 tiles distance between the attacking and defending unit on the combat map.
	 * This is loosely explained in the manual and strategy guide, but the info on the MoM wiki is clearer.
	 * 
	 * @param attacker Unit firing the ranged attack
	 * @param defender Unit being shot
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @return To hit penalty incurred from the distance between the attacker and defender, NB. this is not capped in any way so may get very high values here
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	@Override
	public final int calculateRangedAttackDistancePenalty (final ExpandedUnitDetails attacker, final ExpandedUnitDetails defender,
		final CombatMapSize combatMapCoordinateSystem) throws MomException
	{
		// Magic attacks suffer no penalty
		int penalty;
		if (attacker.getRangedAttackType ().getMagicRealmID () != null)
			penalty = 0;
		else
		{
			final double distance = getCoordinateSystemUtils ().determineReal2DDistanceBetween
				(combatMapCoordinateSystem, attacker.getCombatPosition (), defender.getCombatPosition ());
			
			penalty = (int) (distance / 3);
			
			// Long range skill?
			if ((penalty > 1) && (attacker.hasModifiedSkill (ServerDatabaseValues.UNIT_SKILL_ID_LONG_RANGE)))				
				penalty = 1;
		}
		
		return penalty;
	}
	
	/**
	 * Gets a list of all the units a summoning spell might summon if we cast it.  That's straightforward for normal summoning spells, but heroes can only be
	 * hired once and if killed are never available to summon again.  Plus some heroes are restricted depending on what our spell book picks are.
	 * 
	 * @param spell Summoning spell
	 * @param player Player casting the spell
	 * @param trueUnits List of true units
	 * @param db Lookup lists built over the XML database
	 * @return List of units this spell might summon if we cast it; list can be empty if we're already summoned and killed all heroes for example
	 * @throws RecordNotFoundException If one of the summoned unit IDs can't be found in the DB
	 */
	@Override
	public final List<UnitEx> listUnitsSpellMightSummon (final Spell spell, final PlayerServerDetails player, final List<MemoryUnit> trueUnits, final CommonDatabase db)
		throws RecordNotFoundException
	{
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		
		final List<UnitEx> possibleUnits = new ArrayList<UnitEx> ();
		for (final String possibleSummonedUnitID : spell.getSummonedUnit ())
		{
			// Check whether we can summon this unit If its a hero, this depends on whether we've summoned the hero before, or if he's dead
			final UnitEx possibleUnit = db.findUnit (possibleSummonedUnitID, "listUnitsSpellMightSummon");
			boolean addToList;
			if (possibleUnit.getUnitMagicRealm ().equals (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
			{
				final MemoryUnit hero = getUnitServerUtils ().findUnitWithPlayerAndID (trueUnits,
					player.getPlayerDescription ().getPlayerID (), possibleSummonedUnitID);

				if (hero == null)
					addToList = false;
				else
					addToList = ((hero.getStatus () == UnitStatusID.NOT_GENERATED) || (hero.getStatus () == UnitStatusID.GENERATED));
			}
			else
				addToList = true;
			
			// Check for units that require particular picks to summon
			final Iterator<PickAndQuantity> iter = possibleUnit.getUnitPickPrerequisite ().iterator ();
			while ((addToList) && (iter.hasNext ()))
			{
				final PickAndQuantity prereq = iter.next ();
				if (getPlayerPickUtils ().getQuantityOfPick (pub.getPick (), prereq.getPickID ()) < prereq.getQuantity ())
					addToList = false;
			}

			if (addToList)
				possibleUnits.add (possibleUnit);
		}
		
		return possibleUnits;
	}

	/**
	 * Similar to listUnitsSpellMightSummon, except lists all heroes who haven't been killed, and who we have the necessary spell book picks for. 
	 * 
	 * @param player Player recruiting heroes
	 * @param trueUnits List of true units
	 * @param db Lookup lists built over the XML database
	 * @return List of heroes available to us
	 */
	@Override
	public final List<UnitEx> listHeroesForHire (final PlayerServerDetails player, final List<MemoryUnit> trueUnits, final CommonDatabase db)
	{
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		
		final List<UnitEx> possibleUnits = new ArrayList<UnitEx> ();
		for (final UnitEx possibleUnit : db.getUnits ())
			if ((possibleUnit.getUnitMagicRealm ().equals (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO)) &&
				(possibleUnit.getHiringFame () != null) && (possibleUnit.getProductionCost () != null))
			{
				final MemoryUnit hero = getUnitServerUtils ().findUnitWithPlayerAndID (trueUnits,
					player.getPlayerDescription ().getPlayerID (), possibleUnit.getUnitID ());

				boolean addToList;
				if (hero == null)
					addToList = false;
				else
					addToList = ((hero.getStatus () == UnitStatusID.NOT_GENERATED) || (hero.getStatus () == UnitStatusID.GENERATED));
				
				// Check for units that require particular picks to summon
				final Iterator<PickAndQuantity> iter = possibleUnit.getUnitPickPrerequisite ().iterator ();
				while ((addToList) && (iter.hasNext ()))
				{
					final PickAndQuantity prereq = iter.next ();
					if (getPlayerPickUtils ().getQuantityOfPick (pub.getPick (), prereq.getPickID ()) < prereq.getQuantity ())
						addToList = false;
				}
	
				if (addToList)
					possibleUnits.add (possibleUnit);
			}
		
		return possibleUnits;
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
	 * @return Server-only unit utils
	 */
	public final UnitServerUtils getUnitServerUtils ()
	{
		return unitServerUtils;
	}

	/**
	 * @param utils Server-only unit utils
	 */
	public final void setUnitServerUtils (final UnitServerUtils utils)
	{
		unitServerUtils = utils;
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
	 * @return Player pick utils
	 */
	public final PlayerPickUtils getPlayerPickUtils ()
	{
		return playerPickUtils;
	}

	/**
	 * @param utils Player pick utils
	 */
	public final void setPlayerPickUtils (final PlayerPickUtils utils)
	{
		playerPickUtils = utils;
	}
}