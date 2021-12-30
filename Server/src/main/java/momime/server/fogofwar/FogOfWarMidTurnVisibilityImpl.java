package momime.server.fogofwar;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.database.FogOfWarValue;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MapVolumeOfFogOfWarStates;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.UnitStatusID;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.FogOfWarCalculations;

/**
 * Contains methods for whether each player can see certain game items during a turn;
 * i.e. methods for when the true values change (or are added or removed) but the visible area that each player can see does not change.
 * 
 * This is used by FogOfWarMidTurnChangesImpl and kept seperate to allow mocking out methods in unit tests.
 */
public final class FogOfWarMidTurnVisibilityImpl implements FogOfWarMidTurnVisibility
{
	/** Single cell FOW calculations */
	private FogOfWarCalculations fogOfWarCalculations;

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/**
	 * @param unit True unit to test
	 * @param player The player we are testing whether they can see the unit
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return True if player can see this unit
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found
	 * @throws PlayerNotFoundException If the player who owns the unit cannot be found
	 */
	@Override
	public final boolean canSeeUnitMidTurn (final MemoryUnit unit, final PlayerServerDetails player, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException
	{
		final boolean canSee;

		// Firstly we only know abouts that are alive
		if (unit.getStatus () != UnitStatusID.ALIVE)
			canSee = false;
		else
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			
			/*
			 * This is basically
			 * canSee = (fogOfWarArea.get (unit.getCurrentLocation ()) == FogOfWarStateID.CAN_SEE)
			 *
			 * Towers of Wizardry add a complication - if the unit is standing in a Tower of Wizardry then they'll be on plane 0, but perhaps we can see the
			 * tower on plane 1... so what this breaks down to is that we'll know about the unit providing we can see it on ANY plane
			 */
			canSee = getFogOfWarCalculations ().canSeeMidTurnOnAnyPlaneIfTower
				((MapCoordinates3DEx) unit.getUnitLocation (), mom.getSessionDescription ().getFogOfWarSetting ().getUnits (),
					mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), priv.getFogOfWar (), mom.getServerDB ());
		}

		return canSee;
	}

	/**
	 * @param spell True spell to test
	 * @param player The player we are testing whether they can see the spell
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return True if player can see this spell
	 * @throws RecordNotFoundException If the unit that the spell is cast on, or tile type or map feature IDs cannot be found
	 * @throws PlayerNotFoundException If the player who owns the unit cannot be found
	 */
	@Override
	public final boolean canSeeSpellMidTurn (final MemoryMaintainedSpell spell, final PlayerServerDetails player, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException
	{
		final boolean canSee;

		// Unit spell?
		if (spell.getUnitURN () != null)
		{
			final MemoryUnit unit = getUnitUtils ().findUnitURN (spell.getUnitURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), "canSeeSpellMidTurn");
			canSee = canSeeUnitMidTurn (unit, player, mom);
		}

		// City spell?
		else if (spell.getCityLocation () != null)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			
			canSee = getFogOfWarCalculations ().canSeeMidTurn (priv.getFogOfWar ().getPlane ().get (spell.getCityLocation ().getZ ()).getRow ().get
				(spell.getCityLocation ().getY ()).getCell ().get (spell.getCityLocation ().getX ()), mom.getSessionDescription ().getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ());
		}

		// Overland enchantment
		else
			canSee = true;

		return canSee;
	}

	/**
	 * @param cae True CAE to test
	 * @param fogOfWarArea Area the player can/can't see, outside of FOW recalc
	 * @param setting FOW CAE setting, from session description
	 * @return True if player can see this CAE
	 */
	@Override
	public final boolean canSeeCombatAreaEffectMidTurn (final MemoryCombatAreaEffect cae,
		final MapVolumeOfFogOfWarStates fogOfWarArea, final FogOfWarValue setting)
	{
		final boolean canSee;

		// This is a lot simpler than the spell version, since CAEs can't be targetted on specific units, only on a map cell or globally

		// Localized CAE?
		if (cae.getMapLocation () != null)
			canSee = getFogOfWarCalculations ().canSeeMidTurn (fogOfWarArea.getPlane ().get (cae.getMapLocation ().getZ ()).getRow ().get
				(cae.getMapLocation ().getY ()).getCell ().get (cae.getMapLocation ().getX ()), setting);

		// Global CAE - so can see it everywhere
		else
			canSee = true;

		return canSee;
	}

	/**
	 * @return Single cell FOW calculations
	 */
	public final FogOfWarCalculations getFogOfWarCalculations ()
	{
		return fogOfWarCalculations;
	}

	/**
	 * @param calc Single cell FOW calculations
	 */
	public final void setFogOfWarCalculations (final FogOfWarCalculations calc)
	{
		fogOfWarCalculations = calc;
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
}