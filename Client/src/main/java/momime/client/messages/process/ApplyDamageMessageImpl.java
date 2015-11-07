package momime.client.messages.process;

import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.base.client.AnimatedServerToClientMessage;

import momime.client.MomClient;
import momime.client.audio.AudioPlayer;
import momime.client.calculations.ClientUnitCalculations;
import momime.client.calculations.CombatMapBitmapGenerator;
import momime.client.graphics.database.AnimationGfx;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.RangedAttackTypeCombatImageGfx;
import momime.client.graphics.database.RangedAttackTypeGfx;
import momime.client.graphics.database.SpellGfx;
import momime.client.graphics.database.TileSetGfx;
import momime.client.process.CombatMapProcessing;
import momime.client.ui.components.HideableComponent;
import momime.client.ui.components.SelectUnitButton;
import momime.client.ui.frames.ArmyListUI;
import momime.client.ui.frames.CityViewUI;
import momime.client.ui.frames.CombatUI;
import momime.client.ui.frames.HeroItemsUI;
import momime.client.ui.frames.UnitInfoUI;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.client.utils.AnimationController;
import momime.client.utils.UnitClientUtils;
import momime.common.UntransmittedKillUnitActionID;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DamageTypeID;
import momime.common.database.RangedAttackTypeActionID;
import momime.common.database.Unit;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.ApplyDamageMessage;
import momime.common.messages.servertoclient.ApplyDamageMessageUnit;
import momime.common.messages.servertoclient.KillUnitActionID;
import momime.common.utils.UnitUtils;

/**
 * Message server sends to all clients when an attack takes place, this might damage the attacker and/or the defender.
 * For the players actually involved in the combat, this message will also generate the animation to show the units swinging their swords at each other.
 * 
 * This also gets sent to players not involved in the combat who can see one or other unit; in that situation its possible that an outside observer can see one of the units but
 * not the other - in that situation one of the UnitURNs (and other values associated with that unit) will all be left null, this is why every value is optional.
 */
public final class ApplyDamageMessageImpl extends ApplyDamageMessage implements AnimatedServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (MoveUnitStackOverlandMessageImpl.class);
	
	/** FPS that we show ranged missiles at */
	private static final int RANGED_ATTACK_FPS = 10;
	
	/** How many ticks at the above FPS we show the unit firing a ranged attack for */
	private static final int RANGED_ATTACK_LAUNCH_TICKS = 2;

	/** How many ticks at the above FPS we show the unit being hit by a ranged attack for */
	private static final int RANGED_ATTACK_IMPACT_TICKS = 2;
	
	/** How many ticks an incoming spell makes prior to hitting its target */
	private static final int INCOMING_SPELL_TICKS = 10;
	
	/** How many pixels incoming spells move each tick */
	private static final int INCOMING_SPELL_TICK_DISTANCE = 120 / INCOMING_SPELL_TICKS;

	/** Multiplayer client */
	private MomClient client;
	
	/** Unit utils */
	private UnitUtils unitUtils;

	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** Client-side unit utils */
	private UnitClientUtils unitClientUtils;
	
	/** Combat UI */
	private CombatUI combatUI;
	
	/** Combat map processing */
	private CombatMapProcessing combatMapProcessing;
	
	/** Client unit calculations */
	private ClientUnitCalculations clientUnitCalculations;

	/** Bitmap generator includes routines for calculating pixel coords */
	private CombatMapBitmapGenerator combatMapBitmapGenerator;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Sound effects player */
	private AudioPlayer soundPlayer;
	
	/** Animation controller */
	private AnimationController anim;
	
	/** Overland map right hand panel showing economy etc */
	private OverlandMapRightHandPanel overlandMapRightHandPanel;
	
	/** Army list */
	private ArmyListUI armyListUI;

	/** Hero items UI */
	private HeroItemsUI heroItemsUI;
	
	/** The attacking unit; null if we can't see it */
	private MemoryUnit attackerUnit;
	
	/** The defending unit(s) that we can see */
	private List<ApplyDamageMessageDefenderDetails> defenderUnits;
	
	/** How much damage the unit had taken before this attack; we use this to animate the damage slowly being applied */
	private Integer attackerDamageTakenStart;
	
	/** Work the duration out once only */
	private double duration;
	
	/** Number of animation ticks */
	private int tickCount;
	
	/** Damage is animated if we're one of the players invovled in the combat; damage is instant if its someone else's combat */
	private boolean animated;
	
	/** Start coordinates of each individual missile making a ranged attack, in pixels */
	private int [] [] start;
	
	/** X coordinate of the unit being shot by a ranged attack, in pixels (missiles always converge */
	private int endX;
	
	/** Y coordinate of the unit being shot by a ranged attack, in pixels */
	private int endY;
	
	/** Current coordinates of each individual missile making a ranged attack, in pixels; null if still showing the initial portion of the firing animation */
	private int [] [] current;
	
	/** Image of ranged attack flying towards its target */
	private RangedAttackTypeCombatImageGfx ratFlyImage;

	/** Image of ranged attack hitting its target */
	private RangedAttackTypeCombatImageGfx ratStrikeImage;
	
	/** Current image of ranged attack */
	private RangedAttackTypeCombatImageGfx ratCurrentImage;
	
	/** Animation to display; null if the damage isn't coming from a spell or the spell has no animation */
	private AnimationGfx spellAnim;

	/** Animation to display of the spell flying in from off screen; null if the damage isn't coming from a spell, the spell has no animation, or the spell has a stationary animation */
	private AnimationGfx spellAnimFly;
	
	/** Combat map tile set is required all over the place */
	private TileSetGfx combatMapTileSet;
	
	/** Details of spell graphics */
	private SpellGfx spellGfx;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start");
		
		// Can we see the attacker?
		if (getAttackerUnitURN () != null)
		{
			attackerUnit = getUnitUtils ().findUnitURN (getAttackerUnitURN (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "ApplyDamageMessageImpl-a");
			attackerDamageTakenStart = attackerUnit.getDamageTaken ();
			attackerUnit.setCombatHeading (getAttackerDirection ());
		}

		// Is either unit ours?  If so, then it must be damage from the combat we're in; if not, then it must be some combat going on elsewhere
		animated = (getAttackerPlayerID () == getClient ().getOurPlayerID ());
		
		// Can we see the defender(s)?
		defenderUnits = new ArrayList<ApplyDamageMessageDefenderDetails> ();
		for (final ApplyDamageMessageUnit thisUnitDetails : getDefenderUnit ())
		{
			final MemoryUnit thisUnit = getUnitUtils ().findUnitURN (thisUnitDetails.getDefenderUnitURN (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "ApplyDamageMessageImpl-d");
			thisUnit.setCombatHeading (thisUnitDetails.getDefenderDirection ());
			defenderUnits.add (new ApplyDamageMessageDefenderDetails (thisUnit, thisUnitDetails.getDefenderDamageTaken ()));

			if (thisUnit.getOwningPlayerID () == getClient ().getOurPlayerID ())
				animated = true;			
		}
		
		// Perform any animation startup necessary
		if (!animated)
		{
			duration = 0;
			tickCount = 0;
		}
		else
		{
			getCombatUI ().setAttackAnim (this);
			combatMapTileSet = getGraphicsDB ().findTileSet (GraphicsDatabaseConstants.TILE_SET_COMBAT_MAP, "ApplyDamageMessageImpl");
			
			if ((CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK.equals (getAttackSkillID ())) && (getDefenderUnits ().size () == 1))
			{
				// Start a ranged attack animation - firstly, after a brief frame of showing the unit firing, it'll be back to standing still
				// To animate the missiles, first we need the locations (in pixels) of the two units involved
				final int startX = getCombatMapBitmapGenerator ().combatCoordinatesX (attackerUnit.getCombatPosition ().getX (), attackerUnit.getCombatPosition ().getY (), combatMapTileSet);
				final int startY = getCombatMapBitmapGenerator ().combatCoordinatesY (attackerUnit.getCombatPosition ().getX (), attackerUnit.getCombatPosition ().getY (), combatMapTileSet);
				
				final MemoryUnit singleDefender = getDefenderUnits ().get (0).getDefUnit ();
				endX = getCombatMapBitmapGenerator ().combatCoordinatesX (singleDefender.getCombatPosition ().getX (), singleDefender.getCombatPosition ().getY (), combatMapTileSet);
				endY = getCombatMapBitmapGenerator ().combatCoordinatesY (singleDefender.getCombatPosition ().getX (), singleDefender.getCombatPosition ().getY (), combatMapTileSet);
				
				// Work out the firing distance in pixels
				final double dx = startX - endX;
				final double dy = startY - endY;
				
				final double firingDistancePixels = Math.sqrt ((dx * dx) + (dy * dy));
				
				// Move around 50 pixels per frame
				tickCount = ((int) (firingDistancePixels / 50)) + RANGED_ATTACK_LAUNCH_TICKS + RANGED_ATTACK_IMPACT_TICKS;
				duration = tickCount / (double) RANGED_ATTACK_FPS;
				
				// Get the start location of every individual missile
				final Unit attackerUnitDef = getClient ().getClientDB ().findUnit (attackerUnit.getUnitID (), "ApplyDamageMessageImpl");
				final String unitTypeID = getClient ().getClientDB ().findUnitMagicRealm (attackerUnitDef.getUnitMagicRealm (), "ApplyDamageMessageImpl").getUnitTypeID ();
				final int totalFigureCount = getUnitUtils ().getFullFigureCount (attackerUnitDef);
				final int aliveFigureCount = getUnitCalculations ().calculateAliveFigureCount (attackerUnit, getClient ().getPlayers (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());
				
				start = getUnitClientUtils ().calcUnitFigurePositions (attackerUnit.getUnitID (), unitTypeID, totalFigureCount, aliveFigureCount, startX, startY);

				// Start animation of the missile; e.g. fireballs don't have a constant image
				final String rangedAttackTypeID = attackerUnitDef.getRangedAttackType ();
				final RangedAttackTypeGfx rat = getGraphicsDB ().findRangedAttackType (rangedAttackTypeID, "ApplyDamageMessageImpl");
				ratFlyImage = rat.findCombatImage (RangedAttackTypeActionID.FLY, getAttackerDirection (), "ApplyDamageMessageImpl");
				
				ratStrikeImage = rat.findCombatImage (RangedAttackTypeActionID.STRIKE, getAttackerDirection (), "ApplyDamageMessageImpl");
				
				// Play shooting sound, based on the rangedAttackType
				if (rat.getRangedAttackSoundFile () == null)
					log.warn ("Found entry in graphics DB for rangedAttackType " + rangedAttackTypeID + " but it has no sound effect defined");
				else
					try
					{
						getSoundPlayer ().playAudioFile (rat.getRangedAttackSoundFile ());
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
			}
			else if (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK.equals (getAttackSkillID ()))
			{
				// Start a close combat attack animation
				getUnitClientUtils ().playCombatActionSound (attackerUnit, GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_MELEE_ATTACK);
				
				// For melee attacks is set to 1 second, and melee animations in the graphics XML are set at 6 FPS, so there'll be 6 frames to show
				duration = 1;
				tickCount = 6;
			}
			else if (getAttackSpellID () != null)
			{
				// Find spell graphics
				spellGfx = getGraphicsDB ().findSpell (getAttackSpellID (), "ApplyDamageMessageImpl");
				
				// Is there an animation to display for it?
				if (spellGfx.getCombatCastAnimationFly () != null)
				{
					// Spell that flies in from off the combat map, like fire bolt
					spellAnim = getGraphicsDB ().findAnimation (spellGfx.getCombatCastAnimation (), "ApplyDamageMessageImpl");
					spellAnimFly = getGraphicsDB ().findAnimation (spellGfx.getCombatCastAnimationFly (), "ApplyDamageMessageImpl (F)");
					tickCount = INCOMING_SPELL_TICKS + spellAnim.getFrame ().size () + 1;
					duration = tickCount / spellAnim.getAnimationSpeed ();

					// Show anim on CombatUI
					final int distance = INCOMING_SPELL_TICK_DISTANCE * INCOMING_SPELL_TICKS;
					final int xMultiplier = (spellGfx.getCombatCastAnimationFlyXMultiplier () == null) ? 0 : spellGfx.getCombatCastAnimationFlyXMultiplier ();
					final int adjustX = 2 * ((xMultiplier * distance) + ((spellAnim.getCombatCastOffsetX () == null) ? 0 : spellAnim.getCombatCastOffsetX ()));
					final int adjustY = 2 * (-distance + ((spellAnim.getCombatCastOffsetY () == null) ? 0 : spellAnim.getCombatCastOffsetY ()));
					
					for (final ApplyDamageMessageDefenderDetails spellTargetUnit : getDefenderUnits ())
						getCombatUI ().getCombatCastAnimationPositions ().add (new Point (adjustX + getCombatMapBitmapGenerator ().combatCoordinatesX
							(spellTargetUnit.getDefUnit ().getCombatPosition ().getX (), spellTargetUnit.getDefUnit ().getCombatPosition ().getY (), combatMapTileSet),
						adjustY + getCombatMapBitmapGenerator ().combatCoordinatesY
							(spellTargetUnit.getDefUnit ().getCombatPosition ().getX (), spellTargetUnit.getDefUnit ().getCombatPosition ().getY (), combatMapTileSet)));
	
					getCombatUI ().setCombatCastAnimationFrame (0);
					getCombatUI ().setCombatCastAnimation (spellAnimFly);
					getCombatUI ().setCombatCastAnimationInFront (true);
				}
				else if (spellGfx.getCombatCastAnimation () != null)
				{
					// Spell that animates in place, like psionic blast
					spellAnim = getGraphicsDB ().findAnimation (spellGfx.getCombatCastAnimation (), "ApplyDamageMessageImpl");
					tickCount = spellAnim.getFrame ().size ();
					duration = spellAnim.getFrame ().size () / spellAnim.getAnimationSpeed ();

					// Show anim on CombatUI
					final int adjustX = (spellAnim.getCombatCastOffsetX () == null) ? 0 : 2 * spellAnim.getCombatCastOffsetX ();
					final int adjustY = (spellAnim.getCombatCastOffsetY () == null) ? 0 : 2 * spellAnim.getCombatCastOffsetY ();
					
					for (final ApplyDamageMessageDefenderDetails spellTargetUnit : getDefenderUnits ())
						getCombatUI ().getCombatCastAnimationPositions ().add (new Point (adjustX + getCombatMapBitmapGenerator ().combatCoordinatesX
							(spellTargetUnit.getDefUnit ().getCombatPosition ().getX (), spellTargetUnit.getDefUnit ().getCombatPosition ().getY (), combatMapTileSet),
						adjustY + getCombatMapBitmapGenerator ().combatCoordinatesY
							(spellTargetUnit.getDefUnit ().getCombatPosition ().getX (), spellTargetUnit.getDefUnit ().getCombatPosition ().getY (), combatMapTileSet)));
	
					getCombatUI ().setCombatCastAnimationFrame (0);
					getCombatUI ().setCombatCastAnimation (spellAnim);
					getCombatUI ().setCombatCastAnimationInFront ((spellGfx.isCombatCastAnimationInFront () == null) ? true : spellGfx.isCombatCastAnimationInFront ());
				}
				
				// Play spell sound if there is one (some spells are silent, so should be no warning for this)
				if (spellGfx.getSpellSoundFile () != null)
					try
					{
						getSoundPlayer ().playAudioFile (spellGfx.getSpellSoundFile ());
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
			}
		}
		
		log.trace ("Exiting start");
	}

	/**
	 * @return Number of seconds that the animation takes to display
	 */
	@Override
	public final double getDuration ()
	{
		return duration;
	}
	
	/**
	 * @return Number of ticks that the duration is divided into
	 */
	@Override
	public final int getTickCount ()
	{
		return tickCount;
	}
	
	/**
	 * @param tickNumber How many ticks have occurred, from 1..tickCount
	 */
	@Override
	public final void tick (final int tickNumber)
	{
		if (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK.equals (getAttackSkillID ()))
		{
			// Animate a ranged attack
			if (tickNumber >= RANGED_ATTACK_LAUNCH_TICKS)
			{
				final double ratio = Math.min (1.0, (double) tickNumber / (tickCount - RANGED_ATTACK_LAUNCH_TICKS));
				current = new int [start.length] [3];
				for (int n = 0; n < start.length; n++)
				{
					current [n] [0] = start [n] [UnitClientUtils.CALC_UNIT_FIGURE_POSITIONS_COLUMN_X_INCL_OFFSET] + (int) ((endX - start [n] [UnitClientUtils.CALC_UNIT_FIGURE_POSITIONS_COLUMN_X_INCL_OFFSET]) * ratio);
					current [n] [1] = start [n] [UnitClientUtils.CALC_UNIT_FIGURE_POSITIONS_COLUMN_Y_INCL_OFFSET] + (int) ((endY - start [n] [UnitClientUtils.CALC_UNIT_FIGURE_POSITIONS_COLUMN_Y_INCL_OFFSET]) * ratio);
					current [n] [2] = start [n] [UnitClientUtils.CALC_UNIT_FIGURE_POSITIONS_COLUMN_UNIT_IMAGE_MULTIPLIER];
				}
			}
			
			ratCurrentImage = (tickNumber >= tickCount - RANGED_ATTACK_IMPACT_TICKS) ? ratStrikeImage : ratFlyImage;
		}
		else if (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK.equals (getAttackSkillID ()))
		{
			// Animate a close combat attack - gradually ramp up the damage taken by both units.
			// Don't even need to force a repaint, because registering the 'melee' animation will do it for us.
			// Also Either unit have unit info screens open that need to update?
			final double ratio = (double) tickNumber / tickCount;
			try
			{
				// Attacker
				attackerUnit.setDamageTaken (attackerDamageTakenStart + (int) ((getAttackerDamageTaken () - attackerDamageTakenStart) * ratio));

				final UnitInfoUI attackerUI = getClient ().getUnitInfos ().get (getAttackerUnitURN ());
				if (attackerUI != null)
					attackerUI.getUnitInfoPanel ().showUnit (attackerUI.getUnitInfoPanel ().getUnit ());

				// Defenders
				for (final ApplyDamageMessageDefenderDetails thisUnit : getDefenderUnits ())
				{
					thisUnit.getDefUnit ().setDamageTaken
						(thisUnit.getDefenderDamageTakenStart () + (int) ((thisUnit.getDefenderDamageTakenEnd () - thisUnit.getDefenderDamageTakenStart ()) * ratio));
					
					final UnitInfoUI defenderUI = getClient ().getUnitInfos ().get (thisUnit.getDefUnit ().getUnitURN ());
					if (defenderUI != null)
						defenderUI.getUnitInfoPanel ().showUnit (defenderUI.getUnitInfoPanel ().getUnit ());
				}
			}
			catch (final IOException e)
			{
				log.error (e, e);
			}
			
			// Either unit have a select unit button that needs to update?
			for (final HideableComponent<SelectUnitButton> button : getOverlandMapRightHandPanel ().getSelectUnitButtons ())
				if ((!button.isHidden ()) &&
					((button.getComponent ().getUnit () == attackerUnit) || (button.getComponent ().getUnit () == defenderUnit)))
					button.repaint ();

			// Looking for the city screen at attackerUnit.getUnitLocation () & defenderUnit.getUnitLocation () doesn't seem to work
			// so just check them all.  There shouldn't be too many city screens open anyway.
			for (final CityViewUI cityView : getClient ().getCityViews ().values ())
				for (final SelectUnitButton button : cityView.getSelectUnitButtons ())
					if ((button.getUnit () == attackerUnit) || (button.getUnit () == defenderUnit))
						button.repaint ();
		}
		else if (spellAnimFly != null)
		{
			// Fly the spell in from off screen
			if (tickNumber <= INCOMING_SPELL_TICKS)
			{
				final int distance = INCOMING_SPELL_TICK_DISTANCE * (INCOMING_SPELL_TICKS - tickNumber);
				final int xMultiplier = (spellGfx.getCombatCastAnimationFlyXMultiplier () == null) ? 0 : spellGfx.getCombatCastAnimationFlyXMultiplier ();
				final int adjustX = 2 * ((xMultiplier * distance) + ((spellAnim.getCombatCastOffsetX () == null) ? 0 : spellAnim.getCombatCastOffsetX ()));
				final int adjustY = 2 * (-distance + ((spellAnim.getCombatCastOffsetY () == null) ? 0 : spellAnim.getCombatCastOffsetY ()));
				
				getCombatUI ().getCombatCastAnimationPositions ().clear ();
				for (final ApplyDamageMessageDefenderDetails spellTargetUnit : getDefenderUnits ())
					getCombatUI ().getCombatCastAnimationPositions ().add (new Point (adjustX + getCombatMapBitmapGenerator ().combatCoordinatesX
						(spellTargetUnit.getDefUnit ().getCombatPosition ().getX (), spellTargetUnit.getDefUnit ().getCombatPosition ().getY (), combatMapTileSet),
					adjustY + getCombatMapBitmapGenerator ().combatCoordinatesY
						(spellTargetUnit.getDefUnit ().getCombatPosition ().getX (), spellTargetUnit.getDefUnit ().getCombatPosition ().getY (), combatMapTileSet)));

				getCombatUI ().setCombatCastAnimationFrame (tickNumber % spellAnimFly.getFrame ().size ());
			}
			else
			{
				// Hitting target
				getCombatUI ().setCombatCastAnimationFrame ((tickNumber - INCOMING_SPELL_TICKS - 1) & spellAnim.getFrame ().size ());
				getCombatUI ().setCombatCastAnimation (spellAnim);
			}
		}
		else if (spellAnim != null)
		{
			getCombatUI ().setCombatCastAnimationFrame (tickNumber - 1);
		}
	}

	/**
	 * @return True, because the animation auto completes as soon as the unit reaches the destination cell
	 */
	@Override
	public final boolean isFinishAfterDuration ()
	{
		return true;
	}

	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void finish () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering finish");
		
		// Work out kill actions - different depending on whether its a combat we're involved in or not
		final KillUnitActionID transmittedAction = animated ? null : KillUnitActionID.FREE;
		final UntransmittedKillUnitActionID untransmittedAction = animated ? UntransmittedKillUnitActionID.COMBAT_DAMAGE : null;
		
		// Damage to attacker
		if (attackerUnit != null)
		{
			attackerUnit.setDamageTaken (getAttackerDamageTaken ());
			if (getUnitCalculations ().calculateAliveFigureCount (attackerUnit, getClient ().getPlayers (),
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ()) <= 0)
			{
				// Attacker is dead
				log.debug ("ApplyDamage is killing off dead attacker Unit URN " + getAttackerUnitURN ());
				getUnitClientUtils ().killUnit (attackerUnit, transmittedAction, untransmittedAction);
				
				if (attackerUnit.getOwningPlayerID () == getClient ().getOurPlayerID ())
				{
					getArmyListUI ().refreshArmyList ((MapCoordinates3DEx) attackerUnit.getUnitLocation ());

					if (getClient ().getClientDB ().findUnit (attackerUnit.getUnitID (), "ApplyDamageMessageImpl").getUnitMagicRealm ().equals
						(CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
							
						getHeroItemsUI ().refreshHeroes ();
					
				}
			}
			else
			{
				final UnitInfoUI attackerUI = getClient ().getUnitInfos ().get (getAttackerUnitURN ());
				if (attackerUI != null)
					attackerUI.getUnitInfoPanel ().showUnit (attackerUI.getUnitInfoPanel ().getUnit ());
				
				for (final HideableComponent<SelectUnitButton> button : getOverlandMapRightHandPanel ().getSelectUnitButtons ())
					if ((!button.isHidden ()) && (button.getComponent ().getUnit () == attackerUnit))
						button.repaint ();

				for (final CityViewUI cityView : getClient ().getCityViews ().values ())
					for (final SelectUnitButton button : cityView.getSelectUnitButtons ())
						if (button.getUnit () == attackerUnit)
							button.repaint ();
			}
		}
		
		// Damage to defender(s)
		for (final ApplyDamageMessageDefenderDetails thisUnit : getDefenderUnits ())
		{
			// Apply regular damage
			thisUnit.getDefUnit ().setDamageTaken (thisUnit.getDefenderDamageTakenEnd ());
			
			// Apply special effects
			for (final DamageTypeID damageType : getSpecialDamageType ())
				switch (damageType)
				{
					case ZEROES_AMMO:
						thisUnit.getDefUnit ().setRangedAttackAmmo (0);
						break;
						
					default:
						break;
				}
			
			if (getUnitCalculations ().calculateAliveFigureCount (thisUnit.getDefUnit (), getClient ().getPlayers (),
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ()) <= 0)
			{
				// Defender is dead
				log.debug ("ApplyDamage is killing off dead defender Unit URN " + thisUnit.getDefUnit ().getUnitURN ());
				getUnitClientUtils ().killUnit (thisUnit.getDefUnit (), transmittedAction, untransmittedAction);
				
				if (thisUnit.getDefUnit ().getOwningPlayerID () == getClient ().getOurPlayerID ())
				{
					getArmyListUI ().refreshArmyList ((MapCoordinates3DEx) thisUnit.getDefUnit ().getUnitLocation ());

					if (getClient ().getClientDB ().findUnit (thisUnit.getDefUnit ().getUnitID (), "ApplyDamageMessageImpl").getUnitMagicRealm ().equals
						(CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
								
						getHeroItemsUI ().refreshHeroes ();
				}
			}
			else
			{
				final UnitInfoUI defenderUI = getClient ().getUnitInfos ().get (thisUnit.getDefUnit ().getUnitURN ());
				if (defenderUI != null)
					defenderUI.getUnitInfoPanel ().showUnit (defenderUI.getUnitInfoPanel ().getUnit ());

				for (final HideableComponent<SelectUnitButton> button : getOverlandMapRightHandPanel ().getSelectUnitButtons ())
					if ((!button.isHidden ()) && (button.getComponent ().getUnit () == defenderUnit))
						button.repaint ();

				for (final CityViewUI cityView : getClient ().getCityViews ().values ())
					for (final SelectUnitButton button : cityView.getSelectUnitButtons ())
						if (button.getUnit () == defenderUnit)
							button.repaint ();
			}
		}
		
		// Jump to the next unit to move
		if ((animated) && (attackerUnit != null))
		{
			// Update remaining movement
			attackerUnit.setDoubleCombatMovesLeft (getAttackerDoubleCombatMovesLeft ());

			// Jump to the next unit to move, unless we're a unit who still has some movement left.
			// This routine will then ignore the request if we're not the current player.
			if (attackerUnit.getDoubleCombatMovesLeft () <= 0)
				getCombatMapProcessing ().removeUnitFromLeftToMoveCombat (attackerUnit);
			
			getCombatMapProcessing ().selectNextUnitToMoveCombat ();
		}
		
		getCombatUI ().setAttackAnim (null);
		getCombatUI ().setCombatCastAnimation (null);
		getCombatUI ().getCombatCastAnimationPositions ().clear ();
		
		log.trace ("Exiting finish");
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
	 * @return Client-side unit utils
	 */
	public final UnitClientUtils getUnitClientUtils ()
	{
		return unitClientUtils;
	}

	/**
	 * @param util Client-side unit utils
	 */
	public final void setUnitClientUtils (final UnitClientUtils util)
	{
		unitClientUtils = util;
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
	 * @return Bitmap generator includes routines for calculating pixel coords
	 */
	public final CombatMapBitmapGenerator getCombatMapBitmapGenerator ()
	{
		return combatMapBitmapGenerator;
	}

	/**
	 * @param gen Bitmap generator includes routines for calculating pixel coords
	 */
	public final void setCombatMapBitmapGenerator (final CombatMapBitmapGenerator gen)
	{
		combatMapBitmapGenerator = gen;
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
	 * @return Army list
	 */
	public final ArmyListUI getArmyListUI ()
	{
		return armyListUI;
	}

	/**
	 * @param ui Army list
	 */
	public final void setArmyListUI (final ArmyListUI ui)
	{
		armyListUI = ui;
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
	 * @return The attacking unit; null if we can't see it
	 */
	public final MemoryUnit getAttackerUnit ()
	{
		return attackerUnit;
	}
	
	/**
	 * @return The defending unit; null if we can't see it
	 */
	public final List<ApplyDamageMessageDefenderDetails> getDefenderUnits ()
	{
		return defenderUnits;
	}

	/**
	 * @return Current coordinates of each individual missile making a ranged attack, in pixels; null if still showing the initial portion of the firing animation
	 */
	public final int [] [] getCurrent ()
	{
		return current;
	}

	/**
	 * @return Current image of ranged attack
	 */
	public final RangedAttackTypeCombatImageGfx getRatCurrentImage ()
	{
		return ratCurrentImage;
	}
	
	/**
	 * Stores additional details about each defender unit
	 */
	private class ApplyDamageMessageDefenderDetails
	{
		/** Which unit this is */
		private final MemoryUnit defUnit;

		/** How much damage the unit had taken before this attack; we use this to animate the damage slowly being applied */
		private final int defenderDamageTakenStart;

		/** How much damage the unit is taking in total from the attack */
		private final int defenderDamageTakenEnd;
		
		/**
		 * @param aDefenderUnit Which unit this is
		 * @param aDefenderDamageTakenEnd How much damage the unit is taking in total from the attack
		 */
		private ApplyDamageMessageDefenderDetails (final MemoryUnit aDefenderUnit, final int aDefenderDamageTakenEnd)
		{
			defUnit = aDefenderUnit;
			defenderDamageTakenStart = defUnit.getDamageTaken ();
			defenderDamageTakenEnd = aDefenderDamageTakenEnd;
		}

		/**
		 * @return Which unit this is
		 */
		public final MemoryUnit getDefUnit ()
		{
			return defUnit;
		}

		/**
		 * @return How much damage the unit had taken before this attack; we use this to animate the damage slowly being applied
		 */
		public final int getDefenderDamageTakenStart ()
		{
			return defenderDamageTakenStart;
		}

		/**
		 * @return How much damage the unit is taking in total from the attack
		 */
		public final int getDefenderDamageTakenEnd ()
		{
			return defenderDamageTakenEnd;
		}
	}
}