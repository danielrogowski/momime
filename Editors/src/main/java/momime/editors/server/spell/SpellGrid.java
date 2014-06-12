package momime.editors.server.spell;

import java.util.List;

import com.ndg.xmleditor.editor.XmlEditorException;
import com.ndg.xmleditor.grid.XmlEditorGrid;
import com.ndg.xmleditor.grid.column.XmlGridColumn;

/**
 * Adds a special column listing out all the units that a spell can summon
 */
public final class SpellGrid extends XmlEditorGrid
{

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
}
