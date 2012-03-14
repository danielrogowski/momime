package momime.server.fogofwar;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.UnitHasSkill;
import momime.common.messages.CoordinatesUtils;
import momime.common.messages.MemoryBuildingUtils;
import momime.common.messages.MemoryCombatAreaEffectUtils;
import momime.common.messages.MemoryMaintainedSpellUtils;
import momime.common.messages.UnitUtils;
import momime.common.messages.servertoclient.v0_9_4.AddMaintainedSpellMessageData;
import momime.common.messages.servertoclient.v0_9_4.AddUnitMessageData;
import momime.common.messages.v0_9_4.CombatMapCoordinates;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.MemoryCombatAreaEffect;
import momime.common.messages.v0_9_4.MemoryGridCell;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.server.database.ServerDatabaseLookup;
import momime.server.utils.CompareUtils;

/**
 * Methods for comparing and copying data from one source against a destination container
 * This is used for copying data from the server's true memory into player's memory
 *
 * Note these must always make deep copies - if an object includes another object (e.g. OverlandMapCoordinates or UnitHasSkill) then
 * we need to make a copy of the child object and reference that as well, we can't just copy the reference from source
 */
final class FogOfWarDuplication
{
	/**
	 * Copies all the terrain and node aura related data items from source to destination
	 * @param source The map cell to copy from
	 * @param destination The map cell to copy to
	 * @return Whether any update actually happened (i.e. false if source and destination already had the same info)
	 */
	final static boolean copyTerrainAndNodeAura (final MemoryGridCell source, final MemoryGridCell destination)
	{
		final OverlandMapTerrainData sourceData = source.getTerrainData ();
		OverlandMapTerrainData destinationData = destination.getTerrainData ();

		final boolean updateRequired = (destinationData == null) ||
			(!CompareUtils.safeStringCompare (sourceData.getTileTypeID (), destinationData.getTileTypeID ())) ||
			(!CompareUtils.safeStringCompare (sourceData.getMapFeatureID (), destinationData.getMapFeatureID ())) ||
			(!CompareUtils.safeStringCompare (sourceData.getRiverDirections (), destinationData.getRiverDirections ())) ||
			(!CompareUtils.safeIntegerCompare (sourceData.getNodeOwnerID (), destinationData.getNodeOwnerID ()));

		if (updateRequired)
		{
			if (destinationData == null)
			{
				destinationData = new OverlandMapTerrainData ();
				destination.setTerrainData (destinationData);
			}

			destinationData.setTileTypeID (sourceData.getTileTypeID ());
			destinationData.setMapFeatureID (sourceData.getMapFeatureID ());
			destinationData.setRiverDirections (sourceData.getRiverDirections ());
			destinationData.setNodeOwnerID (sourceData.getNodeOwnerID ());
		}

		return updateRequired;
	}

	/**
	 * Wipes all memory of the terrain at this location
	 * @param destination Map cell from player's memorized map
	 * @return True if an actual update was made; false if the player already knew nothing
	 */
	final static boolean blankTerrainAndNodeAura (final MemoryGridCell destination)
	{
		final OverlandMapTerrainData destinationData = destination.getTerrainData ();

		final boolean updateRequired = (destinationData != null) &&
			((destinationData.getTileTypeID () != null) || (destinationData.getMapFeatureID () != null) ||
			 (destinationData.getRiverDirections () != null) || (destinationData.getNodeOwnerID () != null));

		destination.setTerrainData (null);

		return updateRequired;
	}

	/**
	 * Copies all the city related data items from source to destination
	 * @param source The map cell to copy from
	 * @param destination The map cell to copy to
	 * @param includeCurrentlyConstructing Whether to copy currentlyConstructing from source to destination or null it out
	 * @return Whether any update actually happened (i.e. false if source and destination already had the same info)
	 */
	final static boolean copyCityData (final MemoryGridCell source, final MemoryGridCell destination, final boolean includeCurrentlyConstructing)
	{
		// Careful, may not even be a city here and hence source.getCityData () may be null
		// That's a valid scenario - maybe last time we saw this location there was a city here, but since then someone captured and razed it
		// In that case we're better to use the other routine
		final boolean updateRequired;

		final OverlandMapCityData sourceData = source.getCityData ();
		if (sourceData == null)
			updateRequired = blankCityData (destination);
		else
		{
			OverlandMapCityData destinationData = destination.getCityData ();

			final String newCurrentlyConstructing;
			if (includeCurrentlyConstructing)
				newCurrentlyConstructing = source.getCityData ().getCurrentlyConstructingBuildingOrUnitID ();
			else
				newCurrentlyConstructing = null;

			updateRequired = (destinationData == null) ||
				(!CompareUtils.safeIntegerCompare (sourceData.getCityPopulation (), destinationData.getCityPopulation ())) ||
				(!CompareUtils.safeIntegerCompare (sourceData.getMinimumFarmers (),  destinationData.getMinimumFarmers ())) ||
				(!CompareUtils.safeIntegerCompare (sourceData.getOptionalFarmers (), destinationData.getOptionalFarmers ())) ||
				(!CompareUtils.safeIntegerCompare (sourceData.getNumberOfRebels (), destinationData.getNumberOfRebels ())) ||
				(!CompareUtils.safeIntegerCompare (sourceData.getCityOwnerID (), destinationData.getCityOwnerID ())) ||
				(!CompareUtils.safeStringCompare (sourceData.getCityRaceID (), destinationData.getCityRaceID ())) ||
				(!CompareUtils.safeStringCompare (sourceData.getCitySizeID (), destinationData.getCitySizeID ())) ||
				(!CompareUtils.safeStringCompare (sourceData.getCityName (), destinationData.getCityName ())) ||
				(!CompareUtils.safeStringCompare (newCurrentlyConstructing, destinationData.getCurrentlyConstructingBuildingOrUnitID ()));

			if (updateRequired)
			{
				if (destinationData == null)
				{
					destinationData = new OverlandMapCityData ();
					destination.setCityData (destinationData);
				}

				destinationData.setCityPopulation (sourceData.getCityPopulation ());
				destinationData.setMinimumFarmers (sourceData.getMinimumFarmers ());
				destinationData.setOptionalFarmers (sourceData.getOptionalFarmers ());
				destinationData.setNumberOfRebels (sourceData.getNumberOfRebels ());
				destinationData.setCityOwnerID (sourceData.getCityOwnerID ());
				destinationData.setCityRaceID (sourceData.getCityRaceID ());
				destinationData.setCitySizeID (sourceData.getCitySizeID ());
				destinationData.setCityName (sourceData.getCityName ());
				destinationData.setCurrentlyConstructingBuildingOrUnitID (newCurrentlyConstructing);
			}
		}

		return updateRequired;
	}

	/**
	 * Wipes all memory of the city at this location
	 * @param destination Map cell from player's memorized map
	 * @return True if an actual update was made; false if the player already knew nothing
	 */
	final static boolean blankCityData (final MemoryGridCell destination)
	{
		final OverlandMapCityData destinationData = destination.getCityData ();

		final boolean updateRequired = (destinationData != null) &&
			((destinationData.getCityPopulation () != null) || (destinationData.getMinimumFarmers () != null) || (destinationData.getOptionalFarmers () != null) ||
			 (destinationData.getNumberOfRebels () != null) || (destinationData.getCityOwnerID () != null) || (destinationData.getCityRaceID () != null) ||
			 (destinationData.getCitySizeID () != null) || (destinationData.getCityName () != null) || (destinationData.getCurrentlyConstructingBuildingOrUnitID () != null));

		destination.setCityData (null);

		return updateRequired;
	}

	/**
	 * Copies a building from source into the destination list
	 * @param source The building to copy from (i.e. the true building details)
	 * @param destination The building list to copy into (i.e. the player's memory of buildings)
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Whether any update actually happened (i.e. false if the building was already in the list)
	 */
	final static boolean copyBuilding (final MemoryBuilding source, final List<MemoryBuilding> destination, final Logger debugLogger)
	{
		// Since buildings can't change, only be added or destroyed, we don't need to worry about whether the
		// building in the list but somehow changed, that's can't happen
		final boolean needToAdd = !MemoryBuildingUtils.findBuilding (destination, source.getCityLocation (), source.getBuildingID (), debugLogger);

		if (needToAdd)
		{
			final OverlandMapCoordinates destinationCoords = new OverlandMapCoordinates ();
			destinationCoords.setX (source.getCityLocation ().getX ());
			destinationCoords.setY (source.getCityLocation ().getY ());
			destinationCoords.setPlane (source.getCityLocation ().getPlane ());

			final MemoryBuilding destinationBuilding = new MemoryBuilding ();
			destinationBuilding.setBuildingID (source.getBuildingID ());
			destinationBuilding.setCityLocation (destinationCoords);

			destination.add (destinationBuilding);
		}

		return needToAdd;
	}

	/**
	 * Copies a unit from source into the destination list
	 * @param source The unit to copy from (i.e. the true unit details)
	 * @param destination The building list to copy into (i.e. the player's memory of buildings)
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Whether any update actually happened (i.e. false if the unit was already in the list AND all the details already exactly matched)
	 */
	final static boolean copyUnit (final MemoryUnit source, final List<MemoryUnit> destination, final Logger debugLogger)
	{
		// First see if the unit is in the destination list at all
		boolean needToUpdate;
		MemoryUnit dest = UnitUtils.findUnitURN (source.getUnitURN (), destination, debugLogger);
		if (dest == null)
		{
			dest = new MemoryUnit ();
			destination.add (dest);
			needToUpdate = true;
		}
		else
		{
			// Compare every field to see if anything has changed
			// AvailableUnit fields + number of skills
			needToUpdate = (source.getOwningPlayerID () != dest.getOwningPlayerID ()) ||
				(!CompareUtils.safeStringCompare (source.getUnitID (), dest.getUnitID ())) ||
				(!CoordinatesUtils.overlandMapCoordinatesEqual (source.getUnitLocation (), dest.getUnitLocation (), true)) ||
				(!CompareUtils.safeIntegerCompare (source.getWeaponGrade (), dest.getWeaponGrade ())) ||
				(source.getUnitHasSkill ().size () != dest.getUnitHasSkill ().size ()) ||

				// MemoryUnit fields
				(!CompareUtils.safeStringCompare (source.getHeroNameID (), dest.getHeroNameID ())) ||
				(!CompareUtils.safeStringCompare (source.getUnitName (), dest.getUnitName ())) ||
				(source.getRangedAttackAmmo () != dest.getRangedAttackAmmo ()) ||
				(source.getManaRemaining () != dest.getManaRemaining ()) ||
				(source.getDamageTaken () != dest.getDamageTaken ()) ||
				(source.getStatus () != dest.getStatus ()) ||
				(source.isWasSummonedInCombat () != dest.isWasSummonedInCombat ()) ||
				(!CoordinatesUtils.overlandMapCoordinatesEqual (source.getCombatLocation (), dest.getCombatLocation (), true)) ||
				(!CoordinatesUtils.combatMapCoordinatesEqual (source.getCombatPosition (), dest.getCombatPosition (), true)) ||
				(!CompareUtils.safeIntegerCompare (source.getCombatHeading (), dest.getCombatHeading ())) ||
				(source.getCombatSide () != dest.getCombatSide ()) ||

				// Should these work same as city currentlyConstructing? where only the owner can 'see' the values?
				// Similarly only the two sides involved in a combat should be able to 'see' the combat related values
				(source.getDoubleOverlandMovesLeft () != dest.getDoubleOverlandMovesLeft ()) ||
				(source.getSpecialOrder () != dest.getSpecialOrder ()) ||
				(source.getDoubleCombatMovesLeft () != dest.getDoubleCombatMovesLeft ());

			// AvailableUnit - compare skills in detail - already know the number of skills matches, so just need to verify their existance and values
			final Iterator<UnitHasSkill> sourceSkillsIter = source.getUnitHasSkill ().iterator ();
			while ((!needToUpdate) && (sourceSkillsIter.hasNext ()))
			{
				final UnitHasSkill srcSkill = sourceSkillsIter.next ();
				final int expectedValue = (srcSkill.getUnitSkillValue () == null) ? 0 : srcSkill.getUnitSkillValue ();
				if (UnitUtils.getBasicSkillValue (dest.getUnitHasSkill (), srcSkill.getUnitSkillID ()) != expectedValue)
					needToUpdate = true;
			}
		}

		if (needToUpdate)
		{
			// Copy every field from source to dest
			// AvailableUnit fields
			dest.setOwningPlayerID (source.getOwningPlayerID ());
			dest.setUnitID (source.getUnitID ());
			dest.setWeaponGrade (source.getWeaponGrade ());

			if (source.getUnitLocation () == null)
				dest.setUnitLocation (null);
			else
			{
				final OverlandMapCoordinates unitLocation = new OverlandMapCoordinates ();
				unitLocation.setX (source.getUnitLocation ().getX ());
				unitLocation.setY (source.getUnitLocation ().getY ());
				unitLocation.setPlane (source.getUnitLocation ().getPlane ());
				dest.setUnitLocation (unitLocation);
			}

			dest.getUnitHasSkill ().clear ();
			for (final UnitHasSkill srcSkill : source.getUnitHasSkill ())
			{
				final UnitHasSkill destSkill = new UnitHasSkill ();
				destSkill.setUnitSkillID (srcSkill.getUnitSkillID ());
				destSkill.setUnitSkillValue (srcSkill.getUnitSkillValue ());
				dest.getUnitHasSkill ().add (destSkill);
			}

			// MemoryUnit fields
			dest.setUnitURN (source.getUnitURN ());
			dest.setHeroNameID (source.getHeroNameID ());
			dest.setUnitName (source.getUnitName ());
			dest.setRangedAttackAmmo (source.getRangedAttackAmmo ());
			dest.setManaRemaining (source.getManaRemaining ());
			dest.setDamageTaken (source.getDamageTaken ());
			dest.setDoubleOverlandMovesLeft (source.getDoubleOverlandMovesLeft ());
			dest.setSpecialOrder (source.getSpecialOrder ());
			dest.setStatus (source.getStatus ());
			dest.setWasSummonedInCombat (source.isWasSummonedInCombat ());
			dest.setCombatHeading (source.getCombatHeading ());
			dest.setCombatSide (source.getCombatSide ());
			dest.setDoubleCombatMovesLeft (source.getDoubleCombatMovesLeft ());

			if (source.getCombatLocation () == null)
				dest.setCombatLocation (null);
			else
			{
				final OverlandMapCoordinates combatLocation = new OverlandMapCoordinates ();
				combatLocation.setX (source.getCombatLocation ().getX ());
				combatLocation.setY (source.getCombatLocation ().getY ());
				combatLocation.setPlane (source.getCombatLocation ().getPlane ());
				dest.setCombatLocation (combatLocation);
			}

			if (source.getCombatPosition () == null)
				dest.setCombatPosition (null);
			else
			{
				final CombatMapCoordinates combatPosition = new CombatMapCoordinates ();
				combatPosition.setX (source.getCombatPosition ().getX ());
				combatPosition.setY (source.getCombatPosition ().getY ());
				dest.setCombatPosition (combatPosition);
			}
		}

		return needToUpdate;
	}

	/**
	 * Copies a spell from source into the destination list
	 * @param source The spell to copy from (i.e. the true spell details)
	 * @param destination The spell list to copy into (i.e. the player's memory of spells)
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Whether any update actually happened (i.e. false if the spell was already in the list)
	 */
	final static boolean copyMaintainedSpell (final MemoryMaintainedSpell source, final List<MemoryMaintainedSpell> destination, final Logger debugLogger)
	{
		// Since spells can't change, only be cast or cancelled, we don't need to worry about whether the
		// spell in the list but somehow changed, that's can't happen
		final boolean needToAdd = (MemoryMaintainedSpellUtils.findMaintainedSpell (destination, source.getCastingPlayerID (), source.getSpellID (),
			source.getUnitURN (), source.getUnitSkillID (), source.getCityLocation (), source.getCitySpellEffectID (), debugLogger) == null);

		if (needToAdd)
		{
			final MemoryMaintainedSpell destinationSpell = new MemoryMaintainedSpell ();

			destinationSpell.setCastingPlayerID (source.getCastingPlayerID ());
			destinationSpell.setSpellID (source.getSpellID ());
			destinationSpell.setUnitURN (source.getUnitURN ());
			destinationSpell.setUnitSkillID (source.getUnitSkillID ());
			destinationSpell.setCastInCombat (source.isCastInCombat ());
			destinationSpell.setCitySpellEffectID (source.getCitySpellEffectID ());

			if (source.getCityLocation () == null)
				destinationSpell.setCityLocation (null);
			else
			{
				final OverlandMapCoordinates cityLocation = new OverlandMapCoordinates ();
				cityLocation.setX (source.getCityLocation ().getX ());
				cityLocation.setY (source.getCityLocation ().getY ());
				cityLocation.setPlane (source.getCityLocation ().getPlane ());
				destinationSpell.setCityLocation (cityLocation);
			}

			destination.add (destinationSpell);
		}

		return needToAdd;
	}

	/**
	 * Copies a CAE from source into the destination list
	 * @param source The CAE to copy from (i.e. the true CAE details)
	 * @param destination The CAE list to copy into (i.e. the player's memory of CAEs)
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Whether any update actually happened (i.e. false if the building was already in the list)
	 */
	final static boolean copyCombatAreaEffect (final MemoryCombatAreaEffect source, final List<MemoryCombatAreaEffect> destination, final Logger debugLogger)
	{
		// Since CAEs can't change, only be added or destroyed, we don't need to worry about whether the
		// CAE in the list but somehow changed, that's can't happen
		final boolean needToAdd = !MemoryCombatAreaEffectUtils.findCombatAreaEffect (destination, source.getMapLocation (), source.getCombatAreaEffectID (), source.getCastingPlayerID (), debugLogger);

		if (needToAdd)
		{
			final MemoryCombatAreaEffect destinationCAE = new MemoryCombatAreaEffect ();

			destinationCAE.setCombatAreaEffectID (source.getCombatAreaEffectID ());
			destinationCAE.setCastingPlayerID (source.getCastingPlayerID ());

			if (source.getMapLocation () == null)
				destinationCAE.setMapLocation (null);
			else
			{
				final OverlandMapCoordinates mapLocation = new OverlandMapCoordinates ();
				mapLocation.setX (source.getMapLocation ().getX ());
				mapLocation.setY (source.getMapLocation ().getY ());
				mapLocation.setPlane (source.getMapLocation ().getPlane ());
				destinationCAE.setMapLocation (mapLocation);
			}

			destination.add (destinationCAE);
		}

		return needToAdd;
	}

	/**
	 * There's (for now at least, until I get this all sorted out in 0.9.5) there's a number of different places that
	 * result in unit creation messages being sent to the client, so at least this means there's only one
	 * place that builds those messages based on a source true unit
	 *
	 * @param source True unit details held on server
	 * @param db Lookup lists built over the XML database
	 * @return Unit creation message to send to client
	 * @throws RecordNotFoundException If the unitID doesn't exist
	 */
	final static AddUnitMessageData createAddUnitMessage (final MemoryUnit source, final ServerDatabaseLookup db)
		throws RecordNotFoundException
	{
		final AddUnitMessageData destination = new AddUnitMessageData ();

		// Fields from AddUnitMessageData
		destination.setUnitURN (source.getUnitURN ());
		destination.setHeroNameID (source.getHeroNameID ());

		// Fields from AvailableUnit
		destination.setOwningPlayerID (source.getOwningPlayerID ());
		destination.setUnitID (source.getUnitID ());
		destination.setUnitLocation (source.getUnitLocation ());		// Can get away without making deep copy because assuming msg will be sent and then discarded
		destination.setWeaponGrade (source.getWeaponGrade ());

		// Skills
		if (db.findUnit (source.getUnitID (), "createAddUnitMessage").getUnitMagicRealm ().equals (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
		{
			// Include skills in message; experience will be included in the skills list
			destination.setReadSkillsFromXML (false);
			destination.getUnitHasSkill ().addAll (source.getUnitHasSkill ());
		}
		else
		{
			// Tell client to read skills from XML file; include experience
			final int experience = UnitUtils.getBasicSkillValue (source.getUnitHasSkill (), CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE);

			destination.setReadSkillsFromXML (true);
			if (experience >= 0)
				destination.setExperience (experience);
		}

		return destination;
	}

	/**
	 * @param source True spell details held on server
	 * @return Spell creation message to send to client
	 */
	final static AddMaintainedSpellMessageData createAddSpellMessage (final MemoryMaintainedSpell source)
	{
		final AddMaintainedSpellMessageData destination = new AddMaintainedSpellMessageData ();

		destination.setCastingPlayerID (source.getCastingPlayerID ());
		destination.setSpellID (source.getSpellID ());
		destination.setUnitURN (source.getUnitURN ());
		destination.setUnitSkillID (source.getUnitSkillID ());
		destination.setCastInCombat (source.isCastInCombat ());
		destination.setCityLocation (source.getCityLocation ());
		destination.setCitySpellEffectID (source.getCitySpellEffectID ());

		return destination;
	}

	/**
	 * Prevent instantiation
	 */
	private FogOfWarDuplication ()
	{
	}
}
