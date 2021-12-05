package momime.server.fogofwar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

import momime.common.database.CommonDatabase;
import momime.common.database.FogOfWarSetting;
import momime.common.database.FogOfWarValue;
import momime.common.database.OverlandMapSize;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.FogOfWarStateID;
import momime.common.messages.MapVolumeOfFogOfWarStates;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.servertoclient.AddBuildingMessage;
import momime.common.messages.servertoclient.AddOrUpdateMaintainedSpellMessage;
import momime.common.messages.servertoclient.DestroyBuildingMessage;
import momime.common.messages.servertoclient.UpdateCityMessage;
import momime.common.messages.servertoclient.UpdateTerrainMessage;
import momime.common.movement.OverlandMovementCell;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryBuildingUtilsImpl;
import momime.server.DummyServerToClientConnection;
import momime.server.ServerTestData;
import momime.server.calculations.FogOfWarCalculations;
import momime.server.messages.MomGeneralServerKnowledge;

/**
 * Tests the FogOfWarMidTurnChangesImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestFogOfWarMidTurnChangesImpl extends ServerTestData
{
	/**
	 * Tests the updatePlayerMemoryOfTerrain method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUpdatePlayerMemoryOfTerrain () throws Exception
	{
		// Session description
		final FogOfWarValue fowSetting = FogOfWarValue.REMEMBER_AS_LAST_SEEN;
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();

		// FOW visibility rules
		final FogOfWarCalculations single = mock (FogOfWarCalculations.class);
		when (single.canSeeMidTurn (FogOfWarStateID.NEVER_SEEN, fowSetting)).thenReturn (false);
		when (single.canSeeMidTurn (FogOfWarStateID.CAN_SEE, fowSetting)).thenReturn (true);
		
		// True terrain
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		final MemoryGridCell tc = trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		
		final OverlandMapTerrainData td = new OverlandMapTerrainData ();
		tc.setTerrainData (td);
		
		// Players can see the location or not, have up to date info already or not, and be human/AI, so create 8 players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		final FogOfWarDuplication dup = mock (FogOfWarDuplication.class);
		
		int playerID = 0;
		for (final boolean canSee : new boolean [] {false, true})
			for (final boolean upToDateInfo : new boolean [] {false, true})
				for (final boolean human : new boolean [] {false, true})
				{
					playerID++;
					
					final PlayerDescription pd = new PlayerDescription ();
					pd.setPlayerID (playerID);
					pd.setHuman (human);
					
					// Mock whether the player can see the location
					final MapVolumeOfFogOfWarStates vis = createFogOfWarArea (sys);
					vis.getPlane ().get (1).getRow ().get (10).getCell ().set (20, canSee ? FogOfWarStateID.CAN_SEE : FogOfWarStateID.NEVER_SEEN);
					
					final FogOfWarMemory fow = new FogOfWarMemory ();
					fow.setMap (createOverlandMap (sys));
					
					final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
					priv.setFogOfWar (vis);
					priv.setFogOfWarMemory (fow);
					
					final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
					if (human)
						player.setConnection (new DummyServerToClientConnection ());
					
					players.add (player);
					
					// Mock whether the player has up to date info for this terrain already or not
					if (canSee)
					{
						final MemoryGridCell mc = priv.getFogOfWarMemory ().getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20);
						when (dup.copyTerrainAndNodeAura (tc, mc)).thenReturn (!upToDateInfo);
					}
				}

		// Set up object to test
		final FogOfWarMidTurnChangesImpl calc = new FogOfWarMidTurnChangesImpl ();
		calc.setFogOfWarCalculations (single);
		calc.setFogOfWarDuplication (dup);

		// Run test
		calc.updatePlayerMemoryOfTerrain (trueTerrain, players, new MapCoordinates3DEx (20, 10, 1), fowSetting);
		
		// Players 1-4 can't even see the location so shouldn't get the dup method called; players 5-8 should
		for (int playerIndex = 0; playerIndex < 8; playerIndex++)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) players.get (playerIndex).getPersistentPlayerPrivateKnowledge ();
			final MemoryGridCell mc = priv.getFogOfWarMemory ().getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20);
			verify (dup, times (playerIndex < 4 ? 0 : 1)).copyTerrainAndNodeAura (tc, mc);
		}
		
		// Only player 6 (can see location, out of date info, human) should get a message
		for (int playerIndex = 0; playerIndex < 8; playerIndex++)
		{
			final PlayerServerDetails player = players.get (playerIndex);
			if (player.getPlayerDescription ().isHuman ())
			{
				final DummyServerToClientConnection conn = (DummyServerToClientConnection) player.getConnection ();
				if (playerIndex != 5)
					assertEquals (0, conn.getMessages ().size ());
				else
				{
					assertEquals (1, conn.getMessages ().size ());
					
					final UpdateTerrainMessage updateMsg = (UpdateTerrainMessage) conn.getMessages ().get (0);
					assertEquals (20, updateMsg.getData ().getMapLocation ().getX ());
					assertEquals (10, updateMsg.getData ().getMapLocation ().getY ());
					assertEquals (1, updateMsg.getData ().getMapLocation ().getZ ());
					assertSame (td, updateMsg.getData ().getTerrainData ());
				}
			}
		}
		
		verifyNoMoreInteractions (dup);
	}
	
	/**
	 * Tests the updatePlayerMemoryOfCity method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUpdatePlayerMemoryOfCity () throws Exception
	{
		// Session description
		final FogOfWarValue fowSetting = FogOfWarValue.REMEMBER_AS_LAST_SEEN;
		
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		fowSettings.setCitiesSpellsAndCombatAreaEffects (fowSetting);
		
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();

		// FOW visibility rules
		final FogOfWarCalculations single = mock (FogOfWarCalculations.class);
		when (single.canSeeMidTurn (FogOfWarStateID.NEVER_SEEN, fowSetting)).thenReturn (false);
		when (single.canSeeMidTurn (FogOfWarStateID.CAN_SEE, fowSetting)).thenReturn (true);
		
		// True terrain
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		final MemoryGridCell tc = trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		
		// Players can see the location or not, have up to date info already or not, and be human/AI, so create 8 players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		final FogOfWarDuplication dup = mock (FogOfWarDuplication.class);
		
		int playerID = 0;
		for (final boolean canSee : new boolean [] {false, true})
			for (final boolean upToDateInfo : new boolean [] {false, true})
				for (final boolean human : new boolean [] {false, true})
				{
					playerID++;
					
					final PlayerDescription pd = new PlayerDescription ();
					pd.setPlayerID (playerID);
					pd.setHuman (human);
					
					// Mock whether the player can see the location
					final MapVolumeOfFogOfWarStates vis = createFogOfWarArea (sys);
					vis.getPlane ().get (1).getRow ().get (10).getCell ().set (20, canSee ? FogOfWarStateID.CAN_SEE : FogOfWarStateID.NEVER_SEEN);

					final MapVolumeOfMemoryGridCells terrain = createOverlandMap (sys);
					final OverlandMapCityData playerCityData = new OverlandMapCityData ();
					terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (playerCityData);
					
					final FogOfWarMemory fow = new FogOfWarMemory ();
					fow.setMap (terrain);
					
					final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
					priv.setFogOfWar (vis);
					priv.setFogOfWarMemory (fow);
					
					final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
					if (human)
						player.setConnection (new DummyServerToClientConnection ());
					
					players.add (player);
					
					// Mock whether the player has up to date info for this terrain already or not
					if (canSee)
					{
						final MemoryGridCell mc = priv.getFogOfWarMemory ().getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20);
						when (dup.copyCityData (tc, mc, fowSettings.isSeeEnemyCityConstruction (), false)).thenReturn (!upToDateInfo);
					}
				}
		
		// Set up object to test
		final FogOfWarMidTurnChangesImpl calc = new FogOfWarMidTurnChangesImpl ();
		calc.setFogOfWarCalculations (single);
		calc.setFogOfWarDuplication (dup);

		// Run test
		calc.updatePlayerMemoryOfCity (trueTerrain, players, new MapCoordinates3DEx (20, 10, 1), fowSettings);
		
		// Players 1-4 can't even see the location so shouldn't get the dup method called; players 5-8 should
		for (int playerIndex = 0; playerIndex < 8; playerIndex++)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) players.get (playerIndex).getPersistentPlayerPrivateKnowledge ();
			final MemoryGridCell mc = priv.getFogOfWarMemory ().getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20);
			verify (dup, times (playerIndex < 4 ? 0 : 1)).copyCityData (tc, mc, fowSettings.isSeeEnemyCityConstruction (), false);
		}
		
		// Only player 6 (can see location, out of date info, human) should get a message
		for (int playerIndex = 0; playerIndex < 8; playerIndex++)
		{
			final PlayerServerDetails player = players.get (playerIndex);
			if (player.getPlayerDescription ().isHuman ())
			{
				final DummyServerToClientConnection conn = (DummyServerToClientConnection) player.getConnection ();
				if (playerIndex != 5)
					assertEquals (0, conn.getMessages ().size ());
				else
				{
					assertEquals (1, conn.getMessages ().size ());
					
					final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) players.get (playerIndex).getPersistentPlayerPrivateKnowledge ();
					final MemoryGridCell mc = priv.getFogOfWarMemory ().getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20);
					
					final UpdateCityMessage updateMsg = (UpdateCityMessage) conn.getMessages ().get (0);
					assertEquals (20, updateMsg.getData ().getMapLocation ().getX ());
					assertEquals (10, updateMsg.getData ().getMapLocation ().getY ());
					assertEquals (1, updateMsg.getData ().getMapLocation ().getZ ());
					assertSame (mc.getCityData (), updateMsg.getData ().getCityData ());
				}
			}
		}
		
		verifyNoMoreInteractions (dup);
	}

	/*
	 * NB. There is an additional call to killUnitOnServerAndClients from purgeDeadUnitsAndCombatSummonsFromCombat but it doesn't really
	 * warrant a unit test.  It gets called with FREE, but bear in mind the unit is already marked as DEAD for the players involved in the combat
	 * and hence canSeeUnitMidTurn (which is what the behaviour of killUnitOnServerAndClients hinges on) will always return false and hence
	 * never touch players' memory - that is the entire reason for the custom killing code in purgeDeadUnitsAndCombatSummonsFromCombat
	 * being done outside of killUnitOnServerAndClients to begin with.
	 * 
	 * So in that case, all that happens on the actual call to killUnitOnServerAndClients is that the unit is permanently removed from the
	 * server's true unit list, and nothing else, since no player can "see" the dead unit.
	 */
	
	/**
	 * Tests the addExistingTrueMaintainedSpellToClients method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddExistingTrueMaintainedSpellToClients () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Session description
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setFogOfWarSetting (fowSettings);
		
		// Server knowledge
		final MapVolumeOfMemoryGridCells trueTerrain = new MapVolumeOfMemoryGridCells (); 
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);

		// The spell being added
		final MemoryMaintainedSpell trueSpell = new MemoryMaintainedSpell ();
		trueSpell.setCastingPlayerID (8);
		
		// Players can see the spell or not, and be human/AI, so create 8 players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		final FogOfWarDuplication dup = mock (FogOfWarDuplication.class);
		final FogOfWarMidTurnVisibility vis = mock (FogOfWarMidTurnVisibility.class);
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		
		int playerID = 0;
		for (final boolean canSee : new boolean [] {false, true})
			for (final boolean upToDateInfo : new boolean [] {false, true})
				for (final boolean human : new boolean [] {false, true})
				{
					playerID++;
					
					final PlayerDescription pd = new PlayerDescription ();
					pd.setPlayerID (playerID);
					pd.setHuman (human);
					
					// Need to make the spell lists unique for verify to work correctly
					final FogOfWarMemory fow = new FogOfWarMemory ();
					fow.getMaintainedSpell ().add (new MemoryMaintainedSpell ());
					
					final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
					priv.setFogOfWarMemory (fow);
					
					final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
					if (human)
						player.setConnection (new DummyServerToClientConnection ());
					
					players.add (player);

					// Mock finding the player
					if ((canSee) && (human) && (upToDateInfo))
						when (multiplayerSessionServerUtils.findPlayerWithID (players, playerID, "addExistingTrueMaintainedSpellToClients")).thenReturn (player);
					
					// Mock whether the player can see the spell
					when (vis.canSeeSpellMidTurn (trueSpell, trueTerrain, trueMap.getUnit (), player, db, fowSettings)).thenReturn (canSee);
					
					// Mock whether the player has up to date info for this spell already or not
					if (canSee)
						when (dup.copyMaintainedSpell (trueSpell, fow.getMaintainedSpell ())).thenReturn (!upToDateInfo);
				}
		
		// Set up object to test
		final FogOfWarProcessing proc = mock (FogOfWarProcessing.class);
		
		final FogOfWarMidTurnChangesImpl calc = new FogOfWarMidTurnChangesImpl ();
		calc.setFogOfWarDuplication (dup);
		calc.setFogOfWarMidTurnVisibility (vis);
		calc.setFogOfWarProcessing (proc);
		calc.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run method
		calc.addExistingTrueMaintainedSpellToClients (gsk, trueSpell, false, players, db, sd);
		
		// Players 1-4 can't even see the spell so shouldn't get the dup method called; players 5-8 should
		for (int playerIndex = 0; playerIndex < 8; playerIndex++)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) players.get (playerIndex).getPersistentPlayerPrivateKnowledge ();
			verify (dup, times (playerIndex < 4 ? 0 : 1)).copyMaintainedSpell (trueSpell, priv.getFogOfWarMemory ().getMaintainedSpell ());
		}

		// Only player 6 (can see spell, out of date info, human) should get a message
		for (int playerIndex = 0; playerIndex < 8; playerIndex++)
		{
			final PlayerServerDetails player = players.get (playerIndex);
			if (player.getPlayerDescription ().isHuman ())
			{
				final DummyServerToClientConnection conn = (DummyServerToClientConnection) player.getConnection ();
				if (playerIndex != 5)
					assertEquals (0, conn.getMessages ().size ());
				else
				{
					assertEquals (1, conn.getMessages ().size ());
					
					final AddOrUpdateMaintainedSpellMessage addMsg = (AddOrUpdateMaintainedSpellMessage) conn.getMessages ().get (0);
					assertSame (trueSpell, addMsg.getMaintainedSpell ());
				}
			}
		}
		
		// Only the casters FOW should be updated
		for (int playerIndex = 0; playerIndex < 8; playerIndex++)
			verify (proc, times (playerIndex == (trueSpell.getCastingPlayerID () - 1) ? 1 : 0)).updateAndSendFogOfWar
				(trueMap, players.get (playerIndex), players, "addExistingTrueMaintainedSpellToClients", sd, db);

		verifyNoMoreInteractions (dup);
		verifyNoMoreInteractions (proc);
	}
	
	/**
	 * Tests the addBuildingOnServerAndClients method, when its from the normal completion of constructing a building
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddBuildingOnServerAndClients_OneBuilding () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Session description
		final FogOfWarValue fowSetting = FogOfWarValue.REMEMBER_AS_LAST_SEEN;
		
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		fowSettings.setCitiesSpellsAndCombatAreaEffects (fowSetting);

		final OverlandMapSize sys = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (sys);
		sd.setFogOfWarSetting (fowSettings);

		// FOW visibility rules
		final FogOfWarCalculations single = mock (FogOfWarCalculations.class);
		when (single.canSeeMidTurn (FogOfWarStateID.NEVER_SEEN, fowSetting)).thenReturn (false);
		when (single.canSeeMidTurn (FogOfWarStateID.CAN_SEE, fowSetting)).thenReturn (true);
		
		// Server knowledge
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		gsk.setNextFreeBuildingURN (1);
		
		// Players can see the city or not, and be human/AI, so create 4 players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);

		int playerID = 0;
		for (final boolean canSee : new boolean [] {false, true})
			for (final boolean human : new boolean [] {false, true})
			{
				playerID++;
				
				final PlayerDescription pd = new PlayerDescription ();
				pd.setPlayerID (playerID);
				pd.setHuman (human);
				
				// Mock whether the player can see the city
				final MapVolumeOfFogOfWarStates vis = createFogOfWarArea (sys);
				vis.getPlane ().get (1).getRow ().get (10).getCell ().set (20, canSee ? FogOfWarStateID.CAN_SEE : FogOfWarStateID.NEVER_SEEN);
				
				final FogOfWarMemory fow = new FogOfWarMemory ();
				fow.setMap (createOverlandMap (sys));
				
				final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
				priv.setFogOfWar (vis);
				priv.setFogOfWarMemory (fow);
				
				final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
				if (human)
					player.setConnection (new DummyServerToClientConnection ());
				
				players.add (player);

				// Mock finding the player
				if ((canSee) && (human))
					when (multiplayerSessionServerUtils.findPlayerWithID (players, playerID, "addBuildingOnServerAndClients")).thenReturn (player);
			}
		
		// The human player owns the city
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (4);
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Set up object to test
		final FogOfWarProcessing proc = mock (FogOfWarProcessing.class);
		
		final FogOfWarDuplicationImpl dup = new FogOfWarDuplicationImpl ();		// Used real one here, since makes it easier to check the output than via mock
		dup.setMemoryBuildingUtils (new MemoryBuildingUtilsImpl ());
		
		final FogOfWarMidTurnChangesImpl midTurn = new FogOfWarMidTurnChangesImpl ();
		midTurn.setFogOfWarCalculations (single);
		midTurn.setFogOfWarProcessing (proc);
		midTurn.setFogOfWarDuplication (dup);
		midTurn.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run test
		midTurn.addBuildingOnServerAndClients (gsk, players, new MapCoordinates3DEx (20, 10, 1), Arrays.asList ("BL03"), null, null, sd, db);
		
		// Prove that building got added to server's true map
		assertEquals (1, trueMap.getBuilding ().size ());
		assertEquals (1, trueMap.getBuilding ().get (0).getBuildingURN ());
		assertEquals ("BL03", trueMap.getBuilding ().get (0).getBuildingID ());
		assertEquals (new MapCoordinates3DEx (20, 10, 1), trueMap.getBuilding ().get (0).getCityLocation ());
		
		// Prove that building got added to server's copy of each player's memory, but only the players who can see it
		for (int playerIndex = 0; playerIndex < 4; playerIndex++)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) players.get (playerIndex).getPersistentPlayerPrivateKnowledge ();
			if (playerIndex < 2)
				assertEquals (0, priv.getFogOfWarMemory ().getBuilding ().size ());
			else
			{
				assertEquals (1, priv.getFogOfWarMemory ().getBuilding ().size ());
				assertEquals (1, priv.getFogOfWarMemory ().getBuilding ().get (0).getBuildingURN ());
				assertEquals ("BL03", priv.getFogOfWarMemory ().getBuilding ().get (0).getBuildingID ());
				assertEquals (new MapCoordinates3DEx (20, 10, 1), priv.getFogOfWarMemory ().getBuilding ().get (0).getCityLocation ());
			}
		}

		// Only player 4 (can see city, and is human) should get a message
		for (int playerIndex = 0; playerIndex < 4; playerIndex++)
		{
			final PlayerServerDetails player = players.get (playerIndex);
			if (player.getPlayerDescription ().isHuman ())
			{
				final DummyServerToClientConnection conn = (DummyServerToClientConnection) player.getConnection ();
				if (playerIndex != 3)
					assertEquals (0, conn.getMessages ().size ());
				else
				{
					assertEquals (1, conn.getMessages ().size ());
					
					final AddBuildingMessage msg = (AddBuildingMessage) conn.getMessages ().get (0);
					assertEquals (1, msg.getBuilding ().size ());
					assertEquals (new MapCoordinates3DEx (20, 10, 1), msg.getBuilding ().get (0).getCityLocation ());
					assertEquals ("BL03", msg.getBuilding ().get (0).getBuildingID ());
					assertEquals (1, msg.getBuilding ().get (0).getBuildingURN ());
					assertNull (msg.getBuildingsCreatedFromSpellID ());
					assertNull (msg.getBuildingCreationSpellCastByPlayerID ());
				}
			}
		}

		// Only the city owner's FOW should be updated
		for (int playerIndex = 0; playerIndex < 4; playerIndex++)
			verify (proc, times (playerIndex == (cityData.getCityOwnerID () - 1) ? 1 : 0)).updateAndSendFogOfWar
				(trueMap, players.get (playerIndex), players, "addBuildingOnServerAndClients", sd, db);
		
		verifyNoMoreInteractions (proc);
	}

	/**
	 * Tests the addBuildingOnServerAndClients method, when its casting Move Fortress which adds two buildings at once
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAddBuildingOnServerAndClients_TwoBuildings () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Session description
		final FogOfWarValue fowSetting = FogOfWarValue.REMEMBER_AS_LAST_SEEN;
		
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		fowSettings.setCitiesSpellsAndCombatAreaEffects (fowSetting);

		final OverlandMapSize sys = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (sys);
		sd.setFogOfWarSetting (fowSettings);

		// FOW visibility rules
		final FogOfWarCalculations single = mock (FogOfWarCalculations.class);
		when (single.canSeeMidTurn (FogOfWarStateID.NEVER_SEEN, fowSetting)).thenReturn (false);
		when (single.canSeeMidTurn (FogOfWarStateID.CAN_SEE, fowSetting)).thenReturn (true);
		
		// Server knowledge
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		gsk.setNextFreeBuildingURN (1);
		
		// Players can see the city or not, and be human/AI, so create 4 players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);

		int playerID = 0;
		for (final boolean canSee : new boolean [] {false, true})
			for (final boolean human : new boolean [] {false, true})
			{
				playerID++;
				
				final PlayerDescription pd = new PlayerDescription ();
				pd.setPlayerID (playerID);
				pd.setHuman (human);
				
				// Mock whether the player can see the city
				final MapVolumeOfFogOfWarStates vis = createFogOfWarArea (sys);
				vis.getPlane ().get (1).getRow ().get (10).getCell ().set (20, canSee ? FogOfWarStateID.CAN_SEE : FogOfWarStateID.NEVER_SEEN);
				
				final FogOfWarMemory fow = new FogOfWarMemory ();
				fow.setMap (createOverlandMap (sys));
				
				final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
				priv.setFogOfWar (vis);
				priv.setFogOfWarMemory (fow);
				
				final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
				if (human)
					player.setConnection (new DummyServerToClientConnection ());
				
				players.add (player);

				// Mock finding the player
				if ((canSee) && (human))
					when (multiplayerSessionServerUtils.findPlayerWithID (players, playerID, "addBuildingOnServerAndClients")).thenReturn (player);
			}
		
		// The human player owns the city
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (4);
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Set up object to test
		final FogOfWarProcessing proc = mock (FogOfWarProcessing.class);
		
		final FogOfWarDuplicationImpl dup = new FogOfWarDuplicationImpl ();		// Used real one here, since makes it easier to check the output than via mock
		dup.setMemoryBuildingUtils (new MemoryBuildingUtilsImpl ());
		
		final FogOfWarMidTurnChangesImpl midTurn = new FogOfWarMidTurnChangesImpl ();
		midTurn.setFogOfWarCalculations (single);
		midTurn.setFogOfWarProcessing (proc);
		midTurn.setFogOfWarDuplication (dup);
		midTurn.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run test
		midTurn.addBuildingOnServerAndClients (gsk, players, new MapCoordinates3DEx (20, 10, 1), Arrays.asList ("BL03", "BL04"), "SP001", 2, sd, db);
		
		// Prove that both buildings got added to server's true map
		assertEquals (2, trueMap.getBuilding ().size ());
		assertEquals (1, trueMap.getBuilding ().get (0).getBuildingURN ());
		assertEquals ("BL03", trueMap.getBuilding ().get (0).getBuildingID ());
		assertEquals (new MapCoordinates3DEx (20, 10, 1), trueMap.getBuilding ().get (0).getCityLocation ());
		assertEquals (2, trueMap.getBuilding ().get (1).getBuildingURN ());
		assertEquals ("BL04", trueMap.getBuilding ().get (1).getBuildingID ());
		assertEquals (new MapCoordinates3DEx (20, 10, 1), trueMap.getBuilding ().get (1).getCityLocation ());
		
		// Prove that the buildings got added to server's copy of each player's memory, but only the players who can see it
		for (int playerIndex = 0; playerIndex < 4; playerIndex++)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) players.get (playerIndex).getPersistentPlayerPrivateKnowledge ();
			if (playerIndex < 2)
				assertEquals (0, priv.getFogOfWarMemory ().getBuilding ().size ());
			else
			{
				assertEquals (2, priv.getFogOfWarMemory ().getBuilding ().size ());
				assertEquals (1, priv.getFogOfWarMemory ().getBuilding ().get (0).getBuildingURN ());
				assertEquals ("BL03", priv.getFogOfWarMemory ().getBuilding ().get (0).getBuildingID ());
				assertEquals (new MapCoordinates3DEx (20, 10, 1), priv.getFogOfWarMemory ().getBuilding ().get (0).getCityLocation ());
				assertEquals (2, priv.getFogOfWarMemory ().getBuilding ().get (1).getBuildingURN ());
				assertEquals ("BL04", priv.getFogOfWarMemory ().getBuilding ().get (1).getBuildingID ());
				assertEquals (new MapCoordinates3DEx (20, 10, 1), priv.getFogOfWarMemory ().getBuilding ().get (1).getCityLocation ());
			}
		}

		// Only player 4 (can see city, and is human) should get a message
		for (int playerIndex = 0; playerIndex < 4; playerIndex++)
		{
			final PlayerServerDetails player = players.get (playerIndex);
			if (player.getPlayerDescription ().isHuman ())
			{
				final DummyServerToClientConnection conn = (DummyServerToClientConnection) player.getConnection ();
				if (playerIndex != 3)
					assertEquals (0, conn.getMessages ().size ());
				else
				{
					assertEquals (1, conn.getMessages ().size ());
					
					final AddBuildingMessage msg = (AddBuildingMessage) conn.getMessages ().get (0);
					assertEquals (2, msg.getBuilding ().size ());
					assertEquals (new MapCoordinates3DEx (20, 10, 1), msg.getBuilding ().get (0).getCityLocation ());
					assertEquals ("BL03", msg.getBuilding ().get (0).getBuildingID ());
					assertEquals (1, msg.getBuilding ().get (0).getBuildingURN ());
					assertEquals (new MapCoordinates3DEx (20, 10, 1), msg.getBuilding ().get (1).getCityLocation ());
					assertEquals ("BL04", msg.getBuilding ().get (1).getBuildingID ());
					assertEquals (2, msg.getBuilding ().get (1).getBuildingURN ());
					assertEquals ("SP001", msg.getBuildingsCreatedFromSpellID ());
					assertEquals (2, msg.getBuildingCreationSpellCastByPlayerID ().intValue ());
				}
			}
		}

		// Only the city owner's FOW should be updated
		for (int playerIndex = 0; playerIndex < 4; playerIndex++)
			verify (proc, times (playerIndex == (cityData.getCityOwnerID () - 1) ? 1 : 0)).updateAndSendFogOfWar
				(trueMap, players.get (playerIndex), players, "addBuildingOnServerAndClients", sd, db);
		
		verifyNoMoreInteractions (proc);
	}
	
	/**
	 * Tests the destroyBuildingOnServerAndClients method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDestroyBuildingOnServerAndClients () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Session description
		final FogOfWarValue fowSetting = FogOfWarValue.REMEMBER_AS_LAST_SEEN;
		
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		fowSettings.setCitiesSpellsAndCombatAreaEffects (fowSetting);

		final OverlandMapSize sys = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (sys);
		sd.setFogOfWarSetting (fowSettings);

		// FOW visibility rules
		final FogOfWarCalculations single = mock (FogOfWarCalculations.class);
		when (single.canSeeMidTurn (FogOfWarStateID.NEVER_SEEN, fowSetting)).thenReturn (false);
		when (single.canSeeMidTurn (FogOfWarStateID.CAN_SEE, fowSetting)).thenReturn (true);
		
		// Overland map
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Players can see the city or not, and be human/AI, so create 4 players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);

		int playerID = 0;
		for (final boolean canSee : new boolean [] {false, true})
			for (final boolean human : new boolean [] {false, true})
			{
				playerID++;
				
				final PlayerDescription pd = new PlayerDescription ();
				pd.setPlayerID (playerID);
				pd.setHuman (human);
				
				// Mock whether the player can see the city
				final MapVolumeOfFogOfWarStates vis = createFogOfWarArea (sys);
				vis.getPlane ().get (1).getRow ().get (10).getCell ().set (20, canSee ? FogOfWarStateID.CAN_SEE : FogOfWarStateID.NEVER_SEEN);
				
				// Need to make the spell lists unique for verify to work correctly
				final FogOfWarMemory fow = new FogOfWarMemory ();
				fow.getBuilding ().add (new MemoryBuilding ());
				fow.setMap (createOverlandMap (sys));
				
				final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
				priv.setFogOfWar (vis);
				priv.setFogOfWarMemory (fow);
				
				final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
				if (human)
					player.setConnection (new DummyServerToClientConnection ());
				
				players.add (player);

				// Mock finding the player
				if ((canSee) && (human))
					when (multiplayerSessionServerUtils.findPlayerWithID (players, playerID, "destroyBuildingOnServerAndClients")).thenReturn (player);
			}
		
		// The human player owns the city
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (4);
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// The building being destroyed
		final MemoryBuilding trueBuilding = new MemoryBuilding ();
		trueBuilding.setBuildingURN (3);
		trueBuilding.setBuildingID ("BL03");
		trueBuilding.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getBuilding ().add (trueBuilding);

		final MemoryBuildingUtils buildingUtils = mock (MemoryBuildingUtils.class);
		when (buildingUtils.findBuildingURN (trueBuilding.getBuildingURN (), trueMap.getBuilding (), "destroyBuildingOnServerAndClients")).thenReturn (trueBuilding);
		
		// Set up object to test
		final FogOfWarProcessing proc = mock (FogOfWarProcessing.class);
		
		final FogOfWarMidTurnChangesImpl midTurn = new FogOfWarMidTurnChangesImpl ();
		midTurn.setFogOfWarCalculations (single);
		midTurn.setFogOfWarProcessing (proc);
		midTurn.setMemoryBuildingUtils (buildingUtils);
		midTurn.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run test
		midTurn.destroyBuildingOnServerAndClients (trueMap, players, Arrays.asList (trueBuilding.getBuildingURN ()), false, null, null, null, sd, db);
		
		// Prove that building got removed from server's true map
		verify (buildingUtils, times (1)).removeBuildingURN (trueBuilding.getBuildingURN (), trueMap.getBuilding ());
		
		// Prove that building got removed from server's copy of each player's memory, but only the players who saw it disappear
		for (int playerIndex = 0; playerIndex < 4; playerIndex++)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) players.get (playerIndex).getPersistentPlayerPrivateKnowledge ();
			verify (buildingUtils, times (playerIndex < 2 ? 0 : 1)).removeBuildingURN (trueBuilding.getBuildingURN (), priv.getFogOfWarMemory ().getBuilding ());
		}

		// Prove that only the human player's client that saw the building disappear was sent update msg
		for (int playerIndex = 0; playerIndex < 4; playerIndex++)
		{
			final PlayerServerDetails player = players.get (playerIndex);
			if (player.getPlayerDescription ().isHuman ())
			{
				final DummyServerToClientConnection conn = (DummyServerToClientConnection) player.getConnection ();
				if (playerIndex != 3)
					assertEquals (0, conn.getMessages ().size ());
				else
				{
					assertEquals (1, conn.getMessages ().size ());
					
					final DestroyBuildingMessage msg = (DestroyBuildingMessage) conn.getMessages ().get (0);
					assertEquals (1, msg.getBuildingURN ().size ());
					assertEquals (trueBuilding.getBuildingURN (), msg.getBuildingURN ().get (0).intValue ());
					assertFalse (msg.isUpdateBuildingSoldThisTurn ());
				}
			}
		}
		
		// Only the city owner's FOW should be updated
		for (int playerIndex = 0; playerIndex < 4; playerIndex++)
			verify (proc, times (playerIndex == (cityData.getCityOwnerID () - 1) ? 1 : 0)).updateAndSendFogOfWar
				(trueMap, players.get (playerIndex), players, "destroyBuildingOnServerAndClients", sd, db);

		verifyNoMoreInteractions (buildingUtils);
		verifyNoMoreInteractions (proc);
	}
	
	/**
	 * Tests the determineMovementDirection method in the simplest situation where we moving from one cell to an adjacent cell 
	 */
	@Test
	public final void testDetermineMovementDirection_SingleCell ()
	{
		// Movement array
		final OverlandMovementCell [] [] [] moves = new OverlandMovementCell [2] [40] [60];
		
		final OverlandMovementCell move = new OverlandMovementCell ();
		move.setMovedFrom (new MapCoordinates3DEx (20, 10, 0));
		moves [0] [10] [21] = move;
		
		// Set up object to test
		final FogOfWarMidTurnChangesImpl midTurn = new FogOfWarMidTurnChangesImpl ();
		
		// Run method
		assertEquals (new MapCoordinates3DEx (21, 10, 0),
			midTurn.determineMovementDirection (new MapCoordinates3DEx (20, 10, 0), new MapCoordinates3DEx (21, 10, 0), moves));
	}

	/**
	 * Tests the determineMovementDirection method when our target is two cells away so we must return the cell in the middle 
	 */
	@Test
	public final void testDetermineMovementDirection_TwoCells ()
	{
		// Movement array
		final OverlandMovementCell [] [] [] moves = new OverlandMovementCell [2] [40] [60];
		
		final OverlandMovementCell move1 = new OverlandMovementCell ();
		move1.setMovedFrom (new MapCoordinates3DEx (20, 10, 0));
		moves [0] [10] [21] = move1;

		final OverlandMovementCell move2 = new OverlandMovementCell ();
		move2.setMovedFrom (new MapCoordinates3DEx (21, 10, 0));
		moves [0] [10] [22] = move2;
		
		// Set up object to test
		final FogOfWarMidTurnChangesImpl midTurn = new FogOfWarMidTurnChangesImpl ();
		
		// Run method
		assertEquals (new MapCoordinates3DEx (21, 10, 0),
			midTurn.determineMovementDirection (new MapCoordinates3DEx (20, 10, 0), new MapCoordinates3DEx (22, 10, 0), moves));
	}

	/**
	 * Tests the determineMovementDirection method when we are crossing a tower from Arcanus to Myrror 
	 */
	@Test
	public final void testDetermineMovementDirection_ArcanusToMyrror ()
	{
		// Movement array
		final OverlandMovementCell [] [] [] moves = new OverlandMovementCell [2] [40] [60];
		
		final OverlandMovementCell move1 = new OverlandMovementCell ();
		move1.setMovedFrom (new MapCoordinates3DEx (20, 10, 0));
		moves [0] [10] [21] = move1;

		// Tower is at 21, 10 so we go from (20, 10, 0) > (21, 10, tower) > (22, 10, 1) 
		
		final OverlandMovementCell move2 = new OverlandMovementCell ();
		move2.setMovedFrom (new MapCoordinates3DEx (21, 10, 0));
		moves [1] [10] [22] = move2;
		
		// Set up object to test
		final FogOfWarMidTurnChangesImpl midTurn = new FogOfWarMidTurnChangesImpl ();
		
		// Run method
		assertEquals (new MapCoordinates3DEx (21, 10, 0),
			midTurn.determineMovementDirection (new MapCoordinates3DEx (20, 10, 0), new MapCoordinates3DEx (22, 10, 1), moves));
	}

	/**
	 * Tests the determineMovementDirection method when we are crossing a tower from Arcanus to Myrror 
	 */
	@Test
	public final void testDetermineMovementDirection_MyrrorToArcanus ()
	{
		// Movement array
		final OverlandMovementCell [] [] [] moves = new OverlandMovementCell [2] [40] [60];
		
		final OverlandMovementCell move1 = new OverlandMovementCell ();
		move1.setMovedFrom (new MapCoordinates3DEx (20, 10, 1));
		moves [0] [10] [21] = move1;

		// Tower is at 21, 10 so we go from (20, 10, 1) > (21, 10, tower) > (22, 10, 0) 
		
		final OverlandMovementCell move2 = new OverlandMovementCell ();
		move2.setMovedFrom (new MapCoordinates3DEx (21, 10, 0));
		moves [0] [10] [22] = move2;
		
		// Set up object to test
		final FogOfWarMidTurnChangesImpl midTurn = new FogOfWarMidTurnChangesImpl ();
		
		// Run method
		assertEquals (new MapCoordinates3DEx (21, 10, 0),
			midTurn.determineMovementDirection (new MapCoordinates3DEx (20, 10, 1), new MapCoordinates3DEx (22, 10, 0), moves));
	}
}