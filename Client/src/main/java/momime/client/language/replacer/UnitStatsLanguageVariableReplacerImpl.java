package momime.client.language.replacer;

import java.io.IOException;
import java.util.List;

import momime.client.MomClient;
import momime.client.language.database.CombatAreaEffectLang;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.PickLang;
import momime.client.language.database.UnitAttributeLang;
import momime.client.language.database.UnitTypeLang;
import momime.client.utils.TextUtils;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceAttributeBonus;
import momime.common.database.ExperienceLevel;
import momime.common.database.UnitAttributeComponent;
import momime.common.database.UnitAttributePositiveNegative;
import momime.common.database.UnitType;
import momime.common.messages.AvailableUnit;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.PlayerPick;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.UnitSkillUtils;
import momime.common.utils.UnitTypeUtils;
import momime.common.utils.UnitUtils;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * Replacer for replacing language strings to do with unit stats
 */
public final class UnitStatsLanguageVariableReplacerImpl extends LanguageVariableReplacerTokenImpl implements UnitStatsLanguageVariableReplacer
{
	/** Multiplayer client */
	private MomClient client;

	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Unit utils */
	private UnitUtils unitUtils;

	/** Unit skill utils */
	private UnitSkillUtils unitSkillUtils;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** Client-side unit utils */
	private UnitClientUtils unitClientUtils;
	
	/** The unit whose stats we're outputting */
	private AvailableUnit unit;
	
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
				
			// This outputs the actual level of the unit, including or excluding (a.k.a. natural) any bonuses from Warlord and Crusade,
			// e.g. for a unit with 120 experience owned by a wizard with Warlord and Crusade, this will say Champion
			case "EXPERIENCE_LEVEL_NAME":
			case "EXPERIENCE_NATURAL_LEVEL_NAME":
			{
				final ExperienceLevel expLvl = getUnitUtils ().getExperienceLevel (getUnit (), code.equals ("EXPERIENCE_LEVEL_NAME"), getClient ().getPlayers (),
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
			}
			
			// This outputs a phrase describing the bonuses (if any) the unit gets from its current (modified) experience level, either similar to:
			// 'Normal troops begin as recruits.' or
			// 'Veteran troops have the following enhanced combat factors: +1 to hit, +2 defence and +3 resistance.'
			case "EXPERIENCE_ATTRIBUTE_BONUSES":
			{
				// Work this out only once
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
					else if (expLvl.getLevelNumber () == 0)
						text = unitType.getUnitTypeInexperienced ();
					else
					{
						final StringBuilder bonuses = new StringBuilder ();
						
						// List out all the bonuses this exp level gives
						for (final ExperienceAttributeBonus bonus : expLvl.getExperienceAttributeBonus ())
							
							// Don't mention +1 ranged if the unit has no ranged attack
							if ((!bonus.getUnitAttributeID ().equals (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK)) ||
								(getUnitSkillUtils ().getModifiedAttributeValue (getUnit (), bonus.getUnitAttributeID (), UnitAttributeComponent.BASIC,
									UnitAttributePositiveNegative.BOTH, getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
									getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (), getClient ().getClientDB ()) > 0))
							{
								if (bonuses.length () > 0)
									bonuses.append (", ");

								bonuses.append ("+");
								bonuses.append (bonus.getBonusValue ());
								bonuses.append (" ");
								
								final UnitAttributeLang attr = getLanguage ().findUnitAttribute (bonus.getUnitAttributeID ());
								final String attrDescription = (attr == null) ? null : attr.getUnitAttributeDescription ();
								
								// Strip off the "+ " prefix from "+ to Hit" and "+ to Block" otherwise we end up with "+2 + to Hit" which looks silly
								bonuses.append ((attrDescription != null) ? attrDescription.replaceAll ("\\+ ", "") : bonus.getUnitAttributeID ());
							}

						text = unitType.getUnitTypeExperienced () + " " + getTextUtils ().replaceFinalCommaByAnd (bonuses.toString ()) + ".";
					}
				}

				break;
			}
			
			// If the unit gains some level boost from Warlord and/or Crusade, then this outputs some text describing that
			case "EXPERIENCE_LEVEL_BOOST":
			{
				final ExperienceLevel modifiedExpLvl = getUnitUtils ().getExperienceLevel (getUnit (), true, getClient ().getPlayers (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (), getClient ().getClientDB ());
				
				// 2nd part of this will exclude if we get no actual bonus, e.g. don't list Warlord as usefully doing anything for heroes who are naturally at Demi-God level
				if ((modifiedExpLvl == null) || (modifiedExpLvl == getUnitUtils ().getExperienceLevel (getUnit (), false, getClient ().getPlayers (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (), getClient ().getClientDB ())))
					
					text = null;
				else
					text = getLanguage ().findCategoryEntry ("frmHelp", "ExperienceLevelBoost").replaceAll
						("BONUS_LIST", describeLevelBoosters ());

				break;
			}
			
			// This says how much more experience a unit needs to get to the next level
			case "EXPERIENCE_NEXT_LEVEL":
			{
				final ExperienceLevel naturalExpLvl = getUnitUtils ().getExperienceLevel (getUnit (), false, getClient ().getPlayers (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (), getClient ().getClientDB ());
				if (naturalExpLvl == null)
					text = null;
				else
				{
					final String unitMagicRealmID = getClient ().getClientDB ().findUnit (getUnit ().getUnitID (), "UnitStatsLanguageVariableReplacer").getUnitMagicRealm ();
					final String unitTypeID = getClient ().getClientDB ().findUnitMagicRealm (unitMagicRealmID, "UnitStatsLanguageVariableReplacer").getUnitTypeID ();
					final UnitType unitType = getClient ().getClientDB ().findUnitType (unitTypeID, "UnitStatsLanguageVariableReplacer");
					
					final ExperienceLevel nextExpLevel = UnitTypeUtils.findExperienceLevel (unitType, naturalExpLvl.getLevelNumber () + 1);
					final UnitTypeLang unitTypeLang = getLanguage ().findUnitType (unitTypeID);
					
					if (unitTypeLang == null)
						text = null;

					// See if there's a higher level we can reach at all, either by gaining more experience or by warlord/crusade
					else if (nextExpLevel == null)
						text = unitTypeLang.getUnitTypeMaxExperience ();
					
					// Can the higher level only be attained by warlord/crusade?
					else if (nextExpLevel.getExperienceRequired () == null)
						text = unitTypeLang.getUnitTypeMaxNaturalExperience ();
						
					else
					{
						// How much exp do we need to reach the next level?
						final int expRequired = nextExpLevel.getExperienceRequired () - getUnitUtils ().getBasicSkillValue
							(getUnit ().getUnitHasSkill (), CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE);
						
						// What level is the unit now, including Warlord and Crusade?
						final ExperienceLevel modifiedExpLvl = getUnitUtils ().getExperienceLevel (getUnit (), true, getClient ().getPlayers (),
							getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (), getClient ().getClientDB ());
						
						// Does this wizard get any level boosts from warlord/crusade?  And if so, is there a higher level than the next natural level for this unit?						
						if ((naturalExpLvl != modifiedExpLvl) && (UnitTypeUtils.findExperienceLevel (unitType, naturalExpLvl.getLevelNumber () + 2) != null))
						{
							// We've only proved that some higher level exists - we don't know if the actual level boost will be +1 or +2, if they have both Warlord and Crusade
							// Equally if they DO have both Warlord and Crusade, we have to make sure we don't raise up to a level that doesn't exist
							int modifiedLevelNumber = modifiedExpLvl.getLevelNumber () + 1;
							while (UnitTypeUtils.findExperienceLevel (unitType, modifiedLevelNumber) == null)
								modifiedLevelNumber--;
							
							text = getLanguage ().findCategoryEntry ("frmHelp", "ExperienceForNextLevelWithBoost").replaceAll
								("EXPERIENCE_REQUIRED", new Integer (expRequired).toString ()).replaceAll
								("NEXT_EXPERIENCE_NATURAL_LEVEL_NAME", unitTypeLang.findExperienceLevelName (naturalExpLvl.getLevelNumber () + 1)).replaceAll
								("BONUS_LIST", describeLevelBoosters ()).replaceAll
								("NEXT_EXPERIENCE_LEVEL_NAME", unitTypeLang.findExperienceLevelName (modifiedLevelNumber));
						}
						else
							text = getLanguage ().findCategoryEntry ("frmHelp", "ExperienceForNextLevelWithoutBoost").replaceAll
								("EXPERIENCE_REQUIRED", new Integer (expRequired).toString ()).replaceAll
								("NEXT_EXPERIENCE_NATURAL_LEVEL_NAME", unitTypeLang.findExperienceLevelName (naturalExpLvl.getLevelNumber () + 1));
					}
				}
				break;
			}
				
			default:
				// This outputs the value of the specified skill, e.g. SKILL_VALUE_US098 outputs how much experience the unit has
				if (code.startsWith ("SKILL_VALUE_"))
					text = new Integer (getUnitSkillUtils ().getModifiedSkillValue (getUnit (), getUnit ().getUnitHasSkill (), code.substring (12), getClient ().getPlayers (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (), getClient ().getClientDB ())).toString ();
				
				// This outputs 'Super' if the value of the specified skill is 2 or more
				else if (code.startsWith ("SUPER_"))
				{
					text = (getUnitSkillUtils ().getModifiedSkillValue (getUnit (), getUnit ().getUnitHasSkill (), code.substring (6), getClient ().getPlayers (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (), getClient ().getClientDB ()) > 1) ? "Super" : "";
				}
				
				else
					text = null;
		}
		return text;
	}
	
	/**
	 * @return List of things the wizard have that increase the level of their units, i.e. "", "Warlord", "Crusade" or "Warlord and Crusade"
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 */
	private final String describeLevelBoosters () throws PlayerNotFoundException
	{
		final StringBuilder s = new StringBuilder ();
		
		// This follows the same logic and checks as UnitUtilsImpl.getExperienceLevel ()
		// Does the player have the Warlord retort?
		final PlayerPublicDetails owningPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getUnit ().getOwningPlayerID (), "getExperienceLevel");
		final List<PlayerPick> picks = ((MomPersistentPlayerPublicKnowledge) owningPlayer.getPersistentPlayerPublicKnowledge ()).getPick ();
		if (getPlayerPickUtils ().getQuantityOfPick (picks, CommonDatabaseConstants.RETORT_ID_WARLORD) > 0)
		{
			if (s.length () > 0)
				s.append (", ");
			
			final PickLang pick = getLanguage ().findPick (CommonDatabaseConstants.RETORT_ID_WARLORD);
			final String pickDescription = (pick == null) ? null : pick.getPickDescriptionSingular ();
			s.append ((pickDescription != null) ? pickDescription : CommonDatabaseConstants.RETORT_ID_WARLORD);
		}

		// Does the player have the Crusade CAE?
		if (getMemoryCombatAreaEffectUtils ().findCombatAreaEffect (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (),
			null, CommonDatabaseConstants.COMBAT_AREA_EFFECT_CRUSADE, unit.getOwningPlayerID ()) != null)
		{
			if (s.length () > 0)
				s.append (", ");

			final CombatAreaEffectLang cae = getLanguage ().findCombatAreaEffect (CommonDatabaseConstants.COMBAT_AREA_EFFECT_CRUSADE);
			final String caeDescription = (cae == null) ? null : cae.getCombatAreaEffectDescription ();
			s.append ((caeDescription != null) ? caeDescription : CommonDatabaseConstants.COMBAT_AREA_EFFECT_CRUSADE);
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
	 * @return Unit skill utils
	 */
	public final UnitSkillUtils getUnitSkillUtils ()
	{
		return unitSkillUtils;
	}

	/**
	 * @param utils Unit skill utils
	 */
	public final void setUnitSkillUtils (final UnitSkillUtils utils)
	{
		unitSkillUtils = utils;
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