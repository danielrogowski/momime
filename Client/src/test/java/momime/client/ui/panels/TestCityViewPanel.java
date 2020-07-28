package momime.client.ui.panels;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.junit.Test;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.graphics.database.CityViewElementGfx;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.utils.AnimationControllerImpl;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.OverlandMapSize;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.servertoclient.RenderCityData;

/**
 * Tests the CityViewPanel class
 */
public final class TestCityViewPanel extends ClientTestData
{
	/**
	 * Tests the CityViewPanel panel
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCityViewPanel () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the graphics XML
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		
		final CityViewElementGfx landscape = new CityViewElementGfx ();
		landscape.setLocationX (0);
		landscape.setLocationY (0);
		landscape.setCityViewImageFile ("/momime.client.graphics/cityView/landscape/arcanus.png");
		landscape.setSizeMultiplier (2);

		final CityViewElementGfx sky = new CityViewElementGfx ();
		sky.setLocationX (0);
		sky.setLocationY (0);
		sky.setCityViewImageFile ("/momime.client.graphics/cityView/sky/arcanus-hills.png");
		sky.setSizeMultiplier (2);
		sky.setTileTypeID ("TT02");

		final CityViewElementGfx summoningCircle = new CityViewElementGfx ();
		summoningCircle.setLocationX (122);
		summoningCircle.setLocationY (52);
		summoningCircle.setCityViewImageFile ("/momime.client.graphics/cityView/buildings/BL98.png");
		summoningCircle.setSizeMultiplier (1);
		summoningCircle.setBuildingID (CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE);
		
		final CityViewElementGfx fortress = new CityViewElementGfx ();
		fortress.setLocationX (202);
		fortress.setLocationY (67);
		fortress.setCityViewImageFile ("/momime.client.graphics/cityView/buildings/BL99-frame1.png");
		fortress.setSizeMultiplier (1);
		fortress.setBuildingID (CommonDatabaseConstants.BUILDING_FORTRESS);

		final CityViewElementGfx evil = new CityViewElementGfx ();
		evil.setLocationX (21);
		evil.setLocationY (31);
		evil.setCityViewImageFile ("/momime.client.graphics/cityView/spellEffects/SE183.png");
		evil.setSizeMultiplier (1);
		evil.setCitySpellEffectID ("SE183");
		
		final CityViewElementGfx altar = new CityViewElementGfx ();
		altar.setLocationX (293);
		altar.setLocationY (111);
		altar.setCityViewImageFile ("/momime.client.graphics/cityView/spellEffects/SE146-frame1.png");
		altar.setSizeMultiplier (1);
		altar.setCitySpellEffectID ("SE146");

		final CityViewElementGfx ocean = new CityViewElementGfx ();
		ocean.setLocationX (2);
		ocean.setLocationY (0);
		ocean.setCityViewImageFile ("/momime.client.graphics/cityView/water/arcanus-ocean-frame1.png");
		ocean.setSizeMultiplier (2);
		ocean.setPlaneNumber (0);
		ocean.setCityViewElementSetID ("X");
		
		final CityViewElementGfx river = new CityViewElementGfx ();
		river.setLocationX (70);
		river.setLocationY (0);
		river.setCityViewImageFile ("/momime.client.graphics/cityView/water/arcanus-river-frame1.png");
		river.setSizeMultiplier (2);
		river.setPlaneNumber (1);
		
		final CityViewElementGfx setElement = new CityViewElementGfx ();
		setElement.setLocationX (140);
		setElement.setLocationY (0);
		setElement.setCityViewImageFile ("/momime.client.graphics/cityView/water/myrror-ocean-frame1.png");
		setElement.setSizeMultiplier (2);
		setElement.setPlaneNumber (0);
		setElement.setCityViewElementSetID ("X");		// Matches criteria, but we matched an earlier "X" already so this doesn't get displayed
		
		final List<CityViewElementGfx> elements = new ArrayList<CityViewElementGfx> ();
		elements.add (landscape);
		elements.add (sky);
		elements.add (summoningCircle);
		elements.add (fortress);
		elements.add (evil);
		elements.add (altar);
		elements.add (ocean);
		elements.add (river);
		elements.add (setElement);
		when (gfx.getCityViewElements ()).thenReturn (elements);
		
		// Mock what is in this city
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		
		final MapVolumeOfMemoryGridCells terrain = createOverlandMap (overlandMapSize);

		final FogOfWarMemory fow = new FogOfWarMemory ();
		fow.setMap (terrain);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (fow);
		
		final MomClient client = mock (MomClient.class);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		when (client.getSessionDescription ()).thenReturn (sd);
		
		// City data
		final RenderCityData renderCityData = new RenderCityData ();
		renderCityData.getAdjacentTileTypeID ().add ("TT02");
		renderCityData.getBuildingID ().add (CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE);
		renderCityData.getBuildingID ().add (CommonDatabaseConstants.BUILDING_FORTRESS);
		renderCityData.getCitySpellEffectID ().add ("SE146");
		renderCityData.getCitySpellEffectID ().add ("SE183");
		
		// Animation controller
		final AnimationControllerImpl anim = new AnimationControllerImpl ();
		anim.setGraphicsDB (gfx);
		anim.setUtils (utils);
		
		// Set up panel
		final CityViewPanel panel = new CityViewPanel ();
		panel.setUtils (utils);
		panel.setGraphicsDB (gfx);
		panel.setClient (client);
		panel.setCityLocation (new MapCoordinates3DEx (20, 10, 0));
		panel.setRenderCityData (renderCityData);
		panel.setAnim (anim);
		panel.init ();
		
		// For testing, dump clicks out to the console
		panel.addBuildingListener (new BuildingListener ()
		{
			@Override
			public final void buildingClicked (final String buildingID)
			{
				System.out.println ("Building " + buildingID + " was clicked on");
			}
		});
		
		// Set up a dummy frame to display the panel
		final JFrame frame = new JFrame ("testCityViewPanel");
		frame.setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		frame.setContentPane (panel);
		frame.pack ();
		frame.setLocationRelativeTo (null);

		frame.setVisible (true);
		Thread.sleep (5000);
		frame.setVisible (false);
	}
}