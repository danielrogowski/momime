package momime.client.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import momime.common.database.Building;
import momime.common.database.CombatAreaEffect;
import momime.common.database.CombatTileBorder;
import momime.common.database.CombatTileType;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItemBonus;
import momime.common.database.HeroItemSlotType;
import momime.common.database.HeroItemType;
import momime.common.database.Pick;
import momime.common.database.PickAndQuantity;
import momime.common.database.PickType;
import momime.common.database.Plane;
import momime.common.database.ProductionType;
import momime.common.database.Race;
import momime.common.database.RangedAttackType;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.TileType;
import momime.common.database.Unit;
import momime.common.database.UnitSkill;
import momime.common.database.UnitType;
import momime.common.database.WeaponGrade;

/**
 * Tests the ClientDatabaseLookup class
 * We've already tested the lookups in TestCommonDatabaseLookup - what we're really checking here is that the return types allow direct access to the client-only properties without typecasting
 */
public final class TestClientDatabaseExImpl
{
	/**
	 * Tests the derivation of the mostExpensiveConstructionCost
	 */
	@Test
	public final void testMostExpensiveConstructionCost ()
	{
		// Set up some sample buildings and units
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		
		final Building building1 = new Building ();
		building1.setProductionCost (30);
		db.getBuilding ().add (building1);

		final Building building2 = new Building ();
		db.getBuilding ().add (building2);

		final Unit unit1 = new Unit ();
		unit1.setProductionCost (40);
		unit1.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		db.getUnit ().add (unit1);

		final Unit unit2 = new Unit ();
		unit2.setProductionCost (50);
		unit2.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
		db.getUnit ().add (unit2);

		final Unit unit3 = new Unit ();
		unit3.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		db.getUnit ().add (unit3);
		
		// Run method
		db.buildMapsAndRunConsistencyChecks ();
		
		// Check results
		assertEquals (40, db.getMostExpensiveConstructionCost ());
	}
	
	/**
	 * Tests the findMapFeatureID method to find a mapFeature ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindMapFeatureID_Exists () throws RecordNotFoundException
	{
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final MapFeature newMapFeature = new MapFeature ();
			newMapFeature.setMapFeatureID ("MF0" + n);
			newMapFeature.setAnyMagicRealmsDefined (true);
			db.getMapFeature ().add (newMapFeature);
		}

		db.buildMaps ();

		assertEquals ("MF02", db.findMapFeature ("MF02", "testFindMapFeatureID_Exists").getMapFeatureID ());
		assertTrue (db.findMapFeature ("MF02", "testFindMapFeatureID_Exists").isAnyMagicRealmsDefined ());
	}

	/**
	 * Tests the findMapFeatureID method to find a mapFeature ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindMapFeatureID_NotExists () throws RecordNotFoundException
	{
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final MapFeature newMapFeature = new MapFeature ();
			newMapFeature.setMapFeatureID ("MF0" + n);
			db.getMapFeature ().add (newMapFeature);
		}

		db.buildMaps ();

		assertNull (db.findMapFeature ("MF04", "testFindMapFeatureID_NotExists"));
	}

	/**
	 * Tests the findWizardID method to find a wizard ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindWizardID_Exists () throws RecordNotFoundException
	{
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final Wizard newWizard = new Wizard ();
			newWizard.setWizardID ("WZ0" + n);

			final PickAndQuantity pick = new PickAndQuantity ();
			pick.setPickID ("MB0" + n);
			pick.setQuantity (n);
			newWizard.getWizardPick ().add (pick);

			db.getWizard ().add (newWizard);
		}

		db.buildMaps ();

		assertEquals ("WZ02", db.findWizard ("WZ02", "testFindWizardID_Exists").getWizardID ());
		assertEquals (1, db.findWizard ("WZ02", "testFindWizardID_Exists").getWizardPick ().size ());
		assertEquals ("MB02", db.findWizard ("WZ02", "testFindWizardID_Exists").getWizardPick ().get (0).getPickID ());
		assertEquals (2, db.findWizard ("WZ02", "testFindWizardID_Exists").getWizardPick ().get (0).getQuantity ());
	}

	/**
	 * Tests the findWizardID method to find a wizard ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindWizardID_NotExists () throws RecordNotFoundException
	{
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final Wizard newWizard = new Wizard ();
			newWizard.setWizardID ("WZ0" + n);
			db.getWizard ().add (newWizard);
		}

		db.buildMaps ();

		assertNull (db.findWizard ("WZ04", "testFindWizardID_NotExists"));
	}

	/**
	 * Tests the findPlaneID method to find a plane ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindPlaneID_Exists () throws RecordNotFoundException
	{
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 0; n < 2; n++)
		{
			final Plane newPlane = new Plane ();
			newPlane.setPlaneNumber (n);

			db.getPlane ().add (newPlane);
		}

		db.buildMaps ();

		assertEquals (1, db.findPlane (1, "testFindPlaneID_Exists").getPlaneNumber ());
	}

	/**
	 * Tests the findPlaneID method to find a plane ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindPlaneID_NotExists () throws RecordNotFoundException
	{
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 0; n < 2; n++)
		{
			final Plane newPlane = new Plane ();
			newPlane.setPlaneNumber (n);

			db.getPlane ().add (newPlane);
		}

		db.buildMaps ();

		db.findPlane (2, "testFindPlaneID_NotExists");
	}

	/**
	 * Tests the findTileType method to find a tileType ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindTileType_Exists () throws RecordNotFoundException
	{
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final TileType newTileType = new TileType ();
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
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final TileType newTileType = new TileType ();
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
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final ProductionType newProductionType = new ProductionType ();
			newProductionType.setProductionTypeID ("RE0" + n);

			db.getProductionType ().add (newProductionType);
		}

		db.buildMaps ();

		assertEquals ("RE02", db.findProductionType ("RE02", "testFindProductionTypeID_Exists").getProductionTypeID ());
	}

	/**
	 * Tests the findProductionTypeID method to find a productionType ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindProductionTypeID_NotExists () throws RecordNotFoundException
	{
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final ProductionType newProductionType = new ProductionType ();
			newProductionType.setProductionTypeID ("RE0" + n);

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
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final PickType newPickType = new PickType ();
			newPickType.setPickTypeID ("PT0" + n);

			db.getPickType ().add (newPickType);
		}

		db.buildMaps ();

		assertEquals ("PT02", db.findPickType ("PT02", "testFindPickTypeID_Exists").getPickTypeID ());
	}

	/**
	 * Tests the findPickTypeID method to find a pickType ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindPickTypeID_NotExists () throws RecordNotFoundException
	{
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final PickType newPickType = new PickType ();
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
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final Pick newPick = new Pick ();
			newPick.setPickID ("MB0" + n);
			db.getPick ().add (newPick);
		}

		db.buildMaps ();

		assertEquals ("MB02", db.findPick ("MB02", "testFindPickID_Exists").getPickID ());
	}

	/**
	 * Tests the findPickID method to find a pick ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindPickID_NotExists () throws RecordNotFoundException
	{
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final Pick newPick = new Pick ();
			newPick.setPickID ("MB0" + n);
			db.getPick ().add (newPick);
		}

		db.buildMaps ();

		db.findPick ("MB04", "testFindPickID_NotExists");
	}

	/**
	 * Tests the findUnitType method to find a unitType ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindUnitType_Exists () throws RecordNotFoundException
	{
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitType newUnitType = new UnitType ();
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
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitType newUnitType = new UnitType ();
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
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final Unit newUnit = new Unit ();
			newUnit.setUnitID ("UN00" + n);

			db.getUnit ().add (newUnit);
		}

		db.buildMaps ();

		assertEquals ("UN002", db.findUnit ("UN002", "testFindUnitID_Exists").getUnitID ());
	}

	/**
	 * Tests the findUnitID method to find a unit ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindUnitID_NotExists () throws RecordNotFoundException
	{
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final Unit newUnit = new Unit ();
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
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitSkill newUnitSkill = new UnitSkill ();
			newUnitSkill.setUnitSkillID ("US00" + n);

			db.getUnitSkill ().add (newUnitSkill);
		}

		db.buildMaps ();

		assertEquals ("US002", db.findUnitSkill ("US002", "testFindUnitSkillID_Exists").getUnitSkillID ());
	}

	/**
	 * Tests the findUnitSkillID method to find a unit skill ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindUnitSkillID_NotExists () throws RecordNotFoundException
	{
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitSkill newUnitSkill = new UnitSkill ();
			newUnitSkill.setUnitSkillID ("US00" + n);

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
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final WeaponGrade newWeaponGrade = new WeaponGrade ();
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
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final WeaponGrade newWeaponGrade = new WeaponGrade ();
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
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final RangedAttackType newRangedAttackType = new RangedAttackType ();
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
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final RangedAttackType newRangedAttackType = new RangedAttackType ();
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
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final Race newRace = new Race ();
			newRace.setRaceID ("RC0" + n);

			db.getRace ().add (newRace);
		}

		db.buildMaps ();

		assertEquals ("RC02", db.findRace ("RC02", "testFindRaceID_Exists").getRaceID ());
	}

	/**
	 * Tests the findRaceID method to find a race ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindRaceID_NotExists () throws RecordNotFoundException
	{
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final Race newRace = new Race ();
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
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final Building newBuilding = new Building ();
			newBuilding.setBuildingID ("BL0" + n);
			db.getBuilding ().add (newBuilding);
		}

		db.buildMaps ();

		assertEquals ("BL02", db.findBuilding ("BL02", "testFindBuilding_Exists").getBuildingID ());
	}

	/**
	 * Tests the findBuilding method to find a building ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindBuilding_NotExists () throws RecordNotFoundException
	{
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final Building newBuilding = new Building ();
			newBuilding.setBuildingID ("BL0" + n);
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
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final Spell newSpell = new Spell ();
			newSpell.setSpellID ("SP00" + n);
			db.getSpell ().add (newSpell);
		}

		db.buildMaps ();

		assertEquals ("SP002", db.findSpell ("SP002", "testFindSpellID_Exists").getSpellID ());
	}

	/**
	 * Tests the findSpellID method to find a spell ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindSpellID_NotExists () throws RecordNotFoundException
	{
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final Spell newSpell = new Spell ();
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
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CombatAreaEffect newCombatAreaEffect = new CombatAreaEffect ();
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
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CombatAreaEffect newCombatAreaEffect = new CombatAreaEffect ();
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
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CombatTileType newCombatTileType = new CombatTileType ();
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
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CombatTileType newCombatTileType = new CombatTileType ();
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
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CombatTileBorder newCombatTileBorder = new CombatTileBorder ();
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
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CombatTileBorder newCombatTileBorder = new CombatTileBorder ();
			newCombatTileBorder.setCombatTileBorderID ("CTB0" + n);
			db.getCombatTileBorder ().add (newCombatTileBorder);
		}

		db.buildMaps ();

		assertNull (db.findCombatTileBorder ("CTB04", "testFindCombatTileBorder_NotExists"));
	}

	/**
	 * Tests the findHeroItemSlotType method to find a heroItemSlotType ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindHeroItemSlotType_Exists () throws RecordNotFoundException
	{
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final HeroItemSlotType newHeroItemSlotType = new HeroItemSlotType ();
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
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final HeroItemSlotType newHeroItemSlotType = new HeroItemSlotType ();
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
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final HeroItemType newHeroItemType = new HeroItemType ();
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
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final HeroItemType newHeroItemType = new HeroItemType ();
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
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final HeroItemBonus newHeroItemBonus = new HeroItemBonus ();
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
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final HeroItemBonus newHeroItemBonus = new HeroItemBonus ();
			newHeroItemBonus.setHeroItemBonusID ("IB0" + n);
			db.getHeroItemBonus ().add (newHeroItemBonus);
		}

		db.buildMaps ();

		assertNull (db.findHeroItemBonus ("IB04", "testFindHeroItemBonus_NotExists"));
	}
}