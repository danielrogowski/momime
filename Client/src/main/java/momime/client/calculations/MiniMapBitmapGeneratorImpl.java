package momime.client.calculations;

import java.awt.image.BufferedImage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.TileTypeGfx;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MomTransientPlayerPublicKnowledge;

/**
 * Generates the bitmap for the mini overview map in the top right corner
 */
public final class MiniMapBitmapGeneratorImpl implements MiniMapBitmapGenerator
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (MiniMapBitmapGeneratorImpl.class);

	/** Multiplayer client */
	private MomClient client;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/**
	 * @param mapViewPlane Which plane to generate bitmap for
	 * @return Bitmap the same number of pixels in size as the map is large, i.e. 60x40 pixels for a standard size map
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the graphics DB
	 * @throws PlayerNotFoundException If we encounter a city owner that can't be found in the player list
	 */
	@Override
	public final BufferedImage generateMiniMapBitmap (final int mapViewPlane) throws RecordNotFoundException, PlayerNotFoundException
	{
		log.trace ("Entering generateMiniMapBitmap: " + mapViewPlane);
		
		final BufferedImage image = new BufferedImage (getClient ().getSessionDescription ().getOverlandMapSize ().getWidth (),
			getClient ().getSessionDescription ().getOverlandMapSize ().getHeight (), BufferedImage.TYPE_INT_ARGB);
		
		for (int x = 0; x < getClient ().getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
			for (int y = 0; y < getClient ().getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
			{
				final MemoryGridCell mc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
					(mapViewPlane).getRow ().get (y).getCell ().get (x);
				
				// Is there a city here?
				Integer colour = null;
				
				if (mc.getCityData () != null)
				{
					// Colour pixel according to city owner
					final PlayerPublicDetails player = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), mc.getCityData ().getCityOwnerID (), "generateMiniMapBitmap");
					final MomTransientPlayerPublicKnowledge trans = (MomTransientPlayerPublicKnowledge) player.getTransientPlayerPublicKnowledge ();
					colour = Integer.parseInt (trans.getFlagColour (), 16);
				}
				else if (mc.getTerrainData () != null)
				{
					// Colour pixel according to type of terrain
					final String tileTypeID = mc.getTerrainData ().getTileTypeID ();
					if (tileTypeID != null)
					{
						final TileTypeGfx tileType = getGraphicsDB ().findTileType (tileTypeID, "generateMiniMapBitmap");
						colour = tileType.findMiniMapColour (mapViewPlane);
					}
				}
				
				if (colour != null)
					image.setRGB (x, y, 0xFF000000 | colour);
			}
		
		log.trace ("Exiting generateMiniMapBitmap");
		return image;
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

	/**
	 * @return Session utils
	 */
	public final MultiplayerSessionUtils getMultiplayerSessionUtils ()
	{
		return multiplayerSessionUtils;
	}

	/**
	 * @param util Session utils
	 */
	public final void setMultiplayerSessionUtils (final MultiplayerSessionUtils util)
	{
		multiplayerSessionUtils = util;
	}
}