package momime.server.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import momime.common.MomException;
import momime.common.calculations.CityCalculationsImpl;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.FogOfWarSettingData;
import momime.common.database.newgame.FogOfWarValue;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapAreaOfMemoryGridCells;
import momime.common.messages.MapRowOfMemoryGridCells;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.NewTurnMessageNode;
import momime.common.messages.NewTurnMessageTypeID;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitCombatSideID;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.KillUnitActionID;
import momime.common.utils.UnitUtils;
import momime.server.ServerTestData;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_5.CityNameContainer;
import momime.server.database.v0_9_5.Plane;
import momime.server.database.v0_9_5.Race;
import momime.server.database.v0_9_5.TileType;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.messages.v0_9_5.MomGeneralServerKnowledge;
import momime.server.messages.v0_9_5.ServerGridCell;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.SquareMapDirection;
import com.ndg.map.areas.operations.MapAreaOperations3DImpl;
import com.ndg.map.areas.storage.MapArea3D;
import com.ndg.map.areas.storage.MapArea3DArrayListImpl;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.random.RandomUtils;

/**
 * Tests the OverlandMapServerUtilsImpl class
 */
public final class TestOverlandMapServerUtilsImpl
{
	/**
	 * Tests the setContinentalRace method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSetContinentalRace () throws Exception
	{
		// Mock some tile types
		final TileType grass = new TileType ();
		grass.setIsLand (true);
		
		final TileType ocean = new TileType ();
		ocean.setIsLand (false);
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findTileType ("G", "setContinentalRace")).thenReturn (grass);
		when (db.findTileType ("O", "setContinentalRace")).thenReturn (ocean);

		// Map and coordinate system
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = ServerTestData.createOverlandMap (sys);
		final MapAreaOperations3DImpl<String> op = new MapAreaOperations3DImpl<String> ();
		final CoordinateSystemUtilsImpl coordinateSystemUtils = new CoordinateSystemUtilsImpl (); 

		// Fill map with ocean
		for (final MapAreaOfMemoryGridCells plane : map.getPlane ())
			for (final MapRowOfMemoryGridCells row : plane.getRow ())
				for (final MemoryGridCell cell : row.getCell ())
				{
					final OverlandMapTerrainData terrain = new OverlandMapTerrainData ();
					terrain.setTileTypeID ("O");

					cell.setTerrainData (terrain);
				}

		// Mark a city radius around the tile - just to test a non-square area
		final MapCoordinates2DEx coords = new MapCoordinates2DEx (20, 10);
		map.getPlane ().get (1).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ().setTileTypeID ("G");

		for (final SquareMapDirection d : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
			if (coordinateSystemUtils.move2DCoordinates (sys, coords, d.getDirectionID ()))
				map.getPlane ().get (1).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ().setTileTypeID ("G");
		
		// Create area to output into
		final MapArea3D<String> continentalRace = new MapArea3DArrayListImpl<String> ();
		continentalRace.setCoordinateSystem (sys);
		
		// Set up object to test
		final OverlandMapServerUtilsImpl utils = new OverlandMapServerUtilsImpl ();
		utils.setCoordinateSystemUtils (coordinateSystemUtils);
		
		// Call method
		utils.setContinentalRace (map, continentalRace, 20, 10, 1, "RC02", db);
		
		// Check results
		assertEquals (21, op.countCellsEqualTo (continentalRace, "RC02"));
	}

	/**
	 * Tests the decideAllContinentalRaces method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDecideAllContinentalRaces () throws Exception
	{
		// Mock some tile types
		final TileType grass = new TileType ();
		grass.setIsLand (true);
		
		final TileType ocean = new TileType ();
		ocean.setIsLand (false);
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findTileType ("G", "setContinentalRace")).thenReturn (grass);
		when (db.findTileType ("O", "setContinentalRace")).thenReturn (ocean);
		when (db.findTileType ("G", "decideAllContinentalRaces")).thenReturn (grass);
		when (db.findTileType ("O", "decideAllContinentalRaces")).thenReturn (ocean);

		final Plane arcanus = new Plane ();
		final Plane myrror = new Plane ();
		myrror.setPlaneNumber (1);
		
		final List<Plane> planes = new ArrayList<Plane> ();
		planes.add (arcanus);
		planes.add (myrror);

		when (db.getPlane ()).thenReturn (planes);
		
		// Map and coordinate system
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = ServerTestData.createOverlandMap (sys);
		final MapAreaOperations3DImpl<String> op = new MapAreaOperations3DImpl<String> ();
		final CoordinateSystemUtilsImpl coordinateSystemUtils = new CoordinateSystemUtilsImpl (); 

		// Fill map with ocean
		for (final MapAreaOfMemoryGridCells plane : map.getPlane ())
			for (final MapRowOfMemoryGridCells row : plane.getRow ())
				for (final MemoryGridCell cell : row.getCell ())
				{
					final OverlandMapTerrainData terrain = new OverlandMapTerrainData ();
					terrain.setTileTypeID ("O");

					cell.setTerrainData (terrain);
				}

		// Mark two city radiuses
		final MapCoordinates2DEx coords = new MapCoordinates2DEx (20, 10);
		map.getPlane ().get (1).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ().setTileTypeID ("G");

		for (final SquareMapDirection d : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
			if (coordinateSystemUtils.move2DCoordinates (sys, coords, d.getDirectionID ()))
				map.getPlane ().get (1).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ().setTileTypeID ("G");

		coords.setX (15);
		coords.setY (10);
		map.getPlane ().get (0).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ().setTileTypeID ("G");

		for (final SquareMapDirection d : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
			if (coordinateSystemUtils.move2DCoordinates (sys, coords, d.getDirectionID ()))
				map.getPlane ().get (0).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ().setTileTypeID ("G");
		
		// Fix race selections
		final PlayerPickServerUtils pickUtils = mock (PlayerPickServerUtils.class);
		when (pickUtils.chooseRandomRaceForPlane (0, db)).thenReturn ("RC02");
		when (pickUtils.chooseRandomRaceForPlane (1, db)).thenReturn ("RC04");
		
		// Set up object to test
		final OverlandMapServerUtilsImpl utils = new OverlandMapServerUtilsImpl ();
		utils.setCoordinateSystemUtils (coordinateSystemUtils);
		utils.setPlayerPickServerUtils (pickUtils);
		
		// Run method
		final MapArea3D<String> continentalRace = utils.decideAllContinentalRaces (map, sys, db);

		// Check results
		assertEquals (21, op.countCellsEqualTo (continentalRace, "RC02"));
		assertEquals (21, op.countCellsEqualTo (continentalRace, "RC04"));
	}

	/**
	 * Tests the generateCityName method
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If we can't find the requested race
	 */
	@Test
	public final void testGenerateCityName () throws JAXBException, RecordNotFoundException
	{
		// If we have 4 possible names
		final String names [] = new String [] {"Hanna", "Emily", "Mara", "Rose"};

		final Race race = new Race ();
		for (final String thisName : names)
		{
			final CityNameContainer cont = new CityNameContainer ();
			cont.setCityName (thisName);
			race.getCityName ().add (cont);
		}

		// But 2 of them is already used
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.getUsedCityName ().add ("Hanna");
		gsk.getUsedCityName ().add ("Emily");
		
		// Fix random results
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (2)).thenReturn (1);
		when (random.nextInt (1)).thenReturn (0);

		when (random.nextInt (4)).thenReturn (1);
		when (random.nextInt (3)).thenReturn (2);
		
		// Set up object to test
		final OverlandMapServerUtilsImpl utils = new OverlandMapServerUtilsImpl ();
		utils.setRandomUtils (random);
		
		// Go through the first two remaining choices without a suffix
		assertEquals ("Rose", utils.generateCityName (gsk, race));
		assertEquals ("Mara", utils.generateCityName (gsk, race));

		// Now all 4 are used, the next 4 we generate will be suffixed with II
		assertEquals ("Emily II", utils.generateCityName (gsk, race));
		assertEquals ("Rose II", utils.generateCityName (gsk, race));
		assertEquals ("Mara II", utils.generateCityName (gsk, race));
		assertEquals ("Hanna II", utils.generateCityName (gsk, race));

		// And the next 4 we generate will be suffixed with III
		assertEquals ("Emily III", utils.generateCityName (gsk, race));
		assertEquals ("Rose III", utils.generateCityName (gsk, race));
	}
	
	/**
	 * Tests the attemptToMeldWithNode method when there's no defender, and a human player attacking
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAttemptToMeldWithNode_Undefended_HumanAttacking () throws Exception
	{
		// Mock database
		final Plane arcanus = new Plane ();
		final Plane myrror = new Plane ();
		myrror.setPlaneNumber (1);
		
		final List<Plane> planes = new ArrayList<Plane> ();
		planes.add (arcanus);
		planes.add (myrror);

		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.getPlane ()).thenReturn (planes);

		// Session description
		final FogOfWarSettingData settings = new FogOfWarSettingData ();
		settings.setTerrainAndNodeAuras (FogOfWarValue.REMEMBER_AS_LAST_SEEN);
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setMapSize (ServerTestData.createMapSizeData ());
		sd.setFogOfWarSetting (settings);

		// Map
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Node location
		final MapCoordinates3DEx nodeLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Node
		final ServerGridCell nodeCell = (ServerGridCell) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		final OverlandMapTerrainData nodeData = new OverlandMapTerrainData ();
		nodeCell.setTerrainData (nodeData);
		
		// Node aura - its a small node, the centre tile and the one map square to the right of it :)
		nodeCell.setAuraFromNode (nodeLocation);
		final ServerGridCell adjacentCell = (ServerGridCell) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (21);
		final OverlandMapTerrainData adjacentData = new OverlandMapTerrainData ();
		adjacentCell.setTerrainData (adjacentData);
		adjacentCell.setAuraFromNode (nodeLocation);		
		
		// Players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription attackerPd = new PlayerDescription ();
		attackerPd.setPlayerID (2);
		attackerPd.setHuman (true);
		
		final MomTransientPlayerPrivateKnowledge attackerTrans = new MomTransientPlayerPrivateKnowledge ();
		
		final PlayerServerDetails attacker = new PlayerServerDetails (attackerPd, null, null, null, attackerTrans);
		players.add (attacker);
		
		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, attackerPd.getPlayerID (), "attemptToMeldWithNode (a)")).thenReturn (attacker);
		
		// Units
		final MemoryUnit attackingSpirit = new MemoryUnit ();
		attackingSpirit.setOwningPlayerID (attacker.getPlayerDescription ().getPlayerID ());
		attackingSpirit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		attackingSpirit.setUnitID ("GS");
		attackingSpirit.setStatus (UnitStatusID.ALIVE);
		
		// Set up object to test
		final FogOfWarMidTurnChanges fogOfWarMidTurnChanges = mock (FogOfWarMidTurnChanges.class);
		
		final OverlandMapServerUtilsImpl utils = new OverlandMapServerUtilsImpl ();
		utils.setFogOfWarMidTurnChanges (fogOfWarMidTurnChanges);
		utils.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run method
		utils.attemptToMeldWithNode (attackingSpirit, trueMap, players, sd, db);
		
		// Check results
		assertEquals ("GS", nodeCell.getNodeSpiritUnitID ());
		assertEquals (attacker.getPlayerDescription ().getPlayerID (), nodeData.getNodeOwnerID ());
		assertEquals (attacker.getPlayerDescription ().getPlayerID (), adjacentData.getNodeOwnerID ());
		
		assertEquals (1, attackerTrans.getNewTurnMessage ().size ());
		assertEquals (NewTurnMessageTypeID.NODE_CAPTURED, attackerTrans.getNewTurnMessage ().get (0).getMsgType ());
		final NewTurnMessageNode attackerMsg = (NewTurnMessageNode) attackerTrans.getNewTurnMessage ().get (0);
		assertEquals (nodeLocation, attackerMsg.getNodeLocation ());
		assertEquals ("GS", attackerMsg.getUnitID ());
		assertNull (attackerMsg.getOtherUnitID ());
		assertNull (attackerMsg.getOtherPlayerID ());

		verify (fogOfWarMidTurnChanges, times (1)).killUnitOnServerAndClients (attackingSpirit, KillUnitActionID.FREE, null, trueMap, players, sd.getFogOfWarSetting (), db);
		verify (fogOfWarMidTurnChanges, times (1)).updatePlayerMemoryOfTerrain (trueTerrain, players, nodeLocation, FogOfWarValue.REMEMBER_AS_LAST_SEEN);

		final MapCoordinates3DEx adjacentLocation = new MapCoordinates3DEx (21, 10, 1);
		verify (fogOfWarMidTurnChanges, times (1)).updatePlayerMemoryOfTerrain (trueTerrain, players, adjacentLocation, FogOfWarValue.REMEMBER_AS_LAST_SEEN);
	}
	
	/**
	 * Tests the attemptToMeldWithNode method when there's no defender, and an AI player attacking
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAttemptToMeldWithNode_Undefended_AIAttacking () throws Exception
	{
		// Mock database
		final Plane arcanus = new Plane ();
		final Plane myrror = new Plane ();
		myrror.setPlaneNumber (1);
		
		final List<Plane> planes = new ArrayList<Plane> ();
		planes.add (arcanus);
		planes.add (myrror);

		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.getPlane ()).thenReturn (planes);

		// Session description
		final FogOfWarSettingData settings = new FogOfWarSettingData ();
		settings.setTerrainAndNodeAuras (FogOfWarValue.REMEMBER_AS_LAST_SEEN);
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setMapSize (ServerTestData.createMapSizeData ());
		sd.setFogOfWarSetting (settings);

		// Map
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Node location
		final MapCoordinates3DEx nodeLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Node
		final ServerGridCell nodeCell = (ServerGridCell) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		final OverlandMapTerrainData nodeData = new OverlandMapTerrainData ();
		nodeCell.setTerrainData (nodeData);
		
		// Node aura - its a small node, the centre tile and the one map square to the right of it :)
		nodeCell.setAuraFromNode (nodeLocation);
		final ServerGridCell adjacentCell = (ServerGridCell) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (21);
		final OverlandMapTerrainData adjacentData = new OverlandMapTerrainData ();
		adjacentCell.setTerrainData (adjacentData);
		adjacentCell.setAuraFromNode (nodeLocation);		
		
		// Players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription attackerPd = new PlayerDescription ();
		attackerPd.setPlayerID (-1);
		attackerPd.setHuman (false);
		
		final MomTransientPlayerPrivateKnowledge attackerTrans = new MomTransientPlayerPrivateKnowledge ();
		
		final PlayerServerDetails attacker = new PlayerServerDetails (attackerPd, null, null, null, attackerTrans);
		players.add (attacker);

		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, attackerPd.getPlayerID (), "attemptToMeldWithNode (a)")).thenReturn (attacker);
		
		// Units
		final MemoryUnit attackingSpirit = new MemoryUnit ();
		attackingSpirit.setOwningPlayerID (attacker.getPlayerDescription ().getPlayerID ());
		attackingSpirit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		attackingSpirit.setUnitID ("GS");
		attackingSpirit.setStatus (UnitStatusID.ALIVE);
		
		// Set up object to test
		final FogOfWarMidTurnChanges fogOfWarMidTurnChanges = mock (FogOfWarMidTurnChanges.class);
		
		final OverlandMapServerUtilsImpl utils = new OverlandMapServerUtilsImpl ();
		utils.setFogOfWarMidTurnChanges (fogOfWarMidTurnChanges);
		utils.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run method
		utils.attemptToMeldWithNode (attackingSpirit, trueMap, players, sd, db);
		
		// Check results
		assertEquals ("GS", nodeCell.getNodeSpiritUnitID ());
		assertEquals (attacker.getPlayerDescription ().getPlayerID (), nodeData.getNodeOwnerID ());
		assertEquals (attacker.getPlayerDescription ().getPlayerID (), adjacentData.getNodeOwnerID ());
		
		assertEquals (0, attackerTrans.getNewTurnMessage ().size ());

		verify (fogOfWarMidTurnChanges, times (1)).killUnitOnServerAndClients (attackingSpirit, KillUnitActionID.FREE, null, trueMap, players, sd.getFogOfWarSetting (), db);
		verify (fogOfWarMidTurnChanges, times (1)).updatePlayerMemoryOfTerrain (trueTerrain, players, nodeLocation, FogOfWarValue.REMEMBER_AS_LAST_SEEN);

		final MapCoordinates3DEx adjacentLocation = new MapCoordinates3DEx (21, 10, 1);
		verify (fogOfWarMidTurnChanges, times (1)).updatePlayerMemoryOfTerrain (trueTerrain, players, adjacentLocation, FogOfWarValue.REMEMBER_AS_LAST_SEEN);
	}

	/**
	 * Tests the attemptToMeldWithNode method when there's a defender, but a human player captures it from them
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAttemptToMeldWithNode_Successful_HumanAttacking () throws Exception
	{
		// Mock database
		final Plane arcanus = new Plane ();
		final Plane myrror = new Plane ();
		myrror.setPlaneNumber (1);
		
		final List<Plane> planes = new ArrayList<Plane> ();
		planes.add (arcanus);
		planes.add (myrror);

		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.getPlane ()).thenReturn (planes);

		// Session description
		final FogOfWarSettingData settings = new FogOfWarSettingData ();
		settings.setTerrainAndNodeAuras (FogOfWarValue.REMEMBER_AS_LAST_SEEN);
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setMapSize (ServerTestData.createMapSizeData ());
		sd.setFogOfWarSetting (settings);

		// Map
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Node location
		final MapCoordinates3DEx nodeLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Node
		final ServerGridCell nodeCell = (ServerGridCell) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		final OverlandMapTerrainData nodeData = new OverlandMapTerrainData ();
		nodeData.setNodeOwnerID (3);
		nodeCell.setNodeSpiritUnitID ("MS");
		nodeCell.setTerrainData (nodeData);
		
		// Node aura - its a small node, the centre tile and the one map square to the right of it :)
		nodeCell.setAuraFromNode (nodeLocation);
		final ServerGridCell adjacentCell = (ServerGridCell) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (21);
		final OverlandMapTerrainData adjacentData = new OverlandMapTerrainData ();
		adjacentData.setNodeOwnerID (3);
		adjacentCell.setNodeSpiritUnitID ("MS");
		adjacentCell.setTerrainData (adjacentData);
		adjacentCell.setAuraFromNode (nodeLocation);		
		
		// Players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription attackerPd = new PlayerDescription ();
		attackerPd.setPlayerID (2);
		attackerPd.setHuman (true);
		
		final MomTransientPlayerPrivateKnowledge attackerTrans = new MomTransientPlayerPrivateKnowledge ();
		
		final PlayerServerDetails attacker = new PlayerServerDetails (attackerPd, null, null, null, attackerTrans);
		players.add (attacker);

		final PlayerDescription defenderPd = new PlayerDescription ();
		defenderPd.setPlayerID (3);
		defenderPd.setHuman (true);
		
		final MomTransientPlayerPrivateKnowledge defenderTrans = new MomTransientPlayerPrivateKnowledge ();
		
		final PlayerServerDetails defender = new PlayerServerDetails (defenderPd, null, null, null, defenderTrans);
		players.add (defender);
		
		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, attackerPd.getPlayerID (), "attemptToMeldWithNode (a)")).thenReturn (attacker);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, defenderPd.getPlayerID (), "attemptToMeldWithNode (d)")).thenReturn (defender);
		
		// Units
		final MemoryUnit attackingSpirit = new MemoryUnit ();
		attackingSpirit.setOwningPlayerID (attacker.getPlayerDescription ().getPlayerID ());
		attackingSpirit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		attackingSpirit.setUnitID ("GS");
		attackingSpirit.setStatus (UnitStatusID.ALIVE);
		
		// Unit stats
		// Can get away with matching attackingSpirit.getUnitHasSkill () for the defender also, because they're both just empty lists
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.getModifiedSkillValue (any (AvailableUnit.class), eq (attackingSpirit.getUnitHasSkill ()), eq (CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_MELD_WITH_NODE),
			eq (players), eq (trueMap.getMaintainedSpell ()), eq (trueMap.getCombatAreaEffect ()), eq (db))).thenReturn (2, 1);
		
		// Fix random result
		final RandomUtils randomUtils = mock (RandomUtils.class);
		when (randomUtils.nextInt (3)).thenReturn (1);		// 0-1 = Attacker wins, 2 = Defender wins
		
		// Set up object to test
		final FogOfWarMidTurnChanges fogOfWarMidTurnChanges = mock (FogOfWarMidTurnChanges.class);
		
		final OverlandMapServerUtilsImpl utils = new OverlandMapServerUtilsImpl ();
		utils.setFogOfWarMidTurnChanges (fogOfWarMidTurnChanges);
		utils.setUnitUtils (unitUtils);
		utils.setRandomUtils (randomUtils);
		utils.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run method
		utils.attemptToMeldWithNode (attackingSpirit, trueMap, players, sd, db);
		
		// Check results
		assertEquals ("GS", nodeCell.getNodeSpiritUnitID ());
		assertEquals (attacker.getPlayerDescription ().getPlayerID (), nodeData.getNodeOwnerID ());
		assertEquals (attacker.getPlayerDescription ().getPlayerID (), adjacentData.getNodeOwnerID ());
		
		assertEquals (1, attackerTrans.getNewTurnMessage ().size ());
		assertEquals (NewTurnMessageTypeID.NODE_CAPTURED, attackerTrans.getNewTurnMessage ().get (0).getMsgType ());
		final NewTurnMessageNode attackerMsg = (NewTurnMessageNode) attackerTrans.getNewTurnMessage ().get (0);
		assertEquals (nodeLocation, attackerMsg.getNodeLocation ());
		assertEquals ("GS", attackerMsg.getUnitID ());
		assertEquals ("MS", attackerMsg.getOtherUnitID ());
		assertEquals (3, attackerMsg.getOtherPlayerID ().intValue ());

		assertEquals (1, defenderTrans.getNewTurnMessage ().size ());
		assertEquals (NewTurnMessageTypeID.NODE_LOST, defenderTrans.getNewTurnMessage ().get (0).getMsgType ());
		final NewTurnMessageNode defenderMsg = (NewTurnMessageNode) defenderTrans.getNewTurnMessage ().get (0);
		assertEquals (nodeLocation, defenderMsg.getNodeLocation ());
		assertEquals ("MS", defenderMsg.getUnitID ());
		assertEquals ("GS", defenderMsg.getOtherUnitID ());
		assertEquals (2, defenderMsg.getOtherPlayerID ().intValue ());
		
		verify (fogOfWarMidTurnChanges, times (1)).killUnitOnServerAndClients (attackingSpirit, KillUnitActionID.FREE, null, trueMap, players, sd.getFogOfWarSetting (), db);
		verify (fogOfWarMidTurnChanges, times (1)).updatePlayerMemoryOfTerrain (trueTerrain, players, nodeLocation, FogOfWarValue.REMEMBER_AS_LAST_SEEN);

		final MapCoordinates3DEx adjacentLocation = new MapCoordinates3DEx (21, 10, 1);
		verify (fogOfWarMidTurnChanges, times (1)).updatePlayerMemoryOfTerrain (trueTerrain, players, adjacentLocation, FogOfWarValue.REMEMBER_AS_LAST_SEEN);
	}
	
	/**
	 * Tests the attemptToMeldWithNode method when there's a defender, and a human player fails to capture it from them
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAttemptToMeldWithNode_Failed_HumanAttacking () throws Exception
	{
		// Mock database
		final Plane arcanus = new Plane ();
		final Plane myrror = new Plane ();
		myrror.setPlaneNumber (1);
		
		final List<Plane> planes = new ArrayList<Plane> ();
		planes.add (arcanus);
		planes.add (myrror);

		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.getPlane ()).thenReturn (planes);

		// Session description
		final FogOfWarSettingData settings = new FogOfWarSettingData ();
		settings.setTerrainAndNodeAuras (FogOfWarValue.REMEMBER_AS_LAST_SEEN);
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setMapSize (ServerTestData.createMapSizeData ());
		sd.setFogOfWarSetting (settings);

		// Map
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Node location
		final MapCoordinates3DEx nodeLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Node
		final ServerGridCell nodeCell = (ServerGridCell) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		final OverlandMapTerrainData nodeData = new OverlandMapTerrainData ();
		nodeData.setNodeOwnerID (3);
		nodeCell.setNodeSpiritUnitID ("MS");
		nodeCell.setTerrainData (nodeData);
		
		// Node aura - its a small node, the centre tile and the one map square to the right of it :)
		nodeCell.setAuraFromNode (nodeLocation);
		final ServerGridCell adjacentCell = (ServerGridCell) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (21);
		final OverlandMapTerrainData adjacentData = new OverlandMapTerrainData ();
		adjacentData.setNodeOwnerID (3);
		adjacentCell.setNodeSpiritUnitID ("MS");
		adjacentCell.setTerrainData (adjacentData);
		adjacentCell.setAuraFromNode (nodeLocation);		
		
		// Players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription attackerPd = new PlayerDescription ();
		attackerPd.setPlayerID (2);
		attackerPd.setHuman (true);
		
		final MomTransientPlayerPrivateKnowledge attackerTrans = new MomTransientPlayerPrivateKnowledge ();
		
		final PlayerServerDetails attacker = new PlayerServerDetails (attackerPd, null, null, null, attackerTrans);
		players.add (attacker);

		final PlayerDescription defenderPd = new PlayerDescription ();
		defenderPd.setPlayerID (3);
		defenderPd.setHuman (true);
		
		final MomTransientPlayerPrivateKnowledge defenderTrans = new MomTransientPlayerPrivateKnowledge ();
		
		final PlayerServerDetails defender = new PlayerServerDetails (defenderPd, null, null, null, defenderTrans);
		players.add (defender);
		
		// Units
		final MemoryUnit attackingSpirit = new MemoryUnit ();
		attackingSpirit.setOwningPlayerID (attacker.getPlayerDescription ().getPlayerID ());
		attackingSpirit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		attackingSpirit.setUnitID ("GS");
		attackingSpirit.setStatus (UnitStatusID.ALIVE);
		
		// Unit stats
		// Can get away with matching attackingSpirit.getUnitHasSkill () for the defender also, because they're both just empty lists
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.getModifiedSkillValue (any (AvailableUnit.class), eq (attackingSpirit.getUnitHasSkill ()), eq (CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_MELD_WITH_NODE),
			eq (players), eq (trueMap.getMaintainedSpell ()), eq (trueMap.getCombatAreaEffect ()), eq (db))).thenReturn (2, 1);
		
		// Fix random result
		final RandomUtils randomUtils = mock (RandomUtils.class);
		when (randomUtils.nextInt (3)).thenReturn (2);		// 0-1 = Attacker wins, 2 = Defender wins
		
		// Set up object to test
		final FogOfWarMidTurnChanges fogOfWarMidTurnChanges = mock (FogOfWarMidTurnChanges.class);
		
		final OverlandMapServerUtilsImpl utils = new OverlandMapServerUtilsImpl ();
		utils.setFogOfWarMidTurnChanges (fogOfWarMidTurnChanges);
		utils.setUnitUtils (unitUtils);
		utils.setRandomUtils (randomUtils);
		
		// Run method
		utils.attemptToMeldWithNode (attackingSpirit, trueMap, players, sd, db);
		
		// Check results
		assertEquals ("MS", nodeCell.getNodeSpiritUnitID ());
		assertEquals (defender.getPlayerDescription ().getPlayerID (), nodeData.getNodeOwnerID ());
		assertEquals (defender.getPlayerDescription ().getPlayerID (), adjacentData.getNodeOwnerID ());
		
		assertEquals (0, attackerTrans.getNewTurnMessage ().size ());
		assertEquals (0, defenderTrans.getNewTurnMessage ().size ());
		
		verify (fogOfWarMidTurnChanges, times (1)).killUnitOnServerAndClients (attackingSpirit, KillUnitActionID.FREE, null, trueMap, players, sd.getFogOfWarSetting (), db);
		verify (fogOfWarMidTurnChanges, times (0)).updatePlayerMemoryOfTerrain (trueTerrain, players, nodeLocation, FogOfWarValue.REMEMBER_AS_LAST_SEEN);

		final MapCoordinates3DEx adjacentLocation = new MapCoordinates3DEx (21, 10, 1);
		verify (fogOfWarMidTurnChanges, times (0)).updatePlayerMemoryOfTerrain (trueTerrain, players, adjacentLocation, FogOfWarValue.REMEMBER_AS_LAST_SEEN);
	}
	
	/**
	 * Tests the totalPlayerPopulation method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testTotalPlayerPopulation () throws Exception
	{
		// Mock planes
		final Plane arcanus = new Plane ();
		final Plane myrror = new Plane ();
		myrror.setPlaneNumber (1);
		
		final List<Plane> planes = new ArrayList<Plane> ();
		planes.add (arcanus);
		planes.add (myrror);

		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.getPlane ()).thenReturn (planes);
		
		// Map
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = ServerTestData.createOverlandMap (sys);
		
		final OverlandMapCityData ourCity1 = new OverlandMapCityData ();
		ourCity1.setCityOwnerID (2);
		ourCity1.setCityPopulation (2000);
		map.getPlane ().get (0).getRow ().get (10).getCell ().get (20).setCityData (ourCity1);

		final OverlandMapCityData ourCity2 = new OverlandMapCityData ();
		ourCity2.setCityOwnerID (2);
		ourCity2.setCityPopulation (3000);
		map.getPlane ().get (1).getRow ().get (15).getCell ().get (20).setCityData (ourCity2);

		final OverlandMapCityData ourCity3 = new OverlandMapCityData ();
		ourCity3.setCityOwnerID (2);
		map.getPlane ().get (1).getRow ().get (15).getCell ().get (25).setCityData (ourCity3);

		final OverlandMapCityData theirCity = new OverlandMapCityData ();
		theirCity.setCityOwnerID (3);
		theirCity.setCityPopulation (4000);
		map.getPlane ().get (0).getRow ().get (5).getCell ().get (20).setCityData (theirCity);

		final OverlandMapCityData nobodysCity = new OverlandMapCityData ();
		nobodysCity.setCityPopulation (5000);
		map.getPlane ().get (0).getRow ().get (5).getCell ().get (25).setCityData (nobodysCity);
		
		// Run method
		assertEquals (5000, new OverlandMapServerUtilsImpl ().totalPlayerPopulation (map, 2, sys, db));
	}
	
	/**
	 * Tests the findMapLocationOfUnitsInCombat method
	 * @throws MomException If the requested side is wiped out
	 */
	@Test
	public final void testFindMapLocationOfUnitsInCombat_Found () throws MomException
	{
		// Locations
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		final MapCoordinates3DEx defenderLocation = new MapCoordinates3DEx (20, 10, 1);
		final MapCoordinates3DEx attackerLocation = new MapCoordinates3DEx (21, 10, 1);
		final MapCoordinates3DEx otherLocation = new MapCoordinates3DEx (22, 10, 1);
		
		// Units
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();

		// Not in combat
		final MemoryUnit unit1 = new MemoryUnit ();
		unit1.setUnitLocation (otherLocation);
		unit1.setStatus (UnitStatusID.ALIVE);
		unit1.setCombatPosition (new MapCoordinates2DEx (0, 0));
		unit1.setCombatSide (UnitCombatSideID.ATTACKER);
		units.add (unit1);

		// Dead
		final MemoryUnit unit2 = new MemoryUnit ();
		unit2.setCombatLocation (combatLocation);
		unit2.setUnitLocation (otherLocation);
		unit2.setStatus (UnitStatusID.DEAD);
		unit2.setCombatPosition (new MapCoordinates2DEx (0, 0));
		unit2.setCombatSide (UnitCombatSideID.ATTACKER);
		units.add (unit2);
		
		// No combat position
		final MemoryUnit unit3 = new MemoryUnit ();
		unit3.setCombatLocation (combatLocation);
		unit3.setUnitLocation (otherLocation);
		unit3.setStatus (UnitStatusID.ALIVE);
		unit3.setCombatSide (UnitCombatSideID.ATTACKER);
		units.add (unit3);

		// Wrong combat
		final MemoryUnit unit4 = new MemoryUnit ();
		unit4.setCombatLocation (otherLocation);
		unit4.setUnitLocation (otherLocation);
		unit4.setStatus (UnitStatusID.ALIVE);
		unit4.setCombatPosition (new MapCoordinates2DEx (0, 0));
		unit4.setCombatSide (UnitCombatSideID.ATTACKER);
		units.add (unit4);

		// No side
		final MemoryUnit unit5 = new MemoryUnit ();
		unit5.setCombatLocation (combatLocation);
		unit5.setUnitLocation (otherLocation);
		unit5.setStatus (UnitStatusID.ALIVE);
		unit5.setCombatPosition (new MapCoordinates2DEx (0, 0));
		units.add (unit5);
		
		// Matches
		final MemoryUnit attackingUnit = new MemoryUnit ();
		attackingUnit.setCombatLocation (combatLocation);
		attackingUnit.setUnitLocation (attackerLocation);
		attackingUnit.setStatus (UnitStatusID.ALIVE);
		attackingUnit.setCombatPosition (new MapCoordinates2DEx (0, 0));
		attackingUnit.setCombatSide (UnitCombatSideID.ATTACKER);
		units.add (attackingUnit);

		final MemoryUnit defendingUnit = new MemoryUnit ();
		defendingUnit.setCombatLocation (combatLocation);
		defendingUnit.setUnitLocation (defenderLocation);
		defendingUnit.setStatus (UnitStatusID.ALIVE);
		defendingUnit.setCombatPosition (new MapCoordinates2DEx (0, 0));
		defendingUnit.setCombatSide (UnitCombatSideID.DEFENDER);
		units.add (defendingUnit);
		
		// Set up object to test
		final OverlandMapServerUtilsImpl utils = new OverlandMapServerUtilsImpl ();

		// Run method, recreate location so = would find no match
		final MapCoordinates3DEx searchLocation = new MapCoordinates3DEx (20, 10, 1);
		
		assertSame (attackerLocation, utils.findMapLocationOfUnitsInCombat (searchLocation, UnitCombatSideID.ATTACKER, units));
		assertSame (defenderLocation, utils.findMapLocationOfUnitsInCombat (searchLocation, UnitCombatSideID.DEFENDER, units));
	}

	/**
	 * Tests the findMapLocationOfUnitsInCombat method - this is a copy of the test above, except that the attacker unit (match) is removed
	 * @throws MomException If the requested side is wiped out
	 */
	@Test(expected=MomException.class)
	public final void testFindMapLocationOfUnitsInCombat_WipedOut () throws MomException
	{
		// Locations
		final MapCoordinates3DEx combatLocation =new MapCoordinates3DEx (20, 10, 1);
		final MapCoordinates3DEx defenderLocation = new MapCoordinates3DEx (20, 10, 1);
		final MapCoordinates3DEx otherLocation = new MapCoordinates3DEx (22, 10, 1);
		
		// Units
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();

		// Not in combat
		final MemoryUnit unit1 = new MemoryUnit ();
		unit1.setUnitLocation (otherLocation);
		unit1.setStatus (UnitStatusID.ALIVE);
		unit1.setCombatPosition (new MapCoordinates2DEx (0, 0));
		unit1.setCombatSide (UnitCombatSideID.ATTACKER);
		units.add (unit1);

		// Dead
		final MemoryUnit unit2 = new MemoryUnit ();
		unit2.setCombatLocation (combatLocation);
		unit2.setUnitLocation (otherLocation);
		unit2.setStatus (UnitStatusID.DEAD);
		unit2.setCombatPosition (new MapCoordinates2DEx (0, 0));
		unit2.setCombatSide (UnitCombatSideID.ATTACKER);
		units.add (unit2);
		
		// No combat position
		final MemoryUnit unit3 = new MemoryUnit ();
		unit3.setCombatLocation (combatLocation);
		unit3.setUnitLocation (otherLocation);
		unit3.setStatus (UnitStatusID.ALIVE);
		unit3.setCombatSide (UnitCombatSideID.ATTACKER);
		units.add (unit3);

		// Wrong combat
		final MemoryUnit unit4 = new MemoryUnit ();
		unit4.setCombatLocation (otherLocation);
		unit4.setUnitLocation (otherLocation);
		unit4.setStatus (UnitStatusID.ALIVE);
		unit4.setCombatPosition (new MapCoordinates2DEx (0, 0));
		unit4.setCombatSide (UnitCombatSideID.ATTACKER);
		units.add (unit4);

		// No side
		final MemoryUnit unit5 = new MemoryUnit ();
		unit5.setCombatLocation (combatLocation);
		unit5.setUnitLocation (otherLocation);
		unit5.setStatus (UnitStatusID.ALIVE);
		unit5.setCombatPosition (new MapCoordinates2DEx (0, 0));
		units.add (unit5);
		
		// Matches
		final MemoryUnit defendingUnit = new MemoryUnit ();
		defendingUnit.setCombatLocation (combatLocation);
		defendingUnit.setUnitLocation (defenderLocation);
		defendingUnit.setStatus (UnitStatusID.ALIVE);
		defendingUnit.setCombatPosition (new MapCoordinates2DEx (0, 0));
		defendingUnit.setCombatSide (UnitCombatSideID.DEFENDER);
		units.add (defendingUnit);
		
		// Set up object to test
		final OverlandMapServerUtilsImpl utils = new OverlandMapServerUtilsImpl ();

		// Run method, recreate location so = would find no match
		final MapCoordinates3DEx searchLocation = new MapCoordinates3DEx (20, 10, 1);
		utils.findMapLocationOfUnitsInCombat (searchLocation, UnitCombatSideID.ATTACKER, units);
	}
}