package momime.editors.client.graphics;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;

import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.server.database.ServerXsdResourceResolver;

import org.jdom.input.SAXBuilder;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import com.ndg.xmleditor.editor.AskForXsdAndXmlFileLocations;
import com.ndg.xmleditor.editor.XmlEditorDispatcher;

/**
 * Kicks off the XML editor by opening up the frame asking for the path to the XSD and XML files
 */
public final class GraphicsEditor
{
	/**
	 * Kicks off the XML editor by opening up the frame asking for the path to the XSD and XML files
	 * @param args Command line arguments - ignored
	 */
	public static void main (final String [] args)
	{
		// Switch to Windows look and feel if available, otherwise the open/save dialogs look gross
		try
		{
			UIManager.setLookAndFeel ("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		}
		catch (final Exception e)
		{
			// Don't worry if can't switch look and feel
		}

		// Open up the first frame
		// In the process, initialize both XML parsers and the specialised dispatcher
		try
		{
			// Set up resource resolver - used both by the schema factory and the XML Editor code to load in the referenced XSDs
			final ServerXsdResourceResolver resourceResolver = new ServerXsdResourceResolver (DOMImplementationRegistry.newInstance ());

			// Need to tweak the schema factory to use the resource resolver
			final SchemaFactory schemaFactory = SchemaFactory.newInstance (XMLConstants.W3C_XML_SCHEMA_NS_URI);
			schemaFactory.setResourceResolver (resourceResolver);

			// Show form asking for XSD and XML locations
			final AskForXsdAndXmlFileLocations ask = new AskForXsdAndXmlFileLocations
				(new SAXBuilder (), schemaFactory, new XmlEditorDispatcher (), resourceResolver);

			ask.setLoadXsdFromClasspath (true);
			ask.setXsdFilename (GraphicsDatabaseConstants.GRAPHICS_XSD_LOCATION);
		}
		catch (final Exception e)
		{
			JOptionPane.showMessageDialog (null, e.toString (), "MoM IME Language XML Editor", JOptionPane.ERROR_MESSAGE);
		}
	}

}
