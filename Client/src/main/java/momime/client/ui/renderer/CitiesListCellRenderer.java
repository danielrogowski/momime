package momime.client.ui.renderer;

import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.language.database.BuildingLang;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.RaceLang;
import momime.client.language.database.UnitLang;
import momime.client.ui.MomUIConstants;

/**
 * Renderer for drawing the details about each city on the cities list screen
 */
public final class CitiesListCellRenderer extends JPanel implements ListCellRenderer<CitiesListEntry>
{
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** XML layout */
	private XmlLayoutContainerEx citiesListEntryLayout;
	
	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** Small font */
	private Font smallFont;
	
	/** Background image */
	private BufferedImage background;
	
	/** Label showing the city name */
	private JLabel cityName;
	
	/** Label showing the city race */
	private JLabel cityRace;
	
	/** Label showing the city population */
	private JLabel cityPopulation;
	
	/** Label showing the rations being produced by the city - rations being eaten by the population */
	private JLabel cityRations;
	
	/** Label showing the gold being generated by the city - gold maintainence costs */
	private JLabel cityGold;
	
	/** Label showing the production generated by the city */
	private JLabel cityProduction;
	
	/** Label showing what's currently being constructed in the city */
	private JLabel cityCurrentlyConstructing;
	
	/**
	 * Loads the background image for the panel
	 * @throws IOException If there is a problem
	 */
	public final void init () throws IOException
	{
		background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/citiesListRow.png");
		
		setLayout (new XmlLayoutManager (getCitiesListEntryLayout ()));

		cityName = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		add (cityName, "frmCitiesListRowName");

		cityRace = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		add (cityRace, "frmCitiesListRowRace");

		cityPopulation = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		add (cityPopulation, "frmCitiesListRowPopulation");

		cityRations = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		add (cityRations, "frmCitiesListRowRations");

		cityGold = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		add (cityGold, "frmCitiesListRowGold");

		cityProduction = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		add (cityProduction, "frmCitiesListRowProduction");

		cityCurrentlyConstructing = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		add (cityCurrentlyConstructing, "frmCitiesListRowCurrentlyConstructing");
	}

	/**
	 * Return this panel to draw itself
	 */
	@Override
	public final Component getListCellRendererComponent (final JList<? extends CitiesListEntry> list,
		final CitiesListEntry city, final int index, final boolean isSelected, final boolean cellHasFocus)
	{
		cityName.setText (city.getCityName ());
		cityPopulation.setText (new Integer (city.getCityPopulation () / 1000).toString ());
		cityRations.setText (new Integer (city.getRations ()).toString ());
		cityGold.setText (new Integer (city.getGold ()).toString ());
		cityProduction.setText (new Integer (city.getProduction ()).toString ());
		
		final RaceLang race = getLanguage ().findRace (city.getCityRaceID ());
		final String raceName = (race != null) ? race.getRaceName () : null;
		cityRace.setText ((raceName != null) ? raceName : city.getCityRaceID ());
		
		if (city.getCurrentlyConstructingBuildingID () != null)
		{
			final BuildingLang building = getLanguage ().findBuilding (city.getCurrentlyConstructingBuildingID ());
			final String buildingName = (building != null) ? building.getBuildingName () : null;
			cityCurrentlyConstructing.setText ((buildingName != null) ? buildingName : city.getCurrentlyConstructingBuildingID ());
		}
		else
		{
			final UnitLang unit = getLanguage ().findUnit (city.getCurrentlyConstructingUnitID ());
			final String unitName = (unit != null) ? unit.getUnitName () : null;
			cityCurrentlyConstructing.setText ((unitName != null) ? unitName : city.getCurrentlyConstructingUnitID ());
		}
		
		return this;
	}

	/**
	 * Paint the panel background
	 */
	@Override
	protected final void paintComponent (final Graphics g)
	{
		g.drawImage (background, 0, 0, background.getWidth () * 2, background.getHeight () * 2, null);
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
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getCitiesListEntryLayout ()
	{
		return citiesListEntryLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setCitiesListEntryLayout (final XmlLayoutContainerEx layout)
	{
		citiesListEntryLayout = layout;
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
}