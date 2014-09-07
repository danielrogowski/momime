package momime.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_5.MomScheduledCombat;

import org.junit.Test;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the ScheduledCombatUtilsImpl class
 */
public final class TestScheduledCombatUtilsImpl
{
	/**
	 * Tests the findScheduledCombatURN method looking for a combat that exists
	 * @throws RecordNotFoundException If the requested combat URN doesn't exist in the list
	 */
	@Test
	public final void testFindScheduledCombatURN_Exists () throws RecordNotFoundException
	{
		// Make test list
		final List<MomScheduledCombat> combats = new ArrayList<MomScheduledCombat> ();
		for (int n = 1; n <= 3; n++)
		{
			final MomScheduledCombat combat = new MomScheduledCombat ();
			combat.setScheduledCombatURN (n);
			combats.add (combat);
		}
		
		// Run test
		assertEquals (2, new ScheduledCombatUtilsImpl ().findScheduledCombatURN (combats, 2).getScheduledCombatURN ());
		assertEquals (2, new ScheduledCombatUtilsImpl ().findScheduledCombatURN (combats, 2, "x").getScheduledCombatURN ());
	}

	/**
	 * Tests the findScheduledCombatURN method looking for a combat that doesn't exists
	 * @throws RecordNotFoundException If the requested combat URN doesn't exist in the list
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindScheduledCombatURN_NotExists () throws RecordNotFoundException
	{
		// Make test list
		final List<MomScheduledCombat> combats = new ArrayList<MomScheduledCombat> ();
		for (int n = 1; n <= 3; n++)
		{
			final MomScheduledCombat combat = new MomScheduledCombat ();
			combat.setScheduledCombatURN (n);
			combats.add (combat);
		}
		
		// Run test
		assertNull (new ScheduledCombatUtilsImpl ().findScheduledCombatURN (combats, 4));
		new ScheduledCombatUtilsImpl ().findScheduledCombatURN (combats, 4, "x");
	}
	
	/**
	 * Tests the determineOtherHumanPlayer method
	 * @throws PlayerNotFoundException If one of the players listed for the combat can't be found in the players list
	 */
	@Test
	public final void testDetermineOtherHumanPlayer () throws PlayerNotFoundException
	{
		// Set up two human players and one AI player
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setHuman (true);
		pd1.setPlayerID (1);
		final PlayerPublicDetails human1 = new PlayerPublicDetails (pd1, null, null);
		players.add (human1);
		
		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setHuman (true);
		pd2.setPlayerID (2);
		final PlayerPublicDetails human2 = new PlayerPublicDetails (pd2, null, null);
		players.add (human2);
		
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setHuman (false);
		pd3.setPlayerID (-1);
		players.add (new PlayerPublicDetails (pd3, null, null));
		final PlayerPublicDetails ai1 = new PlayerPublicDetails (pd3, null, null);
		players.add (ai1);
		
		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd1.getPlayerID (), "determineOtherHumanPlayer")).thenReturn (human1);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd2.getPlayerID (), "determineOtherHumanPlayer")).thenReturn (human2);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd3.getPlayerID (), "determineOtherHumanPlayer")).thenReturn (ai1);
		
		// Set up object to test
		final ScheduledCombatUtilsImpl utils = new ScheduledCombatUtilsImpl ();
		utils.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		// Test combat with no defender
		final MomScheduledCombat combat = new MomScheduledCombat ();
		combat.setAttackingPlayerID (1);
		assertNull (utils.determineOtherHumanPlayer (combat, human1, players));

		combat.setAttackingPlayerID (-1);
		assertNull (utils.determineOtherHumanPlayer (combat, ai1, players));
		
		// Test combat with an AI player attacking a human player
		combat.setDefendingPlayerID (1);
		assertNull (utils.determineOtherHumanPlayer (combat, human1, players));
		assertSame (human1, utils.determineOtherHumanPlayer (combat, ai1, players));
		
		// Try requesting for a player who isn't even involved - should get null even though there is an "other" human player
		assertNull (utils.determineOtherHumanPlayer (combat, human2, players));
		
		// Test combat with two human players
		combat.setAttackingPlayerID (2);
		assertSame (human1, utils.determineOtherHumanPlayer (combat, human2, players));
		assertSame (human2, utils.determineOtherHumanPlayer (combat, human1, players));
		assertNull (utils.determineOtherHumanPlayer (combat, ai1, players));
		
		// "Walk in without a fight" nullifies every other condition
		combat.setWalkInWithoutAFight (true);
		assertNull (utils.determineOtherHumanPlayer (combat, human2, players));
	}
}
