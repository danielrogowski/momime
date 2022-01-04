package momime.server.calculations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.ndg.map.CoordinateSystemUtils;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.PickAndQuantity;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.UnitEx;
import momime.common.messages.CombatMapSize;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MemoryUnit;
import momime.common.messages.UnitStatusID;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.PlayerPickUtils;
import momime.server.utils.UnitServerUtils;

/**
 * Server only calculations pertaining to units, e.g. calculations relating to fog of war
 */
public final class ServerUnitCalculationsImpl implements ServerUnitCalculations
{
	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/**
	 * @param unit The unit to check
	 * @param db Lookup lists built over the XML database
	 * @return How many squares this unit can see; by default = 1, flying units automatically get 2, and the Scouting unit skill can push this even higher
	 * @throws RecordNotFoundException If we can't find the player who owns the unit, or the unit has a skill that we can't find in the cache
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	@Override
	public final int calculateUnitScoutingRange (final ExpandedUnitDetails unit, final CommonDatabase db) throws RecordNotFoundException, MomException
	{
		int scoutingRange = 1;

		// Actual scouting skill
		if (unit.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_SCOUTING))
			scoutingRange = Math.max (scoutingRange, unit.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_SCOUTING));

		// Scouting range granted by other skills (i.e. flight skills)
		for (final String thisSkillID : unit.listModifiedSkillIDs ())
		{
			final Integer unitSkillScoutingRange = db.findUnitSkill (thisSkillID, "calculateUnitScoutingRange").getUnitSkillScoutingRange ();
			if (unitSkillScoutingRange != null)
				scoutingRange = Math.max (scoutingRange, unitSkillScoutingRange);
		}

		return scoutingRange;
	}

	/**
	 * Non-magical ranged attack incurr a -10% to hit penalty for each 3 tiles distance between the attacking and defending unit on the combat map.
	 * This is loosely explained in the manual and strategy guide, but the info on the MoM wiki is clearer.
	 * 
	 * @param attacker Unit firing the ranged attack
	 * @param defender Unit being shot
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @return To hit penalty incurred from the distance between the attacker and defender, NB. this is not capped in any way so may get very high values here
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	@Override
	public final int calculateRangedAttackDistancePenalty (final ExpandedUnitDetails attacker, final ExpandedUnitDetails defender,
		final CombatMapSize combatMapCoordinateSystem) throws MomException
	{
		// Magic attacks suffer no penalty
		int penalty;
		if (attacker.getRangedAttackType ().getMagicRealmID () != null)
			penalty = 0;
		else
		{
			final double distance = getCoordinateSystemUtils ().determineReal2DDistanceBetween
				(combatMapCoordinateSystem, attacker.getCombatPosition (), defender.getCombatPosition ());
			
			penalty = (int) (distance / 3);
			
			// Long range skill?
			if ((penalty > 1) && (attacker.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_LONG_RANGE)))				
				penalty = 1;
		}
		
		return penalty;
	}
	
	/**
	 * Gets a list of all the units a summoning spell might summon if we cast it.  That's straightforward for normal summoning spells, but heroes can only be
	 * hired once and if killed are never available to summon again.  Plus some heroes are restricted depending on what our spell book picks are.
	 * 
	 * @param spell Summoning spell
	 * @param wizardDetails Wizard casting the spell
	 * @param trueUnits List of true units
	 * @param db Lookup lists built over the XML database
	 * @return List of units this spell might summon if we cast it; list can be empty if we're already summoned and killed all heroes for example
	 * @throws RecordNotFoundException If one of the summoned unit IDs can't be found in the DB
	 */
	@Override
	public final List<UnitEx> listUnitsSpellMightSummon (final Spell spell, final KnownWizardDetails wizardDetails, final List<MemoryUnit> trueUnits, final CommonDatabase db)
		throws RecordNotFoundException
	{
		final List<UnitEx> possibleUnits = new ArrayList<UnitEx> ();
		for (final String possibleSummonedUnitID : spell.getSummonedUnit ())
		{
			// Check whether we can summon this unit If its a hero, this depends on whether we've summoned the hero before, or if he's dead
			final UnitEx possibleUnit = db.findUnit (possibleSummonedUnitID, "listUnitsSpellMightSummon");
			boolean addToList;
			if (possibleUnit.getUnitMagicRealm ().equals (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
			{
				final MemoryUnit hero = getUnitServerUtils ().findUnitWithPlayerAndID (trueUnits, wizardDetails.getPlayerID (), possibleSummonedUnitID);

				if (hero == null)
					addToList = false;
				else
					addToList = ((hero.getStatus () == UnitStatusID.NOT_GENERATED) || (hero.getStatus () == UnitStatusID.GENERATED));
			}
			else
				addToList = true;
			
			// Check for units that require particular picks to summon
			final Iterator<PickAndQuantity> iter = possibleUnit.getUnitPickPrerequisite ().iterator ();
			while ((addToList) && (iter.hasNext ()))
			{
				final PickAndQuantity prereq = iter.next ();
				if (getPlayerPickUtils ().getQuantityOfPick (wizardDetails.getPick (), prereq.getPickID ()) < prereq.getQuantity ())
					addToList = false;
			}

			if (addToList)
				possibleUnits.add (possibleUnit);
		}
		
		return possibleUnits;
	}

	/**
	 * Similar to listUnitsSpellMightSummon, except lists all heroes who haven't been killed, and who we have the necessary spell book picks for. 
	 * 
	 * @param wizardDetails Wizard recruiting heroes
	 * @param trueUnits List of true units
	 * @param db Lookup lists built over the XML database
	 * @return List of heroes available to us
	 */
	@Override
	public final List<UnitEx> listHeroesForHire (final KnownWizardDetails wizardDetails, final List<MemoryUnit> trueUnits, final CommonDatabase db)
	{
		final List<UnitEx> possibleUnits = new ArrayList<UnitEx> ();
		for (final UnitEx possibleUnit : db.getUnits ())
			if ((possibleUnit.getUnitMagicRealm ().equals (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO)) &&
				(possibleUnit.getHiringFame () != null) && (possibleUnit.getProductionCost () != null))
			{
				final MemoryUnit hero = getUnitServerUtils ().findUnitWithPlayerAndID (trueUnits, wizardDetails.getPlayerID (), possibleUnit.getUnitID ());

				boolean addToList;
				if (hero == null)
					addToList = false;
				else
					addToList = ((hero.getStatus () == UnitStatusID.NOT_GENERATED) || (hero.getStatus () == UnitStatusID.GENERATED));
				
				// Check for units that require particular picks to summon
				final Iterator<PickAndQuantity> iter = possibleUnit.getUnitPickPrerequisite ().iterator ();
				while ((addToList) && (iter.hasNext ()))
				{
					final PickAndQuantity prereq = iter.next ();
					if (getPlayerPickUtils ().getQuantityOfPick (wizardDetails.getPick (), prereq.getPickID ()) < prereq.getQuantity ())
						addToList = false;
				}
	
				if (addToList)
					possibleUnits.add (possibleUnit);
			}
		
		return possibleUnits;
	}
	
	/**
	 * @return Server-only unit utils
	 */
	public final UnitServerUtils getUnitServerUtils ()
	{
		return unitServerUtils;
	}

	/**
	 * @param utils Server-only unit utils
	 */
	public final void setUnitServerUtils (final UnitServerUtils utils)
	{
		unitServerUtils = utils;
	}
	
	/**
	 * @return Coordinate system utils
	 */
	public final CoordinateSystemUtils getCoordinateSystemUtils ()
	{
		return coordinateSystemUtils;
	}

	/**
	 * @param utils Coordinate system utils
	 */
	public final void setCoordinateSystemUtils (final CoordinateSystemUtils utils)
	{
		coordinateSystemUtils = utils;
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
}