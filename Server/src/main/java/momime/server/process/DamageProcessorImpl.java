package momime.server.process;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.UntransmittedKillUnitActionID;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitCombatSideID;
import momime.common.messages.MemoryUnit;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.DamageCalculationMessage;
import momime.common.messages.servertoclient.DamageCalculationMessageTypeID;
import momime.server.MomSessionVariables;
import momime.server.calculations.DamageCalculator;
import momime.server.fogofwar.FogOfWarMidTurnChanges;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Routines dealing with applying combat damage
 */
public final class DamageProcessorImpl implements DamageProcessor
{
	/** Class logger */
	private final Log log = LogFactory.getLog (DamageProcessorImpl.class);

	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** Damage calc */
	private DamageCalculator damageCalculator;
	
	/** Starting and ending combats */
	private CombatStartAndEnd combatStartAndEnd;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/**
	 * Performs one attack in combat.
	 * If a close combat attack, also resolves the defender retaliating.
	 * Also checks to see if the attack results in either side being wiped out, in which case ends the combat.
	 * 
	 * @param attacker Unit making the attack
	 * @param defender Unit being hit
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackerDirection The direction the attacker needs to turn to in order to be facing the defender
	 * @param isRangedAttack True for ranged attacks; false for close combat attacks
	 * @param combatLocation Where the combat is taking place
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void resolveAttack (final MemoryUnit attacker, final MemoryUnit defender,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final int attackerDirection, final boolean isRangedAttack,
		final MapCoordinates3DEx combatLocation, final MomSessionVariables mom)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering resolveAttack: Unit URN " + attacker.getUnitURN () + ", Unit URN " + defender.getUnitURN ());

		// We send this a couple of times for different parts of the calculation, so initialize it here
		final DamageCalculationMessage damageCalculationMsg = new DamageCalculationMessage ();
		damageCalculationMsg.setAttackerUnitURN (attacker.getUnitURN ());
		damageCalculationMsg.setDefenderUnitURN (defender.getUnitURN ());
		
		if (isRangedAttack)
			damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.RANGED_ATTACK);
		else
			damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.MELEE_ATTACK);
		
		getDamageCalculator ().sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		
		// Make the units face each other
		attacker.setCombatHeading (attackerDirection);
		defender.setCombatHeading (getCoordinateSystemUtils ().normalizeDirection (mom.getSessionDescription ().getCombatMapSize ().getCoordinateSystemType (), attackerDirection + 4));
		
		// Both attack simultaneously, before damage is applied
		// Defender only retaliates against close combat attacks, not ranged attacks
		final int damageToDefender;
		final int damageToAttacker;
		if (isRangedAttack)
		{
			damageToDefender = getDamageCalculator ().calculateDamage (attacker, defender, attackingPlayer, defendingPlayer,
				CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK, damageCalculationMsg, mom.getPlayers (),
				mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), mom.getServerDB ());
			damageToAttacker = 0;
			
			getUnitCalculations ().decreaseRangedAttackAmmo (attacker);
		}
		else
		{
			damageToDefender = getDamageCalculator ().calculateDamage (attacker, defender, attackingPlayer, defendingPlayer,
				CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, damageCalculationMsg, mom.getPlayers (),
				mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), mom.getServerDB ());
			
			damageToAttacker = getDamageCalculator ().calculateDamage (defender, attacker, attackingPlayer, defendingPlayer,
				CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, damageCalculationMsg, mom.getPlayers (),
				mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), mom.getServerDB ());
		}
		
		// Now apply damage
		defender.setDamageTaken (defender.getDamageTaken () + damageToDefender);
		attacker.setDamageTaken (attacker.getDamageTaken () + damageToAttacker);
		
		// Update damage taken in player's memory on server, and on all clients who can see the unit.
		// This includes both players involved in the combat (who will queue this up as an animation), and players who aren't involved in the combat but
		// can see the units fighting (who will update the damage immediately).
		// This also sends the number of combat movement points the attacker has left.
		getFogOfWarMidTurnChanges ().sendCombatDamageToClients (attacker, defender, isRangedAttack,
			mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting ());
		
		// Now we know who the COMBAT attacking and defending players are, we can work out whose
		// is whose unit - because it could be the defending players' unit making the attack in combat.
		// We have to know this, because if both units die at the same time, the defender wins the combat.
		final MemoryUnit attackingPlayerUnit;
		final MemoryUnit defendingPlayerUnit;
		if (attacker.getOwningPlayerID () == attackingPlayer.getPlayerDescription ().getPlayerID ())
		{
			attackingPlayerUnit = attacker;
			defendingPlayerUnit = defender;
		}
		else
		{
			attackingPlayerUnit = defender;
			defendingPlayerUnit = attacker;
		}
		
		// Kill off either unit if dead.
		// We don't need to notify the clients of this separately, clients can tell from the damage taken values above whether the units are dead or not, whether
		// or not they're involved in the combat.
		boolean combatEnded = false;
		PlayerServerDetails winningPlayer = null;
		
		if (getUnitCalculations ().calculateAliveFigureCount (attackingPlayerUnit, mom.getPlayers (),
			mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), mom.getServerDB ()) == 0)
		{
			getFogOfWarMidTurnChanges ().killUnitOnServerAndClients (attackingPlayerUnit, null, UntransmittedKillUnitActionID.COMBAT_DAMAGE,
				mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), mom.getSessionDescription ().getFogOfWarSetting (), mom.getServerDB ());
			
			getFogOfWarMidTurnChanges ().grantExperienceToUnitsInCombat (combatLocation, UnitCombatSideID.DEFENDER,
				mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (),
				mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting ());
			
			// If the attacker is now wiped out, this is the last record we will ever have of who the attacking player was, so we have to deal with tidying up the combat now
			if (countUnitsInCombat (combatLocation, UnitCombatSideID.ATTACKER, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ()) == 0)
			{
				combatEnded = true;
				winningPlayer = defendingPlayer;
			}
		}

		if (getUnitCalculations ().calculateAliveFigureCount (defendingPlayerUnit, mom.getPlayers (),
			mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), mom.getServerDB ()) == 0)
		{
			getFogOfWarMidTurnChanges ().killUnitOnServerAndClients (defendingPlayerUnit, null, UntransmittedKillUnitActionID.COMBAT_DAMAGE,
				mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), mom.getSessionDescription ().getFogOfWarSetting (), mom.getServerDB ());
			
			getFogOfWarMidTurnChanges ().grantExperienceToUnitsInCombat (combatLocation, UnitCombatSideID.ATTACKER,
				mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (),
				mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting ());
			
			// If the defender is now wiped out, this is the last record we will ever have of who the defending player was, so we have to deal with tidying up the combat now.
			// If attacker was also wiped out then we've already done this - the defender won by default.
			if ((!combatEnded) && (countUnitsInCombat (combatLocation, UnitCombatSideID.DEFENDER, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ()) == 0))
			{
				combatEnded = true;
				winningPlayer = attackingPlayer;
			}
		}
		
		// End the combat if one side was totally wiped out
		if (combatEnded)
			getCombatStartAndEnd ().combatEnded (combatLocation, attackingPlayer, defendingPlayer, winningPlayer, null, mom);
		
		log.trace ("Exiting resolveAttack");	
	}

	/**
	 * @param combatLocation Location that combat is taking place
	 * @param combatSide Which side to count
	 * @param trueUnits List of true units
	 * @return How many units are still left alive in combat on the requested side
	 */
	final int countUnitsInCombat (final MapCoordinates3DEx combatLocation, final UnitCombatSideID combatSide,
		final List<MemoryUnit> trueUnits)
	{
		log.trace ("Entering countUnitsInCombat: " + combatLocation + ", " + combatSide);
			
		int count = 0;
		for (final MemoryUnit trueUnit : trueUnits)
			if ((trueUnit.getStatus () == UnitStatusID.ALIVE) && (combatLocation.equals (trueUnit.getCombatLocation ())) &&
				(trueUnit.getCombatSide () == combatSide) && (trueUnit.getCombatPosition () != null))
					
				count++;

		log.trace ("Exiting countUnitsInCombat = " + count);
		return count;
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
}