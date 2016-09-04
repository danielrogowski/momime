package momime.client.audio;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.PlaybackListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Changes AdvancedPlayer to be based on an interface and have a no-arg constructor, so we can mock it out in unit tests
 */
public final class AdvancedPlayerImpl implements AdvancedPlayer
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (AdvancedPlayerImpl.class);
	
	/** Underlying player, created once we set a stream */
	private javazoom.jl.player.advanced.AdvancedPlayer player;
	
	/** True if we completely finished playing an audio track (and therefore can't call stop () on it) */
	private boolean finished;
	
	/**
	 * @param resourceName Names of single audio resource on classpath, e.g. /music/blah.mp3 
	 * @throws JavaLayerException If there is a problem
	 */
	@Override
	public final void setInput (final String resourceName) throws JavaLayerException
	{
		log.trace ("Entering setInput: " + resourceName);
		log.debug ("About to try to play audio file \"" + resourceName + "\"");
		player = new javazoom.jl.player.advanced.AdvancedPlayer (getClass ().getResourceAsStream (resourceName));
		log.trace ("Exiting setInput");
	}
	
	/**
	 * Starts playing the audio stream - calling this locks up the thread until the track completes playing
	 * @throws JavaLayerException If there is a problem
	 */
	@Override
	public final void play () throws JavaLayerException
	{
		log.trace ("Entering play");
		player.play ();
		finished = true;
		log.trace ("Exiting play");
	}
	
	/**
	 * Stops playing the audio stream - must call this from a different thread, since play () locks up the thread it was called in
	 */
	@Override
	public final void stop ()
	{
		log.trace ("Entering stop");
		player.stop ();
		log.trace ("Exiting stop");
	}
	
	/**
	 * @param listener Listener to inform of audio events (must set one even if we don't actually want to receive events)
	 */
	@Override
	public final void setPlayBackListener (final PlaybackListener listener)
	{
		log.trace ("Entering setPlayBackListener");
		player.setPlayBackListener (listener);
		log.trace ("Exiting setPlayBackListener");
	}

	/**
	 * @return True if we completely finished playing an audio track (and therefore can't call stop () on it)
	 */
	@Override
	public final boolean isFinished ()
	{
		return finished;
	}
}