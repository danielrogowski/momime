package momime.common.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitCombatSideID;
import momime.common.database.UnitEx;
import momime.common.database.UnitSkill;
import momime.common.database.UnitSpecialOrder;
import momime.common.database.UnitType;
import momime.common.messages.AvailableUnit;
import momime.common.messages.MemoryUnit;
import momime.common.messages.UnitDamage;
import momime.common.messages.UnitStatusID;

/**
 * Stores minimal unit details that can be derived quickly without examining the whole unit stack.
 * This is mainly here to calculate experience levels for Leadership skill which must be known prior to calculating full stats for any unit in the stack.
 */
public class MinimalUnitDetailsImpl implements MinimalUnitDetails
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (MinimalUnitDetailsImpl.class);
	
	/** The unit whose details we are storing */
	private final AvailableUnit unit;
	
	/** Definition for this unit from the XML database */
	private final UnitEx unitDefinition;

	/** Unit type (normal, hero or summoned) */
	private final UnitType unitType;
	
	/** Details about the player who owns the unit */
	private final PlayerPublicDetails owningPlayer;
	
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
	protected final Map<String, Integer> basicSkillValues;
	
	/**
	 * @param aUnit The unit whose details we are storing
	 * @param aUnitDefinition Definition for this unit from the XML database
	 * @param aUnitType Unit type (normal, hero or summoned)
	 * @param anOwningPlayer Details about the player who owns the unit
	 * @param aBasicExpLvl Experience level of this unit (0-5 for regular units, 0-8 for heroes) excluding bonuses from Warlord/Crusade; null for units that don't gain experience (e.g. summoned)
	 * @param aModifiedExpLvl Experience level of this unit (0-5 for regular units, 0-8 for heroes) including bonuses from Warlord/Crusade; null for units that don't gain experience (e.g. summoned)
	 * @param aBasicSkillValues Calculated basic skill map
	 */
	public MinimalUnitDetailsImpl (final AvailableUnit aUnit, final UnitEx aUnitDefinition, final UnitType aUnitType, final PlayerPublicDetails anOwningPlayer,
		final ExperienceLevel aBasicExpLvl, final ExperienceLevel aModifiedExpLvl, final Map<String, Integer> aBasicSkillValues)
	{
		unit = aUnit;
		unitDefinition = aUnitDefinition;
		unitType = aUnitType;
		owningPlayer = anOwningPlayer;
		basicExperienceLevel = aBasicExpLvl;
		modifiedExperienceLevel = aModifiedExpLvl;
		basicSkillValues = aBasicSkillValues;
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
	 * @return The unit whose details we are storing
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	@Override
	public final MemoryUnit getMemoryUnit () throws MomException
	{
		if (!isMemoryUnit ())
			throw new MomException ("ExpandedUnitDetails is storing a " + unit.getClass ().getName () + "; cannot typecast it to MemoryUnit");
		
		return (MemoryUnit) unit;
	}

	/**
	 * @return Definition for this unit from the XML database
	 */
	@Override
	public final UnitEx getUnitDefinition ()
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
	 * @return Whether or not the unit is a hero
	 */
	@Override
	public final boolean isHero ()
	{
		return getUnitDefinition ().getUnitMagicRealm ().equals (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
	}
	
	/**
	 * @return Whether or not the unit is a summoned creature
	 */
	@Override
	public final boolean isSummoned ()
	{
		return getUnitType ().getUnitTypeID ().equals (CommonDatabaseConstants.UNIT_TYPE_ID_SUMMONED);
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
			throw new MomException ("getBasicSkillValue called on " + getUnitID () + " skill ID " + unitSkillID + " but the unit does not have this skill");
		
		return basicSkillValues.get (unitSkillID);
	}

	/**
	 * @param unitSkillID Unit skill ID to check
	 * @return Basic unmodified value of this skill, or null for valueless skills such as movement skills; if unit is a hero then skill is multiplied up by their level
	 * @throws MomException If we call this on a skill that the unit does not have - must verify that the unit has the skill first by calling hasBasicSkill ()
	 */
	@Override
	public final Integer getBasicOrHeroSkillValue (final String unitSkillID) throws MomException
	{
		final Integer basicValue = getBasicSkillValue (unitSkillID);
		final Integer multipliedValue;
		if ((basicValue == null) || (!isHero ()))
			multipliedValue = basicValue;
		else
			// For heroes, basicValue = 1 for normal skill and 2 for super skill
			multipliedValue = (basicValue + 1) * (getModifiedExperienceLevel ().getLevelNumber () + 1);
		
		return multipliedValue;
	}
	
	/**
	 * @return Set of all basic skills this unit has
	 */
	@Override
	public final Set<String> listBasicSkillIDs ()
	{
		return basicSkillValues.keySet ();
	}
	
	/**
	 * @return Map of all basic skill values keyed by skill IDs
	 */
	@Override
	public final Map<String, Integer> getBasicSkillValues ()
	{
		// Return a copy of the map, so the caller can't get access to modify the real skill values
		return new HashMap<String, Integer> (basicSkillValues);
	}
	
	/**
	 * @return Number of figures in this unit before it takes any damage
	 */
	@Override
	public final int getFullFigureCount ()
	{
		return getUnitDefinition ().getFigureCount ();
	}

	/**
	 * @param db Lookup lists built over the XML database
	 * @return True if the unit has a skill with the "ignoreCombatTerrain" flag
	 * @throws RecordNotFoundException If one of the unit skills is not found in the database
	 */
	@Override
	public final boolean unitIgnoresCombatTerrain (final CommonDatabase db) throws RecordNotFoundException
	{
		boolean found = false;

		final Iterator<String> iter = basicSkillValues.keySet ().iterator ();
		while ((!found) && (iter.hasNext ()))
		{
			final UnitSkill skillDef = db.findUnitSkill (iter.next (), "unitIgnoresCombatTerrain");
			if ((skillDef.isIgnoreCombatTerrain () != null) && (skillDef.isIgnoreCombatTerrain ()))
				found = true;
		}

		return found;
	}

	/**
	 * @return How much fame a player loses when this unit dies
	 */
	@Override
	public final int calculateFameLostForUnitDying ()
	{
		int fame = 0;
		
		if (isHero ())
			fame = (getModifiedExperienceLevel ().getLevelNumber () + 1) / 2;
		
		return fame;
	}
	
	/**
	 * @return Map of all upkeep values keyed by production type IDs
	 */
	@Override
	public final Map<String, Integer> getBasicUpeepValues ()
	{
		return getUnitDefinition ().getUnitUpkeep ().stream ().collect
			(Collectors.toMap (u -> u.getProductionTypeID (), u -> u.getUndoubledProductionValue ()));
	}
	
	/**
	 * @return String representation of all class values, for debug purposes
	 */
	@Override
	public String toString ()
	{
		final StringBuilder s = new StringBuilder ();
		s.append ("[" + getDebugIdentifier () + ", ");		
		s.append ("UnitTypeID=\"" + getUnitType ().getUnitTypeID () + "\", ");
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
		
		// Finish off
		s.append ("Raw=(" + raw + "), Basic=(" + basic + ")]");
		return s.toString ();
	}

	/**
	 * @return String identifiying this unit, suitable for including in debug messages
	 */
	@Override
	public final String getDebugIdentifier ()
	{
		final StringBuilder s = new StringBuilder ();
		s.append ("UnitID=\"" + getUnitID ());
		
		if (isMemoryUnit ())
			try
			{
				s.append (", UnitURN=" + getUnitURN ());
			}
			catch (final MomException e)
			{
				log.error (e, e);
			}
		
		return s.toString ();
	}
	
	// Properties that directly delegate to methods on AvailableUnit
	
	/**
	 * @return Unit definition identifier, e.g. UN001
	 */
	@Override
	public final String getUnitID ()
	{
		return getUnit ().getUnitID ();
	}

	/**
	 * @return PlayerID of the player who owns this unit 
	 */
	@Override
	public final int getOwningPlayerID ()
	{
		return getUnit ().getOwningPlayerID ();
	}

	/**
	 * @return Location of the unit on the overland map
	 */
	@Override
	public final MapCoordinates3DEx getUnitLocation ()
	{
		return (MapCoordinates3DEx) getUnit ().getUnitLocation ();
	}
	
	// Properties that directly delegate to methods on MemoryUnit
	
	/**
	 * @return Unit URN
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	@Override
	public final int getUnitURN () throws MomException
	{
		return getMemoryUnit ().getUnitURN ();
	}

	/**
	 * @return Current status of this unit
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	@Override
	public final UnitStatusID getStatus () throws MomException
	{
		return getMemoryUnit ().getStatus ();
	}
	
	/**
	 * @return Location on the overland map of the combat this unit is involved in null if the unit isn't currently in combat
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	@Override
	public final MapCoordinates3DEx getCombatLocation () throws MomException
	{
		return (MapCoordinates3DEx) getMemoryUnit ().getCombatLocation ();
	}

	/**
	 * @param coords Location on the overland map of the combat this unit is involved in; null if the unit isn't currently in combat
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	@Override
	public final void setCombatLocation (final MapCoordinates3DEx coords) throws MomException
	{
		getMemoryUnit ().setCombatLocation (coords);
	}
	
	/**
	 * @return Location within the combat map where this unit is standing null if the unit isn't currently in combat
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	@Override
	public final MapCoordinates2DEx getCombatPosition () throws MomException
	{
		return (MapCoordinates2DEx) getMemoryUnit ().getCombatPosition ();
	}

	/**
	 * @param coords Location within the combat map where this unit is standing; null if the unit isn't currently in combat
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	@Override
	public final void setCombatPosition (final MapCoordinates2DEx coords) throws MomException
	{
		getMemoryUnit ().setCombatPosition (coords);
	}
	
	/**
	 * @return Direction within the combat map that the unit is facing null if the unit isn't currently in combat
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	@Override
	public final Integer getCombatHeading () throws MomException
	{
		return getMemoryUnit ().getCombatHeading ();
	}

	/**
	 * @param heading Direction within the combat map that the unit is facing; null if the unit isn't currently in combat
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	@Override
	public final void setCombatHeading (final Integer heading) throws MomException
	{
		getMemoryUnit ().setCombatHeading (heading);
	}
	
	/**
	 * @return Whether the unit is part of the attacking or defending side in combat null if the unit isn't currently in combat
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	@Override
	public final UnitCombatSideID getCombatSide () throws MomException
	{
		return getMemoryUnit ().getCombatSide ();
	}

	/**
	 * @param side Whether the unit is part of the attacking or defending side in combat; null if the unit isn't currently in combat
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	@Override
	public final void setCombatSide (final UnitCombatSideID side) throws MomException
	{
		getMemoryUnit ().setCombatSide (side);
	}
	
	/**
	 * @return The number of moves remaining for this unit this combat turn; null if the unit isn't currently in combat
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	@Override
	public final Integer getDoubleCombatMovesLeft () throws MomException
	{
		return getMemoryUnit ().getDoubleCombatMovesLeft ();
	}
	
	/**
	 * @param moves The number of moves remaining for this unit this combat turn; null if the unit isn't currently in combat
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	@Override
	public final void setDoubleCombatMovesLeft (final Integer moves) throws MomException
	{
		getMemoryUnit ().setDoubleCombatMovesLeft (moves);
	}
	
	/**
	 * @return The number of moves remaining for this unit this overland turn
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	@Override
	public final int getDoubleOverlandMovesLeft () throws MomException
	{
		return getMemoryUnit ().getDoubleOverlandMovesLeft ();
	}

	/**
	 * @param moves The number of moves remaining for this unit this overland turn
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	@Override
	public final void setDoubleOverlandMovesLeft (final int moves) throws MomException
	{
		getMemoryUnit ().setDoubleOverlandMovesLeft (moves);
	}
	
	/**
	 * @return The number of ranged shots this unit can still fire in the current combat
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	@Override
	public final int getAmmoRemaining () throws MomException
	{
		return getMemoryUnit ().getAmmoRemaining ();
	}
	
	/**
	 * @param ammo The number of ranged shots this unit can still fire in the current combat
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	@Override
	public final void setAmmoRemaining (final int ammo) throws MomException
	{
		getMemoryUnit ().setAmmoRemaining (ammo);
	}
	
	/**
	 * @return The amount of MP this unit can still spend on casting spells in the current combat
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	@Override
	public final int getManaRemaining () throws MomException
	{
		return getMemoryUnit ().getManaRemaining ();
	}
	
	/**
	 * @param mana The amount of MP this unit can still spend on casting spells in the current combat
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	@Override
	public final void setManaRemaining (final int mana) throws MomException
	{
		getMemoryUnit ().setManaRemaining (mana);
	}
	
	/**
	 * @return Any special order this unit is currently on, or null if none
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	@Override
	public final UnitSpecialOrder getSpecialOrder () throws MomException
	{
		return getMemoryUnit ().getSpecialOrder ();
	}
	
	/**
	 * @param o Any special order this unit is currently on, or null if none
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	@Override
	public final void setSpecialOrder (final UnitSpecialOrder o) throws MomException
	{
		getMemoryUnit ().setSpecialOrder (o);
	}	
	
	/**
	 * @return List of damage this unit has taken
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	@Override
	public final List<UnitDamage> getUnitDamage () throws MomException
	{
		return getMemoryUnit ().getUnitDamage ();
	}
}