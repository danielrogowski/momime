package momime.editors.server.heroItemBonus;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.Arrays;

import javax.swing.JFileChooser;

import org.jdom2.Element;

import com.ndg.archive.LbxArchiveReader;
import com.ndg.swing.filefilters.SpecificFilenameFilter;
import com.ndg.utils.StreamUtils;
import com.ndg.utils.StringUtils;
import com.ndg.xml.JdomUtils;
import com.ndg.xmleditor.doc.ComplexTypeReference;
import com.ndg.xmleditor.editor.XmlEditorException;

import momime.common.database.CommonDatabaseConstants;
import momime.editors.grid.MoMEditorGridWithImport;
import momime.editors.server.ServerEditorDatabaseConstants;
import momime.server.database.ServerDatabaseValues;

/**
 * Allows importing data on hero item bonuses (e.g. +1 Attack, -5 Spell Save, Holy Avenger) from original MoM LBXes
 */
public final class HeroItemBonusGrid extends MoMEditorGridWithImport
{
	/** Power types 0-6 indicate normal attribute bonuses */
	private final static String [] POWER_TYPE_SKILL_ID = new String []
		{CommonDatabaseConstants.UNIT_SKILL_ID_ATTACK_APPROPRIATE_FOR_TYPE_OF_HERO_ITEM, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT,
		CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE, CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_UNIT,
		ServerDatabaseValues.UNIT_SKILL_ID_SAVING_THROW_PENALTY,
		CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE};
	
	/**
	 * Specify the filename we're looking for
	 * @param lbxChooser The file open dialog
	 */
	@Override
	protected final void addOtherFilters (final JFileChooser lbxChooser)
	{
		lbxChooser.addChoosableFileFilter (new SpecificFilenameFilter ("ITEMPOW.LBX", "Hero item bonuses (ITEMPOW.LBX)"));
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
			int heroItemBonusNumber = 0;
			for (int recordNo = 0; recordNo < numberOfRecords; recordNo++)
			{
				final byte [] buf = new byte [recordSize];
				if (lbxStream.read (buf) != recordSize)
					throw new IOException ("Ran out of data on record " + recordNo + " of " + numberOfRecords);
				
				// Parse description
				String bonusDescription = new String (Arrays.copyOf (buf, 17));
				final int nullPos = bonusDescription.indexOf ('\u0000');
				if (nullPos >= 0)
					bonusDescription = bonusDescription.substring (0, nullPos);

				if (bonusDescription.length () > 0)
				{
					bonusDescription = bonusDescription.trim ();
					
					// Parse data
					final int itemTypeFlags = ((buf [18] < 0) ? buf [18] + 256 : buf [18]) + (((buf [19] < 0) ? buf [19] + 256 : buf [19]) << 8);
					Integer craftingCost = ((buf [20] < 0) ? buf [20] + 256 : buf [20]) + (((buf [21] < 0) ? buf [21] + 256 : buf [21]) << 8);
					final int powerType = ((buf [22] < 0) ? buf [22] + 256 : buf [22]) + (((buf [23] < 0) ? buf [23] + 256 : buf [23]) << 8);
					
					// The meaning of "count" is different depending on whether its a regular attribute bonus like +Attack, or an enchantment like Bless
					final int count = ((buf [24] < 0) ? buf [24] + 256 : buf [24]) + (((buf [25] < 0) ? buf [25] + 256 : buf [25]) << 8);
					
					// Convert powerType
					final String unitSkillID;
					final String pickID;
					if (count == 0)
					{
						// This is a special case for the "Spell Charges" item bonus
						unitSkillID = null;
						pickID = null;
						craftingCost = null;
					}
					else if (powerType < POWER_TYPE_SKILL_ID.length)
					{
						unitSkillID = POWER_TYPE_SKILL_ID [powerType];
						pickID = null;
					}
					else if (powerType == 777)
					{
						unitSkillID = null;
						pickID = "MB01";	// Life
					}
					else if (powerType == 1033)
					{
						unitSkillID = null;
						pickID = "MB02";	// Death
					}
					else if (powerType == 521)
					{
						unitSkillID = null;
						pickID = "MB03";	// Chaos
					}
					else if ((powerType == 7) || (powerType == 9))
					{
						unitSkillID = null;
						pickID = "MB04";	// Nature
					}
					else if ((powerType == 264) || (powerType == 265))
					{
						unitSkillID = null;
						pickID = "MB05";	// Sorcery
					}
					else
						throw new IOException ("Item bonus " + recordNo + " of " + numberOfRecords + " has unknown powerType = " + powerType);
					
					// Filter out "merging"
					if ((pickID == null) || (count < 10))
					{
						// Add the main heroItemBonus record
						heroItemBonusNumber++;
						final String heroItemBonusID = "IB" + StringUtils.padStart (Integer.valueOf (heroItemBonusNumber).toString (), "0", 2);
						
						final Element heroItemElement = new Element (ServerEditorDatabaseConstants.TAG_ENTITY_HERO_ITEM_BONUS);
						heroItemElement.setAttribute (ServerEditorDatabaseConstants.TAG_ATTRIBUTE_HERO_ITEM_BONUS_ID, heroItemBonusID);
						
						if (craftingCost != null)
						{
							final Element craftingCostElement = new Element (ServerEditorDatabaseConstants.TAG_VALUE_HERO_ITEM_BONUS_CRAFTING_COST);
							craftingCostElement.setText (craftingCost.toString ());
							heroItemElement.addContent (craftingCostElement);
						}
	
						final Element craftingCostMultiplierAppliesElement = new Element (ServerEditorDatabaseConstants.TAG_VALUE_HERO_ITEM_BONUS_CRAFTING_COST_MULTIPLIER_APPLES);
						craftingCostMultiplierAppliesElement.setText ((unitSkillID != null) ? "true" : "false");
						heroItemElement.addContent (craftingCostMultiplierAppliesElement);
						
						if (unitSkillID != null)
						{
							final Element bonusStatElement = new Element (ServerEditorDatabaseConstants.TAG_CHILD_ENTITY_HERO_ITEM_BONUS_STAT);
							bonusStatElement.setAttribute (ServerEditorDatabaseConstants.TAG_ATTRIBUTE_UNIT_SKILL_ID, unitSkillID);
							
							final Element bonusStatValueElement = new Element (ServerEditorDatabaseConstants.TAG_VALUE_UNIT_SKILL_VALUE);
							bonusStatValueElement.setText (Integer.valueOf (count).toString ());
							bonusStatElement.addContent (bonusStatValueElement);
							
							heroItemElement.addContent (bonusStatElement);
						}
						
						if (pickID != null)
						{
							final Element prereqElement = new Element (ServerEditorDatabaseConstants.TAG_CHILD_ENTITY_HERO_ITEM_BONUS_PREREQ);
							prereqElement.setAttribute (ServerEditorDatabaseConstants.TAG_ATTRIBUTE_PICK_ID, pickID);
							
							final Element quantityElement = new Element (ServerEditorDatabaseConstants.TAG_VALUE_QUANTITY);
							quantityElement.setText (Integer.valueOf (count).toString ());
							prereqElement.addContent (quantityElement);
							
							heroItemElement.addContent (prereqElement);
						}
						
						final Element descriptionElement = new Element (ServerEditorDatabaseConstants.TAG_VALUE_HERO_ITEM_BONUS_DESCRIPTION);
						descriptionElement.setText (bonusDescription);
						heroItemElement.addContent (descriptionElement);
	
						// Be careful about where we add it
						final int insertionPoint = getMdiEditor ().getXmlDocuments ().determineElementInsertionPoint
							(new ComplexTypeReference (getMdiEditor ().getXmlDocuments ().get (0), getMdiEditor ().getXmlDocuments ().get (0).getXsd ().getTopLevelTypeDefinition ()),
							getContainer (), ServerEditorDatabaseConstants.TAG_ENTITY_HERO_ITEM_BONUS);
						getContainer ().addContent (insertionPoint, heroItemElement);
						
						// Find the existing heroItemType records, and mark against them which item types can have this bonus
						int flagBitmask = 1;
						for (int itemType = 1; itemType <= 10; itemType++)
						{
							if ((itemTypeFlags & flagBitmask) > 0)
							{
								final String heroItemTypeID = "IT" + StringUtils.padStart (Integer.valueOf (itemType).toString (), "0", 2);
								final Element heroItemType = JdomUtils.findDomChildNodeWithTextAttribute (getMdiEditor ().getXmlDocuments ().get (0).getXml (),
									ServerEditorDatabaseConstants.TAG_ENTITY_HERO_ITEM_TYPE, ServerEditorDatabaseConstants.TAG_ATTRIBUTE_HERO_ITEM_TYPE_ID, heroItemTypeID);
								
								if (heroItemType == null)
									throw new IOException ("Can't find hero item type " + heroItemTypeID + " in XML");
								
								final Element allowedElement = new Element (ServerEditorDatabaseConstants.TAG_CHILD_ENTITY_HERO_ITEM_TYPE_ALLOWED_BONUS);
								allowedElement.setAttribute (ServerEditorDatabaseConstants.TAG_ATTRIBUTE_HERO_ITEM_BONUS_ID, heroItemBonusID);

								final int allowedInsertionPoint = getMdiEditor ().getXmlDocuments ().determineElementInsertionPoint
									(new ComplexTypeReference (getMdiEditor ().getXmlDocuments ().get (0),
										getMdiEditor ().getXmlDocuments ().get (0).getXsd ().findTopLevelComplexType (ServerEditorDatabaseConstants.TAG_ENTITY_HERO_ITEM_TYPE)),
									heroItemType, ServerEditorDatabaseConstants.TAG_CHILD_ENTITY_HERO_ITEM_TYPE_ALLOWED_BONUS);
								heroItemType.addContent (allowedInsertionPoint, allowedElement);
							}
							
							flagBitmask = flagBitmask << 1;
						}
					}
				}
			}
			
			lbxStream.close ();
		}
	}
}