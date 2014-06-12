package momime.editors.server.wizardPickCount;

import java.util.List;

import com.ndg.xmleditor.editor.XmlEditorException;
import com.ndg.xmleditor.grid.XmlEditorGrid;
import com.ndg.xmleditor.grid.column.XmlGridColumn;

/**
 * Adds a special column showing all the picks for a particular pick count, rather than having to drill down to view them
 */
public final class WizardPickCountGrid extends XmlEditorGrid
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

		columns.add (new WizardPicksForCountColumn (getTypeDefinition ().getComplexTypeDefinition (), getMdiEditor ().getXmlDocuments ()));

		return columns;
	}
}
