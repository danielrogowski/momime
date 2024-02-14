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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.utils.swing.GridBagConstraintsNoFill;
import com.ndg.utils.swing.NdgUIUtils;

import momime.client.MomClient;
import momime.client.graphics.AnimationContainer;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.ui.MomUIConstants;
import momime.client.ui.frames.ChangeConstructionUI;
import momime.client.ui.frames.NewTurnMessagesUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.client.utils.AnimationController;
import momime.client.utils.WizardClientUtils;
import momime.common.database.CityViewElement;
import momime.common.messages.NewTurnMessageDestroyBuilding;
import momime.common.messages.OverlandMapCityData;

/**
 * NTM describing a building that was destroyed by a spell.
 * If construction gets changed as a result, that gets sent as abort building/unit separately.
 */
public final class NewTurnMessageDestroyBuildingEx extends NewTurnMessageDestroyBuilding
	implements NewTurnMessageExpiration, NewTurnMessageComplexUI, NewTurnMessageClickable, NewTurnMessageAnimated
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
	
	/** Wizard client utils */
	private WizardClientUtils wizardClientUtils;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Panel created to display the NTM */
	private JPanel panel;
	
	/** Label displaying what was destroyed */
	private JTextArea label;

	/** Image displaying what was destroyed */
	private JLabel image;
	
	/**
	 * @return One of the SORT_ORDER_ constants, indicating the sort order/title category to group this message under
	 */
	@Override
	public final NewTurnMessageSortOrder getSortOrder ()
	{
		return NewTurnMessageSortOrder.SORT_ORDER_DESTROYED;
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
				(NewTurnMessagesUI.SCROLL_WIDTH - getClient ().getClientDB ().getLargestBuildingSize ().width - (INSET * 3),
					getClient ().getClientDB ().getLargestBuildingSize ().height);
			
			// Now set up the panel
			panel = new JPanel ();
			panel.setLayout (new GridBagLayout ());
			panel.setOpaque (false);
			
			label = getUtils ().createWrappingLabel (MomUIConstants.SILVER, getSmallFont ());
			label.setMinimumSize (labelSize);
			label.setMaximumSize (labelSize);
			label.setPreferredSize (labelSize);
			panel.add (label, getUtils ().createConstraintsNoFill (0, 0, 1, 1, new Insets (0, INSET, 0, INSET), GridBagConstraintsNoFill.CENTRE));

			image = new JLabel ();
			image.setMinimumSize (getClient ().getClientDB ().getLargestBuildingSize ());
			image.setMaximumSize (getClient ().getClientDB ().getLargestBuildingSize ());
			image.setPreferredSize (getClient ().getClientDB ().getLargestBuildingSize ());
			panel.add (image, getUtils ().createConstraintsNoFill (1, 0, 1, 1, new Insets (0, 0, 0, INSET), GridBagConstraintsNoFill.CENTRE));
		}
		
		// Set labels for this NTM
		final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
			(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ()).getCityData ();

		try
		{
			final String destroyedBuildingName = getLanguageHolder ().findDescription (getClient ().getClientDB ().findBuilding (getBuildingID (), "getComponent").getBuildingName ());
			final String spellName = getLanguageHolder ().findDescription (getClient ().getClientDB ().findSpell (getDestroyedBySpellID (), "getComponent").getSpellName ());
			
			final PlayerPublicDetails castingPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getCastingPlayerID ());

			String text = getLanguageHolder ().findDescription (getLanguages ().getNewTurnMessages ().getBuildingDestroyed ()).replaceAll
				("CITY_NAME", (cityData == null) ? "" : cityData.getCityName ()).replaceAll
				("DESTROYED_BUILDING", destroyedBuildingName).replaceAll
				("SPELL_NAME", spellName);
			
			if (castingPlayer != null)
				text = text.replaceAll ("PLAYER_NAME", getWizardClientUtils ().getPlayerName (castingPlayer));
			
			label.setText (text);
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}

		// Look up the image for the destroyed building
		image.setIcon (null);
		try
		{
			final CityViewElement buildingImage = getClient ().getClientDB ().findCityViewElementBuilding (getBuildingID (), "getComponent");
			final BufferedImage bufferedImage = getAnim ().loadImageOrAnimationFrame
				((buildingImage.getCityViewAlternativeImageFile () != null) ? buildingImage.getCityViewAlternativeImageFile () : buildingImage.getCityViewImageFile (),
				buildingImage.getCityViewAnimation (), true, AnimationContainer.COMMON_XML);

			image.setIcon (new ImageIcon (bufferedImage));
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
		final CityViewElement destroyedBuilding = getClient ().getClientDB ().findCityViewElementBuilding (getBuildingID (), "registerRepaintTriggers");
		getAnim ().registerRepaintTrigger (destroyedBuilding.getCityViewAnimation (), newTurnMessagesList, AnimationContainer.COMMON_XML);
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