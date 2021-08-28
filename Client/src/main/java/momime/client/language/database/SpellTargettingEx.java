package momime.client.language.database;

import java.util.List;

import momime.client.languages.database.SpellTargetting;
import momime.common.MomException;
import momime.common.database.LanguageText;
import momime.common.utils.TargetSpellResult;

/**
 * TargetSpellResult is in common, so need separate class in client to use the enum values to pick out language entries
 */
public final class SpellTargettingEx extends SpellTargetting
{
	 /**
	  * @param targetSpellResult Targetting result we want language text for
	  * @return Language text for targetting at a city
	  * @throws MomException If the enum value is unknown
	  */
	public final List<LanguageText> getUnitLanguageText (final TargetSpellResult targetSpellResult) throws MomException
	{
		 final List<LanguageText> languageText;
		 switch (targetSpellResult)
		 {
			case ENCHANTING_OR_HEALING_ENEMY:
				languageText = getEnchantingEnemyUnit ();
				break;
				
			case CURSING_OR_ATTACKING_OWN:
				languageText = getCursingOwnUnit ();
				break;
				
			case NO_SPELL_EFFECT_IDS_DEFINED:
				languageText = getNoUnitSpellEffectIDsDefined ();
				break;
				
			case ALREADY_HAS_ALL_POSSIBLE_SPELL_EFFECTS:
				languageText = getAlreadyHasAllPossibleUnitSpellEffects ();
				break;
				
			case UNIT_INVALID_MAGIC_REALM_LIFEFORM_TYPE:
				languageText = getInvalidMagicRealmLifeformType ();
				break;
				
			case TOO_HIGH_RESISTANCE:
				languageText = getTooHighResistance ();
				break;
				
			case IMMUNE:
				languageText = getImmune ();
				break;
				
			case UNIT_NOT_IN_EXPECTED_COMBAT:
				languageText = getUnitNotInExpectedCombat ();
				break;
				
			case UNIT_DEAD:
				languageText = getUnitDead ();
				break;
				
			case UNDAMAGED:
				languageText = getUndamaged ();
				break;
				
			case PERMANENTLY_DAMAGED:
				languageText = getPermanentlyDamaged ();
				break;
				
			case UNHEALABLE_LIFEFORM_TYPE:
				languageText = getUnhealableLifeformType ();
				break;
				
			case NOTHING_TO_DISPEL:
				languageText = getNoEnemySpellsUnit ();
				break;
				
			case NO_RANGED_ATTACK:
				languageText = getNoRangedAttack ();
				break;
				
			case INVALID_RANGED_ATTACK_TYPE:
				languageText = getInvalidRangedAttackType ();
				break;
				
			case NO_AMMUNITION:
				languageText = getNoAmmunition ();
				break;
				
			case TOO_MUCH_EXPERIENCE:
				languageText = getTooMuchExperience ();
				break;
			 
			default:
				throw new MomException ("NewTurnMessagesEx.getUnitLanguageText doesn't know what to do with enum value " + targetSpellResult);
		 }
		 
		 return languageText;
	}

	 /**
	  * @param targetSpellResult Targetting result we want language text for
	  * @return Language text for targetting at a city
	  * @throws MomException If the enum value is unknown
	  */
	public final List<LanguageText> getCityLanguageText (final TargetSpellResult targetSpellResult) throws MomException
	{
		 final List<LanguageText> languageText;
		 switch (targetSpellResult)
		 {
			case ENCHANTING_OR_HEALING_ENEMY:
				languageText = getEnchantingEnemyCity ();
				break;
				
			case CURSING_OR_ATTACKING_OWN:
				languageText = getCursingOwnCity ();
				break;
				
			case NO_SPELL_EFFECT_IDS_DEFINED:
				languageText = getNoCitySpellEffectIDsDefined ();
				break;
				
			case ALREADY_HAS_ALL_POSSIBLE_SPELL_EFFECTS:
				languageText = getAlreadyHasAllPossibleCitySpellEffects ();
				break;
				
			case CITY_ALREADY_HAS_BUILDING:
				languageText = getAlreadyHasBuilding ();
				break;
				
			case CANNOT_SEE_TARGET:
				languageText = getCannotSeeCity ();
				break;
			 
			default:
				throw new MomException ("NewTurnMessagesEx.getCityLanguageText doesn't know what to do with enum value " + targetSpellResult);
		 }
		 
		 return languageText;
	}

	 /**
	  * @param targetSpellResult Targetting result we want language text for
	  * @return Language text for targetting at a location
	  * @throws MomException If the enum value is unknown
	  */
	public final List<LanguageText> getLocationLanguageText (final TargetSpellResult targetSpellResult) throws MomException
	{
		 final List<LanguageText> languageText;
		 switch (targetSpellResult)
		 {
			case ALREADY_HAS_ALL_POSSIBLE_SPELL_EFFECTS:
				languageText = getLocationAlreadyCorrupted ();
				break;
				
			case CANNOT_SEE_TARGET:
				languageText = getCannotSeeLocation ();
				break;
				
			case INVALID_TILE_TYPE:
				languageText = getInvalidTileType ();
				break;
			 
			case NOTHING_TO_DISPEL:
				languageText = getNoEnemySpellsLocation ();
				break;
			 
			case ENEMIES_HERE:
				languageText = getEnemiesHere ();
				break;
			 
			case CELL_FULL:
				languageText = getCellFull ();
				break;
			 
			case TERRAIN_IMPASSABLE:
				languageText = getTerrainImpassable ();
				break;
			 
			default:
				throw new MomException ("NewTurnMessagesEx.getLocationLanguageText doesn't know what to do with enum value " + targetSpellResult);
		 }
		 
		 return languageText;
	}
}