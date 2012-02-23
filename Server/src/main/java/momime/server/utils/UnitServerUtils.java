package momime.server.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.calculations.MomUnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.UnitHasSkill;
import momime.common.messages.MemoryBuildingUtils;
import momime.common.messages.UnitUtils;
import momime.common.messages.v0_9_4.CombatMapCoordinates;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.common.messages.v0_9_4.UnitSpecialOrder;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.server.database.ServerDatabaseLookup;
import momime.server.database.v0_9_4.Unit;
import momime.server.database.v0_9_4.UnitSkill;
import momime.server.messages.v0_9_4.MomGeneralServerKnowledge;
import momime.server.process.FogOfWarProcessing;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Server side only helper methods for dealing with units
 */
public final class UnitServerUtils
{
	/**
	 * Used for copying units from true map into player's memory
	 * @param src Unit to copy
	 * @return Deep copy of unit and all its skills
	 */
	public final static MemoryUnit duplicateMemoryUnit (final MemoryUnit src)
	{
		final MemoryUnit dest = new MemoryUnit ();

		// AvailableUnit fields
		dest.setOwningPlayerID (src.getOwningPlayerID ());
		dest.setUnitID (src.getUnitID ());
		dest.setWeaponGrade (src.getWeaponGrade ());

		if (src.getUnitLocation () == null)
			dest.setUnitLocation (null);
		else
		{
			final OverlandMapCoordinates unitLocation = new OverlandMapCoordinates ();
			unitLocation.setX (src.getUnitLocation ().getX ());
			unitLocation.setY (src.getUnitLocation ().getY ());
			unitLocation.setPlane (src.getUnitLocation ().getPlane ());
			dest.setUnitLocation (unitLocation);
		}

		for (final UnitHasSkill srcSkill : src.getUnitHasSkill ())
		{
			final UnitHasSkill destSkill = new UnitHasSkill ();
			destSkill.setUnitSkillID (srcSkill.getUnitSkillID ());
			destSkill.setUnitSkillValue (srcSkill.getUnitSkillValue ());
			dest.getUnitHasSkill ().add (destSkill);
		}

		// MemoryUnit fields
		dest.setUnitURN (src.getUnitURN ());
	    dest.setHeroNameID (src.getHeroNameID ());
	    dest.setUnitName (src.getUnitName ());
	    dest.setRangedAttackAmmo (src.getRangedAttackAmmo ());
	    dest.setManaRemaining (src.getManaRemaining ());
	    dest.setDamageTaken (src.getDamageTaken ());
	    dest.setDoubleOverlandMovesLeft (src.getDoubleOverlandMovesLeft ());
	    dest.setSpecialOrder (src.getSpecialOrder ());
	    dest.setStatus (src.getStatus ());
	    dest.setWasSummonedInCombat (src.isWasSummonedInCombat ());
	    dest.setCombatHeading (src.getCombatHeading ());
	    dest.setCombatSide (src.getCombatSide ());
	    dest.setDoubleCombatMovesLeft (src.getDoubleCombatMovesLeft ());

		if (src.getCombatLocation () == null)
			dest.setCombatLocation (null);
		else
		{
			final OverlandMapCoordinates combatLocation = new OverlandMapCoordinates ();
			combatLocation.setX (src.getCombatLocation ().getX ());
			combatLocation.setY (src.getCombatLocation ().getY ());
			combatLocation.setPlane (src.getCombatLocation ().getPlane ());
			dest.setCombatLocation (combatLocation);
		}

		if (src.getCombatPosition () == null)
			dest.setCombatPosition (null);
		else
		{
			final CombatMapCoordinates combatPosition = new CombatMapCoordinates ();
			combatPosition.setX (src.getCombatPosition ().getX ());
			combatPosition.setY (src.getCombatPosition ().getY ());
			dest.setCombatPosition (combatPosition);
		}

		return dest;
	}

	/**
	 * Adds a unit to the server's true memory, and checks who can see it - sending a message to update the client of any human players who can see it
	 *
	 * Heroes are added using this method during game startup - at which point they're added only on the server and they're off
	 * the map (null location) - so heroes are NEVER sent to the client using this method, meaning that we don't need to worry about sending skill lists with this method
	 *
	 * @param gsk Server knowledge structure to add the unit to
	 * @param unitID Type of unit to create
	 * @param locationToAddUnit Location to add the new unit; can be null for adding heroes that haven't been summoned yet
	 * @param buildingsLocation Location the unit was built - might be different from locationToAddUnit if the city is full and the unit got bumped to an adjacent tile; passed as null for units not built in cities such as summons
	 * @param combatLocation The location of the combat that this unit is being summoned into; null for anything other than combat summons
	 * @param unitOwner Player who will own the new unit
	 * @param initialStatus Initial status of the unit, typically ALIVE
	 * @param session Dummy flag to control FOW updates until this is written properly
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Newly created unit
	 * @throws MomException If initialStatus is an inappropriate value
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 */
	public final static MemoryUnit addUnitOnServerAndClients (final MomGeneralServerKnowledge gsk,
		final String unitID, final OverlandMapCoordinates locationToAddUnit, final OverlandMapCoordinates buildingsLocation, final OverlandMapCoordinates combatLocation,
		final PlayerServerDetails unitOwner, final UnitStatusID initialStatus,
		final Integer session,
		final MomSessionDescription sd, final ServerDatabaseLookup db, final Logger debugLogger)
		throws MomException, RecordNotFoundException
	{
		debugLogger.entering (UnitServerUtils.class.getName (), "addUnitOnServerAndClients");

		// There's a bunch of other unit statuses that don't make sense to use here - so worth checking this
		if ((initialStatus != UnitStatusID.NOT_GENERATED) && (initialStatus != UnitStatusID.ALIVE))
			throw new MomException ("addUnitOnServerAndClients: Invalid initial status of " + initialStatus);

		final MomPersistentPlayerPublicKnowledge unitOwnerPPK = (MomPersistentPlayerPublicKnowledge) unitOwner.getPersistentPlayerPublicKnowledge ();

		// Check how much experience this unit should have
		// Note the reason we pass in buildingsLocation separately from locationToAddUnit is in case a city is full and a unit gets bumped
		// to the outside - it still needs to get the bonus from the buildings back in the city
		final int startingExperience;
		final Integer weaponGrade;
		if (buildingsLocation != null)
		{
			startingExperience = MemoryBuildingUtils.experienceFromBuildings (gsk.getTrueMap ().getBuilding (), buildingsLocation, db, debugLogger);
			weaponGrade = MomUnitCalculations.calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort
				(gsk.getTrueMap ().getBuilding (), gsk.getTrueMap ().getMap (), buildingsLocation, unitOwnerPPK.getPick (), sd.getMapSize (), db, debugLogger);
		}
		else
		{
			startingExperience = 0;
			weaponGrade = null;
		}

		// Add on server
		// Even for heroes, we load in their default skill list - this is how heroes default skills are loaded during game startup
		final MemoryUnit newUnit = UnitUtils.createMemoryUnit (unitID, gsk.getNextFreeUnitURN (), weaponGrade, startingExperience, true, db, debugLogger);

		gsk.setNextFreeUnitURN (gsk.getNextFreeUnitURN () + 1);

		newUnit.setOwningPlayerID (unitOwner.getPlayerDescription ().getPlayerID ());
		newUnit.setStatus (initialStatus);

		if (locationToAddUnit != null)
		{
			final OverlandMapCoordinates unitLocation = new OverlandMapCoordinates ();
			unitLocation.setX (locationToAddUnit.getX ());
			unitLocation.setY (locationToAddUnit.getY ());
			unitLocation.setPlane (locationToAddUnit.getPlane ());

			newUnit.setUnitLocation (unitLocation);
		}

		gsk.getTrueMap ().getUnit ().add (newUnit);

		// What can the new unit see? (it may expand the unit owner's vision to see things that they couldn't previously)
		if ((session != null) && (initialStatus.equals (UnitStatusID.ALIVE)) && (combatLocation == null))
			throw new MomException ("addUnitOnServerAndClients: Functionality to check area new unit can see not yet written");

		// Tell clients?
		// Player list can be null, we use this for pre-adding units to the map before the fog of war has even been set up
		if ((session != null) && (initialStatus.equals (UnitStatusID.ALIVE)))
			throw new MomException ("addUnitOnServerAndClients: Functionality to check which players can see the new unit not yet written");

		debugLogger.exiting (UnitServerUtils.class.getName (), "addUnitOnServerAndClients", newUnit.getUnitURN ());
		return newUnit;
	}

	/**
	 * Chooses a name for this hero (out of 5 possibilities) and rolls their random skills
	 * @param unit The hero to generate name and skills for
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws MomException If we find a hero who has no possible names defined, or who needs a random skill and we can't find a suitable one
	 * @throws RecordNotFoundException If we can't find the definition for the unit
	 */
	public static final void generateHeroNameAndRandomSkills (final MemoryUnit unit, final ServerDatabaseLookup db, final Logger debugLogger)
		throws MomException, RecordNotFoundException
	{
		debugLogger.entering (UnitServerUtils.class.getName (), "generateHeroNameAndRandomSkills", unit.getUnitID ());

		final Unit unitDefinition = db.findUnit (unit.getUnitID (), "generateHeroNameAndRandomSkills");

		// Pick a name at random
		if (unitDefinition.getHeroName ().size () == 0)
			throw new MomException ("No hero names found for unit ID \"" + unit.getUnitID () + "\"");

		unit.setHeroNameID (unitDefinition.getHeroName ().get (RandomUtils.getGenerator ().nextInt (unitDefinition.getHeroName ().size ())).getHeroNameID ());

		// Any random skills to add?
		if ((unitDefinition.getHeroRandomPickCount () != null) && (unitDefinition.getHeroRandomPickCount () > 0))
		{
			debugLogger.finest ("Hero " + unit.getUnitID () + "' belonging to player ID " + unit.getOwningPlayerID () + " skills before rolling extras: " + UnitUtils.describeBasicSkillValuesInDebugString (unit));

			// Run once for each random skill we get
			for (int pickNo = 0; pickNo < unitDefinition.getHeroRandomPickCount (); pickNo++)
			{
				// Get a list of all valid choices
				final List<String> skillChoices = new ArrayList<String> ();
				for (final UnitSkill thisSkill : db.getUnitSkills ())
				{
					// We can spot hero skills since they'll have at least one of these values filled in
					if (((thisSkill.getHeroSkillTypeID () != null) && (!thisSkill.getHeroSkillTypeID ().equals (""))) ||
						((thisSkill.isOnlyIfHaveAlready () != null) && (thisSkill.isOnlyIfHaveAlready ())) ||
						((thisSkill.getMaxOccurrences () != null) && (thisSkill.getMaxOccurrences () > 0)))
					{
						// Its a hero skill - do we have it already?
						final int currentSkillValue = UnitUtils.getBasicSkillValue (unit.getUnitHasSkill (), thisSkill.getUnitSkillID ());

						// Is it applicable?
						// If the unit has no hero random pick type specified, then it means it can use any hero skills
						// If the skill has no hero random pick type specified, then it means it can be used by any units
						if (((unitDefinition.getHeroRandomPickType () == null) || (thisSkill.getHeroSkillTypeID () == null) || (unitDefinition.getHeroRandomPickType ().equals (thisSkill.getHeroSkillTypeID ()))) &&
							((thisSkill.isOnlyIfHaveAlready () == null) || (!thisSkill.isOnlyIfHaveAlready ()) || (currentSkillValue > 0)) &&
							((thisSkill.getMaxOccurrences () == null) || (currentSkillValue < thisSkill.getMaxOccurrences ())))

								skillChoices.add (thisSkill.getUnitSkillID ());
					}
				}

				// Pick one
				if (skillChoices.size () == 0)
					throw new MomException ("generateHeroNameAndRandomSkills: No valid hero skill choices for unit " + unit.getUnitID ());

				final String skillID = skillChoices.get (RandomUtils.getGenerator ().nextInt (skillChoices.size ()));
				final int currentSkillValue = UnitUtils.getBasicSkillValue (unit.getUnitHasSkill (), skillID);
				if (currentSkillValue >= 0)
					UnitUtils.setBasicSkillValue (unit, skillID, currentSkillValue + 1, debugLogger);
				else
				{
					final UnitHasSkill newSkill = new UnitHasSkill ();
					newSkill.setUnitSkillID (skillID);
					newSkill.setUnitSkillValue (1);
					unit.getUnitHasSkill ().add (newSkill);
				}

				debugLogger.finest ("Hero " + unit.getUnitID () + "' belonging to player ID " + unit.getOwningPlayerID () + " skills after rolling extras: " + UnitUtils.describeBasicSkillValuesInDebugString (unit));
			}
		}

		// Update status
		unit.setStatus (UnitStatusID.GENERATED);

		debugLogger.exiting (UnitServerUtils.class.getName (), "generateHeroNameAndRandomSkills");
	}

	/**
	 * @param order Special order that a unit has
	 * @return True if this special order results in the unit dying/being removed from play
	 */
	public static final boolean doesUnitSpecialOrderResultInDeath (final UnitSpecialOrder order)
	{
		return (order == UnitSpecialOrder.BUILD_CITY) || (order == UnitSpecialOrder.MELD_WITH_NODE) || (order == UnitSpecialOrder.DISMISS);
	}

	/**
	 *
	 * @param trueUnits True list of units to heal/gain experience
	 * @param onlyOnePlayerID If zero, will heal/exp units belonging to all players; if specified will heal/exp only units belonging to the specified player
	 * @param trueTerrain True terrain map
	 * @param players List of players in the session
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found, or the player should be able to see the unit but it isn't in their list
	 * @throws PlayerNotFoundException If the player who owns the unit cannot be found
	 * @throws MomException If the player's unit doesn't have the experience skill
	 */
	public final static void healUnitsAndGainExperience (final List<MemoryUnit> trueUnits, final int onlyOnePlayerID, final MapVolumeOfMemoryGridCells trueTerrain,
		final List<PlayerServerDetails> players, final ServerDatabaseLookup db, final MomSessionDescription sd, final Logger debugLogger)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		debugLogger.entering (UnitServerUtils.class.getName (), "healUnitsAndGainExperience", onlyOnePlayerID);

		for (final MemoryUnit thisUnit : trueUnits)
			if ((thisUnit.getStatus () == UnitStatusID.ALIVE) && ((onlyOnePlayerID == 0) || (onlyOnePlayerID == thisUnit.getOwningPlayerID ())))
			{
				boolean sendMsg = false;

				// Heal?
				if (thisUnit.getDamageTaken () > 0)
				{
					thisUnit.setDamageTaken (thisUnit.getDamageTaken () - 1);
					sendMsg = true;
				}

				// Experience?
				final int exp = UnitUtils.getBasicSkillValue (thisUnit.getUnitHasSkill (), CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE);
				if (exp >= 0)
				{
					UnitUtils.setBasicSkillValue (thisUnit, CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE, exp + 1, debugLogger);
					sendMsg = true;
				}

				// Inform any clients who know about this unit
				if (sendMsg)
					FogOfWarProcessing.updatePlayerMemoryOfUnit_DamageTakenAndExperience (thisUnit, trueTerrain, players, db, sd, debugLogger);
			}

		debugLogger.exiting (UnitServerUtils.class.getName (), "healUnitsAndGainExperience");
	}

	/**
	 * Prevent instantiation
	 */
	private UnitServerUtils ()
	{
	}
}
