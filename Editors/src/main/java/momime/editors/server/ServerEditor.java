package momime.editors.server;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Kicks off the XML editor by opening up the frame asking for the path to the XSD and XML files
 */
public final class ServerEditor
{
	/**
	 * @param args Command line arguments, ignored
	 */
	@SuppressWarnings ("resource")
	public final static void main (final String [] args)
	{
		System.setProperty ("log4j.configuration", "file:MoMIMEEditorsLogging.properties");
		
		// Start up the editor via Spring
		new ClassPathXmlApplicationContext ("/momime.editors.spring/server-editor-beans.xml");
	}
}