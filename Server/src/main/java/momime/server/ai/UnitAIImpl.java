package momime.server.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.calculations.CityCalculations;
import momime.common.calculations.UnitCalculations;
import momime.common.calculations.UnitStack;
import momime.common.database.AiMovementCode;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItemTypeAllowedBonus;
import momime.common.database.RecordNotFoundException;
import momime.common.database.SpellBookSectionID;
import momime.common.database.SpellSetting;
import momime.common.database.Unit;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MemoryUnitHeroItemSlot;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.SpellResearchStatusID;
import momime.common.messages.TurnSystem;
import momime.common.messages.UnitDamage;
import momime.common.messages.UnitStatusID;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.SpellUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerUnitCalculations;
import momime.server.database.AiUnitCategorySvr;
import momime.server.database.HeroItemBonusSvr;
import momime.server.database.HeroItemSlotTypeSvr;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.SpellSvr;
import momime.server.database.UnitSkillSvr;
import momime.server.database.UnitSvr;
import momime.server.fogofwar.FogOfWarMidTurnMultiChanges;
import momime.server.messages.ServerMemoryGridCellUtils;
import momime.server.utils.UnitServerUtils;
import momime.server.utils.UnitSkillDirectAccess;

/**
 * Methods for AI players evaluating the strength of units
 */
public final class UnitAIImpl implements UnitAI
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (UnitAIImpl.class);

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Unit skill values direct access */
	private UnitSkillDirectAccess unitSkillDirectAccess;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** City calculations */
	private CityCalculations cityCalculations;
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** Spell utils */
	private SpellUtils spellUtils;
	
	/** Server-only unit calculations */
	private ServerUnitCalculations serverUnitCalculations;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
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
	
	/**
	 * @param xu Unit to calculate value for
	 * @param db Lookup lists built over the XML database
	 * @return Value AI estimates for the quality, usefulness and effectiveness of this unit
	 * @throws MomException If we hit any problems reading unit skill values
	 * @throws RecordNotFoundException If the unit has a skill that we can't find in the database
	 */
	final int calculateUnitRating (final ExpandedUnitDetails xu, final ServerDatabaseEx db)
		throws MomException, RecordNotFoundException
	{		
		log.trace ("Entering calculateUnitRating: " + xu.getDebugIdentifier ());

		// Units with no attacks whatsoever (settlers) aren't even considered to be combat units
		int total = 0;
		if ((xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK)) ||
			(xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK)))
		{
			// Add 10% for each figure over 1 that the unit has
			double multipliers = ((double) xu.calculateHitPointsRemaining ()) / ((double) xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS));
			multipliers = ((multipliers - 1d) / 10d) + 1d;
	
			// Go through all skills totalling up additive and multiplicative bonuses from skills
			for (final String unitSkillID : xu.listModifiedSkillIDs ())
			{
				Integer value = xu.getModifiedSkillValue (unitSkillID);
				if ((value == null) || (value == 0))
					value = 1;
				
				final UnitSkillSvr skillDef = db.findUnitSkill (unitSkillID, "calculateUnitRating");
				if (skillDef.getAiRatingMultiplicative () != null)
					multipliers = multipliers * skillDef.getAiRatingMultiplicative ();
				
				else if (skillDef.getAiRatingAdditive () != null)
				{
					if ((skillDef.getAiRatingDiminishesAfter () == null) || (value <= skillDef.getAiRatingDiminishesAfter ()))
						total = total + (value * skillDef.getAiRatingAdditive ());
					else
					{
						// Diminishing skill - add on the fixed part
						total = total + (skillDef.getAiRatingDiminishesAfter () * skillDef.getAiRatingAdditive ());
						int leftToAdd = Math.min (value - skillDef.getAiRatingDiminishesAfter (), skillDef.getAiRatingAdditive () - 1);
						for (int n = 1; n <= leftToAdd; n++)
							total = total + (skillDef.getAiRatingAdditive () - n);
					}
				}
			}
			
			// Apply multiplicative modifiers
			total = (int) (total * multipliers);
		}
		
		log.trace ("Exiting calculateUnitRating = " + total);
		return total;
	}
	
	/**
	 * @param unit Unit to calculate value for
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Value AI estimates for the current quality, usefulness and effectiveness of this unit
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final int calculateUnitCurrentRating (final AvailableUnit unit, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering calculateUnitCurrentRating: " + unit.getUnitID () + " owned by player ID " + unit.getOwningPlayerID ());
		
		final int rating = calculateUnitRating (getUnitUtils ().expandUnitDetails (unit, null, null, null, players, mem, db), db);
		
		log.trace ("Exiting calculateUnitCurrentRating = " + rating);
		return rating;
	}
	
	/**
	 * @param bonus Hero item bonus to evaluate
	 * @return Value AI estimates for how good of a hero item bonus this is 
	 */
	final int calculateHeroItemBonusRating (final HeroItemBonusSvr bonus)
	{
		log.trace ("Entering calculateHeroItemBonusRating: " + bonus.getHeroItemBonusID ());

		final int rating;
		
		if (bonus.getHeroItemBonusStat ().isEmpty ())
			rating = 2;
		else
			rating = bonus.getHeroItemBonusStat ().stream ().mapToInt (s -> (s.getUnitSkillValue () == null) ? 2 : s.getUnitSkillValue ()).sum ();
		
		log.trace ("Exiting calculateHeroItemBonusRating = " + rating);
		return rating;
	}

	/**
	 * @param item Hero item to evaluate
	 * @param db Lookup lists built over the XML database
	 * @return Value AI estimates for how good of a hero item this is
	 * @throws RecordNotFoundException If the item has a bonus property that we can't find in the database 
	 */
	final int calculateHeroItemRating (final NumberedHeroItem item, final ServerDatabaseEx db) throws RecordNotFoundException
	{
		log.trace ("Entering calculateHeroItemRating: Item URN " + item.getHeroItemURN () + ", name " + item.getHeroItemName ());

		int rating = 0;
		for (final HeroItemTypeAllowedBonus bonus : item.getHeroItemChosenBonus ())
			rating = rating + calculateHeroItemBonusRating (db.findHeroItemBonus (bonus.getHeroItemBonusID (), "calculateUnitPotentialRating"));
		
		log.trace ("Exiting calculateHeroItemRating = " + rating);
		return rating;
	}

	/**
	 * @param unit Unit to calculate value for
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Value AI estimates for the quality, usefulness and effectiveness that this unit has the potential to become
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final int calculateUnitPotentialRating (final AvailableUnit unit, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering calculateUnitPotentialRating: " + unit.getUnitID () + " owned by player ID " + unit.getOwningPlayerID ());
		
		// Since MoM is single threaded (with respect to each session) we can temporarily fiddle with the existing unit details, then put it back the way it was afterwards
		final int experience = getUnitSkillDirectAccess ().getDirectSkillValue (unit.getUnitHasSkill (), CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE);
		if (experience >= 0)
			getUnitSkillDirectAccess ().setDirectSkillValue (unit, CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE, Integer.MAX_VALUE);
		
		final List<UnitDamage> unitDamage;
		final List<NumberedHeroItem> heroItems;
		final MemoryUnit mu;
		if (unit instanceof MemoryUnit)
		{
			mu = (MemoryUnit) unit;
			final UnitSvr unitDef = db.findUnit (unit.getUnitID (), "calculateUnitPotentialRating");
			
			unitDamage = new ArrayList<UnitDamage> ();
			unitDamage.addAll (mu.getUnitDamage ());
			mu.getUnitDamage ().clear ();
			
			heroItems = new ArrayList<NumberedHeroItem> ();
			for (int slotNumber = 0; slotNumber < mu.getHeroItemSlot ().size (); slotNumber++)
			{
				final MemoryUnitHeroItemSlot slot = mu.getHeroItemSlot ().get (slotNumber);
				heroItems.add (slot.getHeroItem ());
				
				// Is the item in this slot good enough already?
				if ((slotNumber < unitDef.getHeroItemSlot ().size ()) && ((slot.getHeroItem () == null) || (calculateHeroItemRating (slot.getHeroItem (), db) < 6)))
				{
					final HeroItemSlotTypeSvr slotType = db.findHeroItemSlotType (unitDef.getHeroItemSlot ().get (slotNumber).getHeroItemSlotTypeID (), "calculateUnitPotentialRating");
					
					if (slotType.getBasicHeroItemForAiRatingItemTypeID () != null)
					{
						// It needs to be a numberedHeroItem, so take a copy of the important details
						final NumberedHeroItem numberedItem = new NumberedHeroItem ();
						numberedItem.setHeroItemTypeID (slotType.getBasicHeroItemForAiRatingItemTypeID ());
						numberedItem.getHeroItemChosenBonus ().addAll (slotType.getBasicHeroItemForAiRatingChosenBonus ());
						
						slot.setHeroItem (numberedItem);
					}
				}
			}
		}
		else
		{
			mu = null;
			unitDamage = null;
			heroItems = null;
		}

		// Now calculate its rating
		final int rating = calculateUnitRating (getUnitUtils ().expandUnitDetails (unit, null, null, null, players, mem, db), db);
		
		// Now put everything back the way it was
		if (experience >= 0)
			getUnitSkillDirectAccess ().setDirectSkillValue (unit, CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE, experience);
		
		if (mu != null)
		{
			mu.getUnitDamage ().addAll (unitDamage);

			for (int slotNumber = 0; slotNumber < mu.getHeroItemSlot ().size (); slotNumber++)
				mu.getHeroItemSlot ().get (slotNumber).setHeroItem (heroItems.get (slotNumber));
		}
		
		log.trace ("Exiting calculateUnitPotentialRating = " + rating);
		return rating;
	}

	/**
	 * @param unit Unit to calculate value for
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Value AI estimates for the quality, usefulness and effectiveness for defensive purposes
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final int calculateUnitAverageRating (final AvailableUnit unit, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering calculateUnitAverageRating: " + unit.getUnitID () + " owned by player ID " + unit.getOwningPlayerID ());
		
		final int rating = (calculateUnitCurrentRating (unit, players, mem, db) + calculateUnitPotentialRating (unit, players, mem, db)) / 2;

		log.trace ("Exiting calculateUnitAverageRating = " + rating);
		return rating;
	}
	
	/**
	 * @param player AI player who is considering constructing the specified unit
	 * @param players Players list
	 * @param unit Unit they want to construct
	 * @param spellSettings Spell combination settings, either from the server XML cache or the Session description
	 * @param db Lookup lists built over the XML database
	 * @return Whether or not we can afford the additional maintenance cost of this unit - will ignore rations since we can always allocate more farmers
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	final boolean canAffordUnitMaintenance (final PlayerServerDetails player, final List<PlayerServerDetails> players, final AvailableUnit unit,
		final SpellSetting spellSettings, final ServerDatabaseEx db) throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering canAffordUnitMaintenance: " + unit.getUnitID () + " owned by player ID " + player.getPlayerDescription ().getPlayerID ());

		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		
		final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (unit, null, null, null, players, priv.getFogOfWarMemory (), db);
		
		// Now we can check its upkeep
		boolean ok = true;
		final Iterator<String> iter = xu.listModifiedUpkeepProductionTypeIDs ().iterator ();
		while ((ok) && (iter.hasNext ()))
		{
			final String productionTypeID = iter.next ();
			if ((!productionTypeID.equals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS)) &&
				(xu.getModifiedUpkeepValue (productionTypeID) > getResourceValueUtils ().calculateAmountPerTurnForProductionType (priv, pub.getPick (), productionTypeID, spellSettings, db)))
				
				ok = false;
		}
		
		log.trace ("Exiting canAffordUnitMaintenance = " + ok);
		return ok;
	}
	
	/**
	 * Lists every unit this AI player can build at every city they own, as well as any units they can summon, sorted with the best units first.
	 * This won't list heroes, since if we cast Summon Hero/Champion, we never know which one we're going to get.
	 * Will not include any units that we wouldn't be able to afford the maintenance cost of if we constructed them
	 * (mana for summoned units; gold for units constructed in cities - will ignore rations since we can always allocate more farmers).
	 * 
	 * @param player AI player who is considering constructing a unit
	 * @param players Players list
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @return List of all possible units this AI player can construct or summon, sorted with the best first
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final List<AIConstructableUnit> listAllUnitsWeCanConstruct (final PlayerServerDetails player, final List<PlayerServerDetails> players,
		final MomSessionDescription sd, final ServerDatabaseEx db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering listAllUnitsWeCanConstruct: Player ID " + player.getPlayerDescription ().getPlayerID ());

		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();

		final List<AIConstructableUnit> results = new ArrayList<AIConstructableUnit> ();
		
		// Units we can construct in cities
		for (int z = 0; z < sd.getOverlandMapSize ().getDepth (); z++)
			for (int y = 0; y < sd.getOverlandMapSize ().getHeight (); y++)
				for (int x = 0; x < sd.getOverlandMapSize ().getWidth (); x++)
				{
					final OverlandMapCityData cityData = priv.getFogOfWarMemory ().getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x).getCityData ();
					if ((cityData != null) && (cityData.getCityOwnerID () == player.getPlayerDescription ().getPlayerID ()))
					{
						final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (x, y, z);
						
						final List<Unit> unitDefs = getCityCalculations ().listUnitsCityCanConstruct (cityLocation,
							priv.getFogOfWarMemory ().getMap (), priv.getFogOfWarMemory ().getBuilding (), db);
						
						for (final Unit unitDef : unitDefs)
						{
							// Need real example of the unit so that we property take into account if we have
							// e.g. retorts that make it cheaper to maintained summoned creatures, or so on 
							final AvailableUnit unit = new AvailableUnit ();
							unit.setOwningPlayerID (player.getPlayerDescription ().getPlayerID ());
							unit.setUnitID (unitDef.getUnitID ());
							unit.setUnitLocation (cityLocation);

							// Need to get experience and weapon grade right so we tend to construct units in cities with e.g. a Fighters' or Alchemists' Guild
							final int startingExperience = getMemoryBuildingUtils ().experienceFromBuildings (priv.getFogOfWarMemory ().getBuilding (), cityLocation, db);
							
							unit.setWeaponGrade (getUnitCalculations ().calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort
								(priv.getFogOfWarMemory ().getBuilding (), priv.getFogOfWarMemory ().getMap (), cityLocation, pub.getPick (), sd.getOverlandMapSize (), db));
							
							getUnitUtils ().initializeUnitSkills (unit, startingExperience, db);
							
							results.add (new AIConstructableUnit ((UnitSvr) unitDef, cityLocation, null,
								calculateUnitAverageRating (unit, players, priv.getFogOfWarMemory (), db),
								canAffordUnitMaintenance (player, players, unit, sd.getSpellSetting (), db)));
						}									
					}
				}
		
		// Summonining spells we know
		for (final SpellSvr spell : db.getSpells ())
			if ((spell.getSpellBookSectionID () == SpellBookSectionID.SUMMONING) && (spell.getSummonedUnit ().size () == 1) && (spell.getOverlandCastingCost () != null) &&
				(getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), spell.getSpellID ()).getStatus () == SpellResearchStatusID.AVAILABLE))
			{
				final AvailableUnit unit = new AvailableUnit ();
				unit.setOwningPlayerID (player.getPlayerDescription ().getPlayerID ());
				unit.setUnitID (spell.getSummonedUnit ().get (0).getSummonedUnitID ());
				
				final UnitSvr unitDef = (UnitSvr) getUnitUtils ().initializeUnitSkills (unit, null, db);

				results.add (new AIConstructableUnit (unitDef, null, spell,
					calculateUnitAverageRating (unit, players, priv.getFogOfWarMemory (), db),
					canAffordUnitMaintenance (player, players, unit, sd.getSpellSetting (), db)));
			}
		
		// Sort the results
		Collections.sort (results);
		
		log.trace ("Exiting listAllUnitsWeCanConstruct = " + results.size ());
		return results;
	}

	/**
	 * Calculates the current and average rating of every unit we can see on the map.
	 * 
	 * @param ourUnits Array to populate our unit ratings into
	 * @param enemyUnits Array to populate enemy unit ratings into
	 * @param playerID Player ID to consider as "our" units
	 * @param mem Memory data known to playerID
	 * @param players Players list
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final void calculateUnitRatingsAtEveryMapCell (final AIUnitsAndRatings [] [] [] ourUnits, final AIUnitsAndRatings [] [] [] enemyUnits,
		final int playerID, final FogOfWarMemory mem, final List<PlayerServerDetails> players, final ServerDatabaseEx db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering calculateUnitRatingsAtEveryMapCell: AI Player ID " + playerID);
		
		for (final MemoryUnit mu : mem.getUnit ())
			if (mu.getStatus () == UnitStatusID.ALIVE)
			{
				final AIUnitsAndRatings [] [] [] unitArray = (mu.getOwningPlayerID () == playerID) ? ourUnits : enemyUnits;
				AIUnitsAndRatings unitList = unitArray [mu.getUnitLocation ().getZ ()] [mu.getUnitLocation ().getY ()] [mu.getUnitLocation ().getX ()];
				if (unitList == null)
				{
					unitList = new AIUnitsAndRatingsImpl ();
					unitArray [mu.getUnitLocation ().getZ ()] [mu.getUnitLocation ().getY ()] [mu.getUnitLocation ().getX ()] = unitList; 
				}
				
				unitList.add (new AIUnitAndRatings (mu,
					calculateUnitCurrentRating (mu, players, mem, db),
					calculateUnitAverageRating (mu, players, mem, db)));
			}

		log.trace ("Exiting calculateUnitRatingsAtEveryMapCell");
	}
	
	/**
	 * Checks every city, node, lair and tower that we either own or is undefended, and checks how much short of our desired defence level it currently is.
	 * As a side effect, any units where we have too much defence, or units which are not in a defensive location, are put into a list of mobile units.
	 * 
	 * @param ourUnits Array of our unit ratings populated by calculateUnitRatingsAtEveryMapCell
	 * @param enemyUnits Array of enemy unit ratings populated by calculateUnitRatingsAtEveryMapCell
	 * @param mobileUnits List to populate with details of all units that are in excess of defensive requirements, or are not in defensive positions
	 * @param playerID Player ID to consider as "our" units
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
		final List<AIUnitAndRatings> mobileUnits, final int playerID, final FogOfWarMemory mem, final int highestAverageRating, final int turnNumber,
		final CoordinateSystem sys, final ServerDatabaseEx db) throws RecordNotFoundException
	{
		log.trace ("Entering evaluateCurrentDefence: AI Player ID " + playerID);
		
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
						if (((cityData != null) || (ServerMemoryGridCellUtils.isNodeLairTower (terrainData, db))) && (theirs == null)) 
						{
							final MapCoordinates3DEx coords = new MapCoordinates3DEx (x, y, z);
							
							final int defenceRating = (ours == null) ? 0 : ours.totalAverageRatings ();
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
										if (thisUnit.getAverageRating () == 0)
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
									if ((defenceSoFar >= desiredDefenceRating) || (thisUnit.getAverageRating () == 0))
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

		log.trace ("Exiting evaluateCurrentDefence");
		return underdefendedLocations;
	}
	
	/**
	 * @param xu Unit to test
	 * @param category Category to test whether the unit matches or not
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Whether the unit matches the specified category or not
	 * @throws RecordNotFoundException If we encounter a transport that we can't find the unit definition for
	 */
	final boolean unitMatchesCategory (final ExpandedUnitDetails xu, final AiUnitCategorySvr category, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException
	{
		log.trace ("Entering unitMatchesCategory: " + xu.getDebugIdentifier () + ", category " + category.getAiUnitCategoryID ());

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
			if (getUnitCalculations ().calculateDoubleMovementToEnterTileType (xu, xu.listModifiedSkillIDs (), getMemoryGridCellUtils ().convertNullTileTypeToFOW (terrainData), db) == null)
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
		
		log.trace ("Exiting unitMatchesCategory = " + matches);
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
	final AiUnitCategorySvr determineUnitCategory (final MemoryUnit mu, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering determineUnitCategory: Unit URN " + mu.getUnitURN ());
		
		final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (mu, null, null, null, players, mem, db);

		// Check categories from the bottom up, since the end categories are the most specific
		AiUnitCategorySvr category = null;
		int catNo = db.getAiUnitCategories ().size () - 1;
		while ((category == null) && (catNo >= 0))
		{
			final AiUnitCategorySvr thisCategory = db.getAiUnitCategories ().get (catNo);
			if (unitMatchesCategory (xu, thisCategory, mem, db))
				category = thisCategory;
			else
				catNo--;
		}
		
		// If the database is set up correctly then the first category (i.e. the one we hit last) will have
		// no conditions and automatically match, so this will never happen
		if (category == null)
			throw new MomException ("No unit category matched for unit URN " + mu.getUnitURN ());

		log.trace ("Exiting determineUnitCategory = " + category.getAiUnitCategoryID ());
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
		final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering categoriseAndStackUnits");		

		// Process each unit in turn
		final Map<String, List<AIUnitsAndRatings>> categories = new HashMap<String, List<AIUnitsAndRatings>> ();
		for (final AIUnitAndRatings unit : units)
		{
			final AiUnitCategorySvr category = determineUnitCategory (unit.getUnit (), players, mem, db);
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
		
		log.trace ("Exiting categoriseAndStackUnits = " + categories.size ());
		return categories;
	}
	
	/**
	 * Uses an ordered list of AI movement codes to try to decide what to do with a particular unit stack
	 * 
	 * @param units The units to move
	 * @param movementCodes List of movement codes to try
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @param underdefendedLocations Locations which are either ours (cities/towers) but lack enough defence, or not ours but can be freely captured (empty lairs/cities/etc)
	 * @param enemyUnits Array of enemy unit ratings populated by calculateUnitRatingsAtEveryMapCell
	 * @param terrain Player knowledge of terrain
	 * @param desiredCityLocation Location where we want to put a city
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return See AIMovementDecision for explanation of return values
	 * @throws RecordNotFoundException If an expected record cannot be found
	 * @throws MomException If we encounter a movement code that we don't know how to process
	 */
	@Override
	public final AIMovementDecision decideUnitMovement (final AIUnitsAndRatings units, final List<AiMovementCode> movementCodes, final int [] [] [] doubleMovementDistances,
		final List<AIDefenceLocation> underdefendedLocations, final AIUnitsAndRatings [] [] [] enemyUnits, final MapVolumeOfMemoryGridCells terrain, final MapCoordinates3DEx desiredCityLocation,
		final CoordinateSystem sys, final ServerDatabaseEx db) throws MomException, RecordNotFoundException
	{
		log.trace ("Entering decideUnitMovement");
		
		AIMovementDecision decision = null;
		final Iterator<AiMovementCode> iter = movementCodes.iterator ();
		while ((decision == null) && (iter.hasNext ()))
		{
			final AiMovementCode movementCode = iter.next ();
			
			log.debug ("AI considering movement code " + movementCode + " for stack of " + units.size () + " units at " + units.get (0).getUnit ().getUnitLocation ());
			
			switch (movementCode)
			{
				case REINFORCE:
					decision = getUnitAIMovement ().considerUnitMovement_Reinforce (doubleMovementDistances, underdefendedLocations);
					break;
					
				case ATTACK_STATIONARY:
					decision = getUnitAIMovement ().considerUnitMovement_AttackStationary (units, doubleMovementDistances, enemyUnits, terrain, sys, db);
					break;
					
				case ATTACK_WANDERING:
					decision = getUnitAIMovement ().considerUnitMovement_AttackWandering (units, doubleMovementDistances, enemyUnits, terrain, sys, db);
					break;
					
				case SCOUT_LAND:
					decision = getUnitAIMovement ().considerUnitMovement_ScoutLand (doubleMovementDistances, terrain, sys, db, units.get (0).getUnit ().getOwningPlayerID ());
					break;
					
				case SCOUT_ALL:
					decision = getUnitAIMovement ().considerUnitMovement_ScoutAll (doubleMovementDistances, terrain, sys, units.get (0).getUnit ().getOwningPlayerID ());
					break;
					
				case JOIN_STACK:
					decision = getUnitAIMovement ().considerUnitMovement_JoinStack (doubleMovementDistances);
					break;
					
				case PLANE_SHIFT:
					decision = getUnitAIMovement ().considerUnitMovement_PlaneShift (doubleMovementDistances);
					break;
					
				case GET_IN_TRANSPORT:
					decision = getUnitAIMovement ().considerUnitMovement_GetInTransport (doubleMovementDistances);
					break;
					
				case OVERDEFEND:
					decision = getUnitAIMovement ().considerUnitMovement_Overdefend (doubleMovementDistances);
					break;
				
				case BUILD_CITY:
					decision = getUnitAIMovement ().considerUnitMovement_BuildCity (doubleMovementDistances, (MapCoordinates3DEx) units.get (0).getUnit ().getUnitLocation (), desiredCityLocation);
					break;
				
				case BUILD_ROAD:
					decision = getUnitAIMovement ().considerUnitMovement_BuildRoad (doubleMovementDistances);
					break;
				
				case PURIFY:
					decision = getUnitAIMovement ().considerUnitMovement_Purify (doubleMovementDistances);
					break;
					
				case MELD_WITH_NODE:
					decision = getUnitAIMovement ().considerUnitMovement_MeldWithNode (doubleMovementDistances);
					break;
				
				case CARRY_UNITS:
					decision = getUnitAIMovement ().considerUnitMovement_CarryUnits (doubleMovementDistances);
					break;
					
				case LOAD_UNITS:
					decision = getUnitAIMovement ().considerUnitMovement_LoadUnits (doubleMovementDistances);
					break;
					
				case FORTRESS_ISLAND:
					decision = getUnitAIMovement ().considerUnitMovement_FortressIsland (doubleMovementDistances);
					break;
					
				default:
					throw new MomException ("decideUnitMovement doesn't know what to do with AI movement code: " + movementCode);
			}

			log.debug ("AI movement code " + movementCode + " for stack of " + units.size () + " units at " + units.get (0).getUnit ().getUnitLocation () +
				((decision == null) ? " rejected" : (" accepted = " + decision)));
		}
		
		log.trace ("Exiting decideUnitMovement = " + decision);
		return decision;
	}
	
	/**
	 * AI decides where to move a unit to on the overland map.
	 * 
	 * @param units The units to move
	 * @param category What category of units these are
	 * @param underdefendedLocations Locations we should consider a priority to aim for
	 * @param enemyUnits Array of enemy unit ratings populated by calculateUnitRatingsAtEveryMapCell
	 * @param desiredCityLocation Location where we want to put a city
	 * @param player Player who owns the unit
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether we moved or not; if we did something else (like turn a settler into a city, or make an engineer build a road) then returns false
	 * @throws RecordNotFoundException If an expected record cannot be found
	 * @throws PlayerNotFoundException If a player cannot be found
	 * @throws MomException If there is a significant problem in the game logic
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final boolean decideAndExecuteUnitMovement (final AIUnitsAndRatings units, final AiUnitCategorySvr category, final List<AIDefenceLocation> underdefendedLocations,
		final AIUnitsAndRatings [] [] [] enemyUnits, final MapCoordinates3DEx desiredCityLocation, final PlayerServerDetails player, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		log.trace ("Entering decideAndExecuteUnitMovement: AI Player ID " + player.getPlayerDescription ().getPlayerID () + ", first Unit URN " + units.get (0).getUnit ().getUnitURN ()); 
		
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		
		// Create a stack for the units (so transports pick up any units stacked with them)
		final List<ExpandedUnitDetails> selectedUnits = new ArrayList<ExpandedUnitDetails> ();
		for (final AIUnitAndRatings mu : units)
			selectedUnits.add (getUnitUtils ().expandUnitDetails (mu.getUnit (), null, null, null, mom.getPlayers (), priv.getFogOfWarMemory (), mom.getServerDB ()));
		
		final UnitStack unitStack = getUnitCalculations ().createUnitStack (selectedUnits, mom.getPlayers (), priv.getFogOfWarMemory (), mom.getServerDB ());
		
		// Work out where the unit can reach
		final int [] [] [] doubleMovementDistances			= new int [mom.getSessionDescription ().getOverlandMapSize ().getDepth ()] [mom.getSessionDescription ().getOverlandMapSize ().getHeight ()] [mom.getSessionDescription ().getOverlandMapSize ().getWidth ()];
		final int [] [] [] movementDirections					= new int [mom.getSessionDescription ().getOverlandMapSize ().getDepth ()] [mom.getSessionDescription ().getOverlandMapSize ().getHeight ()] [mom.getSessionDescription ().getOverlandMapSize ().getWidth ()];
		final boolean [] [] [] canMoveToInOneTurn			= new boolean [mom.getSessionDescription ().getOverlandMapSize ().getDepth ()] [mom.getSessionDescription ().getOverlandMapSize ().getHeight ()] [mom.getSessionDescription ().getOverlandMapSize ().getWidth ()];
		final boolean [] [] [] movingHereResultsInAttack	= new boolean [mom.getSessionDescription ().getOverlandMapSize ().getDepth ()] [mom.getSessionDescription ().getOverlandMapSize ().getHeight ()] [mom.getSessionDescription ().getOverlandMapSize ().getWidth ()];
		
		final MapCoordinates3DEx moveFrom = (MapCoordinates3DEx) units.get (0).getUnit ().getUnitLocation ();

		// Get the list of units who are actually moving
		final List<ExpandedUnitDetails> movingUnits = (unitStack.getTransports ().size () > 0) ? unitStack.getTransports () : unitStack.getUnits ();
		// What's the lowest movement remaining of any unit in the stack
		int doubleMovementRemaining = Integer.MAX_VALUE;
		for (final ExpandedUnitDetails thisUnit : movingUnits)
			if (thisUnit.getDoubleOverlandMovesLeft () < doubleMovementRemaining)
				doubleMovementRemaining = thisUnit.getDoubleOverlandMovesLeft ();
		
		getServerUnitCalculations ().calculateOverlandMovementDistances (moveFrom.getX (), moveFrom.getY (), moveFrom.getZ (),
			player.getPlayerDescription ().getPlayerID (), priv.getFogOfWarMemory (),
			unitStack, doubleMovementRemaining, doubleMovementDistances, movementDirections, canMoveToInOneTurn, movingHereResultsInAttack,
			mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());
		
		// Use list of movement codes from the unit stack's category
		final List<AiMovementCode> movementCodes = category.getMovementCode ().stream ().map (c -> c.getAiMovementCode ()).collect (Collectors.toList ());		
		final AIMovementDecision destination = decideUnitMovement (units, movementCodes, doubleMovementDistances,
			underdefendedLocations, enemyUnits, priv.getFogOfWarMemory ().getMap (), desiredCityLocation, mom.getSessionDescription ().getOverlandMapSize (), mom.getServerDB ());
		
		// Move, if we found somewhere to go
		final boolean moved;
		if (destination == null)
			moved = false;
		
		else if (destination.getDestination () != null)
		{
			// If its a simultaneous turns game, then move as far as we can in one turn.
			// If its a one-player-at-a-time game, then move only 1 cell.
			// This is basically a copy of FogOfWarMidTurnChangesImpl.determineMovementDirection.
			MapCoordinates3DEx coords = new MapCoordinates3DEx (destination.getDestination ());
			MapCoordinates3DEx lastCoords = null;
			while (((mom.getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS) && (!canMoveToInOneTurn [coords.getZ ()] [coords.getY ()] [coords.getX ()])) ||
						((mom.getSessionDescription ().getTurnSystem () == TurnSystem.ONE_PLAYER_AT_A_TIME) && ((coords.getX () != moveFrom.getX ()) || (coords.getY () != moveFrom.getY ()))))
			{
				lastCoords = new MapCoordinates3DEx (coords);
				final int d = getCoordinateSystemUtils ().normalizeDirection (mom.getSessionDescription ().getOverlandMapSize ().getCoordinateSystemType (),
					movementDirections [coords.getZ ()] [coords.getY ()] [coords.getX ()] + 4);
				
				if (!getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, d))
					throw new MomException ("decideAndExecuteUnitMovement: Server map tracing moved to a cell off the map");
			}
			
			if (mom.getSessionDescription ().getTurnSystem () == TurnSystem.ONE_PLAYER_AT_A_TIME)
				coords = lastCoords;
			
			log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " decided to move from " + moveFrom + " to " + coords +
				", eventually aiming for " + destination);
			
			if (coords == null)
				moved = false;		// Until I understand when and how this happens sometimes
			else
			{
				moved = true;
				
				// We need the true unit versions to execute the move
				final List<ExpandedUnitDetails> trueUnits = new ArrayList<ExpandedUnitDetails> ();
				for (final AIUnitAndRatings mu : units)
				{
					final MemoryUnit tu = getUnitUtils ().findUnitURN (mu.getUnit ().getUnitURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), "decideAndExecuteUnitMovement");
					trueUnits.add (getUnitUtils ().expandUnitDetails (tu, null, null, null, mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ()));
				}
				
				// Execute move
				getFogOfWarMidTurnMultiChanges ().moveUnitStack (trueUnits, player, true, moveFrom, coords,
					(mom.getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS), mom);
			}
		}
		
		else if (destination.getSpecialOrder () != null)
		{
			// This seems backwards, but the return value being true means the unit tries to move again; false means stop trying to move
			moved = false;
			final List<Integer> unitURNs = units.stream ().map (u -> u.getUnit ().getUnitURN ()).collect (Collectors.toList ());
			final String error = getUnitServerUtils ().processSpecialOrder (unitURNs, destination.getSpecialOrder (), (MapCoordinates3DEx) units.get (0).getUnit ().getUnitLocation (), player, mom);
			
			if (error != null)
				log.warn ("AI wanted to process special order " + destination.getSpecialOrder () + " but was rejected for reason: " + error);
		}
		
		else
			moved = false;
		
		log.trace ("Exiting decideAndExecuteUnitMovement = " + moved);
		return moved;
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
	 * @return Resource value utils
	 */
	public final ResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
	}

	/**
	 * @param util Resource value utils
	 */
	public final void setResourceValueUtils (final ResourceValueUtils util)
	{
		resourceValueUtils = util;
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
}