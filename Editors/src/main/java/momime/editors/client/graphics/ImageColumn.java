package momime.editors.client.graphics;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.table.TableRowSorter;
import javax.xml.namespace.QName;

import org.jdom.Element;

import com.ndg.xmleditor.editor.XmlDocument;
import com.ndg.xmleditor.grid.XmlTableModel;
import com.ndg.xmleditor.grid.column.XmlGridColumn;
import com.ndg.xmleditor.schema.TopLevelComplexTypeEx;

/**
 * Column that displays an image from the classpath
 */
public final class ImageColumn extends XmlGridColumn
{
	/** The element containing the filename of the image to display */
	private final String filenameElement;
	
	/**
	 * Creates a special column that displays an image from the classpath
	 * @param aTypeDefinition The xsd:complexType node of the entity being edited from the XSD
	 * @param aXmlDocuments A list of the main XML document being edited, plus any referenced documents
	 * @param aFilenameElement The element containing the filename of the image to display
	 */
	public ImageColumn (final TopLevelComplexTypeEx aTypeDefinition, final List <XmlDocument> aXmlDocuments, final String aFilenameElement)
	{
		super (aTypeDefinition, aXmlDocuments);
		filenameElement = aFilenameElement;
	}
	
	/**
	 * @return The title to display in the header above this column
	 */
	@Override
	public final String getColumnHeading ()
	{
		return "Image";
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
		return null;
	}

	/**
	 * @return Class hint, so the correct rendeder can be selected
	 */
	@Override
	public final Class<?> getColumnClass ()
	{
		return ImageIcon.class;
	}
	
	/**
	 * @param record The XML node representing this record
     * @param rowIndex The row whose value is to be queried (most implementations can ignore this)
     * @param columnIndex The column whose value is to be queried (most implementations can ignore this)
     * @param tableSorter The sort/filter used in displaying the data (most implementations can ignore this)
	 * @return The column value to display for this record
	 */
	@Override
	public final Object getColumnValueObj (final Element record, final int rowIndex, final int columnIndex, final TableRowSorter<XmlTableModel> tableSorter)
	{
		final String filename;
		if (getTypeDefinition () == null)
			filename = record.getText ();
		else
			filename = record.getChildText (filenameElement);		
		
		ImageIcon icon = null;
		if (filename != null)
			try
			{
				try (final InputStream in = getClass ().getResourceAsStream (filename))
				{
					if (in != null)
					{
						final BufferedImage image = ImageIO.read (in);
						icon = new ImageIcon (image);
					}
				}
			}
			catch (final IOException e)
			{
				e.printStackTrace ();;
			}
		
		return icon;
	}
}