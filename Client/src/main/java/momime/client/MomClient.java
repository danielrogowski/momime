package momime.client;

import java.io.FileInputStream;
import java.security.InvalidParameterException;
import java.util.logging.LogManager;

import momime.common.MomCommonConstants;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Main class to kickstart client
 */
public final class MomClient
{
	/**
	 * @param args Command line arguments, ignored
	 */
	@SuppressWarnings ("resource")
	public final static void main (final String [] args)
	{
		try
		{
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
			try (final FileInputStream in = new FileInputStream ("MoMIMEClientLogging.properties"))
			{
				LogManager.getLogManager ().readConfiguration (in);
				in.close ();
			}

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
