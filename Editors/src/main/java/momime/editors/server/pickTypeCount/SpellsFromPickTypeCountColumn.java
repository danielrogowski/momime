package momime.editors.server.pickTypeCount;

import java.util.Iterator;

import javax.swing.table.TableRowSorter;
import javax.xml.namespace.QName;

import momime.editors.server.ServerEditorDatabaseConstants;

import org.jdom.Element;

import com.ndg.xml.JdomUtils;
import com.ndg.xmleditor.constants.XsdConstants;
import com.ndg.xmleditor.doc.ListOfXmlDocuments;
import com.ndg.xmleditor.grid.XmlTableModel;
import com.ndg.xmleditor.grid.column.XmlGridColumn;
import com.ndg.xmleditor.schema.ComplexTypeEx;

/**
 * Special column which shows how many spells we get available or for free in our spell book by choosing a certain number of spell books at the start of the game
 * Whether this displays available or free spells depends on the values passed into the constructor (saved having two separate and very similar classes)
 */
public final class SpellsFromPickTypeCountColumn extends XmlGridColumn
{
	/**
	 * The title to display in the header above this column
	 */
	private final String columnHeading;

	/**
	 * The XML tag for the field containing the quantity we want to display
	 */
	private final String fieldTag;

	/**
	 * Creates a special column which shows how many spells we get available or for free in our spell book by choosing a certain number of spell books at the start of the game
	 * @param aTypeDefinition The xsd:complexType node of the entity being edited from the XSD
	 * @param aXmlDocuments A list of the main XML document being edited, plus any referenced documents
	 * @param aColumnHeading The title to display in the header above this column
	 * @param aFieldTag The XML tag for the field containing the quantity we want to display
	 */
	public SpellsFromPickTypeCountColumn (final ComplexTypeEx aTypeDefinition, final ListOfXmlDocuments aXmlDocuments, final String aColumnHeading, final String aFieldTag)
	{
		super (aTypeDefinition, aXmlDocuments);

		columnHeading = aColumnHeading;
		fieldTag = aFieldTag;
	}

	/**
	 * @return The width to display this column
	 */
	@Override
	public final int getColumnWidth ()
	{
		return 300;
	}

	/**
	 * @return The title to display in the header above this column
	 */
	@Override
	public final String getColumnHeading ()
	{
		return columnHeading;
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
	@Override
	public final String getColumnValue (final Element record, final int rowIndex, final int columnIndex, final TableRowSorter<XmlTableModel> tableSorter)
	{
		String list = "";

		@SuppressWarnings ("rawtypes")
		final Iterator spellRanks = record.getChildren (ServerEditorDatabaseConstants.TAG_GRANDCHILD_ENTITY_SPELL_COUNT).iterator ();
		while (spellRanks.hasNext ())
		{
			final Element thisSpellRank = (Element) spellRanks.next ();

			// Quantity
			final String quantity = thisSpellRank.getChildText (fieldTag);
			if (quantity != null)
			{
				// Spell Rank
				final String spellRankId = thisSpellRank.getAttributeValue (ServerEditorDatabaseConstants.TAG_ATTRIBUTE_SPELL_COUNT_RANK);
				final Element spellRankNode = JdomUtils.findDomChildNodeWithTextAttribute (getXmlDocuments ().get (0).getXml (),
					ServerEditorDatabaseConstants.TAG_ENTITY_SPELL_RANK, ServerEditorDatabaseConstants.TAG_ATTRIBUTE_SPELL_RANK_ID, spellRankId);

				String spellRankDescription;
				if (spellRankNode == null)
					spellRankDescription = spellRankId;
				else
					spellRankDescription = spellRankNode.getChildText (ServerEditorDatabaseConstants.TAG_VALUE_SPELL_RANK_DESCRIPTION);

				// Add to list
				if (!list.equals (""))
					list = list + ", ";

				list = list + quantity + "x " + spellRankDescription;
			}
		}

		return list;
	}
}