package momime.server.worldupdates;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.KillUnitMessage;
import momime.common.utils.PendingMovementUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.fogofwar.FogOfWarMidTurnVisibility;
import momime.server.fogofwar.KillUnitActionID;
import momime.server.knowledge.ServerGridCellEx;

/**
 * World update for killing a unit, whether that means really removing it permanently or just marking it as dead 
 */
public final class KillUnitUpdate implements WorldUpdate
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (KillUnitUpdate.class);
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** FOW visibility checks */
	private FogOfWarMidTurnVisibility fogOfWarMidTurnVisibility;
	
	/** Pending movement utils */
	private PendingMovementUtils pendingMovementUtils;
	
	/** The unit to set to kill */
	private int unitURN;
	
	/** Method by which the unit is being killed; this controls whether the unit is fully removed, or just marked as dead and could be raised */
	private KillUnitActionID untransmittedAction;
	
	/**
	 * @return Enum indicating which kind of update this is
	 */
	@Override
	public final KindOfWorldUpdate getKindOfWorldUpdate ()
	{
		return KindOfWorldUpdate.KILL_UNIT;
	}
	
	/**
	 * @param o Other object to compare against
	 * @return Whether this and the other object hold the same values
	 */
	@Override
	public final boolean equals (final Object o)
	{
		final boolean e;
		if (o instanceof KillUnitUpdate)
			e = (getUnitURN () == ((KillUnitUpdate) o).getUnitURN ());
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
		return "Kill unit URN " + getUnitURN () + " as " + getUntransmittedAction ();
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
		
		// If the unit has any spells cast on it, remove those first
		for (final MemoryMaintainedSpell spell : mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ())
			if ((spell.getUnitURN () != null) && (spell.getUnitURN () == getUnitURN ()))
			{
				if (mom.getWorldUpdates ().switchOffSpell (spell.getSpellURN ()))
					result = WorldUpdateResult.REDO_BECAUSE_EARLIER_UPDATES_ADDED;
			}
		
		if (result == WorldUpdateResult.DONE)
		{
			final MemoryUnit trueUnit = getUnitUtils ().findUnitURN (getUnitURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), "KillUnitUpdate (tu)");

			final String unitMagicRealmID = mom.getServerDB ().findUnit (trueUnit.getUnitID (), "KillUnitUpdate").getUnitMagicRealm ();
			final boolean isHero = unitMagicRealmID.equals (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
			
			// If the unit was a hero dying in combat, move any items they had into the pool for the winner of the combat to claim
			final ServerGridCellEx gc = (trueUnit.getCombatLocation () == null) ? null :
				(ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get (trueUnit.getCombatLocation ().getZ ()).getRow ().get
					(trueUnit.getCombatLocation ().getY ()).getCell ().get (trueUnit.getCombatLocation ().getX ());

			if ((trueUnit.getCombatLocation () != null) && (untransmittedAction != KillUnitActionID.PERMANENT_DAMAGE))
				trueUnit.getHeroItemSlot ().stream ().filter (slot -> (slot.getHeroItem () != null)).forEach (slot ->
				{
					gc.getItemsFromHeroesWhoDiedInCombat ().add (slot.getHeroItem ());
					slot.setHeroItem (null);
				});
			
			// Check which players could see the unit
			for (final PlayerServerDetails player : mom.getPlayers ())
			{
				if (getFogOfWarMidTurnVisibility ().canSeeUnitMidTurn (trueUnit, mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
					player, mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting ()))
				{
					final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
					
					// Remove unit from players' memory on server - this doesn't suffer from the issue described below so we can just do it
					getPendingMovementUtils ().removeUnitFromAnyPendingMoves (priv.getPendingMovement (), trueUnit.getUnitURN ());
					
					// Map the transmittedAction to a unit status
					final UnitStatusID newStatusInPlayersMemoryOnServer;
					final UnitStatusID newStatusInPlayersMemoryOnClient;
					
					switch (untransmittedAction)
					{
						// Heroes are killed outright on the clients (even if ours, and just dismissing him and may resummon him later), but return to 'Generated' status on the server below
						case PERMANENT_DAMAGE:
						case DISMISS:
							newStatusInPlayersMemoryOnServer = null;
							newStatusInPlayersMemoryOnClient = null;
							break;
							
						// If its our unit or hero dying from lack of production then the client still needs the unit object left around temporarily while it sorts the NTM out.
						// But anybody else's units dying from lack of production can just be removed.
					    case LACK_OF_PRODUCTION:
							newStatusInPlayersMemoryOnServer = null;
							if (trueUnit.getOwningPlayerID () == player.getPlayerDescription ().getPlayerID ())
								newStatusInPlayersMemoryOnClient = UnitStatusID.KILLED_BY_LACK_OF_PRODUCTION;
							else
								newStatusInPlayersMemoryOnClient = null;
							break;
						
						// If we're not involved in the combat, then units are remove immediately from the client.
						// If its somebody else's hero dying, then they're remove immediately from the client.
						// If its a regular unit dying in a combat we're involved in, or our own hero dying, then we might raise/animate dead it, so mark those as dead but don't remove them.
					    case HEALABLE_COMBAT_DAMAGE:
							if (trueUnit.getOwningPlayerID () == player.getPlayerDescription ().getPlayerID ())
								newStatusInPlayersMemoryOnServer = UnitStatusID.DEAD;
							else if (isHero)
								newStatusInPlayersMemoryOnServer = null;
							else if ((player.getPlayerDescription ().getPlayerID ().equals (gc.getAttackingPlayerID ())) || (player.getPlayerDescription ().getPlayerID ().equals (gc.getDefendingPlayerID ())))
								newStatusInPlayersMemoryOnServer = UnitStatusID.DEAD;
							else
								newStatusInPlayersMemoryOnServer = null;
							
							newStatusInPlayersMemoryOnClient = newStatusInPlayersMemoryOnServer;
					    	break;
					    	
					    // As above, but immediately remove regular units even if we own them
					    case HEALABLE_OVERLAND_DAMAGE:
					    	if (isHero && (trueUnit.getOwningPlayerID () == player.getPlayerDescription ().getPlayerID ()))
								newStatusInPlayersMemoryOnServer = UnitStatusID.DEAD;
					    	else
					    		newStatusInPlayersMemoryOnServer = null;

							newStatusInPlayersMemoryOnClient = newStatusInPlayersMemoryOnServer;
					    	break;
					    	
					    default:
					    	throw new MomException ("killUnitOnServerAndClients doesn't know what unit status to convert " + untransmittedAction + " into");
					}

					// If still in combat, only set to DEAD in player's memory on server, rather than removing entirely
					if (newStatusInPlayersMemoryOnServer == null)
					{
						log.debug ("Removing unit URN " + trueUnit.getUnitURN () + " from player ID " + player.getPlayerDescription ().getPlayerID () + "'s memory on server");
						getUnitUtils ().removeUnitURN (trueUnit.getUnitURN (), priv.getFogOfWarMemory ().getUnit ());
					}
					else
					{
						log.debug ("Marking unit URN " + trueUnit.getUnitURN () + " as " + newStatusInPlayersMemoryOnServer + " in player ID " + player.getPlayerDescription ().getPlayerID () + "'s memory on server");
						getUnitUtils ().findUnitURN (trueUnit.getUnitURN (), priv.getFogOfWarMemory ().getUnit (), "KillUnitUpdate (mu)").setStatus (newStatusInPlayersMemoryOnServer);
					}
					
					if (player.getPlayerDescription ().isHuman ())
					{
						// New status has to be set per player depending on who can see it
						log.debug ("Telling client to mark unit URN " + trueUnit.getUnitURN () + " as " + newStatusInPlayersMemoryOnClient + " in player ID " + player.getPlayerDescription ().getPlayerID () + "'s memory");

						final KillUnitMessage msg = new KillUnitMessage ();
						msg.setUnitURN (trueUnit.getUnitURN ());
						msg.setNewStatus (newStatusInPlayersMemoryOnClient);
						
						player.getConnection ().sendMessageToClient (msg);
					}
				}
			}

			// Now update server's true memory
			switch (untransmittedAction)
			{
				// Complete remove unit
				case PERMANENT_DAMAGE:
					log.debug ("Permanently removing unit URN " + trueUnit.getUnitURN () + " from server's true memory");
					getUnitUtils ().removeUnitURN (trueUnit.getUnitURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ());
					break;

				// Dismissed heroes go back to Generated
				// Heroes dismissed by lack of production go back to Generated
				case DISMISS:
				case LACK_OF_PRODUCTION:
					if (isHero)
					{
						log.debug ("Setting hero with unit URN " + trueUnit.getUnitURN () + " back to generated in server's true memory (dismissed or lack of production)");
						trueUnit.setStatus (UnitStatusID.GENERATED);
					}
					else
					{
						log.debug ("Permanently removing unit URN " + trueUnit.getUnitURN () + " from server's true memory (dismissed or lack of production)");
						getUnitUtils ().removeUnitURN (trueUnit.getUnitURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ());
					}
					break;
					
				// Killed by taking damage in combat.
				// All units killed by combat damage are kept around for the moment, since one of the players in the combat may Raise Dead them.
				// Heroes are kept at DEAD even after the combat ends, in case the player resurrects them.
				case HEALABLE_COMBAT_DAMAGE:
					log.debug ("Marking unit with unit URN " + trueUnit.getUnitURN () + " as dead in server's true memory (combat damage)");
					trueUnit.setStatus (UnitStatusID.DEAD);
					break;

				// Killed by taking damage overland.
				// As above except we only need to mark heroes as DEAD, since there's no way to resurrect regular units on the overland map.
				case HEALABLE_OVERLAND_DAMAGE:
					if (isHero)
					{
						log.debug ("Marking hero with unit URN " + trueUnit.getUnitURN () + " as dead in server's true memory (overland damage)");
						trueUnit.setStatus (UnitStatusID.DEAD);
					}
					else
					{
						log.debug ("Permanently removing unit URN " + trueUnit.getUnitURN () + " from server's true memory (overland damage)");
						getUnitUtils ().removeUnitURN (trueUnit.getUnitURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ());
					}
					break;
					
				default:
					throw new MomException ("killUnitOnServerAndClients doesn't know what to do with true units when action = " + untransmittedAction);
			}
			
			// If the unit died overland, recheck the remaining units at the location (if it died in combat, this is deferred until the combat ends)
			if (trueUnit.getCombatLocation () == null)
			{
				// Was the unit in a city?  If so recalculate the city; if not recalculate the unit stack
				// Units can have no location, for example generated heroes that were never actually used
				if (trueUnit.getUnitLocation () != null)
				{
					final MemoryGridCell tc = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
						(trueUnit.getUnitLocation ().getZ ()).getRow ().get (trueUnit.getUnitLocation ().getY ()).getCell ().get (trueUnit.getUnitLocation ().getX ());
					
					if (tc.getCityData () != null)
					{
						// Units other than summoned units help reduce unrest if they are in a city
						if (!mom.getServerDB ().findPick (unitMagicRealmID, "KillUnitUpdate").getUnitTypeID ().equals (CommonDatabaseConstants.UNIT_TYPE_ID_SUMMONED))
							if (mom.getWorldUpdates ().recalculateCity ((MapCoordinates3DEx) trueUnit.getUnitLocation ()))
								result = WorldUpdateResult.DONE_AND_LATER_UPDATES_ADDED;
					}
				}
				else
				{
					if (mom.getWorldUpdates ().recheckTransportCapacity ((MapCoordinates3DEx) trueUnit.getUnitLocation ()))
						result = WorldUpdateResult.DONE_AND_LATER_UPDATES_ADDED;
				}
			
				// Unit probably had some upkeep
				if (mom.getWorldUpdates ().recalculateProduction (trueUnit.getOwningPlayerID ()))
					result = WorldUpdateResult.DONE_AND_LATER_UPDATES_ADDED;
				
				// Unit might have been the only one who could see certain areas of the map
				if (mom.getWorldUpdates ().recalculateFogOfWar (trueUnit.getOwningPlayerID ()))
					result = WorldUpdateResult.DONE_AND_LATER_UPDATES_ADDED;
			}
		}
		
		return result;
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
	 * @return Pending movement utils
	 */
	public final PendingMovementUtils getPendingMovementUtils ()
	{
		return pendingMovementUtils;
	}

	/**
	 * @param utils Pending movement utils
	 */
	public final void setPendingMovementUtils (final PendingMovementUtils utils)
	{
		pendingMovementUtils = utils;
	}
	
	/**
	 * @return The unit to set to kill
	 */
	public final int getUnitURN ()
	{
		return unitURN;
	}

	/**
	 * @param u The unit to set to kill
	 */
	public final void setUnitURN (final int u)
	{
		unitURN = u;
	}
	
	/**
	 * @return Method by which the unit is being killed; this controls whether the unit is fully removed, or just marked as dead and could be raised
	 */
	public final KillUnitActionID getUntransmittedAction ()
	{
		return untransmittedAction;
	}
	
	/**
	 * @param a Method by which the unit is being killed; this controls whether the unit is fully removed, or just marked as dead and could be raised
	 */
	public final void setUntransmittedAction (final KillUnitActionID a)
	{
		untransmittedAction = a;
	}
}