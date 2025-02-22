package momime.client.messages.process;

import java.io.IOException;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.base.client.AnimatedServerToClientMessage;

import momime.client.MomClient;
import momime.client.audio.MomAudioPlayer;
import momime.client.calculations.CombatMapBitmapGenerator;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.ui.dialogs.MiniCityViewUI;
import momime.client.ui.dialogs.OverlandEnchantmentsUI;
import momime.client.ui.frames.CityViewUI;
import momime.client.ui.frames.CombatUI;
import momime.client.ui.frames.CombatUICastAnimation;
import momime.client.ui.frames.NewTurnMessagesUI;
import momime.client.ui.frames.OverlandMapUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.client.ui.frames.UnitInfoUI;
import momime.client.ui.frames.WizardsUI;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.common.calculations.CityCalculations;
import momime.common.database.AnimationEx;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.TileSetEx;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.servertoclient.AddOrUpdateMaintainedSpellMessage;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.KindOfSpell;
import momime.common.utils.KindOfSpellUtils;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.UnitUtils;

/**
 * Server sends this to notify clients of new maintained spells cast, or those that have newly come into view
 */
public final class AddOrUpdateMaintainedSpellMessageImpl extends AddOrUpdateMaintainedSpellMessage implements AnimatedServerToClientMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (AddOrUpdateMaintainedSpellMessageImpl.class);

	/** Multiplayer client */
	private MomClient client;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** Overland map right hand panel showing economy etc */
	private OverlandMapRightHandPanel overlandMapRightHandPanel;
	
	/** New turn messages UI */
	private NewTurnMessagesUI newTurnMessagesUI;
	
	/** Combat UI */
	private CombatUI combatUI;
	
	/** Overland map UI */
	private OverlandMapUI overlandMapUI;
	
	/** Bitmap generator includes routines for calculating pixel coords */
	private CombatMapBitmapGenerator combatMapBitmapGenerator;

	/** Sound effects player */
	private MomAudioPlayer soundPlayer;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** City calculations */
	private CityCalculations cityCalculations;
	
	/** MemoryGridCell utils */
	private MemoryGridCellUtils memoryGridCellUtils;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Wizards UI */
	private WizardsUI wizardsUI;
	
	/** Kind of spell utils */
	private KindOfSpellUtils kindOfSpellUtils;

	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/** True for city enchantments/curses and overland enchantments */
	private boolean animatedByOtherFrame;
	
	/** Animation to display; null to process message instantly, or if animation is being handled by another frame */
	private AnimationEx anim;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		// Some types of important spells show their castings as animations that are displayed in a different window;
		// in that case the animations, and even adding the spell itself, are handled by those other windows instead.
		final Spell spell = getClient ().getClientDB ().findSpell (getMaintainedSpell ().getSpellID (), "AddMaintainedSpellMessageImpl");
		final KindOfSpell kind = getKindOfSpellUtils ().determineKindOfSpell (spell, null);
		animatedByOtherFrame = false;

		// Overland enchantments are always animated
		anim = null;
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
				getOverlandMapRightHandPanel ().getTargetSpell ().setTargetedCity ((MapCoordinates3DEx) getMaintainedSpell ().getCityLocation ());
				
				// Redraw the NTMs
				getNewTurnMessagesUI ().languageChanged ();
			}
			
			// If we cast it OR its our city, then display a popup window for it, as long as it isn't in combat
			final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
				(getMaintainedSpell ().getCityLocation ().getZ ()).getRow ().get (getMaintainedSpell ().getCityLocation ().getY ()).getCell ().get (getMaintainedSpell ().getCityLocation ().getX ()).getCityData ();
			
			if ((!getMaintainedSpell ().isCastInCombat ()) &&
				((getMaintainedSpell ().getCastingPlayerID () == getClient ().getOurPlayerID ()) ||
				((cityData != null) && (cityData.getCityOwnerID () == getClient ().getOurPlayerID ()))))
			{
				animatedByOtherFrame = true;
				
				final MiniCityViewUI miniCityView = getPrototypeFrameCreator ().createMiniCityView ();
				miniCityView.setCityLocation ((MapCoordinates3DEx) getMaintainedSpell ().getCityLocation ());
				miniCityView.setRenderCityData (getCityCalculations ().buildRenderCityData ((MapCoordinates3DEx) getMaintainedSpell ().getCityLocation (),
					getClient ().getSessionDescription ().getOverlandMapSize (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ()));						
				miniCityView.setAddSpellMessage (this);
				miniCityView.setVisible (true);
			}
		}
		
		else if ((spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_ENCHANTMENTS) || (spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_CURSES) ||
			(spell.getSpellBookSectionID () == SpellBookSectionID.SPECIAL_UNIT_SPELLS) ||
			(((kind == KindOfSpell.RAISE_DEAD) || (kind == KindOfSpell.ATTACK_UNITS) || (kind == KindOfSpell.ATTACK_UNITS_AND_BUILDINGS)) &
				(!getMaintainedSpell ().isCastInCombat ())))
		{
			// If we cast it, then update the entry on the NTM scroll that's telling us to choose a target for it
			if ((getMaintainedSpell ().getCastingPlayerID () == getClient ().getOurPlayerID ()) && (getOverlandMapRightHandPanel ().getTargetSpell () != null) &&
				(getOverlandMapRightHandPanel ().getTargetSpell ().getSpellID ().equals (getMaintainedSpell ().getSpellID ())))
			{
				getOverlandMapRightHandPanel ().getTargetSpell ().setTargetedUnitURN (getMaintainedSpell ().getUnitURN ());
				getOverlandMapRightHandPanel ().getTargetSpell ().setTargetedCity ((MapCoordinates3DEx) getMaintainedSpell ().getCityLocation ());
				
				// Redraw the NTMs
				getNewTurnMessagesUI ().languageChanged ();
			}
			
			// Is there an animation to display for it?
			if (((getMaintainedSpell ().getUnitURN () != null) || (getMaintainedSpell ().getCityLocation () != null)) && (isNewlyCast ()))
			{
				if (spell.getCombatCastAnimation () != null)
				{
					final MemoryUnit spellTargetUnit = (getMaintainedSpell ().getUnitURN () != null) ? getUnitUtils ().findUnitURN (getMaintainedSpell ().getUnitURN (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "AddMaintainedSpellMessageImpl") : null;
					final MapCoordinates3DEx spellTargetLocation = (MapCoordinates3DEx)
						((spellTargetUnit != null) ? spellTargetUnit.getUnitLocation () : getMaintainedSpell ().getCityLocation ());
					
					anim = getClient ().getClientDB ().findAnimation (spell.getCombatCastAnimation (), "AddMaintainedSpellMessageImpl");
	
					if (getMaintainedSpell ().isCastInCombat ())
					{
						if ((getCombatUI ().isVisible ()) && (spellTargetUnit.getCombatPosition () != null))
						{
							// Show anim on CombatUI
							final TileSetEx combatMapTileSet = getClient ().getClientDB ().findTileSet (GraphicsDatabaseConstants.TILE_SET_COMBAT_MAP, "AddMaintainedSpellMessageImpl");
							
							final int adjustX = (anim.getCombatCastOffsetX () == null) ? 0 : 2 * anim.getCombatCastOffsetX ();
							final int adjustY = (anim.getCombatCastOffsetY () == null) ? 0 : 2 * anim.getCombatCastOffsetY ();
							
							final CombatUICastAnimation castAnim = new CombatUICastAnimation ();
							castAnim.setPositionX (adjustX + getCombatMapBitmapGenerator ().combatCoordinatesX
								(spellTargetUnit.getCombatPosition ().getX (), spellTargetUnit.getCombatPosition ().getY (), combatMapTileSet));
							castAnim.setPositionY (adjustY + getCombatMapBitmapGenerator ().combatCoordinatesY
								(spellTargetUnit.getCombatPosition ().getX (), spellTargetUnit.getCombatPosition ().getY (), combatMapTileSet));
							castAnim.setAnim (anim);
							castAnim.setFrameCount (anim.getFrame ().size ());
							castAnim.setInFront (true);
							
							getCombatUI ().getCombatCastAnimations ().add (castAnim);
						}
						else
							anim = null;
					}
					else if (spellTargetLocation != null)
					{
						// Show anim on OverlandMapUI - is the unit in a tower?
						final MemoryGridCell mc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
							(spellTargetLocation.getZ ()).getRow ().get (spellTargetLocation.getY ()).getCell ().get
							(spellTargetLocation.getX ());
						
						if (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (mc.getTerrainData ()))
							getOverlandMapUI ().setOverlandCastAnimationPlane (null);
						else
							getOverlandMapUI ().setOverlandCastAnimationPlane (spellTargetLocation.getZ ());
						
						final TileSetEx overlandMapTileSet = getClient ().getClientDB ().findTileSet (CommonDatabaseConstants.TILE_SET_OVERLAND_MAP, "AddMaintainedSpellMessageImpl.init");
	
						final int adjustX = (anim.getOverlandCastOffsetX () == null) ? 0 : anim.getOverlandCastOffsetX ();
						final int adjustY = (anim.getOverlandCastOffsetY () == null) ? 0 : anim.getOverlandCastOffsetY ();
	
						getOverlandMapUI ().setOverlandCastAnimationX (adjustX + (overlandMapTileSet.getTileWidth () * spellTargetLocation.getX ()));
						getOverlandMapUI ().setOverlandCastAnimationY (adjustY + (overlandMapTileSet.getTileHeight () * spellTargetLocation.getY ()));
						
						getOverlandMapUI ().setOverlandCastAnimationFrame (0);
						getOverlandMapUI ().setOverlandCastAnimation (anim);
						getOverlandMapUI ().repaintSceneryPanel ();
					}
				}
				
				// See if there's a sound effect defined in the graphics XML file
				if (spell.getSpellSoundFile () != null)
					try
					{
						getSoundPlayer ().playAudioFile (spell.getSpellSoundFile ());
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
			}
		}
		
		else if ((spell.getSpellBookSectionID () == SpellBookSectionID.SPECIAL_OVERLAND_SPELLS) ||
			(spell.getSpellBookSectionID () == SpellBookSectionID.DISPEL_SPELLS) || (spell.getSpellBookSectionID () == SpellBookSectionID.SUMMONING))
		{
			// If we cast it, then update the entry on the NTM scroll that's telling us to choose a target for it
			// Only summoning spell that comes here is Floating Island
			if ((getMaintainedSpell ().getCastingPlayerID () == getClient ().getOurPlayerID ()) && (getOverlandMapRightHandPanel ().getTargetSpell () != null) &&
				(getOverlandMapRightHandPanel ().getTargetSpell ().getSpellID ().equals (getMaintainedSpell ().getSpellID ())))
			{
				if (getMaintainedSpell ().getCityLocation () != null)
					getOverlandMapRightHandPanel ().getTargetSpell ().setTargetedCity ((MapCoordinates3DEx) getMaintainedSpell ().getCityLocation ());
				else
					// Just stick a value in there to stop asking about targeting disjunction spells
					getOverlandMapRightHandPanel ().getTargetSpell ().setTargetedCity (new MapCoordinates3DEx (-1, -1, -1));
				
				// Redraw the NTMs
				getNewTurnMessagesUI ().languageChanged ();
			}
			
			// Is there an animation to display for it?
			if ((spell.getCombatCastAnimation () != null) && (getMaintainedSpell ().getCityLocation () != null) && (isNewlyCast ()))
			{
				anim = getClient ().getClientDB ().findAnimation (spell.getCombatCastAnimation (), "AddMaintainedSpellMessageImpl");

				if (!getMaintainedSpell ().isCastInCombat ())
				{
					// Show anim on OverlandMapUI
					final TileSetEx overlandMapTileSet = getClient ().getClientDB ().findTileSet (CommonDatabaseConstants.TILE_SET_OVERLAND_MAP, "AddMaintainedSpellMessageImpl.init");

					final int adjustX = (anim.getOverlandCastOffsetX () == null) ? 0 : anim.getOverlandCastOffsetX ();
					final int adjustY = (anim.getOverlandCastOffsetY () == null) ? 0 : anim.getOverlandCastOffsetY ();

					getOverlandMapUI ().setOverlandCastAnimationX (adjustX + (overlandMapTileSet.getTileWidth () * getMaintainedSpell ().getCityLocation ().getX ()));
					getOverlandMapUI ().setOverlandCastAnimationY (adjustY + (overlandMapTileSet.getTileHeight () * getMaintainedSpell ().getCityLocation ().getY ()));
					
					getOverlandMapUI ().setOverlandCastAnimationFrame (0);
					getOverlandMapUI ().setOverlandCastAnimation (anim);
					getOverlandMapUI ().repaintSceneryPanel ();
				}
			
				// See if there's a sound effect defined in the graphics XML file
				if (spell.getSpellSoundFile () != null)
					try
					{
						getSoundPlayer ().playAudioFile (spell.getSpellSoundFile ());
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
			}
		}
		
		else if (spell.getSpellBookSectionID () == SpellBookSectionID.ENEMY_WIZARD_SPELLS)
		{
			// If we cast it, then update the entry on the NTM scroll that's telling us to choose a target for it
			if ((getMaintainedSpell ().getCastingPlayerID () == getClient ().getOurPlayerID ()) && (getOverlandMapRightHandPanel ().getTargetSpell () != null) &&
				(getOverlandMapRightHandPanel ().getTargetSpell ().getSpellID ().equals (getMaintainedSpell ().getSpellID ())))
			{
				// Just stick a value in there to stop asking about targeting wizard spells
				if (getMaintainedSpell ().getTargetPlayerID () != null)
					getOverlandMapRightHandPanel ().getTargetSpell ().setTargetedPlayerID (getMaintainedSpell ().getTargetPlayerID ());
				else
					getOverlandMapRightHandPanel ().getTargetSpell ().setTargetedCity (new MapCoordinates3DEx (-1, -1, -1));
				
				// Redraw the NTMs
				getNewTurnMessagesUI ().languageChanged ();
			}

			// Is there an animation to display for it?
			if ((spell.getCombatCastAnimation () != null) && (getMaintainedSpell ().getTargetPlayerID () != null) && (isNewlyCast ()))
			{
				anim = getClient ().getClientDB ().findAnimation (spell.getCombatCastAnimation (), "AddMaintainedSpellMessageImpl");
				
				// Show anim on WizardsUI
				getWizardsUI ().setVisible (true);
				getWizardsUI ().setWizardCastAnimationPlayerID (getMaintainedSpell ().getTargetPlayerID ());
				getWizardsUI ().setWizardCastAnimationFrame (0);
				getWizardsUI ().setWizardCastAnimation (anim);
				
				// See if there's a sound effect defined in the graphics XML file
				if (spell.getSpellSoundFile () != null)
					try
					{
						getSoundPlayer ().playAudioFile (spell.getSpellSoundFile ());
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
			}
		}
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
		if (getMaintainedSpell ().getTargetPlayerID () != null)
		{
			getWizardsUI ().setWizardCastAnimationFrame (tickNumber - 1);
			getWizardsUI ().repaintWizards ();
		}
		else if (getMaintainedSpell ().isCastInCombat ())
		{
			for (final CombatUICastAnimation castAnim : getCombatUI ().getCombatCastAnimations ())
				castAnim.setFrameNumber (tickNumber - 1);
		}
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
		if (!isSpellTransient ())
		{
			final MemoryMaintainedSpell oldSpell = getMemoryMaintainedSpellUtils ().findSpellURN
				(getMaintainedSpell ().getSpellURN (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell ());
			if (oldSpell != null)
			{
				oldSpell.setCastingPlayerID (getMaintainedSpell ().getCastingPlayerID ());
				oldSpell.setVariableDamage (getMaintainedSpell ().getVariableDamage ());
			}
			else
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell ().add (getMaintainedSpell ());
		}
		
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
					ui.getUnitInfoPanel ().refreshUnitDetails ();
				
				// If its being cast on a combat unit, need to check if we now need to display an animation over the unit's head to show the new effect, e.g. Confusion
				if (getMaintainedSpell ().isCastInCombat ())
				{
					final MemoryUnit u = getUnitUtils ().findUnitURN (getMaintainedSpell ().getUnitURN (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "AddMaintainedSpellMessageImpl.processOneUpdate (C)");
					
					// We might be witnessing the combat from an adjacent tile so can see the spell being cast, but not know the unit's exact location if we're not directly involved
					if (u.getCombatPosition () != null)
					{
						final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (u, null, null, null,
							getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());
						
						getCombatUI ().setUnitToDrawAtLocation (u.getCombatPosition ().getX (), u.getCombatPosition ().getY (), xu);
					}
				}
			}
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
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
			if (getMaintainedSpell ().getTargetPlayerID () != null)
			{
				getWizardsUI ().setWizardCastAnimation (null);
				getWizardsUI ().repaintWizards ();
			}
			else if (getMaintainedSpell ().isCastInCombat ())
				getCombatUI ().getCombatCastAnimations ().clear ();
			else
			{
				getOverlandMapUI ().setOverlandCastAnimation (null);
				getOverlandMapUI ().repaintSceneryPanel ();
			}
		}

		// If animation is being processed by another frame, then that other frame is also responsible for actually processing whatever update takes place (e.g. adding the spell).
		// If we're responsible for it, then process it now the anim completed playing (if there was one).
		if (!animatedByOtherFrame)
			processOneUpdate ();
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
	 * @return City calculations
	 */
	public final CityCalculations getCityCalculations ()
	{
		return cityCalculations;
	}

	/**
	 * @param calc City calculations
	 */
	public final void setCityCalculations (final CityCalculations calc)
	{
		cityCalculations = calc;
	}

	/**
	 * @return MemoryGridCell utils
	 */
	public final MemoryGridCellUtils getMemoryGridCellUtils ()
	{
		return memoryGridCellUtils;
	}

	/**
	 * @param utils MemoryGridCell utils
	 */
	public final void setMemoryGridCellUtils (final MemoryGridCellUtils utils)
	{
		memoryGridCellUtils = utils;
	}

	/**
	 * @return MemoryMaintainedSpell utils
	 */
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param spellUtils MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils spellUtils)
	{
		memoryMaintainedSpellUtils = spellUtils;
	}

	/**
	 * @return Wizards UI
	 */
	public final WizardsUI getWizardsUI ()
	{
		return wizardsUI;
	}

	/**
	 * @param ui Wizards UI
	 */
	public final void setWizardsUI (final WizardsUI ui)
	{
		wizardsUI = ui;
	}

	/**
	 * @return Kind of spell utils
	 */
	public final KindOfSpellUtils getKindOfSpellUtils ()
	{
		return kindOfSpellUtils;
	}

	/**
	 * @param k Kind of spell utils
	 */
	public final void setKindOfSpellUtils (final KindOfSpellUtils k)
	{
		kindOfSpellUtils = k;
	}

	/**
	 * @return expandUnitDetails method
	 */
	public final ExpandUnitDetails getExpandUnitDetails ()
	{
		return expandUnitDetails;
	}

	/**
	 * @param e expandUnitDetails method
	 */
	public final void setExpandUnitDetails (final ExpandUnitDetails e)
	{
		expandUnitDetails = e;
	}
}