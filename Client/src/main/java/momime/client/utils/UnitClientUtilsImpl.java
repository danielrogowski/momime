package momime.client.utils;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.JComponent;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.audio.AudioPlayer;
import momime.client.calculations.MomClientUnitCalculations;
import momime.client.config.v0_9_5.MomImeClientConfig;
import momime.client.graphics.database.CombatTileFigurePositionsEx;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.RangedAttackTypeEx;
import momime.client.graphics.database.UnitAttributeEx;
import momime.client.graphics.database.UnitCombatActionEx;
import momime.client.graphics.database.UnitEx;
import momime.client.graphics.database.UnitTypeEx;
import momime.client.graphics.database.v0_9_5.FigurePositionsForFigureCount;
import momime.client.graphics.database.v0_9_5.UnitCombatImage;
import momime.client.graphics.database.v0_9_5.UnitSkill;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.v0_9_5.Race;
import momime.client.process.CombatMapProcessing;
import momime.client.process.OverlandMapProcessing;
import momime.client.ui.components.HideableComponent;
import momime.client.ui.components.SelectUnitButton;
import momime.client.ui.frames.CityViewUI;
import momime.client.ui.frames.UnitInfoUI;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.common.MomException;
import momime.common.UntransmittedKillUnitActionID;
import momime.common.calculations.MomUnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Unit;
import momime.common.messages.AvailableUnit;
import momime.common.messages.MemoryUnit;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.KillUnitActionID;
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

	/** Combat map processing */
	private CombatMapProcessing combatMapProcessing;
	
	/** Pending movement utils */
	private PendingMovementUtils pendingMovementUtils;
	
	/** Client config, containing the scale setting */
	private MomImeClientConfig clientConfig;
	
	/** Sound effects player */
	private AudioPlayer soundPlayer;
	
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
		log.trace ("Entering getUnitName: " + unit.getUnitID () + ", " + unitNameType);
		
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
		
		log.trace ("Exiting getUnitName = " + unitName);
		return unitName;
	}
	
	/**
	 * Rules for finding the right icon for a unit attribute aren't totally straightforward; ranged attacks have their own images and some
	 * unit attributes (and some RATs) have different icons for different weapon types and some do not.  So this method deals with all that.
	 * 
	 * @param unit Unit whose attributes we're drawing
	 * @param unitAttributeID Which attribute to draw
	 * @return Icon for this unit attribute, or null if there isn't one
	 * @throws IOException If there's a problem finding the unit attribute icon
	 */
	@Override
	public final BufferedImage getUnitAttributeIcon (final AvailableUnit unit, final String unitAttributeID) throws IOException
	{
		log.trace ("Entering getUnitAttributeIcon: " + unit.getUnitID () + ", " + unitAttributeID);
		
		final String attributeImageName;
		if (unitAttributeID.equals (CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_RANGED_ATTACK))
		{
			// Ranged attacks have their own special rules, so we select the appropriate
			// type of range attack icon, e.g. bow, rock, blue blast.
			final Unit unitInfo = getClient ().getClientDB ().findUnit (unit.getUnitID (), "getUnitAttributeIcon");
			if (unitInfo.getRangedAttackType () == null)
				attributeImageName = null;
			else
			{
				// If there is only a single image then just use it; if there are multiple, then select the right one by weapon grade
				final RangedAttackTypeEx rat = getGraphicsDB ().findRangedAttackType (unitInfo.getRangedAttackType (), "getUnitAttributeIcon");
				if ((unit.getWeaponGrade () == null) || (rat.getRangedAttackTypeWeaponGrade ().size () == 1))
					attributeImageName = rat.getRangedAttackTypeWeaponGrade ().get (0).getUnitDisplayRangedImageFile ();
				else
					attributeImageName = rat.findWeaponGradeImageFile (unit.getWeaponGrade (), "getUnitAttributeIcon");
			}
		}
		else
		{
			// Some attribute other than ranged attack; same behaviour as above with weapon grades
			final UnitAttributeEx attrGfx = getGraphicsDB ().findUnitAttribute (unitAttributeID, "getUnitAttributeIcon");
			if ((unit.getWeaponGrade () == null) || (attrGfx.getUnitAttributeWeaponGrade ().size () == 1))
				attributeImageName = attrGfx.getUnitAttributeWeaponGrade ().get (0).getAttributeImageFile ();
			else
				attributeImageName = attrGfx.findWeaponGradeImageFile (unit.getWeaponGrade (), "getUnitAttributeIcon");
		}
		
		final BufferedImage attributeImage = (attributeImageName == null) ? null : getUtils ().loadImage (attributeImageName);
		
		log.trace ("Exiting getUnitAttributeIcon = " + attributeImage);
		return attributeImage;
	}
	
	/**
	 * Rules for finding the right icon for a unit skills aren't totally straightforward; experience icon changes as units
	 * gain level, and some skills (particularly movement type skills like walking/flying) have no icon at all.  So this method deals with all that.
	 * 
	 * @param unit Unit whose skills we're drawing
	 * @param unitSkillID Which attribute to draw
	 * @return Icon for this unit skill, or null if there isn't one
	 * @throws IOException If there's a problem finding the unit skill icon
	 */
	@Override
	public final BufferedImage getUnitSkillIcon (final AvailableUnit unit, final String unitSkillID) throws IOException
	{
		log.trace ("Entering getUnitSkillIcon: " + unit.getUnitID () + ", " + unitSkillID);
		
		final String image;
		if (unitSkillID.equals (CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE))
		{
			// Experience skill icon changes as the unit gains experience levels
			final ExperienceLevel expLvl = getUnitUtils ().getExperienceLevel (unit, true, getClient ().getPlayers (),
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (), getClient ().getClientDB ());
			if (expLvl == null)
				image = null;
			else
			{
				final String unitMagicRealmID = getClient ().getClientDB ().findUnit (unit.getUnitID (), "getUnitSkillIcon").getUnitMagicRealm ();
				final String unitTypeID = getClient ().getClientDB ().findUnitMagicRealm (unitMagicRealmID, "getUnitSkillIcon").getUnitTypeID ();
				final UnitTypeEx unitType = getGraphicsDB ().findUnitType (unitTypeID, "getUnitSkillIcon");
				image = unitType.findExperienceLevelImageFile (expLvl.getLevelNumber (), "getUnitSkillIcon");
			}
		}
		else
		{
			// Regular skill
			final UnitSkill skillGfx = getGraphicsDB ().findUnitSkill (unitSkillID, "getUnitSkillIcon");
			image = skillGfx.getUnitSkillImageFile ();
		}

		final BufferedImage skillImage = (image == null) ? null : getUtils ().loadImage (image);
		
		log.trace ("Exiting getUnitSkillIcon = " + skillImage);
		return skillImage;
	}

	/**
	 * Kills a unit, either permanently removing it or marking it as dead in case it gets Raise or Animate Dead cast on it later
	 * 
	 * @param unitURN Unit to kill
	 * @param transmittedAction Method by which the unit is being killed, out of possible values that are sent from the server; null if untransmittedAction is filled in
	 * @param untransmittedAction Method by which the unit is being killed, out of possible values that are inferred from other messages; null if transmittedAction is filled in
	 * @throws IOException If there is a problem
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final void killUnit (final int unitURN, final KillUnitActionID transmittedAction, final UntransmittedKillUnitActionID untransmittedAction)
		throws IOException, JAXBException, XMLStreamException
	{
		log.trace ("Entering killUnit: Unit URN " + unitURN + ", " + transmittedAction + ", " + untransmittedAction);

		// Even if not actually freeing the unit, we still need to eliminate all references to it, except for it being in the main unit list
		getPendingMovementUtils ().removeUnitFromAnyPendingMoves (getClient ().getOurTransientPlayerPrivateKnowledge ().getPendingMovement (), unitURN);
		getUnitUtils ().beforeKillingUnit (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), unitURN);	// Removes spells cast on unit
		
		// Is there a unit info screen open for it?
		final UnitInfoUI unitInfo = getClient ().getUnitInfos ().get (unitURN);
		if (unitInfo != null)
			unitInfo.close ();

		// Find the unit being removed
		final MemoryUnit unit = getUnitUtils ().findUnitURN (unitURN,
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "killUnit");

		getOverlandMapProcessing ().removeUnitFromLeftToMoveOverland (unit);
		getCombatMapProcessing ().removeUnitFromLeftToMoveCombat (unit);
		
		// Select unit buttons on the Map
		for (final HideableComponent<SelectUnitButton> button : getOverlandMapRightHandPanel ().getSelectUnitButtons ())
			if ((!button.isHidden ()) && (button.getComponent ().getUnit () != null) && (button.getComponent ().getUnit ().getUnitURN () == unitURN))
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
		if (transmittedAction != null)
			switch (transmittedAction)
			{
				// Phyically free the unit
				case FREE:
				case VISIBLE_AREA_CHANGED:
					getUnitUtils ().removeUnitURN (unitURN, getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ());
					break;
	
				// Units dying from lack of production are a problem, because we always get the kill message first before the NTM arrives, so if we just
				// immediately kill the unit off, the NTM then doesn't know what it was - just its UnitURN, so it can't output any meaningful description.
				// So don't remove these just yet - just mark them with a special status.  The NTM itself permanently removes these after.
				case UNIT_LACK_OF_PRODUCTION:
				case HERO_LACK_OF_PRODUCTION:
					unit.setStatus (UnitStatusID.KILLED_BY_LACK_OF_PRODUCTION);
					break;
					
				default:
					throw new MomException ("killUnit got a transmittedAction that it doesn't know how to handle: " + transmittedAction);
			}
		else
			switch (untransmittedAction)
			{
				/**
				 * If a unit is killed in a combat we're involved in, we still need a record of it, so we can cast Raise Dead on it (no matter if its ours or theirs).
				 * Our killed heroes are also just marked as Dead but not freed, so after combat we can cast Resurrection on them.
				 * Only exception is killed enemy heroes, who we can't Raise or Resurrect, so we just free them.
				 */
				case COMBAT_DAMAGE:
					if ((unit.getOwningPlayerID () != getClient ().getOurPlayerID ()) && (getClient ().getClientDB ().findUnit
						(unit.getUnitID (), "killUnit").getUnitMagicRealm ().equals (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO)))
						
						getUnitUtils ().removeUnitURN (unitURN, getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ());
					else
						unit.setStatus (UnitStatusID.DEAD);
					break;

				default:
					throw new MomException ("killUnit got an untransmittedAction that it doesn't know how to handle: " + untransmittedAction);
			}

		// Select unit buttons on the City screen
		if (unit.getUnitLocation () != null)
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
		log.trace ("Entering registerUnitFiguresAnimation: " + unitID + ", " + combatActionID + ", " + direction + ", " + component);
		
		final UnitEx unit = getGraphicsDB ().findUnit (unitID, "registerUnitFiguresAnimation");
		final UnitCombatImage unitImage = unit.findCombatAction (combatActionID, "registerUnitFiguresAnimation").findDirection (direction, "registerUnitFiguresAnimation");
		getAnim ().registerRepaintTrigger (unitImage.getUnitCombatAnimation (), component);
		
		if (unit.getSecondaryUnitID () != null)
		{
			final UnitEx secondaryUnit = getGraphicsDB ().findUnit (unit.getSecondaryUnitID (), "registerUnitFiguresAnimation");
			final UnitCombatImage secondaryUnitImage = secondaryUnit.findCombatAction (combatActionID, "registerUnitFiguresAnimation").findDirection (direction, "registerUnitFiguresAnimation");
			getAnim ().registerRepaintTrigger (secondaryUnitImage.getUnitCombatAnimation (), component);
		}

		log.trace ("Exiting registerUnitFiguresAnimation");
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
		log.trace ("Entering unregisterUnitFiguresAnimation: " + unitID + ", " + combatActionID + ", " + direction + ", " + component);

		final UnitEx unit = getGraphicsDB ().findUnit (unitID, "unregisterUnitFiguresAnimation");
		final UnitCombatImage unitImage = unit.findCombatAction (combatActionID, "unregisterUnitFiguresAnimation").findDirection (direction, "unregisterUnitFiguresAnimation");
		getAnim ().unregisterRepaintTrigger (unitImage.getUnitCombatAnimation (), component);
		
		if (unit.getSecondaryUnitID () != null)
		{
			final UnitEx secondaryUnit = getGraphicsDB ().findUnit (unit.getSecondaryUnitID (), "unregisterUnitFiguresAnimation");
			final UnitCombatImage secondaryUnitImage = secondaryUnit.findCombatAction (combatActionID, "unregisterUnitFiguresAnimation").findDirection (direction, "unregisterUnitFiguresAnimation");
			getAnim ().unregisterRepaintTrigger (secondaryUnitImage.getUnitCombatAnimation (), component);
		}
		
		log.trace ("Exiting unregisterUnitFiguresAnimation");
	}

	/**
	 * Calculates the positions of all the figures of a unit, taking into account combat scale and so on.
	 * This is used both to draw the figures of a unit, and to get the positions to start ranged attack missiles at when the unit fires.
	 * NB. The resulting array may not have aliveFigureCount elements; it may be x1, x4 or x5 depending on combat scale.
	 * The calling routine should just iterate over the resulting array and make no assumptions about how many elements will contain. 
	 * 
	 * @param unitID The unit to draw
	 * @param unitTypeID The type of unit it is (can pass in null - the only value it cares about is if this is 'S' for summoned units)
	 * @param totalFigureCount The number of figures the unit had when fully healed
	 * @param aliveFigureCount The number of figures the unit has now
	 * @param offsetX The x offset into the graphics context to draw the unit at
	 * @param offsetY The y offset into the graphics context to draw the unit at
	 * @return Array of all figure positions in pixels; [0] element = x coord, [1] element = y coord, [2] element = multiplier to enlarge image by
	 * @throws IOException If there is a problem
	 */
	@Override
	public final int [] [] calcUnitFigurePositions (final String unitID, final String unitTypeID, final int totalFigureCount, final int aliveFigureCount,
		final int offsetX, final int offsetY) throws IOException
	{
		// Get the main unit
		final UnitEx unit = getGraphicsDB ().findUnit (unitID, "calcUnitFigurePositions");
		
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
				throw new MomException ("calcUnitFigurePositions encountered a scale that it doesn't know how to handle: " + getClientConfig ().getUnitCombatScale ());
		}
		
		// Show heroes with entourage of cavalry accompanying them
		if ((figureMultiplier == 4) && (unit.getSecondaryUnitID () != null))
			figureMultiplier++;
		
		// Get the positions of the n times
		final int [] [] result = new int [aliveFigureCount * figureMultiplier] [3];
		final CombatTileFigurePositionsEx positions = getGraphicsDB ().findCombatTileUnitRelativeScale (relativeScale, "calcUnitFigurePositions").findFigureCount (totalFigureCount * figureMultiplier, "drawUnitFigures");
		for (int n = 0; n < (aliveFigureCount * figureMultiplier); n++)
		{
			final FigurePositionsForFigureCount position = positions.findFigureNumber (n+1, "calcUnitFigurePositions");

			// This is to account that the unit's feet don't touch the bottom of the image
			final int fudgeY = (totalFigureCount <= 2) ? 4 : 6;
			
			// Generate coordinates
			result [n] [0] = offsetX + (position.getTileRelativeX () * relativeScaleMultiplier);
			result [n] [1] = offsetY + (position.getTileRelativeY () * relativeScaleMultiplier) + (fudgeY * unitImageMultiplier);
			result [n] [2] = unitImageMultiplier;
		}
		
		return result;
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
	 * @param registeredAnimation Determines frame number: True=by Swing timer, must have previously called registerRepaintTrigger; False=by System.nanoTime ()
	 * @throws IOException If there is a problem
	 */
	@Override
	public final void drawUnitFigures (final String unitID, final String unitTypeID, final int totalFigureCount, final int aliveFigureCount, final String combatActionID,
		final int direction, final Graphics g, final int offsetX, final int offsetY, final String sampleTileImageFile, final boolean registeredAnimation) throws IOException
	{
		// Draw sample tile
		if (sampleTileImageFile != null)
		{
			final BufferedImage tileImage = getUtils ().loadImage (sampleTileImageFile);
			g.drawImage (tileImage, offsetX, offsetY, tileImage.getWidth () * 2, tileImage.getHeight () * 2, null);
		}
		
		// Get the main unit
		final UnitEx unit = getGraphicsDB ().findUnit (unitID, "drawUnitFigures");
		
		// Get all the figure positions
		final int [] [] figurePositions = calcUnitFigurePositions (unitID, unitTypeID, totalFigureCount, aliveFigureCount, offsetX, offsetY);
		
		// Show heroes with entourage of cavalry accompanying them
		BufferedImage secondaryImage = null;
		if (figurePositions.length / aliveFigureCount == 5)		// sneaky way to figure out figureMultiplier value
		{
			final UnitEx secondaryUnit = getGraphicsDB ().findUnit (unit.getSecondaryUnitID (), "drawUnitFigures");
			final UnitCombatImage secondaryUnitImage = secondaryUnit.findCombatAction (combatActionID, "drawUnitFigures").findDirection (direction, "drawUnitFigures");
			secondaryImage = getAnim ().loadImageOrAnimationFrame
				(secondaryUnitImage.getUnitCombatImageFile (), secondaryUnitImage.getUnitCombatAnimation (), registeredAnimation);
		}
		
		// Work out the image to draw n times
		final UnitCombatImage unitImage = unit.findCombatAction (combatActionID, "drawUnitFigures").findDirection (direction, "drawUnitFigures");
		final BufferedImage image = getAnim ().loadImageOrAnimationFrame
			(unitImage.getUnitCombatImageFile (), unitImage.getUnitCombatAnimation (), registeredAnimation);
		
		// Draw the figure in each position
		int n = 1;
		for (final int [] position : figurePositions)
		{
			// Select image to draw
			final BufferedImage useImage;
			if (secondaryImage == null)
				useImage = image;
			else
				useImage = (n == 3) ? image : secondaryImage;
			
			// 3rd array element tells us what to multiply the image size up by
			final int imageWidth = useImage.getWidth () * position [2];
			final int imageHeight = useImage.getHeight () * position [2];
			
			// TileRelativeX, Y in the graphics XML indicates the position of the unit's feet, so need to adjust according to the unit size
			g.drawImage (useImage,
				position [0] - (imageWidth / 2),
				position [1] - imageHeight,
				imageWidth, imageHeight, null);
			
			n++;
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
	 * @throws IOException If there is a problem
	 */
	@Override
	public final void drawUnitFigures (final AvailableUnit unit, final String combatActionID,
		final int direction, final Graphics g, final int offsetX, final int offsetY, final boolean drawSampleTile, final boolean registeredAnimation) throws IOException
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
		
		if (aliveFigureCount > 0)
		{
			// Get unit type
			final String unitTypeID = getClient ().getClientDB ().findUnitMagicRealm (unitDef.getUnitMagicRealm (), "drawUnitFigures").getUnitTypeID ();
			
			// Get sample tile
			final String sampleTileImageFile;
			if (drawSampleTile)
				sampleTileImageFile = getClientUnitCalculations ().findPreferredMovementSkillGraphics (unit).getSampleTileImageFile ();
			else
				sampleTileImageFile = null; 
			
			// Call other version now that we have all the necessary values
			drawUnitFigures (unit.getUnitID (), unitTypeID, totalFigureCount, aliveFigureCount, combatActionID,
				direction, g, offsetX, offsetY, sampleTileImageFile, registeredAnimation);
		}
	}
	
	/**
	 * When we 4x the number of units in a tile, if they still take the same amount of time to walk from tile to tile, it looks
	 * like they are really sprinting it - so make these take longer, according to the same rules as how unitImageMultiplier is
	 * calculated in drawUnitFigures above.  i.e. if unitImageMultiplier == 2 then take 1 second, if unitImageMultiplier == 1 then take 2 seconds.
	 *  
	 * @param unit The unit that is walking/flying
	 * @return Time, in seconds, a unit takes to walk from tile to tile in combat
	 * @throws RecordNotFoundException If we can't find the unit definition or its magic realm
	 * @throws MomException If we encounter a combatScale that we don't know how to handle
	 */
	@Override
	public final double calculateWalkTiming (final AvailableUnit unit) throws RecordNotFoundException, MomException
	{
		log.trace ("Entering calculateWalkTiming: " + unit.getUnitID ());
		
		final int unitImageMultiplier;
		
		// Derivation of unitImageMultiplier is copied directly from drawUnitFigures
		switch (getClientConfig ().getUnitCombatScale ())
		{
			case DOUBLE_SIZE_UNITS:
				unitImageMultiplier = 2;
				break;
				
			case FOUR_TIMES_FIGURES:
				unitImageMultiplier = 1;
				break;
				
			case FOUR_TIMES_FIGURES_EXCEPT_SINGLE_SUMMONED:
				
				// Get total figures
				final Unit unitDef = getClient ().getClientDB ().findUnit (unit.getUnitID (), "calculateWalkTiming");
				final int totalFigureCount = getUnitUtils ().getFullFigureCount (unitDef);

				// Get unit type
				final String unitTypeID = getClient ().getClientDB ().findUnitMagicRealm (unitDef.getUnitMagicRealm (), "calculateWalkTiming").getUnitTypeID ();
				
				unitImageMultiplier = ((totalFigureCount == 1) && (CommonDatabaseConstants.VALUE_UNIT_TYPE_ID_SUMMONED.equals (unitTypeID))) ? 2 : 1;
				break;
				
			default:
				throw new MomException ("calculateWalkTiming encountered a scale that it doesn't know how to handle: " + getClientConfig ().getUnitCombatScale ());
		}
		
		final int result = 3 - unitImageMultiplier;		
		log.trace ("Exiting calculateWalkTiming = " + result);
		return result;
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
		log.trace ("Entering playCombatActionSound: " + unit.getUnitID () + ", " + combatActionID);
		
		// See if there's a specific override sound specified for this unit.
		// This so earth elementals make a stomp stomp noise, cavalry go clippity clop and regular swordsmen go clank clank, even though they're all just doing the WALK action.
		final String soundEffectFilename;
		final UnitEx unitGfx = getGraphicsDB ().findUnit (unit.getUnitID (), "playCombatActionSound");
		final UnitCombatActionEx unitCombatAction = unitGfx.findCombatAction (combatActionID, "playCombatActionSound");
		if (unitCombatAction.getOverrideActionSoundFile () != null)
			soundEffectFilename = unitCombatAction.getOverrideActionSoundFile ();
		else
		{
			// If we didn't find a specific sound for this unit, use the general one
			soundEffectFilename = getGraphicsDB ().findCombatAction (combatActionID, "playCombatActionSound").getDefaultActionSoundFile ();
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
		
		log.trace ("Exiting playCombatActionSound");
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
	public final AudioPlayer getSoundPlayer ()
	{
		return soundPlayer;
	}

	/**
	 * @param player Sound effects player
	 */
	public final void setSoundPlayer (final AudioPlayer player)
	{
		soundPlayer = player;
	}
}