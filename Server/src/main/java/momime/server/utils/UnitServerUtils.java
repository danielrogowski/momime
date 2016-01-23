package momime.server.utils;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.FogOfWarSetting;
import momime.common.database.RecordNotFoundException;
import momime.common.database.StoredDamageTypeID;
import momime.common.database.UnitSpecialOrder;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.UnitDamage;
import momime.server.database.ServerDatabaseEx;

/**
 * Server side only helper methods for dealing with units
 */
public interface UnitServerUtils
{
	/**
	 * Creates and initializes a new unit - this is the equivalent of the TMomUnit.Create constructor in Delphi (except that it doesn't add the created unit into the unit list)
	 * @param unitID Type of unit to create
	 * @param unitURN Unique number identifying this unit
	 * @param weaponGrade Weapon grade to give to this unit
	 * @param startingExperience Initial experience; if -1 or null then experience won't be added into skill list, which is used when server sends units to client since they already have exp skill in list
	 * @param db Lookup lists built over the XML database
	 * @return Newly created unit
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 */
	public MemoryUnit createMemoryUnit (final String unitID, final int unitURN, final Integer weaponGrade, final Integer startingExperience,
		final ServerDatabaseEx db) throws RecordNotFoundException;

	/**
	 * Chooses a name for this hero (out of 5 possibilities) and rolls their random skills
	 * @param unit The hero to generate name and skills for
	 * @param db Lookup lists built over the XML database
	 * @throws MomException If we find a hero who has no possible names defined, or who needs a random skill and we can't find a suitable one
	 * @throws RecordNotFoundException If we can't find the definition for the unit
	 */
	public void generateHeroNameAndRandomSkills (final MemoryUnit unit, final ServerDatabaseEx db)
		throws MomException, RecordNotFoundException;
	
	/**
	 * @param order Special order that a unit has
	 * @return True if this special order results in the unit dying/being removed from play
	 */
	public boolean doesUnitSpecialOrderResultInDeath (final UnitSpecialOrder order);

	/**
	 * Sets a special order on a unit, and sends the special order to the player owning the unit
	 * 
	 * @param trueUnit Unit to give an order to
	 * @param specialOrder Order to give to this unit
	 * @param player Player who owns the unit
	 * @param trueTerrain True terrain map
	 * @param players List of players in the session
	 * @param db Lookup lists built over the XML database
	 * @param fogOfWarSettings Fog of war settings from session description
	 * @throws RecordNotFoundException If we can't find the unit in the player's memory (they don't know about their own unit?)
	 * @throws JAXBException If there is a problem sending the message to the client
	 * @throws XMLStreamException If there is a problem sending the message to the client
	 * @throws PlayerNotFoundException If the player who owns the unit cannot be found
	 * @throws MomException If the player's unit doesn't have the experience skill
	 */
	public void setAndSendSpecialOrder (final MemoryUnit trueUnit, final UnitSpecialOrder specialOrder, final PlayerServerDetails player,
		final MapVolumeOfMemoryGridCells trueTerrain, final List<PlayerServerDetails> players, final ServerDatabaseEx db, final FogOfWarSetting fogOfWarSettings)
		throws RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException, MomException;
	
	/**
	 * @param units List of units to search through
	 * @param playerID Player to search for
	 * @param unitID Unit ID to search for
	 * @return Unit with requested ID belonging to the requested player, or null if not found
	 */
	public MemoryUnit findUnitWithPlayerAndID (final List<MemoryUnit> units, final int playerID, final String unitID);

	/**
	 * When a unit is built or summoned, works out where to put it.
	 * If the city is already full, will resort to bumping the new unit into one of the outlying 8 squares.
	 * This is also used for when prisoners are rescued from a node/lair/tower.
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
	public UnitAddLocation findNearestLocationWhereUnitCanBeAdded (final MapCoordinates3DEx desiredLocation, final String unitID, final int playerID,
		final FogOfWarMemory trueMap, final MomSessionDescription sd, final ServerDatabaseEx db)
		throws RecordNotFoundException;

	/**
	 * @param units List of units to check through
	 * @param order Type of order to look for
	 * @return List of all units, regardless of which player they belong to, with the requested order
	 */
	public List<MemoryUnit> listUnitsWithSpecialOrder (final List<MemoryUnit> units, final UnitSpecialOrder order);

	/**
	 * Applys damage to a unit, optionally making defence rolls as each figure gets struck.
	 * NB. This doesn't actually record the damage against the unit, just calculates how many points of damage it will take.
	 * 
	 * @param defender Unit being hit
	 * @param hitsToApply The number of hits striking the defender (number that passed the attacker's to hit roll)
	 * @param defenderDefenceStrength Value of defence stat for the defender unit
	 * @param chanceToDefend Chance (0-10) for a defence point to block an incoming hit
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Number of hits actually applied to the unit, after any were maybe blocked by defence; also this will never be more than the HP the unit had
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 */
	public int applyDamage (final MemoryUnit defender, final int hitsToApply, final int defenderDefenceStrength, final int chanceToDefend,
		final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException;

	/**
	 * Adds damage to a unit; so will find and add to an existing damage type entry if one exists, or add one if it doesn't.
	 * 
	 * @param damages List of damages to add to
	 * @param damageType Type of damage to add
	 * @param damageTaken Amount of damage to add
	 */
	public void addDamage (final List<UnitDamage> damages, final StoredDamageTypeID damageType, final int damageTaken);
	
	/**
	 * @param damages List of damages to search
	 * @param damageType Type of damage to search for
	 * @return Amount of damage of this type in the list; if the requested type of damage isn't present in the list at all, will return 0
	 */
	public int findDamageTakenOfType (final List<UnitDamage> damages, final StoredDamageTypeID damageType);

	/**
	 * Heals a specified number of HP from the damage list.  If more HP is specified than exists in the list, the list will simply be emptied.
	 * Healable damage is always healed first, followed by permanent damage, and lastly life stealing damage.
	 * See comments on StoredDamageTypeID in MoMIMECommonDatabase.xsd. 
	 * 
	 * @param damages List of damages to heal
	 * @param amountToHeal Number of HP to heal
	 */
	public void healDamage (final List<UnitDamage> damages, final int amountToHeal);
	
	/**
	 * @param damages List of damages a unit had taken
	 * @return A special damage type, if the unit was at least half killed by a special damage type; otherwise will just return HEALABLE
	 */
	public StoredDamageTypeID whatKilledUnit (final List<UnitDamage> damages);
}