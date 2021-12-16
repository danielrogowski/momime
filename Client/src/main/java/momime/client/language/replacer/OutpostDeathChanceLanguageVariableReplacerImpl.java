package momime.client.language.replacer;

import momime.common.database.RecordNotFoundException;
import momime.common.internal.OutpostDeathChanceBreakdown;
import momime.common.internal.OutpostDeathChanceBreakdownSpell;

/**
 * Language replacer for outpost death chance variables
 */
public final class OutpostDeathChanceLanguageVariableReplacerImpl extends BreakdownLanguageVariableReplacerImpl<OutpostDeathChanceBreakdown>
	implements OutpostDeathChanceLanguageVariableReplacer
{
	/** Spell specific breakdown */
	private OutpostDeathChanceBreakdownSpell currentSpell;

	/**
	 * @param code Code to replace
	 * @return Replacement value; or null if the code is not recognized
	 * @throws RecordNotFoundException If an expected data item can't be found
	 */
	@Override
	public final String determineVariableValue (final String code) throws RecordNotFoundException
	{
		final String text;
		switch (code)
		{
			case "BASE_CHANCE":
				text = getTextUtils ().intToStrPlusMinus (getBreakdown ().getBaseChance ());
				break;
				
			case "TOTAL_CHANCE":
				text = Integer.valueOf (getBreakdown ().getTotalChance ()).toString ();
				break;
				
			// Dependant on current spell
			case "SPELL_NAME":
				text = getLanguageHolder ().findDescription (getClient ().getClientDB ().findSpell (getCurrentSpell ().getSpellID (), "determineVariableValue").getSpellName ());
				break;

			case "SPELL_MODIFIER":
				text = getTextUtils ().intToStrPlusMinus (getCurrentSpell ().getSpellModifier ());
				break;
				
			default:
				text = null;
		}
		return text;
	}

	/**
	 * @return Spell specific breakdown
	 */
	public final OutpostDeathChanceBreakdownSpell getCurrentSpell ()
	{
		return currentSpell;
	}
	
	/**
	 * @param spell Spell specific breakdown
	 */
	@Override
	public final void setCurrentSpell (final OutpostDeathChanceBreakdownSpell spell)
	{
		currentSpell = spell;
	}
}