package momime.server.calculations;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.sessionbase.PlayerType;
import com.ndg.utils.Holder;
import com.ndg.utils.random.RandomUtils;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.calculations.UnitCalculations;
import momime.common.database.AttackSpellTargetID;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DamageResolutionTypeID;
import momime.common.database.DamageType;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.UnitSkill;
import momime.common.database.ValidUnitTarget;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.DamageCalculationAttackData;
import momime.common.messages.servertoclient.DamageCalculationData;
import momime.common.messages.servertoclient.DamageCalculationDefenceData;
import momime.common.messages.servertoclient.DamageCalculationHeaderData;
import momime.common.messages.servertoclient.DamageCalculationMessage;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.SpellCastType;
import momime.common.utils.SpellUtils;
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
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
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
		if (((attackingPlayer != null) && (attackingPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)) ||
			((defendingPlayer != null) && (defendingPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)))
		{
			final DamageCalculationMessage wrapper = new DamageCalculationMessage ();
			wrapper.setBreakdown (msg);
			
			if ((attackingPlayer != null) && (attackingPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN))
				attackingPlayer.getConnection ().sendMessageToClient (wrapper);
			
			if ((defendingPlayer != attackingPlayer) && (defendingPlayer != null) && (defendingPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN))
				defendingPlayer.getConnection ().sendMessageToClient (wrapper);
		}
	}

	/**
	 * Sends header about one attack that's about to take place or one spell that's about to be cast in comat
	 * 
	 * @param attacker Unit making the attack; or null if the attack isn't coming from a unit
	 * @param defenders Unit(s) being hit; some attacks can hit multiple units such as Flame Strike
	 * @param existingCurse True if this isn't a new "attack", but is the defender trying to shake off an existing curse (Stasis)
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param eventID The event that caused an attack, if it wasn't initiated by a player
	 * @param attackSkillID The skill being used to attack, i.e. UA01 (swords) or UA02 (ranged); or null if the attack isn't coming from a unit
	 * @param spell The spell being cast; or null if the attack isn't coming from a spell
	 * @param castingPlayer The player casting the spell; or null if the attack isn't coming from a spell
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final void sendDamageHeader (final MemoryUnit attacker, final List<MemoryUnit> defenders, final boolean existingCurse,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final String eventID,
		final String attackSkillID, final Spell spell, final PlayerServerDetails castingPlayer)
		throws JAXBException, XMLStreamException
	{
		final DamageCalculationHeaderData damageCalculationMsg = new DamageCalculationHeaderData ();
		damageCalculationMsg.setAttackSkillID (attackSkillID);
		damageCalculationMsg.setEventID (eventID);

		// Unit curses don't have the single/all units flag, they always have a single target
		if ((defenders.size () > 0) && ((spell == null) || (spell.getAttackSpellCombatTarget () == AttackSpellTargetID.SINGLE_UNIT) ||
			(spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_CURSES)))
			
			damageCalculationMsg.setDefenderUnitURN (defenders.get (0).getUnitURN ());
		
		if (spell != null)
			damageCalculationMsg.setAttackSpellID (spell.getSpellID ());

		if (attacker != null)
		{
			damageCalculationMsg.setAttackerUnitURN (attacker.getUnitURN ());
			damageCalculationMsg.setAttackerPlayerID (attacker.getOwningPlayerID ());
		}
		else if (castingPlayer != null)
			damageCalculationMsg.setAttackerPlayerID (castingPlayer.getPlayerDescription ().getPlayerID ());
		
		if (existingCurse)
			damageCalculationMsg.setExistingCurse (true);
		
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
	}
	
	/**
	 * Calculates the strength of an attack coming from a unit skill, e.g. Thrown Weapons, breath and gaze attacks, or Posion Touch
	 * 
	 * @param attacker Unit making the attack
	 * @param defender Unit being attacked
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackSkillID The skill being used to attack
	 * @param requiredSkillID Optional secondary skill we must also have in order to make this kind of attack; usually null
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
		final String attackSkillID, final String requiredSkillID, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		// We need the attacker's full details to generate the defender's full details, and vice versa, so this is a real chicken and egg situation.
		// e.g. Defender may have Weapon Immunity but the attacker has Holy Weapon which negates it
		// e.g. Attacker may have First Strike but the defender has Negate First Strike
		// So to get around this, we generate the attacker's stats twice.
		final ExpandedUnitDetails xuAttackerPreliminary = getExpandUnitDetails ().expandUnitDetails (attacker.getUnit (), null, attackSkillID, null, players, mem, db);
		final List<ExpandedUnitDetails> attackers = new ArrayList<ExpandedUnitDetails> ();
		attackers.add (xuAttackerPreliminary);
		
		// In fact worse than that.  If attacker is invisible, the stats we worked out for them above will say they're invisible even if we have True Sight,
		// because we haven't taken the defender into account yet.  That means if we work the final defender stats out now, they'll suffer -1 to hit. 
		final ExpandedUnitDetails xuDefenderPreliminary = getExpandUnitDetails ().expandUnitDetails (defender.getUnit (), attackers, attackSkillID, null, players, mem, db);
		final List<ExpandedUnitDetails> defenders = new ArrayList<ExpandedUnitDetails> ();
		defenders.add (xuDefenderPreliminary);

		// Now work the real stats out
		final ExpandedUnitDetails xuAttacker = getExpandUnitDetails ().expandUnitDetails (attacker.getUnit (), defenders, attackSkillID, null, players, mem, db);
		attackers.clear ();
		attackers.add (xuAttacker);
		
		final ExpandedUnitDetails xuDefender = getExpandUnitDetails ().expandUnitDetails (defender.getUnit (), attackers, attackSkillID, null, players, mem, db);
		
		// The unit's skill level indicates the strength of the attack (e.g. Poison Touch 2 vs Poison Touch 4)
		final AttackDamage attackDamage;
		final int figureCount = xuAttacker.calculateAliveFigureCount () - attacker.getFiguresFrozenInFear ();
		
		// If attacker is dead then can't do any damage
		if (figureCount <= 0)
			attackDamage = null;
		else
		{
			final boolean haveSkill;
			Integer damage = null;

			// We have to have the attack skill (normal check)
			if (requiredSkillID == null)
			{
				haveSkill = xuAttacker.hasModifiedSkill (attackSkillID);
				if (haveSkill)
					damage = xuAttacker.getModifiedSkillValue (attackSkillID);
			}
			
			// If there is a secondary required skill, then we need to have it, and then compute the skill map with respect to that skill instead to see if we have the main skill,
			// because what we are asking is "Do we have a thrown weapon attack, and does our Touch Dispels Evil skill apply to thrown weapon attacks?"
			// since this has to take into account what kind of hero weapon granted us the skill.
			else if (xuAttacker.hasModifiedSkill (requiredSkillID))
			{
				final ExpandedUnitDetails xuAttackerRequiredSkill = getExpandUnitDetails ().expandUnitDetails
					(attacker.getUnit (), defenders, requiredSkillID, null, players, mem, db);
				haveSkill = xuAttackerRequiredSkill.hasModifiedSkill (attackSkillID);
				if (haveSkill)
					damage = xuAttackerRequiredSkill.getModifiedSkillValue (attackSkillID);
			}
			else
				haveSkill = false;
			
			if (!haveSkill)
				attackDamage = null;
			else
			{
				// nulls are ok here, its possible to attack with some valueless skills, such as Cloak of Fear.
				// However negative values are not ok, that would indicate our attack had been reduced to useless by some effect such as Black Prayer.
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
					
					// Check for skills that only affect certain types of creatures (you can't Touch Dispels Evil normal units)
					else if ((unitSkill.getSkillValidUnitTarget ().size () > 0) && (unitSkill.getSkillValidUnitTarget ().stream ().noneMatch
						(t -> t.getTargetMagicRealmID ().equals (xuDefender.getModifiedUnitMagicRealmLifeformType ().getPickID ()))))
						
						attackDamage = null;
					
					else
					{
						// Does the particular magic realm/lifeform type of the target make it take more damage than usual?
						if (damage != null)
							for (final ValidUnitTarget validUnitTarget : unitSkill.getSkillValidUnitTarget ())
								if ((validUnitTarget.getTargetMagicRealmID ().equals (xuDefender.getModifiedUnitMagicRealmLifeformType ().getPickID ())) &&
									(validUnitTarget.getMagicRealmAdditionalSavingThrowModifier () != null))
									
									damage = damage + validUnitTarget.getMagicRealmAdditionalSavingThrowModifier ();
						
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
						if (xuAttacker.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_CREATE_UNDEAD))
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
							damageCalculationMsg.setAttackerUnitURN (attacker.getUnit ().getUnitURN ());
							damageCalculationMsg.setAttackerPlayerID (attacker.getUnit ().getOwningPlayerID ());
							damageCalculationMsg.setAttackSkillID (attackSkillID);
							damageCalculationMsg.setRequiredUnitSkillID (requiredSkillID);
							damageCalculationMsg.setDamageTypeID (damageType.getDamageTypeID ());
							damageCalculationMsg.setStoredDamageTypeID (damageType.getStoredDamageTypeID ());
							
							// Different skills deal different types of damage; illusionary attack skill overrides the damage resolution type, if the defender isn't immune to it
							damageCalculationMsg.setDamageResolutionTypeID (unitSkill.getDamageResolutionTypeID ());
							if ((unitSkill.isDamageResolutionTypeUpgradeable () != null) && (unitSkill.isDamageResolutionTypeUpgradeable ()))
							{
								if (xuAttacker.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_DOOM_ATTACK))
									damageCalculationMsg.setDamageResolutionTypeID (DamageResolutionTypeID.DOOM);
								else if (xuAttacker.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_ILLUSIONARY_ATTACK))
									damageCalculationMsg.setDamageResolutionTypeID (DamageResolutionTypeID.ILLUSIONARY);
								else if (xuAttacker.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_ARMOUR_PIERCING))
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
							// Work out to hit bonuses - to hit penalties are applied in processAttackResolutionStep
							final int plusToHit = !xuAttacker.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT) ? 0 :
								xuAttacker.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT);
					
							attackDamage = new AttackDamage (damageCalculationMsg.getPotentialHits (), plusToHit, damageType, damageCalculationMsg.getDamageResolutionTypeID (), null,
								attackSkillID, attackFromMagicRealmID, repetitions);
						}
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
	 * @param eventID The event that caused an attack, if it wasn't initiated by a player
	 * @param db Lookup lists built over the XML database
	 * @param castType Whether spell is being cast in combat or overland
	 * @param skipDamageHeader Whether to skip sending the damage header, if this is part of a bigger spell (used for Call Chaos)
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If there is a problem with the game logic
	 */
	@Override
	public final AttackDamage attackFromSpell (final Spell spell, final Integer variableDamage, final PlayerServerDetails castingPlayer, final ExpandedUnitDetails castingUnit,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final String eventID,
		final CommonDatabase db, final SpellCastType castType, final boolean skipDamageHeader)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException
	{
		// Work out damage done - note this isn't applicable to all types of attack, e.g. Warp Wood has no attack value, so we might get null here
		Integer damage;
		if (variableDamage != null)
			damage = variableDamage;
		else if (castType == SpellCastType.COMBAT)
			damage = spell.getCombatBaseDamage ();
		else
			damage = spell.getOverlandBaseDamage ();
		
		final DamageType damageType = (spell.getAttackSpellDamageTypeID () == null) ? null : db.findDamageType (spell.getAttackSpellDamageTypeID (), "attackFromSpell");

		// For spells that roll against resistance, add on any -spell save from hero items
		// RESISTANCE_ROLLS is intentionally excluded, as for that "damage" a.k.a. "potentialHits" is the number of rolls to me - so can't modify this by the saving throw penalty
		if ((castingUnit != null) && (castingUnit.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_SAVING_THROW_PENALTY)) &&
			((CommonDatabaseConstants.RESISTANCE_BASED_DAMAGE.contains (spell.getAttackSpellDamageResolutionTypeID ())) ||
				(spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_CURSES)))
			
			damage = ((damage == null) ? 0 : damage) + castingUnit.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_SAVING_THROW_PENALTY);
		
		// Send breakdown message
		if (!skipDamageHeader)
		{
			final DamageCalculationAttackData damageCalculationMsg = new DamageCalculationAttackData ();
			if (castingPlayer != null)
				damageCalculationMsg.setAttackerPlayerID (castingPlayer.getPlayerDescription ().getPlayerID ());
			
			damageCalculationMsg.setEventID (eventID);
			damageCalculationMsg.setAttackSpellID (spell.getSpellID ());
			damageCalculationMsg.setPotentialHits (damage);
			damageCalculationMsg.setDamageTypeID (spell.getAttackSpellDamageTypeID ());
			damageCalculationMsg.setDamageResolutionTypeID (spell.getAttackSpellDamageResolutionTypeID ());
			
			if (damageType != null)
				damageCalculationMsg.setStoredDamageTypeID (damageType.getStoredDamageTypeID ());
			
			sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		}

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
	 * @param attacker Unit making the attack if there is one; null if the damage is coming from a spell (even if the spell was cast by a unit)
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @param combatLocation Location where the combat is taking place
	 * @param combatMap Combat scenery
	 * @param trueBuildings True list of buildings
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateSingleFigureDamage (final ExpandedUnitDetails defender, final ExpandedUnitDetails attacker,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final MapCoordinates3DEx combatLocation,
		final MapAreaOfCombatTiles combatMap, final List<MemoryBuilding> trueBuildings, final CommonDatabase db)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException
	{
		final int defenderDefenceStrength = getDamageTypeCalculations ().getDefenderDefenceStrength (defender, attacker, attackDamage, 1,
			combatLocation, combatMap, trueBuildings, db);
		
		final int totalHits = calculateSingleFigureDamageInternal (defender, defenderDefenceStrength, attackingPlayer, defendingPlayer, attackDamage, db);
		return totalHits;
	}
	
	/**
	 * Rolls the number of actual hits and blocks for "armour piercing" type damage, as per single figure
	 * damage except that the defender's defence stat is halved.
	 * 
	 * @param defender Unit being hit
	 * @param attacker Unit making the attack if there is one; null if the damage is coming from a spell (even if the spell was cast by a unit)
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @param combatLocation Location where the combat is taking place
	 * @param combatMap Combat scenery
	 * @param trueBuildings True list of buildings
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateArmourPiercingDamage (final ExpandedUnitDetails defender, final ExpandedUnitDetails attacker,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final MapCoordinates3DEx combatLocation,
		final MapAreaOfCombatTiles combatMap, final List<MemoryBuilding> trueBuildings, final CommonDatabase db)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException
	{
		final int defenderDefenceStrength = getDamageTypeCalculations ().getDefenderDefenceStrength (defender, attacker, attackDamage, 2,
			combatLocation, combatMap, trueBuildings, db);
		
		final int totalHits = calculateSingleFigureDamageInternal (defender, defenderDefenceStrength, attackingPlayer, defendingPlayer, attackDamage, db);
		return totalHits;
	}

	/**
	 * Rolls the number of actual hits for "illusionary" type damage, as per single figure
	 * damage except that the defender gets no defence rolls at all.
	 * 
	 * @param defender Unit being hit
	 * @param attacker Unit making the attack if there is one; null if the damage is coming from a spell (even if the spell was cast by a unit)
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @param combatLocation Location where the combat is taking place
	 * @param combatMap Combat scenery
	 * @param trueBuildings True list of buildings
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateIllusionaryDamage (final ExpandedUnitDetails defender, final ExpandedUnitDetails attacker,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final MapCoordinates3DEx combatLocation,
		final MapAreaOfCombatTiles combatMap, final List<MemoryBuilding> trueBuildings, final CommonDatabase db)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException
	{
		final int defenderDefenceStrength = getDamageTypeCalculations ().getDefenderDefenceStrength (defender, attacker, attackDamage, 0,
			combatLocation, combatMap, trueBuildings, db);
		
		final int totalHits = calculateSingleFigureDamageInternal (defender, defenderDefenceStrength, attackingPlayer, defendingPlayer, attackDamage, db);
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
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	private final int calculateSingleFigureDamageInternal (final ExpandedUnitDetails defender, final int defenderDefenceStrength,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final AttackDamage attackDamage, final CommonDatabase db)
		throws MomException, JAXBException, XMLStreamException
	{
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setDefenderUnitURN (defender.getUnitURN ());
		damageCalculationMsg.setDefenderUnitOwningPlayerID (defender.getOwningPlayerID ());
		damageCalculationMsg.setChanceToHit (attackDamage.getChanceToHit ());
		damageCalculationMsg.setTenTimesAverageDamage (attackDamage.getPotentialHits () * damageCalculationMsg.getChanceToHit ());
		damageCalculationMsg.setDamageResolutionTypeID (attackDamage.getDamageResolutionTypeID ());
		
		// How many actually hit
		int actualDamage = 0;
		for (int swingNo = 0; swingNo < attackDamage.getPotentialHits (); swingNo++)
			if (getRandomUtils ().nextInt (10) < damageCalculationMsg.getChanceToHit ())
				actualDamage++;
		
		damageCalculationMsg.setActualHits (actualDamage);
		
		// If defending unit is blurred then some of those hits may not actually hit, which is done in between making attack rolls and defence rolls
		if ((actualDamage > 0) && (defender.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_BLUR)) &&
			(attackDamage.getAttackFromSkillID () != null) && (attackDamage.getSpell () == null))
		{
			int dodgedHits = 0;
			int remainingHits = 0;
			
			for (int n = 0; n < actualDamage; n++)
				if (getRandomUtils ().nextInt (10) == 0)
					dodgedHits++;
				else
					remainingHits++;
			
			damageCalculationMsg.setBlurredHits (dodgedHits);
			actualDamage = remainingHits;
		}
		
		// Set up defender stats
		damageCalculationMsg.setDefenderFigures (defender.calculateAliveFigureCount ());
		damageCalculationMsg.setUnmodifiedDefenceStrength (!defender.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE) ? 0 :
			Math.max (0, defender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE)));
		damageCalculationMsg.setModifiedDefenceStrength (defenderDefenceStrength);

		// Can't reduce chance below 10%
		damageCalculationMsg.setChanceToDefend (Math.max (1, 3 + (!defender.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK) ? 0 :
			defender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK))));
		damageCalculationMsg.setTenTimesAverageBlock (damageCalculationMsg.getModifiedDefenceStrength () * damageCalculationMsg.getChanceToDefend ());
		
		// Dish out damage
		final int totalHits = getUnitServerUtils ().applySingleFigureDamage (defender, actualDamage, damageCalculationMsg.getModifiedDefenceStrength (),
			damageCalculationMsg.getChanceToDefend (), true, db);
		
		damageCalculationMsg.setFinalHits (totalHits);
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		
		return totalHits;
	}

	/**
	 * Rolls the number of actual hits and blocks for "multi figure" a.k.a. immolation type damage, where all figures are hit individually by the attack,
	 * making their own to hit and defence rolls.
	 * 
	 * @param defender Unit being hit
	 * @param attacker Unit making the attack if there is one; null if the damage is coming from a spell (even if the spell was cast by a unit)
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @param combatLocation Location where the combat is taking place
	 * @param combatMap Combat scenery
	 * @param trueBuildings True list of buildings
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateMultiFigureDamage (final ExpandedUnitDetails defender, final ExpandedUnitDetails attacker,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final MapCoordinates3DEx combatLocation,
		final MapAreaOfCombatTiles combatMap, final List<MemoryBuilding> trueBuildings, final CommonDatabase db)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException
	{
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setDefenderUnitURN (defender.getUnitURN ());
		damageCalculationMsg.setDefenderUnitOwningPlayerID (defender.getOwningPlayerID ());
		damageCalculationMsg.setChanceToHit (attackDamage.getChanceToHit ());
		damageCalculationMsg.setDamageResolutionTypeID (attackDamage.getDamageResolutionTypeID ());

		// Set up defender stats
		damageCalculationMsg.setDefenderFigures (defender.calculateAliveFigureCount ());
		damageCalculationMsg.setUnmodifiedDefenceStrength (!defender.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE) ? 0 :
			Math.max (0, defender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE)));
		damageCalculationMsg.setModifiedDefenceStrength (getDamageTypeCalculations ().getDefenderDefenceStrength (defender, attacker, attackDamage, 1,
			combatLocation, combatMap, trueBuildings, db));
		
		// Can't reduce chance below 10%
		damageCalculationMsg.setChanceToDefend (Math.max (1, 3 + (!defender.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK) ? 0 :
			defender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK))));
		damageCalculationMsg.setTenTimesAverageBlock (damageCalculationMsg.getModifiedDefenceStrength () * damageCalculationMsg.getChanceToDefend ());

		// For multi figure damage, we have to work this out last
		damageCalculationMsg.setTenTimesAverageDamage
			(attackDamage.getPotentialHits () * damageCalculationMsg.getChanceToHit () * damageCalculationMsg.getDefenderFigures ());

		// Dish out damage
		final Holder<Integer> actualDamage = new Holder<Integer> ();
		final int totalHits = getUnitServerUtils ().applyMultiFigureDamage (defender, attackDamage.getPotentialHits (), damageCalculationMsg.getChanceToHit (),
			damageCalculationMsg.getModifiedDefenceStrength (), damageCalculationMsg.getChanceToDefend (), actualDamage, true, db);
		
		// Store and send final totals
		damageCalculationMsg.setActualHits (actualDamage.getValue ());		
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
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateDoomDamage (final ExpandedUnitDetails defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final CommonDatabase db) throws MomException, JAXBException, XMLStreamException
	{
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setDefenderUnitURN (defender.getUnitURN ());
		damageCalculationMsg.setDefenderUnitOwningPlayerID (defender.getOwningPlayerID ());
		damageCalculationMsg.setDamageResolutionTypeID (attackDamage.getDamageResolutionTypeID ());
		
		// No to hit rolls - they automatically hit
		damageCalculationMsg.setActualHits (attackDamage.getPotentialHits ());
		
		// No defence hit rolls - they automatically hit
		damageCalculationMsg.setDefenderFigures (defender.calculateAliveFigureCount ());

		// Dish out damage
		final int totalHits = getUnitServerUtils ().applySingleFigureDamage (defender, attackDamage.getPotentialHits (), 0, 0, false, db);
		
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
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateChanceOfDeathDamage (final ExpandedUnitDetails defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final CommonDatabase db) throws MomException, JAXBException, XMLStreamException
	{
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setDefenderUnitURN (defender.getUnitURN ());
		damageCalculationMsg.setDefenderUnitOwningPlayerID (defender.getOwningPlayerID ());
		damageCalculationMsg.setDamageResolutionTypeID (attackDamage.getDamageResolutionTypeID ());
		
		// Store the dice roll
		damageCalculationMsg.setActualHits (getRandomUtils ().nextInt (100));
		
		// No defence hit rolls - they automatically hit
		damageCalculationMsg.setDefenderFigures (defender.calculateAliveFigureCount ());

		// Unit either takes no damage, or dies outright
		if (damageCalculationMsg.getActualHits () < attackDamage.getPotentialHits ())
		{
			final int totalHits = getUnitServerUtils ().applySingleFigureDamage (defender, Integer.MAX_VALUE, 0, 0, false, db);
			damageCalculationMsg.setFinalHits (totalHits);
		}
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		
		return damageCalculationMsg.getFinalHits ();
	}

	/**
	 * Doesn't actually do any damage - just makes a resistance roll and returns true/false for whether the unit failed the roll or not.
	 * 
	 * @param defender Unit being hit
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @param existingCurse Whether the resistance roll is to shake off an existing curse (false is normal setting, if its to try to avoid being cursed in the first place)
	 * @return Whether the defender failed the resistance roll or not, i.e. true if something bad happens
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final boolean calculateResistanceRoll (final ExpandedUnitDetails defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final boolean existingCurse) throws MomException, JAXBException, XMLStreamException
	{
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setDefenderUnitURN (defender.getUnitURN ());
		damageCalculationMsg.setDefenderUnitOwningPlayerID (defender.getOwningPlayerID ());
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
			final ValidUnitTarget magicRealmLifeformTypeTarget = getSpellUtils ().findMagicRealmLifeformTypeTarget
				(attackDamage.getSpell (), defender.getModifiedUnitMagicRealmLifeformType ().getPickID ());
			if ((magicRealmLifeformTypeTarget != null) && (magicRealmLifeformTypeTarget.getMagicRealmAdditionalSavingThrowModifier () != null) &&
				(magicRealmLifeformTypeTarget.getMagicRealmAdditionalSavingThrowModifier () != 0))
				
				savingThrowModifier = savingThrowModifier + magicRealmLifeformTypeTarget.getMagicRealmAdditionalSavingThrowModifier ();
		}
		
		// Work out the target's effective resistance score, reduced by any saving throw modifier
		damageCalculationMsg.setModifiedDefenceStrength (damageCalculationMsg.getUnmodifiedDefenceStrength () - savingThrowModifier);
		
		// Make resistance roll
		final boolean failed = (getRandomUtils ().nextInt (10) >= damageCalculationMsg.getModifiedDefenceStrength ());
		
		// Store and send final totals
		final int hits = failed ? 1 : 0;
		damageCalculationMsg.setActualHits (hits);		
		damageCalculationMsg.setFinalHits (hits);
		damageCalculationMsg.setExistingCurse (existingCurse);
		
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		
		return failed;
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
		damageCalculationMsg.setDefenderUnitURN (defender.getUnitURN ());
		damageCalculationMsg.setDefenderUnitOwningPlayerID (defender.getOwningPlayerID ());
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
			final ValidUnitTarget magicRealmLifeformTypeTarget = getSpellUtils ().findMagicRealmLifeformTypeTarget
				(attackDamage.getSpell (), defender.getModifiedUnitMagicRealmLifeformType ().getPickID ());
			if ((magicRealmLifeformTypeTarget != null) && (magicRealmLifeformTypeTarget.getMagicRealmAdditionalSavingThrowModifier () != null) &&
				(magicRealmLifeformTypeTarget.getMagicRealmAdditionalSavingThrowModifier () != 0))
				
				savingThrowModifier = savingThrowModifier + magicRealmLifeformTypeTarget.getMagicRealmAdditionalSavingThrowModifier ();
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
	 * Rolls the number of actual hits for "each figure resist or lose 1 HP" damage, where each figure has to make a resistance roll.  Used for wrack.
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
	public final int calculateEachFigureResistOrLose1HPDamage (final ExpandedUnitDetails defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage) throws MomException, JAXBException, XMLStreamException
	{
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setDefenderUnitURN (defender.getUnitURN ());
		damageCalculationMsg.setDefenderUnitOwningPlayerID (defender.getOwningPlayerID ());
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
			final ValidUnitTarget magicRealmLifeformTypeTarget = getSpellUtils ().findMagicRealmLifeformTypeTarget
				(attackDamage.getSpell (), defender.getModifiedUnitMagicRealmLifeformType ().getPickID ());
			if ((magicRealmLifeformTypeTarget != null) && (magicRealmLifeformTypeTarget.getMagicRealmAdditionalSavingThrowModifier () != null) &&
				(magicRealmLifeformTypeTarget.getMagicRealmAdditionalSavingThrowModifier () != 0))
				
				savingThrowModifier = savingThrowModifier + magicRealmLifeformTypeTarget.getMagicRealmAdditionalSavingThrowModifier ();
		}
		
		// Work out the target's effective resistance score, reduced by any saving throw modifier
		damageCalculationMsg.setModifiedDefenceStrength (damageCalculationMsg.getUnmodifiedDefenceStrength () - savingThrowModifier);
		
		// Each figure individually
		int totalHits = 0;
		for (int figureNo = 0; figureNo < damageCalculationMsg.getDefenderFigures (); figureNo++)
			
			// Make resistance roll for this figure
			if (getRandomUtils ().nextInt (10) >= damageCalculationMsg.getModifiedDefenceStrength ())
				totalHits++;
		
		// Store and send final totals
		damageCalculationMsg.setActualHits (totalHits);		
		damageCalculationMsg.setFinalHits (totalHits);
		sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		
		return totalHits;
	}
	
	/**
	 * Rolls the number of actual hits for "single figure resist or die" damage, where only one figure has to make a resistance roll.  Used for stoning touch.
	 * The fact that one such attack is made for each attacking figure is dealt with up in attackFromUnitSkill, because stoning touch has
	 * damagePerFigure set to PER_FIGURE_SEPARATE.  As per example copied from the wiki:
	 * 
	 * A unit of 4 Cockatrices engages a unit of 2 War Bears.  The War Bears must make four resistance rolls, one for each Cockatrice.
	 * Each time a roll fails, the unit is struck for the full health of a single War Bear figure, or 8 Damage.
	 * In point of contrast, when exposed to the Stoning Gaze of 4 Gorgons, the bears will only make two resistance roll - one for each War Bear.
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
		damageCalculationMsg.setDefenderUnitURN (defender.getUnitURN ());
		damageCalculationMsg.setDefenderUnitOwningPlayerID (defender.getOwningPlayerID ());
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
			final ValidUnitTarget magicRealmLifeformTypeTarget = getSpellUtils ().findMagicRealmLifeformTypeTarget
				(attackDamage.getSpell (), defender.getModifiedUnitMagicRealmLifeformType ().getPickID ());
			if ((magicRealmLifeformTypeTarget != null) && (magicRealmLifeformTypeTarget.getMagicRealmAdditionalSavingThrowModifier () != null) &&
				(magicRealmLifeformTypeTarget.getMagicRealmAdditionalSavingThrowModifier () != 0))
				
				savingThrowModifier = savingThrowModifier + magicRealmLifeformTypeTarget.getMagicRealmAdditionalSavingThrowModifier ();
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
	 * Rolls the number of actual hits for "unit resist or die" damage, where the unit as a whole gets a single resistance roll and is killed off if it failed.  Used for death wish.
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
	public final int calculateUnitResistOrDieDamage (final ExpandedUnitDetails defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage) throws MomException, JAXBException, XMLStreamException
	{
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setDefenderUnitURN (defender.getUnitURN ());
		damageCalculationMsg.setDefenderUnitOwningPlayerID (defender.getOwningPlayerID ());
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
			final ValidUnitTarget magicRealmLifeformTypeTarget = getSpellUtils ().findMagicRealmLifeformTypeTarget
				(attackDamage.getSpell (), defender.getModifiedUnitMagicRealmLifeformType ().getPickID ());
			if ((magicRealmLifeformTypeTarget != null) && (magicRealmLifeformTypeTarget.getMagicRealmAdditionalSavingThrowModifier () != null) &&
				(magicRealmLifeformTypeTarget.getMagicRealmAdditionalSavingThrowModifier () != 0))
				
				savingThrowModifier = savingThrowModifier + magicRealmLifeformTypeTarget.getMagicRealmAdditionalSavingThrowModifier ();
		}
		
		// Work out the target's effective resistance score, reduced by any saving throw modifier
		damageCalculationMsg.setModifiedDefenceStrength (damageCalculationMsg.getUnmodifiedDefenceStrength () - savingThrowModifier);
		
		// Make resistance roll
		final int totalHits;
		final int unitDead;
		if (getRandomUtils ().nextInt (10) >= damageCalculationMsg.getModifiedDefenceStrength ())
		{
			unitDead = 1;
			
			// Kill off full HP of unit
			totalHits = defender.calculateHitPointsRemaining ();
		}
		else
		{
			unitDead = 0;
			totalHits = 0;
		}
		
		// Store and send final totals
		damageCalculationMsg.setActualHits (unitDead);		
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
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateResistOrTakeDamage (final ExpandedUnitDetails defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final CommonDatabase db) throws MomException, JAXBException, XMLStreamException
	{
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setDefenderUnitURN (defender.getUnitURN ());
		damageCalculationMsg.setDefenderUnitOwningPlayerID (defender.getOwningPlayerID ());
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
			final ValidUnitTarget magicRealmLifeformTypeTarget = getSpellUtils ().findMagicRealmLifeformTypeTarget
				(attackDamage.getSpell (), defender.getModifiedUnitMagicRealmLifeformType ().getPickID ());
			if ((magicRealmLifeformTypeTarget != null) && (magicRealmLifeformTypeTarget.getMagicRealmAdditionalSavingThrowModifier () != null) &&
				(magicRealmLifeformTypeTarget.getMagicRealmAdditionalSavingThrowModifier () != 0))
				
				savingThrowModifier = savingThrowModifier + magicRealmLifeformTypeTarget.getMagicRealmAdditionalSavingThrowModifier ();
		}
		
		damageCalculationMsg.setModifiedDefenceStrength (damageCalculationMsg.getUnmodifiedDefenceStrength () - savingThrowModifier);
		
		// Make resistance roll
		damageCalculationMsg.setActualHits (getRandomUtils ().nextInt (10) + 1);
		int totalHits = damageCalculationMsg.getActualHits () - damageCalculationMsg.getModifiedDefenceStrength ();
				
		// Can't do negative damage and can't overkill the unit
		if (totalHits < 0)
			totalHits = 0;
		else
			totalHits = getUnitServerUtils ().applySingleFigureDamage (defender, totalHits, 0, 0, false, db);
			
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
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateResistanceRollsDamage (final ExpandedUnitDetails defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final CommonDatabase db) throws MomException, JAXBException, XMLStreamException
	{
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setDefenderUnitURN (defender.getUnitURN ());
		damageCalculationMsg.setDefenderUnitOwningPlayerID (defender.getOwningPlayerID ());
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
		totalHits = getUnitServerUtils ().applySingleFigureDamage (defender, totalHits, 0, 0, false, db);
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
	 * @param db Lookup lists built over the XML database
	 * @return How much damage defender takes as a result of being attacked by attacker
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateDisintegrateDamage (final ExpandedUnitDetails defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final CommonDatabase db) throws MomException, JAXBException, XMLStreamException
	{
		// Store values straight into the message
		final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
		damageCalculationMsg.setDefenderUnitURN (defender.getUnitURN ());
		damageCalculationMsg.setDefenderUnitOwningPlayerID (defender.getOwningPlayerID ());
		damageCalculationMsg.setDamageResolutionTypeID (attackDamage.getDamageResolutionTypeID ());
		
		// Set up defender stats
		damageCalculationMsg.setDefenderFigures (defender.calculateAliveFigureCount ());
		damageCalculationMsg.setUnmodifiedDefenceStrength (defender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE));

		// Is there a saving throw modifier?
		int savingThrowModifier = (attackDamage.getPotentialHits () == null) ? 0 : attackDamage.getPotentialHits ();

		// Is there an additional saving throw modifier because of the magic realm/lifeform type of the target?
		// (Dispel Evil and Holy Word have an additional -5 modifier against Undead)
		if (attackDamage.getSpell () != null)
		{
			final ValidUnitTarget magicRealmLifeformTypeTarget = getSpellUtils ().findMagicRealmLifeformTypeTarget
				(attackDamage.getSpell (), defender.getModifiedUnitMagicRealmLifeformType ().getPickID ());
			if ((magicRealmLifeformTypeTarget != null) && (magicRealmLifeformTypeTarget.getMagicRealmAdditionalSavingThrowModifier () != null) &&
				(magicRealmLifeformTypeTarget.getMagicRealmAdditionalSavingThrowModifier () != 0))
				
				savingThrowModifier = savingThrowModifier + magicRealmLifeformTypeTarget.getMagicRealmAdditionalSavingThrowModifier ();
		}
		
		damageCalculationMsg.setModifiedDefenceStrength (damageCalculationMsg.getUnmodifiedDefenceStrength () - savingThrowModifier);

		// Unit either takes no damage, or dies outright
		if (damageCalculationMsg.getModifiedDefenceStrength () < 10)
		{
			final int totalHits = getUnitServerUtils ().applySingleFigureDamage (defender, Integer.MAX_VALUE, 0, 0, false, db);
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
		damageCalculationMsg.setDefenderUnitURN (xuDefender.getUnitURN ());
		damageCalculationMsg.setDefenderUnitOwningPlayerID (xuDefender.getOwningPlayerID ());
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
	 * Rolls the effect of a Drain Power attack, which causes no actual damage, but drains 2-20 MP from the target.
	 * 
	 * @param defender Unit being hit
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @return Amount of MP to drain
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateDrainPowerDamage (final MemoryUnit defender,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage) throws MomException, JAXBException, XMLStreamException
	{
		int manaDrained = 0;
		if (defender.getManaRemaining () > 0)
		{
			// Store values straight into the message
			final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
			damageCalculationMsg.setDefenderUnitURN (defender.getUnitURN ());
			damageCalculationMsg.setDefenderUnitOwningPlayerID (defender.getOwningPlayerID ());
			damageCalculationMsg.setDamageResolutionTypeID (attackDamage.getDamageResolutionTypeID ());
			
			// Make dice roll
			final int mana = getRandomUtils ().nextInt (19) + 2;
			
			// Can't drain more MP than the unit has
			manaDrained = Math.min (mana, defender.getManaRemaining ());
			
			// Store and send final totals
			damageCalculationMsg.setActualHits (mana);
			damageCalculationMsg.setFinalHits (manaDrained);
			sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		}
		
		return manaDrained;
	}

	/**
	 * Rolls the effect of a Warp Wood attack, which causes no actual damage, but eliminates all remaining ammo on the target.
	 * 
	 * @param defender Unit being hit
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param attackDamage The maximum possible damage the attack may do, and any pluses to hit
	 * @return Amount of MP to drain
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final int calculateZeroesAmmoDamage (final MemoryUnit defender,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage) throws MomException, JAXBException, XMLStreamException
	{
		int ammoDrained = 0;
		if (defender.getAmmoRemaining () > 0)
		{
			// Store values straight into the message
			final DamageCalculationDefenceData damageCalculationMsg = new DamageCalculationDefenceData ();
			damageCalculationMsg.setDefenderUnitURN (defender.getUnitURN ());
			damageCalculationMsg.setDefenderUnitOwningPlayerID (defender.getOwningPlayerID ());
			damageCalculationMsg.setDamageResolutionTypeID (attackDamage.getDamageResolutionTypeID ());
			
			// No dice roll, always eliminates all ammo
			ammoDrained = defender.getAmmoRemaining ();
			
			// Store and send final totals
			damageCalculationMsg.setActualHits (ammoDrained);
			damageCalculationMsg.setFinalHits (ammoDrained);
			sendDamageCalculationMessage (attackingPlayer, defendingPlayer, damageCalculationMsg);
		}
		
		return ammoDrained;
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