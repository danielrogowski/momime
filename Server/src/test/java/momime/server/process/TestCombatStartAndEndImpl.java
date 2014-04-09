package momime.server.process;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import momime.common.messages.servertoclient.v0_9_5.AskForCaptureCityDecisionMessage;
import momime.common.messages.v0_9_5.FogOfWarMemory;
import momime.common.messages.v0_9_5.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_5.OverlandMapCityData;
import momime.server.DummyServerToClientConnection;
import momime.server.MomSessionVariables;
import momime.server.ServerTestData;
import momime.server.messages.v0_9_5.MomGeneralServerKnowledge;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the CombatStartAndEndImpl class
 */
public final class TestCombatStartAndEndImpl
{
	/**
	 * Just to save repeating this a dozen times in the test cases
	 * @param x X coord
	 * @return Coordinates object
	 */
	private final MapCoordinates3DEx createCoordinates (final int x)
	{
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx ();
		combatLocation.setX (x);
		combatLocation.setY (10);
		combatLocation.setZ (1);
		return combatLocation;
	}

	/**
	 * Tests the combatEnded method when we're captured a city, but didn't decide whether to capture or raze yet
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCombatEnded_CaptureCityUndecided () throws Exception
	{
		// General server knowledge
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Players
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (true);
		attackingPd.setPlayerID (3);
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, null, null, null);
		
		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setHuman (true);
		defendingPd.setPlayerID (5);
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		final PlayerServerDetails winningPlayer = attackingPlayer;

		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Location
		final MapCoordinates3DEx combatLocation = createCoordinates (20);
		
		// There's a city here, owned by the defender
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityPopulation (1);
		cityData.setCityOwnerID (defendingPd.getPlayerID ());
		
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Set up object to test
		final CombatStartAndEndImpl cse = new CombatStartAndEndImpl (); 
		
		// Run method
		cse.combatEnded (combatLocation, attackingPlayer, defendingPlayer, winningPlayer, null, mom);
		
		// Check message was sent
		assertEquals (1, attackingMsgs.getMessages ().size ());
		assertEquals (AskForCaptureCityDecisionMessage.class.getName (), attackingMsgs.getMessages ().get (0).getClass ().getName ());
		final AskForCaptureCityDecisionMessage msg = (AskForCaptureCityDecisionMessage) attackingMsgs.getMessages ().get (0);
		assertEquals (combatLocation, msg.getCityLocation ());
		assertEquals (defendingPd.getPlayerID ().intValue (), msg.getDefendingPlayerID ());
	}
}
