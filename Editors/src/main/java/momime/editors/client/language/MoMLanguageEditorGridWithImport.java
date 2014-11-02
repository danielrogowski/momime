package momime.editors.client.language;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import com.ndg.swing.filefilters.ExtensionFileFilter;
import com.ndg.xmleditor.editor.XmlEditorException;
import com.ndg.xmleditor.grid.XmlEditorGridWithImport;

/**
 * Shows a file open dialog asking for the location of an LBX file to import when the Import button is clicked
 */
public abstract class MoMLanguageEditorGridWithImport extends XmlEditorGridWithImport
{
	/**
	 * Shows a file open dialog asking for the location of an LBX file to import when the Import button is clicked
	 * @throws XmlEditorException If there a syntax problem parsing the XSD
	 * @throws IOException If there is a problem loading the button images
	 */
	@Override
	public void init () throws XmlEditorException, IOException
	{
		super.init ();
		
		// Import action
		setImportAction (new AbstractAction ()
		{
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