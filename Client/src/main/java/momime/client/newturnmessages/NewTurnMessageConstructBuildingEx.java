package momime.client.newturnmessages;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.v0_9_5.CityViewElement;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.v0_9_5.Building;
import momime.client.language.database.v0_9_5.Unit;
import momime.client.ui.MomUIConstants;
import momime.client.ui.frames.ChangeConstructionUI;
import momime.client.ui.frames.NewTurnMessagesUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.client.utils.AnimationController;
import momime.common.messages.NewTurnMessageConstructBuilding;
import momime.common.messages.OverlandMapCityData;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.swing.GridBagConstraintsNoFill;
import com.ndg.swing.NdgUIUtils;

/**
 * NTM describing a building that completed construction
 */
public final class NewTurnMessageConstructBuildingEx extends NewTurnMessageConstructBuilding
	implements NewTurnMessageExpiration, NewTurnMessageComplexUI, NewTurnMessageClickable, NewTurnMessageAnimated, NewTurnMessageRepaintOnCityDataChanged, NewTurnMessageMusic
{
	/** Class logger */
	private final Log log = LogFactory.getLog (NewTurnMessageConstructBuildingEx.class);
	
	/** Space left around each text column */
	private final static int INSET = 2;
	
	/** Current status of this NTM */
	private NewTurnMessageStatus status;

	/** Small font */
	private Font smallFont;
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;

	/** Animation controller */
	private AnimationController anim;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
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
		return NewTurnMessageSortOrder.SORT_ORDER_CONSTRUCTION_COMPLETED;
	}

	/**
	 * @return Name of music file on the classpath to play when this NTM is displayed; null if this message has no music associated
	 */
	@Override
	public final String getMusicResourceName ()
	{
		return "/momime.client.music/MUSIC_108 - Finished a building.mp3";
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
				((NewTurnMessagesUI.SCROLL_WIDTH - (getGraphicsDB ().getLargestBuildingSize ().width * 2) - (INSET * 5)) / 2,
					getGraphicsDB ().getLargestBuildingSize ().height);
			
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
			constructionCompletedImage.setMinimumSize (getGraphicsDB ().getLargestBuildingSize ());
			constructionCompletedImage.setMaximumSize (getGraphicsDB ().getLargestBuildingSize ());
			constructionCompletedImage.setPreferredSize (getGraphicsDB ().getLargestBuildingSize ());
			panel.add (constructionCompletedImage, getUtils ().createConstraintsNoFill (1, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));

			nextConstructionLabel = getUtils ().createWrappingLabel (MomUIConstants.SILVER, getSmallFont ());
			nextConstructionLabel.setMinimumSize (labelSize);
			nextConstructionLabel.setMaximumSize (labelSize);
			nextConstructionLabel.setPreferredSize (labelSize);
			panel.add (nextConstructionLabel, getUtils ().createConstraintsNoFill (2, 0, 1, 1, new Insets (0, INSET, 0, INSET), GridBagConstraintsNoFill.CENTRE));

			nextConstructionImage = new JLabel ();
			nextConstructionImage.setMinimumSize (getGraphicsDB ().getLargestBuildingSize ());
			nextConstructionImage.setMaximumSize (getGraphicsDB ().getLargestBuildingSize ());
			nextConstructionImage.setPreferredSize (getGraphicsDB ().getLargestBuildingSize ());
			panel.add (nextConstructionImage, getUtils ().createConstraintsNoFill (3, 0, 1, 1, new Insets (0, 0, 0, INSET), GridBagConstraintsNoFill.CENTRE));
		}
		
		// Set labels for this NTM
		final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
			(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ()).getCityData ();

		final Building oldBuilding = getLanguage ().findBuilding (getBuildingID ());
		constructionCompletedLabel.setText (getLanguage ().findCategoryEntry ("NewTurnMessages", "ConstructionCompleted").replaceAll
			("CITY_NAME", (cityData == null) ? "" : cityData.getCityName ()).replaceAll
			("OLD_CONSTRUCTION", (oldBuilding != null) ? oldBuilding.getBuildingName () : getBuildingID ()));
		
		String text = getLanguage ().findCategoryEntry ("NewTurnMessages", "NextConstruction");
		
		if (cityData.getCurrentlyConstructingBuildingID () != null)
		{
			final Building newBuilding = getLanguage ().findBuilding (cityData.getCurrentlyConstructingBuildingID ());
			text = text.replaceAll ("NEW_CONSTRUCTION", (newBuilding != null) ? newBuilding.getBuildingName () : cityData.getCurrentlyConstructingBuildingID ());
		}

		if (cityData.getCurrentlyConstructingUnitID () != null)
		{
			final Unit newUnit = getLanguage ().findUnit (cityData.getCurrentlyConstructingUnitID ());
			text = text.replaceAll ("NEW_CONSTRUCTION", (newUnit != null) ? newUnit.getUnitName () : cityData.getCurrentlyConstructingUnitID ());
		}
		
		nextConstructionLabel.setText (text);

		// Look up the image for the old building
		constructionCompletedImage.setIcon (null);
		try
		{
			final CityViewElement buildingImage = getGraphicsDB ().findBuilding (getBuildingID (), "getComponent-Old");
			final BufferedImage image = getAnim ().loadImageOrAnimationFrame
				((buildingImage.getCityViewAlternativeImageFile () != null) ? buildingImage.getCityViewAlternativeImageFile () : buildingImage.getCityViewImageFile (),
				buildingImage.getCityViewAnimation (), true);

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
				final CityViewElement buildingImage = getGraphicsDB ().findBuilding (cityData.getCurrentlyConstructingBuildingID (), "getComponent-New");
				final BufferedImage image = getAnim ().loadImageOrAnimationFrame
					((buildingImage.getCityViewAlternativeImageFile () != null) ? buildingImage.getCityViewAlternativeImageFile () : buildingImage.getCityViewImageFile (),
					buildingImage.getCityViewAnimation (), true);

				nextConstructionImage.setIcon (new ImageIcon (image));
			}
			
			// Unit image
			if (cityData.getCurrentlyConstructingUnitID () != null)
			{
				final BufferedImage image = getUtils ().loadImage (getGraphicsDB ().findUnit
					(cityData.getCurrentlyConstructingUnitID (), "getComponent-New").getUnitOverlandImageFile ());

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
		final CityViewElement oldBuilding = getGraphicsDB ().findBuilding (getBuildingID (), "registerRepaintTriggers-Old");
		getAnim ().registerRepaintTrigger (oldBuilding.getCityViewAnimation (), newTurnMessagesList);

		// Look up the image for the new construction, if it is a building
		if (cityData.getCurrentlyConstructingBuildingID () != null)
		{
			final CityViewElement newBuilding = getGraphicsDB ().findBuilding (cityData.getCurrentlyConstructingBuildingID (), "registerRepaintTriggers-New");
			getAnim ().registerRepaintTrigger (newBuilding.getCityViewAnimation (), newTurnMessagesList);
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
	public final LanguageDatabaseEx getLanguage ()
	{
		return languageHolder.getLanguage ();
	}

	/**
	 * @return Graphics database
	 */
	public final GraphicsDatabaseEx getGraphicsDB ()
	{
		return graphicsDB;
	}

	/**
	 * @param db Graphics database
	 */
	public final void setGraphicsDB (final GraphicsDatabaseEx db)
	{
		graphicsDB = db;
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
}