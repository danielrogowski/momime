package momime.server.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.calculations.CityCalculations;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.FogOfWarSetting;
import momime.common.database.RecordNotFoundException;
import momime.common.database.StoredDamageTypeID;
import momime.common.database.Unit;
import momime.common.database.UnitSetting;
import momime.common.database.UnitSkillAndValue;
import momime.common.database.UnitSpecialOrder;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MemoryUnitHeroItemSlot;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.NewTurnMessageHeroGainedALevel;
import momime.common.messages.NewTurnMessageTypeID;
import momime.common.messages.TurnSystem;
import momime.common.messages.UnitAddBumpTypeID;
import momime.common.messages.UnitDamage;
import momime.common.messages.UnitStatusID;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.PendingMovementUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.database.MapFeatureSvr;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.TileTypeSvr;
import momime.server.database.UnitSkillSvr;
import momime.server.database.UnitSvr;
import momime.server.database.UnitTypeSvr;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.messages.process.SpecialOrderButtonMessageImpl;
import momime.server.process.PlayerMessageProcessing;

/**
 * Server side only helper methods for dealing with units
 */
public final class UnitServerUtilsImpl implements UnitServerUtils
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (UnitServerUtilsImpl.class);
	
	/** Unit utils */
	private UnitUtils unitUtils;

	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** Pending movement utils */
	private PendingMovementUtils pendingMovementUtils;

	/** Random number generator */
	private RandomUtils randomUtils;

	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;

	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;

	/** MemoryGridCell utils */
	private MemoryGridCellUtils memoryGridCellUtils;
	
	/** Unit skill values direct access */
	private UnitSkillDirectAccess unitSkillDirectAccess;
	
	/** Server-only city utils */
	private CityServerUtils cityServerUtils;
	
	/** Server-only overland map utils */
	private OverlandMapServerUtils overlandMapServerUtils;
	
	/** Resource calculations */
	private ServerResourceCalculations serverResourceCalculations;

	/** Methods for dealing with player msgs */
	private PlayerMessageProcessing playerMessageProcessing;
	
	/** City calculations */
	private CityCalculations cityCalculations;
	
	/**
	 * @param unit Unit whose skills we want to output, not including bonuses from things like adamantium weapons, spells cast on the unit and so on
	 * @return Debug string listing out all the skills
	 */
	@Override
	public final String describeBasicSkillValuesInDebugString (final AvailableUnit unit)
	{
		String result = "";
		for (final UnitSkillAndValue thisSkill : unit.getUnitHasSkill ())
		{
			if (!result.equals (""))
				result = result + ", ";

			if ((thisSkill.getUnitSkillValue () != null) && (thisSkill.getUnitSkillValue () != 0))
				result = result + thisSkill.getUnitSkillValue () + "x";

			result = result + thisSkill.getUnitSkillID ();
		}

		return result;
	}
	
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
	@Override
	public final MemoryUnit createMemoryUnit (final String unitID, final int unitURN, final Integer weaponGrade, final Integer startingExperience,
		final ServerDatabaseEx db) throws RecordNotFoundException
	{
		log.trace ("Entering createMemoryUnit: " + unitID + ", Unit URN " + unitURN);

		final MemoryUnit newUnit = new MemoryUnit ();
		newUnit.setUnitURN (unitURN);
		newUnit.setUnitID (unitID);
		newUnit.setWeaponGrade (weaponGrade);
		newUnit.setStatus (UnitStatusID.ALIVE);		// Assume unit is alive - heroes being initialized will reset this value

		final Unit unitDef = getUnitUtils ().initializeUnitSkills (newUnit, startingExperience, db);
		
		// Add empty slots for hero items
		for (int slotNumber = 0; slotNumber < unitDef.getHeroItemSlot ().size (); slotNumber++)
			newUnit.getHeroItemSlot ().add (new MemoryUnitHeroItemSlot ());

		log.trace ("Exiting createMemoryUnit");
		return newUnit;
	}

	/**
	 * Chooses a name for this hero (out of 5 possibilities) and rolls their random skills
	 * 
	 * @param unit The hero to generate name and skills for
	 * @param db Lookup lists built over the XML database
	 * @throws MomException If we find a hero who has no possible names defined, or who needs a random skill and we can't find a suitable one
	 * @throws RecordNotFoundException If we can't find the definition for the unit
	 */
	@Override
	public final void generateHeroNameAndRandomSkills (final MemoryUnit unit, final ServerDatabaseEx db) throws MomException, RecordNotFoundException
	{
		log.trace ("Entering generateHeroNameAndRandomSkills: " + unit.getUnitID ());

		final UnitSvr unitDefinition = db.findUnit (unit.getUnitID (), "generateHeroNameAndRandomSkills");

		// Pick a name at random
		if (unitDefinition.getHeroName ().size () == 0)
			throw new MomException ("No hero names found for unit ID \"" + unit.getUnitID () + "\"");

		unit.setHeroNameID (unitDefinition.getHeroName ().get (getRandomUtils ().nextInt (unitDefinition.getHeroName ().size ())).getHeroNameID ());

		// Any random skills to add?
		if ( (unitDefinition.getHeroRandomPickCount () != null) && (unitDefinition.getHeroRandomPickCount () > 0))
		{
			log.debug ("Hero " + unit.getUnitID () + "' belonging to player ID " + unit.getOwningPlayerID () + " skills before rolling extras: " + describeBasicSkillValuesInDebugString (unit));

			// Run once for each random skill we get
			for (int pickNo = 0; pickNo < unitDefinition.getHeroRandomPickCount (); pickNo++)
			{
				// Get a list of all valid choices
				final List<String> skillChoices = new ArrayList<String> ();
				for (final UnitSkillSvr thisSkill : db.getUnitSkills ())
				{
					// We can spot hero skills since they'll have at least one of these values filled in
					if ( ( (thisSkill.getHeroSkillTypeID () != null) && (!thisSkill.getHeroSkillTypeID ().equals (""))) || ( (thisSkill.isOnlyIfHaveAlready () != null) && (thisSkill.isOnlyIfHaveAlready ())) || ( (thisSkill.getMaxOccurrences () != null) && (thisSkill.getMaxOccurrences () > 0)))
					{
						// Its a hero skill - do we have it already?
						final int currentSkillValue = getUnitSkillDirectAccess ().getDirectSkillValue (unit.getUnitHasSkill (), thisSkill.getUnitSkillID ());

						// Is it applicable?
						// If the unit has no hero random pick type specified, then it means it can use any hero skills
						// If the skill has no hero random pick type specified, then it means it can be used by any units
						if ( ( (unitDefinition.getHeroRandomPickType () == null) || (thisSkill.getHeroSkillTypeID () == null) || (unitDefinition.getHeroRandomPickType ().equals (thisSkill.getHeroSkillTypeID ()))) && ( (thisSkill.isOnlyIfHaveAlready () == null) || (!thisSkill.isOnlyIfHaveAlready ()) || (currentSkillValue > 0)) && ( (thisSkill.getMaxOccurrences () == null) || (currentSkillValue < thisSkill.getMaxOccurrences ())))

							skillChoices.add (thisSkill.getUnitSkillID ());
					}
				}

				// Pick one
				if (skillChoices.size () == 0)
					throw new MomException ("generateHeroNameAndRandomSkills: No valid hero skill choices for unit " + unit.getUnitID ());

				final String skillID = skillChoices.get (getRandomUtils ().nextInt (skillChoices.size ()));
				final int currentSkillValue = getUnitSkillDirectAccess ().getDirectSkillValue (unit.getUnitHasSkill (), skillID);
				if (currentSkillValue >= 0)
					getUnitSkillDirectAccess ().setDirectSkillValue (unit, skillID, currentSkillValue + 1);
				else
				{
					final UnitSkillAndValue newSkill = new UnitSkillAndValue ();
					newSkill.setUnitSkillID (skillID);
					newSkill.setUnitSkillValue (1);
					unit.getUnitHasSkill ().add (newSkill);
				}

				log.debug ("Hero " + unit.getUnitID () + "' belonging to player ID " + unit.getOwningPlayerID () + " skills after rolling extras: " + describeBasicSkillValuesInDebugString (unit));
			}
		}

		// Update status
		unit.setStatus (UnitStatusID.GENERATED);

		log.trace ("Exiting generateHeroNameAndRandomSkills");
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
	@Override
	public void setAndSendSpecialOrder (final MemoryUnit trueUnit, final UnitSpecialOrder specialOrder, final PlayerServerDetails player,
		final MapVolumeOfMemoryGridCells trueTerrain, final List<PlayerServerDetails> players, final ServerDatabaseEx db, final FogOfWarSetting fogOfWarSettings)
		throws RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering setAndSendSpecialOrder: Player ID " + player.getPlayerDescription ().getPlayerID () +
			", Unit URN " + trueUnit.getUnitURN () + ", " + specialOrder);

		// Setting a special order cancels any other kind of move
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		getPendingMovementUtils ().removeUnitFromAnyPendingMoves (priv.getPendingMovement (), trueUnit.getUnitURN ());

		// Set in true memory
		trueUnit.setSpecialOrder (specialOrder);

		// Update in player's memory and on clients
		getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (trueUnit, trueTerrain, players, db, fogOfWarSettings, null);

		log.trace ("Exiting setAndSendSpecialOrder");
	}
	
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
	@Override
	public final String processSpecialOrder (final List<Integer> unitURNs, final UnitSpecialOrder specialOrder, final MapCoordinates3DEx mapLocation,
		final PlayerServerDetails player, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering processSpecialOrder: Player ID " + player.getPlayerDescription ().getPlayerID () + ", " + specialOrder);
		
		// What skill do we need
		final String necessarySkillID;
		final boolean allowMultipleUnits;
		
		switch (specialOrder)
		{
			case BUILD_CITY:
				necessarySkillID = CommonDatabaseConstants.UNIT_SKILL_ID_CREATE_OUTPOST;
				allowMultipleUnits = false;
				break;
				
			case MELD_WITH_NODE:
				necessarySkillID = CommonDatabaseConstants.UNIT_SKILL_ID_MELD_WITH_NODE;
				allowMultipleUnits = false;
				break;

			case PURIFY:
				necessarySkillID = CommonDatabaseConstants.UNIT_SKILL_ID_PURIFY;
				allowMultipleUnits = true;
				break;
				
			case BUILD_ROAD:
				necessarySkillID = CommonDatabaseConstants.UNIT_SKILL_ID_BUILD_ROAD;
				allowMultipleUnits = true;
				break;
				
			case PATROL:
				necessarySkillID = null;
				allowMultipleUnits = true;
				break;
				
			default:
				throw new MomException (SpecialOrderButtonMessageImpl.class.getName () + " does not know skill ID corresponding to order of " + specialOrder);
		}
		
		// Get map cell
		final MemoryGridCell tc = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(mapLocation.getZ ()).getRow ().get (mapLocation.getY ()).getCell ().get (mapLocation.getX ());
		final TileTypeSvr tileType = mom.getServerDB ().findTileType (tc.getTerrainData ().getTileTypeID (), "SpecialOrderButtonMessageImpl");
		final MapFeatureSvr mapFeature = (tc.getTerrainData ().getMapFeatureID () == null) ? null : mom.getServerDB ().findMapFeature
			(tc.getTerrainData ().getMapFeatureID (), "SpecialOrderButtonMessageImpl");
		
		// Process through all the units
		String error = null;
		if (unitURNs.size () == 0)
			error = "You must select at least one unit to give a special order to.";

		final List<ExpandedUnitDetails> unitsWithNecessarySkillID = new ArrayList<ExpandedUnitDetails> ();

		final Iterator<Integer> unitUrnIterator = unitURNs.iterator ();
		while ((error == null) && (unitUrnIterator.hasNext ()))
		{
			final Integer thisUnitURN = unitUrnIterator.next ();
			final MemoryUnit thisUnit = getUnitUtils ().findUnitURN (thisUnitURN, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ());

			if (thisUnit == null)
				error = "Some of the units you are trying to give a special order to could not be found";
			else if (thisUnit.getOwningPlayerID () != player.getPlayerDescription ().getPlayerID ())
				error = "Some of the units you are trying to give a special order to belong to another player";
			else if (thisUnit.getStatus () != UnitStatusID.ALIVE)
				error = "Some of the units you are trying to give a special order to are dead/dismissed";
			else if (!thisUnit.getUnitLocation ().equals (mapLocation))
				error = "Some of the units you are trying to give a special order to are not at the right location";
			else
			{
				// Does it have the necessary skill?
				final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (thisUnit, null, null, null,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
				
				if ((necessarySkillID == null) || (xu.hasModifiedSkill (necessarySkillID)))				
					unitsWithNecessarySkillID.add (xu);
			}
		}

		// We must have at least one unit
		if ((error == null) && (unitsWithNecessarySkillID.size () == 0))
			error = "No unit in the unit stack has the necessary skill to perform the requested special order";
		
		// Is it an order that requires us to only have a single unit with the skill?
		if ((error == null) && (!allowMultipleUnits) && (unitsWithNecessarySkillID.size () > 1))
			switch (specialOrder)
			{
				case BUILD_CITY:
					error = "You must select only a single settler to build a city with";
					break;
					
				case MELD_WITH_NODE:
					error = "You must select only a single spirit to meld with a node";
					break;
					
				default:
					error = "You must select only a single unit with the relevant skill";
			}
		
		// Skill-specific validation
		if (error == null)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

			if ((specialOrder == UnitSpecialOrder.BUILD_CITY) && (!tileType.isCanBuildCity ()))
				error = "You can't build a city on this type of terrain";
			else if ((specialOrder == UnitSpecialOrder.BUILD_CITY) && (mapFeature != null) && (!mapFeature.isCanBuildCity ()))
				error = "You can't build a city on top of this type of map feature";
			else if ((specialOrder == UnitSpecialOrder.BUILD_CITY) && (getCityCalculations ().markWithinExistingCityRadius
				(mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), priv.getFogOfWarMemory ().getMap (),
				mapLocation.getZ (), mom.getSessionDescription ().getOverlandMapSize ()).get (mapLocation.getX (), mapLocation.getY ())))
				error = "Cities cannot be built within " + mom.getSessionDescription ().getOverlandMapSize ().getCitySeparation () + " squares of another city";
			else if ((specialOrder == UnitSpecialOrder.MELD_WITH_NODE) && (tileType.getMagicRealmID () == null))
				error = "Can only use the meld with node skill with node map squares";
			else if ((specialOrder == UnitSpecialOrder.MELD_WITH_NODE) && (player.getPlayerDescription ().getPlayerID ().equals (tc.getTerrainData ().getNodeOwnerID ())))
				error = "You already control this node so cannot meld with it again";
			else if ((specialOrder == UnitSpecialOrder.PURIFY) && (tc.getTerrainData ().getCorrupted () == null))
				error = "You can only use purify on corrupted terrain";
		}

		if (error == null)
		{
			// In a simultaneous turns game, settlers are put on special orders and the city isn't built until movement resolution.
			// But we still have to confirm to the client that their unit selection/build location was fine.
			// Multi-turn orders like purify and build road also just set the order here and tick up progress later.
			if ((mom.getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS) || (specialOrder == UnitSpecialOrder.PATROL) ||
				(specialOrder == UnitSpecialOrder.PURIFY) || (specialOrder == UnitSpecialOrder.BUILD_ROAD))
			{
				for (final ExpandedUnitDetails trueUnit : unitsWithNecessarySkillID)
					setAndSendSpecialOrder (trueUnit.getMemoryUnit (), specialOrder, player,
						mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting ());
			}
			else
			{
				// In a one-player-at-a-time game, actions take place immediately
				switch (specialOrder)
				{
					case BUILD_CITY:
						getCityServerUtils ().buildCityFromSettler (mom.getGeneralServerKnowledge (), player,
							unitsWithNecessarySkillID.get (0).getMemoryUnit (), mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());
						break;
						
					case MELD_WITH_NODE:
						// If successful, this will generate messages about the node capture
						getOverlandMapServerUtils ().attemptToMeldWithNode (unitsWithNecessarySkillID.get (0), mom.getGeneralServerKnowledge ().getTrueMap (),
							mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());
						
						getPlayerMessageProcessing ().sendNewTurnMessages (mom.getGeneralPublicKnowledge (), mom.getPlayers (), null);						
						break;
						
					default:
						throw new MomException (SpecialOrderButtonMessageImpl.class.getName () + " does not know how to handle order of " + specialOrder);
				}
			}
			
			// The settler was probably eating some rations and is now 'dead' so won't be eating those rations anymore
			// Or the spirit has now melded with the node and so a) is no longer consuming mana and b) we now get the big magic power boost from capturing the node
			getServerResourceCalculations ().recalculateGlobalProductionValues (player.getPlayerDescription ().getPlayerID (), false, mom);
		}
		
		log.trace ("Exiting processSpecialOrder = " + error);
		return error;
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
		log.trace ("Entering findUnitWithPlayerAndID: Player ID " + playerID + ", "  + unitID);

		MemoryUnit result = null;
		final Iterator<MemoryUnit> iter = units.iterator ();
		while ( (result == null) && (iter.hasNext ()))
		{
			final MemoryUnit thisUnit = iter.next ();
			if ( (thisUnit.getOwningPlayerID () == playerID) && (thisUnit.getUnitID ().equals (unitID)))
				result = thisUnit;
		}

		log.trace ("Exiting findUnitWithPlayerAndID = " + result);
		return result;
	}

	/**
	 * Internal method used by findNearestLocationWhereUnitCanBeAdded
	 * 
	 * @param addLocation Location that we're trying to add a unit
	 * @param testUnit Type of unit that we're trying to add
	 * @param trueMap Server's true knowledge of terrain, units and so on
	 * @param settings Unit settings from session description
	 * @param db Lookup lists built over the XML database
	 * @return Whether unit can be added here or not
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found
	 */
	final boolean canUnitBeAddedHere (final MapCoordinates3DEx addLocation, final ExpandedUnitDetails testUnit,
		final FogOfWarMemory trueMap, final UnitSetting settings, final ServerDatabaseEx db) throws RecordNotFoundException
	{
		log.trace ("Entering canUnitBeAddedHere: " + addLocation + ", " + testUnit.getDebugIdentifier ());

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

		// Also specifically check for nodes/lairs/towers, since although we've checked for units, we can't place a unit on top of an empty lair either.
		// But we can put units onto empty nodes and towers.
		final boolean okToAdd;
		if (!unitCheckOk)
			okToAdd = false;
		else
		{
			final MemoryGridCell tc = trueMap.getMap ().getPlane ().get (addLocation.getZ ()).getRow ().get (addLocation.getY ()).getCell ().get (addLocation.getX ());
			if (((tc.getTerrainData ().getMapFeatureID () != null) &&
				(db.findMapFeature (tc.getTerrainData ().getMapFeatureID (), "canUnitBeAddedHere").getMapFeatureMagicRealm ().size () > 0) &&
				(!getMemoryGridCellUtils ().isTerrainTowerOfWizardry (tc.getTerrainData ()))) ||
					
				// Terrain must be passable (so building boats get bumped to ocean tiles)
				(getUnitCalculations ().calculateDoubleMovementToEnterTileType (testUnit, testUnit.listModifiedSkillIDs (), tc.getTerrainData ().getTileTypeID (), db) == null))

				okToAdd = false;
			else
			{
				// Lastly check for someone else's empty city (although to have two adjacent cities would be a bit weird)
				if ( (tc.getCityData () != null) && (tc.getCityData ().getCityOwnerID () != testUnit.getOwningPlayerID ()))

					okToAdd = false;
				else
					okToAdd = true;
			}
		}

		log.trace ("Exiting canUnitBeAddedHere = " + okToAdd);
		return okToAdd;
	}

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
	@Override
	public final UnitAddLocation findNearestLocationWhereUnitCanBeAdded (final MapCoordinates3DEx desiredLocation, final String unitID, final int playerID,
		final FogOfWarMemory trueMap, final List<PlayerServerDetails> players, final MomSessionDescription sd, final ServerDatabaseEx db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering findNearestLocationWhereUnitCanBeAdded: " + desiredLocation + ", " + unitID + ", Player ID + " + playerID);

		// Create test unit
		final AvailableUnit testUnit = new AvailableUnit ();
		testUnit.setUnitID (unitID);
		testUnit.setOwningPlayerID (playerID);
		getUnitUtils ().initializeUnitSkills (testUnit, 0, db);

		final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (testUnit, null, null, null, players, trueMap, db);

		// First try the centre
		MapCoordinates3DEx addLocation = null;
		UnitAddBumpTypeID bumpType = UnitAddBumpTypeID.NO_ROOM;

		if (canUnitBeAddedHere (desiredLocation, xu, trueMap, sd.getUnitSetting (), db))
		{
			addLocation = desiredLocation;
			bumpType = UnitAddBumpTypeID.CITY;
		}
		else
		{
			int direction = 1;
			while ((addLocation == null) && (direction <= getCoordinateSystemUtils ().getMaxDirection (sd.getOverlandMapSize ().getCoordinateSystemType ())))
			{
				final MapCoordinates3DEx adjacentLocation = new MapCoordinates3DEx (desiredLocation);
				if (getCoordinateSystemUtils ().move3DCoordinates (sd.getOverlandMapSize (), adjacentLocation, direction))
					if (canUnitBeAddedHere (adjacentLocation, xu, trueMap, sd.getUnitSetting (), db))
					{
						addLocation = adjacentLocation;
						bumpType = UnitAddBumpTypeID.BUMPED;
					}

				direction++;
			}
		}

		final UnitAddLocation result = new UnitAddLocation (addLocation, bumpType);

		log.trace ("Exiting findNearestLocationWhereUnitCanBeAdded = " + result);
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
		log.trace ("Entering listUnitsWithSpecialOrder: " + order);

		final List<MemoryUnit> matches = new ArrayList<MemoryUnit> ();
		for (final MemoryUnit thisUnit : units)
			if ( (thisUnit.getStatus () == UnitStatusID.ALIVE) && (thisUnit.getSpecialOrder () == order))
				matches.add (thisUnit);

		log.trace ("Exiting listUnitsWithSpecialOrder = " + matches.size ());
		return matches;
	}

	/**
	 * Applys damage to a unit, optionally making defence rolls as each figure gets struck.
	 * NB. This doesn't actually record the damage against the unit, just calculates how many points of damage it will take.
	 * 
	 * @param defender Unit being hit
	 * @param hitsToApply The number of hits striking the defender (number that passed the attacker's to hit roll)
	 * @param defenderDefenceStrength Value of defence stat for the defender unit
	 * @param chanceToDefend Chance (0-10) for a defence point to block an incoming hit
	 * @return Number of hits actually applied to the unit, after any were maybe blocked by defence; also this will never be more than the HP the unit had
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	@Override
	public final int applyDamage (final ExpandedUnitDetails defender, final int hitsToApply, final int defenderDefenceStrength, final int chanceToDefend)
		throws MomException
	{
		log.trace ("Entering applyDamage: Unit URN " + defender.getUnitURN () + " hit by " + hitsToApply + " vs defence " + defenderDefenceStrength + " at " + chanceToDefend + "0%");

		// Dish out damage - See page 287 in the strategy guide
		// We can't do all defending in one go, each figure only gets to use its shields if the previous figure dies.
		// e.g. a unit of 8 spearmen has to take 2 hits, if all 8 spearmen get to try to block the 2 hits, they might not even lose 1 figure.
		// However only the first unit gets to use its shield, even if it blocks 1 hit it will be killed by the 2nd hit.
		int totalHits = 0;
		int defendingFiguresRemaining = defender.calculateAliveFigureCount ();
		int hitPointsRemainingOfFirstFigure = defender.calculateHitPointsRemainingOfFirstFigure ();
		int hitsLeftToApply = hitsToApply;
		
		while ((defendingFiguresRemaining > 0) && (hitsLeftToApply > 0))
		{
			// New figure taking damage, so it gets to try to block some hits
			int thisBlockedHits = 0;
			for (int blockNo = 0; blockNo < defenderDefenceStrength; blockNo++)
				if (getRandomUtils ().nextInt (10) < chanceToDefend)
					thisBlockedHits++;
			
			hitsLeftToApply = hitsLeftToApply - thisBlockedHits;
			
			// If any damage was not blocked by shields then it goes to health
			if (hitsLeftToApply > 0)
			{
				// Work out how many hits the current figure will take
				final int hitsOnThisFigure = Math.min (hitsLeftToApply, hitPointsRemainingOfFirstFigure);
				
				// Update counters for next figure.
				// Note it doesn't matter that we're decreasing defendingFigures even if the figure didn't die, because in that case Hits
				// will now be zero and the loop with exit, so the values of these variables won't matter at all, only the totalHits return value does.
				hitsLeftToApply = hitsLeftToApply - hitsOnThisFigure;
				totalHits = totalHits + hitsOnThisFigure;
				defendingFiguresRemaining--;
				hitPointsRemainingOfFirstFigure = defender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS);
			}
		}
		
		log.trace ("Exiting applyDamage = " + totalHits);
		return totalHits;
	}

	/**
	 * Adds damage to a unit; so will find and add to an existing damage type entry if one exists, or add one if it doesn't.
	 * 
	 * @param damages List of damages to add to
	 * @param damageType Type of damage to add
	 * @param damageTaken Amount of damage to add
	 */
	@Override
	public final void addDamage (final List<UnitDamage> damages, final StoredDamageTypeID damageType, final int damageTaken)
	{
		log.trace ("Entering addDamage: adding " + damageTaken + " hit points of damage type " + damageType);
		
		if (damageTaken != 0)
		{
			UnitDamage entry = null;
			final Iterator<UnitDamage> iter = damages.iterator ();
			while ((entry == null) && (iter.hasNext ()))
			{
				final UnitDamage thisDamage = iter.next ();
				if (thisDamage.getDamageType () == damageType)
					entry = thisDamage;
			}
			
			if (entry != null)
			{
				// Add to existing entry
				entry.setDamageTaken (entry.getDamageTaken () + damageTaken);
				
				// Remove it, if it is now zero
				if (entry.getDamageTaken () == 0)
					damages.remove (entry);
			}
			else
			{
				// Add new entry
				entry = new UnitDamage ();
				entry.setDamageType (damageType);
				entry.setDamageTaken (damageTaken);
				damages.add (entry);
			}
		}
		
		log.trace ("Exiting addDamage"); 
	}
	
	/**
	 * @param damages List of damages to search
	 * @param damageType Type of damage to search for
	 * @return Amount of damage of this type in the list; if the requested type of damage isn't present in the list at all, will return 0
	 */
	@Override
	public final int findDamageTakenOfType (final List<UnitDamage> damages, final StoredDamageTypeID damageType)
	{
		log.trace ("Entering findDamageTakenOfType: " + damageType);
		
		int value = 0;
		boolean found = false;
		final Iterator<UnitDamage> iter = damages.iterator ();
		while ((!found) && (iter.hasNext ()))
		{
			final UnitDamage thisDamage = iter.next ();
			if (thisDamage.getDamageType () == damageType)
			{
				found = true;
				value = thisDamage.getDamageTaken ();
			}
		}
		
		log.trace ("Exiting findDamageTakenOfType = " + value);
		return value;
	}
	
	/**
	 * Heals a specified number of HP from the damage list.  If more HP is specified than exists in the list, the list will simply be emptied.
	 * Healable damage is always healed first, followed by life stealing damage, and lastly permanent damage.
	 * See comments on StoredDamageTypeID in MoMIMECommonDatabase.xsd. 
	 * 
	 * @param damages List of damages to heal
	 * @param amountToHeal Number of HP to heal
	 * @param healPermanentDamage Whether we can heal permanent damage or not
	 */
	@Override
	public final void healDamage (final List<UnitDamage> damages, final int amountToHeal, final boolean healPermanentDamage)
	{
		log.trace ("Entering healDamage: " + amountToHeal + ", " + healPermanentDamage);
		
		if ((amountToHeal > 0) && (damages.size () > 0))
		{
			// We always heal in a specific order of damage types, which is the order they're defined on the enum, not the order they are in the list passed into this method
			int amountLeftToHeal = amountToHeal;
			for (final StoredDamageTypeID damageType : StoredDamageTypeID.values ())
				if ((amountLeftToHeal > 0) && ((healPermanentDamage) || (damageType != StoredDamageTypeID.PERMANENT)))
				{
					final int healThisDamageType = Math.min (amountLeftToHeal, findDamageTakenOfType (damages, damageType));
					if (healThisDamageType > 0)
					{
						amountLeftToHeal = amountLeftToHeal - healThisDamageType;
						addDamage (damages, damageType, -healThisDamageType);
					}
				}
		}
		
		log.trace ("Exiting healDamage");
	}
	
	/**
	 * @param damages List of damages a unit had taken
	 * @return A special damage type, if the unit was at least half killed by a special damage type; otherwise will just return HEALABLE
	 */
	@Override
	public final StoredDamageTypeID whatKilledUnit (final List<UnitDamage> damages)
	{
		log.trace ("Entering whatKilledUnit");
		
		// First work out how many HP we have to have taken of one damage type in order for it to count
		final int halfDamage = (getUnitUtils ().getTotalDamageTaken (damages) + 1) / 2;
		
		// Default to Healable, then check each damage type in turn
		// Important to note that we check Life Stealing first and jump out if we get a match, so in the case that a unit has an even number of HP and
		// takes exactly half Life Stealing and half Permanent damage, the Life Stealing "wins".
		StoredDamageTypeID result = StoredDamageTypeID.HEALABLE;
		for (final StoredDamageTypeID damageType : StoredDamageTypeID.values ())
			if ((result == StoredDamageTypeID.HEALABLE) && (findDamageTakenOfType (damages, damageType) >= halfDamage))
				result = damageType;
		
		log.trace ("Exiting whatKilledUnit = " + result);
		return result;
	}
	
	/**
	 * Checks if a hero just gianed a level (experience exactly equals one of the amounts listed in XML) and if so, sends the player a NTM about it
	 * 
	 * @param unitURN Unit to check
	 * @param unitType Which type of unit it is
	 * @param owningPlayer Who owns the unit
	 * @param experienceSkillValue The number of experience points the unit now has
	 */
	@Override
	public final void checkIfHeroGainedALevel (final int unitURN, final UnitTypeSvr unitType, final PlayerServerDetails owningPlayer, final int experienceSkillValue)
	{
		log.trace ("Entering checkIfHeroGainedALevel: Unit URN " + unitURN);
		
		if ((unitType.isAnnounceWhenLevelGained ()) && (owningPlayer.getPlayerDescription ().isHuman ()))
		{
			if ((unitType.getExperienceLevel ().stream ().anyMatch (lvl -> experienceSkillValue == lvl.getExperienceRequired ())))
			{
				final NewTurnMessageHeroGainedALevel ntm = new NewTurnMessageHeroGainedALevel ();
				ntm.setMsgType (NewTurnMessageTypeID.HERO_GAINED_A_LEVEL);
				ntm.setUnitURN (unitURN);
				
				((MomTransientPlayerPrivateKnowledge) owningPlayer.getTransientPlayerPrivateKnowledge ()).getNewTurnMessage ().add (ntm);
			}
		}
		
		log.trace ("Exiting checkIfHeroGainedALevel");
	}
	
	/**
	 * @param combatLocation Location of combat to check
	 * @param combatMap Scenery of the combat map at that location
	 * @param startPosition Position in the combat map to start checking from
	 * @param trueUnits List of true units
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return Closest free passable combat tile to startPosition; assumes it will eventually find one, will get error if parses the entire combat map and fails to find a suitable cell
	 * @throws RecordNotFoundException If we counter a combatTileBorderID or combatTileTypeID that can't be found in the db
	 */
	@Override
	public final MapCoordinates2DEx findFreeCombatPositionClosestTo (final MapCoordinates3DEx combatLocation, final MapAreaOfCombatTiles combatMap,
		final MapCoordinates2DEx startPosition, final List<MemoryUnit> trueUnits, final CoordinateSystem combatMapCoordinateSystem, final ServerDatabaseEx db)
		throws RecordNotFoundException
	{
		log.trace ("Entering findFreeCombatPositionClosestTo: " + combatLocation + ", " + startPosition);
		
		MapCoordinates2DEx found = null;
		final MapCoordinates2DEx coords = new MapCoordinates2DEx (startPosition);
		
		int ringNumber = 1;
		while (found == null)
		{
			// Move left
			getCoordinateSystemUtils ().move2DCoordinates (combatMapCoordinateSystem, coords, 7);

			int directionChk = 1;
			while ((found == null) && (directionChk <= 4))
			{
				final int d = directionChk * 2;
				int traverseSide = 1;
				while ((found == null) && (traverseSide <= ringNumber * 2))
				{
					// Move in direction d
					if (getCoordinateSystemUtils ().move2DCoordinates (combatMapCoordinateSystem, coords, d))
					{
						// Is this cell unoccupied + passable terrain?
						if ((getUnitUtils ().findAliveUnitInCombatAt (trueUnits, combatLocation, coords) == null) &&
							(getUnitCalculations ().calculateDoubleMovementToEnterCombatTile
								(combatMap.getRow ().get (coords.getY ()).getCell ().get (coords.getX ()), db) >= 0))
							
							found = coords;
					}
					
					traverseSide++;
				}
				
				directionChk++;
			}
			
			ringNumber++;
		}
		
		log.trace ("Exiting findFreeCombatPositionClosestTo = " + found);
		return found;
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
	 * @return Unit skill values direct access
	 */
	public final UnitSkillDirectAccess getUnitSkillDirectAccess ()
	{
		return unitSkillDirectAccess;
	}

	/**
	 * @param direct Unit skill values direct access
	 */
	public final void setUnitSkillDirectAccess (final UnitSkillDirectAccess direct)
	{
		unitSkillDirectAccess = direct;
	}

	/**
	 * @return Server-only city utils
	 */
	public final CityServerUtils getCityServerUtils ()
	{
		return cityServerUtils;
	}

	/**
	 * @param utils Server-only city utils
	 */
	public final void setCityServerUtils (final CityServerUtils utils)
	{
		cityServerUtils = utils;
	}

	/**
	 * @return Server-only overland map utils
	 */
	public final OverlandMapServerUtils getOverlandMapServerUtils ()
	{
		return overlandMapServerUtils;
	}
	
	/**
	 * @param utils Server-only overland map utils
	 */
	public final void setOverlandMapServerUtils (final OverlandMapServerUtils utils)
	{
		overlandMapServerUtils = utils;
	}

	/**
	 * @return Resource calculations
	 */
	public final ServerResourceCalculations getServerResourceCalculations ()
	{
		return serverResourceCalculations;
	}

	/**
	 * @param calc Resource calculations
	 */
	public final void setServerResourceCalculations (final ServerResourceCalculations calc)
	{
		serverResourceCalculations = calc;
	}

	/**
	 * @return Methods for dealing with player msgs
	 */
	public PlayerMessageProcessing getPlayerMessageProcessing ()
	{
		return playerMessageProcessing;
	}

	/**
	 * @param obj Methods for dealing with player msgs
	 */
	public final void setPlayerMessageProcessing (final PlayerMessageProcessing obj)
	{
		playerMessageProcessing = obj;
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
}