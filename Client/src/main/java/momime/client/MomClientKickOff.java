package momime.client;

import java.security.InvalidParameterException;

import momime.common.MomCommonConstants;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * The MomClientImpl class declares a log, so as soon as the classloader touches it, it fires up log4j.
 * Therefore we have to put the 'main' method in a separate class that doesn't declare a log, so that
 * we get a chance to correctly configure log4j before Spring fires up.
 */
public final class MomClientKickOff
{
	/**
	 * @param args Command line arguments, first param can set the config location
	 */
	@SuppressWarnings ("resource")
	public final static void main (final String [] args)
	{
		try
		{
			// Allow reading config from the 'client' folder after the app is assembled, but the root folder when in Eclipse
			final String configDir = (args.length == 0) ? "" : (args [0] + "/");
			System.setProperty ("configDir", configDir);
			
			// Ensure v1.7 JVM
			final String [] javaVersion = System.getProperty ("java.version").split ("\\.");
			final int majorVersion = Integer.parseInt (javaVersion [0]);
			final int minorVersion = Integer.parseInt (javaVersion [1]);

			if ((majorVersion < MomCommonConstants.JAVA_REQUIRED_MAJOR_VERSION) ||
				((majorVersion == MomCommonConstants.JAVA_REQUIRED_MAJOR_VERSION) && (minorVersion < MomCommonConstants.JAVA_REQUIRED_MINOR_VERSION)))
				
				throw new InvalidParameterException ("MoM IME requires a Java Virtual Machine version " +
					MomCommonConstants.JAVA_REQUIRED_MAJOR_VERSION + "." + MomCommonConstants.JAVA_REQUIRED_MINOR_VERSION +
					" or newer to run, but only detected version " + majorVersion + "." + minorVersion);
			
			// Initialize logging first, in case debug logging for spring itself is enabled
			System.setProperty ("log4j.configuration", "file:" + configDir + "MoMIMEClientLogging.properties");

			// Everything is now set to start with spring
			new ClassPathXmlApplicationContext ("/momime.client.spring/momime-client-beans.xml");			
		}
		catch (final Exception e)
		{
			System.out.println ("Exception in main method:");
			e.printStackTrace ();
		}
	}
}