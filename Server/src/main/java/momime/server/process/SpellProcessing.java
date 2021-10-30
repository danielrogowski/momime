package momime.server.process;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.HeroItem;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.utils.ExpandedUnitDetails;
import momime.server.MomSessionVariables;

/**
 * Methods for processing the effects of spells that have completed casting
 */
public interface SpellProcessing
{
	/**
	 * Handles casting an overland spell, i.e. when we've finished channeling sufficient mana in to actually complete the casting
	 *
	 * @param player Player who is casting the spell
	 * @param spell Which spell is being cast
	 * @param variableDamage Chosen damage selected for the spell, for spells like fire bolt where a varying amount of mana can be channeled into the spell
	 * @param heroItem The item being created; null for spells other than Enchant Item or Create Artifact
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void castOverlandNow (final PlayerServerDetails player, final Spell spell, final Integer variableDamage, final HeroItem heroItem, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException;
	
	/**
	 * Handles casting a spell in combat, after all validation has passed.
	 * If its a spell where we need to choose a target (like Doom Bolt or Phantom Warriors), additional mana (like Counter Magic)
	 * or both (like Firebolt), then the client will already have done all this and supplied us with the chosen values.
	 * 
	 * @param castingPlayer Player who is casting the spell
	 * @param xuCombatCastingUnit Unit who is casting the spell; null means its the wizard casting, rather than a specific unit
	 * @param combatCastingFixedSpellNumber For casting fixed spells the unit knows (e.g. Giant Spiders casting web), indicates the spell number; for other types of casting this is null
	 * @param combatCastingSlotNumber For casting spells imbued into hero items, this is the number of the slot (0, 1 or 2); for other types of casting this is null
	 * @param spell Which spell they want to cast
	 * @param reducedCombatCastingCost Skill cost of the spell, reduced by any book or retort bonuses the player may have
	 * @param multipliedManaCost MP cost of the spell, reduced as above, then multiplied up according to the distance the combat is from the wizard's fortress
	 * @param variableDamage Chosen damage selected for the spell, for spells like fire bolt where a varying amount of mana can be channeled into the spell
	 * @param combatLocation Location of the combat where this spell is being cast; null = being cast overland
	 * @param defendingPlayer Defending player in the combat
	 * @param attackingPlayer Attacking player in the combat
	 * @param targetUnit Unit to target the spell on, if appropriate for spell book section, otherwise null
	 * @param targetLocation Location to target the spell at, if appropriate for spell book section, otherwise null
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether the spell cast was an attack that resulted in the combat ending
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public boolean castCombatNow (final PlayerServerDetails castingPlayer, final ExpandedUnitDetails xuCombatCastingUnit, final Integer combatCastingFixedSpellNumber,
		final Integer combatCastingSlotNumber, final Spell spell, final int reducedCombatCastingCost, final int multipliedManaCost,
		final Integer variableDamage, final MapCoordinates3DEx combatLocation, final PlayerServerDetails defendingPlayer, final PlayerServerDetails attackingPlayer,
		final MemoryUnit targetUnit, final MapCoordinates2DEx targetLocation, final MomSessionVariables mom)
		throws MomException, JAXBException, XMLStreamException, PlayerNotFoundException, RecordNotFoundException;
	
	/**
	 * The method in the FOW class physically removed spells from the server and players' memory; this method
	 * deals with all the knock on effects of spells being switched off, which isn't really much since spells don't grant money or anything when sold
	 * so this is mostly here for consistency with the building and unit methods
	 *
	 * Does not recalc global production (which will now be reduced from not having to pay the maintenance of the cancelled spell),
	 * this has to be done by the calling routine
	 * 
	 * NB. Delphi method was called OkToSwitchOffMaintainedSpell
	 *
	 * @param spellURN Which spell it is
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether switching off the spell resulted in the death of the unit it was cast on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public boolean switchOffSpell (final int spellURN, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException;
	
	/**
	 * Overland spells are cast first (probably taking several turns) and a target is only chosen after casting is completed.
	 * So this actually processes the actions from the spell once its target is chosen.
	 * This assumes all necessary validation has been done to verify that the action is allowed.
	 * 
	 * @param spell Definition of spell being targeted
	 * @param maintainedSpell Spell being targeted in server's true memory - at the time this is called, this is the only copy of the spell that exists
	 * 	as we can only determine which clients can "see" it once a target location has been chosen.  Even the player who cast it doesn't have a
	 *		record of it, just a special entry on their new turn messages scroll telling them to pick a target for it.
	 * @param targetPlayerID If the spell is targeted at a wizard, then which one
	 * @param targetLocation If the spell is targeted at a city or a map location, then sets that location; null for spells targeted on other things
	 * @param targetUnit If the spell is targeted at a unit, then the true unit to aim at; null for spells targeted on other things
	 * @param targetSpell If the spell is targeted at another spell, then the true spell to aim at; null for spells targeted on other things
	 * @param citySpellEffectID If spell creates a city spell effect, then which one - currently chosen at random, but supposed to be player choosable for Spell Ward
	 * @param unitSkillID If spell creates a unit skill, then which one - chosen at random for Chaos Channels
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void targetOverlandSpell (final Spell spell, final MemoryMaintainedSpell maintainedSpell, final Integer targetPlayerID,
		final MapCoordinates3DEx targetLocation, final MemoryUnit targetUnit, final MemoryMaintainedSpell targetSpell,
		final String citySpellEffectID, final String unitSkillID, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException;

	/**
	 * Overland spells are cast first (probably taking several turns) and a target is only chosen after casting is completed.
	 * But perhaps by the time we finish casting it, we no longer have a valid target or changed our minds, so this just cancels and loses the spell.
	 * 
	 * @param maintainedSpell Spell being targeted in server's true memory - at the time this is called, this is the only copy of the spell that exists,
	 * 	so its the only thing we need to clean up
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void cancelTargetOverlandSpell (final MemoryMaintainedSpell maintainedSpell, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException;

	/**
	 * When a wizard is banished or defeated, can steal up to 2 spells from them as long as we have enough books to allow it.
	 * 
	 * @param stealFrom Wizard who was banished, who spells are being stolen from
	 * @param giveTo Wizard who banished them, who spells are being given to
	 * @param spellsStolenFromFortress Maximum number of spells to steal
	 * @param db Lookup lists built over the XML database
	 * @return List of spells that were stolen
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	public List<String> stealSpells (final PlayerServerDetails stealFrom, final PlayerServerDetails giveTo, final int spellsStolenFromFortress, final CommonDatabase db)
		throws JAXBException, XMLStreamException, RecordNotFoundException;

	/**
	 * For Gaia's blessing.  Each turn it has a chance of turning deserts into grasslands, or volcanoes into hills.
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param onlyOnePlayerID If zero, will process all spells; if specified will process only spells cast by the specified player
	 * @throws RecordNotFoundException If we encounter a spell with an unknown city spell effect
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 */
	public void rollSpellTerrainEffectsEachTurn (final MomSessionVariables mom, final int onlyOnePlayerID)
		throws RecordNotFoundException, JAXBException, XMLStreamException;
	
	/**
	 * For Stasis.  Each turn, units with stasis get a resistance roll for a chance to free themselves.
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param onlyOnePlayerID If zero, will process units belonging to everyone; if specified will process only units owned by the specified player
	 * @throws RecordNotFoundException If we encounter an unknown spell
	 * @throws PlayerNotFoundException If we can't find one of the players
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 */
	public void rollToRemoveOverlandCurses (final MomSessionVariables mom, final int onlyOnePlayerID)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException;

	/**
	 * For Chaos Rift.  Each turn, units in the city get struck by lightning bolts.
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param onlyOnePlayerID If zero, will process CSEs belonging to everyone; if specified will process only CAEs owned by the specified player
	 * @throws RecordNotFoundException If we encounter an unknown spell
	 * @throws PlayerNotFoundException If we can't find one of the players
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 */
	public void citySpellEffectsAttackingUnits (final MomSessionVariables mom, final int onlyOnePlayerID)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException;

	/**
	 * For Chaos Rift.  Each turn, there is a chance of each building in the city being destroyed.
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param onlyOnePlayerID If zero, will process CSEs belonging to everyone; if specified will process only CAEs owned by the specified player
	 * @throws RecordNotFoundException If we encounter an unknown spell
	 * @throws PlayerNotFoundException If we can't find one of the players
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 */
	public void citySpellEffectsAttackingBuildings (final MomSessionVariables mom, final int onlyOnePlayerID)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException;
}