package momime.client.calculations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.TileTypeGfx;
import momime.client.graphics.database.TileTypeMiniMapGfx;
import momime.common.database.OverlandMapSize;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;

/**
 * Tests the MiniMapBitmapGeneratorImpl class
 */
public final class TestMiniMapBitmapGeneratorImpl
{
	/**
	 * Tests the generateMiniMapBitmap method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateMiniMapBitmap () throws Exception
	{
		// Mock graphics database
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		
		for (int n = 1; n <= 4; n++)
		{
			final TileTypeGfx tileType = new TileTypeGfx ();
			when (gfx.findTileType ("TT0" + n, "generateMiniMapBitmap")).thenReturn (tileType);

			if (n < 4)
			{
				final TileTypeMiniMapGfx miniMap = new TileTypeMiniMapGfx ();
				miniMap.setPlaneNumber (0);
				miniMap.setMiniMapPixelColour ("00000" + n);
				tileType.getTileTypeMiniMap ().add (miniMap);
			}
			
			tileType.buildMap ();
		}
		
		// Map size
		final OverlandMapSize mapSize = new OverlandMapSize ();
		mapSize.setWidth (3);
		mapSize.setHeight (2);
		mapSize.setDepth (2);
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (mapSize);
		
		// Terrain
		final MapVolumeOfMemoryGridCells terrain = ClientTestData.createOverlandMap (mapSize);
		
		final FogOfWarMemory fow = new FogOfWarMemory ();
		fow.setMap (terrain);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (fow);
		
		// First row are tile types 1, 2, 3
		// Second row has a city, a tile we've seen but has no colour defined, and a tile we've not seen at all
		for (int x = 0; x < mapSize.getWidth (); x++)
		{
			// First row
			final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
			terrainData.setTileTypeID ("TT0" + (x+1));
			terrain.getPlane ().get (0).getRow ().get (0).getCell ().get (x).setTerrainData (terrainData);
			
			// Second row
			if (x < 2)
			{
				final OverlandMapTerrainData terrainData2 = new OverlandMapTerrainData ();
				terrainData2.setTileTypeID ("TT0" + (x+3));
				terrain.getPlane ().get (0).getRow ().get (1).getCell ().get (x).setTerrainData (terrainData2);
			}
		}
		
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (1);
		terrain.getPlane ().get (0).getRow ().get (1).getCell ().get (0).setCityData (cityData);
		
		// City owner
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final MomTransientPlayerPublicKnowledge trans = new MomTransientPlayerPublicKnowledge ();
		trans.setFlagColour ("000004");
		
		final PlayerPublicDetails player = new PlayerPublicDetails (null, null, trans);
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, 1, "generateMiniMapBitmap")).thenReturn (player);
		
		// Client
		final MomClient client = mock (MomClient.class);
		when (client.getSessionDescription ()).thenReturn (sd);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		when (client.getPlayers ()).thenReturn (players);
		
		// Set up object to test
		final MiniMapBitmapGeneratorImpl gen = new MiniMapBitmapGeneratorImpl ();
		gen.setClient (client);
		gen.setGraphicsDB (gfx);
		gen.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		// Run method
		final BufferedImage image = gen.generateMiniMapBitmap (0);
		
		// Check results
		assertEquals (mapSize.getWidth (), image.getWidth ());
		assertEquals (mapSize.getHeight (), image.getHeight ());
		
		assertEquals (-0xFFFFFF, image.getRGB (0, 0));
		assertEquals (-0xFFFFFE, image.getRGB (1, 0));
		assertEquals (-0xFFFFFD, image.getRGB (2, 0));
		assertEquals (-0xFFFFFC, image.getRGB (0, 1));
		assertEquals (0, image.getRGB (1, 1));
		assertEquals (0, image.getRGB (2, 1));
	}
}