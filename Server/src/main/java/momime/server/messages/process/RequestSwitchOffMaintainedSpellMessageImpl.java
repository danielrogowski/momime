package momime.server.messages.process;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
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
import momime.server.calculations.ServerResourceCalculations;
import momime.server.process.CombatStartAndEnd;
import momime.server.process.DamageProcessor;
import momime.server.process.SpellProcessing;
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
	
	/** Resource calculations */
	private ServerResourceCalculations serverResourceCalculations;

	/** Spell processing methods */
	private SpellProcessing spellProcessing;
	
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
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, MomException, PlayerNotFoundException, RecordNotFoundException
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
			if (getSpellProcessing ().switchOffSpell (getSpellURN (), mom))
				
				// Unit died - if it was in combat, did switching off the spell lose the combat?
				if ((combatPlayers != null) && (combatPlayers.bothFound ()))
					if (getDamageProcessor ().countUnitsInCombat ((MapCoordinates3DEx) trueUnit.getCombatLocation (),
						trueUnit.getCombatSide (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), mom.getServerDB ()) == 0)
					{
						final PlayerServerDetails attackingPlayer = (PlayerServerDetails) combatPlayers.getAttackingPlayer ();
						final PlayerServerDetails defendingPlayer = (PlayerServerDetails) combatPlayers.getDefendingPlayer ();
						
						getCombatStartAndEnd ().combatEnded ((MapCoordinates3DEx) trueUnit.getCombatLocation (),
							attackingPlayer, defendingPlayer,
							(trueUnit.getOwningPlayerID () == attackingPlayer.getPlayerDescription ().getPlayerID ()) ? defendingPlayer : attackingPlayer,
							null, mom);
					}
			
			// Spell no longer using mana
			getServerResourceCalculations ().recalculateGlobalProductionValues (sender.getPlayerDescription ().getPlayerID (), false, mom);
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
	 * @return Resource calculations
	 */
	public final ServerResourceCalculations getServerResourceCalculations ()
	{
		return serverResourceCalculations;
	}

	/**
	 * @param calc Resource calculations
	 */
	public final void setServerResourceCalculations (final ServerResourceCalculations calc)
	{
		serverResourceCalculations = calc;
	}

	/**
	 * @return Spell processing methods
	 */
	public final SpellProcessing getSpellProcessing ()
	{
		return spellProcessing;
	}

	/**
	 * @param obj Spell processing methods
	 */
	public final void setSpellProcessing (final SpellProcessing obj)
	{
		spellProcessing = obj;
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
}