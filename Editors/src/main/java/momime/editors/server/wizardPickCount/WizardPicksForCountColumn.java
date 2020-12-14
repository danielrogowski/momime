package momime.editors.server.wizardPickCount;

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
 * Special column showing all the picks for a particular pick count, rather than having to drill down to view them
 */
public final class WizardPicksForCountColumn extends XmlGridColumn
{
	/**
	 * Creates a special column showing all the picks for a particular pick count, rather than having to drill down to view them
	 * @param aTypeDefinition The xsd:complexType node of the entity being edited from the XSD
	 * @param aXmlDocuments A list of the main XML document being edited, plus any referenced documents
	 */
	public WizardPicksForCountColumn (final ComplexTypeEx aTypeDefinition, final ListOfXmlDocuments aXmlDocuments)
	{
		super (aTypeDefinition, aXmlDocuments);
	}

	/**
	 * @return The width to display this column
	 */
	@Override
	public final int getColumnWidth ()
	{
		return 500;
	}

	/**
	 * @return The title to display in the header above this column
	 */
	@Override
	public final String getColumnHeading ()
	{
		return "Picks";
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
		final Iterator picks = record.getChildren (ServerEditorDatabaseConstants.TAG_GRANDCHILD_ENTITY_WIZARD_PICK).iterator ();
		while (picks.hasNext ())
		{
			final Element thisPick = (Element) picks.next ();

			// Pick
			final String pickId = thisPick.getAttributeValue (ServerEditorDatabaseConstants.TAG_ATTRIBUTE_PICK_ID);
			final Element pickNode = JdomUtils.findDomChildNodeWithTextAttribute (getXmlDocuments ().get (0).getXml (),
				ServerEditorDatabaseConstants.TAG_ENTITY_PICK, ServerEditorDatabaseConstants.TAG_ATTRIBUTE_PICK_ID, pickId);

			String pickDescription = "";
			if (pickNode != null)
			{
				final Element description = pickNode.getChild (ServerEditorDatabaseConstants.TAG_VALUE_PICK_DESCRIPTION_SINGULAR);
				if (description != null)
					pickDescription = description.getChildText (ServerEditorDatabaseConstants.TAG_VALUE_TEXT);
			}

			// Quantity
			final String quantity = thisPick.getChildText (ServerEditorDatabaseConstants.TAG_VALUE_QUANTITY);
			if (quantity != null)
				if (!quantity.equals ("1"))
					pickDescription = quantity + "x " + pickDescription + "s";

			// Add to list
			if (!list.equals (""))
				list = list + ", ";

			list = list + pickDescription;
		}

		return list;
	}
}