package momime.client.utils;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.JComponent;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.calculations.MomClientUnitCalculations;
import momime.client.config.v0_9_5.MomImeClientConfig;
import momime.client.graphics.database.CombatTileFigurePositionsEx;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.UnitEx;
import momime.client.graphics.database.v0_9_5.FigurePositionsForFigureCount;
import momime.client.graphics.database.v0_9_5.UnitCombatImage;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.v0_9_5.Race;
import momime.client.process.OverlandMapProcessing;
import momime.client.ui.components.SelectUnitButton;
import momime.client.ui.frames.CityViewUI;
import momime.client.ui.frames.UnitInfoUI;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.common.MomException;
import momime.common.calculations.MomUnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_5.Unit;
import momime.common.messages.servertoclient.v0_9_5.KillUnitActionID;
import momime.common.messages.v0_9_5.AvailableUnit;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.messages.v0_9_5.UnitStatusID;
import momime.common.utils.PendingMovementUtils;
import momime.common.utils.UnitUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.NdgUIUtils;

/**
 * Client side only helper methods for dealing with units
 */
public final class UnitClientUtilsImpl implements UnitClientUtils
{
	/** Class logger */
	private final Log log = LogFactory.getLog (UnitClientUtilsImpl.class);
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Animation controller */
	private AnimationController anim;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Unit calculations */
	private MomUnitCalculations unitCalculations;
	
	/** Client unit calculations */
	private MomClientUnitCalculations clientUnitCalculations;
	
	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** Overland map right hand panel showing economy etc */
	private OverlandMapRightHandPanel overlandMapRightHandPanel;
	
	/** Turn sequence and movement helper methods */
	private OverlandMapProcessing overlandMapProcessing;
	
	/** Pending movement utils */
	private PendingMovementUtils pendingMovementUtils;
	
	/** Client config, containing the scale setting */
	private MomImeClientConfig clientConfig;
	
	/**
	 * Note the generated unit names are obviously very dependant on the selected language, but the names themselves don't get notified
	 * to update themselves when the language changes.  It is the responsibility of whatever is calling this method to register itself to be
	 * notified of language updates, and cause this method to be re-evalulated when that happens.
	 * 
	 * @param unit Unit to generate the name of
	 * @param unitNameType Type of name to generate (see comments against that enum)
	 * @return Generated unit name
	 * @throws RecordNotFoundException If we can't find the unit definition in the server XML
	 */
	@Override
	public final String getUnitName (final AvailableUnit unit, final UnitNameType unitNameType) throws RecordNotFoundException
	{
		// Heroes just output their name for all unitNameTypes, so in that case we don't need to look up anything at all
		String unitName = null;
		if (unit instanceof MemoryUnit)
			unitName = ((MemoryUnit) unit).getUnitName ();
		
		if (unitName == null)
		{
			// Find the name in the language XML
			final momime.client.language.database.v0_9_5.Unit unitLang = getLanguage ().findUnit (unit.getUnitID ());
			unitName = (unitLang != null) ? unitLang.getUnitName () : unit.getUnitID ();
			
			// If we just want the simple name with no race prefix, then we're done
			if (unitNameType != UnitNameType.SIMPLE_UNIT_NAME)
			{
				// Need the record from the server XML to know whether to put the racial prefix on or not
				final Unit unitInfo = getClient ().getClientDB ().findUnit (unit.getUnitID (), "getUnitName");
				if ((unitInfo.isIncludeRaceInUnitName () != null) && (unitInfo.isIncludeRaceInUnitName ()))
				{
					final Race race = getLanguage ().findRace (unitInfo.getUnitRaceID ());
					unitName = ((race != null) ? race.getRaceName () : unitInfo.getUnitRaceID ()) + " " + unitName;
				}
				
				// How we modify this now depends on the requested unitNameType
				switch (unitNameType)
				{
					case A_UNIT_NAME:
						if ((unitLang != null) && (unitLang.getUnitNamePrefix () != null))
							unitName = unitLang.getUnitNamePrefix () + " " + unitName;
						break;
						
					case THE_UNIT_OF_NAME:
						final String languageEntryID;
						if ((unitLang != null) && (unitLang.getUnitNamePrefix () != null))
							languageEntryID = "Singular";
						else
							languageEntryID = "Plural";
						
						unitName = getLanguage ().findCategoryEntry ("UnitName", "TheUnitOfName" + languageEntryID).replaceAll ("RACE_UNIT_NAME", unitName);
						break;
						
					// Other name types are handled elsewhere or require nothing to be added
					default:
						break;
				}
			}
		}
		
		return unitName;
	}

	/**
	 * Kills a unit, either permanently removing it or marking it as dead in case it gets Raise or Animate Dead cast on it later
	 * 
	 * @param unitURN Unit to kill
	 * @param action Type of update
	 * @throws IOException If there is a problem
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final void killUnit (final int unitURN, final KillUnitActionID action) throws IOException, JAXBException, XMLStreamException
	{
		log.trace ("Entering killUnit: Unit URN " + unitURN + ", " + action);

		// Even if not actually freeing the unit, we still need to eliminate all references to it, except for it being in the main unit list
		getPendingMovementUtils ().removeUnitFromAnyPendingMoves (getClient ().getOurTransientPlayerPrivateKnowledge ().getPendingMovement (), unitURN);
		getUnitUtils ().beforeKillingUnit (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), unitURN);	// Removes spells cast on unit
		
		// Is there a unit info screen open for it?
		final UnitInfoUI unitInfo = getClient ().getUnitInfos ().get (unitURN);
		if (unitInfo != null)
			unitInfo.close ();
		
		// Select unit buttons on the Map
		for (final SelectUnitButton button : getOverlandMapRightHandPanel ().getSelectUnitButtons ())
			if ((button.getUnit () != null) && (button.getUnit ().getUnitURN () == unitURN))
			{
				button.setUnit (null);
				
				final boolean updateMovement = (button.isVisible ()) && (button.isSelected ());
				button.setSelected (false);
				button.setVisible (false);
					
				if (updateMovement)
				{
					// Do same processing as if button was manually clicked
					getOverlandMapProcessing ().enableOrDisableSpecialOrderButtons ();
					getOverlandMapProcessing ().updateMovementRemaining ();
				}
			}
		
		// Find the unit being removed
		final MemoryUnit unit = getUnitUtils ().findUnitURN (unitURN,
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "killUnit");
		
		// The server works out what action we need to take
		switch (action)
		{
			// Phyically free the unit
			case FREE:
			case VISIBLE_AREA_CHANGED:
			case UNIT_LACK_OF_PRODUCTION:		// <-- Delphi client had different logic for this due to finicky memory management, TBC if needs to be different here or not
			case HERO_LACK_OF_PRODUCTION:
				getUnitUtils ().removeUnitURN (unitURN, getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ());
				break;
				
			// The two special statuses generated by ApplyDamageMessage on the client aren't handled yet
			
			default:
				throw new MomException ("killUnit got a KillUnitActionID that it doesn't know how to handle: " + action);
		}

		// Select unit buttons on the City screen
		if ((unit.getStatus () == UnitStatusID.ALIVE) && (unit.getUnitLocation () != null))
		{
			final CityViewUI cityView = getClient ().getCityViews ().get (unit.getUnitLocation ().toString ());
			if (cityView != null)
				cityView.unitsChanged ();
		}
		
		log.trace ("Exiting killUnit");
	}
	
	/**
	 * Many unit figures are animated, and so must call this routine to register the animation prior to calling drawUnitFigures. 
	 * NB. This has to work without relying on the AvailableUnit so that we can draw units on the Options screen before joining a game.
	 * 
	 * @param unitID The unit to draw
	 * @param combatActionID The action to show the unit doing
	 * @param direction The direction to show the unit facing
	 * @param component The component that the unit will be drawn onto
	 * @throws IOException If there is a problem
	 */
	@Override
	public final void registerUnitFiguresAnimation (final String unitID, final String combatActionID, final int direction, final JComponent component) throws IOException
	{
		final UnitEx unit = getGraphicsDB ().findUnit (unitID, "registerUnitFiguresAnimation");
		final UnitCombatImage unitImage = unit.findCombatAction (combatActionID, "registerUnitFiguresAnimation").findDirection (direction, "registerUnitFiguresAnimation");
		getAnim ().registerRepaintTrigger (unitImage.getUnitCombatAnimation (), component);
		
		if (unit.getSecondaryUnitID () != null)
		{
			final UnitEx secondaryUnit = getGraphicsDB ().findUnit (unit.getSecondaryUnitID (), "registerUnitFiguresAnimation");
			final UnitCombatImage secondaryUnitImage = secondaryUnit.findCombatAction (combatActionID, "registerUnitFiguresAnimation").findDirection (direction, "registerUnitFiguresAnimation");
			getAnim ().registerRepaintTrigger (secondaryUnitImage.getUnitCombatAnimation (), component);
		}
	}

	/**
	 * Draws the figures of a unit.
	 * NB. This has to work without relying on the AvailableUnit so that we can draw units on the Options screen before joining a game.
	 * 
	 * @param unitID The unit to draw
	 * @param unitTypeID The type of unit it is (can pass in null - the only value it cares about is if this is 'S' for summoned units)
	 * @param totalFigureCount The number of figures the unit had when fully healed
	 * @param aliveFigureCount The number of figures the unit has now
	 * @param combatActionID The action to show the unit doing
	 * @param direction The direction to show the unit facing
	 * @param g The graphics context to draw the unit onto
	 * @param offsetX The x offset into the graphics context to draw the unit at
	 * @param offsetY The y offset into the graphics context to draw the unit at
	 * @param sampleTileImageFile The filename of the sample tile (grass or ocean) to draw under this unit; if null, then no sample tile will be drawn
	 * @throws IOException If there is a problem
	 */
	@Override
	public final void drawUnitFigures (final String unitID, final String unitTypeID, final int totalFigureCount, final int aliveFigureCount, final String combatActionID,
		final int direction, final Graphics g, final int offsetX, final int offsetY, final String sampleTileImageFile) throws IOException
	{
		// Draw sample tile
		if (sampleTileImageFile != null)
		{
			final BufferedImage tileImage = getUtils ().loadImage (sampleTileImageFile);
			g.drawImage (tileImage, offsetX, offsetY, tileImage.getWidth () * 2, tileImage.getHeight () * 2, null);
		}

		// Get the main unit
		final UnitEx unit = getGraphicsDB ().findUnit (unitID, "drawUnitFigures");
		
		// relativeScale = which set of positions from the graphics XML to use for this unit's figures
		// relativeScaleMultiplier = what to multiply the tileRelativeX, Y in th egraphics XML by
		// unitImageMultiplier = how much to multiply the size of the unit figures by
		// figureMultiplier = how many times the number of figures should we draw
		final int relativeScale;
		final int relativeScaleMultiplier;
		int figureMultiplier;
		final int unitImageMultiplier;
		
		switch (getClientConfig ().getUnitCombatScale ())
		{
			case DOUBLE_SIZE_UNITS:
				relativeScale = 1;
				relativeScaleMultiplier = 2;
				figureMultiplier = 1;
				unitImageMultiplier = 2;
				break;
				
			case FOUR_TIMES_FIGURES:
				relativeScale = 2;
				relativeScaleMultiplier = 1;
				figureMultiplier = 4;
				unitImageMultiplier = 1;
				break;
				
			case FOUR_TIMES_FIGURES_EXCEPT_SINGLE_SUMMONED:
				relativeScale = 2;
				relativeScaleMultiplier = 1;
				figureMultiplier = ((totalFigureCount == 1) && (CommonDatabaseConstants.VALUE_UNIT_TYPE_ID_SUMMONED.equals (unitTypeID))) ? 1 : 4;
				unitImageMultiplier = ((totalFigureCount == 1) && (CommonDatabaseConstants.VALUE_UNIT_TYPE_ID_SUMMONED.equals (unitTypeID))) ? 2 : 1;
				break;
				
			default:
				throw new MomException ("drawUnitFigures encountered a scale that it doesn't know how to handle: " + getClientConfig ().getUnitCombatScale ());
		}
		
		// Show heroes with entourage of cavalry accompanying them
		BufferedImage secondaryImage = null;
		if ((figureMultiplier == 4) && (unit.getSecondaryUnitID () != null))
		{
			figureMultiplier++;
			final UnitEx secondaryUnit = getGraphicsDB ().findUnit (unit.getSecondaryUnitID (), "drawUnitFigures");
			final UnitCombatImage secondaryUnitImage = secondaryUnit.findCombatAction (combatActionID, "drawUnitFigures").findDirection (direction, "drawUnitFigures");
			secondaryImage = getAnim ().loadImageOrAnimationFrame (secondaryUnitImage.getUnitCombatImageFile (), secondaryUnitImage.getUnitCombatAnimation ());
		}
		
		// Work out the image to draw n times
		final UnitCombatImage unitImage = unit.findCombatAction (combatActionID, "drawUnitFigures").findDirection (direction, "drawUnitFigures");
		final BufferedImage image = getAnim ().loadImageOrAnimationFrame (unitImage.getUnitCombatImageFile (), unitImage.getUnitCombatAnimation ());
		final int imageWidth = image.getWidth () * unitImageMultiplier;
		final int imageHeight = image.getHeight () * unitImageMultiplier;
		
		// Get the positions of the n times
		final CombatTileFigurePositionsEx positions = getGraphicsDB ().findCombatTileUnitRelativeScale (relativeScale, "drawUnitFigures").findFigureCount (aliveFigureCount * figureMultiplier, "drawUnitFigures");
		for (int n = 1; n <= (aliveFigureCount * figureMultiplier); n++)
		{
			final FigurePositionsForFigureCount position = positions.findFigureNumber (n, "drawUnitFigures");

			// This is to account that the unit's feet don't touch the bottom of the image
			final int fudgeY = (totalFigureCount <= 2) ? 4 : 6;
			
			// Select image to draw
			final BufferedImage useImage;
			if (secondaryImage == null)
				useImage = image;
			else
				useImage = (n == 3) ? image : secondaryImage;
			
			// TileRelativeX, Y in the graphics XML indicates the position of the unit's feet, so need to adjust according to the unit size
			g.drawImage (useImage, 
				offsetX + (position.getTileRelativeX () * relativeScaleMultiplier) - (imageWidth / 2),
				offsetY + (position.getTileRelativeY () * relativeScaleMultiplier) + (fudgeY * unitImageMultiplier) - imageHeight,
				image.getWidth () * unitImageMultiplier, image.getHeight () * unitImageMultiplier, null);
		}
	}

	/**
	 * Version which derives most of the values from an existing unit object.
	 * 
	 * @param unit The unit to draw
	 * @param combatActionID The action to show the unit doing
	 * @param direction The direction to show the unit facing
	 * @param g The graphics context to draw the unit onto
	 * @param offsetX The x offset into the graphics context to draw the unit at
	 * @param offsetY The y offset into the graphics context to draw the unit at
	 * @param drawSampleTile Whether to draw a sample tile (grass or ocean) under this unit
	 * @throws IOException If there is a problem
	 */
	@Override
	public final void drawUnitFigures (final AvailableUnit unit, final String combatActionID,
		final int direction, final Graphics g, final int offsetX, final int offsetY, final boolean drawSampleTile) throws IOException
	{
		// Get total figures
		final Unit unitDef = getClient ().getClientDB ().findUnit (unit.getUnitID (), "drawUnitFigures");
		final int totalFigureCount = getUnitUtils ().getFullFigureCount (unitDef);
		
		// Get alive figures
		final int aliveFigureCount;
		if (unit instanceof MemoryUnit)
		{
			aliveFigureCount = getUnitCalculations ().calculateAliveFigureCount ((MemoryUnit) unit, getClient ().getPlayers (),
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (),
				getClient ().getClientDB ());
		}
		else
			aliveFigureCount = totalFigureCount;
		
		// Get unit type
		final String unitMagicRealmID = getClient ().getClientDB ().findUnit (unit.getUnitID (), "drawUnitFigures").getUnitMagicRealm ();
		final String unitTypeID = getClient ().getClientDB ().findUnitMagicRealm (unitMagicRealmID, "drawUnitFigures").getUnitTypeID ();
		
		// Get sample tile
		final String sampleTileImageFile;
		if (drawSampleTile)
			sampleTileImageFile = getClientUnitCalculations ().findPreferredMovementSkillGraphics (unit).getSampleTileImageFile ();
		else
			sampleTileImageFile = null; 
		
		// Call other version now that we have all the necessary values
		drawUnitFigures (unit.getUnitID (), unitTypeID, totalFigureCount, aliveFigureCount, combatActionID,
			direction, g, offsetX, offsetY, sampleTileImageFile);
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
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param util Unit utils
	 */
	public final void setUnitUtils (final UnitUtils util)
	{
		unitUtils = util;
	}

	/**
	 * @return Unit calculations
	 */
	public final MomUnitCalculations getUnitCalculations ()
	{
		return unitCalculations;
	}

	/**
	 * @param calc Unit calculations
	 */
	public final void setUnitCalculations (final MomUnitCalculations calc)
	{
		unitCalculations = calc;
	}

	/**
	 * @return Client unit calculations
	 */
	public final MomClientUnitCalculations getClientUnitCalculations ()
	{
		return clientUnitCalculations;
	}

	/**
	 * @param calc Client unit calculations
	 */
	public final void setClientUnitCalculations (final MomClientUnitCalculations calc)
	{
		clientUnitCalculations = calc;
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
	 * @return Client config, containing the scale setting
	 */
	public final MomImeClientConfig getClientConfig ()
	{
		return clientConfig;
	}

	/**
	 * @param config Client config, containing the scale setting
	 */
	public final void setClientConfig (final MomImeClientConfig config)
	{
		clientConfig = config;
	}

	/**
	 * @return Overland map right hand panel showing economy etc
	 */
	public final OverlandMapRightHandPanel getOverlandMapRightHandPanel ()
	{
		return overlandMapRightHandPanel;
	}

	/**
	 * @param panel Overland map right hand panel showing economy etc
	 */
	public final void setOverlandMapRightHandPanel (final OverlandMapRightHandPanel panel)
	{
		overlandMapRightHandPanel = panel;
	}

	/**
	 * @return Turn sequence and movement helper methods
	 */
	public final OverlandMapProcessing getOverlandMapProcessing ()
	{
		return overlandMapProcessing;
	}

	/**
	 * @param proc Turn sequence and movement helper methods
	 */
	public final void setOverlandMapProcessing (final OverlandMapProcessing proc)
	{
		overlandMapProcessing = proc;
	}

	/**
	 * @return Pending movement utils
	 */
	public final PendingMovementUtils getPendingMovementUtils ()
	{
		return pendingMovementUtils;
	}

	/**
	 * @param util Pending movement utils
	 */
	public final void setPendingMovementUtils (final PendingMovementUtils util)
	{
		pendingMovementUtils = util;
	}
}