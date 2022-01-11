package momime.server.ai;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.SpellResearchStatus;
import momime.common.utils.ExpandedUnitDetails;
import momime.server.MomSessionVariables;
import momime.server.knowledge.CombatDetails;

/**
 * Methods for AI players making decisions about spells
 */
public interface SpellAI
{
	/**
	 * @param player AI player who needs to choose what to research
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 * @throws MomException If there is an error in the logic
	 */
	public void decideWhatToResearch (final PlayerServerDetails player, final CommonDatabase db)
		throws RecordNotFoundException, MomException;

	/**
	 * AI player at the start of the game chooses any spell of the specific magic realm & rank and researches it for free
	 * @param spells Pre-locked list of the player's spell
	 * @param magicRealmID Magic Realm (e.g. chaos) to pick a spell from
	 * @param spellRankID Spell rank (e.g. uncommon) to pick a spell of
	 * @param db Lookup lists built over the XML database
	 * @return Spell AI chose to learn for free
	 * @throws MomException If no eligible spells are available (e.g. player has them all researched already)
	 * @throws RecordNotFoundException If the spell chosen couldn't be found in the player's spell list
	 */
	public SpellResearchStatus chooseFreeSpellAI (final List<SpellResearchStatus> spells, final String magicRealmID, final String spellRankID, final CommonDatabase db)
		throws MomException, RecordNotFoundException;

	/**
	 * If AI player is not currently casting any spells overland, then look through all of them and consider if we should cast any.
	 * 
	 * @param player AI player who needs to choose what to cast
	 * @param wizardDetails AI wizard who needs to choose what to cast
	 * @param constructableUnits List of everything we can construct everywhere or summon
	 * @param wantedUnitTypesOnEachPlane Map of which unit types we need to construct or summon on each plane
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	public void decideWhatToCastOverland (final PlayerServerDetails player, final KnownWizardDetails wizardDetails, final List<AIConstructableUnit> constructableUnits,
		final Map<Integer, List<AIUnitType>> wantedUnitTypesOnEachPlane, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;

	/**
	 * Overland spells are cast first (probably taking several turns) and a target is only chosen after casting is completed.  So after the AI finishes
	 * casting an overland spell that requires a target, this method tries to pick a good target for the spell.  This won't even get called for
	 * types of spell that don't require targets (e.g. overland enchantments or summoning spells), see method castOverlandNow.
	 * 
	 * @param player AI player who needs to choose a spell target
	 * @param spell Definition for the spell to target
	 * @param maintainedSpell Spell being targetted in server's true memory - at the time this is called, this is the only copy of the spell that exists,
	 * 	so its the only thing we need to clean up
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	public void decideOverlandSpellTarget (final PlayerServerDetails player, final Spell spell, final MemoryMaintainedSpell maintainedSpell, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;

	/**
	 * AI player decides whether to cast a spell in combat
	 * 
	 * @param player AI player who needs to choose what to cast
	 * @param wizardDetails AI wizard who needs to choose what to cast
	 * @param combatCastingUnit Unit who is casting the spell; null means its the wizard casting, rather than a specific unit
	 * @param combatDetails Details about the combat taking place
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether a spell was cast or not
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	public CombatAIMovementResult decideWhatToCastCombat (final PlayerServerDetails player, final KnownWizardDetails wizardDetails,
		final ExpandedUnitDetails combatCastingUnit, final CombatDetails combatDetails, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;

	/**
	 * AI player decides whether to use a fixed spell a unit can cast in combat, e.g. Giant Spiders' web
	 * 
	 * @param player AI player who needs to choose what to cast
	 * @param combatCastingUnit Unit who is casting the spell
	 * @param combatDetails Details about the combat taking place
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether a spell was cast or not
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	public CombatAIMovementResult decideWhetherToCastFixedSpellInCombat (final PlayerServerDetails player, final ExpandedUnitDetails combatCastingUnit,
		final CombatDetails combatDetails, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;

	/**
	 * AI player decides whether to use the spell imbued in a hero item
	 * 
	 * @param player AI player who needs to choose what to cast
	 * @param combatCastingUnit Unit who is casting the spell
	 * @param combatDetails Details about the combat taking place
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether a spell was cast or not
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	public CombatAIMovementResult decideWhetherToCastSpellImbuedInHeroItem (final PlayerServerDetails player, final ExpandedUnitDetails combatCastingUnit,
		final CombatDetails combatDetails, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;
}