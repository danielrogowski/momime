package momime.server.process;

import java.util.Iterator;
import java.util.List;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.database.CombatTileBorder;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.utils.ExpandedUnitDetails;
import momime.server.utils.CombatMapServerUtils;

/**
 * More methods dealing with executing combats
 */
public final class CombatHandlingImpl implements CombatHandling
{
	/** Methods dealing with combat maps that are only needed on the server */
	private CombatMapServerUtils combatMapServerUtils;
	
	/**
	 * Checks to see if anything special needs to happen when a unit crosses over the border between two combat tiles
	 * 
	 * @param xu Unit that is moving across a border
	 * @param combatLocation Location where the combat is taking place
	 * @param combatMap Combat scenery
	 * @param moveFrom Cell being moved from
	 * @param moveTo Cell moving into
	 * @param trueSpells True spell details held on server
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If we can't find one of the border definitions
	 */
	@Override
	public final void crossCombatBorder (final ExpandedUnitDetails xu, final MapCoordinates3DEx combatLocation, final MapAreaOfCombatTiles combatMap,
		final MapCoordinates2DEx moveFrom, final MapCoordinates2DEx moveTo,
		final List<MemoryMaintainedSpell> trueSpells, final CommonDatabase db)
		throws RecordNotFoundException
	{
		// The reason this doesn't look specifically for the border between the from and to tile is mainly for the case where
		// there's no city walls and we move diagonally into the wall of fire from the corners - which case there's no border
		// there but the unit should still get burned - so instead just check if we're outside the walls moving to the inside
		if ((!getCombatMapServerUtils ().isWithinWallOfFire (combatLocation, moveFrom, combatMap, trueSpells, db)) &&
			(getCombatMapServerUtils ().isWithinWallOfFire (combatLocation, moveTo, combatMap, trueSpells, db)))
		{
			// Crossing wall of fire - but still might be ignored if we are flying
			final CombatTileBorder borderDef = db.findCombatTileBorder (db.getWallOfFireBorderID (), "crossCombatBorder");
			
			boolean damage = true;
			final Iterator<String> iter = borderDef.getBorderDamageNegatedBySkill ().iterator ();
			while ((damage) && (iter.hasNext ()))
			{
				final String unitSkillID = iter.next ();
				if (xu.hasModifiedSkill (unitSkillID))
					damage = false;
			}
			
			if (damage)
			{
				// Take damage from wall of fire
				System.out.println ("Unit struck by wall of fire");
			}
		}
	}

	/**
	 * @return Methods dealing with combat maps that are only needed on the server
	 */
	public final CombatMapServerUtils getCombatMapServerUtils ()
	{
		return combatMapServerUtils;
	}

	/**
	 * @param u Methods dealing with combat maps that are only needed on the server
	 */
	public final void setCombatMapServerUtils (final CombatMapServerUtils u)
	{
		combatMapServerUtils = u;
	}
}