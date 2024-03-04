package momime.client.audio;

import java.util.ArrayList;
import java.util.List;

import com.ndg.audio.AudioPlayerImpl;

import javazoom.jl.decoder.JavaLayerException;
import momime.client.MomClient;
import momime.client.graphics.AnimationContainer;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.common.database.PlayList;
import momime.common.database.RecordNotFoundException;

/**
 * Extends vanilla auto player with ability to play from MoM playlists
 */
public final class MomAudioPlayerImpl extends AudioPlayerImpl implements MomAudioPlayer
{
	/** Multiplayer client */
	private MomClient client;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
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
}