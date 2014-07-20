package momime.client.ui.renderer;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.v0_9_5.CityViewElement;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.common.database.v0_9_5.Building;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.NdgUIUtils;

/**
 * Renderer for drawing the name and image of a building in a list cell
 */
public final class BuildingListCellRenderer extends JPanel implements ListCellRenderer<Building>
{
	/** Class logger */
	private final Log log = LogFactory.getLog (BuildingListCellRenderer.class);
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;

	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;

	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** Label containing the text portion */
	private JLabel textLabel;
	
	/** Label containing the image portion */
	private JLabel imageLabel;
	
	/**
	 * Set up the panel with a border layout
	 */
	public BuildingListCellRenderer ()
	{
		super (new BorderLayout ());
	}
	
	/**
	 * Sets up the layout of the panel
	 */
	public final void init ()
	{
		textLabel = new JLabel ();
		add (textLabel, BorderLayout.WEST);
		
		imageLabel = new JLabel ();
		add (imageLabel, BorderLayout.EAST);
	}
	
	/**
	 * Sets up the label to draw the list cell
	 */
	@Override
	public final Component getListCellRendererComponent (final JList<? extends Building> list, final Building building, final int index, final boolean isSelected, final boolean cellHasFocus)
	{
		// Look up the name of the building
		final momime.client.language.database.v0_9_5.Building buildingLang = getLanguage ().findBuilding (building.getBuildingID ());
		textLabel.setText ((buildingLang != null) ? buildingLang.getBuildingName () : building.getBuildingID ());
		textLabel.setFont (getFont ());
		textLabel.setForeground (getForeground ());
		
		// Look up the image for the building
		imageLabel.setIcon (null);
		try
		{
			final CityViewElement buildingImage = getGraphicsDB ().findBuilding (building.getBuildingID (), "BuildingListCellRenderer");
		
			final String imageName;
			if (buildingImage.getCityViewAlternativeImageFile () != null)
				imageName = buildingImage.getCityViewAlternativeImageFile ();
			else if (buildingImage.getCityViewImageFile () != null)
				imageName = buildingImage.getCityViewImageFile ();
			else
				imageName = getGraphicsDB ().findAnimation (buildingImage.getCityViewAnimation (), "BuildingListCellRenderer").getFrame ().get (0).getFrameImageFile ();

			imageLabel.setIcon (new ImageIcon (getUtils ().loadImage (imageName)));
		}
		catch (final Exception e)
		{
			log.warn (e, e);
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
}