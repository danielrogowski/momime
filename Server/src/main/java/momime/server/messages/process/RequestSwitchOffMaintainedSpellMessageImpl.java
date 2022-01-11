package momime.server.messages.process;

import java.io.IOException;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

import momime.common.database.Spell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.UnitStatusID;
import momime.common.messages.clienttoserver.RequestSwitchOffMaintainedSpellMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.utils.CombatMapUtils;
import momime.common.utils.CombatPlayers;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.knowledge.CombatDetails;
import momime.server.process.CombatStartAndEnd;
import momime.server.process.DamageProcessor;
import momime.server.utils.CombatMapServerUtils;
import momime.server.utils.PlayerServerUtils;

/**
 * Client sends this when they want to switch off a maintained spell (overland, unit or city).
 * Note because spells don't get allocated any kind of spellURN, we basically have to pass the full details of the spell to the server to make sure that it can find it.
 */
public final class RequestSwitchOffMaintainedSpellMessageImpl extends RequestSwitchOffMaintainedSpellMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (RequestSwitchOffMaintainedSpellMessageImpl.class);

	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Combat map utils */
	private CombatMapUtils combatMapUtils;
	
	/** Damage processor */
	private DamageProcessor damageProcessor;
	
	/** Starting and ending combats */
	private CombatStartAndEnd combatStartAndEnd;
	
	/** Player utils */
	private PlayerServerUtils playerServerUtils;
	
	/** Methods dealing with combat maps that are only needed on the server */
	private CombatMapServerUtils combatMapServerUtils;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there was another kind of problem
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, IOException
	{
		final MomSessionVariables mom = (MomSessionVariables) thread;

		// Look for the spell
		final MemoryMaintainedSpell trueSpell = getMemoryMaintainedSpellUtils ().findSpellURN (getSpellURN (),
			mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ());

		final Spell spellDef = (trueSpell == null) ? null : mom.getServerDB ().findSpell (trueSpell.getSpellID (), "RequestSwitchOffMaintainedSpellMessageImpl");
		
		// Do some checks
		final String error;
		if (!getPlayerServerUtils ().isPlayerTurn (sender, mom.getGeneralPublicKnowledge (), mom.getSessionDescription ().getTurnSystem ()))
			error = "You can't switch off spells when it isn't your turn";
		else if (trueSpell == null)
			error = "Couldn't find the spell you wanted to switch off";
		else if (!sender.getPlayerDescription ().getPlayerID ().equals (trueSpell.getCastingPlayerID ()))
			error = "You cannot switch off another wizard's spells!";
		else if ((spellDef.isPermanent () != null) && (spellDef.isPermanent ()))
			error = "You cannot switch off permanent spells";
		else if (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ().stream ().anyMatch (u -> (u.getStatus () == UnitStatusID.ALIVE) && (u.getCombatLocation () != null)))
			error = "You can't switch off spells when there is a combat in progress";
		else
			error = null;
		
		// All ok?
		if (error != null)
		{
			// Return error
			log.warn ("process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + error);

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (error);
			sender.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			// If the spell is cast on a unit then grab it now, because we could potentially kill it when we turn the spell off
			final MemoryUnit trueUnit = (trueSpell.getUnitURN () == null) ? null : getUnitUtils ().findUnitURN
				(trueSpell.getUnitURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), "switchOffSpell");
			
			// Once its dead we also won't be able to figure out who the players in combat were
			final CombatPlayers combatPlayers = ((trueUnit == null) || (trueUnit.getCombatLocation () == null)) ? null :
				getCombatMapUtils ().determinePlayersInCombatFromLocation ((MapCoordinates3DEx) trueUnit.getCombatLocation (),
					mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), mom.getPlayers (), mom.getServerDB ()); 
			
			// Switch off spell + associated CAEs
			mom.getWorldUpdates ().switchOffSpell (getSpellURN ());
			mom.getWorldUpdates ().process (mom);
			
			// Did switching off the spell lose the combat?
			if ((combatPlayers != null) && (combatPlayers.bothFound ()))
				if (getDamageProcessor ().countUnitsInCombat ((MapCoordinates3DEx) trueUnit.getCombatLocation (),
					trueUnit.getCombatSide (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), mom.getServerDB ()) == 0)
				{
					final PlayerServerDetails attackingPlayer = (PlayerServerDetails) combatPlayers.getAttackingPlayer ();
					final PlayerServerDetails defendingPlayer = (PlayerServerDetails) combatPlayers.getDefendingPlayer ();
					
					final CombatDetails combatDetails = getCombatMapServerUtils ().findCombatByLocation (mom.getCombatDetails (),
						(MapCoordinates3DEx) trueUnit.getCombatLocation (), "RequestSwitchOffMaintainedSpellMessageImpl");
					
					getCombatStartAndEnd ().combatEnded (combatDetails, attackingPlayer, defendingPlayer,
						(trueUnit.getOwningPlayerID () == attackingPlayer.getPlayerDescription ().getPlayerID ()) ? defendingPlayer : attackingPlayer,
						null, mom);
				}
		}
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
	 * @return Combat map utils
	 */
	public final CombatMapUtils getCombatMapUtils ()
	{
		return combatMapUtils;
	}

	/**
	 * @param util Combat map utils
	 */
	public final void setCombatMapUtils (final CombatMapUtils util)
	{
		combatMapUtils = util;
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
	 * @return Starting and ending combats
	 */
	public final CombatStartAndEnd getCombatStartAndEnd ()
	{
		return combatStartAndEnd;
	}

	/**
	 * @param cse Starting and ending combats
	 */
	public final void setCombatStartAndEnd (final CombatStartAndEnd cse)
	{
		combatStartAndEnd = cse;
	}

	/**
	 * @return Player utils
	 */
	public final PlayerServerUtils getPlayerServerUtils ()
	{
		return playerServerUtils;
	}
	
	/**
	 * @param utils Player utils
	 */
	public final void setPlayerServerUtils (final PlayerServerUtils utils)
	{
		playerServerUtils = utils;
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