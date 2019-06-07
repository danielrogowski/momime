package momime.editors.client.graphics;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Kicks off the XML editor by opening up the frame asking for the path to the XSD and XML files
 */
public final class GraphicsEditor
{
	/**
	 * @param args Command line arguments, ignored
	 */
	@SuppressWarnings ("resource")
	public final static void main (final String [] args)
	{
		System.setProperty ("log4j.configurationFile", "MoMIMEEditorsLogging.xml");
		
		// Start up the editor via Spring
		new ClassPathXmlApplicationContext ("/momime.editors.spring/graphics-editor-beans.xml");
	}
}