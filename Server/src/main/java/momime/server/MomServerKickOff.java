package momime.server;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.ndg.utils.ProcessUtilsImpl;

import momime.common.MomCommonConstants;

/**
 * The MomServer class declares a log, so as soon as the classloader touches it, it fires up log4j.
 * Therefore we have to put the 'main' method in a separate class that doesn't declare a log, so that
 * we get a chance to correctly configure log4j before Spring fires up.
 */
public final class MomServerKickOff
{
	/**
	 * @param args Command line arguments, first param can set the config location
	 */
	@SuppressWarnings ("resource")
	public final static void main (final String [] args)
	{
		try
		{
			// Allow reading config from the 'server' folder after the app is assembled, but the root folder when in Eclipse
			final String configDir = (args.length == 0) ? "" : (args [0] + "/");
			System.setProperty ("configDir", configDir);
			
			// Ensure minimum required JVM version
			new ProcessUtilsImpl ().ensureMinimumJavaVersion ("MoM IME Server", MomCommonConstants.JAVA_REQUIRED_VERSION);
			
			// Initialize logging first, in case debug logging for spring itself is enabled
			System.setProperty ("log4j.configurationFile", "" + configDir + "MoMIMEServerLogging.xml");

			// Everything is now set to start with spring
			new ClassPathXmlApplicationContext ("/momime.server.spring/momime-server-beans.xml");			
		}
		catch (final Exception e)
		{
			System.out.println ("Exception in main method:");
			e.printStackTrace ();
		}
	}
}