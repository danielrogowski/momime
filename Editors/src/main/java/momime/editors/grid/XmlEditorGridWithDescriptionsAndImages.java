package momime.editors.grid;

import java.io.IOException;
import java.util.List;

import com.ndg.xmleditor.editor.XmlEditorException;
import com.ndg.xmleditor.grid.XmlEditorGrid;
import com.ndg.xmleditor.grid.column.XmlGridColumn;

/**
 * Adds a special column displaying an image from the classpath
 */
public class XmlEditorGridWithDescriptionsAndImages extends XmlEditorGrid
{
	/** Height to give each row in the table - varies, depending on the typical images referenced in this table */
	private int rowHeight;

	/** List of child elements of type languageText to display the first description from */
	private List<String> descriptionColumns;
	
	/** List of fields/elements in this table that contain image filenames, that we want to add image columns for */
	private List<String> imageColumns;
	
	/** First index to add description columns; defaults to 0 but if there's a primary key-type attribute then we probably want the description in column 1 instead */
	private int firstDescriptionColumnIndex;
	
	/**
	 * Creates a new form with a grid showing one type of record from the XML file
	 * @throws XmlEditorException If there a syntax problem parsing the XSD
	 * @throws IOException If there is a problem loading the button images
	 */
	@Override
	public void init () throws XmlEditorException, IOException
	{
		super.init ();
		
		if (rowHeight > 0)
			getTable ().setRowHeight (rowHeight);
	}

	/**
	 * Adds special columns displaying descriptions and images
	 * @return The list of columns to display in the grid
	 * @throws XmlEditorException If there a syntax problem parsing the XSD
	 */
	@Override
	protected final List<XmlGridColumn> buildColumnsList ()
		throws XmlEditorException
	{
		final List<XmlGridColumn> columns = super.buildColumnsList ();

		// Add description columns at the start
		if (getDescriptionColumns () != null)
		{
			int n = getFirstDescriptionColumnIndex ();
			for (final String descriptionColumn : getDescriptionColumns ())
			{
				columns.add (n, new DescriptionColumn ((getTypeDefinition () == null) ? null : getTypeDefinition ().getComplexTypeDefinition (),
					getMdiEditor ().getXmlDocuments (), descriptionColumn));
				n++;
			}
		}
		
		// Add image columns at the end
		if (getImageColumns () != null)
			for (final String imageColumn : getImageColumns ())
				columns.add (new ImageColumn ((getTypeDefinition () == null) ? null : getTypeDefinition ().getComplexTypeDefinition (),
					getMdiEditor ().getXmlDocuments (), imageColumn));

		return columns;
	}

	/**
	 * @return Height to give each row in the table - varies, depending on the typical images referenced in this table
	 */
	public final int getRowHeight ()
	{
		return rowHeight;
	}

	/**
	 * @param height Height to give each row in the table - varies, depending on the typical images referenced in this table
	 */
	public final void setRowHeight (final int height)
	{
		rowHeight = height;
	}

	/**
	 * @return List of child elements of type languageText to display the first description from
	 */
	public final List<String> getDescriptionColumns ()
	{
		return descriptionColumns;
	}

	/**
	 * @param c List of child elements of type languageText to display the first description from
	 */
	public final void setDescriptionColumns (final List<String> c)
	{
		descriptionColumns = c;
	}
	
	/**
	 * @return List of fields/elements in this table that contain image filenames, that we want to add image columns for
	 */
	public final List<String> getImageColumns ()
	{
		return imageColumns;
	}
	
	/**
	 * @param c List of fields/elements in this table that contain image filenames, that we want to add image columns for
	 */
	public final void setImageColumns (final List<String> c)
	{
		imageColumns = c;
	}

	/**
	 * @return First index to add description columns; defaults to 0 but if there's a primary key-type attribute then we probably want the description in column 1 instead
	 */
	public final int getFirstDescriptionColumnIndex ()
	{
		return firstDescriptionColumnIndex;
	}

	/**
	 * @param i First index to add description columns; defaults to 0 but if there's a primary key-type attribute then we probably want the description in column 1 instead
	 */
	public final void setFirstDescriptionColumnIndex (final int i)
	{
		firstDescriptionColumnIndex = i;
	}
}