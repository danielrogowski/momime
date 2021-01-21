package momime.client.ui.frames;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Optional;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.actions.LoggingAction;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutComponent;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.languages.database.Month;
import momime.client.ui.MomUIConstants;
import momime.client.utils.WizardClientUtils;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.utils.PlayerKnowledgeUtils;

/**
 * UI for screen showing power base history for each wizard
 */
public final class HistoryUI extends MomClientFrameUI
{
	/** How spaced apart to draw score ticks */
	private final static int SCORE_TICK = 100;

	/** How spaced apart to draw turn ticks */
	private final static int TURN_TICK = 12;
	
	/** Length in pixels of axes ticks */
	private final static int TICK_SIZE = 6;

	/** XML layout */
	private XmlLayoutContainerEx historyLayout;

	/** Large font */
	private Font largeFont;

	/** Small font */
	private Font smallFont;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Wizard client utils */
	private WizardClientUtils wizardClientUtils;

	/** Turn label */
	private JLabel turnLabel;

	/** Title label */
	private JLabel titleLabel;
	
	/** Content pane */
	private JPanel contentPane;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/history.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/okButton41x15Normal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/okButton41x15Pressed.png");

		// Actions
		final Action okAction = new LoggingAction ((ev) ->
		{
			getFrame ().setVisible (false);
		});				
		
		// Initialize the content pane
		contentPane = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g1)
			{
				final Graphics2D g = (Graphics2D) g1;
				g.drawImage (background, 0, 0, background.getWidth () * 2, background.getHeight () * 2, null);

				// Draw chart area
				final XmlLayoutComponent chart = getHistoryLayout ().findComponent ("frmHistoryChart");
				if (chart != null)
				{
					// Find the highest values
					int maxScore = 0;
					int maxTurns = 0;
					for (final PlayerPublicDetails player : getClient ().getPlayers ())
					{
						final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
						if (PlayerKnowledgeUtils.isWizard (pub.getWizardID ()))
						{
							maxScore = Math.max (maxScore, pub.getPowerBaseHistory ().stream ().mapToInt (v -> v).max ().orElse (0));
							maxTurns = Math.max (maxTurns, pub.getPowerBaseHistory ().size ());
						}
					}
					
					// Work out scaling
					int xScaling = 1;
					while (maxTurns > (chart.getWidth () / 2) * xScaling)
						xScaling++;
					
					int yScaling = 1;
					while (maxScore > chart.getHeight () * yScaling)
						yScaling++;
					
					// Antialias all the lines
					g.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g.setFont (getSmallFont ());

					// Data for each wizard
					g.setStroke (new BasicStroke (2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f));

					int wizardNo = 0;
					for (final PlayerPublicDetails player : getClient ().getPlayers ())
					{
						final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
						if (PlayerKnowledgeUtils.isWizard (pub.getWizardID ()))
						{
							final MomTransientPlayerPublicKnowledge trans = (MomTransientPlayerPublicKnowledge) player.getTransientPlayerPublicKnowledge ();
							g.setColor (new Color (Integer.parseInt (trans.getFlagColour (), 16)));
							
							// Output name
							g.drawString (getWizardClientUtils ().getPlayerName (player), 56, 66 + (wizardNo * 10));
							
							// Draw line
							final int x [] = new int [pub.getPowerBaseHistory ().size ()];
							final int y [] = new int [pub.getPowerBaseHistory ().size ()];
							for (int n = 0; n < pub.getPowerBaseHistory ().size () / xScaling; n++)
							{
								x [n] = getChartX (chart, n);
								y [n] = getChartY (chart, pub.getPowerBaseHistory ().get (n * xScaling) / yScaling);
							}
							g.drawPolyline (x, y, pub.getPowerBaseHistory ().size () / xScaling);
							
							wizardNo++;
						}
					}
					
					// Draw axes
					g.setColor (MomUIConstants.GOLD);
					g.setStroke (new BasicStroke (2.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f));
					
					g.drawPolyline
						(new int [] {chart.getLeft (), chart.getLeft (), chart.getLeft () + chart.getWidth ()},
						 new int [] {chart.getTop (), chart.getTop () + chart.getHeight (), chart.getTop () + chart.getHeight ()}, 3);
					
					// Draw score ticks every 100
					final int scoreTickCount = chart.getHeight () / SCORE_TICK;
					for (int tick = 0; tick <= scoreTickCount; tick++)
					{
						final int score = tick * SCORE_TICK;
						final int y = getChartY (chart, score);
						g.drawLine (chart.getLeft (), y, chart.getLeft () - TICK_SIZE, y);
						
						final String scoreText = Integer.valueOf (score * yScaling).toString ();
						final int scoreWidth = g.getFontMetrics ().stringWidth (scoreText);
						g.drawString (scoreText, 40 - scoreWidth, y + 4);
					}
					
					final int turnTickCount = chart.getWidth () / TURN_TICK / 2;
					for (int tick = 0; tick <= turnTickCount; tick++)
					{
						final int turnNumber = tick * TURN_TICK;
						final int x = getChartX (chart, turnNumber);
						g.drawLine (x, chart.getTop () + chart.getHeight (), x, chart.getTop () + chart.getHeight () + TICK_SIZE);
						if (turnNumber % 60 == 0)
							g.drawString (Integer.valueOf (((turnNumber / 12) * xScaling) + 1400).toString (), x - 13, 380);
					}
				}
			}
		};
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getHistoryLayout ()));

		contentPane.add (getUtils ().createImageButton (okAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getLargeFont (),
			buttonNormal, buttonPressed, buttonNormal), "frmHistoryOK");
		
		turnLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		contentPane.add (turnLabel, "frmHistoryTurn");
		
		titleLabel = getUtils ().createLabel (MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (titleLabel, "frmHistoryTitle");
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);
	}
	
	/**
	 * @param chart Chart location details
	 * @param turnNumber Turn number to show on chart
	 * @return X coordinate for turn 
	 */
	private final int getChartX (final XmlLayoutComponent chart, final int turnNumber)
	{
		return chart.getLeft () + (turnNumber * 2);
	}
	
	/**
	 * @param chart Chart location details
	 * @param score Score number to show on chart
	 * @return Y coordinate for turn 
	 */
	private final int getChartY (final XmlLayoutComponent chart, final int score)
	{
		return chart.getTop () + chart.getHeight () - score;
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		titleLabel.setText (getLanguageHolder ().findDescription (getLanguages ().getHistoryScreen ().getTitle ()));
		getFrame ().setTitle (titleLabel.getText ());
	}
	
	/**
	 * Updates the turn label
	 */
	public final void updateTurnLabelText ()
	{
		if (turnLabel != null)
		{
			// Turn 1 is January 1400 - so last turn in 1400 is turn 12
			// Months are numbered 1-12
			final int year = 1400 + ((getClient ().getGeneralPublicKnowledge ().getTurnNumber () - 1) / 12);
			int month = getClient ().getGeneralPublicKnowledge ().getTurnNumber () % 12;
			if (month == 0)
				month = 12;
	
			// Build up description
			final int monthNumber = month;
			final Optional<Month> monthLang = getLanguages ().getMonth ().stream ().filter (m -> m.getMonthNumber () == monthNumber).findAny ();
			final String monthText = monthLang.isEmpty () ? Integer.valueOf (month).toString () :
				getLanguageHolder ().findDescription (monthLang.get ().getName ());
			
			turnLabel.setText (getLanguageHolder ().findDescription (getLanguages ().getOverlandMapScreen ().getMapButtonBar ().getTurn ()).replaceAll
				("MONTH", monthText).replaceAll ("YEAR", Integer.valueOf (year).toString ()).replaceAll
				("TURN", Integer.valueOf (getClient ().getGeneralPublicKnowledge ().getTurnNumber ()).toString ()));
		}
	}
	
	/**
	 * Update the chart as we receive more data
	 */
	public final void redrawChart ()
	{
		if (contentPane != null)
			contentPane.repaint ();
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getHistoryLayout ()
	{
		return historyLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setHistoryLayout (final XmlLayoutContainerEx layout)
	{
		historyLayout = layout;
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
}