package momime.common.utils;

import momime.common.MomException;
import momime.common.database.AttackSpellTargetID;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;

/**
 * Working out the kind of spell from the XML is a bunch of ad hoc rules for each spell book section, so keep them all in one
 * place so we don't have to track down everywhere that needs updating every time these rules change.
 */
public final class KindOfSpellUtilsImpl implements KindOfSpellUtils
{
	/**
	 * @param spell Spell definition
	 * @param overrideSpellBookSection Usually null; filled in when a spell is of one type, but has a specially coded secondary effect of another type
	 *		For example Wall of Fire is a city enchantment for placing it, but then when we roll for damage we have to treat it like an attack spell 
	 * @return Which kind of spell it is
	 * @throws MomException If we encounter a spell book section we don't know how to handle
	 */
	@Override
	public final KindOfSpell determineKindOfSpell (final Spell spell, final SpellBookSectionID overrideSpellBookSection)
		throws MomException
	{
		final SpellBookSectionID useSpellBookSection = (overrideSpellBookSection != null) ? overrideSpellBookSection : spell.getSpellBookSectionID ();		
		final KindOfSpell kind;
		
		switch (useSpellBookSection)
		{
			case SUMMONING:
				if (spell.getResurrectedHealthPercentage () != null)
					kind = KindOfSpell.RAISE_DEAD;
				else if (spell.getHeroItemBonusMaximumCraftingCost () != null)
					kind = KindOfSpell.CREATE_ARTIFACT;
				else
					kind = KindOfSpell.SUMMONING;
				break;
				
			case OVERLAND_ENCHANTMENTS:
				kind = KindOfSpell.OVERLAND_ENCHANTMENTS;
				break;
				
			case CITY_ENCHANTMENTS:
				kind = KindOfSpell.CITY_ENCHANTMENTS;
				break;
				
			case UNIT_ENCHANTMENTS:
				if (spell.getSummonedUnit ().size () > 0)
					kind = KindOfSpell.CHANGE_UNIT_ID;
				else
					kind = KindOfSpell.UNIT_ENCHANTMENTS;
				break;
				
			case COMBAT_ENCHANTMENTS:
				kind = KindOfSpell.COMBAT_ENCHANTMENTS;
				break;
				
			case CITY_CURSES:
				kind = KindOfSpell.CITY_CURSES;
				break;
				
			case UNIT_CURSES:
				kind = KindOfSpell.UNIT_CURSES;
				break;
				
			case ATTACK_SPELLS:
				if (spell.getSpellValidBorderTarget ().size () > 0)
					kind = KindOfSpell.ATTACK_UNITS_AND_WALLS;
				else if ((spell.getAttackSpellOverlandTarget () != null) && (spell.getAttackSpellOverlandTarget () == AttackSpellTargetID.ALL_UNITS_AND_BUILDINGS))
					kind = KindOfSpell.ATTACK_UNITS_AND_BUILDINGS;
				else
					kind = KindOfSpell.ATTACK_UNITS;
				break;
				
			case SPECIAL_UNIT_SPELLS:
				if (spell.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_PLANE_SHIFT))
					kind = KindOfSpell.PLANE_SHIFT;
				
				else if ((spell.getCombatBaseDamage () != null) || ((spell.getOverlandCastingCost () != null) &&
					(spell.getAttackSpellOverlandTarget () != null) && (spell.getAttackSpellOverlandTarget () == AttackSpellTargetID.ALL_UNITS)))
					
					kind = KindOfSpell.HEALING;
				else
					kind = KindOfSpell.RECALL;
				break;
				
			case SPECIAL_OVERLAND_SPELLS:
				if ((spell.getTileTypeID () != null) && (spell.getSpellRadius () != null))
					kind = KindOfSpell.ENCHANT_ROAD;
				else if (spell.getSpellRadius () != null)
					kind = KindOfSpell.EARTH_LORE;
				else if ((spell.getSpellValidTileTypeTarget ().size () > 0) && (spell.getSpellValidTileTypeTarget ().get (0).getChangeToTileTypeID () != null))
					kind = KindOfSpell.CHANGE_TILE_TYPE;
				else if ((spell.getSpellValidMapFeatureTarget ().size () > 0) && (spell.getSpellValidMapFeatureTarget ().get (0).getChangeToMapFeatureID () != null))
					kind = KindOfSpell.CHANGE_MAP_FEATURE;
				else if (spell.getSpellValidTileTypeTarget ().size () <= 3)
					kind = KindOfSpell.WARP_NODE;
				else
					kind = KindOfSpell.CORRUPTION;
				break;
				
			case SPECIAL_COMBAT_SPELLS:
				if (spell.getSpellValidBorderTarget ().size () > 0)
					kind = KindOfSpell.ATTACK_WALLS;
				else
					kind = KindOfSpell.EARTH_TO_MUD;
				break;
				
			case DISPEL_SPELLS:
				// Every other dispel spell has a custom slider for the dispel power; only spell binding has a fixed power (no max specified)
				if ((spell.getOverlandMaxDamage () == null) && (spell.getCombatMaxDamage () == null))
					kind = KindOfSpell.SPELL_BINDING;
				else if (spell.getAttackSpellCombatTarget () == null)
					kind = KindOfSpell.DISPEL_OVERLAND_ENCHANTMENTS;
				else
					kind = KindOfSpell.DISPEL_UNIT_CITY_COMBAT_SPELLS;
				break;
				
			case SPECIAL_SPELLS:
				kind = KindOfSpell.SPECIAL_SPELLS;
				break;
				
			case ENEMY_WIZARD_SPELLS:
				if (spell.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_SPELL_BLAST))
					kind = KindOfSpell.SPELL_BLAST;
				else
					kind = KindOfSpell.ENEMY_WIZARD_SPELLS;
				break;
				
			default:
				throw new MomException ("determineKindOfSpell does not know how to handle spell book section " + spell.getSpellBookSectionID ());
		}
		
		return kind;
	}
}