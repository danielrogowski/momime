package momime.client.newturnmessages;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.utils.swing.GridBagConstraintsNoFill;
import com.ndg.utils.swing.NdgUIUtils;

import momime.client.MomClient;
import momime.client.graphics.AnimationContainer;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.ui.MomUIConstants;
import momime.client.ui.PlayerColourImageGenerator;
import momime.client.ui.frames.ChangeConstructionUI;
import momime.client.ui.frames.NewTurnMessagesUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.client.utils.AnimationController;
import momime.common.database.CityViewElement;
import momime.common.database.LanguageText;
import momime.common.database.UnitEx;
import momime.common.messages.NewTurnMessageConstructBuilding;
import momime.common.messages.NewTurnMessageTypeID;
import momime.common.messages.OverlandMapCityData;

/**
 * NTM describing a building that completed construction, or that we aborted trying to construct
 */
public final class NewTurnMessageConstructBuildingEx extends NewTurnMessageConstructBuilding
	implements NewTurnMessageExpiration, NewTurnMessageComplexUI, NewTurnMessageClickable, NewTurnMessageAnimated, NewTurnMessageRepaintOnCityDataChanged, NewTurnMessageMusic
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (NewTurnMessageConstructBuildingEx.class);
	
	/** Space left around each text column */
	private final static int INSET = 2;
	
	/** Current status of this NTM */
	private NewTurnMessageStatus status;

	/** Small font */
	private Font smallFont;
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Animation controller */
	private AnimationController anim;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** Player colour image generator */
	private PlayerColourImageGenerator playerColourImageGenerator;
	
	/** Panel created to display the NTM */
	private JPanel panel;
	
	/** Label displaying what we just constructed */
	private JTextArea constructionCompletedLabel;

	/** Image displaying what we just constructed */
	private JLabel constructionCompletedImage;
	
	/** Label displaying what we're now constructing */
	private JTextArea nextConstructionLabel;
	
	/** Image displaying what we're now constructing */
	private JLabel nextConstructionImage;
	
	/**
	 * @return One of the SORT_ORDER_ constants, indicating the sort order/title category to group this message under
	 */
	@Override
	public final NewTurnMessageSortOrder getSortOrder ()
	{
		return NewTurnMessageSortOrder.SORT_ORDER_CONSTRUCTION;
	}

	/**
	 * @return Name of music file on the classpath to play when this NTM is displayed; null if this message has no music associated
	 */
	@Override
	public final String getMusicResourceName ()
	{
		// Don't play cheery music for failing to construct a building :)
		return (getMsgType () == NewTurnMessageTypeID.ABORT_BUILDING) ? null : "/momime.client.music/MUSIC_108 - Finished a building.mp3";
	}
	
	/**
	 * @return Custom component to draw this NTM with
	 */
	@Override
	public final Component getComponent ()
	{
		// Set up panel if this is the first time
		if (panel == null)
		{
			// How much space do we have for the text?
			final Dimension labelSize = new Dimension
				((NewTurnMessagesUI.SCROLL_WIDTH - (getClient ().getClientDB ().getLargestBuildingSize ().width * 2) - (INSET * 5)) / 2,
					getClient ().getClientDB ().getLargestBuildingSize ().height);
			
			// Now set up the panel
			panel = new JPanel ();
			panel.setLayout (new GridBagLayout ());
			panel.setOpaque (false);
			
			constructionCompletedLabel = getUtils ().createWrappingLabel (MomUIConstants.SILVER, getSmallFont ());
			constructionCompletedLabel.setMinimumSize (labelSize);
			constructionCompletedLabel.setMaximumSize (labelSize);
			constructionCompletedLabel.setPreferredSize (labelSize);
			panel.add (constructionCompletedLabel, getUtils ().createConstraintsNoFill (0, 0, 1, 1, new Insets (0, INSET, 0, INSET), GridBagConstraintsNoFill.CENTRE));

			constructionCompletedImage = new JLabel ();
			constructionCompletedImage.setMinimumSize (getClient ().getClientDB ().getLargestBuildingSize ());
			constructionCompletedImage.setMaximumSize (getClient ().getClientDB ().getLargestBuildingSize ());
			constructionCompletedImage.setPreferredSize (getClient ().getClientDB ().getLargestBuildingSize ());
			panel.add (constructionCompletedImage, getUtils ().createConstraintsNoFill (1, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));

			nextConstructionLabel = getUtils ().createWrappingLabel (MomUIConstants.SILVER, getSmallFont ());
			nextConstructionLabel.setMinimumSize (labelSize);
			nextConstructionLabel.setMaximumSize (labelSize);
			nextConstructionLabel.setPreferredSize (labelSize);
			panel.add (nextConstructionLabel, getUtils ().createConstraintsNoFill (2, 0, 1, 1, new Insets (0, INSET, 0, INSET), GridBagConstraintsNoFill.CENTRE));

			nextConstructionImage = new JLabel ();
			nextConstructionImage.setMinimumSize (getClient ().getClientDB ().getLargestBuildingSize ());
			nextConstructionImage.setMaximumSize (getClient ().getClientDB ().getLargestBuildingSize ());
			nextConstructionImage.setPreferredSize (getClient ().getClientDB ().getLargestBuildingSize ());
			panel.add (nextConstructionImage, getUtils ().createConstraintsNoFill (3, 0, 1, 1, new Insets (0, 0, 0, INSET), GridBagConstraintsNoFill.CENTRE));
		}
		
		// Set labels for this NTM
		final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
			(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ()).getCityData ();

		try
		{
			final String oldBuildingName = getLanguageHolder ().findDescription (getClient ().getClientDB ().findBuilding (getBuildingID (), "getComponent-Old").getBuildingName ());
			
			final List<LanguageText> languageText;
			if (getMsgType () == NewTurnMessageTypeID.ABORT_BUILDING)
				languageText = getLanguages ().getNewTurnMessages ().getConstructionAborted ();
			else
				languageText = getLanguages ().getNewTurnMessages ().getConstructionCompleted ();
				
			constructionCompletedLabel.setText (getLanguageHolder ().findDescription (languageText).replaceAll
				("CITY_NAME", (cityData == null) ? "" : cityData.getCityName ()).replaceAll
				("OLD_CONSTRUCTION", oldBuildingName));
			
			String text = getLanguageHolder ().findDescription (getLanguages ().getNewTurnMessages ().getNextConstruction ());
			
			if (cityData.getCurrentlyConstructingBuildingID () != null)
				text = text.replaceAll ("NEW_CONSTRUCTION", getLanguageHolder ().findDescription
					(getClient ().getClientDB ().findBuilding (cityData.getCurrentlyConstructingBuildingID (), "getComponent-New").getBuildingName ()));
	
			if (cityData.getCurrentlyConstructingUnitID () != null)
				text = text.replaceAll ("NEW_CONSTRUCTION", getLanguageHolder ().findDescription (getClient ().getClientDB ().findUnit
					(cityData.getCurrentlyConstructingUnitID (), "NewTurnMessageConstructBuildingEx").getUnitName ()));
			
			nextConstructionLabel.setText (text);
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}

		// Look up the image for the old building
		constructionCompletedImage.setIcon (null);
		try
		{
			final CityViewElement buildingImage = getClient ().getClientDB ().findCityViewElementBuilding (getBuildingID (), "getComponent-Old");
			final BufferedImage image = getAnim ().loadImageOrAnimationFrame
				((buildingImage.getCityViewAlternativeImageFile () != null) ? buildingImage.getCityViewAlternativeImageFile () : buildingImage.getCityViewImageFile (),
				buildingImage.getCityViewAnimation (), true, AnimationContainer.COMMON_XML);

			constructionCompletedImage.setIcon (new ImageIcon (image));
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
		// Look up the image for the new building
		nextConstructionImage.setIcon (null);
		try
		{
			// Building image
			if (cityData.getCurrentlyConstructingBuildingID () != null)
			{
				final CityViewElement buildingImage = getClient ().getClientDB ().findCityViewElementBuilding (cityData.getCurrentlyConstructingBuildingID (), "getComponent-New");
				final BufferedImage image = getAnim ().loadImageOrAnimationFrame
					((buildingImage.getCityViewAlternativeImageFile () != null) ? buildingImage.getCityViewAlternativeImageFile () : buildingImage.getCityViewImageFile (),
					buildingImage.getCityViewAnimation (), true, AnimationContainer.COMMON_XML);

				nextConstructionImage.setIcon (new ImageIcon (image));
			}
			
			// Unit image
			if (cityData.getCurrentlyConstructingUnitID () != null)
			{
				final UnitEx unitDef = getClient ().getClientDB ().findUnit (cityData.getCurrentlyConstructingUnitID (), "getComponent-New");
				final Image image = getPlayerColourImageGenerator ().getOverlandUnitImage (unitDef, getClient ().getOurPlayerID (), true);

				nextConstructionImage.setIcon (new ImageIcon (image));
			}
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
		return panel;
	}
	
	/**
	 * Register repaint triggers for any animations displayed by this NTM
	 * @param newTurnMessagesList The JList that is displaying the NTMs
	 * @throws IOException If there is a problem
	 */
	@Override
	public final void registerRepaintTriggers (final JList<NewTurnMessageUI> newTurnMessagesList) throws IOException
	{
		final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
			(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ()).getCityData ();

		// Look up the image for the old building
		final CityViewElement oldBuilding = getClient ().getClientDB ().findCityViewElementBuilding (getBuildingID (), "registerRepaintTriggers-Old");
		getAnim ().registerRepaintTrigger (oldBuilding.getCityViewAnimation (), newTurnMessagesList, AnimationContainer.COMMON_XML);

		// Look up the image for the new construction, if it is a building
		if ((cityData != null) && (cityData.getCurrentlyConstructingBuildingID () != null))
		{
			final CityViewElement newBuilding = getClient ().getClientDB ().findCityViewElementBuilding (cityData.getCurrentlyConstructingBuildingID (), "registerRepaintTriggers-New");
			getAnim ().registerRepaintTrigger (newBuilding.getCityViewAnimation (), newTurnMessagesList, AnimationContainer.COMMON_XML);
		}
		
		// Units are displayed with their overland icon rather than the full combat tile and all the figures, so are never animated
	}
	
	/**
	 * Clicking on completed construction brings up the change construction screen
	 * @throws Exception If there is a problem
	 */
	@Override
	public final void clicked () throws Exception
	{
		// Is there a change construction window already open for this city?
		ChangeConstructionUI changeConstruction = getClient ().getChangeConstructions ().get (getCityLocation ().toString ());
		if (changeConstruction == null)
		{
			changeConstruction = getPrototypeFrameCreator ().createChangeConstruction ();
			changeConstruction.setCityLocation (new MapCoordinates3DEx ((MapCoordinates3DEx) getCityLocation ()));
			getClient ().getChangeConstructions ().put (getCityLocation ().toString (), changeConstruction);
		}
		
		changeConstruction.setVisible (true);
	}
	
	/**
	 * @return Current status of this NTM
	 */
	@Override
	public final NewTurnMessageStatus getStatus ()
	{
		return status;
	}
	
	/**
	 * @param newStatus New status for this NTM
	 */
	@Override
	public final void setStatus (final NewTurnMessageStatus newStatus)
	{
		status = newStatus;
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
	 * @return Language database holder
	 */
	public final LanguageDatabaseHolder getLanguageHolder ()
	{
		return languageHolder;
	}
	
	/**
	 * @param holder Language database holder
	 */
	public final void setLanguageHolder (final LanguageDatabaseHolder holder)
	{
		languageHolder = holder;
	}

	/**
	 * Convenience shortcut for accessing the Language XML database
	 * @return Language database
	 */
	public final MomLanguagesEx getLanguages ()
	{
		return languageHolder.getLanguages ();
	}

	/**
	 * @return Animation controller
	 */
	public final AnimationController getAnim ()
	{
		return anim;
	}

	/**
	 * @param controller Animation controller
	 */
	public final void setAnim (final AnimationController controller)
	{
		anim = controller;
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
	 * @return Helper methods and constants for creating and laying out Swing components
	 */
	public final NdgUIUtils getUtils ()
	{
		return utils;
	}

	/**
	 * @param util Helper methods and constants for creating and laying out Swing components
	 */
	public final void setUtils (final NdgUIUtils util)
	{
		utils = util;
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
	 * @return Player colour image generator
	 */
	public final PlayerColourImageGenerator getPlayerColourImageGenerator ()
	{
		return playerColourImageGenerator;
	}

	/**
	 * @param gen Player colour image generator
	 */
	public final void setPlayerColourImageGenerator (final PlayerColourImageGenerator gen)
	{
		playerColourImageGenerator = gen;
	}
}