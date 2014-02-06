package momime.editors.client.graphics;

import java.io.IOException;
import java.util.List;

import org.jdom.Element;

import com.ndg.xmleditor.editor.ComplexTypeReference;
import com.ndg.xmleditor.editor.XmlEditorException;
import com.ndg.xmleditor.editor.XmlEditorMain;
import com.ndg.xmleditor.grid.XmlEditorGrid;
import com.ndg.xmleditor.grid.column.XmlGridColumn;

/**
 * Adds a special column displaying an image from the classpath
 */
public final class XmlEditorGridWithImages extends XmlEditorGrid
{
	/**
	 * Creates a new form with a grid showing one type of record from the XML file
	 * @param anEntityElement The xsd:element node of the entity being edited from the XSD, i.e. for a top level entity, this will be a the entry under the xsd:sequence under the database complex type
	 * @param aTypeDefinition The xsd:complexType node of the entity being edited from the XSD
	 * @param aParentRecord If this is a child entity, this value holds the parent record; if this is a top level entity, this value will be null
	 * @param aParentEntityElements Array of xsd:element entries that have been drilled down to to reach here - earliest parent first in the list, immediate parent to entity last in the list - or null if this is a top level entity
	 * @param aParentTypeDefinitions Array of type definitions that have been drilled down to to reach here - earliest parent first in the list, immediate parent to entity last in the list - or null if this is a top level entity
	 * @param aMdiEditor The main MDI window
	 * @param filenameElements The elements containing the filenames of the images to display
	 * @param rowHeight A row height to use, appropriate for the type of images being displayed 
	 * @throws XmlEditorException If there a syntax problem parsing the XSD
	 * @throws IOException If there is a problem loading the button images
	 */
	public XmlEditorGridWithImages (final Element anEntityElement, final ComplexTypeReference aTypeDefinition,
		final Element aParentRecord, final Element [] aParentEntityElements, final ComplexTypeReference [] aParentTypeDefinitions, final XmlEditorMain aMdiEditor,
		final String [] filenameElements, final int rowHeight)
		throws XmlEditorException, IOException
	{
		super (anEntityElement, aTypeDefinition, aParentRecord, aParentEntityElements, aParentTypeDefinitions, aMdiEditor, filenameElements);
		table.setRowHeight (rowHeight);
	}

	
	/**
	 * Adds a special column displaying an image from the classpath
	 * @param param Misc parameter passed down from constructor
	 * @return The list of columns to display in the grid
	 * @throws XmlEditorException If there a syntax problem parsing the XSD
	 */
	@Override
	protected List<XmlGridColumn> buildColumnsList (final Object param)
		throws XmlEditorException
	{
		final List<XmlGridColumn> columns = super.buildColumnsList (param);

		final String [] filenameElements = (String []) param;
		for (final String filenameElement : filenameElements)
			columns.add (new ImageColumn (getTypeDefinition ().getComplexTypeDefinition (), getMdiEditor ().getXmlDocuments (), filenameElement));

		return columns;
	}
}
