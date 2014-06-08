package momime.common.utils;

import java.util.Iterator;
import java.util.List;

import momime.common.MomException;
import momime.common.calculations.UnitHasSkillMergedList;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.CombatAreaAffectsPlayersID;
import momime.common.database.v0_9_4.CombatAreaEffect;
import momime.common.database.v0_9_4.CombatAreaEffectAttributeBonus;
import momime.common.database.v0_9_4.CombatAreaEffectSkillBonus;
import momime.common.database.v0_9_4.ExperienceAttributeBonus;
import momime.common.database.v0_9_4.ExperienceLevel;
import momime.common.database.v0_9_4.ExperienceSkillBonus;
import momime.common.database.v0_9_4.Unit;
import momime.common.database.v0_9_4.UnitHasAttributeValue;
import momime.common.database.v0_9_4.UnitHasSkill;
import momime.common.database.v0_9_4.UnitSkill;
import momime.common.database.v0_9_4.UnitType;
import momime.common.database.v0_9_4.UnitUpkeep;
import momime.common.database.v0_9_4.WeaponGradeAttributeBonus;
import momime.common.database.v0_9_4.WeaponGradeSkillBonus;
import momime.common.messages.v0_9_5.AvailableUnit;
import momime.common.messages.v0_9_5.FogOfWarMemory;
import momime.common.messages.v0_9_5.MemoryCombatAreaEffect;
import momime.common.messages.v0_9_5.MemoryMaintainedSpell;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.messages.v0_9_5.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_5.PlayerPick;
import momime.common.messages.v0_9_5.UnitStatusID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;


/**
 * Simple unit lookups and calculations
 */
public final class UnitUtilsImpl implements UnitUtils
{
	/** Class logger */
	private final Log log = LogFactory.getLog (UnitUtilsImpl.class);
	
	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/** Memory CAE utils */
	private MemoryCombatAreaEffectUtils memoryCombatAreaEffectUtils;
	
	/**
	 * @param unitURN Unit URN to search for
	 * @param units List of units to search through
	 * @return Unit with requested URN, or null if not found
	 */
	@Override
	public final MemoryUnit findUnitURN (final int unitURN, final List<MemoryUnit> units)
	{
		log.trace ("Entering findUnitURN: Unit URN " + unitURN);

		MemoryUnit result = null;
		final Iterator<MemoryUnit> iter = units.iterator ();
		while ((result == null) && (iter.hasNext ()))
		{
			final MemoryUnit thisUnit = iter.next ();
			if (thisUnit.getUnitURN () == unitURN)
				result = thisUnit;
		}

		log.trace ("Exiting findUnitURN = " + result);
		return result;
	}

	/**
	 * @param unitURN Unit URN to search for
	 * @param units List of units to search through
	 * @param caller The routine that was looking for the value
	 * @return Unit with requested URN
	 * @throws RecordNotFoundException If unit with requested URN is not found
	 */
	@Override
	public final MemoryUnit findUnitURN (final int unitURN, final List<MemoryUnit> units, final String caller)
		throws RecordNotFoundException
	{
		log.trace ("Entering findUnitURN: Unit URN " + unitURN + ", " + caller);

		final MemoryUnit result = findUnitURN (unitURN, units);

		if (result == null)
			throw new RecordNotFoundException (MemoryUnit.class, unitURN, caller);

		log.trace ("Exiting findUnitURN = " + result);
		return result;
	}

	/**
	 * @param unitURN Unit URN to remove
	 * @param units List of units to search through
	 * @throws RecordNotFoundException If unit with requested URN is not found
	 */
	@Override
	public final void removeUnitURN (final int unitURN, final List<MemoryUnit> units)
		throws RecordNotFoundException
	{
		log.trace ("Entering removeUnitURN: Unit URN " + unitURN);

		boolean found = false;
		final Iterator<MemoryUnit> iter = units.iterator ();
		while ((!found) && (iter.hasNext ()))
		{
			final MemoryUnit thisUnit = iter.next ();
			if (thisUnit.getUnitURN () == unitURN)
			{
				iter.remove ();
				found = true;
			}
		}

		if (!found)
			throw new RecordNotFoundException (MemoryUnit.class, unitURN, "removeUnitURN");

		log.trace ("Exiting removeUnitURN");
	}

	/**
	 * Populates a unit's list of skills after creation - this is the equivalent of the TMomAvailableUnit.CreateAvailableUnit constructor in Delphi
	 * @param unit Unit that has just been created
	 * @param startingExperience Initial experience; if -1 then experience won't be added into skill list, which is used when server sends units to client since they already have exp skill in list
	 * @param loadDefaultSkillsFromXML Whether to add the skills defined in the db for this unit into its skills list
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 * @return Unit definition
	 */
	@Override
	public final Unit initializeUnitSkills (final AvailableUnit unit, final int startingExperience, final boolean loadDefaultSkillsFromXML,
		final CommonDatabase db) throws RecordNotFoundException
	{
		log.trace ("Entering initializeUnitSkills: " + unit.getUnitID ());

		final Unit unitDefinition = db.findUnit (unit.getUnitID (), "initializeUnitSkills");

		// Check whether this type of unit gains experience (summoned units do not)
		// Also when sending heroes from the server to the client, experience is sent in amongst the rest of the skill list, so we don't need to
		// handle it separately here - in this case, experience will be -1
		if (startingExperience >= 0)
		{
			final String unitTypeID = db.findUnitMagicRealm (unitDefinition.getUnitMagicRealm (), "initializeUnitSkills").getUnitTypeID ();
			final UnitType unitType = db.findUnitType (unitTypeID, "initializeUnitSkills");

			if (unitType.getExperienceLevel ().size () > 0)
			{
				final UnitHasSkill exp = new UnitHasSkill ();
				exp.setUnitSkillID (CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE);
				exp.setUnitSkillValue (startingExperience);
				unit.getUnitHasSkill ().add (exp);
			}
		}

		if (loadDefaultSkillsFromXML)
			for (final UnitHasSkill srcSkill : unitDefinition.getUnitHasSkill ())
			{
				final UnitHasSkill destSkill = new UnitHasSkill ();
				destSkill.setUnitSkillID (srcSkill.getUnitSkillID ());
				destSkill.setUnitSkillValue (srcSkill.getUnitSkillValue ());
				unit.getUnitHasSkill ().add (destSkill);
			}

		log.trace ("Entering initializeUnitSkills");
		return unitDefinition;
	}

	/**
	 * Creates and initializes a new unit - this is the equivalent of the TMomUnit.Create constructor in Delphi (except that it doesn't add the created unit into the unit list)
	 * @param unitID Type of unit to create
	 * @param unitURN Unique number identifying this unit
	 * @param weaponGrade Weapon grade to give to this unit
	 * @param startingExperience Initial experience; if -1 then experience won't be added into skill list, which is used when server sends units to client since they already have exp skill in list
	 * @param loadDefaultSkillsFromXML Whether to add the skills defined in the db for this unit into its skills list
	 * @param db Lookup lists built over the XML database
	 * @return Newly created unit
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 */
	@Override
	public final MemoryUnit createMemoryUnit (final String unitID, final int unitURN, final Integer weaponGrade, final int startingExperience,
		final boolean loadDefaultSkillsFromXML, final CommonDatabase db) throws RecordNotFoundException
	{
		log.trace ("Entering createMemoryUnit: " + unitID + ", Unit URN " + unitURN);

		final MemoryUnit newUnit = new MemoryUnit ();
		newUnit.setUnitURN (unitURN);
		newUnit.setUnitID (unitID);
		newUnit.setWeaponGrade (weaponGrade);
		newUnit.setStatus (UnitStatusID.ALIVE);		// Assume unit is alive - heroes being initialized will reset this value

		final Unit unitDefinition = initializeUnitSkills (newUnit, startingExperience, loadDefaultSkillsFromXML, db);

		newUnit.setDoubleOverlandMovesLeft (unitDefinition.getDoubleMovement ());

		log.trace ("Exiting createMemoryUnit");
		return newUnit;
	}

	/**
	 * @param unit Unit to test
	 * @return Number of figures in this unit before it takes any damage
	 */
	@Override
	public final int getFullFigureCount (final Unit unit)
	{
		final int countAccordingToRecord = unit.getFigureCount ();

        // Fudge until we do Hydras properly with a 'Figures-as-heads' skill
		final int realCount;
        if (countAccordingToRecord == 9)
        	realCount = 1;
       	else
       		realCount = countAccordingToRecord;

		return realCount;
	}

	/**
	 * @param skills List of unit skills to check; this can either be the unmodified list read straight from unit.getUnitHasSkill () or UnitHasSkillMergedList
	 * @param unitSkillID Unique identifier for this skill
	 * @return Basic value of the specified skill (defined in the XML or heroes rolled randomly); whether skills granted from spells are included depends on whether we pass in a UnitHasSkillMergedList or not; -1 if we do not have the skill
	 */
	@Override
	public final int getBasicSkillValue (final List<UnitHasSkill> skills, final String unitSkillID)
	{
		int skillValue = -1;
		final Iterator<UnitHasSkill> iter = skills.iterator ();

		while ((skillValue < 0) && (iter.hasNext ()))
		{
			final UnitHasSkill thisSkill = iter.next ();
			if (thisSkill.getUnitSkillID ().equals (unitSkillID))
			{
				if (thisSkill.getUnitSkillValue () == null)
					skillValue = 0;
				else
					skillValue = thisSkill.getUnitSkillValue ();
			}
		}

		return skillValue;
	}

	/**
	 * @param unit Unit whose skills to modify (note we pass in the unit rather than the skills list to force using the live list and not a UnitHasSkillMergedList)
	 * @param unitSkillID Unique identifier for this skill
	 * @param skillValue New basic value of the specified skill
	 * @throws MomException If this unit didn't previously have the specified skill (this method only modifies existing skills, not adds new ones)
	 */
	@Override
	public final void setBasicSkillValue (final AvailableUnit unit, final String unitSkillID, final int skillValue)
		throws MomException
	{
		log.trace ("Entering setBasicSkillValue: " + unit.getUnitID () + ", " + unitSkillID + ", " + skillValue);

		boolean found = false;
		final Iterator<UnitHasSkill> iter = unit.getUnitHasSkill ().iterator ();

		while ((!found) && (iter.hasNext ()))
		{
			final UnitHasSkill thisSkill = iter.next ();
			if (thisSkill.getUnitSkillID ().equals (unitSkillID))
			{
				found = true;
				thisSkill.setUnitSkillValue (skillValue);
			}
		}

		if (!found)
			throw new MomException ("setBasicSkillValue: Unit " + unit.getUnitID () + " does not have skill " + unitSkillID + " and so cannot set its value to " + skillValue);

		log.trace ("Exiting setBasicSkillValue");
	}

	/**
	 * @param unit Unit whose skills we want to output, not including bonuses from things like adamantium weapons, spells cast on the unit and so on
	 * @return Debug string listing out all the skills
	 */
	@Override
	public final String describeBasicSkillValuesInDebugString (final AvailableUnit unit)
	{
		String result = "";
		for (final UnitHasSkill thisSkill : unit.getUnitHasSkill ())
		{
			if (!result.equals (""))
				result = result + ", ";

			if ((thisSkill.getUnitSkillValue () != null) && (thisSkill.getUnitSkillValue () != 0))
				result = result + thisSkill.getUnitSkillValue () + "x";

			result = result + thisSkill.getUnitSkillID ();
		}

		return result;
	}

	/**
	 * @param spells List of known maintained spells
	 * @param unit Unit whose skill list this is
	 * @return List of all skills this unit has, with skills gained from spells (both enchantments such as Holy Weapon and curses such as Vertigo) merged into the list
	 */
	@Override
	public final UnitHasSkillMergedList mergeSpellEffectsIntoSkillList (final List<MemoryMaintainedSpell> spells, final MemoryUnit unit)
	{
		log.trace ("Entering mergeSpellEffectsIntoSkillList: " + unit.getUnitID () + ", " + spells.size () + ", " + unit.getUnitHasSkill ().size ());

		// To avoid getModifiedSkillValue () getting stuck in a loop, we HAVE to return a MomUnitSkillValueMergedList object with the
		// different implementation of getModifiedSkillValue (), even if there's no spells cast on this unit that grant extra skills
		final UnitHasSkillMergedList mergedSkills = new UnitHasSkillMergedList ();
		mergedSkills.addAll (unit.getUnitHasSkill ());

		// Add skills granted by spells cast on this unit
		for (final MemoryMaintainedSpell thisSpell : spells)
			if ((thisSpell.getUnitURN () != null) && (thisSpell.getUnitURN () == unit.getUnitURN ()))
			{
				final UnitHasSkill spellSkill = new UnitHasSkill ();
				spellSkill.setUnitSkillID (thisSpell.getUnitSkillID ());
				mergedSkills.add (spellSkill);
			}

		log.trace ("Exiting mergeSpellEffectsIntoSkillList = " + mergedSkills.size ());

		return mergedSkills;
	}

	/**
	 * @param unit Unit to get value for
	 * @param includeBonuses Whether to include level increases from Warlord+Crusade
	 * @param players Players list
	 * @param combatAreaEffects List of combat area effects known to us (we may not be the owner of the unit)
	 * @param db Lookup lists built over the XML database
	 * @return Experience level of this unit (0-5 for regular units, 0-8 for heroes); for units that don't gain experience (e.g. summoned), returns null
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws RecordNotFoundException If we can't find the unit, unit type, magic realm or so on
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	@Override
	public final ExperienceLevel getExperienceLevel (final AvailableUnit unit, final boolean includeBonuses, final List<? extends PlayerPublicDetails> players,
		final List<MemoryCombatAreaEffect> combatAreaEffects, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering getExperienceLevel: " + unit.getUnitID () + ", " + includeBonuses);

		// Experience can never be increased by spells, combat area effects, weapon grades, etc. etc. therefore safe to do this from the basic skill value on the unmerged list
		final int experienceSkillValue = getBasicSkillValue (unit.getUnitHasSkill (), CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE);

		final ExperienceLevel result;
		if (experienceSkillValue < 0)
			result = null;		// This type of unit doesn't gain experience (e.g. summoned)
		else
		{
			// Check all experience levels defined under the unit type
			// This checks them all so we aren't relying on them being defined in the correct orer
			final String unitMagicRealmID = db.findUnit (unit.getUnitID (), "getExperienceLevel").getUnitMagicRealm ();
			final String unitTypeID = db.findUnitMagicRealm (unitMagicRealmID, "getExperienceLevel").getUnitTypeID ();
			final UnitType unitType = db.findUnitType (unitTypeID, "unitMagicRealmID");

			ExperienceLevel levelFromExperience = null;
			for (final ExperienceLevel experienceLevel : unitType.getExperienceLevel ())

				// Careful - getExperienceRequired () can be null, for normal units' Ultra-Elite and Champion statuses
				// Levels that actually require 0 experience must state a 0 in the XML rather than omit the field
				if ((experienceLevel.getExperienceRequired () != null) && (experienceSkillValue >= experienceLevel.getExperienceRequired ()) &&
					((levelFromExperience == null) || (levelFromExperience.getLevelNumber () < experienceLevel.getLevelNumber ())))
						levelFromExperience = experienceLevel;

			// Check we got one
			if (levelFromExperience == null)
				throw new MomException ("Unit " + unit.getUnitID () + " of type " + unitTypeID + " with " + experienceSkillValue + " experience cannot find any appropraite experience level");

			// Now we've found the level that we're at due to actual experience, see if we get any level bonuses, i.e. Warlord retort or Crusade spell
			if (includeBonuses)
			{
				int levelIncludingBonuses = levelFromExperience.getLevelNumber ();

				// Does the player have the Warlord retort?
				final PlayerPublicDetails owningPlayer = MultiplayerSessionUtils.findPlayerWithID (players, unit.getOwningPlayerID (), "getExperienceLevel");
				final List<PlayerPick> picks = ((MomPersistentPlayerPublicKnowledge) owningPlayer.getPersistentPlayerPublicKnowledge ()).getPick ();
				if (getPlayerPickUtils ().getQuantityOfPick (picks, CommonDatabaseConstants.VALUE_RETORT_ID_WARLORD) > 0)
					levelIncludingBonuses++;

				// Does the player have the Crusade CAE?
				if (getMemoryCombatAreaEffectUtils ().findCombatAreaEffect (combatAreaEffects, null, CommonDatabaseConstants.COMBAT_AREA_EFFECT_CRUSADE, unit.getOwningPlayerID ()))
					levelIncludingBonuses++;

				// Now we have to ensure that the level we've attained actually exists, this is fine for units but a hero might reach Demi-God naturally,
				// then giving them +1 level on top of that will move them to an undefined level
				do
				{
					levelFromExperience = UnitTypeUtils.findExperienceLevel (unitType, levelIncludingBonuses);
					levelIncludingBonuses--;
				} while (levelFromExperience == null);
			}

			result = levelFromExperience;
		}

		log.trace ("Exiting getExperienceLevel = " + result);
		return result;
	}

	/**
	 * Since Available Units cannot be in combat, this is quite a bit simpler than the MomUnit version
	 *
	 * The unit has to:
	 * 1) Be in the right location (or it be a global CAE)
	 * 2) Belong to the right player (either the CAE applies to all players, or just the caster)
	 *
	 * CAEs with player code B or O can never apply to Available Units, since these require the unit to be in combat
	 * The only way CAEs with player code C can apply is if they're global (e.g. Holy Arms)
	 *
	 * @param unit Unit to test
	 * @param effect The combat area effect to test
	 * @param db Lookup lists built over the XML database
	 * @return True if this combat area effect affects this unit
	 * @throws RecordNotFoundException If we can't find the definition for the CAE
	 */
	@Override
	public final boolean doesCombatAreaEffectApplyToUnit (final AvailableUnit unit, final MemoryCombatAreaEffect effect, final CommonDatabase db)
		throws RecordNotFoundException
	{
		log.trace ("Entering doesCombatAreaEffectApplyToUnit: " + unit.getUnitID () + ", " + effect.getCombatAreaEffectID ());

		// Check if unit is in combat (available units can never be in combat)
		final MapCoordinates3DEx combatLocation;
		if (unit instanceof MemoryUnit)
			combatLocation = (MapCoordinates3DEx) ((MemoryUnit) unit).getCombatLocation ();
		else
			combatLocation = null;

		// Check location
		final boolean locationOk;
		if (effect.getMapLocation () == null)
		{
			// Area effect covering the whole map, so must apply
			locationOk = true;
		}
		else if (combatLocation != null)
		{
			// If unit is in combat, then the effect must be located at the combat
			locationOk = effect.getMapLocation ().equals (combatLocation);
		}
		else
		{
			// Area effect in one map location only, so we have to be in the right place
			locationOk = effect.getMapLocation ().equals (unit.getUnitLocation ());
		}

		// Check which player(s) this CAE affects
		final boolean applies;
		if (!locationOk)
			applies = false;
		else
		{
			final CombatAreaEffect combatAreaEffect = db.findCombatAreaEffect (effect.getCombatAreaEffectID (), "doesCombatAreaEffectApplyToUnit");

			// Check player - if this is blank, its a combat area effect that doesn't provide unit bonuses/penalties, e.g. Call Lightning, so we can just return false
			if (combatAreaEffect.getCombatAreaAffectsPlayers () == null)
				applies = false;

			// All is easy, either its a global CAE that affects everyone (like Chaos Surge or Eternal Night) or its a CAE in a
			// specific location that affects everyone, whether or not they're in combat (like Node Auras)
			else if (combatAreaEffect.getCombatAreaAffectsPlayers ().equals (CombatAreaAffectsPlayersID.ALL_EVEN_NOT_IN_COMBAT))
				applies = true;

			// Spells that apply only to the caster only apply to available units if they're global (like Holy Arms)
			// Localised spells that apply only to the caster (like Prayer or Mass Invisibility) only apply to units in combat
			// NDG 19/6/2011 - On looking at this again I think that's highly debatable - if there was a caster only buff CAE then why
			// wouldn't it help units in the city defend against overland attacks like Call the Void?  However I've checked, there's no
			// City Enchantments in the original MoM that grant these kind of bonuses so its pretty irrelevant, so leaving it as it is
			else if (combatAreaEffect.getCombatAreaAffectsPlayers ().equals (CombatAreaAffectsPlayersID.CASTER_ONLY))
				applies = ((effect.getCastingPlayerID () == unit.getOwningPlayerID ()) && ((combatLocation != null) || (effect.getMapLocation () == null)));

			// 'Both' CAEs (like Darkness) apply only to units in combat
			// If the unit is in a combat at the right location, then by definition it is one of the two players (either attacker or defender) and so the CAE applies
			else if (combatAreaEffect.getCombatAreaAffectsPlayers ().equals (CombatAreaAffectsPlayersID.BOTH_PLAYERS_IN_COMBAT))
				applies = (combatLocation != null);

			// Similarly we must be in combat for 'Opponent' CAEs to apply, and to be an 'Opponent' CAE the CAE must be in combat at the same
			// location we are... so this simply needs us to check that we're not the caster
			else if (combatAreaEffect.getCombatAreaAffectsPlayers ().equals (CombatAreaAffectsPlayersID.COMBAT_OPPONENT))
				applies = ((effect.getCastingPlayerID () != unit.getOwningPlayerID ()) && (combatLocation != null));

			// 'Both' CAEs (like Darkness) and 'Opponent' CAEs (like Black Prayer) can only apply to units in combat, which Available Units can never be
			else
				applies = false;
		}

		log.trace ("Exiting doesCombatAreaEffectApplyToUnit = " + applies);
		return applies;
	}

	/**
	 * @param unit Unit we want to check
	 * @param skills List of skills the unit has, either just unit.getUnitHasSkill () or can pre-merge with spell skill list by calling mergeSpellEffectsIntoSkillList
	 * @param spells Known spells
	 * @param db Lookup lists built over the XML database
	 * @return True magic realm/lifeform type ID of this unit, taking into account skills/spells that may modify the value (e.g. Chaos Channels, Undead)
	 * @throws RecordNotFoundException If the unit has a skill that we can't find in the cache
	 */
	@Override
	public final String getModifiedUnitMagicRealmLifeformTypeID (final AvailableUnit unit, final List<UnitHasSkill> skills,
		final List<MemoryMaintainedSpell> spells, final CommonDatabase db)
		throws RecordNotFoundException
	{
		log.trace ("Entering getModifiedUnitMagicRealmLifeformTypeID: " + unit.getUnitID ());

		// Get basic value
		String magicRealmLifeformTypeID = db.findUnit (unit.getUnitID (), "getModifiedUnitMagicRealmLifeformTypeID").getUnitMagicRealm ();

		// If its an actual unit, check if the caller pre-merged the list of skills with skills from spells, or if we need to do it here
		final List<UnitHasSkill> mergedSkills;
		if ((unit instanceof MemoryUnit) && (!(skills instanceof UnitHasSkillMergedList)))
			mergedSkills = mergeSpellEffectsIntoSkillList (spells, (MemoryUnit) unit);
		else
			mergedSkills = skills;

		// Check if any skills or spells override this
		for (final UnitHasSkill thisSkill : mergedSkills)
		{
			final String changedMagicRealmLifeformTypeID = db.findUnitSkill (thisSkill.getUnitSkillID (), "getModifiedUnitMagicRealmLifeformTypeID").getChangesUnitToMagicRealm ();
			if (changedMagicRealmLifeformTypeID != null)
				magicRealmLifeformTypeID = changedMagicRealmLifeformTypeID;
		}

		log.trace ("Exiting getModifiedUnitMagicRealmLifeformTypeID = " + magicRealmLifeformTypeID);
		return magicRealmLifeformTypeID;
	}

	/**
	 * @param unit Unit we want to check
	 * @param skills List of skills the unit has, either just unit.getUnitHasSkill () or can pre-merge with spell skill list by calling mergeSpellEffectsIntoSkillList
	 * @param unitSkillID Unique identifier for this skill
	 * @param players Players list
	 * @param spells Known spells
	 * @param combatAreaEffects Known combat area effects
	 * @param db Lookup lists built over the XML database
	 * @return Value of the specified skill - base value can be improved by weapon grades, experience or CAEs (e.g. Node Auras or Prayer), or can be reduced by curses or enemy CAEs (e.g. Black Prayer); skills granted from spells currently always return zero but this is likely to change
	 * @throws RecordNotFoundException If the unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	@Override
	public final int getModifiedSkillValue (final AvailableUnit unit, final List<UnitHasSkill> skills, final String unitSkillID, final List<? extends PlayerPublicDetails> players,
		final List<MemoryMaintainedSpell> spells, final List<MemoryCombatAreaEffect> combatAreaEffects, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering getModifiedSkillValue: " + unit.getUnitID () + ", " + unitSkillID);

		// If its an actual unit, check if the caller pre-merged the list of skills with skills from spells, or if we need to do it here
		final List<UnitHasSkill> mergedSkills;
		if ((unit instanceof MemoryUnit) && (!(skills instanceof UnitHasSkillMergedList)))
			mergedSkills = mergeSpellEffectsIntoSkillList (spells, (MemoryUnit) unit);
		else
			mergedSkills = skills;

		// Get unit magic realm ID
		final String storeMagicRealmLifeformTypeID = getModifiedUnitMagicRealmLifeformTypeID (unit, mergedSkills, spells, db);

		// Get basic skill value
		final int basicValue = getBasicSkillValue (mergedSkills, unitSkillID);
		int modifiedValue = basicValue;

		// Bonuses only apply if we already have the skill, and its a skill that has a numeric value such as Flame Breath
		// (skills which we have, but have no numeric value, will have value zero so are excluded here)

		// Exclude experience, otherwise we get a repetitive loop as the call to expLvl = getExperienceLevel () lower down calls getSkillValue!
		if ((modifiedValue > 0) && (!unitSkillID.equals (CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE)))
		{
			// Any bonuses due to weapon grades?
			if (unit.getWeaponGrade () != null)
				for (final WeaponGradeSkillBonus bonus : db.findWeaponGrade (unit.getWeaponGrade (), "getModifiedSkillValue").getWeaponGradeSkillBonus ())
					if (bonus.getUnitSkillID ().equals (unitSkillID))
						modifiedValue = modifiedValue + bonus.getBonusValue ();

			// Any bonuses due to experience?
			final ExperienceLevel expLvl = getExperienceLevel (unit, true, players, combatAreaEffects, db);
			if (expLvl != null)
				for (final ExperienceSkillBonus bonus : expLvl.getExperienceSkillBonus ())
					if (bonus.getUnitSkillID ().equals (unitSkillID))
						modifiedValue = modifiedValue + bonus.getBonusValue ();

			// Any bonuses from CAEs?
			for (final MemoryCombatAreaEffect thisCAE : combatAreaEffects)
				if (doesCombatAreaEffectApplyToUnit (unit, thisCAE, db))
				{
					// Found a combat area effect whose location matches this unit, as well as any player or other pre-requisites
					// So this means all the skill bonuses apply, except we still need to do the magic realm
					// check since some effects have different components which apply to different lifeform types, e.g. True Light and Darkness
					for (final CombatAreaEffectSkillBonus caeBonusCache : db.findCombatAreaEffect (thisCAE.getCombatAreaEffectID (), "getModifiedSkillValue").getCombatAreaEffectSkillBonus ())

						// Magic realm/lifeform type can be blank for effects that apply to all types of unit (e.g. Prayer)
						if ((caeBonusCache.getUnitSkillID ().equals (unitSkillID)) &&
							((caeBonusCache.getEffectMagicRealm () == null) || (caeBonusCache.getEffectMagicRealm ().equals (storeMagicRealmLifeformTypeID))))

							modifiedValue = modifiedValue + caeBonusCache.getBonusValue ();
				}
		}

		log.trace ("Exiting getModifiedSkillValue = " + modifiedValue);
		return modifiedValue;
	}
	
	/**
	 * @param value Value of 1 attribute component
	 * @param positiveNegative Whether to only include positive effects, only negative effects, or both
	 * @return value, if positive/negative as desired, or 0 if not wanted
	 */
	final int addToAttributeValue (final int value, final MomUnitAttributePositiveNegative positiveNegative)
	{
		final int result;
		switch (positiveNegative)
		{
			case BOTH:
				result = value;
				break;
				
			case POSITIVE:
				result = (value > 0) ? value : 0;
				break;
				
			case NEGATIVE:
				result = (value < 0) ? value : 0;
				break;
				
			default:
				throw new UnsupportedOperationException ("addToAttributeValue doesn't know how to handle " + positiveNegative);
		}
		return result;
	}
	
	/**
	 * NB. The reason there is no getBasicAttributeValue method is because this can be achieved by passing in MomUnitAttributeComponent.BASIC
	 * 
	 * @param unit Unit to calculate attribute value for
	 * @param unitAttributeID Unique identifier for this attribute
	 * @param component Which component(s) making up this attribute to calculate
	 * @param positiveNegative Whether to only include positive effects, only negative effects, or both
	 * @param players Players list
	 * @param spells Known spells
	 * @param combatAreaEffects Known combat area effects
	 * @param db Lookup lists built over the XML database
	 * @return Calculated unit attribute (e.g. swords/shields/hearts); 0 if attribute is N/A for this unit (unlike skills, which return -1)
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 */
	@Override
	public final int getModifiedAttributeValue (final AvailableUnit unit, final String unitAttributeID, final MomUnitAttributeComponent component,
		final MomUnitAttributePositiveNegative positiveNegative, final List<? extends PlayerPublicDetails> players,
		final List<MemoryMaintainedSpell> spells, final List<MemoryCombatAreaEffect> combatAreaEffects, final CommonDatabase db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.trace ("Entering getModifiedAttributeValue: " + unit.getUnitID () + ", " + unitAttributeID);

		// If its an actual unit, check if the caller pre-merged the list of skills with skills from spells, or if we need to do it here
		final List<UnitHasSkill> mergedSkills;
		if (unit instanceof MemoryUnit)
			mergedSkills = mergeSpellEffectsIntoSkillList (spells, (MemoryUnit) unit);
		else
			mergedSkills = unit.getUnitHasSkill ();
		
		// First get basic attribute value from the DB - we need this regardless of whether it was actually asked for
		final Unit unitDefinition = db.findUnit (unit.getUnitID (), "getModifiedAttributeValue");
		int basicValue = 0;
		boolean found = false;
		final Iterator<UnitHasAttributeValue> iter = unitDefinition.getUnitAttributeValue ().iterator ();
		while ((!found) && (iter.hasNext ()))
		{
			final UnitHasAttributeValue thisAttr = iter.next ();
			if (unitAttributeID.equals (thisAttr.getUnitAttributeID ()))
			{
				found = true;
				basicValue = thisAttr.getAttributeValue ();
			}
		}
		
		// Include basic value in total?
		int total = 0;
		if ((component == MomUnitAttributeComponent.BASIC) || (component == MomUnitAttributeComponent.ALL))
			total = total + addToAttributeValue (basicValue, positiveNegative);
		
		// Any bonuses due to weapon grades?
		// If this is the Ranged Attack skill, only grant bonuses if the unit had a ranged attack to begin with.
		// Otherwise, the weapon grade entries in the database have child nodes underneath them stating which attributes gain how much bonus -
		// this is how we know that e.g. adamantium gives +2 defense but not +2 resistance.
		if ((unit.getWeaponGrade () != null) &&
			((component == MomUnitAttributeComponent.WEAPON_GRADE) || (component == MomUnitAttributeComponent.ALL)) &&
			((!unitAttributeID.equals (CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_RANGED_ATTACK)) || (basicValue > 0)))
		{
			// Only certain types of ranged attack get bonuses from Mithril and Adamantium weapons - e.g. bows do, magical blasts do not
			final boolean weaponGradeBonusApplies;
			if (unitAttributeID.equals (CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_RANGED_ATTACK))
			{
				if (unitDefinition.getRangedAttackType () == null)
					weaponGradeBonusApplies = false;
				else
					weaponGradeBonusApplies = db.findRangedAttackType (unitDefinition.getRangedAttackType (), "getModifiedAttributeValue").isMithrilAndAdamantiumVersions ();
			}
			else
				weaponGradeBonusApplies = true;
			
			if (weaponGradeBonusApplies)
				for (final WeaponGradeAttributeBonus bonus : db.findWeaponGrade (unit.getWeaponGrade (), "getModifiedAttributeValue").getWeaponGradeAttributeBonus ())
					if (bonus.getUnitAttributeID ().equals (unitAttributeID))
						total = total + addToAttributeValue (bonus.getBonusValue (), positiveNegative);
		}
		
		// Any bonuses due to experience?
		// If this is the Ranged Attack skill, only grant bonuses if the unit had a ranged attack to begin with
		final ExperienceLevel expLevel = getExperienceLevel (unit, true, players, combatAreaEffects, db);
		if ((expLevel != null) &&
			((component == MomUnitAttributeComponent.EXPERIENCE) || (component == MomUnitAttributeComponent.ALL)) &&
			((!unitAttributeID.equals (CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_RANGED_ATTACK)) || (basicValue > 0)))
		{
			for (final ExperienceAttributeBonus bonus : expLevel.getExperienceAttributeBonus ())
				if (bonus.getUnitAttributeID ().equals (unitAttributeID))
					total = total + addToAttributeValue (bonus.getBonusValue (), positiveNegative);
		}
		
		// Any bonuses from hero skills? (Might gives +melee, Constitution gives +hit points, Agility gives +defence, and so on)
		if ((expLevel != null) &&
			((component == MomUnitAttributeComponent.HERO_SKILLS) || (component == MomUnitAttributeComponent.ALL)))
			
			// Read down all the skills defined in the database looking for skills that grant a bonus to the attribute we're calculating
			for (final UnitSkill skillDef : db.getUnitSkill ())
				if (unitAttributeID.equals (skillDef.getAddsToAttributeID ()))
				{
					// Now see if the unit has that skill
					int multiplier = getModifiedSkillValue (unit, mergedSkills, skillDef.getUnitSkillID (), players, spells, combatAreaEffects, db);
					if (multiplier > 0)
					{
						// Multiplier will either equal 1 or 2, indicating whether we have the regular or super version of the skill - change this to be 2 for regular or 3 for super
						multiplier++;
						
						// Some skills take more than 1 level to gain 1 attribute point, so get this value
						final int divisor = (skillDef.getAddsToAttributeDivisor () == null) ? 1 : skillDef.getAddsToAttributeDivisor ();
						
						// Now can do the calculation
						final int bonus = ((expLevel.getLevelNumber () + 1) * multiplier) / (divisor*2);
						total = total + addToAttributeValue (bonus, positiveNegative);
					}
				}
		
		// Any bonuses due to spells/special effects in the location the unit is currently in?
		// Ditto, ranged attack bonuses only apply if we had a ranged attack to begin with
		if (((component == MomUnitAttributeComponent.COMBAT_AREA_EFFECTS) || (component == MomUnitAttributeComponent.ALL)) &&
			((!unitAttributeID.equals (CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_RANGED_ATTACK)) || (basicValue > 0)))
		{
			final String storeMagicRealmLifeformTypeID = getModifiedUnitMagicRealmLifeformTypeID (unit, mergedSkills, spells, db);
			for (final MemoryCombatAreaEffect effect : combatAreaEffects)
				if (doesCombatAreaEffectApplyToUnit (unit, effect, db))
				{
					// Found a combat area effect whose location matches this unit, as well as any player or other pre-requisites.
					// So this means all the attribute bonuses apply, except we still need to do the magic realm check
					// since some effects have different components which apply to different lifeform types e.g. True Light and Darkness
					for (final CombatAreaEffectAttributeBonus bonus : db.findCombatAreaEffect (effect.getCombatAreaEffectID (), "getModifiedAttributeValue").getCombatAreaEffectAttributeBonus ())
						if (bonus.getUnitAttributeID ().equals (unitAttributeID))
						{
							// Magic realm/lifeform type can be blank for effects that apply to all types of unit (e.g. prayer)
							if ((bonus.getEffectMagicRealm () == null) || (bonus.getEffectMagicRealm ().equals (storeMagicRealmLifeformTypeID)))
								total = total + addToAttributeValue (bonus.getBonusValue (), positiveNegative);
						}
				}
		}
		
		log.trace ("Exiting getModifiedAttributeValue = " + total);
		return total;
	}

	/**
	 * @param unit Unit to look up the base upkeep for
	 * @param productionTypeID Production type we want to look up the base upkeep for
	 * @param db Lookup lists built over the XML database
	 * @return Base upkeep value, before any reductions such as the Summoner retort reducing upkeep for summoned units; 0 if this unit has no upkeep of this type
	 * @throws RecordNotFoundException If the unitID doesn't exist
	 */
	@Override
	public final int getBasicUpkeepValue (final AvailableUnit unit, final String productionTypeID, final CommonDatabase db)
		throws RecordNotFoundException
	{
		log.trace ("Entering getBasicUpkeepValue: " + unit.getUnitID () + ", " + productionTypeID);

		int result = 0;
		final Iterator<UnitUpkeep> iter = db.findUnit (unit.getUnitID (), "getBasicUpkeepValue").getUnitUpkeep ().iterator ();
		while ((result == 0) && (iter.hasNext ()))
		{
			final UnitUpkeep thisUpkeep = iter.next ();
			if (thisUpkeep.getProductionTypeID ().equals (productionTypeID))
				result = thisUpkeep.getUpkeepValue ();
		}

		log.trace ("Exiting getBasicUpkeepValue = " + result);
		return result;
	}

	/**
	 * @param unit Unit to look up the base upkeep for
	 * @param productionTypeID Production type we want to look up the modified upkeep for
	 * @param players Players list
	 * @param db Lookup lists built over the XML database
	 * @return Upkeep value, modified by reductions such as the Summoner retort reducing upkeep for summoned units; 0 if this unit has no upkeep of this type
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws RecordNotFoundException If the unitID doesn't exist
	 */
	@Override
	public final int getModifiedUpkeepValue (final AvailableUnit unit, final String productionTypeID, final List<? extends PlayerPublicDetails> players,
		final CommonDatabase db)
		throws PlayerNotFoundException, RecordNotFoundException
	{
		log.trace ("Entering getModifiedUpkeepValue: " + unit.getUnitID () + ", " + productionTypeID);

		// Get base value
		final int baseUpkeepValue = getBasicUpkeepValue (unit, productionTypeID, db);

		// Reduce upkeep for Summoner retort?
		final int upkeepValue;
		if (baseUpkeepValue <= 0)
			upkeepValue = baseUpkeepValue;
		else
		{
			// Get reduction as a percentage
			// Note we use the special "unit upkeep" production type, not "Mana"
			final PlayerPublicDetails owningPlayer = MultiplayerSessionUtils.findPlayerWithID (players, unit.getOwningPlayerID (), "getModifiedUpkeepValue");
			final List<PlayerPick> picks = ((MomPersistentPlayerPublicKnowledge) owningPlayer.getPersistentPlayerPublicKnowledge ()).getPick ();

			final String unitMagicRealmID = db.findUnit (unit.getUnitID (), "getModifiedUpkeepValue").getUnitMagicRealm ();
			final String unitTypeID = db.findUnitMagicRealm (unitMagicRealmID, "getModifiedUpkeepValue").getUnitTypeID ();

			final int percentageReduction = getPlayerPickUtils ().totalProductionBonus (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_UNIT_UPKEEP_REDUCTION, unitTypeID, picks, db);

			// Calculate actual amount of reduction, rounding down
			final int amountReduction = (baseUpkeepValue * percentageReduction) / 100;

			upkeepValue = baseUpkeepValue - amountReduction;
		}

		log.trace ("Exiting getModifiedUpkeepValue = " + upkeepValue);
		return upkeepValue;
	}

	/**
	 * Gives all units full movement back again
	 *
	 * @param units List of units to update
	 * @param onlyOnePlayerID If zero, will reset movmenet for units belonging to all players; if specified will reset movement only for units belonging to the specified player
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If we can't find the definition for one of the units
	 */
	@Override
	public final void resetUnitOverlandMovement (final List<MemoryUnit> units, final int onlyOnePlayerID, final CommonDatabase db)
		throws RecordNotFoundException
	{
		log.trace ("Entering resetUnitOverlandMovement: Player ID " + onlyOnePlayerID);

		for (final MemoryUnit thisUnit : units)
			if ((onlyOnePlayerID == 0) || (onlyOnePlayerID == thisUnit.getOwningPlayerID ()))
				thisUnit.setDoubleOverlandMovesLeft (db.findUnit (thisUnit.getUnitID (), "resetUnitOverlandMovement").getDoubleMovement ());

		log.trace ("Exiting resetUnitOverlandMovement");
	}

	/**
	 * Gives all units full movement back again for their combat turn
	 *
	 * @param units List of units to update
	 * @param playerID Player whose units to update 
	 * @param combatLocation Where the combat is taking place
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If we can't find the definition for one of the units
	 */
	@Override
	public final void resetUnitCombatMovement (final List<MemoryUnit> units, final int playerID, final MapCoordinates3DEx combatLocation, final CommonDatabase db)
		throws RecordNotFoundException
	{
		log.trace ("Entering resetUnitCombatMovement: Player ID " + playerID + ", " + combatLocation);

		for (final MemoryUnit thisUnit : units)
			if ((thisUnit.getOwningPlayerID () == playerID) && (combatLocation.equals (thisUnit.getCombatLocation ())) && (thisUnit.getCombatPosition () != null))
				thisUnit.setDoubleCombatMovesLeft (db.findUnit (thisUnit.getUnitID (), "resetUnitCombatMovement").getDoubleMovement ());

		log.trace ("Exiting resetUnitCombatMovement");
	}
	
	/**
	 * @param units Unit stack
	 * @return Comma delimited list of their unit URNs, for debug messages
	 */
	@Override
	public final String listUnitURNs (final List<MemoryUnit> units)
	{
		String list = "";
		if (units != null)
			for (final MemoryUnit thisUnit : units)
			{
				if (!list.equals (""))
					list = list + ", ";

				list = list + thisUnit.getUnitURN ();
			}

		return "(" + list + ")";
	}

	/**
	 * @param units List of units to check (usually movingPlayer's memory)
	 * @param x X coordinate of location to check
	 * @param y Y coordinate of location to check
	 * @param plane Plane to check
	 * @param exceptPlayerID Player who's units to not consider (can pass in 0 to test if there are units *at all* at this location)
	 * @return First unit we find at the requested location who belongs to someone other than the specified player
	 */
	@Override
	public final MemoryUnit findFirstAliveEnemyAtLocation (final List<MemoryUnit> units, final int x, final int y, final int plane, final int exceptPlayerID)
	{
		log.trace ("Entering findFirstAliveEnemyAtLocation: (" + x + ", " + y + ", " + plane + "), Player ID " + exceptPlayerID);

		// The reason this is done separately from countAliveEnemiesAtLocation is because this routine can exit as
		// soon as it finds the first matching unit, whereas countAliveEnemiesAtLocation always has to run over the entire list

		MemoryUnit found = null;
		final Iterator<MemoryUnit> iter = units.iterator ();
		while ((found == null) && (iter.hasNext ()))
		{
			final MemoryUnit thisUnit = iter.next ();

			if ((thisUnit.getStatus () == UnitStatusID.ALIVE) && (thisUnit.getOwningPlayerID () != exceptPlayerID) && (thisUnit.getUnitLocation () != null) &&
				(thisUnit.getUnitLocation ().getX () == x) && (thisUnit.getUnitLocation ().getY () == y) && (thisUnit.getUnitLocation ().getZ () == plane))

				found = thisUnit;
		}

		log.trace ("Exiting findFirstAliveEnemyAtLocation = " +
			((found == null) ? "Not Found" : "Unit URN" + found.getUnitURN ()));
		return found;
	}

	/**
	 * @param units List of units to check
	 * @param x X coordinate of location to check
	 * @param y Y coordinate of location to check
	 * @param plane Plane to check
	 * @param exceptPlayerID Player who's units to not consider (can pass in 0 to count *all* units at this location)
	 * @return Number of units that we find at the requested location who belongs to someone other than the specified player
	 */
	@Override
	public final int countAliveEnemiesAtLocation (final List<MemoryUnit> units, final int x, final int y, final int plane, final int exceptPlayerID)
	{
		log.trace ("Entering countAliveEnemiesAtLocation: (" + x + ", " + y + ", " + plane + "), Player ID " + exceptPlayerID);

		int count = 0;
		for (final MemoryUnit thisUnit : units)
		{
			if ((thisUnit.getStatus () == UnitStatusID.ALIVE) && (thisUnit.getOwningPlayerID () != exceptPlayerID) && (thisUnit.getUnitLocation () != null) &&
				(thisUnit.getUnitLocation ().getX () == x) && (thisUnit.getUnitLocation ().getY () == y) && (thisUnit.getUnitLocation ().getZ () == plane))

				count++;
		}

		log.trace ("Exiting countAliveEnemiesAtLocation = " + count);
		return count;
	}

	/**
	 * Clears up any references to the specified unit from under the FogOfWarMemory structure, because the unit has just been killed
	 * This is used even if the unit is not actually being freed, e.g. could be dismissing a hero or just setting a unit in combat to 'dead' but not actually freeing the unit
	 * 
	 * @param mem Fog of war memory structure to remove references from; can be player's memory or the true map on the server
	 * @param unitURN Unit about to be killed
	 */
	@Override
	public final void beforeKillingUnit (final FogOfWarMemory mem, final int unitURN)
	{
		log.trace ("Entering beforeKillingUnit: " + unitURN);
		
		final Iterator<MemoryMaintainedSpell> iter = mem.getMaintainedSpell ().iterator ();
		while (iter.hasNext ())
		{
			final MemoryMaintainedSpell spell = iter.next ();
			if ((spell.getUnitURN () != null) && (spell.getUnitURN () == unitURN))
				iter.remove ();
		}
		
		log.trace ("Exiting beforeKillingUnit");
	}

	/**
	 * @param units List of units to check
	 * @param combatLocation Location on overland map where the combat is taking place
	 * @param combatPosition Position within the combat map to look at
	 * @return Unit at this position, or null if there isn't one
	 */
	@Override
	public final MemoryUnit findAliveUnitInCombatAt (final List<MemoryUnit> units, final MapCoordinates3DEx combatLocation, final MapCoordinates2DEx combatPosition)
	{
		log.trace ("Entering findAliveUnitInCombatAt: " + combatLocation + ", " + combatPosition);

		MemoryUnit found = null;
		final Iterator<MemoryUnit> iter = units.iterator ();
		while ((found == null) && (iter.hasNext ()))
		{
			final MemoryUnit thisUnit = iter.next ();

			if ((thisUnit.getStatus () == UnitStatusID.ALIVE) && (combatLocation.equals (thisUnit.getCombatLocation ())) &&
				(combatPosition.equals (thisUnit.getCombatPosition ())))

				found = thisUnit;
		}

		log.trace ("Exiting findAliveUnitInCombatAt = " + found);
		return found;
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
}