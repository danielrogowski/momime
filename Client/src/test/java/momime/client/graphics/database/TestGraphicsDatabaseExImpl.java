package momime.client.graphics.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitAttributeComponent;
import momime.common.database.UnitSpecialOrder;
import momime.common.messages.MemoryBuilding;
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
		
		final TileSetGfx overlandMapTileSet = new TileSetGfx ();
		overlandMapTileSet.setTileSetID (GraphicsDatabaseConstants.TILE_SET_OVERLAND_MAP);
		db.getTileSet ().add (overlandMapTileSet);
		
		// 30 x 20 image
		when (utils.loadImage ("building1.png")).thenReturn (new BufferedImage (30, 20, BufferedImage.TYPE_INT_ARGB));
		
		final CityViewElementGfx building1 = new CityViewElementGfx ();
		building1.setBuildingID ("BL01");
		building1.setCityViewImageFile ("building1.png");
		db.getCityViewElement ().add (building1);
		
		// 50 x 10 image, note this is the alternate image
		when (utils.loadImage ("building2.png")).thenReturn (new BufferedImage (60, 70, BufferedImage.TYPE_INT_ARGB));
		when (utils.loadImage ("building2alt.png")).thenReturn (new BufferedImage (50, 10, BufferedImage.TYPE_INT_ARGB));

		final CityViewElementGfx building2 = new CityViewElementGfx ();
		building2.setBuildingID ("BL02");
		building2.setCityViewImageFile ("building2.png");
		building2.setCityViewAlternativeImageFile ("building2alt.png");
		db.getCityViewElement ().add (building2);
		
		// 10 x 40 animation
		when (utils.loadImage ("frame1.png")).thenReturn (new BufferedImage (10, 40, BufferedImage.TYPE_INT_ARGB));
		when (utils.loadImage ("frame2.png")).thenReturn (new BufferedImage (10, 40, BufferedImage.TYPE_INT_ARGB));
		
		final AnimationGfx anim = new AnimationGfx ();
		anim.setAnimationID ("building3");
		anim.getFrame ().add ("frame1.png");
		anim.getFrame ().add ("frame2.png");
		anim.setUtils (utils);
		db.getAnimation ().add (anim);

		final CityViewElementGfx building3 = new CityViewElementGfx ();
		building3.setBuildingID ("BL03");
		building3.setCityViewAnimation ("building3");
		db.getCityViewElement ().add (building3);
		
		// Huge image that isn't a building
		when (utils.loadImage ("nonBuilding.png")).thenReturn (new BufferedImage (100, 100, BufferedImage.TYPE_INT_ARGB));
		
		final CityViewElementGfx nonBuilding = new CityViewElementGfx ();
		nonBuilding.setCityViewImageFile ("nonBuilding.png");
		db.getCityViewElement ().add (nonBuilding);
		
		// Huge Wizard's Fortress to prove that it gets ignored
		when (utils.loadImage ("fortress.png")).thenReturn (new BufferedImage (100, 100, BufferedImage.TYPE_INT_ARGB));
		
		final CityViewElementGfx fortress = new CityViewElementGfx ();
		fortress.setBuildingID (CommonDatabaseConstants.BUILDING_FORTRESS);
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
			final PickGfx newPick = new PickGfx ();
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
			final PickGfx newPick = new PickGfx ();
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
			final WizardGfx newWizard = new WizardGfx ();
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
			final WizardGfx newWizard = new WizardGfx ();
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
			final ProductionTypeGfx newProductionType = new ProductionTypeGfx ();
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
			final ProductionTypeGfx newProductionType = new ProductionTypeGfx ();
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
			final RaceGfx newRace = new RaceGfx ();
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
			final RaceGfx newRace = new RaceGfx ();
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
			final CityViewElementGfx newBuilding = new CityViewElementGfx ();
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
			final CityViewElementGfx newBuilding = new CityViewElementGfx ();
			newBuilding.setBuildingID ("BL0" + n);
			db.getCityViewElement ().add (newBuilding);
		}

		db.buildMaps ();

		db.findBuilding ("BL04", "testFindBuilding_NotExists");
	}

	/**
	 * Tests the findCitySpellEffect method to find a citySpellEffect ID that does exist
	 */
	@Test
	public final void testFindCitySpellEffect ()
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CityViewElementGfx newCitySpellEffect = new CityViewElementGfx ();
			newCitySpellEffect.setCitySpellEffectID ("CSE0" + n);
			db.getCityViewElement ().add (newCitySpellEffect);
		}

		db.buildMaps ();

		assertEquals ("CSE02", db.findCitySpellEffect ("CSE02", "testFindCitySpellEffect").getCitySpellEffectID ());
		assertNull (db.findCitySpellEffect ("CSE04", "testFindCitySpellEffect"));
	}

	/**
	 * Tests the findSpell method to find a spell ID that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindSpell_Exists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final SpellGfx newSpell = new SpellGfx ();
			newSpell.setSpellID ("SP0" + n);
			db.getSpell ().add (newSpell);
		}

		db.buildMaps ();

		assertEquals ("SP02", db.findSpell ("SP02", "testFindSpell_Exists").getSpellID ());
	}

	/**
	 * Tests the findSpell method to find a spell ID that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindSpell_NotExists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final SpellGfx newSpell = new SpellGfx ();
			newSpell.setSpellID ("SP0" + n);
			db.getSpell ().add (newSpell);
		}

		db.buildMaps ();

		db.findSpell ("SP04", "testFindSpell_NotExists");
	}
	
	/**
	 * Tests the findCombatAction method to find a combatAction ID that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindCombatAction_Exists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CombatActionGfx newCombatAction = new CombatActionGfx ();
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
	@Test(expected=RecordNotFoundException.class)
	public final void testFindCombatAction_NotExists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CombatActionGfx newCombatAction = new CombatActionGfx ();
			newCombatAction.setCombatActionID ("CMB0" + n);
			db.getCombatAction ().add (newCombatAction);
		}

		db.buildMaps ();

		db.findCombatAction ("CMB04", "testFindCombatAction_NotExists");
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
			final UnitTypeGfx newUnitType = new UnitTypeGfx ();
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
			final UnitTypeGfx newUnitType = new UnitTypeGfx ();
			newUnitType.setUnitTypeID ("UT0" + n);
			db.getUnitType ().add (newUnitType);
		}

		db.buildMaps ();

		db.findUnitType ("UT04", "testFindUnitType_NotExists");
	}
	
	/**
	 * Tests the findUnitAttributeComponent method to find a unit attribute ID that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindUnitAttributeComponent_Exists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 65; n <= 67; n++)
		{
			final UnitAttributeComponentImageGfx newUnitAttributeComponent = new UnitAttributeComponentImageGfx ();
			newUnitAttributeComponent.setUnitAttributeComponentID (UnitAttributeComponent.fromValue (new String (new char [] {(char) n})));
			db.getUnitAttributeComponentImage ().add (newUnitAttributeComponent);
		}

		db.buildMaps ();

		assertEquals (UnitAttributeComponent.BASIC,
			db.findUnitAttributeComponent (UnitAttributeComponent.BASIC, "testFindUnitAttributeComponent_Exists").getUnitAttributeComponentID ());
	}

	/**
	 * Tests the findUnitAttributeComponent method to find a unit attribute ID that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindUnitAttributeComponent_NotExists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 65; n <= 67; n++)
		{
			final UnitAttributeComponentImageGfx newUnitAttributeComponent = new UnitAttributeComponentImageGfx ();
			newUnitAttributeComponent.setUnitAttributeComponentID (UnitAttributeComponent.fromValue (new String (new char [] {(char) n})));
			db.getUnitAttributeComponentImage ().add (newUnitAttributeComponent);
		}

		db.buildMaps ();

		db.findUnitAttributeComponent (UnitAttributeComponent.HERO_SKILLS, "testFindUnitAttributeComponent_NotExists");
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
			final UnitAttributeGfx newUnitAttribute = new UnitAttributeGfx ();
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
			final UnitAttributeGfx newUnitAttribute = new UnitAttributeGfx ();
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
			final UnitSkillGfx newUnitSkill = new UnitSkillGfx ();
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
			final UnitSkillGfx newUnitSkill = new UnitSkillGfx ();
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
			final UnitGfx newUnit = new UnitGfx ();
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
			final UnitGfx newUnit = new UnitGfx ();
			newUnit.setUnitID ("UN00" + n);
			db.getUnit ().add (newUnit);
		}

		db.buildMaps ();

		db.findUnit ("UN004", "testFindUnit_NotExists");
	}

	/**
	 * Tests the findUnitSpecialOrder method to find a unit attribute ID that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindUnitSpecialOrder_Exists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 67; n <= 68; n++)
		{
			final UnitSpecialOrderImageGfx newUnitSpecialOrder = new UnitSpecialOrderImageGfx ();
			newUnitSpecialOrder.setUnitSpecialOrderID (UnitSpecialOrder.fromValue (new String (new char [] {(char) n})));
			db.getUnitSpecialOrderImage ().add (newUnitSpecialOrder);
		}

		db.buildMaps ();

		assertEquals (UnitSpecialOrder.BUILD_CITY,
			db.findUnitSpecialOrder (UnitSpecialOrder.BUILD_CITY, "testFindUnitSpecialOrder_Exists").getUnitSpecialOrderID ());
	}

	/**
	 * Tests the findUnitSpecialOrder method to find a unit attribute ID that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindUnitSpecialOrder_NotExists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 67; n <= 68; n++)
		{
			final UnitSpecialOrderImageGfx newUnitSpecialOrder = new UnitSpecialOrderImageGfx ();
			newUnitSpecialOrder.setUnitSpecialOrderID (UnitSpecialOrder.fromValue (new String (new char [] {(char) n})));
			db.getUnitSpecialOrderImage ().add (newUnitSpecialOrder);
		}

		db.buildMaps ();

		db.findUnitSpecialOrder (UnitSpecialOrder.BUILD_ROAD, "testFindUnitSpecialOrder_NotExists");
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
			final RangedAttackTypeGfx newRangedAttackType = new RangedAttackTypeGfx ();
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
			final RangedAttackTypeGfx newRangedAttackType = new RangedAttackTypeGfx ();
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
			final WeaponGradeGfx newWeaponGrade = new WeaponGradeGfx ();
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
			final WeaponGradeGfx newWeaponGrade = new WeaponGradeGfx ();
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
			final CombatTileUnitRelativeScaleGfx newCombatTileUnitRelativeScale = new CombatTileUnitRelativeScaleGfx ();
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
			final CombatTileUnitRelativeScaleGfx newCombatTileUnitRelativeScale = new CombatTileUnitRelativeScaleGfx ();
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
			final TileSetGfx newTileSet = new TileSetGfx ();
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
			final TileSetGfx newTileSet = new TileSetGfx ();
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
			final MapFeatureGfx newMapFeature = new MapFeatureGfx ();
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
			final MapFeatureGfx newMapFeature = new MapFeatureGfx ();
			newMapFeature.setMapFeatureID ("MF0" + n);
			db.getMapFeature ().add (newMapFeature);
		}

		db.buildMaps ();

		db.findMapFeature ("MF04", "testFindMapFeatureID_NotExists");
	}

	/**
	 * Tests the findCombatAreaEffectID method to find a combatAreaEffect ID that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindCombatAreaEffectID_Exists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CombatAreaEffectGfx newCombatAreaEffect = new CombatAreaEffectGfx ();
			newCombatAreaEffect.setCombatAreaEffectID ("CAE0" + n);
			db.getCombatAreaEffect ().add (newCombatAreaEffect);
		}

		db.buildMaps ();

		assertEquals ("CAE02", db.findCombatAreaEffect ("CAE02", "testFindCombatAreaEffectID_Exists").getCombatAreaEffectID ());
	}

	/**
	 * Tests the findCombatAreaEffectID method to find a combatAreaEffect ID that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindCombatAreaEffectID_NotExists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final CombatAreaEffectGfx newCombatAreaEffect = new CombatAreaEffectGfx ();
			newCombatAreaEffect.setCombatAreaEffectID ("CAE0" + n);
			db.getCombatAreaEffect ().add (newCombatAreaEffect);
		}

		db.buildMaps ();

		db.findCombatAreaEffect ("CAE04", "testFindCombatAreaEffectID_NotExists");
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
		when (memoryBuildingUtils.findBuilding (buildings, cityLocation, "BL03")).thenReturn (new MemoryBuilding ());
		
		// Set up object to test
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		db.setMemoryBuildingUtils (memoryBuildingUtils);
		
		// An image for CS01 needing no buildings
		final CityImageGfx image1 = new CityImageGfx ();
		image1.setCitySizeID ("CS01");
		db.getCityImage ().add (image1);
		
		// An image for CS01 needing a building that we have (this should get chosen)
		final CityImagePrerequisiteGfx image2prereq = new CityImagePrerequisiteGfx ();
		image2prereq.setPrerequisiteID ("BL01");
		
		final CityImageGfx image2 = new CityImageGfx ();
		image2.setCitySizeID ("CS01");
		image2.getCityImagePrerequisite ().add (image2prereq);
		db.getCityImage ().add (image2);

		// An image for CS01 needing a building that we have plus one that we don't
		final CityImagePrerequisiteGfx image3prereq1 = new CityImagePrerequisiteGfx ();
		image3prereq1.setPrerequisiteID ("BL01");
		final CityImagePrerequisiteGfx image3prereq2 = new CityImagePrerequisiteGfx ();
		image3prereq2.setPrerequisiteID ("BL02");
		
		final CityImageGfx image3 = new CityImageGfx ();
		image3.setCitySizeID ("CS01");
		image3.getCityImagePrerequisite ().add (image3prereq1);
		image3.getCityImagePrerequisite ().add (image3prereq2);
		db.getCityImage ().add (image3);
		
		// An image for CS02 needing two building that we have
		final CityImagePrerequisiteGfx image4prereq1 = new CityImagePrerequisiteGfx ();
		image4prereq1.setPrerequisiteID ("BL01");
		final CityImagePrerequisiteGfx image4prereq2 = new CityImagePrerequisiteGfx ();
		image4prereq2.setPrerequisiteID ("BL03");
		
		final CityImageGfx image4 = new CityImageGfx ();
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
		final CityImageGfx image1 = new CityImageGfx ();
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
			final AnimationGfx newAnimation = new AnimationGfx ();
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
			final AnimationGfx newAnimation = new AnimationGfx ();
			newAnimation.setAnimationID ("AN0" + n);
			db.getAnimation ().add (newAnimation);
		}

		db.buildMaps ();

		// Check results
		db.findAnimation ("AN04", "testFindAnimation_NotExists");
	}

	/**
	 * Tests the findPlayList method to find a play list ID that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindPlayList_Exists () throws RecordNotFoundException
	{
		// Set up object to test
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final PlayListGfx newPlayList = new PlayListGfx ();
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
	@Test(expected=RecordNotFoundException.class)
	public final void testFindPlayList_NotExists () throws RecordNotFoundException
	{
		// Set up object to test
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final PlayListGfx newPlayList = new PlayListGfx ();
			newPlayList.setPlayListID ("PL0" + n);
			db.getPlayList ().add (newPlayList);
		}

		db.buildMaps ();

		// Check results
		db.findPlayList ("PL04", "testFindPlayList_NotExists");
	}
}