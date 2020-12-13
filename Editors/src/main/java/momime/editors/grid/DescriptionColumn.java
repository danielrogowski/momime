package momime.editors.grid;

import javax.swing.table.TableRowSorter;
import javax.xml.namespace.QName;

import org.jdom2.Element;

import com.ndg.xmleditor.constants.XsdConstants;
import com.ndg.xmleditor.doc.ListOfXmlDocuments;
import com.ndg.xmleditor.grid.XmlTableModel;
import com.ndg.xmleditor.grid.column.XmlGridColumn;
import com.ndg.xmleditor.schema.ComplexTypeEx;

/**
 * Column that displays the first of a particular languageText element
 */
public final class DescriptionColumn extends XmlGridColumn
{
	/** The languageText element to display */
	private final String textElement;
	
	/**
	 * Creates a special column that displays an image from the classpath
	 * @param aTypeDefinition The xsd:complexType node of the entity being edited from the XSD
	 * @param aXmlDocuments A list of the main XML document being edited, plus any referenced documents
	 * @param aTextElement The languageText element to display
	 */
	public DescriptionColumn (final ComplexTypeEx aTypeDefinition, final ListOfXmlDocuments aXmlDocuments, final String aTextElement)
	{
		super (aTypeDefinition, aXmlDocuments);
		textElement = aTextElement;
	}

	/**
	 * @return The title to display in the header above this column
	 */
	@Override
	public final String getColumnHeading ()
	{
		return "Description";
	}

	/**
	 * @return The width to display this column
	 */
	@Override
	public final int getColumnWidth ()
	{
		return 200;
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
		String text = null;
		if (getTypeDefinition () == null)
			text = record.getText ();
		else
		{
			final Element element = record.getChild (textElement);
			if ((element != null) && (element.getChildren ().size () == 1))
				text = element.getChildren ().get (0).getText ();
		}		
		
		return text;
	}
}