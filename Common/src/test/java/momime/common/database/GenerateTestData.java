package momime.common.database;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.common.database.newgame.CastingReductionCombination;
import momime.common.database.newgame.MapSizeData;
import momime.common.database.newgame.SpellSettingData;
import momime.common.database.newgame.SwitchResearch;
import momime.common.database.Building;
import momime.common.database.BuildingPopulationProductionModifier;
import momime.common.database.BuildingRequiresTileType;
import momime.common.database.CombatAreaAffectsPlayersID;
import momime.common.database.CombatAreaEffect;
import momime.common.database.CombatAreaEffectAttributeBonus;
import momime.common.database.CombatAreaEffectSkillBonus;
import momime.common.database.CombatTileBorder;
import momime.common.database.CombatTileBorderBlocksMovementID;
import momime.common.database.CombatTileType;
import momime.common.database.ExperienceAttributeBonus;
import momime.common.database.ExperienceLevel;
import momime.common.database.ExperienceSkillBonus;
import momime.common.database.FortressPickTypeProduction;
import momime.common.database.FortressPlaneProduction;
import momime.common.database.MapFeature;
import momime.common.database.MapFeatureProduction;
import momime.common.database.Pick;
import momime.common.database.PickPrerequisite;
import momime.common.database.PickProductionBonus;
import momime.common.database.PickType;
import momime.common.database.Plane;
import momime.common.database.ProductionType;
import momime.common.database.Race;
import momime.common.database.RacePopulationTask;
import momime.common.database.RacePopulationTaskProduction;
import momime.common.database.RaceUnrest;
import momime.common.database.RangedAttackType;
import momime.common.database.RoundingDirectionID;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.SummonedUnit;
import momime.common.database.TaxRate;
import momime.common.database.TileType;
import momime.common.database.Unit;
import momime.common.database.UnitHasAttributeValue;
import momime.common.database.UnitHasSkill;
import momime.common.database.UnitMagicRealm;
import momime.common.database.UnitSkill;
import momime.common.database.UnitType;
import momime.common.database.UnitUpkeep;
import momime.common.database.WeaponGrade;
import momime.common.database.WeaponGradeAttributeBonus;
import momime.common.database.WeaponGradeSkillBonus;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MapAreaOfMemoryGridCells;
import momime.common.messages.MapRowOfCombatTiles;
import momime.common.messages.MapRowOfMemoryGridCells;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MomCombatTile;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemType;

/**
 * Since the tests in the common project can't use the XML file (since the classes generated from the server XSD that allow
 * JAXB to load the server XML are server side only) yet many of the tests in common need it, this manufactures pieces
 * of test data that are used by more than one test
 */
public final class GenerateTestData
{
	/** Gems */
	public static final String GEMS = "MF01";

	/** Adamantium ore */
	public static final String ADAMANTIUM_ORE = "MF09";

	/** Wild game */
	public static final String WILD_GAME = "MF10";

	/** Mountains */
	public static final String MOUNTAINS_TILE = "TT01";

	/** Hills */
	public static final String HILLS_TILE = "TT02";

	/** Shore */
	public static final String SHORE_TILE = "TT08";

	/** River */
	public static final String RIVER_TILE = "TT10";

	/** Life book */
	public static final String LIFE_BOOK = "MB01";

	/** Chaos book */
	public static final String CHAOS_BOOK = "MB03";

	/** Nature book */
	public static final String NATURE_BOOK = "MB04";

	/** Sorcery book */
	public static final String SORCERY_BOOK = "MB05";

	/** Archmage retort */
	public static final String ARCHMAGE = "RT04";

	/** Artificer retort */
	public static final String ARTIFICER = "RT05";

	/** Summoner retort */
	public static final String SUMMONER = "RT06";

	/** Summoner retort */
	public static final String SAGE_MASTER = "RT07";

	/** Divine power retort */
	public static final String DIVINE_POWER = "RT09";

	/** Famous retort */
	public static final String FAMOUS = "RT10";

	/** Runemaster retort */
	public static final String RUNEMASTER = "RT11";

	/** Charismatic retort */
	public static final String CHARISMATIC = "RT12";

	/** Chaos mastery retort */
	public static final String CHAOS_MASTERY = "RT13";

	/** Mana focusing retort */
	public static final String MANA_FOCUSING = "RT17";

	/** Pick type for books */
	public static final String BOOK = "B";

	/** Pick type for retorts */
	public static final String RETORT = "R";

	/** Typical melee unit */
	public static final String BARBARIAN_SPEARMEN = "UN040";

	/** Typical ranged unit */
	public static final String BARBARIAN_BOWMEN = "UN042";
	
	/** Typical magic unit */
	public static final String DARK_ELF_WARLOCKS = "UN065";

	/** Stone giant */
	public static final String STONE_GIANT_UNIT = "UN185";

	/** Magic spirit */
	public static final String MAGIC_SPIRIT_UNIT = "UN155";

	/** Hell hounds */
	public static final String HELL_HOUNDS_UNIT = "UN156";

	/** War bears */
	public static final String WAR_BEARS_UNIT = "UN180";

	/** Giant spiders unit */
	public final static String GIANT_SPIDERS_UNIT = "UN184";

	/** Sample hero unit */
	public static final String DWARF_HERO = "UN001";

	/** Unit magic realm/Lifeform type */
	public static final String LIFEFORM_TYPE_ARCANE = "LT06";

	/** Unit magic realm/Lifeform type */
	public static final String LIFEFORM_TYPE_CHAOS = "LT03";

	/** Unit magic realm/Lifeform type */
	public static final String LIFEFORM_TYPE_NATURE = "LT04";

	/** Chaos channeled unit */
	public static final String LIFEFORM_TYPE_CC = "LTCC";

	/** Natural flight skill */
	public static final String UNIT_SKILL_FLIGHT = "USX04";

	/** Chaos channels flight skill */
	public static final String UNIT_SKILL_CC_FLIGHT = "SS093C";

	/** Thrown weapons skill */
	public final static String UNIT_SKILL_THROWN_WEAPONS = "US126";
	
	/** Hero agility skill */
	public final static String HERO_SKILL_AGILITY = "HS01";

	/** Hero blademaster skill */
	public final static String HERO_SKILL_BLADEMASTER = "HS04";
	
	/** Earth to mud spell */
	public final static String EARTH_TO_MUD = "SP001";

	/** Warp wood spell */
	public final static String WARP_WOOD = "SP081";

	/** Giant spiders spell */
	public final static String GIANT_SPIDERS_SPELL = "SP014";

	/** Hell hounds spell */
	public final static String HELL_HOUNDS_SPELL = "SP084";

	/** Magic spirit spell */
	public final static String MAGIC_SPIRIT_SPELL = "SP201";

	/** Dispel magic spell */
	public final static String DISPEL_MAGIC_SPELL = "SP202";

	/** Summoning circle spell */
	public final static String SUMMONING_CIRCLE = "SP203";

	/** Common spell */
	public final static String COMMON = "SR01";

	/** Uncommon spell */
	public final static String UNCOMMON = "SR02";

	/** CAE with affects players = blank */
	public final static String CAE_AFFECTS_BLANK = "CAE01";

	/** CAE with affects players = all */
	public final static String CAE_AFFECTS_ALL = "CAE02";

	/** CAE with affects players = caster */
	public final static String CAE_AFFECTS_CASTER = "CAE03";

	/** CAE with affects players = both */
	public final static String CAE_AFFECTS_BOTH = "CAE04";

	/** CAE with affects players = opponent */
	public final static String CAE_AFFECTS_OPPONENT = "CAE05";

	/** Animists' guild */
	public final static String ANIMISTS_GUILD = "BL10";

	/** Ship wrights' guild */
	public final static String SHIP_WRIGHTS_GUILD = "BL12";

	/** Sawmill */
	public final static String SAWMILL = "BL15";

	/** Sages' guild */
	public final static String SAGES_GUILD = "BL17";

	/** Alchemists' guild */
	public final static String ALCHEMISTS_GUILD = "BL19";

	/** Shrine */
	public final static String SHRINE = "BL22";

	/** Temple */
	public final static String TEMPLE = "BL23";

	/** Granary */
	public final static String GRANARY = "BL29";

	/** Farmers' Market */
	public final static String FARMERS_MARKET = "BL30";

	/** Miners' guild */
	public final static String MINERS_GUILD = "BL34";

	/** Barbarian */
	public final static String BARBARIAN = "RC01";

	/** High elf */
	public final static String HIGH_ELF = "RC04";

	/** High men */
	public final static String HIGH_MEN = "RC05";

	/** Klackons */
	public final static String KLACKONS = "RC06";

	/** Dwarves */
	public final static String DWARVES = "RC13";

	/** Tax rate with 0 gold and 0% unrest */
	public final static String TAX_RATE_0_GOLD_0_UNREST = "TR01";

	/** Tax rate with 2 gold and 45% unrest */
	public final static String TAX_RATE_2_GOLD_45_UNREST = "TR05";

	/** Tax rate with 3 gold and 75% unrest */
	public final static String TAX_RATE_3_GOLD_75_UNREST = "TR07";

	/** Walking skill */
	public final static String WALKING = "USX01";

	/** Quantity of ranged attack ammo skill */
	public final static String RANGED_ATTACK_AMMO = "US132";
	
	/** Arrows */
	public final static String RANGED_ATTACK_TYPE_ARROWS = "RAT14";

	/** Fireball */
	public final static String RANGED_ATTACK_TYPE_FIREBALL = "RAT1F";
	
	/**
	 * @return Archmage retort with its pre-requisites
	 */
	public final static Pick createArchmageRetort ()
	{
		final Pick pick = new Pick ();
		pick.setPickID (ARCHMAGE);
		pick.setPickCost (1);
		pick.setPickType (RETORT);

		final PickPrerequisite req = new PickPrerequisite ();
		req.setPrerequisiteTypeID (BOOK);
		req.setPrerequisiteCount (4);
		pick.getPickPrerequisite ().add (req);

		final PickProductionBonus bonus = new PickProductionBonus ();
		bonus.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT);
		bonus.setPercentageBonus (50);
		pick.getPickProductionBonus ().add (bonus);

		return pick;
	}

	/**
	 * @return Selected data required by the tests
	 * @throws RecordNotFoundException If an entry expected in the database can't be found; though this is here because of the mocking and shouldn't be triggered
	 */
	public final static CommonDatabase createDB () throws RecordNotFoundException
	{
		// Planes
		final List<Plane> planes = new ArrayList<Plane> ();
		for (int n = 0; n < 2; n++)
		{
			final Plane plane = new Plane ();
			plane.setPlaneNumber (n);
			planes.add (plane);
		}

		final FortressPlaneProduction myrrorFortressMana = new FortressPlaneProduction ();
		myrrorFortressMana.setFortressProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER);
		myrrorFortressMana.setDoubleAmount (10);
		planes.get (1).getFortressPlaneProduction ().add (myrrorFortressMana);

		// Map features
		final List<MapFeature> mapFeatures = new ArrayList<MapFeature> ();

		final MapFeature gems = new MapFeature ();
		gems.setMapFeatureID (GEMS);
		gems.setRaceMineralMultiplerApplies (true);
		final MapFeatureProduction gemsProduction = new MapFeatureProduction ();
		gemsProduction.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		gemsProduction.setDoubleAmount (10);
		gems.getMapFeatureProduction ().add (gemsProduction);
		mapFeatures.add (gems);

		final MapFeature adamantiumOre = new MapFeature ();
		adamantiumOre.setMapFeatureID (ADAMANTIUM_ORE);
		adamantiumOre.setFeatureMagicWeapons (3);
		adamantiumOre.setRaceMineralMultiplerApplies (true);
		final MapFeatureProduction adamantiumOreProduction = new MapFeatureProduction ();
		adamantiumOreProduction.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER);
		adamantiumOreProduction.setDoubleAmount (4);
		adamantiumOre.getMapFeatureProduction ().add (adamantiumOreProduction);
		mapFeatures.add (adamantiumOre);

		final MapFeature wildGame = new MapFeature ();
		wildGame.setMapFeatureID (WILD_GAME);
		final MapFeatureProduction wildGameRations = new MapFeatureProduction ();
		wildGameRations.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS);
		wildGameRations.setDoubleAmount (4);
		wildGame.getMapFeatureProduction ().add (wildGameRations);
		final MapFeatureProduction wildGameFood = new MapFeatureProduction ();
		wildGameFood.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
		wildGameFood.setDoubleAmount (4);
		wildGame.getMapFeatureProduction ().add (wildGameFood);
		mapFeatures.add (wildGame);

		// Tile types
		final List<TileType> tileTypes = new ArrayList<TileType> ();

		final TileType mountains = new TileType ();
		mountains.setTileTypeID (MOUNTAINS_TILE);
		mountains.setProductionBonus (5);
		tileTypes.add (mountains);

		final TileType hills = new TileType ();
		hills.setTileTypeID (HILLS_TILE);
		hills.setProductionBonus (3);
		hills.setDoubleFood (1);
		tileTypes.add (hills);

		final TileType shore = new TileType ();
		shore.setTileTypeID (SHORE_TILE);
		shore.setGoldBonus (10);
		shore.setGoldBonusSurroundingTiles (true);
		shore.setDoubleFood (1);
		tileTypes.add (shore);

		final TileType river = new TileType ();
		river.setTileTypeID (RIVER_TILE);
		river.setGoldBonus (20);
		river.setDoubleFood (4);
		tileTypes.add (river);

		// Production types
		final List<ProductionType> productionTypes = new ArrayList<ProductionType> ();

		final ProductionType rations = new ProductionType ();
		rations.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS);
		rations.setRoundingDirectionID (RoundingDirectionID.MUST_BE_EXACT_MULTIPLE);
		productionTypes.add (rations);

		final ProductionType production = new ProductionType ();
		production.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
		production.setRoundingDirectionID (RoundingDirectionID.ROUND_UP);
		productionTypes.add (production);

		final ProductionType gold = new ProductionType ();
		gold.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		gold.setRoundingDirectionID (RoundingDirectionID.ROUND_DOWN);
		productionTypes.add (gold);

		final ProductionType magicPower = new ProductionType ();
		magicPower.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER);
		magicPower.setRoundingDirectionID (RoundingDirectionID.ROUND_DOWN);
		productionTypes.add (magicPower);

		final ProductionType research = new ProductionType ();
		research.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH);
		research.setRoundingDirectionID (RoundingDirectionID.MUST_BE_EXACT_MULTIPLE);
		productionTypes.add (research);

		final ProductionType food = new ProductionType ();
		food.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
		food.setRoundingDirectionID (RoundingDirectionID.ROUND_UP);
		productionTypes.add (food);

		// Pick types
		final List<PickType> pickTypes = new ArrayList<PickType> ();

		final PickType bookPickType = new PickType ();
		bookPickType.setPickTypeID (BOOK);
		final FortressPickTypeProduction bookFortressMana = new FortressPickTypeProduction ();
		bookFortressMana.setFortressProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER);
		bookFortressMana.setDoubleAmount (2);
		bookPickType.getFortressPickTypeProduction ().add (bookFortressMana);
		pickTypes.add (bookPickType);

		final PickType retortPickType = new PickType ();
		retortPickType.setPickTypeID (RETORT);
		pickTypes.add (retortPickType);

		// Picks
		final List<Pick> picks = new ArrayList<Pick> ();

		// Life book
		final Pick lifeBook = new Pick ();
		lifeBook.setPickID (LIFE_BOOK);
		lifeBook.setPickCost (1);
		lifeBook.setPickType (BOOK);
		picks.add (lifeBook);

		// Chaos book
		final Pick chaosBook = new Pick ();
		chaosBook.setPickID (CHAOS_BOOK);
		picks.add (chaosBook);

		// Nature book
		final Pick natureBook = new Pick ();
		natureBook.setPickID (NATURE_BOOK);
		natureBook.setPickCost (1);
		natureBook.setPickType (BOOK);
		picks.add (natureBook);

		// Sorcery book
		final Pick sorceryBook = new Pick ();
		sorceryBook.setPickID (SORCERY_BOOK);
		sorceryBook.setPickCost (1);
		sorceryBook.setPickType (BOOK);
		picks.add (sorceryBook);

		// Alchemy retort
		final Pick alchemy = new Pick ();
		alchemy.setPickID (CommonDatabaseConstants.RETORT_ID_ALCHEMY);
		alchemy.setPickMagicWeapons (1);
		picks.add (alchemy);

		// Archmage retort
		picks.add (createArchmageRetort ());

		// Artificer retort
		final Pick artificer = new Pick ();
		artificer.setPickID (ARTIFICER);
		artificer.setPickCost (1);
		artificer.setPickType (RETORT);
		picks.add (artificer);

		// Summoner
		final Pick summoner = new Pick ();
		summoner.setPickID (SUMMONER);
		summoner.setPickType (RETORT);

		final PickProductionBonus summonerResearchBonus = new PickProductionBonus ();
		summonerResearchBonus.setUnitTypeID (CommonDatabaseConstants.UNIT_TYPE_ID_SUMMONED);
		summonerResearchBonus.setPercentageBonus (25);
		summonerResearchBonus.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH);
		summoner.getPickProductionBonus ().add (summonerResearchBonus);

		final PickProductionBonus summonerCastingBonus = new PickProductionBonus ();
		summonerCastingBonus.setUnitTypeID (CommonDatabaseConstants.UNIT_TYPE_ID_SUMMONED);
		summonerCastingBonus.setPercentageBonus (25);
		summonerCastingBonus.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_SPELL_COST_REDUCTION);
		summoner.getPickProductionBonus ().add (summonerCastingBonus);

		final PickProductionBonus summonerUpkeepReduction = new PickProductionBonus ();
		summonerUpkeepReduction.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_UNIT_UPKEEP_REDUCTION);
		summonerUpkeepReduction.setUnitTypeID (CommonDatabaseConstants.UNIT_TYPE_ID_SUMMONED);
		summonerUpkeepReduction.setPercentageBonus (25);
		summoner.getPickProductionBonus ().add (summonerUpkeepReduction);

		picks.add (summoner);

		// Sage master
		final Pick sageMaster = new Pick ();
		sageMaster.setPickID (SAGE_MASTER);

		final PickProductionBonus sageMasterResearchBonus = new PickProductionBonus ();
		sageMasterResearchBonus.setPercentageBonus (25);
		sageMasterResearchBonus.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH);
		sageMaster.getPickProductionBonus ().add (sageMasterResearchBonus);

		picks.add (sageMaster);

		// Divine power retort
		final Pick divinePower = new Pick ();
		divinePower.setPickID (DIVINE_POWER);
		divinePower.setPickCost (2);
		divinePower.setPickType (RETORT);
		divinePower.setPickReligiousBuildingBonus (50);
		picks.add (divinePower);

		// Famous retort
		final Pick famous = new Pick ();
		famous.setPickID (FAMOUS);
		famous.setPickCost (2);
		famous.setPickType (RETORT);
		picks.add (famous);

		// Runemaster
		final Pick runemaster = new Pick ();
		runemaster.setPickID (RUNEMASTER);

		final PickProductionBonus runemasterResearchBonus = new PickProductionBonus ();
		runemasterResearchBonus.setMagicRealmIdBlank (true);
		runemasterResearchBonus.setPercentageBonus (25);
		runemasterResearchBonus.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH);
		runemaster.getPickProductionBonus ().add (runemasterResearchBonus);

		final PickProductionBonus runemasterCastingBonus = new PickProductionBonus ();
		runemasterCastingBonus.setMagicRealmIdBlank (true);
		runemasterCastingBonus.setPercentageBonus (25);
		runemasterCastingBonus.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_SPELL_COST_REDUCTION);
		runemaster.getPickProductionBonus ().add (runemasterCastingBonus);

		picks.add (runemaster);

		// Charismatic retort
		final Pick charismatic = new Pick ();
		charismatic.setPickID (CHARISMATIC);
		charismatic.setPickCost (1);
		charismatic.setPickType (RETORT);
		picks.add (charismatic);

		// Chaos mastery
		final Pick chaosMastery = new Pick ();
		chaosMastery.setPickID (CHAOS_MASTERY);

		final PickProductionBonus chaosResearchBonus = new PickProductionBonus ();
		chaosResearchBonus.setMagicRealmID (CHAOS_BOOK);
		chaosResearchBonus.setPercentageBonus (15);
		chaosResearchBonus.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH);
		chaosMastery.getPickProductionBonus ().add (chaosResearchBonus);

		final PickProductionBonus chaosCastingBonus = new PickProductionBonus ();
		chaosCastingBonus.setMagicRealmID (CHAOS_BOOK);
		chaosCastingBonus.setPercentageBonus (15);
		chaosCastingBonus.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_SPELL_COST_REDUCTION);
		chaosMastery.getPickProductionBonus ().add (chaosCastingBonus);

		picks.add (chaosMastery);

		final Pick manaFocusing = new Pick ();
		manaFocusing.setPickID (MANA_FOCUSING);
		final PickProductionBonus manaFocusingBonus = new PickProductionBonus ();
		manaFocusingBonus.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
		manaFocusingBonus.setPercentageBonus (25);
		manaFocusing.getPickProductionBonus ().add (manaFocusingBonus);
		picks.add (manaFocusing);

		// Typical melee unit
		final List<Unit> units = new ArrayList<Unit> ();

		final Unit spearmen = new Unit ();
		spearmen.setUnitID (BARBARIAN_SPEARMEN);
		spearmen.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		spearmen.setDoubleMovement (2);

		final UnitHasAttributeValue spearmenMelee = new UnitHasAttributeValue ();
		spearmenMelee.setUnitAttributeID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK);
		spearmenMelee.setAttributeValue (1);
		spearmen.getUnitAttributeValue ().add (spearmenMelee);
		
		final UnitHasAttributeValue spearmenDefence = new UnitHasAttributeValue ();
		spearmenDefence.setUnitAttributeID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE);
		spearmenDefence.setAttributeValue (2);
		spearmen.getUnitAttributeValue ().add (spearmenDefence);
		units.add (spearmen);
		
		// Typical ranged unit
		final Unit bowmen = new Unit ();
		bowmen.setUnitID (BARBARIAN_BOWMEN);
		bowmen.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		bowmen.setDoubleMovement (2);
		bowmen.setRangedAttackType (RANGED_ATTACK_TYPE_ARROWS);

		final UnitHasAttributeValue bowmenMelee = new UnitHasAttributeValue ();
		bowmenMelee.setUnitAttributeID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK);
		bowmenMelee.setAttributeValue (1);
		bowmen.getUnitAttributeValue ().add (bowmenMelee);

		final UnitHasAttributeValue bowmenRanged = new UnitHasAttributeValue ();
		bowmenRanged.setUnitAttributeID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK);
		bowmenRanged.setAttributeValue (1);
		bowmen.getUnitAttributeValue ().add (bowmenRanged);
		
		final UnitHasAttributeValue bowmenDefence = new UnitHasAttributeValue ();
		bowmenDefence.setUnitAttributeID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE);
		bowmenDefence.setAttributeValue (2);
		bowmen.getUnitAttributeValue ().add (bowmenDefence);
		units.add (bowmen);

		// Typical magic unit
		final Unit warlocks = new Unit ();
		warlocks.setUnitID (DARK_ELF_WARLOCKS);
		warlocks.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		warlocks.setDoubleMovement (2);
		warlocks.setRangedAttackType (RANGED_ATTACK_TYPE_FIREBALL);

		final UnitHasAttributeValue warlocksMelee = new UnitHasAttributeValue ();
		warlocksMelee.setUnitAttributeID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK);
		warlocksMelee.setAttributeValue (1);
		warlocks.getUnitAttributeValue ().add (warlocksMelee);

		final UnitHasAttributeValue warlocksRanged = new UnitHasAttributeValue ();
		warlocksRanged.setUnitAttributeID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK);
		warlocksRanged.setAttributeValue (7);
		warlocks.getUnitAttributeValue ().add (warlocksRanged);
		
		final UnitHasAttributeValue warlocksDefence = new UnitHasAttributeValue ();
		warlocksDefence.setUnitAttributeID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE);
		warlocksDefence.setAttributeValue (4);
		warlocks.getUnitAttributeValue ().add (warlocksDefence);
		units.add (warlocks);
		
		final UnitUpkeep warlocksGold = new UnitUpkeep ();
		warlocksGold.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		warlocksGold.setUpkeepValue (5);
		warlocks.getUnitUpkeep ().add (warlocksGold);
		final UnitUpkeep warlocksRations = new UnitUpkeep ();
		warlocksRations.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS);
		warlocksRations.setUpkeepValue (1);
		warlocks.getUnitUpkeep ().add (warlocksRations);
		final UnitHasSkill warlocksWalking = new UnitHasSkill ();
		warlocksWalking.setUnitSkillID (WALKING);
		warlocks.getUnitHasSkill ().add (warlocksWalking);
		final UnitHasSkill warlocksShooting = new UnitHasSkill ();
		warlocksShooting.setUnitSkillID (RANGED_ATTACK_AMMO);
		warlocksShooting.setUnitSkillValue (4);
		warlocks.getUnitHasSkill ().add (warlocksShooting);
		units.add (warlocks);

		// Typical summoned unit
		final Unit stoneGiant = new Unit ();
		stoneGiant.setUnitID (STONE_GIANT_UNIT);
		stoneGiant.setUnitMagicRealm (LIFEFORM_TYPE_NATURE);

		final UnitHasAttributeValue stoneGiantDefence = new UnitHasAttributeValue ();
		stoneGiantDefence.setUnitAttributeID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE);
		stoneGiantDefence.setAttributeValue (8);
		stoneGiant.getUnitAttributeValue ().add (stoneGiantDefence);
		units.add (stoneGiant);
		
		final UnitUpkeep stoneGiantUpkeep = new UnitUpkeep ();
		stoneGiantUpkeep.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
		stoneGiantUpkeep.setUpkeepValue (9);
		stoneGiant.getUnitUpkeep ().add (stoneGiantUpkeep);
		units.add (stoneGiant);

		final Unit magicSpirit = new Unit ();
		magicSpirit.setUnitID (MAGIC_SPIRIT_UNIT);
		magicSpirit.setUnitMagicRealm (LIFEFORM_TYPE_ARCANE);
		units.add (magicSpirit);

		final Unit warBears = new Unit ();
		warBears.setUnitID (WAR_BEARS_UNIT);
		warBears.setUnitMagicRealm (LIFEFORM_TYPE_NATURE);
		units.add (warBears);

		final Unit hellHounds = new Unit ();
		hellHounds.setUnitID (HELL_HOUNDS_UNIT);
		hellHounds.setUnitMagicRealm (LIFEFORM_TYPE_CHAOS);
		hellHounds.setDoubleMovement (4);
		units.add (hellHounds);

		final Unit dwarf = new Unit ();
		dwarf.setUnitID (DWARF_HERO);
		dwarf.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
		units.add (dwarf);

		final UnitHasAttributeValue dwarfDefence = new UnitHasAttributeValue ();
		dwarfDefence.setUnitAttributeID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE);
		dwarfDefence.setAttributeValue (4);
		dwarf.getUnitAttributeValue ().add (dwarfDefence);
		units.add (dwarf);
		
		final Unit giantSpidersUnit = new Unit ();
		giantSpidersUnit.setUnitID (GIANT_SPIDERS_UNIT);
		giantSpidersUnit.setUnitMagicRealm (LIFEFORM_TYPE_NATURE);
		units.add (giantSpidersUnit);

		// Magic realm/lifeform types
		final List<UnitMagicRealm> unitMagicRealms = new ArrayList<UnitMagicRealm> ();

		final UnitMagicRealm arcane = new UnitMagicRealm ();
		arcane.setUnitMagicRealmID (LIFEFORM_TYPE_ARCANE);
		arcane.setUnitTypeID (CommonDatabaseConstants.UNIT_TYPE_ID_SUMMONED);
		unitMagicRealms.add (arcane);

		final UnitMagicRealm nature = new UnitMagicRealm ();
		nature.setUnitMagicRealmID (LIFEFORM_TYPE_NATURE);
		nature.setUnitTypeID (CommonDatabaseConstants.UNIT_TYPE_ID_SUMMONED);
		unitMagicRealms.add (nature);

		final UnitMagicRealm chaos = new UnitMagicRealm ();
		chaos.setUnitMagicRealmID (LIFEFORM_TYPE_CHAOS);
		chaos.setUnitTypeID (CommonDatabaseConstants.UNIT_TYPE_ID_SUMMONED);
		unitMagicRealms.add (chaos);

		final UnitMagicRealm normalMagicRealm = new UnitMagicRealm ();
		normalMagicRealm.setUnitMagicRealmID (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		normalMagicRealm.setUnitTypeID ("N");
		unitMagicRealms.add (normalMagicRealm);

		final UnitMagicRealm heroMagicRealm = new UnitMagicRealm ();
		heroMagicRealm.setUnitMagicRealmID (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
		heroMagicRealm.setUnitTypeID ("H");
		unitMagicRealms.add (heroMagicRealm);

		// Unit Types
		final List<UnitType> unitTypes = new ArrayList<UnitType> ();

		final UnitType normalUnit = new UnitType ();
		normalUnit.setUnitTypeID ("N");

		for (int n = 0; n <= 5; n++)
		{
			final ExperienceLevel expLevel = new ExperienceLevel ();
			expLevel.setLevelNumber (n);
			if (n <= 3)
				expLevel.setExperienceRequired (n * 10);		// Set up levels 4 and 5 to be only attainable with warlord+crusade

			normalUnit.getExperienceLevel ().add (expLevel);

			// Give bonus to thrown weapons
			if (n > 0)
			{
				final ExperienceSkillBonus bonus = new ExperienceSkillBonus ();
				bonus.setUnitSkillID (UNIT_SKILL_THROWN_WEAPONS);
				bonus.setBonusValue (n);
				expLevel.getExperienceSkillBonus ().add (bonus);
			}

			// Give bonus to defence and ranged attack
			if (n > 0)
			{
				final ExperienceAttributeBonus  bonus1 = new ExperienceAttributeBonus  ();
				bonus1.setUnitAttributeID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE);
				bonus1.setBonusValue (n);
				expLevel.getExperienceAttributeBonus ().add (bonus1);

				final ExperienceAttributeBonus  bonus2 = new ExperienceAttributeBonus  ();
				bonus2.setUnitAttributeID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK);
				bonus2.setBonusValue (n);
				expLevel.getExperienceAttributeBonus ().add (bonus2);
			}
		}

		unitTypes.add (normalUnit);

		final UnitType heroUnit = new UnitType ();
		heroUnit.setUnitTypeID ("H");

		for (int n = 0; n <= 8; n++)
		{
			final ExperienceLevel expLevel = new ExperienceLevel ();
			expLevel.setLevelNumber (n);
			expLevel.setExperienceRequired (n * 10);		// All levels attaintable through experience

			heroUnit.getExperienceLevel ().add (expLevel);

			// Give bonus to thrown weapons
			if (n > 0)
			{
				final ExperienceSkillBonus bonus = new ExperienceSkillBonus ();
				bonus.setUnitSkillID (UNIT_SKILL_THROWN_WEAPONS);
				bonus.setBonusValue (n);
				expLevel.getExperienceSkillBonus ().add (bonus);
			}
		}

		unitTypes.add (heroUnit);

		final UnitType summonedUnit = new UnitType ();
		summonedUnit.setUnitTypeID (CommonDatabaseConstants.UNIT_TYPE_ID_SUMMONED);
		unitTypes.add (summonedUnit);

		// Unit skills
		final List<UnitSkill> unitSkills = new ArrayList<UnitSkill> ();

		final UnitSkill experience = new UnitSkill ();
		experience.setUnitSkillID (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE);
		unitSkills.add (experience);

		final UnitSkill flight = new UnitSkill ();
		flight.setUnitSkillID (UNIT_SKILL_FLIGHT);
		unitSkills.add (flight);

		final UnitSkill ccFlight = new UnitSkill ();
		ccFlight.setUnitSkillID (UNIT_SKILL_CC_FLIGHT);
		ccFlight.setChangesUnitToMagicRealm (LIFEFORM_TYPE_CC);
		unitSkills.add (ccFlight);

		final UnitSkill thrownWeapons = new UnitSkill ();
		thrownWeapons.setUnitSkillID (UNIT_SKILL_THROWN_WEAPONS);
		unitSkills.add (thrownWeapons);

		final UnitSkill agility = new UnitSkill ();
		agility.setUnitSkillID (HERO_SKILL_AGILITY);
		agility.setAddsToAttributeID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE);
		agility.setAddsToAttributeDivisor (1);
		unitSkills.add (agility);

		final UnitSkill blademaster = new UnitSkill ();
		blademaster.setUnitSkillID (HERO_SKILL_BLADEMASTER);
		blademaster.setAddsToAttributeID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT);
		blademaster.setAddsToAttributeDivisor (2);
		unitSkills.add (blademaster);
		
		// Weapon grades
		final List<WeaponGrade> weaponGrades = new ArrayList<WeaponGrade> ();

		for (int n = 0; n <= 3; n++)
		{
			final WeaponGrade grade = new WeaponGrade ();
			grade.setWeaponGradeNumber (n);
			weaponGrades.add (grade);

			// Give bonus to thrown weapons
			if (n >= 2)
			{
				final WeaponGradeSkillBonus bonus = new WeaponGradeSkillBonus ();
				bonus.setUnitSkillID (UNIT_SKILL_THROWN_WEAPONS);
				bonus.setBonusValue (n - 1);
				grade.getWeaponGradeSkillBonus ().add (bonus);
			}

			// Give bonus to defence and ranged attack
			if (n >= 2)
			{
				final WeaponGradeAttributeBonus bonus1 = new WeaponGradeAttributeBonus ();
				bonus1.setUnitAttributeID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE);
				bonus1.setBonusValue (n - 1);
				grade.getWeaponGradeAttributeBonus ().add (bonus1);

				final WeaponGradeAttributeBonus bonus2 = new WeaponGradeAttributeBonus ();
				bonus2.setUnitAttributeID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK);
				bonus2.setBonusValue (n - 1);
				grade.getWeaponGradeAttributeBonus ().add (bonus2);
			}
		}
		
		// Ranged attack types
		final List<RangedAttackType> rangedAttackTypes = new ArrayList<RangedAttackType> ();
		
		final RangedAttackType ratArrows = new RangedAttackType ();
		ratArrows.setRangedAttackTypeID (RANGED_ATTACK_TYPE_ARROWS);
		ratArrows.setMithrilAndAdamantiumVersions (true);
		rangedAttackTypes.add (ratArrows);

		final RangedAttackType ratFireball = new RangedAttackType ();
		ratFireball.setRangedAttackTypeID (RANGED_ATTACK_TYPE_FIREBALL);
		ratFireball.setMithrilAndAdamantiumVersions (false);
		rangedAttackTypes.add (ratFireball);
		
		// Spells
		final List<Spell> spells = new ArrayList<Spell> ();

		final Spell earthToMud = new Spell ();
		earthToMud.setSpellID (EARTH_TO_MUD);
		earthToMud.setSpellRealm (NATURE_BOOK);
		earthToMud.setSpellRank (COMMON);
		earthToMud.setCombatCastingCost (10);
		earthToMud.setResearchCost (6);
		spells.add (earthToMud);

		final Spell warpWood = new Spell ();
		warpWood.setSpellID (WARP_WOOD);
		warpWood.setSpellRealm (CHAOS_BOOK);
		warpWood.setSpellRank (COMMON);
		warpWood.setCombatCastingCost (15);
		warpWood.setOverlandCastingCost (5);
		warpWood.setResearchCost (3);
		spells.add (warpWood);

		final Spell giantSpiders = new Spell ();
		giantSpiders.setSpellID (GIANT_SPIDERS_SPELL);
		giantSpiders.setSpellRealm (NATURE_BOOK);
		giantSpiders.setSpellRank (UNCOMMON);
		giantSpiders.setSpellBookSectionID (SpellBookSectionID.SUMMONING);
		giantSpiders.setOverlandCastingCost (3);
		giantSpiders.setResearchCost (7);
		final SummonedUnit giantSpidersSummon = new SummonedUnit ();
		giantSpidersSummon.setSummonedUnitID (GIANT_SPIDERS_UNIT);
		giantSpiders.getSummonedUnit ().add (giantSpidersSummon);
		spells.add (giantSpiders);

		final Spell hellHoundsSpell = new Spell ();
		hellHoundsSpell.setSpellID (HELL_HOUNDS_SPELL);
		hellHoundsSpell.setSpellRealm (CHAOS_BOOK);
		hellHoundsSpell.setSpellRank (COMMON);
		hellHoundsSpell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);
		hellHoundsSpell.setOverlandCastingCost (7);
		hellHoundsSpell.setResearchCost (9);
		final SummonedUnit hellHoundsSummon = new SummonedUnit ();
		hellHoundsSummon.setSummonedUnitID (HELL_HOUNDS_UNIT);
		hellHoundsSpell.getSummonedUnit ().add (hellHoundsSummon);
		spells.add (hellHoundsSpell);

		final Spell magicSpiritSpell = new Spell ();
		magicSpiritSpell.setSpellID (MAGIC_SPIRIT_SPELL);
		magicSpiritSpell.setSpellRank (COMMON);
		magicSpiritSpell.setOverlandCastingCost (6);
		magicSpiritSpell.setResearchCost (2);
		spells.add (magicSpiritSpell);

		final Spell dispelMagicSpell = new Spell ();
		dispelMagicSpell.setSpellID (DISPEL_MAGIC_SPELL);
		dispelMagicSpell.setSpellRank (COMMON);
		dispelMagicSpell.setOverlandCastingCost (9);
		dispelMagicSpell.setResearchCost (1);
		spells.add (dispelMagicSpell);

		final Spell summoningCircle = new Spell ();
		summoningCircle.setSpellID (SUMMONING_CIRCLE);
		summoningCircle.setSpellRank (COMMON);
		summoningCircle.setCombatCastingCost (5);
		summoningCircle.setResearchCost (5);
		spells.add (summoningCircle);

		// Combat area effects
		final List<CombatAreaEffect> combatAreaEffects = new ArrayList<CombatAreaEffect> ();

		final CombatAreaEffect affectsBlank = new CombatAreaEffect ();
		affectsBlank.setCombatAreaEffectID (CAE_AFFECTS_BLANK);
		combatAreaEffects.add (affectsBlank);

		final CombatAreaEffect affectsAll = new CombatAreaEffect ();
		affectsAll.setCombatAreaEffectID (CAE_AFFECTS_ALL);
		affectsAll.setCombatAreaAffectsPlayers (CombatAreaAffectsPlayersID.ALL_EVEN_NOT_IN_COMBAT);

		final CombatAreaEffectSkillBonus caeSkillBonus = new CombatAreaEffectSkillBonus ();
		caeSkillBonus.setUnitSkillID (UNIT_SKILL_THROWN_WEAPONS);
		caeSkillBonus.setBonusValue (1);
		caeSkillBonus.setEffectMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		affectsAll.getCombatAreaEffectSkillBonus ().add (caeSkillBonus);

		final CombatAreaEffectAttributeBonus caeAttributeBonus = new CombatAreaEffectAttributeBonus ();
		caeAttributeBonus.setUnitAttributeID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE);
		caeAttributeBonus.setBonusValue (1);
		caeAttributeBonus.setEffectMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		affectsAll.getCombatAreaEffectAttributeBonus ().add (caeAttributeBonus);
		
		combatAreaEffects.add (affectsAll);

		final CombatAreaEffect affectsCaster = new CombatAreaEffect ();
		affectsCaster.setCombatAreaEffectID (CAE_AFFECTS_CASTER);
		affectsCaster.setCombatAreaAffectsPlayers (CombatAreaAffectsPlayersID.CASTER_ONLY);
		combatAreaEffects.add (affectsCaster);

		final CombatAreaEffect affectsBoth = new CombatAreaEffect ();
		affectsBoth.setCombatAreaEffectID (CAE_AFFECTS_BOTH);
		affectsBoth.setCombatAreaAffectsPlayers (CombatAreaAffectsPlayersID.BOTH_PLAYERS_IN_COMBAT);
		combatAreaEffects.add (affectsBoth);

		final CombatAreaEffect affectsOpponent = new CombatAreaEffect ();
		affectsOpponent.setCombatAreaEffectID (CAE_AFFECTS_OPPONENT);
		affectsOpponent.setCombatAreaAffectsPlayers (CombatAreaAffectsPlayersID.COMBAT_OPPONENT);
		combatAreaEffects.add (affectsOpponent);

		// Buildings
		final List<Building> buildings = new ArrayList<Building> ();

		final Building animistsGuild = new Building ();
		animistsGuild.setBuildingID (ANIMISTS_GUILD);
		animistsGuild.setBuildingUnrestReduction (1);
		animistsGuild.setProductionCost (300);
		buildings.add (animistsGuild);

		final Building shipWrightsGuild = new Building ();
		shipWrightsGuild.setBuildingID (SHIP_WRIGHTS_GUILD);
		final BuildingRequiresTileType buildingRequiresRiver = new BuildingRequiresTileType ();
		buildingRequiresRiver.setTileTypeID (RIVER_TILE);
		buildingRequiresRiver.setDistance (1);
		shipWrightsGuild.getBuildingRequiresTileType ().add (buildingRequiresRiver);
		buildings.add (shipWrightsGuild);

		final Building sawmill = new Building ();
		sawmill.setBuildingID (SAWMILL);
		final BuildingPopulationProductionModifier sawmillMaintenance = new BuildingPopulationProductionModifier ();
		sawmillMaintenance.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		sawmillMaintenance.setDoubleAmount (-4);
		sawmill.getBuildingPopulationProductionModifier ().add (sawmillMaintenance);
		final BuildingPopulationProductionModifier sawmillProductionBoost = new BuildingPopulationProductionModifier ();
		sawmillProductionBoost.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
		sawmillProductionBoost.setPercentageBonus (25);
		sawmill.getBuildingPopulationProductionModifier ().add (sawmillProductionBoost);
		buildings.add (sawmill);

		final Building sagesGuild = new Building ();
		sagesGuild.setBuildingID (SAGES_GUILD);
		final BuildingPopulationProductionModifier sagesGuildMaintenance = new BuildingPopulationProductionModifier ();
		sagesGuildMaintenance.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		sagesGuildMaintenance.setDoubleAmount (-4);
		sagesGuild.getBuildingPopulationProductionModifier ().add (sagesGuildMaintenance);
		final BuildingPopulationProductionModifier sagesGuildResearch = new BuildingPopulationProductionModifier ();
		sagesGuildResearch.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH);
		sagesGuildResearch.setDoubleAmount (6);
		sagesGuild.getBuildingPopulationProductionModifier ().add (sagesGuildResearch);
		buildings.add (sagesGuild);

		final Building alchemistsGuild = new Building ();
		alchemistsGuild.setBuildingID (ALCHEMISTS_GUILD);
		alchemistsGuild.setBuildingMagicWeapons (1);
		buildings.add (alchemistsGuild);

		final Building shrine = new Building ();
		shrine.setBuildingID (SHRINE);
		shrine.setBuildingUnrestReduction (1);
		shrine.setBuildingUnrestReductionImprovedByRetorts (true);
		buildings.add (shrine);

		final Building temple = new Building ();
		temple.setBuildingID (TEMPLE);
		temple.setBuildingUnrestReduction (1);
		temple.setBuildingUnrestReductionImprovedByRetorts (true);
		buildings.add (temple);

		final Building granary = new Building ();
		granary.setBuildingID (GRANARY);
		granary.setGrowthRateBonus (20);
		buildings.add (granary);

		final Building farmersMarket = new Building ();
		farmersMarket.setBuildingID (FARMERS_MARKET);
		farmersMarket.setGrowthRateBonus (30);
		buildings.add (farmersMarket);

		final Building minersGuild = new Building ();
		minersGuild.setBuildingID (MINERS_GUILD);
		final BuildingRequiresTileType buildingRequiresMountains = new BuildingRequiresTileType ();
		buildingRequiresMountains.setTileTypeID (MOUNTAINS_TILE);
		buildingRequiresMountains.setDistance (2);
		minersGuild.getBuildingRequiresTileType ().add (buildingRequiresMountains);
		final BuildingRequiresTileType buildingRequiresHills = new BuildingRequiresTileType ();
		buildingRequiresHills.setTileTypeID (HILLS_TILE);
		buildingRequiresHills.setDistance (2);
		minersGuild.getBuildingRequiresTileType ().add (buildingRequiresHills);
		final BuildingPopulationProductionModifier minersGuildMaintenance = new BuildingPopulationProductionModifier ();
		minersGuildMaintenance.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		minersGuildMaintenance.setDoubleAmount (-6);
		minersGuild.getBuildingPopulationProductionModifier ().add (minersGuildMaintenance);
		final BuildingPopulationProductionModifier minersGuildProductionBoost = new BuildingPopulationProductionModifier ();
		minersGuildProductionBoost.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
		minersGuildProductionBoost.setPercentageBonus (50);
		minersGuild.getBuildingPopulationProductionModifier ().add (minersGuildProductionBoost);
		final BuildingPopulationProductionModifier minersGuildMineralsBoost = new BuildingPopulationProductionModifier ();
		minersGuildMineralsBoost.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAP_FEATURE_MODIFIER);
		minersGuildMineralsBoost.setPercentageBonus (50);
		minersGuild.getBuildingPopulationProductionModifier ().add (minersGuildMineralsBoost);
		buildings.add (minersGuild);

		final Building fortress = new Building ();
		fortress.setBuildingID (CommonDatabaseConstants.BUILDING_FORTRESS);
		buildings.add (fortress);

		// Races
		final List<Race> races = new ArrayList<Race> ();

		final Race barbarian = new Race ();
		barbarian.setRaceID (BARBARIAN);
		barbarian.setGrowthRateModifier (20);
		barbarian.setMineralBonusMultiplier (1);
		races.add (barbarian);

		final Race highElf = new Race ();
		highElf.setRaceID (HIGH_ELF);
		highElf.setGrowthRateModifier (-20);
		highElf.setMineralBonusMultiplier (1);

		final RacePopulationTask highElfFarmers = new RacePopulationTask ();
		highElfFarmers.setPopulationTaskID (CommonDatabaseConstants.POPULATION_TASK_ID_FARMER);
		final RacePopulationTaskProduction highElfFarmersRations = new RacePopulationTaskProduction ();
		highElfFarmersRations.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS);
		highElfFarmersRations.setDoubleAmount (4);
		highElfFarmers.getRacePopulationTaskProduction ().add (highElfFarmersRations);
		final RacePopulationTaskProduction highElfFarmersProduction = new RacePopulationTaskProduction ();
		highElfFarmersProduction.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
		highElfFarmersProduction.setDoubleAmount (1);
		highElfFarmers.getRacePopulationTaskProduction ().add (highElfFarmersProduction);
		final RacePopulationTaskProduction highElfFarmersMagicPower = new RacePopulationTaskProduction ();
		highElfFarmersMagicPower.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER);
		highElfFarmersMagicPower.setDoubleAmount (1);
		highElfFarmers.getRacePopulationTaskProduction ().add (highElfFarmersMagicPower);
		highElf.getRacePopulationTask ().add (highElfFarmers);

		final RacePopulationTask highElfWorkers = new RacePopulationTask ();
		highElfWorkers.setPopulationTaskID (CommonDatabaseConstants.POPULATION_TASK_ID_WORKER);
		final RacePopulationTaskProduction highElfWorkersProduction = new RacePopulationTaskProduction ();
		highElfWorkersProduction.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
		highElfWorkersProduction.setDoubleAmount (4);
		highElfWorkers.getRacePopulationTaskProduction ().add (highElfWorkersProduction);
		final RacePopulationTaskProduction highElfWorkersMagicPower = new RacePopulationTaskProduction ();
		highElfWorkersMagicPower.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER);
		highElfWorkersMagicPower.setDoubleAmount (1);
		highElfWorkers.getRacePopulationTaskProduction ().add (highElfWorkersMagicPower);
		highElf.getRacePopulationTask ().add (highElfWorkers);

		final RacePopulationTask highElfRebels = new RacePopulationTask ();
		highElfRebels.setPopulationTaskID (CommonDatabaseConstants.POPULATION_TASK_ID_REBEL);
		final RacePopulationTaskProduction highElfRebelsMagicPower = new RacePopulationTaskProduction ();
		highElfRebelsMagicPower.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER);
		highElfRebelsMagicPower.setDoubleAmount (1);
		highElfRebels.getRacePopulationTaskProduction ().add (highElfRebelsMagicPower);
		highElf.getRacePopulationTask ().add (highElfRebels);

		final RaceUnrest highElvesDwarves = new RaceUnrest ();
		highElvesDwarves.setCapitalRaceID (DWARVES);
		highElvesDwarves.setUnrestPercentage (30);
		highElf.getRaceUnrest ().add (highElvesDwarves);
		races.add (highElf);

		final Race highMen = new Race ();
		highMen.setRaceID (HIGH_MEN);
		highMen.setMineralBonusMultiplier (1);

		final RacePopulationTask highMenFarmers = new RacePopulationTask ();
		highMenFarmers.setPopulationTaskID (CommonDatabaseConstants.POPULATION_TASK_ID_FARMER);
		final RacePopulationTaskProduction highMenFarmersRations = new RacePopulationTaskProduction ();
		highMenFarmersRations.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS);
		highMenFarmersRations.setDoubleAmount (4);
		highMenFarmers.getRacePopulationTaskProduction ().add (highMenFarmersRations);
		final RacePopulationTaskProduction highMenFarmersProduction = new RacePopulationTaskProduction ();
		highMenFarmersProduction.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
		highMenFarmersProduction.setDoubleAmount (1);
		highMenFarmers.getRacePopulationTaskProduction ().add (highMenFarmersProduction);
		highMen.getRacePopulationTask ().add (highMenFarmers);

		final RacePopulationTask highMenWorkers = new RacePopulationTask ();
		highMenWorkers.setPopulationTaskID (CommonDatabaseConstants.POPULATION_TASK_ID_WORKER);
		final RacePopulationTaskProduction highMenWorkersProduction = new RacePopulationTaskProduction ();
		highMenWorkersProduction.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
		highMenWorkersProduction.setDoubleAmount (4);
		highMenWorkers.getRacePopulationTaskProduction ().add (highMenWorkersProduction);
		highMen.getRacePopulationTask ().add (highMenWorkers);

		races.add (highMen);

		final Race klackons = new Race ();
		klackons.setRaceID (KLACKONS);
		klackons.setMineralBonusMultiplier (1);
		final RaceUnrest klackonsUnrest = new RaceUnrest ();
		klackonsUnrest.setCapitalRaceID (KLACKONS);
		klackonsUnrest.setUnrestLiteral (-2);
		klackons.getRaceUnrest ().add (klackonsUnrest);
		races.add (klackons);

		final Race dwarves = new Race ();
		dwarves.setRaceID (DWARVES);
		dwarves.setMineralBonusMultiplier (2);

		final RacePopulationTask dwarvesFarmers = new RacePopulationTask ();
		dwarvesFarmers.setPopulationTaskID (CommonDatabaseConstants.POPULATION_TASK_ID_FARMER);
		final RacePopulationTaskProduction dwarvesFarmersRations = new RacePopulationTaskProduction ();
		dwarvesFarmersRations.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS);
		dwarvesFarmersRations.setDoubleAmount (4);
		dwarvesFarmers.getRacePopulationTaskProduction ().add (dwarvesFarmersRations);
		final RacePopulationTaskProduction dwarvesFarmersProduction = new RacePopulationTaskProduction ();
		dwarvesFarmersProduction.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
		dwarvesFarmersProduction.setDoubleAmount (1);
		dwarvesFarmers.getRacePopulationTaskProduction ().add (dwarvesFarmersProduction);
		dwarves.getRacePopulationTask ().add (dwarvesFarmers);

		final RacePopulationTask dwarvesWorkers = new RacePopulationTask ();
		dwarvesWorkers.setPopulationTaskID (CommonDatabaseConstants.POPULATION_TASK_ID_WORKER);
		final RacePopulationTaskProduction dwarvesWorkersProduction = new RacePopulationTaskProduction ();
		dwarvesWorkersProduction.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
		dwarvesWorkersProduction.setDoubleAmount (6);
		dwarvesWorkers.getRacePopulationTaskProduction ().add (dwarvesWorkersProduction);
		dwarves.getRacePopulationTask ().add (dwarvesWorkers);

		races.add (dwarves);

		// Tax rates
		final List<TaxRate> taxRates = new ArrayList<TaxRate> ();

		final TaxRate tr01 = new TaxRate ();
		tr01.setTaxRateID (TAX_RATE_0_GOLD_0_UNREST);
		taxRates.add (tr01);

		final TaxRate tr05 = new TaxRate ();
		tr05.setTaxRateID (TAX_RATE_2_GOLD_45_UNREST);
		tr05.setDoubleTaxGold (4);
		tr05.setTaxUnrestPercentage (45);
		taxRates.add (tr05);

		final TaxRate tr07 = new TaxRate ();
		tr07.setTaxRateID (TAX_RATE_3_GOLD_75_UNREST);
		tr07.setDoubleTaxGold (6);
		tr07.setTaxUnrestPercentage (75);
		taxRates.add (tr07);
		
		// Combat tiles
		final List<CombatTileType> combatTileTypes = new ArrayList<CombatTileType> ();
		
		final CombatTileType ctl01 = new CombatTileType ();
		ctl01.setCombatTileTypeID ("CTL01");
		ctl01.setDoubleMovement (2);
		combatTileTypes.add (ctl01);

		final CombatTileType crl03 = new CombatTileType ();
		crl03.setCombatTileTypeID ("CRL03");
		crl03.setDoubleMovement (1);
		combatTileTypes.add (crl03);
		
		final CombatTileType cbl01 = new CombatTileType ();
		cbl01.setCombatTileTypeID ("CBL01");
		combatTileTypes.add (cbl01);

		final CombatTileType cbl03 = new CombatTileType ();
		cbl03.setCombatTileTypeID ("CBL03");
		cbl03.setDoubleMovement (-1);
		combatTileTypes.add (cbl03);
		
		// Combat tile borders
		final List<CombatTileBorder> combatTileBorders = new ArrayList<CombatTileBorder> ();
		
		final CombatTileBorder ctb01 = new CombatTileBorder ();
		ctb01.setCombatTileBorderID ("CTB01");
		ctb01.setBlocksMovement (CombatTileBorderBlocksMovementID.CANNOT_CROSS_SPECIFIED_BORDERS);
		combatTileBorders.add (ctb01);

		final CombatTileBorder ctb02 = new CombatTileBorder ();
		ctb02.setCombatTileBorderID ("CTB02");
		ctb02.setBlocksMovement (CombatTileBorderBlocksMovementID.WHOLE_TILE_IMPASSABLE);
		combatTileBorders.add (ctb02);

		final CombatTileBorder ctb03 = new CombatTileBorder ();
		ctb03.setCombatTileBorderID ("CTB03");
		combatTileBorders.add (ctb03);
		
		// Now have all the lists, mock the results accordingly
		final CommonDatabase db = mock (CommonDatabase.class);
		for (final Plane plane : planes)
			when (db.findPlane (eq (plane.getPlaneNumber ()), anyString ())).thenReturn (plane);

		for (final MapFeature mapFeature : mapFeatures)
			when (db.findMapFeature (eq (mapFeature.getMapFeatureID ()), anyString ())).thenReturn (mapFeature);

		for (final TileType tileType : tileTypes)
			when (db.findTileType (eq (tileType.getTileTypeID ()), anyString ())).thenReturn (tileType);
		
		for (final ProductionType productionType : productionTypes)
			when (db.findProductionType (eq (productionType.getProductionTypeID ()), anyString ())).thenReturn (productionType);

		doReturn (pickTypes).when (db).getPickType ();
		for (final PickType pickType : pickTypes)
			when (db.findPickType (eq (pickType.getPickTypeID ()), anyString ())).thenReturn (pickType);

		for (final Pick pick : picks)
			when (db.findPick (eq (pick.getPickID ()), anyString ())).thenReturn (pick);

		for (final UnitType unitType : unitTypes)
			when (db.findUnitType (eq (unitType.getUnitTypeID ()), anyString ())).thenReturn (unitType);

		for (final UnitMagicRealm unitMagicRealm : unitMagicRealms)
			when (db.findUnitMagicRealm (eq (unitMagicRealm.getUnitMagicRealmID ()), anyString ())).thenReturn (unitMagicRealm);

		for (final Unit unit : units)
			when (db.findUnit (eq (unit.getUnitID ()), anyString ())).thenReturn (unit);

		doReturn (unitSkills).when (db).getUnitSkill ();
		for (final UnitSkill unitSkill : unitSkills)
			when (db.findUnitSkill (eq (unitSkill.getUnitSkillID ()), anyString ())).thenReturn (unitSkill);

		for (final WeaponGrade weaponGrade : weaponGrades)
			when (db.findWeaponGrade (eq (weaponGrade.getWeaponGradeNumber ()), anyString ())).thenReturn (weaponGrade);

		for (final RangedAttackType rangedAttackType : rangedAttackTypes)
			when (db.findRangedAttackType (eq (rangedAttackType.getRangedAttackTypeID ()), anyString ())).thenReturn (rangedAttackType);
		
		for (final Race race : races)
			when (db.findRace (eq (race.getRaceID ()), anyString ())).thenReturn (race);

		for (final TaxRate taxRate : taxRates)
			when (db.findTaxRate (eq (taxRate.getTaxRateID ()), anyString ())).thenReturn (taxRate);

		doReturn (buildings).when (db).getBuilding ();
		for (final Building building : buildings)
			when (db.findBuilding (eq (building.getBuildingID ()), anyString ())).thenReturn (building);

		doReturn (spells).when (db).getSpell ();
		for (final Spell spell : spells)
			when (db.findSpell (eq (spell.getSpellID ()), anyString ())).thenReturn (spell);

		for (final CombatAreaEffect combatAreaEffect : combatAreaEffects)
			when (db.findCombatAreaEffect (eq (combatAreaEffect.getCombatAreaEffectID ()), anyString ())).thenReturn (combatAreaEffect);

		for (final CombatTileType combatTileType : combatTileTypes)
			when (db.findCombatTileType (eq (combatTileType.getCombatTileTypeID ()), anyString ())).thenReturn (combatTileType);

		for (final CombatTileBorder combatTileBorder : combatTileBorders)
			when (db.findCombatTileBorder (eq (combatTileBorder.getCombatTileBorderID ()), anyString ())).thenReturn (combatTileBorder);
		
		return db;
	}

	/**
	 *	<spellSetting spellSettingID="SS01">
	 *		<switchResearch>D</switchResearch>
	 *		<spellBooksToObtainFirstReduction>8</spellBooksToObtainFirstReduction>
	 *		<spellBooksCastingReduction>10</spellBooksCastingReduction>
	 *		<spellBooksCastingReductionCap>100</spellBooksCastingReductionCap>
	 *		<spellBooksCastingReductionCombination>A</spellBooksCastingReductionCombination>
	 *		<spellBooksResearchBonus>10</spellBooksResearchBonus>
	 *		<spellBooksResearchBonusCap>1000</spellBooksResearchBonusCap>
	 *		<spellBooksResearchBonusCombination>A</spellBooksResearchBonusCombination>
	 *		<spellSettingDescription>Original</spellSettingDescription>
	 *	</spellSetting>
	 *
	 * @return Spell settings, configured like the original spell settings in the server XML file
	 */
	public final static SpellSettingData createOriginalSpellSettings ()
	{
		final SpellSettingData settings = new SpellSettingData ();
		settings.setSwitchResearch (SwitchResearch.DISALLOWED);
		settings.setSpellBooksToObtainFirstReduction (8);
		settings.setSpellBooksCastingReduction (10);
		settings.setSpellBooksCastingReductionCap (100);
		settings.setSpellBooksCastingReductionCombination (CastingReductionCombination.ADDITIVE);
		settings.setSpellBooksResearchBonus (10);
		settings.setSpellBooksResearchBonusCap (1000);
		settings.setSpellBooksResearchBonusCombination (CastingReductionCombination.ADDITIVE);
		return settings;
	}

	/**
	 *	<spellSetting spellSettingID="SS02">
	 *		<switchResearch>F</switchResearch>
	 *		<spellBooksToObtainFirstReduction>8</spellBooksToObtainFirstReduction>
	 *		<spellBooksCastingReduction>8</spellBooksCastingReduction>
	 *		<spellBooksCastingReductionCap>90</spellBooksCastingReductionCap>
	 *		<spellBooksCastingReductionCombination>M</spellBooksCastingReductionCombination>
	 *		<spellBooksResearchBonus>10</spellBooksResearchBonus>
	 *		<spellBooksResearchBonusCap>1000</spellBooksResearchBonusCap>
	 *		<spellBooksResearchBonusCombination>A</spellBooksResearchBonusCombination>
	 *		<spellSettingDescription>Recommended</spellSettingDescription>
	 *	</spellSetting>

	 * @return Spell settings, configured like the recommended spell settings in the server XML file
	 */
	public final static SpellSettingData createRecommendedSpellSettings ()
	{
		final SpellSettingData settings = new SpellSettingData ();
		settings.setSwitchResearch (SwitchResearch.FREE);
		settings.setSpellBooksToObtainFirstReduction (8);
		settings.setSpellBooksCastingReduction (8);
		settings.setSpellBooksCastingReductionCap (90);
		settings.setSpellBooksCastingReductionCombination (CastingReductionCombination.MULTIPLICATIVE);
		settings.setSpellBooksResearchBonus (10);
		settings.setSpellBooksResearchBonusCap (1000);
		settings.setSpellBooksResearchBonusCombination (CastingReductionCombination.ADDITIVE);
		return settings;
	}

	/**
	 * @return Sample Arcane normal spell
	 */
	public final static Spell createArcaneNormalSpell ()
	{
		final Spell spell = new Spell ();
		return spell;
	}

	/**
	 * @return Sample Arcane summoning spell
	 */
	public final static Spell createArcaneSummoningSpell ()
	{
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);

		final SummonedUnit unit = new SummonedUnit ();
		unit.setSummonedUnitID (MAGIC_SPIRIT_UNIT);
		spell.getSummonedUnit ().add (unit);

		return spell;
	}

	/**
	 * @return Sample Nature normal spell
	 */
	public final static Spell createNatureNormalSpell ()
	{
		final Spell spell = new Spell ();
		spell.setSpellRealm (NATURE_BOOK);
		return spell;
	}

	/**
	 * @return Sample Nature summoning spell
	 */
	public final static Spell createNatureSummoningSpell ()
	{
		final Spell spell = new Spell ();
		spell.setSpellRealm (NATURE_BOOK);
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);

		final SummonedUnit unit = new SummonedUnit ();
		unit.setSummonedUnitID (WAR_BEARS_UNIT);
		spell.getSummonedUnit ().add (unit);

		return spell;
	}

	/**
	 * @return Sample Chaos normal spell
	 */
	public final static Spell createChaosNormalSpell ()
	{
		final Spell spell = new Spell ();
		spell.setSpellRealm (CHAOS_BOOK);
		return spell;
	}

	/**
	 * @return Sample Chaos summoning spell
	 */
	public final static Spell createChaosSummoningSpell ()
	{
		final Spell spell = new Spell ();
		spell.setSpellRealm (CHAOS_BOOK);
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);

		final SummonedUnit unit = new SummonedUnit ();
		unit.setSummonedUnitID (HELL_HOUNDS_UNIT);
		spell.getSummonedUnit ().add (unit);

		return spell;
	}

	/**
	 * @return Sample Arcane summoning spell
	 */
	public final static Spell createSummonHeroSpell ()
	{
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);

		final SummonedUnit unit = new SummonedUnit ();
		unit.setSummonedUnitID (DWARF_HERO);
		spell.getSummonedUnit ().add (unit);

		return spell;
	}

	/**
	 * @return Demo MoM overland map-like coordinate system with a 60x40 square map wrapping left-to-right but not top-to-bottom
	 */
	public final static CoordinateSystem createOverlandMapCoordinateSystem ()
	{
		final CoordinateSystem sys = new CoordinateSystem ();
		sys.setCoordinateSystemType (CoordinateSystemType.SQUARE);
		sys.setWidth (60);
		sys.setHeight (40);
		sys.setWrapsLeftToRight (true);
		return sys;
	}

	/**
	 * @return Overland map coordinate system that can be included into session description
	 */
	public final static MapSizeData createMapSizeData ()
	{
		final MapSizeData sys = new MapSizeData ();
		sys.setCoordinateSystemType (CoordinateSystemType.SQUARE);
		sys.setWidth (60);
		sys.setHeight (40);
		sys.setDepth (2);
		sys.setWrapsLeftToRight (true);
		
		sys.setCitySeparation (3);
		sys.setContinentalRaceChance (75);
		
		return sys;
	}
	
	/**
	 * @param sys Overland map coordinate system
	 * @return Map area prepopulated with empty cells
	 */
	public final static MapVolumeOfMemoryGridCells createOverlandMap (final CoordinateSystem sys)
	{
		final MapVolumeOfMemoryGridCells map = new MapVolumeOfMemoryGridCells ();
		for (int plane = 0; plane < 2; plane++)
		{
			final MapAreaOfMemoryGridCells area = new MapAreaOfMemoryGridCells ();
			for (int y = 0; y < sys.getHeight (); y++)
			{
				final MapRowOfMemoryGridCells row = new MapRowOfMemoryGridCells ();
				for (int x = 0; x < sys.getWidth (); x++)
					row.getCell ().add (new MemoryGridCell ());

				area.getRow ().add (row);
			}

			map.getPlane ().add (area);
		}

		return map;
	}

	/**
	 * @return Demo MoM combat map-like coordinate system with a 60x40 diamond non-wrapping map
	 */
	public final static CoordinateSystem createCombatMapCoordinateSystem ()
	{
		final CoordinateSystem sys = new CoordinateSystem ();
		sys.setCoordinateSystemType (CoordinateSystemType.DIAMOND);
		sys.setWidth (12);
		sys.setHeight (25);
		return sys;
	}

	/**
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @return Map area prepopulated with empty cells
	 */
	public final static MapAreaOfCombatTiles createCombatMap (final CoordinateSystem combatMapCoordinateSystem)
	{
		final MapAreaOfCombatTiles map = new MapAreaOfCombatTiles ();
		for (int y = 0; y < combatMapCoordinateSystem.getHeight (); y++)
		{
			final MapRowOfCombatTiles row = new MapRowOfCombatTiles ();
			for (int x = 0; x < combatMapCoordinateSystem.getWidth (); x++)
				row.getCell ().add (new MomCombatTile ());

			map.getRow ().add (row);
		}

		return map;
	}

	/**
	 * Prevent instantiation
	 */
	private GenerateTestData ()
	{
	}
}