package momime.server.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.servertoclient.v0_9_4.SetSpecialOrderMessage;
import momime.common.messages.v0_9_4.AvailableUnit;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.common.messages.v0_9_4.UnitAddBumpTypeID;
import momime.common.messages.v0_9_4.UnitSpecialOrder;
import momime.common.utils.IPendingMovementUtils;
import momime.common.utils.IUnitUtils;
import momime.common.utils.UnitUtils;
import momime.server.DummyServerToClientConnection;
import momime.server.ServerTestData;
import momime.server.calculations.MomServerUnitCalculations;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_4.UnitSkill;

import org.junit.Test;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the UnitServerUtils class
 */
public final class TestUnitServerUtils
{
	/**
	 * Tests the generateHeroNameAndRandomSkills method on a hero who gets no random rolled skills
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateHeroNameAndRandomSkills_NoExtras () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		
		// Set up object to test
		final UnitServerUtils utils = new UnitServerUtils ();
		final UnitUtils unitUtils = new UnitUtils ();
		utils.setUnitUtils (unitUtils);

		// Dwarf hero
		final MemoryUnit unit = unitUtils.createMemoryUnit ("UN001", 3, 0, 0, true, db);
		assertNull (unit.getHeroNameID ());

		// Run test
		utils.generateHeroNameAndRandomSkills (unit, db);
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
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateHeroNameAndRandomSkills_SingleChoice () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		// Set up object to test
		final UnitServerUtils utils = new UnitServerUtils ();
		final UnitUtils unitUtils = new UnitUtils ();
		utils.setUnitUtils (unitUtils);
		
		// Orc warrior - gets 1 fighter pick, but has no skills, so none that are "only if have already" will be available
		final MemoryUnit unit = unitUtils.createMemoryUnit ("UN007", 3, 0, 0, true, db);
		assertNull (unit.getHeroNameID ());

		// Set every skill except for HS11 to "only if have already", so HS11 remains the only possible choice
		for (final UnitSkill thisSkill : db.getUnitSkill ())
			if ((thisSkill.getUnitSkillID ().startsWith ("HS")) && (!thisSkill.getUnitSkillID ().equals ("HS11")))
				thisSkill.setOnlyIfHaveAlready (true);

		// Run test
		utils.generateHeroNameAndRandomSkills (unit, db);
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
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateHeroNameAndRandomSkills_SingleChoiceHaveAlready () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		// Set up object to test
		final UnitServerUtils utils = new UnitServerUtils ();
		final UnitUtils unitUtils = new UnitUtils ();
		utils.setUnitUtils (unitUtils);
		
		// Healer hero - gets 1 mage pick
		final MemoryUnit unit = unitUtils.createMemoryUnit ("UN008", 3, 0, 0, true, db);
		assertNull (unit.getHeroNameID ());

		// Set every skill to "only if have already", so HS05 remains the only possible choice because its the only one we have
		for (final UnitSkill thisSkill : db.getUnitSkill ())
			if (thisSkill.getUnitSkillID ().startsWith ("HS"))
				thisSkill.setOnlyIfHaveAlready (true);

		// Run test
		utils.generateHeroNameAndRandomSkills (unit, db);
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
	 * @throws Exception If there is a problem
	 */
	@Test(expected=MomException.class)
	public final void testGenerateHeroNameAndRandomSkills_NoChoices () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		// Set up object to test
		final UnitServerUtils utils = new UnitServerUtils ();
		final UnitUtils unitUtils = new UnitUtils ();
		utils.setUnitUtils (unitUtils);
		
		// Orc warrior - gets 1 fighter pick, but has no skills, so none that are "only if have already" will be available
		final MemoryUnit unit = unitUtils.createMemoryUnit ("UN007", 3, 0, 0, true, db);
		assertNull (unit.getHeroNameID ());

		// Set every skill except to "only if have already", so none are possible choices
		for (final UnitSkill thisSkill : db.getUnitSkill ())
			if (thisSkill.getUnitSkillID ().startsWith ("HS"))
				thisSkill.setOnlyIfHaveAlready (true);

		// Run test
		utils.generateHeroNameAndRandomSkills (unit, db);
	}

	/**
	 * Tests the generateHeroNameAndRandomSkills method, providing that heroes with a defined hero random pick type can get skills with a blank hero random pick type
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateHeroNameAndRandomSkills_SingleChoice_SkillNotTypeSpecific () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		// Set up object to test
		final UnitServerUtils utils = new UnitServerUtils ();
		final UnitUtils unitUtils = new UnitUtils ();
		utils.setUnitUtils (unitUtils);
		
		// Orc warrior - gets 1 fighter pick, but has no skills, so none that are "only if have already" will be available
		final MemoryUnit unit = unitUtils.createMemoryUnit ("UN007", 3, 0, 0, true, db);
		assertNull (unit.getHeroNameID ());

		// Set every skill except for HS10 to "only if have already", so HS10 remains the only possible choice
		// Note HS10 isn't a fighter skill, it has no hero random pick type defined so can be obtained by anyone
		for (final UnitSkill thisSkill : db.getUnitSkill ())
			if ((thisSkill.getUnitSkillID ().startsWith ("HS")) && (!thisSkill.getUnitSkillID ().equals ("HS10")))
				thisSkill.setOnlyIfHaveAlready (true);

		// Run test
		utils.generateHeroNameAndRandomSkills (unit, db);
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
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateHeroNameAndRandomSkills_SingleChoice_HeroNotTypeSpecific () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		// Set up object to test
		final UnitServerUtils utils = new UnitServerUtils ();
		final UnitUtils unitUtils = new UnitUtils ();
		utils.setUnitUtils (unitUtils);
		
		// Warrior Mage - gets 1 pick of any type, and has HS05
		final MemoryUnit unit = unitUtils.createMemoryUnit ("UN013", 3, 0, 0, true, db);
		assertNull (unit.getHeroNameID ());

		// Set every skill except to "only if have already", so HS05 remains the only possible choice
		// Note HS05 is a mage specific skill
		for (final UnitSkill thisSkill : db.getUnitSkill ())
			if (thisSkill.getUnitSkillID ().startsWith ("HS"))
				thisSkill.setOnlyIfHaveAlready (true);

		// Run test
		utils.generateHeroNameAndRandomSkills (unit, db);
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
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateHeroNameAndRandomSkills_Uncapped () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		// Set up object to test
		final UnitServerUtils utils = new UnitServerUtils ();
		final UnitUtils unitUtils = new UnitUtils ();
		utils.setUnitUtils (unitUtils);
		
		// Unknown hero - gets 5 picks of any type, and has HS05
		final MemoryUnit unit = unitUtils.createMemoryUnit ("UN025", 3, 0, 0, true, db);
		assertNull (unit.getHeroNameID ());

		// Set every skill to "only if have already", so HS05 remains the only possible choice
		for (final UnitSkill thisSkill : db.getUnitSkill ())
			if (thisSkill.getUnitSkillID ().startsWith ("HS"))
				thisSkill.setOnlyIfHaveAlready (true);

		// Run test
		utils.generateHeroNameAndRandomSkills (unit, db);
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
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateHeroNameAndRandomSkills_Capped () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		// Set up object to test
		final UnitServerUtils utils = new UnitServerUtils ();
		final UnitUtils unitUtils = new UnitUtils ();
		utils.setUnitUtils (unitUtils);
		
		// Unknown hero - gets 5 picks of any type
		final MemoryUnit unit = unitUtils.createMemoryUnit ("UN025", 3, 0, 0, true, db);
		assertNull (unit.getHeroNameID ());

		// Alter unit to have 2xHS01 instead of 2xHS05 (HS05 in uncapped so no use for this test)
		unit.getUnitHasSkill ().get (2).setUnitSkillID ("HS01");

		// Set every skill except HS01,3 and 4 to "only if have already", so these are our only possible choices
		for (final UnitSkill thisSkill : db.getUnitSkill ())
			if ((thisSkill.getUnitSkillID ().startsWith ("HS")) && (!thisSkill.getUnitSkillID ().equals ("HS01")) && (!thisSkill.getUnitSkillID ().equals ("HS03")) && (!thisSkill.getUnitSkillID ().equals ("HS04")))
				thisSkill.setOnlyIfHaveAlready (true);

		// Run test - since HS01 is already maxed, we'll put 2 points into HS03, 2 points into HS04, then bomb out because we
		// still have to put a 5th point somewhere and there's nowhere left for it to go
		try
		{
			utils.generateHeroNameAndRandomSkills (unit, db);
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
		// Set up object to test
		final UnitServerUtils utils = new UnitServerUtils ();

		// Run test
		assertFalse (utils.doesUnitSpecialOrderResultInDeath (null));
		assertFalse (utils.doesUnitSpecialOrderResultInDeath (UnitSpecialOrder.BUILD_ROAD));
		assertTrue (utils.doesUnitSpecialOrderResultInDeath (UnitSpecialOrder.BUILD_CITY));
	}
	
	/**
	 * Tests the setAndSendSpecialOrder method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSetAndSendSpecialOrder () throws Exception
	{
		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge (); 
		final MomTransientPlayerPrivateKnowledge trans = new MomTransientPlayerPrivateKnowledge ();

		priv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, trans);
		
		// Connection
		final DummyServerToClientConnection msgs = new DummyServerToClientConnection ();
		player.setConnection (msgs);
		
		// Unit
		final MemoryUnit trueUnit = new MemoryUnit ();
		trueUnit.setUnitURN (5);

		final MemoryUnit memoryUnit = new MemoryUnit ();
		memoryUnit.setUnitURN (5);
		
		// Set up object to test
		final IUnitUtils unitUtils = mock (IUnitUtils.class);
		when (unitUtils.findUnitURN (5, priv.getFogOfWarMemory ().getUnit (), "setAndSendSpecialOrder")).thenReturn (memoryUnit);
		
		final IPendingMovementUtils pendingMovementUtils = mock (IPendingMovementUtils.class);
		
		final UnitServerUtils utils = new UnitServerUtils ();
		utils.setUnitUtils (unitUtils);
		utils.setPendingMovementUtils (pendingMovementUtils);
		
		// Run test
		utils.setAndSendSpecialOrder (trueUnit, UnitSpecialOrder.BUILD_ROAD, player);
		
		// Check results
		assertEquals (UnitSpecialOrder.BUILD_ROAD, trueUnit.getSpecialOrder ());
		assertEquals (UnitSpecialOrder.BUILD_ROAD, memoryUnit.getSpecialOrder ());
		
		verify (pendingMovementUtils).removeUnitFromAnyPendingMoves (trans.getPendingMovement (), 5);
		
		assertEquals (1, msgs.getMessages ().size ());
		final SetSpecialOrderMessage msg = (SetSpecialOrderMessage) msgs.getMessages ().get (0);
		assertEquals (1, msg.getUnitURN ().size ());
		assertEquals (5, msg.getUnitURN ().get (0).intValue ());
		assertEquals (UnitSpecialOrder.BUILD_ROAD, msg.getSpecialOrder ());
	}

	/**
	 * Tests the findUnitWithPlayerAndID method on a unit that does exist
	 * @throws RecordNotFoundException If unit with requested URN is not found
	 */
	@Test
	public final void testFndUnitWithPlayerAndID () throws RecordNotFoundException
	{
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();
		for (int n = 1; n <= 3; n++)
		{
			final MemoryUnit unit = new MemoryUnit ();
			unit.setOwningPlayerID (n);
			unit.setUnitID ("UN00" + new Integer (n+1).toString ());
			units.add (unit);
		}

		// Set up object to test
		final UnitServerUtils utils = new UnitServerUtils ();

		// Run test
		assertEquals (2, utils.findUnitWithPlayerAndID (units, 2, "UN003").getOwningPlayerID ());
		assertNull (utils.findUnitWithPlayerAndID (units, 2, "UN002"));
	}

	/**
	 * Tests the canUnitBeAddedHere method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanUnitBeAddedHere () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		final List<String> emptySkillList = new ArrayList<String> ();

		// Set up object to test
		final UnitServerUtils utils = new UnitServerUtils ();
		final UnitUtils unitUtils = new UnitUtils ();
		utils.setUnitUtils (unitUtils);
		utils.setServerUnitCalculations (new MomServerUnitCalculations ());
		
		// Map
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
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
		unitUtils.initializeUnitSkills (spearmen,  0, true, db);

		final OverlandMapCoordinatesEx addLocation = new OverlandMapCoordinatesEx ();
		addLocation.setX (20);
		addLocation.setY (10);
		addLocation.setPlane (1);

		assertFalse (utils.canUnitBeAddedHere (addLocation, spearmen, emptySkillList, trueMap, sd.getUnitSetting (), db));

		// Nothing here
		terrainData.setMapFeatureID (null);
		assertTrue (utils.canUnitBeAddedHere (addLocation, spearmen, emptySkillList, trueMap, sd.getUnitSetting (), db));

		// Enemy city here
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityPopulation (1);
		cityData.setCityOwnerID (3);
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		assertFalse (utils.canUnitBeAddedHere (addLocation, spearmen, emptySkillList, trueMap, sd.getUnitSetting (), db));

		// Our city here
		cityData.setCityOwnerID (2);
		assertTrue (utils.canUnitBeAddedHere (addLocation, spearmen, emptySkillList, trueMap, sd.getUnitSetting (), db));

		// Enemy unit here
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (null);

		final OverlandMapCoordinatesEx unitLocation = new OverlandMapCoordinatesEx ();
		unitLocation.setX (20);
		unitLocation.setY (10);
		unitLocation.setPlane (1);

		final MemoryUnit unitInTheWay = unitUtils.createMemoryUnit ("UN105", 1, 0, 0, true, db);
		unitInTheWay.setOwningPlayerID (3);
		unitInTheWay.setUnitLocation (unitLocation);

		trueMap.getUnit ().add (unitInTheWay);

		assertFalse (utils.canUnitBeAddedHere (addLocation, spearmen, emptySkillList, trueMap, sd.getUnitSetting (), db));

		// Our units here, but space left
		unitInTheWay.setOwningPlayerID (2);
		assertTrue (utils.canUnitBeAddedHere (addLocation, spearmen, emptySkillList, trueMap, sd.getUnitSetting (), db));

		// Our units here, space left, but cell is impassable to the type of unit we're trying to add
		final AvailableUnit trireme = new AvailableUnit ();
		trireme.setOwningPlayerID (2);
		trireme.setUnitID ("UN016");
		unitUtils.initializeUnitSkills (trireme,  0, true, db);

		assertFalse (utils.canUnitBeAddedHere (addLocation, trireme, emptySkillList, trueMap, sd.getUnitSetting (), db));

		// Our units have the cell full already
		for (int n = 2; n <= sd.getUnitSetting ().getUnitsPerMapCell (); n++)
		{
			final OverlandMapCoordinatesEx anotherUnitLocation = new OverlandMapCoordinatesEx ();
			anotherUnitLocation.setX (20);
			anotherUnitLocation.setY (10);
			anotherUnitLocation.setPlane (1);

			final MemoryUnit anotherUnitInTheWay = unitUtils.createMemoryUnit ("UN105", n, 0, 0, true, db);
			anotherUnitInTheWay.setOwningPlayerID (2);
			anotherUnitInTheWay.setUnitLocation (anotherUnitLocation);

			trueMap.getUnit ().add (anotherUnitInTheWay);

			if (n < sd.getUnitSetting ().getUnitsPerMapCell ())
				assertTrue (utils.canUnitBeAddedHere (addLocation, spearmen, emptySkillList, trueMap, sd.getUnitSetting (), db));
			else
				assertFalse (utils.canUnitBeAddedHere (addLocation, spearmen, emptySkillList, trueMap, sd.getUnitSetting (), db));
		}
	}

	/**
	 * Tests the findNearestLocationWhereUnitCanBeAdded method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFindNearestLocationWhereUnitCanBeAdded () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		// Set up object to test
		final UnitServerUtils utils = new UnitServerUtils ();
		final UnitUtils unitUtils = new UnitUtils ();
		utils.setUnitUtils (unitUtils);
		utils.setServerUnitCalculations (new MomServerUnitCalculations ());
		
		// Map
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
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
			final OverlandMapCoordinatesEx anotherUnitLocation = new OverlandMapCoordinatesEx ();
			anotherUnitLocation.setX (20);
			anotherUnitLocation.setY (10);
			anotherUnitLocation.setPlane (1);

			final MemoryUnit anotherUnitInTheWay = unitUtils.createMemoryUnit ("UN105", n, 0, 0, true, db);
			anotherUnitInTheWay.setOwningPlayerID (2);
			anotherUnitInTheWay.setUnitLocation (anotherUnitLocation);

			trueMap.getUnit ().add (anotherUnitInTheWay);
		}

		// Run test
		final OverlandMapCoordinatesEx desiredLocation = new OverlandMapCoordinatesEx ();
		desiredLocation.setX (20);
		desiredLocation.setY (10);
		desiredLocation.setPlane (1);

		final UnitAddLocation centre = utils.findNearestLocationWhereUnitCanBeAdded (desiredLocation, "UN105", 2, trueMap, sd, db);
		assertEquals (desiredLocation, centre.getUnitLocation ());
		assertEquals (UnitAddBumpTypeID.CITY, centre.getBumpType ());

		// Add a 9th unit in the middle
		final OverlandMapCoordinatesEx anotherUnitLocation = new OverlandMapCoordinatesEx ();
		anotherUnitLocation.setX (20);
		anotherUnitLocation.setY (10);
		anotherUnitLocation.setPlane (1);

		final MemoryUnit anotherUnitInTheWay = unitUtils.createMemoryUnit ("UN105", 9, 0, 0, true, db);
		anotherUnitInTheWay.setOwningPlayerID (2);
		anotherUnitInTheWay.setUnitLocation (anotherUnitLocation);

		trueMap.getUnit ().add (anotherUnitInTheWay);

		// Add an enemy unit above us
		final OverlandMapCoordinatesEx unitLocation = new OverlandMapCoordinatesEx ();
		unitLocation.setX (20);
		unitLocation.setY (9);
		unitLocation.setPlane (1);

		final MemoryUnit unitInTheWay = unitUtils.createMemoryUnit ("UN105", 9, 0, 0, true, db);
		unitInTheWay.setOwningPlayerID (3);
		unitInTheWay.setUnitLocation (unitLocation);

		trueMap.getUnit ().add (unitInTheWay);

		// Add an enemy city above-right of us
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityPopulation (1);
		cityData.setCityOwnerID (3);
		trueTerrain.getPlane ().get (1).getRow ().get (9).getCell ().get (21).setCityData (cityData);

		// So now we should get bumped to the right...
		final UnitAddLocation right = utils.findNearestLocationWhereUnitCanBeAdded (desiredLocation, "UN105", 2, trueMap, sd, db);
		assertEquals (21, right.getUnitLocation ().getX ());
		assertEquals (10, right.getUnitLocation ().getY ());
		assertEquals (1, right.getUnitLocation ().getPlane ());
		assertEquals (UnitAddBumpTypeID.BUMPED, right.getBumpType ());

		// Set every tile in the 3x3 area to be a lair!
		for (int x = 19; x <= 21; x++)
			for (int y = 9; y <= 11; y++)
				trueTerrain.getPlane ().get (1).getRow ().get (y).getCell ().get (x).getTerrainData ().setMapFeatureID ("MF13");

		// Now we won't fit anywhere
		final UnitAddLocation wontFit = utils.findNearestLocationWhereUnitCanBeAdded (desiredLocation, "UN105", 2, trueMap, sd, db);
		assertNull (wontFit.getUnitLocation ());
		assertEquals (UnitAddBumpTypeID.NO_ROOM, wontFit.getBumpType ());
	}
}
