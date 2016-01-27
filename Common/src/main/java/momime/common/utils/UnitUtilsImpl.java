package momime.common.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.calculations.UnitHasSkillMergedList;
import momime.common.database.CombatAreaAffectsPlayersID;
import momime.common.database.CombatAreaEffect;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.database.HeroItemTypeAllowedBonus;
import momime.common.database.MergedFromPick;
import momime.common.database.Pick;
import momime.common.database.ProductionTypeAndUndoubledValue;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.Unit;
import momime.common.database.UnitSkillAndValue;
import momime.common.database.UnitSpecialOrder;
import momime.common.database.UnitSpellEffect;
import momime.common.database.UnitType;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MemoryUnitHeroItemSlot;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.PlayerPick;
import momime.common.messages.UnitDamage;
import momime.common.messages.UnitStatusID;

/**
 * Simple unit lookups of basic skill, attribute and upkeep values
 */
public final class UnitUtilsImpl implements UnitUtils
{
	/** Class logger */
	private final Log log = LogFactory.getLog (UnitUtilsImpl.class);
	
	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/** Memory CAE utils */
	private MemoryCombatAreaEffectUtils memoryCombatAreaEffectUtils;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
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
	 * Populates a unit's list of skills after creation - this is the equivalent of the TMomAvailableUnit.CreateAvailableUnit constructor in Delphi.
	 * The client will never use this on real units - the server always sends them will all info already populated; but the client does
	 * need this for initializing skills of sample units, e.g. when drawing units on the change construction screen.
	 * 
	 * @param unit Unit that has just been created
	 * @param startingExperience Initial experience; if -1 or null then experience won't be added into skill list, which is used when server sends units to client since they already have exp skill in list
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 * @return Unit definition
	 */
	@Override
	public final Unit initializeUnitSkills (final AvailableUnit unit, final Integer startingExperience, final CommonDatabase db) throws RecordNotFoundException
	{
		log.trace ("Entering initializeUnitSkills: " + unit.getUnitID ());

		final Unit unitDefinition = db.findUnit (unit.getUnitID (), "initializeUnitSkills");

		// Check whether this type of unit gains experience (summoned units do not)
		// Also when sending heroes from the server to the client, experience is sent in amongst the rest of the skill list, so we don't need to
		// handle it separately here - in this case, experience will be -1 or null
		if ((startingExperience != null) && (startingExperience >= 0))
		{
			final String unitTypeID = db.findPick (unitDefinition.getUnitMagicRealm (), "initializeUnitSkills").getUnitTypeID ();
			final UnitType unitType = db.findUnitType (unitTypeID, "initializeUnitSkills");

			if (unitType.getExperienceLevel ().size () > 0)
			{
				final UnitSkillAndValue exp = new UnitSkillAndValue ();
				exp.setUnitSkillID (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE);
				exp.setUnitSkillValue (startingExperience);
				unit.getUnitHasSkill ().add (exp);
			}
		}

		// Copy skills from DB
		for (final UnitSkillAndValue srcSkill : unitDefinition.getUnitHasSkill ())
		{
			final UnitSkillAndValue destSkill = new UnitSkillAndValue ();
			destSkill.setUnitSkillID (srcSkill.getUnitSkillID ());
			destSkill.setUnitSkillValue (srcSkill.getUnitSkillValue ());
			unit.getUnitHasSkill ().add (destSkill);
		}

		log.trace ("Entering initializeUnitSkills");
		return unitDefinition;
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
	public final int getBasicSkillValue (final List<UnitSkillAndValue> skills, final String unitSkillID)
	{
		int skillValue = -1;
		final Iterator<UnitSkillAndValue> iter = skills.iterator ();

		while ((skillValue < 0) && (iter.hasNext ()))
		{
			final UnitSkillAndValue thisSkill = iter.next ();
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
		final Iterator<UnitSkillAndValue> iter = unit.getUnitHasSkill ().iterator ();

		while ((!found) && (iter.hasNext ()))
		{
			final UnitSkillAndValue thisSkill = iter.next ();
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
		for (final UnitSkillAndValue thisSkill : unit.getUnitHasSkill ())
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
	 * @param db Lookup lists built over the XML database
	 * @return List of all skills this unit has, with skills gained from spells (both enchantments such as Holy Weapon and curses such as Vertigo) merged into the list
	 * @throws RecordNotFoundException If the definition of a spell that is cast on the unit cannot be found in the db
	 */
	@Override
	public final UnitHasSkillMergedList mergeSpellEffectsIntoSkillList (final List<MemoryMaintainedSpell> spells, final MemoryUnit unit, final CommonDatabase db)
		throws RecordNotFoundException
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
				final UnitSkillAndValue spellSkill = new UnitSkillAndValue ();
				spellSkill.setUnitSkillID (thisSpell.getUnitSkillID ());
				
				// Get the strength of this skill from the spell definition
				final Spell spellDef = db.findSpell (thisSpell.getSpellID (), "mergeSpellEffectsIntoSkillList");
				boolean found = false;
				final Iterator<UnitSpellEffect> iter = spellDef.getUnitSpellEffect ().iterator ();
				while ((!found) && (iter.hasNext ()))
				{
					final UnitSpellEffect effect = iter.next ();
					if (effect.getUnitSkillID ().equals (thisSpell.getUnitSkillID ()))
					{
						found = true;
						spellSkill.setUnitSkillValue (effect.getUnitSkillValue ());
					}
				}
				
				if (!found)
					throw new RecordNotFoundException ("UnitSpellEffect", thisSpell.getUnitSkillID (), "mergeSpellEffectsIntoSkillList");
				
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
		final int experienceSkillValue = getBasicSkillValue (unit.getUnitHasSkill (), CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE);

		final ExperienceLevel result;
		if (experienceSkillValue < 0)
			result = null;		// This type of unit doesn't gain experience (e.g. summoned)
		else
		{
			// Check all experience levels defined under the unit type
			// This checks them all so we aren't relying on them being defined in the correct orer
			final String unitMagicRealmID = db.findUnit (unit.getUnitID (), "getExperienceLevel").getUnitMagicRealm ();
			final String unitTypeID = db.findPick (unitMagicRealmID, "getExperienceLevel").getUnitTypeID ();
			final UnitType unitType = db.findUnitType (unitTypeID, "getExperienceLevel");

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
				final PlayerPublicDetails owningPlayer = getMultiplayerSessionUtils ().findPlayerWithID (players, unit.getOwningPlayerID (), "getExperienceLevel");
				final List<PlayerPick> picks = ((MomPersistentPlayerPublicKnowledge) owningPlayer.getPersistentPlayerPublicKnowledge ()).getPick ();
				if (getPlayerPickUtils ().getQuantityOfPick (picks, CommonDatabaseConstants.RETORT_ID_WARLORD) > 0)
					levelIncludingBonuses++;

				// Does the player have the Crusade CAE?
				if (getMemoryCombatAreaEffectUtils ().findCombatAreaEffect (combatAreaEffects, null, CommonDatabaseConstants.COMBAT_AREA_EFFECT_CRUSADE, unit.getOwningPlayerID ()) != null)
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
		{
			final MemoryUnit mu = (MemoryUnit) unit;
			if ((mu.getCombatLocation () != null) && (mu.getCombatPosition () != null) && (mu.getCombatHeading () != null) && (mu.getCombatSide () != null))
				combatLocation = (MapCoordinates3DEx) mu.getCombatLocation ();
			else
				combatLocation = null;
		}
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
	 * @throws MomException If no matching merger record exists when multiple lifeform type modifications apply
	 */
	@Override
	public final String getModifiedUnitMagicRealmLifeformTypeID (final AvailableUnit unit, final List<UnitSkillAndValue> skills,
		final List<MemoryMaintainedSpell> spells, final CommonDatabase db)
		throws RecordNotFoundException, MomException
	{
		log.trace ("Entering getModifiedUnitMagicRealmLifeformTypeID: " + unit.getUnitID ());

		// If its an actual unit, check if the caller pre-merged the list of skills with skills from spells, or if we need to do it here
		final List<UnitSkillAndValue> mergedSkills;
		if ((unit instanceof MemoryUnit) && (!(skills instanceof UnitHasSkillMergedList)))
			mergedSkills = mergeSpellEffectsIntoSkillList (spells, (MemoryUnit) unit, db);
		else
			mergedSkills = skills;

		// Check if any skills or spells override this
		final List<String> changedMagicRealmLifeformTypeIDs = new ArrayList<String> ();
		for (final UnitSkillAndValue thisSkill : mergedSkills)
		{
			final String changedMagicRealmLifeformTypeID = db.findUnitSkill (thisSkill.getUnitSkillID (), "getModifiedUnitMagicRealmLifeformTypeID").getChangesUnitToMagicRealm ();
			if (changedMagicRealmLifeformTypeID != null)
				changedMagicRealmLifeformTypeIDs.add (changedMagicRealmLifeformTypeID);
		}

		// Now what we do depends on how many modifications we found
		final String magicRealmLifeformTypeID;
		
		// No modifications - use value from unit definition, unaltered
		if (changedMagicRealmLifeformTypeIDs.size () == 0)
			magicRealmLifeformTypeID = db.findUnit (unit.getUnitID (), "getModifiedUnitMagicRealmLifeformTypeID").getUnitMagicRealm ();

		// Exactly one modification - use the value set by that skill (i.e. unit is Undead or Chaos Channeled)
		else if (changedMagicRealmLifeformTypeIDs.size () == 1)
			magicRealmLifeformTypeID = changedMagicRealmLifeformTypeIDs.get (0);
		
		// Multiple - look for a magic realm whose merge list matches our list (i.e. unit is Undead AND Chaos Channeled)
		else
		{
			final Iterator<? extends Pick> iter = db.getPicks ().iterator ();
			String match = null;
			while ((match == null) && (iter.hasNext ()))
			{
				final Pick pick = iter.next ();
				
				if (pick.getMergedFromPick ().size () == changedMagicRealmLifeformTypeIDs.size ())
				{
					boolean ok = true;
					for (final MergedFromPick mergedFromPick : pick.getMergedFromPick ())
						if (!changedMagicRealmLifeformTypeIDs.contains (mergedFromPick.getMergedFromPickID ()))
							ok = false;
				
					if (ok)
						match = pick.getPickID ();
				}
			}
			
			if (match != null)
				magicRealmLifeformTypeID = match;
			else
			{
				final StringBuilder msg = new StringBuilder ();
				changedMagicRealmLifeformTypeIDs.forEach (s ->
				{
					if (msg.length () > 0)
						msg.append (", ");
					
					msg.append (s);
				});
				
				throw new MomException ("No magic realm/lifeform type (Pick) found that merges lifeform types: " + msg);
			}
		}
		
		log.trace ("Exiting getModifiedUnitMagicRealmLifeformTypeID = " + magicRealmLifeformTypeID);
		return magicRealmLifeformTypeID;
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
		final Iterator<ProductionTypeAndUndoubledValue> iter = db.findUnit (unit.getUnitID (), "getBasicUpkeepValue").getUnitUpkeep ().iterator ();
		while ((result == 0) && (iter.hasNext ()))
		{
			final ProductionTypeAndUndoubledValue thisUpkeep = iter.next ();
			if (thisUpkeep.getProductionTypeID ().equals (productionTypeID))
				result = thisUpkeep.getUndoubledProductionValue ();
		}

		log.trace ("Exiting getBasicUpkeepValue = " + result);
		return result;
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

			if ((thisUnit.getStatus () == UnitStatusID.ALIVE) && (combatLocation.equals (thisUnit.getCombatLocation ())) && (combatPosition.equals (thisUnit.getCombatPosition ())) &&
				(thisUnit.getCombatSide () != null) && (thisUnit.getCombatHeading () != null))

				found = thisUnit;
		}

		log.trace ("Exiting findAliveUnitInCombatAt = " + found);
		return found;
	}
	
	/**
	 * Performs a deep copy (i.e. creates copies of every sub object rather than copying the references) of every field value from one unit to another
	 * @param source Unit to copy values from
	 * @param dest Unit to copy values to
	 * @param includeMovementFields Only the player who owns a unit can see its movement remaining and special orders
	 */
	@Override
	public final void copyUnitValues (final MemoryUnit source, final MemoryUnit dest, final boolean includeMovementFields)
	{
		log.trace ("Entering copyUnitValues: Unit URN" + source.getUnitURN ());
		
		// Destination values for a couple of movement related fields depend on input param
		final int newDoubleOverlandMovesLeft = includeMovementFields ? source.getDoubleOverlandMovesLeft () : 0;
		final Integer newDoubleCombatMovesLeft = includeMovementFields ? source.getDoubleCombatMovesLeft () : null;
		final UnitSpecialOrder newSpecialOrder = includeMovementFields ? source.getSpecialOrder () : null;
		
		// AvailableUnit fields
		dest.setOwningPlayerID (source.getOwningPlayerID ());
		dest.setUnitID (source.getUnitID ());
		dest.setWeaponGrade (source.getWeaponGrade ());

		if (source.getUnitLocation () == null)
			dest.setUnitLocation (null);
		else
			dest.setUnitLocation (new MapCoordinates3DEx ((MapCoordinates3DEx) source.getUnitLocation ()));

		// AvailableUnit - skills list
		dest.getUnitHasSkill ().clear ();
		for (final UnitSkillAndValue srcSkill : source.getUnitHasSkill ())
		{
			final UnitSkillAndValue destSkill = new UnitSkillAndValue ();
			destSkill.setUnitSkillID (srcSkill.getUnitSkillID ());
			destSkill.setUnitSkillValue (srcSkill.getUnitSkillValue ());
			dest.getUnitHasSkill ().add (destSkill);
		}

		// MemoryUnit fields
		dest.setUnitURN (source.getUnitURN ());
		dest.setHeroNameID (source.getHeroNameID ());
		dest.setUnitName (source.getUnitName ());
		dest.setAmmoRemaining (source.getAmmoRemaining ());
		
		dest.setManaRemaining (source.getManaRemaining ());
		dest.setDoubleOverlandMovesLeft (newDoubleOverlandMovesLeft);
		dest.setSpecialOrder (newSpecialOrder);
		dest.setStatus (source.getStatus ());
		dest.setWasSummonedInCombat (source.isWasSummonedInCombat ());
		dest.setCombatHeading (source.getCombatHeading ());
		dest.setCombatSide (source.getCombatSide ());
		dest.setDoubleCombatMovesLeft (newDoubleCombatMovesLeft);

		dest.getFixedSpellsRemaining ().clear ();
		dest.getFixedSpellsRemaining ().addAll (source.getFixedSpellsRemaining ());
		
		dest.getHeroItemSpellChargesRemaining ().clear ();
		dest.getHeroItemSpellChargesRemaining ().addAll (source.getHeroItemSpellChargesRemaining ());
		
		if (source.getCombatLocation () == null)
			dest.setCombatLocation (null);
		else
			dest.setCombatLocation (new MapCoordinates3DEx ((MapCoordinates3DEx) source.getCombatLocation ()));

		if (source.getCombatPosition () == null)
			dest.setCombatPosition (null);
		else
			dest.setCombatPosition (new MapCoordinates2DEx ((MapCoordinates2DEx) source.getCombatPosition ()));
		
		// MemoryUnit - hero item slots list
		dest.getHeroItemSlot ().clear ();
		
		source.getHeroItemSlot ().forEach (srcItemSlot ->
		{
			final MemoryUnitHeroItemSlot destItemSlot = new MemoryUnitHeroItemSlot ();
			if (srcItemSlot.getHeroItem () != null)
			{
				final NumberedHeroItem srcItem = srcItemSlot.getHeroItem ();
				final NumberedHeroItem destItem = new NumberedHeroItem ();
				
				destItem.setHeroItemURN (srcItem.getHeroItemURN ());
				destItem.setHeroItemName (srcItem.getHeroItemName ());
				destItem.setHeroItemTypeID (srcItem.getHeroItemTypeID ());
				destItem.setHeroItemImageNumber (srcItem.getHeroItemImageNumber ());
				destItem.setSpellID (srcItem.getSpellID ());
				destItem.setSpellChargeCount (srcItem.getSpellChargeCount ());
				
				srcItem.getHeroItemChosenBonus ().forEach (srcBonus ->
				{
					final HeroItemTypeAllowedBonus destBonus = new HeroItemTypeAllowedBonus ();
					destBonus.setHeroItemBonusID (srcBonus.getHeroItemBonusID ());
					destItem.getHeroItemChosenBonus ().add (destBonus);
				});
				
				destItemSlot.setHeroItem (destItem);
			}
			dest.getHeroItemSlot ().add (destItemSlot);
		});
		
		// Memory unit - damage
		dest.getUnitDamage ().clear ();
		source.getUnitDamage ().forEach (srcDamage ->
		{
			final UnitDamage destDamage = new UnitDamage ();
			destDamage.setDamageType (srcDamage.getDamageType ());
			destDamage.setDamageTaken (srcDamage.getDamageTaken ());
			dest.getUnitDamage ().add (destDamage);
		});
		
		log.trace ("Exiting copyUnitValues");
	}
	
	/**
	 * @param damages List of types of unit damage
	 * @return Total damage taken across all types
	 */
	@Override
	public final int getTotalDamageTaken (final List<UnitDamage> damages)
	{
		log.trace ("Entering getTotalDamageTaken");
		
		final int total = damages.stream ().mapToInt (d -> d.getDamageTaken ()).sum ();
		
		log.trace ("Exiting getTotalDamageTaken = " + total);
		return total;
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