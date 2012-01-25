package momime.editors.server.pickTypeCount;

import java.io.IOException;
import java.util.List;

import momime.editors.server.ServerEditorDatabaseConstants;

import org.jdom.Element;

import com.ndg.xmleditor.editor.ComplexTypeReference;
import com.ndg.xmleditor.editor.XmlEditorException;
import com.ndg.xmleditor.editor.XmlEditorMain;
import com.ndg.xmleditor.grid.XmlEditorGrid;
import com.ndg.xmleditor.grid.column.XmlGridColumn;

/**
 * Adds two special columns which shows how many spells we get available and for free in our spell book by choosing a certain number of spell books at the start of the game
 */
public class PickTypeCountGrid extends XmlEditorGrid
{
	/**
	 * Creates a grid two special columns which shows how many spells we get available and for free in our spell book by choosing a certain number of spell books at the start of the game
	 * @param anEntityElement The xsd:element node of the entity being edited from the XSD, i.e. for a top level entity, this will be a the entry under the xsd:sequence under the database complex type
	 * @param aTypeDefinition The xsd:complexType node of the entity being edited from the XSD
	 * @param aParentRecord If this is a child entity, this value holds the parent record; if this is a top level entity, this value will be null
	 * @param aParentEntityElements Array of xsd:element entries that have been drilled down to to reach here - earliest parent first in the list, immediate parent to entity last in the list - or null if this is a top level entity
	 * @param aParentTypeDefinitions Array of type definitions that have been drilled down to to reach here - earliest parent first in the list, immediate parent to entity last in the list - or null if this is a top level entity
	 * @param aMdiEditor The main MDI window
	 * @throws XmlEditorException If there a syntax problem parsing the XSD
	 * @throws IOException If there is a problem loading the button images
	 */
	public PickTypeCountGrid (final Element anEntityElement, final ComplexTypeReference aTypeDefinition,
		final Element aParentRecord, final Element [] aParentEntityElements, final ComplexTypeReference [] aParentTypeDefinitions, final XmlEditorMain aMdiEditor)
		throws XmlEditorException, IOException
	{
		super (anEntityElement, aTypeDefinition, aParentRecord, aParentEntityElements, aParentTypeDefinitions, aMdiEditor);
	}

	/**
	 * Adds a special column showing all the picks for a particular pick count, rather than having to drill down to view them
	 * @return The list of columns to display in the grid
	 * @throws XmlEditorException If there a syntax problem parsing the XSD
	 */
	@Override
	protected List<XmlGridColumn> buildColumnsList ()
		throws XmlEditorException
	{
		final List<XmlGridColumn> columns = super.buildColumnsList ();

		columns.add (new SpellsFromPickTypeCountColumn (getTypeDefinition ().getComplexTypeDefinition (), getMdiEditor ().getXmlDocuments (), "Spells Available",
			ServerEditorDatabaseConstants.TAG_VALUE_SPELLS_AVAILABLE));

		columns.add (new SpellsFromPickTypeCountColumn (getTypeDefinition ().getComplexTypeDefinition (), getMdiEditor ().getXmlDocuments (), "Spells Free at Start",
			ServerEditorDatabaseConstants.TAG_VALUE_SPELLS_FREE_AT_START));

		return columns;
	}
}
