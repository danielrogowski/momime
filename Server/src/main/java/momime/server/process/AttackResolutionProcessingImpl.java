package momime.server.process;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.calculations.UnitCalculations;
import momime.common.database.DamageTypeID;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitCombatSideID;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.utils.UnitSkillUtils;
import momime.server.calculations.AttackDamage;
import momime.server.calculations.DamageCalculator;
import momime.server.database.AttackResolutionConditionSvr;
import momime.server.database.AttackResolutionStepSvr;
import momime.server.database.AttackResolutionSvr;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.UnitAttributeSvr;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Methods for processing attack resolutions.  This would all just be part of DamageProcessor, these methods
 * are just moved out so they can be mocked separately in unit tests.
 */
public final class AttackResolutionProcessingImpl implements AttackResolutionProcessing
{
	/** Class logger */
	private final Log log = LogFactory.getLog (AttackResolutionProcessingImpl.class);
	
	/** Unit skill utils */
	private UnitSkillUtils unitSkillUtils;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** Damage calc */
	private DamageCalculator damageCalculator;
	
	/**
	 * When one unit initiates a unit attribute-based attack in combat against another, determines the most appropriate attack resolution rules to deal with processing the attack.
	 * 
	 * @param attacker Unit making the attack (may be owned by the player that is defending in combat) 
	 * @param defender Unit being attacked (may be owned by the player that is attacking in combat)
	 * @param attackAttributeID Which attribute they are attacking with (melee or ranged)
	 * @param players Players list
	 * @param spells Known spells
	 * @param combatAreaEffects Known combat area effects
	 * @param db Lookup lists built over the XML database
	 * @return Chosen attack resolution
	 * @throws RecordNotFoundException If the unit attribute or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If no attack resolutions are appropriate, or if there are errors checking unit skills
	 */
	@Override
	public final AttackResolutionSvr chooseAttackResolution (final MemoryUnit attacker, final MemoryUnit defender, final String attackAttributeID,
		final List<PlayerServerDetails> players, final List<MemoryMaintainedSpell> spells, final List<MemoryCombatAreaEffect> combatAreaEffects, final ServerDatabaseEx db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering chooseAttackResolution: Unit URN " + attacker.getUnitURN () + " hitting Unit URN " + defender.getUnitURN () + " with " + attackAttributeID);
	
		final UnitAttributeSvr unitAttr = db.findUnitAttribute (attackAttributeID, "chooseAttackResolution");
		
		// Check all possible attack resolutions to select the most appropriate one
		AttackResolutionSvr match = null;
		final Iterator<AttackResolutionSvr> iter = unitAttr.getAttackResolutions ().iterator ();
		while ((match == null) && (iter.hasNext ()))
		{
			final AttackResolutionSvr attackResolution = iter.next ();
			
			// Check all the conditions
			boolean conditionsMatch = true;
			final Iterator<AttackResolutionConditionSvr> conditions = attackResolution.getAttackResolutionConditions ().iterator ();
			while ((conditionsMatch) && (conditions.hasNext ()))
			{
				final AttackResolutionConditionSvr condition = conditions.next ();
				
				// Check this condition
				final MemoryUnit unitToTest = (condition.getCombatSide () == UnitCombatSideID.ATTACKER) ? attacker : defender;
				if (getUnitSkillUtils ().getModifiedSkillValue (unitToTest, unitToTest.getUnitHasSkill (), condition.getUnitSkillID (), players, spells, combatAreaEffects, db) < 0)
					conditionsMatch = false;
			}
			
			if (conditionsMatch)
				match = attackResolution;
		}
		
		// Its an error if we fail to find any match at all
		if (match == null)
			throw new MomException ("Unit URN " + attacker.getUnitURN () + " tried to hit Unit URN " + defender.getUnitURN () + " with attack of type " + attackAttributeID +
				" but there are no defined attack resolutions capable of processing the attack");
		
		log.trace ("Exiting chooseAttackResolution = " + match);
		return match;
	}
	
	/**
	 * @param steps Steps in one continuous list
	 * @return Same list as input, but segmented into sublists where all steps share the same step number
	 * @throws MomException If the steps in the input list aren't in stepNumber order
	 */
	@Override
	public final List<List<AttackResolutionStepSvr>> splitAttackResolutionStepsByStepNumber (final List<AttackResolutionStepSvr> steps)
		throws MomException
	{
		log.trace ("Entering splitAttackResolutionStepsByStepNumber: " + steps.size () + " steps");
		
		final List<List<AttackResolutionStepSvr>> result = new ArrayList<List<AttackResolutionStepSvr>> ();
		
		int currentStepNumber = 0;
		List<AttackResolutionStepSvr> currentList = null;
		for (final AttackResolutionStepSvr step : steps)
		{
			// Only acceptable values are currentStepNumber, or currentStepNumber+1
			if (step.getStepNumber () <= 0)
				throw new MomException ("Found an attack resolution step with step number of " + step.getStepNumber ());
			
			else if (step.getStepNumber () == currentStepNumber)
				currentList.add (step);
			
			else if (step.getStepNumber () == currentStepNumber + 1)
			{
				currentStepNumber = currentStepNumber + 1;
				currentList = new ArrayList<AttackResolutionStepSvr> ();
				currentList.add (step);
				result.add (currentList);
			}
			
			else
				throw new MomException ("Found an attack resolution with steps out of order - jumped from " + currentStepNumber + " to " + step.getStepNumber ());
		}

		log.trace ("Exiting splitAttackResolutionStepsByStepNumber = " + result.size () + " step numbers");
		return result;
	}
	
	/**
	 * Executes all of the steps of an attack sequence that have the same step number, i.e. all those steps such that damage is calculated and applied simultaneously.
	 * 
	 * @param attacker Unit making the attack; or null if the attack isn't coming from a unit 
	 * @param defender Unit being attacked
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param steps The steps to take, i.e. all of the steps defined under the chosen attackResolution that have the same stepNumber
	 * @param commonPotentialDamageToDefenders This damage is applied to the defender if any "null" entries are encountered in the steps list (used for spell damage)
	 * @param players Players list
	 * @param spells Known spells
	 * @param combatAreaEffects Known combat area effects
	 * @param db Lookup lists built over the XML database
	 * @return List of special damage types done to the defender (used for warp wood); limitation that client assumes this damage type is applied to ALL defenders
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit or other rule errors
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final List<DamageTypeID> processAttackResolutionStep (final AttackResolutionUnit attacker, final AttackResolutionUnit defender,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final List<AttackResolutionStepSvr> steps, final AttackDamage commonPotentialDamageToDefenders,
		final List<PlayerServerDetails> players, final List<MemoryMaintainedSpell> spells, final List<MemoryCombatAreaEffect> combatAreaEffects, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering processAttackResolutionStep: Attacking unit URN " + ((attacker != null) ? new Integer (attacker.getUnit ().getUnitURN ()).toString () : "N/A") +
			", Defending unit URN " + defender.getUnit ().getUnitURN () + ", " + steps.size () + " steps");

		// Zero out damage taken
		int damageToDefender = 0;
		int damageToAttacker = 0;
		
		// Calculate and total up all the damage before we apply any of it
		final List<DamageTypeID> specialDamageTypesApplied = new ArrayList<DamageTypeID> ();
		for (final AttackResolutionStepSvr step : steps)
		{
			// Which unit is being attacked?
			final AttackResolutionUnit unitBeingAttacked = ((step == null) || (step.getCombatSide () == UnitCombatSideID.ATTACKER)) ? defender : attacker;
			if (unitBeingAttacked == null)
				throw new MomException ("processAttackResolutionStep: Tried to process attack step from a null unitBeingAttacked, attacking side = " + step.getCombatSide ());

			final int stepRepetitions = ((step == null) || (step.getRepetitions () == null)) ? 1 : step.getRepetitions ();
			for (int stepRepetitionNo = 0; stepRepetitionNo < stepRepetitions; stepRepetitionNo++)
			{
				// If the unit being attacked is already dead, then don't bother proceeding
				final int damageTaken = (unitBeingAttacked == defender) ? damageToDefender : damageToAttacker;
				if (damageTaken < getUnitCalculations ().calculateHitPointsRemaining (unitBeingAttacked.getUnit (), players, spells, combatAreaEffects, db))					
				{
					// Work out potential damage from the attack
					final AttackDamage potentialDamage;
					if (step == null)
						potentialDamage = commonPotentialDamageToDefenders;
					else
					{
						// Which unit is attacking?
						final AttackResolutionUnit unitMakingAttack = (step.getCombatSide () == UnitCombatSideID.ATTACKER) ? attacker : defender;
						if (unitMakingAttack == null)
							throw new MomException ("processAttackResolutionStep: Tried to process attack step from a null unitMakingAttack, attacking side = " + step.getCombatSide ());
						
						// What are they attacking with?
						if (step.getUnitAttributeID () != null)
							potentialDamage = getDamageCalculator ().attackFromUnitAttribute
								(unitMakingAttack, attackingPlayer, defendingPlayer, step.getUnitAttributeID (), players, spells, combatAreaEffects, db);
						
						else if (step.getUnitSkillID () != null)
							potentialDamage = getDamageCalculator ().attackFromUnitSkill
								(unitMakingAttack, attackingPlayer, defendingPlayer, step.getUnitSkillID (), players, spells, combatAreaEffects, db);
						
						else
							throw new MomException ("processAttackResolutionStep: Tried to process attack step that specifies neither an attribute ID or skill ID to attack with, side = " + step.getCombatSide ());
					}
					
					// We may get null here, if the step says to attack with a skill that this unit doesn't have
					if (potentialDamage != null)
					{
						// Work out how much of the damage gets through
						for (int repetitionNo = 0; repetitionNo < potentialDamage.getRepetitions (); repetitionNo++)
						{
							final int thisDamage;				
							switch (potentialDamage.getDamageType ())
							{
								case SINGLE_FIGURE:
									thisDamage = getDamageCalculator ().calculateSingleFigureDamage
										(unitBeingAttacked, attackingPlayer, defendingPlayer, potentialDamage,
										players, spells, combatAreaEffects, db); 
									break;
									
								case ARMOUR_PIERCING:
									thisDamage = getDamageCalculator ().calculateArmourPiercingDamage
										(unitBeingAttacked, attackingPlayer, defendingPlayer, potentialDamage,
										players, spells, combatAreaEffects, db); 
									break;
									
								case ILLUSIONARY:
									thisDamage = getDamageCalculator ().calculateIllusionaryDamage
										(unitBeingAttacked, attackingPlayer, defendingPlayer, potentialDamage,
										players, spells, combatAreaEffects, db); 
									break;
				
								case MULTI_FIGURE:
									thisDamage = getDamageCalculator ().calculateMultiFigureDamage
										(unitBeingAttacked, attackingPlayer, defendingPlayer, potentialDamage,
										players, spells, combatAreaEffects, db); 
									break;
									
								case DOOM:
									thisDamage = getDamageCalculator ().calculateDoomDamage
										(unitBeingAttacked, attackingPlayer, defendingPlayer, potentialDamage,
										players, spells, combatAreaEffects, db); 
									break;
			
								case CHANCE_OF_DEATH:
									thisDamage = getDamageCalculator ().calculateChanceOfDeathDamage
										(unitBeingAttacked, attackingPlayer, defendingPlayer, potentialDamage,
										players, spells, combatAreaEffects, db); 
									break;
			
								case EACH_FIGURE_RESIST_OR_DIE:
									thisDamage = getDamageCalculator ().calculateEachFigureResistOrDieDamage
										(unitBeingAttacked, attackingPlayer, defendingPlayer, potentialDamage,
										players, spells, combatAreaEffects, db); 
									break;
	
								case SINGLE_FIGURE_RESIST_OR_DIE:
									thisDamage = getDamageCalculator ().calculateSingleFigureResistOrDieDamage
										(unitBeingAttacked, attackingPlayer, defendingPlayer, potentialDamage,
										players, spells, combatAreaEffects, db); 
									break;
									
								case RESIST_OR_TAKE_DAMAGE:
									thisDamage = getDamageCalculator ().calculateResistOrTakeDamage
										(unitBeingAttacked, attackingPlayer, defendingPlayer, potentialDamage,
										players, spells, combatAreaEffects, db); 
									break;
									
								case RESISTANCE_ROLLS:
									thisDamage = getDamageCalculator ().calculateResistanceRollsDamage
										(unitBeingAttacked, attackingPlayer, defendingPlayer, potentialDamage,
										players, spells, combatAreaEffects, db); 
									break;
			
								case DISINTEGRATE:
									thisDamage = getDamageCalculator ().calculateDisintegrateDamage
										(unitBeingAttacked, attackingPlayer, defendingPlayer, potentialDamage,
										players, spells, combatAreaEffects, db); 
									break;
									
								case FEAR:
									thisDamage = 0;
									getDamageCalculator ().calculateFearDamage
										(unitBeingAttacked, attackingPlayer, defendingPlayer, potentialDamage,
										players, spells, combatAreaEffects, db); 
								break;
									
								case ZEROES_AMMO:
									thisDamage = 0;
									break;
									
								default:
									throw new MomException ("resolveAttack trying to deal attack damage of type " + potentialDamage.getDamageType () +
										" to the unitBeingAttacked, which it does not know how to deal with yet");
							}
							
							// Add damage to running total
							if (unitBeingAttacked == defender)
								damageToDefender = damageToDefender + thisDamage;
							else
								damageToAttacker = damageToAttacker + thisDamage;
						}
					
						// Apply any special effect
						switch (potentialDamage.getDamageType ())
						{
							case ZEROES_AMMO:
								unitBeingAttacked.getUnit ().setRangedAttackAmmo (0);
								
								// Make sure ammo is zeroed on the client as well
								if (!specialDamageTypesApplied.contains (potentialDamage.getDamageType ()))
									specialDamageTypesApplied.add (potentialDamage.getDamageType ());
								break;
								
							default:
								break;
						}
					}
				}
			}
		}

		// Each individual step will never allow a unit to be over-killed, i.e. take more damage than it has remaining HP.
		// But potentially we might have dealt multiple types of damage simultaneously which now added together total more than the unit's remaining HP,
		// so we have to check for that here.
		// e.g. unit has 4 HP remaining and we simultaneously hit it with a thrown attack that does 3 damage and a breath attack that does 2 damage.
		damageToDefender = Math.min (damageToDefender, getUnitCalculations ().calculateHitPointsRemaining (defender.getUnit (), players, spells, combatAreaEffects, db));
		if (attacker != null)
			damageToAttacker = Math.min (damageToAttacker, getUnitCalculations ().calculateHitPointsRemaining (attacker.getUnit (), players, spells, combatAreaEffects, db));
		
		// Apply the damage
		defender.getUnit ().setDamageTaken (defender.getUnit ().getDamageTaken () + damageToDefender);
		if (attacker != null)
			attacker.getUnit ().setDamageTaken (attacker.getUnit ().getDamageTaken () + damageToAttacker);

		log.trace ("Exiting processAttackResolutionStep = " + specialDamageTypesApplied.size ());
		return specialDamageTypesApplied;
	}

	/**
	 * @return Unit skill utils
	 */
	public final UnitSkillUtils getUnitSkillUtils ()
	{
		return unitSkillUtils;
	}

	/**
	 * @param utils Unit skill utils
	 */
	public final void setUnitSkillUtils (final UnitSkillUtils utils)
	{
		unitSkillUtils = utils;
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
}