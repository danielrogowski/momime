package momime.client.language.replacer;

import java.io.IOException;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.UnitTypeLang;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.common.calculations.UnitCalculations;
import momime.common.database.ExperienceLevel;
import momime.common.messages.AvailableUnit;
import momime.common.messages.MemoryUnit;
import momime.common.utils.UnitUtils;

/**
 * Replacer for replacing language strings to do with unit stats
 */
public final class UnitStatsLanguageVariableReplacerImpl extends LanguageVariableReplacerImpl implements UnitStatsLanguageVariableReplacer
{
	/** Multiplayer client */
	private MomClient client;

	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** Client-side unit utils */
	private UnitClientUtils unitClientUtils;
	
	/** The unit whose stats we're outputting */
	private AvailableUnit unit;
	
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
			// Unit names (see comments against the UnitNameType enum for exactly what text these generate)
			case "SIMPLE_UNIT_NAME":
				text = getUnitClientUtils ().getUnitName (getUnit (), UnitNameType.SIMPLE_UNIT_NAME);
				break;
			
			case "RACE_UNIT_NAME":
				text = getUnitClientUtils ().getUnitName (getUnit (), UnitNameType.RACE_UNIT_NAME);
				break;
			
			case "A_UNIT_NAME":
				text = getUnitClientUtils ().getUnitName (getUnit (), UnitNameType.A_UNIT_NAME);
				break;
			
			case "THE_UNIT_OF_NAME":
				text = getUnitClientUtils ().getUnitName (getUnit (), UnitNameType.THE_UNIT_OF_NAME);
				break;
			
			// Used by skill descriptions
			case "MANA_TOTAL":
				text = new Integer (getUnitCalculations ().calculateManaTotal (getUnit (), getUnit ().getUnitHasSkill (), getClient ().getPlayers (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (), getClient ().getClientDB ())).toString ();
				break;

			case "MANA_REMAINING":
				if (getUnit () instanceof MemoryUnit)
					text = new Integer (((MemoryUnit) getUnit ()).getManaRemaining ()).toString ();
				else
					text = "MANA_TOTAL";
				break;

			case "AMMO_TOTAL":
				text = new Integer (getUnitCalculations ().calculateFullRangedAttackAmmo (getUnit (), getUnit ().getUnitHasSkill (), getClient ().getPlayers (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (), getClient ().getClientDB ())).toString ();
				break;
				
			case "AMMO_REMAINING":
				if (getUnit () instanceof MemoryUnit)
					text = new Integer (((MemoryUnit) getUnit ()).getRangedAttackAmmo ()).toString ();
				else
					text = "AMMO_TOTAL";
				break;
				
			case "RAT_NAME":
				final String rangedAttackTypeID = getClient ().getClientDB ().findUnit (getUnit ().getUnitID (), "UnitStatsLanguageVariableReplacer").getRangedAttackType ();
				if (rangedAttackTypeID == null)
					text = null;
				else
					text = getLanguage ().findRangedAttackTypeDescription (rangedAttackTypeID);
				break;
				
			// This outputs the actual level of the unit, including any bonuses from Warlord and Crusade,
			// e.g. for a unit with 120 experience owned by a wizard with Warlord and Crusade, this will say Champion
			case "EXPERIENCE_LEVEL_NAME":
				final ExperienceLevel expLvl = getUnitUtils ().getExperienceLevel (getUnit (), true, getClient ().getPlayers (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (), getClient ().getClientDB ());
				if (expLvl == null)
					text = null;
				else
				{
					final String unitMagicRealmID = getClient ().getClientDB ().findUnit (getUnit ().getUnitID (), "UnitStatsLanguageVariableReplacer").getUnitMagicRealm ();
					final String unitTypeID = getClient ().getClientDB ().findUnitMagicRealm (unitMagicRealmID, "UnitStatsLanguageVariableReplacer").getUnitTypeID ();
					final UnitTypeLang unitType = getLanguage ().findUnitType (unitTypeID);
					if (unitType == null)
						text = null;
					else
						text = unitType.findExperienceLevelName (expLvl.getLevelNumber ());
				}
				break;
				
			default:
				// This outputs the value of the specified skill, e.g. SKILL_VALUE_US098 outputs how much experience the unit has
				if (code.startsWith ("SKILL_VALUE_"))
					text = new Integer (getUnitUtils ().getModifiedSkillValue (getUnit (), getUnit ().getUnitHasSkill (), code.substring (12), getClient ().getPlayers (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (), getClient ().getClientDB ())).toString ();
				
				// This outputs 'Super' if the value of the specified skill is 2 or more
				else if (code.startsWith ("SUPER_"))
				{
					text = (getUnitUtils ().getModifiedSkillValue (getUnit (), getUnit ().getUnitHasSkill (), code.substring (6), getClient ().getPlayers (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (), getClient ().getClientDB ()) > 1) ? "Super" : "";
				}
				
				else
					text = null;
		}
		return text;
	}

	/**
	 * @return Multiplayer client
	 */
	public final MomClient getClient ()
	{
		return client;
	}
	
	/**
	 * @param obj Multiplayer client
	 */
	public final void setClient (final MomClient obj)
	{
		client = obj;
	}
	
	/**
	 * @return Language database holder
	 */
	public final LanguageDatabaseHolder getLanguageHolder ()
	{
		return languageHolder;
	}
	
	/**
	 * @param holder Language database holder
	 */
	public final void setLanguageHolder (final LanguageDatabaseHolder holder)
	{
		languageHolder = holder;
	}

	/**
	 * Convenience shortcut for accessing the Language XML database
	 * @return Language database
	 */
	public final LanguageDatabaseEx getLanguage ()
	{
		return languageHolder.getLanguage ();
	}

	/**
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final UnitUtils utils)
	{
		unitUtils = utils;
	}
	
	/**
	 * @return Unit calculations
	 */
	public final UnitCalculations getUnitCalculations ()
	{
		return unitCalculations;
	}

	/**
	 * @param calc Unit calculations
	 */
	public final void setUnitCalculations (final UnitCalculations calc)
	{
		unitCalculations = calc;
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
	 * @return The unit whose stats we're outputting
	 */
	public final AvailableUnit getUnit ()
	{
		return unit;
	}

	/**
	 * @param u The unit whose stats we're outputting
	 */
	@Override
	public final void setUnit (final AvailableUnit u)
	{
		unit = u;
	}
}