package momime.server.process;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.AttackResolution;
import momime.common.database.AttackResolutionStep;
import momime.common.database.AttackSpellCombatTargetID;
import momime.common.database.DamageResolutionTypeID;
import momime.common.database.NegatedBySkill;
import momime.common.database.NegatedByUnitID;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.StoredDamageTypeID;
import momime.common.database.UnitCombatSideID;
import momime.common.messages.MemoryUnit;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.DamageCalculationHeaderData;
import momime.common.messages.servertoclient.DamageCalculationMessageTypeID;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.AttackDamage;
import momime.server.calculations.DamageCalculator;
import momime.server.database.ServerDatabaseValues;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.FogOfWarMidTurnMultiChanges;
import momime.server.fogofwar.KillUnitActionID;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.utils.UnitServerUtils;

/**
 * Routines dealing with applying combat damage
 */
public final class DamageProcessorImpl implements DamageProcessor
{
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnMultiChanges fogOfWarMidTurnMultiChanges;
	
	/** Damage calc */
	private DamageCalculator damageCalculator;
	
	/** Attack resolution processing */
	private AttackResolutionProcessing attackResolutionProcessing;
	
	/** Starting and ending combats */
	private CombatStartAndEnd combatStartAndEnd;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;

	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/**
	 * Performs one attack in combat, which may be a melee, ranged or spell attack.
	 * If a close combat attack, also resolves the defender retaliating.
	 * Also checks to see if the attack results in either side being wiped out, in which case ends the combat.
	 * 
	 * @param attacker Unit making the attack; or null if the attack isn't coming from a unit
	 * @param defenders Unit(s) being hit; some attacks can hit multiple units such as Flame Strike
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackerDirection The direction the attacker needs to turn to in order to be facing the defender; or null if the attack isn't coming from a unit
	 * @param attackSkillID The skill being used to attack, i.e. UA01 (swords) or UA02 (ranged); or null if the attack isn't coming from a unit
	 * @param spell The spell being cast; or null if the attack isn't coming from a spell
	 * @param variableDamage The damage chosen, for spells where variable mana can be channeled into casting them, e.g. fire bolt; or null if the attack isn't coming from a spell
	 * @param castingPlayer The player casting the spell; or null if the attack isn't coming from a spell
	 * @param combatLocation Where the combat is taking place
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether the attack resulted in the combat ending
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final boolean resolveAttack (final MemoryUnit attacker, final List<MemoryUnit> defenders,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final Integer attackerDirection, final String attackSkillID,
		final Spell spell, final Integer variableDamage, final PlayerServerDetails castingPlayer, 
		final MapCoordinates3DEx combatLocation, final MomSessionVariables mom)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		if (defenders.size () == 0)
			throw new MomException ("resolveAttack was called with 0 defenders");

		// We send this a couple of times for different parts of the calculation, so initialize it here
		final DamageCalculationHeaderData damageCalculationMsg = new DamageCalculationHeaderData ();
		damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.HEADER);
		damageCalculationMsg.setAttackSkillID (attackSkillID);

		if ((spell == null) || (spell.getAttackSpellCombatTarget () == AttackSpellCombatTargetID.SINGLE_UNIT))
			damageCalculationMsg.setDefenderUnitURN (defenders.get (0).getUnitURN ());
		
		if (spell != null)
			damageCalculationMsg.setAttackSpellID (spell.getSpellID ());

		if (attacker != null)
		{
			damageCalculationMsg.setAttackerUnitURN (attacker.getUnitURN ());
			damageCalculationMsg.setAttackerPlayerID (attacker.getOwningPlayerID ());
		}
		else
			damageCalculationMsg.setAttackerPlayerID (castingPlayer.getPlayerDescription ().getPlayerID ());
		
		getDamageCalculator ().sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		
		// Make the units face each other
		if (attackerDirection != null)
		{
			attacker.setCombatHeading (attackerDirection);
			
			final int defenderDirection = getCoordinateSystemUtils ().normalizeDirection
				(mom.getSessionDescription ().getCombatMapSize ().getCoordinateSystemType (), attackerDirection + 4);
			
			for (final MemoryUnit defender : defenders) 
				defender.setCombatHeading (defenderDirection);
		}
		
		// If its a spell, we work out the kind of damage dealt once only (so it only appears in the client message log once)
		// If its a regular attribute-based attack, damage is worked out inside each resolution step.
		final AttackDamage commonPotentialDamageToDefenders;
		if (spell == null)
			commonPotentialDamageToDefenders = null;
		else
		{
			final ExpandedUnitDetails xuAttacker = (attacker == null) ? null : 
				getUnitUtils ().expandUnitDetails (attacker, null, null, null,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());

			commonPotentialDamageToDefenders = getDamageCalculator ().attackFromSpell
				(spell, variableDamage, castingPlayer, xuAttacker, attackingPlayer, defendingPlayer, mom.getServerDB ());
		}
		
		// Process our attack against each defender
		final List<DamageResolutionTypeID> specialDamageResolutionsApplied = new ArrayList<DamageResolutionTypeID> ();
		for (final MemoryUnit defender : defenders)
		{
			// Calculate the attacker stats, with the defender listed as the opponent.
			// This is important for the call to chooseAttackResolution, since the defender may have Negate First Strike.
			final ExpandedUnitDetails xuDefender = getUnitUtils ().expandUnitDetails (defender, null, null, null,
				mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
			
			final ExpandedUnitDetails xuAttacker;
			if (attacker == null)
				xuAttacker = null;
			else
			{
				final List<ExpandedUnitDetails> xuDefenders = new ArrayList<ExpandedUnitDetails> ();
				xuDefenders.add (xuDefender);
				
				xuAttacker = getUnitUtils ().expandUnitDetails (attacker, xuDefenders, null, null,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
			}

			// For attacks based on unit attributes (i.e. melee or ranged attacks), use the full routine to work out the sequence of steps to resolve the attack.
			// If its an attack from a spell, just make a dummy list with a null in it - processAttackResolutionStep looks for this.
			final List<List<AttackResolutionStep>> steps;
			if (commonPotentialDamageToDefenders != null)
			{
				final List<AttackResolutionStep> dummySteps = new ArrayList<AttackResolutionStep> ();
				dummySteps.add (null);
				
				steps = new ArrayList<List<AttackResolutionStep>> ();
				steps.add (dummySteps);
			}
			else
			{
				final AttackResolution attackResolution = getAttackResolutionProcessing ().chooseAttackResolution (xuAttacker, xuDefender, attackSkillID, mom.getServerDB ());
				
				steps = getAttackResolutionProcessing ().splitAttackResolutionStepsByStepNumber (attackResolution.getAttackResolutionStep ());
			}

			// If this particular defender is immune to an illusionary attack, then temporarily set the spell damage resolution type to normal
			boolean downgradeIllusionaryAttack = false;
			if ((commonPotentialDamageToDefenders != null) && (commonPotentialDamageToDefenders.getDamageResolutionTypeID () == DamageResolutionTypeID.ILLUSIONARY))
			{
				// Borrow the list of immunities from the Illusionary Attack skill - I don't want to have to define immunties to damage types in the XSD just for this
				final Iterator<NegatedBySkill> iter = mom.getServerDB ().findUnitSkill
					(ServerDatabaseValues.UNIT_SKILL_ID_ILLUSIONARY_ATTACK, "resolveAttack").getNegatedBySkill ().iterator ();
				while ((!downgradeIllusionaryAttack) && (iter.hasNext ()))
				{
					final NegatedBySkill negateIllusionaryAttack = iter.next ();
					if ((negateIllusionaryAttack.getNegatedByUnitID () == NegatedByUnitID.ENEMY_UNIT) && (xuDefender.hasModifiedSkill (negateIllusionaryAttack.getNegatedBySkillID ())))
						downgradeIllusionaryAttack = true;
				}
			}
			
			final AttackDamage spellDamageToThisDefender = downgradeIllusionaryAttack ?
				new AttackDamage (commonPotentialDamageToDefenders, DamageResolutionTypeID.SINGLE_FIGURE) : commonPotentialDamageToDefenders;
			
			// Some figures being frozen in fear lasts for the duration of one attack resolution,
			// i.e. spans multiple resolution steps so have to keep track of it out here
			final AttackResolutionUnit attackerWrapper = (attacker == null) ? null : new AttackResolutionUnit (attacker);
			final AttackResolutionUnit defenderWrapper = new AttackResolutionUnit (defender);
			
			// Process each step
			for (final List<AttackResolutionStep> step : steps)
				
				// Skip the entire step if either unit is already dead
				if (((xuAttacker == null) || (xuAttacker.calculateAliveFigureCount () > 0)) &&
					(xuDefender.calculateAliveFigureCount () > 0))
				{
					final List<DamageResolutionTypeID> thisSpecialDamageResolutionsApplied = getAttackResolutionProcessing ().processAttackResolutionStep
						(attackerWrapper, defenderWrapper, attackingPlayer, defendingPlayer, combatLocation, step, spellDamageToThisDefender,
							mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getSessionDescription ().getCombatMapSize (), mom.getServerDB ());
					
					for (final DamageResolutionTypeID thisSpecialDamageResolutionApplied : thisSpecialDamageResolutionsApplied)
						if (!specialDamageResolutionsApplied.contains (thisSpecialDamageResolutionApplied))
							specialDamageResolutionsApplied.add (thisSpecialDamageResolutionApplied);
				}
		}
		
		// Update damage taken in player's memory on server, and on all clients who can see the unit.
		// This includes both players involved in the combat (who will queue this up as an animation), and players who aren't involved in the combat but
		// can see the units fighting (who will update the damage immediately).
		// This also sends the number of combat movement points the attacker has left.
		getFogOfWarMidTurnChanges ().sendCombatDamageToClients (attacker, damageCalculationMsg.getAttackerPlayerID (), defenders,
			damageCalculationMsg.getAttackSkillID (), damageCalculationMsg.getAttackSpellID (),
			specialDamageResolutionsApplied, mom.getPlayers (),
			mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting ());
		
		// Now we know who the COMBAT attacking and defending players are, we can work out whose
		// is whose unit - because it could be the defending players' unit making the attack in combat.
		// We have to know this, because if both units die at the same time, the defender wins the combat.
		final List<MemoryUnit> attackingPlayerUnits;
		final List<MemoryUnit> defendingPlayerUnits;
		if (defenders.get (0).getOwningPlayerID () == defendingPlayer.getPlayerDescription ().getPlayerID ())
		{
			attackingPlayerUnits = new ArrayList<MemoryUnit> ();
			if (attacker != null)
				attackingPlayerUnits.add (attacker);
			
			defendingPlayerUnits = defenders;
		}
		else
		{
			attackingPlayerUnits = defenders;
			
			defendingPlayerUnits = new ArrayList<MemoryUnit> ();
			if (attacker != null)
				defendingPlayerUnits.add (attacker);
		}
		
		// Kill off any of the units who may have died.
		// We don't need to notify the clients of this separately, clients can tell from the damage taken values above whether the units are dead or not,
		// whether or not they're involved in the combat.
		final ServerGridCellEx tc = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(combatLocation.getZ ()).getRow ().get (combatLocation.getY ()).getCell ().get (combatLocation.getX ());
		
		boolean combatEnded = false;
		PlayerServerDetails winningPlayer = null;
		
		if (attackingPlayerUnits.size () > 0)
		{
			boolean anyAttackingPlayerUnitsSurvived = false;
			for (final MemoryUnit attackingPlayerUnit : attackingPlayerUnits)
			{
				final ExpandedUnitDetails xuAttackingPlayerUnit = getUnitUtils ().expandUnitDetails (attackingPlayerUnit, null, null, null,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());

				if (xuAttackingPlayerUnit.calculateAliveFigureCount () > 0)
					anyAttackingPlayerUnitsSurvived = true;
				else
				{
					tc.setAttackerSpecialFameLost (tc.getAttackerSpecialFameLost () + xuAttackingPlayerUnit.calculateFameLostForUnitDying ());
					
					final KillUnitActionID action = (getUnitServerUtils ().whatKilledUnit (attackingPlayerUnit.getUnitDamage ()) == StoredDamageTypeID.PERMANENT) ?
						KillUnitActionID.PERMANENT_DAMAGE : KillUnitActionID.HEALABLE_COMBAT_DAMAGE;
					
					getFogOfWarMidTurnChanges ().killUnitOnServerAndClients (attackingPlayerUnit, action,
						mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), mom.getSessionDescription ().getFogOfWarSetting (), mom.getServerDB ());
					
					getFogOfWarMidTurnMultiChanges ().grantExperienceToUnitsInCombat (combatLocation, UnitCombatSideID.DEFENDER,
						mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting ());
				}
			}
					
			// If the attacker is now wiped out, this is the last record we will ever have of who the attacking player was, so we have to deal with tidying up the combat now
			if ((!anyAttackingPlayerUnitsSurvived) &&
				(countUnitsInCombat (combatLocation, UnitCombatSideID.ATTACKER, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ()) == 0))
			{
				combatEnded = true;
				winningPlayer = defendingPlayer;
			}
		}

		if (defendingPlayerUnits.size () > 0)
		{
			boolean anyDefendingPlayerUnitsSurvived = false;
			for (final MemoryUnit defendingPlayerUnit : defendingPlayerUnits)
			{
				final ExpandedUnitDetails xuDefendingPlayerUnit = getUnitUtils ().expandUnitDetails (defendingPlayerUnit, null, null, null,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());

				if (xuDefendingPlayerUnit.calculateAliveFigureCount () > 0)
					anyDefendingPlayerUnitsSurvived = true;
				else
				{
					tc.setDefenderSpecialFameLost (tc.getDefenderSpecialFameLost () + xuDefendingPlayerUnit.calculateFameLostForUnitDying ());
					
					final KillUnitActionID action = (getUnitServerUtils ().whatKilledUnit (defendingPlayerUnit.getUnitDamage ()) == StoredDamageTypeID.PERMANENT) ?
						KillUnitActionID.PERMANENT_DAMAGE : KillUnitActionID.HEALABLE_COMBAT_DAMAGE;
					
					getFogOfWarMidTurnChanges ().killUnitOnServerAndClients (defendingPlayerUnit, action,
						mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), mom.getSessionDescription ().getFogOfWarSetting (), mom.getServerDB ());
					
					getFogOfWarMidTurnMultiChanges ().grantExperienceToUnitsInCombat (combatLocation, UnitCombatSideID.ATTACKER,
						mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting ());
				}
			}
			
			// If the defender is now wiped out, this is the last record we will ever have of who the defending player was, so we have to deal with tidying up the combat now.
			// If attacker was also wiped out then we've already done this - the defender won by default.
			if ((!combatEnded) && (!anyDefendingPlayerUnitsSurvived) &&
				(countUnitsInCombat (combatLocation, UnitCombatSideID.DEFENDER, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ()) == 0))
			{
				combatEnded = true;
				winningPlayer = attackingPlayer;
			}
		}
		
		// End the combat if one side was totally wiped out
		if (combatEnded)
			getCombatStartAndEnd ().combatEnded (combatLocation, attackingPlayer, defendingPlayer, winningPlayer, null, mom);
		
		return combatEnded;
	}

	/**
	 * @param combatLocation Location that combat is taking place
	 * @param combatSide Which side to count
	 * @param trueUnits List of true units
	 * @return How many units are still left alive in combat on the requested side
	 */
	@Override
	public final int countUnitsInCombat (final MapCoordinates3DEx combatLocation, final UnitCombatSideID combatSide,
		final List<MemoryUnit> trueUnits)
	{
		int count = 0;
		for (final MemoryUnit trueUnit : trueUnits)
			if ((trueUnit.getStatus () == UnitStatusID.ALIVE) && (combatLocation.equals (trueUnit.getCombatLocation ())) &&
				(trueUnit.getCombatSide () == combatSide) && (trueUnit.getCombatPosition () != null) && (trueUnit.getCombatHeading () != null))
					
				count++;

		return count;
	}
	
	/**
	 * @return Methods for updating true map + players' memory
	 */
	public final FogOfWarMidTurnChanges getFogOfWarMidTurnChanges ()
	{
		return fogOfWarMidTurnChanges;
	}

	/**
	 * @param obj Methods for updating true map + players' memory
	 */
	public final void setFogOfWarMidTurnChanges (final FogOfWarMidTurnChanges obj)
	{
		fogOfWarMidTurnChanges = obj;
	}

	/**
	 * @return Methods for updating true map + players' memory
	 */
	public final FogOfWarMidTurnMultiChanges getFogOfWarMidTurnMultiChanges ()
	{
		return fogOfWarMidTurnMultiChanges;
	}

	/**
	 * @param obj Methods for updating true map + players' memory
	 */
	public final void setFogOfWarMidTurnMultiChanges (final FogOfWarMidTurnMultiChanges obj)
	{
		fogOfWarMidTurnMultiChanges = obj;
	}
	
	/**
	 * @return Damage calc
	 */
	public final DamageCalculator getDamageCalculator ()
	{
		return damageCalculator;
	}

	/**
	 * @param calc Damage calc
	 */
	public final void setDamageCalculator (final DamageCalculator calc)
	{
		damageCalculator = calc;
	}

	/**
	 * @return Attack resolution processing
	 */
	public final AttackResolutionProcessing getAttackResolutionProcessing ()
	{
		return attackResolutionProcessing;
	}

	/**
	 * @param proc Attack resolution processing
	 */
	public final void setAttackResolutionProcessing (final AttackResolutionProcessing proc)
	{
		attackResolutionProcessing= proc;
	}
	
	/**
	 * @return Starting and ending combats
	 */
	public final CombatStartAndEnd getCombatStartAndEnd ()
	{
		return combatStartAndEnd;
	}

	/**
	 * @param cse Starting and ending combats
	 */
	public final void setCombatStartAndEnd (final CombatStartAndEnd cse)
	{
		combatStartAndEnd = cse;
	}
	
	/**
	 * @return Coordinate system utils
	 */
	public final CoordinateSystemUtils getCoordinateSystemUtils ()
	{
		return coordinateSystemUtils;
	}

	/**
	 * @param utils Coordinate system utils
	 */
	public final void setCoordinateSystemUtils (final CoordinateSystemUtils utils)
	{
		coordinateSystemUtils = utils;
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