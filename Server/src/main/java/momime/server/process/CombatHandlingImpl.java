package momime.server.process;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MemoryUnit;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.TargetSpellResult;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.messages.ServerGridCell;
import momime.server.utils.CombatMapServerUtils;

/**
 * More methods dealing with executing combats
 */
public final class CombatHandlingImpl implements CombatHandling
{
	/** Number of point damage done by vortex doom bolts + lightning bolts */
	private final static int VORTEX_VARIABLE_DAMAGE = 5;
	
	/** Methods dealing with combat maps that are only needed on the server */
	private CombatMapServerUtils combatMapServerUtils;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Damage processor */
	private DamageProcessor damageProcessor;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;

	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;
	
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
			if (getMemoryMaintainedSpellUtils ().isUnitValidTargetForSpell (spellDef, SpellBookSectionID.ATTACK_SPELLS, combatLocation, combatMap, 0, null, null, xu,
				false, mom.getGeneralServerKnowledge ().getTrueMap (), null, mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET)
			{
				final List<ResolveAttackTarget> targetUnits = new ArrayList<ResolveAttackTarget> ();
				targetUnits.add (new ResolveAttackTarget (xu.getMemoryUnit ()));
				
				// castingPlayer (the owner of the wall of fire) has to be the defendingPlayer
				combatEnded = getDamageProcessor ().resolveAttack (null, targetUnits, attackingPlayer, defendingPlayer, null, null, null, null, spellDef, null,
					defendingPlayer, combatLocation, true, mom).isCombatEnded ();
			}
		}
		
		return combatEnded;
	}
	
	/**
	 * Checks to see if a Magic Vortex hits any units directly under it or adjacent to it.  It will attack the side who owns it as well.
	 * 
	 * @param vortex The vortex to check damage from
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether the vortex killed the last unit on one or other side of the combat and ended it or not
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final boolean damageFromVortex (final MemoryUnit vortex, final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		final PlayerServerDetails castingPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), vortex.getOwningPlayerID (), "damageFromVortex");
		
		final ServerGridCell gc = (ServerGridCell) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(vortex.getCombatLocation ().getZ ()).getRow ().get (vortex.getCombatLocation ().getY ()).getCell ().get (vortex.getCombatLocation ().getX ());
		
		// Build a list of all the units being attacked
		final List<ResolveAttackTarget> defenders = new ArrayList<ResolveAttackTarget> ();
		
		// Is there a unit in the same space as the vortex?
		final MemoryUnit doomUnit = getUnitUtils ().findAliveUnitInCombatAt (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (),
			(MapCoordinates3DEx) vortex.getCombatLocation (), (MapCoordinates2DEx) vortex.getCombatPosition (), mom.getServerDB (), false);
		if (doomUnit != null)
		{
			// Use the Doom Bolt spell definition, which has all the magic realm, damage type etc set correctly, and just override the damage
			final Spell doomBoltSpell = mom.getServerDB ().findSpell (CommonDatabaseConstants.SPELL_ID_DOOM_BOLT, "damageFromVortex");
			final ExpandedUnitDetails xuDoomUnit = getExpandUnitDetails ().expandUnitDetails (doomUnit, null, null, doomBoltSpell.getSpellRealm (),
				mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());

			if (getMemoryMaintainedSpellUtils ().isUnitValidTargetForSpell (doomBoltSpell, null, (MapCoordinates3DEx) vortex.getCombatLocation (), gc.getCombatMap (),
				0, null, VORTEX_VARIABLE_DAMAGE, xuDoomUnit, false, mom.getGeneralServerKnowledge ().getTrueMap (), null,
				mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET)
			{
				final ResolveAttackTarget doomUnitTarget = new ResolveAttackTarget (doomUnit);
				doomUnitTarget.setSpellOverride (doomBoltSpell);
				defenders.add (doomUnitTarget);
			}
		}
		
		// Are there any units in the 8 tiles adjacent to the vortex?
		Spell lightningBoltSpell = null;
		for (int d = 1; d <= getCoordinateSystemUtils ().getMaxDirection (mom.getSessionDescription ().getCombatMapSize ().getCoordinateSystemType ()); d++)
		{
			final MapCoordinates2DEx coords = new MapCoordinates2DEx ((MapCoordinates2DEx) vortex.getCombatPosition ());
			if (getCoordinateSystemUtils ().move2DCoordinates (mom.getSessionDescription ().getCombatMapSize (), coords, d))
			{
				final MemoryUnit lightningUnit = getUnitUtils ().findAliveUnitInCombatAt (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (),
					(MapCoordinates3DEx) vortex.getCombatLocation (), coords, mom.getServerDB (), false);
				if ((lightningUnit != null) && (getRandomUtils ().nextInt (3) == 0))
				{
					// Use the Lightning Bolt spell definition, which has all the magic realm, damage type etc set correctly, and just override the damage
					if (lightningBoltSpell == null)
						lightningBoltSpell = mom.getServerDB ().findSpell (CommonDatabaseConstants.SPELL_ID_LIGHTNING_BOLT, "damageFromVortex");
					
					final ExpandedUnitDetails xuLightningUnit = getExpandUnitDetails ().expandUnitDetails (lightningUnit, null, null, lightningBoltSpell.getSpellRealm (),
						mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());

					if (getMemoryMaintainedSpellUtils ().isUnitValidTargetForSpell (lightningBoltSpell, null, (MapCoordinates3DEx) vortex.getCombatLocation (), gc.getCombatMap (),
						0, null, VORTEX_VARIABLE_DAMAGE, xuLightningUnit, false, mom.getGeneralServerKnowledge ().getTrueMap (), null,
						mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET)
					{
						final ResolveAttackTarget lightningUnitTarget = new ResolveAttackTarget (lightningUnit);
						lightningUnitTarget.setSpellOverride (lightningBoltSpell);
						defenders.add (lightningUnitTarget);
					}
				}
			}
		}
		
		// Any attack to do?
		final boolean combatEnded;
		if (defenders.size () == 0)
			combatEnded = false;
		else
		{
			// Find the spell that summoned the magic vortex, so the damage log says magic vortex attacking, rather than lightning bolt or doom bolt
			final Spell magicVortexSpell = mom.getServerDB ().getSpell ().stream ().filter
				(s -> s.getSummonedUnit ().contains (vortex.getUnitID ())).findAny ().orElse (null);
			
			if (magicVortexSpell == null)
				throw new MomException ("damageFromVortex can't find the spell that summoned the vortex");
			
			combatEnded = getDamageProcessor ().resolveAttack (vortex, defenders, attackingPlayer, defendingPlayer, null, null, null, null,
				magicVortexSpell, VORTEX_VARIABLE_DAMAGE, castingPlayer, (MapCoordinates3DEx) vortex.getCombatLocation (), true, mom).isCombatEnded ();
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
	 * @return Random number generator
	 */
	public final RandomUtils getRandomUtils ()
	{
		return randomUtils;
	}

	/**
	 * @param utils Random number generator
	 */
	public final void setRandomUtils (final RandomUtils utils)
	{
		randomUtils = utils;
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

	/**
	 * @return Server only helper methods for dealing with players in a session
	 */
	public final MultiplayerSessionServerUtils getMultiplayerSessionServerUtils ()
	{
		return multiplayerSessionServerUtils;
	}

	/**
	 * @param obj Server only helper methods for dealing with players in a session
	 */
	public final void setMultiplayerSessionServerUtils (final MultiplayerSessionServerUtils obj)
	{
		multiplayerSessionServerUtils = obj;
	}
}