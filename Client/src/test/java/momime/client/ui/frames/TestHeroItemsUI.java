package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.anyString;

import java.awt.datatransfer.DataFlavor;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.HeroItemTypeGfx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.ui.draganddrop.TransferableFactory;
import momime.client.ui.draganddrop.TransferableHeroItem;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.ui.renderer.UnassignedHeroItemCellRenderer;
import momime.common.database.HeroItem;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;

/**
 * Tests the HeroItemsUI class
 */
public final class TestHeroItemsUI
{
	/**
	 * Tests the HeroItemsUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHeroItemsUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();

		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmHeroItems", "Title")).thenReturn ("Hero Items");
		when (lang.findCategoryEntry ("frmHeroItems", "Bank")).thenReturn ("Fortress Vault");
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Mock graphics
		final HeroItemTypeGfx itemType = new HeroItemTypeGfx ();

		for (int n = 1; n <= 4; n++)
			itemType.getHeroItemTypeImageFile ().add ("/momime.client.graphics/heroItems/items/sword-0" + n + ".png");
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.findHeroItemType (eq ("IT01"), anyString ())).thenReturn (itemType);
		
		// Unassigned items
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge (); 
		
		for (int n = 1; n <= 4; n++)
		{
			final HeroItem item = new HeroItem ();
			item.setHeroItemTypeID ("IT01");
			item.setHeroItemName ("Sword no. " + n);
			item.setHeroItemImageNumber (n - 1);
			
			priv.getUnassignedHeroItem ().add (item);
		}
		
		// Client
		final MomClient client = mock (MomClient.class);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		
		// Drag and drop flavours
		final DataFlavor heroItemFlavour = new DataFlavor (DataFlavor.javaJVMLocalObjectMimeType + ";class=" + HeroItem.class.getName ());
		
		final TransferableFactory transferableFactory = new TransferableFactory ()
		{
			@Override
			public final TransferableHeroItem createTransferableHeroItem ()
			{
				final TransferableHeroItem item = new TransferableHeroItem ();
				item.setHeroItemFlavour (heroItemFlavour);
				return item;
			}
		};
		
		// Layouts
		final XmlLayoutContainerEx cellLayout = (XmlLayoutContainerEx) ClientTestData.createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/HeroItemsUI-Unassigned.xml"));
		cellLayout.buildMaps ();
		
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) ClientTestData.createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/HeroItemsUI.xml"));
		layout.buildMaps ();

		// Bank items renderer
		final UnassignedHeroItemCellRenderer cellRenderer = new UnassignedHeroItemCellRenderer ();
		cellRenderer.setUnassignedHeroItemLayout (cellLayout);
		cellRenderer.setUtils (utils);
		cellRenderer.setGraphicsDB (gfx);
		cellRenderer.setMediumFont (CreateFontsForTests.getMediumFont ());
		
		// Set up form
		final HeroItemsUI items = new HeroItemsUI ();
		items.setHeroItemsLayout (layout);
		items.setUtils (utils);
		items.setLanguageHolder (langHolder);
		items.setLanguageChangeMaster (langMaster);
		items.setGraphicsDB (gfx);
		items.setSmallFont (CreateFontsForTests.getSmallFont ());
		items.setLargeFont (CreateFontsForTests.getLargeFont ());
		items.setClient (client);
		items.setUnassignedHeroItemCellRenderer (cellRenderer);
		items.setTransferableFactory (transferableFactory);
		items.setHeroItemFlavour (heroItemFlavour);
		
		// Display form		
		items.setVisible (true);
		Thread.sleep (50000);
		items.setVisible (false);
	}	
}