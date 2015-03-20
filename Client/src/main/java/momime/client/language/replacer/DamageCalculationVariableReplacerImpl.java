package momime.client.language.replacer;

import java.io.IOException;

import momime.client.language.database.UnitAttributeLang;
import momime.client.language.database.UnitSkillLang;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.client.utils.WizardClientUtils;

/**
 * Language replacer for detailed breakdown message of combat dice rolls used in rolling damage
 */
public final class DamageCalculationVariableReplacerImpl extends BreakdownLanguageVariableReplacerImpl<DamageCalculationBreakdown> implements DamageCalculationVariableReplacer
{
	/** Client-side unit utils */
	private UnitClientUtils unitClientUtils;
	
	/** Wizard client utils */
	private WizardClientUtils wizardClientUtils;
	
	/**
	 * @param code Code to replace
	 * @return Replacement value; or null if the code is not recognized
	 * @throws IOException If there is an error calculating a replacement value
	 */
	@Override
	protected final String determineVariableValue (final String code) throws IOException
	{
		final String text;
		switch (code)
		{
			case "ATTACKER_NAME":
				text = getWizardClientUtils ().getPlayerName (getBreakdown ().getAttackingPlayer ());
				break;

			case "DEFENDER_NAME":
				text = getWizardClientUtils ().getPlayerName (getBreakdown ().getDefenderPlayer ());
				break;

			case "ATTACKER_RACE_UNIT_NAME":
				text = getUnitClientUtils ().getUnitName (getBreakdown ().getAttackerUnit (), UnitNameType.RACE_UNIT_NAME);
				break;

			case "DEFENDER_RACE_UNIT_NAME":
				text = getUnitClientUtils ().getUnitName (getBreakdown ().getDefenderUnit (), UnitNameType.RACE_UNIT_NAME);
				break;

			case "ATTACKER_FIGURES":
				text = getBreakdown ().getAttackerFigures ().toString ();
				break;
				
			case "DEFENDER_FIGURES":
				text = getBreakdown ().getDefenderFigures ().toString ();
				break;
				
			case "ATTACK_STRENGTH":
				text = getBreakdown ().getAttackStrength ().toString ();
				break;

			case "DEFENCE_STRENGTH":
				text = getBreakdown ().getDefenceStrength ().toString ();
				break;
				
			case "CHANCE_TO_HIT":
				text = new Integer (getBreakdown ().getChanceToHit () * 10).toString ();
				break;
				
			case "CHANCE_TO_DEFEND":
				text = new Integer (getBreakdown ().getChanceToDefend () * 10).toString ();
				break;
				
			case "POTENTIAL_DAMAGE":
				text = getBreakdown ().getPotentialDamage ().toString ();
				break;
				
			case "AVERAGE_DAMAGE":
				text = getTextUtils ().insertDecimalPoint (getBreakdown ().getTenTimesAverageDamage (), 1);				
				break;
				
			case "ACTUAL_DAMAGE":
				text = getBreakdown ().getActualDamage ().toString ();
				break;
				
			case "AVERAGE_BLOCK":
				text = getTextUtils ().insertDecimalPoint (getBreakdown ().getTenTimesAverageBlock (), 1);
				break;
				
			case "ACTUAL_BLOCK":
				final StringBuilder buf = new StringBuilder ();
				for (final Integer n : getBreakdown ().getActualBlockedHits ())
				{
					if (buf.length () > 0)
						buf.append (",");
					
					buf.append (n);
				}
				text = buf.toString ();
				break;

			// Either a unit skill ID or a unit attribute ID
			case "ATTACK_TYPE":
				if (getBreakdown ().getAttackSkillID () != null)
				{
					final UnitSkillLang unitSkill = getLanguage ().findUnitSkill (getBreakdown ().getAttackSkillID ());
					final String unitSkillDescription = (unitSkill == null) ? null : unitSkill.getUnitSkillDescription ();
					text = (unitSkillDescription != null) ? unitSkillDescription : getBreakdown ().getAttackSkillID ();
				}
				else
				{
					final UnitAttributeLang unitAttr = getLanguage ().findUnitAttribute (getBreakdown ().getAttackAttributeID ());
					final String unitAttrDescription = (unitAttr == null) ? null : unitAttr.getUnitAttributeDescription ();
					text = (unitAttrDescription != null) ? unitAttrDescription : getBreakdown ().getAttackAttributeID ();
				}
				break;
				
			default:
				text = null;
		}
		return text;
	}

	/**
	 * @return Client-side unit utils
	 */
	public final UnitClientUtils getUnitClientUtils ()
	{
		return unitClientUtils;
	}

	/**
	 * @param util Client-side unit utils
	 */
	public final void setUnitClientUtils (final UnitClientUtils util)
	{
		unitClientUtils = util;
	}

	/**
	 * @return Wizard client utils
	 */
	public final WizardClientUtils getWizardClientUtils ()
	{
		return wizardClientUtils;
	}

	/**
	 * @param util Wizard client utils
	 */
	public final void setWizardClientUtils (final WizardClientUtils util)
	{
		wizardClientUtils = util;
	}
}