package momime.common.utils;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.database.Pick;
import momime.common.database.RangedAttackType;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Unit;
import momime.common.database.UnitSkill;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitType;
import momime.common.database.WeaponGrade;
import momime.common.messages.AvailableUnit;
import momime.common.messages.MemoryUnit;

/**
 * Stores all derived skill, upkeep and other values for a particular unit and stores them for easy and quick lookup.  
 * Build this object by calling getUnitUtils ().expandUnitDetails (), or unit tests can just mock the interface.
 */
public final class ExpandedUnitDetailsImpl implements ExpandedUnitDetails
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (ExpandedUnitDetailsImpl.class);
	
	/** The unit whose details we are storing */
	private final AvailableUnit unit;
	
	/** Definition for this unit from the XML database */
	private final Unit unitDefinition;

	/** Unit type (normal, hero or summoned) */
	private final UnitType unitType;
	
	/** Details about the player who owns the unit */
	private final PlayerPublicDetails owningPlayer;
	
	/** True magic realm/lifeform type of this unit, taking into account skills/spells that may modify the value (e.g. Chaos Channels, Undead) */
	private final Pick modifiedUnitMagicRealmLifeformType;

	/** Weapon grade this unit has, or null for summoned units and heroes */
	private final WeaponGrade weaponGrade;
	
	/** Ranged attack type this unit has, or null if it has none */
	private final RangedAttackType rangedAttackType;

	/** Experience level of this unit (0-5 for regular units, 0-8 for heroes) excluding bonuses from Warlord/Crusade; for units that don't gain experience (e.g. summoned), returns null */
	private final ExperienceLevel basicExperienceLevel;

	/** Experience level of this unit (0-5 for regular units, 0-8 for heroes) including bonuses from Warlord/Crusade; for units that don't gain experience (e.g. summoned), returns null */
	private final ExperienceLevel modifiedExperienceLevel;
	
	/**
	 * Basic skill values, including any granted from spells, hero items, or any granted from other skills, including any that may get negated even by our own skills.
	 * e.g. if a unit has Invulnerability, Resist Elements and Elemental Armour spells all cast on it, this list will contain
	 * Invulnerability, Weapon Immunity, Resist Elements and Elemental Armour.
	 * This used to display the unit's skills in the unit info panel in the UI, so we need to display all skills granted from spells so they can be clicked on to cancel them.
	 * 
	 * Many skills (such as movement skills) do not have a value, the unit just has the skill, with a null value.
	 * As such you cannot do (basicSkillValues.get ("US000") != null) to test whether a unit has a particular skill or not.  You must use (basicSkillValues.containsKey ("US000")).
	 * 
	 * Also this only lists basic skill values - if a unit has a melee attack of 4, is experienced and has flame blade cast on it, the value in the map will still just read "4".
	 * To get the true unit skill value, with all modifiers and penalties applied, use the modifiedSkillValues map instead.
	 */
	private final Map<String, Integer> basicSkillValues;
	
	/** Modified skill values, broken down into their individual components; valueless skills will just have a null in the outer map */
	private final Map<String, Map<UnitSkillComponent, Integer>> modifiedSkillValues;
	
	/** Base upkeep values, before any reductions such as the Summoner retort reducing upkeep for summoned units; cannot have null values in here */
	private final Map<String, Integer> basicUpkeepValues;

	/** Upkeep values, modified by reductions such as the Summoner retort reducing upkeep for summoned units; cannot have null values in here */
	private final Map<String, Integer> modifiedUpkeepValues;
	
	/**
	 * @param aUnit The unit whose details we are storing
	 * @param aUnitDefinition Definition for this unit from the XML database
	 * @param aUnitType Unit type (normal, hero or summoned)
	 * @param anOwningPlayer Details about the player who owns the unit
	 * @param aModifiedUnitMagicRealmLifeformType True magic realm/lifeform type of this unit, taking into account skills/spells that may modify the value (e.g. Chaos Channels, Undead)
	 * @param aWeaponGrade Weapon grade this unit has, or null for summoned units and heroes
	 * @param aRangedAttackType Ranged attack type this unit has, or null if it has none
	 * @param aBasicExpLvl Experience level of this unit (0-5 for regular units, 0-8 for heroes) excluding bonuses from Warlord/Crusade; null for units that don't gain experience (e.g. summoned)
	 * @param aModifiedExpLvl Experience level of this unit (0-5 for regular units, 0-8 for heroes) including bonuses from Warlord/Crusade; null for units that don't gain experience (e.g. summoned)
	 * @param aBasicSkillValues Calculated basic skill map
	 * @param aModifiedSkillValues Modified skill values, broken down into their individual components; valueless skills will just have a null in the outer map
	 * @param aBasicUpkeepValues Base upkeep values, before any reductions such as the Summoner retort reducing upkeep for summoned units; cannot have null values in here
	 * @param aModifiedUpkeepValues Upkeep values, modified by reductions such as the Summoner retort reducing upkeep for summoned units; cannot have null values in here
	 */
	public ExpandedUnitDetailsImpl (final AvailableUnit aUnit, final Unit aUnitDefinition, final UnitType aUnitType, final PlayerPublicDetails anOwningPlayer,
		final Pick aModifiedUnitMagicRealmLifeformType, final WeaponGrade aWeaponGrade, final RangedAttackType aRangedAttackType,
		final ExperienceLevel aBasicExpLvl, final ExperienceLevel aModifiedExpLvl,
		final Map<String, Integer> aBasicSkillValues, final Map<String, Map<UnitSkillComponent, Integer>> aModifiedSkillValues,
		final Map<String, Integer> aBasicUpkeepValues, final Map<String, Integer> aModifiedUpkeepValues)
	{
		unit = aUnit;
		unitDefinition = aUnitDefinition;
		unitType = aUnitType;
		owningPlayer = anOwningPlayer;
		modifiedUnitMagicRealmLifeformType = aModifiedUnitMagicRealmLifeformType;
		weaponGrade = aWeaponGrade;
		rangedAttackType = aRangedAttackType;
		basicExperienceLevel = aBasicExpLvl;
		modifiedExperienceLevel = aModifiedExpLvl;
		basicSkillValues = aBasicSkillValues;
		modifiedSkillValues = aModifiedSkillValues;
		basicUpkeepValues = aBasicUpkeepValues;
		modifiedUpkeepValues = aModifiedUpkeepValues;
	}
	
	/**
	 * @return The unit whose details we are storing
	 */
	@Override
	public final AvailableUnit getUnit ()
	{
		return unit;
	}

	/**
	 * @return Whether this is a MemoryUnit or not 
	 */
	@Override
	public final boolean isMemoryUnit ()
	{
		return (unit instanceof MemoryUnit);
	}
	
	/**
	 * @return The unit whose details we are storing if it is a MemoryUnit; null if it is an AvailableUnit 
	 */
	@Override
	public final MemoryUnit getMemoryUnit ()
	{
		return (unit instanceof MemoryUnit) ? ((MemoryUnit) unit) : null;
	}

	/**
	 * @return Definition for this unit from the XML database
	 */
	@Override
	public final Unit getUnitDefinition ()
	{
		return unitDefinition;
	}

	/**
	 * @return Unit type (normal, hero or summoned)
	 */
	@Override
	public final UnitType getUnitType ()
	{
		return unitType;
	}
	
	/**
	 * @return Details about the player who owns the unit
	 */
	@Override
	public final PlayerPublicDetails getOwningPlayer ()
	{
		return owningPlayer;
	}
	
	/**
	 * @return True magic realm/lifeform type of this unit, taking into account skills/spells that may modify the value (e.g. Chaos Channels, Undead)
	 */
	@Override
	public final Pick getModifiedUnitMagicRealmLifeformType ()
	{
		return modifiedUnitMagicRealmLifeformType;
	}
	
	/**
	 * @return Weapon grade this unit has, or null for summoned units and heroes
	 */
	@Override
	public final WeaponGrade getWeaponGrade ()
	{
		return weaponGrade;
	}
	
	/**
	 * @return Ranged attack type this unit has, or null if it has none
	 */
	@Override
	public final RangedAttackType getRangedAttackType ()
	{
		return rangedAttackType;
	}
	
	/**
	 * @return Experience level of this unit (0-5 for regular units, 0-8 for heroes) excluding bonuses from Warlord/Crusade; for units that don't gain experience (e.g. summoned), returns null
	 */
	@Override
	public final ExperienceLevel getBasicExperienceLevel ()
	{
		return basicExperienceLevel;
	}

	/**
	 * @return Experience level of this unit (0-5 for regular units, 0-8 for heroes) including bonuses from Warlord/Crusade; for units that don't gain experience (e.g. summoned), returns null
	 */
	@Override
	public final ExperienceLevel getModifiedExperienceLevel ()
	{
		return modifiedExperienceLevel;
	}
	
	/**
	 * @param unitSkillID Unit skill ID to check
	 * @return Whether or not the unit has this skill, prior to negations
	 */
	@Override
	public final boolean hasBasicSkill (final String unitSkillID)
	{
		return basicSkillValues.containsKey (unitSkillID);
	}

	/**
	 * @param unitSkillID Unit skill ID to check
	 * @return Basic unmodified value of this skill, or null for valueless skills such as movement skills
	 * @throws MomException If we call this on a skill that the unit does not have - must verify that the unit has the skill first by calling hasBasicSkill ()
	 */
	@Override
	public final Integer getBasicSkillValue (final String unitSkillID) throws MomException
	{
		if (!hasBasicSkill (unitSkillID))
			throw new MomException ("getBasicSkillValue called on " + getUnit ().getUnitID () + " skill ID " + unitSkillID + " but the unit does not have this skill");
		
		return basicSkillValues.get (unitSkillID);
	}

	/**
	 * @param unitSkillID Unit skill ID to check
	 * @return Whether or not the unit has this skill, after negations
	 */
	@Override
	public final boolean hasModifiedSkill (final String unitSkillID)
	{
		return modifiedSkillValues.containsKey (unitSkillID);
	}
	
	/**
	 * This totals across all the breakdown components.  This is the only value the server is ever interested in.
	 * 
	 * @param unitSkillID Unit skill ID to check
	 * @return Modified value of this skill, or null for valueless skills such as movement skills
	 * @throws MomException If we call this on a skill that the unit does not have - must verify that the unit has the skill first by calling hasBasicSkill (); also if it has any null components
	 */
	@Override
	public final Integer getModifiedSkillValue (final String unitSkillID) throws MomException
	{
		if (!hasModifiedSkill (unitSkillID))
			throw new MomException ("getModifiedSkillValue called on " + getUnit ().getUnitID () + " skill ID " + unitSkillID + " but the unit does not have this skill");

		final Map<UnitSkillComponent, Integer> components = modifiedSkillValues.get (unitSkillID);
		Integer total;
		if ((components == null) || (components.isEmpty ()))
		{
			// Exception here is if this is the +to hit or +to defend skill and we have no modifiers whatsoever, we must still return 0
			total = ((unitSkillID.equals (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT)) || (unitSkillID.equals (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK)))
				? 0 : null;
		}
		else
		{
			total = 0;
			for (final Entry<UnitSkillComponent, Integer> c : components.entrySet ())
				if (c.getValue () == null)
					throw new MomException ("getModifiedSkillValue called on " + getUnit ().getUnitID () + " skill ID " + unitSkillID + " but the " + c.getKey () + " component is null");
				else
					total = total + c.getValue ();
		}
		
		return total;
	}

	/**
	 * @param productionTypeID Production type we want to look up the base upkeep for
	 * @return Base upkeep value, before any reductions such as the Summoner retort reducing upkeep for summoned units; 0 if this unit has no upkeep of this type
	 */
	@Override
	public final int getBasicUpkeepValue (final String productionTypeID)
	{
		final Integer v = basicUpkeepValues.get (productionTypeID);
		return (v == null) ? 0 : v;
	}

	/**
	 * @param productionTypeID Production type we want to look up the modified upkeep for
	 * @return Upkeep value, modified by reductions such as the Summoner retort reducing upkeep for summoned units; 0 if this unit has no upkeep of this type
	 */
	@Override
	public final int getModifiedUpkeepValue (final String productionTypeID)
	{
		final Integer v = modifiedUpkeepValues.get (productionTypeID);
		return (v == null) ? 0 : v;
	}

	/**
	 * @return Number of figures in this unit before it takes any damage
	 */
	@Override
	public final int getFullFigureCount ()
	{
		final int countAccordingToRecord = getUnitDefinition ().getFigureCount ();

        // Fudge until we do Hydras properly with a 'Figures-as-heads' skill
		final int realCount;
        if (countAccordingToRecord == 9)
        	realCount = 1;
       	else
       		realCount = countAccordingToRecord;

		return realCount;
	}

	/**
	 * @param db Lookup lists built over the XML database
	 * @return True if the unit has a skill with the "ignoreCombatTerrain" flag
	 * @throws RecordNotFoundException If one of the unit skills is not found in the database
	 */
	@Override
	public final boolean unitIgnoresCombatTerrain (final CommonDatabase db) throws RecordNotFoundException
	{
		log.trace ("Entering unitIgnoresCombatTerrain" + (isMemoryUnit () ? (": Unit URN " + getMemoryUnit ().getUnitURN ()) : ""));

		boolean found = false;

		final Iterator<String> iter = basicSkillValues.keySet ().iterator ();
		while ((!found) && (iter.hasNext ()))
		{
			final UnitSkill skillDef = db.findUnitSkill (iter.next (), "unitIgnoresCombatTerrain");
			if ((skillDef.isIgnoreCombatTerrain () != null) && (skillDef.isIgnoreCombatTerrain ()))
				found = true;
		}

		log.trace ("Exiting unitIgnoresCombatTerrain = " + found);
		return found;
	}
	
	/**
	 * @return String representation of all class values, for debug purposes
	 */
	@Override
	public final String toString ()
	{
		final StringBuilder s = new StringBuilder ();
		s.append ("[UnitID=\"" + getUnit ().getUnitID () + "\", ");
		
		if (isMemoryUnit ())
			s.append ("UnitURN=" + getMemoryUnit ().getUnitURN () + ", ");
		
		s.append ("UnitTypeID=\"" + getUnitType ().getUnitTypeID () + "\", ");
		s.append ("LifeformTypeID=\"" + getModifiedUnitMagicRealmLifeformType ().getPickID () + "\", ");
		s.append ("PlayerID=" + getUnit ().getOwningPlayerID () + ", ");
		s.append ("Location=" + getUnit ().getUnitLocation () + ", ");
		s.append ("WeaponGrade=" + getUnit ().getWeaponGrade () + ", ");
		s.append ("RAT=" + getUnitDefinition ().getRangedAttackType () + ", ");
		
		if (getBasicExperienceLevel () != null)
			s.append ("BasicExpLvl=" + getBasicExperienceLevel ().getLevelNumber () + ", ");

		if (getModifiedExperienceLevel () != null)
			s.append ("ModExpLvl=" + getModifiedExperienceLevel ().getLevelNumber () + ", ");
		
		// Different sets of skill values
		final StringBuilder raw = new StringBuilder ();
		getUnit ().getUnitHasSkill ().stream ().forEach (k ->
		{
			if (raw.length () > 0)
				raw.append (",");
			
			raw.append (k.getUnitSkillID ());
			if (k.getUnitSkillValue () != null)
				raw.append ("=" + k.getUnitSkillValue ());
		});	
		
		final StringBuilder basic = new StringBuilder ();
		basicSkillValues.forEach ((k, v) ->
		{
			if (basic.length () > 0)
				basic.append (",");
			
			basic.append (k);
			if (v != null)
				basic.append ("=" + v);
		});	
		
		final StringBuilder mod = new StringBuilder ();
		modifiedSkillValues.forEach ((k, components) ->
		{
			if (mod.length () > 0)
				mod.append (",");
			
			mod.append (k);
			if ((components != null) && (!components.isEmpty ()))
				mod.append ("=" + components.values ().stream ().mapToInt (v -> v).sum ());
		});	
		
		// Finish off
		s.append ("Raw=(" + raw + "), Basic=(" + basic + "), Mod=(" + mod + ")]");
		return s.toString ();
	}
}