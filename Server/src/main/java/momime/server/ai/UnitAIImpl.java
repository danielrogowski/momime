package momime.server.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.calculations.CityCalculations;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItemTypeAllowedBonus;
import momime.common.database.RecordNotFoundException;
import momime.common.database.SpellBookSectionID;
import momime.common.database.SpellSetting;
import momime.common.database.Unit;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MemoryUnitHeroItemSlot;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.SpellResearchStatusID;
import momime.common.messages.UnitDamage;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.SpellUtils;
import momime.common.utils.UnitUtils;
import momime.server.database.HeroItemBonusSvr;
import momime.server.database.HeroItemSlotTypeSvr;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.SpellSvr;
import momime.server.database.UnitSkillSvr;
import momime.server.database.UnitSvr;
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
		
		// Add 10% for each figure over 1 that the unit has
		double multipliers = ((double) xu.calculateHitPointsRemaining ()) / ((double) xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS));
		multipliers = ((multipliers - 1d) / 10d) + 1d;

		// Go through all skills totalling up additive and multiplicative bonuses from skills
		int total = 0;
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

		// If the unit has no attacks whatsoever (settlers) then severaly hamper its rating since its clearly not a combat unit,
		// and this is supposed to be an estimate of units' capability in combat.
		// This is to stop Troll Settlers getting a massive rating because of their 40 HP
		if ((!xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK)) &&
			(!xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK)))
			
			multipliers = multipliers / 20;
		
		// Apply multiplicative modifiers
		total = (int) (total * multipliers);
		
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
}