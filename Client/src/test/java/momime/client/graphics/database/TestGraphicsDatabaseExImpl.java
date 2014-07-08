package momime.client.graphics.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import momime.client.graphics.database.v0_9_5.CityImage;
import momime.client.graphics.database.v0_9_5.CityImagePrerequisite;
import momime.client.graphics.database.v0_9_5.Pick;
import momime.client.graphics.database.v0_9_5.Unit;
import momime.client.graphics.database.v0_9_5.Wizard;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_5.MemoryBuilding;
import momime.common.utils.MemoryBuildingUtils;

import org.junit.Test;

import com.ndg.map.coordinates.MapCoordinates3DEx;

/**
 * Tests the GraphicsDatabaseExImpl class
 */
public final class TestGraphicsDatabaseExImpl
{
	/**
	 * Tests the findPick method to find a pick ID that does exist
	 * @throws IOException If there is a problem
	 */
	@Test
	public final void testFindPick_Exists () throws IOException
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
	 * @throws IOException If there is a problem
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindPick_NotExists () throws IOException
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
	 * @throws IOException If there is a problem
	 */
	@Test
	public final void testFindWizard_Exists () throws IOException
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
	 * @throws IOException If there is a problem
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindWizard_NotExists () throws IOException
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
	 * Tests the findProductionType method
	 */
	@Test
	public final void findProductionType ()
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final ProductionTypeEx newProductionType = new ProductionTypeEx ();
			newProductionType.setProductionTypeID ("RE0" + n);
			db.getProductionType ().add (newProductionType);
		}

		db.buildMaps ();

		assertEquals ("RE02", db.findProductionType ("RE02").getProductionTypeID ());
		assertNull ("RE04", db.findProductionType ("RE04"));
	}
	
	/**
	 * Tests the findRace method to find a race ID that does exist
	 * @throws IOException If there is a problem
	 */
	@Test
	public final void testFindRace_Exists () throws IOException
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
	 * @throws IOException If there is a problem
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindRace_NotExists () throws IOException
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
	 * Tests the findUnit method to find a unit ID that does exist
	 * @throws IOException If there is a problem
	 */
	@Test
	public final void testFindUnit_Exists () throws IOException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final Unit newUnit = new Unit ();
			newUnit.setUnitID ("UN00" + n);
			db.getUnit ().add (newUnit);
		}

		db.buildMaps ();

		assertEquals ("UN002", db.findUnit ("UN002", "testFindUnit_Exists").getUnitID ());
	}

	/**
	 * Tests the findUnit method to find a unit ID that doesn't exist
	 * @throws IOException If there is a problem
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindUnit_NotExists () throws IOException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final Unit newUnit = new Unit ();
			newUnit.setUnitID ("UN00" + n);
			db.getUnit ().add (newUnit);
		}

		db.buildMaps ();

		db.findUnit ("UN004", "testFindUnit_NotExists");
	}

	/**
	 * Tests the findTileSet method to find a tile set ID that does exist
	 * @throws IOException If there is a problem
	 */
	@Test
	public final void testFindTileSet_Exists () throws IOException
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
	 * @throws IOException If there is a problem
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindTileSet_NotExists () throws IOException
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
	 * @throws IOException If there is a problem
	 */
	@Test
	public final void testFindMapFeatureID_Exists () throws IOException
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
	 * @throws IOException If there is a problem
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindMapFeatureID_NotExists () throws IOException
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
	 * @throws IOException If there is a problem
	 */
	@Test
	public final void testFindAnimation_Exists () throws IOException
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
	 * @throws IOException If there is a problem
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindAnimation_NotExists () throws IOException
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
