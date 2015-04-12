package momime.server.process;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.UntransmittedKillUnitActionID;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DamageTypeID;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitCombatSideID;
import momime.common.messages.MemoryUnit;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.DamageCalculationHeaderData;
import momime.common.messages.servertoclient.DamageCalculationMessageTypeID;
import momime.server.MomSessionVariables;
import momime.server.calculations.AttackDamage;
import momime.server.calculations.DamageCalculator;
import momime.server.database.SpellSvr;
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
	 * @param attacker Unit making the attack; or null if the attack isn't coming from a unit
	 * @param defender Unit being hit
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackerDirection The direction the attacker needs to turn to in order to be facing the defender
	 * @param attackAttributeID The attribute being used to attack, i.e. UA01 (swords) or UA02 (ranged); or null if the attack isn't coming from a unit
	 * @param spell The spell being cast; or null if the attack isn't coming from a spell
	 * @param variableDamage The damage chosen, for spells where variable mana can be channeled into casting them, e.g. fire bolt; or null if the attack isn't coming from a spell
	 * @param castingPlayer The player casting the spell; or null if the attack isn't coming from a spell
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
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final Integer attackerDirection, final String attackAttributeID,
		final SpellSvr spell, final Integer variableDamage, final PlayerServerDetails castingPlayer, 
		final MapCoordinates3DEx combatLocation, final MomSessionVariables mom)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering resolveAttack: Unit URN " + ((attacker != null) ? new Integer (attacker.getUnitURN ()).toString () : "N/A") + ", Unit URN " + defender.getUnitURN ());

		// We send this a couple of times for different parts of the calculation, so initialize it here
		final DamageCalculationHeaderData damageCalculationMsg = new DamageCalculationHeaderData ();
		damageCalculationMsg.setDefenderUnitURN (defender.getUnitURN ());
		damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.HEADER);
		damageCalculationMsg.setAttackAttributeID (attackAttributeID);
		
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
			defender.setCombatHeading (getCoordinateSystemUtils ().normalizeDirection
				(mom.getSessionDescription ().getCombatMapSize ().getCoordinateSystemType (), attackerDirection + 4));
		}
		
		// Work out potential damage from the attack
		final AttackDamage potentialDamageToDefender;
		if (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK.equals (attackAttributeID))
		{
			potentialDamageToDefender = getDamageCalculator ().attackFromUnit
				(attacker, attackingPlayer, defendingPlayer, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK, mom.getPlayers (),
				mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), mom.getServerDB ());

			getUnitCalculations ().decreaseRangedAttackAmmo (attacker);
		}
		else if (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK.equals (attackAttributeID))
		{
			potentialDamageToDefender = getDamageCalculator ().attackFromUnit
				(attacker, attackingPlayer, defendingPlayer, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, mom.getPlayers (),
				mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), mom.getServerDB ());
		}
		else if (spell != null)
		{
			potentialDamageToDefender = getDamageCalculator ().attackFromSpell (spell, variableDamage, castingPlayer, attackingPlayer, defendingPlayer);
		}
		else
			throw new MomException ("resolveAttack doesn't know how to process an attack from attribute " + attackAttributeID);
		
		// Work out how much of the damage gets through
		final int damageToDefender;
		if (potentialDamageToDefender == null)
			damageToDefender = 0;
		else
			switch (potentialDamageToDefender.getDamageType ())
			{
				case SINGLE_FIGURE:
					damageToDefender = getDamageCalculator ().calculateSingleFigureDamage
						(defender, attackingPlayer, defendingPlayer, potentialDamageToDefender,
						mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
						mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), mom.getServerDB ());
					break;
					
				case ARMOUR_PIERCING:
					damageToDefender = getDamageCalculator ().calculateArmourPiercingDamage
						(defender, attackingPlayer, defendingPlayer, potentialDamageToDefender,
						mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
						mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), mom.getServerDB ());
					break;
					
				case ILLUSIONARY:
					damageToDefender = getDamageCalculator ().calculateIllusionaryDamage
						(defender, attackingPlayer, defendingPlayer, potentialDamageToDefender,
						mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
						mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), mom.getServerDB ());
					break;

				case MULTI_FIGURE:
					damageToDefender = getDamageCalculator ().calculateMultiFigureDamage
						(defender, attackingPlayer, defendingPlayer, potentialDamageToDefender,
						mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
						mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), mom.getServerDB ());
					break;
					
				case DOOM:
					damageToDefender = getDamageCalculator ().calculateDoomDamage
						(defender, attackingPlayer, defendingPlayer, potentialDamageToDefender,
						mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
						mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), mom.getServerDB ());
					break;
					
				default:
					throw new MomException ("resolveAttack trying to deal attack damage of type " + potentialDamageToDefender.getDamageType () +
						" to the defender, which it does not know how to deal with yet");
			}

		// Work out potential damage from the counterattack
		final AttackDamage potentialDamageToAttacker;
		if (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK.equals (attackAttributeID))
		{
			potentialDamageToAttacker = getDamageCalculator ().attackFromUnit
				(defender, attackingPlayer, defendingPlayer, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, mom.getPlayers (),
				mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), mom.getServerDB ());
		}
		else
			potentialDamageToAttacker = null;
		
		// Work out how much of the damage gets through
		final int damageToAttacker;
		if (potentialDamageToAttacker == null)
			damageToAttacker = 0;
		else
		{
			if (potentialDamageToAttacker.getDamageType () != DamageTypeID.SINGLE_FIGURE)
				throw new MomException ("resolveAttack trying to deal counterattack damage of type " + potentialDamageToAttacker.getDamageType () +
					" back to the attacker, but only single figure damage counterattacks are supported");
			
			damageToAttacker = getDamageCalculator ().calculateSingleFigureDamage
				(attacker, attackingPlayer, defendingPlayer, potentialDamageToAttacker,
				mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
				mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), mom.getServerDB ());
		}
		
		// Now apply damage
		if (defender != null)
			defender.setDamageTaken (defender.getDamageTaken () + damageToDefender);
		
		if (attacker != null)
			attacker.setDamageTaken (attacker.getDamageTaken () + damageToAttacker);
		
		// Update damage taken in player's memory on server, and on all clients who can see the unit.
		// This includes both players involved in the combat (who will queue this up as an animation), and players who aren't involved in the combat but
		// can see the units fighting (who will update the damage immediately).
		// This also sends the number of combat movement points the attacker has left.
		getFogOfWarMidTurnChanges ().sendCombatDamageToClients (attacker, damageCalculationMsg.getAttackerPlayerID (), defender,
			damageCalculationMsg.getAttackSkillID (), damageCalculationMsg.getAttackAttributeID (), damageCalculationMsg.getAttackSpellID (),
			mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting ());
		
		// Now we know who the COMBAT attacking and defending players are, we can work out whose
		// is whose unit - because it could be the defending players' unit making the attack in combat.
		// We have to know this, because if both units die at the same time, the defender wins the combat.
		final MemoryUnit attackingPlayerUnit;
		final MemoryUnit defendingPlayerUnit;
		if (defender.getOwningPlayerID () == defendingPlayer.getPlayerDescription ().getPlayerID ())
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
		
		if ((attackingPlayerUnit != null) && (getUnitCalculations ().calculateAliveFigureCount (attackingPlayerUnit, mom.getPlayers (),
			mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), mom.getServerDB ()) == 0))
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

		if ((defendingPlayerUnit != null) && (getUnitCalculations ().calculateAliveFigureCount (defendingPlayerUnit, mom.getPlayers (),
			mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), mom.getServerDB ()) == 0))
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