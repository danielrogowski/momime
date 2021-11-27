package momime.server.ai;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.WeightedChoices;

import momime.common.MomException;
import momime.common.database.AttackSpellTargetID;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.SampleUnitUtils;
import momime.common.utils.TargetSpellResult;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.process.SpellQueueing;
import momime.server.utils.CombatMapServerUtils;
import momime.server.utils.UnitServerUtils;

/**
 * Methods relating to casting spells in combat
 */
public final class CombatSpellAIImpl implements CombatSpellAI
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (CombatSpellAIImpl.class);
	
	/** Memory CAE utils */
	private MemoryCombatAreaEffectUtils memoryCombatAreaEffectUtils;
	
	/** Methods dealing with combat maps that are only needed on the server */
	private CombatMapServerUtils combatMapServerUtils;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;
	
	/** Spell queueing methods */
	private SpellQueueing spellQueueing;
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/** Sample unit method */
	private SampleUnitUtils sampleUnitUtils;
	
	/**
	 * Checks whether casting the specified spell in combat is valid, e.g. does it have a valid target, and lists out all possible choices for casting it (if any).
	 * 
	 * @param player AI player who is considering casting a spell
	 * @param spell Spell they are considering casting
	 * @param combatLocation Combat location
	 * @param combatCastingUnit Unit who is going to cast the spell; null = the wizard
	 * @param combatCastingFixedSpellNumber For casting fixed spells the unit knows (e.g. Giant Spiders casting web), indicates the spell number; for other types of casting this is null
	 * @param combatCastingSlotNumber For casting spells imbued into hero items, this is the number of the slot (0, 1 or 2); for other types of casting this is null
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param choices List of choices to add to
	 * @throws PlayerNotFoundException If we can't find one of the players
	 * @throws RecordNotFoundException If we find the spell they're trying to cast, or other expected game elements
	 * @throws MomException If there are any issues with data or calculation logic
	 */
	@Override
	public final void listChoicesForSpell (final PlayerServerDetails player, final Spell spell, final MapCoordinates3DEx combatLocation,
		final ExpandedUnitDetails combatCastingUnit, final Integer combatCastingFixedSpellNumber, final Integer combatCastingSlotNumber,
		final MomSessionVariables mom, final WeightedChoices<CombatAISpellChoice> choices)
		throws PlayerNotFoundException, RecordNotFoundException, MomException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		
		// Validation for certain spell book sections
		boolean valid = true;
		switch (spell.getSpellBookSectionID ())
		{
			// Only add combat enchantments that aren't already there
			case COMBAT_ENCHANTMENTS:
				if (getMemoryCombatAreaEffectUtils ().listCombatEffectsNotYetCastAtLocation (mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (),
					spell, player.getPlayerDescription ().getPlayerID (), combatLocation).size () == 0)
					
					valid = false;
				break;
				
			// Only allow Wall of Fire/Darkness/Stone if we don't already have it
			case CITY_ENCHANTMENTS:
				valid = (getMemoryMaintainedSpellUtils ().isCityValidTargetForSpell (mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
					spell, player.getPlayerDescription ().getPlayerID (), combatLocation, mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
					priv.getFogOfWar (), mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (), mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET);
				break;
				
			// Can't summon (including raise dead-type spells) if already max number of units
			case SUMMONING:
				if ((!mom.getSessionDescription ().getUnitSetting ().isCanExceedMaximumUnitsDuringCombat ()) &&
					(getCombatMapServerUtils ().countPlayersAliveUnitsAtCombatLocation (player.getPlayerDescription ().getPlayerID (), combatLocation,
						mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), mom.getServerDB ()) >= CommonDatabaseConstants.MAX_UNITS_PER_MAP_CELL))
					
					valid = false;
				break;
				
			// Earth to Mud and Disrupt Wall aren't useful enough to bother teaching the AI there to target them
			case SPECIAL_COMBAT_SPELLS:
				valid = false;
				break;
				
			default:
		}
		
		if (valid)
		{
			// Do we need to pick a target?  Or if spell hits multiple targets, prove there is at least one?
			if (spell.getSpellBookSectionID () == SpellBookSectionID.COMBAT_ENCHANTMENTS)
			{
				log.debug ("AI player " + player.getPlayerDescription ().getPlayerID () + " considering casting combat enchantment " + spell.getSpellID () + " in combat which requires no unit checks");
				final CombatAISpellChoice choice = new CombatAISpellChoice (spell, null, null, null, combatCastingFixedSpellNumber, combatCastingSlotNumber);
				choices.add (choice.getWeighting (), choice);
			}
			else if ((spell.getSpellBookSectionID () == SpellBookSectionID.SUMMONING) && (spell.getSummonedUnit ().size () > 0))
			{
				// Pick where to summon it - because maybe won't find anywhere
				final ExpandedUnitDetails summonedUnit = getSampleUnitUtils ().createSampleUnit (spell.getSummonedUnit ().get (0), player.getPlayerDescription ().getPlayerID (), null,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
				
				final ServerGridCellEx gc = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
					(combatLocation.getZ ()).getRow ().get (combatLocation.getY ()).getCell ().get (combatLocation.getX ());
				
				final MapCoordinates2DEx summoningLocation = getUnitServerUtils ().findFreeCombatPositionClosestTo (summonedUnit, combatLocation, gc.getCombatMap (),
					new MapCoordinates2DEx (mom.getSessionDescription ().getCombatMapSize ().getWidth () / 2, mom.getSessionDescription ().getCombatMapSize ().getHeight () / 2),
					player.getPlayerDescription ().getPlayerID (), mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (),
					mom.getServerDB (), mom.getSessionDescription ().getCombatMapSize ());
				
				if (summoningLocation != null)
				{
					log.debug ("AI player " + player.getPlayerDescription ().getPlayerID () + " considering casting summoning spell " + spell.getSpellID () + " in combat at cell " + summoningLocation);
					final CombatAISpellChoice choice = new CombatAISpellChoice (spell, null, summoningLocation, null, combatCastingFixedSpellNumber, combatCastingSlotNumber);
					choices.add (choice.getWeighting (), choice);
				}
			}
			else
			{
				// Every other kind of spell requires either to be targetted on a specific unit, or at least for spells that hit all units (e.g. Flame Strike or Mass Healing)
				// that there are some appropriate targets for the spell to act on.  First figure out which of those situations it is.
				Integer targetCount;
				if ((spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_ENCHANTMENTS) || (spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_CURSES) ||
					(spell.getSpellBookSectionID () == SpellBookSectionID.SUMMONING) ||
					(((spell.getSpellBookSectionID () == SpellBookSectionID.ATTACK_SPELLS) || (spell.getSpellBookSectionID () == SpellBookSectionID.SPECIAL_UNIT_SPELLS) ||
						(spell.getSpellBookSectionID () == SpellBookSectionID.DISPEL_SPELLS)) && (spell.getAttackSpellCombatTarget () == AttackSpellTargetID.SINGLE_UNIT)))
					
					targetCount = null;		// Targetted at a specific unit, so do not keep a count of targets
				else
					targetCount = 0;		// Targetted at all units, so count how many

				final ServerGridCellEx gc = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
					(combatLocation.getZ ()).getRow ().get (combatLocation.getY ()).getCell ().get (combatLocation.getX ());
				
				for (final MemoryUnit targetUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
				{
					final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (targetUnit, null, null, spell.getSpellRealm (),
						mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());

					if ((getMemoryMaintainedSpellUtils ().isUnitValidTargetForSpell (spell, null, combatLocation, gc.getCombatMap (), player.getPlayerDescription ().getPlayerID (),
						combatCastingUnit, null, xu, true, mom.getGeneralServerKnowledge ().getTrueMap (), priv.getFogOfWar (), mom.getPlayers (),
						mom.getServerDB ()) == TargetSpellResult.VALID_TARGET) &&
							
							(getUnitUtils ().canSeeUnitInCombat (xu, player.getPlayerDescription ().getPlayerID (), mom.getPlayers (),
								mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB (), mom.getSessionDescription ().getCombatMapSize ())))
					{
						if (targetCount == null)
						{
							// If we're trying to raise dead a specific unit, we have to prove there's somewhere valid to bring it back
							if (spell.getSpellBookSectionID () == SpellBookSectionID.SUMMONING)
							{
								final MapCoordinates2DEx summoningLocation = getUnitServerUtils ().findFreeCombatPositionClosestTo (xu, combatLocation, gc.getCombatMap (),
									new MapCoordinates2DEx (mom.getSessionDescription ().getCombatMapSize ().getWidth () / 2, mom.getSessionDescription ().getCombatMapSize ().getHeight () / 2),
									player.getPlayerDescription ().getPlayerID (), mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (),
									mom.getServerDB (), mom.getSessionDescription ().getCombatMapSize ());
								
								if (summoningLocation != null)
								{
									log.debug ("AI player " + player.getPlayerDescription ().getPlayerID () + " considering casting raise dead spell " + spell.getSpellID () + " in combat at Unit URN " + targetUnit.getUnitURN ()  + " at cell " + summoningLocation);
									final CombatAISpellChoice choice = new CombatAISpellChoice (spell, xu, summoningLocation, null, combatCastingFixedSpellNumber, combatCastingSlotNumber);
									choices.add (choice.getWeighting (), choice);
								}
							}
							else
							{
								// Some other spell aimed at a unit with no additional validation to do
								log.debug ("AI player " + player.getPlayerDescription ().getPlayerID () + " considering casting spell " + spell.getSpellID () + " in combat at Unit URN " + targetUnit.getUnitURN ());
								final CombatAISpellChoice choice = new CombatAISpellChoice (spell, xu, null, null, combatCastingFixedSpellNumber, combatCastingSlotNumber);
								choices.add (choice.getWeighting (), choice);
							}
						}
						else
							targetCount++;
					}
				}
				
				// Is it a spell that hits all units, and found some targets?
				if ((targetCount != null) && (targetCount > 0))
				{
					log.debug ("AI player " + player.getPlayerDescription ().getPlayerID () + " considering casting spell " + spell.getSpellID () + " in combat which will hit " + targetCount + " target(s)");
					final CombatAISpellChoice choice = new CombatAISpellChoice (spell, null, null, targetCount, combatCastingFixedSpellNumber, combatCastingSlotNumber);
					choices.add (choice.getWeighting (), choice);
				}
			}
		}
	}
	
	/**
	 * Given a choice of spells and targets the AI can choose from in combat, picks one and casts it
	 * 
	 * @param player AI player who is considering casting a spell
	 * @param combatLocation Combat location
	 * @param choices List of spells and targets to choose from
	 * @param combatCastingUnit Unit who is going to cast the spell; null = the wizard
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether a spell was cast or not
	 * @throws PlayerNotFoundException If we can't find one of the players
	 * @throws RecordNotFoundException If we find the spell they're trying to cast, or other expected game elements
	 * @throws MomException If there are any issues with data or calculation logic
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final CombatAIMovementResult makeCastingChoice (final PlayerServerDetails player, final MapCoordinates3DEx combatLocation,
		final WeightedChoices<CombatAISpellChoice> choices, final ExpandedUnitDetails combatCastingUnit, final MomSessionVariables mom)
		throws PlayerNotFoundException, RecordNotFoundException, MomException, JAXBException, XMLStreamException
	{
		final CombatAISpellChoice choice = choices.nextWeightedValue ();
		final CombatAIMovementResult result;
		if (choice == null)
			result = CombatAIMovementResult.NOTHING;
		else
		{
			log.debug ("AI player " + player.getPlayerDescription ().getPlayerID () + " decided to cast combat spell " + choice.getSpell ().getSpellID () + " (" +
				choice.getSpell ().getSpellName () + ")");
			
			if (getSpellQueueing ().requestCastSpell (player, (combatCastingUnit == null) ? null : combatCastingUnit.getUnitURN (),
				choice.getCombatCastingFixedSpellNumber (), choice.getCombatCastingSlotNumber (),
				choice.getSpell ().getSpellID (), null, combatLocation, choice.getTargetLocation (),
				(choice.getTargetUnit () == null) ? null : choice.getTargetUnit ().getUnitURN (), null, mom))
				
				result = CombatAIMovementResult.ENDED_COMBAT;
			else
				result = CombatAIMovementResult.MOVED_OR_ATTACKED;
		}
		
		return result;
	}

	/**
	 * @return Memory CAE utils
	 */
	public final MemoryCombatAreaEffectUtils getMemoryCombatAreaEffectUtils ()
	{
		return memoryCombatAreaEffectUtils;
	}

	/**
	 * @param utils Memory CAE utils
	 */
	public final void setMemoryCombatAreaEffectUtils (final MemoryCombatAreaEffectUtils utils)
	{
		memoryCombatAreaEffectUtils = utils;
	}

	/**
	 * @return Methods dealing with combat maps that are only needed on the server
	 */
	public final CombatMapServerUtils getCombatMapServerUtils ()
	{
		return combatMapServerUtils;
	}

	/**
	 * @param u Methods dealing with combat maps that are only needed on the server
	 */
	public final void setCombatMapServerUtils (final CombatMapServerUtils u)
	{
		combatMapServerUtils = u;
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
	 * @return MemoryMaintainedSpell utils
	 */
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param spellUtil MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils spellUtil)
	{
		memoryMaintainedSpellUtils = spellUtil;
	}

	/**
	 * @return Server-only unit utils
	 */
	public final UnitServerUtils getUnitServerUtils ()
	{
		return unitServerUtils;
	}

	/**
	 * @param utils Server-only unit utils
	 */
	public final void setUnitServerUtils (final UnitServerUtils utils)
	{
		unitServerUtils = utils;
	}

	/**
	 * @return Spell queueing methods
	 */
	public final SpellQueueing getSpellQueueing ()
	{
		return spellQueueing;
	}

	/**
	 * @param obj Spell queueing methods
	 */
	public final void setSpellQueueing (final SpellQueueing obj)
	{
		spellQueueing = obj;
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
}