package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.NewTurnMessages;
import momime.client.newturnmessages.NewTurnMessageCategory;
import momime.client.newturnmessages.NewTurnMessageOfferItemEx;
import momime.client.newturnmessages.NewTurnMessagePopulationChangeEx;
import momime.client.newturnmessages.NewTurnMessageSortOrder;
import momime.client.newturnmessages.NewTurnMessageUI;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.ui.renderer.NewTurnMessageRenderer;
import momime.client.utils.AnimationController;
import momime.client.utils.TextUtilsImpl;
import momime.common.database.CommonDatabase;
import momime.common.database.HeroItemType;
import momime.common.database.Language;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.OverlandMapCityData;

/**
 * Tests the NewTurnMessagesUI class
 */
public final class TestNewTurnMessagesUI extends ClientTestData
{
	/**
	 * Tests the NewTurnMessagesUI form with a small number of messages
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testNewTurnMessagesUI_Small () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();

		// Mock entries from the language XML
		final NewTurnMessages newTurnMessagesLang = new NewTurnMessages ();
		newTurnMessagesLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Messages"));
		newTurnMessagesLang.getCityGrowthCategory ().add (createLanguageText (Language.ENGLISH, "City Growth"));
		newTurnMessagesLang.getCityDeathCategory ().add (createLanguageText (Language.ENGLISH, "City Death"));
		newTurnMessagesLang.getCityGrowth ().add (createLanguageText (Language.ENGLISH, "CITY_NAME population has grown from OLD_POPULATION to NEW_POPULATION"));
		newTurnMessagesLang.getCityDeath ().add (createLanguageText (Language.ENGLISH,"CITY_NAME population has dropped from OLD_POPULATION to NEW_POPULATION"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getNewTurnMessages ()).thenReturn (newTurnMessagesLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// City names
		final MapVolumeOfMemoryGridCells terrain = createOverlandMap (createOverlandMapCoordinateSystem ());
		
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
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/NewTurnMessagesUI.xml"));
		layout.buildMaps ();

		// Renderer
		final NewTurnMessageRenderer renderer = new NewTurnMessageRenderer ();
		renderer.setUtils (utils);
		
		// Set up form
		final NewTurnMessagesUI scroll = new NewTurnMessagesUI ();
		scroll.setNewTurnMessagesLayout (layout);
		scroll.setUtils (utils);
		scroll.setLanguageHolder (langHolder);
		scroll.setLanguageChangeMaster (langMaster);
		scroll.setAnim (mock (AnimationController.class));
		scroll.setNewTurnMessages (msgs);
		scroll.setNewTurnMessageRenderer (renderer);
		
		// Display form		
		scroll.setVisible (true);
		Thread.sleep (5000);
		scroll.setVisible (false);
	}

	/**
	 * Tests the NewTurnMessagesUI form with a large number of messages, enough to make it scroll vertically
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testNewTurnMessagesUI_Large () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();

		// Mock entries from the language XML
		final NewTurnMessages newTurnMessagesLang = new NewTurnMessages ();
		newTurnMessagesLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Messages"));
		newTurnMessagesLang.getCityGrowthCategory ().add (createLanguageText (Language.ENGLISH, "City Growth"));
		newTurnMessagesLang.getCityDeathCategory ().add (createLanguageText (Language.ENGLISH, "City Death"));
		newTurnMessagesLang.getCityGrowth ().add (createLanguageText (Language.ENGLISH, "CITY_NAME population has grown from OLD_POPULATION to NEW_POPULATION"));
		newTurnMessagesLang.getCityDeath ().add (createLanguageText (Language.ENGLISH,"CITY_NAME population has dropped from OLD_POPULATION to NEW_POPULATION"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getNewTurnMessages ()).thenReturn (newTurnMessagesLang);

		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// City names
		final MapVolumeOfMemoryGridCells terrain = createOverlandMap (createOverlandMapCoordinateSystem ());
		
		for (int x = 1; x <= 20; x++)
		{
			final OverlandMapCityData city1 = new OverlandMapCityData ();
			city1.setCityName ("City #" + x);
			terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (x).setCityData (city1);

			final OverlandMapCityData city2 = new OverlandMapCityData ();
			city2.setCityName ("City #" + (20+x));
			terrain.getPlane ().get (0).getRow ().get (11).getCell ().get (x).setCityData (city2);
		}

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
		
		for (int x = 1; x <= 20; x++)
		{
			final NewTurnMessagePopulationChangeEx msg = new NewTurnMessagePopulationChangeEx ();
			msg.setLanguageHolder (langHolder);
			msg.setSmallFont (CreateFontsForTests.getSmallFont ());
			msg.setClient (client);
			msg.setCityLocation (new MapCoordinates3DEx (x, 10, 0));
			msg.setTextUtils (textUtils);
			msg.setOldPopulation (4567);
			msg.setNewPopulation (5678);
			msgs.add (msg);
		}
		
		final NewTurnMessageCategory cat2 = new NewTurnMessageCategory ();
		cat2.setLanguageHolder (langHolder);
		cat2.setLargeFont (CreateFontsForTests.getLargeFont ());
		cat2.setSortOrder (NewTurnMessageSortOrder.SORT_ORDER_CITY_DEATH);
		msgs.add (cat2);
		
		for (int x = 1; x <= 20; x++)
		{
			final NewTurnMessagePopulationChangeEx msg = new NewTurnMessagePopulationChangeEx ();
			msg.setLanguageHolder (langHolder);
			msg.setSmallFont (CreateFontsForTests.getSmallFont ());
			msg.setClient (client);
			msg.setCityLocation (new MapCoordinates3DEx (x, 11, 0));
			msg.setTextUtils (textUtils);
			msg.setOldPopulation (23100);
			msg.setNewPopulation (22850);
			msgs.add (msg);
		}
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/NewTurnMessagesUI.xml"));
		layout.buildMaps ();
		
		// Renderer
		final NewTurnMessageRenderer renderer = new NewTurnMessageRenderer ();
		renderer.setUtils (utils);
		
		// Set up form
		final NewTurnMessagesUI scroll = new NewTurnMessagesUI ();
		scroll.setNewTurnMessagesLayout (layout);
		scroll.setUtils (utils);
		scroll.setLanguageHolder (langHolder);
		scroll.setLanguageChangeMaster (langMaster);
		scroll.setAnim (mock (AnimationController.class));
		scroll.setNewTurnMessages (msgs);
		scroll.setNewTurnMessageRenderer (renderer);
		
		// Display form		
		scroll.setVisible (true);
		Thread.sleep (5000);
		scroll.setVisible (false);
	}

	/**
	 * Tests the NewTurnMessagesUI form with a mixture of types of messages, some wide enough to require text wrapping and some with images
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testNewTurnMessagesUI_Mixed () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();

		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final HeroItemType itemType = new HeroItemType ();
		itemType.getHeroItemTypeImageFile ().add ("/momime.client.graphics/heroItems/items/sword-01.png");
		itemType.getHeroItemTypeImageFile ().add ("/momime.client.graphics/heroItems/items/sword-05.png");
		when (db.findHeroItemType ("IT01", "NewTurnMessageOfferItemEx")).thenReturn (itemType);
		
		// Mock entries from the language XML
		final NewTurnMessages newTurnMessagesLang = new NewTurnMessages ();
		newTurnMessagesLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Messages"));
		newTurnMessagesLang.getCityGrowthCategory ().add (createLanguageText (Language.ENGLISH, "City Growth"));
		newTurnMessagesLang.getOffersCategory ().add (createLanguageText (Language.ENGLISH, "Offers"));
		newTurnMessagesLang.getCityGrowth ().add (createLanguageText (Language.ENGLISH, "CITY_NAME population has grown from OLD_POPULATION to NEW_POPULATION"));
		newTurnMessagesLang.getOfferItem ().add (createLanguageText (Language.ENGLISH, "A merchant offers to sell you ITEM_NAME for COST gold"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getNewTurnMessages ()).thenReturn (newTurnMessagesLang);

		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// City names
		final MapVolumeOfMemoryGridCells terrain = createOverlandMap (createOverlandMapCoordinateSystem ());
		
		final OverlandMapCityData city1 = new OverlandMapCityData ();
		city1.setCityName ("Normal city");
		terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (0).setCityData (city1);

		final OverlandMapCityData city2 = new OverlandMapCityData ();
		city2.setCityName ("City with a ridiculously long name so it has to wrap over multiple lines to fit on the scroll");
		terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (1).setCityData (city2);

		final FogOfWarMemory fow = new FogOfWarMemory ();
		fow.setMap (terrain);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (fow);
		
		final MomClient client = mock (MomClient.class);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		when (client.getClientDB ()).thenReturn (db);
		
		// Items offered
		final NumberedHeroItem item1 = new NumberedHeroItem ();
		item1.setHeroItemTypeID ("IT01");
		item1.setHeroItemImageNumber (0);
		item1.setHeroItemName ("A boring sword");

		final NumberedHeroItem item2 = new NumberedHeroItem ();
		item2.setHeroItemTypeID ("IT01");
		item2.setHeroItemImageNumber (1);
		item2.setHeroItemName ("An awesome sword with a super long name so we get a text that wraps over multiple lines with an icon too");

		// Dummy list of messages to display
		final TextUtilsImpl textUtils = new TextUtilsImpl ();
		
		final List<NewTurnMessageUI> msgs = new ArrayList<NewTurnMessageUI> ();
		
		final NewTurnMessageCategory cat1 = new NewTurnMessageCategory ();
		cat1.setLanguageHolder (langHolder);
		cat1.setLargeFont (CreateFontsForTests.getLargeFont ());
		cat1.setSortOrder (NewTurnMessageSortOrder.SORT_ORDER_CITY_GROWTH);
		msgs.add (cat1);
		
		for (int x = 0; x <= 1; x++)
		{
			final NewTurnMessagePopulationChangeEx msg = new NewTurnMessagePopulationChangeEx ();
			msg.setLanguageHolder (langHolder);
			msg.setSmallFont (CreateFontsForTests.getSmallFont ());
			msg.setClient (client);
			msg.setCityLocation (new MapCoordinates3DEx (x, 10, 0));
			msg.setTextUtils (textUtils);
			msg.setOldPopulation (4567);
			msg.setNewPopulation (5678);
			msgs.add (msg);
		}

		final NewTurnMessageCategory cat2 = new NewTurnMessageCategory ();
		cat2.setLanguageHolder (langHolder);
		cat2.setLargeFont (CreateFontsForTests.getLargeFont ());
		cat2.setSortOrder (NewTurnMessageSortOrder.SORT_ORDER_OFFERS);
		msgs.add (cat2);

		for (final NumberedHeroItem item : new NumberedHeroItem [] {item1, item2})
		{
			final NewTurnMessageOfferItemEx msg = new NewTurnMessageOfferItemEx ();
			msg.setLanguageHolder (langHolder);
			msg.setSmallFont (CreateFontsForTests.getSmallFont ());
			msg.setClient (client);
			msg.setTextUtils (textUtils);
			msg.setUtils (utils);
			msg.setHeroItem (item);
			msgs.add (msg);
		}
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/NewTurnMessagesUI.xml"));
		layout.buildMaps ();
		
		// Renderer
		final NewTurnMessageRenderer renderer = new NewTurnMessageRenderer ();
		renderer.setUtils (utils);
		
		// Set up form
		final NewTurnMessagesUI scroll = new NewTurnMessagesUI ();
		scroll.setNewTurnMessagesLayout (layout);
		scroll.setUtils (utils);
		scroll.setLanguageHolder (langHolder);
		scroll.setLanguageChangeMaster (langMaster);
		scroll.setAnim (mock (AnimationController.class));
		scroll.setNewTurnMessages (msgs);
		scroll.setNewTurnMessageRenderer (renderer);
		
		// Display form		
		scroll.setVisible (true);
		Thread.sleep (5000);
		scroll.setVisible (false);
	}
}