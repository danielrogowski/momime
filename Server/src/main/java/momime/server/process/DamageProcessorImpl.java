package momime.server.process;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.utils.random.RandomUtils;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.database.AttackResolution;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.StoredDamageTypeID;
import momime.common.database.UnitCombatSideID;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomCombatTile;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.DamageCalculationWallData;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.SpellCastType;
import momime.server.MomSessionVariables;
import momime.server.calculations.AttackDamage;
import momime.server.calculations.DamageCalculator;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.FogOfWarMidTurnMultiChanges;
import momime.server.fogofwar.KillUnitActionID;
import momime.server.knowledge.CombatDetails;
import momime.server.utils.CombatMapServerUtils;
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
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/** Methods dealing with combat maps that are only needed on the server */
	private CombatMapServerUtils combatMapServerUtils;
	
	/**
	 * Performs one attack in combat, which may be a melee, ranged or spell attack.
	 * If a close combat attack, also resolves the defender retaliating.
	 * Also checks to see if the attack results in either side being wiped out, in which case ends the combat.
	 * 
	 * @param attacker Unit making the attack; or null if the attack isn't coming from a unit
	 * @param defenders Unit(s) being hit; some attacks can hit multiple units such as Flame Strike
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param eventID The event that caused an attack, if it wasn't initiated by a player
	 * @param wreckTileChance Whether to roll an attack against the tile in addition to the defenders; null = no, any other value is the chance so 4 = 1/4 chance
	 * @param wreckTilePosition The position within the combat map of the tile that will be attacked
	 * @param attackerDirection The direction the attacker needs to turn to in order to be facing the defender; or null if the attack isn't coming from a unit
	 * @param attackSkillID The skill being used to attack, i.e. UA01 (swords) or UA02 (ranged); or null if the attack isn't coming from a unit
	 * @param spell The spell being cast; or null if the attack isn't coming from a spell
	 * @param variableDamage The damage chosen, for spells where variable mana can be channeled into casting them, e.g. fire bolt; or null if the attack isn't coming from a spell
	 * @param castingPlayer The player casting the spell; or null if the attack isn't coming from a spell
	 * @param combatLocation Where the combat is taking place; null if its damage from an overland spell
	 * @param skipAnimation Tell the client to skip showing any animation and sound effect associated with this spell
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether the attack resulted in the combat ending and how many units on each side were killed
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final ResolveAttackResult resolveAttack (final MemoryUnit attacker, final List<ResolveAttackTarget> defenders,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final String eventID,
		final Integer wreckTileChance, final MapCoordinates2DEx wreckTilePosition,
		final Integer attackerDirection, final String attackSkillID,
		final Spell spell, final Integer variableDamage, final PlayerServerDetails castingPlayer, 
		final MapCoordinates3DEx combatLocation, final boolean skipAnimation, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		final List<MemoryUnit> defenderUnits = defenders.stream ().map (d -> d.getDefender ()).collect (Collectors.toList ());
		getDamageCalculator ().sendDamageHeader (attacker, defenderUnits, false, attackingPlayer, defendingPlayer, eventID, attackSkillID, spell, castingPlayer);
		
		// Make the units face each other
		if (attackerDirection != null)
		{
			attacker.setCombatHeading (attackerDirection);
			
			final int defenderDirection = getCoordinateSystemUtils ().normalizeDirection
				(mom.getSessionDescription ().getCombatMapSize ().getCoordinateSystemType (), attackerDirection + 4);
			
			for (final ResolveAttackTarget defender : defenders) 
				defender.getDefender ().setCombatHeading (defenderDirection);
		}
		
		// If its a spell, we work out the kind of damage dealt once only (so it only appears in the client message log once)
		// If its a regular attribute-based attack, damage is worked out inside each resolution step.
		final ExpandedUnitDetails xuAttackerPreliminary = (attacker == null) ? null : 
			getExpandUnitDetails ().expandUnitDetails (attacker, null, null, null,
				mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());

		// Process our attack against each defender
		final CombatDetails combatDetails = (combatLocation == null) ? null :
			getCombatMapServerUtils ().findCombatByLocation (mom.getCombatDetails (), combatLocation, "resolveAttack");
		
		// If any defenders are being attacked by a different kind of spell, skip them until the next iteration
		final List<ResolveAttackTarget> defendersLeftToProcess = new ArrayList<ResolveAttackTarget> ();
		defendersLeftToProcess.addAll (defenders);
		
		while (defendersLeftToProcess.size () > 0)
		{
			Spell spellThisPass = null;
			List<List<AttackResolutionStepContainer>> spellStepsThisPass = null;
			
			final Iterator<ResolveAttackTarget> defendersIter = defendersLeftToProcess.iterator ();
			while (defendersIter.hasNext ())
			{
				final ResolveAttackTarget defender = defendersIter.next ();
				
				// Calculate the attacker stats, with the defender listed as the opponent.
				// This is important for the call to chooseAttackResolution, since the defender may have Negate First Strike.
				final ExpandedUnitDetails xuDefender = getExpandUnitDetails ().expandUnitDetails (defender.getDefender (), null, null, null,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
				
				final ExpandedUnitDetails xuAttacker;
				if (attacker == null)
					xuAttacker = null;
				else
				{
					final List<ExpandedUnitDetails> xuDefenders = new ArrayList<ExpandedUnitDetails> ();
					xuDefenders.add (xuDefender);
					
					xuAttacker = getExpandUnitDetails ().expandUnitDetails (attacker, xuDefenders, null, null,
						mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
				}
				
				// For attacks based on unit attributes (i.e. melee or ranged attacks), use the full routine to work out the sequence of steps to resolve the attack.
				// If its an attack from a spell, this was already worked out above.
				final List<List<AttackResolutionStepContainer>> steps;
				if (spell != null)
				{
					// What spell is this unit being attacked by?
					final Spell thisSpell = (defender.getSpellOverride () != null) ? defender.getSpellOverride () : spell;
					if ((spellThisPass == null) ||
							
						// If its Warp Lightning then recreate the steps every time to make the damage log look correct, and so we don't keep secondary steps from the previous unit
						((spellThisPass == thisSpell) && (thisSpell.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_WARP_LIGHTNING))))
					{
						spellThisPass = thisSpell;
						spellStepsThisPass = new ArrayList<List<AttackResolutionStepContainer>> ();
						steps = spellStepsThisPass;
						
						final List<AttackResolutionStepContainer> spellInnerSteps = new ArrayList<AttackResolutionStepContainer> ();
						spellStepsThisPass.add (spellInnerSteps);
						
						// If its Call Chaos making a fire bolt attack then have to tweak damage up to 15
						final Integer useVariableDamage;
						if ((spell.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_CALL_CHAOS)) && (thisSpell.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_FIRE_BOLT)))
							useVariableDamage = 15;
						else
							useVariableDamage = variableDamage;
						
						spellInnerSteps.add (new AttackResolutionStepContainer (getDamageCalculator ().attackFromSpell
							(thisSpell, useVariableDamage, castingPlayer, xuAttackerPreliminary, attackingPlayer, defendingPlayer, eventID, mom.getServerDB (),
								(combatLocation == null) ? SpellCastType.OVERLAND : SpellCastType.COMBAT, false)));
					}
					
					// Same spell as before
					else if (spellThisPass == thisSpell)
						steps = spellStepsThisPass;
					
					// Different spell - defer it to next pass
					else
						steps = null;
				}
				else
				{
					final AttackResolution attackResolution = getAttackResolutionProcessing ().chooseAttackResolution (xuAttacker, xuDefender, attackSkillID, mom.getServerDB ());
					
					steps = getAttackResolutionProcessing ().splitAttackResolutionStepsByStepNumber (attackResolution.getAttackResolutionStep ());
				}
	
				if (steps != null)
				{
					// Some figures being frozen in fear lasts for the duration of one attack resolution,
					// i.e. spans multiple resolution steps so have to keep track of it out here
					final AttackResolutionUnit attackerWrapper = (attacker == null) ? null : new AttackResolutionUnit (attacker);
					final AttackResolutionUnit defenderWrapper = new AttackResolutionUnit (defender.getDefender ());
					
					// Process each step; use counter as we might add more steps as we process the list 
					// Skip the entire step if either unit is already dead
					int stepNumber = 0;
					while ((stepNumber < steps.size ()) && (xuDefender.calculateAliveFigureCount () > 0) &&
						((xuAttacker == null) || (xuAttacker.calculateAliveFigureCount () > 0)))
					{
						final List<AttackResolutionStepContainer> step = steps.get (stepNumber);
						
						getAttackResolutionProcessing ().processAttackResolutionStep
							(attackerWrapper, defenderWrapper, attackingPlayer, defendingPlayer, combatLocation, step, mom);
						
						stepNumber++;
						
						// Warp lightning generates additional steps, each time subtracting off the stepNumber in damage until we reach 0.
						// It would be neater if this was handled prior to this loop, but then it sends all 10 "spell attack" damage calcs to the client,
						// followed by the 10 resolutions, and we want them to be interspersed.
						if ((spellThisPass != null) && (spellThisPass.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_WARP_LIGHTNING)) &&
							(stepNumber < spellThisPass.getCombatBaseDamage ()) && (xuDefender.calculateAliveFigureCount () > 0))
						{
							final List<AttackResolutionStepContainer> spellInnerSteps = new ArrayList<AttackResolutionStepContainer> ();
							steps.add (spellInnerSteps);
							
							spellInnerSteps.add (new AttackResolutionStepContainer (getDamageCalculator ().attackFromSpell
								(spellThisPass, spellThisPass.getCombatBaseDamage () - stepNumber, castingPlayer, xuAttackerPreliminary, attackingPlayer, defendingPlayer,
									eventID, mom.getServerDB (), (combatLocation == null) ? SpellCastType.OVERLAND : SpellCastType.COMBAT, false)));
						}
					}
					
					// Count this as an attack against this defender, as long as its a regular attack from a unit and not a spell
					if (attackSkillID != null)
					{
						final Integer count = combatDetails.getNumberOfTimedAttacked ().get (xuDefender.getUnitURN ());
						combatDetails.getNumberOfTimedAttacked ().put (xuDefender.getUnitURN (), ((count == null) ? 0 : count) + 1);
					}
					
					// Done processing this defender
					defendersIter.remove ();
				}
			}
		}
		
		// Process attack against the wall
		Boolean sendWrecked = null;
		if (wreckTileChance != null)
		{
			final DamageCalculationWallData wallMsg = new DamageCalculationWallData ();
			wallMsg.setWreckTileChance (wreckTileChance);
			wallMsg.setWrecked ((getRandomUtils ().nextInt (wreckTileChance) == 0));
			getDamageCalculator ().sendDamageCalculationMessage (attackingPlayer, defendingPlayer, wallMsg);

			if (wallMsg.isWrecked ())
			{
				final MapAreaOfCombatTiles combatMap = combatDetails.getCombatMap ();
				
				final MomCombatTile tile = combatMap.getRow ().get (wreckTilePosition.getY ()).getCell ().get (wreckTilePosition.getX ());
				tile.setWrecked (true);
				sendWrecked = true;
			}
		}
		
		// Update damage taken in player's memory on server, and on all clients who can see the unit.
		// This includes both players involved in the combat (who will queue this up as an animation), and players who aren't involved in the combat but
		// can see the units fighting (who will update the damage immediately).
		// This also sends the number of combat movement points the attacker has left.
		// Must pass attacking/defendingPlayer as null if this is not combat damage, so overland damage isn't animated on clients.
		getFogOfWarMidTurnChanges ().sendDamageToClients (attacker,
			(combatLocation == null) ? null : attackingPlayer, (combatLocation == null) ? null : defendingPlayer,
			defenders, attackSkillID, (spell == null) ? null : spell.getSpellID (),
			wreckTilePosition, sendWrecked, skipAnimation, mom);
		
		// Now we know who the COMBAT attacking and defending players are, we can work out whose
		// is whose unit - because it could be the defending players' unit making the attack in combat.
		// We have to know this, because if both units die at the same time, the defender wins the combat.
		
		// In the case where there are ZERO defenders, we're just hitting a wall.  In that case it doesn't really matter
		// which way around we assign the players as knocking a wall down isn't going to end the combat.
		final List<MemoryUnit> attackingPlayerUnits;
		final List<MemoryUnit> defendingPlayerUnits;
		if ((defenders.size () == 0) || (defenders.get (0).getDefender ().getOwningPlayerID () == defendingPlayer.getPlayerDescription ().getPlayerID ()))
		{
			attackingPlayerUnits = new ArrayList<MemoryUnit> ();
			if (attacker != null)
				attackingPlayerUnits.add (attacker);
			
			defendingPlayerUnits = defenderUnits;
		}
		else
		{
			attackingPlayerUnits = defenderUnits;
			
			defendingPlayerUnits = new ArrayList<MemoryUnit> ();
			if (attacker != null)
				defendingPlayerUnits.add (attacker);
		}
		
		// Kill off any of the units who may have died.
		// We don't need to notify the clients of this separately, clients can tell from the damage taken values above whether the units are dead or not,
		// whether or not they're involved in the combat.
		boolean combatEnded = false;
		PlayerServerDetails winningPlayer = null;
		
		int attackingPlayerUnitsKilled = 0;
		if (attackingPlayerUnits.size () > 0)
		{
			boolean anyAttackingPlayerUnitsSurvived = false;
			for (final MemoryUnit attackingPlayerUnit : attackingPlayerUnits)
			{
				final ExpandedUnitDetails xuAttackingPlayerUnit = getExpandUnitDetails ().expandUnitDetails (attackingPlayerUnit, null, null, null,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());

				if (xuAttackingPlayerUnit.calculateAliveFigureCount () > 0)
					anyAttackingPlayerUnitsSurvived = true;
				else
				{
					combatDetails.setAttackerSpecialFameLost (combatDetails.getAttackerSpecialFameLost () + xuAttackingPlayerUnit.calculateFameLostForUnitDying ());
					
					final KillUnitActionID action;
					if (getUnitServerUtils ().whatKilledUnit (attackingPlayerUnit.getUnitDamage ()) == StoredDamageTypeID.PERMANENT)
						action = KillUnitActionID.PERMANENT_DAMAGE;
					else if (combatLocation != null)
						action = KillUnitActionID.HEALABLE_COMBAT_DAMAGE;
					else
						action = KillUnitActionID.HEALABLE_OVERLAND_DAMAGE;
					
					attackingPlayerUnitsKilled++;
					mom.getWorldUpdates ().killUnit (attackingPlayerUnit.getUnitURN (), action);
					mom.getWorldUpdates ().process (mom);
					
					if (combatLocation != null)
						getFogOfWarMidTurnMultiChanges ().grantExperienceToUnitsInCombat (combatLocation, UnitCombatSideID.DEFENDER, mom);
				}
			}
					
			// If the attacker is now wiped out, this is the last record we will ever have of who the attacking player was, so we have to deal with tidying up the combat now
			if ((!anyAttackingPlayerUnitsSurvived) &&
				(countUnitsInCombat (combatLocation, UnitCombatSideID.ATTACKER, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), mom.getServerDB ()) == 0))
			{
				combatEnded = true;
				winningPlayer = defendingPlayer;
			}
		}

		int defendingPlayerUnitsKilled = 0;
		if (defendingPlayerUnits.size () > 0)
		{
			boolean anyDefendingPlayerUnitsSurvived = false;
			for (final MemoryUnit defendingPlayerUnit : defendingPlayerUnits)
			{
				final ExpandedUnitDetails xuDefendingPlayerUnit = getExpandUnitDetails ().expandUnitDetails (defendingPlayerUnit, null, null, null,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());

				if (xuDefendingPlayerUnit.calculateAliveFigureCount () > 0)
					anyDefendingPlayerUnitsSurvived = true;
				else
				{
					if ((combatDetails != null) && (combatDetails.getDefenderSpecialFameLost () > 0))
						combatDetails.setDefenderSpecialFameLost (combatDetails.getDefenderSpecialFameLost () + xuDefendingPlayerUnit.calculateFameLostForUnitDying ());
					
					final KillUnitActionID action;
					if (getUnitServerUtils ().whatKilledUnit (defendingPlayerUnit.getUnitDamage ()) == StoredDamageTypeID.PERMANENT)
						action = KillUnitActionID.PERMANENT_DAMAGE;
					else if (combatLocation != null)
						action = KillUnitActionID.HEALABLE_COMBAT_DAMAGE;
					else
						action = KillUnitActionID.HEALABLE_OVERLAND_DAMAGE;
					
					defendingPlayerUnitsKilled++;
					mom.getWorldUpdates ().killUnit (defendingPlayerUnit.getUnitURN (), action);
					mom.getWorldUpdates ().process (mom);
					
					if (combatLocation != null)
						getFogOfWarMidTurnMultiChanges ().grantExperienceToUnitsInCombat (combatLocation, UnitCombatSideID.ATTACKER, mom);
				}
			}
			
			// If the defender is now wiped out, this is the last record we will ever have of who the defending player was, so we have to deal with tidying up the combat now.
			// If attacker was also wiped out then we've already done this - the defender won by default.
			if ((!combatEnded) && (!anyDefendingPlayerUnitsSurvived) && (combatLocation != null) &&
				(countUnitsInCombat (combatLocation, UnitCombatSideID.DEFENDER, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), mom.getServerDB ()) == 0))
			{
				combatEnded = true;
				winningPlayer = attackingPlayer;
			}
		}
		
		// End the combat if one side was totally wiped out
		if (combatEnded)
			getCombatStartAndEnd ().combatEnded (combatDetails, attackingPlayer, defendingPlayer, winningPlayer, null, mom, true);
		
		return new ResolveAttackResult (combatEnded, attackingPlayerUnitsKilled, defendingPlayerUnitsKilled);
	}

	/**
	 * When we are trying to curse a unit, for example with Confusion or Black Sleep, handles making the resistance roll to see if they are affected or not
	 * 
	 * @param attacker Unit casting the spell; or null if wizard is casting
	 * @param defender Unit we are trying to curse
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param spell The spell being cast
	 * @param variableDamage The damage chosen, for spells where variable mana can be channeled into casting them, e.g. fire bolt
	 * @param existingCurse Whether the resistance roll is to shake off an existing curse (false is normal setting, if its to try to avoid being cursed in the first place)
	 * @param castingPlayer The player casting the spell
	 * @param castType Whether spell is being cast in combat or overland
	 * @param skipDamageHeader Whether to skip sending the damage header, if this is part of a bigger spell (used for Call Chaos)
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether the defender failed the resistance roll or not, i.e. true if something bad happens
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final boolean makeResistanceRoll (final MemoryUnit attacker, final MemoryUnit defender,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final Spell spell, final Integer variableDamage, final boolean existingCurse,
		final PlayerServerDetails castingPlayer, final SpellCastType castType, final boolean skipDamageHeader, final MomSessionVariables mom)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		final List<MemoryUnit> defenders = new ArrayList<MemoryUnit> ();
		defenders.add (defender);
		
		if (!skipDamageHeader)
			getDamageCalculator ().sendDamageHeader (attacker, defenders, existingCurse, attackingPlayer, defendingPlayer, null, null, spell, castingPlayer);
	
		// Spell might be being cast by a unit, or a hero casting a spell imbued in an item
		final ExpandedUnitDetails xuUnitMakingAttack = (attacker == null) ? null : 
			getExpandUnitDetails ().expandUnitDetails (attacker, null, null, null,
				mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
		
		// Work out base saving throw plus any modifiers from hero items with -spell save
		final AttackDamage potentialDamage = getDamageCalculator ().attackFromSpell
			(spell, variableDamage, castingPlayer, xuUnitMakingAttack, attackingPlayer, defendingPlayer, null, mom.getServerDB (), castType, skipDamageHeader);
		
		// Make the actual roll
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
		
		final ExpandedUnitDetails xuUnitBeingAttacked = getExpandUnitDetails ().expandUnitDetails (defender, xuUnitsMakingAttack,
			potentialDamage.getAttackFromSkillID (), potentialDamage.getAttackFromMagicRealmID (),
			mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
		
		return getDamageCalculator ().calculateResistanceRoll (xuUnitBeingAttacked, attackingPlayer, defendingPlayer, potentialDamage, existingCurse);	
	}
	
	/**
	 * @param combatLocation Location that combat is taking place
	 * @param combatSide Which side to count
	 * @param trueUnits List of true units
	 * @param db Lookup lists built over the XML database
	 * @return How many units are still left alive in combat on the requested side
	 */
	@Override
	public final int countUnitsInCombat (final MapCoordinates3DEx combatLocation, final UnitCombatSideID combatSide,
		final List<MemoryUnit> trueUnits, final CommonDatabase db)
	{
		int count = 0;
		for (final MemoryUnit trueUnit : trueUnits)
			if ((trueUnit.getStatus () == UnitStatusID.ALIVE) && (combatLocation.equals (trueUnit.getCombatLocation ())) &&
				(trueUnit.getCombatSide () == combatSide) && (trueUnit.getCombatPosition () != null) && (trueUnit.getCombatHeading () != null) &&
				(!db.getUnitsThatMoveThroughOtherUnits ().contains (trueUnit.getUnitID ())))
					
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
}