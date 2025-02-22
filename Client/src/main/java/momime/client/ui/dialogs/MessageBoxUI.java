package momime.client.ui.dialogs;

import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.sessionbase.DeleteSavedGame;
import com.ndg.multiplayer.sessionbase.LeaveSession;
import com.ndg.utils.swing.actions.LoggingAction;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.languages.database.Shortcut;
import momime.client.newturnmessages.NewTurnMessageSpellEx;
import momime.client.process.OverlandMapProcessing;
import momime.client.ui.MomUIConstants;
import momime.client.ui.frames.MagicSlidersUI;
import momime.client.ui.frames.NewTurnMessagesUI;
import momime.client.ui.frames.WizardsUI;
import momime.common.database.LanguageText;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.clienttoserver.CancelTargetSpellMessage;
import momime.common.messages.clienttoserver.DismissUnitMessage;
import momime.common.messages.clienttoserver.HeroItemLocationID;
import momime.common.messages.clienttoserver.RequestCastSpellMessage;
import momime.common.messages.clienttoserver.RequestMoveHeroItemMessage;
import momime.common.messages.clienttoserver.RequestRemoveQueuedSpellMessage;
import momime.common.messages.clienttoserver.RequestResearchSpellMessage;
import momime.common.messages.clienttoserver.RequestSwitchOffMaintainedSpellMessage;
import momime.common.messages.clienttoserver.RushBuyMessage;
import momime.common.messages.clienttoserver.SellBuildingMessage;

/**
 * Modal dialog which displays a message with an OK button, or a Yes and No choice, with Yes taking some action
 */
public final class MessageBoxUI extends MomClientDialogUI
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (MessageBoxUI.class);
	
	/** XML layout */
	private XmlLayoutContainerEx messageBoxLayout;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Small font */
	private Font smallFont;
	
	/** New turn messages UI */
	private NewTurnMessagesUI newTurnMessagesUI;
	
	/** Turn sequence and movement helper methods */
	private OverlandMapProcessing overlandMapProcessing;

	/** Magic sliders screen */
	private MagicSlidersUI magicSlidersUI;
	
	/** Wizards UI */
	private WizardsUI wizardsUI;
	
	/** OK action */
	private Action okAction;

	/** No action */
	private Action noAction;

	/** Yes action */
	private Action yesAction;

	/** Message text */
	private JTextArea messageText;

	/** Language options to use for the title; null if title is not language-variable */
	private List<LanguageText> languageTitle;
	
	/** Language category ID to use for the message; null if message is not language-variable */
	private List<LanguageText> languageText;
	
	/** Fixed title that isn't lanuage variant; null if title is language variable */
	private String title;	
	
	/** Fixed text that isn't lanuage variant; null if message is language variable */
	private String text;
	
	/** The unit being dismissed; null if the message box isn't asking about dismissing a unit */
	private MemoryUnit unitToDismiss;
	
	/** The city to rush by at or sell a building at; null if the message box isn't about rush buying or selling a building */
	private MapCoordinates3DEx cityLocation;
	
	/** The building being sold; null if the message box isn't about selling a building */
	private Integer buildingURN;
	
	/** The spell we're trying to research; null if the message box isn't about researching a spell */
	private String researchSpellID;
	
	/** The spell we're trying to cast; null if the message box isn't about casting a spell */
	private String castSpellID;
	
	/** The NTM telling us to target the spell that we're cancelling; null if the message box isn't about cancelling targeting a spell */
	private NewTurnMessageSpellEx cancelTargetingSpell;
	
	/** Spell we're thinking of switching off; null if the message box isn't about switching off a spell */
	private MemoryMaintainedSpell switchOffSpell;
	
	/** Hero item we're thinking of destroying on the anvil; null if the message box isn't about destroying a hero item */
	private RequestMoveHeroItemMessage destroyHeroItemMessage;
	
	/** Saved game we're thinking of deleting; null if the message box isn't about deleting a saved game */
	private Integer savedGameID;
	
	/** Queued overland spell we're thinking of cancelling; null if the message box isn't about cancelling a queued overland spell */
	private Integer removeQueuedSpellIndex;
	
	/** True if message box is being showed to ask player if they want to leave session */
	private boolean leaveSession;
	
	/** Content pane */
	private JPanel contentPane;
	
	/**
	 * Sets up the dialog once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/messageBox498x100.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x18goldNormal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x18goldPressed.png");

		// Actions
		okAction = new LoggingAction ((ev) -> getDialog ().dispose ());
		
		noAction = new LoggingAction ((ev) ->
		{
			// Cancel cancel targeting a spell, leaving it on the NTM list to be retargeted again, but still we have to close out the "Target Spell" right hand panel
			if (getCancelTargetingSpell () != null)
				getOverlandMapProcessing ().updateMovementRemaining ();

			// In case targeting at an overland enchantment, reset magic screen back to normal; similar with wizards screen
			getMagicSlidersUI ().setTargetingSpell (null);
			getWizardsUI ().setTargetingSpell (null);
			getWizardsUI ().updateWizards (false);
			
			// Close the form
			getDialog ().dispose ();
		});
		
		yesAction = new LoggingAction ((ev) ->
		{
			// Dismiss a unit on overland map
			if (getUnitToDismiss () != null)
			{
				final DismissUnitMessage msg = new DismissUnitMessage ();
				msg.setUnitURN (getUnitToDismiss ().getUnitURN ());
				getClient ().getServerConnection ().sendMessageToServer (msg);
			}
			
			// Rush buy current construction project or sell a building
			else if (getCityLocation () != null)
			{
				if (getBuildingURN () == null)
				{
					final RushBuyMessage msg = new RushBuyMessage ();
					msg.setCityLocation (getCityLocation ());
					getClient ().getServerConnection ().sendMessageToServer (msg);
				}
				else
				{
					final SellBuildingMessage msg = new SellBuildingMessage ();
					msg.setCityLocation (getCityLocation ());
					msg.setBuildingURN (getBuildingURN ());
					getClient ().getServerConnection ().sendMessageToServer (msg);
				}
			}
			
			// Research a spell
			else if (getResearchSpellID () != null)
			{
				final RequestResearchSpellMessage msg = new RequestResearchSpellMessage ();
				msg.setSpellID (getResearchSpellID ());
				getClient ().getServerConnection ().sendMessageToServer (msg);
			}
			
			// Cast a spell
			else if (getCastSpellID () != null)
			{
				final RequestCastSpellMessage msg = new RequestCastSpellMessage ();
				msg.setSpellID (getCastSpellID ());
				getClient ().getServerConnection ().sendMessageToServer (msg);
			}
			
			// Cancel targeting a spell
			else if (getCancelTargetingSpell () != null)
			{
				// Mark the NTM as cancelled
				getCancelTargetingSpell ().setTargetingCancelled (true);
				
				// Redraw the NTMs
				getNewTurnMessagesUI ().languageChanged ();
				
				// Server will have spell listed in their Maintained Spells list, so tell the server to remove it
				final CancelTargetSpellMessage msg = new CancelTargetSpellMessage ();
				msg.setSpellID (getCancelTargetingSpell ().getSpellID ());
				getClient ().getServerConnection ().sendMessageToServer (msg);
				
				// Close out the "Target Spell" right hand panel
				getOverlandMapProcessing ().updateMovementRemaining ();
				
				// In case targeting at an overland enchantment, reset magic screen back to normal; similar with targeting at wizards
				getMagicSlidersUI ().setTargetingSpell (null);
				getWizardsUI ().setTargetingSpell (null);
				getWizardsUI ().updateWizards (false);
			}
			
			// Switch off a spell
			else if (getSwitchOffSpell () != null)
			{
				final RequestSwitchOffMaintainedSpellMessage msg = new RequestSwitchOffMaintainedSpellMessage ();
			    msg.setSpellURN (getSwitchOffSpell ().getSpellURN ());
			    getClient ().getServerConnection ().sendMessageToServer (msg);
			}
			
			// Destroy a hero item
			else if (getDestroyHeroItemMessage () != null)
			{
				// The "from" parts of the message must have already been filled out
				getDestroyHeroItemMessage ().setToLocation (HeroItemLocationID.DESTROY);
				getClient ().getServerConnection ().sendMessageToServer (getDestroyHeroItemMessage ());
			}
			
			// Delete saved game
			else if (getSavedGameID () != null)
			{
				final DeleteSavedGame msg = new DeleteSavedGame ();
			    msg.setSavedGameID (getSavedGameID ());
			    getClient ().getServerConnection ().sendMessageToServer (msg);
			}
			
			// Cancel queued overland spell
			else if (getRemoveQueuedSpellIndex () != null)
			{
				final RequestRemoveQueuedSpellMessage msg = new RequestRemoveQueuedSpellMessage ();
				msg.setQueuedSpellIndex (getRemoveQueuedSpellIndex ());
			    getClient ().getServerConnection ().sendMessageToServer (msg);
			}
			
			// Leave session
			else if (isLeaveSession ())
				getClient ().getServerConnection ().sendMessageToServer (new LeaveSession ());
			
			else
				log.warn ("MessageBoxUI had yes button clicked for text \"" + messageText.getText () + " but took no action");
				
			// Close the form
			getDialog ().dispose ();
		});
		
		// Initialize the dialog
		final MessageBoxUI ui = this;
		getDialog ().setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		getDialog ().addWindowListener (new WindowAdapter ()
		{
			@Override
			public final void windowClosed (@SuppressWarnings ("unused") final WindowEvent ev)
			{
				getLanguageChangeMaster ().removeLanguageChangeListener (ui);
			}
		});
		
		// Initialize the content pane
		contentPane = getUtils ().createPanelWithBackgroundImage (background);
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getMessageBoxLayout ()));
		
		final int buttonCount = ((getUnitToDismiss () == null) && (getCityLocation () == null) && (getResearchSpellID () == null) &&
			(getCastSpellID () == null) && (getCancelTargetingSpell () == null) && (getSwitchOffSpell () == null) && (getDestroyHeroItemMessage () == null) &&
			(getSavedGameID () == null) && (getRemoveQueuedSpellIndex () == null) && (!isLeaveSession ())) ? 1 : 2;
		
		messageText = getUtils ().createWrappingLabel (MomUIConstants.SILVER, getSmallFont ());
		contentPane.add (getUtils ().createTransparentScrollPane (messageText), "frmMessageBoxText");

		// Is it just a regular message box with an OK button, or do we need separate no/yes buttons?
		if (buttonCount == 1)
		{
			contentPane.add (getUtils ().createImageButton (okAction, MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (), buttonNormal, buttonPressed, buttonNormal), "frmMessageBoxOK");
			noAction.setEnabled (false);
			yesAction.setEnabled (false);
		}
		else
		{
			contentPane.add (getUtils ().createImageButton (noAction, MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (), buttonNormal, buttonPressed, buttonNormal), "frmMessageBoxNo");
			contentPane.add (getUtils ().createImageButton (yesAction, MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (), buttonNormal, buttonPressed, buttonNormal), "frmMessageBoxYes");
			okAction.setEnabled (false);
		}
		
		// Lock dialog size
		getDialog ().setContentPane (contentPane);
		getDialog ().setResizable (false);

		// Shortcut keys
		contentPane.getActionMap ().put (Shortcut.MESSAGE_BOX_CLOSE,	okAction);
		contentPane.getActionMap ().put (Shortcut.MESSAGE_BOX_YES,		yesAction);
		contentPane.getActionMap ().put (Shortcut.MESSAGE_BOX_NO,		noAction);
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		// Title
		final String useTitle;
		if (getTitle () != null)
			useTitle = getTitle ();
		else
			useTitle = getLanguageHolder ().findDescription (getLanguageTitle ());
		
		// Text
		final String useText;
		if (getText () != null)
			useText = getText ();
		else
			useText = getLanguageHolder ().findDescription (getLanguageText ());
		
		getDialog ().setTitle (useTitle);
		messageText.setText (useText);
		
		// Button
		okAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getSimple ().getOk ()));
		noAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getSimple ().getNo ()));
		yesAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getSimple ().getYes ()));

		// Shortcut keys
		getLanguageHolder ().configureShortcutKeys (contentPane);
	}

	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getMessageBoxLayout ()
	{
		return messageBoxLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setMessageBoxLayout (final XmlLayoutContainerEx layout)
	{
		messageBoxLayout = layout;
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
	 * @return Small font
	 */
	public final Font getSmallFont ()
	{
		return smallFont;
	}

	/**
	 * @param font Small font
	 */
	public final void setSmallFont (final Font font)
	{
		smallFont = font;
	}

	/**
	 * @return New turn messages UI
	 */
	public final NewTurnMessagesUI getNewTurnMessagesUI ()
	{
		return newTurnMessagesUI;
	}

	/**
	 * @param ui New turn messages UI
	 */
	public final void setNewTurnMessagesUI (final NewTurnMessagesUI ui)
	{
		newTurnMessagesUI = ui;
	}
	
	/**
	 * @return Turn sequence and movement helper methods
	 */
	public final OverlandMapProcessing getOverlandMapProcessing ()
	{
		return overlandMapProcessing;
	}

	/**
	 * @param proc Turn sequence and movement helper methods
	 */
	public final void setOverlandMapProcessing (final OverlandMapProcessing proc)
	{
		overlandMapProcessing = proc;
	}
	
	/**
	 * @return Magic sliders screen
	 */
	public final MagicSlidersUI getMagicSlidersUI ()
	{
		return magicSlidersUI;
	}

	/**
	 * @param ui Magic sliders screen
	 */
	public final void setMagicSlidersUI (final MagicSlidersUI ui)
	{
		magicSlidersUI = ui;
	}
	
	/**
	 * @return Wizards UI
	 */
	public final WizardsUI getWizardsUI ()
	{
		return wizardsUI;
	}

	/**
	 * @param ui Wizards UI
	 */
	public final void setWizardsUI (final WizardsUI ui)
	{
		wizardsUI = ui;
	}
	
	/**
	 * @return Language options to use for the title; null if title is not language-variable
	 */
	public final List<LanguageText> getLanguageTitle ()
	{
		return languageTitle;
	}

	/**
	 * @param t Language options to use for the title; null if title is not language-variable
	 */
	public final void setLanguageTitle (final List<LanguageText> t)
	{
		languageTitle = t;
	}
	
	/**
	 * @return Language category ID to use for the message; null if message is not language-variable
	 */
	public final List<LanguageText> getLanguageText ()
	{
		return languageText;
	}
	
	/**
	 * @param t Language category ID to use for the message; null if message is not language-variable
	 */
	public final void setLanguageText (final List<LanguageText> t)
	{
		languageText = t;
	}
	
	/**
	 * @return Fixed title that isn't lanuage variant; null if title is language variable
	 */
	public final String getTitle ()
	{
		return title;
	}

	/**
	 * @param txt Fixed title that isn't lanuage variant; null if title is language variable
	 */
	public final void setTitle (final String txt)
	{
		title = txt;
	}
	
	/**
	 * @return Fixed text that isn't lanuage variant; null if message is language variable
	 */
	public final String getText ()
	{
		return text;
	}

	/**
	 * @param txt Fixed text that isn't lanuage variant; null if message is language variable
	 */
	public final void setText (final String txt)
	{
		text = txt;
	}

	/**
	 * @return The unit being dismissed; null if the message box isn't asking about dismissing a unit
	 */
	public final MemoryUnit getUnitToDismiss ()
	{
		return unitToDismiss;
	}

	/**
	 * @param unit The unit being dismissed; null if the message box isn't asking about dismissing a unit
	 */
	public final void setUnitToDismiss (final MemoryUnit unit)
	{
		unitToDismiss = unit;
	}

	/**
	 * @return The city to rush by at or sell a building at; null if the message box isn't about rush buying or selling a building
	 */
	public final MapCoordinates3DEx getCityLocation ()
	{
		return cityLocation;
	}

	/**
	 * @param location The city to rush by at or sell a building at; null if the message box isn't about rush buying or selling a building
	 */
	public final void setCityLocation (final MapCoordinates3DEx location)
	{
		cityLocation = location;
	}

	/**
	 * @return The building being sold; null if the message box isn't about selling a building
	 */
	public final Integer getBuildingURN ()
	{
		return buildingURN;
	}

	/**
	 * @param building The building being sold; null if the message box isn't about selling a building
	 */
	public final void setBuildingURN (final Integer building)
	{
		buildingURN = building;
	}

	/**
	 * @return The spell we're trying to research; null if the message box isn't about researching a spell
	 */
	public final String getResearchSpellID ()
	{
		return researchSpellID;
	}

	/**
	 * @param spellID The spell we're trying to research; null if the message box isn't about researching a spell
	 */
	public final void setResearchSpellID (final String spellID)
	{
		researchSpellID = spellID;
	}
	
	/**
	 * @return The spell we're trying to cast; null if the message box isn't about casting a spell
	 */
	public final String getCastSpellID ()
	{
		return castSpellID;
	}
	
	/**
	 * @param spellID The spell we're trying to cast; null if the message box isn't about casting a spell
	 */
	public final void setCastSpellID (final String spellID)
	{
		castSpellID = spellID;
	}

	/**
	 * @return The NTM telling us to target the spell that we're cancelling; null if the message box isn't about cancelling targeting a spell
	 */
	public final NewTurnMessageSpellEx getCancelTargetingSpell ()
	{
		return cancelTargetingSpell;
	}

	/**
	 * @param msg The NTM telling us to target the spell that we're cancelling; null if the message box isn't about cancelling targeting a spell
	 */
	public final void setCancelTargetingSpell (final NewTurnMessageSpellEx msg)
	{
		cancelTargetingSpell = msg;
	}

	/**
	 * @return Spell we're thinking of switching off; null if the message box isn't about switching off a spell
	 */
	public final MemoryMaintainedSpell getSwitchOffSpell ()
	{
		return switchOffSpell;
	}

	/**
	 * @param spell Spell we're thinking of switching off; null if the message box isn't about switching off a spell
	 */
	public final void setSwitchOffSpell (final MemoryMaintainedSpell spell)
	{
		switchOffSpell = spell;
	}

	/**
	 * @return Hero item we're thinking of destroying on the anvil; null if the message box isn't about destroying a hero item
	 */
	public final RequestMoveHeroItemMessage getDestroyHeroItemMessage ()
	{
		return destroyHeroItemMessage;
	}

	/**
	 * @param msg Hero item we're thinking of destroying on the anvil; null if the message box isn't about destroying a hero item
	 */
	public final void setDestroyHeroItemMessage (final RequestMoveHeroItemMessage msg)
	{
		destroyHeroItemMessage = msg;
	}

	/**
	 * @return Saved game we're thinking of deleting; null if the message box isn't about deleting a saved game
	 */
	public final Integer getSavedGameID ()
	{
		return savedGameID;
	}

	/**
	 * @param id Saved game we're thinking of deleting; null if the message box isn't about deleting a saved game
	 */
	public final void setSavedGameID (final Integer id)
	{
		savedGameID = id;
	}

	/**
	 * @return Queued overland spell we're thinking of cancelling; null if the message box isn't about cancelling a queued overland spell
	 */
	public final Integer getRemoveQueuedSpellIndex ()
	{
		return removeQueuedSpellIndex;
	}

	/**
	 * @param index Queued overland spell we're thinking of cancelling; null if the message box isn't about cancelling a queued overland spell
	 */
	public final void setRemoveQueuedSpellIndex (final Integer index)
	{
		removeQueuedSpellIndex = index;
	}

	/**
	 * @return True if message box is being showed to ask player if they want to leave session
	 */
	public final boolean isLeaveSession ()
	{
		return leaveSession;
	}
	
	/**
	 * @param l True if message box is being showed to ask player if they want to leave session
	 */
	public final void setLeaveSession (final boolean l)
	{
		leaveSession = l;
	}
}