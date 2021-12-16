package momime.client.language.replacer;

import momime.common.internal.OutpostDeathChanceBreakdown;
import momime.common.internal.OutpostDeathChanceBreakdownSpell;

/**
 * Language replacer for outpost death chance variables
 */
public interface OutpostDeathChanceLanguageVariableReplacer extends BreakdownLanguageVariableReplacer<OutpostDeathChanceBreakdown>
{
	/**
	 * @param spell Spell specific breakdown
	 */
	public void setCurrentSpell (final OutpostDeathChanceBreakdownSpell spell);
}