package momime.common.utils;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DamageType;
import momime.common.database.ExperienceLevel;
import momime.common.database.Pick;
import momime.common.database.RangedAttackTypeEx;
import momime.common.database.UnitEx;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillPositiveNegative;
import momime.common.database.UnitType;
import momime.common.database.WeaponGrade;
import momime.common.messages.AvailableUnit;

/**
 * Stores all derived skill, upkeep and other values for a particular unit and stores them for easy and quick lookup.  
 * Build this object by calling getUnitUtils ().expandUnitDetails (), or unit tests can just mock the interface.
 */
public final class ExpandedUnitDetailsImpl extends MinimalUnitDetailsImpl implements ExpandedUnitDetails
{
	/** True magic realm/lifeform type of this unit, taking into account skills/spells that may modify the value (e.g. Chaos Channels, Undead) */
	private final Pick modifiedUnitMagicRealmLifeformType;

	/** Weapon grade this unit has, or null for summoned units and heroes */
	private final WeaponGrade weaponGrade;
	
	/** Ranged attack type this unit has, or null if it has none */
	private final RangedAttackTypeEx rangedAttackType;
	
	/** Modified skill values, broken down into their individual components; valueless skills will just have a null in the outer map */
	private final Map<String, Map<UnitSkillComponent, Integer>> modifiedSkillValues;
	
	/** Base upkeep values, before any reductions such as the Summoner retort reducing upkeep for summoned units; cannot have null values in here */
	private final Map<String, Integer> basicUpkeepValues;

	/** Upkeep values, modified by reductions such as the Summoner retort reducing upkeep for summoned units; cannot have null values in here */
	private final Map<String, Integer> modifiedUpkeepValues;
	
	/** PlayerID of the player who currently controls the unit; may not be owningPlayerID if unit has confusion cast on it */
	private final int controllingPlayerID;
	
	/** Unit utils */
	private final UnitUtils unitUtils;
	
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
	 * @param aControllingPlayerID PlayerID of the player who currently controls the unit; may not be owningPlayerID if unit has confusion cast on it
	 * @param aBasicSkillValues Calculated basic skill map
	 * @param aModifiedSkillValues Modified skill values, broken down into their individual components; valueless skills will just have a null in the outer map
	 * @param aBasicUpkeepValues Base upkeep values, before any reductions such as the Summoner retort reducing upkeep for summoned units; cannot have null values in here
	 * @param aModifiedUpkeepValues Upkeep values, modified by reductions such as the Summoner retort reducing upkeep for summoned units; cannot have null values in here
	 * @param aUnitUtils Unit utils
	 */
	public ExpandedUnitDetailsImpl (final AvailableUnit aUnit, final UnitEx aUnitDefinition, final UnitType aUnitType, final PlayerPublicDetails anOwningPlayer,
		final Pick aModifiedUnitMagicRealmLifeformType, final WeaponGrade aWeaponGrade, final RangedAttackTypeEx aRangedAttackType,
		final ExperienceLevel aBasicExpLvl, final ExperienceLevel aModifiedExpLvl, final int aControllingPlayerID,
		final Map<String, Integer> aBasicSkillValues, final Map<String, Map<UnitSkillComponent, Integer>> aModifiedSkillValues,
		final Map<String, Integer> aBasicUpkeepValues, final Map<String, Integer> aModifiedUpkeepValues, final UnitUtils aUnitUtils)
	{
		super (aUnit, aUnitDefinition, aUnitType, anOwningPlayer, aBasicExpLvl, aModifiedExpLvl, aBasicSkillValues);
		
		controllingPlayerID = aControllingPlayerID;
		modifiedUnitMagicRealmLifeformType = aModifiedUnitMagicRealmLifeformType;
		weaponGrade = aWeaponGrade;
		rangedAttackType = aRangedAttackType;
		modifiedSkillValues = aModifiedSkillValues;
		basicUpkeepValues = aBasicUpkeepValues;
		modifiedUpkeepValues = aModifiedUpkeepValues;
		unitUtils = aUnitUtils;
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
	public final RangedAttackTypeEx getRangedAttackType ()
	{
		return rangedAttackType;
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
	 * @throws MomException If we call this on a skill that the unit does not have - must verify that the unit has the skill first by calling hasModifiedSkill (); also if it has any null components
	 */
	@Override
	public final Integer getModifiedSkillValue (final String unitSkillID) throws MomException
	{
		return filterModifiedSkillValue (unitSkillID, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH);
	}

	/**
	 * Filters only specific breakdown components, for displaying attributes in the unit info panel where we want to colour the skill icons
	 * differently according to their component, and shading out negated skill points.
	 * 
	 * @param unitSkillID Unit skill ID to check
	 * @param component Which component(s) to include in the total
	 * @param positiveNegative Whether to only include positive effects, only negative effects, or both
	 * @return Portion of the modified value of this skill that matches the requested filters; 0 for valued skills where no filters matched; null for valueless skills such as movement skills
	 * @throws MomException If we call this on a skill that the unit does not have - must verify that the unit has the skill first by calling hasModifiedSkill (); also if it has any null components
	 */
	@Override
	public final Integer filterModifiedSkillValue (final String unitSkillID, final UnitSkillComponent component, final UnitSkillPositiveNegative positiveNegative) throws MomException
	{
		if (!hasModifiedSkill (unitSkillID))
			throw new MomException ("filterModifiedSkillValue called on " + getUnitID () + " skill ID " + unitSkillID + " but the unit does not have this skill");

		final Map<UnitSkillComponent, Integer> components = modifiedSkillValues.get (unitSkillID);
		Integer total;
		if ((components == null) || (components.isEmpty ()))
		{
			// Exception here is if this is the experience skill and we're still at 0, it'll be an empty list but we must still return 0 not a null
			total = (unitSkillID.equals (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE)) ? 0 : null;
		}
		else
		{
			total = 0;
			for (final Entry<UnitSkillComponent, Integer> c : components.entrySet ())
				if (c.getValue () == null)
					throw new MomException ("filterModifiedSkillValue called on " + getUnitID () + " skill ID " + unitSkillID + " but the " + c.getKey () + " component is null");
				else if (((component == UnitSkillComponent.ALL) || (component  == c.getKey ())) &&
					((positiveNegative == UnitSkillPositiveNegative.BOTH) ||
					((positiveNegative == UnitSkillPositiveNegative.POSITIVE) && (c.getValue () > 0)) ||
					((positiveNegative == UnitSkillPositiveNegative.NEGATIVE) && (c.getValue () < 0))))
					
					total = total + c.getValue ();
		}
		
		return total;
	}
	
	/**
	 * @return Set of all modified skills this unit has
	 */
	@Override
	public final Set<String> listModifiedSkillIDs ()
	{
		return modifiedSkillValues.keySet ();
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
	 * @return Set of all basic upkeeps this unit has
	 */
	@Override
	public final Set<String> listBasicUpkeepProductionTypeIDs ()
	{
		return basicUpkeepValues.keySet ();
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
	 * @return Set of all modified upkeeps this unit has
	 */
	@Override
	public Set<String> listModifiedUpkeepProductionTypeIDs ()
	{
		return modifiedUpkeepValues.keySet ();
	}

	/**
	 * @return Total damage taken by this unit across all types
	 * @throws MomException Won't happen, since we return 0 for AvailableUnits 
	 */
	@Override
	public final int getTotalDamageTaken () throws MomException
	{
		return isMemoryUnit () ? getUnitUtils ().getTotalDamageTaken (getUnitDamage ()) : 0;
	}	

	/**
	 * @return How many hit points the unit as a whole has left
	 * @throws MomException If we hit any problems reading unit skill values
	 */
	@Override
	public final int calculateHitPointsRemaining () throws MomException
	{
		final int result = (getFullFigureCount () * getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS)) - getTotalDamageTaken ();
		return result;
	}
	
	/**
	 * First figure will take full damage before the second figure takes any damage
	 * 
	 * @return Number of figures left alive in this unit
	 * @throws MomException If we hit any problems reading unit skill values
	 */
	@Override
	public final int calculateAliveFigureCount () throws MomException
	{
		int figures = getFullFigureCount () -
				
			// Take off 1 for each full set of HP the unit has taken in damage
			(getTotalDamageTaken () / getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS));
		
		// Protect against weird results
		if (figures < 0)
			figures = 0;
		
		return figures;
	}
	
	/**
	 * @return How many hit points the first figure in this unit has left
	 * @throws MomException If we hit any problems reading unit skill values
	 */
	@Override
	public final int calculateHitPointsRemainingOfFirstFigure () throws MomException
	{
		final int hitPointsPerFigure = getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS);
		
		// Work out how much damage the first figure has taken
		final int firstFigureDamageTaken = getTotalDamageTaken () % hitPointsPerFigure;
		
		// Then from that work out how many hit points the first figure has left
		final int result = hitPointsPerFigure - firstFigureDamageTaken;
		
		return result;
	}
	
	/**
	 * @param damageType Type of damage they are being hit by
	 * @return Whether or not the unit is completely immune to this type of damage - so getting a boost to e.g. 50 shields still returns false
	 */
	@Override
	public final boolean isUnitImmuneToDamageType (final DamageType damageType)
	{
    	// We only want complete immunities - even if it boots defence to 50, its still a valid target
    	final boolean immunity = damageType.getDamageTypeImmunity ().stream ().anyMatch
    		(i -> (i.getBoostsDefenceTo () == null) && (hasModifiedSkill (i.getUnitSkillID ())));
		
		return immunity;
	}

	/**
	 * @return How much ranged ammo this unit has when fully loaded
	 * @throws MomException If we call this on a skill that the unit does not have - must verify that the unit has the skill first by calling hasBasicSkill (); also if it has any null components
	 */
	@Override
	public final int calculateFullRangedAttackAmmo () throws MomException
	{
		return hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_RANGED_ATTACK_AMMO) ?
			getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_RANGED_ATTACK_AMMO) : 0;
	}

	/**
	 * @return How much mana the unit has total, before any is spent in combat
	 * @throws MomException If we call this on a skill that the unit does not have - must verify that the unit has the skill first by calling hasBasicSkill (); also if it has any null components
	 */
	@Override
	public final int calculateManaTotal () throws MomException
	{
		// Unit caster skill is easy, this directly says how many MP the unit has
		int total = hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_UNIT) ?
			getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_UNIT) : 0;
		
		// The hero caster skill is a bit more of a pain, since we get more mana at higher experience levels
		if (hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_HERO))
		{
			final int expLevel = getModifiedExperienceLevel ().getLevelNumber ();
			final int heroSkillValue = (getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_HERO) * 5 * (expLevel+1)) / 2;
			total = total + heroSkillValue;
		}
		
		return total;
	}
	
	/**
	 * @return PlayerID of the player who currently controls the unit; may not be owningPlayerID if unit has confusion cast on it 
	 */
	@Override
	public final int getControllingPlayerID ()
	{
		return controllingPlayerID;
	}

	/**
	 * @return Movement speed calculated for this unit (just a shortcut to reading this particular skill value)
	 * @throws MomException If we call this on a skill that the unit does not have - must verify that the unit has the skill first by calling hasModifiedSkill (); also if it has any null components
	 */
	@Override
	public final int getMovementSpeed () throws MomException
	{
		return getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED);
	}
	
	/**
	 * @return String representation of all class values, for debug purposes
	 */
	@Override
	public final String toString ()
	{
		final StringBuilder s = new StringBuilder ();
		s.append ("[" + getDebugIdentifier () + ", ");		
		s.append ("UnitTypeID=\"" + getUnitType ().getUnitTypeID () + "\", ");
		s.append ("LifeformTypeID=\"" + getModifiedUnitMagicRealmLifeformType ().getPickID () + "\", ");
		s.append ("PlayerID=" + getOwningPlayerID () + ", ");
		s.append ("Location=" + getUnitLocation () + ", ");
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

	/**
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}
}