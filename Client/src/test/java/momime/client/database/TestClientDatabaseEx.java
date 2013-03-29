package momime.client.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import momime.client.database.v0_9_4.MapFeature;
import momime.client.database.v0_9_4.Wizard;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.Building;
import momime.common.database.v0_9_4.CombatAreaEffect;
import momime.common.database.v0_9_4.Pick;
import momime.common.database.v0_9_4.PickType;
import momime.common.database.v0_9_4.Plane;
import momime.common.database.v0_9_4.ProductionType;
import momime.common.database.v0_9_4.Race;
import momime.common.database.v0_9_4.Spell;
import momime.common.database.v0_9_4.TileType;
import momime.common.database.v0_9_4.Unit;
import momime.common.database.v0_9_4.UnitMagicRealm;
import momime.common.database.v0_9_4.UnitSkill;
import momime.common.database.v0_9_4.UnitType;
import momime.common.database.v0_9_4.WeaponGrade;
import momime.common.database.v0_9_4.WizardPick;

import org.junit.Test;

/**
 * Tests the ClientDatabaseLookup class
 * We've already tested the lookups in TestCommonDatabaseLookup - what we're really checking here is that the return types allow direct access to the client-only properties without typecasting
 */
public final class TestClientDatabaseEx
{
	/**
	 * Tests the findMapFeatureID method to find a mapFeature ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindMapFeatureID_Exists () throws RecordNotFoundException
	{
		final ClientDatabaseEx db = new ClientDatabaseEx ();
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
		final ClientDatabaseEx db = new ClientDatabaseEx ();
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
		final ClientDatabaseEx db = new ClientDatabaseEx ();
		for (int n = 1; n <= 3; n++)
		{
			final Wizard newWizard = new Wizard ();
			newWizard.setWizardID ("WZ0" + n);

			final WizardPick pick = new WizardPick ();
			pick.setPick ("MB0" + n);
			pick.setQuantity (n);
			newWizard.getWizardPick ().add (pick);

			db.getWizard ().add (newWizard);
		}

		db.buildMaps ();

		assertEquals ("WZ02", db.findWizard ("WZ02", "testFindWizardID_Exists").getWizardID ());
		assertEquals (1, db.findWizard ("WZ02", "testFindWizardID_Exists").getWizardPick ().size ());
		assertEquals ("MB02", db.findWizard ("WZ02", "testFindWizardID_Exists").getWizardPick ().get (0).getPick ());
		assertEquals (2, db.findWizard ("WZ02", "testFindWizardID_Exists").getWizardPick ().get (0).getQuantity ());
	}

	/**
	 * Tests the findWizardID method to find a wizard ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindWizardID_NotExists () throws RecordNotFoundException
	{
		final ClientDatabaseEx db = new ClientDatabaseEx ();
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
		final ClientDatabaseEx db = new ClientDatabaseEx ();
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
		final ClientDatabaseEx db = new ClientDatabaseEx ();
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
		final ClientDatabaseEx db = new ClientDatabaseEx ();
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
		final ClientDatabaseEx db = new ClientDatabaseEx ();
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
		final ClientDatabaseEx db = new ClientDatabaseEx ();
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
		final ClientDatabaseEx db = new ClientDatabaseEx ();
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
		final ClientDatabaseEx db = new ClientDatabaseEx ();
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
		final ClientDatabaseEx db = new ClientDatabaseEx ();
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
		final ClientDatabaseEx db = new ClientDatabaseEx ();
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
		final ClientDatabaseEx db = new ClientDatabaseEx ();
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
		final ClientDatabaseEx db = new ClientDatabaseEx ();
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
		final ClientDatabaseEx db = new ClientDatabaseEx ();
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
	 * Tests the findUnitMagicRealm method to find a unitMagicRealm ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindUnitMagicRealm_Exists () throws RecordNotFoundException
	{
		final ClientDatabaseEx db = new ClientDatabaseEx ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitMagicRealm newUnitMagicRealm = new UnitMagicRealm ();
			newUnitMagicRealm.setUnitMagicRealmID ("LT0" + n);
			db.getUnitMagicRealm ().add (newUnitMagicRealm);
		}

		db.buildMaps ();

		assertEquals ("LT02", db.findUnitMagicRealm ("LT02", "testFindUnitMagicRealm_Exists").getUnitMagicRealmID ());
	}

	/**
	 * Tests the findUnitMagicRealm method to find a unitMagicRealm ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindUnitMagicRealm_NotExists () throws RecordNotFoundException
	{
		final ClientDatabaseEx db = new ClientDatabaseEx ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitMagicRealm newUnitMagicRealm = new UnitMagicRealm ();
			newUnitMagicRealm.setUnitMagicRealmID ("LT0" + n);
			db.getUnitMagicRealm ().add (newUnitMagicRealm);
		}

		db.buildMaps ();

		assertNull (db.findUnitMagicRealm ("LT04", "testFindUnitMagicRealm_NotExists"));
	}
	
	/**
	 * Tests the findUnitID method to find a unit ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindUnitID_Exists () throws RecordNotFoundException
	{
		final ClientDatabaseEx db = new ClientDatabaseEx ();
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
		final ClientDatabaseEx db = new ClientDatabaseEx ();
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
		final ClientDatabaseEx db = new ClientDatabaseEx ();
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
		final ClientDatabaseEx db = new ClientDatabaseEx ();
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
		final ClientDatabaseEx db = new ClientDatabaseEx ();
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
		final ClientDatabaseEx db = new ClientDatabaseEx ();
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
	 * Tests the findRaceID method to find a race ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindRaceID_Exists () throws RecordNotFoundException
	{
		final ClientDatabaseEx db = new ClientDatabaseEx ();
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
		final ClientDatabaseEx db = new ClientDatabaseEx ();
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
		final ClientDatabaseEx db = new ClientDatabaseEx ();
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
		final ClientDatabaseEx db = new ClientDatabaseEx ();
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
		final ClientDatabaseEx db = new ClientDatabaseEx ();
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
		final ClientDatabaseEx db = new ClientDatabaseEx ();
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
		final ClientDatabaseEx db = new ClientDatabaseEx ();
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
		final ClientDatabaseEx db = new ClientDatabaseEx ();
		for (int n = 1; n <= 3; n++)
		{
			final CombatAreaEffect newCombatAreaEffect = new CombatAreaEffect ();
			newCombatAreaEffect.setCombatAreaEffectID ("CAE0" + n);
			db.getCombatAreaEffect ().add (newCombatAreaEffect);
		}

		db.buildMaps ();

		assertNull (db.findCombatAreaEffect ("CAE04", "testFindCombatAreaEffect_NotExists"));
	}
}
