package momime.client.ui.dialogs;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.v0_9_5.CityViewElement;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.v0_9_5.Spell;
import momime.client.messages.process.AddMaintainedSpellMessageImpl;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.ui.panels.CityViewPanel;
import momime.client.utils.AnimationControllerImpl;
import momime.common.database.newgame.MapSizeData;
import momime.common.messages.servertoclient.AddMaintainedSpellMessageData;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.utils.MemoryMaintainedSpellUtilsImpl;

import org.junit.Test;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

/**
 * Tests the MiniCityViewUI class
 */
public final class TestMiniCityViewUI
{
	/**
	 * Tests the TestMiniCityViewUI form from casting a spell
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testMiniCityViewUI_Spell () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();

		// Mock entries from the graphics XML
		final momime.client.graphics.database.v0_9_5.Spell spellGfx = new momime.client.graphics.database.v0_9_5.Spell ();
		spellGfx.setSoundAndImageDelay (2);
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.findSpell ("SP001", "MiniCityViewUI")).thenReturn (spellGfx);
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCitySizeName ("CS01")).thenReturn ("Test City of CITY_NAME");
		when (lang.findCategoryEntry ("SpellCasting", "YouHaveCast")).thenReturn ("You have cast SPELL_NAME");
		
		final Spell spellLang = new Spell ();
		spellLang.setSpellName ("Dark Rituals");
		when (lang.findSpell ("SP001")).thenReturn (spellLang);

		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// City data
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCitySizeID ("CS01");
		cityData.setCityName ("Blahdy Blah");
		
		final MapSizeData mapSize = ClientTestData.createMapSizeData ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setMapSize (mapSize);
		
		final MapVolumeOfMemoryGridCells terrain = ClientTestData.createOverlandMap (mapSize);
		final MemoryGridCell mc = terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (20);
		mc.setCityData (cityData);
		
		final FogOfWarMemory fow = new FogOfWarMemory ();
		fow.setMap (terrain);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (fow);
		
		final MomClient client = mock (MomClient.class);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		
		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final PlayerPublicDetails player = new PlayerPublicDetails (pd, null, null);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player);
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd.getPlayerID ())).thenReturn (player);
		
		when (client.getOurPlayerID ()).thenReturn (pd.getPlayerID ());
		
		// Display at least some landscape, plus the spell itself
		final CityViewElement landscape = new CityViewElement ();
		landscape.setLocationX (0);
		landscape.setLocationY (0);
		landscape.setSizeMultiplier (2);
		landscape.setCityViewImageFile ("/momime.client.graphics/cityView/landscape/arcanus.png");
		
		final CityViewElement spellImage = new CityViewElement ();
		spellImage.setLocationX (100);
		spellImage.setLocationY (100);
		spellImage.setSizeMultiplier (2);
		spellImage.setCityViewImageFile ("/momime.client.graphics/cityView/spellEffects/SE163-frame1.png");
		spellImage.setCitySpellEffectID ("CSE001");
		
		final List<CityViewElement> elements = new ArrayList<CityViewElement> ();
		elements.add (landscape);
		elements.add (spellImage);
		when (gfx.getCityViewElement ()).thenReturn (elements);
		
		// Set up spell
		final AddMaintainedSpellMessageData spell = new AddMaintainedSpellMessageData ();
		spell.setSpellID ("SP001");
		spell.setCitySpellEffectID ("CSE001");
		spell.setCityLocation (new MapCoordinates3DEx (20, 10, 0));
		spell.setCastingPlayerID (pd.getPlayerID ());
		
		final AddMaintainedSpellMessageImpl msg = new AddMaintainedSpellMessageImpl (); 
		msg.setData (spell);

		// Set up animation controller
		final AnimationControllerImpl anim = new AnimationControllerImpl ();
		anim.setGraphicsDB (gfx);
		anim.setUtils (utils);
		
		// Set up terrain panel
		final CityViewPanel panel = new CityViewPanel ();
		panel.setClient (client);
		panel.setUtils (utils);
		panel.setGraphicsDB (gfx);
		panel.setAnim (anim);
		panel.setMemoryMaintainedSpellUtils (new MemoryMaintainedSpellUtilsImpl ());		// Since we need it to really look for the spell

		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) ClientTestData.createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.dialogs/MiniCityViewUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final MiniCityViewUI cityView = new MiniCityViewUI ();
		cityView.setUtils (utils);
		cityView.setClient (client);
		cityView.setLanguageHolder (langHolder);
		cityView.setLanguageChangeMaster (langMaster);
		cityView.setMultiplayerSessionUtils (multiplayerSessionUtils);
		cityView.setGraphicsDB (gfx);
		cityView.setCityLocation (new MapCoordinates3DEx (20, 10, 0));
		cityView.setCityViewPanel (panel);
		cityView.setLargeFont (CreateFontsForTests.getLargeFont ());
		cityView.setMiniCityViewLayout (layout);
		cityView.setSpellMessage (msg);
	
		// Display form
		cityView.setModal (false);
		cityView.setVisible (true);
		Thread.sleep (5000);
	}
}