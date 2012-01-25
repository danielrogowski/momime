package momime.common.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import momime.common.database.v0_9_4.Building;
import momime.common.database.v0_9_4.CombatAreaEffect;
import momime.common.database.v0_9_4.MapFeature;
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
import momime.common.database.v0_9_4.Wizard;

import org.junit.Test;

/**
 * Tests the CommonDatabaseLookup class
 */
public final class TestCommonDatabaseLookup
{
	/**
	 * Tests the getPlanes method
	 */
	@Test
	public final void testGetPlanes ()
	{
		final List<Plane> planes = new ArrayList<Plane> ();
		for (int n = 0; n <= 1; n++)
		{
			final Plane newPlane = new Plane ();
			newPlane.setPlaneNumber (n);
			newPlane.setPrerequisitePickToChooseNativeRace ("RT0" + n);
			planes.add (newPlane);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (planes, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

		for (int n = 0; n <= 1; n++)
		{
			assertEquals (n, db.getPlanes ().get (n).getPlaneNumber ());
			assertEquals ("RT0" + n, db.getPlanes ().get (n).getPrerequisitePickToChooseNativeRace ());
		}
	}

	/**
	 * Tests the findPlane method to find a planeNumber that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindPlane_Exists () throws RecordNotFoundException
	{
		final List<Plane> planes = new ArrayList<Plane> ();
		for (int n = 1; n <= 3; n++)
		{
			final Plane newPlane = new Plane ();
			newPlane.setPlaneNumber (n);
			planes.add (newPlane);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (planes, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

		assertEquals (2, db.findPlane (2, "testFindPlane_Exists").getPlaneNumber ());
	}

	/**
	 * Tests the findPlane method to find a planeNumber that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindPlane_NotExists () throws RecordNotFoundException
	{
		final List<Plane> planes = new ArrayList<Plane> ();
		for (int n = 1; n <= 3; n++)
		{
			final Plane newPlane = new Plane ();
			newPlane.setPlaneNumber (n);
			planes.add (newPlane);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (planes, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

		assertNull (db.findPlane (4, "testFindPlane_NotExists"));
	}

	/**
	 * Tests the getMapFeatures method
	 */
	@Test
	public final void testGetMapFeatures ()
	{
		final List<MapFeature> mapFeatures = new ArrayList<MapFeature> ();
		for (int n = 1; n <= 3; n++)
		{
			final MapFeature newMapFeature = new MapFeature ();
			newMapFeature.setMapFeatureID ("MF0" + n);
			mapFeatures.add (newMapFeature);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, mapFeatures, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

		assertEquals (3, db.getMapFeatures ().size ());
		for (int n = 1; n <= 3; n++)
			assertEquals ("MF0" + n, db.getMapFeatures ().get (n - 1).getMapFeatureID ());
	}

	/**
	 * Tests the findMapFeature method to find a mapFeature ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindMapFeature_Exists () throws RecordNotFoundException
	{
		final List<MapFeature> mapFeatures = new ArrayList<MapFeature> ();
		for (int n = 1; n <= 3; n++)
		{
			final MapFeature newMapFeature = new MapFeature ();
			newMapFeature.setMapFeatureID ("MF0" + n);
			mapFeatures.add (newMapFeature);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, mapFeatures, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

		assertEquals ("MF02", db.findMapFeature ("MF02", "testFindMapFeature_Exists").getMapFeatureID ());
	}

	/**
	 * Tests the findMapFeature method to find a mapFeature ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindMapFeature_NotExists () throws RecordNotFoundException
	{
		final List<MapFeature> mapFeatures = new ArrayList<MapFeature> ();
		for (int n = 1; n <= 3; n++)
		{
			final MapFeature newMapFeature = new MapFeature ();
			newMapFeature.setMapFeatureID ("MF0" + n);
			mapFeatures.add (newMapFeature);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, mapFeatures, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

		assertNull (db.findMapFeature ("MF04", "testFindMapFeature_NotExists"));
	}

	/**
	 * Tests the getTileTypes method
	 */
	@Test
	public final void testGetTileTypes ()
	{
		final List<TileType> tileTypes = new ArrayList<TileType> ();
		for (int n = 1; n <= 3; n++)
		{
			final TileType newTileType = new TileType ();
			newTileType.setTileTypeID ("TT0" + n);
			tileTypes.add (newTileType);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, tileTypes, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

		assertEquals (3, db.getTileTypes ().size ());
		for (int n = 1; n <= 3; n++)
			assertEquals ("TT0" + n, db.getTileTypes ().get (n - 1).getTileTypeID ());
	}

	/**
	 * Tests the findTileType method to find a tileType ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindTileType_Exists () throws RecordNotFoundException
	{
		final List<TileType> tileTypes = new ArrayList<TileType> ();
		for (int n = 1; n <= 3; n++)
		{
			final TileType newTileType = new TileType ();
			newTileType.setTileTypeID ("TT0" + n);
			tileTypes.add (newTileType);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, tileTypes, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

		assertEquals ("TT02", db.findTileType ("TT02", "testFindTileType_Exists").getTileTypeID ());
	}

	/**
	 * Tests the findTileType method to find a tileType ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindTileType_NotExists () throws RecordNotFoundException
	{
		final List<TileType> tileTypes = new ArrayList<TileType> ();
		for (int n = 1; n <= 3; n++)
		{
			final TileType newTileType = new TileType ();
			newTileType.setTileTypeID ("TT0" + n);
			tileTypes.add (newTileType);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, tileTypes, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

		assertNull (db.findTileType ("TT04", "testFindTileType_NotExists"));
	}

	/**
	 * Tests the findProductionType method to find a productionType ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindProductionType_Exists () throws RecordNotFoundException
	{
		final List<ProductionType> productionTypes = new ArrayList<ProductionType> ();
		for (int n = 1; n <= 3; n++)
		{
			final ProductionType newProductionType = new ProductionType ();
			newProductionType.setProductionTypeID ("RE0" + n);
			productionTypes.add (newProductionType);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, productionTypes, null, null, null, null, null, null, null, null, null, null, null, null, null);

		assertEquals ("RE02", db.findProductionType ("RE02", "testFindProductionType_Exists").getProductionTypeID ());
	}

	/**
	 * Tests the findProductionType method to find a productionType ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindProductionType_NotExists () throws RecordNotFoundException
	{
		final List<ProductionType> productionTypes = new ArrayList<ProductionType> ();
		for (int n = 1; n <= 3; n++)
		{
			final ProductionType newProductionType = new ProductionType ();
			newProductionType.setProductionTypeID ("RE0" + n);
			productionTypes.add (newProductionType);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, productionTypes, null, null, null, null, null, null, null, null, null, null, null, null, null);

		assertNull (db.findProductionType ("RE04", "testFindProductionType_NotExists"));
	}

	/**
	 * Tests the getPickTypes method
	 */
	@Test
	public final void testGetPickTypes ()
	{
		final List<PickType> pickTypes = new ArrayList<PickType> ();
		for (int n = 1; n <= 3; n++)
		{
			final PickType newPickType = new PickType ();
			newPickType.setPickTypeID ("PT0" + n);		// Proper values are B and R
			pickTypes.add (newPickType);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, pickTypes, null, null, null, null, null, null, null, null, null, null, null, null);

		assertEquals (3, db.getPickTypes ().size ());
		for (int n = 1; n <= 3; n++)
			assertEquals ("PT0" + n, db.getPickTypes ().get (n - 1).getPickTypeID ());
	}

	/**
	 * Tests the findPickType method to find a pickType ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindPickType_Exists () throws RecordNotFoundException
	{
		final List<PickType> pickTypes = new ArrayList<PickType> ();
		for (int n = 1; n <= 3; n++)
		{
			final PickType newPickType = new PickType ();
			newPickType.setPickTypeID ("PT0" + n);
			pickTypes.add (newPickType);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, pickTypes, null, null, null, null, null, null, null, null, null, null, null, null);

		assertEquals ("PT02", db.findPickType ("PT02", "testFindPickType_Exists").getPickTypeID ());
	}

	/**
	 * Tests the findPickType method to find a pickType ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindPickType_NotExists () throws RecordNotFoundException
	{
		final List<PickType> pickTypes = new ArrayList<PickType> ();
		for (int n = 1; n <= 3; n++)
		{
			final PickType newPickType = new PickType ();
			newPickType.setPickTypeID ("PT0" + n);
			pickTypes.add (newPickType);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, pickTypes, null, null, null, null, null, null, null, null, null, null, null, null);

		assertNull (db.findPickType ("PT04", "testFindPickType_NotExists"));
	}

	/**
	 * Tests the findPick method to find a pick ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindPick_Exists () throws RecordNotFoundException
	{
		final List<Pick> picks = new ArrayList<Pick> ();
		for (int n = 1; n <= 3; n++)
		{
			final Pick newPick = new Pick ();
			newPick.setPickID ("MB0" + n);
			picks.add (newPick);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, picks, null, null, null, null, null, null, null, null, null, null, null);

		assertEquals ("MB02", db.findPick ("MB02", "testFindPick_Exists").getPickID ());
	}

	/**
	 * Tests the findPick method to find a pick ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindPick_NotExists () throws RecordNotFoundException
	{
		final List<Pick> picks = new ArrayList<Pick> ();
		for (int n = 1; n <= 3; n++)
		{
			final Pick newPick = new Pick ();
			newPick.setPickID ("MB0" + n);
			picks.add (newPick);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, picks, null, null, null, null, null, null, null, null, null, null, null);

		assertNull (db.findPick ("MB04", "testFindPick_NotExists"));
	}

	/**
	 * Tests the getWizards method
	 */
	@Test
	public final void testGetWizards ()
	{
		final List<Wizard> wizards = new ArrayList<Wizard> ();
		for (int n = 1; n <= 3; n++)
		{
			final Wizard newWizard = new Wizard ();
			newWizard.setWizardID ("WZ0" + n);
			wizards.add (newWizard);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, wizards, null, null, null, null, null, null, null, null, null, null);

		assertEquals (3, db.getWizards ().size ());
		for (int n = 1; n <= 3; n++)
			assertEquals ("WZ0" + n, db.getWizards ().get (n - 1).getWizardID ());
	}

	/**
	 * Tests the findWizard method to find a wizard ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindWizard_Exists () throws RecordNotFoundException
	{
		final List<Wizard> wizards = new ArrayList<Wizard> ();
		for (int n = 1; n <= 3; n++)
		{
			final Wizard newWizard = new Wizard ();
			newWizard.setWizardID ("WZ0" + n);
			wizards.add (newWizard);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, wizards, null, null, null, null, null, null, null, null, null, null);

		assertEquals ("WZ02", db.findWizard ("WZ02", "testFindWizard_Exists").getWizardID ());
	}

	/**
	 * Tests the findWizard method to find a wizard ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindWizard_NotExists () throws RecordNotFoundException
	{
		final List<Wizard> wizards = new ArrayList<Wizard> ();
		for (int n = 1; n <= 3; n++)
		{
			final Wizard newWizard = new Wizard ();
			newWizard.setWizardID ("WZ0" + n);
			wizards.add (newWizard);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, wizards, null, null, null, null, null, null, null, null, null, null);

		assertNull (db.findWizard ("WZ04", "testFindWizard_NotExists"));
	}

	/**
	 * Tests the findUnitType method to find a unitType ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindUnitType_Exists () throws RecordNotFoundException
	{
		final List<UnitType> unitTypes = new ArrayList<UnitType> ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitType newUnitType = new UnitType ();
			newUnitType.setUnitTypeID ("T" + n);		// Real values are N, H, S
			unitTypes.add (newUnitType);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, unitTypes, null, null, null, null, null, null, null, null, null);

		assertEquals ("T2", db.findUnitType ("T2", "testFindUnitType_Exists").getUnitTypeID ());
	}

	/**
	 * Tests the findUnitType method to find a unitType ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindUnitType_NotExists () throws RecordNotFoundException
	{
		final List<UnitType> unitTypes = new ArrayList<UnitType> ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitType newUnitType = new UnitType ();
			newUnitType.setUnitTypeID ("T" + n);		// Real values are N, H, S
			unitTypes.add (newUnitType);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, unitTypes, null, null, null, null, null, null, null, null, null);

		assertNull (db.findUnitType ("T4", "testFindUnitType_NotExists"));
	}


	/**
	 * Tests the findUnitMagicRealm method to find a unitMagicRealm ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindUnitMagicRealm_Exists () throws RecordNotFoundException
	{
		final List<UnitMagicRealm> unitMagicRealms = new ArrayList<UnitMagicRealm> ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitMagicRealm newUnitMagicRealm = new UnitMagicRealm ();
			newUnitMagicRealm.setUnitMagicRealmID ("LT0" + n);
			unitMagicRealms.add (newUnitMagicRealm);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, unitMagicRealms, null, null, null, null, null, null, null, null);

		assertEquals ("LT02", db.findUnitMagicRealm ("LT02", "testFindUnitMagicRealm_Exists").getUnitMagicRealmID ());
	}

	/**
	 * Tests the findUnitMagicRealm method to find a unitMagicRealm ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindUnitMagicRealm_NotExists () throws RecordNotFoundException
	{
		final List<UnitMagicRealm> unitMagicRealms = new ArrayList<UnitMagicRealm> ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitMagicRealm newUnitMagicRealm = new UnitMagicRealm ();
			newUnitMagicRealm.setUnitMagicRealmID ("LT0" + n);
			unitMagicRealms.add (newUnitMagicRealm);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, unitMagicRealms, null, null, null, null, null, null, null, null);

		assertNull (db.findUnitMagicRealm ("LT04", "testFindUnitMagicRealm_NotExists"));
	}

	/**
	 * Tests the getUnits method
	 */
	@Test
	public final void testGetUnits ()
	{
		final List<Unit> units = new ArrayList<Unit> ();
		for (int n = 1; n <= 3; n++)
		{
			final Unit newUnit = new Unit ();
			newUnit.setUnitID ("UN0" + n);
			units.add (newUnit);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, units, null, null, null, null, null, null, null);

		assertEquals (3, db.getUnits ().size ());
		for (int n = 1; n <= 3; n++)
			assertEquals ("UN0" + n, db.getUnits ().get (n - 1).getUnitID ());
	}

	/**
	 * Tests the findUnit method to find a unit ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindUnit_Exists () throws RecordNotFoundException
	{
		final List<Unit> units = new ArrayList<Unit> ();
		for (int n = 1; n <= 3; n++)
		{
			final Unit newUnit = new Unit ();
			newUnit.setUnitID ("UN0" + n);
			units.add (newUnit);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, units, null, null, null, null, null, null, null);

		assertEquals ("UN02", db.findUnit ("UN02", "testFindUnit_Exists").getUnitID ());
	}

	/**
	 * Tests the findUnit method to find a unit ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindUnit_NotExists () throws RecordNotFoundException
	{
		final List<Unit> units = new ArrayList<Unit> ();
		for (int n = 1; n <= 3; n++)
		{
			final Unit newUnit = new Unit ();
			newUnit.setUnitID ("UN0" + n);
			units.add (newUnit);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, units, null, null, null, null, null, null, null);

		assertNull (db.findUnit ("UN04", "testFindUnit_NotExists"));
	}

	/**
	 * Tests the getUnitSkills method
	 */
	@Test
	public final void testGetUnitSkills ()
	{
		final List<UnitSkill> unitSkills = new ArrayList<UnitSkill> ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitSkill newUnitSkill = new UnitSkill ();
			newUnitSkill.setUnitSkillID ("US00" + n);
			unitSkills.add (newUnitSkill);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, null, unitSkills, null, null, null, null, null, null);

		assertEquals (3, db.getUnitSkills ().size ());
		for (int n = 1; n <= 3; n++)
			assertEquals ("US00" + n, db.getUnitSkills ().get (n - 1).getUnitSkillID ());
	}

	/**
	 * Tests the findUnitSkill method to find a unitSkill ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindUnitSkill_Exists () throws RecordNotFoundException
	{
		final List<UnitSkill> unitSkills = new ArrayList<UnitSkill> ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitSkill newUnitSkill = new UnitSkill ();
			newUnitSkill.setUnitSkillID ("US00" + n);
			unitSkills.add (newUnitSkill);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, null, unitSkills, null, null, null, null, null, null);

		assertEquals ("US002", db.findUnitSkill ("US002", "testFindUnitSkill_Exists").getUnitSkillID ());
	}

	/**
	 * Tests the findUnitSkill method to find a unitSkill ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindUnitSkill_NotExists () throws RecordNotFoundException
	{
		final List<UnitSkill> unitSkills = new ArrayList<UnitSkill> ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitSkill newUnitSkill = new UnitSkill ();
			newUnitSkill.setUnitSkillID ("US00" + n);
			unitSkills.add (newUnitSkill);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, null, unitSkills, null, null, null, null, null, null);

		assertNull (db.findUnitSkill ("US004", "testFindUnitSkill_NotExists"));
	}

	/**
	 * Tests the findWeaponGrade method to find a weaponGradeNumber that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindWeaponGrade_Exists () throws RecordNotFoundException
	{
		final List<WeaponGrade> weaponGrades = new ArrayList<WeaponGrade> ();
		for (int n = 1; n <= 3; n++)
		{
			final WeaponGrade newWeaponGrade = new WeaponGrade ();
			newWeaponGrade.setWeaponGradeNumber (n);
			weaponGrades.add (newWeaponGrade);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, null, null, weaponGrades, null, null, null, null, null);

		assertEquals (2, db.findWeaponGrade (2, "testFindWeaponGrade_Exists").getWeaponGradeNumber ());
	}

	/**
	 * Tests the findWeaponGrade method to find a weaponGradeNumber that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindWeaponGrade_NotExists () throws RecordNotFoundException
	{
		final List<WeaponGrade> weaponGrades = new ArrayList<WeaponGrade> ();
		for (int n = 1; n <= 3; n++)
		{
			final WeaponGrade newWeaponGrade = new WeaponGrade ();
			newWeaponGrade.setWeaponGradeNumber (n);
			weaponGrades.add (newWeaponGrade);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, null, null, weaponGrades, null, null, null, null, null);

		assertNull (db.findWeaponGrade (4, "testFindWeaponGrade_NotExists"));
	}

	/**
	 * Tests the getRaces method
	 */
	@Test
	public final void testGetRaces ()
	{
		final List<Race> races = new ArrayList<Race> ();
		for (int n = 1; n <= 3; n++)
		{
			final Race newRace = new Race ();
			newRace.setRaceID ("RC0" + n);
			races.add (newRace);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, null, null, null, races, null, null, null, null);

		assertEquals (3, db.getRaces ().size ());
		for (int n = 1; n <= 3; n++)
			assertEquals ("RC0" + n, db.getRaces ().get (n - 1).getRaceID ());
	}

	/**
	 * Tests the findRace method to find a race ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindRace_Exists () throws RecordNotFoundException
	{
		final List<Race> races = new ArrayList<Race> ();
		for (int n = 1; n <= 3; n++)
		{
			final Race newRace = new Race ();
			newRace.setRaceID ("RC0" + n);
			races.add (newRace);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, null, null, null, races, null, null, null, null);

		assertEquals ("RC02", db.findRace ("RC02", "testFindRace_Exists").getRaceID ());
	}

	/**
	 * Tests the findRace method to find a race ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindRace_NotExists () throws RecordNotFoundException
	{
		final List<Race> races = new ArrayList<Race> ();
		for (int n = 1; n <= 3; n++)
		{
			final Race newRace = new Race ();
			newRace.setRaceID ("RC0" + n);
			races.add (newRace);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, null, null, null, races, null, null, null, null);

		assertNull (db.findRace ("RC04", "testFindRace_NotExists"));
	}

	/**
	 * Tests the getBuildings method
	 */
	@Test
	public final void testGetBuildings ()
	{
		final List<Building> buildings = new ArrayList<Building> ();
		for (int n = 1; n <= 3; n++)
		{
			final Building newBuilding = new Building ();
			newBuilding.setBuildingID ("BL0" + n);
			buildings.add (newBuilding);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, null, null, null, null, null, buildings, null, null);

		assertEquals (3, db.getBuildings ().size ());
		for (int n = 1; n <= 3; n++)
			assertEquals ("BL0" + n, db.getBuildings ().get (n - 1).getBuildingID ());
	}

	/**
	 * Tests the findBuilding method to find a building ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindBuilding_Exists () throws RecordNotFoundException
	{
		final List<Building> buildings = new ArrayList<Building> ();
		for (int n = 1; n <= 3; n++)
		{
			final Building newBuilding = new Building ();
			newBuilding.setBuildingID ("BL0" + n);
			buildings.add (newBuilding);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, null, null, null, null, null, buildings, null, null);

		assertEquals ("BL02", db.findBuilding ("BL02", "testFindBuilding_Exists").getBuildingID ());
	}

	/**
	 * Tests the findBuilding method to find a building ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindBuilding_NotExists () throws RecordNotFoundException
	{
		final List<Building> buildings = new ArrayList<Building> ();
		for (int n = 1; n <= 3; n++)
		{
			final Building newBuilding = new Building ();
			newBuilding.setBuildingID ("BL0" + n);
			buildings.add (newBuilding);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, null, null, null, null, null, buildings, null, null);

		assertNull (db.findBuilding ("BL04", "testFindBuilding_NotExists"));
	}

	/**
	 * Tests the getSpells method
	 */
	@Test
	public final void testGetSpells ()
	{
		final List<Spell> spells = new ArrayList<Spell> ();
		for (int n = 1; n <= 3; n++)
		{
			final Spell newSpell = new Spell ();
			newSpell.setSpellID ("SP00" + n);
			spells.add (newSpell);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, spells, null);

		assertEquals (3, db.getSpells ().size ());
		for (int n = 1; n <= 3; n++)
			assertEquals ("SP00" + n, db.getSpells ().get (n - 1).getSpellID ());
	}

	/**
	 * Tests the findSpell method to find a spell ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindSpell_Exists () throws RecordNotFoundException
	{
		final List<Spell> spells = new ArrayList<Spell> ();
		for (int n = 1; n <= 3; n++)
		{
			final Spell newSpell = new Spell ();
			newSpell.setSpellID ("SP00" + n);
			spells.add (newSpell);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, spells, null);

		assertEquals ("SP002", db.findSpell ("SP002", "testFindSpell_Exists").getSpellID ());
	}

	/**
	 * Tests the findSpell method to find a spell ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindSpell_NotExists () throws RecordNotFoundException
	{
		final List<Spell> spells = new ArrayList<Spell> ();
		for (int n = 1; n <= 3; n++)
		{
			final Spell newSpell = new Spell ();
			newSpell.setSpellID ("SP00" + n);
			spells.add (newSpell);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, spells, null);

		assertNull (db.findSpell ("SP004", "testFindSpell_NotExists"));
	}

	/**
	 * Tests the findCombatAreaEffect method to find a combatAreaEffect ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindCombatAreaEffect_Exists () throws RecordNotFoundException
	{
		final List<CombatAreaEffect> combatAreaEffects = new ArrayList<CombatAreaEffect> ();
		for (int n = 1; n <= 3; n++)
		{
			final CombatAreaEffect newCombatAreaEffect = new CombatAreaEffect ();
			newCombatAreaEffect.setCombatAreaEffectID ("CAE0" + n);
			combatAreaEffects.add (newCombatAreaEffect);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, combatAreaEffects);

		assertEquals ("CAE02", db.findCombatAreaEffect ("CAE02", "testFindCombatAreaEffect_Exists").getCombatAreaEffectID ());
	}

	/**
	 * Tests the findCombatAreaEffect method to find a combatAreaEffect ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindCombatAreaEffect_NotExists () throws RecordNotFoundException
	{
		final List<CombatAreaEffect> combatAreaEffects = new ArrayList<CombatAreaEffect> ();
		for (int n = 1; n <= 3; n++)
		{
			final CombatAreaEffect newCombatAreaEffect = new CombatAreaEffect ();
			newCombatAreaEffect.setCombatAreaEffectID ("CAE0" + n);
			combatAreaEffects.add (newCombatAreaEffect);
		}

		final CommonDatabaseLookup db = new CommonDatabaseLookup (null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, combatAreaEffects);

		assertNull (db.findCombatAreaEffect ("CAE04", "testFindCombatAreaEffect_NotExists"));
	}
}
