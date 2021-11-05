package momime.server.process;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.calculations.UnitCalculations;
import momime.common.database.AttackResolution;
import momime.common.database.AttackResolutionCondition;
import momime.common.database.AttackResolutionStep;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DamageResolutionTypeID;
import momime.common.database.NegatedBySkill;
import momime.common.database.NegatedByUnitID;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitCombatSideID;
import momime.common.database.UnitSkill;
import momime.common.messages.CombatMapSize;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.UnitDamage;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.UnitUtils;
import momime.server.calculations.AttackDamage;
import momime.server.calculations.DamageCalculator;
import momime.server.calculations.ServerUnitCalculations;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.utils.UnitServerUtils;

/**
 * Methods for processing attack resolutions.  This would all just be part of DamageProcessor, these methods
 * are just moved out so they can be mocked separately in unit tests.
 */
public final class AttackResolutionProcessingImpl implements AttackResolutionProcessing
{
	/** Unit utils */
	private UnitUtils unitUtils;	
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;

	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;
	
	/** Server-only unit calculations */
	private ServerUnitCalculations serverUnitCalculations;
	
	/** Damage calc */
	private DamageCalculator damageCalculator;

	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/**
	 * When one unit initiates a basic attack in combat against another, determines the most appropriate attack resolution rules to deal with processing the attack.
	 * 
	 * @param attacker Unit making the attack (may be owned by the player that is defending in combat) 
	 * 	Note the attacker stats must be calculated listing the defender as the opponent, in order for Negate First Strike to work correctly 
	 * @param defender Unit being attacked (may be owned by the player that is attacking in combat)
	 * @param attackSkillID Which skill they are attacking with (melee or ranged)
	 * @param db Lookup lists built over the XML database
	 * @return Chosen attack resolution
	 * @throws RecordNotFoundException If the unit skill or so on can't be found in the XML database
	 * @throws MomException If no attack resolutions are appropriate, or if there are errors checking unit skills
	 */
	@Override
	public AttackResolution chooseAttackResolution (final ExpandedUnitDetails attacker, final ExpandedUnitDetails defender, final String attackSkillID,
		final CommonDatabase db) throws RecordNotFoundException, MomException
	{
		final UnitSkill unitSkill = db.findUnitSkill (attackSkillID, "chooseAttackResolution");
		
		// Check all possible attack resolutions to select the most appropriate one
		AttackResolution match = null;
		final Iterator<AttackResolution> iter = unitSkill.getAttackResolution ().iterator ();
		while ((match == null) && (iter.hasNext ()))
		{
			final AttackResolution attackResolution = iter.next ();
			
			// Check all the conditions
			boolean conditionsMatch = true;
			final Iterator<AttackResolutionCondition> conditions = attackResolution.getAttackResolutionCondition ().iterator ();
			while ((conditionsMatch) && (conditions.hasNext ()))
			{
				final AttackResolutionCondition condition = conditions.next ();
				
				// Check this condition; these are things like haste + first strike, so its ok to pass in nulls here - we don't know the actual attack steps yet
				final ExpandedUnitDetails unitToTest = (condition.getCombatSide () == UnitCombatSideID.ATTACKER) ? attacker : defender;
				if ((!unitToTest.hasModifiedSkill (condition.getUnitSkillID ())) &&
					((condition.getAlternativeUnitSkillID () == null) || (!unitToTest.hasModifiedSkill (condition.getAlternativeUnitSkillID ()))))
					
					conditionsMatch = false;
			}
			
			if (conditionsMatch)
				match = attackResolution;
		}
		
		// Its an error if we fail to find any match at all
		if (match == null)
			throw new MomException ("Unit URN " + attacker.getUnitURN () + " tried to hit Unit URN " + defender.getUnitURN () + " with attack of type " + attackSkillID +
				" but there are no defined attack resolutions capable of processing the attack");
		
		return match;
	}
	
	/**
	 * @param steps Steps in one continuous list
	 * @return Same list as input, but segmented into sublists where all steps share the same step number; also wraps each step in the container class
	 * @throws MomException If the steps in the input list aren't in stepNumber order
	 */
	@Override
	public final List<List<AttackResolutionStepContainer>> splitAttackResolutionStepsByStepNumber (final List<AttackResolutionStep> steps)
		throws MomException
	{
		final List<List<AttackResolutionStepContainer>> result = new ArrayList<List<AttackResolutionStepContainer>> ();
		
		int currentStepNumber = 0;
		List<AttackResolutionStepContainer> currentList = null;
		for (final AttackResolutionStep step : steps)
		{
			// Only acceptable values are currentStepNumber, or currentStepNumber+1
			if (step.getStepNumber () <= 0)
				throw new MomException ("Found an attack resolution step with step number of " + step.getStepNumber ());
			
			else if (step.getStepNumber () == currentStepNumber)
				currentList.add (new AttackResolutionStepContainer (step));
			
			else if (step.getStepNumber () == currentStepNumber + 1)
			{
				currentStepNumber = currentStepNumber + 1;
				currentList = new ArrayList<AttackResolutionStepContainer> ();
				currentList.add (new AttackResolutionStepContainer (step));
				result.add (currentList);
			}
			
			else
				throw new MomException ("Found an attack resolution with steps out of order - jumped from " + currentStepNumber + " to " + step.getStepNumber ());
		}

		return result;
	}
	
	/**
	 * Executes all of the steps of an attack sequence that have the same step number, i.e. all those steps such that damage is calculated and applied simultaneously.
	 * 
	 * @param attacker Unit making the attack; or null if the attack isn't coming from a unit 
	 * @param defender Unit being attacked
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param combatLocation Location the combat is taking place; null if its damage from an overland spell
	 * @param steps The steps to take, i.e. all of the steps defined under the chosen attackResolution that have the same stepNumber
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
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final MapCoordinates3DEx combatLocation,
		final List<AttackResolutionStepContainer> steps,
		final List<PlayerServerDetails> players, final FogOfWarMemory mem, final CombatMapSize combatMapCoordinateSystem, final CommonDatabase db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		final ServerGridCellEx tc = (combatLocation == null) ? null : (ServerGridCellEx) mem.getMap ().getPlane ().get
			(combatLocation.getZ ()).getRow ().get (combatLocation.getY ()).getCell ().get (combatLocation.getX ());
		
		final MapAreaOfCombatTiles combatMap = (tc == null) ? null : tc.getCombatMap ();
		
		// Zero out damage taken
		final List<UnitDamage> damageToDefender = new ArrayList<UnitDamage> ();
		final List<UnitDamage> damageToAttacker = new ArrayList<UnitDamage> ();
		
		int lifeStealingDamageToDefender = 0;
		int lifeStealingDamageToAttacker = 0;
		
		// Calculate and total up all the damage before we apply any of it
		final List<DamageResolutionTypeID> specialDamageResolutionsApplied = new ArrayList<DamageResolutionTypeID> ();
		for (final AttackResolutionStepContainer step : steps)
		{
			// Which unit is being attacked?
			final AttackResolutionUnit unitBeingAttacked = (step.getCombatSide () == UnitCombatSideID.ATTACKER) ? defender : attacker;
			if (unitBeingAttacked == null)
				throw new MomException ("processAttackResolutionStep: Tried to process attack step from a null unitBeingAttacked, attacking side = " + step.getCombatSide ());

			final int stepRepetitions = step.getRepetitions ();
			for (int stepRepetitionNo = 0; stepRepetitionNo < stepRepetitions; stepRepetitionNo++)
			{
				// If the unit being attacked is already dead, then don't bother proceeding
				final ExpandedUnitDetails xuUnitBeingAttackedHPcheck = getExpandUnitDetails ().expandUnitDetails (unitBeingAttacked.getUnit (), null, null, null, players, mem, db);
				
				final List<UnitDamage> damageTaken = (unitBeingAttacked == defender) ? damageToDefender : damageToAttacker;
				if (getUnitUtils ().getTotalDamageTaken (damageTaken) < xuUnitBeingAttackedHPcheck.calculateHitPointsRemaining ())					
				{
					// Work out potential damage from the attack
					final AttackDamage potentialDamage;
					final AttackResolutionUnit unitMakingAttack;
					final ExpandedUnitDetails xuUnitMakingAttack;
					if (step.getSpellStep () != null)
					{
						// If this particular defender is immune to an illusionary attack, then temporarily set the spell damage resolution type to normal
						boolean downgradeIllusionaryAttack = false;
						if ((step.getSpellStep ().getDamageResolutionTypeID () == DamageResolutionTypeID.ILLUSIONARY))
						{
							// Borrow the list of immunities from the Illusionary Attack skill - I don't want to have to define immunties to damage types in the XSD just for this
							final Iterator<NegatedBySkill> iter = db.findUnitSkill
								(CommonDatabaseConstants.UNIT_SKILL_ID_ILLUSIONARY_ATTACK, "processAttackResolutionStep").getNegatedBySkill ().iterator ();
							while ((!downgradeIllusionaryAttack) && (iter.hasNext ()))
							{
								final NegatedBySkill negateIllusionaryAttack = iter.next ();
								if ((negateIllusionaryAttack.getNegatedByUnitID () == NegatedByUnitID.ENEMY_UNIT) && (xuUnitBeingAttackedHPcheck.hasModifiedSkill (negateIllusionaryAttack.getNegatedBySkillID ())))
									downgradeIllusionaryAttack = true;
							}
						}
						
						potentialDamage = downgradeIllusionaryAttack ?
							new AttackDamage (step.getSpellStep (), DamageResolutionTypeID.SINGLE_FIGURE) : step.getSpellStep ();
						
						unitMakingAttack = null;
						xuUnitMakingAttack = null;
					}
					else
					{
						// Which unit is attacking?
						unitMakingAttack = (step.getCombatSide () == UnitCombatSideID.ATTACKER) ? attacker : defender;
						if (unitMakingAttack == null)
							throw new MomException ("processAttackResolutionStep: Tried to process attack step from a null unitMakingAttack, attacking side = " + step.getCombatSide ());
						
						xuUnitMakingAttack = getExpandUnitDetails ().expandUnitDetails (unitMakingAttack.getUnit (), null, null, null, players, mem, db);
						
						// If this is a hasted ranged attack, make sure we actually have enough ammo to make both attacks
						if ((CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK.equals (step.getUnitSkillStep ().getUnitSkillID ())) &&
							(!getUnitCalculations ().canMakeRangedAttack (xuUnitMakingAttack)))
							
							potentialDamage = null;
						else
							potentialDamage = getDamageCalculator ().attackFromUnitSkill
								(unitMakingAttack, unitBeingAttacked, attackingPlayer, defendingPlayer, step.getUnitSkillStep ().getUnitSkillID (), players, mem, db);
					}
					
					// We may get null here, if the step says to attack with a skill that this unit doesn't have
					if (potentialDamage != null)
					{
						// Work out to hit penalties
						int penalty = 0;
						
						// If its a non-magical ranged attack, work out any distance penalty
						if ((step.getUnitSkillStep () != null) && (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK.equals (step.getUnitSkillStep ().getUnitSkillID ())))
							penalty = penalty + getServerUnitCalculations ().calculateRangedAttackDistancePenalty
								(xuUnitMakingAttack, xuUnitBeingAttackedHPcheck, combatMapCoordinateSystem);
						
						// If the unit has suffered too many attacks, its counterattack to hit chance goes down
						if ((step != null) && (step.getCombatSide () == UnitCombatSideID.DEFENDER))
						{
							final Integer numberOfTimedAttacked = tc.getNumberOfTimedAttacked ().get (unitMakingAttack.getUnit ().getUnitURN ());
							if ((numberOfTimedAttacked != null) && (numberOfTimedAttacked >= 2))
								penalty = penalty + (numberOfTimedAttacked / 2);
						}

						// Can't reduce chance below 10%
						if (penalty > 0)
						{
							final int newChanceToHitWithPenalty = potentialDamage.getChanceToHit () - penalty;							
							potentialDamage.setChanceToHit (Math.max (newChanceToHitWithPenalty, 1));
						}
						
						// Work out how much of the damage gets through
						for (int repetitionNo = 0; repetitionNo < potentialDamage.getRepetitions (); repetitionNo++)
						{
							// Now we know all the details about the type of attack, we can properly generate stats of the
							// unit being attacked, since it might have bonuses against certain kinds of incoming attack so
							// can't just generate its details using nulls for the attack details
							final List<ExpandedUnitDetails> xuUnitsMakingAttack;
							if (xuUnitMakingAttack == null)
								xuUnitsMakingAttack = null;
							else
							{
								xuUnitsMakingAttack = new ArrayList<ExpandedUnitDetails> ();
								xuUnitsMakingAttack.add (xuUnitMakingAttack);
							}
							
							final ExpandedUnitDetails xuUnitBeingAttacked = getExpandUnitDetails ().expandUnitDetails (unitBeingAttacked.getUnit (), xuUnitsMakingAttack,
								potentialDamage.getAttackFromSkillID (), potentialDamage.getAttackFromMagicRealmID (), players, mem, db);
							
							final int thisDamage;				
							switch (potentialDamage.getDamageResolutionTypeID ())
							{
								case SINGLE_FIGURE:
									thisDamage = getDamageCalculator ().calculateSingleFigureDamage (xuUnitBeingAttacked, xuUnitMakingAttack,
										attackingPlayer, defendingPlayer, potentialDamage, combatLocation, combatMap, mem.getBuilding (), db);
									break;
									
								case ARMOUR_PIERCING:
									thisDamage = getDamageCalculator ().calculateArmourPiercingDamage (xuUnitBeingAttacked, xuUnitMakingAttack,
										attackingPlayer, defendingPlayer, potentialDamage, combatLocation, combatMap, mem.getBuilding (), db);
									break;
									
								case ILLUSIONARY:
									thisDamage = getDamageCalculator ().calculateIllusionaryDamage (xuUnitBeingAttacked,  xuUnitMakingAttack,
										attackingPlayer, defendingPlayer, potentialDamage, combatLocation, combatMap, mem.getBuilding (), db);
									break;
				
								case MULTI_FIGURE:
									thisDamage = getDamageCalculator ().calculateMultiFigureDamage (xuUnitBeingAttacked, xuUnitMakingAttack,
										attackingPlayer, defendingPlayer, potentialDamage, combatLocation, combatMap, mem.getBuilding (), db);
									break;
									
								case DOOM:
									thisDamage = getDamageCalculator ().calculateDoomDamage
										(xuUnitBeingAttacked, attackingPlayer, defendingPlayer, potentialDamage, db);
									break;
			
								case CHANCE_OF_DEATH:
									thisDamage = getDamageCalculator ().calculateChanceOfDeathDamage
										(xuUnitBeingAttacked, attackingPlayer, defendingPlayer, potentialDamage, db);
									break;
			
								case EACH_FIGURE_RESIST_OR_DIE:
									thisDamage = getDamageCalculator ().calculateEachFigureResistOrDieDamage
										(xuUnitBeingAttacked, attackingPlayer, defendingPlayer, potentialDamage);
									break;
	
								case EACH_FIGURE_RESIST_OR_LOSE_1HP:
									thisDamage = getDamageCalculator ().calculateEachFigureResistOrLose1HPDamage
										(xuUnitBeingAttacked, attackingPlayer, defendingPlayer, potentialDamage);
									break;
	
								case SINGLE_FIGURE_RESIST_OR_DIE:
									thisDamage = getDamageCalculator ().calculateSingleFigureResistOrDieDamage
										(xuUnitBeingAttacked, attackingPlayer, defendingPlayer, potentialDamage);
									break;
									
								case RESIST_OR_TAKE_DAMAGE:
									thisDamage = getDamageCalculator ().calculateResistOrTakeDamage
										(xuUnitBeingAttacked, attackingPlayer, defendingPlayer, potentialDamage, db);
									break;
									
								case RESISTANCE_ROLLS:
									thisDamage = getDamageCalculator ().calculateResistanceRollsDamage
										(xuUnitBeingAttacked, attackingPlayer, defendingPlayer, potentialDamage, db);
									break;
			
								case DISINTEGRATE:
									thisDamage = getDamageCalculator ().calculateDisintegrateDamage
										(xuUnitBeingAttacked, attackingPlayer, defendingPlayer, potentialDamage, db);
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
									throw new MomException ("processAttackResolutionStep trying to deal attack damage of type " + potentialDamage.getDamageResolutionTypeID () +
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
		final ExpandedUnitDetails xuDefender = getExpandUnitDetails ().expandUnitDetails (defender.getUnit (), null, null, null, players, mem, db);
		getUnitServerUtils ().healDamage (defender.getUnit ().getUnitDamage (), -xuDefender.calculateHitPointsRemaining (), true);
		if (attacker != null)
		{
			final ExpandedUnitDetails xuAttacker = getExpandUnitDetails ().expandUnitDetails (attacker.getUnit (), null, null, null, players, mem, db);
			getUnitServerUtils ().healDamage (attacker.getUnit ().getUnitDamage (), -xuAttacker.calculateHitPointsRemaining (), true);
		}
		
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