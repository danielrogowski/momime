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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.ui.MomUIConstants;

/**
 * Renderer for drawing the details about each city on the cities list screen
 */
public final class CitiesListCellRenderer extends JPanel implements ListCellRenderer<CitiesListEntry>
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (CitiesListCellRenderer.class);
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Multiplayer client */
	private MomClient client;
	
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
	
	/** Images showing all the civilian population; clickable to set number of optional farmers */
	private JLabel cityPopulation;
	
	/** Images showing units garrisoned in the city */
	private JLabel cityUnits;
	
	/** Icon showing the weapon grade units constructed in this city will get */
	private JLabel cityWeaponGrade;
	
	/** Count of how many enchantments + curses are cast on this city */
	private JLabel cityEnchantments;
	
	/** Icon to open popup to select a building to sell */
	private JLabel sellIcon;
	
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

		cityPopulation = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		add (cityPopulation, "frmCitiesListRowPopulation");

		cityUnits = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		add (cityUnits, "frmCitiesListRowUnits");

		cityWeaponGrade = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		add (cityWeaponGrade, "frmCitiesListRowWeaponGrade");

		cityEnchantments = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		add (cityEnchantments, "frmCitiesListRowEnchantments");

		sellIcon = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		add (sellIcon, "frmCitiesListRowSell");

		cityCurrentlyConstructing = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		add (cityCurrentlyConstructing, "frmCitiesListRowCurrentlyConstructing");
	}

	/**
	 * Return this panel to draw itself
	 */
	@SuppressWarnings ("unused")
	@Override
	public final Component getListCellRendererComponent (final JList<? extends CitiesListEntry> list,
		final CitiesListEntry city, final int index, final boolean isSelected, final boolean cellHasFocus)
	{
		cityName.setText (city.getCityName ());
		cityUnits.setText (Integer.valueOf (city.getCityPopulation () / 1000).toString ());
		cityWeaponGrade.setText (Integer.valueOf (city.getRations ()).toString ());
		cityEnchantments.setText (Integer.valueOf (city.getGold ()).toString ());
		sellIcon.setText (Integer.valueOf (city.getProduction ()).toString ());
		
		try
		{
			cityPopulation.setText (getLanguageHolder ().findDescription (getClient ().getClientDB ().findRace (city.getCityRaceID (), "CitiesListCellRenderer").getRaceNameSingular ()));
			
			if (city.getCurrentlyConstructingBuildingID () != null)
				cityCurrentlyConstructing.setText (getLanguageHolder ().findDescription
					(getClient ().getClientDB ().findBuilding (city.getCurrentlyConstructingBuildingID (), "CitiesListCellRenderer").getBuildingName ()));
			else
				cityCurrentlyConstructing.setText (getLanguageHolder ().findDescription
					(getClient ().getClientDB ().findUnit (city.getCurrentlyConstructingUnitID (), "CitiesListCellRenderer").getUnitName ()));
		}
		catch (final Exception e)
		{
			log.error (e, e);
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