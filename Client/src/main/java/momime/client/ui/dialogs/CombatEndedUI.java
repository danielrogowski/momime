package momime.client.ui.dialogs;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.actions.LoggingAction;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.messages.process.CombatEndedMessageImpl;
import momime.client.ui.MomUIConstants;
import momime.client.utils.TextUtils;
import momime.common.messages.CaptureCityDecisionID;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.OverlandMapCityData;

/**
 * Popup box at the end of a combat, it doesn't do much, just says whether we won or lost and
 * any details like destroyed buildings, looted coins and so on.  Processing of further messages
 * is blocked until we click this popup to close it.
 */
public final class CombatEndedUI extends MomClientDialogUI
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (CombatEndedUI.class);
	
	/** XML layout */
	private XmlLayoutContainerEx combatEndedLayout;

	/** Multiplayer client */
	private MomClient client;
	
	/** Small font */
	private Font smallFont;
	
	/** Text utils */
	private TextUtils textUtils;
	
	/** Details about how the combat ended */
	private CombatEndedMessageImpl message;
	
	/** Whether we've unblocked the message queue */
	private boolean unblocked;
	
	/** Single line of heading text */
	private JLabel headingText;
	
	/** Bulk of message text */
	private JTextArea mainText;
	
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init: " + getMessage ().getCombatLocation ());
		
		// Load images
		final boolean weWon = (getMessage ().getWinningPlayerID () == getClient ().getOurPlayerID ());
		
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/combatEnded/you" + (weWon ? "Won" : "Lost") + ".png");
		final BufferedImage footer = getUtils ().loadImage ("/momime.client.graphics/ui/combatEnded/footer.png");
		final BufferedImage closeButtonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/scroll/closeButtonNormal.png");
		final BufferedImage closeButtonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/scroll/closeButtonPressed.png");
		
		// Actions
		final Action closeAction = new LoggingAction ((ev) -> getDialog ().dispose ());
		
		// Initialize the dialog
		final CombatEndedUI ui = this;
		getDialog ().setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		getDialog ().addWindowListener (new WindowAdapter ()
		{
			@Override
			public final void windowClosed (@SuppressWarnings ("unused") final WindowEvent ev)
			{
				try
				{
					getLanguageChangeMaster ().removeLanguageChangeListener (ui);
					
					// Unblock the message that caused this
					// This somehow seems to get called twice, so protect against that
					if (!unblocked)
					{
						getClient ().finishCustomDurationMessage (getMessage ());						
						unblocked = true;
					}
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		});
		
		// Initialize the content pane
		final JPanel contentPane = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				super.paintComponent (g);
				g.drawImage (background, 0, 0, null);
			}
		};
		contentPane.setBackground (Color.BLACK);
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getCombatEndedLayout ()));
		
		contentPane.add (getUtils ().createImage (footer), "frmCombatEndedBottomScroll");
		
		headingText = getUtils ().createLabel (MomUIConstants.DARK_BROWN, getSmallFont ());
		contentPane.add (headingText, "frmCombatEndedHeading");
		
		mainText = getUtils ().createWrappingLabel (MomUIConstants.DARK_BROWN, getSmallFont ());
		contentPane.add (mainText, "frmCombatEndedText");

		contentPane.add (getUtils ().createImageButton (closeAction, null, null, null, closeButtonNormal, closeButtonPressed, closeButtonNormal),
			"frmCombatEndedClose");
		
		// Lock frame size
		getDialog ().setContentPane (contentPane);
		getDialog ().setResizable (false);
		getDialog ().setUndecorated (true);

		final int bottomRollerBottom = getCombatEndedLayout ().getFormHeight () - 19;	// Just because this is copied from the close button on the NTM UI
		getDialog ().setShape (new Polygon
			(new int [] {0, 2, 3, 6, 199, 202, 203, 206, 205, 205, 206, 206, 203, 202, 199,
				158, 161, 161, 158, 158, 162, 142, 140, 140, 143, 143, 140,		// centre row of numbers is the close button
				6, 3, 2, 0, 2, 5, 9, 9, 5, 2, 0},
				
			new int [] {6, 3, 2, 0, 0, 2, 3, 11, 17, 189, 193, 200, 207, 208, 210,
				bottomRollerBottom - 6, bottomRollerBottom - 3, bottomRollerBottom + 2, bottomRollerBottom + 5, getCombatEndedLayout ().getFormHeight () - 7, getCombatEndedLayout ().getFormHeight (), getCombatEndedLayout ().getFormHeight (), getCombatEndedLayout ().getFormHeight () - 7, bottomRollerBottom + 5, bottomRollerBottom + 2, bottomRollerBottom - 3, bottomRollerBottom - 6, 
				210, 208, 207, 204, 192, 189, 186, 23, 21, 18, 14},
			38));
		
		log.trace ("Exiting init");
	}

	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged: " + getMessage ().getCombatLocation ());
		
		// Get the city name, in case we need it
		final MemoryGridCell mc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
			(getMessage ().getCombatLocation ().getZ ()).getRow ().get (getMessage ().getCombatLocation ().getY ()).getCell ().get (getMessage ().getCombatLocation ().getX ());
		final OverlandMapCityData cityData = (mc != null) ? mc.getCityData () : null;
		final String cityName = (cityData != null) ? cityData.getCityName () : null;
		
		// Work out the text to display
		final boolean weWon = (getMessage ().getWinningPlayerID () == getClient ().getOurPlayerID ());
		
		final String languageEntryID;
		final StringBuilder bottomText = new StringBuilder ();
		if (getMessage ().getCaptureCityDecisionID () == null)
		{
			// Not a city combat, or a city combat where the defender won
			languageEntryID = weWon ? "Victory" : "Defeat";
		}
		else
		{
			// Was a city combat
			languageEntryID = (weWon ? "You" : "Enemy") + (getMessage ().getCaptureCityDecisionID () == CaptureCityDecisionID.CAPTURE ? "Captured" : "Razed");
			
			// Gold looted from the city
			if (getMessage ().getGoldSwiped () != null)
				bottomText.append (getLanguage ().findCategoryEntry ("frmCombatEnded", weWon ? "YouGotGoldFromEnemyCity" : "EnemyGotGoldFromYourCity").replaceAll
					("GOLD_FROM_CITY", getTextUtils ().intToStrCommas (getMessage ().getGoldSwiped ())));
			
			if ((getMessage ().getGoldFromRazing () != null) && (weWon))
			{
				if (bottomText.length () > 0)
					bottomText.append (System.lineSeparator ());
				
				bottomText.append (getLanguage ().findCategoryEntry ("frmCombatEnded", "YouGotGoldFromRazing").replaceAll
					("GOLD_FROM_RAZING", getTextUtils ().intToStrCommas (getMessage ().getGoldFromRazing ())));
			}
		}
		
		// Hero items?
		if (getMessage ().getHeroItemCount () > 0)
		{
			if (bottomText.length () > 0)
				bottomText.append (System.lineSeparator ());
			
			bottomText.append (getLanguage ().findCategoryEntry ("frmCombatEnded", "HeroItems").replaceAll
				("ITEM_COUNT", Integer.valueOf (getMessage ().getHeroItemCount ()).toString ()));
		}
		
		// Undead created?
		if (getMessage ().getUndeadCreated () > 0)
		{
			if (bottomText.length () > 0)
				bottomText.append (System.lineSeparator ());
			
			bottomText.append (getLanguage ().findCategoryEntry ("frmCombatEnded", "UndeadCreated" +
				((getMessage ().getUndeadCreated () == 1) ? "Singular" : "Plural")).replaceAll
				("UNDEAD_COUNT", Integer.valueOf (getMessage ().getUndeadCreated ()).toString ()));
		}
		
		headingText.setText (getLanguage ().findCategoryEntry ("frmCombatEnded", languageEntryID).replaceAll ("CITY_NAME", cityName));
		mainText.setText (bottomText.toString ());
		
		log.trace ("Exiting languageChanged");
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getCombatEndedLayout ()
	{
		return combatEndedLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setCombatEndedLayout (final XmlLayoutContainerEx layout)
	{
		combatEndedLayout = layout;
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
	 * @return Text utils
	 */
	public final TextUtils getTextUtils ()
	{
		return textUtils;
	}

	/**
	 * @param tu Text utils
	 */
	public final void setTextUtils (final TextUtils tu)
	{
		textUtils = tu;
	}
	
	/**
	 * @return Details about how the combat ended
	 */
	public final CombatEndedMessageImpl getMessage ()
	{
		return message;
	}
	
	/**
	 * @param msg Details about how the combat ended
	 */
	public final void setMessage (final CombatEndedMessageImpl msg)
	{
		message = msg;
	}
}