package momime.common.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.utils.swing.NdgUIUtils;

import momime.common.messages.MemoryBuilding;
import momime.common.utils.MemoryBuildingUtils;

/**
 * Tests the CommonDatabaseImpl class
 * Note there's no point testing any of the 'get' methods that return the complete lists, since they're provided by the JAXB-generated code
 * Only need to test the maps that we coded in the Ex class
 */
@ExtendWith(MockitoExtension.class)
public final class TestCommonDatabaseImpl
{
	/**
	 * Tests the findPlaneID method to find a plane ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindPlaneID_Exists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 0; n < 2; n++)
		{
			final Plane newPlane = new Plane ();
			newPlane.setPlaneNumber (n);
			newPlane.setPrerequisitePickToChooseNativeRace ("Plane " + n);

			db.getPlane ().add (newPlane);
		}

		db.buildMaps ();

		assertEquals ("Plane 1", db.findPlane (1, "testFindPlaneID_Exists").getPrerequisitePickToChooseNativeRace ());
	}

	/**
	 * Tests the findPlaneID method to find a plane ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test
	public final void testFindPlaneID_NotExists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 0; n < 2; n++)
		{
			final Plane newPlane = new Plane ();
			newPlane.setPlaneNumber (n);
			newPlane.setPrerequisitePickToChooseNativeRace ("Plane " + n);

			db.getPlane ().add (newPlane);
		}

		db.buildMaps ();

		assertThrows (RecordNotFoundException.class, () ->
		{
			db.findPlane (2, "testFindPlaneID_NotExists");
		});
	}

	/**
	 * Tests the findMapFeatureID method to find a mapFeature ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindMapFeatureID_Exists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final MapFeatureEx newMapFeature = new MapFeatureEx ();
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
	@Test
	public final void testFindMapFeatureID_NotExists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final MapFeatureEx newMapFeature = new MapFeatureEx ();
			newMapFeature.setMapFeatureID ("MF0" + n);
			db.getMapFeature ().add (newMapFeature);
		}

		db.buildMaps ();

		assertThrows (RecordNotFoundException.class, () ->
		{
			db.findMapFeature ("MF04", "testFindMapFeatureID_NotExists");
		});
	}

	/**
	 * Tests the findTileType method to find a tileType ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindTileType_Exists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final TileTypeEx newTileType = new TileTypeEx ();
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
	@Test
	public final void testFindTileType_NotExists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final TileTypeEx newTileType = new TileTypeEx ();
			newTileType.setTileTypeID ("TT0" + n);
			db.getTileType ().add (newTileType);
		}

		db.buildMaps ();

		assertThrows (RecordNotFoundException.class, () ->
		{
			assertNull (db.findTileType ("TT04", "testFindTileType_NotExists"));
		});
	}

	/**
	 * Tests the findProductionTypeID method to find a productionType ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindProductionTypeID_Exists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final ProductionTypeEx newProductionType = new ProductionTypeEx ();
			newProductionType.setProductionTypeID ("RE0" + n);
			newProductionType.setAccumulatesInto ("ProductionType " + n);

			db.getProductionType ().add (newProductionType);
		}

		db.buildMaps ();

		assertEquals ("RE02", db.findProductionType ("RE02", "testFindProductionTypeID_Exists").getProductionTypeID ());
		assertEquals ("ProductionType 2", db.findProductionType ("RE02", "testFindProductionTypeID_Exists").getAccumulatesInto ());
	}

	/**
	 * Tests the findProductionTypeID method to find a productionType ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test
	public final void testFindProductionTypeID_NotExists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final ProductionTypeEx newProductionType = new ProductionTypeEx ();
			newProductionType.setProductionTypeID ("RE0" + n);
			newProductionType.setAccumulatesInto ("ProductionType " + n);

			db.getProductionType ().add (newProductionType);
		}

		db.buildMaps ();

		assertThrows (RecordNotFoundException.class, () ->
		{
			db.findProductionType ("RE04", "testFindProductionTypeID_NotExists");
		});
	}

	/**
	 * Tests the findPickTypeID method to find a pickType ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindPickTypeID_Exists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final PickType newPickType = new PickType ();
			newPickType.setPickTypeID ("PT0" + n);

			final PickTypeCountContainer container = new PickTypeCountContainer ();
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
	@Test
	public final void testFindPickTypeID_NotExists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final PickType newPickType = new PickType ();
			newPickType.setPickTypeID ("PT0" + n);
			db.getPickType ().add (newPickType);
		}

		db.buildMaps ();

		assertThrows (RecordNotFoundException.class, () ->
		{
			db.findPickType ("PT04", "testFindPickTypeID_NotExists");
		});
	}

	/**
	 * Tests the findPickID method to find a pick ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindPickID_Exists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final Pick newPick = new Pick ();
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
	@Test
	public final void testFindPickID_NotExists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final Pick newPick = new Pick ();
			newPick.setPickID ("MB0" + n);
			db.getPick ().add (newPick);
		}

		db.buildMaps ();

		assertThrows (RecordNotFoundException.class, () ->
		{
			db.findPick ("MB04", "testFindPickID_NotExists");
		});
	}

	/**
	 * Tests the findWizardID method to find a wizard ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindWizardID_Exists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final WizardEx newWizard = new WizardEx ();
			newWizard.setWizardID ("WZ0" + n);

			final WizardPickCount pick = new WizardPickCount ();
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
	@Test
	public final void testFindWizardID_NotExists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final WizardEx newWizard = new WizardEx ();
			newWizard.setWizardID ("WZ0" + n);
			db.getWizard ().add (newWizard);
		}

		db.buildMaps ();

		assertThrows (RecordNotFoundException.class, () ->
		{
			db.findWizard ("WZ04", "testFindWizardID_NotExists");
		});
	}

	/**
	 * Tests the findCombatAction method to find a combatAction ID that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindCombatAction_Exists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CombatAction newCombatAction = new CombatAction ();
			newCombatAction.setCombatActionID ("CMB0" + n);
			db.getCombatAction ().add (newCombatAction);
		}

		db.buildMaps ();

		assertEquals ("CMB02", db.findCombatAction ("CMB02", "testFindCombatAction_Exists").getCombatActionID ());
	}

	/**
	 * Tests the findCombatAction method to find a combatAction ID that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindCombatAction_NotExists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CombatAction newCombatAction = new CombatAction ();
			newCombatAction.setCombatActionID ("CMB0" + n);
			db.getCombatAction ().add (newCombatAction);
		}

		db.buildMaps ();

		assertThrows (RecordNotFoundException.class, () ->
		{
			db.findCombatAction ("CMB04", "testFindCombatAction_NotExists");
		});
	}
	
	/**
	 * Tests the findUnitType method to find a unitType ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindUnitType_Exists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitTypeEx newUnitType = new UnitTypeEx ();
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
	@Test
	public final void testFindUnitType_NotExists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitTypeEx newUnitType = new UnitTypeEx ();
			newUnitType.setUnitTypeID ("T" + n);		// Real values are N, H, S
			db.getUnitType ().add (newUnitType);
		}

		db.buildMaps ();

		assertThrows (RecordNotFoundException.class, () ->
		{
			assertNull (db.findUnitType ("T4", "testFindUnitType_NotExists"));
		});
	}

	/**
	 * Tests the findUnitID method to find a unit ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindUnitID_Exists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitEx newUnit = new UnitEx ();
			newUnit.setUnitID ("UN00" + n);
			newUnit.setUnitMagicRealm ("Unit name " + n);

			db.getUnit ().add (newUnit);
		}

		db.buildMaps ();

		assertEquals ("UN002", db.findUnit ("UN002", "testFindUnitID_Exists").getUnitID ());
		assertEquals ("Unit name 2", db.findUnit ("UN002", "testFindUnitID_Exists").getUnitMagicRealm ());
	}

	/**
	 * Tests the findUnitID method to find a unit ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test
	public final void testFindUnitID_NotExists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitEx newUnit = new UnitEx ();
			newUnit.setUnitID ("UN00" + n);
			db.getUnit ().add (newUnit);
		}

		db.buildMaps ();

		assertThrows (RecordNotFoundException.class, () ->
		{
			db.findUnit ("UN004", "testFindUnitID_NotExists");
		});
	}

	/**
	 * Tests the findUnitSkillID method to find a unit skill ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindUnitSkillID_Exists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitSkillEx newUnitSkill = new UnitSkillEx ();
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
	@Test
	public final void testFindUnitSkillID_NotExists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitSkillEx newUnitSkill = new UnitSkillEx ();
			newUnitSkill.setUnitSkillID ("US00" + n);
			newUnitSkill.setUnitSkillScoutingRange (n);

			db.getUnitSkill ().add (newUnitSkill);
		}

		db.buildMaps ();

		assertThrows (RecordNotFoundException.class, () ->
		{
			db.findUnitSkill ("US004", "testFindUnitSkillID_NotExists");
		});
	}

	/**
	 * Tests the findWeaponGrade method to find a weaponGradeNumber that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindWeaponGrade_Exists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
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
	@Test
	public final void testFindWeaponGrade_NotExists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final WeaponGrade newWeaponGrade = new WeaponGrade ();
			newWeaponGrade.setWeaponGradeNumber (n);
			db.getWeaponGrade ().add (newWeaponGrade);
		}

		db.buildMaps ();

		assertThrows (RecordNotFoundException.class, () ->
		{
			assertNull (db.findWeaponGrade (4, "testFindWeaponGrade_NotExists"));
		});
	}
	
	/**
	 * Tests the findRangedAttackType method to find a rangedAttackTypeNumber that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindRangedAttackType_Exists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final RangedAttackTypeEx newRangedAttackType = new RangedAttackTypeEx ();
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
	@Test
	public final void testFindRangedAttackType_NotExists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final RangedAttackTypeEx newRangedAttackType = new RangedAttackTypeEx ();
			newRangedAttackType.setRangedAttackTypeID ("RAT0" + n);
			db.getRangedAttackType ().add (newRangedAttackType);
		}

		db.buildMaps ();

		assertThrows (RecordNotFoundException.class, () ->
		{
			assertNull (db.findRangedAttackType ("RAT04", "testFindRangedAttackType_NotExists"));
		});
	}
	
	/**
	 * Tests the findRaceID method to find a race ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindRaceID_Exists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final RaceEx newRace = new RaceEx ();
			newRace.setRaceID ("RC0" + n);

			newRace.getCityName ().add ("Blah");

			db.getRace ().add (newRace);
		}

		db.buildMaps ();

		assertEquals ("RC02", db.findRace ("RC02", "testFindRaceID_Exists").getRaceID ());
		assertEquals (1, db.findRace ("RC02", "testFindRaceID_Exists").getCityName ().size ());
		assertEquals ("Blah", db.findRace ("RC02", "testFindRaceID_Exists").getCityName ().get (0));
	}

	/**
	 * Tests the findRaceID method to find a race ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test
	public final void testFindRaceID_NotExists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final RaceEx newRace = new RaceEx ();
			newRace.setRaceID ("RC0" + n);
			db.getRace ().add (newRace);
		}

		db.buildMaps ();

		assertThrows (RecordNotFoundException.class, () ->
		{
			db.findRace ("RC04", "testFindRaceID_NotExists");
		});
	}

	/**
	 * Tests the findBuilding method to find a building ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindBuilding_Exists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final Building newBuilding = new Building ();
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
	@Test
	public final void testFindBuilding_NotExists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final Building newBuilding = new Building ();
			newBuilding.setBuildingID ("BL0" + n);
			newBuilding.setBuildingScoutingRange (n);
			db.getBuilding ().add (newBuilding);
		}

		db.buildMaps ();

		assertThrows (RecordNotFoundException.class, () ->
		{
			assertNull (db.findBuilding ("BL04", "testFindBuilding_NotExists"));
		});
	}

	/**
	 * Tests the findSpellID method to find a spell ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindSpellID_Exists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final Spell newSpell = new Spell ();
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
	@Test
	public final void testFindSpellID_NotExists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final Spell newSpell = new Spell ();
			newSpell.setSpellID ("SP00" + n);
			db.getSpell ().add (newSpell);
		}

		db.buildMaps ();

		assertThrows (RecordNotFoundException.class, () ->
		{
			db.findSpell ("SP004", "testFindSpellID_NotExists");
		});
	}

	/**
	 * Tests the findCityViewElementBuilding method to find a building ID that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindCityViewElementBuilding_Exists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CityViewElement newBuilding = new CityViewElement ();
			newBuilding.setBuildingID ("BL0" + n);
			db.getCityViewElement ().add (newBuilding);
		}

		db.buildMaps ();

		assertEquals ("BL02", db.findCityViewElementBuilding ("BL02", "testFindCityViewElementBuilding_Exists").getBuildingID ());
	}

	/**
	 * Tests the findCityViewElementBuilding method to find a building ID that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindCityViewElementBuilding_NotExists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CityViewElement newBuilding = new CityViewElement ();
			newBuilding.setBuildingID ("BL0" + n);
			db.getCityViewElement ().add (newBuilding);
		}

		db.buildMaps ();

		assertThrows (RecordNotFoundException.class, () ->
		{
			db.findCityViewElementBuilding ("BL04", "testFindCityViewElementBuilding_NotExists");
		});
	}

	/**
	 * Tests the findCityViewElementSpellEffect method to find a citySpellEffect ID that does exist
	 */
	@Test
	public final void testFindCityViewElementSpellEffect ()
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CityViewElement newCitySpellEffect = new CityViewElement ();
			newCitySpellEffect.setCitySpellEffectID ("CSE0" + n);
			db.getCityViewElement ().add (newCitySpellEffect);
		}

		db.buildMaps ();

		assertEquals ("CSE02", db.findCityViewElementSpellEffect ("CSE02").getCitySpellEffectID ());
		assertNull (db.findCityViewElementSpellEffect ("CSE04"));
	}
	
	/**
	 * Tests the findCombatAreaEffect method to find a combatAreaEffect ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindCombatAreaEffect_Exists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
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
	@Test
	public final void testFindCombatAreaEffect_NotExists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CombatAreaEffect newCombatAreaEffect = new CombatAreaEffect ();
			newCombatAreaEffect.setCombatAreaEffectID ("CAE0" + n);
			db.getCombatAreaEffect ().add (newCombatAreaEffect);
		}

		db.buildMaps ();

		assertThrows (RecordNotFoundException.class, () ->
		{
			assertNull (db.findCombatAreaEffect ("CAE04", "testFindCombatAreaEffect_NotExists"));
		});
	}

	/**
	 * Tests the findCombatTileType method to find a combatTileType ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindCombatTileType_Exists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
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
	@Test
	public final void testFindCombatTileType_NotExists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CombatTileType newCombatTileType = new CombatTileType ();
			newCombatTileType.setCombatTileTypeID ("CTL0" + n);
			db.getCombatTileType ().add (newCombatTileType);
		}

		db.buildMaps ();

		assertThrows (RecordNotFoundException.class, () ->
		{
			assertNull (db.findCombatTileType ("CTL04", "testFindCombatTileType_NotExists"));
		});
	}

	/**
	 * Tests the findCombatTileBorder method to find a combatTileBorder ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindCombatTileBorder_Exists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
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
	@Test
	public final void testFindCombatTileBorder_NotExists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CombatTileBorder newCombatTileBorder = new CombatTileBorder ();
			newCombatTileBorder.setCombatTileBorderID ("CTB0" + n);
			db.getCombatTileBorder ().add (newCombatTileBorder);
		}

		db.buildMaps ();

		assertThrows (RecordNotFoundException.class, () ->
		{
			assertNull (db.findCombatTileBorder ("CTB04", "testFindCombatTileBorder_NotExists"));
		});
	}
	
	/**
	 * Tests the findCitySpellEffect method to find a citySpellEffect ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindCitySpellEffect_Exists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CitySpellEffect newCitySpellEffect = new CitySpellEffect ();
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
	@Test
	public final void testFindCitySpellEffect_NotExists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CitySpellEffect newCitySpellEffect = new CitySpellEffect ();
			newCitySpellEffect.setCitySpellEffectID ("SE00" + n);
			db.getCitySpellEffect ().add (newCitySpellEffect);
		}

		db.buildMaps ();

		assertThrows (RecordNotFoundException.class, () ->
		{
			assertNull (db.findCitySpellEffect ("SE004", "testFindCitySpellEffect_NotExists"));
		});
	}

	/**
	 * Tests the findHeroItemSlotType method to find a heroItemSlotType ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindHeroItemSlotType_Exists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
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
	@Test
	public final void testFindHeroItemSlotType_NotExists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final HeroItemSlotType newHeroItemSlotType = new HeroItemSlotType ();
			newHeroItemSlotType.setHeroItemSlotTypeID ("IST0" + n);
			db.getHeroItemSlotType ().add (newHeroItemSlotType);
		}

		db.buildMaps ();

		assertThrows (RecordNotFoundException.class, () ->
		{
			assertNull (db.findHeroItemSlotType ("IST04", "testFindHeroItemSlotType_NotExists"));
		});
	}

	/**
	 * Tests the findHeroItemType method to find a heroItemType ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindHeroItemType_Exists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
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
	@Test
	public final void testFindHeroItemType_NotExists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final HeroItemType newHeroItemType = new HeroItemType ();
			newHeroItemType.setHeroItemTypeID ("IT0" + n);
			db.getHeroItemType ().add (newHeroItemType);
		}

		db.buildMaps ();

		assertThrows (RecordNotFoundException.class, () ->
		{
			assertNull (db.findHeroItemType ("IT04", "testFindHeroItemType_NotExists"));
		});
	}

	/**
	 * Tests the findHeroItemBonus method to find a heroItemBonus ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindHeroItemBonus_Exists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
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
	@Test
	public final void testFindHeroItemBonus_NotExists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final HeroItemBonus newHeroItemBonus = new HeroItemBonus ();
			newHeroItemBonus.setHeroItemBonusID ("IB0" + n);
			db.getHeroItemBonus ().add (newHeroItemBonus);
		}

		db.buildMaps ();

		assertThrows (RecordNotFoundException.class, () ->
		{
			assertNull (db.findHeroItemBonus ("IB04", "testFindHeroItemBonus_NotExists"));
		});
	}

	/**
	 * Tests the findDamageType method to find a damageType ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindDamageType_Exists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final DamageType newDamageType = new DamageType ();
			newDamageType.setDamageTypeID ("DT0" + n);
			db.getDamageType ().add (newDamageType);
		}

		db.buildMaps ();

		assertEquals ("DT02", db.findDamageType ("DT02", "testFindDamageType_Exists").getDamageTypeID ());
	}

	/**
	 * Tests the findDamageType method to find a damageType ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test
	public final void testFindDamageType_NotExists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final DamageType newDamageType = new DamageType ();
			newDamageType.setDamageTypeID ("DT0" + n);
			db.getDamageType ().add (newDamageType);
		}

		db.buildMaps ();

		assertThrows (RecordNotFoundException.class, () ->
		{
			assertNull (db.findDamageType ("DT04", "testFindDamageType_NotExists"));
		});
	}
	
	/**
	 * Tests the findCombatTileBorderImages method
	 */
	@Test
	public final void testFindCombatTileBorderImages ()
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CombatTileBorderImage newCtb = new CombatTileBorderImage ();
			newCtb.setCombatTileBorderID ("CTB0" + n);
			newCtb.setDirections ("1");
			newCtb.setFrontOrBack (FrontOrBack.FRONT);
			newCtb.setSortPosition (n);
			db.getCombatTileBorderImage ().add (newCtb);
		}
		
		db.buildMaps ();
		
		assertEquals (2, db.findCombatTileBorderImages ("CTB02", "1", FrontOrBack.FRONT).getSortPosition ());
		assertNull (db.findCombatTileBorderImages ("CTB04", "1", FrontOrBack.FRONT));
		assertNull (db.findCombatTileBorderImages ("CTB02", "1", FrontOrBack.BACK));
	}
	
	/**
	 * Tests the findBestCityImage method
	 * @throws RecordNotFoundException If no city size entries match the requested citySizeID
	 */
	@Test
	public final void testFindBestCityImage_Exists () throws RecordNotFoundException
	{
		// City location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (25, 10, 1);

		// Buildings that we have
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		
		when (memoryBuildingUtils.findBuilding (buildings, cityLocation, "BL01")).thenReturn (new MemoryBuilding ());
		when (memoryBuildingUtils.findBuilding (buildings, cityLocation, "BL02")).thenReturn (null);
		
		// Set up object to test
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		db.setMemoryBuildingUtils (memoryBuildingUtils);
		
		// An image for CS01 needing no buildings
		final CityImage image1 = new CityImage ();
		image1.setCitySizeID ("CS01");
		db.getCityImage ().add (image1);
		
		// An image for CS01 needing a building that we have (this should get chosen)
		final CityImage image2 = new CityImage ();
		image2.setCitySizeID ("CS01");
		image2.getCityImagePrerequisite ().add ("BL01");
		db.getCityImage ().add (image2);

		// An image for CS01 needing a building that we have plus one that we don't
		final CityImage image3 = new CityImage ();
		image3.setCitySizeID ("CS01");
		image3.getCityImagePrerequisite ().add ("BL01");
		image3.getCityImagePrerequisite ().add ("BL02");
		db.getCityImage ().add (image3);
		
		// An image for CS02 needing two building that we have
		final CityImage image4 = new CityImage ();
		image4.setCitySizeID ("CS02");
		image4.getCityImagePrerequisite ().add ("BL01");
		image4.getCityImagePrerequisite ().add ("BL03");
		db.getCityImage ().add (image4);
		
		// Run test
		assertSame (image2, db.findBestCityImage ("CS01", cityLocation, buildings, "testFindBestCityImage"));
	}
	
	/**
	 * Tests the findBestCityImage method looking for a citySizeID that doesn't exist
	 * @throws RecordNotFoundException If no city size entries match the requested citySizeID
	 */
	@Test
	public final void testFindBestCityImage_NotExists () throws RecordNotFoundException
	{
		// City location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (25, 10, 1);

		// Buildings that we have
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		
		// Set up object to test
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		db.setMemoryBuildingUtils (memoryBuildingUtils);
		
		// An image for CS01 needing no buildings
		final CityImage image1 = new CityImage ();
		image1.setCitySizeID ("CS01");
		db.getCityImage ().add (image1);
		
		// Run test
		assertThrows (RecordNotFoundException.class, () ->
		{
			db.findBestCityImage ("CS02", cityLocation, buildings, "testFindBestCityImage");
		});
	}
	
	/**
	 * Tests the findTileSet method to find a tile set ID that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindTileSet_Exists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final TileSetEx newTileSet = new TileSetEx ();
			newTileSet.setTileSetID ("WZ0" + n);
			db.getTileSet ().add (newTileSet);
		}

		db.buildMaps ();

		assertEquals ("WZ02", db.findTileSet ("WZ02", "testFindTileSet_Exists").getTileSetID ());
	}

	/**
	 * Tests the findTileSet method to find a tile set ID that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindTileSet_NotExists () throws RecordNotFoundException
	{
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final TileSetEx newTileSet = new TileSetEx ();
			newTileSet.setTileSetID ("WZ0" + n);
			db.getTileSet ().add (newTileSet);
		}

		db.buildMaps ();

		assertThrows (RecordNotFoundException.class, () ->
		{
			db.findTileSet ("WZ04", "testFindTileSet_NotExists");
		});
	}

	/**
	 * Tests the findAnimation method to find a animation ID that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindAnimation_Exists () throws RecordNotFoundException
	{
		// Set up object to test
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final AnimationEx newAnimation = new AnimationEx ();
			newAnimation.setAnimationID ("AN0" + n);
			db.getAnimation ().add (newAnimation);
		}

		db.buildMaps ();

		// Check results
		assertEquals ("AN02", db.findAnimation ("AN02", "testFindAnimation_Exists").getAnimationID ());
	}

	/**
	 * Tests the findAnimation method to find a animation ID that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindAnimation_NotExists () throws RecordNotFoundException
	{
		// Set up object to test
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final AnimationEx newAnimation = new AnimationEx ();
			newAnimation.setAnimationID ("AN0" + n);
			db.getAnimation ().add (newAnimation);
		}

		db.buildMaps ();

		// Check results
		assertThrows (RecordNotFoundException.class, () ->
		{
			db.findAnimation ("AN04", "testFindAnimation_NotExists");
		});
	}

	/**
	 * Tests the findPlayList method to find a play list ID that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindPlayList_Exists () throws RecordNotFoundException
	{
		// Set up object to test
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final PlayList newPlayList = new PlayList ();
			newPlayList.setPlayListID ("PL0" + n);
			db.getPlayList ().add (newPlayList);
		}

		db.buildMaps ();

		// Check results
		assertEquals ("PL02", db.findPlayList ("PL02", "testFindPlayList_Exists").getPlayListID ());
	}

	/**
	 * Tests the findPlayList method to find a play list ID that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindPlayList_NotExists () throws RecordNotFoundException
	{
		// Set up object to test
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final PlayList newPlayList = new PlayList ();
			newPlayList.setPlayListID ("PL0" + n);
			db.getPlayList ().add (newPlayList);
		}

		db.buildMaps ();

		// Check results
		assertThrows (RecordNotFoundException.class, () ->
		{
			db.findPlayList ("PL04", "testFindPlayList_NotExists");
		});
	}
	
	/**
	 * Tests the derivation of the mostExpensiveConstructionCost
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testMostExpensiveConstructionCost () throws Exception
	{
		// Set up some sample buildings and units
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		
		final UnitSkillEx walking = new UnitSkillEx ();
		walking.setUnitSkillID ("USX01");
		walking.setMovementIconImagePreference (1);
		db.getUnitSkills ().add (walking);
		
		final Building building1 = new Building ();
		building1.setBuildingID ("BL01");
		building1.setProductionCost (30);
		db.getBuilding ().add (building1);

		final Building building2 = new Building ();
		building2.setBuildingID ("BL02");
		db.getBuilding ().add (building2);

		final UnitEx unit1 = new UnitEx ();
		unit1.setUnitID ("UN001");
		unit1.setProductionCost (40);
		unit1.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		db.getUnit ().add (unit1);

		final UnitEx unit2 = new UnitEx ();
		unit2.setUnitID ("UN002");
		unit2.setProductionCost (50);
		unit2.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
		db.getUnit ().add (unit2);

		final UnitEx unit3 = new UnitEx ();
		unit3.setUnitID ("UN003");
		unit3.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		db.getUnit ().add (unit3);
		
		// Need to assign skill values to pass consistency checks
		for (final Unit unitDef : db.getUnit ())
			for (final String unitSkillID : new String []
				{CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE,
					CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED, "USX01"})
			{
				final UnitSkillAndValue skillValue = new UnitSkillAndValue ();
				skillValue.setUnitSkillID (unitSkillID);
				skillValue.setUnitSkillValue (1);
				unitDef.getUnitHasSkill ().add (skillValue);
			}
		
		// Run method
		db.buildMaps ();
		db.consistencyChecks ();
		
		// Check results
		assertEquals (40, db.getMostExpensiveConstructionCost ());
	}

	/**
	 * Tests the derivation of the largestBuildingSize
	 * @throws IOException If there is a problem
	 */
	@Test
	public final void testLargestBuildingSize () throws IOException
	{
		// Set up some sample images
		final NdgUIUtils utils = mock (NdgUIUtils.class);
		
		final CommonDatabaseImpl db = new CommonDatabaseImpl ();
		db.setUtils (utils);
		
		// 30 x 20 image
		when (utils.loadImage ("building1.png")).thenReturn (new BufferedImage (30, 20, BufferedImage.TYPE_INT_ARGB));
		
		final CityViewElement building1 = new CityViewElement ();
		building1.setBuildingID ("BL01");
		building1.setCityViewImageFile ("building1.png");
		db.getCityViewElement ().add (building1);
		
		// 50 x 10 image, note this is the alternate image
		when (utils.loadImage ("building2alt.png")).thenReturn (new BufferedImage (50, 10, BufferedImage.TYPE_INT_ARGB));

		final CityViewElement building2 = new CityViewElement ();
		building2.setBuildingID ("BL02");
		building2.setCityViewImageFile ("building2.png");
		building2.setCityViewAlternativeImageFile ("building2alt.png");
		db.getCityViewElement ().add (building2);
		
		// 10 x 40 animation
		when (utils.loadImage ("frame1.png")).thenReturn (new BufferedImage (10, 40, BufferedImage.TYPE_INT_ARGB));
		when (utils.loadImage ("frame2.png")).thenReturn (new BufferedImage (10, 40, BufferedImage.TYPE_INT_ARGB));
		
		final AnimationEx anim = new AnimationEx ();
		anim.setAnimationID ("building3");
		
		for (int n = 1; n <= 2; n++)
		{
			final AnimationFrame frame = new AnimationFrame ();
			frame.setImageFile ("frame" + n + ".png");
			anim.getFrame ().add (frame);
		}
		
		anim.setUtils (utils);
		db.getAnimation ().add (anim);

		final CityViewElement building3 = new CityViewElement ();
		building3.setBuildingID ("BL03");
		building3.setCityViewAnimation ("building3");
		db.getCityViewElement ().add (building3);
		
		// Huge image that isn't a building
		final CityViewElement nonBuilding = new CityViewElement ();
		nonBuilding.setCityViewImageFile ("nonBuilding.png");
		db.getCityViewElement ().add (nonBuilding);
		
		// Huge Wizard's Fortress to prove that it gets ignored
		final CityViewElement fortress = new CityViewElement ();
		fortress.setBuildingID (CommonDatabaseConstants.BUILDING_FORTRESS);
		fortress.setCityViewImageFile ("fortress.png");
		db.getCityViewElement ().add (fortress);
		
		// Necessary as clientConsistencyChecks expects to find this
		final TileSetEx overlandMapTileSet = new TileSetEx ();
		overlandMapTileSet.setTileSetID (CommonDatabaseConstants.TILE_SET_OVERLAND_MAP);
		db.getTileSets ().add (overlandMapTileSet);
		
		// Run method
		db.buildMaps ();
		db.clientConsistencyChecks ();
		final Dimension largestBuildingSize = db.getLargestBuildingSize ();
		
		// Check results
		assertEquals (50, largestBuildingSize.width);
		assertEquals (40, largestBuildingSize.height);
	}
}