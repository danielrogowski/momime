package momime.client.language.replacer;

import momime.common.messages.v0_9_5.AvailableUnit;

/**
 * Replacer for replacing language strings to do with unit stats
 */
public interface UnitStatsLanguageVariableReplacer extends LanguageVariableReplacer
{
	/**
	 * @param u The unit whose stats we're outputting
	 */
	public void setUnit (final AvailableUnit u);
}