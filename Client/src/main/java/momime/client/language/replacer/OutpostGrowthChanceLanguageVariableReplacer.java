package momime.client.language.replacer;

import momime.common.internal.OutpostGrowthChanceBreakdown;
import momime.common.internal.OutpostGrowthChanceBreakdownMapFeature;
import momime.common.internal.OutpostGrowthChanceBreakdownSpell;

/**
 * Language replacer for outpost growth chance variables
 */
public interface OutpostGrowthChanceLanguageVariableReplacer extends BreakdownLanguageVariableReplacer<OutpostGrowthChanceBreakdown>
{
	/**
	 * @param mapFeature Map feature specific breakdown
	 */
	public void setCurrentMapFeature (final OutpostGrowthChanceBreakdownMapFeature mapFeature);
	
	/**
	 * @param spell Spell specific breakdown
	 */
	public void setCurrentSpell (final OutpostGrowthChanceBreakdownSpell spell);
}