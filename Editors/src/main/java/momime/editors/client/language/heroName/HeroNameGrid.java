package momime.editors.client.language.heroName;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;

import momime.editors.client.language.MoMLanguageEditorGridWithImport;
import momime.editors.server.ServerEditorDatabaseConstants;

import org.jdom.Element;

import com.ndg.archive.LbxArchiveReader;
import com.ndg.swing.SwingLayoutConstants;
import com.ndg.swing.filefilters.SpecificFilenameFilter;
import com.ndg.utils.StreamUtils;
import com.ndg.utils.StringUtils;
import com.ndg.xml.JdomUtils;
import com.ndg.xmleditor.editor.ComplexTypeReference;
import com.ndg.xmleditor.editor.XmlEditorException;
import com.ndg.xmleditor.editor.XmlEditorMain;
import com.ndg.xmleditor.editor.XmlEditorUtils;

/**
 * Grid for displaying and editing hero names names
 * Allows importing hero names from the original MoM LBXes
 */
public class HeroNameGrid extends MoMLanguageEditorGridWithImport
{
	/**
	 * Conjunction to use if hero class starts with a vowel, e.g. l' for Tumu l'Assassin
	 */
	private final JTextField vowelConjunction;

	/**
	 * Conjunction to use if hero is male, e.g. le for Gunther le Barbare
	 */
	private final JTextField maleConjunction;

	/**
	 * Conjunction to use if hero is female, e.g. la for Valana la Barde
	 */
	private final JTextField femaleConjunction;

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
	public HeroNameGrid (final Element anEntityElement, final ComplexTypeReference aTypeDefinition,
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

		// Conjunction if starts with vowel
		final JLabel vowelLabel = new JLabel ("Conjunction if starts with vowel:");
		vowelLabel.setPreferredSize (size);
		vowelLabel.setMinimumSize (size);
		vowelLabel.setMaximumSize (size);
		vowelLabel.setAlignmentX (Component.LEFT_ALIGNMENT);
		vowelLabel.setAlignmentY (Component.TOP_ALIGNMENT);
		getButtonPanel ().add (vowelLabel);

		vowelConjunction = new JTextField (" the ");
		vowelConjunction.setPreferredSize (size);
		vowelConjunction.setMinimumSize (size);
		vowelConjunction.setMaximumSize (size);
		vowelConjunction.setAlignmentX (Component.LEFT_ALIGNMENT);
		vowelConjunction.setAlignmentY (Component.TOP_ALIGNMENT);
		getButtonPanel ().add (vowelConjunction);

		// Conjunction if male
		final JLabel maleLabel = new JLabel ("Conjunction if male:");
		maleLabel.setPreferredSize (size);
		maleLabel.setMinimumSize (size);
		maleLabel.setMaximumSize (size);
		maleLabel.setAlignmentX (Component.LEFT_ALIGNMENT);
		maleLabel.setAlignmentY (Component.TOP_ALIGNMENT);
		getButtonPanel ().add (maleLabel);

		maleConjunction = new JTextField (" the ");
		maleConjunction.setPreferredSize (size);
		maleConjunction.setMinimumSize (size);
		maleConjunction.setMaximumSize (size);
		maleConjunction.setAlignmentX (Component.LEFT_ALIGNMENT);
		maleConjunction.setAlignmentY (Component.TOP_ALIGNMENT);
		getButtonPanel ().add (maleConjunction);

		// Conjunction if female
		final JLabel femaleLabel = new JLabel ("Conjunction if female:");
		femaleLabel.setPreferredSize (size);
		femaleLabel.setMinimumSize (size);
		femaleLabel.setMaximumSize (size);
		femaleLabel.setAlignmentX (Component.LEFT_ALIGNMENT);
		femaleLabel.setAlignmentY (Component.TOP_ALIGNMENT);
		getButtonPanel ().add (femaleLabel);

		femaleConjunction = new JTextField (" the ");
		femaleConjunction.setPreferredSize (size);
		femaleConjunction.setMinimumSize (size);
		femaleConjunction.setMaximumSize (size);
		femaleConjunction.setAlignmentX (Component.LEFT_ALIGNMENT);
		femaleConjunction.setAlignmentY (Component.TOP_ALIGNMENT);
		getButtonPanel ().add (femaleConjunction);
	}

	/**
	 * Specify the filename we're looking for
	 * @param lbxChooser The file open dialog
	 */
	@Override
	protected void addOtherFilters (final JFileChooser lbxChooser)
	{
		lbxChooser.addChoosableFileFilter (new SpecificFilenameFilter ("NAMES.LBX", "Original Master of Magic hero names (NAMES.LBX)"));
	};

	/**
	 * Imports hero names descriptions from NAMES.LBX
	 * @param lbxFilename The lbx filename chosen in the file open dialog
	 * @throws IOException If there is a problem reading the LBX file
	 * @throws XmlEditorException If there is a problem with one of the XML editor helper methods
	 */
	@Override
	protected void importFromLbx (final String lbxFilename)
		throws IOException, XmlEditorException
	{
		// NAMES.LBX only has a single subfile in it
		final InputStream lbxStream = LbxArchiveReader.getSubFileInputStream (lbxFilename, 0);

		// Read number and size of records
		StreamUtils.readUnsigned2ByteIntFromStream (lbxStream, ByteOrder.LITTLE_ENDIAN, "Number of Records");
		final int recordSize = StreamUtils.readUnsigned2ByteIntFromStream (lbxStream, ByteOrder.LITTLE_ENDIAN, "Record Size");

		for (int heroNameNo = 1; heroNameNo <= 5; heroNameNo++)
			for (int unitNo = 1; unitNo <= 35; unitNo++)
			{
				String heroName = StreamUtils.readNullTerminatedFixedLengthStringFromStream (lbxStream, recordSize, "Name " + heroNameNo + " for hero " + unitNo);

				// Look up the hero class, e.g. Dwarf, Assassin, Chosen
				final String unitId = "UN" + StringUtils.padStart (new Integer (unitNo).toString (), "0", 3);
				final String heroClass = JdomUtils.findDomChildNodeWithTextAttribute (getContainer (), ServerEditorDatabaseConstants.TAG_ENTITY_UNIT,
					ServerEditorDatabaseConstants.TAG_ATTRIBUTE_UNIT_ID, unitId).getChildText (ServerEditorDatabaseConstants.TAG_VALUE_UNIT_NAME);

				final String firstChar = heroClass.substring (0, 1).toUpperCase ();
				if ("AEIOU".indexOf (firstChar) >= 0)
					heroName = heroName + vowelConjunction.getText () + heroClass;

				else if ((unitNo ==6) || (unitNo ==8) || (unitNo ==9) || (unitNo ==10) || (unitNo ==19) || (unitNo ==23) || (unitNo ==26) || (unitNo ==28) || (unitNo ==31))
					heroName = heroName + femaleConjunction.getText () + heroClass;

				else
					heroName = heroName + maleConjunction.getText () + heroClass;

				// Add to XML
				final Element heroElement = new Element (ServerEditorDatabaseConstants.TAG_ENTITY_HERO);
				heroElement.setAttribute (ServerEditorDatabaseConstants.TAG_ATTRIBUTE_HERO_NAME_ID, unitId + "_HN0" + heroNameNo);

				final Element heroNameElement = new Element (ServerEditorDatabaseConstants.TAG_VALUE_HERO_NAME);
				heroNameElement.setText (heroName);
				heroElement.addContent (heroNameElement);

				// Be careful about where we add it
				final int insertionPoint = XmlEditorUtils.determineElementInsertionPoint
					(getMdiEditor ().getXmlDocuments ().get (0).getTopLevelTypeDefinition (), getContainer (), ServerEditorDatabaseConstants.TAG_ENTITY_HERO);
				getContainer ().addContent (insertionPoint, heroElement);
			}
	}

}
