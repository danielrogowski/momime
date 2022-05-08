package momime.editors.grid;

import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.filechooser.FileFilter;

import com.ndg.archive.LbxArchiveReader;
import com.ndg.swing.GridBagConstraintsHorizontalFill;
import com.ndg.swing.GridBagConstraintsNoFill;
import com.ndg.swing.filefilters.SpecificFilenameFilter;
import com.ndg.utils.StreamUtils;
import com.ndg.xmleditor.doc.XmlDocument;
import com.ndg.xmleditor.editor.XmlEditorException;
import com.ndg.xmleditor.schema.SimpleTypeEx;

import momime.common.database.CommonDatabaseConstants;

/**
 * Allows importing phrases from the original MoM DIPLOMSG.LBX
 */
public abstract class MoMEditorGridWithDiplomacyMessagesImport extends MoMEditorGridWithImport
{
	/** Number of variants of each message in DIPLOMSG.LBX */
	protected final static int VARIANT_COUNT = 15;
	
	/** Offset to select which language the DIPLOMSG we're importing is */
	private JComboBox<String> languageCombo;

	/**
	 * Adds a drop down to select the language of Diplomacy messages being imported
	 * @throws XmlEditorException If there a syntax problem parsing the XSD
	 * @throws IOException If there is a problem loading the button images
	 */
	@Override
	public final void init () throws XmlEditorException, IOException
	{
		super.init ();
		
		// Find list of languages
		final XmlDocument commonXSD = getMdiEditor ().getXmlDocuments ().findDocumentWithNamespaceURI (CommonDatabaseConstants.COMMON_XSD_NAMESPACE_URI);
		final SimpleTypeEx languageType = commonXSD.getXsd ().findTopLevelSimpleType ("language");
		final String [] languages = languageType.getEnumerations (false) [0];
		
		// Overall heading
		final JLabel headingLabel = new JLabel ("For use with Import button:");
		headingLabel.setFont (headingLabel.getFont ().deriveFont (Font.BOLD));
		getButtonPanel ().add (headingLabel, getUtils ().createConstraintsNoFill (0, getButtonPanelY (), 2, 1, INSET, GridBagConstraintsNoFill.WEST));
		setButtonPanelY (getButtonPanelY () + 1);

		// List languages defined in XSD
		final JLabel languageLabel = new JLabel ("Import language:");
		getButtonPanel ().add (languageLabel, getUtils ().createConstraintsNoFill (0, getButtonPanelY (), 2, 1, INSET, GridBagConstraintsNoFill.WEST));
		setButtonPanelY (getButtonPanelY () + 1);

		languageCombo = new JComboBox<String> ();
		for (final String language : languages)
			languageCombo.addItem (language);
		languageCombo.setEditable (true);
		getButtonPanel ().add (languageCombo, getUtils ().createConstraintsHorizontalFill (0, getButtonPanelY (), 2, 1, INSET, GridBagConstraintsHorizontalFill.CENTRE));
		setButtonPanelY (getButtonPanelY () + 1);
	}
	
	/**
	 * Specify the filename we're looking for
	 * @param lbxChooser The file open dialog
	 */
	@Override
	protected final void addOtherFilters (final JFileChooser lbxChooser)
	{
		for (final FileFilter filter : lbxChooser.getChoosableFileFilters ())
			lbxChooser.removeChoosableFileFilter (filter);

		lbxChooser.addChoosableFileFilter (new SpecificFilenameFilter ("DIPLOMSG.LBX", "Original Master of Magic diplomacy messages (DIPLOMSG.LBX)"));
	};
	
	/**
	 * @param lbxFilename The LBX filename chosen in the file open dialog
	 * @return Whole diplomacy messages file in a list, so its easier to jump to specific entries
	 * @throws IOException If there is a problem reading the LBX file
	 */
	protected final List<String> readDiplomacyMessages (final File lbxFilename) throws IOException
	{
		final List<String> list = new ArrayList<String> ();
		
		// DIPLOMSG.LBX has 2 subfiles, need 2nd one
		try (final InputStream lbxStream = new FileInputStream (lbxFilename))
		{
			LbxArchiveReader.positionToSubFile (lbxStream, 1);
			
			// Read number and size of records
			final int numberOfRecords = StreamUtils.readUnsigned2ByteIntFromStream (lbxStream, ByteOrder.LITTLE_ENDIAN, "Number of Records");
			final int recordSize = StreamUtils.readUnsigned2ByteIntFromStream (lbxStream, ByteOrder.LITTLE_ENDIAN, "Record Size");

			// Read each record
			for (int recordNo = 0; recordNo < numberOfRecords; recordNo++)
			{
				// Can't read it directly as a string as we need the raw bytes to find the special codes that get replaced by wizard names, city names, spell names and so on
				final byte [] byteArray = StreamUtils.readByteArrayFromStream (lbxStream, recordSize, "Diplomacy message " + recordNo + " of " + numberOfRecords);
				
				// Now go through it one byte at a time
				final StringBuilder diplomacyText = new StringBuilder ();
				boolean foundNull = false;
				int n = 0;
				while ((n < recordSize) && (!foundNull))
				{
					final byte b = byteArray [n];
					
					// There are no 0x86 codes
					if (b == 0)
						foundNull = true;
					else if (b == (byte) 0x80)
						diplomacyText.append ("OUR_PLAYER_NAME");
					else if (b == (byte) 0x81)
						diplomacyText.append ("TALKING_PLAYER_NAME");
					else if ((b == (byte) 0x82) || (b == (byte) 0x85) || (b == (byte) 0x87) || (b == (byte) 0x92) || (b == (byte) 0x93))
						diplomacyText.append ("THIRD_PLAYER_NAME");
					else if (b == (byte) 0x83)
						diplomacyText.append ("CITY_NAME");
					else if (b == (byte) 0x84)
						diplomacyText.append ("CITY_SIZE");
					else if ((b == (byte) 0x88) || (b == (byte) 0x94))
						diplomacyText.append ("GOLD_AMOUNT");
					else if ((b == (byte) 0x89) || (b == (byte) 0x8A) || (b == (byte) 0x8C) || (b == (byte) 0x91))
						diplomacyText.append ("SPELL_NAME");
					else if ((b == (byte) 0x8B) || (b == (byte) 0x8F))
						diplomacyText.append ("TYPE_OF_PACT");
					else if ((b == (byte) 0x8D) || (b == (byte) 0x8E))
						diplomacyText.append ("UNIT_NAME");
					else if (b == (byte) 0x90)
						diplomacyText.append ("YEAR");
					else
						diplomacyText.append (new String (new byte [] {b}));
					
					n++;
				}

				list.add ((diplomacyText.length () == 0) ? null : diplomacyText.toString ());
			}
		}

		return list;
	}
	
	/**
	 * @param list List output from readDiplomacyMessages
	 * @param index Index of first message to extract from the list
	 * @return Up to VARIANT_COUNT messages starting from index
	 */
	protected final List<String> getMessageVariants (final List<String> list, final int index)
	{
		final List<String> out = new ArrayList<String> ();
		
		for (int n = 0; n < VARIANT_COUNT; n++)
		{
			final String s = list.get (index + n);
			if (s != null)
				out.add (s);
		}
		
		return out;
	}

	/**
	 * @return Offset to select which language the DIPLOMSG we're importing is
	 */
	public final JComboBox<String> getLanguageCombo ()
	{
		return languageCombo;
	}
}