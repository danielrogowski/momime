package momime.common.calculations;

import java.util.List;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.v0_9_4.SpellSettingData;
import momime.common.database.v0_9_4.Spell;
import momime.common.messages.v0_9_4.PlayerPick;

/**
 * Calculations for dealing with spell casting cost reductions and research bonuses
 */
public interface MomSpellCalculations
{
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
	public double calculateCastingCostReduction (final int bookCount, final SpellSettingData spellSettings, final Spell spell,
		final List<PlayerPick> picks, final CommonDatabase db)
		throws MomException, RecordNotFoundException;

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
	public double calculateResearchBonus (final int bookCount, final SpellSettingData spellSettings, final Spell spell,
		final List<PlayerPick> picks, final CommonDatabase db)
		throws MomException, RecordNotFoundException;
}
