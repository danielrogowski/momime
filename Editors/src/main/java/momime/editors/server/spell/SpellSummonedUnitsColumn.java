package momime.editors.server.spell;

import java.util.Iterator;

import javax.swing.table.TableRowSorter;
import javax.xml.namespace.QName;

import momime.editors.server.ServerEditorDatabaseConstants;

import org.jdom2.Element;

import com.ndg.xml.JdomUtils;
import com.ndg.xmleditor.constants.XsdConstants;
import com.ndg.xmleditor.doc.ListOfXmlDocuments;
import com.ndg.xmleditor.grid.XmlTableModel;
import com.ndg.xmleditor.grid.column.XmlGridColumn;
import com.ndg.xmleditor.schema.ComplexTypeEx;

/**
 * Special column listing out all the units that a spell can summon
 */
public final class SpellSummonedUnitsColumn extends XmlGridColumn
{
	/**
	 * Creates a special column listing out all the units that a spell can summon
	 * @param aTypeDefinition The xsd:complexType node of the entity being edited from the XSD
	 * @param aXmlDocuments A list of the main XML document being edited, plus any referenced documents
	 */
	public SpellSummonedUnitsColumn (final ComplexTypeEx aTypeDefinition, final ListOfXmlDocuments aXmlDocuments)
	{
		super (aTypeDefinition, aXmlDocuments);
	}

	/**
	 * @return The width to display this column
	 */
	@Override
	public final int getColumnWidth ()
	{
		return 150;
	}

	/**
	 * @return The title to display in the header above this column
	 */
	@Override
	public final String getColumnHeading ()
	{
		return "Summoned Unit(s)";
	}

	/**
	 * @return Simple data type of this column - not necessarily resolved its ultimate simple type - so this may be e.g. momimesvr:description, rather than xsd:string
	 */
	@Override
	public final QName getColumnType ()
	{
		return new QName (XsdConstants.XML_SCHEMA_XSD_NAMESPACE_URI, XsdConstants.VALUE_DATA_TYPE_SINGLE_LINE_STRING);
	}

	/**
	 * @param record The XML node representing this record
     * @param rowIndex The row whose value is to be queried (most implementations can ignore this)
     * @param columnIndex The column whose value is to be queried (most implementations can ignore this)
     * @param tableSorter The sort/filter used in displaying the data (most implementations can ignore this)
	 * @return The column value to display for this record
	 */
	@SuppressWarnings ("unused")
	@Override
	public final String getColumnValue (final Element record, final int rowIndex, final int columnIndex, final TableRowSorter<XmlTableModel> tableSorter)
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