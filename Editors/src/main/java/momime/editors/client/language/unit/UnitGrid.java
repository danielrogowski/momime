package momime.editors.client.language.unit;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.filechooser.FileFilter;

import momime.editors.client.language.MoMLanguageEditorGridWithImport;
import momime.editors.server.ServerEditorDatabaseConstants;

import org.jdom.Element;

import com.ndg.swing.SwingLayoutConstants;
import com.ndg.swing.filefilters.SpecificFilenameFilter;
import com.ndg.utils.StreamUtils;
import com.ndg.utils.StringUtils;
import com.ndg.xmleditor.editor.ComplexTypeReference;
import com.ndg.xmleditor.editor.XmlEditorException;
import com.ndg.xmleditor.editor.XmlEditorMain;
import com.ndg.xmleditor.editor.XmlEditorUtils;

/**
 * Grid for displaying and editing hero names names
 * Allows importing unit names from the original MoM WIZARDS.EXE
 */
public class UnitGrid extends MoMLanguageEditorGridWithImport
{
	/**
	 * Offset into WIZARDS.EXE of the names table
	 */
	private final JComboBox dataOffsetCombo;

	/**
	 * Offset to add to the values read from the names table in order to find the address of the actual name
	 */
	private final JComboBox namesOffsetCombo;

	/**
	 * Number of unit names to read
	 */
	private static final int UNIT_COUNT = 198;

	/**
	 * Size of each block of unit data (2 bytes of name offset followed by 36 bytes of data)
	 */
	private static final int UNIT_DATA_BLOCK_SIZE = 36;

	/**
	 * Creates a grid for displaying and editing hero names names
	 * @param anEntityElement The xsd:element node of the entity being edited from the XSD, i.e. for a top level entity, this will be a the entry under the xsd:sequence under the database complex type
	 * @param aTypeDefinition The xsd:complexType node of the entity being edited from the XSD
	 * @param aParentRecord If this is a child entity, this value holds the parent record; if this is a top level entity, this value will be null
	 * @param aParentEntityElements Array of xsd:element entries that have been drilled down to to reach here - earliest parent first in the list, immediate parent to entity last in the list - or null if this is a top level entity
	 * @param aParentTypeDefinitions Array of type definitions that have been drilled down to to reach here - earliest parent first in the list, immediate parent to entity last in the list - or null if this is a top level entity
	 * @param aMdiEditor The main MDI window
	 * @throws XmlEditorException If there a syntax problem parsing the XSD
	 * @throws IOException If there is a problem loading the button images
	 */
	public UnitGrid (final Element anEntityElement, final ComplexTypeReference aTypeDefinition,
		final Element aParentRecord, final Element [] aParentEntityElements, final ComplexTypeReference [] aParentTypeDefinitions, final XmlEditorMain aMdiEditor)
		throws XmlEditorException, IOException
	{
		super (anEntityElement, aTypeDefinition, aParentRecord, aParentEntityElements, aParentTypeDefinitions, aMdiEditor);

		final Dimension size = new Dimension (BUTTON_WIDTH, SwingLayoutConstants.TEXT_FIELD_HEIGHT);

		// Overall heading
		final JLabel headingLabel = new JLabel ("For use with Import button:");
		headingLabel.setFont (headingLabel.getFont ().deriveFont (Font.BOLD));
		headingLabel.setPreferredSize (size);
		headingLabel.setMinimumSize (size);
		headingLabel.setMaximumSize (size);
		headingLabel.setAlignmentX (Component.LEFT_ALIGNMENT);
		headingLabel.setAlignmentY (Component.TOP_ALIGNMENT);
		getButtonPanel ().add (headingLabel);

		// Offset into WIZARDS.EXE of the names table
		final JLabel dataOffsetLabel = new JLabel ("Offset of names table:");
		dataOffsetLabel.setPreferredSize (size);
		dataOffsetLabel.setMinimumSize (size);
		dataOffsetLabel.setMaximumSize (size);
		dataOffsetLabel.setAlignmentX (Component.LEFT_ALIGNMENT);
		dataOffsetLabel.setAlignmentY (Component.TOP_ALIGNMENT);
		getButtonPanel ().add (dataOffsetLabel);

		dataOffsetCombo = new JComboBox ();
		dataOffsetCombo.setPreferredSize (size);
		dataOffsetCombo.setMinimumSize (size);
		dataOffsetCombo.setMaximumSize (size);
		dataOffsetCombo.setAlignmentX (Component.LEFT_ALIGNMENT);
		dataOffsetCombo.setAlignmentY (Component.TOP_ALIGNMENT);
		dataOffsetCombo.addItem ("2963C (English)");
		dataOffsetCombo.addItem ("2906C (French)");
		dataOffsetCombo.addItem ("2966C (German)");
		dataOffsetCombo.setEditable (true);
		getButtonPanel ().add (dataOffsetCombo);

		// Offset to add to the values read from the names table in order to find the address of the actual name
		final JLabel namesOffsetLabel = new JLabel ("Offset to add to names table values:");
		namesOffsetLabel.setPreferredSize (size);
		namesOffsetLabel.setMinimumSize (size);
		namesOffsetLabel.setMaximumSize (size);
		namesOffsetLabel.setAlignmentX (Component.LEFT_ALIGNMENT);
		namesOffsetLabel.setAlignmentY (Component.TOP_ALIGNMENT);
		getButtonPanel ().add (namesOffsetLabel);

		namesOffsetCombo = new JComboBox ();
		namesOffsetCombo.setPreferredSize (size);
		namesOffsetCombo.setMinimumSize (size);
		namesOffsetCombo.setMaximumSize (size);
		namesOffsetCombo.setAlignmentX (Component.LEFT_ALIGNMENT);
		namesOffsetCombo.setAlignmentY (Component.TOP_ALIGNMENT);
		namesOffsetCombo.addItem ("294A0 (English)");
		namesOffsetCombo.addItem ("28ED0 (French)");
		namesOffsetCombo.addItem ("294D0 (German)");
		namesOffsetCombo.setEditable (true);
		getButtonPanel ().add (namesOffsetCombo);
	}

	/**
	 * Specify the filename we're looking for
	 * @param lbxChooser The file open dialog
	 */
	@Override
	protected void addOtherFilters (final JFileChooser lbxChooser)
	{
		for (final FileFilter filter : lbxChooser.getChoosableFileFilters ())
			lbxChooser.removeChoosableFileFilter (filter);

		lbxChooser.addChoosableFileFilter (new SpecificFilenameFilter ("WIZARDS.EXE", "Original Master of Magic executable (WIZARDS.EXE)"));
	};

	/**
	 * Handles that the hex numbers in the combo box may or may not be followed by a description after a space
	 * @param value Text read from combo box
	 * @return Integer offset value
	 */
	private int readValueFromComboBox (final Object value)
	{
		String useValue = value.toString ();
		final int spacePos = useValue.indexOf (" ");
		if (spacePos >= 0)
			useValue = useValue.substring (0, spacePos);

		return Integer.parseInt (useValue, 16);
	}

	/**
	 * Imports unit names descriptions from WIZARDS.EXE
	 * @param exeFilename The EXE filename chosen in the file open dialog
	 * @throws IOException If there is a problem reading the EXE file
	 * @throws XmlEditorException If there is a problem using helper methods from the XML editor
	 */
	@Override
	protected void importFromLbx (final File exeFilename)
		throws IOException, XmlEditorException
	{
		// Read the two offsets from the form
		final int dataOffset = readValueFromComboBox (dataOffsetCombo.getSelectedItem ());
		final int namesOffset = readValueFromComboBox (namesOffsetCombo.getSelectedItem ());

		// Open the file
		final BufferedInputStream exeStream = new BufferedInputStream (new FileInputStream (exeFilename));

		// How long from the start of the file is the END of the names table
		final int dataEnd = dataOffset + (UNIT_COUNT * UNIT_DATA_BLOCK_SIZE);

		// Read in the entire name offset table
		exeStream.mark (dataEnd);
		StreamUtils.readByteArrayFromStream (exeStream, dataOffset, "Skip to names table");

		final int [] namesTable = new int [UNIT_COUNT + 1];
		for (int unitNo = 1; unitNo <= UNIT_COUNT; unitNo++)
		{
			namesTable [unitNo] = StreamUtils.readUnsigned2ByteIntFromStream (exeStream, ByteOrder.LITTLE_ENDIAN, "Name offset");
			StreamUtils.readByteArrayFromStream (exeStream, UNIT_DATA_BLOCK_SIZE - 2, "Skip data block");
		}

		exeStream.reset ();

		// Now read each name
		StreamUtils.readByteArrayFromStream (exeStream, namesOffset, "Skip to names table");
		for (int unitNo = 1; unitNo <= UNIT_COUNT; unitNo++)
		{
			exeStream.mark (namesTable [unitNo] + 100);
			StreamUtils.readByteArrayFromStream (exeStream, namesTable [unitNo], "Skip to name " + unitNo + " of " + UNIT_COUNT);
			final String unitName = StreamUtils.readNullTerminatedFixedLengthStringFromStream (exeStream, 100, "Unit name " + unitNo + " of " + UNIT_COUNT);

			// Add to XML
			final Element unitElement = new Element (ServerEditorDatabaseConstants.TAG_ENTITY_UNIT);
			unitElement.setAttribute (ServerEditorDatabaseConstants.TAG_ATTRIBUTE_UNIT_ID, "UN" + StringUtils.padStart (new Integer (unitNo).toString (), "0", 3));

			final Element nameElement = new Element (ServerEditorDatabaseConstants.TAG_VALUE_UNIT_NAME);
			nameElement.setText (unitName);
			unitElement.addContent (nameElement);

			// Be careful about where we add it
			final int insertionPoint = XmlEditorUtils.determineElementInsertionPoint
				(getMdiEditor ().getXmlDocuments ().get (0).getTopLevelTypeDefinition (), getContainer (), ServerEditorDatabaseConstants.TAG_ENTITY_UNIT);
			getContainer ().addContent (insertionPoint, unitElement);

			// Position back to name offset location
			exeStream.reset ();
		}
	}

}
