package momime.server.calculations;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.calculations.UnitCalculations;
import momime.common.calculations.UnitHasSkillMergedList;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.SpellValidUnitTarget;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillPositiveNegative;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.servertoclient.DamageCalculationAttackData;
import momime.common.messages.servertoclient.DamageCalculationData;
import momime.common.messages.servertoclient.DamageCalculationDefenceData;
import momime.common.messages.servertoclient.DamageCalculationMessage;
import momime.common.messages.servertoclient.DamageCalculationMessageTypeID;
import momime.common.utils.DamageTypeUtils;
import momime.common.utils.SpellUtils;
import momime.common.utils.UnitSkillUtils;
import momime.common.utils.UnitUtils;
import momime.server.database.DamageTypeSvr;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseValues;
import momime.server.database.SpellSvr;
import momime.server.database.UnitSkillSvr;
import momime.server.database.UnitSvr;
import momime.server.process.AttackResolutionUnit;
import momime.server.utils.UnitServerUtils;

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
	
	/** Unit skill utils */
	private UnitSkillUtils unitSkillUtils;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** Random number generator */
	private RandomUtils randomUtils;

	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;
	
	/** Spell utils */
	private SpellUtils spellUtils;
	
	/** Damage type utils */
	private DamageTypeUtils damageTypeUtils;
	
	/** Damage type calculations */
	private DamageTypeCalculations damageTypeCalculations;
	
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
	 * Calculates the strength of an attack coming from a unit skill, e.g. Thrown Weapons, breath and gaze attacks, or Posion Touch
	 * 
	 * @param attacker Unit making the attack
	 * @param defender Unit being attacked
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackSkillID The skill being used to attack
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker, or null if the attacker doesn't even have the requested skill or the defender is immune to it
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final AttackDamage attackFromUnitSkill (final AttackResolutionUnit attacker, final AttackResolutionUnit defender,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final String attackSkillID, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering attackFromUnitSkill: Unit URN " + attacker.getUnit ().getUnitURN () + ", " + attackSkillID);

		// The unit's skill level indicates the strength of the attack (e.g. Poison Touch 2 vs Poison Touch 4)
		final AttackDamage attackDamage;
		final int figureCount = getUnitCalculations ().calculateAliveFigureCount (attacker.getUnit (), players, mem, db) - attacker.getFiguresFrozenInFear ();
		final int damage = getUnitSkillUtils ().getModifiedSkillValue (attacker.getUnit (), attacker.getUnit ().getUnitHasSkill (), attackSkillID,
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, mem, db);
		if ((damage < 0) || (figureCount <= 0))
			attackDamage = null;
		else
		{
			// Figure out the magic realm of the attack; getting a null here is fine, this just means the attack deals physical damage
			final String attackFromMagicRealmID;
			if (attackSkillID.equals (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK))
			{
				// Ranged attack magic realms are defined against the RAT rather than the realm of the attacker
				// so e.g. Storm Giants throw lightning bolts, which are Chaos-based RATs, rather than the Storm Giant being a Sorcery creature... which is a bit weird
				final UnitSvr attackerDef = db.findUnit (attacker.getUnit ().getUnitID (), "attackFromUnitSkill");
				attackFromMagicRealmID = db.findRangedAttackType (attackerDef.getRangedAttackType (), "attackFromUnitSkill").getMagicRealmID ();
			}
			else
			{
				// Special attack magic realms are defined against the unit skill rather than the realm of the attacker
				// so e.g. Sky Drakes have lightning breath, which is a skill that deals Chaos damage, rather than the Sky Drake being a Sorcery creature... which is also a bit weird
				final UnitSkillSvr unitSkillDef = db.findUnitSkill (attackSkillID, "attackFromUnitSkill");
				attackFromMagicRealmID = unitSkillDef.getMagicRealmID ();
			}

			// Figure out the type of damage, and check whether the defender is immune to it.
			// Firstly if the unit has the "create undead" skill, then force all damage to "life stealing" as long as the defender isn't immune to it -
			// if they are immune to it, leave it as regular melee damage (tested in the original MoM that Ghouls can hurt Zombies).
			DamageTypeSvr damageType = null;
			if (getUnitSkillUtils ().getModifiedSkillValue (attacker.getUnit (), attacker.getUnit ().getUnitHasSkill (), ServerDatabaseValues.UNIT_SKILL_ID_CREATE_UNDEAD,
				UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, mem, db) >= 0)
			{
				damageType = db.findDamageType (ServerDatabaseValues.DAMAGE_TYPE_ID_LIFE_STEALING, "attackFromUnitSkill");
				if (getDamageTypeUtils ().isUnitImmuneToDamageType (defender.getUnit (), damageType, attackSkillID, attackFromMagicRealmID, players, mem, db))
					damageType = null;
			}
			
			// If life stealing damage didn't apply, just use whatever is defined against the unit skill like normal
			if (damageType == null)
				damageType = getDamageTypeCalculations ().determineSkillDamageType (attacker.getUnit (), attackSkillID, mem.getMaintainedSpell (), db);
			
			if (getDamageTypeUtils ().isUnitImmuneToDamageType (defender.getUnit (), damageType, attackSkillID, attackFromMagicRealmID, players, mem, db))
				attackDamage = null;
			else
			{
				// Expend ammo server side - the client expends ammo when it receives the above message, so the two stay in step
				if (attackSkillID.equals (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK))
					getUnitCalculations ().decreaseRangedAttackAmmo (attacker.getUnit ());
				
				// Start breakdown message
				final DamageCalculationAttackData damageCalculationMsg = new DamageCalculationAttackData ();
				damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.ATTACK_DATA);
				damageCalculationMsg.setAttackerUnitURN (attacker.getUnit ().getUnitURN ());
				damageCalculationMsg.setAttackerPlayerID (attacker.getUnit ().getOwningPlayerID ());
				damageCalculationMsg.setAttackSkillID (attackSkillID);
				damageCalculationMsg.setDamageTypeID (damageType.getDamageTypeID ());
				damageCalculationMsg.setStoredDamageTypeID (damageType.getStoredDamageTypeID ());
				
				// Different skills deal different types of damage
				final UnitSkillSvr unitSkill = db.findUnitSkill (attackSkillID, "attackFromUnitSkill");
	
				if (unitSkill.getDamageResolutionTypeID () == null)
					throw new MomException ("attackFromUnitSkill tried to attack with skill " + attackSkillID + ", but it has no damageResolutionTypeID defined");
				
				damageCalculationMsg.setDamageResolutionTypeID (unitSkill.getDamageResolutionTypeID ());
				
				// Some skills hit just once from the whole attacking unit, some hit once per figure
				if (unitSkill.getDamagePerFigure () == null)
					throw new MomException ("attackFromUnitSkill tried to attack with skill " + attackSkillID + ", but it has no damagePerFigure defined");
				
				final int repetitions;
				switch (unitSkill.getDamagePerFigure ())
				{
					case PER_UNIT:
						damageCalculationMsg.setPotentialHits (damage);
						repetitions = 1;
						break;
	
					case PER_FIGURE_SEPARATE:
						damageCalculationMsg.setPotentialHits (damage);
						repetitions = figureCount;
						break;
	
					case PER_FIGURE_COMBINED:
						damageCalculationMsg.setAttackerFigures (figureCount);
						damageCalculationMsg.setAttackStrength (damage);
						damageCalculationMsg.setPotentialHits (damage * damageCalculationMsg.getAttackerFigures ());
						repetitions = 1;
						break;
						
					default:
						throw new MomException ("attackFromUnitSkill does not know how to handle damagePerFigure of " + unitSkill.getDamagePerFigure ());
				}
		
				sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		
				// Fill in the damage object
				final int plusToHit = getUnitSkillUtils ().getModifiedSkillValue (attacker.getUnit (), attacker.getUnit ().getUnitHasSkill (),
					CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT,
					UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, mem, db);
		
				attackDamage = new AttackDamage (damageCalculationMsg.getPotentialHits (), plusToHit, damageType, damageCalculationMsg.getDamageResolutionTypeID (), null,
					attackSkillID, attackFromMagicRealmID, repetitions);
			}
		}
		
		log.trace ("Exiting attackFromUnitSkill = " + attackDamage);
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
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 */
	@Override
	public final AttackDamage attackFromSpell (final SpellSvr spell, final Integer variableDamage,
		final PlayerServerDetails castingPlayer, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final ServerDatabaseEx db)
		throws JAXBException, XMLStreamException, RecordNotFoundException
	{
		log.trace ("Entering attackFromSpell: Unit URN " + spell.getSpellID () + ", " + variableDamage);
		
		// Work out damage done - note this isn't applicable to all types of attack, e.g. Warp Wood has no attack value, so we might get null here
		final Integer damage = (variableDamage != null) ? variableDamage : spell.getCombatBaseDamage ();
		final DamageTypeSvr damageType = db.findDamageType (spell.getAttackSpellDamageTypeID (), "attackFromSpell");
		
		// Start breakdown message
		final DamageCalculationAttackData damageCalculationMsg = new DamageCalculationAttackData ();
		damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.ATTACK_DATA);
		damageCalculationMsg.setAttackerPlayerID (castingPlayer.getPlayerDescription ().getPlayerID ());
		damageCalculationMsg.setAttackSpellID (spell.getSpellID ());
		damageCalculationMsg.setPotentialHits (damage);
		damageCalculationMsg.setDamageTypeID (spell.getAttackSpellDamageTypeID ());
		damageCalculationMsg.setStoredDamageTypeID (damageType.getStoredDamageTypeID ());
		damageCalculationMsg.setDamageResolutionTypeID (spell.getAttackSpellDamageResolutionTypeID ());
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);

		// Fill in the damage object
		final AttackDamage attackDamage = new AttackDamage (damage, 0, damageType, null, spell, null, null, 1);
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
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateSingleFigureDamage (final AttackResolutionUnit defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final List<PlayerServerDetails> players,
		final FogOfWarMemory mem, final ServerDatabaseEx db) throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering calculateSingleFigureDamage: Unit URN " + defender.getUnit ().getUnitURN () + " hit by " + attackDamage);
		
		final int defenderDefenceStrength = getDamageTypeCalculations ().getDefenderDefenceStrength (defender.getUnit (), attackDamage, 1, players, mem, db);
		
		final int totalHits = calculateSingleFigureDamageInternal (defender, defenderDefenceStrength,
			attackingPlayer, defendingPlayer, attackDamage, players, mem, db);
		
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
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateArmourPiercingDamage (final AttackResolutionUnit defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering calculateArmourPiercingDamage: Unit URN " + defender.getUnit ().getUnitURN () + " hit by " + attackDamage);
		
		final int defenderDefenceStrength = getDamageTypeCalculations ().getDefenderDefenceStrength (defender.getUnit (), attackDamage, 2, players, mem, db);
		
		final int totalHits = calculateSingleFigureDamageInternal (defender, defenderDefenceStrength,
			attackingPlayer, defendingPlayer, attackDamage, players, mem, db);
		
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
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateIllusionaryDamage (final AttackResolutionUnit defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering calculateIllusionaryDamage: Unit URN " + defender.getUnit ().getUnitURN () + " hit by " + attackDamage);
		
		final int totalHits = calculateSingleFigureDamageInternal (defender, 0,
			attackingPlayer, defendingPlayer, attackDamage, players, mem, db);
		
		log.trace ("Exiting calculateIllusionaryDamage = " + totalHits);
		return totalHits;
	}
	
	/**
	 * Internal method that takes the defender's defence value as an input, so it can be used for single figure, armour piercing and illusionary damage.
	 * 
	 * @param defender Unit being hit
	 * @param defenderDefenceStrength Value of defence stat for the defender unit
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	private final int calculateSingleFigureDamageInternal (final AttackResolutionUnit defender, final int defenderDefenceStrength,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering calculateSingleFigureDamageInternal: Unit URN " + defender.getUnit ().getUnitURN () + " hit by " + attackDamage);
		
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.DEFENCE_DATA);
		damageCalculationMsg.setDefenderUnitURN (defender.getUnit ().getUnitURN ());
		damageCalculationMsg.setChanceToHit (attackDamage.getChanceToHit ());
		damageCalculationMsg.setTenTimesAverageDamage (attackDamage.getPotentialHits () * damageCalculationMsg.getChanceToHit ());
		damageCalculationMsg.setDamageResolutionTypeID (attackDamage.getDamageResolutionTypeID ());
		
		// How many actually hit
		int actualDamage = 0;
		for (int swingNo = 0; swingNo < attackDamage.getPotentialHits (); swingNo++)
			if (getRandomUtils ().nextInt (10) < damageCalculationMsg.getChanceToHit ())
				actualDamage++;
		
		damageCalculationMsg.setActualHits (actualDamage);
		
		// Set up defender stats
		final UnitHasSkillMergedList defenderSkills = getUnitUtils ().mergeSpellEffectsIntoSkillList (mem.getMaintainedSpell (), defender.getUnit (), db);
		
		damageCalculationMsg.setDefenderFigures (getUnitCalculations ().calculateAliveFigureCount (defender.getUnit (), players, mem, db));
		damageCalculationMsg.setUnmodifiedDefenceStrength (Math.max (0, getUnitSkillUtils ().getModifiedSkillValue (defender.getUnit (), defenderSkills,
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH,
			attackDamage.getAttackFromSkillID (), attackDamage.getAttackFromMagicRealmID (), players, mem, db)));
		damageCalculationMsg.setModifiedDefenceStrength (defenderDefenceStrength);

		damageCalculationMsg.setChanceToDefend (3 + Math.max (0, getUnitSkillUtils ().getModifiedSkillValue (defender.getUnit (), defenderSkills,
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH,
			attackDamage.getAttackFromSkillID (), attackDamage.getAttackFromMagicRealmID (), players, mem, db)));
		
		damageCalculationMsg.setTenTimesAverageBlock (damageCalculationMsg.getModifiedDefenceStrength () * damageCalculationMsg.getChanceToDefend ());
		
		// Dish out damage
		final int totalHits = getUnitServerUtils ().applyDamage (defender.getUnit (), actualDamage, damageCalculationMsg.getModifiedDefenceStrength (),
			damageCalculationMsg.getChanceToDefend (), players, mem, db);
		
		damageCalculationMsg.setFinalHits (totalHits);
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		
		log.trace ("Exiting calculateSingleFigureDamageInternal = " + totalHits);
		return totalHits;
	}

	/**
	 * Rolls the number of actual hits and blocks for "multi figure" a.k.a. immolation type damage, where all figures are hit individually by the attack,
	 * making their own to hit and defence rolls.
	 * 
	 * @param defender Unit being hit
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateMultiFigureDamage (final AttackResolutionUnit defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering calculateMultiFigureDamage: Unit URN " + defender.getUnit ().getUnitURN () + " hit by " + attackDamage);
		
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.DEFENCE_DATA);
		damageCalculationMsg.setDefenderUnitURN (defender.getUnit ().getUnitURN ());
		damageCalculationMsg.setChanceToHit (attackDamage.getChanceToHit ());
		damageCalculationMsg.setDamageResolutionTypeID (attackDamage.getDamageResolutionTypeID ());

		// Set up defender stats
		final UnitHasSkillMergedList defenderSkills = getUnitUtils ().mergeSpellEffectsIntoSkillList (mem.getMaintainedSpell (), defender.getUnit (), db);

		damageCalculationMsg.setDefenderFigures (getUnitCalculations ().calculateAliveFigureCount (defender.getUnit (), players, mem, db));
		damageCalculationMsg.setUnmodifiedDefenceStrength (Math.max (0, getUnitSkillUtils ().getModifiedSkillValue (defender.getUnit (), defenderSkills,
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH,
			attackDamage.getAttackFromSkillID (), attackDamage.getAttackFromMagicRealmID (), players, mem, db)));
		damageCalculationMsg.setModifiedDefenceStrength (getDamageTypeCalculations ().getDefenderDefenceStrength (defender.getUnit (), attackDamage, 1, players, mem, db));

		damageCalculationMsg.setChanceToDefend (3 + Math.max (0, getUnitSkillUtils ().getModifiedSkillValue (defender.getUnit (), defenderSkills,
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH,
			attackDamage.getAttackFromSkillID (), attackDamage.getAttackFromMagicRealmID (), players, mem, db)));
			
		damageCalculationMsg.setTenTimesAverageBlock (damageCalculationMsg.getModifiedDefenceStrength () * damageCalculationMsg.getChanceToDefend ());

		// For multi figure damage, we have to work this out last
		damageCalculationMsg.setTenTimesAverageDamage
			(attackDamage.getPotentialHits () * damageCalculationMsg.getChanceToHit () * damageCalculationMsg.getDefenderFigures ());

		// Keep track of how many HP the current figure has
		int hitPointsThisFigure = getUnitCalculations ().calculateHitPointsRemainingOfFirstFigure (defender.getUnit (), players, mem, db);
		
		// Attack each figure individually
		int actualDamage = 0;
		int totalHits = 0;
		for (int figureNo = 0; figureNo < damageCalculationMsg.getDefenderFigures (); figureNo++)
		{
			// How many hit this figure
			int damageToThisFigure = 0;
			for (int swingNo = 0; swingNo < attackDamage.getPotentialHits (); swingNo++)
				if (getRandomUtils ().nextInt (10) < damageCalculationMsg.getChanceToHit ())
				{
					actualDamage++;
					damageToThisFigure++;
				}
			
			// How many hits does this figure block
			int blocksFromThisFigure = 0;
			for (int blockNo = 0; blockNo < damageCalculationMsg.getModifiedDefenceStrength (); blockNo++)
				if (getRandomUtils ().nextInt (10) < damageCalculationMsg.getChanceToDefend ())
					blocksFromThisFigure++;
			
			// We can't do less than 0, or more than the full HP, damage to each individual figure
			int hitsOnThisFigure = damageToThisFigure - blocksFromThisFigure;
			if (hitsOnThisFigure < 0)
				hitsOnThisFigure = 0;
			else if (hitsOnThisFigure > hitPointsThisFigure)
				hitsOnThisFigure = hitPointsThisFigure;
			
			totalHits = totalHits + hitsOnThisFigure;
			
			// Keep track of how many HP the next figure has
			if ((figureNo == 0) && (damageCalculationMsg.getDefenderFigures () > 1))
				hitPointsThisFigure = getUnitSkillUtils ().getModifiedSkillValue (defender.getUnit (), defenderSkills, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS,
					UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, attackDamage.getAttackFromSkillID (), attackDamage.getAttackFromMagicRealmID (), players, mem, db);
		}
		
		// Store and send final totals
		damageCalculationMsg.setActualHits (actualDamage);		
		damageCalculationMsg.setFinalHits (totalHits);
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		
		log.trace ("Exiting calculateMultiFigureDamage = " + totalHits);
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
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateDoomDamage (final AttackResolutionUnit defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering calculateDoomDamage: Unit URN " + defender.getUnit ().getUnitURN () + " hit by " + attackDamage);
		
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.DEFENCE_DATA);
		damageCalculationMsg.setDefenderUnitURN (defender.getUnit ().getUnitURN ());
		damageCalculationMsg.setDamageResolutionTypeID (attackDamage.getDamageResolutionTypeID ());
		
		// No to hit rolls - they automatically hit
		damageCalculationMsg.setActualHits (attackDamage.getPotentialHits ());
		
		// No defence hit rolls - they automatically hit
		damageCalculationMsg.setDefenderFigures (getUnitCalculations ().calculateAliveFigureCount (defender.getUnit (), players, mem, db));

		// Dish out damage
		final int totalHits = getUnitServerUtils ().applyDamage (defender.getUnit (), attackDamage.getPotentialHits (), 0, 0, players, mem, db);
		
		damageCalculationMsg.setFinalHits (totalHits);
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		
		log.trace ("Exiting calculateDoomDamage = " + totalHits);
		return totalHits;
	}
	
	/**
	 * Sets the number of actual hits for "% chance of death" damage, used by cracks call.
	 * 
	 * @param defender Unit being hit
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateChanceOfDeathDamage (final AttackResolutionUnit defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering calculateChanceOfDeathDamage: Unit URN " + defender.getUnit ().getUnitURN () + " hit by " + attackDamage);
		
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.DEFENCE_DATA);
		damageCalculationMsg.setDefenderUnitURN (defender.getUnit ().getUnitURN ());
		damageCalculationMsg.setDamageResolutionTypeID (attackDamage.getDamageResolutionTypeID ());
		
		// Store the dice roll
		damageCalculationMsg.setActualHits (getRandomUtils ().nextInt (100));
		
		// No defence hit rolls - they automatically hit
		damageCalculationMsg.setDefenderFigures (getUnitCalculations ().calculateAliveFigureCount (defender.getUnit (), players, mem, db));

		// Unit either takes no damage, or dies outright
		if (damageCalculationMsg.getActualHits () < attackDamage.getPotentialHits ())
		{
			final int totalHits = getUnitServerUtils ().applyDamage (defender.getUnit (), Integer.MAX_VALUE, 0, 0, players, mem, db);
			damageCalculationMsg.setFinalHits (totalHits);
		}
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		
		log.trace ("Exiting calculateChanceOfDeathDamage = " + damageCalculationMsg.getFinalHits ());
		return damageCalculationMsg.getFinalHits ();
	}

	/**
	 * Rolls the number of actual hits for "each figure resist or die" damage, where each figure has to make a resistance roll.  Used for stoning gaze and many others.
	 * 
	 * @param defender Unit being hit
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateEachFigureResistOrDieDamage (final AttackResolutionUnit defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering calculateEachFigureResistOrDieDamage: Unit URN " + defender.getUnit ().getUnitURN () + " hit by " + attackDamage);
		
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.DEFENCE_DATA);
		damageCalculationMsg.setDefenderUnitURN (defender.getUnit ().getUnitURN ());
		damageCalculationMsg.setDamageResolutionTypeID (attackDamage.getDamageResolutionTypeID ());

		// Set up defender stats
		final UnitHasSkillMergedList defenderSkills = getUnitUtils ().mergeSpellEffectsIntoSkillList (mem.getMaintainedSpell (), defender.getUnit (), db);

		damageCalculationMsg.setDefenderFigures (getUnitCalculations ().calculateAliveFigureCount (defender.getUnit (), players, mem, db));
		damageCalculationMsg.setUnmodifiedDefenceStrength (Math.max (0, getUnitSkillUtils ().getModifiedSkillValue (defender.getUnit (), defenderSkills,
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH,
			attackDamage.getAttackFromSkillID (), attackDamage.getAttackFromMagicRealmID (), players, mem, db)));

		// Is there a saving throw modifier?
		int savingThrowModifier = (attackDamage.getPotentialHits () == null) ? 0 : attackDamage.getPotentialHits ();
		
		// Is there an additional saving throw modifier because of the magic realm/lifeform type of the target?
		// (Dispel Evil and Holy Word have an additional -5 modifier against Undead)
		if (attackDamage.getSpell () != null)
		{
			final SpellValidUnitTarget magicRealmLifeformTypeTarget = getSpellUtils ().findMagicRealmLifeformTypeTarget
				(attackDamage.getSpell (), getUnitUtils ().getModifiedUnitMagicRealmLifeformTypeID (defender.getUnit (), defender.getUnit ().getUnitHasSkill (), mem.getMaintainedSpell (), db));
			if ((magicRealmLifeformTypeTarget != null) && (magicRealmLifeformTypeTarget.getSavingThrowModifier () != null) &&
				(magicRealmLifeformTypeTarget.getSavingThrowModifier () != 0))
			{
				if (!CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE.equals (magicRealmLifeformTypeTarget.getSavingThrowSkillID ()))
					throw new MomException ("calculateEachFigureResistOrDieDamage from spell " + attackDamage.getSpell ().getSpellID () +
						" has a saving throw modifier that rolls against a stat other than resistance");
				
				savingThrowModifier = savingThrowModifier + magicRealmLifeformTypeTarget.getSavingThrowModifier ();
			}
		}
		
		// Work out the target's effective resistance score, reduced by any saving throw modifier
		damageCalculationMsg.setModifiedDefenceStrength (damageCalculationMsg.getUnmodifiedDefenceStrength () - savingThrowModifier);
		
		// Keep track of how many HP the current figure has
		int hitPointsThisFigure = getUnitCalculations ().calculateHitPointsRemainingOfFirstFigure (defender.getUnit (), players, mem, db);
		
		// Each figure individually
		int figuresDied = 0;
		int totalHits = 0;
		for (int figureNo = 0; figureNo < damageCalculationMsg.getDefenderFigures (); figureNo++)
		{
			// Make resistance roll for this figure
			if (getRandomUtils ().nextInt (10) >= damageCalculationMsg.getModifiedDefenceStrength ())
			{
				figuresDied++;
				totalHits = totalHits + hitPointsThisFigure;
			}
			
			// Keep track of how many HP the next figure has
			if ((figureNo == 0) && (damageCalculationMsg.getDefenderFigures () > 1))
				hitPointsThisFigure = getUnitSkillUtils ().getModifiedSkillValue (defender.getUnit (), defenderSkills, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS,
					UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, attackDamage.getAttackFromSkillID (), attackDamage.getAttackFromMagicRealmID (), players, mem, db);
		}
		
		// Store and send final totals
		damageCalculationMsg.setActualHits (figuresDied);		
		damageCalculationMsg.setFinalHits (totalHits);
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		
		log.trace ("Exiting calculateEachFigureResistOrDieDamage = " + totalHits);
		return totalHits;
	}
	
	/**
	 * Rolls the number of actual hits for "single figure resist or die" damage, where only one figure has to make a resistance roll.  Used for stoning touch.
	 * 
	 * @param defender Unit being hit
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateSingleFigureResistOrDieDamage (final AttackResolutionUnit defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering calculateSingleFigureResistOrDieDamage: Unit URN " + defender.getUnit ().getUnitURN () + " hit by " + attackDamage);
		
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.DEFENCE_DATA);
		damageCalculationMsg.setDefenderUnitURN (defender.getUnit ().getUnitURN ());
		damageCalculationMsg.setDamageResolutionTypeID (attackDamage.getDamageResolutionTypeID ());

		// Set up defender stats
		final UnitHasSkillMergedList defenderSkills = getUnitUtils ().mergeSpellEffectsIntoSkillList (mem.getMaintainedSpell (), defender.getUnit (), db);

		damageCalculationMsg.setDefenderFigures (getUnitCalculations ().calculateAliveFigureCount (defender.getUnit (), players, mem, db));
		damageCalculationMsg.setUnmodifiedDefenceStrength (Math.max (0, getUnitSkillUtils ().getModifiedSkillValue (defender.getUnit (), defenderSkills,
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH,
			attackDamage.getAttackFromSkillID (), attackDamage.getAttackFromMagicRealmID (), players, mem, db)));

		// Is there a saving throw modifier?
		int savingThrowModifier = (attackDamage.getPotentialHits () == null) ? 0 : attackDamage.getPotentialHits ();
		
		// Is there an additional saving throw modifier because of the magic realm/lifeform type of the target?
		// (Dispel Evil and Holy Word have an additional -5 modifier against Undead)
		if (attackDamage.getSpell () != null)
		{
			final SpellValidUnitTarget magicRealmLifeformTypeTarget = getSpellUtils ().findMagicRealmLifeformTypeTarget
				(attackDamage.getSpell (), getUnitUtils ().getModifiedUnitMagicRealmLifeformTypeID (defender.getUnit (), defender.getUnit ().getUnitHasSkill (), mem.getMaintainedSpell (), db));
			if ((magicRealmLifeformTypeTarget != null) && (magicRealmLifeformTypeTarget.getSavingThrowModifier () != null) &&
				(magicRealmLifeformTypeTarget.getSavingThrowModifier () != 0))
			{
				if (!CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE.equals (magicRealmLifeformTypeTarget.getSavingThrowSkillID ()))
					throw new MomException ("calculateSingleFigureResistOrDieDamage from spell " + attackDamage.getSpell ().getSpellID () +
						" has a saving throw modifier that rolls against a stat other than resistance");
				
				savingThrowModifier = savingThrowModifier + magicRealmLifeformTypeTarget.getSavingThrowModifier ();
			}
		}
		
		// Work out the target's effective resistance score, reduced by any saving throw modifier
		damageCalculationMsg.setModifiedDefenceStrength (damageCalculationMsg.getUnmodifiedDefenceStrength () - savingThrowModifier);
		
		// Make resistance roll
		final int totalHits;
		final int figuresDied;
		if (getRandomUtils ().nextInt (10) >= damageCalculationMsg.getModifiedDefenceStrength ())
		{
			figuresDied = 1;
			
			// Kill off an entire figure, unless there's only 1 figure left in which be careful that it may already be damaged
			if (damageCalculationMsg.getDefenderFigures () == 1)
				totalHits = getUnitCalculations ().calculateHitPointsRemainingOfFirstFigure (defender.getUnit (), players, mem, db);
			else
				totalHits = getUnitSkillUtils ().getModifiedSkillValue (defender.getUnit (), defenderSkills, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS,
					UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, attackDamage.getAttackFromSkillID (), attackDamage.getAttackFromMagicRealmID (), players, mem, db);
		}
		else
		{
			figuresDied = 0;
			totalHits = 0;
		}
		
		// Store and send final totals
		damageCalculationMsg.setActualHits (figuresDied);		
		damageCalculationMsg.setFinalHits (totalHits);
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		
		log.trace ("Exiting calculateSingleFigureResistOrDieDamage = " + totalHits);
		return totalHits;
	}
	
	/**
	 * Rolls the number of actual hits for "resist or take damage", where the unit takes damage equal to how
	 * much they miss a resistance roll by.  Used for Life Drain.
	 * 
	 * @param defender Unit being hit
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateResistOrTakeDamage (final AttackResolutionUnit defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering calculateResistOrTakeDamage: Unit URN " + defender.getUnit ().getUnitURN () + " hit by " + attackDamage);
		
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.DEFENCE_DATA);
		damageCalculationMsg.setDefenderUnitURN (defender.getUnit ().getUnitURN ());
		damageCalculationMsg.setDamageResolutionTypeID (attackDamage.getDamageResolutionTypeID ());

		// Set up defender stats
		damageCalculationMsg.setDefenderFigures (getUnitCalculations ().calculateAliveFigureCount (defender.getUnit (), players, mem, db));
		damageCalculationMsg.setUnmodifiedDefenceStrength (Math.max (0, getUnitSkillUtils ().getModifiedSkillValue (defender.getUnit (), defender.getUnit ().getUnitHasSkill (),
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH,
			attackDamage.getAttackFromSkillID (), attackDamage.getAttackFromMagicRealmID (), players, mem, db)));

		// Is there a saving throw modifier?
		final int savingThrowModifier = (attackDamage.getPotentialHits () == null) ? 0 : attackDamage.getPotentialHits ();
		damageCalculationMsg.setModifiedDefenceStrength (damageCalculationMsg.getUnmodifiedDefenceStrength () - savingThrowModifier);
		
		// Make resistance roll
		damageCalculationMsg.setActualHits (getRandomUtils ().nextInt (10));
		int totalHits = damageCalculationMsg.getActualHits () - damageCalculationMsg.getModifiedDefenceStrength ();
				
		// Can't do negative damage and can't overkill the unit
		if (totalHits < 0)
			totalHits = 0;
		else
			totalHits = getUnitServerUtils ().applyDamage (defender.getUnit (), totalHits, 0, 0, players, mem, db);
			
		// Store and send final totals
		damageCalculationMsg.setFinalHits (totalHits);
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		
		log.trace ("Exiting calculateResistOrTakeDamage = " + totalHits);
		return totalHits;
	}
	
	/**
	 * Rolls the number of actual hits for "resistance rolls damage", where the unit has to make n resistance rolls and
	 * loses 1 HP for each failed roll.  Used for Poison Touch.
	 * 
	 * @param defender Unit being hit
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateResistanceRollsDamage (final AttackResolutionUnit defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering calculateResistanceRollsDamage: Unit URN " + defender.getUnit ().getUnitURN () + " hit by " + attackDamage);
		
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.DEFENCE_DATA);
		damageCalculationMsg.setDefenderUnitURN (defender.getUnit ().getUnitURN ());
		damageCalculationMsg.setDamageResolutionTypeID (attackDamage.getDamageResolutionTypeID ());

		// Set up defender stats
		damageCalculationMsg.setDefenderFigures (getUnitCalculations ().calculateAliveFigureCount (defender.getUnit (), players, mem, db));
		damageCalculationMsg.setUnmodifiedDefenceStrength (Math.max (0, getUnitSkillUtils ().getModifiedSkillValue (defender.getUnit (), defender.getUnit ().getUnitHasSkill (),
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH,
			attackDamage.getAttackFromSkillID (), attackDamage.getAttackFromMagicRealmID (), players, mem, db)));

		// The potential hits is the number of rolls to make - this type of damage cannot have a saving throw modifier
		damageCalculationMsg.setModifiedDefenceStrength (damageCalculationMsg.getUnmodifiedDefenceStrength ());
		
		// Make rolls
		int totalHits = 0;
		for (int hitNo = 0; hitNo < attackDamage.getPotentialHits (); hitNo++)
			if (getRandomUtils ().nextInt (10) >= damageCalculationMsg.getModifiedDefenceStrength ())
				totalHits++;
		
		// Store and send final totals
		damageCalculationMsg.setActualHits (totalHits);
		
		// Can't overkill the unit
		totalHits = getUnitServerUtils ().applyDamage (defender.getUnit (), totalHits, 0, 0, players, mem, db);
		damageCalculationMsg.setFinalHits (totalHits);
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		
		log.trace ("Exiting calculateResistanceRollsDamage = " + totalHits);
		return totalHits;
	}
	
	/**
	 * Sets the number of actual hits for disintegrate, which completely kills the unit if it has 9 resistance or less.
	 * 
	 * @param defender Unit being hit
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateDisintegrateDamage (final AttackResolutionUnit defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering calculateDisintegrateDamage: Unit URN " + defender.getUnit ().getUnitURN () + " hit by " + attackDamage);
		
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.DEFENCE_DATA);
		damageCalculationMsg.setDefenderUnitURN (defender.getUnit ().getUnitURN ());
		damageCalculationMsg.setDamageResolutionTypeID (attackDamage.getDamageResolutionTypeID ());
		
		// Set up defender stats
		damageCalculationMsg.setDefenderFigures (getUnitCalculations ().calculateAliveFigureCount (defender.getUnit (), players, mem, db));
		damageCalculationMsg.setUnmodifiedDefenceStrength (Math.max (0, getUnitSkillUtils ().getModifiedSkillValue (defender.getUnit (), defender.getUnit ().getUnitHasSkill (),
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH,
			attackDamage.getAttackFromSkillID (), attackDamage.getAttackFromMagicRealmID (), players, mem, db)));

		// Is there a saving throw modifier?
		final int savingThrowModifier = (attackDamage.getPotentialHits () == null) ? 0 : attackDamage.getPotentialHits ();
		damageCalculationMsg.setModifiedDefenceStrength (damageCalculationMsg.getUnmodifiedDefenceStrength () - savingThrowModifier);

		// Unit either takes no damage, or dies outright
		if (damageCalculationMsg.getModifiedDefenceStrength () < 10)
		{
			final int totalHits = getUnitServerUtils ().applyDamage (defender.getUnit (), Integer.MAX_VALUE, 0, 0, players, mem, db);
			damageCalculationMsg.setFinalHits (totalHits);
		}
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		
		log.trace ("Exiting calculateDisintegrateDamage = " + damageCalculationMsg.getFinalHits ());
		return damageCalculationMsg.getFinalHits ();
	}
	
	/**
	 * Rolls the effect of a fear attack, which causes no actual damage, but can cause some of the figures to become
	 * frozen in fear and unable to attack for the remainder of this attack resolution.
	 * 
	 * @param defender Unit being hit
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final void calculateFearDamage (final AttackResolutionUnit defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering calculateFearDamage: Unit URN " + defender.getUnit ().getUnitURN () + " hit by " + attackDamage);
		
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.DEFENCE_DATA);
		damageCalculationMsg.setDefenderUnitURN (defender.getUnit ().getUnitURN ());
		damageCalculationMsg.setDamageResolutionTypeID (attackDamage.getDamageResolutionTypeID ());

		// Set up defender stats - note the minus here, so if 2 figures are already frozen, we can't roll for them again and use those rolls to freeze other figures
		damageCalculationMsg.setDefenderFigures (getUnitCalculations ().calculateAliveFigureCount (defender.getUnit (), players, mem, db) - defender.getFiguresFrozenInFear ());
		damageCalculationMsg.setUnmodifiedDefenceStrength (Math.max (0, getUnitSkillUtils ().getModifiedSkillValue (defender.getUnit (), defender.getUnit ().getUnitHasSkill (),
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH,
			attackDamage.getAttackFromSkillID (), attackDamage.getAttackFromMagicRealmID (), players, mem, db)));

		// Is there a saving throw modifier?
		final int savingThrowModifier = (attackDamage.getPotentialHits () == null) ? 0 : attackDamage.getPotentialHits ();
		
		// Work out the target's effective resistance score, reduced by any saving throw modifier
		damageCalculationMsg.setModifiedDefenceStrength (damageCalculationMsg.getUnmodifiedDefenceStrength () - savingThrowModifier);
		
		// Make resistance rolls
		int figuresFrozen = 0;
		for (int figureNo = 0; figureNo < damageCalculationMsg.getDefenderFigures (); figureNo++)
			if (getRandomUtils ().nextInt (10) >= damageCalculationMsg.getModifiedDefenceStrength ())
				figuresFrozen++;
		
		// Store and send final totals
		damageCalculationMsg.setActualHits (figuresFrozen);
		damageCalculationMsg.setFinalHits (figuresFrozen);
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		
		defender.setFiguresFrozenInFear (defender.getFiguresFrozenInFear () + figuresFrozen);
		
		log.trace ("Exiting calculateFearDamage = " + figuresFrozen);
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
	 * @return Damage type utils
	 */
	public final DamageTypeUtils getDamageTypeUtils ()
	{
		return damageTypeUtils;
	}

	/**
	 * @param utils Damage type utils
	 */
	public final void setDamageTypeUtils (final DamageTypeUtils utils)
	{
		damageTypeUtils = utils;
	}

	/**
	 * @return Damage type calculations
	 */
	public final DamageTypeCalculations getDamageTypeCalculations ()
	{
		return damageTypeCalculations;
	}

	/**
	 * @param calc Damage type calculations
	 */
	public final void setDamageTypeCalculations (final DamageTypeCalculations calc)
	{
		damageTypeCalculations = calc;
	}
}