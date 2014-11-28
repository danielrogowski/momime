package momime.common.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import momime.common.MomException;
import momime.common.calculations.MomSpellCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.SpellSettingData;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.SpellValidUnitTarget;
import momime.common.database.SummonedUnit;
import momime.common.messages.PlayerPick;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Simple spell lookups and calculations
 */
public final class SpellUtilsImpl implements SpellUtils
{
	/** Class logger */
	private final Log log = LogFactory.getLog (SpellUtilsImpl.class);

	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/** Spell calculations */
	private MomSpellCalculations spellCalculations;
	
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
		log.trace ("Entering findSpellResearchStatus: " + spellID);

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

		log.trace ("Exiting findSpellResearchStatus = " + chosenSpellStatus);
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
		log.trace ("Entering calculateCastingCostReduction: " + spell.getSpellID ());

		// for War Bears, Sky Drakes, etc. we want "S", and for Summon Hero, Champion & Incarnation we want "H"
		String result = null;
		if (spell.getSpellBookSectionID () == SpellBookSectionID.SUMMONING)
			for (final SummonedUnit spellSummonsUnit : spell.getSummonedUnit ())
			{
				// Now find the unit's magic realm / lifeform type
				final String thisMagicRealmID = db.findUnit (spellSummonsUnit.getSummonedUnitID (), "spellSummonsUnitTypeID").getUnitMagicRealm ();

				// Use the cache for the magic realm / lifeform type to get the Unit Type ID
				final String unitTypeID = db.findUnitMagicRealm (thisMagicRealmID, "spellSummonsUnitTypeID").getUnitTypeID ();

				// We need to only end up with a single Unit Type ID, so it must either be the first one encountered or the same as what we already have
				if (result == null)
					result = unitTypeID;
				else if (!result.equals (unitTypeID))
					throw new MomException ("Summoning spell " + spell.getSpellID () + " can summon units of more than one Unit Type");
			}

		log.trace ("Exiting calculateCastingCostReduction = " + result);
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
		log.trace ("Entering spellCanBeCastIn: " + spell.getSpellID () + ", " + castType);

		boolean result;

		switch (castType)
		{
			case OVERLAND:
				result = ((spell.getOverlandCastingCost () != null) && (spell.getOverlandCastingCost () > 0));
				break;
			case COMBAT:
				result = ((spell.getCombatCastingCost () != null) && (spell.getCombatCastingCost () > 0));
				break;
			default:
				throw new MomException ("spellCanBeCastIn: Invalid CastType");
		}

		log.trace ("Exiting spellCanBeCastIn = " + result);
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
	public final int getReducedCombatCastingCost (final Spell spell, final List<PlayerPick> picks, final SpellSettingData spellSettings, final CommonDatabase db)
		throws MomException, RecordNotFoundException
	{
		return getReducedCastingCostForCastingType (spell, SpellCastType.COMBAT, picks, spellSettings, db);
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
	public final int getReducedOverlandCastingCost (final Spell spell, final List<PlayerPick> picks, final SpellSettingData spellSettings, final CommonDatabase db)
		throws MomException, RecordNotFoundException
	{
		return getReducedCastingCostForCastingType (spell, SpellCastType.OVERLAND, picks, spellSettings, db);
	}

	/**
	 * @param spell Spell we want to cast
	 * @param castType The context of the spell cast (overland or combat), determines base cost of spell
	 * @param picks Books and retorts the player has, so we can check them for any which give casting cost reductions
	 * @param spellSettings Spell combination settings, either from the server XML cache or the Session description
	 * @param db Lookup lists built over the XML database
	 * @return Casting cost, modified (reduced) by us having 8 or more spell books, Chaos/Nature/Sorcery Mastery, and so on
	 * @throws MomException If we find an invalid casting reduction type
	 * @throws RecordNotFoundException If there is a pick in the list that we can't find in the DB
	 */
	private final int getReducedCastingCostForCastingType (final Spell spell, final SpellCastType castType, final List<PlayerPick> picks,
		final SpellSettingData spellSettings, final CommonDatabase db)
		throws MomException, RecordNotFoundException
	{
		log.trace ("Entering getReducedCastingCostForCastingType: " + spell.getSpellID () + ", " + castType);

		// Get the base cost, which is different depending on casting type
		int baseCastingCost;
		switch (castType)
		{
			case OVERLAND:
				baseCastingCost = spell.getOverlandCastingCost ();
				break;
			case COMBAT:
				baseCastingCost = spell.getCombatCastingCost ();
				break;
			default:
				throw new MomException ("getReducedCastingCostForCastingType: Unexpected SpellCastType value for castType parameter.");
		}

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

		final int reductionAmount = (int) (reduction * baseCastingCost);
		final int castingCostForCastingType = baseCastingCost - reductionAmount;

		log.trace ("Exiting getReducedCastingCostForCastingType = " + castingCostForCastingType);
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
	public final SpellBookSectionID getModifiedSectionID (final Spell spell, final SpellResearchStatus researchStatus, final boolean considerWhetherResearched)
		throws MomException
	{
		log.trace ("Entering getModifiedSectionID: " + researchStatus + ", " + considerWhetherResearched);

		final SpellBookSectionID result;
		if (!considerWhetherResearched)
		{
			// Ignore whether researched or not, just return the value from the DB
			result = spell.getSpellBookSectionID ();
		}
		else
			switch (researchStatus.getStatus ())
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
					throw new MomException ("getSectionID: Unknown spell status " + researchStatus.getStatus ());
			}

		log.trace ("Exiting getModifiedSectionID = " + result);
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
		log.trace ("Entering spellCanTargetMagicRealmLifeform: " + spell.getSpellID () + ", " + targetMagicRealmLifeformTypeID);

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

		log.trace ("Exiting spellCanTargetMagicRealmLifeform = " + targetIsValidForThisSpell);
		return targetIsValidForThisSpell;
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
		log.trace ("Entering getSpellsForRealmRankStatusInternal: " +
			desiredMagicRealmID + ", " + spellRankID + ", " + ((statuses == null) ? null : new Integer (statuses.size ()).toString ()));

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

		log.trace ("Exiting getSpellsForRealmRankStatusInternal = " + resultList.size ());
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
		log.trace ("Entering getSpellsForRealmRankStatus: " + magicRealmID + ", " + spellRankID + ", " + status);

		// Convert null to empty string if searching for arcane spells
		final String useMagicRealmID = (magicRealmID == null) ? "" : magicRealmID;

		// Put status into a single item list
		final List<SpellResearchStatusID> tempStatusList = new ArrayList<SpellResearchStatusID> ();
		tempStatusList.add (status);

		final List<Spell> result = getSpellsForRealmRankStatusInternal (spells, useMagicRealmID, spellRankID, tempStatusList, db);

		log.trace ("Exiting getSpellsForRealmRankStatus = " + result.size ());
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
		log.trace ("Entering getSpellsForStatus: " + status);

		// Put status into a single item list
		final List<SpellResearchStatusID> tempStatusList = new ArrayList<SpellResearchStatusID> ();
		tempStatusList.add (status);

		final List<Spell> result = getSpellsForRealmRankStatusInternal (spells, null, null, tempStatusList, db);

		log.trace ("Exiting getSpellsForStatus = " + result.size ());
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
		log.trace ("Entering getSpellsForRealmAndRank: " + magicRealmID + ", " + spellRankID);

		// Convert null to empty string if searching for arcane spells
		final String useMagicRealmID = (magicRealmID == null) ? "" : magicRealmID;

		final List<Spell> result = getSpellsForRealmRankStatusInternal (spells, useMagicRealmID, spellRankID, null, db);

		log.trace ("Exiting getSpellsForRealmAndRank = " + result.size ());
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
		log.trace ("Entering getSpellsForRankAndStatus: " + spellRankID + ", " + status);

		// Put status into a single item list
		final List<SpellResearchStatusID> tempStatusList = new ArrayList<SpellResearchStatusID> ();
		tempStatusList.add (status);

		final List<Spell> result = getSpellsForRealmRankStatusInternal (spells, null, spellRankID, tempStatusList, db);

		log.trace ("Exiting getSpellsForRankAndStatus = " + result.size ());
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
		log.trace ("Entering getSpellsNotInBookForRealmAndRank: " + magicRealmID + ", " + spellRankID);

		// Convert null to empty string if searching for arcane spells
		final String useMagicRealmID = (magicRealmID == null) ? "" : magicRealmID;

		// Build list of statuses that we're looking for
		final List<SpellResearchStatusID> tempStatusList = new ArrayList<SpellResearchStatusID> ();

		tempStatusList.add (SpellResearchStatusID.UNAVAILABLE);
		tempStatusList.add (SpellResearchStatusID.NOT_IN_SPELL_BOOK);

		final List<Spell> result = getSpellsForRealmRankStatusInternal (spells, useMagicRealmID, spellRankID, tempStatusList, db);

		log.trace ("Exiting getSpellsNotInBookForRealmAndRank = " + result);
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
		log.trace ("Entering getSpellRanksForStatus: " + status);

		final List<String> spellRanksFound = new ArrayList<String> ();

		for (final SpellResearchStatus thisSpellResearchStatus : spells)
		{
			final Spell thisSpell = db.findSpell (thisSpellResearchStatus.getSpellID (), "getSpellRanksForStatus");

			if ((thisSpellResearchStatus.getStatus () == status) && (!spellRanksFound.contains (thisSpell.getSpellRank ())))
				spellRanksFound.add (thisSpell.getSpellRank ());
		}

		log.trace ("Exiting getSpellRanksForStatus = " + spellRanksFound);
		return spellRanksFound;
	}

	/**
	 * @param spells List of all the spells defined in the XML file
	 * @param magicRealmID Filters list to items in this magic realm; arcane spells have null magic realm so null searches for Arcane spells
	 * @return List of Spell Rank IDs for which there are spells in this list in the specified magic realm
	 */
	@Override
	public final List<String> getSpellRanksForMagicRealm (final List<? extends Spell> spells, final String magicRealmID)
	{
		log.trace ("Entering getSpellRanksForMagicRealm: " + magicRealmID);

		final List<String> spellRanksFound = new ArrayList<String> ();

		for (final Spell thisSpell : spells)

			if (((magicRealmID == null) && (thisSpell.getSpellRealm () == null)) ||
				((magicRealmID != null) && (magicRealmID.equals (thisSpell.getSpellRealm ()))))

				if (!spellRanksFound.contains (thisSpell.getSpellRank ()))
					spellRanksFound.add (thisSpell.getSpellRank ());

		log.trace ("Exiting getSpellRanksForMagicRealm = " + spellRanksFound.size ());
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
		log.trace ("Entering getSortedSpellsInSection: " + desiredSectionID + ", " + castType);

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
			final SpellBookSectionID modifiedSectionID = getModifiedSectionID (thisSpell, thisSpellResearchStatus, true);

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

		log.trace ("Exiting getSortedSpellsInSection = " + result.size ());
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
	public final MomSpellCalculations getSpellCalculations ()
	{
		return spellCalculations;
	}

	/**
	 * @param calc Spell calculations
	 */
	public final void setSpellCalculations (final MomSpellCalculations calc)
	{
		spellCalculations = calc;
	}
}