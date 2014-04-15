package momime.client.graphics.database;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.IOException;

import momime.client.graphics.database.v0_9_5.AnimationFrame;
import momime.client.graphics.database.v0_9_5.MapFeature;
import momime.client.graphics.database.v0_9_5.Pick;
import momime.client.graphics.database.v0_9_5.Wizard;
import momime.client.ui.MomUIUtils;
import momime.common.database.RecordNotFoundException;

import org.junit.Test;

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
			final MapFeature newMapFeature = new MapFeature ();
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
			final MapFeature newMapFeature = new MapFeature ();
			newMapFeature.setMapFeatureID ("MF0" + n);
			db.getMapFeature ().add (newMapFeature);
		}

		db.buildMaps ();

		db.findMapFeature ("MF04", "testFindMapFeatureID_NotExists");
	}
	
	/**
	 * Tests the findAnimation method to find a animation ID that does exist
	 * @throws IOException If there is a problem
	 */
	@Test
	public final void testFindAnimation_Exists () throws IOException
	{
		// Mock some images
		final MomUIUtils utils = mock (MomUIUtils.class);
		
		// Set up object to test
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final AnimationEx newAnimation = new AnimationEx ();
			newAnimation.setAnimationID ("AN0" + n);
			db.getAnimation ().add (newAnimation);
			
			// Have to go to some lengths to make the animation pass consistency checks performed by buildMaps below
			final BufferedImage image = new BufferedImage (10, 5, BufferedImage.TYPE_INT_ARGB);
			when (utils.loadImage ("ImageFile" + n)).thenReturn (image);

			final AnimationFrame frame = new AnimationFrame ();
			frame.setFrameImageFile ("ImageFile" + n);
			
			newAnimation.setUtils (utils);
			newAnimation.getFrame ().add (frame);
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
		// Mock some images
		final MomUIUtils utils = mock (MomUIUtils.class);
		
		// Set up object to test
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final AnimationEx newAnimation = new AnimationEx ();
			newAnimation.setAnimationID ("AN0" + n);
			db.getAnimation ().add (newAnimation);
			
			// Have to go to some lengths to make the animation pass consistency checks performed by buildMaps below
			final BufferedImage image = new BufferedImage (10, 5, BufferedImage.TYPE_INT_ARGB);
			when (utils.loadImage ("ImageFile" + n)).thenReturn (image);

			final AnimationFrame frame = new AnimationFrame ();
			frame.setFrameImageFile ("ImageFile" + n);
			
			newAnimation.setUtils (utils);
			newAnimation.getFrame ().add (frame);
		}

		db.buildMaps ();

		// Check results
		db.findAnimation ("AN04", "testFindAnimation_NotExists");
	}
}
