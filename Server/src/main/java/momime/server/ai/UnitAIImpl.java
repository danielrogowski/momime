package momime.server.ai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.calculations.CityCalculations;
import momime.common.calculations.UnitCalculations;
import momime.common.database.AiMovementCode;
import momime.common.database.AiUnitCategory;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItemSlotType;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.TileType;
import momime.common.database.UnitEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MemoryUnitHeroItemSlot;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.SpellResearchStatusID;
import momime.common.messages.TurnSystem;
import momime.common.messages.UnitStatusID;
import momime.common.movement.OverlandMovementCell;
import momime.common.movement.UnitMovement;
import momime.common.movement.UnitStack;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.PlayerKnowledgeUtils;
import momime.common.utils.SampleUnitUtils;
import momime.common.utils.SpellUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerUnitCalculations;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.FogOfWarMidTurnMultiChanges;
import momime.server.process.CityProcessing;
import momime.server.utils.UnitServerUtils;

/**
 * Methods for AI players evaluating the strength of units
 */
public final class UnitAIImpl implements UnitAI
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (UnitAIImpl.class);
	
	/** Rampaging monsters only attack, they won't merge stacks together do anything clever */
	private final static List<AiMovementCode> RAMPAGING_MONSTERS_MOVEMENT_CODES = Arrays.asList
		(AiMovementCode.REINFORCE, AiMovementCode.ATTACK_STATIONARY, AiMovementCode.ATTACK_WANDERING);

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** City calculations */
	private CityCalculations cityCalculations;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** Spell utils */
	private SpellUtils spellUtils;
	
	/** Methods dealing with unit movement */
	private UnitMovement unitMovement;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnMultiChanges fogOfWarMidTurnMultiChanges;
	
	/** MemoryGridCell utils */
	private MemoryGridCellUtils memoryGridCellUtils;
	
	/** Unit AI movement decisions */
	private UnitAIMovement unitAIMovement;
	
	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;
	
	/** Underlying methods that the AI uses to calculate ratings about how good units are */
	private AIUnitRatingCalculations aiUnitRatingCalculations;
	
	/** Methods that the AI uses to calculate stats about types of units and rating how good units are */
	private AIUnitCalculations aiUnitCalculations;
		
	/** AI decisions about cities */
	private CityAI cityAI;
	
	/** City processing methods */
	private CityProcessing cityProcessing;
	
	/** Server-only unit calculations */
	private ServerUnitCalculations serverUnitCalculations;
	
	/** Methods that the AI uses to calculate ratings about how good hero items are */
	private AIHeroItemRatingCalculations aiHeroItemRatingCalculations;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/** Sample unit method */
	private SampleUnitUtils sampleUnitUtils;
	
	/** Methods for working with wizardIDs */
	private PlayerKnowledgeUtils playerKnowledgeUtils;
	
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/**
	 * Lists every unit this AI player can build at every city they own, as well as any units they can summon, sorted with the best units first.
	 * This won't list heroes, since if we cast Summon Hero/Champion, we never know which one we're going to get.
	 * Will not include any units that we wouldn't be able to afford the maintenance cost of if we constructed them
	 * (mana for summoned units; gold for units constructed in cities - will ignore rations since we can always allocate more farmers).
	 * 
	 * @param player AI player who is considering constructing a unit
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return List of all possible units this AI player can construct or summon, sorted with the best first
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final List<AIConstructableUnit> listAllUnitsWeCanConstruct (final PlayerServerDetails player, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		final KnownWizardDetails wizardDetails = getKnownWizardUtils ().findKnownWizardDetails
			(mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), player.getPlayerDescription ().getPlayerID (), "listAllUnitsWeCanConstruct");

		final List<AIConstructableUnit> results = new ArrayList<AIConstructableUnit> ();
		
		// Units we can construct in cities
		for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
			for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
				for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
				{
					final OverlandMapCityData cityData = priv.getFogOfWarMemory ().getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x).getCityData ();
					if ((cityData != null) && (cityData.getCityOwnerID () == player.getPlayerDescription ().getPlayerID ()) && (cityData.getCityPopulation () >= CommonDatabaseConstants.MIN_CITY_POPULATION))
					{
						final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (x, y, z);
						
						final List<UnitEx> unitDefs = getCityCalculations ().listUnitsCityCanConstruct (cityLocation,
							priv.getFogOfWarMemory ().getMap (), priv.getFogOfWarMemory ().getBuilding (), mom.getServerDB ());
						
						for (final UnitEx unitDef : unitDefs)
						{
							// Need real example of the unit so that we property take into account if we have
							// e.g. retorts that make it cheaper to maintained summoned creatures, or so on 
							final ExpandedUnitDetails xu = getSampleUnitUtils ().createSampleUnitFromCity (unitDef.getUnitID (), wizardDetails, cityLocation,
								mom.getPlayers (), priv.getFogOfWarMemory (), mom.getSessionDescription ().getOverlandMapSize (), mom.getServerDB ());
							
							results.add (new AIConstructableUnit (unitDef, cityLocation, null,
								getAiUnitCalculations ().calculateUnitAverageRating (xu.getUnit (), xu, mom.getPlayers (), priv.getFogOfWarMemory (), mom.getServerDB ()),
								getAiUnitCalculations ().determineAIUnitType (xu),
								getAiUnitCalculations ().canAffordUnitMaintenance (player, wizardDetails, mom.getPlayers (),
									xu.getUnit (), mom.getSessionDescription ().getSpellSetting (), mom.getServerDB ())));
						}									
					}
				}
		
		// Summonining spells we know
		if (getPlayerKnowledgeUtils ().isWizard (wizardDetails.getWizardID ()))
			for (final Spell spell : mom.getServerDB ().getSpell ())
				if ((spell.getSpellBookSectionID () == SpellBookSectionID.SUMMONING) && (spell.getOverlandCastingCost () != null) &&
					(getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), spell.getSpellID ()).getStatus () == SpellResearchStatusID.AVAILABLE))
				{
					final List<UnitEx> unitDefs = getServerUnitCalculations ().listUnitsSpellMightSummon (spell, wizardDetails,
						mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), mom.getServerDB ());
					for (final UnitEx unitDef : unitDefs)
						if (!unitDef.getUnitMagicRealm ().equals (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
						{
							final ExpandedUnitDetails xu = getSampleUnitUtils ().createSampleUnit (unitDef.getUnitID (), player.getPlayerDescription ().getPlayerID (), null,
								mom.getPlayers (), priv.getFogOfWarMemory (), mom.getServerDB ());
							
							results.add (new AIConstructableUnit (unitDef, null, spell,
								getAiUnitCalculations ().calculateUnitAverageRating (xu.getUnit (), xu, mom.getPlayers (), priv.getFogOfWarMemory (), mom.getServerDB ()),
								getAiUnitCalculations ().determineAIUnitType (xu),
								getAiUnitCalculations ().canAffordUnitMaintenance (player, wizardDetails, mom.getPlayers (),
									xu.getUnit (), mom.getSessionDescription ().getSpellSetting (), mom.getServerDB ())));
						}
				}
		
		// Sort the results
		Collections.sort (results);
		return results;
	}

	/**
	 * Calculates the current and average rating of every unit we can see on the map.
	 * 
	 * @param ourUnits Array to populate our unit ratings into
	 * @param enemyUnits Array to populate enemy unit ratings into
	 * @param playerID Player ID to consider as "our" units
	 * @param ignorePlayerID Player ID whose units to ignore, usually null
	 * @param mem Memory data known to playerID
	 * @param players Players list
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final void calculateUnitRatingsAtEveryMapCell (final AIUnitsAndRatings [] [] [] ourUnits, final AIUnitsAndRatings [] [] [] enemyUnits,
		final int playerID, final Integer ignorePlayerID, final FogOfWarMemory mem, final List<PlayerServerDetails> players, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		for (final MemoryUnit mu : mem.getUnit ())
			if ((mu.getStatus () == UnitStatusID.ALIVE) &&
				((ignorePlayerID == null) || (mu.getOwningPlayerID () != ignorePlayerID)))
			{
				final AIUnitsAndRatings [] [] [] unitArray = (mu.getOwningPlayerID () == playerID) ? ourUnits : enemyUnits;
				AIUnitsAndRatings unitList = unitArray [mu.getUnitLocation ().getZ ()] [mu.getUnitLocation ().getY ()] [mu.getUnitLocation ().getX ()];
				if (unitList == null)
				{
					unitList = new AIUnitsAndRatingsImpl ();
					unitArray [mu.getUnitLocation ().getZ ()] [mu.getUnitLocation ().getY ()] [mu.getUnitLocation ().getX ()] = unitList; 
				}

				final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (mu, null, null, null, players, mem, db);
				
				unitList.add (new AIUnitAndRatings (mu,
					getAiUnitCalculations ().determineAIUnitType (xu),
					getAiUnitRatingCalculations ().calculateUnitCurrentRating (mu, xu, players, mem, db),
					getAiUnitCalculations ().calculateUnitAverageRating (mu, xu, players, mem, db)));
			}
	}
	
	/**
	 * Checks every city, node, lair and tower that we either own or is undefended, and checks how much short of our desired defence level it currently is.
	 * As a side effect, any units where we have too much defence, or units which are not in a defensive location, are put into a list of mobile units.
	 * 
	 * @param ourUnits Array of our unit ratings populated by calculateUnitRatingsAtEveryMapCell
	 * @param enemyUnits Array of enemy unit ratings populated by calculateUnitRatingsAtEveryMapCell
	 * @param mobileUnits List to populate with details of all units that are in excess of defensive requirements, or are not in defensive positions
	 * @param playerID Player ID to consider as "our" units
	 * @param isRaiders Whether it is the raiders player
	 * @param mem Memory data known to playerID
	 * @param highestAverageRating Rating for the best unit we can construct in any city, as a guage for the strength of units we should be defending with
	 * @param turnNumber Current turn number
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return List of all defence locations, and how many points short we are of our desired defence level
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found
	 */
	@Override
	public final List<AIDefenceLocation> evaluateCurrentDefence (final AIUnitsAndRatings [] [] [] ourUnits, final AIUnitsAndRatings [] [] [] enemyUnits,
		final List<AIUnitAndRatings> mobileUnits, final int playerID, final boolean isRaiders, final FogOfWarMemory mem, final int highestAverageRating, final int turnNumber,
		final CoordinateSystem sys, final CommonDatabase db) throws RecordNotFoundException
	{
		final List<AIDefenceLocation> underdefendedLocations = new ArrayList<AIDefenceLocation> ();
		
		for (int z = 0; z < sys.getDepth (); z++)
			for (int y = 0; y < sys.getHeight (); y++)
				for (int x = 0; x < sys.getWidth (); x++)
				{
					// Only process towers on the first plane
					final MemoryGridCell mc = mem.getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x);
					final OverlandMapTerrainData terrainData = mc.getTerrainData ();
					if ((z == 0) || (!getMemoryGridCellUtils ().isTerrainTowerOfWizardry (terrainData)))
					{					
						final AIUnitsAndRatings ours = ourUnits [z] [y] [x];
						final AIUnitsAndRatings theirs = enemyUnits [z] [y] [x];
						
						final OverlandMapCityData cityData = mc.getCityData ();
						if ((theirs == null) &&
							((cityData != null) ||
								((!isRaiders) && (getMemoryGridCellUtils ().isNodeLairTower (terrainData, db))))) 
						{
							final MapCoordinates3DEx coords = new MapCoordinates3DEx (x, y, z);
							
							final int defenceRating = (ours == null) ? 0 : ours.totalCombatUnitAverageRatings ();
							final String description = (cityData != null) ? ("city belonging to " + cityData.getCityOwnerID ()) : (terrainData.getTileTypeID () + "/" + terrainData.getMapFeatureID ());
							
							// For any regular locations, on turn 1..50 we want 1 defender, 51..100 want 2, 101..150 want 3, 151..200 want 4 and 201+ want 5
							// For wizard's fortress, on turn 1..25 we want 1 defender, 26..50 want 2, and so on up to 201+ want 9
							final int desiredDefenderCount = (getMemoryBuildingUtils ().findBuilding (mem.getBuilding (), coords, CommonDatabaseConstants.BUILDING_FORTRESS) == null) ?
								((Math.min (turnNumber, 201) + 49) / 50) :
								((Math.min (turnNumber, 201) + 24) / 25);
							
							final int desiredDefenceRating = highestAverageRating * desiredDefenderCount;
							
							log.debug ("AI Player ID " + playerID + " sees " + description +
								" at (" + x + ", " + y + ", " + z + "), CDR = " + defenceRating + ", DDC = " + desiredDefenderCount + ", DDR = " + desiredDefenceRating);
							
							if (defenceRating < desiredDefenceRating)
							{
								underdefendedLocations.add (new AIDefenceLocation (coords, desiredDefenceRating - defenceRating));
								
								// Even though its underdefended, specialist units like settlers are always movable
								if (ours != null)
								{
									final Iterator<AIUnitAndRatings> iter = ours.iterator ();
									while (iter.hasNext ())
									{
										final AIUnitAndRatings thisUnit = iter.next ();
										if (thisUnit.getAiUnitType () != AIUnitType.COMBAT_UNIT)
										{
											iter.remove ();
											mobileUnits.add (thisUnit);
										}
									}
								}
							}
							else if (defenceRating > desiredDefenceRating)
							{
								// Maybe we have too much defence?  Can we lose a unit or two without becoming underdefended?
								// Important note: the sorting order makes this tend to keep stronger units guarding cities and
								// send weaker units out over the map, in that way we ensure that weaker units get killed off and replaced.
								Collections.sort (ours);
								int defenceSoFar = 0;
								final Iterator<AIUnitAndRatings> iter = ours.iterator ();
								while (iter.hasNext ())
								{
									final AIUnitAndRatings thisUnit = iter.next ();
									if ((defenceSoFar >= desiredDefenceRating) || (thisUnit.getAiUnitType () != AIUnitType.COMBAT_UNIT))
									{
										iter.remove ();
										mobileUnits.add (thisUnit);
									}
									else
										defenceSoFar = defenceSoFar + thisUnit.getAverageRating ();
								}
							}
							
							if ((ours != null) && (ours.size () == 0))
								ourUnits [z] [y] [x] = null;
						}
	
						// All units not in defensive positions are automatically mobile
						else if (ours != null)
						{
							mobileUnits.addAll (ours);
							ourUnits [z] [y] [x] = null;
						}
					}
				}
		
		Collections.sort (underdefendedLocations);
		return underdefendedLocations;
	}
	
	/**
	 * evaluateCurrentDefence lists every city, node, lair and tower that we either own or is undefended that we want to send units to.
	 * The rampaging monsters player isn't interested in nodes/lairs/towers so the equivalent for them is to list all undefended wizard owned cities.
	 * 
	 * @param enemyUnits Array of enemy unit ratings populated by calculateUnitRatingsAtEveryMapCell
	 * @param playerID Player ID to consider as "our" units
	 * @param ignorePlayerID Player ID whose cities to ignore
	 * @param terrain Known terrain
	 * @param sys Overland map coordinate system
	 * @return List of all defence locations, and how many points short we are of our desired defence level
	 */
	@Override
	public final List<AIDefenceLocation> listUndefendedWizardCities (final AIUnitsAndRatings [] [] [] enemyUnits,
		final int playerID, final int ignorePlayerID, final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys)
	{
		final List<AIDefenceLocation> undefendedWizardCities = new ArrayList<AIDefenceLocation> ();
		
		for (int z = 0; z < sys.getDepth (); z++)
			for (int y = 0; y < sys.getHeight (); y++)
				for (int x = 0; x < sys.getWidth (); x++)
				{
					final AIUnitsAndRatings theirs = enemyUnits [z] [y] [x];
					final OverlandMapCityData cityData = terrain.getPlane ().get (z).getRow ().get (y).getCell ().get (x).getCityData ();
					if ((theirs == null) && (cityData != null) && (cityData.getCityOwnerID () != ignorePlayerID))
					{
						log.debug ("Rampaging monsters Player ID " + playerID + " sees undefended wizard city at (" + x + ", " + y + ", " + z + ")");						
						undefendedWizardCities.add (new AIDefenceLocation (new MapCoordinates3DEx (x, y, z), 0));
					}
				}
		
		return undefendedWizardCities;
	}

	/**
	 * Checks every city, node, lair and tower that we either own or is undefended, and checks how much short of our desired defence level it currently is.
	 * As a side effect, any units where we have too much defence, or units which are not in a defensive location, are put into a list of mobile units.
	 * 
	 * @param ourUnits Array of our unit ratings populated by calculateUnitRatingsAtEveryMapCell
	 * @param mobileUnits List to populate with details of all units that are in excess of defensive requirements, or are not in defensive positions
	 * @param terrain Known terrain
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found
	 */
	@Override
	public final void listUnitsNotInNodeLairTowers (final AIUnitsAndRatings [] [] [] ourUnits, final List<AIUnitAndRatings> mobileUnits,
		final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys, final CommonDatabase db) throws RecordNotFoundException
	{
		for (int z = 0; z < sys.getDepth (); z++)
			for (int y = 0; y < sys.getHeight (); y++)
				for (int x = 0; x < sys.getWidth (); x++)
				{
					final OverlandMapTerrainData terrainData = terrain.getPlane ().get (z).getRow ().get (y).getCell ().get (x).getTerrainData ();
					final AIUnitsAndRatings ours = ourUnits [z] [y] [x];
					
					if ((ours != null) && (!getMemoryGridCellUtils ().isNodeLairTower (terrainData, db)))
						mobileUnits.addAll (ours);
				}
	}
	
	/**
	 * @param xu Unit to test
	 * @param category Category to test whether the unit matches or not
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Whether the unit matches the specified category or not
	 * @throws RecordNotFoundException If we encounter a transport that we can't find the unit definition for
	 */
	final boolean unitMatchesCategory (final ExpandedUnitDetails xu, final AiUnitCategory category, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException
	{
		// Do the relatively easy checks first
		boolean matches = ((category.getUnitSkillID () == null) || (xu.hasModifiedSkill (category.getUnitSkillID ()))) &&
			((category.isTransport () == null) || (!category.isTransport ()) || ((xu.getUnitDefinition ().getTransportCapacity () != null) && (xu.getUnitDefinition ().getTransportCapacity () > 0))) &&
			((category.isAllTerrainPassable () == null) || (!category.isAllTerrainPassable ()) || (getUnitCalculations ().areAllTerrainTypesPassable (xu, xu.listModifiedSkillIDs (), db)));
			
		// Now check if we need to be loaded in a transport, which is a bit more involved
		if (matches && (category.isInTransport () != null) && (category.isInTransport ()))
		{
			final OverlandMapTerrainData terrainData = mem.getMap ().getPlane ().get (xu.getUnitLocation ().getZ ()).getRow ().get
				(xu.getUnitLocation ().getY ()).getCell ().get (xu.getUnitLocation ().getX ()).getTerrainData ();
			
			// If the current terrain is passable to us, then we aren't in a transport, and so the category doesn't match
			matches = false;
			if (getUnitCalculations ().isTileTypeImpassable (xu, xu.listModifiedSkillIDs (), getMemoryGridCellUtils ().convertNullTileTypeToFOW (terrainData, false), db))
			{
				// Look for a transport in the same space that can carry us
				final Iterator<MemoryUnit> iter = mem.getUnit ().iterator ();
				while ((!matches) && (iter.hasNext ()))
				{
					final MemoryUnit thisUnit = iter.next ();
					if ((thisUnit.getOwningPlayerID () == xu.getOwningPlayerID ()) && (thisUnit.getStatus () == UnitStatusID.ALIVE) &&
						(xu.getUnitLocation ().equals (thisUnit.getUnitLocation ())))
					{
						final Integer thisTransportCapacity = db.findUnit (thisUnit.getUnitID (), "unitMatchesCategory").getTransportCapacity ();
						if ((thisTransportCapacity != null) && (thisTransportCapacity > 0))
							matches = true;
					}
				}
			}
		}
		
		return matches;
	}
	
	/**
	 * @param mu Unit to check
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return AI category for this unit 
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final AiUnitCategory determineUnitCategory (final MemoryUnit mu, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (mu, null, null, null, players, mem, db);

		// Check categories from the bottom up, since the end categories are the most specific
		AiUnitCategory category = null;
		int catNo = db.getAiUnitCategory ().size () - 1;
		while ((category == null) && (catNo >= 0))
		{
			final AiUnitCategory thisCategory = db.getAiUnitCategory ().get (catNo);
			if (unitMatchesCategory (xu, thisCategory, mem, db))
				category = thisCategory;
			else
				catNo--;
		}
		
		// If the database is set up correctly then the first category (i.e. the one we hit last) will have
		// no conditions and automatically match, so this will never happen
		if (category == null)
			throw new MomException ("No unit category matched for unit URN " + mu.getUnitURN ());

		return category;
	}
	
	/**
	 * @param units Flat list of units to convert
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Units split by category and their map location, so units are grouped into stacks only of matching types
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final Map<String, List<AIUnitsAndRatings>> categoriseAndStackUnits (final List<AIUnitAndRatings> units,
		final List<PlayerServerDetails> players, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		// Process each unit in turn
		final Map<String, List<AIUnitsAndRatings>> categories = new HashMap<String, List<AIUnitsAndRatings>> ();
		for (final AIUnitAndRatings unit : units)
		{
			final AiUnitCategory category = determineUnitCategory (unit.getUnit (), players, mem, db);
			log.debug ("Movable unit URN " + unit.getUnit ().getUnitURN () + " type " + unit.getUnit ().getUnitID () + " at " + unit.getUnit ().getUnitLocation () +
				" categorized as " + category.getAiUnitCategoryID () + " - " + category.getAiUnitCategoryDescription ());
			
			// Does the category exist already?
			List<AIUnitsAndRatings> locations = categories.get (category.getAiUnitCategoryID ());
			if (locations == null)
			{
				locations = new ArrayList<AIUnitsAndRatings> ();
				categories.put (category.getAiUnitCategoryID (), locations);
			}
			
			// Does the location exist already?
			final AIUnitsAndRatings location;
			final Optional<AIUnitsAndRatings> locationWrapper = locations.stream ().filter (l -> l.get (0).getUnit ().getUnitLocation ().equals (unit.getUnit ().getUnitLocation ())).findAny ();
			if (locationWrapper.isPresent ())
				location = locationWrapper.get ();
			else
			{
				location = new AIUnitsAndRatingsImpl ();
				locations.add (location);
			}
			
			// Now can finally add it
			location.add (unit);
		}
		
		return categories;
	}
	
	/**
	 * @param playerID AI player whose turn it is
	 * @param mobileUnits List of units AI decided it can move each turn; note all non-combat units are automatically considered to be mobile
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @return List of non-combat units, broken up by what type they are and which plane they are on
	 */
	@Override
	public final Map<Integer, Map<AIUnitType, List<AIUnitAndRatings>>> determineSpecialistUnitsOnEachPlane
		(final int playerID, final List<AIUnitAndRatings> mobileUnits, final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys)
	{
		final Map<Integer, Map<AIUnitType, List<AIUnitAndRatings>>> specialistUnitsOnEachPlane = new HashMap<Integer, Map<AIUnitType, List<AIUnitAndRatings>>> ();
		for (final AIUnitAndRatings mu : mobileUnits)
			if (mu.getAiUnitType () != AIUnitType.COMBAT_UNIT)
			{
				// If the unit is standing on a tower, then we could use it on either plane
				final MemoryGridCell mc = terrain.getPlane ().get (mu.getUnit ().getUnitLocation ().getZ ()).getRow ().get (mu.getUnit ().getUnitLocation ().getY ()).getCell ().get (mu.getUnit ().getUnitLocation ().getX ());
				final boolean isTower = getMemoryGridCellUtils ().isTerrainTowerOfWizardry (mc.getTerrainData ());

				log.debug ("AI Player ID " + playerID + " has a spare " + mu.getAiUnitType () + " Unit URN " + mu.getUnit ().getUnitURN () + " at " + mu.getUnit ().getUnitLocation () +
					(isTower ? " which is a tower of wizardry" : ""));
				
				for (int z = 0; z < sys.getDepth (); z++)
					if ((isTower) || (z == mu.getUnit ().getUnitLocation ().getZ ()))
					{
						// Make sure the plane is listed
						Map<AIUnitType, List<AIUnitAndRatings>> specialistUnitsOnThisPlane = specialistUnitsOnEachPlane.get (z);
						if (specialistUnitsOnThisPlane == null)
						{
							specialistUnitsOnThisPlane = new HashMap<AIUnitType, List<AIUnitAndRatings>> ();
							specialistUnitsOnEachPlane.put (z, specialistUnitsOnThisPlane);
						}
						
						// Make sure the unit type is listed
						List<AIUnitAndRatings> specialistUnits = specialistUnitsOnThisPlane.get (mu.getAiUnitType ());
						if (specialistUnits == null)
						{
							specialistUnits = new ArrayList<AIUnitAndRatings> ();
							specialistUnitsOnThisPlane.put (mu.getAiUnitType (), specialistUnits);
						}
						
						// Now can just add it
						specialistUnits.add (mu);
					}
			}
		
		return specialistUnitsOnEachPlane;
	}

	/**
	 * @param playerID AI player whose turn it is
	 * @param fogOfWarMemory Known overland terrain, units, buildings and so on
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Map listing all locations the AI wants to send specialised units of each type
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 * @throws RecordNotFoundException If we encounter a tile type or map feature that can't be found in the cache
	 * @throws MomException If we find a consumption value that is not an exact multiple of 2, or we find a production value that is not an exact multiple of 2 that should be
	 */
	@Override
	public final Map<AIUnitType, List<MapCoordinates3DEx>> determineDesiredSpecialUnitLocations (final int playerID,
		final FogOfWarMemory fogOfWarMemory, final MomSessionVariables mom)
		throws PlayerNotFoundException, RecordNotFoundException, MomException
	{
		final List<MapCoordinates3DEx> desiredCityLocations = new ArrayList<MapCoordinates3DEx> ();
		final List<MapCoordinates3DEx> desiredRoadLocations = new ArrayList<MapCoordinates3DEx> ();
		final List<MapCoordinates3DEx> desiredNodeLocations = new ArrayList<MapCoordinates3DEx> ();
		final List<MapCoordinates3DEx> corruptedLocations = new ArrayList<MapCoordinates3DEx> ();
		
		for (int plane = 0; plane < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); plane++)
		{
			// Best place on each plane to put a new city
			final MapCoordinates3DEx desiredCityLocation = getCityAI ().chooseCityLocation (fogOfWarMemory, plane, false, mom, "considering building/moving settler");
			if (desiredCityLocation != null)
			{
				log.debug ("AI Player ID " + playerID + " can put a city at " + desiredCityLocation);
				desiredCityLocations.add (desiredCityLocation);
			}
			
			// All places on each plane that we want to put road tiles
			final List<MapCoordinates3DEx> missingRoadCells = getCityProcessing ().listMissingRoadCells (playerID, plane, null, fogOfWarMemory, mom);
			if ((missingRoadCells != null) && (missingRoadCells.size () > 0))
			{
				log.debug ("AI Player ID " + playerID + " has " + missingRoadCells.size () + " cells it wants to put road on plane " + plane);
				desiredRoadLocations.addAll (missingRoadCells);
			}
			
			// All places on each plane where we have units guarding a node, but don't own the node
			// Don't just send spirits wildly towards nodes where we have no units or they'll end up attacking enemies guarding it and just die pointlessly
			final List<MapCoordinates3DEx> nodesWeDontOwn = listNodesWeDontOwnOnPlane (playerID, plane, fogOfWarMemory,
				mom.getSessionDescription ().getOverlandMapSize (), mom.getServerDB ());
			if ((nodesWeDontOwn != null) && (nodesWeDontOwn.size () > 0))
			{
				log.debug ("AI Player ID " + playerID + " has " + nodesWeDontOwn.size () + " guarded nodes it needs to capture on plane " + plane);
				desiredNodeLocations.addAll (nodesWeDontOwn);
			}
			
			// Corrupted tiles near our cities
			final List<MapCoordinates3DEx> corruptedTiles = getCityAI ().listCorruptedTilesNearOurCities (playerID, plane, fogOfWarMemory.getMap (),
				mom.getSessionDescription ().getOverlandMapSize ());
			if ((corruptedTiles != null) && (corruptedTiles.size () > 0))
			{
				log.debug ("AI Player ID " + playerID + " has " + corruptedTiles.size () + " cells it needs to purify on plane " + plane);
				corruptedLocations.addAll (corruptedTiles);
			}
		}

		// Now put the non-empty lists into the map
		final Map<AIUnitType, List<MapCoordinates3DEx>> desiredSpecialUnitLocations = new HashMap<AIUnitType, List<MapCoordinates3DEx>> ();
		if (desiredCityLocations.size () > 0)
			desiredSpecialUnitLocations.put (AIUnitType.BUILD_CITY, desiredCityLocations);

		if (desiredRoadLocations.size () > 0)
			desiredSpecialUnitLocations.put (AIUnitType.BUILD_ROAD, desiredRoadLocations);

		if (desiredNodeLocations.size () > 0)
			desiredSpecialUnitLocations.put (AIUnitType.MELD_WITH_NODE, desiredNodeLocations);

		if (corruptedLocations.size () > 0)
			desiredSpecialUnitLocations.put (AIUnitType.PURIFY, corruptedLocations);
		
		return desiredSpecialUnitLocations;
	}
	
	/**
	 * Uses an ordered list of AI movement codes to try to decide what to do with a particular unit stack
	 * 
	 * @param units The units to move
	 * @param movementCodes List of movement codes to try
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param underdefendedLocations Locations which are either ours (cities/towers) but lack enough defence, or not ours but can be freely captured (empty lairs/cities/etc)
	 * @param ourUnitsInSameCategory List of all our mobile unit stacks in the same category as the ones we are moving
	 * @param enemyUnits Array of enemy unit ratings populated by calculateUnitRatingsAtEveryMapCell
	 * @param terrain Player knowledge of terrain
	 * @param desiredSpecialUnitLocations Locations we want to put cities, road, capture nodes, purify corruption
	 * @param isRaiders Whether it is the raiders player
	 * @param isMonsters Whether it is the rampaging monsters player
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @param players Players list
	 * @param wizards True wizard details list
	 * @return See AIMovementDecision for explanation of return values
	 * @throws RecordNotFoundException If an expected record cannot be found
	 * @throws PlayerNotFoundException If the player owning a unit stack can't be found
	 * @throws MomException If we encounter a movement code that we don't know how to process
	 */
	@Override
	public final AIMovementDecision decideUnitMovement (final AIUnitsAndRatings units, final List<AiMovementCode> movementCodes, final OverlandMovementCell [] [] [] moves,
		final List<AIDefenceLocation> underdefendedLocations, final List<AIUnitsAndRatings> ourUnitsInSameCategory, final AIUnitsAndRatings [] [] [] enemyUnits,
		final MapVolumeOfMemoryGridCells terrain, final Map<AIUnitType, List<MapCoordinates3DEx>> desiredSpecialUnitLocations,
		final boolean isRaiders, final boolean isMonsters, final CoordinateSystem sys, final CommonDatabase db, final List<PlayerServerDetails> players, final List<KnownWizardDetails> wizards)
		throws MomException, RecordNotFoundException, PlayerNotFoundException
	{
		AIMovementDecision decision = null;
		final Iterator<AiMovementCode> iter = movementCodes.iterator ();
		while ((decision == null) && (iter.hasNext ()))
		{
			final AiMovementCode movementCode = iter.next ();

			if ((!isMonsters) || (RAMPAGING_MONSTERS_MOVEMENT_CODES.contains (movementCode)))
			{
				log.debug ("AI considering movement code " + movementCode + " for stack of " + units.size () + " units at " + units.get (0).getUnit ().getUnitLocation ());
				switch (movementCode)
				{
					case REINFORCE:
						decision = getUnitAIMovement ().considerUnitMovement_Reinforce (units, moves, underdefendedLocations, sys);
						break;
						
					case ATTACK_STATIONARY:
						decision = getUnitAIMovement ().considerUnitMovement_AttackStationary (units, moves, enemyUnits, isRaiders, isMonsters, terrain, sys, db, players, wizards);
						break;
						
					case ATTACK_WANDERING:
						decision = getUnitAIMovement ().considerUnitMovement_AttackWandering (units, moves, enemyUnits, isMonsters, terrain, sys, db, players, wizards);
						break;
						
					case SCOUT_LAND:
						decision = getUnitAIMovement ().considerUnitMovement_ScoutLand (units, moves, terrain, sys, db, units.get (0).getUnit ().getOwningPlayerID ());
						break;
						
					case SCOUT_ALL:
						decision = getUnitAIMovement ().considerUnitMovement_ScoutAll (units, moves, terrain, sys, units.get (0).getUnit ().getOwningPlayerID ());
						break;
						
					case JOIN_STACK:
						decision = getUnitAIMovement ().considerUnitMovement_JoinStack (units, moves, ourUnitsInSameCategory, enemyUnits, isRaiders, terrain, sys, db);
						break;
						
					case GET_IN_TRANSPORT:
						decision = getUnitAIMovement ().considerUnitMovement_GetInTransport (units, moves, sys);
						break;
						
					case OVERDEFEND:
						decision = getUnitAIMovement ().considerUnitMovement_Overdefend (units, moves, enemyUnits, isRaiders, terrain, sys, db);
						break;
					
					case BUILD_CITY:
						decision = getUnitAIMovement ().considerUnitMovement_BuildCity (units, moves, desiredSpecialUnitLocations.get (AIUnitType.BUILD_CITY), sys);
						break;
					
					case BUILD_ROAD:
						decision = getUnitAIMovement ().considerUnitMovement_BuildRoad (units, moves, desiredSpecialUnitLocations.get (AIUnitType.BUILD_ROAD), sys);
						break;
					
					case PURIFY:
						decision = getUnitAIMovement ().considerUnitMovement_Purify (units, moves, desiredSpecialUnitLocations.get (AIUnitType.PURIFY), sys);
						break;
						
					case MELD_WITH_NODE:
						decision = getUnitAIMovement ().considerUnitMovement_MeldWithNode (units, moves, desiredSpecialUnitLocations.get (AIUnitType.MELD_WITH_NODE), sys);
						break;
					
					case CARRY_UNITS:
						decision = getUnitAIMovement ().considerUnitMovement_CarryUnits (units, moves, sys);
						break;
						
					case LOAD_UNITS:
						decision = getUnitAIMovement ().considerUnitMovement_LoadUnits (units, moves, sys);
						break;
						
					case FORTRESS_ISLAND:
						decision = getUnitAIMovement ().considerUnitMovement_FortressIsland (units, moves, sys);
						break;
						
					default:
						throw new MomException ("decideUnitMovement doesn't know what to do with AI movement code: " + movementCode);
				}
	
				log.debug ("AI movement code " + movementCode + " for stack of " + units.size () + " units at " + units.get (0).getUnit ().getUnitLocation () +
					((decision == null) ? " rejected" : (" accepted = " + decision)));
			}
		}
		
		return decision;
	}
	
	/**
	 * AI decides where to move a unit to on the overland map and actually does the move.
	 * 
	 * @param units The units to move
	 * @param category What category of units these are
	 * @param underdefendedLocations Locations we should consider a priority to aim for
	 * @param ourUnitsInSameCategory List of all our mobile unit stacks in the same category as the ones we are moving
	 * @param enemyUnits Array of enemy unit ratings populated by calculateUnitRatingsAtEveryMapCell
	 * @param desiredSpecialUnitLocations Locations we want to put cities, road, capture nodes, purify corruption
	 * @param player Player who owns the unit
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Reason indicating some action was taken or not
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final AIMovementResult decideAndExecuteUnitMovement (final AIUnitsAndRatings units, final AiUnitCategory category, final List<AIDefenceLocation> underdefendedLocations,
		final List<AIUnitsAndRatings> ourUnitsInSameCategory, final AIUnitsAndRatings [] [] [] enemyUnits,
		final Map<AIUnitType, List<MapCoordinates3DEx>> desiredSpecialUnitLocations, final PlayerServerDetails player, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		
		final KnownWizardDetails wizardDetails = getKnownWizardUtils ().findKnownWizardDetails
			(mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), player.getPlayerDescription ().getPlayerID (), "decideAndExecuteUnitMovement");
		
		// Create a stack for the units (so transports pick up any units stacked with them)
		final List<ExpandedUnitDetails> selectedUnits = new ArrayList<ExpandedUnitDetails> ();
		for (final AIUnitAndRatings mu : units)
			selectedUnits.add (getExpandUnitDetails ().expandUnitDetails (mu.getUnit (), null, null, null, mom.getPlayers (), priv.getFogOfWarMemory (), mom.getServerDB ()));
		
		final UnitStack unitStack = getUnitCalculations ().createUnitStack (selectedUnits, mom.getPlayers (), priv.getFogOfWarMemory (), mom.getServerDB ());
		
		// Work out where the unit can reach
		final MapCoordinates3DEx moveFrom = (MapCoordinates3DEx) units.get (0).getUnit ().getUnitLocation ();

		// Get the list of units who are actually moving
		final List<ExpandedUnitDetails> movingUnits = (unitStack.getTransports ().size () > 0) ? unitStack.getTransports () : unitStack.getUnits ();
		
		// What's the lowest movement remaining of any unit in the stack
		int doubleMovementRemaining = Integer.MAX_VALUE;
		for (final ExpandedUnitDetails thisUnit : movingUnits)
			if (thisUnit.getDoubleOverlandMovesLeft () < doubleMovementRemaining)
				doubleMovementRemaining = thisUnit.getDoubleOverlandMovesLeft ();

		// Its possible unit stack may have no movement left, and we don't really know that until we sorted out what's a transport and what's in a transport above
		final AIMovementResult result;
		if (doubleMovementRemaining <= 0)
			result = AIMovementResult.NO_MOVEMENT_LEFT;
		else
		{
			final OverlandMovementCell [] [] [] moves = getUnitMovement ().calculateOverlandMovementDistances (moveFrom,
				player.getPlayerDescription ().getPlayerID (), unitStack, doubleMovementRemaining,
				mom.getPlayers (), mom.getSessionDescription ().getOverlandMapSize (), priv.getFogOfWarMemory (), mom.getServerDB ());
			
			// Use list of movement codes from the unit stack's category
			final boolean isRaiders = CommonDatabaseConstants.WIZARD_ID_RAIDERS.equals (wizardDetails.getWizardID ());
			final boolean isMonsters = CommonDatabaseConstants.WIZARD_ID_MONSTERS.equals (wizardDetails.getWizardID ());
	
			final AIMovementDecision destination = decideUnitMovement (units, category.getMovementCode (), moves, underdefendedLocations,
				ourUnitsInSameCategory, enemyUnits, priv.getFogOfWarMemory ().getMap (), desiredSpecialUnitLocations, isRaiders, isMonsters,
				mom.getSessionDescription ().getOverlandMapSize (), mom.getServerDB (), mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails ());
			
			// Move, if we found somewhere to go
			if (destination == null)
				result = AIMovementResult.NO_DESTINATION_CHOSEN;
			
			else if (destination.getDestination () != null)
			{
				// If its a simultaneous turns game, then move as far as we can in one turn.
				// If its a one-player-at-a-time game, then move only 1 cell.
				// This is basically a copy of FogOfWarMidTurnChangesImpl.determineMovementDirection.
				MapCoordinates3DEx coords = destination.getDestination ();
				OverlandMovementCell cell = moves [coords.getZ ()] [coords.getY ()] [coords.getX ()];
				MapCoordinates3DEx lastCoords = null;
				
				while (((mom.getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS) && (!cell.isMoveToInOneTurn ())) ||
							((mom.getSessionDescription ().getTurnSystem () == TurnSystem.ONE_PLAYER_AT_A_TIME) && (!coords.equals (moveFrom))))
				{
					lastCoords = new MapCoordinates3DEx (coords);
					
					coords = cell.getMovedFrom ();
					cell = moves [coords.getZ ()] [coords.getY ()] [coords.getX ()];
				}
				
				if (mom.getSessionDescription ().getTurnSystem () == TurnSystem.ONE_PLAYER_AT_A_TIME)
					coords = lastCoords;
				
				log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " decided to move from " + moveFrom + " to " + coords +
					", eventually aiming for " + destination);
				
				if (coords == null)
					result = AIMovementResult.NO_ROUTE_TO_DESTINATION;
				else if (coords.equals (moveFrom))
					result = AIMovementResult.ALREADY_AT_DESTINATION;
				else
				{
					// We need the true unit versions to execute the move
					final List<ExpandedUnitDetails> trueUnits = new ArrayList<ExpandedUnitDetails> ();
					for (final AIUnitAndRatings mu : units)
					{
						final MemoryUnit tu = getUnitUtils ().findUnitURN (mu.getUnit ().getUnitURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), "decideAndExecuteUnitMovement");
						trueUnits.add (getExpandUnitDetails ().expandUnitDetails (tu, null, null, null,
							mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ()));
					}
					
					// Execute move
					result = getFogOfWarMidTurnMultiChanges ().moveUnitStack (trueUnits, player, true, moveFrom, coords,
						(mom.getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS), mom) ? AIMovementResult.MOVED_AND_STARTED_COMBAT : AIMovementResult.MOVED;
				}
			}
			
			else if (destination.getSpecialOrder () != null)
			{
				// This seems backwards, but the return value being true means the unit tries to move again; false means stop trying to move
				final List<Integer> unitURNs = units.stream ().map (u -> u.getUnit ().getUnitURN ()).collect (Collectors.toList ());
				final String error = getUnitServerUtils ().processSpecialOrder (unitURNs, destination.getSpecialOrder (), (MapCoordinates3DEx) units.get (0).getUnit ().getUnitLocation (), player, mom);
				
				if (error == null)
					result = AIMovementResult.PROCESSED_SPECIAL_ORDER;
				else
				{
					log.warn ("AI wanted to process special order " + destination.getSpecialOrder () + " but was rejected for reason: " + error);
					result = AIMovementResult.PROCESS_SPECIAL_ORDER_FAILED;
				}
			}
			
			else
				result = AIMovementResult.DESTINATION_HAS_NO_VALUE_SET;
		}
		
		return result;
	}
	
	/**
	 * @param playerID AI player who is deciding movement
	 * @param plane Plane to look on
	 * @param fogOfWarMemory Known overland terrain, units, buildings and so on
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return List of locations where there are nodes either unowned or owned by somebody else
	 * @throws RecordNotFoundException If we can't find one of the tile types
	 */
	@Override
	public final List<MapCoordinates3DEx> listNodesWeDontOwnOnPlane (final int playerID, final Integer plane, final FogOfWarMemory fogOfWarMemory, final CoordinateSystem sys,
		final CommonDatabase db) throws RecordNotFoundException
	{
		final List<MapCoordinates3DEx> list = new ArrayList<MapCoordinates3DEx> ();

		// Get a list of the locations of all of our units
		final List<MapCoordinates3DEx> unitLocations = fogOfWarMemory.getUnit ().stream ().filter
			(u -> u.getOwningPlayerID () == playerID).map (u -> (MapCoordinates3DEx) u.getUnitLocation ()).distinct ().collect (Collectors.toList ());
				
		// Find all nodes we don't own on this plane, as long as we have a unit there
		for (int z = 0; z < sys.getDepth (); z++)
			if ((plane == null) || (plane == z))
				for (int y = 0; y < sys.getHeight (); y++)
					for (int x = 0; x < sys.getWidth (); x++)
					{
						final MemoryGridCell mc = fogOfWarMemory.getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x);
						final MapCoordinates3DEx coords = new MapCoordinates3DEx (x, y, z);
						
						if ((mc != null) && (mc.getTerrainData () != null) && (mc.getTerrainData ().getTileTypeID () != null) && (unitLocations.contains (coords)))
						{
							final TileType tileTypeDef = db.findTileType (mc.getTerrainData ().getTileTypeID (), "listNodesWeDontOwnOnPlane");
							if ((tileTypeDef.getMagicRealmID () != null) &&
								((mc.getTerrainData ().getNodeOwnerID () == null) || (mc.getTerrainData ().getNodeOwnerID () != playerID)) &&
								
								// Can't meld with warped nodes, so no point sending spirits to them
								((mc.getTerrainData ().isWarped () == null) || (!mc.getTerrainData ().isWarped ())))
								
								list.add (coords);
						}
					}
		
		return list;
	}
	
	/**
	 * @param player AI player whose hero items we want to reallocate
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 */
	@Override
	public final void reallocateHeroItems (final PlayerServerDetails player, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		// Get a list of all hero items we own
		final List<NumberedHeroItem> itemsList = new ArrayList<NumberedHeroItem> ();
		
		mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ().stream ().filter
			(u -> (u.getStatus () == UnitStatusID.ALIVE) && (u.getOwningPlayerID () == player.getPlayerDescription ().getPlayerID ())).forEach
			(u -> u.getHeroItemSlot ().stream ().filter (s -> s.getHeroItem () != null).forEach (s ->
		{
			itemsList.add (s.getHeroItem ());
			s.setHeroItem (null);		// Remove item from hero slot, so it does't skew the ratings
		}));
		
		itemsList.addAll (priv.getUnassignedHeroItem ());
		
		if (itemsList.size () > 0)
		{
			// Now build a map keyed by the item type (so we have a list of swords, a list of armours, a list of wands, and so on)
			// Rate them all while we're at it
			final Map<String, List<NumberedHeroItemAndRating>> itemsMap = new HashMap<String, List<NumberedHeroItemAndRating>> ();
			for (final NumberedHeroItem item : itemsList)
			{
				List<NumberedHeroItemAndRating> itemTypeList = itemsMap.get (item.getHeroItemTypeID ());
				if (itemTypeList == null)
				{
					itemTypeList = new ArrayList<NumberedHeroItemAndRating> ();
					itemsMap.put (item.getHeroItemTypeID (), itemTypeList);
				}
				itemTypeList.add (new NumberedHeroItemAndRating (item, getAiHeroItemRatingCalculations ().calculateHeroItemRating (item, mom.getServerDB ())));
			}
			
			// Now list all heroes
			List<ExpandedUnitDetailsAndRating> heroes = new ArrayList<ExpandedUnitDetailsAndRating> ();
			for (final MemoryUnit tu : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
				if ((tu.getStatus () == UnitStatusID.ALIVE) && (tu.getOwningPlayerID () == player.getPlayerDescription ().getPlayerID ()) && (tu.getHeroItemSlot ().size () > 0))
				{
					final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (tu, null, null, null,
						mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
					if (xu.isHero ())
						heroes.add (new ExpandedUnitDetailsAndRating (xu, getAiUnitRatingCalculations ().calculateUnitCurrentRating (tu, xu,
							mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ())));
				}
			
			if (heroes.size () > 0)
			{
				// The best heroes get first pick of the best items
				if (heroes.size () > 1)
					heroes.sort (null);

				// Which item types give a bonus to range attacks?  (bow, wand, staff, misc)
				final List<String> rangedItemTypeIDs = mom.getServerDB ().getHeroItemType ().stream ().filter
					(it -> it.getHeroItemTypeAttackType ().contains (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK)).map
					(it -> it.getHeroItemTypeID ()).collect (Collectors.toList ());
				
				// So which item slot types can hold item types that give a bonus to range attacks? (bow/melee, mag/melee, mag, misc)
				final List<String> rangedItemSlotTypeIDs = mom.getServerDB ().getHeroItemSlotType ().stream ().filter
					(ist -> ist.getHeroSlotAllowedItemType ().stream ().anyMatch (it -> rangedItemTypeIDs.contains (it))).map
					(ist -> ist.getHeroItemSlotTypeID ()).collect (Collectors.toList ());
				
				// Find items for each hero in turn
				for (final ExpandedUnitDetailsAndRating hero : heroes)
				{
					final ExpandedUnitDetails xu = hero.getUnit ();
					final MemoryUnit mu = xu.getMemoryUnit ();
					
					for (int slotNumber = 0; slotNumber < mu.getHeroItemSlot ().size (); slotNumber++)
					{
						final HeroItemSlotType slotType = mom.getServerDB ().findHeroItemSlotType (xu.getUnitDefinition ().getHeroItemSlot ().get (slotNumber), "reallocateHeroItems");
						
						// Get a list of all the items we could put in this slot
						final List<NumberedHeroItemAndRating> potentialItems = new ArrayList<NumberedHeroItemAndRating> ();
						for (final String itemTypeID : slotType.getHeroSlotAllowedItemType ())
							
							// If its a ranged slot, then only consider ranged items
							if ((!rangedItemSlotTypeIDs.contains (slotType.getHeroItemSlotTypeID ())) || (rangedItemTypeIDs.contains (itemTypeID)))
							{
								final List<NumberedHeroItemAndRating> itemsOfThisType = itemsMap.get (itemTypeID);
								if (itemsOfThisType != null)
									potentialItems.addAll (itemsOfThisType);
							}
						
						// Pick the best item
						if (potentialItems.size () > 0)
						{
							if (potentialItems.size () > 1)
								potentialItems.sort (null);
							
							final NumberedHeroItemAndRating chosenItem = potentialItems.get (0);
							
							// Remove it from further items we can allocate
							itemsMap.get (chosenItem.getItem ().getHeroItemTypeID ()).remove (chosenItem);
							itemsList.remove (chosenItem.getItem ());
							
							// Add it into the slot
							final MemoryUnitHeroItemSlot slot = mu.getHeroItemSlot ().get (slotNumber);
							slot.setHeroItem (chosenItem.getItem ());
						}
					}
					
					// Let everyone see which items this hero now has
					getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (mu, mom, null);
				}
			}
			
			// Any items leftover unused go back in the bank
			priv.getUnassignedHeroItem ().clear ();
			priv.getUnassignedHeroItem ().addAll (itemsList);
		}
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
	 * @return Memory building utils
	 */
	public final MemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param utils Memory building utils
	 */
	public final void setMemoryBuildingUtils (final MemoryBuildingUtils utils)
	{
		memoryBuildingUtils = utils;
	}

	/**
	 * @return Spell utils
	 */
	public final SpellUtils getSpellUtils ()
	{
		return spellUtils;
	}

	/**
	 * @param utils Spell utils
	 */
	public final void setSpellUtils (final SpellUtils utils)
	{
		spellUtils = utils;
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
	public final FogOfWarMidTurnMultiChanges getFogOfWarMidTurnMultiChanges ()
	{
		return fogOfWarMidTurnMultiChanges;
	}

	/**
	 * @param obj Methods for updating true map + players' memory
	 */
	public final void setFogOfWarMidTurnMultiChanges (final FogOfWarMidTurnMultiChanges obj)
	{
		fogOfWarMidTurnMultiChanges = obj;
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
	 * @return Unit AI movement decisions
	 */
	public final UnitAIMovement getUnitAIMovement ()
	{
		return unitAIMovement;
	}

	/**
	 * @param ai Unit AI movement decisions
	 */
	public final void setUnitAIMovement (final UnitAIMovement ai)
	{
		unitAIMovement = ai;
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
	 * @return Underlying methods that the AI uses to calculate ratings about how good units are
	 */
	public final AIUnitRatingCalculations getAiUnitRatingCalculations ()
	{
		return aiUnitRatingCalculations;
	}

	/**
	 * @param calc Underlying methods that the AI uses to calculate ratings about how good units are
	 */
	public final void setAiUnitRatingCalculations (final AIUnitRatingCalculations calc)
	{
		aiUnitRatingCalculations = calc;
	}
	
	/**
	 * @return Methods that the AI uses to calculate stats about types of units and rating how good units are
	 */
	public final AIUnitCalculations getAiUnitCalculations ()
	{
		return aiUnitCalculations;
	}

	/**
	 * @param calc Methods that the AI uses to calculate stats about types of units and rating how good units are
	 */
	public final void setAiUnitCalculations (final AIUnitCalculations calc)
	{
		aiUnitCalculations = calc;
	}

	/**
	 * @return AI decisions about cities
	 */
	public final CityAI getCityAI ()
	{
		return cityAI;
	}

	/**
	 * @param ai AI decisions about cities
	 */
	public final void setCityAI (final CityAI ai)
	{
		cityAI = ai;
	}
	
	/**
	 * @return City processing methods
	 */
	public final CityProcessing getCityProcessing ()
	{
		return cityProcessing;
	}

	/**
	 * @param obj City processing methods
	 */
	public final void setCityProcessing (final CityProcessing obj)
	{
		cityProcessing = obj;
	}

	/**
	 * @return Server-only unit calculations
	 */
	public final ServerUnitCalculations getServerUnitCalculations ()
	{
		return serverUnitCalculations;
	}

	/**
	 * @param calc Server-only unit calculations
	 */
	public final void setServerUnitCalculations (final ServerUnitCalculations calc)
	{
		serverUnitCalculations = calc;
	}

	/**
	 * @return Methods that the AI uses to calculate ratings about how good hero items are
	 */
	public final AIHeroItemRatingCalculations getAiHeroItemRatingCalculations ()
	{
		return aiHeroItemRatingCalculations;
	}

	/**
	 * @param calc Methods that the AI uses to calculate ratings about how good hero items are
	 */
	public final void setAiHeroItemRatingCalculations (final AIHeroItemRatingCalculations calc)
	{
		aiHeroItemRatingCalculations = calc;
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
	 * @return Sample unit method
	 */
	public final SampleUnitUtils getSampleUnitUtils ()
	{
		return sampleUnitUtils;
	}

	/**
	 * @param s Sample unit method
	 */
	public final void setSampleUnitUtils (final SampleUnitUtils s)
	{
		sampleUnitUtils = s;
	}

	/**
	 * @return Methods for working with wizardIDs
	 */
	public final PlayerKnowledgeUtils getPlayerKnowledgeUtils ()
	{
		return playerKnowledgeUtils;
	}

	/**
	 * @param k Methods for working with wizardIDs
	 */
	public final void setPlayerKnowledgeUtils (final PlayerKnowledgeUtils k)
	{
		playerKnowledgeUtils = k;
	}

	/**
	 * @return Methods for finding KnownWizardDetails from the list
	 */
	public final KnownWizardUtils getKnownWizardUtils ()
	{
		return knownWizardUtils;
	}

	/**
	 * @param k Methods for finding KnownWizardDetails from the list
	 */
	public final void setKnownWizardUtils (final KnownWizardUtils k)
	{
		knownWizardUtils = k;
	}
}