package momime.editors.server.building;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

import javax.swing.JFileChooser;

import org.jdom2.Element;

import com.ndg.archive.LbxArchiveReader;
import com.ndg.utils.StreamUtils;
import com.ndg.utils.StringUtils;
import com.ndg.utils.swing.filefilters.SpecificFilenameFilter;
import com.ndg.xmleditor.doc.ComplexTypeReference;
import com.ndg.xmleditor.editor.XmlEditorException;

import momime.editors.grid.MoMEditorGridWithImport;
import momime.editors.server.ServerEditorDatabaseConstants;

/**
 * Grid for displaying and editing building names and descriptions
 * Allows importing building help text from the original MoM LBXes
 */
public final class BuildingGrid extends MoMEditorGridWithImport
{
	/**
	 * Specify the filename we're looking for
	 * @param lbxChooser The file open dialog
	 */
	@Override
	protected final void addOtherFilters (final JFileChooser lbxChooser)
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
	protected final void importFromLbx (final File lbxFilename)
		throws IOException, XmlEditorException
	{
		// BUILDESC.LBX only has a single subfile in it
		try (final InputStream lbxStream = new FileInputStream (lbxFilename))
		{
			LbxArchiveReader.positionToSubFile (lbxStream, 0);
			
			// Read number and size of records
			final int numberOfRecords = StreamUtils.readUnsigned2ByteIntFromStream (lbxStream, ByteOrder.LITTLE_ENDIAN, "Number of Records");
			final int recordSize = StreamUtils.readUnsigned2ByteIntFromStream (lbxStream, ByteOrder.LITTLE_ENDIAN, "Record Size");

			// Read each record
			for (int recordNo = 0; recordNo < numberOfRecords; recordNo++)
			{
				final String buildingHelpText = StreamUtils.readNullTerminatedFixedLengthStringFromStream (lbxStream, recordSize, "Building " + recordNo + " of " + numberOfRecords + " Help Text");

				// Add to XML
				final Element buildingElement = new Element (ServerEditorDatabaseConstants.TAG_ENTITY_BUILDING);
				buildingElement.setAttribute (ServerEditorDatabaseConstants.TAG_ATTRIBUTE_BUILDING_ID, "BL" + StringUtils.padStart (Integer.valueOf (recordNo).toString (), "0", 2));

				final Element helpTextElement = new Element (ServerEditorDatabaseConstants.TAG_VALUE_BUILDING_HELP_TEXT);
				helpTextElement.setText (buildingHelpText);
				buildingElement.addContent (helpTextElement);

				// Be careful about where we add it
				final int insertionPoint = getMdiEditor ().getXmlDocuments ().determineElementInsertionPoint
					(new ComplexTypeReference (getMdiEditor ().getXmlDocuments ().get (0), getMdiEditor ().getXmlDocuments ().get (0).getXsd ().getTopLevelTypeDefinition ()),
					getContainer (), ServerEditorDatabaseConstants.TAG_ENTITY_BUILDING);
				getContainer ().addContent (insertionPoint, buildingElement);
			}
			
			lbxStream.close ();
		}
	}
}