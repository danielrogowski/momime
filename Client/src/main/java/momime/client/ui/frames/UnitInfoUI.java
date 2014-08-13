package momime.client.ui.frames;

import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.WindowConstants;

import momime.client.MomClient;
import momime.client.ui.panels.UnitInfoPanel;
import momime.common.messages.v0_9_5.MemoryUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Most of the guts of the unit info screen is handled by UnitInfoPanel.
 * This UI just sorts out the button actions and keeps track of which unit info panels are open. 
 */
public final class UnitInfoUI extends MomClientFrameUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (UnitInfoUI.class);

	/** Multiplayer client */
	private MomClient client;
	
	/** Overland map UI */
	private OverlandMapUI overlandMapUI;
	
	/** Unit info panel */
	private UnitInfoPanel unitInfoPanel;
	
	/** The unit being displayed */
	private MemoryUnit unit;
	
	/** OK (close) action */
	private Action okAction;

	/** Dismiss action */
	private Action dismissAction;

	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init: " + getUnit ().getUnitURN ());
		
		// Actions
		okAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				getFrame ().dispose ();
			}
		};
		
		// Only show the dismiss button for our own units
		if (getUnit ().getOwningPlayerID () == getClient ().getOurPlayerID ())
		{
			dismissAction = new AbstractAction ()
			{
				@Override
				public final void actionPerformed (final ActionEvent ev)
				{
				}
			};
			
			getUnitInfoPanel ().setActions (new Action [] {dismissAction, okAction});
		}
		else
			getUnitInfoPanel ().setActions (new Action [] {okAction});
		
		// Initialize the frame
		final UnitInfoUI ui = this;
		getFrame ().setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		getFrame ().addWindowListener (new WindowAdapter ()
		{
			@Override
			public final void windowClosed (final WindowEvent ev)
			{
				getUnitInfoPanel ().unitInfoPanelClosing ();
				getLanguageChangeMaster ().removeLanuageChangeListener (ui);
				getClient ().getUnitInfos ().remove (getUnit ().getUnitURN ());
			}
		});
		
		// Do this "too early" on purpose, so that the window isn't centred over the map, but is a little down-right of it
		getFrame ().setLocationRelativeTo (getOverlandMapUI ().getFrame ());
		
		// Finish setting up the panel and frame
		getUnitInfoPanel ().setButtonsPositionRight (true);
		getFrame ().setContentPane (getUnitInfoPanel ().getPanel ());
		getUnitInfoPanel ().showUnit (getUnit ());
		getFrame ().setResizable (false);	// Must turn resizeable off before calling pack, so pack uses the size for the correct type of window decorations
		getFrame ().pack ();
		
		log.trace ("Exiting init");
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged: " + getUnit ().getUnitURN ());

		okAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmUnitInfo", "OK"));
		if (dismissAction != null)
			dismissAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmUnitInfo", "Dismiss"));

		// Copy the unit name from the panel
		getFrame ().setTitle (getUnitInfoPanel ().getCurrentlyConstructingName ());
		
		log.trace ("Exiting languageChanged");
	}

	/**
	 * @return Multiplayer client
	 */
	public final MomClient getClient ()
	{
		return client;
	}
	
	/**
	 * @param obj Multiplayer client
	 */
	public final void setClient (final MomClient obj)
	{
		client = obj;
	}

	/**
	 * @return Overland map UI
	 */
	public final OverlandMapUI getOverlandMapUI ()
	{
		return overlandMapUI;
	}

	/**
	 * @param ui Overland map UI
	 */
	public final void setOverlandMapUI (final OverlandMapUI ui)
	{
		overlandMapUI = ui;
	}
	
	/**
	 * @return Unit info panel
	 */
	public final UnitInfoPanel getUnitInfoPanel ()
	{
		return unitInfoPanel;
	}

	/**
	 * @param pnl Unit info panel
	 */
	public final void setUnitInfoPanel (final UnitInfoPanel pnl)
	{
		unitInfoPanel = pnl;
	}

	/**
	 * @return The unit being displayed
	 */
	public final MemoryUnit getUnit ()
	{
		return unit;
	}

	/**
	 * @param obj The unit being displayed
	 */
	public final void setUnit (final MemoryUnit obj)
	{
		unit = obj;
	}
}