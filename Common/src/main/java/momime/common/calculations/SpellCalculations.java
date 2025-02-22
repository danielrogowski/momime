package momime.common.calculations;

import java.util.List;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellSetting;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.PlayerPick;

/**
 * Calculations for dealing with spell casting cost reductions and research bonuses
 */
public interface SpellCalculations
{
	/**
	 * The ability to pass nulls in for spell+picks is a leftover from the Delphi client/editor having a preview screen showing the
	 * calculated cost reduction given a particular set of spell settings, during the new game process before we have chosen
	 * any picks or are researching/casting anything. 
	 * 
	 * @param bookCount The number of books we have in the magic realm of the spell for which we want to calculate the reduction, e.g. to calculate reductions for life spells, pass in how many life books we have
	 * @param spellSettings Spell combination settings, either from the server XML cache or the Session description
	 * @param spell Cache object of the spell we want to check the reduction for (need this since certain retorts give bonuses to certain types of spells), can pass in null for this
	 * @param picks Retorts the player has, so we can check them for any which give casting cost reductions
	 * @param db Lookup lists built over the XML database
	 * @return The casting cost reduction
	 * @throws MomException If we find an invalid casting reduction type
	 * @throws RecordNotFoundException If there is a pick in the list that we can't find in the DB
	 */
	public double calculateCastingCostReduction (final int bookCount, final SpellSetting spellSettings, final Spell spell,
		final List<PlayerPick> picks, final CommonDatabase db)
		throws MomException, RecordNotFoundException;

	/**
	 * The ability to pass nulls in for spell+picks is a leftover from the Delphi client/editor having a preview screen showing the
	 * calculated research bonus given a particular set of spell settings, during the new game process before we have chosen
	 * any picks or are researching/casting anything.
	 *
	 * @param bookCount The number of books we have in the magic realm of the spell for which we want to calculate the reduction, e.g. to calculate reductions for life spells, pass in how many life books we have
	 * @param spellSettings Spell combination settings, either from the server XML cache or the Session description
	 * @param spell Cache object of the spell we want to check the reduction for (need this since certain retorts give bonuses to certain types of spells), can pass in null for this
	 * @param picks Retorts the player has, so we can check them for any which give casting cost reductions
	 * @param db Lookup lists built over the XML database
	 * @return The casting cost reduction
	 * @throws MomException If we find an invalid casting reduction type
	 * @throws RecordNotFoundException If there is a pick in the list that we can't find in the DB
	 */
	public double calculateResearchBonus (final int bookCount, final SpellSetting spellSettings, final Spell spell,
		final List<PlayerPick> picks, final CommonDatabase db)
		throws MomException, RecordNotFoundException;
	
	/**
	 * Spells cast into combat cost more MP to cast the further away they are from the wizard's fortess (though the casting skill used remains the same).
	 * Table is on p323 in the strategy guide. 
	 * 
	 * @param wizardDetails Wizard casting the spell
	 * @param combatLocation Combat location they are casting a spell at
	 * @param allowEitherPlane For combats in Towers of Wizardry, where we can always consider ourselves to be on the same plane as the wizard's fortress
	 * @param map Known terrain
	 * @param buildings Known buildings
	 * @param overlandMapCoordinateSystem Overland map coordinate system
	 * @return Double the range penalty for casting spells in combat; or null if the player simply can't cast any spells at all right now because they're banished
	 */
	public Integer calculateDoubleCombatCastingRangePenalty (final KnownWizardDetails wizardDetails, final MapCoordinates3DEx combatLocation,
		final boolean allowEitherPlane, final MapVolumeOfMemoryGridCells map, final List<MemoryBuilding> buildings, final CoordinateSystem overlandMapCoordinateSystem);
}