package momime.server.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import momime.common.database.RecordNotFoundException;

import org.junit.Test;

/**
 * Tests the ServerDatabaseEx class
 * Note there's no point testing any of the 'get' methods that return the complete lists, since they're provided by the JAXB-generated code
 * Only need to test the maps that we coded in the Ex class
 */
public final class TestServerDatabaseExImpl
{
	/**
	 * Tests the findPlaneID method to find a plane ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindPlaneID_Exists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 0; n < 2; n++)
		{
			final PlaneSvr newPlane = new PlaneSvr ();
			newPlane.setPlaneNumber (n);
			newPlane.setPlaneDescription ("Plane " + n);

			db.getPlane ().add (newPlane);
		}

		db.buildMaps ();

		assertEquals ("Plane 1", db.findPlane (1, "testFindPlaneID_Exists").getPlaneDescription ());
	}

	/**
	 * Tests the findPlaneID method to find a plane ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindPlaneID_NotExists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 0; n < 2; n++)
		{
			final PlaneSvr newPlane = new PlaneSvr ();
			newPlane.setPlaneNumber (n);
			newPlane.setPlaneDescription ("Plane " + n);

			db.getPlane ().add (newPlane);
		}

		db.buildMaps ();

		db.findPlane (2, "testFindPlaneID_NotExists");
	}

	/**
	 * Tests the findMapFeatureID method to find a mapFeature ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindMapFeatureID_Exists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final MapFeatureSvr newMapFeature = new MapFeatureSvr ();
			newMapFeature.setMapFeatureID ("MF0" + n);
			newMapFeature.setCityQualityEstimate (n * 10);
			db.getMapFeature ().add (newMapFeature);
		}

		db.buildMaps ();

		assertEquals ("MF02", db.findMapFeature ("MF02", "testFindMapFeatureID_Exists").getMapFeatureID ());
		assertEquals (20, db.findMapFeature ("MF02", "testFindMapFeatureID_Exists").getCityQualityEstimate ().intValue ());
	}

	/**
	 * Tests the findMapFeatureID method to find a mapFeature ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindMapFeatureID_NotExists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final MapFeatureSvr newMapFeature = new MapFeatureSvr ();
			newMapFeature.setMapFeatureID ("MF0" + n);
			db.getMapFeature ().add (newMapFeature);
		}

		db.buildMaps ();

		db.findMapFeature ("MF04", "testFindMapFeatureID_NotExists");
	}

	/**
	 * Tests the findTileType method to find a tileType ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindTileType_Exists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final TileTypeSvr newTileType = new TileTypeSvr ();
			newTileType.setTileTypeID ("TT0" + n);
			db.getTileType ().add (newTileType);
		}

		db.buildMaps ();

		assertEquals ("TT02", db.findTileType ("TT02", "testFindTileType_Exists").getTileTypeID ());
	}

	/**
	 * Tests the findTileType method to find a tileType ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindTileType_NotExists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final TileTypeSvr newTileType = new TileTypeSvr ();
			newTileType.setTileTypeID ("TT0" + n);
			db.getTileType ().add (newTileType);
		}

		db.buildMaps ();

		assertNull (db.findTileType ("TT04", "testFindTileType_NotExists"));
	}

	/**
	 * Tests the findProductionTypeID method to find a productionType ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindProductionTypeID_Exists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final ProductionTypeSvr newProductionType = new ProductionTypeSvr ();
			newProductionType.setProductionTypeID ("RE0" + n);
			newProductionType.setProductionTypeDescription ("ProductionType " + n);

			db.getProductionType ().add (newProductionType);
		}

		db.buildMaps ();

		assertEquals ("RE02", db.findProductionType ("RE02", "testFindProductionTypeID_Exists").getProductionTypeID ());
		assertEquals ("ProductionType 2", db.findProductionType ("RE02", "testFindProductionTypeID_Exists").getProductionTypeDescription ());
	}

	/**
	 * Tests the findProductionTypeID method to find a productionType ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindProductionTypeID_NotExists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final ProductionTypeSvr newProductionType = new ProductionTypeSvr ();
			newProductionType.setProductionTypeID ("RE0" + n);
			newProductionType.setProductionTypeDescription ("ProductionType " + n);

			db.getProductionType ().add (newProductionType);
		}

		db.buildMaps ();

		db.findProductionType ("RE04", "testFindProductionTypeID_NotExists");
	}

	/**
	 * Tests the findPickTypeID method to find a pickType ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindPickTypeID_Exists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final PickTypeSvr newPickType = new PickTypeSvr ();
			newPickType.setPickTypeID ("PT0" + n);

			final PickTypeCountContainerSvr container = new PickTypeCountContainerSvr ();
			container.setCount (n);
			newPickType.getPickTypeCount ().add (container);

			db.getPickType ().add (newPickType);
		}

		db.buildMaps ();

		assertEquals ("PT02", db.findPickType ("PT02", "testFindPickTypeID_Exists").getPickTypeID ());
		assertEquals (1, db.findPickType ("PT02", "testFindPickTypeID_Exists").getPickTypeCount ().size ());
		assertEquals (2, db.findPickType ("PT02", "testFindPickTypeID_Exists").getPickTypeCount ().get (0).getCount ());
	}

	/**
	 * Tests the findPickTypeID method to find a pickType ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindPickTypeID_NotExists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final PickTypeSvr newPickType = new PickTypeSvr ();
			newPickType.setPickTypeID ("PT0" + n);
			db.getPickType ().add (newPickType);
		}

		db.buildMaps ();

		db.findPickType ("PT04", "testFindPickTypeID_NotExists");
	}

	/**
	 * Tests the findPickID method to find a pick ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindPickID_Exists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final PickSvr newPick = new PickSvr ();
			newPick.setPickID ("MB0" + n);
			newPick.setPickInitialSkill (2);
			db.getPick ().add (newPick);
		}

		db.buildMaps ();

		assertEquals ("MB02", db.findPick ("MB02", "testFindPickID_Exists").getPickID ());
		assertEquals (2, db.findPick ("MB02", "testFindPickID_Exists").getPickInitialSkill ().intValue ());
	}

	/**
	 * Tests the findPickID method to find a pick ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindPickID_NotExists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final PickSvr newPick = new PickSvr ();
			newPick.setPickID ("MB0" + n);
			db.getPick ().add (newPick);
		}

		db.buildMaps ();

		db.findPick ("MB04", "testFindPickID_NotExists");
	}

	/**
	 * Tests the findWizardID method to find a wizard ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindWizardID_Exists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final WizardSvr newWizard = new WizardSvr ();
			newWizard.setWizardID ("WZ0" + n);

			final WizardPickCountSvr pick = new WizardPickCountSvr ();
			pick.setPickCount (10 + n);
			newWizard.getWizardPickCount ().add (pick);

			db.getWizard ().add (newWizard);
		}

		db.buildMaps ();

		assertEquals ("WZ02", db.findWizard ("WZ02", "testFindWizardID_Exists").getWizardID ());
		assertEquals (1, db.findWizard ("WZ02", "testFindWizardID_Exists").getWizardPickCount ().size ());
		assertEquals (12, db.findWizard ("WZ02", "testFindWizardID_Exists").getWizardPickCount ().get (0).getPickCount ());
	}

	/**
	 * Tests the findWizardID method to find a wizard ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindWizardID_NotExists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final WizardSvr newWizard = new WizardSvr ();
			newWizard.setWizardID ("WZ0" + n);
			db.getWizard ().add (newWizard);
		}

		db.buildMaps ();

		db.findWizard ("WZ04", "testFindWizardID_NotExists");
	}

	/**
	 * Tests the findUnitType method to find a unitType ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindUnitType_Exists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitTypeSvr newUnitType = new UnitTypeSvr ();
			newUnitType.setUnitTypeID ("T" + n);		// Real values are N, H, S
			db.getUnitType ().add (newUnitType);
		}

		db.buildMaps ();

		assertEquals ("T2", db.findUnitType ("T2", "testFindUnitType_Exists").getUnitTypeID ());
	}

	/**
	 * Tests the findUnitType method to find a unitType ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindUnitType_NotExists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitTypeSvr newUnitType = new UnitTypeSvr ();
			newUnitType.setUnitTypeID ("T" + n);		// Real values are N, H, S
			db.getUnitType ().add (newUnitType);
		}

		db.buildMaps ();

		assertNull (db.findUnitType ("T4", "testFindUnitType_NotExists"));
	}

	/**
	 * Tests the findUnitID method to find a unit ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindUnitID_Exists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitSvr newUnit = new UnitSvr ();
			newUnit.setUnitID ("UN00" + n);
			newUnit.setUnitName ("Unit name " + n);

			db.getUnit ().add (newUnit);
		}

		db.buildMaps ();

		assertEquals ("UN002", db.findUnit ("UN002", "testFindUnitID_Exists").getUnitID ());
		assertEquals ("Unit name 2", db.findUnit ("UN002", "testFindUnitID_Exists").getUnitName ());
	}

	/**
	 * Tests the findUnitID method to find a unit ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindUnitID_NotExists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitSvr newUnit = new UnitSvr ();
			newUnit.setUnitID ("UN00" + n);
			db.getUnit ().add (newUnit);
		}

		db.buildMaps ();

		db.findUnit ("UN004", "testFindUnitID_NotExists");
	}

	/**
	 * Tests the findUnitSkillID method to find a unit skill ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindUnitSkillID_Exists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitSkillSvr newUnitSkill = new UnitSkillSvr ();
			newUnitSkill.setUnitSkillID ("US00" + n);
			newUnitSkill.setUnitSkillScoutingRange (n);

			db.getUnitSkill ().add (newUnitSkill);
		}

		db.buildMaps ();

		assertEquals (2, db.findUnitSkill ("US002", "testFindUnitSkillID_Exists").getUnitSkillScoutingRange ().intValue ());
	}

	/**
	 * Tests the findUnitSkillID method to find a unit skill ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindUnitSkillID_NotExists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitSkillSvr newUnitSkill = new UnitSkillSvr ();
			newUnitSkill.setUnitSkillID ("US00" + n);
			newUnitSkill.setUnitSkillScoutingRange (n);

			db.getUnitSkill ().add (newUnitSkill);
		}

		db.buildMaps ();

		db.findUnitSkill ("US004", "testFindUnitSkillID_NotExists");
	}

	/**
	 * Tests the findWeaponGrade method to find a weaponGradeNumber that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindWeaponGrade_Exists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final WeaponGradeSvr newWeaponGrade = new WeaponGradeSvr ();
			newWeaponGrade.setWeaponGradeNumber (n);
			db.getWeaponGrade ().add (newWeaponGrade);
		}

		db.buildMaps ();

		assertEquals (2, db.findWeaponGrade (2, "testFindWeaponGrade_Exists").getWeaponGradeNumber ());
	}

	/**
	 * Tests the findWeaponGrade method to find a weaponGradeNumber that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindWeaponGrade_NotExists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final WeaponGradeSvr newWeaponGrade = new WeaponGradeSvr ();
			newWeaponGrade.setWeaponGradeNumber (n);
			db.getWeaponGrade ().add (newWeaponGrade);
		}

		db.buildMaps ();

		assertNull (db.findWeaponGrade (4, "testFindWeaponGrade_NotExists"));
	}
	
	/**
	 * Tests the findRangedAttackType method to find a rangedAttackTypeNumber that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindRangedAttackType_Exists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final RangedAttackTypeSvr newRangedAttackType = new RangedAttackTypeSvr ();
			newRangedAttackType.setRangedAttackTypeID ("RAT0" + n);
			db.getRangedAttackType ().add (newRangedAttackType);
		}

		db.buildMaps ();

		assertEquals ("RAT02", db.findRangedAttackType ("RAT02", "testFindRangedAttackType_Exists").getRangedAttackTypeID ());
	}

	/**
	 * Tests the findRangedAttackType method to find a rangedAttackTypeNumber that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindRangedAttackType_NotExists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final RangedAttackTypeSvr newRangedAttackType = new RangedAttackTypeSvr ();
			newRangedAttackType.setRangedAttackTypeID ("RAT0" + n);
			db.getRangedAttackType ().add (newRangedAttackType);
		}

		db.buildMaps ();

		assertNull (db.findRangedAttackType ("RAT04", "testFindRangedAttackType_NotExists"));
	}
	
	/**
	 * Tests the findRaceID method to find a race ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindRaceID_Exists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final RaceSvr newRace = new RaceSvr ();
			newRace.setRaceID ("RC0" + n);

			final CityNameContainerSvr cityName = new CityNameContainerSvr ();
			cityName.setCityName ("Blah");
			newRace.getCityName ().add (cityName);

			db.getRace ().add (newRace);
		}

		db.buildMaps ();

		assertEquals ("RC02", db.findRace ("RC02", "testFindRaceID_Exists").getRaceID ());
		assertEquals (1, db.findRace ("RC02", "testFindRaceID_Exists").getCityName ().size ());
		assertEquals ("Blah", db.findRace ("RC02", "testFindRaceID_Exists").getCityName ().get (0).getCityName ());
	}

	/**
	 * Tests the findRaceID method to find a race ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindRaceID_NotExists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final RaceSvr newRace = new RaceSvr ();
			newRace.setRaceID ("RC0" + n);
			db.getRace ().add (newRace);
		}

		db.buildMaps ();

		db.findRace ("RC04", "testFindRaceID_NotExists");
	}

	/**
	 * Tests the findBuilding method to find a building ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindBuilding_Exists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final BuildingSvr newBuilding = new BuildingSvr ();
			newBuilding.setBuildingID ("BL0" + n);
			newBuilding.setBuildingScoutingRange (n);
			db.getBuilding ().add (newBuilding);
		}

		db.buildMaps ();

		assertEquals (2, db.findBuilding ("BL02", "testFindBuilding_Exists").getBuildingScoutingRange ().intValue ());
	}

	/**
	 * Tests the findBuilding method to find a building ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindBuilding_NotExists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final BuildingSvr newBuilding = new BuildingSvr ();
			newBuilding.setBuildingID ("BL0" + n);
			newBuilding.setBuildingScoutingRange (n);
			db.getBuilding ().add (newBuilding);
		}

		db.buildMaps ();

		assertNull (db.findBuilding ("BL04", "testFindBuilding_NotExists"));
	}

	/**
	 * Tests the findSpellID method to find a spell ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindSpellID_Exists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final SpellSvr newSpell = new SpellSvr ();
			newSpell.setSpellID ("SP00" + n);
			newSpell.setAiResearchOrder (n);
			db.getSpell ().add (newSpell);
		}

		db.buildMaps ();

		assertEquals ("SP002", db.findSpell ("SP002", "testFindSpellID_Exists").getSpellID ());
		assertEquals (2, db.findSpell ("SP002", "testFindSpellID_Exists").getAiResearchOrder ().intValue ());
	}

	/**
	 * Tests the findSpellID method to find a spell ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindSpellID_NotExists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final SpellSvr newSpell = new SpellSvr ();
			newSpell.setSpellID ("SP00" + n);
			db.getSpell ().add (newSpell);
		}

		db.buildMaps ();

		db.findSpell ("SP004", "testFindSpellID_NotExists");
	}

	/**
	 * Tests the findCombatAreaEffect method to find a combatAreaEffect ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindCombatAreaEffect_Exists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CombatAreaEffectSvr newCombatAreaEffect = new CombatAreaEffectSvr ();
			newCombatAreaEffect.setCombatAreaEffectID ("CAE0" + n);
			db.getCombatAreaEffect ().add (newCombatAreaEffect);
		}

		db.buildMaps ();

		assertEquals ("CAE02", db.findCombatAreaEffect ("CAE02", "testFindCombatAreaEffect_Exists").getCombatAreaEffectID ());
	}

	/**
	 * Tests the findCombatAreaEffect method to find a combatAreaEffect ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindCombatAreaEffect_NotExists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CombatAreaEffectSvr newCombatAreaEffect = new CombatAreaEffectSvr ();
			newCombatAreaEffect.setCombatAreaEffectID ("CAE0" + n);
			db.getCombatAreaEffect ().add (newCombatAreaEffect);
		}

		db.buildMaps ();

		assertNull (db.findCombatAreaEffect ("CAE04", "testFindCombatAreaEffect_NotExists"));
	}

	/**
	 * Tests the findCombatTileType method to find a combatTileType ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindCombatTileType_Exists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CombatTileTypeSvr newCombatTileType = new CombatTileTypeSvr ();
			newCombatTileType.setCombatTileTypeID ("CTL0" + n);
			db.getCombatTileType ().add (newCombatTileType);
		}

		db.buildMaps ();

		assertEquals ("CTL02", db.findCombatTileType ("CTL02", "testFindCombatTileType_Exists").getCombatTileTypeID ());
	}

	/**
	 * Tests the findCombatTileType method to find a combatTileType ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindCombatTileType_NotExists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CombatTileTypeSvr newCombatTileType = new CombatTileTypeSvr ();
			newCombatTileType.setCombatTileTypeID ("CTL0" + n);
			db.getCombatTileType ().add (newCombatTileType);
		}

		db.buildMaps ();

		assertNull (db.findCombatTileType ("CTL04", "testFindCombatTileType_NotExists"));
	}

	/**
	 * Tests the findCombatTileBorder method to find a combatTileBorder ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindCombatTileBorder_Exists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CombatTileBorderSvr newCombatTileBorder = new CombatTileBorderSvr ();
			newCombatTileBorder.setCombatTileBorderID ("CTB0" + n);
			db.getCombatTileBorder ().add (newCombatTileBorder);
		}

		db.buildMaps ();

		assertEquals ("CTB02", db.findCombatTileBorder ("CTB02", "testFindCombatTileBorder_Exists").getCombatTileBorderID ());
	}

	/**
	 * Tests the findCombatTileBorder method to find a combatTileBorder ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindCombatTileBorder_NotExists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CombatTileBorderSvr newCombatTileBorder = new CombatTileBorderSvr ();
			newCombatTileBorder.setCombatTileBorderID ("CTB0" + n);
			db.getCombatTileBorder ().add (newCombatTileBorder);
		}

		db.buildMaps ();

		assertNull (db.findCombatTileBorder ("CTB04", "testFindCombatTileBorder_NotExists"));
	}
	
	/**
	 * Tests the findCitySpellEffect method to find a citySpellEffect ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindCitySpellEffect_Exists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CitySpellEffectSvr newCitySpellEffect = new CitySpellEffectSvr ();
			newCitySpellEffect.setCitySpellEffectID ("SE00" + n);
			db.getCitySpellEffect ().add (newCitySpellEffect);
		}

		db.buildMaps ();

		assertEquals ("SE002", db.findCitySpellEffect ("SE002", "testFindCitySpellEffect_Exists").getCitySpellEffectID ());
	}

	/**
	 * Tests the findCitySpellEffect method to find a citySpellEffect ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindCitySpellEffect_NotExists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CitySpellEffectSvr newCitySpellEffect = new CitySpellEffectSvr ();
			newCitySpellEffect.setCitySpellEffectID ("SE00" + n);
			db.getCitySpellEffect ().add (newCitySpellEffect);
		}

		db.buildMaps ();

		assertNull (db.findCitySpellEffect ("SE004", "testFindCitySpellEffect_NotExists"));
	}

	/**
	 * Tests the findHeroItemSlotType method to find a heroItemSlotType ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindHeroItemSlotType_Exists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final HeroItemSlotTypeSvr newHeroItemSlotType = new HeroItemSlotTypeSvr ();
			newHeroItemSlotType.setHeroItemSlotTypeID ("IST0" + n);
			db.getHeroItemSlotType ().add (newHeroItemSlotType);
		}

		db.buildMaps ();

		assertEquals ("IST02", db.findHeroItemSlotType ("IST02", "testFindHeroItemSlotType_Exists").getHeroItemSlotTypeID ());
	}

	/**
	 * Tests the findHeroItemSlotType method to find a heroItemSlotType ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindHeroItemSlotType_NotExists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final HeroItemSlotTypeSvr newHeroItemSlotType = new HeroItemSlotTypeSvr ();
			newHeroItemSlotType.setHeroItemSlotTypeID ("IST0" + n);
			db.getHeroItemSlotType ().add (newHeroItemSlotType);
		}

		db.buildMaps ();

		assertNull (db.findHeroItemSlotType ("IST04", "testFindHeroItemSlotType_NotExists"));
	}

	/**
	 * Tests the findHeroItemType method to find a heroItemType ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindHeroItemType_Exists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final HeroItemTypeSvr newHeroItemType = new HeroItemTypeSvr ();
			newHeroItemType.setHeroItemTypeID ("IT0" + n);
			db.getHeroItemType ().add (newHeroItemType);
		}

		db.buildMaps ();

		assertEquals ("IT02", db.findHeroItemType ("IT02", "testFindHeroItemType_Exists").getHeroItemTypeID ());
	}

	/**
	 * Tests the findHeroItemType method to find a heroItemType ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindHeroItemType_NotExists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final HeroItemTypeSvr newHeroItemType = new HeroItemTypeSvr ();
			newHeroItemType.setHeroItemTypeID ("IT0" + n);
			db.getHeroItemType ().add (newHeroItemType);
		}

		db.buildMaps ();

		assertNull (db.findHeroItemType ("IT04", "testFindHeroItemType_NotExists"));
	}

	/**
	 * Tests the findHeroItemBonus method to find a heroItemBonus ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindHeroItemBonus_Exists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final HeroItemBonusSvr newHeroItemBonus = new HeroItemBonusSvr ();
			newHeroItemBonus.setHeroItemBonusID ("IB0" + n);
			db.getHeroItemBonus ().add (newHeroItemBonus);
		}

		db.buildMaps ();

		assertEquals ("IB02", db.findHeroItemBonus ("IB02", "testFindHeroItemBonus_Exists").getHeroItemBonusID ());
	}

	/**
	 * Tests the findHeroItemBonus method to find a heroItemBonus ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindHeroItemBonus_NotExists () throws RecordNotFoundException
	{
		final ServerDatabaseExImpl db = new ServerDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final HeroItemBonusSvr newHeroItemBonus = new HeroItemBonusSvr ();
			newHeroItemBonus.setHeroItemBonusID ("IB0" + n);
			db.getHeroItemBonus ().add (newHeroItemBonus);
		}

		db.buildMaps ();

		assertNull (db.findHeroItemBonus ("IB04", "testFindHeroItemBonus_NotExists"));
	}
}