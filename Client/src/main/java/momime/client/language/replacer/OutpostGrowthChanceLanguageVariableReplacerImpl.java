package momime.client.language.replacer;

import momime.common.database.RecordNotFoundException;
import momime.common.internal.OutpostGrowthChanceBreakdown;
import momime.common.internal.OutpostGrowthChanceBreakdownMapFeature;
import momime.common.internal.OutpostGrowthChanceBreakdownSpell;

/**
 * Language replacer for outpost growth chance variables
 */
public final class OutpostGrowthChanceLanguageVariableReplacerImpl extends BreakdownLanguageVariableReplacerImpl<OutpostGrowthChanceBreakdown>
	implements OutpostGrowthChanceLanguageVariableReplacer
{
	/** Map feature specific breakdown */
	private OutpostGrowthChanceBreakdownMapFeature currentMapFeature;
	
	/** Spell specific breakdown */
	private OutpostGrowthChanceBreakdownSpell currentSpell;
	
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
			case "MAXIMUM_POPULATION":
				text = getTextUtils ().intToStrPlusMinus (getBreakdown ().getMaximumPopulation ());
				break;
				
			case "RACIAL_GROWTH_MODIFIER":
				text = getTextUtils ().intToStrPlusMinus (getBreakdown ().getRacialGrowthModifier ());
				break;

			case "TOTAL_CHANCE":
				text = Integer.valueOf (getBreakdown ().getTotalChance ()).toString ();
				break;
				
			// Dependant on current map feature
			case "MAP_FEATURE":
				text = getLanguageHolder ().findDescription (getClient ().getClientDB ().findMapFeature (getCurrentMapFeature ().getMapFeatureID (), "determineVariableValue").getMapFeatureDescription ());
				break;

			case "MAP_FEATURE_MODIFIER":
				text = getTextUtils ().intToStrPlusMinus (getCurrentMapFeature ().getMapFeatureModifier ());
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
	 * @return Map feature specific breakdown
	 */
	public final OutpostGrowthChanceBreakdownMapFeature getCurrentMapFeature ()
	{
		return currentMapFeature;
	}
	
	/**
	 * @param mapFeature Map feature specific breakdown
	 */
	@Override
	public final void setCurrentMapFeature (final OutpostGrowthChanceBreakdownMapFeature mapFeature)
	{
		currentMapFeature = mapFeature;
	}

	/**
	 * @return Spell specific breakdown
	 */
	public final OutpostGrowthChanceBreakdownSpell getCurrentSpell ()
	{
		return currentSpell;
	}
	
	/**
	 * @param spell Spell specific breakdown
	 */
	@Override
	public final void setCurrentSpell (final OutpostGrowthChanceBreakdownSpell spell)
	{
		currentSpell = spell;
	}
}