package momime.server.calculations;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.DamageCalculationMessage;
import momime.common.messages.servertoclient.DamageCalculationMessageTypeID;
import momime.common.utils.UnitAttributeComponent;
import momime.common.utils.UnitAttributePositiveNegative;
import momime.common.utils.UnitUtils;
import momime.server.database.ServerDatabaseEx;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

/**
 * Routines for making damage rolls to see how many HP a unit attacking another unit knocks off
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
	public final void sendDamageCalculationMessage (final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final DamageCalculationMessage msg)
		throws JAXBException, XMLStreamException
	{
		if (attackingPlayer.getPlayerDescription ().isHuman ())
			attackingPlayer.getConnection ().sendMessageToClient (msg);
		
		if (defendingPlayer.getPlayerDescription ().isHuman ())
			defendingPlayer.getConnection ().sendMessageToClient (msg);		
	}
	
	/**
	 * NB. This doesn't actually apply the damage, so that both the attack and counterattack damage can be calculated and applied at the same time
	 * 
	 * @param attacker Unit making the attack
	 * @param defender Unit being hit
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackAttributeID The attribute being used to attack, i.e. UA01 (swords) or UA02 (ranged)
	 * @param damageCalculationMsg Partially pre-filled damage calc message so that it can be reused
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
	public final int calculateDamage (final MemoryUnit attacker, final MemoryUnit defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final String attackAttributeID, final DamageCalculationMessage damageCalculationMsg, final List<PlayerServerDetails> players,
		final List<MemoryMaintainedSpell> spells, final List<MemoryCombatAreaEffect> combatAreaEffects, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering calculateDamage: Unit URN " + attacker.getUnitURN () + ", Unit URN " + defender.getUnitURN () + ", " + attackAttributeID);
		
		// Store values straight into the message
		// The attacker and defender may be switched, so redo the message from scratch
		damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.ATTACK_AND_DEFENCE_STATISTICS);
		damageCalculationMsg.setAttackerUnitURN (attacker.getUnitURN ());
		damageCalculationMsg.setDefenderUnitURN (defender.getUnitURN ());
		damageCalculationMsg.setAttackAttributeID (attackAttributeID);
		
		// How many potential hits can we make - See page 285 in the strategy guide
		damageCalculationMsg.setAttackerFigures (getUnitCalculations ().calculateAliveFigureCount (attacker, players, spells, combatAreaEffects, db));
		damageCalculationMsg.setAttackStrength (getUnitUtils ().getModifiedAttributeValue (attacker, attackAttributeID,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db));
		
		damageCalculationMsg.setPotentialDamage (damageCalculationMsg.getAttackerFigures () * damageCalculationMsg.getAttackStrength ());
		
		damageCalculationMsg.setChanceToHit (3 + getUnitUtils ().getModifiedAttributeValue (attacker, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_PLUS_TO_HIT,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db));
		
		damageCalculationMsg.setTenTimesAverageDamage (damageCalculationMsg.getPotentialDamage () * damageCalculationMsg.getChanceToHit ());
		
		// How many actually hit
		int actualDamage = 0;
		for (int swingNo = 0; swingNo < damageCalculationMsg.getPotentialDamage (); swingNo++)
			if (getRandomUtils ().nextInt (10) < damageCalculationMsg.getChanceToHit ())
				actualDamage++;
		
		damageCalculationMsg.setActualDamage (actualDamage);
		
		// Set up defender stats
		damageCalculationMsg.setDefenderFigures (getUnitCalculations ().calculateAliveFigureCount (defender, players, spells, combatAreaEffects, db));
		damageCalculationMsg.setDefenceStrength (getUnitUtils ().getModifiedAttributeValue (defender, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_DEFENCE,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db));

		damageCalculationMsg.setChanceToDefend (3 + getUnitUtils ().getModifiedAttributeValue (defender, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db));
		
		damageCalculationMsg.setTenTimesAverageBlock (damageCalculationMsg.getDefenceStrength () * damageCalculationMsg.getChanceToDefend ());
		
		// Dish out damage - See page 287 in the strategy guide
		// We can't do all defending in one go, each figure only gets to use its shields if the previous figure dies.
		// e.g. a unit of 8 spearmen has to take 2 hits, if all 8 spearmen get to try to block the 2 hits, they might not even lose 1 figure.
		// However only the first unit gets to use its shield, even if it blocks 1 hit it will be killed by the 2nd hit.
		int totalHits = 0;
		int defendingFiguresRemaining = damageCalculationMsg.getDefenderFigures ();
		int hitPointsRemainingOfFirstFigure = getUnitCalculations ().calculateHitPointsRemainingOfFirstFigure (defender, players, spells, combatAreaEffects, db);
		int hitsLeftToApply = actualDamage;
		final StringBuffer actualBlockedHits = new StringBuffer ();
		
		while ((defendingFiguresRemaining > 0) && (hitsLeftToApply > 0))
		{
			// New figure taking damage, so it gets to try to block some hits
			int thisBlockedHits = 0;
			for (int blockNo = 0; blockNo < damageCalculationMsg.getDefenceStrength (); blockNo++)
				if (getRandomUtils ().nextInt (10) < damageCalculationMsg.getChanceToDefend ())
					thisBlockedHits++;
			
			hitsLeftToApply = hitsLeftToApply - thisBlockedHits;
			
			if (actualBlockedHits.length () > 0)
				actualBlockedHits.append (",");
			
			actualBlockedHits.append (thisBlockedHits);
			
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
				hitPointsRemainingOfFirstFigure = getUnitUtils ().getModifiedAttributeValue (defender, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_HIT_POINTS,
					UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db);
			}
		}
		
		damageCalculationMsg.setActualBlockedHits (actualBlockedHits.toString ());
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		
		log.trace ("Exiting calculateDamage = " + totalHits);
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