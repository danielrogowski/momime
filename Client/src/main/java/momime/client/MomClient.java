package momime.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.logging.LogManager;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import momime.client.ui.MainMenuUI;
import momime.common.MomCommonConstants;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Main class to kickstart client
 */
public final class MomClient
{
	/** Main menu with options to connect to a server and create or join games */
	private MainMenuUI mainMenuUI;
	
	/**
	 * Kick off method invoked by spring's init-method
	 */
	public final void start ()
	{
		// Use Nimbus look and feel
		try
		{
		    for (final LookAndFeelInfo info : UIManager.getInstalledLookAndFeels ())
		    {
		        if ("Nimbus".equals (info.getName ()))
		        {
		            UIManager.setLookAndFeel (info.getClassName ());
		            break;
		        }
		    }
		}
		catch (final Exception e)
		{
		}
		
		// To be correct, should start up the first Swing frame in the Swing thread
		SwingUtilities.invokeLater (new Runnable ()
		{
			@Override
			public void run ()
			{
				try
				{
					getMainMenuUI ().setVisible (true);
				}
				catch (final IOException e)
				{
					e.printStackTrace ();
				}
			}
		});
	}

	/**
	 * @return Main menu with options to connect to a server and create or join games
	 */
	public final MainMenuUI getMainMenuUI ()
	{
		return mainMenuUI;
	}

	/**
	 * @param ui Main menu with options to connect to a server and create or join game
	 */
	public final void setMainMenuUI (final MainMenuUI ui)
	{
		mainMenuUI = ui;
	}
	
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
