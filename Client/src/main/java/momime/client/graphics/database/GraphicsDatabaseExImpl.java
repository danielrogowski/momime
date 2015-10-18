package momime.client.graphics.database;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.swing.NdgUIUtils;

import momime.client.graphics.database.v0_9_7.Animation;
import momime.client.graphics.database.v0_9_7.CityImage;
import momime.client.graphics.database.v0_9_7.CityImagePrerequisite;
import momime.client.graphics.database.v0_9_7.CityViewElement;
import momime.client.graphics.database.v0_9_7.CombatAction;
import momime.client.graphics.database.v0_9_7.CombatAreaEffect;
import momime.client.graphics.database.v0_9_7.CombatTileBorderImage;
import momime.client.graphics.database.v0_9_7.CombatTileUnitRelativeScale;
import momime.client.graphics.database.v0_9_7.GraphicsDatabase;
import momime.client.graphics.database.v0_9_7.HeroItemSlotType;
import momime.client.graphics.database.v0_9_7.HeroItemType;
import momime.client.graphics.database.v0_9_7.MapFeature;
import momime.client.graphics.database.v0_9_7.Pick;
import momime.client.graphics.database.v0_9_7.PlayList;
import momime.client.graphics.database.v0_9_7.ProductionType;
import momime.client.graphics.database.v0_9_7.Race;
import momime.client.graphics.database.v0_9_7.RangedAttackType;
import momime.client.graphics.database.v0_9_7.Spell;
import momime.client.graphics.database.v0_9_7.TileSet;
import momime.client.graphics.database.v0_9_7.TileType;
import momime.client.graphics.database.v0_9_7.Unit;
import momime.client.graphics.database.v0_9_7.UnitSkill;
import momime.client.graphics.database.v0_9_7.UnitSkillComponentImage;
import momime.client.graphics.database.v0_9_7.UnitSpecialOrderImage;
import momime.client.graphics.database.v0_9_7.UnitType;
import momime.client.graphics.database.v0_9_7.WeaponGrade;
import momime.client.graphics.database.v0_9_7.Wizard;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.FrontOrBack;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSpecialOrder;
import momime.common.messages.MemoryBuilding;
import momime.common.utils.MemoryBuildingUtils;

/**
 * Implementation of graphics XML database - extends stubs auto-generated from XSD to add additional functionality from the interface
 */
public final class GraphicsDatabaseExImpl extends GraphicsDatabase implements GraphicsDatabaseEx
{
	/** Class logger */
	private final Log log = LogFactory.getLog (GraphicsDatabaseExImpl.class);
	
	/** Map of pick IDs to pick objects */
	private Map<String, PickGfx> picksMap;

	/** Map of wizard IDs to wizard objects */
	private Map<String, WizardGfx> wizardsMap;

	/** Map of production type IDs to production type objects */
	private Map<String, ProductionTypeGfx> productionTypesMap;
	
	/** Map of race IDs to race objects */
	private Map<String, RaceGfx> racesMap;

	/** Map of building IDs to city view elements */
	private Map<String, CityViewElementGfx> buildingsMap;

	/** Map of city spell effect IDs to city view elements */
	private Map<String, CityViewElementGfx> citySpellEffectsMap;
	
	/** Map of spell IDs to spell objects */
	private Map<String, SpellGfx> spellsMap;
	
	/** Map of combat action IDs to combat action objects */
	private Map<String, CombatActionGfx> combatActionsMap;

	/** Map of unit type IDs to unit type objects */
	private Map<String, UnitTypeGfx> unitTypesMap;

	/** Map of unit attribute component IDs to unit attribute component objects */
	private Map<UnitSkillComponent, UnitSkillComponentImageGfx> UnitSkillComponentsMap;
	
	/** Map of unit skill IDs to unit skill objects */
	private Map<String, UnitSkillGfx> unitSkillsMap;
	
	/** Map of unit IDs to unit objects */
	private Map<String, UnitGfx> unitsMap;

	/** Map of unit special order IDs to unit special order objects */
	private Map<UnitSpecialOrder, UnitSpecialOrderImageGfx> unitSpecialOrdersMap;
	
	/** Map of ranged attack type IDs to ranged attack type objects */
	private Map<String, RangedAttackTypeGfx> rangedAttackTypesMap;
	
	/** Map of weapon grade numbers to weapon grade objects */
	private Map<Integer, WeaponGradeGfx> weaponGradesMap;
	
	/** Map of scales to coordinates for each figure count */
	private Map<Integer, CombatTileUnitRelativeScaleGfx> combatTileUnitRelativeScalesMap;
	
	/** Map of tileSet IDs to tileSet objects */
	private Map<String, TileSetGfx> tileSetsMap;

	/** Map of tile type IDs to tile type XML objects */
	private Map<String, TileTypeGfx> tileTypesMap;
	
	/** Map of map feature IDs to map feature XML objects */
	private Map<String, MapFeatureGfx> mapFeaturesMap;
	
	/** Map of combat area effect IDs to combat area effect XML objects */
	private Map<String, CombatAreaEffectGfx> combatAreaEffectsMap;
	
	/** Map of borderID-directions-F/B to combat tile border image objects */
	private Map<String, CombatTileBorderImageGfx> combatTileBorderImagesMap;
	
	/** Map of hero item type IDs to hero item type objects */
	private Map<String, HeroItemTypeGfx> heroItemTypesMap;

	/** Map of hero item slot type IDs to hero item slot type objects */
	private Map<String, HeroItemSlotTypeGfx> heroItemSlotTypesMap;
	
	/** Map of animation IDs to animation objects */
	private Map<String, AnimationGfx> animationsMap;
	
	/** Map of play list IDs to play list objects */
	private Map<String, PlayListGfx> playListsMap;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;

	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** Size of the largest building image that can be constructed */
	private Dimension largestBuildingSize;
	
	/**
	 * Builds all the hash maps to enable finding records faster
	 */
	public final void buildMaps ()
	{
		log.trace ("Entering buildMaps");
		
		// Create picks map
		picksMap = new HashMap<String, PickGfx> ();
		for (final Pick thisPick : getPick ())
			picksMap.put (thisPick.getPickID (), (PickGfx) thisPick);

		// Create wizards map
		wizardsMap = new HashMap<String, WizardGfx> ();
		for (final Wizard thisWizard : getWizard ())
			wizardsMap.put (thisWizard.getWizardID (), (WizardGfx) thisWizard);

		// Create production types map
		productionTypesMap = new HashMap<String, ProductionTypeGfx> ();
		for (final ProductionType thisProductionType : getProductionType ())
		{
			final ProductionTypeGfx ptex = (ProductionTypeGfx) thisProductionType;
			ptex.buildMap ();
			productionTypesMap.put (thisProductionType.getProductionTypeID (), ptex);
		}
		
		// Create races map
		racesMap = new HashMap<String, RaceGfx> ();
		for (final Race thisRace : getRace ())
		{
			final RaceGfx rex = (RaceGfx) thisRace;
			rex.buildMap ();
			racesMap.put (rex.getRaceID (), rex);
		}

		// Create buildings and city spell effects maps
		buildingsMap = new HashMap<String, CityViewElementGfx> ();
		citySpellEffectsMap = new HashMap<String, CityViewElementGfx> ();
		for (final CityViewElement thisElement : getCityViewElement ())
		{
			final CityViewElementGfx elem = (CityViewElementGfx) thisElement;
			
			// CityViewElements may be buildings, spell effects or neither (e.g. landscape)
			if (elem.getBuildingID () != null)
				buildingsMap.put (elem.getBuildingID (), elem);
			
			if (elem.getCitySpellEffectID () != null)
				citySpellEffectsMap.put (elem.getCitySpellEffectID (), elem);
		}

		// Create spells map
		spellsMap = new HashMap<String, SpellGfx> ();
		for (final Spell thisSpell : getSpell ())
			spellsMap.put (thisSpell.getSpellID (), (SpellGfx) thisSpell);
		
		// Create combatActions map
		combatActionsMap = new HashMap<String, CombatActionGfx> ();
		for (final CombatAction thisCombatAction : getCombatAction ())
			combatActionsMap.put (thisCombatAction.getCombatActionID (), (CombatActionGfx) thisCombatAction);
		
		// Create unit types map
		unitTypesMap = new HashMap<String, UnitTypeGfx> ();
		for (final UnitType thisUnitType : getUnitType ())
		{
			final UnitTypeGfx utex = (UnitTypeGfx) thisUnitType;
			utex.buildMap ();
			unitTypesMap.put (utex.getUnitTypeID (), utex);
		}
		
		// Create unit attribute components map
		UnitSkillComponentsMap = new HashMap<UnitSkillComponent, UnitSkillComponentImageGfx> ();
		for (final UnitSkillComponentImage thisComponent : getUnitSkillComponentImage ())
			UnitSkillComponentsMap.put (thisComponent.getUnitSkillComponentID (), (UnitSkillComponentImageGfx) thisComponent);

		// Create unit skills map
		unitSkillsMap = new HashMap<String, UnitSkillGfx> ();
		for (final UnitSkill thisUnitSkill : getUnitSkill ())
		{
			final UnitSkillGfx skillEx = (UnitSkillGfx) thisUnitSkill;
			skillEx.buildMap ();
			unitSkillsMap.put (thisUnitSkill.getUnitSkillID (), skillEx);
		}
		
		// Create units map
		unitsMap = new HashMap<String, UnitGfx> ();
		for (final Unit thisUnit : getUnit ())
		{
			final UnitGfx UnitGfx = (UnitGfx) thisUnit;
			UnitGfx.buildMap ();
			unitsMap.put (UnitGfx.getUnitID (), UnitGfx);
		}

		// Create unit special orders map
		unitSpecialOrdersMap = new HashMap<UnitSpecialOrder, UnitSpecialOrderImageGfx> ();
		for (final UnitSpecialOrderImage thisSpecialOrder : getUnitSpecialOrderImage ())
			unitSpecialOrdersMap.put (thisSpecialOrder.getUnitSpecialOrderID (), (UnitSpecialOrderImageGfx) thisSpecialOrder);
		
		// Create ranged attack types map
		rangedAttackTypesMap = new HashMap<String, RangedAttackTypeGfx> ();
		for (final RangedAttackType thisRangedAttackType : getRangedAttackType ())
		{
			final RangedAttackTypeGfx ratEx = (RangedAttackTypeGfx) thisRangedAttackType;
			ratEx.buildMap ();
			rangedAttackTypesMap.put (ratEx.getRangedAttackTypeID (), ratEx);
		}
		
		// Create weapon grades map
		weaponGradesMap = new HashMap<Integer, WeaponGradeGfx> ();
		for (final WeaponGrade thisWeaponGrade : getWeaponGrade ())
			weaponGradesMap.put (thisWeaponGrade.getWeaponGradeNumber (), (WeaponGradeGfx) thisWeaponGrade);
		
		// Create combat tile unit relative scales map
		combatTileUnitRelativeScalesMap = new HashMap<Integer, CombatTileUnitRelativeScaleGfx> ();
		for (final CombatTileUnitRelativeScale scale : getCombatTileUnitRelativeScale ())
		{
			final CombatTileUnitRelativeScaleGfx scaleEx = (CombatTileUnitRelativeScaleGfx) scale;
			scaleEx.buildMap ();
			combatTileUnitRelativeScalesMap.put (scaleEx.getScale (), scaleEx);
		}
		
		// Create animations map
		animationsMap = new HashMap<String, AnimationGfx> ();
		for (final Animation anim : getAnimation ())
			animationsMap.put (anim.getAnimationID (), (AnimationGfx) anim);
		
		// Create tile sets map
		tileSetsMap = new HashMap<String, TileSetGfx> ();
		for (final TileSet ts : getTileSet ())
			tileSetsMap.put (ts.getTileSetID (), (TileSetGfx) ts);

		// Create tile types map
		tileTypesMap = new HashMap<String, TileTypeGfx> ();
		for (final TileType tt : getTileType ())
		{
			final TileTypeGfx ttex = (TileTypeGfx) tt;
			ttex.buildMap ();
			tileTypesMap.put (tt.getTileTypeID (), ttex);
		}
		
		// Create map features map
		mapFeaturesMap = new HashMap<String, MapFeatureGfx> ();
		for (final MapFeature mf : getMapFeature ())
			mapFeaturesMap.put (mf.getMapFeatureID (), (MapFeatureGfx) mf);

		// Create combat area effects map
		combatAreaEffectsMap = new HashMap<String, CombatAreaEffectGfx> ();
		for (final CombatAreaEffect cae : getCombatAreaEffect ())
			combatAreaEffectsMap.put (cae.getCombatAreaEffectID (), (CombatAreaEffectGfx) cae);
		
		// Create combat tile border images map
		combatTileBorderImagesMap = new HashMap<String, CombatTileBorderImageGfx> ();
		for (final CombatTileBorderImage ctb : getCombatTileBorderImage ())
			combatTileBorderImagesMap.put (ctb.getCombatTileBorderID () + "-" + ctb.getDirections () + "-" + ctb.getFrontOrBack ().value (), (CombatTileBorderImageGfx) ctb);

		// Create hero item types map
		heroItemTypesMap = new HashMap<String, HeroItemTypeGfx> ();
		for (final HeroItemType itemType : getHeroItemType ())
			heroItemTypesMap.put (itemType.getHeroItemTypeID (), (HeroItemTypeGfx) itemType);

		// Create hero item slot types map
		heroItemSlotTypesMap = new HashMap<String, HeroItemSlotTypeGfx> ();
		for (final HeroItemSlotType itemSlotType : getHeroItemSlotType ())
			heroItemSlotTypesMap.put (itemSlotType.getHeroItemSlotTypeID (), (HeroItemSlotTypeGfx) itemSlotType);
		
		// Create play lists map
		playListsMap = new HashMap<String, PlayListGfx> ();
		for (final PlayList pl : getPlayList ())
			playListsMap.put (pl.getPlayListID (), (PlayListGfx) pl);
		
		log.trace ("Exiting buildMaps");
	}

	/**
	 * Verifies that all animations, tiles and so on are consistent across the graphics DB
	 * @throws IOException If any images cannot be loaded, or any consistency checks fail
	 */
	public final void consistencyChecks () throws IOException
	{
		log.trace ("Entering consistencyChecks");
		log.info ("Processing graphics XML file");
		
		// Check all animations have frames with consistent sizes
		for (final Animation anim : getAnimation ())
		{
			final AnimationGfx aex = (AnimationGfx) anim;
			aex.deriveAnimationWidthAndHeight ();
		}
		log.info ("All " + getAnimation ().size () + " animations passed consistency checks");		
		
		// Build all the smoothing rule bitmask maps, and determine the size of tiles in each set
		for (final TileSet ts : getTileSet ())
		{
			final TileSetGfx tsex = (TileSetGfx) ts;
			tsex.buildMaps ();
			tsex.deriveAnimationFrameCountAndSpeed (this);
			tsex.deriveTileWidthAndHeight (this);
		}
		final TileSetGfx overlandMapTileSet = findTileSet (GraphicsDatabaseConstants.TILE_SET_OVERLAND_MAP, "consistencyChecks");

		// Ensure all map features match the size of the overland map tiles
		for (final MapFeature mf : getMapFeature ())
		{
			final MapFeatureGfx mfex = (MapFeatureGfx) mf;
			mfex.checkWidthAndHeight (overlandMapTileSet);
		}
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
					final AnimationGfx anim = findAnimation (thisBuilding.getCityViewAnimation (), "consistencyChecks");
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
		
		log.trace ("Exiting consistencyChecks");
	}

	/**
	 * Method triggered by Spring when the the DB is created
	 * @throws IOException If any images cannot be loaded, or any consistency checks fail
	 */
	public final void buildMapsAndRunConsistencyChecks () throws IOException
	{
		log.trace ("Entering buildMapsAndRunConsistencyChecks");

		buildMaps ();
		consistencyChecks ();

		log.trace ("Exiting buildMapsAndRunConsistencyChecks");
	}
	
	/**
	 * @param pickID Pick ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Pick object
	 * @throws RecordNotFoundException If the pickID doesn't exist
	 */
	@Override
	public final PickGfx findPick (final String pickID, final String caller) throws RecordNotFoundException
	{
		final PickGfx found = picksMap.get (pickID);
		if (found == null)
			throw new RecordNotFoundException (Pick.class, pickID, caller);

		return found;
	}
	
	/**
	 * @param wizardID Wizard ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Wizard object
	 * @throws RecordNotFoundException If the wizardID doesn't exist
	 */
	@Override
	public final WizardGfx findWizard (final String wizardID, final String caller) throws RecordNotFoundException
	{
		final WizardGfx found = wizardsMap.get (wizardID);
		if (found == null)
			throw new RecordNotFoundException (Wizard.class, wizardID, caller);

		return found;
	}

	/**
	 * @param productionTypeID Production type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Production type object
	 * @throws RecordNotFoundException If the productionTypeID doesn't exist
	 */
	@Override
	public final ProductionTypeGfx findProductionType (final String productionTypeID, final String caller) throws RecordNotFoundException
	{
		final ProductionTypeGfx found = productionTypesMap.get (productionTypeID);
		if (found == null)
			throw new RecordNotFoundException (ProductionType.class, productionTypeID, caller);

		return found;
	}
	
	/**
	 * @param raceID Race ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Race object
	 * @throws RecordNotFoundException If the raceID doesn't exist
	 */
	@Override
	public final RaceGfx findRace (final String raceID, final String caller) throws RecordNotFoundException
	{
		final RaceGfx found = racesMap.get (raceID);
		if (found == null)
			throw new RecordNotFoundException (Race.class, raceID, caller);

		return found;
	}

	/**
	 * @param buildingID Building ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Building object; note buildings in the graphics XML are just a special case of city view elements
	 * @throws RecordNotFoundException If the buildingID doesn't exist
	 */
	@Override
	public final CityViewElementGfx findBuilding (final String buildingID, final String caller) throws RecordNotFoundException
	{
		final CityViewElementGfx found = buildingsMap.get (buildingID);
		if (found == null)
			throw new RecordNotFoundException (CityViewElementGfx.class, buildingID, caller);

		return found;
	}
	
	/**
	 * @param citySpellEffectID City spell effect ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return City spell effect object, or null if not found (e.g. Pestilence has no image)
	 */
	@Override
	public final CityViewElementGfx findCitySpellEffect (final String citySpellEffectID, final String caller)
	{
		return citySpellEffectsMap.get (citySpellEffectID);
	}
	
	/**
	 * @param spellID Spell ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Spell object
	 * @throws RecordNotFoundException If the spellID doesn't exist
	 */
	@Override
	public final SpellGfx findSpell (final String spellID, final String caller) throws RecordNotFoundException
	{
		final SpellGfx found = spellsMap.get (spellID);
		if (found == null)
			throw new RecordNotFoundException (Spell.class, spellID, caller);

		return found;
	}
	
	/**
	 * @param combatActionID Combat action ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Combat action object
	 * @throws RecordNotFoundException If the combatActionID doesn't exist
	 */
	@Override
	public final CombatActionGfx findCombatAction (final String combatActionID, final String caller) throws RecordNotFoundException
	{
		final CombatActionGfx found = combatActionsMap.get (combatActionID);
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
	public final UnitTypeGfx findUnitType (final String unitTypeID, final String caller) throws RecordNotFoundException
	{
		final UnitTypeGfx found = unitTypesMap.get (unitTypeID);
		if (found == null)
			throw new RecordNotFoundException (UnitType.class, unitTypeID, caller);

		return found;
	}
	
	/**
	 * @param UnitSkillComponentID Unit attribute component ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit attribute component object
	 * @throws RecordNotFoundException If the UnitSkillComponentID doesn't exist
	 */
	@Override
	public final UnitSkillComponentImageGfx findUnitSkillComponent (final UnitSkillComponent UnitSkillComponentID, final String caller)
		throws RecordNotFoundException
	{
		final UnitSkillComponentImageGfx found = UnitSkillComponentsMap.get (UnitSkillComponentID);
		if (found == null)
			throw new RecordNotFoundException (UnitSkillComponentImage.class, UnitSkillComponentID.toString (), caller);

		return found;
	}
	
	/**
	 * @return List of all unit skill graphics
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<UnitSkillGfx> getUnitSkills ()
	{
		return (List<UnitSkillGfx>) (List<?>) getUnitSkill ();
	}
	
	/**
	 * @param unitSkillID Unit skill ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit skill object
	 * @throws RecordNotFoundException If the unitSkillID doesn't exist
	 */
	@Override
	public final UnitSkillGfx findUnitSkill (final String unitSkillID, final String caller) throws RecordNotFoundException
	{
		final UnitSkillGfx found = unitSkillsMap.get (unitSkillID);
		if (found == null)
			throw new RecordNotFoundException (UnitSkill.class, unitSkillID, caller);

		return found;
	}
	
	/**
	 * @param unitID Unit ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit object
	 * @throws RecordNotFoundException If the unitID doesn't exist
	 */
	@Override
	public final UnitGfx findUnit (final String unitID, final String caller) throws RecordNotFoundException
	{
		final UnitGfx found = unitsMap.get (unitID);
		if (found == null)
			throw new RecordNotFoundException (Unit.class, unitID, caller);

		return found;
	}
	
	/**
	 * @param unitSpecialOrderID Unit special order ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit special order object
	 * @throws RecordNotFoundException If the unitSpecialOrderID doesn't exist
	 */
	@Override
	public final UnitSpecialOrderImageGfx findUnitSpecialOrder (final UnitSpecialOrder unitSpecialOrderID, final String caller) throws RecordNotFoundException
	{
		final UnitSpecialOrderImageGfx found = unitSpecialOrdersMap.get (unitSpecialOrderID);
		if (found == null)
			throw new RecordNotFoundException (UnitSpecialOrderImage.class, unitSpecialOrderID.toString (), caller);

		return found;
	}
	
	/**
	 * @param rangedAttackTypeID Ranged attack type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Ranged attack type object
	 * @throws RecordNotFoundException If the rangedAttackTypeID doesn't exist
	 */
	@Override
	public final RangedAttackTypeGfx findRangedAttackType (final String rangedAttackTypeID, final String caller) throws RecordNotFoundException
	{
		final RangedAttackTypeGfx found = rangedAttackTypesMap.get (rangedAttackTypeID);
		if (found == null)
			throw new RecordNotFoundException (RangedAttackType.class, rangedAttackTypeID, caller);

		return found;
	}
	
	/**
	 * @param weaponGradeNumber Weapon grade number to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Weapon grade object
	 * @throws RecordNotFoundException If the weaponGradNumber doesn't exist
	 */
	@Override
	public final WeaponGradeGfx findWeaponGrade (final int weaponGradeNumber, final String caller) throws RecordNotFoundException
	{
		final WeaponGradeGfx found = weaponGradesMap.get (weaponGradeNumber);
		if (found == null)
			throw new RecordNotFoundException (WeaponGrade.class, weaponGradeNumber, caller);

		return found;
	}

	/**
	 * @param scale Combat tile unit relative scale
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Scale object
	 * @throws RecordNotFoundException If the scale doesn't exist
	 */
	@Override
	public final CombatTileUnitRelativeScaleGfx findCombatTileUnitRelativeScale (final int scale, final String caller) throws RecordNotFoundException
	{
		final CombatTileUnitRelativeScaleGfx found = combatTileUnitRelativeScalesMap.get (scale);
		if (found == null)
			throw new RecordNotFoundException (CombatTileUnitRelativeScale.class, scale, caller);

		return found;
	}
	
	/**
	 * @param tileSetID Tile set ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Tile set object
	 * @throws RecordNotFoundException If the tileSetID doesn't exist
	 */
	@Override
	public final TileSetGfx findTileSet (final String tileSetID, final String caller) throws RecordNotFoundException
	{
		final TileSetGfx found = tileSetsMap.get (tileSetID);
		if (found == null)
			throw new RecordNotFoundException (TileSet.class, tileSetID, caller);

		return found;
	}

	/**
	 * @param tileTypeID Tile type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Tile type object
	 * @throws RecordNotFoundException If the tileTypeID doesn't exist
	 */
	@Override
	public final TileTypeGfx findTileType (final String tileTypeID, final String caller) throws RecordNotFoundException
	{
		final TileTypeGfx found = tileTypesMap.get (tileTypeID);
		if (found == null)
			throw new RecordNotFoundException (TileType.class, tileTypeID, caller);

		return found;
	}
	
	/**
	 * @param mapFeatureID Map feature ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Map feature object
	 * @throws RecordNotFoundException If the mapFeatureID doesn't exist
	 */
	@Override
	public final MapFeatureGfx findMapFeature (final String mapFeatureID, final String caller) throws RecordNotFoundException
	{
		final MapFeatureGfx found = mapFeaturesMap.get (mapFeatureID);
		if (found == null)
			throw new RecordNotFoundException (MapFeature.class, mapFeatureID, caller);

		return found;
	}
	
	/**
	 * @param combatAreaEffectID Combat area effect ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Combat area effect object
	 * @throws RecordNotFoundException If the combatAreaEffectID doesn't exist
	 */
	@Override
	public final CombatAreaEffectGfx findCombatAreaEffect (final String combatAreaEffectID, final String caller) throws RecordNotFoundException
	{
		final CombatAreaEffectGfx found = combatAreaEffectsMap.get (combatAreaEffectID);
		if (found == null)
			throw new RecordNotFoundException (CombatAreaEffect.class, combatAreaEffectID, caller);

		return found;
	}
	
    /**
     * @param combatTileBorderID Combat tile border ID to search for
     * @param directions Border directions to search for
     * @param frontOrBack Whether to look for the front or back image
     * @return Image details if found; null if not found
     */
	@Override
    public final CombatTileBorderImageGfx findCombatTileBorderImages (final String combatTileBorderID, final String directions, final FrontOrBack frontOrBack)
    {
		return combatTileBorderImagesMap.get (combatTileBorderID + "-" + directions + "-" + frontOrBack.value ());
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
	public final CityImageGfx findBestCityImage (final String citySizeID, final MapCoordinates3DEx cityLocation,
		final List<MemoryBuilding> buildings, final String caller) throws RecordNotFoundException
	{
		CityImageGfx bestMatch = null;
		int bestMatchBuildingCount = 0;
		
		for (final CityImage image : getCityImage ())
			if (image.getCitySizeID ().equals (citySizeID))
			{
				// So the size matches - but does this image require buildings to be present?  If so, count how many
				boolean buildingsMatch = true;
				int buildingCount = 0;
				final Iterator<CityImagePrerequisite> iter = image.getCityImagePrerequisite ().iterator ();
				while ((buildingsMatch) && (iter.hasNext ()))
				{
					final String buildingID = iter.next ().getPrerequisiteID ();
					if (getMemoryBuildingUtils ().findBuilding (buildings, cityLocation, buildingID) != null)
						buildingCount++;
					else
						buildingsMatch = false;
				}
				
				// Is it a better match than we had already?
				if ((buildingsMatch) && ((bestMatch == null) || (buildingCount > bestMatchBuildingCount)))
				{
					bestMatch = (CityImageGfx) image;
					bestMatchBuildingCount = buildingCount;
				}
			}
		
		if (bestMatch == null)
			throw new RecordNotFoundException (CityImage.class, citySizeID, caller);
			
		return bestMatch;
	}
	
	/**
	 * @return List of all city view elemenets (backgrounds, buildings, spell effects and so on)
	 */
	@Override
    @SuppressWarnings ("unchecked")
	public final List<CityViewElementGfx> getCityViewElements ()
    {
    	return (List<CityViewElementGfx>) (List<?>) getCityViewElement ();
    }
	
	/**
	 * @param heroItemTypeID Hero item type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Hero item type object
	 * @throws RecordNotFoundException If the heroItemTypeID doesn't exist
	 */
    @Override
    public final HeroItemTypeGfx findHeroItemType (final String heroItemTypeID, final String caller) throws RecordNotFoundException
    {
		final HeroItemTypeGfx found = heroItemTypesMap.get (heroItemTypeID);
		if (found == null)
			throw new RecordNotFoundException (HeroItemType.class, heroItemTypeID, caller);

		return found;
    }

	/**
	 * @param heroItemSlotTypeID Hero item slot type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Hero item slot type object
	 * @throws RecordNotFoundException If the heroItemSlotTypeID doesn't exist
	 */
    @Override
    public final HeroItemSlotTypeGfx findHeroItemSlotType (final String heroItemSlotTypeID, final String caller) throws RecordNotFoundException
    {
		final HeroItemSlotTypeGfx found = heroItemSlotTypesMap.get (heroItemSlotTypeID);
		if (found == null)
			throw new RecordNotFoundException (HeroItemSlotType.class, heroItemSlotTypeID, caller);

		return found;
    }
	
	/**
	 * @param animationID Animation ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Animation object
	 * @throws RecordNotFoundException If the animationID doesn't exist
	 */
	@Override
	public final AnimationGfx findAnimation (final String animationID, final String caller) throws RecordNotFoundException
	{
		final AnimationGfx found = animationsMap.get (animationID);
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
	public final PlayListGfx findPlayList (final String playListID, final String caller) throws RecordNotFoundException
	{
		final PlayListGfx found = playListsMap.get (playListID);
		if (found == null)
			throw new RecordNotFoundException (PlayList.class, playListID, caller);

		return found;
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