package momime.common.messages;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import momime.common.MomException;
import momime.common.calculations.UnitHasSkillMergedList;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.CommonDatabaseLookup;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.CombatAreaAffectsPlayersID;
import momime.common.database.v0_9_4.CombatAreaEffect;
import momime.common.database.v0_9_4.CombatAreaEffectSkillBonus;
import momime.common.database.v0_9_4.ExperienceLevel;
import momime.common.database.v0_9_4.ExperienceSkillBonus;
import momime.common.database.v0_9_4.Unit;
import momime.common.database.v0_9_4.UnitHasSkill;
import momime.common.database.v0_9_4.UnitType;
import momime.common.database.v0_9_4.UnitUpkeep;
import momime.common.database.v0_9_4.WeaponGradeSkillBonus;
import momime.common.messages.v0_9_4.AvailableUnit;
import momime.common.messages.v0_9_4.MemoryCombatAreaEffect;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.common.messages.v0_9_4.PlayerPick;
import momime.common.messages.v0_9_4.UnitStatusID;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * Simple unit lookups and calculations
 */
public final class UnitUtils
{
	/**
	 * @param unitURN Unit URN to search for
	 * @param units List of units to search through
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Unit with requested URN, or null if not found
	 */
	public final static MemoryUnit findUnitURN (final int unitURN, final List<MemoryUnit> units, final Logger debugLogger)
	{
		debugLogger.entering (UnitUtils.class.getName (), "findUnitURN", unitURN);

		MemoryUnit result = null;
		final Iterator<MemoryUnit> iter = units.iterator ();
		while ((result == null) && (iter.hasNext ()))
		{
			final MemoryUnit thisUnit = iter.next ();
			if (thisUnit.getUnitURN () == unitURN)
				result = thisUnit;
		}

		debugLogger.exiting (UnitUtils.class.getName (), "findUnitURN", result);
		return result;
	}

	/**
	 * @param unitURN Unit URN to search for
	 * @param units List of units to search through
	 * @param caller The routine that was looking for the value
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Unit with requested URN
	 * @throws RecordNotFoundException If unit with requested URN is not found
	 */
	public final static MemoryUnit findUnitURN (final int unitURN, final List<MemoryUnit> units, final String caller, final Logger debugLogger)
		throws RecordNotFoundException
	{
		debugLogger.entering (UnitUtils.class.getName (), "findUnitURN", new String [] {new Integer (unitURN).toString (), caller});

		final MemoryUnit result = findUnitURN (unitURN, units, debugLogger);

		if (result == null)
			throw new RecordNotFoundException ("UnitURN", unitURN, caller);

		debugLogger.exiting (UnitUtils.class.getName (), "findUnitURN", result);
		return result;
	}

	/**
	 * Populates a unit's list of skills after creation - this is the equivalent of the TMomAvailableUnit.CreateAvailableUnit constructor in Delphi
	 * @param unit Unit that has just been created
	 * @param startingExperience Initial experience; if -1 then experience won't be added into skill list, which is used when server sends units to client since they already have exp skill in list
	 * @param loadDefaultSkillsFromXML Whether to add the skills defined in the db for this unit into its skills list
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 * @return Unit definition
	 */
	final static Unit initializeUnitSkills (final AvailableUnit unit, final int startingExperience, final boolean loadDefaultSkillsFromXML,
		final CommonDatabaseLookup db, final Logger debugLogger) throws RecordNotFoundException
	{
		debugLogger.entering (UnitUtils.class.getName (), "initializeUnitSkills", unit.getUnitID ());

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

		debugLogger.entering (UnitUtils.class.getName (), "initializeUnitSkills");
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
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Newly created unit
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 */
	public final static MemoryUnit createMemoryUnit (final String unitID, final int unitURN, final Integer weaponGrade, final int startingExperience,
		final boolean loadDefaultSkillsFromXML, final CommonDatabaseLookup db, final Logger debugLogger) throws RecordNotFoundException
	{
		debugLogger.entering (UnitUtils.class.getName (), "createMemoryUnit", new String [] {unitID, new Integer (unitURN).toString ()});

		final MemoryUnit newUnit = new MemoryUnit ();
		newUnit.setUnitURN (unitURN);
		newUnit.setUnitID (unitID);
		newUnit.setWeaponGrade (weaponGrade);
		newUnit.setStatus (UnitStatusID.ALIVE);		// Assume unit is alive - heroes being initialized will reset this value

		final Unit unitDefinition = initializeUnitSkills (newUnit, startingExperience, loadDefaultSkillsFromXML, db, debugLogger);

		newUnit.setDoubleOverlandMovesLeft (unitDefinition.getDoubleMovement ());

		debugLogger.exiting (UnitUtils.class.getName (), "createMemoryUnit");
		return newUnit;
	}

	/**
	 * @param unit Unit to test
	 * @return Number of figures in this unit before it takes any damage
	 */
	public final static int getFullFigureCount (final Unit unit)
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
	public final static int getBasicSkillValue (final List<UnitHasSkill> skills, final String unitSkillID)
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
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws MomException If this unit didn't previously have the specified skill (this method only modifies existing skills, not adds new ones)
	 */
	public final static void setBasicSkillValue (final AvailableUnit unit, final String unitSkillID, final int skillValue, final Logger debugLogger)
		throws MomException
	{
		debugLogger.entering (UnitUtils.class.getName (), "setBasicSkillValue", new String [] {unit.getUnitID (), unitSkillID, new Integer (skillValue).toString ()});

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

		debugLogger.exiting (UnitUtils.class.getName (), "setBasicSkillValue");
	}

	/**
	 * @param unit Unit whose skills we want to output, not including bonuses from things like adamantium weapons, spells cast on the unit and so on
	 * @return Debug string listing out all the skills
	 */
	public final static String describeBasicSkillValuesInDebugString (final AvailableUnit unit)
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
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return List of all skills this unit has, with skills gained from spells (both enchantments such as Holy Weapon and curses such as Vertigo) merged into the list
	 */
	public final static UnitHasSkillMergedList mergeSpellEffectsIntoSkillList (final List<MemoryMaintainedSpell> spells, final MemoryUnit unit, final Logger debugLogger)
	{
		debugLogger.entering (UnitUtils.class.getName (), "mergeSpellEffectsIntoSkillList", new String [] {unit.getUnitID (), new Integer (spells.size ()).toString (), new Integer (unit.getUnitHasSkill ().size ()).toString ()});

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

		debugLogger.exiting (UnitUtils.class.getName (), "mergeSpellEffectsIntoSkillList", mergedSkills.size ());

		return mergedSkills;
	}

	/**
	 * @param unit Unit to get value for
	 * @param includeBonuses Whether to include level increases from Warlord+Crusade
	 * @param players Players list
	 * @param combatAreaEffects List of combat area effects known to us (we may not be the owner of the unit)
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Experience level of this unit (0-5 for regular units, 0-8 for heroes); for units that don't gain experience (e.g. summoned), returns null
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws RecordNotFoundException If we can't find the unit, unit type, magic realm or so on
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	public final static ExperienceLevel getExperienceLevel (final AvailableUnit unit, final boolean includeBonuses, final List<? extends PlayerPublicDetails> players,
		final List<MemoryCombatAreaEffect> combatAreaEffects, final CommonDatabaseLookup db, final Logger debugLogger)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		debugLogger.entering (UnitUtils.class.getName (), "getExperienceLevel", unit.getUnitID ());

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
				if (PlayerPickUtils.getQuantityOfPick (picks, CommonDatabaseConstants.VALUE_RETORT_ID_WARLORD, debugLogger) > 0)
					levelIncludingBonuses++;

				// Does the player have the Crusade CAE?
				if (MemoryCombatAreaEffectUtils.findCombatAreaEffect (combatAreaEffects, null, CommonDatabaseConstants.COMBAT_AREA_EFFECT_CRUSADE, unit.getOwningPlayerID (), debugLogger))
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

		debugLogger.exiting (UnitUtils.class.getName (), "getExperienceLevel", result);
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
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return True if this combat area effect affects this unit
	 * @throws RecordNotFoundException If we can't find the definition for the CAE
	 */
	public final static boolean doesCombatAreaEffectApplyToUnit (final AvailableUnit unit, final MemoryCombatAreaEffect effect, final CommonDatabaseLookup db, final Logger debugLogger)
		throws RecordNotFoundException
	{
		debugLogger.entering (UnitUtils.class.getName (), "doesCombatAreaEffectApplyToUnit", new String [] {unit.getUnitID (), effect.getCombatAreaEffectID ()});

		// Check if unit is in combat (available units can never be in combat)
		final OverlandMapCoordinates combatLocation;
		if (unit instanceof MemoryUnit)
			combatLocation = ((MemoryUnit) unit).getCombatLocation ();
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
			locationOk = CoordinatesUtils.overlandMapCoordinatesEqual (effect.getMapLocation (), combatLocation);
		}
		else
		{
			// Area effect in one map location only, so we have to be in the right place
			locationOk = CoordinatesUtils.overlandMapCoordinatesEqual (effect.getMapLocation (), unit.getUnitLocation ());
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

		debugLogger.exiting (UnitUtils.class.getName (), "doesCombatAreaEffectApplyToUnit", applies);
		return applies;
	}

	/**
	 * @param unit Unit we want to check
	 * @param skills List of skills the unit has, either just unit.getUnitHasSkill () or can pre-merge with spell skill list by calling mergeSpellEffectsIntoSkillList
	 * @param spells Known spells
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return True magic realm/lifeform type ID of this unit, taking into account skills/spells that may modify the value (e.g. Chaos Channels, Undead)
	 * @throws RecordNotFoundException If the unit has a skill that we can't find in the cache
	 */
	public static final String getModifiedUnitMagicRealmLifeformTypeID (final AvailableUnit unit, final List<UnitHasSkill> skills,
		final List<MemoryMaintainedSpell> spells, final CommonDatabaseLookup db, final Logger debugLogger)
		throws RecordNotFoundException
	{
		debugLogger.entering (UnitUtils.class.getName (), "getModifiedUnitMagicRealmLifeformTypeID", unit);

		// Get basic value
		String magicRealmLifeformTypeID = db.findUnit (unit.getUnitID (), "getModifiedUnitMagicRealmLifeformTypeID").getUnitMagicRealm ();

		// If its an actual unit, check if the caller pre-merged the list of skills with skills from spells, or if we need to do it here
		final List<UnitHasSkill> mergedSkills;
		if ((unit instanceof MemoryUnit) && (!(skills instanceof UnitHasSkillMergedList)))
			mergedSkills = mergeSpellEffectsIntoSkillList (spells, (MemoryUnit) unit, debugLogger);
		else
			mergedSkills = skills;

		// Check if any skills or spells override this
		for (final UnitHasSkill thisSkill : mergedSkills)
		{
			final String changedMagicRealmLifeformTypeID = db.findUnitSkill (thisSkill.getUnitSkillID (), "getModifiedUnitMagicRealmLifeformTypeID").getChangesUnitToMagicRealm ();
			if (changedMagicRealmLifeformTypeID != null)
				magicRealmLifeformTypeID = changedMagicRealmLifeformTypeID;
		}

		debugLogger.exiting (UnitUtils.class.getName (), "getModifiedUnitMagicRealmLifeformTypeID", magicRealmLifeformTypeID);
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
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Value of the specified skill - base value can be improved by weapon grades, experience or CAEs (e.g. Node Auras or Prayer), or can be reduced by curses or enemy CAEs (e.g. Black Prayer); skills granted from spells currently always return zero but this is likely to change
	 * @throws RecordNotFoundException If the unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	public final static int getModifiedSkillValue (final AvailableUnit unit, final List<UnitHasSkill> skills, final String unitSkillID, final List<? extends PlayerPublicDetails> players,
		final List<MemoryMaintainedSpell> spells, final List<MemoryCombatAreaEffect> combatAreaEffects, final CommonDatabaseLookup db, final Logger debugLogger)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		debugLogger.entering (UnitUtils.class.getName (), "getModifiedSkillValue", new String [] {unit.getUnitID (), unitSkillID});

		// If its an actual unit, check if the caller pre-merged the list of skills with skills from spells, or if we need to do it here
		final List<UnitHasSkill> mergedSkills;
		if ((unit instanceof MemoryUnit) && (!(skills instanceof UnitHasSkillMergedList)))
			mergedSkills = mergeSpellEffectsIntoSkillList (spells, (MemoryUnit) unit, debugLogger);
		else
			mergedSkills = skills;

		// Get unit magic realm ID
		final String storeMagicRealmLifeformTypeID = getModifiedUnitMagicRealmLifeformTypeID (unit, mergedSkills, spells, db, debugLogger);

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
			{
				for (final WeaponGradeSkillBonus bonus : db.findWeaponGrade (unit.getWeaponGrade (), "getModifiedSkillValue").getWeaponGradeSkillBonus ())
					if (bonus.getUnitSkillID ().equals (unitSkillID))
						modifiedValue = modifiedValue + bonus.getBonusValue ();
			}

			// Any bonuses due to experience?
			final ExperienceLevel expLvl = getExperienceLevel (unit, true, players, combatAreaEffects, db, debugLogger);
			if (expLvl != null)
			{
				for (final ExperienceSkillBonus bonus : expLvl.getExperienceSkillBonus ())
					if (bonus.getUnitSkillID ().equals (unitSkillID))
						modifiedValue = modifiedValue + bonus.getBonusValue ();
			}

			// Any bonuses from CAEs?
			for (final MemoryCombatAreaEffect thisCAE : combatAreaEffects)
				if (doesCombatAreaEffectApplyToUnit (unit, thisCAE, db, debugLogger))
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

		debugLogger.exiting (UnitUtils.class.getName (), "getModifiedSkillValue", modifiedValue);
		return modifiedValue;
	}

	/**
	 * @param unit Unit to look up the base upkeep for
	 * @param productionTypeID Production type we want to look up the base upkeep for
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Base upkeep value, before any reductions such as the Summoner retort reducing upkeep for summoned units; 0 if this unit has no upkeep of this type
	 * @throws RecordNotFoundException If the unitID doesn't exist
	 */
	public final static int getBasicUpkeepValue (final AvailableUnit unit, final String productionTypeID, final CommonDatabaseLookup db, final Logger debugLogger)
		throws RecordNotFoundException
	{
		debugLogger.entering (UnitUtils.class.getName (), "getBasicUpkeepValue", new String [] {unit.getUnitID (), productionTypeID});

		int result = 0;
		final Iterator<UnitUpkeep> iter = db.findUnit (unit.getUnitID (), "getBasicUpkeepValue").getUnitUpkeep ().iterator ();
		while ((result == 0) && (iter.hasNext ()))
		{
			final UnitUpkeep thisUpkeep = iter.next ();
			if (thisUpkeep.getProductionTypeID ().equals (productionTypeID))
				result = thisUpkeep.getUpkeepValue ();
		}

		debugLogger.exiting (UnitUtils.class.getName (), "getBasicUpkeepValue", result);
		return result;
	}

	/**
	 * @param unit Unit to look up the base upkeep for
	 * @param productionTypeID Production type we want to look up the modified upkeep for
	 * @param players Players list
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Upkeep value, modified by reductions such as the Summoner retort reducing upkeep for summoned units; 0 if this unit has no upkeep of this type
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws RecordNotFoundException If the unitID doesn't exist
	 */
	public final static int getModifiedUpkeepValue (final AvailableUnit unit, final String productionTypeID, final List<? extends PlayerPublicDetails> players,
		final CommonDatabaseLookup db, final Logger debugLogger)
		throws PlayerNotFoundException, RecordNotFoundException
	{
		debugLogger.entering (UnitUtils.class.getName (), "getModifiedUpkeepValue", new String [] {unit.getUnitID (), productionTypeID});

		// Get base value
		final int baseUpkeepValue = getBasicUpkeepValue (unit, productionTypeID, db, debugLogger);

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

			final int percentageReduction = PlayerPickUtils.totalProductionBonus (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_UNIT_UPKEEP_REDUCTION, unitTypeID, picks, db, debugLogger);

			// Calculate actual amount of reduction, rounding down
			final int amountReduction = (baseUpkeepValue * percentageReduction) / 100;

			upkeepValue = baseUpkeepValue - amountReduction;
		}

		debugLogger.exiting (UnitUtils.class.getName (), "getModifiedUpkeepValue", upkeepValue);
		return upkeepValue;
	}

	/**
	 * Prevent instantiation
	 */
	private UnitUtils ()
	{
	}
}
