package momime.client.graphics.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import momime.client.graphics.database.v0_9_5.AnimationFrame;
import momime.client.graphics.database.v0_9_5.CityImage;
import momime.client.graphics.database.v0_9_5.CityImagePrerequisite;
import momime.client.graphics.database.v0_9_5.CityViewElement;
import momime.client.graphics.database.v0_9_5.Pick;
import momime.client.graphics.database.v0_9_5.UnitSkill;
import momime.client.graphics.database.v0_9_5.WeaponGrade;
import momime.client.graphics.database.v0_9_5.Wizard;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_5.MemoryBuilding;
import momime.common.utils.MemoryBuildingUtils;

import org.junit.Test;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.swing.NdgUIUtils;

/**
 * Tests the GraphicsDatabaseExImpl class
 */
public final class TestGraphicsDatabaseExImpl
{
	/**
	 * Tests the derivation of the largestBuildingSize
	 * @throws IOException If there is a problem
	 */
	@Test
	public final void testLargestBuildingSize () throws IOException
	{
		// Set up some sample images
		final NdgUIUtils utils = mock (NdgUIUtils.class);
		
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		db.setUtils (utils);
		
		final TileSetEx overlandMapTileSet = new TileSetEx ();
		overlandMapTileSet.setTileSetID (GraphicsDatabaseConstants.VALUE_TILE_SET_OVERLAND_MAP);
		db.getTileSet ().add (overlandMapTileSet);
		
		// 30 x 20 image
		when (utils.loadImage ("building1.png")).thenReturn (new BufferedImage (30, 20, BufferedImage.TYPE_INT_ARGB));
		
		final CityViewElement building1 = new CityViewElement ();
		building1.setBuildingID ("BL01");
		building1.setCityViewImageFile ("building1.png");
		db.getCityViewElement ().add (building1);
		
		// 50 x 10 image, note this is the alternate image
		when (utils.loadImage ("building2.png")).thenReturn (new BufferedImage (60, 70, BufferedImage.TYPE_INT_ARGB));
		when (utils.loadImage ("building2alt.png")).thenReturn (new BufferedImage (50, 10, BufferedImage.TYPE_INT_ARGB));

		final CityViewElement building2 = new CityViewElement ();
		building2.setBuildingID ("BL02");
		building2.setCityViewImageFile ("building2.png");
		building2.setCityViewAlternativeImageFile ("building2alt.png");
		db.getCityViewElement ().add (building2);
		
		// 10 x 40 animation
		when (utils.loadImage ("frame1.png")).thenReturn (new BufferedImage (10, 40, BufferedImage.TYPE_INT_ARGB));
		when (utils.loadImage ("frame2.png")).thenReturn (new BufferedImage (10, 40, BufferedImage.TYPE_INT_ARGB));
		
		final AnimationFrame frame1 = new AnimationFrame ();
		frame1.setFrameImageFile ("frame1.png");

		final AnimationFrame frame2 = new AnimationFrame ();
		frame2.setFrameImageFile ("frame2.png");
		
		final AnimationEx anim = new AnimationEx ();
		anim.setAnimationID ("building3");
		anim.getFrame ().add (frame1);
		anim.getFrame ().add (frame2);
		anim.setUtils (utils);
		db.getAnimation ().add (anim);

		final CityViewElement building3 = new CityViewElement ();
		building3.setBuildingID ("BL03");
		building3.setCityViewAnimation ("building3");
		db.getCityViewElement ().add (building3);
		
		// Huge image that isn't a building
		when (utils.loadImage ("nonBuilding.png")).thenReturn (new BufferedImage (100, 100, BufferedImage.TYPE_INT_ARGB));
		
		final CityViewElement nonBuilding = new CityViewElement ();
		nonBuilding.setCityViewImageFile ("nonBuilding.png");
		db.getCityViewElement ().add (nonBuilding);
		
		// Huge Wizard's Fortress to prove that it gets ignored
		when (utils.loadImage ("fortress.png")).thenReturn (new BufferedImage (100, 100, BufferedImage.TYPE_INT_ARGB));
		
		final CityViewElement fortress = new CityViewElement ();
		fortress.setBuildingID (CommonDatabaseConstants.VALUE_BUILDING_FORTRESS);
		fortress.setCityViewImageFile ("fortress.png");
		db.getCityViewElement ().add (fortress);
		
		// Run method
		db.buildMapsAndRunConsistencyChecks ();
		final Dimension largestBuildingSize = db.getLargestBuildingSize ();
		
		// Check results
		assertEquals (50, largestBuildingSize.width);
		assertEquals (40, largestBuildingSize.height);
	}
	
	/**
	 * Tests the findPick method to find a pick ID that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindPick_Exists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final Pick newPick = new Pick ();
			newPick.setPickID ("MB0" + n);
			db.getPick ().add (newPick);
		}

		db.buildMaps ();

		assertEquals ("MB02", db.findPick ("MB02", "testFindPick_Exists").getPickID ());
	}

	/**
	 * Tests the findPick method to find a pick ID that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindPick_NotExists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final Pick newPick = new Pick ();
			newPick.setPickID ("MB0" + n);
			db.getPick ().add (newPick);
		}

		db.buildMaps ();

		db.findPick ("MB04", "testFindPick_NotExists");
	}

	/**
	 * Tests the findWizard method to find a wizard ID that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindWizard_Exists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final Wizard newWizard = new Wizard ();
			newWizard.setWizardID ("WZ0" + n);
			db.getWizard ().add (newWizard);
		}

		db.buildMaps ();

		assertEquals ("WZ02", db.findWizard ("WZ02", "testFindWizard_Exists").getWizardID ());
	}

	/**
	 * Tests the findWizard method to find a wizard ID that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindWizard_NotExists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final Wizard newWizard = new Wizard ();
			newWizard.setWizardID ("WZ0" + n);
			db.getWizard ().add (newWizard);
		}

		db.buildMaps ();

		db.findWizard ("WZ04", "testFindWizard_NotExists");
	}

	/**
	 * Tests the findProductionType method to find a productionType ID that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindProductionType_Exists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final ProductionTypeEx newProductionType = new ProductionTypeEx ();
			newProductionType.setProductionTypeID ("RE0" + n);
			db.getProductionType ().add (newProductionType);
		}

		db.buildMaps ();

		assertEquals ("RE02", db.findProductionType ("RE02", "testFindProductionType_Exists").getProductionTypeID ());
	}

	/**
	 * Tests the findProductionType method to find a productionType ID that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindProductionType_NotExists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final ProductionTypeEx newProductionType = new ProductionTypeEx ();
			newProductionType.setProductionTypeID ("RE0" + n);
			db.getProductionType ().add (newProductionType);
		}

		db.buildMaps ();

		db.findProductionType ("RE04", "testFindProductionType_NotExists");
	}
	
	/**
	 * Tests the findRace method to find a race ID that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindRace_Exists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final RaceEx newRace = new RaceEx ();
			newRace.setRaceID ("RC0" + n);
			db.getRace ().add (newRace);
		}

		db.buildMaps ();

		assertEquals ("RC02", db.findRace ("RC02", "testFindRace_Exists").getRaceID ());
	}

	/**
	 * Tests the findRace method to find a race ID that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindRace_NotExists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final RaceEx newRace = new RaceEx ();
			newRace.setRaceID ("RC0" + n);
			db.getRace ().add (newRace);
		}

		db.buildMaps ();

		db.findRace ("RC04", "testFindRace_NotExists");
	}

	/**
	 * Tests the findBuilding method to find a building ID that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindBuilding_Exists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CityViewElement newBuilding = new CityViewElement ();
			newBuilding.setBuildingID ("BL0" + n);
			db.getCityViewElement ().add (newBuilding);
		}

		db.buildMaps ();

		assertEquals ("BL02", db.findBuilding ("BL02", "testFindBuilding_Exists").getBuildingID ());
	}

	/**
	 * Tests the findBuilding method to find a building ID that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindBuilding_NotExists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CityViewElement newBuilding = new CityViewElement ();
			newBuilding.setBuildingID ("BL0" + n);
			db.getCityViewElement ().add (newBuilding);
		}

		db.buildMaps ();

		db.findBuilding ("BL04", "testFindBuilding_NotExists");
	}

	/**
	 * Tests the findUnitType method to find a unit type ID that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindUnitType_Exists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitTypeEx newUnitType = new UnitTypeEx ();
			newUnitType.setUnitTypeID ("UT0" + n);
			db.getUnitType ().add (newUnitType);
		}

		db.buildMaps ();

		assertEquals ("UT02", db.findUnitType ("UT02", "testFindUnitType_Exists").getUnitTypeID ());
	}

	/**
	 * Tests the findUnitType method to find a unit type ID that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindUnitType_NotExists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitTypeEx newUnitType = new UnitTypeEx ();
			newUnitType.setUnitTypeID ("UT0" + n);
			db.getUnitType ().add (newUnitType);
		}

		db.buildMaps ();

		db.findUnitType ("UT04", "testFindUnitType_NotExists");
	}
	
	/**
	 * Tests the findUnitAttribute method to find a unit attribute ID that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindUnitAttribute_Exists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitAttributeEx newUnitAttribute = new UnitAttributeEx ();
			newUnitAttribute.setUnitAttributeID ("UA0" + n);
			db.getUnitAttribute ().add (newUnitAttribute);
		}

		db.buildMaps ();

		assertEquals ("UA02", db.findUnitAttribute ("UA02", "testFindUnitAttribute_Exists").getUnitAttributeID ());
	}

	/**
	 * Tests the findUnitAttribute method to find a unit attribute ID that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindUnitAttribute_NotExists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitAttributeEx newUnitAttribute = new UnitAttributeEx ();
			newUnitAttribute.setUnitAttributeID ("UA0" + n);
			db.getUnitAttribute ().add (newUnitAttribute);
		}

		db.buildMaps ();

		db.findUnitAttribute ("UA04", "testFindUnitAttribute_NotExists");
	}

	/**
	 * Tests the findUnitSkill method to find a unit skill ID that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindUnitSkill_Exists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitSkill newUnitSkill = new UnitSkill ();
			newUnitSkill.setUnitSkillID ("US0" + n);
			db.getUnitSkill ().add (newUnitSkill);
		}

		db.buildMaps ();

		assertEquals ("US02", db.findUnitSkill ("US02", "testFindUnitSkill_Exists").getUnitSkillID ());
	}

	/**
	 * Tests the findUnitSkill method to find a unit skill ID that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindUnitSkill_NotExists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitSkill newUnitSkill = new UnitSkill ();
			newUnitSkill.setUnitSkillID ("US0" + n);
			db.getUnitSkill ().add (newUnitSkill);
		}

		db.buildMaps ();

		db.findUnitSkill ("US04", "testFindUnitSkill_NotExists");
	}

	/**
	 * Tests the findUnit method to find a unit ID that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindUnit_Exists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitEx newUnit = new UnitEx ();
			newUnit.setUnitID ("UN00" + n);
			db.getUnit ().add (newUnit);
		}

		db.buildMaps ();

		assertEquals ("UN002", db.findUnit ("UN002", "testFindUnit_Exists").getUnitID ());
	}

	/**
	 * Tests the findUnit method to find a unit ID that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindUnit_NotExists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitEx newUnit = new UnitEx ();
			newUnit.setUnitID ("UN00" + n);
			db.getUnit ().add (newUnit);
		}

		db.buildMaps ();

		db.findUnit ("UN004", "testFindUnit_NotExists");
	}

	/**
	 * Tests the findRangedAttackType method to find a rangedAttackType ID that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindRangedAttackType_Exists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
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
	 * Tests the findRangedAttackType method to find a rangedAttackType ID that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindRangedAttackType_NotExists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final RangedAttackTypeEx newRangedAttackType = new RangedAttackTypeEx ();
			newRangedAttackType.setRangedAttackTypeID ("RAT0" + n);
			db.getRangedAttackType ().add (newRangedAttackType);
		}

		db.buildMaps ();

		db.findRangedAttackType ("RAT04", "testFindRangedAttackType_NotExists");
	}

	/**
	 * Tests the findWeaponGrade method to find a weaponGrade ID that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindWeaponGrade_Exists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
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
	 * Tests the findWeaponGrade method to find a weaponGrade ID that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindWeaponGrade_NotExists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final WeaponGrade newWeaponGrade = new WeaponGrade ();
			newWeaponGrade.setWeaponGradeNumber (n);
			db.getWeaponGrade ().add (newWeaponGrade);
		}

		db.buildMaps ();

		db.findWeaponGrade (4, "testFindWeaponGrade_NotExists");
	}

	/**
	 * Tests the findCombatTileUnitRelativeScale method to find a scale that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindCombatTileUnitRelativeScale_Exists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CombatTileUnitRelativeScaleEx newCombatTileUnitRelativeScale = new CombatTileUnitRelativeScaleEx ();
			newCombatTileUnitRelativeScale.setScale (n);
			db.getCombatTileUnitRelativeScale ().add (newCombatTileUnitRelativeScale);
		}

		db.buildMaps ();

		assertEquals (2, db.findCombatTileUnitRelativeScale (2, "testFindCombatTileUnitRelativeScale_Exists").getScale ());
	}

	/**
	 * Tests the findCombatTileUnitRelativeScale method to find a scale that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindCombatTileUnitRelativeScale_NotExists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CombatTileUnitRelativeScaleEx newCombatTileUnitRelativeScale = new CombatTileUnitRelativeScaleEx ();
			newCombatTileUnitRelativeScale.setScale (n);
			db.getCombatTileUnitRelativeScale ().add (newCombatTileUnitRelativeScale);
		}

		db.buildMaps ();

		db.findCombatTileUnitRelativeScale (4, "testFindCombatTileUnitRelativeScale_NotExists");
	}

	/**
	 * Tests the findTileSet method to find a tile set ID that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindTileSet_Exists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
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
	@Test(expected=RecordNotFoundException.class)
	public final void testFindTileSet_NotExists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final TileSetEx newTileSet = new TileSetEx ();
			newTileSet.setTileSetID ("WZ0" + n);
			db.getTileSet ().add (newTileSet);
		}

		db.buildMaps ();

		db.findTileSet ("WZ04", "testFindTileSet_NotExists");
	}

	/**
	 * Tests the findMapFeatureID method to find a mapFeature ID that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindMapFeatureID_Exists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final MapFeatureEx newMapFeature = new MapFeatureEx ();
			newMapFeature.setMapFeatureID ("MF0" + n);
			db.getMapFeature ().add (newMapFeature);
		}

		db.buildMaps ();

		assertEquals ("MF02", db.findMapFeature ("MF02", "testFindMapFeatureID_Exists").getMapFeatureID ());
	}

	/**
	 * Tests the findMapFeatureID method to find a mapFeature ID that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindMapFeatureID_NotExists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final MapFeatureEx newMapFeature = new MapFeatureEx ();
			newMapFeature.setMapFeatureID ("MF0" + n);
			db.getMapFeature ().add (newMapFeature);
		}

		db.buildMaps ();

		db.findMapFeature ("MF04", "testFindMapFeatureID_NotExists");
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
		
		when (memoryBuildingUtils.findBuilding (buildings, cityLocation, "BL01")).thenReturn (true);
		when (memoryBuildingUtils.findBuilding (buildings, cityLocation, "BL02")).thenReturn (false);
		when (memoryBuildingUtils.findBuilding (buildings, cityLocation, "BL03")).thenReturn (true);
		
		// Set up object to test
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		db.setMemoryBuildingUtils (memoryBuildingUtils);
		
		// An image for CS01 needing no buildings
		final CityImage image1 = new CityImage ();
		image1.setCitySizeID ("CS01");
		db.getCityImage ().add (image1);
		
		// An image for CS01 needing a building that we have (this should get chosen)
		final CityImagePrerequisite image2prereq = new CityImagePrerequisite ();
		image2prereq.setPrerequisiteID ("BL01");
		
		final CityImage image2 = new CityImage ();
		image2.setCitySizeID ("CS01");
		image2.getCityImagePrerequisite ().add (image2prereq);
		db.getCityImage ().add (image2);

		// An image for CS01 needing a building that we have plus one that we don't
		final CityImagePrerequisite image3prereq1 = new CityImagePrerequisite ();
		image3prereq1.setPrerequisiteID ("BL01");
		final CityImagePrerequisite image3prereq2 = new CityImagePrerequisite ();
		image3prereq2.setPrerequisiteID ("BL02");
		
		final CityImage image3 = new CityImage ();
		image3.setCitySizeID ("CS01");
		image3.getCityImagePrerequisite ().add (image3prereq1);
		image3.getCityImagePrerequisite ().add (image3prereq2);
		db.getCityImage ().add (image3);
		
		// An image for CS02 needing two building that we have
		final CityImagePrerequisite image4prereq1 = new CityImagePrerequisite ();
		image4prereq1.setPrerequisiteID ("BL01");
		final CityImagePrerequisite image4prereq2 = new CityImagePrerequisite ();
		image4prereq2.setPrerequisiteID ("BL03");
		
		final CityImage image4 = new CityImage ();
		image4.setCitySizeID ("CS02");
		image4.getCityImagePrerequisite ().add (image4prereq1);
		image4.getCityImagePrerequisite ().add (image4prereq2);
		db.getCityImage ().add (image4);
		
		// Run test
		assertSame (image2, db.findBestCityImage ("CS01", cityLocation, buildings, "testFindBestCityImage"));
	}
	
	/**
	 * Tests the findBestCityImage method looking for a citySizeID that doesn't exist
	 * @throws RecordNotFoundException If no city size entries match the requested citySizeID
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindBestCityImage_NotExists () throws RecordNotFoundException
	{
		// City location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (25, 10, 1);

		// Buildings that we have
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		
		// Set up object to test
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		db.setMemoryBuildingUtils (memoryBuildingUtils);
		
		// An image for CS01 needing no buildings
		final CityImage image1 = new CityImage ();
		image1.setCitySizeID ("CS01");
		db.getCityImage ().add (image1);
		
		// Run test
		db.findBestCityImage ("CS02", cityLocation, buildings, "testFindBestCityImage");
	}
	
	/**
	 * Tests the findAnimation method to find a animation ID that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindAnimation_Exists () throws RecordNotFoundException
	{
		// Set up object to test
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
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
	@Test(expected=RecordNotFoundException.class)
	public final void testFindAnimation_NotExists () throws RecordNotFoundException
	{
		// Set up object to test
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final AnimationEx newAnimation = new AnimationEx ();
			newAnimation.setAnimationID ("AN0" + n);
			db.getAnimation ().add (newAnimation);
		}

		db.buildMaps ();

		// Check results
		db.findAnimation ("AN04", "testFindAnimation_NotExists");
	}
}