package momime.server.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import momime.common.messages.MomTransientPlayerPublicKnowledge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

/**
 * Tests the PlayerServerUtilsImpl class
 */
@ExtendWith(MockitoExtension.class)
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