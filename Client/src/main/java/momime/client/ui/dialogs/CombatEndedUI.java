package momime.client.ui.dialogs;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

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
import momime.common.database.LanguageText;
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
	private final static Log log = LogFactory.getLog (CombatEndedUI.class);
	
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
	}

	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		// Get the city name, in case we need it
		final MemoryGridCell mc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
			(getMessage ().getCombatLocation ().getZ ()).getRow ().get (getMessage ().getCombatLocation ().getY ()).getCell ().get (getMessage ().getCombatLocation ().getX ());
		final OverlandMapCityData cityData = (mc != null) ? mc.getCityData () : null;
		final String cityName = (cityData != null) ? cityData.getCityName () : null;
		
		// Work out the text to display
		final boolean weWon = (getMessage ().getWinningPlayerID () == getClient ().getOurPlayerID ());
		
		final List<LanguageText> languageText;
		final StringBuilder bottomText = new StringBuilder ();
		if (getMessage ().getCaptureCityDecisionID () == null)
		{
			// Not a city combat, or a city combat where the defender won
			languageText = weWon ? getLanguages ().getCombatEndedScreen ().getVictory () : getLanguages ().getCombatEndedScreen ().getDefeat ();
		}
		else
		{
			// Was a city combat
			if (getMessage ().getCaptureCityDecisionID () == CaptureCityDecisionID.CAPTURE)
				languageText = weWon ? getLanguages ().getCombatEndedScreen ().getYouCaptured () : getLanguages ().getCombatEndedScreen ().getEnemyCaptured ();
			else
				languageText = weWon ? getLanguages ().getCombatEndedScreen ().getYouRazed () : getLanguages ().getCombatEndedScreen ().getEnemyRazed ();
			
			// Gold looted from the city
			if (getMessage ().getGoldSwiped () != null)
			{
				final List<LanguageText> goldText = weWon ? getLanguages ().getCombatEndedScreen ().getYouGotGoldFromEnemyCity () :
					getLanguages ().getCombatEndedScreen ().getEnemyGotGoldFromYourCity ();
				
				bottomText.append (getLanguageHolder ().findDescription (goldText).replaceAll
					("GOLD_FROM_CITY", getTextUtils ().intToStrCommas (getMessage ().getGoldSwiped ())));
			}
			
			if ((getMessage ().getGoldFromRazing () != null) && (weWon))
			{
				if (bottomText.length () > 0)
					bottomText.append (System.lineSeparator ());
				
				bottomText.append (getLanguageHolder ().findDescription (getLanguages ().getCombatEndedScreen ().getYouGotGoldFromRazing ()).replaceAll
					("GOLD_FROM_RAZING", getTextUtils ().intToStrCommas (getMessage ().getGoldFromRazing ())));
			}
		}
		
		// Fame changed?
		if (getMessage ().getFameChange () != 0)
		{
			if (bottomText.length () > 0)
				bottomText.append (System.lineSeparator ());

			if (getMessage ().getFameChange () > 0)
				bottomText.append (getLanguageHolder ().findDescription (getLanguages ().getCombatEndedScreen ().getFameGained ()).replaceAll
					("FAME_GAINED", getTextUtils ().intToStrCommas (getMessage ().getFameChange ())));
			else
				bottomText.append (getLanguageHolder ().findDescription (getLanguages ().getCombatEndedScreen ().getFameLost ()).replaceAll
					("FAME_LOST", getTextUtils ().intToStrCommas (-getMessage ().getFameChange ())));
		}
		
		// Hero items?
		if ((getMessage ().getHeroItemCount () > 0) && (getMessage ().getWinningPlayerID () == getClient ().getOurPlayerID ()))
		{
			if (bottomText.length () > 0)
				bottomText.append (System.lineSeparator ());
			
			bottomText.append (getLanguageHolder ().findDescription (getLanguages ().getCombatEndedScreen ().getHeroItems ()).replaceAll
				("ITEM_COUNT", Integer.valueOf (getMessage ().getHeroItemCount ()).toString ()));
		}
		
		// Units regenerated?
		if (getMessage ().getRegeneratedCount () > 0)
		{
			if (bottomText.length () > 0)
				bottomText.append (System.lineSeparator ());
			
			final List<LanguageText> regeneratedText = (getMessage ().getRegeneratedCount () == 1) ?
				getLanguages ().getCombatEndedScreen ().getRegeneratedSingular () : getLanguages ().getCombatEndedScreen ().getRegeneratedPlural ();
			
			bottomText.append (getLanguageHolder ().findDescription (regeneratedText).replaceAll
				("REGENERATED_COUNT", Integer.valueOf (getMessage ().getRegeneratedCount ()).toString ()));
		}
		
		// Undead created?
		if (getMessage ().getUndeadCreated () > 0)
		{
			if (bottomText.length () > 0)
				bottomText.append (System.lineSeparator ());
			
			final List<LanguageText> undeadText = (getMessage ().getUndeadCreated () == 1) ?
				getLanguages ().getCombatEndedScreen ().getUndeadCreatedSingular () : getLanguages ().getCombatEndedScreen ().getUndeadCreatedPlural ();
			
			bottomText.append (getLanguageHolder ().findDescription (undeadText).replaceAll
				("UNDEAD_COUNT", Integer.valueOf (getMessage ().getUndeadCreated ()).toString ()));
		}
		
		headingText.setText (getLanguageHolder ().findDescription (languageText).replaceAll ("CITY_NAME", cityName));
		mainText.setText (bottomText.toString ());
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