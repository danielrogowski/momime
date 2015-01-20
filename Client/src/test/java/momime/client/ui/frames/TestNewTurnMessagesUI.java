package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.newturnmessages.NewTurnMessageCategory;
import momime.client.newturnmessages.NewTurnMessagePopulationChangeEx;
import momime.client.newturnmessages.NewTurnMessageSortOrder;
import momime.client.newturnmessages.NewTurnMessageUI;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.AnimationController;
import momime.client.utils.TextUtilsImpl;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.OverlandMapCityData;

import org.junit.Test;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

/**
 * Tests the NewTurnMessagesUI class
 */
public final class TestNewTurnMessagesUI
{
	/**
	 * Tests the NewTurnMessagesUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testNewTurnMessagesUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();

		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("NewTurnMessages", "Title")).thenReturn ("Messages");
		when (lang.findCategoryEntry ("NewTurnMessages", "CityGrowthCategory")).thenReturn ("City Growth");
		when (lang.findCategoryEntry ("NewTurnMessages", "CityDeathCategory")).thenReturn ("City Death");
		when (lang.findCategoryEntry ("NewTurnMessages", "CityGrowth")).thenReturn ("CITY_NAME population has grown from OLD_POPULATION to NEW_POPULATION");
		when (lang.findCategoryEntry ("NewTurnMessages", "CityDeath")).thenReturn ("CITY_NAME population has dropped from OLD_POPULATION to NEW_POPULATION");

		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// City names
		final MapVolumeOfMemoryGridCells terrain = ClientTestData.createOverlandMap (ClientTestData.createOverlandMapCoordinateSystem ());
		
		final OverlandMapCityData city1 = new OverlandMapCityData ();
		city1.setCityName ("Foo");
		terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (20).setCityData (city1);

		final OverlandMapCityData city2 = new OverlandMapCityData ();
		city2.setCityName ("Bar");
		terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (21).setCityData (city2);

		final OverlandMapCityData city3 = new OverlandMapCityData ();
		city3.setCityName ("Pants");
		terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (22).setCityData (city3);

		final FogOfWarMemory fow = new FogOfWarMemory ();
		fow.setMap (terrain);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (fow);
		
		final MomClient client = mock (MomClient.class);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		
		// Dummy list of messages to display
		final TextUtilsImpl textUtils = new TextUtilsImpl ();
		
		final List<NewTurnMessageUI> msgs = new ArrayList<NewTurnMessageUI> ();
		
		final NewTurnMessageCategory cat1 = new NewTurnMessageCategory ();
		cat1.setLanguageHolder (langHolder);
		cat1.setLargeFont (CreateFontsForTests.getLargeFont ());
		cat1.setSortOrder (NewTurnMessageSortOrder.SORT_ORDER_CITY_GROWTH);
		msgs.add (cat1);
		
		final NewTurnMessagePopulationChangeEx msg1 = new NewTurnMessagePopulationChangeEx ();
		msg1.setLanguageHolder (langHolder);
		msg1.setSmallFont (CreateFontsForTests.getSmallFont ());
		msg1.setClient (client);
		msg1.setCityLocation (new MapCoordinates3DEx (20, 10, 0));
		msg1.setTextUtils (textUtils);
		msg1.setOldPopulation (4567);
		msg1.setNewPopulation (5678);
		msgs.add (msg1);
		
		final NewTurnMessagePopulationChangeEx msg2 = new NewTurnMessagePopulationChangeEx ();
		msg2.setLanguageHolder (langHolder);
		msg2.setSmallFont (CreateFontsForTests.getSmallFont ());
		msg2.setClient (client);
		msg2.setCityLocation (new MapCoordinates3DEx (21, 10, 0));
		msg2.setTextUtils (textUtils);
		msg2.setOldPopulation (14567);
		msg2.setNewPopulation (15678);
		msgs.add (msg2);
		
		final NewTurnMessageCategory cat2 = new NewTurnMessageCategory ();
		cat2.setLanguageHolder (langHolder);
		cat2.setLargeFont (CreateFontsForTests.getLargeFont ());
		cat2.setSortOrder (NewTurnMessageSortOrder.SORT_ORDER_CITY_DEATH);
		msgs.add (cat2);
		
		final NewTurnMessagePopulationChangeEx msg3 = new NewTurnMessagePopulationChangeEx ();
		msg3.setLanguageHolder (langHolder);
		msg3.setSmallFont (CreateFontsForTests.getSmallFont ());
		msg3.setClient (client);
		msg3.setCityLocation (new MapCoordinates3DEx (22, 10, 0));
		msg3.setTextUtils (textUtils);
		msg3.setOldPopulation (23100);
		msg3.setNewPopulation (22850);
		msgs.add (msg3);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) ClientTestData.createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/NewTurnMessagesUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final NewTurnMessagesUI scroll = new NewTurnMessagesUI ();
		scroll.setNewTurnMessagesLayout (layout);
		scroll.setUtils (utils);
		scroll.setLanguageHolder (langHolder);
		scroll.setLanguageChangeMaster (langMaster);
		scroll.setAnim (mock (AnimationController.class));
		scroll.setNewTurnMessages (msgs);
		
		// Display form		
		scroll.setVisible (true);
		Thread.sleep (5000);
		scroll.setVisible (false);
	}
}