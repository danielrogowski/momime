package momime.client.ui.frames;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.datatransfer.DataFlavor;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.database.ClientDatabaseEx;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.HeroItemSlotTypeGfx;
import momime.client.graphics.database.HeroItemTypeGfx;
import momime.client.graphics.database.UnitGfx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.ui.draganddrop.TransferableFactory;
import momime.client.ui.draganddrop.TransferableHeroItem;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.ui.renderer.HeroTableCellRenderer;
import momime.client.ui.renderer.UnassignedHeroItemCellRenderer;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItemSlot;
import momime.common.database.Unit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MemoryUnitHeroItemSlot;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.UnitStatusID;

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
		
		// Database
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		for (int n = 1; n <= 3; n++)
		{
			final Unit unitDef = new Unit ();
			unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
			
			for (int s = 0; s < 3; s++)
			{
				final HeroItemSlot slot = new HeroItemSlot ();
				slot.setHeroItemSlotTypeID ("IST0" + (n+s));
				unitDef.getHeroItemSlot ().add (slot);				
			}
			
			when (db.findUnit (eq ("UN00" + n), anyString ())).thenReturn (unitDef);
		}

		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmHeroItems", "Title")).thenReturn ("Hero Items");
		when (lang.findCategoryEntry ("frmHeroItems", "Bank")).thenReturn ("Fortress Vault");
		when (lang.findCategoryEntry ("frmHeroItems", "Alchemy")).thenReturn ("Alchemy");
		when (lang.findCategoryEntry ("frmHeroItems", "OK")).thenReturn ("OK");
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Mock graphics
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		
		final HeroItemTypeGfx itemType = new HeroItemTypeGfx ();
		for (int n = 1; n <= 4; n++)
			itemType.getHeroItemTypeImageFile ().add ("/momime.client.graphics/heroItems/items/sword-0" + n + ".png");
		
		when (gfx.findHeroItemType (eq ("IT01"), anyString ())).thenReturn (itemType);
		
		int slotTypeNumber = 0;
		for (final String slotImageFilename : new String [] {"melee", "bowOrMelee", "meleeOrMagic", "magic", "armour"})
		{
			slotTypeNumber++;
			
			final HeroItemSlotTypeGfx slot = new HeroItemSlotTypeGfx ();
			slot.setHeroItemSlotTypeImageFile ("/momime.client.graphics/heroItems/slots/" + slotImageFilename + ".png");
			when (gfx.findHeroItemSlotType ("IST0" + slotTypeNumber, "HeroTableCellRenderer")).thenReturn (slot);
		}
		
		// Hero portraits
		for (int n = 1; n <= 3; n++)
		{
			final UnitGfx unitGfx = new UnitGfx ();
			unitGfx.setHeroPortraitImageFile ("/momime.client.graphics/units/UN00" + n + "/portrait.png");
			when (gfx.findUnit ("UN00" + n, "HeroTableCellRenderer")).thenReturn (unitGfx);
		}
		
		// Unassigned items
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge (); 
		
		for (int n = 1; n <= 4; n++)
		{
			final NumberedHeroItem item = new NumberedHeroItem ();
			item.setHeroItemTypeID ("IT01");
			item.setHeroItemName ("Sword no. " + n);
			item.setHeroItemImageNumber (n - 1);
			
			priv.getUnassignedHeroItem ().add (item);
		}
		
		// Heroes
		final UnitClientUtils unitClientUtils = mock (UnitClientUtils.class);

		final FogOfWarMemory fow = new FogOfWarMemory ();
		priv.setFogOfWarMemory (fow);
		
		for (int n = 1; n <= 3; n++)
		{
			final MemoryUnit unit = new MemoryUnit ();
			unit.setUnitID ("UN00" + n);
			unit.setStatus (UnitStatusID.ALIVE);
			unit.setOwningPlayerID (1);
			fow.getUnit ().add (unit);

			// Hero name
			when (unitClientUtils.getUnitName (unit, UnitNameType.RACE_UNIT_NAME)).thenReturn ("Name of hero #" + n);
		}
		
		// Give one of them a real item (cheat by reusing one of the unassigned ones to save building another object)
		final MemoryUnitHeroItemSlot itemContainer = new MemoryUnitHeroItemSlot ();
		itemContainer.setHeroItem (priv.getUnassignedHeroItem ().get (0));
		fow.getUnit ().get (0).getHeroItemSlot ().add (itemContainer);
		
		// Client
		final MomClient client = mock (MomClient.class);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		when (client.getOurPlayerID ()).thenReturn (1);
		when (client.getClientDB ()).thenReturn (db);
		
		// Drag and drop flavours
		final DataFlavor heroItemFlavour = new DataFlavor (DataFlavor.javaJVMLocalObjectMimeType + ";class=" + NumberedHeroItem.class.getName ());
		
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

		final XmlLayoutContainerEx tableLayout = (XmlLayoutContainerEx) ClientTestData.createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/HeroItemsUI-Hero.xml"));
		tableLayout.buildMaps ();
		
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) ClientTestData.createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/HeroItemsUI.xml"));
		layout.buildMaps ();

		// Bank items renderer
		final UnassignedHeroItemCellRenderer cellRenderer = new UnassignedHeroItemCellRenderer ();
		cellRenderer.setUnassignedHeroItemLayout (cellLayout);
		cellRenderer.setUtils (utils);
		cellRenderer.setGraphicsDB (gfx);
		cellRenderer.setMediumFont (CreateFontsForTests.getMediumFont ());
		
		// Hero renderer
		final HeroTableCellRenderer tableRenderer = new HeroTableCellRenderer ();
		tableRenderer.setHeroLayout (tableLayout);
		tableRenderer.setUtils (utils);
		tableRenderer.setGraphicsDB (gfx);
		tableRenderer.setMediumFont (CreateFontsForTests.getMediumFont ());
		tableRenderer.setUnitClientUtils (unitClientUtils);
		tableRenderer.setClient (client);
		
		// Set up form
		final HeroItemsUI items = new HeroItemsUI ();
		items.setHeroItemsLayout (layout);
		items.setUtils (utils);
		items.setLanguageHolder (langHolder);
		items.setLanguageChangeMaster (langMaster);
		items.setGraphicsDB (gfx);
		items.setSmallFont (CreateFontsForTests.getSmallFont ());
		items.setMediumFont (CreateFontsForTests.getMediumFont ());
		items.setLargeFont (CreateFontsForTests.getLargeFont ());
		items.setClient (client);
		items.setUnassignedHeroItemCellRenderer (cellRenderer);
		items.setHeroTableCellRenderer (tableRenderer);
		items.setTransferableFactory (transferableFactory);
		items.setHeroItemFlavour (heroItemFlavour);
		
		// Display form		
		items.setVisible (true);
		Thread.sleep (50000);
		items.setVisible (false);
	}	
}