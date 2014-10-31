package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.audio.AudioPlayer;
import momime.client.calculations.CombatMapBitmapGenerator;
import momime.client.calculations.MomClientUnitCalculations;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.RangedAttackTypeEx;
import momime.client.graphics.database.TileSetEx;
import momime.client.graphics.database.v0_9_5.RangedAttackTypeActionID;
import momime.client.graphics.database.v0_9_5.RangedAttackTypeCombatImage;
import momime.client.process.CombatMapProcessing;
import momime.client.ui.frames.CombatUI;
import momime.client.utils.AnimationController;
import momime.client.utils.UnitClientUtils;
import momime.common.UntransmittedKillUnitActionID;
import momime.common.calculations.MomUnitCalculations;
import momime.common.database.v0_9_5.Unit;
import momime.common.messages.servertoclient.v0_9_5.ApplyDamageMessage;
import momime.common.messages.servertoclient.v0_9_5.KillUnitActionID;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.utils.UnitUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.AnimatedServerToClientMessage;

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

	/** Multiplayer client */
	private MomClient client;
	
	/** Unit utils */
	private UnitUtils unitUtils;

	/** Unit calculations */
	private MomUnitCalculations unitCalculations;
	
	/** Client-side unit utils */
	private UnitClientUtils unitClientUtils;
	
	/** Combat UI */
	private CombatUI combatUI;
	
	/** Combat map processing */
	private CombatMapProcessing combatMapProcessing;
	
	/** Client unit calculations */
	private MomClientUnitCalculations clientUnitCalculations;

	/** Bitmap generator includes routines for calculating pixel coords */
	private CombatMapBitmapGenerator combatMapBitmapGenerator;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Sound effects player */
	private AudioPlayer soundPlayer;
	
	/** Animation controller */
	private AnimationController anim;
	
	/** The attacking unit; null if we can't see it */
	private MemoryUnit attackerUnit;
	
	/** The defending unit; null if we can't see it */
	private MemoryUnit defenderUnit;
	
	/** How much damage the unit had taken before this attack; we use this to animate the damage slowly being applied */
	private Integer attackerDamageTakenStart;
	
	/** How much damage the unit had taken before this attack; we use this to animate the damage slowly being applied */
	private Integer defenderDamageTakenStart;
	
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
	private RangedAttackTypeCombatImage ratFlyImage;

	/** Image of ranged attack hitting its target */
	private RangedAttackTypeCombatImage ratStrikeImage;
	
	/** Current image of ranged attack */
	private RangedAttackTypeCombatImage ratCurrentImage;
	
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
			if (isRangedAttack ())
				getUnitCalculations ().decreaseRangedAttackAmmo (attackerUnit);
			
			attackerDamageTakenStart = attackerUnit.getDamageTaken ();
			attackerUnit.setCombatHeading (getAttackerDirection ());
		}
		
		// Can we see the defender?
		if (getDefenderUnitURN () != null)
		{
			defenderUnit = getUnitUtils ().findUnitURN (getDefenderUnitURN (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "ApplyDamageMessageImpl-d");
			defenderDamageTakenStart = defenderUnit.getDamageTaken ();
			defenderUnit.setCombatHeading (getDefenderDirection ());
		}
		
		// Is either unit ours?  If so, then it must be damage from the combat we're in; if not, then it must be some combat going on elsewhere
		animated =
			(((attackerUnit != null) && (attackerUnit.getOwningPlayerID () == getClient ().getOurPlayerID ())) ||
			((defenderUnit != null) && (defenderUnit.getOwningPlayerID () == getClient ().getOurPlayerID ())));
		
		// Perform any animation startup necessary
		if (!animated)
		{
			duration = 0;
			tickCount = 0;
		}
		else
		{
			getCombatUI ().setAttackAnim (this);
			
			if (isRangedAttack ())
			{
				// Start a ranged attack animation - firstly, after a brief frame of showing the unit firing, it'll be back to standing still
				// It might however because of the attack now be facing a different direction than before, so possibly a new anim needs to be registered
				final String attackerStandingActionID = getClientUnitCalculations ().determineCombatActionID (attackerUnit, false);
				getUnitClientUtils ().registerUnitFiguresAnimation (attackerUnit.getUnitID (), attackerStandingActionID, attackerUnit.getCombatHeading (), getCombatUI ().getContentPane ());
				
				// To animate the missiles, first we need the locations (in pixels) of the two units involved
				final TileSetEx combatMapTileSet = getGraphicsDB ().findTileSet (GraphicsDatabaseConstants.VALUE_TILE_SET_COMBAT_MAP, "ApplyDamageMessageImpl");
				
				final int startX = getCombatMapBitmapGenerator ().combatCoordinatesX (attackerUnit.getCombatPosition ().getX (), attackerUnit.getCombatPosition ().getY (), combatMapTileSet);
				final int startY = getCombatMapBitmapGenerator ().combatCoordinatesY (attackerUnit.getCombatPosition ().getX (), attackerUnit.getCombatPosition ().getY (), combatMapTileSet);
				endX = getCombatMapBitmapGenerator ().combatCoordinatesX (defenderUnit.getCombatPosition ().getX (), defenderUnit.getCombatPosition ().getY (), combatMapTileSet);
				endY = getCombatMapBitmapGenerator ().combatCoordinatesY (defenderUnit.getCombatPosition ().getX (), defenderUnit.getCombatPosition ().getY (), combatMapTileSet);
				
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
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (),
					getClient ().getClientDB ());
				
				start = getUnitClientUtils ().calcUnitFigurePositions (attackerUnit.getUnitID (), unitTypeID, totalFigureCount, aliveFigureCount, startX, startY);

				// Start animation of the missile; e.g. fireballs don't have a constant image
				final String rangedAttackTypeID = attackerUnitDef.getRangedAttackType ();
				final RangedAttackTypeEx rat = getGraphicsDB ().findRangedAttackType (rangedAttackTypeID, "ApplyDamageMessageImpl");
				ratFlyImage = rat.findCombatImage (RangedAttackTypeActionID.FLY, getAttackerDirection (), "ApplyDamageMessageImpl");
				getAnim ().registerRepaintTrigger (ratFlyImage.getRangedAttackTypeCombatAnimation (), getCombatUI ().getContentPane ());
				
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
			else
			{
				// Start a close combat attack animation
				getUnitClientUtils ().registerUnitFiguresAnimation (attackerUnit.getUnitID (), GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_MELEE_ATTACK, getAttackerDirection (), getCombatUI ().getContentPane ());
				getUnitClientUtils ().playCombatActionSound (attackerUnit, GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_MELEE_ATTACK);
				
				// For close combat attacks, animate the defender as well as the attacker, to show that they are counter-attacking
				getUnitClientUtils ().registerUnitFiguresAnimation (defenderUnit.getUnitID (), GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_MELEE_ATTACK, getDefenderDirection (), getCombatUI ().getContentPane ());

				// For melee attacks is set to 1 second, and melee animations in the graphics XML are set at 6 FPS, so there'll be 6 frames to show
				duration = 1;
				tickCount = 6;
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
		if (isRangedAttack ())
		{
			// Animate a ranged attack
			if (tickNumber >= RANGED_ATTACK_LAUNCH_TICKS)
			{
				final double ratio = Math.min (1.0, (double) tickNumber / (tickCount - RANGED_ATTACK_LAUNCH_TICKS));
				current = new int [start.length] [3];
				for (int n = 0; n < start.length; n++)
				{
					current [n] [0] = start [n] [0] + (int) ((endX - start [n] [0]) * ratio);
					current [n] [1] = start [n] [1] + (int) ((endY - start [n] [1]) * ratio);
					current [n] [2] = start [n] [2];
				}
			}
			
			ratCurrentImage = (tickNumber >= tickCount - RANGED_ATTACK_IMPACT_TICKS) ? ratStrikeImage : ratFlyImage;
		}
		else
		{
			// Animate a close combat attack - gradually ramp up the damage taken by both units.
			// Don't even need to force a repaint, because registering the 'melee' animation will do it for us.
			final double ratio = (double) tickNumber / tickCount;
			attackerUnit.setDamageTaken (attackerDamageTakenStart + (int) ((getAttackerDamageTaken () - attackerDamageTakenStart) * ratio));
			defenderUnit.setDamageTaken (defenderDamageTakenStart + (int) ((getDefenderDamageTaken () - defenderDamageTakenStart) * ratio));
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
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (), getClient ().getClientDB ()) <= 0)
			{
				// Attacker is dead
				log.debug ("ApplyDamage is killing off dead attacker Unit URN " + getAttackerUnitURN ());
				getUnitClientUtils ().killUnit (getAttackerUnitURN (), transmittedAction, untransmittedAction);
			}
		}
		
		// Damage to defender
		if (defenderUnit != null)
		{
			defenderUnit.setDamageTaken (getDefenderDamageTaken ());
			if (getUnitCalculations ().calculateAliveFigureCount (defenderUnit, getClient ().getPlayers (),
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (), getClient ().getClientDB ()) <= 0)
			{
				// Defender is dead
				log.debug ("ApplyDamage is killing off dead defender Unit URN " + getDefenderUnitURN ());
				getUnitClientUtils ().killUnit (getDefenderUnitURN (), transmittedAction, untransmittedAction);
			}
		}
		
		// Jump to the next unit to move
		if (animated)
		{
			if (isRangedAttack ())
			{
				// Stop missile animation
				getAnim ().unregisterRepaintTrigger (ratFlyImage.getRangedAttackTypeCombatAnimation (), getCombatUI ().getContentPane ());
			}
			else
			{
				// End the melee attack animation
				getUnitClientUtils ().unregisterUnitFiguresAnimation (attackerUnit.getUnitID (), GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_MELEE_ATTACK, getAttackerDirection (), getCombatUI ().getContentPane ());				
				getUnitClientUtils ().unregisterUnitFiguresAnimation (defenderUnit.getUnitID (), GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_MELEE_ATTACK, getDefenderDirection (), getCombatUI ().getContentPane ());
				
				// If the units are facing a different direction than before then we might have a new anim to kick off
				final String attackerStandingActionID = getClientUnitCalculations ().determineCombatActionID (attackerUnit, false);
				getUnitClientUtils ().registerUnitFiguresAnimation (attackerUnit.getUnitID (), attackerStandingActionID, attackerUnit.getCombatHeading (), getCombatUI ().getContentPane ());
	
				final String defenderStandingActionID = getClientUnitCalculations ().determineCombatActionID (defenderUnit, false);
				getUnitClientUtils ().registerUnitFiguresAnimation (defenderUnit.getUnitID (), defenderStandingActionID, defenderUnit.getCombatHeading (), getCombatUI ().getContentPane ());
			}
			
			// Update remaining movement
			attackerUnit.setDoubleCombatMovesLeft (getAttackerDoubleCombatMovesLeft ());

			// Jump to the next unit to move, unless we're a unit who still has some movement left.
			// This routine will then ignore the request if we're not the current player.
			if (attackerUnit.getDoubleCombatMovesLeft () <= 0)
				getCombatMapProcessing ().removeUnitFromLeftToMoveCombat (attackerUnit);
			
			getCombatMapProcessing ().selectNextUnitToMoveCombat ();
		}
		
		getCombatUI ().setAttackAnim (null);
		
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
	 * @return The attacking unit; null if we can't see it
	 */
	public final MemoryUnit getAttackerUnit ()
	{
		return attackerUnit;
	}
	
	/**
	 * @return The defending unit; null if we can't see it
	 */
	public final MemoryUnit getDefenderUnit ()
	{
		return defenderUnit;
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
	public final RangedAttackTypeCombatImage getRatCurrentImage ()
	{
		return ratCurrentImage;
	}
}