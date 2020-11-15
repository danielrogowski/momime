package momime.client.graphics.database;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import momime.common.database.AnimationGfx;
import momime.common.database.PlayList;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSpecialOrder;

/**
 * Tests the GraphicsDatabaseExImpl class
 */
public final class TestGraphicsDatabaseExImpl
{
	/**
	 * Tests the findUnitSkillComponent method to find a unit attribute ID that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindUnitSkillComponent_Exists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 65; n <= 67; n++)
		{
			final UnitSkillComponentImageGfx newUnitSkillComponent = new UnitSkillComponentImageGfx ();
			newUnitSkillComponent.setUnitSkillComponentID (UnitSkillComponent.fromValue (new String (new char [] {(char) n})));
			db.getUnitSkillComponentImage ().add (newUnitSkillComponent);
		}

		db.buildMaps ();

		assertEquals (UnitSkillComponent.BASIC,
			db.findUnitSkillComponent (UnitSkillComponent.BASIC, "testFindUnitSkillComponent_Exists").getUnitSkillComponentID ());
	}

	/**
	 * Tests the findUnitSkillComponent method to find a unit attribute ID that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindUnitSkillComponent_NotExists () throws RecordNotFoundException
	{
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 65; n <= 67; n++)
		{
			final UnitSkillComponentImageGfx newUnitSkillComponent = new UnitSkillComponentImageGfx ();
			newUnitSkillComponent.setUnitSkillComponentID (UnitSkillComponent.fromValue (new String (new char [] {(char) n})));
			db.getUnitSkillComponentImage ().add (newUnitSkillComponent);
		}

		db.buildMaps ();

		db.findUnitSkillComponent (UnitSkillComponent.HERO_SKILLS, "testFindUnitSkillComponent_NotExists");
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
	@Test(expected=RecordNotFoundException.class)
	public final void testFindPlayList_NotExists () throws RecordNotFoundException
	{
		// Set up object to test
		final GraphicsDatabaseExImpl db = new GraphicsDatabaseExImpl ();
		for (int n = 1; n <= 3; n++)
		{
			final PlayList newPlayList = new PlayList ();
			newPlayList.setPlayListID ("PL0" + n);
			db.getPlayList ().add (newPlayList);
		}

		db.buildMaps ();

		// Check results
		db.findPlayList ("PL04", "testFindPlayList_NotExists");
	}
}