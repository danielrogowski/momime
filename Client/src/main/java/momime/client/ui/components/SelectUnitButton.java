package momime.client.ui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.JToggleButton;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.NdgUIUtils;

import momime.client.MomClient;
import momime.client.ui.PlayerColourImageGenerator;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.utils.ExpandedUnitDetails;

/**
 * Buttons on the main map screen which select and deselect each unit.
 * The buttons show the player's colour, an image of the unit, a health indicator, experience and weapon grade.
 */
public final class SelectUnitButton extends JToggleButton
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (SelectUnitButton.class);
	
	/** Diameter of experience rings */
	private final static int EXPERIENCE_RING_SIZE = 4;

	/** Colour of health bar for units with over 50% health */
	private final static Color HEALTH_BAR_GREEN = new Color (0x00A400);
	
	/** Colour of health bar for units with between 25% and 50% health */
	private final static Color HEALTH_BAR_AMBER = new Color (0xE4D000);
	
	/** Colour of health bar for units with under 25% health */
	private final static Color HEALTH_BAR_RED = new Color (0xD40000);
	
	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Player colour image generator */
	private PlayerColourImageGenerator playerColourImageGenerator;

	/** Unit being selected */
	private ExpandedUnitDetails unit;
	
	/** Normal button appearance */
	private BufferedImage unitButtonNormal;	
	
	/** Pressed button appearance */
	private BufferedImage unitButtonPressed;
	
	/**
	 * Sets up the panel once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	public final void init () throws IOException
	{
		// Load in all necessary images		
		unitButtonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/overland/rightHandPanel/unitHealthButtonNormal.png");
		unitButtonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/overland/rightHandPanel/unitHealthButtonPressed.png");
	
		// Fix size
		final Dimension buttonSize = new Dimension (unitButtonNormal.getWidth (), unitButtonNormal.getHeight ());
		setMinimumSize (buttonSize);
		setMaximumSize (buttonSize);
		setPreferredSize (buttonSize);
	}
	
	/**
	 * Override default drawing of the label
	 */
	@Override
	protected final void paintComponent (final Graphics g)
	{
		// Offset the image when the button is pressed
		final int offset = (getModel ().isPressed () && getModel ().isArmed ()) ? 1 : 0;
		
		// Draw background
		g.drawImage ((offset > 0) ? unitButtonPressed : unitButtonNormal, 0, 0, null);
		
		// All the rest only makes sense if we have a unit to draw
		if (getUnit () != null)
		{
			try
			{
				// Only draw patch of player colour if this unit has been selected to move or belongs to somebody else
				if ((isSelected ()) || (!Integer.valueOf (getUnit ().getOwningPlayerID ()).equals (getClient ().getOurPlayerID ())))
				{
					final BufferedImage playerColour = getPlayerColourImageGenerator ().getUnitBackgroundImage (getUnit ().getOwningPlayerID ());
					g.drawImage (playerColour, 6 + offset, 3 + offset, null);
				}
			
				// Draw the unit itself
				final BufferedImage unitImage = getUtils ().loadImage (getClient ().getClientDB ().findUnit (getUnit ().getUnitID (), "SelectUnitButton").getUnitOverlandImageFile ());
				g.drawImage (unitImage, 6 + offset, 3 + offset, null);

				// Experience rings
				final ExperienceLevel expLevel = getUnit ().getModifiedExperienceLevel ();				
				if ((expLevel != null) && (expLevel.getRingCount () > 0))
				{
					g.setColor (new Color (Integer.parseInt (expLevel.getRingColour (), 16)));
			
					int x = 3;
					for (int n = 0; n < expLevel.getRingCount (); n++)
					{
						g.drawOval (x + offset, unitButtonNormal.getHeight () - EXPERIENCE_RING_SIZE - 3 + offset, EXPERIENCE_RING_SIZE - 1, EXPERIENCE_RING_SIZE - 1);
						x = x + EXPERIENCE_RING_SIZE + 1;
					}
				}
				
				// Weapon grade
				if ((getUnit ().getWeaponGrade () != null) && (getUnit ().getWeaponGrade ().getWeaponGradeMiniImageFile () != null))
				{
					final BufferedImage wepGradeImage = getUtils ().loadImage (getUnit ().getWeaponGrade ().getWeaponGradeMiniImageFile ());
					g.drawImage (wepGradeImage, unitButtonNormal.getWidth () - wepGradeImage.getWidth () - 3 + offset,
						unitButtonNormal.getHeight () - wepGradeImage.getHeight () - 3 + offset, null);
				}
				
				// Health bar
				final int damageTaken = getUnit ().getTotalDamageTaken ();
				final double healthProportion;
				if (damageTaken <= 0)
					healthProportion = 1;
				else
				{
					final double totalHits = getUnit ().getFullFigureCount () * getUnit ().getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS);					
					healthProportion = 1 - (damageTaken / totalHits);
				}
				
				if (healthProportion <= 0.25)
					g.setColor (HEALTH_BAR_RED);
				else if (healthProportion <= 0.5)
					g.setColor (HEALTH_BAR_AMBER);
				else
					g.setColor (HEALTH_BAR_GREEN);
				
				g.fillRect (6 + offset, 21 + offset, (int) (18 * healthProportion), 2);
			}
			catch (final IOException e)
			{
				log.error (e, e);
			}
		}
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
	
	/**
	 * @return Unit being selected
	 */
	public final ExpandedUnitDetails getUnit ()
	{
		return unit;
	}

	/**
	 * @param u Unit being selected
	 */
	public final void setUnit (final ExpandedUnitDetails u)
	{
		unit = u;
		repaint ();
	}
}