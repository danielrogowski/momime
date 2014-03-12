package momime.common.calculations;

import java.text.DecimalFormat;
import java.util.List;
import java.util.logging.Logger;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.v0_9_4.SpellSettingData;
import momime.common.database.v0_9_4.PickProductionBonus;
import momime.common.database.v0_9_4.Spell;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.PlayerPick;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.SpellUtils;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * Calculations for dealing with spell casting cost reductions and research bonuses
 */
public final class MomSpellCalculationsImpl implements MomSpellCalculations
{
	/** Class logger */
	private final Logger log = Logger.getLogger (MomSpellCalculationsImpl.class.getName ());
	
	/** Format used for doubles in debug messages */
	private static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat ("0.000");
	
	/** Spell utils */
	private SpellUtils spellUtils;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;

	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/**
	 * @param bookCount The number of books we have in the magic realm of the spell for which we want to calculate the reduction, e.g. to calculate reductions for life spells, pass in how many life books we have
	 * @param spellSettings Spell combination settings, either from the server XML cache or the Session description
	 * @param spell Cache object of the spell we want to check the reduction for (need this since certain retorts give bonuses to certain types of spells), can pass in null for this
	 * @param picks Retorts the player has, so we can check them for any which give casting cost reductions
	 * @param db Lookup lists built over the XML database
	 * @return The casting cost reduction
	 * @throws MomException If we find an invalid casting reduction type
	 * @throws RecordNotFoundException If there is a pick in the list that we can't find in the DB
	 */
	@Override
	public final double calculateCastingCostReduction (final int bookCount, final SpellSettingData spellSettings, final Spell spell,
		final List<PlayerPick> picks, final CommonDatabase db)
		throws MomException, RecordNotFoundException
	{
		log.entering (MomSpellCalculationsImpl.class.getName (), "calculateCastingCostReduction", new String [] {new Integer (bookCount).toString (),
			new Integer (spellSettings.getSpellBooksToObtainFirstReduction ()).toString (),
			new Integer (spellSettings.getSpellBooksCastingReduction ()).toString (),
			spellSettings.getSpellBooksCastingReductionCombination ().toString (), new Integer (spellSettings.getSpellBooksCastingReductionCap ()).toString (),
			(spell == null) ? null : spell.getSpellID (), (picks == null) ? null : picks.toString ()});

		// How many books do we have that will give a bonus?
		final int booksThatGiveReduction = Math.max (bookCount - spellSettings.getSpellBooksToObtainFirstReduction () + 1, 0);

		double castingCostMultiplier;
		if (booksThatGiveReduction <= 0)
			// 1 = 100% casting cost
			castingCostMultiplier = 1d;
		else
			// Calculate reduction from number of books
			switch (spellSettings.getSpellBooksCastingReductionCombination ())
			{
				case ADDITIVE:
					// e.g. 10% * 4 = 40% (but the figure we actually need is 0.6)
					castingCostMultiplier = 1d - (spellSettings.getSpellBooksCastingReduction () * booksThatGiveReduction / 100d);
					break;
				case MULTIPLICATIVE:
					// e.g. for a value of 5%, we want to do 0.95 ^ 4
					castingCostMultiplier = Math.pow (1d - (spellSettings.getSpellBooksCastingReduction () / 100d), booksThatGiveReduction);
					break;
				default:
					throw new MomException ("calculateCastingCostReduction: Unknown combination type (books)");
			}

		log.finest (booksThatGiveReduction + " books give a base casting cost reduction of " + DECIMAL_FORMATTER.format (castingCostMultiplier) + "...");

		// Get the values we need from the spell
		final String spellMagicRealmID;
		final String spellUnitTypeID;
		if (spell != null)
		{
			spellMagicRealmID = spell.getSpellRealm ();
			spellUnitTypeID = getSpellUtils ().spellSummonsUnitTypeID (spell, db);
		}
		else
		{
			// Cannot use blank magic realm ID, since Runemaster applies only to Arcane spells, which have blank Magic Realm ID, so we have to use something else
			// No issues with unit type ID though
			spellMagicRealmID = "_";
			spellUnitTypeID = null;
		}

		// Search for retorts that give us other bonuses
		// This is basically doing the same as MomPlayerPicks.getTotalProductionBonus
		if (picks != null)
			for (final PlayerPick p : picks)
			{
				// Does this pick give any casting cost reduction?
				for (final PickProductionBonus pickProductionBonus : db.findPick (p.getPickID (), "calculateCastingCostReduction").getPickProductionBonus ())
					if (pickProductionBonus.getProductionTypeID ().equals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_SPELL_COST_REDUCTION))
					{
						// Do we have a reduction value, and do any magic realm requirements match what we're casting?
						final boolean isMagicRealmIdBlank = (pickProductionBonus.isMagicRealmIdBlank () == null) ? false : pickProductionBonus.isMagicRealmIdBlank ();

						if ((pickProductionBonus.getPercentageBonus () > 0) &&
							((pickProductionBonus.getMagicRealmID () == null) || (pickProductionBonus.getMagicRealmID ().equals (spellMagicRealmID))) &&
							((!isMagicRealmIdBlank) || (spellMagicRealmID == null)) &&
							((pickProductionBonus.getUnitTypeID () == null) || (pickProductionBonus.getUnitTypeID ().equals (spellUnitTypeID))))
						{
							switch (spellSettings.getSpellBooksCastingReductionCombination ())
							{
								case ADDITIVE:
									castingCostMultiplier = castingCostMultiplier - (pickProductionBonus.getPercentageBonus () * p.getQuantity () / 100d);
									break;
								case MULTIPLICATIVE:
									castingCostMultiplier = castingCostMultiplier * Math.pow (1d - (pickProductionBonus.getPercentageBonus () / 100d), p.getQuantity ());
									break;
								default:
									throw new MomException ("calculateCastingCostReduction: Unknown combination type (retorts)");
							}

							log.finest (pickProductionBonus.getPercentageBonus () + "% further reduction from " + p.getQuantity () + "x " + p.getPickID () +
								" improves casting cost reduction to " + DECIMAL_FORMATTER.format (castingCostMultiplier) + "...");
						}
					}
			}

		// Result up to this point will be e.g. 0.75 for a 25% bonus, so convert this to actually say 25
		double castingCostPercentageReduction = (1d - castingCostMultiplier) * 100d;

		if (castingCostPercentageReduction > spellSettings.getSpellBooksCastingReductionCap ())
		{
			log.finest ("Final casting cost reduction = " + DECIMAL_FORMATTER.format (castingCostPercentageReduction) +
				"% but this is capped at " + DECIMAL_FORMATTER.format (spellSettings.getSpellBooksCastingReductionCap ()));

			castingCostPercentageReduction = spellSettings.getSpellBooksCastingReductionCap ();
		}
		else
			log.finest ("Final casting cost reduction = " + DECIMAL_FORMATTER.format (castingCostPercentageReduction) + "%");

		log.exiting (MomSpellCalculationsImpl.class.getName (), "calculateCastingCostReduction", castingCostPercentageReduction);
		return castingCostPercentageReduction;
	}

	/**
	 * @param bookCount The number of books we have in the magic realm of the spell for which we want to calculate the reduction, e.g. to calculate reductions for life spells, pass in how many life books we have
	 * @param spellSettings Spell combination settings, either from the server XML cache or the Session description
	 * @param spell Cache object of the spell we want to check the reduction for (need this since certain retorts give bonuses to certain types of spells), can pass in null for this
	 * @param picks Retorts the player has, so we can check them for any which give casting cost reductions
	 * @param db Lookup lists built over the XML database
	 * @return The casting cost reduction
	 * @throws MomException If we find an invalid casting reduction type
	 * @throws RecordNotFoundException If there is a pick in the list that we can't find in the DB
	 */
	@Override
	public final double calculateResearchBonus (final int bookCount, final SpellSettingData spellSettings, final Spell spell,
		final List<PlayerPick> picks, final CommonDatabase db)
		throws MomException, RecordNotFoundException
	{
		log.entering (MomSpellCalculationsImpl.class.getName (), "calculateResearchBonus", new String [] {new Integer (bookCount).toString (),
			new Integer (spellSettings.getSpellBooksToObtainFirstReduction ()).toString (),
			new Integer (spellSettings.getSpellBooksResearchBonus ()).toString (),
			spellSettings.getSpellBooksResearchBonusCombination ().toString (), new Integer (spellSettings.getSpellBooksResearchBonusCap ()).toString (),
			(spell == null) ? null : spell.getSpellID (), (picks == null) ? null : picks.toString ()});

		// How many books do we have that will give a bonus?
		final int booksThatGiveBonus = Math.max (bookCount - spellSettings.getSpellBooksToObtainFirstReduction () + 1, 0);

		double researchBonus;
		if (booksThatGiveBonus <= 0)
			// Research put towards spell = 1 * research generated, i.e. no bonus
			researchBonus = 1d;
		else
			// Calculate bonus from number of books
			switch (spellSettings.getSpellBooksResearchBonusCombination ())
			{
				case ADDITIVE:
					// 10% * 4 = 40% (but the figure we actually need is 1.4)
					researchBonus = 1d + (spellSettings.getSpellBooksResearchBonus () * booksThatGiveBonus / 100d);
					break;
				case MULTIPLICATIVE:
					// e.g. for a value of 5%, we want to do 1.05 ^ 4
					researchBonus = Math.pow (1d + (spellSettings.getSpellBooksResearchBonus () / 100d), booksThatGiveBonus);
					break;
				default:
					throw new MomException ("calculateResearchBonus: Unknown combination type (books)");
			}

		log.finest (booksThatGiveBonus + " books give a base research bonus of " + DECIMAL_FORMATTER.format (researchBonus) + "...");

		// Get the values we need from the spell
		final String spellMagicRealmID;
		final String spellUnitTypeID;
		if (spell != null)
		{
			spellMagicRealmID = spell.getSpellRealm ();
			spellUnitTypeID = getSpellUtils ().spellSummonsUnitTypeID (spell, db);
		}
		else
		{
			// Cannot use blank magic realm ID, since Runemaster applies only to Arcane spells, which have blank Magic Realm ID, so we have to use something else
			// No issues with unit type ID though
			spellMagicRealmID = "_";
			spellUnitTypeID = null;
		}

		// Search for retorts that give us other bonuses
		// This is basically doing the same as MomPlayerPicks.getTotalProductionBonus
		if (picks != null)
			for (final PlayerPick p : picks)
			{
				// Does this pick give any research bonus?
				for (final PickProductionBonus pickProductionBonus : db.findPick (p.getPickID (), "calculateResearchBonus").getPickProductionBonus ())
					if (pickProductionBonus.getProductionTypeID ().equals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH))
					{
						// Do we have a bonus value, and do any magic realm requirements match what we're casting?
						final boolean isMagicRealmIdBlank = (pickProductionBonus.isMagicRealmIdBlank () == null) ? false : pickProductionBonus.isMagicRealmIdBlank ();

						if ((pickProductionBonus.getPercentageBonus () > 0) &&
							((pickProductionBonus.getMagicRealmID () == null) || (pickProductionBonus.getMagicRealmID ().equals (spellMagicRealmID))) &&
							((!isMagicRealmIdBlank) || (spellMagicRealmID == null)) &&
							((pickProductionBonus.getUnitTypeID () == null) || (pickProductionBonus.getUnitTypeID ().equals (spellUnitTypeID))))
						{
							switch (spellSettings.getSpellBooksResearchBonusCombination ())
							{
								case ADDITIVE:
									researchBonus = researchBonus + (pickProductionBonus.getPercentageBonus () * p.getQuantity () / 100d);
									break;
								case MULTIPLICATIVE:
									researchBonus = researchBonus * Math.pow (1d + (pickProductionBonus.getPercentageBonus () / 100d), p.getQuantity ());
									break;
								default:
									throw new MomException ("calculateCastingCostReduction: Unknown combination type (retorts)");
							}

							log.finest (pickProductionBonus.getPercentageBonus () + "% further bonus from " + p.getQuantity () + "x " + p.getPickID () +
								" improves research bonus reduction to " + DECIMAL_FORMATTER.format (researchBonus) + "...");
						}
					}
			}

		// Result up to this point will be e.g. 1.25 for a 25% bonus, so convert this to actually say 25
		double researchPercentageBonus = (researchBonus - 1d) * 100d;

		if (researchPercentageBonus > spellSettings.getSpellBooksResearchBonusCap ())
		{
			log.finest ("Final research bonus = " + DECIMAL_FORMATTER.format (researchPercentageBonus) +
				"% but this is capped at " + DECIMAL_FORMATTER.format (spellSettings.getSpellBooksResearchBonusCap ()));

			researchPercentageBonus = spellSettings.getSpellBooksResearchBonusCap ();
		}
		else
			log.finest ("Final research bonus = " + DECIMAL_FORMATTER.format (researchPercentageBonus) + "%");

		log.exiting (MomSpellCalculationsImpl.class.getName (), "calculateResearchBonus", researchPercentageBonus);
		return researchPercentageBonus;
	}

	/**
	 * Spells cast into combat cost more MP to cast the further away they are from the wizard's fortess (though the casting skill used remains the same).
	 * Table is on p323 in the strategy guide. 
	 * 
	 * @param player Player casting the spell
	 * @param combatLocation Combat location they are casting a spell at
	 * @param allowEitherPlane For combats in Towers of Wizardry, where we can always consider ourselves to be on the same plane as the wizard's fortress
	 * @param map Known terrain
	 * @param buildings Known buildings
	 * @param overlandMapCoordinateSystem Overland map coordinate system
	 * @return Double the range penalty for casting spells in combat; or null if the player simply can't cast any spells at all right now because they're banished
	 */
	@Override
	public final Integer calculateDoubleCombatCastingRangePenalty (final PlayerPublicDetails player, final OverlandMapCoordinatesEx combatLocation,
		final boolean allowEitherPlane, final MapVolumeOfMemoryGridCells map, final List<MemoryBuilding> buildings, final CoordinateSystem overlandMapCoordinateSystem)
	{
		log.entering (MomSpellCalculationsImpl.class.getName (), "calculateDoubleCombatCastingRangePenalty",
			new String [] {player.getPlayerDescription ().getPlayerID ().toString (), combatLocation.toString ()});
			
		// First need to find where the wizard's fortress is
		Integer penalty;
		final OverlandMapCoordinatesEx fortressLocation = getMemoryBuildingUtils ().findCityWithBuilding
			(player.getPlayerDescription ().getPlayerID (), CommonDatabaseConstants.VALUE_BUILDING_FORTRESS, map, buildings);
		if (fortressLocation == null)
			penalty = null;
		else
		{
			// Different planes always = max penalty
			if ((!allowEitherPlane) && (combatLocation.getPlane () != fortressLocation.getPlane ()))
				penalty = 6;
			else
			{
				// This gives a real distance, e.g. 1.4 for a 1x1 diagonal
				// Remember we need to return double the value in the table
				final double distance = CoordinateSystemUtils.determineRealDistanceBetween (overlandMapCoordinateSystem, combatLocation, fortressLocation);
				penalty = (((int) distance) + 9) / 5;
				if (penalty > 6)
					penalty = 6;
			}
				
			// Channeler retort limits range penalty to 1x
			// Tested this in the original MoM to prove it really does cap it at 1x, because that's not really "as if at the wizard's fortress"
			// like the description says, which would be capping it at �x, so the description is a bit ambigious
			final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
			if ((penalty > 2) && (getPlayerPickUtils ().getQuantityOfPick (ppk.getPick (), CommonDatabaseConstants.VALUE_RETORT_ID_CHANNELER) > 0))
				penalty = 2;
		}

		log.exiting (MomSpellCalculationsImpl.class.getName (), "calculateDoubleCombatCastingRangePenalty", penalty);
		return penalty;
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
	 * @return Memory building utils
	 */
	public final MemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param utils Memory building utils
	 */
	public final void setMemoryBuildingUtils (final MemoryBuildingUtils utils)
	{
		memoryBuildingUtils = utils;
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
}
