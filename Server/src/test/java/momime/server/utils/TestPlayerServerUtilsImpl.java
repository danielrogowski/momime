package momime.server.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import momime.common.messages.v0_9_5.MomTransientPlayerPublicKnowledge;

import org.junit.Test;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

/**
 * Tests the PlayerServerUtilsImpl class
 */
public final class TestPlayerServerUtilsImpl
{
	/**
	 * Tests the allPlayersFinishedAllocatingMovement method
	 */
	@Test
	public final void testAllPlayersFinishedAllocatingMovement ()
	{
		// Set up some sample players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final MomTransientPlayerPublicKnowledge tpk1 = new MomTransientPlayerPublicKnowledge ();
		tpk1.setMovementAllocatedForTurnNumber (3);
		players.add (new PlayerServerDetails (null, null, null, tpk1, null));
		
		final MomTransientPlayerPublicKnowledge tpk2 = new MomTransientPlayerPublicKnowledge ();
		tpk2.setMovementAllocatedForTurnNumber (2);
		players.add (new PlayerServerDetails (null, null, null, tpk2, null));
		
		final MomTransientPlayerPublicKnowledge tpk3 = new MomTransientPlayerPublicKnowledge ();
		tpk3.setMovementAllocatedForTurnNumber (3);
		players.add (new PlayerServerDetails (null, null, null, tpk3, null));
		
		// Set up object to test
		final PlayerServerUtilsImpl utils = new PlayerServerUtilsImpl ();
		
		// Leave one not done
		assertFalse (utils.allPlayersFinishedAllocatingMovement (players, 3));
		
		// Now they've finished too
		tpk2.setMovementAllocatedForTurnNumber (3);
		assertTrue (utils.allPlayersFinishedAllocatingMovement (players, 3));
	}
}
