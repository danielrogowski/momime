package momime.common.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.AddsToSkill;
import momime.common.database.CombatAreaAffectsPlayersID;
import momime.common.database.CombatAreaEffect;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.NegatedBySkill;
import momime.common.database.NegatedByUnitID;
import momime.common.database.RecordNotFoundException;
import momime.common.database.StoredDamageTypeID;
import momime.common.database.UnitEx;
import momime.common.database.UnitSkillAndValue;
import momime.common.database.UnitSkillEx;
import momime.common.database.UnitSpecialOrder;
import momime.common.database.UnitSpellEffect;
import momime.common.database.UnitType;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MemoryUnitHeroItemSlot;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.UnitDamage;
import momime.common.messages.UnitStatusID;

/**
 * Simple unit lookups of basic skill, attribute and upkeep values
 */
public final class UnitUtilsImpl implements UnitUtils
{
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/**
	 * @param unitURN Unit URN to search for
	 * @param units List of units to search through
	 * @return Unit with requested URN, or null if not found
	 */
	@Override
	public final MemoryUnit findUnitURN (final int unitURN, final List<MemoryUnit> units)
	{
		MemoryUnit result = null;
		final Iterator<MemoryUnit> iter = units.iterator ();
		while ((result == null) && (iter.hasNext ()))
		{
			final MemoryUnit thisUnit = iter.next ();
			if (thisUnit.getUnitURN () == unitURN)
				result = thisUnit;
		}

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
		final MemoryUnit result = findUnitURN (unitURN, units);

		if (result == null)
			throw new RecordNotFoundException (MemoryUnit.class, unitURN, caller);

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
	public final UnitEx initializeUnitSkills (final AvailableUnit unit, final Integer startingExperience, final CommonDatabase db) throws RecordNotFoundException
	{
		final UnitEx unitDefinition = db.findUnit (unit.getUnitID (), "initializeUnitSkills");

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

		return unitDefinition;
	}

	/**
	 * @param unitSkillID Unit skill we want to check for
	 * @param ourSkillValues List of skills the unit has
	 * @param enemyUnits List of enemy units who may have skills that negate the skill we're checking for; typically this is the unit we're engaging in an attack with; in some
	 * 	cases such as Invisibility, it may be ALL units we're in combat with; for situations not involved in combats or specific attacks, just pass null here
	 * @param db Lookup lists built over the XML database
	 * @return Whether the skill is negated or not
	 * @throws RecordNotFoundException If we can't find the skill definition
	 * @throws MomException If the skill definition has an unknown negatedByUnitID value
	 */
	@Override
	public final boolean isSkillNegated (final String unitSkillID, final Map<String, ? extends Object> ourSkillValues, final List<ExpandedUnitDetails> enemyUnits,
		final CommonDatabase db) throws RecordNotFoundException, MomException
	{
		final UnitSkillEx skillDef = db.findUnitSkill (unitSkillID, "isSkillNegated");
		final Iterator<NegatedBySkill> iter = skillDef.getNegatedBySkill ().iterator ();
		boolean negated = false;
		
		while ((!negated) && (iter.hasNext ()))
		{
			final NegatedBySkill negation = iter.next ();
			switch (negation.getNegatedByUnitID ())
			{
				case OUR_UNIT:
					if (ourSkillValues.containsKey (negation.getNegatedBySkillID ()))
						negated = true;
					break;
					
				case ENEMY_UNIT:
					if (enemyUnits != null)
						negated = enemyUnits.stream ().anyMatch (e -> e.hasModifiedSkill (negation.getNegatedBySkillID ()));
					break;
					
				default:
					throw new MomException ("isSkillNegated doesn't know what to do with negatedByUnitID value of " +
						negation.getNegatedByUnitID () + " when determining value of skill " + unitSkillID);
			}
		}
		
		return negated;
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

		return applies;
	}

	/**
	 * @param units Unit stack
	 * @return Comma delimited list of their unit URNs, for debug messages
	 * @throws MomException If the list includes something other than MemoryUnits or ExpandedUnitDetails
	 */
	@Override
	public String listUnitURNs (@SuppressWarnings ("rawtypes") final List units) throws MomException
	{
		final StringBuilder list = new StringBuilder ();
		if (units != null)
			for (final Object thisUnit : units)
			{
				if (list.length () > 0)
					list.append (", ");

				if (thisUnit instanceof MemoryUnit)
					list.append (((MemoryUnit) thisUnit).getUnitURN ());
				else if (thisUnit instanceof ExpandedUnitDetails)
				{
					final ExpandedUnitDetails xu = (ExpandedUnitDetails) thisUnit;
					if (xu.isMemoryUnit ())
						list.append (xu.getUnitURN ());
				}
				else
					throw new MomException ("listUnitURNs got an object of type " + thisUnit.getClass ());
			}

		return "(" + list + ")";
	}

	/**
	 * Will find units even if they're invisible
	 * 
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

		return found;
	}

	/**
	 * @param ourPlayerID Our player ID
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param x X coordinate of location to check
	 * @param y Y coordinate of location to check
	 * @param plane Plane to check
	 * @param exceptPlayerID Player who's units to not consider (can pass in 0 to test if there are units *at all* at this location)
	 * @param db Lookup lists built over the XML database
	 * @return First unit we find at the requested location who belongs to someone other than the specified player
	 */
	@Override
	public final MemoryUnit findFirstAliveEnemyWeCanSeeAtLocation (final int ourPlayerID, final FogOfWarMemory mem, final int x, final int y, final int plane,
		final int exceptPlayerID, final CommonDatabase db)
	{
		MemoryUnit found = null;
		final Iterator<MemoryUnit> iter = mem.getUnit ().iterator ();
		while ((found == null) && (iter.hasNext ()))
		{
			final MemoryUnit thisUnit = iter.next ();

			if ((thisUnit.getStatus () == UnitStatusID.ALIVE) && (thisUnit.getOwningPlayerID () != exceptPlayerID) && (thisUnit.getUnitLocation () != null) &&
				(thisUnit.getUnitLocation ().getX () == x) && (thisUnit.getUnitLocation ().getY () == y) && (thisUnit.getUnitLocation ().getZ () == plane) &&
				(canSeeUnitOverland (thisUnit, ourPlayerID, mem.getMaintainedSpell (), db)))

				found = thisUnit;
		}

		return found;
	}

	/**
	 * Lists the enemy units at a specified location on the overland map, regardless of whether they might be invisible or not.
	 * 
	 * @param units List of units to check
	 * @param x X coordinate of location to check
	 * @param y Y coordinate of location to check
	 * @param plane Plane to check
	 * @param exceptPlayerID Player who's units to not consider (can pass in 0 to count *all* units at this location)
	 * @return Number of units that we find at the requested location who belongs to someone other than the specified player
	 */
	@Override
	public final List<MemoryUnit> listAliveEnemiesAtLocation (final List<MemoryUnit> units, final int x, final int y, final int plane, final int exceptPlayerID)
	{
		final List<MemoryUnit> list = new ArrayList<MemoryUnit> ();
		
		for (final MemoryUnit thisUnit : units)
			if ((thisUnit.getStatus () == UnitStatusID.ALIVE) && (thisUnit.getOwningPlayerID () != exceptPlayerID) && (thisUnit.getUnitLocation () != null) &&
				(thisUnit.getUnitLocation ().getX () == x) && (thisUnit.getUnitLocation ().getY () == y) && (thisUnit.getUnitLocation ().getZ () == plane))

				list.add (thisUnit);
		
		return list;
	}
	
	/**
	 * Counts the number of enemy units at a specified location on the overland map, regardless of whether they might be invisible or not.
	 * 
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
		// Could just call listAliveEnemiesAtLocation ().size () but maybe fraction faster if we don't
		int count = 0;
		for (final MemoryUnit thisUnit : units)
		{
			if ((thisUnit.getStatus () == UnitStatusID.ALIVE) && (thisUnit.getOwningPlayerID () != exceptPlayerID) && (thisUnit.getUnitLocation () != null) &&
				(thisUnit.getUnitLocation ().getX () == x) && (thisUnit.getUnitLocation ().getY () == y) && (thisUnit.getUnitLocation ().getZ () == plane))

				count++;
		}

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
		final Iterator<MemoryMaintainedSpell> iter = mem.getMaintainedSpell ().iterator ();
		while (iter.hasNext ())
		{
			final MemoryMaintainedSpell spell = iter.next ();
			if ((spell.getUnitURN () != null) && (spell.getUnitURN () == unitURN))
				iter.remove ();
		}
	}

	/**
	 * @param combatLocation Location on overland map where the combat is taking place
	 * @param combatPosition Position within the combat map to look at
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Unit at this position, or null if there isn't one
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final ExpandedUnitDetails findAliveUnitInCombatAt (final MapCoordinates3DEx combatLocation,
		final MapCoordinates2DEx combatPosition, final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem, final CommonDatabase db)
		throws PlayerNotFoundException, RecordNotFoundException, MomException
	{
		ExpandedUnitDetails found = null;
		final Iterator<MemoryUnit> iter = mem.getUnit ().iterator ();
		while ((found == null) && (iter.hasNext ()))
		{
			final MemoryUnit thisUnit = iter.next ();

			if ((thisUnit.getStatus () == UnitStatusID.ALIVE) && (combatLocation.equals (thisUnit.getCombatLocation ())) && (combatPosition.equals (thisUnit.getCombatPosition ())) &&
				(thisUnit.getCombatSide () != null) && (thisUnit.getCombatHeading () != null))
			{
				final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (thisUnit, null, null, null, players, mem, db);
				if (!xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_MOVE_THROUGH_UNITS))
					found = xu;
			}
		}

		return found;
	}

	/**
	 * findAliveUnitInCombatAt will still return units we cannot see because they're invisible.  This adds that check.  So for example if we have a unit
	 * adjacent to an invisible unit, we can still "see" it and this method will return it.
	 * 
	 * @param combatLocation Location on overland map where the combat is taking place
	 * @param combatPosition Position within the combat map to look at
	 * @param ourPlayerID Our player ID
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @return Unit at this position, or null if there isn't one, or if there is one but we can't see it
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final ExpandedUnitDetails findAliveUnitInCombatWeCanSeeAt (final MapCoordinates3DEx combatLocation, final MapCoordinates2DEx combatPosition,
		final int ourPlayerID, final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem, final CommonDatabase db,
		final CoordinateSystem combatMapCoordinateSystem)
		throws PlayerNotFoundException, RecordNotFoundException, MomException
	{
		ExpandedUnitDetails xu = findAliveUnitInCombatAt (combatLocation, combatPosition, players, mem, db);
		
		if (xu != null)
			if (!canSeeUnitInCombat (xu, ourPlayerID, players, mem, db, combatMapCoordinateSystem))
				xu = null;
		
		return xu;
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
		dest.setConfusionEffect (source.getConfusionEffect ());

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
				
				destItem.getHeroItemChosenBonus ().addAll (srcItem.getHeroItemChosenBonus ());
				
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
	}
	
	/**
	 * @param damages List of types of unit damage
	 * @return Total damage taken across all types
	 */
	@Override
	public final int getTotalDamageTaken (final List<UnitDamage> damages)
	{
		final int total = damages.stream ().mapToInt (d -> d.getDamageTaken ()).sum ();
		return total;
	}
	
	/**
	 * @param damages List of types of unit damage
	 * @return Total damage taken across all types, excluding PERMANENT
	 */
	@Override
	public final int getHealableDamageTaken (final List<UnitDamage> damages)
	{
		final int total = damages.stream ().filter (d -> d.getDamageType () != StoredDamageTypeID.PERMANENT).mapToInt (d -> d.getDamageTaken ()).sum ();
		return total;
	}
	
	/**
	 * Whether a unit can be seen *at all* in combat.  So this isn't simply asking whether it has the Invisibility skill and whether we have
	 * True Sight or Immunity to Illusions to negate it.  Even if a unit is invisible, we can still see it if we have one of our units adjacent to it.
	 * 
	 * So for a unit to be completely hidden in combat it must:
	 * 1) not be ours AND
	 * 2) be invisible (either natively, from Invisibility spell, or from Mass Invisible CAE) AND
	 * 3) we must have no unit with True Sight or Immunity to Illusions AND
	 * 4) we must have no unit adjacent to it
	 * 
	 * @param xu Unit present on the combat map
	 * @param ourPlayerID Our player ID
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @return Whether we can see it or its completely hidden
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final boolean canSeeUnitInCombat (final ExpandedUnitDetails xu, final int ourPlayerID,
		final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem, final CommonDatabase db,
		final CoordinateSystem combatMapCoordinateSystem)
		throws MomException, RecordNotFoundException, PlayerNotFoundException
	{
		boolean invisible = false;
		if (xu.getOwningPlayerID () != ourPlayerID)
		{
			// expandUnitDetails takes care of granting invisibility spell from Mass Invisibility CAE, so we don't need to check for that here
			for (final String invisibilitySkillkID : CommonDatabaseConstants.UNIT_SKILL_IDS_INVISIBILITY)
				if (xu.hasModifiedSkill (invisibilitySkillkID))
					invisible = true;
			
			if (invisible)
			{
				final List<String> skillsThatNegateInvisibility = db.findUnitSkill
					(CommonDatabaseConstants.UNIT_SKILL_IDS_INVISIBILITY.get (0), "canSeeUnitInCombat").getNegatedBySkill ().stream ().filter
						(n -> n.getNegatedByUnitID () == NegatedByUnitID.ENEMY_UNIT).map (n -> n.getNegatedBySkillID ()).collect (Collectors.toList ());
				
				// Look through our units who are also in the combat looking for one which has True Sight or Immunity to Illusions or is adjacent to the enemy unit
				final Iterator<MemoryUnit> iter = mem.getUnit ().iterator ();
				while ((invisible) && (iter.hasNext ()))
				{
					final MemoryUnit thisUnit = iter.next ();
					if ((thisUnit.getOwningPlayerID () == ourPlayerID) && (thisUnit.getStatus () == UnitStatusID.ALIVE) &&
						(thisUnit.getCombatPosition () != null) && (thisUnit.getCombatHeading () != null) && (thisUnit.getCombatSide () != null) &&
						(xu.getCombatLocation ().equals (thisUnit.getCombatLocation ())))
					{
						// Check for adjacency first since its quicker
						if (getCoordinateSystemUtils ().determineStep2DDistanceBetween (combatMapCoordinateSystem,
							xu.getCombatPosition (), (MapCoordinates2DEx) thisUnit.getCombatPosition ()) <= 1)
							
							invisible = false;
						else
						{
							final ExpandedUnitDetails ourXU = getExpandUnitDetails ().expandUnitDetails (thisUnit, null, null, null, players, mem, db);
							for (final String negatingSkillID : skillsThatNegateInvisibility)
								if (ourXU.hasModifiedSkill (negatingSkillID))
									invisible = false;
						}
					}
				}
			}
		}
		
		return !invisible;
	}
	
	/**
	 * Needed to test whether to draw units on the overland map.  Calling expandUnitDetails continually is too
	 * expensive so need a quicker way to check whether units are invisible or not.
	 * 
	 * @param mu Unit to test
	 * @param ourPlayerID Our player ID
	 * @param spells Known spells
	 * @param db Lookup lists built over the XML database
	 * @return Whether the unit should be visible on the overland map
	 */
	@Override
	public final boolean canSeeUnitOverland (final MemoryUnit mu, final int ourPlayerID, final List<MemoryMaintainedSpell> spells, final CommonDatabase db)
	{
		final boolean visible;
		
		if (mu.getOwningPlayerID () == ourPlayerID)
			visible = true;
		
		else if (mu.getUnitHasSkill ().stream ().anyMatch (s -> s.getUnitSkillID ().equals (CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY)))
			visible = false;
		
		else if (getMemoryMaintainedSpellUtils ().findMaintainedSpell (spells,
			null, null, mu.getUnitURN (), CommonDatabaseConstants.UNIT_SKILL_ID_INVISIBILITY_FROM_SPELL, null, null) != null)
			
			visible = false;
		else
			visible = mu.getHeroItemSlot ().stream ().filter (s -> s.getHeroItem () != null).noneMatch
				(s -> s.getHeroItem ().getHeroItemChosenBonus ().contains (db.getInvisibilityHeroItemBonusID ()));
		
		return visible;
	}
	
	/**
	 * @param xu Unit to test
	 * @param unitSpellEffects List of unit skills to test
	 * @param db Lookup lists built over the XML database
	 * @return True if the unit is immune to all listed effects, false if we find at least one it isn't immune to
	 * @throws RecordNotFoundException If we can't find definition for one of the skills
	 */
	@Override
	public final boolean isUnitImmuneToSpellEffects (final ExpandedUnitDetails xu, final List<UnitSpellEffect> unitSpellEffects, final CommonDatabase db)
		throws RecordNotFoundException
	{
		boolean immuneToAll = true;
		
		for (final UnitSpellEffect effect : unitSpellEffects)
		{
			final UnitSkillEx unitSkill = db.findUnitSkill (effect.getUnitSkillID (), "isUnitImmuneToSpellEffects");
			
			boolean negated = false;
			for (final NegatedBySkill negatedBySkill : unitSkill.getNegatedBySkill ())
				if ((negatedBySkill.getNegatedByUnitID () == NegatedByUnitID.OUR_UNIT) && (xu.hasModifiedSkill (negatedBySkill.getNegatedBySkillID ())))
					negated = true;
			
			if (!negated)
				immuneToAll = false;
		}
		
		return immuneToAll;
	}
	
	/**
	 * Used to stop casting Heroism on units that already have 120 exp naturally
	 * 
	 * @param xu Unit to test
	 * @param unitSpellEffects List of unit skills to test
	 * @param db Lookup lists built over the XML database
	 * @return True if the unit skills give a boost to experience, but the unit already has that much experience naturally
	 * @throws RecordNotFoundException If we can't find definition for one of the skills
	 * @throws MomException If the unit doesn't have an experience value (but checks for this, so should never happen)
	 */
	@Override
	public final boolean isExperienceBonusAndWeAlreadyHaveTooMuch (final ExpandedUnitDetails xu, final List<UnitSpellEffect> unitSpellEffects, final CommonDatabase db)
		throws RecordNotFoundException, MomException
	{
		boolean result = false;
		
		for (final UnitSpellEffect effect : unitSpellEffects)
		{
			final UnitSkillEx unitSkill = db.findUnitSkill (effect.getUnitSkillID (), "isExperienceBonusAndWeAlreadyHaveTooMuch");
			for (final AddsToSkill addsToSkill : unitSkill.getAddsToSkill ())
				if ((addsToSkill.getAddsToSkillID ().equals (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE)) &&
					(xu.hasBasicSkill (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE)) &&
					(xu.getBasicSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE) >= addsToSkill.getAddsToSkillValue ()))
					
					result = true;
		}
		
		return result;
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
	 * @return MemoryMaintainedSpell utils
	 */
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param spellUtils MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils spellUtils)
	{
		memoryMaintainedSpellUtils = spellUtils;
	}

	/**
	 * @return expandUnitDetails method
	 */
	public final ExpandUnitDetails getExpandUnitDetails ()
	{
		return expandUnitDetails;
	}

	/**
	 * @param e expandUnitDetails method
	 */
	public final void setExpandUnitDetails (final ExpandUnitDetails e)
	{
		expandUnitDetails = e;
	}
}