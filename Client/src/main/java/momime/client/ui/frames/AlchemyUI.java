package momime.client.ui.frames;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import momime.client.MomClient;
import momime.client.language.database.v0_9_5.ProductionType;
import momime.client.ui.MomUIConstants;
import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.clienttoserver.AlchemyMessage;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.ResourceValueUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.GridBagConstraintsNoFill;

/**
 * Alchemy screen, for converting gold to mana or vice versa
 */
public final class AlchemyUI extends MomClientFrameUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (AlchemyUI.class);

	/** Typical inset used on this screen layout */
	private final static int INSET = 0;

	/** Size of the chopped off corners for the custom window shape */
	private final static int TL_CORNER = 11;

	/** Size of the chopped off corners for the custom window shape - for some reason the bottom right edges clip differently */
	private final static int BR_CORNER = 10;
	
	/** Large font */
	private Font largeFont;
	
	/** Small font */
	private Font smallFont;

	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** OK action */
	private Action okAction;
	
	/** Cancel action */
	private Action cancelAction;
	
	/** Indicates whether we're converting gold > mana or mana > gold */
	private JLabel conversionLabel;
	
	/** Button to change whether we're converting gold > mana or mana > gold */
	private JButton changeDirectionButton;
	
	/** Label showing gold amount */
	private JLabel goldLabel;
	
	/** Label showing mana amount */
	private JLabel manaLabel;
	
	/** The slider to choose the amount */
	private JSlider slider;
	
	/** Default to converting gold > mana */
	private String fromProductionTypeID = CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD;

	/** Default to converting gold > mana */
	private String toProductionTypeID = CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init");
		
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/alchemy/background.png");
		final BufferedImage goldToManaButtonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/alchemy/goldToManaButtonNormal.png");
		final BufferedImage goldToManaButtonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/alchemy/goldToManaButtonPressed.png");
		final BufferedImage manaToGoldButtonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/alchemy/manaToGoldButtonNormal.png");
		final BufferedImage manaToGoldButtonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/alchemy/manaToGoldButtonPressed.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button98x32greyNormal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button98x32greyPressed.png");
		final BufferedImage buttonDisabled = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button98x32greyDisabled.png");
		final BufferedImage sliderImage = getUtils ().loadImage ("/momime.client.graphics/ui/alchemy/slider.png");
		
		// Actions
		final Action changeDirectionAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				// Do the swap
				final String temp = fromProductionTypeID;
				fromProductionTypeID = toProductionTypeID;
				toProductionTypeID = temp;
				
				// Update the labels
				directionChanged ();
				
				// Update the button
				if (fromProductionTypeID.equals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD))
				{
					changeDirectionButton.setIcon (new ImageIcon (goldToManaButtonNormal));
					changeDirectionButton.setPressedIcon (new ImageIcon (goldToManaButtonPressed));
					changeDirectionButton.setDisabledIcon (new ImageIcon (goldToManaButtonNormal));
				}
				else
				{
					changeDirectionButton.setIcon (new ImageIcon (manaToGoldButtonNormal));
					changeDirectionButton.setPressedIcon (new ImageIcon (manaToGoldButtonPressed));
					changeDirectionButton.setDisabledIcon (new ImageIcon (manaToGoldButtonNormal));
				}
			}
		};
		
		okAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
				{
					// If we do not have alchemy retort, then the actual slider value represents half, so it is only possible to select even numbers
					int fromValue = slider.getValue ();
				
					final PlayerPublicDetails ourPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getClient ().getOurPlayerID (), "okActionPerformed");
					final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) ourPlayer.getPersistentPlayerPublicKnowledge ();

					if (getPlayerPickUtils ().getQuantityOfPick (pub.getPick (), CommonDatabaseConstants.VALUE_RETORT_ID_ALCHEMY) == 0) 
						fromValue = fromValue * 2;

					// Send message to the server
					final AlchemyMessage msg = new AlchemyMessage ();
					msg.setFromProductionTypeID (fromProductionTypeID);
					msg.setFromValue (fromValue);
				
					getClient ().getServerConnection ().sendMessageToServer (msg);
				
					// Hide the form
					getFrame ().setVisible (false);
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};

		cancelAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				getFrame ().setVisible (false);
			}
		};

		// Initialize the content pane
		final JPanel contentPane = getUtils ().createPanelWithBackgroundImage (background);
 		contentPane.setOpaque (false);
		
		// Set up layout - I tried to do this in one 5x3 grid but just couldn't get it to behave correctly, so had to split it into a 1x3 grid and deal with each row separately
		contentPane.setLayout (new GridBagLayout ());
		
		// Put a slither of a column down the left of everything, so it all lines up correctly in the centre
		contentPane.add (Box.createRigidArea (new Dimension (4, 0)),
			getUtils ().createConstraintsNoFill (0, 0, 1, 3, INSET, GridBagConstraintsNoFill.CENTRE));
		
		conversionLabel = getUtils ().createShadowedLabel (MomUIConstants.DULL_GOLD, Color.BLACK, getLargeFont ());
		contentPane.add (conversionLabel, getUtils ().createConstraintsNoFill (1, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));

		// Top row
		final Dimension labelSize = new Dimension (56, 14);
		final Dimension sliderSize = new Dimension (sliderImage.getWidth (), sliderImage.getHeight ());
		
		final JPanel topRow = new JPanel ();
		topRow.setOpaque (false);
		topRow.setLayout (new GridBagLayout ());
		contentPane.add (topRow, getUtils ().createConstraintsNoFill (1, 1, 1, 1, new Insets (8, 0, 14, 0), GridBagConstraintsNoFill.CENTRE));
		
		goldLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		goldLabel.setMinimumSize (labelSize);
		goldLabel.setMaximumSize (labelSize);
		goldLabel.setPreferredSize (labelSize);
		goldLabel.setHorizontalAlignment (SwingConstants.CENTER);
		topRow.add (goldLabel, getUtils ().createConstraintsNoFill (0, 0, 1, 1, INSET, GridBagConstraintsNoFill.WEST));
		
		// Don't bother setting a maximum in the constructor - directionChanged () sets it anyway
		slider = new JSlider ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				// Only draw the piece according to the current value - if there's no maximum (we have 0 of the 'from' resource) then just draw nothing
				if (getMaximum () > 0)
				{
					final int width = (sliderImage.getWidth () * getValue ()) / getMaximum ();
				
					// Flip the direction of the image if converting mana > gold
					final int x1;
					final int x2;
					if (fromProductionTypeID.equals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD))
					{
						x1 = 0;
						x2 = width;
					}
					else
					{
						x1 = sliderImage.getWidth ();
						x2 = sliderImage.getWidth () - width;
					}
				
					// Draw a piece of the brighter colour, according to the current slider position
					g.drawImage (sliderImage,
						0, 0, width, sliderImage.getHeight (),
						x1, 0, x2, sliderImage.getHeight (), null);
				}
			}
		};
		
		slider.addChangeListener (new ChangeListener ()
		{
			@Override
			public final void stateChanged (final ChangeEvent ev)
			{
				sliderPositionChanged ();
			}
		});
		
		slider.setMinimumSize (sliderSize);
		slider.setMaximumSize (sliderSize);
		slider.setPreferredSize (sliderSize);
		topRow.add (slider, getUtils ().createConstraintsNoFill (1, 0, 1, 1, new Insets (0, 18, 0, 18), GridBagConstraintsNoFill.CENTRE));

		manaLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		manaLabel.setMinimumSize (labelSize);
		manaLabel.setMaximumSize (labelSize);
		manaLabel.setPreferredSize (labelSize);
		manaLabel.setHorizontalAlignment (SwingConstants.CENTER);
		topRow.add (manaLabel, getUtils ().createConstraintsNoFill (2, 0, 1, 1, INSET, GridBagConstraintsNoFill.EAST));
		
		// Buttons row
		final JPanel bottomRow = new JPanel ();
		bottomRow.setOpaque (false);
		bottomRow.setLayout (new GridBagLayout ());
		contentPane.add (bottomRow, getUtils ().createConstraintsNoFill (1, 2, 1, 1, new Insets (0, 0, 6, 0), GridBagConstraintsNoFill.CENTRE));
		
		bottomRow.add (getUtils ().createImageButton (cancelAction, MomUIConstants.LIGHT_GRAY, MomUIConstants.DARK_GRAY, getLargeFont (), buttonNormal, buttonPressed, buttonDisabled),
			getUtils ().createConstraintsNoFill (0, 0, 1, 1, INSET, GridBagConstraintsNoFill.WEST));
		
		changeDirectionButton = getUtils ().createImageButton (changeDirectionAction, null, null, null, goldToManaButtonNormal, goldToManaButtonPressed, goldToManaButtonNormal);
		bottomRow.add (changeDirectionButton, getUtils ().createConstraintsNoFill (1, 0, 1, 1, new Insets (0, 8, 0, 8), GridBagConstraintsNoFill.CENTRE));

		bottomRow.add (getUtils ().createImageButton (okAction, MomUIConstants.LIGHT_GRAY, MomUIConstants.DARK_GRAY, getLargeFont (), buttonNormal, buttonPressed, buttonDisabled),
			getUtils ().createConstraintsNoFill (2, 0, 1, 1, INSET, GridBagConstraintsNoFill.EAST));
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);
		getFrame ().setUndecorated (true);
		
		getFrame ().setShape (new Polygon
			(new int [] {0, TL_CORNER, background.getWidth () - BR_CORNER, background.getWidth () , background.getWidth () , background.getWidth () - BR_CORNER, TL_CORNER, 0},
			new int [] {TL_CORNER, 0, 0, TL_CORNER, background.getHeight () - BR_CORNER, background.getHeight () , background.getHeight () , background.getHeight () - BR_CORNER},
			8));		
		
		log.trace ("Exiting init");
	}		

	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged");
		
		getFrame ().setTitle (getLanguage ().findCategoryEntry ("frmAlchemy", "Title"));
		
		okAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmAlchemy", "OK"));
		cancelAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmAlchemy", "Cancel"));
		
		directionChanged ();

		log.trace ("Exiting languageChanged");
	}
	
	/**
	 * Updates the conversion label when either the language or the conversion direction changes
	 */
	private final void directionChanged ()
	{
		log.trace ("Entering directionChanged: " + fromProductionTypeID + " -> " + toProductionTypeID);
		
		final ProductionType fromProductionType	= getLanguage ().findProductionType (fromProductionTypeID);
		final ProductionType toProductionType		= getLanguage ().findProductionType (toProductionTypeID);
		
		conversionLabel.setText (getLanguage ().findCategoryEntry ("frmAlchemy", "Conversion").replaceAll
			("FROM_PRODUCTION_TYPE", (fromProductionType != null) ? fromProductionType.getProductionTypeDescription () : fromProductionTypeID).replaceAll
			("TO_PRODUCTION_TYPE", (toProductionType != null) ? toProductionType.getProductionTypeDescription () : toProductionTypeID));

		updateSliderMaximum ();

		log.trace ("Exiting directionChanged");
	}
	
	/**
	 * Update the maximum range of the slider, this could be because
	 * 1) We clicked the direction arrow, to alter whether we're converting gold>mana or mana>gold
	 * 2) The amount of the resource that we have changed
	 * 3) We gained the alchemy retort when we previously didn't have it
	 */
	public final void updateSliderMaximum ()
	{
		log.trace ("Entering updateSliderMaximum: " + fromProductionTypeID + " -> " + toProductionTypeID);

		// This often gets called before we've ever used the screen and so before the controls exist
		if (slider != null)
		{		
			// Update slider maximum.
			// If we have alchemy, we allow the value to go from 0..999.
			// If we don't have alchemy, we must only allow even numbers, which we do by setting the slider range only up to 0..499 and then doubling it when we read it.
			try
			{
				int fromValue = getResourceValueUtils ().findAmountStoredForProductionType
					(getClient ().getOurPersistentPlayerPrivateKnowledge ().getResourceValue (), fromProductionTypeID);

				final PlayerPublicDetails ourPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getClient ().getOurPlayerID (), "updateSliderMaximum");
				final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) ourPlayer.getPersistentPlayerPublicKnowledge ();
			
				if (getPlayerPickUtils ().getQuantityOfPick (pub.getPick (), CommonDatabaseConstants.VALUE_RETORT_ID_ALCHEMY) > 0)
					fromValue = Math.min (fromValue, 999);
				else
					fromValue = Math.min (fromValue, 998) / 2;
			
				slider.setMaximum (fromValue);
			}
			catch (final Exception e)
			{
				log.error (e, e);
			}
		
			// Update slider labels accordingly
			sliderPositionChanged ();
		}
		
		log.trace ("Exiting updateSliderMaximum");
	}
	
	/**
	 * Updates the gold and mana labels as we move the slider
	 */
	public final void sliderPositionChanged ()
	{
		log.trace ("Entering sliderPositionChanged");
		
		try
		{
			// First get the two numbers
			int fromValue = slider.getValue ();
			final int toValue = fromValue;

			// Can't click OK if the value is zero
			okAction.setEnabled (fromValue > 0);
			
			// If we do not have alchemy retort, then the actual slider value represents half, so it is only possible to select even numbers
			final PlayerPublicDetails ourPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getClient ().getOurPlayerID (), "sliderPositionChanged");
			final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) ourPlayer.getPersistentPlayerPublicKnowledge ();

			if (getPlayerPickUtils ().getQuantityOfPick (pub.getPick (), CommonDatabaseConstants.VALUE_RETORT_ID_ALCHEMY) == 0) 
				fromValue = fromValue * 2;
		
			// Then figure out which represents which resource
			final int goldValue;
			final int manaValue;
			if (fromProductionTypeID.equals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD))
			{
				goldValue = fromValue;
				manaValue = toValue;
			}
			else
			{
				goldValue = toValue;
				manaValue = fromValue;
			}
		
			// Update the labels
			String goldText = new Integer (goldValue).toString ();
			String manaText = new Integer (manaValue).toString ();

			final ProductionType goldProduction = getLanguage ().findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD);
			if ((goldProduction != null) && (goldProduction.getProductionTypeSuffix () != null))
				goldText = goldText + " " + goldProduction.getProductionTypeSuffix ();

			final ProductionType manaProduction = getLanguage ().findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA);
			if ((manaProduction != null) && (manaProduction.getProductionTypeSuffix () != null))
				manaText = manaText + " " + manaProduction.getProductionTypeSuffix ();
		
			goldLabel.setText (goldText);
			manaLabel.setText (manaText);
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
		log.trace ("Exiting sliderPositionChanged");
	}

	/**
	 * @return Large font
	 */
	public final Font getLargeFont ()
	{
		return largeFont;
	}

	/**
	 * @param font Large font
	 */
	public final void setLargeFont (final Font font)
	{
		largeFont = font;
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
	 * @return Player pick utils
	 */
	public final PlayerPickUtils getPlayerPickUtils ()
	{
		return playerPickUtils;
	}

	/**
	 * @param utils Player pick utils
	 */
	public final void setPlayerPickUtils (final PlayerPickUtils utils)
	{
		playerPickUtils = utils;
	}

	/**
	 * @return Resource value utils
	 */
	public final ResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
	}

	/**
	 * @param util Resource value utils
	 */
	public final void setResourceValueUtils (final ResourceValueUtils util)
	{
		resourceValueUtils = util;
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
}