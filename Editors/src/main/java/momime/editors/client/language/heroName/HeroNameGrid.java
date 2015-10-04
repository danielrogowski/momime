package momime.editors.client.language.heroName;

import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.jdom.Element;

import com.ndg.archive.LbxArchiveReader;
import com.ndg.swing.GridBagConstraintsHorizontalFill;
import com.ndg.swing.GridBagConstraintsNoFill;
import com.ndg.swing.filefilters.SpecificFilenameFilter;
import com.ndg.utils.StreamUtils;
import com.ndg.utils.StringUtils;
import com.ndg.xml.JdomUtils;
import com.ndg.xmleditor.doc.ComplexTypeReference;
import com.ndg.xmleditor.editor.XmlEditorException;

import momime.editors.MoMEditorGridWithImport;
import momime.editors.server.ServerEditorDatabaseConstants;

/**
 * Grid for displaying and editing hero names
 * Allows importing hero names from the original MoM LBXes
 */
public final class HeroNameGrid extends MoMEditorGridWithImport
{
	/** Conjunction to use if hero class starts with a vowel, e.g. l' for Tumu l'Assassin */
	private JTextField vowelConjunction;

	/** Conjunction to use if hero is male, e.g. le for Gunther le Barbare */
	private JTextField maleConjunction;

	/** Conjunction to use if hero is female, e.g. la for Valana la Barde */
	private JTextField femaleConjunction;

	/**
	 * Creates a grid for displaying and editing hero names names
	 * @throws XmlEditorException If there a syntax problem parsing the XSD
	 * @throws IOException If there is a problem loading the button images
	 */
	@Override
	public final void init () throws XmlEditorException, IOException
	{
		super.init ();
		
		// Overall heading
		final JLabel headingLabel = new JLabel ("For use with Import button:");
		headingLabel.setFont (headingLabel.getFont ().deriveFont (Font.BOLD));
		getButtonPanel ().add (headingLabel, getUtils ().createConstraintsNoFill (0, getButtonPanelY (), 2, 1, INSET, GridBagConstraintsNoFill.WEST));
		setButtonPanelY (getButtonPanelY () + 1);

		// Conjunction if starts with vowel
		final JLabel vowelLabel = new JLabel ("Conjunction if starts with vowel:");
		getButtonPanel ().add (vowelLabel, getUtils ().createConstraintsNoFill (0, getButtonPanelY (), 2, 1, INSET, GridBagConstraintsNoFill.WEST));
		setButtonPanelY (getButtonPanelY () + 1);

		vowelConjunction = new JTextField (" the ");
		getButtonPanel ().add (vowelConjunction, getUtils ().createConstraintsHorizontalFill (0, getButtonPanelY (), 2, 1, INSET, GridBagConstraintsHorizontalFill.CENTRE));
		setButtonPanelY (getButtonPanelY () + 1);

		// Conjunction if male
		final JLabel maleLabel = new JLabel ("Conjunction if male:");
		getButtonPanel ().add (maleLabel, getUtils ().createConstraintsNoFill (0, getButtonPanelY (), 2, 1, INSET, GridBagConstraintsNoFill.WEST));
		setButtonPanelY (getButtonPanelY () + 1);

		maleConjunction = new JTextField (" the ");
		getButtonPanel ().add (maleConjunction, getUtils ().createConstraintsHorizontalFill (0, getButtonPanelY (), 2, 1, INSET, GridBagConstraintsHorizontalFill.CENTRE));
		setButtonPanelY (getButtonPanelY () + 1);

		// Conjunction if female
		final JLabel femaleLabel = new JLabel ("Conjunction if female:");
		getButtonPanel ().add (femaleLabel, getUtils ().createConstraintsNoFill (0, getButtonPanelY (), 2, 1, INSET, GridBagConstraintsNoFill.WEST));
		setButtonPanelY (getButtonPanelY () + 1);

		femaleConjunction = new JTextField (" the ");
		getButtonPanel ().add (femaleConjunction, getUtils ().createConstraintsHorizontalFill (0, getButtonPanelY (), 2, 1, INSET, GridBagConstraintsHorizontalFill.CENTRE));
		setButtonPanelY (getButtonPanelY () + 1);
	}

	/**
	 * Specify the filename we're looking for
	 * @param lbxChooser The file open dialog
	 */
	@Override
	protected final void addOtherFilters (final JFileChooser lbxChooser)
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
	protected final void importFromLbx (final File lbxFilename)
		throws IOException, XmlEditorException
	{
		// NAMES.LBX only has a single subfile in it
		try (final InputStream lbxStream = new FileInputStream (lbxFilename))
		{
			LbxArchiveReader.positionToSubFile (lbxStream, 0);

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
					final int insertionPoint = getMdiEditor ().getXmlDocuments ().determineElementInsertionPoint
						(new ComplexTypeReference (getMdiEditor ().getXmlDocuments ().get (0), getMdiEditor ().getXmlDocuments ().get (0).getXsd ().getTopLevelTypeDefinition ()),
						getContainer (), ServerEditorDatabaseConstants.TAG_ENTITY_HERO);
					getContainer ().addContent (insertionPoint, heroElement);
				}
			
			lbxStream.close ();
		}
	}
}