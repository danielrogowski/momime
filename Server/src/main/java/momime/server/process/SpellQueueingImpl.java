package momime.server.process;

import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.calculations.SpellCalculations;
import momime.common.calculations.UnitCalculations;
import momime.common.database.AttackSpellTargetID;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItem;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.UnitCanCast;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.QueuedSpell;
import momime.common.messages.SpellResearchStatusID;
import momime.common.messages.TurnSystem;
import momime.common.messages.UnitStatusID;
import momime.common.messages.WizardState;
import momime.common.messages.servertoclient.AnimationID;
import momime.common.messages.servertoclient.OverlandCastQueuedMessage;
import momime.common.messages.servertoclient.PlayAnimationMessage;
import momime.common.messages.servertoclient.RemoveQueuedSpellMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.messages.servertoclient.UpdateManaSpentOnCastingCurrentSpellMessage;
import momime.common.utils.CombatMapUtils;
import momime.common.utils.CombatPlayers;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
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
import momime.server.knowledge.ServerGridCellEx;
import momime.server.utils.CombatMapServerUtils;
import momime.server.utils.HeroItemServerUtils;

/**
 * Methods for validating spell requests and deciding whether to queue them up or cast immediately.
 * Once they're actually ready to cast, this is handled by the SpellProcessing interface.  I split these up so that the unit
 * tests dealing with validating and queueing don't have to invoke the real castOverlandNow/castCombatNow methods.
 */
public final class SpellQueueingImpl implements SpellQueueing
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (SpellQueueingImpl.class);

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
	
	/** Hero item server utils */
	private HeroItemServerUtils heroItemServerUtils;
	
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;
	
	/** Methods dealing with combat maps that are only needed on the server */
	private CombatMapServerUtils combatMapServerUtils;
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/**
	 * Client wants to cast a spell, either overland or in combat
	 * We may not actually be able to cast it yet - big overland spells take a number of turns to channel, so this
	 * routine does all the checks to see if it can be instantly cast or needs to be queued up and cast over multiple turns
	 * 
	 * @param player Player who is casting the spell
	 * @param combatCastingUnitURN Unit who is casting the spell; null means its the wizard casting, rather than a specific unit
	 * @param combatCastingFixedSpellNumber For casting fixed spells the unit knows (e.g. Giant Spiders casting web), indicates the spell number; for other types of casting this is null
	 * @param combatCastingSlotNumber For casting spells imbued into hero items, this is the number of the slot (0, 1 or 2); for other types of casting this is null
	 * @param spellID Which spell they want to cast
	 * @param heroItem The item being created; null for spells other than Enchant Item or Create Artifact
	 * @param combatLocation Location of the combat where this spell is being cast; null = being cast overland
	 * @param combatTargetLocation Which specific tile of the combat map the spell is being cast at, for cell-targetted spells like combat summons
	 * @param combatTargetUnitURN Which specific unit within combat the spell is being cast at, for unit-targetted spells like Fire Bolt
	 * @param variableDamage Chosen damage selected for the spell, for spells like fire bolt where a varying amount of mana can be channeled into the spell
	 * @param mom Allows accessing server knowledge structures, player list and so on
 	 * @return Whether the spell cast was a combat spell that was an attack that resulted in the combat ending
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 * @throws RecordNotFoundException If we find the spell they're trying to cast, or other expected game elements
	 * @throws MomException If there are any issues with data or calculation logic
	 */
	@Override
	public final boolean requestCastSpell (final PlayerServerDetails player, final Integer combatCastingUnitURN, final Integer combatCastingFixedSpellNumber,
		final Integer combatCastingSlotNumber, final String spellID, final HeroItem heroItem,
		final MapCoordinates3DEx combatLocation, final MapCoordinates2DEx combatTargetLocation, final Integer combatTargetUnitURN,
		final Integer variableDamage, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, PlayerNotFoundException, RecordNotFoundException, MomException
	{
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();
		
		// Find the spell in the player's search list
		final Spell spell = mom.getServerDB ().findSpell (spellID, "requestCastSpell");
		
		// Validation checks on the type of spell and whether it needs a target
		String msg;
		if (spellID.equals (CommonDatabaseConstants.SPELL_ID_SPELL_OF_RETURN))
			msg = "You cannot cast the Spell of Return";
		
		else if ((combatLocation == null) && (!getSpellUtils ().spellCanBeCastIn (spell, SpellCastType.OVERLAND)))
			msg = "That spell cannot be cast overland.";
		
		else if ((combatLocation != null) && (combatCastingFixedSpellNumber == null) && (!getSpellUtils ().spellCanBeCastIn (spell, SpellCastType.COMBAT)))
			msg = "That spell cannot be cast in combat.";

		else if ((combatLocation == null) && ((combatTargetLocation != null) || (combatTargetUnitURN != null)))
			msg = "Cannot specify a target when casting an overland spell.";

		else if ((combatLocation != null) && (combatTargetUnitURN == null) &&
			((spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_ENCHANTMENTS) || (spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_CURSES) ||
			(((spell.getSpellBookSectionID () == SpellBookSectionID.ATTACK_SPELLS) || (spell.getSpellBookSectionID () == SpellBookSectionID.SPECIAL_UNIT_SPELLS) ||
				(spell.getSpellBookSectionID () == SpellBookSectionID.DISPEL_SPELLS)) && (spell.getAttackSpellCombatTarget () == AttackSpellTargetID.SINGLE_UNIT))))
		{
			if ((spell.getSpellBookSectionID () == SpellBookSectionID.ATTACK_SPELLS) && (spell.getSpellValidBorderTarget ().size () > 0) &&
				(combatTargetLocation != null))
			{
				// Cracks call can also be aimed at walls, so this is fine
				msg = null;
			}
			else
				msg = "You must specify a unit target when casting this spell in combat.";
		}

		else if ((combatLocation != null) && (combatTargetLocation == null) &&
			(spell.getSpellBookSectionID () == SpellBookSectionID.SUMMONING))
			msg = "You must specify a target location when casting summoning spells in combat.";

		else if ((combatLocation != null) && (combatTargetLocation == null) &&
			(spell.getSpellBookSectionID () == SpellBookSectionID.SPECIAL_COMBAT_SPELLS))
			msg = "You must specify a target location when casting special combat spells.";
		
		else if ((combatLocation != null) && (combatTargetUnitURN == null) &&
			(spell.getSpellBookSectionID () == SpellBookSectionID.SUMMONING) && (spell.getResurrectedHealthPercentage () != null))
			msg = "You must specify which unit you want to raise from the dead.";
		
		else
			msg = null;
		
		// Validation checks about who is casting it (wizard or a unit)
		ExpandedUnitDetails xuCombatCastingUnit = null;
		if (msg == null)
		{
			final SpellResearchStatusID researchStatus = getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), spellID).getStatus ();
			if (combatCastingUnitURN == null)
			{
				// Wizard casting
				if (pub.getWizardState () != WizardState.ACTIVE)
					msg = "You cannot cast spells while you are banished.";
				
				else if (researchStatus != SpellResearchStatusID.AVAILABLE)
					msg = "You don't have that spell researched and/or available so can't cast it.";
			}
			
			else if (combatLocation == null)
				msg = "A unit or hero cannot cast an overland spell.";
			
			else
			{
				// Unit or hero casting
				final MemoryUnit combatCastingUnit = getUnitUtils ().findUnitURN (combatCastingUnitURN, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ());
				if (combatCastingUnit == null)
					msg = "Cannot find the unit who is trying to cast a spell.";
				
				else if (combatCastingUnit.getStatus () != UnitStatusID.ALIVE)
					msg = "The unit you are trying to cast a spell from is dead";
				
				else if ((!combatLocation.equals (combatCastingUnit.getCombatLocation ())) || (combatCastingUnit.getCombatHeading () == null) ||
					(combatCastingUnit.getCombatSide () == null) || (combatCastingUnit.getCombatPosition () == null))
					msg = "The unit you are trying to cast a spell from is not in the correct combat.";

				else
				{
					xuCombatCastingUnit = getExpandUnitDetails ().expandUnitDetails (combatCastingUnit, null, null, null,
						mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());

					if (xuCombatCastingUnit.getControllingPlayerID () != player.getPlayerDescription ().getPlayerID ())
						msg = "The unit you are trying to cast a spell from is controlled by somebody else.";
					
					else if (combatCastingFixedSpellNumber != null)
					{
						// Validation for using fixed spells, e.g. Giant Spiders casting Web
						if ((combatCastingFixedSpellNumber < 0) || (combatCastingFixedSpellNumber >= combatCastingUnit.getFixedSpellsRemaining ().size ()) ||
							(combatCastingFixedSpellNumber >= xuCombatCastingUnit.getUnitDefinition ().getUnitCanCast ().size ()))
							msg = "This unit doesn't have the fixed spell number that you are trying to cast.";
						
						else if (combatCastingUnit.getFixedSpellsRemaining ().get (combatCastingFixedSpellNumber) <= 0)
							msg = "The unit has all of this kind of spell used up already for this combat.";
						
						else if (!spellID.equals (xuCombatCastingUnit.getUnitDefinition ().getUnitCanCast ().get (combatCastingFixedSpellNumber).getUnitSpellID ()))
							msg = "The spell you are trying to cast doesn't match the fixed spell that this unit has.";
					}
					else if (combatCastingSlotNumber != null)
					{
						// Validation for using spells imbued in hero items
						if ((combatCastingSlotNumber < 0) || (combatCastingSlotNumber >= combatCastingUnit.getHeroItemSlot ().size ()) ||
							(combatCastingSlotNumber >= combatCastingUnit.getHeroItemSpellChargesRemaining ().size ()))
							msg = "This hero doesn't have the item slot that you are trying to cast an imbued spell from.";
						
						else if (combatCastingUnit.getHeroItemSpellChargesRemaining ().get (combatCastingSlotNumber) <= 0)
							msg = "The spell charges in this hero item are all used up.";
						
						else if (combatCastingUnit.getHeroItemSlot ().get (combatCastingSlotNumber).getHeroItem () == null)
							msg = "This hero has no item in the slot that you are trying to cast an imbued spell from.";
						
						else if (!spellID.equals (combatCastingUnit.getHeroItemSlot ().get (combatCastingSlotNumber).getHeroItem ().getSpellID ()))
							msg = "The spell you are trying to cast doesn't match the spell imbued into this hero item.";
					}
					else
					{
						// Unit or hero casting from their own MP pool.
						// Units with the caster skill (Archangels, Efreets and Djinns) cast spells from their magic realm, totally ignoring whatever spells their controlling wizard knows.
						// Using getModifiedUnitMagicRealmLifeformTypeID makes this account for them casting Death spells instead if you get an undead Archangel or similar.
						String overridePickID = null;
						if (xuCombatCastingUnit.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_UNIT))
						{
							overridePickID = xuCombatCastingUnit.getModifiedUnitMagicRealmLifeformType ().getCastSpellsFromPickID ();
							if (overridePickID == null)
								overridePickID = xuCombatCastingUnit.getModifiedUnitMagicRealmLifeformType ().getPickID ();
						}

						boolean knowSpell;
						if (overridePickID != null)
							knowSpell = overridePickID.equals (spell.getSpellRealm ());
						else
						{
							knowSpell = (researchStatus == SpellResearchStatusID.AVAILABLE);
							final Iterator<UnitCanCast> knownSpellsIter = xuCombatCastingUnit.getUnitDefinition ().getUnitCanCast ().iterator ();
							while ((!knowSpell) && (knownSpellsIter.hasNext ()))
							{
								final UnitCanCast thisKnownSpell = knownSpellsIter.next ();
								if ((thisKnownSpell.getUnitSpellID ().equals (spellID)) && (thisKnownSpell.getNumberOfTimes () == null))
									knowSpell = true;
							}
						}
						
						if (!knowSpell)
							msg = "You don't have that spell researched and/or available so can't cast it.";
					}
				}
			}
		}
		
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
				(combatLocation, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), mom.getPlayers (), mom.getServerDB ());
			
			final ServerGridCellEx gc = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
				(combatLocation.getZ ()).getRow ().get (combatLocation.getY ()).getCell ().get (combatLocation.getX ());

			// Work out unmodified casting cost
			if (!combatPlayers.bothFound ())
				msg = "You cannot cast combat spells if one side has been wiped out in the combat.";
			
			else if (!player.getPlayerDescription ().getPlayerID ().equals (gc.getCombatCurrentPlayerID ()))
				msg = "You cannot cast combat spells when it isn't your turn.";
			
			else if (xuCombatCastingUnit == null)
			{
				// Validate wizard casting
				if ((gc.isSpellCastThisCombatTurn () != null) && (gc.isSpellCastThisCombatTurn ()))
					msg = "You have already cast a spell this combat turn.";
				else
				{
					// Apply books/retorts that make spell cheaper to cast
					reducedCombatCastingCost = getSpellUtils ().getReducedCombatCastingCost
						(spell, variableDamage, pub.getPick (), mom.getSessionDescription ().getSpellSetting (), mom.getServerDB ());
					
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
			}
			else if ((combatCastingSlotNumber == null) && (combatCastingFixedSpellNumber == null))
			{
				// Validate unit or hero casting
				// Reductions for number of spell books, or certain retorts, only apply when the wizard is casting, but on the plus side, the range penalty doesn't apply
				reducedCombatCastingCost = getSpellUtils ().getUnmodifiedCombatCastingCost (spell, variableDamage, pub.getPick ());
				multipliedManaCost = reducedCombatCastingCost;
				
				if (multipliedManaCost > xuCombatCastingUnit.getManaRemaining ())
					msg = "This unit or hero doesn't have enough mana remaining to cast the spell.";
			}
			
			// Validation for specific types of combat spells
			if (msg != null)
			{
				// Do nothing - more serious message already generated
			}
			else if ((combatTargetUnitURN != null) &&
				((spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_ENCHANTMENTS) || (spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_CURSES) ||
				((spell.getSpellBookSectionID () == SpellBookSectionID.SUMMONING) && (spell.getResurrectedHealthPercentage () != null)) ||
				(((spell.getSpellBookSectionID () == SpellBookSectionID.ATTACK_SPELLS) || (spell.getSpellBookSectionID () == SpellBookSectionID.SPECIAL_UNIT_SPELLS) ||
					(spell.getSpellBookSectionID () == SpellBookSectionID.DISPEL_SPELLS)) &&
					(spell.getAttackSpellCombatTarget () == AttackSpellTargetID.SINGLE_UNIT))))
			{
				// (Note overland spells tend to have a lot less validation since we don't pick targets until they've completed casting - so the checks are done then)
				// Verify that the chosen unit is a valid target for unit enchantments/curses (we checked above that a unit has chosen)
				combatTargetUnit = getUnitUtils ().findUnitURN (combatTargetUnitURN, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ());
				if (combatTargetUnit == null)
					msg = "Cannot find the unit you are trying to target the spell on.";
				else
				{
					final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (combatTargetUnit, null, null, spell.getSpellRealm (),
						mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
					
					final TargetSpellResult validTarget = getMemoryMaintainedSpellUtils ().isUnitValidTargetForSpell
						(spell, null, combatLocation, player.getPlayerDescription ().getPlayerID (), xuCombatCastingUnit, variableDamage, xu, true,
							mom.getGeneralServerKnowledge ().getTrueMap (), priv.getFogOfWar (), mom.getPlayers (), mom.getServerDB ());
					
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
					mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), priv.getFogOfWar (),
					mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (), mom.getPlayers ());
				
				if (validTarget != TargetSpellResult.VALID_TARGET)
				{
					// Using the enum name isn't that great, but the client will already have
					// performed this validation so should never see any message generated here anyway
					msg = "This city spell cannot be cast in this combat location for reason " + validTarget;
				}
			}
			else if (spell.getSpellBookSectionID () == SpellBookSectionID.SUMMONING)
			{
				// Verify for summoning spells that there isn't a unit in that location... one we know about anyway
				if (getUnitUtils ().findAliveUnitInCombatWeCanSeeAt (combatLocation, combatTargetLocation, player.getPlayerDescription ().getPlayerID (), mom.getPlayers (),
					mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB (), mom.getSessionDescription ().getCombatMapSize ()) != null)
					msg = "There is already a unit in the chosen location so you cannot summon there.";
				
				else if ((!mom.getSessionDescription ().getUnitSetting ().isCanExceedMaximumUnitsDuringCombat ()) &&
					(getCombatMapServerUtils ().countPlayersAliveUnitsAtCombatLocation (player.getPlayerDescription ().getPlayerID (), combatLocation,
						mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), mom.getServerDB ()) >= CommonDatabaseConstants.MAX_UNITS_PER_MAP_CELL))
					
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
			else if ((spell.getSpellBookSectionID () == SpellBookSectionID.SPECIAL_COMBAT_SPELLS) ||	// Earth to Mud or Disrupt Wall
				((spell.getSpellBookSectionID () == SpellBookSectionID.ATTACK_SPELLS) && (spell.getSpellValidBorderTarget ().size () > 0) &&
					(combatTargetLocation != null)))		// Cracks Call when targetted at a wall instead of a unit
			{
				// Check location is valid 
				if (!getMemoryMaintainedSpellUtils ().isCombatLocationValidTargetForSpell (spell, combatTargetLocation, gc.getCombatMap ()))
					msg = "This location is not a valid target for this combat spell";
			}
		}
		
		// Separate routine to validate hero items we're trying to craft
		if ((msg == null) && (heroItem != null))
			msg = getHeroItemServerUtils ().validateHeroItem (player, spell, heroItem, mom.getSessionDescription ().getUnitSetting (), mom.getServerDB ());
		
		// Ok to go ahead and cast (or queue) it?
		boolean combatEnded = false;
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
			combatEnded = getSpellProcessing ().castCombatNow (player, xuCombatCastingUnit, combatCastingFixedSpellNumber, combatCastingSlotNumber, spell,
				reducedCombatCastingCost, multipliedManaCost, variableDamage, combatLocation,
				(PlayerServerDetails) combatPlayers.getDefendingPlayer (), (PlayerServerDetails) combatPlayers.getAttackingPlayer (),
				combatTargetUnit, combatTargetLocation, mom);
		}
		else
		{
			// Overland spell - need to see if we can instant cast it or need to queue it up
			final boolean castInstantly;
			final int reducedCastingCost;
			if ((mom.getSessionDescription ().getTurnSystem () == TurnSystem.ONE_PLAYER_AT_A_TIME) &&
				(!mom.getGeneralPublicKnowledge ().getCurrentPlayerID ().equals (player.getPlayerDescription ().getPlayerID ())))
			{
				castInstantly = false;
				reducedCastingCost = 0;
			}
			else
			{
				reducedCastingCost = getSpellUtils ().getReducedOverlandCastingCost (spell, heroItem, variableDamage, pub.getPick (),
					mom.getSessionDescription ().getSpellSetting (), mom.getServerDB ());
				
				castInstantly = (priv.getQueuedSpell ().size () == 0) && (Math.min (trans.getOverlandCastingSkillRemainingThisTurn (),
					getResourceValueUtils ().findAmountStoredForProductionType (priv.getResourceValue (),
						CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)) >= reducedCastingCost);
			}
			
			if (castInstantly)
			{
				// Cast instantly, and show the casting message instantly too
				getSpellProcessing ().castOverlandNow (player, spell, variableDamage, heroItem, mom);
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
				queueSpell (player, mom.getPlayers (), spellID, heroItem, variableDamage);
		}
		
		return combatEnded;
	}
	
	/**
	 * Adds a spell to a player's overland casting queue.  This assumes we've already been through all the validation to make sure they're allowed to cast it,
	 * and to make sure they can't cast it instantly.
	 * 
	 * @param player Player casting the spell
	 * @param players List of players in the session
	 * @param spellID Which spell they want to cast
	 * @param heroItem If create item/artifact, the details of the item to create
	 * @param variableDamage Chosen damage selected for the spell, for spells like disenchant area where a varying amount of mana can be channeled into the spell
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void queueSpell (final PlayerServerDetails player, final List<PlayerServerDetails> players, final String spellID, final HeroItem heroItem,
		final Integer variableDamage) throws JAXBException, XMLStreamException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		
		// Queue it on server
		final QueuedSpell queued = new QueuedSpell ();
		queued.setQueuedSpellID (spellID);
		queued.setHeroItem (heroItem);
		queued.setVariableDamage (variableDamage);
		
		priv.getQueuedSpell ().add (queued);
		
		// Queue it on client
		if (player.getPlayerDescription ().isHuman ())
		{
			final OverlandCastQueuedMessage reply = new OverlandCastQueuedMessage ();
			reply.setSpellID (spellID);
			reply.setHeroItem (heroItem);
			reply.setVariableDamage (variableDamage);
			
			player.getConnection ().sendMessageToClient (reply);
		}
		
		// Tell everyone if someone started casting Spell of Mastery
		if ((priv.getQueuedSpell ().size () == 1) && (spellID.equals (CommonDatabaseConstants.SPELL_ID_SPELL_OF_MASTERY)))
		{
			final PlayAnimationMessage msg = new PlayAnimationMessage ();
			msg.setAnimationID (AnimationID.STARTED_SPELL_OF_MASTERY);
			msg.setPlayerID (player.getPlayerDescription ().getPlayerID ());
			
			getMultiplayerSessionServerUtils ().sendMessageToAllClients (players, msg);
		}
	}

	/**
	 * Spends any skill/mana the player has left towards casting queued spells
	 *
	 * @param player Player whose casting to progress
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return True if we cast at least one spell
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final boolean progressOverlandCasting (final PlayerServerDetails player, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();

		// Keep going while this player has spells queued, free mana and free skill
		boolean anySpellsCast = false;
		int manaRemaining = getResourceValueUtils ().findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
		while ((mom.getPlayers ().size () > 0) && (priv.getQueuedSpell ().size () > 0) &&
			(trans.getOverlandCastingSkillRemainingThisTurn () > 0) && (manaRemaining > 0))
		{
			// How much to put towards this spell?
			final QueuedSpell queued = priv.getQueuedSpell ().get (0);
			final Spell spell = mom.getServerDB ().findSpell (queued.getQueuedSpellID (), "progressOverlandCasting");
			final int reducedCastingCost = getSpellUtils ().getReducedOverlandCastingCost (spell, queued.getHeroItem (), queued.getVariableDamage (), pub.getPick (),
				mom.getSessionDescription ().getSpellSetting (), mom.getServerDB ());
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
				priv.getQueuedSpell ().remove (0);
				priv.setManaSpentOnCastingCurrentSpell (0);

				// Remove queued spell on client
				if (player.getPlayerDescription ().isHuman ())
				{
					final RemoveQueuedSpellMessage msg = new RemoveQueuedSpellMessage ();
					msg.setQueuedSpellIndex (0);
					player.getConnection ().sendMessageToClient (msg);
				}

				// Cast it
				getSpellProcessing ().castOverlandNow (player, spell, queued.getVariableDamage (), queued.getHeroItem (), mom);
				anySpellsCast = true;
				
				// If the next thing we had queued is Spell of Mastery then announce it
				// Note if what we just cast was Spell of Mastery, the session may have already been wiped out and the players list be empty
				if ((mom.getPlayers ().size () > 0) && (priv.getQueuedSpell ().size () > 0) &&
					(priv.getQueuedSpell ().get (0).getQueuedSpellID ().equals (CommonDatabaseConstants.SPELL_ID_SPELL_OF_MASTERY)))
				{
					final PlayAnimationMessage msg = new PlayAnimationMessage ();
					msg.setAnimationID (AnimationID.STARTED_SPELL_OF_MASTERY);
					msg.setPlayerID (player.getPlayerDescription ().getPlayerID ());
					
					getMultiplayerSessionServerUtils ().sendMessageToAllClients (mom.getPlayers (), msg);
				}
			}

			// Update mana spent so far on client (or set to 0 if finished)
			// Maybe this should be moved out?  If we cast multiple queued spells, do we really have to keep sending 0's?
			// Surely the client only cares on the mana spent on casting ones that we don't remove from its queue
			if ((mom.getPlayers ().size () > 0) && (player.getPlayerDescription ().isHuman ()))
			{
				final UpdateManaSpentOnCastingCurrentSpellMessage msg = new UpdateManaSpentOnCastingCurrentSpellMessage ();
				msg.setManaSpentOnCastingCurrentSpell (priv.getManaSpentOnCastingCurrentSpell ());
				player.getConnection ().sendMessageToClient (msg);
			}

			// No need to tell client how much skill they've got left or mana stored since this is the end of the turn and both will be sent next start phase
		}

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

	/**
	 * @return Hero item server utils
	 */
	public final HeroItemServerUtils getHeroItemServerUtils ()
	{
		return heroItemServerUtils;
	}

	/**
	 * @param util Hero item server utils
	 */
	public final void setHeroItemServerUtils (final HeroItemServerUtils util)
	{
		heroItemServerUtils = util;
	}

	/**
	 * @return Server only helper methods for dealing with players in a session
	 */
	public final MultiplayerSessionServerUtils getMultiplayerSessionServerUtils ()
	{
		return multiplayerSessionServerUtils;
	}

	/**
	 * @param obj Server only helper methods for dealing with players in a session
	 */
	public final void setMultiplayerSessionServerUtils (final MultiplayerSessionServerUtils obj)
	{
		multiplayerSessionServerUtils = obj;
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