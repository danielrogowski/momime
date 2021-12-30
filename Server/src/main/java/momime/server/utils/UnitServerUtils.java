package momime.server.utils;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.utils.Holder;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.StoredDamageTypeID;
import momime.common.database.UnitSpecialOrder;
import momime.common.database.UnitType;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.UnitDamage;
import momime.common.utils.ExpandedUnitDetails;
import momime.server.MomSessionVariables;

/**
 * Server side only helper methods for dealing with units
 */
public interface UnitServerUtils
{
	/**
	 * @param unit Unit whose skills we want to output, not including bonuses from things like adamantium weapons, spells cast on the unit and so on
	 * @return Debug string listing out all the skills
	 */
	public String describeBasicSkillValuesInDebugString (final AvailableUnit unit);
	
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
		final CommonDatabase db) throws RecordNotFoundException;

	/**
	 * Chooses a name for this hero (out of 5 possibilities) and rolls their random skills
	 * @param unit The hero to generate name and skills for
	 * @param db Lookup lists built over the XML database
	 * @throws MomException If we find a hero who has no possible names defined, or who needs a random skill and we can't find a suitable one
	 * @throws RecordNotFoundException If we can't find the definition for the unit
	 */
	public void generateHeroNameAndRandomSkills (final MemoryUnit unit, final CommonDatabase db)
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
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If we can't find the unit in the player's memory (they don't know about their own unit?)
	 * @throws JAXBException If there is a problem sending the message to the client
	 * @throws XMLStreamException If there is a problem sending the message to the client
	 * @throws PlayerNotFoundException If the player who owns the unit cannot be found
	 * @throws MomException If the player's unit doesn't have the experience skill
	 */
	public void setAndSendSpecialOrder (final MemoryUnit trueUnit, final UnitSpecialOrder specialOrder, final PlayerServerDetails player, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException, MomException;

	/**
	 * Attempts to process a special order (one of the buttons like Patrol or Build City in the right hand panel) on a unit stack.  Used by both human players and the AI.
	 * 
	 * @param unitURNs Units in the selected stack
	 * @param specialOrder Special order we want to process
	 * @param mapLocation Where we want to process the special order
	 * @param player Player who owns the units
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Error message if there was a problem; null = success
	 * @throws RecordNotFoundException If an expected data item cannot be found
	 * @throws JAXBException If there is a problem sending the message to the client
	 * @throws XMLStreamException If there is a problem sending the message to the client
	 * @throws PlayerNotFoundException If the player who owns the unit cannot be found
	 * @throws MomException If the player's unit doesn't have the experience skill
	 */
	public String processSpecialOrder (final List<Integer> unitURNs, final UnitSpecialOrder specialOrder, final MapCoordinates3DEx mapLocation,
		final PlayerServerDetails player, final MomSessionVariables mom)
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
	 * @param players List of players in the session
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @return Location + bump type; note class and bump type will always be filled in, but location may be null if the unit cannot fit anywhere
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public UnitAddLocation findNearestLocationWhereUnitCanBeAdded (final MapCoordinates3DEx desiredLocation, final String unitID, final int playerID,
		final FogOfWarMemory trueMap, final List<PlayerServerDetails> players, final MomSessionDescription sd, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;

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
	 * @param damageReductionApplies Whether the type of damage allows defence rolls or not; this is to differentiate between 0 defence
	 * 	because the unit has no defence (like Phantom Warriors) and 0 defence because the damage type allows no defence (like Doom Bolt)
	 * @param db Lookup lists built over the XML database
	 * @return Number of hits actually applied to the unit, after any were maybe blocked by defence; also this will never be more than the HP the unit had
	 * @throws MomException If there are any problems with the unit stats calculation
	 */
	public int applySingleFigureDamage (final ExpandedUnitDetails defender, final int hitsToApply, final int defenderDefenceStrength, final int chanceToDefend,
		final boolean damageReductionApplies, final CommonDatabase db) throws MomException;

	/**
	 * Makes attack and defence rolls for multi figure damage.  The scope of this is a bit different than applySingleFigureDamage, as for
	 * single figure damage we make all attack rolls up front (before that method is called), whereas with multi figure damage,
	 * the hits are rolled separately against each figure so are an integral part of the figure loop.
	 *  
	 * NB. This doesn't actually record the damage against the unit, just calculates how many points of damage it will take.
	 * 
	 * @param defender Unit being hit
	 * @param potentialHitsPerFigure The strength of the attack.  Each potential hit has chanceToHit chance of actually hitting, then the figure can defend to try to block it.
	 * @param chanceToHit Chance (0-10) for a potential hit to actually hit
	 * @param defenderDefenceStrength Value of defence stat for the defender unit
	 * @param chanceToDefend Chance (0-10) for a defence point to block an incoming hit
	 * @param actualDamage Placeholder to output number of potential hits which actually hit (before blocking)
	 * @param damageReductionApplies Whether the type of damage allows defence rolls or not; this is to differentiate between 0 defence
	 * 	because the unit has no defence (like Phantom Warriors) and 0 defence because the damage type allows no defence (like Doom Bolt)
	 * @param db Lookup lists built over the XML database
	 * @return Number of hits actually applied to the unit, after any were maybe blocked by defence; also this will never be more than the HP the unit had
	 * @throws MomException If there are any problems with the unit stats calculation
	 */
	public int applyMultiFigureDamage (final ExpandedUnitDetails defender, final int potentialHitsPerFigure, final int chanceToHit,
		final int defenderDefenceStrength, final int chanceToDefend, final Holder<Integer> actualDamage, final boolean damageReductionApplies,
		final CommonDatabase db) throws MomException;
	
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
	 * Healable damage is always healed first, followed by life stealing damage, and lastly permanent damage.
	 * See comments on StoredDamageTypeID in MoMIMECommonDatabase.xsd. 
	 * 
	 * @param damages List of damages to heal
	 * @param amountToHeal Number of HP to heal
	 * @param healPermanentDamage Whether we can heal permanent damage or not
	 */
	public void healDamage (final List<UnitDamage> damages, final int amountToHeal, final boolean healPermanentDamage);
	
	/**
	 * @param damages List of damages a unit had taken
	 * @return A special damage type, if the unit was at least half killed by a special damage type; otherwise will just return HEALABLE
	 */
	public StoredDamageTypeID whatKilledUnit (final List<UnitDamage> damages);

	/**
	 * Checks if a hero just gianed a level (experience exactly equals one of the amounts listed in XML) and if so, sends the player a NTM about it
	 * 
	 * @param unitURN Unit to check
	 * @param unitType Which type of unit it is
	 * @param owningPlayer Who owns the unit
	 * @param experienceSkillValue The number of experience points the unit now has
	 */
	public void checkIfHeroGainedALevel (final int unitURN, final UnitType unitType, final PlayerServerDetails owningPlayer, final int experienceSkillValue);

	/**
	 * Checks for units naturally reaching 120 exp with Heroism cast on them, in which case we automatically switch off the spell
	 * 
	 * @param mu Unit to check
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void checkIfNaturallyElite (final MemoryUnit mu, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException;
	
	/**
	 * This is used for the AI picking where to target in combat summoning spells like Fire Elemental.  As such it has to work the same way
	 * a human player targets spells, in that the AI player is not allowed to know the location of any invisible units it cannot see.  It may
	 * therefore pick a location which actually has a unit in it.  The spell casting code then deals with this.
	 * 
	 * @param xu Unit we are trying to summon in combat
	 * @param combatLocation Location of combat to check
	 * @param combatMap Scenery of the combat map at that location
	 * @param startPosition Position in the combat map to start checking from
	 * @param ourPlayerID Our player ID
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @return Closest free passable combat tile to startPosition, or null if it checks the whole combat map and no location is suitable
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public MapCoordinates2DEx findFreeCombatPositionClosestTo (final ExpandedUnitDetails xu,
		final MapCoordinates3DEx combatLocation, final MapAreaOfCombatTiles combatMap,
		final MapCoordinates2DEx startPosition, final int ourPlayerID, final List<PlayerServerDetails> players, final FogOfWarMemory mem,
		final CommonDatabase db, final CoordinateSystem combatMapCoordinateSystem)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;

	/**
	 * Like above, except this will avoid units even if they're invisible
	 * 
	 * @param xu Unit we are trying to summon in combat
	 * @param combatLocation Location of combat to check
	 * @param combatMap Scenery of the combat map at that location
	 * @param startPosition Position in the combat map to start checking from
	 * @param trueUnits List of true units
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return Closest free passable combat tile to startPosition, or null if it checks the whole combat map and no location is suitable
	 * @throws RecordNotFoundException If we counter a combatTileBorderID or combatTileTypeID that can't be found in the db
	 */
	public MapCoordinates2DEx findFreeCombatPositionAvoidingInvisibleClosestTo (final ExpandedUnitDetails xu,
		final MapCoordinates3DEx combatLocation, final MapAreaOfCombatTiles combatMap,
		final MapCoordinates2DEx startPosition, final List<MemoryUnit> trueUnits, final CoordinateSystem combatMapCoordinateSystem, final CommonDatabase db)
		throws RecordNotFoundException;
}