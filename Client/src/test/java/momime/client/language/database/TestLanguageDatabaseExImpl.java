package momime.client.language.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import momime.common.database.Shortcut;
import momime.common.database.SpellBookSectionID;

/**
 * Tests the LanguageDatabaseExImpl class
 */
public final class TestLanguageDatabaseExImpl
{
	/**
	 * Tests the findPlane method
	 */
	@Test
	public final void testFindPlane ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final PlaneLang newPlane = new PlaneLang ();
			newPlane.setPlaneNumber(n);
			newPlane.setPlaneDescription ("PLDesc0" + n);
			lang.getPlane ().add (newPlane);
		}

		lang.buildMaps ();

		assertEquals ("PLDesc02", lang.findPlane (2).getPlaneDescription ());
		assertNull (lang.findPlane (4));
	}

	/**
	 * Tests the findProductionType method
	 */
	@Test
	public final void testFindProductionType ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final ProductionTypeLang newProductionType = new ProductionTypeLang ();
			newProductionType.setProductionTypeID ("RE0" + n);
			newProductionType.setProductionTypeDescription ("REDesc0" + n);
			lang.getProductionType ().add (newProductionType);
		}

		lang.buildMaps ();

		assertEquals ("REDesc02", lang.findProductionType ("RE02").getProductionTypeDescription ());
		assertNull (lang.findProductionType ("RE04"));
	}

	/**
	 * Tests the findMapFeature method
	 */
	@Test
	public final void testFindMapFeature ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final MapFeatureLang newMapFeature = new MapFeatureLang ();
			newMapFeature.setMapFeatureID ("MF0" + n);
			newMapFeature.setMapFeatureDescription ("MFDesc0" + n);
			lang.getMapFeature ().add (newMapFeature);
		}

		lang.buildMaps ();

		assertEquals ("MFDesc02", lang.findMapFeature ("MF02").getMapFeatureDescription ());
		assertNull (lang.findMapFeature ("MF04"));
	}

	/**
	 * Tests the findTileType method
	 */
	@Test
	public final void testFindTileType ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final TileTypeLang newTileType = new TileTypeLang ();
			newTileType.setTileTypeID ("TT0" + n);
			newTileType.setTileTypeDescription ("TTDesc0" + n);
			lang.getTileType ().add (newTileType);
		}

		lang.buildMaps ();

		assertEquals ("TTDesc02", lang.findTileType ("TT02").getTileTypeDescription ());
		assertNull (lang.findTileType ("TT04"));
	}

	/**
	 * Tests the findPickType method
	 */
	@Test
	public final void testFindPickType ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final PickTypeLang newPickType = new PickTypeLang ();
			newPickType.setPickTypeID ("PT0" + n);
			newPickType.setPickTypeDescriptionSingular ("PTDesc0" + n);
			lang.getPickType ().add (newPickType);
		}

		lang.buildMaps ();

		assertEquals ("PTDesc02", lang.findPickType ("PT02").getPickTypeDescriptionSingular ());
		assertNull (lang.findPickType ("PT04"));
	}
	
	/**
	 * Tests the findPick method
	 */
	@Test
	public final void testFindPick ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final PickLang newPick = new PickLang ();
			newPick.setPickID ("MB0" + n);
			newPick.setPickDescriptionSingular ("MBDesc0" + n);
			lang.getPick ().add (newPick);
		}

		lang.buildMaps ();

		assertEquals ("MBDesc02", lang.findPick ("MB02").getPickDescriptionSingular ());
		assertNull (lang.findPick ("MB04"));
	}

	/**
	 * Tests the findWizardName method
	 */
	@Test
	public final void testFindWizardName ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final WizardLang newWizard = new WizardLang ();
			newWizard.setWizardID ("WZ0" + n);
			newWizard.setWizardName ("WZDesc0" + n);
			lang.getWizard ().add (newWizard);
		}

		lang.buildMaps ();

		assertEquals ("WZDesc02", lang.findWizardName ("WZ02"));
		assertEquals ("WZ04", lang.findWizardName ("WZ04"));
	}

	/**
	 * Tests the findPopulationTask method
	 */
	@Test
	public final void testFindPopulationTask ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final PopulationTaskLang newPopulationTask = new PopulationTaskLang ();
			newPopulationTask.setPopulationTaskID ("PT0" + n);
			newPopulationTask.setPopulationTaskSingular ("PTSingle0" + n);
			lang.getPopulationTask ().add (newPopulationTask);
		}

		lang.buildMaps ();

		assertEquals ("PTSingle02", lang.findPopulationTask ("PT02").getPopulationTaskSingular ());
		assertNull (lang.findPopulationTask ("PT04"));
	}
	
	/**
	 * Tests the findRace method
	 */
	@Test
	public final void testFindRace ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final RaceLang newRace = new RaceLang ();
			newRace.setRaceID ("RC0" + n);
			newRace.setRaceName ("RCDesc0" + n);
			lang.getRace ().add (newRace);
		}

		lang.buildMaps ();

		assertEquals ("RCDesc02", lang.findRace ("RC02").getRaceName ());
		assertNull (lang.findRace ("RC04"));
	}
	
	/**
	 * Tests the findBuilding method
	 */
	@Test
	public final void testFindBuilding ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final BuildingLang newBuilding = new BuildingLang ();
			newBuilding.setBuildingID ("BL0" + n);
			newBuilding.setBuildingName ("BLDesc0" + n);
			lang.getBuilding ().add (newBuilding);
		}

		lang.buildMaps ();

		assertEquals ("BLDesc02", lang.findBuilding ("BL02").getBuildingName ());
		assertNull (lang.findBuilding ("BL04"));
	}

	/**
	 * Tests the findUnitType method
	 */
	@Test
	public final void testFindUnitType ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitTypeLang newUnitType = new UnitTypeLang ();
			newUnitType.setUnitTypeID ("UT0" + n);
			newUnitType.setUnitTypeExperienced ("UTDesc0" + n);		// There's no "description" field, so just using any other text field to test with
			lang.getUnitType ().add (newUnitType);
		}

		lang.buildMaps ();

		assertEquals ("UTDesc02", lang.findUnitType ("UT02").getUnitTypeExperienced ());
		assertNull (lang.findUnitType ("UT04"));
	}
	
	/**
	 * Tests the findUnitSkill method
	 */
	@Test
	public final void testFindUnitSkill ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitSkillLang newUnitSkill = new UnitSkillLang ();
			newUnitSkill.setUnitSkillID ("US0" + n);
			newUnitSkill.setUnitSkillDescription ("USDesc0" + n);
			lang.getUnitSkill ().add (newUnitSkill);
		}

		lang.buildMaps ();

		assertEquals ("USDesc02", lang.findUnitSkill ("US02").getUnitSkillDescription ());
		assertNull (lang.findUnitSkill ("US04"));
	}

	/**
	 * Tests the findRangedAttackTypeDescription method
	 */
	@Test
	public final void testFindRangedAttackTypeDescription ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final RangedAttackTypeLang newRangedAttackType = new RangedAttackTypeLang ();
			newRangedAttackType.setRangedAttackTypeID ("RAT0" + n);
			newRangedAttackType.setRangedAttackTypeDescription ("RATDesc0" + n);
			lang.getRangedAttackType ().add (newRangedAttackType);
		}

		lang.buildMaps ();

		assertEquals ("RATDesc02", lang.findRangedAttackTypeDescription ("RAT02"));
		assertEquals ("RAT04", lang.findRangedAttackTypeDescription ("RAT04"));
	}
	
	/**
	 * Tests the findDamageTypeDescription method
	 */
	@Test
	public final void testFindDamageTypeDescription ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final DamageTypeLang newDamageType = new DamageTypeLang ();
			newDamageType.setDamageTypeID ("DT0" + n);
			newDamageType.setDamageTypeName ("DTDesc0" + n);
			lang.getDamageType ().add (newDamageType);
		}

		lang.buildMaps ();

		assertEquals ("DTDesc02", lang.findDamageTypeName ("DT02"));
		assertEquals ("DT04", lang.findDamageTypeName ("DT04"));
	}
	
	/**
	 * Tests the findUnit method
	 */
	@Test
	public final void testFindUnit ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitLang newUnit = new UnitLang ();
			newUnit.setUnitID ("UN00" + n);
			newUnit.setUnitName ("UNDesc0" + n);
			lang.getUnit ().add (newUnit);
		}

		lang.buildMaps ();

		assertEquals ("UNDesc02", lang.findUnit ("UN002").getUnitName ());
		assertNull (lang.findUnit ("UN004"));
	}

	/**
	 * Tests the findHeroName method
	 */
	@Test
	public final void testFindHeroName ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final HeroLang newHeroName = new HeroLang ();
			newHeroName.setHeroNameID ("HN0" + n);
			newHeroName.setHeroName ("HNDesc0" + n);
			lang.getHero ().add (newHeroName);
		}

		lang.buildMaps ();

		assertEquals ("HNDesc02", lang.findHeroName ("HN02"));
		assertEquals ("HN04", lang.findHeroName ("HN04"));
	}
	
	/**
	 * Tests the findCitySizeName method
	 */
	@Test
	public final void testFindCitySizeName ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CitySizeLang newCitySize = new CitySizeLang ();
			newCitySize.setCitySizeID ("CS0" + n);
			newCitySize.setCitySizeName ("CSDesc0" + n);
			lang.getCitySize ().add (newCitySize);
		}

		lang.buildMaps ();

		assertEquals ("CSDesc02", lang.findCitySizeName ("CS02"));
		assertEquals ("CS04", lang.findCitySizeName ("CS04"));
	}
	
	/**
	 * Tests the findCitySpellEffect method
	 */
	@Test
	public final void testFindCitySpellEffect ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CitySpellEffectLang newCitySpellEffect = new CitySpellEffectLang ();
			newCitySpellEffect.setCitySpellEffectID ("CSE00" + n);
			newCitySpellEffect.setCitySpellEffectName ("CSEDesc0" + n);
			lang.getCitySpellEffect ().add (newCitySpellEffect);
		}

		lang.buildMaps ();

		assertEquals ("CSEDesc02", lang.findCitySpellEffect ("CSE002").getCitySpellEffectName ());
		assertNull (lang.findCitySpellEffect ("CSE004"));
	}

	/**
	 * Tests the findCombatAreaEffect method
	 */
	@Test
	public final void testFindCombatAreaEffect ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CombatAreaEffectLang newCombatAreaEffect = new CombatAreaEffectLang ();
			newCombatAreaEffect.setCombatAreaEffectID ("CAE00" + n);
			newCombatAreaEffect.setCombatAreaEffectDescription ("CAEDesc0" + n);
			lang.getCombatAreaEffect ().add (newCombatAreaEffect);
		}

		lang.buildMaps ();

		assertEquals ("CAEDesc02", lang.findCombatAreaEffect ("CAE002").getCombatAreaEffectDescription ());
		assertNull (lang.findCombatAreaEffect ("CAE004"));
	}
	
	/**
	 * Tests the findSpellRankDescription method
	 */
	@Test
	public final void testFindSpellRankDescription ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final SpellRankLang newSpellRank = new SpellRankLang ();
			newSpellRank.setSpellRankID ("SR0" + n);
			newSpellRank.setSpellRankDescription ("SRDesc0" + n);
			lang.getSpellRank ().add (newSpellRank);
		}

		lang.buildMaps ();

		assertEquals ("SRDesc02", lang.findSpellRankDescription ("SR02"));
		assertEquals ("SR04", lang.findSpellRankDescription ("SR04"));
	}

	/**
	 * Tests the findSpellBookSection method
	 */
	@Test
	public final void testFindSpellBookSection ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final SpellBookSectionLang newSection = new SpellBookSectionLang ();
			newSection.setSpellBookSectionID (SpellBookSectionID.fromValue ("SC0" + n));
			newSection.setSpellBookSectionName ("SCDesc0" + n);
			lang.getSpellBookSection ().add (newSection);
		}

		lang.buildMaps ();

		assertEquals ("SCDesc02", lang.findSpellBookSection (SpellBookSectionID.OVERLAND_ENCHANTMENTS).getSpellBookSectionName ());
		assertNull (lang.findSpellBookSection (SpellBookSectionID.UNIT_ENCHANTMENTS));
	}

	/**
	 * Tests the findSpell method
	 */
	@Test
	public final void testFindSpell ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final SpellLang newSpell = new SpellLang ();
			newSpell.setSpellID ("MB0" + n);
			newSpell.setSpellDescription ("MBDesc0" + n);
			lang.getSpell ().add (newSpell);
		}

		lang.buildMaps ();

		assertEquals ("MBDesc02", lang.findSpell ("MB02").getSpellDescription ());
		assertNull (lang.findSpell ("MB04"));
	}
	
	/**
	 * Tests the findHeroItemTypeDescription method
	 */
	@Test
	public final void testFindHeroItemTypeDescription ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final HeroItemTypeLang newMapSize = new HeroItemTypeLang ();
			newMapSize.setHeroItemTypeID ("IT0" + n);
			newMapSize.setHeroItemTypeDescription ("ITDesc0" + n);
			lang.getHeroItemType ().add (newMapSize);
		}

		lang.buildMaps ();

		assertEquals ("ITDesc02", lang.findHeroItemTypeDescription ("IT02"));
		assertEquals ("IT04", lang.findHeroItemTypeDescription ("IT04"));
	}
	
	/**
	 * Tests the findHeroItemSlotTypeDescription method
	 */
	@Test
	public final void testFindHeroItemSlotTypeDescription ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final HeroItemSlotTypeLang newMapSize = new HeroItemSlotTypeLang ();
			newMapSize.setHeroItemSlotTypeID ("IST0" + n);
			newMapSize.setSlotTypeDescription ("ISTDesc0" + n);
			lang.getHeroItemSlotType ().add (newMapSize);
		}

		lang.buildMaps ();

		assertEquals ("ISTDesc02", lang.findHeroItemSlotTypeDescription ("IST02"));
		assertEquals ("IST04", lang.findHeroItemSlotTypeDescription ("IST04"));
	}
	
	/**
	 * Tests the findHeroItemBonusDescription method
	 */
	@Test
	public final void testFindHeroItemBonusDescription ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final HeroItemBonusLang newMapSize = new HeroItemBonusLang ();
			newMapSize.setHeroItemBonusID ("IB0" + n);
			newMapSize.setHeroItemBonusDescription ("IBDesc0" + n);
			lang.getHeroItemBonus ().add (newMapSize);
		}

		lang.buildMaps ();

		assertEquals ("IBDesc02", lang.findHeroItemBonusDescription ("IB02"));
		assertEquals ("IB04", lang.findHeroItemBonusDescription ("IB04"));
	}

	/**
	 * Tests the findOverlandMapSizeDescription method
	 */
	@Test
	public final void testFindOverlandMapSizeDescription ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final OverlandMapSizeLang newMapSize = new OverlandMapSizeLang ();
			newMapSize.setOverlandMapSizeID ("MS0" + n);
			newMapSize.setOverlandMapSizeDescription ("MSDesc0" + n);
			lang.getOverlandMapSize ().add (newMapSize);
		}

		lang.buildMaps ();

		assertEquals ("MSDesc02", lang.findOverlandMapSizeDescription ("MS02"));
		assertEquals ("MS04", lang.findOverlandMapSizeDescription ("MS04"));
	}
	
	/**
	 * Tests the findLandProportionDescription method
	 */
	@Test
	public final void testFindLandProportionDescription ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final LandProportionLang newLandProportion = new LandProportionLang ();
			newLandProportion.setLandProportionID ("LP0" + n);
			newLandProportion.setLandProportionDescription ("LPDesc0" + n);
			lang.getLandProportion ().add (newLandProportion);
		}

		lang.buildMaps ();

		assertEquals ("LPDesc02", lang.findLandProportionDescription ("LP02"));
		assertEquals ("LP04", lang.findLandProportionDescription ("LP04"));
	}
	
	/**
	 * Tests the findNodeStrengthDescription method
	 */
	@Test
	public final void testFindNodeStrengthDescription ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final NodeStrengthLang newNodeStrength = new NodeStrengthLang ();
			newNodeStrength.setNodeStrengthID ("NS0" + n);
			newNodeStrength.setNodeStrengthDescription ("NSDesc0" + n);
			lang.getNodeStrength ().add (newNodeStrength);
		}

		lang.buildMaps ();

		assertEquals ("NSDesc02", lang.findNodeStrengthDescription ("NS02"));
		assertEquals ("NS04", lang.findNodeStrengthDescription ("NS04"));
	}
	
	/**
	 * Tests the findDifficultyLevelDescription method
	 */
	@Test
	public final void testFindDifficultyLevelDescription ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final DifficultyLevelLang newDifficultyLevel = new DifficultyLevelLang ();
			newDifficultyLevel.setDifficultyLevelID ("DL0" + n);
			newDifficultyLevel.setDifficultyLevelDescription ("DLDesc0" + n);
			lang.getDifficultyLevel ().add (newDifficultyLevel);
		}

		lang.buildMaps ();

		assertEquals ("DLDesc02", lang.findDifficultyLevelDescription ("DL02"));
		assertEquals ("DL04", lang.findDifficultyLevelDescription ("DL04"));
	}
	
	/**
	 * Tests the findFogOfWarSettingDescription method
	 */
	@Test
	public final void testFindFogOfWarSettingDescription ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final FogOfWarSettingLang newFogOfWarSetting = new FogOfWarSettingLang ();
			newFogOfWarSetting.setFogOfWarSettingID ("FS0" + n);
			newFogOfWarSetting.setFogOfWarSettingDescription ("FSDesc0" + n);
			lang.getFogOfWarSetting ().add (newFogOfWarSetting);
		}

		lang.buildMaps ();

		assertEquals ("FSDesc02", lang.findFogOfWarSettingDescription ("FS02"));
		assertEquals ("FS04", lang.findFogOfWarSettingDescription ("FS04"));
	}
	
	/**
	 * Tests the findUnitSettingDescription method
	 */
	@Test
	public final void testFindUnitSettingDescription ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitSettingLang newUnitSetting = new UnitSettingLang ();
			newUnitSetting.setUnitSettingID ("US0" + n);
			newUnitSetting.setUnitSettingDescription ("USDesc0" + n);
			lang.getUnitSetting ().add (newUnitSetting);
		}

		lang.buildMaps ();

		assertEquals ("USDesc02", lang.findUnitSettingDescription ("US02"));
		assertEquals ("US04", lang.findUnitSettingDescription ("US04"));
	}
	
	/**
	 * Tests the findSpellSettingDescription method
	 */
	@Test
	public final void testFindSpellSettingDescription ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final SpellSettingLang newSpellSetting = new SpellSettingLang ();
			newSpellSetting.setSpellSettingID ("SS0" + n);
			newSpellSetting.setSpellSettingDescription ("SSDesc0" + n);
			lang.getSpellSetting ().add (newSpellSetting);
		}

		lang.buildMaps ();

		assertEquals ("SSDesc02", lang.findSpellSettingDescription ("SS02"));
		assertEquals ("SS04", lang.findSpellSettingDescription ("SS04"));
	}
	
	/**
	 * Tests the findCategoryEntry method 
	 */
	@Test
	public final void testFindCategoryEntry ()
	{
		// Create some dummy categories and entries
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		
		for (int catNo = 1; catNo <= 3; catNo++)
		{
			final LanguageCategoryEx cat = new LanguageCategoryEx ();
			cat.setLanguageCategoryID ("C" + catNo);
			
			for (int entryNo = 1; entryNo <= 3; entryNo++)
			{
				final LanguageEntryEx entry = new LanguageEntryEx ();
				entry.setLanguageEntryID ("C" + catNo + "E" + entryNo);
				entry.setLanguageEntryText ("Blah" + catNo + entryNo);
				cat.getLanguageEntry ().add (entry);
			}			
			
			lang.getLanguageCategory ().add (cat);
		}
		
		lang.buildMaps ();

		// Entry exists
		assertEquals ("Blah22", lang.findCategoryEntry ("C2", "C2E2"));
		
		// Entry missing
		assertEquals ("C2/C2E4", lang.findCategoryEntry ("C2", "C2E4"));
		
		// Whole category missing
		assertEquals ("C4/C4E4", lang.findCategoryEntry ("C4", "C4E4"));
	}

	/**
	 * Tests the findShortcutKey method
	 */
	@Test
	public final void testFindShortcutKey ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 2; n++)
		{
			final ShortcutKeyLang newKey = new ShortcutKeyLang ();
			newKey.setNormalKey (new Integer (n).toString ());
			newKey.setShortcut ((n == 1) ? Shortcut.SPELLBOOK : Shortcut.OVERLAND_MOVE_DONE);
			lang.getShortcutKey ().add (newKey);
		}

		lang.buildMaps ();

		assertEquals ("2", lang.findShortcutKey (Shortcut.OVERLAND_MOVE_DONE).getNormalKey ());
		assertNull (lang.findShortcutKey (Shortcut.OVERLAND_MOVE_WAIT));
	}
}