package momime.client.ui.frames;

import java.awt.Dimension;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.WindowConstants;

import momime.client.MomClient;
import momime.client.language.replacer.UnitStatsLanguageVariableReplacer;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.ui.panels.UnitInfoPanel;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MemoryUnit;

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
	
	/** Client-side unit utils */
	private UnitClientUtils unitClientUtils;
	
	/** Unit info panel */
	private UnitInfoPanel unitInfoPanel;
	
	/** Variable replacer for outputting skill descriptions */
	private UnitStatsLanguageVariableReplacer unitStatsReplacer;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** The unit being displayed */
	private MemoryUnit unit;
	
	/** OK (close) action */
	private Action okAction;

	/** Dismiss action */
	private Action dismissAction;

	/** Rename action */
	private Action renameAction;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init: " + getUnit ().getUnitURN ());
		
		// Actions
		// Only show the dismiss button for our own units
		if (getUnit ().getOwningPlayerID () == getClient ().getOurPlayerID ())
		{
			dismissAction = new AbstractAction ()
			{
				@Override
				public final void actionPerformed (final ActionEvent ev)
				{
					// Show name of unit
					String text = getLanguage ().findCategoryEntry ("frmUnitInfo", "DismissPrompt");
					getUnitStatsReplacer ().setUnit (getUnit ());
					text = getUnitStatsReplacer ().replaceVariables (text);
					
					// Show message box
					final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
					msg.setTitleLanguageCategoryID ("frmUnitInfo");
					msg.setTitleLanguageEntryID ("DismissTitle");
					msg.setText (text);
					msg.setUnitToDismiss (getUnit ());
					try
					{
						msg.setVisible (true);
					}
					catch (final IOException e)
					{
						log.error (e, e);
					}
				}
			};
			
			getUnitInfoPanel ().getActions ().add (dismissAction);
			
			// Only show the rename button for our own heroes
			if (getClient ().getClientDB ().findUnit
				(getUnit ().getUnitID (), "UnitInfoUI").getUnitMagicRealm ().equals (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
			{
				renameAction = new AbstractAction ()
				{
					@Override
					public final void actionPerformed (final ActionEvent ev)
					{
						final EditStringUI msg = getPrototypeFrameCreator ().createEditString ();
						msg.setTitleLanguageCategoryID ("frmUnitInfo");
						msg.setTitleLanguageEntryID ("Rename");
						msg.setPromptLanguageCategoryID ("frmUnitInfo");
						msg.setPromptLanguageEntryID ("RenamePrompt");
						msg.setUnitBeingNamed (getUnit ().getUnitURN ());
						
						try
						{
							msg.setText (getUnitClientUtils ().getUnitName (getUnit (), UnitNameType.SIMPLE_UNIT_NAME));
							msg.setVisible (true);
						}
						catch (final IOException e)
						{
							log.error (e, e);
						}
					}
				};

				getUnitInfoPanel ().getActions ().add (renameAction);
			}			
		}

		okAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				getFrame ().dispose ();
			}
		};
		getUnitInfoPanel ().getActions ().add (okAction);
		
		// Initialize the frame
		final UnitInfoUI ui = this;
		getFrame ().setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		getFrame ().addWindowListener (new WindowAdapter ()
		{
			@Override
			public final void windowClosed (final WindowEvent ev)
			{
				try
				{
					getUnitInfoPanel ().unitInfoPanelClosing ();
				}
				catch (final MomException e)
				{
					log.error (e, e);
				}
				
				getLanguageChangeMaster ().removeLanguageChangeListener (ui);
				getClient ().getUnitInfos ().remove (getUnit ().getUnitURN ());
			}
		});
		
		// Finish setting up the panel and frame
		getUnitInfoPanel ().setButtonsPositionRight (true);
		getFrame ().setContentPane (getUnitInfoPanel ().getPanel ());
		getUnitInfoPanel ().showUnit (getUnit ());
		getFrame ().setResizable (false);
		getFrame ().setUndecorated (true);
		
		final Dimension panelSize = getUnitInfoPanel ().getPanel ().getPreferredSize ();
		getFrame ().setShape (new Polygon
			(new int [] {0, panelSize.width - getUnitInfoPanel ().getBackgroundButtonsWidth (), panelSize.width - getUnitInfoPanel ().getBackgroundButtonsWidth (), panelSize.width, panelSize.width, 0},
			new int [] {0, 0, panelSize.height - getUnitInfoPanel ().getBackgroundButtonsHeight (), panelSize.height - getUnitInfoPanel ().getBackgroundButtonsHeight (), panelSize.height, panelSize.height},
			6));
		
		log.trace ("Exiting init");
	}
	
	/**
	 * Close the unit info screen when a unit dies 
	 */
	public final void close ()
	{
		log.trace ("Entering close: " + getUnit ().getUnitURN ());
		
		getFrame ().dispose ();

		log.trace ("Exiting close: " + getUnit ().getUnitURN ());
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

		if (renameAction != null)
			renameAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmUnitInfo", "Rename"));
		
		try
		{
			getFrame ().setTitle (getUnitClientUtils ().getUnitName (getUnit (), UnitNameType.RACE_UNIT_NAME));
		}
		catch (final RecordNotFoundException e)
		{
			log.error (e, e);
		}
		
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
	 * @return Client-side unit utils
	 */
	public final UnitClientUtils getUnitClientUtils ()
	{
		return unitClientUtils;
	}

	/**
	 * @param util Client-side unit utils
	 */
	public final void setUnitClientUtils (final UnitClientUtils util)
	{
		unitClientUtils = util;
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
	 * @return Variable replacer for outputting skill descriptions
	 */
	public final UnitStatsLanguageVariableReplacer getUnitStatsReplacer ()
	{
		return unitStatsReplacer;
	}

	/**
	 * @param replacer Variable replacer for outputting skill descriptions
	 */
	public final void setUnitStatsReplacer (final UnitStatsLanguageVariableReplacer replacer)
	{
		unitStatsReplacer = replacer;
	}
	
	/**
	 * @return Prototype frame creator
	 */
	public final PrototypeFrameCreator getPrototypeFrameCreator ()
	{
		return prototypeFrameCreator;
	}

	/**
	 * @param obj Prototype frame creator
	 */
	public final void setPrototypeFrameCreator (final PrototypeFrameCreator obj)
	{
		prototypeFrameCreator = obj;
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