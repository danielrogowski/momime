package momime.server.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import momime.common.database.RecordNotFoundException;
import momime.server.database.v0_9_4.Building;
import momime.server.database.v0_9_4.CityNameContainer;
import momime.server.database.v0_9_4.CitySize;
import momime.server.database.v0_9_4.MapFeature;
import momime.server.database.v0_9_4.Pick;
import momime.server.database.v0_9_4.PickType;
import momime.server.database.v0_9_4.PickTypeCountContainer;
import momime.server.database.v0_9_4.Plane;
import momime.server.database.v0_9_4.ProductionType;
import momime.server.database.v0_9_4.Race;
import momime.server.database.v0_9_4.ServerDatabase;
import momime.server.database.v0_9_4.Spell;
import momime.server.database.v0_9_4.Unit;
import momime.server.database.v0_9_4.UnitSkill;
import momime.server.database.v0_9_4.Wizard;
import momime.server.database.v0_9_4.WizardPickCount;

import org.junit.Test;

/**
 * Tests the ServerDatabaseLookup class
 * We've already tested the lookups in TestCommonDatabaseLookup - what we're really checking here is that the return types allow direct access to the server-only properties without typecasting
 */
public final class TestServerDatabaseLookup
{
	/**
	 * Tests the getCitySizes method
	 */
	@Test
	public final void testGetCitySizes ()
	{
		final ServerDatabase db = new ServerDatabase ();
		for (int n = 1; n <= 3; n++)
		{
			final CitySize newCitySize = new CitySize ();
			newCitySize.setCitySizeID ("CS0" + n);
			db.getCitySize ().add (newCitySize);
		}

		final ServerDatabaseLookup lookup = new ServerDatabaseLookup (db);

		assertEquals (3, lookup.getCitySizes ().size ());
		for (int n = 1; n <= 3; n++)
			assertEquals ("CS0" + n, lookup.getCitySizes ().get (n - 1).getCitySizeID ());
	}

	/**
	 * Tests the findCitySize method to find a citySizeNumber that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindCitySize_Exists () throws RecordNotFoundException
	{
		final ServerDatabase db = new ServerDatabase ();
		for (int n = 1; n <= 3; n++)
		{
			final CitySize newCitySize = new CitySize ();
			newCitySize.setCitySizeID ("CS0" + n);
			db.getCitySize ().add (newCitySize);
		}

		final ServerDatabaseLookup lookup = new ServerDatabaseLookup (db);

		assertEquals ("CS02", lookup.findCitySize ("CS02", "testFindCitySize_Exists").getCitySizeID ());
	}

	/**
	 * Tests the findCitySize method to find a citySizeNumber that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindCitySize_NotExists () throws RecordNotFoundException
	{
		final ServerDatabase db = new ServerDatabase ();
		for (int n = 1; n <= 3; n++)
		{
			final CitySize newCitySize = new CitySize ();
			newCitySize.setCitySizeID ("CS0" + n);
			db.getCitySize ().add (newCitySize);
		}

		final ServerDatabaseLookup lookup = new ServerDatabaseLookup (db);

		assertNull (lookup.findCitySize ("CS04", "testFindCitySize_NotExists"));
	}

	/**
	 * Tests the findPlaneID method to find a plane ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindPlaneID_Exists () throws RecordNotFoundException
	{
		final ServerDatabase db = new ServerDatabase ();
		for (int n = 0; n < 2; n++)
		{
			final Plane newPlane = new Plane ();
			newPlane.setPlaneNumber (n);
			newPlane.setPlaneDescription ("Plane " + n);

			db.getPlane ().add (newPlane);
		}

		final ServerDatabaseLookup lookup = new ServerDatabaseLookup (db);

		assertEquals ("Plane 1", lookup.findPlane (1, "testFindPlaneID_Exists").getPlaneDescription ());
	}

	/**
	 * Tests the findPlaneID method to find a plane ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindPlaneID_NotExists () throws RecordNotFoundException
	{
		final ServerDatabase db = new ServerDatabase ();
		for (int n = 0; n < 2; n++)
		{
			final Plane newPlane = new Plane ();
			newPlane.setPlaneNumber (n);
			newPlane.setPlaneDescription ("Plane " + n);

			db.getPlane ().add (newPlane);
		}

		final ServerDatabaseLookup lookup = new ServerDatabaseLookup (db);

		lookup.findPlane (2, "testFindPlaneID_NotExists");
	}

	/**
	 * Tests the findMapFeatureID method to find a mapFeature ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindMapFeatureID_Exists () throws RecordNotFoundException
	{
		final ServerDatabase db = new ServerDatabase ();
		for (int n = 1; n <= 3; n++)
		{
			final MapFeature newMapFeature = new MapFeature ();
			newMapFeature.setMapFeatureID ("MF0" + n);
			newMapFeature.setCityQualityEstimate (n * 10);
			db.getMapFeature ().add (newMapFeature);
		}

		final ServerDatabaseLookup lookup = new ServerDatabaseLookup (db);

		assertEquals ("MF02", lookup.findMapFeature ("MF02", "testFindMapFeatureID_Exists").getMapFeatureID ());
		assertEquals (20, lookup.findMapFeature ("MF02", "testFindMapFeatureID_Exists").getCityQualityEstimate ().intValue ());
	}

	/**
	 * Tests the findMapFeatureID method to find a mapFeature ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindMapFeatureID_NotExists () throws RecordNotFoundException
	{
		final ServerDatabase db = new ServerDatabase ();
		for (int n = 1; n <= 3; n++)
		{
			final MapFeature newMapFeature = new MapFeature ();
			newMapFeature.setMapFeatureID ("MF0" + n);
			db.getMapFeature ().add (newMapFeature);
		}

		final ServerDatabaseLookup lookup = new ServerDatabaseLookup (db);

		lookup.findMapFeature ("MF04", "testFindMapFeatureID_NotExists");
	}

	/**
	 * Tests the findProductionTypeID method to find a productionType ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindProductionTypeID_Exists () throws RecordNotFoundException
	{
		final ServerDatabase db = new ServerDatabase ();
		for (int n = 1; n <= 3; n++)
		{
			final ProductionType newProductionType = new ProductionType ();
			newProductionType.setProductionTypeID ("RE0" + n);
			newProductionType.setProductionTypeDescription ("ProductionType " + n);

			db.getProductionType ().add (newProductionType);
		}

		final ServerDatabaseLookup lookup = new ServerDatabaseLookup (db);

		assertEquals ("RE02", lookup.findProductionType ("RE02", "testFindProductionTypeID_Exists").getProductionTypeID ());
		assertEquals ("ProductionType 2", lookup.findProductionType ("RE02", "testFindProductionTypeID_Exists").getProductionTypeDescription ());
	}

	/**
	 * Tests the findProductionTypeID method to find a productionType ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindProductionTypeID_NotExists () throws RecordNotFoundException
	{
		final ServerDatabase db = new ServerDatabase ();
		for (int n = 1; n <= 3; n++)
		{
			final ProductionType newProductionType = new ProductionType ();
			newProductionType.setProductionTypeID ("RE0" + n);
			newProductionType.setProductionTypeDescription ("ProductionType " + n);

			db.getProductionType ().add (newProductionType);
		}

		final ServerDatabaseLookup lookup = new ServerDatabaseLookup (db);

		lookup.findProductionType ("RE04", "testFindProductionTypeID_NotExists");
	}

	/**
	 * Tests the findPickTypeID method to find a pickType ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindPickTypeID_Exists () throws RecordNotFoundException
	{
		final ServerDatabase db = new ServerDatabase ();
		for (int n = 1; n <= 3; n++)
		{
			final PickType newPickType = new PickType ();
			newPickType.setPickTypeID ("PT0" + n);

			final PickTypeCountContainer container = new PickTypeCountContainer ();
			container.setCount (n);
			newPickType.getPickTypeCount ().add (container);

			db.getPickType ().add (newPickType);
		}

		final ServerDatabaseLookup lookup = new ServerDatabaseLookup (db);

		assertEquals ("PT02", lookup.findPickType ("PT02", "testFindPickTypeID_Exists").getPickTypeID ());
		assertEquals (1, lookup.findPickType ("PT02", "testFindPickTypeID_Exists").getPickTypeCount ().size ());
		assertEquals (2, lookup.findPickType ("PT02", "testFindPickTypeID_Exists").getPickTypeCount ().get (0).getCount ());
	}

	/**
	 * Tests the findPickTypeID method to find a pickType ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindPickTypeID_NotExists () throws RecordNotFoundException
	{
		final ServerDatabase db = new ServerDatabase ();
		for (int n = 1; n <= 3; n++)
		{
			final PickType newPickType = new PickType ();
			newPickType.setPickTypeID ("PT0" + n);
			db.getPickType ().add (newPickType);
		}

		final ServerDatabaseLookup lookup = new ServerDatabaseLookup (db);

		lookup.findPickType ("PT04", "testFindPickTypeID_NotExists");
	}

	/**
	 * Tests the findPickID method to find a pick ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindPickID_Exists () throws RecordNotFoundException
	{
		final ServerDatabase db = new ServerDatabase ();
		for (int n = 1; n <= 3; n++)
		{
			final Pick newPick = new Pick ();
			newPick.setPickID ("MB0" + n);
			newPick.setPickInitialSkill (2);
			db.getPick ().add (newPick);
		}

		final ServerDatabaseLookup lookup = new ServerDatabaseLookup (db);

		assertEquals ("MB02", lookup.findPick ("MB02", "testFindPickID_Exists").getPickID ());
		assertEquals (2, lookup.findPick ("MB02", "testFindPickID_Exists").getPickInitialSkill ().intValue ());
	}

	/**
	 * Tests the findPickID method to find a pick ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindPickID_NotExists () throws RecordNotFoundException
	{
		final ServerDatabase db = new ServerDatabase ();
		for (int n = 1; n <= 3; n++)
		{
			final Pick newPick = new Pick ();
			newPick.setPickID ("MB0" + n);
			db.getPick ().add (newPick);
		}

		final ServerDatabaseLookup lookup = new ServerDatabaseLookup (db);

		lookup.findPick ("MB04", "testFindPickID_NotExists");
	}

	/**
	 * Tests the findWizardID method to find a wizard ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindWizardID_Exists () throws RecordNotFoundException
	{
		final ServerDatabase db = new ServerDatabase ();
		for (int n = 1; n <= 3; n++)
		{
			final Wizard newWizard = new Wizard ();
			newWizard.setWizardID ("WZ0" + n);

			final WizardPickCount pick = new WizardPickCount ();
			pick.setPickCount (10 + n);
			newWizard.getWizardPickCount ().add (pick);

			db.getWizard ().add (newWizard);
		}

		final ServerDatabaseLookup lookup = new ServerDatabaseLookup (db);

		assertEquals ("WZ02", lookup.findWizard ("WZ02", "testFindWizardID_Exists").getWizardID ());
		assertEquals (1, lookup.findWizard ("WZ02", "testFindWizardID_Exists").getWizardPickCount ().size ());
		assertEquals (12, lookup.findWizard ("WZ02", "testFindWizardID_Exists").getWizardPickCount ().get (0).getPickCount ());
	}

	/**
	 * Tests the findWizardID method to find a wizard ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindWizardID_NotExists () throws RecordNotFoundException
	{
		final ServerDatabase db = new ServerDatabase ();
		for (int n = 1; n <= 3; n++)
		{
			final Wizard newWizard = new Wizard ();
			newWizard.setWizardID ("WZ0" + n);
			db.getWizard ().add (newWizard);
		}

		final ServerDatabaseLookup lookup = new ServerDatabaseLookup (db);

		lookup.findWizard ("WZ04", "testFindWizardID_NotExists");
	}

	/**
	 * Tests the findUnitID method to find a unit ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindUnitID_Exists () throws RecordNotFoundException
	{
		final ServerDatabase db = new ServerDatabase ();
		for (int n = 1; n <= 3; n++)
		{
			final Unit newUnit = new Unit ();
			newUnit.setUnitID ("UN00" + n);
			newUnit.setUnitName ("Unit name " + n);

			db.getUnit ().add (newUnit);
		}

		final ServerDatabaseLookup lookup = new ServerDatabaseLookup (db);

		assertEquals ("UN002", lookup.findUnit ("UN002", "testFindUnitID_Exists").getUnitID ());
		assertEquals ("Unit name 2", lookup.findUnit ("UN002", "testFindUnitID_Exists").getUnitName ());
	}

	/**
	 * Tests the findUnitID method to find a unit ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindUnitID_NotExists () throws RecordNotFoundException
	{
		final ServerDatabase db = new ServerDatabase ();
		for (int n = 1; n <= 3; n++)
		{
			final Unit newUnit = new Unit ();
			newUnit.setUnitID ("UN00" + n);
			db.getUnit ().add (newUnit);
		}

		final ServerDatabaseLookup lookup = new ServerDatabaseLookup (db);

		lookup.findUnit ("UN004", "testFindUnitID_NotExists");
	}

	/**
	 * Tests the findUnitSkillID method to find a unit skill ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindUnitSkillID_Exists () throws RecordNotFoundException
	{
		final ServerDatabase db = new ServerDatabase ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitSkill newUnitSkill = new UnitSkill ();
			newUnitSkill.setUnitSkillID ("US00" + n);
			newUnitSkill.setUnitSkillScoutingRange (n);

			db.getUnitSkill ().add (newUnitSkill);
		}

		final ServerDatabaseLookup lookup = new ServerDatabaseLookup (db);

		assertEquals (2, lookup.findUnitSkill ("US002", "testFindUnitSkillID_Exists").getUnitSkillScoutingRange ().intValue ());
	}

	/**
	 * Tests the findUnitSkillID method to find a unit skill ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindUnitSkillID_NotExists () throws RecordNotFoundException
	{
		final ServerDatabase db = new ServerDatabase ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitSkill newUnitSkill = new UnitSkill ();
			newUnitSkill.setUnitSkillID ("US00" + n);
			newUnitSkill.setUnitSkillScoutingRange (n);

			db.getUnitSkill ().add (newUnitSkill);
		}

		final ServerDatabaseLookup lookup = new ServerDatabaseLookup (db);

		lookup.findUnitSkill ("US004", "testFindUnitSkillID_NotExists");
	}

	/**
	 * Tests the findRaceID method to find a race ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindRaceID_Exists () throws RecordNotFoundException
	{
		final ServerDatabase db = new ServerDatabase ();
		for (int n = 1; n <= 3; n++)
		{
			final Race newRace = new Race ();
			newRace.setRaceID ("RC0" + n);

			final CityNameContainer cityName = new CityNameContainer ();
			cityName.setCityName ("Blah");
			newRace.getCityName ().add (cityName);

			db.getRace ().add (newRace);
		}

		final ServerDatabaseLookup lookup = new ServerDatabaseLookup (db);

		assertEquals ("RC02", lookup.findRace ("RC02", "testFindRaceID_Exists").getRaceID ());
		assertEquals (1, lookup.findRace ("RC02", "testFindRaceID_Exists").getCityName ().size ());
		assertEquals ("Blah", lookup.findRace ("RC02", "testFindRaceID_Exists").getCityName ().get (0).getCityName ());
	}

	/**
	 * Tests the findRaceID method to find a race ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindRaceID_NotExists () throws RecordNotFoundException
	{
		final ServerDatabase db = new ServerDatabase ();
		for (int n = 1; n <= 3; n++)
		{
			final Race newRace = new Race ();
			newRace.setRaceID ("RC0" + n);
			db.getRace ().add (newRace);
		}

		final ServerDatabaseLookup lookup = new ServerDatabaseLookup (db);

		lookup.findRace ("RC04", "testFindRaceID_NotExists");
	}

	/**
	 * Tests the findBuilding method to find a building ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindBuilding_Exists () throws RecordNotFoundException
	{
		final ServerDatabase db = new ServerDatabase ();
		for (int n = 1; n <= 3; n++)
		{
			final Building newBuilding = new Building ();
			newBuilding.setBuildingID ("BL0" + n);
			newBuilding.setBuildingScoutingRange (n);
			db.getBuilding ().add (newBuilding);
		}

		final ServerDatabaseLookup lookup = new ServerDatabaseLookup (db);

		assertEquals (2, lookup.findBuilding ("BL02", "testFindBuilding_Exists").getBuildingScoutingRange ().intValue ());
	}

	/**
	 * Tests the findBuilding method to find a building ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindBuilding_NotExists () throws RecordNotFoundException
	{
		final ServerDatabase db = new ServerDatabase ();
		for (int n = 1; n <= 3; n++)
		{
			final Building newBuilding = new Building ();
			newBuilding.setBuildingID ("BL0" + n);
			newBuilding.setBuildingScoutingRange (n);
			db.getBuilding ().add (newBuilding);
		}

		final ServerDatabaseLookup lookup = new ServerDatabaseLookup (db);

		assertNull (lookup.findBuilding ("BL04", "testFindBuilding_NotExists"));
	}

	/**
	 * Tests the findSpellID method to find a spell ID that does exist
	 * @throws RecordNotFoundException If we can't find it
	 */
	@Test
	public final void testFindSpellID_Exists () throws RecordNotFoundException
	{
		final ServerDatabase db = new ServerDatabase ();
		for (int n = 1; n <= 3; n++)
		{
			final Spell newSpell = new Spell ();
			newSpell.setSpellID ("SP00" + n);
			newSpell.setAiResearchOrder (n);
			db.getSpell ().add (newSpell);
		}

		final ServerDatabaseLookup lookup = new ServerDatabaseLookup (db);

		assertEquals ("SP002", lookup.findSpell ("SP002", "testFindSpellID_Exists").getSpellID ());
		assertEquals (2, lookup.findSpell ("SP002", "testFindSpellID_Exists").getAiResearchOrder ().intValue ());
	}

	/**
	 * Tests the findSpellID method to find a spell ID that doesn't exist
	 * @throws RecordNotFoundException If we can't find it as expected
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindSpellID_NotExists () throws RecordNotFoundException
	{
		final ServerDatabase db = new ServerDatabase ();
		for (int n = 1; n <= 3; n++)
		{
			final Spell newSpell = new Spell ();
			newSpell.setSpellID ("SP00" + n);
			db.getSpell ().add (newSpell);
		}

		final ServerDatabaseLookup lookup = new ServerDatabaseLookup (db);

		lookup.findSpell ("SP004", "testFindSpellID_NotExists");
	}
}
