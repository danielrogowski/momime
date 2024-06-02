package momime.server.worldupdates;

import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.sessionbase.PlayerType;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.database.CitySpellEffect;
import momime.common.database.CombatAreaAffectsPlayersID;
import momime.common.database.CombatAreaEffect;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.UnitSkillEx;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomCombatTile;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.SwitchOffMaintainedSpellMessage;
import momime.common.messages.servertoclient.UpdateCombatMapMessage;
import momime.common.movement.MovementUtils;
import momime.common.utils.CombatMapUtils;
import momime.common.utils.CombatPlayers;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.fogofwar.FogOfWarMidTurnVisibility;
import momime.server.fogofwar.KillUnitActionID;
import momime.server.knowledge.CombatDetails;
import momime.server.mapgenerator.CombatMapGenerator;
import momime.server.utils.CombatMapServerUtils;

/**
 * World update for switching off a maintained spell
 */
public final class SwitchOffSpellUpdate implements WorldUpdate
{
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Memory CAE utils */
	private MemoryCombatAreaEffectUtils memoryCombatAreaEffectUtils;
	
	/** Combat map utils */
	private CombatMapUtils combatMapUtils;
	
	/** Map generator */
	private CombatMapGenerator combatMapGenerator;
	
	/** FOW visibility checks */
	private FogOfWarMidTurnVisibility fogOfWarMidTurnVisibility;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/** Movement utils */
	private MovementUtils movementUtils;
	
	/** The spell to switch off */
	private int spellURN;
	
	/** Methods dealing with combat maps that are only needed on the server */
	private CombatMapServerUtils combatMapServerUtils;
	
	/** If true, the spell will be removed in player's memory both on the server and clients, but won't be removed from the server's true memory */
	private boolean retainSpellInServerTrueMemory;
	
	/**
	 * @return Enum indicating which kind of update this is
	 */
	@Override
	public final KindOfWorldUpdate getKindOfWorldUpdate ()
	{
		return KindOfWorldUpdate.SWITCH_OFF_SPELL;
	}
	
	/**
	 * @param o Other object to compare against
	 * @return Whether this and the other object hold the same values
	 */
	@Override
	public final boolean equals (final Object o)
	{
		final boolean e;
		if (o instanceof SwitchOffSpellUpdate)
			e = (getSpellURN () == ((SwitchOffSpellUpdate) o).getSpellURN ());
		else
			e = false;
		
		return e;
	}
	
	/**
	 * @return String representation of class, for debug messages
	 */
	@Override
	public final String toString ()
	{
		String s = "Switch off spell URN " + getSpellURN ();
		
		if (isRetainSpellInServerTrueMemory ())
			s = s + ", but keep it in server's true memory";
		
		return s;
	}
	
	/**
	 * Processes this update
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether this update was processed and/or generated any further updates
	 * @throws JAXBException If there is a problem sending some message to the client
	 * @throws XMLStreamException If there is a problem sending some message to the client
	 * @throws RecordNotFoundException If we find a game element (unit, building or so on) that we can't find the definition for in the DB
	 * @throws PlayerNotFoundException If we can't find the player who owns a game element
	 * @throws MomException If there are any issues with data or calculation logic
	 */
	@Override
	public final WorldUpdateResult process (final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		WorldUpdateResult result = WorldUpdateResult.DONE;
		
		// If the spell generates a CAE, switch that off first
		final MemoryMaintainedSpell trueSpell = getMemoryMaintainedSpellUtils ().findSpellURN
			(getSpellURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), "SwitchOffSpellUpdate");
		final Spell spellDef = mom.getServerDB ().findSpell (trueSpell.getSpellID (), "SwitchOffSpellUpdate");

		// Overland enchantments
		if (spellDef.getSpellBookSectionID () == SpellBookSectionID.OVERLAND_ENCHANTMENTS)
		{
			// Check each combat area effect that this overland enchantment gives to see if we have any of them in effect - if so cancel them
			for (final String combatAreaEffectID: spellDef.getSpellHasCombatEffect ())
			{
				final MemoryCombatAreaEffect cae = getMemoryCombatAreaEffectUtils ().findCombatAreaEffect
					(mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), null, combatAreaEffectID, trueSpell.getCastingPlayerID ());
				
				if (cae != null)
					if (mom.getWorldUpdates ().removeCombatAreaEffect (cae.getCombatAreaEffectURN ()))
						result = WorldUpdateResult.REDO_BECAUSE_EARLIER_UPDATES_ADDED;
			}
		}
		
		else if (trueSpell.getCitySpellEffectID () != null)
		{
			final CitySpellEffect citySpellEffect = mom.getServerDB ().findCitySpellEffect (trueSpell.getCitySpellEffectID (), "SwitchOffSpellUpdate");
			if (citySpellEffect.getCombatAreaEffectID () != null)
			{
				final MemoryCombatAreaEffect trueCAE = getMemoryCombatAreaEffectUtils ().findCombatAreaEffect
					(mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), (MapCoordinates3DEx) trueSpell.getCityLocation (),
						citySpellEffect.getCombatAreaEffectID (), trueSpell.getCastingPlayerID ());
				
				if (trueCAE != null)
					if (mom.getWorldUpdates ().removeCombatAreaEffect (trueCAE.getCombatAreaEffectURN ()))
						result = WorldUpdateResult.REDO_BECAUSE_EARLIER_UPDATES_ADDED;
			}
		}
		
		
		if (result == WorldUpdateResult.DONE)
		{
			// Switch off on server
			if (!isRetainSpellInServerTrueMemory ())
				getMemoryMaintainedSpellUtils ().removeSpellURN (getSpellURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ());
	
			// Build the message ready to send it to whoever could see the spell
			final SwitchOffMaintainedSpellMessage msg = new SwitchOffMaintainedSpellMessage ();
			msg.setSpellURN (getSpellURN ());
	
			// Check which players could see the spell
			for (final PlayerServerDetails player : mom.getPlayers ())
			{
				final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
				if ((getMemoryMaintainedSpellUtils ().findSpellURN (getSpellURN (), priv.getFogOfWarMemory ().getMaintainedSpell ()) != null) &&
					(getFogOfWarMidTurnVisibility ().canSeeSpellMidTurn (trueSpell, player, mom)))
				{
					// Update player's memory on server
					getMemoryMaintainedSpellUtils ().removeSpellURN (getSpellURN (), priv.getFogOfWarMemory ().getMaintainedSpell ());
	
					// Update on client
					if (player.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
						player.getConnection ().sendMessageToClient (msg);
				}
			}
			
			// The only spells with a citySpellEffectID that can be cast in combat are Wall of Fire / Wall of Darkness.
			// If these get cancelled, we need to regenerate the combat map.
			// Can only do this after the spell is removed, or regenerating the combat map will still see the spell existing.
			if (((spellDef.getSpellBookSectionID () == SpellBookSectionID.CITY_ENCHANTMENTS) || (spellDef.getSpellBookSectionID () == SpellBookSectionID.CITY_CURSES)) &&
				(spellDef.getCombatCastingCost () != null) && (trueSpell.getCitySpellEffectID () != null))
			{
				final CombatPlayers combatPlayers = getCombatMapUtils ().determinePlayersInCombatFromLocation
					((MapCoordinates3DEx) trueSpell.getCityLocation (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), mom.getPlayers (), mom.getServerDB ());
				if (combatPlayers.bothFound ())
				{
					final PlayerServerDetails attackingPlayer = (PlayerServerDetails) combatPlayers.getAttackingPlayer ();
					final PlayerServerDetails defendingPlayer = (PlayerServerDetails) combatPlayers.getDefendingPlayer ();
				
					final CombatDetails combatDetails = getCombatMapServerUtils ().findCombatByLocation (mom.getCombatDetails (),
						(MapCoordinates3DEx) trueSpell.getCityLocation (), "SwitchOffSpellUpdate");
					
					getCombatMapGenerator ().regenerateCombatTileBorders (combatDetails.getCombatMap (), mom.getServerDB (),
						mom.getGeneralServerKnowledge ().getTrueMap (), mom.getSessionDescription ().getOverlandMapSize (), (MapCoordinates3DEx) trueSpell.getCityLocation ());
					
					// Send the updated map
					final UpdateCombatMapMessage combatMapMsg = new UpdateCombatMapMessage ();
					combatMapMsg.setCombatLocation (trueSpell.getCityLocation ());
					combatMapMsg.setCombatTerrain (combatDetails.getCombatMap ());
					
					if (attackingPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
						attackingPlayer.getConnection ().sendMessageToClient (combatMapMsg);

					if (defendingPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
						defendingPlayer.getConnection ().sendMessageToClient (combatMapMsg);
				}
			}
			
			// If spell was cast on a unit, then see the spell gave it an HP boost.  If so then removing the spell may have killed it.
			// e.g. Unit has 5 HP, cast Lionheart on it in combat gives +3 so now has 8 HP.  Unit takes 6 HP damage, then wins the combat.
			// Lionheart gets cancelled so now unit has -1 HP.  Heroism can have the same effect because Elite units have +1 HP per figure.
			if (trueSpell.getUnitURN () != null)
			{
				boolean killed = false;
				final MemoryUnit mu = getUnitUtils ().findUnitURN (trueSpell.getUnitURN (),
					mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), "SwitchOffSpellUpdate");
				ExpandedUnitDetails xu = null;
				
				if (trueSpell.getUnitSkillID () != null)
				{
					final UnitSkillEx unitSkill = mom.getServerDB ().findUnitSkill (trueSpell.getUnitSkillID (), "SwitchOffSpellUpdate");
					if (unitSkill.getAddsToSkill ().stream ().anyMatch
						(s -> (s.getAddsToSkillID ().equals (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS) ||
							(s.getAddsToSkillID ().equals (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE)))))
					{
						xu = getExpandUnitDetails ().expandUnitDetails (mu, null, null, null,
							mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
						if (xu.calculateAliveFigureCount () <= 0)
							killed = true;
					}
				}
				
				if ((!killed) && (mu.getCombatLocation () != null) && (mu.getCombatPosition () != null))
				{
					// Make sure the unit is still able to be on the combat tile it is on, and that we didn't lose our flight spell over water
					final CombatDetails combatDetails = getCombatMapServerUtils ().findCombatByLocation (mom.getCombatDetails (),
						(MapCoordinates3DEx) mu.getCombatLocation (), "SwitchOffSpellUpdate");
					
					final MomCombatTile tile = combatDetails.getCombatMap ().getRow ().get (mu.getCombatPosition ().getY ()).getCell ().get (mu.getCombatPosition ().getX ());

					if (xu == null)
						xu = getExpandUnitDetails ().expandUnitDetails (mu, null, null, null,
							mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
					
					if (getMovementUtils ().calculateDoubleMovementToEnterCombatTile (xu, tile, mom.getServerDB ()) < 0)
						killed = true;
				}
				
				if (killed)
				{
					// Work out if this is happening in combat or not
					final KillUnitActionID action = (mu.getCombatLocation () == null) ? KillUnitActionID.HEALABLE_OVERLAND_DAMAGE : KillUnitActionID.HEALABLE_COMBAT_DAMAGE;
					if (mom.getWorldUpdates ().killUnit (trueSpell.getUnitURN (), action))
						result = WorldUpdateResult.DONE_AND_LATER_UPDATES_ADDED;
				}
				
				// Regardless of whether the unit died, recheck the map cell it is in.  Maybe a unit over water lost a flight spell and killed itself,
				// or maybe it lost a wind walking spell and will kill the whole stack.
				if ((mu.getCombatLocation () == null) && (mu.getCombatPosition () == null))
					if (mom.getWorldUpdates ().recheckTransportCapacity ((MapCoordinates3DEx) mu.getUnitLocation ()))
						result = WorldUpdateResult.DONE_AND_LATER_UPDATES_ADDED;
			}
			
			// If the spell was cast on a city, better recalculate everything on the city
			else if (trueSpell.getCityLocation () != null)
			{
				if (mom.getWorldUpdates ().recalculateCity ((MapCoordinates3DEx) trueSpell.getCityLocation ()))
					result = WorldUpdateResult.DONE_AND_LATER_UPDATES_ADDED;
			}
			
			// If it was a global enchantment that gives +HP then recheck every unit (Charm of Life)
			else if (spellDef.getSpellBookSectionID () == SpellBookSectionID.OVERLAND_ENCHANTMENTS)
			{
				// This is a pretty long winded way we have to find this, especially since the CAE will already have been switched off
				// (it has to be done like that, as its the CAE that has the +HP boost defined on it, so if that isn't switched off first, we couldn't recheck the units)
				for (final String combatAreaEffectID : spellDef.getSpellHasCombatEffect ())
				{
					final CombatAreaEffect caeDef = mom.getServerDB ().findCombatAreaEffect (combatAreaEffectID, "SwitchOffSpellUpdate");
					if (caeDef.getCombatAreaAffectsPlayers () == CombatAreaAffectsPlayersID.CASTER_ONLY)
						for (final String unitSkillID : caeDef.getCombatAreaEffectGrantsSkill ())
						{
							final UnitSkillEx unitSkill = mom.getServerDB ().findUnitSkill (unitSkillID, "SwitchOffSpellUpdate");
							if (unitSkill.getAddsToSkill ().stream ().anyMatch (s -> s.getAddsToSkillID ().equals (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS)))
								for (final MemoryUnit mu : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
									if ((mu.getOwningPlayerID () == trueSpell.getCastingPlayerID ()) && (mu.getStatus () == UnitStatusID.ALIVE))
									{
										final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (mu, null, null, null,
											mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
										if (xu.calculateAliveFigureCount () <= 0)
											if (mom.getWorldUpdates ().killUnit (mu.getUnitURN (), KillUnitActionID.HEALABLE_OVERLAND_DAMAGE))
												result = WorldUpdateResult.DONE_AND_LATER_UPDATES_ADDED;
									}
						}
				}
			}
			
			// The removed spell might be Awareness, Nature Awareness, Nature's Eye, or a curse on an enemy city, so might affect the fog of war of the player who cast it
			if (mom.getWorldUpdates ().recalculateFogOfWar (trueSpell.getCastingPlayerID ()))
				result = WorldUpdateResult.DONE_AND_LATER_UPDATES_ADDED;
			
			// Spell probably had some upkeep
			if (mom.getWorldUpdates ().recalculateProduction (trueSpell.getCastingPlayerID ()))
				result = WorldUpdateResult.DONE_AND_LATER_UPDATES_ADDED;
		}
		
		return result;
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
	
	/**
	 * @return Combat map utils
	 */
	public final CombatMapUtils getCombatMapUtils ()
	{
		return combatMapUtils;
	}
	
	/**
	 * @param utils Combat map utils
	 */
	public final void setCombatMapUtils (final CombatMapUtils utils)
	{
		combatMapUtils = utils;
	}
	
	/**
	 * @return Map generator
	 */
	public final CombatMapGenerator getCombatMapGenerator ()
	{
		return combatMapGenerator;
	}

	/**
	 * @param gen Map generator
	 */
	public final void setCombatMapGenerator (final CombatMapGenerator gen)
	{
		combatMapGenerator = gen;
	}
	
	/**
	 * @return FOW visibility checks
	 */
	public final FogOfWarMidTurnVisibility getFogOfWarMidTurnVisibility ()
	{
		return fogOfWarMidTurnVisibility;
	}

	/**
	 * @param vis FOW visibility checks
	 */
	public final void setFogOfWarMidTurnVisibility (final FogOfWarMidTurnVisibility vis)
	{
		fogOfWarMidTurnVisibility = vis;
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
	 * @return Movement utils
	 */
	public final MovementUtils getMovementUtils ()
	{
		return movementUtils;
	}

	/**
	 * @param u Movement utils
	 */
	public final void setMovementUtils (final MovementUtils u)
	{
		movementUtils = u;
	}
	
	/**
	 * @return The spell to switch off
	 */
	public final int getSpellURN ()
	{
		return spellURN;
	}

	/**
	 * @param s The spell to switch off
	 */
	public final void setSpellURN (final int s)
	{
		spellURN = s;
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
	 * @return If true, the spell will be removed in player's memory both on the server and clients, but won't be removed from the server's true memory
	 */
	public final boolean isRetainSpellInServerTrueMemory ()
	{
		return retainSpellInServerTrueMemory;
	}

	/**
	 * @param r If true, the spell will be removed in player's memory both on the server and clients, but won't be removed from the server's true memory
	 */
	public final void setRetainSpellInServerTrueMemory (final boolean r)
	{
		retainSpellInServerTrueMemory = r;
	}
}