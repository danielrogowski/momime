package momime.common.database;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.swing.NdgUIUtils;

import momime.common.MomException;
import momime.common.messages.MemoryBuilding;
import momime.common.utils.MemoryBuildingUtils;

/**
 * Adds maps for faster key lookups over the server-side database read in via JAXB
 */
public final class CommonDatabaseImpl extends MomDatabase implements CommonDatabase
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (CommonDatabaseImpl.class);
	
	/** Map of plane numbers to plane XML objects */
	private Map<Integer, Plane> planesMap;

	/** Map of map feature IDs to map feature XML objects */
	private Map<String, MapFeatureEx> mapFeaturesMap;

	/** Map of tile type IDs to file type XML objects */
	private Map<String, TileTypeEx> tileTypesMap;

	/** Map of production type IDs to production type XML objects */
	private Map<String, ProductionTypeEx> productionTypesMap;

	/** Map of pick type IDs to pick XML objects */
	private Map<String, PickType> pickTypesMap;

	/** Map of pick IDs to pick XML objects */
	private Map<String, Pick> picksMap;

	/** Map of wizard IDs to wizard XML objects */
	private Map<String, WizardEx> wizardsMap;

	/** Map of combat action IDs to combat action XML objects */
	private Map<String, CombatAction> combatActionsMap;
	
	/** Map of unit type IDs to unit type XML objects */
	private Map<String, UnitType> unitTypesMap;

	/** Map of unit IDs to unit XML objects */
	private Map<String, UnitEx> unitsMap;

	/** Map of unit skill IDs to unit skill XML objects */
	private Map<String, UnitSkillEx> unitSkillsMap;

	/** Map of weapon grade numbers to weapon grade XML objects */
	private Map<Integer, WeaponGrade> weaponGradesMap;

	/** Map of ranged attack type IDs to ranged attack type XML objects */
	private Map<String, RangedAttackTypeEx> rangedAttackTypesMap;
	
	/** Map of population task IDs to population task XML objects */
	private Map<String, PopulationTask> populationTasksMap;
	
	/** Map of city size IDs to city size XML objects */
	private Map<String, CitySize> citySizesMap;
	
	/** Map of building IDs to city view elements */
	private Map<String, CityViewElement> cityViewElementBuildingsMap;

	/** Map of city spell effect IDs to city view elements */
	private Map<String, CityViewElement> cityViewElementSpellEffectsMap;
	
	/** Map of race IDs to race XML objects */
	private Map<String, RaceEx> racesMap;

	/** Map of tax rate IDs to tax rate XML objects */
	private Map<String, TaxRate> taxRatesMap;

	/** Map of building IDs to building XML objects */
	private Map<String, Building> buildingsMap;

	/** Map of spell rank IDs to spell rank XML objects */
	private Map<String, SpellRank> spellRanksMap;

	/** Map of spell book section IDs to spell book section objects */
	private Map<SpellBookSectionID, SpellBookSection> spellBookSectionsMap;
	
	/** Map of spell IDs to spell XML objects */
	private Map<String, Spell> spellsMap;

	/** Map of combat area effect IDs to combat area effect objects */
	private Map<String, CombatAreaEffect> combatAreaEffectsMap;
	
	/** Map of combat tile type IDs to combat tile type objects */
	private Map<String, CombatTileType> combatTileTypesMap;

	/** Map of combat tile border IDs to combat tile border objects */
	private Map<String, CombatTileBorder> combatTileBordersMap;
	
	/** Map of borderID-directions-F/B to combat tile border image objects */
	private Map<String, CombatTileBorderImage> combatTileBorderImagesMap;
	
	/** Map of city spell effect IDs to city spell effect objects */
	private Map<String, CitySpellEffect> citySpellEffectsMap;
	
	/** Map of hero item slot type IDs to hero item slot type objects */
	private Map<String, HeroItemSlotType> heroItemSlotTypesMap;

	/** Map of hero item type IDs to hero item type objects */
	private Map<String, HeroItemType> heroItemTypesMap;

	/** Map of hero item bonus IDs to hero item bonus objects */
	private Map<String, HeroItemBonus> heroItemBonusesMap;
	
	/** Map of damage type IDs to damage type objects */
	private Map<String, DamageType> damageTypesMap;
	
	/** Map of tileSet IDs to tileSet objects */
	private Map<String, TileSetEx> tileSetsMap;
	
	/** Map of animation IDs to animation objects */
	private Map<String, AnimationEx> animationsMap;
	
	/** Map of playlist IDs to playlist objects */
	private Map<String, PlayList> playListsMap;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;

	/** Cost to construct the most expensive unit or building in the database */
	private int mostExpensiveConstructionCost;
	
	/** Size of the largest building image that can be constructed */
	private Dimension largestBuildingSize;
	
	/** Hero item bonus ID that grants invisibility */
	private String invisibilityHeroItemBonusID;
	
	/** City walls building ID */
	private String cityWallsBuildingID;

	/**
	 * Builds all the hash maps to enable finding records faster
	 */
	@Override
	public final void buildMaps ()
	{
		// Build lower levels maps
		getTileTypes ().forEach (t -> t.buildMap ());
		getUnits ().forEach (u -> u.buildMaps ());
		getUnitSkills ().forEach (u -> u.buildMap ());
		getRaces ().forEach (r -> r.buildMap ());
		getProductionTypes ().forEach (p -> p.buildMap ());
		getRangedAttackTypes ().forEach (r -> r.buildMaps ());
		
		// Create maps
		planesMap = getPlane ().stream ().collect (Collectors.toMap (p -> p.getPlaneNumber (), p -> p));
		mapFeaturesMap = getMapFeatures ().stream ().collect (Collectors.toMap (f -> f.getMapFeatureID (), f -> f));
		tileTypesMap = getTileTypes ().stream ().collect (Collectors.toMap (t -> t.getTileTypeID (), t -> t));
		productionTypesMap = getProductionTypes ().stream ().collect (Collectors.toMap (p -> p.getProductionTypeID (), p -> p));
		pickTypesMap = getPickType ().stream ().collect (Collectors.toMap (p -> p.getPickTypeID (), p -> p));
		picksMap = getPick ().stream ().collect (Collectors.toMap (p -> p.getPickID (), p -> p));
		wizardsMap = getWizards ().stream ().collect (Collectors.toMap (w -> w.getWizardID (), w -> w));
		combatActionsMap = getCombatAction ().stream ().collect (Collectors.toMap (a -> a.getCombatActionID (), a -> a));
		unitTypesMap = getUnitType ().stream ().collect (Collectors.toMap (u -> u.getUnitTypeID (), u -> u));
		unitsMap = getUnits ().stream ().collect (Collectors.toMap (u -> u.getUnitID (), u -> u));
		unitSkillsMap = getUnitSkills ().stream ().collect (Collectors.toMap (s -> s.getUnitSkillID (), s -> s));
		weaponGradesMap = getWeaponGrade ().stream ().collect (Collectors.toMap (g -> g.getWeaponGradeNumber (), g -> g));
		rangedAttackTypesMap = getRangedAttackTypes ().stream ().collect (Collectors.toMap (r -> r.getRangedAttackTypeID (), r -> r));
		populationTasksMap = getPopulationTask ().stream ().collect (Collectors.toMap (t -> t.getPopulationTaskID (), t -> t));
		citySizesMap = getCitySize ().stream ().collect (Collectors.toMap (s -> s.getCitySizeID (), s -> s));
		racesMap = getRaces ().stream ().collect (Collectors.toMap (r -> r.getRaceID (), r -> r));
		taxRatesMap = getTaxRate ().stream ().collect (Collectors.toMap (r -> r.getTaxRateID (), r -> r));
		buildingsMap = getBuilding ().stream ().collect (Collectors.toMap (b -> b.getBuildingID (), b -> b));
		spellRanksMap = getSpellRank ().stream ().collect (Collectors.toMap (r -> r.getSpellRankID (), r -> r));
		spellBookSectionsMap = getSpellBookSection ().stream ().collect (Collectors.toMap (b -> b.getSpellBookSectionID (), b -> b));
		spellsMap = getSpell ().stream ().collect (Collectors.toMap (s -> s.getSpellID (), s -> s));
		combatAreaEffectsMap = getCombatAreaEffect ().stream ().collect (Collectors.toMap (c -> c.getCombatAreaEffectID (), c -> c));
		combatTileTypesMap = getCombatTileType ().stream ().collect (Collectors.toMap (c -> c.getCombatTileTypeID (), c -> c));
		combatTileBordersMap = getCombatTileBorder ().stream ().collect (Collectors.toMap (c -> c.getCombatTileBorderID (), c -> c));
		citySpellEffectsMap = getCitySpellEffect ().stream ().collect (Collectors.toMap (c -> c.getCitySpellEffectID (), c -> c));
		heroItemSlotTypesMap = getHeroItemSlotType ().stream ().collect (Collectors.toMap (h -> h.getHeroItemSlotTypeID (), h -> h));
		heroItemTypesMap = getHeroItemType ().stream ().collect (Collectors.toMap (h -> h.getHeroItemTypeID (), h -> h));
		heroItemBonusesMap = getHeroItemBonus ().stream ().collect (Collectors.toMap (h -> h.getHeroItemBonusID (), h -> h));
		damageTypesMap = getDamageType ().stream ().collect (Collectors.toMap (d -> d.getDamageTypeID (), d -> d));
		tileSetsMap = getTileSets ().stream ().collect (Collectors.toMap (s -> s.getTileSetID (), s -> s));
		animationsMap = getAnimations ().stream ().collect (Collectors.toMap (a -> a.getAnimationID (), a -> a));
		playListsMap = getPlayList ().stream ().collect (Collectors.toMap (p -> p.getPlayListID (), p -> p));
		
		// City view element maps are a bit unusual as there's separate maps for each type of element
		cityViewElementBuildingsMap = getCityViewElement ().stream ().filter (e -> e.getBuildingID () != null).collect (Collectors.toMap (e -> e.getBuildingID (), e -> e));
		
		// Some spell effects are listed twice, once for Arcanus, once for Myrror, but not all so careful as planeNumber may be null
		cityViewElementSpellEffectsMap = getCityViewElement ().stream ().filter (e -> e.getCitySpellEffectID () != null).collect (Collectors.toMap
			(e -> e.getCitySpellEffectID (), e -> e, (e1, e2) ->
		{
			final CityViewElement e;
			
			// If one has a null plane then pick it
			if (e1.getPlaneNumber () == null)
				e = e1;
			
			else if (e2.getPlaneNumber () == null)
				e = e2;
			
			// Otherwise both must have plane numbers, so pick lowest one
			else if (e1.getPlaneNumber () < e2.getPlaneNumber ())
				e = e1;
			else
				e = e2;
			
			log.info ("City spell effect " + e1.getCitySpellEffectID () + " appears twice in city view element list, with planes " +
				e1.getPlaneNumber () + " and " + e2.getPlaneNumber () + ", so chose one with plane " + e.getPlaneNumber ());
			return e;
		}));

		// There are multiple alternatives for some of the tile borders, i.e. duplicate keys, so Collectors.toMap isn't happy about it, do it the old way
		combatTileBorderImagesMap = new HashMap<String, CombatTileBorderImage> ();
		for (final CombatTileBorderImage ctb : getCombatTileBorderImage ())
			combatTileBorderImagesMap.put (ctb.getCombatTileBorderID () + "-" + ctb.getDirections () + "-" + ctb.getFrontOrBack ().value (), ctb);
	}

	/**
	 * Derives values from the received database
	 * @throws MomException If any of the consistency checks fail
	 */
	@Override
	public final void consistencyChecks () throws MomException
	{
		// Find all movement skills
		final List<String> movementSkills = getUnitSkill ().stream ().filter
			(s -> s.getMovementIconImagePreference () != null).map (s -> s.getUnitSkillID ()).collect (Collectors.toList ());

		// Remove flight - even if the unit has flight, it needs a backup skill in case flight gets cancelled (black sleep/web)
		final List<String> movementSkillsWithoutFlight = new ArrayList<String> ();
		movementSkillsWithoutFlight.addAll (movementSkills);
		movementSkillsWithoutFlight.remove (CommonDatabaseConstants.UNIT_SKILL_ID_FLIGHT);
		
		// Check all units have an HP and double movement speed "skill" value defined
		for (final Unit unitDef : getUnit ())
		{
			final Optional<Integer> hitPoints = unitDef.getUnitHasSkill ().stream ().filter
				(s -> s.getUnitSkillID ().equals (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS)).map (s -> s.getUnitSkillValue ()).findAny ();
			if ((hitPoints.isEmpty ()) || (hitPoints.get () < 1))
				throw new MomException ("Unit " + unitDef.getUnitID () + " has no HP value defined");
			
			final Optional<Integer> resistance = unitDef.getUnitHasSkill ().stream ().filter
				(s -> s.getUnitSkillID ().equals (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE)).map (s -> s.getUnitSkillValue ()).findAny ();
			if ((resistance.isEmpty ()) || (resistance.get () < 1))
				throw new MomException ("Unit " + unitDef.getUnitID () + " has no resistance value defined");
			
			final Optional<Integer> speed = unitDef.getUnitHasSkill ().stream ().filter
				(s -> s.getUnitSkillID ().equals (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED)).map (s -> s.getUnitSkillValue ()).findAny ();
			if ((speed.isEmpty ()) || (speed.get () < 1))
				throw new MomException ("Unit " + unitDef.getUnitID () + " has no movement speed defined");
			
			if (unitDef.getUnitHasSkill ().stream ().noneMatch (s -> movementSkills.contains (s.getUnitSkillID ())))
				throw new MomException ("Unit " + unitDef.getUnitID () + " has no movement skill defined");

			if (unitDef.getUnitHasSkill ().stream ().noneMatch (s -> movementSkillsWithoutFlight.contains (s.getUnitSkillID ())))
				throw new MomException ("Unit " + unitDef.getUnitID () + " only has Flight movement skill - it needs a secondary movement skill in case flight is cancelled");
		}

		// Check all buildings and units to find the most expensive one
		mostExpensiveConstructionCost = 0;
		for (final Building thisBuilding : getBuilding ())
			if (thisBuilding.getProductionCost () != null)
				mostExpensiveConstructionCost = Math.max (mostExpensiveConstructionCost, thisBuilding.getProductionCost ());

		for (final Unit thisUnit : getUnit ())
			if ((CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL.equals (thisUnit.getUnitMagicRealm ())) &&
				(thisUnit.getProductionCost () != null))
				
				mostExpensiveConstructionCost = Math.max (mostExpensiveConstructionCost, thisUnit.getProductionCost ());
		
		log.info ("Most expensive construction project is " + mostExpensiveConstructionCost);
		
		// Find list of all hero item bonuses that grant invisibility
		// Need this multiple times for every draw of the map, so better to figure this out only once
		invisibilityHeroItemBonusID = getHeroItemBonus ().stream ().filter (b -> b.getHeroItemBonusStat ().stream ().anyMatch
			(s -> s.getUnitSkillID ().equals (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_SPELL))).map (b -> b.getHeroItemBonusID ()).findAny ().orElse (null);
		
		if (invisibilityHeroItemBonusID == null)
			log.warn ("No hero item bonus that grants invisibility found");
		else
			log.info ("Hero item bonus that grants invisibility = " + invisibilityHeroItemBonusID);
		
		// Find building ID for city walls by looking for borders which give a defence bonus
		final List<String> combatTileBorderIDsWithDefenceBonus = getCombatTileBorder ().stream ().filter
			(b -> (b.getDefenceBonus () != null) && (b.getDefenceBonus () > 0)).map (b -> b.getCombatTileBorderID ()).collect (Collectors.toList ());
		
		cityWallsBuildingID = getCombatMapElement ().stream ().filter
			(e -> (combatTileBorderIDsWithDefenceBonus.contains (e.getCombatTileBorderID ())) && (e.getBuildingID () != null)).map (e -> e.getBuildingID ()).findAny ().orElse (null);

		if (cityWallsBuildingID == null)
			log.warn ("No building ID found that gives a defence bonus");
		else
			log.info ("Building ID that grants defence bonus (city walls) = " + cityWallsBuildingID);
	}
	
	/**
	 * Consistency checks that can only be ran on the client
	 * @throws IOException If any images cannot be loaded, or any consistency checks fail
	 */
	@Override
	public final void clientConsistencyChecks () throws IOException
	{
		log.info ("Processing common XML file");
		
		// Check all animations have frames with consistent sizes
		for (final Animation anim : getAnimation ())
		{
			final AnimationEx aex = (AnimationEx) anim;
			aex.deriveAnimationWidthAndHeight ();
		}
		log.info ("All " + getAnimation ().size () + " common XML animations passed consistency checks");

		// Build all the smoothing rule bitmask maps, and determine the size of tiles in each set
		for (final TileSetEx ts : getTileSets ())
		{
			ts.buildMaps ();
			ts.deriveAnimationFrameCountAndSpeed (this);
			ts.deriveTileWidthAndHeight (this);
		}
		final TileSetEx overlandMapTileSet = findTileSet (CommonDatabaseConstants.TILE_SET_OVERLAND_MAP, "clientConsistencyChecks");
		
		// Ensure all map features match the size of the overland map tiles
		for (final MapFeatureEx mf : getMapFeatures ())
			mf.checkWidthAndHeight (overlandMapTileSet);
		
		log.info ("All " + getMapFeature ().size () + " map features passed consistency checks");
		
		// Find the largest building
		int largestWidth = 0;
		int largestHeight = 0;
		for (final CityViewElement thisBuilding : getCityViewElement ())
			
			// Not all CityViewElements represent buildings; also ignore Summoning circle and Wizard's fortress because we can't actually construct those
			// (Hard coding these is a bit of a hack, but we don't learn "properly" that we can't construct these until we receive the XML from the server when we join a game)
			if ((thisBuilding.getBuildingID () != null) && (!thisBuilding.getBuildingID ().equals (CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE)) &&
				(!thisBuilding.getBuildingID ().equals (CommonDatabaseConstants.BUILDING_FORTRESS)))
			{
				// It could be an image or animation
				final int thisWidth;
				final int thisHeight;
				if (thisBuilding.getCityViewAnimation () != null)
				{
					final AnimationEx anim = findAnimation (thisBuilding.getCityViewAnimation (), "clientConsistencyChecks");
					thisWidth = anim.getAnimationWidth ();
					thisHeight = anim.getAnimationHeight ();
				}
				else
				{
					final BufferedImage image = getUtils ().loadImage
						((thisBuilding.getCityViewAlternativeImageFile () != null) ? thisBuilding.getCityViewAlternativeImageFile () : thisBuilding.getCityViewImageFile ());
					thisWidth = image.getWidth ();
					thisHeight = image.getHeight ();
				}
				
				largestWidth = Math.max (largestWidth, thisWidth);
				largestHeight = Math.max (largestHeight, thisHeight);
			}
		
		log.info ("Largest building image is " + largestWidth + "x" + largestHeight);
		largestBuildingSize = new Dimension (largestWidth, largestHeight);
	}
	
	/**
	 * @param planeNumber Plane number to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Plane object
	 * @throws RecordNotFoundException If the plane number doesn't exist
	 */
	@Override
	public final Plane findPlane (final int planeNumber, final String caller) throws RecordNotFoundException
	{
		final Plane found = planesMap.get (planeNumber);
		if (found == null)
			throw new RecordNotFoundException (Plane.class, planeNumber, caller);

		return found;
	}

	/**
	 * @return Complete list of all map features in game
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<MapFeatureEx> getMapFeatures ()
	{
		return (List<MapFeatureEx>) (List<?>) getMapFeature ();
	}
	
	/**
	 * @param mapFeatureID Map feature ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Map feature object
	 * @throws RecordNotFoundException If the mapFeatureID doesn't exist
	 */
	@Override
	public final MapFeatureEx findMapFeature (final String mapFeatureID, final String caller) throws RecordNotFoundException
	{
		final MapFeatureEx found = mapFeaturesMap.get (mapFeatureID);
		if (found == null)
			throw new RecordNotFoundException (MapFeature.class, mapFeatureID, caller);

		return found;
	}

	/**
	 * @return Complete list of all tile types in game
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<TileTypeEx> getTileTypes ()
	{
		return (List<TileTypeEx>) (List<?>) getTileType ();
	}
	
	/**
	 * @param tileTypeID Tile type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Tile type object
	 * @throws RecordNotFoundException If the tileTypeID doesn't exist
	 */
	@Override
	public final TileTypeEx findTileType (final String tileTypeID, final String caller) throws RecordNotFoundException
	{
		final TileTypeEx found = tileTypesMap.get (tileTypeID);
		if (found == null)
			throw new RecordNotFoundException (TileType.class, tileTypeID, caller);

		return found;
	}

	/**
	 * @return Complete list of all production types in game
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<ProductionTypeEx> getProductionTypes ()
	{
		return (List<ProductionTypeEx>) (List<?>) getProductionType ();
	}
	
	/**
	 * @param productionTypeID Production type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Production type object
	 * @throws RecordNotFoundException If the productionTypeID doesn't exist
	 */
	@Override
	public final ProductionTypeEx findProductionType (final String productionTypeID, final String caller) throws RecordNotFoundException
	{
		final ProductionTypeEx found = productionTypesMap.get (productionTypeID);
		if (found == null)
			throw new RecordNotFoundException (ProductionType.class, productionTypeID, caller);

		return found;
	}

	/**
	 * @param pickTypeID Pick type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return PickType object
	 * @throws RecordNotFoundException If the pickTypeID doesn't exist
	 */
	@Override
	public final PickType findPickType (final String pickTypeID, final String caller) throws RecordNotFoundException
	{
		final PickType found = pickTypesMap.get (pickTypeID);
		if (found == null)
			throw new RecordNotFoundException (PickType.class, pickTypeID, caller);

		return found;
	}
	
	/**
	 * @param pickID Pick ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Pick object
	 * @throws RecordNotFoundException If the pickID doesn't exist
	 */
	@Override
	public final Pick findPick (final String pickID, final String caller) throws RecordNotFoundException
	{
		final Pick found = picksMap.get (pickID);
		if (found == null)
			throw new RecordNotFoundException (Pick.class, pickID, caller);

		return found;
	}
	
	/**
	 * @return Complete list of all wizards in game
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<WizardEx> getWizards ()
	{
		return (List<WizardEx>) (List<?>) getWizard ();
	}
	
	/**
	 * @param wizardID Wizard ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Wizard object
	 * @throws RecordNotFoundException If the wizardID doesn't exist
	 */
	@Override
	public final WizardEx findWizard (final String wizardID, final String caller) throws RecordNotFoundException
	{
		final WizardEx found = wizardsMap.get (wizardID);
		if (found == null)
			throw new RecordNotFoundException (Wizard.class, wizardID, caller);

		return found;
	}

	/**
	 * @param combatActionID Combat action ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Combat action object
	 * @throws RecordNotFoundException If the combatActionID doesn't exist
	 */
	@Override
	public final CombatAction findCombatAction (final String combatActionID, final String caller) throws RecordNotFoundException
	{
		final CombatAction found = combatActionsMap.get (combatActionID);
		if (found == null)
			throw new RecordNotFoundException (CombatAction.class, combatActionID, caller);

		return found;
	}
	
	/**
	 * @param unitTypeID Unit type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit type object
	 * @throws RecordNotFoundException If the unitTypeID doesn't exist
	 */
	@Override
	public final UnitType findUnitType (final String unitTypeID, final String caller) throws RecordNotFoundException
	{
		final UnitType found = unitTypesMap.get (unitTypeID);
		if (found == null)
			throw new RecordNotFoundException (UnitType.class, unitTypeID, caller);

		return found;
	}
	
	/**
	 * @return Complete list of all units in game
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<UnitEx> getUnits ()
	{
		return (List<UnitEx>) (List<?>) getUnit ();
	}
	
	/**
	 * @param unitID Unit ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit object
	 * @throws RecordNotFoundException If the unitID doesn't exist
	 */
	@Override
	public final UnitEx findUnit (final String unitID, final String caller) throws RecordNotFoundException
	{
		final UnitEx found = unitsMap.get (unitID);
		if (found == null)
			throw new RecordNotFoundException (Unit.class, unitID, caller);

		return found;
	}
	
	/**
	 * @return Complete list of all unit skills in game
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<UnitSkillEx> getUnitSkills ()
	{
		return (List<UnitSkillEx>) (List<?>) getUnitSkill ();
	}
	
	/**
	 * @param unitSkillID Unit skill ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit skill object
	 * @throws RecordNotFoundException If the unitSkillID doesn't exist
	 */
	@Override
	public final UnitSkillEx findUnitSkill (final String unitSkillID, final String caller) throws RecordNotFoundException
	{
		final UnitSkillEx found = unitSkillsMap.get (unitSkillID);
		if (found == null)
			throw new RecordNotFoundException (UnitSkill.class, unitSkillID, caller);

		return found;
	}

	/**
	 * @param weaponGradeNumber Weapon grade number to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Weapon grade object
	 * @throws RecordNotFoundException If the weapon grade number doesn't exist
	 */
	@Override
	public final WeaponGrade findWeaponGrade (final int weaponGradeNumber, final String caller) throws RecordNotFoundException
	{
		final WeaponGrade found = weaponGradesMap.get (weaponGradeNumber);
		if (found == null)
			throw new RecordNotFoundException (WeaponGrade.class, weaponGradeNumber, caller);

		return found;
	}

	/**
	 * @return Complete list of all ranged attack types in game
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<RangedAttackTypeEx> getRangedAttackTypes ()
	{
		return (List<RangedAttackTypeEx>) (List<?>) getRangedAttackType ();
	}
	
	/**
	 * @param rangedAttackTypeID RAT ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return RAT object
	 * @throws RecordNotFoundException If the RAT ID doesn't exist
	 */
	@Override
	public final RangedAttackTypeEx findRangedAttackType (final String rangedAttackTypeID, final String caller) throws RecordNotFoundException
	{
		final RangedAttackTypeEx found = rangedAttackTypesMap.get (rangedAttackTypeID);
		if (found == null)
			throw new RecordNotFoundException (RangedAttackType.class, rangedAttackTypeID, caller);

		return found;
	}
	
	/**
	 * @param populationTaskID Population task ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Population task object
	 * @throws RecordNotFoundException If the population task ID doesn't exist
	 */
	@Override
	public final PopulationTask findPopulationTask (final String populationTaskID, final String caller) throws RecordNotFoundException
	{
		final PopulationTask found = populationTasksMap.get (populationTaskID);
		if (found == null)
			throw new RecordNotFoundException (PopulationTask.class, populationTaskID, caller);

		return found;
	}
	
	/**
	 * @param citySizeID City size ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return City size object
	 * @throws RecordNotFoundException If the city size ID doesn't exist
	 */
	@Override
	public final CitySize findCitySize (final String citySizeID, final String caller) throws RecordNotFoundException
	{
		final CitySize found = citySizesMap.get (citySizeID);
		if (found == null)
			throw new RecordNotFoundException (CitySize.class, citySizeID, caller);

		return found;
	}
	
	/**
	 * @return Complete list of all races in game
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<RaceEx> getRaces ()
	{
		return (List<RaceEx>) (List<?>) getRace ();
	}
	
	/**
	 * @param raceID Race ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Race object
	 * @throws RecordNotFoundException If the raceID doesn't exist
	 */
	@Override
	public final RaceEx findRace (final String raceID, final String caller) throws RecordNotFoundException
	{
		final RaceEx found = racesMap.get (raceID);
		if (found == null)
			throw new RecordNotFoundException (Race.class, raceID, caller);

		return found;
	}

	/**
	 * @param taxRateID Tax rate ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Tax rate object
	 * @throws RecordNotFoundException If the tax rateID doesn't exist
	 */
	@Override
	public final TaxRate findTaxRate (final String taxRateID, final String caller) throws RecordNotFoundException
	{
		final TaxRate found = taxRatesMap.get (taxRateID);
		if (found == null)
			throw new RecordNotFoundException (TaxRate.class, taxRateID, caller);

		return found;
	}

	/**
	 * @param buildingID Building ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Building object
	 * @throws RecordNotFoundException If the buildingID doesn't exist
	 */
	@Override
	public final Building findBuilding (final String buildingID, final String caller) throws RecordNotFoundException
	{
		final Building found = buildingsMap.get (buildingID);
		if (found == null)
			throw new RecordNotFoundException (Building.class, buildingID, caller);

		return found;
	}

	/**
	 * @param spellRankID Spell rank ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Spell rank object
	 * @throws RecordNotFoundException If the spellRankID doesn't exist
	 */
	@Override
	public final SpellRank findSpellRank (final String spellRankID, final String caller) throws RecordNotFoundException
	{
		final SpellRank found = spellRanksMap.get (spellRankID);
		if (found == null)
			throw new RecordNotFoundException (SpellRank.class, spellRankID, caller);

		return found;
	}
	
	/**
	 * @param sectionID Spell book section ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Spell book section object
	 * @throws RecordNotFoundException If the sectionID doesn't exist
	 */
	@Override
	public final SpellBookSection findSpellBookSection (final SpellBookSectionID sectionID, final String caller) throws RecordNotFoundException
	{
		final SpellBookSection found = spellBookSectionsMap.get (sectionID);
		if (found == null)
			throw new RecordNotFoundException (SpellBookSection.class, sectionID.toString (), caller);

		return found;
	}
	
	/**
	 * @param spellID Spell ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Spell object
	 * @throws RecordNotFoundException If the spellID doesn't exist
	 */
	@Override
	public final Spell findSpell (final String spellID, final String caller) throws RecordNotFoundException
	{
		final Spell found = spellsMap.get (spellID);
		if (found == null)
			throw new RecordNotFoundException (Spell.class, spellID, caller);

		return found;
	}
	
	/**
	 * @param combatAreaEffectID Combat area effect ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return CombatAreaEffect object
	 * @throws RecordNotFoundException If the combat area effect ID doesn't exist
	 */
	@Override
	public final CombatAreaEffect findCombatAreaEffect (final String combatAreaEffectID, final String caller) throws RecordNotFoundException
	{
		final CombatAreaEffect found = combatAreaEffectsMap.get (combatAreaEffectID);
		if (found == null)
			throw new RecordNotFoundException (CombatAreaEffect.class, combatAreaEffectID, caller);

		return found;
	}

	/**
	 * @param combatTileTypeID Combat tile type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return CombatTileType object
	 * @throws RecordNotFoundException If the combat tile type ID doesn't exist
	 */
	@Override
	public final CombatTileType findCombatTileType (final String combatTileTypeID, final String caller) throws RecordNotFoundException
	{
		final CombatTileType found = combatTileTypesMap.get (combatTileTypeID);
		if (found == null)
			throw new RecordNotFoundException (CombatTileType.class, combatTileTypeID, caller);

		return found;
	}

	/**
	 * @param combatTileBorderID Combat tile border ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return CombatTileBorder object
	 * @throws RecordNotFoundException If the combat tile border ID doesn't exist
	 */
	@Override
	public final CombatTileBorder findCombatTileBorder (final String combatTileBorderID, final String caller) throws RecordNotFoundException
	{
		final CombatTileBorder found = combatTileBordersMap.get (combatTileBorderID);
		if (found == null)
			throw new RecordNotFoundException (CombatTileBorder.class, combatTileBorderID, caller);

		return found;
	}

    /**
     * @param combatTileBorderID Combat tile border ID to search for
     * @param directions Border directions to search for
     * @param frontOrBack Whether to look for the front or back image
     * @return Image details if found; null if not found
     */
	@Override
    public final CombatTileBorderImage findCombatTileBorderImages (final String combatTileBorderID, final String directions, final FrontOrBack frontOrBack)
    {
		return combatTileBorderImagesMap.get (combatTileBorderID + "-" + directions + "-" + frontOrBack.value ());
    }
	
	/**
	 * @param citySpellEffectID City spell effect ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return CitySpellEffect object
	 * @throws RecordNotFoundException If the city spell effect ID doesn't exist
	 */
	@Override
	public final CitySpellEffect findCitySpellEffect (final String citySpellEffectID, final String caller) throws RecordNotFoundException
	{
		final CitySpellEffect found = citySpellEffectsMap.get (citySpellEffectID);
		if (found == null)
			throw new RecordNotFoundException (CitySpellEffect.class, citySpellEffectID, caller);

		return found;
	}
	
	/**
	 * Note this isn't straightforward like the other lookups, since one citySizeID can have multiple entries in the graphics XML,
	 * some with specialised graphics showing particular buildings.  So this must pick the most appropriate entry.
	 * 
	 * @param citySizeID City size ID to search for
	 * @param cityLocation Location of the city, so we can check what buildings it has
	 * @param buildings List of known buildings
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return City size object
	 * @throws RecordNotFoundException If no city size entries match the requested citySizeID
	 */
	@Override
	public final CityImage findBestCityImage (final String citySizeID, final MapCoordinates3DEx cityLocation,
		final List<MemoryBuilding> buildings, final String caller) throws RecordNotFoundException
	{
		CityImage bestMatch = null;
		int bestMatchBuildingCount = 0;
		
		for (final CityImage image : getCityImage ())
			if (image.getCitySizeID ().equals (citySizeID))
			{
				// So the size matches - but does this image require buildings to be present?  If so, count how many
				boolean buildingsMatch = true;
				int buildingCount = 0;
				final Iterator<String> iter = image.getCityImagePrerequisite ().iterator ();
				while ((buildingsMatch) && (iter.hasNext ()))
				{
					if (getMemoryBuildingUtils ().findBuilding (buildings, cityLocation, iter.next ()) != null)
						buildingCount++;
					else
						buildingsMatch = false;
				}
				
				// Is it a better match than we had already?
				if ((buildingsMatch) && ((bestMatch == null) || (buildingCount > bestMatchBuildingCount)))
				{
					bestMatch = image;
					bestMatchBuildingCount = buildingCount;
				}
			}
		
		if (bestMatch == null)
			throw new RecordNotFoundException (CityImage.class, citySizeID, caller);
			
		return bestMatch;
	}
		
	/**
	 * @param buildingID Building ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Building object; note buildings in the graphics XML are just a special case of city view elements
	 * @throws RecordNotFoundException If the buildingID doesn't exist
	 */
	@Override
	public final CityViewElement findCityViewElementBuilding (final String buildingID, final String caller) throws RecordNotFoundException
	{
		final CityViewElement found = cityViewElementBuildingsMap.get (buildingID);
		if (found == null)
			throw new RecordNotFoundException (CityViewElement.class, buildingID, caller);

		return found;
	}
	
	/**
	 * Note some city spell effects have more than one city view element defined, one for arcanus and one for myrror.  In this case this will return the arcanus entry in preference.
	 * 
	 * @param citySpellEffectID City spell effect ID to search for
	 * @return City spell effect object, or null if not found (e.g. Pestilence has no image)
	 */
	@Override
	public final CityViewElement findCityViewElementSpellEffect (final String citySpellEffectID)
	{
		return cityViewElementSpellEffectsMap.get (citySpellEffectID);
	}
	
	/**
	 * @param heroItemSlotTypeID Hero item slot type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return HeroItemSlotType object
	 * @throws RecordNotFoundException If the hero item slot type ID doesn't exist
	 */
	@Override
	public final HeroItemSlotType findHeroItemSlotType (final String heroItemSlotTypeID, final String caller) throws RecordNotFoundException
	{
		final HeroItemSlotType found = heroItemSlotTypesMap.get (heroItemSlotTypeID);
		if (found == null)
			throw new RecordNotFoundException (HeroItemSlotType.class, heroItemSlotTypeID, caller);

		return found;
	}
	
	/**
	 * @param heroItemTypeID Hero item type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return HeroItemType object
	 * @throws RecordNotFoundException If the hero item type ID doesn't exist
	 */
	@Override
	public final HeroItemType findHeroItemType (final String heroItemTypeID, final String caller) throws RecordNotFoundException
	{
		final HeroItemType found = heroItemTypesMap.get (heroItemTypeID);
		if (found == null)
			throw new RecordNotFoundException (HeroItemType.class, heroItemTypeID, caller);

		return found;
	}

	/**
	 * @param heroItemBonusID Hero item bonus ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return HeroItemBonus object
	 * @throws RecordNotFoundException If the hero item bonus ID doesn't exist
	 */
	@Override
	public final HeroItemBonus findHeroItemBonus (final String heroItemBonusID, final String caller) throws RecordNotFoundException
	{
		final HeroItemBonus found = heroItemBonusesMap.get (heroItemBonusID);
		if (found == null)
			throw new RecordNotFoundException (HeroItemBonus.class, heroItemBonusID, caller);

		return found;
	}

	/**
	 * @param damageTypeID Damage type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return DamageType object
	 * @throws RecordNotFoundException If the damage type ID doesn't exist
	 */
	@Override
	public final DamageType findDamageType (final String damageTypeID, final String caller) throws RecordNotFoundException
	{
		final DamageType found = damageTypesMap.get (damageTypeID);
		if (found == null)
			throw new RecordNotFoundException (DamageType.class, damageTypeID, caller);

		return found;
	}

	/**
	 * @return Complete list of all tile sets in game
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<TileSetEx> getTileSets ()
	{
		return (List<TileSetEx>) (List<?>) getTileSet ();
	}
	
	/**
	 * @param tileSetID Tile set ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Tile set object
	 * @throws RecordNotFoundException If the tileSetID doesn't exist
	 */
	@Override
	public final TileSetEx findTileSet (final String tileSetID, final String caller) throws RecordNotFoundException
	{
		final TileSetEx found = tileSetsMap.get (tileSetID);
		if (found == null)
			throw new RecordNotFoundException (TileSet.class, tileSetID, caller);

		return found;
	}
	
	/**
	 * @return Complete list of all animations in game
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<AnimationEx> getAnimations ()
	{
		return (List<AnimationEx>) (List<?>) getAnimation ();
	}
	
	/**
	 * @param animationID Animation ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Animation object
	 * @throws RecordNotFoundException If the animationID doesn't exist
	 */
	@Override
	public final AnimationEx findAnimation (final String animationID, final String caller) throws RecordNotFoundException
	{
		final AnimationEx found = animationsMap.get (animationID);
		if (found == null)
			throw new RecordNotFoundException (Animation.class, animationID, caller);

		return found;
	}
	
	/**
	 * @param playListID Play list ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Play list object
	 * @throws RecordNotFoundException If the playListID doesn't exist
	 */
	@Override
	public final PlayList findPlayList (final String playListID, final String caller) throws RecordNotFoundException
	{
		final PlayList found = playListsMap.get (playListID);
		if (found == null)
			throw new RecordNotFoundException (PlayList.class, playListID, caller);

		return found;
	}
	
	/**
	 * @return Cost to construct the most expensive unit or building in the database
	 */
	@Override
	public final int getMostExpensiveConstructionCost ()
	{
		return mostExpensiveConstructionCost;
	}

	/**
	 * NB. This will find the largest width and the largest height separately, so its possible this may return a dimension
	 * which no building actually has, if e.g. the widest is 50x25 and the tallest is 20x40 then it would return 50x40.
	 * 
	 * @return Size of the largest building image that can be constructed
	 */
	@Override
	public final Dimension getLargestBuildingSize ()
	{
		return largestBuildingSize;
	}
	
	/**
	 * @return Hero item bonus ID that grants invisibility
	 */
	@Override
	public final String getInvisibilityHeroItemBonusID ()
	{
		return invisibilityHeroItemBonusID;
	}

	/**
	 * @return City walls building ID
	 */
	@Override
	public final String getCityWallsBuildingID ()
	{
		return cityWallsBuildingID;
	}
	
	/**
	 * @return Memory building utils
	 */
	public final MemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param util Memory building utils
	 */
	public final void setMemoryBuildingUtils (final MemoryBuildingUtils util)
	{
		memoryBuildingUtils = util;
	}

	/**
	 * @return Helper methods and constants for creating and laying out Swing components
	 */
	public final NdgUIUtils getUtils ()
	{
		return utils;
	}

	/**
	 * @param util Helper methods and constants for creating and laying out Swing components
	 */
	public final void setUtils (final NdgUIUtils util)
	{
		utils = util;
	}
}