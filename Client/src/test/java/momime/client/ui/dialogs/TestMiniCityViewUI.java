package momime.client.ui.dialogs;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.graphics.database.CityViewElementGfx;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.SpellGfx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.CitySpellEffectLang;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.messages.process.AddMaintainedSpellMessageImpl;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.ui.panels.CityViewPanel;
import momime.client.utils.AnimationControllerImpl;
import momime.client.utils.WizardClientUtils;
import momime.common.database.OverlandMapSize;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.servertoclient.RenderCityData;

/**
 * Tests the MiniCityViewUI class
 */
public final class TestMiniCityViewUI extends ClientTestData
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
		final SpellGfx spellGfx = new SpellGfx ();
		spellGfx.setSoundAndImageDelay (2);
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.findSpell ("SP001", "MiniCityViewUI")).thenReturn (spellGfx);
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCitySizeName ("CS01", true)).thenReturn ("PLAYER_NAME's Test City of CITY_NAME");
		when (lang.findCategoryEntry ("SpellCasting", "YouHaveCast")).thenReturn ("You have cast SPELL_NAME");
		
		final CitySpellEffectLang effectLang = new CitySpellEffectLang ();
		effectLang.setCitySpellEffectName ("Dark Rituals");
		when (lang.findCitySpellEffect ("CSE001")).thenReturn (effectLang);

		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// City data
		final RenderCityData renderCityData = new RenderCityData ();
		renderCityData.setCitySizeID ("CS01");
		renderCityData.setCityName ("Blahdy Blah");
		renderCityData.setCityOwnerID (2);
		
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
		
		// Player
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		final WizardClientUtils wizardClientUtils = mock (WizardClientUtils.class);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();

		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);
		
		final PlayerPublicDetails player1 = new PlayerPublicDetails (pd1, null, null);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd1.getPlayerID ())).thenReturn (player1);
		
		final PlayerPublicDetails player2 = new PlayerPublicDetails (null, null, null);
		when (multiplayerSessionUtils.findPlayerWithID (players, 2)).thenReturn (player2);
		when (wizardClientUtils.getPlayerName (player2)).thenReturn ("Jafar");

		when (client.getOurPlayerID ()).thenReturn (pd1.getPlayerID ());
		
		// Display at least some landscape, plus the spell itself
		final CityViewElementGfx landscape = new CityViewElementGfx ();
		landscape.setLocationX (0);
		landscape.setLocationY (0);
		landscape.setSizeMultiplier (2);
		landscape.setCityViewImageFile ("/momime.client.graphics/cityView/landscape/arcanus.png");
		
		final CityViewElementGfx spellImage = new CityViewElementGfx ();
		spellImage.setLocationX (100);
		spellImage.setLocationY (100);
		spellImage.setSizeMultiplier (2);
		spellImage.setCityViewImageFile ("/momime.client.graphics/cityView/spellEffects/SE163-frame1.png");
		spellImage.setCitySpellEffectID ("CSE001");
		
		final List<CityViewElementGfx> elements = new ArrayList<CityViewElementGfx> ();
		elements.add (landscape);
		elements.add (spellImage);
		when (gfx.getCityViewElements ()).thenReturn (elements);
		
		// Set up spell
		final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
		spell.setSpellID ("SP001");
		spell.setCitySpellEffectID ("CSE001");
		spell.setCityLocation (new MapCoordinates3DEx (20, 10, 0));
		spell.setCastingPlayerID (pd1.getPlayerID ());
		
		final AddMaintainedSpellMessageImpl msg = new AddMaintainedSpellMessageImpl (); 
		msg.setMaintainedSpell (spell);
		
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

		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.dialogs/MiniCityViewUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final MiniCityViewUI cityView = new MiniCityViewUI ();
		cityView.setUtils (utils);
		cityView.setClient (client);
		cityView.setLanguageHolder (langHolder);
		cityView.setLanguageChangeMaster (langMaster);
		cityView.setMultiplayerSessionUtils (multiplayerSessionUtils);
		cityView.setWizardClientUtils (wizardClientUtils);
		cityView.setGraphicsDB (gfx);
		cityView.setCityLocation (new MapCoordinates3DEx (20, 10, 0));
		cityView.setCityViewPanel (panel);
		cityView.setLargeFont (CreateFontsForTests.getLargeFont ());
		cityView.setMiniCityViewLayout (layout);
		cityView.setAddSpellMessage (msg);
		cityView.setRenderCityData (renderCityData);
	
		// Display form
		cityView.setModal (false);
		cityView.setVisible (true);
		Thread.sleep (5000);
		cityView.setVisible (false);
	}
}