package momime.client.ui.frames;

import java.awt.Dimension;
import java.awt.Polygon;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.Action;
import javax.swing.WindowConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.actions.LoggingAction;

import momime.client.MomClient;
import momime.client.language.replacer.UnitStatsLanguageVariableReplacer;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.ui.panels.UnitInfoPanel;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.client.utils.WizardClientUtils;
import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MemoryUnit;

/**
 * Most of the guts of the unit info screen is handled by UnitInfoPanel.
 * This UI just sorts out the button actions and keeps track of which unit info panels are open. 
 */
public final class UnitInfoUI extends MomClientFrameUI
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (UnitInfoUI.class);

	/** Multiplayer client */
	private MomClient client;
	
	/** Client-side unit utils */
	private UnitClientUtils unitClientUtils;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Wizard client utils */
	private WizardClientUtils wizardClientUtils;
	
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
		// Actions
		// Only show the dismiss button for our own units
		if (getUnit ().getOwningPlayerID () == getClient ().getOurPlayerID ())
		{
			dismissAction = new LoggingAction ((ev) ->
			{
				// Show name of unit
				String text = getLanguageHolder ().findDescription (getLanguages ().getUnitInfoScreen ().getDismissPrompt ());
				getUnitStatsReplacer ().setUnit (getUnitInfoPanel ().getUnit ());
				text = getUnitStatsReplacer ().replaceVariables (text);
				
				// Show message box
				final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
				msg.setLanguageTitle (getLanguages ().getUnitInfoScreen ().getDismissTitle ());
				msg.setText (text);
				msg.setUnitToDismiss (getUnit ());
				msg.setVisible (true);
			});
			
			getUnitInfoPanel ().getActions ().add (dismissAction);
			
			// Only show the rename button for our own heroes
			if (getClient ().getClientDB ().findUnit
				(getUnit ().getUnitID (), "UnitInfoUI").getUnitMagicRealm ().equals (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
			{
				renameAction = new LoggingAction ((ev) ->
				{
					final EditStringUI msg = getPrototypeFrameCreator ().createEditString ();
					msg.setLanguageTitle (getLanguages ().getUnitInfoScreen ().getRename ());
					msg.setLanguagePrompt (getLanguages ().getUnitInfoScreen ().getRenamePrompt ());
					msg.setUnitBeingNamed (getUnit ().getUnitURN ());
					msg.setText (getUnitClientUtils ().getUnitName (getUnit (), UnitNameType.SIMPLE_UNIT_NAME));
					msg.setVisible (true);
				});

				getUnitInfoPanel ().getActions ().add (renameAction);
			}			
		}

		okAction = new LoggingAction ((ev) -> getFrame ().dispose ());
		
		getUnitInfoPanel ().getActions ().add (okAction);
		
		// Initialize the frame
		final UnitInfoUI ui = this;
		getFrame ().setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		getFrame ().addWindowListener (new WindowAdapter ()
		{
			@Override
			public final void windowClosed (@SuppressWarnings ("unused") final WindowEvent ev)
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
	}
	
	/**
	 * Close the unit info screen when a unit dies 
	 */
	public final void close ()
	{
		getFrame ().dispose ();
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		okAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getSimple ().getOk ()));
		if (dismissAction != null)
			dismissAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getUnitInfoScreen ().getDismiss ()));

		if (renameAction != null)
			renameAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getUnitInfoScreen ().getRename ()));
		
		try
		{
			String unitName = getLanguageHolder ().findDescription (getLanguages ().getUnitInfoScreen ().getTitle ()).replaceAll
				("UNIT_NAME", getUnitClientUtils ().getUnitName (getUnit (), UnitNameType.RACE_UNIT_NAME));
			
			final PlayerPublicDetails owningPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getUnit ().getOwningPlayerID ());
			if (owningPlayer != null)
				unitName = unitName.replaceAll ("PLAYER_NAME", getWizardClientUtils ().getPlayerName (owningPlayer));
			
			getFrame ().setTitle (unitName);
		}
		catch (final RecordNotFoundException e)
		{
			log.error (e, e);
		}
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
	 * @return Session utils
	 */
	public final MultiplayerSessionUtils getMultiplayerSessionUtils ()
	{
		return multiplayerSessionUtils;
	}

	/**
	 * @param util Session utils
	 */
	public final void setMultiplayerSessionUtils (final MultiplayerSessionUtils util)
	{
		multiplayerSessionUtils = util;
	}

	/**
	 * @return Wizard client utils
	 */
	public final WizardClientUtils getWizardClientUtils ()
	{
		return wizardClientUtils;
	}

	/**
	 * @param util Wizard client utils
	 */
	public final void setWizardClientUtils (final WizardClientUtils util)
	{
		wizardClientUtils = util;
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