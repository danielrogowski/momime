package momime.client.audio;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.PlaybackListener;

/**
 * Changes AdvancedPlayer to be based on an interface and have a no-arg constructor, so we can mock it out in unit tests
 */
public interface AdvancedPlayer
{
	/**
	 * @param resourceName Names of single audio resource on classpath, e.g. /music/blah.mp3 
	 * @throws JavaLayerException If there is a problem
	 */
	public void setInput (final String resourceName) throws JavaLayerException;
	
	/**
	 * Starts playing the audio stream - calling this locks up the thread until the track completes playing
	 * @throws JavaLayerException If there is a problem
	 */
	public void play () throws JavaLayerException;
	
	/**
	 * Stops playing the audio stream - must call this from a different thread, since play () locks up the thread it was called in
	 */
	public void stop ();
	
	/**
	 * @param listener Listener to inform of audio events (must set one even if we don't actually want to receive events)
	 */
	public void setPlayBackListener (final PlaybackListener listener);

	/**
	 * @return True if we completely finished playing an audio track (and therefore can't call stop () on it)
	 */
	public boolean isFinished ();
}