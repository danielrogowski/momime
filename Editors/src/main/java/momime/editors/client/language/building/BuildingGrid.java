package momime.editors.client.language.building;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

import javax.swing.JFileChooser;

import momime.editors.client.language.MoMLanguageEditorGridWithImport;
import momime.editors.server.ServerEditorDatabaseConstants;

import org.jdom.Element;

import com.ndg.archive.LbxArchiveReader;
import com.ndg.swing.filefilters.SpecificFilenameFilter;
import com.ndg.utils.StreamUtils;
import com.ndg.utils.StringUtils;
import com.ndg.xmleditor.editor.ComplexTypeReference;
import com.ndg.xmleditor.editor.XmlEditorException;
import com.ndg.xmleditor.editor.XmlEditorMain;
import com.ndg.xmleditor.editor.XmlEditorUtils;

/**
 * Grid for displaying and editing building names and descriptions
 * Allows importing building help text from the original MoM LBXes
 */
public class BuildingGrid extends MoMLanguageEditorGridWithImport
{
	/**
	 * Creates a grid for displaying and editing building names and descriptions
	 * @param anEntityElement The xsd:element node of the entity being edited from the XSD, i.e. for a top level entity, this will be a the entry under the xsd:sequence under the database complex type
	 * @param aTypeDefinition The xsd:complexType node of the entity being edited from the XSD
	 * @param aParentRecord If this is a child entity, this value holds the parent record; if this is a top level entity, this value will be null
	 * @param aParentEntityElements Array of xsd:element entries that have been drilled down to to reach here - earliest parent first in the list, immediate parent to entity last in the list - or null if this is a top level entity
	 * @param aParentTypeDefinitions Array of type definitions that have been drilled down to to reach here - earliest parent first in the list, immediate parent to entity last in the list - or null if this is a top level entity
	 * @param aMdiEditor The main MDI window
	 * @throws XmlEditorException If there a syntax problem parsing the XSD
	 * @throws IOException If there is a problem loading the button images
	 */
	public BuildingGrid (final Element anEntityElement, final ComplexTypeReference aTypeDefinition,
		final Element aParentRecord, final Element [] aParentEntityElements, final ComplexTypeReference [] aParentTypeDefinitions, final XmlEditorMain aMdiEditor)
		throws XmlEditorException, IOException
	{
		super (anEntityElement, aTypeDefinition, aParentRecord, aParentEntityElements, aParentTypeDefinitions, aMdiEditor);
	}

	/**
	 * Specify the filename we're looking for
	 * @param lbxChooser The file open dialog
	 */
	@Override
	protected void addOtherFilters (final JFileChooser lbxChooser)
	{
		lbxChooser.addChoosableFileFilter (new SpecificFilenameFilter ("BUILDESC.LBX", "Original Master of Magic building descriptions (BUILDESC.LBX)"));
	};

	/**
	 * Imports building descriptions from BUILDESC.LBX
	 * @param lbxFilename The lbx filename chosen in the file open dialog
	 * @throws IOException If there is a problem reading the LBX file
	 * @throws XmlEditorException If there is a problem with one of the XML editor helper methods
	 */
	@Override
	protected void importFromLbx (final File lbxFilename)
		throws IOException, XmlEditorException
	{
		// BUILDESC.LBX only has a single subfile in it
		final InputStream lbxStream = LbxArchiveReader.getSubFileInputStream (new FileInputStream (lbxFilename), 0);

		// Read number and size of records
		final int numberOfRecords = StreamUtils.readUnsigned2ByteIntFromStream (lbxStream, ByteOrder.LITTLE_ENDIAN, "Number of Records");
		final int recordSize = StreamUtils.readUnsigned2ByteIntFromStream (lbxStream, ByteOrder.LITTLE_ENDIAN, "Record Size");

		// Read each record
		for (int recordNo = 0; recordNo < numberOfRecords; recordNo++)
		{
			final String buildingHelpText = StreamUtils.readNullTerminatedFixedLengthStringFromStream (lbxStream, recordSize, "Building " + recordNo + " of " + numberOfRecords + " Help Text");

			// Add to XML
			final Element buildingElement = new Element (ServerEditorDatabaseConstants.TAG_ENTITY_BUILDING);
			buildingElement.setAttribute (ServerEditorDatabaseConstants.TAG_ATTRIBUTE_BUILDING_ID, "BL" + StringUtils.padStart (new Integer (recordNo).toString (), "0", 2));

			final Element helpTextElement = new Element (ServerEditorDatabaseConstants.TAG_VALUE_BUILDING_HELP_TEXT);
			helpTextElement.setText (buildingHelpText);
			buildingElement.addContent (helpTextElement);

			// Be careful about where we add it
			final int insertionPoint = XmlEditorUtils.determineElementInsertionPoint
				(getMdiEditor ().getXmlDocuments ().get (0).getTopLevelTypeDefinition (), getContainer (), ServerEditorDatabaseConstants.TAG_ENTITY_BUILDING);
			getContainer ().addContent (insertionPoint, buildingElement);
		}
	}

}
