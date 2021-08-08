package momime.common.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.AddsToSkill;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitEx;
import momime.common.database.UnitSkillEx;
import momime.common.database.UnitType;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.PlayerPick;

/**
 * Methods for working out minimal unit details
 */
public final class UnitDetailsUtilsImpl implements UnitDetailsUtils
{
	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/** Memory CAE utils */
	private MemoryCombatAreaEffectUtils memoryCombatAreaEffectUtils;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/**
	 * Calculates minimal unit details that can be derived quickly without examining the whole unit stack.
	 * 
	 * @param unit Unit to expand skill list for
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return List of all skills this unit has, with skills granted from other skills and skills granted from spells merged into the list
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final MinimalUnitDetails expandMinimalUnitDetails (final AvailableUnit unit,
		final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		// STEP 1 - First just copy the skills from the unit into a map
		// NB. can't just use Collectors.toMap () because this throws an exception if you have any null values (which we often will)
		final Map<String, Integer> basicSkillValues = new HashMap<String, Integer> ();
		unit.getUnitHasSkill ().forEach (s -> basicSkillValues.put (s.getUnitSkillID (), s.getUnitSkillValue ()));
		
		// STEP 2 - Do simple lookups
		// Need the player picks to look for Crusade, and need the unit type to get their experience levels
		final UnitEx unitDef = db.findUnit (unit.getUnitID (), "expandMinimalUnitDetails");
		final PlayerPublicDetails owningPlayer = (unit.getOwningPlayerID () == 0) ? null : getMultiplayerSessionUtils ().findPlayerWithID (players, unit.getOwningPlayerID (), "expandMinimalUnitDetails");
		final List<PlayerPick> picks = (owningPlayer == null) ? null : ((MomPersistentPlayerPublicKnowledge) owningPlayer.getPersistentPlayerPublicKnowledge ()).getPick ();
		
		final String unitTypeID = db.findPick (unitDef.getUnitMagicRealm (), "expandMinimalUnitDetails").getUnitTypeID ();
		final UnitType unitType = db.findUnitType (unitTypeID, "expandMinimalUnitDetails");
		
		// STEP 3 - Find the unit's experience level
		// Experience can never be increased by spells, combat area effects, weapon grades, etc. etc. therefore safe to do this from the basic skill value on the unmerged list
		Integer experienceSkillValue = basicSkillValues.get (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE);
		final ExperienceLevel basicExpLvl;
		final ExperienceLevel modifiedExpLvl;
		if (experienceSkillValue == null)
		{
			basicExpLvl = null;		// This type of unit doesn't gain experience (e.g. summoned)
			modifiedExpLvl = null;
		}
		else
		{
			// Check to see if the unit has heroism cast on it
			// This is a special case from all the other "adds to skill" modifiers as it has to happen way earlier
			if (unit instanceof MemoryUnit)
			{
				final int unitURN = ((MemoryUnit) unit).getUnitURN ();
				for (final MemoryMaintainedSpell thisSpell : mem.getMaintainedSpell ())
					if ((thisSpell.getUnitURN () != null) && (thisSpell.getUnitURN () == unitURN) && (thisSpell.getUnitSkillID () != null))
					{
						final UnitSkillEx unitSkill = db.findUnitSkill (thisSpell.getUnitSkillID (), "expandMinimalUnitDetails");
						for (final AddsToSkill addsToSkill : unitSkill.getAddsToSkill ())
							if (addsToSkill.getAddsToSkillID ().equals (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE))
								switch (addsToSkill.getAddsToSkillValueType ())
								{
									case ADD_FIXED:
										experienceSkillValue = experienceSkillValue + addsToSkill.getAddsToSkillValue ();
										break;
										
									case LOCK:
										experienceSkillValue = addsToSkill.getAddsToSkillValue ();
										break;
										
									default:
										// Either kind of divide makes no sense for this
								}
					}
			}
			
			// Check all experience levels defined under the unit type
			// This checks them all so we aren't relying on them being defined in the correct orer
			ExperienceLevel levelFromExperience = null;
			for (final ExperienceLevel experienceLevel : unitType.getExperienceLevel ())

				// Careful - getExperienceRequired () can be null, for normal units' Ultra-Elite and Champion statuses
				// Levels that actually require 0 experience must state a 0 in the XML rather than omit the field
				if ((experienceLevel.getExperienceRequired () != null) && (experienceSkillValue >= experienceLevel.getExperienceRequired ()) &&
					((levelFromExperience == null) || (levelFromExperience.getLevelNumber () < experienceLevel.getLevelNumber ())))
						levelFromExperience = experienceLevel;

			// Check we got one
			if (levelFromExperience == null)
				throw new MomException ("Unit " + unit.getUnitID () + " of type " + unitTypeID + " with " + experienceSkillValue + " experience cannot find any appropriate experience level");

			// Now we've found the level that we're at due to actual experience, see if we get any level bonuses, i.e. Warlord retort or Crusade spell
			basicExpLvl = levelFromExperience;
			int levelIncludingBonuses = levelFromExperience.getLevelNumber ();

			// Does the player have the Warlord retort?
			if ((picks != null) && (getPlayerPickUtils ().getQuantityOfPick (picks, CommonDatabaseConstants.RETORT_ID_WARLORD) > 0))
				levelIncludingBonuses++;

			// Does the player have the Crusade CAE?
			if (getMemoryCombatAreaEffectUtils ().findCombatAreaEffect (mem.getCombatAreaEffect (), null, CommonDatabaseConstants.COMBAT_AREA_EFFECT_CRUSADE, unit.getOwningPlayerID ()) != null)
				levelIncludingBonuses++;

			// Now we have to ensure that the level we've attained actually exists, this is fine for units but a hero might reach Demi-God naturally,
			// then giving them +1 level on top of that will move them to an undefined level
			do
			{
				levelFromExperience = UnitTypeUtils.findExperienceLevel (unitType, levelIncludingBonuses);
				levelIncludingBonuses--;
			} while (levelFromExperience == null);

			modifiedExpLvl = levelFromExperience;
		}
		
		// Finally can build the unit object
		final MinimalUnitDetailsImpl container = new MinimalUnitDetailsImpl (unit, unitDef, unitType, owningPlayer, basicExpLvl, modifiedExpLvl, basicSkillValues);
		return container;
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
}