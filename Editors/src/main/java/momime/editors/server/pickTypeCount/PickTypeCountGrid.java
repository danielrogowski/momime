package momime.editors.server.pickTypeCount;

import java.util.List;

import momime.editors.server.ServerEditorDatabaseConstants;

import com.ndg.xmleditor.editor.XmlEditorException;
import com.ndg.xmleditor.grid.XmlEditorGrid;
import com.ndg.xmleditor.grid.column.XmlGridColumn;

/**
 * Adds two special columns which shows how many spells we get available and for free in our spell book by choosing a certain number of spell books at the start of the game
 */
public final class PickTypeCountGrid extends XmlEditorGrid
{
	/**
	 * Adds a special column showing all the picks for a particular pick count, rather than having to drill down to view them
	 * @return The list of columns to display in the grid
	 * @throws XmlEditorException If there a syntax problem parsing the XSD
	 */
	@Override
	protected final List<XmlGridColumn> buildColumnsList ()
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