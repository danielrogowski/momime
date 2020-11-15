package momime.client.audio;

import java.util.List;

import javazoom.jl.decoder.JavaLayerException;
import momime.client.graphics.AnimationContainer;
import momime.common.database.RecordNotFoundException;

/**
 * Wrapper around JavaZoom's MP3 player.  The player blocks the thread is running in, so we need a thread-based class to play audio from. 
 * This also handles playlist-like functionality where it'll play another random file when the previous one stops.
 */
public interface AudioPlayer
{
	/**
	 * @param aResourceName Names of single audio resource on classpath, e.g. /music/blah.mp3 
	 * @throws JavaLayerException If there is a problem playing the audio file
	 */
	public void playAudioFile (final String aResourceName) throws JavaLayerException;
	
	/**
	 * 
	 * @param playListID Play list from the graphics XML file to play
	 * @param container Whether the playlist is defined in the graphics or common XML
	 * @throws JavaLayerException If there is a problem playing the audio file
	 * @throws RecordNotFoundException If the play list can't be found
	 */
	public void playPlayList (final String playListID, final AnimationContainer container) throws JavaLayerException, RecordNotFoundException;

	/**
	 * @param aResourceNames Names of audio resources on classpath, e.g. /music/blah.mp3 
	 * @throws JavaLayerException If there is a problem playing the audio file
	 */
	public void playAudioFiles (final List<String> aResourceNames) throws JavaLayerException;
	
	/**
	 * This will play the specified audio file, then resume playing whatever playlist was previous being played.
	 * 
	 * @param resourceName Names of audio resource on classpath, e.g. /music/blah.mp3 
	 * @throws JavaLayerException If there is a problem playing the audio file
	 */
	public void playThenResume (final String resourceName) throws JavaLayerException;
	
	/**
	 * The intention is to use loop=true for playing music and loop=false for playing sound effects 
	 * @return True if the player should play another random file after completing each file
	 */
	public boolean isLoop ();

	/**
	 * The intention is to use loop=true for playing music and loop=false for playing sound effects
	 * @param value True if the player should play another random file after completing each file
	 */
	public void setLoop (final boolean value);
	
	/**
	 * @return Whether tracks will be chosen randomly from a playlist, or played through in set sequence
	 */
	public boolean isShuffle ();
	
	/**
	 * Should set this correctly *before* calling playPlayList or playAudioFiles, since it will affect the track they choose when called
	 * @param shuf Whether tracks will be chosen randomly from a playlist, or played through in set sequence
	 */
	public void setShuffle (final boolean shuf);
}