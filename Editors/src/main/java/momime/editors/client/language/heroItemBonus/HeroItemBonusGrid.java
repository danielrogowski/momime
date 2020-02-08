package momime.editors.client.language.heroItemBonus;

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
import com.ndg.xmleditor.doc.ComplexTypeReference;
import com.ndg.xmleditor.editor.XmlEditorException;

import momime.editors.MoMEditorGridWithImport;
import momime.editors.server.ServerEditorDatabaseConstants;

/**
 * Allows importing descriptions of hero item bonuses (e.g. +1 Attack, -5 Spell Save, Holy Avenger) from original MoM LBXes
 */
public final class HeroItemBonusGrid extends MoMEditorGridWithImport
{
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
					final int powerType = ((buf [22] < 0) ? buf [22] + 256 : buf [22]) + (((buf [23] < 0) ? buf [23] + 256 : buf [23]) << 8);
					
					// The meaning of "count" is different depending on whether its a regular attribute bonus like +Attack, or an enchantment like Bless
					final int count = ((buf [24] < 0) ? buf [24] + 256 : buf [24]) + (((buf [25] < 0) ? buf [25] + 256 : buf [25]) << 8);
					
					// Filter out "merging"
					if ((count < 10) || (powerType < 7))
					{
						// Add the main heroItemBonus record
						heroItemBonusNumber++;
						final String heroItemBonusID = "IB" + StringUtils.padStart (Integer.valueOf (heroItemBonusNumber).toString (), "0", 2);
						
						final Element heroItemElement = new Element (ServerEditorDatabaseConstants.TAG_ENTITY_HERO_ITEM_BONUS);
						heroItemElement.setAttribute (ServerEditorDatabaseConstants.TAG_ATTRIBUTE_HERO_ITEM_BONUS_ID, heroItemBonusID);
						
						final Element descriptionElement = new Element (ServerEditorDatabaseConstants.TAG_VALUE_HERO_ITEM_BONUS_DESCRIPTION);
						descriptionElement.setText (bonusDescription);
						heroItemElement.addContent (descriptionElement);
	
						// Be careful about where we add it
						final int insertionPoint = getMdiEditor ().getXmlDocuments ().determineElementInsertionPoint
							(new ComplexTypeReference (getMdiEditor ().getXmlDocuments ().get (0), getMdiEditor ().getXmlDocuments ().get (0).getXsd ().getTopLevelTypeDefinition ()),
							getContainer (), ServerEditorDatabaseConstants.TAG_ENTITY_HERO_ITEM_BONUS);
						getContainer ().addContent (insertionPoint, heroItemElement);
					}
				}
			}
			
			lbxStream.close ();
		}
	}
}