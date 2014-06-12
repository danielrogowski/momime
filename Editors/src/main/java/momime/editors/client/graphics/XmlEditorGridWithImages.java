package momime.editors.client.graphics;

import java.io.IOException;
import java.util.List;

import com.ndg.xmleditor.editor.XmlEditorException;
import com.ndg.xmleditor.grid.XmlEditorGrid;
import com.ndg.xmleditor.grid.column.XmlGridColumn;

/**
 * Adds a special column displaying an image from the classpath
 */
public final class XmlEditorGridWithImages extends XmlEditorGrid
{
	/** Height to give each row in the table - varies, depending on the typical images referenced in this table */
	private int rowHeight;
	
	/** List of fields/elements in this table that contain image filenames, that we want to add image columns for */
	private List<String> filenameElements;
	
	/**
	 * Creates a new form with a grid showing one type of record from the XML file
	 * @throws XmlEditorException If there a syntax problem parsing the XSD
	 * @throws IOException If there is a problem loading the button images
	 */
	@Override
	public final void init () throws XmlEditorException, IOException
	{
		super.init ();
		table.setRowHeight (rowHeight);
	}

	/**
	 * Adds a special column displaying an image from the classpath
	 * @return The list of columns to display in the grid
	 * @throws XmlEditorException If there a syntax problem parsing the XSD
	 */
	@Override
	protected final List<XmlGridColumn> buildColumnsList ()
		throws XmlEditorException
	{
		final List<XmlGridColumn> columns = super.buildColumnsList ();

		for (final String filenameElement : filenameElements)
			columns.add (new ImageColumn (getTypeDefinition ().getComplexTypeDefinition (), getMdiEditor ().getXmlDocuments (), filenameElement));

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
	 * @return List of fields/elements in this table that contain image filenames, that we want to add image columns for
	 */
	public final List<String> getFilenameElements ()
	{
		return filenameElements;
	}

	/**
	 * @param elems List of fields/elements in this table that contain image filenames, that we want to add image columns for
	 */
	public final void setFilenameElements (final List<String> elems)
	{
		filenameElements = elems;
	}
}