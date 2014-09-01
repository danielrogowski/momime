package momime.client.ui.renderer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.ui.MomUIConstants;
import momime.client.utils.UnitClientUtils;
import momime.common.database.v0_9_5.Unit;
import momime.common.messages.v0_9_5.AvailableUnit;
import momime.common.utils.UnitUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Renderer for drawing the name and figures of a unit in a list cell
 */
public final class UnitListCellRenderer implements ListCellRenderer<Unit>
{
	/** Class logger */
	private final Log log = LogFactory.getLog (UnitListCellRenderer.class);
	
	/** Size of the image portion of the panel */
	private final static Dimension PANEL_SIZE = new Dimension (60, 56);
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;

	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Utils for drawing units */
	private UnitClientUtils unitClientUtils;
	
	/** Font to write the text in */
	private Font font;

	/** Colour to write the text in */
	private Color foreground;
	
	/**
	 * Sets up the label to draw the list cell
	 */
	@Override
	public final Component getListCellRendererComponent (final JList<? extends Unit> list, final Unit unitDef, final int index, final boolean isSelected, final boolean cellHasFocus)
	{
		// Because this includes a custom paintComponent, we have to recreate everything every time
		final JPanel container = new JPanel ();
		container.setLayout (new BorderLayout ());
		container.setOpaque (false);
		
		final JLabel textLabel = new JLabel ();
		container.add (textLabel, BorderLayout.WEST);

		// Create a dummy unit
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID (unitDef.getUnitID ());
		
		try
		{
			// We don't have to get the weapon grade or experience right just to draw the figures
			getUnitUtils ().initializeUnitSkills (unit, -1, true, getClient ().getClientDB ());
		
			// Set up the panel
			final JPanel imagePanel = new JPanel ()
			{
				private static final long serialVersionUID = 3648813308002413154L;

				@Override
				protected final void paintComponent (final Graphics g)
				{
					try
					{
						getUnitClientUtils ().drawUnitFigures (unit, GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, 4, g, 0, PANEL_SIZE.height - 32, true);
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
				}
			};

			imagePanel.setMinimumSize (PANEL_SIZE);
			imagePanel.setMaximumSize (PANEL_SIZE);
			imagePanel.setPreferredSize (PANEL_SIZE);
			imagePanel.setOpaque (false);
			container.add (imagePanel, BorderLayout.EAST);
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
		// Look up the name of the unit
		final momime.client.language.database.v0_9_5.Unit unitLang = getLanguage ().findUnit (unitDef.getUnitID ());
		textLabel.setText ((unitLang != null) ? unitLang.getUnitName () : unitDef.getUnitID ());
		textLabel.setFont (getFont ());
		
		if (isSelected)
			textLabel.setForeground (MomUIConstants.SELECTED);
		else
			textLabel.setForeground (getForeground ());
		
		return container;
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
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final UnitUtils utils)
	{
		unitUtils = utils;
	}
	
	/**
	 * @return Utils for drawing units
	 */
	public final UnitClientUtils getUnitClientUtils ()
	{
		return unitClientUtils;
	}

	/**
	 * @param util Utils for drawing units
	 */
	public final void setUnitClientUtils (final UnitClientUtils util)
	{
		unitClientUtils = util;
	}
	
	/**
	 * @return Font to write the text in
	 */
	public final Font getFont ()
	{
		return font;
	}

	/**
	 * @param newFont Font to write the text in
	 */
	public final void setFont (final Font newFont)
	{
		font = newFont;
	}
	
	/**
	 * @return Colour to write the text in
	 */
	public final Color getForeground ()
	{
		return foreground;
	}

	/**
	 * @param colour Colour to write the text in
	 */
	public final void setForeground (final Color colour)
	{
		foreground = colour;
	}
}