package momime.client.language.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import momime.client.language.database.v0_9_5.Building;
import momime.client.language.database.v0_9_5.CitySize;
import momime.client.language.database.v0_9_5.CombatAreaEffect;
import momime.client.language.database.v0_9_5.DifficultyLevel;
import momime.client.language.database.v0_9_5.FogOfWarSetting;
import momime.client.language.database.v0_9_5.Hero;
import momime.client.language.database.v0_9_5.LandProportion;
import momime.client.language.database.v0_9_5.LanguageEntry;
import momime.client.language.database.v0_9_5.MapFeature;
import momime.client.language.database.v0_9_5.MapSize;
import momime.client.language.database.v0_9_5.NodeStrength;
import momime.client.language.database.v0_9_5.Pick;
import momime.client.language.database.v0_9_5.PickType;
import momime.client.language.database.v0_9_5.Plane;
import momime.client.language.database.v0_9_5.PopulationTask;
import momime.client.language.database.v0_9_5.ProductionType;
import momime.client.language.database.v0_9_5.Race;
import momime.client.language.database.v0_9_5.RangedAttackType;
import momime.client.language.database.v0_9_5.Shortcut;
import momime.client.language.database.v0_9_5.ShortcutKey;
import momime.client.language.database.v0_9_5.Spell;
import momime.client.language.database.v0_9_5.SpellBookSection;
import momime.client.language.database.v0_9_5.SpellRank;
import momime.client.language.database.v0_9_5.SpellSetting;
import momime.client.language.database.v0_9_5.TileType;
import momime.client.language.database.v0_9_5.Unit;
import momime.client.language.database.v0_9_5.UnitAttribute;
import momime.client.language.database.v0_9_5.UnitSetting;
import momime.client.language.database.v0_9_5.UnitSkill;
import momime.client.language.database.v0_9_5.Wizard;
import momime.common.database.SpellBookSectionID;

import org.junit.Test;

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
			final Plane newPlane = new Plane ();
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
			final ProductionType newProductionType = new ProductionType ();
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
			final MapFeature newMapFeature = new MapFeature ();
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
			final TileType newTileType = new TileType ();
			newTileType.setTileTypeID ("TT0" + n);
			newTileType.setTileTypeDescription ("TTDesc0" + n);
			lang.getTileType ().add (newTileType);
		}

		lang.buildMaps ();

		assertEquals ("TTDesc02", lang.findTileType ("TT02").getTileTypeDescription ());
		assertNull (lang.findTileType ("TT04"));
	}

	/**
	 * Tests the findPickTypeDescription method
	 */
	@Test
	public final void testFindPickTypeDescription ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final PickType newPickType = new PickType ();
			newPickType.setPickTypeID ("PT0" + n);
			newPickType.setPickTypeDescription ("PTDesc0" + n);
			lang.getPickType ().add (newPickType);
		}

		lang.buildMaps ();

		assertEquals ("PTDesc02", lang.findPickTypeDescription ("PT02"));
		assertEquals ("PT04", lang.findPickTypeDescription ("PT04"));
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
			final Pick newPick = new Pick ();
			newPick.setPickID ("MB0" + n);
			newPick.setPickDescription ("MBDesc0" + n);
			lang.getPick ().add (newPick);
		}

		lang.buildMaps ();

		assertEquals ("MBDesc02", lang.findPick ("MB02").getPickDescription ());
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
			final Wizard newWizard = new Wizard ();
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
			final PopulationTask newPopulationTask = new PopulationTask ();
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
			final Race newRace = new Race ();
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
			final Building newBuilding = new Building ();
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
			final UnitTypeEx newUnitType = new UnitTypeEx ();
			newUnitType.setUnitTypeID ("UT0" + n);
			newUnitType.setUnitTypeExperienced ("UTDesc0" + n);		// There's no "description" field, so just using any other text field to test with
			lang.getUnitType ().add (newUnitType);
		}

		lang.buildMaps ();

		assertEquals ("UTDesc02", lang.findUnitType ("UT02").getUnitTypeExperienced ());
		assertNull (lang.findUnitType ("UT04"));
	}
	
	/**
	 * Tests the findUnitAttribute method
	 */
	@Test
	public final void testFindUnitAttribute ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitAttribute newUnitAttribute = new UnitAttribute ();
			newUnitAttribute.setUnitAttributeID ("UA0" + n);
			newUnitAttribute.setUnitAttributeDescription ("UADesc0" + n);
			lang.getUnitAttribute ().add (newUnitAttribute);
		}

		lang.buildMaps ();

		assertEquals ("UADesc02", lang.findUnitAttribute ("UA02").getUnitAttributeDescription ());
		assertNull (lang.findUnitAttribute ("UA04"));
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
			final UnitSkill newUnitSkill = new UnitSkill ();
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
			final RangedAttackType newRangedAttackType = new RangedAttackType ();
			newRangedAttackType.setRangedAttackTypeID ("RAT0" + n);
			newRangedAttackType.setRangedAttackTypeDescription ("RATDesc0" + n);
			lang.getRangedAttackType ().add (newRangedAttackType);
		}

		lang.buildMaps ();

		assertEquals ("RATDesc02", lang.findRangedAttackTypeDescription ("RAT02"));
		assertEquals ("RAT04", lang.findRangedAttackTypeDescription ("RAT04"));
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
			final Unit newUnit = new Unit ();
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
			final Hero newHeroName = new Hero ();
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
			final CitySize newCitySize = new CitySize ();
			newCitySize.setCitySizeID ("CS0" + n);
			newCitySize.setCitySizeName ("CSDesc0" + n);
			lang.getCitySize ().add (newCitySize);
		}

		lang.buildMaps ();

		assertEquals ("CSDesc02", lang.findCitySizeName ("CS02"));
		assertEquals ("CS04", lang.findCitySizeName ("CS04"));
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
			final CombatAreaEffect newCombatAreaEffect = new CombatAreaEffect ();
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
			final SpellRank newSpellRank = new SpellRank ();
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
			final SpellBookSection newSection = new SpellBookSection ();
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
			final Spell newSpell = new Spell ();
			newSpell.setSpellID ("MB0" + n);
			newSpell.setSpellDescription ("MBDesc0" + n);
			lang.getSpell ().add (newSpell);
		}

		lang.buildMaps ();

		assertEquals ("MBDesc02", lang.findSpell ("MB02").getSpellDescription ());
		assertNull (lang.findSpell ("MB04"));
	}
	
	/**
	 * Tests the findMapSizeDescription method
	 */
	@Test
	public final void testFindMapSizeDescription ()
	{
		final LanguageDatabaseExImpl lang = new LanguageDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final MapSize newMapSize = new MapSize ();
			newMapSize.setMapSizeID ("MS0" + n);
			newMapSize.setMapSizeDescription ("MSDesc0" + n);
			lang.getMapSize ().add (newMapSize);
		}

		lang.buildMaps ();

		assertEquals ("MSDesc02", lang.findMapSizeDescription ("MS02"));
		assertEquals ("MS04", lang.findMapSizeDescription ("MS04"));
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
			final LandProportion newLandProportion = new LandProportion ();
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
			final NodeStrength newNodeStrength = new NodeStrength ();
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
			final DifficultyLevel newDifficultyLevel = new DifficultyLevel ();
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
			final FogOfWarSetting newFogOfWarSetting = new FogOfWarSetting ();
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
			final UnitSetting newUnitSetting = new UnitSetting ();
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
			final SpellSetting newSpellSetting = new SpellSetting ();
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
				final LanguageEntry entry = new LanguageEntry ();
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
			final ShortcutKey newKey = new ShortcutKey ();
			newKey.setNormalKey (new Integer (n).toString ());
			newKey.setShortcut ((n == 1) ? Shortcut.SPELLBOOK : Shortcut.OVERLAND_MOVE_DONE);
			lang.getShortcutKey ().add (newKey);
		}

		lang.buildMaps ();

		assertEquals ("2", lang.findShortcutKey (Shortcut.OVERLAND_MOVE_DONE).getNormalKey ());
		assertNull (lang.findShortcutKey (Shortcut.OVERLAND_MOVE_WAIT));
	}
}