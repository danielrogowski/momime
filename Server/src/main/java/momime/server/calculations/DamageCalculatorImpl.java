package momime.server.calculations;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DamageResolutionTypeID;
import momime.common.database.DamageType;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellValidUnitTarget;
import momime.common.database.UnitSkill;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.servertoclient.DamageCalculationAttackData;
import momime.common.messages.servertoclient.DamageCalculationData;
import momime.common.messages.servertoclient.DamageCalculationDefenceData;
import momime.common.messages.servertoclient.DamageCalculationMessage;
import momime.common.messages.servertoclient.DamageCalculationMessageTypeID;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.SpellUtils;
import momime.common.utils.UnitUtils;
import momime.server.database.ServerDatabaseValues;
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
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** Random number generator */
	private RandomUtils randomUtils;

	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;
	
	/** Spell utils */
	private SpellUtils spellUtils;
	
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
		final String attackSkillID, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		// We need the attacker's full details to generate the defender's full details, and vice versa, so this is a real chicken and egg situation.
		// e.g. Defender may have Weapon Immunity but the attacker has Holy Weapon which negates it
		// e.g. Attacker may have First Strike but the defender has Negate First Strike
		// So to get around this, we generate the attacker's stats twice.
		final ExpandedUnitDetails xuAttackerPreliminary = getUnitUtils ().expandUnitDetails (attacker.getUnit (), null, attackSkillID, null, players, mem, db);
		final List<ExpandedUnitDetails> attackers = new ArrayList<ExpandedUnitDetails> ();
		attackers.add (xuAttackerPreliminary);
		
		// In fact worse than that.  If attacker is invisible, the stats we worked out for them above will say they're invisible even if we have True Sight,
		// because we haven't taken the defender into account yet.  That means if we work the final defender stats out now, they'll suffer -1 to hit. 
		final ExpandedUnitDetails xuDefenderPreliminary = getUnitUtils ().expandUnitDetails (defender.getUnit (), attackers, attackSkillID, null, players, mem, db);
		final List<ExpandedUnitDetails> defenders = new ArrayList<ExpandedUnitDetails> ();
		defenders.add (xuDefenderPreliminary);

		// Now work the real stats out
		final ExpandedUnitDetails xuAttacker = getUnitUtils ().expandUnitDetails (attacker.getUnit (), defenders, attackSkillID, null, players, mem, db);
		attackers.clear ();
		attackers.add (xuAttacker);
		
		final ExpandedUnitDetails xuDefender = getUnitUtils ().expandUnitDetails (defender.getUnit (), attackers, attackSkillID, null, players, mem, db);
		
		// The unit's skill level indicates the strength of the attack (e.g. Poison Touch 2 vs Poison Touch 4)
		final AttackDamage attackDamage;
		final int figureCount = xuAttacker.calculateAliveFigureCount () - attacker.getFiguresFrozenInFear ();
		if ((!xuAttacker.hasModifiedSkill (attackSkillID)) || (figureCount <= 0))
			attackDamage = null;
		else
		{
			// nulls are ok here, its possible to attack with some valueless skills, such as Cloak of Fear.
			// However negative values are not ok, that would indicate our attack had been reduced to useless by some effect such as Black Prayer.
			final Integer damage = xuAttacker.getModifiedSkillValue (attackSkillID);
			if ((damage != null) && (damage < 0))
				attackDamage = null;
			else
			{			
				// Certain types of damage resolution must do at least one point of attack in order to even stand a chance
				final UnitSkill unitSkill = db.findUnitSkill (attackSkillID, "attackFromUnitSkill");
				if (unitSkill.getDamageResolutionTypeID () == null)
					throw new MomException ("attackFromUnitSkill tried to attack with skill " + attackSkillID + ", but it has no damageResolutionTypeID defined");
				
				if (((damage == null) || (damage < 1)) &&
					((unitSkill.getDamageResolutionTypeID () == DamageResolutionTypeID.SINGLE_FIGURE) ||
					(unitSkill.getDamageResolutionTypeID () == DamageResolutionTypeID.ARMOUR_PIERCING) ||
					(unitSkill.getDamageResolutionTypeID () == DamageResolutionTypeID.ILLUSIONARY) ||
					(unitSkill.getDamageResolutionTypeID () == DamageResolutionTypeID.MULTI_FIGURE) ||
					(unitSkill.getDamageResolutionTypeID () == DamageResolutionTypeID.DOOM) ||
					(unitSkill.getDamageResolutionTypeID () == DamageResolutionTypeID.RESISTANCE_ROLLS)))
					
					attackDamage = null;
				else
				{
					// Figure out the magic realm of the attack; getting a null here is fine, this just means the attack deals physical damage
					final String attackFromMagicRealmID;
					if (attackSkillID.equals (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK))
					{
						// Ranged attack magic realms are defined against the RAT rather than the realm of the attacker
						// so e.g. Storm Giants throw lightning bolts, which are Chaos-based RATs, rather than the Storm Giant being a Sorcery creature... which is a bit weird
						attackFromMagicRealmID = xuAttacker.getRangedAttackType ().getMagicRealmID ();
					}
					else
					{
						// Special attack magic realms are defined against the unit skill rather than the realm of the attacker
						// so e.g. Sky Drakes have lightning breath, which is a skill that deals Chaos damage, rather than the Sky Drake being a Sorcery creature... which is also a bit weird
						attackFromMagicRealmID = unitSkill.getMagicRealmID ();
					}
		
					// Figure out the type of damage, and check whether the defender is immune to it.
					// Firstly if the unit has the "create undead" skill, then force all damage to "life stealing" as long as the defender isn't immune to it -
					// if they are immune to it, leave it as regular melee damage (tested in the original MoM that Ghouls can hurt Zombies).
					DamageType damageType = null;
					if (xuAttacker.hasModifiedSkill (ServerDatabaseValues.UNIT_SKILL_ID_CREATE_UNDEAD))
					{
						damageType = db.findDamageType (ServerDatabaseValues.DAMAGE_TYPE_ID_LIFE_STEALING, "attackFromUnitSkill");
						if (xuDefender.isUnitImmuneToDamageType (damageType))
							damageType = null;
					}
					
					// If life stealing damage didn't apply, just use whatever is defined against the unit skill like normal
					if (damageType == null)
						damageType = getDamageTypeCalculations ().determineSkillDamageType (xuAttacker, attackSkillID, db);
					
					if (xuDefender.isUnitImmuneToDamageType (damageType))
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
						
						// Different skills deal different types of damage; illusionary attack skill overrides the damage resolution type, if the defender isn't immune to it
						damageCalculationMsg.setDamageResolutionTypeID (unitSkill.getDamageResolutionTypeID ());
						if ((unitSkill.isDamageResolutionTypeUpgradeable () != null) && (unitSkill.isDamageResolutionTypeUpgradeable ()))
						{
							if (xuAttacker.hasModifiedSkill (ServerDatabaseValues.UNIT_SKILL_ID_ILLUSIONARY_ATTACK))
								damageCalculationMsg.setDamageResolutionTypeID (DamageResolutionTypeID.ILLUSIONARY);
							else if (xuAttacker.hasModifiedSkill (ServerDatabaseValues.UNIT_SKILL_ID_ARMOUR_PIERCING))
								damageCalculationMsg.setDamageResolutionTypeID (DamageResolutionTypeID.ARMOUR_PIERCING);
						}
						
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
						final int plusToHit = !xuAttacker.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT) ? 0 :
							xuAttacker.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT);
				
						attackDamage = new AttackDamage (damageCalculationMsg.getPotentialHits (), plusToHit, damageType, damageCalculationMsg.getDamageResolutionTypeID (), null,
							attackSkillID, attackFromMagicRealmID, repetitions);
					}
				}
			}
		}
		
		return attackDamage;
	}
	
	/**
	 * Calculates the strength of an attack coming from a spell.  Note the same output from here is used to attack *all* the defenders,
	 * so should not do anything here regarding any immunities each individual defender may have.
	 *  
	 * @param spell The spell being cast
	 * @param variableDamage The damage chosen, for spells where variable mana can be channeled into casting them, e.g. fire bolt
	 * @param castingPlayer The player casting the spell
	 * @param castingUnit Unit who is casting the spell; null means its the wizard casting, rather than a specific unit
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If there is a problem with the game logic
	 */
	@Override
	public final AttackDamage attackFromSpell (final Spell spell, final Integer variableDamage, final PlayerServerDetails castingPlayer, final ExpandedUnitDetails castingUnit,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final CommonDatabase db)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException
	{
		// Work out damage done - note this isn't applicable to all types of attack, e.g. Warp Wood has no attack value, so we might get null here
		Integer damage = (variableDamage != null) ? variableDamage : spell.getCombatBaseDamage ();
		final DamageType damageType = db.findDamageType (spell.getAttackSpellDamageTypeID (), "attackFromSpell");

		// For spells that roll against resistance, add on any -spell save from hero items
		// RESISTANCE_ROLLS is intentionally excluded, as for that "damage" a.k.a. "potentialHits" is the number of rolls to me - so can't modify this by the saving throw penalty
		if ((castingUnit != null) && (castingUnit.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_SAVING_THROW_PENALTY)) &&
			((spell.getAttackSpellDamageResolutionTypeID () == DamageResolutionTypeID.EACH_FIGURE_RESIST_OR_DIE) ||
				(spell.getAttackSpellDamageResolutionTypeID () == DamageResolutionTypeID.SINGLE_FIGURE_RESIST_OR_DIE) ||
				(spell.getAttackSpellDamageResolutionTypeID () == DamageResolutionTypeID.RESIST_OR_TAKE_DAMAGE) ||
				(spell.getAttackSpellDamageResolutionTypeID () == DamageResolutionTypeID.DISINTEGRATE)))
			
			damage = ((damage == null) ? 0 : damage) + castingUnit.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_SAVING_THROW_PENALTY);
		
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
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateSingleFigureDamage (final ExpandedUnitDetails defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage) throws MomException, JAXBException, XMLStreamException
	{
		final int defenderDefenceStrength = getDamageTypeCalculations ().getDefenderDefenceStrength (defender, attackDamage, 1);
		
		final int totalHits = calculateSingleFigureDamageInternal (defender, defenderDefenceStrength, attackingPlayer, defendingPlayer, attackDamage);
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
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateArmourPiercingDamage (final ExpandedUnitDetails defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage) throws MomException, JAXBException, XMLStreamException
	{
		final int defenderDefenceStrength = getDamageTypeCalculations ().getDefenderDefenceStrength (defender, attackDamage, 2);
		
		final int totalHits = calculateSingleFigureDamageInternal (defender, defenderDefenceStrength, attackingPlayer, defendingPlayer, attackDamage);
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
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateIllusionaryDamage (final ExpandedUnitDetails defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage) throws MomException, JAXBException, XMLStreamException
	{
		final int totalHits = calculateSingleFigureDamageInternal (defender, 0, attackingPlayer, defendingPlayer, attackDamage);
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
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	private final int calculateSingleFigureDamageInternal (final ExpandedUnitDetails defender, final int defenderDefenceStrength,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final AttackDamage attackDamage)
		throws MomException, JAXBException, XMLStreamException
	{
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.DEFENCE_DATA);
		damageCalculationMsg.setDefenderUnitURN (defender.getUnitURN ());
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
		damageCalculationMsg.setDefenderFigures (defender.calculateAliveFigureCount ());
		damageCalculationMsg.setUnmodifiedDefenceStrength (!defender.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE) ? 0 :
			Math.max (0, defender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE)));
		damageCalculationMsg.setModifiedDefenceStrength (defenderDefenceStrength);

		damageCalculationMsg.setChanceToDefend (Math.max (0, 3 + (!defender.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK) ? 0 :
			defender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK))));
		damageCalculationMsg.setTenTimesAverageBlock (damageCalculationMsg.getModifiedDefenceStrength () * damageCalculationMsg.getChanceToDefend ());
		
		// Dish out damage
		final int totalHits = getUnitServerUtils ().applyDamage (defender, actualDamage, damageCalculationMsg.getModifiedDefenceStrength (),
			damageCalculationMsg.getChanceToDefend ());
		
		damageCalculationMsg.setFinalHits (totalHits);
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		
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
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateMultiFigureDamage (final ExpandedUnitDetails defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage) throws MomException, JAXBException, XMLStreamException
	{
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.DEFENCE_DATA);
		damageCalculationMsg.setDefenderUnitURN (defender.getUnitURN ());
		damageCalculationMsg.setChanceToHit (attackDamage.getChanceToHit ());
		damageCalculationMsg.setDamageResolutionTypeID (attackDamage.getDamageResolutionTypeID ());

		// Set up defender stats
		damageCalculationMsg.setDefenderFigures (defender.calculateAliveFigureCount ());
		damageCalculationMsg.setUnmodifiedDefenceStrength (!defender.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE) ? 0 :
			Math.max (0, defender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE)));
		damageCalculationMsg.setModifiedDefenceStrength (getDamageTypeCalculations ().getDefenderDefenceStrength (defender, attackDamage, 1));
		damageCalculationMsg.setChanceToDefend (Math.max (0, 3 + (!defender.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK) ? 0 :
			defender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK))));
		damageCalculationMsg.setTenTimesAverageBlock (damageCalculationMsg.getModifiedDefenceStrength () * damageCalculationMsg.getChanceToDefend ());

		// For multi figure damage, we have to work this out last
		damageCalculationMsg.setTenTimesAverageDamage
			(attackDamage.getPotentialHits () * damageCalculationMsg.getChanceToHit () * damageCalculationMsg.getDefenderFigures ());

		// Keep track of how many HP the current figure has
		int hitPointsThisFigure = defender.calculateHitPointsRemainingOfFirstFigure ();
		
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
				hitPointsThisFigure = defender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS);
		}
		
		// Store and send final totals
		damageCalculationMsg.setActualHits (actualDamage);		
		damageCalculationMsg.setFinalHits (totalHits);
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		
		return totalHits;
	}
	
	/**
	 * Sets the number of actual hits for "doom" type constant damage.
	 * 
	 * @param defender Unit being hit
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateDoomDamage (final ExpandedUnitDetails defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage) throws MomException, JAXBException, XMLStreamException
	{
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.DEFENCE_DATA);
		damageCalculationMsg.setDefenderUnitURN (defender.getUnitURN ());
		damageCalculationMsg.setDamageResolutionTypeID (attackDamage.getDamageResolutionTypeID ());
		
		// No to hit rolls - they automatically hit
		damageCalculationMsg.setActualHits (attackDamage.getPotentialHits ());
		
		// No defence hit rolls - they automatically hit
		damageCalculationMsg.setDefenderFigures (defender.calculateAliveFigureCount ());

		// Dish out damage
		final int totalHits = getUnitServerUtils ().applyDamage (defender, attackDamage.getPotentialHits (), 0, 0);
		
		damageCalculationMsg.setFinalHits (totalHits);
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		
		return totalHits;
	}
	
	/**
	 * Sets the number of actual hits for "% chance of death" damage, used by cracks call.
	 * 
	 * @param defender Unit being hit
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateChanceOfDeathDamage (final ExpandedUnitDetails defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage) throws MomException, JAXBException, XMLStreamException
	{
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.DEFENCE_DATA);
		damageCalculationMsg.setDefenderUnitURN (defender.getUnitURN ());
		damageCalculationMsg.setDamageResolutionTypeID (attackDamage.getDamageResolutionTypeID ());
		
		// Store the dice roll
		damageCalculationMsg.setActualHits (getRandomUtils ().nextInt (100));
		
		// No defence hit rolls - they automatically hit
		damageCalculationMsg.setDefenderFigures (defender.calculateAliveFigureCount ());

		// Unit either takes no damage, or dies outright
		if (damageCalculationMsg.getActualHits () < attackDamage.getPotentialHits ())
		{
			final int totalHits = getUnitServerUtils ().applyDamage (defender, Integer.MAX_VALUE, 0, 0);
			damageCalculationMsg.setFinalHits (totalHits);
		}
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		
		return damageCalculationMsg.getFinalHits ();
	}

	/**
	 * Rolls the number of actual hits for "each figure resist or die" damage, where each figure has to make a resistance roll.  Used for stoning gaze and many others.
	 * 
	 * @param defender Unit being hit
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateEachFigureResistOrDieDamage (final ExpandedUnitDetails defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage) throws MomException, JAXBException, XMLStreamException
	{
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.DEFENCE_DATA);
		damageCalculationMsg.setDefenderUnitURN (defender.getUnitURN ());
		damageCalculationMsg.setDamageResolutionTypeID (attackDamage.getDamageResolutionTypeID ());

		// Set up defender stats
		damageCalculationMsg.setDefenderFigures (defender.calculateAliveFigureCount ());
		damageCalculationMsg.setUnmodifiedDefenceStrength (Math.max (0, defender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE)));

		// Is there a saving throw modifier?
		int savingThrowModifier = (attackDamage.getPotentialHits () == null) ? 0 : attackDamage.getPotentialHits ();
		
		// Is there an additional saving throw modifier because of the magic realm/lifeform type of the target?
		// (Dispel Evil and Holy Word have an additional -5 modifier against Undead)
		if (attackDamage.getSpell () != null)
		{
			final SpellValidUnitTarget magicRealmLifeformTypeTarget = getSpellUtils ().findMagicRealmLifeformTypeTarget
				(attackDamage.getSpell (), defender.getModifiedUnitMagicRealmLifeformType ().getPickID ());
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
		int hitPointsThisFigure = defender.calculateHitPointsRemainingOfFirstFigure ();
		
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
				hitPointsThisFigure = defender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS);
		}
		
		// Store and send final totals
		damageCalculationMsg.setActualHits (figuresDied);		
		damageCalculationMsg.setFinalHits (totalHits);
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		
		return totalHits;
	}
	
	/**
	 * Rolls the number of actual hits for "single figure resist or die" damage, where only one figure has to make a resistance roll.  Used for stoning touch.
	 * 
	 * @param defender Unit being hit
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateSingleFigureResistOrDieDamage (final ExpandedUnitDetails defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage) throws MomException, JAXBException, XMLStreamException
	{
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.DEFENCE_DATA);
		damageCalculationMsg.setDefenderUnitURN (defender.getUnitURN ());
		damageCalculationMsg.setDamageResolutionTypeID (attackDamage.getDamageResolutionTypeID ());

		// Set up defender stats
		damageCalculationMsg.setDefenderFigures (defender.calculateAliveFigureCount ());
		damageCalculationMsg.setUnmodifiedDefenceStrength (Math.max (0, defender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE)));

		// Is there a saving throw modifier?
		int savingThrowModifier = (attackDamage.getPotentialHits () == null) ? 0 : attackDamage.getPotentialHits ();
		
		// Is there an additional saving throw modifier because of the magic realm/lifeform type of the target?
		// (Dispel Evil and Holy Word have an additional -5 modifier against Undead)
		if (attackDamage.getSpell () != null)
		{
			final SpellValidUnitTarget magicRealmLifeformTypeTarget = getSpellUtils ().findMagicRealmLifeformTypeTarget
				(attackDamage.getSpell (), defender.getModifiedUnitMagicRealmLifeformType ().getPickID ());
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
				totalHits = defender.calculateHitPointsRemainingOfFirstFigure ();
			else
				totalHits = defender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS);
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
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateResistOrTakeDamage (final ExpandedUnitDetails defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage) throws MomException, JAXBException, XMLStreamException
	{
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.DEFENCE_DATA);
		damageCalculationMsg.setDefenderUnitURN (defender.getUnitURN ());
		damageCalculationMsg.setDamageResolutionTypeID (attackDamage.getDamageResolutionTypeID ());

		// Set up defender stats
		damageCalculationMsg.setDefenderFigures (defender.calculateAliveFigureCount ());
		damageCalculationMsg.setUnmodifiedDefenceStrength (Math.max (0, defender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE)));

		// Is there a saving throw modifier?
		final int savingThrowModifier = (attackDamage.getPotentialHits () == null) ? 0 : attackDamage.getPotentialHits ();
		damageCalculationMsg.setModifiedDefenceStrength (damageCalculationMsg.getUnmodifiedDefenceStrength () - savingThrowModifier);
		
		// Make resistance roll
		damageCalculationMsg.setActualHits (getRandomUtils ().nextInt (10) + 1);
		int totalHits = damageCalculationMsg.getActualHits () - damageCalculationMsg.getModifiedDefenceStrength ();
				
		// Can't do negative damage and can't overkill the unit
		if (totalHits < 0)
			totalHits = 0;
		else
			totalHits = getUnitServerUtils ().applyDamage (defender, totalHits, 0, 0);
			
		// Store and send final totals
		damageCalculationMsg.setFinalHits (totalHits);
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		
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
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateResistanceRollsDamage (final ExpandedUnitDetails defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage) throws MomException, JAXBException, XMLStreamException
	{
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.DEFENCE_DATA);
		damageCalculationMsg.setDefenderUnitURN (defender.getUnitURN ());
		damageCalculationMsg.setDamageResolutionTypeID (attackDamage.getDamageResolutionTypeID ());

		// Set up defender stats
		damageCalculationMsg.setDefenderFigures (defender.calculateAliveFigureCount ());
		damageCalculationMsg.setUnmodifiedDefenceStrength (defender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE));

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
		totalHits = getUnitServerUtils ().applyDamage (defender, totalHits, 0, 0);
		damageCalculationMsg.setFinalHits (totalHits);
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		
		return totalHits;
	}
	
	/**
	 * Sets the number of actual hits for disintegrate, which completely kills the unit if it has 9 resistance or less.
	 * 
	 * @param defender Unit being hit
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateDisintegrateDamage (final ExpandedUnitDetails defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage) throws MomException, JAXBException, XMLStreamException
	{
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.DEFENCE_DATA);
		damageCalculationMsg.setDefenderUnitURN (defender.getUnitURN ());
		damageCalculationMsg.setDamageResolutionTypeID (attackDamage.getDamageResolutionTypeID ());
		
		// Set up defender stats
		damageCalculationMsg.setDefenderFigures (defender.calculateAliveFigureCount ());
		damageCalculationMsg.setUnmodifiedDefenceStrength (defender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE));

		// Is there a saving throw modifier?
		final int savingThrowModifier = (attackDamage.getPotentialHits () == null) ? 0 : attackDamage.getPotentialHits ();
		damageCalculationMsg.setModifiedDefenceStrength (damageCalculationMsg.getUnmodifiedDefenceStrength () - savingThrowModifier);

		// Unit either takes no damage, or dies outright
		if (damageCalculationMsg.getModifiedDefenceStrength () < 10)
		{
			final int totalHits = getUnitServerUtils ().applyDamage (defender, Integer.MAX_VALUE, 0, 0);
			damageCalculationMsg.setFinalHits (totalHits);
		}
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		
		return damageCalculationMsg.getFinalHits ();
	}
	
	/**
	 * Rolls the effect of a fear attack, which causes no actual damage, but can cause some of the figures to become
	 * frozen in fear and unable to attack for the remainder of this attack resolution.
	 * 
	 * @param defender Unit being hit
	 * @param xuDefender Expanded details about unit being hit
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final void calculateFearDamage (final AttackResolutionUnit defender, final ExpandedUnitDetails xuDefender,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage) throws MomException, JAXBException, XMLStreamException
	{
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setMessageType (DamageCalculationMessageTypeID.DEFENCE_DATA);
		damageCalculationMsg.setDefenderUnitURN (xuDefender.getUnitURN ());
		damageCalculationMsg.setDamageResolutionTypeID (attackDamage.getDamageResolutionTypeID ());

		// Set up defender stats - note the minus here, so if 2 figures are already frozen, we can't roll for them again and use those rolls to freeze other figures
		damageCalculationMsg.setDefenderFigures (xuDefender.calculateAliveFigureCount ()  - defender.getFiguresFrozenInFear ());
		damageCalculationMsg.setUnmodifiedDefenceStrength (Math.max (0, xuDefender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE)));

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