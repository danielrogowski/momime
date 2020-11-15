package momime.common.utils;

import java.util.List;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.HeroItem;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.SpellSetting;
import momime.common.database.SpellValidUnitTarget;
import momime.common.messages.PlayerPick;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;

/**
 * Simple spell lookups and calculations
 */
public interface SpellUtils
{
	// Methods dealing with a single spell

	/**
	 * @param spells List of spell research statuses to search
	 * @param spellID Spell ID to search for
	 * @return Requested spell research status
	 * @throws RecordNotFoundException If the research status for this spell can't be found
	 */
	public SpellResearchStatus findSpellResearchStatus (final List<SpellResearchStatus> spells, final String spellID)
		throws RecordNotFoundException;

	/**
	 * @param spell Spell we want to check
	 * @param db Lookup lists built over the XML database
	 * @return The Unit type ID of the unit(s) that this spell summons, or null if it isn't a summoning spell
	 * @throws MomException If the spell can summon units with different unit types
	 * @throws RecordNotFoundException If we encounter a unit or unit magic realm that cannot be found
	 */
	public String spellSummonsUnitTypeID (final Spell spell, final CommonDatabase db)
		throws MomException, RecordNotFoundException;

	/**
	 * Checks if this spell can be cast in overland or combat
	 *
	 * We can't just check e.g. OverlandCastingCost > 0 since if you have a
	 * large number of spell books you can get 100% reduction and reduce a valid
	 * spell to zero casting cost
	 *
	 * @param spell Spell we want to cast
	 * @param castType Type of casting to check is possible for the spell
	 * @return Whether or not the spell can be cast in the specified context
	 * @throws MomException If castType is an unexpected MomSpellCastType
	 */
	public boolean spellCanBeCastIn (final Spell spell, final SpellCastType castType)
		throws MomException;

	/**
	 * @param spell Spell we want to cast
	 * @param picks Books and retorts the player has, so we can check them for any which give casting cost reductions
	 * @param spellSettings Spell combination settings, either from the server XML cache or the Session description
	 * @param db Lookup lists built over the XML database
	 * @return Overland casting cost, modified (reduced) by us having 8 or more spell books, Chaos/Nature/Sorcery Mastery, and so on
	 * @throws MomException If MomSpellCastType.OVERLAND is unexpected by getCastingCostForCastingType (this should never happen)
	 * @throws RecordNotFoundException If there is a pick in the list that we can't find in the DB
	 */
	public int getReducedCombatCastingCost (final Spell spell, final List<PlayerPick> picks, final SpellSetting spellSettings, final CommonDatabase db)
		throws MomException, RecordNotFoundException;

	/**
	 * @param spell Spell we want to cast
	 * @param heroItem If this spell is Enchant Item or Create Artifact then the item being made; for all other spells pass null
	 * @param picks Books and retorts the player has, so we can check them for any which give casting cost reductions
	 * @param spellSettings Spell combination settings, either from the server XML cache or the Session description
	 * @param db Lookup lists built over the XML database
	 * @return Overland casting cost, modified (reduced) by us having 8 or more spell books, Chaos/Nature/Sorcery Mastery, and so on
	 * @throws MomException If MomSpellCastType.OVERLAND is unexpected by getCastingCostForCastingType (this should never happen)
	 * @throws RecordNotFoundException If there is a pick in the list that we can't find in the DB
	 */
	public int getReducedOverlandCastingCost (final Spell spell, final HeroItem heroItem, final List<PlayerPick> picks, final SpellSetting spellSettings, final CommonDatabase db)
		throws MomException, RecordNotFoundException;

	/**
	 * @param spell Spell we want to cast
	 * @param castingCost The casting cost of the spell (base, or possibly increased if a variable mana spell e.g. fire bolt)
	 * @param picks Books and retorts the player has, so we can check them for any which give casting cost reductions
	 * @param spellSettings Spell combination settings, either from the server XML cache or the Session description
	 * @param db Lookup lists built over the XML database
	 * @return Casting cost, modified (reduced) by us having 8 or more spell books, Chaos/Nature/Sorcery Mastery, and so on
	 * @throws MomException If we find an invalid casting reduction type
	 * @throws RecordNotFoundException If there is a pick in the list that we can't find in the DB
	 */
	public int getReducedCastingCost (final Spell spell, final int castingCost, final List<PlayerPick> picks,
		final SpellSetting spellSettings, final CommonDatabase db)
		throws MomException, RecordNotFoundException;
	
	/**
	 * Section this spell should appear in the spell book
	 *
	 * @param spell Spell we want the section for
	 * @param researchStatus Status of us learning this spell
	 * @param considerWhetherResearched If ConsiderWhetherResearched is false, just returns the section ID defined in the database. If ConsiderWhetherResearched is true, will alter the section to move the spell into the 'researchable' or 'unresearchable' sections depending on whether we have/can get the spell
	 * @return sectionID as described above
	 * @throws MomException If getStatus () returns an unexpected status
	 */
	public SpellBookSectionID getModifiedSectionID (final Spell spell, final SpellResearchStatusID researchStatus, final boolean considerWhetherResearched)
		throws MomException;

	/**
	 * For unit enchantment/curse spells, checks whether the requested magic realm/lifeform type ID is a valid target for the spell
	 * Delphi method was named CheckMagicRealmLifeformTypeIsValidTarget, and also allowed building up a list of the valid magicRealmLifeformTypeIDs
	 * But really that was only necessary because of the awkwardness of parsing the XML database in Delphi - there's
	 * no point doing that here because the list of valid targets is already directly accessible as spell.getSpellValidUnitTarget ()
	 *
	 * @param spell Spell we want to cast
	 * @param targetMagicRealmLifeformTypeID the unique string ID of the magic realm/lifeform type to check
	 * @return True if this spell can be cast on this type of target
	 */
	public boolean spellCanTargetMagicRealmLifeformType (final Spell spell, final String targetMagicRealmLifeformTypeID);

	/**
	 * @param spell Spell we want to cast
	 * @param targetMagicRealmLifeformTypeID the unique string ID of the magic realm/lifeform type to check
	 * @return Record for the specific magicRealmLifeformTypeID if there is one; otherwise the null record if there is one; otherwise null
	 */
	public SpellValidUnitTarget findMagicRealmLifeformTypeTarget (final Spell spell, final String targetMagicRealmLifeformTypeID);
	
	// Methods dealing with lists of spells

	/**
	 * @param spells Research status of every spell for this player
	 * @param magicRealmID Filters list to items in this magic realm; arcane spells have null magic realm so null searches for Arcane spells
	 * @param spellRankID Filters list to items in this spell rank
	 * @param status Filters list to items with this research status
	 * @param db Lookup lists built over the XML database
	 * @return List of all the players' spells, filtered by any, all, or none of magic realm, spell rank, and status
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	public List<Spell> getSpellsForRealmRankStatus (final List<SpellResearchStatus> spells, final String magicRealmID,
		final String spellRankID, final SpellResearchStatusID status, final CommonDatabase db)
		throws RecordNotFoundException;

	/**
	 * @param spells Research status of every spell for this player
	 * @param status Filters list to items with this research status
	 * @param db Lookup lists built over the XML database
	 * @return List containing string IDs of all the spells in the specified status
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	public List<Spell> getSpellsForStatus (final List<SpellResearchStatus> spells,
		final SpellResearchStatusID status, final CommonDatabase db)
		throws RecordNotFoundException;

	/**
	 * Note this is a bit weird that we pass in a list of research statuses instead of just a list of spells, but
	 * just left it like this to stay consistent with all the other methods
	 *
	 * @param spells Research status of every spell for this player
	 * @param magicRealmID Filters list to items in this magic realm; arcane spells have null magic realm so null searches for Arcane spells
	 * @param spellRankID Filters list to items in this spell rank
	 * @param db Lookup lists built over the XML database
	 * @return List containing string IDs of all the spells (whether available or not) for a particular magic realm and spell rank
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	public List<Spell> getSpellsForRealmAndRank (final List<SpellResearchStatus> spells, final String magicRealmID,
		final String spellRankID, final CommonDatabase db) throws RecordNotFoundException;

	/**
	 * @param spells Research status of every spell for this player
	 * @param spellRankID Filters list to items in this spell rank
	 * @param status Filters list to items with this MomPlayerSpellStatus
	 * @param db Lookup lists built over the XML database
	 * @return List containing all the spells in the specified spell rank and status
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	public List<Spell> getSpellsForRankAndStatus (final List<SpellResearchStatus> spells,
		final String spellRankID, final SpellResearchStatusID status, final CommonDatabase db) throws RecordNotFoundException;

	/**
	 * @param spells Research status of every spell for this player
	 * @param magicRealmID Filters list to items in this magic realm; arcane spells have null magic realm so null searches for Arcane spells
	 * @param spellRankID Filters list to items in this spell rank
	 * @param db Lookup lists built over the XML database
	 * @return List containing string IDs of all the spells for a particular magic realm and spell rank, but only those not in our spell book
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	public List<Spell> getSpellsNotInBookForRealmAndRank (final List<SpellResearchStatus> spells,
		final String magicRealmID, final String spellRankID, final CommonDatabase db) throws RecordNotFoundException;

	/**
	 * @param spells Research status of every spell for this player
	 * @param status Filters list to items with this MomPlayerSpellStatus
	 * @param db Lookup lists built over the XML database
	 * @return List of Spell Rank IDs for which there are spells in this list in the specified status (e.g. available, researchable)
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	public List<String> getSpellRanksForStatus (final List<SpellResearchStatus> spells,
		final SpellResearchStatusID status, final CommonDatabase db) throws RecordNotFoundException;

	/**
	 * @param spells List of all the spells defined in the XML file
	 * @param magicRealmID Filters list to items in this magic realm; arcane spells have null magic realm so null searches for Arcane spells
	 * @return List of Spell Rank IDs for which there are spells in this list in the specified magic realm
	 */
	public List<String> getSpellRanksForMagicRealm (final List<Spell> spells, final String magicRealmID);

	/**
	 * Depending on the sectionID requested, lists spells either sorted by Remaining Research Cost, or Casting Cost
	 *
	 * This is a little less complicated than listing out the sections in the first place (which is done by TfrmMomClient.ShowSpellBook)
	 * since we know we're going to have been given a valid SectionID
	 *
	 * e.g. we're not going to pass in section = RESEARCH with CastType = msctCombat
	 *
	 * @param spells Research status of every spell for this player
	 * @param desiredSectionID Filters list to items with this section ID
	 * @param castType Filters list to items that allow this MomSpellCastType (this filter does not apply if sectionID requested is SPELL_BOOK_SECTION_RESEARCH_SPELLS or SPELL_BOOK_SECTION_UNKNOWN_SPELLS)
	 * @param db Lookup lists built over the XML database
	 * @return List of string IDs for all the spells in the specified spell book section, sorted by casting cost (or remaining research cost if spell is not yet researched)
	 * @throws MomException If we encounter an unkown research status or castType
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	public List<Spell> getSortedSpellsInSection (final List<SpellResearchStatus> spells, final SpellBookSectionID desiredSectionID,
		final SpellCastType castType, final CommonDatabase db) throws MomException, RecordNotFoundException;
}