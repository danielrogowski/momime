package momime.client.ui.frames;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.datatransfer.DataFlavor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.HeroItemsScreen;
import momime.client.languages.database.Simple;
import momime.client.ui.draganddrop.TransferableFactory;
import momime.client.ui.draganddrop.TransferableHeroItem;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.ui.renderer.HeroTableCellRenderer;
import momime.client.ui.renderer.UnassignedHeroItemCellRenderer;
import momime.client.utils.TextUtilsImpl;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItemSlotType;
import momime.common.database.HeroItemType;
import momime.common.database.Language;
import momime.common.database.ProductionTypeEx;
import momime.common.database.UnitEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MemoryUnitHeroItemSlot;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.UnitStatusID;
import momime.common.utils.ResourceValueUtils;

/**
 * Tests the HeroItemsUI class
 */
@ExtendWith(MockitoExtension.class)
public final class TestHeroItemsUI extends ClientTestData
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
		
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final ProductionTypeEx gold = new ProductionTypeEx ();
		gold.getProductionTypeSuffix ().add (createLanguageText (Language.ENGLISH, "GP"));
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, "updateAmountStored")).thenReturn (gold);
		
		final ProductionTypeEx mana = new ProductionTypeEx ();
		mana.getProductionTypeSuffix ().add (createLanguageText (Language.ENGLISH, "MP"));
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, "updateAmountStored")).thenReturn (mana);
		
		for (int n = 1; n <= 3; n++)
		{
			final UnitEx unitDef = new UnitEx ();
			unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
			unitDef.setHeroPortraitImageFile ("/momime.client.graphics/units/UN00" + n + "/portrait.png");
			
			for (int s = 0; s < 3; s++)
				unitDef.getHeroItemSlot ().add ("IST0" + (n+s));				
			
			when (db.findUnit (eq ("UN00" + n), anyString ())).thenReturn (unitDef);
		}

		final HeroItemType itemType = new HeroItemType ();
		for (int n = 1; n <= 4; n++)
			itemType.getHeroItemTypeImageFile ().add ("/momime.client.graphics/heroItems/items/sword-0" + n + ".png");
		
		when (db.findHeroItemType (eq ("IT01"), anyString ())).thenReturn (itemType);
		
		int slotTypeNumber = 0;
		for (final String slotImageFilename : new String [] {"melee", "bowOrMelee", "meleeOrMagic", "magic", "armour"})
		{
			slotTypeNumber++;
			
			final HeroItemSlotType slot = new HeroItemSlotType ();
			slot.setHeroItemSlotTypeImageFile ("/momime.client.graphics/heroItems/slots/" + slotImageFilename + ".png");
			
			if (slotTypeNumber != 1)
				when (db.findHeroItemSlotType ("IST0" + slotTypeNumber, "HeroTableCellRenderer")).thenReturn (slot);
		}
		
		// Mock entries from the language XML
		final Simple simpleLang = new Simple ();
		simpleLang.getOk ().add (createLanguageText (Language.ENGLISH, "OK"));
		
		final HeroItemsScreen heroItemsScreenLang = new HeroItemsScreen ();
		heroItemsScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Hero Items"));
		heroItemsScreenLang.getBank ().add (createLanguageText (Language.ENGLISH, "Fortress Vault"));
		heroItemsScreenLang.getAlchemy ().add (createLanguageText (Language.ENGLISH, "Alchemy"));
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSimple ()).thenReturn (simpleLang);
		when (lang.getHeroItemsScreen ()).thenReturn (heroItemsScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Unassigned items
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge (); 
		
		for (int n = 1; n <= 4; n++)
		{
			final NumberedHeroItem item = new NumberedHeroItem ();
			item.setHeroItemTypeID ("IT01");
			item.setHeroItemName ("Sword no. " + n);
			item.setHeroItemImageNumber (n - 1);
			item.setHeroItemURN (n);
			
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
			unit.setUnitURN (n);
			fow.getUnit ().add (unit);
			
			for (int s = 0; s < 3; s++)
				unit.getHeroItemSlot ().add (new MemoryUnitHeroItemSlot ());

			// Hero name
			when (unitClientUtils.getUnitName (unit, UnitNameType.RACE_UNIT_NAME)).thenReturn ("Name of hero #" + n);
		}
		
		// Give one of them a real item (cheat by reusing one of the unassigned ones to save building another object)
		fow.getUnit ().get (0).getHeroItemSlot ().get (0).setHeroItem (priv.getUnassignedHeroItem ().get (0));
		
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
		final XmlLayoutContainerEx cellLayout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/HeroItemsUI-Unassigned.xml"));
		cellLayout.buildMaps ();

		final XmlLayoutContainerEx tableLayout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/HeroItemsUI-Hero.xml"));
		tableLayout.buildMaps ();
		
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/HeroItemsUI.xml"));
		layout.buildMaps ();

		// Bank items renderer
		final UnassignedHeroItemCellRenderer cellRenderer = new UnassignedHeroItemCellRenderer ();
		cellRenderer.setUnassignedHeroItemLayout (cellLayout);
		cellRenderer.setUtils (utils);
		cellRenderer.setClient (client);
		cellRenderer.setMediumFont (CreateFontsForTests.getMediumFont ());
		
		// Hero renderer
		final HeroTableCellRenderer tableRenderer = new HeroTableCellRenderer ();
		tableRenderer.setHeroLayout (tableLayout);
		tableRenderer.setUtils (utils);
		tableRenderer.setMediumFont (CreateFontsForTests.getMediumFont ());
		tableRenderer.setUnitClientUtils (unitClientUtils);
		tableRenderer.setClient (client);
		
		// Set up form
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		
		final HeroItemsUI items = new HeroItemsUI ();
		items.setHeroItemsLayout (layout);
		items.setUtils (utils);
		items.setLanguageHolder (langHolder);
		items.setLanguageChangeMaster (langMaster);
		items.setSmallFont (CreateFontsForTests.getSmallFont ());
		items.setMediumFont (CreateFontsForTests.getMediumFont ());
		items.setLargeFont (CreateFontsForTests.getLargeFont ());
		items.setClient (client);
		items.setUnassignedHeroItemCellRenderer (cellRenderer);
		items.setHeroTableCellRenderer (tableRenderer);
		items.setTransferableFactory (transferableFactory);
		items.setHeroItemFlavour (heroItemFlavour);
		items.setResourceValueUtils (resourceValueUtils);
		items.setTextUtils (new TextUtilsImpl ());
		
		// Display form		
		items.setVisible (true);
		Thread.sleep (5000);
		items.setVisible (false);
	}	
}