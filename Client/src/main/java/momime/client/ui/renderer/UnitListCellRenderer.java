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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.zorder.ZOrderGraphicsImmediateImpl;

import momime.client.MomClient;
import momime.client.calculations.ClientUnitCalculations;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.UnitLang;
import momime.client.ui.MomUIConstants;
import momime.client.utils.UnitClientUtils;
import momime.common.utils.ExpandedUnitDetails;

/**
 * Renderer for drawing the name and figures of a unit in a list cell
 */
public final class UnitListCellRenderer implements ListCellRenderer<ExpandedUnitDetails>
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

	/** Utils for drawing units */
	private UnitClientUtils unitClientUtils;
	
	/** Client unit calculations */
	private ClientUnitCalculations clientUnitCalculations;
	
	/** Font to write the text in */
	private Font font;

	/** Colour to write the text in */
	private Color foreground;
	
	/**
	 * Sets up the label to draw the list cell
	 */
	@SuppressWarnings ("unused")
	@Override
	public final Component getListCellRendererComponent (final JList<? extends ExpandedUnitDetails> list, final ExpandedUnitDetails unit,
		final int index, final boolean isSelected, final boolean cellHasFocus)
	{
		// Because this includes a custom paintComponent, we have to recreate everything every time
		final JPanel container = new JPanel ();
		container.setLayout (new BorderLayout ());
		container.setOpaque (false);
		
		final JLabel textLabel = new JLabel ();
		container.add (textLabel, BorderLayout.WEST);

		final ZOrderGraphicsImmediateImpl zOrderGraphics = new ZOrderGraphicsImmediateImpl (); 
		
		try
		{
			// Set up the panel
			final JPanel imagePanel = new JPanel ()
			{
				@Override
				protected final void paintComponent (final Graphics g)
				{
					try
					{
						zOrderGraphics.setGraphics (g);
						final String movingActionID = getClientUnitCalculations ().determineCombatActionID (unit.getUnit (), true);
						getUnitClientUtils ().drawUnitFigures (unit.getUnit (), movingActionID, 4, zOrderGraphics, 0, PANEL_SIZE.height - 32, true, true, 0, null);
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
		final UnitLang unitLang = getLanguage ().findUnit (unit.getUnitID ());
		textLabel.setText ((unitLang != null) ? unitLang.getUnitName () : unit.getUnitID ());
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
	 * @return Client unit calculations
	 */
	public final ClientUnitCalculations getClientUnitCalculations ()
	{
		return clientUnitCalculations;
	}

	/**
	 * @param calc Client unit calculations
	 */
	public final void setClientUnitCalculations (final ClientUnitCalculations calc)
	{
		clientUnitCalculations = calc;
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