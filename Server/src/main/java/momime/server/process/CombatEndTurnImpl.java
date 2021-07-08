package momime.server.process;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.FogOfWarSetting;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;
import momime.common.messages.UnitStatusID;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.UnitUtils;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.utils.UnitServerUtils;

/**
 * Deals with any processing at the end of one player's turn in combat (after none of their units have any moves left)
 */
public final class CombatEndTurnImpl implements CombatEndTurn
{
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/**
	 * Deals with any processing at the end of one player's turn in combat (after none of their units have any moves left) 
	 * 
	 * @param combatLocation The location the combat is taking place
	 * @param playerID Which player just finished their combat turn
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @param fogOfWarSettings Fog of War settings from session description
	 * @throws RecordNotFoundException If an expected data item cannot be found
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final void combatEndTurn (final MapCoordinates3DEx combatLocation, final int playerID, final List<PlayerServerDetails> players,
		final FogOfWarMemory mem, final CommonDatabase db, final FogOfWarSetting fogOfWarSettings)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		// Note we don't check the unit can normally heal damage (is not undead) because regeneration works even on undead
		final List<MemoryUnit> healedUnits = new ArrayList<MemoryUnit> ();

		for (final MemoryUnit thisUnit : mem.getUnit ())
			if ((thisUnit.getOwningPlayerID () == playerID) && (combatLocation.equals (thisUnit.getCombatLocation ())) && (thisUnit.getCombatPosition () != null) &&
				(thisUnit.getCombatSide () != null) && (thisUnit.getCombatHeading () != null) && (thisUnit.getStatus () == UnitStatusID.ALIVE) &&
				(thisUnit.getUnitDamage ().size () > 0))
			{
				final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (thisUnit, null, null, null, players, mem, db);

				if (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_REGENERATION) ||
					xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_REGENERATION_FROM_SPELL))
				{
					getUnitServerUtils ().healDamage (thisUnit.getUnitDamage (), 1, false);
					healedUnits.add (thisUnit);
				}
			}
		
		if (healedUnits.size () > 0)
			getFogOfWarMidTurnChanges ().sendCombatDamageToClients (null, playerID, healedUnits, null, null, null, null, null, players, mem.getMap (), db, fogOfWarSettings);
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
	 * @return Methods for updating true map + players' memory
	 */
	public final FogOfWarMidTurnChanges getFogOfWarMidTurnChanges ()
	{
		return fogOfWarMidTurnChanges;
	}

	/**
	 * @param obj Methods for updating true map + players' memory
	 */
	public final void setFogOfWarMidTurnChanges (final FogOfWarMidTurnChanges obj)
	{
		fogOfWarMidTurnChanges = obj;
	}
}