package momime.editors.client.language;

import java.awt.event.ActionEvent;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;

import momime.server.database.ServerDatabaseConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Attribute;
import org.jdom.Element;

import com.ndg.xml.JdomUtils;
import com.ndg.xmleditor.editor.XmlDocument;
import com.ndg.xmleditor.editor.XmlEditorException;
import com.ndg.xmleditor.editor.XmlEditorMain;
import com.ndg.xmleditor.editor.XmlEditorUtils;
import com.ndg.xmleditor.schema.ComplexTypeEx;
import com.ndg.xmleditor.schema.TopLevelComplexTypeEx;

/**
 * Specialised main window for the language XML editor which adds an option to the File menu
 */
public final class LanguageEditorMain extends XmlEditorMain
{
	/** Class logger */
	private final Log log = LogFactory.getLog (LanguageEditorMain.class);

	/**
	 * Adds a special menu option
	 * @throws XmlEditorException If there are syntax problems with the XSD
	 */
	@Override
	public final void init () throws XmlEditorException
	{
		super.init ();

		// Add separator
		getFileMenu ().add (new JSeparator (), 0);

		// Special option to check all entries from the server XML file exist in this language XML file
		final LanguageEditorMain form = this;
		final Action checkAction = new AbstractAction ("Verify that all necessary entries are in the Language XML file")
		{
			@Override
			public void actionPerformed (final ActionEvent event)
			{
				try
				{
					final XmlDocument serverXml = XmlEditorUtils.findDocumentWithNamespaceURI (getXmlDocuments (), ServerDatabaseConstants.SERVER_XSD_NAMESPACE_URI);

					String valuesAdded = form.checkNode (serverXml.getXml (),
						getXmlDocuments ().get (0).getXml (), getXmlDocuments ().get (0).getXsd ().getTopLevelTypeDefinition ());

					if (valuesAdded.equals (""))
						valuesAdded = "Language XML file checks out OK against Server XML file - all necessary entries were already present.";
					else
						valuesAdded = "The following missing entries were added.  Ensure you re-load the editor after fixing these values up to prove all mandatory fields are present." + valuesAdded;

					JOptionPane.showMessageDialog (null, valuesAdded, FORM_TITLE, JOptionPane.INFORMATION_MESSAGE);
				}
				catch (final XmlEditorException e)
				{
					log.error (e, e);
					JOptionPane.showMessageDialog (null, e.toString (), FORM_TITLE, JOptionPane.ERROR_MESSAGE);
				}
			}
		};

		final JMenuItem checkOption = new JMenuItem ();
		checkOption.setAction (checkAction);
		getFileMenu ().add (checkOption, 0);
	}

	/**
	 * Checks all nodes under the parents
	 * @param serverContainer Container from the server XML file to search under
	 * @param languageContainer Container from the language XML file to search under
	 * @param containerTypeDefinition Type definition for the complex type in the language XSD which matches languageContainer (for top level entities, this is the complexType for "database")
	 * @return List of nodes added or empty string if none
	 * @throws XmlEditorException If there is an error parsing the XSD
	 */
	private final String checkNode (final Element serverContainer, final Element languageContainer, final ComplexTypeEx containerTypeDefinition)
		throws XmlEditorException
	{
		log.trace ("Entering checkNode: " + containerTypeDefinition + ", " + serverContainer + ", " + languageContainer);
				
		String result = "";

		@SuppressWarnings ("rawtypes")
		final Iterator serverNodes = serverContainer.getChildren ().iterator ();
		while (serverNodes.hasNext ())
		{
			final Element serverNode = (Element) serverNodes.next ();

			// Get the name of the entity, e.g. "unit" or "building"
			final String entityName = serverNode.getName ();

			// Find the definition for this entity
			// Firstly we need to prove that this is an entry which should be in the language XML file
			// Secondly we need this below if we need to check for child nodes
			// Also FKs are only supported with a single attribute
			TopLevelComplexTypeEx entityDefinition = null;
			try
			{
				entityDefinition = getXmlDocuments ().get (0).getXsd ().findTopLevelComplexType (entityName);
			}
			catch (final XmlEditorException e)
			{
				// That's fine, not all entries in the server XML are supposed to be in the language XML, e.g. taxRate
			}
			
			if ((entityDefinition != null) && (serverNode.getAttributes ().size () == 1))
			{
				final Attribute serverAttribute = (Attribute) serverNode.getAttributes ().get (0);

				// Look for a matching entry in the language XML file
				Element languageNode = JdomUtils.findDomChildNodeWithTextAttribute (languageContainer, entityName, serverAttribute.getName (), serverAttribute.getValue ());
				if (languageNode == null)
				{
					languageNode = new Element (entityName);
					languageNode.setAttribute (serverAttribute.getName (), serverAttribute.getValue ());

					final int insertionPoint = XmlEditorUtils.determineElementInsertionPoint (containerTypeDefinition, languageContainer, entityName);
					languageContainer.addContent (insertionPoint, languageNode);

					result = result + "\r\n" + entityName + " was missing key \"" + serverAttribute.getValue () + "\"";
				}

				// Nested loop to deal with experience levels, which are  stored under each unit type
				// and language entries which are also two tier
				if ((entityName.equals ("unitType")) || (entityName.equals ("languageCategory")))
					result = result + checkNode (serverNode, languageNode, entityDefinition);
			}
		}

		log.trace ("Exiting checkNode: " + result);
		return result;
	}
}