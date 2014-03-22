package momime.client.graphics.database;

import static org.junit.Assert.assertEquals;
import momime.client.graphics.database.v0_9_5.Animation;
import momime.client.graphics.database.v0_9_5.Pick;
import momime.client.graphics.database.v0_9_5.Wizard;
import momime.common.MomException;
import momime.common.database.RecordNotFoundException;

import org.junit.Test;

/**
 * Tests the GraphicsDatabaseExImpl class
 */
public final class TestGraphicsDatabaseExImpl
{
	/**
	 * Tests the findPick method to find a pick ID that does exist
	 * @throws MomException If there is an error in buildMaps
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindPick_Exists () throws MomException, RecordNotFoundException
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
	 * @throws MomException If there is an error in buildMaps
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindPick_NotExists () throws MomException, RecordNotFoundException
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
	 * @throws MomException If there is an error in buildMaps
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindWizard_Exists () throws MomException, RecordNotFoundException
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
	 * @throws MomException If there is an error in buildMaps
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindWizard_NotExists () throws MomException, RecordNotFoundException
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
	 * Tests the findAnimation method to find a animation ID that does exist
	 * @throws MomException If there is an error in buildMaps
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindAnimation_Exists () throws MomException, RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final Animation newAnimation = new Animation ();
			newAnimation.setAnimationID ("AN0" + n);
			db.getAnimation ().add (newAnimation);
		}

		db.buildMaps ();

		assertEquals ("AN02", db.findAnimation ("AN02", "testFindAnimation_Exists").getAnimationID ());
	}

	/**
	 * Tests the findAnimation method to find a animation ID that doesn't exist
	 * @throws MomException If there is an error in buildMaps
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindAnimation_NotExists () throws MomException, RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final Animation newAnimation = new Animation ();
			newAnimation.setAnimationID ("AN0" + n);
			db.getAnimation ().add (newAnimation);
		}

		db.buildMaps ();

		db.findAnimation ("AN04", "testFindAnimation_NotExists");
	}
}
