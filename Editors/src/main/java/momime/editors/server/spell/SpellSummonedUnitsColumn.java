package momime.editors.server.spell;

import java.util.Iterator;
import java.util.List;

import javax.swing.table.TableRowSorter;

import momime.editors.server.ServerEditorDatabaseConstants;

import org.jdom.Element;

import com.ndg.xml.JdomUtils;
import com.ndg.xmleditor.constants.XsdConstants;
import com.ndg.xmleditor.editor.XmlDocument;
import com.ndg.xmleditor.grid.XmlTableModel;
import com.ndg.xmleditor.grid.column.XmlGridColumn;

/**
 * Special column listing out all the units that a spell can summon
 */
public class SpellSummonedUnitsColumn extends XmlGridColumn
{
	/**
	 * Creates a special column listing out all the units that a spell can summon
	 * @param aTypeDefinition The xsd:complexType node of the entity being edited from the XSD
	 * @param aXmlDocuments A list of the main XML document being edited, plus any referenced documents
	 */
	public SpellSummonedUnitsColumn (final Element aTypeDefinition, final List <XmlDocument> aXmlDocuments)
	{
		super (aTypeDefinition, aXmlDocuments);
	}

	/**
	 * @return The width to display this column
	 */
	@Override
	public int getColumnWidth ()
	{
		return 150;
	}

	/**
	 * @return The title to display in the header above this column
	 */
	@Override
	public String getColumnHeading ()
	{
		return "Summoned Unit(s)";
	}

	/**
	 * @return Simple data type of this column - not necessarily resolved its ultimate simple type - so this may be e.g. momimesvr:description, rather than xsd:string
	 */
	@Override
	public String getColumnType ()
	{
		return XsdConstants.VALUE_DATA_TYPE_SINGLE_LINE_STRING;
	}

	/**
	 * @param record The XML node representing this record
     * @param rowIndex The row whose value is to be queried (most implementations can ignore this)
     * @param columnIndex The column whose value is to be queried (most implementations can ignore this)
     * @param tableSorter The sort/filter used in displaying the data (most implementations can ignore this)
	 * @return The column value to display for this record
	 */
	@Override
	public String getColumnValue (final Element record, final int rowIndex, final int columnIndex, final TableRowSorter<XmlTableModel> tableSorter)
	{
		String list = "";

		@SuppressWarnings ("rawtypes")
		final Iterator picks = record.getChildren (ServerEditorDatabaseConstants.TAG_CHILD_ENTITY_SPELL_SUMMONED_UNIT).iterator ();
		while (picks.hasNext ())
		{
			final Element thisUnit = (Element) picks.next ();

			final String unitId = thisUnit.getAttributeValue (ServerEditorDatabaseConstants.TAG_ATTRIBUTE_SUMMONED_UNIT_ID);

			final Element unitNode = JdomUtils.findDomChildNodeWithTextAttribute (getXmlDocuments ().get (0).getXml (),
					ServerEditorDatabaseConstants.TAG_ENTITY_UNIT, ServerEditorDatabaseConstants.TAG_ATTRIBUTE_UNIT_ID, unitId);

			String unitName;
			if (unitNode == null)
				unitName = unitId;
			else
				unitName = unitNode.getChildText (ServerEditorDatabaseConstants.TAG_VALUE_UNIT_NAME);

			// Add to list
			if (!list.equals (""))
				list = list + ", ";

			list = list + unitName;
		}

		return list;
	}

}
