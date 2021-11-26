package momime.client.utils;

import java.io.IOException;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.ui.dialogs.VariableManaUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.client.ui.renderer.CastCombatSpellFrom;
import momime.common.MomException;
import momime.common.calculations.UnitCalculations;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomCombatTile;
import momime.common.messages.clienttoserver.RequestCastSpellMessage;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.KindOfSpell;
import momime.common.utils.KindOfSpellUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.SampleUnitUtils;
import momime.common.utils.TargetSpellResult;
import momime.common.utils.UnitUtils;

/**
 * Handlers for mouse move and mouse click events in CombatUI relating to targeting spells
 */
public final class CombatSpellClientUtilsImpl implements CombatSpellClientUtils
{
	/** Multiplayer client */
	private MomClient client;

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Variable MP popup */
	private VariableManaUI variableManaUI;	
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;

	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Client-side spell utils */
	private SpellClientUtils spellClientUtils;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Kind of spell utils */
	private KindOfSpellUtils kindOfSpellUtils;
	
	/** Sample unit method */
	private SampleUnitUtils sampleUnitUtils;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/**
	 * Handles doing final validation and building up the request message to cast a combat spell when the player clicks a tile in the combat map.
	 * So this won't be called for combat spells that don't require any targeting, e.g. combat enchantments like Prayer.
	 * 
	 * @param spell The spell being cast
	 * @param combatLocation Where the combat is taking place
	 * @param combatCoords The tile within the combat map where the player wants to target the spell
	 * @param castingSource Source that is currently casting a combat spell
	 * @param combatTerrain Combat terrain
	 * @param unitBeingRaised If casting a raise dead spell, which unit the player chose to raise
	 * @return Message to send to server to request spell cast if it is valid; if it is not valid for some reason then returns null
	 * @throws IOException If there is a problem
	 */
	@Override
	public final RequestCastSpellMessage buildCastCombatSpellMessage (final Spell spell, final MapCoordinates3DEx combatLocation,
		final MapCoordinates2DEx combatCoords, final CastCombatSpellFrom castingSource, final MapAreaOfCombatTiles combatTerrain,
		final MemoryUnit unitBeingRaised) throws IOException
	{
		final KindOfSpell kind = getKindOfSpellUtils ().determineKindOfSpell (spell, null);
		
		// Is there a unit in the clicked cell?
		final ExpandedUnitDetails xu = getUnitUtils ().findAliveUnitInCombatWeCanSeeAt
			(combatLocation, combatCoords, getClient ().getOurPlayerID (), getClient ().getPlayers (),
			 getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB (),
			 getClient ().getSessionDescription ().getCombatMapSize (),
			 spell.getSpellBookSectionID () == SpellBookSectionID.DISPEL_SPELLS);
		
		// Build message
		final RequestCastSpellMessage msg = new RequestCastSpellMessage ();
		msg.setSpellID (spell.getSpellID ());
		msg.setCombatLocation (combatLocation);
		if ((castingSource != null) && (castingSource.getCastingUnit () != null))
		{
			msg.setCombatCastingUnitURN (castingSource.getCastingUnit ().getUnitURN ());
			msg.setCombatCastingFixedSpellNumber (castingSource.getFixedSpellNumber ());
			msg.setCombatCastingSlotNumber (castingSource.getHeroItemSlotNumber ());
		}
		
		// Does the spell have varying cost?  Ignore this if its being cast from a hero item or a fixed spell
		if ((spell.getCombatMaxDamage () != null) &&
			(msg.getCombatCastingSlotNumber () == null) && (msg.getCombatCastingFixedSpellNumber () == null))
			msg.setVariableDamage (getVariableManaUI ().getVariableDamage ());
					
		boolean isValidTarget = false;
		switch (spell.getSpellBookSectionID ())
		{
			// Summoning spell - valid as long as there isn't a unit here
			case SUMMONING:
				if (xu == null)
				{
					// What unit are we going to summon?
					final ExpandedUnitDetails summonUnit;
					if (kind == KindOfSpell.RAISE_DEAD)
						summonUnit = getExpandUnitDetails ().expandUnitDetails (unitBeingRaised, null, null, null,
							getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());
					else
						summonUnit = getSampleUnitUtils ().createSampleUnit (spell.getSummonedUnit ().get (0), getClient ().getOurPlayerID (), null,
							getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());
					
					final MomCombatTile tile = combatTerrain.getRow ().get (combatCoords.getY ()).getCell ().get (combatCoords.getX ());
					
					if (getUnitCalculations ().calculateDoubleMovementToEnterCombatTile (summonUnit, tile, getClient ().getClientDB ()) >= 0)
					{
						isValidTarget = true;
						msg.setCombatTargetLocation (combatCoords);
						
						// Resurrecting an existing unit?
						if (spell.getResurrectedHealthPercentage () != null)
							msg.setCombatTargetUnitURN (unitBeingRaised.getUnitURN ());
					}
				}
				break;

			// Unit enchantment / curse - separate method to perform all validation that this unit is a valid target
			case UNIT_ENCHANTMENTS:
			case UNIT_CURSES:
			case ATTACK_SPELLS:
			case SPECIAL_UNIT_SPELLS:
			case DISPEL_SPELLS:
				if (xu == null)
				{
					// Cracks call can also be aimed at walls
					if ((spell.getSpellBookSectionID () == SpellBookSectionID.ATTACK_SPELLS) &&
						(spell.getSpellValidBorderTarget ().size () > 0) &&
						(getMemoryMaintainedSpellUtils ().isCombatLocationValidTargetForSpell (spell, combatCoords, combatTerrain)))
					{
						msg.setCombatTargetLocation (combatCoords);
						isValidTarget = true;
					}
				}
				else
				{
					final ExpandedUnitDetails xus = getExpandUnitDetails ().expandUnitDetails (xu.getUnit (), null, null, spell.getSpellRealm (),
						getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());
					
					final Integer variableDamage;
					if ((spell.getCombatMaxDamage () != null) &&
						(castingSource.getHeroItemSlotNumber () == null) &&		// Can't put additional power into spells imbued into items
						(castingSource.getFixedSpellNumber () == null))				// or casting fixed spells like Magicians' Fireball spell
						
						variableDamage = getVariableManaUI ().getVariableDamage ();
					else
						variableDamage = null;
					
					TargetSpellResult validTarget = getMemoryMaintainedSpellUtils ().isUnitValidTargetForSpell
						(spell, null, combatLocation, getClient ().getOurPlayerID (), castingSource.getCastingUnit (), variableDamage,
						 xus, true, getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (),
						 getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWar (), getClient ().getPlayers (), getClient ().getClientDB ());
					
					if (validTarget == TargetSpellResult.VALID_TARGET)
					{
						isValidTarget = true;
						msg.setCombatTargetUnitURN (xu.getUnitURN ());
					}
					
					// Cracks call can also be aimed at walls even if the unit is flying
					else if ((spell.getSpellBookSectionID () == SpellBookSectionID.ATTACK_SPELLS) &&
						(spell.getSpellValidBorderTarget ().size () > 0) &&
						(getMemoryMaintainedSpellUtils ().isCombatLocationValidTargetForSpell (spell, combatCoords, combatTerrain)))
					{
						msg.setCombatTargetLocation (combatCoords);
						isValidTarget = true;
						validTarget = TargetSpellResult.VALID_TARGET;		// Just so we don't display error below
					}

					// If we can't target on this unit, tell the player why not
					if (validTarget != TargetSpellResult.VALID_TARGET)
					{
						final String spellName = getLanguageHolder ().findDescription
							(getClient ().getClientDB ().findSpell (spell.getSpellID (), "CombatUI").getSpellName ());
						
						String text = getLanguageHolder ().findDescription (getLanguages ().getSpellTargeting ().getUnitLanguageText (validTarget)).replaceAll
							("SPELL_NAME", (spellName != null) ? spellName : spell.getSpellID ());
						
						// If spell can only be targeted on specific magic realm/lifeform types, the list them
						if (validTarget == TargetSpellResult.UNIT_INVALID_MAGIC_REALM_LIFEFORM_TYPE)
							text = text + getSpellClientUtils ().listValidMagicRealmLifeformTypeTargetsOfSpell (spell);
						
						final MessageBoxUI msgBox = getPrototypeFrameCreator ().createMessageBox ();
						msgBox.setLanguageTitle (getLanguages ().getSpellTargeting ().getTitle ());
						msgBox.setText (text);
						msgBox.setVisible (true);
					}
				}
				break;
				
			// Combat spells targeted at a location have their own method too
			case SPECIAL_COMBAT_SPELLS:
				if (getMemoryMaintainedSpellUtils ().isCombatLocationValidTargetForSpell (spell, combatCoords, combatTerrain))
				{
					msg.setCombatTargetLocation (combatCoords);
					isValidTarget = true;
				}
				break;
				
			default:
				throw new MomException ("CombatUI doesn't know targeting rules (clicking) for spells from section " + spell.getSpellBookSectionID ());
		}
		
		return isValidTarget ? msg : null;
	}
	
	/**
	 * As player is moving the mouse around the combat terrain, test whether the tile they are holding the mouse over at the moment
	 * is a vaild place to cast the spell or not.
	 * 
	 * Basically this is exactly the same logic as in buildCastCombatSpellMessage, except that we aren't building up the message.
	 * 
	 * @param spell The spell being cast
	 * @param combatLocation Where the combat is taking place
	 * @param combatCoords The tile within the combat map where the player wants to target the spell
	 * @param castingSource Source that is currently casting a combat spell
	 * @param combatTerrain Combat terrain
	 * @param unitBeingRaised If casting a raise dead spell, which unit the player chose to raise
	 * @return Whether the desired target tile is a valid place to cast the spell or not
	 * @throws IOException If there is a problem
	 */
	@Override
	public final boolean isCombatTileValidTargetForSpell (final Spell spell, final MapCoordinates3DEx combatLocation,
		final MapCoordinates2DEx combatCoords, final CastCombatSpellFrom castingSource, final MapAreaOfCombatTiles combatTerrain,
		final MemoryUnit unitBeingRaised) throws IOException
	{
		// This was doing exactly the same logic as buildCastCombatSpellMessage just without building up the message.
		// Rather than repeating it all, just use the same method and test the result
		return (buildCastCombatSpellMessage (spell, combatLocation, combatCoords, castingSource, combatTerrain, unitBeingRaised) != null);
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
	 * @return Variable MP popup
	 */
	public VariableManaUI getVariableManaUI ()
	{
		return variableManaUI;
	}

	/**
	 * @param ui Variable MP popup
	 */
	public final void setVariableManaUI (final VariableManaUI ui)
	{
		variableManaUI = ui;
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

	/**
	 * @return Client-side spell utils
	 */
	public final SpellClientUtils getSpellClientUtils ()
	{
		return spellClientUtils;
	}

	/**
	 * @param utils Client-side spell utils
	 */
	public final void setSpellClientUtils (final SpellClientUtils utils)
	{
		spellClientUtils = utils;
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
	 * @return New singular language XML
	 */
	public final MomLanguagesEx getLanguages ()
	{
		return getLanguageHolder ().getLanguages ();
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
	 * @return Sample unit method
	 */
	public final SampleUnitUtils getSampleUnitUtils ()
	{
		return sampleUnitUtils;
	}

	/**
	 * @param s Sample unit method
	 */
	public final void setSampleUnitUtils (final SampleUnitUtils s)
	{
		sampleUnitUtils = s;
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
}