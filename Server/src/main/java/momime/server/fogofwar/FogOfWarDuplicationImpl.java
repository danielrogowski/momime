package momime.server.fogofwar;

import java.util.Iterator;
import java.util.List;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.database.UnitSkillAndValue;
import momime.common.database.UnitSpecialOrder;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MemoryUnitHeroItemSlot;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitDamage;
import momime.common.utils.CompareUtils;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.UnitUtils;
import momime.server.utils.UnitServerUtils;

/**
 * Methods for comparing and copying data from one source against a destination container
 * This is used for copying data from the server's true memory into player's memory
 *
 * Note these must always make deep copies - if an object includes another object (e.g. OverlandMapCoordinates or UnitHasSkill) then
 * we need to make a copy of the child object and reference that as well, we can't just copy the reference from source
 */
public final class FogOfWarDuplicationImpl implements FogOfWarDuplication
{
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;
	
	/** MemoryBuilding utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Memory CAE utils */
	private MemoryCombatAreaEffectUtils memoryCombatAreaEffectUtils;
	
	/**
	 * Copies all the terrain and node aura related data items from source to destination
	 * @param source The map cell to copy from
	 * @param destination The map cell to copy to
	 * @return Whether any update actually happened (i.e. false if source and destination already had the same info)
	 */
	@Override
	public final boolean copyTerrainAndNodeAura (final MemoryGridCell source, final MemoryGridCell destination)
	{
		final OverlandMapTerrainData sourceData = source.getTerrainData ();
		OverlandMapTerrainData destinationData = destination.getTerrainData ();

		final boolean updateRequired = (destinationData == null) ||
			(sourceData.isCorrupted () != destinationData.isCorrupted ()) ||
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

			destinationData.setCorrupted (sourceData.isCorrupted ());
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
	@Override
	public final boolean blankTerrainAndNodeAura (final MemoryGridCell destination)
	{
		final OverlandMapTerrainData destinationData = destination.getTerrainData ();

		final boolean updateRequired = (destinationData != null) &&
			((destinationData.isCorrupted ()) || (destinationData.getTileTypeID () != null) || (destinationData.getMapFeatureID () != null) ||
			 (destinationData.getRiverDirections () != null) || (destinationData.getNodeOwnerID () != null));

		destination.setTerrainData (null);

		return updateRequired;
	}

	/**
	 * Copies all the city related data items from source to destination
	 * @param source The map cell to copy from
	 * @param destination The map cell to copy to
	 * @param includeCurrentlyConstructing Can see current construction project if its our city OR session description FOW option is set
	 * @param includeProductionSoFar Can see progress made on current construction project if its our city
	 * @return Whether any update actually happened (i.e. false if source and destination already had the same info)
	 */
	@Override
	public final boolean copyCityData (final MemoryGridCell source, final MemoryGridCell destination, final boolean includeCurrentlyConstructing, final boolean includeProductionSoFar)
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

			// Destination values for current construction depend on the input param
			final String newCurrentlyConstructingBuilding;
			final String newCurrentlyConstructingUnit;
			if (includeCurrentlyConstructing)
			{
				newCurrentlyConstructingBuilding = source.getCityData ().getCurrentlyConstructingBuildingID ();
				newCurrentlyConstructingUnit = source.getCityData ().getCurrentlyConstructingUnitID ();
			}
			else
			{
				newCurrentlyConstructingBuilding = null;
				newCurrentlyConstructingUnit = null;
			}

			// Destination value for construction progress depends on the input param
			final Integer newProductionSoFar = includeProductionSoFar ? source.getCityData ().getProductionSoFar () : null;
			

			// Now can figure out if any updates are necessary
			updateRequired = (destinationData == null) ||
				(sourceData.getCityPopulation () != destinationData.getCityPopulation ()) ||
				(sourceData.getMinimumFarmers () != destinationData.getMinimumFarmers ()) ||
				(sourceData.getOptionalFarmers () != destinationData.getOptionalFarmers ()) ||
				(sourceData.getNumberOfRebels () != destinationData.getNumberOfRebels ()) ||
				(sourceData.getCityOwnerID () != destinationData.getCityOwnerID ()) ||
				(!CompareUtils.safeStringCompare (sourceData.getCityRaceID (), destinationData.getCityRaceID ())) ||
				(!CompareUtils.safeStringCompare (sourceData.getCitySizeID (), destinationData.getCitySizeID ())) ||
				(!CompareUtils.safeStringCompare (sourceData.getCityName (), destinationData.getCityName ())) ||
				(!CompareUtils.safeStringCompare (newCurrentlyConstructingBuilding, destinationData.getCurrentlyConstructingBuildingID ())) ||
				(!CompareUtils.safeStringCompare (newCurrentlyConstructingUnit, destinationData.getCurrentlyConstructingUnitID ())) ||
				(!CompareUtils.safeIntegerCompare (newProductionSoFar, destinationData.getProductionSoFar ()));

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
				destinationData.setCurrentlyConstructingBuildingID (newCurrentlyConstructingBuilding);
				destinationData.setCurrentlyConstructingUnitID (newCurrentlyConstructingUnit);
				destinationData.setProductionSoFar (newProductionSoFar);
			}
		}

		return updateRequired;
	}

	/**
	 * Wipes all memory of the city at this location
	 * @param destination Map cell from player's memorized map
	 * @return True if an actual update was made; false if the player already knew nothing
	 */
	@Override
	public final boolean blankCityData (final MemoryGridCell destination)
	{
		final OverlandMapCityData destinationData = destination.getCityData ();

		final boolean updateRequired = (destinationData != null) &&
			((destinationData.getCityPopulation () > 0) || (destinationData.getMinimumFarmers () > 0) || (destinationData.getOptionalFarmers () > 0) ||
			 (destinationData.getNumberOfRebels () > 0) || (destinationData.getCityOwnerID () != 0) || (destinationData.getCityRaceID () != null) ||
			 (destinationData.getCitySizeID () != null) || (destinationData.getCityName () != null) ||
			 (destinationData.getCurrentlyConstructingBuildingID () != null) || (destinationData.getCurrentlyConstructingUnitID () != null) ||
			 (destinationData.getProductionSoFar () != null));

		destination.setCityData (null);

		return updateRequired;
	}

	/**
	 * Copies a building from source into the destination list
	 * @param source The building to copy from (i.e. the true building details)
	 * @param destination The building list to copy into (i.e. the player's memory of buildings)
	 * @return Whether any update actually happened (i.e. false if the building was already in the list)
	 */
	@Override
	public final boolean copyBuilding (final MemoryBuilding source, final List<MemoryBuilding> destination)
	{
		// Since buildings can't change, only be added or destroyed, we don't need to worry about whether the
		// building in the list but somehow changed, that's can't happen
		final boolean needToAdd = (getMemoryBuildingUtils ().findBuildingURN (source.getBuildingURN (), destination) == null);

		if (needToAdd)
		{
			final MemoryBuilding destinationBuilding = new MemoryBuilding ();
			destinationBuilding.setBuildingURN (source.getBuildingURN ());
			destinationBuilding.setBuildingID (source.getBuildingID ());
			destinationBuilding.setCityLocation (new MapCoordinates3DEx ((MapCoordinates3DEx) source.getCityLocation ()));

			destination.add (destinationBuilding);
		}

		return needToAdd;
	}

	/**
	 * Copies a unit from source into the destination list
	 * @param source The unit to copy from (i.e. the true unit details)
	 * @param destination The building list to copy into (i.e. the player's memory of buildings)
	 * @param includeMovementFields Only the player who owns a unit can see its movement remaining and special orders
	 * @return Whether any update actually happened (i.e. false if the unit was already in the list AND all the details already exactly matched)
	 */
	@Override
	public final boolean copyUnit (final MemoryUnit source, final List<MemoryUnit> destination, final boolean includeMovementFields)
	{
		// First see if the unit is in the destination list at all
		boolean needToUpdate;
		MemoryUnit dest = getUnitUtils ().findUnitURN (source.getUnitURN (), destination);
		if (dest == null)
		{
			dest = new MemoryUnit ();
			destination.add (dest);
			needToUpdate = true;
		}
		else
		{
			// Destination values for a couple of movement related fields depend on input param
			final int newDoubleOverlandMovesLeft = includeMovementFields ? source.getDoubleOverlandMovesLeft () : 0;
			final Integer newDoubleCombatMovesLeft = includeMovementFields ? source.getDoubleCombatMovesLeft () : null;
			final UnitSpecialOrder newSpecialOrder = includeMovementFields ? source.getSpecialOrder () : null;

			// AvailableUnit fields + number of skills
			needToUpdate = (source.getOwningPlayerID () != dest.getOwningPlayerID ()) ||
				(!CompareUtils.safeStringCompare (source.getUnitID (), dest.getUnitID ())) ||
				(!CompareUtils.safeOverlandMapCoordinatesCompare ((MapCoordinates3DEx) source.getUnitLocation (), (MapCoordinates3DEx) dest.getUnitLocation ())) ||
				(!CompareUtils.safeIntegerCompare (source.getWeaponGrade (), dest.getWeaponGrade ())) ||
				(source.getUnitHasSkill ().size () != dest.getUnitHasSkill ().size ()) ||

				// MemoryUnit fields
				(!CompareUtils.safeStringCompare (source.getHeroNameID (), dest.getHeroNameID ())) ||
				(!CompareUtils.safeStringCompare (source.getUnitName (), dest.getUnitName ())) ||
				(source.getAmmoRemaining () != dest.getAmmoRemaining ()) ||
				(source.getManaRemaining () != dest.getManaRemaining ()) ||
				(source.getFixedSpellsRemaining ().size () != dest.getFixedSpellsRemaining ().size ()) ||
				(source.getHeroItemSpellChargesRemaining ().size () != dest.getHeroItemSpellChargesRemaining ().size ()) ||
				(source.getStatus () != dest.getStatus ()) ||
				(source.isWasSummonedInCombat () != dest.isWasSummonedInCombat ()) ||
				(!CompareUtils.safeOverlandMapCoordinatesCompare ((MapCoordinates3DEx) source.getCombatLocation (), (MapCoordinates3DEx) dest.getCombatLocation ())) ||
				(!CompareUtils.safeCombatMapCoordinatesCompare ((MapCoordinates2DEx) source.getCombatPosition (), (MapCoordinates2DEx) dest.getCombatPosition ())) ||
				(!CompareUtils.safeIntegerCompare (source.getCombatHeading (), dest.getCombatHeading ())) ||
				(source.getCombatSide () != dest.getCombatSide ()) ||
				(source.getHeroItemSlot ().size () != dest.getHeroItemSlot ().size ()) ||
				(source.getUnitDamage ().size () != dest.getUnitDamage ().size ()) ||

				// These work same as city currentlyConstructing? where only the owner can 'see' the values.
				// Similarly only the two sides involved in a combat should be able to 'see' the combat related values, but don't worry about that for now.
				(newDoubleOverlandMovesLeft != dest.getDoubleOverlandMovesLeft ()) ||
				(newSpecialOrder != dest.getSpecialOrder ()) ||
				(!CompareUtils.safeIntegerCompare (newDoubleCombatMovesLeft, dest.getDoubleCombatMovesLeft ()));

			// MemoryUnit - fixed spells remaining - already know the number of spells matches; order here is important.
			final Iterator<Integer> sourceFixedSpells = source.getFixedSpellsRemaining ().iterator ();
			final Iterator<Integer> destFixedSpells = dest.getFixedSpellsRemaining ().iterator ();
			while ((!needToUpdate) && (sourceFixedSpells.hasNext ()) && (destFixedSpells.hasNext ()))
				if (!CompareUtils.safeIntegerCompare (sourceFixedSpells.next (), destFixedSpells.next ()))
					needToUpdate = true;
			
			// MemoryUnit - spell charges remaining - already know the number of skills matches; order here is important.
			final Iterator<Integer> sourceSpellCharges = source.getHeroItemSpellChargesRemaining ().iterator ();
			final Iterator<Integer> destSpellCharges = dest.getHeroItemSpellChargesRemaining ().iterator ();
			while ((!needToUpdate) && (sourceSpellCharges.hasNext ()) && (destSpellCharges.hasNext ()))
				if (!CompareUtils.safeIntegerCompare (sourceSpellCharges.next (), destSpellCharges.next ()))
					needToUpdate = true;
			
			// Memory unit - damage taken - already know the number of damage entries matches; order here isn't important.
			final Iterator<UnitDamage> sourceDamageIter = source.getUnitDamage ().iterator ();
			while ((!needToUpdate) && (sourceDamageIter.hasNext ()))
			{
				final UnitDamage srcDamage = sourceDamageIter.next ();
				if (getUnitServerUtils ().findDamageTakenOfType (dest.getUnitDamage (), srcDamage.getDamageType ()) != srcDamage.getDamageTaken ())
					needToUpdate = true;
			}

			// AvailableUnit - compare skills in detail - already know the number of skills matches, so just need to verify their existance and values.
			// NB. The order here isn't really important, A=1, B=2 is the same as B=2, A=1. 
			final Iterator<UnitSkillAndValue> sourceSkillsIter = source.getUnitHasSkill ().iterator ();
			while ((!needToUpdate) && (sourceSkillsIter.hasNext ()))
			{
				final UnitSkillAndValue srcSkill = sourceSkillsIter.next ();
				final int expectedValue = (srcSkill.getUnitSkillValue () == null) ? 0 : srcSkill.getUnitSkillValue ();
				if (getUnitUtils ().getBasicSkillValue (dest.getUnitHasSkill (), srcSkill.getUnitSkillID ()) != expectedValue)
					needToUpdate = true;
			}
			
			// MemoryUnit - compare hero slots in detail - already know the number of slots matches, so just need to verify their values.
			// NB. The order here is important, even for magic heroes where slots 2 & 3 are both rings ("miscellaneous accessory"),
			// we can move the same item between those slots and that must be considered a "change" to the unit.
			final Iterator<MemoryUnitHeroItemSlot> sourceItemSlots = source.getHeroItemSlot ().iterator ();
			final Iterator<MemoryUnitHeroItemSlot> destItemSlots = dest.getHeroItemSlot ().iterator ();
			while ((!needToUpdate) && (sourceItemSlots.hasNext ()) && (destItemSlots.hasNext ()))
			{
				final MemoryUnitHeroItemSlot srcItemSlot = sourceItemSlots.next ();
				final MemoryUnitHeroItemSlot destItemSlot = destItemSlots.next ();
				
				if (!CompareUtils.safeNumberedHeroItemCompare (srcItemSlot.getHeroItem (), destItemSlot.getHeroItem ()))
					needToUpdate = true;
			}
		}

		if (needToUpdate)
			getUnitUtils ().copyUnitValues (source, dest, includeMovementFields);

		return needToUpdate;
	}

	/**
	 * Copies a spell from source into the destination list
	 * @param source The spell to copy from (i.e. the true spell details)
	 * @param destination The spell list to copy into (i.e. the player's memory of spells)
	 * @return Whether any update actually happened (i.e. false if the spell was already in the list)
	 */
	@Override
	public final boolean copyMaintainedSpell (final MemoryMaintainedSpell source, final List<MemoryMaintainedSpell> destination)
	{
		// Since spells can't change, only be cast or cancelled, we don't need to worry about whether the
		// spell in the list but somehow changed, that's can't happen
		final boolean needToAdd = (getMemoryMaintainedSpellUtils ().findSpellURN (source.getSpellURN (), destination) == null);

		if (needToAdd)
		{
			final MemoryMaintainedSpell destinationSpell = new MemoryMaintainedSpell ();
			destinationSpell.setSpellURN (source.getSpellURN ());
			destinationSpell.setCastingPlayerID (source.getCastingPlayerID ());
			destinationSpell.setSpellID (source.getSpellID ());
			destinationSpell.setUnitURN (source.getUnitURN ());
			destinationSpell.setUnitSkillID (source.getUnitSkillID ());
			destinationSpell.setCastInCombat (source.isCastInCombat ());
			destinationSpell.setCitySpellEffectID (source.getCitySpellEffectID ());

			if (source.getCityLocation () == null)
				destinationSpell.setCityLocation (null);
			else
				destinationSpell.setCityLocation (new MapCoordinates3DEx ((MapCoordinates3DEx) source.getCityLocation ()));

			destination.add (destinationSpell);
		}

		return needToAdd;
	}

	/**
	 * Copies a CAE from source into the destination list
	 * @param source The CAE to copy from (i.e. the true CAE details)
	 * @param destination The CAE list to copy into (i.e. the player's memory of CAEs)
	 * @return Whether any update actually happened (i.e. false if the building was already in the list)
	 */
	@Override
	public final boolean copyCombatAreaEffect (final MemoryCombatAreaEffect source, final List<MemoryCombatAreaEffect> destination)
	{
		// Since CAEs can't change, only be added or destroyed, we don't need to worry about whether the
		// CAE in the list but somehow changed, that's can't happen
		final boolean needToAdd = (getMemoryCombatAreaEffectUtils ().findCombatAreaEffectURN (source.getCombatAreaEffectURN (), destination) == null);

		if (needToAdd)
		{
			final MemoryCombatAreaEffect destinationCAE = new MemoryCombatAreaEffect ();
			destinationCAE.setCombatAreaEffectURN (source.getCombatAreaEffectURN ());
			destinationCAE.setCombatAreaEffectID (source.getCombatAreaEffectID ());
			destinationCAE.setCastingPlayerID (source.getCastingPlayerID ());

			if (source.getMapLocation () == null)
				destinationCAE.setMapLocation (null);
			else
				destinationCAE.setMapLocation (new MapCoordinates3DEx ((MapCoordinates3DEx) source.getMapLocation ()));

			destination.add (destinationCAE);
		}

		return needToAdd;
	}

	/**
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final UnitUtils utils)
	{
		unitUtils = utils;
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
	 * @return MemoryBuilding utils
	 */
	public final MemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param utils MemoryBuilding utils
	 */
	public final void setMemoryBuildingUtils (final MemoryBuildingUtils utils)
	{
		memoryBuildingUtils = utils;
	}

	/**
	 * @return MemoryMaintainedSpell utils
	 */
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param utils MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils utils)
	{
		memoryMaintainedSpellUtils = utils;
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