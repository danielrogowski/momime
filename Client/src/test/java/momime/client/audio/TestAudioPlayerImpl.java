package momime.client.audio;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.ndg.random.RandomUtils;

/**
 * Tests the AudioPlayerImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestAudioPlayerImpl
{
	/**
	 * Tests playing a single fixed file one time, like would be used to play sound effects
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testPlayAudioFile_NoLoop () throws Exception
	{
		// Mock players
		final AdvancedPlayer player1 = mock (AdvancedPlayer.class);
		final AdvancedPlayer player2 = mock (AdvancedPlayer.class);
		
		// Mock factory
		final AdvancedPlayerFactory factory = mock (AdvancedPlayerFactory.class);
		when (factory.createAdvancedPlayer ()).thenReturn (player1, player2);
		
		// Set up object to test
		final AudioPlayerImpl obj = new AudioPlayerImpl ();
		obj.setAdvancedPlayerFactory (factory);
		
		// Run method
		obj.playAudioFile ("Test.mp3");
		
		// The "player" runs off in another thread, so give it some time to complete
		Thread.sleep (500);
		
		// Check results
		verify (player1, times (1)).setInput ("Test.mp3");
		verify (player1, times (1)).play ();
		verify (player2, times (0)).setInput (anyString ());
		verify (player2, times (0)).play ();
	}

	/**
	 * Tests playing a single fixed file repeatedly, like is used to play the music on the title screen
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testPlayAudioFile_Loop () throws Exception
	{
		// Mock players
		final AdvancedPlayer player1 = mock (AdvancedPlayer.class);
		final AdvancedPlayer player2 = mock (AdvancedPlayer.class);
		final AdvancedPlayer player3 = mock (AdvancedPlayer.class);
		final AdvancedPlayer player4 = mock (AdvancedPlayer.class);
		
		// Allow it to loop 3 times
		final AudioPlayerImpl obj = new AudioPlayerImpl ();
		doAnswer (new Answer<Void> ()
		{
			@Override
			public final Void answer (@SuppressWarnings ("unused") final InvocationOnMock invocation) throws Throwable
			{
				obj.setLoop (false);
				return null;
			}
		}).when (player3).play ();
		
		// Mock factory
		final AdvancedPlayerFactory factory = mock (AdvancedPlayerFactory.class);
		when (factory.createAdvancedPlayer ()).thenReturn (player1, player2, player3, player4);
		
		// Set up object to test
		obj.setAdvancedPlayerFactory (factory);
		obj.setLoop (true);
		
		// Run method
		obj.playAudioFile ("Test.mp3");

		// The "player" runs off in another thread, so give it some time to complete
		Thread.sleep (500);
		
		// Check results
		verify (player1, times (1)).setInput ("Test.mp3");
		verify (player1, times (1)).play ();
		verify (player2, times (1)).setInput ("Test.mp3");
		verify (player2, times (1)).play ();
		verify (player3, times (1)).setInput ("Test.mp3");
		verify (player3, times (1)).play ();
		verify (player4, times (0)).setInput (anyString ());
		verify (player4, times (0)).play ();
	}

	/**
	 * Tests playing a random file one time, can't think of a real example where would be used
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testPlayAudioFiles_Shuffle_NoLoop () throws Exception
	{
		// Mock players
		final AdvancedPlayer player1 = mock (AdvancedPlayer.class);
		final AdvancedPlayer player2 = mock (AdvancedPlayer.class);
		
		// Mock factory
		final AdvancedPlayerFactory factory = mock (AdvancedPlayerFactory.class);
		when (factory.createAdvancedPlayer ()).thenReturn (player1, player2);
		
		// Set up play list
		final List<String> playList = new ArrayList<String> ();
		for (int n = 1; n <= 3; n++)
			playList.add ("Test" + n + ".mp3");
		
		// Mock random selection
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (3)).thenReturn (1);
		
		// Set up object to test
		final AudioPlayerImpl obj = new AudioPlayerImpl ();
		obj.setAdvancedPlayerFactory (factory);
		obj.setRandomUtils (random);
		obj.setShuffle (true);
		
		// Run method
		obj.playAudioFiles (playList);
		
		// The "player" runs off in another thread, so give it some time to complete
		Thread.sleep (500);
		
		// Check results
		verify (player1, times (1)).setInput ("Test2.mp3");
		verify (player1, times (1)).play ();
		verify (player2, times (0)).setInput (anyString ());
		verify (player2, times (0)).play ();
	}

	/**
	 * Tests playing a series of random files, like playing random background music tracks on the overland map
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testPlayAudioFiles_Shuffle_Loop () throws Exception
	{
		// Mock players
		final AdvancedPlayer player1 = mock (AdvancedPlayer.class);
		final AdvancedPlayer player2 = mock (AdvancedPlayer.class);
		final AdvancedPlayer player3 = mock (AdvancedPlayer.class);
		final AdvancedPlayer player4 = mock (AdvancedPlayer.class);
		
		// Allow it to loop 3 times
		final AudioPlayerImpl obj = new AudioPlayerImpl ();
		doAnswer (new Answer<Void> ()
		{
			@Override
			public final Void answer (@SuppressWarnings ("unused") final InvocationOnMock invocation) throws Throwable
			{
				obj.setLoop (false);
				return null;
			}
		}).when (player3).play ();
		
		// Mock factory
		final AdvancedPlayerFactory factory = mock (AdvancedPlayerFactory.class);
		when (factory.createAdvancedPlayer ()).thenReturn (player1, player2, player3, player4);
		
		// Set up play list
		final List<String> playList = new ArrayList<String> ();
		for (int n = 1; n <= 3; n++)
			playList.add ("Test" + n + ".mp3");
		
		// Mock random selection
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (3)).thenReturn (1, 0, 2, 1);
		
		// Set up object to test
		obj.setAdvancedPlayerFactory (factory);
		obj.setRandomUtils (random);
		obj.setShuffle (true);
		obj.setLoop (true);
		
		// Run method
		obj.playAudioFiles (playList);
		
		// The "player" runs off in another thread, so give it some time to complete
		Thread.sleep (500);
		
		// Check results
		verify (player1, times (1)).setInput ("Test2.mp3");
		verify (player1, times (1)).play ();
		verify (player2, times (1)).setInput ("Test1.mp3");
		verify (player2, times (1)).play ();
		verify (player3, times (1)).setInput ("Test3.mp3");
		verify (player3, times (1)).play ();
		verify (player4, times (0)).setInput (anyString ());
		verify (player4, times (0)).play ();
	}

	/**
	 * Tests playing a series files in a fixed order, like playing a wizard's combat prelude followed by looping the main combat track
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testPlayAudioFiles_Loop_NoShuffle () throws Exception
	{
		// Mock players
		final AdvancedPlayer player1 = mock (AdvancedPlayer.class);
		final AdvancedPlayer player2 = mock (AdvancedPlayer.class);
		final AdvancedPlayer player3 = mock (AdvancedPlayer.class);
		final AdvancedPlayer player4 = mock (AdvancedPlayer.class);
		
		// Allow it to loop 3 times
		final AudioPlayerImpl obj = new AudioPlayerImpl ();
		doAnswer (new Answer<Void> ()
		{
			@Override
			public final Void answer (@SuppressWarnings ("unused") final InvocationOnMock invocation) throws Throwable
			{
				obj.setLoop (false);
				return null;
			}
		}).when (player3).play ();
		
		// Mock factory
		final AdvancedPlayerFactory factory = mock (AdvancedPlayerFactory.class);
		when (factory.createAdvancedPlayer ()).thenReturn (player1, player2, player3, player4);
		
		// Set up play list
		final List<String> playList = new ArrayList<String> ();
		for (int n = 1; n <= 2; n++)
			playList.add ("Test" + n + ".mp3");
		
		// Set up object to test
		obj.setAdvancedPlayerFactory (factory);
		obj.setLoop (true);
		
		// Run method
		obj.playAudioFiles (playList);
		
		// The "player" runs off in another thread, so give it some time to complete
		Thread.sleep (500);
		
		// Check results
		verify (player1, times (1)).setInput ("Test1.mp3");
		verify (player1, times (1)).play ();
		verify (player2, times (1)).setInput ("Test2.mp3");
		verify (player2, times (1)).play ();
		verify (player3, times (1)).setInput ("Test2.mp3");
		verify (player3, times (1)).play ();
		verify (player4, times (0)).setInput (anyString ());
		verify (player4, times (0)).play ();
	}
	
	/**
	 * Tests the playThenResume method, like playing the background music on the overland map, then
	 * a new turn message arrives and plays a specific track, then goes back to playing random background music 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testPlayThenResume () throws Exception
	{
		// Mock players
		final AdvancedPlayer player1 = mock (AdvancedPlayer.class);
		final AdvancedPlayer player2 = mock (AdvancedPlayer.class);
		final AdvancedPlayer player3 = mock (AdvancedPlayer.class);
		final AdvancedPlayer player4 = mock (AdvancedPlayer.class);
		final AdvancedPlayer player5 = mock (AdvancedPlayer.class);
		final AdvancedPlayer player6 = mock (AdvancedPlayer.class);
		final AdvancedPlayer player7 = mock (AdvancedPlayer.class);
		
		// The audio tracks are 1/2 second long
		final Answer<Void> delayHalfSecond = new Answer<Void> ()
		{
			@Override
			public final Void answer (@SuppressWarnings ("unused") final InvocationOnMock invocation) throws Throwable
			{
				Thread.sleep (500);
				return null;
			}
		};
		
		doAnswer (delayHalfSecond).when (player1).play ();
		doAnswer (delayHalfSecond).when (player2).play ();
		doAnswer (delayHalfSecond).when (player3).play ();
		doAnswer (delayHalfSecond).when (player4).play ();
		doAnswer (delayHalfSecond).when (player5).play ();
		doAnswer (delayHalfSecond).when (player6).play ();
		
		// Mock factory
		final AdvancedPlayerFactory factory = mock (AdvancedPlayerFactory.class);
		when (factory.createAdvancedPlayer ()).thenReturn (player1, player2, player3, player4, player5, player6, player7);
		
		// Set up play list
		final List<String> playList = new ArrayList<String> ();
		for (int n = 1; n <= 3; n++)
			playList.add ("Test" + n + ".mp3");
		
		// Mock random selection
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (3)).thenReturn (1, 0, 2, 1, 2);
		
		// Set up object to test
		final AudioPlayerImpl obj = new AudioPlayerImpl ();
		obj.setAdvancedPlayerFactory (factory);
		obj.setRandomUtils (random);
		obj.setShuffle (true);
		obj.setLoop (true);
		
		// Run method
		obj.playAudioFiles (playList);
		
		// Let it play 2 tracks, then halfway through the 3rd, stop it and make it play something else
		Thread.sleep (1250);
		obj.playThenResume ("Interrupt.mp3");
		
		// Give it time to play the interrupting track, and 2 more
		Thread.sleep (1250);
		obj.setLoop (false);
		
		// Check results
		verify (player1, times (1)).setInput ("Test2.mp3");
		verify (player1, times (1)).play ();
		verify (player2, times (1)).setInput ("Test1.mp3");
		verify (player2, times (1)).play ();
		verify (player3, times (1)).setInput ("Test3.mp3");
		verify (player3, times (1)).play ();
		verify (player4, times (1)).setInput ("Interrupt.mp3");
		verify (player4, times (1)).play ();
		verify (player5, times (1)).setInput ("Test2.mp3");
		verify (player5, times (1)).play ();
		verify (player6, times (1)).setInput ("Test3.mp3");
		verify (player6, times (1)).play ();
		verify (player7, times (0)).setInput (anyString ());
		verify (player7, times (0)).play ();
	}
}