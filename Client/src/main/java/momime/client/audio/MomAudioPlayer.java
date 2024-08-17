package momime.client.audio;

import com.ndg.audio.AudioPlayer;

import javazoom.jl.decoder.JavaLayerException;
import momime.client.graphics.AnimationContainer;
import momime.common.database.RecordNotFoundException;

/**
 * Extends vanilla auto player with ability to play from MoM playlists
 */
public interface MomAudioPlayer extends AudioPlayer
{
	/**
	 * 
	 * @param playListID Play list from the graphics XML file to play
	 * @param container Whether the playlist is defined in the graphics or common XML
	 * @throws JavaLayerException If there is a problem playing the audio file
	 * @throws RecordNotFoundException If the play list can't be found
	 */
	public void playPlayList (final String playListID, final AnimationContainer container) throws JavaLayerException, RecordNotFoundException;
}