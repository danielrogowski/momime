package momime.server.process;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DamageResolutionTypeID;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitCombatSideID;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillPositiveNegative;
import momime.common.messages.CombatMapSize;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;
import momime.common.messages.UnitDamage;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.UnitSkillUtils;
import momime.common.utils.UnitUtils;
import momime.server.calculations.AttackDamage;
import momime.server.calculations.DamageCalculator;
import momime.server.calculations.ServerUnitCalculations;
import momime.server.database.AttackResolutionConditionSvr;
import momime.server.database.AttackResolutionStepSvr;
import momime.server.database.AttackResolutionSvr;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.UnitSkillSvr;
import momime.server.utils.UnitServerUtils;

/**
 * Methods for processing attack resolutions.  This would all just be part of DamageProcessor, these methods
 * are just moved out so they can be mocked separately in unit tests.
 */
public final class AttackResolutionProcessingImpl implements AttackResolutionProcessing
{
	/** Class logger */
	private final Log log = LogFactory.getLog (AttackResolutionProcessingImpl.class);

	/** Unit utils */
	private UnitUtils unitUtils;	
	
	/** Unit skill utils */
	private UnitSkillUtils unitSkillUtils;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;

	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;
	
	/** Server-only unit calculations */
	private ServerUnitCalculations serverUnitCalculations;
	
	/** Damage calc */
	private DamageCalculator damageCalculator;
	
	/**
	 * When one unit initiates a basic attack in combat against another, determines the most appropriate attack resolution rules to deal with processing the attack.
	 * 
	 * @param attacker Unit making the attack (may be owned by the player that is defending in combat) 
	 * @param defender Unit being attacked (may be owned by the player that is attacking in combat)
	 * @param attackSkillID Which skill they are attacking with (melee or ranged)
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Chosen attack resolution
	 * @throws RecordNotFoundException If the unit skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If no attack resolutions are appropriate, or if there are errors checking unit skills
	 */
	@Override
	public AttackResolutionSvr chooseAttackResolution (final MemoryUnit attacker, final MemoryUnit defender, final String attackSkillID,
		final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering chooseAttackResolution: Unit URN " + attacker.getUnitURN () + " hitting Unit URN " + defender.getUnitURN () + " with " + attackSkillID);
	
		final UnitSkillSvr unitSkill = db.findUnitSkill (attackSkillID, "chooseAttackResolution");
		
		// Check all possible attack resolutions to select the most appropriate one
		AttackResolutionSvr match = null;
		final Iterator<AttackResolutionSvr> iter = unitSkill.getAttackResolutions ().iterator ();
		while ((match == null) && (iter.hasNext ()))
		{
			final AttackResolutionSvr attackResolution = iter.next ();
			
			// Check all the conditions
			boolean conditionsMatch = true;
			final Iterator<AttackResolutionConditionSvr> conditions = attackResolution.getAttackResolutionConditions ().iterator ();
			while ((conditionsMatch) && (conditions.hasNext ()))
			{
				final AttackResolutionConditionSvr condition = conditions.next ();
				
				// Check this condition; these are things like haste + first strike, so its ok to pass in nulls here - we don't know the actual attack steps yet
				final MemoryUnit unitToTest = (condition.getCombatSide () == UnitCombatSideID.ATTACKER) ? attacker : defender;
				final MemoryUnit enemy = (condition.getCombatSide () == UnitCombatSideID.ATTACKER) ? defender : attacker;
				final List<MemoryUnit> enemies = new ArrayList<MemoryUnit> ();
				enemies.add (enemy);
				
				if (getUnitSkillUtils ().getModifiedSkillValue (unitToTest, unitToTest.getUnitHasSkill (), condition.getUnitSkillID (), enemies,
					UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, mem, db) < 0)
					
					conditionsMatch = false;
			}
			
			if (conditionsMatch)
				match = attackResolution;
		}
		
		// Its an error if we fail to find any match at all
		if (match == null)
			throw new MomException ("Unit URN " + attacker.getUnitURN () + " tried to hit Unit URN " + defender.getUnitURN () + " with attack of type " + attackSkillID +
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
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return List of special damage resolutions done to the defender (used for warp wood); limitation that client assumes this damage type is applied to ALL defenders
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit or other rule errors
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final List<DamageResolutionTypeID> processAttackResolutionStep (final AttackResolutionUnit attacker, final AttackResolutionUnit defender,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final List<AttackResolutionStepSvr> steps, final AttackDamage commonPotentialDamageToDefenders,
		final List<PlayerServerDetails> players, final FogOfWarMemory mem, final CombatMapSize combatMapCoordinateSystem, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering processAttackResolutionStep: Attacking unit URN " + ((attacker != null) ? new Integer (attacker.getUnit ().getUnitURN ()).toString () : "N/A") +
			", Defending unit URN " + defender.getUnit ().getUnitURN () + ", " + steps.size () + " steps");

		// Zero out damage taken
		final List<UnitDamage> damageToDefender = new ArrayList<UnitDamage> ();
		final List<UnitDamage> damageToAttacker = new ArrayList<UnitDamage> ();
		
		int lifeStealingDamageToDefender = 0;
		int lifeStealingDamageToAttacker = 0;
		
		// Calculate and total up all the damage before we apply any of it
		final List<DamageResolutionTypeID> specialDamageResolutionsApplied = new ArrayList<DamageResolutionTypeID> ();
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
				final ExpandedUnitDetails xuUnitBeingAttackedHPcheck = getUnitUtils ().expandUnitDetails (unitBeingAttacked.getUnit (), null, null, null, players, mem, db);
				
				final List<UnitDamage> damageTaken = (unitBeingAttacked == defender) ? damageToDefender : damageToAttacker;
				if (getUnitUtils ().getTotalDamageTaken (damageTaken) < xuUnitBeingAttackedHPcheck.calculateHitPointsRemaining ())					
				{
					// Work out potential damage from the attack
					final AttackDamage potentialDamage;
					final AttackResolutionUnit unitMakingAttack;
					if (step == null)
					{
						potentialDamage = commonPotentialDamageToDefenders;
						unitMakingAttack = null;
					}
					else
					{
						// Which unit is attacking?
						unitMakingAttack = (step.getCombatSide () == UnitCombatSideID.ATTACKER) ? attacker : defender;
						if (unitMakingAttack == null)
							throw new MomException ("processAttackResolutionStep: Tried to process attack step from a null unitMakingAttack, attacking side = " + step.getCombatSide ());
						
						// If this is a hasted ranged attack, make sure we actually have enough ammo to make both attacks
						if ((CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK.equals (step.getUnitSkillID ())) &&
							(!getUnitCalculations ().canMakeRangedAttack (unitMakingAttack.getUnit (), players, mem, db)))
							
							potentialDamage = null;
						else
							potentialDamage = getDamageCalculator ().attackFromUnitSkill
								(unitMakingAttack, unitBeingAttacked, attackingPlayer, defendingPlayer, step.getUnitSkillID (), players, mem, db);
					}
					
					// We may get null here, if the step says to attack with a skill that this unit doesn't have
					if (potentialDamage != null)
					{
						// If its a non-magical ranged attack, work out any distance penalty
						if ((step != null) && (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK.equals (step.getUnitSkillID ())))
						{
							final int penalty = getServerUnitCalculations ().calculateRangedAttackDistancePenalty
								(attacker.getUnit (), defender.getUnit (), combatMapCoordinateSystem, players, mem, db);
							
							if (penalty > 0)
							{
								final int newChanceToHitWithPenalty = potentialDamage.getChanceToHit () - penalty;
								
								// Can't reduce chance below 10%
								potentialDamage.setChanceToHit (Math.max (newChanceToHitWithPenalty, 1));
							}
						}
						
						// Work out how much of the damage gets through
						for (int repetitionNo = 0; repetitionNo < potentialDamage.getRepetitions (); repetitionNo++)
						{
							// Now we know all the details about the type of attack, we can properly generate stats of the
							// unit being attacked, since it might have bonuses against certain kinds of incoming attack so
							// can't just generate its details using nulls for the attack details
							final List<ExpandedUnitDetails> xuUnitsMakingAttack;
							if (unitMakingAttack == null)
								xuUnitsMakingAttack = null;
							else
							{
								xuUnitsMakingAttack = new ArrayList<ExpandedUnitDetails> ();
								xuUnitsMakingAttack.add (getUnitUtils ().expandUnitDetails (unitMakingAttack.getUnit (), null, null, null, players, mem, db));
							}
							
							final ExpandedUnitDetails xuUnitBeingAttacked = getUnitUtils ().expandUnitDetails (unitBeingAttacked.getUnit (), xuUnitsMakingAttack,
								potentialDamage.getAttackFromSkillID (), potentialDamage.getAttackFromMagicRealmID (), players, mem, db);
							
							final int thisDamage;				
							switch (potentialDamage.getDamageResolutionTypeID ())
							{
								case SINGLE_FIGURE:
									thisDamage = getDamageCalculator ().calculateSingleFigureDamage (xuUnitBeingAttacked,
										(unitMakingAttack == null) ? null : unitMakingAttack.getUnit (),
										attackingPlayer, defendingPlayer, potentialDamage,
										players, mem, db); 
									break;
									
								case ARMOUR_PIERCING:
									thisDamage = getDamageCalculator ().calculateArmourPiercingDamage (xuUnitBeingAttacked,
										(unitMakingAttack == null) ? null : unitMakingAttack.getUnit (),
										attackingPlayer, defendingPlayer, potentialDamage, players, mem, db); 
									break;
									
								case ILLUSIONARY:
									thisDamage = getDamageCalculator ().calculateIllusionaryDamage
										(xuUnitBeingAttacked, attackingPlayer, defendingPlayer, potentialDamage);
									break;
				
								case MULTI_FIGURE:
									thisDamage = getDamageCalculator ().calculateMultiFigureDamage (xuUnitBeingAttacked,
										(unitMakingAttack == null) ? null : unitMakingAttack.getUnit (),
										attackingPlayer, defendingPlayer, potentialDamage, players, mem, db); 
									break;
									
								case DOOM:
									thisDamage = getDamageCalculator ().calculateDoomDamage
										(xuUnitBeingAttacked, attackingPlayer, defendingPlayer, potentialDamage);
									break;
			
								case CHANCE_OF_DEATH:
									thisDamage = getDamageCalculator ().calculateChanceOfDeathDamage
										(xuUnitBeingAttacked, attackingPlayer, defendingPlayer, potentialDamage);
									break;
			
								case EACH_FIGURE_RESIST_OR_DIE:
									thisDamage = getDamageCalculator ().calculateEachFigureResistOrDieDamage
										(xuUnitBeingAttacked, attackingPlayer, defendingPlayer, potentialDamage);
									break;
	
								case SINGLE_FIGURE_RESIST_OR_DIE:
									thisDamage = getDamageCalculator ().calculateSingleFigureResistOrDieDamage
										(xuUnitBeingAttacked, attackingPlayer, defendingPlayer, potentialDamage);
									break;
									
								case RESIST_OR_TAKE_DAMAGE:
									thisDamage = getDamageCalculator ().calculateResistOrTakeDamage
										(xuUnitBeingAttacked, attackingPlayer, defendingPlayer, potentialDamage);
									break;
									
								case RESISTANCE_ROLLS:
									thisDamage = getDamageCalculator ().calculateResistanceRollsDamage
										(xuUnitBeingAttacked, attackingPlayer, defendingPlayer, potentialDamage);
									break;
			
								case DISINTEGRATE:
									thisDamage = getDamageCalculator ().calculateDisintegrateDamage
										(xuUnitBeingAttacked, attackingPlayer, defendingPlayer, potentialDamage);
									break;
									
								case FEAR:
									thisDamage = 0;
									getDamageCalculator ().calculateFearDamage
										(unitBeingAttacked, xuUnitBeingAttacked, attackingPlayer, defendingPlayer, potentialDamage);
								break;
									
								case ZEROES_AMMO:
									thisDamage = 0;
									break;
									
								default:
									throw new MomException ("resolveAttack trying to deal attack damage of type " + potentialDamage.getDamageResolutionTypeID () +
										" to the unitBeingAttacked, which it does not know how to deal with yet");
							}
							
							// Add damage to running total
							if (thisDamage != 0)
							{
								getUnitServerUtils ().addDamage ((unitBeingAttacked == defender) ? damageToDefender : damageToAttacker,
									potentialDamage.getDamageType ().getStoredDamageTypeID (), thisDamage);
								
								if (CommonDatabaseConstants.UNIT_SKILL_ID_LIFE_STEALING.equals (potentialDamage.getAttackFromSkillID ()))
								{
									if (unitBeingAttacked == defender)
										lifeStealingDamageToDefender = lifeStealingDamageToDefender + thisDamage;
									else
										lifeStealingDamageToAttacker = lifeStealingDamageToAttacker + thisDamage;
								}
							}
						}
					
						// Apply any special effect
						switch (potentialDamage.getDamageResolutionTypeID ())
						{
							case ZEROES_AMMO:
								unitBeingAttacked.getUnit ().setAmmoRemaining (0);
								
								// Make sure ammo is zeroed on the client as well
								if (!specialDamageResolutionsApplied.contains (potentialDamage.getDamageResolutionTypeID ()))
									specialDamageResolutionsApplied.add (potentialDamage.getDamageResolutionTypeID ());
								break;
								
							default:
								break;
						}
					}
				}
			}
		}

		// Add each type of damage to the units' totals
		damageToDefender.forEach (d -> getUnitServerUtils ().addDamage (defender.getUnit ().getUnitDamage (), d.getDamageType (), d.getDamageTaken ()));
		if (attacker != null)
			damageToAttacker.forEach (d -> getUnitServerUtils ().addDamage (attacker.getUnit ().getUnitDamage (), d.getDamageType (), d.getDamageTaken ()));
		
		// Life stealing units like Wraiths might get some health back
		if (lifeStealingDamageToDefender > 0)
		{
			final int amountToHeal = Math.min (lifeStealingDamageToDefender, getUnitUtils ().getHealableDamageTaken (attacker.getUnit ().getUnitDamage ()));
			
			if (amountToHeal > 0)
				getUnitServerUtils ().healDamage (attacker.getUnit ().getUnitDamage (), amountToHeal, false);
		}

		if (lifeStealingDamageToAttacker > 0)
		{
			final int amountToHeal = Math.min (lifeStealingDamageToAttacker, getUnitUtils ().getHealableDamageTaken (defender.getUnit ().getUnitDamage ()));
			
			if (amountToHeal > 0)
				getUnitServerUtils ().healDamage (defender.getUnit ().getUnitDamage (), amountToHeal, false);
		}
		
		// Each individual step will never allow a unit to be over-killed, i.e. take more damage than it has remaining HP.
		// But potentially we might have dealt multiple types of damage simultaneously which now added together total more than the unit's remaining HP,
		// so we might have over-killed the unit.  In that situation, keep healing 1 HP of damage until it no longer has negative health.
		// e.g. unit has 4 HP remaining and we simultaneously hit it with a thrown attack that does 3 damage and a breath attack that does 2 damage.
		
		// Note interesting implication of the way I've implemented this by healing negative health after it is applied - this means we might heal damage types
		// other than what was just applied - we heal the unit by killing it!  e.g. unit has 6 HP in total, but has already taken 4 hits of healable damage and only has 2 HP left.
		// It gets attacked by a life stealing attack, which does 4 hits of damage.  If we pre-shrunk the damage down to 2 hits of life steal damage, the unit
		// would fail to become undead and it would generally be very difficult to get special damages to take effect.
		// Instead we apply both, then the unit has -2 HP, and the heal routine will always heal healable damage first.
		// So we convert 2 HP of the healable damage already taken into life stealing damage, ending up with the unit dying from
		// taking 2 HP healable damage and 4 HP life stealing damage, and so it becomes undead.
		final ExpandedUnitDetails xuDefender = getUnitUtils ().expandUnitDetails (defender.getUnit (), null, null, null, players, mem, db);
		getUnitServerUtils ().healDamage (defender.getUnit ().getUnitDamage (), -xuDefender.calculateHitPointsRemaining (), true);
		if (attacker != null)
		{
			final ExpandedUnitDetails xuAttacker = getUnitUtils ().expandUnitDetails (attacker.getUnit (), null, null, null, players, mem, db);
			getUnitServerUtils ().healDamage (attacker.getUnit ().getUnitDamage (), -xuAttacker.calculateHitPointsRemaining (), true);
		}
		
		log.trace ("Exiting processAttackResolutionStep = " + specialDamageResolutionsApplied.size ());
		return specialDamageResolutionsApplied;
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
	 * @param calc Unit calculations
	 */
	public final void setUnitCalculations (final UnitCalculations calc)
	{
		unitCalculations = calc;
	}

	/**
	 * @return Server-only unit calculations
	 */
	public final ServerUnitCalculations getServerUnitCalculations ()
	{
		return serverUnitCalculations;
	}

	/**
	 * @param calc Server-only unit calculations
	 */
	public final void setServerUnitCalculations (final ServerUnitCalculations calc)
	{
		serverUnitCalculations = calc;
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