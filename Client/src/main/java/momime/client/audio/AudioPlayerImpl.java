package momime.client.audio;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.utils.random.RandomUtils;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.PlaybackListener;
import momime.client.MomClient;
import momime.client.graphics.AnimationContainer;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.common.database.PlayList;
import momime.common.database.RecordNotFoundException;

/**
 * Wrapper around JavaZoom's MP3 player.  The player blocks the thread is running in, so we need a thread-based class to play audio from. 
 * This also handles playlist-like functionality where it'll play another random file when the previous one stops.
 */
public final class AudioPlayerImpl implements AudioPlayer
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (AudioPlayerImpl.class);
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Random utils */
	private RandomUtils randomUtils;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Creates new player implementations as needed */
	private AdvancedPlayerFactory advancedPlayerFactory;

	/** True if the player thread is stopping because we've got a new playlist to switch to */
	private boolean stopping;
	
	/** Player that is currently playing an audio file in this thread */
	private AdvancedPlayer player;
	
	/** Names of audio resources on classpath, e.g. /music/blah.mp3 */
	private List<String> resourceNames;
	
	/** Used to uniquely number player threads */
	private int lastThreadNumber;
	
	/** Used to distinguish threads created by one AudioPlayer from another; provide a sensible default though */
	private String playerName = "AudioPlayer";
	
	/**
	 * True if the player should play another random file after completing each file; false means every 'play' kicks off a new thread and will play to completion.
	 * So the intention is to use loop=true for playing music and loop=false for playing sound effects.
	 */
	private boolean loop;

	/**
	 * Whether tracks will be chosen randomly from a playlist, or played through in set sequence.
	 * Should set this correctly *before* calling playPlayList or playAudioFiles, since it will affect the track they choose when called.
	 */
	private boolean shuffle;
	
	/**
	 * @param aResourceName Names of single audio resource on classpath, e.g. /music/blah.mp3 
	 * @throws JavaLayerException If there is a problem playing the audio file
	 */
	@Override
	public final void playAudioFile (final String aResourceName) throws JavaLayerException
	{
		final List<String> singleItemList = new ArrayList<String> ();
		singleItemList.add (aResourceName);
		playAudioFiles (singleItemList);
	}
	
	/**
	 * @param playListID Play list from the graphics XML file to play
	 * @param container Whether the playlist is defined in the graphics or common XML
	 * @throws JavaLayerException If there is a problem playing the audio file
	 * @throws RecordNotFoundException If the play list can't be found
	 */
	@Override
	public final void playPlayList (final String playListID, final AnimationContainer container) throws JavaLayerException, RecordNotFoundException
	{
		final PlayList playList = (container == AnimationContainer.GRAPHICS_XML) ? getGraphicsDB ().findPlayList (playListID, "playPlayList") :
			getClient ().getClientDB ().findPlayList (playListID, "playPlayList");
		
		// Copy the playlist, so that removing entries if loop=false doesn't change the actual XML file
		final List<String> audioFiles = new ArrayList<String> ();
		audioFiles.addAll (playList.getAudioFile ());
				
		playAudioFiles (audioFiles);
	}

	/**
	 * @param aResourceNames Names of audio resources on classpath, e.g. /music/blah.mp3 
	 * @throws JavaLayerException If there is a problem playing the audio file
	 */
	@Override
	public final void playAudioFiles (final List<String> aResourceNames) throws JavaLayerException
	{
		// Pick a file to play
		resourceNames = aResourceNames;
		if (resourceNames.size () > 0)
		{
			final int index;
			if (isShuffle ())
				index = getRandomUtils ().nextInt (resourceNames.size ());
			else
				index = 0;
			
			playThenResume (resourceNames.get (index));
		}
	}
	
	/**
	 * This will play the specified audio file, then resume playing whatever playlist was previous being played.
	 * 
	 * @param resourceName Names of audio resource on classpath, e.g. /music/blah.mp3 
	 * @throws JavaLayerException If there is a problem playing the audio file
	 */
	@Override
	public final void playThenResume (final String resourceName) throws JavaLayerException
	{
		// Close out previous player if there was one
		if ((player != null) && (!player.isFinished ()) && (isLoop ()))
		{
			log.debug ("Telling existing player thread to stop (" + player + ", " + player.isFinished () + ", " + isLoop () + ")");
			stopping = true;
			player.stop ();
		}
		else
			log.debug ("There was no existing player thread to stop (" + player + ", " + isLoop () + ")");
		
		// Set up new player
		final AdvancedPlayer thisPlayer = getAdvancedPlayerFactory ().createAdvancedPlayer ();
		player = thisPlayer;
		thisPlayer.setInput (resourceName);
		
		// We don't need a playback listener, but the player requires one, so create a blank one
		thisPlayer.setPlayBackListener (new PlaybackListener () {});
		
		// Need to run the player in its own thread
		lastThreadNumber++;
		new Thread (getPlayerName () + "-" + lastThreadNumber)
		{
			@Override
			public final void run ()
			{
				try
				{
					log.debug ("Thread playing \"" + resourceName + "\" starting");
					thisPlayer.play ();
					log.debug ("Thread playing \"" + resourceName + "\" stopping = " + stopping);
					
					// Play another random file
					if ((!stopping) && (isLoop ()))
					{
						log.debug ("Selecting next file to play");

						// If playing in sequence then remove the track we just played from the head of the list
						if ((!isShuffle ()) && (resourceNames.size () > 1))
							resourceNames.remove (0);
						
						playAudioFiles (resourceNames);
					}
					
					stopping = false;
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		}.start ();
	}

	/**
	 * @return Multiplayer client
	 */
	public final MomClient getClient ()
	{
		return client;
	}
	
	/**
	 * @param obj Multiplayer client
	 */
	public final void setClient (final MomClient obj)
	{
		client = obj;
	}
	
	/**
	 * @return Random utils
	 */
	public final RandomUtils getRandomUtils ()
	{
		return randomUtils;
	}

	/**
	 * @param utils Random utils
	 */
	public final void setRandomUtils (final RandomUtils utils)
	{
		randomUtils = utils;
	}

	/**
	 * @return Graphics database
	 */
	public final GraphicsDatabaseEx getGraphicsDB ()
	{
		return graphicsDB;
	}

	/**
	 * @param db Graphics database
	 */
	public final void setGraphicsDB (final GraphicsDatabaseEx db)
	{
		graphicsDB = db;
	}

	/**
	 * @return reates new player implementations as needed
	 */
	public final AdvancedPlayerFactory getAdvancedPlayerFactory ()
	{
		return advancedPlayerFactory;
	}

	/**
	 * @param fac Creates new player implementations as needed
	 */
	public final void setAdvancedPlayerFactory (final AdvancedPlayerFactory fac)
	{
		advancedPlayerFactory = fac;
	}
	
	/**
	 * @return True if the player should play another random file after completing each file
	 */
	@Override
	public final boolean isLoop ()
	{
		return loop;
	}

	/**
	 * @param value True if the player should play another random file after completing each file
	 */
	@Override
	public final void setLoop (final boolean value)
	{
		loop = value;
	}

	/**
	 * @return Whether tracks will be chosen randomly from a playlist, or played through in set sequence
	 */
	@Override
	public final boolean isShuffle ()
	{
		return shuffle;
	}
	
	/**
	 * Should set this correctly *before* calling playPlayList or playAudioFiles, since it will affect the track they choose when called
	 * @param shuf Whether tracks will be chosen randomly from a playlist, or played through in set sequence
	 */
	@Override
	public final void setShuffle (final boolean shuf)
	{
		shuffle = shuf;
	}

	/**
	 * @return Used to distinguish threads created by one AudioPlayer from another
	 */
	public final String getPlayerName ()
	{
		return playerName;
	}

	/**
	 * @param name Used to distinguish threads created by one AudioPlayer from another
	 */
	public final void setPlayerName (final String name)
	{
		playerName = name;
	}
}