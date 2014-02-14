package momime.client.ui;

import java.io.IOException;

import javax.swing.JFrame;

import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;

/**
 * Partial implementation of some of the methods from the MomClientUI interface.  Ancestor used by all of the UI screens.
 * 
 * The way this basically works is that, we aren't allowed to create/configure Swing components outside of the event dispatcher thread.
 * Spring will start up in the JVM's initial thread, so we can't create Swing components from Spring.  So instead Spring creates these
 * "container" objects and injects any resources like fonts that we might need, and injects the Language XML holder.
 * 
 * At some later point, the app then calls setVisible (true) for the first time, which must be done from the event dispatcher thread, at which
 * point init () gets called to create all the Swing components.  setVisible is done in such a way that the screen can be subsequently hidden
 * and then redisplayed, and the Swing components won't need to be recreated.
 */
public abstract class MomClientAbstractUI implements MomClientUI
{
	/** The actual frame */
	private JFrame frame;

	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Component responsible for controlling the selected language */
	private LanguageChangeMaster languageChangeMaster;
	
	/** Helper methods and constants for creating and laying out Swing components */
	private MomUIUtils utils;
	
	/**
	 * This is package-private so it can be accessed for setLocationRelativeTo () methods
	 * @return The actual frame
	 */
	final JFrame getFrame ()
	{
		return frame;
	}

	/**
	 * @return Whether this screen is currently displayed or not
	 */
	@Override
	public final boolean isVisible ()
	{
		// If haven't done init yet, then obviously it isn't visible
		return (frame == null) ? false : frame.isVisible ();
	}
	
	/**
	 * @param v Whether to display or hide this screen
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	public final void setVisible (final boolean v) throws IOException
	{
		if (frame == null)
		{
			frame = new JFrame ();
			init ();
			languageChanged ();
			getLanguageChangeMaster ().addLanuageChangeListener (this);
		}
		frame.setVisible (v);
	}
	
	/**
	 * Descendant classes put code here to create and lay out all the Swing components
	 * @throws IOException If a resource cannot be found
	 */
	protected abstract void init () throws IOException;

	/**
	 * @return Language database holder
	 */
	@Override
	public final LanguageDatabaseHolder getLanguageHolder ()
	{
		return languageHolder;
	}
	
	/**
	 * @param holder Language database holder
	 */
	@Override
	public final void setLanguageHolder (final LanguageDatabaseHolder holder)
	{
		languageHolder = holder;
	}

	/**
	 * Convenience shortcut for accessing the Language XML database
	 * @return Language database
	 */
	@Override
	public final LanguageDatabaseEx getLanguage ()
	{
		return languageHolder.getLanguage ();
	}

	/**
	 * @return Component responsible for controlling the selected language
	 */
	@Override
	public final LanguageChangeMaster getLanguageChangeMaster ()
	{
		return languageChangeMaster;
	}

	/**
	 * @param master Component responsible for controlling the selected language
	 */
	@Override
	public final void setLanguageChangeMaster (final LanguageChangeMaster master)
	{
		languageChangeMaster = master;
	}
	
	/**
	 * @return Helper methods and constants for creating and laying out Swing components
	 */
	public final MomUIUtils getUtils ()
	{
		return utils;
	}

	/**
	 * @param util Helper methods and constants for creating and laying out Swing components
	 */
	public final void setUtils (final MomUIUtils util)
	{
		utils = util;
	}
}
