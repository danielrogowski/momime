package momime.client.ui.dialogs;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import momime.client.MomClient;
import momime.client.newturnmessages.NewTurnMessageSpellEx;
import momime.client.process.OverlandMapProcessing;
import momime.client.ui.MomUIConstants;
import momime.client.ui.frames.NewTurnMessagesUI;
import momime.common.messages.clienttoserver.v0_9_5.CancelTargetSpellMessage;
import momime.common.messages.clienttoserver.v0_9_5.DismissUnitMessage;
import momime.common.messages.clienttoserver.v0_9_5.RequestCastSpellMessage;
import momime.common.messages.clienttoserver.v0_9_5.RequestResearchSpellMessage;
import momime.common.messages.clienttoserver.v0_9_5.RushBuyMessage;
import momime.common.messages.clienttoserver.v0_9_5.SellBuildingMessage;
import momime.common.messages.v0_9_5.MemoryUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.swing.GridBagConstraintsNoFill;

/**
 * Modal dialog which displays a message with an OK button, or a Yes and No choice, with Yes taking some action
 */
public final class MessageBoxUI extends MomClientDialogUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (MessageBoxUI.class);
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Small font */
	private Font smallFont;
	
	/** New turn messages UI */
	private NewTurnMessagesUI newTurnMessagesUI;
	
	/** Turn sequence and movement helper methods */
	private OverlandMapProcessing overlandMapProcessing;
	
	/** OK action */
	private Action okAction;

	/** No action */
	private Action noAction;

	/** Yes action */
	private Action yesAction;

	/** Message text */
	private JTextArea messageText;

	/** Language category ID to use for the title; null if title is not language-variable */
	private String titleLanguageCategoryID;
	
	/** Language entry ID to use for the title; null if title is not language-variable */
	private String titleLanguageEntryID;
	
	/** Language category ID to use for the message; null if message is not language-variable */
	private String textLanguageCategoryID;
	
	/** Language entry ID to use for the message; null if message is not language-variable */
	private String textLanguageEntryID;

	/** Fixed title that isn't lanuage variant; null if title is language variable */
	private String title;	
	
	/** Fixed text that isn't lanuage variant; null if message is language variable */
	private String text;
	
	/** The unit being dismissed; null if the message box isn't asking about dismissing a unit */
	private MemoryUnit unitToDismiss;
	
	/** The city to rush by at or sell a building at; null if the message box isn't about rush buying or selling a building */
	private MapCoordinates3DEx cityLocation;
	
	/** The building being sold; null if the message box isn't about selling a building */
	private String buildingID;
	
	/** The spell we're trying to research; null if the message box isn't about researching a spell */
	private String researchSpellID;
	
	/** The spell we're trying to cast; null if the message box isn't about casting a spell */
	private String castSpellID;
	
	/** The NTM telling us to target the spell that we're cancelling; null if the message box isn't about cancelling targetting a spell */
	private NewTurnMessageSpellEx cancelTargettingSpell;
	
	/** Typical inset used on this screen layout */
	private final static int INSET = 8;
	
	/**
	 * Sets up the dialog once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init");
		
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/messageBox498x100.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x18goldNormal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x18goldPressed.png");

		// Actions
		okAction = new AbstractAction ()
		{
			private static final long serialVersionUID = -7614787019328142967L;

			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				getDialog ().dispose ();
			}
		};
		
		noAction = new AbstractAction ()
		{
			private static final long serialVersionUID = 2288860186594112684L;

			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
				{
					// Cancel cancel targetting a spell, leaving it on the NTM list to be retargetted again, but still we have to close out the "Target Spell" right hand panel
					if (getCancelTargettingSpell () != null)
						getOverlandMapProcessing ().updateMovementRemaining ();
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}

				// Close the form
				getDialog ().dispose ();
			}
		};
		
		yesAction = new AbstractAction ()
		{
			private static final long serialVersionUID = -2607638551174255278L;

			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
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
						if (getBuildingID () == null)
						{
							final RushBuyMessage msg = new RushBuyMessage ();
							msg.setCityLocation (getCityLocation ());
							getClient ().getServerConnection ().sendMessageToServer (msg);
						}
						else
						{
							final SellBuildingMessage msg = new SellBuildingMessage ();
							msg.setCityLocation (getCityLocation ());
							msg.setBuildingID (getBuildingID ());
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
						msg.setSpellID (getResearchSpellID ());
						getClient ().getServerConnection ().sendMessageToServer (msg);
					}
					
					// Cancel targetting a spell
					else if (getCancelTargettingSpell () != null)
					{
						// Mark the NTM as cancelled
						getCancelTargettingSpell ().setTargettingCancelled (true);
						
						// Redraw the NTMs
						getNewTurnMessagesUI ().languageChanged ();
						
						// Server will have spell listed in their Maintained Spells list, so tell the server to remove it
						final CancelTargetSpellMessage msg = new CancelTargetSpellMessage ();
						msg.setSpellID (getCancelTargettingSpell ().getSpellID ());
						getClient ().getServerConnection ().sendMessageToServer (msg);
						
						// Close out the "Target Spell" right hand panel
						getOverlandMapProcessing ().updateMovementRemaining ();
					}
					
					else
						log.warn ("MessageBoxUI had yes button clicked for text \"" + messageText.getText () + " but took no action");
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
				
				// Close the form
				getDialog ().dispose ();
			}
		};
		
		// Initialize the dialog
		final MessageBoxUI ui = this;
		getDialog ().setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		getDialog ().addWindowListener (new WindowAdapter ()
		{
			@Override
			public final void windowClosed (final WindowEvent ev)
			{
				getLanguageChangeMaster ().removeLanguageChangeListener (ui);
			}
		});
		
		// Initialize the content pane
		final JPanel contentPane = getUtils ().createPanelWithBackgroundImage (background);
		
		// Set up layout
		contentPane.setLayout (new GridBagLayout ());
		
		final int buttonCount = ((getUnitToDismiss () == null) && (getCityLocation () == null) && (getResearchSpellID () == null) &&
			(getCastSpellID () == null) && (getCancelTargettingSpell () == null)) ? 1 : 2;
		
		final GridBagConstraints constraints = getUtils ().createConstraintsBothFill (0, 0, buttonCount, 1, new Insets (INSET, INSET, 3, INSET));
		constraints.weightx = 1;
		constraints.weighty = 1;
		
		messageText = getUtils ().createWrappingLabel (MomUIConstants.SILVER, getSmallFont ());
		contentPane.add (getUtils ().createTransparentScrollPane (messageText), constraints);

		// Is it just a regular message box with an OK button, or do we need separate no/yes buttons?
		if (buttonCount == 1)
			contentPane.add (getUtils ().createImageButton (okAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (),
				buttonNormal, buttonPressed, buttonNormal), getUtils ().createConstraintsNoFill (0, 1, 1, 1, new Insets (0, INSET, 9, INSET), GridBagConstraintsNoFill.CENTRE));
		else
		{
			final GridBagConstraints constraintsNo = getUtils ().createConstraintsNoFill (0, 1, 1, 1, new Insets (0, INSET, 9, INSET), GridBagConstraintsNoFill.EAST);
			constraintsNo.weightx = 0.5;
			
			contentPane.add (getUtils ().createImageButton (noAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (),
				buttonNormal, buttonPressed, buttonNormal), constraintsNo);

			final GridBagConstraints constraintsYes = getUtils ().createConstraintsNoFill (1, 1, 1, 1, new Insets (0, INSET, 9, INSET), GridBagConstraintsNoFill.WEST);
			constraintsYes.weightx = 0.5;
			
			contentPane.add (getUtils ().createImageButton (yesAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (),
				buttonNormal, buttonPressed, buttonNormal), constraintsYes);
		}
		
		// Lock dialog size
		getDialog ().setContentPane (contentPane);
		getDialog ().setResizable (false);

		log.trace ("Exiting init");
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		// Title
		String useTitle;
		if (getTitle () != null)
			useTitle = getTitle ();
		else
			useTitle = getLanguage ().findCategoryEntry (getTitleLanguageCategoryID (), getTitleLanguageEntryID ());
		
		// Text
		String useText;
		if (getText () != null)
			useText = getText ();
		else
			useText = getLanguage ().findCategoryEntry (getTextLanguageCategoryID (), getTextLanguageEntryID ());
		
		getDialog ().setTitle (useTitle);
		messageText.setText (useText);
		
		// Button
		okAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMessageBox", "OK"));
		noAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMessageBox", "No"));
		yesAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMessageBox", "Yes"));
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
	 * @return Language category ID to use for the title; null if title is not language-variable
	 */
	public final String getTitleLanguageCategoryID ()
	{
		return titleLanguageCategoryID;
	}

	/**
	 * @param cat Language category ID to use for the title; null if title is not language-variable
	 */
	public final void setTitleLanguageCategoryID (final String cat)
	{
		titleLanguageCategoryID = cat;
	}
	
	/**
	 * @return Language entry ID to use for the title; null if title is not language-variable
	 */
	public final String getTitleLanguageEntryID ()
	{
		return titleLanguageEntryID;
	}

	/**
	 * @param entry Language entry ID to use for the title; null if title is not language-variable
	 */
	public final void setTitleLanguageEntryID (final String entry)
	{
		titleLanguageEntryID = entry;
	}
	
	/**
	 * @return Language category ID to use for the message; null if message is not language-variable
	 */
	public final String getTextLanguageCategoryID ()
	{
		return textLanguageCategoryID;
	}

	/**
	 * @param cat Language category ID to use for the message; null if message is not language-variable
	 */
	public final void setTextLanguageCategoryID (final String cat)
	{
		textLanguageCategoryID = cat;
	}
	
	/**
	 * @return Language entry ID to use for the message; null if message is not language-variable
	 */
	public final String getTextLanguageEntryID ()
	{
		return textLanguageEntryID;
	}

	/**
	 * @param entry Language entry ID to use for the message; null if message is not language-variable
	 */
	public final void setTextLanguageEntryID (final String entry)
	{
		textLanguageEntryID = entry;
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
	public final String getBuildingID ()
	{
		return buildingID;
	}

	/**
	 * @param building The building being sold; null if the message box isn't about selling a building
	 */
	public final void setBuildingID (final String building)
	{
		buildingID = building;
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
	 * @return The NTM telling us to target the spell that we're cancelling; null if the message box isn't about cancelling targetting a spell
	 */
	public final NewTurnMessageSpellEx getCancelTargettingSpell ()
	{
		return cancelTargettingSpell;
	}

	/**
	 * @param msg The NTM telling us to target the spell that we're cancelling; null if the message box isn't about cancelling targetting a spell
	 */
	public final void setCancelTargettingSpell (final NewTurnMessageSpellEx msg)
	{
		cancelTargettingSpell = msg;
	}
}