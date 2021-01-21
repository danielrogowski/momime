package momime.client.ui.renderer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import momime.client.MomClient;
import momime.client.graphics.AnimationContainer;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.ui.MomUIConstants;
import momime.client.utils.AnimationController;
import momime.common.database.Building;
import momime.common.database.CityViewElement;

/**
 * Renderer for drawing the name and image of a building in a list cell
 */
public final class BuildingListCellRenderer extends JPanel implements ListCellRenderer<Building>
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (BuildingListCellRenderer.class);
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;

	/** Multiplayer client */
	private MomClient client;

	/** Animation controller */
	private AnimationController anim;
	
	/** Label containing the text portion */
	private JLabel textLabel;
	
	/** Label containing the image portion */
	private JLabel imageLabel;
	
	/**
	 * Sets up the layout of the panel
	 */
	public final void init ()
	{
		setLayout (new BorderLayout ());
		
		textLabel = new JLabel ();
		add (textLabel, BorderLayout.WEST);
		
		imageLabel = new JLabel ();
		add (imageLabel, BorderLayout.EAST);
		
		setOpaque (false);
	}
	
	/**
	 * Sets up the label to draw the list cell
	 */
	@SuppressWarnings ("unused")
	@Override
	public final Component getListCellRendererComponent (final JList<? extends Building> list, final Building building,
		final int index, final boolean isSelected, final boolean cellHasFocus)
	{
		// Look up the name of the building
		try
		{
			textLabel.setText (getLanguageHolder ().findDescription (getClient ().getClientDB ().findBuilding (building.getBuildingID (), "BuildingListCellRenderer").getBuildingName ()));
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
		textLabel.setFont (getFont ());
		
		if (isSelected)
			textLabel.setForeground (MomUIConstants.SELECTED);
		else
			textLabel.setForeground (getForeground ());
		
		// Look up the image for the building
		imageLabel.setIcon (null);
		try
		{
			final CityViewElement buildingImage = getClient ().getClientDB ().findCityViewElementBuilding (building.getBuildingID (), "BuildingListCellRenderer");
			final BufferedImage image = getAnim ().loadImageOrAnimationFrame
				((buildingImage.getCityViewAlternativeImageFile () != null) ? buildingImage.getCityViewAlternativeImageFile () : buildingImage.getCityViewImageFile (),
				buildingImage.getCityViewAnimation (), true, AnimationContainer.COMMON_XML);

			imageLabel.setIcon (new ImageIcon (image));
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
		return this;
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
}