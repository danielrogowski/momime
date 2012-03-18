
package momime.server.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.UnitUtils;
import momime.common.messages.v0_9_4.AvailableUnit;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.common.messages.v0_9_4.UnitAddBumpTypeID;
import momime.common.messages.v0_9_4.UnitSpecialOrder;
import momime.server.ServerTestData;
import momime.server.database.JAXBContextCreator;
import momime.server.database.ServerDatabaseLookup;
import momime.server.database.v0_9_4.ServerDatabase;
import momime.server.database.v0_9_4.UnitSkill;

import org.junit.Test;

/**
 * Tests the UnitServerUtils class
 */
public final class TestUnitServerUtils
{
	/** Dummy logger to use during unit tests */
	private final Logger debugLogger = Logger.getLogger ("MoMIMEServerUnitTests");

	/**
	 * Tests the generateHeroNameAndRandomSkills method on a hero who gets no random rolled skills
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws MomException If we find a hero who has no possible names defined, or who needs a random skill and we can't find a suitable one
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 */
	@Test
	public final void testGenerateHeroNameAndRandomSkills_NoExtras () throws IOException, JAXBException, MomException, RecordNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		// Dwarf hero
		final MemoryUnit unit = UnitUtils.createMemoryUnit ("UN001", 3, 0, 0, true, db, debugLogger);
		assertNull (unit.getHeroNameID ());

		// Run test
		UnitServerUtils.generateHeroNameAndRandomSkills (unit, db, debugLogger);
		assertEquals ("UN001_HN0", unit.getHeroNameID ().substring (0, 9));

		assertEquals (4, unit.getUnitHasSkill ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE, unit.getUnitHasSkill ().get (0).getUnitSkillID ());
		assertEquals (0, unit.getUnitHasSkill ().get (0).getUnitSkillValue ().intValue ());
		assertEquals ("US002", unit.getUnitHasSkill ().get (1).getUnitSkillID ());
		assertNull (unit.getUnitHasSkill ().get (1).getUnitSkillValue ());
		assertEquals ("USX01", unit.getUnitHasSkill ().get (2).getUnitSkillID ());
		assertNull (unit.getUnitHasSkill ().get (2).getUnitSkillValue ());
		assertEquals ("HS07", unit.getUnitHasSkill ().get (3).getUnitSkillID ());
		assertEquals (1, unit.getUnitHasSkill ().get (3).getUnitSkillValue ().intValue ());
	}

	/**
	 * Tests the generateHeroNameAndRandomSkills method on a hero who only has one viable skill to pick (all others are "only if have already")
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws MomException If we find a hero who has no possible names defined, or who needs a random skill and we can't find a suitable one
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 */
	@Test
	public final void testGenerateHeroNameAndRandomSkills_SingleChoice () throws IOException, JAXBException, MomException, RecordNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		// Orc warrior - gets 1 fighter pick, but has no skills, so none that are "only if have already" will be available
		final MemoryUnit unit = UnitUtils.createMemoryUnit ("UN007", 3, 0, 0, true, db, debugLogger);
		assertNull (unit.getHeroNameID ());

		// Set every skill except for HS11 to "only if have already", so HS11 remains the only possible choice
		for (final UnitSkill thisSkill : serverDB.getUnitSkill ())
			if ((thisSkill.getUnitSkillID ().startsWith ("HS")) && (!thisSkill.getUnitSkillID ().equals ("HS11")))
				thisSkill.setOnlyIfHaveAlready (true);

		// Run test
		UnitServerUtils.generateHeroNameAndRandomSkills (unit, db, debugLogger);
		assertEquals ("UN007_HN0", unit.getHeroNameID ().substring (0, 9));

		assertEquals (5, unit.getUnitHasSkill ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE, unit.getUnitHasSkill ().get (0).getUnitSkillID ());
		assertEquals (0, unit.getUnitHasSkill ().get (0).getUnitSkillValue ().intValue ());
		assertEquals ("US126", unit.getUnitHasSkill ().get (1).getUnitSkillID ());
		assertEquals (3, unit.getUnitHasSkill ().get (1).getUnitSkillValue ().intValue ());
		assertEquals ("US002", unit.getUnitHasSkill ().get (2).getUnitSkillID ());
		assertNull (unit.getUnitHasSkill ().get (2).getUnitSkillValue ());
		assertEquals ("USX01", unit.getUnitHasSkill ().get (3).getUnitSkillID ());
		assertNull (unit.getUnitHasSkill ().get (3).getUnitSkillValue ());
		assertEquals ("HS11", unit.getUnitHasSkill ().get (4).getUnitSkillID ());
		assertEquals (1, unit.getUnitHasSkill ().get (4).getUnitSkillValue ().intValue ());
	}

	/**
	 * Tests the generateHeroNameAndRandomSkills method on a hero who only has one viable skill to pick (it is "only if have already" and we do have the skill)
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws MomException If we find a hero who has no possible names defined, or who needs a random skill and we can't find a suitable one
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 */
	@Test
	public final void testGenerateHeroNameAndRandomSkills_SingleChoiceHaveAlready () throws IOException, JAXBException, MomException, RecordNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		// Healer hero - gets 1 mage pick
		final MemoryUnit unit = UnitUtils.createMemoryUnit ("UN008", 3, 0, 0, true, db, debugLogger);
		assertNull (unit.getHeroNameID ());

		// Set every skill to "only if have already", so HS05 remains the only possible choice because its the only one we have
		for (final UnitSkill thisSkill : serverDB.getUnitSkill ())
			if (thisSkill.getUnitSkillID ().startsWith ("HS"))
				thisSkill.setOnlyIfHaveAlready (true);

		// Run test
		UnitServerUtils.generateHeroNameAndRandomSkills (unit, db, debugLogger);
		assertEquals ("UN008_HN0", unit.getHeroNameID ().substring (0, 9));

		assertEquals (4, unit.getUnitHasSkill ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE, unit.getUnitHasSkill ().get (0).getUnitSkillID ());
		assertEquals (0, unit.getUnitHasSkill ().get (0).getUnitSkillValue ().intValue ());
		assertEquals ("USX01", unit.getUnitHasSkill ().get (1).getUnitSkillID ());
		assertNull (unit.getUnitHasSkill ().get (1).getUnitSkillValue ());
		assertEquals ("US016", unit.getUnitHasSkill ().get (2).getUnitSkillID ());
		assertNull (unit.getUnitHasSkill ().get (2).getUnitSkillValue ());
		assertEquals ("HS05", unit.getUnitHasSkill ().get (3).getUnitSkillID ());
		assertEquals (4, unit.getUnitHasSkill ().get (3).getUnitSkillValue ().intValue ());
	}

	/**
	 * Tests the generateHeroNameAndRandomSkills method on a hero who only has no viable skills to pick (they are all "only if have already" and we have none of them)
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws MomException If we find a hero who has no possible names defined, or who needs a random skill and we can't find a suitable one
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 */
	@Test(expected=MomException.class)
	public final void testGenerateHeroNameAndRandomSkills_NoChoices () throws IOException, JAXBException, MomException, RecordNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		// Orc warrior - gets 1 fighter pick, but has no skills, so none that are "only if have already" will be available
		final MemoryUnit unit = UnitUtils.createMemoryUnit ("UN007", 3, 0, 0, true, db, debugLogger);
		assertNull (unit.getHeroNameID ());

		// Set every skill except to "only if have already", so none are possible choices
		for (final UnitSkill thisSkill : serverDB.getUnitSkill ())
			if (thisSkill.getUnitSkillID ().startsWith ("HS"))
				thisSkill.setOnlyIfHaveAlready (true);

		// Run test
		UnitServerUtils.generateHeroNameAndRandomSkills (unit, db, debugLogger);
	}

	/**
	 * Tests the generateHeroNameAndRandomSkills method, providing that heroes with a defined hero random pick type can get skills with a blank hero random pick type
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws MomException If we find a hero who has no possible names defined, or who needs a random skill and we can't find a suitable one
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 */
	@Test
	public final void testGenerateHeroNameAndRandomSkills_SingleChoice_SkillNotTypeSpecific () throws IOException, JAXBException, MomException, RecordNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		// Orc warrior - gets 1 fighter pick, but has no skills, so none that are "only if have already" will be available
		final MemoryUnit unit = UnitUtils.createMemoryUnit ("UN007", 3, 0, 0, true, db, debugLogger);
		assertNull (unit.getHeroNameID ());

		// Set every skill except for HS10 to "only if have already", so HS10 remains the only possible choice
		// Note HS10 isn't a fighter skill, it has no hero random pick type defined so can be obtained by anyone
		for (final UnitSkill thisSkill : serverDB.getUnitSkill ())
			if ((thisSkill.getUnitSkillID ().startsWith ("HS")) && (!thisSkill.getUnitSkillID ().equals ("HS10")))
				thisSkill.setOnlyIfHaveAlready (true);

		// Run test
		UnitServerUtils.generateHeroNameAndRandomSkills (unit, db, debugLogger);
		assertEquals ("UN007_HN0", unit.getHeroNameID ().substring (0, 9));

		assertEquals (5, unit.getUnitHasSkill ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE, unit.getUnitHasSkill ().get (0).getUnitSkillID ());
		assertEquals (0, unit.getUnitHasSkill ().get (0).getUnitSkillValue ().intValue ());
		assertEquals ("US126", unit.getUnitHasSkill ().get (1).getUnitSkillID ());
		assertEquals (3, unit.getUnitHasSkill ().get (1).getUnitSkillValue ().intValue ());
		assertEquals ("US002", unit.getUnitHasSkill ().get (2).getUnitSkillID ());
		assertNull (unit.getUnitHasSkill ().get (2).getUnitSkillValue ());
		assertEquals ("USX01", unit.getUnitHasSkill ().get (3).getUnitSkillID ());
		assertNull (unit.getUnitHasSkill ().get (3).getUnitSkillValue ());
		assertEquals ("HS10", unit.getUnitHasSkill ().get (4).getUnitSkillID ());
		assertEquals (1, unit.getUnitHasSkill ().get (4).getUnitSkillValue ().intValue ());
	}

	/**
	 * Tests the generateHeroNameAndRandomSkills method, providing that heroes without a defined hero random pick type can get skills even if they do specify a hero random pick type
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws MomException If we find a hero who has no possible names defined, or who needs a random skill and we can't find a suitable one
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 */
	@Test
	public final void testGenerateHeroNameAndRandomSkills_SingleChoice_HeroNotTypeSpecific () throws IOException, JAXBException, MomException, RecordNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		// Warrior Mage - gets 1 pick of any type, and has HS05
		final MemoryUnit unit = UnitUtils.createMemoryUnit ("UN013", 3, 0, 0, true, db, debugLogger);
		assertNull (unit.getHeroNameID ());

		// Set every skill except to "only if have already", so HS05 remains the only possible choice
		// Note HS05 is a mage specific skill
		for (final UnitSkill thisSkill : serverDB.getUnitSkill ())
			if (thisSkill.getUnitSkillID ().startsWith ("HS"))
				thisSkill.setOnlyIfHaveAlready (true);

		// Run test
		UnitServerUtils.generateHeroNameAndRandomSkills (unit, db, debugLogger);
		assertEquals ("UN013_HN0", unit.getHeroNameID ().substring (0, 9));

		assertEquals (3, unit.getUnitHasSkill ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE, unit.getUnitHasSkill ().get (0).getUnitSkillID ());
		assertEquals (0, unit.getUnitHasSkill ().get (0).getUnitSkillValue ().intValue ());
		assertEquals ("USX01", unit.getUnitHasSkill ().get (1).getUnitSkillID ());
		assertNull (unit.getUnitHasSkill ().get (1).getUnitSkillValue ());
		assertEquals ("HS05", unit.getUnitHasSkill ().get (2).getUnitSkillID ());
		assertEquals (3, unit.getUnitHasSkill ().get (2).getUnitSkillValue ().intValue ());
	}

	/**
	 * Tests the generateHeroNameAndRandomSkills method where we add lots of adds into a single skill to prove it isn't capped
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws MomException If we find a hero who has no possible names defined, or who needs a random skill and we can't find a suitable one
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 */
	@Test
	public final void testGenerateHeroNameAndRandomSkills_Uncapped () throws IOException, JAXBException, MomException, RecordNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		// Unknown hero - gets 5 picks of any type, and has HS05
		final MemoryUnit unit = UnitUtils.createMemoryUnit ("UN025", 3, 0, 0, true, db, debugLogger);
		assertNull (unit.getHeroNameID ());

		// Set every skill to "only if have already", so HS05 remains the only possible choice
		for (final UnitSkill thisSkill : serverDB.getUnitSkill ())
			if (thisSkill.getUnitSkillID ().startsWith ("HS"))
				thisSkill.setOnlyIfHaveAlready (true);

		// Run test
		UnitServerUtils.generateHeroNameAndRandomSkills (unit, db, debugLogger);
		assertEquals ("UN025_HN0", unit.getHeroNameID ().substring (0, 9));

		assertEquals (3, unit.getUnitHasSkill ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE, unit.getUnitHasSkill ().get (0).getUnitSkillID ());
		assertEquals (0, unit.getUnitHasSkill ().get (0).getUnitSkillValue ().intValue ());
		assertEquals ("USX01", unit.getUnitHasSkill ().get (1).getUnitSkillID ());
		assertNull (unit.getUnitHasSkill ().get (1).getUnitSkillValue ());
		assertEquals ("HS05", unit.getUnitHasSkill ().get (2).getUnitSkillID ());
		assertEquals (7, unit.getUnitHasSkill ().get (2).getUnitSkillValue ().intValue ());
	}

	/**
	 * Tests the generateHeroNameAndRandomSkills method where we run out of skills to add to because they're all capped with a maxOccurrences value
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws MomException If we find a hero who has no possible names defined, or who needs a random skill and we can't find a suitable one
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 */
	@Test
	public final void testGenerateHeroNameAndRandomSkills_Capped () throws IOException, JAXBException, MomException, RecordNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		// Unknown hero - gets 5 picks of any type
		final MemoryUnit unit = UnitUtils.createMemoryUnit ("UN025", 3, 0, 0, true, db, debugLogger);
		assertNull (unit.getHeroNameID ());

		// Alter unit to have 2xHS01 instead of 2xHS05 (HS05 in uncapped so no use for this test)
		unit.getUnitHasSkill ().get (2).setUnitSkillID ("HS01");

		// Set every skill except HS01,3 and 4 to "only if have already", so these are our only possible choices
		for (final UnitSkill thisSkill : serverDB.getUnitSkill ())
			if ((thisSkill.getUnitSkillID ().startsWith ("HS")) && (!thisSkill.getUnitSkillID ().equals ("HS01")) && (!thisSkill.getUnitSkillID ().equals ("HS03")) && (!thisSkill.getUnitSkillID ().equals ("HS04")))
				thisSkill.setOnlyIfHaveAlready (true);

		// Run test - since HS01 is already maxed, we'll put 2 points into HS03, 2 points into HS04, then bomb out because we
		// still have to put a 5th point somewhere and there's nowhere left for it to go
		try
		{
			UnitServerUtils.generateHeroNameAndRandomSkills (unit, db, debugLogger);
			fail ("generateHeroNameAndRandomSkills should have ran out of available skills to choose");
		}
		catch (final MomException e)
		{
			// Expected result
		}

		// We need to prove that it got as far as filling out HS03 and HS04 before it bombed out - if it bombed out right at the start, fail the test
		assertEquals ("UN025_HN0", unit.getHeroNameID ().substring (0, 9));

		assertEquals (5, unit.getUnitHasSkill ().size ());
		assertEquals (CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE, unit.getUnitHasSkill ().get (0).getUnitSkillID ());
		assertEquals (0, unit.getUnitHasSkill ().get (0).getUnitSkillValue ().intValue ());
		assertEquals ("USX01", unit.getUnitHasSkill ().get (1).getUnitSkillID ());
		assertNull (unit.getUnitHasSkill ().get (1).getUnitSkillValue ());
		assertEquals ("HS01", unit.getUnitHasSkill ().get (2).getUnitSkillID ());
		assertEquals (2, unit.getUnitHasSkill ().get (2).getUnitSkillValue ().intValue ());
		assertEquals (2, unit.getUnitHasSkill ().get (3).getUnitSkillValue ().intValue ());
		assertEquals (2, unit.getUnitHasSkill ().get (4).getUnitSkillValue ().intValue ());

		// HS03 and HS04 could have been added in either order
		final List<String> skillIDs = new ArrayList<String> ();
		for (int n = 3; n <= 4; n++)
			skillIDs.add (unit.getUnitHasSkill ().get (n).getUnitSkillID ());

		Collections.sort (skillIDs);

		assertEquals ("HS03", skillIDs.get (0));
		assertEquals ("HS04", skillIDs.get (1));
	}

	/**
	 * Tests the doesUnitSpecialOrderResultInDeath method
	 */
	@Test
	public final void testDoesUnitSpecialOrderResultInDeath ()
	{
		assertFalse (UnitServerUtils.doesUnitSpecialOrderResultInDeath (null));
		assertFalse (UnitServerUtils.doesUnitSpecialOrderResultInDeath (UnitSpecialOrder.BUILD_ROAD));
		assertTrue (UnitServerUtils.doesUnitSpecialOrderResultInDeath (UnitSpecialOrder.BUILD_CITY));
	}

	/**
	 * Tests the findUnitURN method on a unit that does exist
	 * @throws RecordNotFoundException If unit with requested URN is not found
	 */
	@Test
	public final void testFindUnitURN () throws RecordNotFoundException
	{
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();
		for (int n = 1; n <= 3; n++)
		{
			final MemoryUnit unit = new MemoryUnit ();
			unit.setOwningPlayerID (n);
			unit.setUnitID ("UN00" + new Integer (n+1).toString ());
			units.add (unit);
		}

		assertEquals (2, UnitServerUtils.findUnitWithPlayerAndID (units, 2, "UN003", debugLogger).getOwningPlayerID ());
		assertNull (UnitServerUtils.findUnitWithPlayerAndID (units, 2, "UN002", debugLogger));
	}

	/**
	 * Tests the canUnitBeAddedHere method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 */
	@Test
	public final void testCanUnitBeAddedHere () throws IOException, JAXBException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		final List<String> emptySkillList = new ArrayList<String> ();

		// Map
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (serverDB, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Lair here
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID (CommonDatabaseConstants.VALUE_TILE_TYPE_FOREST);
		terrainData.setMapFeatureID ("MF13");
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);

		final AvailableUnit spearmen = new AvailableUnit ();
		spearmen.setOwningPlayerID (2);
		spearmen.setUnitID ("UN105");
		UnitUtils.initializeUnitSkills (spearmen,  0, true, db, debugLogger);

		final OverlandMapCoordinates addLocation = new OverlandMapCoordinates ();
		addLocation.setX (20);
		addLocation.setY (10);
		addLocation.setPlane (1);

		assertFalse (UnitServerUtils.canUnitBeAddedHere (addLocation, spearmen, emptySkillList, trueMap, sd.getUnitSetting (), db, debugLogger));

		// Nothing here
		terrainData.setMapFeatureID (null);
		assertTrue (UnitServerUtils.canUnitBeAddedHere (addLocation, spearmen, emptySkillList, trueMap, sd.getUnitSetting (), db, debugLogger));

		// Enemy city here
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityPopulation (1);
		cityData.setCityOwnerID (3);
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		assertFalse (UnitServerUtils.canUnitBeAddedHere (addLocation, spearmen, emptySkillList, trueMap, sd.getUnitSetting (), db, debugLogger));

		// Our city here
		cityData.setCityOwnerID (2);
		assertTrue (UnitServerUtils.canUnitBeAddedHere (addLocation, spearmen, emptySkillList, trueMap, sd.getUnitSetting (), db, debugLogger));

		// Enemy unit here
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (null);

		final OverlandMapCoordinates unitLocation = new OverlandMapCoordinates ();
		unitLocation.setX (20);
		unitLocation.setY (10);
		unitLocation.setPlane (1);

		final MemoryUnit unitInTheWay = UnitUtils.createMemoryUnit ("UN105", 1, 0, 0, true, db, debugLogger);
		unitInTheWay.setOwningPlayerID (3);
		unitInTheWay.setUnitLocation (unitLocation);

		trueMap.getUnit ().add (unitInTheWay);

		assertFalse (UnitServerUtils.canUnitBeAddedHere (addLocation, spearmen, emptySkillList, trueMap, sd.getUnitSetting (), db, debugLogger));

		// Our units here, but space left
		unitInTheWay.setOwningPlayerID (2);
		assertTrue (UnitServerUtils.canUnitBeAddedHere (addLocation, spearmen, emptySkillList, trueMap, sd.getUnitSetting (), db, debugLogger));

		// Our units here, space left, but cell is impassable to the type of unit we're trying to add
		final AvailableUnit trireme = new AvailableUnit ();
		trireme.setOwningPlayerID (2);
		trireme.setUnitID ("UN016");
		UnitUtils.initializeUnitSkills (trireme,  0, true, db, debugLogger);

		assertFalse (UnitServerUtils.canUnitBeAddedHere (addLocation, trireme, emptySkillList, trueMap, sd.getUnitSetting (), db, debugLogger));

		// Our units have the cell full already
		for (int n = 2; n <= sd.getUnitSetting ().getUnitsPerMapCell (); n++)
		{
			final OverlandMapCoordinates anotherUnitLocation = new OverlandMapCoordinates ();
			anotherUnitLocation.setX (20);
			anotherUnitLocation.setY (10);
			anotherUnitLocation.setPlane (1);

			final MemoryUnit anotherUnitInTheWay = UnitUtils.createMemoryUnit ("UN105", n, 0, 0, true, db, debugLogger);
			anotherUnitInTheWay.setOwningPlayerID (2);
			anotherUnitInTheWay.setUnitLocation (anotherUnitLocation);

			trueMap.getUnit ().add (anotherUnitInTheWay);

			if (n < sd.getUnitSetting ().getUnitsPerMapCell ())
				assertTrue (UnitServerUtils.canUnitBeAddedHere (addLocation, spearmen, emptySkillList, trueMap, sd.getUnitSetting (), db, debugLogger));
			else
				assertFalse (UnitServerUtils.canUnitBeAddedHere (addLocation, spearmen, emptySkillList, trueMap, sd.getUnitSetting (), db, debugLogger));
		}
	}

	/**
	 * Tests the findNearestLocationWhereUnitCanBeAdded method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 */
	@Test
	public final void testFindNearestLocationWhereUnitCanBeAdded () throws IOException, JAXBException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		// Map
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (serverDB, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Put regular tiles in a 1 radius (3x3 pattern)
		for (int x = 19; x <= 21; x++)
			for (int y = 9; y <= 11; y++)
			{
				final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
				terrainData.setTileTypeID (CommonDatabaseConstants.VALUE_TILE_TYPE_FOREST);
				trueTerrain.getPlane ().get (1).getRow ().get (y).getCell ().get (x).setTerrainData (terrainData);
			}

		// Put 8 of our units in the middle
		for (int n = 1; n < sd.getUnitSetting ().getUnitsPerMapCell (); n++)
		{
			final OverlandMapCoordinates anotherUnitLocation = new OverlandMapCoordinates ();
			anotherUnitLocation.setX (20);
			anotherUnitLocation.setY (10);
			anotherUnitLocation.setPlane (1);

			final MemoryUnit anotherUnitInTheWay = UnitUtils.createMemoryUnit ("UN105", n, 0, 0, true, db, debugLogger);
			anotherUnitInTheWay.setOwningPlayerID (2);
			anotherUnitInTheWay.setUnitLocation (anotherUnitLocation);

			trueMap.getUnit ().add (anotherUnitInTheWay);
		}

		// Run test
		final OverlandMapCoordinates desiredLocation = new OverlandMapCoordinates ();
		desiredLocation.setX (20);
		desiredLocation.setY (10);
		desiredLocation.setPlane (1);

		final UnitAddLocation centre = UnitServerUtils.findNearestLocationWhereUnitCanBeAdded (desiredLocation, "UN105", 2, trueMap, sd, db, debugLogger);
		assertEquals (desiredLocation, centre.getUnitLocation ());
		assertEquals (UnitAddBumpTypeID.CITY, centre.getBumpType ());

		// Add a 9th unit in the middle
		final OverlandMapCoordinates anotherUnitLocation = new OverlandMapCoordinates ();
		anotherUnitLocation.setX (20);
		anotherUnitLocation.setY (10);
		anotherUnitLocation.setPlane (1);

		final MemoryUnit anotherUnitInTheWay = UnitUtils.createMemoryUnit ("UN105", 9, 0, 0, true, db, debugLogger);
		anotherUnitInTheWay.setOwningPlayerID (2);
		anotherUnitInTheWay.setUnitLocation (anotherUnitLocation);

		trueMap.getUnit ().add (anotherUnitInTheWay);

		// Add an enemy unit above us
		final OverlandMapCoordinates unitLocation = new OverlandMapCoordinates ();
		unitLocation.setX (20);
		unitLocation.setY (9);
		unitLocation.setPlane (1);

		final MemoryUnit unitInTheWay = UnitUtils.createMemoryUnit ("UN105", 9, 0, 0, true, db, debugLogger);
		unitInTheWay.setOwningPlayerID (3);
		unitInTheWay.setUnitLocation (unitLocation);

		trueMap.getUnit ().add (unitInTheWay);

		// Add an enemy city above-right of us
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityPopulation (1);
		cityData.setCityOwnerID (3);
		trueTerrain.getPlane ().get (1).getRow ().get (9).getCell ().get (21).setCityData (cityData);

		// So now we should get bumped to the right...
		final UnitAddLocation right = UnitServerUtils.findNearestLocationWhereUnitCanBeAdded (desiredLocation, "UN105", 2, trueMap, sd, db, debugLogger);
		assertEquals (21, right.getUnitLocation ().getX ());
		assertEquals (10, right.getUnitLocation ().getY ());
		assertEquals (1, right.getUnitLocation ().getPlane ());
		assertEquals (UnitAddBumpTypeID.BUMPED, right.getBumpType ());

		// Set every tile in the 3x3 area to be a lair!
		for (int x = 19; x <= 21; x++)
			for (int y = 9; y <= 11; y++)
				trueTerrain.getPlane ().get (1).getRow ().get (y).getCell ().get (x).getTerrainData ().setMapFeatureID ("MF13");

		// Now we won't fit anywhere
		final UnitAddLocation wontFit = UnitServerUtils.findNearestLocationWhereUnitCanBeAdded (desiredLocation, "UN105", 2, trueMap, sd, db, debugLogger);
		assertNull (wontFit.getUnitLocation ());
		assertEquals (UnitAddBumpTypeID.NO_ROOM, wontFit.getBumpType ());
	}
}
