package momime.client.utils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.swing.JComponent;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.ObjectUtils;

import com.ndg.utils.swing.NdgUIUtils;
import com.ndg.utils.swing.zorder.ZOrderGraphics;

import jakarta.xml.bind.JAXBException;
import momime.client.MomClient;
import momime.client.audio.MomAudioPlayer;
import momime.client.graphics.AnimationContainer;
import momime.client.graphics.database.CombatTileFigurePositionsGfx;
import momime.client.graphics.database.FigurePositionsForFigureCount;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.process.CombatMapProcessing;
import momime.client.process.OverlandMapProcessing;
import momime.client.ui.PlayerColourImageGenerator;
import com.ndg.utils.swing.components.HideableComponent;
import momime.client.ui.components.SelectUnitButton;
import momime.client.ui.frames.CityViewUI;
import momime.client.ui.frames.CombatUI;
import momime.client.ui.frames.HeroItemsUI;
import momime.client.ui.frames.UnitInfoUI;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.common.calculations.UnitCalculations;
import momime.common.database.AnimationFrame;
import momime.common.database.CombatAction;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.database.LanguageText;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitCombatActionEx;
import momime.common.database.UnitCombatImage;
import momime.common.database.UnitEx;
import momime.common.database.UnitShadowOffset;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillEx;
import momime.common.database.UnitSkillPositiveNegative;
import momime.common.messages.AvailableUnit;
import momime.common.messages.MemoryUnit;
import momime.common.messages.UnitStatusID;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.PendingMovementUtils;
import momime.common.utils.UnitUtils;

/**
 * Client side only helper methods for dealing with units
 */
public final class UnitClientUtilsImpl implements UnitClientUtils
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (UnitClientUtilsImpl.class);

	/** Attribute icons leave a gap every so often to make them easier to count */
	private final static int ATTRIBUTE_ICONS_PER_GROUP = 5;
	
	/** How many groups make up one row */
	private final static int ATTRIBUTE_ICON_GROUPS_PER_ROW = 4;

	/** How many attribute icons per row */
	private final static int ATTRIBUTE_ICONS_PER_ROW = ATTRIBUTE_ICONS_PER_GROUP * ATTRIBUTE_ICON_GROUPS_PER_ROW;
	
	/** Darkening colour drawn over the top of attributes that are being reduced by a negative effect, e.g. Black Prayer */
	private final static Color COLOUR_NEGATIVE_ATTRIBUTES = new Color (0, 0, 0, 0xA0);
	
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
	private UnitCalculations unitCalculations;
	
	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** Overland map right hand panel showing economy etc */
	private OverlandMapRightHandPanel overlandMapRightHandPanel;
	
	/** Turn sequence and movement helper methods */
	private OverlandMapProcessing overlandMapProcessing;

	/** Combat map processing */
	private CombatMapProcessing combatMapProcessing;
	
	/** Pending movement utils */
	private PendingMovementUtils pendingMovementUtils;
	
	/** Sound effects player */
	private MomAudioPlayer soundPlayer;
	
	/** Combat UI */
	private CombatUI combatUI;

	/** Hero items UI */
	private HeroItemsUI heroItemsUI;
	
	/** Player colour image generator */
	private PlayerColourImageGenerator playerColourImageGenerator;

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
		final UnitEx unitDef = getClient ().getClientDB ().findUnit (unit.getUnitID (), "getUnitName");
		
		// Heroes just output their name for all unitNameTypes, so in that case we don't need to look up anything at all
		String unitName = null;
		if (unit instanceof MemoryUnit)
		{
			final MemoryUnit mu = (MemoryUnit) unit;
			if (mu.getUnitName () != null)
				unitName = mu.getUnitName ();
			else if (mu.getHeroNameID () != null)
				unitName = getLanguageHolder ().findDescription (unitDef.findHeroName (mu.getHeroNameID (), "getUnitName").getHeroNameLang ());
		}
		
		if (unitName == null)
		{
			// Find the name in the language XML
			unitName = getLanguageHolder ().findDescription (unitDef.getUnitName ());
			
			// If we just want the simple name with no race prefix, then we're done
			if (unitNameType != UnitNameType.SIMPLE_UNIT_NAME)
			{
				// Need the record from the server XML to know whether to put the racial prefix on or not
				if ((unitDef.isIncludeRaceInUnitName () != null) && (unitDef.isIncludeRaceInUnitName ()))
					unitName = getLanguageHolder ().findDescription
						(getClient ().getClientDB ().findRace (unitDef.getUnitRaceID (), "getUnitName").getRaceNameSingular ()) + " " + unitName;
				
				// How we modify this now depends on the requested unitNameType
				switch (unitNameType)
				{
					case A_UNIT_NAME:
					{
						final String prefix = getLanguageHolder ().findDescription (unitDef.getUnitNamePrefix ());
						if (!ObjectUtils.isEmpty (prefix))
							unitName = prefix + " " + unitName;
						break;
					}
						
					case THE_UNIT_OF_NAME:
					{
						final String prefix = getLanguageHolder ().findDescription (unitDef.getUnitNamePrefix ());
						final List<LanguageText> languageText = ObjectUtils.isEmpty (prefix) ? getLanguages ().getUnitName ().getTheUnitOfNamePlural () : getLanguages ().getUnitName ().getTheUnitOfNameSingular ();
						
						unitName = getLanguageHolder ().findDescription (languageText).replaceAll ("RACE_UNIT_NAME", unitName);
						break;
					}

					case A_UNIT_OF_NAME:
					{
						final String prefix = getLanguageHolder ().findDescription (unitDef.getUnitNamePrefix ());
						final List<LanguageText> languageText = ObjectUtils.isEmpty (prefix) ? getLanguages ().getUnitName ().getAUnitOfNamePlural () : getLanguages ().getUnitName ().getAUnitOfNameSingular ();
						
						unitName = getLanguageHolder ().findDescription (languageText).replaceAll ("RACE_UNIT_NAME", unitName);
						break;
					}
					
					case UNITS_OF_NAME:
					{
						final String prefix = getLanguageHolder ().findDescription (unitDef.getUnitNamePrefix ());
						final List<LanguageText> languageText = ObjectUtils.isEmpty (prefix) ? getLanguages ().getUnitName ().getUnitsOfNamePlural () : getLanguages ().getUnitName ().getUnitsOfNameSingular ();
						
						unitName = getLanguageHolder ().findDescription (languageText).replaceAll ("RACE_UNIT_NAME", unitName);
						break;
					}
					
					// Other name types are handled elsewhere or require nothing to be added
					default:
						break;
				}
			}
		}
		
		return unitName;
	}
	
	/**
	 * Finds the right icon for skills displayed in the top half of the unit info screen, where we display one icon per each "point"
	 * of the skill, so e.g. Melee 5 displays 5 swords.  The image returned here is transparent, so we can superimpose it over a
	 * background showing which component provided that value, so you can tell the difference between e.g. a unit's base
	 * stat and the bonus being granted from experience.  These were previously referred to as unit attributes.
	 * 
	 * Rules for finding the right icon for these aren't totally straightforward; ranged attacks have their own images and some
	 * unit attributes (and some RATs) have different icons for different weapon grades and some do not.  So this method deals with all that.
	 * 
	 * @param unit Unit whose skills we're drawing
	 * @param unitSkillID Which skill to draw
	 * @return Icon for this unit skill, or null if there isn't one
	 * @throws IOException If there's a problem finding the unit skill icon
	 */
	@Override
	public final BufferedImage getUnitSkillComponentBreakdownIcon (final ExpandedUnitDetails unit, final String unitSkillID) throws IOException
	{
		final String skillImageName;
		if (unitSkillID.equals (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK))
		{
			// Ranged attacks have their own special rules, so we select the appropriate
			// type of range attack icon, e.g. bow, rock, blue blast.
			if (unit.getRangedAttackType () == null)
				skillImageName = null;
			else
			{
				// If there is only a single image then just use it; if there are multiple, then select the right one by weapon grade
				if ((unit.getWeaponGrade () == null) || (unit.getRangedAttackType ().getRangedAttackTypeWeaponGrade ().size () == 1))
					skillImageName = unit.getRangedAttackType ().getRangedAttackTypeWeaponGrade ().get (0).getUnitDisplayRangedImageFile ();
				else
					skillImageName = unit.getRangedAttackType ().findWeaponGradeImageFile (unit.getWeaponGrade ().getWeaponGradeNumber (), "getUnitSkillComponentBreakdownIcon");
			}
		}
		else if (unitSkillID.equals (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED))
		{
			// Movement has its own special rules, so we show a boot or wings or sailing icon
			skillImageName = getUnitCalculations ().findPreferredMovementSkillGraphics (unit, getClient ().getClientDB ()).getMovementIconImageFile ();
		}
		else
		{
			// Some skill other than ranged attack; same behaviour as above with weapon grades
			final UnitSkillEx skillGfx = getClient ().getClientDB ().findUnitSkill (unitSkillID, "getUnitSkillComponentBreakdownIcon");
			if ((unit.getWeaponGrade () == null) || (skillGfx.getUnitSkillWeaponGrade ().size () == 1))
				skillImageName = skillGfx.getUnitSkillWeaponGrade ().get (0).getSkillImageFile ();
			else
				skillImageName = skillGfx.findWeaponGradeImageFile (unit.getWeaponGrade ().getWeaponGradeNumber (), "getUnitSkillComponentBreakdownIcon");
		}
		
		final BufferedImage skillImage = (skillImageName == null) ? null : getUtils ().loadImage (skillImageName);
		return skillImage;
	}
	
	/**
	 * Finds the right icon for skills displayed in the bottom half of the unit info screen, where we just display a single icon for
	 * the skill no matter what its numeric value is, or if it has no value.  So the image returned here already includes a background.
	 * These were previously referred to as unit skills.
	 * 
	 * Rules for finding the right icon for these aren't totally straightforward; experience icon changes as units
	 * gain level, and some skills (particularly movement type skills like walking/flying) have no icon at all.  So this method deals with all that.
	 * 
	 * @param unit Unit whose skills we're drawing
	 * @param unitSkillID Which skill to draw
	 * @return Icon for this unit skill, or null if there isn't one
	 * @throws IOException If there's a problem finding the unit skill icon
	 */
	@Override
	public final BufferedImage getUnitSkillSingleIcon (final ExpandedUnitDetails unit, final String unitSkillID) throws IOException
	{
		final String image;
		if (unitSkillID.equals (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE))
		{
			// Experience skill icon changes as the unit gains experience levels
			final ExperienceLevel expLvl = unit.getModifiedExperienceLevel ();
			if (expLvl == null)
				image = null;
			else
				image = expLvl.getExperienceLevelImageFile ();
		}
		else
		{
			// Regular skill
			final UnitSkillEx skillGfx = getClient ().getClientDB ().findUnitSkill (unitSkillID, "getUnitSkillSingleIcon");
			image = skillGfx.getUnitSkillImageFile ();
		}

		final BufferedImage skillImage = (image == null) ? null : getUtils ().loadImage (image);
		return skillImage;
	}

	/**
	 * Kills a unit, either permanently removing it or marking it as dead in case it gets Raise or Animate Dead cast on it later
	 * 
	 * @param unit Unit to kill
	 * @param newStatus The new status to set the unit to, e.g. DEAD or KILLED_BY_LACK_OF_PRODUCTION; a null here means remove the unit entirely
	 * @throws IOException If there is a problem
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final void killUnit (final MemoryUnit unit, final UnitStatusID newStatus) throws IOException, JAXBException, XMLStreamException
	{
		// Even if not actually freeing the unit, we still need to eliminate all references to it, except for it being in the main unit list
		getPendingMovementUtils ().removeUnitFromAnyPendingMoves (getClient ().getOurPersistentPlayerPrivateKnowledge ().getPendingMovement (), unit.getUnitURN ());
		
		if ((unit.getCombatLocation () != null) && (unit.getCombatLocation ().equals (getCombatUI ().getCombatLocation ())))
			getCombatUI ().clearUnitToDrawFromLocation (unit.getCombatPosition ().getX (), unit.getCombatPosition ().getY (), unit.getUnitID ());
		
		// Is there a unit info screen open for it?
		final UnitInfoUI unitInfo = getClient ().getUnitInfos ().get (unit.getUnitURN ());
		if (unitInfo != null)
			unitInfo.close ();

		// Remove from movement lists
		getOverlandMapProcessing ().removeUnitFromLeftToMoveOverland (unit);
		getCombatMapProcessing ().removeUnitFromLeftToMoveCombat (unit);
		
		// Select unit buttons on the Map
		for (final HideableComponent<SelectUnitButton> button : getOverlandMapRightHandPanel ().getSelectUnitButtons ())
			if ((!button.isHidden ()) && (button.getComponent ().getUnit () != null) && (button.getComponent ().getUnit ().getMemoryUnit () == unit))
			{
				button.getComponent ().setUnit (null);
				
				final boolean updateMovement = (!button.isHidden ()) && (button.getComponent ().isSelected ());
				button.getComponent ().setSelected (false);
				button.setHidden (true);
					
				if (updateMovement)
				{
					// Do same processing as if button was manually clicked
					getOverlandMapProcessing ().enableOrDisableSpecialOrderButtons ();
					getOverlandMapProcessing ().updateMovementRemaining ();
				}
			}
		
		// The server works out what action we need to take
		if (newStatus == null)
			getUnitUtils ().removeUnitURN (unit.getUnitURN (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ());
		else
			unit.setStatus (newStatus);

		// Select unit buttons on the City screen
		if (unit.getUnitLocation () != null)
		{
			final CityViewUI cityView = getClient ().getCityViews ().get (unit.getUnitLocation ().toString ());
			if (cityView != null)
				cityView.unitsChanged ();
		}
		
		// Update various UI screens if our units get killed
		if (unit.getOwningPlayerID () == getClient ().getOurPlayerID ())
		{
			if (getClient ().getClientDB ().findUnit (unit.getUnitID (), "killUnit").getUnitMagicRealm ().equals
				(CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
					
				getHeroItemsUI ().refreshHeroes ();
			
			// If it was our unit dying in combat, jump to next unit to move
			getCombatMapProcessing ().selectNextUnitToMoveCombat ();
		}
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
		final UnitEx unit = getClient ().getClientDB ().findUnit (unitID, "registerUnitFiguresAnimation");
		final UnitCombatImage unitImage = unit.findCombatAction (combatActionID, "registerUnitFiguresAnimation").findDirection (direction, "registerUnitFiguresAnimation");
		getAnim ().registerRepaintTrigger (unitImage.getUnitCombatAnimation (), component, AnimationContainer.COMMON_XML);
	}

	/**
	 * Unregisters unit animations started by registerUnitFiguresAnimation.  Note animation timers aren't reference counted, so to call this you must be
	 * certain that the animation is no longer being used.  e.g. If two flying units are facing direction 1, and one of them turns to face direction 2, we have to
	 * additionally register the 'flying d2' anim but can't unregister the 'flying d1' anim since another unit is still using it.
	 * 
	 * @param unitID The unit to draw
	 * @param combatActionID The action to show the unit doing
	 * @param direction The direction to show the unit facing
	 * @param component The component that the unit will be drawn onto
	 * @throws IOException If there is a problem
	 */
	@Override
	public final void unregisterUnitFiguresAnimation (final String unitID, final String combatActionID, final int direction, final JComponent component) throws IOException
	{
		final UnitEx unit = getClient ().getClientDB ().findUnit (unitID, "unregisterUnitFiguresAnimation");
		final UnitCombatImage unitImage = unit.findCombatAction (combatActionID, "unregisterUnitFiguresAnimation").findDirection (direction, "unregisterUnitFiguresAnimation");
		getAnim ().unregisterRepaintTrigger (unitImage.getUnitCombatAnimation (), component);
	}

	/**
	 * Calculates the positions of all the figures of a unit, taking into account combat scale and so on.
	 * This is used both to draw the figures of a unit, and to get the positions to start ranged attack missiles at when the unit fires.
	 * NB. The resulting array may not have aliveFigureCount elements; it may be x1, x4 or x5 depending on combat scale.
	 * The calling routine should just iterate over the resulting array and make no assumptions about how many elements will contain. 
	 * 
	 * @param totalFigureCount The number of figures the unit had when fully healed
	 * @param aliveFigureCount The number of figures the unit has now
	 * @param offsetX The x offset into the graphics context to draw the unit at
	 * @param offsetY The y offset into the graphics context to draw the unit at
	 * @return Array of all figure positions in pixels; see CALC_UNIT_FIGURE_POSITIONS_COLUMN_ constants for what's stored in each column of the array
	 * @throws IOException If there is a problem
	 */
	@Override
	public final int [] [] calcUnitFigurePositions (final int totalFigureCount, final int aliveFigureCount, final int offsetX, final int offsetY) throws IOException
	{
		// This is to account that the unit's feet don't touch the bottom of the image
		final int fudgeY = (totalFigureCount <= 2) ? 4 : 6;
		
		// Get the positions of the n times
		final int [] [] result = new int [aliveFigureCount] [CALC_UNIT_FIGURE_POSITIONS_COLUMN_Y_INCL_OFFSET+1];
		final CombatTileFigurePositionsGfx positions = getGraphicsDB ().findFigureCount (totalFigureCount, "calcUnitFigurePositions");
		for (int n = 0; n < aliveFigureCount; n++)
		{
			final FigurePositionsForFigureCount position = positions.findFigureNumber (n+1, "calcUnitFigurePositions");

			// Generate coordinates
			result [n] [CALC_UNIT_FIGURE_POSITIONS_COLUMN_X_EXCL_OFFSET] = (position.getTileRelativeX () * 2);
			result [n] [CALC_UNIT_FIGURE_POSITIONS_COLUMN_Y_EXCL_OFFSET] = (position.getTileRelativeY () * 2) + (fudgeY * 2);
			result [n] [CALC_UNIT_FIGURE_POSITIONS_COLUMN_X_INCL_OFFSET] = offsetX + result [n] [CALC_UNIT_FIGURE_POSITIONS_COLUMN_X_EXCL_OFFSET];
			result [n] [CALC_UNIT_FIGURE_POSITIONS_COLUMN_Y_INCL_OFFSET] = offsetY + result [n] [CALC_UNIT_FIGURE_POSITIONS_COLUMN_Y_EXCL_OFFSET];
		}
		
		return result;
	}
	
	/**
	 * Draws the figures of a unit.
	 * NB. This has to work without relying on the AvailableUnit so that we can draw units on the Options screen before joining a game.
	 * 
	 * @param unitID The unit to draw
	 * @param playerID The owner of the unit
	 * @param totalFigureCount The number of figures the unit had when fully healed
	 * @param aliveFigureCount The number of figures the unit has now
	 * @param combatActionID The action to show the unit doing
	 * @param direction The direction to show the unit facing
	 * @param g The graphics context to draw the unit onto
	 * @param offsetX The x offset into the graphics context to draw the unit at
	 * @param offsetY The y offset into the graphics context to draw the unit at
	 * @param sampleTileImageFile The filename of the sample tile (grass or ocean) to draw under this unit; if null, then no sample tile will be drawn
	 * @param registeredAnimation Determines frame number: True=by Swing timer, must have previously called registerRepaintTrigger; False=by System.nanoTime ()
	 * @param baseZOrder Z order for the top of the tile
	 * @param shadingColours List of shading colours to apply to the image
	 * @param mergingRatio How much "dug into the ground" the unit should appear; null/0 means draw normally, 1 will draw nothing at all
	 * @param newShadows Whether new shadows option is switched on in client config
	 * @param newShadowsUnitAdjustY Adjusts Y position a unit is drawn at if it is flying (its shadow stays in the same place)
	 * @param drawShadows Whether to draw shadows or not (this only has an effect if newShadows is switched on)
	 * @throws IOException If there is a problem
	 */
	@Override
	public final void drawUnitFigures (final String unitID, final int playerID, final int totalFigureCount, final int aliveFigureCount, final String combatActionID,
		final int direction, final ZOrderGraphics g, final int offsetX, final int offsetY, final String sampleTileImageFile, final boolean registeredAnimation,
		final int baseZOrder, final List<String> shadingColours, final Double mergingRatio, final boolean newShadows, final int newShadowsUnitAdjustY, final boolean drawShadows) throws IOException
	{
		// Draw sample tile
		if (sampleTileImageFile != null)
		{
			final BufferedImage tileImage = getUtils ().loadImage (sampleTileImageFile);
			g.drawStretchedImage (tileImage, offsetX, offsetY, tileImage.getWidth () * 2, tileImage.getHeight () * 2, baseZOrder);
		}
		
		// Get the main unit
		final UnitEx unit = getClient ().getClientDB ().findUnit (unitID, "drawUnitFigures");
		
		// Get all the figure positions
		final int [] [] figurePositions = calcUnitFigurePositions (totalFigureCount, aliveFigureCount, offsetX, offsetY);
		
		// Work out the image to draw n times
		final UnitCombatImage unitImage = unit.findCombatAction (combatActionID, "drawUnitFigures").findDirection (direction, "drawUnitFigures");
		final AnimationFrame frame = getAnim ().getUnitCombatImageFrame (unitImage, registeredAnimation, AnimationContainer.COMMON_XML);
		final String imageName = ((newShadows) && (frame.getShadowlessImageFile () != null)) ? frame.getShadowlessImageFile () : frame.getImageFile (); 
		final Image image = getPlayerColourImageGenerator ().getModifiedImage (imageName, false, frame.getImageFlag (),
			frame.getFlagOffsetX (), frame.getFlagOffsetY (), playerID, shadingColours, null);
		
		final Image shadowImage;
		if ((newShadows) && (drawShadows) && (frame.getShadowImageFile () != null))
			shadowImage = getPlayerColourImageGenerator ().getModifiedImage (frame.getShadowImageFile (), false, null, null, null, null, shadingColours, null);
		else
			shadowImage = null;
		
		// This needs to be done properly with a map, rather than assuming the directions are listed in the correct order
		final UnitShadowOffset shadowOffset = (shadowImage != null) ? unit.getUnitShadowOffset ().get (direction - 1) : null;
		
		// Draw the figure in each position
		for (final int [] position : figurePositions)
		{
			// Last array element tells us what to multiply the image size up by; shadows are multiplied in X direction but not y
			final int doubleWidth = image.getWidth (null) * 2;
			final int doubleHeight = image.getHeight (null) * 2;
			final int figureX = position [CALC_UNIT_FIGURE_POSITIONS_COLUMN_X_INCL_OFFSET] - (doubleWidth / 2);
			int figureY = position [CALC_UNIT_FIGURE_POSITIONS_COLUMN_Y_INCL_OFFSET] - doubleHeight;
			
			if ((newShadows) && (unit.getNewShadowsUnitOffsetY () != null))
				figureY = figureY + unit.getNewShadowsUnitOffsetY ();
			
			// TileRelativeX, Y in the graphics XML indicates the position of the unit's feet, so need to adjust according to the unit size
			if ((mergingRatio == null) || (mergingRatio == 0d))
			{
				if (shadowImage != null)
					g.drawStretchedImage (shadowImage, figureX + shadowOffset.getShadowOffsetX (), figureY + shadowOffset.getShadowOffsetY (),
						doubleWidth, shadowImage.getHeight (null), 0);

				if (newShadows)
					figureY = figureY + newShadowsUnitAdjustY;
				
				g.drawStretchedImage (image, figureX, figureY, doubleWidth, doubleHeight,
					baseZOrder + 2 + position [CALC_UNIT_FIGURE_POSITIONS_COLUMN_Y_EXCL_OFFSET]);
			}
			else
			{
				if (shadowImage != null)
					g.drawStretchedClippedImage (shadowImage,
						0, 0, shadowImage.getWidth (null), (int) (shadowImage.getHeight (null) * (1d - mergingRatio)),
						figureX + shadowOffset.getShadowOffsetX (), figureY  + shadowOffset.getShadowOffsetY () + ((int) (shadowImage.getHeight (null) * mergingRatio)),
						doubleWidth, (int) (shadowImage.getHeight (null) * (1d - mergingRatio)), 0);

				if (newShadows)
					figureY = figureY + newShadowsUnitAdjustY;
				
				g.drawStretchedClippedImage (image,
					0, 0, image.getWidth (null), (int) (image.getHeight (null) * (1d - mergingRatio)),
					figureX, figureY + ((int) (doubleHeight * mergingRatio)),
					doubleWidth, (int) (doubleHeight * (1d - mergingRatio)),
					baseZOrder + 2 + position [CALC_UNIT_FIGURE_POSITIONS_COLUMN_Y_EXCL_OFFSET]);
			}
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
	 * @param registeredAnimation Determines frame number: True=by Swing timer, must have previously called registerRepaintTrigger; False=by System.nanoTime ()
	 * @param baseZOrder Z order for the top of the tile
	 * @param shadingColours List of shading colours to apply to the image
	 * @param mergingRatio How much "dug into the ground" the unit should appear; null/0 means draw normally, 1 will draw nothing at all
	 * @param newShadows Whether new shadows option is switched on in client config
	 * @param drawShadows Whether to draw shadows or not (this only has an effect if newShadows is switched on)
	 * @throws IOException If there is a problem
	 */
	@Override
	public final void drawUnitFigures (final ExpandedUnitDetails unit, final String combatActionID, final int direction, final ZOrderGraphics g,
		final int offsetX, final int offsetY, final boolean drawSampleTile, final boolean registeredAnimation, final int baseZOrder,
		final List<String> shadingColours, final Double mergingRatio, final boolean newShadows, final boolean drawShadows) throws IOException
	{
		// Get alive figures
		int aliveFigureCount = unit.calculateAliveFigureCount ();
		
		if (aliveFigureCount > 0)
		{
			// Get sample tile
			final String sampleTileImageFile;
			if (drawSampleTile)
				sampleTileImageFile = getUnitCalculations ().findPreferredMovementSkillGraphics (unit, getClient ().getClientDB ()).getSampleTileImageFile ();
			else
				sampleTileImageFile = null; 

			// Is it flying?  Can't rely on the combatActionID being passed in as that might say MELEE or RANGED when making an attack, but we still need to know if its flying
			final String standingCombatActionID = getUnitCalculations ().determineCombatActionID (unit, false, getClient ().getClientDB ());
			final CombatAction combatAction = getClient ().getClientDB ().findCombatAction (standingCombatActionID, "drawUnitFigures");
			final int newShadowsUnitAdjustY = (combatAction.getNewShadowsUnitAdjustY () == null) ? 0 : combatAction.getNewShadowsUnitAdjustY ();
			
			// Call other version now that we have all the necessary values
			int fullFigureCount = unit.getFullFigureCount ();
			
			if (unit.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_DRAW_ONE_FIGURE))
			{
				fullFigureCount = 1;
				if (aliveFigureCount > 1)
					aliveFigureCount = 1;
			}			
			
			drawUnitFigures (unit.getUnitID (), unit.getOwningPlayerID (), fullFigureCount, aliveFigureCount, combatActionID,
				direction, g, offsetX, offsetY, sampleTileImageFile, registeredAnimation, baseZOrder, shadingColours, mergingRatio, newShadows, newShadowsUnitAdjustY, drawShadows);
		}
	}
	
	/**
	 * Plays the sound effect for a particular unit taking a particular action.  This covers all combat actions, so the clank clank of units walking,
	 * the chop chop of melee attacks, the pew or whoosh of ranged attacks, and so on.
	 * 
	 * @param unit The unit making an action
	 * @param combatActionID The type of action being performed
	 * @throws RecordNotFoundException If the unit or its action can't be found in the graphics XML
	 */
	@Override
	public final void playCombatActionSound (final AvailableUnit unit, final String combatActionID) throws RecordNotFoundException
	{
		if (unit != null)
		{
			// See if there's a specific override sound specified for this unit.
			// This so earth elementals make a stomp stomp noise, cavalry go clippity clop and regular swordsmen go clank clank, even though they're all just doing the WALK action.
			final String soundEffectFilename;
			final UnitEx unitDef = getClient ().getClientDB ().findUnit (unit.getUnitID (), "playCombatActionSound");
			final UnitCombatActionEx unitCombatAction = unitDef.findCombatAction (combatActionID, "playCombatActionSound");
			if (unitCombatAction.getOverrideActionSoundFile () != null)
				soundEffectFilename = unitCombatAction.getOverrideActionSoundFile ();
			else
			{
				// If we didn't find a specific sound for this unit, use the general one
				soundEffectFilename = getClient ().getClientDB ().findCombatAction (combatActionID, "playCombatActionSound").getDefaultActionSoundFile ();
				if (soundEffectFilename == null)
					log.warn ("Found combatAction " + combatActionID + " in the graphics XML, but it doesn't specify a defaultActionSoundFile");
			}
			
			if (soundEffectFilename != null)
				try
				{
					getSoundPlayer ().playAudioFile (soundEffectFilename);
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
		}
	}
	
	/**
	 * @param unit Unit to generate attribute icons for
	 * @param unitSkillID Skill ID to generate attribute icons for
	 * @return Combined image showing icons for this unit attribute, with appropriate background colours; or null if the unit doesn't have this skill or it is value-less
	 * @throws IOException If there is a problem loading any of the images
	 */
	@Override
	public final BufferedImage generateAttributeImage (final ExpandedUnitDetails unit, final String unitSkillID) throws IOException
	{
		// If the unit doesn't even have the skill, then just return a null image.
		// Also if it is a value-less skill like a movement skill then there's nothing to draw.
		// If they DID have the skill but something is zeroing it out, then DO display the greyed out icons
		final BufferedImage image;
		if ((!unit.hasModifiedSkill (unitSkillID)) || (unit.getModifiedSkillValue (unitSkillID) == null))
			image = null;
		else
		{
			// Create all images the same size, since it makes laying them out in the list simpler
			// But we still need to know the size of an image to base this off
			final BufferedImage basicComponentBackgroundImage = getUtils ().loadImage
				(getGraphicsDB ().findUnitSkillComponent (UnitSkillComponent.BASIC, "generateAttributeImage").getUnitSkillComponentImageFile ()); 

			image = new BufferedImage
				((basicComponentBackgroundImage.getWidth () * ATTRIBUTE_ICONS_PER_ROW) + ATTRIBUTE_ICONS_PER_ROW - 1 +		// Space for icons themselves + 1 pixel gap between each
					((ATTRIBUTE_ICON_GROUPS_PER_ROW - 1) * 2) +																										// Additional 2 pixel gap between each block of 5
					4,																																													// Indent 2nd row by 4 pixels
				basicComponentBackgroundImage.getHeight () + 3, BufferedImage.TYPE_INT_ARGB);
					
			// Work out the icon to use to display this type of unit attribute
			final BufferedImage attributeImage = getUnitSkillComponentBreakdownIcon (unit, unitSkillID);

			// Do we need to draw any icons faded, due to negative spells (e.g. Black Prayer) or losing hitpoints?
			final int attributeValueIncludingNegatives;
			if (unitSkillID.equals (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS))
				attributeValueIncludingNegatives = unit.calculateHitPointsRemainingOfFirstFigure ();
			else
				attributeValueIncludingNegatives = unit.getModifiedSkillValue (unitSkillID);
			
			final Graphics2D g = image.createGraphics ();
			try
			{
				// Calculate and draw each component separately
				int drawnAttributeCount = 0;
				for (final UnitSkillComponent attrComponent : UnitSkillComponent.values ())
					if (attrComponent != UnitSkillComponent.ALL)
					{
						// Work out the total value (without negative effects), and our actual current value (after negative effects),
						// so we can show stats knocked off by e.g. Black Prayer as faded.
						// Simiarly we fade icons for hit points/hearts lost due to damage we've taken.
						final int totalValue = unit.filterModifiedSkillValue (unitSkillID, attrComponent,
							unitSkillID.equals (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS) ? UnitSkillPositiveNegative.BOTH : UnitSkillPositiveNegative.POSITIVE);
						
						if (totalValue > 0)
						{
							// Work out background image according to the component that the bonus is coming from
							final BufferedImage backgroundImage = getUtils ().loadImage
								(getGraphicsDB ().findUnitSkillComponent (attrComponent, "generateAttributeImage").getUnitSkillComponentImageFile ()); 
							
							// Draw right number of attribute icons
							for (int n = 0; n < totalValue; n++)
							{
								final int attrX = ((drawnAttributeCount % ATTRIBUTE_ICONS_PER_ROW) * (backgroundImage.getWidth () + 1)) +
									
									// Leave a slightly bigger gap each 5
									(((drawnAttributeCount / ATTRIBUTE_ICONS_PER_GROUP) % ATTRIBUTE_ICON_GROUPS_PER_ROW) * 2) +
											
									// Indent 2nd, 3rd rows (i.e. after 15 or 20) slightly
									((drawnAttributeCount / ATTRIBUTE_ICONS_PER_ROW) * 4);
								
								final int attrY = (drawnAttributeCount / ATTRIBUTE_ICONS_PER_ROW) * 3;
								
								g.drawImage (backgroundImage, attrX, attrY, null);
								g.drawImage (attributeImage, attrX, attrY, null);
								
								// Dark hit points when we have lost health
								drawnAttributeCount++;
								if (drawnAttributeCount > attributeValueIncludingNegatives)
								{
									g.setColor (COLOUR_NEGATIVE_ATTRIBUTES);
									g.fillRect (attrX, attrY, backgroundImage.getWidth (), backgroundImage.getHeight ());
								}
							}
						}
					}
			}
			finally
			{
				g.dispose ();
			}
		}
		
		return image;
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
	public final MomLanguagesEx getLanguages ()
	{
		return languageHolder.getLanguages ();
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
	public final UnitCalculations getUnitCalculations ()
	{
		return unitCalculations;
	}

	/**
	 * @param calc Unit calculations
	 */
	public final void setUnitCalculations (final UnitCalculations calc)
	{
		unitCalculations = calc;
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
	 * @return Combat map processing
	 */
	public final CombatMapProcessing getCombatMapProcessing ()
	{
		return combatMapProcessing;
	}

	/**
	 * @param proc Combat map processing
	 */
	public final void setCombatMapProcessing (final CombatMapProcessing proc)
	{
		combatMapProcessing = proc;
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

	/**
	 * @return Sound effects player
	 */
	public final MomAudioPlayer getSoundPlayer ()
	{
		return soundPlayer;
	}

	/**
	 * @param player Sound effects player
	 */
	public final void setSoundPlayer (final MomAudioPlayer player)
	{
		soundPlayer = player;
	}

	/**
	 * @return Combat UI
	 */
	public final CombatUI getCombatUI ()
	{
		return combatUI;
	}

	/**
	 * @param ui Combat UI
	 */
	public final void setCombatUI (final CombatUI ui)
	{
		combatUI = ui;
	}
	
	/**
	 * @return Hero items UI
	 */
	public final HeroItemsUI getHeroItemsUI ()
	{
		return heroItemsUI;
	}

	/**
	 * @param ui Hero items UI
	 */
	public final void setHeroItemsUI (final HeroItemsUI ui)
	{
		heroItemsUI = ui;
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

}