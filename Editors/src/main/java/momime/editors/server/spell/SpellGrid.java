package momime.editors.server.spell;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.List;

import javax.swing.JFileChooser;

import org.jdom2.Element;

import com.ndg.archive.LbxArchiveReader;
import com.ndg.utils.StreamUtils;
import com.ndg.utils.StringUtils;
import com.ndg.utils.swing.filefilters.ExtensionFileFilter;
import com.ndg.utils.swing.filefilters.SpecificFilenameFilter;
import com.ndg.xmleditor.doc.ComplexTypeReference;
import com.ndg.xmleditor.editor.XmlEditorException;
import com.ndg.xmleditor.grid.column.XmlGridColumn;

import momime.editors.grid.MoMEditorGridWithImport;
import momime.editors.server.ServerEditorDatabaseConstants;

/**
 * Adds a special column listing out all the units that a spell can summon
 */
public final class SpellGrid extends MoMEditorGridWithImport
{
	/** Amount of data for each spell in SPELLDAT.LBX (rest of each record is the spell name) */
	private final static int SPELL_DATA_LENGTH = 17;

	/** Length of the ID for each help text item, e.g. "WAR BEARS" (rest of each record is the actual help text) */
	private final static int HELP_TEXT_ID_LENGTH = 48;

	/**
	 * Adds a special column listing out all the units that a spell can summon
	 * @return The list of columns to display in the grid
	 * @throws XmlEditorException If there a syntax problem parsing the XSD
	 */
	@Override
	protected final List<XmlGridColumn> buildColumnsList ()
		throws XmlEditorException
	{
		final List<XmlGridColumn> columns = super.buildColumnsList ();

		columns.add (new SpellSummonedUnitsColumn (getTypeDefinition ().getComplexTypeDefinition (), getMdiEditor ().getXmlDocuments ()));

		return columns;
	}

	/**
	 * Specify the filename we're looking for
	 * @param lbxChooser The file open dialog
	 */
	@Override
	protected final void addOtherFilters (final JFileChooser lbxChooser)
	{
		lbxChooser.addChoosableFileFilter (new SpecificFilenameFilter ("SPELLDAT.LBX", "Original Master of Magic spell data (SPELLDAT.LBX)"));
	};

	/**
	 * Imports spell names and descriptions from 3 different LBX files
	 * @param lbxFilename The lbx filename chosen in the file open dialog
	 * @throws IOException If there is a problem reading one of the LBX file
	 * @throws XmlEditorException If there is a problem with one of the XML editor helper methods
	 */
	@Override
	protected final void importFromLbx (final File lbxFilename) throws IOException, XmlEditorException
	{
		// Need to ask for 2 other LBXes first
		final JFileChooser descriptionsLbxChooser = new JFileChooser ();
		descriptionsLbxChooser.addChoosableFileFilter (new ExtensionFileFilter ("lbx", "Original Master of Magic LBX files"));
		descriptionsLbxChooser.addChoosableFileFilter (new SpecificFilenameFilter ("DESC.LBX", "Original Master of Magic spell descriptions (DESC.LBX)"));

		final JFileChooser helpTextLbxChooser = new JFileChooser ();
		helpTextLbxChooser.addChoosableFileFilter (new ExtensionFileFilter ("lbx", "Original Master of Magic LBX files"));
		helpTextLbxChooser.addChoosableFileFilter (new SpecificFilenameFilter ("HELP.LBX", "Original Master of Magic help text (HELP.LBX)"));

		if ((descriptionsLbxChooser.showOpenDialog (null) == JFileChooser.APPROVE_OPTION) && (helpTextLbxChooser.showOpenDialog (null) == JFileChooser.APPROVE_OPTION))
		{
			// Open all 3 files
			// SPELLDAT.LBX only has a single subfile in it
			try (final InputStream namesStream = new FileInputStream (lbxFilename))
			{
				LbxArchiveReader.positionToSubFile (namesStream, 0);
				final int numberOfNames = StreamUtils.readUnsigned2ByteIntFromStream (namesStream, ByteOrder.LITTLE_ENDIAN, "Number of Records (Names)");
				final int namesRecordSize = StreamUtils.readUnsigned2ByteIntFromStream (namesStream, ByteOrder.LITTLE_ENDIAN, "Record Size (Names)");
				final int nameLength = namesRecordSize - SPELL_DATA_LENGTH;

				// DESC.LBX only has a single subfile in it
				try (final InputStream descriptionsStream = new FileInputStream (descriptionsLbxChooser.getSelectedFile ()))
				{
					LbxArchiveReader.positionToSubFile (descriptionsStream, 0);
					StreamUtils.readUnsigned2ByteIntFromStream (descriptionsStream, ByteOrder.LITTLE_ENDIAN, "Number of Records (Descriptions)");
					final int descriptionsRecordSize = StreamUtils.readUnsigned2ByteIntFromStream (descriptionsStream, ByteOrder.LITTLE_ENDIAN, "Record Size (Descriptions)");

					// HELP.LBX has 3 subfiles in it, and we need the 3rd one
					try (final InputStream helpTextStream = new FileInputStream (helpTextLbxChooser.getSelectedFile ()))
					{
						LbxArchiveReader.positionToSubFile (helpTextStream, 2);
						StreamUtils.readUnsigned2ByteIntFromStream (helpTextStream, ByteOrder.LITTLE_ENDIAN, "Number of Records (Help Text)");
						final int helpTextRecordSize = StreamUtils.readUnsigned2ByteIntFromStream (helpTextStream, ByteOrder.LITTLE_ENDIAN, "Record Size (Help Text)");

						// Read each record
						for (int recordNo = 0; recordNo < numberOfNames; recordNo++)
						{
							// Get the name and description
							final String spellName = StreamUtils.readNullTerminatedFixedLengthStringFromStream (namesStream, nameLength, "Spell " + recordNo + " of " + numberOfNames + " Name");
							final String spellDescription = StreamUtils.readNullTerminatedFixedLengthStringFromStream (descriptionsStream, descriptionsRecordSize, "Spell " + recordNo + " of " + numberOfNames + " Description");

							// Skip over the spell data
							StreamUtils.readByteArrayFromStream (namesStream, SPELL_DATA_LENGTH, "Spell " + recordNo + " of " + numberOfNames + " Data");

							// Ignore the first dummy record
							if (recordNo > 0)
							{
								// Help text - Finding the entry is a little tricky, especially since there are multiple help text entries for things like 'War Bears'
								// First spell is 'Earth to Mud' which only appears once, so best way is to position to the right entry on the first spell, and thereafter just keep reading down the file
								String helpText;
								if (recordNo == 1)
								{
									String helpTextId;
									do
									{
										helpTextId = StreamUtils.readNullTerminatedFixedLengthStringFromStream (helpTextStream, HELP_TEXT_ID_LENGTH, "Skip help text ID");
										helpText = StreamUtils.readNullTerminatedFixedLengthStringFromStream (helpTextStream, helpTextRecordSize - HELP_TEXT_ID_LENGTH, "Skip help text");
									} while (!spellName.toUpperCase ().equals (helpTextId));
								}
								else
								{
									StreamUtils.readNullTerminatedFixedLengthStringFromStream (helpTextStream, HELP_TEXT_ID_LENGTH, "Spell " + recordNo + " of " + numberOfNames + " Help Text ID");
									helpText = StreamUtils.readNullTerminatedFixedLengthStringFromStream (helpTextStream, helpTextRecordSize - HELP_TEXT_ID_LENGTH, "Spell " + recordNo + " of " + numberOfNames + " Help Text");
								}

								// Eliminate the upkeep/target/etc crap from the start of the help text
								final int crapPos = helpText.lastIndexOf ('\u0014');
								if (crapPos >= 0)
									helpText = helpText.substring (crapPos + 1);

								helpText = helpText.replaceAll ("\\.  ", "\\. ");
								helpText = helpText.replaceAll ("\\. ", "\\.\n\n");	// Have verified with a hex editor that this does generate CR+LF, in fact if you include the \r you get garbage in the generated XML

								// Add to XML
								final Element spellElement = new Element (ServerEditorDatabaseConstants.TAG_ENTITY_SPELL);
								spellElement.setAttribute (ServerEditorDatabaseConstants.TAG_ATTRIBUTE_SPELL_ID, "SP" + StringUtils.padStart (Integer.valueOf (recordNo).toString (), "0", 3));

								final Element nameElement = new Element (ServerEditorDatabaseConstants.TAG_VALUE_SPELL_NAME);
								nameElement.setText (spellName);
								spellElement.addContent (nameElement);

								final Element descriptionElement = new Element (ServerEditorDatabaseConstants.TAG_VALUE_SPELL_DESCRIPTION);
								descriptionElement.setText (spellDescription);
								spellElement.addContent (descriptionElement);

								final Element helpTextElement = new Element (ServerEditorDatabaseConstants.TAG_VALUE_SPELL_HELP_TEXT);
								helpTextElement.setText (helpText);
								spellElement.addContent (helpTextElement);

								// Be careful about where we add it
								final int insertionPoint = getMdiEditor ().getXmlDocuments ().determineElementInsertionPoint
									(new ComplexTypeReference (getMdiEditor ().getXmlDocuments ().get (0), getMdiEditor ().getXmlDocuments ().get (0).getXsd ().getTopLevelTypeDefinition ()),
									getContainer (), ServerEditorDatabaseConstants.TAG_ENTITY_SPELL);
								getContainer ().addContent (insertionPoint, spellElement);
							}
						}
						helpTextStream.close ();
					}
					descriptionsStream.close ();
				}
				namesStream.close ();
			}
		}
	}
}