package momime.client.messages.process;

import java.awt.Point;
import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.audio.AudioPlayer;
import momime.client.calculations.CombatMapBitmapGenerator;
import momime.client.graphics.database.AnimationGfx;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.SpellGfx;
import momime.client.graphics.database.TileSetGfx;
import momime.client.ui.dialogs.MiniCityViewUI;
import momime.client.ui.dialogs.OverlandEnchantmentsUI;
import momime.client.ui.frames.CityViewUI;
import momime.client.ui.frames.CombatUI;
import momime.client.ui.frames.NewTurnMessagesUI;
import momime.client.ui.frames.OverlandMapUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.client.ui.frames.UnitInfoUI;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.messages.MemoryUnit;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.servertoclient.AddMaintainedSpellMessage;
import momime.common.utils.UnitUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.base.client.AnimatedServerToClientMessage;

/**
 * Server sends this to notify clients of new maintained spells cast, or those that have newly come into view
 */
public final class AddMaintainedSpellMessageImpl extends AddMaintainedSpellMessage implements AnimatedServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (AddMaintainedSpellMessageImpl.class);

	/** Multiplayer client */
	private MomClient client;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** Overland map right hand panel showing economy etc */
	private OverlandMapRightHandPanel overlandMapRightHandPanel;
	
	/** New turn messages UI */
	private NewTurnMessagesUI newTurnMessagesUI;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Combat UI */
	private CombatUI combatUI;
	
	/** Overland map UI */
	private OverlandMapUI overlandMapUI;
	
	/** Bitmap generator includes routines for calculating pixel coords */
	private CombatMapBitmapGenerator combatMapBitmapGenerator;

	/** Sound effects player */
	private AudioPlayer soundPlayer;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** True for city enchantments/curses and overland enchantments */
	private boolean animatedByOtherFrame;
	
	/** Animation to display; null to process message instantly, or if animation is being handled by another frame */
	private AnimationGfx anim;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start: " + getMaintainedSpell ().getSpellID ());
		
		// Some types of important spells show their castings as animations that are displayed in a different window;
		// in that case the animations, and even adding the spell itself, are handled by those other windows instead.
		final Spell spell = getClient ().getClientDB ().findSpell (getMaintainedSpell ().getSpellID (), "AddMaintainedSpellMessageImpl");
		animatedByOtherFrame = false;

		// Overland enchantments are always animated
		if (spell.getSpellBookSectionID () == SpellBookSectionID.OVERLAND_ENCHANTMENTS)
		{
			animatedByOtherFrame = true;
			
			final OverlandEnchantmentsUI overlandEnchantmentsPopup = getPrototypeFrameCreator ().createOverlandEnchantments ();
			overlandEnchantmentsPopup.setAddSpellMessage (this);
			overlandEnchantmentsPopup.setVisible (true);
		}
		
		else if ((spell.getSpellBookSectionID () == SpellBookSectionID.CITY_ENCHANTMENTS) || (spell.getSpellBookSectionID () == SpellBookSectionID.CITY_CURSES))
		{
			// If we cast it, then update the entry on the NTM scroll that's telling us to choose a target for it
			if ((getMaintainedSpell ().getCastingPlayerID () == getClient ().getOurPlayerID ()) && (getOverlandMapRightHandPanel ().getTargetSpell () != null) &&
				(getOverlandMapRightHandPanel ().getTargetSpell ().getSpellID ().equals (getMaintainedSpell ().getSpellID ())))
			{
				getOverlandMapRightHandPanel ().getTargetSpell ().setTargettedCity ((MapCoordinates3DEx) getMaintainedSpell ().getCityLocation ());
				
				// Redraw the NTMs
				getNewTurnMessagesUI ().languageChanged ();
			}
			
			// If we cast it OR its our city, then display a popup window for it, as long as it isn't in combat
			final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
				(getMaintainedSpell ().getCityLocation ().getZ ()).getRow ().get (getMaintainedSpell ().getCityLocation ().getY ()).getCell ().get (getMaintainedSpell ().getCityLocation ().getX ()).getCityData ();
			
			if ((!getMaintainedSpell ().isCastInCombat ()) &&
				((getMaintainedSpell ().getCastingPlayerID () == getClient ().getOurPlayerID ()) ||
				((cityData != null) && (cityData.getCityOwnerID () != null) && (cityData.getCityOwnerID ().equals (getClient ().getOurPlayerID ())))))
			{
				animatedByOtherFrame = true;
				
				final MiniCityViewUI miniCityView = getPrototypeFrameCreator ().createMiniCityView ();
				miniCityView.setCityLocation ((MapCoordinates3DEx) getMaintainedSpell ().getCityLocation ());
				miniCityView.setAddSpellMessage (this);
				miniCityView.setVisible (true);
			}
		}
		
		else if ((spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_ENCHANTMENTS) || (spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_CURSES))
		{
			// If we cast it, then update the entry on the NTM scroll that's telling us to choose a target for it
			if ((getMaintainedSpell ().getCastingPlayerID () == getClient ().getOurPlayerID ()) && (getOverlandMapRightHandPanel ().getTargetSpell () != null) &&
				(getOverlandMapRightHandPanel ().getTargetSpell ().getSpellID ().equals (getMaintainedSpell ().getSpellID ())))
			{
				getOverlandMapRightHandPanel ().getTargetSpell ().setTargettedUnitURN (getMaintainedSpell ().getUnitURN ());
				
				// Redraw the NTMs
				getNewTurnMessagesUI ().languageChanged ();
			}
			
			// Is there an animation to display for it?
			final SpellGfx spellGfx = getGraphicsDB ().findSpell (getMaintainedSpell ().getSpellID (), "AddMaintainedSpellMessageImpl");
			
			anim = null;
			if ((spellGfx.getCombatCastAnimation () != null) && (getMaintainedSpell ().getUnitURN () != null))
			{
				final MemoryUnit spellTargetUnit = getUnitUtils ().findUnitURN (getMaintainedSpell ().getUnitURN (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "AddMaintainedSpellMessageImpl");
				
				anim = getGraphicsDB ().findAnimation (spellGfx.getCombatCastAnimation (), "AddMaintainedSpellMessageImpl");

				if (getMaintainedSpell ().isCastInCombat ())
				{
					// Show anim on CombatUI
					final TileSetGfx combatMapTileSet = getGraphicsDB ().findTileSet (GraphicsDatabaseConstants.TILE_SET_COMBAT_MAP, "AddMaintainedSpellMessageImpl");
					
					final int adjustX = (anim.getCombatCastOffsetX () == null) ? 0 : 2 * anim.getCombatCastOffsetX ();
					final int adjustY = (anim.getCombatCastOffsetY () == null) ? 0 : 2 * anim.getCombatCastOffsetY ();
					
					getCombatUI ().getCombatCastAnimationPositions ().add (new Point (adjustX + getCombatMapBitmapGenerator ().combatCoordinatesX
						(spellTargetUnit.getCombatPosition ().getX (), spellTargetUnit.getCombatPosition ().getY (), combatMapTileSet),
					adjustY + getCombatMapBitmapGenerator ().combatCoordinatesY
						(spellTargetUnit.getCombatPosition ().getX (), spellTargetUnit.getCombatPosition ().getY (), combatMapTileSet)));
	
					getCombatUI ().setCombatCastAnimationFrame (0);
					getCombatUI ().setCombatCastAnimation (anim);
					getCombatUI ().setCombatCastAnimationInFront (true);
				}
				else
				{
					// Show anim on OverlandMapUI
					final TileSetGfx overlandMapTileSet = getGraphicsDB ().findTileSet (GraphicsDatabaseConstants.TILE_SET_OVERLAND_MAP, "AddMaintainedSpellMessageImpl.init");

					final int adjustX = (anim.getOverlandCastOffsetX () == null) ? 0 : anim.getOverlandCastOffsetX ();
					final int adjustY = (anim.getOverlandCastOffsetY () == null) ? 0 : anim.getOverlandCastOffsetY ();

					getOverlandMapUI ().setOverlandCastAnimationX (adjustX + (overlandMapTileSet.getTileWidth () * spellTargetUnit.getUnitLocation ().getX ()));
					getOverlandMapUI ().setOverlandCastAnimationY (adjustY + (overlandMapTileSet.getTileHeight () * spellTargetUnit.getUnitLocation ().getY ()));
					
					getOverlandMapUI ().setOverlandCastAnimationFrame (0);
					getOverlandMapUI ().setOverlandCastAnimation (anim);
				}
			
				// See if there's a sound effect defined in the graphics XML file
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
		
		// If no spell animation, then just add it right away
		if (!animatedByOtherFrame)
			processOneUpdate ();
		
		log.trace ("Exiting start");
	}

	/**
	 * @return Number of ticks that the duration is divided into
	 */
	@Override
	public final int getTickCount ()
	{
		return (anim == null) ? 0 : anim.getFrame ().size ();
	}
	
	/**
	 * @return Number of seconds that the animation takes to display
	 */
	@Override
	public final double getDuration ()
	{
		return (anim == null) ? 0 : (anim.getFrame ().size () / anim.getAnimationSpeed ());
	}
	
	/**
	 * @param tickNumber How many ticks have occurred, from 1..tickCount
	 */
	@Override
	public final void tick (final int tickNumber)
	{
		if (getMaintainedSpell ().isCastInCombat ())
			getCombatUI ().setCombatCastAnimationFrame (tickNumber - 1);
		else
		{
			getOverlandMapUI ().setOverlandCastAnimationFrame (tickNumber - 1);
			getOverlandMapUI ().repaintSceneryPanel ();
		}
	}
	
	/**
	 * @return False for city anims and overland enchantments which have to be clicked on to close their window 
	 */
	@Override
	public final boolean isFinishAfterDuration ()
	{
		return !animatedByOtherFrame;
	}
	
	/**
	 * Method called for each individual update; so called once if message was sent in isolation, or multiple times if part of FogOfWarVisibleAreaChangedMessage
	 */
	public final void processOneUpdate ()
	{
		log.trace ("Entering processOneUpdate: " + getMaintainedSpell ().getSpellID ());
		
		getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell ().add (getMaintainedSpell ());
		
		try
		{
			// If we've got a city screen open showing where the spell was cast, may need to set up animation to display it
			if (getMaintainedSpell ().getCityLocation () != null)
			{
				final CityViewUI cityView = getClient ().getCityViews ().get (getMaintainedSpell ().getCityLocation ().toString ());
				if (cityView != null)
				{
					cityView.cityDataChanged ();
					cityView.spellsChanged ();
				}
			}
	
			// If we've got a unit info display showing for this unit, then show the new spell effect on it
			else if (getMaintainedSpell ().getUnitURN () != null)
			{
				final UnitInfoUI ui = getClient ().getUnitInfos ().get (getMaintainedSpell ().getUnitURN ());
				if (ui != null)
					ui.getUnitInfoPanel ().showUnit (ui.getUnit ());
			}
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
		log.trace ("Exiting processOneUpdate");
	}
	
	/**
	 * Nothing to do here when the message completes, because its all handled in MiniCityViewUI
	 */
	@Override
	public final void finish ()
	{
		// Remove the anim
		if (anim != null)
		{
			if (getMaintainedSpell ().isCastInCombat ())
			{
				getCombatUI ().setCombatCastAnimation (null);
				getCombatUI ().getCombatCastAnimationPositions ().clear ();
			}
			else
				getOverlandMapUI ().setOverlandCastAnimation (null);
		}
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
	 * @return Prototype frame creator
	 */
	public final PrototypeFrameCreator getPrototypeFrameCreator ()
	{
		return prototypeFrameCreator;
	}

	/**
	 * @param obj Prototype frame creator
	 */
	public final void setPrototypeFrameCreator (final PrototypeFrameCreator obj)
	{
		prototypeFrameCreator = obj;
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
	 * @return New turn messages UI
	 */
	public final NewTurnMessagesUI getNewTurnMessagesUI ()
	{
		return newTurnMessagesUI;
	}

	/**
	 * @param ui New turn messages UI
	 */
	public final void setNewTurnMessagesUI (final NewTurnMessagesUI ui)
	{
		newTurnMessagesUI = ui;
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
	 * @return Overland map UI
	 */
	public final OverlandMapUI getOverlandMapUI ()
	{
		return overlandMapUI;
	}

	/**
	 * @param ui Overland map UI
	 */
	public final void setOverlandMapUI (final OverlandMapUI ui)
	{
		overlandMapUI = ui;
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
}