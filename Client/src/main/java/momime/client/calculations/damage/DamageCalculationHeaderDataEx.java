package momime.client.calculations.damage;

import java.io.IOException;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.client.utils.WizardClientUtils;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.DamageCalculationHeaderData;
import momime.common.messages.servertoclient.DamageCalculationMessageTypeID;
import momime.common.utils.UnitUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * Header about how the source and target of an attack, before any attacks or counterattacks are rolled
 */
public final class DamageCalculationHeaderDataEx extends DamageCalculationHeaderData implements DamageCalculationText
{
	/** Class logger */
	private final Log log = LogFactory.getLog (DamageCalculationHeaderDataEx.class);
	
	/** Attacking unit */
	private MemoryUnit attackerUnit;
	
	/** Unit being attacked */
	private MemoryUnit defenderUnit;
	
	/** Player who owns the attacking unit */
	private PlayerPublicDetails attackingPlayer;
	
	/** Player who owns unit being attacked */
	private PlayerPublicDetails defenderPlayer;

	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Unit utils */
	private UnitUtils unitUtils;

	/** Multiplayer client */
	private MomClient client;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Client-side unit utils */
	private UnitClientUtils unitClientUtils;
	
	/** Wizard client utils */
	private WizardClientUtils wizardClientUtils;
	
	/**
	 * These damage calculations can live in the history for a long time, easily after the unit(s) in question have been destroyed
	 * and potentially even after the player(s) who own the unit(s) have been kicked out of the game.
	 * So find and record them up front, so we can't fail to find the unitURNs or playerIDs later.
	 * @throws IOException If there is a problem
	 */
	@Override
	public final void preProcess () throws IOException
	{
		log.trace ("Entering preProcess");
		
		setAttackerUnit (getUnitUtils ().findUnitURN (getAttackerUnitURN (),
	    	getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "DamageCalculationHeaderDataEx-au"));

	    setDefenderUnit (getUnitUtils ().findUnitURN (getDefenderUnitURN (),
	    	getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "DamageCalculationHeaderDataEx-du"));
		    
	    setAttackingPlayer (getMultiplayerSessionUtils ().findPlayerWithID
	    	(getClient ().getPlayers (), getAttackerUnit ().getOwningPlayerID (), "DamageCalculationHeaderDataEx-ap"));

	    setDefenderPlayer (getMultiplayerSessionUtils ().findPlayerWithID
	    	(getClient ().getPlayers (), getDefenderUnit ().getOwningPlayerID (), "DamageCalculationHeaderDataEx-dp"));

		log.trace ("Exiting preProcess");
	}
	
	/**
	 * @return Text to display for this breakdown line
	 * @throws IOException If there is a problem
	 */
	@Override
	public final String getText () throws IOException
	{
		final String languageEntryID = (getMessageType () == DamageCalculationMessageTypeID.MELEE_ATTACK) ? "MeleeAttack" : "RangedAttack";
		return getLanguage ().findCategoryEntry ("CombatDamage", languageEntryID).replaceAll
			("ATTACKER_NAME", getWizardClientUtils ().getPlayerName (getAttackingPlayer ())).replaceAll
			("DEFENDER_NAME", getWizardClientUtils ().getPlayerName (getDefenderPlayer ())).replaceAll
			("ATTACKER_RACE_UNIT_NAME", getUnitClientUtils ().getUnitName (getAttackerUnit (), UnitNameType.RACE_UNIT_NAME)).replaceAll
			("DEFENDER_RACE_UNIT_NAME", getUnitClientUtils ().getUnitName (getDefenderUnit (), UnitNameType.RACE_UNIT_NAME));
	}
	
	/**
	 * @return Attacking unit
	 */
	public final MemoryUnit getAttackerUnit ()
	{
		return attackerUnit;
	}

	/**
	 * @param attacker Attacking unit
	 */
	public final void setAttackerUnit (final MemoryUnit attacker)
	{
		attackerUnit = attacker;
	}
	
	/**
	 * @return Unit being attacked
	 */
	public final MemoryUnit getDefenderUnit ()
	{
		return defenderUnit;
	}
	
	/**
	 * @param defender Unit being attacked
	 */
	public final void setDefenderUnit (final MemoryUnit defender)
	{
		defenderUnit = defender;
	}

	/**
	 * @return Player who owns the attacking unit
	 */
	public final PlayerPublicDetails getAttackingPlayer ()
	{
		return attackingPlayer;
	}

	/**
	 * @param player Player who owns the attacking unit
	 */
	public final void setAttackingPlayer (final PlayerPublicDetails player)
	{
		attackingPlayer = player;
	}
	
	/**
	 * @return Player who owns unit being attacked
	 */
	public final PlayerPublicDetails getDefenderPlayer ()
	{
		return defenderPlayer;
	}
	
	/**
	 * @param player Player who owns unit being attacked
	 */
	public final void setDefenderPlayer (final PlayerPublicDetails player)
	{
		defenderPlayer = player;
	}

	/**
	 * @return Language database holder
	 */
	public final LanguageDatabaseHolder getLanguageHolder ()
	{
		return languageHolder;
	}
	
	/**
	 * @param holder Language database holder
	 */
	public final void setLanguageHolder (final LanguageDatabaseHolder holder)
	{
		languageHolder = holder;
	}

	/**
	 * Convenience shortcut for accessing the Language XML database
	 * @return Language database
	 */
	public final LanguageDatabaseEx getLanguage ()
	{
		return languageHolder.getLanguage ();
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
	 * @return Multiplayer client
	 */
	public final MomClient getClient ()
	{
		return client;
	}
	
	/**
	 * @param obj Multiplayer client
	 */
	public final void setClient (final MomClient obj)
	{
		client = obj;
	}

	/**
	 * @return Session utils
	 */
	public final MultiplayerSessionUtils getMultiplayerSessionUtils ()
	{
		return multiplayerSessionUtils;
	}

	/**
	 * @param util Session utils
	 */
	public final void setMultiplayerSessionUtils (final MultiplayerSessionUtils util)
	{
		multiplayerSessionUtils = util;
	}

	/**
	 * @return Client-side unit utils
	 */
	public final UnitClientUtils getUnitClientUtils ()
	{
		return unitClientUtils;
	}

	/**
	 * @param util Client-side unit utils
	 */
	public final void setUnitClientUtils (final UnitClientUtils util)
	{
		unitClientUtils = util;
	}

	/**
	 * @return Wizard client utils
	 */
	public final WizardClientUtils getWizardClientUtils ()
	{
		return wizardClientUtils;
	}

	/**
	 * @param util Wizard client utils
	 */
	public final void setWizardClientUtils (final WizardClientUtils util)
	{
		wizardClientUtils = util;
	}
}