package momime.server.calculations;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.DamageCalculationData;
import momime.common.utils.ExpandedUnitDetails;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.SpellSvr;
import momime.server.process.AttackResolutionUnit;

/**
 * Routines for making all the different kinds of damage rolls to see how many HP a unit attacking another unit knocks off.
 * This doesn't deal with actually applying the damage to the units, so that damage calculations for all combat actions in
 * a single step can all be called in succession, added up and applied in one go.
 * 
 * e.g. attacker's melee and poison attacks, and defender's counterattack would be 3 calls to damage calc routines
 * but the damage is all applied at once in DamageProcessorImpl.
 */
public interface DamageCalculator
{
	/**
	 * Just deals with making sure we only send to human players
	 * 
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param msg Message to send
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	public void sendDamageCalculationMessage (final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final DamageCalculationData msg)
		throws JAXBException, XMLStreamException;
	
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
	public AttackDamage attackFromUnitSkill (final AttackResolutionUnit attacker, final AttackResolutionUnit defender,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final String attackSkillID, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException;
	
	/**
	 * Calculates the strength of an attack coming from a spell.  Note the same output from here is used to attack *all* the defenders,
	 * so should not do anything here regarding any immunities each individual defender may have.
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
	public AttackDamage attackFromSpell (final SpellSvr spell, final Integer variableDamage,
		final PlayerServerDetails castingPlayer, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final ServerDatabaseEx db)
		throws JAXBException, XMLStreamException, RecordNotFoundException;
	
	/**
	 * Rolls the number of actual hits and blocks for normal "single figure" type damage, where the first figure defends then takes hits, then the
	 * second figure defends and takes hits, so each figure defends and is then (maybe) killed off individually, until all damage is
	 * absorbed or every figure is killed.
	 * 
	 * @param defender Unit being hit
	 * @param attacker Unit making the attack - this is only used for immunity purposes, e.g. do they have a skill that can punch through our weapon immunity?  So its fine to pass null here
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
	public int calculateSingleFigureDamage (final ExpandedUnitDetails defender, final MemoryUnit attacker, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException;

	/**
	 * Rolls the number of actual hits and blocks for "armour piercing" type damage, as per single figure
	 * damage except that the defender's defence stat is halved.
	 * 
	 * @param defender Unit being hit
	 * @param attacker Unit making the attack - this is only used for immunity purposes, e.g. do they have a skill that can punch through our weapon immunity?  So its fine to pass null here
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
	public int calculateArmourPiercingDamage (final ExpandedUnitDetails defender, final MemoryUnit attacker, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException;

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
	public int calculateIllusionaryDamage (final ExpandedUnitDetails defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage) throws MomException, JAXBException, XMLStreamException;

	/**
	 * Rolls the number of actual hits and blocks for "multi figure" a.k.a. immolation type damage, where all figures are hit individually by the attack,
	 * making their own to hit and defence rolls.
	 * 
	 * @param defender Unit being hit
	 * @param attacker Unit making the attack - this is only used for immunity purposes, e.g. do they have a skill that can punch through our weapon immunity?  So its fine to pass null here
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
	public int calculateMultiFigureDamage (final ExpandedUnitDetails defender, final MemoryUnit attacker, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException;
	
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
	public int calculateDoomDamage (final ExpandedUnitDetails defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage) throws MomException, JAXBException, XMLStreamException;

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
	public int calculateChanceOfDeathDamage (final ExpandedUnitDetails defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage) throws MomException, JAXBException, XMLStreamException;

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
	public int calculateSingleFigureResistOrDieDamage (final ExpandedUnitDetails defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage) throws MomException, JAXBException, XMLStreamException;

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
	public int calculateEachFigureResistOrDieDamage (final ExpandedUnitDetails defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage) throws MomException, JAXBException, XMLStreamException;

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
	public int calculateResistOrTakeDamage (final ExpandedUnitDetails defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage) throws MomException, JAXBException, XMLStreamException;
	
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
	public int calculateResistanceRollsDamage (final ExpandedUnitDetails defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage) throws MomException, JAXBException, XMLStreamException;
	
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
	public int calculateDisintegrateDamage (final ExpandedUnitDetails defender, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage) throws MomException, JAXBException, XMLStreamException;

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
	public void calculateFearDamage (final AttackResolutionUnit defender, final ExpandedUnitDetails xuDefender,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final AttackDamage attackDamage) throws MomException, JAXBException, XMLStreamException;
}