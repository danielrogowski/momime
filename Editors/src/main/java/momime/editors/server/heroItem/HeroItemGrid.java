package momime.editors.server.heroItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFileChooser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;

import com.ndg.archive.LbxArchiveReader;
import com.ndg.utils.StreamUtils;
import com.ndg.utils.StringUtils;
import com.ndg.utils.swing.filefilters.SpecificFilenameFilter;
import com.ndg.xmleditor.doc.ComplexTypeReference;
import com.ndg.xmleditor.editor.XmlEditorException;
import com.ndg.xmleditor.jdom.JdomUtils;

import momime.common.database.CommonDatabaseConstants;
import momime.editors.grid.MoMEditorGridWithImport;
import momime.editors.server.ServerEditorDatabaseConstants;

/**
 * Allows importing the data for the 250 predefined hero items
 */
public final class HeroItemGrid extends MoMEditorGridWithImport
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (HeroItemGrid.class);
	
	/** If we generate this in the skill list, then don't output the item at all - this is to filter out the 2 items with the deactivated merging skill */
	private final static String DONT_OUTPUT_ITEM = "X";
	
	/** Item bonus IDs for all the bit flags - the 3 nulls I assume are Lionheart, Pathfinding and Invisibility, but no predefined items have these skills */
	private final static String [] ITEM_BONUS_ID = new String []
		{"IB38", "IB48", "IB35", "IB53", "IB37", "IB52", "IB61", null,
		"IB60", "IB43", "IB44", "IB36", "IB46", "IB57", "IB49", null,
		"IB39", "IB63", "IB50", "IB51", "IB34", "IB41", "IB42", "IB47",
		"IB40", "IB54", null, "IB45", "IB58", DONT_OUTPUT_ITEM, "IB55", "IB56"};
	
	/**
	 * Specify the filename we're looking for
	 * @param lbxChooser The file open dialog
	 */
	@Override
	protected final void addOtherFilters (final JFileChooser lbxChooser)
	{
		lbxChooser.addChoosableFileFilter (new SpecificFilenameFilter ("ITEMDATA.LBX", "Predefined hero items (ITEMDATA.LBX)"));
	};

	/**
	 * Imports hero item bonus data from ITEMPOW.LBX
	 * @param lbxFilename The lbx filename chosen in the file open dialog
	 * @throws IOException If there is a problem reading the LBX file
	 * @throws XmlEditorException If there is a problem with one of the XML editor helper methods
	 */
	@Override
	protected final void importFromLbx (final File lbxFilename)
		throws IOException, XmlEditorException
	{
		// ITEMPOW.LBX only has a single subfile in it
		try (final InputStream lbxStream = new FileInputStream (lbxFilename))
		{
			LbxArchiveReader.positionToSubFile (lbxStream, 0);
			
			// Read number and size of records
			final int numberOfRecords = StreamUtils.readUnsigned2ByteIntFromStream (lbxStream, ByteOrder.LITTLE_ENDIAN, "Number of Records");
			final int recordSize = StreamUtils.readUnsigned2ByteIntFromStream (lbxStream, ByteOrder.LITTLE_ENDIAN, "Record Size");

			// Read each record
			for (int recordNo = 0; recordNo < numberOfRecords; recordNo++)
			{
				final byte [] buf = new byte [recordSize];
				if (lbxStream.read (buf) != recordSize)
					throw new IOException ("Ran out of data on record " + recordNo + " of " + numberOfRecords);
				
				// Parse description
				String itemDescription = new String (Arrays.copyOf (buf, 30));
				final int nullPos = itemDescription.indexOf ('\u0000');
				if (nullPos >= 0)
					itemDescription = itemDescription.substring (0, nullPos);

				// Parse data
				// Bytes 31, 45
				// Byte 32 indicates 1=Melee, 2=Bow, 3=Magic, 4=Armour, 5=Misc but this is superceeded by the more detailed itemType byte at 33
				// Bytes 50..54 are supposed to be the number of nature, sorcery, chaos, life, death books that a wizard must have in order
				//		to acquire this item from a node - unfortunately ITEMMAKE.EXE is bugged and sets these values incorrectly, hence the original
				//		MoM has a bizzare mix of item requirements not matching the enchantments on the items.  So I'm just going to completely
				//		ignore these flags and derive the book requirements for items on the fly.  We don't save the cost + derive that on the fly too.
				// Byte 55 is 0 or 1 and no idea what this is for
				int imageNumber = buf [30] < 0 ? buf [30] + 256 : buf [30];
				final int itemTypeNumber = buf [33] < 0 ? buf [33] + 256 : buf [33];
				final int craftingCost = ((buf [34] < 0) ? buf [34] + 256 : buf [34]) + (((buf [35] < 0) ? buf [35] + 256 : buf [35]) << 8);
				
				final int attackBonus = buf [36] < 0 ? buf [36] + 256 : buf [36];
				final int toHitBonus = buf [37] < 0 ? buf [37] + 256 : buf [37];
				final int defenceBonus = buf [38] < 0 ? buf [38] + 256 : buf [38];
				final int movementBonus = buf [39] < 0 ? buf [39] + 256 : buf [39];
				final int resistanceBonus = buf [40] < 0 ? buf [40] + 256 : buf [40];
				final int spellSkillBonus = (buf [41] < 0 ? buf [41] + 256 : buf [41]) / 5;
				final int spellSaveBonus = buf [42] < 0 ? buf [42] + 256 : buf [42];
				final int spellNumber = buf [43] < 0 ? buf [43] + 256 : buf [43];
				final int spellCharges = buf [44] < 0 ? buf [44] + 256 : buf [44];
				
				final long effectFlags = (buf [46] < 0 ? buf [46] + 256l : buf [46]) + ((buf [47] < 0 ? buf [47] + 256l : buf [47]) << 8) +
					((buf [48] < 0 ? buf [48] + 256l : buf [48]) << 16) + ((buf [49] < 0 ? buf [49] + 256l : buf [49]) << 24);

				final String heroItemTypeID = "IT" + StringUtils.padStart (Integer.valueOf (itemTypeNumber+1).toString (), "0", 2);
				final String attackBonusID = (attackBonus == 0) ? null : "IB" + StringUtils.padStart (Integer.valueOf (attackBonus).toString (), "0", 2);
				final String toHitBonusID = (toHitBonus == 0) ? null : "IB" + StringUtils.padStart (Integer.valueOf (toHitBonus + 12).toString (), "0", 2);
				final String defenceBonusID = (defenceBonus == 0) ? null : "IB" + StringUtils.padStart (Integer.valueOf (defenceBonus + 6).toString (), "0", 2);
				final String movementBonusID = (movementBonus == 0) ? null : "IB" + StringUtils.padStart (Integer.valueOf (movementBonus + 15).toString (), "0", 2);
				final String resistanceBonusID = (resistanceBonus == 0) ? null : "IB" + StringUtils.padStart (Integer.valueOf (resistanceBonus + 19).toString (), "0", 2);
				final String spellSkillBonusID = (spellSkillBonus == 0) ? null : "IB" + StringUtils.padStart (Integer.valueOf (spellSkillBonus + 25).toString (), "0", 2);
				final String spellSaveBonusID = (spellSaveBonus == 0) ? null : "IB" + StringUtils.padStart (Integer.valueOf (spellSaveBonus + 29).toString (), "0", 2);
				final String spellID = (spellNumber == 0) ? null : "SP" + StringUtils.padStart (Integer.valueOf (spellNumber).toString (), "0", 3);
				
				// The image numbers need altering - in the original MoM this is an index into ITEMS.LBX, from 0..115
				// In MoM IME this is just the index within the item type, so there is a sword 0..8 just as there is a separate sheild 0..8
				if (imageNumber >= 107)
					imageNumber = imageNumber - 107;
				else if (imageNumber >= 72)
					imageNumber = imageNumber - 72;
				else if (imageNumber >= 62)
				{
					imageNumber = imageNumber - 62;
					
					// The last shield is duplicated
					if (imageNumber == 9)
						imageNumber--;
				}
				else if (imageNumber >= 55)
					imageNumber = imageNumber - 55;
				else if (imageNumber >= 47)
					imageNumber = imageNumber - 47;
				else if (imageNumber >= 38)
					imageNumber = imageNumber - 38;
				else if (imageNumber >= 29)
					imageNumber = imageNumber - 29;
				else if (imageNumber >= 20)
					imageNumber = imageNumber - 20;
				else if (imageNumber >= 9)
					imageNumber = imageNumber - 9;
				
				// Build up the list of bonuses
				final List<String> bonusIDs = new ArrayList<String> ();
				for (final String bonusID : new String [] {attackBonusID, toHitBonusID, defenceBonusID, movementBonusID, resistanceBonusID, spellSkillBonusID, spellSaveBonusID})
					if (bonusID != null)
						bonusIDs.add (bonusID);
				
				long flagBitmask = 1;
				for (final String bonusID : ITEM_BONUS_ID)
				{
					if ((effectFlags & flagBitmask) > 0)
					{
						if (bonusID == null)
							throw new IOException ("Bitmask for item \"" + itemDescription + "\" (" + recordNo + " of " + numberOfRecords +
								") includes a flag that is mapped to a null heroItemBonusID");
						
						bonusIDs.add (bonusID);
					}
					
					flagBitmask = flagBitmask << 1;
				}
				
				if (spellID != null)
					bonusIDs.add (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES);
				
				// Filter out the 2 items that include the broken Merging skill
				if (bonusIDs.contains (DONT_OUTPUT_ITEM))
					log.warn ("Skipping item \"" + itemDescription + "\" (" + recordNo + " of " + numberOfRecords + ") because it includes the disabled Merging skill");
				else
				{
					if (bonusIDs.size () > 4)
					{
						final StringBuilder bonuses = new StringBuilder ();
						for (final String bonusID : bonusIDs)
						{
							if (bonuses.length () > 0)
								bonuses.append (", ");
							
							bonuses.append (bonusID);
						}
						
						log.warn ("Item \"" + itemDescription + "\" (" + recordNo + " of " + numberOfRecords + ") has more than 4 bonuses: " + bonuses);
					}
					
					// Verify the cost
					final Element itemTypeElement = JdomUtils.findDomChildNodeWithTextAttribute (getMdiEditor ().getXmlDocuments ().get (0).getXml (),
						ServerEditorDatabaseConstants.TAG_ENTITY_HERO_ITEM_TYPE, ServerEditorDatabaseConstants.TAG_ATTRIBUTE_HERO_ITEM_TYPE_ID, heroItemTypeID);
					if (itemTypeElement == null)
						throw new IOException ("Item \"" + itemDescription + "\" (" + recordNo + " of " + numberOfRecords +
							") is of type " + heroItemTypeID + " that cannot be found in the XML");
					
					int calculatedCost = Integer.parseInt (itemTypeElement.getChildText (ServerEditorDatabaseConstants.TAG_VALUE_HERO_ITEM_TYPE_BASE_CRAFTING_COST));

					for (final String bonusID : bonusIDs)
					{
						final Element itemBonusElement = JdomUtils.findDomChildNodeWithTextAttribute (getMdiEditor ().getXmlDocuments ().get (0).getXml (),
							ServerEditorDatabaseConstants.TAG_ENTITY_HERO_ITEM_BONUS, ServerEditorDatabaseConstants.TAG_ATTRIBUTE_HERO_ITEM_BONUS_ID, bonusID);
						if (itemBonusElement == null)
							throw new IOException ("Item \"" + itemDescription + "\" (" + recordNo + " of " + numberOfRecords +
								") includes a bonus " + bonusID + " that cannot be found in the XML");
						
						final int bonusCraftingCost;
						if (bonusID.equals (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES))
						{
							final Element spellElement = JdomUtils.findDomChildNodeWithTextAttribute (getMdiEditor ().getXmlDocuments ().get (0).getXml (),
								ServerEditorDatabaseConstants.TAG_ENTITY_SPELL, ServerEditorDatabaseConstants.TAG_ATTRIBUTE_SPELL_ID, spellID);
							if (spellElement == null)
								throw new IOException ("Item \"" + itemDescription + "\" (" + recordNo + " of " + numberOfRecords +
									") includes spell charges for " + spellID + " that cannot be found in the XML");
								
							bonusCraftingCost = spellCharges * 20 * Integer.parseInt
								(spellElement.getChildText (ServerEditorDatabaseConstants.TAG_VALUE_SPELL_COMBAT_CASTING_COST));
						}
						else
							// Note, we're NOT including here that non-spell bonuses on Misc items are supposed to cost double,
							// i.e. we're proving that the cost of every Misc item is WRONG!  But at least it verifies that they're consistent...
							bonusCraftingCost = Integer.parseInt (itemBonusElement.getChildText (ServerEditorDatabaseConstants.TAG_VALUE_HERO_ITEM_BONUS_CRAFTING_COST));
						
						calculatedCost = calculatedCost + bonusCraftingCost;
					}

					if (calculatedCost != craftingCost)
						log.warn ("Item \"" + itemDescription + "\" (" + recordNo + " of " + numberOfRecords + ") has its cost set as " + craftingCost +
							" but calculate the correct cost to be " + calculatedCost);
					
					// Output XML
					final Element heroItem = new Element (ServerEditorDatabaseConstants.TAG_ENTITY_HERO_ITEM);
					
					final Element heroItemNameElement = new Element (ServerEditorDatabaseConstants.TAG_VALUE_HERO_ITEM_NAME);
					heroItemNameElement.setText (itemDescription);
					heroItem.addContent (heroItemNameElement);

					final Element heroItemTypeElement = new Element (ServerEditorDatabaseConstants.TAG_ATTRIBUTE_HERO_ITEM_TYPE_ID);
					heroItemTypeElement.setText (heroItemTypeID);
					heroItem.addContent (heroItemTypeElement);

					final Element imageNumberElement = new Element (ServerEditorDatabaseConstants.TAG_VALUE_HERO_ITEM_IMAGE_NUMBER);
					imageNumberElement.setText (Integer.valueOf (imageNumber).toString ());
					heroItem.addContent (imageNumberElement);
					
					if (spellID != null)
					{
						final Element spellElement = new Element (ServerEditorDatabaseConstants.TAG_ATTRIBUTE_SPELL_ID);
						spellElement.setText (spellID);
						heroItem.addContent (spellElement);

						final Element spellChargesElement = new Element (ServerEditorDatabaseConstants.TAG_VALUE_HERO_ITEM_SPELL_CHARGES);
						spellChargesElement.setText (Integer.valueOf (spellCharges).toString ());
						heroItem.addContent (spellChargesElement);
					}
					
					// Output bonuses
					for (final String bonusID : bonusIDs)
					{
						final Element bonusElement = new Element (ServerEditorDatabaseConstants.TAG_CHILD_ENTITY_HERO_ITEM_CHOSEN_BONUS);
						bonusElement.setAttribute (ServerEditorDatabaseConstants.TAG_ATTRIBUTE_HERO_ITEM_BONUS_ID, bonusID);
						heroItem.addContent (bonusElement);
					}
					
					// Be careful about where we add it
					final int insertionPoint = getMdiEditor ().getXmlDocuments ().determineElementInsertionPoint
						(new ComplexTypeReference (getMdiEditor ().getXmlDocuments ().get (0), getMdiEditor ().getXmlDocuments ().get (0).getXsd ().getTopLevelTypeDefinition ()),
						getContainer (), ServerEditorDatabaseConstants.TAG_ENTITY_HERO_ITEM);
					getContainer ().addContent (insertionPoint, heroItem);
				}
			}
			
			lbxStream.close ();
		}
	}
}