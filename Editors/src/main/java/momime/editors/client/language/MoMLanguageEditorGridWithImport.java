package momime.editors.client.language;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.jdom.Element;

import com.ndg.swing.filefilters.ExtensionFileFilter;
import com.ndg.xmleditor.editor.ComplexTypeReference;
import com.ndg.xmleditor.editor.XmlEditorException;
import com.ndg.xmleditor.editor.XmlEditorMain;
import com.ndg.xmleditor.grid.XmlEditorGridWithImport;

/**
 * Shows a file open dialog asking for the location of an LBX file to import when the Import button is clicked
 */
public abstract class MoMLanguageEditorGridWithImport extends XmlEditorGridWithImport
{
	/**
	 * Shows a file open dialog asking for the location of an LBX file to import when the Import button is clicked
	 * @param anEntityElement The xsd:element node of the entity being edited from the XSD, i.e. for a top level entity, this will be a the entry under the xsd:sequence under the database complex type
	 * @param aTypeDefinition The xsd:complexType node of the entity being edited from the XSD
	 * @param aParentRecord If this is a child entity, this value holds the parent record; if this is a top level entity, this value will be null
	 * @param aParentEntityElements Array of xsd:element entries that have been drilled down to to reach here - earliest parent first in the list, immediate parent to entity last in the list - or null if this is a top level entity
	 * @param aParentTypeDefinitions Array of type definitions that have been drilled down to to reach here - earliest parent first in the list, immediate parent to entity last in the list - or null if this is a top level entity
	 * @param aMdiEditor The main MDI window
	 * @throws XmlEditorException If there a syntax problem parsing the XSD
	 * @throws IOException If there is a problem loading the button images
	 */
	public MoMLanguageEditorGridWithImport (final Element anEntityElement, final ComplexTypeReference aTypeDefinition,
		final Element aParentRecord, final Element [] aParentEntityElements, final ComplexTypeReference [] aParentTypeDefinitions, final XmlEditorMain aMdiEditor)
		throws XmlEditorException, IOException
	{
		super (anEntityElement, aTypeDefinition, aParentRecord, aParentEntityElements, aParentTypeDefinitions, aMdiEditor, null);

		// Import action
		setImportAction (new AbstractAction ()
		{
			private static final long serialVersionUID = 4837848398404063304L;

			@Override
			public void actionPerformed (final ActionEvent event)
			{
				final JFileChooser lbxChooser = new JFileChooser ();
				lbxChooser.addChoosableFileFilter (new ExtensionFileFilter ("lbx", "Original Master of Magic LBX files"));
				addOtherFilters (lbxChooser);

				if (lbxChooser.showOpenDialog (null) == JFileChooser.APPROVE_OPTION)
					try
					{
						importFromLbx (lbxChooser.getSelectedFile ());
						getTableModel ().fireTableDataChanged ();
					}
					catch (final Exception e)
					{
						JOptionPane.showMessageDialog (null, e.toString (), "MoM IME Language Editor Import", JOptionPane.ERROR_MESSAGE);
					}
			}
		});
	}

	/**
	 * Descendant classes can override this if they wish to add any filters to the file open dialog
	 * @param lbxChooser The file open dialog
	 */
	protected void addOtherFilters (final JFileChooser lbxChooser)
	{
	};

	/**
	 * Descendant classes must override this to implement the import functionality
	 * @param lbxFilename The lbx filename chosen in the file open dialog
	 * @throws IOException If there is a problem reading the LBX file
	 * @throws XmlEditorException If there is a problem using helper methods from the XML editor
	 */
	protected abstract void importFromLbx (final File lbxFilename)
		throws IOException, XmlEditorException;
}
