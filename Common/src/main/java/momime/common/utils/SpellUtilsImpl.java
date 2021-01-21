package momime.common.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import momime.common.MomException;
import momime.common.calculations.HeroItemCalculations;
import momime.common.calculations.SpellCalculations;
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
public final class SpellUtilsImpl implements SpellUtils
{
	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/** Spell calculations */
	private SpellCalculations spellCalculations;
	
	/** Hero item calculations */
	private HeroItemCalculations heroItemCalculations;
	
	// Methods dealing with a single spell

	/**
	 * @param spells List of spell research statuses to search
	 * @param spellID Spell ID to search for
	 * @return Requested spell research status
	 * @throws RecordNotFoundException If the research status for this spell can't be found
	 */
	@Override
	public final SpellResearchStatus findSpellResearchStatus (final List<SpellResearchStatus> spells, final String spellID)
		throws RecordNotFoundException
	{
		SpellResearchStatus chosenSpellStatus = null;
		final Iterator<SpellResearchStatus> iter = spells.iterator ();
		while ((chosenSpellStatus == null) && (iter.hasNext ()))
		{
			final SpellResearchStatus thisSpell = iter.next ();
			if (thisSpell.getSpellID ().equals (spellID))
				chosenSpellStatus = thisSpell;
		}

		if (chosenSpellStatus == null)
			throw new RecordNotFoundException ("SpellResearchStatus", spellID, "findSpellResearchStatus");

		return chosenSpellStatus;
	}

	/**
	 * @param spell Spell we want to check
	 * @param db Lookup lists built over the XML database
	 * @return The Unit type ID of the unit(s) that this spell summons, or null if it isn't a summoning spell
	 * @throws MomException If the spell can summon units with different unit types
	 * @throws RecordNotFoundException If we encounter a unit or unit magic realm that cannot be found
	 */
	@Override
	public final String spellSummonsUnitTypeID (final Spell spell, final CommonDatabase db)
		throws MomException, RecordNotFoundException
	{
		// for War Bears, Sky Drakes, etc. we want "S", and for Summon Hero, Champion & Incarnation we want "H"
		String result = null;
		if (spell.getSpellBookSectionID () == SpellBookSectionID.SUMMONING)
			for (final String spellSummonsUnitID : spell.getSummonedUnit ())
			{
				// Now find the unit's magic realm / lifeform type
				final String thisMagicRealmID = db.findUnit (spellSummonsUnitID, "spellSummonsUnitTypeID").getUnitMagicRealm ();

				// Use the cache for the magic realm / lifeform type to get the Unit Type ID
				final String unitTypeID = db.findPick (thisMagicRealmID, "spellSummonsUnitTypeID").getUnitTypeID ();

				// We need to only end up with a single Unit Type ID, so it must either be the first one encountered or the same as what we already have
				if (result == null)
					result = unitTypeID;
				else if (!result.equals (unitTypeID))
					throw new MomException ("Summoning spell " + spell.getSpellID () + " can summon units of more than one Unit Type");
			}

		return result;
	}

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
	 * @throws MomException If castType is an unexpected SpellCastType
	 */
	@Override
	public final boolean spellCanBeCastIn (final Spell spell, final SpellCastType castType)
		throws MomException
	{
		boolean result;
		switch (castType)
		{
			// Note Special case to allow Enchant Item and Create Artifact to be cast, which have no overland cost
			case OVERLAND:
				result = ((spell.getOverlandCastingCost () != null) && (spell.getOverlandCastingCost () > 0)) ||
					(spell.getHeroItemBonusMaximumCraftingCost () != null);
				break;
				
			case COMBAT:
				result = (spell.getCombatCastingCost () != null) && (spell.getCombatCastingCost () > 0);
				break;
				
			default:
				throw new MomException ("spellCanBeCastIn: Invalid CastType");
		}

		return result;
	}

	/**
	 * @param spell Spell we want to cast
	 * @param picks Books and retorts the player has, so we can check them for any which give casting cost reductions
	 * @param spellSettings Spell combination settings, either from the server XML cache or the Session description
	 * @param db Lookup lists built over the XML database
	 * @return Overland casting cost, modified (reduced) by us having 8 or more spell books, Chaos/Nature/Sorcery Mastery, and so on
	 * @throws MomException If SpellCastType.OVERLAND is unexpected by getCastingCostForCastingType (this should never happen)
	 * @throws RecordNotFoundException If there is a pick in the list that we can't find in the DB
	 */
	@Override
	public final int getReducedCombatCastingCost (final Spell spell, final List<PlayerPick> picks, final SpellSetting spellSettings, final CommonDatabase db)
		throws MomException, RecordNotFoundException
	{
		return getReducedCastingCost (spell, spell.getCombatCastingCost (), picks, spellSettings, db);
	}

	/**
	 * @param spell Spell we want to cast
	 * @param heroItem If this spell is Enchant Item or Create Artifact then the item being made; for all other spells pass null
	 * @param picks Books and retorts the player has, so we can check them for any which give casting cost reductions
	 * @param spellSettings Spell combination settings, either from the server XML cache or the Session description
	 * @param db Lookup lists built over the XML database
	 * @return Overland casting cost, modified (reduced) by us having 8 or more spell books, Chaos/Nature/Sorcery Mastery, and so on
	 * @throws MomException If SpellCastType.OVERLAND is unexpected by getCastingCostForCastingType (this should never happen)
	 * @throws RecordNotFoundException If there is a pick in the list that we can't find in the DB
	 */
	@Override
	public final int getReducedOverlandCastingCost (final Spell spell, final HeroItem heroItem, final List<PlayerPick> picks, final SpellSetting spellSettings, final CommonDatabase db)
		throws MomException, RecordNotFoundException
	{
		final int castingCost;
		if (heroItem != null)
			castingCost = getHeroItemCalculations ().calculateCraftingCost (heroItem, db);
		else
			castingCost = spell.getOverlandCastingCost ();
		
		return getReducedCastingCost (spell, castingCost, picks, spellSettings, db);
	}

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
	@Override
	public final int getReducedCastingCost (final Spell spell, final int castingCost, final List<PlayerPick> picks,
		final SpellSetting spellSettings, final CommonDatabase db)
		throws MomException, RecordNotFoundException
	{
		// What magic realm ID is this spell?
		final String magicRealmID = spell.getSpellRealm ();

		// How many spell books do we have in this magic realm? Realm is null for Arcane spells
		int bookCount;
		if (magicRealmID == null)
			bookCount = 0;
		else
			bookCount = getPlayerPickUtils ().getQuantityOfPick (picks, magicRealmID);

		// Do the calculation Calculation function returns a percentage reduction
		final double reduction = getSpellCalculations ().calculateCastingCostReduction (bookCount, spellSettings, spell, picks, db) / 100;

		final int reductionAmount = (int) (reduction * castingCost);
		final int castingCostForCastingType = castingCost - reductionAmount;

		return castingCostForCastingType;
	}

	/**
	 * Section this spell should appear in the spell book
	 *
	 * @param spell Spell we want the section for
	 * @param researchStatus Status of us learning this spell
	 * @param considerWhetherResearched If ConsiderWhetherResearched is false, just returns the section ID defined in the database. If ConsiderWhetherResearched is true, will alter the section to move the spell into the 'researchable' or 'unresearchable' sections depending on whether we have/can get the spell
	 * @return sectionID as described above
	 * @throws MomException If getStatus () returns an unexpected status
	 */
	@Override
	public final SpellBookSectionID getModifiedSectionID (final Spell spell, final SpellResearchStatusID researchStatus, final boolean considerWhetherResearched)
		throws MomException
	{
		final SpellBookSectionID result;
		if (!considerWhetherResearched)
		{
			// Ignore whether researched or not, just return the value from the DB
			result = spell.getSpellBookSectionID ();
		}
		else
			switch (researchStatus)
			{
				case UNAVAILABLE:
				case NOT_IN_SPELL_BOOK:
					result = null;
					break;
				case RESEARCHABLE:
					result = SpellBookSectionID.RESEARCHABLE;
					break;
				case RESEARCHABLE_NOW:
					result = SpellBookSectionID.RESEARCHABLE_NOW;
					break;
				case AVAILABLE:
					result = spell.getSpellBookSectionID ();
					break;
				default:
					throw new MomException ("getSectionID: Unknown spell status " + researchStatus);
			}

		return result;
	}

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
	@Override
	public final boolean spellCanTargetMagicRealmLifeformType (final Spell spell, final String targetMagicRealmLifeformTypeID)
	{
		boolean targetIsValidForThisSpell = false;
		boolean anyTargetEntryFound = false;

		// Have to do them all in order to build up List, even if we find a matching one early on
		for (final SpellValidUnitTarget spellValidUnitTarget : spell.getSpellValidUnitTarget ())
		{
			final String magicRealmLifeformTypeID = spellValidUnitTarget.getTargetMagicRealmID ();
			if (magicRealmLifeformTypeID != null)
			{
				anyTargetEntryFound = true;

				if (targetMagicRealmLifeformTypeID.equals (magicRealmLifeformTypeID))
					targetIsValidForThisSpell = true;
			}
		}

		// If there are no targets defined, this means the spell can be used on any type of target
		if (!anyTargetEntryFound)
			targetIsValidForThisSpell = true;

		return targetIsValidForThisSpell;
	}
	
	/**
	 * @param spell Spell we want to cast
	 * @param targetMagicRealmLifeformTypeID the unique string ID of the magic realm/lifeform type to check
	 * @return Record for the specific magicRealmLifeformTypeID if there is one; otherwise the null record if there is one; otherwise null
	 */
	@Override
	public final SpellValidUnitTarget findMagicRealmLifeformTypeTarget (final Spell spell, final String targetMagicRealmLifeformTypeID)
	{
		SpellValidUnitTarget found = null;
		final Iterator<SpellValidUnitTarget> iter = spell.getSpellValidUnitTarget ().iterator ();
		
		while (((found == null) || (found.getTargetMagicRealmID () == null)) && (iter.hasNext ()))
		{
			final SpellValidUnitTarget spellValidUnitTarget = iter.next ();
			if ((targetMagicRealmLifeformTypeID.equals (spellValidUnitTarget.getTargetMagicRealmID ())) || (spellValidUnitTarget.getTargetMagicRealmID () == null))
				found = spellValidUnitTarget;
		}

		return found;
	}

	// Methods dealing with lists of spells

	/**
	 * Internal general purpose search routine that all the other routines call
	 *
	 * @param spells Research status of every spell for this player
	 * @param desiredMagicRealmID If non-null, filters list to items in this magic realm; empty string searches for Arcane spells
	 * @param spellRankID If non-null, filters list to items in this spell rank
	 * @param statuses If non-null, filters list to items with any of the MomPlayerSpellStatuses in the list
	 * @param db Lookup lists built over the XML database
	 * @return List of all the players' spells, filtered by any, all, or none of magic realm, spell rank, and status
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	private final List<Spell> getSpellsForRealmRankStatusInternal (final List<SpellResearchStatus> spells,
		final String desiredMagicRealmID, final String spellRankID, final List<SpellResearchStatusID> statuses, final CommonDatabase db)
		throws RecordNotFoundException
	{
		final List<Spell> resultList = new ArrayList<Spell> ();
		
		for (final SpellResearchStatus thisSpellResearchStatus : spells)
		{
			final Spell thisSpell = db.findSpell (thisSpellResearchStatus.getSpellID (), "getSpellsForRealmRankStatusInternal");

			// Careful with magic realm ID, the 3 conditions are either: None specified (so select everything) OR we specified Arcane (empty string) so match only null OR we specified a real value so match only it
			// i.e. the way we specify Arcane as an input param (empty string) is different to how its returned from the cache
			if (
					(
					  /* if we don't care about magic realm */	 (desiredMagicRealmID == null)
					  /* or we want arcane and spell lacks a realm */ || ((desiredMagicRealmID.equals ("")) && (thisSpell.getSpellRealm () == null))
					  /* or we want a specific non-arcane realm and spell matches */ || (thisSpell.getSpellRealm () != null && thisSpell.getSpellRealm ().equals (desiredMagicRealmID))
					)
				  && ((spellRankID == null) || (thisSpell.getSpellRank ().equals (spellRankID)))
				  && ((statuses == null) || (statuses.contains (thisSpellResearchStatus.getStatus ())))
			   )

				resultList.add (thisSpell);
		}

		return resultList;
	}

	/**
	 * @param spells Research status of every spell for this player
	 * @param magicRealmID Filters list to items in this magic realm; arcane spells have null magic realm so null searches for Arcane spells
	 * @param spellRankID Filters list to items in this spell rank
	 * @param status Filters list to items with this research status
	 * @param db Lookup lists built over the XML database
	 * @return List of all the players' spells, filtered by any, all, or none of magic realm, spell rank, and status
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Override
	public final List<Spell> getSpellsForRealmRankStatus (final List<SpellResearchStatus> spells, final String magicRealmID,
		final String spellRankID, final SpellResearchStatusID status, final CommonDatabase db)
		throws RecordNotFoundException
	{
		// Convert null to empty string if searching for arcane spells
		final String useMagicRealmID = (magicRealmID == null) ? "" : magicRealmID;

		// Put status into a single item list
		final List<SpellResearchStatusID> tempStatusList = new ArrayList<SpellResearchStatusID> ();
		tempStatusList.add (status);

		final List<Spell> result = getSpellsForRealmRankStatusInternal (spells, useMagicRealmID, spellRankID, tempStatusList, db);
		return result;
	}

	/**
	 * @param spells Research status of every spell for this player
	 * @param status Filters list to items with this research status
	 * @param db Lookup lists built over the XML database
	 * @return List containing string IDs of all the spells in the specified status
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Override
	public final List<Spell> getSpellsForStatus (final List<SpellResearchStatus> spells,
		final SpellResearchStatusID status, final CommonDatabase db)
		throws RecordNotFoundException
	{
		// Put status into a single item list
		final List<SpellResearchStatusID> tempStatusList = new ArrayList<SpellResearchStatusID> ();
		tempStatusList.add (status);

		final List<Spell> result = getSpellsForRealmRankStatusInternal (spells, null, null, tempStatusList, db);
		return result;
	}

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
	@Override
	public final List<Spell> getSpellsForRealmAndRank (final List<SpellResearchStatus> spells, final String magicRealmID,
		final String spellRankID, final CommonDatabase db) throws RecordNotFoundException
	{
		// Convert null to empty string if searching for arcane spells
		final String useMagicRealmID = (magicRealmID == null) ? "" : magicRealmID;

		final List<Spell> result = getSpellsForRealmRankStatusInternal (spells, useMagicRealmID, spellRankID, null, db);
		return result;
	}

	/**
	 * @param spells Research status of every spell for this player
	 * @param spellRankID Filters list to items in this spell rank
	 * @param status Filters list to items with this MomPlayerSpellStatus
	 * @param db Lookup lists built over the XML database
	 * @return List containing all the spells in the specified spell rank and status
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Override
	public final List<Spell> getSpellsForRankAndStatus (final List<SpellResearchStatus> spells,
		final String spellRankID, final SpellResearchStatusID status, final CommonDatabase db) throws RecordNotFoundException
	{
		// Put status into a single item list
		final List<SpellResearchStatusID> tempStatusList = new ArrayList<SpellResearchStatusID> ();
		tempStatusList.add (status);

		final List<Spell> result = getSpellsForRealmRankStatusInternal (spells, null, spellRankID, tempStatusList, db);
		return result;
	}

	/**
	 * @param spells Research status of every spell for this player
	 * @param magicRealmID Filters list to items in this magic realm; arcane spells have null magic realm so null searches for Arcane spells
	 * @param spellRankID Filters list to items in this spell rank
	 * @param db Lookup lists built over the XML database
	 * @return List containing string IDs of all the spells for a particular magic realm and spell rank, but only those not in our spell book
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Override
	public final List<Spell> getSpellsNotInBookForRealmAndRank (final List<SpellResearchStatus> spells,
		final String magicRealmID, final String spellRankID, final CommonDatabase db) throws RecordNotFoundException
	{
		// Convert null to empty string if searching for arcane spells
		final String useMagicRealmID = (magicRealmID == null) ? "" : magicRealmID;

		// Build list of statuses that we're looking for
		final List<SpellResearchStatusID> tempStatusList = new ArrayList<SpellResearchStatusID> ();

		tempStatusList.add (SpellResearchStatusID.UNAVAILABLE);
		tempStatusList.add (SpellResearchStatusID.NOT_IN_SPELL_BOOK);

		final List<Spell> result = getSpellsForRealmRankStatusInternal (spells, useMagicRealmID, spellRankID, tempStatusList, db);
		return result;
	}

	/**
	 * @param spells Research status of every spell for this player
	 * @param status Filters list to items with this MomPlayerSpellStatus
	 * @param db Lookup lists built over the XML database
	 * @return List of Spell Rank IDs for which there are spells in this list in the specified status (e.g. available, researchable)
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Override
	public final List<String> getSpellRanksForStatus (final List<SpellResearchStatus> spells,
		final SpellResearchStatusID status, final CommonDatabase db) throws RecordNotFoundException
	{
		final List<String> spellRanksFound = new ArrayList<String> ();

		for (final SpellResearchStatus thisSpellResearchStatus : spells)
		{
			final Spell thisSpell = db.findSpell (thisSpellResearchStatus.getSpellID (), "getSpellRanksForStatus");

			if ((thisSpellResearchStatus.getStatus () == status) && (!spellRanksFound.contains (thisSpell.getSpellRank ())))
				spellRanksFound.add (thisSpell.getSpellRank ());
		}

		return spellRanksFound;
	}

	/**
	 * @param spells List of all the spells defined in the XML file
	 * @param magicRealmID Filters list to items in this magic realm; arcane spells have null magic realm so null searches for Arcane spells
	 * @return List of Spell Rank IDs for which there are spells in this list in the specified magic realm
	 */
	@Override
	public final List<String> getSpellRanksForMagicRealm (final List<Spell> spells, final String magicRealmID)
	{
		final List<String> spellRanksFound = new ArrayList<String> ();

		for (final Spell thisSpell : spells)

			if (((magicRealmID == null) && (thisSpell.getSpellRealm () == null)) ||
				((magicRealmID != null) && (magicRealmID.equals (thisSpell.getSpellRealm ()))))

				if (!spellRanksFound.contains (thisSpell.getSpellRank ()))
					spellRanksFound.add (thisSpell.getSpellRank ());

		return spellRanksFound;
	}

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
	 * @param castType Filters list to items that allow this SpellCastType (this filter does not apply if sectionID requested is SPELL_BOOK_SECTION_RESEARCH_SPELLS or SPELL_BOOK_SECTION_UNKNOWN_SPELLS)
	 * @param db Lookup lists built over the XML database
	 * @return List of string IDs for all the spells in the specified spell book section, sorted by casting cost (or remaining research cost if spell is not yet researched)
	 * @throws MomException If we encounter an unkown research status or castType
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Override
	public final List<Spell> getSortedSpellsInSection (final List<SpellResearchStatus> spells, final SpellBookSectionID desiredSectionID,
		final SpellCastType castType, final CommonDatabase db) throws MomException, RecordNotFoundException
	{
		// Different selection and sorting logic if we are after spells that we've not yet researched, so just check this once up front
		final boolean desiredSectionIsResearch =
			(desiredSectionID == SpellBookSectionID.RESEARCHABLE_NOW) ||
			(desiredSectionID == SpellBookSectionID.RESEARCHABLE);

		// Check each spell
		final List<SpellWithSortValue> spellsFound = new ArrayList<SpellWithSortValue> ();

		for (final SpellResearchStatus thisSpellResearchStatus : spells)
		{
			final Spell thisSpell = db.findSpell (thisSpellResearchStatus.getSpellID (), "getSortedSpellsInSection");

			// Check section matches
			final SpellBookSectionID modifiedSectionID = getModifiedSectionID (thisSpell, thisSpellResearchStatus.getStatus (), true);

			if (((desiredSectionID == null) && (modifiedSectionID == null)) ||
				((desiredSectionID != null) && (desiredSectionID.equals (modifiedSectionID))))

				// Combat only spells appear in our overland spell book only if they've not yet been researched
				if ((desiredSectionIsResearch) ||
					(!desiredSectionIsResearch) && (spellCanBeCastIn (thisSpell, castType)))
				{
					final int castingCost;

					/*
					 * Which cost should we use for sorting?
					 * Use Base costs, so spells still stay sorted the same even if we reduce their actual casting cost to zero
					 */
					if (desiredSectionIsResearch)
						castingCost = thisSpellResearchStatus.getRemainingResearchCost ();
					else
						switch (castType)
						{
							case OVERLAND:
								castingCost = thisSpell.getOverlandCastingCost ();
								break;
							case COMBAT:
								castingCost = thisSpell.getCombatCastingCost ();
								break;
							default:
								throw new MomException ("getSortedSpellsInSection: Invalid CastType");
						}

					// Add it
					spellsFound.add (new SpellWithSortValue (thisSpell, castingCost));
				}
		}

		// Sort the list
		Collections.sort (spellsFound);

		// Convert the list, stripping off the sort values
		final List<Spell> result = new ArrayList<Spell> ();
		for (final SpellWithSortValue thisSpell : spellsFound)
			result.add (thisSpell.spell);

		return result;
	}

	/**
	 * @return Player pick utils
	 */
	public final PlayerPickUtils getPlayerPickUtils ()
	{
		return playerPickUtils;
	}

	/**
	 * @param utils Player pick utils
	 */
	public final void setPlayerPickUtils (final PlayerPickUtils utils)
	{
		playerPickUtils = utils;
	}

	/**
	 * @return Spell calculations
	 */
	public final SpellCalculations getSpellCalculations ()
	{
		return spellCalculations;
	}

	/**
	 * @param calc Spell calculations
	 */
	public final void setSpellCalculations (final SpellCalculations calc)
	{
		spellCalculations = calc;
	}

	/**
	 * @return Hero item calculations
	 */
	public final HeroItemCalculations getHeroItemCalculations ()
	{
		return heroItemCalculations;
	}

	/**
	 * @param calc Hero item calculations
	 */
	public final void setHeroItemCalculations (final HeroItemCalculations calc)
	{
		heroItemCalculations = calc;
	}
}