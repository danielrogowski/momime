package momime.server.process;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MemoryUnit;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.TargetSpellResult;
import momime.server.MomSessionVariables;
import momime.server.utils.CombatMapServerUtils;

/**
 * More methods dealing with executing combats
 */
public final class CombatHandlingImpl implements CombatHandling
{
	/** Methods dealing with combat maps that are only needed on the server */
	private CombatMapServerUtils combatMapServerUtils;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Damage processor */
	private DamageProcessor damageProcessor;
	
	/**
	 * Checks to see if anything special needs to happen when a unit crosses over the border between two combat tiles
	 * 
	 * @param xu Unit that is moving across a border
	 * @param combatLocation Location where the combat is taking place
	 * @param combatMap Combat scenery
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param moveFrom Cell being moved from
	 * @param moveTo Cell moving into
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether the final unit on one side of combat burned itself to death hence letting the other side win
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final boolean crossCombatBorder (final ExpandedUnitDetails xu, final MapCoordinates3DEx combatLocation, final MapAreaOfCombatTiles combatMap,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final MapCoordinates2DEx moveFrom, final MapCoordinates2DEx moveTo, final MomSessionVariables mom)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		boolean combatEnded = false;
		
		// The reason this doesn't look specifically for the border between the from and to tile is mainly for the case where
		// there's no city walls and we move diagonally into the wall of fire from the corners - which case there's no border
		// there but the unit should still get burned - so instead just check if we're outside the walls moving to the inside
		if ((!getCombatMapServerUtils ().isWithinWallOfFire (combatLocation, moveFrom, combatMap,
				mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), mom.getServerDB ())) &&
			(getCombatMapServerUtils ().isWithinWallOfFire (combatLocation, moveTo, combatMap,
				mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), mom.getServerDB ())))
		{
			// Find the spell
			final Spell spellDef = mom.getServerDB ().findSpell (CommonDatabaseConstants.SPELL_ID_WALL_OF_FIRE, "crossCombatBorder");
						
			// Specify 0 for castingPlayerID, since we can hurt ourselves
			if (getMemoryMaintainedSpellUtils ().isUnitValidTargetForSpell (spellDef, SpellBookSectionID.ATTACK_SPELLS, combatLocation, 0, null, null, xu,
				false, mom.getGeneralServerKnowledge ().getTrueMap (), null, mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET)
			{
				final List<MemoryUnit> targetUnits = new ArrayList<MemoryUnit> ();
				targetUnits.add (xu.getMemoryUnit ());
				
				// castingPlayer (the owner of the wall of fire) has to be the defendingPlayer
				combatEnded = getDamageProcessor ().resolveAttack (null, targetUnits, attackingPlayer, defendingPlayer, null, null, null, null, spellDef, null,
					defendingPlayer, combatLocation, mom);
			}
		}
		
		return combatEnded;
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
	 * @return Damage processor
	 */
	public final DamageProcessor getDamageProcessor ()
	{
		return damageProcessor;
	}

	/**
	 * @param proc Damage processor
	 */
	public final void setDamageProcessor (final DamageProcessor proc)
	{
		damageProcessor = proc;
	}
}