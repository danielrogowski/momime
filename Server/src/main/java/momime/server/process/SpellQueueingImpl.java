package momime.server.process;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.calculations.SpellCalculations;
import momime.common.calculations.UnitCalculations;
import momime.common.database.AttackSpellCombatTargetID;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.SpellBookSectionID;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;
import momime.common.messages.servertoclient.OverlandCastQueuedMessage;
import momime.common.messages.servertoclient.RemoveQueuedSpellMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.messages.servertoclient.UpdateManaSpentOnCastingCurrentSpellMessage;
import momime.common.utils.CombatMapUtils;
import momime.common.utils.CombatPlayers;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.SpellCastType;
import momime.common.utils.SpellUtils;
import momime.common.utils.TargetSpellResult;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.SpellSvr;
import momime.server.knowledge.MomGeneralServerKnowledgeEx;
import momime.server.knowledge.ServerGridCellEx;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Methods for validating spell requests and deciding whether to queue them up or cast immediately.
 * Once they're actually ready to cast, this is handled by the SpellProcessing interface.  I split these up so that the unit
 * tests dealing with validating and queueing don't have to invoke the real castOverlandNow/castCombatNow methods.
 */
public final class SpellQueueingImpl implements SpellQueueing
{
	/** Class logger */
	private final Log log = LogFactory.getLog (SpellQueueingImpl.class);

	/** Spell utils */
	private SpellUtils spellUtils;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** MemoryGridCell utils */
	private MemoryGridCellUtils memoryGridCellUtils;
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** Methods for dealing with player msgs */
	private PlayerMessageProcessing playerMessageProcessing;

	/** Resource calculations */
	private ServerResourceCalculations serverResourceCalculations;

	/** Combat map utils */
	private CombatMapUtils combatMapUtils;
	
	/** Spell calculations */
	private SpellCalculations spellCalculations;

	/** Spell processing */
	private SpellProcessing spellProcessing;

	/** Memory CAE utils */
	private MemoryCombatAreaEffectUtils memoryCombatAreaEffectUtils;
	
	/**
	 * Client wants to cast a spell, either overland or in combat
	 * We may not actually be able to cast it yet - big overland spells take a number of turns to channel, so this
	 * routine does all the checks to see if it can be instantly cast or needs to be queued up and cast over multiple turns
	 * 
	 * @param player Player who is casting the spell
	 * @param spellID Which spell they want to cast
	 * @param combatLocation Location of the combat where this spell is being cast; null = being cast overland
	 * @param combatTargetLocation Which specific tile of the combat map the spell is being cast at, for cell-targetted spells like combat summons
	 * @param combatTargetUnitURN Which specific unit within combat the spell is being cast at, for unit-targetted spells like Fire Bolt
	 * @param variableDamage Chosen damage selected for the spell, for spells like fire bolt where a varying amount of mana can be channeled into the spell
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 * @throws RecordNotFoundException If we find the spell they're trying to cast, or other expected game elements
	 * @throws MomException If there are any issues with data or calculation logic
	 */
	@Override
	public final void requestCastSpell (final PlayerServerDetails player, final String spellID,
		final MapCoordinates3DEx combatLocation, final MapCoordinates2DEx combatTargetLocation, final Integer combatTargetUnitURN,
		final Integer variableDamage, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, PlayerNotFoundException, RecordNotFoundException, MomException
	{
		log.trace ("Entering requestCastSpell: Player ID " + player.getPlayerDescription ().getPlayerID () + ", " + spellID);
		
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();
		
		// Find the spell in the player's search list
		final SpellSvr spell = mom.getServerDB ().findSpell (spellID, "requestCastSpell");
		final SpellResearchStatus researchStatus = getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), spellID);
		
		// Do validation checks
		String msg;
		if (researchStatus.getStatus () != SpellResearchStatusID.AVAILABLE)
			msg = "You don't have that spell researched and/or available so can't cast it.";
		
		else if ((combatLocation == null) && (!getSpellUtils ().spellCanBeCastIn (spell, SpellCastType.OVERLAND)))
			msg = "That spell cannot be cast overland.";
		
		else if ((combatLocation != null) && (!getSpellUtils ().spellCanBeCastIn (spell, SpellCastType.COMBAT)))
			msg = "That spell cannot be cast in combat.";

		else if ((combatLocation == null) && ((combatTargetLocation != null) || (combatTargetUnitURN != null)))
			msg = "Cannot specify a target when casting an overland spell.";

		else if ((combatLocation != null) && (combatTargetUnitURN == null) &&
			((spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_ENCHANTMENTS) ||
			 (spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_CURSES)))
			msg = "You must specify a unit target when casting unit enchantments or curses in combat.";

		else if ((combatLocation != null) && (combatTargetLocation == null) &&
			(spell.getSpellBookSectionID () == SpellBookSectionID.SUMMONING))
			msg = "You must specify a target location when casting summoning spells in combat.";
		
		else
			msg = null;
		
		// Casting cost is only relevant for validation if we're casting in combat - if overland,
		// it can be as expensive spell as we like, its still valid, it'll just take ages to cast it
		int reducedCombatCastingCost = Integer.MAX_VALUE;
		int multipliedManaCost = Integer.MAX_VALUE;
		MemoryUnit combatTargetUnit = null;
		CombatPlayers combatPlayers = null;
		if ((msg == null) && (combatLocation != null))
		{
			// First need to know if we're the attacker or defender
			combatPlayers = getCombatMapUtils ().determinePlayersInCombatFromLocation
				(combatLocation, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), mom.getPlayers ());
			
			final ServerGridCellEx gc = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
				(combatLocation.getZ ()).getRow ().get (combatLocation.getY ()).getCell ().get (combatLocation.getX ());
			
			if (!combatPlayers.bothFound ())
				msg = "You cannot cast combat spells if one side has been wiped out in the combat.";

			else if ((gc.isSpellCastThisCombatTurn () != null) && (gc.isSpellCastThisCombatTurn ()))
				msg = "You have already cast a spell this combat turn.";
			
			else
			{
				// Work out unmodified casting cost
				final int unmodifiedCombatCastingCost = (variableDamage == null) ? spell.getCombatCastingCost () :
					spell.getCombatCastingCost () + ((variableDamage - spell.getCombatBaseDamage ()) * spell.getCombatManaPerAdditionalDamagePoint ());
				
				// Apply books/retorts that make spell cheaper to cast
				reducedCombatCastingCost = getSpellUtils ().getReducedCastingCost
					(spell, unmodifiedCombatCastingCost, pub.getPick (), mom.getSessionDescription ().getSpellSetting (), mom.getServerDB ());
				
				// What our remaining skill?
				final int ourSkill;
				if (player == combatPlayers.getAttackingPlayer ())
					ourSkill = gc.getCombatAttackerCastingSkillRemaining ();
				else if (player == combatPlayers.getDefendingPlayer ())
					ourSkill = gc.getCombatDefenderCastingSkillRemaining ();
				else
				{
					ourSkill = -1;
					msg = "You tried to cast a combat spell in a combat that you are not participating in.";
				}
				
				// Check range penalty
				if (msg == null)
				{
					final Integer doubleRangePenalty = getSpellCalculations ().calculateDoubleCombatCastingRangePenalty (player, combatLocation,
						getMemoryGridCellUtils ().isTerrainTowerOfWizardry (gc.getTerrainData ()),
						mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (),
						mom.getSessionDescription ().getOverlandMapSize ());
				
					if (doubleRangePenalty == null)
						msg = "You cannot cast combat spells while you are banished.";
					else
						multipliedManaCost = (reducedCombatCastingCost * doubleRangePenalty + 1) / 2;
				}
				
				// Casting skill/MP checks
				if (msg != null)
				{
					// Do nothing - more serious message already generated
				}
				else if (reducedCombatCastingCost > ourSkill)
					msg = "You don't have enough casting skill remaining to cast that spell in combat.";
				else if (multipliedManaCost > getResourceValueUtils ().findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA))
					msg = "You don't have enough mana remaining to cast that spell in combat at this range.";
			}
			
			// Validation for specific types of combat spells
			if (msg != null)
			{
				// Do nothing - more serious message already generated
			}
			else if ((spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_ENCHANTMENTS) ||
				(spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_CURSES) ||
				((spell.getSpellBookSectionID () == SpellBookSectionID.ATTACK_SPELLS) && (spell.getAttackSpellCombatTarget () == AttackSpellCombatTargetID.SINGLE_UNIT)))
			{
				// (Note overland spells tend to have a lot less validation since we don't pick targets until they've completed casting - so the checks are done then)
				// Verify that the chosen unit is a valid target for unit enchantments/curses (we checked above that a unit has chosen)
				combatTargetUnit = getUnitUtils ().findUnitURN (combatTargetUnitURN, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ());
				if (combatTargetUnit == null)
					msg = "Cannot find the unit you are trying to target the spell on";
				else
				{
					final TargetSpellResult validTarget = getMemoryMaintainedSpellUtils ().isUnitValidTargetForSpell
						(spell, combatLocation, player.getPlayerDescription ().getPlayerID (), variableDamage, combatTargetUnit,
						mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
						mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), mom.getServerDB ());
					
					if (validTarget != TargetSpellResult.VALID_TARGET)
					{
						// Using the enum name isn't that great, but the client will already have
						// performed this validation so should never see any message generated here anyway
						msg = "This unit is not a valid target for this spell for reason " + validTarget;
					}
				}
			}
			else if ((spell.getSpellBookSectionID () == SpellBookSectionID.CITY_ENCHANTMENTS) || (spell.getSpellBookSectionID () == SpellBookSectionID.CITY_CURSES))
			{
				// (Note overland spells tend to have a lot less validation since we don't pick targets until they've completed casting - so the checks are done then)
				// Verify that the city the combat is being played at is a valid target for city enchantments/curses
				final TargetSpellResult validTarget = getMemoryMaintainedSpellUtils ().isCityValidTargetForSpell
					(mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
					spell, player.getPlayerDescription ().getPlayerID (), combatLocation,
					mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (), mom.getServerDB ());
				
				if (validTarget != TargetSpellResult.VALID_TARGET)
				{
					// Using the enum name isn't that great, but the client will already have
					// performed this validation so should never see any message generated here anyway
					msg = "This city spell cannot be cast in this combat location for reason " + validTarget;
				}
			}
			else if (spell.getSpellBookSectionID () == SpellBookSectionID.SUMMONING)
			{
				// Verify for summoning spells that there isn't a unit in that location
				if (getUnitUtils ().findAliveUnitInCombatAt (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), combatLocation, combatTargetLocation) != null)
					msg = "There is already a unit in the chosen location so you cannot summon there.";
				
				else if ((!mom.getSessionDescription ().getUnitSetting ().isCanExceedMaximumUnitsDuringCombat ()) &&
					(getCombatMapUtils ().countPlayersAliveUnitsAtCombatLocation (player.getPlayerDescription ().getPlayerID (), combatLocation,
						mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ()) >= mom.getSessionDescription ().getUnitSetting ().getUnitsPerMapCell ()))
					
					msg = "You already have the maximum number of units in combat so cannot summon any more.";
				
				// Make sure the combat location is passable, i.e. can't summon on top of city wall corners
				else if (getUnitCalculations ().calculateDoubleMovementToEnterCombatTile
					(gc.getCombatMap ().getRow ().get (combatTargetLocation.getY ()).getCell ().get (combatTargetLocation.getX ()), mom.getServerDB ()) < 0)
					
					msg = "The terrain at your chosen location is impassable so you cannot summon a unit there.";						
			}
			else if (spell.getSpellBookSectionID () == SpellBookSectionID.COMBAT_ENCHANTMENTS)
			{
				// Check we haven't already cast this enchantment already
				final List<String> combatAreaEffectIDs = getMemoryCombatAreaEffectUtils ().listCombatEffectsNotYetCastAtLocation (mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (),
					spell, player.getPlayerDescription ().getPlayerID (), combatLocation);
				if (combatAreaEffectIDs == null)
					msg = "This combat enchantment spell has no possible combat area effect IDs defined";
				else if (combatAreaEffectIDs.size () == 0)
					msg = "You have already cast all possible effects of this combat enchantment";
			}
		}
		
		// Ok to go ahead and cast (or queue) it?
		if (msg != null)
		{
			log.warn (player.getPlayerDescription ().getPlayerName () + " disallowed from casting spell " + spellID + ": " + msg);
			
			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (msg);
			player.getConnection ().sendMessageToClient (reply);
		}
		else if (combatLocation != null)
		{
			// Cast combat spell
			// Always cast instantly
			// If its a spell where we need to choose a target and/or additional mana, the client will already have done so
			getSpellProcessing ().castCombatNow (player, spell, reducedCombatCastingCost, multipliedManaCost, variableDamage, combatLocation,
				(PlayerServerDetails) combatPlayers.getDefendingPlayer (), (PlayerServerDetails) combatPlayers.getAttackingPlayer (),
				combatTargetUnit, combatTargetLocation, mom);
		}
		else
		{
			// Overland spell - need to see if we can instant cast it or need to queue it up
			final int reducedCastingCost = getSpellUtils ().getReducedOverlandCastingCost (spell, pub.getPick (),
				mom.getSessionDescription ().getSpellSetting (), mom.getServerDB ());
			
			if ((priv.getQueuedSpellID ().size () == 0) && (Math.min (trans.getOverlandCastingSkillRemainingThisTurn (),
				getResourceValueUtils ().findAmountStoredForProductionType (priv.getResourceValue (),
					CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)) >= reducedCastingCost))
			{
				// Cast instantly, and show the casting message instantly too
				getSpellProcessing ().castOverlandNow (mom.getGeneralServerKnowledge (), player, spell, mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ());
				getPlayerMessageProcessing ().sendNewTurnMessages (null, mom.getPlayers (), null);
				
				// Charge player the skill/mana
				trans.setOverlandCastingSkillRemainingThisTurn (trans.getOverlandCastingSkillRemainingThisTurn () - reducedCastingCost);
				getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA,
					-reducedCastingCost);
				
				// Recalc their production values, to send them their reduced skill/mana, but also the spell just cast might have had some maintenance
				getServerResourceCalculations ().recalculateGlobalProductionValues
					(player.getPlayerDescription ().getPlayerID (), false, mom);
			}
			else
			{
				// Queue it on server
				priv.getQueuedSpellID ().add (spellID);
				
				// Queue it on client
				final OverlandCastQueuedMessage reply = new OverlandCastQueuedMessage ();
				reply.setSpellID (spellID);
				player.getConnection ().sendMessageToClient (reply);
			}
		}
		
		log.trace ("Exiting requestCastSpell");
	}

	/**
	 * Spends any skill/mana the player has left towards casting queued spells
	 *
	 * @param gsk Server knowledge structure
	 * @param player Player whose casting to progress
	 * @param players List of players in the session
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @return True if we cast at least one spell
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final boolean progressOverlandCasting (final MomGeneralServerKnowledgeEx gsk, final PlayerServerDetails player, final List<PlayerServerDetails> players,
		final MomSessionDescription sd, final ServerDatabaseEx db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		log.trace ("Entering progressOverlandCasting: Player ID " + player.getPlayerDescription ().getPlayerID ());

		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();

		// Keep going while this player has spells queued, free mana and free skill
		boolean anySpellsCast = false;
		int manaRemaining = getResourceValueUtils ().findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
		while ((priv.getQueuedSpellID ().size () > 0) && (trans.getOverlandCastingSkillRemainingThisTurn () > 0) && (manaRemaining > 0))
		{
			// How much to put towards this spell?
			final SpellSvr spell = db.findSpell (priv.getQueuedSpellID ().get (0), "progressOverlandCasting");
			final int reducedCastingCost = getSpellUtils ().getReducedOverlandCastingCost (spell, pub.getPick (), sd.getSpellSetting (), db);
			final int leftToCast = Math.max (0, reducedCastingCost - priv.getManaSpentOnCastingCurrentSpell ());
			final int manaAmount = Math.min (Math.min (trans.getOverlandCastingSkillRemainingThisTurn (), manaRemaining), leftToCast);

			// Put this amount towards the spell
			trans.setOverlandCastingSkillRemainingThisTurn (trans.getOverlandCastingSkillRemainingThisTurn () - manaAmount);
			priv.setManaSpentOnCastingCurrentSpell (priv.getManaSpentOnCastingCurrentSpell () + manaAmount);
			getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, -manaAmount);
			manaRemaining = manaRemaining - manaAmount;

			// Finished?
			if (priv.getManaSpentOnCastingCurrentSpell () >= reducedCastingCost)
			{
				// Remove queued spell on server
				priv.getQueuedSpellID ().remove (0);
				priv.setManaSpentOnCastingCurrentSpell (0);

				// Remove queued spell on client
				final RemoveQueuedSpellMessage msg = new RemoveQueuedSpellMessage ();
				msg.setQueuedSpellIndex (0);
				player.getConnection ().sendMessageToClient (msg);

				// Cast it
				getSpellProcessing ().castOverlandNow (gsk, player, spell, players, db, sd);
				anySpellsCast = true;
			}

			// Update mana spent so far on client (or set to 0 if finished)
			// Maybe this should be moved out?  If we cast multiple queued spells, do we really have to keep sending 0's?
			// Surely the client only cares on the mana spent on casting ones that we don't remove from its queue
			final UpdateManaSpentOnCastingCurrentSpellMessage msg = new UpdateManaSpentOnCastingCurrentSpellMessage ();
			msg.setManaSpentOnCastingCurrentSpell (priv.getManaSpentOnCastingCurrentSpell ());
			player.getConnection ().sendMessageToClient (msg);

			// No need to tell client how much skill they've got left or mana stored since this is the end of the turn and both will be sent next start phase
		}

		log.trace ("Exiting progressOverlandCasting = " + anySpellsCast);
		return anySpellsCast;
	}

	/**
	 * @return Spell utils
	 */
	public final SpellUtils getSpellUtils ()
	{
		return spellUtils;
	}

	/**
	 * @param utils Spell utils
	 */
	public final void setSpellUtils (final SpellUtils utils)
	{
		spellUtils = utils;
	}

	/**
	 * @return MemoryMaintainedSpell utils
	 */
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param utils MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils utils)
	{
		memoryMaintainedSpellUtils = utils;
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
	 * @return Resource value utils
	 */
	public final ResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
	}

	/**
	 * @param utils Resource value utils
	 */
	public final void setResourceValueUtils (final ResourceValueUtils utils)
	{
		resourceValueUtils = utils;
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
	 * @return Methods for dealing with player msgs
	 */
	public PlayerMessageProcessing getPlayerMessageProcessing ()
	{
		return playerMessageProcessing;
	}

	/**
	 * @param obj Methods for dealing with player msgs
	 */
	public final void setPlayerMessageProcessing (final PlayerMessageProcessing obj)
	{
		playerMessageProcessing = obj;
	}

	/**
	 * @return Resource calculations
	 */
	public final ServerResourceCalculations getServerResourceCalculations ()
	{
		return serverResourceCalculations;
	}

	/**
	 * @param calc Resource calculations
	 */
	public final void setServerResourceCalculations (final ServerResourceCalculations calc)
	{
		serverResourceCalculations = calc;
	}

	/**
	 * @return Combat map utils
	 */
	public final CombatMapUtils getCombatMapUtils ()
	{
		return combatMapUtils;
	}
	
	/**
	 * @param utils Combat map utils
	 */
	public final void setCombatMapUtils (final CombatMapUtils utils)
	{
		combatMapUtils = utils;
	}

	/**
	 * @return Spell calculations
	 */
	public final SpellCalculations getSpellCalculations ()
	{
		return spellCalculations;
	}

	/**
	 * @param calc Spell calculations
	 */
	public final void setSpellCalculations (final SpellCalculations calc)
	{
		spellCalculations = calc;
	}

	/**
	 * @return Spell processing
	 */
	public final SpellProcessing getSpellProcessing ()
	{
		return spellProcessing;
	}

	/**
	 * @param proc Spell processing
	 */
	public final void setSpellProcessing (final SpellProcessing proc)
	{
		spellProcessing = proc;
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
}