package momime.client.language.replacer;

import java.io.IOException;
import java.util.List;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.utils.TextUtils;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitSkill;
import momime.common.database.UnitSkillAndValue;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.PlayerPick;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.PlayerPickUtils;

/**
 * Replacer for replacing language strings to do with unit stats
 */
public final class UnitStatsLanguageVariableReplacerImpl extends LanguageVariableReplacerTokenImpl implements UnitStatsLanguageVariableReplacer
{
	/** Multiplayer client */
	private MomClient client;

	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Client-side unit utils */
	private UnitClientUtils unitClientUtils;
	
	/** The unit whose stats we're outputting */
	private ExpandedUnitDetails unit;
	
	/** Text utils */
	private TextUtils textUtils;
	
	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Memory CAE utils */
	private MemoryCombatAreaEffectUtils memoryCombatAreaEffectUtils;
	
	/**
	 * @param code Code to replace
	 * @return Replacement value; or null if the code is not recognized
	 * @throws IOException If there is an error calculating a replacement value
	 */
	@Override
	public final String determineVariableValue (final String code) throws IOException
	{
		final String text;
		switch (code)
		{
			// Unit names (see comments against the UnitNameType enum for exactly what text these generate)
			case "SIMPLE_UNIT_NAME":
				
				text = getUnitClientUtils ().getUnitName (getUnit ().getUnit (), UnitNameType.SIMPLE_UNIT_NAME);
				break;
			
			case "RACE_UNIT_NAME":
				text = getUnitClientUtils ().getUnitName (getUnit ().getUnit (), UnitNameType.RACE_UNIT_NAME);
				break;
			
			case "A_UNIT_NAME":
				text = getUnitClientUtils ().getUnitName (getUnit ().getUnit (), UnitNameType.A_UNIT_NAME);
				break;
			
			case "THE_UNIT_OF_NAME":
				text = getUnitClientUtils ().getUnitName (getUnit ().getUnit (), UnitNameType.THE_UNIT_OF_NAME);
				break;
			
			case "A_UNIT_OF_NAME":
				text = getUnitClientUtils ().getUnitName (getUnit ().getUnit (), UnitNameType.A_UNIT_OF_NAME);
				break;
			
			case "UNITS_OF_NAME":
				text = getUnitClientUtils ().getUnitName (getUnit ().getUnit (), UnitNameType.UNITS_OF_NAME);
				break;
			
			// Used by skill descriptions
			case "MANA_TOTAL":
				text = Integer.valueOf (getUnit ().calculateManaTotal ()).toString ();
				break;

			case "MANA_REMAINING":
				if (getUnit ().isMemoryUnit ())
					text = Integer.valueOf (getUnit ().getManaRemaining ()).toString ();
				else
					text = "MANA_TOTAL";
				break;

			case "AMMO_TOTAL":
				text = Integer.valueOf (getUnit ().calculateFullRangedAttackAmmo ()).toString ();
				break;
				
			case "AMMO_REMAINING":
				if (getUnit ().isMemoryUnit ())
					text = Integer.valueOf (getUnit ().getAmmoRemaining ()).toString ();
				else
					text = "AMMO_TOTAL";
				break;
				
			case "RAT_NAME":
				if (getUnit ().getRangedAttackType () == null)
					text = null;
				else
					text = getLanguageHolder ().findDescription (getUnit ().getRangedAttackType ().getRangedAttackTypeDescription ());
				break;
				
			// This outputs the actual level of the unit, including or excluding (a.k.a. natural) any bonuses from Warlord and Crusade,
			// e.g. for a unit with 120 experience owned by a wizard with Warlord and Crusade, this will say Champion
			case "EXPERIENCE_LEVEL_NAME":
			case "EXPERIENCE_NATURAL_LEVEL_NAME":
			{
				final ExperienceLevel expLvl = code.equals ("EXPERIENCE_LEVEL_NAME") ? getUnit ().getModifiedExperienceLevel () : getUnit ().getBasicExperienceLevel ();
				if (expLvl == null)
					text = null;
				else
					text = getLanguageHolder ().findDescription (expLvl.getExperienceLevelName ());
				break;
			}
			
			// This outputs a phrase describing the bonuses (if any) the unit gets from its current (modified) experience level, either similar to:
			// 'Normal troops begin as recruits.' or
			// 'Veteran troops have the following enhanced combat factors: +1 to hit, +2 defence and +3 resistance.'
			case "EXPERIENCE_SKILL_BONUSES":
			{
				// Work this out only once
				final ExperienceLevel expLvl = getUnit ().getModifiedExperienceLevel ();
				if (expLvl == null)
					text = null;
				else if (expLvl.getLevelNumber () == 0)
					text = getLanguageHolder ().findDescription (getUnit ().getUnitType ().getUnitTypeInexperienced ());
				else
				{
					final StringBuilder bonuses = new StringBuilder ();
					
					// List out all the bonuses this exp level gives
					for (final UnitSkillAndValue bonus : expLvl.getExperienceSkillBonus ())
						
						// Don't mention skills that the unit does not have
						if ((bonus.getUnitSkillValue () != null) && (getUnit ().hasModifiedSkill (bonus.getUnitSkillID ())))
						{
							if (bonuses.length () > 0)
								bonuses.append (", ");

							bonuses.append ("+");
							bonuses.append (bonus.getUnitSkillValue ());
							bonuses.append (" ");
							
							final UnitSkill attr = getClient ().getClientDB ().findUnitSkill (bonus.getUnitSkillID (), "determineVariableValue");
							final String attrDescription = getLanguageHolder ().findDescription (attr.getUnitSkillDescription ());
							
							// Strip off the "+ " prefix from "+ to Hit" and "+ to Block" otherwise we end up with "+2 + to Hit" which looks silly
							bonuses.append ((attrDescription != null) ? attrDescription.replaceAll ("\\+ ", "") : bonus.getUnitSkillID ());
						}

					text = getLanguageHolder ().findDescription (getUnit ().getUnitType ().getUnitTypeExperienced ()) + " " +
						getTextUtils ().replaceFinalCommaByAnd (bonuses.toString ()) + ".";
				}

				break;
			}
			
			// If the unit gains some level boost from Warlord and/or Crusade, then this outputs some text describing that
			case "EXPERIENCE_LEVEL_BOOST":
			{
				final ExperienceLevel modifiedExpLvl = getUnit ().getModifiedExperienceLevel ();
				
				// 2nd part of this will exclude if we get no actual bonus, e.g. don't list Warlord as usefully doing anything for heroes who are naturally at Demi-God level
				if ((modifiedExpLvl == null) || (modifiedExpLvl == getUnit ().getBasicExperienceLevel ()))					
					text = null;
				else
					text = getLanguageHolder ().findDescription (getLanguages ().getHelpScreen ().getExperienceLevelBoost ()).replaceAll
						("BONUS_LIST", describeLevelBoosters ());

				break;
			}
			
			// This says how much more experience a unit needs to get to the next level
			case "EXPERIENCE_NEXT_LEVEL":
			{
				final ExperienceLevel naturalExpLvl = getUnit ().getBasicExperienceLevel ();
				if (naturalExpLvl == null)
					text = null;
				else
				{
					final ExperienceLevel nextExpLevel = getUnit ().getUnitType ().findExperienceLevel (naturalExpLvl.getLevelNumber () + 1);

					// See if there's a higher level we can reach at all, either by gaining more experience or by warlord/crusade
					if (nextExpLevel == null)
						text = getLanguageHolder ().findDescription (getUnit ().getUnitType ().getUnitTypeMaxExperience ());
					
					// Can the higher level only be attained by warlord/crusade?
					else if (nextExpLevel.getExperienceRequired () == null)
						text = getLanguageHolder ().findDescription (getUnit ().getUnitType ().getUnitTypeMaxNaturalExperience ());
						
					else
					{
						// How much exp do we need to reach the next level?
						final int expRequired = nextExpLevel.getExperienceRequired () - getUnit ().getBasicSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE);
						
						// What level is the unit now, including Warlord and Crusade?
						final ExperienceLevel modifiedExpLvl = getUnit ().getModifiedExperienceLevel ();
						
						// Does this wizard get any level boosts from warlord/crusade?  And if so, is there a higher level than the next natural level for this unit?						
						if ((naturalExpLvl != modifiedExpLvl) && (getUnit ().getUnitType ().findExperienceLevel (naturalExpLvl.getLevelNumber () + 2) != null))
						{
							// We've only proved that some higher level exists - we don't know if the actual level boost will be +1 or +2, if they have both Warlord and Crusade
							// Equally if they DO have both Warlord and Crusade, we have to make sure we don't raise up to a level that doesn't exist
							int modifiedLevelNumber = modifiedExpLvl.getLevelNumber () + 1;
							ExperienceLevel modifiedLevel = getUnit ().getUnitType ().findExperienceLevel (modifiedLevelNumber);
							while (modifiedLevel == null)
							{
								modifiedLevelNumber--;
								modifiedLevel = getUnit ().getUnitType ().findExperienceLevel (modifiedLevelNumber);
							}
							
							text = getLanguageHolder ().findDescription (getLanguages ().getHelpScreen ().getExperienceForNextLevelWithBoost ()).replaceAll
								("EXPERIENCE_REQUIRED", Integer.valueOf (expRequired).toString ()).replaceAll
								("NEXT_EXPERIENCE_NATURAL_LEVEL_NAME", getLanguageHolder ().findDescription (nextExpLevel.getExperienceLevelName ())).replaceAll
								("BONUS_LIST", describeLevelBoosters ()).replaceAll
								("NEXT_EXPERIENCE_LEVEL_NAME", getLanguageHolder ().findDescription (modifiedLevel.getExperienceLevelName ()));
						}
						else
							text = getLanguageHolder ().findDescription (getLanguages ().getHelpScreen ().getExperienceForNextLevelWithoutBoost ()).replaceAll
								("EXPERIENCE_REQUIRED", Integer.valueOf (expRequired).toString ()).replaceAll
								("NEXT_EXPERIENCE_NATURAL_LEVEL_NAME", getLanguageHolder ().findDescription (nextExpLevel.getExperienceLevelName ()));
					}
				}
				break;
			}
				
			default:
				// This outputs the value of the specified skill, e.g. SKILL_VALUE_US098 outputs how much experience the unit has
				if (code.startsWith ("SKILL_VALUE_"))
					text = Integer.valueOf (getUnit ().getModifiedSkillValue (code.substring (12))).toString ();
				
				// This outputs 'Super' if the value of the specified skill is 2 or more
				else if (code.startsWith ("SUPER_"))
					text = (getUnit ().getModifiedSkillValue (code.substring (6)) > 1) ? "Super" : "";
				
				else
					text = null;
		}
		return text;
	}
	
	/**
	 * @return List of things the wizard have that increase the level of their units, i.e. "", "Warlord", "Crusade" or "Warlord and Crusade"
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws RecordNotFoundException If we can't find one of the text strings
	 */
	private final String describeLevelBoosters () throws PlayerNotFoundException, RecordNotFoundException
	{
		final StringBuilder s = new StringBuilder ();
		
		// This follows the same logic and checks as UnitUtilsImpl.getExperienceLevel ()
		// Does the player have the Warlord retort?
		final List<PlayerPick> picks = ((MomPersistentPlayerPublicKnowledge) getUnit ().getOwningPlayer ().getPersistentPlayerPublicKnowledge ()).getPick ();
		if (getPlayerPickUtils ().getQuantityOfPick (picks, CommonDatabaseConstants.RETORT_ID_WARLORD) > 0)
		{
			if (s.length () > 0)
				s.append (", ");
			
			s.append (getLanguageHolder ().findDescription
				(getClient ().getClientDB ().findPick (CommonDatabaseConstants.RETORT_ID_WARLORD, "describeLevelBoosters").getPickDescriptionSingular ()));
		}

		// Does the player have the Crusade CAE?
		if (getMemoryCombatAreaEffectUtils ().findCombatAreaEffect (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (),
			null, CommonDatabaseConstants.COMBAT_AREA_EFFECT_CRUSADE, unit.getOwningPlayerID ()) != null)
		{
			if (s.length () > 0)
				s.append (", ");

			s.append (getLanguageHolder ().findDescription
				(getClient ().getClientDB ().findCombatAreaEffect (CommonDatabaseConstants.COMBAT_AREA_EFFECT_CRUSADE, "describeLevelBoosters").getCombatAreaEffectDescription ()));
		}
		
		return getTextUtils ().replaceFinalCommaByAnd (s.toString ());
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
	public final MomLanguagesEx getLanguages ()
	{
		return languageHolder.getLanguages ();
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
	public final ExpandedUnitDetails getUnit ()
	{
		return unit;
	}

	/**
	 * @param u The unit whose stats we're outputting
	 */
	@Override
	public final void setUnit (final ExpandedUnitDetails u)
	{
		unit = u;
	}

	/**
	 * @return Text utils
	 */
	public final TextUtils getTextUtils ()
	{
		return textUtils;
	}

	/**
	 * @param tu Text utils
	 */
	public final void setTextUtils (final TextUtils tu)
	{
		textUtils = tu;
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

	/**
	 * @return Session utils
	 */
	public final MultiplayerSessionUtils getMultiplayerSessionUtils ()
	{
		return multiplayerSessionUtils;
	}

	/**
	 * @param util Session utils
	 */
	public final void setMultiplayerSessionUtils (final MultiplayerSessionUtils util)
	{
		multiplayerSessionUtils = util;
	}

	/**
	 * @return Memory CAE utils
	 */
	public final MemoryCombatAreaEffectUtils getMemoryCombatAreaEffectUtils ()
	{
		return memoryCombatAreaEffectUtils;
	}

	/**
	 * @param utils Memory CAE utils
	 */
	public final void setMemoryCombatAreaEffectUtils (final MemoryCombatAreaEffectUtils utils)
	{
		memoryCombatAreaEffectUtils = utils;
	}
}