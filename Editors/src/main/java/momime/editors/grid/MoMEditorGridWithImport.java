package momime.editors.grid;

import java.io.File;
import java.io.IOException;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;

import com.ndg.utils.swing.GridBagConstraintsHorizontalFill;
import com.ndg.utils.swing.actions.MessageDialogAction;
import com.ndg.utils.swing.filefilters.ExtensionFileFilter;
import com.ndg.xmleditor.editor.XmlEditorException;
import com.ndg.xmleditor.grid.XmlEditorGridWithDescriptionsAndImages;

/**
 * Shows a file open dialog asking for the location of an LBX file to import when the Import button is clicked.
 * 
 * This really needs to inherit from both XmlEditorGridWithDescriptionsAndImages and XmlEditorGridWithImport, but can't do that,
 * so just pulled in the code from XmlEditorGridWithImport to here.
 */
public abstract class MoMEditorGridWithImport extends XmlEditorGridWithDescriptionsAndImages
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
		final Action importAction = new MessageDialogAction ("Import", new ImageIcon (getUtils ().loadImage ("/com.ndg.xmleditor.icons/Import.gif")), (ev) ->
		{
			final JFileChooser lbxChooser = new JFileChooser ();
			lbxChooser.addChoosableFileFilter (new ExtensionFileFilter ("lbx", "Original Master of Magic LBX files"));
			addOtherFilters (lbxChooser);

			if (lbxChooser.showOpenDialog (null) == JFileChooser.APPROVE_OPTION)
			{
				importFromLbx (lbxChooser.getSelectedFile ());
				getTableModel ().fireTableDataChanged ();
			}
		});
		
		// Import button
		final JButton importButton = new JButton (importAction);
		getButtonPanel ().add (importButton, getUtils ().createConstraintsHorizontalFill (0, getButtonPanelY (), 2, 1, INSET, GridBagConstraintsHorizontalFill.CENTRE));
		setButtonPanelY (getButtonPanelY () + 1);
	}

	/**
	 * Descendant classes can override this if they wish to add any filters to the file open dialog
	 * @param lbxChooser The file open dialog
	 */
	@SuppressWarnings ("unused")
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