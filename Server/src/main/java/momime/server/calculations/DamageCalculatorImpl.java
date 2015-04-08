package momime.server.calculations;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DamageTypeID;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.DamageCalculationAttackData;
import momime.common.messages.servertoclient.DamageCalculationData;
import momime.common.messages.servertoclient.DamageCalculationDefenceData;
import momime.common.messages.servertoclient.DamageCalculationMessage;
import momime.common.messages.servertoclient.DamageCalculationMessageTypeID;
import momime.common.utils.UnitAttributeComponent;
import momime.common.utils.UnitAttributePositiveNegative;
import momime.common.utils.UnitUtils;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.SpellSvr;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

/**
 * Routines for making all the different kinds of damage rolls to see how many HP a unit attacking another unit knocks off.
 * This doesn't deal with actually applying the damage to the units, so that damage calculations for all combat actions in
 * a single step can all be called in succession, added up and applied in one go.
 * 
 * e.g. attacker's melee and poison attacks, and defender's counterattack would be 3 calls to damage calc routines
 * but the damage is all applied at once in DamageProcessorImpl.
 */
public final class DamageCalculatorImpl implements DamageCalculator
{
	/** Class logger */
	private final Log log = LogFactory.getLog (DamageCalculatorImpl.class);

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** Random number generator */
	private RandomUtils randomUtils;

	/**
	 * Just deals with making sure we only send to human players
	 * 
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param msg Message to send
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final void sendDamageCalculationMessage (final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final DamageCalculationData msg)
		throws JAXBException, XMLStreamException
	{
		if ((attackingPlayer.getPlayerDescription ().isHuman ()) || (defendingPlayer.getPlayerDescription ().isHuman ()))
		{
			final DamageCalculationMessage wrapper = new DamageCalculationMessage ();
			wrapper.setBreakdown (msg);
			
			if (attackingPlayer.getPlayerDescription ().isHuman ())
				attackingPlayer.getConnection ().sendMessageToClient (wrapper);
			
			if (defendingPlayer.getPlayerDescription ().isHuman ())
				defendingPlayer.getConnection ().sendMessageToClient (wrapper);
		}
	}

	/**
	 * Calculates the strength of an attack coming from a unit, i.e. a regular melee or ranged attack.
	 * 
	 * @param attacker Unit making the attack
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackAttributeID The attribute being used to attack, i.e. UA01 (swords) or UA02 (ranged)
	 * @param players Players list
	 * @param spells Known spells
	 * @param combatAreaEffects Known combat area effects
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final AttackDamage attackFromUnit (final MemoryUnit attacker, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final String attackAttributeID, final List<PlayerServerDetails> players,
		final List<MemoryMaintainedSpell> spells, final List<MemoryCombatAreaEffect> combatAreaEffects, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering attackFromUnit: Unit URN " + attacker.getUnitURN () + ", " + attackAttributeID);
		
		// Start breakdown message
		final DamageCalculationAttackData damageCalculationMsg = new DamageCalculationAttackData ();
		damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.ATTACK_DATA);
		damageCalculationMsg.setAttackerUnitURN (attacker.getUnitURN ());
		damageCalculationMsg.setAttackerPlayerID (attacker.getOwningPlayerID ());
		damageCalculationMsg.setAttackAttributeID (attackAttributeID);

		// How many potential hits can we make - See page 285 in the strategy guide
		damageCalculationMsg.setAttackerFigures (getUnitCalculations ().calculateAliveFigureCount (attacker, players, spells, combatAreaEffects, db));
		damageCalculationMsg.setAttackStrength (getUnitUtils ().getModifiedAttributeValue (attacker, attackAttributeID,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db));
		damageCalculationMsg.setPotentialHits (damageCalculationMsg.getAttackerFigures () * damageCalculationMsg.getAttackStrength ());
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);

		final int plusToHit = getUnitUtils ().getModifiedAttributeValue (attacker, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db);

		final AttackDamage attackDamage = new AttackDamage (damageCalculationMsg.getPotentialHits (), plusToHit, DamageTypeID.SINGLE_FIGURE);
		log.trace ("Exiting attackFromUnit = " + attackDamage);
		return attackDamage;
	}
	
	/**
	 * Calculates the strength of an attack coming from a spell.
	 *  
	 * @param spell The spell being cast
	 * @param variableDamage The damage chosen, for spells where variable mana can be channeled into casting them, e.g. fire bolt
	 * @param castingPlayer The player casting the spell
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final AttackDamage attackFromSpell (final SpellSvr spell, final Integer variableDamage,
		final PlayerServerDetails castingPlayer, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer)
		throws JAXBException, XMLStreamException
	{
		log.trace ("Entering attackFromSpell: Unit URN " + spell.getSpellID () + ", " + variableDamage);
		
		// Work out damage done - note this isn't applicable to all types of attack, e.g. Warp Wood has no attack value, so we might get null here
		final Integer damage = (variableDamage != null) ? variableDamage : spell.getCombatBaseDamage (); 
		
		// Start breakdown message
		final DamageCalculationAttackData damageCalculationMsg = new DamageCalculationAttackData ();
		damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.ATTACK_DATA);
		damageCalculationMsg.setAttackerPlayerID (castingPlayer.getPlayerDescription ().getPlayerID ());
		damageCalculationMsg.setAttackSpellID (spell.getSpellID ());
		damageCalculationMsg.setPotentialHits (damage);
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);

		final AttackDamage attackDamage = new AttackDamage (damage, 0, spell.getAttackSpellDamageType ());
		log.trace ("Exiting attackFromSpell = " + attackDamage);
		return attackDamage;
	}
	
	/**
	 * Rolls the number of actual hits and blocks for normal "single figure" type damage, where the first figure defends then takes hits, then the
	 * second figure defends and takes hits, so each figure defends and is then (maybe) killed off individually, until all damage is
	 * absorbed or every figure is killed.
	 * 
	 * @param defender Unit being hit
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @param players Players list
	 * @param spells Known spells
	 * @param combatAreaEffects Known combat area effects
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateSingleFigureDamage (final MemoryUnit defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final List<PlayerServerDetails> players,
		final List<MemoryMaintainedSpell> spells, final List<MemoryCombatAreaEffect> combatAreaEffects, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering calculateSingleFigureDamage: Unit URN " + defender.getUnitURN () + " hit by " + attackDamage);
		
		final int defenderDefenceStrength = getUnitUtils ().getModifiedAttributeValue (defender, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db);
		
		final int totalHits = calculateSingleFigureDamageInternal (defender, defenderDefenceStrength, DamageTypeID.SINGLE_FIGURE,
			attackingPlayer, defendingPlayer, attackDamage, players, spells, combatAreaEffects, db);
		
		log.trace ("Exiting calculateSingleFigureDamage = " + totalHits);
		return totalHits;
	}
	
	/**
	 * Rolls the number of actual hits and blocks for "armour piercing" type damage, as per single figure
	 * damage except that the defender's defence stat is halved.
	 * 
	 * @param defender Unit being hit
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @param players Players list
	 * @param spells Known spells
	 * @param combatAreaEffects Known combat area effects
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateArmourPiercingDamage (final MemoryUnit defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final List<PlayerServerDetails> players,
		final List<MemoryMaintainedSpell> spells, final List<MemoryCombatAreaEffect> combatAreaEffects, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering calculateArmourPiercingDamage: Unit URN " + defender.getUnitURN () + " hit by " + attackDamage);
		
		final int defenderDefenceStrength = getUnitUtils ().getModifiedAttributeValue (defender, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db);
		
		final int totalHits = calculateSingleFigureDamageInternal (defender, defenderDefenceStrength / 2, DamageTypeID.ARMOUR_PIERCING,
			attackingPlayer, defendingPlayer, attackDamage, players, spells, combatAreaEffects, db);
		
		log.trace ("Exiting calculateArmourPiercingDamage = " + totalHits);
		return totalHits;
	}

	/**
	 * Rolls the number of actual hits for "illusionary" type damage, as per single figure
	 * damage except that the defender gets no defence rolls at all.
	 * 
	 * @param defender Unit being hit
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @param players Players list
	 * @param spells Known spells
	 * @param combatAreaEffects Known combat area effects
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateIllusionaryDamage (final MemoryUnit defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final List<PlayerServerDetails> players,
		final List<MemoryMaintainedSpell> spells, final List<MemoryCombatAreaEffect> combatAreaEffects, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering calculateIllusionaryDamage: Unit URN " + defender.getUnitURN () + " hit by " + attackDamage);
		
		final int totalHits = calculateSingleFigureDamageInternal (defender, 0, DamageTypeID.ILLUSIONARY,
			attackingPlayer, defendingPlayer, attackDamage, players, spells, combatAreaEffects, db);
		
		log.trace ("Exiting calculateIllusionaryDamage = " + totalHits);
		return totalHits;
	}
	
	/**
	 * Internal method that takes the defender's defence value as an input, so it can be used for single figure, armour piercing and illusionary damage.
	 * 
	 * @param defender Unit being hit
	 * @param defenderDefenceStrength Value of defence stat for the defender unit
	 * @param damageType The type of damage being dealt
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @param players Players list
	 * @param spells Known spells
	 * @param combatAreaEffects Known combat area effects
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	private final int calculateSingleFigureDamageInternal (final MemoryUnit defender, final int defenderDefenceStrength, final DamageTypeID damageType,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final List<PlayerServerDetails> players,
		final List<MemoryMaintainedSpell> spells, final List<MemoryCombatAreaEffect> combatAreaEffects, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering calculateSingleFigureDamageInternal: Unit URN " + defender.getUnitURN () + " hit by " + attackDamage + " of type " + damageType);
		
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.DEFENCE_DATA);
		damageCalculationMsg.setDefenderUnitURN (defender.getUnitURN ());
		damageCalculationMsg.setChanceToHit (3 + attackDamage.getPlusToHit ());
		damageCalculationMsg.setTenTimesAverageDamage (attackDamage.getPotentialHits () * damageCalculationMsg.getChanceToHit ());
		damageCalculationMsg.setDamageType (damageType);
		
		// How many actually hit
		int actualDamage = 0;
		for (int swingNo = 0; swingNo < attackDamage.getPotentialHits (); swingNo++)
			if (getRandomUtils ().nextInt (10) < damageCalculationMsg.getChanceToHit ())
				actualDamage++;
		
		damageCalculationMsg.setActualHits (actualDamage);
		
		// Set up defender stats
		damageCalculationMsg.setDefenderFigures (getUnitCalculations ().calculateAliveFigureCount (defender, players, spells, combatAreaEffects, db));
		damageCalculationMsg.setUnmodifiedDefenceStrength (getUnitUtils ().getModifiedAttributeValue (defender, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db));
		damageCalculationMsg.setModifiedDefenceStrength (defenderDefenceStrength);

		damageCalculationMsg.setChanceToDefend (3 + getUnitUtils ().getModifiedAttributeValue (defender, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db));
		
		damageCalculationMsg.setTenTimesAverageBlock (damageCalculationMsg.getModifiedDefenceStrength () * damageCalculationMsg.getChanceToDefend ());
		
		// Dish out damage
		final int totalHits = applyDamage (defender, actualDamage, damageCalculationMsg.getModifiedDefenceStrength (),
			damageCalculationMsg.getChanceToDefend (), players, spells, combatAreaEffects, db);
		
		damageCalculationMsg.setFinalHits (totalHits);
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		
		log.trace ("Exiting calculateSingleFigureDamageInternal = " + totalHits);
		return totalHits;
	}

	/**
	 * Sets the number of actual hits for "doom" type constant damage.
	 * 
	 * @param defender Unit being hit
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @param players Players list
	 * @param spells Known spells
	 * @param combatAreaEffects Known combat area effects
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateDoomDamage (final MemoryUnit defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final List<PlayerServerDetails> players,
		final List<MemoryMaintainedSpell> spells, final List<MemoryCombatAreaEffect> combatAreaEffects, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering calculateDoomDamage: Unit URN " + defender.getUnitURN () + " hit by " + attackDamage);
		
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.DEFENCE_DATA);
		damageCalculationMsg.setDefenderUnitURN (defender.getUnitURN ());
		damageCalculationMsg.setDamageType (DamageTypeID.DOOM);
		
		// No to hit rolls - they automatically hit
		damageCalculationMsg.setActualHits (attackDamage.getPotentialHits ());
		
		// No defence hit rolls - they automatically hit
		damageCalculationMsg.setDefenderFigures (getUnitCalculations ().calculateAliveFigureCount (defender, players, spells, combatAreaEffects, db));

		// Dish out damage
		final int totalHits = applyDamage (defender, attackDamage.getPotentialHits (), 0, 0, players, spells, combatAreaEffects, db);
		
		damageCalculationMsg.setFinalHits (totalHits);
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		
		log.trace ("Exiting calculateDoomDamage = " + totalHits);
		return totalHits;
	}
	
	/**
	 * Applys damage to a unit, optionally making defence rolls as each figure gets struck
	 * 
	 * @param defender Unit being hit
	 * @param hitsToApply The number of hits striking the defender (number that passed the attacker's to hit roll)
	 * @param defenderDefenceStrength Value of defence stat for the defender unit
	 * @param chanceToDefend Chance (0-10) for a defence point to block an incoming hit
	 * @param players Players list
	 * @param spells Known spells
	 * @param combatAreaEffects Known combat area effects
	 * @param db Lookup lists built over the XML database
	 * @return Number of hits actually applied to the unit, after any were maybe blocked by defence; also this will never be more than the HP the unit had
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 */
	private final int applyDamage (final MemoryUnit defender, final int hitsToApply, final int defenderDefenceStrength, final int chanceToDefend,
		final List<PlayerServerDetails> players, final List<MemoryMaintainedSpell> spells, final List<MemoryCombatAreaEffect> combatAreaEffects, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.trace ("Entering applyDamage: Unit URN " + defender.getUnitURN () + " hit by " + hitsToApply + " vs defence " + defenderDefenceStrength + " at " + chanceToDefend + "0%");

		// Dish out damage - See page 287 in the strategy guide
		// We can't do all defending in one go, each figure only gets to use its shields if the previous figure dies.
		// e.g. a unit of 8 spearmen has to take 2 hits, if all 8 spearmen get to try to block the 2 hits, they might not even lose 1 figure.
		// However only the first unit gets to use its shield, even if it blocks 1 hit it will be killed by the 2nd hit.
		int totalHits = 0;
		int defendingFiguresRemaining = getUnitCalculations ().calculateAliveFigureCount (defender, players, spells, combatAreaEffects, db);
		int hitPointsRemainingOfFirstFigure = getUnitCalculations ().calculateHitPointsRemainingOfFirstFigure (defender, players, spells, combatAreaEffects, db);
		int hitsLeftToApply = hitsToApply;
		
		while ((defendingFiguresRemaining > 0) && (hitsLeftToApply > 0))
		{
			// New figure taking damage, so it gets to try to block some hits
			int thisBlockedHits = 0;
			for (int blockNo = 0; blockNo < defenderDefenceStrength; blockNo++)
				if (getRandomUtils ().nextInt (10) < chanceToDefend)
					thisBlockedHits++;
			
			hitsLeftToApply = hitsLeftToApply - thisBlockedHits;
			
			// If any damage was not blocked by shields then it goes to health
			if (hitsLeftToApply > 0)
			{
				// Work out how many hits the current figure will take
				final int hitsOnThisFigure = Math.min (hitsLeftToApply, hitPointsRemainingOfFirstFigure);
				
				// Update counters for next figure.
				// Note it doesn't matter that we're decreasing defendingFigures even if the figure didn't die, because in that case Hits
				// will now be zero and the loop with exit, so the values of these variables won't matter at all, only the totalHits return value does.
				hitsLeftToApply = hitsLeftToApply - hitsOnThisFigure;
				totalHits = totalHits + hitsOnThisFigure;
				defendingFiguresRemaining--;
				hitPointsRemainingOfFirstFigure = getUnitUtils ().getModifiedAttributeValue (defender, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS,
					UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db);
			}
		}
		
		log.trace ("Exiting applyDamage = " + totalHits);
		return totalHits;
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
	 * @return Random number generator
	 */
	public final RandomUtils getRandomUtils ()
	{
		return randomUtils;
	}

	/**
	 * @param utils Random number generator
	 */
	public final void setRandomUtils (final RandomUtils utils)
	{
		randomUtils = utils;
	}
}